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
    public static final String ACK="/sanftwecker/reminder/v1/ack";
    private ReminderWatchBridge(){}
    private static void send(Context c,String path,ReminderStore.Item item){
        // Old Watch releases understand seven fields only. The eighth field
        // marks the shared wake-cascade profile for Watch v53+.
        // Keep the legacy packet until both devices can reliably negotiate versions.
        // The actual reference Testwecker owns ALL Watch haptic settings,
        // including previously saved reminders with old cascade=false flags.
        AlarmProfile reference=ReminderAlarmConfig.profile(c);
        String payload=item.id+":"+item.dueAt+":"+(reference.cascadeEnabled?"1":"0")+":"+
            reference.watchIntensity+":"+Math.max(1,reference.watchPulses)+":"+
            reference.watchIntervalSeconds+":"+item.postponeMinutes;
        String shared=payload+":1";
        String detailed=shared+":"+reference.autoStopMinutes;
        try{
            Wearable.getNodeClient(c).getConnectedNodes().addOnSuccessListener(nodes->{
                for(com.google.android.gms.wearable.Node node:nodes){
                    // Prefer the versioned 9-field profile. Legacy watches
                    // receive the old 8- and 7-field packets as compatibility.
                    if(START.equals(path)){
                        Wearable.getMessageClient(c).sendMessage(node.getId(),path,
                            detailed.getBytes(StandardCharsets.UTF_8))
                            .addOnSuccessListener(sent->
                                    AlarmRepository.diagnostic(c,"Erinnerung an Wear OS gesendet (noch nicht bestätigt): "+item.id))
                            .addOnFailureListener(e->{
                                AlarmRepository.diagnostic(c,"FEHLER: Erinnerung nicht an Wear OS übertragen");
                                Log.w("Sanftwecker","Profile-based reminder delivery deferred",e);
                            });
                        Wearable.getMessageClient(c).sendMessage(node.getId(),path,
                            shared.getBytes(StandardCharsets.UTF_8))
                            .addOnFailureListener(e->Log.w("Sanftwecker",
                                    "Compatible reminder delivery deferred",e));
                    }
                    Wearable.getMessageClient(c).sendMessage(node.getId(),path,
                        payload.getBytes(StandardCharsets.UTF_8))
                        .addOnFailureListener(e->Log.w("Sanftwecker","Reminder Watch delivery deferred",e));
                }
                if(nodes.isEmpty())
                    AlarmRepository.diagnostic(c,"Erinnerung: keine verbundene Galaxy Watch gefunden");
            }).addOnFailureListener(error->{
                AlarmRepository.diagnostic(c,"FEHLER: Galaxy-Watch-Verbindung nicht abfragbar");
                Log.w("Sanftwecker","Reminder Watch node query failed",error);
            });
        }catch(RuntimeException e){
            AlarmRepository.diagnostic(c,"FEHLER: Reminder-Watch-Übertragung nicht möglich");
            Log.w("Sanftwecker","Reminder Watch connection unavailable",e);
        }
    }
    public static void start(Context c,ReminderStore.Item i){send(c,START,i);}
    public static void escalate(Context c,ReminderStore.Item i){send(c,ESCALATE,i);}
    public static void stop(Context c,ReminderStore.Item i){send(c,STOP,i);}
}