import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
test('navigation order changes only presentation, not stable screen keys',()=>{
 const tabs=read('alarm/src/main/java/com/cobrachris/sanftwecker/ClockTabs.java');
 assert.match(tabs,/String\[\] names=\{"Wecker","Erinnerungen","Timer","Stoppuhr","Weltuhr"\}/);
 assert.match(tabs,/Class<\?>\[\] targets=\{MainActivity.class,ReminderActivity.class,/);
 assert.match(tabs,/ALARMS=0, TIMERS=1, STOPWATCH=2, WORLD_CLOCK=3, REMINDERS=4/);
});
test('verified APK from phone is prominently installable without redundant Watch searches',()=>{
 const ui=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchSelfUpdateActivity.java');
 assert.match(ui,/JETZT UPDATE INSTALLIEREN/);
 assert.ok(ui.indexOf('content.addView(install')<ui.indexOf('content.addView(check'));
 assert.match(ui,/boolean ready=!busy&&verified!=null&&candidate!=null/);
 assert.match(ui,/check.setVisibility\(ready\?View.GONE:View.VISIBLE\)/);
 assert.match(ui,/download.setVisibility\(ready\?View.GONE:View.VISIBLE\)/);
 assert.match(ui,/install.setVisibility\(ready\?View.VISIBLE:View.GONE\)/);
 assert.match(ui,/PhoneDeliveredWatchUpdate.verifiedPending\(this\)/);
 assert.match(ui,/WatchSelfUpdateClient.installedCode\(this\)>=candidate.code/);
 assert.match(ui,/confirmInstall\(\)/);
 assert.match(ui,/getPackageManager\(\).canRequestPackageInstalls\(\)/);
});
test('visible notification points to already verified installation without auto consent',()=>{
 const incoming=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/PhoneDeliveredWatchUpdate.java');
 assert.match(incoming,/WatchSelfUpdateClient.verifyIdentity\(context,partial,code\)/);
 assert.match(incoming,/NotificationManager.IMPORTANCE_HIGH/);
 assert.match(incoming,/Jetzt antippen: geprüfte APK installieren/);
 assert.match(incoming,/new Intent\(context,WatchSelfUpdateActivity.class\)/);
 assert.doesNotMatch(incoming,/silentInstall|setRequireUserAction\(.*NOT_REQUIRED/);
});

test('only a verified newer watch APK gets green install highlight and success message',()=>{
 const ui=read('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchSelfUpdateActivity.java');
 assert.match(ui,/boolean ready=!busy&&verified!=null&&candidate!=null/);
 assert.match(ui,/ColorStateList\.valueOf\(Color\.rgb\(30,150,78\)\):normalInstallTint/);
 assert.match(ui,/status\.setTextColor\(ready\?Color\.rgb\(112,235,160\):normalStatusColor\)/);
 assert.match(ui,/if\(ready\)install\.setText\("✓ UPDATE v"\+candidate\.code\+" INSTALLIEREN"\)/);
 assert.match(ui,/✓ UPDATE BEREIT: Watch v/);
 assert.match(ui,/normalInstallTint=install\.getBackgroundTintList\(\)/);
 assert.match(ui,/PhoneDeliveredWatchUpdate\.verifiedPending\(this\)/);
 assert.doesNotMatch(ui,/setRequireUserAction\(.*NOT_REQUIRED/);
});
