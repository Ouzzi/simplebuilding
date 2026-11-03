# Plan MACH32-MOD Stufe 3 (Sitzung B, b-machines03, 03.11.) – Trichter und Trichterloren nach Simple Machines

Auftrag: /root/sb-tools-b/b02-auftrag.md, Stufe 3 aus docs/ai/PLAN-B-MACHINES01-2026-11-01.md, Muster Stufe 2
(docs/ai/PLAN-B-MACHINES02-2026-11-03.md). Besitzer MACH32-MOD: „alle die in simple machines tab liegen in simple machines submod
legen“. MACH32-ENDSYS: Enderit-Teile nur mit Simple Ores; wo Enderit optional ist, Simple Ores als „recommends“.
Basis `hub/b-bundle97-gen` 7a5cb2046 (Bündel 97 = Stufe 2 gemergt).

## 1. Ist-Stand

- SB-Kern (`common/src/shared`, baut für 26.2 und 26.3 Fabric/NeoForge/Forge): `ModHopperBlock`, `ModHopperBlockEntity` (BE-Typ
  `simplebuilding:mod_hopper`, NBT `Items`, `FilterMode`, `TransferCooldown`), `ModHopperScreenHandler` + `NetheriteHopperScreenHandler`
  (Menü `simplebuilding:netherite_hopper`, Öffnungsdaten BlockPos über `platform/HopperMenus` je Loader), `NetheriteHopperScreen`
  (26.3-Kastenstil über `ModScreenStyle.hopper*`), `ToggleHopperFilterPayload` (`simplebuilding:toggle_filter`), `FilterHopper`,
  `HopperFilterMode`, `ItemFilter` (auch vom Autonomen Crafter genutzt → bleiben in SB), `TieredHopperMinecart` (+ `TieredMinecartItem`,
  NBT `Enabled`, `FilterMode`), Tempo `server.machines.{reinforced,netherite,enderite}HopperSpeed` (`ServerTuning.hopperSpeed/-Cooldown`),
  Hammer-Aufwertung (SledgehammerUpgrades → `in_world_upgrades`), Tooltip `InfoTooltips` (`tooltip.simplebuilding.hopper.*`),
  Jade `BlockInfo.HOPPER_FILTER`, Renderer `VehicleRenderers` (26.3).
- Daten 26.3: Loot (`mc26_3/generated`), Rezept `reinforced_hopper_from_crafting` + 5 Lorenrezepte (2 Schmiede), Rezept-Fortschritte,
  Blockstates/Modelle/Items (`src/main/generated`), Lorenmodelle (`mc26_3/overlay`), Texturen (`src/main/resources`), Tags
  (pickaxe, enderite_items/_ingot_tier), Fortschritt `machines/reinforcements`.
- Tests: `HopperTests` (12), `HopperAndTrimTests` (Trichterteil), `VehicleTests` (Trichterloren), Teile von `SledgehammerUpgradeTests`,
  `WorldUpgradeTests`, `BlockInfoTests`, `ModpackTests`; Client: `ModScreensClientTest`, `HudAndTooltipClientTest`, `VehicleClientTest`.

## 2. Schnitt (wie Stufe 2 / Stufe 4 Ovens)

Trichter und Trichterloren gehen zusammen (die Lore zeigt den Trichter, teilt Menü, Filter und Tempo). Eine Teilung wäre eine
künstliche Kopplung zwischen Modul und Kern.

- **26.2 behält alles** (Klassen bleiben, neuer Schalter `McVersion.HOPPERS_IN_SB`: 26.2 `true`, 26.3 `false`).
- **26.3:** das Modul registriert `simplemachines:{reinforced,netherite,enderite}_hopper` (Block+Item), BE-Typ `simplemachines:hopper`,
  drei Menütypen `simplemachines:<stufe>_hopper` (Stufe am Typ, keine Öffnungsdaten → keine Loader-Menü-API, gleiches Muster wie
  Simple Storages Truhenmenüs), Entities+Items `simplemachines:<stufe>_hopper_minecart`. SBs Felder `ModBlocks/ModItems.*_HOPPER*`
  zeigen auf die Modul-Objekte (`MachineRefs`, SB lädt das Modul zuerst – `initializeFirst` aus Stufe 2), `ModEntities.*_HOPPER_MINECART`
  werden `null` (wie die Ofenloren). SB registriert auf 26.3 weder BE-Typ noch Menü (Fabric/NeoForge/Forge: `if (HOPPERS_IN_SB)`, sonst
  `Set.of` mit Luft-Duplikaten auf NeoForge/Forge), Screen-/Renderer-Registrierung ebenso geschützt.
- **Modul (Kopie, Drift über `tools/dup_pairs.txt`):** Paket `com.simplemachines.hopper`:
  `TierHopperBlock`, `TierHopperBlockEntity`, `TierHopperMenu` (Filter-Slots, `clickMenuButton` statt eigenem Paket – Vanilla-Weg wie
  Steinsäge/Verzauberung, loader-neutral), `TierHopperScreen` (Kastenstil über SimpleLib `UiBoxes/UiMotifs/UiFilterButton`, gleiche
  Maße), `HopperFilterMode`/`ItemFilter`/`FilterHopper` (Modul-Kopien; SB behält seine für den Crafter), `TierHopperMinecart`,
  `HopperCartItem`, `HopperItem` (Tooltip Tempo+Filter), `Hoppers` (Registrierung, Stufen, Rezeptlisten), `HopperUse` (Aufwertungs-Klick
  wie `OvenUse`), `HopperAutomation` (Haken: Fabric setzt Transfer-API-Einfügen, wie SBs `FabricItemAutomation`).
  SB-Abhängigkeiten ersetzt: `WorldPermissions.mayAutomate` → `MachineProtection`, `ServerTuning.hopperCooldown` → Modul-Config,
  `TieredChests.maxStackSize` → `max(Vanilla, container.getMaxStackSize(stack))` (Truhen-Mixin für das eigentliche Einfüllen bleibt in SB
  und greift über `HopperBlockEntity.addItem`), `SledgehammerUpgrades.shouldSkipBlockUse` → SimpleLib `InWorldUpgrades`/`toolWants`.
