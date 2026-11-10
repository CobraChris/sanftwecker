package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Browser-free, explicitly user-initiated watch APK download.
 * The watch APK is NEVER installed on the phone; only a verified shareable URI
 * is provided. Requires a separate user-confirmed Wear OS installation.
 */
public final class VerifiedWatchApkDownloader {
    private static final long MAX_BYTES=30L*1024*1024;
    private static final int MAX_REDIRECTS=5;
    private VerifiedWatchApkDownloader(){}

    public static Uri download(Context context,String url,String tag,String sha256,
            long expectedCode,VerifiedApkDownloader.Progress progress) throws Exception {
        long phoneCode=installedPhoneCode(context);
        if(!WatchApkPolicy.isCompatibleWithPhone(expectedCode,phoneCode)||
                !WatchApkPolicy.isApproved(url,tag,expectedCode,sha256))
            throw new SecurityException("Uhren-APK passt nicht zur Handyversion oder Quelle");

        File dir=new File(context.getCacheDir(),"watch_updates");
        if(!dir.isDirectory()&&!dir.mkdirs())
            throw new IOException("Watch-Update-Speicher nicht verfügbar");
        String filename=WatchApkPolicy.filename(expectedCode);
        File part=new File(dir,filename+".part");
        File ready=new File(dir,filename);

        Exception last=null;
        for(int attempt=1;attempt<=3;attempt++){
            if(part.exists()&&!part.delete())throw new IOException("Altes Downloadfragment blockiert");
            try {
                transfer(url,sha256,part,progress);
                Files.move(part.toPath(),ready.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                try {
                    verifyOriginalSigningIdentity(context,ready,expectedCode);
                }catch(Exception badApk){
                    ready.delete();
                    throw badApk;
                }
                if(progress!=null)progress.onProgress(100);
                return FileProvider.getUriForFile(context,
                        context.getPackageName()+".updates",ready);
            }catch(SecurityException fatal){
                part.delete();
                throw fatal;
            }catch(IOException interrupted){
                last=interrupted;
                part.delete();
                if(attempt==3)break;
                if(progress!=null)progress.onProgress(0);
            }catch(Exception fatal){
                part.delete();
                throw fatal;
            }
        }
        throw new IOException("Download nach drei Versuchen fehlgeschlagen",last);
    }

    private static void transfer(String url,String expectedSha,File output,
            VerifiedApkDownloader.Progress progress) throws Exception {
        HttpURLConnection conn=null;
        String next=url;
        try {
            for(int i=0;i<=MAX_REDIRECTS;i++){
                if(!VerifiedUpdatePolicy.isTrustedDownloadHop(next))
                    throw new SecurityException("Unerlaubte APK-Weiterleitung");
                conn=(HttpURLConnection)new URL(next).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(12_000);
                conn.setReadTimeout(20_000);
                conn.setRequestProperty("User-Agent","Sanftwecker-Watch-Verified-Update");
                int status=conn.getResponseCode();
                if(status==301||status==302||status==303||
                        status==307||status==308){
                    String location=conn.getHeaderField("Location");
                    if(location==null)throw new IOException("Weiterleitung ohne Ziel");
                    next=new URL(new URL(next),location).toString();
                    conn.disconnect();
                    conn=null;
                    continue;
                }
                if(status!=200)throw new IOException("GitHub-Download HTTP "+status);
                break;
            }
            if(conn==null)throw new SecurityException("Zu viele Weiterleitungen");
            long total=conn.getContentLengthLong();
            if(total>MAX_BYTES)throw new SecurityException("APK über 30 MB");
            long count=0;
            MessageDigest sha=MessageDigest.getInstance("SHA-256");
            int reported=-1;
            try(InputStream in=conn.getInputStream();
                FileOutputStream out=new FileOutputStream(output,false)){
                byte[] buffer=new byte[16_384];int read;
                while((read=in.read(buffer))!=-1){
                    count+=read;
                    if(count>MAX_BYTES)throw new SecurityException("APK über 30 MB");
                    out.write(buffer,0,read);
                    sha.update(buffer,0,read);
                    if(total>0&&progress!=null){
                        int percent=(int)Math.min(99,100L*count/total);
                        if(percent>=reported+5){
                            progress.onProgress(percent);
                            reported=percent;
                        }
                    }
                }
                out.getFD().sync();
            }
            if(count<=0)throw new IOException("GitHub hat keine APK-Daten geliefert");
            if(total>0&&count!=total)
                throw new IOException("APK unvollständig: "+count+" statt "+total+" Bytes");
            StringBuilder hex=new StringBuilder(64);
            for(byte b:sha.digest())
                hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            if(!expectedSha.equals(hex.toString()))
                throw new SecurityException("SHA-256 stimmt nicht mit dem Release überein");
        }finally{
            if(conn!=null)conn.disconnect();
        }
    }

    private static long installedPhoneCode(Context c)throws Exception{
        PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),0);
        return info.getLongVersionCode();
    }

    private static void verifyOriginalSigningIdentity(Context c,File apk,long code)
            throws Exception{
        PackageManager pm=c.getPackageManager();
        int flags=PackageManager.GET_SIGNING_CERTIFICATES;
        PackageInfo incoming=pm.getPackageArchiveInfo(apk.getAbsolutePath(),flags);
        PackageInfo current=pm.getPackageInfo(c.getPackageName(),flags);
        if(incoming==null||current==null||
                !c.getPackageName().equals(incoming.packageName)||
                incoming.getLongVersionCode()!=code||
                incoming.signingInfo==null||current.signingInfo==null)
            throw new SecurityException("Watch-APK hat falsche Paketdaten");
        Signature[] old=current.signingInfo.getApkContentsSigners();
        Signature[] next=incoming.signingInfo.getApkContentsSigners();
        if(old==null||next==null||old.length!=1||next.length!=1||
                !Arrays.equals(old[0].toByteArray(),next[0].toByteArray()))
            throw new SecurityException("Originalsignatur der Watch-APK stimmt nicht");
    }
}
