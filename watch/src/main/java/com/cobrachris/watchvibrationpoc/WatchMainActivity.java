package com.cobrachris.watchvibrationpoc;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Minimal standalone screen; vibration runs from local AlarmManager without UI. */
public final class WatchMainActivity extends Activity {
    private TextView status;
    private static final int NOTIFICATION_REQUEST=431;
    private Button snooze,local,openAlarmScreen,stop,updateWatch,notificationAccess;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable update=new Runnable(){
        @Override public void run(){
            refresh();
            handler.postDelayed(this,1000L);
        }
    };
    // Wear OS 4+ may suppress the alarm notification entirely unless the
    // user grants notification access. Vibration and the local stop screen
    // remain independent of this permission.
    private boolean notificationsAllowed(){
        if(Build.VERSION.SDK_INT>=33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        !=PackageManager.PERMISSION_GRANTED)return false;
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        return manager!=null && manager.areNotificationsEnabled();
    }
    private void openNotificationAccess(){
        if(Build.VERSION.SDK_INT>=33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        !=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_REQUEST);
            return;
        }
        // Permission granted but notifications switched off in system settings.
        Intent settings=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());
        try{startActivity(settings);}
        catch(RuntimeException unsupported){
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:"+getPackageName())));
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode,
            String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==NOTIFICATION_REQUEST)refresh();
    }
    private void refresh(){
        long when=getSharedPreferences(WatchAlarmService.ACTIVE_PREF,MODE_PRIVATE)
                .getLong("when",0);
        int snoozeMinutes=getSharedPreferences(WatchAlarmService.ACTIVE_PREF,MODE_PRIVATE)
                .getInt("snooze_minutes",0);
        int activeId=getSharedPreferences(WatchAlarmService.ACTIVE_PREF,MODE_PRIVATE)
                .getInt("id",0);
        boolean ringing=when>0&&activeId>0;
        // Keep only actions relevant to the current state on the round screen.
        updateWatch.setVisibility(ringing?View.GONE:View.VISIBLE);
        local.setVisibility(ringing?View.GONE:View.VISIBLE);
        notificationAccess.setVisibility(!ringing&&!notificationsAllowed()
                ?View.VISIBLE:View.GONE);
        stop.setVisibility(ringing?View.VISIBLE:View.GONE);
        openAlarmScreen.setVisibility(ringing?View.VISIBLE:View.GONE);
        if(ringing){
            boolean cascade=getSharedPreferences(WatchAlarmService.ACTIVE_PREF,MODE_PRIVATE)
                    .getBoolean("cascade",false);
            int intensity=WatchCascade.safeIntensity(
                    getSharedPreferences(WatchAlarmService.ACTIVE_PREF,MODE_PRIVATE)
                    .getInt("watch_intensity",WatchCascade.NORMAL));
            if(cascade){
                String profile=intensity==WatchCascade.SANFT?"Sanft":
                        intensity==WatchCascade.INTENSIV?"Intensiv":"Normal";
                String phase=WatchCascade.strong(when,System.currentTimeMillis())
                        ?"Phase 2 – deutlich":"Phase 1 – leise";
                status.setText("Sanftwecker klingelt\n"+profile+" · "+phase);
            }else status.setText("Sanftwecker klingelt");
            snooze.setVisibility(snoozeMinutes>0?View.VISIBLE:View.GONE);
            snooze.setText("Schlummern · "+snoozeMinutes+" Min.");
        }else{
            snooze.setVisibility(View.GONE);
            long next=WatchScheduleStore.next(this);
            status.setText(next>0
                    ?"Nächster lokaler Wecker:\n"+
                    new SimpleDateFormat("dd.MM. HH:mm",Locale.GERMANY).format(new Date(next))
                    :"Kein kommender Wecker auf der Uhr.\nFalls am Handy einer aktiv ist: Synchronisierung prüfen.");
            if(!notificationsAllowed())status.append("\nAlarmmeldung auf der Uhr deaktiviert.");
        }
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout layout=new LinearLayout(this);
        layout.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        layout.setOrientation(LinearLayout.VERTICAL);
        float density=getResources().getDisplayMetrics().density;
        layout.setPadding(Math.round(18*density),Math.round(40*density),
                Math.round(18*density),Math.round(88*density));
        // A square Android window overestimates the usable circular viewport.
        // Ensure a real scroll range even if the round edges clip a control.
        layout.setMinimumHeight(getResources().getDisplayMetrics().heightPixels+
                Math.round(64*density));
        status=new TextView(this);
        status.setTextSize(16);status.setGravity(Gravity.CENTER);
        layout.addView(status);
        notificationAccess=new Button(this);
        notificationAccess.setText("Alarmmeldungen erlauben");
        notificationAccess.setVisibility(View.GONE);
        layout.addView(notificationAccess);
        // Only a tap opens Android's permission dialog/settings. No automatic
        // permission prompt on installation or while the alarm is ringing.
        notificationAccess.setOnClickListener(v->openNotificationAccess());
        updateWatch=new Button(this);
        updateWatch.setText("Uhr aktualisieren");
        layout.addView(updateWatch);
        updateWatch.setOnClickListener(v->startActivity(
                new Intent(this,WatchSelfUpdateActivity.class)));
        local=new Button(this);
        local.setText("Alarmvibration testen");
        layout.addView(local);
        local.setOnClickListener(v->WatchAlarmService.start(
                this,1,System.currentTimeMillis(),1,"Uhr-Vibrationstest"));
        snooze=new Button(this);
        snooze.setText("Schlummern");
        snooze.setVisibility(View.GONE);
        layout.addView(snooze);
        snooze.setOnClickListener(v->{
            int id=getSharedPreferences(WatchAlarmService.ACTIVE_PREF,MODE_PRIVATE)
                    .getInt("id",0);
            long when=getSharedPreferences(WatchAlarmService.ACTIVE_PREF,MODE_PRIVATE)
                    .getLong("when",0L);
            int minutes=getSharedPreferences(WatchAlarmService.ACTIVE_PREF,MODE_PRIVATE)
                    .getInt("snooze_minutes",0);
            if(id<=0||when<=0||minutes<=0){refresh();return;}
            Intent snoozeRequest=new Intent(this,WatchAlarmService.class)
                    .setAction(WatchAlarmService.SNOOZE)
                    .putExtra("id",id).putExtra("when",when)
                    .putExtra("user",true);
            try{
                startService(snoozeRequest);
                status.setText("Schlummern angefordert – Handy-Bestätigung abwarten");
            }catch(RuntimeException ignored){
                status.setText("Keine Handy-Verbindung für Schlummern");
            }
        });
        openAlarmScreen=new Button(this);
        openAlarmScreen.setText("Alarmansicht öffnen");
        openAlarmScreen.setVisibility(View.GONE);
        layout.addView(openAlarmScreen);
        openAlarmScreen.setOnClickListener(v->
                startActivity(WatchRingActivity.openIntent(this)));
        stop=new Button(this);stop.setText("Alarm beenden");
        // On the circular watch the first visible action must be STOP.
        // Insert directly below status; snooze and secondary navigation follow.
        layout.addView(stop,1);
        stop.setOnClickListener(v->{
            // Local user can always cancel an ongoing alarm without the phone.
            Intent i=new Intent(this,WatchAlarmService.class).setAction(WatchAlarmService.STOP)
                    .putExtra("user",true).putExtra("stop_all",true);
            startService(i);
        });
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(layout);
        setContentView(scroll);
        refresh();
        WatchDataBootstrap.refresh(this,()->runOnUiThread(this::refresh));
    }
    @Override protected void onResume(){
        super.onResume();
        handler.removeCallbacks(update);
        handler.post(update);
    }
    @Override protected void onPause(){
        handler.removeCallbacks(update);
        super.onPause();
    }
}