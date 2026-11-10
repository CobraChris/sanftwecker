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
    private static final String CHANNEL="sanftwecker_native_watch_alarm";
    private static final int NOTIFICATION_ID=4831;
    private static final long MAX_DURATION=15*60_000L;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable finish=()->{stopHaptics(false);stopSelf();};
    private final Runnable escalate=()->{
        if(activeWhen>0 && activeCascade && vibrator!=null){
            try{
                applyHaptic(WatchCascade.strongTimings(),WatchCascade.strongAmplitudes());
                sendAck("CASCADE_STRONG",activeId);
            }catch(RuntimeException failure){
                Log.w("SanftweckerWatch","Phase two vibration unavailable",failure);
            }
        }
    };
    private PowerManager.WakeLock wake;
    private Vibrator vibrator;
    private long activeWhen;
    private int activeId;
    private boolean activeCascade;

    public static void start(Context c,int id,long when,int minutes,String title){
        start(c,id,when,minutes,title,false);
    }
    public static void start(Context c,int id,long when,int minutes,String title,
            boolean cascade){
        if(id<1||id>1000||!WatchAlarmProtocol.nearTrigger(when,System.currentTimeMillis()))return;
        Intent i=new Intent(c,WatchAlarmService.class).setAction(START)
                .putExtra("id",id).putExtra("when",when)
                .putExtra("minutes",Math.max(cascade?2:1,Math.min(minutes,15)))
                .putExtra("title",title).putExtra("cascade",cascade);
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
    private static PendingIntent openApp(Context c){
        return PendingIntent.getActivity(c,11,new Intent(c,WatchMainActivity.class),
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
        Notification n=new Notification.Builder(this,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentTitle(title==null?"Sanftwecker":title)
                .setContentText("Uhralarm aktiv – zum Stoppen tippen")
                .setContentIntent(openApp(this))
                .setOngoing(true).setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause,"Stoppen",stopAction()).build())
                .build();
        startForeground(NOTIFICATION_ID,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
    }
    @Override public int onStartCommand(Intent i,int flags,int startId){
        if(i==null)return START_NOT_STICKY;
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
                    applyHaptic(WatchCascade.gentleTimings(),
                            WatchCascade.gentleAmplitudes());
                    handler.postDelayed(escalate,
                            WatchCascade.untilStrong(when,System.currentTimeMillis()));
                    sendAck("CASCADE_GENTLE",id);
                }else{
                    applyHaptic(WatchCascade.strongTimings(),
                            WatchCascade.strongAmplitudes());
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
        activeCascade=false;
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