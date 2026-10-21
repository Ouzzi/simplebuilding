# Plan b-s8c2-storage (21.10.2026): Familienstufe 8, Teil 8c-2 – Rucksäcke

Branch `b-s8c2-storage` (von wave1 6232d8abe = 8a + 8b-1 + 8b-2 + 8c-1), Sitzung B, Queue-Zeile 94 („8c-2 [~]“).
Grundlage: `PLAN-B-S8C-STORAGE-2026-10-20.md` §7, Vorgehen wie `PLAN-B-S8C1-STORAGE-2026-10-20.md` (SB-Klassen bleiben in
`common/src/shared` für 26.2, auf 26.3 registriert SB sie nicht – `McVersion.STORAGE_IN_SB` –, das Modul hat Kopien unter
`com.simplestorage.*`, SB greift per `StorageRefs`/Id zu), `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`.

## 1. Ist-Zustand (git grep, Stand 6232d8abe)

- 4 Rucksäcke `backpack`, `reinforced_/netherite_/enderite_backpack` = Item (`BackpackItem`, BlockItem, Brust-`EQUIPPABLE`) +
  Block (`BackpackBlock`, abstellbar) + BE `backpack` (`BackpackBlockEntity`), Komponente `simplebuilding:backpack_contents`
  (`BackpackContents`, Stapel bis 396), `BackpackTier`, Menü `backpack` (`BackpackMenu` = Spielerinventar + Rucksack, Öffnungsdaten
  `BackpackOpenData`, je Loader `platform/BackpackMenus`), Bildschirm `BackpackScreen` (Rezeptbuch, Kasten-Stil `ModScreenStyle`),
  Taste B (`BackpackKeyHandler`, `OpenBackpackPayload`), Tooltip (`BackpackTooltipData`/`BackpackTooltip`), Blocktönung
  (`BackpackBlockTint`), Rücken-Ebene (`BackpackLayer`), Rezepttyp `simplebuilding:backpack_upgrade` (`BackpackUpgradeRecipe`),
  Tag `not_allowed_in_backpack`, `backpacks`, `dyeable_storage` (26.3: nur noch Rucksäcke), Daten-Fixer (alte Slot-Ids).
- Kopplung an SB: Meisterbauer (Baustab-Material, Pick-Taste, Pick-Paket, Köcher im Rucksack), Konstrukteurs Hand (Hand
  auffüllen), Farbpalette (Exklusiv-Set), Zusatz-Kopfplatz N49 (`ExtraHead.HeadSlot`) + Resonanzknopf (`TrimStatsPanel`) im
  Rucksack-Menü/-Bildschirm, Erfolg `storage/pitching_camp` (SB-Feature-Trigger), Curios/Trinkets-Haken (SB), Tabs/Suchtab/
  Testzentrale/Guide/Discover/Advancements/Loot (Lagerstätten-Loot? nein: nur Rezepte), Money-Händlerzeilen.
- Tests: `BackpackTests` (1430 Z.), Rucksack-Teile in `AccessorySlotTests`, `DyedStorageTests`, `WorldUpgradeTests`,
  `WandEnchantmentTests`, `ModScreenStyleTests`, `ModpackTests`, `DataIntegrityTests`, `AdvancementTriggerTests`; Client
  `BackpackClientTest` (Client-Läufe heute nicht erlaubt).

## 2. Entscheidungen (selbst getroffen)

- **E1 Umfang 8c-2:** alles in §1 außer den SB-Verzauberungen (Meisterbauer/Konstrukteurs Hand/Farbpalette bleiben SB; das Modul
  liest sie per `ResourceKey simplebuilding:*`, wie 8c-1).
- **E2 26.2 (Muster 8b-1/8c-1):** SB-Klassen bleiben, 26.2 unverändert. 26.3: `ModItems/ModBlocks.*BACKPACK` = null, BE/Menü/
  Serializer/Komponente nicht registriert, Client-Registrierungen nur bei Nicht-null, Datagen 26.3 ohne Rucksack-Modelle/-Rezepte/
  -Loot/-Tags (`removed-on-26.3.txt` per Sync), SBs 26.3-Overlay-Färberezepte `<farbe>_dyed_storage` (Tag wäre leer) entfallen.
- **E3 Modul-Code** `com.simplestorage.backpack.*` (Kopien ohne SB-Import): Item, Tier, Block, BE, Contents, Container,
  WornContainer, Menü (+Slots, Layout, OpenData, Providers), UpgradeRecipe, TooltipData, `BackpackNet` (Öffnen-Paket, Öffnen je
  Loader über Haken), Client `BackpackScreen` (Kasten-Stil mit SimpleLib-UI, sonst Fenster-Textur), `BackpackTooltip`,
  `BackpackBlockTint`, `BackpackLayer` (zeigt auch den Köcher-Vorrang wie bisher), `BackpackKeys` (Taste
  `key.simplestorage.open_backpack`, Standard B; Rebelegung alter SB-Taste geht verloren – vermerkt). Funnel am getragenen
  Rucksack in `ItemEntityBundleMixin` nach den Bündeln. Konstrukteurs Hand/Meisterbauer bleiben SB-Logik.
