package com.cobrachris.sanftwecker;

/** Explicit opt-in: temporarily compensate for a muted OR very low Android alarm stream. */
public final class AlarmVolumePolicy {
    private AlarmVolumePolicy(){}

    public static int temporaryLevel(int max) {
        if(max <= 0)throw new IllegalArgumentException("No alarm volume range");
        return Math.max(1,Math.min(max,(int)Math.ceil(max*0.75)));
    }

    public static boolean shouldRaise(int original, int maximum) {
        if(maximum<=0||original<0||original>maximum)return false;
        // Tiny Android STREAM_ALARM values make any in-app ramp almost inaudible.
        // Never override a healthy system level. This applies ONLY when opted-in.
        return original < temporaryLevel(maximum) && original <= maximum / 3;
    }
    public static boolean shouldRestore(int original,int current,int boosted) {
        // Preserve any volume change made manually during the alarm.
        return original >= 0 && boosted>original && current==boosted;
    }
}
