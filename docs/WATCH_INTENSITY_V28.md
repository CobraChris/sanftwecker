# Sanftwecker v28 – einstellbare Watch-Kaskadenvibration

**Stand 10.11.2026: Entwicklungs-PR, nur unsignierte CI; keine Veröffentlichung oder Geräteinstallation.**

## Motivation und Abnahme von v27

Der Nutzer hat Weltuhr, lokale Watch-Vibration, Handy/Watch-Alarm, Stoppen, Schlummern
und zweistufige Kaskade auf echter Hardware erfolgreich getestet. Die Wrist-Vibration
der zweiten Phase ist etwas zu intensiv. **v27 darf deshalb weder verändert noch
durch einen ungetesteten Release ersetzt werden.**

## Bedienung nach einem später freigegebenen v28-Update

**Sanftwecker (Handy) → Wecker → konkreten Wecker → Galaxy Watch →
Watch-Vibration bei zweistufiger Kaskade**. Drei Profile je Wecker:

- **Sanft:** Phase 1: 65 ms alle ~12,6 s (Amplitude 28, sofern steuerbar);
  Phase 2: 200 ms alle ~2,2 s (Amplitude 130).
- **Normal:** exakt die v27-Watch-Kaskade (Phase 1: 110 ms alle 9,1 s;
  Phase 2: bekannte kräftige 850-ms-Pulsfolge). Standard aller Bestandswecker.
- **Intensiv:** Phase 1: 140 ms alle ~6,6 s;
  Phase 2: 1-s-Pulse mit kurzen Pausen, Maximalamplitude.

Die Auswahl gilt **nur bei aktivierter zweistufiger Kaskade**. Nicht-Kaskadenalarme
behalten unverändert ihr ursprüngliches Verhalten. Die tatsächliche fühlbare Stärke
ist hardwareabhängig; auch Wear-OS-Uhren ohne regelbare Amplitude unterscheiden
die Stufen durch Pulsdauer und -abstand. Es gibt keine automatische Stärkenänderung
beim Update: Normal bleibt Standard, bis ein Nutzer einen Wecker bewusst ändert
und auf **✓** speichert.

## Übertragung und Sicherheit

- Handy speichert die Auswahl pro Wecker in den vorhandenen SharedPreferences.
  Alte Weckprofile erhalten automatisch Normal. Duplikate, Einmal-Ausnahmen,
  aktivieren/deaktivieren und Stoppen/Schlummern erhalten den Wert.
- Wear Data Layer synchronisiert die Intensität in **Schedule** und **State**.
  Die Uhr speichert sie im lokalen Alarmplan. Bei Offlinealarm wird diese
  Einstellung verwendet, ohne Onlineverbindung oder neue Berechtigungen.
- Erweiterter Ring-Message-Payload mit einem strikt validierten sechsten Feld
  (0–2); bestehende 3-/4-/5-Feld-Nachrichten werden weiterhin verstanden.
  Für Normal sendet v28-Handy weiterhin das alte 5-Feld-Format an eine
  möglicherweise noch nicht aktualisierte v27-Uhr.
- Wird bei einer *gemischten Installation* (Handy v28, Uhr v27) Sanft gewählt,
  kann die alte Uhr die neue direkte Nachricht nicht interpretieren; ein
  bereits synchronisierter lokaler Alarm bleibt möglich, allerdings mit v27-Rhythmus.
  **Vor der Nutzung der neuen Profile beide Geräte auf v28 aktualisieren.**
- Keine Änderungen am Originalsignierzertifikat, Alarmpaket, Alarmberechtigungen,
  Releasepipeline oder bestehenden gespeicherten Uhr-/Wecker-Einstellungen.

## Tests

- Unveränderte 60-Sekunden-Phasengrenze, unverändertes Normal-Muster.
- Sanft ist kürzer und seltener als Normal; zweite Phase stärker als erste.
- Timings und Amplituden haben passende Array-Längen.
- Strict Codec für 0/1/2, Ablehnung manipulierter Pakete; alte Pakete bleiben lesbar.
- Persistente Modelle und Duplikate behalten Intensität, Standard 1/Normal.
- Vollständige CI für beide Module plus unsignierte Builds.

**Vor einem Release notwendig:** eigener v28-Releasefreigabeauftrag, Originalsignatur,
v27-Geräte-Upgrade ohne Deinstallation, Hardwareabnahme aller drei Modi, Wechsel an
der 60-Sekunden-Grenze sowie Stoppen/Schlummern und Boot-/Offlinealarm.
