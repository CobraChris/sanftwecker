package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public class AlarmDetailLayoutPolicyTest {
    @Test public void everyWeekdayFitsWithoutHorizontalOverflow() {
        int[] widths = {180, 220, 240, 280, 320, 360, 393, 480, 600};
        float[] scales = {0.85f, 1f, 1.3f, 1.5f, 2f, 2.5f};
        for (int width : widths) {
            for (float scale : scales) {
                int columns = AlarmDetailLayoutPolicy.weekdayColumns(width, scale);
                int target = AlarmDetailLayoutPolicy.dayMinimumWidthDp(scale);
                assertTrue("minimum 48dp target", target >= 48);
                assertTrue("at least one column", columns >= 1);
                assertTrue("at most seven", columns <= 7);
                if (columns > 1) {
                    int occupied = columns * target + (columns - 1) *
                            AlarmDetailLayoutPolicy.DAY_GAP_DP;
                    assertTrue("screen=" + width + " scale=" + scale,
                            occupied <= AlarmDetailLayoutPolicy.availableWeekdayWidthDp(width));
                }
            }
        }
    }

    @Test public void mostPhonesUseMultipleRowsWith48dpOrLargerTargets() {
        assertEquals(4, AlarmDetailLayoutPolicy.weekdayColumns(320, 1f));
        assertEquals(4, AlarmDetailLayoutPolicy.weekdayColumns(393, 1f));
        assertEquals(7, AlarmDetailLayoutPolicy.weekdayColumns(480, 1f));
        assertEquals(3, AlarmDetailLayoutPolicy.weekdayColumns(320, 2f));
        assertEquals(4, AlarmDetailLayoutPolicy.weekdayColumns(393, 2f));
        assertEquals(2, AlarmDetailLayoutPolicy.weekdayColumns(240, 2f));
    }

    @Test public void badConfigurationScalesDoNotProduceBrokenGeometry() {
        assertEquals(48, AlarmDetailLayoutPolicy.dayMinimumWidthDp(0f));
        assertEquals(48, AlarmDetailLayoutPolicy.dayMinimumWidthDp(Float.NaN));
        assertEquals(48, AlarmDetailLayoutPolicy.dayMinimumWidthDp(Float.POSITIVE_INFINITY));
        assertEquals(0, AlarmDetailLayoutPolicy.availableWeekdayWidthDp(30));
        assertEquals(1, AlarmDetailLayoutPolicy.weekdayColumns(30, 2f));
    }
}
