package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WatchApkPolicyTest {
    private static final String ROOT=
            "https://github.com/CobraChris/sanftwecker/releases/download/";
    private static final String HASH=
            "60dbadb976acfa51d1b3d500e96354f736bec031ede9c830384441d63fd91b1d";

    @Test public void watchApkMustHaveExactTagFilenameAndRepo(){
        assertTrue(WatchApkPolicy.isApproved(
                ROOT+"sanftwecker-v25/sanftwecker-watch-v25.apk",
                "sanftwecker-v25",25,HASH));
        for(String url:new String[]{
                ROOT+"sanftwecker-v25/sanftwecker-release.apk",
                ROOT+"sanftwecker-v24/sanftwecker-watch-v25.apk",
                ROOT+"sanftwecker-v25/sanftwecker-watch-v25.apk?x=1",
                ROOT+"sanftwecker-v25/../sanftwecker-watch-v25.apk",
                "http://github.com/CobraChris/sanftwecker/releases/download/"+
                        "sanftwecker-v25/sanftwecker-watch-v25.apk",
                "https://evil.example/sanftwecker-watch-v25.apk",
                "https://github.com/CobraChris/other/releases/download/"+
                        "sanftwecker-v25/sanftwecker-watch-v25.apk"
        })assertFalse(url,WatchApkPolicy.isApproved(
                url,"sanftwecker-v25",25,HASH));
        assertFalse(WatchApkPolicy.isApproved(
                ROOT+"sanftwecker-v25/sanftwecker-watch-v25.apk",
                "sanftwecker-v26",25,HASH));
        assertFalse(WatchApkPolicy.isApproved(
                ROOT+"sanftwecker-v25/sanftwecker-watch-v25.apk",
                "sanftwecker-v25",25,null));
        assertFalse(WatchApkPolicy.isApproved(
                ROOT+"sanftwecker-v25/sanftwecker-watch-v25.apk",
                "sanftwecker-v25",25,"fakedeface"));
    }
    @Test public void downloadOnlyMatchingPhoneBuildNotIncompatibleWatch(){
        assertTrue(WatchApkPolicy.isCompatibleWithPhone(25,25));
        assertFalse(WatchApkPolicy.isCompatibleWithPhone(25,24));
        assertFalse(WatchApkPolicy.isCompatibleWithPhone(26,25));
        assertFalse(WatchApkPolicy.isCompatibleWithPhone(0,25));
        assertEquals("sanftwecker-watch-v25.apk",WatchApkPolicy.filename(25));
        try{WatchApkPolicy.filename(-2);fail();}
        catch(IllegalArgumentException expected){}
        try{WatchApkPolicy.filename(2_100_000_001L);fail();}
        catch(IllegalArgumentException expected){}
    }
}
