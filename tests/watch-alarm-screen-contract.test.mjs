import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const read=path=>readFileSync(new URL('../'+path,import.meta.url),'utf8');
test('scheduled Watch alarm keeps independent vibration and visible UI operations',()=>{
    const store=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchScheduleStore.java');
    assert.match(store,/m\.setAlarmClock\(info,event\)/);
    assert.match(store,/m\.setAlarmClock\(info,screen\)/);
    assert.match(store,/m\.cancel\(receiver\(c,slot,0\)\)/);
    assert.match(store,/m\.cancel\(ringScreen\(c,slot,0\)\)/);
    const fired=store.split('public static synchronized void fired(')[1]
        ?.split('public static synchronized void restore(')[0];
    assert.ok(fired);
    assert.match(fired,/forget\(c,slot\)/);
    assert.doesNotMatch(fired,/clear\(c,slot\)/);
});
test('watch ringing screen runs over lockscreen and exposes explicit actions',()=>{
    const manifest=read('watch/src/main/AndroidManifest.xml');
    assert.match(manifest,/android:name="\.WatchRingActivity"/);
    assert.match(manifest,/android:showWhenLocked="true"/);
    assert.match(manifest,/android:turnScreenOn="true"/);
    const screen=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchRingActivity.java');
    assert.match(screen,/setShowWhenLocked\(true\)/);
    assert.match(screen,/setTurnScreenOn\(true\)/);
    assert.match(screen,/WatchAlarmService\.SNOOZE/);
    assert.match(screen,/WatchAlarmService\.STOP/);
    assert.match(screen,/WatchAlarmScreenAck\.visible\(this,id\)/);
});
test('Wear OS notification remains actionable without unsupported full-screen intents',()=>{
    const service=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchAlarmService.java');
    assert.match(service,/\.setContentIntent\(openApp\(this\)\)/);
    assert.match(service,/\.addAction\(new Notification\.Action\.Builder\(/);
    assert.match(service,/WatchRingActivity\.openIntent\(c\)/);
    assert.doesNotMatch(service,/setFullScreenIntent\(/);
    const phone=read('alarm/src/main/java/com/cobrachris/sanftwecker/WatchAcknowledgement.java');
    assert.match(phone,/SCREEN_VISIBLE/);
});


test('empty watch schedule shows a factual status without inferring a signing failure',()=>{
    const main=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchMainActivity.java');
    assert.match(main,/long next=WatchScheduleStore\.next\(this\)/);
    assert.match(main,/Kein kommender Wecker auf der Uhr/);
    assert.match(main,/Falls am Handy einer aktiv ist: Synchronisierung prüfen/);
    assert.doesNotMatch(main,/Noch kein Uhralarm synchronisiert/);
    assert.doesNotMatch(main,/Handy- und Uhr-App müssen dieselbe Signatur haben/);
    assert.match(main,/Nächster lokaler Wecker/);
    assert.match(main,/WatchAlarmService\.STOP/);
    assert.match(main,/WatchAlarmService\.SNOOZE/);
});


test('watch cascade uses finite user-counted pulses and stops all callbacks',()=>{
    const service=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchAlarmService.java');
    const schedule=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchScheduleStore.java');
    const bootstrap=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchDataBootstrap.java');
    const phone=read('alarm/src/main/java/com/cobrachris/sanftwecker/WearAlarmBridge.java');
    const ui=read('alarm/src/main/java/com/cobrachris/sanftwecker/DetailActivity.java');
    assert.match(service,/private final Runnable gentlePulse/);
    assert.match(service,/gentlePulseLimit/);
    assert.match(service,/gentleIntervalSeconds\*1000L/);
    assert.match(service,/gentlePulsesPlayed<gentlePulseLimit/);
    assert.match(service,/applyHaptic\(WatchCascade.gentleTimings\(activeIntensity\),\s*WatchCascade.gentleAmplitudes\(activeIntensity\),false\)/);
    assert.match(service,/int repeatIndex=repeat\?0:-1/);
    assert.match(service,/handler.removeCallbacks\(gentlePulse\)/);
    assert.match(service,/handler.removeCallbacks\(escalate\)/);
    assert.match(schedule,/putInt\(key\(slot,"pulse_count"\)/);
    assert.match(schedule,/putInt\(key\(slot,"pulse_interval_seconds"\)/);
    assert.match(schedule,/prefs.getInt\(key\(slot,"pulse_count"\),6\)/);
    assert.match(bootstrap,/WatchCascade.safePulses\(map.getInt\("watch_pulses",6\)\)/);
    assert.match(bootstrap,/WatchCascade.safeInterval\(map.getInt\("watch_interval_seconds",5\)\)/);
    assert.match(phone,/data.putInt\("watch_pulses"/);
    assert.match(phone,/data.putInt\("watch_interval_seconds"/);
    assert.match(ui,/Anzahl und Abstand die/);
});


test('active Watch alarm always has manual screen access from high-priority notification',()=>{
    const watch=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchAlarmService.java');
    const main=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchMainActivity.java');
    assert.match(watch,/"Alarmansicht",openApp\(this\)/);
    assert.match(watch,/NotificationManager.IMPORTANCE_HIGH/);
    assert.match(watch,/"Stoppen",stopAction\(\)/);
    assert.match(main,/openAlarmScreen.setVisibility\(ringing\?View.VISIBLE:View.GONE\)/);
    assert.match(main,/startActivity\(WatchRingActivity.openIntent\(this\)\)/);
});
test('Watch fired captures configured pulse count and interval before deleting schedule',()=>{
    const store=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchScheduleStore.java');
    const fired=store.split('public static synchronized void fired(')[1]
        ?.split('public static synchronized void restore(')[0];
    assert.ok(fired);
    assert.ok(fired.indexOf('int pulseCount=')<fired.indexOf('forget(c,slot)'));
    assert.ok(fired.indexOf('int intervalSeconds=')<fired.indexOf('forget(c,slot)'));
    assert.match(fired,/pulseCount,intervalSeconds/);
});
test('phone only offers full-screen permission when the Android grant is missing',()=>{
    const main=read('alarm/src/main/java/com/cobrachris/sanftwecker/MainActivity.java');
    const status=read('alarm/src/main/java/com/cobrachris/sanftwecker/FullScreenPermissionStatus.java');
    const receiver=read('alarm/src/main/java/com/cobrachris/sanftwecker/RescheduleReceiver.java');
    assert.match(main,/FullScreenPermissionStatus.isAllowed\(this\)/);
    assert.match(main,/fullScreenPermissionButton.setVisibility\(allowed \? View.GONE : View.VISIBLE\)/);
    assert.match(main,/fullScreenPermissionStatus.setText/);
    assert.match(status,/canUseFullScreenIntent\(\)/);
    assert.match(receiver,/Intent.ACTION_MY_PACKAGE_REPLACED/);
    assert.match(receiver,/FullScreenPermissionStatus.label/);
});


test('unlocked phone has explicit alarm-screen entry and stop without bypassing FSI',()=>{
    const main=read('alarm/src/main/java/com/cobrachris/sanftwecker/MainActivity.java');
    const service=read('alarm/src/main/java/com/cobrachris/sanftwecker/RingingService.java');
    const activity=read('alarm/src/main/java/com/cobrachris/sanftwecker/AlarmActivity.java');
    const state=read('alarm/src/main/java/com/cobrachris/sanftwecker/ActiveAlarmState.java');
    const manifest=read('alarm/src/main/AndroidManifest.xml');
    assert.match(service,/ActiveAlarmState\.start\(this,ringId,ringWhen\)/);
    assert.match(service,/ActiveAlarmState\.clear\(this\)/);
    assert.match(service,/\.setFullScreenIntent\(openApp\(\), true\)/);
    assert.match(service,/"Stoppen", STOP/);
    assert.match(service,/"Alarmansicht",openApp\(\)/);
    assert.match(main,/LAUFENDEN WECKER ÖFFNEN/);
    assert.match(main,/ActiveAlarmState\.activeId\(this\)/);
    assert.match(main,/new Intent\(this,AlarmActivity\.class\)/);
    assert.match(state,/MAX_AGE=30L\*60_000L/);
    assert.match(state,/putLong\("started_at",System\.currentTimeMillis\(\)\)/);
    assert.match(activity,/RingingService\.STOP/);
    assert.match(activity,/RingingService\.SNOOZE/);
    assert.match(manifest,/android:showWhenLocked="true"/);
    assert.match(manifest,/android:turnScreenOn="true"/);
});

test('watch notification fallback exposes user-controlled Wear OS permission repair',()=>{
    const manifest=read('watch/src/main/AndroidManifest.xml');
    const main=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchMainActivity.java');
    const service=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchAlarmService.java');
    assert.match(manifest,/android.permission.POST_NOTIFICATIONS/);
    assert.match(main,/NotificationManager/);
    assert.match(main,/areNotificationsEnabled\(\)/);
    assert.match(main,/checkSelfPermission\(Manifest.permission.POST_NOTIFICATIONS\)/);
    assert.match(main,/notificationAccess.setVisibility\(!ringing&&!notificationsAllowed\(\)/);
    assert.match(main,/setText\("Alarmmeldungen erlauben"\)/);
    assert.match(main,/notificationAccess.setOnClickListener\(v->openNotificationAccess\(\)\)/);
    assert.match(main,/requestPermissions\(new String\[\]\{Manifest.permission.POST_NOTIFICATIONS\}/);
    assert.match(main,/Settings.ACTION_APP_NOTIFICATION_SETTINGS/);
    // A declined permission must not disable local stop, snooze or vibration.
    assert.match(main,/WatchAlarmService.STOP/);
    assert.match(main,/WatchAlarmService.SNOOZE/);
    assert.match(service,/applyHaptic\(WatchCascade.strongTimings\(activeIntensity\)/);
    assert.match(service,/setContentIntent\(openApp\(this\)\)/);
    assert.doesNotMatch(main,/onCreate\([\s\S]*?requestPermissions\(/,
        'do not automatically trigger an alert permission prompt');
});
