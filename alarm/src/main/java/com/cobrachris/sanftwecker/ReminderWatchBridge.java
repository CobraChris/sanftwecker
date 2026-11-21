package com.cobrachris.sanftwecker;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.wearable.Wearable;
import java.nio.charset.StandardCharsets;

/** Independent reminder messages, never masquerading as wake alarms or timers. */
public final class ReminderWatchBridge {
    public static final String START="/sanftwecker/reminder/v1/start";
    public static final String ESCALATE="/sanftwecker/reminder/v1/escalate";
    public static final String STOP="/sanftwecker/reminder/v1/stop";
    public static final String ACTION="/sanftwecker/reminder/v1/action";
    private ReminderWatchBridge(){}
    private static void send(Context c,String path,ReminderStore.Item item){
        String payload=item.id+":"+item.dueAt+":"+(item.cascade?"1":"0")+":"+
            item.watchIntensity+":"+item.watchPulses+":"+item.watchIntervalSeconds;
        try{
            Wearable.getNodeClient(c).getConnectedNodes().addOnSuccessListener(nodes->{
                for(com.google.android.gms.wearable.Node node:nodes){
                    Wearable.getMessageClient(c).sendMessage(node.getId(),path,
                        payload.getBytes(StandardCharsets.UTF_8))
                        .addOnFailureListener(e->Log.w("Sanftwecker","Reminder Watch delivery deferred",e));
                }
            });
        }catch(RuntimeException e){
            Log.w("Sanftwecker","Reminder Watch connection unavailable",e);
        }
    }
    public static void start(Context c,ReminderStore.Item i){send(c,START,i);}
    public static void escalate(Context c,ReminderStore.Item i){send(c,ESCALATE,i);}
    public static void stop(Context c,ReminderStore.Item i){send(c,STOP,i);}
}