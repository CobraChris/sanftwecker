# Sanftwecker v38 – Navigations- und Bedienabnahme

Stand: 13.11.2026. Status: **nur Releasevorbereitung, keine Freigabedatei, keine Signierung oder Veröffentlichung**.

## Geprüfte Grundlage
- PR #65 zusammengeführt, Quellstand `02302268da63e204382c424d1d6b94996734629b`.
- Neue Bedienkorrekturen: Android-Statusleisten/Navigation/IME-Inset-Abstände; Zeitanzeige „Klingelt in X Std. Y Min.“ mit Echtzeitaktualisierung; volle Timer- und Stoppuhrkartenbreite auf Handy.
- Vorherige Alarmfunktion, Profile, Watch-Kommunikation, Stoppen/Schlummern, Signatur und App-ID unverändert.
- `versionCode 38` für Handy und Watch. `0.9.19-beta` / `0.9.19-watch`.

## Geschützter Signierpfad
Ein künftiger separater, **ausdrücklich genehmigter** Request darf lediglich
`requests/sanftwecker-v38-release.json` neu auf `main` anlegen, exakt auf die
dann geprüfte SHA des freigegebenen Basiscommits verweisen und muss ein unveränderliches
Freigabeformat erfüllen.

Die Pipeline prüft **vor Zugriff auf die Signing-Secrets** die veröffentlichten
v37-Vorgängerdateien:
- Handy SHA-256: `be5ddb9d8a3e15c64c0bb294381719608f7006f3f1f4f52d4c06d95dd5904d9d`
- Watch SHA-256: `bd72b8fb93f65ebbf4b8611045b3be108f1f2089b6b911b8b8de91e591bfeff7`

Die originale Zertifikatsidentität muss bei beiden Vorgängern identisch sein und
für beide neuen APKs erneut nachgewiesen werden. Die Jobs laufen nur im bestehenden
Environment `android-releases`, mit vorhandenen Secrets und ohne Überschreiben
vorhandener Releases. Keine automatische Installation/Deinstallation.

**Abnahme erst auf realem Handy:** Obere Tabs frei von Android-Statussymbolen,
unterer Plus-Knopf frei von Android-Navigationsleiste, langer Timer/Stoppuhrtext
nicht abgeschnitten, Countdown anhand tatsächlich geplanter Weckzeit plausibel.

Der Nutzer hat mit „N“ nur die nächste Vorbereitung angefordert, **nicht** die
Signierung oder Veröffentlichung freigegeben.
