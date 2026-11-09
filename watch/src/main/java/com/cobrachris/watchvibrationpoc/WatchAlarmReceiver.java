package com.cobrachris.watchvibrationpoc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Dedicated receiver for the watch's own AlarmManager.setAlarmClock event. */
public final class WatchAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        if(intent!=null&&WatchScheduleStore.ACTION_RING.equals(intent.getAction()))
            WatchScheduleStore.fired(c,intent.getIntExtra("slot",-1),
                    intent.getLongExtra("when",0));
    }
}