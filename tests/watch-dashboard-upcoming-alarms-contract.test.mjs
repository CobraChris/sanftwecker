import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const watch=name=>readFileSync(new URL(
  '../watch/src/main/java/com/cobrachris/watchvibrationpoc/'+name+'.java',
  import.meta.url),'utf8');
const manifest=readFileSync(new URL('../watch/src/main/AndroidManifest.xml',
  import.meta.url),'utf8');
const main=watch('WatchMainActivity');
const dashboard=watch('WatchDashboardView');
const agenda=watch('WatchAgenda');
const store=watch('WatchScheduleStore');
const bootstrap=watch('WatchDataBootstrap');
const alarm=watch('WatchAlarmService');

test('watch home actually renders next alarm and all locally scheduled future alarms',()=>{
  assert.match(main,/dashboard=new WatchDashboardView\(this\)/);
  assert.match(main,/dashboard\.render\(WatchScheduleStore\.upcoming\(this\)\)/);
  assert.match(store,/public static java\.util\.List<WatchAgenda\.Entry> upcoming\(Context c\)/);
  assert.match(store,/WatchAgenda\.upcoming\(store\(c\)\.getAll\(\),System\.currentTimeMillis\(\)\)/);
  assert.match(dashboard,/NÄCHSTER WECKER/);
  assert.match(dashboard,/nextTime\.setText\(time\(next\.when\)\)/);
  assert.match(dashboard,/nextName\.setText\(next\.name\)/);
  assert.match(dashboard,/for\(WatchAgenda\.Entry a:items\)/);
  assert.match(dashboard,/entries\.addView\(row,separated\(getContext\(\),9\)\)/);
  assert.match(dashboard,/setAutoSizeTextTypeUniformWithConfiguration\(/);
  assert.match(dashboard,/setContentDescription\(a\.name/);
  assert.match(main,/new ScrollView\(this\)/);
  assert.match(main,/setFillViewport\(false\)/);
});

test('scheduled agenda is a validated read-only snapshot and never edits alarm store',()=>{
  assert.match(agenda,/List<Entry> upcoming\(Map<String,\?> saved,long now\)/);
  assert.match(agenda,/!key\.matches\("slot_\[0-9\]\{1,4\}_when"\)/);
  assert.match(agenda,/WatchAlarmProtocol\.validSlot\(slot\)/);
  assert.match(agenda,/if\(when<=now\)continue/);
  assert.match(agenda,/id<1\|\|id>1000/);
  assert.match(agenda,/Comparator\.comparingLong/);
  assert.match(agenda,/Collections\.unmodifiableList\(list\)/);
  assert.doesNotMatch(agenda,/AlarmManager|PendingIntent|\.edit\(\)|\.commit\(\)/);
  assert.match(dashboard,/deaktivierte Wecker bleiben am Handy/);
  assert.match(dashboard,/Keine? kommender Wecker auf der Uhr/);
  assert.match(bootstrap,/WatchScheduleStore\.plan\(/);
});

test('critical watch stop and snooze controls remain above scrolling dashboard',()=>{
  assert.match(main,/layout\.addView\(status\)/);
  assert.match(main,/layout\.addView\(stop,1\)/);
  assert.match(main,/layout\.addView\(snooze,2\)/);
  assert.match(main,/layout\.addView\(openAlarmScreen,3\)/);
  assert.match(main,/dashboard\.setVisibility\(ringing\|\|timerRinging\?View\.GONE:View\.VISIBLE\)/);
  assert.match(main,/stopTimer\.setVisibility\(!ringing&&timerRinging\?View.VISIBLE:View.GONE\)/);
  assert.match(main,/WatchAlarmService\.STOP/);
  assert.match(main,/WatchAlarmService\.SNOOZE/);
  assert.match(main,/WatchTimerAlertService\.STOP_ALL/);
  assert.match(main,/startActivity\(WatchRingActivity\.openIntent\(this\)\)/);
  assert.match(alarm,/class WatchAlarmService extends Service/);
});

test('maintenance actions, notification consent and original signing identity survive',()=>{
  assert.ok(main.indexOf('layout.addView(updateWatch)')<
        main.indexOf('layout.addView(local)'));
  assert.match(main,/new Intent\(this,WatchSelfUpdateActivity\.class\)/);
  assert.match(main,/Alarmvibration testen/);
  assert.match(main,/notificationAccess\.setOnClickListener\(v->openNotificationAccess\(\)\)/);
  assert.match(main,/checkSelfPermission\(Manifest\.permission\.POST_NOTIFICATIONS\)/);
  assert.match(main,/WatchDataBootstrap\.refresh\(this,\(\)->runOnUiThread\(this::refresh\)\)/);
  assert.match(manifest,/\.WatchAlarmService/);
  assert.match(manifest,/\.WatchTimerAlertService/);
  assert.match(manifest,/android:name="\.WatchRingActivity"/);
  assert.doesNotMatch(main,/WatchScheduleStore\.(?:plan|clear|fired)\(/);
});
