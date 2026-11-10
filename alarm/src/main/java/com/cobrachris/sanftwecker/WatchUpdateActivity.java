package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;

/** Explicit watch APK download to the phone, without browser/MediaStore downloads. */
public final class WatchUpdateActivity extends Activity {
    private static final String API=
            "https://api.github.com/repos/CobraChris/sanftwecker/releases?per_page=20";
    private static final int SAVE_FILE=752;
    private TextView status;
    private Button check,download,save,share;
    private Candidate selected;
    private Uri verifiedUri;
    private boolean busy=false;

    static final class Candidate {
        final String tag,url,sha256;
        final long code;
        Candidate(String tag,String url,String sha256,long code){
            this.tag=tag;this.url=url;this.sha256=sha256;this.code=code;
        }
    }

    /** Reject all assets except one exact same-version original-repo watch APK. */
    static Candidate findMatchingWatchAsset(String raw,long phoneCode){
        try {
            JSONArray releases=new JSONArray(raw);
            for(int i=0;i<releases.length();i++){
                JSONObject release=releases.getJSONObject(i);
                if(release.optBoolean("draft",true))continue;
                String tag=release.optString("tag_name","");
                if(UpdateUrlPolicy.versionCode(tag)!=phoneCode)continue;
                JSONArray assets=release.optJSONArray("assets");
                if(assets==null)continue;
                for(int j=0;j<assets.length();j++){
                    JSONObject asset=assets.getJSONObject(j);
                    if(!WatchApkPolicy.filename(phoneCode)
                            .equals(asset.optString("name","")))continue;
                    String url=asset.optString("browser_download_url","");
                    String digest=VerifiedUpdatePolicy.parseSha256(
                            asset.optString("digest",""));
                    if(WatchApkPolicy.isApproved(url,tag,phoneCode,digest))
                        return new Candidate(tag,url,digest,phoneCode);
                }
            }
        }catch(Exception ignored){}
        return null;
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        UiKit.window(this);
        LinearLayout root=UiKit.mainColumn(this);
        root.addView(UiKit.heading(this,"Galaxy Watch aktualisieren"));
        root.addView(UiKit.label(this,
                "Watch-APK ohne Browser direkt im Sanftwecker laden. "+
                "Der Download wird mit SHA-256 und Originalsignatur geprüft.",
                15,UiKit.TEXT));
        root.addView(UiKit.label(this,
                "Wichtig: Diese Funktion installiert nichts auf der Uhr. "+
                "Du speicherst die geprüfte APK oder gibst sie an eine "+
                "Watch-Installations-App weiter. Die Installation erfolgt "+
                "ausschließlich nach deiner Bestätigung auf der Uhr. "+
                "Die vorhandene Watch-App niemals deinstallieren.",
                14,UiKit.MUTED));
        status=UiKit.label(this,"Noch keine Watch-APK geprüft.",15,UiKit.TEXT);
        LinearLayout.LayoutParams space=new LinearLayout.LayoutParams(-1,-2);
        space.topMargin=UiKit.dp(this,20);
        space.bottomMargin=UiKit.dp(this,12);
        root.addView(status,space);
        check=UiKit.button(this,"Passende Watch-Version suchen",false);
        root.addView(check);
        check.setOnClickListener(v->checkForWatch());
        download=UiKit.button(this,"Watch-APK direkt herunterladen",true);
        root.addView(download);
        download.setOnClickListener(v->downloadWatch());
        save=UiKit.button(this,"Geprüfte APK in Downloads speichern",false);
        root.addView(save);
        save.setOnClickListener(v->saveToDownloads());
        share=UiKit.button(this,"APK an Watch-Installations-App weitergeben",false);
        root.addView(share);
        share.setOnClickListener(v->shareWithInstaller());
        setContentView(UiKit.scroll(this,root));
        updateButtons();
    }

    private void updateButtons(){
        check.setEnabled(!busy);
        download.setEnabled(!busy&&selected!=null);
        save.setEnabled(!busy&&verifiedUri!=null);
        share.setEnabled(!busy&&verifiedUri!=null);
    }

