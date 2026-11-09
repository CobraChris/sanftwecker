package com.cobrachris.watchvibrationpoc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Minimal standalone screen; vibration runs from local AlarmManager without UI. */
public final class WatchMainActivity extends Activity {
    private TextView status;
    private void refresh(){
        long next=WatchScheduleStore.next(this);
        status.setText(next>0
                ?"Nächster lokaler Wecker:\n"+
                new SimpleDateFormat("dd.MM. HH:mm",Locale.GERMANY).format(new Date(next))
                :"Noch kein Uhralarm synchronisiert.\nHandy- und Uhr-App müssen dieselbe Signatur haben.");
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout layout=new LinearLayout(this);
        layout.setGravity(Gravity.CENTER);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24,20,24,20);
        status=new TextView(this);
        status.setTextSize(16);status.setGravity(Gravity.CENTER);
        layout.addView(status);
        Button local=new Button(this);
        local.setText("Alarmvibration testen");
        layout.addView(local);
        local.setOnClickListener(v->WatchAlarmService.start(
                this,1,System.currentTimeMillis(),1,"Uhr-Vibrationstest"));
        Button stop=new Button(this);stop.setText("Alarm beenden");
        layout.addView(stop);
        stop.setOnClickListener(v->{
            // Local user can always cancel an ongoing alarm without the phone.
            Intent i=new Intent(this,WatchAlarmService.class).setAction(WatchAlarmService.STOP)
                    .putExtra("user",true).putExtra("stop_all",true);
            startService(i);
        });
        setContentView(layout);refresh();
    }
    @Override protected void onResume(){super.onResume();refresh();}
}