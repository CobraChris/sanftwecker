package com.cobrachris.watchvibrationpoc;

import org.junit.Test;
import static org.junit.Assert.*;

public class WatchAlarmPriorityTest {
    private static final long NOW=1_800_000_000_000L;

    @Test public void activeClockAlarmTakesPriority(){
        assertTrue(WatchAlarmPriority.blocksReminder(3,NOW-60_000L,NOW));
        assertTrue(WatchAlarmPriority.blocksReminder(3,NOW-15*60_000L,NOW));
        assertTrue(WatchAlarmPriority.blocksReminder(3,NOW+60_000L,NOW));
    }

    @Test public void staleAlarmRecordNeverBlocksFutureReminders(){
        assertFalse(WatchAlarmPriority.blocksReminder(3,NOW-15*60_000L-1,NOW));
        assertFalse(WatchAlarmPriority.blocksReminder(3,NOW-24*60*60_000L,NOW));
        assertFalse(WatchAlarmPriority.blocksReminder(3,NOW+60_001L,NOW));
        assertFalse(WatchAlarmPriority.blocksReminder(0,NOW,NOW));
        assertFalse(WatchAlarmPriority.blocksReminder(3,0,NOW));
    }
}
