# Sanftwecker v29 – klar differenzierte Watch-Kaskade

Ausgangslage (Hardwaretest am 11.11.2026): Nutzer meldete mit auf Handy und Uhr
installierter v28, dass Profil **Sanft** in Phase 1 zu stark ist und der
Unterschied beim Übergang ab 60 Sekunden nicht spürbar genug ausfällt.
Alle übrigen Alarm-, Watch-, Schlummer- und Stoppfunktionen funktionieren.

## Eingriff nur ins Sanft-Profil
- **Phase 1 (0–60 s)**: 45-ms-Minipuls, danach 17 Sekunden Ruhe.
  Amplitude 20 (falls Watch-Motor steuerbar), bei nicht regelbarem Motor bleibt
  die besonders kurze Impulsdauer und lange Pause erhalten.
- **Phase 2 (ab 60 s)**: Erkennbarer Doppelimpuls
  250 ms ein / 200 ms Pause / 250 ms ein / 2500 ms Pause.
  Amplitude 175, falls steuerbar.
- Der Übergang beendet die laufende Phase-1-Wellenform ausdrücklich per
  Vibrator.cancel() vor der Phase-2-Wellenform.
- Die bekannten Profile **Normal** und **Intensiv** bleiben unverändert.
  Ebenfalls unverändert: 60-s-Umschaltzeitpunkt, Handyton und Handyvibration,
  Schlummern, Stoppen, Weltuhr, Weckerprofile, Offline-/Autostartplanung und
  die v28-Paketübertragung.
- Bereits gespeicherte **Sanft**-Einstellungen bleiben erhalten und benutzen
  nach einem später freigegebenen v29-Update automatisch die verfeinerte Kurve.

## Nachweis und Diagnose
- Watch-App zeigt bei aktivem Alarm auf ihrem Bildschirm:
  gewähltes Profil sowie „Phase 1 – leise“ bzw. „Phase 2 – deutlich“
  anhand des vorgesehenen 60-s-Zeitpunkts.
- Die Uhr sendet weiterhin native `CASCADE_GENTLE`- und
  `CASCADE_STRONG`-ACKs. Die Handy-App akzeptiert und protokolliert diese
  nun in ihrer bestehenden Diagnose. Die ACKs erlauben zu prüfen, ob beide
  Phasen von der Watch-Alarmsteuerung ausgeführt wurden.
- Unit-Regressionstest prüft v29-Sanft-Impulsdauern, deutlichen Unterschied
  selbst ohne Amplitudensteuerung, alte Normal-Muster, 60-s-Phasengrenze und
  sichere Nachrichtenvalidierung.

## Abnahmekriterien auf der Galaxy Watch
1. Beide Geräte per Originalsignatur als v29 **in-place** aktualisiert;
   keine Deinstallation oder neuen Berechtigungen.
2. Wecker mit zweistufiger Kaskade und Watch-Profil **Sanft**:
   bis 60 Sekunden maximal kurze einzelne Impulse (~45 ms alle ~17 s)
   und am Handy keinerlei Vibration.
3. Ab 60 Sekunden Doppelimpulse auf der Watch (statt Einzelimpulsen)
   und beginnende Handy-Vibration. In der Watch-App wird die jeweilige Phase
   angezeigt; Handy-Diagnose enthält nach Möglichkeit beide ACKs.
4. Schlummern und Stoppen funktionieren auf beiden Geräten wie in v28.
5. **Noch kein v29-Release freigegeben.** Diese Änderung zunächst nur als
   unsignierter, CI-geprüfter Entwicklungsstand veröffentlichen. Für die
   Originalsignierung und das Veröffentlichen sind die explizite Einzelfreigabe
   und die geschützte Pipeline nötig.
