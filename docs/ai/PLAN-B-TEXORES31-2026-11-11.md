# PLAN B-TEXORES31 (11.11.2026) – Simple Ores: 16 rote Texturen (RELSB31 L4, Punkt #6)

Branch `b-texores31`, Basis `hub/b-bundle182` 9b5764995 (neuestes Bündel; enthält `b-bundle181-gen` 6e6faa936 –
ein `b-bundle182-gen` gab es noch nicht, `b-bundle181-gen` ist im Hub nicht mehr vorhanden).
Vorschau `/root/sb-tools-b/previews/b-texores31-preview.html`, Antwortcode **TEXORES31** (keine Wahl nötig).

## Ist-Stand
`python3 tools/textures/texrate.py` auf `modules/simpleores` → genau 16 rot:

| Gruppe | Texturen | texrate-Gründe | Abnahme |
|---|---|---|---|
| Gitter | nickel/brass/stainless_steel/bronze/lead_grate | große Fläche, Flächen ≥6, Hauptton | **METALS25** (Besitzer 10.11. 18:40): A, `brass_grate` = B; eingebaut a224c612d (b-metals25b), `metals25_2026_11_10.py --check` grün |
| Stäbe | brass/bronze/stainless_steel/lead/silver/nickel/zinc_bars | Farben 3, Hauptton, Flächen ≥6 | **METALS25**: A, `lead_bars` = B (wie oben) |
| Dim.-Schrott | dimensional_scrap_side/_top, end_dimensional_scrap_side/_top | hart 31–34 %, Fläche 20 | **MISCTEX23** (W23-4, 23.10.: Animation A „atmen“, Ruhebild = abgenommene Textur; Blobs identisch zum Stand a9c0af0c5) + **TEXRATE17 „alle ok“** (10.11. ~09:10; Vorschau listete sie als Ausschluss „leuchtendes Motiv“) |

## Entscheidung
Alle 16 sind **rot, aber abgenommen** → nach Auftrag, Memory `texturen-nicht-vermischen` und `abnahme-nicht-doppelt-fragen`
**keine Änderung**, keine Varianten A/B (wäre erneutes Vorlegen entschiedener Wahlen). Release-Frage dazu ist **f6**
(rot mit Begründung akzeptieren?). Die A8c-Runden entfallen mangels nicht abgenommener Ziele (Abweichung vom Auftrag, bewusst).

Begründung (Härte-Sonderregel) für f6:
- Stäbe/Gitter: Cutout-Motiv mit wenigen Metalltönen wie Vanilla `iron_bars` (texrate grün, aber hart 30,2 %, 5 Farben);
  `copper_grate` 8 Farben/hart 2,7 %. „Farben 3“/„Hauptton“ ist beim dünnen Stab strukturell; Härte ist bei unseren Stäben
  16–42 % (Vanilla-Stäbe selbst hart).
- Schrott: nach `ancient_debris`; Vanilla `ancient_debris_top` ist selbst rot (hart 39,4 %), `_side` orange.

## Prüfungen (lokal, kein sbt – Regel 17)
- `tools/textures/layers.py check` RC=0 (3189 Quellen, 0 Probleme)
- `texrate.py --selftest` RC=0
- `tools/check_missing_textures.py` RC=0 (0 Fehler)
- `metals25_2026_11_10.py --check` RC=0 (19 Texturen)
- `modules/simpleores/tools/check_data.py` RC=0

## Nebenbefund (offen, nicht Teil dieses Punkts)
Die Schrott-Generatoren sind seit dem Umzug nach Simple Ores (ig3113) veraltet: `misctex23b_2026_10_23.py --check`
bricht ab (sucht `mc26_3/overlay/.../simplebuilding/textures/block/dimensional_scrap_side.png`),
`dimensional_scrap_textures.py` braucht einen Vanilla-Ordner und schreibt den Stand vor MISCTEX23. Ein Neulauf
würde die abgenommenen Pixel also nicht reproduzieren → Pfade auf `modules/simpleores/...` umstellen (eigenes kleines Paket).
