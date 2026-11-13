# Sanftwecker v39 – finale Navigations- und Timer-Abnahme

Stand 13.11.2026. Nutzerfreigabe: alle zuletzt besprochenen UI-Korrekturen
in einem Paket umsetzen, prüfen und anschließend über die geschützte
Originalsignaturpipeline als Prerelease für Handy und Watch veröffentlichen.

## Änderungen
- Timer auf Smartphones wieder in **zwei Spalten**, mit kompaktem
  Button `Neu starten`, maximal zweizeiligen Namen plus Ellipse und
  vollständigem zugänglichem Namen sowie automatisch verkleinerbarer Zeitanzeige.
- Stoppuhrkarten auf Smartphones weiterhin einspaltig.
- Inaktive Hauptreiter minimal kontrastreicher, ohne Navigation oder
  die bisherige Position/Anordnung zu ändern.
- Kurze, sachliche Hinweise auf der Wecker- und Stoppuhr-Hauptansicht.

## Ausdrücklich unverändert
Alarmplanung, Vollbildberechtigung, Alarmberechtigungen und Not-Stopp,
Ton- und Haptikrampe, Galaxy-Watch-Kommunikation, Timerlaufzeiten,
gespeicherte Daten, bestehende APKs und Signierzertifikat.

## Release-Sicherheit
- Signierter Handy- und Watch-Stand `versionCode 39`;
  Paketkennung beider Apps `com.cobrachris.sanftwecker`.
- Originalzertifikatsprüfung gegen veröffentlichte v38-APKs:
  - Handy SHA-256 `5914361eb1d976d58e7d04629222cf2f4f8da083bf613767a86a923879ef6ba3`
  - Watch SHA-256 `3e0e2a881d96c4911a4f186def0bcdb2a1ff4b44ae2ea66b1f1b00c997abc2d6`
- Geschütztes GitHub-Environment `android-releases`, einmaliger
  Release-Request mit exakter, geprüfter Quelle, strikt ohne Overwrite,
  APK-Identitäts- und Zertifikatsvergleich vor Veröffentlichung.
- Keine automatische Installation oder Deinstallation. Nur Nutzer installiert.

## Geräteabnahme
Nutzer prüft die zwei Timer-Spalten samt langer Namen und Stoppuhr
im Hochformat; außerdem die vier festen Hauptreiter, Wecker-Countdown,
den Not-Stopp und die Systemleisten-Abstände. Navigation erst nach
ausdrücklicher Nutzerbestätigung final abnehmen; danach Design-Auswahl.
