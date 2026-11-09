package com.cobrachris.watchvibrationpoc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-arm watch alarms independently after reboot, timezone or package upgrade. */
public final class WatchBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        if(intent!=null){
            WatchScheduleStore.restore(c);
            WatchDataBootstrap.refresh(c);
        }
    }
}