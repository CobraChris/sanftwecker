package com.cobrachris.sanftwecker;

import android.content.Context;
import android.net.Uri;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;

/** User-initiated, signed APK handoff over Wear Data Layer; never installs. */
public final class PhoneWatchUpdateRelay {
    public static final String PATH="/sanftwecker/update/v1/apk";
    private PhoneWatchUpdateRelay(){}
    public interface Result { void done(String message); }

    public static void send(Context context,Uri uri,long code,String expectedSha,Result result){
        if(uri==null||expectedSha==null||!expectedSha.matches("[a-f0-9]{64}")){
            result.done("Keine geprüfte Watch-APK vorhanden.");return;
        }
        new Thread(()->{
            try{
                byte[] bytes;
                try(InputStream in=context.getContentResolver().openInputStream(uri);
                    ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    if(in==null)throw new IllegalStateException("APK nicht lesbar");
                    byte[] buffer=new byte[16384];int n,total=0;
                    while((n=in.read(buffer))!=-1){
                        total+=n;
                        if(total>30*1024*1024)throw new SecurityException("APK zu groß");
                        out.write(buffer,0,n);
                    }
                    bytes=out.toByteArray();
                }
                if(bytes.length==0)throw new SecurityException("APK leer");
                MessageDigest digest=MessageDigest.getInstance("SHA-256");
                StringBuilder actual=new StringBuilder();
                for(byte b:digest.digest(bytes))
                    actual.append(String.format(Locale.ROOT,"%02x",b&255));
                if(!expectedSha.equals(actual.toString()))
                    throw new SecurityException("SHA-256 beim Senden ungültig");
                PutDataMapRequest request=PutDataMapRequest.create(PATH);
                request.getDataMap().putAsset("apk",Asset.createFromBytes(bytes));
                request.getDataMap().putString("sha256",expectedSha);
                request.getDataMap().putLong("version_code",code);
                request.getDataMap().putLong("revision",System.nanoTime());
                Wearable.getDataClient(context.getApplicationContext())
                    .putDataItem(request.asPutDataRequest().setUrgent())
                    .addOnSuccessListener(item->result.done(
                        "Update für Wear OS bereitgestellt. Auf der Uhr Sanftwecker > "+
                        "Uhr aktualisieren öffnen. Der Empfang ist noch nicht bestätigt."))
                    .addOnFailureListener(e->result.done("Übergabe fehlgeschlagen: "+e.getMessage()));
            }catch(Exception e){result.done("Übergabe fehlgeschlagen: "+e.getMessage());}
        },"Sanftwecker-Wear-APK-Relay").start();
    }
}
