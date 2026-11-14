package com.cobrachris.sanftwecker;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/** Live Android permission facts. Never store or simulate a granted checkbox. */
public final class SetupPermissionStatus {
    private SetupPermissionStatus() {}

    public static boolean exactAlarms(Context context) {
        return AlarmScheduler.canSchedule(context);
    }

    public static boolean notifications(Context context) {
        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager manager=(NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        return manager!=null && manager.areNotificationsEnabled();
    }

    public static boolean fullScreen(Context context) {
        return FullScreenPermissionStatus.isAllowed(context);
    }

    /** Additional permission for in-app APK updates; not needed to ring. */
    public static boolean apkUpdates(Context context) {
        return Build.VERSION.SDK_INT < 26 ||
                context.getPackageManager().canRequestPackageInstalls();
    }

    /** Only core phone alarm capabilities produce an amber warning. */
    public static int missingRequired(Context context) {
        int missing=0;
        if(!exactAlarms(context)) missing++;
        if(!notifications(context)) missing++;
        if(!fullScreen(context)) missing++;
        return missing;
    }

    public static String summary(int missing) {
        if(missing==0) return "Alle drei Handy-Freigaben erteilt";
        return missing==1 ? "1 Handy-Freigabe fehlt" : missing+" Handy-Freigaben fehlen";
    }
}
