package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AlarmEnvelopeTest {
    private static AlarmProfile delayed() {
        return new AlarmProfile(1,"Probe",true,7,0,0x7f,20,300,35,6,20,5,0);
    }

    @Test public void realAlarmRespectsTwentySecondSilence() {
        AlarmProfile profile = delayed();
        assertEquals(0f,AlarmEnvelope.gain(profile,false,0),0f);
        assertEquals(0f,AlarmEnvelope.gain(profile,false,19_999),0f);
        assertEquals(0f,AlarmEnvelope.gain(profile,false,20_000),0f);
        assertTrue(AlarmEnvelope.gain(profile,false,50_000)>0);
    }

    @Test public void diagnosticStartsFromZeroButIsAudibleWithinEightSeconds() {
        AlarmProfile profile = delayed();
        assertEquals(0f,AlarmEnvelope.gain(profile,true,0),0f);
        assertTrue(AlarmEnvelope.gain(profile,true,3_000)>0f);
        assertEquals(.45f,AlarmEnvelope.gain(profile,true,8_000),.00001f);
        assertEquals(.45f,AlarmEnvelope.gain(profile,true,20_000),.00001f);
    }

    @Test public void normalRampRemainsSlow() {
        AlarmProfile p=delayed();
        assertTrue(AlarmEnvelope.gain(p,false,30_000) < .01f);
        assertEquals(.35f,AlarmEnvelope.gain(p,false,320_000),.00001f);
    }

    @Test public void regularAlarmGrowsSoonerAtConfiguredVolume(){
        AlarmProfile p=delayed();
        assertEquals(0f,AlarmEnvelope.gain(p,false,20_000),0f);
        assertEquals(.042f,AlarmEnvelope.gain(p,false,80_000),.00001f);
        assertTrue(AlarmEnvelope.gain(p,false,120_000)>
                   AlarmEnvelope.gain(p,false,80_000));
        assertEquals(.35f,AlarmEnvelope.gain(p,false,320_000),.00001f);
    }
    @Test public void previewUsesUserHigherLimitWhenRequested() {
        AlarmProfile loud=new AlarmProfile(3,"Probe",false,8,30,127,
                0,600,70,6,20,5,0);
        assertEquals(.70f,AlarmEnvelope.gain(loud,true,10_000),.00001f);
    }
}
