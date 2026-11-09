# Sanftwecker – Ein-Knopf-APK-Update (Entwicklungsversion v22)

Stand: 09.11.2026. **Noch nicht signiert oder veröffentlicht.** Die bereits vom Nutzer freigegebene signierte v21 bleibt unverändert.

## Nutzerwunsch und aktueller Weg

Auf dem Samsung zeigt v20 korrekt `Update verfügbar: Build 21`, aber zur Installation muss bisher `APK im Browser herunterladen` gewählt, anschließend die Downloads-Datei geöffnet und im Android-Dialog `Aktualisieren` bestätigt werden.

**Bis eine neue signierte Version mit dem Knopf veröffentlicht ist:** In der v20 den vorhandenen Browser-Download wählen oder [v21 direkt herunterladen](https://github.com/CobraChris/sanftwecker/releases/download/sanftwecker-v21/sanftwecker-release.apk). Im Android-Dialog ausschließlich **Aktualisieren**, nicht deinstallieren.

## In der zukünftigen v22 vorbereitet

- Großer Hauptknopf **Update installieren** nach erfolgreicher manuell gestarteter Updateprüfung. `Nach Update suchen` und **Alternativ im Browser herunterladen** bleiben verfügbar.
- Die App liest nur signierte GitHub-Release-Assets mit exakt freigegebener Repository-/Versions-URL und **von GitHub gemeldetem SHA-256-Digest**. Fehlt ein valider Hash, wird kein Update als verifizierbar angeboten.
- Beim Klick holt die App die APK mit HTTPS über die bekannte URL und erlaubt nur Redirects zu `github.com` oder `release-assets.githubusercontent.com` (nicht beliebige Domains). Maximal fünf Weiterleitungen und 50 MiB Nutzdaten.
- Prüfung **vor** dem Android-Installer: Hash der heruntergeladenen APK entspricht genau dem GitHub-Asset-Digest; Android-Paket-ID `com.cobrachris.sanftwecker` und die Version stimmen; die Signatur entspricht bytegleich dem bereits **installierten** Zertifikat. Fehler führen zum Abbruch und nicht zu einer Deinstallation.
- Die geprüfte APK wird ausschließlich im privaten App-Cache abgelegt und nur über einen read-only `FileProvider`-URI an den Android-Systeminstaller übergeben. Die Datei wird nicht über eine offene externe Datei-URI geteilt.
- Android verlangt eine **sichtbare** Nutzerbestätigung zur APK-Installation. Beim ersten Mal kann zusätzlich das Systemfenster `Aus dieser Quelle zulassen` für Sanftwecker erscheinen; die App vergibt diese Berechtigung nicht still. Ohne Berechtigung kann der Nutzer den Browser-Fallback verwenden.
- Keine automatische Aktualisierung, keine App-Deinstallation, keine Änderung gespeicherter Wecker oder Timer, kein Token, kein Keystore, keine zusätzliche GitHub-App-Berechtigung und kein kostenpflichtiger Dienst.

## CI und Freigabe

Neue `VerifiedUpdatePolicyTest`-Tests lehnen fehlerhafte SHA-256-Metadaten, Host-Tricks, nicht vertrauenswürdige Redirects und Downgrades ab. Die übliche Android-CI prüft zusätzlich den Java-Code und den unsignierten Build.

**Wichtige Grenze:** Ein erfolgreicher CI-Lauf beweist noch nicht den realen Samsung-Systemdialog `Aus dieser Quelle zulassen` / `Aktualisieren`; dieser muss am Gerät getestet werden. Die bereits veröffentlichte **v21** enthält diese Funktion **noch nicht**. Eine zukünftige **v22** benötigt eine eigene ausdrückliche signierte Releasefreigabe. Der alte v19/v20-Updatekanal bleibt erhalten, bis alle Gerätetests bestanden sind.
