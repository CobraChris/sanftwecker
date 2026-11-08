# Sanftwecker – Ein-Schritt-Umstiegsplan v19 → v20 → v21

**Status: Entwicklungsstand, noch nicht veröffentlicht.** Derzeit ist `sanftwecker-v19` (VersionCode 19) im alten Release-Feed die letzte signierte Ausgabe. Es wurde nichts auf dem Android-Handy installiert oder geändert.

## Warum eine Übergangsversion nötig ist

Der v19-Updater fragt ausschließlich `CobraChris/app-fabrik-template` ab. Ein Release nur im neuen `CobraChris/sanftwecker` würde von vorhandenen v19-Installationen **nicht entdeckt**.

## Geplante große Release-Etappe

1. **Quellcode und Tests:** Beide Standorte erhalten die identische, sicher begrenzte Updateprüfung mit zwei festen GitHub-Feeds, deren Assets gegen ihren jeweiligen Repository-Namen und den Release-Tag validiert werden. Bei gleicher Versionsnummer hat der alte Feed Vorrang. Fehlt ein Feed kurzfristig, kann der andere weiterhin eine gültige Version liefern. Es werden keine Installationen im Hintergrund vorgenommen.
2. **Altes Repository, Build 20:** Nach expliziter Releasefreigabe wird aus dem geprüften alten Quellzweig mit dem bestehenden Signaturschlüssel eine v20-Brücke veröffentlicht. Diese Version bleibt über die alte Update-URL erreichbar und bietet künftig duale Updateprüfung.
3. **Neues Repository, Build 21:** Erst wenn der originale Keystore **identisch** übernommen wurde und CI, Zertifikatsfingerprint, App-ID und Upgrade-Pfad nachweislich stimmen, wird v21 im neuen Release-Feed signiert veröffentlicht. Die neue v20-Updaterbrücke soll v21 entdecken.
4. **Samsung + Galaxy Watch:** In-place-Upgrade **19 → 20 → 21** mit erhaltenen Weckern, Timern, Stoppuhrdaten und Berechtigungen sowie Alarm- und Watch-Funktion bestätigen. Keine Deinstallation. Anschließend alte Release-Pipeline kontrolliert stilllegen.

**Keine automatische Veröffentlichung:** Änderungen an Quellcode/CI/Tests in diesem Pull Request lösen ausschließlich *unsignierte* Builds aus. VersionCode 20 ist nur ein Entwicklungsziel, bis ein gesondert genehmigter signierter Releaseauftrag existiert.

## Sicherheit

- Nur `https://github.com/CobraChris/{app-fabrik-template|sanftwecker}/releases/download/sanftwecker-v<code>/sanftwecker-release.apk` sind als Downloadadressen zulässig. Kein frei konfigurierbarer Host, Redirect, Debug-APK oder tagfremdes Asset.
- Die GitHub-API wird ausschließlich bei Nutzerklick gelesen. Nur freigegebene, vollen GitHub-Release-Adressen entsprechende Assets werden vorgeschlagen. Die tatsächliche Installation bleibt Androids Systemdialog.
- Android erzwingt die alte Paket-ID `com.cobrachris.sanftwecker`, denselben Signatur-Key und steigende VersionCodes. Der Bridge-Workflow muss vor Release den Zertifikats-SHA-256-Fingerprint gegen die zuletzt signierte v19-APK vergleichen.
- Kein GitHub-Token oder Signiermaterial in Dateien, Logs oder Apps; keine Deinstallation/Reset von App-Daten. Unsignierte CI und Signatur-Rehearsal sind keine Samsung-Funktionsabnahme.
- Im aktuellen GitHub-Free-Modell ist `main` nicht zwangsweise durch Reviews/CI gesperrt. Deshalb signierte Releases ausschließlich nach bewusster Freigabe des Nutzers.

Blocker ist weiterhin die fehlende GitHub-App-Installationsfreigabe für `CobraChris/sanftwecker`: Controller-Run [#42427425852](https://github.com/CobraChris/github-app-fabrik-control/actions/runs/42427425852).

Fortschritt und Freigaben werden in [Issue #25](https://github.com/CobraChris/app-fabrik-template/issues/25) dokumentiert.
