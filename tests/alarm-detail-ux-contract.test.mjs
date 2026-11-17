import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const detail=read('alarm/src/main/java/com/cobrachris/sanftwecker/DetailActivity.java');
const home=read('alarm/src/main/java/com/cobrachris/sanftwecker/MainActivity.java');

test('detail starts with compact alarm, explicit mode and four expandable settings',()=>{
  assert.match(detail,/Wecker bearbeiten/);
  assert.match(detail,/top\.setPadding\(UiKit\.dp\(this,16\),UiKit\.dp\(this,12\)/);
  assert.match(detail,/clock=UiKit\.label\(this,"07:00",58,UiKit\.BLUE\)/);
  assert.match(detail,/LinearLayout mode=section\(root,"Weckmodus"\)/);
  assert.match(detail,/rowSwitch\(mode,"Sanft in 2 Stufen",cascade\)/);
  for(const title of ['Weckton & Lautstärke','Handy-Vibration','Galaxy Watch',
                      'Schlummern & Stoppen'])
    assert.ok(detail.includes('foldSection(root,"'+title+'"'),title);
  assert.match(detail,/content\.setVisibility\(View\.GONE\)/);
  assert.match(detail,/content\.setVisibility\(open\?View\.VISIBLE:View\.GONE\)/);
  assert.match(detail,/header\.setFocusable\(true\)/);
  assert.equal((detail.match(/UiKit\.button\(this,"Speichern",true\)/g)||[]).length,1);
  assert.doesNotMatch(detail,/Beta – bisherigen zuverlässigen Wecker beibehalten/);
  assert.doesNotMatch(detail,/ALARM \/ VIBRATION SOFORT STOPPEN/);
});

test('cascade only hides irrelevant manual sound controls and retains stored values',()=>{
  assert.match(detail,/manualToneOptions\.setVisibility\(gentle\?View\.GONE:View\.VISIBLE\)/);
  assert.match(detail,/cascade\.setOnCheckedChangeListener\(\(b,on\)->\{/);
  assert.match(detail,/delay=slider\(manualToneOptions,30\)/);
  assert.match(detail,/ramp=slider\(manualToneOptions,29\)/);
  assert.match(detail,/volume=slider\(music,95\)/);
  assert.match(detail,/watchIntensity\.setSelection\(p\.watchIntensity\)/);
  assert.match(detail,/cascade\.setChecked\(p\.cascadeEnabled\)/);
  assert.match(detail,/loading=false;dirty=false;clearTimeUndo\(\);updateLabels\(\);refreshModeUi\(\);diagnostics\(\)/);
  assert.match(detail,/audibleWhenMuted\.isChecked\(\),cascade\.isChecked\(\)/);
  assert.match(detail,/AlarmRepository\.put\(this,update\);AlarmScheduler\.reschedule\(this,id\)/);
  assert.match(detail,/if\(!dirty\)\{finish\(\);return;\}/);
});

test('tests, emergency stop, watch and permission repair remain reachable',()=>{
  for(const action of ['Wecker in 30 Sekunden testen','Berechtigungen & Einrichtung',
          'Diagnose-Protokoll anzeigen','Geplanten Test abbrechen',
          'Alarm zwangsweise stoppen'])
    assert.ok(detail.includes('menu.getMenu().add("'+action+'")'),action);
  assert.match(detail,/startActivity\(new Intent\(this,SetupAssistantActivity\.class\)\)/);
  assert.match(detail,/PhoneVibration\.stop\(\)/);
  assert.match(detail,/AlarmScheduler\.cancelTest\(this\)/);
  assert.match(detail,/RingingService\.STOP/);
  assert.match(detail,/menu.getMenu\(\)\.add\("Duplizieren"\)/);
  assert.match(detail,/menu.getMenu\(\)\.add\("Nächsten Termin überspringen"\)/);
  assert.match(detail,/menu.getMenu\(\)\.add\("Löschen"\)/);
  assert.match(detail,/Voreinstellung: 12 Impulse · alle 5 Sek/);
  assert.match(detail,/watchTest\.setOnClickListener\(v->watchTest\(\)\)/);
  assert.match(home,/AlarmScheduler\.toggle\(this,p\.id,isChecked\)/);
});
