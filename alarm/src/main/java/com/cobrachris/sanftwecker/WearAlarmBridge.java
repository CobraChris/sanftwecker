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
    public static final String SNOOZE=PREFIX+"/snooze";
    public static final String ACK=PREFIX+"/ack";
    public static final int TEST_SLOT=1001, SNOOZE_SLOT=1002;
    private WearAlarmBridge() {}

    static String packet(int id,long when,int minutes){
        if(id<1||id>AlarmProfile.MAX_ALARMS||when<=0||
           minutes<1||minutes>30)throw new IllegalArgumentException("Invalid watch alarm");
        return id+":"+when+":"+minutes;
    }

    static String packet(int id,long when,int minutes,boolean cascade){
        return packet(id,when,minutes)+(cascade?":1":"");
    }
    static String packet(int id,long when,int minutes,boolean cascade,int snoozeMinutes){
        if(snoozeMinutes<0||snoozeMinutes>30)
            throw new IllegalArgumentException("Invalid snooze minutes");
        return packet(id,when,minutes)+":"+(cascade?1:0)+":"+snoozeMinutes;
    }
    /** Extended v28 packet. Old 5-field packets still decode as Normal on updated watches. */
    static String packet(int id,long when,int minutes,boolean cascade,
            int snoozeMinutes,int watchIntensity){
        if(!WatchIntensity.valid(watchIntensity))
            throw new IllegalArgumentException("Invalid watch intensity");
        return packet(id,when,minutes,cascade,snoozeMinutes)+":"+watchIntensity;
    }
    /** New 8-field format; old 3–6 field packets remain accepted by updated Watch. */
    static String packet(int id,long when,int minutes,boolean cascade,
            int snoozeMinutes,int intensity,int count,int seconds){
        if(count<0||count>12||!WatchIntervals.isSupported(seconds))
            throw new IllegalArgumentException("Invalid watch pulses");
        return packet(id,when,minutes,cascade,snoozeMinutes,intensity)
                +":"+count+":"+seconds;
    }
    /** Fail-safe: a disconnected watch does not interfere with phone scheduling. */
    public static void schedule(Context c,int slot,int alarmId,long when){
        if(slot<1||slot>1002||alarmId<1||alarmId>AlarmProfile.MAX_ALARMS)return;
        AlarmProfile p=AlarmRepository.get(c,alarmId);
        boolean enabled=p!=null&&(p.watchPulses>0||p.cascadeEnabled)&&when>0;
        try{
            PutDataMapRequest request=PutDataMapRequest.create(SCHEDULE+slot);
            DataMap data=request.getDataMap();
            data.putInt("slot",slot);
            data.putInt("id",alarmId);
            data.putLong("when",enabled?when:0);
            data.putInt("minutes",enabled?
                    Math.min(Math.max(p.autoStopMinutes,p.cascadeEnabled?2:1),15):0);
            data.putBoolean("cascade",enabled&&p.cascadeEnabled);
            data.putInt("snooze_minutes",enabled&&p.snoozeEnabled?p.snoozeMinutes:0);
            data.putInt("watch_intensity",p==null?WatchIntensity.NORMAL:p.watchIntensity);
            data.putInt("watch_pulses",p==null?6:p.watchPulses);
            data.putInt("watch_interval_seconds",p==null?5:p.watchIntervalSeconds);
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
        if(p==null||(p.watchPulses<=0&&!p.cascadeEnabled))return;
        int duration=Math.min(Math.max(minutes,p.cascadeEnabled?2:1),15);
        int snooze=p.snoozeEnabled?p.snoozeMinutes:0;
        String payload=packet(id,when,duration,p.cascadeEnabled,snooze,
                p.watchIntensity,p.watchPulses,p.watchIntervalSeconds);
        send(c,RING,payload);
        state(c,id,when,true,p.cascadeEnabled,duration,snooze,p.watchIntensity,
                p.watchPulses,p.watchIntervalSeconds);
    }

    public static void alarmStopped(Context c,int id,long when){
        if(when<=0||id<1||id>AlarmProfile.MAX_ALARMS)return;
        send(c,STOP,packet(id,when,1));
        state(c,id,when,false,false,1,0,WatchIntensity.NORMAL,0,5);
    }

    private static void state(Context c,int id,long when,boolean ringing,
            boolean cascade,int minutes,int snoozeMinutes,int watchIntensity,
            int pulseCount,int intervalSeconds){
        try{
            PutDataMapRequest request=PutDataMapRequest.create(STATE);
            DataMap map=request.getDataMap();
            map.putInt("id",id);map.putLong("when",when);
            map.putBoolean("ringing",ringing);
            map.putBoolean("cascade",cascade);
            map.putInt("minutes",minutes);
            map.putInt("snooze_minutes",snoozeMinutes);
            map.putInt("watch_intensity",watchIntensity);
            map.putInt("watch_pulses",pulseCount);
            map.putInt("watch_interval_seconds",intervalSeconds);
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
