package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * User-confirmed timer alarm screen. May appear above the lock screen only
 * when Android allows the full-screen alarm intent; never unlocks the device.
 */
public final class TimerRingActivity extends Activity {
    public static final String EXTRA_LABEL="timer_ring_label";
    private TextView timerName;
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

        timerName=UiKit.label(this,"Timer",34,UiKit.TEXT);
        timerName.setGravity(Gravity.CENTER);
        timerName.setMaxLines(3);
        root.addView(timerName);

        TextView help=UiKit.label(this,
                "Alarmton und Vibration laufen bis zum Stoppen, maximal 3 Minuten.",
                16,UiKit.MUTED);
        help.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(-1,-2);
        labelParams.topMargin=UiKit.dp(this,24);
        root.addView(help,labelParams);

        Button stop=UiKit.button(this,"TIMER STOPPEN",true);
        stop.setMinHeight(UiKit.dp(this,72));
        LinearLayout.LayoutParams stopParams=new LinearLayout.LayoutParams(-1,UiKit.dp(this,72));
        stopParams.topMargin=UiKit.dp(this,36);
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
        updateName(getIntent());
        setContentView(UiKit.scroll(this,root));
    }
    private void updateName(Intent intent){
        String name=intent==null?null:intent.getStringExtra(EXTRA_LABEL);
        if(timerName!=null)timerName.setText(name==null||name.isEmpty()?"Timer":name);
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
            if(Build.VERSION.SDK_INT>=33)
                registerReceiver(finished,f,Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(finished,f);
            registered=true;
        }
    }
    @Override protected void onPause(){
        if(registered){unregisterReceiver(finished);registered=false;}
        super.onPause();
    }
    @Override public void onBackPressed(){
        // Never mistake dismissing this screen for acknowledging a timer.
        // Home remains available; a persistent notification includes STOP.
    }
}
