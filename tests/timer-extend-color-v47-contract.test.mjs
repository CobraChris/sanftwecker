import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const get=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const phone='alarm/src/main/java/com/cobrachris/sanftwecker/';
const watch='watch/src/main/java/com/cobrachris/watchvibrationpoc/';

test('expiry-only extension cannot retrigger an old or unrelated timer',()=>{
 const scheduler=get(phone+'TimerScheduler.java');
 const entry=get(phone+'TimerEntry.java');
 const receiver=get(phone+'TimerReceiver.java');
 assert.match(scheduler,/before\.state!=TimerEntry\.FINISHED/);
 assert.match(scheduler,/before\.dueWallMs!=expectedDue/);
 assert.match(scheduler,/arm\(c,after\)/);
 assert.match(scheduler,/TimerAlerts\.dismiss\(c,id\)/);
 assert.match(entry,/originalDurationMs/);
 assert.match(entry,/extendFinished\(/);
 assert.match(entry,/reset\(\)/);
 assert.match(receiver,/ACTION_EXTEND/);
 assert.match(receiver,/30000\|60000\|120000\|300000\|600000|add==30_000L/);
});

test('phone + watch offer extension controls without silent installation',()=>{
 const ui=get(phone+'TimerRingActivity.java');
 const service=get(phone+'TimerRingingService.java');
 const wear=get(watch+'WatchTimerRingActivity.java');
 const listener=get(phone+'WearPhoneListener.java');
 assert.match(ui,/\+30 Sek\./);
 assert.match(ui,/\+1 Min\./);
 assert.match(ui,/Mehr: \+2 \/ \+5 \/ \+10 Min\./);
 assert.match(service,/extendAction\(30_000L\)/);
 assert.match(service,/extendAction\(60_000L\)/);
 assert.match(wear,/WatchTimerProtocol\.WATCH_EXTEND/);
 assert.match(wear,/active\.getInt\("count",0\)!=1/);
 assert.match(listener,/TimerWatchPacket\.WATCH_EXTEND/);
 assert.match(listener,/TimerWearBridge\.isCurrent/);
});

test('color palette per timer does not change countdown state or user data',()=>{
 const settings=get(phone+'TimerAlertSettings.java');
 const details=get(phone+'TimerDetailActivity.java');
 const cards=get(phone+'TimerActivity.java');
 const store=get(phone+'TimerRepository.java');
 assert.match(settings,/COLOR_CHOICES/);
 assert.match(settings,/setColor\(Context c,int id,int color\)/);
 assert.match(settings,/putInt\(key\("color",targetId\),color\(c,sourceId\)\)/);
 assert.match(settings,/remove\(key\("color",id\)\)/);
 assert.match(details,/TimerAlertSettings\.setColor/);
 assert.match(cards,/TimerAlertSettings\.color/);
 assert.match(store,/originalDuration/);
});
