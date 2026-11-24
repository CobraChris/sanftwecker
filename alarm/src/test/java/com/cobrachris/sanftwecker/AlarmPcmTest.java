package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

/** Protect the exact Testwecker sound while sharing the audio renderer. */
public class AlarmPcmTest {
    @Test public void everyOriginalToneRetainsItsPcmSamples() {
        double[] seconds={0.0,0.035,0.2,1.25,4.89,18.5,60.3};
        for(int tone=0;tone<ToneCatalog.count();tone++){
            for(double t:seconds){
                double oscillation=ToneCatalog.sample(tone,t);
                double breathe=0.75+0.25*Math.cos(2*Math.PI*0.24*t);
                short original=(short)(Short.MAX_VALUE*0.36*breathe*oscillation);
                assertEquals("tone="+tone+" t="+t,original,AlarmPcm.sample(tone,t));
            }
        }
        assertEquals(22050,AlarmPcm.RATE);
    }

    @Test public void savedTestweckerEnvelopeIsNotAConstantFortyTwoPercent() {
        AlarmProfile d=AlarmProfile.defaults(1);
        AlarmProfile configured=new AlarmProfile(d.id,"Testwecker",false,d.hour,d.minute,
                d.days,d.delaySeconds,d.rampSeconds,83,
                d.watchPulses,d.watchIntervalSeconds,d.snoozeMinutes,d.tone,
                true,30,0,0,0,true,d.vibrationMode,d.audibleWhenMuted,true,d.watchIntensity);
        assertEquals(0f,AlarmEnvelope.gain(configured,false,0),0.00001f);
        assertEquals(0.83f,AlarmEnvelope.gain(configured,false,60_000),0.00001f);
        assertEquals(30*60_000L,
                WakeCascade.duration(false,false,configured.cascadeEnabled,
                        configured.autoStopMinutes));
    }
}
