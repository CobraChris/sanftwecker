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
        final long dueWallMs,dueElapsedMs,bootOffsetMs;
        Entry(String name,boolean tone,boolean vibration,long expiresAt,
                long dueWallMs,long dueElapsedMs,long bootOffsetMs){
            this.name=name;this.tone=tone;this.vibration=vibration;
            this.expiresAt=expiresAt;
            this.dueWallMs=dueWallMs;
            this.dueElapsedMs=dueElapsedMs;
            this.bootOffsetMs=bootOffsetMs;
        }
    }
    private final LinkedHashMap<Integer,Entry> active=new LinkedHashMap<>();

    public void add(int id,String name,boolean tone,boolean vibration,long elapsedNow){
        add(id,name,tone,vibration,elapsedNow,0,0,0);
    }
    /** Snapshot the original due time: a repeating timer may already have
     * restarted before its previous completion screen is displayed. */
    public void add(int id,String name,boolean tone,boolean vibration,long elapsedNow,
            long dueWallMs,long dueElapsedMs,long bootOffsetMs){
        if(id<1||id>9999||elapsedNow<0)
            throw new IllegalArgumentException("Invalid timer alarm");
        if(name==null||name.trim().isEmpty())name="Timer";
        active.remove(id);
        active.put(id,new Entry(name,tone,vibration,elapsedNow+MAX_RING_MS,
                dueWallMs,dueElapsedMs,bootOffsetMs));
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
    public int firstId(){return active.isEmpty()?0:active.keySet().iterator().next();}
    public long firstDueWallMs(){
        return active.isEmpty()?0:active.values().iterator().next().dueWallMs;
    }
    public long firstDueElapsedMs(){
        return active.isEmpty()?0:active.values().iterator().next().dueElapsedMs;
    }
    public long firstBootOffsetMs(){
        return active.isEmpty()?0:active.values().iterator().next().bootOffsetMs;
    }
    public boolean contains(int id){return active.containsKey(id);}
    public int[] ids(){
        int[] result=new int[active.size()];
        int i=0;
        for(Integer id:active.keySet())result[i++]=id;
        return result;
    }
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
