package com.cobrachris.watchvibrationpoc;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression protection for independently scheduled UI and alarm actions. */
public final class WatchAlarmUiPolicyTest {
    private static final long WHEN=1_800_000_000_000L;

    @Test public void onlyActiveAlarmsExposeStopAndSnooze(){
        assertFalse(WatchAlarmUiPolicy.active(0,WHEN));
        assertFalse(WatchAlarmUiPolicy.active(1,0));
        assertFalse(WatchAlarmUiPolicy.active(1001,WHEN));
        assertTrue(WatchAlarmUiPolicy.active(1,WHEN));
        assertTrue(WatchAlarmUiPolicy.active(1000,WHEN));
        assertFalse(WatchAlarmUiPolicy.snoozeVisible(1,WHEN,0));
        assertFalse(WatchAlarmUiPolicy.snoozeVisible(0,WHEN,5));
        assertFalse(WatchAlarmUiPolicy.snoozeVisible(1,WHEN,31));
        assertTrue(WatchAlarmUiPolicy.snoozeVisible(1,WHEN,5));
        assertTrue(WatchAlarmUiPolicy.snoozeVisible(1,WHEN,30));
    }
    @Test public void watchScreenUsesOriginalMinuteTransition(){
        assertEquals("Phase 1 – leise",WatchAlarmUiPolicy.stage(true,WHEN,WHEN));
        assertEquals("Phase 1 – leise",WatchAlarmUiPolicy.stage(true,WHEN,WHEN+59_999L));
        assertEquals("Phase 2 – deutlich",WatchAlarmUiPolicy.stage(true,WHEN,WHEN+60_000L));
        assertEquals("Alarm aktiv",WatchAlarmUiPolicy.stage(false,WHEN,WHEN+60_000L));
    }
}