- **E4 SimpleLib:** `CarriedStacks.Entry` bekommt den getragenen Behälter (`container`, 3-Arg-Konstruktor, 2-Arg bleibt);
  Aufrufer filtern selbst nach Verzauberung. Das Modul meldet den getragenen Rucksack an (alle Einträge; Rückschreiben über die
  Komponenten-Slot-Id), SB meldet nichts mehr an. Neu `com.simplelib.api.InventoryExtras` (Zusatz-Slot in spielerinventar-artigen
  Menüs: Fabrik + Schnellverschieben-Regel) und `com.simplelib.api.client.InventoryExtrasClient` (Zeichnen: Rahmen, Knopf,
  Tooltip); SB meldet auf 26.3 den Zusatz-Kopfplatz + Resonanzknopf dort an → Rucksack-Menü des Moduls zeigt beides mit SB, allein
  nicht. Curios/Trinkets-Haken ziehen ins Modul (compileOnly wie SB, nur bei geladenem Mod), SB registriert sie auf 26.3 nicht mehr.
- **E5 Ids (SimpleLib `IdMoves`):** ITEM + BLOCK der 4, BLOCK_ENTITY_TYPE `backpack`, DATA_COMPONENT_TYPE `backpack_contents`,
  RECIPE + Rezept-Advancement `backpack`, `reinforced_backpack`, `netherite_/enderite_backpack_smithing`. Menütyp/Paket/Serializer
  werden nicht gespeichert (neue Ids `simplestorage:*`). Item-Tag `simplebuilding:not_allowed_in_backpack` liest das Modul
  zusätzlich (Modpacks), eigener `simplestorage:not_allowed_in_backpack`.
- **E6 Daten im Modul:** Modelle/Items/Blockstates/Texturen (26.3-Overlay-Stand), Loot `copy_components`, Rezepte: Grundrucksack
  mit `simplebuilding:leather_sheet` nur mit SB, sonst `backpack_without_simplebuilding` mit Leder; Verstärkt per Upgrade-Rezept;
  Netherit (Vanilla), Enderit (SimpleLib-Vorlage/Barren). Färben über `#simplestorage:dyeable_storage` (+ Rucksäcke). Verzauber-Tags
  `deep_pockets_/funnel_enchantable`, `bundle_enchantable`? nein – Rucksäcke: Tiefe Taschen/Trichter + SB-Tags `master_builder`/
  `constructors_touch`/`color_palette` optional `#simplestorage:backpacks`. Curios `back`/Trinkets `chest/back` Tags. Lang EN/DE.
- **E7 SB 26.3 per Id:** `StorageRefs.backpack(tier)`, `isBackpack`, Tabs/Suchtab/Testzentrale/Advancements (`pitching_camp` über
  Vanillas `placed_block`)/Guide/Discover/Item-Tags; Baustab/Pick/Konstrukteurs Hand über SB-Overlay-Paar `util/WornStorage`
  (26.2: `BackpackItem`, 26.3: SimpleLib `CarriedStacks` gefiltert nach SB-Verzauberung).
- **E8 Tests:** `BackpackTests` + Rucksack-Teile von `AccessorySlotTests`/`DyedStorageTests` ins Modul (`BackpackTests`,
  Fabric-Adapter); SB-Tests mit Rucksack-Klassen laufen auf 26.3 nicht mehr (`STORAGE_IN_SB`-Wächter), Zusammenspiel
  (Meisterbauer/Konstrukteurs Hand am Modul-Rucksack) per Id in SB-Tests. Neu: Migration alter `simplebuilding:`-Rucksäcke (Item
  mit Inhalt/Farbe/Verzauberung, abgestellter Block mit BE).

## 3. Umsetzung (Commits)

1. Plan. 2. SimpleLib (`CarriedStacks.Entry.container`, `InventoryExtras`). 3. Modul: Code, 3 Loader, Ressourcen, IdMoves, Tests.
4. SB: Gates, `StorageRefs`, `WornStorage`-Paar, Aufrufer, Client-Gates, Datagen, Overlays, Tests. 5. sb-test: Compile, Tests,
Datagen 26.3, Guides/Wiki/Discover (`-gen`), `check`.

## 4. Risiken

- Rucksack-Menü ist Spielerinventar-Kopie: Zusatz-Kopfplatz über neuen SimpleLib-Haken (Server und Client müssen dieselbe
  Slotzahl haben – beide fragen denselben Haken; SB meldet ihn in der gemeinsamen Init an).
- Curios/Trinkets-Abhängigkeiten im Modul-Build (compileOnly); fehlt das Artefakt, Haken in SB lassen (Abweichung vermerken).
- Client (Bildschirm, Ebene, Tönung, Taste) nur per Compile prüfbar (keine Client-Läufe).

## 5. Prüfung (sb-test)

