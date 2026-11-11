package com.cobrachris.watchvibrationpoc;

import org.junit.Test;
import static org.junit.Assert.*;

/** Security boundary of the optional self-updater, no network or Android device required. */
public final class WatchSelfUpdatePolicyTest {
    private static final String HASH="a".repeat(64);
    private static final String TAG="sanftwecker-v31";
    private static final String URL="https://github.com/CobraChris/sanftwecker/releases/download/"
            +TAG+"/sanftwecker-watch-v31.apk";

    @Test public void acceptsOnlyNewerOriginalWatchApk(){
        assertTrue(WatchSelfUpdatePolicy.approved(TAG,31,URL,"sha256:"+HASH,30));
        assertFalse(WatchSelfUpdatePolicy.approved(TAG,31,URL,"sha256:"+HASH,31));
        assertFalse(WatchSelfUpdatePolicy.approved(TAG,31,URL,"sha256:"+HASH,32));
        assertFalse(WatchSelfUpdatePolicy.approved("sanftwecker-v30",31,URL,"sha256:"+HASH,30));
        assertFalse(WatchSelfUpdatePolicy.approved(TAG,31,
                URL.replace("CobraChris","someone-else"),"sha256:"+HASH,30));
        assertFalse(WatchSelfUpdatePolicy.approved(TAG,31,
                URL.replace("watch-v31","watch-v30"),"sha256:"+HASH,30));
        assertFalse(WatchSelfUpdatePolicy.approved(TAG,31,URL,"sha256:bad",30));
    }
    @Test public void parsedTagsAndDigestsNeverGuess(){
        assertEquals(31,WatchSelfUpdatePolicy.tagVersion(TAG));
        for(String tag:new String[]{"v31","sanftwecker-v031","sanftwecker-v0",
                "sanftwecker-v31-alpha","sanftwecker-v31/other",""})
            assertEquals(-1,WatchSelfUpdatePolicy.tagVersion(tag));
        assertNull(WatchSelfUpdatePolicy.parseSha256(HASH));
        assertNull(WatchSelfUpdatePolicy.parseSha256("sha512:"+HASH));
        assertEquals(HASH,WatchSelfUpdatePolicy.parseSha256("sha256:"+HASH));
    }
    @Test public void redirectsAreHttpsAndTrustedHostsOnly(){
        assertTrue(WatchSelfUpdatePolicy.trustedHop(URL));
        assertTrue(WatchSelfUpdatePolicy.trustedHop(
                "https://release-assets.githubusercontent.com/foo?token=abc"));
        for(String hop:new String[]{"http://github.com/file",
                "https://github.com.evil.example/file",
                "https://evil.example/file",
                "https://user@github.com/file",
                "https://github.com:8080/file",
                "https://github.com/file#fragment",
                "file:///tmp/a.apk",""})
            assertFalse(hop,WatchSelfUpdatePolicy.trustedHop(hop));
    }
    @Test public void fileNamesAreBounded(){
        assertEquals("sanftwecker-watch-v31.apk",WatchSelfUpdatePolicy.filename(31));
        try{WatchSelfUpdatePolicy.filename(0);fail("zero");}
        catch(IllegalArgumentException expected){}
    }
}
