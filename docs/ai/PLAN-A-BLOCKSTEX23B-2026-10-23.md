# Plan a-blockstex23b (W23-3, Simple Blocks) – 23.10. abends

Basis a-blockstex23 (Vorschau B23-T1..T4) + Bündel 50. Nur MC 26.3 Fabric. Besitzer-Wahl W23-3 (.claude/QUEUE.md):
Moos neu (nur Vorschau), Risse einbauen (durchgehend wie Vanilla), Sculk-Tiefenschiefer B (organischer), gehüllte
Tiefenschiefer-Fliesen B (weniger gerade Linien), neuer Block Sculk-Fliesen (C, animiert), gemeißeltes Packeis B + Blaueis analog.

## Ist-Stand
- Rissige/bemooste Ziegel (13) sind Vanilla-abgeleitet: `tools/textures/missing_blocks_2026_10_16.py` (`cracked`/`mossy`),
  gebaut über `vanilla_derived.MODULE_OUTPUTS['simpleblocks']`, Pfade in `.gitignore`.
- `sculk_deepslate`, `hushed_deepslate_tiles`, `chiseled_packed_ice`, `chiseled_blue_ice`: eigene, eingecheckte PNG + `.layers.json`.
  Die gewählten B-Varianten setzen auf Vanilla-Basistexturen (deepslate, deepslate_tiles, packed_ice, blue_ice) → sie werden
  wie die Ziegel beim Build erzeugt (AGENTS.md 7), PNG + Ebenen + Wiki-Kopien raus aus Git, Pfade in `.gitignore`.
- Vanilla-Analyse (26.3, pixelgenau): `mossy_stone_bricks` ändert 98 Pixel, 5 Oliv-Töne (Lum 86–130, dunkler als die Steinmitte);
  Moos sitzt auf den waagerechten Fugen: dunkelster Ton in der Fuge, hellere Töne als Band unter der Fuge (oberste Ziegelreihe)
  mit 1–5 px langen „Tropfen“ nach unten (Spitze dunkler), darüber 1 Reihe aufsteigend; Steinpixel neben Moos werden um eine
  Stufe dunkler (Schatten). `cracked_stone_bricks`/`cracked_deepslate_bricks`: lange, zusammenhängende 1-px-Risse (−20…−55 Lum),
  die über Fugen hinweg durch mehrere Ziegel laufen, verzweigt, plus helle Kantenpixel (+10…+20).

## Umsetzung
1. Neuer Build-Generator `tools/textures/blocks_b23_2026_10_23.py` (eingetragen in `MODULE_GENERATORS['simpleblocks']`):
   - `cracked()` Vanilla-Stil: 3 lange Risse (12–18 px, 8-verbunden, über Fugen), 1–2 Äste, Kern dunkel, Halbton an Diagonalschritten,
     Lichtkante; ersetzt `missing_blocks.cracked` für alle 6 rissigen Ziegel. Ausnahmen (rissiger Diamantblock, rissiges Eis) unberührt.
   - `sculk_deepslate()` = Vanilla-Tiefenschiefer + organische Sculk-Adern (gekrümmte Zufallspfade, wechselnde Dicke, Knoten, Saum rundum, Leuchtpunkte).
   - `hushed_tiles()` = Vanilla-Tiefenschiefer-Fliesen + Sculk als unregelmäßige Polster/Adern über Fliesen und Fugen (nicht entlang der Fugen).
   - `chiseled_ice('packed'|'blue')` = Vanilla-Packeis/-Blaueis + eingemeißeltes Relief (Motiv bleibt; Blaueis: Raute wie bisher).
   - `sculk_tiles()` eigene Textur (eingecheckt, Ebenen, `.mcmeta`): Fliesen in Sculk-Farben, versetzt/ungleich groß (wenige durchgehende Fugen),
     jede Fliese eigene Helligkeit, dezentes Pulsieren einiger Leuchtpixel (8 Frames).
2. Neuer Block `simpleblocks:sculk_tiles` (Würfel, Eigenschaften von Tiefenschiefer-Fliesen): Registry, Rezept (4 Tiefenschiefer-Fliesen + 4 Sculk → 4),
   Loot (sich selbst), Pickaxe-Tag, Lang EN/DE, Tab (neben gehüllten Fliesen), Wiki-Manual, Discover, Inventar Baublöcke.
   Octet-Regel (A4): gilt nur für Blöcke mit Treppe/Stufe – Sculk-Fliesen bekommen (wie Sculk-Tiefenschiefer) keine → kein Octet nötig.
3. Moos: 3 neue Varianten (Vanilla-Struktur) an bemoosten Tiefenschiefer-Fliesen + bemoosten Endstein-Ziegeln, nur Vorschau.
4. Vorschau EINE Seite `/root/sb-tools-a/previews/a-blockstex23b-preview.html` (Eingebaut / Wahl offen getrennt).

## Verifikation
- Gametest `sculk_tiles` (Registrierung, Loot, Tab) + Fabric-Adapter; vorhandene Rundum-Tests (Rezept/Loot je Block) decken ihn mit ab.
- sbt: Compile `:mc26_3:fabric` + `:modules:simpleblocks:fabric`, `module-simpleblocks-fabric-263` + `-standalone-fabric-263`,
  Generatoren (module_guides, wiki --all, discover, textures.js), `layers.py check`, `./gradlew check -q`.
