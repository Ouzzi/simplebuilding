# PLAN a-astral07 (06.11.2026): Astralit-Glimmen in den Fugen der Nihilit-End-Bretter

Besitzer END07=A: "Astralit-Holz soll wie beim Blazewood in den Kerben des Holzes die Astralit-Animation haben."
Ist-Stand: Blazewood-Bretter = 16-Bilder-Streifen (misctex23b `A1`, `animate`: abgenommene Textur als Ruhebild, nur Glutpixel atmen, frametime 4, interpoliert). Ein Astralit-Holz gibt es nicht; das einzige Holz dieser Art sind die Nihilit-End-Bretter (a-end07).
Entscheidung: die Nihilit-Bretter bekommen denselben Streifen; leuchten in den Fugen (zwei dunkle Naht-Toene + Tuerkis-Splitter) im hellen Astralit-Rosa (astralit_block). A: jede Fuge atmet eigen (gebaut), B: diagonale Welle (nur Vorschau). Skript tools/textures/end07_2026_11_06.py, kein Mojang-Pixel.
Rueckfrage: Sollen es echte Astralit-End-Bretter (rosa Holz, eigener Block) sein? Dann neuer Block samt Rezept.
Test: Compile fabric, data_integrity*, module-simpleend fabric + standalone.
