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
        for(DataEvent event:events){
            if(event.getType()!=DataEvent.TYPE_CHANGED)continue;
            String path=event.getDataItem().getUri().getPath();
            if(path==null||!path.startsWith(WatchAlarmProtocol.PREFIX+"/"))continue;
            try{
                DataMap map=DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                if(path.startsWith(WatchAlarmProtocol.SCHEDULE)){
                    String suffix=path.substring(WatchAlarmProtocol.SCHEDULE.length());
                    if(!suffix.matches("[0-9]{1,4}"))continue;
                    int slot=Integer.parseInt(suffix);
                    int id=map.getInt("id"),minutes=map.getInt("minutes");
                    long when=map.getLong("when");
                    if(slot!=map.getInt("slot"))continue;
                    WatchScheduleStore.plan(this,slot,id,when,
                            map.getString("name"),minutes);
                }else if(WatchAlarmProtocol.STATE.equals(path)){
                    int id=map.getInt("id");long when=map.getLong("when");
                    if(id<1||id>1000)continue;
                    if(map.getBoolean("ringing")){
                        if(WatchAlarmProtocol.nearTrigger(when,System.currentTimeMillis()))
                            WatchAlarmService.start(this,id,when,5,"Sanftwecker");
                    }else stop(id,when,false);
                }
            }catch(RuntimeException e){
                Log.w("SanftweckerWatch","Invalid watch DataLayer item",e);
            }
        }
    }
    @Override public void onMessageReceived(MessageEvent event){
        WatchAlarmProtocol.Packet packet=WatchAlarmProtocol.parse(
                new String(event.getData(),StandardCharsets.UTF_8));
        if(packet==null)return;
        if(WatchAlarmProtocol.RING.equals(event.getPath())&&
                WatchAlarmProtocol.nearTrigger(packet.when,System.currentTimeMillis())){
            WatchAlarmService.start(this,packet.id,packet.when,packet.minutes,"Sanftwecker");
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