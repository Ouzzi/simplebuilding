# Paletten-Analyse 2026-11-05 (IG31-42, Stufe 1)

Reine Analyse, nichts gebaut. Quelle: Vanilla-Blockstates aus `minecraft_26.3_client.jar` (1289 Einträge), Mod-Blockstates aller `assets/*/blockstates` im Repo (Stand claude-wave1 8f77507b5), Octets aus `modules/simpleoctets`. Maschinenlesbar: `PALETTEN-ANALYSE-2026-11-05.json` (erzeugt aus den Registries/Blockstates, nicht aus dem Bild). Vorschau: `/root/sb-tools-b/previews/a-palette05-preview.html`.

Legende: V = Vanilla vorhanden, M:<Mod> = Mod vorhanden, FEHLT, (weg) = bewusst weggelassen, - = Glied gibt es in dieser Familie nicht (kein Vorbild).

## Familienlogik (Besitzer)
- Jede Grundart ist eine eigene Familie (Stone, Cobblestone, Stone Bricks, Mossy Stone Bricks, Polished X, X Bricks, X Tiles ...).
- Cracked/Chiseled stehen bei der Herkunftsfamilie (Cracked Stone Bricks -> Stone Bricks; Chiseled Deepslate -> Cobbled Deepslate; Chiseled Polished Blackstone -> Polished Blackstone).
- Deepslate pur gehört zu Cobbled Deepslate. Ausreißer (Sculk Tiles, Smooth Stone) bekommen keine volle Palette.
- Octet-Regel (PRINZIPIEN A4): jede Familie mit Treppe+Stufe bekommt ein Octet in `simpleoctets`; ohne Treppe/Stufe kein Octet (folgt).
- Nicht erfasst (kein Stein im Sinne des Bildes, bewusst weggelassen): Terracotta/Concrete/Wolle (haben Octets, keine Treppen), Kupfer-Schnittblöcke (Metallfamilie, eigener Fall), Eis/Packed Ice/Blue Ice, Schach- und Quarz-Checker-Blöcke, Säulen (Quartz/Purpur Pillar), Infested-Blöcke, Packed Mud, Resin Block.

## Zahlen

- Familien: 71; Zellen 497: Vanilla 189, Mod 133, FEHLT 59, bewusst weggelassen 5, nicht vorhanden-in-Familie 111.
- Fehlende Glieder gesamt: **59** = Mauer 21, Treppe 12, Cracked 5, Stufe 11, Octet 10.
- Nach Mod: Simple Blocks 42, Simple Octets 10, SimpleBuilding 7.
- Davon 5 neue Vollblöcke mit eigener Textur (Cracked Prismarine, Cracked Prismarine Bricks, Cracked Dark Prismarine, Cracked Polished Deepslate, Cracked Blue Nether Bricks); der Rest nutzt die Textur des Familienblocks (Treppe/Stufe/Mauer) bzw. ist ein Octet.

## Tabelle Familie x Glied


### Stein

| Familie | Block | Treppe | Stufe | Mauer | Octet | Cracked | Chiseled | Anmerkung |
|---|---|---|---|---|---|---|---|---|
| Stone | V | V | V | FEHLT | M:Simple Octets | - | - | Vanilla hat keine Stone-Mauer |
| Smooth Stone | V | FEHLT | V | (weg) | M:Simple Octets | - | - | Vanilla nur Stufe(slab); Treppe/Mauer fehlen, Kontinuität zur Familie Stone |
| Cobblestone | V | V | V | V | M:Simple Octets | - | - |  |
| Mossy Cobblestone | V | V | V | V | M:Simple Octets | - | - |  |
| Stone Bricks | V | V | V | V | M:Simple Octets | V | V |  |
| Mossy Stone Bricks | V | V | V | V | M:Simple Octets | - | - |  |
| Granite | V | V | V | V | M:Simple Octets | - | - |  |
| Polished Granite | V | V | V | FEHLT | M:Simple Octets | - | - |  |
| Diorite | V | V | V | V | M:Simple Octets | - | - |  |
| Polished Diorite | V | V | V | FEHLT | M:Simple Octets | - | - |  |
| Andesite | V | V | V | V | M:Simple Octets | - | - |  |
| Polished Andesite | V | V | V | FEHLT | M:Simple Octets | - | - |  |

### Deepslate

