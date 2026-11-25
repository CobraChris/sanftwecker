package com.cobrachris.sanftwecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Reminder notifications retain their own IDs; cascades reuse the proven clock-alarm channel. */
public final class ReminderNotice {
    private static final String SOFT="sanftwecker_reminder_soft_v1";
    private static final String STRONG="sanftwecker_reminder_strong_custom_v2";
    private static final String VIBRATE_ONLY="sanftwecker_reminder_vibrate_only_v1";
    private static final int BASE=99000;
    private ReminderNotice(){}
    public static void clear(Context c,int id){
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager!=null)manager.cancel(BASE+id);
    }
    /** Passive updates (restore/escalation) must never unexpectedly wake the display. */
    public static void show(Context c,ReminderStore.Item item,boolean strong){
        show(c,item,strong,false);
    }
    /** Only an actual newly fired, user-scheduled reminder requests full-screen UI. */
    public static void show(Context c,ReminderStore.Item item,boolean strong,
            boolean justFired){
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager==null||!manager.areNotificationsEnabled())return;
        NotificationChannel soft=new NotificationChannel(SOFT,"Erinnerungen – sanft",
                NotificationManager.IMPORTANCE_HIGH);
        soft.setSound(null,null);soft.enableVibration(false);
        NotificationChannel loud=new NotificationChannel(STRONG,"Erinnerungen – deutlich",
                NotificationManager.IMPORTANCE_HIGH);
        // Chosen procedural tone is played only by ReminderToneService.
        loud.setSound(null,null);
        loud.enableVibration(true);
        manager.createNotificationChannel(soft);
        manager.createNotificationChannel(loud);
        NotificationChannel vibration=new NotificationChannel(VIBRATE_ONLY,
                "Erinnerungen – Handyvibration ohne Ton",NotificationManager.IMPORTANCE_HIGH);
        vibration.setSound(null,null);
        vibration.enableVibration(true);
        manager.createNotificationChannel(vibration);
        Intent open=new Intent(c,ReminderOptionsActivity.class)
            .putExtra(ReminderScheduler.ID,item.id)
            .putExtra(ReminderScheduler.DUE,item.dueAt);
        PendingIntent view=PendingIntent.getActivity(c,BASE+item.id,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent fullScreen=new Intent(c,ReminderOptionsActivity.class)
            .putExtra(ReminderScheduler.ID,item.id)
            .putExtra(ReminderScheduler.DUE,item.dueAt)
            .putExtra(ReminderOptionsActivity.EXTRA_FULLSCREEN,true);
        PendingIntent fullView=PendingIntent.getActivity(c,BASE+10_000+item.id,
                fullScreen,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        // Cascade reminders use the SAME high-priority notification channel as
        // real wake alarms. Distinct notification IDs keep both occurrences safe.
        boolean cascade=ReminderAlarmConfig.profile(c).cascadeEnabled;
        if(cascade)RingingService.ensureChannels(c);
        String channel=cascade&&(strong||justFired)
                ?RingingService.ACTIVE_CHANNEL
                :strong?(item.phoneSound?STRONG:VIBRATE_ONLY):SOFT;
        Notification.Builder builder=new Notification.Builder(c,channel)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Erinnerung: "+item.title)
                .setContentText(strong?"Wichtige Erinnerung – bitte bearbeiten":
                        "Noch offen – erledigen oder später erinnern")
                .setContentIntent(view)
                .setCategory(cascade?Notification.CATEGORY_ALARM:Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_MAX)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOngoing(false)
                .addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_input_add,"Erledigt",
                    ReminderScheduler.pending(c,item.id,item.dueAt,ReminderScheduler.DONE)).build())
                .addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_menu_recent_history,"Alle Optionen",view).build())
                .addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_media_pause,"Alarm stoppen",
                    ReminderScheduler.pending(c,item.id,item.dueAt,ReminderScheduler.STOP)).build());
        // Android decides between the lockscreen activity (screen off) and
        // an expanded heads-up alert (screen already unlocked). We never
        // start an activity directly from a background receiver.
        if(justFired)builder.setFullScreenIntent(fullView,true);
        manager.notify(BASE+item.id,builder.build());
    }
}