# Sanftwecker v27 – Weltuhr mit weltweiter Ortssuche

**Implementierungsstand: PR, unsignierte Prüfung; Hardwaretest und signierte v27-Freigabe separat.**

## Warum?

Bisher kann der Weltuhr-Dialog nur die bevorzugte Städteliste und IANA-Regionsnamen finden.
Normale Orte wie Enfield, Connecticut, sind **keine** IANA-Zeitzone und erscheinen deshalb
nicht, obwohl die richtige Zeitzone `America/New_York` vorhanden ist.

## Bedienung

**Weltuhr → Stadt hinzufügen → „Enfield CT“ → Weltweit suchen (Internet)**.
Auch `Enfield, Connecticut` und andere internationale Ortsnamen werden unterstützt.
Die öffentliche Open-Meteo-Geocoding-API liefert landesweite GeoNames-Orte zusammen
mit ihrer IANA-Zeitzone. Der Nutzer wählt den richtigen gleichnamigen Ort anhand
Region und Land aus. Die bestehende lokale Suche bleibt unverändert verfügbar.

- Tippfehler, fehlende Verbindung oder keine Treffer werden im Dialog angezeigt.
- Die App sendet **nur beim Tippen auf die Schaltfläche** den Suchbegriff über HTTPS.
  Kein Key, kein Konto, kein Hintergrundabruf; keine neue Android-Berechtigung.
- US-Bundesstaatskürzel wie `CT` werden als `Enfield, CT` an die dokumentierte
  Suchschnittstelle übergeben.
- Die Ergebnisauswahl speichert **GeoNames-ID + Ortsname + Region/Land + IANA-Zone**
  lokal gemeinsam mit den bestehenden Weltuhr-IDs in SharedPreferences.
- Nach dem Hinzufügen funktionieren Uhrzeit, Sommerzeit, Datum und Sortierung **offline**.
- Bestehende Weltuhr-Einträge bleiben gültig, einschließlich mehrerer Städte derselben Zone.
  Datenhaltung für Wecker, Timer, Stoppuhr und Wear OS bleibt unverändert.
- Der Nutzer kann bis zu 24 Orte behalten. Der bisherige lokale IANA-Katalog bleibt
  als Offline-Alternative verfügbar.
- Suchergebnisse sind von der Verfügbarkeit und Abdeckung des Ortsdiensts abhängig;
  eine Garantie für jeden Weiler der Welt ist nicht gegeben.

## Anbieter und Lizenz

Ortsdaten: [Open-Meteo Geocoding](https://open-meteo.com/en/docs/geocoding-api),
basierend auf GeoNames. Daten unter CC BY 4.0; Nutzung der kostenlosen API ist
nur für nicht-kommerzielle Apps erlaubt. In der App gibt es beim Online-Dialog
und auf hinzugefügten Ortskarten einen sichtbaren Anbieterlink.
Keine kostenpflichtigen Dienste / Tokens. Bei späterer kommerzieller Nutzung
muss die Datenversorgung getrennt neu bewertet werden.

## Teststrategie

Unit-Tests prüfen:
- `Enfield CT` → `Enfield, CT` als Serverqualifier
- Gleichnamige Orte in Connecticut und London mit korrekter Zeitzone
- Ungültige Zeitzonen, beschädigte Antworten, doppelte IDs
- Register & Wiederauffindbarkeit von Orts-ID und UTC-Zone
- Bestehende Weltuhr-Auswahl und -Limit bleiben unverändert

CI testet Phone/Watch und baut **nur unsignierte** v27-Pakete.
Vor dem späteren v27-Release sind Prüfungen von Internet/Offline,
Ortsergebnissen, gespeicherten Orten nach Neustart sowie Updates auf
den bereits installierten v26-Signer-Geräten notwendig.

**Version:** Android-Handy 27 (`0.9.8-beta`), Watch 27 (`0.9.8-watch`).
**Keine Signierung, kein Release, keine Installation** ohne separate ausdrückliche
Freigabe des Nutzers und Prüfung der ursprünglichen Signatur.
