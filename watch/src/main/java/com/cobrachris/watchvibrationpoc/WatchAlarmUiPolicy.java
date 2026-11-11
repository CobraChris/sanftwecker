package com.cobrachris.watchvibrationpoc;

/** Pure policy for watch ringing-screen controls; independent of Wear OS UI. */
public final class WatchAlarmUiPolicy {
    private WatchAlarmUiPolicy(){}
    public static boolean active(int id,long when){
        return id>=1 && id<=1000 && when>0;
    }
    public static boolean snoozeVisible(int id,long when,int snoozeMinutes){
        return active(id,when) && snoozeMinutes>=1 && snoozeMinutes<=30;
    }
    public static String stage(boolean cascade,long when,long now){
        if(!cascade)return "Alarm aktiv";
        return WatchCascade.strong(when,now)
                ?"Phase 2 – deutlich":"Phase 1 – leise";
    }
}
