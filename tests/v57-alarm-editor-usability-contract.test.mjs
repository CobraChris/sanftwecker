import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const detail=read('alarm/src/main/java/com/cobrachris/sanftwecker/DetailActivity.java');
const home=read('alarm/src/main/java/com/cobrachris/sanftwecker/MainActivity.java');
const layout=read('alarm/src/main/java/com/cobrachris/sanftwecker/AlarmDetailLayoutPolicy.java');

test('overview and editor both show unambiguous weekdays without changing saved day bits',()=>{
 for(const src of [home,detail])
   assert.match(src,/\{"Mo","Di","Mi","Do","Fr","Sa","So"\}/);
 assert.match(detail,/days\[which\]=!days\[which\]/);
 assert.match(detail,/if\(days\[i\]\)mask\|=1<<i/);
 assert.match(layout,/weekdayColumns\(int screenWidthDp,\s*float fontScale\)/);
 assert.match(detail,/getConfiguration\(\)\.fontScale/);
 assert.match(detail,/chips\[i\]\.setContentDescription|chips\[index\]\.setContentDescription/);
});

test('one permanently reachable Save button and a clear unsaved-change indicator',()=>{
 assert.equal((detail.match(/UiKit\.button\(this,"Speichern",true\)/g)||[]).length,1);
 assert.match(detail,/ScrollView scrolling=UiKit\.scroll\(this,root\)/);
 assert.match(detail,/shell\.addView\(scrolling,new LinearLayout\.LayoutParams\(/);
 assert.match(detail,/shell\.addView\(footer\)/);
 assert.match(detail,/setContentView\(shell\)/);
 assert.match(detail,/Änderungen noch nicht gespeichert/);
 assert.match(detail,/saveState\.setVisibility\(dirty\?View\.VISIBLE:View\.GONE\)/);
 assert.match(detail,/dirty = !saved\.sameConfiguration\(read\(\)\)/);
 assert.match(detail,/if\(!dirty\)\{finish\(\);return;\}/);
 assert.match(detail,/Nur die übrigen Änderungen sind noch offen/);
 assert.match(detail,/timeUndoRow\.setVisibility\(View\.VISIBLE\)/);
});

test('expert controls and explanations stay mounted but are initially hidden',()=>{
 assert.match(detail,/private LinearLayout advancedOptions\(/);
 assert.match(detail,/content\.setVisibility\(View\.GONE\)/);
 assert.match(detail,/content\.setVisibility\(expanded\?View\.VISIBLE:View\.GONE\)/);
 for(const label of ['Erweiterte Toneinstellungen','Watch-Feineinstellungen','Hinweise'])
   assert.ok(detail.includes(label),label);
 assert.match(detail,/manualToneOptions=UiKit\.column\(this\)/);
 assert.match(detail,/advancedTone\.addView\(manualToneOptions\)/);
 assert.match(detail,/volume=slider\(music,95\)/);
 assert.match(detail,/rowSwitch\(advancedTone,"Niedrigen Android-Weckerpegel anheben",audibleWhenMuted\)/);
 assert.match(detail,/watchFine\.addView\(watchIntensity\)/);
 assert.match(detail,/pulses=slider\(watchFine,12\)/);
 assert.match(detail,/interval=slider\(watchFine,WatchIntervals\.maxProgress\(\)\)/);
 assert.match(detail,/snooze=slider\(end,29\)/);
 assert.match(detail,/autoStop=slider\(end,29\)/);
 assert.match(detail,/vibrationSummary\.setText\(VibrationPresets\.LABELS/);
});

test('prominent full alarm/Watch test reuses real schedule, never a new sound simulation',()=>{
 assert.match(detail,/UiKit\.button\(this,"Weckablauf testen",false\)/);
 assert.match(detail,/startTest\.setOnClickListener\(v->testAlarm\(\)\)/);
 assert.match(detail,/private void testAlarm\(\)/);
 assert.match(detail,/if\(dirty\)\{/);
 assert.match(detail,/setTitle\("Änderungen vor dem Test speichern\?"\)/);
 assert.match(detail,/setPositiveButton\("Speichern und testen",\(dialog,which\)->\{/);
 assert.match(detail,/if\(save\(\)\)scheduleAlarmTest\(\)/);
 assert.match(detail,/private void scheduleAlarmTest\(\)/);
 assert.match(detail,/long when=AlarmScheduler\.test\(this,id\)/);
 assert.match(detail,/setOnClickListener\(v->\{\s*AlarmScheduler\.cancelTest\(this\)/);
 assert.match(detail,/Geplanten Test abbrechen/);
 assert.match(detail,/long when=AlarmScheduler\.testWhen\(this\)/);
 assert.match(detail,/if\(Build.VERSION.SDK_INT>=33&&checkSelfPermission\(Manifest.permission.POST_NOTIFICATIONS\)/);
 assert.match(detail,/Der Test selbst verändert den Schalter nicht/);
});

test('old sound, vibration, watch and alarm controls are preserved and editable',()=>{
 for(const s of ['TonePickerActivity.class','PhoneVibration.preview(this,',
   'watchTest.setOnClickListener(v->watchTest())','rowSwitch(mode,"Sanft in 2 Stufen",cascade)',
   'rowSwitch(wrist,"Uhr benachrichtigen",watch)','snoozeEnabled.setOnCheckedChangeListener',
   'audibleWhenMuted.isChecked(),cascade.isChecked()','AlarmScheduler.reschedule(this,id)',
   'Alarm zwangsweise stoppen','Berechtigungen & Einrichtung'])
    assert.ok(detail.includes(s),s);
 assert.match(detail,/if\(!dirty\)\{finish\(\);return;\}/);
 assert.match(detail,/AlarmRepository\.put\(this,update\);AlarmScheduler\.reschedule\(this,id\)/);
});
