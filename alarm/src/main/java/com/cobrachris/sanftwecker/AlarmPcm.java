package com.cobrachris.sanftwecker;

/**
 * One PCM renderer shared by real wake alarms and reminders.
 * Its waveform is identical to RingingService's original synthesis path.
 * Only the caller owns AudioTrack, volume envelopes and stopping.
 */
public final class AlarmPcm {
    public static final int RATE=22050;
    private AlarmPcm(){}

    public static short sample(int tone,double seconds){
        double oscillation=ToneCatalog.sample(tone,seconds);
        double breathe=0.75+0.25*Math.cos(2*Math.PI*0.24*seconds);
        return (short)(Short.MAX_VALUE*0.36*breathe*oscillation);
    }
}
