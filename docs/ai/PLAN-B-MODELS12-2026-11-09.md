# Plan b-models12: Simple Models Editor, Stufe 0 + 1 (IG31-60, 09.11.2026)

Basis `a-models11` (Konzept `docs/ai/KONZEPT-A-MODELS11-2026-11-09.md`). Besitzer-Wahl 09.11.:
**MODELS11 = A (Blockbench-klassisch) plus Umschaltknopf „nur Code“** (Code-Editor | Live-Vorschau nebeneinander).
Nur 26.3 Fabric. Kein Gate (macht A). Vorschau `/root/sb-tools-b/previews/b-models12-preview.html`, Antwortcode
**`MODELS12: code=A|B|C + Anmerkung`**.

## Ist-Stand (erhoben 09.11.)
- `modules/simplemodels`: Katalog + Amboss-Zuweisung, kein Editor, keine Geometrie. Beispiel nur `renamed:tomato`
  (`examples/`, manuell zu installieren). Server-Gametests in `ModelTests.ALL` + Fabric-Adapter `ModuleGameTest`
  (`tools/check_data.py` prüft die Parität). Kein JUnit-Sourceset im Modul → Logik-Tests als Gametests (ohne Welt).
- Vanilla 26.3 (`/root/vsrc263/.../model/cuboid/`): `from`/`to` -16..32, `rotation` mit `origin` und `axis`+`angle`
  **oder** `x`/`y`/`z`, `rescale`; `light_emission` 0..15; `shade_direction_override` (das alte `shade` gibt es nicht
  mehr); Flächen `uv`, `texture`, `cullface`, `rotation` 0/90/180/270, `tintindex`; `display` mit `rotation`,
  `translation` (JSON ±80), `scale` (±4), `right_rotation`. Item-Modelle dürfen Block-Texturen nutzen
  (`MaterialBaker`: erst Item-Atlas, dann Block-Atlas).

## Stufe 0: Beispielmodelle (eigener Commit)
- 6 Modelle (Konzept §6, Nr. 1–6): Hocker (`oak_slab`), Tischlaterne (`lantern`), Briefkasten (`barrel`), Vogelhaus
  (`oak_fence`), Wetterfahne (`lightning_rod`), Kristallgruppe (`amethyst_cluster`). Nur Würfel mit
  `minecraft:block/...`-Verweisen (A8: keine Pixel eingecheckt), Elternmodell `minecraft:block/block` (Display-Werte).
- Dateien im Modul-Jar (= eingebautes Pack, immer geladen, da Mod-Ressourcen): `assets/simplemodels/models/item/example/<n>.json`,
  `assets/simplemodels/items/example/<n>.json`; Katalog-Einträge `data/simplemodels/model_examples/<n>.json`.
- `ModelExamples`: liest die 6 Einträge aus dem Klassenpfad (feste Namensliste, kein Ordner-Scan), `merge(snapshot)` hängt
  sie an den geladenen Katalog an (Ordner-Einträge gewinnen bei gleicher ID, `maxModels` gilt weiter). `Models.reload`
  nutzt `merge`. Neue Policy-Option `builtinExamples` (Standard **true**, abschaltbar in `config/simplemodels/config.json`),
  mit Lang EN/DE inkl. „Default:“/„Standard:“, `balance/simplemodels/options.json`, `wiki/manual.json` (`config_builtinExamples`,
  neues Feature `examples`), Discover-Liste.
- Gametest `examples`: 6 Einträge, gültig, Basis-Items Vanilla, Modell-JSONs im Jar mit Grenzen, Zuweisung je Beispiel im
  Amboss-Weg (`Models.assign`), Schalter aus = keine Beispiele, Ordner-Eintrag gleicher ID gewinnt.
- **Abweichung vom Konzept:** keine zweite Kopie unter `modules/simplemodels/examples/` (nur Tomate bleibt dort als
  Muster für manuelle Packs) – eine Quelle statt zwei, die auseinanderlaufen können.

## Stufe 1: Datenmodell + JSON + Undo (eigener Commit)
- Paket `simplemodels.editor` (nur Java + Gson, keine Client-/MC-Importe → server- und testfähig):
  - `EditableModel` (unveränderliche Records): `parent`, `textures`, `elements` (`Element`: `name`, `from`, `to`,
    `rotation` (`Rotation`: `origin`, Einachs `axis`+`angle` oder Euler `x/y/z`, `rescale`), `shadeDirection`,
    `lightEmission`, `faces` (`Face`: `texture`, `uv`, `rotation`, `cullface`, `tintindex`)), `display` (`Transform`),
    `ambientOcclusion`, `guiLight`. Hilfen `withElement/addElement/removeElement`.
  - `ModelJson.read(String)` → `Result(model, problems)`; `Problem(path, key, args)` mit Pfad wie `elements[2].from`
    (Zeile/Spalte kommen mit der DSL in Stufe 4). Alles-oder-nichts: bei Problemen kein Modell. `write(model)` →
    Vanilla-JSON (ganze Zahlen ohne `.0`, Standardwerte weggelassen). Elementname `name` bleibt im JSON (wie Blockbench,
    Vanilla ignoriert unbekannte Felder).
  - Grenzen: Vanilla (siehe oben) + Editor: max. 256 Würfel, JSON max. 65 536 Zeichen, Verschachtelung ≤ 16, uv 0..16,
    Texturen `#var` oder gültige ID, Display-Slots nur die 9 Vanilla-Namen.
  - `EditHistory<T>`: Undo/Redo-Stapel, Grenze 200, gleicher Stand = kein Eintrag, neuer Eintrag löscht Redo
    (ein Gizmo-Zug = ein `push` am Zugende).
- Gametests `editable_round_trip` (alle Felder, Einachs + Euler, 6 Beispiele JSON → Modell → JSON gleich),
  `editable_bounds` (je Grenze ein Fehlerfall mit Pfad), `edit_history`.

## UI (exemplarisch, noch kein Screen-Code)
Stufe 1 hat keine Bildschirm-UI. Vorschau mit Layout A + Modus „nur Code“ (drei Varianten der Code-Ansicht) zur Abnahme;
umgesetzt wird die Optik erst in Stufe 2/4 nach Antwort MODELS12. Kontrast: `#404040` auf `#C6C6C6` 6,6:1,
`#F2EEE8` auf `#1E1F23` ≈ 15:1, Code-Kommentar `#8A8F98` auf `#1E1F23` ≈ 5,5:1.

## Verifikation
- Lokal ohne Gradle: `python3 tools/check_missing_textures.py --jar <client.jar>`, `modules/simplemodels/tools/check_data.py`.
- Ein sbt-Lauf: `:mc26_3:fabric:compileJava :modules:simplemodels:fabric:compileJava` + `module-simplemodels-fabric-263`,
  `module-simplemodels-standalone-fabric-263`.
- Nicht getestet: Client-Sicht der Beispielmodelle (Bündel/Client-Abnahme), UI (gibt es noch nicht).
