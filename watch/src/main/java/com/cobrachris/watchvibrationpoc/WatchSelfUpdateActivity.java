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
 * Optional update UI running on the watch itself. No ADB or companion installer,
 * no silent installations. Android still controls unknown-source permission and
 * displays the system installation confirmation if supported on this device.
 */
public final class WatchSelfUpdateActivity extends Activity {
    private TextView status;
    private Button check,download,install;
    private WatchSelfUpdateClient.Candidate candidate;
    private Uri verified;
    private boolean busy;

    private void show(String text){
        status.setText(text);
    }
    private void buttons(){
        check.setEnabled(!busy);
        download.setEnabled(!busy&&candidate!=null);
        install.setEnabled(!busy&&verified!=null);
    }
    private boolean idle(){
        try{WatchSelfUpdateClient.requireIdleAlarm(this);return true;}
        catch(IllegalStateException error){show(error.getMessage());return false;}
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        int pad=Math.round(12*getResources().getDisplayMetrics().density);
        content.setPadding(pad,pad,pad,pad);
        TextView title=new TextView(this);
        title.setText("Sanftwecker Uhr\nAktualisierung");
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        content.addView(title);
        TextView hint=new TextView(this);
        hint.setText("Kostenlos ohne GeminiMan. Nur offizielle APKs mit Originalsignatur.");
        hint.setGravity(Gravity.CENTER);
        hint.setTextSize(12);
        content.addView(hint);
        status=new TextView(this);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        status.setText("Keine Aktualisierung geprüft.");
        content.addView(status);
        check=new Button(this);check.setText("Update suchen");
        content.addView(check);
        check.setOnClickListener(v->checkUpdate());
        download=new Button(this);download.setText("APK laden und prüfen");
        content.addView(download);
        download.setOnClickListener(v->downloadUpdate());
        install=new Button(this);install.setText("Update installieren");
        content.addView(install);
        install.setOnClickListener(v->confirmInstall());
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content);
        setContentView(scroll);
        buttons();
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
                            "Originalzertifikat geprüft. Du kannst das Update starten.");
                    buttons();
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;
                    show("Download nicht abgeschlossen: "+e.getMessage());
                    buttons();
                });
            }
        },"Sanftwecker-Watch-Verified-Download").start();
    }
    private void confirmInstall(){
        if(busy||verified==null||!idle())return;
        new AlertDialog.Builder(this)
                .setTitle("Sanftwecker Uhr aktualisieren?")
                .setMessage("Die APK ist geprüft. Android muss die Installation "+
                        "auf dieser Uhr zusätzlich bestätigen. "+
                        "Die vorhandene App und ihre Daten bleiben erhalten. "+
                        "Nicht deinstallieren.")
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Weiter", (dialog,which)->openInstaller())
                .show();
    }
    private void openInstaller(){
        if(!idle())return;
        // Do not bypass Android's per-source consent; unsupported watch variants
        // show a clear fallback instead of trying privileged or silent installation.
        if(!getPackageManager().canRequestPackageInstalls()){
            show("Auf der Uhr muss die Installation aus Sanftwecker erlaubt werden.");
            try{
                Intent allow=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:"+getPackageName()));
                startActivity(allow);
            }catch(ActivityNotFoundException|SecurityException error){
                show("Wear OS bietet hier keine Freigabe für externe APK-Updates. "+
                        "Bitte vorerst GeminiMan verwenden; keine Deinstallation.");
            }
            return;
        }
        Intent intent=new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(verified,"application/vnd.android.package-archive");
        intent.setClipData(ClipData.newUri(getContentResolver(),
                "Signiertes Sanftwecker-Uhr-Update",verified));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{
            startActivity(intent);
            show("Bitte das offizielle Android-Update auf der Uhr bestätigen.");
        }catch(ActivityNotFoundException|SecurityException e){
            // A device may not expose a regular APK viewer; try the explicit
            // system installer before falling back to the existing ADB route.
            try{
                Intent installIntent=new Intent(Intent.ACTION_INSTALL_PACKAGE);
                installIntent.setData(verified);
                installIntent.setClipData(ClipData.newUri(getContentResolver(),
                        "Signiertes Sanftwecker-Uhr-Update",verified));
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(installIntent);
                show("Bitte die Installation auf der Uhr bestätigen.");
            }catch(ActivityNotFoundException|SecurityException unavailable){
                show("Der Android-Paketinstaller ist auf dieser Watch nicht "+
                        "direkt erreichbar. GeminiMan bleibt als sicherer Ausweg.");
            }
        }
    }
}
