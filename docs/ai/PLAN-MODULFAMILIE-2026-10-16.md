# Plan: Modulfamilie SimpleBuilding (Erweiterung, 2026-10-16)

Nur Plan, kein Code. Baut auf `PLAN-MODULAUFTEILUNG-2026-10-11.md` (Stufen 1a-4 erledigt, 5-9 offen),
`KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md`, `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md` und AGENTS.md Regel 16 auf.
Besitzer-Vorgabe 16.10.: SimpleBuilding (SB) ist Super-Mod einer Familie; alle Baublöcke gehören zur Familie;
Gemeinsames liegt in simplelib; alles bleibt kompatibel; Varianten schalten sich frei, sobald das passende Modul läuft.
Zahlen sind per grep belegt (Stand Branch claude-splitplan2): `ModItems` 245 `registerItem`, `ModBlocks` 156
`registerBlock`; Lang `en_us` (mc26_3 overlay) Schlüsselgruppen siehe Tabelle.

## 1. Zielstruktur

| Sub-Mod | ID | Inhalt (Gruppen, ca. Anzahl Lang-Schlüssel item+block) | Abhängigkeiten | nach simplelib |
|---|---|---|---|---|
| **SimpleBuilding** (Super-Mod, bleibt) | `simplebuilding` | Config-Oberfläche, Mega-Guide, Testzentrale-Rahmen, Tabs, Bauzauberstäbe (7 `wand`), Meißel (17), Blaupausen (3), Oktant, Farbpinsel, Astral-Verzauberer, Schach (157), Hängematten (33), Pads (20), Platten/Musik (9 `disc`), Trichter, Eimer (26) | Pflicht: simplelib, framework, cloth-config + gebündelte Pflicht-Sub-Mods. Optional (Schalter `enableSimpleX`): alle Sub-Mods | - |
| **Simple Octets** (Stufe 6) | `simpleoctets` | 1/8-Blöcke: ca. 117 Items / 106 Blöcke (352 Lang-Schlüssel mit `octet`), 12 Holz, ca. 92 Material, 377 Steinsäge-Rezepte; Octet-Zellen. Schach-Octets bleiben SB (F3) | Pflicht simplelib; optional simplehammers (Schnitzen), simpleblocks/simpleend (Material-Gruppen) | `OctetCells` (Dienst), Octet-Generator-Schema |
| **Simple Blocks** (läuft: Branch `claude-missingblocks`, N69) | `simpleblocks` | fehlende Block-Varianten: Hushed Deepslate Tiles, Sculk Deepslate, fehlende Treppe/Stufe/Mauer + rissig/bemoost (KONZEPT-MODREF-BLOECKE M1-M5); heute noch in SB geplant, wird auf Familie umgelenkt (Regel 16) | Pflicht simplelib; optional simpleoctets (Octet je neuem Block), simpleend (polierter Endstein-Familie) | `MaterialVariants`-Helfer (Treppe/Stufe/Mauer/rissig/bemoost aus einem Basisblock), falls Simple End/Enderite ihn auch brauchen |
| **Simple End** (existiert) | `simpleend` | Endholz (Ashwood/Duskwood), Knospen, Gras, Bäume, Biome, Nibblemite (siehe `EndRegistry`); bündelt `simpleenddragonfight`. **Neu:** End-Baublöcke (polierter Endstein + Treppe/Stufe/Mauer, Astral-/Nihil-Endstein bleiben vorerst SB, siehe F4) | Pflicht keine; optional simplelib, simpleblocks, simpleoctets | `polished_end_stone` + Tags (Nutzer: Blocks, End, Enderite, Octets) |
| **Simple Enderite** (später, Stufe E) | `simpleenderite` | Enderit-Werkstoff und -Ausrüstung: Sword/Spear/Pickaxe/Axe/Shovel/Hoe, `enderite_core`, `enderite_horse_armor`/`nautilus_armor`, Rüstung (`ModArmorMaterials`), Äpfel/Karotte, Bündel/Köcher-Variante nur Hook; Dimensions-Schrott-Erz-Bezug (`DimensionalScrapBlock`); 78 Lang-Schlüssel `enderite` gesamt, davon ca. 12 Ausrüstung | Pflicht simplelib; optional alle Stufen-Mods (siehe 2.3) | Grundmaterial bleibt in simplelib (Stufe 3d erledigt, §2.5 Hauptplan) |
| **Simple Tools** (Namenskollision!) | `simpletools` | **heute QoL-Sub-Mod**: Soulbound-Verzauberung, Todes-Tresor, Amboss-Mixins, volle Haltbarkeit, Thrift, Befehle (kein einziges Werkzeug-Item; `ToolsRegistry` 0 Items). Werkzeug-Items (Hämmer, Meißel, Stäbe) sind NICHT drin | Pflicht cloth-config; optional simplebuilding, simplequalityoflife | - |
| **Simple Gadgets** (neu) | `simplegadgets` | Gerätschaften: `rotator`, `velocity_gauge` (Messuhr), `detector`, `magnet`, `octant`, Echolot (neu), Attractor (aus Claims/Tweaks `claims`, heute simpletweaks), Farbpinsel, Resonanzstab/Eisenstab (Rotator-Zutat) | Pflicht simplelib; optional simpleoctets (Rotator dreht Octets), simplehammers | Tag `#simplelib:gadgets`, Gerät-Tooltip-Helfer |
| Simple Hammers (Stufe 7) | `simplehammers` | 7 Hämmer, 13 Kernklassen, Hammer-Verzauberungen, Schnitzen, In-World-Crafting | wie Hauptplan §2.2 | `in_world_upgrades`, `isHammer` (3c erledigt) |
| Simple Storage (Stufe 8) | `simplestorage` | ca. 50 Items / 36 Blöcke, 7 BE, 3 Menüs | wie Hauptplan | Vorlagen (3d erledigt) |
| Simple Ovens (4 erledigt) / Crucibles (5) | `simpleovens`/`simplecrucibles` | 9 Öfen+Loren / Tiegel, Fässer, Seelenlava | wie Hauptplan | - |

