import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const java=name=>readFileSync(new URL(
  '../alarm/src/main/java/com/cobrachris/sanftwecker/'+name+'.java',
  import.meta.url),'utf8');
const source=path=>readFileSync(new URL('../'+path,import.meta.url),'utf8');
const service=java('TimerRingingService');
const screen=java('TimerRingActivity');
const session=java('TimerRingSession');
const alert=java('TimerAlerts');
const timer=java('TimerScheduler');
const details=java('TimerDetailActivity');
const manifest=source('alarm/src/main/AndroidManifest.xml');

test('timer completion no longer ends with one transient notification',()=>{
  assert.match(timer,/TimerAlerts\.show\(c,current\)/);
  assert.match(alert,/TimerRingingService\.start\(c,timer,tone,vibration\)/);
  assert.match(alert,/showFallback\(c,timer\.id,timer\.name,tone,vibration\)/);
  assert.match(alert,/TimerRingingService\.dismiss\(c,id\)/);
  assert.match(service,/c\.startForegroundService\(i\)/);
  assert.match(service,/startForeground\(NOTIFICATION_ID,notification/);
  assert.match(service,/ServiceInfo\.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK/);
  assert.match(service,/NotificationManager\.IMPORTANCE_HIGH/);
  assert.match(service,/setCategory\(Notification\.CATEGORY_ALARM\)/);
  assert.match(service,/setOngoing\(true\)/);
  assert.match(service,/\.addAction\(new Notification\.Action\.Builder\(/);
  assert.match(service,/android\.R\.drawable\.ic_media_pause,"Stoppen",stopAll\(\)/);
  assert.match(service,/setVisibility\(Notification\.VISIBILITY_PUBLIC\)/);
});

test('dedicated service repeats alarm-stream sound and direct, finite vibration',()=>{
  assert.match(service,/new ToneGenerator\(AudioManager\.STREAM_ALARM,90\)/);
  assert.match(service,/generator\.startTone\(ToneGenerator\.TONE_PROP_BEEP,850\)/);
  assert.match(service,/handler\.postDelayed\(this,REPEAT_MS\)/);
  assert.match(service,/VibrationEffect\.createWaveform\(/);
  assert.match(service,/new long\[\]\{0,620,200,620\},-1/);
  assert.match(service,/AudioAttributes\.USAGE_ALARM/);
  assert.match(service,/PowerManager\.PARTIAL_WAKE_LOCK/);
  assert.match(service,/wakeLock\.acquire\(TimerRingSession\.MAX_RING_MS\+5_000L\)/);
  assert.match(session,/MAX_RING_MS=3L\*60_000L/);
  assert.match(session,/public boolean expire\(long elapsedNow\)/);
  assert.match(service,/active\.expire\(SystemClock\.elapsedRealtime\(\)\)/);
  assert.match(service,/handler\.removeCallbacks\(pulse\)/);
  assert.match(service,/stopForeground\(STOP_FOREGROUND_REMOVE\)/);
  assert.match(service,/if\(motor!=null && ActiveAlarmState\.activeId\(this\)==0\)/);
  assert.match(service,/active\.wantsTone\(\)/);
  assert.match(service,/active\.wantsVibration\(\)/);
});

test('lock screen offers one-tap STOP but never dismisses security',()=>{
  assert.match(manifest,/android:name="\.TimerRingActivity" android:exported="false"/);
  assert.match(manifest,/android:showWhenLocked="true" android:turnScreenOn="true"/);
  assert.match(manifest,/android:name="\.TimerRingingService" android:exported="false"/);
  assert.match(manifest,/android:foregroundServiceType="mediaPlayback"/);
  assert.match(screen,/setShowWhenLocked\(true\)/);
  assert.match(screen,/setTurnScreenOn\(true\)/);
  assert.match(screen,/UiKit\.button\(this,"ALARM STOPPEN",true\)/);
  assert.match(screen,/setAction\(TimerRingingService\.ACTION_STOP_ALL\)/);
  assert.match(screen,/setOnClickListener\(v->\{/);
  assert.match(service,/manager\.canUseFullScreenIntent\(\)/);
  assert.match(service,/builder\.setFullScreenIntent\(openAlarm\(\),true\)/);
  assert.match(service,/setContentIntent\(openAlarm\(\)\)/);
  assert.match(screen,/public void onBackPressed\(\)/);
  assert.doesNotMatch(screen,/KeyguardManager|requestDismissKeyguard|disableKeyguard/);
});

test('multiple timers, individual reset and existing wake alarm remain separate',()=>{
  assert.match(service,/new TimerRingSession\(\)/);
  assert.match(session,/LinkedHashMap<Integer,Entry>/);
  assert.match(session,/public void remove\(int id\)/);
  assert.match(session,/public void removeAll\(\)/);
  assert.match(timer,/TimerAlerts\.dismiss\(c,id\)/);
  assert.match(timer,/TimerRepository\.put\(c,current\.finish\(\)\)/);
  assert.match(service,/ActiveAlarmState\.activeId\(this\)>0/);
  assert.doesNotMatch(service,/RingingService\.STOP|PhoneVibration\.stop|WearAlarmBridge/);
  assert.match(details,/maximal 3 Minuten/);
  for(const permission of [
    'android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK',
    'android.permission.FOREGROUND_SERVICE',
    'android.permission.WAKE_LOCK',
    'android.permission.VIBRATE',
    'android.permission.USE_FULL_SCREEN_INTENT',
    'android.permission.SCHEDULE_EXACT_ALARM'
  ])assert.ok(manifest.includes(permission),permission);
  assert.match(manifest,/android:stopWithTask="false"/);
});
