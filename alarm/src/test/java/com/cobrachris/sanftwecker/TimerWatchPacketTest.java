package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TimerWatchPacketTest {
    @Test public void buildPacketForNativeWatchTimerWithoutAlteringClockAlarmWireFormat(){
        long when=1_800_000_000_000L;
        assertEquals("9:"+when+":1:VGVl",TimerWatchPacket.start(9,when,true,"Tee"));
        assertEquals("9:"+when+":0:VGVl",TimerWatchPacket.start(9,when,false,"Tee"));
        assertEquals("9:"+when,TimerWatchPacket.stop(9,when));
        assertTrue(TimerWatchPacket.validWhen(when,when+2_000));
        assertFalse(TimerWatchPacket.validWhen(when,when+181_000));
        assertNotEquals(TimerWatchPacket.PREFIX,WearAlarmBridge.PREFIX);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectInvalidId(){TimerWatchPacket.start(10_000,1234,true,"x");}
}
