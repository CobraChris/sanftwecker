import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const get=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const phone='alarm/src/main/java/com/cobrachris/sanftwecker/';
const watch='watch/src/main/java/com/cobrachris/watchvibrationpoc/';

test('one-tap Watch setup orchestrates match, verified download, relay and Watch acknowledgment',()=>{
 const page=get(phone+'WatchUpdateActivity.java');
 assert.match(page,/Watch-Update mit einem Tippen vorbereiten/);
 assert.match(page,/private void startGuided\(\)/);
 assert.match(page,/handler\.post\(this::downloadWatch\)/);
 assert.match(page,/handler\.post\(this::sendToWatch\)/);
 assert.match(page,/VerifiedWatchApkDownloader\.download/);
 assert.match(page,/PhoneWatchUpdateRelay\.send/);
 assert.match(page,/ackMonitor/);
 assert.match(page,/getLong\("verified_code"/);
 assert.match(page,/at>=relayStartedAt/);
 assert.match(page,/getBooleanExtra\(EXTRA_GUIDED,false\)/);
 assert.match(page,/onDestroy\(\)/);
});

test('after phone update offer companion setup, but user may postpone',()=>{
 const main=get(phone+'MainActivity.java');
 assert.match(main,/offerCompanionUpdateAfterUpgrade\(\)/);
 assert.match(main,/version<48/);
 assert.match(main,/prompted_version/);
 assert.match(main,/version-request/);
 assert.match(main,/getConnectedNodes\(\)/);
 assert.match(main,/getLong\("installed_watch_checked"/);
 assert.match(main,/watchCode>=version/);
 assert.match(main,/setPositiveButton\("Watch-Update starten"/);
 assert.match(main,/setNegativeButton\("Später",null\)/);
 assert.match(main,/WatchUpdateActivity\.EXTRA_GUIDED,true/);
});

test('Watch version exchange is distinct from signed APK reception and Android confirmation',()=>{
 const watchListener=get(watch+'SanftweckerWearListener.java');
 const phoneListener=get(phone+'WearPhoneListener.java');
 const watchManifest=get('watch/src/main/AndroidManifest.xml');
 const phoneManifest=get('alarm/src/main/AndroidManifest.xml');
 const receiver=get(watch+'PhoneDeliveredWatchUpdate.java');
 const watchUi=get(watch+'WatchSelfUpdateActivity.java');
 assert.match(watchListener,/version-request/);
 assert.match(watchListener,/version-response/);
 assert.match(watchListener,/getLongVersionCode/);
 assert.match(phoneListener,/installed_watch_code/);
 assert.match(phoneListener,/installed_watch_checked/);
 assert.match(watchManifest,/android:path="\/sanftwecker\/update\/v1\/version-request"/);
 assert.match(phoneManifest,/android:path="\/sanftwecker\/update\/v1\/version-response"/);
 assert.match(receiver,/WatchSelfUpdateClient\.verifyIdentity/);
 assert.match(receiver,/notifyReady\(context,code\)/);
 assert.match(watchUi,/confirmInstall\(\)/);
 assert.doesNotMatch(receiver,/silentInstall|setRequireUserAction\(.*NOT_REQUIRED/);
});

test('two independently signed packages retained while user needs only phone download link',()=>{
 const phoneGradle=get('alarm/build.gradle'),watchGradle=get('watch/build.gradle');
 assert.ok(Number.parseInt(phoneGradle.split('versionCode ')[1],10)>=48);
 assert.ok(Number.parseInt(watchGradle.split('versionCode ')[1],10)>=48);
 const fallback=get(phone+'WatchUpdateActivity.java');
 assert.match(fallback,/saveToDownloads\(\)/);
 assert.match(fallback,/shareWithInstaller\(\)/);
 const release=get('.github/workflows/sanftwecker-v48-release.yml');
 assert.match(release,/sanftwecker-release\.apk/);
 assert.match(release,/sanftwecker-watch-v48\.apk/);
 assert.match(release,/environment: android-releases/);
});
