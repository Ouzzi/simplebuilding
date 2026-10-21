# Plan b-wither21 (21.10.2026)

Queue „(B, b-wither21, 21.10.)“ – Wither-Beute + Starke-Upgrade-Vorlage neu (Besitzer 21.10., Bilder
`/root/wt/owner-img/17.png` = Vervielfältigung, `18.png` = heutiges Herstellungsrezept).

## Ist-Zustand

- `simplebeacons:strong_upgrade_template` (`BeaconsRegistry`): Rezepte `strong_upgrade_template` (Obsidian-Ecken,
  4 Diamanten, Netherstern Mitte = Bild 18), `_duplication` (7 Diamanten + Obsidian + Vorlage → 2 = Bild 17),
  `_from_basic` (nur mit SimpleBuilding: Basis-Aufwertung + Netherstern + 4 Obsidian).
- Wither-Stufen in Simple Mobs: Stufe 1 = Vanilla-Wither, Stufe 2 = `BlightedWither` (Netherithelm), Stufe 3 =
  `AbyssalWither` (Enderithelm). Beute in `BlightedWither#dropCustomDeathLoot`: Witherherz (`simplemobs:wither_heart`),
  Vanilla-Netherstern (super) + 0–1 weitere, 3–5 Nuggets. Keine Explosion beim Tod (Vanilla-Todesanimation 20 Ticks).
- Biom-Pinsel `simplemobs:biome_brush` (`BiomeBrush`, `BiomeBrushItem`, Config `biomeBrush*` in `MobsConfig`,
  Daten `data/simplemobs/biome_brush/*.json`, Tag `simplemobs:biome_brush_locked`, Rezept Witherherz+Netheritbarren+Pinsel).
- Simple Mobs bündelt kein SimpleLib (also keine `IdMoves`); Simple Tools und Simple Beacons bündeln SimpleLib.

## Entscheidungen (Auslegung)

1. **Vorlage nicht frei craftbar.** Bleiben: Vervielfältigung (Bild 17) und Bild-18-Form mit **Netherherz** statt
   Netherstern (bedingt: nur mit Simple Mobs). Neu: **9 Strong-Upgrade-Splitter → 1 Vorlage** (3×3). Weg:
   Netherstern-Rezept und `_from_basic` (beides Grundzutaten-Wege; Besitzer: „nicht craftbar, nur replizierbar“).
2. **Splitter** `simplebeacons:strong_upgrade_shard` gehört Simple Beacons (Besitzer der Vorlage, Modul-Unabhängigkeit):
   der Vanilla-Wither (nur Typ `minecraft:wither`, nicht die Stufen 2/3) lässt **immer** einen fallen – auch mit
   Simple Mobs („immer beim normalen Wither“). Umsetzung als Mixin auf `WitherBoss#dropCustomDeathLoot` (wie der
   Vanilla-Netherstern, alle drei Loader gleich, kein Loot-Table-Eingriff), explosionsfest (Schadensresistenz
   `#minecraft:is_explosion`), längere Lebenszeit. Schalter: Super-Mod-Schalter `SimpleBeacons.on()`.
3. **Netherherz** `simplemobs:nether_heart` ersetzt das Witherherz: neuer Block + BlockItem, `IdMoves`
   `simplemobs:wither_heart → simplemobs:nether_heart` (Item). Dafür bündelt Simple Mobs jetzt SimpleLib (wie
   Beacons/Tools: Fabric `include`, NeoForge `jarJar`, Forge `forgeBundleSimplelib`, Metadaten `simplelib >=0.2.0`,
   `modules.json requires`). Stufe 2 und 3 lassen je **ein** Netherherz fallen.
4. **Nether-Juwel** `simplemobs:nether_gem` (Block + BlockItem), nur Stufe 3, Zweck „später“ (eine Tooltip-Zeile).
5. **Stufe 3 ohne Nethersterne** (auch nicht den Vanilla-Stern): Netherherz + Nether-Juwel + Nuggets. Stufe 2 behält
   1–2 Nethersterne (Besitzer nennt nur Stufe 3).
6. **Platzierte Form:** Netherherz/Nether-Juwel werden beim Ende der Todesanimation („nach der Explosion“ = das
   Verpuffen nach 20 Ticks) unter dem Wither gesetzt: Suche abwärts (bis 24 Blöcke) nach einem freien, flüssigkeitsfreien
   ersetzbaren Platz über einer tragfähigen Oberseite; das Juwel auf einen freien Nachbarplatz. Kein Platz → Item-Entity
   (explosions- und feuerfest, lange Lebenszeit). Blöcke: Härte 1,5, Explosionsfestigkeit 1200, `wither_immune`/
   `dragon_immune`, Abbauen gibt das Item (Loot-Table), kleine Form (Herz 8×8×8, Juwel Kristallbüschel/Kreuzmodell),
   leuchten schwach. Nuggets/Nethersterne fallen wie bisher sofort.
