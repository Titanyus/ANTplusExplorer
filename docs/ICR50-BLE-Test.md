# Matrix ICR50 · erfolgreicher BLE-Test

Am **12.10.2026** hat der Nutzer den erfolgreichen Test mit **ANT+ Explorer 0.1.2**, einem **Xiaomi mit Android 16** und dem **Matrix ICR50** bestätigt. Grundlage sind der kopierte Diagnosebericht und die anschließende Rückmeldung „Hat geklappt!“. Der Test wurde auf dem Smartphone ausgeführt, nicht in der Cloud.

## Bestätigte Anbindung

- Gerät meldet sich als **JFICcycle**.
- Verbindung: **Bluetooth Low Energy**.
- Fitness Machine Service: `00001826-0000-1000-8000-00805f9b34fb`.
- Trainingsdaten: **Indoor Bike Data**, `00002ad2-0000-1000-8000-00805f9b34fb`.
- Zusätzlich angeboten und abonniert: Cycling Speed/Cadence, Messmerkmal `00002a5b-0000-1000-8000-00805f9b34fb`. Die im Bericht protokollierten Benachrichtigungen stammen aus Indoor Bike Data; ein separater CSC-Datenempfang ist damit noch nicht belegt.
- Zwei Datenabos aktiv, **27 Live-Messdatenereignisse** zum Zeitpunkt des Berichts.
- **ANT Radio Service war nicht installiert**. Er wird für diesen BLE-Weg nicht benötigt.

## Empfangene Werte im Bericht

| Messwert | Wert |
| --- | --- |
| Leistung | 5 W |
| Trittfrequenz | 40 U/min |
| Geschwindigkeit | 10,9 km/h |
| Distanz | 0,12 km |
| Trainingszeit | 37 s |
| Puls | nicht empfangen |

Der Bericht enthält wechselnde FTMS-Datenframes mit und ohne Geschwindigkeit. Die App dekodiert diese anhand der Flags und erhält Felder, die in einem Folgeframe fehlen. Dies bestätigt Verbindung und Datenübertragung; die physikalische Genauigkeit der vom Trainer gelieferten Werte wurde nicht vermessen.

## Übernahme in B-Cycle

Für die getestete Ausstattung ist **BLE FTMS** der bestätigte Weg. `BleTrainerClient`, `BleMeasurements` und `TrainerSnapshot` enthalten Suche, Geräteauswahl, Datenabos, Dekodierung und Empfangsstatus. In B-Cycle werden Bluetooth-Such-/Verbindungsberechtigungen und eine Freigabe der GATT-Verbindung beim Beenden benötigt; ANT+-Bibliotheken sind für diesen BLE-Weg nicht erforderlich.

Weiterhin offen: Langzeitstabilität, Wiederverbindung, Geräteverhalten bei gleichzeitig geöffneten Trainingsapps und eine eventuelle separate ANT+-Unterstützung. Die Zugehörigkeit des Android-Bluetooth-Namens „iTrack“ wurde nicht bestätigt. Widerstandssteuerung wurde nicht getestet und ist nicht implementiert.
