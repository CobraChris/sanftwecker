package com.cobrachris.sanftwecker;

import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative German offline time parser. Ambiguous phrases produce no
 * timestamp and require explicit selection in the editor.
 */
public final class ReminderVoiceParser {
    public static final class Parsed {
        public final String title;
        public final long due;
        Parsed(String title,long due){this.title=title;this.due=due;}
    }
    private ReminderVoiceParser(){}
    public static Parsed parse(String spoken,long now){
        if(spoken==null)return new Parsed("",0);
        String raw=spoken.trim();
        String lower=raw.toLowerCase(Locale.GERMANY);
        Calendar time=Calendar.getInstance(Locale.GERMANY);
        time.setTimeInMillis(now);
        boolean dateKnown=false;
        Matcher relative=Pattern.compile("in (\\d{1,3}) (minuten?|stunden?|tagen?)").matcher(lower);
        long due=0;
        String title=raw.replaceFirst("(?i)^erinnere mich( bitte)?( daran)?(,)?\\s*","").trim();
        if(relative.find()){
            long number=Long.parseLong(relative.group(1));
            String unit=relative.group(2);
            long step=unit.startsWith("min")?60_000L:
                    unit.startsWith("stund")?3_600_000L:86_400_000L;
            due=now+number*step;
            title=title.replaceFirst("(?i)\\bin \\d{1,3} (minuten?|stunden?|tagen?)\\b","").trim();
        }else{
            if(lower.contains("übermorgen")){time.add(Calendar.DAY_OF_YEAR,2);dateKnown=true;}
            else if(lower.contains("morgen")){time.add(Calendar.DAY_OF_YEAR,1);dateKnown=true;}
            else if(lower.contains("heute"))dateKnown=true;
            Matcher at=Pattern.compile("(?:um |gegen )(\\d{1,2})(?::(\\d{2}))?\\s*(?:uhr)?").matcher(lower);
            if(at.find()){
                int hour=Integer.parseInt(at.group(1));
                int minute=at.group(2)==null?0:Integer.parseInt(at.group(2));
                if(hour<24&&minute<60){
                    time.set(Calendar.HOUR_OF_DAY,hour);time.set(Calendar.MINUTE,minute);
                    time.set(Calendar.SECOND,0);time.set(Calendar.MILLISECOND,0);
                    // "um 17 Uhr" means the next occurrence, not an expired date.
                    if(!dateKnown&&time.getTimeInMillis()<=now)time.add(Calendar.DAY_OF_YEAR,1);
                    due=time.getTimeInMillis();
                }
            }
            title=title.replaceFirst("(?i)\\b(heute|morgen|übermorgen)\\b","")
                .replaceFirst("(?i)\\b(?:um|gegen) \\d{1,2}(?::\\d{2})?\\s*(?:uhr)?\\b","").trim();
        }
        title=title.replaceFirst("(?i)^daran,?\\s*","")
            .replaceFirst("(?i)^dass ich\\s+","").replaceFirst("(?i)^zu\\s+","").trim();
        if(title.isEmpty())title=raw;
        return new Parsed(title,due>now?due:0);
    }
}