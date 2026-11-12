package com.cobrachris.watchvibrationpoc;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;

/**
 * Package versions, not UI callbacks, are the source of truth after an in-place
 * self-update: Android may kill the updating app before returning a result.
 */
public final class WatchSelfUpdateFeedback {
    private static final String PREFS="watch_self_update_feedback";
    private static final String REQUESTED="requested_version";
    private static final String REPLACED="replaced_version";
    private WatchSelfUpdateFeedback(){}

    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
    public static String installedVersion(Context c)throws Exception{
        PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),0);
        return info.versionName+" (v"+info.getLongVersionCode()+")";
    }
    public static void markRequested(Context c,long code){
        prefs(c).edit().putLong(REQUESTED,code).apply();
    }
    public static void clearRequest(Context c){
        prefs(c).edit().remove(REQUESTED).apply();
    }
    public static void markReplaced(Context c){
        try{
            long actual=WatchSelfUpdateClient.installedCode(c);
            prefs(c).edit().putLong(REPLACED,actual).commit();
        }catch(Exception ignored){
            // A subsequent foreground version check still works without a receipt.
        }
    }
    public static String consumeSuccess(Context c)throws Exception{
        long actual=WatchSelfUpdateClient.installedCode(c);
        long requested=prefs(c).getLong(REQUESTED,0);
        long replaced=prefs(c).getLong(REPLACED,0);
        if(actual>0&&(actual==requested||actual==replaced)){
            prefs(c).edit().remove(REQUESTED).remove(REPLACED).apply();
            return "Update erfolgreich installiert: "+installedVersion(c)+".";
        }
        return null;
    }
}