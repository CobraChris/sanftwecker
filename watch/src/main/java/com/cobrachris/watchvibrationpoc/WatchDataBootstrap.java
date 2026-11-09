package com.cobrachris.watchvibrationpoc;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

/** First-install/reboot recovery: read existing synchronized data without user clicks. */
public final class WatchDataBootstrap {
    private WatchDataBootstrap(){}
    public static void refresh(Context context){refresh(context,null);}
    public static void refresh(Context context,Runnable completed){
        Context app=context.getApplicationContext();
        try{
            Wearable.getDataClient(app).getDataItems().addOnSuccessListener(buffer->{
                try{
                    for(DataItem item:buffer) apply(app,item);
                }finally{
                    buffer.release();
                    if(completed!=null)completed.run();
                }
            }).addOnFailureListener(e->Log.w("SanftweckerWatch","Plan sync pending",e));
        }catch(RuntimeException e){
            Log.w("SanftweckerWatch","Play Services unavailable; saved local alarms retained",e);
        }
    }
    public static void apply(Context c,DataItem item){
        String path=item.getUri().getPath();
        if(path==null||!path.startsWith(WatchAlarmProtocol.PREFIX+"/"))return;
        try{
            DataMap map=DataMapItem.fromDataItem(item).getDataMap();
            if(path.startsWith(WatchAlarmProtocol.SCHEDULE)){
                String suffix=path.substring(WatchAlarmProtocol.SCHEDULE.length());
                if(!suffix.matches("[0-9]{1,4}"))return;
                int slot=Integer.parseInt(suffix),id=map.getInt("id");
                if(slot!=map.getInt("slot")||id<1||id>1000)return;
                WatchScheduleStore.plan(c,slot,id,map.getLong("when"),
                        map.getString("name"),map.getInt("minutes"));
            }else if(WatchAlarmProtocol.STATE.equals(path)){
                int id=map.getInt("id");
                long when=map.getLong("when");
                if(id<1||id>1000)return;
                if(map.getBoolean("ringing")){
                    if(WatchAlarmProtocol.nearTrigger(when,System.currentTimeMillis()))
                        WatchAlarmService.start(c,id,when,5,"Sanftwecker");
                }else {
                    android.content.Intent stop=new android.content.Intent(
                            c,WatchAlarmService.class).setAction(WatchAlarmService.STOP)
                            .putExtra("id",id).putExtra("when",when);
                    try{c.startService(stop);}catch(RuntimeException ignored){}
                }
            }
        }catch(RuntimeException e){Log.w("SanftweckerWatch","Invalid synchronized alarm",e);}
    }
}