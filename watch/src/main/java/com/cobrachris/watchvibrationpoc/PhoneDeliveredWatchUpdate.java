package com.cobrachris.watchvibrationpoc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Locale;

/** Validate and stage phone-delivered Watch APK, never install without user consent. */
public final class PhoneDeliveredWatchUpdate {
    public static final String PATH="/sanftwecker/update/v1/apk";
    private static final String PREF="phone_delivered_watch_update";
    private static final String CHANNEL="sanftwecker_watch_update_ready";
    private PhoneDeliveredWatchUpdate(){}

    public static void receive(Context context,Asset asset,long code,String sha){
        if(asset==null||sha==null||!sha.matches("[a-f0-9]{64}"))return;
        new Thread(()->{
            File partial=null;
            try{
                WatchSelfUpdateClient.requireIdleAlarm(context);
                if(code<=WatchSelfUpdateClient.installedCode(context))return;
                File dir=new File(context.getCacheDir(),"watch_self_updates");
                if(!dir.isDirectory()&&!dir.mkdirs())throw new IllegalStateException("Kein Cache");
                partial=new File(dir,"phone-relay.part");
                File ready=new File(dir,"phone-relay.apk");
                MessageDigest digest=MessageDigest.getInstance("SHA-256");
                long size=0;
                com.google.android.gms.wearable.DataClient.GetFdForAssetResponse response=
                    Tasks.await(Wearable.getDataClient(context).getFdForAsset(asset));
                try(InputStream in=response.getInputStream();
                    FileOutputStream out=new FileOutputStream(partial,false)){
                    if(in==null)throw new IllegalStateException("Asset nicht lesbar");
                    byte[] buffer=new byte[16384];int n;
                    while((n=in.read(buffer))!=-1){
                        size+=n;
                        if(size>WatchSelfUpdatePolicy.MAX_BYTES)
                            throw new SecurityException("APK zu groß");
                        digest.update(buffer,0,n);
                        out.write(buffer,0,n);
                    }
                    out.getFD().sync();
                }
                if(size==0)throw new SecurityException("APK leer");
                StringBuilder actual=new StringBuilder();
                for(byte b:digest.digest())
                    actual.append(String.format(Locale.ROOT,"%02x",b&255));
                if(!sha.equals(actual.toString()))
                    throw new SecurityException("SHA-256 stimmt nicht");
                WatchSelfUpdateClient.verifyIdentity(context,partial,code);
                WatchSelfUpdateClient.requireIdleAlarm(context);
                Files.move(partial.toPath(),ready.toPath(),StandardCopyOption.REPLACE_EXISTING);
                context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
                    .putLong("code",code).apply();
                notifyReady(context,code);
            }catch(Exception e){
                android.util.Log.w("SanftweckerWatch","Phone APK refused",e);
                if(partial!=null)partial.delete();
            }
        },"Sanftwecker-Wear-APK-Verify").start();
    }

    public static long pendingCode(Context context){
        return context.getSharedPreferences(PREF,Context.MODE_PRIVATE).getLong("code",0);
    }
    public static Uri verifiedPending(Context context)throws Exception{
        long code=pendingCode(context);
        if(code<=WatchSelfUpdateClient.installedCode(context))return null;
        File file=new File(new File(context.getCacheDir(),"watch_self_updates"),"phone-relay.apk");
        if(!file.isFile())return null;
        WatchSelfUpdateClient.verifyIdentity(context,file,code);
        return FileProvider.getUriForFile(context,context.getPackageName()+".updates",file);
    }
    private static void notifyReady(Context context,long code){
        NotificationManager nm=(NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)return;
        nm.createNotificationChannel(new NotificationChannel(
            CHANNEL,"Sanftwecker Update",NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent click=PendingIntent.getActivity(context,46,
            new Intent(context,WatchSelfUpdateActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notice=new Notification.Builder(context,CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Watch-Update v"+code+" bereit")
            .setContentText("Geprüft. Installation auf der Uhr bestätigen")
            .setContentIntent(click).setAutoCancel(true).build();
        try{nm.notify(4600,notice);}catch(SecurityException ignored){}
    }
}
