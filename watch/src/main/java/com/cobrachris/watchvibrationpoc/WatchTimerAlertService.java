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
import android.os.SystemClock;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import com.google.android.gms.wearable.Wearable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Watch-local timer notification and sustained vibration (up to 3 minutes).
 * Separate from WatchAlarmService, so a timer cannot stop or reconfigure
 * the existing clock alarm or its gentle-to-strong vibration cascade.
 */
public final class WatchTimerAlertService extends Service {
    public static final String START="com.cobrachris.sanftwecker.WATCH_TIMER_START";
    public static final String STOP="com.cobrachris.sanftwecker.WATCH_TIMER_STOP";
    public static final String STOP_ALL="com.cobrachris.sanftwecker.WATCH_TIMER_STOP_ALL";
    public static final String ACTIVE_PREF="sanftwecker_watch_timer_active_v1";
    private static final String CHANNEL="sanftwecker_watch_timer_alarm_v1";
    private static final String PREF="sanftwecker_watch_timer_ack_v1";
    private static final int NOTIFICATION_ID=4840;
    private static final long MAX_MS=180_000L;
    private static final long INTERVAL_MS=1500L;
    private final LinkedHashMap<Integer,Entry> active=new LinkedHashMap<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private boolean inForeground;

    private static final class Entry {
        final int id;
        final long when,expiresAt;
        final boolean vibration;
        final String name;
        Entry(int id,long when,boolean vibration,String name){
            this.id=id;this.when=when;this.vibration=vibration;this.name=name;
            this.expiresAt=when+MAX_MS;
        }
    }

    private final Runnable pulse=new Runnable(){
        @Override public void run(){
            long now=System.currentTimeMillis();
            boolean changed=false;
            for(Entry entry:new ArrayList<>(active.values())){
                if(now>=entry.expiresAt){
                    active.remove(entry.id);
                    markStopped(entry.id,entry.when);
                    changed=true;
                }
            }
            if(active.isEmpty()){shutdown();return;}
            if(changed)foreground();
            if(!clockAlarmActive()){
                for(Entry entry:active.values()){
                    if(entry.vibration){vibrate();break;}
                }
            }
            handler.postDelayed(this,INTERVAL_MS);
        }
    };

