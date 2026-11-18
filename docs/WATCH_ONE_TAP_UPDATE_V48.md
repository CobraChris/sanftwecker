# Sanftwecker v48: ein Handy-Download, geführtes Watch-Update

## Nutzerablauf
1. Der Nutzer installiert **nur die signierte Handy-APK v48**. Der bisherige Android-Installer fragt wie gewohnt nach Zustimmung.
2. Bei der ersten Öffnung des aktualisierten Sanftweckers und gekoppelter Galaxy Watch fragt die Handy-App einmalig: **Galaxy Watch ebenfalls aktualisieren?**. Nach „Watch-Update starten“ wird die bestehende WatchUpdateActivity mit einem expliziten Guided-Flag geöffnet.
3. Die Handy-App findet die exakt zu v48 passende Watch-APK im offiziellen GitHub-Release, validiert GitHub-Asset-Digest, Paketkennung, versionCode und Originalsignatur. Anschließend wird sie über Wear Data Layer übertragen.
4. Die derzeitige Watch **v46** kann den APK-Asset-Pfad bereits empfangen und verifiziert die APK nochmals selbst. Sie bietet über ihre eigene Benachrichtigung die Installation auf der Uhr an. Der Nutzer bestätigt **auf der Uhr**; keine stille Installation.
5. Das Handy zeigt zunächst nur **„an Wear OS übergeben“** und erst nach einer frischen Watch-ACK **„auf der Uhr empfangen und geprüft“**. Ein ACK ist **keine Installationsbestätigung**.
6. Ab Watch v48 kann die Uhr auf eine Versionsabfrage antworten; neuere Handyversionen unterdrücken dann das Upgrade-Angebot bei bereits gleich/neuer installierter Watch-Version.

## Sicherheits- und Kompatibilitätsbedingungen
- Android und Wear OS bleiben **zwei signierte Pakete**; die Watch-APK wird im Release weiterhin produziert, aber nicht mehr vom Benutzer manuell heruntergeladen.
- Watch v46 benötigt **kein WLAN** für die vorgesehene Datenübertragung; Wear-Verbindung/Google Play Services müssen bestehen.
- Beim Bootstrapping von Watch v46 kann die Uhr die neue Versionsabfrage nicht beantworten, das gilt als unbekannter Versionsstand und darf das opt-in-Update nicht verhindern.
- Das automatische Angebot darf nur bei entsperrtem und gestarteten Handy-UI erfolgen; nicht während eines aktiven Weckeralarms. Kein Hintergrund-Installer und keine automatische Installation.
- Watch v46/v47 Self-Update, Dateispeicherung und „an Installations-App weitergeben“ bleiben als Fallback unter **Einzelschritte / Ersatzweg** erhalten.
- Ein nicht verbundenes Gerät, ein unterbrochener Download, ein falscher SHA-256 oder ein Watch-Signaturproblem führen zur Fehlermeldung ohne stille Installation.
- v47 Timerverlängerungen, Timerfarben und bestehende Alarm- und Vibrationskaskaden bleiben unverändert.

## Hardwareabnahme
Einmalig v48 auf Handy mit installierter Watch v46 starten und Watch-Updater aktivieren; prüfen, dass die Watch-APK über die Bluetooth-Wear-Verbindung ohne Watch-WLAN vollständig ankommt und der Android-Installationsdialog auf der Uhr erscheint. Nach bestätigter Installation Version/Signatur/Datenbestand und Wecker- sowie Timer-Vibration prüfen. Den Versionsabgleich anschließend wiederholen, damit keine unnötigen Updateangebote erscheinen.

**Entwicklungs-PR, noch keine Erlaubnis zum Mergen oder zum Signieren/Publizieren.** Release-Vorbereitung in getrenntem v48-Gate, nur nach exakter Eigentümerfreigabe.
