import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const phone=n=>readFileSync(new URL('../alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java',import.meta.url),'utf8');
const watch=n=>readFileSync(new URL('../watch/src/main/java/com/cobrachris/watchvibrationpoc/'+n+'.java',import.meta.url),'utf8');

test('existing and new cascades use the wake profile automatically',()=>{
    const store=phone('ReminderStore'),ui=phone('ReminderActivity');
    assert.match(store,/p.getBoolean\(id\+"_cascade",true\)\);/);
    assert.match(store,/public final boolean cascade,done,unifiedCascade/);
    assert.match(store,/putBoolean\(id\+"_unified_cascade",cascade\)/);
    assert.match(store,/putBoolean\(item.id\+"_unified_cascade",item.unifiedCascade\)/);
    assert.match(store,/remove\(id\+"_unified_cascade"\)/);
    assert.doesNotMatch(ui,/sameAsAlarm/);
    assert.match(ui,/tone.getSelectedItemPosition\(\),cascade.isChecked\(\)/);
    assert.match(ui,/item.cascade\?" · Wecker-Kaskade"/);
});

test('new cascade uses the actual alarm envelope and only current reminder owns vibration',()=>{
    const tone=phone('ReminderToneService'),recv=phone('ReminderReceiver'),motor=phone('PhoneVibration');
    assert.match(tone,/GentleGain.level\(/);
    assert.match(tone,/System.currentTimeMillis\(\)-rampDue/);
    assert.match(tone,/0,60,42/);
    assert.match(tone,/unifiedRamp=reminder.cascade&&reminder.unifiedCascade/);
    assert.match(tone,/track.setVolume\(unifiedRamp\?0f:.42f\)/);
    assert.match(tone,/if\(playing&&activeId==id&&activeDue==due\)return START_NOT_STICKY/);
    assert.match(recv,/if\(!item.cascade\|\|item.unifiedCascade\)ReminderToneService.start/);
    assert.match(recv,/if\(item.unifiedCascade\)PhoneVibration.startReminder/);
    assert.match(recv,/WakeCascade.phoneMode\(0\)/);
    assert.match(motor,/ActiveAlarmState.activeId\(c\)>0/);
    assert.match(motor,/if\(id==reminderId&&due==reminderWhen/);
    assert.match(motor,/ActiveAlarmState.activeId\(c\)<=0/);
    assert.match(recv,/PhoneVibration.stopReminder\(c,item.id,item.dueAt\)/);
    assert.match(uiNoExtra(phone('ReminderActivity')),/PhoneVibration.stopReminder\(this,item.id,item.dueAt\)/);
    function uiNoExtra(s){return s;}
});

test('Watch transport retains seven-field legacy packets and adds optional mode',()=>{
    const bridge=phone('ReminderWatchBridge'),listener=watch('SanftweckerWearListener'),
        service=watch('WatchReminderService');
    assert.match(bridge,/String payload=item.id/);
    assert.match(bridge,/String shared=payload\+":1"/);
    assert.match(bridge,/if\(START.equals\(path\)&&item.cascade&&item.unifiedCascade\)/);
    assert.match(listener,/putExtra\("unified_cascade",a.length==8 && "1".equals\(a\[7\]\)\)/);
    assert.match(listener,/\(:\[01\]\)\?/);
    assert.match(service,/unifiedCascade=cascade&&receivedUnified/);
    assert.match(service,/if\(id==requestedId&&due==when\)/);
    assert.match(service,/if\(receivedUnified&&cascade&&!unifiedCascade\)/);
    assert.match(service,/WatchCascade.strongTimings\(intensity\)/);
    assert.match(service,/WatchCascade.gentleTimings\(intensity\)/);
    assert.match(service,/WatchCascade.untilStrong\(due,System.currentTimeMillis\(\)\)/);
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
