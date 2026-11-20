package com.cobrachris.watchvibrationpoc;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
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

/** Watch home dashboard; actual alarm/vibration always runs in independent services. */
public final class WatchMainActivity extends Activity {
    private TextView status;
    private WatchDashboardView dashboard;
    private static final int NOTIFICATION_REQUEST=431;
    private Button snooze,local,openAlarmScreen,stop,stopTimer,updateWatch,notificationAccess;
    private Button timerPlus30,timerPlus60,openTimerScreen;
    private LinearLayout quickTimer;
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
        return manager!=null && manager.areNotificationsEnabled() &&
                !WatchAlarmNotice.channelBlocked(this);
    }
    private void extendCurrentTimer(long extraMs){
        android.content.SharedPreferences active=getSharedPreferences(
                WatchTimerAlertService.ACTIVE_PREF,MODE_PRIVATE);
        int id=active.getInt("id",-1);
        long when=active.getLong("when",0);
        if(active.getInt("count",0)!=1||id<1||when<=0){
            status.setText("Verlängern nur bei einem einzelnen Timeralarm.");
            return;
        }
        Intent command=new Intent(this,WatchTimerAlertService.class)
                .setAction(WatchTimerAlertService.EXTEND)
                .putExtra("id",id).putExtra("when",when)
                .putExtra("extension",extraMs);
        try{
            startService(command);
            status.setText("Timerverlängerung an Handy gesendet …");
        }catch(RuntimeException e){
            status.setText("Timerverlängerung nicht möglich.");
        }
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
        android.content.SharedPreferences timerState=
                getSharedPreferences(WatchTimerAlertService.ACTIVE_PREF,MODE_PRIVATE);
        long timerWhen=timerState.getLong("when",0);
        boolean timerRinging=timerState.getInt("count",0)>0 &&
                timerWhen>0&&System.currentTimeMillis()<timerWhen+180_000L;
        // Clock alarm takes priority; a timer cannot replace its Stop/Snooze UI.
        dashboard.setVisibility(ringing||timerRinging?View.GONE:View.VISIBLE);
        stopTimer.setVisibility(!ringing&&timerRinging?View.VISIBLE:View.GONE);
        int timerActionVisibility=!ringing&&timerRinging&&
                timerState.getInt("count",0)==1?View.VISIBLE:View.GONE;
        quickTimer.setVisibility(timerActionVisibility);
        openTimerScreen.setVisibility(!ringing&&timerRinging?View.VISIBLE:View.GONE);
        updateWatch.setVisibility(ringing||timerRinging?View.GONE:View.VISIBLE);
        local.setVisibility(ringing||timerRinging?View.GONE:View.VISIBLE);
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
        }else if(timerRinging){
            snooze.setVisibility(View.GONE);
            int count=timerState.getInt("count",1);
            String name=timerState.getString("name","Timer");
            status.setText(count>1?count+" Timer abgelaufen":
                    name+" abgelaufen");
            if(!notificationsAllowed())
                status.append("\nTimer-Benachrichtigungen erlauben");
        }else{
            snooze.setVisibility(View.GONE);
            status.setText("SANFTWECKER");
            // Display only the alarms actually planned on the watch. A disabled
            // or not-yet-synced phone alarm is not falsely shown as scheduled.
            dashboard.render(WatchScheduleStore.upcoming(this));
            if(!notificationsAllowed())status.append("\nAlarmmeldung auf der Uhr deaktiviert.");
        }
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(WatchDashboardView.backgroundColor());
        getWindow().setNavigationBarColor(WatchDashboardView.backgroundColor());
        LinearLayout layout=new LinearLayout(this);
        layout.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        layout.setOrientation(LinearLayout.VERTICAL);
        float density=getResources().getDisplayMetrics().density;
        layout.setPadding(Math.round(18*density),Math.round(40*density),
                Math.round(18*density),Math.round(88*density));
        layout.setBackgroundColor(WatchDashboardView.backgroundColor());
        // A square Android window overestimates the usable circular viewport.
        // Ensure a real scroll range even if the round edges clip a control.
        layout.setMinimumHeight(getResources().getDisplayMetrics().heightPixels+
                Math.round(64*density));
        status=new TextView(this);
        status.setTextSize(16);status.setGravity(Gravity.CENTER);
        status.setTextColor(Color.rgb(157,192,255));
        status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        status.setMinHeight(Math.round(42*density));
        layout.addView(status);
        dashboard=new WatchDashboardView(this);
        layout.addView(dashboard);
        // Quick timer controls on the main Watch display (not only hidden
        // behind a notification). Clock alarm still takes priority.
        quickTimer=new LinearLayout(this);
        quickTimer.setOrientation(LinearLayout.HORIZONTAL);
        quickTimer.setGravity(Gravity.CENTER);
        timerPlus30=new Button(this);
        timerPlus30.setText("+30 Sek.");
        timerPlus30.setTextSize(12);
        WatchDashboardView.styleButton(timerPlus30,false,false);
        quickTimer.addView(timerPlus30,new LinearLayout.LayoutParams(0,
                Math.round(49*density),1f));
        timerPlus60=new Button(this);
        timerPlus60.setText("+1 Min.");
        timerPlus60.setTextSize(12);
        WatchDashboardView.styleButton(timerPlus60,false,false);
        quickTimer.addView(timerPlus60,new LinearLayout.LayoutParams(0,
                Math.round(49*density),1f));
        layout.addView(quickTimer);
        timerPlus30.setOnClickListener(v->extendCurrentTimer(30_000L));
        timerPlus60.setOnClickListener(v->extendCurrentTimer(60_000L));
        openTimerScreen=new Button(this);
        openTimerScreen.setText("Timer-Details / Mehr");
        WatchDashboardView.styleButton(openTimerScreen,false,false);
        layout.addView(openTimerScreen);
        openTimerScreen.setOnClickListener(v->
                startActivity(new Intent(this,WatchTimerRingActivity.class)));
        stopTimer=new Button(this);
        stopTimer.setText("Alarm stoppen");
        WatchDashboardView.styleButton(stopTimer,false,true);
        stopTimer.setVisibility(View.GONE);
        layout.addView(stopTimer);
        stopTimer.setOnClickListener(v->{
            Intent stopRequest=new Intent(this,WatchTimerAlertService.class)
                    .setAction(WatchTimerAlertService.STOP_ALL);
            try{startService(stopRequest);}
            catch(RuntimeException error){
                status.setText("Timer auf der Uhr konnte nicht gestoppt werden");
            }
        });
        notificationAccess=new Button(this);
        notificationAccess.setText("Alarmmeldungen erlauben");
        WatchDashboardView.styleButton(notificationAccess,true,false);
        notificationAccess.setVisibility(View.GONE);
        layout.addView(notificationAccess);
        // Only a tap opens Android's permission dialog/settings. No automatic
        // permission prompt on installation or while the alarm is ringing.
        notificationAccess.setOnClickListener(v->openNotificationAccess());
        updateWatch=new Button(this);
        updateWatch.setText("Uhr aktualisieren");
        WatchDashboardView.styleButton(updateWatch,false,false);
        layout.addView(updateWatch);
        updateWatch.setOnClickListener(v->startActivity(
                new Intent(this,WatchSelfUpdateActivity.class)));
        local=new Button(this);
        local.setText("Alarmvibration testen");
        WatchDashboardView.styleButton(local,false,false);
        layout.addView(local);
        local.setOnClickListener(v->WatchAlarmService.start(
                this,1,System.currentTimeMillis(),1,"Uhr-Vibrationstest"));
        snooze=new Button(this);
        snooze.setText("Schlummern");
        WatchDashboardView.styleButton(snooze,true,false);
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
        WatchDashboardView.styleButton(openAlarmScreen,false,false);
        openAlarmScreen.setVisibility(View.GONE);
        layout.addView(openAlarmScreen);
        openAlarmScreen.setOnClickListener(v->
                startActivity(WatchRingActivity.openIntent(this)));
        stop=new Button(this);stop.setText("Alarm beenden");
        WatchDashboardView.styleButton(stop,false,true);
        // On the circular watch the first visible action must be STOP.
        // Insert directly below status; snooze and secondary navigation follow.
        layout.addView(stop,1);
        // Keep Snooze and the alarm screen immediately below the emergency stop
        // rather than after the long list of upcoming alarms and maintenance tools.
        layout.removeView(snooze);
        layout.addView(snooze,2);
        layout.removeView(openAlarmScreen);
        layout.addView(openAlarmScreen,3);
        stop.setOnClickListener(v->{
            // Local user can always cancel an ongoing alarm without the phone.
            Intent i=new Intent(this,WatchAlarmService.class).setAction(WatchAlarmService.STOP)
                    .putExtra("user",true).putExtra("stop_all",true);
            startService(i);
        });
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setBackgroundColor(WatchDashboardView.backgroundColor());
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(layout);
        setContentView(scroll);
        refresh();
        WatchDataBootstrap.refresh(this,()->runOnUiThread(this::refresh));
    }
    @Override protected void onResume(){
        super.onResume();
        dashboard.invalidateSnapshot();
        handler.removeCallbacks(update);
        handler.post(update);
    }
    @Override protected void onPause(){
        handler.removeCallbacks(update);
        super.onPause();
    }
}