package com.cobrachris.sanftwecker;

/**
 * Pure dp-based policy for the editable weekday controls.
 * Layout uses separate rows so every day remains visible and tappable
 * even in split-screen, narrow displays and large system font settings.
 */
public final class AlarmDetailLayoutPolicy {
    public static final int MIN_TOUCH_TARGET_DP = 48;
    public static final int DAY_GAP_DP = 4;
    private static final int SIDE_RESERVE_DP = 18 + 16 + 8;
    private AlarmDetailLayoutPolicy() { }

    public static int dayMinimumWidthDp(float fontScale) {
        float safeScale = Float.isFinite(fontScale) && fontScale > 0f ? fontScale : 1f;
        // Two abbreviated letters at 14sp with 12dp inner horizontal breathing room.
        return Math.max(MIN_TOUCH_TARGET_DP, (int)Math.ceil(28f * safeScale + 12f));
    }

    public static int availableWeekdayWidthDp(int screenWidthDp) {
        return Math.max(0, screenWidthDp - 2 * SIDE_RESERVE_DP);
    }

    public static int weekdayColumns(int screenWidthDp, float fontScale) {
        int width = availableWeekdayWidthDp(screenWidthDp);
        int cell = dayMinimumWidthDp(fontScale);
        for (int columns : new int[]{7, 4, 3, 2}) {
            if (width >= columns * cell + (columns - 1) * DAY_GAP_DP)
                return columns;
        }
        return 1; // Extremely narrow windows: prioritize reachability over density.
    }
}
