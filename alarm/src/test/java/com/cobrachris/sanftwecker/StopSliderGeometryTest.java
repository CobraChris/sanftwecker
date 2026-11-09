package com.cobrachris.sanftwecker;
import org.junit.Test;
import static org.junit.Assert.*;
/** Verify that the native thumb cannot stick outside the pill outline at progress zero. */
public final class StopSliderGeometryTest {
    @Test public void thumbIsInsetInsteadOfHangingOverEdge(){
        assertTrue(StopSliderGeometry.MIN_INSET_DP>=10);
        assertEquals(12,StopSliderGeometry.MIN_INSET_DP);
        assertTrue(StopSliderGeometry.TRACK_HEIGHT_DP-
                StopSliderGeometry.THUMB_DIAMETER_DP>=16);
        assertEquals(StopSliderGeometry.THUMB_DIAMETER_DP/2,
                StopSliderGeometry.THUMB_OFFSET_DP);
    }
}
