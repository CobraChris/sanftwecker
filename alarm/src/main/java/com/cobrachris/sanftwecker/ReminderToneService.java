package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Occurrence-safe adapter only. There is NO reminder-specific audio engine:
 * the actual Testwecker RingingService synthesizes and controls every reminder.
 */
public final class ReminderToneService {
    private ReminderToneService(){}

    public static void start(Context c,ReminderStore.Item item){
        if(item==null||item.done)return;
        Intent intent=new Intent(c,RingingService.class)
                .setAction(RingingService.REMINDER_START)
                .putExtra(ReminderScheduler.ID,item.id)
                .putExtra(ReminderScheduler.DUE,item.dueAt);
        try{
            AlarmRepository.diagnostic(c,"Erinnerungs-Weckerdienst angefordert: "+
                    item.title+" / "+item.id);
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
                c.startForegroundService(intent);
            else c.startService(intent);
        }catch(RuntimeException failure){
            // The notification must still offer its time-sensitive screen if
            // the foreground service was refused by the system.
            ReminderPlaybackStatus.phone(c,item.id,item.dueAt,"START_GESPERRT");
            AlarmRepository.diagnostic(c,
                    "FEHLER: Weckerdienst für Erinnerung nicht gestartet: "+
                    failure.getClass().getSimpleName());
            ReminderNotice.show(c,item,false,true);
            Log.e("Sanftwecker","Shared alarm service unavailable for reminder",failure);
        }
    }

    /** Never stop a true wake alarm or a different reminder occurrence. */
    public static void stop(Context c,ReminderStore.Item item){
        if(item==null)return;
        Intent intent=new Intent(c,RingingService.class)
                .setAction(RingingService.REMINDER_STOP)
                .putExtra(ReminderScheduler.ID,item.id)
                .putExtra(ReminderScheduler.DUE,item.dueAt);
        try{c.startService(intent);}
        catch(RuntimeException failure){
            AlarmRepository.diagnostic(c,
                    "WARNUNG: Erinnerungsstopp nicht an Audiodienst übergeben");
            Log.w("Sanftwecker","Shared reminder stop deferred",failure);
        }
    }
}
