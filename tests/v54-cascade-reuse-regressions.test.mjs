import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const phone=n=>readFileSync(new URL('../alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java',import.meta.url),'utf8');
const watch=n=>readFileSync(new URL('../watch/src/main/java/com/cobrachris/watchvibrationpoc/'+n+'.java',import.meta.url),'utf8');

test('previously saved reminders automatically run the SAME cascade engine as alarms',()=>{
    const store=phone('ReminderStore'),screen=phone('ReminderActivity');
    assert.match(store,/p.getBoolean\(id\+"_cascade",true\)\);/);
    assert.doesNotMatch(store,/p.getBoolean\(id\+"_unified_cascade",false\)/);
    assert.match(screen,/cascade.setChecked\(existing==null\|\|existing.cascade\)/);
    assert.doesNotMatch(screen,/sameAsAlarm/);
    assert.match(screen,/Wecker-Kaskade: Tonrampe/);
    assert.match(screen,/item.cascade\?" · Wecker-Kaskade"/);
    assert.match(screen,/tone.getSelectedItemPosition\(\)\);/);
});

test('reminder notification uses exact working clock-alarm channel when it first rings',()=>{
    const clock=phone('RingingService'),reminder=phone('ReminderNotice');
    assert.match(clock,/static final String ACTIVE_CHANNEL = "sanftwecker_active_v4"/);
    assert.match(reminder,/RingingService.ensureChannels\(c\)/);
    assert.match(reminder,/RingingService.ACTIVE_CHANNEL/);
    assert.match(reminder,/Notification.CATEGORY_ALARM/);
    assert.match(reminder,/if\(justFired\)builder.setFullScreenIntent\(fullView,true\)/);
    assert.match(reminder,/ReminderOptionsActivity.class/);
    assert.match(reminder,/BASE\+10_000\+item.id/);
    assert.match(phone('ReminderOptionsActivity'),/setShowWhenLocked\(true\)/);
    assert.match(phone('ReminderOptionsActivity'),/setTurnScreenOn\(true\)/);
});

test('same timed 60s escalation as wake alarm without relying only on a second broadcast',()=>{
    const tone=phone('ReminderToneService'),recv=phone('ReminderReceiver'),
          vibration=phone('PhoneVibration'),sched=phone('ReminderScheduler');
    assert.match(tone,/GentleGain.level\(/);
    assert.match(tone,/WakeCascade.untilStrong\(rampDue,System.currentTimeMillis\(\),0\)/);
    assert.match(tone,/handler.postDelayed\(strongPhase,/);
    assert.match(tone,/WakeCascade.strongStage\(/);
    assert.match(tone,/PhoneVibration.startReminder\(/);
    assert.match(tone,/handler.removeCallbacks\(strongPhase\)/);
    assert.match(recv,/ReminderScheduler.ESCALATE.equals\(action\)/);
    assert.match(sched,/item.dueAt\+WakeCascade.SECOND_STAGE_MS/);
    assert.match(vibration,/if\(alarmRunning&&reminderId==id&&reminderWhen==due\)return true/);
    assert.match(vibration,/ActiveAlarmState.activeId\(c\)>0/);
    assert.match(recv,/ReminderScheduler.DONE.equals\(action\)/);
    assert.match(recv,/ReminderScheduler.STOP.equals\(action\)/);
    assert.match(recv,/ReminderScheduler.CHOICE.equals\(action\)/);
});

test('watch reuses original WatchCascade patterns, not hard-coded reminder pulses',()=>{
    const watchReminder=watch('WatchReminderService');
    assert.match(watchReminder,/WatchCascade.strongTimings\(intensity\)/);
    assert.match(watchReminder,/WatchCascade.gentleTimings\(intensity\)/);
    assert.match(watchReminder,/WatchCascade.strongAmplitudes\(intensity\)/);
    assert.match(watchReminder,/WatchCascade.gentleAmplitudes\(intensity\)/);
    assert.match(watchReminder,/WatchCascade.untilStrong\(due,System.currentTimeMillis\(\)\)/);
    assert.match(phone('ReminderWatchBridge'),/if\(START.equals\(path\)&&item.cascade\)/);
});
