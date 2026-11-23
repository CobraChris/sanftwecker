package com.cobrachris.watchvibrationpoc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Round-screen-friendly reminder actions, never dismissing a reminder as done. */
public final class WatchReminderOptionsActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        int id=getIntent().getIntExtra("id",0);
        long due=getIntent().getLongExtra("due",0);
        if(id<=0||due<=0){finish();return;}
        int width=Math.round(getResources().getDisplayMetrics().widthPixels*.79f);
        LinearLayout column=new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(12,32,12,60);
        TextView title=new TextView(this);
        title.setText("Erinnerung");
        title.setTextSize(19);
        title.setGravity(Gravity.CENTER);
        column.addView(title);
        TextView caption=new TextView(this);
        caption.setText("Wann wieder erinnern?");
        caption.setGravity(Gravity.CENTER);
        column.addView(caption);
        add(column,width,"In 10 Minuten",id,due,10);
        add(column,width,"In 30 Minuten",id,due,30);
        add(column,width,"In 60 Minuten",id,due,60);
        add(column,width,"Morgen wieder",id,due,-1);
        add(column,width,"Stoppen",id,due,0);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(column);
        setContentView(scroll);
    }
    private void add(LinearLayout column,int width,String label,int id,long due,int delay){
        Button button=new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(13);
        column.addView(button,new LinearLayout.LayoutParams(
            width,LinearLayout.LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(v->{
            Intent cmd=new Intent(this,WatchReminderService.class)
                .setAction(delay==0?WatchReminderService.USER_STOP:
                    WatchReminderService.CHOICE)
                .putExtra("id",id).putExtra("due",due)
                .putExtra("choice",delay);
            try{startService(cmd);}catch(RuntimeException ignored){}
            finish();
        });
    }
}