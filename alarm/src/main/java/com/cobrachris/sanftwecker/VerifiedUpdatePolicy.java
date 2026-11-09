package com.cobrachris.sanftwecker;

import java.net.URI;
import java.util.Locale;

/** Fail-closed metadata and redirect policy for the optional in-app installer. */
public final class VerifiedUpdatePolicy {
    private VerifiedUpdatePolicy(){}

    /** GitHub asset digests are SHA-256 hex, not hashes from a guessed URL. */
    public static String parseSha256(String digest) {
        if(digest==null || !digest.matches("sha256:[a-fA-F0-9]{64}"))return null;
        return digest.substring(7).toLowerCase(Locale.ROOT);
    }

    /** Redirects may only stay on GitHub or its dedicated release-asset host. */
    public static boolean isTrustedDownloadHop(String url) {
        if(url==null)return false;
        try {
            URI uri=new URI(url);
            if(!"https".equalsIgnoreCase(uri.getScheme())||
               uri.getRawUserInfo()!=null||uri.getRawFragment()!=null||
               (uri.getPort()!=-1&&uri.getPort()!=443))return false;
            String host=uri.getHost();
            return "github.com".equalsIgnoreCase(host)||
                   "release-assets.githubusercontent.com".equalsIgnoreCase(host);
        }catch(Exception ignored){return false;}
    }

    public static boolean isNewerApprovedUpdate(
            String url,String sha256,long code,long installedCode) {
        return UpdateUrlPolicy.isAllowed(url)&&
                sha256!=null&&sha256.matches("[a-f0-9]{64}")&&
                UpdateUrlPolicy.isNewer(code,installedCode);
    }
}
