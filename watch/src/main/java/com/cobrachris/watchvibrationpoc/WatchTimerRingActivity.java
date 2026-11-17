package com.cobrachris.watchvibrationpoc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

/**
 * Round, manually opened Wear OS timer alarm screen.
 * Wear OS does not support notification full-screen intents. Existing
 * vibration, STOP_ALL and phone messaging remain service-owned.
 */
public final class WatchTimerRingActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){
        @Override public void run(){
            refresh();
            handler.postDelayed(this,250L);
        }
    };
    private TextView title,clock,state;
    private Button stop;
    private boolean hadActive;
    private int dp(int n){
        return Math.round(n*getResources().getDisplayMetrics().density);
    }
    private TextView text(String value,int sp,int color,boolean bold){
        TextView t=new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private static String overrun(long ms){
        long seconds=Math.max(0,ms/1000L);
        return String.format(Locale.GERMANY,"+%02d:%02d",
                seconds/60,seconds%60);
    }
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);
        // When the user opens the urgent notification while the watch is
        // sleeping, ask the OS to reveal the timer controls on the lockscreen.
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        int bg=WatchDashboardView.backgroundColor();
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        LinearLayout page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setBackgroundColor(bg);
        page.setPadding(dp(19),dp(33),dp(19),dp(74));
        page.setMinimumHeight(getResources().getDisplayMetrics().heightPixels+
                dp(56));

        TextView heading=text("TIMER ABGELAUFEN",12,0xff9dc0ff,true);
        page.addView(heading);
        title=text("Timer",19,Color.WHITE,true);
        title.setMaxLines(2);
        page.addView(title);
        clock=text("+00:00",34,0xff9dc0ff,true);
        clock.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);
        clock.setSingleLine(true);
        clock.setAutoSizeTextTypeUniformWithConfiguration(
                21,35,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        clock.setMinHeight(dp(60));
        page.addView(clock);
        state=text("Seit Ablauf",12,0xffb0b3c5,false);
        page.addView(state);
        stop=new Button(this);
        stop.setText("TIMER STOPPEN");
        WatchDashboardView.styleButton(stop,false,true);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(56));
        p.topMargin=dp(12);
        page.addView(stop,p);
        stop.setOnClickListener(v->{
            Intent i=new Intent(this,WatchTimerAlertService.class)
                    .setAction(WatchTimerAlertService.STOP_ALL);
            try{
                startService(i);
                stop.setEnabled(false);
                state.setText("Auf der Uhr beendet. Handy wird benachrichtigt.");
            }catch(RuntimeException error){
                state.setText("Stoppen fehlgeschlagen. Über Benachrichtigung versuchen.");
            }
        });

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setBackgroundColor(bg);
        scroll.addView(page);
        setContentView(scroll);
        refresh();
    }
    private void refresh(){
        SharedPreferences active=getSharedPreferences(
                WatchTimerAlertService.ACTIVE_PREF,MODE_PRIVATE);
        int count=active.getInt("count",0);
        long due=active.getLong("when",0L);
        boolean ringing=count>0&&due>0&&
                System.currentTimeMillis()<due+180_000L;
        if(!ringing){
            if(hadActive){finish();return;}
            clock.setText("+00:00");
            title.setText("Kein Timeralarm");
            state.setText("Der Timer kann am Handy erneut gestartet werden.");
            stop.setVisibility(View.GONE);
            return;
        }
        hadActive=true;
        stop.setVisibility(View.VISIBLE);
        String name=active.getString("name","Timer");
        title.setText(count>1?count+" Timer abgelaufen":name);
        clock.setText(overrun(System.currentTimeMillis()-due));
        state.setText("Seit Ablauf · Stoppen auf der Uhr oder am Handy");
    }
    @Override protected void onResume(){
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }
    @Override protected void onPause(){
        handler.removeCallbacks(ticker);
        super.onPause();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }
}
