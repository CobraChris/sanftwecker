import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {
    REPO,REQUEST,EXPECTED_CODE,PHONE_V51_SHA256,WATCH_V51_SHA256,
    EXPECTED_PACKAGE,validateRequest,validateDiff,validateActor
} from '../scripts/sanftwecker-v52-release-request.mjs';

const source='a'.repeat(40),after='b'.repeat(40);
const valid={
    intent:'signed-native-wear-v52',
    request_id:'2026-11-23-v52-owner-approved-dual-device',
    expected_version_code:52,
    source_sha:source,
    watch_application_id:'com.cobrachris.sanftwecker',
    confirmation:'SIGN_PHONE_AND_WATCH_V52_WITH_EXISTING_V51_CERTIFICATE'
};

test('only exact v52 owner approval for both APKs with existing v51 signing certificate',()=>{
    assert.deepEqual(validateRequest(valid),{source_sha:source,expected_version_code:'52'});
    for(const modification of [
        {expected_version_code:51},{expected_version_code:53},
        {source_sha:'main'},{source_sha:'a'.repeat(41)},
        {watch_application_id:'com.cobrachris.watchvibrationpoc'},
        {confirmation:'GENERATE_NEW_KEY'},
        {confirmation:'SIGN_PHONE_ONLY'},
        {confirmation:'SIGN_PHONE_AND_WATCH_V52_WITH_EXISTING_V50_CERTIFICATE'},
        {request_id:'../unsafe'}, {extra:'unexpected'}
    ])assert.throws(()=>validateRequest({...valid,...modification}));
    for(const input of [null,[],true,'bad'])assert.throws(()=>validateRequest(input));
    assert.equal(EXPECTED_CODE,52);
    assert.equal(EXPECTED_PACKAGE,'com.cobrachris.sanftwecker');
});

test('only one newly added immutable approval file against exact source',()=>{
    validateDiff(source,after,'A\t'+REQUEST+'\n',source);
    for(const diff of [
        'M\t'+REQUEST,'A\tother.json',
        'A\t'+REQUEST+'\nM\twatch/build.gradle',
        'A\t'+REQUEST+'\nA\tother.json',
        'D\t'+REQUEST,
    ])assert.throws(()=>validateDiff(source,after,diff,source));
    assert.throws(()=>validateDiff(after,source,'A\t'+REQUEST,source));
    assert.throws(()=>validateDiff(source,source,'A\t'+REQUEST,source));
});

test('only CobraChris main push can release, no automated bypass',()=>{
    const good={GITHUB_REPOSITORY:REPO,GITHUB_ACTOR:'CobraChris',
        GITHUB_REF:'refs/heads/main',GITHUB_EVENT_NAME:'push'};
    validateActor(good);
    for(const modification of [
        {GITHUB_ACTOR:'github-actions[bot]'},{GITHUB_ACTOR:'someone-else'},
        {GITHUB_REF:'refs/heads/feature'},
        {GITHUB_EVENT_NAME:'workflow_dispatch'},
        {GITHUB_REPOSITORY:'CobraChris/app-fabrik-template'}
    ])assert.throws(()=>validateActor({...good,...modification}));
});

test('protected v52 release checks original v51 both devices before sign and publish',()=>{
    const yml=readFileSync(new URL(
        '../.github/workflows/sanftwecker-v52-release.yml',import.meta.url),'utf8');
    for(const label of [
        "branches: [main]", "paths: ['requests/sanftwecker-v52-release.json']",
        "github.actor == 'CobraChris'",
        "github.event.repository.fork == false",
        "persist-credentials: false",
        "environment: android-releases",
        "needs.validate.outputs.source_sha",
        "scripts/sanftwecker-v52-release-request.mjs",
        "tests/sanftwecker-v52-release-request.test.mjs",
        "gh release download sanftwecker-v51",
        "legacy/phone/sanftwecker-release.apk",
        "legacy/watch/sanftwecker-watch-v51.apk",
        PHONE_V51_SHA256,WATCH_V51_SHA256,
        "cmp -s legacy/phone/cert.sha legacy/watch/cert.sha",
        "cmp -s signed/phone.cert.sha signed/watch.cert.sha",
        "cmp -s signed/phone.cert.sha legacy/phone/cert.sha",
        "cmp -s signed/watch.cert.sha legacy/watch/cert.sha",
        "versionCode='51'","versionCode='52'",
        "assert expected==52","assert e['versionCode']==52",
        "sanftwecker-watch-v52.apk","android.hardware.type.watch",
        "APP_SIGNING_KEYSTORE_BASE64","APP_SIGNING_STORE_PASSWORD",
        "APP_SIGNING_KEY_ALIAS","APP_SIGNING_KEY_PASSWORD",
        "gh release create sanftwecker-v52","--prerelease"
    ])assert.ok(yml.includes(label),label);
    assert.match(yml,/Sign both original-certificate APKs/);
    assert.ok(yml.indexOf('Verify signed phone v51 and signed watch v51 before secrets')<
        yml.indexOf('Sign both original-certificate APKs'));
    assert.ok(yml.indexOf('Sign both original-certificate APKs')<
        yml.indexOf('gh release create sanftwecker-v52'));
    assert.ok(yml.indexOf('if gh release view sanftwecker-v52')<
        yml.indexOf('Sign both original-certificate APKs'));
    assert.doesNotMatch(yml,/workflow_dispatch:|pull_request_target:|genkeypair/);
    assert.doesNotMatch(yml,/uses:\s*[^\s]+@v[0-9]+\s*$/m);
    assert.equal(PHONE_V51_SHA256.length,64);
    assert.equal(WATCH_V51_SHA256.length,64);
});

test('both build modules are upgraded in lockstep without changing the application ID',()=>{
    for(const module of ['alarm','watch']){
        const gradle=readFileSync(new URL('../'+module+'/build.gradle',import.meta.url),'utf8');
        // This checks compatibility with the v52 release, not a frozen current build.
        const version=gradle.match(/versionCode ([0-9]+)/);
        assert.ok(version && Number(version[1])>=52);
        assert.match(gradle,/applicationId 'com.cobrachris.sanftwecker'/);
    }
});
