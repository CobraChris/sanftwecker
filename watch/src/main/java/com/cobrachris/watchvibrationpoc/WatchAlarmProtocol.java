package com.cobrachris.watchvibrationpoc;

/** Shared wire values must match the protected phone-side WearAlarmBridge. */
public final class WatchAlarmProtocol {
    public static final String PREFIX="/sanftwecker/alarm/v1";
    public static final String SCHEDULE=PREFIX+"/schedule/";
    public static final String STATE=PREFIX+"/state";
    public static final String RING=PREFIX+"/ring";
    public static final String STOP=PREFIX+"/stop";
    public static final String SNOOZE=PREFIX+"/snooze";
    public static final String ACK=PREFIX+"/ack";
    public static final int TEST_SLOT=1001,SNOOZE_SLOT=1002;
    private WatchAlarmProtocol(){}

    public static final class Packet {
        public final int id,minutes,snoozeMinutes;
        public final long when;
        public final boolean cascade;
        Packet(int id,long when,int minutes,boolean cascade,int snoozeMinutes){
            this.id=id;this.when=when;this.minutes=minutes;
            this.cascade=cascade;this.snoozeMinutes=snoozeMinutes;
        }
    }
    public static Packet parse(String value){
        if(value==null||!value.matches("[1-9][0-9]{0,3}:[1-9][0-9]{11,12}:[1-9][0-9]?(?::1|:[01]:([0-9]|[12][0-9]|30))?"))
            return null;
        try{
            String[] split=value.split(":");
            int id=Integer.parseInt(split[0]),minutes=Integer.parseInt(split[2]);
            long when=Long.parseLong(split[1]);
            if(id>1000||minutes>15)return null;
            boolean cascade=split.length==5 ? "1".equals(split[3]):split.length==4;
            int snooze=split.length==5 ? Integer.parseInt(split[4]):0;
            return new Packet(id,when,minutes,cascade,snooze);
        }catch(NumberFormatException e){return null;}
    }
    public static boolean nearTrigger(long when,long now){
        return when>0&&when>=now-90_000L&&when<=now+15_000L;
    }
    public static boolean validSlot(int slot){return slot>=1&&slot<=1002;}
    public static boolean validWhen(long when,long now){
        return when>now+1000L&&when<=now+366L*24*60*60*1000;
    }
}