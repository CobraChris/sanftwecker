package com.cobrachris.watchvibrationpoc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Fully separate from the clock-alarm protocol and its 1–1000 alarm IDs. */
public final class WatchTimerProtocol {
    public static final String PREFIX="/sanftwecker/timer/v1";
    public static final String START=PREFIX+"/start";
    public static final String STOP=PREFIX+"/stop";
    public static final String WATCH_STOP=PREFIX+"/watch-stop";
    public static final String STATE=PREFIX+"/state/";
    public static final long MAX_AGE_MS=180_000L;

    public static final class Packet {
        public final int id;
        public final long when;
        public final boolean vibration;
        public final String name;
        Packet(int id,long when,boolean vibration,String name){
            this.id=id;this.when=when;this.vibration=vibration;this.name=name;
        }
    }
    private WatchTimerProtocol(){}

    public static boolean validWhen(long when,long now){
        return when>0&&when>=now-MAX_AGE_MS&&when<=now+15_000L;
    }
    public static Packet start(String text){
        if(text==null||text.length()>350 ||
                !text.matches("[1-9][0-9]{0,3}:[1-9][0-9]{11,12}:[01]:[A-Za-z0-9_-]{0,220}"))
            return null;
        try{
            String[] parts=text.split(":",4);
            int id=Integer.parseInt(parts[0]);
            long when=Long.parseLong(parts[1]);
            if(id>9999)return null;
            String name=new String(Base64.getUrlDecoder().decode(parts[3]),
                    StandardCharsets.UTF_8);
            if(name.length()>80)return null;
            return new Packet(id,when,"1".equals(parts[2]),
                    name.trim().isEmpty()?"Timer":name);
        }catch(RuntimeException ex){return null;}
    }
    public static Packet stop(String text){
        if(text==null||!text.matches("[1-9][0-9]{0,3}:[1-9][0-9]{11,12}"))
            return null;
        try{
            String[] parts=text.split(":");
            int id=Integer.parseInt(parts[0]);
            if(id>9999)return null;
            return new Packet(id,Long.parseLong(parts[1]),false,"Timer");
        }catch(RuntimeException ex){return null;}
    }
}
