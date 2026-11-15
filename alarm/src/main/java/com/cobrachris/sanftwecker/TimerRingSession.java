package com.cobrachris.sanftwecker;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Independent, finite timer alert sessions. One active timer never overwrites
 * or silently acknowledges another timer. No wake-alarm state is accessed.
 */
public final class TimerRingSession {
    public static final long MAX_RING_MS=3L*60_000L;
    private static final class Entry {
        final String name;
        final boolean tone,vibration;
        final long expiresAt;
        Entry(String name,boolean tone,boolean vibration,long expiresAt){
            this.name=name;this.tone=tone;this.vibration=vibration;
            this.expiresAt=expiresAt;
        }
    }
    private final LinkedHashMap<Integer,Entry> active=new LinkedHashMap<>();

    public void add(int id,String name,boolean tone,boolean vibration,long elapsedNow){
        if(id<1||id>9999||elapsedNow<0)
            throw new IllegalArgumentException("Invalid timer alarm");
        if(name==null||name.trim().isEmpty())name="Timer";
        // Remove/reinsert if the same recurring countdown completes again.
        active.remove(id);
        active.put(id,new Entry(name,tone,vibration,elapsedNow+MAX_RING_MS));
    }
    public void remove(int id){ active.remove(id); }
    public void removeAll(){ active.clear(); }
    public boolean expire(long elapsedNow){
        boolean changed=false;
        for(Iterator<Map.Entry<Integer,Entry>> it=active.entrySet().iterator();
                it.hasNext();){
            if(elapsedNow>=it.next().getValue().expiresAt){
                it.remove();changed=true;
            }
        }
        return changed;
    }
    public int count(){return active.size();}
    public boolean hasAlerts(){return !active.isEmpty();}
    public boolean wantsTone(){
        for(Entry item:active.values())if(item.tone)return true;
        return false;
    }
    public boolean wantsVibration(){
        for(Entry item:active.values())if(item.vibration)return true;
        return false;
    }
    public String label(){
        if(active.isEmpty())return "Timer";
        if(active.size()>1)return active.size()+" Timer abgelaufen";
        return active.values().iterator().next().name;
    }
}
