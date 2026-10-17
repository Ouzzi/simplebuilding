# Plan B-stylefix2 (17.10.2026) - offene Befunde aus AUDIT-STIL-MODULE-2026-10-17.md

Branch `b-stylefix2` (Basis 27052859a). Ziel: ST-11, ST-20, ST-40, ST-41 abarbeiten.

## ST-11 Einstiegsklassen
- `FabricExample`/`NeoForgeExample`/`ForgeExample` in aesthetics, dimensions, mobs, models, QoL, riding, sounds, visuals und
  wiringexample -> `<Name>Fabric|NeoForge|Forge` (Namen wie die übrigen Module: Aesthetics, Dimensions, Mobs, Models,
  QualityOfLife, Riding, Sounds, Visuals, WiringExample). Forge/NeoForge finden die Klassen per `@Mod`, nur `fabric.mod.json` verweist.
- `tools/templates/module` (Quelle von `tools/newmod.py`; `modules/wiringexample` ist nur eine Kopie davon) bekommt den Token
  `__CLASS__` (Mod-ID ohne `simple`, CamelCase); `newmod.py` ersetzt ihn in Pfaden und Inhalt.
- Risiko: gering (reine Umbenennung); Prüfung: Compile 3 Loader + `:compileJava`.

## ST-20 Config-Lang-Schlüssel
- Referenz ist SimpleBuilding selbst: Schema (b) `text.autoconfig.<mod>.option.*` + `.@Tooltip` (501 Schlüssel). Der Audit-Vorschlag
  "Zielschema (a)" widerspricht dem. Schemata (c)/(d) werden zur Laufzeit aus Code zusammengesetzt (`"simplemodels.config." + field`),
  (b) wird von Autoconfig aus Feldnamen abgeleitet. Umbenennen = Laufzeitlogik -> **nur dokumentieren**, nicht umbauen (Plan im Audit).

## ST-40 Handbuch + FTB-Startquest
- simplemaps, simpletools, simpleenddragonfight: Eintrag in `tools/guides/module_guides.py` (Item-Modul wie simplestats), Generator erzeugt
  Guide-Klasse, Lang, Rezept, Advancement, Item-Modell, FTB-Kapitel, Wiki-Notiz. Dazu je Modul: Registrierung in den drei Loadern
  (Item, Kreativtab-Eintrag, `installQuests`), GameTest `guide_book`, Buchtextur (`tools/textures/module_guide_books_2026_10_05.py`),
  Simple-Money-Preisnotiz (`excluded`).
- interfaces/sounds/visuals: sounds/visuals haben bereits ein Client-Handbuch; interfaces ist Sub-Mod von QoL und im QoL-Buch
  abgedeckt (reines Client-Overlay ohne eigenes Spielziel) -> keine Änderung, im Audit begründet.

## ST-41 questionary/
- simpleblocks, simpledimensions, simplestats, simplewanderers: `questionary/{en_us,de_de}.json` im Format der vorhandenen Module
  (z. B. simplemodels), aus den Wiki-Manuals.

## Verifikation
Compile 3 Loader + `:compileJava`; Modul-Targets maps/tools/enddragonfight (fabric+neoforge) und der vier Questionary-Module;
`module_guides.py --check`; `wiki/generate.py --all` auf sb-test; Audit-Tabelle aktualisieren.
