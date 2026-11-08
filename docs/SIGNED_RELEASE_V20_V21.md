# Sanftwecker – signierte Release-Brücke 19 → 20 → 21

Stand: 08.11.2026. **Noch keine neuen signierten APKs veröffentlicht.**

## Bereits erledigt

- Originaler Signing-Keystore über den privaten Controller kontrolliert und unverändert in das neue Repo übertragen; geschützter [Actions-Lauf #42473588752](https://github.com/CobraChris/github-app-fabrik-control/actions/runs/42473588752) war erfolgreich.
- Alter und neuer Quellstand für die v20-Brücke mit identischen Java- und Gradle-Dateien versehen. Dual-Feed-Updater und Tests laufen in beiden Codebasen.
- **Neues Repo** besitzt nun einen getrennten Workflow für genau **v21**: Die Signatur wird ausschließlich aus den übertragenen vier Original-Secrets entnommen. Keine automatische Key-Erzeugung.

## Releaseablauf und Sperren

1. **Signierte v20 im alten Repo:** Entwurfs-[Release-PR #34](https://github.com/CobraChris/app-fabrik-template/pull/34) aktiviert erst nach separater ausdrücklicher Nutzerfreigabe den bisherigen Signierworkflow. Er baut den fest vorgegebenen alten Sanftwecker-Entwicklungsstand mit VersionCode 20. Das Release erscheint in CobraChris/app-fabrik-template als sanftwecker-v20. Die bereits installierte v19 sieht den alten Release-Feed und entdeckt v20. Keine Deinstallation.
2. **Signierte v21 im neuen Repo:** Der neue Release-Workflow ist nur auf eine **neue** einmalige Release-Auftragsdatei auf main scharf geschaltet. Vor Zugriff auf den Signing-Key muss der Workflow v19 und v20 aus dem **alten** Repo herunterladen, die feste SHA-256 der v19-APK sowie **beide** Signierzertifikate, Paket-IDs und Versionscodes prüfen. Wenn v20 fehlt, die Zertifikate verschieden sind oder v21 schon existiert, bricht der Workflow **vor jedem Secrets-Zugriff** ab.
3. Erst danach darf v21 aus unveränderlich freigegebenem Quell-SHA, VersionCode 21 und Original-Secrets signiert werden; das neue Zertifikat muss im SHA-256-Fingerprint mit dem v19/v20-Signer übereinstimmen. Nur eine sanftwecker-v21-Prerelease im **neuen** Repository wird erzeugt.
4. **Geräteabnahme:** Nutzer installiert selbst v20 über die bestehende v19-App und testet Wecker, Timer, Stoppuhr, Galaxy Watch, Alarmton bei Lautlos, Hintergrund-/Overlay-Rechte, Neustart und Sperrbildschirm. Danach v21 über den Dual-Feed-Updater und die gleichen Daten/Integrationen erneut prüfen. Alte Pipelines und Release-Adressen bleiben bis zur Abnahme erhalten.

## Sicherungsregeln

- Release-Aufträge werden nicht automatisch bei PR, Quellcode-Push oder workflow_dispatch ausgeführt. Nur **ein neu hinzugefügter Besitzerauftrag** in requests/sanftwecker-release.json und nur auf main löst v21 aus. Request-Identität, exakter alter main-SHA, Version 21, Paket-ID und Freigabekonstante sind fest.
- Keine persönlichen PATs, keine neuen Schlüssel und keine Secrets in Quelltext, PRs oder Logs. Der Build läuft **ohne** Signing-Secrets, der spätere Signierjob unter android-releases und mit minimalem GitHub-Token. Alles bleibt im GitHub-Free-Kontingent, soweit verfügbar.
- Vor jeder echten Veröffentlichung ist separate Releasefreigabe erforderlich. Das Einrichten oder Mergen des Pipeline-Codes allein löst **keinen** Signierlauf aus.
- Ein grüner CI-Lauf beweist den **unsignierten Build und die statischen Sicherheitsregeln**, noch nicht die Signatur von v21 oder ein erfolgreiches Android-In-place-Update.
- Die aktuelle installierte **v19** bleibt bis zur bewussten Update-Installation unangetastet.

## Freigabe und offene Punkte

- [ ] Code-/CI-PR für die neue v21-Release-Pipeline testen, prüfen und in main übernehmen (kein Release).
- [ ] Signierte v20-Brücke im bisherigen Repo separat freigeben und deren Release-Zertifikat prüfen.
- [ ] Erst danach einen eigenständigen einmaligen **v21-Releaseauftrag als separaten PR** auf main anlegen und ausdrücklich freigeben lassen.
- [ ] Nach Veröffentlichung v20/v21 auf dem Android-Handy und der Galaxy Watch nacheinander abnehmen.
- [ ] Erst dann alte Update-Pipeline stilllegen.

Primärer Alt-Repo-Migrationsplan: https://github.com/CobraChris/app-fabrik-template/issues/25
