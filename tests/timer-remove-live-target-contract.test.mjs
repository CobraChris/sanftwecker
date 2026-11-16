import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const java=name=>readFileSync(new URL(
  '../alarm/src/main/java/com/cobrachris/sanftwecker/'+name+'.java',import.meta.url),'utf8');
const detail=java('TimerDetailActivity');
const settings=java('TimerAlertSettings');
const display=java('TimerDisplay');
const scheduler=java('TimerScheduler');
const timer=java('TimerEntry');

test('target clock never updates while timer is ready, running or finished',()=>{
  assert.doesNotMatch(detail,/Ziel-Uhrzeit bei Start jetzt|Ziel-Uhrzeit:/);
  assert.doesNotMatch(detail,/Abgelaufen um:|formatTime\(t\.dueWallMs\)/);
  assert.doesNotMatch(detail,/else if\(info==TimerAlertSettings\.EXTRA_TARGET\)/);
  assert.match(detail,/boolean showDuration=info==TimerAlertSettings\.EXTRA_DURATION/);
  assert.match(detail,/extraInfoText\.setVisibility\(showDuration\?View\.VISIBLE:View\.GONE\)/);
  assert.match(detail,/extraInfoText\.setText\("Dauer: "\+TimerDisplay\.time\(t\.durationMs/);
});

test('only original duration and none remain selectable',()=>{
  assert.match(detail,/final String\[\] values=\{"Ursprüngliche Dauer","Keine"\}/);
  assert.match(detail,/final int\[\] options=\{TimerAlertSettings\.EXTRA_DURATION,\s*TimerAlertSettings\.EXTRA_NONE\}/);
  assert.match(detail,/TimerAlertSettings\.setExtraInfo\(this,timerId,options\[which\]\)/);
  assert.doesNotMatch(detail,/final String\[\] values=\{"Ziel-Uhrzeit"/);
  assert.match(display,/if\(extra==TimerAlertSettings\.EXTRA_DURATION\)return "Ursprüngliche Dauer"/);
  assert.match(display,/return "Keine"/);
});

test('legacy stored target time becomes none without changing saved timer deadlines',()=>{
  assert.match(settings,/public static final int EXTRA_TARGET=0, EXTRA_DURATION=1, EXTRA_NONE=2/);
  assert.match(settings,/return saved==EXTRA_DURATION\?EXTRA_DURATION:EXTRA_NONE/);
  assert.match(settings,/getInt\(key\("extra",id\),EXTRA_NONE\)/);
  assert.match(settings,/return normalizeExtraInfo\(v\)/);
  assert.match(settings,/if\(value!=EXTRA_DURATION && value!=EXTRA_NONE\)/);
  assert.match(timer,/public TimerEntry finish\(\)/);
  assert.match(timer,/dueWallMs,dueElapsedMs,bootOffsetMs,favorite/);
  assert.match(scheduler,/TimerRepository\.put\(c,current\.finish\(\)\)/);
});
