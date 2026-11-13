package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * UI-only fallback for a foreground alarm on unlocked phones.
 * Never used for AlarmManager scheduling, permission state or auto-starting UI.
 */
public final class ActiveAlarmState {
    private static final String PREF="sanftwecker_active_phone_alarm_v1";
    private static final long MAX_AGE=30L*60_000L;
    private ActiveAlarmState(){}
    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    public static void start(Context c,int id,long when){
        if(id<1||id>AlarmProfile.MAX_ALARMS)return;
        prefs(c).edit()
                .putInt("id",id).putLong("when",when)
                .putLong("started_at",System.currentTimeMillis()).commit();
    }
    public static void clear(Context c){
        prefs(c).edit().clear().commit();
    }
    public static int activeId(Context c){
        SharedPreferences p=prefs(c);
        long since=p.getLong("started_at",0);
        long now=System.currentTimeMillis();
        int id=p.getInt("id",0);
        if(id<=0||id>AlarmProfile.MAX_ALARMS||since<=0||since>now||
                now-since>MAX_AGE)return 0;
        return id;
    }
}
