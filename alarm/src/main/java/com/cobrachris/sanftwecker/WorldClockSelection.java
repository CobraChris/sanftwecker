package com.cobrachris.sanftwecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure ordered selection policy, independent of Android storage/UI. */
public final class WorldClockSelection {
    public static final int MAX_CITIES=24;
    private WorldClockSelection(){}

    public static List<String> defaults(){
        return Arrays.asList("Europe/Berlin#hamburg","Asia/Tokyo",
                "Europe/London","America/New_York");
    }

    public static List<String> sanitize(List<String> zones){
        List<String> selected=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        if(zones==null)return selected;
        for(String zone:zones){
            if(selected.size()>=MAX_CITIES)break;
            if(!WorldClockCatalog.isSelectable(zone)||!seen.add(zone))continue;
            selected.add(zone);
        }
        return selected;
    }
    public static List<String> add(List<String> selected,String zone){
        List<String> out=sanitize(selected);
        if(!WorldClockCatalog.isSelectable(zone))
            throw new IllegalArgumentException("Ungültige Zeitzone");
        if(out.contains(zone))return out;
        if(out.size()>=MAX_CITIES)
            throw new IllegalStateException("Maximal 24 Städte möglich");
        out.add(zone);
        return out;
    }
    public static List<String> remove(List<String> selected,String zone){
        List<String> out=sanitize(selected);
        out.remove(zone);
        return out;
    }
    public static List<String> move(List<String> selected,String zone,int offset){
        List<String> out=sanitize(selected);
        int index=out.indexOf(zone),next=index+offset;
        if(index<0||Math.abs(offset)!=1||next<0||next>=out.size())return out;
        Collections.swap(out,index,next);
        return out;
    }
}
