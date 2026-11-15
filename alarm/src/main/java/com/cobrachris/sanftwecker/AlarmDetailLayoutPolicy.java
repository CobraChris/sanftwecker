package com.cobrachris.sanftwecker;

/**
 * Responsive weekday selector. Seven single-letter chips share one row
 * on ordinary phones, matching the overview screen. In narrow multi-window
 * mode or at large font scales, wrap instead of clipping/hiding weekdays.
 */
public final class AlarmDetailLayoutPolicy {
    public static final int MIN_TOUCH_TARGET_DP = 48;
    public static final int COMPACT_DAY_WIDTH_DP = 40;
    public static final int DAY_HEIGHT_DP = 44;
    public static final int DAY_GAP_DP = 2;
    private static final int SIDE_RESERVE_DP = 18 + 16 + 8;
    private AlarmDetailLayoutPolicy() { }

    public static int dayMinimumWidthDp(float fontScale) {
        float safeScale = Float.isFinite(fontScale) && fontScale > 0f ? fontScale : 1f;
        // Normal-size single letters can be narrower than the ideal 48dp target.
        // This intentional compact exception keeps all seven days visible in one
        // row on 360dp phones; expanded text/window modes retain the 48dp target.
        if(safeScale <= 1.3f) return COMPACT_DAY_WIDTH_DP;
        return Math.max(MIN_TOUCH_TARGET_DP, (int)Math.ceil(18f * safeScale + 12f));
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
