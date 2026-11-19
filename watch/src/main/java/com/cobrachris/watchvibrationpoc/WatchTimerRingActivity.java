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
    private Button stop,plus30,plus60,more;
    private boolean hadActive,extensionPending;
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
        page.setPadding(dp(15),dp(9),dp(15),dp(24));
        page.setMinimumHeight(getResources().getDisplayMetrics().heightPixels);

        TextView heading=text("TIMER ABGELAUFEN",12,0xff9dc0ff,true);
        page.addView(heading);
        title=text("Timer",16,Color.WHITE,true);
        title.setMaxLines(2);
        page.addView(title);
        clock=text("+00:00",29,0xff9dc0ff,true);
        clock.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);
        clock.setSingleLine(true);
        clock.setAutoSizeTextTypeUniformWithConfiguration(
                20,30,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        clock.setMinHeight(dp(42));
        page.addView(clock);
        // Keep the primary choices above the fold on a round Galaxy Watch:
        // compact time, side-by-side extensions, and a dedicated STOP button.
        LinearLayout quick=new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.setGravity(Gravity.CENTER);
        plus30=new Button(this);plus30.setText("+30 Sek.");
        plus30.setTextSize(12);
        WatchDashboardView.styleButton(plus30,false,false);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(48),1f);
        left.rightMargin=dp(3);
        quick.addView(plus30,left);
        plus30.setOnClickListener(v->extend(30_000L));
        plus60=new Button(this);plus60.setText("+1 Min.");
        plus60.setTextSize(12);
        WatchDashboardView.styleButton(plus60,false,false);
        LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,dp(48),1f);
        right.leftMargin=dp(3);
        quick.addView(plus60,right);
        plus60.setOnClickListener(v->extend(60_000L));
        page.addView(quick);
        stop=new Button(this);
        stop.setText("TIMER STOPPEN");
        stop.setTextSize(13);
        WatchDashboardView.styleButton(stop,false,true);
        LinearLayout.LayoutParams stopParams=new LinearLayout.LayoutParams(-1,dp(48));
        stopParams.topMargin=dp(3);
        page.addView(stop,stopParams);
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
        more=new Button(this);more.setText("Mehr: +2 / +5 / +10 Min.");
        more.setTextSize(11);
        WatchDashboardView.styleButton(more,false,false);
        page.addView(more,new LinearLayout.LayoutParams(-1,dp(43)));
        more.setOnClickListener(v->new android.app.AlertDialog.Builder(this)
            .setTitle("Verlängern")
            .setItems(new String[]{"+2 Minuten","+5 Minuten","+10 Minuten"},
                (dialog,which)->extend(new long[]{120_000L,300_000L,600_000L}[which]))
            .setNegativeButton("Abbrechen",null).show());
        state=text("Seit Ablauf",11,0xffb0b3c5,false);
        page.addView(state);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(bg);
        scroll.addView(page);
        setContentView(scroll);
        refresh();
    }
    private void extend(long millis){
        SharedPreferences active=getSharedPreferences(
                WatchTimerAlertService.ACTIVE_PREF,MODE_PRIVATE);
        int id=active.getInt("id",-1);
        long due=active.getLong("when",0L);
        if(active.getInt("count",0)!=1||id<=0||due<=0){
            state.setText("Verlängern nur bei einem einzelnen Timeralarm möglich.");
            return;
        }
        String payload=id+":"+due+":"+millis;
        state.setText("Verlängerung am Handy angefordert …");
        extensionPending=true;
        plus30.setEnabled(false);plus60.setEnabled(false);more.setEnabled(false);
        handler.postDelayed(()->{
            if(extensionPending&&!isFinishing())
                restoreExtensionButtons("Keine Bestätigung; erneut versuchen oder am Handy verlängern.");
        },9000L);
        com.google.android.gms.wearable.Wearable.getNodeClient(this)
            .getConnectedNodes().addOnSuccessListener(nodes->{
                if(nodes.isEmpty()){
                    runOnUiThread(()->restoreExtensionButtons("Handy nicht verbunden"));
                    return;
                }
                for(com.google.android.gms.wearable.Node node:nodes)
                    com.google.android.gms.wearable.Wearable.getMessageClient(this)
                        .sendMessage(node.getId(),WatchTimerProtocol.WATCH_EXTEND,
                            payload.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        .addOnFailureListener(e->runOnUiThread(()->
                                restoreExtensionButtons("Verlängerung nicht gesendet")));
            }).addOnFailureListener(e->runOnUiThread(()->
                restoreExtensionButtons("Handy nicht erreichbar")));
    }
    private void restoreExtensionButtons(String message){
        extensionPending=false;
        state.setText(message);plus30.setEnabled(true);
        plus60.setEnabled(true);more.setEnabled(true);
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
            plus30.setVisibility(View.GONE);plus60.setVisibility(View.GONE);
            more.setVisibility(View.GONE);
            return;
        }
        hadActive=true;
        stop.setVisibility(View.VISIBLE);
        int show=count==1?View.VISIBLE:View.GONE;
        plus30.setVisibility(show);plus60.setVisibility(show);more.setVisibility(show);
        String name=active.getString("name","Timer");
        title.setText(count>1?count+" Timer abgelaufen":name);
        clock.setText(overrun(System.currentTimeMillis()-due));
        if(!extensionPending)
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
