/**
 * Owner-controlled, one-time v43 release gate for identical original phone and watch signatures.
 * Never reads signing keys; a separate protected job does that after APK lineage checks.
 */
import {readFileSync,appendFileSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
export const REPO='CobraChris/sanftwecker';
export const REQUEST='requests/sanftwecker-v43-release.json';
export const EXPECTED_CODE=43;
export const PHONE_V42_SHA256='3ab210f7ffde95734c36f4b01aacc77d6d7d5d5843f706e7634ffeff10fa22f4';
export const WATCH_V42_SHA256='9e13fb47b350d183316ae7713ff06ee5d3eaa8d9fbb5b3b36bb32af904c29ccd';
export const EXPECTED_PACKAGE='com.cobrachris.sanftwecker';
export function validateActor(e){
  if(e.GITHUB_REPOSITORY!==REPO||e.GITHUB_ACTOR!=='CobraChris'||
     e.GITHUB_REF!=='refs/heads/main'||e.GITHUB_EVENT_NAME!=='push')
    throw Error('Owner-only main push is required for a signed v43 release');
}
export function validateRequest(o){
  const keys=['confirmation','expected_version_code','intent','request_id',
    'source_sha','watch_application_id'];
  if(o===null||typeof o!=='object'||Array.isArray(o)||
     Object.keys(o).sort().join('|')!==keys.join('|'))
    throw Error('Unexpected release request fields');
  if(o.intent!=='signed-native-wear-v43'||o.expected_version_code!==43||
     o.watch_application_id!==EXPECTED_PACKAGE||
     o.confirmation!=='SIGN_PHONE_AND_WATCH_V43_WITH_EXISTING_V42_CERTIFICATE')
    throw Error('v43 identity or original signer approval mismatch');
  if(typeof o.request_id!=='string'||
     !/^\d{4}-\d{2}-\d{2}-[a-z0-9][a-z0-9-]{3,60}$/.test(o.request_id))
    throw Error('Bad immutable request identifier');
  if(typeof o.source_sha!=='string'||!/^[0-9a-f]{40}$/.test(o.source_sha))
    throw Error('Expected exact immutable pre-approval source SHA');
  return Object.freeze({source_sha:o.source_sha,expected_version_code:'43'});
}
export function validateDiff(before,after,diff,approvedSource){
  if(before!==approvedSource||before===after||
     !/^[a-f0-9]{40}$/.test(before)||!/^[a-f0-9]{40}$/.test(after)||
     typeof diff!=='string'||diff.trim()!=='A\t'+REQUEST)
    throw Error('Exactly one added approval file against pre-release main required');
}
export function run(){
  validateActor(process.env);
  const req=validateRequest(JSON.parse(readFileSync(REQUEST,'utf8')));
  const before=process.env.REQUEST_BEFORE;
  const after=process.env.GITHUB_SHA;
  const changed=execFileSync('git',['diff','--name-status',before,after],
    {encoding:'utf8',timeout:10000,maxBuffer:2048});
  validateDiff(before,after,changed,req.source_sha);
  if(!process.env.GITHUB_OUTPUT)throw Error('Missing GitHub Actions output');
  appendFileSync(process.env.GITHUB_OUTPUT,
    'source_sha='+req.source_sha+'\nversion_code='+req.expected_version_code+'\n');
  console.log('One-time original-key phone + watch release preflight approved.');
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  try{run();}catch(e){console.error('RELEASE BLOCKED: '+e.message);process.exitCode=1;}
}