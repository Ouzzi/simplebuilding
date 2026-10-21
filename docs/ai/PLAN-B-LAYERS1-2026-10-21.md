# Plan b-layers1 (21.10.2026)

Queue „(B, b-layers1, 21.10., Teil 1 …)“, Prinzip A8 (b) in `docs/ai/PRINZIPIEN.md`: jede eigene Textur hat neben dem
PNG eine Ebenen-Quelle `<name>.layers.json` (Format `docs/ai/PLAN-B-PIXELEDITOR-2026-10-21.md`), das PNG ist ihre
Zusammensetzung. Teil 1 = alle eingecheckten eigenen Texturen; **kein Pixel ändert sich** (`git diff --stat -- '*.png'` leer).

## Ist-Zustand

- 2846 eingecheckte PNGs unter `*/textures/**` (ohne `wiki/`). Davon außerhalb des Auftrags: `mc1_21_11/` (690, vollständige
  Port-Kopie, „wird nur im Port-Run angefasst“, AGENTS.md) und `tools/textures/**` (70, Vorlagen/Vorschauen, keine
  Spiel-Texturen). **Im Umfang: 2086** (src/main 736, mc26_3/overlay 525, modules/* 825), davon 1718 × 16×16,
  größte 256×512 (Handbuch). Rund 2 Mio. Pixel → etwa 5 MB Ebenen-JSON.
- Git-ignorierte Pfade (Vanilla-abgeleitet, `tools/textures/vanilla_derived.py`) sind nicht eingecheckt und bleiben ohne Quelle.
- Vorhanden: `tools/textures/layers.py` (import/compose/check, reines Python) und `checkTextureLayers` in `build.gradle`;
  bisher nur Mod-Icons + Pilot als Ebenen-Quelle.
- Generatoren: ~150 Skripte in `tools/textures/` (meist datierte Einmal-Runden/Vorschläge, deren Ausgabe später von
  anderen Runden überschrieben wurde) und `modules/*/tools/gen_*.py`. Herkunft je Textur ist nur durch Ausführen sicher
  feststellbar.

## Vorgehen

1. **Herkunft messen** (sb-test, Wegwerf-Spiegel): jeden Generator einzeln ab Branch-Stand laufen lassen und
   festhalten, welche eingecheckten PNGs er **byte-identisch** neu schreibt (`same`), verändert (`changed` = veraltet)
   oder neu anlegt. Textur „vom Generator X“ = X schreibt sie identisch. Ergebnis als Inventar unten.
2. **Gemeinsamer Helfer** in `layers.py`:
   - `from_steps(w, h, [(Name, RGBA nach dem Schritt), …])`: Ebene *i* = Pixel, die Schritt *i* verändert hat. Damit
     die Zusammensetzung exakt bleibt, steht ein halbdurchsichtiger oder gelöschter Endpixel nur in der obersten Ebene,
     die ihn gesetzt hat; deckende Pixel bleiben auch in den unteren Ebenen (Grundform bleibt vollständig).
     Selbstprüfung `compose == Endbild`, sonst Ein-Ebenen-Rückfall.
   - `write_beside(png_path, doc)` / `save_steps(png_path, w, h, steps)`: schreibt `<name>.layers.json`.
   - `sync` (neu): legt für jede eingecheckte eigene Textur ohne Quelle eine Ein-Ebenen-Quelle „Grundform“ an und
     erneuert **Ein-Ebenen**-Quellen, deren PNG sich geändert hat (Generator lief, Ebenen fehlen) – mehrlagige Quellen
     werden nie überschrieben, nur gemeldet. So bleibt jeder Generator ohne Ebenen-Ausgabe weiter nutzbar.
3. **Generatoren mit Zeichenschritten**: die aktuellen Generatoren, die viele Texturen identisch erzeugen
   (Kandidaten nach Messung: `tools/textures/generate_textures.py`, `modules/*/tools/gen_textures.py`,
   `modules/*/tools/gen_resources.py`), schreiben über `from_steps` benannte Ebenen (z. B. Grundform aus der
   Pixelkarte, danach Griffe/Glimmer/Glanz/Kontrast-Nachbearbeitung). Veraltete Generatoren (schreiben nicht mehr
   identisch) werden nicht umgebaut → Schritt 4.
4. **Rest**: `layers.py sync` = Ein-Ebenen-Import „Grundform“ (kein Pixel ändert sich).
5. **Prüfung** `check`: zusätzlich Abdeckung – jede eingecheckte, nicht ignorierte Textur unter `src/main`,
   `mc26_3/overlay`, `modules/*` (`…/textures/**/*.png`) braucht eine Quelle; begründete Ausnahmen in `EXEMPT`
   (layers.py). Animationsstreifen (`.png.mcmeta`) brauchen keine Ausnahme: die Quelle deckt den ganzen Streifen ab
   (Format kennt beliebige Höhe); Ebenen je Frame entfallen. `git check-ignore` gebündelt (`--stdin`) statt je Datei.
   Fehlermeldung nennt `layers.py sync`.
6. **Vanilla-Nähe** (keine Pixeländerung): Heuristik über alle Texturen im Umfang – Farbanzahl, Anteil Halbtransparenz,
   Kontur (reines Schwarz / fehlende Kontur bei Items), Sättigung, Helligkeitsstufen, Rauschen, Licht oben-links –
   plus Stichprobe; Liste `docs/ai/AUDIT-VANILLA-NAEHE-2026-10-21.md` für Teil 2 (Besitzer entscheidet).

## Dateien

`tools/textures/layers.py`, `tools/textures/tests/test_layers.py`, die angepassten Generatoren, ~2086 neue
`*.layers.json` (Commits je Modul/Baum), `wiki/` (neu erzeugt: Kopien + `textures.js`), Plan/Audit, `.claude/QUEUE.md`
(eigene Zeile). Nicht: `wiki/assets/pixeleditor*` (b-pixeleditor2), `mc1_21_11/`.

## Verifikation

- Unit-Tests `test_layers.py` (from_steps: deckend/halbdurchsichtig/gelöscht exakt; sync; Abdeckung + Ausnahme).
- sb-test: Generatoren erneut laufen lassen → `git diff --stat -- '*.png'` leer; `layers.py check` 0 Probleme;
  `module_guides.py`, `wiki/generate.py --all`, `discover.py`; `./gradlew check -q` grün; Compile (keine Java-Änderung,
  trotzdem einmal Fabric 26.3).

## Risiken

- Parallele Agenten ändern PNGs → ihre Quelle veraltet → `check` rot. Abhilfe: `layers.py sync` (in der Meldung);
  Hinweis in AGENTS.md (Texturen-Regel).
- Repo-/Jar-Größe +5 MB Text; Wiki-Kopien noch einmal so viel.
- Laufzeit `check`: reines Python über 2 Mio. Pixel (Ziel < 60 s).

## Ergebnis Inventar (Messung sb-test, 211 Skripte je einzeln ab Branch-Stand)

- **2086 eigene Texturen im Umfang.** 1306 davon schreibt mindestens ein Generator pixelgleich neu (553 davon mehrere
  Skripte, weil Runden-Skripte frühere Generatoren importieren), 780 haben keinen laufenden Generator (handgemalt bzw.
  Besitzer-Vorlagen, z. B. `tools/textures/hand/`).
- 14 datierte Skripte schreiben **abweichende** Pixel (veraltet: u. a. `copper_barrels_2026_10_05` 10,
  `round5_settled_2026_10_02` 9, `cores_*` 6, `crucibles_2026_10_04` 6, `end_system_textures` 6) → nicht umgebaut, ihre
  Texturen bekamen die Ein-Ebenen-Quelle.
- `generate_textures.py` erzeugt seine 516 Texturen pixelgleich, aber nicht byte-gleich (Pillow-Kodierung auf sb-test);
  PNGs bleiben deshalb die eingecheckten Bytes, die Quellen vergleichen Pixel.

## Umsetzung (Abweichungen vom Plan benannt)

- **Mit Zeichenschritt-Ebenen** (Generator schreibt `.layers.json` selbst): `modules/simpleend` (Rinde/Astknoten,
  Bretter/Rahmen/Fenster, Blätter/Pollen, Endstein/Gänge/Krümel …), `modules/simpleenddragonfight` (Körper/Gesicht/
  Stacheln/Flügel; Items nach Material), `modules/simplemaps` (Papier/Motiv/Falz/Kontur/Kompass), `modules/simplemusic`
  (seine vorhandenen benannten Ebenen), `modules/simplequestionary` + `modules/simpletools` (nach Material),
  `tools/textures/generate_textures.py` (Grundform aus der Pixelkarte, Glimmer, Griff, Grundplatte/Schleier/Sterne der
  Pads; sonst eine Ebene), `tools/textures/generate_trim_overlays.py` (Muster + dunkle Kontur 6/7).
- **Abweichung:** weitere pixelgleich laufende, aber datierte Einmal-Skripte (`light_panes_2026_10_14` 56,
  `copper_light_panes_2026_10_16` 52, `pad_fade` 34, `chess_2026_10_06` 26, `module_guide_books_2026_10_05` 26 …) schreiben
  keine Ebenen; ihre Texturen haben die Ein-Ebenen-Quelle, `layers.py sync` hält sie nach einem Lauf aktuell.
  `gen_resources.py`/`generate_heads.py` der Module schreiben keine PNGs (die Messung zeigte zunächst Fehltreffer).
- Ergebnis: 2130 Quellen (2086 Texturen + Icons), 216 mehrlagig, 1914 einlagig; `git diff --stat -- '*.png'` leer;
  rund 2 MB JSON.
