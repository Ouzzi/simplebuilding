# Bericht claude-endleaves2 (N68: End-Blätter N4 + Stock-Bug)
- Recherche: docs/ai/RECHERCHE-BLAETTER-2026-10-16.md (Vanilla, Better-Leaves-Ansätze, Merkmalsliste).
- Blätter: Ursache der "lila Punkt-Raster" = Kristallwürfel in JEDEM Laubblock + gekreuzte Tendril-Ebenen. Jetzt Vanilla-nahe Würfel (modules/simpleend/tools/gen_data.py), Blattflecken-Texturen per Generator (gen_textures.py: leaf_clumps; außen luftig, innen dicht), Kristall nur an Blättern mit Knospe/Frucht (drop 1/2), *_leaves_outer.png entfernt, Außen/Innen bleibt (inner-Blockstate).
- Stock-Bug: Stäbe berühren Nachbarn nur entlang ihrer Achse, daher schwebten Knicke der Äste, Wurzeln und der 1-px-Spalt am Stamm. Fix: natürliche Stäbe Dicke 3 = Vollwürfel (Stamm, Äste an Knicken, Wurzeln als liegende Würfel); platzierte Stäbe unverändert (EndSticksBlock.full, EndTrees.branch/roots, Blockstate *_sticks_full).
- Test: neuer GameTest stick_tree_wood_connected (6 Bäume BIG/HUGE, jedes Holz trifft einen Nachbarn, der zurückreicht).
- Läufe: Compile 3 Loader ok; module-simpleend fabric/neoforge/forge 34/34 grün (inkl. neuem Test); Client-Smoke module-simpleend-client-263 BUILD SUCCESSFUL.
- Vorschau: docs/ai/previews/claude-endleaves2/ (Textur-8x-Sheets, Baum-Screenshots). Nicht getestet: Weltgen im echten End (nur Feature-Test), Test schlug vor dem Fix nicht nachweislich fehl, Forge/NeoForge-Client.
- Offen: Besitzer-Abnahme der Blätter (Tönung wirkt braun-grau wie zuvor), Merge nach wave1.
