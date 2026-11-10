package com.cobrachris.sanftwecker;

import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import java.nio.charset.StandardCharsets;

/** Records actual acknowledgements from the native watch companion. */
public final class WearPhoneListener extends WearableListenerService {
    @Override public void onMessageReceived(MessageEvent event){
        boolean watchStop=WearAlarmBridge.STOP.equals(event.getPath());
        boolean watchSnooze=WearAlarmBridge.SNOOZE.equals(event.getPath());
        if(watchStop||watchSnooze){
            String text=new String(event.getData(),StandardCharsets.UTF_8);
            if(!text.matches("[1-9][0-9]{0,3}:[1-9][0-9]{11,12}:1"))return;
            String[] fields=text.split(":");
            int id=Integer.parseInt(fields[0]);
            long when=Long.parseLong(fields[1]);
            // An outdated watch must never turn a disabled snooze into a stop.
            AlarmProfile profile=AlarmRepository.get(this,id);
            if(watchSnooze&&(profile==null||!profile.snoozeEnabled))return;
            AlarmRepository.diagnostic(this,"Uhr: "+
                    (watchSnooze?"Schlummern":"Stoppen")+" angefordert für Alarm "+id);
            Intent action=new Intent(this,RingingService.class)
                    .setAction(watchSnooze?RingingService.SNOOZE:RingingService.STOP)
                    .putExtra(AlarmScheduler.EXTRA_ID,id)
                    .putExtra(AlarmScheduler.EXTRA_WHEN,when);
            try{
                if(Build.VERSION.SDK_INT>=26)startForegroundService(action);
                else startService(action);
            }catch(RuntimeException ex){Log.w("Sanftwecker",
                    "Watch alarm action deferred",ex);}
            return;
        }
        if(!WearAlarmBridge.ACK.equals(event.getPath()))return;
        String value=new String(event.getData(),StandardCharsets.UTF_8);
        if(value.length()>96||!value.matches("(STARTED|STOPPED|NO_VIBRATOR|SCHEDULED):[0-9]{1,4}"))
            return;
        AlarmRepository.diagnostic(this,"Wear OS direkt: "+value);
        getSharedPreferences("sanftwecker_watch_native",MODE_PRIVATE).edit()
                .putLong("last_ack",System.currentTimeMillis())
                .putString("last_status",value).apply();
    }
}
