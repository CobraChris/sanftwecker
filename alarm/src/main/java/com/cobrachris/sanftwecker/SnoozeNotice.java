package com.cobrachris.sanftwecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Public, quiet lock-screen status for an alarm already snoozing. */
public final class SnoozeNotice {
    static final String CANCEL="com.cobrachris.sanftwecker.CANCEL_SNOOZE_ONLY";
    private static final String CHANNEL="sanftwecker_snoozing_v1";
    private static final int NOTICE_ID=603;
    private SnoozeNotice(){}

    public static void show(Context c,int alarmId,long when){
        AlarmProfile alarm=AlarmRepository.get(c,alarmId);
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null||alarm==null||when<=System.currentTimeMillis())return;
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Schlummernde Wecker",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Zeigt den nächsten Schlummeralarm auf dem Sperrbildschirm");
        channel.setSound(null,null);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(channel);
        PendingIntent open=PendingIntent.getActivity(c,6300+alarmId,
                new Intent(c,MainActivity.class).addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent cancel=PendingIntent.getBroadcast(c,6301,
                new Intent(c,SnoozeActionReceiver.class).setAction(CANCEL)
                        .putExtra(AlarmScheduler.EXTRA_ID,alarmId)
                        .putExtra(AlarmScheduler.EXTRA_WHEN,when),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String at=new SimpleDateFormat("HH:mm",Locale.GERMANY).format(new Date(when));
        Notification note=new Notification.Builder(c,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(alarm.name+" – schlummert")
                .setContentText("Klingelt erneut um "+at+" Uhr")
                .setContentIntent(open)
                .setCategory(Notification.CATEGORY_ALARM)
                .setWhen(when)
                .setShowWhen(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setLocalOnly(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Schlummern für heute beenden",cancel).build())
                .build();
        nm.notify(NOTICE_ID,note);
    }

    public static void clear(Context c){
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm!=null)nm.cancel(NOTICE_ID);
    }
}
