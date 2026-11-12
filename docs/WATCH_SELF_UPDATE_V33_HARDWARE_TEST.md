# Sanftwecker v33 – isolierter Watch-Selbstupdate-Hardwaretest

**Vorbereitung, 11.11.2026. Kein Release und keine Installationsfreigabe.**

## Ziel
Die bereits auf Galaxy Watch installierte **v32** soll später die **v33** direkt über
**Sanftwecker Uhr → Uhr aktualisieren → Update suchen → APK laden und prüfen →
Update installieren** beziehen und aktualisieren, ohne GeminiMan/ADB.

## Bewusst minimierter Änderungsumfang
- Beide Android-App-Module nur von `versionCode 32` auf `33` und die
  Namen `0.9.14-beta` / `0.9.14-watch` erhöhen.
- **Keine Änderung** an Weckern, Vibration, Alarmdiensten, Snooze/Stop,
  Synchronisierung, Uhr-Oberfläche, Download/Update-Client oder Berechtigungen.
- Auf der Watch bleibt **v32** bis zur separat genehmigten v33-Veröffentlichung
  unverändert installiert. Der geprüfte Quellcode wird beim Release eingefroren.
- Separate Signierpipeline mit einmaligem Owner-Auftrag, geschütztem
  `android-releases`-Environment und bestehender Originalsignatur.
- Die signierten v32-Vorgängerdateien werden vor Zugang zu Secrets anhand
  ihrer veröffentlichten SHA-256-Digests geprüft; danach Zertifikatgleichheit,
  Paketkennung und VersionCode beider v33-APKs prüfen.
- **Keine Freigabedatei** `requests/sanftwecker-v33-release.json` in der Vorbereitung.
  Kein Signing/Publishing, keine automatische Installation/Deinstallation, kein neuer Schlüssel.

## Abnahme nach separater ausdrücklicher Veröffentlichungsfreigabe
1. **v32 auf der Uhr belassen.** Keine v33-APK manuell auf die Uhr kopieren.
2. Auf der Galaxy Watch in Sanftwecker **Uhr aktualisieren → Update suchen**.
   Erwartung: **Watch-Version 33 gefunden. APK jetzt laden?**
3. **APK laden und prüfen**. Erwartung: 100 % und Meldung über validierte
   SHA-256, Paketkennung, VersionCode 33 und Originalsignatur. Bei Fehlern
   exakten Wortlaut protokollieren, nicht deinstallieren.
4. **Update installieren → Weiter**. Wear OS muss die Installation
   beziehungsweise die Berechtigung für unbekannte Quellen ausdrücklich
   bestätigen. Prüfen, ob ein Systemdialog erscheint und ob v33 als
   In-Place-Update installiert wird. Keine unbekannte Herkunft erlauben,
   wenn der Dialog nicht eindeutig Sanftwecker betrifft.
5. Sanftwecker auf der Uhr neu öffnen und **Update suchen**; bei Erfolg
   wird kein neueres geprüftes Update mehr angeboten. Bestehende Wecker,
   Vibration und Snooze/Stop unverändert prüfen.
6. Falls Wear OS den Paketinstaller oder die Berechtigung blockiert,
   **keine Umgehung**. V32 bleibt lauffähig; GeminiMan ist der bekannte Fallback.
   Screenshot der Fehlermeldung dokumentieren.

## Freigabegrenze
Diese PR erlaubt nur unsignierte CI und nach erfolgreich geprüften Checks
die Übernahme des Entwicklungscodes. **Eine v33-Signierung oder Veröffentlichung
erfordert eine neue ausdrückliche Nutzerfreigabe.** Das Kommando „N“ ist
keine Releasefreigabe.