| Familie | Block | Treppe | Stufe | Mauer | Octet | Cracked | Chiseled | Anmerkung |
|---|---|---|---|---|---|---|---|---|
| Cobbled Deepslate (+ Deepslate) | V | V | V | V | M:Simple Octets | - | V | Deepslate pur gehört hierher (Besitzer); Octet für Deepslate pur nicht nötig (Ausreißer) |
| Polished Deepslate | V | V | V | V | M:Simple Octets | FEHLT | - |  |
| Deepslate Bricks | V | V | V | V | M:Simple Octets | V | - |  |
| Mossy Deepslate Bricks | M:Simple Blocks | FEHLT | FEHLT | FEHLT | FEHLT | - | - |  |
| Deepslate Tiles | V | V | V | V | M:Simple Octets | V | - |  |
| Mossy Deepslate Tiles | M:Simple Blocks | FEHLT | FEHLT | FEHLT | FEHLT | - | - |  |
| Hushed Deepslate Tiles | M:Simple Blocks | M:Simple Blocks | M:Simple Blocks | M:Simple Blocks | M:Simple Octets | - | - | Mod-Familie, vollständig |
| Sculk Tiles / Sculk Deepslate | M:Simple Blocks | (weg) | (weg) | (weg) | (weg) | - | - | Sonderblöcke (animiert / Erfahrung), nur Block |

### Tuff

| Familie | Block | Treppe | Stufe | Mauer | Octet | Cracked | Chiseled | Anmerkung |
|---|---|---|---|---|---|---|---|---|
| Tuff | V | V | V | V | M:Simple Octets | - | V |  |
| Polished Tuff | V | V | V | V | M:Simple Octets | - | - |  |
| Tuff Bricks | V | V | V | V | M:Simple Octets | M:Simple Blocks | V |  |
| Mossy Tuff Bricks | M:Simple Blocks | FEHLT | FEHLT | FEHLT | FEHLT | - | - |  |

### Blackstone

| Familie | Block | Treppe | Stufe | Mauer | Octet | Cracked | Chiseled | Anmerkung |
|---|---|---|---|---|---|---|---|---|
| Blackstone | V | V | V | V | M:Simple Octets | - | - |  |
| Polished Blackstone | V | V | V | V | M:Simple Octets | - | V |  |
| Polished Blackstone Bricks | V | V | V | V | M:Simple Octets | V | - |  |

### Sandstein

| Familie | Block | Treppe | Stufe | Mauer | Octet | Cracked | Chiseled | Anmerkung |
|---|---|---|---|---|---|---|---|---|
| Sandstone | V | V | V | V | M:Simple Octets | - | V |  |
| Cut Sandstone | V | M:Simple Blocks | V | M:Simple Blocks | M:Simple Octets | - | - |  |
| Smooth Sandstone | V | V | V | M:Simple Blocks | M:Simple Octets | - | - |  |
| Red Sandstone | V | V | V | V | M:Simple Octets | - | V |  |
| Cut Red Sandstone | V | M:Simple Blocks | V | M:Simple Blocks | M:Simple Octets | - | - |  |
| Smooth Red Sandstone | V | V | V | M:Simple Blocks | M:Simple Octets | - | - |  |

### Quarz & Co

| Familie | Block | Treppe | Stufe | Mauer | Octet | Cracked | Chiseled | Anmerkung |
|---|---|---|---|---|---|---|---|---|
| Quartz Block | V | V | V | FEHLT | M:Simple Octets | - | V | Vanilla hat keine Quarz-Mauer |
| Smooth Quartz | V | V | V | FEHLT | M:Simple Octets | - | - |  |
| Quartz Bricks | V | FEHLT | FEHLT | FEHLT | FEHLT | - | - | Vanilla: nur Block, keine Formen |
| Purpur Block | V | V | V | FEHLT | M:Simple Octets | - | - | Pillar = Ausreißer |
| Bricks | V | V | V | V | M:Simple Octets | - | - |  |

### Nether

| Familie | Block | Treppe | Stufe | Mauer | Octet | Cracked | Chiseled | Anmerkung |
|---|---|---|---|---|---|---|---|---|
| Nether Bricks | V | V | V | V | M:Simple Octets | V | V |  |
| Red Nether Bricks | V | V | V | V | M:Simple Octets | M:Simple Blocks | - |  |
| Blue Nether Bricks | M:Simple Blocks | M:Simple Blocks | M:Simple Blocks | M:Simple Blocks | M:Simple Octets | FEHLT | - |  |
| Netherrack | V | M:Simple Blocks | M:Simple Blocks | M:Simple Blocks | M:Simple Octets | - | - |  |
| Smooth Basalt | V | M:Simple Blocks | M:Simple Blocks | M:Simple Blocks | M:Simple Octets | - | - |  |

### End

