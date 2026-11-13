# Geschützte v36-Veröffentlichung vorbereiten (kein Release)

Stand 13.11.2026. Auftrag: v36 für Handy und Galaxy Watch mit dem Original-
signierzertifikat signieren und nach erfolgreichen CI-Prüfungen veröffentlichen.

Die Pipeline ist nur ein **vorbereitender Code-Änderungs-PR**, ohne
`requests/sanftwecker-v36-release.json`. Eine spätere einmalige Request-Datei
darf nur nach geprüfter v36-Implementierung von der berechtigten Eigentümerin /
dem berechtigten Eigentümer auf `main` hinzugefügt werden. Der geschützte
Job darf nur bei genau einem neuen Request starten.

Offizielle v35-Prüfsummen vor Secret-Zugriff:
- Phone: `4cc60aec98347536839cb9b3663158ac6e55010c55753d5d5c8057a04d6af5e8`
- Watch: `022eb593e0e0203d6e5aa9ee37813351e8bbc5b7528c7f6b63eeb77b8579b125`

Sicherheit: SHA-256 der v35-Artefakte, gleiche originale Zertifikats-SHA-256
beider Geräte, bestehende Paketkennung `com.cobrachris.sanftwecker`,
v35-VersionCode, neue v36-VersionCodes und Watch-Gerätekennung.
Kein neuer Schlüssel, keine Änderungen an existierenden Releases, keine
Kosten, keine automatische Installation/Deinstallation. v36 bleibt Beta bis
zur erfolgreichen Hardwareabnahme der Alarmansichten und Vibrationsprofile.
