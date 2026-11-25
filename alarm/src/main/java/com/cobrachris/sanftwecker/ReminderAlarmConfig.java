package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * One authoritative output configuration for every reminder.
 * Auto-link by saved "Testwecker" name, then retain its stable alarm ID
 * even if the user later renames it. Never take playback settings from an
 * old reminder's tone, cascade, mute or Watch fields.
 */
public final class ReminderAlarmConfig {
    private static final String PREF="sanftwecker_reminder_testwecker_link_v1";
    private ReminderAlarmConfig(){}

    private static SharedPreferences prefs(Context context){
        return context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }

    /** Returns the actual saved alarm, or null when it was deleted/ambiguous. */
    public static AlarmProfile linked(Context context){
        try{
            AlarmProfile exact=AlarmPresetImporter.referenceAlarm(context);
            prefs(context).edit().putInt("alarm_id",exact.id).apply();
            return exact;
        }catch(IllegalStateException missingOrAmbiguous){
            int id=prefs(context).getInt("alarm_id",0);
            return id>0?AlarmRepository.get(context,id):null;
        }
    }

    public static AlarmProfile profile(Context context){
        AlarmProfile linked=linked(context);
        if(linked!=null)return linked;
        AlarmRepository.diagnostic(context,
                "WARNUNG: Testwecker nicht verknüpft – Ersatzprofil aktiv; bitte Vorlage prüfen");
        return AlarmProfile.defaults(1);
    }
}