| Familie | Block | Treppe | Stufe | Mauer | Octet | Cracked | Chiseled | Anmerkung |
|---|---|---|---|---|---|---|---|---|
| End Stone | V | FEHLT | FEHLT | FEHLT | M:Simple Octets | - | - | Vanilla: nur Ziegel-Formen, Block selbst ohne Treppe/Stufe/Mauer |
| End Stone Bricks | V | V | V | V | M:Simple Octets | M:Simple Blocks | - |  |
| Mossy End Stone Bricks | M:Simple Blocks | FEHLT | FEHLT | FEHLT | FEHLT | - | - |  |
| Polished End Stone | M:SimpleBuilding | M:Simple Lib | M:Simple Lib | M:Simple Lib | M:Simple Octets | - | - |  |
| Ender Quartz Block | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | FEHLT | M:Simple Octets | - | M:SimpleBuilding | Chiseled hier = chiseled_ender_quartz_bricks |
| Ender Quartz Bricks | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | M:Simple Octets | - | - |  |
| Polished Ender Quartz | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | M:Simple Octets | - | - |  |
| Block of Astralit | M:SimpleBuilding | FEHLT | FEHLT | FEHLT | FEHLT | - | - |  |
| Astralit Bricks | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | M:Simple Octets | - | M:SimpleBuilding |  |
| Polished Astralit | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | M:Simple Octets | - | - |  |
| Block of Nihilith | M:SimpleBuilding | FEHLT | FEHLT | FEHLT | FEHLT | - | - |  |
| Nihilith Bricks | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | M:Simple Octets | - | M:SimpleBuilding |  |
| Polished Nihilith | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | M:SimpleBuilding | M:Simple Octets | - | - |  |

### Prismarin

| Familie | Block | Treppe | Stufe | Mauer | Octet | Cracked | Chiseled | Anmerkung |
|---|---|---|---|---|---|---|---|---|
| Prismarine | V | V | V | V | M:Simple Octets | FEHLT | - |  |
| Prismarine Bricks | V | V | V | FEHLT | M:Simple Octets | FEHLT | - |  |
| Dark Prismarine | V | V | V | FEHLT | M:Simple Octets | FEHLT | - |  |

### Erde & Harz

| Familie | Block | Treppe | Stufe | Mauer | Octet | Cracked | Chiseled | Anmerkung |
|---|---|---|---|---|---|---|---|---|
| Mud Bricks | V | V | V | V | M:Simple Octets | M:Simple Blocks | - |  |
| Mossy Mud Bricks | M:Simple Blocks | FEHLT | FEHLT | FEHLT | FEHLT | - | - |  |
| Resin Bricks | V | V | V | V | M:Simple Octets | - | V |  |

### Zinnober & Schwefel

| Familie | Block | Treppe | Stufe | Mauer | Octet | Cracked | Chiseled | Anmerkung |
|---|---|---|---|---|---|---|---|---|
| Cinnabar | V | V | V | V | M:Simple Octets | - | V |  |
| Polished Cinnabar | V | V | V | V | M:Simple Octets | - | - |  |
| Cinnabar Bricks | V | V | V | V | M:Simple Octets | M:Simple Blocks | - |  |
| Mossy Cinnabar Bricks | M:Simple Blocks | FEHLT | FEHLT | FEHLT | FEHLT | - | - |  |
| Sulfur | V | V | V | V | M:Simple Octets | - | V |  |
| Polished Sulfur | V | V | V | V | M:Simple Octets | - | - |  |
| Sulfur Bricks | V | V | V | V | M:Simple Octets | M:Simple Blocks | - |  |
| Mossy Sulfur Bricks | M:Simple Blocks | FEHLT | FEHLT | FEHLT | FEHLT | - | - |  |

### Sonstige

| Familie | Block | Treppe | Stufe | Mauer | Octet | Cracked | Chiseled | Anmerkung |
|---|---|---|---|---|---|---|---|---|
| Calcite | V | M:Simple Blocks | M:Simple Blocks | M:Simple Blocks | M:Simple Octets | - | - |  |

## Fehlende Glieder (Id, Ziel-Mod)

