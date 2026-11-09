# Sanftwecker v23 – einmalige Installation und gebündelter Endtest (nur Android-Handy)

**Stand: 09.11.2026.** Der signierte [GitHub-Release v23](https://github.com/CobraChris/sanftwecker/releases/tag/sanftwecker-v23) ist veröffentlicht; der geschützte [Workflow #42584113643](https://github.com/CobraChris/sanftwecker/actions/runs/42584113643) hat beide VersionCode-23-APKs mit dem **bisherigen Originalzertifikat** signiert. Die installierte v21 bleibt bis zur bewussten Aktualisierung unverändert. Ein tatsächlicher Uhrtest bei aktivem Nicht-stören-Modus steht noch aus.

## Vorher: sicherer Download

- **Handy-APK:** [sanftwecker-release.apk – v23](https://github.com/CobraChris/sanftwecker/releases/download/sanftwecker-v23/sanftwecker-release.apk), 5.925.111 Bytes; SHA-256 `213a3488e11326f17c1e52fc7da1853ab1db87dad3fe83d1be3d2824477e586f`.
- **Wear-OS-APK:** [sanftwecker-watch-v23.apk – v23](https://github.com/CobraChris/sanftwecker/releases/download/sanftwecker-v23/sanftwecker-watch-v23.apk), 2.164.807 Bytes; SHA-256 `311cc657239c7a81b2d8fbe7ddc8ed923be3de46192b80e5ae3bf1ef7ecec5cf`.
- **Keine Deinstallation des Handy-Sanftweckers.** Die alte Uhr-Test-App hat eine andere Paketkennung und wird durch die neue Watch-App nicht automatisch ersetzt. Sie kann zunächst installiert bleiben.

## Ein einziger Installationsdurchgang

### A. Handy aktualisieren

1. In deiner vorhandenen Sanftwecker-v21-App nach Updates suchen und die angebotene v23 öffnen oder die direkte Handy-APK oben herunterladen.
2. Android muss **„Aktualisieren“** anbieten. Bestätigen. Falls eine Deinstallation verlangt wird oder eine Signaturwarnung erscheint: **abbrechen**, die vorhandenen Wecker nicht löschen.
3. Nach dem Start **Build 23** prüfen. Die neuen Funktionen (einschließlich „Update installieren“ und Timer-Sofortneustart) sind in v23 enthalten. Ein Neustart des Handys ist für die Installation nicht erforderlich.

### B. Die neue Uhr-App einmalig installieren – ohne Laptop

Die Galaxy Watch hat keinen allgemein verfügbaren APK-Installer für einen Handy-Download. Ein einfacher Weg ohne Computer ist **Wear Installer 2** für Android, erhältlich im [Google Play Store](https://play.google.com/store/apps/details?id=org.freepoc.wearinstaller2); offizielle [Kurzanleitung des Entwicklers](https://freepoc.org/wear-installer-2-help-page/).

1. Lade **die separate Watch-APK** von oben in den Downloadordner deines Handys. Installiere **Wear Installer 2 auf dem Handy**, nicht auf der Uhr.
2. Verbinde Handy und Galaxy Watch mit **demselben privaten WLAN**. Öffentliche Hotel-/Gästenetze können Geräte untereinander abschotten; bei Problemen besser in einem eigenen WLAN zuhause erledigen.
3. Auf der **Uhr**: `Einstellungen → Info zur Uhr → Softwareinformationen → Softwareversion` mehrfach antippen, bis der Entwicklermodus freigeschaltet ist. Dann `Einstellungen → Entwickleroptionen → ADB-Debugging` und `Drahtloses Debugging` einschalten.
4. In **Wear Installer 2 auf dem Handy** die von der Uhr angezeigte IP-Adresse eintragen. Über **„Pair with watch“** und **„Neues Gerät koppeln“** auf der Uhr den angezeigten Kopplungscode mit dem **Kopplungs-Port** verbinden.
5. Nach erfolgreicher Kopplung den **separaten Verbindungs-Port** aus „Drahtloses Debugging“ in Wear Installer 2 eingeben. Achtung: Kopplungsport und Verbindungsport sind unterschiedlich.
6. In Wear Installer 2 **„Custom APK“** auswählen, `sanftwecker-watch-v23.apk` aus dem Downloadordner öffnen und auf **„Install“** tippen. Warten, bis die Uhr die Installation bestätigt.
7. **Auf der Uhr ADB-Debugging und Drahtloses Debugging danach wieder ausschalten**. Es ist für den späteren Weckbetrieb nicht erforderlich und sollte nicht dauerhaft aktiviert bleiben.

Die neue Uhr-App heißt **„Sanftwecker Uhr“**. Das neue Paket nutzt die **gleiche Paketkennung und denselben Signierschlüssel wie die Handy-App**, weil der Google-Wear-Data-Layer sonst nicht zuverlässig koppelt.

### C. Genau ein gemeinsamer Endtest (erst wenn beide APKs installiert sind)

1. Auf der Uhr „Sanftwecker Uhr“ öffnen. Im Handy-Sanftwecker einen Wecker für **zwei bis drei Minuten später** einstellen und aktivieren. Die Uhr sollte den synchronisierten Termin anzeigen; erst jetzt beginnt die eigentliche Funktionsprüfung.
2. Handy **„Nicht stören“ an**, Uhr **„Nicht stören“ an**. Uhr tragen und die Bildschirme sperren.
3. Der **Handy-Wecker muss mit ansteigendem Ton klingeln** und die **Watch über ihren eigenen Vibrationsdienst** vibrieren. Die App fordert dabei eine Alarm- statt einer Benachrichtigungsvibration an. **Ob Samsung DND sie tatsächlich zulässt, ist noch nicht am Gerät bestätigt.**
4. Den Alarm auf der Watch beenden und prüfen, ob das Handy ebenfalls aufhört. Anschließend im Handy den **Timer-Sofortneustart** und die gespeicherten Wecker/Timer prüfen.
5. Wenn alles klappt, darf die Uhr im gewünschten Schlaf-/DND-Modus bleiben. Eine zusätzliche Probe ohne Handyverbindung kann später erfolgen. Der Handy-Neustart-Test ist weiterhin optional nach Rückkehr nach Deutschland.

## Fehlergrenzen und Rückmeldung

- **Watch-APK auf dem Handy anzutippen installiert sie nicht auf der Watch.** Dafür ist die separate Übertragung nötig.
- **Kein Gerätetest vor vollständiger Installation erforderlich.** Bestehende v21 auf dem Handy ist bis zur Bestätigung ein sicherer Rückfallstand – aber APK-Downgrades ohne Datenverlust sind durch Android nicht garantiert. Nicht blind v21 über v23 installieren.
- Wenn die neue Uhr-App keine Weckzeiten anzeigt oder bei Uhr-DND nicht vibriert: **keine Systemrichtlinien oder Sicherheitsberechtigungen ungeprüft umgehen**. Den angezeigten Status und das Verhalten als einmaliges Gesamtfeedback melden.
- Eine erfolgreiche GitHub-CI beweist **korrekte Builds und gleiche Android-Signatur**, nicht die tatsächliche Vibration unter Samsung-DND. Die Prüfung am Handgelenk ist deshalb erforderlich.
- Kein Token, kein kostenpflichtiger Dienst und kein PC notwendig. Es gibt keine automatische oder unbeaufsichtigte Installation.

**Beispiel für eine einzige Rückmeldung:** `v23 Handy installiert, Sanftwecker Uhr installiert; beide DND an; Uhralarm JA/NEIN; Handyton JA/NEIN; Stoppen auf Uhr JA/NEIN; Timer-Sofortneustart JA/NEIN.`
