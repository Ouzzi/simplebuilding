# Plan QW11 - Kreativ-Tabs aufraeumen (B, 04.11., DW04C-7)

Besitzer 04.11.: "bitte dann auch in quick wins die creative tabs aufraeumen." (Anlass: Simple Arrows hatte zwei Tabs, DW04C-4.)
Nur MC 26.3 Fabric. Basis 0a0de070f (Buendel 107).

## Ist-Stand (aus den Registrierungen, Stand vor QW11)

| Tab (Id) | Mod | Name EN vorher | Probleme |
|---|---|---|---|
| simplebuilding:tools, combat, building_blocks (nur ohne Simple Blocks), materials, food, functional, pads | SimpleBuilding (sieben Themen-Tabs, Besitzer 02.10.) | "SimpleTools", "SimpleCombat", "SimpleBlocks", "SimpleMaterials", "SimpleFood", "SimpleMachines", "SimplePads" | Namen nicht "Simple X"; "SimpleMachines"/"SimpleBlocks" fast gleich den Tabs der Module "Simple Machines"/"Simple Blocks" |
| simplearrows:arrows, simpleblocks:blocks, simpleenchanting:enchanting, simplegadgets:gadgets, simplehammers:hammers, simplelights:lights, simplemachines:machines, simplemobheads:heads, simplemobs:mobs, simpleoctets:main, simpleovens:ovens, simplecrucibles:main, simplefoundry:foundry, simplestorage:storage, simplebeacons, simpleend:main, simplefun, simplemaps, simplemoney, simplemusic, simpleriding, simplesandwiches:kitchen, simpleshops, simpletrims, simpletweaks:claims | je ein Tab je Mod | alle "Simple X" | Reihenfolge innerhalb: von Hand je Mod, Bloecke/Treppen/Stufen/Octets nicht nach Familie gruppiert; Tab-Reihenfolge der Mods schon fest (SimpleLib CreativeTabPolicy, creative_tab_order.json aus modules.json) |
| (keiner) | simpleores, simpletools, simpleanimals, simplewanderers, simpledinos, simplemobmanners, simpleenddragonfight, simplequestionary, simplelib | - | Items nur in Vanilla-Tabs (Werkzeuge, Zutaten, Kampf, Nahrung, Funktional, Spawn-Eier) bzw. fremdem SB-Tab (Questionary-Tisch) |
| Fremde Items in Mod-Tabs | z. B. Geraete/Haemmer/Enderit-Materialien in SimpleBuilding-Tabs | | Item steht im Tab des Super-Mods UND im Tab seines Mods |

Laufzeit-Tabelle (Tab, Anzahl Items) schreibt der Cross-Mod-Gametest als Zeile `SB-TABS ...` in die Testausgabe.

## Entscheidungen
1. SimpleBuilding behaelt seine sieben Themen-Tabs (Besitzer 02.10.: Kampf/Nahrung wie in Vanilla getrennt; Zeilen-Layout). Sie heissen jetzt "Simple Building: Tools / Combat / Blocks / Materials / Food / Machines / Pads" (DE: Werkzeuge, Kampf, Bloecke, Materialien, Nahrung, Maschinen, Pads) - eindeutig, "Simple X"-Schema, kein Namensdoppel mit Simple Machines/Simple Blocks. Alles in einen Tab zu legen waere ein Layout-Umbau, den der Besitzer nicht verlangt hat. Rueckfrage im Bericht.
2. Module ohne eigenen Tab bekommen genau einen "Sammel-Tab" (`ModTabs.register` in SimpleLib): ores, tools, animals, wanderers, dinos, mobmanners, enddragonfight, questionary, lib. Die Module fuegen weiter in Vanilla-Events ein; die Policy (`CreativeTabPolicy.fillCollectors`) holt nach dem Vanilla-Bau alle Items des Namensraums in den Tab (Vanilla-Reihenfolge) - kein Modul listet von Hand, neue Items landen automatisch dort (Pads/Regale: `simplepads`/`simpleshelves`, falls sie keinen Tab registrieren, ein `ModTabs.register`-Aufruf genuegt).
3. Ein Item steht im Tab seines Mods: fremde Mod-Tabs verlieren Items, deren Mod einen Tab mit ihnen hat (neue Policy-Regel); Vanilla-Tabs wie bisher (Regel 1), Ausnahme Vanilla-Konvention: Spawn-Eier bleiben zusaetzlich im Tab "Spawn Eggs".
4. Familien-Reihenfolge: `TabSort` (SimpleLib) - je Material-Familie Block, Treppe, Stufe, Mauer, Zaun ..., Octet, dann die naechste Variante (poliert, ... gemeisselt); Nicht-Familien-Items bleiben an ihrem Platz, die Familie steht an der Stelle ihres ersten Mitglieds. Dieselbe Regel (materialKey/variantRank) nutzt jetzt `ModBlockGallery` der Testzentrale (eine Quelle). SimpleBuilding-Tabs bleiben von Hand (Zeilen).
5. Legacy/versteckte Items (guide_book, Enderit-Varianten ohne Simple Ores) bleiben wie bisher aus den Tabs (Policy).
6. Tab-Reihenfolge der Mods: unveraendert `creative_tab_order.json` (SimpleBuilding zuerst, Sub-Mods nach modules.json, SimpleLib zuletzt); jetzt im Cross-Mod-Test geprueft.
7. NeoForge/Forge: nicht angefasst (nur 26.3 Fabric); die neuen Sammel-Tabs muessen dort bei der Portierung registriert werden (Memory-Eintrag).

## Dateien
SimpleLib: `api/TabSort.java` (neu), `api/ModTabs.java` (neu), `api/CreativeTabPolicy.java`, `test/LibTests.java`; je Modul `<Mod>Fabric.java` (eine Zeile) + Lang EN/DE; SB-Lang (vier Dateien); `ModBlockGallery`; `integration/CrossModGameTest`.

## Verifikation
Compile; `fabric-263` Cross-Mod-Test (`integration`) `everyModuleItemIsInSomeCreativeTab` erweitert: Titel-Schluessel und englische Namen eindeutig und "Simple X", kein leerer Tab, jedes Item hoechstens einmal in den Tabs seines Mods, kein Item in Fremd-Tab wenn der eigene Tab es hat, Tab-Reihenfolge nach Mod-Baum, Familien-Reihenfolge idempotent; `creative_tab_policy` (SimpleLib) um TabSort/Lib-Tab erweitert; precheck; Client-Screenshot der Tabs als JPG (`abnahme-b/bilder/QW11-TABS.jpg`).
