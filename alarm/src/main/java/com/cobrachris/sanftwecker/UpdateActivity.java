package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Opt-in update checker and verified in-app APK installer (only on user tap).
 * Android still requires source authorization and a visible confirmation dialog.
 *
 * A true in-place update additionally requires a stable private signing key,
 * which CI must receive through GitHub Actions secrets; never bundle keys here.
 */
public final class UpdateActivity extends Activity {
    // Old feed stays authoritative for installed v19 clients. The new source
    // becomes available ONLY to a later, explicitly signed bridge release.
    private static final String[] RELEASE_APIS = {
            "https://api.github.com/repos/CobraChris/app-fabrik-template/releases?per_page=20",
            "https://api.github.com/repos/CobraChris/sanftwecker/releases?per_page=20"
    };
    private static final String[] RELEASE_REPOS = {
            UpdateUrlPolicy.OLD_REPO, UpdateUrlPolicy.NEW_REPO
    };
    private static final String ASSET_NAME = "sanftwecker-release.apk";
    private TextView status;
    private static final int INSTALL_SOURCE_REQUEST=721;
    private Button check, install, browser;
    private Candidate candidate;
    private boolean downloading=false;
    private boolean waitingForSourceApproval=false;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        UiKit.window(this);
        LinearLayout root=UiKit.mainColumn(this);
        root.addView(UiKit.heading(this,"App aktualisieren"));
        ClockTabs.add(this,root,ClockTabs.ALARMS);
        root.addView(UiKit.label(this,"Installierte Version: "+
                getCurrentVersionName()+" (Build "+getCurrentVersion()+")",
                16,UiKit.TEXT));
        root.addView(UiKit.label(this,
                "Eine neue APK kann die vorhandene App ersetzen, wenn sie mit "+
                "demselben dauerhaften Schlüssel signiert wurde. "+
                "Deine gespeicherten Wecker und Berechtigungen bleiben bei einem "+
                "regulären Update normalerweise erhalten.",
                14,UiKit.MUTED));
        status=UiKit.label(this,"Noch nicht geprüft.",15,UiKit.TEXT);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=UiKit.dp(this,18);p.bottomMargin=UiKit.dp(this,14);
        root.addView(status,p);
        install=UiKit.button(this,"Update installieren",true);
        install.setEnabled(false);
        root.addView(install);
        install.setOnClickListener(v->installUpdate());
        check=UiKit.button(this,"Nach Update suchen",false);
        root.addView(check);
        check.setOnClickListener(v->checkUpdates());
        browser=UiKit.button(this,"Alternativ im Browser herunterladen",false);
        browser.setEnabled(false);
        root.addView(browser);
        browser.setOnClickListener(v->openBrowser());
        Button watchDownload=UiKit.button(this,
                "Galaxy Watch: APK ohne Browser laden",false);
        root.addView(watchDownload);
        watchDownload.setOnClickListener(v->startActivity(
                new Intent(this,WatchUpdateActivity.class)));
        root.addView(UiKit.label(this,
                "„Update installieren“ lädt die geprüfte APK direkt in der App herunter "+
                "und öffnet den Android-Installationsdialog. Beim ersten Mal muss "+
                "Android eventuell die Installation aus Sanftwecker erlauben. "+
                "Es wird niemals automatisch bestätigt, deinstalliert oder gelöscht. "+
                "Bei einer Signaturfehlermeldung bitte abbrechen, nicht deinstallieren.",
                13,UiKit.MUTED));
        setContentView(UiKit.scroll(this,root));
    }

    private long getCurrentVersion(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0)
                .getLongVersionCode();}
        catch(Exception ignored){return -1;}
    }
    private String getCurrentVersionName(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
        catch(Exception ignored){return "unbekannt";}
    }

    private static final class Candidate {
        final long code; final String url; final String sha256;
        Candidate(long code,String url,String sha256){
            this.code=code;this.url=url;this.sha256=sha256;
        }
    }

    /** GitHub releases (including prereleases) need exact filenames, tags and SHA-256. */
    static Candidate newestSignedAsset(String raw,String repository){
        try{
            JSONArray releases=new JSONArray(raw);
            Candidate best=null;
            for(int i=0;i<releases.length();i++){
                JSONObject release=releases.getJSONObject(i);
                if(release.optBoolean("draft",true))continue;
                String tag=release.optString("tag_name","");
                long code=UpdateUrlPolicy.versionCode(tag);
                if(code<=0)continue;
                JSONArray assets=release.optJSONArray("assets");
                if(assets==null)continue;
                for(int j=0;j<assets.length();j++){
                    JSONObject asset=assets.getJSONObject(j);
                    if(!ASSET_NAME.equals(asset.optString("name","")))continue;
                    String url=asset.optString("browser_download_url","");
                    if(!UpdateUrlPolicy.isForTagFromRepository(url,tag,repository))continue;
                    String sha256=VerifiedUpdatePolicy.parseSha256(
                            asset.optString("digest",""));
                    if(sha256==null)continue; // No unsigned/unverifiable download.
                    if(best==null||code>best.code)
                        best=new Candidate(code,url,sha256);
                }
            }
            return best;
        }catch(Exception e){return null;}
    }

    /** Read-only HTTPS GET; no APK bytes or credentials are downloaded here. */
    static String fetchReleases(String api) throws Exception {
        HttpURLConnection conn=null;
        try{
            conn=(HttpURLConnection)new URL(api).openConnection();
            conn.setRequestProperty("Accept","application/vnd.github+json");
            conn.setRequestProperty("User-Agent","Sanftwecker-UpdateCheck");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setInstanceFollowRedirects(false);
            if(conn.getResponseCode()!=200)
                throw new IllegalStateException("GitHub HTTP "+conn.getResponseCode());
            StringBuilder json=new StringBuilder();
            try(InputStream in=conn.getInputStream();
                BufferedReader reader=new BufferedReader(new InputStreamReader(in,
                        java.nio.charset.StandardCharsets.UTF_8))){
                char[] buf=new char[2048];int count;
                while((count=reader.read(buf))!=-1) {
                    json.append(buf,0,count);
                    if(json.length()>500_000)throw new IllegalStateException("Antwort zu groß");
                }
            }
            return json.toString();
        }finally{if(conn!=null)conn.disconnect();}
    }

    private void openBrowser(){
        Candidate selected=candidate;
        if(selected==null||downloading)return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(selected.url))
                    .addCategory(Intent.CATEGORY_BROWSABLE));
        }catch(ActivityNotFoundException e) {
            new AlertDialog.Builder(this).setMessage("Kein Browser verfügbar.")
                    .setPositiveButton("OK",null).show();
        }
    }

    private void installUpdate(){
        Candidate selected=candidate;
        if(selected==null||downloading)return;
        if(!VerifiedUpdatePolicy.isNewerApprovedUpdate(
                selected.url,selected.sha256,selected.code,getCurrentVersion())){
            status.setText("Updatequelle ist nicht mehr gültig. Bitte erneut prüfen.");
            return;
        }
        // System consent is mandatory; do not silently grant install privileges.
        if(!getPackageManager().canRequestPackageInstalls()){
            waitingForSourceApproval=true;
            status.setText("Einmalig „Aus dieser Quelle zulassen“ für Sanftwecker aktivieren.");
            try {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:"+getPackageName())),INSTALL_SOURCE_REQUEST);
            }catch(ActivityNotFoundException e){
                waitingForSourceApproval=false;
                status.setText("Android-Einstellung nicht gefunden. Bitte Browser nutzen.");
            }
            return;
        }
        downloading=true;
        install.setEnabled(false);
        check.setEnabled(false);
        browser.setEnabled(false);
        status.setText("Lade geprüfte APK herunter …");
        new Thread(()->{
            try{
                Uri verified=VerifiedApkDownloader.download(
                        getApplicationContext(),selected.url,selected.sha256,selected.code,
                        percent->runOnUiThread(()->{
                            if(!isFinishing()&&!isDestroyed())
                                status.setText("APK herunterladen und prüfen: "+percent+" %");
                        }));
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    downloading=false;
                    install.setEnabled(true);
                    check.setEnabled(true);
                    browser.setEnabled(true);
                    try{
                        Intent intent=new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(verified,VerifiedApkDownloader.mimeType());
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setClipData(ClipData.newUri(getContentResolver(),
                                "Geprüftes Sanftwecker-Update",verified));
                        startActivity(intent);
                        status.setText("APK geprüft. Bitte Android-Update bestätigen.");
                    }catch(ActivityNotFoundException|SecurityException e){
                        status.setText("Android-Installer nicht verfügbar. "+
                                "Alternativ im Browser herunterladen.");
                    }
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    downloading=false;
                    install.setEnabled(true);
                    check.setEnabled(true);
                    browser.setEnabled(true);
                    status.setText("Update nicht gestartet: "+e.getMessage()+
                            ". Bitte erneut prüfen oder Browser verwenden.");
                });
            }
        },"Sanftwecker-Verified-Apk-Download").start();
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=INSTALL_SOURCE_REQUEST||!waitingForSourceApproval)return;
        waitingForSourceApproval=false;
        if(getPackageManager().canRequestPackageInstalls())installUpdate();
        else status.setText("Installation aus dieser Quelle nicht erlaubt. "+
                "Du kannst das Update auch über den Browser herunterladen.");
    }

    private void checkUpdates(){
        if(downloading)return;
        status.setText("Prüfe GitHub-Veröffentlichungen …");
        candidate=null;install.setEnabled(false);browser.setEnabled(false);
        check.setEnabled(false);
        new Thread(()->{
            Candidate best=null;
            int checked=0;
            for(int index=0;index<RELEASE_APIS.length;index++){
                try{
                    Candidate current=newestSignedAsset(
                            fetchReleases(RELEASE_APIS[index]),RELEASE_REPOS[index]);
                    checked++;
                    // Keep the legacy feed as a tie-breaker for the bridge update.
                    if(current!=null&&(best==null||current.code>best.code))
                        best=current;
                }catch(Exception ignored){
                    // One unreachable feed must not hide a valid release on the other.
                }
            }
            final Candidate selected=best;
            final boolean networkFailed=checked==0;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                check.setEnabled(true);
                if(networkFailed){
                    status.setText("Updateprüfung fehlgeschlagen. Bitte Internetverbindung prüfen.");
                }else if(selected==null){
                    status.setText("Noch keine freigegebene Sanftwecker-APK veröffentlicht.");
                }else if(!UpdateUrlPolicy.isNewer(selected.code,getCurrentVersion())){
                    status.setText("Die installierte Version ist aktuell. "+
                            "(Veröffentlichter Build "+selected.code+")");
                }else{
                    status.setText("Update verfügbar: Build "+selected.code+
                            ". Android installiert es erst nach deiner Bestätigung.");
                    candidate=selected;
                    install.setEnabled(true);
                    browser.setEnabled(true);
                }
            });
        },"Sanftwecker-Update-Check").start();
    }

}
