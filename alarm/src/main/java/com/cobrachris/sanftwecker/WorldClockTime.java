package com.cobrachris.sanftwecker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Civil time and date information; uses platform zone rules including DST. */
public final class WorldClockTime {
    private static final DateTimeFormatter CLOCK=DateTimeFormatter.ofPattern("HH:mm",Locale.GERMANY);
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("EEE, dd.MM.",Locale.GERMANY);
    public final String clock,date,utcOffset,dayDifference;
    public final long dateDelta;
    private WorldClockTime(String clock,String date,String utcOffset,
            String dayDifference,long dateDelta){
        this.clock=clock;this.date=date;this.utcOffset=utcOffset;
        this.dayDifference=dayDifference;this.dateDelta=dateDelta;
    }

    public static WorldClockTime at(String zone,long epochMillis,ZoneId localZone){
        ZoneId target=ZoneId.of(zone);
        Instant instant=Instant.ofEpochMilli(epochMillis);
        ZonedDateTime city=instant.atZone(target);
        LocalDate local=instant.atZone(localZone).toLocalDate();
        long delta=ChronoUnit.DAYS.between(local,city.toLocalDate());
        String day=delta==0?"Heute":delta==-1?"Gestern":delta==1?"Morgen":
                delta<0?"Vor "+(-delta)+" Tagen":"In "+delta+" Tagen";
        int offsetSeconds=city.getOffset().getTotalSeconds();
        int abs=Math.abs(offsetSeconds);
        String suffix=(abs/3600)+((abs%3600)==0?"":String.format(Locale.ROOT,
                ":%02d",(abs%3600)/60));
        String utc="UTC"+(offsetSeconds>=0?"+":"−")+suffix;
        return new WorldClockTime(CLOCK.format(city),DATE.format(city),utc,day,delta);
    }
}
