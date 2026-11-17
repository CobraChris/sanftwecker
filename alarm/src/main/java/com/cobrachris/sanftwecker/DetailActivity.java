package com.cobrachris.sanftwecker;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Dark alarm details; time confirmation saves immediately, other edits remain explicit. */
public final class DetailActivity extends Activity {
    private static final int NOTIFY_REQ=18;
    private static final int TONE_PICKER_REQ=24;
    private final String[] weekdays={"M","D","M","D","F","S","S"};
    private final String[] weekdayNames={"Montag","Dienstag","Mittwoch",
            "Donnerstag","Freitag","Samstag","Sonntag"};
    private int id,hour,minute,selectedTone;
    private boolean loading,dirty;
    private AlarmProfile saved,undoTimeBefore,undoTimeAfter;
    private EditText name;
    private TextView clock,status,advanced,nextDisplay,timeSavedLabel,timeUndoAction;
    private LinearLayout timeUndoRow;
    private TextView toneSummary,watchSummary,snoozeSummary,modeDescription;
    private LinearLayout manualToneOptions;
    private Button tonePicker;
    private final TextView[] chips=new TextView[7];
    private final boolean[] days=new boolean[7];
    private Switch enabled,rising,watch,snoozeEnabled,audibleWhenMuted,cascade;
    private Spinner vibration,watchIntensity;
    private SeekBar delay,ramp,volume,pulses,interval,snooze,autoStop;
    private TextView delayLabel,rampLabel,volumeLabel,pulsesLabel,intervalLabel,snoozeLabel,stopLabel;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        UiKit.window(this);
        id=getIntent().getIntExtra(AlarmScheduler.EXTRA_ID,-1);
        saved=AlarmRepository.get(this,id);
        if(saved==null){finish();return;}
        RingingService.ensureChannels(this);
        build();
        load(saved);
    }
    @Override public void onResume(){
        super.onResume();
        if(status!=null)diagnostics();
    }
    private TextView label(String s,int size){return UiKit.label(this,s,size,UiKit.TEXT);}
    private LinearLayout section(LinearLayout root,String title){
        LinearLayout card=UiKit.card(this);
        card.addView(UiKit.heading(this,title));
        UiKit.placeCard(this,root,card);
        return card;
    }
    private void rowSwitch(LinearLayout into,String text,Switch control){
        LinearLayout row=UiKit.row(this);
        row.addView(label(text,17),UiKit.weight());
        row.addView(control);
        into.addView(row);
    }
    private SeekBar slider(LinearLayout card,int max){
        SeekBar s=UiKit.seek(this,max);
        s.setMinHeight(UiKit.dp(this,48));
        card.addView(s);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean user){
                updateLabels();if(user)changed();
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        return s;
    }
    /** Sections stay mounted while collapsed: stored values and listeners are never lost. */
    private LinearLayout foldSection(LinearLayout root,String title,TextView summary){
        LinearLayout card=UiKit.card(this);
        card.setPadding(UiKit.dp(this,16),UiKit.dp(this,12),
                UiKit.dp(this,16),UiKit.dp(this,12));
        LinearLayout header=UiKit.row(this);
        LinearLayout labels=UiKit.column(this);
        labels.addView(UiKit.heading(this,title));
        if(summary!=null)labels.addView(summary);
        header.addView(labels,UiKit.weight());
        TextView arrow=UiKit.action(this,"⌄");
        // Only the complete row is announced as an expandable action.
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(arrow);
        LinearLayout content=UiKit.column(this);
        content.setVisibility(View.GONE);
        header.setClickable(true);
        header.setFocusable(true);
        header.setContentDescription(title+" öffnen");
        header.setOnClickListener(v->{
            boolean open=content.getVisibility()!=View.VISIBLE;
            content.setVisibility(open?View.VISIBLE:View.GONE);
            arrow.setText(open?"⌃":"⌄");
            String action=title+(open?" schließen":" öffnen");
            header.setContentDescription(action);
            arrow.setContentDescription(action);
        });
        card.addView(header);
        card.addView(content);
        UiKit.placeCard(this,root,card);
        return content;
    }

    private void refreshModeUi(){
        if(cascade==null)return;
        boolean gentle=cascade.isChecked();
        if(manualToneOptions!=null)
            manualToneOptions.setVisibility(gentle?View.GONE:View.VISIBLE);
        if(modeDescription!=null)modeDescription.setText(gentle
                ?"Erste Minute sanfter Ton und leichte Watch-Impulse (falls aktiviert). "+
                  "Danach kräftigere Watch-Impulse und Handyvibration."
                :"Individuell: Tonstart und Lautstärkeanstieg selbst einstellen.");
        updateSummaries();
    }

    private void updateSummaries(){
        if(toneSummary!=null && selectedTone>=0 && selectedTone<ToneCatalog.LABELS.length)
            toneSummary.setText(ToneCatalog.LABELS[selectedTone]+
                    (cascade!=null&&cascade.isChecked()?" · sanfte 2-Stufen-Kaskade":" · individuell"));
        if(watchSummary!=null && watch!=null && pulses!=null && interval!=null)
            watchSummary.setText(watch.isChecked()
                    ?"Aktiv · "+pulses.getProgress()+" Impulse · "+
                    WatchIntervals.secondsAt(interval.getProgress())+" Sek. Abstand"
                    :"Deaktiviert");
        if(snoozeSummary!=null && snoozeEnabled!=null && snooze!=null && autoStop!=null)
            snoozeSummary.setText((snoozeEnabled.isChecked()
                    ?(snooze.getProgress()+1)+" Min. schlummern":"Schlummern aus")+
                    " · Auto-Stopp "+(autoStop.getProgress()+1)+" Min.");
    }

    private void applyDayState(int index) {
        UiKit.setChip(this,chips[index],days[index]);
        chips[index].setSelected(days[index]);
        chips[index].setContentDescription(weekdayNames[index] +
                (days[index] ? " ausgewählt" : " nicht ausgewählt"));
    }

    private void build(){
        LinearLayout root=UiKit.mainColumn(this);
        LinearLayout header=UiKit.row(this);
        TextView back=UiKit.action(this,"‹");
        back.setContentDescription("Zurück");
        back.setOnClickListener(v->back());
        header.addView(back);
        TextView pageTitle=UiKit.heading(this,"Wecker");
        pageTitle.setContentDescription("Wecker bearbeiten");
        header.addView(pageTitle,UiKit.weight());
        TextView overflow=UiKit.action(this,"⋮");
        overflow.setContentDescription("Weitere Aktionen");
        overflow.setOnClickListener(v->more(overflow));
        header.addView(overflow);
        root.addView(header);

        LinearLayout top=UiKit.card(this);
        top.setPadding(UiKit.dp(this,16),UiKit.dp(this,12),UiKit.dp(this,16),UiKit.dp(this,12));
        name=new EditText(this);
        name.setSingleLine(true);name.setTextSize(19);name.setTextColor(UiKit.TEXT);
        name.setMinHeight(UiKit.dp(this,48));
        name.setHint("Weckername");name.setHintTextColor(UiKit.MUTED);
        name.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int f){}
            public void onTextChanged(CharSequence s,int a,int b,int c){changed();}
            public void afterTextChanged(Editable e){}
        });
        top.addView(name);
        LinearLayout time=UiKit.row(this);
        clock=UiKit.label(this,"07:00",58,UiKit.BLUE);
        clock.setSingleLine(true);
        clock.setMinHeight(UiKit.dp(this,48));
        clock.setAutoSizeTextTypeUniformWithConfiguration(
                26,58,2,android.util.TypedValue.COMPLEX_UNIT_SP);
        clock.setContentDescription("Uhrzeit ändern");
        time.addView(clock,UiKit.weight());
        clock.setOnClickListener(v->new TimePickerDialog(this,(picker,h,m)->
                saveSelectedTime(h,m),hour,minute,true).show());
        enabled=UiKit.toggle(this);
        enabled.setContentDescription("Wecker aktivieren");
        enabled.setOnCheckedChangeListener((b,value)->changed());
        time.addView(enabled);
        top.addView(time);
        // Appears only after a successful instant time save, so the normal
        // detail card stays compact and the user can reverse an accidental edit.
        timeUndoRow=UiKit.row(this);
        timeSavedLabel=UiKit.label(this,"Weckzeit gespeichert",13,UiKit.MUTED);
        timeUndoRow.addView(timeSavedLabel,UiKit.weight());
        timeUndoAction=UiKit.action(this,"Rückgängig");
        timeUndoAction.setContentDescription("Automatisch gespeicherte Weckzeit rückgängig machen");
        timeUndoAction.setMinHeight(UiKit.dp(this,48));
        timeUndoAction.setOnClickListener(v->undoSavedTime());
        timeUndoRow.addView(timeUndoAction);
        timeUndoRow.setVisibility(View.GONE);
        top.addView(timeUndoRow);
        // Keep seven one-letter chips on a single row on ordinary phones.
        // Wrap only for narrow split-screen windows or enlarged system fonts.
        int columns=AlarmDetailLayoutPolicy.weekdayColumns(
                getResources().getConfiguration().screenWidthDp,
                getResources().getConfiguration().fontScale);
        int dayMin=AlarmDetailLayoutPolicy.dayMinimumWidthDp(
                getResources().getConfiguration().fontScale);
        LinearLayout daysGrid=UiKit.column(this);
        LinearLayout dayRow=null;
        for(int i=0;i<7;i++){
            if(i%columns==0){
                dayRow=UiKit.row(this);
                daysGrid.addView(dayRow);
            }
            final int which=i;
            chips[i]=UiKit.chip(this,weekdays[i],false);
            chips[i].setMinWidth(UiKit.dp(this,dayMin));
            chips[i].setMinHeight(UiKit.dp(this,AlarmDetailLayoutPolicy.DAY_HEIGHT_DP));
            chips[i].setFocusable(true);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                    0,UiKit.dp(this,AlarmDetailLayoutPolicy.DAY_HEIGHT_DP),1);
            // The final chip in a row must not consume an extra 4dp margin.
            p.rightMargin=(i%columns==columns-1 || i==6) ? 0 :
                    UiKit.dp(this,AlarmDetailLayoutPolicy.DAY_GAP_DP);
            p.bottomMargin=UiKit.dp(this,4);
            dayRow.addView(chips[i],p);
            applyDayState(i);
            chips[i].setOnClickListener(v->{
                days[which]=!days[which];
                applyDayState(which);
                changed();
            });
        }
        top.addView(daysGrid);
        nextDisplay=UiKit.label(this,"Noch nicht geplant",15,UiKit.MUTED);
        top.addView(nextDisplay);
        UiKit.placeCard(this,root,top);

        LinearLayout mode=section(root,"Weckmodus");
        mode.setPadding(UiKit.dp(this,16),UiKit.dp(this,12),
                UiKit.dp(this,16),UiKit.dp(this,12));
        cascade=UiKit.toggle(this);
        cascade.setContentDescription("Sanft in 2 Stufen oder individuell wecken");
        rowSwitch(mode,"Sanft in 2 Stufen",cascade);
        modeDescription=UiKit.label(this,"",14,UiKit.MUTED);
        mode.addView(modeDescription);
        cascade.setOnCheckedChangeListener((b,on)->{
            refreshModeUi();
            changed();
        });

        toneSummary=UiKit.label(this,"",14,UiKit.MUTED);
        LinearLayout music=foldSection(root,"Weckton & Lautstärke",toneSummary);
        tonePicker=UiKit.button(this,"Klingelton auswählen",false);
        tonePicker.setContentDescription("Klingelton auswählen und anhören");
        music.addView(tonePicker);
        tonePicker.setOnClickListener(v->{
            Intent choose=new Intent(this,TonePickerActivity.class)
                    .putExtra(TonePickerActivity.EXTRA_ALARM_ID,id)
                    .putExtra(TonePickerActivity.EXTRA_SELECTED,selectedTone);
            startActivityForResult(choose,TONE_PICKER_REQ);
        });
        manualToneOptions=UiKit.column(this);
        music.addView(manualToneOptions);
        Button recommended=UiKit.button(this,"Sanft & sicher: empfohlene Werte übernehmen",false);
        manualToneOptions.addView(recommended);
        recommended.setOnClickListener(v->new AlertDialog.Builder(this)
                .setTitle("Empfohlene Weckwerte übernehmen?")
                .setMessage("Nur für diesen Wecker: Uhr sofort benachrichtigen, "+
                        "Handyton nach 10 Sekunden, Lautstärkeanstieg über 1 Minute "+
                        "und maximal 55 % des Android-Weckerpegels. "+
                        "Erst mit Speichern werden die Werte dauerhaft übernommen.")
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Übernehmen",(dialog,which)->{
                    delay.setProgress(1);   // 10 seconds
                    ramp.setProgress(0);    // one minute
                    volume.setProgress(50); // 55 %
                    rising.setChecked(true);
                    watch.setChecked(true);
                    if(pulses.getProgress()==0) pulses.setProgress(6);
                    changed();
                }).show());
        rising=UiKit.toggle(this);
        rising.setOnCheckedChangeListener((b,value)->{
            if(ramp!=null)ramp.setEnabled(value);changed();
        });
        rowSwitch(manualToneOptions,"Ansteigende Lautstärke",rising);
        delayLabel=label("",15);manualToneOptions.addView(delayLabel);
        delay=slider(manualToneOptions,30);
        rampLabel=label("",15);manualToneOptions.addView(rampLabel);
        ramp=slider(manualToneOptions,29);
        volumeLabel=label("",15);music.addView(volumeLabel);
        volume=slider(music,95);
        audibleWhenMuted=UiKit.toggle(this);
        audibleWhenMuted.setOnCheckedChangeListener((b,on)->changed());
        rowSwitch(music,"Niedrigen Android-Weckerpegel beim Alarm anheben",audibleWhenMuted);
        music.addView(UiKit.label(this,
                "Optional, nur für diesen Wecker: Wenn die Android-Weckerlautstärke "+
                "stumm oder sehr niedrig ist, wird sie während des Alarms "+
                "vorübergehend auf etwa 75 % angehoben und danach wiederhergestellt. "+
                "Der sanfte Tonanstieg bleibt aktiv. Das übergeht 'Nicht stören' nicht: "+
                "Bitte Alarme und Sanftwecker in den Android-Ausnahmen erlauben.",
                13,UiKit.MUTED));
        Button preview=UiKit.button(this,"Tonprobe starten (20 Sek.)",false);
        music.addView(preview);
        preview.setOnClickListener(v->previewTone());

        LinearLayout vibrationCard=foldSection(root,"Handy-Vibration",null);
        vibrationCard.addView(UiKit.label(this,
                "Handy-Vibrationsmuster (Watch-Impulse separat einstellen)",15,UiKit.MUTED));
        vibration=new Spinner(this);
        vibration.setContentDescription("Vibrationsrhythmus auswählen");
        vibration.setMinimumHeight(UiKit.dp(this,48));
        vibration.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,VibrationPresets.LABELS));
        vibrationCard.addView(vibration);
        vibration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int pos,long key){
                changed();
            }
            public void onNothingSelected(AdapterView<?> parent){}
        });
        Button vibrationPreview=UiKit.button(this,"Muster am Handy fühlen",false);
        vibrationCard.addView(vibrationPreview);
        vibrationPreview.setOnClickListener(v->{
            AlertDialog stopDialog=new AlertDialog.Builder(this)
                    .setTitle("Vibrationstest")
                    .setMessage("Die Vorschau läuft maximal 5 Sekunden. "+
                            "Du kannst sie hier jederzeit beenden.")
                    .setPositiveButton("SOFORT STOPPEN",(dialog,which)->PhoneVibration.stop())
                    .create();
            stopDialog.setOnDismissListener(dialog->PhoneVibration.stop());
            stopDialog.show(); // Always show the stop control before starting haptics.
            if(!PhoneVibration.preview(this,vibration.getSelectedItemPosition())){
                stopDialog.dismiss();
                notice("Vibration nicht verfügbar oder ein Alarm ist bereits aktiv.");
                return;
            }
            new Handler(Looper.getMainLooper()).postDelayed(()->{
                if(!isFinishing() && stopDialog.isShowing())stopDialog.dismiss();
            },PhoneVibration.MAX_PREVIEW_MS+200);
        });
        vibrationCard.addView(UiKit.label(this,
                "Vorschau: maximal 5 Sekunden, mit sichtbarem Stoppknopf. "+
                "Im Alarm: kräftige Handyvibration bis zu 90 Sekunden. "+
                "Sie endet sofort bei Stoppen/Schlummern; eine Vorschau dauert maximal 5 Sekunden. "+
                "Die tatsächliche Stärke hängt von der Hardware ab.",
                13,UiKit.MUTED));

        watchSummary=UiKit.label(this,"",14,UiKit.MUTED);
        LinearLayout wrist=foldSection(root,"Galaxy Watch",watchSummary);
        watch=UiKit.toggle(this);
        watch.setOnCheckedChangeListener((b,value)->{
            if(!loading && pulses!=null){
                if(!value)pulses.setProgress(0);
                else if(pulses.getProgress()==0)pulses.setProgress(6);
                pulses.setEnabled(value);
                interval.setEnabled(value);
            }
            changed();
        });
        rowSwitch(wrist,"Uhr benachrichtigen",watch);
        wrist.addView(UiKit.label(this,
                "Watch-Vibration bei zweistufiger Kaskade (pro Wecker)",15,UiKit.MUTED));
        watchIntensity=new Spinner(this);
        watchIntensity.setContentDescription("Watch-Intensität auswählen");
        watchIntensity.setMinimumHeight(UiKit.dp(this,48));
        watchIntensity.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,WatchIntensity.LABELS));
        wrist.addView(watchIntensity);
        watchIntensity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int pos,long key){
                changed();
            }
            public void onNothingSelected(AdapterView<?> parent){}
        });
        wrist.addView(UiKit.label(this,
                "Sanft: leichter spürbarer Impuls, Normal: deutlicher Impuls, "+
                "Intensiv: kräftig. Anzahl und Abstand stellst du unten ein. "+
                "Nach 60 Sekunden beginnt die zweite Weckstufe. "+
                "Nur nach Speichern aktiv; die Stärke hängt von der Uhr ab.",13,UiKit.MUTED));
        pulsesLabel=label("",15);wrist.addView(pulsesLabel);
        pulses=slider(wrist,12);
        intervalLabel=label("",15);wrist.addView(intervalLabel);
        interval=slider(wrist,WatchIntervals.maxProgress());
        Button repeatedWatch=UiKit.button(this,
                "Voreinstellung: 12 Impulse · alle 5 Sek.",false);
        wrist.addView(repeatedWatch);
        repeatedWatch.setOnClickListener(v->{
            watch.setChecked(true);
            pulses.setProgress(12);
            interval.setProgress(WatchIntervals.progressFor(5));
            changed();
        });
        Button watchTest=UiKit.button(this,"Watch-Test senden",false);
        wrist.addView(watchTest);watchTest.setOnClickListener(v->watchTest());
        wrist.addView(UiKit.label(this,"Du kannst 3, 5, 10 bis 120 Sekunden "+
                "Abstand wählen. Sehr schnelle Impulse werden eventuell durch "+
                "Android oder Samsung gedämpft oder zusammengefasst. "+
                "Bei zweistufigen Kaskaden bestimmen Anzahl und Abstand die "+
                "kurzen Impulse in Minute 1; danach folgt die kräftigere Phase. "+
                "Die Musterstärke bleibt von der Hardware abhängig.",
                13,UiKit.MUTED));

        snoozeSummary=UiKit.label(this,"",14,UiKit.MUTED);
        LinearLayout end=foldSection(root,"Schlummern & Stoppen",snoozeSummary);
        snoozeEnabled=UiKit.toggle(this);
        rowSwitch(end,"Schlummern aktiv",snoozeEnabled);
        snoozeEnabled.setOnCheckedChangeListener((b,value)->{
            if(snooze!=null)snooze.setEnabled(value);
            changed();
        });
        snoozeLabel=label("",15);end.addView(snoozeLabel);
        snooze=slider(end,29);
        stopLabel=label("",15);end.addView(stopLabel);
        autoStop=slider(end,29);
        end.addView(UiKit.label(this,"Stoppen und Schlummern erscheinen beim Klingeln "+
                "in der App und in der Android-Benachrichtigung.",13,UiKit.MUTED));

        Button bottom=UiKit.button(this,"Speichern",true);
        root.addView(bottom);
        bottom.setOnClickListener(v->{if(save())finish();});
        // Diagnostics remain available through the overflow menu, never as
        // permanent configuration controls or duplicate Save actions.
        status=UiKit.label(this,"",13,UiKit.MUTED);
        advanced=UiKit.label(this,"",14,UiKit.MUTED);
        setContentView(UiKit.scroll(this,root));
    }

    private void changed(){
        if (loading || saved == null || autoStop == null || snoozeEnabled == null) return;
        try { dirty = !saved.sameConfiguration(read()); }
        catch (RuntimeException invalid) { dirty = true; }
        updateSummaries();
    }
    private void showTime(){clock.setText(String.format(Locale.GERMANY,"%02d:%02d",hour,minute));}

    private void clearTimeUndo(){
        undoTimeBefore=null;
        undoTimeAfter=null;
        if(timeUndoRow!=null)timeUndoRow.setVisibility(View.GONE);
    }

    /** The time-picker OK is the sole auto-commit boundary: no intermediate
     * spinner changes or unrelated unsaved sliders are persisted. */
    private void saveSelectedTime(int h,int m){
        AlarmProfile before=AlarmRepository.get(this,id);
        if(before==null){notice("Wecker existiert nicht mehr.");return;}
        AlarmProfile after;
        try{after=before.withTimeAndEnabled(h,m);}
        catch(RuntimeException invalid){notice("Ungültige Weckzeit.");return;}
        if(after.sameConfiguration(before)){
            loading=true;hour=h;minute=m;showTime();enabled.setChecked(true);
            loading=false;changed();return;
        }
        if(!AlarmScheduler.canSchedule(this)){
            requestExactAlarmAccess();
            return;
        }
        try{
            AlarmRepository.put(this,after);
            AlarmScheduler.reschedule(this,id);
            saved=after;
            loading=true;hour=h;minute=m;showTime();enabled.setChecked(true);
            loading=false;
            changed();diagnostics();
            undoTimeBefore=before;
            undoTimeAfter=after;
            timeSavedLabel.setText("Weckzeit gespeichert · Aktiv");
            timeUndoRow.setVisibility(View.VISIBLE);
        }catch(RuntimeException failure){
            try{AlarmRepository.put(this,before);AlarmScheduler.reschedule(this,id);}
            catch(RuntimeException ignored){}
            notice("Weckzeit konnte nicht übernommen werden: "+failure.getMessage());
        }
    }

    private void undoSavedTime(){
        if(undoTimeBefore==null||undoTimeAfter==null)return;
        AlarmProfile current=AlarmRepository.get(this,id);
        if(current==null||!current.sameConfiguration(undoTimeAfter)){
            clearTimeUndo();
            notice("Wecker wurde inzwischen geändert. Bitte die aktuelle Einstellung prüfen.");
            return;
        }
        if(undoTimeBefore.enabled&&!AlarmScheduler.canSchedule(this)){
            requestExactAlarmAccess();return;
        }
        AlarmProfile prior=undoTimeBefore;
        try{
            AlarmRepository.put(this,prior);
            AlarmScheduler.reschedule(this,id);
            saved=prior;
            loading=true;hour=prior.hour;minute=prior.minute;showTime();
            enabled.setChecked(prior.enabled);
            loading=false;
            clearTimeUndo();
            changed();diagnostics();
        }catch(RuntimeException failure){
            try{AlarmRepository.put(this,current);AlarmScheduler.reschedule(this,id);}
            catch(RuntimeException ignored){}
            notice("Rückgängig fehlgeschlagen: "+failure.getMessage());
        }
    }

    private void requestExactAlarmAccess(){
        new AlertDialog.Builder(this).setTitle("Exakte Alarme erlauben")
                .setMessage("Ohne Android-Berechtigung kann der Wecker nicht aktiviert werden. "+
                        "Die bisherige Weckzeit bleibt unverändert.")
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Einstellungen",(d,w)->{
                    try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:"+getPackageName())));}
                    catch(RuntimeException e){notice("Bitte speziellen App-Zugriff erlauben.");}
                }).show();
    }
    private void updateLabels(){
        if(delayLabel!=null&&delay!=null)delayLabel.setText("Tonstart: +"+delay.getProgress()*10+" Sekunden");
        if(rampLabel!=null&&ramp!=null)rampLabel.setText("Anstieg: "+(ramp.getProgress()+1)+" Minuten");
        if(volumeLabel!=null&&volume!=null)volumeLabel.setText("Maximaler App-Pegel: "+
                (volume.getProgress()+5)+" % des Android-Weckerpegels");
        if(pulsesLabel!=null&&pulses!=null)pulsesLabel.setText("Watch-Impulse: "+pulses.getProgress());
        if(intervalLabel!=null&&interval!=null)intervalLabel.setText("Intervall: "+
                WatchIntervals.secondsAt(interval.getProgress())+" Sekunden");
        if(snoozeLabel!=null&&snooze!=null)snoozeLabel.setText("Schlummern: "+
                (snooze.getProgress()+1)+" Minuten");
        if(stopLabel!=null&&autoStop!=null)stopLabel.setText("Auto-Stopp nach "+
                (autoStop.getProgress()+1)+" Minuten");
        updateSummaries();
    }

    private void load(AlarmProfile p){
        loading=true;saved=p;name.setText(p.name);hour=p.hour;minute=p.minute;showTime();
        enabled.setChecked(p.enabled);
        for(int i=0;i<7;i++){
            days[i]=(p.days&(1<<i))!=0;
            applyDayState(i);
        }
        selectedTone=ToneCatalog.safe(p.tone);
        tonePicker.setText("Klingelton: "+ToneCatalog.LABELS[selectedTone]+"  ›");
        vibration.setSelection(p.vibrationMode);
        rising.setChecked(p.rampEnabled);
        delay.setProgress(p.delaySeconds/10);ramp.setProgress(p.rampSeconds/60-1);
        volume.setProgress(p.maxVolume-5);
        audibleWhenMuted.setChecked(p.audibleWhenMuted);
        cascade.setChecked(p.cascadeEnabled);
        watch.setChecked(p.watchPulses>0);
        watchIntensity.setSelection(p.watchIntensity);
        pulses.setProgress(p.watchPulses);
        interval.setProgress(WatchIntervals.progressFor(p.watchIntervalSeconds));
        snoozeEnabled.setChecked(p.snoozeEnabled);
        snooze.setProgress(p.snoozeMinutes-1);snooze.setEnabled(p.snoozeEnabled);
        autoStop.setProgress(p.autoStopMinutes-1);
        ramp.setEnabled(p.rampEnabled);
        pulses.setEnabled(p.watchPulses>0);interval.setEnabled(p.watchPulses>0);
        loading=false;dirty=false;clearTimeUndo();updateLabels();refreshModeUi();diagnostics();
    }

    private AlarmProfile read(){
        int mask=0;for(int i=0;i<7;i++)if(days[i])mask|=1<<i;
        boolean changedTime=hour!=saved.hour||minute!=saved.minute||mask!=saved.days;
        return new AlarmProfile(id,name.getText().toString(),enabled.isChecked(),hour,minute,mask,
                delay.getProgress()*10,(ramp.getProgress()+1)*60,volume.getProgress()+5,
                watch.isChecked()?pulses.getProgress():0,WatchIntervals.secondsAt(interval.getProgress()),
                snooze.getProgress()+1,selectedTone,rising.isChecked(),
                autoStop.getProgress()+1,changedTime?0:saved.skipAt,
                changedTime?0:saved.oneTimeAt,changedTime?0:saved.replacedAt,
                snoozeEnabled.isChecked(),vibration.getSelectedItemPosition(),
                audibleWhenMuted.isChecked(),cascade.isChecked(),
                watchIntensity.getSelectedItemPosition());
    }

    private boolean save(){
        AlarmProfile update;
        try{update=read();}catch(RuntimeException e){
            notice("Bitte mindestens einen Wochentag auswählen.");return false;
        }
        if(update.enabled&&!AlarmScheduler.canSchedule(this)){
            requestExactAlarmAccess();
            return false;
        }
        AlarmProfile old=AlarmRepository.get(this,id);
        if(old==null){notice("Wecker existiert nicht mehr.");return false;}
        try{
            AlarmRepository.put(this,update);AlarmScheduler.reschedule(this,id);
            saved=update;dirty=false;clearTimeUndo();diagnostics();return true;
        }catch(RuntimeException e){
            try{AlarmRepository.put(this,old);AlarmScheduler.reschedule(this,id);}
            catch(RuntimeException ignored){}
            notice("Speichern fehlgeschlagen: "+e.getMessage());return false;
        }
    }

    private void emergencyStop(){
        PhoneVibration.stop();
        AlarmScheduler.cancelTest(this);
        try{
            startService(new Intent(this,RingingService.class).setAction(RingingService.STOP));
            AlarmRepository.diagnostic(this,"Manueller Not-Stopp ausgeführt");
            notice("Stopp gesendet. Ton und Handyvibration sollten jetzt beendet sein.");
        }catch(RuntimeException e){
            notice("Handyvibration gestoppt. Falls ein Alarm weiterläuft: "+
                    "Einstellungen → Apps → Sanftwecker (Test) → Stopp erzwingen.");
        }
    }

    private void testAlarm(){
        if(!save())return;
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                !=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFY_REQ);return;
        }
        try{
            long when=AlarmScheduler.test(this,id);
            notice("Test geplant um "+new java.text.SimpleDateFormat("HH:mm:ss",
                    Locale.GERMANY).format(new Date(when))+". Bitte Handy sperren. "+
                    (saved.cascadeEnabled ?
                    "Kaskaden-Test: 60 Sekunden sanfter Ton und leichte Uhrvibration, "+
                    "danach stärkere Uhr- und Handyvibration. Die Probe endet "+
                    "nach insgesamt 150 Sekunden." :
                    "Dieser Kurztest umgeht die Tonverzögerung und "+
                    "steigt innerhalb von 8 Sekunden auf Testlautstärke an."));
        }catch(RuntimeException e){notice("Test konnte nicht starten: "+e.getMessage());}
    }

    private void previewTone(){
        try{
            startForegroundService(new Intent(this,RingingService.class)
                    .setAction(RingingService.START).putExtra(AlarmScheduler.EXTRA_ID,id)
                    .putExtra("tone_only_preview",true)
                    .putExtra("tone_preview_index",selectedTone));
            notice("Tonprobe für "+ToneCatalog.LABELS[selectedTone]+
                    " (max. 20 Sekunden), ohne Einstellungen zu speichern. "+
                    "Über die Alarmbenachrichtigung stoppen.");
        }catch(RuntimeException e){notice("Tonprobe fehlgeschlagen.");}
    }

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=TONE_PICKER_REQ||result!=RESULT_OK||data==null)return;
        int choice=data.getIntExtra(TonePickerActivity.EXTRA_SELECTED,selectedTone);
        if(!ToneCatalog.valid(choice))return;
        selectedTone=choice;
        tonePicker.setText("Klingelton: "+ToneCatalog.LABELS[selectedTone]+"  ›");
        changed(); // Apply changes only after the user's existing Save action.
    }

    private void watchTest(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                !=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFY_REQ);return;
        }
        NotificationManager m=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(m==null||!m.areNotificationsEnabled()){notice("Benachrichtigungen deaktiviert.");return;}
        RingingService.ensureChannels(this);
        Notification n=new Notification.Builder(this,
                WatchAlert.channelId(vibration.getSelectedItemPosition()))
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Sanftwecker – Watch-Test")
                .setContentText("Bitte prüfen, ob deine Galaxy Watch vibriert.")
                .setCategory(Notification.CATEGORY_ALARM).setAutoCancel(true).build();
        try{
            m.notify((int)(System.currentTimeMillis()&0x7fffffff),n);
            AlarmRepository.diagnostic(this,"Watch-Einzeltest an Android übergeben");
            notice("Test gesendet. Ob die Uhr vibriert hat, bitte selbst prüfen.");
        }catch(SecurityException e){notice("Benachrichtigungsrecht fehlt.");}
    }

    private void diagnostics(){
        if(status==null)return;
        long upcoming=AlarmRepository.next(this,id);
        String upcomingText="Nächster Termin: "+(upcoming<=0?"nicht eingeplant":
                new java.text.SimpleDateFormat("EEE dd.MM. HH:mm",Locale.GERMANY)
                        .format(new Date(upcoming)));
        status.setText(upcomingText);
        if(nextDisplay!=null) nextDisplay.setText(upcomingText);
        if(advanced!=null){
            NotificationManager m=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
            int level=audio==null?-1:audio.getStreamVolume(AudioManager.STREAM_ALARM);
            int maximum=audio==null?-1:audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            NotificationChannel channel=m==null?null:
                    m.getNotificationChannel(WatchAlert.channelId(
                            vibration==null?saved.vibrationMode:vibration.getSelectedItemPosition()));
            boolean notifications=m!=null&&m.areNotificationsEnabled();
            boolean watchChannel=channel!=null&&
                    channel.getImportance()!=NotificationManager.IMPORTANCE_NONE;
            boolean doNotDisturb=m!=null &&
                    m.getCurrentInterruptionFilter()!=NotificationManager.INTERRUPTION_FILTER_ALL;
            long testTime=AlarmScheduler.testWhen(this);
            advanced.setText("Exakte Alarme: "+(AlarmScheduler.canSchedule(this)?"OK":"FEHLEN")+
                    "\nAndroid-Benachrichtigungen: "+(notifications?"OK":"BLOCKIERT")+
                    "\nWatch-Kanal: "+(watchChannel?"aktiv":"BLOCKIERT")+
                    "\nVibrationsmuster: "+
                    VibrationPresets.LABELS[vibration.getSelectedItemPosition()]+
                    "\nSystem-Weckerlautstärke: "+level+" / "+maximum+
                    (level==0?" – STUMM!":maximum>0&&level<=maximum/3?
                    " – sehr niedrig":"")+
                    "\nNicht stören: "+(doNotDisturb?"AKTIV – Alarm-Ausnahmen prüfen":"aus")+
                    "\nWatch: Alarmbenachrichtigungen können durch Watch-DND "+
                    "unterdrückt werden"+
                    "\nVollbild-Wecker: "+(Build.VERSION.SDK_INT<34||
                    (m!=null&&m.canUseFullScreenIntent())?"OK":"prüfen")+
                    "\nKurztest geplant: "+(testTime>System.currentTimeMillis()?
                    new java.text.SimpleDateFormat("HH:mm:ss",Locale.GERMANY)
                            .format(new Date(testTime)):"nein")+
                    "\nLetzte Ereignisse (neueste zuerst):\n"+
                    AlarmRepository.history(this)+
                    "\nDirekte Uhr-Bestätigung: "+getSharedPreferences(
                    "sanftwecker_watch_native",MODE_PRIVATE).getString(
                    "last_status","Noch keine Rückmeldung der neuen Uhr-App")+
                    "\nWatch-DND: Betriebssystem entscheidet über tatsächliche Vibration.");
        }
    }

    private void more(View anchor){
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add("Wecker in 30 Sekunden testen")
                .setOnMenuItemClickListener(item->{testAlarm();return true;});
        menu.getMenu().add("Berechtigungen & Einrichtung")
                .setOnMenuItemClickListener(item->{
                    startActivity(new Intent(this,SetupAssistantActivity.class));return true;
                });
        menu.getMenu().add("Diagnose-Protokoll anzeigen")
                .setOnMenuItemClickListener(item->{
                    diagnostics();
                    new AlertDialog.Builder(this).setTitle("Sanftwecker – Diagnose")
                            .setMessage(advanced.getText().toString())
                            .setPositiveButton("OK",null).show();
                    return true;
                });
        menu.getMenu().add("Geplanten Test abbrechen")
                .setOnMenuItemClickListener(item->{
                    PhoneVibration.stop();
                    AlarmScheduler.cancelTest(this);
                    diagnostics();
                    notice("Geplanter Test abgebrochen.");
                    return true;
                });
        menu.getMenu().add("Alarm zwangsweise stoppen")
                .setOnMenuItemClickListener(item->{emergencyStop();return true;});
        menu.getMenu().add("Duplizieren").setOnMenuItemClickListener(item->{
            if(save())try{
                int newId=AlarmRepository.create(this,saved);
                startActivity(new Intent(this,DetailActivity.class)
                        .putExtra(AlarmScheduler.EXTRA_ID,newId));finish();
            }catch(RuntimeException e){notice("Duplizieren fehlgeschlagen.");}
            return true;
        });
        menu.getMenu().add("Nächsten Termin überspringen").setOnMenuItemClickListener(item->{
            if(save())try{AlarmScheduler.skipNext(this,id);load(AlarmRepository.get(this,id));}
            catch(RuntimeException e){notice("Überspringen fehlgeschlagen.");}
            return true;
        });
        menu.getMenu().add("Ausnahme rückgängig").setOnMenuItemClickListener(item->{
            if(save())try{
                AlarmProfile p=AlarmRepository.get(this,id);
                AlarmRepository.put(this,p.withExceptions(0,0,0));
                AlarmScheduler.reschedule(this,id);load(AlarmRepository.get(this,id));
            }catch(RuntimeException e){notice("Rückgängig fehlgeschlagen.");}
            return true;
        });
        menu.getMenu().add("Löschen").setOnMenuItemClickListener(item->{
            new AlertDialog.Builder(this).setTitle("Wecker löschen?")
                    .setMessage("„"+saved.name+"“ wirklich löschen?")
                    .setNegativeButton("Abbrechen",null)
                    .setPositiveButton("Löschen",(d,w)->{
                        try{AlarmScheduler.delete(this,id);dirty=false;finish();}
                        catch(RuntimeException e){notice("Löschen fehlgeschlagen.");}
                    }).show();
            return true;
        });
        menu.show();
    }

    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==NOTIFY_REQ)notice(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED
                ?"Benachrichtigungen erlaubt. Test erneut starten."
                :"Ohne Benachrichtigungsrecht kann die Uhr stumm bleiben.");
    }
    private void notice(String text){
        new AlertDialog.Builder(this).setMessage(text).setPositiveButton("OK",null).show();
    }
    private void back(){
        if(!dirty){finish();return;}
        new AlertDialog.Builder(this).setTitle("Änderungen speichern?")
                .setMessage(undoTimeAfter==null?
                        "Noch nicht gespeicherte Einstellungen übernehmen?":
                        "Die neue Weckzeit ist bereits aktiv und gespeichert. "+
                        "Nur die übrigen Änderungen sind noch offen.")
                .setPositiveButton("Speichern",(d,w)->{if(save())finish();})
                .setNeutralButton("Verwerfen",(d,w)->{dirty=false;finish();})
                .setNegativeButton("Weiter bearbeiten",null).show();
    }
    @Override public void onBackPressed(){back();}
}
