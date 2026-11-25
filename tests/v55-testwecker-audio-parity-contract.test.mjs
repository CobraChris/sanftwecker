import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const phone=n=>readFileSync(new URL('../alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java',import.meta.url),'utf8');
const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');

test('Testwecker and reminders use ONE actual AudioTrack and PCM implementation',()=>{
    const ring=phone('RingingService'),adapter=phone('ReminderToneService'),pcm=phone('AlarmPcm');
    assert.match(ring,/buffer\[i\] = AlarmPcm\.sample\(tone,t\)/);
    assert.match(ring,/AudioTrack\.MODE_STREAM/);
    assert.match(ring,/AudioFormat\.ENCODING_PCM_16BIT/);
    assert.match(ring,/AudioAttributes\.USAGE_ALARM/);
    assert.match(pcm,/ToneCatalog\.sample\(tone,seconds\)/);
    assert.match(pcm,/0\.75\+0\.25\*Math\.cos\(2\*Math\.PI\*0\.24\*seconds\)/);
    assert.match(adapter,/RingingService.REMINDER_START/);
    assert.doesNotMatch(adapter,/AudioTrack|MODE_STREAM|ToneCatalog\.sample|AlarmEnvelope\.gain/);
});

test('reference Testwecker settings, not saved reminder flags, own the sound',()=>{
    const ring=phone('RingingService'),config=phone('ReminderAlarmConfig'),
          store=phone('ReminderStore'),adapter=phone('ReminderToneService');
    assert.match(config,/AlarmPresetImporter.referenceAlarm\(context\)/);
    assert.match(ring,/requestedProfile=ReminderAlarmConfig.profile\(this\)/);
    assert.match(ring,/profile=requestedProfile/);
    assert.match(ring,/activeTone=toneOnlyPreview\?ToneCatalog.safe\(/);
    assert.match(ring,/AlarmEnvelope\.gain\(profile,/);
    assert.match(ring,/profile\.maxVolume/);
    assert.match(ring,/profile\.cascadeEnabled/);
    assert.match(ring,/profile\.autoStopMinutes/);
    assert.match(ring,/if\(profile.audibleWhenMuted\)/);
    assert.match(adapter,/if\(item==null\|\|item.done\)return/);
    assert.doesNotMatch(adapter,/!item.phoneSound|item.cascade|item.tone/);
    assert.match(store,/p.getBoolean\(id\+"_sound",true\)/); // backwards readable only
});

test('one real wake foreground notification owns reminder full-screen and stop actions',()=>{
    const ring=phone('RingingService'),receiver=phone('ReminderReceiver'),
          manifest=read('alarm/src/main/AndroidManifest.xml');
    assert.match(ring,/REMINDER_START.equals\(action\)/);
    assert.match(ring,/new Intent\(this,ReminderOptionsActivity.class\)/);
    assert.match(ring,/builder.setFullScreenIntent\(openApp\(\), true\)/);
    assert.match(ring,/ReminderScheduler.pending\(this,reminderId,reminderDue,/);
    assert.match(ring,/if\(active&&reminderActive&&reminderId==id&&reminderDue==due\)/);
    assert.match(ring,/if\(active&&!reminderActive&&!toneOnlyPreview\)/);
    assert.match(receiver,/ReminderToneService.start\(c,item\)/);
    assert.match(receiver,/ReminderToneService.stop\(c,item\)/);
    assert.match(manifest,/android:name="\.RingingService"/);
    assert.doesNotMatch(manifest,/android:name="\.ReminderToneService"/);
});

test('reminder schedule and user actions remain separate from true wake alarms',()=>{
    const receiver=phone('ReminderReceiver'),ring=phone('RingingService'),adapter=phone('ReminderToneService');
    for(const act of ['DONE','CHOICE','LATER','STOP'])
        assert.match(receiver,new RegExp('ReminderScheduler\\.'+act+'\\.equals\\(action\\)'));
    assert.match(adapter,/RingingService.REMINDER_STOP/);
    assert.match(adapter,/putExtra\(ReminderScheduler.ID,item.id\)/);
    assert.match(adapter,/putExtra\(ReminderScheduler.DUE,item.dueAt\)/);
    assert.match(ring,/if\(reminderActive\)return START_NOT_STICKY/);
    assert.match(ring,/if\(wasReminder\)PhoneVibration.stopReminder/);
    assert.match(ring,/if\(wasActive&&!wasReminder\)/);
    assert.match(read('alarm/src/test/java/com/cobrachris/sanftwecker/AlarmPcmTest.java'),
        /everyOriginalToneRetainsItsPcmSamples/);
});
