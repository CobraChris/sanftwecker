import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const read=path=>readFileSync(new URL('../'+path,import.meta.url),'utf8');
const watch=name=>read('watch/src/main/java/com/cobrachris/watchvibrationpoc/'+name+'.java');
const notice=watch('WatchAlarmNotice');
const clock=watch('WatchAlarmService');
const timer=watch('WatchTimerAlertService');
const alarmUi=watch('WatchRingActivity');
const timerUi=watch('WatchTimerRingActivity');
const home=watch('WatchMainActivity');
const manifest=read('watch/src/main/AndroidManifest.xml');

test('Wear OS uses a separate visible notification rather than unsupported phone-style full-screen intents',()=>{
  assert.match(notice,/CHANNEL="sanftwecker_watch_visible_alarm_v1"/);
  assert.match(notice,/NotificationManager\.IMPORTANCE_HIGH/);
  assert.match(notice,/setCategory\(Notification\.CATEGORY_ALARM\)/);
  assert.match(notice,/setPriority\(Notification\.PRIORITY_HIGH\)/);
  assert.match(notice,/setVisibility\(Notification\.VISIBILITY_PUBLIC\)/);
  assert.match(notice,/setContentIntent\(open\)/);
  assert.match(notice,/setOnlyAlertOnce\(true\)/);
  assert.match(notice,/setTimeoutAfter\(maxVisibleMs\)/);
  assert.match(notice,/ch\.enableVibration\(true\)/);
  assert.match(notice,/ch\.setVibrationPattern\(new long\[\]\{0L,65L\}\)/);
  assert.doesNotMatch(notice,/setFullScreenIntent|USE_FULL_SCREEN_INTENT|startActivity\(/);
  assert.doesNotMatch(notice,/ACQUIRE_CAUSES_WAKEUP|SCREEN_BRIGHT_WAKE_LOCK/);
});

test('watch alerts never bypass user notification permission or channel settings',()=>{
  assert.match(notice,/nm\.areNotificationsEnabled\(\)/);
  assert.match(notice,/checkSelfPermission\(Manifest\.permission\.POST_NOTIFICATIONS\)/);
  assert.match(notice,/ch\.getImportance\(\)==NotificationManager\.IMPORTANCE_NONE/);
  assert.match(home,/!WatchAlarmNotice\.channelBlocked\(this\)/);
  assert.match(home,/notificationAccess\.setOnClickListener\(v->openNotificationAccess\(\)\)/);
  assert.match(notice,/catch\(RuntimeException error\)/);
  assert.match(notice,/nm\.createNotificationChannel\(ch\)/);
});

test('clock alarm notification appears only for a new valid alarm and is dismissed on stop',()=>{
  assert.match(clock,/if\(activeWhen==when&&activeId==id\)return START_NOT_STICKY/);
  assert.match(clock,/WatchAlarmNotice\.clock\(this,id,activeTitle/);
  assert.match(clock,/!"Uhr-Vibrationstest"\.equals\(activeTitle\)/);
  assert.match(clock,/WatchAlarmNotice\.clearClock\(this,oldId\)/);
  assert.match(clock,/sendToPhone\(WatchAlarmProtocol\.STOP/);
  assert.match(clock,/WatchAlarmProtocol\.SNOOZE/);
  assert.match(clock,/WatchCascade\.gentleTimings\(activeIntensity\)/);
  assert.match(notice,/CLOCK_BASE=24_000/);
  assert.match(notice,/if\(snooze!=null\)/);
  assert.match(alarmUi,/setShowWhenLocked\(true\)/);
  assert.match(alarmUi,/setTurnScreenOn\(true\)/);
});

test('timer notification opens existing live overrun window and is cleared on stop and expiry',()=>{
  assert.match(timer,/showVisibleTimerNotice\(active\.get\(id\)\)/);
  assert.match(timer,/WatchAlarmNotice\.timer\(this,entry\.id,entry\.name\+" abgelaufen",open,stop\)/);
  assert.match(timer,/WatchAlarmNotice\.clearTimer\(this,entry\.id\)/);
  assert.match(timer,/WatchAlarmNotice\.clearTimer\(this,id\)/);
  assert.match(timer,/if\(current!=null && current\.when==when\)return START_NOT_STICKY/);
  assert.match(timer,/sendStopToPhone\(entry\)/);
  assert.match(timer,/WatchTimerProtocol\.WATCH_STOP/);
  assert.match(timer,/new Intent\(this,WatchTimerAlertService\.class\)\.setAction\(STOP_ALL\)/);
  assert.match(timerUi,/setShowWhenLocked\(true\)/);
  assert.match(timerUi,/setTurnScreenOn\(true\)/);
  assert.match(timerUi,/clock\.setText\(overrun\(System\.currentTimeMillis\(\)-due\)\)/);
  assert.match(manifest,/android:name="\.WatchTimerRingActivity"\s+android:exported="false"\s+android:showWhenLocked="true"\s+android:turnScreenOn="true"/);
});

test('visual notices never replace reliable vibration foreground services',()=>{
  assert.match(clock,/startForeground\(NOTIFICATION_ID,builder\.build\(\)/);
  assert.match(clock,/PowerManager\.PARTIAL_WAKE_LOCK/);
  assert.match(timer,/startForeground\(NOTIFICATION_ID,notification/);
  assert.match(timer,/PowerManager\.PARTIAL_WAKE_LOCK/);
  assert.match(timer,/clockAlarmActive\(\)/);
  assert.match(notice,/private static final int TIMER_BASE=38_000/);
  assert.match(notice,/if\(id<1\|\|id>9999\)return/);
  assert.match(notice,/if\(id<1\|\|id>1000\)return/);
});
