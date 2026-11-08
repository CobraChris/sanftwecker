package com.cobrachris.sanftwecker;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AlarmVolumePolicyTest {
    @Test public void temporaryOverrideHasFiniteAudibleTarget(){
        assertEquals(1,AlarmVolumePolicy.temporaryLevel(1));
        assertEquals(8,AlarmVolumePolicy.temporaryLevel(10));
        assertEquals(12,AlarmVolumePolicy.temporaryLevel(15));
    }
    @Test public void restoreOnlyIfUserHasNotChangedVolume(){
        assertTrue(AlarmVolumePolicy.shouldRestore(0,12,12));
        assertFalse(AlarmVolumePolicy.shouldRestore(0,10,12));
        assertTrue(AlarmVolumePolicy.shouldRestore(5,12,12));
        assertFalse(AlarmVolumePolicy.shouldRestore(12,12,12));
        assertFalse(AlarmVolumePolicy.shouldRestore(0,0,12));
    }
    @Test public void optInOnlyBoostsMutedOrVeryQuietSystemAlarms(){
        assertTrue(AlarmVolumePolicy.shouldRaise(0,15));
        assertTrue(AlarmVolumePolicy.shouldRaise(2,15));
        assertTrue(AlarmVolumePolicy.shouldRaise(5,15));
        assertFalse(AlarmVolumePolicy.shouldRaise(6,15));
        assertFalse(AlarmVolumePolicy.shouldRaise(12,15));
        assertFalse(AlarmVolumePolicy.shouldRaise(-1,15));
        assertFalse(AlarmVolumePolicy.shouldRaise(2,0));
        assertFalse(AlarmVolumePolicy.shouldRaise(16,15));
    }
    @Test(expected=IllegalArgumentException.class)
    public void invalidAlarmVolumeRangeRejected(){
        AlarmVolumePolicy.temporaryLevel(0);
    }
}
