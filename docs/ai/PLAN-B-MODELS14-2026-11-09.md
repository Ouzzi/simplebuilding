# Plan b-models14: Simple Models Editor – UV, Texturwahl, Speichern/Export, Raster, Kürzel-Hilfe (IG31-60, 09.11.2026)

Basis `hub/b-bundle163` (enthält b-models13 + b-fix162). Auftrag N7 (`/root/b-auftrag-20261109.md`): nächste Stufen laut
Konzept `docs/ai/KONZEPT-A-MODELS11-2026-11-09.md` §3/§5/§7 (Stufe 5 + 6 ohne Display-Reiter/Beispiele 7–8). Nur 26.3
Fabric, kein Gate. Besitzer-Antworten: `grep MODELS1[234]` in Auftrag/Queue → keine Antwort auf MODELS12/13 (nur Fragen)
→ Standard bleibt (Layout A, Nur Code = Variante A). Vorschau `/root/sb-tools-b/previews/b-models14-preview.html`,
Antwortcode **`MODELS14: ok | nachbessern=<punkt>`**.

## Ist-Stand (erhoben 09.11.)
- Editor-Daten/Logik: `EditableModel`, `ModelJson` (Probleme mit JSON-Pfad, keine Zeile/Spalte), `EditHistory`,
  `ModelGeometry` (Bake-Mathe, Pick ohne Fläche), `Gizmo` (Raster 1/0,5/0,25, Drehen 22,5° fest), `ModelEditorState`,
  `ModelEditorLayout`. Client: `ModelEditorScreen` (Werkzeugleiste, Liste, Viewport, Eigenschaften von/bis/Drehpunkt,
  Nur Code mit `UiCodeArea`), `ModelViewport`.
- Fehlt: UV-Editor, Flächen an/aus/cull/tint, Texturwahl, Speichern/Export, Raster aus/Drehraster wählbar,
  Tastenkürzel-Übersicht. Bekannte Lücken models13: JSON-Fehler nur als Pfad (keine Unterstreichung), lange JSON-Zeilen
  brechen bei GUI-Skala 2 in der Code-Spalte um.
- Pack-Format 26.3: `SharedConstants.RESOURCE_PACK_FORMAT_MAJOR` (97); Pack-Liste über `PackRepository.reload/addPack`
  + `Options.updateResourcePacks` (lädt neu), sonst `Minecraft.reloadResourcePacks()`.

## Stufen (je eigener Commit)
**S1 Raster/Snap + Kürzel-Hilfe + Code-Lücken**
- `Gizmo`: Raster-Stufen als Paare (Schritt px / Drehwinkel): 1/22,5°, 0,5/15°, 0,25/5°, aus/frei. Ein Knopf
  schaltet weiter (Werkzeugleiste bleibt bei GUI-Skala 3 auf 1280×720 in einer Zeile). Shift = 1°, Alt = frei bleiben.
- `editor.EditorShortcuts`: Liste (Taste, Lang-Schlüssel, Gruppe) = einzige Quelle für die Hilfe-Ansicht (F1 / „?“).
  Test: jeder Schlüssel EN+DE vorhanden.
- `ModelJson.locate(text, path)`: Zeichenbereich des Werts zu einem Problem-Pfad (`elements[2].faces.up.uv[1]`), bei
  Syntaxfehlern Zeile/Spalte aus der Gson-Meldung. Screen unterstreicht den Bereich in `UiCodeArea` (Problem-Range).
- `ModelJson.writeCompact(model, width)`: Breite = Zeichen, die in die Code-Spalte passen → kein Umbruch bei Skala 2.
**S2 UV-Editor + Flächen**
- `editor.UvEdit` (rein): wirksame UV (eigene oder Auto), UV setzen/geklemmt 0..16, Auto-UV, Drehen 0/90/180/270,
  Spiegeln U/V, Ziehen (ganzes Rechteck oder Ecke, Raster), Panel↔UV-Umrechnung, Fläche an/aus (≥1 bleibt), cullface
  und tintindex durchschalten.
