# In-World-Umwandlungen – Inventar (2026-10-06)

Stand Branch `claude-hammer12`. Plan: `docs/ai/PLAN-HAMMER12-2026-10-06.md`. Gemeinsame Regeln für Schlag-Umwandlungen
in SimpleBuilding: `common/src/shared/java/com/simplebuilding/util/InWorldStrikes.java` (Rechtsklick, 8 Ticks Mindestabstand
zwischen gezählten Schlägen, Zählung je Position mit 100 Ticks Verfall, Risse + Block-Partikel + Schlagklang mit
steigender Tonhöhe). SimpleLib (ohne SB: Axt) hat dieselbe Riss-Rückmeldung (`CrucibleBlankBlock.crackStage`,
`CrucibleBarrelBlock.crackId`).

Legende Schritt: **Teil** = jeder Schlag bringt seinen Teil des Ergebnisses (Item), **Block** = Ergebnis ist ein Block,
der am Ende entsteht (Fortschritt sichtbar als Risse bzw. Rohling-Stufen), **1** = ein Schlag.

## Vorschlaghammer (SimpleBuilding)

| Umwandlung | Code | Auslöser | Nebenhand | Schläge | Rückmeldung | Schritt | JEI | GameTest (echter Pfad) |
|---|---|---|---|---|---|---|---|---|
| Maschinen/Truhen/Shulker aufwerten | `SledgehammerUpgrades` | Rechtsklick **halten** (Benutzung, Schlag alle 20 Ticks) | Klumpen / Rissiger Diamant | Config (5) × Faktor | Risse (gespeichert, neu gesendet), Amboss-/Netherit-Klang, Partikel | Block | `machine_upgrade` | `SledgehammerUpgradeTests` (`gameMode.useItemOn` + `connection.tick`) |
| Tiegel/Fass aufwerten (SimpleLib-Blöcke) | `SledgehammerUpgrades.crucibleUpgrades` | wie oben | wie oben, 2 Stück | 2× | wie oben | Block | `cauldron_world` (`hammer_upgrade/…`, neu) | `CrucibleTests.sledgehammerUpgradesCostDouble` (Tabelle), Aufwertungsweg wie Maschinen |
| Kessel → verstärkter Kessel | `SledgehammerUpgrades.crucibleUpgrades` | wie oben | 8 Rissige Diamanten (vorher 4) | 5 | wie oben | Block | `cauldron_world` (`cauldron_reinforce`) | `CrucibleTests` (Tabelle/JEI) |
| Umformen Block→Treppe→Stufe, Ecken | `SledgehammerItem#useOn/finishUsingItem` | Rechtsklick-Ladung | – | 1 Ladung | Abbauklang, Partikel | Block | `reshape` | `SledgehammerTests` |
| Diamantblock → 81 Diamantkiesel | `SledgehammerItem#strikeDiamondBlock` | Rechtsklick (je Klick ein Schlag) | – | 8 | gemeinsam (`InWorldStrikes.feedback`), Metallbruch am Ende | Teil (Punkt 4) | `diamond_crush` | `SledgehammerTests` |
| Quarzblock → 4 Quarz | `SledgehammerItem#crushQuartzBlock` | Rechtsklick | – | 1 (Punkt 4: 4) | Gold-Erz-Bruch, Partikel | Teil (Punkt 4) | neu in `diamond_crush` (Punkt 4) | Punkt 4 |
| Eis/Packeis/Obsidian/Feuerkugel → Splitter | `SledgehammerChips#tryCrush` | Rechtsklick (auch auf Haufen) | – | 1 (Punkt 4: je Item) | Glasbruch, Partikel | Teil (Punkt 4) | Info-Seiten; Blöcke neu in `diamond_crush` (Punkt 4) | Punkt 4 |
| Besatz-Vorlage + Leuchttinte/Glowstone | `PlacedTemplates#strike/hit` | **Rechtsklick** (vorher Linksklick) | Leuchttinte / Glowstonestaub (26.3: 2 + Inventar-Material) | 5 | Risse, Amethyst-Klang steigend, Material-Partikel | Block (Vorlage wird getauscht) | `trim_template` | `PlacedTemplateTests.placedTrimTemplateUpgradesOnRightClickOnly` (neu) |
| Schallplatte wenden | `DiscFlips#flip` | Rechtsklick auf abgelegte Platte | – | 1 | Amboss + Glocke, Noten | 1 | `disc_flip` | `MusicDiscTests.hammerFlipsPlacedDiscBackAndForth` |
| Eisen-Tiegel bauen | `CrucibleCompat#hammerUse` → `CrucibleBlankBlock#strike` | Rechtsklick | 4 Wägeplatten, dann 2 Stäbe/Barren | 6 | Rohling-Stufen + Risse (neu), Amboss steigend | Block | `crucible_build` | `CrucibleTests.sledgehammerBuildsTheIronCrucible` |
| Fass an Tiegel | `CrucibleBarrelBlock#attachStrike` | Rechtsklick | – | 6 | Risse, Kupferklang | Block | `barrel_attach` | `LibTests.barrelAttach` |

