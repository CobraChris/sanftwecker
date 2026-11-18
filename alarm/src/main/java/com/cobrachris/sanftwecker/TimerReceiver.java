package com.cobrachris.sanftwecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Exact timer delivery and notification acknowledgement; never starts the wake alarm service. */
public final class TimerReceiver extends BroadcastReceiver {
    public static final String ACTION_EXTEND="com.cobrachris.sanftwecker.TIMER_EXTEND";
    public static final String EXTRA_EXTENSION_MS="timer_extension_ms";
    public static final String ACTION_DISMISS="com.cobrachris.sanftwecker.TIMER_DISMISS";

    @Override public void onReceive(Context c,Intent intent){
        if(intent==null)return;
        int id=intent.getIntExtra(TimerScheduler.EXTRA_ID,-1);
        if(id<1)return;
        if(TimerScheduler.ACTION_FINISH.equals(intent.getAction())){
            TimerScheduler.fired(c,id,intent.getLongExtra(TimerScheduler.EXTRA_DUE,-1));
        }else if(TimerStartSchedule.ACTION_START.equals(intent.getAction())){
            TimerStartSchedule.fired(c,id,
                    intent.getLongExtra(TimerScheduler.EXTRA_DUE,-1));
        }else if(TimerMilestoneScheduler.ACTION_INTERVAL.equals(intent.getAction()) ||
                TimerMilestoneScheduler.ACTION_PREPARE.equals(intent.getAction())){
            TimerMilestoneScheduler.fired(c,id,
                    TimerMilestoneScheduler.ACTION_PREPARE.equals(intent.getAction()),
                    intent.getLongExtra(TimerScheduler.EXTRA_DUE,-1),
                    intent.getLongExtra(TimerMilestoneScheduler.EXTRA_EVENT_AT,-1));
        }else if(ACTION_EXTEND.equals(intent.getAction())){
            long add=intent.getLongExtra(EXTRA_EXTENSION_MS,0);
            long due=intent.getLongExtra(TimerScheduler.EXTRA_DUE,0);
            if(add==30_000L||add==60_000L||add==120_000L||
                    add==300_000L||add==600_000L){
                try{TimerScheduler.extendFinished(c,id,due,add);}
                catch(RuntimeException e){
                    AlarmRepository.diagnostic(c,"Timer-Verlängerung nicht möglich: "+e.getMessage());
                }
            }
        }else if(ACTION_DISMISS.equals(intent.getAction())){
            TimerAlerts.dismiss(c,id);
        }
    }
}
