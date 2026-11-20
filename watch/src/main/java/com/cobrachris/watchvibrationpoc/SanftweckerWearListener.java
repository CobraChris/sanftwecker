package com.cobrachris.watchvibrationpoc;

import android.content.Intent;
import android.util.Log;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import java.nio.charset.StandardCharsets;

/** Data Layer receiver independent of phone notification mirroring. */
public final class SanftweckerWearListener extends WearableListenerService {
    @Override public void onDataChanged(DataEventBuffer events){
        for(DataEvent event:events)
            if(event.getType()==DataEvent.TYPE_CHANGED){
                if(PhoneDeliveredWatchUpdate.PATH.equals(event.getDataItem().getUri().getPath())){
                    DataMap update=DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    PhoneDeliveredWatchUpdate.receive(getApplicationContext(),
                        update.getAsset("apk"),update.getLong("version_code"),
                        update.getString("sha256"));
                }else WatchDataBootstrap.apply(this,event.getDataItem());
            }
    }
    @Override public void onMessageReceived(MessageEvent event){
        String path=event.getPath();
        if("/sanftwecker/update/v1/version-request".equals(path)){
            try{
                long installed=getPackageManager().getPackageInfo(getPackageName(),0)
                        .getLongVersionCode();
                byte[] payload=("installed:"+installed).getBytes(StandardCharsets.UTF_8);
                com.google.android.gms.wearable.Wearable.getMessageClient(this)
                        .sendMessage(event.getSourceNodeId(),
                            "/sanftwecker/update/v1/version-response",payload);
            }catch(Exception error){
                Log.w("SanftweckerWatch","Version reply failed",error);
            }
            return;
        }
        if(WatchTimerProtocol.EXTEND_RESULT.equals(path)){
            WatchTimerExtensionFeedback.receive(this,event.getData());
            return;
        }
        if(WatchTimerProtocol.START.equals(path)){
            WatchTimerProtocol.Packet timer=WatchTimerProtocol.start(
                    new String(event.getData(),StandardCharsets.UTF_8));
            WatchTimerAlertService.start(this,timer);
            return;
        }
        if(WatchTimerProtocol.STOP.equals(path)){
            WatchTimerProtocol.Packet timer=WatchTimerProtocol.stop(
                    new String(event.getData(),StandardCharsets.UTF_8));
            if(timer!=null)WatchTimerAlertService.stop(this,timer.id,timer.when);
            return;
        }
        WatchAlarmProtocol.Packet packet=WatchAlarmProtocol.parse(
                new String(event.getData(),StandardCharsets.UTF_8));
        if(packet==null)return;
        if(WatchAlarmProtocol.RING.equals(event.getPath())&&
                WatchAlarmProtocol.nearTrigger(packet.when,System.currentTimeMillis())){
            WatchAlarmService.start(this,packet.id,packet.when,packet.minutes,
                    "Sanftwecker",packet.cascade,packet.snoozeMinutes,packet.intensity,
                    packet.pulseCount,packet.pulseIntervalSeconds);
        }else if(WatchAlarmProtocol.STOP.equals(event.getPath()))
            stop(packet.id,packet.when,false);
    }
    private void stop(int id,long when,boolean fromUser){
        Intent i=new Intent(this,WatchAlarmService.class).setAction(WatchAlarmService.STOP)
                .putExtra("id",id).putExtra("when",when).putExtra("user",fromUser);
        try{startService(i);}
        catch(RuntimeException ex){
            Log.w("SanftweckerWatch","Stop waiting for active haptic service",ex);
        }
    }
}