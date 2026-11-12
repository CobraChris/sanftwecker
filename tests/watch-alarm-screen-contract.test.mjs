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
