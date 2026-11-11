package com.cobrachris.watchvibrationpoc;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Parse real GitHub metadata without network or an Android watch. */
public final class WatchSelfUpdateClientTest {
    private static final String HASH="a".repeat(64);

    private static JSONObject release(int code,boolean draft,String owner,boolean valid)
            throws org.json.JSONException {
        String tag="sanftwecker-v"+code;
        JSONObject asset=new JSONObject();
        asset.put("name","sanftwecker-watch-v"+code+".apk");
        asset.put("digest",valid?"sha256:"+HASH:"sha256:bad");
        asset.put("browser_download_url","https://github.com/"+owner+
                "/sanftwecker/releases/download/"+tag+"/sanftwecker-watch-v"+code+".apk");
        JSONObject rel=new JSONObject();
        rel.put("tag_name",tag);
        rel.put("draft",draft);
        rel.put("assets",new JSONArray().put(asset));
        return rel;
    }
    @Test public void picksLatestReleasedMatchingWatchOnly() throws Exception{
        JSONArray raw=new JSONArray()
                .put(release(30,false,"CobraChris",true))
                .put(release(33,true,"CobraChris",true))
                .put(release(32,false,"someone-else",true))
                .put(release(34,false,"CobraChris",false))
                .put(release(31,false,"CobraChris",true));
        WatchSelfUpdateClient.Candidate found=WatchSelfUpdateClient.find(raw.toString(),30);
        assertNotNull(found);
        assertEquals(31,found.code);
        assertEquals("sanftwecker-v31",found.tag);
        assertEquals(HASH,found.sha256);
    }
    @Test public void refusesWhenNoNewSignedPackageExists()throws Exception{
        JSONArray raw=new JSONArray().put(release(31,false,"CobraChris",true));
        assertNull(WatchSelfUpdateClient.find(raw.toString(),31));
        assertNull(WatchSelfUpdateClient.find(new JSONArray()
                .put(release(33,true,"CobraChris",true)).toString(),30));
        assertNull(WatchSelfUpdateClient.find(new JSONArray()
                .put(release(33,false,"CobraChris",false)).toString(),30));
    }
}
