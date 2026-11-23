package com.cobrachris.sanftwecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** A stale notification must never cancel a different or newer alarm. */
public final class SnoozeActionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        if(intent==null||!SnoozeNotice.CANCEL.equals(intent.getAction()))return;
        int id=intent.getIntExtra(AlarmScheduler.EXTRA_ID,-1);
        long when=intent.getLongExtra(AlarmScheduler.EXTRA_WHEN,0L);
        if(id<=0||when<=0||id!=AlarmScheduler.snoozedId(c)||
                when!=AlarmScheduler.snoozedWhen(c))return;
        AlarmScheduler.cancelSnooze(c);
        AlarmRepository.diagnostic(c,"Schlummeralarm für heute beendet");
    }
}
