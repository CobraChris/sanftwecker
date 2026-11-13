# Sanftwecker – Berechtigungsstatus, Watch-Alarmbedienung und Haptik (Entwicklung)

Stand 13.11.2026. **Kein Release, keine Signierung, keine Installation.**

## Ausgangssituation
- Nutzer bestätigt: v35 auf Handy und Galaxy Watch; Weck- und Vibrationsfunktion
  funktionieren grundsätzlich.
- Beim Handy wurde der Klingelbildschirm nach einem Update erneut erlaubt;
  Screenshot der Samsung-Systemseite zeigt aktuell **Berechtigung erteilen: EIN**.
  Der tatsächliche Zustand **vor** der Aktion ist nicht dokumentiert.
- Auf der Galaxy Watch erscheint die Alarmansicht nicht automatisch,
  obwohl die Vibration auslöst.
- „Normal“ ist kaum fühlbar, „Sanft“ praktisch nicht, „Intensiv“ passt.

## Änderungen
1. **Berechtigungsdiagnose, kein automatischer Grant:** Auf der Handy-Startseite
   dauerhafte Anzeige des tatsächlichen Android-FSI-Status; der vorhandene
   Freigabeknopf ist nur bei fehlender Berechtigung sichtbar. Nach einem
   regulären Paketupdate protokolliert `RescheduleReceiver` den vom System
   gemeldeten Status. Keine neuen Permissions, kein Versuch, Android zu umgehen.
2. **Wear-OS-Bedienung:** In der bestehenden Alarmbenachrichtigung wird die
   Aktion **Alarmansicht** neben **Stoppen** angeboten. In der Watch-App
   erscheint beim aktiven Alarm **Alarmansicht öffnen**. Beide führen aus
   der Nutzerinteraktion heraus zu `WatchRingActivity`; die unabhängig
   ausgeführte Vibrationssteuerung und die bereits programmierten exakten
   Uhrenalarme bleiben unverändert. Wear OS unterstützt keine FSI; ein
   automatisches Aufpoppen auf jedem Gerät ist **nicht garantiert**.
3. **Kaskaden-Vibrationsstufen:** Erste Minute nur:
   Sanft 145 ms / 64 von 255, Normal 205 ms / 112 von 255,
   Intensiv unverändert 320 ms / 185 von 255.
   Auswahl und gespeicherte Profilwerte bleiben erhalten, ebenso Impulszahl,
   Intervall, Phase-2-Muster sowie der Übergang bei 60 Sekunden.
   Die tatsächliche Wahrnehmung muss am Gerät geprüft werden.
4. **Zusätzliche Korrektur:** Lokale Watch-Alarmauslösung liest Impulszahl und
   Intervall *vor* dem Löschen der gespeicherten Einzelplanung. Andernfalls
   fielen die Werte beim lokalen Alarm unbemerkt auf Standardwerte zurück.

## Abnahme auf echter Hardware
- Full-Screen-Berechtigung nach Update und App-Neustart prüfen;
  Statusanzeige muss dem Android-Schalter entsprechen. Bei Verlust Android-
  Berechtigung erneut manuell erlauben; Ursache anhand Log klären.
- Uhralarm im gesperrten Zustand auslösen; wenn die UI nicht automatisch
  erscheint, über Alarmbenachrichtigung **Alarmansicht** öffnen. Stop/Snooze
  müssen sichtbar und nutzbar bleiben. Dabei keine FSI auf der Uhr erwarten.
- Sanft/Normal/Intensiv einzeln prüfen, mindestens 75 Sekunden pro Kaskade;
  Phase 2 soll unverändert kräftig auslösen. Direkter Watch-Test separat.
- Die bestehenden Wecker und Originalsignatur unangetastet lassen. Neue
  signierte APKs nur nach einer separaten ausdrücklichen Releasefreigabe.
