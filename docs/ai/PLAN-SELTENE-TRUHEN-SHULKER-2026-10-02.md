# Plan: Bessere Truhen in Strukturen + seltene verstärkte Shulker (2026-10-02)

Queue: `.claude/QUEUE.md` – „Bessere Truhen“ und „seltene verstärkte Shulker in End-Städten“. Branch `claude-loot`.
Flag: `McVersion.RARE_STRUCTURE_FINDS` (26.3 true, 26.2 false). Alles serverseitig, Server-Config mit Obergrenzen.

## A) Bessere Truhen (je 1 %, Obergrenze 5 %)
- Neu `util/BetterChests.java`: Loot-Tabelle → Stufe
  (Festung corridor/crossing/library → Verstärkt; Bastion ×4 + nether_bridge → Netherit; end_city_treasure → Enderit).
- Würfel deterministisch aus Weltseed + Position (beide Hälften einer Doppeltruhe kommen zum selben Ergebnis, auch wenn
  sie in verschiedenen Chunk-Durchläufen entstehen). Einzeltruhe: `roll(pos)`. Doppeltruhe: `roll(erste) && roll(zweite)`
  = Besitzerregel „erste Hälfte trifft, zweite neu mit 1 %, sonst beide normal“.
- Haken (Mixins in `common/src/shared/java/com/simplebuilding/mixin`, Ziele in 26.2 und 26.3 gleich, per javap geprüft):
  1. `StructurePiece#createChest(ServerLevelAccessor, …)` RETURN → Festung, Netherfestung.
  2. `EndCityPieces$EndCityPiece#handleDataMarker` TAIL bei „Chest“ → End-Stadt und End-Schiff.
  3. `StructureTemplate#processBlockInfos` RETURN → Bastion (Truhen tragen `LootTable` im NBT der Vorlage).
- Ersetzen: Blockzustand `withPropertiesOf` auf die Stufen-Truhe, Loot-Tabelle + Seed übernehmen.
- Inhalt „doppelt“: `TieredChestBlockEntity#unpackLootTable` füllt die Tabelle zweimal (zweiter Seed abgeleitet).
  Entscheidung: doppelter Loot (nicht höherstufig) – einfach, für jede Tabelle gleich, im Code belegbar.
- Config `server.loot.betterChestPercent` 1,0 (0..5).

## B) Seltene Shulker
- `util/RareShulkers.java`: Stufe als Max-Health-Attributmodifikator (`simplebuilding:reinforced_shulker` +0,5 ×Basis =
  1,5×, `simplebuilding:enderite_shulker` +2,0 = 3×). Wird gespeichert und zum Client synchronisiert (Max-Health ist
  syncable) → keine eigene Sync-Logik.
- Spawn: `@ModifyArg` auf `addFreshEntity` in `handleDataMarker` (nur Shulker der End-Stadt „Sentry“).
  Würfel: r < enderit% → Enderit, r < enderit%+verstärkt% → verstärkt. Config `server.loot.reinforcedShulkerPercent`
  2,0 (0..10), `server.loot.enderiteShulkerPercent` 0,5 (0..5).
- Nachtrag Besitzer: je seltenem Shulker 4 Endermiten (`server.loot.endermitesPerRareShulker` 4, 0..8).
  Entscheidung: nicht schon in der Weltgenerierung (Vanilla entfernt Monster fern jedes Spielers und Endermiten nach
  2 min – sie wären weg, bevor jemand ankommt), sondern einmalig, sobald ein Spieler (nicht Kreativ/Zuschauer) näher
  als 24 Blöcke kommt: Entity-Tag `simplebuilding.endermite_escort` (gespeichert) + Mixin `Shulker#tick`.
  Sichere Plätze: fester Boden, keine Flüssigkeit, keine Kollision, Umkreis 3. Nicht dauerhaft → nicht farmbar.
- Drops: `Mob#dropCustomDeathLoot` TAIL, nur Shulker mit Stufe: 0–2 Schalen der eigenen Stufe (zusätzlich zur Vanilla-Schale).
- Hülle: Client-Mixin `ShulkerRenderer` (extractRenderState TAIL → Stufe in den Render-State per Duck-Interface,
  getTextureLocation HEAD → `simplebuilding:textures/entity/shulker/<stufe>.png`). Wiederverwendung der vorhandenen,
  abgenommenen Stufen-Shulkerkisten-Texturen (gleiches 64×64-Layout inkl. Kopf). Gefärbte Stufen-Shulker zeigen die
  ungefärbte Stufentextur (Einschränkung).
- Items: `reinforced_shulker_shell`, `netherite_shulker_shell`, `enderite_shulker_shell` (Texturen per
  `tools/textures/shulker_shell_textures.py` aus Vanilla-Schale + Plattierungsfarben der Stufen-Shulkerkisten).
  Vorschau `previews/seltene-shulker-vorschau.png`.
- In-World: Schalen (Vanilla + Stufen) kommen in `simplebuilding:placeable_small` (Schleich-Rechtsklick legt ab).
  Rechtsklick (ohne Schleichen) auf das Häufchen mit Nugget: `PlacedSmallPartsBlock#useItemOn` →
  `ShulkerShells.upgrade`: Kette Vanilla→verstärkt (Eisen-Nugget), verstärkt→Netherit (Netherit-Nugget),
  Netherit→Enderit (Enderit-Nugget); genau ein Nugget je Schale, die zuletzt gelegte passende Schale wird ersetzt.
  Annahme: Stufenkette (jeweils eine Stufe), damit Netherit-Schale nötig bleibt.
  Hinweis: `TransformTargets` (useItemOn-Abschnitt) fragt dieselbe Methode `ShulkerShells.canUpgrade`.
  Wiki/JEI: Abschnitt `shellUpgrade` in `InWorldTransformations`, Kind `SHELL_UPGRADE` in JEI/REI, `wiki/generate.py`
  Kind `shell_upgrade`, Prosa in `wiki/manual.json`.
- Rezept zusätzlich (Datagen, formlos): Kupfertruhe (Tag) + Stufen-Schale + Shulkerschale → Stufen-Shulkerkiste.

## Dateien
McVersion (2×), ModItems, ModItemTagProvider, ModModelProvider, ModRecipeProvider, ServerTuningConfig/ConfigOptionTests,
Lang (4 Dateien), Mixins + JSONs, TieredChestBlockEntity, PlacedSmallPartsBlock, TransformTargets,
InWorldTransformations, InWorldRecipeCatalog, JEI/REI-Kategorie, wiki/generate.py, wiki/manual.json, docs/CONFIG.md,
Gruppen/Suche je nach Integritätstests.

## Risiken
- Worldgen-Mixins: Fehler würden Strukturgenerierung brechen → Code defensiv (nur Vanilla-CHEST, nur bekannte Tabellen).
- Mixin `require=1`: Ziele in 26.2/26.3 identisch (javap).
- Echte Strukturgenerierung (Festung/End-Stadt) nicht im GameTest – nur die Bastion-Vorlagenroute (öffentliche
  statische Methode) und die gemeinsamen Funktionen.

## Verifikation
GameTests `RareStructureFindsTests`: Würfel/Tabelle, Doppeltruhenregel, processBlockInfos ersetzt Bastion-Truhe,
doppelter Loot, Shulker-Stufe/Leben/Drops/Config-Obergrenzen, Schalen-Aufwertung (je Nugget, falsches Nugget,
Hinweis), Rezept. Gates: Datagen, `fabric-263`, `neoforge-263`, 26.2-Compile, Forge-26.3-Compile, Texturen-/Wiki-Check.
