# Welt-Upgrade Minecraft 26.2 -> 26.3

Stand 2026-09-25. Frage: Eine Welt, die auf 26.2 mit SimpleBuilding gespielt wurde, wird auf 26.3 mit
dem 26.3-Build der Mod geoeffnet - bleibt alles, was die Mod gespeichert hat, erhalten?

Kurzantwort: **ja**, mit dem Build ab diesem Stand. Vorher gingen bestimmte Gegenstaende in
Mod-Behaeltern verloren (siehe 2). Getestet von `WorldUpgradeTests` gegen echte 26.2-Speicherdaten
in `testing/fixtures/upgrade-26.2/`.

## 1. Was Vanilla uebernimmt und was nicht

Minecraft aktualisiert alte Speicherdaten beim Laden mit dem DataFixer (26.2 = Datenversion 4903,
26.3 = 5023). Der kennt nur Vanilla-Typen:

- Blockzustaende in Chunk-Paletten, Gegenstaende in Vanilla-Behaeltern, im Spielerinventar, in
  Vanilla-Komponenten (Buendel, `minecraft:container`) werden aktualisiert - auch wenn es
  Mod-Gegenstaende sind (Mod-Items sind fuer den Fixer normale Item-Stapel).
- **Nicht** aktualisiert wird alles unter einer Mod-Id: der Inhalt von `simplebuilding:mod_hopper`,
  `mod_furnace`, `mod_blast_furnace`, `mod_smoker`, `backpack` (Blockentitaeten), die Entity
  `simplebuilding:levitating_block` und der Inhalt der Komponente `simplebuilding:backpack_contents`.
  Unbekannte Schluessel werden unveraendert durchgereicht.

Was 26.3 an Vanilla-Formaten aendert (DataFixer 4996-5016), und was davon Mod-Daten trifft:

| Aenderung in 26.3 | trifft |
|---|---|
| Blockzustand `Name`/`Properties` -> `id`/`properties` (BlockStateFieldNamesFix) | aufsteigender Block (Entity), Erzdetektor-Ziel (custom_data) |
| Entdeckerkarten umbenannt (`ocean_explorer_map` -> `ocean_monument_map` u. a.) | jeder Stapel in Mod-Behaeltern, Magnet-Filter |
| `minecraft:pot_decorations` Liste -> Karte | verzierte Kruege in Mod-Behaeltern |
| `minecraft:map_color`, `block_transformer` entfernt, `swing_animation` geteilt | Stapel mit diesen Komponenten in Mod-Behaeltern |

## 2. Audit aller persistenten Mod-Daten

Legende: **OK** = 26.3 liest, was 26.2 schrieb, ohne Zutun; **behoben** = war eine Luecke, jetzt
geschlossen; Test = welcher Test es belegt.

