# Sanftwecker v25 – zweistufige Weckkaskade (Handy + Galaxy Watch)

**Stand: 10.11.2026: Nur Quellcode und automatisierte Tests. Noch kein signierter v25-Release und kein Gerätetest.**

## Gewünschtes Verhalten

Der Nutzer hat nach dem v24-Praxistest eine optional aktivierbare Kaskade verlangt:

| Alarmzeit | Handy-Ton | Galaxy Watch | Handyvibration |
| --- | --- | --- | --- |
| 0–60 Sekunden | Beginnt leise, steigt kontinuierlich in 60 Sekunden auf die vom Profil festgelegte Obergrenze | Ein kurzer, schwacher Impuls ca. alle 9 Sekunden; Amplitude 48/255, wenn vom Motor unterstützt | Aus |
| Ab 60 Sekunden | Hält die gewählte Obergrenze, ohne plötzlichen Sprung oder Tonhöhenveränderung | Starkes, dichtes Vibrationsmuster mit 850-ms-Impulsen | Startet mit dem ausgewählten Muster, bei bisher deaktivierter Vibration mit „Fast durchgehend“ |

Die Phase richtet sich auf beiden Geräten nach **demselben absoluten Alarmzeitpunkt**. Beginnt der Watch-Dienst spät, setzt er direkt mit der zutreffenden Phase ein. Die Watch kann durch bereits übertragene, lokal gespeicherte Weckzeiten auch vorübergehend ohne Handy vibrieren. Die Stopp- und Schlummerbefehle behalten ihre bestehende Zuordnung zu Alarm-ID und -Zeit.

## Bedienung und Abwärtskompatibilität

- In **Wecker-Detail → Weckkaskade (2 Stufen)** befindet sich ein Schalter pro Wecker.
- Für bereits gespeicherte und neue Profile steht der Schalter **standardmäßig auf Aus**. Die bestehenden Tonstart-, Anstiegs-, Watch- und Handyvibrationswerte bleiben gespeichert und gelten unverändert bei ausgeschalteter Kaskade.
- Bei aktiver Kaskade ersetzt die 60-Sekunden-Rampe nur das Verhalten während dieses Weckens; der gespeicherte Tonstart und die bisherige Rampendauer gehen nicht verloren.
- Der übliche 30-Sekunden-Test bleibt für normale Wecker erhalten. Bei eingeschalteter Kaskade wird daraus ein **150-Sekunden-Test**, sodass beide Phasen tatsächlich beobachtet werden können. Die Ton-Auswahl-Hörprobe bleibt bei 20 Sekunden ohne Vibration.
- Die alte Watch-Benachrichtigung wird für aktivierte Kaskaden **nicht gespiegelt**, weil sie sonst die sanfte erste Phase mit der alten starken Vibration überlagern würde. Die native Wear-OS-App v25 ist Voraussetzung.
- Das Watch-Protokoll ist rückwärtskompatibel: alte 3-teilige Startnachrichten erhalten das frühere Muster, neue Kaskaden-Nachrichten tragen ausschließlich das zusätzliche sichere Kennzeichen `:1`. Offline-Daten und Startzustand speichern die Kaskadenoption.
- Die vorhandenen Vibrations-Sicherheitsgrenzen bleiben erhalten (Telefon: spätestens nach 90 Sekunden ab Beginn der Handyvibration, Watch: bisherige max. 15 Minuten, Stopp sofort). Ein Kaskadenalarm wird mindestens zwei Minuten ausgeführt, damit die zweite Stufe tatsächlich beginnt.

## Sicherheits- und Verifikationsgrenzen

- **Beide Anwendungen müssen aktualisiert werden:** Handy und Watch v25, gleiche Paketkennung `com.cobrachris.sanftwecker` und bei einem späteren Release zwingend das **bestehende Originalsignierzertifikat**. **Keine** neue Signatur, Deinstallation, neue App, PATs oder kostenpflichtige Dienste.
- GitHub Actions prüft Unit-Tests/Android-Lint/unsignierte Builds beider Apps. Tests decken die 60-Sekunden-Grenze, beide Phasen, Profil-Abwärtskompatibilität, Dauergrenzen und Watch-Nachrichten ab.
- **Noch offen:** echte Watch-Motorintensität, Samsung-DND und synchroner Start unter Praxisbedingungen. Wear-Hardware ohne Amplitudensteuerung kann lediglich kürzere/seltenere Impulse liefern, nicht garantiert eine geringere physische Amplitude.
- Ein signierter v25-Handy-/Watch-Release erfordert **separate ausdrückliche Freigabe** und eine geprüfte Releasepipeline mit dem ursprünglichen Zertifikat. Keine automatische Installation. Der signierte v24-Handy- und v23-Watch-Stand bleiben bis dahin unverändert.
- Zur ersten Geräteabnahme sollte parallel ein verlässlicher Standardwecker als Backup aktiv bleiben.
