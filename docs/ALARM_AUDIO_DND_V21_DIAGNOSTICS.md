# Sanftwecker v20-Gerätetest: Tonanstieg / Nicht stören / Galaxy Watch

## Beobachtung vom 08.11.2026

- **Bestanden:** Android-In-place-Installation v19→v20, gespeicherte Wecker/Timer, Vollbildalarm mit Stoppen und Schlummern, übrige Grundfunktionen. Ein Screenshot des laufenden `Werktage test`-Alarms bestätigt die Vollbildoberfläche.
- **Fehler:** Handy vibriert, aber beim aktivierten Samsung-Modus `Bitte nicht stören` gibt es keinen Alarmton und die Galaxy Watch erhält keine fühlbaren Impulse.
- **Fehler:** Ohne `Bitte nicht stören` ist der Ton auch bei laufendem Alarm sehr leise und scheint nicht lauter zu werden. Die Vibration der Watch wird subjektiv minimal kürzer/schwächer wahrgenommen; Geräteabgleich folgt.
- **Offen:** Handy-Neustart nach Rückkehr nach Deutschland. Nutzer möchte vorhandene v20 erst später erneut testen. **Keine** v21-Releasefreigabe und kein v21-Gerätetest.

## Technische Ursachen und Änderungen für den noch nicht veröffentlichten v21-Quellstand

1. Das bisherige quadratische Gain-Profil `max*t*t` erhöhte den hörbaren Ton insbesondere zu Beginn einer langen Anstiegszeit extrem schwach. Die neue Kurve `max*t*(0.5+0.5*t)` hält ursprünglichen Tonstart, Endpegel und gewünschte Rampendauer ein, erhöht aber früh gleichmäßiger. JUnit prüft Nullbeginn, Delay, Zwischenwerte, monotone Entwicklung und Maximalpegel.
2. `AudioTrack` verwendet bereits `AudioAttributes.USAGE_ALARM`; zusätzlich begrenzt aber Androids `STREAM_ALARM`-Lautstärke die reale Lautheit. Die bisherige, **nur pro Wecker zuschaltbare** Funktion hob den Stream nur bei exakt 0 an. Jetzt kompensiert sie optional auch einen sehr niedrigen Pegel (höchstens ein Drittel des Systemmaximums), bringt ihn **nur während des Alarms** auf ungefähr 75 % und stellt den vorherigen Wert danach wieder her. Ein während des Alarms manuell geänderter Wert wird nicht zurückgesetzt. Vorhandene Benutzeroptionen werden nicht automatisch eingeschaltet oder verändert. Die App kann nicht garantieren, dass ein vom System blockierter Pegel angehoben werden darf.
3. Die an Galaxy Wearable gespiegelt gesendeten Watch-Impulse sind jetzt korrekt als `Notification.CATEGORY_ALARM` statt `CATEGORY_REMINDER` deklariert. Der **existierende Benachrichtigungskanal** bleibt aus Kompatibilitätsgründen unverändert; es wird keine neue Watch-Berechtigung erzwungen. Es besteht **weiterhin keine direkte Watch-Haptiksteuerung**: DND, Kanalberechtigungen und Samsung-Wearable-Mirroring haben Vorrang.
4. Der erweiterte Diagnosebereich im Wecker-Detail zeigt künftig `Nicht stören` und einen sehr niedrigen Android-Weckerpegel. Das Alarmprotokoll meldet die Systemzustände beim Start, statt DND ungefragt auszuschalten.

## Sofortige Einstellungsprüfung am Samsung

- **Handy:** `Einstellungen → Benachrichtigungen → Nicht stören` – **Alarme/Wecker zulassen**; gegebenenfalls `Sanftwecker` als erlaubte App-Benachrichtigung hinzufügen. Bei Samsung One UI können Menüpunkte je nach Modell variieren. Für den Test die **Weckerlautstärke**, nicht nur die Medienlautstärke, kontrollieren.
- **Uhr:** `Galaxy Wearable → Uhreneinstellungen → Benachrichtigungen → Nicht stören mit Tel. synchronisieren`. Prüfen, ob DND auch auf der Uhr aktiv ist, ob die App-Benachrichtigungen an die Uhr geleitet werden und ob die Ausnahme greift. DND kann die Uhr auch bei `CATEGORY_ALARM` weiterhin unterdrücken. `Theatermodus` ebenfalls ausschließen.
- **Wecker-Detail:** Tonstart, Anstiegsdauer und maximaler App-Pegel prüfen. Bei weiterhin sehr niedrigem Android-Weckerpegel die **freiwillige** Option `Niedrigen Android-Weckerpegel beim Alarm anheben` wählen. Nicht stören wird dadurch nicht überschrieben.

Samsung-Dokumentation:
- https://www.samsung.com/de/support/mobile-devices/bitte-nicht-stoeren-funktion-verwenden/
- https://www.samsung.com/de/support/mobile-devices/so-verwendest-du-den-nicht-storen-modus-auf-deiner-samsung-galaxy-watch/

## Freigabegrenzen

Diese Änderungen landen nur im bereits vorhandenen **v21-Entwicklungsquellstand**; die veröffentlichte und installierte v20 bleibt unverändert. Android-JUnit, Lint, Unsigned-APK und Signaturkettenprüfung v18–v20 laufen automatisch. **Kein** neues Signing, kein Releaseauftrag-Merge, keine APK-Veröffentlichung und kein Gerätseingriff ohne erneute Freigabe.

Vor einer eventuellen v21-Veröffentlichung muss zunächst die Funktionsabnahme der installierten v20 erfolgen. Der Nutzer installiert APKs selbst.
