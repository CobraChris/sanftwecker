package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class VerifiedUpdatePolicyTest {
    private static final String URL=
            "https://github.com/CobraChris/sanftwecker/releases/download/"+
            "sanftwecker-v22/sanftwecker-release.apk";
    private static final String DIGEST=
            "17b8097a231e088b33b99f7a624405c0ce9cae705ab721fbd5c77022e7b8df53";
    @Test public void digestMustComeFromActualGitHubAssetMetadata(){
        assertEquals(DIGEST,VerifiedUpdatePolicy.parseSha256("sha256:"+DIGEST));
        assertEquals(DIGEST,VerifiedUpdatePolicy.parseSha256(
                "sha256:"+DIGEST.toUpperCase(java.util.Locale.ROOT)));
        for(String invalid:new String[]{
                null, "", DIGEST, "sha1:"+DIGEST, "SHA256:"+DIGEST,
                "sha256:abc", "sha256:"+DIGEST+"00",
                "sha256:"+DIGEST+"\r\nmalicious"
        })assertNull(VerifiedUpdatePolicy.parseSha256(invalid));
    }
    @Test public void onlyHttpsGitHubAndOfficialReleaseAssetRedirects(){
        assertTrue(VerifiedUpdatePolicy.isTrustedDownloadHop(URL));
        assertTrue(VerifiedUpdatePolicy.isTrustedDownloadHop(
                "https://release-assets.githubusercontent.com/github-production-release-asset-2e65be/1/2?token=opaque"));
        assertTrue(VerifiedUpdatePolicy.isTrustedDownloadHop("https://github.com:443/"));
        for(String bad:new String[]{
                null,"http://github.com/download",
                "https://github.com.evil.example/app.apk",
                "https://release-assets.githubusercontent.com.evil.example/app.apk",
                "https://attacker.github.com/release",
                "https://github.com@evil.example/release",
                "https://user:pass@github.com/release",
                "https://github.com:8080/release",
                "https://github.com/release#fragment",
                "file:///data/local/tmp/evil.apk",
                "https://raw.githubusercontent.com/user/apk"
        })assertFalse("Rejected hop: "+bad,VerifiedUpdatePolicy.isTrustedDownloadHop(bad));
    }
    @Test public void onlyHigherVersionFromPinnedRepoWithValidDigest(){
        assertTrue(VerifiedUpdatePolicy.isNewerApprovedUpdate(URL,DIGEST,22,21));
        assertFalse(VerifiedUpdatePolicy.isNewerApprovedUpdate(URL,DIGEST,21,21));
        assertFalse(VerifiedUpdatePolicy.isNewerApprovedUpdate(URL,DIGEST,20,21));
        assertFalse(VerifiedUpdatePolicy.isNewerApprovedUpdate(URL,"x",22,21));
        assertFalse(VerifiedUpdatePolicy.isNewerApprovedUpdate(
                "https://example.com/sanftwecker-v22/sanftwecker-release.apk",DIGEST,22,21));
        assertFalse(VerifiedUpdatePolicy.isNewerApprovedUpdate(
                URL+"?redirect=1",DIGEST,22,21));
    }
}
