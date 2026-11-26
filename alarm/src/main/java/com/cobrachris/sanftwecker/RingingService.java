package com.cobrachris.sanftwecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * Audible alarm plus direct Wear OS alarm commands with notification fallback.
 * Native Watch vibrates with USAGE_ALARM; Samsung's user DND policy remains sovereign.
 */
public final class RingingService extends Service {
    public static final String START = "com.cobrachris.sanftwecker.START";
    // Reminders use this exact foreground/audio engine, not a clone.
    public static final String REMINDER_START = "com.cobrachris.sanftwecker.REMINDER_ALARM_START";
    public static final String REMINDER_STOP = "com.cobrachris.sanftwecker.REMINDER_ALARM_STOP";
    public static final String STOP = "com.cobrachris.sanftwecker.STOP";
    public static final String STOP_PREVIEW = "com.cobrachris.sanftwecker.STOP_PREVIEW";
    public static final String SNOOZE = "com.cobrachris.sanftwecker.SNOOZE";
    public static final String RING_FINISHED = "com.cobrachris.sanftwecker.RING_FINISHED";
    public static final String WATCH_CHANNEL = "sanftwecker_watch_v3";
    public static final String EXTRA_QUICK_TEST = "quick_test";
    static final String ACTIVE_CHANNEL = "sanftwecker_active_v4";
    private static final int ONGOING_ID = 601;
    // Watch notification IDs are owned by WatchAlert.
    private static final long MAX_RING = 30L * 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private NotificationManager manager;
    private AudioTrack audio;
    private Thread writer;
    private PowerManager.WakeLock wakeLock;
    private volatile boolean active;
    private long startedAt;
    private int pulseCount;
    private int ringId = 1;
    private long ringWhen;
    private AlarmProfile profile;
    private boolean toneOnlyPreview;
    private int activeTone;
    private boolean quickTest;
    private boolean reminderActive;
    private int reminderId;
    private long reminderDue;
    private String reminderTitle;

    private final Runnable gainTick = new Runnable() {
        @Override public void run() {
            if (!active || audio == null) return;
            long elapsed = profile.cascadeEnabled ?
                    WakeCascade.elapsedSinceAlarm(ringWhen,System.currentTimeMillis(),
                            SystemClock.elapsedRealtime()-startedAt) :
                    SystemClock.elapsedRealtime()-startedAt;
            float gain = AlarmEnvelope.gain(profile,
                    toneOnlyPreview || (quickTest && !profile.cascadeEnabled), elapsed);
            try { audio.setVolume(gain); }
            catch (IllegalStateException e) { Log.w("Sanftwecker", "Audio gain update", e); }
            if (elapsed < MAX_RING) handler.postDelayed(this, 200L);
        }
    };

