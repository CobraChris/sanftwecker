package com.cobrachris.watchvibrationpoc;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

/**
 * A one-shot visible alarm notification, separate from the quiet FGS
 * notification. Wear OS does not support phone-style full-screen intents:
 * the watch OS controls whether its notification turns on the screen.
 * Existing vibration services and their channels are intentionally unchanged.
 */
public final class WatchAlarmNotice {
    public static final String CHANNEL="sanftwecker_watch_visible_alarm_v1";
    private static final int CLOCK_BASE=24_000;
    private static final int TIMER_BASE=38_000;
    private WatchAlarmNotice(){}

    private static NotificationManager manager(Context c){
        return (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    private static boolean allowed(Context c,NotificationManager nm){
        return nm!=null && nm.areNotificationsEnabled() &&
                (Build.VERSION.SDK_INT<33 ||
                 c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                         ==PackageManager.PERMISSION_GRANTED);
    }
    /** Settings can disable just this visible-alert channel independently. */
    public static boolean channelBlocked(Context c){
        NotificationManager nm=manager(c);
        if(nm==null)return true;
        NotificationChannel ch=nm.getNotificationChannel(CHANNEL);
        return ch!=null&&ch.getImportance()==NotificationManager.IMPORTANCE_NONE;
    }
    private static boolean ensureChannel(Context c,NotificationManager nm){
        if(!allowed(c,nm))return false;
        NotificationChannel ch=new NotificationChannel(CHANNEL,
                "Wecker und Timer sichtbar anzeigen",NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Sichtbare Alarmmeldung mit Stoppen und optional Schlummern");
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        // A brief notification cue is intentional: the app's long, adjustable
        // vibration remains entirely under control of the existing services.
        ch.setSound(null,null);
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0L,65L});
        nm.createNotificationChannel(ch);
        return !channelBlocked(c);
    }
    private static void post(Context c,int notificationId,String title,
            String detail,PendingIntent open,PendingIntent stop,
            PendingIntent snooze,long maxVisibleMs){
        NotificationManager nm=manager(c);
        if(!ensureChannel(c,nm))return;
        Notification.Builder b=new Notification.Builder(c,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentTitle(title)
                .setContentText(detail)
                .setContentIntent(open)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setTimeoutAfter(maxVisibleMs)
                .setShowWhen(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause,"Stoppen",stop).build());
        if(snooze!=null)
            b.addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_lock_idle_alarm,"Schlummern",snooze).build());
        try{nm.notify(notificationId,b.build());}
        catch(RuntimeException error){
            Log.w("SanftweckerWatch","Watch visual alarm notification unavailable",error);
        }
    }
    public static void clock(Context c,int id,String title,
            PendingIntent open,PendingIntent stop,PendingIntent snooze,
            long maxVisibleMs){
        if(id<1||id>1000)return;
        post(c,CLOCK_BASE+id,title==null?"Sanftwecker":title,
                "Wecker aktiv – zum Stoppen oder Schlummern öffnen",
                open,stop,snooze,maxVisibleMs);
    }
    public static void timer(Context c,int id,String title,
            PendingIntent open,PendingIntent stop,
            PendingIntent plus30,PendingIntent plus60){
        if(id<1||id>9999)return;
        NotificationManager nm=manager(c);
        if(!ensureChannel(c,nm))return;
        Notification.Builder notice=new Notification.Builder(c,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentTitle(title==null?"Timer abgelaufen":title)
                .setContentText("Timer verlängern oder stoppen")
                .setContentIntent(open)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setTimeoutAfter(190_000L)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause,"Stoppen",stop).build());
        if(plus30!=null&&plus60!=null){
            notice.addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_media_play,"+30 Sek.",plus30).build());
            notice.addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_media_play,"+1 Min.",plus60).build());
        }
        try{nm.notify(TIMER_BASE+id,notice.build());}
        catch(RuntimeException error){
            Log.w("SanftweckerWatch","Watch timer notification unavailable",error);
        }
    }
    public static void clearClock(Context c,int id){
        if(id>0&&id<=1000)cancel(c,CLOCK_BASE+id);
    }
    public static void clearTimer(Context c,int id){
        if(id>0&&id<=9999)cancel(c,TIMER_BASE+id);
    }
    private static void cancel(Context c,int notificationId){
        NotificationManager nm=manager(c);
        if(nm!=null)nm.cancel(notificationId);
    }
}
