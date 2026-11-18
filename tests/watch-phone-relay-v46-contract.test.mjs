import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const get=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');

test('phone only relays a previously verified same-version Watch APK',()=>{
 const ui=get('alarm/src/main/java/com/cobrachris/sanftwecker/WatchUpdateActivity.java');
 const downloader=get('alarm/src/main/java/com/cobrachris/sanftwecker/VerifiedWatchApkDownloader.java');
 const relay=get('alarm/src/main/java/com/cobrachris/sanftwecker/PhoneWatchUpdateRelay.java');
 assert.match(ui,/VerifiedWatchApkDownloader\.download/);
 assert.match(ui,/PhoneWatchUpdateRelay\.send\(getApplicationContext\(\),verifiedUri/);
 assert.match(ui,/transfer\.setEnabled\(!busy&&verifiedUri!=null\)/);
 assert.match(downloader,/verifyOriginalSigningIdentity/);
 assert.match(downloader,/expectedSha\.equals\(hex\.toString\(\)\)/);
 assert.match(relay,/getConnectedNodes\(\)/);
 assert.match(relay,/nodes\.isEmpty\(\)/);
 assert.match(relay,/putAsset\("apk",Asset\.createFromBytes\(bytes\)\)/);
 assert.match(relay,/expectedSha\.equals\(actual\.toString\(\)\)/);
 assert.doesNotMatch(relay,/PackageInstaller|ACTION_INSTALL_PACKAGE|adb shell|pm install/);
});

test('watch rejects invalid or stale APK before exposing file provider to installer',()=>{
 const receiver=get('watch/src/main/java/com/cobrachris/watchvibrationpoc/PhoneDeliveredWatchUpdate.java');
 const client=get('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchSelfUpdateClient.java');
 const ui=get('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchSelfUpdateActivity.java');
 assert.match(receiver,/WatchSelfUpdateClient\.requireIdleAlarm\(context\)/);
 assert.match(receiver,/code<=WatchSelfUpdateClient\.installedCode\(context\)/);
 assert.match(receiver,/WatchSelfUpdatePolicy\.MAX_BYTES/);
 assert.match(receiver,/sha\.equals\(actual\.toString\(\)\)/);
 assert.match(receiver,/WatchSelfUpdateClient\.verifyIdentity\(context,partial,code\)/);
 assert.match(receiver,/WatchSelfUpdateClient\.verifyIdentity\(context,file,code\)/);
 assert.match(client,/getApkContentsSigners\(\)/);
 assert.match(ui,/PhoneDeliveredWatchUpdate\.verifiedPending\(this\)/);
 assert.match(ui,/setPositiveButton\("Weiter"/);
 assert.doesNotMatch(receiver,/ACTION_INSTALL_PACKAGE|ACTION_VIEW|pm install|adb shell/);
});

test('wear update path is isolated, and watch acknowledgement is separate from installation',()=>{
 const phoneManifest=get('alarm/src/main/AndroidManifest.xml');
 const watchManifest=get('watch/src/main/AndroidManifest.xml');
 const phoneListener=get('alarm/src/main/java/com/cobrachris/sanftwecker/WearPhoneListener.java');
 const watchListener=get('watch/src/main/java/com/cobrachris/watchvibrationpoc/SanftweckerWearListener.java');
 const receiver=get('watch/src/main/java/com/cobrachris/watchvibrationpoc/PhoneDeliveredWatchUpdate.java');
 for(const manifest of [phoneManifest,watchManifest])
   assert.match(manifest,/sanftwecker\/update\/v1\//);
 assert.match(watchListener,/PhoneDeliveredWatchUpdate\.receive/);
 assert.match(receiver,/ready:\+code/);
 assert.match(phoneListener,/PhoneWatchUpdateRelay\.ACK/);
 assert.match(phoneListener,/verified_code/);
 assert.match(receiver,/notifyReady\(context,code\)/);
 assert.doesNotMatch(receiver,/silentInstall|setRequireUserAction\(.*NOT_REQUIRED/);
});

test('v46 does not amend historical release gates or remove the fallback update',()=>{
 for(const kind of ['alarm','watch']){
   const gradle=get(kind+'/build.gradle');
   assert.match(gradle,/versionCode 46/);
 }
 const watch=get('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchSelfUpdateActivity.java');
 const phone=get('alarm/src/main/java/com/cobrachris/sanftwecker/WatchUpdateActivity.java');
 assert.match(watch,/WatchSelfUpdateClient\.check/);
 assert.match(watch,/WatchSelfUpdateClient\.download/);
 assert.match(phone,/shareWithInstaller/);
 assert.match(phone,/saveToDownloads/);
});
