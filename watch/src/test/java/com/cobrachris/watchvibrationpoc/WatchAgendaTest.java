package com.cobrachris.watchvibrationpoc;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Never modifies or acknowledges the production local alarm schedule. */
public final class WatchAgendaTest {
    private static final long NOW=1_800_000_000_000L;

    private static void plan(Map<String,Object> m,int slot,int id,long when,String name){
        m.put("slot_"+slot+"_when",when);
        m.put("slot_"+slot+"_id",id);
        m.put("slot_"+slot+"_name",name);
    }
    @Test public void upcomingOnlyAndChronologicalWithSameTimestampStableOrder(){
        Map<String,Object> saved=new HashMap<>();
        plan(saved,3,3,NOW+70_000L,"Sport");
        plan(saved,2,2,NOW+20_000L,"Arbeit");
        plan(saved,1,1,NOW+20_000L,"Früh");
        plan(saved,4,4,NOW-1000L,"Verpasst");
        List<WatchAgenda.Entry> rows=WatchAgenda.upcoming(saved,NOW);
        assertEquals(3,rows.size());
        assertEquals(1,rows.get(0).slot);
        assertEquals(2,rows.get(1).slot);
        assertEquals(3,rows.get(2).slot);
        assertEquals("Arbeit",rows.get(1).name);
        assertEquals(NOW+20_000L,rows.get(0).when);
    }
    @Test public void corruptedOrDisabledSlotsCannotAppearAsScheduled(){
        Map<String,Object> saved=new HashMap<>();
        plan(saved,1,1,0,"Deaktiviert");
        plan(saved,2,2,NOW-1L,"Vergangen");
        plan(saved,3,2,NOW+1L,"Zukunft");
        plan(saved,4,1001,NOW+2L,"Ungültige ID");
        plan(saved,5,3,NOW+3L," ");
        saved.put("slot_6_when",NOW+4L); // no corresponding ID
        saved.put("slot_7_when","tomorrow"); // malformed persisted value
        saved.put("slot_8_when",NOW+5L);
        saved.put("slot_8_id","malformed");
        saved.put("slot_1003_when",NOW+10L);
        saved.put("slot_1003_id",1);
        List<WatchAgenda.Entry> rows=WatchAgenda.upcoming(saved,NOW);
        assertEquals(2,rows.size());
        assertEquals("Zukunft",rows.get(0).name);
        assertEquals("Wecker 3",rows.get(1).name);
        assertTrue(WatchAgenda.upcoming(null,NOW).isEmpty());
    }
    @Test public void projectionIsImmutableAndLeavesStorageUntouched(){
        Map<String,Object> saved=new HashMap<>();
        plan(saved,30,18,NOW+120_000L,"Wiederkehrender Test");
        int size=saved.size();
        List<WatchAgenda.Entry> rows=WatchAgenda.upcoming(saved,NOW);
        assertEquals(1,rows.size());
        assertEquals(size,saved.size());
        assertEquals("Wiederkehrender Test",saved.get("slot_30_name"));
        try{rows.clear();fail("Read-only agenda changed");}
        catch(UnsupportedOperationException expected){}
    }
    @Test public void longNamesAreShortenedOnlyForTheWatchDisplay(){
        Map<String,Object> saved=new HashMap<>();
        String longName="ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOP";
        plan(saved,1002,1000,NOW+1000L,longName);
        List<WatchAgenda.Entry> rows=WatchAgenda.upcoming(saved,NOW);
        assertEquals(60,rows.get(0).name.length());
        assertEquals(longName,saved.get("slot_1002_name"));
    }
}
