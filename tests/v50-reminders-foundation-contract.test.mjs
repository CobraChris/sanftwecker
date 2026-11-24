import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=(p)=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const phone=n=>read('alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java');
const watch=n=>read('watch/src/main/java/com/cobrachris/watchvibrationpoc/'+n+'.java');

test('new fifth tab and private reminder screen coexist with original navigation',()=>{
 const tabs=phone('ClockTabs'),activity=phone('ReminderActivity');
 assert.match(tabs,/REMINDERS=4/);
 for(const name of ['Wecker','Timer','Stoppuhr','Weltuhr','Erinnerungen'])
   assert.ok(tabs.includes(name),name);
 assert.match(activity,/ClockTabs\.REMINDERS/);
 assert.match(activity,/RecognizerIntent\.ACTION_RECOGNIZE_SPEECH/);
 assert.match(activity,/ReminderVoiceParser\.parse/);
 assert.match(activity,/cascade\.setChecked\(existing==null\|\|existing\.cascade\)/);
});
test('reminders never share clock or timer identity and stay open until done',()=>{
 const model=phone('ReminderStore'),scheduler=phone('ReminderScheduler'),receiver=phone('ReminderReceiver');
 assert.match(model,/sanftwecker_reminders_v1/);
 assert.match(model,/putBoolean\(id\+"_done",true\)/);
 assert.match(scheduler,/new Intent\(c,ReminderReceiver\.class\)/);
 assert.match(scheduler,/BASE=150000/);
 assert.match(scheduler,/WakeCascade\.SECOND_STAGE_MS/);
 assert.match(receiver,/ReminderScheduler\.DONE\.equals\(action\)/);
 assert.match(receiver,/ReminderScheduler\.LATER\.equals\(action\)/);
 assert.match(receiver,/ReminderScheduler\.STOP\.equals\(action\)/);
 assert.doesNotMatch(receiver,/AlarmRepository\.delete|AlarmScheduler\.cancelSnooze/);
});
test('notification offers done, postpone and stop with separate gentle / strong channel',()=>{
 const notice=phone('ReminderNotice');
 for(const name of ['REMINDER_DONE','REMINDER_STOP'])
   assert.match(notice,new RegExp(name.replace('REMINDER_','')));
 assert.match(notice,/sanftwecker_reminder_soft_v1/);
 assert.match(notice,/sanftwecker_reminder_strong_custom_v2/);
 assert.match(notice,/ReminderScheduler\.DONE/);
 assert.match(notice,/ReminderOptionsActivity\.class/);
 assert.match(notice,/ReminderScheduler\.STOP/);
});
test('Watch cascade is not routed through existing wake alarm or timer IDs',()=>{
 const phoneBridge=phone('ReminderWatchBridge'),watchListener=watch('SanftweckerWearListener'),
       service=watch('WatchReminderService'),phoneListener=phone('WearPhoneListener');
 assert.match(phoneBridge,/\/sanftwecker\/reminder\/v1\/start/);
 assert.match(watchListener,/WatchReminderService\.START/);
 assert.match(watchListener,/WatchReminderService\.STRONG/);
 assert.match(watchListener,/WatchReminderService\.STOP/);
 assert.match(service,/active|Reminder/);
 assert.match(service,/clockActive\(\)/);
 assert.match(service,/DONE\.equals\(action\)/);
 assert.match(service,/LATER\.equals\(action\)/);
 assert.match(service,/USER_STOP\.equals\(action\)/);
 assert.match(phoneListener,/ReminderWatchBridge\.ACTION/);
 assert.match(phoneListener,/reminder\.dueAt!=due/);
});
test('reminders restore after reboot and app update, remain private',()=>{
 assert.match(phone('RescheduleReceiver'),/ReminderScheduler\.restore\(c\)/);
 const phoneManifest=read('alarm/src/main/AndroidManifest.xml');
 const watchManifest=read('watch/src/main/AndroidManifest.xml');
 assert.match(phoneManifest,/android:name="\.ReminderActivity" android:exported="false"/);
 assert.match(phoneManifest,/android:name="\.ReminderReceiver" android:exported="false"/);
 assert.match(watchManifest,/\.WatchReminderService/);
 assert.match(watchManifest,/pathPrefix="\/sanftwecker\/reminder\/v1"/);
});

