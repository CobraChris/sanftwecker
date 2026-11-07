# Sanftwecker-Migration – sichere Etappen

Stand: 07.11.2026. Ziel ist die Trennung von `CobraChris/app-fabrik-template` in `CobraChris/sanftwecker` bei kompatiblen Updates der bereits installierten Android-App.

- [x] Automatisiertes öffentliches Repository mit README erstellt, GitHub Actions #42384045324 erfolgreich
- [x] Quellcode und Tests in isolierten Migrationszweig kopiert (kein Cutover)
- [x] GitHub-CI, Unit-Tests, Lint und unsignierten Build des kopierten Projekts prüfen – erfolgreich vor und nach Merge, letzter Hauptzweig-Run #42384995807
- [ ] Quellstand und alte Releases vor Übergabe erneut abgleichen
- [ ] Signiermigration: technischer Workflow in [Controller-PR #14](https://github.com/CobraChris/github-app-fabrik-control/pull/14) erstellt, Tests erfolgreich; gesonderte Sicherheits-/Mergefreigabe und anschließender einmaliger GitOps-Auftrag fehlen noch. Verwendet vorhandenes Backup und identisches Zertifikat; **kein neuer Keystore**.
- [ ] Ersten signierten neuen Build anhand des SHA-256-Zertifikatsfingerprints gegen Alt-Release verifizieren; neuer `versionCode` **größer als 19**
- [ ] Bisherige Release-URL im In-App-Updater mittels verifiziertem Brückenrelease kompatibel halten oder bewusst eingeführte sichere Dual-Repo-Updateprüfung testen
- [ ] Zwei signierte In-place-Upgrades auf Samsung prüfen, inklusive Wecker/Timer/Stoppuhr, Galaxy Watch, Bildschirmfreigabe und Sperrbildschirm-Alarm
- [ ] Erst nach erfolgreicher Abnahme Release-Kanal wechseln und alte app-spezifische GitHub Actions kontrolliert stilllegen

**Nicht ändern:** Paket-ID `com.cobrachris.sanftwecker`, bestehendes Android-Signaturzertifikat, lokale Nutzerdaten und aktuelle Release-Adresse. Keine automatische Installation, keine Deinstallation, keine Secret-Werte im Git-Repo.

Quelle und Abnahme: https://github.com/CobraChris/app-fabrik-template/issues/25
