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
        if(!current(p,id,due))return;
        String old=p.getString("watch","");
        // Async Data Layer deliveries can arrive out of phase order.
        if(("STRONG".equals(old) && ("GENTLE".equals(phase)||"STARTED".equals(phase)))||
                ("GENTLE".equals(old)&&"STARTED".equals(phase)))return;
        p.edit().putString("watch",phase).apply();
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
        return "Handy: "+friendly(p.getString("phone","NICHT_BESTAETIGT"))+
                " · Uhr: "+friendly(p.getString("watch","NICHT_BESTAETIGT"));
    }
    private static String friendly(String phase){
        switch(phase){
            case "AUSGELOEST":return "Alarm ausgelöst";
            case "STUFE_1_TON":return "sanfter Ton gestartet";
            case "TON_AKTIV":return "Ton gestartet";
            case "STUFE_2_HANDY":return "starke Vibration gestartet";
            case "TON_FEHLER":return "Tonstart fehlgeschlagen";
            case "START_FEHLER":case "START_GESPERRT":return "Alarmdienst nicht gestartet";
            case "HANDYVIBRATION_BLOCKIERT":return "Handyvibration blockiert";
            case "STARTED":return "Uhrdienst gestartet";
            case "GENTLE":return "sanfte Watch-Vibration ausgelöst";
            case "STRONG":return "starke Watch-Vibration ausgelöst";
            case "NO_VIBRATOR":return "Watch-Motor nicht verfügbar";
            case "START_ERROR":return "Watch-Start fehlgeschlagen";
            case "VIBRATION_ERROR":return "Watch-Vibration fehlgeschlagen";
            case "NUR_GESENDET":return "an Uhr gesendet, noch keine Bestätigung";
            case "NICHT_VERBUNDEN":return "Uhr nicht verbunden";
            case "SEND_ERROR":case "VERBINDUNGSFEHLER":return "Übertragung fehlgeschlagen";
            default:return "keine Ausführungsbestätigung";
        }
    }

}
