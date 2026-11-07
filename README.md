# Sanftwecker (Android)

Eigenständiges Repository für die bestehende Android-Sanftwecker-App aus `CobraChris/app-fabrik-template`.

**Migration in Vorbereitung:** Der Android-Quellcode stammt aus `feature/watch-vibration-poc`. Eine neue Signatur, ein neuer Release-Kanal oder eine signierte APK-Veröffentlichung werden durch diesen Import **nicht** aktiviert. Bestehende Nutzer behalten vorerst den Updatekanal `CobraChris/app-fabrik-template`.

- Paket-ID: `com.cobrachris.sanftwecker` (unverändert)
- Referenzquelle: https://github.com/CobraChris/app-fabrik-template/tree/feature/watch-vibration-poc/experiments/watch-vibration-poc
- Signierte Alt-Releases: https://github.com/CobraChris/app-fabrik-template/releases
- Migrations-Checkliste: https://github.com/CobraChris/app-fabrik-template/issues/25

Der CI-Workflow baut und testet **ausschließlich unsigniert**. Neue signierte Releases dürfen erst nach dokumentierter Zertifikats-Fingerprint-Gleichheit, sicherer Übertragung der geschützten Signing-Secrets und tatsächlichem In-place-Update-Test des Geräts in Betrieb gehen. Bis dahin ist die APK vom bisherigen GitHub-Releases-Kanal maßgeblich.

## Entwicklung

Gradle 8.11.1 und JDK 17 mit Android SDK 35:

```sh
gradle --no-daemon :alarm:testDebugUnitTest :alarm:lintDebug :alarm:assembleDebug
```

Die Module `alarm`, `phone` und `watch` werden für Quellstandtreue zunächst beibehalten; die Alltags-App ist `alarm`.
