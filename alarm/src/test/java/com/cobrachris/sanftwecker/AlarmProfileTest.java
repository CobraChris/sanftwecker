package com.cobrachris.sanftwecker;
import org.junit.Test;
import static org.junit.Assert.*;

public class AlarmProfileTest {
    @Test public void threeDefaultsDisabled() {
        assertEquals(AlarmProfile.COUNT, 3);
        for (int id=1; id<=AlarmProfile.COUNT; id++) {
            AlarmProfile p = AlarmProfile.defaults(id);
            assertFalse(p.enabled);
            assertTrue(p.maxVolume > 0);
            assertTrue(p.days != 0);
        }
        assertEquals(0x1f, AlarmProfile.defaults(1).days);
        assertEquals(0x60, AlarmProfile.defaults(2).days);
    }
    @Test public void newAlarmsDefaultToFiveSecondWatchInterval() {
        assertEquals(5,AlarmProfile.defaults(1).watchIntervalSeconds);
    }
    @Test public void existingTenSecondProfilesArePreservedAcrossEdits() {
        AlarmProfile a=new AlarmProfile(1,"Test",true,7,0,127,10,60,55,12,10,5,0);
        assertEquals(10,a.withEnabled(false).watchIntervalSeconds);
        assertEquals(10,a.duplicateAs(2).watchIntervalSeconds);
        assertTrue(WatchIntervals.isSupported(a.watchIntervalSeconds));
    }
    @Test public void newThreeSecondIntervalsAreValid() {
        AlarmProfile a=new AlarmProfile(1,"Schnell",true,7,0,127,10,60,55,12,3,5,0);
        assertEquals(3,a.watchIntervalSeconds);
    }
    @Test public void audibleOnMutedAlarmIsOptInAndSurvivesCopy() {
        AlarmProfile old=AlarmProfile.defaults(1);
        assertFalse(old.audibleWhenMuted);
        AlarmProfile optIn=new AlarmProfile(old.id,old.name,old.enabled,old.hour,old.minute,
                old.days,old.delaySeconds,old.rampSeconds,old.maxVolume,
                old.watchPulses,old.watchIntervalSeconds,old.snoozeMinutes,old.tone,
                old.rampEnabled,old.autoStopMinutes,old.skipAt,old.oneTimeAt,
                old.replacedAt,old.snoozeEnabled,old.vibrationMode,true);
        assertTrue(optIn.audibleWhenMuted);
        assertTrue(optIn.withEnabled(true).audibleWhenMuted);
        assertTrue(optIn.duplicateAs(2).audibleWhenMuted);
        assertFalse(old.sameConfiguration(optIn));
    }
    @Test public void enablesWithoutLosingValues() {
        AlarmProfile p = AlarmProfile.defaults(2);
        AlarmProfile q = p.withEnabled(true);
        assertTrue(q.enabled);
        assertEquals(p.rampSeconds, q.rampSeconds);
        assertEquals(p.days, q.days);
    }
    @Test public void nineToneProfilesKeepTheirIdsOnCopyAndEdit() {
        for(int index=0;index<ToneCatalog.count();index++){
            AlarmProfile old=AlarmProfile.defaults(1);
            AlarmProfile chosen=new AlarmProfile(old.id,old.name,old.enabled,old.hour,old.minute,
                    old.days,old.delaySeconds,old.rampSeconds,old.maxVolume,old.watchPulses,
                    old.watchIntervalSeconds,old.snoozeMinutes,index);
            assertEquals(index,chosen.tone);
            assertEquals(index,chosen.duplicateAs(7).tone);
            assertEquals(index,chosen.withEnabled(true).tone);
        }
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsUnknownToneIndex() {
        new AlarmProfile(1,"Unsicher",true,7,0,127,10,60,55,6,5,5,ToneCatalog.count());
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsNoWeekday() {
        new AlarmProfile(1, "X", true, 7, 0, 0, 0, 300, 30, 5, 20, 5, 0);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsUnsafeHighDelay() {
        new AlarmProfile(1, "X", true, 7, 0, 1, 4000, 300, 30, 5, 20, 5, 0);
    }
    @Test public void watchIntensityIsOptInAndSurvivesEveryProfileOperation(){
        AlarmProfile prior=AlarmProfile.defaults(1);
        assertEquals(WatchIntensity.NORMAL,prior.watchIntensity);
        AlarmProfile sanft=new AlarmProfile(prior.id,prior.name,prior.enabled,
                prior.hour,prior.minute,prior.days,prior.delaySeconds,
                prior.rampSeconds,prior.maxVolume,prior.watchPulses,
                prior.watchIntervalSeconds,prior.snoozeMinutes,prior.tone,
                prior.rampEnabled,prior.autoStopMinutes,prior.skipAt,prior.oneTimeAt,
                prior.replacedAt,prior.snoozeEnabled,prior.vibrationMode,
                prior.audibleWhenMuted,prior.cascadeEnabled,WatchIntensity.SANFT);
        assertFalse(prior.sameConfiguration(sanft));
        assertEquals(WatchIntensity.SANFT,sanft.withEnabled(true).watchIntensity);
        assertEquals(WatchIntensity.SANFT,sanft.withExceptions(0,0,0).watchIntensity);
        assertEquals(WatchIntensity.SANFT,sanft.duplicateAs(2).watchIntensity);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidWatchIntensity(){
        AlarmProfile old=AlarmProfile.defaults(1);
        new AlarmProfile(old.id,old.name,old.enabled,old.hour,old.minute,
                old.days,old.delaySeconds,old.rampSeconds,old.maxVolume,
                old.watchPulses,old.watchIntervalSeconds,old.snoozeMinutes,
                old.tone,old.rampEnabled,old.autoStopMinutes,old.skipAt,
                old.oneTimeAt,old.replacedAt,old.snoozeEnabled,old.vibrationMode,
                old.audibleWhenMuted,old.cascadeEnabled,99);
    }

}