Abgrenzung: Alles, was gebaut wird (Block-Varianten) -> Blocks/Octets/End/Enderite-Blöcke. Alles, was man in der Hand hält und
benutzt (Messen, Drehen, Anziehen, Orten) -> Gadgets. Hämmer/Meißel/Stäbe (Baugeräte) bleiben bei Hammers bzw. SB (F2).
Familienregel: SB ist nur Hülle; neue Baublöcke werden nie in SB selbst, sondern im passenden Sub-Mod angelegt (Regel 16).

## 2. Kopplungsanalyse

### 2.1 Enderit quer durch SB (grep)
186 Java-Dateien in `common/src/shared` nennen "enderite" (gametest 70, util 18, tweaks 17, items 16, blocks 15, mixin 10,
client 6, fluid 5, dev 5, entity 3, compat 3, guide 2). Wichtige Knoten:
- **Items** (`ModItems`, ca. 33 `enderite_*`): Material (nugget/scrap/ingot/block/shulker_shell, 3d: -> simplelib), Werkzeuge
  (6 + Speer), Stufen-Blöcke (chest, trapped_chest, shulker_box, furnace/smoker/blast_furnace, hopper, piston, bundle,
  quiver, backpack, paint_box), Fahrzeuge (chest/furnace/hopper minecart, chest_boat), Eimer, Rüstung, Pferde-/Nautilus-
  Rüstung, `enderite_core`, `enderite_pole`.
- **Material-Klassen:** `ModToolMaterials`, `ModArmorMaterials`, `ChestTier`, `FurnaceTierPerks`, `ModHopperBlockEntity`,
  `MetalRodBlock`, `ModPistonHeadBlock`, `PlacedBundleBlock`, `DimensionalScrapBlock`, `SledgehammerItem`, `ChiselItem`,
  `BuildingWandItem`, `QuiverItem`, `ReinforcedBundleItem`.
- **Tabs/Suche:** `ModItemGroupsContent`, `SearchTabPlacement`, `DevEnchantedTab`, `InfoTooltips` kennen Enderit-Items statisch.
- **Tests:** 70 Gametest-Dateien referenzieren Enderit (einziger Teil mit hoher Streuung).
- **Fremd-IDs:** simplemoney `prices.json` (275 SB-Einträge), Riding (feste ID `enderite_*`), Sandwiches (Tag).

