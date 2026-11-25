import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const phone=n=>read('alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java');
const watch=n=>read('watch/src/main/java/com/cobrachris/watchvibrationpoc/'+n+'.java');

test('reminder FIRE, Android FGS and lockscreen use the SAME concrete RingingService as wake alarms',()=>{
 const receiver=phone('ReminderReceiver'),proxy=phone('ReminderToneService'),
       alarm=phone('AlarmReceiver'),ring=phone('RingingService'),manifest=read('alarm/src/main/AndroidManifest.xml');
 assert.match(receiver,/ReminderToneService.start\(c,item\)/);
 assert.match(proxy,/new Intent\(c,RingingService.class\)/);
 assert.match(proxy,/setAction\(RingingService.REMINDER_START\)/);
 assert.match(proxy,/c.startForegroundService\(intent\)/);
 assert.match(alarm,/new Intent\(c, RingingService.class\)/);
 assert.match(ring,/REMINDER_START.equals\(action\)/);
 assert.match(ring,/profile=requestedProfile/);
 assert.match(ring,/requestedProfile=ReminderAlarmConfig.profile\(this\)/);
 assert.match(ring,/startForegroundNotification\(\)/);
 assert.match(ring,/beginAudio\(\)/);
 assert.match(ring,/handler.post\(gainTick\)/);
 assert.match(ring,/builder.setFullScreenIntent\(openApp\(\), true\)/);
 assert.match(ring,/new Intent\(this,ReminderOptionsActivity.class\)/);
 assert.doesNotMatch(manifest,/android:name="\.ReminderToneService"/);
 assert.doesNotMatch(proxy,/extends Service|new AudioTrack|AudioTrack.Builder/);
});

test('all existing reminder rows use live saved Testwecker settings, never an old cascade or mute flag',()=>{
 const config=phone('ReminderAlarmConfig'),ring=phone('RingingService'),
       ui=phone('ReminderActivity'),bridge=phone('ReminderWatchBridge'),proxy=phone('ReminderToneService');
 assert.match(config,/AlarmPresetImporter.referenceAlarm\(context\)/);
 assert.match(ring,/requestedProfile=ReminderAlarmConfig.profile\(this\)/);
 assert.match(ring,/AlarmEnvelope.gain\(profile,/);
 assert.match(ring,/profile\.cascadeEnabled/);
 assert.match(ring,/WakeCascade.phoneMode\(profile.vibrationMode\)/);
 assert.match(ring,/WakeCascade.duration\(toneOnlyPreview,quickTest,/);
 assert.match(bridge,/AlarmProfile reference=ReminderAlarmConfig.profile\(c\)/);
 for(const prop of ['cascadeEnabled','watchIntensity','watchPulses','watchIntervalSeconds','autoStopMinutes'])
    assert.ok(bridge.includes('reference.'+prop),prop);
 assert.match(proxy,/if\(item==null\|\|item.done\)return/);
 assert.doesNotMatch(proxy,/!item\.phoneSound|item\.cascade|item\.tone/);
 assert.doesNotMatch(bridge,/item\.cascade|item\.watchIntensity|item\.watchPulses|item\.watchIntervalSeconds/);
 assert.match(ui,/Ausgabe wie Testwecker/);
 assert.doesNotMatch(ui,/cascade\.setChecked|phoneSound\.isChecked|tone\.getSelectedItemPosition/);
});

test('reminder-specific postpone/done/stop cannot interrupt real alarms or another occurrence',()=>{
 const ring=phone('RingingService'),proxy=phone('ReminderToneService'),
       receiver=phone('ReminderReceiver'),ui=phone('ReminderOptionsActivity');
 assert.match(proxy,/RingingService.REMINDER_STOP/);
 assert.match(ring,/if\(active&&reminderActive&&reminderId==id&&reminderDue==due\)/);
 assert.match(ring,/if\(active&&!reminderActive&&!toneOnlyPreview\)/);
 assert.match(ring,/if\(reminderActive\)return START_NOT_STICKY/);
 assert.match(ring,/if\(wasReminder\)PhoneVibration.stopReminder/);
 assert.match(ring,/if\(wasActive&&!wasReminder\)/);
 assert.match(receiver,/ReminderScheduler.CHOICE.equals\(action\)/);
 assert.match(receiver,/ReminderScheduler.DONE.equals\(action\)/);
 assert.match(receiver,/ReminderScheduler.STOP.equals\(action\)/);
 assert.match(ui,/Erledigt \(abschließen\)/);
 assert.match(ui,/Nächsten Tag wieder erinnern/);
 assert.match(ui,/In 60 Minuten nochmal erinnern/);
});

test('watch actual reminder uses the same repeating strong waveform and duration rule as clock',()=>{
 const bridge=phone('ReminderWatchBridge'),listener=watch('SanftweckerWearListener'),
       reminder=watch('WatchReminderService'),alarm=watch('WatchAlarmService');
 assert.match(bridge,/String detailed=shared\+":"\+reference.autoStopMinutes/);
 assert.match(bridge,/String shared=payload\+":1"/);
 assert.match(bridge,/payload.getBytes\(StandardCharsets.UTF_8\)/);
 assert.match(listener,/putExtra\("autostop",a.length>=9\?Integer.parseInt\(a\[8\]\):0\)/);
 assert.match(reminder,/int repeatIndex=hard\?0:-1/);
 assert.match(alarm,/int repeatIndex=repeat\?0:-1/);
 assert.match(reminder,/WatchCascade.strongTimings\(intensity\)/);
 assert.match(alarm,/WatchCascade.strongTimings\(activeIntensity\)/);
 assert.match(reminder,/autoStopMinutes=intent.getIntExtra\("autostop",0\)/);
 assert.match(reminder,/Math.min\(15,autoStopMinutes\)/);
 assert.doesNotMatch(reminder,/strongPlayed>=12|handler.postDelayed\(this,8_000L\)/);
 assert.match(reminder,/handler.removeCallbacks\(wakePriorityCheck\)/);
});

test('legacy reminder data is still readable but never drives audio or haptics',()=>{
 const model=phone('ReminderStore'),ui=phone('ReminderActivity');
 assert.match(model,/sanftwecker_reminders_v1/);
 assert.match(model,/public final boolean cascade,done/);
 assert.match(model,/public final boolean phoneSound/);
 assert.match(ui,/previous.phoneSound/);
 assert.match(ui,/previous.cascade/);
 assert.match(ui,/ReminderStore.restore\(this,previous\)/);
 assert.match(phone('ReminderScheduler'),/new Intent\(c,ReminderReceiver.class\)/);
});

test('a replacement Watch reminder cannot inherit stale gentle/strong/escalation callbacks',()=>{
 const source=watch('WatchReminderService');
 const start=source.indexOf('if(id>0){');
 const end=source.indexOf('id=requestedId;due=when;',start);
 assert.ok(start>=0&&end>start);
 const replaceBlock=source.slice(start,end);
 for(const task of ['pulse','strongPulse','localEscalate','autoEnd','wakePriorityCheck'])
     assert.ok(replaceBlock.includes('handler.removeCallbacks('+task+')'),task);
 assert.match(source,/if\(played<pulses\)\s*handler.postDelayed\(this,interval\*1000L\)/);
 assert.match(source,/if\(id==requestedId&&due==when\)/);
 assert.match(source,/if\(requestedId==id&&when==due&&cascade&&!strong\)/);
 assert.match(source,/handler\.postDelayed\(localEscalate,\s*WatchCascade\.untilStrong/);
});
