package com.cobrachris.sanftwecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

/**
 * Timer-only foreground alarm. Unlike a one-shot notification, its audible
 * alarm-stream tone and direct vibration repeat until acknowledged (or 3 min).
 * Never touches the wake-alarm RingingService, schedules, or Watch cascade.
 */
public final class TimerRingingService extends Service {
    public static final String ACTION_START="com.cobrachris.sanftwecker.TIMER_RING_START";
    public static final String ACTION_STOP="com.cobrachris.sanftwecker.TIMER_RING_STOP";
    public static final String ACTION_STOP_ALL="com.cobrachris.sanftwecker.TIMER_RING_STOP_ALL";
    public static final String ACTION_FINISHED="com.cobrachris.sanftwecker.TIMER_RING_FINISHED";
    private static final String EXTRA_LABEL="timer_ring_name";
    private static final String EXTRA_TONE="timer_ring_tone";
    private static final String EXTRA_VIBRATION="timer_ring_vibration";
    private static final String EXTRA_DUE_WALL="timer_ring_due_wall";
    private static final String EXTRA_DUE_ELAPSED="timer_ring_due_elapsed";
    private static final String EXTRA_BOOT_OFFSET="timer_ring_boot_offset";
    private static final String CHANNEL="sanftwecker_timer_active_alarm_v1";
    private static final int NOTIFICATION_ID=42570;
    private static final long REPEAT_MS=1700L;
    private final TimerRingSession active=new TimerRingSession();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private NotificationManager manager;
    private ToneGenerator generator;
    private Vibrator motor;
    private PowerManager.WakeLock wakeLock;
    private boolean foregroundActive;

    private final Runnable pulse=new Runnable(){
        @Override public void run(){
            if(!active.hasAlerts())return;
            int[] before=active.ids();
            boolean expired=active.expire(SystemClock.elapsedRealtime());
            if(expired){
                for(int id:before)if(!active.contains(id))TimerWearBridge.stopped(
                        TimerRingingService.this,id);
            }
            if(!active.hasAlerts()){shutdown();return;}
            if(expired){
                try{showForeground();}catch(RuntimeException e){
                    AlarmRepository.diagnostic(TimerRingingService.this,
                            "WARNUNG: Timer-Alarminformation nicht aktualisiert");
                }
            }
            if(active.wantsTone())beep();
            else releaseTone();
            if(active.wantsVibration())vibrateOnce();
            else cancelVibration();
            handler.postDelayed(this,REPEAT_MS);
        }
    };