### 2.2 Weitere Querverbindungen der Familie
- `polished_end_stone`: `ModBlocks` 370 / `ModItems` 179 / `ModItemGroupsContent` 355 (SB), Rezepte/Octets/End-Holz-Plan
  nutzen es -> Kandidat simplelib (Regel 16) mit Alias `simplebuilding:polished_end_stone`.
- Rotator/Messuhr/Detektor/Magnet (`ModItems` 716-722) hängen an Octet-Zellen (Rotator), `ModMessageHandlers`
  (Pakete) und `ModItemGroupsContent`; Attractor liegt in `simpletweaks/claims` (Name bitte klären, F5).
- Hammer <-> Octets (Zyklus H<->O), Storage/Ovens <-> Upgrade-Vorlagen: durch Stufen 3c/3d bereits gelöst.
- Tag-Altlasten: `simplebuilding:sledgehammer_tools` in simplelib und simpletrims (Alias bleibt >= 2 Versionen).

### 2.3 Bedingte Freischaltung (technisch)
Regel: **Registrierung nie vom Laden abhängig machen** (Welten ladbar, kein Datenverlust, Hauptplan §2.5). Es wird nur
*Bezug/Sichtbarkeit/Rezept* geschaltet.
1. **Besitzer-Mod registriert immer**, auch wenn Partner fehlt: Enderit-Blöcke/-Items eines Sub-Mods (z. B. Enderit-Truhe in
   Storage) bleiben registriert; das *Material* kommt aus simplelib (3d). Grundregel gilt weiter für Stufen-Mods.
2. **Enderit-Ausrüstung (Simple Enderite):** registriert nur in `simpleenderite`. Andere Mods bieten ihre Enderit-**Variante**
   über **Tag + optionales Rezept** an: Rezept-JSON mit Condition `fabric:all_mods_loaded`/`neoforge:mod_loaded`-Äquivalent
   über vorhandene `simplelib:material_requested`/`mod_loaded`-Conditions (3c/3d), Zutat als Tag (`#c:ingots/enderite`,
   `#simplelib:enderite_tools`). Fehlt Simple Enderite, ist das Rezept inaktiv, der Block bleibt (nur Stufen-Blöcke, siehe 1.).
3. **Reine Variante ohne Registrierung beim Partner:** Variante lebt im *Variantenmod* (z. B. Enderit-Hammerkopf in
   Simple Enderite, mit `hammer_tier`-Komponente + `in_world_upgrades`-Eintrag im eigenen Namespace). Hammers kennt Enderit
   nur als Tag/Datentyp -> keine Klassenabhängigkeit.
4. **Java-Schnittstellen:** nur reine Verträge in `framework`/`simplelib` (`SubMods.isLoaded(id)`, `MaterialRequests`,
   `TransformHints`); Fremdcode nur über `Class.forName`/Services, nie Import (Regel 6).
5. **Tabs/Guide/JEI:** Fremde Items nur über ID-Listen (fehlende IDs übersprungen); jedes Sub-Mod eigener Tab
   (`CreativeTabSettings`), SB-Suchtab ordnet nur nach ID.
6. **Fremd-IDs bei Umzug:** `IdMoves` (3a, erledigt) je Registry (ITEM, BLOCK, BE, ENTITY, FLUID, COMPONENT, ENCHANTMENT),
   Rezeptbuch-/Advancement-Remap, Config-Übernahme; Tag-Altdateien bleiben 2 Versionen. Alle generierten Tabellen aus
   `tools/split/id_moves.py` (Verschiebeliste).
7. **Money:** pro Sub-Mod `money/<submod>/prices.json` + `links:<submod>`-Condition (Hauptplan).

## 3. Reihenfolge der Stufen

Grundsatz wie Hauptplan: eigener Branch von `claude-wave1`, je Stufe gate-grün mergbar, strikt nacheinander ab Stufe 5
(gemeinsame Registrierungsdateien), Gate über `/root/sb-gate.sh`, alle Standalone-Targets, Migrationstargets
`migration-*-263`. Neue Stufen sind eingeschoben (P = neu).

