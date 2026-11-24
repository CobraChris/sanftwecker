import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const phone=name=>readFileSync(new URL('../alarm/src/main/java/com/cobrachris/sanftwecker/'+name+'.java',import.meta.url),'utf8');
const read=path=>readFileSync(new URL('../'+path,import.meta.url),'utf8');

test('Testwecker and reminder emit samples from the identical PCM renderer',()=>{
    const alarm=phone('RingingService'),reminder=phone('ReminderToneService'),pcm=phone('AlarmPcm');
    assert.match(alarm,/buffer\[i\] = AlarmPcm\.sample\(tone,t\)/);
    assert.match(reminder,/samples\[k\]=AlarmPcm\.sample\(tone,t\)/);
    assert.match(pcm,/ToneCatalog\.sample\(tone,seconds\)/);
    assert.match(pcm,/0\.75\+0\.25\*Math\.cos\(2\*Math\.PI\*0\.24\*seconds\)/);
    assert.match(pcm,/Short\.MAX_VALUE\*0\.36\*breathe\*oscillation/);
    assert.doesNotMatch(reminder,/Short\.MAX_VALUE\*\.35/);
});

test('reminder reads saved Testwecker sound parameters at time of ringing',()=>{
    const reminder=phone('ReminderToneService'),reference=phone('AlarmPresetImporter');
    assert.match(reminder,/savedTestwecker=AlarmPresetImporter\.referenceAlarm\(this\)/);
    assert.match(reminder,/selectedTone=savedTestwecker\.tone/);
    assert.match(reference,/!"Testwecker".equals\(alarm.name\)/);
    assert.match(reminder,/soundProfile=reference/);
    assert.match(reminder,/AudioAttributes\.USAGE_ALARM/);
    assert.match(reminder,/AlarmEnvelope\.gain\(soundProfile,false,elapsed\)/);
    assert.match(phone('RingingService'),/AlarmEnvelope\.gain\(profile,/);
    assert.match(reminder,/WakeCascade\.duration\(\s*false,false,reference.cascadeEnabled,reference.autoStopMinutes\)/);
    assert.match(reminder,/reference\.audibleWhenMuted\)AlarmVolumeGuard\.ensureAudible/);
    assert.match(reminder,/AlarmVolumeGuard\.restore\(this\)/);
    assert.doesNotMatch(reminder,/0,60,42|track\.setVolume\(cascadeRamp\?0f:.42f\)/);
    assert.doesNotMatch(reminder,/handler\.postDelayed\(finish,cascadeRamp\?180_000L:90_000L\)/);
});

test('reminder stopping and postponing remain occurrence-safe and silent opt-out remains explicit',()=>{
    const reminder=phone('ReminderToneService'),receiver=phone('ReminderReceiver'),ui=phone('ReminderActivity');
    assert.match(reminder,/if\(item==null\|\|item.done\|\|!item.phoneSound\)return/);
    assert.match(ui,/Am Handy wie Testwecker klingeln \(aus = stumm\)/);
    assert.match(ui,/Ersatzton \(nur wenn Testwecker fehlt\)/);
    assert.match(reminder,/if\(playing&&activeId==id&&activeDue==due\)return START_NOT_STICKY/);
    assert.match(reminder,/ActiveAlarmState\.activeId\(this\)>0/);
    assert.match(reminder,/if\(activeId==id&&activeDue==due\)stopAudio\(\)/);
    assert.match(reminder,/handler\.removeCallbacks\(strongPhase\)/);
    for(const action of ['DONE','CHOICE','LATER','STOP'])
      assert.match(receiver,new RegExp('ReminderScheduler\\.'+action+'\\.equals\\(action\\)'));
    assert.match(receiver,/ReminderToneService\.stop\(c,item\)/);
    assert.match(reminder,/selectedTone=reminder.tone/); // safe fallback if Testwecker renamed
});

test('both alarm and reminder continue to use the same alarm audio stream',()=>{
    for(const service of ['RingingService','ReminderToneService']){
        const s=phone(service);
        assert.match(s,/AudioAttributes\.USAGE_ALARM/);
        assert.match(s,/AudioTrack\.MODE_STREAM/);
        assert.match(s,/AudioFormat\.ENCODING_PCM_16BIT/);
    }
    assert.match(read('alarm/src/test/java/com/cobrachris/sanftwecker/AlarmPcmTest.java'),
        /everyOriginalToneRetainsItsPcmSamples/);
});
