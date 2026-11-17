package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

/** Phone alarm overrun must not reset as the display ticks. */
public final class TimerRingClockTest {
    @Test public void monotonicClockWinsAgainstWallTimeEdits(){
        long dueWall=1_800_000_000_000L;
        long dueElapsed=500_000L;
        long offset=dueWall-dueElapsed;
        assertEquals(0L,TimerRingClock.elapsed(
                dueWall,dueElapsed,offset,dueWall,dueElapsed));
        assertEquals(14_000L,TimerRingClock.elapsed(
                dueWall,dueElapsed,offset,dueWall+14_000L,dueElapsed+14_000L));
        assertEquals(14_000L,TimerRingClock.elapsed(
                dueWall,dueElapsed,offset,dueWall+60_000L,dueElapsed+14_000L));
    }
    @Test public void rebootOrMissingMonotonicDeadlineFallsBackToWallClock(){
        long due=1_800_000_000_000L;
        assertEquals(25_000L,TimerRingClock.elapsed(
                due,500_000L,due-500_000L,due+25_000L,1_000L));
        assertEquals(11_000L,TimerRingClock.elapsed(
                due,0,0,due+11_000L,4_000L));
        assertEquals(0L,TimerRingClock.elapsed(
                due,0,0,due-1000L,4_000L));
        assertEquals(0L,TimerRingClock.elapsed(
                0,0,0,due+100_000L,10_000L));
    }
    @Test public void originalOccurrenceSurvivesRestartedCountdown(){
        TimerRingSession session=new TimerRingSession();
        long due=1_800_000_000_000L,offset=due-500_000L;
        session.add(20,"Wiederholung",true,true,500_000L,
                due,500_000L,offset);
        assertEquals(20,session.firstId());
        assertEquals(due,session.firstDueWallMs());
        assertEquals(500_000L,session.firstDueElapsedMs());
        assertEquals(offset,session.firstBootOffsetMs());
        assertEquals(18_000L,TimerRingClock.elapsed(session.firstDueWallMs(),
                session.firstDueElapsedMs(),session.firstBootOffsetMs(),
                due+18_000L,518_000L));
        session.remove(20);
        assertEquals(0,session.firstId());
        assertEquals(0L,session.firstDueWallMs());
    }
    @Test public void elapsedFormattingUsesExistingUnifiedDisplayPolicy(){
        assertEquals("+00:00:03",
                TimerDisplay.overrun(3000L,TimerAlertSettings.FORMAT_HMS));
        assertEquals("+00:18",TimerDisplay.overrun(18_000L,
                TimerAlertSettings.FORMAT_MS));
        assertEquals("+00:59:59",TimerDisplay.overrun(3_599_000L,
                TimerAlertSettings.FORMAT_HMS));
    }
}