test('recurring reminder completes a single occurrence and schedules next',()=>{
 const model=phone('ReminderStore'),receiver=phone('ReminderReceiver');
 assert.match(model,/Calendar\.DAY_OF_YEAR/);
 assert.match(model,/Calendar\.WEEK_OF_YEAR/);
 assert.match(model,/item\.repeat==0/);
 assert.match(model,/cal\.add\(field,1\)/);
 assert.match(receiver,/ReminderStore\.Item next=ReminderStore\.get\(c,id\)/);
 assert.match(receiver,/ReminderScheduler\.schedule\(c,id\)/);
 assert.match(receiver,/item\.postponeMinutes\*60_000L/);
});
test('editor never silently accepts unrecognized voice time or unscheduled save',()=>{
 const ui=phone('ReminderActivity'),model=phone('ReminderStore');
 assert.match(ui,/timeConfirmed=false/);
 assert.match(ui,/if\(!timeConfirmed\)/);
 assert.match(ui,/dialog\.setOnShowListener/);
 assert.match(ui,/if\(save\(\)\)dialog\.dismiss\(\)/);
 assert.match(ui,/ReminderStore\.abandon\(this,id\)/);
 assert.match(model,/public static synchronized void abandon/);
 assert.match(ui,/Wöchentlich/);
 assert.match(ui,/new int\[\]\{5,10,30\}/);
 assert.match(ui,/phoneSound\.isChecked\(\)/);
});
test('strong reminder chooses system tone or silent vibration without other alarm channels',()=>{
 const notice=phone('ReminderNotice'),model=phone('ReminderStore');
 assert.match(notice,/VIBRATE_ONLY/);
 assert.match(notice,/item\.phoneSound\?STRONG:VIBRATE_ONLY/);
 assert.match(notice,/"Alle Optionen"/);
 assert.match(model,/putBoolean\(id\+"_sound",phoneSound\)/);
});

test('deleting an open or finished reminder cancels only its own scheduled and Watch instance',()=>{
 const model=phone('ReminderStore'),ui=phone('ReminderActivity');
 assert.match(model,/public static synchronized void delete\(Context c,int id\)/);
 assert.match(ui,/ReminderScheduler\.cancel\(this,item\.id\)/);
 assert.match(ui,/ReminderNotice\.clear\(this,item\.id\)/);
 assert.match(ui,/ReminderWatchBridge\.stop\(this,item\)/);
 assert.match(ui,/ReminderStore\.delete\(this,item\.id\)/);
 assert.match(ui,/setTitle\("Erinnerung löschen\?"\)/);
});

test('the nine original tones play in a separate bounded reminder service',()=>{
 const model=phone('ReminderStore'),ui=phone('ReminderActivity');
 const receiver=phone('ReminderReceiver'),sound=phone('ReminderToneService');
 const notice=phone('ReminderNotice');
 const manifest=read('alarm/src/main/AndroidManifest.xml');
 assert.match(model,/!ToneCatalog\.valid\(tone\)/);
 assert.match(model,/putInt\(id\+"_tone",tone\)/);
 assert.match(ui,/ToneCatalog\.LABELS/);
 assert.match(ui,/tone\.getSelectedItemPosition\(\)/);
 assert.match(receiver,/ReminderToneService\.start\(c,item\)/);
 assert.match(receiver,/ReminderToneService\.stop\(c,item\)/);
 assert.match(sound,/ToneCatalog\.sample\(tone,t\)/);
 assert.match(sound,/handler\.postDelayed\(finish,cascadeRamp\?180_000L:90_000L\)/);
 assert.match(sound,/ActiveAlarmState\.activeId\(this\)>0/);
 assert.match(sound,/ReminderScheduler\.DUE/);
 assert.match(sound,/reminder\.dueAt!=due/);
 assert.match(manifest,/\.ReminderToneService/);
 assert.match(notice,/loud\.setSound\(null,null\)/);
});

test('two-stage Watch vibration respects selected postpone time and protects wake alarms',()=>{
 const bridge=phone('ReminderWatchBridge');
 const listener=watch('SanftweckerWearListener');
 const service=watch('WatchReminderService');
 const manifest=read('alarm/src/main/AndroidManifest.xml');
 assert.match(bridge,/item\.postponeMinutes/);
 assert.match(listener,/putExtra\("postpone",Integer\.parseInt\(a\[6\]\)\)/);
 assert.match(service,/postponeMinutes!=5&&postponeMinutes!=10&&postponeMinutes!=30/);
 assert.match(service,/strongPlayed>=12/);
 assert.match(service,/handler\.postDelayed\(this,8_000L\)/);
 assert.match(service,/clockActive\(\)/);
 assert.match(service,/handler\.removeCallbacks\(strongPulse\)/);
 assert.match(manifest,/android:pathPrefix="\/sanftwecker\/reminder\/v1"/);
});
test('deferred repeated reminders retain their original local-time cadence',()=>{
 const model=phone('ReminderStore');
 assert.match(model,/public final long dueAt,anchorAt/);
 assert.match(model,/putLong\(id\+"_anchor",dueAt\)/);
 assert.match(model,/cal\.setTimeInMillis\(item\.anchorAt\)/);
 assert.match(model,/putLong\(id\+"_anchor",cal\.getTimeInMillis\(\)\)/);
});
test('reminder tone stops when user stops or defers and when a wake alarm starts later',()=>{
 const receiver=phone('ReminderReceiver');
 const tone=phone('ReminderToneService');
 assert.match(receiver,/ReminderScheduler\.STOP\.equals\(action\)[\s\S]*ReminderToneService\.stop\(c,item\)/);
 assert.match(receiver,/ReminderScheduler\.LATER\.equals\(action\)[\s\S]*ReminderToneService\.stop\(c,item\)/);
 assert.match(tone,/private final Runnable wakePriorityCheck/);
 assert.match(tone,/ActiveAlarmState\.activeId\(ReminderToneService\.this\)>0/);
});
