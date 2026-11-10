package com.cobrachris.watchvibrationpoc;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import java.util.Map;

/** Durable watch-local alarm clock; exact alarm survives disconnection from phone. */
public final class WatchScheduleStore {
    private static final String PREF="sanftwecker_watch_schedule_v1";
    public static final String ACTION_RING="com.cobrachris.sanftwecker.WATCH_LOCAL_RING";
    private WatchScheduleStore(){}
    private static SharedPreferences store(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    private static String key(int slot,String name){return "slot_"+slot+"_"+name;}

    private static PendingIntent receiver(Context c,int slot,long when){
        Intent i=new Intent(c,WatchAlarmReceiver.class).setAction(ACTION_RING)
                .putExtra("slot",slot).putExtra("when",when);
        return PendingIntent.getBroadcast(c,6000+slot,i,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static void setAlarm(Context c,int slot,long when){
        AlarmManager m=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(m==null)return;
        PendingIntent event=receiver(c,slot,when);
        PendingIntent display=PendingIntent.getActivity(c,7000+slot,
                new Intent(c,WatchMainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        try{
            if(Build.VERSION.SDK_INT<Build.VERSION_CODES.S||m.canScheduleExactAlarms())
                m.setAlarmClock(new AlarmManager.AlarmClockInfo(when,display),event);
            else {
                Log.w("SanftweckerWatch","Exact alarms not yet allowed; idle fallback");
                m.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,event);
            }
        }catch(SecurityException e){
            Log.w("SanftweckerWatch","Exact watch alarm unavailable",e);
            m.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,event);
        }
    }
    public static synchronized void plan(Context c,int slot,int id,long when,
            String name,int minutes){
        plan(c,slot,id,when,name,minutes,false);
    }
    public static synchronized void plan(Context c,int slot,int id,long when,
            String name,int minutes,boolean cascade){
        plan(c,slot,id,when,name,minutes,cascade,0);
    }
    public static synchronized void plan(Context c,int slot,int id,long when,
            String name,int minutes,boolean cascade,int snoozeMinutes){
        if(!WatchAlarmProtocol.validSlot(slot)||id<1||id>1000 ||
                snoozeMinutes<0||snoozeMinutes>30)return;
        clear(c,slot);
        if(when==0)return;
        long now=System.currentTimeMillis();
        if(!WatchAlarmProtocol.validWhen(when,now))return;
        int duration=Math.max(1,Math.min(15,minutes));
        if(!store(c).edit().putLong(key(slot,"when"),when)
                .putInt(key(slot,"id"),id).putInt(key(slot,"minutes"),duration)
                .putString(key(slot,"name"),name==null?"Sanftwecker":name)
                .putBoolean(key(slot,"cascade"),cascade)
                .putInt(key(slot,"snooze_minutes"),snoozeMinutes).commit())
            return;
        setAlarm(c,slot,when);
    }
    public static synchronized void clear(Context c,int slot){
        if(!WatchAlarmProtocol.validSlot(slot))return;
        AlarmManager m=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(m!=null)m.cancel(receiver(c,slot,0));
        store(c).edit().remove(key(slot,"when")).remove(key(slot,"id"))
                .remove(key(slot,"minutes")).remove(key(slot,"name"))
                .remove(key(slot,"cascade"))
                .remove(key(slot,"snooze_minutes")).commit();
    }
    public static synchronized void fired(Context c,int slot,long when){
        if(!WatchAlarmProtocol.validSlot(slot))return;
        SharedPreferences prefs=store(c);
        if(prefs.getLong(key(slot,"when"),0)!=when)return;
        int id=prefs.getInt(key(slot,"id"),0);
        String name=prefs.getString(key(slot,"name"),"Sanftwecker");
        int minutes=prefs.getInt(key(slot,"minutes"),2);
        boolean cascade=prefs.getBoolean(key(slot,"cascade"),false);
        int snooze=prefs.getInt(key(slot,"snooze_minutes"),0);
        clear(c,slot);
        if(id>0&&id<=1000&&WatchAlarmProtocol.nearTrigger(when,System.currentTimeMillis()))
            WatchAlarmService.start(c,id,when,minutes,name,cascade,snooze);
    }
    public static synchronized void restore(Context c){
        SharedPreferences prefs=store(c);
        for(Map.Entry<String,?> entry:prefs.getAll().entrySet()){
            String key=entry.getKey();
            if(!key.matches("slot_[0-9]{1,4}_when"))continue;
            int slot=Integer.parseInt(key.substring(5,key.length()-5));
            if(!WatchAlarmProtocol.validSlot(slot))continue;
            long when=prefs.getLong(key,0);
            if(WatchAlarmProtocol.validWhen(when,System.currentTimeMillis()))
                setAlarm(c,slot,when);
            else clear(c,slot);
        }
    }
    public static long next(Context c){
        long first=Long.MAX_VALUE,now=System.currentTimeMillis();
        for(Map.Entry<String,?> entry:store(c).getAll().entrySet()){
            if(entry.getKey().matches("slot_[0-9]{1,4}_when")&&entry.getValue() instanceof Long){
                long when=(Long)entry.getValue();
                if(when>now&&when<first)first=when;
            }
        }
        return first==Long.MAX_VALUE?0:first;
    }
}