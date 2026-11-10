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
}
