package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WatchAcknowledgementTest {
    @Test public void acknowledgesBothNativeCascadePhases(){
        assertTrue(WatchAcknowledgement.valid("CASCADE_GENTLE:1"));
        assertTrue(WatchAcknowledgement.valid("CASCADE_STRONG:1"));
        assertTrue(WatchAcknowledgement.valid("CASCADE_GENTLE:1000"));
        assertTrue(WatchAcknowledgement.valid("SNOOZE_UNCONFIRMED:7"));
    }
    @Test public void preservesLegacyAckEvents(){
        for(String name:new String[]{"STARTED","STOPPED","NO_VIBRATOR","SCHEDULED"})
            assertTrue(name,WatchAcknowledgement.valid(name+":42"));
    }
    @Test public void rejectsMalformedOrUnknownDeviceMessages(){
        for(String value:new String[]{null,"","PHASE_ONE:1","CASCADE_GENTLE:-1",
                "CASCADE_GENTLE:10000","CASCADE_GENTLE:1:extra",
                "CASCADE_STRONG:evil","CASCADE_STRONG:1\\nINJECTED"})
            assertFalse(String.valueOf(value),WatchAcknowledgement.valid(value));
    }
}
