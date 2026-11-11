package com.cobrachris.watchvibrationpoc;

import java.net.URI;
import java.util.Locale;

/** Fail-closed release identity and redirect policy for user-triggered Watch self updates. */
public final class WatchSelfUpdatePolicy {
    private static final String ROOT=
            "https://github.com/CobraChris/sanftwecker/releases/download/";
    public static final String PACKAGE="com.cobrachris.sanftwecker";
    public static final long MAX_BYTES=30L*1024L*1024L;
    private WatchSelfUpdatePolicy(){}

    public static String filename(long code){
        if(code<1||code>2_100_000_000L)
            throw new IllegalArgumentException("Ungültige Version");
        return "sanftwecker-watch-v"+code+".apk";
    }
    public static long tagVersion(String tag){
        if(tag==null||!tag.matches("sanftwecker-v[1-9][0-9]{0,8}"))return -1;
        try{return Long.parseLong(tag.substring("sanftwecker-v".length()));}
        catch(NumberFormatException ignored){return -1;}
    }
    public static String parseSha256(String digest){
        if(digest==null||!digest.matches("sha256:[a-fA-F0-9]{64}"))return null;
        return digest.substring("sha256:".length()).toLowerCase(Locale.ROOT);
    }
    public static boolean approved(String tag,long code,String url,String digest,long installedCode){
        return code>installedCode && code>0 && code<=2_100_000_000L &&
                tagVersion(tag)==code && parseSha256(digest)!=null &&
                (ROOT+tag+"/"+filename(code)).equals(url);
    }
    public static boolean trustedHop(String url){
        if(url==null)return false;
        try{
            URI u=new URI(url);
            if(!"https".equalsIgnoreCase(u.getScheme())||
                    u.getRawUserInfo()!=null||u.getRawFragment()!=null||
                    (u.getPort()!=-1&&u.getPort()!=443))return false;
            String host=u.getHost();
            return "github.com".equalsIgnoreCase(host) ||
                    "release-assets.githubusercontent.com".equalsIgnoreCase(host);
        }catch(Exception ignored){return false;}
    }
}
