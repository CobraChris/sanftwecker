package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Full set of user-chosen actions for an actual live reminder occurrence. */
public final class ReminderOptionsActivity extends Activity {
    public static final String EXTRA_ID=ReminderScheduler.ID;
    public static final String EXTRA_DUE=ReminderScheduler.DUE;
    private int id;
    private long due;

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        UiKit.window(this);
        id=getIntent().getIntExtra(EXTRA_ID,0);
        due=getIntent().getLongExtra(EXTRA_DUE,0);
        ReminderStore.Item item=ReminderStore.get(this,id);
        if(item==null||item.done||item.dueAt!=due){finish();return;}
        LinearLayout root=UiKit.mainColumn(this);
        TextView heading=UiKit.heading(this,"Erinnerung: "+item.title);
        root.addView(heading);
        root.addView(UiKit.label(this,"Wann wieder erinnern?",16,UiKit.MUTED));
        add(root,"In 10 Minuten nochmal erinnern",10);
        add(root,"In 30 Minuten nochmal erinnern",30);
        add(root,"In 60 Minuten nochmal erinnern",60);
        add(root,"Nächsten Tag wieder erinnern",-1);
        Button stop=UiKit.button(this,"Stoppen (bleibt offen)",false);
        stop.setOnClickListener(v->dispatch(ReminderScheduler.STOP,0));
        root.addView(stop);
        // Completion remains possible in the reminder list, but is not
        // confused with merely stopping its current vibration/sound.
        setContentView(UiKit.scroll(this,root));
    }
    private void add(LinearLayout root,String text,int delay){
        Button b=UiKit.button(this,text,true);
        b.setOnClickListener(v->dispatch(ReminderScheduler.CHOICE,delay));
        root.addView(b);
    }
    private void dispatch(String action,int delay){
        ReminderStore.Item current=ReminderStore.get(this,id);
        if(current==null||current.done||current.dueAt!=due){finish();return;}
        Intent event=new Intent(this,ReminderReceiver.class).setAction(action)
            .putExtra(EXTRA_ID,id).putExtra(EXTRA_DUE,due);
        if(ReminderScheduler.CHOICE.equals(action))
            event.putExtra(ReminderScheduler.DELAY,delay);
        sendBroadcast(event);
        finish();
    }
}