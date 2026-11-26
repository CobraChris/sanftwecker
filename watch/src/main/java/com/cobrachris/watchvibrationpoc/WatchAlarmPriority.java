package com.cobrachris.watchvibrationpoc;

/** Stale watch-alarm preferences cannot mute later independent reminders. */
public final class WatchAlarmPriority {
    static final long MAX_ALARM_MS=15L*60_000L;
    static final long MAX_FUTURE_SKEW_MS=60_000L;
    private WatchAlarmPriority(){}

    public static boolean blocksReminder(int alarmId,long scheduledAt,long now){
        if(alarmId<=0||scheduledAt<=0)return false;
        long elapsed=now-scheduledAt;
        return elapsed>=-MAX_FUTURE_SKEW_MS&&elapsed<=MAX_ALARM_MS;
    }
}
