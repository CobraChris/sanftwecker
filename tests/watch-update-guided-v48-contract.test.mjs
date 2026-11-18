import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const src=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const phone='alarm/src/main/java/com/cobrachris/sanftwecker/';

test('phone upgrade offers an opt-in exactly once per installed version',()=>{
 const main=src(phone+'MainActivity.java');
 assert.match(main,/offerWatchUpdateAfterPhoneUpgrade/);
 assert.match(main,/offered_for_code/);
 assert.match(main,/code<48/);
 assert.match(main,/setPositiveButton\("Watch-Update vorbereiten"/);
 assert.match(main,/WatchUpdateActivity.EXTRA_START_GUIDED,true/);
 assert.match(main,/Galaxy Watch aktualisieren/);
 assert.doesNotMatch(main,/ACTION_INSTALL_PACKAGE|pm install/);
});

test('single Watch update action does discovery, verified download and handoff sequentially',()=>{
 const ui=src(phone+'WatchUpdateActivity.java');
 const downloader=src(phone+'VerifiedWatchApkDownloader.java');
 const relay=src(phone+'PhoneWatchUpdateRelay.java');
 assert.match(ui,/runGuidedUpdate\(\)/);
 assert.match(ui,/findMatchingWatchAsset\(json,code\)/);
 assert.match(ui,/VerifiedWatchApkDownloader\.download/);
 assert.match(ui,/PhoneWatchUpdateRelay\.send\(getApplicationContext\(\),file/);
 const first=ui.indexOf('String json=UpdateActivity.fetchReleases(API)');
 const check=ui.indexOf('Candidate match=findMatchingWatchAsset(json,code)');
 const verify=ui.indexOf('Uri file=VerifiedWatchApkDownloader.download');
 const transfer=ui.indexOf('PhoneWatchUpdateRelay.send(getApplicationContext(),file');
 assert.ok(first<check&&check<verify&&verify<transfer);
 assert.match(downloader,/verifyOriginalSigningIdentity\(context,ready,expectedCode\)/);
 assert.match(relay,/expectedSha\.equals\(actual\.toString\(\)\)/);
 assert.match(relay,/getConnectedNodes\(\)/);
 assert.doesNotMatch(ui,/ACTION_INSTALL_PACKAGE|PackageInstaller/);
});

test('existing v46 Watch receiver remains compatible, and manual install is mandatory',()=>{
 const watch=src('watch/src/main/java/com/cobrachris/watchvibrationpoc/PhoneDeliveredWatchUpdate.java');
 const activity=src('watch/src/main/java/com/cobrachris/watchvibrationpoc/WatchSelfUpdateActivity.java');
 assert.match(watch,/PATH="\/sanftwecker\/update\/v1\/apk"/);
 assert.match(watch,/WatchSelfUpdateClient\.verifyIdentity\(context,partial,code\)/);
 assert.match(watch,/notifyReady\(context,code\)/);
 assert.match(activity,/setPositiveButton\("Weiter"/);
 assert.match(activity,/PhoneDeliveredWatchUpdate\.verifiedPending\(this\)/);
 const phoneUi=src(phone+'WatchUpdateActivity.java');
 assert.match(phoneUi,/shareWithInstaller/);
 assert.match(phoneUi,/saveToDownloads/);
});

test('v48 version and strict original release gate match',()=>{
 for(const mod of ['alarm','watch']){
   const gradle=src(mod+'/build.gradle');
   assert.match(gradle,/versionCode 48/);
 }
 const workflow=src('.github/workflows/sanftwecker-v48-release.yml');
 assert.match(workflow,/environment: android-releases/);
 assert.match(workflow,/gh release download sanftwecker-v47/);
 assert.match(workflow,/sanftwecker-watch-v48.apk/);
 assert.match(workflow,/needs.validate.outputs.source_sha/);
 assert.doesNotMatch(workflow,/workflow_dispatch:|genkeypair/);
});
