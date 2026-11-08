# Sanftwecker – geprüfte Release-Brücke 19 → 20 → 21

**Stand: 08.11.2026:** Die ursprüngliche Sanftwecker-Signatur ist im neuen Repository hinterlegt. **Signierte v20 ist im alten Repository veröffentlicht**, das nächste Release v21 ist **nicht veröffentlicht und bleibt bis nach dem Gerätetest gesperrt**.

**V20-Releasenachweis:** [Signierte v20 APK](https://github.com/CobraChris/app-fabrik-template/releases/download/sanftwecker-v20/sanftwecker-release.apk) · [Release](https://github.com/CobraChris/app-fabrik-template/releases/tag/sanftwecker-v20) · [Signing-Actions-Lauf #42479300564](https://github.com/CobraChris/app-fabrik-template/actions/runs/42479300564) SUCCESS. Die veröffentlichten Asset-Metadaten weisen `versionCode 20`, Größe **217677 Bytes** und SHA-256 **`0c4e56c55b04d720768f60c3c8b35cad212212df470bdfdeb265715b747909eb`** aus. Der Signierjob verglich das Zertifikat mit dem vorhandenen v19-Release und bestätigte Gleichheit.

**Wichtig:** Release- und Zertifikatsprüfung ersetzen keinen echten In-place-Gerätetest. Die bestehende v19 soll nicht deinstalliert werden; Installation und Abnahme von v20 erfolgen ausschließlich durch den Nutzer. Der Nutzer hat die v20-Veröffentlichung freigegeben, **aber v21 noch nicht**.

## Aktueller Plan

1. **V20-Geräteabnahme:** Über `Sanftwecker → App aktualisieren → Nach Update suchen` v20 installieren. Falls die Updatefunktion nicht funktioniert: APK direkt über den oben verlinkten Release laden. Android muss eine *Aktualisierung* der bestehenden App ermöglichen. Niemals zur Deinstallation auffordern. [Prüfprotokoll](ANDROID_V20_ACCEPTANCE.md) für Samsung und Galaxy Watch verwenden.
2. **Automatische Signatur-Nachprüfung ohne Geheimnisse:** Die normale Android-CI lädt in jedem PR die signierten Alt-APKs **v18, v19 und v20** aus dem alten Repo. Sie überprüft für jede APK die festgehaltene SHA-256-Prüfsumme und die Paket-ID / den Versionscode, vergleicht alle drei Zertifikatsfingerprints und baut die lokale neue APK weiter **unsigniert**. Keine APK-Veröffentlichung.
3. **V21 später:** [Draft-PR #5](https://github.com/CobraChris/sanftwecker/pull/5) enthält nur die Release-Auftragsdatei; derzeit bewusst nicht gemergt. Der v21-Workflow prüft vor jedem Signing-Key-Zugriff die feste v19- und die feste v20-Prüfsumme, Paketname, Versionen und identisches Zertifikat. Erst mit einer **gesonderten ausdrücklichen Freigabe** darf der signierte v21-Release ausgelöst werden.
4. **V21-Geräteabnahme:** Erst nach erfolgreichem Samsung-/Galaxy-Watch-Test v19→v20 die neue v21 im Dual-Feed-Updater auf v20 finden und ausdrücklich installieren. Erneuter Test der Alarme, Timer, Stoppuhren, Uhr-Verbindung und Berechtigungen; erst danach über Stilllegung des alten Release-Feeds entscheiden.

## Grenzen und Sicherheitsbedingungen

- Gleiche Android-Paket-ID `com.cobrachris.sanftwecker`, gleiche Originalsignatur und strikt steigende VersionCodes. Keine neuen Schlüssel, PATs oder kostenpflichtigen Dienste.
- Ein Release-PR-Merge ist ein **produktiver Freigabeakt**, während Code-, Dokumentations- und Test-PRs nur unsignierte CI auslösen. Aktuell ist **kein** v21-Auftrag gemergt.
- Das Android-Handy und die Watch werden nicht aus der Ferne verändert. Keine Deinstallation oder Datenlöschung. Bei Installationsverweigerung Vorgang abbrechen und Diagnose festhalten.
- Die alte Updatequelle bleibt aktiv und darf nicht vor bestandener Geräteabnahme entfernt werden. Projektverlauf: [Sanftwecker-Migration #25](https://github.com/CobraChris/app-fabrik-template/issues/25).

