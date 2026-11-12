# Plan A: ICEBRICKS38 – Eis-Ziegel als eigene Familie (Simple Blocks, Besitzer 12.11.)

Branch `a-icebricks38`, Basis `hub/claude-wave1` (240d278d5). Auftrag `/root/sb-tools-a/agenten-1530.md` Abschnitt 8. Nur 26.3 Fabric.

## 1. Ist-Stand

- PAL14 (b-pal14, `docs/ai/PLAN-B-PAL14-2026-11-09.md`): je Eisart `polished_<eis>`, `<eis>_bricks` (Vollblock), `<eis>_stairs|slab|wall`
  (Formen des **Grundeises**), dazu `chiseled_ice`; `chiseled_packed_ice`, `chiseled_blue_ice`, `cracked_ice|packed_ice|blue_ice` gibt es
  seit B23/ABN30 (NatureBlocks). Eis-Ziegel haben **keine** Treppe/Stufe/Mauer, keine rissige Variante, kein Octet.
- Logik vorhanden und wiederverwendbar: `IceShapes` (schmelzende Treppe/Stufe/Mauer aus normalem Eis), `IceBlock` für Vollblöcke,
  Loot „nur Behutsamkeit“ (`generate_data.ICE_ALL`), Tag `#minecraft:ice`, Octet-Vorlagen (`OctetRegistry.TEMPLATES`),
  Riss-Overlay `blocks_b23_2026_10_23.cracked` (rissige Ziegel N69/a-palette05), Ziegelmuster `pal14_2026_11_09.bricks`.
- Testwelt: `ModBlockGallery` stellt Familien aus der Id automatisch aus – keine Code-Änderung nötig.

## 2. A12 Vanilla-Nutzen

(1) Nutzen: Eis-Ziegel lassen sich wie jede Vanilla-Ziegelfamilie verbauen (Treppen, Stufen, Mauern, Achtel, rissige Variante für
Ruinen-/Alters-Look). (2) Änderung: nur neue Baublöcke, kein Eingriff in Vanilla-Blöcke. (3) Eingriff: keiner.

## 3. Neue Glieder (12 + 3 Octets)

Je Eisart `<k>` ∈ {ice, packed_ice, blue_ice}:
- `cracked_<k>_bricks` – Vollblock (normales Eis: `IceBlock`, schmilzt wie Vanilla-Eis; Pack-/Blaueis: `Block`, schmilzt nie).
- `<k>_brick_stairs|slab|wall` – normales Eis: `IceShapes` (schmelzen), sonst Vanilla-Klassen.
- Octet `simpleoctets:<k>_bricks_octet` (Octet-Regel O1), Vorlage = die Eisart (Reibung/Klang/Härte), nur mit Simple Blocks.

**Gemeißelt/poliert:** jede Eisart hat schon `polished_<k>` und ein gemeißeltes Glied (`chiseled_ice|packed_ice|blue_ice`) – die Familie
verlangt kein weiteres. Entscheidung (Vanilla-Logik wie gemeißelte Steinziegel aus 2 Steinziegelstufen): **2 Eisziegelstufen
übereinander → 1 gemeißeltes Eis der Art** (Werkbank) und Steinmetz `<k>_bricks` → gemeißeltes Eis; so hängt das gemeißelte Glied
an der Ziegelfamilie, ohne einen vierten gemeißelten Block zu erfinden.

## 4. Logik (begründet)

- Eigenschaften `ofFullCopy` der Eisart → Reibung 0,98 (Eis/Packeis) / 0,989 (Blaueis), Glas-Klang, Härte; normales Eis durchscheinend.
- Schmelzen nur normales Eis (Vanilla: nur `ice` schmilzt). Octets schmelzen nicht (wie das vorhandene Eis-Octet, `MaterialOctetBlock`).
- Beute: nur mit Behutsamkeit (wie Vanilla-Eis und alle PAL14-Eisglieder), doppelte Stufe → 2.
- Rissig: Vanilla macht rissige Ziegel im Ofen – Eis würde schmelzen. Daher **Steinmetz** `<k>_bricks` → `cracked_<k>_bricks`
  (wie `cracked_ice` aus Eis im Steinmetz). Rissige Eisziegel sind Deko, sie brechen nicht weiter (anders als „Rissiges Eis“).
- Rezepte: Werkbank Treppe 6→4, Stufe 3→6, Mauer 6→6 aus Eisziegeln; Steinmetz wie Vanilla-Tiefenschieferziegel: Eisziegel,
  Poliertes Eis **und** Grundeis → Ziegel-Treppe/-Stufe(2)/-Mauer; Formsäge: Eisziegel → 8 Octets (Octet-Generator).
- Tags: `mineable/pickaxe`, `stairs|slabs|walls` (Block + Item), rissige Eisziegel in `#minecraft:ice` (wie die Eisziegel).
- Preise: `excluded` wie alle Simple-Blocks-Baublöcke und Octets.
- Kreativ: Simple-Blocks-Tab direkt hinter den Eisziegeln; Vanilla-Natur-Tab in Vanilla-Reihenfolge (Grundformen, gemeißelt, poliert,
  Ziegel, rissig, Ziegel-Formen).

## 5. Texturen (A8/A8c/A8d)

