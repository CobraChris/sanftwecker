package com.cobrachris.sanftwecker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Dedicated reminder alarms; never reuses a wake-alarm or timer PendingIntent.
 * Phase two must not run after completion or postponement of the occurrence.
 */
public final class ReminderScheduler {
    public static final String FIRE="com.cobrachris.sanftwecker.REMINDER_FIRE";
    public static final String ESCALATE="com.cobrachris.sanftwecker.REMINDER_ESCALATE";
    public static final String DONE="com.cobrachris.sanftwecker.REMINDER_DONE";
    public static final String LATER="com.cobrachris.sanftwecker.REMINDER_LATER";
    public static final String STOP="com.cobrachris.sanftwecker.REMINDER_STOP";
    public static final String CHOICE="com.cobrachris.sanftwecker.REMINDER_CHOICE";
    public static final String DELAY="reminder_delay";
    public static final String ID="reminder_id",DUE="reminder_due";
    private static final int BASE=150000;
    private ReminderScheduler(){}
    public static PendingIntent pending(Context c,int id,long due,String action){
        Intent i=new Intent(c,ReminderReceiver.class).setAction(action)
            .putExtra(ID,id).putExtra(DUE,due);
        int offset=ESCALATE.equals(action)?10000:DONE.equals(action)?20000:
                LATER.equals(action)?30000:STOP.equals(action)?40000:0;
        return PendingIntent.getBroadcast(c,BASE+offset+id,i,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    /** Distinct immutable choices are checked again against the live occurrence. */
    public static PendingIntent choice(Context c,int id,long due,int minutes){
        if(minutes!=10&&minutes!=30&&minutes!=60&&minutes!=-1)
            throw new IllegalArgumentException("Ungültige Erinnerungsauswahl");
        Intent intent=new Intent(c,ReminderReceiver.class).setAction(CHOICE)
            .putExtra(ID,id).putExtra(DUE,due).putExtra(DELAY,minutes);
        int slot=minutes==-1?4:minutes==10?1:minutes==30?2:3;
        return PendingIntent.getBroadcast(c,200000+slot*10000+id,intent,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static void cancel(Context c,int id){
        AlarmManager manager=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(manager!=null){
            manager.cancel(pending(c,id,0,FIRE));
            manager.cancel(pending(c,id,0,ESCALATE));
        }
    }
    public static void schedule(Context c,int id){
        ReminderStore.Item item=ReminderStore.get(c,id);
        if(item==null||item.done)return;
        if(!AlarmScheduler.canSchedule(c))
            throw new SecurityException("Genaue Alarme für Erinnerungen nicht freigegeben");
        AlarmManager manager=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(manager==null)throw new IllegalStateException("AlarmManager fehlt");
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
            item.dueAt,pending(c,id,item.dueAt,FIRE));
    }
    public static void restore(Context c){
        for(ReminderStore.Item item:ReminderStore.all(c)){
            if(item.done)continue;
            if(item.dueAt>System.currentTimeMillis()){
                try{schedule(c,item.id);}catch(RuntimeException ignored){}
            }else{
                // Missed reminders are retained as open, never silently marked done.
                ReminderNotice.show(c,item,false);
            }
        }
    }
    public static void scheduleEscalation(Context c,ReminderStore.Item item){
        if(item==null||!item.cascade)return;
        AlarmManager manager=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(manager==null)return;
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
            item.dueAt+WakeCascade.SECOND_STAGE_MS,
            pending(c,item.id,item.dueAt,ESCALATE));
    }
}