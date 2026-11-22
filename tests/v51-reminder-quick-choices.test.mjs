import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const phone=n=>read('alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java');
const watch=n=>read('watch/src/main/java/com/cobrachris/watchvibrationpoc/'+n+'.java');

test('phone offers 10/30/60/tomorrow/stop while retaining Done separately',()=>{
 const ui=phone('ReminderOptionsActivity');
 for(const label of ['In 10 Minuten nochmal erinnern','In 30 Minuten nochmal erinnern',
   'In 60 Minuten nochmal erinnern','Nächsten Tag wieder erinnern','Stoppen (bleibt offen)'])
   assert.ok(ui.includes(label),label);
 const notice=phone('ReminderNotice');
 assert.ok(notice.includes('ReminderOptionsActivity.class'));
 assert.ok(notice.includes('"Alle Optionen"'));
 assert.ok(notice.includes('ReminderScheduler.DONE'));
 assert.ok(notice.includes('ReminderScheduler.STOP'));
 assert.ok(ui.includes('current.dueAt!=due'));
});
test('safe choices preserve reminder identity and use local original-time tomorrow',()=>{
 const scheduler=phone('ReminderScheduler');
 const receiver=phone('ReminderReceiver');
 assert.ok(scheduler.includes('REMINDER_CHOICE'));
 assert.ok(scheduler.includes('minutes!=10&&minutes!=30&&minutes!=60&&minutes!=-1'));
 assert.ok(receiver.includes('due!=item.dueAt'));
 assert.ok(receiver.includes('delay!=10&&delay!=30&&delay!=60&&delay!=-1'));
 assert.ok(receiver.includes('original.setTimeInMillis(item.anchorAt)'));
 assert.ok(receiver.includes('tomorrow.add(Calendar.DAY_OF_YEAR,1)'));
 assert.ok(receiver.includes('ReminderStore.postpone(c,id,next)'));
 assert.ok(receiver.includes('ReminderScheduler.cancel(c,id)'));
 assert.ok(receiver.includes('ReminderToneService.stop(c,item)'));
 assert.ok(receiver.includes('ReminderWatchBridge.stop(c,item)'));
 assert.ok(receiver.includes('ReminderScheduler.STOP.equals(action)'));
});
test('watch round screen displays five actions and sends constrained choice messages',()=>{
 const screen=watch('WatchReminderOptionsActivity');
 const service=watch('WatchReminderService');
 const listener=phone('WearPhoneListener');
 for(const label of ['In 10 Minuten','In 30 Minuten','In 60 Minuten','Morgen wieder','Stoppen'])
   assert.ok(screen.includes(label),label);
 assert.ok(service.includes('WatchReminderOptionsActivity.class'));
 assert.ok(service.includes('"Optionen",options()'));
 assert.ok(service.includes('minutes!=10&&minutes!=30&&minutes!=60&&minutes!=-1'));
 assert.ok(service.includes('requestedId!=id||when!=due'));
 assert.ok(service.includes('"tomorrow":"later"+minutes'));
 assert.ok(listener.includes('later10|later30|later60|tomorrow'));
 assert.ok(listener.includes('reminder.dueAt!=due'));
 assert.ok(listener.includes('request.putExtra(ReminderScheduler.DELAY,delay)'));
});
test('both choice activities are internal and cannot be launched from external apps',()=>{
 assert.ok(read('alarm/src/main/AndroidManifest.xml').includes(
   'android:name=".ReminderOptionsActivity" android:exported="false"'));
 assert.ok(read('watch/src/main/AndroidManifest.xml').includes(
   'android:name=".WatchReminderOptionsActivity" android:exported="false"'));
});
