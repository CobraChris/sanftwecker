package com.cobrachris.watchvibrationpoc;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Wear OS round-screen dashboard: read-only, future watch-local alarms.
 * Actions and alarm scheduling remain in their existing independent services.
 */
public final class WatchDashboardView extends LinearLayout {
    private static final int PANEL=Color.rgb(32,35,46);
    private static final int ROW=Color.rgb(43,47,61);
    private static final int TEXT=Color.rgb(239,241,249);
    private static final int DIM=Color.rgb(170,179,197);
    private static final int BLUE=Color.rgb(157,192,255);
    private static final int ALERT=Color.rgb(255,172,150);
    private static final int BG=Color.rgb(16,18,24);

    private final TextView nextTime,nextName,nextDate,countLabel;
    private final LinearLayout entries;
    private String lastSignature="";

    public WatchDashboardView(Context context){
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setPadding(0,dp(context,6),0,dp(context,4));

        LinearLayout hero=panel(context,PANEL,24,16,15);
        hero.addView(label(context,"NÄCHSTER WECKER",12,BLUE,true));
        nextTime=label(context,"--:--",43,TEXT,true);
        nextTime.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);
        nextTime.setAutoSizeTextTypeUniformWithConfiguration(
                26,43,1,TypedValue.COMPLEX_UNIT_SP);
        nextTime.setSingleLine(true);
        nextTime.setMinHeight(dp(context,60));
        hero.addView(nextTime);
        nextName=label(context,"",15,TEXT,true);
        nextName.setMaxLines(2);
        hero.addView(nextName);
        nextDate=label(context,"",12,DIM,false);
        hero.addView(nextDate);
        addView(hero,separated(context,14));

        countLabel=label(context,"WECKER AUF DER UHR",12,BLUE,true);
        addView(countLabel,separated(context,8));
        entries=new LinearLayout(context);
        entries.setOrientation(VERTICAL);
        addView(entries,new LayoutParams(-1,-2));

        TextView hint=label(context,
                "Hier stehen die auf der Uhr geplanten Wecker. "+
                "Weitere Einstellungen und deaktivierte Wecker bleiben am Handy.",
                12,DIM,false);
        hint.setPadding(dp(context,4),dp(context,8),dp(context,4),dp(context,4));
        addView(hint,separated(context,8));
    }

    public static int backgroundColor(){return BG;}

    private static int dp(Context c,int value){
        return Math.round(value*c.getResources().getDisplayMetrics().density);
    }
    private static GradientDrawable shape(Context c,int color,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c,radius));
        return d;
    }
    private static LinearLayout panel(Context c,int color,int radius,int h,int v){
        LinearLayout l=new LinearLayout(c);
        l.setOrientation(VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setPadding(dp(c,h),dp(c,v),dp(c,h),dp(c,v));
        l.setBackground(shape(c,color,radius));
        return l;
    }
    private static LayoutParams separated(Context c,int bottom){
        LayoutParams p=new LayoutParams(-1,-2);
        p.bottomMargin=dp(c,bottom);
        return p;
    }
    private static TextView label(Context c,String text,int size,int color,boolean bold){
        TextView view=new TextView(c);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        if(bold)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return view;
    }
    public static void styleButton(Button b,boolean primary,boolean critical){
        Context c=b.getContext();
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(primary||critical?BG:TEXT);
        b.setBackground(shape(c,critical?ALERT:primary?BLUE:ROW,20));
        b.setMinHeight(dp(c,52));
        b.setPadding(dp(c,8),dp(c,8),dp(c,8),dp(c,8));
    }
    private static String time(long when){
        return new SimpleDateFormat("HH:mm",Locale.GERMANY).format(new Date(when));
    }
    private static String day(long when){
        Calendar today=Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY,0);
        today.set(Calendar.MINUTE,0);
        today.set(Calendar.SECOND,0);
        today.set(Calendar.MILLISECOND,0);
        long midnight=today.getTimeInMillis();
        today.add(Calendar.DAY_OF_YEAR,1);
        long tomorrow=today.getTimeInMillis();
        today.add(Calendar.DAY_OF_YEAR,1);
        long afterTomorrow=today.getTimeInMillis();
        if(when>=midnight&&when<tomorrow)return "Heute";
        if(when>=tomorrow&&when<afterTomorrow)return "Morgen";
        return new SimpleDateFormat("EEE, dd.MM.",Locale.GERMANY)
                .format(new Date(when));
    }

    public void invalidateSnapshot(){lastSignature="";}
    public void render(List<WatchAgenda.Entry> items){
        Calendar today=Calendar.getInstance();
        StringBuilder signature=new StringBuilder()
                .append(today.get(Calendar.YEAR)).append(':')
                .append(today.get(Calendar.DAY_OF_YEAR)).append(':')
                .append(today.getTimeZone().getID()).append('|');
        for(WatchAgenda.Entry a:items)
            signature.append(a.slot).append(':').append(a.when)
                    .append(':').append(a.name).append('|');
        if(signature.toString().equals(lastSignature))return;
        lastSignature=signature.toString();
        entries.removeAllViews();

        int total=items.size();
        countLabel.setText(total==1?"1 WECKER AUF DER UHR":
                total+" WECKER AUF DER UHR");
        if(total==0){
            nextTime.setText("--:--");
            nextName.setText("Noch nichts geplant");
            nextDate.setText("Kein kommender Wecker auf der Uhr.\n"+
                    "Falls am Handy einer aktiv ist: Synchronisierung prüfen.");
            return;
        }

        WatchAgenda.Entry next=items.get(0);
        nextTime.setText(time(next.when));
        nextName.setText(next.name);
        nextDate.setText(day(next.when)+" · "+
                new SimpleDateFormat("dd.MM.yyyy",Locale.GERMANY)
                        .format(new Date(next.when)));
        for(WatchAgenda.Entry a:items){
            LinearLayout row=panel(getContext(),ROW,18,12,10);
            TextView hour=label(getContext(),time(a.when),25,TEXT,true);
            hour.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);
            row.addView(hour);
            TextView name=label(getContext(),a.name,14,TEXT,false);
            name.setMaxLines(2);
            row.addView(name);
            row.addView(label(getContext(),day(a.when),12,DIM,false));
            row.setContentDescription(a.name+", "+time(a.when)+", "+
                    day(a.when)+", auf der Uhr geplanter Wecker");
            entries.addView(row,separated(getContext(),9));
        }
    }
}
