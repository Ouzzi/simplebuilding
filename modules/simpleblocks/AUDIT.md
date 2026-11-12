# Audit simpleblocks (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplestorage/AUDIT.md`.
Umfang: 47 Java-Dateien / 4,6k Zeilen (ohne Tests), 1 Menue (`ShapecutterMenu`), 2 Block-Entities (`SnowloggedBlockEntity`, Entity `LevitatingBlockEntity`
als Fall-Entity), 5 Mixins, **1 Payload** (S2C `Sawing.Payload`, kein C2S), 2 Fabric-`UseBlockCallback`s. Registrierung, Rezepte und Texturen sind
generiert und per `check_data.py` belegt (1847 Dateien aktuell).

## Vollstaendig gelesen
- Shapecutter: `saw/ShapecutterMenu.java` (gegen Vanillas `StonecutterMenu` verglichen: Slots, `clickMenuButton`, `quickMoveStack`, `onTake`, `removed`),
  `saw/ShapecutterBlock.java`, `saw/Sawing.java` (Rezeptliste, Payload), `saw/SawingRecipe.java`, `saw/SawUpgrade.java`, `saw/SawPalette.java`,
  Registrierung/Haken in `BlocksFabric.java` + `BlocksFabricClient.java`.
- Schnee: `snow/SnowloggedBlock.java` + `SnowloggedBlockEntity.java`, `snow/Snowlogging.java`, `snow/SnowCaps.java`, `snow/SnowCapBlock.java`,
  `snow/FrostGrassBlock.java`, `snow/SnowBlocks.java`, `snow/SnowConfig.java`.
- Natur: `nature/LevitatingBlock.java` + `LevitatingBlockEntity.java`, `nature/FallingSlabBlock.java`, `MergingSlabBlock.java`, `GrassSlabBlock.java`,
  `CrackedIceBlock.java`, `CrackedHardIceBlock.java`; `pale/PaleBlocks.java`, `PaleConfig.java`, `PalePumpkinBlock.java`; `compact/CompactBlocks.java`,
  `CollapsedEffect.java`, `CollapsedConfig.java`, `compact/client/CollapsedAlert.java`; `BlocksLoot.java`, `SimpleBlocks.java`; die 5 Mixins.