    private final Runnable pulse = new Runnable() {
        @Override public void run() {
            if (!active || reminderActive || profile.cascadeEnabled || profile.watchPulses == 0 ||
                    pulseCount >= profile.watchPulses) return;
            WatchAlert.send(RingingService.this, profile, pulseCount++);
            if (pulseCount < profile.watchPulses) {
                handler.postDelayed(this, profile.watchIntervalSeconds * 1000L);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        ensureChannels();
    }

    public static void ensureChannels(android.content.Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel running = new NotificationChannel(
                ACTIVE_CHANNEL, "Aktiver Alarm", NotificationManager.IMPORTANCE_HIGH);
        running.setSound(null, null);
        running.enableVibration(false);
        running.setDescription("Laufender Wecker mit immer sichtbarer Stoppen-Aktion");
        nm.createNotificationChannel(running);
        WatchAlert.ensureChannel(c);
    }

    private void ensureChannels() { ensureChannels(this); }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (REMINDER_STOP.equals(action)) {
            int id=intent.getIntExtra(ReminderScheduler.ID,0);
            long due=intent.getLongExtra(ReminderScheduler.DUE,0L);
            if(active&&reminderActive&&reminderId==id&&reminderDue==due){
                AlarmRepository.diagnostic(this,"Erinnerungston gestoppt: "+reminderTitle);
                stopPlaying();stopSelf();
            }else if(!active)stopSelf();
            return START_NOT_STICKY;
        }
        if (STOP_PREVIEW.equals(action)) {
            // A picker must NEVER stop a real alarm that started during its preview.
            if(active && toneOnlyPreview){stopPlaying();stopSelf();}
            else if(!active)stopSelf(); // Never retain an idle started service.
            return START_NOT_STICKY;
        }
        if (STOP.equals(action) || SNOOZE.equals(action)) {
            // A wake-alarm stop must not accidentally stop a reminder.
            if(reminderActive)return START_NOT_STICKY;
            // Watch-originated stops must match the active alarm occurrence.
            long expectedWhen=intent.getLongExtra(AlarmScheduler.EXTRA_WHEN,0);
            if(expectedWhen>0&&(!active||expectedWhen!=ringWhen||
                    intent.getIntExtra(AlarmScheduler.EXTRA_ID,-1)!=ringId))
                return START_NOT_STICKY;
            int id = ringId;
            int minutes = profile == null ? 5 : profile.snoozeMinutes;
            boolean snoozing = SNOOZE.equals(action) && active && !toneOnlyPreview &&
                    profile != null && profile.snoozeEnabled;
            // Never replace a rejected snooze by an accidental permanent stop.
            if (SNOOZE.equals(action) && !snoozing) {
                AlarmRepository.diagnostic(this,
                        "Schlummern abgewiesen: kein aktiver schlummerbarer Alarm");
                return START_NOT_STICKY;
            }
            long snoozeUntil=0L;
            if (snoozing) {
                try { snoozeUntil=AlarmScheduler.snooze(this, id, minutes); }
                catch (RuntimeException e) {
                    // Keep the audible alarm and wrist alarm active if exact
                    // scheduling failed. No misleading 'snoozed' state.
                    AlarmRepository.diagnostic(this, "FEHLER: Schlummeralarm konnte nicht geplant werden");
                    Log.e("Sanftwecker", "Snooze failed", e);
                    return START_NOT_STICKY;
                }
            }
            if (!snoozing) AlarmScheduler.cancelSnooze(this);
            AlarmRepository.diagnostic(this,
                    snoozing ? "Schlummern aktiv" : "Alarm gestoppt");
            stopPlaying();
            if(snoozing)SnoozeNotice.show(this,id,snoozeUntil);
            stopSelf();
            return START_NOT_STICKY;
        }
        final boolean reminderRequested=REMINDER_START.equals(action);
        if (!START.equals(action) && !reminderRequested) {
            if(!active)stopSelf();
            return START_NOT_STICKY;
        }
        int requestedId = reminderRequested
                ?intent.getIntExtra(ReminderScheduler.ID,-1)
                :intent.getIntExtra(AlarmScheduler.EXTRA_ID,1);
        long requestedDue=reminderRequested?
                intent.getLongExtra(ReminderScheduler.DUE,0L):0L;
        ReminderStore.Item reminder=null;
        AlarmProfile requestedProfile;
        if(reminderRequested){
            reminder=ReminderStore.get(this,requestedId);
            if(reminder==null||reminder.done||reminder.dueAt!=requestedDue
                    ||requestedDue<=0){
                if(!active)stopSelf();
                return START_NOT_STICKY;
            }
            if(active&&reminderActive&&reminderId==requestedId
                    &&reminderDue==requestedDue)return START_NOT_STICKY;
            // Never replace a true wake alarm with a reminder.
            if(active&&!reminderActive&&!toneOnlyPreview){
                AlarmRepository.diagnostic(this,
                        "Erinnerung unterdrückt: echter Wecker hat Vorrang");
                return START_NOT_STICKY;
            }
            requestedProfile=ReminderAlarmConfig.profile(this);
        }else{
            requestedProfile=AlarmRepository.get(this,requestedId);
            if (requestedId<1||requestedId>AlarmProfile.MAX_ALARMS
                    ||requestedProfile==null){
                if(!active)stopSelf();
                return START_NOT_STICKY;
            }
        }
        boolean previewRequested=!reminderRequested
                &&intent.getBooleanExtra("tone_only_preview",false);
        if(active && !toneOnlyPreview && previewRequested){
            AlarmRepository.diagnostic(this,"Tonprobe blockiert: echter Alarm klingelt");
            return START_NOT_STICKY;
        }
        if(active)stopPlaying();
        reminderActive=reminderRequested;
        reminderId=reminderRequested?requestedId:0;
        reminderDue=reminderRequested?requestedDue:0L;
        reminderTitle=reminderRequested?reminder.title:null;
        ringId=requestedProfile.id;
        ringWhen=reminderRequested?requestedDue:
                intent.getLongExtra(AlarmScheduler.EXTRA_WHEN,0L);
        profile=requestedProfile;
        toneOnlyPreview=previewRequested;
        activeTone=toneOnlyPreview?ToneCatalog.safe(
                intent.getIntExtra("tone_preview_index",profile.tone)):profile.tone;
        quickTest=!reminderRequested&&intent.getBooleanExtra(EXTRA_QUICK_TEST,false);
        final long ringDuration=WakeCascade.duration(toneOnlyPreview,quickTest,
                profile.cascadeEnabled,profile.autoStopMinutes);
        try {
            // Store the active occurrence before Android can open the
            // foreground notification. This is only a UI fallback, not alarm scheduling.
            if(!toneOnlyPreview && !reminderActive){
                ActiveAlarmState.start(this,ringId,ringWhen);
                // A wake alarm always preempts reminder vibration.
                PhoneVibration.stop();
            }
            startForegroundNotification();
            active = true;
            startedAt = SystemClock.elapsedRealtime();
            pulseCount = reminderActive || toneOnlyPreview || profile.cascadeEnabled ?
                    profile.watchPulses : Math.min(1, profile.watchPulses);
            AlarmRepository.diagnostic(this, (reminderActive?
                    "Erinnerung über RingingService: "+reminderTitle+
                        " / Vorlage "+profile.name+" / Kaskade "+profile.cascadeEnabled+
                        " / Ton "+profile.tone+" / Lautstärke "+profile.maxVolume:
                    "Audiodienst gestartet: "+profile.name+
                    (toneOnlyPreview?" (Tonprobe)":quickTest?" (Kurztest)":"")));
            try {
                acquireWakeLock();
            } catch (RuntimeException e) {
                Log.w("Sanftwecker", "WakeLock unavailable; continue with active service", e);
            }
            // Diagnose routing without ever silently changing Do Not Disturb.
            try {
                android.media.AudioManager system=(android.media.AudioManager)
                        getSystemService(AUDIO_SERVICE);
                if(system!=null){
                    int level=system.getStreamVolume(android.media.AudioManager.STREAM_ALARM);
                    int max=system.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM);
                    if(max>0&&level<=max/3)
                        AlarmRepository.diagnostic(this,
                                "HINWEIS: Android-Weckerpegel sehr niedrig: "+level+"/"+max);
                }
                if(manager!=null && manager.getCurrentInterruptionFilter()!=
                        NotificationManager.INTERRUPTION_FILTER_ALL)
                    AlarmRepository.diagnostic(this,
                            "HINWEIS: Nicht stören aktiv – Alarm-Ausnahmen auf Handy/Uhr prüfen");
            }catch(RuntimeException error){
                Log.w("Sanftwecker","System routing diagnostics unavailable",error);
            }
            // Explicit opt-in: temporarily raise a very low ALARM stream, never DND.
            if(profile.audibleWhenMuted) {
                if(AlarmVolumeGuard.ensureAudible(this))
                    AlarmRepository.diagnostic(this,
                            "Niedrigen Android-Weckerpegel vorübergehend angehoben");
                else
                    AlarmRepository.diagnostic(this,
                            "Weckerlautstärke unverändert – Android-Alarmpegel / DND prüfen");
            }
            // Ordinary alarms retain their previous immediate vibration.
            // Cascade alarms are completely vibration-free on the handset in
            // minute one; phase two begins exactly 60 s after alarm start.
            if(!toneOnlyPreview && !profile.cascadeEnabled){
                if(reminderActive)
                    PhoneVibration.startReminder(this,profile.vibrationMode,
                            reminderId,reminderDue);
                else PhoneVibration.start(this,profile.vibrationMode);
            }else if(!toneOnlyPreview && profile.cascadeEnabled){
                final int expectedId=reminderActive?reminderId:ringId;
                final long expectedWhen=ringWhen;
                final long expectedStart=startedAt;
                final boolean expectedReminder=reminderActive;
                final int strongMode=WakeCascade.phoneMode(profile.vibrationMode);
                handler.postDelayed(()->{
                    if(active && !toneOnlyPreview && profile!=null &&
                            profile.cascadeEnabled && reminderActive==expectedReminder &&
                            (reminderActive?reminderId:ringId)==expectedId &&
                            ringWhen==expectedWhen && startedAt==expectedStart &&
                            WakeCascade.strongStage(
                                    WakeCascade.elapsedSinceAlarm(
                                            ringWhen,System.currentTimeMillis(),
                                            SystemClock.elapsedRealtime()-startedAt))){
                        if(reminderActive)
                            PhoneVibration.startReminder(this,strongMode,
                                    reminderId,reminderDue);
                        else PhoneVibration.start(this,strongMode);
                        if(reminderActive)ReminderPlaybackStatus.phone(this,
                                reminderId,reminderDue,"STUFE_2_HANDY");
                        AlarmRepository.diagnostic(this,
                                "Kaskade Phase 2: Handyvibration gestartet");
                    }
                },WakeCascade.untilStrong(ringWhen,System.currentTimeMillis(),
                        SystemClock.elapsedRealtime()-startedAt));
            }
            // Even if audio hardware fails, keep the watch alerts and stop/snooze controls.
            try {
                beginAudio();
                handler.post(gainTick);
                if(reminderActive){
                    ReminderPlaybackStatus.phone(this,reminderId,reminderDue,
                            profile.cascadeEnabled?"STUFE_1_TON":"TON_AKTIV");
                    AlarmRepository.diagnostic(this,
                            "Erinnerung Phase 1 gestartet: "+reminderTitle+
                            " / Uhrstart angefordert");
                }
                AlarmRepository.diagnostic(this, "AudioTrack initialisiert: " + profile.name);
            } catch (RuntimeException audioError) {
                Log.e("Sanftwecker", "Audio unavailable, watch alerts still active", audioError);
                if(reminderActive)
                    ReminderPlaybackStatus.phone(this,reminderId,reminderDue,"TON_FEHLER");
                AlarmRepository.diagnostic(this, "WARNUNG: Audio fehlgeschlagen; Watch-Impulse laufen");
            }
            if (!reminderActive && !toneOnlyPreview && pulseCount < profile.watchPulses)
                handler.postDelayed(pulse, profile.watchIntervalSeconds * 1000L);
            handler.postDelayed(() -> {
                AlarmRepository.diagnostic(this, "Alarm automatisch beendet");
                stopPlaying();
                stopSelf();
            }, ringDuration);
        } catch (RuntimeException e) {
            Log.e("Sanftwecker", "Could not launch foreground alarm", e);
            if(reminderActive)
                ReminderPlaybackStatus.phone(this,reminderId,reminderDue,"START_FEHLER");
            AlarmRepository.diagnostic(this, "FEHLER: Hintergrundalarm konnte nicht starten");
            // Retain the receiver-issued watch notification if foreground start failed.
            stopPlaying(false);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void acquireWakeLock() {
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (power != null) {
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sanftwecker:Ringing");
            wakeLock.acquire(MAX_RING + 5_000L);
        }
    }

    private PendingIntent serviceAction(String action, int id) {
        return PendingIntent.getService(this, id,
                new Intent(this, RingingService.class).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent openApp() {
        if(reminderActive){
            Intent open=new Intent(this,ReminderOptionsActivity.class)
                    .putExtra(ReminderScheduler.ID,reminderId)
                    .putExtra(ReminderScheduler.DUE,reminderDue)
                    .putExtra(ReminderOptionsActivity.EXTRA_FULLSCREEN,true);
            return PendingIntent.getActivity(this,160000+reminderId,open,
                    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        }
        Intent open = new Intent(this, AlarmActivity.class)
                .putExtra(AlarmScheduler.EXTRA_ID, ringId)
                .putExtra("from_alarm", true);
        return PendingIntent.getActivity(this, 1600 + ringId, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification.Action action(String label, String type, int icon, int id) {
        return new Notification.Action.Builder(icon, label, serviceAction(type, id)).build();
    }

    private void startForegroundNotification() {
        Notification.Builder builder = new Notification.Builder(this, ACTIVE_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(reminderActive?"Erinnerung: "+reminderTitle:
                        toneOnlyPreview?"Sanftwecker-Tonprobe":profile.name+" klingelt")
                .setContentText(reminderActive?"Wie Testwecker • später / erledigt / stoppen":
                        toneOnlyPreview?"Tonprobe – jederzeit stoppen":
                        "Zum Bedienen antippen • Stoppen / Schlummern")
                .setContentIntent(openApp())
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setLocalOnly(true);
        if(reminderActive){
            builder.addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_media_pause,"Erinnerung stoppen",
                    ReminderScheduler.pending(this,reminderId,reminderDue,
                            ReminderScheduler.STOP)).build());
            builder.addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_menu_recent_history,
                    "Später / Erledigt",openApp()).build());
        }else{
            builder.addAction(action("Stoppen",STOP,android.R.drawable.ic_media_pause,612));
        }
        if (!toneOnlyPreview) {
            // Same system-managed full-screen notification as a real Testwecker.
            builder.setFullScreenIntent(openApp(), true);
            builder.addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_lock_idle_alarm,
                    reminderActive?"Erinnerung bedienen":"Alarmansicht",openApp()).build());
            if (!reminderActive && profile.snoozeEnabled)
                builder.addAction(action("Schlummern", SNOOZE, android.R.drawable.ic_media_play, 613));
        }
        Notification running = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ONGOING_ID, running,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(ONGOING_ID, running);
        }
    }

    private void beginAudio() {
        final int rate = 22050;
        int minBytes = AudioTrack.getMinBufferSize(rate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBytes <= 0) throw new IllegalStateException("Audio device unavailable");
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(rate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        audio = new AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(Math.max(minBytes, rate / 5 * 2))
                .setTransferMode(AudioTrack.MODE_STREAM).build();
        if (audio.getState() != AudioTrack.STATE_INITIALIZED) {
            throw new IllegalStateException("AudioTrack was not initialized");
        }
        audio.setVolume(0f);
        audio.play();
        AudioTrack sink = audio;
        int tone = activeTone;
        writer = new Thread(() -> {
            short[] buffer = new short[2205];
            long sampleIndex = 0;
            while (active && !Thread.currentThread().isInterrupted()) {
                for (int i = 0; i < buffer.length; i++, sampleIndex++) {
                    double t = sampleIndex / (double) rate;
                    buffer[i] = AlarmPcm.sample(tone,t);
                }
                try { if (sink.write(buffer, 0, buffer.length) < 0) break; }
                catch (IllegalStateException e) { break; }
            }
        }, "SanftweckerTone");
        writer.start();
    }

    private void stopPlaying() { stopPlaying(true); }

    private void stopPlaying(boolean removeWatchNotifications) {
        boolean wasActive=active;
        final boolean wasReminder=reminderActive;
        final int previousReminderId=reminderId;
        final long previousReminderDue=reminderDue;
        if (wasActive) {
            AlarmRepository.diagnostic(this,wasReminder?
                    "Erinnerungston beendet":"Ton und Watch-Impulse beendet");
            if(!wasReminder&&!toneOnlyPreview&&ringWhen>0)
                WearAlarmBridge.alarmStopped(this,ringId,ringWhen);
        }
        active=false;
        if(wasReminder)PhoneVibration.stopReminder(this,previousReminderId,previousReminderDue);
        else{
            ActiveAlarmState.clear(this);
            PhoneVibration.stop();
        }
        AlarmVolumeGuard.restore(this);
        if(wasActive&&!wasReminder)
            sendBroadcast(new Intent(RING_FINISHED).setPackage(getPackageName()));
        handler.removeCallbacksAndMessages(null);
        AudioTrack old = audio;
        audio = null;
        if (old != null) {
            try { old.pause(); old.flush(); }
            catch (RuntimeException e) { Log.w("Sanftwecker", "Stopping audio", e); }
        }
        if (writer != null) {
            writer.interrupt();
            try { writer.join(300L); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            writer = null;
        }
        if (old != null) {
            try { old.release(); }
            catch (RuntimeException e) { Log.w("Sanftwecker", "Audio release", e); }
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
        if (removeWatchNotifications && !wasReminder && manager != null)
            for (int i = 0; i < 12; i++) manager.cancel(WatchAlert.ID_BASE + i);
        reminderActive=false;reminderId=0;reminderDue=0L;reminderTitle=null;
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    @Override public void onDestroy() {
        stopPlaying();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
