package com.cobrachris.sanftwecker;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class AlarmCountdownTest {
    private static final long NOW=1_800_000_000_000L;

    @Test public void hoursAndMinutes() {
        assertEquals("Klingelt in 18 Std. 7 Min.",
                AlarmCountdown.label(NOW+((18*60+7)*60000L), NOW));
        assertEquals("Klingelt in 1 Std. 0 Min.",
                AlarmCountdown.label(NOW+3600000L, NOW));
    }
    @Test public void shortWaitNeverShowsZeroMinutes() {
        assertEquals("Klingelt in 1 Min.",AlarmCountdown.label(NOW+1L,NOW));
        assertEquals("Klingelt in 2 Min.",AlarmCountdown.label(NOW+60001L,NOW));
        assertEquals("Klingelt in 59 Min.",AlarmCountdown.label(NOW+59*60000L,NOW));
    }
    @Test public void staleAndMissingOccurrences() {
        assertEquals("Klingelt jetzt",AlarmCountdown.label(NOW,NOW));
        assertEquals("Klingelt jetzt",AlarmCountdown.label(NOW-10L,NOW));
        assertEquals("Nicht eingeplant",AlarmCountdown.label(0L,NOW));
    }
}
