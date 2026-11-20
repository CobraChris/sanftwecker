package com.cobrachris.sanftwecker;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Watch timer command, deliberately separate from normal alarm protocol. */
public final class TimerWatchPacket {
    public static final String PREFIX="/sanftwecker/timer/v1";
    public static final String START=PREFIX+"/start";
    public static final String STOP=PREFIX+"/stop";
    public static final String WATCH_STOP=PREFIX+"/watch-stop";
    public static final String WATCH_EXTEND=PREFIX+"/watch-extend";
    public static final String EXTEND_RESULT=PREFIX+"/extend-result";
    public static final String STATE=PREFIX+"/state/";
    public static final long MAX_AGE_MS=180_000L;
    private TimerWatchPacket(){}

    public static boolean validId(int id){return id>0&&id<=9999;}
    public static boolean validWhen(long when,long now){
        return when>0&&when>=now-MAX_AGE_MS&&when<=now+15_000L;
    }
    public static String start(int id,long when,boolean vibrate,String label){
        if(!validId(id)||when<=0)throw new IllegalArgumentException("Invalid timer watch ID");
        String safe=label==null?"Timer":label.trim();
        if(safe.isEmpty())safe="Timer";
        if(safe.length()>80)safe=safe.substring(0,80);
        String encoded=Base64.getUrlEncoder().withoutPadding().encodeToString(
                safe.getBytes(StandardCharsets.UTF_8));
        return id+":"+when+":"+(vibrate?"1":"0")+":"+encoded;
    }
    public static String stop(int id,long when){
        if(!validId(id)||when<=0)throw new IllegalArgumentException("Invalid timer stop");
        return id+":"+when;
    }
}
