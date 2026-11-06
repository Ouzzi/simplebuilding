# Plan A: Kompakte Blöcke Stufe 1 Compact / Stufe 2 Dense (WH26-10, a-wh26-10, 06.11.2026)

## Auftrag
Stufe 1 „Compact“ = 9 Blöcke, Stufe 2 „Dense“ = 9 Compact, für die meisten Vanilla-Blöcke; Abbau ca. 4× so lange wie der Grundblock. Zuständigkeit Simple Blocks; Optik zuerst exemplarisch (Vorschau).

## Entscheidungen (selbst begründet)
- **Blöcke (19 Basen, 38 Blöcke):** Erde, Schlamm, Bruchstein, Stein, Sand, Roter Sand, Kies, Ton, Netherrack, Bruchtiefenschiefer, Andesit, Diorit, Granit, Tuff, Sandstein, Roter Sandstein, Endstein, Schwarzstein, Seelensand. Nur Vollblöcke ohne Zustand/Blockentität, die man in großen Mengen sammelt. Bewusst nicht: Erze/Metallblöcke (haben schon 9er-Blöcke), Holz/Planken (viele Familien), Glas, Wolle, Blöcke mit Zuständen.
- **Namen:** `simpleblocks:compact_<x>` und `simpleblocks:dense_<x>` (DE „Kompakter/Dichter …“). Ein Tab „Simple Blocks“ (nach den Pale-Blöcken), keine Vanilla-Tab-Einsortierung (A11, IG31-3).
- **Rezepte (je Basis 4):** 9 Basis → 1 Compact, 9 Compact → 1 Dense (geformt 3×3), 1 Compact → 9 Basis, 1 Dense → 9 Compact (formlos). Verlustfrei, keine neuen Quellen.
- **Abbau:** Härte Compact = 4 × Basis, Dense = 4 × Compact (16 × Basis); Werkzeugregel, Klang und Kartenfarbe wie die Basis (`ofFullCopy`), Explosionswiderstand unverändert. Drop: der Block selbst (mit dem passenden Werkzeug, Tags mineable/pickaxe bzw. shovel).
- **Sand/Kies fallen nicht** (einfache Blöcke): verdichtet = stabil. Hinweis im Wiki.
- **A12 Vanilla-Nutzen und Eingriff:** Nutzen: platzsparend lagern und bauen; Eingriff: gering, nichts Vanillas ändert sich, keine Config nötig (Block-Eintrag fehlt nur, wenn Simple Blocks aus ist). Kein Handel (Simple Money: ausgeschlossen mit Grund).
- **Module allein spielbar:** nur Vanilla-Zutaten, keine Abhängigkeit.
- **Texturen:** Vanilla-abgeleitet beim Bauen (`tools/textures/compact_2026_11_06.py`, `vanilla_derived.MODULE_OUTPUTS['simpleblocks']`), nichts eingecheckt. Variante A „Rahmen“ (gepresster Rand, Dense zusätzlich innere Stufe) als Grundgerüst für alle 19; B „Körnung“ und C „Gurte“ nur in der Vorschau `/root/sb-tools-b/previews/a-wh26-10-preview.html` (Antwortcode `WH2610-A|B|C`). Der Anteil harter Übergänge von A liegt auf Vanilla-Niveau (A8b).

## Dateien
`CompactBlocks.java` (Registrierung, Tab, Teststation), `BlocksRegistry`/`SimpleBlocks` (Einhängen), `generate_data.py` (+ `check_data.py`: Modelle, Blockstates, Loot, Rezepte, Advancements, Tags, Lang EN+DE), `CompactTests.java` + 3 Adapter in `ModuleGameTest`, `wiki/manual.json`, Simple-Money-Ausschlussliste, `vanilla_derived.py` + `.gitignore`.

## Risiken
- Hartes Werkzeug-Tempo: Dense Bruchtiefenschiefer 56 s Härte (3,5 × 16) – gewollt „schwerer“; Faktor in `CompactBlocks.FACTOR`.
- Rezeptbuch-Menge: 76 Rezepte; kein Konflikt mit Vanilla-3×3-Rezepten (geprüft über den Rezeptmanager im Test).

## Verifikation
`generate_data.py --check`, `check_data.py`, `check_lang_keys`, `layers check` (lokal); Gradle einmal: `:mc26_3:fabric:compileJava` + `module-simpleblocks-fabric-263` und `-standalone-` mit Filter `compact*`. `./gradlew check` und Wiki-Neuerzeugung (`wiki/data`) macht die Bündel-Vorbereitung.

## Offen / Rückfrage
- **Rückfrage Besitzer:** Look A/B/C (Vorschau), Faktor 4×/16× oder 4× für beide Stufen, ob weitere Blöcke (Holz, Glas, Wolle, Prismarin) gewünscht sind.
- NeoForge/Forge-Läufe nicht (26.3 Fabric Vorgabe).
