package com.cobrachris.watchvibrationpoc;

import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.NoDataComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;
import java.text.DateFormat;
import java.util.Date;

/** Optional read-only next alarm on supported Wear OS watch faces. */
public final class SanftweckerNextAlarmComplicationService
        extends ComplicationDataSourceService {
    @Override public void onComplicationRequest(ComplicationRequest request,
            ComplicationRequestListener listener){
        try{
            listener.onComplicationData(data(request.getComplicationType()));
        }catch(android.os.RemoteException error){
            android.util.Log.w("SanftweckerWatch","Complication update deferred",error);
        }
    }
    @Override public ComplicationData getPreviewData(ComplicationType type){
        return data(type);
    }
    private PlainComplicationText text(String value){
        return new PlainComplicationText.Builder(value).build();
    }
    private ComplicationData data(ComplicationType type){
        long next=WatchScheduleStore.next(this);
        String display=next>0?DateFormat.getTimeInstance(DateFormat.SHORT)
            .format(new Date(next)):"–";
        if(type==ComplicationType.SHORT_TEXT)
            return new ShortTextComplicationData.Builder(text(display),
                text("Nächster Sanftwecker-Alarm")).setTitle(text("Wecker")).build();
        if(type==ComplicationType.LONG_TEXT)
            return new LongTextComplicationData.Builder(
                text(next>0?"Nächster Wecker: "+display:"Kein Wecker geplant"),
                text("Nächster Sanftwecker-Alarm")).build();
        return new NoDataComplicationData();
    }
}