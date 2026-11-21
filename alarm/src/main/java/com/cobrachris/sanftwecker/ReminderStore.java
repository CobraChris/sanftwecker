package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reminders are independent of wake-alarm IDs and snooze state. */
public final class ReminderStore {
    private static final String PREF="sanftwecker_reminders_v1";
    private ReminderStore(){}
    public static final class Item {
        public final int id;
        public final String title;
        public final long dueAt;
        public final boolean cascade,done;
        public final int watchIntensity,watchPulses,watchIntervalSeconds;
        Item(int id,String title,long dueAt,boolean cascade,boolean done,
                int intensity,int pulses,int interval){
            this.id=id;this.title=title;this.dueAt=dueAt;this.cascade=cascade;
            this.done=done;this.watchIntensity=intensity;
            this.watchPulses=pulses;this.watchIntervalSeconds=interval;
        }
    }
    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    public static synchronized List<Item> all(Context c){
        SharedPreferences p=prefs(c);
        List<Item> result=new ArrayList<>();
        for(int id=1;id<p.getInt("last_id",0)+1;id++){
            if(!p.contains(id+"_due"))continue;
            result.add(get(c,id));
        }
        Collections.sort(result,(a,b)->Long.compare(a.dueAt,b.dueAt));
        return result;
    }
    public static Item get(Context c,int id){
        SharedPreferences p=prefs(c);
        if(id<1||!p.contains(id+"_due"))return null;
        return new Item(id,p.getString(id+"_title","Erinnerung"),
            p.getLong(id+"_due",0),p.getBoolean(id+"_cascade",true),
            p.getBoolean(id+"_done",false),p.getInt(id+"_intensity",1),
            p.getInt(id+"_pulses",6),p.getInt(id+"_interval",5));
    }
    public static synchronized int create(Context c,String title,long dueAt,
            boolean cascade,int intensity,int pulses,int interval){
        if(title==null||title.trim().isEmpty()||title.length()>200||
                dueAt<=System.currentTimeMillis()||intensity<0||intensity>2||
                pulses<1||pulses>30||interval<1||interval>60)
            throw new IllegalArgumentException("Erinnerung oder Termin ungültig");
        SharedPreferences p=prefs(c);
        int id=p.getInt("last_id",0)+1;
        if(id>9999)throw new IllegalStateException("Erinnerungslimit erreicht");
        if(!p.edit().putInt("last_id",id).putString(id+"_title",title.trim())
                .putLong(id+"_due",dueAt).putBoolean(id+"_cascade",cascade)
                .putInt(id+"_intensity",intensity).putInt(id+"_pulses",pulses)
                .putInt(id+"_interval",interval).putBoolean(id+"_done",false).commit())
            throw new IllegalStateException("Erinnerung nicht gespeichert");
        return id;
    }
    /** Stopping a notification never marks a reminder done. */
    public static synchronized void complete(Context c,int id){
        if(get(c,id)==null)return;
        prefs(c).edit().putBoolean(id+"_done",true).commit();
    }
    public static synchronized void postpone(Context c,int id,long nextDue){
        if(get(c,id)==null||nextDue<=System.currentTimeMillis())return;
        prefs(c).edit().putLong(id+"_due",nextDue).putBoolean(id+"_done",false).commit();
    }
}