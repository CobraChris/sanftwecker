package com.cobrachris.sanftwecker;

/** Per-alarm Wear OS haptic intensity. Existing alarms retain their v27 rhythm. */
public final class WatchIntensity {
    public static final int SANFT=0, NORMAL=1, INTENSIV=2;
    public static final String[] LABELS={"Sanft – sehr leiser Start","Normal – bisheriges Verhalten","Intensiv – dicht und kräftig"};
    private WatchIntensity(){}
    public static boolean valid(int mode){return mode>=SANFT&&mode<=INTENSIV;}
}
