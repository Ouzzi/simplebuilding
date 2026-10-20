# Plan b-s8c1-storage (20.10.2026): Familienstufe 8, Teil 8c-1 – Bündel, Köcher, platziertes Bündel, Lager-Verzauberungen

Branch `b-s8c1-storage` (von wave1 22e0760f2 = 8a + 8b-1 + 8b-2), Sitzung B, Queue-Zeile 94 („8c-1 [~]“). Grundlage:
`PLAN-B-S8C-STORAGE-2026-10-20.md` §7 (Inventar/Zuschnitt), Muster für Inhalt beider Linien `PLAN-B-S8B-STORAGE-2026-10-19.md`
(`McVersion.STORAGE_IN_SB`, SB-Klassen bleiben für 26.2, SB greift per `StorageRefs`/Id zu), `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`.

## 1. Ist-Zustand (git grep, Stand 22e0760f2)

- Items: `reinforced/netherite/enderite_bundle` (`ReinforcedBundleItem`, 686 Z., extends Vanillas `BundleItem`, Inhalt
  `minecraft:bundle_contents`), `quiver`, `reinforced/netherite/enderite_quiver` (`QuiverItem` extends Bündel, Brustplatz-
  `EQUIPPABLE`). Block `placed_bundle` (`PlacedBundleBlock`/`-BlockEntity`/`PlacedBundleRenderer`, `util/PlacedBundles`; stellt auch
  Vanillas Bündel ab), Rezepttyp `simplebuilding:reinforced_bundle` (`ReinforcedBundleRecipe`, **auch vom SB-Farbkasten genutzt**).
- Verzauberungen `drawer`, `deep_pockets`, `funnel` (Datagen-Bootstrap `ModEnchantments`, Tags `bundle_enchantable`,
  `deep_pockets_enchantable`/`funnel_enchantable` = Bündel/Köcher + `#backpacks`, Exklusiv-Set `builder_group` mit SBs
  Master Builder/Color Palette), Bücher-Texturen über SBs `enchanted_book`-Modell (`VanillaBookTextures`), Quellen nur SB-Loot und
  SB-Bibliothekar.
- Kopplung an SB: Config `tools.invertBundleInteractions`, SB-Verzauberungen Master Builder/Color Palette/Constructor's Touch
  (per Schlüssel), `BackpackItem` (Köcher im getragenen Rucksack mit Meisterbauer; Rucksack-Funnel nach den Bündeln), `AccessorySlots`
  (Curios/Trinkets-Haken, Rucksack + Köcher), `DyedStorage`/`StorageDyes`, `guiDrawHelper` (Tooltip-Farbe), `McVersion`-Shims,
  `ModRegistries` (Serializer), Netzwerk (`ReinforcedBundleSelectionPayload`, `PlacedBundleScrollPayload`, 3 Loader), Mixins
  (`BowItemMixin`, `CrossbowItemMixin`, `AbstractArrowPickupMixin` nur Köcher; `ItemEntityMixin` Funnel/Feuerfest gemischt mit
  Rucksack/Attractor; `ItemUseOnMixin` gemischt (Vanilla-Bündel abstellen); `ItemStackUseOnMixin` nur Bündel; Client
  `MouseMixin`/`HandledScreenMixin`/`MinecraftClientMixin` gemischt, 26.3-Overlay `BundleTooltipComponentMixin`), `QuiverLayer`
  + `BackpackLayer.backItem`, Baustab (`instanceof ReinforcedBundleItem` als Materialquelle mit Meisterbauer), Tabs/Suchtab,
  Testzentrale, Guide-Kapitel, Discover, Wiki-Export, Advancements (`storage/bundle_of_joy`… bleiben SB-Baum), Loot/Händler.
- Beide Linien (26.2 + 26.3), Tests ~7000 Zeilen (59 Testmethoden in 8 Klassen + Teile in ~12 weiteren).

## 2. Entscheidungen (selbst getroffen)

- **E1 Umfang 8c-1:** 7 Items, `placed_bundle` (Block + BE + Renderer), Lager-Verzauberungen (3), Rezepttyp
  `simplestorage:reinforced_bundle`, Tooltip, Netzwerk, Mixins, Köcher-Ebene. **Nicht:** Rucksäcke (8c-2), Farbkasten (SB, behält
  `simplebuilding:reinforced_bundle`), Bücher-Texturen (SB besitzt das `enchanted_book`-Modell; ohne SB zeigen die drei Bücher das
  Vanilla-Buch), Master Builder/Color Palette/Constructor's Touch (SB; das Modul liest sie per `ResourceKey` `simplebuilding:*`).
