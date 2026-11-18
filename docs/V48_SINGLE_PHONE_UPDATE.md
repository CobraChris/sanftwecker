# Sanftwecker v48 – ein geführter Watch-Updatevorgang vom Handy

## Installationsablauf
1. Nutzer installiert **nur die Handy-APK** v48. Die bestehende Watch-v46 bleibt zunächst installiert.
2. Beim ersten Öffnen nach dem Handy-Upgrade erscheint **einmalig** „Galaxy Watch mit aktualisieren?“. „Später“ lässt beide installierten Apps unberührt.
3. Auf „Watch-Update vorbereiten“ folgt in Sanftwecker auf dem Handy ein zusammenhängender Vorgang: Watch-Verbindung prüfen, passendes signiertes Release auf GitHub finden, Watch-APK herunterladen, SHA-256/Paket-ID/Version/Originalzertifikat prüfen, über Wear Data Layer senden.
4. Watch-v46 besitzt bereits den Empfangspfad `/sanftwecker/update/v1/apk`; keine Zwischeninstallation von v47 erforderlich. Auf der Uhr wird die APK erneut geprüft, eine lokale Bereitstellungsbenachrichtigung angezeigt und der Nutzer muss den Android-Paketinstaller ausdrücklich bestätigen.
5. Das Handy unterscheidet „an Wear OS übergeben“ und „von der Uhr geprüft“. Keine Erfolgsmeldung über tatsächlich installierte Software ohne nachprüfbare installierte Versionsnummer.

## Rückfall und Schutz
- Unter `⋮ → Galaxy Watch aktualisieren` kann der Nutzer die gemeinsame Aktion wiederholen, ohne die APK manuell zu suchen. Bei Wiederholungen wird eine vorhandene verifizierte APK wiederverwendet.
- Bestehende Funktionen „APK in Downloads speichern“, „an Installer weitergeben“ sowie der direkte Watch-GitHub-Download bleiben verfügbar.
- Kein automatisches Erteilen der Android-Installationsberechtigung und keine stille Installation; nichts deinstallieren.
- Watch benötigt keinen eigenen Internetzugang, nur Wear-OS-Kopplung. Handy benötigt Internetzugang zum Download.
- Die v46→v48 Übertragung ist **quellseitig kompatibel** geprüft, aber hardwareseitig noch unbestätigt. Falls die Watch v46 keinen Paketinstaller bereitstellt, muss der vorhandene manuelle Installer verwendet werden.

## Releasebedingungen
- Handy: `0.9.29-beta` (versionCode 48).
- Watch: `0.9.29-watch` (versionCode 48).
- Bestehende Wecker-, Timer-, Watch-Steuerungs- und Farb-Funktionen von v47 bleiben bestehen.
- Neues Besitzer-Release-Gate verwendet das **unveränderte Originalzertifikat** des v47-Releases; kein neuer Schlüssel.
- Geschützte Signierung und Veröffentlichung erst nach einem **separaten ausdrücklichen Freigabeauftrag**, erfolgreicher CI, exaktem Commit- und Diff-Abgleich.