| Familie | Glied | Id | Mod |
|---|---|---|---|
| Stone | Mauer | `stone_wall` | Simple Blocks |
| Smooth Stone | Treppe | `smooth_stone_stairs` | Simple Blocks |
| Polished Granite | Mauer | `polished_granite_wall` | Simple Blocks |
| Polished Diorite | Mauer | `polished_diorite_wall` | Simple Blocks |
| Polished Andesite | Mauer | `polished_andesite_wall` | Simple Blocks |
| Polished Deepslate | Cracked | `cracked_polished_deepslate` | Simple Blocks |
| Mossy Deepslate Bricks | Treppe | `mossy_deepslate_brick_stairs` | Simple Blocks |
| Mossy Deepslate Bricks | Stufe | `mossy_deepslate_brick_slab` | Simple Blocks |
| Mossy Deepslate Bricks | Mauer | `mossy_deepslate_brick_wall` | Simple Blocks |
| Mossy Deepslate Bricks | Octet | `mossy_deepslate_bricks_octet` | Simple Octets |
| Mossy Deepslate Tiles | Treppe | `mossy_deepslate_tile_stairs` | Simple Blocks |
| Mossy Deepslate Tiles | Stufe | `mossy_deepslate_tile_slab` | Simple Blocks |
| Mossy Deepslate Tiles | Mauer | `mossy_deepslate_tile_wall` | Simple Blocks |
| Mossy Deepslate Tiles | Octet | `mossy_deepslate_tiles_octet` | Simple Octets |
| Mossy Tuff Bricks | Treppe | `mossy_tuff_brick_stairs` | Simple Blocks |
| Mossy Tuff Bricks | Stufe | `mossy_tuff_brick_slab` | Simple Blocks |
| Mossy Tuff Bricks | Mauer | `mossy_tuff_brick_wall` | Simple Blocks |
| Mossy Tuff Bricks | Octet | `mossy_tuff_bricks_octet` | Simple Octets |
| Quartz Block | Mauer | `quartz_wall` | Simple Blocks |
| Smooth Quartz | Mauer | `smooth_quartz_wall` | Simple Blocks |
| Quartz Bricks | Treppe | `quartz_brick_stairs` | Simple Blocks |
| Quartz Bricks | Stufe | `quartz_brick_slab` | Simple Blocks |
| Quartz Bricks | Mauer | `quartz_brick_wall` | Simple Blocks |
| Quartz Bricks | Octet | `quartz_bricks_octet` | Simple Octets |
| Purpur Block | Mauer | `purpur_wall` | Simple Blocks |
| Blue Nether Bricks | Cracked | `cracked_blue_nether_bricks` | Simple Blocks |
| End Stone | Treppe | `end_stone_stairs` | Simple Blocks |
| End Stone | Stufe | `end_stone_slab` | Simple Blocks |
| End Stone | Mauer | `end_stone_wall` | Simple Blocks |
| Mossy End Stone Bricks | Treppe | `mossy_end_stone_brick_stairs` | Simple Blocks |
| Mossy End Stone Bricks | Stufe | `mossy_end_stone_brick_slab` | Simple Blocks |
| Mossy End Stone Bricks | Mauer | `mossy_end_stone_brick_wall` | Simple Blocks |
| Mossy End Stone Bricks | Octet | `mossy_end_stone_bricks_octet` | Simple Octets |
| Ender Quartz Block | Mauer | `ender_quartz_wall` | SimpleBuilding |
| Block of Astralit | Treppe | `astralit_stairs` | SimpleBuilding |
| Block of Astralit | Stufe | `astralit_slab` | SimpleBuilding |
| Block of Astralit | Mauer | `astralit_wall` | SimpleBuilding |
| Block of Astralit | Octet | `astralit_block_octet` | Simple Octets |
| Block of Nihilith | Treppe | `nihilith_stairs` | SimpleBuilding |
| Block of Nihilith | Stufe | `nihilith_slab` | SimpleBuilding |
| Block of Nihilith | Mauer | `nihilith_wall` | SimpleBuilding |
| Block of Nihilith | Octet | `nihilith_block_octet` | Simple Octets |
| Prismarine | Cracked | `cracked_prismarine` | Simple Blocks |
| Prismarine Bricks | Mauer | `prismarine_brick_wall` | Simple Blocks |
| Prismarine Bricks | Cracked | `cracked_prismarine_bricks` | Simple Blocks |
| Dark Prismarine | Mauer | `dark_prismarine_wall` | Simple Blocks |
| Dark Prismarine | Cracked | `cracked_dark_prismarine` | Simple Blocks |
| Mossy Mud Bricks | Treppe | `mossy_mud_brick_stairs` | Simple Blocks |
| Mossy Mud Bricks | Stufe | `mossy_mud_brick_slab` | Simple Blocks |
| Mossy Mud Bricks | Mauer | `mossy_mud_brick_wall` | Simple Blocks |
| Mossy Mud Bricks | Octet | `mossy_mud_bricks_octet` | Simple Octets |
| Mossy Cinnabar Bricks | Treppe | `mossy_cinnabar_brick_stairs` | Simple Blocks |
| Mossy Cinnabar Bricks | Stufe | `mossy_cinnabar_brick_slab` | Simple Blocks |
| Mossy Cinnabar Bricks | Mauer | `mossy_cinnabar_brick_wall` | Simple Blocks |
| Mossy Cinnabar Bricks | Octet | `mossy_cinnabar_bricks_octet` | Simple Octets |
| Mossy Sulfur Bricks | Treppe | `mossy_sulfur_brick_stairs` | Simple Blocks |
| Mossy Sulfur Bricks | Stufe | `mossy_sulfur_brick_slab` | Simple Blocks |
| Mossy Sulfur Bricks | Mauer | `mossy_sulfur_brick_wall` | Simple Blocks |
| Mossy Sulfur Bricks | Octet | `mossy_sulfur_bricks_octet` | Simple Octets |

