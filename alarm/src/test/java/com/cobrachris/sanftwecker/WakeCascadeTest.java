package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WakeCascadeTest {
    @Test public void stageSwitchesExactlyAtSixtySeconds(){
        assertFalse(WakeCascade.strongStage(0));
        assertFalse(WakeCascade.strongStage(59_999L));
        assertTrue(WakeCascade.strongStage(60_000L));
        assertTrue(WakeCascade.strongStage(120_000L));
    }
    @Test public void existingShortTestAndPreviewRemainShort(){
        assertEquals(20_000L,WakeCascade.duration(true,true,true,20));
        assertEquals(30_000L,WakeCascade.duration(false,true,false,20));
        assertEquals(60_000L,WakeCascade.duration(false,false,false,1));
    }
    @Test public void cascadeQuickTestCoversBothStages(){
        assertEquals(150_000L,WakeCascade.duration(false,true,true,1));
        assertEquals(150_000L,WakeCascade.duration(false,true,true,20));
        assertEquals(120_000L,WakeCascade.duration(false,false,true,1));
        assertEquals(600_000L,WakeCascade.duration(false,false,true,10));
    }
    @Test public void cascadeHandsetVibratesEvenForOldNotificationOnlyProfile(){
        assertEquals(4,WakeCascade.phoneMode(0));
        for(int mode=1;mode<=4;mode++)
            assertEquals(mode,WakeCascade.phoneMode(mode));
    }
    @Test public void cascadeGainRisesMonotonicallyAndHonorsConfiguredMax(){
        AlarmProfile base=AlarmProfile.defaults(1);
        AlarmProfile p=new AlarmProfile(base.id,base.name,true,base.hour,base.minute,
                base.days,30,600,55,0,base.watchIntervalSeconds,base.snoozeMinutes,
                base.tone,true,20,0,0,0,base.snoozeEnabled,0,false,true);
        float prev=-1;
        for(int ms=0;ms<=90_000;ms+=1_000){
            float gain=AlarmEnvelope.gain(p,false,ms);
            assertTrue(gain>=prev);
            assertTrue(gain<=.55f);
            prev=gain;
        }
        assertEquals(0f,AlarmEnvelope.gain(p,false,0),0f);
        assertTrue(AlarmEnvelope.gain(p,false,10_000)>0f);
        assertEquals(.55f,AlarmEnvelope.gain(p,false,60_000),.00001f);
        assertEquals(.55f,AlarmEnvelope.gain(p,false,90_000),.00001f);
        assertFalse(base.cascadeEnabled);
        assertTrue(p.withEnabled(false).cascadeEnabled);
        assertTrue(p.duplicateAs(7).cascadeEnabled);
        assertFalse(base.sameConfiguration(p));
    }
}
