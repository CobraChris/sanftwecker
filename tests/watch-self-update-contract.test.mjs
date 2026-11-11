import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const get=path=>readFileSync(new URL('../'+path,import.meta.url),'utf8');

test('watch update requests explicit foreground user confirmation, never adb',()=>{
 const activity=get('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchSelfUpdateActivity.java');
 assert.match(activity,/new AlertDialog\.Builder\(this\)/);
 assert.match(activity,/setPositiveButton\("Weiter"/);
 assert.match(activity,/canRequestPackageInstalls\(\)/);
 assert.match(activity,/ACTION_MANAGE_UNKNOWN_APP_SOURCES/);
 assert.match(activity,/Intent\.ACTION_VIEW/);
 assert.match(activity,/Intent\.ACTION_INSTALL_PACKAGE/);
 assert.match(activity,/FLAG_GRANT_READ_URI_PERMISSION/);
 assert.doesNotMatch(activity,/pm install|adb shell|setRequireUserAction\(.*NOT_REQUIRED|INSTALL_PACKAGES/);
});

test('watch signed update is separate from alarm service and validates original signer',()=>{
 const client=get('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchSelfUpdateClient.java');
 assert.match(client,/WatchSelfUpdatePolicy\.approved/);
 assert.match(client,/WatchSelfUpdatePolicy\.trustedHop/);
 assert.match(client,/MessageDigest\.getInstance\("SHA-256"\)/);
 assert.match(client,/verifyIdentity\(c,ready,candidate.code\)/);
 assert.match(client,/getApkContentsSigners\(\)/);
 assert.match(client,/WatchAlarmService\.ACTIVE_PREF/);
 assert.doesNotMatch(client,/startForegroundService|WatchScheduleStore\.clear/);
});

test('self-update UI is opt-in and private; existing alarm path unchanged',()=>{
 const manifest=get('watch/src/main/AndroidManifest.xml');
 assert.match(manifest,/android:name="android\.permission\.REQUEST_INSTALL_PACKAGES"/);
 assert.match(manifest,/android:name="android\.permission\.INTERNET"/);
 assert.match(manifest,/android:name="\.WatchSelfUpdateActivity" android:exported="false"/);
 assert.match(manifest,/androidx\.core\.content\.FileProvider/);
 assert.match(manifest,/@xml\/watch_update_file_paths/);
 const paths=get('watch/src/main/res/xml/watch_update_file_paths.xml');
 assert.match(paths,/watch_self_updates\//);
 assert.doesNotMatch(paths,/<external-path/);
 const main=get('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchMainActivity.java');
 assert.match(main,/Uhr aktualisieren/);
 assert.match(main,/new Intent\(this,WatchSelfUpdateActivity\.class\)/);
 const alarm=get('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchAlarmService.java');
 assert.match(alarm,/void requestSnooze\(\)/);
 assert.match(alarm,/void stopHaptics\(boolean acknowledged\)/);
});
