# Plan B: Octet-Regel (a) + Polierter Endstein Treppe/Stufe/Mauer (2026-10-18)

Branch `b-octetrule`. Ist-Zustand (grep): Die 15 Bretter-Achtel (`oak_octet` ... `duskwood_octet`, Generator `WOODS`, Rezept aus
`*_planks`) EXISTIEREN schon (das Inventar 17.10. nannte sie fälschlich fehlend, `OctetMaterials` enthält nur die Nicht-Holz-Materialien).
Es fehlen also nur: `cut_sandstone`, `cut_red_sandstone`, `smooth_stone` (Vanilla-Stufenfamilien, Treppe ergänzt Simple Blocks),
`petrified_oak` (siehe Ausnahme) und das neue `simplelib:polished_end_stone`.

## Teil 1: Octets
- `tools/textures/octets_2026_10_09.py`: `EXTRA_BASES` += cut_sandstone, cut_red_sandstone, smooth_stone (Textur aus Vanilla-Blockmodell, Gruppen
  sandstone/stone); neue Liste `LIB_EXTRAS` (simplelib:polished_end_stone, Gruppe stone, Textur `simplelib:block/polished_end_stone`);
  `conditions('simplelib')` = leer (simplelib ist Pflicht-Abhängigkeit). Lauf auf sb-test erzeugt octet_materials.json, OctetMaterials.java,
  Modelle, Blockstates, Items, Lang, Rezepte (Steinsäge hin, 8 Achtel zurück), Advancements, Abbau-Tags.
- `OctetRegistry`: TEMPLATES += `polished_end_stone` -> END_STONE (Quelle spät gelesen).
- Ausnahme `petrified_oak_slab`: kein Vollblock (Textur = Eichenbretter, Vollblock `oak_planks` hat schon `oak_octet`). Begründete Ausnahme im Test.
- Abdeckungstest `OctetTests.stairSlabBaseCoverage`: jeder registrierte `StairBlock`/`SlabBlock` (Basisblock aus Id: `_stairs`/`_slab`/Varianten) hat ein
  Octet (`minecraft`/Namespace-Material in `OctetMaterials`, Holz-Achtel) oder steht in der Ausnahmeliste mit Grund:
  Naturstufen dirt/grass/sand/gravel (Besitzer-Entscheidung 17.10.), petrified_oak, Schach-Achtel `checker_octet`.
## Teil 2: Polierter Endstein Treppe/Stufe/Mauer (simplelib)
- `LibMaterials`: Blöcke `polished_end_stone_stairs/_slab/_wall` (StairBlock/SlabBlock/WallBlock, Kopie der Eigenschaften), Items, Gruppe
  `polished_end_stone`. `gen_resources.py`: Modelle/Blockstates/Items (Muster aus simpleblocks `generate_data.py`, vorhandene Textur), Loot (Stufe 2er),
  Rezepte Werkbank + Steinsäge (Bedingung `material_requested`), Advancements, Tags (mineable/pickaxe, stairs/slabs/walls Block+Item). Lang EN/DE.
- Tests: `MaterialRequestTests` (Registrierung, Rezepte folgen Anfrage), Octet-Test für polished_end_stone.
- SB-Suchtab-Kette: Items hinter `POLISHED_END_STONE` (nur Platzierung, keine neuen SB-Ids). Wiki/Guide/Discover auf sb-test regeneriert.
## Verifikation
Compile aller Loader, module-simplelib-/simpleoctets-Gametests fabric+neoforge, Standalone-Targets, fabric-263 Filter Octet, `gradlew check`.