Compile 26.3 fabric/neoforge/forge + 26.2 `:compileJava`/neoforge/forge, Modul fabric/neoforge/forge; module-simplestorage
fabric/neoforge/forge/standalone-fabric/standalone-neoforge; fabric-263 + neoforge-263 gezielt (`*backpack*`, `*accessory*`,
`*dyed*`, `*wand*`, `*world_upgrade*`, `data_integrity*`, `*screen_style*`, `*modpack*`); integration-263; fabric-262 Rucksack;
Datagen 26.3; Guides/Wiki/Discover; `check`. Nicht geprüft (Auftrag): Client-Läufe.

## 6. Stand und Abweichungen

Umgesetzt wie geplant: Modul `com.simplestorage.backpack.*` (Item, Tier, Block, BE, Contents, Container, WornContainer, Menü
mit Slots/Layout/OpenData/Providers, UpgradeRecipe, TooltipData, `BackpackNet`, `BackpackKeyMigration`), Client
(`BackpackScreen` mit Kasten-Stil aus SimpleLib-UI, `BackpackTooltip`, `BackpackBlockTint`, `BackpackLayer`, `BackpackKeyHandler`,
Mixin `BackpackKeyScreenMixin`), Trichter am getragenen Rucksack in `ItemEntityBundleMixin`, 3 Loader (Menütyp je Loader,
Öffnen über `BackpackMenuProviders.opener`, Paket, Taste B, Tönung, Ebene, Tooltip, Vanilla-Tabs allein), Curios-/Trinkets-Haken
(`CuriosHook`, `TrinketsHook`, compileOnly) + Daten, IdMoves (4 Items/Blöcke, BE, Komponente, 4 Rezepte + Freischaltungen,
16 `<farbe>_dyed_storage`), Ressourcen (Modelle, Items, Blockstates, Texturen im 26.3-Stand, Loot, Rezepte mit
`*_without_simplebuilding`, Tags `backpacks`/`not_allowed_in_backpack`/Färben/Verzauberungen, Lang, Fragentisch, Wiki-Notiz,
Money-Zeilen, Discover-Tipp). SimpleLib: `CarriedStacks.Entry.container`, `InventoryExtras`, `client.InventoryExtrasClient`.
SB: `STORAGE_IN_SB`-Gates (Items, Blöcke, BE, Komponente, Client-Tönung/Taste, Accessory-Haken, Datagen), `StorageRefs.backpack*`,
`util/WornStorage` (Overlay-Paar) für Baustab/Pick/Konstrukteurs Hand, Kopfplatz + Resonanzknopf über `InventoryExtras`
(`LibServices`, `client/LibClientServices`), Advancement `pitching_camp` über Vanillas `placed_block` (`AdvancementCompat`),
Guide/Tabs/Suchtab per Id, 26.3-Färberezepte von SB entfernt, Tests per Id (`StorageBackpacks`) oder `STORAGE_IN_SB`-Wächter.

Belege (sb-test, 542ed9057 bzw. davor): Compile 26.3 fabric(+gametest)/neoforge/forge, Modul fabric/neoforge/forge, 26.2
`:compileJava`/`:neoforge`/`:forge` Exit 0; fabric-263 + neoforge-263 2398/2404 → nach Fix advancement_tree* 16/16,
server_tuning* 16/16; module-simplestorage fabric/neoforge/forge/standalone-fabric/standalone-neoforge je 104 (520/520,
fabric + standalone-fabric nach Namensfix erneut 208/208); integration-263 3/3; forge-263 `data_integrity*` 51/51; fabric-262
`backpack*` 16/16, `*accessory*` 5/5 (`*world_upgrade*` 7/8: `fixtures_are_what_twenty_six_two_writes` scheitert an
`GhostItems` des Trichters – vorbestehend, nicht Rucksack); Datagen 26.3, Guides, Wiki, Discover, Money-Links; `./gradlew check` Exit 0.

Abweichungen (bewusst):
- SBs Schalter `server.features.backpack` wirkt auf 26.3 nicht mehr (Rucksäcke sind Modul-Inhalt; Ausblenden in Stufe 9); sein
  Rezeptfilter-Test läuft nur auf 26.2.
- Rucksack-Taste heißt `key.simplestorage.open_backpack` (Kategorie Inventar); die alte Belegung wird einmal aus `options.txt`
  übernommen.
- Discover-Tipp „Rucksack“ ist Simple Storages (26.2 verliert den SB-Tipp, wie bei den Hämmern in Stufe 7).
- Meisterbauer/Konstrukteurs-Hand-Tests mit Modul-Rucksack laufen in SBs `BackpackTests` (per Id), nicht in den Modulzielen.
- Client-Tests `BackpackClientTest` auf 26.3 ausgesetzt; `ModUiStyleClientTest`/`HudAndTooltipClientTest` bauen weiter SBs
  (26.2-)Rucksack-Bildschirm. Client-Sicht (Bildschirm, Ebene, Tönung, Taste, Kopfplatz) offen.
