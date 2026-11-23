package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Calendar;

/** Reminders are independent of wake-alarm IDs and snooze state. */
public final class ReminderStore {
    private static final String PREF="sanftwecker_reminders_v1";
    private ReminderStore(){}
    public static final class Item {
        public final int id;
        public final String title;
        public final long dueAt,anchorAt;
        public final boolean cascade,done;
        public final int watchIntensity,watchPulses,watchIntervalSeconds,repeat,postponeMinutes,tone;
        public final boolean phoneSound;
        Item(int id,String title,long dueAt,long anchorAt,boolean cascade,boolean done,
                int intensity,int pulses,int interval,int repeat,int postponeMinutes,boolean phoneSound,int tone){
            this.id=id;this.title=title;this.dueAt=dueAt;this.anchorAt=anchorAt;
            this.cascade=cascade;
            this.done=done;this.watchIntensity=intensity;
            this.watchPulses=pulses;this.watchIntervalSeconds=interval;
            this.repeat=repeat;this.postponeMinutes=postponeMinutes;this.phoneSound=phoneSound;
            this.tone=tone;
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
            p.getLong(id+"_due",0),p.getLong(id+"_anchor",p.getLong(id+"_due",0)),
            p.getBoolean(id+"_cascade",true),
            p.getBoolean(id+"_done",false),p.getInt(id+"_intensity",1),
            p.getInt(id+"_pulses",6),p.getInt(id+"_interval",5),
            p.getInt(id+"_repeat",0),p.getInt(id+"_postpone",10),
            p.getBoolean(id+"_sound",true),p.getInt(id+"_tone",0));
    }
    public static synchronized int create(Context c,String title,long dueAt,
            boolean cascade,int intensity,int pulses,int interval){
        return create(c,title,dueAt,cascade,intensity,pulses,interval,0,10,true,0);
    }
    public static synchronized int create(Context c,String title,long dueAt,
            boolean cascade,int intensity,int pulses,int interval,
            int repeat,int postponeMinutes,boolean phoneSound,int tone){
        if(title==null||title.trim().isEmpty()||title.length()>200||
                dueAt<=System.currentTimeMillis()||intensity<0||intensity>2||
                pulses<1||pulses>30||interval<1||interval>60||
                repeat<0||repeat>2||!ToneCatalog.valid(tone)||
                !(postponeMinutes==5||postponeMinutes==10||postponeMinutes==30))
            throw new IllegalArgumentException("Erinnerung oder Termin ungültig");
        SharedPreferences p=prefs(c);
        int id=p.getInt("last_id",0)+1;
        if(id>9999)throw new IllegalStateException("Erinnerungslimit erreicht");
        if(!p.edit().putInt("last_id",id).putString(id+"_title",title.trim())
                .putLong(id+"_due",dueAt).putLong(id+"_anchor",dueAt).putBoolean(id+"_cascade",cascade)
                .putInt(id+"_intensity",intensity).putInt(id+"_pulses",pulses)
                .putInt(id+"_interval",interval).putBoolean(id+"_done",false)
                .putInt(id+"_repeat",repeat).putInt(id+"_postpone",postponeMinutes)
                .putBoolean(id+"_sound",phoneSound).putInt(id+"_tone",tone).commit())
            throw new IllegalStateException("Erinnerung nicht gespeichert");
        return id;
    }
    /** Edit the original reminder ID, atomically, without creating a duplicate. */
    public static synchronized void update(Context c,int id,String title,long dueAt,
            boolean cascade,int intensity,int pulses,int interval,
            int repeat,int postponeMinutes,boolean phoneSound,int tone){
        Item original=get(c,id);
        if(original==null)throw new IllegalArgumentException("Erinnerung nicht gefunden");
        if(title==null||title.trim().isEmpty()||title.length()>200||
                dueAt<=System.currentTimeMillis()||intensity<0||intensity>2||
                pulses<1||pulses>30||interval<1||interval>60||
                repeat<0||repeat>2||!ToneCatalog.valid(tone)||
                !(postponeMinutes==5||postponeMinutes==10||postponeMinutes==30))
            throw new IllegalArgumentException("Erinnerung oder Termin ungültig");
        SharedPreferences.Editor edit=prefs(c).edit();
        if(!edit.putString(id+"_title",title.trim())
                .putLong(id+"_due",dueAt).putLong(id+"_anchor",dueAt)
                .putBoolean(id+"_cascade",cascade).putBoolean(id+"_done",false)
                .putInt(id+"_intensity",intensity).putInt(id+"_pulses",pulses)
                .putInt(id+"_interval",interval).putInt(id+"_repeat",repeat)
                .putInt(id+"_postpone",postponeMinutes).putBoolean(id+"_sound",phoneSound)
                .putInt(id+"_tone",tone).commit())
            throw new IllegalStateException("Bearbeitung nicht gespeichert");
    }

