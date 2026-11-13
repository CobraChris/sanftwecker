package com.cobrachris.sanftwecker;

/** Pure, locale-independent minute countdown; uses actual scheduled epoch time. */
public final class AlarmCountdown {
    private AlarmCountdown() { }

    public static String label(long scheduledAt, long now) {
        if (scheduledAt <= 0L) return "Nicht eingeplant";
        if (scheduledAt <= now) return "Klingelt jetzt";
        long diff = scheduledAt - now;
        // Round up: never say '0 Min.' while the next alarm is in the future.
        long minutes = diff / 60000L + (diff % 60000L == 0 ? 0 : 1);
        long hours = minutes / 60L;
        long rest = minutes % 60L;
        if (hours == 0) return "Klingelt in " + minutes + " Min.";
        return "Klingelt in " + hours + " Std. " + rest + " Min.";
    }
}
