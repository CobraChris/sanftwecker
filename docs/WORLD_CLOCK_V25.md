# Sanftwecker v25 – Weltuhr als vierter Reiter

**Stand 10.11.2026 – Implementiert im Entwicklungszweig, noch kein signierter v25-Release und kein Endtest auf dem Samsung.**

## Funktion

- Die bisherige native Navigation **Wecker · Timer · Stoppuhr** erhält den vierten Reiter **Weltuhr**. Alle vorhandenen Reiter behalten ihre bisherigen Funktionen.
- Die Weltuhr zeigt ausgewählte Orte als übersichtliche Karten mit **Ortsname, Land, aktueller Uhrzeit in 24-Stunden-Schreibweise, Datum, Heute/Gestern/Morgen und UTC-Abweichung**.
- Die Zeit wird bei geöffneter Weltuhr automatisch zur nächsten Minute aktualisiert; beim Zurückkehren in die App sofort neu berechnet. Ein Hintergrunddienst ist dafür nicht nötig.
- Startbeispiele: **Hamburg, Tokio, London, New York**. Nutzer kann alle entfernen, beliebige weitere hinzufügen und die Reihenfolge über Auf-/Ab-Pfeile verändern.
- **Städteauswahl per Suche:** zuerst bekannte weltweite Städte (deutsche Namen), danach alle weiteren benannten IANA-Regionszeitzonen aus der lokalen Android-Datenbank. Suchen nach Stadtnamen, Land, englischer IANA-Schreibweise (z. B. „Tokyo“ oder „America/New_York“) möglich. Auch verschiedene Städte mit identischer Zeitzone (z. B. Hamburg und Berlin) können nebeneinander angezeigt werden.
- Maximal **24 Städte**; Auswahl und Sortierung werden lokal als geordnete JSON-Liste in SharedPreferences gespeichert. Fehlerhafte, doppelte oder inzwischen ungültige Einträge werden verworfen; bestehende Wecker-/Timerdaten bleiben unangetastet.
- **Offline ohne Netz, Google-API-Key oder Gebühren:** Berechnung per `java.time.ZoneId` / `ZonedDateTime` und den System-Zeitzonenregeln von Android. Dadurch werden Sommerzeit und halbstündige/viertelstündige Offsets automatisch berücksichtigt. Zeitzonenregeln können mit Android-Systemupdates aktualisiert werden.

## Test und Sicherheit

- JUnit-Regressionstests für Auswahl, Mehrfachorte derselben Zeitzone, Suche ohne und mit Akzenten, 24-Städte-Limit, Reihenfolge, ungültige IDs, lokale Datumssprünge, Berlin-Sommerzeitwechsel, New-York-Winterzeitwechsel, Kathmandu (UTC+05:45), Tokio/Los Angeles.
- Keine neuen Android-Berechtigungen, Netzwerkanfragen, kostenpflichtigen Dienste, Alarme, Benachrichtigungen oder Hintergrundarbeiten.
- Ziel ist der **bestehende v25-Entwicklungsstand**; Watch-Modul, Signier-Secrets und Releasepipeline bleiben unverändert. Ein signierter v25-Release (insbesondere bei gemeinsamem Handy-/Watch-Update) erfordert weiterhin eine **separate ausdrückliche Nutzerfreigabe** und die ursprüngliche Signatur; keine automatische Installation.
