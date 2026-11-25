import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const phone=n=>readFileSync(new URL('../alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java',import.meta.url),'utf8');
const watch=n=>readFileSync(new URL('../watch/src/main/java/com/cobrachris/watchvibrationpoc/'+n+'.java',import.meta.url),'utf8');

test('previously saved reminder flags cannot override the Testwecker output configuration',()=>{
    const store=phone('ReminderStore'),ui=phone('ReminderActivity');
    const config=phone('ReminderAlarmConfig');
    assert.match(store,/p.getBoolean\(id\+"_cascade",true\)/); // backwards-readable
    assert.match(store,/remove\(id\+"_unified_cascade"\)/); // one-time cleanup
    assert.match(config,/AlarmPresetImporter\.referenceAlarm\(context\)/);
    assert.match(ui,/ReminderAlarmConfig\.profile\(this\)/);
    assert.doesNotMatch(ui,/cascade\.setChecked|sameAsAlarm|phoneSound\.isChecked/);
    assert.match(ui,/Ausgabe wie Testwecker/);
});

test('real wake audio engine owns every reminder; no independent PCM or gain path survives',()=>{
    const adapter=phone('ReminderToneService'),recv=phone('ReminderReceiver'),
          ring=phone('RingingService'),motor=phone('PhoneVibration');
    assert.match(adapter,/new Intent\(c,RingingService.class\)/);
    assert.match(adapter,/RingingService.REMINDER_START/);
    assert.doesNotMatch(adapter,/AudioTrack|ToneCatalog\.sample|GentleGain/);
    assert.match(ring,/AlarmEnvelope\.gain\(profile,/);
    assert.match(ring,/AlarmPcm\.sample\(tone,t\)/);
    assert.match(ring,/REMINDER_START.equals\(action\)/);
    assert.match(ring,/profile=|profile=requestedProfile/);
    assert.match(ring,/PhoneVibration.startReminder\(this,strongMode,/);
    assert.match(ring,/WakeCascade\.strongStage\(/);
    assert.match(recv,/ReminderToneService.start\(c,item\)/);
    assert.match(recv,/settings.cascadeEnabled\)PhoneVibration.startReminder/);
    assert.match(motor,/ActiveAlarmState.activeId\(c\)>0/);
    assert.match(recv,/PhoneVibration.stopReminder\(c,item.id,item.dueAt\)/);
});

test('Watch transport stays backward compatible, but new Watch receives Testwecker settings',()=>{
    const bridge=phone('ReminderWatchBridge'),listener=watch('SanftweckerWearListener'),
          service=watch('WatchReminderService');
    assert.match(bridge,/String payload=item.id/);
    assert.match(bridge,/reference.watchIntensity/);
    assert.match(bridge,/reference.watchIntervalSeconds/);
    assert.match(bridge,/String detailed=shared/);
    assert.match(listener,/putExtra\("autostop"/);
    assert.doesNotMatch(service,/unifiedCascade|receivedUnified/);
    assert.match(service,/if\(id==requestedId&&due==when\)/);
    assert.match(service,/strong=!cascade\|\|WatchCascade.strong\(due,System.currentTimeMillis\(\)\)/);
    assert.match(service,/WatchCascade.strongTimings\(intensity\)/);
    assert.match(service,/WatchCascade.gentleTimings\(intensity\)/);
    assert.match(service,/repeatIndex=hard\?0:-1/);
    assert.match(service,/scheduleAutoEnd\(\)/);
    assert.match(service,/handler.removeCallbacks\(localEscalate\)/);
});

test('urgent reminders request phone and Watch full-screen options without background launch',()=>{
    const ph=phone('ReminderNotice'),pw=phone('ReminderOptionsActivity'),
        wh=watch('WatchReminderService'),ww=watch('WatchReminderOptionsActivity');
    assert.match(ph,/if\(justFired\)builder.setFullScreenIntent\(fullView,true\)/);
    assert.match(pw,/setShowWhenLocked\(true\)/);
    assert.match(wh,/setFullScreenIntent\(options\(\),true\)/);
    assert.match(ww,/setShowWhenLocked\(true\)/);
    assert.match(ww,/setTurnScreenOn\(true\)/);
    assert.doesNotMatch(phone('ReminderReceiver'),/startActivity\(/);
});
