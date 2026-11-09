# Sanftwecker v23 – eigenständige Uhralarme statt Benachrichtigungs-Spiegelung

**Stand: 09.11.2026. Entwicklungsstand, noch kein signierter v23-Release und kein Geräte-Endtest.**

## Ausgangslage und Annahme

Die auf dem Samsung installierte, signierte v21 klingelt und steigert den Alarmton auch bei aktiviertem DND zuverlässig. Die Galaxy Watch erhält ihre Impulse über Samsung Notification-Mirroring; mit aktiviertem Watch-DND werden solche Benachrichtigungen unter Umständen unterdrückt. Der Nutzer wünscht **keine weiteren Einstellungs- oder Zwischentests**: Die wahrscheinlichste Ursache wird direkt durch eine unabhängig von Mirroring funktionierende Uhr-App behoben. Die finale Prüfung erfolgt erst an der fertigen signierten Handy-/Uhr-Kombination.

## Native Architektur

1. **Handy (v23)**: Die bisherigen Wecker, gespeicherten Daten, Timer, Stoppuhr, Tonanstieg und der sichere Update-Installer werden erhalten. Bei Planen/Ändern/Löschen von normalen Weckern, Testweckern und Schlummerterminen legt der Android Wear Data Layer per \`DataClient\` eine dringliche, persistente Uhr-Terminmeldung an. Das ist auch bei kurzzeitig getrennter Verbindung synchronisierbar. Ein tatsächlicher Alarm sendet zusätzlich eine kurzfristige \`MessageClient\`-Startnachricht; Stoppen sendet eine Stopnachricht. Die normale Galaxy-Wearable-Benachrichtigung wird als ältere Fallback-Variante vorerst beibehalten.
2. **Neue Watch-App (v23)**: Eigenständige Wear-OS-Anwendung mit exakt derselben Android-Paketkennung \`com.cobrachris.sanftwecker\` und später exakt demselben ursprünglichen Android-Signaturzertifikat wie die Handy-App. **Nicht identisch mit der alten separat installierten \`Watch-Vibrationstest\`-PoC-App** (andere Paketkennung). Google Play Services erlaubt Data-Layer-Nachrichten ausschließlich zwischen Apps mit gleichem Paketnamen und Signatur.
3. **Lokale Weckplanung auf der Uhr:** Das Wear-Listener-Service speichert geplante Termine dauerhaft. \`AlarmManager.setAlarmClock\` löst auf der Uhr einen **lokalen echten Alarm** aus, auch wenn das Telefon gerade nicht verbunden ist (unter Voraussetzung vorheriger Termin-Synchronisierung). Nach Uhrneustart werden gespeicherte Termine wieder aktiviert. Wo das Betriebssystem noch keine exakten Alarme genehmigt, besteht ein potenziell ungenauer Idle-Fallback; der Uhrstatus ist prüfbar.
4. **Direkte Vibrationssteuerung:** Ein eigenes Wear-OS-Foreground-Service startet den Hardware-Vibrator mit einer wiederholten Impulsfolge. Die Vibration wird auf Android 13+ mit \`VibrationAttributes.USAGE_ALARM\`, andernfalls mit \`AudioAttributes.USAGE_ALARM\` klassifiziert, nicht mehr als gewöhnliche Benachrichtigung. Der Alarm ist zeitlich auf maximal 15 Minuten begrenzt; ein sichtbarer Uhr-Stoppen-Knopf kann ihn jederzeit beenden. Ein Stopp von Handy oder Uhr synchronisiert, sobald verbunden, beide Seiten. Das Telefon kann weiterhin unabhängig klingeln.
5. **Kontrollierte Fehlersicherheit:** Nachrichten sind auf einen exakten App-Pfad, Zahlen-/Versionsbereiche und ein enges Zeitfenster beschränkt; verspätete, wiederholte Startsignale lösen keinen Alarm erneut aus. Eingehende Stoppbefehle müssen zum aktiven Alarmzeitpunkt passen. Watch-Status-/ACK-Nachrichten werden ohne sensible Nutzerdaten im Handy-Diagnoseverlauf protokolliert.

## Nicht behauptete Garantien

- Die Uhr kann beim aktivierten Samsung-Modus \`Nicht stören\` trotz Alarm-Verwendungszweck noch durch eine System-/Herstellerrichtlinie oder fehlende Alarmerlaubnis blockiert werden. Apps dürfen DND nicht heimlich aufheben. Der **eine abschließende Endtest** klärt diese Plattformabhängigkeit.
- Bestehende v21-Installationen kennen die neue Companion-App nicht. Telefon und Uhr müssen beide die neue **passend signierte v23** tragen. Wegen der anderen Paketkennung ist die neue Watch-App eine **einmalige zusätzliche Uhr-Installation**; die frühere Watch-Test-App wird nicht automatisch ersetzt oder entfernt. Es wird **keine** alte Handy-App deinstalliert und keine Nutzerdaten gelöscht.
- Der direkte Wear OS Data Layer funktioniert nur bei Google-Play-Services-Kompatibilität und passender Signatur; bei getrenntem Gerät greift ausschließlich die bereits synchronisierte lokale Alarmplanung. Es wird kein kostenpflichtiger Service benötigt.
- Die Android- und Watch-APK werden vor der Veröffentlichung gemeinsam kompiliert, gelintet, automatisch getestet und müssen im Signierworkflow auf **das identische ursprüngliche Zertifikat** geprüft werden. Die Veröffentlichung bleibt ein **gesonderter ausdrücklicher Release-Freigabeakt**; das Mergen von Quellcode löst keine Signierung aus.
- Die derzeit signierte v21 bleibt unverändert und das alte Release-Repository bleibt für ältere Nutzer weiterhin erreichbar.

## Einmalige Endabnahme, erst NACH signiertem v23-Release

1. Auf dem Handy **v23 über die vorhandene v21 aktualisieren**, niemals deinstallieren. Auf der Galaxy Watch die **neue** \`Sanftwecker Uhr\`-APK einmalig installieren. Die zuvor verwendete Watch-Vibrationstest-App braucht nicht für die neue Architektur genutzt zu werden.
2. In der Uhr-App prüfen, dass die nächste Weckzeit erscheint. Ohne weitere Zwischentests Handy-DND **an** und Uhr-DND **an**, Uhr tragen und einen Sanftwecker-Testalarm starten.
3. Auf der Watch sollte die **eigene** Weckvibration starten, während der Handy-Alarm normal klingelt. Uhralarm stoppen und prüfen, ob er auch auf dem Handy endet; anschließend normalen Wecktermin und Schlummern testen.
4. Einmal den Alarm bei abgeschaltetem Bluetooth / getrennter Verbindung überprüfen, **nur wenn die erste DND-Abnahme bestanden ist**. Der vorher synchronisierte Uhrtermin muss selbstständig starten können.
5. Bewahrung aller Wecker/Timer und später den Handy-Neustart nach Rückkehr nach Deutschland überprüfen. Für die Abnahme reicht ein einziges gebündeltes Feedback.

## Primärquellen

- Google Wear Data Layer / gleiche Paket-ID und Signatur: https://developer.android.com/training/wearables/data/overview
- Offline-fähige DataItems: https://developer.android.com/training/wearables/data/data-items
- Lokaler Alarm: https://developer.android.com/reference/android/app/AlarmManager#setAlarmClock(android.app.AlarmManager.AlarmClockInfo,%20android.app.PendingIntent)
- Vibration als Alarm: https://developer.android.com/reference/android/os/Vibrator
