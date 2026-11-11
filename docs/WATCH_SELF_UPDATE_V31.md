# Sanftwecker v31 – Watch-Selbstupdate ohne GeminiMan (experimentell)

**Stand 11.11.2026:** Implementiert im Entwicklungszweig, keine signierte
Veröffentlichung, keine Installation oder Verifikation auf echter Galaxy Watch.

## Problem
Der Nutzer aktualisiert den Sanftwecker auf Galaxy Watch bislang über
eine zusätzliche ADB-/GeminiMan-App. Der browserfreie Download auf dem
Handy (v30) ist bereits hardwareseitig bestätigt, aber für die eigentliche
Installation ist eine zusätzliche App mit drahtlosem Debugging nötig.

## Kostenlose Zielarchitektur
- Nach **einer einmaligen Aktualisierung der Watch auf v31** mit dem
  bisherigen Installationsweg kann die Watch-App spätere Versionen
  hoffentlich selbst installieren: **Sanftwecker Uhr → Uhr aktualisieren
  → Update suchen → APK laden und prüfen → Update installieren**.
- Der Code läuft direkt auf der **Uhr**; keine Google-Play-Veröffentlichung,
  kein ADB, keine zusätzliche App und kein Server/Billing notwendig.
  Die Uhr braucht temporär Internet (z. B. über gekoppeltes Handy/WLAN).
- Das Update wird ausschließlich nach Nutzeraktion geprüft und heruntergeladen.
  Keine automatischen Installationen, keine Deinstallation, keine neuen Schlüssel.
- GitHub Release API liefert ausschließlich veröffentlichte, neuere
  `sanftwecker-vN`-Releases mit genauem
  `sanftwecker-watch-vN.apk`-Dateinamen und SHA-256-Digest.
- HTTPS-Weiterleitungen sind auf GitHub-/Release-Assets-Hosts beschränkt.
  Download mit Größenlimit, Zeitlimits und drei Versuchen; erst nach
  **SHA-256, Paket-ID, Versionscode und Originalsignaturabgleich mit der auf
  der Uhr bereits installierten App** ist die APK verwendbar.
  Cache-Datei wird ausschließlich über den eigenen, nicht exportierten
  FileProvider mit zeitlich begrenzter Leseerlaubnis geteilt.
- Der Android-Paketinstaller wird mit
  `ACTION_VIEW` (`application/vnd.android.package-archive`) oder alternativ
  `ACTION_INSTALL_PACKAGE` geöffnet, **nur** nach explizitem Dialog in
  der Watch-App. Android darf eine zusätzliche Berechtigung und die finale
  Installationsbestätigung verlangen. Es werden keine privilegierten
  Silent-Installation-APIs oder ungeschützte ADB-Befehle eingesetzt.
- **Kein Update während eines aktiven Watch-Alarms**.
- Die vorhandene Handy-Updatefunktion, Watch-APK-Download auf dem Handy,
  das etablierte ADB/GeminiMan-Fallback und die komplette Kaskaden-/
  Snooze-/Stop-Logik bleiben erhalten.

## Ungewissheit, die nur am Gerät geprüft werden kann
Die Galaxy Watch6 könnte den Systemdialog zum Zulassen unbekannter
Installationsquellen oder das APK-Öffnen über `ACTION_VIEW` sperren.
Das kann auch mit korrekter Implementierung vorkommen. Dann zeigt die
Watch-App eine klare Fehlermeldung und **GeminiMan bleibt der Fallback**.
Daher ist v31 ausdrücklich ein **Hardware-Prototyp**, kein bereits
nachgewiesen funktionierender Self-Updater. Keine Aussage, dass Wear OS
die Self-Installation sicher zulässt.

## Abnahme
1. Beide Geräte nach separater Releasefreigabe signiert auf v31 updaten,
   **nicht** deinstallieren. Dafür auf der Uhr ein letztes Mal den
   bestehenden ADB-Installationsweg verwenden.
2. Auf der Watch `Sanftwecker Uhr` öffnen. Der neue Menüpunkt
   `Uhr aktualisieren` muss erreichbar sein (scrollbar).
3. `Update suchen` ohne verfügbaren neueren Release:
   `Die Watch-App ist aktuell` – kein unsicherer Download/Install.
4. Nach einer **später separat freigegebenen v32** auf der Uhr:
   Release erkennen, die verifizierte APK ohne Browser laden;
   SHA-256, Zertifikat und Package-ID bestätigen; Nutzerdialog und
   Android-Installationsbestätigung auf der Watch öffnen.
5. Nach der Installation bleibt die bestehende App mitsamt Weckern
   bestehen; Alarm-Kaskade, Stoppen, Schlummern und Offline-Funktion
   erneut prüfen.
6. Falls Android die Freigabe für selbst installierte APKs nicht anbietet,
   Fallback GeminiMan; keine riskante Umgehung.

## Freigabegrenze
Quellcode, unsignierte CI und ggf. Entwicklungs-Merge sind zulässig.
**Signieren, geschützter v31-Release, Installation und Veröffentlichung
benötigen eine erneute ausdrückliche Freigabe.**
