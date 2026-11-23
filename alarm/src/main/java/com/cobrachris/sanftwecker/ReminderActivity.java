package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class ReminderActivity extends Activity {
    private static final int VOICE=8301;
    private LinearLayout list;
    private EditText title;
    private TextView whenLabel;
    private Switch cascade,sameAsAlarm;
    private SeekBar intensity,pulses,interval;
    private Spinner recurrence,postpone,tone;
    private Switch phoneSound;
    private long chosenAt;
    private int editingId=-1;
    private boolean timeConfirmed;
    private final SimpleDateFormat format=
        new SimpleDateFormat("dd.MM.yyyy · HH:mm",Locale.GERMANY);

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        UiKit.window(this);
        build();
    }
    private void build(){
        LinearLayout root=UiKit.mainColumn(this);
        ClockTabs.add(this,root,ClockTabs.REMINDERS);
        root.addView(UiKit.heading(this,"Erinnerungen"));
        root.addView(UiKit.label(this,"Offen bis Erledigt – Stoppen beendet nur den Alarm.",14,UiKit.MUTED));
        Button newButton=UiKit.button(this,"+ Neue Erinnerung",true);
        root.addView(newButton);
        newButton.setOnClickListener(v->editor(null));
        list=UiKit.column(this);
        root.addView(list);
        setContentView(ClockTabs.pin(this,root,UiKit.scroll(this,root)));
        render();
    }
    @Override protected void onResume(){super.onResume();if(list!=null)render();}
    private void render(){
        list.removeAllViews();
        for(ReminderStore.Item item:ReminderStore.all(this)){
            LinearLayout card=UiKit.card(this);
            card.addView(UiKit.heading(this,item.title));
            card.addView(UiKit.label(this,format.format(new Date(item.dueAt))+
                (item.done?" · Erledigt":item.dueAt<System.currentTimeMillis()?
                " · Überfällig":" · Offen")+
                (item.cascade?(item.unifiedCascade?" · Wecker-Kaskade":" · Bisherige Kaskade"):" · Sanft")+
                (item.repeat==1?" · Täglich":item.repeat==2?" · Wöchentlich":""),14,UiKit.MUTED));
            Button edit=UiKit.button(this,"Erinnerung bearbeiten",false);
            edit.setOnClickListener(v->editor(item));
            card.addView(edit);
            if(!item.done){
                Button done=UiKit.button(this,"Erledigt",false);
                done.setOnClickListener(v->{
                    ReminderStore.complete(this,item.id);
                    ReminderScheduler.cancel(this,item.id);
                    ReminderNotice.clear(this,item.id);
                    ReminderWatchBridge.stop(this,item);
                    ReminderToneService.stop(this,item);
                    PhoneVibration.stopReminder(this,item.id,item.dueAt);
                    ReminderStore.Item next=ReminderStore.get(this,item.id);
                    if(next!=null&&!next.done){
                        try{ReminderScheduler.schedule(this,next.id);}
                        catch(RuntimeException failure){toast("Nächste Erinnerung nicht planbar");}
                    }
                    render();
                });
                card.addView(done);
                Button later=UiKit.button(this,"In "+item.postponeMinutes+" Minuten erinnern",false);
                later.setOnClickListener(v->{
                    ReminderScheduler.cancel(this,item.id);
                    ReminderNotice.clear(this,item.id);
                    ReminderWatchBridge.stop(this,item);
                    ReminderToneService.stop(this,item);
                    PhoneVibration.stopReminder(this,item.id,item.dueAt);
                    ReminderStore.postpone(this,item.id,System.currentTimeMillis()+item.postponeMinutes*60_000L);
                    try{ReminderScheduler.schedule(this,item.id);}
                    catch(RuntimeException e){toast("Genaue Alarme fehlen: "+e.getMessage());}
                    render();
                });
                card.addView(later);
            }
            Button delete=UiKit.button(this,"Erinnerung löschen",false);
            delete.setOnClickListener(v->new android.app.AlertDialog.Builder(this)
                .setTitle("Erinnerung löschen?")
                .setMessage(item.title)
                .setPositiveButton("Löschen",(d,w)->{
                    ReminderScheduler.cancel(this,item.id);
                    ReminderNotice.clear(this,item.id);
                    ReminderWatchBridge.stop(this,item);
                    ReminderToneService.stop(this,item);
                    PhoneVibration.stopReminder(this,item.id,item.dueAt);
                    ReminderStore.delete(this,item.id);
                    render();
                })
                .setNegativeButton("Abbrechen",null).show());
            card.addView(delete);
            UiKit.placeCard(this,list,card);
        }
    }
    private void editor(ReminderStore.Item existing){
        editingId=existing==null?-1:existing.id;
        Calendar when=Calendar.getInstance();
        when.add(Calendar.HOUR_OF_DAY,1);
        chosenAt=existing==null?when.getTimeInMillis():existing.dueAt;
        timeConfirmed=existing!=null && existing.dueAt>System.currentTimeMillis();
        LinearLayout form=UiKit.mainColumn(this);
        title=new EditText(this);
        title.setSingleLine(false);title.setMaxLines(3);
        title.setTextColor(UiKit.TEXT);title.setHintTextColor(UiKit.MUTED);
        title.setHint("Woran möchtest du erinnert werden?");
        if(existing!=null)title.setText(existing.title);
        form.addView(title);
        Button voice=UiKit.button(this,"🎙 Erinnerung einsprechen",false);
        voice.setOnClickListener(v->{
            Intent speech=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speech.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speech.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"de-DE");
            speech.putExtra(RecognizerIntent.EXTRA_PROMPT,
                    "Zum Beispiel: Morgen um 17 Uhr Pflanzen gießen");
            try{startActivityForResult(speech,VOICE);}
            catch(ActivityNotFoundException missing){toast("Keine Spracherkennung installiert");}
        });
        form.addView(voice);
        whenLabel=UiKit.label(this,"",17,UiKit.TEXT);
        form.addView(whenLabel);refreshWhen();
        Button date=UiKit.button(this,"Datum und Uhrzeit auswählen",false);
        date.setOnClickListener(v->chooseDate());
        form.addView(date);
        cascade=UiKit.toggle(this);
        cascade.setText("Sanft-stark-Kaskade mit Handy in Phase 2");
        cascade.setTextColor(UiKit.TEXT);
        cascade.setChecked(existing==null||existing.cascade);form.addView(cascade);
        sameAsAlarm=UiKit.toggle(this);
        sameAsAlarm.setText("Kaskade wie beim Wecker: Tonrampe ab Start, kräftige Watch- und Handyvibration ab Minute 2");
        sameAsAlarm.setTextColor(UiKit.TEXT);
        // Existing reminder settings are kept unless explicitly changed.
        sameAsAlarm.setChecked(existing==null || existing.unifiedCascade);
        sameAsAlarm.setEnabled(cascade.isChecked());
        cascade.setOnCheckedChangeListener((button,on)->sameAsAlarm.setEnabled(on));
        form.addView(sameAsAlarm);
        form.addView(UiKit.label(this,"Watch-Vibrationsstärke (0 sanft · 1 normal · 2 intensiv)",
                13,UiKit.MUTED));
        intensity=UiKit.seek(this,2);intensity.setProgress(existing==null?1:existing.watchIntensity);form.addView(intensity);
        form.addView(UiKit.label(this,"Watch-Impulse Phase 1 (1–30)",13,UiKit.MUTED));
        pulses=UiKit.seek(this,29);pulses.setProgress(existing==null?5:existing.watchPulses-1);form.addView(pulses);
        form.addView(UiKit.label(this,"Impulsabstand in Sekunden (1–60)",13,UiKit.MUTED));
        interval=UiKit.seek(this,59);interval.setProgress(existing==null?4:existing.watchIntervalSeconds-1);form.addView(interval);
        form.addView(UiKit.label(this,"Wiederholung",15,UiKit.TEXT));
        recurrence=new Spinner(this);
        recurrence.setAdapter(new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Einmalig","Täglich","Wöchentlich"}));
        recurrence.setSelection(existing==null?0:existing.repeat);
        form.addView(recurrence);
        form.addView(UiKit.label(this,"Später erinnern um",15,UiKit.TEXT));
        postpone=new Spinner(this);
        postpone.setAdapter(new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item,
            new String[]{"5 Minuten","10 Minuten","30 Minuten"}));
        postpone.setSelection(existing==null?1:existing.postponeMinutes==5?0:
                existing.postponeMinutes==30?2:1);
        form.addView(postpone);
        form.addView(UiKit.label(this,"Erinnerungston (Sanftwecker-Klänge)",15,UiKit.TEXT));
        tone=new Spinner(this);
        tone.setAdapter(new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item,ToneCatalog.LABELS));
        tone.setSelection(existing==null?0:existing.tone);
        form.addView(tone);
        phoneSound=UiKit.toggle(this);
        phoneSound.setText("Handy-Alarmton bei starker Phase");
        phoneSound.setTextColor(UiKit.TEXT);
        phoneSound.setChecked(existing==null||existing.phoneSound);
        form.addView(phoneSound);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this)
            .setTitle(existing==null?"Neue Erinnerung":"Erinnerung bearbeiten")
            .setView(UiKit.scroll(this,form))
            .setPositiveButton("Speichern",null)
            .setNegativeButton("Abbrechen",null).create();
        dialog.setOnShowListener(d->dialog.getButton(
            android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                if(save())dialog.dismiss();
            }));
        dialog.show();
    }
    private void chooseDate(){
        Calendar c=Calendar.getInstance();c.setTimeInMillis(chosenAt);
        new DatePickerDialog(this,(picker,y,m,day)->{
            c.set(y,m,day);
            new TimePickerDialog(this,(time,h,min)->{
                c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,min);
                c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
                chosenAt=c.getTimeInMillis();timeConfirmed=true;refreshWhen();
            },c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show();
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void refreshWhen(){if(whenLabel!=null)
        whenLabel.setText((timeConfirmed?"Fällig: ":"Bitte Termin bestätigen: ")+
                format.format(new Date(chosenAt)));}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=VOICE||result!=RESULT_OK||data==null||title==null)return;
        ArrayList<String> results=data.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS);
        if(results==null||results.isEmpty())return;
        ReminderVoiceParser.Parsed parsed=ReminderVoiceParser.parse(
                results.get(0),System.currentTimeMillis());
        title.setText(parsed.title);
        if(parsed.due>0){
            chosenAt=parsed.due;timeConfirmed=true;refreshWhen();
        }else{
            timeConfirmed=false;
            refreshWhen();
            toast("Datum/Uhrzeit nicht sicher erkannt – bitte auswählen");
        }
    }
    private boolean save(){
        try{
            if(!timeConfirmed){
                toast("Bitte Datum und Uhrzeit bestätigen.");
                return false;
            }
            if(!AlarmScheduler.canSchedule(this)){
                toast("Genaue Alarme fehlen – im Einrichtungsassistenten erlauben.");
                return false;
            }
            int minutes=new int[]{5,10,30}[postpone.getSelectedItemPosition()];
            ReminderStore.Item previous=editingId>0?ReminderStore.get(this,editingId):null;
            int id;
            if(editingId>0){
                if(previous==null)throw new IllegalStateException("Erinnerung nicht mehr vorhanden");
                id=previous.id;
                ReminderStore.update(this,id,title.getText().toString(),chosenAt,
                        cascade.isChecked(),intensity.getProgress(),
                        pulses.getProgress()+1,interval.getProgress()+1,
                        recurrence.getSelectedItemPosition(),minutes,phoneSound.isChecked(),
                        tone.getSelectedItemPosition(),sameAsAlarm.isChecked());
            }else{
                id=ReminderStore.create(this,title.getText().toString(),chosenAt,
                        cascade.isChecked(),intensity.getProgress(),
                        pulses.getProgress()+1,interval.getProgress()+1,
                        recurrence.getSelectedItemPosition(),minutes,phoneSound.isChecked(),
                        tone.getSelectedItemPosition(),sameAsAlarm.isChecked());
            }
            try{
                if(previous!=null){
                    ReminderScheduler.cancel(this,id);
                    ReminderNotice.clear(this,id);
                    ReminderWatchBridge.stop(this,previous);
                    ReminderToneService.stop(this,previous);
                    PhoneVibration.stopReminder(this,previous.id,previous.dueAt);
                }
                ReminderScheduler.schedule(this,id);
            }catch(RuntimeException failure){
                if(previous!=null){
                    ReminderStore.restore(this,previous);
                    if(!previous.done && previous.dueAt>System.currentTimeMillis()){
                        try{ReminderScheduler.schedule(this,id);}
                        catch(RuntimeException restoreError){
                            AlarmRepository.diagnostic(this,
                                    "WARNUNG: Erinnerung nach fehlgeschlagener Bearbeitung nicht eingeplant");
                        }
                    }
                }else ReminderStore.abandon(this,id);
                throw failure;
            }
            editingId=-1;
            toast("Erinnerung gespeichert und eingeplant");render();
            return true;
        }catch(RuntimeException e){
            toast("Erinnerung nicht planbar: "+e.getMessage());
            return false;
        }
    }
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
}