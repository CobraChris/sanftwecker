# Sanftwecker v37 – einmalige Navigationsabnahme (13.11.2026)

## Freigegebener Zweck
Die Nutzerfreigabe gilt ausschließlich einer einmaligen, signierten **Navigations-Testversion**
für Android-Handy und Galaxy Watch, auf Basis von PR #62 und dem bereits geprüften
aktuellen Stand. PR #62 wurde unter `e7b9aa992c678dddeddb9377979ed92ea2d0e216` in
`main` übernommen. Alle technischen und UI-Korrekturen aus PR #59–#62 sind enthalten.

## Sicherheit
- `versionCode 37` für Handy und Watch; identische App-ID `com.cobrachris.sanftwecker`.
- Ausschließlich das bestehende Signierzertifikat, dessen SHA-256-Fingerabdruck
  vor Secret-Zugriff anhand der veröffentlichten v36-Handy- und Watch-APKs verglichen wird.
- SHA-256-Baseline: Handy `9d2de16a3bd439195faea44e45cccebc622e3f267a9104485f4f2ea7922baa82`;
  Watch `a270713a477bbbf88c57474dab19b63529323d26bd72c1b0d07517d22b916910`.
- Freigabedatei erst nach erfolgreichen Tests und Merge dieses Vorbereitungs-PR;
  ihr Push darf **nur die neue Request-Datei** hinzufügen und muss die konkrete
  `main`-SHA als unveränderliche Quelle nennen.
- Signierung nur im bestehenden Environment `android-releases`, keine neuen
  Schlüssel, Dienste, Installationen oder Änderungen an existierenden Releases.
- Publikation einmalig unter `sanftwecker-v37` als Prerelease und ohne Überschreiben.

## Abnahme durch den Nutzer (erst nach eigener Installation)
1. Hauptreiter Wecker, Timer, Stoppuhr und Weltuhr wechseln; alle bleiben beim
   Scrollen auf jeder Hauptseite sichtbar.
2. Android-Zurück-Taste nach einem Reiterwechsel und aus einer Detailansicht prüfen.
3. Wecker neu anlegen, bearbeiten und aktivieren; Weckerdaten bleiben erhalten.
4. Aktiven Wecker am Handy stoppen/schlummern; Not-Stopp ist währenddessen sichtbar.
5. Auf der Galaxy Watch laufenden Alarm stoppen oder schlummern; Berechtigung für
   Alarmbenachrichtigungen nur per Benutzertipp anfordern.
6. Timer, Stoppuhr und Weltuhr auf ihre bisherigen Funktionen prüfen.

Automatisierte Build- und Logiktests können die echte Bildschirm-/Hardwareabnahme
nicht ersetzen. Die Navigation gilt erst nach ausdrücklicher Benutzerabnahme
als abgeschlossen; die Designauswahl beginnt erst danach.