    private long phoneVersion(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0)
                .getLongVersionCode();}
        catch(Exception e){return -1;}
    }

    private void checkForWatch(){
        if(busy)return;
        busy=true;
        selected=null;
        verifiedUri=null;
        status.setText("Prüfe passende Original-Watch-APK auf GitHub …");
        updateButtons();
        new Thread(()->{
            String result;Candidate candidate;
            try{
                result=UpdateActivity.fetchReleases(API);
                candidate=findMatchingWatchAsset(result,phoneVersion());
            }catch(Exception e){
                runOnUiThread(()->complete(null,
                        "Watch-Updateprüfung fehlgeschlagen: "+e.getMessage()));
                return;
            }
            Candidate finalCandidate=candidate;
            runOnUiThread(()->complete(finalCandidate,finalCandidate==null?
                    "Für deine installierte Handy-Version gibt es keine passende "+
                    "verifizierte Watch-APK. Bitte zuerst die Handy-App aktualisieren.":
                    "Signierte Watch-APK für Build "+finalCandidate.code+
                    " gefunden. Du kannst sie jetzt direkt laden."));
        },"Sanftwecker-Watch-Check").start();
    }

    private void complete(Candidate result,String message){
        if(isFinishing()||isDestroyed())return;
        selected=result;
        busy=false;
        status.setText(message);
        updateButtons();
    }

    private void downloadWatch(){
        Candidate c=selected;
        if(c==null||busy)return;
        if(!WatchApkPolicy.isCompatibleWithPhone(c.code,phoneVersion())){
            complete(null,"Handy-Version wurde geändert. Bitte erneut prüfen.");
            return;
        }
        verifiedUri=null;
        busy=true;
        status.setText("Watch-APK direkt herunterladen; Browser wird nicht geöffnet …");
        updateButtons();
        new Thread(()->{
            try {
                Uri uri=VerifiedWatchApkDownloader.download(
                        getApplicationContext(),c.url,c.tag,c.sha256,c.code,
                        p->runOnUiThread(()->{
                            if(!isFinishing()&&!isDestroyed())
                                status.setText("Watch-APK laden und prüfen: "+p+" %");
                        }));
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    verifiedUri=uri;
                    busy=false;
                    status.setText("Watch-APK vollständig heruntergeladen. "+
                            "SHA-256 und Originalzertifikat geprüft. "+
                            "Jetzt speichern oder an eine Watch-Installations-App weitergeben.");
                    updateButtons();
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;
                    status.setText("Kein vollständiger Watch-Download: "+
                            e.getMessage()+". Du kannst erneut versuchen.");
                    updateButtons();
                });
            }
        },"Sanftwecker-Watch-APK-Download").start();
    }

    private void saveToDownloads(){
        if(busy||verifiedUri==null||selected==null)return;
        Intent create=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        create.setType(VerifiedApkDownloader.mimeType());
        create.addCategory(Intent.CATEGORY_OPENABLE);
        create.putExtra(Intent.EXTRA_TITLE,WatchApkPolicy.filename(selected.code));
        try{startActivityForResult(create,SAVE_FILE);}
        catch(ActivityNotFoundException e){
            status.setText("Android-Dateiauswahl nicht verfügbar. "+
                    "Du kannst die APK alternativ weitergeben.");
        }
    }

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=SAVE_FILE||result!=RESULT_OK||data==null||
                data.getData()==null||verifiedUri==null||busy)return;
        Uri source=verifiedUri,target=data.getData();
        busy=true;updateButtons();
        status.setText("Geprüfte APK in gewählten Ordner kopieren …");
        new Thread(()->{
            try(InputStream in=getContentResolver().openInputStream(source);
                OutputStream out=getContentResolver().openOutputStream(target,"w")){
                if(in==null||out==null)throw new IllegalStateException("Speichern nicht möglich");
                byte[] dataBuffer=new byte[16_384];
                int read;
                while((read=in.read(dataBuffer))!=-1)out.write(dataBuffer,0,read);
                out.flush();
                runOnUiThread(()->finishSave(
                        "Watch-APK gespeichert. Nutze die Datei zur bestätigten Watch-Installation."));
            }catch(Exception e){
                runOnUiThread(()->finishSave(
                        "Speichern fehlgeschlagen: "+e.getMessage()));
            }
        },"Sanftwecker-Save-Verified-Watch-APK").start();
    }

    private void finishSave(String text){
        if(isFinishing()||isDestroyed())return;
        busy=false;status.setText(text);updateButtons();
    }

    private void shareWithInstaller(){
        if(busy||verifiedUri==null)return;
        try{
            Intent send=new Intent(Intent.ACTION_SEND);
            send.setType(VerifiedApkDownloader.mimeType());
            send.putExtra(Intent.EXTRA_STREAM,verifiedUri);
            send.setClipData(ClipData.newUri(getContentResolver(),
                    "Verifizierte Sanftwecker Watch-APK",verifiedUri));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send,
                    "Watch-APK an Installations-App übergeben"));
        }catch(ActivityNotFoundException|SecurityException e){
            new AlertDialog.Builder(this).setMessage(
                    "Keine geeignete App zur Dateiübergabe gefunden. "+
                    "Speichere die APK stattdessen über „in Downloads speichern“.")
                    .setPositiveButton("OK",null).show();
        }
    }
}
