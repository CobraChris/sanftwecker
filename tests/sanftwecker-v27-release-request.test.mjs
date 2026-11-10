import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {
  REPO,REQUEST,EXPECTED_CODE,PHONE_V26_SHA256,WATCH_V26_SHA256,
  EXPECTED_PACKAGE,validateRequest,validateDiff,validateActor
} from '../scripts/sanftwecker-v27-release-request.mjs';

const source='a'.repeat(40),after='b'.repeat(40);
const valid={
  intent:'signed-native-wear-v27',
  request_id:'2026-11-10-phone-watch-cascade',
  expected_version_code:27,source_sha:source,
  watch_application_id:'com.cobrachris.sanftwecker',
  confirmation:'SIGN_PHONE_AND_WATCH_V27_WITH_EXISTING_V26_CERTIFICATE'
};

test('v27 approval must explicitly authorize both APKs with original signers',()=>{
  assert.deepEqual(validateRequest(valid),
    {source_sha:source,expected_version_code:'27'});
  for(const update of [
    {expected_version_code:26},{expected_version_code:28},{source_sha:'main'},
    {source_sha:'C'.repeat(40)},{source_sha:'a'.repeat(41)},
    {watch_application_id:'com.cobrachris.watchvibrationpoc'},
    {confirmation:'GENERATE_NEW_KEY'},{confirmation:'SIGN_PHONE_ONLY'},
    {request_id:'../unsafe'},{request_id:'x'}, {extra:'injected'}
  ])assert.throws(()=>validateRequest({...valid,...update}));
  for(const payload of [null,[],true,'bad'])assert.throws(()=>validateRequest(payload));
});

test('one added immutable approval file, zero modified code or scripts',()=>{
  validateDiff(source,after,'A\t'+REQUEST+'\n',source);
  for(const diff of [
    'M\t'+REQUEST,'D\t'+REQUEST,'A\tother.json',
    'A\t'+REQUEST+'\nM\twatch/build.gradle',
    'A\t'+REQUEST+'\nA\tother.json',
    'R100\told.json\t'+REQUEST,
    'A\t'+REQUEST+'\nM\t.github/workflows/sanftwecker-v27-release.yml',
  ])assert.throws(()=>validateDiff(source,after,diff,source));
  assert.throws(()=>validateDiff(after,source,'A\t'+REQUEST+'\n',source));
  assert.throws(()=>validateDiff(source,source,'A\t'+REQUEST+'\n',source));
});

test('GitHub owner and private original repo only',()=>{
  const good={GITHUB_REPOSITORY:REPO,GITHUB_ACTOR:'CobraChris',
    GITHUB_REF:'refs/heads/main',GITHUB_EVENT_NAME:'push'};
  validateActor(good);
  for(const patch of [
    {GITHUB_ACTOR:'github-actions[bot]'},
    {GITHUB_ACTOR:'someone-else'},
    {GITHUB_REPOSITORY:'CobraChris/app-fabrik-template'},
    {GITHUB_REF:'refs/heads/feature'},
    {GITHUB_EVENT_NAME:'workflow_dispatch'}
  ])assert.throws(()=>validateActor({...good,...patch}));
});

test('dual-device signed v27 release requires explicit future approval',()=>{
  const yml=readFileSync(new URL(
    '../.github/workflows/sanftwecker-v27-release.yml',import.meta.url),'utf8');
  const checks=[
    "branches: [main]", "paths: ['requests/sanftwecker-v27-release.json']",
    "github.actor == 'CobraChris'",
    "github.event.repository.fork == false",
    'persist-credentials: false',
    'environment: android-releases',
    'needs.validate.outputs.source_sha',
    'scripts/sanftwecker-v27-release-request.mjs',
    'tests/sanftwecker-v27-release-request.test.mjs',
    'sanftwecker-v26',
    'legacy/phone/sanftwecker-release.apk',
    'legacy/watch/sanftwecker-watch-v26.apk',
    'sanftwecker-watch-v27.apk',
    PHONE_V26_SHA256,WATCH_V26_SHA256,
    'cmp -s legacy/phone/cert.sha legacy/watch/cert.sha',
    'cmp -s signed/phone.cert.sha signed/watch.cert.sha',
    'cmp -s signed/phone.cert.sha legacy/phone/cert.sha',
    'cmp -s signed/watch.cert.sha legacy/watch/cert.sha',
    "versionCode='26'",
    "versionCode='26'",
    "versionCode='27'",
    'assert expected==27',
    'assert e[\'versionCode\']==27',
    'android.hardware.type.watch',
    'APP_SIGNING_KEYSTORE_BASE64',
    'APP_SIGNING_STORE_PASSWORD',
    'APP_SIGNING_KEY_ALIAS',
    'APP_SIGNING_KEY_PASSWORD',
    'gh release create sanftwecker-v27',
    'Signed phone and watch v26',
    '--prerelease'
  ];
  for(const token of checks)assert.ok(yml.includes(token),token);
  assert.equal(EXPECTED_CODE,27);
  assert.equal(EXPECTED_PACKAGE,'com.cobrachris.sanftwecker');
  assert.equal(PHONE_V26_SHA256.length,64);
  assert.equal(WATCH_V26_SHA256.length,64);
  assert.doesNotMatch(yml,/workflow_dispatch:|pull_request_target:|genkeypair/);
  assert.doesNotMatch(yml,/uses:\s*[^\s]+@v[0-9]+\s*$/m);
  assert.ok(yml.indexOf('Verify signed phone v26 and signed watch v26 before secrets')<
       yml.indexOf('Sign both original-certificate APKs'));
  assert.ok(yml.indexOf('Sign both original-certificate APKs')<
       yml.indexOf('gh release create sanftwecker-v27'));
  assert.ok(yml.indexOf('if gh release view sanftwecker-v27')<
       yml.indexOf('Sign both original-certificate APKs'));
});
