package com.cobrachris.sanftwecker;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

/** Versioned per-device selection. Persist order and allow safe empty lists. */
public final class WorldClockStore {
    private static final String PREF="sanftwecker_world_clock_v1";
    private static final String KEY="selected_zones";
    private WorldClockStore(){}

    public static List<String> load(Context context){
        SharedPreferences prefs=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        if(!prefs.contains(KEY))return WorldClockSelection.sanitize(WorldClockSelection.defaults());
        List<String> zones=new ArrayList<>();
        try{
            JSONArray array=new JSONArray(prefs.getString(KEY,"[]"));
            int count=Math.min(array.length(),200);
            for(int i=0;i<count;i++){
                String value=array.optString(i,"");
                zones.add(value);
            }
        }catch(JSONException ignored){
            // Damaged preferences must never break the alarm application.
            return WorldClockSelection.sanitize(WorldClockSelection.defaults());
        }
        return WorldClockSelection.sanitize(zones);
    }
    public static void save(Context context,List<String> selection){
        List<String> zones=WorldClockSelection.sanitize(selection);
        JSONArray array=new JSONArray();
        for(String zone:zones)array.put(zone);
        if(!context.getSharedPreferences(PREF,Context.MODE_PRIVATE)
                .edit().putString(KEY,array.toString()).commit())
            throw new IllegalStateException("Weltuhr konnte nicht gespeichert werden");
    }
}