- **E2 26.2 (Muster 8b-1):** SB-Klassen bleiben in `common/src/shared` (26.2 unverändert). 26.3 (`STORAGE_IN_SB=false`): SB
  registriert die 7 Items, den Block, die BE nicht (Felder `null`); `ModEnchantments.DRAWER/DEEP_POCKETS/FUNNEL` liegen im Namensraum
  `McVersion.STORAGE_NAMESPACE` (26.3 also die Modul-Verzauberungen; Rucksack, Loot, Tabs, Tests von SB lesen sie damit ohne Import),
  der Datagen-Bootstrap legt sie auf 26.3 nicht mehr an. Reine Bündel-Mixins (`BowItemMixin`, `CrossbowItemMixin`,
  `AbstractArrowPickupMixin`, `ItemStackUseOnMixin`) über `AliasMixinPlugin.STORAGE_MIXINS` aus; gemischte Mixins bleiben, ihre
  Bündel-Zweige sind auf 26.3 tot (`instanceof` SB-Klasse) bzw. werden geschützt (`PlacedBundles.tryPlace` → `STORAGE_IN_SB`).
  `mc1_21_11` bleibt unberührt (Port-Run).
- **E3 Modul-Code** `com.simplestorage.bundle.*` (Kopien ohne SB-Import): `ReinforcedBundleItem`, `QuiverItem`, `PlacedBundleBlock`,
  `PlacedBundleBlockEntity`, `PlacedBundles`, `ReinforcedBundleRecipe` (nur `BundleItem`), `BundleTooltipData`, `DyedStorage`
  (+ `StorageDyes`-Tabelle), `BundleEnchantments` (Schlüssel + Lesehilfen), `StorageConfig` (`config/simplestorage.json`,
  `invertBundleInteractions`, beim ersten Start aus SBs `simplebuilding.json` übernommen, `ConfigFiles.adoptLegacy`), Netzwerk
  `BundleNet` + 2 Payloads (3 Loader), Client `BundleTooltips`, `BundleScrollHandler` (Tooltip-Mausrad), `PlacedBundleRenderer`,
  `QuiverLayer` (3 Loader). Mixins: Bogen/Armbrust/Pfeil-Aufheben, Item-Aufheben (Funnel, mit höherer Priorität als SBs Rucksack-
  Funnel), Feuer-/Explosionsfestigkeit liegender Netherit-/Enderit-Teile, `Item.useOn` (Vanilla-Bündel abstellen), `ItemStack.useOn`
  (in abgestelltes Bündel legen), Client: Mausrad auf abgestelltem Bündel, Tooltip-Handler im Container-Bildschirm,
  `ClientBundleTooltip` (Füllbalken/Tönung, eigener Accessor).
- **E4 SimpleLib (gemeinsam, kein Import zwischen SB und Modul):** `com.simplelib.api.AccessorySlots` (der bisherige SB-Haken,
  unverändert; SBs Curios-/Trinkets-Haken melden sich auf 26.3 dort an – SB-`AccessorySlots` wird Overlay-Paar: 26.2 wie bisher,
  26.3 reicht an SimpleLib weiter) und `com.simplelib.api.CarriedStacks` (Einträge, die ein Spieler in getragenen Behältern hat;
  SB meldet den getragenen Rucksack mit Meisterbauer an → Köcher-Schritt 5 und Baustab bleiben gleich). 8c-2 zieht beides ins Modul nach.
- **E5 Ids (SimpleLib `IdMoves`):** ITEM der 7, BLOCK `placed_bundle`, BLOCK_ENTITY_TYPE `placed_bundle`, ENCHANTMENT
  `drawer`/`deep_pockets`/`funnel`, RECIPE + Rezept-Advancement (`recipes/tools/…`) von `reinforced_bundle`, `quiver`,
  `reinforced_quiver`, `netherite_/enderite_bundle_smithing`, `netherite_/enderite_quiver_smithing`. Tags `simplebuilding:*`
  bleiben als SB-Tags (verweisen optional auf `#simplestorage:*`); Rezepttyp `simplebuilding:reinforced_bundle` bleibt SB (Farbkasten).
