import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {REPO,REQUEST,EXPECTED_CODE,PHONE_V57_SHA256,WATCH_V57_SHA256,
    EXPECTED_PACKAGE,validateRequest,validateDiff,validateActor}
    from '../scripts/sanftwecker-v58-release-request.mjs';
const before='a'.repeat(40),after='b'.repeat(40);
const valid={
    confirmation:'SIGN_PHONE_AND_WATCH_V58_WITH_EXISTING_V57_CERTIFICATE',
    expected_version_code:58,intent:'signed-native-wear-v58',
    request_id:'2026-11-26-v58-alarm-editor-ux-approved',
    source_sha:before,watch_application_id:'com.cobrachris.sanftwecker'
};
test('only exact owner-v58 approval permits both original-signer APKs',()=>{
    assert.deepEqual(validateRequest(valid),{source_sha:before,expected_version_code:'58'});
    for(const patch of [
        {expected_version_code:57},{expected_version_code:59},{source_sha:'main'},
        {source_sha:'a'.repeat(41)},{watch_application_id:'invalid'},
        {confirmation:'SIGN_PHONE_AND_WATCH_V58_WITH_EXISTING_V53_CERTIFICATE'},
        {confirmation:'GENERATE_NEW_KEY'},{extra:'unexpected'},{request_id:'../bad'}
    ])assert.throws(()=>validateRequest({...valid,...patch}));
    for(const bad of [null,[],true,'bad'])assert.throws(()=>validateRequest(bad));
});
test('one added release request, no source modifications in approval commit',()=>{
    validateDiff(before,after,'A\t'+REQUEST+'\n',before);
    for(const diff of ['M\t'+REQUEST,'A\tother.json',
        'A\t'+REQUEST+'\nM\twatch/build.gradle',
        'A\t'+REQUEST+'\nA\tother.json'])
        assert.throws(()=>validateDiff(before,after,diff,before));
    assert.throws(()=>validateDiff(after,before,'A\t'+REQUEST,before));
});
test('only original owner may release by main push',()=>{
    const good={GITHUB_REPOSITORY:REPO,GITHUB_ACTOR:'CobraChris',
        GITHUB_REF:'refs/heads/main',GITHUB_EVENT_NAME:'push'};
    validateActor(good);
    for(const patch of [{GITHUB_ACTOR:'github-actions[bot]'},
        {GITHUB_REF:'refs/heads/feature'},{GITHUB_EVENT_NAME:'workflow_dispatch'},
        {GITHUB_REPOSITORY:'CobraChris/not-this-repo'}])
        assert.throws(()=>validateActor({...good,...patch}));
});
test('v58 protected release compares signed phone and Watch to exact v57 assets',()=>{
    const yml=readFileSync(new URL('../.github/workflows/sanftwecker-v58-release.yml',import.meta.url),'utf8');
    for(const token of [
        "branches: [main]","paths: ['requests/sanftwecker-v58-release.json']",
        "github.actor == 'CobraChris'","github.event.repository.fork == false",
        "persist-credentials: false","environment: android-releases",
        "needs.validate.outputs.source_sha",
        "scripts/sanftwecker-v58-release-request.mjs",
        "tests/sanftwecker-v58-release-request.test.mjs",
        "gh release download sanftwecker-v57",
        "legacy/phone/sanftwecker-release.apk",
        "legacy/watch/sanftwecker-watch-v57.apk",
        PHONE_V57_SHA256,WATCH_V57_SHA256,
        "cmp -s legacy/phone/cert.sha legacy/watch/cert.sha",
        "cmp -s signed/phone.cert.sha signed/watch.cert.sha",
        "cmp -s signed/phone.cert.sha legacy/phone/cert.sha",
        "cmp -s signed/watch.cert.sha legacy/watch/cert.sha",
        "versionCode='57'","versionCode='58'",
        "assert expected==58","assert e['versionCode']==58",
        "sanftwecker-watch-v58.apk","android.hardware.type.watch",
        "APP_SIGNING_KEYSTORE_BASE64","APP_SIGNING_STORE_PASSWORD",
        "APP_SIGNING_KEY_ALIAS","APP_SIGNING_KEY_PASSWORD",
        "gh release create sanftwecker-v58","--prerelease"])
        assert.ok(yml.includes(token),token);
    assert.ok(yml.indexOf('Verify signed phone v57 and signed watch v57 before secrets')<
        yml.indexOf('Sign both original-certificate APKs'));
    assert.ok(yml.indexOf('Sign both original-certificate APKs')<
        yml.indexOf('gh release create sanftwecker-v58'));
    assert.doesNotMatch(yml,/workflow_dispatch:|pull_request_target:|genkeypair/);
    assert.equal(EXPECTED_CODE,58);
    assert.equal(EXPECTED_PACKAGE,'com.cobrachris.sanftwecker');
    assert.equal(PHONE_V57_SHA256.length,64);
    assert.equal(WATCH_V57_SHA256.length,64);
});
test('phone and watch versionCode 58 preserve application IDs',()=>{
    for(const module of ['alarm','watch']){
        const gradle=readFileSync(new URL('../'+module+'/build.gradle',import.meta.url),'utf8');
        assert.match(gradle,/versionCode 58/);
        assert.match(gradle,/applicationId 'com.cobrachris.sanftwecker'/);
    }
});
