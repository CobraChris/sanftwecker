package com.cobrachris.watchvibrationpoc;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.wearable.Wearable;
import java.nio.charset.StandardCharsets;

/** Best-effort diagnostic: proves the watch alarm screen was resumed. */
public final class WatchAlarmScreenAck {
    private WatchAlarmScreenAck(){}
    public static void visible(Context context,int alarmId){
        if(alarmId<1||alarmId>1000)return;
        Context app=context.getApplicationContext();
        byte[] body=("SCREEN_VISIBLE:"+alarmId).getBytes(StandardCharsets.UTF_8);
        try{
            Wearable.getNodeClient(app).getConnectedNodes().addOnSuccessListener(nodes->{
                for(com.google.android.gms.wearable.Node node:nodes)
                    Wearable.getMessageClient(app).sendMessage(node.getId(),
                            WatchAlarmProtocol.ACK,body)
                            .addOnFailureListener(e->Log.w("SanftweckerWatch",
                                    "Alarm screen ACK not delivered",e));
            }).addOnFailureListener(e->Log.w("SanftweckerWatch",
                    "Alarm screen ACK deferred",e));
        }catch(RuntimeException e){
            Log.w("SanftweckerWatch","Alarm screen ACK unavailable",e);
        }
    }
}