Rissige Eisziegel = Vanilla-abgeleitet (aus dem Vanilla-Eis gebaut wie die Eisziegel selbst, nie eingecheckt): neuer Generator
`tools/textures/icebricks38_2026_11_12.py` (Ziegel aus `pal14.bricks`, eigene Risse, Alpha der Risse angehoben, damit sie im
durchsichtigen Eis lesbar sind), Eintrag in `vanilla_derived.MODULE_OUTPUTS/GENERATORS['simpleblocks']`, `.gitignore`.
Treppe/Stufe/Mauer/Octet nutzen die Eisziegel-Textur (wie alle Formen). Runden + Bewertung: Abschnitt 8.

## 6. Dateien

Simple Blocks: `tools/generate_data.py` (+ Ausgabe), `BlocksRegistry.java`, `test/BlocksTests.java` + Fabric-Adapter, `wiki/manual.json`,
`tools/check_data.py` (Anzahl). simpleoctets: `tools/textures/octet_materials.json`, `OctetRegistry.TEMPLATES`, Generator-Ausgabe.
simplemoney: Preise (excluded). tools/textures: Generator, `vanilla_derived.py`, `.gitignore`. Discover/Wiki-Daten: Generatoren.

## 7. Prüfung

Lokal: `generate_data.py --check`, alle `modules/*/tools/check_data.py`, Octet-Generator `--check`, `precheck.py --fast`,
`check_dup_impl`, `check_tooltip_text`, `wiki/generate.py --all --check`, `check_missing_textures`, `layers.py check`.
Ein sbt: Compile + `module-simpleblocks` (fabric + standalone) + `module-simpleoctets` (26.3 Fabric).
Gametests: neuer Test `icebricks38_family` (Steinmetz-Schnitte, gemeißelt aus 2 Stufen, Tab-Plätze, rissig kopiert die Eisart);
`pal14_ice` deckt über `BlocksRegistry.ICE` Reibung/Klang/Schmelzen der neuen Glieder mit ab; `recipes_and_loot_exist` Rezept + Beute.

## 8. Runden-Bewertung Texturen

Geprüft je Runde als Blatt (16 px → 8× nearest-neighbour) neben Vanilla `cracked_stone_bricks` (hart 5 %, größte Fläche 12 px,
Flächen ≥ 6 px 29 %), `cracked_deepslate_bricks` (7 %, 10 px, 25 %) und den eigenen Eisziegeln (4–6 %, 8–9 px, 6–12 %).
Risse in der Sprache des abgenommenen Rissigen Eises (ice31/tex24: Risskern fast weiß, Schattenseite tiefer, Spitzen laufen aus).

| Runde | Eis / Packeis / Blaueis hart | Bewertung | Änderung danach |
|---|---|---|---|
| R1 | 20 / 21 / 21 % | Risse gut lesbar, aber zu hart: gleich weißer Kern, volle Schattenseite, Haarriss wirkt krümelig | Spitzen ausblenden, Schatten halb |
| R2 | 12 / 16 / 15 % | ruhiger; Pack-/Blaueis noch hart – fast alle harten Stufen dort, wo ein weißer Riss eine dunkle Fuge kreuzt | Tondrift alle 2 px (A8d 4), Haarriss weg |
| R3 | 10 / 14 / 13 % | Linie lebendiger, Packeis weiter > 12 % (Fugen) | Kern auf Fugen ×0,4, keine Schattenseite in Fugen, Kern 0,62 |
| R4 (gebaut) | 8 / 10 / 8 % | Risse laufen über 2–3 Ziegel und über die Fugen wie Vanilla, lesbar in Spielgröße, keine flachen Stellen (größte Fläche 7–9 px, Flächen ≥ 6 px 5–6 %); Kerne auf normalem Eis deckender (Alpha → 210), damit sie im durchsichtigen Block stehen | – |

Ein Client-Screenshot (Block im Spiel) war nicht erlaubt/geplant (keine Client-Läufe); offen für die Abnahme im Bündel.
Vorschau mit allen Runden: `/root/sb-tools-a/previews/a-icebricks38-preview.html` (Vanilla-Pixel nur dort, nie im Repo).

## 9. Abweichungen

- **Kein neuer gemeißelter Block:** jede Eisart hat schon ein gemeißeltes Glied; stattdessen Werkbank 2 Ziegelstufen → gemeißeltes Eis
  der Art und Steinmetz Ziegel → gemeißeltes Eis (Vanilla-Logik gemeißelte Steinziegel).
- **Rissig aus dem Steinmetz** statt Ofen (Eis würde schmelzen).
- **Octets schmelzen nicht** (wie das vorhandene Eis-Octet); Ziegel-Formen aus normalem Eis hinterlassen beim Abbau kein Wasser
  (wie die PAL14-Formen, Teilblock).
- Vanilla-Natur-Tab: die vorhandenen Eis-Glieder stehen jetzt in Vanilla-Reihenfolge (Grundformen vor gemeißelt/poliert/Ziegel).
- Octet-Generator: `PALETTE_EXTRAS` überspringt Einträge ohne Anker (vorher Absturz bei `--scan`, weil die Eis-Grundeinträge
  nicht aus dem Jar gescannt werden).
- Wiki-Daten nur für Simple Blocks/Simple Octets/Texturen neu erzeugt; ein vorbestehender, fremder Unterschied in
  `wiki/data/simpledimensions.*` (Basis veraltet) bleibt dem Bündel.
