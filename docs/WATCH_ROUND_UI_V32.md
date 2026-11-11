# Sanftwecker v32 – Watch-Updateknopf auf rundem Display erreichbar

## Fehlerbericht 11.11.2026
Nutzer öffnet die v31-Galaxy-Watch-App, sieht „Nächster lokaler Wecker“,
„ALARMVIBRATION TESTEN“ und „ALARM BEENDEN“. Die unterhalb platzierte
Schaltfläche „Uhr aktualisieren“ ist nicht sichtbar, und Wischen in jede
Richtung verändert den Bildschirm nicht.

## Ursache im v31-Code
- `WatchMainActivity` setzt Update **unter** einen dauerhaft sichtbaren
  „Alarm beenden“-Knopf, der ohne aktiven Alarm keinen Nutzen hat.
- `ScrollView.setFillViewport(true)` und zentrierter Inhalt ohne
  Reserve für die runden Bildschirmränder können dazu führen, dass der
  abgeschnittene Bereich nicht als zusätzliche Scrollhöhe zählt.
- Dies erklärt die Beobachtung schlüssig, beweist aber nicht allein die
  tatsächlich auf der Watch installierte Versionsnummer.

## Korrektur
- **Kein Alarm aktiv:** Status/Nächster Wecker, dann unmittelbar der
  **Uhr aktualisieren**-Knopf, danach **Alarmvibration testen**.
  Nicht benötigtes „Alarm beenden“ wird ausgeblendet.
- **Alarm aktiv:** Update und lokale Vibrationstest-Schaltflächen werden
  ausgeblendet; „Schlummern“ (falls konfiguriert) und **Alarm beenden**
  bleiben sichtbar und verwenden ihre bisherigen Service-Aktionen.
- `ScrollView` beider Watch-Ansichten hat oben und unten Raum für
  die runden Ränder sowie eine garantierte scrollbare Mindesthöhe.
- Auf der nachgeordneten Updateansicht steht **Update suchen** direkt
  unter der kurzen Überschrift; Statusinformationen und die beiden
  (zunächst deaktivierten) Download-/Installationsschaltflächen folgen.
- Keine Änderung an AlarmManager, Vibration, Watch-Kaskade,
  Offlinealarm, Watch-Installationsberechtigungen, Signaturprüfung,
  Updatequelle oder den vorhandenen Updates.
- Beide APKs nutzen versionCode 32, Handy 0.9.13-beta, Uhr
  0.9.13-watch; Entwicklungsstand **nur unsigniert**.

## Abnahme
- Unit- und Quelltext-Regressionstests prüfen die Sichtbarkeit/Ordnung
  und die unveränderten Stopp-/Schlummern-Serviceaktionen sowie
  explizite Scrollreserve.
- Unsignierte CI, Lint und Release-Builds für Handy und Watch.
- Hardware-Abnahme nach separater v32-Freigabe:
  „Uhr aktualisieren“ muss bei v32 ohne Wischen sofort auf dem
  Watch-Startbildschirm sichtbar sein. „Update suchen“ zeigt mangels
  neuerer veröffentlichter Version „Die Watch-App ist aktuell…“.
- **Wichtig:** Um das UI in v32 testen zu können, ist unter Umständen
  noch einmal der bisherige GeminiMan-Installationsweg nötig.
  Ein tatsächlicher Self-Update-Installationsversuch ist erst nach
  einem späteren, separat autorisierten neuen Release prüfbar.

## Releasegrenze
Keine Signierung, Veröffentlichung oder Installation ohne ausdrückliche
Releasefreigabe. Der bestehende v31-Release und Geräte-Apps bleiben unverändert.
