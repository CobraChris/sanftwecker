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
        if("/sanftwecker/update/v1/version-response".equals(event.getPath())){
            String reply=new String(event.getData(),StandardCharsets.UTF_8);
            if(!reply.matches("installed:[1-9][0-9]{0,8}"))return;
            long watchCode=Long.parseLong(reply.substring("installed:".length()));
            getSharedPreferences("sanftwecker_watch_relay",MODE_PRIVATE).edit()
                .putLong("installed_watch_code",watchCode)
                .putLong("installed_watch_checked",System.currentTimeMillis()).apply();
            return;
        }
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
        if(ReminderWatchBridge.ACK.equals(event.getPath())){
            String receipt=new String(event.getData(),StandardCharsets.UTF_8);
            if(!receipt.matches("[1-9][0-9]{0,3}:[1-9][0-9]{11,12}:"+
                    "(STARTED|GENTLE|STRONG|START_ERROR|NO_VIBRATOR|VIBRATION_ERROR)"))
                return;
            String[] parts=receipt.split(":");
            int id=Integer.parseInt(parts[0]);
            long due=Long.parseLong(parts[1]);
            ReminderStore.Item reminder=ReminderStore.get(this,id);
            // Do not display stale Watch output from a previous occurrence.
            if(reminder==null||reminder.done||reminder.dueAt!=due)return;
            String phase=parts[2];
            AlarmRepository.diagnostic(this,"Erinnerung Uhr bestätigt: "+phase+
                    " (ID "+id+")");
            getSharedPreferences("sanftwecker_reminder_watch_acks_v1",MODE_PRIVATE)
                    .edit().putInt("id",id).putLong("due",due)
                    .putString("phase",phase).putLong("at",System.currentTimeMillis())
                    .apply();
            return;
        }
        if(ReminderWatchBridge.ACTION.equals(event.getPath())){
            String value=new String(event.getData(),StandardCharsets.UTF_8);
            if(!value.matches("[1-9][0-9]{0,3}:[1-9][0-9]{11,12}:(done|later|stop|later10|later30|later60|tomorrow)"))
                return;
            String[] parts=value.split(":");
            int id=Integer.parseInt(parts[0]);
            long due=Long.parseLong(parts[1]);
            ReminderStore.Item reminder=ReminderStore.get(this,id);
            if(reminder==null||reminder.done||reminder.dueAt!=due)return;
            String choice=parts[2];
            String action="done".equals(choice)?ReminderScheduler.DONE:
                "later".equals(choice)?ReminderScheduler.LATER:
                "stop".equals(choice)?ReminderScheduler.STOP:ReminderScheduler.CHOICE;
            Intent request=new Intent(this,ReminderReceiver.class).setAction(action)
                .putExtra(ReminderScheduler.ID,id).putExtra(ReminderScheduler.DUE,due);
            if(ReminderScheduler.CHOICE.equals(action)){
                int delay="tomorrow".equals(choice)?-1:
                    Integer.parseInt(choice.substring("later".length()));
                request.putExtra(ReminderScheduler.DELAY,delay);
            }
            sendBroadcast(request);
            return;
        }
        if(TimerWatchPacket.WATCH_EXTEND.equals(event.getPath())){
            String text=new String(event.getData(),StandardCharsets.UTF_8);
            if(!text.matches("[1-9][0-9]{0,3}:[1-9][0-9]{11,12}:(30000|60000|120000|300000|600000)"))
                return;
            String[] parts=text.split(":");
            int id=Integer.parseInt(parts[0]);
            long due=Long.parseLong(parts[1]);
            long extra=Long.parseLong(parts[2]);
            boolean applied=false;
            if(TimerWearBridge.isCurrent(this,id,due)){
                try{
                    applied=TimerScheduler.extendFinished(this,id,due,extra);
                }catch(RuntimeException e){
                    AlarmRepository.diagnostic(this,
                        "Uhr-Verlängerung nicht möglich: "+e.getMessage());
                }
            }
            // ACK is sent only after the phone has actually applied the
            // scheduled extension. A queued message is not an ACK.
            String receipt=id+":"+due+":"+extra+":"+(applied?"ok":"rejected");
            com.google.android.gms.wearable.Wearable.getMessageClient(this)
                .sendMessage(event.getSourceNodeId(),
                    TimerWatchPacket.EXTEND_RESULT,
                    receipt.getBytes(StandardCharsets.UTF_8))
                .addOnFailureListener(error->Log.w("Sanftwecker",
                    "Timer extension receipt not delivered",error));
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
