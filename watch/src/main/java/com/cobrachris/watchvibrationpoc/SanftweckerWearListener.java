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
            if(event.getType()==DataEvent.TYPE_CHANGED)
                WatchDataBootstrap.apply(this,event.getDataItem());
    }
    @Override public void onMessageReceived(MessageEvent event){
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