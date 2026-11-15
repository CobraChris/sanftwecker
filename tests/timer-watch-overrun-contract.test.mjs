import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const src=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const phone=n=>src('alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java');
const watch=n=>src('watch/src/main/java/com/cobrachris/watchvibrationpoc/'+n+'.java');

test('completed timers count upwards without resetting the original deadline',()=>{
  const entry=phone('TimerEntry');
  const scheduler=phone('TimerScheduler');
  const view=phone('TimerDisplay');
  const overview=phone('TimerActivity');
  const detail=phone('TimerDetailActivity');
  assert.match(entry,/public TimerEntry finish\(\)\{[\s\S]*?FINISHED,\s*dueWallMs,dueElapsedMs,bootOffsetMs,favorite/);
  assert.match(entry,/public long overrun\(long wallNow,long elapsedNow\)/);
  assert.match(entry,/Math\.max\(0,now-due\)/);
  assert.match(view,/public static String overrun\(long elapsedMs,int format\)/);
  assert.match(view,/public static String visible\(TimerEntry item,long wallNow,long elapsedNow,int format\)/);
  assert.match(overview,/TimerDisplay\.visible\(t,wall,elapsed/);
  assert.match(overview,/TimerDisplay\.visible\(t,/);
  assert.match(detail,/TimerDisplay\.visible\(t,System\.currentTimeMillis\(\)/);
  assert.match(overview,/t\.state==TimerEntry\.FINISHED\?"Überzogen"/);
  assert.match(detail,/state\.setText\("Überzogen – seit Ablauf"\)/);
  assert.match(scheduler,/TimerRepository\.put\(c,current\.finish\(\)\)/);
  assert.match(scheduler,/TimerAlertSettings\.autoRepeat\(c,id\)/);
  assert.match(scheduler,/TimerAlertSettings\.oneShot\(c,id\)/);
  assert.match(overview,/handler\.postDelayed\(this,250\)/);
});

test('the timer explicitly sends a separate watch event with fast and durable delivery',()=>{
  const scheduler=phone('TimerScheduler');
  const bridge=phone('TimerWearBridge');
  const pkt=phone('TimerWatchPacket');
  const alarm=phone('WearAlarmBridge');
  assert.match(scheduler,/TimerWearBridge\.finished\(c,current,TimerAlertSettings\.vibration\(c,id\)\)/);
  assert.match(bridge,/Wearable\.getMessageClient\(c\)\.sendMessage\(/);
  assert.match(bridge,/Wearable\.getDataClient\(c\)\.putDataItem\(/);
  assert.match(bridge,/\.setUrgent\(\)/);
  assert.match(bridge,/TimerWatchPacket\.STATE\+id/);
  assert.match(bridge,/TimerWatchPacket\.WATCH_STOP/);
  assert.match(pkt,/PREFIX="\/sanftwecker\/timer\/v1"/);
  assert.match(pkt,/MAX_AGE_MS=180_000L/);
  assert.match(alarm,/PREFIX="\/sanftwecker\/alarm\/v1"/);
  assert.match(bridge,/public static boolean isCurrent\(Context c,int id,long when\)/);
});

test('watch listens to timer data/messages and provides ongoing local haptics',()=>{
  const listener=watch('SanftweckerWearListener');
  const bootstrap=watch('WatchDataBootstrap');
  const service=watch('WatchTimerAlertService');
  const screen=watch('WatchMainActivity');
  const manifest=src('watch/src/main/AndroidManifest.xml');
  assert.match(listener,/WatchTimerProtocol\.START\.equals\(path\)/);
  assert.match(listener,/WatchTimerProtocol\.STOP\.equals\(path\)/);
  assert.match(bootstrap,/path\.startsWith\(WatchTimerProtocol\.STATE\)/);
  assert.match(bootstrap,/WatchTimerAlertService\.start\(c,new WatchTimerProtocol\.Packet/);
  assert.match(bootstrap,/WatchTimerAlertService\.stop\(c,id,when\)/);
  assert.match(service,/ServiceInfo\.FOREGROUND_SERVICE_TYPE_SPECIAL_USE/);
  assert.match(service,/NotificationManager\.IMPORTANCE_HIGH/);
  assert.match(service,/setCategory\(Notification\.CATEGORY_ALARM\)/);
  assert.match(service,/setOngoing\(true\)/);
  assert.match(service,/Timer stoppen/);
  assert.match(service,/handler\.postDelayed\(this,INTERVAL_MS\)/);
  assert.match(service,/MAX_MS=180_000L/);
  assert.match(service,/VibrationAttributes\.USAGE_ALARM/);
  assert.match(service,/AudioAttributes\.USAGE_ALARM/);
  assert.match(service,/VibrationEffect\.createWaveform\(/);
  assert.match(service,/clockAlarmActive\(\)/);
  assert.match(service,/WatchAlarmService\.ACTIVE_PREF/);
  assert.match(screen,/stopTimer\.setText\("Timer stoppen"\)/);
  assert.match(screen,/WatchTimerAlertService\.STOP_ALL/);
  assert.match(manifest,/pathPrefix="\/sanftwecker\/timer\/v1"/);
  assert.match(manifest,/\.WatchTimerAlertService/);
  assert.match(manifest,/User-requested timer-completion vibration/);
});

test('stopping or resetting a phone timer also clears the matching watch alert',()=>{
  const alerts=phone('TimerAlerts');
  const service=phone('TimerRingingService');
  const bridge=phone('TimerWearBridge');
  const listener=phone('WearPhoneListener');
  const manifest=src('alarm/src/main/AndroidManifest.xml');
  assert.match(alerts,/TimerWearBridge\.stopped\(c,id\)/);
  assert.match(service,/TimerWearBridge\.stopped\(this,id\)/);
  assert.match(service,/for\(int id:active\.ids\(\)\)TimerWearBridge\.stopped\(this,id\)/);
  assert.match(service,/TimerWearBridge\.stopped\(\s*TimerRingingService\.this,id\)/);
  assert.match(bridge,/m\.putBoolean\("active",active\)/);
  assert.match(bridge,/TimerWatchPacket\.STOP/);
  assert.match(listener,/if\(TimerWatchPacket\.WATCH_STOP\.equals\(event\.getPath\(\)\)\)/);
  assert.match(listener,/TimerWearBridge\.isCurrent\(this,id,when\)/);
  assert.match(listener,/TimerAlerts\.dismiss\(this,id\)/);
  assert.match(manifest,/pathPrefix="\/sanftwecker\/timer\/v1"/);
  const serviceWatch=watch('WatchTimerAlertService');
  assert.match(serviceWatch,/sendStopToPhone\(entry\)/);
  assert.doesNotMatch(serviceWatch,/WatchAlarmService\.STOP|WatchAlarmService\.START|vibrator\.cancel\(\);\s*\/\/.*clock/);
});
