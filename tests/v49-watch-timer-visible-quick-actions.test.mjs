import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const watch=n=>read('watch/src/main/java/com/cobrachris/watchvibrationpoc/'+n+'.java');

test('round Watch timer places both quick actions ahead of STOP and extras',()=>{
 const ui=watch('WatchTimerRingActivity');
 const i30=ui.indexOf('quick.addView(plus30,left)');
 const i60=ui.indexOf('quick.addView(plus60,right)');
 const stop=ui.indexOf('page.addView(stop,stopParams)');
 const more=ui.indexOf('page.addView(more,');
 assert.ok(i30>0&&i60>i30&&stop>i60&&more>stop);
 assert.match(ui,/quick\.setOrientation\(LinearLayout\.HORIZONTAL\)/);
 assert.match(ui,/plus30\.setText\("\+30 Sek\."\)/);
 assert.match(ui,/plus60\.setText\("\+1 Min\."\)/);
 assert.match(ui,/WatchTimerAlertService\.STOP_ALL/);
 assert.match(ui,/active\.getInt\("count",0\)!=1/);
 assert.match(ui,/WatchTimerProtocol\.WATCH_EXTEND/);
});

test('both Wear OS notification paths expose single-timer quick actions safely',()=>{
 const service=watch('WatchTimerAlertService');
 const notice=watch('WatchAlarmNotice');
 assert.match(service,/public static final String EXTEND=/);
 assert.match(service,/if\(EXTEND\.equals\(action\)\)/);
 assert.match(service,/active\.size\(\)==1 && current!=null && current\.when==when/);
 assert.match(service,/extension==30_000L\|\|extension==60_000L/);
 assert.match(service,/WatchTimerProtocol\.WATCH_EXTEND/);
 assert.match(service,/active\.size\(\)==1\?extendAction\(entry,30_000L\):null/);
 assert.match(service,/active\.size\(\)==1\?extendAction\(entry,60_000L\):null/);
 assert.match(service,/notificationBuilder\.addAction/);
 assert.match(notice,/PendingIntent plus30,PendingIntent plus60/);
 assert.match(notice,/if\(plus30!=null&&plus60!=null\)/);
 assert.match(notice,/setTimeoutAfter\(190_000L\)/);
 assert.match(notice,/ensureChannel\(c,nm\)/);
 assert.match(service,/\.setAction\(STOP_ALL\)/);
});

test('Watch dashboard also shows actions for single timer but not clock alarm',()=>{
 const home=watch('WatchMainActivity');
 assert.match(home,/quickTimer\.setVisibility\(timerActionVisibility\)/);
 assert.match(home,/timerState\.getInt\("count",0\)==1/);
 assert.match(home,/!ringing&&timerRinging/);
 assert.match(home,/timerPlus30\.setOnClickListener\(v->extendCurrentTimer\(30_000L\)\)/);
 assert.match(home,/timerPlus60\.setOnClickListener\(v->extendCurrentTimer\(60_000L\)\)/);
 assert.match(home,/WatchTimerAlertService\.EXTEND/);
 assert.match(home,/WatchTimerAlertService\.STOP_ALL/);
 assert.match(home,/new Intent\(this,WatchTimerRingActivity\.class\)/);
});

test('phone validates expiry identity and only extends the completed occurrence',()=>{
 const listener=read('alarm/src/main/java/com/cobrachris/sanftwecker/WearPhoneListener.java');
 const scheduler=read('alarm/src/main/java/com/cobrachris/sanftwecker/TimerScheduler.java');
 assert.match(listener,/TimerWatchPacket\.WATCH_EXTEND\.equals\(event\.getPath\(\)\)/);
 assert.match(listener,/TimerWearBridge\.isCurrent\(this,id,due\)/);
 assert.match(listener,/TimerScheduler\.extendFinished\(this,id,due,extra\)/);
 assert.match(scheduler,/before\.state!=TimerEntry\.FINISHED/);
 assert.match(scheduler,/before\.dueWallMs!=expectedDue/);
 assert.match(scheduler,/TimerAlerts\.dismiss\(c,id\)/);
});
