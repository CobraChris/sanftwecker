import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const java=name=>readFileSync(new URL(
 '../alarm/src/main/java/com/cobrachris/sanftwecker/'+name+'.java',
 import.meta.url),'utf8');
const detail=java('DetailActivity');
const profile=java('AlarmProfile');
const schedule=java('AlarmScheduler');
const screen=java('AlarmActivity');
const service=java('RingingService');

test('confirming a time saves just time and activation without requiring Save',()=>{
  assert.match(detail,/new TimePickerDialog\(this,\(picker,h,m\)->\s*saveSelectedTime\(h,m\)/);
  assert.match(detail,/AlarmProfile before=AlarmRepository\.get\(this,id\)/);
  assert.match(detail,/before\.withTimeAndEnabled\(h,m\)/);
  assert.match(profile,/public AlarmProfile withTimeAndEnabled\(int newHour,int newMinute\)/);
  assert.match(profile,/return new AlarmProfile\(id,name,true,newHour,newMinute,days/);
  assert.match(profile,/changedTime\?0L:skipAt/);
  assert.match(profile,/changedTime\?0L:oneTimeAt/);
  assert.match(profile,/changedTime\?0L:replacedAt/);
  assert.match(detail,/AlarmRepository\.put\(this,after\);\s*AlarmScheduler\.reschedule\(this,id\)/);
  assert.match(detail,/enabled\.setChecked\(true\)/);
  assert.match(detail,/saved=after/);
  assert.match(detail,/changed\(\);diagnostics\(\)/);
  assert.match(schedule,/WearAlarmBridge\.schedule\(c,id,id,when\)/);
});

test('a denied permission or failed exact scheduling must never silently enable alarm',()=>{
  assert.match(detail,/if\(!AlarmScheduler\.canSchedule\(this\)\)\{\s*requestExactAlarmAccess\(\);\s*return;/);
  assert.match(detail,/AlarmRepository\.put\(this,before\);AlarmScheduler\.reschedule\(this,id\)/);
  assert.match(detail,/Weckzeit konnte nicht übernommen werden:/);
  assert.match(detail,/Die bisherige Weckzeit bleibt unverändert/);
  assert.match(detail,/ACTION_REQUEST_SCHEDULE_EXACT_ALARM/);
});

test('pending watch, tone and weekday edits stay unsaved and time can be undone',()=>{
  assert.match(detail,/undoTimeBefore=before/);
  assert.match(detail,/undoTimeAfter=after/);
  assert.match(detail,/timeUndoRow\.setVisibility\(View\.VISIBLE\)/);
  assert.match(detail,/timeUndoAction=UiKit\.action\(this,"Rückgängig"\)/);
  assert.match(detail,/timeUndoAction\.setOnClickListener\(v->undoSavedTime\(\)\)/);
  assert.match(detail,/current\.sameConfiguration\(undoTimeAfter\)/);
  assert.match(detail,/AlarmRepository\.put\(this,prior\);\s*AlarmScheduler\.reschedule\(this,id\)/);
  assert.match(detail,/if\(!dirty\)\{finish\(\);return;\}/);
  assert.match(detail,/Nur die übrigen Änderungen sind noch offen/);
  assert.match(detail,/AlarmRepository\.put\(this,update\);AlarmScheduler\.reschedule\(this,id\)/);
  assert.match(detail,/clearTimeUndo\(\);diagnostics\(\);return true;/);
  assert.match(detail,/rowSwitch\(mode,"Sanft in 2 Stufen",cascade\)/);
  assert.match(detail,/watchTest\.setOnClickListener\(v->watchTest\(\)\)/);
});

test('ordinary ring screen has a single visible anti-accidental stop method',()=>{
  assert.match(screen,/swipe\.setOnSeekBarChangeListener/);
  assert.match(screen,/if\(b\.getProgress\(\)>=90\)/);
  assert.match(screen,/swipe\.setAccessibilityDelegate\(new View\.AccessibilityDelegate\(\)\{/);
  assert.match(screen,/info\.addAction\(new AccessibilityNodeInfo\.AccessibilityAction\(/);
  assert.match(screen,/AccessibilityNodeInfo\.ACTION_CLICK,"Wecker sofort stoppen"/);
  assert.match(screen,/if\(action==AccessibilityNodeInfo\.ACTION_CLICK\)\{\s*stopAlarm\(\)/);
  assert.doesNotMatch(screen,/Alternativ: direkt stoppen/);
  assert.doesNotMatch(screen,/accessibleStop\.setOnClickListener/);
  assert.match(screen,/RingingService\.STOP/);
  assert.match(screen,/snooze\.setOnClickListener\(v->snoozeAlarm\(\)\)/);
  assert.match(service,/builder.addAction\(action\("Stoppen",\s*STOP/);
});