## SimpleLib-Axtwege (nur ohne SimpleBuilding)

| Umwandlung | Code | Auslöser | Nebenhand | Schläge | Rückmeldung | JEI |
|---|---|---|---|---|---|---|
| Tiegel bauen | `AxeBuildMixin` → `CrucibleBlankBlock#strike` | Rechtsklick | Platten/Griffe | 6 | Stufen + Risse | – (SimpleLib hat kein JEI) |
| Tiegel/Fass aufwerten | `CrucibleUpgrades#strike` | Rechtsklick | Diamanten / Netheritbarren | 10 | Risse (neu), Amboss, Partikel | – |
| Kessel → verstärkt | `CrucibleUpgrades#strike` | Rechtsklick | 8 Diamanten (vorher 4) | 5 | Risse (neu), Amboss | – |
| Fass anbringen | `CrucibleBarrelBlock#attachStrike` | Rechtsklick | – | 6 | Risse | – |

## Weitere (ein Klick, keine Schlagfolge)

Kern-Erzumwandlung (`CoreOreTransmutation`, `core_ore`), Schere auf Wolle (`shear_wool`), Meißel/Spachtel (`chisel`),
Kolben-Reparatur (`piston_repair`), Kupfer-Druckplatte wachsen/schaben (`copper_plate`), Rotator (`rotate`),
Constructor's Touch (`constructors_touch`), Shulkerschale + Klumpen (`shell_upgrade`), Oktant im Kessel waschen
(`cauldron_wash`), fallender Amboss auf Diamantblock (`diamond_crush`, ohne Spieler). Alle Rechtsklick.

Module: SimpleSandwiches (Schneidebrett, Messer auf Melone/Kuchen, Scheibenblock, Milchkessel mit Butter/Käse in
`cauldron_world`) – alles Rechtsklick, ein Klick je Schritt. SimpleQualityOfLife (Hacke ernten, Ofen mit Lava füllen)
und SimpleDimensions (Portal zünden) sind keine Werkzeug-Umwandlungen im Hammer-Sinn.

## Offen / bewusst nicht geändert
- Maschinen-Aufwertungen bleiben „Rechtsklick halten“ (eigene, gespeicherte Fortschrittslogik mit Rissen) – ebenfalls
  Rechtsklick, nur mit Halten statt Einzelklicks.
- SimpleLib-Fass-Aufwertung per Axt teilt den Schlagzähler mit dem Anbringen (`CrucibleUpgrades#strike`,
  `addAttachStrike`) – nicht angefasst, Folgepunkt.
- Splitter aus einer Feuerkugel bleiben JEI-Info-Seite (Eingabe ist ein abgelegtes Item, kein Block).