- `ModelGeometry.hitFace`: welche Fläche der Strahl trifft (Strg+Klick im Viewport wählt Fläche → Reiter Fläche).
- `ModelEditorState`: gewählte Fläche, `editElement` (ein Undo-Schritt), UV-Ziehen über den vorhandenen Drag-Mechanismus.
- Screen: Eigenschaften-Panel mit Reitern **Würfel | Fläche**; Fläche: 6 Flächenknöpfe, UV-Feld (Textur der Fläche aus
  dem Block-Atlas, Rechteck ziehen/Ecken), Knöpfe Drehen/Auto/↔/↕, An/Aus, Cull, Tint, Textur-Zeile.
**S3 Texturwahl**
- `editor.TextureChoice` (rein): Variablen-Liste mit aufgelöstem Wert, Suche über Atlas-IDs (alle Wörter, sortiert,
  Grenze), neue Variable mit freiem Namen, Zuweisen nur gültiger IDs.
- Client `TexturePickerScreen`: links Variablen (Klick = Fläche nutzt diese Variable, „+“ neu), rechts Suchfeld + Raster
  aus Atlas-Icons (Block-Atlas, Vanilla und Mods); Klick setzt den Wert der Variable (ein Undo-Schritt). Keine Uploads.
**S4 Speichern/Export**
- `editor.ModelExport` (rein, java.nio): Name prüfen (`[a-z0-9_]{1,20}`), Namensraum `mymodels`; Dateien
  `pack.mcmeta`, `assets/mymodels/models/item/<n>.json`, `assets/mymodels/items/<n>.json`; Katalog-Eintrag
  (`ModelDefinition`-Format); Schreiben nur unter einem festen Wurzelordner: Pfad normalisiert + `startsWith(root)`,
  keine Symlinks auf dem Weg, kein `..`/Trenner im Namen; Zip-Export; Arbeitsstand `config/simplemodels/workspace/<n>.json`
  speichern/auflisten/laden (Größengrenze, `ModelCatalogue.read`).
- Client `ModelFileScreen` (Werkzeugleiste „Datei“ / Strg+S): Name, Basis-Item (Registry-Prüfung), Speichern,
  „Als Modell nutzen“ (Pack `resourcepacks/simplemodels-editor` + Katalog `config/simplemodels/catalog/` + Pack an +
  Neuladen; im Einzelspieler zusätzlich Katalog-Reload auf dem integrierten Server), „Zip“ nach `simplemodels/exports/`,
  Liste gespeicherter Arbeitsstände zum Öffnen.

## Tests / Verifikation
- Gametests (Katalog `ModelTests.ALL` + Fabric-Adapter): `editor_snap_help` (Raster-Paare, Kürzel-Lang, locate,
  writeCompact-Breite), `editor_uv` (UvEdit, hitFace, State-Fläche), `editor_textures` (TextureChoice),
  `editor_export` (Pfad-Sicherheit: `..`, `/`, Symlink-Wurzel, Symlink-Unterordner; Dateien lesen sich zurück, Katalog-
  Eintrag parst über `ModelDefinition.parse`, Zip-Inhalt, Arbeitsstand rund).
- Client-Smoke simplemodels: Screenshots Fläche/UV, Texturwahl, Hilfe, Datei-Dialog (xvfb).
- Lokal vorher: `precheck.py --fast`, `check_ui_contrast.py`, `modules/simplemodels/tools/check_data.py`; reine Logik
  per javac gegen Gson/JOML vorab.
- Ein sbt am Ende: Compile Kern + simplemodels, module-simplemodels fabric+standalone, Client-Smoke mit xvfb-run,
  Guides/Wiki/Discover neu (gen-Branch) + `wiki/generate.py --all --check`.

## Entscheidungen (selbst getroffen)
- Raster als gekoppelte Stufen (ein Knopf) statt zweier Knöpfe: Werkzeugleiste passt sonst bei 427 px Breite nicht.
- Namensraum `mymodels` für Exporte (kurz, nicht `minecraft`, kollidiert nicht mit Modulen); Anvil-Name
  `@model:mymodels:<name>` bleibt < 50 Zeichen.
- Display-Reiter (Hand/GUI/Boden) und Beispiele 7–8 bleiben offen (Konzept Stufe 5/6 Rest), nicht in N7 genannt.
- Mehrspieler: Export wirkt nur lokal (Pack) und im Einzelspieler-Katalog; auf fremden Servern Hinweis, kein Upload.
