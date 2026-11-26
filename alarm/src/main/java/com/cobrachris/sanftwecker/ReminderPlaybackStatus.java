package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Local-only evidence for the last fired reminder occurrence.
 * An accepted transport message is not proof of actual haptic playback.
 */
public final class ReminderPlaybackStatus {
    private static final String STORE="sanftwecker_reminder_playback_status_v1";
    private ReminderPlaybackStatus(){}

    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(STORE,Context.MODE_PRIVATE);
    }
    public static synchronized void fired(Context c,int id,long due){
        prefs(c).edit().putInt("id",id).putLong("due",due)
                .putString("phone","AUSGELOEST")
                .putString("watch","NICHT_BESTAETIGT").commit();
    }
    private static boolean current(SharedPreferences p,int id,long due){
        return p.getInt("id",0)==id&&p.getLong("due",0L)==due;
    }
    public static synchronized void phone(Context c,int id,long due,String phase){
        SharedPreferences p=prefs(c);
        if(current(p,id,due))p.edit().putString("phone",phase).apply();
    }
    public static synchronized void watch(Context c,int id,long due,String phase){
        SharedPreferences p=prefs(c);
        if(current(p,id,due))p.edit().putString("watch",phase).apply();
    }
    /** Transport acknowledgements must not overwrite a later motor-stage ACK. */
    public static synchronized void watchSent(Context c,int id,long due){
        SharedPreferences p=prefs(c);
        if(current(p,id,due)&&"NICHT_BESTAETIGT".equals(
                p.getString("watch","NICHT_BESTAETIGT")))
            p.edit().putString("watch","NUR_GESENDET").apply();
    }
    public static synchronized String summary(Context c,int id,long due){
        SharedPreferences p=prefs(c);
        if(!current(p,id,due))return "";
        return "Handy: "+p.getString("phone","NICHT_BESTAETIGT")+
                " · Uhr: "+p.getString("watch","NICHT_BESTAETIGT");
    }
}
