package com.cobrachris.sanftwecker;

import org.junit.Test;
import java.time.Instant;
import java.time.ZoneId;
import static org.junit.Assert.*;

public final class WorldClockTimeTest {
    private static final ZoneId TOKYO=ZoneId.of("Asia/Tokyo");
    private static long timestamp(String iso){
        return Instant.parse(iso).toEpochMilli();
    }

    @Test public void sameInstantAcrossContinentsWithRelativeDay(){
        long now=timestamp("2026-11-10T00:30:00Z");
        WorldClockTime tokyo=WorldClockTime.at("Asia/Tokyo",now,TOKYO);
        assertEquals("09:30",tokyo.clock);
        assertEquals("Heute",tokyo.dayDifference);
        assertEquals("UTC+9",tokyo.utcOffset);
        WorldClockTime losAngeles=WorldClockTime.at("America/Los_Angeles",now,TOKYO);
        assertEquals("16:30",losAngeles.clock);
        assertEquals("Gestern",losAngeles.dayDifference);
        assertEquals(-1L,losAngeles.dateDelta);
        assertEquals("UTC−8",losAngeles.utcOffset);
    }
    @Test public void berlinAdjustsAutomaticallyAtSummerTimeBoundary(){
        long before=timestamp("2026-03-29T00:30:00Z");
        long after=timestamp("2026-03-29T01:30:00Z");
        assertEquals("01:30",WorldClockTime.at("Europe/Berlin",before,TOKYO).clock);
        assertEquals("UTC+1",WorldClockTime.at("Europe/Berlin",before,TOKYO).utcOffset);
        assertEquals("03:30",WorldClockTime.at("Europe/Berlin",after,TOKYO).clock);
        assertEquals("UTC+2",WorldClockTime.at("Europe/Berlin",after,TOKYO).utcOffset);
    }
    @Test public void newYorkRepeatsHourWhenDaylightTimeEnds(){
        long first=timestamp("2026-11-01T05:30:00Z");
        long second=timestamp("2026-11-01T06:30:00Z");
        WorldClockTime a=WorldClockTime.at("America/New_York",first,TOKYO);
        WorldClockTime b=WorldClockTime.at("America/New_York",second,TOKYO);
        assertEquals("01:30",a.clock);
        assertEquals("01:30",b.clock);
        assertEquals("UTC−4",a.utcOffset);
        assertEquals("UTC−5",b.utcOffset);
    }
    @Test public void fractionalUtcOffsetUsesMinutes(){
        WorldClockTime nepal=WorldClockTime.at(
                "Asia/Kathmandu",timestamp("2026-11-10T00:00:00Z"),TOKYO);
        assertEquals("05:45",nepal.clock);
        assertEquals("UTC+5:45",nepal.utcOffset);
    }
    @Test public void yesterdayAndTomorrowAreRelativeToDeviceZone(){
        long when=timestamp("2026-11-10T05:00:00Z");
        WorldClockTime tokyo=WorldClockTime.at("Asia/Tokyo",when,
                ZoneId.of("America/Los_Angeles"));
        assertEquals("Morgen",tokyo.dayDifference);
        assertEquals(1L,tokyo.dateDelta);
    }
}
