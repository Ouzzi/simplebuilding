# Plan b-s8b3-storage (21.10.2026): Familienstufe 8, Teil 8b-3 – seltene Funde und Shulker-Hüllen

Branch `b-s8b3-storage` (von wave1 a37b19f8b = 8a + 8b-1 + 8b-2 + 8c-1 + 8c-2), Sitzung B, Queue-Zeile 94 („8b-3 [~]“).
Grundlage: `PLAN-B-S8C-STORAGE-2026-10-20.md` §7 (Zuschnitt), Vorgehen wie 8b-2 (`PLAN-B-S8C-STORAGE-2026-10-20.md`, nur 26.3:
Klassen ziehen ganz um), `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`.

## 1. Ist-Zustand (git grep, Stand a37b19f8b)

- **Nur 26.3:** alles hängt an `McVersion.RARE_STRUCTURE_FINDS` (26.2 `false`, Items dort `null`, Mixins prüfen das Flag) →
  Muster 8b-2: SB-Klassen und -Dateien ziehen ganz um, 26.2 verliert nichts.
- **Bessere Truhen** `util/BetterChests` (Loot-Tabelle → Stufe, Würfel aus Weltseed/Position, Doppeltruhen-Regel) mit Mixins
  `StructurePieceBetterChestMixin` (Festung/Netherfestung), `StructureTemplateBetterChestMixin` (Bastion), `EndCityPieceMixin`
  (End-Stadt/-Schiff, auch Wachposten-Shulker). Doppelte Beute macht schon die Modul-`TieredChestBlockEntity` (8b-1).
- **Seltene Shulker** `util/RareShulkers` (Stufe = Lebens-Modifikator `simplebuilding:<stufe>_shulker`, Entity-Tags
  `simplebuilding.upgraded_shulker` / `simplebuilding.endermite_escort`, Endermiten-Begleitung, Aufwerten lebender Shulker mit
  Klumpen, 0–2 Schalen beim Tod) mit Mixins `MobShulkerShellDropMixin`, `ShulkerEscortMixin`, Client `ShulkerRendererMixin` +
  `ShulkerRenderStateMixin` / `client/render/TieredShulkerRenderState` (Hülle = Stufen-Shulkerkiste mit Vanilla-Kopf,
  `textures/entity/rare_shulker/<stufe>.png`, Vanilla-abgeleitet).
- **Stufen-Schalen** `ModItems.{REINFORCED,NETHERITE,ENDERITE}_SHULKER_SHELL` + `util/ShulkerShells` (Kette Schale + Klumpen →
  nächste Schale; Aufwerten der **abgelegten** Schale auf SBs Häufchen `PlacedSmallPartsBlock`, Handhinweis `TransformTargets`,
  Wiki/JEI `InWorldTransformations`/`InWorldRecipeCatalog` „shellUpgrade“), Rezepte `<stufe>_shulker_box_from_shells`
  (`#simplebuilding:copper_chests` + Stufen-Schale + Shulkerschale → Modul-Shulkerkiste), Tags `placeable_small`,
  `enderite_items`, `enderite_ingot_tier`, Tab-/Suchtab-Zeilen, Item-Modelle (26.3-generated), Vanilla-abgeleitete Item-Texturen
  (`shulker_shell_textures.py`), Lang, Fragentisch, Wiki (Feature `rare_structure_finds`, Notiz `*_shulker_shell`), Discover.
- **Config** `server.loot.{betterChestPercent, reinforcedShulkerPercent, netheriteShulkerPercent, enderiteShulkerPercent,
  endermitesPerRareShulker}` in SBs `ServerTuningConfig` (gemeinsame Klasse beider Linien, Obergrenzen 5/10/5/5/8).
- Tests `RareStructureFindsTests` (8 Tests) + Fabric-Adapter `RareStructureFindsGameTest`, Tab-/Datenteile in `DataIntegrityTests`.
- **Shulker-Deckel** (`ShulkerLids`, Komponente `simplebuilding:shulker_open`, `ShulkerOpenStateMixin`/`ShulkerOpenComponentsMixin`,
  Teil von `ShulkerBoxBlockMixin`, offene Item-Modelle der 17 Vanilla-Kisten): **beide Linien**, nur Vanilla-Shulkerkisten (N17).

## 2. Entscheidungen (selbst getroffen)

