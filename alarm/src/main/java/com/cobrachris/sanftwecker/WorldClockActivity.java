package com.cobrachris.sanftwecker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Selectable offline world clock – no internet, background service, or permissions. */
public final class WorldClockActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Map<String,ClockLabels> clocks=new HashMap<>();
    private final Runnable tick=new Runnable(){
        @Override public void run(){
            updateTimes();
            long ms=System.currentTimeMillis();
            handler.postDelayed(this,60_000L-ms%60_000L+150L);
        }
    };
    private List<String> selected;
    private LinearLayout cards;
    private TextView subtitle;
    private boolean visible;

    private static final class ClockLabels {
        final TextView time,date,offset;
        ClockLabels(TextView time,TextView date,TextView offset){
            this.time=time;this.date=date;this.offset=offset;
        }
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        UiKit.window(this);
        selected=WorldClockStore.load(this);
        buildScreen();
        render();
    }
    @Override protected void onResume(){
        super.onResume();
        visible=true;
        handler.removeCallbacks(tick);
        handler.post(tick);
    }
    @Override protected void onPause(){
        visible=false;
        handler.removeCallbacks(tick);
        super.onPause();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    private void buildScreen(){
        LinearLayout base=UiKit.mainColumn(this);
        TextView title=UiKit.label(this,"Weltuhr",28,UiKit.TEXT);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        base.addView(title);
        subtitle=UiKit.label(this,"Uhrzeiten weltweit",14,UiKit.MUTED);
        base.addView(subtitle);
        ClockTabs.add(this,base,ClockTabs.WORLD_CLOCK);

        TextView info=UiKit.label(this,
                "Städte selbst auswählen · automatische Sommerzeit · funktioniert offline",
                13,UiKit.MUTED);
        LinearLayout.LayoutParams infoParams=new LinearLayout.LayoutParams(-1,-2);
        infoParams.bottomMargin=UiKit.dp(this,12);
        base.addView(info,infoParams);

        android.widget.Button add=UiKit.button(this,"＋  Stadt hinzufügen",true);
        add.setContentDescription("Stadt zur Weltuhr hinzufügen");
        base.addView(add);
        add.setOnClickListener(v->showCityPicker());

        TextView instruction=UiKit.label(this,
                "Mit ↑ und ↓ sortieren · × entfernt eine Stadt",12,UiKit.MUTED);
        LinearLayout.LayoutParams hintParams=new LinearLayout.LayoutParams(-1,-2);
        hintParams.topMargin=UiKit.dp(this,12);
        hintParams.bottomMargin=UiKit.dp(this,12);
        base.addView(instruction,hintParams);
        cards=UiKit.column(this);
        base.addView(cards);
        setContentView(ClockTabs.pin(this,base,UiKit.scroll(this,base)));
    }

    private void render(){
        if(cards==null)return;
        cards.removeAllViews();
        clocks.clear();
        subtitle.setText(selected.size()+
                (selected.size()==1?" Stadt":" Städte")+" · live und offline");
        if(selected.isEmpty()){
            LinearLayout card=UiKit.card(this);
            card.addView(UiKit.label(this,
                    "Noch keine Städte ausgewählt. Tippe oben auf „Stadt hinzufügen“.",
                    17,UiKit.TEXT));
            UiKit.placeCard(this,cards,card);
        }
        for(int i=0;i<selected.size();i++){
            String id=selected.get(i);
            WorldClockCatalog.City city=WorldClockCatalog.get(id);
            if(city==null)continue;
            UiKit.placeCard(this,cards,clockCard(city,i));
        }
        updateTimes();
    }

    private View clockCard(WorldClockCatalog.City city,int index){
        LinearLayout card=UiKit.card(this);
        LinearLayout headline=UiKit.row(this);
        LinearLayout location=UiKit.column(this);
        TextView cityName=UiKit.label(this,city.name,20,UiKit.TEXT);
        cityName.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        location.addView(cityName);
        location.addView(UiKit.label(this,city.country,13,UiKit.MUTED));
        if(city.id.startsWith("geo:")){
            TextView credit=UiKit.label(this,"Ortsdaten: Open-Meteo / GeoNames ↗",11,UiKit.MUTED);
            credit.setOnClickListener(v->openGeoCredits());
            location.addView(credit);
        }
        headline.addView(location,UiKit.weight());

        TextView up=UiKit.action(this,"↑");
        up.setContentDescription(city.name+" nach oben verschieben");
        up.setAlpha(index==0?.35f:1f);
        up.setOnClickListener(v->moveCity(city.id,-1));
        headline.addView(up);
        TextView down=UiKit.action(this,"↓");
        down.setContentDescription(city.name+" nach unten verschieben");
        down.setAlpha(index==selected.size()-1?.35f:1f);
        down.setOnClickListener(v->moveCity(city.id,1));
        headline.addView(down);
        TextView remove=UiKit.action(this,"×");
        remove.setContentDescription(city.name+" aus der Weltuhr entfernen");
        remove.setOnClickListener(v->removeCity(city.id));
        headline.addView(remove);
        card.addView(headline);

        LinearLayout clockRow=UiKit.row(this);
        TextView time=UiKit.label(this,"--:--",43,UiKit.BLUE);
        time.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        clockRow.addView(time,UiKit.weight());
        TextView offset=UiKit.label(this,"UTC",14,UiKit.MUTED);
        clockRow.addView(offset);
        LinearLayout.LayoutParams timeParams=new LinearLayout.LayoutParams(-1,-2);
        timeParams.topMargin=UiKit.dp(this,7);
        card.addView(clockRow,timeParams);
        TextView date=UiKit.label(this,"",14,UiKit.MUTED);
        card.addView(date);
        clocks.put(city.id,new ClockLabels(time,date,offset));
        return card;
    }

    private void updateTimes(){
        long now=System.currentTimeMillis();
        ZoneId local=ZoneId.systemDefault();
        for(Map.Entry<String,ClockLabels> row:clocks.entrySet()){
            WorldClockCatalog.City city=WorldClockCatalog.get(row.getKey());
            if(city==null)continue;
            try{
                WorldClockTime time=WorldClockTime.at(city.zone,now,local);
                row.getValue().time.setText(time.clock);
                row.getValue().date.setText(time.date+" · "+time.dayDifference);
                row.getValue().offset.setText(time.utcOffset);
            }catch(RuntimeException ignored){
                row.getValue().time.setText("—");
                row.getValue().date.setText("Zeitzone derzeit nicht verfügbar");
            }
        }
    }

    private void persist(List<String> next){
        try{
            WorldClockStore.save(this,next);
            selected=next;
            render();
        }catch(RuntimeException error){
            new AlertDialog.Builder(this).setTitle("Weltuhr")
                    .setMessage("Änderung konnte nicht gespeichert werden: "+error.getMessage())
                    .setPositiveButton("OK",null).show();
        }
    }
    private void moveCity(String id,int offset){
        persist(WorldClockSelection.move(selected,id,offset));
    }
    private void removeCity(String id){
        persist(WorldClockSelection.remove(selected,id));
    }

    private void openGeoCredits(){
        startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(
                "https://open-meteo.com/en/docs/geocoding-api")));
    }

    private void showCityPicker(){
        if(selected.size()>=WorldClockSelection.MAX_CITIES){
            new AlertDialog.Builder(this).setMessage(
                    "Maximal 24 Städte. Entferne zuerst eine Stadt, um eine neue hinzuzufügen.")
                    .setPositiveButton("OK",null).show();
            return;
        }
        LinearLayout body=UiKit.column(this);
        body.setPadding(UiKit.dp(this,12),0,UiKit.dp(this,12),0);
        EditText query=new EditText(this);
        query.setSingleLine(true);
        query.setTextColor(UiKit.TEXT);
        query.setHintTextColor(UiKit.MUTED);
        query.setHint("Ort, z. B. Enfield CT");
        query.setTextSize(16);
        body.addView(query,new LinearLayout.LayoutParams(-1,-2));
        android.widget.Button globalSearch=UiKit.button(this,
                "Weltweit suchen (Internet)",false);
        body.addView(globalSearch);
        query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        query.setOnEditorActionListener((text,action,event)->{
            globalSearch.performClick();
            return true;
        });
        TextView count=UiKit.label(this,"",12,UiKit.MUTED);
        body.addView(count);
        ListView list=new ListView(this);
        list.setDividerHeight(UiKit.dp(this,1));
        list.setBackgroundColor(UiKit.CARD);
        body.addView(list,new LinearLayout.LayoutParams(-1,UiKit.dp(this,310)));
        TextView provider=UiKit.label(this,
                "Online-Suche über Open-Meteo / GeoNames · danach offline ↗",
                12,UiKit.MUTED);
        provider.setOnClickListener(v->openGeoCredits());
        body.addView(provider);
        List<WorldClockCatalog.City> matches=new ArrayList<>();
        List<WorldClockCatalog.City> remoteResults=new ArrayList<>();
        final String[] searchedTerm={""};
        final int[] generation={0};
        ExecutorService executor=Executors.newSingleThreadExecutor();
        List<String> names=new ArrayList<>();
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(
                this,android.R.layout.simple_list_item_1,names){
            @Override public View getView(int position,View convert,ViewGroup parent){
                TextView row=(TextView)super.getView(position,convert,parent);
                row.setTextColor(UiKit.TEXT);
                row.setTextSize(15);
                row.setPadding(UiKit.dp(WorldClockActivity.this,12),
                        UiKit.dp(WorldClockActivity.this,6),
                        UiKit.dp(WorldClockActivity.this,8),
                        UiKit.dp(WorldClockActivity.this,6));
                row.setMinHeight(UiKit.dp(WorldClockActivity.this,48));
                return row;
            }
        };
        list.setAdapter(adapter);
        AlertDialog dialog=new AlertDialog.Builder(this)
                .setTitle("Stadt hinzufügen")
                .setView(body)
                .setNegativeButton("Schließen",null)
                .create();

        Runnable refresh=()->{
            String typed=query.getText().toString().trim();
            Set<String> excluded=new HashSet<>(selected);
            matches.clear();
            if(typed.equals(searchedTerm[0])){
                for(WorldClockCatalog.City city:remoteResults){
                    if(excluded.add(city.id))matches.add(city);
                }
            }
            for(WorldClockCatalog.City city:WorldClockCatalog.search(typed,
                    new HashSet<>(selected),60)){
                if(excluded.add(city.id))matches.add(city);
                if(matches.size()>=60)break;
            }
            names.clear();
            for(WorldClockCatalog.City city:matches){
                names.add(city.name+" · "+city.country+"\n"+city.zone);
            }
            adapter.notifyDataSetChanged();
            count.setText(typed.isEmpty()?"Beliebte Städte · tippe zum Hinzufügen":
                    matches.isEmpty()?"Keine lokalen Treffer · tippe auf „Weltweit suchen“":
                    matches.size()+" Treffer · weitere Orte über „Weltweit suchen“");
        };
        query.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                generation[0]++;
                remoteResults.clear();
                searchedTerm[0]="";
                globalSearch.setEnabled(true);
                refresh.run();
            }
            @Override public void afterTextChanged(Editable e){}
        });
        list.setOnItemClickListener((parent,view,position,id)->{
            if(position<0||position>=matches.size())return;
            String choice=matches.get(position).id;
            try{
                if(choice.startsWith("geo:"))
                    WorldClockCatalog.registerOnlineCity(matches.get(position));
                List<String> next=WorldClockSelection.add(selected,choice);
                WorldClockStore.save(this,next);
                selected=next;
                dialog.dismiss();
                render();
            }catch(RuntimeException error){
                new AlertDialog.Builder(this).setMessage(error.getMessage())
                        .setPositiveButton("OK",null).show();
            }
        });
        globalSearch.setOnClickListener(v->{
            String term=query.getText().toString().trim();
            if(term.length()<2 || term.length()>100){
                count.setText("Bitte mindestens 2 und höchstens 100 Zeichen eingeben");
                return;
            }
            final int request=++generation[0];
            android.view.inputmethod.InputMethodManager keyboard=
                    (android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(keyboard!=null)keyboard.hideSoftInputFromWindow(query.getWindowToken(),0);
            query.clearFocus();
            globalSearch.setEnabled(false);
            count.setText("Weltweite Suche läuft …");
            executor.execute(()->{
                List<WorldClockCatalog.City> result=null;
                String error=null;
                try {
                    result=WorldClockGeocoding.search(term);
                } catch(Exception ex){
                    error=ex.getMessage()==null?"Keine Verbindung":ex.getMessage();
                }
                final List<WorldClockCatalog.City> found=result;
                final String problem=error;
                handler.post(()->{
                    if(!dialog.isShowing() || generation[0]!=request)return;
                    globalSearch.setEnabled(true);
                    if(problem!=null){
                        count.setText("Online-Suche nicht möglich: "+problem);
                        return;
                    }
                    searchedTerm[0]=term;
                    remoteResults.clear();
                    remoteResults.addAll(found);
                    refresh.run();
                    if(found.isEmpty())
                        count.setText("Online kein passender Ort gefunden · genauer suchen");
                    else
                        count.setText(matches.size()+" Treffer · Ort zum Hinzufügen antippen");
                });
            });
        });
        dialog.setOnDismissListener(v->{
            generation[0]++;
            executor.shutdownNow();
        });
        refresh.run();
        dialog.show();
        if(dialog.getWindow()!=null)
            dialog.getWindow().setBackgroundDrawable(UiKit.shape(UiKit.CARD,this,20));
    }
}
