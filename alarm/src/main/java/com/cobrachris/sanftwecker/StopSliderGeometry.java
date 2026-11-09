package com.cobrachris.sanftwecker;

/** Layout-only dimensions for the inset swipe-to-stop handle. Alarm logic is unchanged. */
public final class StopSliderGeometry {
    public static final int TRACK_HEIGHT_DP=82;
    public static final int THUMB_DIAMETER_DP=62;
    public static final int THUMB_OFFSET_DP=THUMB_DIAMETER_DP/2;
    public static final int PADDING_HORIZONTAL_DP=43;
    public static final int MIN_INSET_DP=PADDING_HORIZONTAL_DP-THUMB_OFFSET_DP;
    private StopSliderGeometry(){}
}
