package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WatchUpdateActivityTest {
    private static final String URL=
            "https://github.com/CobraChris/sanftwecker/releases/download/"+
            "sanftwecker-v25/sanftwecker-watch-v25.apk";
    private static final String HASH=
            "60dbadb976acfa51d1b3d500e96354f736bec031ede9c830384441d63fd91b1d";

    @Test public void onlyExactSignedWatchAssetForInstalledPhoneIsSelectable(){
        String release="[{"+
                "\"draft\":false,\"tag_name\":\"sanftwecker-v25\",\"assets\":["+
                "{\"name\":\"sanftwecker-release.apk\",\"browser_download_url\":\""+
                URL+"\",\"digest\":\"sha256:"+HASH+"\"},"+
                "{\"name\":\"sanftwecker-watch-v25.apk\",\"browser_download_url\":\""+
                URL+"\",\"digest\":\"sha256:"+HASH+"\"}]}]";
        WatchUpdateActivity.Candidate found=
                WatchUpdateActivity.findMatchingWatchAsset(release,25);
        assertNotNull(found);
        assertEquals(25,found.code);
        assertEquals(URL,found.url);
        assertEquals(HASH,found.sha256);
        assertEquals("sanftwecker-v25",found.tag);
        assertNull(WatchUpdateActivity.findMatchingWatchAsset(release,24));
        assertNull(WatchUpdateActivity.findMatchingWatchAsset(release,26));
    }
    @Test public void rejectingDraftUnknownOriginOrBadDigest(){
        for(String json:new String[]{
            "[{\"draft\":true,\"tag_name\":\"sanftwecker-v25\",\"assets\":["+
                    "{\"name\":\"sanftwecker-watch-v25.apk\",\"browser_download_url\":\""+
                    URL+"\",\"digest\":\"sha256:"+HASH+"\"}]}]",
            "[{\"draft\":false,\"tag_name\":\"sanftwecker-v25\",\"assets\":["+
                    "{\"name\":\"sanftwecker-watch-v25.apk\",\"browser_download_url\":\""+
                    "https://malicious.example/watch.apk\",\"digest\":\"sha256:"+HASH+"\"}]}]",
            "[{\"draft\":false,\"tag_name\":\"sanftwecker-v25\",\"assets\":["+
                    "{\"name\":\"sanftwecker-watch-v25.apk\",\"browser_download_url\":\""+
                    URL+"\",\"digest\":\"sha256:garbage\"}]}]",
            "[{\"draft\":false,\"tag_name\":\"sanftwecker-v25\",\"assets\":["+
                    "{\"name\":\"sanftwecker-watch-v25.apk\",\"browser_download_url\":\""+
                    URL+"\",\"digest\":\"\"}]}]",
            "malformed json","[]"
        })assertNull(json,WatchUpdateActivity.findMatchingWatchAsset(json,25));
    }
}
