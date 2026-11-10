package com.cobrachris.sanftwecker;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

/** Regression coverage for the previously missing Enfield, Connecticut query. */
public final class WorldClockGeocodingTest {
    private static final String RESULTS="{\"results\":["
            +"{\"id\":4833560,\"name\":\"Enfield\",\"admin1\":\"Connecticut\","
            +"\"country\":\"United States\",\"country_code\":\"US\","
            +"\"timezone\":\"America/New_York\"},"
            +"{\"id\":111222333,\"name\":\"Enfield\",\"admin1\":\"Greater London\","
            +"\"country\":\"United Kingdom\",\"country_code\":\"GB\","
            +"\"timezone\":\"Europe/London\"}]}";

    @Test public void stateAbbreviationSearchUsesSupportedQualifier(){
        assertEquals("Enfield, CT",WorldClockGeocoding.qualifiedQuery(" Enfield   ct "));
        assertEquals("Enfield, Connecticut",
                WorldClockGeocoding.qualifiedQuery("Enfield, Connecticut"));
        assertTrue(WorldClockGeocoding.requestUrl("Enfield CT")
                .contains("name=Enfield%2C+CT"));
        assertEquals("São Paulo",WorldClockGeocoding.qualifiedQuery("São Paulo"));
        assertThrows(IllegalArgumentException.class,
                ()->WorldClockGeocoding.requestUrl("a"));
        assertThrows(IllegalArgumentException.class,
                ()->WorldClockGeocoding.requestUrl("x".repeat(101)));
    }

    @Test public void correctlyDisambiguatesConnecticutAndLondonWithRealTimezones() throws Exception {
        List<WorldClockCatalog.City> result=WorldClockGeocoding.parse(RESULTS);
        assertEquals(2,result.size());
        WorldClockCatalog.City ct=result.get(0);
        assertEquals("Enfield",ct.name);
        assertEquals("Connecticut, USA",ct.country);
        assertEquals("geo:4833560",ct.id);
        assertEquals("America/New_York",ct.zone);
        assertEquals("Europe/London",result.get(1).zone);
        assertNotEquals(ct.id,result.get(1).id);
    }

    @Test public void onlinePlaceRemainsSelectableAfterCatalogRegistration() throws Exception {
        WorldClockCatalog.City place=WorldClockGeocoding.parse(RESULTS).get(0);
        WorldClockCatalog.registerOnlineCity(place);
        List<String> selected=WorldClockSelection.add(Collections.emptyList(),place.id);
        assertEquals(Arrays.asList("geo:4833560"),selected);
        assertEquals("America/New_York",
                WorldClockCatalog.get(WorldClockSelection.sanitize(selected).get(0)).zone);
        assertTrue(WorldClockCatalog.search("enfield",Collections.emptySet(),20)
                .stream().anyMatch(x->x.id.equals(place.id)));
    }

    @Test public void invalidZonesAndMalformedLocationsAreDiscarded() throws Exception {
        String json="{\"results\":["
                +"{\"id\":1,\"name\":\"Bad\",\"country\":\"USA\",\"timezone\":\"Invalid/Zone\"},"
                +"{\"id\":2,\"name\":\"Valid\",\"country\":\"USA\",\"timezone\":\"America/New_York\"},"
                +"{\"id\":2,\"name\":\"Duplicate\",\"country\":\"USA\",\"timezone\":\"America/New_York\"}]}";
        assertEquals(1,WorldClockGeocoding.parse(json).size());
        assertTrue(WorldClockGeocoding.parse("{}").isEmpty());
        assertThrows(IllegalArgumentException.class,()->WorldClockCatalog.registerOnlineCity(
                new WorldClockCatalog.City("Unsafe","USA","Bad/Zone","geo:123")));
    }
}
