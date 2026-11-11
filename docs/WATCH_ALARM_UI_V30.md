# Sanftwecker v30 – Watch-Alarmanzeige, Stoppen und Schlummern

## Befund 11.11.2026
Realer v29-Morgenalarm: Galaxy Watch vibrierte, aber die Uhr zeigte keine
bedienbare Alarmoberfläche zum Schlummern oder Beenden. Ursache im v29-Code:
`WatchAlarmService` startet nur eine laufende Benachrichtigung; eine
Alarm-Activity wird beim Klingeln nicht gestartet. Auf Wear OS ist
`setFullScreenIntent` im Gegensatz zum Handy **nicht unterstützt**.
Der vorhandene `WatchMainActivity`-Bildschirm war nur nach manuellem
App-Öffnen erreichbar.

## Fix
- Watch-Schedule nutzt **zwei unabhängige** AlarmClock-PendingIntents zur
  selben geplanten Uhrzeit: den bisherigen Broadcast-Receiver für den
  zuverlässigen Vibrationsstart und eine neue, explizite Activity für die
  bedienbare Alarmansicht. Die frühere Vibrationslogik wird nicht ersetzt.
- Das Entfernen eines **ausgelösten** Termins löscht nur den lokalen
  Datensatz, nicht das parallele, bereits geplante Activity-PendingIntent.
  `clear` bei regulärer Terminabsage oder Neuplanung storniert beide.
  Beim Mehrfachstart verhindert der bisherige Alarmzustand doppelte
  Alarmauslösungen.
- `WatchRingActivity` erscheint über der Sperre, fordert das Einschalten
  des Displays an, zeigt das aktive Profil, und stellt zwei große
  Schaltflächen **SCHLUMMERN** (nur wenn im Wecker aktiviert) und
  **ALARM BEENDEN** bereit.
- **Schlummern** fordert weiterhin die Bestätigung des Handys an. Ohne
  bestätigte Neuterminierung wird die Watch nicht stillgelegt.
  **Beenden** stoppt die Uhr auch offline und informiert das Handy bei
  bestehender Verbindung. Beim Beenden bzw. bestätigten Schlummern
  schließt sich die Alarmansicht.
- Bestehende Wear-OS-Notification bleibt über den Alarmbildschirm
  antippbar. Ihre Aktionen **Stoppen/Schlummern** bleiben erhalten.
  Auf Wear OS wird ausdrücklich **kein** nicht unterstützter
  Full-Screen-Intent verwendet.
- Der Watch-Bildschirm meldet beim tatsächlichen `onResume` einmalig
  `SCREEN_VISIBLE:<Alarm-ID>`. Die Handy-App zeichnet dies im
  bestehenden Diagnoseprotokoll auf. Kein Netzwerk erforderlich, wenn
  Uhr und Handy zeitweise getrennt sind; die Anzeige hängt nicht vom
  erfolgreichen Diagnoseversand ab.
- Beide Apps erhalten versionCode 30; Watch und Handy nutzen weiter
  dieselbe Package-ID und müssen mit **dem Originalzertifikat** signiert
  werden. Bestehende Weckzeiten, Weltuhr, Watch-Intensitätsprofile und
  60-Sekunden-Kaskade bleiben unangetastet.

## Automatisierte Tests
- JVM: nur gültige aktive Alarmzustände erlauben Snooze; keine Snooze-Taste
  für deaktivierte Schlummerfunktion; 60-s-Phasengrenze unverändert.
- CI-Source-Contract: Receiver+Bildschirm werden beide durch AlarmManager
  zeitgesteuert; beide lassen sich stornieren; eine ausgelöste Aufgabe
  storniert nicht die parallel laufende Oberfläche; Lock-Screen-Flags,
  Stop-/Snooze-Aktionen, Notification-Fallback und sichtbare UI-ACK vorhanden.
- Bestehende :alarm/:watch JVM-Tests, Lint, Debug und unsignierte
  Release-APKs.

## Hardwareabnahme (nach separater Releasefreigabe)
1. Beide Geräte in-place auf v30 aktualisieren. Nicht deinstallieren.
2. Auf dem Handy in Sanftwecker einen Wecker öffnen, **Schlummern aktiv**
   und **Uhr benachrichtigen** eingeschaltet lassen, mit ✓ speichern.
3. Über **▶** einen echten 30-Sekunden-Testalarm planen. Uhr tragen,
   Display ausschalten.
4. Bei Alarmbeginn muss die Galaxy Watch eine sichtbare Ansicht mit
   **SCHLUMMERN** und **ALARM BEENDEN** zeigen, auch bei gesperrter Uhr.
5. Einmal **SCHLUMMERN** auslösen; Handy und Watch müssen nach bestätigter
   Neuterminierung verstummen und beim erneuten Alarm die Anzeige zeigen.
   Danach **ALARM BEENDEN** auf der Uhr prüfen; beide Geräte müssen stoppen.
6. Bei ausbleibender Ansicht: Benachrichtigung auf der Watch öffnen und
   native Stop-/Snooze-Aktionen prüfen; am Handy Diagnostik auf
   `SCREEN_VISIBLE` und `CASCADE_GENTLE/STRONG` kontrollieren. Falls
   SCREEN_VISIBLE fehlt, muss die tatsächliche Wear-OS-Hardwarekonfiguration
   weiter geprüft werden. **Nicht** allein aus CI-Erfolg ein funktionierendes
   Aufwecken des Displays behaupten.

**Releasegrenze:** Dies ist nur der unsignierte Entwicklungsstand.
Neue Signierung und Veröffentlichung benötigen die gesonderte
ausdrückliche Freigabe des Nutzers.
