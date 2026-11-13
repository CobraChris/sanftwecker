package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

/** Dark alarm list with arbitrary saved alarms, shortcuts and one-off actions. */
public final class MainActivity extends Activity {
    private final String[] dayNames = {"M","D","M","D","F","S","S"};
    private LinearLayout list;
    private TextView subheading;
    private Button fullScreenPermissionButton;
    private TextView fullScreenPermissionStatus;
    private Button openActiveAlarm;
    private Button emergencyStopButton;
    private boolean sortByNext = true;
    private final Map<Integer,TextView> countdownLabels = new HashMap<>();
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private final Runnable countdownTick = new Runnable() {
        @Override public void run() {
            updateCountdowns();
            countdownHandler.postDelayed(this, 15_000L);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        UiKit.window(this);
        AlarmVolumeGuard.recoverIfIdle(this);
        RingingService.ensureChannels(this);
        sortByNext = getPreferences(MODE_PRIVATE).getBoolean("next_sort", true);
        buildScreen();
    }

    @Override public void onResume() {
        super.onResume();
        if (list != null) {
            if (!AlarmScheduler.canSchedule(this)) {
                for (AlarmProfile p : AlarmRepository.all(this))
                    if (p.enabled) AlarmRepository.next(this,p.id,0L);
            } else {
                for (AlarmProfile p : AlarmRepository.all(this)) {
                    if (p.enabled && AlarmRepository.next(this,p.id)==0L) {
                        try { AlarmScheduler.reschedule(this,p.id); }
                        catch(RuntimeException e) {
                            AlarmRepository.diagnostic(this,"WARNUNG: "+p.name+" nicht eingeplant");
                        }
                    }
                }
            }
            renderList();
        }
        refreshFullScreenPermissionUi();
        refreshActiveAlarmButton();
        countdownHandler.removeCallbacks(countdownTick);
        countdownHandler.post(countdownTick);
    }

    @Override public void onPause() {
        countdownHandler.removeCallbacks(countdownTick);
        super.onPause();
    }

    private void updateCountdowns() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer,TextView> entry : countdownLabels.entrySet()) {
            long next = AlarmRepository.next(this,entry.getKey());
            entry.getValue().setText(AlarmCountdown.label(next,now));
        }
    }

    private void refreshActiveAlarmButton() {
        boolean ringing=ActiveAlarmState.activeId(this)>0;
        if(openActiveAlarm!=null)
            openActiveAlarm.setVisibility(ringing?View.VISIBLE:View.GONE);
        if(emergencyStopButton!=null)
            emergencyStopButton.setVisibility(ringing?View.VISIBLE:View.GONE);
    }

    private void buildScreen() {
        FrameLayout page = new FrameLayout(this);
        page.setBackgroundColor(UiKit.BG);
        LinearLayout base = UiKit.mainColumn(this);

        LinearLayout header = UiKit.row(this);
        LinearLayout headerText = UiKit.column(this);
        headerText.addView(UiKit.label(this, "Meine Wecker", 28, UiKit.TEXT));
        subheading = UiKit.label(this, "Noch keine Wecker", 14, UiKit.MUTED);
        headerText.addView(subheading);
        header.addView(headerText, UiKit.weight());
        TextView options = UiKit.action(this, "⋮");
        options.setContentDescription("Weitere Optionen und Sortierung");
        header.addView(options);
        options.setOnClickListener(this::showMainMenu);
        base.addView(header);
        ClockTabs.add(this,base,ClockTabs.ALARMS);

        // A running alarm and its fallback stop stay one tap away.
        // When idle, maintenance and emergency fallback remain in the menu.
        openActiveAlarm=UiKit.button(this,"LAUFENDEN WECKER ÖFFNEN – Stoppen / Schlummern",true);
        base.addView(openActiveAlarm);
        openActiveAlarm.setOnClickListener(v->{
            int ringingId=ActiveAlarmState.activeId(this);
            if(ringingId<=0){refreshActiveAlarmButton();return;}
            startActivity(new Intent(this,AlarmActivity.class)
                    .putExtra(AlarmScheduler.EXTRA_ID,ringingId)
                    .putExtra("from_alarm",true));
        });
        emergencyStopButton=UiKit.button(this,"Not-Stopp: Ton und Vibration",false);
        base.addView(emergencyStopButton);
        emergencyStopButton.setOnClickListener(v->emergencyStop());
        refreshActiveAlarmButton();
        if(android.os.Build.VERSION.SDK_INT>=34){
            fullScreenPermissionStatus=UiKit.label(this,
                    "Klingelbildschirm: Android-Berechtigung wird geprüft …",
                    13,UiKit.MUTED);
            base.addView(fullScreenPermissionStatus);
            fullScreenPermissionButton=UiKit.button(this,"Klingelbildschirm erlauben",false);
            base.addView(fullScreenPermissionButton);
            fullScreenPermissionButton.setOnClickListener(v->{
                try{
                    startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                            .setData(android.net.Uri.parse("package:"+getPackageName())));
                }catch(RuntimeException e){
                    new android.app.AlertDialog.Builder(this)
                            .setMessage("Bitte Vollbild-Benachrichtigungen für Sanftwecker "+
                                    "in den Android-Einstellungen freigeben.")
                            .setPositiveButton("OK",null).show();
                }
            });
            refreshFullScreenPermissionUi();
        }

        list = UiKit.column(this);
        base.addView(list);
        page.addView(UiKit.scroll(this, base));

        TextView add = UiKit.label(this, "+", 36, UiKit.BG);
        add.setGravity(Gravity.CENTER);
        add.setBackground(UiKit.shape(UiKit.BLUE, this, 25));
        add.setContentDescription("Neuen Wecker anlegen");
        FrameLayout.LayoutParams fab = new FrameLayout.LayoutParams(
                UiKit.dp(this, 64), UiKit.dp(this, 64),
                Gravity.END | Gravity.BOTTOM);
        fab.setMargins(0,0,UiKit.dp(this,24),UiKit.dp(this,24));
        page.addView(add, fab);
        add.setOnClickListener(v -> {
            try {
                int id = AlarmRepository.create(this, null);
                openDetail(id);
            } catch (RuntimeException ex) {
                error("Wecker konnte nicht erstellt werden: " + ex.getMessage());
            }
        });
        setContentView(ClockTabs.pin(this,base,page));
        renderList();
    }

    private void showMainMenu(View anchor) {
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add(sortByNext?"✓ Sortierung: nächste Auslösung":
                "Sortieren: nächste Auslösung").setOnMenuItemClickListener(item->{
            sortByNext=true;saveSort();renderList();return true;
        });
        menu.getMenu().add(!sortByNext?"✓ Sortierung: Uhrzeit":
                "Sortieren: Uhrzeit").setOnMenuItemClickListener(item->{
            sortByNext=false;saveSort();renderList();return true;
        });
        menu.getMenu().add("App aktualisieren").setOnMenuItemClickListener(item->{
            startActivity(new Intent(this,UpdateActivity.class));return true;
        });
        menu.getMenu().add("Not-Stopp: Ton und Vibration").setOnMenuItemClickListener(item->{
            emergencyStop();return true;
        });
        menu.show();
    }

    private void emergencyStop(){
        // Retain the original emergency path, including an alarm test without
        // an ActiveAlarmState token. Never silently remove this safety action.
        PhoneVibration.stop();
        AlarmScheduler.cancelTest(this);
        if(openActiveAlarm!=null)openActiveAlarm.setVisibility(View.GONE);
        if(emergencyStopButton!=null)emergencyStopButton.setVisibility(View.GONE);
        try{
            startService(new Intent(this,RingingService.class)
                    .setAction(RingingService.STOP));
            new AlertDialog.Builder(this)
                    .setMessage("Not-Stopp angefordert: Ton und Handyvibration beenden.")
                    .setPositiveButton("OK",null).show();
        }catch(RuntimeException e){
            new AlertDialog.Builder(this)
                    .setMessage("Handyvibration gestoppt. Falls der Alarm weiterläuft: "+
                            "Einstellungen → Apps → Sanftwecker (Test) → Stopp erzwingen.")
                    .setPositiveButton("OK",null).show();
        }
    }

    private void saveSort() {
        getPreferences(MODE_PRIVATE).edit().putBoolean("next_sort",sortByNext).apply();
    }

    private void renderList() {
        list.removeAllViews();
        countdownLabels.clear();
        List<AlarmProfile> profiles = AlarmRepository.all(this);
        List<AlarmProfile> ordered = new ArrayList<>(profiles);
        if (sortByNext) {
            Collections.sort(ordered, (a,b) -> {
                long aa = a.enabled && AlarmRepository.next(this,a.id)>0 ?
                        AlarmRepository.next(this,a.id) : Long.MAX_VALUE;
                long bb = b.enabled && AlarmRepository.next(this,b.id)>0 ?
                        AlarmRepository.next(this,b.id) : Long.MAX_VALUE;
                return aa == bb ? Integer.compare(a.id,b.id) : Long.compare(aa,bb);
            });
        } else {
            Collections.sort(ordered, (a,b) -> {
                int byTime=Integer.compare(a.hour*60+a.minute,b.hour*60+b.minute);
                return byTime==0 ? Integer.compare(a.id,b.id) : byTime;
            });
        }
        long earliest = Long.MAX_VALUE;
        for (AlarmProfile p : profiles) if (p.enabled) {
            long time=AlarmRepository.next(this,p.id);
            if (time>0) earliest=Math.min(earliest,time);
        }
        subheading.setText(profiles.isEmpty() ? "Noch keine Wecker" :
                profiles.size() + (profiles.size()==1 ? " Wecker" : " Wecker") +
                (earliest==Long.MAX_VALUE ? " · keiner aktiv" :
                        " · nächster " + shortTime(earliest)));
        if (profiles.isEmpty()) {
            LinearLayout empty=UiKit.card(this);
            TextView message=UiKit.label(this,
                    "Noch kein Wecker eingerichtet.\nTippe auf +, um deinen ersten Sanftwecker anzulegen.",
                    18, UiKit.TEXT);
            empty.addView(message);
            UiKit.placeCard(this,list,empty);
        }
        for (AlarmProfile p : ordered) UiKit.placeCard(this,list,renderCard(p,earliest));
        // The watch has its own locally scheduled alarm. Do not describe it
        // as merely mirrored Android notifications.
        TextView hint=UiKit.label(this,
                "Galaxy Watch: Wecker werden auf der Uhr geplant. " +
                "Fehlende Alarme? Synchronisierung prüfen.",
                13,UiKit.MUTED);
        list.addView(hint);
        list.addView(UiKit.label(this,
                "Beta · bisherigen zuverlässigen Wecker vorerst beibehalten.",
                12,UiKit.MUTED));
    }

    private LinearLayout renderCard(AlarmProfile p, long earliest) {
        LinearLayout card=UiKit.card(this);
        card.setAlpha(p.enabled?1f:0.74f);
        LinearLayout top=UiKit.row(this);
        TextView name=UiKit.label(this,p.name,16,UiKit.MUTED);
        name.setMaxLines(1);
        top.addView(name,UiKit.weight());
        TextView menu=UiKit.action(this,"⋯");
        menu.setContentDescription("Weitere Aktionen für "+p.name);
        top.addView(menu);
        card.addView(top);

        LinearLayout middle=UiKit.row(this);
        TextView time=UiKit.label(this,
                String.format(Locale.GERMANY,"%02d:%02d",p.hour,p.minute),
                59,p.enabled?UiKit.BLUE:UiKit.MUTED);
        middle.addView(time,UiKit.weight());
        Switch active=UiKit.toggle(this);
        active.setChecked(p.enabled);
        active.setContentDescription(p.name + (p.enabled?" deaktivieren":" aktivieren"));
        middle.addView(active);
        card.addView(middle);

        LinearLayout days=UiKit.row(this);
        for(int i=0;i<7;i++){
            TextView chip=UiKit.chip(this,dayNames[i],(p.days&(1<<i))!=0);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(
                    0,UiKit.dp(this,34),1);
            cp.rightMargin=UiKit.dp(this,5);
            days.addView(chip,cp);
        }
        card.addView(days);
        // Show only the time remaining; technical tone/vibration settings
        // remain editable in DetailActivity and are not changed by this UI.
        if (p.enabled && AlarmRepository.next(this,p.id)>0L) {
            TextView countdown=UiKit.label(this,
                    AlarmCountdown.label(AlarmRepository.next(this,p.id),
                            System.currentTimeMillis()),15,UiKit.TEXT);
            LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(-1,-2);
            gap.topMargin=UiKit.dp(this,12);
            card.addView(countdown,gap);
            countdownLabels.put(p.id,countdown);
        }
        if (p.enabled && AlarmRepository.next(this,p.id)==0L) {
            card.addView(UiKit.label(this,
                    "ACHTUNG: Wecker nicht eingeplant – Berechtigung prüfen",
                    13,Color.rgb(255,171,128)));
        }
        if (p.skipAt > 0) {
            card.addView(UiKit.label(this,"Ein Termin wird übersprungen",12,UiKit.MUTED));
        }
        if (p.oneTimeAt > System.currentTimeMillis()) {
            card.addView(UiKit.label(this,"Nächste Auslösung einmalig geändert",12,UiKit.BLUE));
        }
        card.setOnClickListener(v->openDetail(p.id));
        time.setOnClickListener(v->openDetail(p.id));
        active.setOnCheckedChangeListener((v,isChecked)-> {
            if (p.enabled==isChecked) return;
            try {
                AlarmScheduler.toggle(this,p.id,isChecked);
            } catch (RuntimeException ex) {
                v.setChecked(p.enabled);
                if (isChecked && !AlarmScheduler.canSchedule(this))
                    showAlarmPermission();
                else error("Wecker konnte nicht umgeschaltet werden: "+ex.getMessage());
            }
            renderList();
        });
        menu.setOnClickListener(v->showMenu(menu,p));
        return card;
    }

    private String shortTime(long millis) {
        return new SimpleDateFormat("EEE dd.MM. HH:mm",Locale.GERMANY).format(new Date(millis));
    }

    private void showMenu(View anchor,AlarmProfile p) {
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add("Bearbeiten").setOnMenuItemClickListener(i->{
            openDetail(p.id); return true;
        });
        menu.getMenu().add("Duplizieren").setOnMenuItemClickListener(i->{
            try { int id=AlarmRepository.create(this,p); openDetail(id); }
            catch(RuntimeException e){error("Duplizieren fehlgeschlagen");}
            return true;
        });
        if(p.enabled) {
            menu.getMenu().add("Nächsten Wecker ändern").setOnMenuItemClickListener(i->{
                chooseNext(p); return true;
            });
            menu.getMenu().add("Nächsten Wecker überspringen").setOnMenuItemClickListener(i->{
                try { AlarmScheduler.skipNext(this,p.id); renderList(); }
                catch(RuntimeException e){error("Überspringen fehlgeschlagen");}
                return true;
            });
            if(p.skipAt>0 || p.oneTimeAt>0) {
                menu.getMenu().add("Ausnahme rückgängig machen").setOnMenuItemClickListener(i->{
                    try {
                        AlarmProfile updated=AlarmRepository.get(this,p.id);
                        if(updated!=null){
                            AlarmRepository.put(this,updated.withExceptions(0,0,0));
                            AlarmScheduler.reschedule(this,p.id);
                        }
                        renderList();
                    } catch(RuntimeException e){error("Rückgängig fehlgeschlagen");}
                    return true;
                });
            }
        }
        menu.getMenu().add("Löschen").setOnMenuItemClickListener(i->{
            new AlertDialog.Builder(this).setTitle("Wecker löschen?")
                    .setMessage("„"+p.name+"“ dauerhaft löschen?")
                    .setNegativeButton("Abbrechen",null)
                    .setPositiveButton("Löschen",(dialog,which)->{
                        try {AlarmScheduler.delete(this,p.id);renderList();}
                        catch(RuntimeException e){error("Löschen fehlgeschlagen");}
                    }).show();
            return true;
        });
        menu.show();
    }

    private void chooseNext(AlarmProfile p) {
        Calendar c=Calendar.getInstance();
        long upcoming=AlarmRepository.next(this,p.id);
        if(upcoming>System.currentTimeMillis()) c.setTimeInMillis(upcoming);
        new DatePickerDialog(this,(view,y,m,d)->{
            Calendar candidate=Calendar.getInstance();
            candidate.set(y,m,d,p.hour,p.minute,0);
            candidate.set(Calendar.MILLISECOND,0);
            new TimePickerDialog(this,(time,h,min)->{
                candidate.set(Calendar.HOUR_OF_DAY,h);
                candidate.set(Calendar.MINUTE,min);
                try {
                    AlarmScheduler.changeNext(this,p.id,candidate.getTimeInMillis());
                    renderList();
                }catch(RuntimeException e){error("Ungültiger Zeitpunkt: "+e.getMessage());}
            },p.hour,p.minute,true).show();
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openDetail(int id) {
        startActivity(new Intent(this,DetailActivity.class)
                .putExtra(AlarmScheduler.EXTRA_ID,id));
    }

    private void refreshFullScreenPermissionUi() {
        if (android.os.Build.VERSION.SDK_INT < 34 || fullScreenPermissionButton == null) return;
        boolean allowed = FullScreenPermissionStatus.isAllowed(this);
        fullScreenPermissionStatus.setText(FullScreenPermissionStatus.label(allowed));
        // Show a setup problem, not a permanent technical status banner.
        fullScreenPermissionStatus.setVisibility(allowed ? View.GONE : View.VISIBLE);
        fullScreenPermissionButton.setVisibility(allowed ? View.GONE : View.VISIBLE);
    }

    private void showAlarmPermission() {
        new AlertDialog.Builder(this).setTitle("Alarme und Erinnerungen")
                .setMessage("Android muss exakte Alarme für Sanftwecker (Test) erlauben. " +
                        "Sonst darf der Schalter nicht eingeschaltet werden.")
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Einstellungen",(d,w)->{
                    try {
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.parse("package:"+getPackageName())));
                    }catch(RuntimeException e){error("Bitte speziellen App-Zugriff manuell erlauben.");}
                }).show();
    }

    private void error(String message) {
        new AlertDialog.Builder(this).setTitle("Hinweis").setMessage(message)
                .setPositiveButton("OK",null).show();
    }
}
