# Test-APK 0.1.2

[ANTplusExplorer-0.1.2-debug.apk herunterladen](https://github.com/Titanyus/ANTplusExplorer/raw/refs/tags/v0.1.2/downloads/ANTplusExplorer-0.1.2-debug.apk)

Update der vorhandenen Testapp für Android 13+; App-ID und Debug-Signatur bleiben gleich.

Neu: **Bluetooth-LE-Test ohne ANT-Dienste**. Im Profil Bluetooth LE auswählen, Geräte in der Nähe erlauben und JFICycle oder iTrack aus der Liste wählen. Wahoo und Jepster vorher vom Rad trennen.

Die App zeigt die GATT-Dienste und empfängt unterstützte FTMS-Indoor-Bike-, Cycling-Power- und CSC-Trittfrequenzdaten. Unbekannte Herstellerdienste werden zur Diagnose mit UUID angezeigt, aber nicht erraten oder gesteuert. Eine BLE-Verbindung allein bestätigt noch keine Trainingsdaten. Ein unterstütztes Datenabo und empfangene Werte sind getrennt sichtbar.

ANT+-Profile und Dienstdiagnose aus 0.1.1 bleiben verfügbar. BLE benötigt keinen ANT Radio Service und keinen ANT+ Plugins Service.

Geprüft am 12.10.2026: Build, **24 JVM-Tests** ohne Fehler/übersprungene Tests, Android Lint ohne Befunde, APK-Signatur. Der BLE-Hardwaretest auf dem Xiaomi/ICR50 steht noch aus.

SHA-256: `719637a6ad11d10d492374abbe399498eeaf03e552a01f4c4194e2d67a5cdfb5`

[Installations- und Testanleitung](../README.md)

## Vorherige Versionen

Die APKs 0.1.0 und 0.1.1 bleiben unverändert unter ihren Git-Tags erhalten. Alle Prüfsummen stehen in `SHA256SUMS`.
