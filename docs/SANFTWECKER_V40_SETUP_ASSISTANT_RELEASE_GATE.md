# Sanftwecker v40: Einrichtungsassistent – geschützte Testfreigabe

Stand: 14.11.2026. Diese Datei beschreibt nur die Vorbereitung einer einmaligen
Testversion; sie ist **keine Freigabe zur Signierung oder Veröffentlichung**.

## Zugelassener Inhalt
- Android-Handy `0.9.21-beta`, Galaxy Watch `0.9.21-watch`, jeweils `versionCode 40`.
- Ursprüngliche Anwendungskennung `com.cobrachris.sanftwecker` und Originalzertifikat beibehalten.
- Änderungen aus PR #70: `Einrichtungsassistent` unter Wecker > ⋮,
  direkt aufrufbare Android-Berechtigungseinstellungen, echte Statushaken,
  kleine Warnung bei fehlenden Pflichtfreigaben und verständlich benannter
  `Alarm zwangsweise stoppen` als Rückfallweg. V39-Verhalten von Uhr,
  Timer, Wecker und Alarmkaskade bleibt erhalten.

## Signiersicherheit
- GitHub-Workflow `.github/workflows/sanftwecker-v40-release.yml`
  wird ausschließlich durch **eine neu hinzugefügte** Datei
  `requests/sanftwecker-v40-release.json` auf `main` ausgelöst.
- Der Freigabe-Request muss den genauen, vorher CI-geprüften `main`-Commit
  als `source_sha` nennen. Jeder weitere Dateiunterschied blockiert den Release.
- Vor Secret-Zugriff werden die veröffentlichten v39-APKs auf ihre SHA256-
  Baselines und die identische bisherige Zertifikatsidentität kontrolliert:
  Handy `762ecde8c83b3154961aafd4005ea9b089448b62128ebf22adf3f0694c630d66`;
  Watch `9e69f9dc79d48d46b4ea80f48ac32dc46960de61d7df84bd7c65cc056030f5e5`.
- Beide v40-APKs werden ausschließlich mit dem vorhandenen Schlüssel signiert.
  Ein Signaturabweichung verhindert die Veröffentlichung.
- Ein bereits vorhandenes Release `sanftwecker-v40` darf nicht überschrieben werden.
- Keine Installation, Deinstallation, neue Schlüssel, kostenpflichtigen Dienste
  oder Änderungen an früheren Releases.

## Hardwareabnahme nach expliziter Freigabe und eigener Installation
1. Wecker > ⋮ > Einrichtungsassistent: jede Berechtigung zeigt den wirklichen
   Android-Status; nach Rückkehr von Android-Einstellungen wird aktualisiert.
2. Fehlende Pflichtberechtigung zeigt das kleine gelbe ! am Menü; nach
   erfolgreicher Gewährung verschwindet es.
3. Klingelbildschirm-Hinweis belegt keine Hauptseitenfläche mehr.
4. Nicht erteilte Berechtigung für genaue Alarme verhindert weiterhin eine
   irreführende Aktivierung von Weckern.
5. Alarm zwangsweise stoppen bleibt im Menü und bei aktivem Wecker direkt
   verfügbar; Stoppen/Schlummern funktionieren weiterhin.
6. Uhrberechtigungen werden nur auf der Uhr überprüft; keine falschen
   Erlaubt-Haken am Handy.
7. Navigation und zwei Timer-Spalten der v39 unverändert.

Die Navigation/Bedienoberfläche wird erst nach tatsächlicher Benutzerabnahme
abgeschlossen. Auswählbare Designs beginnen danach.
