package com.cobrachris.watchvibrationpoc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * User-triggered, signaturgeprüftes Watch-Update. Die Systeminstallation
 * erfordert weiterhin eine ausdrückliche Bestätigung auf der Uhr.
 */
public final class WatchSelfUpdateActivity extends Activity {
    private TextView status,versionLabel;
    private Button check,download,install;
    private WatchSelfUpdateClient.Candidate candidate;
    private Uri verified;
    private boolean busy,awaitingSourcePermission;

    private int dp(float size){
        return Math.round(size*getResources().getDisplayMetrics().density);
    }
    private void show(String text){status.setText(text);}
    private void buttons(){
        check.setEnabled(!busy);
        download.setEnabled(!busy&&candidate!=null);
        install.setEnabled(!busy&&verified!=null);
    }
    private boolean idle(){
        try{WatchSelfUpdateClient.requireIdleAlarm(this);return true;}
        catch(IllegalStateException error){show(error.getMessage());return false;}
    }
    private void refreshVersion(){
        try{
            versionLabel.setText("Installiert: "+WatchSelfUpdateFeedback.installedVersion(this));
        }catch(Exception e){
            versionLabel.setText("Installierte Version nicht lesbar");
        }
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        if(state!=null)awaitingSourcePermission=state.getBoolean("awaiting_permission",false);
        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(16),dp(28),dp(16),dp(80));
        // Round-screen edges are clipped; retain an actual scroll range.
        content.setMinimumHeight(getResources().getDisplayMetrics().heightPixels+dp(80));
        int usableWidth=Math.round(getResources().getDisplayMetrics().widthPixels*0.82f);
        TextView title=new TextView(this);
        title.setText("Uhr aktualisieren");
        title.setTextSize(17);title.setGravity(Gravity.CENTER);
        content.addView(title);
        versionLabel=new TextView(this);
        versionLabel.setGravity(Gravity.CENTER);
        versionLabel.setTextSize(11);
        content.addView(versionLabel,new LinearLayout.LayoutParams(usableWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        status=new TextView(this);
        status.setTextSize(12);status.setGravity(Gravity.CENTER);
        status.setText("Noch nicht geprüft.");
        TextView hint=new TextView(this);
        hint.setText("Nur offizielle APKs. Signaturprüfung und Android-Bestätigung.");
        hint.setGravity(Gravity.CENTER);hint.setTextSize(11);

        check=new Button(this);check.setText("Update suchen");check.setTextSize(13);
        content.addView(check,new LinearLayout.LayoutParams(usableWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        check.setOnClickListener(v->checkUpdate());
        content.addView(status,new LinearLayout.LayoutParams(usableWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        download=new Button(this);download.setText("APK laden und prüfen");
        download.setTextSize(13);
        content.addView(download,new LinearLayout.LayoutParams(usableWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        download.setOnClickListener(v->downloadUpdate());
        install=new Button(this);install.setText("Update installieren");
        install.setTextSize(13);
        content.addView(install,new LinearLayout.LayoutParams(usableWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        install.setOnClickListener(v->confirmInstall());
        content.addView(hint,new LinearLayout.LayoutParams(usableWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(content);
        setContentView(scroll);
        refreshVersion();
        buttons();
    }
    @Override protected void onSaveInstanceState(Bundle outState){
        outState.putBoolean("awaiting_permission",awaitingSourcePermission);
        super.onSaveInstanceState(outState);
    }
    @Override protected void onResume(){
        super.onResume();
        refreshVersion();
        try{
            String result=WatchSelfUpdateFeedback.consumeSuccess(this);
            if(result!=null){
                busy=false;candidate=null;verified=null;
                awaitingSourcePermission=false;
                show(result);
                buttons();
                return;
            }
        }catch(Exception e){
            show("Versionsprüfung fehlgeschlagen: "+e.getMessage());
        }
        // One-time continuation only after the user actively granted permission.
        if(awaitingSourcePermission){
            awaitingSourcePermission=false;
            if(getPackageManager().canRequestPackageInstalls()&&verified!=null&&idle()){
                show("Berechtigung erteilt. Öffne Android-Installation …");
                openInstaller();
            }else if(verified==null){
                show("Aktualisierung unterbrochen. APK erneut laden und prüfen.");
            }else{
                show("Installationsfreigabe nicht erteilt. Keine Installation gestartet.");
            }
        }
    }
    private void checkUpdate(){
        if(busy||!idle())return;
        busy=true;candidate=null;verified=null;
        show("Prüfe offizielle GitHub-Releases …");
        buttons();
        new Thread(()->{
            try{
                WatchSelfUpdateClient.Candidate found=WatchSelfUpdateClient.check(
                        getApplicationContext());
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;candidate=found;
                    show(found==null?"Die Watch-App ist aktuell oder kein geprüftes Update vorhanden.":
                            "Watch-Version "+found.code+" gefunden. APK jetzt laden?");
                    buttons();
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;show("Updateprüfung fehlgeschlagen: "+e.getMessage());
                    buttons();
                });
            }
        },"Sanftwecker-Watch-Release-Check").start();
    }
    private void downloadUpdate(){
        WatchSelfUpdateClient.Candidate c=candidate;
        if(busy||c==null||!idle())return;
        busy=true;verified=null;
        show("Lade signierte Watch-APK direkt auf die Uhr …");
        buttons();
        new Thread(()->{
            try{
                Uri file=WatchSelfUpdateClient.download(getApplicationContext(),c,
                        percent->runOnUiThread(()->{
                            if(!isFinishing()&&!isDestroyed())
                                show("Download und Prüfung: "+percent+" %");
                        }));
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;verified=file;
                    show("APK vollständig geladen: SHA-256, Paket-ID, Version und "+
                            "Originalzertifikat geprüft. Bereit zur Installation.");
                    buttons();
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;show("Download nicht abgeschlossen: "+e.getMessage());
                    buttons();
                });
            }
        },"Sanftwecker-Watch-Verified-Download").start();
    }
    private void confirmInstall(){
        if(busy||verified==null||candidate==null||!idle())return;
        new AlertDialog.Builder(this)
                .setTitle("Sanftwecker Uhr aktualisieren?")
                .setMessage("Die APK wurde geprüft. Android muss das Update "+
                        "bestätigen. Bestehende App-Daten bleiben erhalten. "+
                        "Nicht deinstallieren.")
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Weiter",(dialog,which)->openInstaller())
                .show();
    }
    private void openInstaller(){
        if(verified==null||candidate==null||!idle()){
            show("Geprüfte APK fehlt. Bitte erneut laden.");
            return;
        }
        try{
            if(WatchSelfUpdateClient.installedCode(this)>=candidate.code){
                show("Version "+candidate.code+" ist bereits installiert.");
                refreshVersion();return;
            }
        }catch(Exception e){
            show("Versionsprüfung fehlgeschlagen: "+e.getMessage());return;
        }
        if(!getPackageManager().canRequestPackageInstalls()){
            show("Bitte auf der Uhr die Installation aus Sanftwecker erlauben.");
            try{
                awaitingSourcePermission=true;
                Intent allow=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:"+getPackageName()));
                startActivity(allow);
            }catch(ActivityNotFoundException|SecurityException error){
                awaitingSourcePermission=false;
                show("Wear OS bietet keine Freigabe für externe APK-Updates. "+
                        "Bitte vorerst GeminiMan verwenden; keine Deinstallation.");
            }
            return;
        }
        // Record only the requested target. Success is verified against installed version.
        WatchSelfUpdateFeedback.markRequested(this,candidate.code);
        Intent intent=new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(verified,"application/vnd.android.package-archive");
        intent.setClipData(ClipData.newUri(getContentResolver(),
                "Signiertes Sanftwecker-Uhr-Update",verified));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{
            startActivity(intent);
            show("Android-Installation bestätigen. Nach dem Update Sanftwecker öffnen.");
        }catch(ActivityNotFoundException|SecurityException e){
            // Preserve the previously working ACTION_VIEW path; use system fallback.
            try{
                Intent installIntent=new Intent(Intent.ACTION_INSTALL_PACKAGE);
                installIntent.setData(verified);
                installIntent.setClipData(ClipData.newUri(getContentResolver(),
                        "Signiertes Sanftwecker-Uhr-Update",verified));
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                installIntent.putExtra(Intent.EXTRA_RETURN_RESULT,true);
                startActivity(installIntent);
                show("Android-Installation bestätigen. Danach Version kontrollieren.");
            }catch(ActivityNotFoundException|SecurityException unavailable){
                WatchSelfUpdateFeedback.clearRequest(this);
                show("Der Android-Paketinstaller ist nicht erreichbar. "+
                        "GeminiMan bleibt als sicherer Ausweg.");
            }
        }
    }
}