| Daten | Format | 26.2 -> 26.3 | Test |
|---|---|---|---|
| Oktant-Auswahl (custom_data `Pos1`/`Pos2`/`Locked`/`Shape`/`FillOrder`/`Hollow`) | Mod, Zahlen/Strings | OK | modItems |
| Erzdetektor (custom_data `Mode`, `CustomBlock` via NbtUtils) | Blockzustand | OK - liest schon beide Schreibweisen (Fix vom 2026-09-25) | modItems, OreDetectorTests |
| Blaupause (`simplebuilding:blueprint` code/title/author/signed, `blueprint_rotation`) | Mod-Codec, Strings | OK | modItems |
| Rucksack-Item (`simplebuilding:backpack_contents`) | eigene Eintraege `{slot,id,count,components}` | **behoben**: Eintraege gingen am Fixer vorbei; eine Entdeckerkarte oder ein Krug darin liess die **ganze Liste** scheitern -> Rucksack leer | modItems, player |
| Rucksack-Farbe (`minecraft:dyed_color`) | Vanilla | OK | modItems, modBlockEntities |
| Verstaerkte/Netherit/Enderit-Buendel (`minecraft:bundle_contents`) | Vanilla | OK (im Vanilla-Behaelter); im Rucksack **behoben** | modItems, modBlockEntities |
| Besatz (`minecraft:trim`) inkl. Mod-Materialien astralit/nihilith/enderite | Registry-Ids | OK - Material-Ids gibt es auf beiden Linien (Registry-Format der Materialien ist neu, die Ids nicht) | modItems |
| Verzauberungen inkl. Mod-Verzauberungen | Registry-Ids | OK - alle Mod-Verzauberungen existieren auf 26.3 | modItems |
| Leucht-Aufwertung (`glow_level`, `visual_glow`, `light_source`), `offset`, `coordinates` | Mod-Codecs, Zahlen | OK | modItems |
| Haltbarkeit / Aufwertungsstufe (eigene Items je Stufe, `minecraft:damage`) | Vanilla | OK | modItems |
| Magnet-Filter (custom_data `MagnetFilter` = Item-Id) | Item-Id als String | **behoben**: folgt jetzt Vanillas Umbenennungen (Entdeckerkarten) | modItems |
| Baustab-Einstellungen (custom_data `SettingsRadius`/`SettingsAxis`/`Mode`) | Zahlen | OK | modItems |
| Baustab mitten im Bauen (`BuildBlockRawId`/`CoverBlockRawId`) | **numerische Registry-Id** | **behoben**: jetzt Namen (`BuildBlock`/`CoverBlock`); ein alter laufender Bauvorgang stoppt, statt mit dem Block weiterzubauen, der auf 26.3 diese Nummer hat (26.3 fuegt Bloecke hinzu) | wandMidBuild |
| Mod-Trichter (Items, `GhostItems`, `FilterMode`, `TransferCooldown`) | Vanilla-Container unter Mod-Id | **behoben** | modBlockEntities |
| Mod-Oefen/Hochoefen/Raeucheroefen (Items, int-Timer, `simplebuilding:bonus_progress`) | wie Vanilla-Ofen unter Mod-Id | **behoben** (Items); Timer/Fortschritt OK | modBlockEntities |
| Vanilla-Oefen: int-Timer der Mod (`cooking_time_spent` ...) | Vanilla-Schluessel, Mixin | OK | - |
| Abgestellter Rucksack (`Contents`, `Color`) | wie das Item | **behoben** | modBlockEntities |
| Vorschlaghammer-Fortschritt (Saved Data `simplebuilding:sledgehammer_progress`) | Pos + Block-Id + Zahl | OK - keine Block-Umbenennung in 26.3 | savedData |
| Blaupausen-Auftraege (Saved Data `simplebuilding:blueprint_jobs`) | UUID, Strings, Pos | OK | savedData |
| Dynamisches Licht: `SimpleBuildingOwnedLight` an Entities, Lichtbloecke | BlockPos; Blockzustand in der Palette (Vanilla-Fix) | OK | modEntities |
| Gesperrter Rahmen (`SimpleBuildingLocked`) | bool | OK | modEntities |
| Aufsteigender Block (`simplebuilding:levitating_block`, `BlockState`, `TileEntityData`) | Vanilla-Falling-Block unter Mod-Id | **behoben**: wurde auf 26.3 zu Sand | modEntities |
| Spieler: `SimpleBuildingData` (Tracker, Resonanz-Basiswerte, `ActiveTicks`, `BaseXp`) | Zahlen | OK | player |
| Spieler: Rucksack im Inventar / auf dem Ruecken | Komponente | **behoben** (s. Rucksack-Item) | player |
| Rezeptbuch, Fortschritte (`simplebuilding:*`-Ids) | Ids | OK - alle Rezept- und Fortschritts-Ids gibt es auf 26.3 (nur Inhalte weichen ab) | - |
| Beutetabellen, Handel | datengetrieben, Ids | OK - beim Laden neu gelesen, nicht in der Welt gespeichert | - |
| Weltgenerierung | `configured_feature` fehlt auf 26.3 | OK - betrifft nur neue Chunks; vorhandene Erze bleiben | - |
| Konfiguration `config/simplebuilding.json` | Cloth/Gson, dieselbe Klasse | OK - fehlende Felder bekommen den Standardwert | - |

## 3. Wie die Luecken geschlossen sind

1. **`ModDataFixer` + `DataFixTypesMixin`** (common/src/shared). Jede Vanilla-Aktualisierung
   (Chunk, Entity-Chunk, Spieler, Strukturdatei, Saved Data) laeuft durch
   `DataFixTypes#update(DataFixer, Dynamic, int, int)`. Direkt danach laeuft der Vanilla-Fixer mit
   **demselben Versionsbereich** noch einmal ueber jedes Stueck Mod-Daten, verkleidet als das
   Vanilla-Ding, dem es gleicht: Mod-Trichter/-Oefen als `minecraft:hopper`/`furnace`/
   `blast_furnace`/`smoker` (ihre Klassen erben davon, also gilt jede kuenftige Vanilla-Aenderung
   an diesen Bloecken 1:1 mit), die Eintraege von `Contents`/`backpack_contents`/`GhostItems` als
   Item-Stapel, der aufsteigende Block als `minecraft:falling_block`, der Magnet-Filter als
   Item-Name. Nichts wird geraten: fuer aktuelle Daten (`from >= to`) laeuft nichts.
2. **Rucksack-Codec liest nachsichtig**: ein Eintrag, der nicht mehr dekodiert (entferntes Item),
   kostet nur diesen Eintrag (Warnung im Log), nicht den ganzen Rucksack.
3. **Baustab speichert Block-Namen statt Nummern**; alte Nummern halten einen laufenden Bau an.

## 4. Tests

- `WorldUpgradeTests` (alle Server-Ziele), acht Tests:
  - `fixturesAreWhatTwentySixTwoWrites` - auf 26.2: die Fixtures sind genau das, was der 26.2-Build
    schreibt (jede Datei-Angabe ist in einem frischen Schreiben vorhanden und gleich); auf 26.3:
    alle Fixtures da und mit Datenversion 4903.
  - `modBlockEntitiesSurviveTheUpgrade`, `modItemsSurviveTheUpgrade`, `modEntitiesSurviveTheUpgrade`,
    `playerDataSurvivesTheUpgrade`, `savedDataSurvivesTheUpgrade` - laden die 26.2-Fixtures, schicken
    sie durch den Server-Fixer (4903 -> aktuelle Version) und dann durch die Leser der Mod.
    **Orakel**: dieselben Vanilla-Stapel liegen in einer Vanilla-Truhe im selben Chunk; was Vanilla
    daraus macht, muss auch aus den Mod-Behaeltern herauskommen. So steht nirgends "auf 26.3 heisst
    die Karte so", und der Test gilt auch fuer 26.4.
  - `wandMidBuildFromAnOlderVersionStopsInsteadOfBuildingAShiftedBlock`,
    `backpackWithAnUnreadableEntryKeepsTheRest` - die zwei toleranten Leser.
