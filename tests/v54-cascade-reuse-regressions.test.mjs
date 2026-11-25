import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const phone=n=>readFileSync(new URL('../alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java',import.meta.url),'utf8');
const watch=n=>readFileSync(new URL('../watch/src/main/java/com/cobrachris/watchvibrationpoc/'+n+'.java',import.meta.url),'utf8');

test('all existing reminders use saved Testwecker cascade regardless of old flags',()=>{
    const store=phone('ReminderStore'),screen=phone('ReminderActivity'),
          config=phone('ReminderAlarmConfig');
    assert.match(store,/p.getBoolean\(id\+"_cascade",true\)/);
    assert.doesNotMatch(store,/p.getBoolean\(id\+"_unified_cascade",false\)/);
    assert.match(config,/AlarmPresetImporter.referenceAlarm\(context\)/);
    assert.match(screen,/Ausgabe wie Testwecker/);
    assert.doesNotMatch(screen,/cascade.setChecked|sameAsAlarm/);
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

test('same 60s escalation executes from real wake audio service with occurrence-safe fallback',()=>{
    const adapter=phone('ReminderToneService'),ring=phone('RingingService'),
          recv=phone('ReminderReceiver'),vibration=phone('PhoneVibration'),
          sched=phone('ReminderScheduler');
    assert.match(adapter,/RingingService.REMINDER_START/);
    assert.match(ring,/WakeCascade.untilStrong\(ringWhen,System.currentTimeMillis\(\)/);
    assert.match(ring,/WakeCascade.strongStage\(/);
    assert.match(ring,/PhoneVibration.startReminder\(this,strongMode,/);
    assert.match(ring,/expectedReminder=reminderActive/);
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
    assert.match(phone('ReminderWatchBridge'),/reference.cascadeEnabled/);
});