    public static void ensureChannel(Context context){
        NotificationManager nm=(NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)return;
        NotificationChannel ch=new NotificationChannel(CHANNEL,
                "Aktiver Timeralarm",NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Klingelnder Timer mit sichtbarer Stoppen-Aktion");
        ch.setSound(null,null); // Audio comes from the alarm stream, not a brief notification.
        ch.enableVibration(false); // The foreground service owns its finite vibration pulses.
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    public static void start(Context c,TimerEntry item,boolean tone,boolean vibration){
        Intent i=new Intent(c,TimerRingingService.class)
                .setAction(ACTION_START)
                .putExtra(TimerScheduler.EXTRA_ID,item.id)
                .putExtra(EXTRA_LABEL,item.name)
                .putExtra(EXTRA_TONE,tone)
                .putExtra(EXTRA_VIBRATION,vibration)
                .putExtra(EXTRA_DUE_WALL,item.dueWallMs)
                .putExtra(EXTRA_DUE_ELAPSED,item.dueElapsedMs)
                .putExtra(EXTRA_BOOT_OFFSET,item.bootOffsetMs);
        if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);
        else c.startService(i);
    }

    /** Cancels just one timer; other finished countdowns continue to alert. */
    public static void dismiss(Context c,int id){
        if(id<1)return;
        try{
            c.startService(new Intent(c,TimerRingingService.class)
                    .setAction(ACTION_STOP).putExtra(TimerScheduler.EXTRA_ID,id));
        }catch(RuntimeException e){
            AlarmRepository.diagnostic(c,"WARNUNG: Timer-Alarm konnte nicht quittiert werden");
        }
    }

    @Override public void onCreate(){
        super.onCreate();
        manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        ensureChannel(this);
        if(Build.VERSION.SDK_INT>=31){
            VibratorManager v=(VibratorManager)getSystemService(VIBRATOR_MANAGER_SERVICE);
            motor=v==null?null:v.getDefaultVibrator();
        }else motor=(Vibrator)getSystemService(VIBRATOR_SERVICE);
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?null:intent.getAction();
        if(ACTION_STOP_ALL.equals(action)){
            for(int id:active.ids())TimerWearBridge.stopped(this,id);
            active.removeAll();
            shutdown();
            return START_NOT_STICKY;
        }
        if(ACTION_STOP.equals(action)){
            int id=intent.getIntExtra(TimerScheduler.EXTRA_ID,-1);
            if(id>0){
                active.remove(id);
                TimerWearBridge.stopped(this,id);
            }
            if(!active.hasAlerts())shutdown();
            else refresh();
            return START_NOT_STICKY;
        }
        if(!ACTION_START.equals(action)){
            if(!active.hasAlerts())shutdown();
            return START_NOT_STICKY;
        }
        int id=intent.getIntExtra(TimerScheduler.EXTRA_ID,-1);
        String name=intent.getStringExtra(EXTRA_LABEL);
        boolean tone=intent.getBooleanExtra(EXTRA_TONE,true);
        boolean vibration=intent.getBooleanExtra(EXTRA_VIBRATION,true);
        if(id<1||id>9999){if(!active.hasAlerts())shutdown();return START_NOT_STICKY;}
        try{
            active.add(id,name,tone,vibration,SystemClock.elapsedRealtime(),
                    intent.getLongExtra(EXTRA_DUE_WALL,0L),
                    intent.getLongExtra(EXTRA_DUE_ELAPSED,0L),
                    intent.getLongExtra(EXTRA_BOOT_OFFSET,0L));
            refresh();
            AlarmRepository.diagnostic(this,"Timer-Alarm gestartet: "+active.label());
        }catch(RuntimeException failure){
            Log.e("Sanftwecker","Timer foreground alarm unavailable",failure);
            AlarmRepository.diagnostic(this,"FEHLER: Timer-Daueralarm nicht gestartet");
            TimerAlerts.showFallback(this,id,name,tone,vibration);
            active.remove(id);
            if(active.hasAlerts())refresh();
            else shutdown();
        }
        return START_NOT_STICKY;
    }

    private void refresh(){
        showForeground();
        acquireWakeLock();
        handler.removeCallbacks(pulse);
        handler.post(pulse);
    }
    private PendingIntent stopAll(){
        return PendingIntent.getService(this,7191,
                new Intent(this,TimerRingingService.class).setAction(ACTION_STOP_ALL),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private PendingIntent extendAction(long ms){
        int id=active.firstId();
        Intent extend=new Intent(this,TimerReceiver.class)
            .setAction(TimerReceiver.ACTION_EXTEND)
            .putExtra(TimerScheduler.EXTRA_ID,id)
            .putExtra(TimerScheduler.EXTRA_DUE,active.firstDueWallMs())
            .putExtra(TimerReceiver.EXTRA_EXTENSION_MS,ms);
        return PendingIntent.getBroadcast(this,7300+id*3+(ms==30_000L?1:2),
            extend,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private PendingIntent openAlarm(){
        Intent open=new Intent(this,TimerRingActivity.class)
                .putExtra(TimerRingActivity.EXTRA_LABEL,active.label())
                .putExtra(TimerRingActivity.EXTRA_ID,active.firstId())
                .putExtra(TimerRingActivity.EXTRA_DUE_WALL,active.firstDueWallMs())
                .putExtra(TimerRingActivity.EXTRA_DUE_ELAPSED,active.firstDueElapsedMs())
                .putExtra(TimerRingActivity.EXTRA_BOOT_OFFSET,active.firstBootOffsetMs())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this,7192,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private void showForeground(){
        if(manager==null)throw new IllegalStateException("NotificationManager fehlt");
        Notification.Builder builder=new Notification.Builder(this,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(active.count()==1?active.label()+" ist fertig":active.label())
                .setContentText("Timeralarm läuft – antippen oder Stoppen auswählen")
                .setContentIntent(openAlarm())
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setLocalOnly(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause,"Stoppen",stopAll()).build());
        if(active.count()==1 && active.firstId()>0 && active.firstDueWallMs()>0){
            builder.addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_media_play,"+30 Sek.",extendAction(30_000L)).build());
            builder.addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_media_play,"+1 Min.",extendAction(60_000L)).build());
        }
        // On Android 14+, missing full-screen permission must not prevent sound,
        // vibration or the persistent notification from working.
        if((active.wantsTone()||active.wantsVibration()) &&
                (Build.VERSION.SDK_INT<34||manager.canUseFullScreenIntent()))
            builder.setFullScreenIntent(openAlarm(),true);
        Notification notification=builder.build();
        if(Build.VERSION.SDK_INT>=29)
            startForeground(NOTIFICATION_ID,notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(NOTIFICATION_ID,notification);
        foregroundActive=true;
    }
    private void acquireWakeLock(){
        PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
        if(power==null)return;
        if(wakeLock==null)
            wakeLock=power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "Sanftwecker:TimerAlarm");
        if(wakeLock.isHeld())wakeLock.release();
        wakeLock.acquire(TimerRingSession.MAX_RING_MS+5_000L);
    }
    private void beep(){
        try{
            if(generator==null)
                generator=new ToneGenerator(AudioManager.STREAM_ALARM,90);
            generator.startTone(ToneGenerator.TONE_PROP_BEEP,850);
        }catch(RuntimeException e){
            releaseTone();
            AlarmRepository.diagnostic(this,
                    "WARNUNG: Timer-Alarmton durch Android oder Audiogerät nicht verfügbar");
        }
    }
    private void releaseTone(){
        if(generator!=null){
            try{generator.stopTone();generator.release();}
            catch(RuntimeException ignored){}
            generator=null;
        }
    }
    private void vibrateOnce(){
        if(motor==null||!motor.hasVibrator()||ActiveAlarmState.activeId(this)>0)return;
        try{
            AudioAttributes attributes=new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            VibrationEffect effect=VibrationEffect.createWaveform(
                    new long[]{0,620,200,620},-1);
            motor.vibrate(effect,attributes);
        }catch(RuntimeException e){
            AlarmRepository.diagnostic(this,
                    "WARNUNG: Timer-Vibration durch Android eingeschränkt");
        }
    }
    private void cancelVibration(){
        // Never cancel a concurrent real wake alarm's hardware vibration.
        if(motor!=null && ActiveAlarmState.activeId(this)==0)
            try{motor.cancel();}catch(RuntimeException ignored){}
    }

    private void shutdown(){
        handler.removeCallbacks(pulse);
        releaseTone();
        cancelVibration();
        if(wakeLock!=null && wakeLock.isHeld())wakeLock.release();
        if(foregroundActive){
            stopForeground(STOP_FOREGROUND_REMOVE);
            foregroundActive=false;
        }
        sendBroadcast(new Intent(ACTION_FINISHED).setPackage(getPackageName()));
        stopSelf();
    }
    @Override public void onDestroy(){
        handler.removeCallbacks(pulse);
        releaseTone();
        cancelVibration();
        if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
