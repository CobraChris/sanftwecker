package com.cobrachris.watchvibrationpoc;

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
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Opt-in signed Watch APK retrieval; never installs, approves or uninstalls anything itself. */
public final class WatchSelfUpdateClient {
    public static final String API=
            "https://api.github.com/repos/CobraChris/sanftwecker/releases?per_page=20";
    private static final int REDIRECTS=5;
    private WatchSelfUpdateClient(){}

    public interface Progress { void update(int percent); }

    public static final class Candidate {
        public final String tag,url,sha256;
        public final long code;
        Candidate(String tag,String url,String sha256,long code){
            this.tag=tag;this.url=url;this.sha256=sha256;this.code=code;
        }
    }
    public static long installedCode(Context c) throws Exception {
        return c.getPackageManager().getPackageInfo(c.getPackageName(),0)
                .getLongVersionCode();
    }
    private static boolean alarmActive(Context c){
        return c.getSharedPreferences(WatchAlarmService.ACTIVE_PREF,Context.MODE_PRIVATE)
                .getLong("when",0)>0;
    }
    public static void requireIdleAlarm(Context c){
        if(alarmActive(c))
            throw new IllegalStateException("Während eines aktiven Uhralarms nicht aktualisieren");
    }
    public static Candidate find(String json,long installedCode) throws Exception {
        JSONArray releases=new JSONArray(json);
        Candidate best=null;
        for(int i=0;i<releases.length();i++){
            JSONObject release=releases.getJSONObject(i);
            if(release.optBoolean("draft",true))continue;
            String tag=release.optString("tag_name","");
            long code=WatchSelfUpdatePolicy.tagVersion(tag);
            if(code<=installedCode)continue;
            JSONArray assets=release.optJSONArray("assets");
            if(assets==null)continue;
            for(int j=0;j<assets.length();j++){
                JSONObject asset=assets.getJSONObject(j);
                if(!WatchSelfUpdatePolicy.filename(code).equals(asset.optString("name","")))
                    continue;
                String url=asset.optString("browser_download_url","");
                String digest=asset.optString("digest","");
                if(!WatchSelfUpdatePolicy.approved(tag,code,url,digest,installedCode))
                    continue;
                if(best==null||code>best.code)
                    best=new Candidate(tag,url,
                            WatchSelfUpdatePolicy.parseSha256(digest),code);
            }
        }
        return best;
    }
    public static Candidate check(Context c)throws Exception {
        requireIdleAlarm(c);
        HttpURLConnection conn=null;
        try{
            conn=(HttpURLConnection)new URL(API).openConnection();
            conn.setConnectTimeout(12_000);
            conn.setReadTimeout(15_000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Accept","application/vnd.github+json");
            conn.setRequestProperty("User-Agent","Sanftwecker-Watch-Self-Update");
            if(conn.getResponseCode()!=200)
                throw new IOException("Releaseprüfung: GitHub HTTP "+conn.getResponseCode());
            StringBuilder json=new StringBuilder();
            try(InputStream in=conn.getInputStream()){
                byte[] buffer=new byte[4096];int n;
                while((n=in.read(buffer))!=-1){
                    json.append(new String(buffer,0,n,java.nio.charset.StandardCharsets.UTF_8));
                    if(json.length()>800_000)throw new SecurityException("Metadaten zu groß");
                }
            }
            return find(json.toString(),installedCode(c));
        }finally{if(conn!=null)conn.disconnect();}
    }
    public static Uri download(Context c,Candidate candidate,Progress progress) throws Exception {
        requireIdleAlarm(c);
        long installed=installedCode(c);
        if(candidate==null||!WatchSelfUpdatePolicy.approved(candidate.tag,candidate.code,
                candidate.url,"sha256:"+candidate.sha256,installed))
            throw new SecurityException("Watch-Updatequelle oder Versionsnummer nicht freigegeben");
        File dir=new File(c.getCacheDir(),"watch_self_updates");
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cache nicht beschreibbar");
        File partial=new File(dir,WatchSelfUpdatePolicy.filename(candidate.code)+".part");
        File ready=new File(dir,WatchSelfUpdatePolicy.filename(candidate.code));
        Exception last=null;
        for(int attempt=1;attempt<=3;attempt++){
            if(partial.exists()&&!partial.delete())throw new IOException("Vorheriger Download blockiert");
            try{
                transfer(candidate.url,candidate.sha256,partial,progress);
                requireIdleAlarm(c);
                Files.move(partial.toPath(),ready.toPath(),StandardCopyOption.REPLACE_EXISTING);
                try{
                    verifyIdentity(c,ready,candidate.code);
                }catch(Exception wrong){
                    ready.delete();
                    throw wrong;
                }
                if(progress!=null)progress.update(100);
                return FileProvider.getUriForFile(c,c.getPackageName()+".updates",ready);
            }catch(IOException error){
                partial.delete();
                last=error;
                if(progress!=null)progress.update(0);
            }catch(Exception bad){
                partial.delete();
                throw bad;
            }
        }
        throw new IOException("Watch-Download nach 3 Versuchen fehlgeschlagen",last);
    }
    private static void transfer(String url,String expected,File output,Progress progress)
            throws Exception {
        String target=url;
        HttpURLConnection conn=null;
        try{
            for(int redirects=0;redirects<=REDIRECTS;redirects++){
                if(!WatchSelfUpdatePolicy.trustedHop(target))
                    throw new SecurityException("Unsichere Downloadweiterleitung");
                conn=(HttpURLConnection)new URL(target).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(12_000);
                conn.setReadTimeout(20_000);
                conn.setRequestProperty("User-Agent","Sanftwecker-Watch-Signed-APK");
                int status=conn.getResponseCode();
                if(status==301||status==302||status==303||status==307||status==308){
                    String location=conn.getHeaderField("Location");
                    if(location==null)throw new IOException("Downloadziel fehlt");
                    target=new URL(new URL(target),location).toString();
                    conn.disconnect();conn=null;
                    continue;
                }
                if(status!=200)throw new IOException("Download HTTP "+status);
                break;
            }
            if(conn==null)throw new SecurityException("Zu viele Downloadweiterleitungen");
            long total=conn.getContentLengthLong();
            if(total>WatchSelfUpdatePolicy.MAX_BYTES)throw new SecurityException("APK zu groß");
            long count=0;
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            int reported=-1;
            try(InputStream in=conn.getInputStream();
                FileOutputStream out=new FileOutputStream(output,false)){
                byte[] buffer=new byte[16384];int n;
                while((n=in.read(buffer))!=-1){
                    count+=n;
                    if(count>WatchSelfUpdatePolicy.MAX_BYTES)
                        throw new SecurityException("APK zu groß");
                    out.write(buffer,0,n);
                    digest.update(buffer,0,n);
                    if(total>0&&progress!=null){
                        int percent=(int)Math.min(99,100L*count/total);
                        if(percent>=reported+5){progress.update(percent);reported=percent;}
                    }
                }
                out.getFD().sync();
            }
            if(count<=0||(total>=0&&total!=count))
                throw new IOException("APK unvollständig");
            StringBuilder hex=new StringBuilder();
            for(byte b:digest.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));
            if(!expected.equals(hex.toString()))
                throw new SecurityException("SHA-256 der Watch-APK ist ungültig");
        }finally{if(conn!=null)conn.disconnect();}
    }
    private static void verifyIdentity(Context c,File file,long expectedCode)throws Exception{
        PackageManager pm=c.getPackageManager();
        int flags=PackageManager.GET_SIGNING_CERTIFICATES;
        PackageInfo incoming=pm.getPackageArchiveInfo(file.getAbsolutePath(),flags);
        PackageInfo installed=pm.getPackageInfo(c.getPackageName(),flags);
        if(incoming==null||installed==null||
                !WatchSelfUpdatePolicy.PACKAGE.equals(incoming.packageName)||
                incoming.getLongVersionCode()!=expectedCode||
                incoming.getLongVersionCode()<=installed.getLongVersionCode()||
                incoming.signingInfo==null||installed.signingInfo==null)
            throw new SecurityException("Watch-APK hat falsche Paketkennung oder Version");
        Signature[] a=incoming.signingInfo.getApkContentsSigners();
        Signature[] b=installed.signingInfo.getApkContentsSigners();
        if(a==null||b==null||a.length!=1||b.length!=1||
                !Arrays.equals(a[0].toByteArray(),b[0].toByteArray()))
            throw new SecurityException("Signatur der Watch-APK unterscheidet sich");
    }
}
