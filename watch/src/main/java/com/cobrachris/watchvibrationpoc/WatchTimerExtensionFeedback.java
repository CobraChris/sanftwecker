package com.cobrachris.watchvibrationpoc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import java.nio.charset.StandardCharsets;

/**
 * A phone-verified extension result, not an optimistic Wear send acknowledgement.
 * Mirrors AlarmClockXtreme's action/result separation but keeps Sanftwecker's
 * existing timer protocol, package identity and vibration service unchanged.
 */
public final class WatchTimerExtensionFeedback {
    public static final String PREF="sanftwecker_timer_extend_feedback_v49";
    private static final String CHANNEL="sanftwecker_timer_extend_result_v1";
    private WatchTimerExtensionFeedback(){}
    public static boolean receive(Context c,byte[] payload){
        if(payload==null||payload.length>100)return false;
        String value=new String(payload,StandardCharsets.UTF_8);
        if(!value.matches("[1-9][0-9]{0,3}:[1-9][0-9]{11,12}:(30000|60000|120000|300000|600000):(ok|rejected)"))
            return false;
        String[] fields=value.split(":");
        int id=Integer.parseInt(fields[0]);
        long due=Long.parseLong(fields[1]);
        long amount=Long.parseLong(fields[2]);
        boolean applied="ok".equals(fields[3]);
        // Only accept a result associated with the most recent active request,
        // never a stale or another timer's acknowledgement.
        android.content.SharedPreferences pending=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        if(pending.getInt("id",0)!=id||pending.getLong("due",0)!=due||
                pending.getLong("amount",0)!=amount)return false;
        pending.edit().putString("result",applied?"ok":"rejected")
                .putLong("received",System.currentTimeMillis()).apply();
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager!=null && manager.areNotificationsEnabled()){
            NotificationChannel channel=new NotificationChannel(CHANNEL,
                    "Timerverlängerung bestätigt",NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null,null);
            channel.enableVibration(false);
            manager.createNotificationChannel(channel);
            Intent open=new Intent(c,WatchMainActivity.class);
            PendingIntent launch=PendingIntent.getActivity(c,49971,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification notice=new Notification.Builder(c,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(applied?"Timer verlängert":"Timer nicht verlängert")
                .setContentText(applied?
                    ("Bestätigt am Handy: +"+(amount/1000)+" Sekunden"):
                    "Timer wurde bereits geändert oder ist nicht mehr abgelaufen")
                .setContentIntent(launch)
                .setTimeoutAfter(6000L)
                .setAutoCancel(true)
                .build();
            try{manager.notify(49971,notice);}
            catch(RuntimeException ignored){}
        }
        return true;
    }
    public static void requested(Context c,int id,long due,long extra){
        c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
            .putInt("id",id).putLong("due",due).putLong("amount",extra)
            .putString("result","pending").putLong("received",0L).apply();
    }
}