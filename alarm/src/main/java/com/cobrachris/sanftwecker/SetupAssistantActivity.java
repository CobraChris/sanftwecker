package com.cobrachris.sanftwecker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Central permission dashboard. Android, not the app, owns every grant. */
public final class SetupAssistantActivity extends Activity {
    private static final int REQUEST_NOTIFICATION_PERMISSION=214;
    private TextView overview, exactState, notificationState, fullScreenState, updateState;
    private Button exactAction, notificationAction, fullScreenAction, updateAction;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        UiKit.window(this);
        buildScreen();
    }

    @Override public void onResume() {
        super.onResume();
        refreshPermissionStates();
    }

    @Override public void onRequestPermissionsResult(int requestCode,
            String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQUEST_NOTIFICATION_PERMISSION)refreshPermissionStates();
    }

    private void buildScreen() {
        LinearLayout root=UiKit.mainColumn(this);
        Button back=UiKit.button(this,"← Zurück zu den Weckern",false);
        root.addView(back);
        back.setOnClickListener(v->finish());

        TextView heading=UiKit.label(this,"Sanftwecker einrichten",26,UiKit.TEXT);
        heading.setPadding(0,UiKit.dp(this,20),0,UiKit.dp(this,6));
        root.addView(heading);
        overview=UiKit.label(this,"Berechtigungen werden geprüft …",16,UiKit.MUTED);
        root.addView(overview);

        TextView help=UiKit.label(this,
                "Haken zeigen den echten Android-Status. Tippe auf eine fehlende " +
                "Freigabe, bestätige sie im Android-Systemdialog und kehre zurück. " +
                "Der Status aktualisiert sich automatisch.",14,UiKit.MUTED);
        help.setPadding(0,UiKit.dp(this,12),0,UiKit.dp(this,16));
        root.addView(help);

        exactState=addPermission(root,"Genaue Alarme",
                "Damit Wecker und Timer zum geplanten Zeitpunkt auslösen.",
                v->openExactAlarmSettings());
        exactAction=lastAction;

        notificationState=addPermission(root,"Benachrichtigungen",
                "Damit Alarme und Timer ihre Hinweise und Stoppaktionen anzeigen.",
                v->openNotificationSettings());
        notificationAction=lastAction;

        fullScreenState=addPermission(root,"Klingelbildschirm",
                "Damit Android die Alarmansicht auch auf dem Sperrbildschirm öffnen darf.",
                v->openFullScreenSettings());
        fullScreenAction=lastAction;

        TextView optional=UiKit.label(this,"Optional",18,UiKit.TEXT);
        optional.setPadding(0,UiKit.dp(this,14),0,UiKit.dp(this,6));
        root.addView(optional);

        updateState=addPermission(root,"APK-Updates",
                "Nur nötig, wenn du neue APKs direkt aus Sanftwecker installieren möchtest.",
                v->openInstallSettings());
        updateAction=lastAction;

        LinearLayout watchCard=UiKit.card(this);
        watchCard.addView(UiKit.label(this,"Galaxy Watch",18,UiKit.TEXT));
        TextView watchStatus=UiKit.label(this,
                "◌ Auf der Uhr prüfen – Status nicht vom Handy aus bestätigt",
                14,UiKit.MUTED);
        watchCard.addView(watchStatus);
        watchCard.addView(UiKit.label(this,
                "Die Uhr hat eigene Alarm- und Benachrichtigungsrechte. " +
                "Diese lassen sich nicht auf diesem Handy vergeben.",14,UiKit.MUTED));
        Button watchHelp=UiKit.button(this,"Anleitung für die Uhr",false);
        watchCard.addView(watchHelp);
        watchHelp.setOnClickListener(v->new AlertDialog.Builder(this)
                .setTitle("Galaxy Watch einrichten")
                .setMessage("Öffne Sanftwecker Uhr direkt auf der Galaxy Watch. " +
                        "Erscheint dort „Alarmmeldungen erlauben“, tippe darauf. " +
                        "Android zeigt dann die Berechtigungsabfrage oder die " +
                        "Benachrichtigungseinstellungen auf der Uhr an. " +
                        "Wenn kein Alarm auf der Uhr ankommt, Synchronisierung prüfen.")
                .setPositiveButton("OK",null).show());
        UiKit.placeCard(this,root,watchCard);
        setContentView(UiKit.scroll(this,root));
        refreshPermissionStates();
    }

    // Keep the button inside its own card, no editable fake checkboxes.
    private Button lastAction;
    private TextView addPermission(LinearLayout root,String title,String detail,
            View.OnClickListener click) {
        LinearLayout card=UiKit.card(this);
        TextView name=UiKit.label(this,title,18,UiKit.TEXT);
        card.addView(name);
        TextView status=UiKit.label(this,"Status wird geprüft …",15,UiKit.MUTED);
        card.addView(status);
        card.addView(UiKit.label(this,detail,13,UiKit.MUTED));
        Button action=UiKit.button(this,"Android-Einstellungen öffnen",false);
        LinearLayout.LayoutParams padding=new LinearLayout.LayoutParams(-1,-2);
        padding.topMargin=UiKit.dp(this,10);
        card.addView(action,padding);
        action.setOnClickListener(click);
        lastAction=action;
        UiKit.placeCard(this,root,card);
        return status;
    }

    private void refreshPermissionStates() {
        if(overview==null)return;
        int missing=SetupPermissionStatus.missingRequired(this);
        overview.setText(SetupPermissionStatus.summary(missing));
        overview.setTextColor(missing==0 ? UiKit.BLUE : 0xFFFFBB66);
        updateRow(exactState,exactAction,SetupPermissionStatus.exactAlarms(this),false);
        updateRow(notificationState,notificationAction,
                SetupPermissionStatus.notifications(this),false);
        updateRow(fullScreenState,fullScreenAction,
                SetupPermissionStatus.fullScreen(this),false);
        updateRow(updateState,updateAction,
                SetupPermissionStatus.apkUpdates(this),true);
    }

    private void updateRow(TextView status,Button action,boolean allowed,boolean optional) {
        status.setText(allowed ? "✓ Erlaubt" :
                optional ? "◌ Nicht freigegeben · optional" : "⚠ Freigabe fehlt");
        status.setTextColor(allowed ? UiKit.BLUE :
                optional ? UiKit.MUTED : 0xFFFFBB66);
        action.setText(allowed ? "Einstellungen prüfen" :
                optional ? "Optional freigeben" : "Jetzt freigeben");
    }

    private void openExactAlarmSettings() {
        if(Build.VERSION.SDK_INT>=31)
            openSettings(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:"+getPackageName())));
        else openAppDetails();
    }

    private void openNotificationSettings() {
        if(Build.VERSION.SDK_INT>=33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
            return;
        }
        openSettings(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
    }

    private void openFullScreenSettings() {
        if(Build.VERSION.SDK_INT>=34)
            openSettings(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    .setData(Uri.parse("package:"+getPackageName())));
        else openAppDetails();
    }

    private void openInstallSettings() {
        openSettings(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:"+getPackageName())));
    }

    private void openSettings(Intent intent) {
        try {startActivity(intent);}
        catch(RuntimeException unsupported) {openAppDetails();}
    }

    private void openAppDetails() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:"+getPackageName())));
        }catch(RuntimeException unsupported) {
            new AlertDialog.Builder(this)
                    .setMessage("Die Android-Einstellungen konnten nicht geöffnet werden. " +
                            "Bitte öffne Einstellungen → Apps → Sanftwecker (Test).")
                    .setPositiveButton("OK",null).show();
        }
    }
}
