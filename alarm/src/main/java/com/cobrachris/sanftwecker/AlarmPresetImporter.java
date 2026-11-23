package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;

/**
 * One-time, opt-in import of the visible names and times from the user's
 * November 2026 screenshots. The screenshots do NOT show individual settings:
 * weekdays, sound, vibration and snooze must be reviewed on-device.
 * Every imported alarm is disabled, so an import never changes active schedules.
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
    /** Returns count of newly added, always inactive alarms. */
    public static synchronized int importMissing(Context c){
        if(wasImported(c))return 0;
        int imported=0;
        for(Preset preset:PRESETS){
            if(exists(AlarmRepository.all(c),preset))continue;
            int id=AlarmRepository.create(c,null);
            try {
                AlarmProfile d=AlarmProfile.defaults(id);
                AlarmProfile draft=new AlarmProfile(id,preset.name,false,
                        preset.hour,preset.minute,d.days,d.delaySeconds,d.rampSeconds,
                        d.maxVolume,d.watchPulses,d.watchIntervalSeconds,
                        d.snoozeMinutes,d.tone,d.rampEnabled,d.autoStopMinutes,
                        0L,0L,0L,d.snoozeEnabled,d.vibrationMode,d.audibleWhenMuted,
                        d.cascadeEnabled,d.watchIntensity);
                AlarmRepository.put(c,draft);
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
