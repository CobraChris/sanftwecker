/**
 * Owner-controlled one-time phone-only v24 release authorization. This script has
 * no access to signing material; a protected GitHub Environment job handles signing.
 */
import {readFileSync,appendFileSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
export const REPO='CobraChris/sanftwecker';
export const REQUEST='requests/sanftwecker-v24-release.json';
export const EXPECTED_CODE=24;
export const PREVIOUS_SHA256='213a3488e11326f17c1e52fc7da1853ab1db87dad3fe83d1be3d2824477e586f';
export const EXPECTED_PACKAGE='com.cobrachris.sanftwecker';
export function validateActor(e){
    if(e.GITHUB_REPOSITORY!==REPO||e.GITHUB_ACTOR!=='CobraChris'||
       e.GITHUB_REF!=='refs/heads/main'||e.GITHUB_EVENT_NAME!=='push')
        throw Error('Owner-only main push required; never sign from a PR or dispatch');
}
export function validateRequest(o){
    const keys=['confirmation','expected_version_code','intent','request_id','source_sha'];
    if(o===null||typeof o!=='object'||Array.isArray(o)||
       Object.keys(o).sort().join('|')!==keys.join('|'))
        throw Error('Unexpected v24 release approval shape');
    if(o.intent!=='signed-phone-only-v24'||o.expected_version_code!==24||
       o.confirmation!=='SIGN_PHONE_V24_WITH_EXISTING_V23_CERTIFICATE')
        throw Error('No original-certificate phone v24 owner authorization');
    if(typeof o.request_id!=='string'||!
       /^\d{4}-\d{2}-\d{2}-[a-z0-9][a-z0-9-]{3,60}$/.test(o.request_id))
        throw Error('Bad one-time request identifier');
    if(typeof o.source_sha!=='string'||!/^[0-9a-f]{40}$/.test(o.source_sha))
        throw Error('Approval must pin the immutable previously reviewed source SHA');
    return Object.freeze({source_sha:o.source_sha,expected_version_code:'24'});
}
export function validateDiff(before,after,diff,approvedSource){
    if(before!==approvedSource||before===after||
       !/^[0-9a-f]{40}$/.test(before)||!/^[0-9a-f]{40}$/.test(after)||
       typeof diff!=='string'||diff.trim()!=='A\t'+REQUEST)
        throw Error('Only a single added owner approval file can trigger this release');
}
export function run(){
    validateActor(process.env);
    const request=validateRequest(JSON.parse(readFileSync(REQUEST,'utf8')));
    const before=process.env.REQUEST_BEFORE,after=process.env.GITHUB_SHA;
    const diff=execFileSync('git',['diff','--name-status',before,after],
        {encoding:'utf8',timeout:10000,maxBuffer:2048});
    validateDiff(before,after,diff,request.source_sha);
    if(!process.env.GITHUB_OUTPUT)throw Error('Missing GitHub Actions output');
    appendFileSync(process.env.GITHUB_OUTPUT,
        'source_sha='+request.source_sha+'\nversion_code='+request.expected_version_code+'\n');
    console.log('Owner-approved original-signer v24 source passed release request validation');
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
    try{run();}catch(error){console.error('RELEASE BLOCKED: '+error.message);process.exitCode=1;}
}