| Stufe | Branch (Vorschlag) | Umfang | Risiko | Tests | Migration | Gate |
|---|---|---|---|---|---|---|
| **P1** (jetzt) | `claude-missingblocks` fortführen | N69 in **Simple Blocks** (nicht SB): Modul-Gerüst `simpleblocks`, `modules.json`, `SubMods`-Schalter, Generator, 1. Block Hushed Deepslate Tiles | gering (neue IDs, nichts verschoben); Doppelregistrierung falls SB-Variante schon existiert | Standalone `simpleblocks`, Modul-Targets, Wiki-Check | keine (neu) | Gate, Standalone |
| 5 | `claude-split-s5-crucibles` | wie Hauptplan | Sandwiches-Abhängigkeit, Seelenlava-Alias | `CrucibleTests`, `LibTests`, Sandwiches mit/ohne | Alias `simplebuilding:`/`simplelib:` -> `simplecrucibles:` | Gate + Standalone + Migration |
| P2 | `claude-split-sP2-endstone` | `polished_end_stone` nach simplelib (+ Alias), Simple End bekommt End-Baublock-Familie (Treppe/Stufe/Mauer), Octets-Zugriff per Tag | gering-mittel (Rezepte, Octets, Endholz-Plan nutzen es) | `EndTests`, Block-Daten-Check, Standalone End | `IdMoves` `simplebuilding:polished_end_stone` -> `simplelib:` | Gate |
| 6 | `claude-split-s6-octets` | wie Hauptplan; erste Material-Gruppen Blocks/End über Tags | Masse (Lang/Modelle/Rezepte), Steinsäge-Rezeptbuch | `SledgehammerOctetTests`, `DataIntegrityTests`, Standalone | Aliase 106 Blöcke/117 Items, Rezepte | Gate + Migration |
| 7 | `claude-split-s7-hammers` | wie Hauptplan | breiteste Kopplung (~40 `instanceof`) | `SledgehammerTests`, Hammer+Octets+Ovens | Enchantment-Alias, Tag-Alias | Gate + Migration |
| P3 | `claude-split-sP3-gadgets` | Simple Gadgets: `rotator`, `velocity_gauge`, `detector`, `magnet`, `octant`, Farbpinsel, Echolot (neu), Attractor-Klärung; Pakete aus `ModMessageHandlers` teilen | mittel (Pakete/Menüs, Octant-UI, Tests in 70 Gametest-Dateien) | Gadget-Tests, `OctantUi`-Layout-JUnit, Standalone | ITEM-Alias, Komponenten, Money-Tabelle | Gate + Migration |
| 8 | `claude-split-s8-storage` | wie Hauptplan | größter Umzug, Pakete, Layout-Aliase | Storage-Tests, Layout-JUnit | Aliase ~50 Items/36 Blöcke/7 BE | Gate + Migration |
| P4 | `claude-split-sP4-enderite` | Simple Enderite: Werkzeuge/Rüstung/Äpfel/Pferderüstung/`enderite_core`/Dimensions-Schrott-Bezug nach `simpleenderite`; Stufen-Mods bleiben Eigentümer ihrer Enderit-Blöcke; Varianten über Tags + Conditions (Abschnitt 2.3) | mittel-hoch: 186 Dateien/70 Tests nennen Enderit, `ModToolMaterials`/`ModArmorMaterials`, Money-Preise, Riding feste ID | `EnderiteMachineTests` aufteilen, Standalone Enderite (mit/ohne Storage/Ovens/Hammers), Conditions-Test | Aliase ITEM + Rüstungs-/Werkzeug-Komponenten, Rezepte | Gate + Migration + Probelauf mit/ohne Partner |
| 9 | `claude-split-s9-release` | Aufräumen: Tabs/Suchtab/Guide/Testzentrale nur Verträge; Wiki-Umzug; Versionen (Bündel Familie) | Jar-Größe/Startzeit | volles Gate, alle Standalone, alle Migrationstargets | Brücken-Version 1.3.x-bridge | Volles Gate |

