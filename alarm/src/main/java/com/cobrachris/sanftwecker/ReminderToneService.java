package com.cobrachris.sanftwecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/** Dedicated bounded reminder sound playback. Never owns wake-alarm audio. */
public final class ReminderToneService extends Service {
    public static final String START="com.cobrachris.sanftwecker.REMINDER_TONE_START";
    public static final String STOP="com.cobrachris.sanftwecker.REMINDER_TONE_STOP";
    private static final String CHANNEL="sanftwecker_reminder_tone_playback_v1";
    private static final int NOTIFICATION=99111;
    private static final int RATE=22050;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private volatile boolean playing;
    private int activeId;
    private long activeDue;
    private AudioTrack sink;
    private Thread writer;
    private boolean unifiedRamp;
    private long rampDue;
    private final Runnable gainTick=new Runnable(){
        @Override public void run(){
            if(!playing||sink==null||!unifiedRamp)return;
            try{
                sink.setVolume(GentleGain.level(
                    Math.max(0L,System.currentTimeMillis()-rampDue),0,60,42));
            }catch(IllegalStateException failure){
                Log.w("Sanftwecker","Reminder volume ramp unavailable",failure);
            }
            handler.postDelayed(this,250L);
        }
    };
    // Trigger phase 2 from the running alarm service, just like the wake alarm.
    // The independent AlarmManager escalation remains a fallback if audio is off.
    private final Runnable strongPhase=new Runnable(){
        @Override public void run(){
            if(!playing||!unifiedRamp||activeId<=0||activeDue<=0)return;
            ReminderStore.Item live=ReminderStore.get(ReminderToneService.this,activeId);
            if(live==null||live.done||live.dueAt!=activeDue||!live.cascade)return;
            if(ActiveAlarmState.activeId(ReminderToneService.this)>0)return;
            if(WakeCascade.strongStage(
                    WakeCascade.elapsedSinceAlarm(activeDue,System.currentTimeMillis(),0)))
                PhoneVibration.startReminder(ReminderToneService.this,
                        WakeCascade.phoneMode(0),activeId,activeDue);
        }
    };
    private final Runnable finish=this::stopAudio;
    private final Runnable wakePriorityCheck=new Runnable(){
        @Override public void run(){
            if(!playing)return;
            if(ActiveAlarmState.activeId(ReminderToneService.this)>0){
                stopAudio();return;
            }
            handler.postDelayed(this,800L);
        }
    };

