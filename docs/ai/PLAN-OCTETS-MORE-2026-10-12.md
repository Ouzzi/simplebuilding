# Mehr 0,125er-Blöcke (2026-10-12, Branch claude-octets-more)

Ist: `tools/textures/octets_2026_10_09.py --scan` liefert 91 Material-Achtel (alle Blöcke mit Treppe und Stufe; Wolle, Beton und Kupfer-Schnitt sind dabei). Die Liste liegt in `tools/textures/octet_materials.json` und erzeugt `OctetMaterials.java`, Modelle, Blockstates, Lang (EN/DE); Rezepte (Steinmetz 1 Block -> 8 Achtel, formlos 8 -> 1), Tabs, Drops und Brennbarkeit laufen daraus automatisch.

Neu: das Scan-Skript nimmt zusätzlich eine feste Liste von Vanilla-Vollblöcken ohne Treppe (`EXTRA_BASES` und Stämme nach Namen). 58 neue Achtel, gesamt 149.

| Gruppe | Anzahl | Begründung |
|---|---|---|
| Keramik (Terracotta) | 17 | gedeckte Baufarben in 16 Tönen plus naturfarben; Standardpalette neben Wolle/Beton |
| Metall- und Edelsteinblöcke | 7 | Eisen, Gold, Diamant, Smaragd, Lapis, Kohle, Netherit: Akzentfarben für Details |
| Kupferblöcke | 4 | ungeschnittenes Kupfer in vier Oxidationsstufen, ergänzt die Schnittkupfer-Reihe (gewachste Varianten sind textur-gleich, entfallen) |
| Stämme und Stiele | 24 | 10 Holzarten + Karmesin/Wirr, je roh und geschält: Balken-Optik, Säulen (Sided-Modell: Enden oben/unten, Rinde seitlich) |
| Stein-Ergänzungen | 5 | Kalzit, Glatter Basalt, Endstein, Netherrack, Obsidian: helle/dunkle/warme Töne, die der Palette fehlen |
| Knochenblock | 1 | helles Weiß-Beige, Säulentextur |

Bewusst nicht: Glas (durchsichtig, Render-Layer/Culling je Ecke), Laub (Tönung, Transparenz), glasierte Keramik (gerichtetes Muster), Redstone-, Honig-, Schleim-, Lichtblöcke (Funktion/Licht), Erze, Rohblöcke, Kisten und alle Block-Entities, Waxed-Duplikate und `*_wood` (nur Rinde, Dopplung zum Stamm).

Verifikation: `octets_2026_10_09.py --check`, GameTest `material_octets_cover_the_extra_palette` (Zelle, Item, Rückrezept, keine Dubletten), `data_integrity*` (Drops/Tabelle für alle Zellen).
