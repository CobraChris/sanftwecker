package com.cobrachris.sanftwecker;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/** Always trust Android's current special-access state, never an app preference. */
public final class FullScreenPermissionStatus {
    private FullScreenPermissionStatus(){}
    public static boolean isAllowed(Context context){
        if(Build.VERSION.SDK_INT<34)return true;
        NotificationManager manager=(NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        return manager!=null&&manager.canUseFullScreenIntent();
    }
    public static String label(boolean allowed){
        return allowed
                ? "Klingelbildschirm: von Android erlaubt."
                : "Klingelbildschirm: Android-Freigabe fehlt.";
    }
}
