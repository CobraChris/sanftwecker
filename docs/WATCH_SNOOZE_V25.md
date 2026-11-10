# Sanftwecker – Schlummern direkt auf der Galaxy Watch

**Stand 10.11.2026 – Quellcode / automatische Tests, kein signierter Release, kein Gerätetest.**

## Bedienung

Bei einem echten, aktiven Wecker zeigt die native Galaxy-Watch-App neben **Stoppen** jetzt **Schlummern · X Min.**; die Aktion ist sowohl in der aktiven Alarmbenachrichtigung als auch in der Uhr-App erreichbar. X ist exakt die im Handy-Weckerprofil gespeicherte Schlummerdauer. Wenn Schlummern in diesem Profil deaktiviert ist, gibt es auf der Uhr keinen entsprechenden Knopf.

## Was passiert beim Tippen?

1. Die Uhr sendet eine gebundene Nachricht mit Wecker-ID und ursprünglichem Alarmzeitpunkt über den bestehenden privaten Wear-Data-Layer an das gekoppelte Handy.
2. Das Handy prüft, dass genau dieser Alarm **aktuell klingelt**, Schlummern dafür aktiviert ist und die ursprüngliche Auslösung zeitlich identisch ist. Eine alte, verdoppelte oder nachträglich zugestellte Nachricht darf keinen anderen Wecker anhalten.
3. Nur wenn Android erfolgreich einen neuen **exakten Schlummeralarm** für die eingestellte Minutenzahl plant, stoppt der Handy-Dienst Ton und Handyvibration und sendet über den bestehenden STOP-Kanal den bestätigten Stopp an die Uhr.
4. Die Uhr beendet ihre Haptik **erst nach dem vom Handy bestätigten Stopp**. Wenn die Verbindung fehlt oder die Bestätigung ausbleibt, bleibt die lokale Vibration aktiv, und die Uhr zeigt „Handy nicht erreicht – erneut versuchen“ statt fälschlich einen erfolgreichen Schlummerzustand anzunehmen.
5. Zur Schlummerzeit starten beide Geräte über den bisherigen AlarmScheduler und die synchronisierte Watch-Alarmplanung erneut.

## Rückwärtskompatibilität und Sicherheit

- Daten- und Message-Protokoll `/sanftwecker/alarm/v1` bleibt erhalten. Neue v25-Pakete ergänzen die Schlummerdauer, ältere drei- oder vierteilige Startpakete werden unverändert erkannt und bieten **keine** nicht autorisierte Schlummeroption an.
- Die lokal gespeicherten Uhr-Zeitpläne und Live-Zustände erhalten `snooze_minutes`. Ein temporär vom Handy getrenntes Gerät kann weiterhin lokal klingeln. **Ohne Verbindung zum Handy kann keine gemeinsame Schlummerung bestätigt werden**; der Wecker läuft dann bewusst weiter statt still zu enden.
- Stoppen auf der Uhr bleibt als jederzeitige Not-Abbruchaktion verfügbar.
- Keine neue Signatur, keine neuen Serverdienste, keine Freigabe-/Installationsänderung. Handy und Uhr müssen in einer späteren **gemeinsamen signierten Beta** mit bestehendem Originalzertifikat aktualisiert werden.
- Sowohl die zuletzt signierte Handy-Version v24 als auch die zuletzt signierte Watch-Version v23 bleiben bis zur ausdrücklichen Nutzerfreigabe unverändert.
- Tests: Protokollkompatibilität, gültige/ungültige Schlummerdauer, Eingrenzung der Alarm-ID sowie Android-/Wear-OS-Build und Lint. Die tatsächlichen Uhrenbenachrichtigungsflächen und getrennte-/gekoppelte-Geräte-Situationen müssen später praktisch getestet werden.
