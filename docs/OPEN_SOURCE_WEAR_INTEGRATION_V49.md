# v49 – Wear-OS-Kachel, Zifferblatt und Timerbestätigung

## Referenz und Lizenz
- Architekturvorlage: **AlarmClockXtreme**, https://github.com/SysAdminDoc/AlarmClockXtreme
- Vor allem `wear/NextAlarmTileService.kt`, `wear/NextAlarmComplicationDataSourceService.kt`, `wear/WearAlarmDataListenerService.kt` sowie `app/src/play/.../WearAlarmActionListenerService.kt`
- Upstream-Lizenz: **Apache-2.0**, siehe https://github.com/SysAdminDoc/AlarmClockXtreme/blob/main/LICENSE
- Hier wurde **kein Quellcode direkt kopiert**. Die Komponenten sind eigenständig in Java für Sanftweckers bestehende Scheduling- und Wear-Data-Layer-Architektur implementiert; Strukturideen und Konzepte wurden als Referenz genutzt.
- Falls später originale Text- oder Codepassagen übernommen werden, müssen Lizenz-, Copyright- und gegebenenfalls NOTICE-Vorgaben anhand der verwendeten Revision zusätzlich geprüft und erfüllt werden.

## Funktionsumfang im PR #96
1. `SanftweckerNextAlarmTileService` zeigt die lokal gespeicherte nächste Weckzeit auf einer optionalen Wear-OS-Kachel. Wenn ein **echter Wecker** aktiv ist, kann die Kachel dessen vorhandene Service-Aktionen Stoppen bzw. nur bei erlaubtem Schlummern auslösen. Der Weckerservice bleibt unverändert.
2. `SanftweckerNextAlarmComplicationService` gibt nächste Weckzeit als SHORT_TEXT oder LONG_TEXT an kompatible Zifferblätter. Die Auswahl einer Komplikation bleibt beim Nutzer.
3. `WatchAlarmPresentation` triggert Kachel- und Zifferblatt-Aktualisierungen beim Ändern der lokalen Alarmplanung; Ausfälle der optionalen Anzeige dürfen die zuverlässige Weckplanung nicht beeinträchtigen.
4. Timeraktionen auf der Uhr erzeugen zunächst **nur eine Anfrage**. Das Handy verlängert nur die exakte, noch beendete Timer-Instanz und antwortet über `EXTEND_RESULT` mit `ok` oder `rejected`. Die Uhr prüft Timer-ID, alte Fälligkeit und Zeitaddition und zeigt erst danach eine bestätigte Benachrichtigung.
5. Bereits in v49 vorhandene Kompaktansicht und +30s/+1m Watch-Schnellaktionen bleiben bestehen. Watch und Handy behalten ihre getrennten identitätsgeprüften APKs; das vereinfachte Update über das Handy bleibt aktiv.

## Abnahme
- Android-CI (Node-Vertragstests, Android-Unit, Lint, Debug- und Unsigned-Release-Build) für **beide** Module grün.
- Galaxy Watch Hardwareprüfung: Kachel manuell hinzufügen, nächste Weckzeit prüfen, laufenden Wecker stoppen und optional schlummern; ursprüngliche Wecker-Vibration und Timeralarm bleiben unverändert.
- Watch-Zifferblatt-Editor: Sanftwecker-Komplikation auswählen; Anzeige nach Alarmänderung prüfen.
- Timer auf Uhr ablaufen lassen, +30 Sekunden wählen; **nur nach Handy-Antwort** eine Erfolgsmeldung; bei fehlender Verbindung oder altem Timer keine falsche Erfolgsmeldung.
- Keine Release-Signierung, kein Merge in main und keine Installation ohne gesonderte Freigabe.
