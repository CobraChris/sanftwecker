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
    @Test public void screenshotAlarmsCopyEveryPreferenceFromSavedTestwecker(){
        AlarmProfile reference=new AlarmProfile(1,"Testwecker",false,10,11,
                0x1f,0,180,88,12,3,5,2,
                true,30,0,0,0,true,3,true,true,WatchIntensity.SANFT);
        AlarmProfile imported=new AlarmProfile(40,"6:45 Uhr bei Gero los",
                false,5,30,0x7f,10,60,55,6,5,5,0,
                true,20,0,0,0,true,1,false,false,WatchIntensity.NORMAL);
        AlarmProfile fixed=imported.withSettingsFrom(reference);
        assertEquals(imported.id,fixed.id);
        assertEquals(imported.name,fixed.name);
        assertEquals(imported.hour,fixed.hour);
        assertEquals(imported.minute,fixed.minute);
        assertFalse(fixed.enabled);
        assertEquals(reference.days,fixed.days);
        assertEquals(reference.delaySeconds,fixed.delaySeconds);
        assertEquals(reference.rampSeconds,fixed.rampSeconds);
        assertEquals(reference.maxVolume,fixed.maxVolume);
        assertEquals(reference.watchPulses,fixed.watchPulses);
        assertEquals(reference.watchIntervalSeconds,fixed.watchIntervalSeconds);
        assertEquals(reference.watchIntensity,fixed.watchIntensity);
        assertEquals(reference.snoozeMinutes,fixed.snoozeMinutes);
        assertEquals(reference.tone,fixed.tone);
        assertEquals(reference.rampEnabled,fixed.rampEnabled);
        assertEquals(reference.autoStopMinutes,fixed.autoStopMinutes);
        assertEquals(reference.snoozeEnabled,fixed.snoozeEnabled);
        assertEquals(reference.vibrationMode,fixed.vibrationMode);
        assertEquals(reference.audibleWhenMuted,fixed.audibleWhenMuted);
        assertEquals(reference.cascadeEnabled,fixed.cascadeEnabled);
        assertEquals(imported.skipAt,fixed.skipAt);
        assertEquals(imported.oneTimeAt,fixed.oneTimeAt);
        assertEquals(imported.replacedAt,fixed.replacedAt);
        assertTrue(fixed.sameConfiguration(fixed.withSettingsFrom(reference)));
        assertEquals(6,imported.watchPulses); // Existing objects stay immutable
        assertEquals(12,reference.watchPulses);
    }

    @Test public void copyingSettingsCannotActivateASleepingAlarm(){
        AlarmProfile original=AlarmProfile.defaults(17);
        AlarmProfile activeReference=AlarmProfile.defaults(1).withEnabled(true);
        AlarmProfile copied=original.withSettingsFrom(activeReference);
        assertFalse(copied.enabled);
        assertEquals(original.id,copied.id);
        assertEquals(original.name,copied.name);
        assertEquals(original.hour,copied.hour);
        assertEquals(original.minute,copied.minute);
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