- **E6 Rezepte/Daten im Modul:** Verstärktes Bündel/Köcher mit `simplebuilding:leather_sheet` nur wenn SB geladen, sonst
  `*_without_simplebuilding` mit Leder (Muster simpleblocks), Köcher Vanilla, Schmieden Netherit (Vanilla) / Enderit (SimpleLib
  Vorlage + Barren), 16 Färberezepte `simplestorage:<farbe>_dyed_storage` auf `#simplestorage:dyeable_storage` (SBs Tag behält auf
  26.3 nur die Rucksäcke). Verzauberungen: Modul-Tags `bundle_enchantable`, `deep_pockets_enchantable`/`funnel_enchantable`
  (+ optional `#simplebuilding:backpacks`), Exklusiv-Set `simplestorage:exclusive_set/builder_group` (Drawer + optional SBs Master
  Builder/Color Palette), Curios-/Trinkets-Tags. Modelle/Items/Texturen/Blockstate **kopiert** (26.2 behält SBs), 26.3-Datagen von SB
  lässt Bündel-/Köcher-Modelle, -Rezepte, Verzauberungen weg (`removed-on-26.3.txt`). Lang EN/DE ins Modul (SB-Schlüssel bleiben).
- **E7 SB 26.3 per Id (`StorageRefs`):** Tabs/Suchtab/Testzentrale/Advancements/Loot/Händler/Guide/Discover/Item-Tags
  (`extra_inventory_items`, `master_builder`, Feuer-Tags), Baustab-Materialquelle (`StorageRefs.isStorageBundle`: `BundleItem`
  im Lager-Namensraum), `BackpackLayer.backItem` (Köcher per Id). Händler-/Bibliothekar-Dateien als 26.3-Overlay mit
  `simplestorage:`-Ids.
- **E8 Tests:** Die reinen Bündel-/Köcher-Tests (`ReinforcedBundleTests`, `QuiverTests`, `PlacedBundleTests`, `StorageEnchantmentTests`,
  Bündel-Teile von `BundleWiringTests`/`DyedStorageTests`/`LeatherAndQuiverTests`) werden ins Modul portiert (`BundleTests`,
  Fabric-Adapter `ModuleGameTest`); in SB laufen sie auf 26.3 nicht mehr (`STORAGE_IN_SB`-Wächter, 26.2 unverändert). Zusammenspiel
  mit SB (Baustab, Meisterbauer/Farbpalette, Rucksack, Leder-Platte, Händler, Loot) prüfen SB-Tests auf 26.3 per Id gegen das Modul.
  Neu: Migrationstest (alte `simplebuilding:`-Bündel mit Inhalt + Verzauberung, platziertes Bündel).

## 3. Umsetzung (Commits)

1. Plan (dieses Dokument).
2. SimpleLib: `AccessorySlots`, `CarriedStacks`.
3. Modul: Klassen, Registrierung (3 Loader), Netzwerk, Client, Mixins, IdMoves, Ressourcen, Lang, Config, Tests.
4. SB: `STORAGE_IN_SB`-Gates, `StorageRefs`, `ModEnchantments`, Datagen 26.3, Mixin-Plugin, AccessorySlots-Paar, Rucksack-Anmeldung,
   Aufrufer, 26.3-Overlay-Daten, Tests.
5. Erzeugen auf sb-test (`-gen`): Datagen 26.3, `module_guides.py`, `wiki/generate.py --all`, `discover.py`; `check`.

## 4. Risiken

- Dynamische Register (Verzauberungen, Händler) beim Weltladen: Verweise auf alte `simplebuilding:drawer` in SB-Daten → 26.3 schreibt
  `simplestorage:`; Migration alter Items über IdMoves (Test).
- Reihenfolge Funnel Bündel vor Rucksack (zwei Mixins am selben Kopf) → Priorität + Test.
- Client-Teile (Tooltip-Mixin, Renderer, Ebene, Mausrad) nur per Compile prüfbar (keine Client-Läufe erlaubt).
- Umfang: wird ein Teil nicht grün, bleibt er dokumentiert offen (Abschnitt 6).

## 5. Prüfung (sb-test)

Compile 26.3 fabric (+gametest)/neoforge/forge + 26.2 `:compileJava`, Modul fabric/neoforge/forge; module-simplestorage
fabric/neoforge/forge/standalone-fabric/standalone-neoforge; fabric-263 + neoforge-263 gezielt (`*bundle*`, `*quiver*`,
`*storage_enchantment*`, `*dyed*`, `*accessory*`, `*backpack*`, `*wand_enchantment*`, `data_integrity*`); integration-263; `check`.
Nicht geprüft (Auftrag): Client-Läufe.

## 6. Stand und Abweichungen

