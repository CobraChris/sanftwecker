# Sanftwecker: Watch-Intensiv und Impulsabstand – gezielte Korrektur

## Befund aus Hardware am 12./13.11.2026
- Nutzer wählt **Intensiv**, **12 Watch-Impulse**, **3 Sekunden**, berichtet aber nur ein
  anfängliches kurzes Zucken; direkte Watch-Vibration funktioniert.
- Die bisherige Kaskade ignorierte die eingestellte Impulsanzahl und den Abstand.
  Stattdessen verwendete sie wiederholte Waveform (Intensiv 140 ms / 80/255,
  danach 6500 ms Ruhe). Android kann die tatsächliche Wiedergabe verändern.
- Der Stoppbildschirm des Handys ist ein separates offenes Issue #50.

## Korrektur (nur Entwicklungs-PR, keine APK!)
- Anzahl und Intervall werden zusätzlich, **abwärtskompatibel**, über DataLayer
  und erweiterte 8-Feld-RING-Pakete übermittelt. Alte 3–6-Feld-Pakete erhalten
  sichere Standardwerte; bei zeitlich versetzten Updates ist die bestehende
  lokale Alarmplanung weiterhin verfügbar.
- Wear OS startet jede sanfte Vibration als **endlichen Einzelimpuls** mit
  Handler-Wiederholung zum gespeicherten Intervall; nach der gewählten Anzahl
  endet Phase 1. Damit werden nicht endlos wiederholende kurze Waveforms benötigt.
- Intensiv bekommt einen bewusst besser spürbaren ersten Impuls (320 ms,
  Amplitude 185/255). Normal- und Sanft-Intensität sowie Phase-2-Muster
  bleiben bestehen. Stage-2-Übergang nach 60 Sekunden bleibt erhalten und
  überschreibt verbleibende Phase-1-Runnables.
- Stop/Snooze/Destroy entfernt Phase-1-Callbacks und beendet den Vibrationsmotor.
- Geänderte Beschriftung verdeutlicht die Bedeutung der beiden Schieberegler.
- Kein neuer Schlüssel, keine Deinstallation, kein Release. Bestehende v34
  läuft weiter. Nicht als Hardwarefix bewerten, bis mit gekoppelter v35-Hardware
  verifiziert.

## Regressionen
- Verifikation: 3-, 5-, 6- und 8-Feld-Pakete; ungültige Werte verwerfen,
  Legacy-Protokoll erhalten.
- Der Kaskaden-Umschaltpunkt bleibt exakt bei +60 s, Normal und Sanft unverändert.
- Pulse sind endlich (repeat = -1), maximal 12, Intervall aus vorhandener Liste.
- Stopp entfernt alle Pulse-Callbacks, damit keine Vibration nach Alarmende startet.