- **E1 Umfang:** bessere Truhen, seltene Shulker (inkl. Begleitung, Aufwerten lebender Shulker, Hülle), Stufen-Schalen samt
  Rezepten, Config → `simplestorage`. Begründung: alles erzeugt bzw. verbraucht Lager-Stufen (Truhen, Shulkerkisten); die Hülle ist
  die Textur der Modul-Shulkerkiste.
- **E2 Bleibt SB (begründet):** (a) **Shulker-Deckel** – Vanilla-Verhalten der Vanilla-Kiste auf beiden Linien, unabhängig von den
  Stufen; gehört später eher zu Simple Tweaks (Folgepunkt Stufe 9). (b) **Aufwerten der abgelegten Schale** – Mechanik von SBs
  Häufchen (`PlacedSmallParts`, Handhinweis, Wiki/JEI-In-World-Katalog): `ShulkerShells` bleibt in SB, nennt die Schalen per Id
  (`StorageRefs.shulkerShell`). Ohne SB gibt es die Schalen nur als Shulker-Beute (Modul allein spielbar: Rezepte und Funde
  brauchen nur Vanilla + SimpleLib). (c) `ServerTuningConfig`-Felder `server.loot.*` bleiben (gemeinsame Klasse, 26.2), wirken
  auf 26.3 nicht mehr → Ausblenden in Stufe 9 (wie `features.backpack`).
- **E3 Modul-Code** `com.simplestorage.rare.{BetterChests, RareShulkers, ShulkerShells}` (ohne SB-Import; Kette mit SimpleLib-
  Klumpen), Mixins `com.simplestorage.mixin.{StructurePieceBetterChestMixin, StructureTemplateBetterChestMixin, EndCityPieceMixin,
  RareShulkerMobMixin, ShulkerEscortMixin}`, Client `client.{ShulkerRendererMixin, ShulkerRenderStateMixin}` +
  `com.simplestorage.client.TieredShulkerRenderState`. Items in `StorageItems` (Netherit selten/feuerfest, Enderit episch).
- **E4 Alte Welten:** IdMoves ITEM der 3 Schalen, RECIPE + Rezept-Advancement (`recipes/decorations/`) der 3 Schalen-Rezepte.
  Lebens-Modifikator- und Tag-Namen sind keine Registry-Ids: das Modul schreibt `simplestorage:*`/`simplestorage.*`, liest die
  alten `simplebuilding:*`/`simplebuilding.*` mit (Migrationstest mit altem Shulker).
- **E5 Config** `config/simplestorage.json` bekommt die 5 Werte (gleiche Standards/Obergrenzen); `fromSimpleBuilding` übernimmt
  zusätzlich `server.loot.*` aus `simplebuilding.json` (nur beim ersten Anlegen der Datei, wie bisher).
- **E6 Daten:** Item-Modelle/-Definitionen ins Modul (Namensraum umgeschrieben), Rezepte mit `#simplestorage:copper_chests`,
  Vanilla-abgeleitete Texturen (3 Schalen, 3 Hüllen) über `MODULE_OUTPUTS['simplestorage']` (Generator `shulker_shell_textures`
  mit Modul-Pfaden), SB-`OUTPUTS` ohne sie; `.gitignore` nachziehen. SB-Tags (`placeable_small`, `enderite_items`,
  `enderite_ingot_tier`) nennen die Modul-Schalen per `StorageRefs.itemKey`. Lang EN/DE und Fragentisch ins Modul, SB-Lang ohne
  die Schalen (26.2 nutzt sie nie). Wiki: Feature `rare_structure_finds` + Notiz `*_shulker_shell` ins Modul-Wiki; SBs In-World
  „shellUpgrade“ bleibt (Ids kommen aus `ShulkerShells`).
- **E7 Tabs:** Modul-Tab hinter den Stufen-Shulkerkisten die 3 Schalen; allein im Vanilla-Zutatentab hinter der Shulkerschale;
  mit SB stellt SB sie per Id (Lager-Zeile und Suchtab wie bisher).
- **E8 Tests:** 7 Tests (bessere Truhen, Bastion, End-Stadt, seltene Shulker, Begleitung, lebende Shulker, Rezepte + Config) als
  Modul-`RareFindsTests` + `rare_finds_registered` + `rare_finds_migration_from_simplebuilding`; SB behält
  `placedShellsUpgradeOneTierPerNugget` (Häufchen, per Id).

