# Sanftwecker: Dauerhaft APK-Downloads ohne Browser

**Stand: 10.11.2026 – Feature entwickelt, signierte Veröffentlichung und Samsung-/Watch-Gerätetest noch nicht erfolgt.**

## Motivation und reproduzierbarer Fehler

Beim Herunterladen von `sanftwecker-watch-v25.apk` über einen Android-Browser bleibt die Oberfläche mehrfach bei „Wird heruntergeladen …“ mit **2,17 MB von 2,17 MB** hängen; teils ist die APK danach nicht als fertige Datei verfügbar. Ähnliche 100%-Finalisierungsfehler auf Android 16 sind in einem öffentlichen Browser-Issue dokumentiert. Ursache auf dem konkreten Handy ist nicht abschließend nachgewiesen. Browserwechsel allein ist keine verlässliche dauerhafte Lösung.

## Neue Bedienung

**Wecker → App aktualisieren → „Galaxy Watch: APK ohne Browser laden“**

1. „Passende Watch-Version suchen“ holt auf ausdrücklichen Knopfdruck die offizielle GitHub-Release-Metadatenliste (reiner HTTP-GET). Es wird ausschließlich die zum **installierten Handy-Build** gehörende, veröffentlichte APK aus `CobraChris/sanftwecker` angeboten. Fehlende passende APK führt zu einer klaren Meldung, nicht zu unpassendem Download.
2. „Watch-APK direkt herunterladen“ verwendet den **internen APK-Downloader** des Sanftweckers statt die GitHub-Webseite oder den Browser. Download erfolgt auf einen eigenen privaten Cache unter `watch_updates`, mit bis zu drei Versuchen, Größenlimit und Timeouts.
3. Sicherheit: exakt definierter Release-Tag und Watch-APK-Dateiname; nur GitHub-HTTPS und dessen verifizierte Downloadredirects, SHA-256 aus GitHubs Release-Asset-Metadaten, Android-Paketkennung `com.cobrachris.sanftwecker`, Versionscode und **Signaturgleichheit mit der bereits installierten Handy-App** werden überprüft. Ein fehlgeschlagener oder unvollständiger Download wird nicht als fertig angezeigt.
4. Nach erfolgreichem Prüfen kann die APK über **„Geprüfte APK in Downloads speichern“** mit dem normalen Android-Dateidialog gespeichert oder über **„APK an Watch-Installations-App weitergeben“** als schreibgeschützte Datei freigegeben werden. Der Dateiname wird durch den Versionscode bestimmt; keine unkontrollierten Dateipfade.
5. **Keine automatische Installation** auf der Uhr, kein ADB-Hintergrundzugriff, kein Root, kein neues Token/Key und kein kostenpflichtiger Service. Der Nutzer installiert die bereits bestehende Wear-OS-App mit einem vom Nutzer bestätigten Installationsweg. Auf Wear OS erfolgt das normalerweise über eine externe Watch-Installations-App bzw. drahtloses Debugging; einfache APK-Dateiübertragung auf die Uhr ist nicht automatisch eine Installation.
6. Der bestehende Handy-Updater bleibt unverändert: Er lädt die Handy-APK ebenfalls bereits ohne Browser mit Hash-/Signaturprüfung und öffnet den offiziellen Android-Installationsdialog nur mit Nutzerbestätigung.

## Version und Freigabegrenzen

Die neue Oberfläche ist **noch nicht in der signierten v25-Handyversion**, sondern erst im nachfolgenden Entwicklungsstand. Zunächst helfen daher ein anderer Browser oder ein direkter Download über bereits installiertes Termux; die browserfreie Watch-Funktion kommt erst mit einer künftig ausdrücklich signiert freigegebenen Handy-Beta.

Die bereits signierten Releases **Handy v25 und Watch v25** bleiben unverändert. Keine Änderung an `.github/workflows`, Signierschlüsseln oder bereits installierten Apps. Weitere Veröffentlichung nur mit gesonderter ausdrücklicher Eigentümerfreigabe.

## Tests

Neue JUnit-Tests prüfen die exakte Bindung an Release-Tag, Watch-Dateiname, Repo, SHA-256, die Übereinstimmung von Handy- und Watch-Build sowie die Ablehnung ungültiger/umgeleiteter/fremder bzw. Draft-Assets. Die Android-CI überprüft außerdem die vollständige Kompilierung, Lint und unveränderte Watch-Module. Geräteabnahme muss den tatsächlichen End-to-End-Download und den Export in Downloads testen.
