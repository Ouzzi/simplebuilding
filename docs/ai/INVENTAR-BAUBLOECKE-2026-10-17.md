# Inventar: Baublöcke in SimpleBuilding und Kreativtab-Plan, 17.10.2026

Anlass: Besitzer 17.10.: „Simple Blocks enthält alle Baublöcke, die keinen anderen fachlichen Ort haben“ (`docs/ai/PRINZIPIEN.md`, Regel B1;
verfeinert AGENTS.md Regel 16) und: es soll **ein** Kreativtab für Baublöcke geben. Nur Inventar und Plan, kein Code.

Quellen: `ModItemGroupsContent.buildingBlocksRows()` (Tab „SimpleBlocks“ = `Tab.BUILDING_BLOCKS`, lang `itemgroup.simplebuilding.building_blocks`),
`ModBlocks`/`ModItems`, `SearchTabPlacement` (Einträge in Vanilla-Tabs), Stand `claude-wave1` (cb6c0f8a0); die Octet-Zeilen laufen bereits nach
`modules/simpleoctets` (Branch `claude-s6-octets`, Stufe 6).

## Maßstab „fachlicher Ort“
Ein Block hat einen anderen Ort, wenn (1) ein eigenes Modul oder ein geplantes Sub-Mod der Familie sein Thema trägt (Octets, End/Enderite,
Schach, Holzwerk/Hämmer, Astral-Verzauberer, Mobs, Redstone-Linien) oder (2) er Lager/Maschine/Werkzeug-Teil ist (Regel 16: bleibt wo er ist).
Alles Übrige – reine Bau- und Dekoblöcke ohne Themenmodul – geht nach `simpleblocks`.

## Zeilen des Tabs „SimpleBlocks“ und Entscheidung

| Zeile (Tab) | Inhalt | Items | Ziel | Grund |
|---|---|---:|---|---|
| `end_stones` | polierter Endstein, Astral-/Nihil-Endstein | 3 | bleibt (später simplelib/Simple End) | End-Thema; polierter Endstein → simplelib (Plan §2.2), Astral/Nihil bleiben vorerst SB (F4) |
| `astralit`, `astralit_alternates`, `polished_astralit` | Astralit-Palette inkl. Treppe/Stufe/Mauer, Säule, Purpur | 15 | bleibt (später Simple Enderite) | Palette hängt an Astralit-Erz/Enderite-Plan |
| `nihilith`, `nihilith_alternates`, `polished_nihilith` | Nihilith-Palette | 15 | bleibt (später Simple Enderite) | wie oben |
| `ender_quartz`, `polished_ender_quartz` | Enderquarz-Palette | 13 | bleibt (später Simple End/Enderite) | End-Material |
| `checkers`, `chess_*` | Quarz-Schachbretter, Treppen/Stufen, Schach-Achtel, Figuren | 12 + Figuren | bleibt SB | Schach (F3), Achtel bleiben dort |
| `wood_octets`, `material_octets_*`, gemischte Zelle, Melone | Holz-/Material-Achtel | 15 + 153 + 2 | **simpleoctets** | Stufe 6 (läuft) |
| `gravity_blocks` | schwebender/levitierender Sand und Kies | 4 | **simpleblocks** | Block-Varianten ohne Themenmodul |
| `nature_slabs` | Erde-, Gras-, Sand-, Kiesstufe | 4 | **simpleblocks** | Block-Varianten (Stufen); Octets siehe Octet-Inventar §C |
| `nature_ice_and_shells` | rissiges Eis, gemeißeltes Packeis/Blaueis, Nautilusschalen-Block | 4 | **simpleblocks** | fehlende Vanilla-Varianten, genau der Auftrag von Simple Blocks |
| `froglights` | Scharlach-/Aqua-/Azur-Froschlicht | 3 | **simpleblocks** | Deko-Lichtblöcke, kein Themenmodul |
| `traps` | Sculk-Kiefer | 1 | bleibt SB (später Simple Mobs) | Falle der Howler-Mechanik |
| `storage_and_light` | rissiger Diamantblock, Enderit-Block, Baulicht | 3 | bleibt | Werkstoff-/Hammerkette bzw. Baugerät (Enderit → Simple Enderite) |
| `light_panes*` | Lichtscheiben (normal, durchsichtig, Redstone, 16 Farben, Kupfer-Schalter) | 37 | **bleibt SimpleBuilding** (Besitzer 17.10.: Redstone/Licht, kein Simple Blocks; Sub-Mod später prüfen) | Deko-Beleuchtung; Redstone-Schalten ist Eigenschaft, kein Modul. Alternative: bleibt SB, weil Schaltlogik |
| `sulfur_lights` | Schwefelfackel, -laterne, -lagerfeuer | 3 | **bleibt SimpleBuilding** (Besitzer 17.10.: Feuer/Flamme, kein Simple Blocks) | Deko-Licht; Schwefel ist Vanilla-Stoff in 26.3 |
| `woodwork_*` | Röhren, Platten, Kessel, Kiste, Schnitzholz je Holzart | 1 Zeile je Holzart | bleibt SB (später Simple Hammers) | Holzwerk-Plan, Schnitzen/Hammer; Kessel/Kiste sind Lager |

