# Plan A-STORAGE30 (30.10.2026): Rohr (ST27-1) und Kisten-Netzwerk-Prototyp (BS29b-3), Simple Storage

Agent a-storage30, Branch `a-storage30` (Basis claude-wave1), WP-26. Nur MC 26.3 Fabric. Wortlaut: `.claude/QUEUE.md` ST27-1, BS29b-3.

## 1. Ist-Stand

- Simple Storage (`modules/simplestorage`, Pflicht-Sub-Mod, braucht nur `simplelib`): Stufenkisten `ChestTier` (Verstärkt 36 / Netherit 45 / Enderit 54 Plätze,
  Stapel x1/x2/x4) als `TieredChestBlock` + `TieredChestBlockEntity extends ChestBlockEntity`, Renderer `TieredChestRenderer` (Sprites je Stufe, Fallen-Variante),
  Aufwertung in der Welt über SimpleLib `InWorldUpgrades` (`data/simplestorage/in_world_upgrades/*.json`, Handler je Zielblock, Klick `StorageUse.upgradeClick`).
- Mod-Trichter (Verstärkt/Netherit/Enderit, Filter nach Filter-Prinzip) liegen in SimpleBuilding (`common/src/shared/.../ModHopperBlockEntity`, `ItemFilter`);
  ein Modul darf sie nicht benutzen (Modul-Unabhängigkeit) -> das Rohr bekommt eigene Logik; die Filterregeln gehen als `ItemFilterRules` nach SimpleLib
  (Filter-Prinzip: „gemeinsamer Code in simplelib“), der Knopf ist schon `UiFilterButton`.
- UI-Bausteine: SimpleLib `UiBoxes/UiPalette/UiSymbols/UiFilterButton` (Kasten-Stil je Stufe), wie `TieredChestScreen`.
- Tests: Module-Tests in `shared/.../test/*Tests.java`, Katalog `StorageTests.ALL` + `@GameTest` im Fabric-Adapter `ModuleGameTest` (beides nötig).

## 2. Ergebnis (Umfang)

### 2a. Rohr (ST27-1)
- Vier Stufen als eigene Blöcke `pipe`, `reinforced_pipe`, `netherite_pipe`, `enderite_pipe`, aufwertbar wie die Kisten (Hammer/Axt + Material, `in_world_upgrades/pipes.json`,
  Handler behält Item, Filter, Ausgang) und per Handwerk (Rezepte Stufe -> Stufe).
- Block `ItemPipeBlock extends net.minecraft.world.level.block.PipeBlock` (Vanilla-Armformen): Zustand `north..down` = Arm vorhanden (Eingang an Behälter/Rohr
  oder der Ausgang), `facing` = der EINE Ausgang. Unverbunden = nur Mittelstück (Kabel-Optik). Eingang aus jeder Richtung; Ausgang mit Pfeil-Design.
- BlockEntity `ItemPipeBlockEntity` (1 Platz, `WorldlyContainer`): Takt je Stufe (8/6/4/2 Ticks), Menge je Takt (1/1/2/4), schiebt in den Behälter am Ausgang
  (`HopperBlockEntity.addItem`), zieht aus Behältern an den anderen Seiten (reihum, nie aus Rohren: sie schieben selbst). Basis-Ausgang nur unten/seitlich, ab Verstärkt auch oben.
- Ausgang umstellen (Entscheidung Agent): Schleichen + Rechtsklick mit leerer Hand dreht den Ausgang zur nächsten erlaubten Seite (Aktionsleiste nennt sie),
  im Fenster dieselbe Funktion als Knopf. Kein Schraubenschlüssel (es gibt keinen in Simple Storage).
- Fenster `PipeMenu/PipeScreen`: 1 Platz, Filter-Knopf (Aus / Exakt / Gleiche Art, `UiFilterButton`), Ausgangsknopf. Filter-Prinzip: im Filtermodus bleibt das
  echte Item im Platz, erst das zweite und weitere wird weitergeschoben; Automatik füllt einen leeren Filterplatz nie, nimmt nur Passendes.

### 2b. Kisten-Netzwerk-Prototyp (BS29b-3)
- `copper_cable` (Kupferkabel, auch Dekoblock: verbindet in alle 6 Richtungen, Säulen/Linien), `chest_interface` (Interface-Platte, sitzt in der Luft an einer Kistenseite,
  `facing` = Richtung zur Kiste, verbindet sich an den anderen 5 Seiten mit Kabel/Interface), vier Controller-Kisten `wooden_/reinforced_/netherite_/enderite_controller_chest`.
