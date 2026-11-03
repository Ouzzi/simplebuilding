# Plan MACH32-MOD Stufe 2 (Sitzung B, b-machines02, 03.11.) – Kolben-Upgrades nach Simple Machines

Auftrag: /root/sb-tools-b/b02-auftrag.md, Stufe 2 aus docs/ai/PLAN-B-MACHINES01-2026-11-01.md. Besitzer MACH32-MOD: „alle die in simple
machines tab liegen in simple machines submod legen“. MACH32-ENDSYS: End-Systeme bleiben, wo sie sind (nicht Teil dieser Stufe).
Basis `hub/claude-wave1` d4e396636.

## 1. Wahl des Blocks

Kolben-Upgrades (Vorschlag des Plans 01 als Stufe 2) statt Trichter: Trichter ziehen Block-Entities, Menüs/Screens, Netzwerk und
Trichterloren nach (~200 Referenzen plus Client-UI); die Kolben sind Blöcke ohne Block-Entity, ihr Verhalten sitzt in vier Mixins
und zwei Hilfsklassen. Klar abgegrenzt: Zeile `pistons` des Tabs (Verstärkt, Verstärkt klebrig, Netherit, Enderit) + drei Köpfe.

## 2. Ist-Stand

- SB-Kern (`common/src/shared`, baut für 26.2, 26.3 Fabric/NeoForge/Forge): `ReinforcedPistonBlock`, `NetheriteBreakerPistonBlock`,
  `EnderitePistonBlock`, `ModPistonHeadBlock`, `NetheritePistonItem`, `PistonBreach`, `PistonBoreEffects`, `PistonBreakGuard`
  (+ `FabricPistonBreakGuard`), `PistonConfigPayload`, Mixins `PistonBlockMixin`, `PistonHandlerMixin`, `PistonHeadBlockMixin`,
  `MovingPistonExplosionMixin`, client `PistonHeadRendererMixin`; Optionen `breakerPistonsLoseDurability`,
  `pistonsBreachEndPortalFrames`, `pistonsBreachModdedUnbreakables`, `pistonsFireBreakEvents`; Tags `piston_breach_immune`,
  `piston_breachable_extra`; Sound `block.piston.bore`; Hammer-Aufwertungen (in_world_upgrades) Verstärkt→Netherit→Enderit;
  Fortschritt `good_as_new` (Reparatur). Tests: `PistonBreachTests` (20), Teile von `GravityBlockTests`, `BlockBehaviourTests`,
  `BlockInfoTests`, `EnderiteMachineTests`, `AdvancementTriggerTests`, `SledgehammerTests`, `SledgehammerUpgradeTests`.

## 3. Schnitt (Muster Stufe 4 Ovens / IG31-13 Ores)

- **26.2 behält alles** (Klassen bleiben in `common/src/shared`, Schalter `McVersion.PISTONS_IN_SB`: 26.2 `true`, 26.3 `false`).
- **26.3:** das Modul registriert die sieben Blöcke und vier Items als `simplemachines:*`. SBs Felder `ModBlocks/ModItems.*PISTON*`
  zeigen auf die Modul-Objekte (`MachineRefs`, wie `SharedMaterials` für Simple Ores; SB ruft den Modul-Initializer zuerst,
  `initializeFirst("simplemachines")`, das Modul schützt sich gegen den zweiten Aufruf). So bleiben Tab-Zeile, Testzentrale,
  Hammer-Tabelle, Buch und Fortschritte ohne Umbau richtig. SBs Piston-Mixins schaltet `AliasMixinPlugin` auf 26.3 ab.
- **NeoForge/Forge 26.3:** Modul ist Fabric-only (wie Ovens/Gadgets, Besitzer „nur 26.3 Fabric“): dort fehlen die Kolben; kompiliert
  bleibt alles (Felder werden Luft).
