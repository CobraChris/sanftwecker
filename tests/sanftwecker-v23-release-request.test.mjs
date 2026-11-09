import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {
  REPO,REQUEST,EXPECTED_CODE,PREVIOUS_SHA256,EXPECTED_PACKAGE,
  validateRequest,validateDiff,validateActor
} from '../scripts/sanftwecker-v23-release-request.mjs';
const base='a'.repeat(40),after='b'.repeat(40);
const valid={intent:'signed-native-wear-v23',request_id:'2026-11-09-native-watch',
  expected_version_code:23,source_sha:base,
  watch_application_id:'com.cobrachris.sanftwecker',
  confirmation:'SIGN_PHONE_AND_WATCH_WITH_EXISTING_V21_CERTIFICATE'};
test('owner must explicitly approve two APKs signed with existing identity',()=>{
  assert.deepEqual(validateRequest(valid),{source_sha:base,expected_version_code:'23'});
  for(const v of [
    {expected_version_code:22},{expected_version_code:24},{source_sha:'main'},
    {watch_application_id:'com.cobrachris.watchvibrationpoc'},
    {confirmation:'GENERATE_NEW_KEY'},{request_id:'../evil'},{untrusted:true}
  ])assert.throws(()=>validateRequest({...valid,...v}));
});
test('only single new file and exact previous main qualifies',()=>{
  validateDiff(base,after,'A\t'+REQUEST+'\n',base);
  for(const changed of [
    'M\t'+REQUEST,'A\t'+REQUEST+'\nM\twatch/build.gradle',
    'A\tother.json','D\t'+REQUEST
  ])assert.throws(()=>validateDiff(base,after,changed,base));
  assert.throws(()=>validateDiff(base,after,'A\t'+REQUEST,after));
  assert.throws(()=>validateDiff(base,base,'A\t'+REQUEST,base));
});
test('trusted main actor only',()=>{
  const ok={GITHUB_REPOSITORY:REPO,GITHUB_ACTOR:'CobraChris',
    GITHUB_REF:'refs/heads/main',GITHUB_EVENT_NAME:'push'};
  validateActor(ok);
  for(const change of [
    {GITHUB_ACTOR:'github-actions[bot]'},{GITHUB_REPOSITORY:'evil/repo'},
    {GITHUB_REF:'refs/heads/dev'},{GITHUB_EVENT_NAME:'workflow_dispatch'}
  ])assert.throws(()=>validateActor({...ok,...change}));
});
test('release pipeline checks original v21 and both final APK certificates',()=>{
  const source=readFileSync(new URL('../.github/workflows/sanftwecker-v23-release.yml',import.meta.url),'utf8');
  for(const token of [
    "requests/sanftwecker-v23-release.json","environment: android-releases",
    "github.actor == 'CobraChris'","persist-credentials: false",
    'sanftwecker-v21',PREVIOUS_SHA256,
    'APP_SIGNING_KEYSTORE_BASE64','sanftwecker-watch-v23.apk',
    'sanftwecker-release.apk','cmp -s signed/phone.cert.sha signed/watch.cert.sha',
    'cmp -s signed/phone.cert.sha legacy/cert.sha',
    "test -s watch/build/outputs/apk/release/watch-release-unsigned.apk"
  ])assert.ok(source.includes(token),token);
  assert.equal(EXPECTED_CODE,23);
  assert.equal(EXPECTED_PACKAGE,'com.cobrachris.sanftwecker');
  assert.doesNotMatch(source,/workflow_dispatch:|pull_request_target:|genkeypair|uses:\s*[^\s]+@v[0-9]+\s*$/m);
  assert.ok(source.indexOf('Verify original v21 release before reading secrets')<
      source.indexOf('Sign both original-certificate APKs'));
});