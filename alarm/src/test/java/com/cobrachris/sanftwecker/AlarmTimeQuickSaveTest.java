package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression: time-picker OK is an atomic time+enabled update, not a reset. */
public final class AlarmTimeQuickSaveTest {
    @Test public void selectingNewTimeActivatesPreviouslyDisabledAlarm(){
        AlarmProfile previous=AlarmProfile.defaults(3)
                .withExceptions(1_800_000_000_000L,1_800_000_001_000L,1_800_000_002_000L);
        assertFalse(previous.enabled);
        AlarmProfile updated=previous.withTimeAndEnabled(6,35);
        assertTrue(updated.enabled);
        assertEquals(6,updated.hour);
        assertEquals(35,updated.minute);
        assertEquals(0L,updated.skipAt);
        assertEquals(0L,updated.oneTimeAt);
        assertEquals(0L,updated.replacedAt);
        assertEquals(previous.id,updated.id);
        assertEquals(previous.name,updated.name);
        assertEquals(previous.days,updated.days);
        assertEquals(previous.watchPulses,updated.watchPulses);
        assertEquals(previous.watchIntervalSeconds,updated.watchIntervalSeconds);
        assertEquals(previous.watchIntensity,updated.watchIntensity);
        assertEquals(previous.cascadeEnabled,updated.cascadeEnabled);
        assertEquals(previous.snoozeEnabled,updated.snoozeEnabled);
        assertEquals(previous.snoozeMinutes,updated.snoozeMinutes);
        assertEquals(previous.autoStopMinutes,updated.autoStopMinutes);
        assertEquals(previous.vibrationMode,updated.vibrationMode);
        assertEquals(previous.tone,updated.tone);
        assertEquals(previous.rampEnabled,updated.rampEnabled);
        assertEquals(previous.rampSeconds,updated.rampSeconds);
        assertEquals(previous.delaySeconds,updated.delaySeconds);
        assertEquals(previous.maxVolume,updated.maxVolume);
    }

    @Test public void confirmingSameTimeStillEnablesButDoesNotClearExceptions(){
        AlarmProfile previous=AlarmProfile.defaults(2)
                .withExceptions(12L,34L,56L);
        AlarmProfile result=previous.withTimeAndEnabled(previous.hour,previous.minute);
        assertTrue(result.enabled);
        assertEquals(12L,result.skipAt);
        assertEquals(34L,result.oneTimeAt);
        assertEquals(56L,result.replacedAt);
    }

    @Test public void alreadyEnabledAlarmWithSameTimeRemainsUnchanged(){
        AlarmProfile a=AlarmProfile.defaults(1).withEnabled(true);
        assertTrue(a.sameConfiguration(a.withTimeAndEnabled(a.hour,a.minute)));
    }

    @Test public void invalidTimeNeverReturnsAnEnabledSchedule(){
        AlarmProfile original=AlarmProfile.defaults(3);
        for(int h:new int[]{-1,24})
            try{original.withTimeAndEnabled(h,10);fail("invalid hour");}
            catch(IllegalArgumentException expected){}
        for(int m:new int[]{-1,60})
            try{original.withTimeAndEnabled(6,m);fail("invalid minute");}
            catch(IllegalArgumentException expected){}
        assertFalse(original.enabled);
    }
}
