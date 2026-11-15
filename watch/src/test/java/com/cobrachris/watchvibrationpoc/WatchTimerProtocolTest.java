package com.cobrachris.watchvibrationpoc;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public final class WatchTimerProtocolTest {
    @Test public void acceptsNamedTimerWithCustomVibration(){
        long when=1_800_000_000_000L;
        String label=Base64.getUrlEncoder().withoutPadding()
                .encodeToString("Kaffee & Tee".getBytes(StandardCharsets.UTF_8));
        WatchTimerProtocol.Packet packet=WatchTimerProtocol.start("456:"+when+":1:"+label);
        assertNotNull(packet);
        assertEquals(456,packet.id);
        assertEquals(when,packet.when);
        assertTrue(packet.vibration);
        assertEquals("Kaffee & Tee",packet.name);
        assertFalse(WatchTimerProtocol.start("456:"+when+":0:"+label).vibration);
    }
    @Test public void guardAgainstStaleMessagesAndMalformedCommands(){
        long now=1_800_000_000_000L;
        assertTrue(WatchTimerProtocol.validWhen(now-179_999,now));
        assertFalse(WatchTimerProtocol.validWhen(now-180_001,now));
        assertFalse(WatchTimerProtocol.validWhen(now+20_000,now));
        assertNull(WatchTimerProtocol.start("bad"));
        assertNull(WatchTimerProtocol.start("10000:"+now+":1:VGVzdA"));
        assertNull(WatchTimerProtocol.start("1:"+now+":2:VGVzdA"));
        assertNull(WatchTimerProtocol.start("1:"+now+":1:??"));
        assertNull(WatchTimerProtocol.stop("bad"));
        assertNull(WatchTimerProtocol.stop("10000:"+now));
        assertEquals(456,WatchTimerProtocol.stop("456:"+now).id);
        assertEquals(now,WatchTimerProtocol.stop("456:"+now).when);
    }
}