- Fixtures neu schreiben (nur noetig, wenn sich ein 26.2-Speicherformat absichtlich aendert - dann
  die alte Datei zusaetzlich behalten, 26.3 muss auch sie lesen):

  ```
  SIMPLEBUILDING_WRITE_UPGRADE_FIXTURES=1 ./gradlew.bat :runGametest -PgametestFilter="simplebuilding:world_upgrade_*"
  ```

  (Bash; unter PowerShell `$env:SIMPLEBUILDING_WRITE_UPGRADE_FIXTURES=1` davor. Der Gradle-Daemon
  reicht die Umgebung an den Testserver durch.)
- Lesen pruefen: `python tools/testrunner/run.py --targets fabric-263,neoforge-263 --filter "simplebuilding:world_upgrade_*"`.

## 5. Echte Welt pruefen (von Hand)

Die Fixtures decken dieselben Pfade ab wie eine echte Welt (Chunk-, Entity- und Spieler-Fixer des
Servers); wer es trotzdem an einer echten Welt sehen will:

1. 26.2-Server starten: `./gradlew.bat runServer` (Fabric 26.2, Ordner `run/`), `eula=true` in
   `run/eula.txt`, einloggen oder in der Konsole:
   ```
   /setblock 0 -60 0 simplebuilding:enderite_hopper{Items:[{Slot:0b,id:"minecraft:ocean_explorer_map",count:1}]}
   /setblock 1 -60 0 simplebuilding:enderite_backpack{Color:3368362,Contents:[{slot:0,id:"minecraft:decorated_pot",count:1,components:{"minecraft:pot_decorations":["minecraft:angler_pottery_sherd","minecraft:brick","minecraft:brick","minecraft:skull_pottery_sherd"]}}]}
   /give @p simplebuilding:ore_detector
   /stop
   ```
   (Erzdetektor mit Schleich-Rechtsklick auf einen Block kalibrieren, Oktant-Auswahl setzen,
   Rucksack fuellen, einen Sandblock mit Schwebe-Effekt aufsteigen lassen und vor dem Aufschlag
   `/stop`.)
2. Welt kopieren: `run/world` nach `mc26_3/fabric/run/world` (bzw. den NeoForge-Laufordner).
3. 26.3-Server starten: `./gradlew.bat :mc26_3:fabric:runServer`, dann
   `/data get block 0 -60 0 Items` (Karte heisst jetzt `minecraft:ocean_monument_map`) und
   `/data get block 1 -60 0 Contents` (Krug mit `pot_decorations` als Karte).

Vor jedem Upgrade einer echten Welt: **Sicherung**. Und zuerst den Mod-Build mit diesem Fix
installieren, dann die Welt in 26.3 oeffnen - ein Chunk, der einmal ohne den Fix geladen und
gespeichert wurde, traegt schon die Datenversion 5023; seine Mod-Behaelter werden danach nicht mehr
nachgebessert (was dabei nicht dekodierte, ist weg).

## 6. Was Spieler tun muessen

- 26.3-Build der Mod (und Fabric API/NeoForge fuer 26.3, Cloth Config 26.3) installieren, Welt
  sichern, Welt oeffnen. Sonst nichts.
- Ein Baustab, der beim Speichern gerade baute, bleibt stehen - einmal neu klicken.
- Rueckweg 26.3 -> 26.2 (**Downgrade**) wird von Vanilla nicht unterstuetzt (Minecraft warnt beim
  Oeffnen, Chunks mit neuerer Datenversion werden nicht zurueckgewandelt) und von der Mod ebenso
  wenig. Ein 26.3-Blockzustand (`id`/`properties`) im aufsteigenden Block, eine umbenannte Karte im
  Rucksack usw. liest 26.2 nicht. Nur ueber eine Sicherung zurueck.

## 7. Grenzen

- Mod-Daten, die nicht durch `DataFixTypes#update` gehen, werden nicht aktualisiert: Befehle mit
  altem NBT, Datenpakete/Strukturdateien anderer Mods im alten Format, von Hand editierte Daten.
- Gegenstaende von **anderen** Mods, die in einem SimpleBuilding-Behaelter liegen, laufen durch den
  Vanilla-Fixer - deren eigene Komponenten repariert nur die jeweilige Mod.
- Forge bleibt auf 26.2; der Upgrade-Pfad gilt fuer Fabric und NeoForge.
- 1.21.11 -> 26.x ist ein anderes Thema (eigene Quelltexte) und hier nicht behandelt.
