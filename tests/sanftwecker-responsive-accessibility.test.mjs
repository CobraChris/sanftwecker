import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const read=path=>readFileSync(new URL('../'+path,import.meta.url),'utf8');
const base='alarm/src/main/java/com/cobrachris/sanftwecker/';
const detail=read(base+'DetailActivity.java');
const policy=read(base+'AlarmDetailLayoutPolicy.java');
const kit=read(base+'UiKit.java');
const home=read(base+'MainActivity.java');

test('weekday layout accounts for narrow window and enlarged font without clipping',()=>{
  assert.match(detail,/AlarmDetailLayoutPolicy\.weekdayColumns\(/);
  assert.match(detail,/getResources\(\)\.getConfiguration\(\)\.screenWidthDp/);
  assert.match(detail,/getResources\(\)\.getConfiguration\(\)\.fontScale/);
  assert.match(detail,/if\(i%columns==0\)/);
  assert.match(detail,/daysGrid\.addView\(dayRow\)/);
  assert.match(detail,/dayRow\.addView\(chips\[i\],p\)/);
  assert.match(detail,/chips\[i\]\.setMinWidth\(UiKit\.dp\(this,dayMin\)\)/);
  assert.match(detail,/chips\[i\]\.setMinHeight\(UiKit\.dp\(this,48\)\)/);
  assert.match(detail,/0,UiKit\.dp\(this,48\),1/);
  assert.match(policy,/MIN_TOUCH_TARGET_DP = 48/);
  assert.match(policy,/Math\.max\(MIN_TOUCH_TARGET_DP,/);
  assert.match(policy,/weekdayColumns\(int screenWidthDp, float fontScale\)/);
  assert.doesNotMatch(detail,/UiKit\.dp\(this,38\),1/);
});

test('all editable days have accessible labels and announce selected state',()=>{
  assert.match(detail,/weekdayNames=\{"Montag","Dienstag","Mittwoch"/);
  assert.match(detail,/chips\[index\]\.setSelected\(days\[index\]\)/);
  assert.match(detail,/chips\[index\]\.setContentDescription\(weekdayNames\[index\]/);
  assert.match(detail,/" ausgewählt" : " nicht ausgewählt"/);
  assert.match(detail,/chips\[i\]\.setFocusable\(true\)/);
  assert.match(detail,/applyDayState\(which\)/);
  assert.match(detail,/applyDayState\(i\)/);
  assert.match(detail,/for\(int i=0;i<7;i\+\+\)/);
});

test('large text keeps edit actions and alarm time usable',()=>{
  assert.match(detail,/pageTitle\.setContentDescription\("Wecker bearbeiten"\)/);
  assert.match(detail,/overflow\.setContentDescription\("Weitere Aktionen"\)/);
  assert.match(detail,/name\.setMinHeight\(UiKit\.dp\(this,48\)\)/);
  assert.match(detail,/clock\.setSingleLine\(true\)/);
  assert.match(detail,/clock\.setAutoSizeTextTypeUniformWithConfiguration\(/);
  assert.match(detail,/26,58,2,android\.util\.TypedValue\.COMPLEX_UNIT_SP/);
  assert.match(detail,/clock\.setMinHeight\(UiKit\.dp\(this,48\)\)/);
  assert.match(detail,/s\.setMinHeight\(UiKit\.dp\(this,48\)\)/);
  assert.match(detail,/vibration\.setMinimumHeight\(UiKit\.dp\(this,48\)\)/);
  assert.match(detail,/watchIntensity\.setMinimumHeight\(UiKit\.dp\(this,48\)\)/);
  assert.match(kit,/b\.setMinHeight\(dp\(c, 52\)\)/);
  assert.match(kit,/s\.setMinHeight\(dp\(c, 48\)\)/);
  assert.match(kit,/public static ScrollView scroll/);
  assert.match(detail,/setContentView\(UiKit\.scroll\(this,root\)\)/);
});

test('no alarm features or safety controls were lost in the responsive pass',()=>{
  assert.match(detail,/AlarmRepository\.put\(this,update\);AlarmScheduler\.reschedule\(this,id\)/);
  assert.match(detail,/new TimePickerDialog\(this/);
  assert.match(detail,/watchTest\(\)/);
  assert.match(detail,/emergencyStop\(\)/);
  assert.match(detail,/testAlarm\(\)/);
  assert.match(detail,/AlarmScheduler\.cancelTest\(this\)/);
  assert.match(detail,/startActivity\(new Intent\(this,SetupAssistantActivity\.class\)\)/);
  assert.match(detail,/if\(!dirty\)\{finish\(\);return;\}/);
  assert.match(detail,/rowSwitch\(mode,"Sanft in 2 Stufen",cascade\)/);
  assert.match(home,/AlarmScheduler\.toggle\(this,p\.id,isChecked\)/);
  assert.match(home,/subheading\.setText\("Aktive Wecker: " \+ enabledCount\)/);
});
