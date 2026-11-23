package com.cobrachris.sanftwecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.Calendar;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        if(intent==null)return;
        String action=intent.getAction();
        int id=intent.getIntExtra(ReminderScheduler.ID,-1);
        long due=intent.getLongExtra(ReminderScheduler.DUE,0L);
        ReminderStore.Item item=ReminderStore.get(c,id);
        if(item==null||item.done||due!=item.dueAt)return;
        if(ReminderScheduler.FIRE.equals(action)){
            ReminderNotice.show(c,item,!item.cascade,true);
            ReminderWatchBridge.start(c,item);
            if(!item.cascade||item.unifiedCascade)ReminderToneService.start(c,item);
            if(item.cascade){
                try{ReminderScheduler.scheduleEscalation(c,item);}
                catch(RuntimeException e){Log.w("Sanftwecker","Reminder escalation unavailable",e);}
            }
        }else if(ReminderScheduler.ESCALATE.equals(action)){
            ReminderNotice.show(c,item,true);
            ReminderWatchBridge.escalate(c,item);
            ReminderToneService.start(c,item);
            if(item.unifiedCascade)PhoneVibration.startReminder(c,
                    WakeCascade.phoneMode(0),item.id,item.dueAt);
        }else if(ReminderScheduler.DONE.equals(action)){
            ReminderStore.complete(c,id);
            ReminderScheduler.cancel(c,id);
            ReminderNotice.clear(c,id);
            ReminderWatchBridge.stop(c,item);
            ReminderToneService.stop(c,item);
            PhoneVibration.stopReminder(c,item.id,item.dueAt);
            // Completing a recurring occurrence arms the next independent one.
            ReminderStore.Item next=ReminderStore.get(c,id);
            if(next!=null&&!next.done){
                try{ReminderScheduler.schedule(c,id);}
                catch(RuntimeException e){Log.e("Sanftwecker","Recurring reminder not scheduled",e);}
            }
        }else if(ReminderScheduler.CHOICE.equals(action)){
            int delay=intent.getIntExtra(ReminderScheduler.DELAY,0);
            if(delay!=10&&delay!=30&&delay!=60&&delay!=-1)return;
            long next;
            if(delay==-1){
                // Tomorrow, at the original local reminder clock time.
                Calendar tomorrow=Calendar.getInstance();
                Calendar original=Calendar.getInstance();
                original.setTimeInMillis(item.anchorAt);
                tomorrow.add(Calendar.DAY_OF_YEAR,1);
                tomorrow.set(Calendar.HOUR_OF_DAY,original.get(Calendar.HOUR_OF_DAY));
                tomorrow.set(Calendar.MINUTE,original.get(Calendar.MINUTE));
                tomorrow.set(Calendar.SECOND,0);
                tomorrow.set(Calendar.MILLISECOND,0);
                next=tomorrow.getTimeInMillis();
            }else next=System.currentTimeMillis()+delay*60_000L;
            if(!AlarmScheduler.canSchedule(c))return;
            ReminderScheduler.cancel(c,id);
            ReminderNotice.clear(c,id);
            ReminderWatchBridge.stop(c,item);
            ReminderToneService.stop(c,item);
            PhoneVibration.stopReminder(c,item.id,item.dueAt);
            ReminderStore.postpone(c,id,next);
            try{ReminderScheduler.schedule(c,id);}
            catch(RuntimeException error){
                Log.e("Sanftwecker","Snoozed reminder could not be scheduled",error);
                ReminderNotice.show(c,ReminderStore.get(c,id),false);
            }
        }else if(ReminderScheduler.LATER.equals(action)){
            ReminderScheduler.cancel(c,id);
            ReminderNotice.clear(c,id);
            ReminderWatchBridge.stop(c,item);
            ReminderToneService.stop(c,item);
            PhoneVibration.stopReminder(c,item.id,item.dueAt);
            ReminderStore.postpone(c,id,System.currentTimeMillis()+item.postponeMinutes*60_000L);
            try{ReminderScheduler.schedule(c,id);}
            catch(RuntimeException e){Log.w("Sanftwecker","Reminder postpone needs exact-alarm access",e);}
        }else if(ReminderScheduler.STOP.equals(action)){
            // Stopping output is not completing the reminder.
            ReminderScheduler.cancel(c,id);
            ReminderNotice.show(c,item,false);
            ReminderWatchBridge.stop(c,item);
            ReminderToneService.stop(c,item);
            PhoneVibration.stopReminder(c,item.id,item.dueAt);
        }
    }
}