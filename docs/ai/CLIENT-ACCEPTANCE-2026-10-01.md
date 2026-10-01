# Client-Abnahme 26.3 – 2026-10-01

Nach Ende der Besitzer-Clients wurden die Tests seriell im isolierten
`%TEMP%/sbgate` ausgeführt. Keine Besitzerwelt wurde verändert. Hub-Neustart
war bereits um 07:05 MESZ erledigt; kein weiterer Neustart war erforderlich.

## Befunde und Korrekturen

- Dimensions: Cloth erstellt eine nicht öffentliche Unterklasse von
  `BooleanListEntry`. Der Test prüft jetzt Unterklassen, die exakte Reihenfolge
  der dargestellten Schalter und die öffentliche Basis-API für Ja/Nein.
- Bündel-Tooltip: Die Produktionsanzeige enthält `64/192`. Der alte Test
  verglich ihre Textanzahl mit Vanillas `Full`. Jetzt wird zuerst der echte
  Kapazitätstext geprüft, anschließend die Skalierung ohne diese zusätzliche
  Beschriftung. Gegenproben mit Faktoren 3, 6 und 1 bleiben erhalten.
- Visuals: Mit Simple Models bleibt Legacy-CIT absichtlich inaktiv. Der Test
  gleicht den Guard mit der tatsächlichen Loader-Auswahl ab und prüft den
  unveränderten Stack. Ohne Simple Models bleiben positive CIT-, Prioritäts-
  und Renderkopie-Prüfungen erhalten. Cleanup läuft auch bei Fehlern.

Es wurden Testannahmen korrigiert, keine Gameplay-Funktionen verändert.
Commits: `71e8722d`, `60b26a4a`, `f7257acf`.

## Zusätzlich gefundene Server-Testisolation

Das Abschlussgate auf `68fbf7b5` bestand `check` und 1606/1606 Hauptlinientests,
aber nur 467/485 Integrationstests. 18 Fehler betrafen Fabric-Dimensions.
Log: `.ai-runs/client-followup-full-gate-68fbf7b5-red.log`;
Run: `2026-10-01T18-38-53Z-1a5c`.

Im Log überlappen Skyblock- und Mining-Settings-Journeys trotz Tick-Abständen.
Jede restaurierte dieselbe Datei; ein Cleanup überschreibt dadurch den Zustand
einer anderen Journey und hinterlässt Skyblock ausgeschaltet. Commit `8bab8677`
gibt jeder Journey eine eigene Testumgebung. Fabric und NeoForge wurden danach
zweimal vollständig geprüft: **je 82/82 grün**, Konfigurationsbytes beider
Instanzen nach jedem Lauf unverändert. Beleg:
`.ai-runs/dimensions-isolation-proof.json` und `dimensions-isolation-{1,2}.log`.
Der rote isolierte Zustand wurde zuvor gesichert und nur Skyblock wieder
aktiviert. Kein Besitzerzustand wurde angefasst. Die Forge-Registrierung ist
konsistent nachgezogen und statisch geprüft, jedoch nicht ausgeführt.

## Tatsächliche Läufe

Die JSON-Datensätze und vollständigen Logs liegen unter `testing/runs/`.

| Run-ID | Prüfung | Ergebnis |
| --- | --- | --- |
| `2026-10-01T17-10-19Z-498a` | Dimensions erster Versuch | Rot: Forge-Maven-Netztimeout vor Clientstart |
| `2026-10-01T17-26-02Z-f165` | Dimensions ursprünglicher Test | 3/7; falscher exakter Klassennamenvergleich |
| `2026-10-01T17-39-34Z-4a56` | Vollständige Fabric-/NeoForge-Clients auf `09567ce3` | 218/242; je ein HUD-Skript scheiterte am Tooltip-Vergleich, alle übrigen Skripte liefen durch |
| `2026-10-01T18-00-21Z-8ce0` | Dimensions auf `60b26a4a` | **7/7 grün** |
| `2026-10-01T18-03-09Z-20c7` | HUD/Tooltip auf Fabric und NeoForge, `60b26a4a` | **32/32 grün**, gezielt `hud-and-tooltip` |
| `2026-10-01T18-10-53Z-b4bd` | QoL-Client | **5/5 grün** |
| `2026-10-01T18-12-09Z-9030` | Sounds-Client | **3/3 grün** |
| `2026-10-01T18-13-14Z-31a1` | Ursprünglicher Visuals-Smoke mit Simple Models | 2/5; widersprüchliche Legacy-Erwartung |
| `2026-10-01T18-22-26Z-a224` | Visuals mit Simple Models, `f7257acf` | **5/5 grün** |
| `2026-10-01T18-26-00Z-90b8` | Visuals ohne Simple Models, `f7257acf` | **5/5 grün** |

