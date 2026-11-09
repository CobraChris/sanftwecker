package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** A proper per-alarm picker; previewing a choice does NOT save or alter the alarm. */
public final class TonePickerActivity extends Activity {
    public static final String EXTRA_SELECTED="selected_tone";
    public static final String EXTRA_ALARM_ID="alarm_id";
    private int selected,alarmId,previewing=-1;
    private final LinearLayout[] rows=new LinearLayout[ToneCatalog.LABELS.length];
    private final TextView[] marks=new TextView[ToneCatalog.LABELS.length];
    private final TextView[] plays=new TextView[ToneCatalog.LABELS.length];
    private TextView hint;
    private int dp(int n){return UiKit.dp(this,n);}

    private GradientDrawable pill(int fill,int stroke,int radius,int thickness){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(thickness),stroke);
        return d;
    }
    private TextView label(String value,int size,int color){
        TextView t=new TextView(this);
        t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(false);
        return t;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        UiKit.window(this);
        selected=ToneCatalog.safe(getIntent().getIntExtra(EXTRA_SELECTED,0));
        alarmId=getIntent().getIntExtra(EXTRA_ALARM_ID,-1);
        if(alarmId<1||alarmId>AlarmProfile.MAX_ALARMS){
            finish();return;
        }
        build();
    }
    private void build(){
        FrameLayout base=new FrameLayout(this);
        base.addView(new AlarmBackdrop(this),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout main=new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(18),dp(20),dp(18),dp(16));
        base.addView(main,new FrameLayout.LayoutParams(-1,-1));

        TextView back=label("‹   SANFTWECKER",18,0xffd2f5f4);
        back.setMinHeight(dp(45));
        back.setContentDescription("Zurück ohne Übernahme");
        main.addView(back);
        back.setOnClickListener(v->finish());

        TextView title=label("Klingelton auswählen",27,Color.WHITE);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(52));
        tp.topMargin=dp(4);
        main.addView(title,tp);
        TextView subtitle=label("Wähle den Ton für deinen Sanftwecker.",14,0xffb5dbdc);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(44));
        main.addView(subtitle,sp);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(3),dp(2),dp(3),dp(6));
        scroll.addView(list);
        main.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));

        for(int i=0;i<ToneCatalog.count();i++){
            final int index=i;
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12),dp(7),dp(14),dp(7));
            row.setMinimumHeight(dp(76));
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(82));
            rp.topMargin=dp(7);
            list.addView(row,rp);
            rows[i]=row;

            TextView play=label("▶",21,Color.WHITE);
            play.setGravity(Gravity.CENTER);
            play.setBackground(pill(0x445fb0ac,0x0088eeee,30,1));
            play.setContentDescription("Hörprobe: "+ToneCatalog.LABELS[i]);
            row.addView(play,new LinearLayout.LayoutParams(dp(50),dp(50)));
            plays[i]=play;
            play.setOnClickListener(v->preview(index));

            LinearLayout caption=new LinearLayout(this);
            caption.setOrientation(LinearLayout.VERTICAL);
            caption.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1f);
            cp.leftMargin=dp(13);
            row.addView(caption,cp);
            TextView name=label(ToneCatalog.LABELS[i],17,Color.WHITE);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            caption.addView(name);
            TextView desc=label(ToneCatalog.DESCRIPTIONS[i],11,0xffb6d4d5);
            desc.setSingleLine(true);
            desc.setEllipsize(android.text.TextUtils.TruncateAt.END);
            caption.addView(desc);

            TextView mark=label("○",28,0xffc3d6d8);
            mark.setGravity(Gravity.CENTER);
            row.addView(mark,new LinearLayout.LayoutParams(dp(35),dp(54)));
            marks[i]=mark;
            row.setContentDescription(ToneCatalog.LABELS[i]+". "+ToneCatalog.DESCRIPTIONS[i]);
            row.setOnClickListener(v->{selected=index;refresh();});
        }
        hint=label("▶ für Hörprobe · Auswahl wird erst mit Übernehmen gespeichert",
                12,0xffb9d9dc);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(36));
        main.addView(hint,hp);
        Button apply=new Button(this);
        apply.setAllCaps(false);
        apply.setText("Übernehmen");
        apply.setTextSize(19);
        apply.setTextColor(Color.WHITE);
        apply.setBackground(pill(0x7740a39a,0xff5af9cf,36,2));
        main.addView(apply,new LinearLayout.LayoutParams(-1,dp(62)));
        apply.setOnClickListener(v->{
            Intent chosen=new Intent().putExtra(EXTRA_SELECTED,selected);
            setResult(RESULT_OK,chosen);
            finish();
        });
        refresh();
        setContentView(base);
    }

    private void refresh(){
        for(int i=0;i<rows.length;i++){
            if(rows[i]==null)continue;
            boolean on=i==selected;
            rows[i].setBackground(pill(on?0x67416c70:0x4220454a,
                    on?0xff57edca:0x6e9ac1c2,28,on?2:1));
            marks[i].setText(on?"◉":"○");
            marks[i].setTextColor(on?0xff55f4d0:0xffb9cbd2);
            plays[i].setText(previewing==i?"■":"▶");
        }
    }

    private void preview(int index){
        if(previewing==index){
            stopPreview();return;
        }
        stopPreview();
        try {
            Intent i=new Intent(this,RingingService.class)
                    .setAction(RingingService.START)
                    .putExtra(AlarmScheduler.EXTRA_ID,alarmId)
                    .putExtra("tone_only_preview",true)
                    .putExtra("tone_preview_index",index);
            startForegroundService(i);
            previewing=index;
            hint.setText("Hörprobe: "+ToneCatalog.LABELS[index]+
                    " · max. 20 Sekunden · mit ■ stoppen");
        }catch(RuntimeException ex){
            hint.setText("Hörprobe derzeit nicht möglich.");
        }
        refresh();
    }

    private void stopPreview(){
        if(previewing<0)return;
        try{startService(new Intent(this,RingingService.class)
                .setAction(RingingService.STOP_PREVIEW));}
        catch(RuntimeException ignored){}
        previewing=-1;
        if(hint!=null)hint.setText(
                "▶ für Hörprobe · Auswahl wird erst mit Übernehmen gespeichert");
        refresh();
    }
    @Override protected void onPause(){stopPreview();super.onPause();}
    @Override protected void onDestroy(){stopPreview();super.onDestroy();}
}
