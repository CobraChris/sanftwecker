package com.cobrachris.watchvibrationpoc;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import androidx.wear.tiles.TileService;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester;

/** Optional watch presentation refresh, independent of exact alarm scheduling. */
public final class WatchAlarmPresentation {
    private WatchAlarmPresentation(){}
    public static void changed(Context context){
        Context app=context.getApplicationContext();
        try{
            TileService.getUpdater(app).requestUpdate(SanftweckerNextAlarmTileService.class);
        }catch(RuntimeException error){
            Log.w("SanftweckerWatch","Tile update deferred",error);
        }
        try{
            ComplicationDataSourceUpdateRequester.create(app,
                new ComponentName(app,SanftweckerNextAlarmComplicationService.class))
                .requestUpdateAll();
        }catch(RuntimeException error){
            Log.w("SanftweckerWatch","Watch-face update deferred",error);
        }
    }
}