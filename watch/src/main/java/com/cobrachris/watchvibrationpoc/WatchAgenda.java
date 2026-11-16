package com.cobrachris.watchvibrationpoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Read-only projection of the exact alarms already held on the watch.
 * It does not schedule, cancel, acknowledge or edit any alarm, and does not
 * pretend that disabled phone alarms are present on the watch.
 */
public final class WatchAgenda {
    private WatchAgenda(){}

    public static final class Entry {
        public final int slot;
        public final int id;
        public final long when;
        public final String name;
        Entry(int slot,int id,long when,String name){
            this.slot=slot;
            this.id=id;
            this.when=when;
            this.name=name;
        }
    }

    public static List<Entry> upcoming(Map<String,?> saved,long now){
        if(saved==null||saved.isEmpty())return Collections.emptyList();
        ArrayList<Entry> list=new ArrayList<>();
        for(Map.Entry<String,?> record:saved.entrySet()){
            String key=record.getKey();
            if(key==null||!key.matches("slot_[0-9]{1,4}_when")||
                    !(record.getValue() instanceof Long))continue;
            int slot;
            try{slot=Integer.parseInt(key.substring(5,key.length()-5));}
            catch(NumberFormatException ignored){continue;}
            if(!WatchAlarmProtocol.validSlot(slot))continue;
            long when=(Long)record.getValue();
            if(when<=now)continue;
            Object savedId=saved.get("slot_"+slot+"_id");
            if(!(savedId instanceof Integer))continue;
            int id=(Integer)savedId;
            if(id<1||id>1000)continue;
            Object savedName=saved.get("slot_"+slot+"_name");
            String name=savedName instanceof String?((String)savedName).trim():"";
            if(name.isEmpty())name="Wecker "+id;
            // Prevent an unusually long remote title from clipping round screens.
            if(name.length()>60)name=name.substring(0,60);
            list.add(new Entry(slot,id,when,name));
        }
        list.sort(Comparator.comparingLong((Entry x)->x.when)
                .thenComparingInt(x->x.slot));
        return Collections.unmodifiableList(list);
    }
}
