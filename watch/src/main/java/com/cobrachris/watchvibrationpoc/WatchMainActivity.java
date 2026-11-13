package com.cobrachris.watchvibrationpoc;

import android.app.Activity;
import android.content.Intent;
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
    private Button snooze,local,openAlarmScreen,stop,updateWatch;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable update=new Runnable(){
        @Override public void run(){
            refresh();
            handler.postDelayed(this,1000L);
        }
    };
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
        layout.addView(stop);
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