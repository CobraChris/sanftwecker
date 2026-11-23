package com.cobrachris.sanftwecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Android can revoke full-screen intent special access. This notification does
 * not attempt to bypass Android: one tap opens the existing guided setup screen.
 */
public final class PostUpdatePermissionNotice {
    private static final String CHANNEL="sanftwecker_after_update_permission_v1";
    private static final int ID=604;
    private PostUpdatePermissionNotice(){}

    public static void showIfMissing(Context c){
        if(Build.VERSION.SDK_INT<34||FullScreenPermissionStatus.isAllowed(c)){
            clear(c);return;
        }
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null||!nm.areNotificationsEnabled())return;
        NotificationChannel channel=new NotificationChannel(CHANNEL,
                "Einrichtung nach App-Update",NotificationManager.IMPORTANCE_DEFAULT);
        channel.setSound(null,null);
        channel.enableVibration(false);
        channel.setDescription("Nur falls eine wichtige Android-Freigabe tatsächlich fehlt");
        nm.createNotificationChannel(channel);
        PendingIntent open=PendingIntent.getActivity(c,604,
                new Intent(c,SetupAssistantActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                                  Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification note=new Notification.Builder(c,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Sanftwecker: Klingelbildschirm-Freigabe fehlt")
                .setContentText("Android hat nach dem Update keinen Vollbildzugriff. Jetzt prüfen.")
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(open).setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true).setLocalOnly(true).build();
        nm.notify(ID,note);
    }

    public static void clear(Context c){
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm!=null)nm.cancel(ID);
    }
}
