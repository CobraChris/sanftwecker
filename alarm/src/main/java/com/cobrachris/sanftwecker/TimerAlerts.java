package com.cobrachris.sanftwecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;

/** Timer completion uses sustained foreground output; short milestones stay finite. */
public final class TimerAlerts {
    private static final String CHANNEL="sanftwecker_timer_done_v1";
    private static final int BASE_ID=42000;
    private TimerAlerts(){}

    public static void ensureChannel(Context c){
        NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(m==null)return;
        // Preserve the existing channel for timers with both notifications enabled:
        // Android owns channel settings after first creation.
        createChannel(m,CHANNEL,"Fertige Timer",true,true);
        createChannel(m,TimerAlertSettings.channelId(true,false),
                "Timer – nur Alarmton",true,false);
        createChannel(m,TimerAlertSettings.channelId(false,true),
                "Timer – nur Vibration",false,true);
        createChannel(m,TimerAlertSettings.channelId(false,false),
                "Timer – lautlos",false,false);
    }
    private static void createChannel(NotificationManager m,String id,String label,
            boolean tone,boolean vibration){
        NotificationChannel ch=new NotificationChannel(id,label,
                vibration||tone?NotificationManager.IMPORTANCE_HIGH:
                        NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Benachrichtigung für einen beendeten Countdown");
        if(tone)ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),null);
        else ch.setSound(null,null);
        ch.enableVibration(vibration);
        if(vibration)ch.setVibrationPattern(new long[]{0,250,150,250});
        m.createNotificationChannel(ch);
    }
    public static void show(Context c,TimerEntry timer){
        boolean tone=TimerAlertSettings.tone(c,timer.id);
        boolean vibration=TimerAlertSettings.vibration(c,timer.id);
        try{
            // An exact countdown is allowed to start a foreground alarm service.
            TimerRingingService.start(c,timer,tone,vibration);
        }catch(RuntimeException startupFailure){
            AlarmRepository.diagnostic(c,
                    "FEHLER: Timer-Daueralarm konnte nicht gestartet werden");
            showFallback(c,timer.id,timer.name,tone,vibration);
        }
    }

    /** Only when Android refuses to start the sustained foreground alarm. */
    static void showFallback(Context c,int id,String name,boolean tone,boolean vibration){
        ensureChannel(c);
        NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(m==null||!m.areNotificationsEnabled()){
            AlarmRepository.diagnostic(c,"ACHTUNG: Timer-Benachrichtigungen blockiert");
            return;
        }
        Intent open=new Intent(c,TimerActivity.class);
        PendingIntent launch=PendingIntent.getActivity(c,BASE_ID+id,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent dismiss=new Intent(c,TimerReceiver.class)
                .setAction(TimerReceiver.ACTION_DISMISS).putExtra(TimerScheduler.EXTRA_ID,id);
        PendingIntent stop=PendingIntent.getBroadcast(c,BASE_ID+id,dismiss,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String channelId=TimerAlertSettings.channelId(tone,vibration);
        Notification notification=new Notification.Builder(c,channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle((name==null?"Timer":name)+" ist fertig")
                .setContentText("Timer abgelaufen – öffnen oder quittieren")
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentIntent(launch)
                .setAutoCancel(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,"Quittieren",stop).build())
                .build();
        try{m.notify(BASE_ID+id,notification);}
        catch(RuntimeException ex){AlarmRepository.diagnostic(c,"Timer-Benachrichtigung fehlgeschlagen");}
    }
    /** Brief, non-fullscreen event; Android notification channel respects user controls. */
    public static void showMilestone(Context c,TimerEntry timer,boolean prepare){
        ensureChannel(c);
        NotificationManager manager=
                (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager==null||!manager.areNotificationsEnabled())return;
        Intent open=new Intent(c,TimerDetailActivity.class)
                .putExtra(TimerScheduler.EXTRA_ID,timer.id);
        PendingIntent launch=PendingIntent.getActivity(c,BASE_ID+20000+timer.id,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String channel=TimerAlertSettings.channelId(
                TimerAlertSettings.tone(c,timer.id),
                TimerAlertSettings.vibration(c,timer.id));
        String title=prepare?"Startet gleich: "+timer.name:timer.name+" · Intervall";
        String message=prepare?"Countdown beginnt in 3 Sekunden":
                "Noch "+TimerDisplay.time(timer.remaining(
                        System.currentTimeMillis(),android.os.SystemClock.elapsedRealtime()),
                        TimerAlertSettings.FORMAT_MS);
        Notification notification=new Notification.Builder(c,channel)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title).setContentText(message)
                .setContentIntent(launch).setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setTimeoutAfter(5500L)
                .build();
        try{manager.notify(BASE_ID+(prepare?20000:10000)+timer.id,notification);}
        catch(RuntimeException e){
            AlarmRepository.diagnostic(c,"Timer-Zwischenbenachrichtigung fehlgeschlagen");
        }
    }
    /** A missed planned start is not silently treated as a successful countdown.
     * Alert quietly; do not unexpectedly launch the timer minutes late.
     */
    public static void showMissedSchedule(Context c,TimerEntry item){
        ensureChannel(c);
        NotificationManager manager=
                (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager==null||!manager.areNotificationsEnabled())return;
        Intent open=new Intent(c,TimerDetailActivity.class)
                .putExtra(TimerScheduler.EXTRA_ID,item.id);
        PendingIntent launch=PendingIntent.getActivity(c,BASE_ID+30000+item.id,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(c,
                TimerAlertSettings.channelId(false,false))
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Geplanter Timer nicht gestartet")
                .setContentText(item.name+" – Zeitplan erneut festlegen")
                .setCategory(Notification.CATEGORY_STATUS)
                .setContentIntent(launch).setAutoCancel(true).build();
        try{manager.notify(BASE_ID+30000+item.id,notification);}
        catch(RuntimeException ignored){
            AlarmRepository.diagnostic(c,"Hinweis für verpassten Timer nicht angezeigt");
        }
    }
    public static void dismiss(Context c,int id){
        TimerWearBridge.stopped(c,id);
        TimerRingingService.dismiss(c,id);
        NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(m!=null){
            m.cancel(BASE_ID+id);
            m.cancel(BASE_ID+10000+id);
            m.cancel(BASE_ID+20000+id);
        }
    }
}
