import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const phone=n=>read('alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java');
const watch=n=>read('watch/src/main/java/com/cobrachris/watchvibrationpoc/'+n+'.java');

test('scheduled reminders use the same true alarm-clock trigger class as real wake alarms',()=>{
    const reminders=phone('ReminderScheduler'),wake=phone('AlarmScheduler');
    assert.match(wake,/manager\.setAlarmClock\(new AlarmManager\.AlarmClockInfo\(when, open\)/);
    assert.match(reminders,/manager\.setAlarmClock\(new AlarmManager\.AlarmClockInfo\(item\.dueAt,open\)/);
    assert.match(reminders,/pending\(c,id,item\.dueAt,FIRE\)/);
    assert.match(reminders,/new Intent\(c,ReminderActivity\.class\)/);
    assert.match(reminders,/manager\.setExactAndAllowWhileIdle\(AlarmManager\.RTC_WAKEUP,\s*item\.dueAt\+WakeCascade\.SECOND_STAGE_MS/);
    assert.match(reminders,/public static void cancel\(Context c,int id\)/);
});

test('Watch reminder holds CPU awake across 60s phase boundary like actual Watch alarm',()=>{
    const reminder=watch('WatchReminderService'),wake=watch('WatchAlarmService');
    assert.match(wake,/PowerManager\.PARTIAL_WAKE_LOCK/);
    assert.match(reminder,/PowerManager\.PARTIAL_WAKE_LOCK/);
    assert.match(reminder,/wakeLock\.acquire\(MAX_LOCK_MS\)/);
    assert.match(reminder,/holdCpuForReminder\(\)/);
    assert.match(reminder,/WatchCascade\.untilStrong\(due,System\.currentTimeMillis\(\)\)/);
    assert.match(reminder,/handler\.postDelayed\(localEscalate/);
    assert.match(reminder,/if\(wakeLock\.isHeld\(\)\)wakeLock\.release\(\)/);
    assert.match(reminder,/WatchAlarmPriority\.blocksReminder/);
    assert.match(read('watch/src/test/java/com/cobrachris/watchvibrationpoc/WatchAlarmPriorityTest.java'),
            /staleAlarmRecordNeverBlocksFutureReminders/);
});

test('actual device acknowledgement is persisted rather than equating message send with vibration',()=>{
    const reminder=watch('WatchReminderService'),phoneListen=phone('WearPhoneListener'),
          bridge=phone('ReminderWatchBridge'),status=phone('ReminderPlaybackStatus');
    assert.match(reminder,/boolean pulsed=vibrate\(false\)/);
    assert.match(reminder,/if\(played==0&&pulsed\)sendAck\("GENTLE"\)/);
    assert.match(reminder,/if\(vibrate\(true\)\)sendAck\("STRONG"\)/);
    assert.match(reminder,/sendAck\("STARTED"\)/);
    assert.match(reminder,/sendAck\("START_ERROR"\)/);
    assert.match(reminder,/sendAck\("NO_VIBRATOR"\)/);
    assert.match(reminder,/sendAck\("VIBRATION_ERROR"\)/);
    assert.match(bridge,/public static final String ACK="\/sanftwecker\/reminder\/v1\/ack"/);
    assert.match(phoneListen,/if\(ReminderWatchBridge\.ACK\.equals\(event\.getPath\(\)\)\)/);
    assert.match(phoneListen,/ReminderPlaybackStatus\.watch\(this,id,due,phase\)/);
    assert.match(status,/public static synchronized void watchSent/);
    assert.match(status,/public static synchronized void summary|public static synchronized String summary/);
    assert.match(status,/return "Handy: "/);
    assert.match(bridge,/ReminderPlaybackStatus\.watchSent\(c,item\.id,item\.dueAt\)/);
});

test('real phone tone/phase and failure are visible for an exact occurrence in Reminders',()=>{
    const receiver=phone('ReminderReceiver'),ring=phone('RingingService'),
          adapter=phone('ReminderToneService'),screen=phone('ReminderActivity');
    assert.match(receiver,/ReminderPlaybackStatus\.fired\(c,item\.id,item\.dueAt\)/);
    assert.match(ring,/ReminderPlaybackStatus\.phone\(this,reminderId,reminderDue,/);
    assert.match(ring,/started\?"STUFE_2_HANDY":"HANDYVIBRATION_BLOCKIERT"/);
    assert.match(adapter,/ReminderPlaybackStatus\.phone\(c,item\.id,item\.dueAt,"START_GESPERRT"\)/);
    assert.match(screen,/ReminderPlaybackStatus\.summary\(this,item\.id,item\.dueAt\)/);
    assert.match(screen,/Letzter Auslöseversuch: /);
    assert.match(ring,/requestedProfile=ReminderAlarmConfig\.profile\(this\)/);
    assert.match(adapter,/new Intent\(c,RingingService\.class\)/);
    assert.match(watch('SanftweckerWearListener'),/WatchReminderService\.START/);
});

test('existing reminder data and actions are preserved',()=>{
    const store=phone('ReminderStore'),recv=phone('ReminderReceiver');
    assert.match(store,/sanftwecker_reminders_v1/);
    for(const action of ['CHOICE','DONE','LATER','STOP'])
        assert.match(recv,new RegExp('ReminderScheduler\\.'+action+'\\.equals\\(action\\)'));
    assert.match(recv,/ReminderWatchBridge\.stop\(c,item\)/);
    assert.match(recv,/ReminderToneService\.stop\(c,item\)/);
    assert.match(phone('ReminderScheduler'),/pending\(c,id,0,ESCALATE\)/);
});

test('reminder screen checks the actually installed Watch APK instead of assuming relay transfer equals installation',()=>{
 const ui=phone('ReminderActivity'),receiver=phone('WearPhoneListener'),
       listener=watch('SanftweckerWearListener');
 assert.match(ui,/installed_watch_code/);
 assert.match(ui,/installed_watch_checked/);
 assert.match(ui,/\/sanftwecker\/update\/v1\/version-request/);
 assert.match(ui,/Update auf v"\+phone\+" erforderlich/);
 assert.match(ui,/ReminderPlaybackStatus\.summary\(this,item\.id,item\.dueAt\)/);
 assert.match(receiver,/putLong\("installed_watch_code",watchCode\)/);
 assert.match(listener,/\/sanftwecker\/update\/v1\/version-response/);
});
