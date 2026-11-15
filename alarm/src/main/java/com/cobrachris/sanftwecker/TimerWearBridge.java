package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import java.nio.charset.StandardCharsets;

/**
 * Durable timer completion state plus low-latency watch message.
 * Neither timer nor watch delivery changes the reliable phone alarm.
 */
public final class TimerWearBridge {
    private static final String PREF="sanftwecker_timer_watch_state_v1";
    private TimerWearBridge(){}
    public static void finished(Context c,TimerEntry t,boolean vibrate){
        if(t==null||!TimerWatchPacket.validId(t.id)||t.dueWallMs<=0)return;
        Context app=c.getApplicationContext();
        c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
                .putLong("when_"+t.id,t.dueWallMs).apply();
        put(app,t.id,t.dueWallMs,t.name,vibrate,true);
        message(app,TimerWatchPacket.START,
                TimerWatchPacket.start(t.id,t.dueWallMs,vibrate,t.name));
    }
    public static boolean isCurrent(Context c,int id,long when){
        return TimerWatchPacket.validId(id)&&when>0 &&
                c.getSharedPreferences(PREF,Context.MODE_PRIVATE)
                        .getLong("when_"+id,0)==when;
    }
    public static void stopped(Context c,int id){
        if(!TimerWatchPacket.validId(id))return;
        Context app=c.getApplicationContext();
        SharedPreferences pref=app.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        long when=pref.getLong("when_"+id,0);
        if(when<=0)return;
        // Explicit inactive state survives reconnections, preventing stale restart.
        put(app,id,when,"Timer",false,false);
        message(app,TimerWatchPacket.STOP,TimerWatchPacket.stop(id,when));
        pref.edit().remove("when_"+id).apply();
    }
    private static void put(Context c,int id,long when,String name,
            boolean vibrate,boolean active){
        try{
            PutDataMapRequest request=PutDataMapRequest.create(TimerWatchPacket.STATE+id);
            DataMap m=request.getDataMap();
            m.putInt("id",id);
            m.putLong("when",when);
            m.putString("name",name);
            m.putBoolean("vibrate",vibrate);
            m.putBoolean("active",active);
            m.putLong("revision",System.nanoTime());
            Wearable.getDataClient(c).putDataItem(request.asPutDataRequest().setUrgent())
                    .addOnFailureListener(e->Log.w("Sanftwecker",
                            "Watch timer state sync deferred",e));
        }catch(RuntimeException ex){
            Log.w("Sanftwecker","Timer watch state unavailable",ex);
        }
    }
    private static void message(Context c,String path,String payload){
        try{
            Wearable.getNodeClient(c).getConnectedNodes().addOnSuccessListener(nodes->{
                for(com.google.android.gms.wearable.Node node:nodes)
                    Wearable.getMessageClient(c).sendMessage(node.getId(),path,
                            payload.getBytes(StandardCharsets.UTF_8))
                            .addOnFailureListener(e->Log.w("Sanftwecker",
                                    "Watch timer message not delivered",e));
            }).addOnFailureListener(e->Log.w("Sanftwecker","No connected timer watch",e));
        }catch(RuntimeException e){
            Log.w("Sanftwecker","Timer watch connection unavailable",e);
        }
    }
}
