/**
 * Fail-closed, exact-owner release gate for signing with the EXISTING key.
 * This script never loads or writes any signing secret.
 */
import {readFileSync,appendFileSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

export const REPO='CobraChris/sanftwecker';
export const REQUEST='requests/sanftwecker-release.json';
export const OLD_REPO='CobraChris/app-fabrik-template';
export const VERSION_CODE=21;
export const OLD_BRIDGE_TAG='sanftwecker-v20';
export const OLD_BASE_TAG='sanftwecker-v19';
export const BASE_APK_SHA256='04ae0383d9faf1868a41ee1970c2038e5a52970e10e71b32c2832d15cf2f5f50';

export function validateReleaseRequest(value){
  if(!value||typeof value!=='object'||Array.isArray(value))
    throw new TypeError('Invalid signed release request');
  const keys=['confirmation','expected_version_code','intent','old_bridge_tag','request_id','schema','source_sha'];
  if(Object.keys(value).sort().join('|')!==keys.join('|'))
    throw new TypeError('Unexpected release request fields');
  if(value.schema!==1||value.intent!=='sanftwecker-original-signed-v21'||
      value.confirmation!=='REUSE_ORIGINAL_SIGNER_AND_REQUIRE_V20_BRIDGE'||
      value.old_bridge_tag!==OLD_BRIDGE_TAG||
      value.expected_version_code!==VERSION_CODE)
    throw new TypeError('Release identity, version or bridge confirmation mismatch');
  if(typeof value.request_id!=='string'||
      !/^\d{4}-\d{2}-\d{2}-[a-z0-9][a-z0-9-]{3,60}$/.test(value.request_id))
    throw new TypeError('Invalid immutable request id');
  if(typeof value.source_sha!=='string'||!/^[a-f0-9]{40}$/.test(value.source_sha))
    throw new TypeError('Invalid source SHA');
  return Object.freeze({source_sha:value.source_sha,version_code:String(VERSION_CODE)});
}
export function validateActor(env){
  if(env.GITHUB_REPOSITORY!==REPO||env.GITHUB_ACTOR!=='CobraChris'||
     env.GITHUB_REF!=='refs/heads/main'||env.GITHUB_EVENT_NAME!=='push')
    throw new Error('Untrusted signed release trigger');
}
export function validateExactApproval(before,after,diff,sourceSha){
  if(!/^[a-f0-9]{40}$/.test(before)||!/^[a-f0-9]{40}$/.test(after)||
     before===after||before!==sourceSha||
     typeof diff!=='string'||diff.trim()!=='A\t'+REQUEST)
    throw new Error('Only one NEW owner-approved request against exact previous main is allowed');
  return true;
}
function run(){
  validateActor(process.env);
  const request=validateReleaseRequest(JSON.parse(readFileSync(REQUEST,'utf8')));
  const before=process.env.REQUEST_BEFORE||'';
  const after=process.env.GITHUB_SHA||'';
  const diff=execFileSync('git',['diff','--name-status',before,after],{
    encoding:'utf8',timeout:10000,maxBuffer:2048
  });
  validateExactApproval(before,after,diff,request.source_sha);
  if(typeof process.env.GITHUB_OUTPUT!=='string'||!process.env.GITHUB_OUTPUT)
    throw new Error('Missing GitHub Actions outputs');
  appendFileSync(process.env.GITHUB_OUTPUT,
    'source_sha='+request.source_sha+'\nversion_code='+request.version_code+'\n');
  console.log('Approved exact-source v21 candidate; unchanged original signing identity required.');
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  try{run();}catch(e){
    console.error('Signed release blocked: '+(e?.message||'unknown error'));
    process.exitCode=1;
  }
}
