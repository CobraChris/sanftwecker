package com.cobrachris.sanftwecker;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import java.nio.charset.StandardCharsets;

/**
 * Durable schedules over DataClient plus immediate start/stop over MessageClient.
 * Matching package + original signing certificate are enforced by Google Play services.
 * Phone alarms continue to work even when no matching watch app is installed.
 */
public final class WearAlarmBridge {
    public static final String PREFIX="/sanftwecker/alarm/v1";
    public static final String SCHEDULE=PREFIX+"/schedule/";
    public static final String STATE=PREFIX+"/state";
    public static final String RING=PREFIX+"/ring";
    public static final String STOP=PREFIX+"/stop";
    public static final String ACK=PREFIX+"/ack";
    public static final int TEST_SLOT=1001, SNOOZE_SLOT=1002;
    private WearAlarmBridge() {}

    static String packet(int id,long when,int minutes){
        if(id<1||id>AlarmProfile.MAX_ALARMS||when<=0||
           minutes<1||minutes>30)throw new IllegalArgumentException("Invalid watch alarm");
        return id+":"+when+":"+minutes;
    }

    /** Fail-safe: a disconnected watch does not interfere with phone scheduling. */
    public static void schedule(Context c,int slot,int alarmId,long when){
        if(slot<1||slot>1002||alarmId<1||alarmId>AlarmProfile.MAX_ALARMS)return;
        AlarmProfile p=AlarmRepository.get(c,alarmId);
        boolean enabled=p!=null&&p.watchPulses>0&&when>0;
        try{
            PutDataMapRequest request=PutDataMapRequest.create(SCHEDULE+slot);
            DataMap data=request.getDataMap();
            data.putInt("slot",slot);
            data.putInt("id",alarmId);
            data.putLong("when",enabled?when:0);
            data.putInt("minutes",enabled?Math.min(p.autoStopMinutes,15):0);
            data.putString("name",p==null?"Sanftwecker":p.name);
            data.putLong("revision",System.nanoTime());
            Wearable.getDataClient(c.getApplicationContext())
                    .putDataItem(request.asPutDataRequest().setUrgent())
                    .addOnFailureListener(e->Log.w("Sanftwecker","Watch plan not yet synced",e));
        }catch(RuntimeException e){
            Log.w("Sanftwecker","Watch DataLayer unavailable; phone alarm remains active",e);
        }
    }

    public static void alarmFired(Context c,int id,long when,int minutes){
        if(when<=0||id<1||id>AlarmProfile.MAX_ALARMS)return;
        AlarmProfile p=AlarmRepository.get(c,id);
        if(p==null||p.watchPulses<=0)return;
        send(c,RING,packet(id,when,Math.min(Math.max(minutes,1),15)));
        state(c,id,when,true);
    }

    public static void alarmStopped(Context c,int id,long when){
        if(when<=0||id<1||id>AlarmProfile.MAX_ALARMS)return;
        send(c,STOP,packet(id,when,1));
        state(c,id,when,false);
    }

    private static void state(Context c,int id,long when,boolean ringing){
        try{
            PutDataMapRequest request=PutDataMapRequest.create(STATE);
            DataMap map=request.getDataMap();
            map.putInt("id",id);map.putLong("when",when);
            map.putBoolean("ringing",ringing);
            map.putLong("revision",System.nanoTime());
            Wearable.getDataClient(c.getApplicationContext()).putDataItem(
                    request.asPutDataRequest().setUrgent())
                    .addOnFailureListener(e->Log.w("Sanftwecker","Watch state pending",e));
        }catch(RuntimeException e){Log.w("Sanftwecker","Watch state unavailable",e);}
    }

    private static void send(Context c,String path,String payload){
        try{
            Context app=c.getApplicationContext();
            Wearable.getNodeClient(app).getConnectedNodes().addOnSuccessListener(nodes->{
                for(com.google.android.gms.wearable.Node node:nodes){
                    Wearable.getMessageClient(app).sendMessage(node.getId(),path,
                            payload.getBytes(StandardCharsets.UTF_8))
                            .addOnFailureListener(e->
                                    Log.w("Sanftwecker","Native watch command not delivered",e));
                }
            }).addOnFailureListener(e->Log.w("Sanftwecker","No watch nodes available",e));
        }catch(RuntimeException e){
            Log.w("Sanftwecker","Watch not connected, scheduled local alarm remains",e);
        }
    }
}
