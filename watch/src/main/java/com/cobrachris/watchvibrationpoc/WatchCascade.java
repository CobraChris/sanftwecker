package com.cobrachris.watchvibrationpoc;

/** Wear OS haptic patterns. Defaults preserve v27 behavior for existing alarms. */
public final class WatchCascade {
    public static final long SECOND_STAGE_MS=60_000L;
    public static final int SANFT=0, NORMAL=1, INTENSIV=2;
    private WatchCascade(){}

    public static boolean validIntensity(int value){return value>=SANFT&&value<=INTENSIV;}
    public static int safePulses(int count){return count>=0&&count<=12?count:6;}
    public static int safeInterval(int seconds){
        switch(seconds){
            case 3: case 5: case 10: case 20: case 30: case 40: case 50:
            case 60: case 70: case 80: case 90: case 100: case 110: case 120:
                return seconds;
            default: return 5;
        }
    }
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
            case INTENSIV: return new long[]{0L,320L,0L};
            default: return new long[]{0L,110L,9000L};
        }
    }
    public static int[] gentleAmplitudes(int intensity){
        switch(safeIntensity(intensity)){
            case SANFT: return new int[]{0,20,0};
            case INTENSIV: return new int[]{0,185,0};
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