Weitere einfache Blöcke in SB außerhalb des Tabs:
| Block | Heute | Ziel | Grund |
|---|---|---|---|
| `crimson/warped_blazewood_planks`, `*_bookshelf` (4) | Vanilla-Tab „Redstone/Bau“ per `SearchTabPlacement` | bleibt SB | Astral-Verzauberer (`McVersion.ASTRAL_ENCHANTING`) |
| `blazing_obsidian` | Natur-Tab | bleibt, bis Rework fertig (Queue 17.10.), dann simpleblocks prüfen | Rework läuft |
| `astralit_lamp`, `nihilith_lamp` | Funktional | bleibt (Palette End) | Palette |
| Redstone-/Kolben-/Schienen-/Tresor-Familien (Astral, Nihil, Brücke) | Redstone | bleibt | Redstone-Linien, Maschinen |
| Truhen, Fässer, Rucksäcke, Trichter, Öfen, Eimer | Funktional | bleibt | Lager/Maschinen (Regel 16) |
| Erze (Salbei, Nihilith, Astralit), Dimensionsschrott | Natur | bleibt | Material-/Enderit-Kette |

## Stand 18.10. (b-blocksmove)
Die 15 Items (Schwerkraft 4, Naturstufen 4, Eis/Schale 4, Froschlichter 3) und die Entity `levitating_block` liegen in `simpleblocks`
(`com.simpleblocks.nature.NatureBlocks`, IdMoves aus `simplebuilding:`); Simple Blocks ist Pflicht-Sub-Mod von SimpleBuilding 26.3 (bündelt
SimpleLib). Tab: Stufe T2 umgesetzt (die 15 stehen von Haus aus im Tab `simpleblocks:blocks`, SB hängt seine übrigen Zeilen per T1 an).
SimpleBuilding behält Weltgenerierung (rissige Eisfelder), Meißel-/Hammer-Wege und Schwerkraft-Kolbentests (per Id). Plan:
`docs/ai/PLAN-B-BLOCKSMOVE-2026-10-18.md`.

## Zahlen
- Tab „SimpleBlocks“ hat heute 12 Zeilengruppen (Basis) + Schach + Octets + Natur + Lichtscheiben + Schwefel + Holzwerk.
- **Verschieben nach `simpleblocks`: 15 Items sicher** (4 Schwerkraft, 4 Naturstufen, 4 Eis/Schale, 3 Froschlichter) **plus 40 nach Entscheidung E-B1**
  (37 Lichtscheiben, 3 Schwefellichter) = höchstens **55 Items**.
- Nach `simpleoctets` (Stufe 6, schon geplant): 168 Achtel + gemischte Zelle + Melone.
- Bleiben (mit Grund): End-Paletten 46, Schach, Holzwerk, Material/Baugeräte, Falle.
- Entschieden E-B1 (Besitzer 17.10.): Lichtscheiben und Schwefellichter bleiben in SimpleBuilding (eher Redstone/Feuer); ob später ein Sub-Mod, wird nachgearbeitet.

## Technik der Verschiebung (Plan für die neue Familienstufe)
Vorbild Stufen 4/5/5b und 6: IdMoves (alte `simplebuilding:*`-Ids → `simpleblocks:*`, Welt-Migration per Datenfixer/Alias, Forge `addAlias`),
Rezepte/Loot/Tags/Advancements, Lang EN+DE, Wiki-Seiten, Handbuch-Kapitel, Tests (`DataIntegrityTests`-Tab-Zeilen, Gametests), Testzentrale.
Blöcke mit Block-Entities oder Mixins (Schwerkraftblöcke: Fall-/Schwebe-Entity, Lichtscheiben: Schaltlogik) brauchen eigene Klassen im Modul.
Registrierung bleibt unabhängig vom Laden anderer Mods (`PLAN-MODULFAMILIE` §2.3). Simple Blocks ist **Pflicht-Sub-Mod von SimpleBuilding** zu machen (wie
Ovens/Crucibles/Mobheads), sonst fehlen im Hauptmod Blöcke, deren Ids vorher dort lagen; Ausnahme: Simple Blocks bleibt allein spielbar.
Reihenfolge: nach Stufe 6 (Octets) und der Octet-Regel; vor Stufe 7.

