# Plan b-s8c-storage (20.10.2026): Familienstufe 8, Teil 8b-2 (Kisten-Fahrzeuge) und Zuschnitt 8c

Branch `b-s8c-storage` (von bundle25 4f7b391c8 + Queue-Commits, enthält 8a + 8b-1, b-bundles, b-tooltips), Sitzung B,
Queue-Zeile 94. Grundlage: `PLAN-B-S8-STORAGE-2026-10-19.md` (8a, Muster für 26.3-only-Inhalt: Klassen ziehen ganz um),
`PLAN-B-S8B-STORAGE-2026-10-19.md` (8b-1, Muster für Inhalt beider Linien: `STORAGE_IN_SB`, SB-Kopie bleibt für 26.2),
Vorbild Simple Ovens (Ofenloren, Stufe 4: `OvensRegistry.CARTS`, Vanilla-abgeleitete Item-Texturen im Modul),
`PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`.

## 1. Ist-Zustand (git grep, Stand 66a2b191c)

- **Kisten-Fahrzeuge** (Queue N19/N23): 3 Kistenloren + 3 Kistenboote (`TieredChestMinecart`, `TieredChestBoat`,
  `TieredChestBoatItem`, `TieredMinecartItem`, `BoatWoods`, `OversizedStacks`, `TieredStorageVehicle`), Komponente
  `simplebuilding:boat_wood`, Renderer im 26.3-Overlay (`VehicleRenderers`), Item-Modelle (36 + 6 End-Holz-Modelle in
  simpleend), Rezepte (40 Datagen + 6 End-Holz von Hand), Vanilla-abgeleitete Item-Texturen (`vehicles_2026_10_10.py`).
  **Nur 26.3** (`McVersion.TIERED_VEHICLES`, 26.2 `false`) → Muster 8a: Klassen und Dateien ziehen ganz um.
- Trichterloren bleiben SB (Trichter sind SB), Ofenloren sind schon Simple Ovens'. SB-Trichterlore nutzt nur
  `VehicleTiers`/`TieredMinecartItem` (bleiben).
- Kopplung der Kisten-Fahrzeuge an SB-Inneres: `BackpackItem.maxStackSizeIn` (= SimpleLib `StackLimits.max`), SB-Truhenmenü
  über `TieredChestMenus`-Shims (Modul hat einfache `MenuType`s je Stufe, Vanillas `openMenu(this)` genügt),
  `ModDataComponentTypes.BOAT_WOOD`, `TieredChests.oversizedStorage` (SimpleLib-Trichtermixin deckt `getMaxStackSize(stack)`
  ab), Tabs/Suchtab/Testzentrale/Discover/Wiki, `VehicleTests` (Kisten-Teile), `WorldPolishTests` (Bootsdeck),
  Client-Tests `VehicleClientTest`/`BoatDeckClientTest`.
- **Seltene Funde** (`BetterChests`, `RareShulkers`, `ShulkerShells`, End-Stadt-/Eskorte-/Strukturmixins, ~800 Zeilen + 519 Testzeilen,
  beide Linien) und **8c** (Bündel ~690 + Köcher ~350 + Rucksäcke ~15 Klassen + Lager-Verzauberungen, zusammen ~200 Dateien,
  ~7000 Testzeilen, beide Linien, Accessory-Slots/Tooltips/Mixins) sind je größer als 8b-1.

## 2. Entscheidungen (selbst getroffen)

- **E1 Umfang dieses Branches: 8b-2 = Kisten-Fahrzeuge vollständig und grün.** Seltene Funde → **8b-3**, 8c geteilt in
  **8c-1** (verstärkte Bündel, Köcher, platziertes Bündel, Lager-Verzauberungen Drawer/Deep Pockets/Funnel; die
  Verzauberungen wandern mit ihren Zielen, Köcher erben vom Bündel) und **8c-2** (Rucksäcke: Block, Item, Menü, Taste,
  Layer, Upgrade-Rezept). 8c-1 beginnt auf diesem Branch nur, wenn 8b-2 grün ist und Zeit bleibt; sonst bleibt er mit
  diesem Zuschnitt im Plan/Queue/Memory. b-bundles/b-tooltips sind in der Basis enthalten (Abhängigkeit erfüllt).
  Begründung: Auftrag „lieber ein Teil fertig und grün als alles halb“; 8b-3/8c betreffen beide Linien und brauchen das
  8b-1-Muster mit SB-Kopie.
