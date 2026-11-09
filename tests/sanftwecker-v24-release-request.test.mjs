import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {REPO,REQUEST,EXPECTED_CODE,PREVIOUS_SHA256,EXPECTED_PACKAGE,
  validateActor,validateDiff,validateRequest
} from '../scripts/sanftwecker-v24-release-request.mjs';
const base='a'.repeat(40),after='b'.repeat(40);
const valid={intent:'signed-phone-only-v24',expected_version_code:24,
  source_sha:base,request_id:'2026-11-09-phone-ringtones',
  confirmation:'SIGN_PHONE_V24_WITH_EXISTING_V23_CERTIFICATE'};

test('owner-approved immutable v24 identity is mandatory',()=>{
  assert.deepEqual(validateRequest(valid),{source_sha:base,expected_version_code:'24'});
  for(const bad of [
    {expected_version_code:23},{source_sha:'main'},
    {confirmation:'GENERATE_NEW_KEY'}, {intent:'signed-native-wear-v24'},
    {request_id:'../../unsafe'}, {watch_application_id:'com.cobrachris.sanftwecker'}
  ])assert.throws(()=>validateRequest({...valid,...bad}));
});
test('owner main push only, no bots, no workflow_dispatch',()=>{
  const ok={GITHUB_REPOSITORY:REPO,GITHUB_ACTOR:'CobraChris',
            GITHUB_REF:'refs/heads/main',GITHUB_EVENT_NAME:'push'};
  validateActor(ok);
  for(const bad of [{GITHUB_ACTOR:'github-actions[bot]'},
    {GITHUB_REF:'refs/heads/feature'}, {GITHUB_EVENT_NAME:'workflow_dispatch'},
    {GITHUB_REPOSITORY:'attacker/fork'}
  ])assert.throws(()=>validateActor({...ok,...bad}));
});
test('single added approval-file diff only; reviewed source pinned',()=>{
  validateDiff(base,after,'A\t'+REQUEST+'\n',base);
  for(const diff of ['M\t'+REQUEST,'A\tother.json',
    'A\t'+REQUEST+'\nM\talarm/build.gradle',
    'A\t'+REQUEST+'\nD\twatch/build.gradle'
  ])assert.throws(()=>validateDiff(base,after,diff,base));
  assert.throws(()=>validateDiff(base,base,'A\t'+REQUEST,base));
  assert.throws(()=>validateDiff(base,after,'A\t'+REQUEST,after));
});
test('signing workflow preserves previous production identity and avoids Wear rebuild',()=>{
  const yml=readFileSync(new URL('../.github/workflows/sanftwecker-v24-release.yml',
    import.meta.url),'utf8');
  for(const token of [
    'requests/sanftwecker-v24-release.json',
    "github.actor == 'CobraChris'",
    'persist-credentials: false',
    'environment: android-releases',
    PREVIOUS_SHA256,'sanftwecker-v23',
    'APP_SIGNING_KEYSTORE_BASE64',
    'cmp -s signed/phone.cert.sha legacy/cert.sha',
    "versionCode='24'","versionCode='23'",
    'signed/sanftwecker-release.apk',
    'gh release create sanftwecker-v24',
    'source_sha','needs.validate.outputs.source_sha'
  ])assert.ok(yml.includes(token),token);
  assert.equal(EXPECTED_CODE,24);
  assert.equal(EXPECTED_PACKAGE,'com.cobrachris.sanftwecker');
  assert.doesNotMatch(yml,/workflow_dispatch:|pull_request_target:|genkeypair/);
  assert.ok(yml.indexOf('Verify original v23 before secrets')<
    yml.indexOf('Sign original-certificate phone APK'));
  assert.doesNotMatch(yml,/signed\/sanftwecker-watch-v24\.apk/);
});
