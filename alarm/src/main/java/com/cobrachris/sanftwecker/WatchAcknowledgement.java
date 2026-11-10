package com.cobrachris.sanftwecker;

/** Bounded acknowledgement messages from the signed Wear OS companion. */
public final class WatchAcknowledgement {
    private WatchAcknowledgement() {}

    public static boolean valid(String value){
        return value!=null && value.length()<=96 &&
                value.matches("(STARTED|STOPPED|NO_VIBRATOR|SCHEDULED|CASCADE_GENTLE|CASCADE_STRONG|SNOOZE_UNCONFIRMED):[0-9]{1,4}");
    }
}
