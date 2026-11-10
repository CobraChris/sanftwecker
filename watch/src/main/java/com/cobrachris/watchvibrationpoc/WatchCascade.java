package com.cobrachris.watchvibrationpoc;

/** Wear OS physical vibration phases; monotonic once the shared alarm clock reaches 60s. */
public final class WatchCascade {
    public static final long SECOND_STAGE_MS=60_000L;
    private WatchCascade(){}

    public static boolean strong(long scheduledAt,long now){
        return now-scheduledAt>=SECOND_STAGE_MS;
    }
    public static long untilStrong(long scheduledAt,long now){
        return Math.max(0L,scheduledAt+SECOND_STAGE_MS-now);
    }
    /** 110 ms every 9.1s: intentionally light and unobtrusive even without amplitude control. */
    public static long[] gentleTimings(){
        return new long[]{0L,110L,9000L};
    }
    public static int[] gentleAmplitudes(){
        return new int[]{0,48,0};
    }
    /** Dense, long pulses as phase two; old watch alarm strength is preserved. */
    public static long[] strongTimings(){
        return new long[]{0L,850L,120L,850L,120L,850L,250L,850L,400L};
    }
    public static int[] strongAmplitudes(){
        return new int[]{0,255,0,255,0,255,0,255,0};
    }
}
