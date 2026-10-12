# ANT+ Explorer

Sehr kleine Kotlin-Testapp für **Android 13+** und einen **Matrix ICR50**. Wie B-Cycle: eine Activity mit nativen Android-Views, ohne Compose oder zusätzliche UI-Bibliotheken. Eigenständige App-ID: B-Cycle kann parallel installiert bleiben.

## APK installieren und testen

[APK herunterladen](https://github.com/Titanyus/ANTplusExplorer/raw/refs/heads/main/downloads/ANTplusExplorer-0.1.1-debug.apk)

Die APK ist eine signierte Debug-Testversion (0.1.1), keine Play-Store-Veröffentlichung. Android muss die Installation aus dem verwendeten Browser/Dateimanager erlauben. Prüfsumme: siehe `downloads/SHA256SUMS`.

1. ANT Radio Service und ANT+ Plugins Service auf dem Smartphone installieren bzw. aktualisieren. Die App zeigt deren erkannte Versionen. ANT+-fähige Hardware wird zusätzlich benötigt; installierte Dienste allein reichen nicht. Dein bereits erfolgreich verwendetes ANT+-Setup ist dafür der Ausgangspunkt.
2. ICR50/Konsole aufwecken, treten und eine eventuell vorhandene ANT+-Übertragung aktivieren. Andere Trainingsapps für den ersten Test schließen.
3. **Fitness Equipment (FE / FE-C)** auswählen und **Trainer suchen / verbinden** drücken. Im Suchdialog des ANT+-Dienstes den Trainer auswählen. Geräte-ID und Name helfen, ihn von anderen Geräten zu unterscheiden.
4. Wenn kein Gerät erscheint: Suche abbrechen und **Fahrradleistung (Bicycle Power)** testen. Beide Profile werden einzeln getestet; die App kombiniert keine unabhängigen Sensoren.
5. Nach der Geräteauswahl weiter treten. Erfolg bedeutet: **Verbunden**, **Live-Daten empfangen**, fortlaufende Ereignisse und plausible, auf verändertes Treten reagierende Werte. Ein ausgewähltes Gerät oder der Status „Verbunden“ allein beweist noch keinen Messdatenempfang.
6. **Testbericht kopieren** drücken und den Bericht für die spätere B-Cycle-Integration aufbewahren. Besonders interessant: Smartphone, Dienste-Versionen, Profil, ANT-Geräte-ID, Gerätetyp und welche Messwerte tatsächlich vorhanden sind.
7. **Trennen** beendet den Zugriff und gibt den ANT+-Kanal frei. Beim Verlassen der App wird eine bestehende Verbindung beendet. Der externe ANT+-Suchdialog darf geöffnet bleiben. Eine Bildschirmdrehung beendet die Sitzung; anschließend erneut suchen.

## Dienst wird trotz Installation beanstandet

ANT+ Plugins Service und ANT Radio Service sind **separate Apps**. Beide müssen im selben Android-Profil wie ANT+ Explorer vorhanden und aktiviert sein. Der Play Store kann ein Paket als installiert anzeigen, obwohl der andere Dienst fehlt oder das Paket in einem anderen Benutzer-/Arbeitsprofil liegt.

- [ANT Radio Service im Play Store](https://play.google.com/store/apps/details?id=com.dsi.ant.service.socket)
- [ANT+ Plugins Service im Play Store](https://play.google.com/store/apps/details?id=com.dsi.ant.plugins.antplus)

Ab 0.1.1 meldet die App die konkret vom ANT-SDK beanstandete Abhängigkeit mit Paketname und unterscheidet „nicht gefunden“, „deaktiviert“ und „installiert, aber vom SDK nicht nutzbar“. Die App-Einstellungen sind direkt erreichbar. Der Testbericht enthält Versionscodes und die Paketerkennung des ANT-SDKs. Nach einer Installation/Aktivierung zur App zurückkehren und erneut suchen. Ist das Paket installiert und aktiviert, bitte den neuen Diagnosebericht zur weiteren Analyse verwenden.

## Was wird getestet?

| Profil | Angezeigte Daten, soweit vom Gerät gesendet |
| --- | --- |
| Fitness Equipment | Geschwindigkeit, Distanz, Trainingszeit, Puls; für Gerätetyp BIKE/TRAINER zusätzlich Leistung und Trittfrequenz |
| Fahrradleistung | Berechnete Leistung in Watt und übertragene Trittfrequenz |

„—“ bedeutet fehlend oder ungültig, **0 W** bleibt ein gültiger Wert. Nach fünf Sekunden ohne neue Live-Messdaten erscheint eine Warnung; alte Werte werden nicht als aktuelle Verbindung ausgegeben. Gepufferte Ereignisse und synthetische Power-Start-/Stillstandswerte bestätigen keinen Live-Datenempfang. Die Ereigniszahl zählt API-Messdatenereignisse, keine Funkpakete. Die Trainer-Distanz und Trainer-Zeit sind Gerätewerte, keine lokale Aufzeichnung.

Die App **empfängt ausschließlich**. Sie sendet keine Widerstands-, Leistungs- oder Kalibrierbefehle. Sie setzt kein Bluetooth-Pairing voraus und fordert keine Standort- oder Bluetooth-Berechtigung an; ANT-Hardware und Suche verwaltet der installierte ANT+-Dienst.

Die vom konkreten ICR50 samt Konsole/Sensorausstattung gesendeten ANT+-Profile sind noch **nicht am Gerät bestätigt**. Falls beide Suchen leer bleiben, ist damit weder ein Smartphone-Defekt noch eine erfolgreiche ANT+-Unterstützung des ICR50 bewiesen. Prüfe dann die Funkprofile der tatsächlichen Konsole/Sensoren: Ein ANT+-Herzfrequenzempfänger am Trainer ist nicht automatisch ein Sender für Trainingsdaten. Bluetooth-FTMS und zusätzliche ANT+-Profile sind in dieser ersten Testversion nicht enthalten.

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

- `AntTrainerClient`: ANT+-Suchdialog, Fehlerdiagnose, Datenabos und Freigabe eines Kanals; alte Callbacks werden nach Trennen/Neustart verworfen. UI-Callbacks laufen auf dem Main-Thread.
- `TrainerSnapshot`: Messwerte und Empfangsstatus ohne Android-Abhängigkeiten.
- `MainActivity`: einfache Testoberfläche und kopierbarer Diagnosebericht.

Nach einem erfolgreichen Hardwaretest können Client und Datenmodell in B-Cycle übernommen werden. Der Client erwartet eine Activity für den Suchdialog und muss beim Beenden geschlossen werden. Die ANT+-Bibliothek und Manifest-Paketsichtbarkeit werden ebenfalls benötigt.

## ANT+-SDK

Offizielle ANT+ PluginLib **3.9.0** als lokale AAR; diese Version unterstützt die Android-Paketsichtbarkeit ab API 30 und Android 13. Keine ANT+-Netzwerkschlüssel im Projekt.

Quelle: [ant-wireless/ANT-Android-SDKs](https://github.com/ant-wireless/ANT-Android-SDKs), Commit `0e8cea714a231fb9565170d4ed01eafa868f9798`, Datei `ANT+_Android_SDK/API/antpluginlib_3-9-0.aar`. Lizenzhinweise liegen in `app/src/main/assets/ANT-LICENSE.txt` und damit auch in der APK. SDK-Provenienz und SHA-256 stehen in `app/libs/README.md`.

Die JVM-Tests prüfen Einheiten, ungültige Werte und die Unterscheidung zwischen Verbindung, frischen Daten und Empfangsausfall. Build und Tests ersetzen den Funk-/Kompatibilitätstest auf deinem Smartphone nicht.
