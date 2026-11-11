package com.cobrachris.watchvibrationpoc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * User-facing alarm screen. Scheduled exact alarms can launch this Activity
 * from AlarmManager while the watch is locked or asleep. Wear OS does NOT
 * support notification full-screen intents; the foreground notification
 * remains a separate, tappable fallback.
 */
public final class WatchRingActivity extends Activity {
    public static final String ACTION_LOCAL_ALARM =
            "com.cobrachris.sanftwecker.WATCH_LOCAL_ALARM_UI";
    private static final long START_GRACE_MS=8_000L;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable update=new Runnable(){
        @Override public void run(){
            refresh();
            handler.postDelayed(this,1000L);
        }
    };
    private TextView state;
    private Button snooze,stop;
    private long createdAt,requestedSnoozeAt;
    private boolean alarmWasVisible,screenResumed,ackSent;

    public static Intent ringIntent(Context c,int slot,long when){
        return new Intent(c,WatchRingActivity.class)
                .setAction(ACTION_LOCAL_ALARM)
                .putExtra("slot",slot).putExtra("when",when)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
    public static Intent openIntent(Context c){
        return new Intent(c,WatchRingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
    private static int dp(Context c,int value){
        return Math.round(value*c.getResources().getDisplayMetrics().density);
    }
    private static LinearLayout.LayoutParams params(int height){
        return new LinearLayout.LayoutParams(-1,height);
    }
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        createdAt=SystemClock.elapsedRealtime();

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(this,14),dp(this,6),dp(this,14),dp(this,6));

        state=new TextView(this);
        state.setTextSize(17);
        state.setGravity(Gravity.CENTER);
        root.addView(state,new LinearLayout.LayoutParams(-1,-2));

        snooze=new Button(this);
        snooze.setText("Schlummern");
        snooze.setTextSize(16);
        LinearLayout.LayoutParams snoozeParams=params(dp(this,56));
        snoozeParams.topMargin=dp(this,5);
        root.addView(snooze,snoozeParams);
        snooze.setOnClickListener(v->requestSnooze());

        stop=new Button(this);
        stop.setText("ALARM BEENDEN");
        stop.setTextSize(16);
        LinearLayout.LayoutParams stopParams=params(dp(this,56));
        stopParams.topMargin=dp(this,5);
        root.addView(stop,stopParams);
        stop.setOnClickListener(v->stopAlarm());

        setContentView(root);
        handleAlarm(getIntent());
        refresh();
    }
    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        createdAt=SystemClock.elapsedRealtime();
        alarmWasVisible=false;
        ackSent=false;
        handleAlarm(intent);
        refresh();
    }
    private void handleAlarm(Intent intent){
        if(intent==null || !ACTION_LOCAL_ALARM.equals(intent.getAction()))return;
        int slot=intent.getIntExtra("slot",-1);
        long when=intent.getLongExtra("when",0L);
        if(WatchAlarmProtocol.validSlot(slot) && when>0)
            WatchScheduleStore.fired(this,slot,when);
    }
    private SharedPreferences active(){
        return getSharedPreferences(WatchAlarmService.ACTIVE_PREF,MODE_PRIVATE);
    }
    private void refresh(){
        SharedPreferences current=active();
        int id=current.getInt("id",0);
        long when=current.getLong("when",0L);
        boolean ringing=WatchAlarmUiPolicy.active(id,when);
        if(!ringing){
            snooze.setVisibility(View.GONE);
            stop.setEnabled(false);
            if(alarmWasVisible){
                finish();
            }else if(SystemClock.elapsedRealtime()-createdAt<START_GRACE_MS){
                state.setText("Wecker wird gestartet …");
            }else{
                state.setText("Kein aktiver Uhralarm.\nAm Handy kannst du den Alarm jederzeit stoppen.");
                stop.setText("SCHLIESSEN");
                stop.setEnabled(true);
            }
            return;
        }
        alarmWasVisible=true;
        if(screenResumed&&!ackSent){
            ackSent=true;
            WatchAlarmScreenAck.visible(this,id);
        }
        stop.setText("ALARM BEENDEN");
        stop.setEnabled(true);
        boolean cascade=current.getBoolean("cascade",false);
        int intensity=WatchCascade.safeIntensity(current.getInt(
                "watch_intensity",WatchCascade.NORMAL));
        String level=intensity==WatchCascade.SANFT?"Sanft":
                intensity==WatchCascade.INTENSIV?"Intensiv":"Normal";
        String phase=WatchAlarmUiPolicy.stage(cascade,when,System.currentTimeMillis());
        int minutes=current.getInt("snooze_minutes",0);
        boolean snoozing=requestedSnoozeAt>0 &&
                SystemClock.elapsedRealtime()-requestedSnoozeAt<10_000L;
        state.setText(snoozing?"Schlummern wird bestätigt …":
                "Sanftwecker\n"+(cascade?level+" · ":"")+phase);
        snooze.setVisibility(WatchAlarmUiPolicy.snoozeVisible(id,when,minutes)
                ?View.VISIBLE:View.GONE);
        snooze.setText("SCHLUMMERN · "+minutes+" Min.");
        snooze.setEnabled(!snoozing);
        if(!snoozing)requestedSnoozeAt=0;
    }
    private void requestSnooze(){
        SharedPreferences current=active();
        int id=current.getInt("id",0),minutes=current.getInt("snooze_minutes",0);
        long when=current.getLong("when",0L);
        if(!WatchAlarmUiPolicy.snoozeVisible(id,when,minutes))return;
        Intent request=new Intent(this,WatchAlarmService.class)
                .setAction(WatchAlarmService.SNOOZE)
                .putExtra("id",id).putExtra("when",when).putExtra("user",true);
        try{
            startService(request);
            requestedSnoozeAt=SystemClock.elapsedRealtime();
            refresh();
        }catch(RuntimeException ignored){
            state.setText("Schlummern nicht übertragen.\nAlarm beenden bleibt möglich.");
        }
    }
    private void stopAlarm(){
        if(active().getLong("when",0)<=0){
            finish();
            return;
        }
        // An explicit user press must always stop the watch, even offline.
        Intent stopIntent=new Intent(this,WatchAlarmService.class)
                .setAction(WatchAlarmService.STOP)
                .putExtra("user",true).putExtra("stop_all",true);
        try{
            startService(stopIntent);
            state.setText("Alarm wird beendet …");
        }catch(RuntimeException ignored){
            state.setText("Bitte Alarm am Handy beenden.");
        }
    }
    @Override protected void onResume(){
        super.onResume();
        screenResumed=true;
        handler.removeCallbacks(update);
        handler.post(update);
    }
    @Override protected void onPause(){
        screenResumed=false;
        handler.removeCallbacks(update);
        super.onPause();
    }
}
