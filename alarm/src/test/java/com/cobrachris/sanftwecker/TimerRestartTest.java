package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

/** Pure timer state regression tests for the new one-tap restart button. */
public final class TimerRestartTest {
    private static final long WALL=1_800_000_000_000L;
    private static final long ELAPSED=250_000L;
    private static final long DURATION=120_000L;

    @Test public void runningTimerRestartsAtFullOriginalDuration(){
        TimerEntry old=TimerEntry.create(1,"Training",DURATION)
                .favorite(true).start(WALL,ELAPSED);
        TimerEntry newer=old.restartNow(WALL+45_000L,ELAPSED+45_000L);
        assertEquals(TimerEntry.RUNNING,newer.state);
        assertEquals(DURATION,newer.durationMs);
        assertEquals(DURATION,newer.remaining(WALL+45_000L,ELAPSED+45_000L));
        assertEquals(WALL+45_000L+DURATION,newer.dueWallMs);
        assertEquals(1,newer.id);
        assertEquals("Training",newer.name);
        assertTrue(newer.favorite);
        assertEquals(WALL+DURATION,old.dueWallMs);
        assertNotEquals(old.dueWallMs,newer.dueWallMs);
    }

    @Test public void restartSkipsConfiguredStartDelayWhenWaiting(){
        TimerEntry waiting=TimerEntry.create(2,"Dehnen",DURATION)
                .startDelayed(WALL,ELAPSED,30_000L);
        TimerEntry now=waiting.restartNow(WALL+10_000L,ELAPSED+10_000L);
        assertEquals(TimerEntry.RUNNING,now.state);
        assertEquals(0,now.delayRemaining(WALL+10_000L,ELAPSED+10_000L));
        assertEquals(DURATION,now.remaining(WALL+10_000L,ELAPSED+10_000L));
        assertEquals(WALL+10_000L+DURATION,now.dueWallMs);
        assertEquals(WALL+30_000L+DURATION,waiting.dueWallMs);
    }

    @Test public void pausedTimerDoesNotResumeFromLeftoverTime(){
        TimerEntry paused=TimerEntry.create(3,"Workout",DURATION)
                .start(WALL,ELAPSED).pause(WALL+80_000L,ELAPSED+80_000L);
        assertEquals(40_000L,paused.remainingMs);
        TimerEntry restarted=paused.restartNow(WALL+81_000L,ELAPSED+81_000L);
        assertEquals(TimerEntry.RUNNING,restarted.state);
        assertEquals(DURATION,restarted.remainingMs);
        assertEquals(WALL+81_000L+DURATION,restarted.dueWallMs);
    }

    @Test public void readyAndFinishedAreStartedDirectly(){
        TimerEntry ready=TimerEntry.create(4,"Ei",30_000L);
        TimerEntry finished=ready.start(WALL,ELAPSED).finish();
        for(TimerEntry old:new TimerEntry[]{ready,finished}){
            TimerEntry restarted=old.restartNow(WALL+31_000L,ELAPSED+31_000L);
            assertEquals(TimerEntry.RUNNING,restarted.state);
            assertEquals(30_000L,restarted.remainingMs);
            assertEquals(WALL+61_000L,restarted.dueWallMs);
        }
    }

    @Test public void pressingAgainReplacesOnlyTheSelectedCountdown(){
        TimerEntry first=TimerEntry.create(5,"Erster",DURATION).start(WALL,ELAPSED);
        TimerEntry second=TimerEntry.create(6,"Zweiter",30_000L).start(WALL,ELAPSED);
        TimerEntry rearmed=first.restartNow(WALL+10_000L,ELAPSED+10_000L)
                .restartNow(WALL+11_000L,ELAPSED+11_000L);
        assertEquals(WALL+11_000L+DURATION,rearmed.dueWallMs);
        assertEquals(WALL+30_000L,second.dueWallMs);
        assertEquals(6,second.id);
        assertEquals(TimerEntry.RUNNING,second.state);
    }

    @Test public void oldAlarmDeadlineNoLongerMatchesRestartedTimer(){
        TimerEntry initial=TimerEntry.create(7,"Sicher",60_000L).start(WALL,ELAPSED);
        TimerEntry restarted=initial.restartNow(WALL+10_000L,ELAPSED+10_000L);
        assertNotEquals(initial.dueWallMs,restarted.dueWallMs);
        assertEquals(WALL+70_000L,restarted.dueWallMs);
        assertEquals(60_000L,restarted.remaining(WALL+10_000L,ELAPSED+10_000L));
    }
}
