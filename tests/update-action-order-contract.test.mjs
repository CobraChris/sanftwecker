import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const java=name=>readFileSync(new URL('../alarm/src/main/java/com/cobrachris/sanftwecker/'+name+'.java',import.meta.url),'utf8');
const update=java('UpdateActivity');

test('search is the first highlighted action and install is absent until a new release is selected',()=>{
  const create=update.split('@Override public void onCreate(Bundle saved) {')[1]
    .split('private void updateActions(){')[0];
  assert.ok(create.indexOf('check=UiKit.button(this,"Nach Updates suchen",true)')<
    create.indexOf('install=UiKit.button(this,"Update installieren",true)'));
  assert.match(update,/boolean hasCandidate=candidate!=null/);
  assert.match(update,/install\.setVisibility\(hasCandidate\?View\.VISIBLE:View\.GONE\)/);
  assert.match(update,/install\.setEnabled\(hasCandidate&&!busy\)/);
  assert.match(update,/install\.setText\(hasCandidate\?"Update auf v"\+candidate\.code\+" installieren"/);
  assert.match(update,/browser\.setVisibility\(hasCandidate\?View\.VISIBLE:View\.GONE\)/);
  assert.match(update,/check\.setBackground\(UiKit\.shape\(hasCandidate\?UiKit\.CARD_2:UiKit\.BLUE,this,16\)\)/);
  assert.doesNotMatch(update,/ClockTabs\.add\(this,root,ClockTabs\.ALARMS\)/);
});

test('all verification outcomes keep install hidden unless a verified newer candidate exists',()=>{
  assert.match(update,/checking=true;\s*candidate=null;\s*updateActions\(\)/);
  assert.match(update,/if\(networkFailed\)\{[\s\S]*?else if\(selected==null\)\{/);
  assert.match(update,/else if\(!UpdateUrlPolicy\.isNewer\(selected\.code,getCurrentVersion\(\)\)\)\{/);
  assert.match(update,/Du verwendest die aktuelle Version/);
  assert.match(update,/candidate=selected;\s*}\s*updateActions\(\)/);
  assert.match(update,/if\(downloading\|\|checking\)return/);
  assert.match(update,/if\(selected==null\|\|downloading\|\|checking\)return/);
  assert.match(update,/candidate=null;\s*updateActions\(\);\s*status\.setText\("Updatequelle ist nicht mehr gültig/);
});

test('browser, watch updates and all original signature and download safety survive',()=>{
  assert.match(update,/secondaryOptions\.setVisibility\(View\.GONE\)/);
  assert.match(update,/Weitere Möglichkeiten öffnen/);
  assert.match(update,/new Intent\(this,WatchUpdateActivity\.class\)/);
  assert.match(update,/Intent\.ACTION_VIEW,Uri\.parse\(selected\.url\)/);
  assert.match(update,/VerifiedUpdatePolicy\.isNewerApprovedUpdate\(/);
  assert.match(update,/VerifiedApkDownloader\.download\(/);
  assert.match(update,/Settings\.ACTION_MANAGE_UNKNOWN_APP_SOURCES/);
  assert.match(update,/getPackageManager\(\)\.canRequestPackageInstalls\(\)/);
  assert.match(update,/intent\.setClipData\(ClipData\.newUri/);
  assert.match(update,/startActivity\(intent\)/);
  assert.doesNotMatch(update,/PackageInstaller\.Session|\.deletePackage\(/);
  const verifier=java('VerifiedApkDownloader');
  assert.match(verifier,/getApkContentsSigners\(\)/);
  assert.match(verifier,/MessageDigest\.getInstance\("SHA-256"\)/);
  assert.match(verifier,/context\.getPackageName\(\)\.equals\(downloaded\.packageName\)/);
});
