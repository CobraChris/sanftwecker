# Sanftwecker v34 – Watch-Selbstupdate: Abschluss und Bedienoberfläche

Stand: 12.11.2026. **Nur Quellcode-Vorbereitung; kein signiertes Release.**

## Hardwarebefund aus v32 → v33
- Die Uhr hat das GitHub-Release selbst gefunden, die Watch-APK vollständig
  geladen und SHA-256, Paketkennung, Version und Originalsignatur geprüft.
- Das erste Android-Fenster „Unbekannte Apps installieren?“ erteilte lediglich
  eine Freigabe. Nach mehrmaligem Versuch erschien das tatsächliche
  Installationsfenster; der Nutzer meldete erfolgreiche Installation.
- Es fehlten ein automatischer Rücksprung zur Installation nach erteilter
  Berechtigung und ein eindeutiger Erfolgsstatus nach dem App-Ersatz.

## V34: bewusst additive Korrektur
1. Nach **manueller** Freigabe unbekannter Quellen genau einmal in
   \`onResume\` zur bereits geprüften APK zurückkehren, ohne die
   Installationsbestätigung zu umgehen. Wenn URI/Zustand verlorengeht,
   zum sicheren erneuten Download auffordern.
2. Tatsächlich installierte Version und \`versionCode\` direkt in der
   Watch-Updateansicht anzeigen (PackageManager, niemals Release-Metadaten
   als installierte Version ausgeben).
3. Vor Start der Systeminstallation Zielversion speichern. Ein separater,
   nicht exportierter Receiver nimmt \`MY_PACKAGE_REPLACED\` entgegen und
   protokolliert die installierte Version. Beim nächsten Öffnen erscheint
   **„Update erfolgreich installiert: …“** nur wenn die installierte
   PackageManager-Version zur Empfangs-/Zielversion passt.
4. Breite und Typografie für runde Displays begrenzen; vertikales Scrollen
   sowie Schaltflächen zum Update suchen, Laden und Installieren behalten.

## Strikte Regression
- Bestehenden Android-Installer-Weg \`ACTION_VIEW\` als erste Wahl
  beibehalten, \`ACTION_INSTALL_PACKAGE\` nur als Fallback.
- Nutzer muss Android-Installation ausdrücklich bestätigen.
- Keine Änderungen an signiertem Downloadclient, SHA-256, URL-/Zertifikats-
  prüfung, Updatefeed, AlarmManager, Weckerkaskade, Snooze/Stop,
  Offlinealarm, Telefon-Funktionen oder Android-Berechtigungen.
- \`versionCode 34\` auf Handy und Uhr, dieselbe Paketkennung.
- Nur unsignierte CI. Eine spätere v34-Originalsignierung/Veröffentlichung
  verlangt **gesonderte explizite Freigabe**. Kein v34-Releaseauftrag
  in dieser Vorbereitung.

## Netzwerk
- Handy und Uhr müssen **nicht im selben WLAN** sein.
- Die Uhr benötigt Internet für GitHub-Release-Prüfung/Download; das ist
  direkt per WLAN/LTE oder oft über den Bluetooth-Internet-Proxy des
  gekoppelten Handys möglich.
- Weder ADB-Debugging noch GeminiMan erforderlich.
- WLAN ist für ca. 5,8 MB APK empfehlenswert, aber nicht zwingend.
