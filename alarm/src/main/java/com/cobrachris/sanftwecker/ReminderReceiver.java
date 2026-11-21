package com.cobrachris.sanftwecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        if(intent==null)return;
        String action=intent.getAction();
        int id=intent.getIntExtra(ReminderScheduler.ID,-1);
        long due=intent.getLongExtra(ReminderScheduler.DUE,0L);
        ReminderStore.Item item=ReminderStore.get(c,id);
        if(item==null||item.done||due!=item.dueAt)return;
        if(ReminderScheduler.FIRE.equals(action)){
            ReminderNotice.show(c,item,!item.cascade);
            ReminderWatchBridge.start(c,item);
            if(item.cascade){
                try{ReminderScheduler.scheduleEscalation(c,item);}
                catch(RuntimeException e){Log.w("Sanftwecker","Reminder escalation unavailable",e);}
            }
        }else if(ReminderScheduler.ESCALATE.equals(action)){
            ReminderNotice.show(c,item,true);
            ReminderWatchBridge.escalate(c,item);
        }else if(ReminderScheduler.DONE.equals(action)){
            ReminderStore.complete(c,id);
            ReminderScheduler.cancel(c,id);
            ReminderNotice.clear(c,id);
            ReminderWatchBridge.stop(c,item);
        }else if(ReminderScheduler.LATER.equals(action)){
            ReminderScheduler.cancel(c,id);
            ReminderNotice.clear(c,id);
            ReminderWatchBridge.stop(c,item);
            ReminderStore.postpone(c,id,System.currentTimeMillis()+600_000L);
            try{ReminderScheduler.schedule(c,id);}
            catch(RuntimeException e){Log.w("Sanftwecker","Reminder postpone needs exact-alarm access",e);}
        }else if(ReminderScheduler.STOP.equals(action)){
            // Stopping output is not completing the reminder.
            ReminderScheduler.cancel(c,id);
            ReminderNotice.show(c,item,false);
            ReminderWatchBridge.stop(c,item);
        }
    }
}