- **E2 Modul-Code (8b-2):** `com.simplestorage.vehicle.{BoatWoods, OversizedStacks, TieredChestMinecart, ChestMinecartItem,
  TieredChestBoat, TieredChestBoatItem}`, `com.simplestorage.client.VehicleRenderers` (Kistenlore + Kistenboot, Holzarten,
  End-Hölzer nur mit Simple End), Registrierung `StorageEntities` (ENTITY_TYPE-Phase), Items in `StorageItems`,
  Komponente `simplestorage:boat_wood` in `StorageComponents`. Ohne SB-Import: Stapelgrenzen `StackLimits`, Menü
  `TieredChestMenu.server` (Modul), Brennbarkeit/Seltenheiten wie bisher. Kein `TieredStorageVehicle` mehr (SimpleLib-Trichter
  lesen `getMaxStackSize(stack)`).
- **E3 Ids (SimpleLib `IdMoves`):** ITEM + ENTITY_TYPE der 6 Fahrzeuge, DATA_COMPONENT_TYPE `boat_wood`, RECIPE +
  Rezept-Advancement (`recipes/transportation/…`) aller 46 Rezepte (je Stufe Lore, Boot je Holzart inkl. Ashwood/Duskwood,
  `_smithing` für Netherit/Enderit). Enderit-Schmieden über `simplelib:enderite_upgrade_template` + `simplelib:enderite_ingot`
  (wie Simple Ovens), Netherit Vanilla.
- **E4 SB:** Klassen `TieredChestMinecart`, `TieredChestBoat`, `TieredChestBoatItem`, `BoatWoods`, `OversizedStacks`,
  `TieredStorageVehicle` entfallen; `ModEntities`/`ModItems`/`BOAT_WOOD` ohne die 6. `VehicleTiers.chestMinecart/chestBoat`
  und Tabs (`chest_vehicles`), Suchtab, Testzentrale, DataIntegrity lesen per `StorageRefs` (Id, 26.3 `simplestorage:`).
  Datagen 26.3 ohne Kisten-Fahrzeug-Rezepte; Dateien per `git mv` ins Modul (Namensraum umgeschrieben). Lang-Schlüssel
  der 6 Fahrzeuge in SB entfallen (26.2 nutzt sie nie), Modul-Lang EN/DE. SB-Tests: Kisten-Teile von `VehicleTests` und der
  Bootsdeck-Test ziehen ins Modul; SB behält Trichterloren-Tests; Client-Tests lesen per Id.
- **E5 Texturen:** Vanilla-abgeleitete Sprites (Kistenlore, Boot-/Floß-Overlay je Stufe) über `MODULE_OUTPUTS['simplestorage']`
  + `gradle/module-vanilla-derived.gradle` im Modul (Muster Ovens); `vehicles_2026_10_10.py` schreibt die Modelle der
  Kisten-Fahrzeuge ins Modul (End-Holz-Modelle bleiben in simpleend, Overlay jetzt `simplestorage:`). SB verliert die
  Ausgaben (26.2 hat keine Fahrzeuge).
- **E6 Tabs:** Modul-Tab nach den Shulkerkisten: Kistenloren, Kistenboote (Eiche). Allein (ohne SB): Vanilla-Werkzeugtab
  hinter Kistenlore bzw. Bambus-Kistenfloß (`fillsVanillaTabs`); mit SB stellt SB sie per Id (wie bisher).
- **E7 Doku/Daten:** Modul-Wiki-Notizen `*_chest_minecart`/`*_chest_boat` (aus SB-Wiki), Guide-Kapitel, Discover-Muster,
  `docs/MODS-UND-FEATURES.md`; Erzeugung (Guides, Wiki, Discover, Datagen) auf sb-test.

## 3. Umsetzung (Commits)

1. Plan (dieses Dokument).
2. Modul 8b-2: Klassen, Registrierung (3 Loader), Renderer, IdMoves, Ressourcen (`git mv` + Namensraum), Lang, Texturen-Hook, Tests.
3. SB 8b-2: Klassen/Registrierungen raus, Refs, Tabs/Suchtab/Testzentrale/Discover, Datagen, Lang, Tests, Wiki-Notizen.
4. Erzeugen auf sb-test (`-gen`): Datagen 26.3, `module_guides.py`, `wiki/generate.py --all`, `discover.py`.
5. Wenn Zeit: 8c-1 nach eigenem Abschnitt in diesem Plan.

## 4. Risiken

- Alte Welten: Fahrzeug-Entity, Item und Komponente müssen alle ziehen (Migrationstest liest gespeicherte Entity mit altem Id).
- Fabric-Reihenfolge: Items brauchen den Entity-Typ erst beim Benutzen (Supplier), Entity-Typen nach Items registrieren wie Ovens.
- Client-Renderer nur prüfbar per Compile (keine Client-Läufe erlaubt).
- Wiki-/Textur-Checks kennen die neuen Pfade erst nach Erzeugen.

