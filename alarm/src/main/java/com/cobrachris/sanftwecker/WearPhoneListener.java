package com.cobrachris.sanftwecker;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import java.nio.charset.StandardCharsets;

/** Records actual acknowledgements from the native watch companion. */
public final class WearPhoneListener extends WearableListenerService {
    @Override public void onMessageReceived(MessageEvent event){
        if(!WearAlarmBridge.ACK.equals(event.getPath()))return;
        String value=new String(event.getData(),StandardCharsets.UTF_8);
        if(value.length()>96||!value.matches("(STARTED|STOPPED|NO_VIBRATOR|SCHEDULED):[0-9]{1,4}"))
            return;
        AlarmRepository.diagnostic(this,"Wear OS direkt: "+value);
        getSharedPreferences("sanftwecker_watch_native",MODE_PRIVATE).edit()
                .putLong("last_ack",System.currentTimeMillis())
                .putString("last_status",value).apply();
    }
}
