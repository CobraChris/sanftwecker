import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const phone=n=>read('alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java');
const watch=n=>read('watch/src/main/java/com/cobrachris/watchvibrationpoc/'+n+'.java');
const phoneScreen=phone('TimerRingActivity');
const phoneService=phone('TimerRingingService');
const phoneSession=phone('TimerRingSession');
const clock=phone('TimerRingClock');
const watchTimer=watch('WatchTimerRingActivity');
const watchTimerService=watch('WatchTimerAlertService');
const watchAlarm=watch('WatchRingActivity');
const watchAlarmService=watch('WatchAlarmService');
const phoneListener=phone('WearPhoneListener');
const watchManifest=read('watch/src/main/AndroidManifest.xml');

test('phone timer full-screen view continuously displays true overdue time',()=>{
  assert.match(phoneScreen,/new Handler\(Looper\.getMainLooper\(\)\)/);
  assert.match(phoneScreen,/handler\.postDelayed\(this,250L\)/);
  assert.match(phoneScreen,/elapsedTime\.setText\(TimerDisplay\.overrun\(millis,fmt\)\)/);
  assert.match(phoneScreen,/TimerRingClock\.elapsed\(/);
  assert.match(clock,/sameBoot=dueElapsed>0/);
  assert.match(clock,/Math\.max\(0,now-due\)/);
  assert.match(phoneScreen,/setAutoSizeTextTypeUniformWithConfiguration\(/);
  assert.match(phoneScreen,/Typeface\.MONOSPACE/);
  assert.match(phoneScreen,/UiKit\.button\(this,"ALARM STOPPEN",true\)/);
  assert.match(phoneScreen,/setAction\(TimerRingingService\.ACTION_STOP_ALL\)/);
  assert.match(phoneScreen,/setShowWhenLocked\(true\)/);
  assert.match(phoneScreen,/setTurnScreenOn\(true\)/);
  assert.match(phoneScreen,/handler\.removeCallbacks\(ticker\)/);
});

test('the original completed occurrence is passed to alarm screen, even with timer auto-repeat',()=>{
  for(const field of [
    'EXTRA_DUE_WALL','EXTRA_DUE_ELAPSED','EXTRA_BOOT_OFFSET'
  ]){
    assert.match(phoneScreen,new RegExp('public static final String '+field));
    assert.match(phoneService,new RegExp('TimerRingActivity\\.'+field));
  }
  assert.match(phoneService,/item\.dueWallMs/);
  assert.match(phoneService,/item\.dueElapsedMs/);
  assert.match(phoneService,/item\.bootOffsetMs/);
  assert.match(phoneSession,/final long dueWallMs,dueElapsedMs,bootOffsetMs/);
  assert.match(phoneService,/active\.firstDueWallMs\(\)/);
  assert.match(phoneService,/active\.firstDueElapsedMs\(\)/);
  assert.match(phoneService,/active\.firstBootOffsetMs\(\)/);
  assert.match(phoneSession,/active\.remove\(id\)/);
});

test('watch timer notification opens live counter with local STOP and phone STOP message',()=>{
  assert.match(watchManifest,/android:name="\.WatchTimerRingActivity"/);
  assert.match(watchManifest,/android:exported="false"/);
  assert.match(watchTimerService,/new Intent\(this,WatchTimerRingActivity\.class\)/);
  assert.match(watchTimerService,/\.setContentIntent\(open\)/);
  assert.match(watchTimer,/System\.currentTimeMillis\(\)-due/);
  assert.match(watchTimer,/clock\.setText\(overrun\(/);
  assert.match(watchTimer,/handler\.postDelayed\(this,250L\)/);
  assert.match(watchTimer,/WatchTimerAlertService\.STOP_ALL/);
  assert.match(watchTimerService,/sendStopToPhone\(entry\)/);
  assert.match(watchTimerService,/WatchTimerProtocol\.WATCH_STOP/);
  assert.match(phoneListener,/TimerWearBridge\.isCurrent\(this,id,when\)/);
  assert.match(phoneListener,/TimerAlerts\.dismiss\(this,id\)/);
});

test('watch clock alarm still stops phone alarm and snoozes only when enabled',()=>{
  assert.match(watchAlarm,/WatchAlarmService\.STOP/);
  assert.match(watchAlarm,/WatchAlarmService\.SNOOZE/);
  assert.match(watchAlarm,/WatchAlarmUiPolicy\.snoozeVisible\(id,when,minutes\)/);
  assert.match(watchAlarm,/WatchDashboardView\.styleButton\(stop,false,true\)/);
  assert.match(watchAlarm,/WatchDashboardView\.styleButton\(snooze,true,false\)/);
  assert.match(watchAlarm,/alarmTime\.setText\(/);
  assert.match(watchAlarm,/alarmTitle\.setText\(/);
  assert.match(watchAlarmService,/\.putString\("title",activeTitle\)/);
  assert.match(watchAlarmService,/\.remove\("title"\)/);
  assert.match(watchAlarmService,/sendToPhone\(WatchAlarmProtocol\.STOP/);
  assert.match(watchAlarmService,/WatchAlarmProtocol\.SNOOZE/);
  assert.match(phoneListener,/watchSnooze&&\(profile==null\|\|!profile\.snoozeEnabled\)/);
  assert.match(phoneListener,/watchSnooze\?RingingService\.SNOOZE:RingingService\.STOP/);
});

test('phone and watch wake-alarm and timer actions remain separate',()=>{
  assert.doesNotMatch(clock,/RingingService|WatchAlarmService|AlarmScheduler/);
  assert.doesNotMatch(watchTimer,/WatchAlarmService\.STOP|WatchAlarmService\.SNOOZE/);
  assert.doesNotMatch(watchAlarm,/WatchTimerAlertService\.STOP_ALL/);
  assert.match(phoneService,/TimerWearBridge\.stopped\(this,id\)/);
  assert.match(watchTimerService,/clockAlarmActive\(\)/);
  assert.match(phoneSession,/MAX_RING_MS=3L\*60_000L/);
});
