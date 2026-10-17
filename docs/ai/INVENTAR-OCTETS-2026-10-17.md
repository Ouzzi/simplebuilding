# Inventar: Block-Familien mit Treppe/Stufe und ihre Octets (Achtelblöcke), 17.10.2026

Anlass: Besitzer-Regel 17.10. „Jeder Block, der Treppen- oder Stufen-Varianten (stairs/slab) hat, bekommt auch einen
Octet-Block (Achtelblock)“ (`docs/ai/PRINZIPIEN.md`, Regel O1). Nur Inventar und Plan, kein Code.

Quellen und Methode (alles belegt, keine Schätzung):
- Vanilla 26.3: Rezepte und Blockstates aus `client.jar` 26.3 (`data/minecraft/recipe/*_stairs|*_slab.json`, `assets/minecraft/blockstates`):
  97 Treppen, 101 Stufen (die 101. ist `petrified_oak_slab`, ohne Rezept). Familie = Grundblock, aus dem die Treppe/Stufe gemacht wird.
- Octet-Liste: `modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetMaterials.java` auf Branch `claude-s6-octets`
  (generiert von `tools/textures/octets_2026_10_09.py`; 155 Einträge: 142 Vanilla, 7 SimpleBuilding, 2 Simple Blocks, 4 Simple End;
  dazu die Schach-Achtel `checker_octet` bleiben in SimpleBuilding). Heute (claude-wave1) liegt dieselbe Liste in `common/src/shared/.../blocks/OctetMaterials`.
- SimpleBuilding: `ModBlocks` (`StairBlock`/`SlabBlock`), Simple Blocks: `BlocksRegistry`, Simple End: `EndWood`.
  Kein weiteres Modul registriert Treppen oder Stufen (grep über `modules/`, `common/`, `mc26_3/`).

## Ergebnis in Zahlen

| Gruppe | Familien | Octet vorhanden | Octet fehlt |
|---|---:|---:|---:|
| Vanilla (Treppe und/oder Stufe in Vanilla 26.3) | 101 | 84 | 17 |
| Vanilla-Blöcke, denen Simple Blocks Treppe+Stufe gibt (Kalzit, Netherrack, glatter Basalt) | 3 | 3 | 0 |
| Simple Blocks eigene (Hushed Deepslate Tiles, Blaue Netherziegel) | 2 | 2 | 0 |
| SimpleBuilding (Astralit/Nihilith/Enderquarz-Paletten) | 7 | 7 | 0 |
| SimpleBuilding Schachbretter (11 Farben mit Treppe+Stufe) | 11 | 11 (`checker_octet`, Schach-Achtel) | 0 |
| SimpleBuilding Naturstufen (Erde, Gras, Sand, Kies) | 4 | 0 | 4 |
| Simple End (Aschholz, Dämmerholz: Bretter mit Treppe+Stufe) | 2 | 0 | 2 |
| **Summe** | **130** | **107** | **23** |

Die 17 fehlenden Vanilla-Familien sind 13 Bretter-Familien und 4 reine Stufen-Familien. Dazu 4 SimpleBuilding-Naturstufen und 2 Simple-End-Bretter:
**23 fehlende Octets**, davon 15 Bretter, 4 reine Stufen (Vanilla), 4 Naturstufen.
Die 84 vorhandenen Vanilla-Familien sind ohne Bretter die Steine, Ziegel, Kupfer (alle 8 Stufen inkl. gewachst), Wolle (16 Farben), Beton (16 Farben), Sandstein, Quarz, Purpur, Prismarin, Zinnober/Schwefel (26.3).

## Fehlende Octets (Arbeitsliste für die Octet-Regel)

### A. Bretter (15): bisher nur Holz-Achtel aus Stämmen
Es gibt Holz-Achtel für Stämme/entrindete Stämme (`*_log`, `stripped_*`), aber keine Bretter-Achtel. Die Bretter haben Treppe und Stufe,
also bekommen sie nach der Regel ein Achtel (Gruppe `planks`, neues Muster ohne Maserungsrichtung):

`oak_planks`, `spruce_planks`, `birch_planks`, `jungle_planks`, `acacia_planks`, `dark_oak_planks`, `mangrove_planks`, `cherry_planks`,
`pale_oak_planks`, `bamboo_planks`, `crimson_planks`, `warped_planks`, `poplar_planks` (13 Vanilla) sowie `simpleend:ashwood_planks`,
`simpleend:duskwood_planks` (2, nur bei geladenem Simple End, wie die vier End-Stämme).

