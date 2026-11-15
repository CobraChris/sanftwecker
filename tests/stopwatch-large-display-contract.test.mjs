import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const get=name=>readFileSync(new URL('../alarm/src/main/java/com/cobrachris/sanftwecker/'+name+'.java',import.meta.url),'utf8');
const activity=get('StopwatchActivity');
const timer=get('TimerActivity');
const clock=get('ClockFormat');

test('stopwatch time has timer-like visual priority but fits the full width',()=>{
  assert.match(activity,/ClockFormat\.stopwatch\(s\.elapsed\([\s\S]*?\)\),44,UiKit\.BLUE\)/);
  assert.match(activity,/display\.setSingleLine\(true\)/);
  assert.match(activity,/display\.setMaxLines\(1\)/);
  assert.match(activity,/display\.setIncludeFontPadding\(false\)/);
  assert.match(activity,/display\.setTypeface\(android\.graphics\.Typeface\.MONOSPACE\)/);
  assert.match(activity,/display\.setAutoSizeTextTypeUniformWithConfiguration\(\s*22,44,1,android\.util\.TypedValue\.COMPLEX_UNIT_SP\)/);
  assert.match(activity,/display\.setMinHeight\(UiKit\.dp\(this,60\)\)/);
  assert.match(activity,/display\.setGravity\(Gravity\.CENTER\)/);
  assert.doesNotMatch(activity,/\)\),18,UiKit\.BLUE\)/);
  assert.match(timer,/value\.setAutoSizeTextTypeUniformWithConfiguration\(/);
});

test('live milliseconds and all stopwatch operations remain unchanged',()=>{
  assert.match(activity,/face\.setText\(ClockFormat\.stopwatch\(s\.elapsed\(wall,now\)\)\)/);
  assert.match(activity,/handler\.postDelayed\(this,80\)/);
  assert.match(clock,/"%02d:%02d:%02d\.%03d"/);
  assert.match(activity,/StopwatchRepository\.put\(this,s\.running\?s\.pause\(wall,now\):s\.start\(wall,now\)\)/);
  assert.match(activity,/StopwatchRepository\.put\(this,s\.lap\(wall,now\)\)/);
  assert.match(activity,/StopwatchRepository\.put\(this,s\.reset\(\)\)/);
  assert.match(activity,/screenWidthDp>=600/);
  assert.match(activity,/if\(!twoColumns\)/);
  assert.doesNotMatch(activity,/display\.setContentDescription\("Gemessene Zeit"\)/);
});
