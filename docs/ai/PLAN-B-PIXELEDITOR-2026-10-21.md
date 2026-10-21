# Plan b-pixeleditor (21.10.2026)

Queue „(B, b-pixeleditor, 21.10.)“, Prinzip A8 (b) in `docs/ai/PRINZIPIEN.md`. Analyse: `docs/ai/ANALYSE-RESPRITE-2026-10-21.md`.

## Ist-Zustand

- Wiki = handgeschriebene `wiki/index.html` (kein Build), Daten aus `wiki/generate.py`; Texturliste aus
  `wiki/textures.py` → `wiki/data/textures.js`, Kopien unter `wiki/assets/textures/sheets/`.
- Dev-Modus = Wiki-Spiegel der Zentrale (`/wiki/`), nginx hängt `tools/zentrale-web/dev-overlay.js` an; die Wiki ruft
  `devHook('textures'|'texture-zoom'|…)` → `window.sbDevHook`. Das öffentliche Wiki lädt kein Overlay.
- Keine Ebenen-Quellen, kein Editor. Pillow gibt es auf sb-test, nicht auf sb-agent; `check` läuft mit reinem Python.

## Ebenen-Format (verbindlich für spätere Textur-Agenten)

**Ort: neben dem PNG**, `<name>.layers.json` (aus `foo.png` wird `foo.layers.json`). Begründung: wandert bei
Umbenennen/Verschieben mit, gilt in jedem Modul ohne zentrale Zuordnungstabelle, A8 (b) sagt „neben dem PNG“.
Minecraft liest in `textures/` nur `.png`/`.png.mcmeta`; die Datei stört im Spiel nicht (wenige KB im Jar).

```json
{
  "format": "sb-layers", "version": 1,
  "width": 16, "height": 16,
  "digits": 2,
  "palette": ["#3b2a1fff", "#c6c6c6ff"],
  "layers": [
    {"name": "Grundform", "visible": true, "opacity": 100, "blend": "normal",
     "rows": ["....0000....", "..0011110..."]}
  ]
}
```

- `palette`: `#rrggbbaa`, Index ab 0. `digits`: Zeichen je Pixel (2 bis 256 Farben, 3 bis 4096, 4 darüber),
  Index hexadezimal klein, `.` × digits = durchsichtig. Eine Zeile je Pixelreihe (gute Git-Diffs).
- `layers[0]` ist die **unterste** Ebene. `opacity` 0–100 (ganze Zahl). `blend` nur `normal`.
- Zusammensetzung (Python und JS identisch): von unten nach oben „source-over“ mit
  `a = round(alpha · opacity / 100)`, Rundung immer `floor(x + 0.5)`; volle Deckung ersetzt den Pixel.
  Unsichtbare Ebenen zählen nicht. Das PNG im Repo **ist** diese Zusammensetzung.
- Ausgeschlossen: Vanilla-abgeleitete Texturen (A8, git-ignoriert) – dort keine Ebenen-Quelle; die Prüfung meldet es.

## Werkzeug `tools/textures/layers.py` (reines Python, zlib)

- `compose <layers.json> [--out PNG]` – PNG aus Ebenen schreiben (Standard: das PNG daneben).
- `import <png> [--name Grundform]` – PNG als Ein-Ebenen-Quelle anlegen (Palette aus dem Bild).
- `check [paths…]` – alle eingecheckten `*.layers.json` (git ls-files): Format gültig, PNG daneben vorhanden,
  nicht git-ignoriert, Pixel des PNG == Zusammensetzung (RGBA-Vergleich, nicht Bytes). In `check` eingehängt als
  Gradle-Task `checkTextureLayers` (Liste der Text-Prüfungen in `build.gradle`).
- PNG lesen: Farbtypen 0/2/3/4/6, Bittiefe 1–8 (16 → obere 8 Bit), kein Interlace.

## Wiki

- `wiki/textures.py`: liegt `<name>.layers.json` neben der Quelle, wird sie nach `assets/textures/sheets/<module>/<ns>/<path>.layers.json`
  kopiert, Eintrag bekommt `"layers": <Wiki-Pfad>`. Gleiche Aufräum-/Prüflogik wie die PNG-Kopien.
- `wiki/assets/pixeleditor-core.js`: reine Funktionen (UMD, auch unter node): Format lesen/schreiben, Zusammensetzen,
  PNG lesen/schreiben (eigener Codec, damit halbdurchsichtige Pixel exakt bleiben – Canvas wäre vormultipliziert),
  Füllen, Linie, Rechteck, Symmetrie, Pixel-Perfect, Palette, Verlauf (Rückgängig).
- `wiki/assets/pixeleditor.js`: Oberfläche (`window.SBPixelEditor.open({id, name, png, layers})`), Werkzeuge laut
  Analyse, Ebenen-Paneel, Vorschau 1:1/Kachel, Referenzbild, Export (PNG + JSON + GPL herunterladen). Hell/dunkel,
  Kontrast ≥ 4,5:1 (A7), Touch + Tastenkürzel.
- `tools/zentrale-web/dev-overlay.js`: Knopf „Bearbeiten“ in der Textur-Großansicht und ✎ an jeder Textur-Kachel
  (nur Dev-Spiegel), lädt die beiden Skripte bei Bedarf relativ zur Wiki-Seite (`assets/…`).
- Pilot: `simplebuilding:item/iron_chisel` als Ein-Ebenen-Quelle (Besitzer-Textur, nicht Vanilla-abgeleitet).
  Bestandsüberführung bleibt Queue-Punkt „Bestehende Texturen schrittweise in Ebenen-Quellen überführen“.

## Ablauf für Devs (auch in `wiki/README.md`)

1. Wiki-Spiegel der Zentrale → Texturen → ✎/„Bearbeiten“. 2. Bearbeiten, „Export“ lädt `<name>.png` und
`<name>.layers.json`. 3. Beide neben das Original im Repo legen (gleicher Ordner). 4. `python tools/textures/layers.py check`
und `python wiki/generate.py --all`, committen.

## Tests

- `tools/textures/tests/test_layers.py`: Zusammensetzen, Deckkraft, unsichtbare Ebene, Format-Rundlauf, Import →
  check grün, verändertes PNG → check rot, PNG-Farbtypen.
- `wiki/tests/test_pixeleditor.py`: Overlay hat den Knopf + lädt die Skripte, textures.py setzt `layers`;
  führt `wiki/tests/pixeleditor_core.cjs` mit node aus (übersprungen ohne node): JS-Zusammensetzung == Python-Ergebnis
  für dieselbe Datei, PNG-Rundlauf, Füllen/Linie/Symmetrie/Verlauf.
- Gegenprobe auf sb-test: Python-PNG-Decoder gegen Pillow über alle eingecheckten Texturen.
- sb-test: Wiki erzeugen (-gen), `./gradlew check -q`. Browser-Klick-Test nicht möglich (kein Browser für die
  Zentrale vom CT) → ehrlich offen; Demo-HTML im Scratchpad für die Abnahme durch die Hauptsitzung.

## Risiken

- Halbdurchsichtige Pixel: eigener PNG-Codec im Browser (CompressionStream; Fallback: unkomprimierte Deflate-Blöcke).
- Große GUI-Blätter (256×256+): Editor zeichnet per ImageData, Zoom begrenzt; Funktionsfähig, aber nicht optimiert.
- Gemeinsame Dateien: `build.gradle` (eine Zeile in der Prüfliste), `.claude/QUEUE.md` (nur eigene Zeile).
