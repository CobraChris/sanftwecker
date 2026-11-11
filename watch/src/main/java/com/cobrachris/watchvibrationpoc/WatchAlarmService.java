package com.cobrachris.watchvibrationpoc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import com.google.android.gms.wearable.Wearable;
import java.nio.charset.StandardCharsets;

/** Real watch-local clock-alarm haptics, not mirrored phone notifications. */
public final class WatchAlarmService extends Service {
    public static final String START="com.cobrachris.sanftwecker.WATCH_START";
    public static final String STOP="com.cobrachris.sanftwecker.WATCH_STOP";
    public static final String SNOOZE="com.cobrachris.sanftwecker.WATCH_SNOOZE";
    public static final String ACTIVE_PREF="watch_alarm_current_v1";
    private static final String CHANNEL="sanftwecker_native_watch_alarm";
    private static final int NOTIFICATION_ID=4831;
    private static final long MAX_DURATION=15*60_000L;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable finish=()->{stopHaptics(false);stopSelf();};
    private PowerManager.WakeLock wake;
    private Vibrator vibrator;
    private long activeWhen;
    private int activeId,activeSnoozeMinutes,activeIntensity=WatchCascade.NORMAL;
    private boolean activeCascade,snoozePending,snoozeConnectionError;
    private String activeTitle="Sanftwecker";
    private final Runnable snoozeTimeout=()->{
        if(activeWhen>0&&snoozePending){
            snoozePending=false;
            snoozeConnectionError=true;
            foreground(activeTitle);
            sendAck("SNOOZE_UNCONFIRMED",activeId);
        }
    };
    private final Runnable escalate=()->{
        if(activeWhen>0 && activeCascade && vibrator!=null){
            try{
                // Cancel phase-one looping waveform explicitly: a few Wear OS motors
                // keep playing it when an overlapping waveform is submitted.
                vibrator.cancel();
                applyHaptic(WatchCascade.strongTimings(activeIntensity),
                        WatchCascade.strongAmplitudes(activeIntensity));
                sendAck("CASCADE_STRONG",activeId);
            }catch(RuntimeException failure){
                Log.w("SanftweckerWatch","Phase two vibration unavailable",failure);
            }
        }
    };

