# Sanftwecker-Migration – sichere Etappen

Stand: 08.11.2026. Ziel ist die Trennung von `CobraChris/app-fabrik-template` in `CobraChris/sanftwecker` bei kompatiblen Updates der bereits installierten Android-App.

- [x] Automatisiertes öffentliches Repository mit README erstellt, GitHub Actions #42384045324 erfolgreich
- [x] Quellcode und Tests in isolierten Migrationszweig kopiert (kein Cutover)
- [x] GitHub-CI, Unit-Tests, Lint und unsignierten Build des kopierten Projekts prüfen – erfolgreich vor und nach Merge, letzter Hauptzweig-Run #42384995807
- [x] **Signierte Alt-Releases v18/v19 abgeglichen:** SHA-256 und identische Zertifikatsfingerprints sind per [unsigniertem CI-Lauf #42426886256](https://github.com/CobraChris/sanftwecker/actions/runs/42426886256) geprüft; [Dokumentation](docs/SIGNING_MIGRATION_PREFLIGHT.md). Android-Paket-ID bleibt `com.cobrachris.sanftwecker`.
- [x] **v20-Updater-Brücke vorbereitet:** [Neues Repo PR #3](https://github.com/CobraChris/sanftwecker/pull/3) nach Android-CI [#42466587167](https://github.com/CobraChris/sanftwecker/actions/runs/42466587167) gemergt; paralleler [Alt-Repo PR #33](https://github.com/CobraChris/app-fabrik-template/pull/33) in den bestehenden Sanftwecker-Entwicklungszweig gemergt. Die vier Dateien `UpdateActivity.java`, `UpdateUrlPolicy.java`, Tests und `alarm/build.gradle` sind per Git-Blob-SHA **an beiden Orten identisch**. VersionCode **20** ist nur Entwicklungsstand, keine signierte Veröffentlichung. [Gesamtablauf](docs/BRIDGE_UPDATE_V20_V21.md).
- [ ] **Freigabe signiertes v20-Brückenrelease über altes GitHub-Releases-Repo** – erst nach Abnahme des korrekten Builds, VersionCode und Zertifikat. So erhalten installierte v19-Clients die neue Dual-Feed-Updateprüfung.
- [ ] **Signiermigration genehmigt, aber aktuell blockiert:** [Controller-PR #14](https://github.com/CobraChris/github-app-fabrik-control/pull/14) und [Auftrags-PR #16](https://github.com/CobraChris/github-app-fabrik-control/pull/16) sind gemergt. [Run #42427425852](https://github.com/CobraChris/github-app-fabrik-control/actions/runs/42427425852) und ein erneuter Versuch am 08.11.2026 brachen nach erfolgreich geprüftem Alt-Zertifikat mit **HTTP 404** ab: bestehende Admin-GitHub-App noch nicht auf `CobraChris/sanftwecker` installiert. **Keine** Signing-Secrets übertragen oder neue Schlüssel erzeugt. Einmalig die bestehende App-Installation auf dieses Repository erweitern und den fehlgeschlagenen Job erneut ausführen.
- [ ] Ersten signierten neuen Build anhand des SHA-256-Zertifikatsfingerprints gegen Alt-Release verifizieren; neuer `versionCode` **größer als 19**
- [ ] Bisherige Release-URL im In-App-Updater mittels verifiziertem Brückenrelease kompatibel halten oder bewusst eingeführte sichere Dual-Repo-Updateprüfung testen
- [ ] Zwei signierte In-place-Upgrades auf Samsung prüfen, inklusive Wecker/Timer/Stoppuhr, Galaxy Watch, Bildschirmfreigabe und Sperrbildschirm-Alarm
- [ ] Erst nach erfolgreicher Abnahme Release-Kanal wechseln und alte app-spezifische GitHub Actions kontrolliert stilllegen

**Nicht ändern:** Paket-ID `com.cobrachris.sanftwecker`, bestehendes Android-Signaturzertifikat, lokale Nutzerdaten und aktuelle Release-Adresse. Keine automatische Installation, keine Deinstallation, keine Secret-Werte im Git-Repo.

Quelle und Abnahme: https://github.com/CobraChris/app-fabrik-template/issues/25
