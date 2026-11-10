package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Versioned per-device selection. Existing v1 entries remain readable. */
public final class WorldClockStore {
    private static final String PREF="sanftwecker_world_clock_v1";
    private static final String KEY="selected_zones";
    private static final String PLACES="selected_geo_places";
    private WorldClockStore(){}

    public static List<String> load(Context context){
        SharedPreferences prefs=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        // Register saved place/timezone pairs *before* sanitizing the old ordered ID list.
        try {
            JSONArray stored=new JSONArray(prefs.getString(PLACES,"[]"));
            for(int i=0;i<Math.min(stored.length(),WorldClockSelection.MAX_CITIES);i++){
                JSONObject item=stored.optJSONObject(i);
                if(item==null)continue;
                try {
                    WorldClockCatalog.registerOnlineCity(new WorldClockCatalog.City(
                            item.optString("name",""),
                            item.optString("country",""),
                            item.optString("zone",""),
                            item.optString("id","")));
                } catch(IllegalArgumentException ignored) {
                    // Invalid or stale place cannot break the alarm application.
                }
            }
        } catch(JSONException ignored) {
            // A damaged optional cache must not lose the original clock selection.
        }
        if(!prefs.contains(KEY))return WorldClockSelection.sanitize(WorldClockSelection.defaults());
        List<String> zones=new ArrayList<>();
        try {
            JSONArray array=new JSONArray(prefs.getString(KEY,"[]"));
            int count=Math.min(array.length(),200);
            for(int i=0;i<count;i++) zones.add(array.optString(i,""));
        } catch(JSONException ignored) {
            return WorldClockSelection.sanitize(WorldClockSelection.defaults());
        }
        return WorldClockSelection.sanitize(zones);
    }

    public static void save(Context context,List<String> selection){
        List<String> zones=WorldClockSelection.sanitize(selection);
        JSONArray selectedIds=new JSONArray();
        JSONArray savedPlaces=new JSONArray();
        for(String id:zones){
            selectedIds.put(id);
            if(!id.startsWith("geo:"))continue;
            WorldClockCatalog.City city=WorldClockCatalog.get(id);
            if(city==null)continue;
            try {
                savedPlaces.put(new JSONObject()
                        .put("id",city.id).put("name",city.name)
                        .put("country",city.country).put("zone",city.zone));
            } catch(JSONException error) {
                throw new IllegalStateException("Weltuhr-Ort kann nicht gespeichert werden",error);
            }
        }
        // One atomic preference transaction for both order and custom-place metadata.
        if(!context.getSharedPreferences(PREF,Context.MODE_PRIVATE)
                .edit().putString(KEY,selectedIds.toString())
                .putString(PLACES,savedPlaces.toString()).commit())
            throw new IllegalStateException("Weltuhr konnte nicht gespeichert werden");
    }
}
