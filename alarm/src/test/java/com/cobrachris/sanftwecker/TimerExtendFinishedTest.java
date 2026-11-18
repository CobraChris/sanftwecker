package com.cobrachris.sanftwecker;
import org.junit.Test;
import static org.junit.Assert.*;

public class TimerExtendFinishedTest {
    @Test public void expiredTimerCanExtendWithoutChangingItsOriginalPreset(){
        TimerEntry before=TimerEntry.create(7,"Rekord",25_000)
                .start(1_000_000,100_000).finish();
        TimerEntry extended=before.extendFinished(60_000,1_025_000,125_000);
        assertEquals(TimerEntry.RUNNING,extended.state);
        assertEquals(1_085_000,extended.dueWallMs);
        assertEquals(60_000,extended.remainingMs);
        assertEquals(25_000,extended.originalDurationMs);
        assertEquals(25_000,extended.reset().durationMs);
        assertEquals(25_000,extended.restartNow(2_000_000,200_000).remainingMs);
        assertEquals(TimerEntry.FINISHED,before.state);
    }
    @Test public void preservesOriginalDurationThroughPauseFavoriteAndFinish(){
        TimerEntry first=TimerEntry.create(7,"Rekord",25_000).start(1000,1000).finish();
        TimerEntry t=first.extendFinished(300_000,26_000,26_000)
                .favorite(true).pause(27_000,27_000);
        assertEquals(25_000,t.originalDurationMs);
        assertTrue(t.favorite);
        assertEquals(25_000,t.reset().durationMs);
    }
    @Test public void rejectsExtensionOfRunningOrEmptyTimers(){
        TimerEntry t=TimerEntry.create(5,"X",20_000);
        assertThrows(IllegalArgumentException.class,()->t.extendFinished(30_000,500,500));
        TimerEntry finished=t.start(1000,1000).finish();
        assertThrows(IllegalArgumentException.class,()->finished.extendFinished(0,1000,1000));
        assertThrows(IllegalArgumentException.class,()->finished.extendFinished(86_400_001,1000,1000));
    }
}
