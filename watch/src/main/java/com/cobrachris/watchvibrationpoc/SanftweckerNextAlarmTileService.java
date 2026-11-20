package com.cobrachris.watchvibrationpoc;

import android.content.Intent;
import android.content.SharedPreferences;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.wear.tiles.TileService;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.protolayout.ResourceBuilders;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.ModifiersBuilders;
import androidx.wear.protolayout.ActionBuilders;
import androidx.wear.protolayout.TimelineBuilders;
import com.google.common.util.concurrent.ListenableFuture;
import java.text.DateFormat;
import java.util.Date;

/**
 * Minimal, read-only upcoming alarm tile with safely scoped active-alarm
 * controls. Reimplemented for Sanftwecker from the architecture documented
 * by AlarmClockXtreme (Apache-2.0): never replaces existing alarm services.
 */
public final class SanftweckerNextAlarmTileService extends TileService {
    private static final String CLICK_STOP="stop_current_alarm";
    private static final String CLICK_SNOOZE="snooze_current_alarm";

    private static LayoutElementBuilders.Text line(String text){
        return new LayoutElementBuilders.Text.Builder().setText(text).build();
    }
    private static LayoutElementBuilders.Text action(String text,String clickId){
        ModifiersBuilders.Clickable click=new ModifiersBuilders.Clickable.Builder()
            .setId(clickId)
            .setOnClick(new ActionBuilders.LoadAction.Builder().build())
            .build();
        return new LayoutElementBuilders.Text.Builder().setText(text)
            .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                .setClickable(click).build()).build();
    }
    @Override protected ListenableFuture<TileBuilders.Tile> onTileRequest(
            RequestBuilders.TileRequest request){
        String clicked=request.getCurrentState().getLastClickableId();
        SharedPreferences active=getSharedPreferences(
                WatchAlarmService.ACTIVE_PREF,MODE_PRIVATE);
        long when=active.getLong("when",0);
        int id=active.getInt("id",0);
        int snooze=active.getInt("snooze_minutes",0);
        boolean firing=id>0&&when>0&&
                System.currentTimeMillis()>=when-15_000L&&
                System.currentTimeMillis()<when+15*60_000L;
        if(firing && (CLICK_STOP.equals(clicked)||CLICK_SNOOZE.equals(clicked))){
            if(CLICK_STOP.equals(clicked)||snooze>0){
                Intent action=new Intent(this,WatchAlarmService.class)
                    .setAction(CLICK_STOP.equals(clicked)?
                            WatchAlarmService.STOP:WatchAlarmService.SNOOZE)
                    .putExtra("id",id).putExtra("when",when).putExtra("user",true);
                try{startService(action);}catch(RuntimeException ignored){}
            }
        }
        long next=WatchScheduleStore.next(this);
        String date=next>0?DateFormat.getTimeInstance(DateFormat.SHORT)
                .format(new Date(next)):"Kein Wecker";
        LayoutElementBuilders.Column.Builder column=new LayoutElementBuilders.Column.Builder()
            .addContent(line("SANFTWECKER"))
            .addContent(line(firing?"Wecker klingelt":"Nächster Wecker"))
            .addContent(line(firing?"Aktiv":date));
        if(firing){
            column.addContent(action("Stoppen",CLICK_STOP));
            if(snooze>0)column.addContent(action("Schlummern",CLICK_SNOOZE));
        }else column.addContent(line("Öffne Sanftwecker für Details"));
        TileBuilders.Tile tile=new TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(60_000L)
            .setTileTimeline(new TimelineBuilders.Timeline.Builder()
                .addTimelineEntry(new TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(new LayoutElementBuilders.Layout.Builder()
                        .setRoot(column.build()).build()).build()).build()).build();
        ResolvableFuture<TileBuilders.Tile> result=ResolvableFuture.create();
        result.set(tile);
        return result;
    }
    @Override protected ListenableFuture<ResourceBuilders.Resources> onTileResourcesRequest(
            RequestBuilders.ResourcesRequest request){
        ResolvableFuture<ResourceBuilders.Resources> result=ResolvableFuture.create();
        result.set(new ResourceBuilders.Resources.Builder().setVersion("1").build());
        return result;
    }
}