## 3. Umsetzung (Commits)

1. Plan. 2. Modul: Code, Mixins, Registrierung, Config, IdMoves, Ressourcen, Texturen-Hook, Tests (3 Loader-Adapter).
3. SB: Klassen/Mixins/Items/Lang/Datagen/generated raus, `ShulkerShells` per Id, Tabs/Suchtab/DataIntegrity/Tests, Wiki.
4. sb-test: Compile, Tests, Datagen 26.3, Guides/Wiki/Discover (`-gen`), `check`.

## 4. Risiken

- Doppelte Wirkung: SB-Mixins müssen weg, sonst würfeln Truhen/Shulker zweimal (beide Mixin-Sätze entfernt bzw. nur im Modul).
- Fabric-Reihenfolge Items: Schalen nach den Shulkerkisten registrieren (Tab-Reihenfolge).
- Hülle nur per Compile prüfbar (keine Client-Läufe erlaubt); Vanilla-abgeleitete Sprites nie einchecken (`git status` nach Wiki).

## 5. Prüfung (sb-test)

Compile 26.3 fabric(+gametest)/neoforge/forge + 26.2 `:compileJava`, Modul fabric/neoforge/forge; module-simplestorage
fabric/neoforge/forge/standalone-fabric/standalone-neoforge; fabric-263 + neoforge-263 gezielt (`rare_structure*`,
`data_integrity*`, `tiered_shulker*`, `config_option*`, `server_tuning*`); integration-263; Datagen 26.3; Guides/Wiki/Discover;
`./gradlew check`. Nicht geprüft (Auftrag): Client-Läufe (Hülle, Tab).

## 6. Stand und Abweichungen

Umgesetzt (be88eb235) wie geplant: Modul `com.simplestorage.rare.{BetterChests, RareShulkers, ShulkerShells}`, Mixins
`StructurePieceBetterChestMixin`, `StructureTemplateBetterChestMixin`, `EndCityPieceMixin`, `RareShulkerMobMixin`,
`ShulkerEscortMixin`, Client `ShulkerRendererMixin`/`ShulkerRenderStateMixin` + `client/TieredShulkerRenderState`; 3 Schalen in
`StorageItems` (Tab hinter den Stufen-Shulkerkisten, allein Vanilla-Zutatentab hinter der Shulkerschale, Teststation);
`StorageConfig` + 5 Werte (Obergrenzen, NaN → Standard, Übernahme aus `server.loot.*`); IdMoves (3 Items, 3 Rezepte +
Freischaltungen); Ressourcen per `git mv` (Item-Modelle/-Definitionen, Rezepte mit `#simplestorage:copper_chests`,
Freischaltungen), Lang EN/DE, Fragentisch (Text „mit SimpleBuilding“ beim Aufwerten), Wiki-Feature `rare_structure_finds` +
Notiz `simplestorage:*_shulker_shell`, `check_data.py`; Texturen über `MODULE_OUTPUTS['simplestorage']` + Generator
`shulker_shell_textures` (Hülle aus der Modul-Shulkerkisten-Textur), `.gitignore` (alte SB-Zeilen bleiben zur Sicherheit).
Modul-Tests `RareFindsTests` (9: 7 portiert + `rare_finds_registered` + `rare_finds_migration_from_simplebuilding`), Fabric-Adapter.
SB: Klassen/Mixins/Items/Lang/Datagen (Modelle, Rezepte) raus, `ShulkerShells` per `StorageRefs.shulkerShell`, Tabs/Suchtab/
DataIntegrity/Tags per Id, `RareStructureFindsTests` nur noch Häufchen-Test, `docs/MODS-UND-FEATURES.md`.

Belege: (ausstehend, siehe unten)

Abweichungen (bewusst):
- Shulker-Deckel bleiben SB (E2a), Aufwerten der abgelegten Schale bleibt SB (E2b), SB-Felder `server.loot.*` bleiben (E2c, Stufe 9).
- Neuer Name `RareShulkerMobMixin` statt `MobShulkerShellDropMixin` (Inhalt gleich: Beute + Aufwerten lebender Shulker).
- Modul schaltet Weltfunde und Aufwerten lebender Shulker mit dem Super-Mod-Schalter ab (`SimpleStorage.on()`), Beute bleibt.