- **Modul (Kopie, Drift über tools/dup_pairs.txt):** Paket `com.simplemachines.upgrade` mit denselben Klassen, SB-Abhängigkeiten ersetzt:
  `WorldPermissions.mayAutomate` → framework `Protection.allowsAutomation` (`MachineProtection`); `PlatformServices.mayPistonMove/Break`
  → `PistonGuard` (Fabric: `PlayerBlockBreakEvents` mit FakePlayer, Weltgrenze); Klumpen per Id (`simplelib:netherite_nugget`,
  `simpleores:enderite_nugget`, ohne Simple Ores Luft = keine Reparatur); Enderit-Haltbarkeit 2530 fest (SBs `ModToolMaterials`);
  Fortschritt über SimpleLib `GadgetActions.fire("piston_repair")`, SB hört zu; Hand-Hinweis über `GadgetActions.registerItemHint`
  für beide Klumpen; Hammer: `HammerActions.registerUpgradedState`. Eigener Sound `simplemachines:block.piston.bore`, eigene Tags
  `simplemachines:piston_breach_immune/piston_breachable_extra`, eigenes Payload `simplemachines:piston_config`.
- **Config:** `config/simplemachines-server.json` bekommt `pistons.{breakerPistonsLoseDurability, breachEndPortalFrames,
  breachModdedUnbreakables, fireBreakEvents}`; fehlt der Abschnitt, werden die Werte einmal aus `simplebuilding.json` übernommen.
  SB blendet die vier Optionen auf 26.3 aus (`ConfigOptions.MOVED_TO_MACHINES`).
- **IdMoves:** `simplebuilding:{reinforced_piston, reinforced_sticky_piston, netherite_piston, enderite_piston}` ITEM+BLOCK,
  `*_piston_head` BLOCK, Rezepte `reinforced_piston`, `reinforced_sticky_piston` + `recipes/redstone/<id>` → `simplemachines:` gleich.
  Blockzustände (facing/extended/wear/wear_step) sind gleich, Schaden am Item bleibt.
- **Daten 26.3:** SB-Datagen überspringt die Kolben (`PISTONS_IN_SB`); `mc26_3/generated` von Hand angepasst (Loot/Rezepte/
  Rezept-Fortschritte weg, `removed-on-26.3.txt` ergänzt, Tags/Fortschritte/in_world_upgrades auf `simplemachines:`), Modul liefert
  Blockstates/Modelle/Items/Texturen (git mv aus SB, wie Ovens), Loot, Rezepte, Hammer-Datei, Tags, Lang EN/DE.
- **Tests:** SB-Kolbentests überspringen auf 26.3 (`if (!PISTONS_IN_SB) succeed`), das Modul übernimmt sie (`PistonUpgradeTests`,
  `module_game_test_*`) + `old_piston_ids_move`.
- **Doku:** Wiki-Manual (EN/DE) ins Modul, MODS-UND-FEATURES, modtree, Preise (4 Einträge von simplebuilding nach simplemachines).

## 4. Risiken

- Kein lokaler Compile: alles in einem sbt-Lauf (Compile 26.2 + 26.3 Fabric/NeoForge/Forge + Modul). Nachlauf für Fixes.
- Reihenfolge der Initializer (Felder vor Modul-Registrierung = Luft): `initializeFirst` wie bei Simple Ores; Test `registered`.
- Jade-Haltbarkeitsanzeige und JEI/Wiki-Reparaturexport (`InWorldTransformations.pistonRepair`) kennen nur SB-Klassen → auf 26.3
  vorerst ohne Modul-Kolben (offen, Memory).

## 5. Verifikation

Lokal: check_loot_format --strict, check_dup_impl, check_tooltip_text, check_item_properties, check_lang_keys, multimod, modtree --check,
simplemoney check_data, simplemachines check_data. sbt: Compile (`:compileJava :mc26_3:fabric/neoforge/forge:compileJava`,
`:modules:simplemachines:fabric:compileJava`), Modul-Gametests + standalone, `fabric-263` gefiltert auf `simplebuilding:migration_world*`,
`data_integrity*`, `test_centre*`, `piston*`, `check_client_mixins`, `wiki/generate.py --all --check`.
