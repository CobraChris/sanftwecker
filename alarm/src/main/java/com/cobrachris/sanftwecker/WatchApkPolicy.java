package com.cobrachris.sanftwecker;

/** Restricts watch APK downloads to the matching original-signer release. */
public final class WatchApkPolicy {
    private static final String RELEASE_ROOT=
            "https://github.com/CobraChris/sanftwecker/releases/download/";
    private WatchApkPolicy(){}

    public static String filename(long code){
        if(code<=0||code>2_100_000_000L)
            throw new IllegalArgumentException("Ungültige Versionsnummer");
        return "sanftwecker-watch-v"+code+".apk";
    }

    public static boolean isApproved(String url,String tag,long expectedCode,
            String sha256){
        if(expectedCode<=0||expectedCode>2_100_000_000L)return false;
        if(sha256==null||!sha256.matches("[a-f0-9]{64}"))return false;
        if(!("sanftwecker-v"+expectedCode).equals(tag))return false;
        return (RELEASE_ROOT+tag+"/"+filename(expectedCode)).equals(url);
    }

    public static boolean isCompatibleWithPhone(long watchCode,long phoneCode){
        // Both modules use the same versionCode for a coordinated release.
        return phoneCode>0&&watchCode==phoneCode;
    }
}
