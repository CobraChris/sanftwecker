import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const phone=name=>readFileSync(new URL(
    '../alarm/src/main/java/com/cobrachris/sanftwecker/'+name+'.java',
    import.meta.url),'utf8');
const manifest=()=>readFileSync(new URL('../alarm/src/main/AndroidManifest.xml',
    import.meta.url),'utf8');

test('only genuinely fired reminder requests full-screen alarm display',()=>{
    const notice=phone('ReminderNotice'),receiver=phone('ReminderReceiver');
    assert.match(notice,/show\(Context c,ReminderStore.Item item,boolean strong,\s*boolean justFired\)/);
    assert.match(notice,/if\(justFired\)builder.setFullScreenIntent\(fullView,true\)/);
    assert.match(notice,/cascade\?Notification.CATEGORY_ALARM/);
    assert.match(receiver,/ReminderScheduler.FIRE.equals\(action\)[\s\S]*?ReminderNotice.show\(c,item,false\)/);
    assert.match(receiver,/ReminderScheduler.ESCALATE.equals\(action\)[\s\S]*?ReminderNotice.show\(c,item,true\)/);
    assert.match(phone('ReminderScheduler'),/ReminderNotice.show\(c,item,false\)/);
    assert.doesNotMatch(receiver,/startActivity\(/);
});

test('full-screen intent opens own occurrence-safe internal options activity',()=>{
    const n=phone('ReminderNotice'),ui=phone('ReminderOptionsActivity');
    assert.match(n,/new Intent\(c,ReminderOptionsActivity.class\)/);
    assert.match(n,/putExtra\(ReminderOptionsActivity.EXTRA_FULLSCREEN,true\)/);
    assert.match(n,/PendingIntent.getActivity\(c,BASE\+10_000\+item.id/);
    assert.match(phone('RingingService'),/builder.setFullScreenIntent\(openApp\(\), true\)/);
    assert.match(phone('RingingService'),/new Intent\(this,ReminderOptionsActivity.class\)/);
    assert.match(ui,/if\(getIntent\(\).getBooleanExtra\(EXTRA_FULLSCREEN,false\)\)/);
    assert.match(ui,/setShowWhenLocked\(true\)/);
    assert.match(ui,/setTurnScreenOn\(true\)/);
    assert.match(ui,/item.dueAt!=due/);
    assert.match(ui,/current.dueAt!=due/);
    assert.match(manifest(),/ReminderOptionsActivity" android:exported="false"/);
    assert.match(manifest(),/android.permission.USE_FULL_SCREEN_INTENT/);
});

test('all reminder actions retain distinct stop, done and postponement semantics',()=>{
    const ui=phone('ReminderOptionsActivity'),recv=phone('ReminderReceiver');
    for(const label of ['In 10 Minuten nochmal erinnern','In 30 Minuten nochmal erinnern',
        'In 60 Minuten nochmal erinnern','Nächsten Tag wieder erinnern',
        'Stoppen (bleibt offen)','Erledigt (abschließen)'])
        assert.ok(ui.includes(label),label);
    assert.match(ui,/dispatch\(ReminderScheduler.STOP,0\)/);
    assert.match(ui,/dispatch\(ReminderScheduler.DONE,0\)/);
    assert.match(recv,/ReminderScheduler.STOP.equals\(action\)/);
    assert.match(recv,/ReminderScheduler.DONE.equals\(action\)/);
    assert.match(recv,/ReminderScheduler.CHOICE.equals\(action\)/);
});