## Bau-Reihenfolge und Aufwand

Aufwand je Glied: Treppe/Stufe/Mauer über den vorhandenen `shapes()`-Helfer in `BlocksRegistry` (Blockstate, Model, Item, Lang EN+DE, Loot, Rezept, Formsäge-Rezept, Tags) ungefähr 10 Minuten Agentenarbeit je Familie-Satz; neue Vollblock-Textur nach Endholz-Prozess (A8b) rund 30 Minuten je Textur; Octet je Block über die Octet-Liste. Alles zusammen eine Welle, ein Gradle-Lauf.

1. **Welle A, Mossy-Familien** (7 Familien, je Treppe/Stufe/Mauer + Octet, keine neuen Texturen): Mossy Deepslate Bricks, Mossy Deepslate Tiles, Mossy Tuff Bricks, Mossy Mud Bricks, Mossy End Stone Bricks, Mossy Cinnabar Bricks, Mossy Sulfur Bricks. 21 Formen + 7 Octets. Mod: Simple Blocks / Simple Octets.
2. **Welle B, Prismarin**: Cracked Prismarine, Cracked Prismarine Bricks, Cracked Dark Prismarine (3 neue Texturen) + Mauer für Prismarine Bricks und Dark Prismarine. Simple Blocks.
3. **Welle C, Cracked-Reste**: Cracked Polished Deepslate, Cracked Blue Nether Bricks (2 Texturen). Simple Blocks.
4. **Welle D, Mauern/Rest der Vanilla-Familien**: Polished Granite/Diorite/Andesite Wall, Stone Wall, Quartz Wall, Smooth Quartz Wall, Purpur Wall, Smooth Stone Stairs, Quartz Bricks (Treppe/Stufe/Mauer + Octet), End Stone (Treppe/Stufe/Mauer). Simple Blocks, Octet in Simple Octets.
5. **Welle E, End-Paletten**: Block of Astralit/Nihilith (Treppe/Stufe/Mauer + Octet), Ender Quartz Wall. SimpleBuilding (bleibt dort bis zum Enderite-Umzug, Inventar F4); Octets in Simple Octets.
6. **Abschluss**: Familien-Reihen in Testzentrum/Galerie und Kreativ-Tab (Reihenfolge wie Bild), Abdeckungstests (Octet-Regel, Formsäge-Rezepte je neues Glied, Lang), Wiki/Guide-Zeile, ein Gradle-Lauf `:mc26_3:fabric` + `module-simpleblocks`/`simpleoctets` Gametests + `check`.

Reihenfolge-Begründung: A zuerst (größter Block, reine Wiederholung, Besitzer nennt es ausdrücklich), B/C danach wegen Texturen (Vorschau für Textur-Wahl exemplarisch, Regel „UI/Optik erst exemplarisch“), D/E am Ende (Randfälle).

## Rückfragen (mit Standard-Entscheidung)

- Vanilla-lose Mauern (Stone, Quartz, Smooth Quartz, Purpur, Polished Granite/Diorite/Andesite): Standard = alle bauen, da Besitzer „Polished Granite Wall“ nennt und die Palette vollständig haben will. Reduktion möglich.
- Smooth Stone: Standard = nur Treppe ergänzen (Stufe ist Vanilla), keine Mauer.
- Quartz Bricks: Vanilla hat keine Formen; Standard = volle Palette in Simple Blocks.
- Cracked/Chiseled-Glieder, die Vanilla nicht kennt (z. B. Chiseled Prismarine, Cracked Cobbled Deepslate): Standard = nicht bauen, nur die vom Besitzer genannten.
- Kupfer-Schnittblöcke: nicht Teil der Aufgabe, Hinweis im Dokument.

