# Agenten-Hinweise für die Sanftwecker-Migration

1. Die vorhandene Paket-ID `com.cobrachris.sanftwecker` niemals ändern.
2. Bestehende Android-Signatur und die aus `CobraChris/app-fabrik-template` ausgelieferten Updates müssen kompatibel bleiben. Keine neue Signieridentität und kein ungeprüftes Release.
3. In diesem Repository keine privaten Schlüssel, Keystores, Tokens, Credentials, Backup-Schlüssel oder Workflows zum Erzeugen neuer Signaturzertifikate hinzufügen.
4. Signing-Secrets nur im gesondert autorisierten privaten Controller migrieren; zuvor Backup-/Zertifikatsvergleich und Nutzerfreigabe erforderlich.
5. `UpdateActivity` und `UpdateUrlPolicy` zeigen bis zur verifizierten Übergangsfreigabe auf das ursprüngliche Release-Repository. Nicht blind umstellen.
6. CI darf ohne Zugangsdaten Unit-Tests, Lint und unsignierte Builds ausführen. Nutzer installiert Release-Updates selbst und bestätigt die praktische Funktion einschließlich Bildschirmfreigabe, exakten Alarmen und Galaxy Watch.
7. In GitHub-Issue https://github.com/CobraChris/app-fabrik-template/issues/25 die maßgebliche Migration dokumentieren und aktuelle Release-Version vor neuen Versionsnummern erneut prüfen.
