package com.cobrachris.sanftwecker;

import android.content.Context;

/**
 * One authoritative audio / haptic configuration for reminders:
 * the actual, persisted "Testwecker" alarm (even when disabled).
 *
 * Reminder dates, actions and identifiers are deliberately independent.
 * Legacy per-reminder sound/cascade fields are not used for playback.
 */
public final class ReminderAlarmConfig {
    private ReminderAlarmConfig(){}

    public static AlarmProfile profile(Context context){
        try{
            return AlarmPresetImporter.referenceAlarm(context);
        }catch(IllegalStateException missing){
            AlarmRepository.diagnostic(context,
                    "WARNUNG: Testwecker-Vorlage fehlt oder ist mehrdeutig; Ersatzprofil aktiv");
            return AlarmProfile.defaults(1);
        }
    }
}
