# Sanftwecker v25 – geprüfte Vorbereitung der signierten Handy-/Watch-Beta

**10.11.2026 – ausschließlich vorbereiteter Release-Workflow. Keine v25-APK veröffentlicht. Keine Signierfreigabe erteilt.**

## Inhalt des vorgesehenen Updates

- **Handy:** `0.9.6-beta`, versionCode **25**, gleiche Paketkennung `com.cobrachris.sanftwecker`, enthält zweistufige Weckkaskade, Weltuhr und gemeinsame Watch-Schlummersteuerung.
- **Watch:** `0.9.6-watch`, versionCode **25**, identische Paketkennung, neue sanfte/starke Vibrationsphasen und Watch-Schlummeraktion.
- **Bisher veröffentlicht:** signierte Handy-v24 und Watch-v23. Beide Apps müssen durch **vorhandene Installation aktualisieren** weiterverwendet werden; auf keinen Fall deinstallieren.

## Bereits vorhandene Sicherungsmaßnahmen

1. Owner-only Push auf `main`; Workflow wird ausschließlich durch erstmaliges Hinzufügen der **einzigen** Datei `requests/sanftwecker-v25-release.json` aktiviert. Ein bloßer PR oder Merge von Workflow-/Testdateien kann **keinen** v25-Release auslösen.
2. Der spätere Freigabeauftrag muss `intent=signed-native-wear-v25`, `expected_version_code=25`, exakte Paketkennung der Watch-App, eine eindeutige `request_id`, die spezielle Bestätigung `SIGN_PHONE_AND_WATCH_V25_WITH_EXISTING_V24_V23_CERTIFICATE` und den zuvor geprüften **40-stelligen Quell-Commit-SHA** enthalten. `REQUEST_BEFORE` muss genau diesem SHA entsprechen. Geänderte Code-/Workflow-Dateien im selben Push führen zur Ablehnung.
3. Es werden Android-/Watch-Unit-Tests und Lint ausgeführt; anschließend werden **beide** unsignierten Release-APKs gebaut, die Paketkennung, versionCode 25, einzelne APKs und das Wear-Hardware-Merkmal überprüft. Build-Artefakte bleiben einen Tag gespeichert.
4. **Vor Zugriff auf Signier-Secrets** lädt der geschützte Signierjob die signierte Handy-APK v24 und die Watch-APK v23. Die SHA-256-Digests werden fest überprüft:
   - Handy v24: `4090a245318b54c39ab188e8a30b2aba412891b36360b55bee16441ce246fff9`
   - Watch v23: `311cc657239c7a81b2d8fbe7ddc8ed923be3de46192b80e5ae3bf1ef7ecec5cf`
   - Beide Vorgänger müssen nachweislich dieselbe Paketkennung und **dasselbe** Zertifikat besitzen. Ihre versionCodes sind jeweils 24 beziehungsweise 23.
5. Erst dann wird im GitHub-Environment `android-releases` der schon bestehende Originalschlüssel verwendet. Die neue Handy- und Watch-APK werden **beide** signiert; anschließend werden ihre Fingerprints untereinander und gegen **beide Vorgänger** überprüft. Bei jeder Abweichung scheitert die Veröffentlichung.
6. Der Workflow erzeugt nur einmalig den Prerelease-Tag `sanftwecker-v25` mit `sanftwecker-release.apk`, `sanftwecker-watch-v25.apk`, `SHA256SUMS.txt` und `RELEASE_INFO.txt`. Bestehende Releases werden nicht überschrieben.
7. **Keine automatische Installation**, kein neuer Signierschlüssel, keine neue GitHub-App, keine Tokens und keine zusätzlichen Dienste. Der Nutzer installiert später selbst und testet beide Geräte (Kaskade, Watch-Schlummern bei Verbindungsausfall, Weltuhr, DND).

## Noch zu entscheiden und testen

Die eigentliche v25-Signierung/Veröffentlichung setzt eine **neue, gesonderte ausdrückliche Freigabe durch den Nutzer** voraus, nachdem Pipeline-PR und CI erfolgreich geprüft wurden. Bis dahin bleibt `requests/sanftwecker-v25-release.json` bewusst **nicht vorhanden**. Die Original-Releases v24/v23 werden nicht verändert. Automatisierte CI ersetzt **keinen** Hardwaretest auf der Galaxy Watch.

Bei späterer Freigabe muss die neue Request-Datei in einem eigenen kontrollierten PR mit Source-SHA und signiertem Releaseauftrag vorgelegt, separat geprüft und erst danach gemergt werden.
