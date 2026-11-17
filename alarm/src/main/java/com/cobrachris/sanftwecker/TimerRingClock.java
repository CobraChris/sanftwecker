package com.cobrachris.sanftwecker;

/**
 * Pure elapsed-time view of the exact completed timer occurrence.
 * Uses the monotonic deadline on the same boot, falling back to wall time
 * after a reboot. It does not extend audio/vibration or mutate timer state.
 */
public final class TimerRingClock {
    private TimerRingClock(){}

    public static long elapsed(long dueWall,long dueElapsed,long bootOffset,
            long wallNow,long elapsedNow){
        if(dueWall<=0)return 0;
        boolean sameBoot=dueElapsed>0 &&
                Math.abs((wallNow-elapsedNow)-bootOffset)<60_000L;
        long due=sameBoot?dueElapsed:dueWall;
        long now=sameBoot?elapsedNow:wallNow;
        return Math.max(0,now-due);
    }
}