Die positiven Folgeläufe ergeben 57 Screenshot-Prüfpunkte. Das ist kein neuer
vollständiger Clientlauf auf einem einzigen Commit: unveränderte Skripte wurden
nicht wiederholt. `alles gruen`, Logs und Datensätze wurden gelesen, nicht nur
Exitcodes. `configureondemand=true` vermied beim Clientstart die unbeteiligte
Forge-Konfiguration. Alle Clients blieben seriell, mit einem Gradle-Worker.

Die Standalone-Auswahl enthielt ausschließlich SimpleBuilding, Simple Visuals
und Cloth; `integration/enabled-mods.json` wurde anschließend bytegleich
wiederhergestellt. Der Datensatz dieses Laufs ist daher absichtlich dirty.
Andere Läufe melden die schon bekannte generierte SimpleTweaks-JSON-Datei;
der vollständige normalisierte tracked Diff war leer.

## Sichtprüfung und Grenzen

Archivierte Bilder: `.ai-runs/client-acceptance-evidence-20261001/`.

- Dimensions: drei passende Schalter, EN/DE vollständig lesbar, keine
  Überlappung. Mining/Abbau nach Speichern und Wiederöffnen Nein, die anderen
  beiden Ja. Datei, integrierter Server und Wiederherstellung sind zusätzlich
  durch Assertions geprüft. Portal sichtbar. Der frühe Titel-Screenshot zeigt
  nur den Hintergrund und ersetzt keine visuelle Titelmenü-Abnahme.
- HUD: echter Bündel-Tooltip mit `64/192` und etwa einem Drittel Füllung;
  Wand-Dialoge, Octant-HUD, Kreativinventar, Rucksack und platziertes Bündel
  stichprobenartig geprüft. Die Bilder des ursprünglichen Vollclientlaufs
  wurden vor dem gezielten Lauf angesehen, aber nicht separat archiviert.
- QoL: EN/DE-Interaktionsreiter und Crawl-Ansicht geprüft. Sounds: sichtbare
  General-Einstellungen passen; Auslösung und OFF-Zustand technisch geprüft.
- Visuals: Konfiguration und Amboss lesbar; Legacy-Browser mit Simple Models
  leer, ohne Simple Models zwei echte Modellvorschauen (Diamant/Smaragd).
- Offen: dunkle Beschriftung im Octant-Manager; Rezepttoast überlagerte dessen
  frühes Testbild. Keine pauschale Sichtfreigabe aller Tabs, Bücher oder Shops.
- Hörbare Audioqualität, Forge-Dialoge und Besitzerwelt bleiben ohne Abnahme.
  Claims bleiben AUS; keine neuen Balancing-, Portalform- oder Portentscheidungen.

## Abschlussgate

Die Testkorrekturen benötigen ein neues vollständiges Gate einschließlich
Integration. Exakte SHA, `check`, echte Testresultate und `VERDICT` stehen in
`.ai-runs/client-followup-full-gate.log`; nur dessen GREEN-SHA darf gepusht
werden. Das frühere GREEN für `09567ce3` gilt nicht automatisch für diese
Korrekturen. Der Gate-Lauf erhält bestehende Dateien statt Checkout-Bereinigung.
