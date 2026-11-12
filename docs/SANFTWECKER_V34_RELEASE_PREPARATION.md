# Sanftwecker v34 – Originalsignatur-Releasevorbereitung

Stand 12.11.2026. **Kein Release, keine Signierung, keine Installation.**

Vorbereitung nach geprüfter UI-/Berechtigungsfluss-Korrektur aus PR #45.

- Nur `requests/sanftwecker-v34-release.json` kann später nach einer eigenen
  **ausdrücklichen Nutzerfreigabe** die einmalige Signierpipeline auslösen.
  Diese Datei ist **absichtlich NICHT** enthalten.
- Der spätere Antrag ist an einen unveränderlichen, gesondert verifizierten
  SHA-40-Commit gebunden. Der Push auf `main` muss genau die neue
  Antragsdatei enthalten; nur `CobraChris` ist zulässig.
- Der geschützte Job vergleicht die offiziellen signierten v33-APKs vor
  jeglichem Zugriff auf Signing-Secrets:
  - Phone SHA-256: `4f570ea5be75321bdadf47606eefa3adf0cb1504bc588881eb99c6d0da9e7e46`
  - Watch SHA-256: `594c65f6f3dcdfcc1b79bc6759b7a2a5aeb96c32ee1653ddbd4157a031c94f06`
  - Beide Zertifikate, dieselbe Paket-ID und `versionCode 33`.
- V34 besteht aus `versionCode 34`, Phone `0.9.15-beta`, Watch
  `0.9.15-watch`. Unsignierte Android-Unit-Tests, Lint und Release-Build
  vor der Signatur. Originalzertifikat wird mit v33 verglichen,
  sonst Abbruch. Keine neuen Schlüssel, kein Überschreiben alter Releases.
- Die reale Watch muss die von v33 selbst erkannte und installierte v34
  anschließend testen (Version, Android-Berechtigung, UI, Weckverhalten).
- Keine automatische Installation auf Handy oder Uhr, keine Deinstallation,
  keine Kosten, keine Änderung am signierten v33-Release.

**Releasefreigabe fehlt noch.** Das Kommando `N` gilt nicht als solche.
