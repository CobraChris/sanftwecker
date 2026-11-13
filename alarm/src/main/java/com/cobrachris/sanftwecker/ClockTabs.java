package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared native navigation: independent pages, consistent dark appearance. */
public final class ClockTabs {
    public static final int ALARMS=0, TIMERS=1, STOPWATCH=2, WORLD_CLOCK=3;
    private static final String BAR_MARKER="sanftwecker-clock-navigation";
    private ClockTabs(){}
    public static void add(Activity c,LinearLayout parent,int selected){
        LinearLayout bar=UiKit.row(c);
        bar.setTag(BAR_MARKER);
        String[] names={"Wecker","Timer","Stoppuhr","Weltuhr"};
        Class<?>[] targets={MainActivity.class,TimerActivity.class,
                StopwatchActivity.class,WorldClockActivity.class};
        for(int i=0;i<names.length;i++){
            final int index=i;
            TextView tab=UiKit.label(c,names[i],14,
                    i==selected?UiKit.BLUE:Color.rgb(194,200,219));
            tab.setTypeface(Typeface.DEFAULT,i==selected?Typeface.BOLD:Typeface.NORMAL);
            tab.setGravity(Gravity.CENTER);
            tab.setMinHeight(UiKit.dp(c,52));
            tab.setBackground(UiKit.shape(i==selected?UiKit.CARD_2:UiKit.BG,c,12));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,UiKit.dp(c,52),1);
            p.setMargins(UiKit.dp(c,1),UiKit.dp(c,8),UiKit.dp(c,1),UiKit.dp(c,12));
            bar.addView(tab,p);
            tab.setContentDescription(names[i]+(i==selected?" ausgewählt":" öffnen"));
            tab.setOnClickListener(v->{
                if(index==selected)return;
                Intent open=new Intent(c,targets[index])
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                c.startActivity(open);
                c.finish();
            });
        }
        parent.addView(bar);
    }

    /**
     * Keep all four navigation tabs visible while the page scrolls.
     * Existing screens still call add() in the usual position; pin()
     * relocates that bar once and leaves the rest of the page untouched.
     */
    public static View pin(Activity c,LinearLayout scrollColumn,View scrollArea){
        LinearLayout bar=null;
        for(int i=0;i<scrollColumn.getChildCount();i++){
            View child=scrollColumn.getChildAt(i);
            if(BAR_MARKER.equals(child.getTag()) && child instanceof LinearLayout){
                bar=(LinearLayout)child;
                break;
            }
        }
        if(bar==null)throw new IllegalStateException("Clock navigation missing");
        scrollColumn.removeView(bar);
        bar.setPadding(UiKit.dp(c,17),0,UiKit.dp(c,17),0);
        LinearLayout frame=UiKit.column(c);
        frame.setBackgroundColor(UiKit.BG);
        frame.addView(bar,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        frame.addView(scrollArea,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
        return frame;
    }
}