## Plan: ein Kreativtab

Ist: zwei Tabs. „SimpleBlocks“ (`simplebuilding:building_blocks`, SB, Sammel-Tab mit Achteln, Schach, Paletten) und „Simple Blocks“
(`simpleblocks:blocks`, Modul, nur die eigenen Blöcke; `BlocksRegistry.registerTab`, `tabStacks()`). Beide Namen unterscheiden sich nur durch ein Leerzeichen.
Die Octet-Blöcke stehen zusätzlich per `SearchTabPlacement` im Vanilla-Bautab (diese Platzierung bleibt, sie ist kein eigener Tab).

Ziel: **ein** Tab „Simple Blocks“ für alle Baublöcke der Familie.

### Stufe T1 (Übergang, vor dem Verschieben)
1. SimpleBuilding prüft beim Tab-Registrieren, ob `simpleblocks` geladen ist (`ModList`/`FabricLoader`-Abstraktion `SimpleLibLoaded`-Muster, optionaler Bezug, keine Klassen-Importe;
   `simpleblocks` steht schon in `optional` von SimpleBuilding).
2. Ist `simpleblocks` geladen: SB registriert **seinen** Tab `building_blocks` nicht; stattdessen füllt SB seine Baublock-Zeilen
   (`buildingBlocksRows()` ohne die Zeilen, die nicht dorthin gehören) in den Tab `simpleblocks:blocks`. Mechanik: Simple Blocks stellt eine kleine
   Registry-API bereit (`SimpleBlocks.contributeRows(String ownerModId, Supplier<List<Row>>)`; oder öffentliche Fabric-API-Entsprechung
   `ItemGroupEvents.modifyEntriesEvent` auf den Tab-Schlüssel; NeoForge/Forge `BuildCreativeModeTabContentsEvent` auf denselben Schlüssel). Beitragende Mods werden hinter den
   eigenen Blöcken von Simple Blocks angefügt, in fester Reihenfolge (Simple Blocks, SimpleBuilding-Paletten, Schach, Holzwerk).
3. Ist `simpleblocks` nicht geladen: SimpleBuilding behält seinen eigenen Tab „SimpleBlocks“ unverändert (Standalone).
4. Name des gemeinsamen Tabs: „Simple Blocks“ (en_us/de_de); Icon bleibt das von Simple Blocks (`hushed_deepslate_tiles`), bei SB-Standalone das alte (`astralit_bricks`).
5. Tests: `DataIntegrityTests` (Zeilen/Slots der Tab-Zeilen, heute `expectSlots(... "SimpleBlocks")`) in zwei Varianten: Standalone (eigener Tab) und mit Simple Blocks
   (kein SB-Baublocktab, alle SB-Items im Tab `simpleblocks:blocks`, keine Dubletten); Integrations-Ziel mit allen Modulen; Testzentrale `TabBrowser` zeigt den gemeinsamen Tab.
6. Risiken: Reihenfolge der Mod-Initialisierung (Tab-Inhalt erst bei `displayItems`, also spät genug); Forge/NeoForge-Eventnamen; Wiki/Guide-Texte
   nennen den Tab „SimpleBlocks“ (anpassen); SB-Rezeptbuch/`SearchTabPlacement` unverändert.

### Stufe T2 (Endzustand, nach der Baublock-Verschiebung)
Die verschobenen Blöcke (Inventar oben) liegen in `simpleblocks` und stehen von Haus aus im Tab. Was in SB bleibt (End-Paletten, Schach, Holzwerk) wird weiter per T1 hineingereicht,
solange `simpleblocks` geladen ist. Wandern die End-Paletten später nach Simple End/Enderite, hängen auch diese Module ihre Zeilen an denselben Tab (Mechanismus T1.2 ist modulübergreifend).
Octets: Simple Octets reicht Achtel in denselben Tab, wenn `simpleblocks` geladen ist; allein bleibt es bei dessen eigenem/Vanilla-Tab.
Ein eigener Tab je Modul gilt also nur noch für Nicht-Baublöcke (Werkzeuge, Maschinen, Lager).

### Entscheidung
Die Option „ein SB-Tab für alles“ (statt Tab in Simple Blocks) wurde verworfen: Simple Blocks muss allein spielbar sein und braucht einen eigenen Tab; SimpleBuilding ist der Gastgeber, nicht umgekehrt.