- Stichproben: `BlocksRegistry.java`, `nature/NatureBlocks.java` (nur auf eigene Block-Logik), `saw/client/ShapecutterScreen.java` (nur: Klick = `handleInventoryButtonClick`).
- Nicht gelesen: `discover/BlocksDiscover.java`, `guide/BlocksGuide.java` (generiert), `compat/jei/*`, `snow/client/SnowloggedRenderer.java`, `nature/IceShapes.java`, `NatureIds.java`, `saw/SawTests.java`.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk-Payloads | Nur S2C `Sawing.Payload` (Liste aller Stonecutting- und Sawing-Rezepte) beim Beitritt und nach `/reload`, nur an Clients mit Kanal. Derzeit rund 925 Eintraege (351 Vanilla + 574 Mod-Rezepte), etwa 75 KB bei 1 MiB Limit; keine Obergrenze (P3-2). Kein C2S: ein Klick im Menue ist Vanillas `ServerboundContainerButtonClick`, der Server prueft den Index gegen **seine** Liste (`clickMenuButton`: `button >= 0 && < cuts.size()`). |
| C2 Server-Autoritaet / Reichweite | Ergebnis immer serverseitig (`setupResult`); `stillValid` prueft Block und Abstand (`stillValid(access, player, SAW)`). Wiedereintritt: `Sawing.serverEntries` ist `synchronized` und je Rezept-Manager gecacht. Client-Klassen nur in `client/`. |
| C3 Dupes / Verlust | Menue entspricht dem Vanilla-Stonecutter: Ergebnisplatz `mayPlace` false, `onTake` nimmt genau 1 Eingabe, `quickMoveStack` gleiche Indizes/Rueckgaben, `removed` gibt die Eingabe zurueck. Schnee: Schicht verbraucht (`stack.consume`), Held-Block droppt seine Loot-Tabelle, die Schnee-Beute nur mit Schaufel (nie doppelt, `playerDestroy` stellt den Held-Block wieder hin ohne ihn zu droppen). Zusammenfuehren zweier Stufen = 1 Block (Rezept 3 Bloecke -> 6 Stufen wie Vanilla). Aufsteigender Block: wie `FallingBlockEntity` (Drop nur einmal, 600-Tick-Notbremse). Keine Container, keine Hopper-Seiten. |
| C4 NBT/Komponenten | `SnowloggedBlockEntity`: unlesbarer/leerer Held-Zustand = reiner Schnee; `LAYERS` 1..7 durch Zustand begrenzt. Config geklammert (1..100, Wahrscheinlichkeit 0..1). Block-State-Komponente am Rissigen Eis. |
| C5 Tick-Kosten | Zufallsticks wie Vanilla (Gras-/Erdstufe, Frostgras, Schneekappe, Schneezelle); `SnowCaps.tick` einmal je Chunk bei Schnee; `CollapsedEffect.tick` alle 20 Ticks ueber 41 Plaetze je Spieler; Rissiges Eis plant einen Tick je Block unter Lebewesen (Entity-Suche 1 Block). |
| C6 Leaks | Nur `Sawing.cached/clientEntries` (bei Stopp/Trennen geleert: `PlayerState.onStop`, `DISCONNECT`). |
| C7 Sicherheit/Rechte | Die zwei `UseBlockCallback`s (Shapecutter-Aufwertung, Schneezelle) pruefen `mayBuild`/`mayUseItemAt`; **P2-1: es fehlte die Protection-Abfrage, gefixt.** Mixins: `SnowCapsMixin` (ServerLevel#tickPrecipitation, Ausstieg ohne Schnee), `GrassFreezesMixin` (nur `Blocks.GRASS_BLOCK`), `FenceConnectsSnowloggedMixin` (nur SnowloggedBlock), `PaleGolemHeadMixin` (nur Tag `golem_heads`), `CrackedIceFacesMixin` (Rendern); alle laufen ohne Config-Schalter nur im Vanilla-Pfad. |
| C8 Unabhaengigkeit | allein spielbar (`depends` nur simplelib; Standalone-Ziel vorhanden); alle Hilfsmods nur bedingt (Rezepte mit Lade-Bedingung). |
| C9 Doku/Pflege | Keine TODO/FIXME; Lang EN=DE, `check_data` gruen; 2 Kern-Dubletten (`LevitatingBlock`, `LevitatingBlockEntity`). Ein britisches "colour" im Handbuch-Text (P3-4). |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simpleblocks-P2-1 | P2 (Schutz) | Die beiden Fabric-`UseBlockCallback`s in `BlocksFabric.java:30-35` aendern die Welt (`SawUpgrade#tryUpgrade`/`tryMetal`: Stonecutter -> Shapecutter bzw. Metall-Finish, `Snowlogging#use`: Pflanze/Zaun -> Schneezelle) und pruefen nur `mayBuild`/`mayUseItemAt`; das Modul fragte die Framework-`Protection` (Claims) nie. Ein Callback antwortet vor Vanillas `useItemOn`; ob Simple Tweaks' Claims-Haken (`ClaimGameModeMixin`, HEAD von `useItemOn`) davor oder dahinter laeuft, haengt von der Mixin-Reihenfolge ab (nicht geprueft). Alle anderen Simple-Mods mit solchen Callbacks (Gadgets, Tools, Hammers, Chisels) fragen `Protection` selbst. | **gefixt**: neue Klasse `BlocksProtection#mayBuild` (Claims `BUILD` + Partner-Erlaubnisse), aufgerufen in `SawUpgrade.java:59,80` und `Snowlogging.java:40`. Gametests `saw_upgrade_respects_protection`, `snowlogging_respects_protection` (ein Anbieter, der alles verweigert, laesst Block, Nuggets, Platten und Schnee unberuehrt). Ohne Claims-Anbieter aendert sich nichts. |
| simpleblocks-P3-1 | P3 (Design) | Die Verlangsamung der Collapsed-Bloecke zaehlt nur Stapel im Spielerinventar (`CollapsedEffect.java:28`); in Shulkerkiste, Rucksack oder Buendel verstaut gibt es keine Verlangsamung. | Besitzer-Frage (gewollt?) |
| simpleblocks-P3-2 | P3 | `Sawing.Payload` hat keine Obergrenze (`Sawing.java:76`); bei rund 12 000+ Stonecutting-Rezepten (sehr grosse Modpacks) ueberschritte das Paket 1 MiB und trennte den Client. | Hinweis (wie `ChiselTables.MAX_PACKET_BYTES` kuerzen) |
| simpleblocks-P3-3 | P3 | `Sawing.accepts` (`Sawing.java:166`) durchsucht bei jedem Umschalt-Klick die ganze Liste (~1000 Eintraege); Vanillas `acceptsInput` nutzt eine Map. Nur Last pro Paket. | Hinweis |
| simpleblocks-P3-4 | P3 | Handbuch-Text mit britischer Schreibung "colour" (`questionary/en_us.json:37`, AGENTS 5); der Text steht auch in `wiki/data/simpleblocks.json:24817` (Generator). | Hinweis (beides zusammen aendern und `wiki/generate.py` laufen lassen, ausserhalb dieses Pakets) |
| simpleblocks-P3-5 | P3 | 2 Kern-Dubletten (`LevitatingBlock`, `LevitatingBlockEntity`): Fixes im 26.2-Kern nachziehen. | Liste |

## Urteil
**shipbar nach Fix** (0 P1, 1 P2 gefixt, 5 P3). Das Menue ist eine treue Kopie des Stonecutters; die Fall-Entity und die Schneezellen sind dupe-frei.

## Testergebnis
Lokal: javac der geaenderten Dateien (`BlocksProtection`, `SawUpgrade`, `Snowlogging`, `SawTests`, `SnowTests`, `BlocksTests`, `ModuleGameTest`) gegen 26.3 + Fabric-API: nur die bekannten Access-Widener-Meldungen in nicht geaenderten Dateien (`BlocksRegistry`, `Sawing`, `LibMaterials`), `modules/simpleblocks/tools/check_data.py` gruen, `tools/check_gametest_adapters.py` OK, `check_dup_impl` 0 Probleme.
sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`. Nicht getestet: die neuen Gametests selbst (kein Gradle), Mixin-Reihenfolge Claims gegen Fabric-Callbacks, Client, Forge/NeoForge.
