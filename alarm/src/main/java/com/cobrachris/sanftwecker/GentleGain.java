package com.cobrachris.sanftwecker;

/** Pure gain envelope: 0 until delay, then an audible but gradual climb. */
public final class GentleGain {
    private GentleGain() {}

    public static float level(long elapsedMs, int delaySeconds, int rampSeconds,
            int maxPercent) {
        if (elapsedMs <= delaySeconds * 1000L) return 0f;
        if (rampSeconds < 1 || maxPercent < 0 || maxPercent > 100
                || delaySeconds < 0) throw new IllegalArgumentException("Bad envelope");
        double t = (elapsedMs - delaySeconds * 1000L) / (rampSeconds * 1000.0);
        t = Math.max(0, Math.min(1, t));
        // Quadratic-only rise spent the first several minutes effectively inaudible
        // at the user's existing system alarm volume. Keep the delay, full configured
        // ramp duration and maximum, but make the early climb perceptible and smooth.
        // No jump at t=0; never exceeds the user's chosen peak.
        return (float) ((maxPercent / 100.0) * t * (0.5 + 0.5 * t));
    }
}
