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
import android.os.PowerManager;
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
    public static final String CHOICE="com.cobrachris.sanftwecker.REMINDER_WATCH_CHOICE";
    private static final String CHANNEL="sanftwecker_watch_reminder_v1";
    private static final int NOTIFY_ID=49010;
    private static final String ACK="/sanftwecker/reminder/v1/ack";
    private static final long MAX_LOCK_MS=15*60_000L+5_000L;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Vibrator motor;
    private PowerManager.WakeLock wakeLock;
    private int id,intensity,pulses,interval,played,postponeMinutes=10,autoStopMinutes=3;
    private long due;
    private boolean cascade,strong;
    private final Runnable pulse=new Runnable(){
        @Override public void run(){
            if(due<=0||played>=pulses||strong)return;
            if(!clockActive()){
                vibrate(false);
                if(played==0)sendAck("GENTLE");
                played++;
            }
            if(played<pulses)
                handler.postDelayed(this,interval*1000L);
        }
    };
    // The actual WatchAlarmService repeats the strong WatchCascade waveform
    // continuously until stop; do the SAME, not 12 finite reminder pulses.
    private final Runnable strongPulse=()->{
        if(due>0&&strong&&!clockActive()){
            vibrate(true);
            sendAck("STRONG");
        }
    };
    private final Runnable localEscalate=()->{
        if(due>0&&cascade&&!strong){
            strong=true;
            handler.removeCallbacks(pulse);
            if(motor!=null&&!clockActive())try{motor.cancel();}catch(RuntimeException ignored){}
            handler.post(strongPulse);
        }
    };
    private final Runnable autoEnd=()->stopLocal();
    private void scheduleAutoEnd(){
        handler.removeCallbacks(autoEnd);
        long duration=Math.max(cascade?2:1,Math.min(15,autoStopMinutes))*60_000L;
        long elapsed=Math.max(0L,System.currentTimeMillis()-due);
        handler.postDelayed(autoEnd,Math.max(1000L,duration-elapsed));
    }
    private final Runnable wakePriorityCheck=new Runnable(){
        @Override public void run(){
            if(due<=0)return;
            if(clockActive()){stopLocal();return;}
            handler.postDelayed(this,800L);
        }
    };
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
        // Reuse the established Watch alarm patterns for every reminder.
        long[] timing=hard?WatchCascade.strongTimings(intensity):
                WatchCascade.gentleTimings(intensity);
        int[] amplitudes=hard?WatchCascade.strongAmplitudes(intensity):
                WatchCascade.gentleAmplitudes(intensity);
        // Exactly as WatchAlarmService: repeat strong waveform from index 0.
        int repeatIndex=hard?0:-1;
        VibrationEffect pattern=motor.hasAmplitudeControl()?
            VibrationEffect.createWaveform(timing,amplitudes,repeatIndex):
            VibrationEffect.createWaveform(timing,repeatIndex);
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
    private PendingIntent options(){
        Intent open=new Intent(this,WatchReminderOptionsActivity.class)
            .putExtra("id",id).putExtra("due",due);
        return PendingIntent.getActivity(this,49511,open,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private void show(){
        Notification notification=new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Erinnerung")
            .setContentText("10 / 30 / 60 Minuten / morgen / stoppen")
            .setContentIntent(options())
            .setFullScreenIntent(options(),true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .addAction(new Notification.Action.Builder(
                android.R.drawable.ic_input_add,"Erledigt",action(DONE,49501)).build())
            .addAction(new Notification.Action.Builder(
                android.R.drawable.ic_menu_recent_history,"Optionen",options()).build())
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
        handler.removeCallbacks(pulse);handler.removeCallbacks(strongPulse);
        handler.removeCallbacks(localEscalate);
        handler.removeCallbacks(autoEnd);
        handler.removeCallbacks(wakePriorityCheck);
        if(motor!=null&&!clockActive())try{motor.cancel();}catch(RuntimeException ignored){}
        if(wakeLock!=null){
            try{if(wakeLock.isHeld())wakeLock.release();}
            catch(RuntimeException ignored){}
            wakeLock=null;
        }
        id=0;due=0;
        stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }
    private void holdCpuForReminder(){
        if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();
        PowerManager manager=(PowerManager)getSystemService(POWER_SERVICE);
        if(manager==null)return;
        wakeLock=manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "Sanftwecker:WatchReminder");
        // Same wake management as WatchAlarmService, never unbounded.
        wakeLock.acquire(MAX_LOCK_MS);
    }
    /** Acknowledgement distinguishes "message sent" from actual Watch execution. */
    private void sendAck(String phase){
        if(id<=0||due<=0)return;
        final String receipt=id+":"+due+":"+phase;
        try{
            Wearable.getNodeClient(this).getConnectedNodes()
                    .addOnSuccessListener(nodes->{
                        for(com.google.android.gms.wearable.Node node:nodes)
                            Wearable.getMessageClient(this).sendMessage(node.getId(),ACK,
                                    receipt.getBytes(StandardCharsets.UTF_8))
                                    .addOnFailureListener(error->
                                            Log.w("SanftweckerWatch","Reminder ACK failed",error));
                    })
                    .addOnFailureListener(error->
                            Log.w("SanftweckerWatch","No node for reminder ACK",error));
        }catch(RuntimeException error){
            Log.w("SanftweckerWatch","Reminder ACK unavailable",error);
        }
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        String action=intent.getAction();
        int requestedId=intent.getIntExtra("id",0);
        long when=intent.getLongExtra("due",0);
        if(CHOICE.equals(action)){
            int minutes=intent.getIntExtra("choice",0);
            if(minutes!=10&&minutes!=30&&minutes!=60&&minutes!=-1)
                return START_NOT_STICKY;
            if(requestedId!=id||when!=due||due<=0)return START_NOT_STICKY;
            reply(minutes==-1?"tomorrow":"later"+minutes);
            stopLocal();
            return START_NOT_STICKY;
        }
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
            if(requestedId==id&&when==due&&cascade&&!strong){
                strong=true;
                handler.removeCallbacks(localEscalate);
                handler.removeCallbacks(pulse);
                if(motor!=null&&!clockActive())
                    try{motor.cancel();}catch(RuntimeException ignored){}
                handler.post(strongPulse);
            }
            return START_NOT_STICKY;
        }
        if(!START.equals(action)||requestedId<1||requestedId>9999||
                when<=0||when<System.currentTimeMillis()-180_000L)return START_NOT_STICKY;
        // Legacy and extended packets describe the same reminder: do not restart
        // vibration or alter phase when a duplicate packet arrives.
        if(id==requestedId&&due==when){
            int supplied=intent.getIntExtra("autostop",0);
            if(supplied>0 && supplied!=autoStopMinutes){
                autoStopMinutes=supplied;
                scheduleAutoEnd();
            }
            return START_NOT_STICKY;
        }
        // Replacing the current reminder must not stopSelf() and kill the new one.
        if(id>0){
            // All callbacks belong to the previous occurrence. A late
            // escalation could otherwise switch the NEW reminder immediately
            // to the strong phase, and a stale priority check could stop it.
            handler.removeCallbacks(pulse);
            handler.removeCallbacks(strongPulse);
            handler.removeCallbacks(localEscalate);
            handler.removeCallbacks(autoEnd);
            handler.removeCallbacks(wakePriorityCheck);
            if(motor!=null&&!clockActive())try{motor.cancel();}catch(RuntimeException ignored){}
        }
        id=requestedId;due=when;
        cascade=intent.getBooleanExtra("cascade",true);
        intensity=Math.max(0,Math.min(2,intent.getIntExtra("intensity",1)));
        pulses=Math.max(1,Math.min(30,intent.getIntExtra("pulses",6)));
        interval=Math.max(1,Math.min(60,intent.getIntExtra("interval",5)));
        postponeMinutes=intent.getIntExtra("postpone",10);
        if(postponeMinutes!=5&&postponeMinutes!=10&&postponeMinutes!=30)postponeMinutes=10;
        played=0;
        autoStopMinutes=intent.getIntExtra("autostop",0);
        if(autoStopMinutes<1||autoStopMinutes>30)autoStopMinutes=3;
        strong=!cascade||WatchCascade.strong(due,System.currentTimeMillis());
        try{
            show();
            // The real WatchAlarmService keeps the CPU awake while haptics
            // progress through the 60-second phase boundary. Without this,
            // Android can suspend the reminder's Handler during screen-off.
            holdCpuForReminder();
            sendAck("STARTED");
        }catch(RuntimeException ex){
            Log.w("SanftweckerWatch","Reminder notification/wakelock unavailable",ex);
            sendAck("START_ERROR");
            stopLocal();
            return START_NOT_STICKY;
        }
        if(motor==null||!motor.hasVibrator())sendAck("NO_VIBRATOR");
        if(strong)handler.post(strongPulse);else handler.post(pulse);
        handler.removeCallbacks(localEscalate);
        if(cascade&&!strong)
            handler.postDelayed(localEscalate,
                    WatchCascade.untilStrong(due,System.currentTimeMillis()));
        scheduleAutoEnd();
        handler.removeCallbacks(wakePriorityCheck);
        handler.postDelayed(wakePriorityCheck,800L);
        return START_NOT_STICKY;
    }
    @Override public void onDestroy(){
        handler.removeCallbacks(pulse);handler.removeCallbacks(strongPulse);
        handler.removeCallbacks(localEscalate);
        handler.removeCallbacks(autoEnd);
        handler.removeCallbacks(wakePriorityCheck);
        if(motor!=null&&!clockActive())
            try{motor.cancel();}catch(RuntimeException ignored){}
        if(wakeLock!=null){
            try{if(wakeLock.isHeld())wakeLock.release();}
            catch(RuntimeException ignored){}
            wakeLock=null;
        }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}