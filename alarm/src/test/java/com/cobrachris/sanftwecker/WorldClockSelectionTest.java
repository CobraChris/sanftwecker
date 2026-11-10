package com.cobrachris.sanftwecker;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import static org.junit.Assert.*;

public final class WorldClockSelectionTest {
    @Test public void defaultsFavorHamburgAndTokyo(){
        List<String> defaults=WorldClockSelection.defaults();
        assertEquals("Europe/Berlin#hamburg",defaults.get(0));
        assertNotNull(WorldClockCatalog.get(defaults.get(0)));
        assertEquals("Hamburg",WorldClockCatalog.get(defaults.get(0)).name);
        assertEquals("Asia/Tokyo",defaults.get(1));
        assertEquals(4,defaults.size());
    }
    @Test public void canSelectDifferentCitiesSharingTimeZone(){
        WorldClockCatalog.City hamburg=WorldClockCatalog.get("Europe/Berlin#hamburg");
        WorldClockCatalog.City berlin=WorldClockCatalog.get("Europe/Berlin");
        assertNotNull(hamburg);assertNotNull(berlin);
        assertEquals("Europe/Berlin",hamburg.zone);
        assertEquals(berlin.zone,hamburg.zone);
        List<String> out=WorldClockSelection.add(
                Arrays.asList(hamburg.id),berlin.id);
        assertEquals(2,out.size());
    }
    @Test public void selectionIsOrderedUniqueAndRejectsInvalidIds(){
        List<String> clean=WorldClockSelection.sanitize(Arrays.asList(
                "Asia/Tokyo",null,"Missing/Zone","Asia/Tokyo",
                "Europe/London","Europe/Berlin#hamburg"));
        assertEquals(Arrays.asList("Asia/Tokyo","Europe/London",
                "Europe/Berlin#hamburg"),clean);
        assertEquals(clean,WorldClockSelection.add(clean,"Asia/Tokyo"));
        assertEquals(Arrays.asList("Europe/London","Asia/Tokyo",
                "Europe/Berlin#hamburg"),
                WorldClockSelection.move(clean,"Europe/London",-1));
        assertEquals(clean,WorldClockSelection.move(clean,"Asia/Tokyo",-1));
        assertEquals(Arrays.asList("Asia/Tokyo","Europe/Berlin#hamburg"),
                WorldClockSelection.remove(clean,"Europe/London"));
    }
    @Test public void filtersAvailableCitiesByHumanNameAndIanaName(){
        assertTrue(WorldClockCatalog.search("hamburg",Collections.emptySet(),25)
                .stream().anyMatch(x->x.name.equals("Hamburg")));
        assertTrue(WorldClockCatalog.search("münchen",Collections.emptySet(),25)
                .stream().anyMatch(x->x.name.equals("München")));
        assertTrue(WorldClockCatalog.search("munchen",Collections.emptySet(),25)
                .stream().anyMatch(x->x.name.equals("München")));
        assertTrue(WorldClockCatalog.search("tokyo",Collections.emptySet(),25)
                .stream().anyMatch(x->x.name.equals("Tokio")));
        assertTrue(WorldClockCatalog.search("buenos_aires",Collections.emptySet(),25)
                .stream().anyMatch(x->x.name.equals("Buenos Aires")));
        assertFalse(WorldClockCatalog.search("asia/tokyo",
                new HashSet<>(Collections.singletonList("Asia/Tokyo")),25)
                .stream().anyMatch(x->x.id.equals("Asia/Tokyo")));
        assertFalse(WorldClockCatalog.isSelectable("Etc/GMT+12"));
    }
    @Test public void listIsBoundedTo24AndCannotAddUnknownZone(){
        List<String> choices=new ArrayList<>();
        for(WorldClockCatalog.City city:WorldClockCatalog.search("",null,50)){
            if(choices.size()>=WorldClockSelection.MAX_CITIES)break;
            choices=WorldClockSelection.add(choices,city.id);
        }
        assertEquals(24,choices.size());
        try{WorldClockSelection.add(choices,"America/Chicago");fail();}
        catch(IllegalStateException expected){}
        try{WorldClockSelection.add(choices,"../../password");fail();}
        catch(IllegalArgumentException expected){}
    }
}