Umgesetzt wie geplant: Modul `com.simplestorage.bundle.*` (Bündel, Köcher, abgestelltes Bündel, Rezepttyp, Tooltip-Daten,
Färben, Verzauberungsschlüssel, `BundleNet` mit 2 Paketen auf 3 Loadern), Client (`BundleTooltips`, `BundleScrollHandler`,
`PlacedBundleRenderer`, `PlacedBundleTint`, `QuiverLayer` auf 3 Loadern), 9 Mixins, `StorageConfig`
(`config/simplestorage.json`), IdMoves (7 Items, Block + BE `placed_bundle`, 3 Verzauberungen, 7 Rezepte + Freischaltungen),
112 Ressourcen kopiert (Modelle, Items, Blockstate, Texturen, Rezepte, Verzauberungen), Tags/Färberezepte/Curios-Trinkets-Tags,
Rezepte `*_without_simplebuilding` mit Leder, Lang EN/DE, Fragentisch, Wiki-Notizen (11, aus SBs Handbuch), Handbuch-Intro,
Money-Zeilen, `check_data.py`. SimpleLib: `AccessorySlots`, `CarriedStacks`. SB: `STORAGE_IN_SB`-Gates (Items, Block, BE,
Client-Registrierungen, Datagen), Verzauberungsschlüssel im Lager-Namensraum, `StorageRefs` (Ids, `isStorageBundle`,
`isQuiver`), Baustab/Meisterbauer-Pick/Rücken-Ebene per Id, `PlacedBundles` auf 26.3 aus, Mixin-Plugin, AccessorySlots als
Overlay-Paar, Rucksack meldet sich bei `CarriedStacks` an, 26.3-Overlays (Händler, Bibliothekar, Zubehör-Tags, Craftable),
Testzentrale per Id, Loot/Advancements/Tabs/Guide per Id, Balance- und Wiki-Lootleser kennen `StorageRefs`.
Tests: 7 Klassen ins Modul (85 Modul-Tests inkl. 41 neu: 39 portiert, Registrierung, Migration alter Bündel/abgestellter Bündel,
Rezepte allein/mit SB); SB-Tests der Bündel laufen auf 26.3 nicht mehr (26.2 unverändert), Zusammenspiel-Tests per Id.

Belege (sb-test, 2ec539941 / f92c64aeb): Compile 26.3 fabric/neoforge/forge, 26.2 `:compileJava`/`:neoforge`/`:forge`, Modul
fabric/neoforge/forge; module-simplestorage fabric/neoforge/forge/standalone-fabric/standalone-neoforge 425/425 (je 85);
fabric-263 + neoforge-263 2404/2404; integration-263 3/3; forge-263 `data_integrity*` 51/51; fabric-262 `*bundle*` 41/41,
`*quiver*` 20/20, `*storage_enchantment*` 6/6, `*accessory*` 5/5, `*dyed*` 6/6; Datagen 26.3, Guides, Wiki, Discover, Quests,
Money-Links erzeugt; `./gradlew check` Exit 0.

Abweichungen (bewusst):
- Tests, die SBs Meisterbauer/Farbpalette/Konstrukteurs-Hand brauchen (4 im Modul), bestehen ohne SB ohne Prüfung; mit SB laufen
  sie nur, wo Modultests mit SB geladen werden (nicht in den Modulzielen). Ebenso auf 26.3 nicht mehr geprüft: SB-Tests mit
  Bündel-Klassen (Klick-Umkehr-Config, Rucksack-Waschen/Tooltip-Farbe, Bündelkapazität in ItemBehaviour, Upgrade-Kontinuität im
  Lederplatte-Test).
- Quelle der 3 Verzauberungen nur über SB (Loot, Bibliothekar); das Modul allein hat keine Fundquelle. Bücher-Textur nur mit SB.
- Zubehörplätze (Curios/Trinkets) nur mit SB (Haken in SB); Köcher im Rucksack über `CarriedStacks` bis 8c-2.
- SBs Config-Option `tools.invertBundleInteractions` wirkt auf 26.3 nicht mehr (Modul-Config); Ausblenden in Stufe 9.
- Abgestellte Bündel in der Testzentrale per Id; Client-Test `placed-bundle` und Bündel-Teile von `HudAndTooltipClientTest` auf 26.3
  ausgesetzt (Client-Läufe nicht erlaubt, Client-Sicht offen).
- FTB-Quest „Verstärkter Köcher“ nennt weiter `simplebuilding:reinforced_quiver` (Generator; IdMoves löst auf).
