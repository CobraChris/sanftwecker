package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Timer-only alarm view. The on-screen overrun counts up while ringing;
 * stop actions and the finite 3-minute sound/vibration are service-owned.
 * The existing wake-alarm screen and scheduler remain independent.
 */
public final class TimerRingActivity extends Activity {
    public static final String EXTRA_LABEL="timer_ring_label";
    public static final String EXTRA_ID="timer_ring_id";
    public static final String EXTRA_DUE_WALL="timer_ring_due_wall";
    public static final String EXTRA_DUE_ELAPSED="timer_ring_due_elapsed";
    public static final String EXTRA_BOOT_OFFSET="timer_ring_boot_offset";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){
        @Override public void run(){
            refreshTime();
            handler.postDelayed(this,250L);
        }
    };
    private TextView timerName,elapsedTime;
    private int timerId;
    private long dueWall,dueElapsed,bootOffset;
    private boolean registered;
    private final BroadcastReceiver finished=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){
            if(TimerRingingService.ACTION_FINISHED.equals(i.getAction()))finish();
        }
    };

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        UiKit.window(this);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        LinearLayout root=UiKit.mainColumn(this);
        root.setGravity(Gravity.CENTER_VERTICAL);

        TextView heading=UiKit.label(this,"TIMER ABGELAUFEN",17,UiKit.BLUE);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading);

        timerName=UiKit.label(this,"Timer",31,UiKit.TEXT);
        timerName.setGravity(Gravity.CENTER);
        timerName.setMaxLines(2);
        root.addView(timerName);

        TextView overrunTitle=UiKit.label(this,"ÜBERZIEHUNGSZEIT",13,UiKit.MUTED);
        overrunTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(-1,-2);
        titleParams.topMargin=UiKit.dp(this,24);
        root.addView(overrunTitle,titleParams);

        elapsedTime=UiKit.label(this,"+00:00:00",58,UiKit.BLUE);
        elapsedTime.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);
        elapsedTime.setGravity(Gravity.CENTER);
        elapsedTime.setSingleLine(true);
        elapsedTime.setMaxLines(1);
        elapsedTime.setIncludeFontPadding(false);
        elapsedTime.setMinHeight(UiKit.dp(this,78));
        elapsedTime.setAutoSizeTextTypeUniformWithConfiguration(
                34,62,1,TypedValue.COMPLEX_UNIT_SP);
        root.addView(elapsedTime);

        TextView help=UiKit.label(this,
                "Ton und Vibration enden nach spätestens 3 Minuten. "+
                "Die Überziehungszeit wird bis zum Stoppen angezeigt.",
                14,UiKit.MUTED);
        help.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams helpParams=new LinearLayout.LayoutParams(-1,-2);
        helpParams.topMargin=UiKit.dp(this,18);
        root.addView(help,helpParams);

        Button stop=UiKit.button(this,"ALARM STOPPEN",true);
        stop.setContentDescription("Timeralarm auf Handy und Galaxy Watch stoppen");
        stop.setMinHeight(UiKit.dp(this,72));
        LinearLayout.LayoutParams stopParams=new LinearLayout.LayoutParams(-1,UiKit.dp(this,72));
        stopParams.topMargin=UiKit.dp(this,30);
        root.addView(stop,stopParams);
        stop.setOnClickListener(v->{
            try{
                startService(new Intent(this,TimerRingingService.class)
                        .setAction(TimerRingingService.ACTION_STOP_ALL));
                finish();
            }catch(RuntimeException ex){
                new android.app.AlertDialog.Builder(this)
                        .setMessage("Timer konnte nicht gestoppt werden. "+
                                "Bitte die Stoppen-Aktion in der Benachrichtigung verwenden.")
                        .setPositiveButton("OK",null).show();
            }
        });
        TextView plusTitle=UiKit.label(this,"TIMER VERLÄNGERN",14,UiKit.MUTED);
        plusTitle.setGravity(Gravity.CENTER);
        root.addView(plusTitle);
        LinearLayout quick=UiKit.row(this);
        Button plus30=UiKit.button(this,"+30 Sek.",false);
        Button plus60=UiKit.button(this,"+1 Min.",false);
        quick.addView(plus30,new LinearLayout.LayoutParams(0,UiKit.dp(this,58),1));
        quick.addView(plus60,new LinearLayout.LayoutParams(0,UiKit.dp(this,58),1));
        root.addView(quick);
        plus30.setOnClickListener(v->extend(30_000L));
        plus60.setOnClickListener(v->extend(60_000L));
        Button more=UiKit.button(this,"Mehr: +2 / +5 / +10 Min.",false);
        root.addView(more);
        more.setOnClickListener(v->new android.app.AlertDialog.Builder(this)
                .setTitle("Timer verlängern")
                .setItems(new String[]{"+2 Minuten","+5 Minuten","+10 Minuten"},
                        (dialog,which)->extend(new long[]{120_000L,300_000L,600_000L}[which]))
                .setNegativeButton("Abbrechen",null).show());
        setContentView(UiKit.scroll(this,root));
        updateName(getIntent());
    }
    private void extend(long ms){
        if(timerId<=0||dueWall<=0)return;
        try{
            if(TimerScheduler.extendFinished(this,timerId,dueWall,ms))finish();
            else new android.app.AlertDialog.Builder(this)
                .setMessage("Dieser Timer wurde bereits geändert oder neu gestartet.")
                .setPositiveButton("OK",null).show();
        }catch(RuntimeException ex){
            new android.app.AlertDialog.Builder(this)
                .setMessage("Verlängerung nicht möglich: "+ex.getMessage())
                .setPositiveButton("OK",null).show();
        }
    }
    private void updateName(Intent intent){
        String name=intent==null?null:intent.getStringExtra(EXTRA_LABEL);
        timerId=intent==null?0:intent.getIntExtra(EXTRA_ID,0);
        dueWall=intent==null?0:intent.getLongExtra(EXTRA_DUE_WALL,0);
        dueElapsed=intent==null?0:intent.getLongExtra(EXTRA_DUE_ELAPSED,0);
        bootOffset=intent==null?0:intent.getLongExtra(EXTRA_BOOT_OFFSET,0);
        if(timerName!=null)timerName.setText(name==null||name.isEmpty()?"Timer":name);
        refreshTime();
    }
    private void refreshTime(){
        if(elapsedTime==null)return;
        long wallNow=System.currentTimeMillis();
        long elapsedNow=SystemClock.elapsedRealtime();
        long millis=TimerRingClock.elapsed(
                dueWall,dueElapsed,bootOffset,wallNow,elapsedNow);
        // For legacy notifications without occurrence extras, read a matching
        // stored completed timer. Never count an already restarted cycle.
        if(dueWall<=0&&timerId>0){
            TimerEntry stored=TimerRepository.get(this,timerId);
            if(stored!=null&&stored.state==TimerEntry.FINISHED)
                millis=stored.overrun(wallNow,elapsedNow);
        }
        int fmt=timerId>0?TimerAlertSettings.timeFormat(this,timerId):
                TimerAlertSettings.FORMAT_HMS;
        elapsedTime.setText(TimerDisplay.overrun(millis,fmt));
    }
    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        updateName(intent);
    }
    @Override protected void onResume(){
        super.onResume();
        if(!registered){
            IntentFilter f=new IntentFilter(TimerRingingService.ACTION_FINISHED);
            androidx.core.content.ContextCompat.registerReceiver(
                    this,finished,f,androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
            registered=true;
        }
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }
    @Override protected void onPause(){
        handler.removeCallbacks(ticker);
        if(registered){unregisterReceiver(finished);registered=false;}
        super.onPause();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }
    @Override public void onBackPressed(){
        // The dedicated service remains active; dismissal requires STOP.
    }
}
