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
        if(PhoneWatchUpdateRelay.ACK.equals(event.getPath())){
            String value=new String(event.getData(),StandardCharsets.UTF_8);
            if(!value.matches("ready:[1-9][0-9]{0,8}"))return;
            long version=Long.parseLong(value.substring("ready:".length()));
            try{
                long phoneVersion=getPackageManager().getPackageInfo(getPackageName(),0)
                    .getLongVersionCode();
                if(version!=phoneVersion)return;
            }catch(Exception ignored){return;}
            getSharedPreferences("sanftwecker_watch_relay",MODE_PRIVATE).edit()
                .putLong("verified_code",version)
                .putLong("verified_at",System.currentTimeMillis()).apply();
            return;
        }
        if(TimerWatchPacket.WATCH_STOP.equals(event.getPath())){
            String stop=new String(event.getData(),StandardCharsets.UTF_8);
            if(!stop.matches("[1-9][0-9]{0,3}:[1-9][0-9]{11,12}"))return;
            String[] p=stop.split(":");
            int id=Integer.parseInt(p[0]);
            long when=Long.parseLong(p[1]);
            // Ignore stale or forged commands for an earlier timer occurrence.
            if(TimerWearBridge.isCurrent(this,id,when)){
                TimerAlerts.dismiss(this,id);
                AlarmRepository.diagnostic(this,"Timer auf Galaxy Watch gestoppt: "+id);
            }
            return;
        }
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
        if(!WatchAcknowledgement.valid(value))
            return;
        AlarmRepository.diagnostic(this,"Wear OS direkt: "+value);
        getSharedPreferences("sanftwecker_watch_native",MODE_PRIVATE).edit()
                .putLong("last_ack",System.currentTimeMillis())
                .putString("last_status",value).apply();
    }
}
