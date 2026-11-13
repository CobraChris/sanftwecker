import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=path=>readFileSync(new URL('../'+path,import.meta.url),'utf8');

test('watch idle view shows Update before Vibration test and hides unused Stop',()=>{
  const s=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchMainActivity.java');
  const update=s.indexOf('layout.addView(updateWatch)');
  const vibration=s.indexOf('layout.addView(local)');
  const stop=s.indexOf('layout.addView(stop,1)');
  assert.ok(update>0&&vibration>0&&stop>0);
  assert.ok(update<vibration,'Update must be first visible action at idle');
  assert.ok(vibration<stop,'Local test remains available when the alarm is idle');
  assert.match(s,/updateWatch\.setVisibility\(ringing\?View\.GONE:View\.VISIBLE\)/);
  assert.match(s,/local\.setVisibility\(ringing\?View\.GONE:View\.VISIBLE\)/);
  assert.match(s,/stop\.setVisibility\(ringing\?View\.VISIBLE:View\.GONE\)/);
  assert.match(s,/snooze\.setVisibility\(snoozeMinutes>0\?View\.VISIBLE:View\.GONE\)/);
  assert.match(s,/WatchAlarmService\.STOP/);
  assert.match(s,/WatchAlarmService\.SNOOZE/);
});
test('main view and self-updater preserve scrolling on a circular viewport',()=>{
  for(const file of ['WatchMainActivity.java','WatchSelfUpdateActivity.java']){
    const s=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/'+file);
    assert.match(s,/new ScrollView\(this\)/,file);
    assert.match(s,/setFillViewport\(false\)/,file);
    assert.match(s,/setMinimumHeight\(getResources\(\)\.getDisplayMetrics\(\)\.heightPixels\+/,file);
    assert.match(s,/Gravity\.TOP\|Gravity\.CENTER_HORIZONTAL/,file);
  }
});
test('Watch self-update check is above status; no automatic install introduced',()=>{
  const s=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchSelfUpdateActivity.java');
  assert.ok(s.indexOf('content.addView(check,')<s.indexOf('content.addView(status,'));
  assert.ok(s.indexOf('content.addView(status,')<s.indexOf('content.addView(download,'));
  assert.match(s,/setPositiveButton\("Weiter"/);
  assert.match(s,/canRequestPackageInstalls\(\)/);
  assert.match(s,/FLAG_GRANT_READ_URI_PERMISSION/);
  assert.doesNotMatch(s,/setRequireUserAction\(.*NOT_REQUIRED/);
});

test('watch prioritizes Stop above Snooze and secondary actions',()=>{
  const main=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchMainActivity.java');
  const ring=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchRingActivity.java');
  assert.match(main,/layout.addView\(stop,1\)/);
  assert.ok(main.indexOf('layout.addView(status)')<main.indexOf('layout.addView(stop,1)'));
  assert.ok(ring.indexOf('root.addView(stop,stopParams)')<
            ring.indexOf('root.addView(snooze,snoozeParams)'));
  assert.match(main,/stop.setVisibility\(ringing\?View.VISIBLE:View.GONE\)/);
  assert.match(main,/openAlarmScreen.setVisibility\(ringing\?View.VISIBLE:View.GONE\)/);
  assert.match(ring,/WatchAlarmService.STOP/);
  assert.match(ring,/WatchAlarmService.SNOOZE/);
});

test('bulk pause controls only occupy space when there is active work',()=>{
  const timer=read('alarm/src/main/java/com/cobrachris/sanftwecker/TimerActivity.java');
  const stopwatch=read('alarm/src/main/java/com/cobrachris/sanftwecker/StopwatchActivity.java');
  assert.match(timer,/timer.state==TimerEntry.RUNNING \|\| timer.state==TimerEntry.WAITING/);
  assert.match(timer,/pauseAll.setVisibility\(anyActive\?View.VISIBLE:View.GONE\)/);
  assert.match(timer,/TimerScheduler.pause\(this,t.id\)/);
  assert.match(timer,/TimerEditor.show\(this,null,this::render\)/);
  assert.match(stopwatch,/if\(stopwatch.running\)\{anyRunning=true;break;\}/);
  assert.match(stopwatch,/pauseAll.setVisibility\(anyRunning\?View.VISIBLE:View.GONE\)/);
  assert.match(stopwatch,/StopwatchRepository.put\(this,s.pause\(wall,now\)\)/);
  assert.match(stopwatch,/nameEditor\(null\)/);
});

test('alarm home retains direct emergency stop during an active alarm',()=>{
  const main=read('alarm/src/main/java/com/cobrachris/sanftwecker/MainActivity.java');
  assert.match(main,/emergencyStopButton=UiKit.button\(this,"Not-Stopp: Ton und Vibration",false\)/);
  assert.match(main,/emergencyStopButton.setVisibility\(ringing\?View.VISIBLE:View.GONE\)/);
  assert.match(main,/emergencyStopButton.setOnClickListener\(v->emergencyStop\(\)\)/);
  assert.match(main,/menu.getMenu\(\).add\("Not-Stopp: Ton und Vibration"\)/);
  assert.match(main,/PhoneVibration.stop\(\)/);
  assert.match(main,/RingingService.STOP/);
});