- Controller = `ControllerChestBlock extends TieredChestBlock` (eigener BE-Typ `controller_chest`, `ControllerChestBlockEntity extends TieredChestBlockEntity`), nie Doppelkiste;
  eigener Inhalt (36/45/54 Plätze) zählt zum Netz. Holz-Controller hat 36 Plätze (Abweichung: es gibt keine ChestTier-Stufe „Holz“, und eine Vanilla-Truhen-Unterklasse
  bräuchte Vanilla-Texturen im Repo). Grenzen (Kistenblöcke, eine Doppelkiste = 2): Holz 4 (= 2 Doppelkisten), Verstärkt 12, Netherit 32, Enderit 64. Mitglieder dürfen höchstens
  die Stufe des Controllers haben (Holz/Kupfer = 0, Verstärkt = 1, Netherit = 2, Enderit = 3); höhere werden ignoriert und im Fenster gezählt.
  Zu viele -> Controller „Kaputt“ (Bildschirm mit Ursache, kein Zugriff). Zwei Controller im selben Netz -> beide „Kaputt“ (Konflikt).
- Netz-Suche ereignisgetrieben: BFS über Kabel/Interface nur wenn die globale Netz-Version (hochgezählt von Kabel/Interface bei Setzen/Entfernen/Nachbaränderung) sich seit der letzten
  Suche geändert hat und ein Fenster geöffnet wird; kein Scan pro Tick. Kistenliste (je Interface ein Nachschlag) und Inhalt nur solange ein Fenster offen ist (alle 5 Ticks).
- Fenster `ControllerMenu/ControllerScreen`: durchsuchbare Raster-Ansicht aller Sorten (Summe je Sorte, Scrollen, Sortierung Menge/Name), Klick nimmt, Klick mit Cursor-Item legt ein,
  Shift-Klick im Inventar legt ein; Kaputt-Bildschirm. Stil SB-Kasten (UiBoxes), kein AE-Klon.
- Rezepte (balanciert): Kabel 3 Kupferbarren Mitte, darüber/darunter je 1 Wolle -> 3 Kabel; Interface Glasscheibe Mitte, Gold/Quarz ringsum abwechselnd, oben Verstärker, unten Komparator -> 1;
  Controller formlos Interface + Kiste der Stufe + Buch und Feder (Holz: Vanilla-Truhe).
- Config `chestNetwork` (an): aus -> Kabel wird Kupfergitter, Interface Glasscheibe, Controller die normale Kiste seiner Stufe (Inhalt bleibt); Ersatz bei Nachbaränderung,
  Rechtsklick und Zufalls-Tick (spätestens nach einigen Minuten für unbesuchte Blöcke), Items lassen sich nicht mehr platzieren. `controllerLootPercent` (10): Chance, dass eine
  bessere Struktur-Truhe (Verstärkt/Netherit/Enderit, `BetterChests`) einen Controller der Stufe + Interface enthält.

### 2c. Optik (exemplarisch, Besitzer wählt)
- Eine Variante je Teil eingebaut (Empfehlung), Alternativen in `/root/sb-tools-a/previews/a-storage30-preview.html`. Texturen per Generator `/root/sb-tools-a/gen_storage30_textures.py` (Ebenen-Quellen `.layers.json`).

## 3. Dateien (Auszug)
`modules/simplelib/.../ItemFilterRules`, `modules/simplestorage/shared/java/com/simplestorage/{pipe,network}/*`, `registry/*`, `config/StorageConfig`, `client/{PipeScreen,ControllerScreen,TieredChestRenderer}`,
`fabric/.../StorageFabricClient`, Ressourcen (Blockstates, Modelle, Items, Loot, Rezepte, Advancements, in_world_upgrades, Tags, Lang, Texturen), Tests (`PipeTests`, `NetworkTests`, `StorageTests`, `ModuleGameTest`),
`tools/check_data.py`, Wiki `manual.json`, Handbuch, Discover, `docs/MODS-UND-FEATURES.md`.

## 4. Risiken
- Neue 26.3-API-Namen blind geschrieben: ein einziger sbt-Lauf am Ende; bei Compile-Fehlern wird nachgebessert (ein weiterer Lauf).
- Pfeil-Ausrichtung der Rohr-Modelle (UV-Drehung je Seite) nur in der Client-Sicht prüfbar -> in Memory vermerkt.
- NeoForge/Forge-Clients (Screens/Renderer) bewusst nicht angefasst (NUR 26.3 Fabric).

## 5. Verifikation
Ein `/root/sbt a-storage30`-Lauf: Compile `:mc26_3:fabric` + `:modules:simplestorage:fabric`, Targets `module-simplestorage-fabric-263` und `module-simplestorage-standalone-fabric-263`,
Generatoren (Handbuch, Wiki, Discover), `./gradlew check -q`. Lokal vorher: Python-Gates (check_data, Texturen-Ebenen, Lang, fehlende Texturen).
