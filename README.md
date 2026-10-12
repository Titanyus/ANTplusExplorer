# ANT+ Explorer · Matrix ICR50

Kleine Kotlin-Testapp für **Android 13+** mit ANT+- und Bluetooth-LE-Verbindungstest. Wie B-Cycle: eine Activity mit nativen Android-Views, ohne Compose oder zusätzliche UI-Bibliotheken. Eigenständige App-ID; B-Cycle kann parallel installiert bleiben.

## APK installieren

[Aktuelle Test-APK 0.1.2 herunterladen](https://github.com/Titanyus/ANTplusExplorer/raw/refs/tags/v0.1.2/downloads/ANTplusExplorer-0.1.2-debug.apk)

Debug-signierte Testversion. Android muss die Installation aus dem Browser/Dateimanager erlauben. Bestehende Testapp aktualisieren; die Signatur bleibt gleich. Prüfsummen und Prüfbericht: `downloads/`.

## Bluetooth LE testen · JFICycle / iTrack

Jepster erkennt beim Nutzergerät **JFICycle über BLE**. Die Zuordnung von **iTrack** und die tatsächlich angebotenen Trainingsdatendienste sind noch offen. Die Testapp leitet keinen Verbindungstyp nur aus dem Namen ab.

1. **Wahoo und Jepster vom Trainer trennen**. Rad aktivieren und treten.
2. Bluetooth einschalten, in der Testapp **Bluetooth LE** wählen und **Trainer suchen / verbinden** drücken. Android-Berechtigung **Geräte in der Nähe** erlauben.
3. **JFICycle** aus der Liste auswählen. iTrack oder ein plausibles unbenanntes Gerät können separat getestet werden. Die BLE-Liste zeigt Adresssuffix und Signalstärke. Kein vorheriges Android-Pairing nötig.
4. Die App verbindet über Bluetooth LE, zeigt die **GATT-Dienste** und abonniert unterstützte Trainingsdaten. Die Suche endet nach 20 Sekunden; gefundene Geräte können danach noch ausgewählt werden.
5. Weiter treten und Werte beobachten. **Bluetooth verbunden** allein bestätigt keine Trainingsdaten. Entscheidend sind Live-Messdaten und plausible Werte, die auf verändertes Treten reagieren.
6. **Testbericht kopieren**. Bei fehlenden Werten enthält er Dienste-/Merkmals-UUIDs und erste Datenframes. Unbekannte Herstellerprotokolle werden angezeigt, aber nicht geraten.

**BLE benötigt keine ANT+-Dienste.** Die App fordert Bluetooth-Such-/Verbindungsberechtigungen an, keine Standortberechtigung. Sie verbindet nur nach Geräteauswahl.

## ANT+ testen und Dienste prüfen

ANT+-fähige Hardware, ANT Radio Service und ANT+ Plugins Service im selben Android-Profil sind nötig. Beide Services sind **separate Apps**. ANT+ Fitness Equipment oder ANT+ Fahrradleistung wählen, Suche starten, Trainer auswählen und weiter treten. Verbindung und Messdatenempfang werden getrennt angezeigt.

- [ANT Radio Service im Play Store](https://play.google.com/store/apps/details?id=com.dsi.ant.service.socket)
- [ANT+ Plugins Service im Play Store](https://play.google.com/store/apps/details?id=com.dsi.ant.plugins.antplus)
- [Offizielle Radio-Service-4.18.0-APK aus dem Hersteller-SDK](https://github.com/ant-wireless/ANT-Android-SDKs/raw/0e8cea714a231fb9565170d4ed01eafa868f9798/ANT_Android_SDK/Services/Android_ANTRadioService_4-18-0.apk), auf Android 13 aktualisiert; Funktion auf Android 16 nicht bestätigt.

Die Diagnose unterscheidet nicht gefunden, deaktiviert und installiert, aber vom SDK nicht nutzbar. Die öffentliche ANTlib reserviert ANT+-Verbindungen für die Plugins-API; ein direkter ANT+-Umbau ohne Plugins wird daher nicht umgesetzt.

## Angezeigte Trainingsdaten

| Testweg | Daten, soweit vom ausgewählten Gerät gesendet |
| --- | --- |
| BLE FTMS · Indoor Bike Data (2AD2) | Geschwindigkeit, Trittfrequenz, Leistung, Distanz, Puls, Trainingszeit |
| BLE Cycling Power (2A63) | Leistung |
| BLE Cycling Speed/Cadence (2A5B) | Berechnete Trittfrequenz; keine geschätzte Radgeschwindigkeit |
| ANT+ Fitness Equipment | Geschwindigkeit, Distanz, Trainingszeit, Puls; bei BIKE/TRAINER zusätzlich Leistung und Trittfrequenz |
| ANT+ Fahrradleistung | Leistung und Trittfrequenz |

„—“ bedeutet fehlend/ungültig; **0 W** ist ein echter Messwert. Nach fünf Sekunden ohne neue Messwerte sind letzte Werte als veraltet markiert. Gepufferte ANT+-Ereignisse und synthetische Power-Start-/Stillstandswerte bestätigen keinen Live-Empfang. Der Zähler zählt Messdatenereignisse, keine Funkpakete. Trainer-Distanz und -Zeit sind Gerätewerte.

Die App verändert **keinen Widerstand** und sendet keine Leistungs-, Start-/Stop- oder Kalibrierbefehle. BLE-Schreibzugriffe aktivieren ausschließlich standardisierte Datenbenachrichtigungen. Manche Geräte benötigen Herstellerbefehle; unbekannte Protokolle werden nicht implementiert.

**Trennen** gibt die Verbindung frei. Beim Verlassen der App werden BLE-Suche/Verbindung und bestehende ANT+-Verbindungen beendet. Der externe ANT+-Suchdialog bleibt ausgenommen. Eine Bildschirmdrehung erfordert eine neue Suche. **Der neue BLE-Weg ist noch nicht am Xiaomi/Matrix ICR50 getestet.**

## Entwickeln

Android Studio, JDK 17 oder 21, Android SDK Platform 35 und Build Tools 35.0.0. Gradle 8.11.1, Android Gradle Plugin 8.9.2 und Kotlin 2.1.20 sind festgelegt; der Wrapper prüft die Gradle-SHA-256-Prüfsumme.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

SDK-Pfad über Android Studio, `ANDROID_HOME` oder eine nicht eingecheckte `local.properties` (`sdk.dir=...`) festlegen. APK: `app/build/outputs/apk/debug/app-debug.apk`. Installation mit `adb install -r app/build/outputs/apk/debug/app-debug.apk` oder direkt auf dem Smartphone.

In der verwalteten Linux-x64-Cloud:

```sh
python scripts/setup-cloud.py
python scripts/build-cloud.py
```

Das Setup legt JDK und Android-Werkzeuge ausschließlich unter `/workspace` ab und überprüft Downloads anhand festgelegter Prüfsummen. Der Build-Helper verwendet den vorhandenen HTTPS-Proxy und den System-Vertrauensspeicher, ohne TLS-Prüfungen abzuschalten. Es werden keine Zugangsdaten ins Repository kopiert. Bereits vorhandene Checkouts verwenden; für Cloud-Aufgaben keinen zusätzlichen Git-Worktree erstellen.

### Komponenten für B-Cycle

- `BleTrainerClient`: BLE-Suche, Geräteauswahl, GATT-Dienste und seriell aktivierte Datenabos; nach Bluetooth-Berechtigung aufrufen und beim Beenden schließen.
- `BleMeasurements`: Parser für FTMS, Cycling Power und CSC mit Einheiten, optionalen Feldern und Zählerüberläufen.
- `AntTrainerClient`: ANT+-Suchdialog, Fehlerdiagnose, Datenabos und Freigabe eines Kanals; alte Callbacks werden nach Trennen/Neustart verworfen. UI-Callbacks laufen auf dem Main-Thread.
- `TrainerSnapshot`: Messwerte und Empfangsstatus ohne Android-Abhängigkeiten.
- `MainActivity`: einfache Testoberfläche und kopierbarer Diagnosebericht.

Nach einem erfolgreichen Hardwaretest können Client und Datenmodell in B-Cycle übernommen werden. Der Client erwartet eine Activity für den Suchdialog und muss beim Beenden geschlossen werden. Die ANT+-Bibliothek und Manifest-Paketsichtbarkeit werden ebenfalls benötigt.

## ANT+-SDK

Offizielle ANT+ PluginLib **3.9.0** als lokale AAR; diese Version unterstützt die Android-Paketsichtbarkeit ab API 30 und Android 13. Keine ANT+-Netzwerkschlüssel im Projekt.

Quelle: [ant-wireless/ANT-Android-SDKs](https://github.com/ant-wireless/ANT-Android-SDKs), Commit `0e8cea714a231fb9565170d4ed01eafa868f9798`, Datei `ANT+_Android_SDK/API/antpluginlib_3-9-0.aar`. Lizenzhinweise liegen in `app/src/main/assets/ANT-LICENSE.txt` und damit auch in der APK. SDK-Provenienz und SHA-256 stehen in `app/libs/README.md`.

Die 24 JVM-Tests prüfen Dienste-Diagnose, Einheiten, BLE-Dekodierung einschließlich unvollständiger Frames und Zählerüberläufen sowie Empfangsausfälle. Build und Tests ersetzen den Funk-/Kompatibilitätstest auf deinem Smartphone nicht.
