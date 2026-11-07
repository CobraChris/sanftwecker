# Sanftwecker – signierte Alt-Releases prüfen, ohne Signing-Migration

**Stand:** 08.11.2026. Diese Prüfung ist bewusst **read-only**. Sie
installiert weder eine neue Android-Signatur noch veröffentlicht sie eine APK.

## Automatischer CI-Nachweis
In `.github/workflows/android-ci.yml` wird zusätzlich zum bisherigen
Android-Lint/Unit-Test/Debug-Build eine **unsignierte Release-APK**
erstellt. Anschließend führt CI das Skript
`scripts/verify-migration-baseline.sh` aus:

1. Lädt ausschließlich die öffentlichen, bisher signierten Alt-Releases
   `sanftwecker-v18` und `sanftwecker-v19` aus
   `CobraChris/app-fabrik-template` herunter.
2. Kontrolliert die unveränderlich dokumentierten SHA-256-Dateiprüfsummen:
   - v18: `c0812ea9bb96520f9f5be1aa9daa1c4feffb3b1580be3aab7761fda0513cbaed`
   - v19: `04ae0383d9faf1868a41ee1970c2038e5a52970e10e71b32c2832d15cf2f5f50`
3. Prüft beide vorhandenen APKs mit Android `apksigner verify`
   und vergleicht **deren Zertifikats-SHA-256-Fingerabdrücke**.
4. Prüft im neuen Repository das identische Paket
   `com.cobrachris.sanftwecker`, `versionCode >= 19`,
   ein **unsigniertes** Build-Artefakt und weiterhin die alte
   Update-Releases-Adresse.

**Erwartetes Ergebnis:** Ein grüner Lauf beweist die Integrität und
Zertifikatsgleichheit der **zwei alten signierten APKs** und dass sich
beim unsignierten Quellcode-Umzug Paketname/Updateadresse nicht geändert
haben. Er beweist **nicht** die Identität eines zukünftigen Keystores
im neuen Repository und **nicht** die Updatefähigkeit am Samsung.

Der Workflow benötigt ausschließlich das normale read-only
GitHub-Actions-Token. Keine GitHub-App-Installation, Secrets,
gesonderten API-Schlüssel oder zusätzlichen kostenpflichtigen Dienst.

## Strikt offen vor dem ersten neuen signierten Release
- Geschützte Wiederverwendung des **existierenden** App-Fabrik-Signierkeys
  aus dem privaten verschlüsselten Backup; niemals einen neuen erzeugen.
- GitHub-App-Zugriff auf das neue Repo, sofern GitHub das erfordert.
- Neues Signing-Test-Artefakt muss mit dem **alten** Zertifikats-Fingerprint
  übereinstimmen und einen höheren `versionCode` verwenden.
- Updatebrücke vom alten GitHub-Releases-Feed zu dem neuen Feed,
  anschließend echter Test auf dem Samsung (App-Daten/Wecker/Uhr,
  gesperrter Bildschirm, Berechtigungen, Stummmodus).
- Erst danach Release-Kanal wechseln; den bisherigen Signierkey
  und die vorhandene Installation nicht löschen oder ersetzen.

Siehe die verbindliche Checkliste:
https://github.com/CobraChris/app-fabrik-template/issues/25
