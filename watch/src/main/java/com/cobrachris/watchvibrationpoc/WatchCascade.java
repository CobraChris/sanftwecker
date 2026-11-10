package com.cobrachris.watchvibrationpoc;

/** Wear OS haptic patterns. Defaults preserve v27 behavior for existing alarms. */
public final class WatchCascade {
    public static final long SECOND_STAGE_MS=60_000L;
    public static final int SANFT=0, NORMAL=1, INTENSIV=2;
    private WatchCascade(){}

    public static boolean validIntensity(int value){return value>=SANFT&&value<=INTENSIV;}
    public static int safeIntensity(int value){return validIntensity(value)?value:NORMAL;}

    public static boolean strong(long scheduledAt,long now){
        return now-scheduledAt>=SECOND_STAGE_MS;
    }
    public static long untilStrong(long scheduledAt,long now){
        return Math.max(0L,scheduledAt+SECOND_STAGE_MS-now);
    }
    public static long[] gentleTimings(){return gentleTimings(NORMAL);}
    public static int[] gentleAmplitudes(){return gentleAmplitudes(NORMAL);}
    public static long[] strongTimings(){return strongTimings(NORMAL);}
    public static int[] strongAmplitudes(){return strongAmplitudes(NORMAL);}

    public static long[] gentleTimings(int intensity){
        switch(safeIntensity(intensity)){
            case SANFT: return new long[]{0L,45L,17000L};
            case INTENSIV: return new long[]{0L,140L,6500L};
            default: return new long[]{0L,110L,9000L};
        }
    }
    public static int[] gentleAmplitudes(int intensity){
        switch(safeIntensity(intensity)){
            case SANFT: return new int[]{0,20,0};
            case INTENSIV: return new int[]{0,80,0};
            default: return new int[]{0,48,0};
        }
    }
    /** Phase 2 uses a recognizable double pulse, even without amplitude control. */
    public static long[] strongTimings(int intensity){
        switch(safeIntensity(intensity)){
            case SANFT: return new long[]{0L,250L,200L,250L,2500L};
            case INTENSIV: return new long[]{0L,1000L,80L,1000L,80L,1000L,160L};
            default: return new long[]{0L,850L,120L,850L,120L,850L,250L,850L,400L};
        }
    }
    public static int[] strongAmplitudes(int intensity){
        switch(safeIntensity(intensity)){
            case SANFT: return new int[]{0,175,0,175,0};
            case INTENSIV: return new int[]{0,255,0,255,0,255,0};
            default: return new int[]{0,255,0,255,0,255,0,255,0};
        }
    }
}