    public static void start(Context c,WatchTimerProtocol.Packet packet){
        if(packet==null||!WatchTimerProtocol.validWhen(packet.when,
                System.currentTimeMillis()))return;
        Intent request=new Intent(c,WatchTimerAlertService.class).setAction(START)
                .putExtra("id",packet.id).putExtra("when",packet.when)
                .putExtra("name",packet.name).putExtra("vibration",packet.vibration);
        try{
            if(Build.VERSION.SDK_INT>=26)c.startForegroundService(request);
            else c.startService(request);
        }catch(RuntimeException ex){
            Log.w("SanftweckerWatch","Timer alert could not start",ex);
        }
    }
    public static void stop(Context c,int id,long when){
        if(id<1||id>9999||when<=0)return;
        Intent request=new Intent(c,WatchTimerAlertService.class).setAction(STOP)
                .putExtra("id",id).putExtra("when",when);
        try{c.startService(request);}
        catch(RuntimeException ex){
            Log.w("SanftweckerWatch","Timer stop deferred",ex);
        }
    }
    @Override public void onCreate(){
        super.onCreate();
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(manager!=null){
            NotificationChannel channel=new NotificationChannel(CHANNEL,
                    "Timeralarm auf der Uhr",NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Abgelaufene Timer, mit Stoppen auf der Uhr");
            channel.setSound(null,null);
            channel.enableVibration(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(channel);
        }
        if(Build.VERSION.SDK_INT>=31){
            VibratorManager motors=(VibratorManager)getSystemService(VIBRATOR_MANAGER_SERVICE);
            vibrator=motors==null?null:motors.getDefaultVibrator();
        }else vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?null:intent.getAction();
        if(STOP_ALL.equals(action)){
            for(Entry entry:active.values()){
                markStopped(entry.id,entry.when);
                sendStopToPhone(entry);
            }
            active.clear();
            shutdown();
            return START_NOT_STICKY;
        }
        if(STOP.equals(action)){
            int id=intent.getIntExtra("id",-1);
            long when=intent.getLongExtra("when",0);
            markStopped(id,when);
            Entry old=active.get(id);
            if(old!=null&&old.when==when)active.remove(id);
            if(active.isEmpty())shutdown();
            else refresh();
            return START_NOT_STICKY;
        }
        if(!START.equals(action)){if(active.isEmpty())shutdown();return START_NOT_STICKY;}
        int id=intent.getIntExtra("id",-1);
        long when=intent.getLongExtra("when",0);
        if(id<1||id>9999||
                !WatchTimerProtocol.validWhen(when,System.currentTimeMillis())||
                acknowledged(id,when)){
            if(active.isEmpty())shutdown();
            return START_NOT_STICKY;
        }
        Entry current=active.get(id);
        if(current!=null && current.when==when)return START_NOT_STICKY;
        String name=intent.getStringExtra("name");
        active.put(id,new Entry(id,when,
                intent.getBooleanExtra("vibration",true),
                name==null||name.trim().isEmpty()?"Timer":name));
        try{
            refresh();
            Log.i("SanftweckerWatch","Watch timer alert started");
        }catch(RuntimeException ex){
            Log.e("SanftweckerWatch","Timer alert failed",ex);
            active.remove(id);
            if(active.isEmpty())shutdown();
        }
        return START_NOT_STICKY;
    }
    private void refresh(){
        foreground();
        long now=System.currentTimeMillis(),latest=now;
        for(Entry entry:active.values())latest=Math.max(latest,entry.expiresAt);
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        if(pm!=null){
            if(wakeLock==null)wakeLock=pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,"Sanftwecker:WatchTimer");
            if(wakeLock.isHeld())wakeLock.release();
            wakeLock.acquire(Math.min(MAX_MS+5_000L,Math.max(1000L,latest-now+5_000L)));
        }
        handler.removeCallbacks(pulse);
        handler.post(pulse);
    }
    private void foreground(){
        if(active.isEmpty())return;
        Entry first=active.values().iterator().next();
        getSharedPreferences(ACTIVE_PREF,MODE_PRIVATE).edit()
                .putInt("id",first.id).putLong("when",first.when)
                .putString("name",first.name).putInt("count",active.size()).apply();
        String title=active.size()==1?
                active.values().iterator().next().name+" abgelaufen":
                active.size()+" Timer abgelaufen";
        Intent show=new Intent(this,WatchMainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent open=PendingIntent.getActivity(this,4861,show,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,4862,
                new Intent(this,WatchTimerAlertService.class).setAction(STOP_ALL),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(this,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText("Timer fertig – auf der Uhr stoppen")
                .setContentIntent(open)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause,"Alarm stoppen",stop).build())
                .build();
        startForeground(NOTIFICATION_ID,notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        inForeground=true;
    }
    private boolean clockAlarmActive(){
        android.content.SharedPreferences pref=
                getSharedPreferences(WatchAlarmService.ACTIVE_PREF,MODE_PRIVATE);
        long when=pref.getLong("when",0);
        return pref.getInt("id",0)>0&&when>0 &&
                System.currentTimeMillis()>=when-15_000L &&
                System.currentTimeMillis()<when+15*60_000L;
    }
    private void vibrate(){
        if(vibrator==null||!vibrator.hasVibrator()||clockAlarmActive())return;
        try{
            long[] times={0,340,170,340};
            VibrationEffect effect=vibrator.hasAmplitudeControl()
                    ?VibrationEffect.createWaveform(times,new int[]{0,210,0,210},-1)
                    :VibrationEffect.createWaveform(times,-1);
            if(Build.VERSION.SDK_INT>=33){
                vibrator.vibrate(effect,new VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM).build());
            }else{
                vibrator.vibrate(effect,new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM).build());
            }
        }catch(RuntimeException e){
            Log.w("SanftweckerWatch","Timer haptic blocked by system",e);
        }
    }
    private void sendStopToPhone(Entry entry){
        final String payload=entry.id+":"+entry.when;
        try{
            Wearable.getNodeClient(this).getConnectedNodes().addOnSuccessListener(nodes->{
                for(com.google.android.gms.wearable.Node node:nodes)
                    Wearable.getMessageClient(this).sendMessage(node.getId(),
                            WatchTimerProtocol.WATCH_STOP,
                            payload.getBytes(StandardCharsets.UTF_8))
                            .addOnFailureListener(e->
                                    Log.w("SanftweckerWatch","Phone timer stop not delivered",e));
            }).addOnFailureListener(e->Log.w("SanftweckerWatch","Phone offline",e));
        }catch(RuntimeException ex){
            Log.w("SanftweckerWatch","Watch timer stopped locally; phone offline",ex);
        }
    }
    private boolean acknowledged(int id,long when){
        return getSharedPreferences(PREF,MODE_PRIVATE).getLong("stopped_"+id,0)==when;
    }
    private void markStopped(int id,long when){
        if(id<1||id>9999||when<=0)return;
        getSharedPreferences(PREF,MODE_PRIVATE).edit()
                .putLong("stopped_"+id,when).apply();
    }
    private void cancelVibration(){
        if(vibrator!=null&&!clockAlarmActive())
            try{vibrator.cancel();}catch(RuntimeException ignored){}
    }
    private void shutdown(){
        handler.removeCallbacks(pulse);
        getSharedPreferences(ACTIVE_PREF,MODE_PRIVATE).edit().clear().apply();
        cancelVibration();
        if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();
        if(inForeground){
            stopForeground(STOP_FOREGROUND_REMOVE);
            inForeground=false;
        }
        stopSelf();
    }
    @Override public void onDestroy(){
        handler.removeCallbacks(pulse);
        cancelVibration();
        if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
