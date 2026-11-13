# Sanftwecker v35 – zweistufige Watch-Vibrationskaskade
Vorbereitung für ein **ausdrücklich freigegebenes signiertes Testrelease**, 13.11.2026.

Die Freigabe „N freigegeben“ erfolgte nach der Erklärung, dass zur Auslieferung
der Watch-Vibrationskorrektur aus PR #51 eine **separate Signierfreigabe**
erforderlich ist. Veröffentlichung nur nach erneuten Prüfungen auf Github.

Geprüfter Umfang: 
- Watch Kaskade übernimmt `watch_pulses` und `watch_interval_seconds` als
  tatsächlich ausgegebene endliche Einzelimpulse in Phase 1.
- `Intensiv`: 320 ms bei 185/255 statt 140 ms bei 80/255; Sanft und Normal
  bleiben unverändert; die zweite Phase startet weiterhin bei +60 Sekunden.
- Stop/Snooze und Service-Ende müssen alle geplanten Einzelimpulse löschen.
- Abwärtskompatibles Phone/Watch-Protokoll; beide APKs gemeinsam, originale
  Anwendungs-ID, ursprüngliches Signierzertifikat.
- Nur `versionCode 35`, Namen `0.9.16-beta` und `0.9.16-watch`, die
  Versionsfreigabepipeline und Sicherheitstests kommen zur bereits
  CI-geprüften Korrektur aus PR #51 hinzu.
- **Keine weiteren Änderungen** am Vollbild-Stoppbildschirm (Issue #50),
  an vorhandenen Weckern, Alarmlogik, Berechtigungen oder Watch-Updater.
- Die v34-Originalsignatur wird an signierten Vorläuferdateien geprüft:
  Phone SHA-256 `91e32aa0bbb74c36fe3c96d5d87175b656a9afec725286f40859a8e59bf95a35`
  Watch SHA-256 `bae71a51180ad345ebddac3613baef17d63535c9f6a23dc9c3fce8d05455eae3`.
- **Keine** `requests/sanftwecker-v35-release.json` in diesem
  Vorbereitungsschritt. Nach vollständig erfolgreichem CI und Review ein
  separater exakt gepinnter Releaseauftrag als eigene PR. Keine stille
  Installation, keine Deinstallation, keine neue Schlüssel.
- **Hardwaretest erforderlich**: Auf Telefon und Uhr v35 in-place installieren;
  Testalarm mit 12/3/Intensiv über 90 Sekunden; beendbare UI und
  fortgesetzte Kaskade prüfen. Wecker-Betrieb bis Hardwarebestätigung nicht
  als stabil deklarieren; Samsung-Uhr als Sicherheitsbackup beibehalten.