Einordnung: P1 läuft parallel (neues Gerüst, keine Konflikte). P2 vor 6, weil Octets/Blocks/End das Endstein-Material
brauchen. P3 nach 7 (Octant/Rotator brauchen Hammers/Octets-Verträge), vor 8 (Pakete sauber teilen). P4 vor 9; alle
Stufen mit bestehender Migrationswelt (`testing/worlds/split-before-1.3.x`) plus je eine zusätzliche Welt-Probe mit
Enderit-Inhalten (Truhe, Ofen, Werkzeug, Rüstung) vor und nach P4.

Allgemeine Risiken: nie zwei schwere Gradle-Läufe parallel (CT-Regel); jede Verschiebung im selben Commit alte
Registrierung entfernen; Forge-Registry-Aliase prüfen; Lang/Modelle/Tex der Gruppen mit verschieben (nur additiv an
gemeinsamen Dateien).

## 4. Offene Besitzer-Fragen (mit Empfehlung)

- **F1 Namenskollision "Simple Tools":** `simpletools` ist heute QoL-Sub-Mod (Soulbound, Todes-Tresor, Amboss, Haltbarkeit).
  Soll "Simple Tools" für Werkzeug-Items frei werden? *Empfehlung: bestehendes `simpletools` behalten; Werkzeug-Familie
  heißt "Simple Gadgets" (Geräte) bzw. bleibt bei Hammers/SB; kein zweites Tools-Mod.*
- **F2 Wohin Meißel, Bauzauberstäbe, Blaupausen, Hängematten, Pads?** *Empfehlung: bleiben in SB (Super-Mod-Kern "Bauen");
  nur Rotator/Messuhr/Detektor/Magnet/Oktant/Echolot -> Gadgets.*
- **F3 Enderite-Umfang:** nur Ausrüstung (Werkzeuge, Rüstung, Äpfel) oder auch Stufen-Blöcke (Truhe, Ofen, Hammer)?
  *Empfehlung: nur Ausrüstung + Dimensions-Schrott; Stufen-Blöcke bleiben in ihren Mods (Datenverlust-Regel); Grundmaterial
  simplelib.*
- **F4 Astral-/Nihil-Endstein, Astralit/Nihilith (27/19 Items):** zu Simple End/Enderite oder SB? *Empfehlung: bleiben SB
  (Astral-Verzauberer), nur polierter Endstein nach simplelib.*
- **F5 Attractor:** liegt heute in `simpletweaks/claims` (Claims-Werkzeug). Nach Gadgets oder dort lassen? *Empfehlung:
  dort lassen (Claims-Bezug), Gadgets nur anbinden; Besitzer bitte bestätigen, ob "Attractor" ein neues Gerät meint.*
- **F6 Reihenfolge:** P1 (Simple Blocks) sofort, dann 5, P2, 6, 7, P3, 8, P4, 9. *Empfehlung: so; Enderite zuletzt, weil
  höchster Streuschaden.*
- **F7 Namen/IDs:** `simpleblocks`, `simplegadgets`, `simpleenderite`, "Simple Octets" -> `simpleoctets`. *Empfehlung: so.*

## 5. Antworten Besitzer (2026-10-16, verbindlich)
F1 `simpletools` bleibt wie es ist (QoL: Soulbound, Todes-Tresor, Amboss …), wird aber Sub-Mod unter dem Super-Mod SimpleBuilding; Geräte kommen nach Simple Gadgets. F2 Meißel, Bauzauberstäbe, Blaupausen, Hängematten, Pads bleiben im SimpleBuilding-Kern („Bauen“). F3 Simple Enderite = nur Ausrüstung (Werkzeuge, Rüstung, Äpfel, Kern); Enderit-Stufen-Blöcke (Truhe, Ofen, Hammer) bleiben in ihren Fach-Sub-Mods und werden nur mit Simple Enderite freigeschaltet. F4 Astral-/Nihil-Endstein, Astralit/Nihilith → Simple Enderite. F5 Attractor → Simple Gadgets (aus simpletweaks/claims herauslösen). F6 Reihenfolge: Simple Blocks sofort, dann Tiegel (Stufe 5), polierter Endstein → simplelib, Octets (6), Hämmer (7), Gadgets, Lager (8), Enderite zuletzt, Release (9). F7 IDs simpleblocks, simplegadgets, simpleenderite, simpleoctets.
