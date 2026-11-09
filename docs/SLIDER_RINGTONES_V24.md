# Sanftwecker v24 – bündiger Stopp-Slider und Klingeltonauswahl

**Stand: 09.11.2026: Entwicklungsstand.** Noch keine signierte v24-APK, keine Installation auf dem Samsung und kein Eingriff in die signierte, funktionierende v23. Die native Galaxy-Watch-v23-App bleibt unverändert.

## Umsetzung

- **Stopp-Schloss:** In den vorhandenen AlarmActivity-SeekBar eingebettet. Minzfarbene Schlossfläche Ø 62 dp, 12 dp Abstand links/rechts zur 82 dp hohen ovalen Schaltfläche, optimierte Textfreifläche. Der Schwellwert von 90 % zum Stoppen, die Sicherheitsabfrage beim Loslassen, haptisches Feedback und der barrierefreie direkte Stopp bleiben erhalten. Kein neuer Alarm-Stopp-Pfad.
- **Klingeltonauswahl:** Eigenes TonePickerActivity-Fenster in der türkisfarbenen Designfamilie, neun Optionen mit Beschreibung, Auswahlmarkierung, lokal erzeugter Hörprobe und dem Button „Übernehmen“. Die Auswahl wird zunächst nur in der Detailansicht übernommen und erst nach dem dortigen Speichern dauerhaft gespeichert. Abbrechen lässt den Wecker unverändert.
- **9 unterschiedliche lokal synthetisierte Klingeltöne:** 0 Sanftes Glockenspiel, 1 Reiner weicher Ton, 2 Tiefe Harmonie, 3 Klassische Glocke, 4 Sonnenaufgang, 5 Digital, 6 Waldmorgen, 7 Sanftes Piano, 8 Turbo Alarm. **Die drei ursprünglichen Klangprofile 0–2 bleiben mathematisch unverändert** und behalten ihre IDs in gespeicherten Alarmprofilen.
- **Hörproben-Sicherheit:** Keine Speicherung und kein Rescheduling; maximal 20 Sekunden; direkt über die Tonwahl stoppbar. Der RingingService lehnt Hörproben ab, wenn gerade ein echter Alarm klingelt, und die Stopp-Aktion für Hörproben darf einen echten Alarm nicht stoppen. Bei realem Alarm übernimmt die bestehende Tonrampen- und DND-Behandlung. Die neue Klangfarbe ändert nicht die gespeicherte Lautstärke oder Weckzeit.
- **Versionierung:** Handy versionCode 24 / versionName 0.9.5-beta; Galaxy Watch verbleibt bei versionCode 23, weil nur die Handyoberfläche und Audio-Synthese geändert werden. Gleiche Paket-ID und Originalsignatur. Für ein künftiges Update muss die v24-Handy-APK mit dem bisherigen Signierschlüssel signiert und über v23 installiert werden. Keine neue Uhr-App-Installation vorgesehen.

## Verifikation und Freigabe

- GitHub CI baut und lintet beide Module sowie die bestehende Zertifikatslinie; JUnit prüft die alten Töne mathematisch kompatibel, alle neun Klangprofile (begrenzter Pegel und Unterscheidbarkeit), Profilgrenzen und Slider-Mindestabstand.
- **Nicht durch CI ersetzt:** der abschließende Gerätetest des Schlösschens, der Tonwahl und der Hörproben ohne Störung des Weckers. Für wichtige Alarme bis zum erfolgreichen Gerätetest einen Samsung-Backupwecker nutzen.
- **Keine automatische Signierung, Veröffentlichung oder Installation.** Ein signierter v24-Release braucht eine gesonderte ausdrückliche Nutzerfreigabe und eine neu überprüfte Signierpipeline. Die alte signierte v23 wird nicht überschrieben.
