# Sanftwecker v26: Browserfreie Updates – geschützte Auslieferung

**Stand: 10.11.2026. Nur Entwicklung und Releasevorbereitung, noch kein signierter v26-Release und keine automatische Installation.**

## Neue Funktion

Der Android-16-Browser bleibt teilweise bei 100 % hängen und veröffentlicht die Watch-APK nicht zuverlässig. Diese nächste Sanftwecker-Version löst den Dateidownload innerhalb der Handy-App. Benutzeroberfläche: **Wecker → App aktualisieren → Galaxy Watch: APK ohne Browser laden**.

- Der aktuelle v25-Handy-Sanftwecker hat bereits einen eigenen, SHA-256- und zertifikatsgesicherten **Handy-Updater**. Dadurch kann der Nutzer später eine signierte v26 **ohne GitHub-Webbrowser über die bestehende App** installieren.
- Die künftige **Handy-v26** enthält den neuen **Watch-Downloader**, der die signierte **Watch-v26** direkt aus der offiziellen GitHub-Releasequelle lädt, dreimal versucht, die Vollständigkeit, SHA-256 und Originalsignatur prüft und sie anschließend zur Auswahl per Android-Dateidialog in Downloads speichert oder per Share an eine Watch-Installations-App weitergibt.
- Die Watch-APK wird **nicht auf dem Handy installiert**. Der Benutzer bestätigt die separate Installation auf der Uhr. Die bereits vorhandene Watch-App bleibt mit ihrem bisherigen Paketnamen und Zertifikat erhalten.
- Watch-v26 enthält dieselben bisherigen Wecker-/Watch-Funktionen wie v25, aber mit erhöhtem versionCode **26** für ein reguläres In-place-Update. Der Watch-Modul-Quellcode wurde nicht geändert.
- Kein Server, kostenpflichtiger Dienst, API-Key, Root, Hintergrund-ADB oder neue Signieridentität.

## Geschützter Freigabeprozess

1. Code, Tests und `alarm/build.gradle`/`watch/build.gradle` für VersionCode **26** liegen nach positivem PR-CI im Hauptzweig. **Keine** Freigabedatei auf `main`.
2. Die neue `sanftwecker-v26-release.yml` startet nur bei einem **späteren ausdrücklich genehmigten** erstmaligen Hinzufügen von exakt `requests/sanftwecker-v26-release.json` durch Repo-Owner auf `main`. Bestätigung: `SIGN_PHONE_AND_WATCH_V26_WITH_EXISTING_V25_CERTIFICATE`, exakter freigegebener vorheriger Hauptzweig-Quell-SHA.
3. Der Job führt Tests, Lint, unsignierte Handy- und Wear-APKs aus und prüft Paket-ID `com.cobrachris.sanftwecker`, VersionCode **26** und das Wear-Hardwaremerkmal.
4. Vor Zugriff auf die bestehenden Signing-Secrets werden beide signierten v25-Referenzen heruntergeladen und mit unveränderlichen SHA-256-Fingerprints validiert:
   - Handy v25 `da69da77c15406f9d4d3acd1f15891e73706d6b6e6cd2f4f7caf6817b312bff1`
   - Watch v25 `60dbadb976acfa51d1b3d500e96354f736bec031ede9c830384441d63fd91b1d`
5. Beide alten APK-Zertifikate, beide neuen APK-Zertifikate und die Package-/VersionCodes müssen übereinstimmen. Nur dann wird ein **neuer** (niemals überschreibender) Prerelease `sanftwecker-v26` mit Handy-/Watch-APK und SHA256SUMS erstellt. Der ursprüngliche Schlüssel bleibt im geschützten Environment `android-releases`; temporäre Kopie wird entfernt.

## Noch offen

Eine neue **ausdrückliche Nutzerfreigabe für signierte v26-Handy- und Watch-Beta** ist erforderlich. Vorher keine Veröffentlichung. Danach muss die in-app Download- und Dateiexportfunktion am Samsung praktisch getestet und die separate Uhrinstallation bestätigt werden.

Die bisher signierten v25-Releases sowie die Weckerdaten bleiben erhalten; alte Apps **niemals deinstallieren**.
