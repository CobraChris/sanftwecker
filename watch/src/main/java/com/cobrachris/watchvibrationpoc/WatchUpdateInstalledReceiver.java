package com.cobrachris.watchvibrationpoc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Records the package replacement without touching alarm restoration. */
public final class WatchUpdateInstalledReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(intent!=null&&Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()))
            WatchSelfUpdateFeedback.markReplaced(context);
    }
}