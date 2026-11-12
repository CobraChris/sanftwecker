import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=path=>readFileSync(new URL('../'+path,import.meta.url),'utf8');

test('watch idle view shows Update before Vibration test and hides unused Stop',()=>{
  const s=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchMainActivity.java');
  const update=s.indexOf('layout.addView(updateWatch)');
  const vibration=s.indexOf('layout.addView(local)');
  const stop=s.indexOf('layout.addView(stop)');
  assert.ok(update>0&&vibration>0&&stop>0);
  assert.ok(update<vibration,'Update must be first visible action at idle');
  assert.ok(vibration<stop,'Local test can remain an alternative action');
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
