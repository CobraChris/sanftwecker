package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TimerOverrunTest {
    private static final long WALL=1_800_000_000_000L, ELAPSED=300_000L;

    @Test public void finishedCountdownCountsUpFromItsOriginalDeadline(){
        TimerEntry started=TimerEntry.create(11,"Workout",45_000L).start(WALL,ELAPSED);
        TimerEntry finished=started.finish();
        assertEquals(TimerEntry.FINISHED,finished.state);
        assertEquals(WALL+45_000L,finished.dueWallMs);
        assertEquals(ELAPSED+45_000L,finished.dueElapsedMs);
        assertEquals(0,finished.overrun(WALL+45_000L,ELAPSED+45_000L));
        assertEquals(1_234L,finished.overrun(WALL+46_234L,ELAPSED+46_234L));
        assertEquals("+00:00:01",TimerDisplay.visible(finished,
                WALL+46_234L,ELAPSED+46_234L,TimerAlertSettings.FORMAT_HMS));
        assertEquals("+00:01",TimerDisplay.visible(finished,
                WALL+46_234L,ELAPSED+46_234L,TimerAlertSettings.FORMAT_MS));
        assertEquals("+01:01:01",TimerDisplay.overrun(3_661_444L,
                TimerAlertSettings.FORMAT_HMS));
        assertEquals("+61:01",TimerDisplay.overrun(3_661_444L,
                TimerAlertSettings.FORMAT_MS));
    }
    @Test public void countingUpContinuesAfterTheThreeMinuteAlarmSignalEnds(){
        TimerEntry finished=TimerEntry.create(2,"Tea",10_000).start(WALL,ELAPSED).finish();
        assertEquals("+00:03:30",TimerDisplay.visible(finished,
                WALL+10_000+210_000,ELAPSED+10_000+210_000,
                TimerAlertSettings.FORMAT_HMS));
        assertEquals(210_000,finished.overrun(
                WALL+220_000,ELAPSED+220_000));
    }
    @Test public void rebootFallbackUsesPersistentWallDeadline(){
        TimerEntry finished=TimerEntry.create(3,"Egg",60_000).start(WALL,ELAPSED).finish();
        assertEquals(120_000L,finished.overrun(WALL+180_000,5_000L));
    }
    @Test public void resetRestartAndOldVersionFinishedEntriesRemainSafe(){
        TimerEntry finished=TimerEntry.create(4,"Coffee",15_000).start(WALL,ELAPSED).finish();
        assertEquals(0,finished.reset().overrun(WALL+40_000,ELAPSED+40_000));
        TimerEntry restarted=finished.restartNow(WALL+40_000,ELAPSED+40_000);
        assertEquals(TimerEntry.RUNNING,restarted.state);
        assertEquals("00:00:15",TimerDisplay.visible(restarted,WALL+40_000,
                ELAPSED+40_000,TimerAlertSettings.FORMAT_HMS));
        TimerEntry legacy=new TimerEntry(5,"Old",15_000,0,
                TimerEntry.FINISHED,0,0,0,false);
        assertEquals(0,legacy.overrun(WALL+100_000,ELAPSED+100_000));
        assertEquals("00:00:00",TimerDisplay.visible(legacy,WALL+100_000,
                ELAPSED+100_000,TimerAlertSettings.FORMAT_HMS));
    }
    @Test public void activeTimersStillCountDownNormally(){
        TimerEntry running=TimerEntry.create(6,"Workout",120_000)
                .start(WALL,ELAPSED);
        assertEquals("00:01:55",TimerDisplay.visible(running,WALL+5_000,
                ELAPSED+5_000,TimerAlertSettings.FORMAT_HMS));
        assertEquals(0,running.overrun(WALL+150_000,ELAPSED+150_000));
    }
}
