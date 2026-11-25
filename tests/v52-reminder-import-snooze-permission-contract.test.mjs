import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const phone=n=>read('alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java');

test('all five visual tabs keep their historical target identities',()=>{
    const tabs=phone('ClockTabs');
    assert.match(tabs,/ALARMS=0, TIMERS=1, STOPWATCH=2, WORLD_CLOCK=3, REMINDERS=4/);
    assert.match(tabs,/final int\[\] tabIds=\{ALARMS,REMINDERS,TIMERS,STOPWATCH,WORLD_CLOCK\}/);
    assert.match(tabs,/tabIds\[i\]==selected\?UiKit.BLUE/);
    assert.match(tabs,/if\(tabIds\[index\]==selected\)return/);
    assert.match(phone('ReminderActivity'),/ClockTabs.add\(this,root,ClockTabs.REMINDERS\)/);
});

test('reminders are edited in place with fields prefilled and prior state restored on failure',()=>{
    const activity=phone('ReminderActivity'),store=phone('ReminderStore');
    assert.match(activity,/Button edit=UiKit.button\(this,"Erinnerung bearbeiten"/);
    assert.match(activity,/edit.setOnClickListener\(v->editor\(item\)\)/);
    assert.match(activity,/editingId=existing==null\?-1:existing.id/);
    // Existing saved alarm-related fields remain unchanged but can no longer
    // override the real Testwecker's active output.
    for(const prop of ['existing.title','existing.dueAt','existing.repeat',
        'existing.postponeMinutes','previous.watchIntensity','previous.watchPulses',
        'previous.watchIntervalSeconds','previous.tone','previous.phoneSound'])
        assert.ok(activity.includes(prop),prop);
    assert.match(activity,/Ausgabe wie Testwecker/);
    assert.doesNotMatch(activity,/phoneSound.isChecked|cascade.isChecked/);
    assert.match(store,/public static synchronized void update\(Context c,int id/);
    assert.match(store,/public static synchronized void restore\(Context c,Item item\)/);
    assert.match(activity,/ReminderStore.update\(this,id/);
    assert.match(activity,/ReminderScheduler.cancel\(this,id\)/);
    assert.match(activity,/ReminderScheduler.schedule\(this,id\)/);
    assert.match(activity,/ReminderStore.restore\(this,previous\)/);
    assert.match(activity,/ReminderStore.abandon\(this,id\)/);
});

test('snoozing survives reboot and has a quiet persistent, public lock screen notice',()=>{
    const note=phone('SnoozeNotice'),scheduler=phone('AlarmScheduler');
    const receiver=phone('SnoozeActionReceiver');
    assert.match(note,/NotificationManager.IMPORTANCE_LOW/);
    assert.match(note,/Notification.VISIBILITY_PUBLIC/);
    assert.match(note,/setOngoing\(true\)/);
    assert.match(note,/Klingelt erneut um /);
    assert.match(note,/Schlummern für heute beenden/);
    assert.doesNotMatch(note,/setFullScreenIntent/);
    assert.match(scheduler,/putInt\("snooze_id",id\).putLong\("snooze_when",when\).commit/);
    assert.match(scheduler,/public static void restoreSnooze\(Context c\)/);
    assert.match(phone('RescheduleReceiver'),/AlarmScheduler.restoreSnooze\(c\)/);
    assert.match(phone('RingingService'),/if\(snoozing\)SnoozeNotice.show\(this,id,snoozeUntil\)/);
    assert.match(phone('AlarmReceiver'),/AlarmScheduler.snoozedWhen\(c\)!=when/);
    assert.match(receiver,/when!=AlarmScheduler.snoozedWhen\(c\)/);
    assert.match(receiver,/AlarmScheduler.cancelSnooze\(c\)/);
    assert.match(read('alarm/src/main/AndroidManifest.xml'),/SnoozeActionReceiver" android:exported="false"/);
});

test('screenshot import is opt-in, idempotent, preserves current alarms and stays disabled',()=>{
    const imp=phone('AlarmPresetImporter'),main=phone('MainActivity');
    const presets=[...imp.matchAll(/new Preset\("/g)];
    assert.equal(presets.length,12);
    for(const match of ['"6:45 Uhr bei Gero los",5,30','"7 Uhr bei Gero los",5,45',
        '"Kontingent",6,50','"Kontingent",8,1',
        '"Wochenende (Vibration)",10,30','"Auto umparken",20,40'])
        assert.ok(imp.includes(match),match);
    assert.match(imp,/if\(wasImported\(c\)\)return 0/);
    assert.match(imp,/if\(exists\(AlarmRepository.all\(c\),preset\)\)continue/);
    assert.match(imp,/new AlarmProfile\(id,preset.name,false/);
    assert.match(imp,/AlarmRepository.create\(c,null\)/);
    assert.match(imp,/AlarmProfile reference=referenceAlarm\(c\)/);
    assert.match(imp,/AlarmRepository.put\(c,identity.withSettingsFrom\(reference\)\)/);
    assert.match(imp,/if\(alarm.name.equals\(preset.name\)&&alarm.hour==preset.hour/);
    assert.match(main,/12 Wecker aus Screenshots übernehmen/);
    assert.match(main,/setPositiveButton\("Deaktiviert übernehmen"/);
    assert.match(main,/AlarmPresetImporter.importMissing\(this\)/);
});

test('screenshot-import fix uses the SAVED Testwecker, including old already imported entries',()=>{
    const imp=phone('AlarmPresetImporter'),alarm=phone('AlarmProfile'),main=phone('MainActivity');
    assert.match(imp,/if\(!"Testwecker".equals\(alarm.name\)\)continue/);
    assert.match(imp,/if\(match!=null\)/);
    assert.match(imp,/throw new IllegalStateException\("Mehrere Testwecker/);
    assert.match(alarm,/public AlarmProfile withSettingsFrom\(AlarmProfile reference\)/);
    for(const property of ['days','delaySeconds','rampSeconds','maxVolume',
        'watchPulses','watchIntervalSeconds','snoozeMinutes','tone','rampEnabled',
        'autoStopMinutes','snoozeEnabled','vibrationMode','audibleWhenMuted',
        'cascadeEnabled','watchIntensity'])
        assert.ok(alarm.includes('reference.'+property),property);
    assert.match(alarm,/new AlarmProfile\(id, name, enabled, hour, minute,/);
    assert.match(imp,/public static synchronized List<AlarmProfile> repairCandidates/);
    assert.match(imp,/if\(!alarm.enabled&&matchesPreset\(alarm\)/);
    assert.match(imp,/public static synchronized int applyTestweckerSettings/);
    assert.match(imp,/public static synchronized int repairUntouchedDefaultsOnce/);
    assert.match(imp,/if\(original.enabled\|\|!matchesPreset\(original\)\|\|/);
    assert.match(imp,/isOriginalImportDefaults\(original\)/);
    assert.match(main,/AlarmPresetImporter.repairUntouchedDefaultsOnce\(this\)/);
    assert.match(main,/Importierte Wecker wie Testwecker einstellen/);
    assert.match(main,/AlarmPresetImporter.applyTestweckerSettings\(this\)/);
    assert.match(main,/setPositiveButton\("Einstellungen übernehmen"/);
});

test('post-update permission warning only if Android reports missing special access',()=>{
    const notice=phone('PostUpdatePermissionNotice'),receiver=phone('RescheduleReceiver');
    assert.match(notice,/FullScreenPermissionStatus.isAllowed\(c\)/);
    assert.match(notice,/if\(Build.VERSION.SDK_INT<34\|\|FullScreenPermissionStatus.isAllowed\(c\)\)/);
    assert.match(notice,/SetupAssistantActivity.class/);
    assert.match(receiver,/PostUpdatePermissionNotice.showIfMissing\(c\)/);
    assert.match(phone('SetupAssistantActivity'),/PostUpdatePermissionNotice.clear\(this\)/);
    assert.doesNotMatch(notice,/grantPermission|setPermissionGrantState/);
});
