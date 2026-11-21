package com.cobrachris.sanftwecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

/** Reminder notifications never reuse the clock-alarm notification/channel. */
public final class ReminderNotice {
    private static final String SOFT="sanftwecker_reminder_soft_v1";
    private static final String STRONG="sanftwecker_reminder_strong_v1";
    private static final int BASE=99000;
    private ReminderNotice(){}
    public static void clear(Context c,int id){
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager!=null)manager.cancel(BASE+id);
    }
    public static void show(Context c,ReminderStore.Item item,boolean strong){
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager==null||!manager.areNotificationsEnabled())return;
        NotificationChannel soft=new NotificationChannel(SOFT,"Erinnerungen – sanft",
                NotificationManager.IMPORTANCE_HIGH);
        soft.setSound(null,null);soft.enableVibration(false);
        NotificationChannel loud=new NotificationChannel(STRONG,"Erinnerungen – deutlich",
                NotificationManager.IMPORTANCE_HIGH);
        loud.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),null);
        loud.enableVibration(true);
        manager.createNotificationChannel(soft);
        manager.createNotificationChannel(loud);
        Intent open=new Intent(c,ReminderActivity.class);
        PendingIntent view=PendingIntent.getActivity(c,BASE+item.id,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=new Notification.Builder(c,
                strong?STRONG:SOFT)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Erinnerung: "+item.title)
                .setContentText(strong?"Wichtige Erinnerung – bitte bearbeiten":
                        "Noch offen – erledigen oder später erinnern")
                .setContentIntent(view)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOngoing(false)
                .addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_input_add,"Erledigt",
                    ReminderScheduler.pending(c,item.id,item.dueAt,ReminderScheduler.DONE)).build())
                .addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_menu_recent_history,"+10 Min.",
                    ReminderScheduler.pending(c,item.id,item.dueAt,ReminderScheduler.LATER)).build())
                .addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_media_pause,"Alarm stoppen",
                    ReminderScheduler.pending(c,item.id,item.dueAt,ReminderScheduler.STOP)).build());
        manager.notify(BASE+item.id,builder.build());
    }
}