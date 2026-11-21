package com.cobrachris.watchvibrationpoc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.VibrationAttributes;
import android.media.AudioAttributes;
import android.util.Log;
import com.google.android.gms.wearable.Wearable;
import java.nio.charset.StandardCharsets;

/** Reminder-only Watch haptics, isolated from the wake alarm and timer services. */
public final class WatchReminderService extends Service {
    public static final String START="com.cobrachris.sanftwecker.REMINDER_WATCH_START";
    public static final String STRONG="com.cobrachris.sanftwecker.REMINDER_WATCH_STRONG";
    public static final String STOP="com.cobrachris.sanftwecker.REMINDER_WATCH_STOP";
    public static final String DONE="com.cobrachris.sanftwecker.REMINDER_WATCH_DONE";
    public static final String LATER="com.cobrachris.sanftwecker.REMINDER_WATCH_LATER";
    public static final String USER_STOP="com.cobrachris.sanftwecker.REMINDER_WATCH_USER_STOP";
    private static final String CHANNEL="sanftwecker_watch_reminder_v1";
    private static final int NOTIFY_ID=49010;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Vibrator motor;
    private int id,intensity,pulses,interval,played;
    private long due;
    private boolean cascade,strong;
    private final Runnable pulse=new Runnable(){
        @Override public void run(){
            if(due<=0||played>=pulses||strong)return;
            if(!clockActive()){
                vibrate(false);
                played++;
            }
            handler.postDelayed(this,interval*1000L);
        }
    };
    private final Runnable autoEnd=()->stopLocal();
    @Override public void onCreate(){
        super.onCreate();
        if(Build.VERSION.SDK_INT>=31){
            VibratorManager manager=(VibratorManager)getSystemService(VIBRATOR_MANAGER_SERVICE);
            motor=manager==null?null:manager.getDefaultVibrator();
        }else motor=(Vibrator)getSystemService(VIBRATOR_SERVICE);
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(manager!=null){
            NotificationChannel channel=new NotificationChannel(CHANNEL,
                "Erinnerungen auf der Uhr",NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null,null);channel.enableVibration(false);
            manager.createNotificationChannel(channel);
        }
    }
    private boolean clockActive(){
        android.content.SharedPreferences p=getSharedPreferences(
            WatchAlarmService.ACTIVE_PREF,MODE_PRIVATE);
        return p.getInt("id",0)>0&&p.getLong("when",0)>0;
    }
    private void vibrate(boolean hard){
        if(clockActive()||motor==null||!motor.hasVibrator())return;
        int level=hard?255:intensity==0?45:intensity==1?80:110;
        long[] timing=hard?new long[]{0,440,150,440}:new long[]{0,120};
        VibrationEffect pattern=motor.hasAmplitudeControl()?
            VibrationEffect.createWaveform(timing,
                hard?new int[]{0,level,0,level}:new int[]{0,level},-1):
            VibrationEffect.createWaveform(timing,-1);
        try{
            if(Build.VERSION.SDK_INT>=33)
                motor.vibrate(pattern,new VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM).build());
            else motor.vibrate(pattern,new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM).build());
        }catch(RuntimeException error){
            Log.w("SanftweckerWatch","Reminder vibration unavailable",error);
        }
    }
    private PendingIntent action(String which,int request){
        Intent intent=new Intent(this,WatchReminderService.class).setAction(which)
            .putExtra("id",id).putExtra("due",due);
        return PendingIntent.getService(this,request,intent,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private void show(){
        Notification notification=new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Erinnerung")
            .setContentText("Erledigt, später oder nur Alarm stoppen")
            .setCategory(Notification.CATEGORY_REMINDER)
            .setOngoing(true)
            .addAction(new Notification.Action.Builder(
                android.R.drawable.ic_input_add,"Erledigt",action(DONE,49501)).build())
            .addAction(new Notification.Action.Builder(
                android.R.drawable.ic_menu_recent_history,"+10 Min.",action(LATER,49502)).build())
            .addAction(new Notification.Action.Builder(
                android.R.drawable.ic_media_pause,"Stoppen",action(USER_STOP,49503)).build())
            .build();
        startForeground(NOTIFY_ID,notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
    }
    private void reply(String action){
        final String message=id+":"+due+":"+action;
        try{
            Wearable.getNodeClient(this).getConnectedNodes().addOnSuccessListener(nodes->{
                for(com.google.android.gms.wearable.Node node:nodes)
                    Wearable.getMessageClient(this).sendMessage(node.getId(),
                        "/sanftwecker/reminder/v1/action",
                        message.getBytes(StandardCharsets.UTF_8));
            });
        }catch(RuntimeException e){
            Log.w("SanftweckerWatch","Reminder request not delivered",e);
        }
    }
    private void stopLocal(){
        handler.removeCallbacks(pulse);handler.removeCallbacks(autoEnd);
        if(motor!=null&&!clockActive())try{motor.cancel();}catch(RuntimeException ignored){}
        id=0;due=0;
        stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        String action=intent.getAction();
        int requestedId=intent.getIntExtra("id",0);
        long when=intent.getLongExtra("due",0);
        if(DONE.equals(action)||LATER.equals(action)||USER_STOP.equals(action)){
            if(requestedId!=id||when!=due||due<=0)return START_NOT_STICKY;
            reply(DONE.equals(action)?"done":LATER.equals(action)?"later":"stop");
            stopLocal();
            return START_NOT_STICKY;
        }
        if(STOP.equals(action)){
            if(requestedId==id&&when==due)stopLocal();
            return START_NOT_STICKY;
        }
        if(STRONG.equals(action)){
            if(requestedId==id&&when==due&&cascade){
                strong=true;handler.removeCallbacks(pulse);vibrate(true);
            }
            return START_NOT_STICKY;
        }
        if(!START.equals(action)||requestedId<1||requestedId>9999||
                when<=0||when<System.currentTimeMillis()-180_000L)return START_NOT_STICKY;
        if(id>0&&due!=when)stopLocal();
        id=requestedId;due=when;
        cascade=intent.getBooleanExtra("cascade",true);
        intensity=Math.max(0,Math.min(2,intent.getIntExtra("intensity",1)));
        pulses=Math.max(1,Math.min(30,intent.getIntExtra("pulses",6)));
        interval=Math.max(1,Math.min(60,intent.getIntExtra("interval",5)));
        played=0;strong=!cascade;
        show();
        if(strong)vibrate(true);else handler.post(pulse);
        handler.removeCallbacks(autoEnd);
        handler.postDelayed(autoEnd,180_000L);
        return START_NOT_STICKY;
    }
    @Override public void onDestroy(){
        handler.removeCallbacks(pulse);handler.removeCallbacks(autoEnd);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}