- **Config:** `config/simplemachines-server.json` bekommt `hoppers.{reinforcedSpeed, netheriteSpeed, enderiteSpeed}` (1–8, Standard 2/4/8);
  fehlt der Abschnitt, einmal aus `simplebuilding.json` `server.machines.*HopperSpeed` übernommen. SB blendet die drei Optionen auf 26.3
  aus (`ConfigOptions.MOVED_TO_MACHINES_HOPPERS`, Flag `HOPPERS_IN_SB`).
- **IdMoves:** ITEM+BLOCK der drei Trichter, ENTITY_TYPE+ITEM der drei Loren, BLOCK_ENTITY_TYPE `simplebuilding:mod_hopper` →
  `simplemachines:hopper` (BE-NBT-Schlüssel gleich: Inhalt, Filtermodus, Abklingzeit bleiben), RECIPE+ADVANCEMENT der sechs Rezepte
  (`recipes/redstone/reinforced_hopper_from_crafting`, `recipes/transportation/<lore>`).
- **Daten 26.3:** SB-Datagen überspringt die Trichter (`HOPPERS_IN_SB`); `mc26_3/generated` von Hand: Loot/Rezepte/Rezept-Fortschritte
  weg, `removed-on-26.3.txt` ergänzt (Blockstates/Modelle/Items/Rezept/Loot aus `src/main/generated`), Tags/Fortschritt/in_world_upgrades
  auf `simplemachines:`. Modul liefert Blockstates/Modelle/Items, Texturen (git mv), Loot, Rezepte (Enderit-Schmiede mit
  `load_conditions` simpleores), `in_world_upgrades`, Tags, Lang EN/DE.
- **Enderit (MACH32-ENDSYS):** Enderit-Trichter/-Lore entstehen nur mit Simple-Ores-Material (Hammer-Klumpen, Schmiede-Rezept bedingt);
  `fabric.mod.json`: simpleores von „suggests“ nach „recommends“.
- **Tab/Preise/Wiki/Guide:** Modul-Tab um die Trichter- und Lorenreihe ergänzt, Vanilla-Tabs (nach Trichter bzw. Trichterlore),
  SB-Tab-Zeilen unverändert (Felder = Modul-Objekte); Preise simplemoney, Wiki-Manual EN/DE ins Modul, Buchseite im Modul-Guide,
  Discover, Testzentrale, modtree, MODS-UND-FEATURES.
- **Tests:** `HopperTests` (12) ins Modul als `HopperTests` (Adapter `module_game_test_*`), dazu `old_hopper_ids_move` (BE-NBT-Ladung),
  `hopper_config_adoption`, Lorentests (Tempo, Filtertaste, Rezepte). SB-Tests, die SB-Klassen casten, überspringen auf 26.3
  (`if (!HOPPERS_IN_SB) succeed`); Tests, die die Trichter nur als Blöcke nutzen (Tempo, Öfen beschicken, Enderit-Tempo), bleiben als
  Zusammenspiel-Tests SB+Modul.

## 3. Bewusst offen (Memory/Queue)

- Jade-Filteranzeige (`BlockInfo.HOPPER_FILTER`) kennt nur SBs Trichter → auf 26.3 vorerst ohne (wie Stufe-2-Jade-Haltbarkeit).
- Client-Tests (`ModScreensClientTest`/`HudAndTooltipClientTest` Trichterteil) überspringen auf 26.3; Modul-Client-Smoke fehlt noch.
- Lorentextur `item/<stufe>_hopper_minecart` fehlt schon vorher (auch bei den Ofenloren) – nur übernommen, nicht gelöst.
- NeoForge/Forge 26.3: Modul ist Fabric-only (wie Stufe 2), dort fehlen Trichter/Loren.

## 4. Risiken

- Kein lokaler Compile: alles im einen sbt-Lauf (alle Ziele), ein Nachlauf für Fixes.
- BE-Typ-Wechsel: alte Chunks tragen `simplebuilding:mod_hopper` → IdMoves BLOCK_ENTITY_TYPE (wie Ovens), geprüft im Modul-Test.
- Menü-Klick über `clickMenuButton`: der Server prüft `stillValid` und Menü-Id; Test treibt `clickMenuButton` direkt.

## 5. Verifikation

Lokal: check_loot_format --strict, check_dup_impl (+ `--baseline` für die bewussten 26.2-Zwillinge), check_tooltip_text,
check_item_properties, check_lang_keys, check_load_conditions, multimod, modtree --check, simplemoney check_data. sbt: Compile aller Ziele
(`:compileJava :mc26_3:fabric:compileJava :mc26_3:neoforge:compileJava :mc26_3:forge:compileJava`, `:modules:simplemachines:fabric:compileJava`),
Modul-Gametests + standalone, `fabric-263` gefiltert auf `migration_world*`, `data_integrity*`, `test_centre*`, `modpack*`, `*hopper*`,
`check_client_mixins`, `wiki/generate.py --all --check`.