7. **Biom-Pinsel nach Simple Tools** (`simpletools:biome_brush`): Klassen nach `com.simplebuilding.modules.simpletools.brush`,
   Config `biomeBrushEnabled/OpsOnly/Radius` in `ToolsConfig` (Werte des alten Simple-Mobs-Config nicht übernommen –
   Standardwerte gleich), Daten `data/simpletools/biome_brush/*`, Tag `simpletools:biome_brush_locked`,
   Durability-Tag. `IdMoves` (registriert von Simple Tools): Item, Rezept, Rezept-Fortschritt `simplemobs:… → simpletools:…`.
   Rezepte (bedingt, Regel 4/5): ohne Simple Mobs `biome_brush` = 3 Nethersterne + Netherit-Nugget (`#c:nuggets/netherite`,
   SimpleLib-Material `netherite_nugget` wird angefordert) + Pinsel; mit Simple Mobs stattdessen
   `biome_brush_nether_heart` = Netherherz + Netherit-Nugget + Pinsel.
8. Texturen: Netherherz (Item + Block), Nether-Juwel (Item + Block), Splitter – eigene Pixel, Vanilla-nah, je 2 Varianten
   (A eingebaut), Ebenen-Quelle `<name>.layers.json` neben dem PNG (`tools/textures/layers.py check`), Generator
   `tools/textures/wither21_textures.py`. Biom-Pinsel-PNG zieht unverändert mit um (+ Ebenen-Quelle per Import).
   Abnahme-Seite `wi21.html` (Scratchpad, Code WI21), nicht veröffentlicht.

## Dateien

- simplebeacons: `BeaconsRegistry` (Splitter), neues Mixin `WitherShardMixin` + `simplebeacons.mixins.json`, Rezepte
  (`strong_upgrade_template` bedingt+Netherherz, neu `_from_shards`, weg `_from_basic` + Fortschritte), Lang EN/DE,
  Item-Modell/Textur, Tests `BeaconsTests` (+ Fabric-Adapter), `check_data.py`, Wiki-Manual, Questionary, Preise.
- simplemobs: `WitherMobs` (Blöcke, Items, IdMoves), neu `WitherLootBlock`, `BlightedWither`/`WitherLogic` (Beute,
  Platzierung), Loader (Blockregistrierung, Tabs, IdMoves-Aufruf), Build/Metadaten (SimpleLib), Daten (Blockstates,
  Modelle, Loot, Tags, Schadens-Tag), Lang, Tests `WitherTests` (+ Fabric-Adapter), Brush-Teile entfernt,
  `MobsConfig`, Client-Smoke, Wiki-Manual, Questionary, Preise, `check_data.py`.
- simpletools: `brush/BiomeBrush`, `brush/BiomeBrushItem`, `brush/BrushTests`, `ToolsRegistry`, `ToolsConfig`, Loader
  (Tab), `SimpleTools` (IdMoves, Material-Anforderung), Daten, Lang, Wiki-Manual, Questionary, Preise.
- `tools/textures/wither_textures.py` (Brush-Pfad, Herz raus), neu `tools/textures/wither21_textures.py`;
  `modules/modules.json`, `docs/MODS-UND-FEATURES.md`, `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md` (Abhängigkeit).

## Tests

- simplemobs `wither_drop`: Stufe 2 → Netherherz (gesetzt), 1–2 Sterne, Nuggets; Stufe 3 → Netherherz + Juwel gesetzt,
  **0** Nethersterne; Vanilla-Wither → kein Herz. `wither_loot_place`: Boden → Blöcke unter dem Wither, kein Boden →
  Item-Entities; Explosionsfestigkeit (Block-Widerstand, Item-Entity übersteht `level.explode`); `wither_heart_moved`:
  alte ID `simplemobs:wither_heart` findet das Netherherz.
- simplebeacons `strong_upgrade_shard`: Vanilla-Wither droppt 1 Splitter, Stufe 2/3 keinen; Splitter explosionsfest;
  Rezepte: kein Netherstern-Weg, `_from_basic` weg, 9 Splitter → Vorlage, Netherherz-Weg nur mit Simple Mobs.
- simpletools `biome_brush_*` (umgezogen) + `biome_brush_recipes` (Netherstern-Variante ohne Simple Mobs, Netherherz-
  Variante mit) + `biome_brush_moved` (alte ID).
- Ziele: `module-simplemobs/-simplebeacons/-simpletools-fabric-263`, `-neoforge-263`, Standalone-Varianten,
  `data_integrity`, `integration-263`; Compile aller Loader; Guides/Wiki/Discover (-gen); `./gradlew check -q`.

## Risiken

- Statische Block-Instanz in Simple Mobs (wie die statischen Items) – NeoForge/Forge registrieren im RegisterEvent.
- Zwei neue Mod-Abhängigkeiten (Mobs → SimpleLib): Standalone-Ziel lädt SimpleLib mit (gebündelt).
- Spieler mit altem Witherherz-Rezept im Rezeptbuch: Rezept-ID-Move auf den Pinsel in Simple Tools.
- Gemeinsame Dateien: `.claude/QUEUE.md` (nur eigene Zeile), Preise in `simplemoney` (nur eigene Schlüssel).
