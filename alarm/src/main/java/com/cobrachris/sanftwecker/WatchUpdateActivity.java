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
    public static final String EXTRA_START_GUIDED="start_guided_watch_update";
    private Button check,download,save,share,transfer,guided;
    private Candidate selected;
    private Uri verifiedUri;
    private boolean busy=false,waitingForWatchAck=false;
    private final android.os.Handler ackHandler=
        new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable ackPoll=new Runnable(){
        @Override public void run(){
            if(selected!=null&&waitingForWatchAck){
                long code=getSharedPreferences("sanftwecker_watch_relay",MODE_PRIVATE)
                    .getLong("verified_code",0);
                long at=getSharedPreferences("sanftwecker_watch_relay",MODE_PRIVATE)
                    .getLong("verified_at",0);
                if(code==selected.code&&at>0){
                    waitingForWatchAck=false;
                    status.setText("Galaxy Watch hat Version "+code+
                        " empfangen und ihre Signatur geprüft. "+
                        "Bitte auf der Uhr die Systeminstallation bestätigen.");
                }
            }
            ackHandler.postDelayed(this,1200L);
        }
    };

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
                "Einmal starten: Die Handy-App sucht, lädt, prüft und überträgt "+
                "die passende Watch-APK automatisch. Auf der Uhr bestätigst du "+
                "nur die Installation. Kein Watch-WLAN erforderlich.",
                15,UiKit.TEXT));
        root.addView(UiKit.label(this,
                "Die manuelle Einzeldatei-Installation ist nur noch ein Rückfallweg. "+
                "Die vorhandene Watch-App niemals deinstallieren. "+
                "Eine Systembestätigung auf der Uhr bleibt erforderlich.",
                14,UiKit.MUTED));
        status=UiKit.label(this,"Noch keine Watch-APK geprüft.",15,UiKit.TEXT);
        LinearLayout.LayoutParams space=new LinearLayout.LayoutParams(-1,-2);
        space.topMargin=UiKit.dp(this,20);
        space.bottomMargin=UiKit.dp(this,12);
        root.addView(status,space);
        guided=UiKit.button(this,"Watch-Update mit einem Klick vorbereiten",true);
        root.addView(guided);
        guided.setOnClickListener(v->runGuidedUpdate());
        root.addView(UiKit.label(this,
                "Erweiterte Optionen / manueller Rückfallweg",13,UiKit.MUTED));
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
        transfer=UiKit.button(this,"Geprüfte APK direkt an Galaxy Watch senden",false);
        root.addView(transfer);
        transfer.setOnClickListener(v->sendToWatch());
        setContentView(UiKit.scroll(this,root));
        updateButtons();
        if(state==null&&getIntent().getBooleanExtra(EXTRA_START_GUIDED,false))
            root.post(this::runGuidedUpdate);
    }

    @Override protected void onResume(){
        super.onResume();
        ackHandler.removeCallbacks(ackPoll);
        ackHandler.post(ackPoll);
        if(status!=null){
            long code=getSharedPreferences("sanftwecker_watch_relay",MODE_PRIVATE)
                .getLong("verified_code",0);
            long at=getSharedPreferences("sanftwecker_watch_relay",MODE_PRIVATE)
                .getLong("verified_at",0);
            if(code>0&&at>0&&System.currentTimeMillis()-at<60*60*1000L){
                status.setText("Galaxy Watch hat APK v"+code+" empfangen und geprüft. "+
                    "Installation muss auf der Uhr bestätigt werden.");
            }
        }
    }
    @Override protected void onPause(){
        ackHandler.removeCallbacks(ackPoll);
        super.onPause();
    }
    private void updateButtons(){
        guided.setEnabled(!busy);
        check.setEnabled(!busy);
        download.setEnabled(!busy&&selected!=null);
        save.setEnabled(!busy&&verifiedUri!=null);
        share.setEnabled(!busy&&verifiedUri!=null);
        transfer.setEnabled(!busy&&verifiedUri!=null);
    }

    private long phoneVersion(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0)
                .getLongVersionCode();}
        catch(Exception e){return -1;}
    }

    /**
     * Single explicitly requested end-to-end preparation. GitHub discovery,
     * full APK download and original-certificate verification precede the
     * existing Wear OS Data Layer transfer. This does NOT install on Wear OS.
     */
    private void runGuidedUpdate(){
        if(busy)return;
        busy=true;selected=null;verifiedUri=null;
        waitingForWatchAck=false;
        getSharedPreferences("sanftwecker_watch_relay",MODE_PRIVATE).edit()
            .remove("verified_code").remove("verified_at").apply();
        status.setText("Schritt 1/3: Passende Original-Watch-Version suchen …");
        updateButtons();
        new Thread(()->{
            try{
                long code=phoneVersion();
                if(code<=0)throw new IllegalStateException("Handy-Version nicht lesbar");
                String json=UpdateActivity.fetchReleases(API);
                Candidate match=findMatchingWatchAsset(json,code);
                if(match==null)
                    throw new IllegalStateException(
                        "Keine passende signierte Watch-APK veröffentlicht");
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    status.setText("Schritt 2/3: Watch-APK laden und prüfen …");
                });
                Uri file=VerifiedWatchApkDownloader.download(
                    getApplicationContext(),match.url,match.tag,match.sha256,match.code,
                    percent->runOnUiThread(()->{
                        if(!isFinishing()&&!isDestroyed())
                            status.setText("Schritt 2/3: Download und Prüfung "+percent+" %");
                    }));
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    selected=match;verifiedUri=file;
                    status.setText("Schritt 3/3: Geprüfte APK an gekoppelte Watch übergeben …");
                    PhoneWatchUpdateRelay.send(getApplicationContext(),file,
                        match.code,match.sha256,message->runOnUiThread(()->{
                            if(isFinishing()||isDestroyed())return;
                            busy=false;
                            waitingForWatchAck=message.startsWith(
                                "Update für Wear OS bereitgestellt.");
                            status.setText(message+" Nach Bestätigung auf der Uhr "+
                                "die angezeigte Version kontrollieren.");
                            updateButtons();
                        }));
                });
            }catch(Exception error){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;status.setText("Watch-Update nicht vorbereitet: "+
                        error.getMessage()+". Erneut versuchen oder manuelle Optionen nutzen.");
                    updateButtons();
                });
            }
        },"Sanftwecker-Watch-Guided-Update").start();
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

    private void sendToWatch(){
        if(busy||verifiedUri==null||selected==null)return;
        busy=true;updateButtons();
        status.setText("Bereits geprüfte APK über Wear OS übertragen …");
        PhoneWatchUpdateRelay.send(getApplicationContext(),verifiedUri,
            selected.code,selected.sha256,message->runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                busy=false;status.setText(message);updateButtons();
            }));
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
