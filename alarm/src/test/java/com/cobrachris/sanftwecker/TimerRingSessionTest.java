package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

/** Pure unit tests: no phone, audio hardware or notification permission assumed. */
public final class TimerRingSessionTest {
    @Test public void defaultTimerRingsUntilStoppedOrThreeMinutesPass(){
        TimerRingSession session=new TimerRingSession();
        long now=24_000L;
        session.add(1,"Kaffee",true,true,now);
        assertEquals("Kaffee",session.label());
        assertEquals(1,session.count());
        assertTrue(session.wantsTone());
        assertTrue(session.wantsVibration());
        assertFalse(session.expire(now+TimerRingSession.MAX_RING_MS-1L));
        assertTrue(session.hasAlerts());
        assertTrue(session.expire(now+TimerRingSession.MAX_RING_MS));
        assertFalse(session.hasAlerts());
    }

    @Test public void oneFinishedTimerDoesNotReplaceAnother(){
        TimerRingSession session=new TimerRingSession();
        session.add(1,"Ei",true,false,0L);
        session.add(2,"Tee",false,true,10_000L);
        assertEquals(2,session.count());
        assertEquals("2 Timer abgelaufen",session.label());
        assertTrue(session.wantsTone());
        assertTrue(session.wantsVibration());
        session.remove(1);
        assertEquals(1,session.count());
        assertEquals("Tee",session.label());
        assertFalse(session.wantsTone());
        assertTrue(session.wantsVibration());
        session.remove(2);
        assertFalse(session.hasAlerts());
    }

    @Test public void expiredTimerDoesNotStopAnotherLaterTimer(){
        TimerRingSession session=new TimerRingSession();
        session.add(11,"Früh",true,true,1_000L);
        session.add(12,"Spät",true,true,60_000L);
        assertTrue(session.expire(1_000L+TimerRingSession.MAX_RING_MS));
        assertEquals("Spät",session.label());
        assertEquals(1,session.count());
        assertTrue(session.wantsTone());
    }

    @Test public void aRepeatedCompletionRefreshesOnlyThatTimersExpiry(){
        TimerRingSession session=new TimerRingSession();
        session.add(1,"Loop",true,true,0L);
        session.add(2,"Normal",true,false,5L);
        session.add(1,"Loop",true,true,120_000L);
        assertEquals(2,session.count());
        assertTrue(session.expire(TimerRingSession.MAX_RING_MS+5L));
        assertEquals(1,session.count());
        assertEquals("Loop",session.label());
        assertFalse(session.expire(120_000L+TimerRingSession.MAX_RING_MS-1L));
        assertTrue(session.expire(120_000L+TimerRingSession.MAX_RING_MS));
        assertFalse(session.hasAlerts());
    }

    @Test public void explicitSilentTimerDoesNotCreateAudioOrVibration(){
        TimerRingSession session=new TimerRingSession();
        session.add(9,"Still",false,false,42L);
        assertTrue(session.hasAlerts());
        assertFalse(session.wantsTone());
        assertFalse(session.wantsVibration());
        session.removeAll();
        assertFalse(session.hasAlerts());
    }

    @Test public void invalidIdsAreRejectedAndEmptyNamesSafe(){
        TimerRingSession session=new TimerRingSession();
        for(int id:new int[]{-1,0,10000})
            try{session.add(id,"X",true,true,0L);fail("Expected bad ID");}
            catch(IllegalArgumentException expected){}
        session.add(1," ",true,false,0L);
        assertEquals("Timer",session.label());
    }
}
