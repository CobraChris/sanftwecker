package com.cobrachris.watchvibrationpoc;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WatchAlarmProtocolTest {
    @Test public void ringPacketMustBeExactAndBounded(){
        WatchAlarmProtocol.Packet p=WatchAlarmProtocol.parse("1:1794500000000:5");
        assertNotNull(p);assertEquals(1,p.id);assertEquals(1794500000000L,p.when);
        assertEquals(5,p.minutes);
        for(String bad:new String[]{
            null,"","0:1794500000000:5","1001:1794500000000:5",
            "1:1794500000000:0","1:1794500000000:16",
            "1:1794500000000:5:extra","1:evil:5","1:-1794500000000:5"
        })assertNull(bad,WatchAlarmProtocol.parse(bad));
    }
    @Test public void snoozeIntervalIsTransmittedOnlyWhenExplicitlyEnabled(){
        WatchAlarmProtocol.Packet old=WatchAlarmProtocol.parse("1:1794500000000:5");
        assertNotNull(old);
        assertEquals(0,old.snoozeMinutes);
        assertFalse(old.cascade);
        WatchAlarmProtocol.Packet cascadeLegacy=
                WatchAlarmProtocol.parse("1:1794500000000:5:1");
        assertNotNull(cascadeLegacy);
        assertTrue(cascadeLegacy.cascade);
        assertEquals(0,cascadeLegacy.snoozeMinutes);

        WatchAlarmProtocol.Packet snooze=
                WatchAlarmProtocol.parse("1:1794500000000:5:0:7");
        assertNotNull(snooze);
        assertFalse(snooze.cascade);
        assertEquals(7,snooze.snoozeMinutes);

        WatchAlarmProtocol.Packet cascadeSnooze=
                WatchAlarmProtocol.parse("1:1794500000000:5:1:30");
        assertNotNull(cascadeSnooze);
        assertTrue(cascadeSnooze.cascade);
        assertEquals(30,cascadeSnooze.snoozeMinutes);
        assertEquals(0,WatchAlarmProtocol.parse("1:1794500000000:5:0:0").snoozeMinutes);
        for(String bad:new String[]{
            "1:1794500000000:5:1:31",
            "1:1794500000000:5:1:-1",
            "1:1794500000000:5:2:5",
            "1:1794500000000:5:0:005",
            "1:1794500000000:5:1:5:extra",
            "1:1794500000000:5:0:999"
        })assertNull(bad,WatchAlarmProtocol.parse(bad));
    }
    @Test public void oldDisconnectedMessagesMustNotCauseLateAlarms(){
        long now=1794500000000L;
        assertTrue(WatchAlarmProtocol.nearTrigger(now,now));
        assertTrue(WatchAlarmProtocol.nearTrigger(now-90_000,now));
        assertFalse(WatchAlarmProtocol.nearTrigger(now-90_001,now));
        assertFalse(WatchAlarmProtocol.nearTrigger(now+15_001,now));
        assertFalse(WatchAlarmProtocol.nearTrigger(0,now));
    }
    @Test public void scheduleSlotsAndTimeAreBounded(){
        assertTrue(WatchAlarmProtocol.validSlot(1));
        assertTrue(WatchAlarmProtocol.validSlot(1000));
        assertTrue(WatchAlarmProtocol.validSlot(WatchAlarmProtocol.TEST_SLOT));
        assertTrue(WatchAlarmProtocol.validSlot(WatchAlarmProtocol.SNOOZE_SLOT));
        assertFalse(WatchAlarmProtocol.validSlot(0));
        assertFalse(WatchAlarmProtocol.validSlot(1003));
        long now=1794500000000L;
        assertTrue(WatchAlarmProtocol.validWhen(now+2000,now));
        assertFalse(WatchAlarmProtocol.validWhen(now+999,now));
        assertFalse(WatchAlarmProtocol.validWhen(now+367L*24*60*60*1000,now));
    }
}