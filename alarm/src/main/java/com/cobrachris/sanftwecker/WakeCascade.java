package com.cobrachris.sanftwecker;

/** Stable shared timing policy for the optional two-stage wake cascade. */
public final class WakeCascade {
    public static final long SECOND_STAGE_MS=60_000L;
    public static final long FULL_TEST_MS=150_000L;
    public static final long MIN_ALARM_MS=120_000L;
    private WakeCascade(){}

    /** Both devices calculate the phase from the same scheduled alarm time.
     * Normal tone previews without an occurrence retain monotonic elapsed time.
     */
    public static long elapsedSinceAlarm(long when,long nowWall,long serviceElapsed){
        return when>0?Math.max(0L,nowWall-when):Math.max(0L,serviceElapsed);
    }
    public static long untilStrong(long when,long nowWall,long serviceElapsed){
        return Math.max(0L,SECOND_STAGE_MS-
                elapsedSinceAlarm(when,nowWall,serviceElapsed));
    }
    public static boolean strongStage(long elapsedMs){
        return elapsedMs>=SECOND_STAGE_MS;
    }
    public static int phoneMode(int configuredMode){
        // Cascade is an explicit choice to vibrate the handset in minute two,
        // even if an old profile had notification-only mode zero.
        return configuredMode==0?4:configuredMode;
    }
    public static long duration(boolean preview,boolean quickTest,
            boolean cascadeEnabled,int autoStopMinutes){
        if(preview)return 20_000L;
        if(cascadeEnabled){
            if(quickTest)return FULL_TEST_MS;
            return Math.max(MIN_ALARM_MS,autoStopMinutes*60_000L);
        }
        return quickTest?30_000L:autoStopMinutes*60_000L;
    }
}
