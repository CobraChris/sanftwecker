package com.cobrachris.watchvibrationpoc;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WatchCascadeTest {
    private static final long WHEN=1_800_000_000_000L;
    @Test public void switchesAtSameAbsoluteMinuteAsPhone(){
        assertFalse(WatchCascade.strong(WHEN,WHEN));
        assertFalse(WatchCascade.strong(WHEN,WHEN+59_999L));
        assertTrue(WatchCascade.strong(WHEN,WHEN+60_000L));
        assertTrue(WatchCascade.strong(WHEN,WHEN+90_000L));
        assertEquals(60_000L,WatchCascade.untilStrong(WHEN,WHEN));
        assertEquals(1L,WatchCascade.untilStrong(WHEN,WHEN+59_999L));
        assertEquals(0L,WatchCascade.untilStrong(WHEN,WHEN+60_000L));
    }
    @Test public void lateWatchStartEscalatesImmediately(){
        assertTrue(WatchCascade.strong(WHEN,WHEN+65_000));
        assertEquals(0,WatchCascade.untilStrong(WHEN,WHEN+65_000));
    }
    @Test public void weakPulseIsShortSparseAndLowerAmplitude(){
        long[] a=WatchCascade.gentleTimings(),b=WatchCascade.strongTimings();
        int[] weak=WatchCascade.gentleAmplitudes();
        int[] strong=WatchCascade.strongAmplitudes();
        assertEquals(a.length,weak.length);
        assertEquals(b.length,strong.length);
        assertTrue(a[1]<b[1]);
        assertTrue(a[2]>=8000);
        assertTrue(weak[1]>0&&weak[1]<strong[1]);
        assertEquals(255,strong[1]);
    }
    @Test public void oldThreeFieldPacketsKeepFullStrength(){
        WatchAlarmProtocol.Packet old=WatchAlarmProtocol.parse("1:1794500000000:5");
        assertNotNull(old);
        assertFalse(old.cascade);
        WatchAlarmProtocol.Packet cascade=WatchAlarmProtocol.parse("1:1794500000000:5:1");
        assertNotNull(cascade);
        assertTrue(cascade.cascade);
        assertNull(WatchAlarmProtocol.parse("1:1794500000000:5:0"));
        assertNull(WatchAlarmProtocol.parse("1:1794500000000:5:2"));
    }
    @Test public void sanftIsShorterAndSparserButStillEscalates(){
        long[] weak=WatchCascade.gentleTimings(WatchCascade.SANFT);
        long[] strong=WatchCascade.strongTimings(WatchCascade.SANFT);
        assertTrue(weak[1]<WatchCascade.gentleTimings()[1]);
        assertTrue(weak[2]>WatchCascade.gentleTimings()[2]);
        assertTrue(strong[1]<WatchCascade.strongTimings()[1]);
        assertTrue(strong[2]>WatchCascade.strongTimings()[2]);
        assertTrue(strong[1]>weak[1]);
        assertTrue(strong[2]<weak[2]);
        assertTrue(WatchCascade.gentleAmplitudes(WatchCascade.SANFT)[1]<
                WatchCascade.gentleAmplitudes()[1]);
        assertTrue(WatchCascade.strongAmplitudes(WatchCascade.SANFT)[1]<
                WatchCascade.strongAmplitudes()[1]);
    }
    @Test public void legacyNormalPatternIsByteForByteUnchanged(){
        assertArrayEquals(new long[]{0L,110L,9000L},WatchCascade.gentleTimings(WatchCascade.NORMAL));
        assertArrayEquals(new int[]{0,48,0},WatchCascade.gentleAmplitudes(WatchCascade.NORMAL));
        assertArrayEquals(new long[]{0L,850L,120L,850L,120L,850L,250L,850L,400L},
                WatchCascade.strongTimings(WatchCascade.NORMAL));
        assertArrayEquals(new int[]{0,255,0,255,0,255,0,255,0},
                WatchCascade.strongAmplitudes(WatchCascade.NORMAL));
        assertEquals(WatchCascade.NORMAL,WatchCascade.safeIntensity(-1));
        assertEquals(WatchCascade.NORMAL,WatchCascade.safeIntensity(9));
    }
    @Test public void allPatternsHaveSafeMatchingTimingAndAmplitudeCounts(){
        for(int intensity=0;intensity<=2;intensity++){
            assertEquals(WatchCascade.gentleTimings(intensity).length,
                    WatchCascade.gentleAmplitudes(intensity).length);
            assertEquals(WatchCascade.strongTimings(intensity).length,
                    WatchCascade.strongAmplitudes(intensity).length);
        }
    }

}