Hinweis: `OctetMaterials` schloss Bretter bewusst aus („without planks“). Diese Ausnahme entfällt mit der Regel; Rezepte (Steinsäge/Hammer) wie bei den Stämmen.

### B. Reine Stufen-Familien in Vanilla (4)
| Grundblock | Vanilla hat | Anmerkung |
|---|---|---|
| `cut_sandstone` | Stufe | Simple Blocks ergänzt die Treppe → Familie komplett, Octet dann Pflicht |
| `cut_red_sandstone` | Stufe | wie oben |
| `smooth_stone` | Stufe | Treppe nirgends; Stufe genügt für die Regel |
| `petrified_oak` | Stufe (`petrified_oak_slab`, kein Rezept) | Textur = Eichenbretter; Gruppe `planks` |

### C. SimpleBuilding-Naturstufen (4) – Entscheidung nötig
`dirt_slab`, `grass_slab`, `sand_slab`, `gravel_slab` (nur 26.3, `McVersion.NATURE_VARIANTS`). Es sind Sonderblöcke (`MergingSlabBlock`
verschmilzt zwei Stufen zum vollen Vanilla-Block, `FallingSlabBlock` fällt). Ein Achtel müsste dieses Verhalten ebenfalls haben
(acht Achtel → Vollblock bzw. Fallverhalten). Vorschlag: Achtel von Erde/Gras/Sand/Kies **ohne** Sonderverhalten (reine Deko), dafür
kein Zusammenfügen; Besitzer entscheidet, ob die vier ausgenommen werden (dann Regel O1 mit Ausnahmeliste „Blöcke mit Sonderverhalten“).

## Vorhandene Octets ohne Treppe/Stufe (nur zur Info; die Regel gilt einseitig)
Octets existieren auch für Blöcke ohne Treppe: Obsidian, Endstein, Knochenblock, Metall-/Erzblöcke (Kupfer-Stufen, Kohle, Diamant, Smaragd, Gold,
Eisen, Lapis, Netherit), alle Stämme/Hölzer, 16 Terrakotta-Farben, Melone. Sie bleiben; die Regel verlangt nur die Richtung Treppe/Stufe → Octet.

## Mechanik für Fremdmods (Simple Blocks)
- Jeder Block, dem Simple Blocks Treppe/Stufe gibt, steht mit Octet in `OctetMaterials` (Namespace des Basisblocks; bei Vanilla-Blöcken
  `minecraft:` – damit existiert das Octet unabhängig vom Laden von Simple Blocks, z. B. `calcite_octet`; bei Simple-Blocks-eigenen Blöcken
  `simpleblocks:` – Octet nur bei geladenem Simple Blocks, über `SimpleOctets.isLoaded`).
- Neue Treppen/Stufen in Simple Blocks lösen deshalb immer einen Eintrag im Octet-Generator aus (`octet_materials.json`). Jedes Modul bleibt allein spielbar:
  Simple Blocks kennt Simple Octets nicht, Simple Octets prüft Simple Blocks bedingt.
- Abdeckungstest (Teil der Umsetzung): GameTest in `simpleoctets`, der alle registrierten `StairBlock`/`SlabBlock`-Basisblöcke durchgeht und für jeden ein Octet
  (oder einen Ausnahmeeintrag mit Grund) verlangt. So fällt ein künftiges Modul mit Treppen sofort auf.
- Risiko: Octets von Fremdblöcken sind nur bei geladenem Fremdmod registriert; Entfernen des Fremdmods macht die Octets in alten Welten unbekannt
  (gleiche Lage wie bei den vier End-Stämmen heute). Vorbild: Alias/DataFixer-Regeln aus `PLAN-MODULFAMILIE-2026-10-16.md` §2.3.

## Folgen für die Queue
Siehe `.claude/QUEUE.md`, Block „Besitzer 17.10.“, Punkt (a): Umsetzung erst nach Merge von `claude-s6-octets`; Umfang 23 neue Octets (bzw. 19 ohne Naturstufen), eine Generator-Gruppe `planks`, Abdeckungstest, Rezepte, Wiki/Guide.
