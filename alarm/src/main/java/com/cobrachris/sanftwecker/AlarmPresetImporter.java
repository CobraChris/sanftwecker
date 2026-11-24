package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;
import java.util.ArrayList;

/**
 * One-time, opt-in import of the visible names and times from the user's
 * November 2026 screenshots. All personal settings come from the saved
 * "Testwecker" profile, rather than new-alarm defaults.
 * Every imported alarm stays disabled, so no active schedule changes.
 */
public final class AlarmPresetImporter {
    private static final String PREF="sanftwecker_screenshot_import_v1";
    public static final class Preset {
        public final String name;
        public final int hour,minute;
        Preset(String name,int hour,int minute){
            this.name=name;this.hour=hour;this.minute=minute;
        }
    }
    public static final Preset[] PRESETS={
        new Preset("6:45 Uhr bei Gero los",5,30),
        new Preset("7 Uhr bei Gero los",5,45),
        new Preset("In Oldenburg - 9 Uhr",6,15),
        new Preset("In Oldenburg 9:15 Uhr",6,35),
        new Preset("Kontingent",6,50),
        new Preset("Office mit Bus - 9 Uhr Bhw1",7,10),
        new Preset("Office mit Auto - 9 Uhr Bhw1",7,15),
        new Preset("Home Office - 9 Uhr Start",7,45),
        new Preset("Kontingent",8,1),
        new Preset("Wochenende (Vibration)",10,30),
        new Preset("Parkschein neu",14,45),
        new Preset("Auto umparken",20,40)
    };
    private AlarmPresetImporter(){}
    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    public static boolean wasImported(Context c){
        return prefs(c).getBoolean("completed",false);
    }
    private static boolean exists(List<AlarmProfile> list,Preset preset){
        for(AlarmProfile p:list)
            if(p.name.equals(preset.name)&&p.hour==preset.hour&&p.minute==preset.minute)
                return true;
        return false;
    }
    /** Look up the ACTUAL saved settings, never guess them from a screenshot. */
    public static AlarmProfile referenceAlarm(Context c){
        AlarmProfile match=null;
        for(AlarmProfile alarm:AlarmRepository.all(c)){
            if(!"Testwecker".equals(alarm.name))continue;
            if(match!=null)
                throw new IllegalStateException("Mehrere Testwecker vorhanden – Vorlage nicht eindeutig");
            match=alarm;
        }
        if(match==null)throw new IllegalStateException(
                "Testwecker nicht gefunden – zuerst einen Wecker mit diesem Namen speichern");
        return match;
    }

    private static boolean matchesPreset(AlarmProfile alarm){
        for(Preset preset:PRESETS)
            if(alarm.name.equals(preset.name)&&alarm.hour==preset.hour
                    &&alarm.minute==preset.minute)return true;
        return false;
    }

    /**
     * A one-tap repair for the v52/v53 import, which mistakenly used defaults.
     * Only precisely matching, disabled screenshot entries are eligible.
     * Activated or renamed/rescheduled alarms are never silently rewritten.
     */
    public static synchronized List<AlarmProfile> repairCandidates(Context c){
        List<AlarmProfile> matches=new ArrayList<>();
        if(!wasImported(c))return matches;
        AlarmProfile reference=referenceAlarm(c);
        for(AlarmProfile alarm:AlarmRepository.all(c)){
            if(!alarm.enabled&&matchesPreset(alarm) &&
                    !alarm.sameConfiguration(alarm.withSettingsFrom(reference)))
                matches.add(alarm);
        }
        return matches;
    }

    /** Repairs existing presets without changing their identity, clock or enabled state. */
    public static synchronized int applyTestweckerSettings(Context c){
        if(!wasImported(c))throw new IllegalStateException("Screenshot-Import nicht vorhanden");
        AlarmProfile reference=referenceAlarm(c);
        List<AlarmProfile> candidates=repairCandidates(c);
        List<AlarmProfile> changed=new ArrayList<>();
        try{
            for(AlarmProfile before:candidates){
                AlarmProfile current=AlarmRepository.get(c,before.id);
                if(current==null||current.enabled||!matchesPreset(current))continue;
                // The user may have changed an entry since the preview.
                if(!current.sameConfiguration(before))continue;
                AlarmRepository.put(c,current.withSettingsFrom(reference));
                changed.add(before);
            }
            return changed.size();
        }catch(RuntimeException failure){
            // Revert any already written entry if a later write fails.
            for(int i=changed.size()-1;i>=0;i--){
                try{AlarmRepository.put(c,changed.get(i));}
                catch(RuntimeException rollbackFailed){
                    throw new IllegalStateException(
                            "Teilweise Reparatur – einige Wecker bitte prüfen",rollbackFailed);
                }
            }
            throw failure;
        }
    }

    /** Returns count of newly added, always inactive alarms. */
    public static synchronized int importMissing(Context c){
        if(wasImported(c))return 0;
        AlarmProfile reference=referenceAlarm(c);
        int imported=0;
        for(Preset preset:PRESETS){
            if(exists(AlarmRepository.all(c),preset))continue;
            int id=AlarmRepository.create(c,null);
            try {
                AlarmProfile d=AlarmProfile.defaults(id);
                AlarmProfile identity=new AlarmProfile(id,preset.name,false,
                        preset.hour,preset.minute,d.days,d.delaySeconds,d.rampSeconds,
                        d.maxVolume,d.watchPulses,d.watchIntervalSeconds,
                        d.snoozeMinutes,d.tone,d.rampEnabled,d.autoStopMinutes,
                        0L,0L,0L,d.snoozeEnabled,d.vibrationMode,d.audibleWhenMuted,
                        d.cascadeEnabled,d.watchIntensity);
                AlarmRepository.put(c,identity.withSettingsFrom(reference));
                imported++;
            }catch(RuntimeException failed){
                AlarmRepository.delete(c,id);
                throw failed;
            }
        }
        if(!prefs(c).edit().putBoolean("completed",true).commit())
            throw new IllegalStateException("Importstatus konnte nicht gespeichert werden");
        return imported;
    }
}
