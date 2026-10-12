# Test-APK 0.1.1

[ANTplusExplorer-0.1.1-debug.apk herunterladen](https://github.com/Titanyus/ANTplusExplorer/raw/refs/tags/v0.1.1/downloads/ANTplusExplorer-0.1.1-debug.apk)

Update der vorhandenen Testapp für Android 13+; App-ID und Debug-Signatur bleiben gleich.

0.1.1 korrigiert die pauschale Meldung über fehlende Dienste: Die konkret vom ANT-SDK beanstandete Abhängigkeit wird mit Paketname angezeigt. Installation, Aktivierung, Version und Versionscode sowie die Paketerkennung des SDKs sind im Diagnosebericht enthalten. Buttons öffnen die Android-App-Einstellungen der beiden Dienste.

ANT+ Plugins Service **ersetzt nicht** den separaten ANT Radio Service. Dieses App-Update installiert keine ANT-Dienste und bestätigt noch keine Funkverbindung.

Geprüft am 12.10.2026: Build, 14 JVM-Tests ohne Fehler/übersprungene Tests, Android Lint ohne Befunde und APK-Signatur. Die Verbindung auf dem Smartphone bleibt ein Hardwaretest.

SHA-256: `bbcefda487c7ca074a54029149d07cb862174c30eee9cd99706194e6b43935f2`

[Installations- und Testanleitung](../README.md)

## Vorherige Version

[0.1.0 herunterladen](https://github.com/Titanyus/ANTplusExplorer/raw/refs/tags/v0.1.0/downloads/ANTplusExplorer-0.1.0-debug.apk). Die ursprüngliche APK bleibt unverändert erhalten; beide Prüfsummen stehen in `SHA256SUMS`.