    /** Roll back a failed re-schedule, including the original recurring anchor. */
    public static synchronized void restore(Context c,Item item){
        if(item==null||get(c,item.id)==null)return;
        if(!prefs(c).edit().putString(item.id+"_title",item.title)
                .putLong(item.id+"_due",item.dueAt).putLong(item.id+"_anchor",item.anchorAt)
                .putBoolean(item.id+"_cascade",item.cascade).putBoolean(item.id+"_done",item.done)
                .putInt(item.id+"_intensity",item.watchIntensity)
                .putInt(item.id+"_pulses",item.watchPulses)
                .putInt(item.id+"_interval",item.watchIntervalSeconds)
                .putInt(item.id+"_repeat",item.repeat)
                .putInt(item.id+"_postpone",item.postponeMinutes)
                .putBoolean(item.id+"_sound",item.phoneSound)
                .putInt(item.id+"_tone",item.tone).commit())
            throw new IllegalStateException("Vorherigen Zustand nicht wiederhergestellt");
    }

    /** A recurring reminder schedules the next local wall-clock occurrence.
     * One-time reminders are completed; the current occurrence is always closed.
     */
    public static synchronized void complete(Context c,int id){
        Item item=get(c,id);
        if(item==null||item.done)return;
        if(item.repeat==0){
            prefs(c).edit().putBoolean(id+"_done",true).commit();
            return;
        }
        Calendar cal=Calendar.getInstance();
        cal.setTimeInMillis(item.anchorAt);
        int field=item.repeat==1?Calendar.DAY_OF_YEAR:Calendar.WEEK_OF_YEAR;
        do{cal.add(field,1);}while(cal.getTimeInMillis()<=System.currentTimeMillis());
        prefs(c).edit().putLong(id+"_due",cal.getTimeInMillis())
                .putLong(id+"_anchor",cal.getTimeInMillis())
                .putBoolean(id+"_done",false).commit();
    }
    /** Deleted reminders must not remain in the future schedule. */
    public static synchronized void delete(Context c,int id){abandon(c,id);}
    /** Roll back an unschedulable newly created reminder. */
    public static synchronized void abandon(Context c,int id){
        SharedPreferences p=prefs(c);
        if(!p.contains(id+"_due"))return;
        p.edit().remove(id+"_due").remove(id+"_title").remove(id+"_cascade")
          .remove(id+"_intensity").remove(id+"_pulses").remove(id+"_interval")
          .remove(id+"_done").remove(id+"_repeat").remove(id+"_postpone")
          .remove(id+"_sound").remove(id+"_tone").remove(id+"_anchor").commit();
    }
    public static synchronized void postpone(Context c,int id,long nextDue){
        if(get(c,id)==null||nextDue<=System.currentTimeMillis())return;
        prefs(c).edit().putLong(id+"_due",nextDue).putBoolean(id+"_done",false).commit();
    }
}