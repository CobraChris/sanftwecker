# Sanftwecker – Samsung-/Galaxy-Watch-Abnahme nach v20

**Gerätetest steht aus.** Dieser Plan ist für den späteren Test der bereits veröffentlichten signierten v20-APK gedacht. Keine Deinstallation, kein Rücksetzen von App-Daten und keine manuelle Token- oder GitHub-Konfiguration nötig.

## Vorbereitung: Bestand unverändert dokumentieren

Vor dem Update einmal die Anzahl vorhandener Wecker, Timer und Stoppuhr-Einträge sowie mindestens einen eingestellten Alarm prüfen. Falls du Einstellungen nicht ablesen kannst, Screenshots anfertigen; kein Export-/Import- oder Restore-Versuch nötig. Wecker nicht alle löschen, um sie neu anzulegen.

## Ein In-place-Update – keine Neuinstallation

1. In der vorhandenen Sanftwecker-App `App aktualisieren → Nach Update suchen` öffnen und v20 herunterladen. Wenn der Browser nicht wie erwartet öffnet, über [direkte signierte v20-APK](https://github.com/CobraChris/app-fabrik-template/releases/download/sanftwecker-v20/sanftwecker-release.apk) herunterladen.
2. Im Android-Systemdialog muss eine Aktualisierung der **bestehenden** App angeboten werden. Wenn stattdessen eine Deinstallation, eine andere Paket-ID oder eine Signaturfehlermeldung auftaucht: **abbrechen**, Screenshot anfertigen und den Fehler melden.
3. Nach dem Update in der App **Build 20 / 0.9.1-beta** prüfen. Vorhandene Einträge dürfen nicht verschwunden sein.

## Funktionsabnahme (jeweils OK / Fehler / nicht getestet festhalten)

- [ ] Bestehende Wecker, Wiederholungsmuster und Alarmdetails unverändert
- [ ] Mehrere vorhandene Timer und Stoppuhrstände noch vorhanden
- [ ] Einen neuen Testalarm etwa 2 Minuten in der Zukunft erstellen und tatsächlich auslösen
- [ ] Sanfter Tonanstieg und Handy-Vibration auch bei **lautlosem** Handy (keine vorschnelle Änderung globaler Android-Lautstärke)
- [ ] Galaxy Watch trägt die erwarteten schnellen/längeren Vibrationsimpulse zuverlässig bei ausgeschaltetem Uhrdisplay
- [ ] Vollbild-Anzeige bei Alarm, korrektes Beenden/Schlummern, kein steckenbleibender Klingelprozess
- [ ] Benachrichtigungen, genaue Alarme, Overlay/Vollbild und Akku-Hintergrundfreigaben weiterhin vorhanden; gegebenenfalls Android-Freigabedialog prüfen
- [ ] Ein Timeralarm löst aus, auch wenn die App im Hintergrund ist
- [ ] Nach einem regulären Handy-Neustart bleiben Wecker/Timer erhalten und der nächste geplante Alarm ist weiterhin aktiv
- [ ] Unter `App aktualisieren` funktioniert die manuelle Update-Abfrage; sie zeigt später neue Releases aus altem **oder** neuem GitHub-Repository an. **Aktuell noch keine v21 installieren**.

## Welche Rückmeldung reicht?

Zum Beispiel: `v20 installiert, Wecker/Timer erhalten, Alarm+Watch+Lautlos OK, Overlay OK, Neustart noch nicht getestet`. Ein einzelnes fehlgeschlagenes Verhalten mit Screenshot reicht für eine gezielte Fehlerbehebung; die Version auf keinen Fall deinstallieren, um auf v19 zurückzukommen.

## Veröffentlichungskontrolle

- GitHub-v20-Releasenachweis: [Sanftwecker v20](https://github.com/CobraChris/app-fabrik-template/releases/tag/sanftwecker-v20)
- SHA-256 der signierten v20-APK: `0c4e56c55b04d720768f60c3c8b35cad212212df470bdfdeb265715b747909eb`
- GitHub-Signierlauf: [#42479300564](https://github.com/CobraChris/app-fabrik-template/actions/runs/42479300564), **SUCCESS**
- Folgeversion **v21** bleibt bis zur ausdrücklichen weiteren Nutzerfreigabe gesperrt: [Draft-PR #5](https://github.com/CobraChris/sanftwecker/pull/5)

**Nur ein echter Gerätetest kann bestätigen, dass Android und die Galaxy Watch nach dem Upgrade genauso funktionieren wie vorher.**
