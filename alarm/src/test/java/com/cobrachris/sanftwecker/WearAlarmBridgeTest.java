package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WearAlarmBridgeTest {
    @Test public void independentWatchProtocolHasExactStablePaths(){
        assertEquals("/sanftwecker/alarm/v1",WearAlarmBridge.PREFIX);
        assertEquals("/sanftwecker/alarm/v1/schedule/",WearAlarmBridge.SCHEDULE);
        assertEquals("/sanftwecker/alarm/v1/ring",WearAlarmBridge.RING);
        assertEquals("/sanftwecker/alarm/v1/stop",WearAlarmBridge.STOP);
        assertEquals("/sanftwecker/alarm/v1/ack",WearAlarmBridge.ACK);
        assertEquals(1001,WearAlarmBridge.TEST_SLOT);
        assertEquals(1002,WearAlarmBridge.SNOOZE_SLOT);
    }
    @Test public void ringingPacketsHaveExactTimeAndNoExcessDuration(){
        assertEquals("1:1794500000000:5",WearAlarmBridge.packet(1,1794500000000L,5));
        assertEquals("1000:1794500000000:15",WearAlarmBridge.packet(1000,1794500000000L,15));
        for(int id:new int[]{0,-1,1001}) {
            try{WearAlarmBridge.packet(id,1794500000000L,5);fail();}
            catch(IllegalArgumentException expected){}
        }
        for(int duration:new int[]{0,31}){
            try{WearAlarmBridge.packet(1,1794500000000L,duration);fail();}
            catch(IllegalArgumentException expected){}
        }
    }
}