## 5. Prüfung (sb-test)

Compile 26.3 fabric/neoforge/forge + 26.2 `:compileJava`; `module-simplestorage-{fabric,neoforge,standalone-fabric}-263`
(+ standalone-neoforge, falls vorhanden); fabric-263/neoforge-263 gezielt `simplebuilding:vehicle*`, `simplebuilding:world_polish*`,
`simplebuilding:data_integrity*`; `integration-263` einmal; `./gradlew check`. Nicht geprüft (Auftrag): Client-Läufe.

## 6. Stand und Abweichungen

(Belege werden nach den Läufen ergänzt)

## 7. Zuschnitt der Folgeteile (für die nächste Sitzung, je eigener Branch von `claude-wave1` nach Merge von 8b-2)

Alle drei betreffen **beide Linien** → Muster 8b-1: SB-Klassen bleiben in `common/src/shared` für 26.2, auf 26.3 registriert SB
sie nicht (`McVersion.STORAGE_IN_SB`), das Modul hat Kopien unter `com.simplestorage.*`, SB greift per `StorageRefs` (Id) zu.

- **8b-3 Seltene Funde** (`util/BetterChests`, `util/RareShulkers`, `util/ShulkerShells` + Hüllen-Items, Mixins
  `StructureTemplateBetterChestMixin`, `StructurePieceBetterChestMixin`, `EndCityPieceMixin`, `ShulkerEscortMixin`,
  `MobShulkerShellDropMixin`, `ShulkerOpenComponentsMixin`/`ShulkerOpenStateMixin`/`client/ShulkerRendererMixin`
  (Deckel, nur Vanilla-Shulker), Textur `entity/rare_shulker/*` (Vanilla-abgeleitet), Tests `RareStructureFindsTests` 519 Z.).
  Offene Frage zuerst klären: Shulker-Deckel/-Hüllen sind Vanilla-Verhalten, gehören eher zu Simple Mobs/Tweaks als zu Lager;
  nur die Weltfunde (bessere Truhen, seltene Shulker mit Stufen-Kiste) sind sicher Lager.
- **8c-1 Bündel, Köcher, platziertes Bündel, Lager-Verzauberungen** (~45 Produktionsdateien): `ReinforcedBundleItem` (686 Z.),
  `QuiverItem` (erbt vom Bündel, 352 Z.), `PlacedBundleBlock`/`-BlockEntity`/`PlacedBundleRenderer`/`util/PlacedBundles`,
  `recipe/ReinforcedBundleRecipe` (eigener Serializer), `util/BundleUtil`, `util/DyedStorage` (auch Rucksack → SimpleLib
  oder Kopie), Tooltips (`ReinforcedBundleTooltipData`, `client/gui/tooltip/ReinforcedBundleTooltip*`, Overlay
  `BundleTooltipComponentMixin`), Netzwerk (`ReinforcedBundleSelectionPayload`, `PlacedBundleScrollPayload`, 3 Loader-
  Registrierungen), Mixins (`BowItemMixin`, `CrossbowItemMixin`, `AbstractArrowPickupMixin`, `ItemEntityMixin` (Funnel),
  `client/MouseMixin`, `client/HandledScreenMixin`), `QuiverLayer` + Accessory-Slot-Kompat (`compat/accessory`),
  Verzauberungen Drawer/Deep Pockets/Funnel (`ModEnchantments`, Datagen `ModEnchantmentTagProvider`, Tags
  `*_enchantable`, Bücher-Texturen `VanillaBookTextures`). Die Verzauberungen ziehen mit (sie gelten für Bündel, Köcher
  **und** Rucksäcke): SBs Rucksack liest sie auf 26.3 per `ResourceKey` im Namensraum `McVersion.STORAGE_NAMESPACE`.
  Tests: `ReinforcedBundleTests` (1397 Z.), `QuiverTests` (1410), `BundleWiringTests` (1525), `StorageEnchantmentTests`
  (1227), `DyedStorageTests`, `LeatherAndQuiverTests` (Köcher-Teil). Abhängigkeit b-bundles/b-tooltips: in der Basis enthalten.
- **8c-2 Rucksäcke** (~15 Klassen + Menü/Taste/Layer/Upgrade-Rezept, `BackpackTests`, `BackpackClientTest`, ~100 Dateien
  nennen `BackpackItem`; `BackpackItem.maxStackSizeIn` vorher überall durch SimpleLib `StackLimits.max` ersetzen).
