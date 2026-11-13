# Sanftwecker v36 – Verbesserungen und Hardwareabnahme

13.11.2026. Nur CI-geprüfter Quellstand – **kein Release durch diesen Entwicklungs-PR**.

## Aus v35 bereits vorbereitet (PR #54)
- Android-FSI-Berechtigungsstatus sichtbar, Freigabeknopf nur bei fehlender Berechtigung;
  Log nach `MY_PACKAGE_REPLACED` ohne stille Neufreigabe.
- Wear-OS-Alarmbenachrichtigung bietet Alarmansicht/Stoppen/Schlummern als manuelle
  Wege. Watch-Hauptseite bietet bei aktivem Alarm „Alarmansicht öffnen“.
  Automatisches Öffnen kann das Betriebssystem beschränken.
- Watch-Kaskade: Sanft 145 ms / 64, Normal 205 ms / 112, Intensiv **unverändert**
  320 ms / 185. Phase 2 unverändert ab +60 s.
- Watch-Alarm liest Pulsanzahl und Abstand vor dem Löschen seiner Planungsdaten.

## V36: Ergänzung für Handy-Stoppbildschirm
- Die Alarmbenachrichtigung bietet bei laufendem Wecker explizit „Alarmansicht“
  und weiterhin vorrangig „Stoppen“ sowie optional „Schlummern“. Tippen auf die
  Benachrichtigung selbst öffnet den bestehenden Alarmbildschirm.
- Beim Entsperren zeigt die Sanftwecker-Hauptseite nur während eines aktiven
  Alarms zusätzlich „LAUFENDEN WECKER ÖFFNEN – Stoppen / Schlummern“.
- Der Foreground-Dienst speichert rein für die UI eine zeitbegrenzte Kennung,
  entfernt sie nach Stoppen/Auto-Stopp und nach App-Ersatz / Reboot.
  Die **Alarmplanung** hängt niemals von diesem UI-Status ab.
- Keine Hintergrund-Activity-Launches. Android 14+ startet bei entsperrtem Handy
  typischerweise Heads-up statt Vollbild. Die FSI-Sonderberechtigung bleibt
  Nutzerentscheidung.

## In-place Update / Regression
- Beide Apps VersionCode 36, Handy 0.9.17-beta, Watch 0.9.17-watch,
  unveränderte App-ID `com.cobrachris.sanftwecker`.
- Keine Änderung an Uhr-AlarmManager, Weckerkaskade, Audio, Signier-Identität,
  Self-Update-Prüfung, DND oder Android-Berechtigungsmanifest.
- Nach Aktualisierung: FSI-Status kontrollieren, Wecker sichtbar/stopbar am
  gesperrten und entsperrten Handy, Watch-Benachrichtigungsaktion und lokale
  Watch-Stopp-/Schlummerfunktionen prüfen, alle Intensitäten einmal testen.
- Alle realen Geräteprüfungen sind ausstehend, selbst wenn unsignierte CI besteht.
