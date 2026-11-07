#!/usr/bin/env bash
# Read-only migration baseline: validate legacy signed releases, without keys.
# Do NOT sign an APK, change the updater, or publish a release here.
set -euo pipefail
repo="CobraChris/app-fabrik-template"
apksigner="$ANDROID_HOME/build-tools/35.0.0/apksigner"
test -x "$apksigner" || { echo "Android apksigner 35.0.0 missing" >&2; exit 1; }
command -v gh >/dev/null || { echo "GitHub CLI missing" >&2; exit 1; }
command -v sha256sum >/dev/null
test -n "$GH_TOKEN" || { echo "Read-only GitHub API token missing" >&2; exit 1; }

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

verify_old_release() {
  local code="$1" expected="$2" archive="$work/v$1"
  mkdir -p "$archive"
  gh release download "sanftwecker-v$code" \
    --repo "$repo" --pattern 'sanftwecker-release.apk' --dir "$archive"
  test -s "$archive/sanftwecker-release.apk"
  # SHA-256 taken from GitHub's public, versioned release-asset metadata.
  printf '%s  %s\n' "$expected" "$archive/sanftwecker-release.apk" \
    | sha256sum --check --status
  "$apksigner" verify --min-sdk-version 30 --verbose \
    --print-certs "$archive/sanftwecker-release.apk" \
    > "$archive/verified-certificate.txt"
  local fingerprint
  fingerprint="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' \
    "$archive/verified-certificate.txt" | head -n1)"
  test -n "$fingerprint"
  printf '%s' "$fingerprint" > "$archive/fingerprint"
}

verify_old_release 18 'c0812ea9bb96520f9f5be1aa9daa1c4feffb3b1580be3aab7761fda0513cbaed'
verify_old_release 19 '04ae0383d9faf1868a41ee1970c2038e5a52970e10e71b32c2832d15cf2f5f50'
cmp -s "$work/v18/fingerprint" "$work/v19/fingerprint" || {
  echo "BLOCKED: certificate identity changed between legacy v18 and v19" >&2
  exit 1
}
echo "PASS: signed legacy v18 and v19 APK checksums and certificate identity match."
echo "This does NOT certify a new-repository signing key or an in-place device update."

# Ensure release build remains unsigned, update URL remains legacy, and
# package identity/version have not been reset by the repository migration.
python3 - <<'PY'
import json
from pathlib import Path
p=Path('alarm/build/outputs/apk/release/output-metadata.json')
data=json.loads(p.read_text(encoding='utf8'))
if data.get('applicationId') != 'com.cobrachris.sanftwecker':
    raise SystemExit('BLOCKED: Android package identity changed')
items=data.get('elements')
if not isinstance(items,list) or len(items)!=1:
    raise SystemExit('BLOCKED: unexpected Android build metadata')
e=items[0]
code=e.get('versionCode')
if not isinstance(code,int) or isinstance(code,bool) or code<19:
    raise SystemExit('BLOCKED: Android versionCode downgraded')
if e.get('outputFile')!='alarm-release-unsigned.apk':
    raise SystemExit('BLOCKED: build unexpectedly signed or artifact changed')
if not Path('alarm/build/outputs/apk/release/alarm-release-unsigned.apk').is_file():
    raise SystemExit('BLOCKED: expected unsigned APK missing')
source=Path('alarm/src/main/java/com/cobrachris/sanftwecker/UpdateUrlPolicy.java').read_text(encoding='utf8')
if 'https://github.com/CobraChris/app-fabrik-template/releases/download/' not in source:
    raise SystemExit('BLOCKED: updater URL was moved before cutover approval')
print('PASS: unsigned new-repo release has stable package, versionCode and legacy update URL.')
PY
