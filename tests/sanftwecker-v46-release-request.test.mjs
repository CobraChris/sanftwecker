import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {
  REPO,REQUEST,EXPECTED_CODE,PHONE_V45_SHA256,WATCH_V45_SHA256,
  EXPECTED_PACKAGE,validateRequest,validateDiff,validateActor
} from '../scripts/sanftwecker-v46-release-request.mjs';

const source='a'.repeat(40),after='b'.repeat(40);
const valid={
  intent:'signed-native-wear-v46',
  request_id:'2026-11-17-visible-watch-alarm-notices',
  expected_version_code:46,source_sha:source,
  watch_application_id:'com.cobrachris.sanftwecker',
  confirmation:'SIGN_PHONE_AND_WATCH_V46_WITH_EXISTING_V45_CERTIFICATE'
};

test('v46 approval must explicitly authorize both APKs with original signers',()=>{
  assert.deepEqual(validateRequest(valid),
    {source_sha:source,expected_version_code:'46'});
  for(const update of [
    {expected_version_code:44},{expected_version_code:46},{source_sha:'main'},
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
    'A\t'+REQUEST+'\nM\t.github/workflows/sanftwecker-v46-release.yml',
  ])assert.throws(()=>validateDiff(source,after,diff,source));
  assert.throws(()=>validateDiff(after,source,'A\t'+REQUEST+'\n',source));
  assert.throws(()=>validateDiff(source,source,'A\t'+REQUEST+'\n',source));
});

test('GitHub owner and original repo only',()=>{
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

test('dual-device signed v46 visible Watch alarm notifications requires exact owner approval',()=>{
  const yml=readFileSync(new URL(
    '../.github/workflows/sanftwecker-v46-release.yml',import.meta.url),'utf8');
  const checks=[
    "branches: [main]", "paths: ['requests/sanftwecker-v46-release.json']",
    "github.actor == 'CobraChris'",
    "github.event.repository.fork == false",
    'persist-credentials: false',
    'environment: android-releases',
    'needs.validate.outputs.source_sha',
    'scripts/sanftwecker-v46-release-request.mjs',
    'tests/sanftwecker-v46-release-request.test.mjs',
    'sanftwecker-v45',
    'legacy/phone/sanftwecker-release.apk',
    'legacy/watch/sanftwecker-watch-v45.apk',
    'sanftwecker-watch-v46.apk',
    PHONE_V45_SHA256,WATCH_V45_SHA256,
    'cmp -s legacy/phone/cert.sha legacy/watch/cert.sha',
    'cmp -s signed/phone.cert.sha signed/watch.cert.sha',
    'cmp -s signed/phone.cert.sha legacy/phone/cert.sha',
    'cmp -s signed/watch.cert.sha legacy/watch/cert.sha',
    "versionCode='46'",
    "versionCode='46'",
    "versionCode='46'",
    'assert expected==46',
    'assert e[\'versionCode\']==45',
    'android.hardware.type.watch',
    'APP_SIGNING_KEYSTORE_BASE64',
    'APP_SIGNING_STORE_PASSWORD',
    'APP_SIGNING_KEY_ALIAS',
    'APP_SIGNING_KEY_PASSWORD',
    'gh release create sanftwecker-v46',
    'Signed phone and watch v45',
    '--prerelease'
  ];
  for(const token of checks)assert.ok(yml.includes(token),token);
  assert.equal(EXPECTED_CODE,46);
  assert.equal(EXPECTED_PACKAGE,'com.cobrachris.sanftwecker');
  assert.equal(PHONE_V45_SHA256.length,64);
  assert.equal(WATCH_V45_SHA256.length,64);
  assert.doesNotMatch(yml,/workflow_dispatch:|pull_request_target:|genkeypair/);
  assert.doesNotMatch(yml,/uses:\s*[^\s]+@v[0-9]+\s*$/m);
  assert.ok(yml.indexOf('Verify signed phone v45 and signed watch v45 before secrets')<
       yml.indexOf('Sign both original-certificate APKs'));
  assert.ok(yml.indexOf('Sign both original-certificate APKs')<
       yml.indexOf('gh release create sanftwecker-v46'));
  assert.ok(yml.indexOf('if gh release view sanftwecker-v46')<
       yml.indexOf('Sign both original-certificate APKs'));
});