    public static void start(Context c,int id,long when,int minutes,String title){
        start(c,id,when,minutes,title,false);
    }
    public static void start(Context c,int id,long when,int minutes,String title,
            boolean cascade){
        start(c,id,when,minutes,title,cascade,0);
    }
    public static void start(Context c,int id,long when,int minutes,String title,
            boolean cascade,int snoozeMinutes){
        start(c,id,when,minutes,title,cascade,snoozeMinutes,WatchCascade.NORMAL);
    }
    public static void start(Context c,int id,long when,int minutes,String title,
            boolean cascade,int snoozeMinutes,int intensity){
        if(id<1||id>1000||snoozeMinutes<0||snoozeMinutes>30||
                !WatchCascade.validIntensity(intensity)||
                !WatchAlarmProtocol.nearTrigger(when,System.currentTimeMillis()))return;
        Intent i=new Intent(c,WatchAlarmService.class).setAction(START)
                .putExtra("id",id).putExtra("when",when)
                .putExtra("minutes",Math.max(cascade?2:1,Math.min(minutes,15)))
                .putExtra("title",title).putExtra("cascade",cascade)
                .putExtra("snooze_minutes",snoozeMinutes)
                .putExtra("watch_intensity",intensity);
        try{c.startForegroundService(i);}
        catch(RuntimeException error){
            Log.e("SanftweckerWatch","Unable to start local watch alarm service",error);
        }
    }
    private void applyHaptic(long[] timings,int[] amplitudes){
        if(vibrator==null)return;
        VibrationEffect effect=vibrator.hasAmplitudeControl()
                ? VibrationEffect.createWaveform(timings,amplitudes,0)
                : VibrationEffect.createWaveform(timings,0);
        if(Build.VERSION.SDK_INT>=33){
            VibrationAttributes attrs=new VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM).build();
            vibrator.vibrate(effect,attrs);
        }else{
            AudioAttributes attrs=new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM).build();
            vibrator.vibrate(effect,attrs);
        }
    }
    private PendingIntent stopAction(){
        Intent stop=new Intent(this,WatchAlarmService.class).setAction(STOP)
                .putExtra("id",activeId).putExtra("when",activeWhen)
                .putExtra("user",true);
        return PendingIntent.getService(this,10,stop,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private PendingIntent snoozeAction(){
        Intent snooze=new Intent(this,WatchAlarmService.class).setAction(SNOOZE)
                .putExtra("id",activeId).putExtra("when",activeWhen)
                .putExtra("user",true);
        return PendingIntent.getService(this,12,snooze,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private void requestSnooze(){
        if(activeWhen<=0||activeSnoozeMinutes<=0||snoozePending)return;
        final int id=activeId;
        final long when=activeWhen;
        snoozePending=true;
        snoozeConnectionError=false;
        foreground(activeTitle);
        handler.removeCallbacks(snoozeTimeout);
        // Do not stop the local alarm yet. Only the phone's confirmed STOP
        // message means snoozing was actually scheduled on the handset.
        try{
            Wearable.getNodeClient(this).getConnectedNodes().addOnSuccessListener(nodes->{
                if(nodes.isEmpty()){
                    if(activeWhen==when&&activeId==id)rejectSnooze();
                    return;
                }
                for(com.google.android.gms.wearable.Node node:nodes){
                    Wearable.getMessageClient(this).sendMessage(node.getId(),
                            WatchAlarmProtocol.SNOOZE,
                            (id+":"+when+":1").getBytes(StandardCharsets.UTF_8))
                            .addOnFailureListener(e->{
                                if(activeWhen==when&&activeId==id)rejectSnooze();
                            });
                }
            }).addOnFailureListener(e->{
                if(activeWhen==when&&activeId==id)rejectSnooze();
            });
        }catch(RuntimeException e){
            rejectSnooze();
        }
        handler.postDelayed(snoozeTimeout,8000L);
    }
    private void rejectSnooze(){
        handler.removeCallbacks(snoozeTimeout);
        snoozePending=false;
        snoozeConnectionError=true;
        if(activeWhen>0)foreground(activeTitle);
    }
    private static PendingIntent openApp(Context c){
        return PendingIntent.getActivity(c,11,WatchRingActivity.openIntent(c),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private void foreground(String title){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(nm!=null){
            NotificationChannel channel=new NotificationChannel(CHANNEL,
                    "Eigenständiger Sanftwecker-Alarm",NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null,null);
            channel.enableVibration(false);
            channel.setDescription("Lokaler Weckalarm mit direkter Vibrationssteuerung");
            nm.createNotificationChannel(channel);
        }
        Notification.Builder builder=new Notification.Builder(this,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentTitle(title==null?"Sanftwecker":title)
                .setContentText(snoozePending?"Schlummern wird am Handy bestätigt…":
                        snoozeConnectionError?"Handy nicht erreicht – erneut versuchen":
                        activeSnoozeMinutes>0?"Schlummern oder stoppen":
                        "Uhralarm aktiv – zum Stoppen tippen")
                .setContentIntent(openApp(this))
                .setPriority(Notification.PRIORITY_HIGH)
                .setOngoing(true).setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if(activeSnoozeMinutes>0&&!snoozePending)
            builder.addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_lock_idle_alarm,
                    "Schlummern · "+activeSnoozeMinutes+" Min.",snoozeAction()).build());
        builder.addAction(new Notification.Action.Builder(
                android.R.drawable.ic_media_pause,"Stoppen",stopAction()).build());
        startForeground(NOTIFICATION_ID,builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
    }
    @Override public int onStartCommand(Intent i,int flags,int startId){
        if(i==null)return START_NOT_STICKY;
        if(SNOOZE.equals(i.getAction())){
            long when=i.getLongExtra("when",0);
            int id=i.getIntExtra("id",-1);
            if(activeWhen==when&&activeId==id&&when>0&&
                    i.getBooleanExtra("user",false))requestSnooze();
            return START_NOT_STICKY;
        }
        if(STOP.equals(i.getAction())){
            long when=i.getLongExtra("when",0);
            int id=i.getIntExtra("id",-1);
            if(i.getBooleanExtra("stop_all",false)){
                if(activeWhen>0){
                    if(i.getBooleanExtra("user",false))
                        sendToPhone(WatchAlarmProtocol.STOP,
                                activeId+":"+activeWhen+":1");
                    stopHaptics(true);stopSelf();
                }
                stopSelf();
                return START_NOT_STICKY;
            }
            if(activeWhen==when&&activeId==id&&when>0){
                if(i.getBooleanExtra("user",false))sendToPhone(
                        WatchAlarmProtocol.STOP,id+":"+when+":1");
                stopHaptics(true);stopSelf();
            }
            return START_NOT_STICKY;
        }
        if(!START.equals(i.getAction()))return START_NOT_STICKY;
        final int id=i.getIntExtra("id",-1);
        final long when=i.getLongExtra("when",0);
        if(id<1||id>1000||!WatchAlarmProtocol.nearTrigger(when,System.currentTimeMillis())){
            stopSelf();
            return START_NOT_STICKY;
        }
        if(activeWhen==when&&activeId==id)return START_NOT_STICKY;
        long last=getSharedPreferences("watch_last_start",MODE_PRIVATE)
                .getLong("alarm_"+id,0);
        if(last==when)return START_NOT_STICKY;
        if(activeWhen!=0)stopHaptics(false);
        activeWhen=when;activeId=id;
        activeCascade=i.getBooleanExtra("cascade",false);
        activeIntensity=activeCascade
                ?WatchCascade.safeIntensity(i.getIntExtra(
                        "watch_intensity",WatchCascade.NORMAL))
                :WatchCascade.NORMAL;
        activeSnoozeMinutes=Math.max(0,Math.min(30,i.getIntExtra("snooze_minutes",0)));
        activeTitle=i.getStringExtra("title")==null?
                "Sanftwecker":i.getStringExtra("title");
        snoozePending=false;snoozeConnectionError=false;
        getSharedPreferences(ACTIVE_PREF,MODE_PRIVATE).edit()
                .putInt("id",id).putLong("when",when)
                .putInt("snooze_minutes",activeSnoozeMinutes)
                .putBoolean("cascade",activeCascade)
                .putInt("watch_intensity",activeIntensity).commit();
        int minutes=Math.max(activeCascade?2:1,
                Math.min(15,i.getIntExtra("minutes",5)));
        try{
            foreground(i.getStringExtra("title"));
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            if(pm!=null){
                wake=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Sanftwecker:WatchAlarm");
                wake.acquire(Math.min(MAX_DURATION,minutes*60_000L)+5000L);
            }
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                VibratorManager manager=(VibratorManager)getSystemService(VIBRATOR_MANAGER_SERVICE);
                vibrator=manager==null?null:manager.getDefaultVibrator();
            }else vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
            if(vibrator==null||!vibrator.hasVibrator()){
                sendAck("NO_VIBRATOR",id);
            }else{
                // Before the 60-second boundary, give sparse, short pulses at
                // low amplitude when the motor supports it. Phase two has the
                // dense original strong rhythm. Both use USAGE_ALARM.
                handler.removeCallbacks(escalate);
                if(activeCascade && !WatchCascade.strong(when,System.currentTimeMillis())){
                    applyHaptic(WatchCascade.gentleTimings(activeIntensity),
                            WatchCascade.gentleAmplitudes(activeIntensity));
                    handler.postDelayed(escalate,
                            WatchCascade.untilStrong(when,System.currentTimeMillis()));
                    sendAck("CASCADE_GENTLE",id);
                }else{
                    applyHaptic(WatchCascade.strongTimings(activeIntensity),
                            WatchCascade.strongAmplitudes(activeIntensity));
                }
                getSharedPreferences("watch_last_start",MODE_PRIVATE).edit()
                        .putLong("alarm_"+id,when).commit();
                sendAck("STARTED",id);
            }
            handler.removeCallbacks(finish);
            handler.postDelayed(finish,Math.min(MAX_DURATION,minutes*60_000L));
        }catch(RuntimeException ex){
            Log.e("SanftweckerWatch","Watch haptic alarm failed",ex);
            sendAck("NO_VIBRATOR",id);
            stopHaptics(false);stopSelf();
        }
        return START_NOT_STICKY;
    }
    private void sendAck(String result,int id){
        sendToPhone(WatchAlarmProtocol.ACK,result+":"+id);
    }
    private void sendToPhone(String path,String text){
        Wearable.getNodeClient(this).getConnectedNodes().addOnSuccessListener(nodes->{
            for(com.google.android.gms.wearable.Node node:nodes)
                Wearable.getMessageClient(this).sendMessage(node.getId(),path,
                        text.getBytes(StandardCharsets.UTF_8))
                        .addOnFailureListener(e->Log.w("SanftweckerWatch","Watch ack pending",e));
        }).addOnFailureListener(e->Log.w("SanftweckerWatch","Phone disconnected",e));
    }
    private void stopHaptics(boolean acknowledged){
        handler.removeCallbacks(finish);
        handler.removeCallbacks(escalate);
        handler.removeCallbacks(snoozeTimeout);
        activeCascade=false;snoozePending=false;snoozeConnectionError=false;
        activeIntensity=WatchCascade.NORMAL;
        activeSnoozeMinutes=0;
        getSharedPreferences(ACTIVE_PREF,MODE_PRIVATE).edit()
                .remove("id").remove("when").remove("snooze_minutes")
                .remove("cascade").remove("watch_intensity").commit();
        if(vibrator!=null)vibrator.cancel();
        vibrator=null;
        if(wake!=null&&wake.isHeld())wake.release();
        wake=null;
        int oldId=activeId;
        activeWhen=0;activeId=0;
        if(oldId>0&&acknowledged)sendAck("STOPPED",oldId);
        stopForeground(STOP_FOREGROUND_REMOVE);
    }
    @Override public void onDestroy(){stopHaptics(false);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}