    public static void start(Context c,ReminderStore.Item item){
        if(item==null||item.done||!item.phoneSound)return;
        Intent i=new Intent(c,ReminderToneService.class).setAction(START)
            .putExtra(ReminderScheduler.ID,item.id)
            .putExtra(ReminderScheduler.DUE,item.dueAt);
        try{c.startForegroundService(i);}
        catch(RuntimeException failure){
            Log.w("Sanftwecker","Reminder tone unavailable",failure);
        }
    }
    public static void stop(Context c,ReminderStore.Item item){
        if(item==null)return;
        Intent i=new Intent(c,ReminderToneService.class).setAction(STOP)
            .putExtra(ReminderScheduler.ID,item.id)
            .putExtra(ReminderScheduler.DUE,item.dueAt);
        try{c.startService(i);}catch(RuntimeException ignored){}
    }
    @Override public void onCreate(){
        super.onCreate();
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(nm!=null){
            NotificationChannel channel=new NotificationChannel(CHANNEL,
                "Erinnerungston wird abgespielt",NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null,null);
            channel.enableVibration(false);
            nm.createNotificationChannel(channel);
        }
    }
    @Override public int onStartCommand(Intent i,int flags,int startId){
        if(i==null)return START_NOT_STICKY;
        int id=i.getIntExtra(ReminderScheduler.ID,0);
        long due=i.getLongExtra(ReminderScheduler.DUE,0);
        if(STOP.equals(i.getAction())){
            if(activeId==id&&activeDue==due)stopAudio();
            return START_NOT_STICKY;
        }
        if(!START.equals(i.getAction()))return START_NOT_STICKY;
        ReminderStore.Item reminder=ReminderStore.get(this,id);
        if(reminder==null||reminder.done||reminder.dueAt!=due||
                !reminder.phoneSound||!ToneCatalog.valid(reminder.tone)){
            stopSelf();return START_NOT_STICKY;
        }
        // The strong phase may retry a service already running; never reset the ramp.
        if(playing&&activeId==id&&activeDue==due)return START_NOT_STICKY;
        // Genuine wake alarms take precedence. Do not mix the reminder's
        // waveform with a user-scheduled alarm's critical audio.
        if(ActiveAlarmState.activeId(this)>0){stopSelf();return START_NOT_STICKY;}
        resetAudio(false);
        activeId=id;activeDue=due;
        unifiedRamp=reminder.cascade&&reminder.unifiedCascade;
        rampDue=reminder.dueAt;
        Notification notice=new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Erinnerungston")
            .setContentText(reminder.title)
            .setOngoing(true)
            .build();
        try{
            startForeground(NOTIFICATION,notice,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            beginAudio(reminder.tone);
            if(unifiedRamp){
                handler.post(gainTick);
                handler.postDelayed(strongPhase,
                        WakeCascade.untilStrong(rampDue,System.currentTimeMillis(),0));
            }
            handler.postDelayed(finish,unifiedRamp?180_000L:90_000L);
            handler.post(wakePriorityCheck);
        }catch(RuntimeException problem){
            Log.w("Sanftwecker","Reminder audio startup failed",problem);
            stopAudio();
        }
        return START_NOT_STICKY;
    }
    private void beginAudio(int tone){
        int min=AudioTrack.getMinBufferSize(RATE,AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if(min<=0)throw new IllegalStateException("No audio output");
        AudioAttributes attrs=new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        AudioFormat format=new AudioFormat.Builder()
                .setSampleRate(RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        AudioTrack track=new AudioTrack.Builder().setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes(Math.max(min,RATE/5*2))
                .setTransferMode(AudioTrack.MODE_STREAM).build();
        if(track.getState()!=AudioTrack.STATE_INITIALIZED){
            track.release();throw new IllegalStateException("Audio output not initialized");
        }
        sink=track;playing=true;
        track.setVolume(unifiedRamp?0f:.42f);
        track.play();
        writer=new Thread(()->{
            short[] samples=new short[2205];
            long pos=0;
            while(playing&&!Thread.currentThread().isInterrupted()){
                for(int k=0;k<samples.length;k++,pos++){
                    double t=pos/(double)RATE;
                    samples[k]=(short)(Short.MAX_VALUE*.35*ToneCatalog.sample(tone,t));
                }
                try{if(track.write(samples,0,samples.length)<0)break;}
                catch(IllegalStateException error){break;}
            }
        },"SanftweckerReminderTone");
        writer.start();
    }
    private void stopAudio(){resetAudio(true);}
    private void resetAudio(boolean endService){
        playing=false;
        handler.removeCallbacks(finish);
        handler.removeCallbacks(gainTick);
        handler.removeCallbacks(strongPhase);
        handler.removeCallbacks(wakePriorityCheck);
        AudioTrack old=sink;sink=null;
        if(old!=null){
            try{old.pause();old.flush();}catch(RuntimeException ignored){}
        }
        Thread worker=writer;writer=null;
        if(worker!=null){
            worker.interrupt();
            try{worker.join(250L);}catch(InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
        if(old!=null)try{old.release();}catch(RuntimeException ignored){}
        activeId=0;activeDue=0;
        unifiedRamp=false;
        rampDue=0L;
        if(endService){
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }
    @Override public void onDestroy(){
        playing=false;handler.removeCallbacks(finish);
        handler.removeCallbacks(gainTick);
        handler.removeCallbacks(wakePriorityCheck);
        AudioTrack old=sink;sink=null;
        if(old!=null){
            try{old.pause();old.flush();old.release();}catch(RuntimeException ignored){}
        }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}