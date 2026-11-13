import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const file=n=>read('alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java');

test('native system bar and keyboard insets protect all phone screens',()=>{
  const kit=file('UiKit');
  assert.match(kit,/WindowCompat\.setDecorFitsSystemWindows\(c\.getWindow\(\), false\)/);
  assert.match(kit,/ViewCompat\.setOnApplyWindowInsetsListener\(content/);
  assert.match(kit,/WindowInsetsCompat\.Type\.systemBars\(\)/);
  assert.match(kit,/WindowInsetsCompat\.Type\.displayCutout\(\)/);
  assert.match(kit,/WindowInsetsCompat\.Type\.ime\(\)/);
  assert.match(kit,/Math\.max\(bars\.bottom,keyboard\.bottom\)/);
  assert.match(kit,/view\.setPadding\(bars\.left,bars\.top,bars\.right/);
  assert.match(kit,/ViewCompat\.requestApplyInsets\(content\)/);
  for(const n of ['MainActivity','TimerActivity','StopwatchActivity','WorldClockActivity']){
    assert.match(file(n),/UiKit\.window\(this\)/,n);
  }
});

test('alarm card replaces technical details with live remaining hours and minutes',()=>{
  const main=file('MainActivity');
  const countdown=file('AlarmCountdown');
  assert.match(main,/AlarmCountdown\.label\(AlarmRepository\.next/);
  assert.match(main,/countdownHandler\.postDelayed\(this, 15_000L\)/);
  assert.match(main,/countdownHandler\.removeCallbacks\(countdownTick\)/);
  assert.match(main,/@Override public void onPause\(\)/);
  assert.match(main,/countdownLabels\.clear\(\)/);
  assert.match(countdown,/long minutes = diff \/ 60000L \+ \(diff % 60000L == 0 \? 0 : 1\)/);
  assert.match(countdown,/Klingelt in /);
  assert.doesNotMatch(main,/"Ton \+" \+ p\.delaySeconds/);
  assert.doesNotMatch(main,/"Vibration: "\+VibrationPresets\.LABELS/);
  assert.match(main,/ACHTUNG: Wecker nicht eingeplant/);
  assert.match(main,/Ein Termin wird übersprungen/);
  assert.match(main,/Nächste Auslösung einmalig geändert/);
  assert.match(main,/Klingelbildschirm erlauben/);
});

test('timer cards appear in two columns on phones with short readable controls',()=>{
  const timer=file('TimerActivity');
  assert.match(timer,/for\(int i=0;i<timers\.size\(\);i\+=2\)/);
  assert.match(timer,/row\.addView\(card\(timers\.get\(i\)\),weighted\(\)\)/);
  assert.match(timer,/if\(i\+1<timers\.size\(\)\)row\.addView\(card\(timers\.get\(i\+1\)\),weighted\(\)\)/);
  assert.match(timer,/title\.setMaxLines\(2\)/);
  assert.match(timer,/title\.setEllipsize\(TextUtils\.TruncateAt\.END\)/);
  assert.match(timer,/title\.setContentDescription\(t\.name\)/);
  assert.match(timer,/value\.setAutoSizeTextTypeUniformWithConfiguration\(/);
  assert.match(timer,/Button restart=UiKit\.button\(this,"Neu starten",false\)/);
  assert.match(timer,/restart\.setSingleLine\(true\)/);
  assert.match(timer,/restart\.setContentDescription\("Timer sofort von vorne starten"\)/);
  assert.match(timer,/TimerScheduler\.restartNow\(this,id\)/);
});
test('stopwatch stays full-width on phone and navigation contrast improves',()=>{
  const stopwatch=file('StopwatchActivity');
  const tabs=file('ClockTabs');
  assert.match(stopwatch,/screenWidthDp>=600/);
  assert.match(stopwatch,/if\(!twoColumns\)\{[\s\S]*?cards\.addView\(card\(/);
  assert.match(stopwatch,/StopwatchRepository\.put\(this,s\.pause\(wall,now\)\)/);
  assert.match(tabs,/i==selected\?UiKit\.BLUE:Color\.rgb\(194,200,219\)/);
});
