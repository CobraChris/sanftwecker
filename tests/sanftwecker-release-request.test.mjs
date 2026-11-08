import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {
  REPO,REQUEST,OLD_REPO,VERSION_CODE,OLD_BRIDGE_TAG,OLD_BASE_TAG,BASE_APK_SHA256,
  validateActor,validateReleaseRequest,validateExactApproval
} from '../scripts/sanftwecker-release-request.mjs';

const sha='a'.repeat(40),after='b'.repeat(40);
const fixture={schema:1,intent:'sanftwecker-original-signed-v21',
  request_id:'2026-11-08-sanftwecker-v21',expected_version_code:21,
  source_sha:sha,old_bridge_tag:'sanftwecker-v20',
  confirmation:'REUSE_ORIGINAL_SIGNER_AND_REQUIRE_V20_BRIDGE'};
test('v21 request locks old v20 bridge and original signer',()=>{
  assert.deepEqual(validateReleaseRequest(fixture),{
    source_sha:sha,version_code:'21'
  });
  for(const bad of [
    {schema:2},{intent:'sign-arbitrary-app'},{request_id:'../escape'},
    {expected_version_code:20},{expected_version_code:22},
    {source_sha:'main'},{old_bridge_tag:'sanftwecker-v19'},
    {confirmation:'NEW_SIGNING_KEY'},{unexpected:'extra'}
  ])assert.throws(()=>validateReleaseRequest({...fixture,...bad}));
});
test('signed release runs only for CobraChris main push',()=>{
  const ok={GITHUB_REPOSITORY:REPO,GITHUB_ACTOR:'CobraChris',
    GITHUB_REF:'refs/heads/main',GITHUB_EVENT_NAME:'push'};
  validateActor(ok);
  for(const change of [
    {GITHUB_ACTOR:'github-actions[bot]'},{GITHUB_EVENT_NAME:'pull_request'},
    {GITHUB_REPOSITORY:OLD_REPO},{GITHUB_REF:'refs/heads/feature'}
  ])assert.throws(()=>validateActor({...ok,...change}));
});
test('only brand-new single file on exact approved previous main is accepted',()=>{
  assert.equal(validateExactApproval(sha,after,'A\t'+REQUEST+'\n',sha),true);
  for(const row of [
    'M\t'+REQUEST,'D\t'+REQUEST,'A\trequests/other.json',
    'A\t'+REQUEST+'\nM\talarm/build.gradle',''
  ])assert.throws(()=>validateExactApproval(sha,after,row,sha));
  assert.throws(()=>validateExactApproval(sha,after,'A\t'+REQUEST,after));
  assert.throws(()=>validateExactApproval(sha,sha,'A\t'+REQUEST,sha));
});
test('workflow requires verified old v20 bridge and pins v19 signer digest',()=>{
  assert.equal(VERSION_CODE,21);
  assert.equal(OLD_BRIDGE_TAG,'sanftwecker-v20');
  assert.equal(OLD_BASE_TAG,'sanftwecker-v19');
  assert.equal(BASE_APK_SHA256.length,64);
  const flow=readFileSync(new URL('../.github/workflows/sanftwecker-release-candidate.yml',import.meta.url),'utf8');
  for(const must of [
    "paths: ['requests/sanftwecker-release.json']",
    "github.actor == 'CobraChris'",
    'environment: android-releases',
    'persist-credentials: false',
    'node scripts/sanftwecker-release-request.mjs',
    'sanftwecker-v19',
    'sanftwecker-v20',
    BASE_APK_SHA256,
    '0c4e56c55b04d720768f60c3c8b35cad212212df470bdfdeb265715b747909eb',
    'legacy/v20/sanftwecker-release.apk | sha256sum --check',
    'gh release download "$TAG"',
    'apksigner',
    'APP_SIGNING_KEYSTORE_BASE64'
  ])assert.ok(flow.includes(must),must);
  assert.ok(flow.indexOf('Verify both legacy APKs before secrets')<
    flow.indexOf('Read original signing key only for signature'));
  assert.doesNotMatch(flow,/workflow_dispatch:|pull_request_target:|git push|genkeypair/);
  assert.doesNotMatch(flow,/uses:\s*[^\s]+@v[0-9]+\s*$/m);
  const baseline=readFileSync(new URL('../scripts/verify-migration-baseline.sh',import.meta.url),'utf8');
  for(const release of ['verify_old_release 18','verify_old_release 19','verify_old_release 20'])
    assert.ok(baseline.includes(release),release);
  assert.ok(baseline.includes('cmp -s "$work/v19/fingerprint" "$work/v20/fingerprint"'));
  assert.ok(baseline.includes('0c4e56c55b04d720768f60c3c8b35cad212212df470bdfdeb265715b747909eb'));
  assert.ok(baseline.includes('versionCode='));
});
