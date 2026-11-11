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
