# Plan b-texrate15: Prinzip A8d + Textur-Bewertung + Ueberarbeitung der roten (09.11., N13 c+d)

Auftrag /root/b-auftrag-20261109.md N13 c) + d). Basis hub/b-bundle165 + hub/b-fix165. Antwortcode TEXRATE15.

## Ist-Stand
- RIFT14 (b-rift14, docs/ai/PLAN-B-RIFT14-2026-11-09.md): Vanilla-Analyse je Textur mit Messung (groesste einfarbige
  Flaeche, Anteil in Flaechen >= 6 px, Hauptton, hart) in /root/sb-tools-b/rift14/scr/measure.py und als Funktionen in
  tools/textures/rift14_2026_11_09.py (`regions`, `metrics`, `overlay`). Nur 16x16, ohne Transparenz, kein Ampel-Urteil.
- A8b (hart ~7 %, ueber ~12 % zu hart) und A8c (Runden) stehen in docs/ai/PRINZIPIEN.md, AGENTS.md verweist auf A8c.
- generate_textures.py --check ist auf der Basis schon rot (fehlende Handvorlage tools/textures/hand/enderite_carrot.png)
  und laeuft in keinem Gate; Gate fuer Texturen ist `layers.py check` (Pixelgleichheit PNG <-> .layers.json).

## c) Prinzip + Werkzeug
1. PRINZIPIEN.md A8d direkt nach A8c: Vanilla-Analyse je Textur (passende Vanilla-Vorbilder; flache Stellen: groesste
   einfarbige Flaeche, Anteil in Flaechen >= 6 px; Kanten-Highlights oben/links, Innenschatten unten/rechts; 2-4-px-
   Pixelgruppen; Tonwechsel alle 2-3 px auf langen Linien), Messwerte als Richtwerte, Ampel-Regel, Werkzeug.
2. AGENTS.md Textur-Abschnitt: Satz zu A8d neben dem A8c-Satz.
3. tools/textures/texrate.py: Messung aus rift14 extrahiert und verallgemeinert (beliebige Groesse, erstes
   Animationsbild, nur deckende Pixel, Groessen auf 16x16 normiert), Ampel-Regel `rate()`, `--vanilla` misst die
   Vanilla-Texturen aus dem Client-Jar zur Laufzeit (nichts eingecheckt), `--selftest`.

## d) Bewertung
- tools/textures/texrate15_2026_11_09.py: alle getrackten Block-/Item-Texturen fuer 26.3 (Kern src/main + Overlay,
  modules/*/shared; ohne mc1_21_11-Kopien und Wiki). Prioritaet: 1 Simple End (Modul + End-Texturen im Kern), 2 Simple
  Ores, 3 Simple Blocks + Kern-Baubloecke, 4 weitere Module, 5 Kern sonstige. Vanilla-abgeleitete sind git-ignoriert
  und damit nicht dabei. Toenungsmasken/Animationsteile (Redstone-Linien, *_dyed, *_overlay, Detektor-Nadeln,
  Schnitz-Masken carved/*, UV-Atlanten placed_*) = grau (nicht bewertet). Gesperrt markiert: chiseled_* (b-pal15),
  simplefun (b-fun14).
- Dokument docs/ai/TEXTUR-BEWERTUNG-2026-11-09.md (Regel, Vanilla-Kalibrierung, Blick-Befunde, Tabelle rot/orange),
  Vorschau /root/sb-tools-b/previews/b-texrate15-preview.html (Thumbnail 8x mit Flaechen-Markierung, Filter Familie/
  Ampel, Sortierung per Spaltenkopf). Stichproben je Familie per Read ansehen.

## Ueberarbeitung (A8c-Runden, A8d-Methode)
Auswahl rot + hohe Sichtbarkeit + nicht gesperrt + keine laufende Abnahme ausser ausdruecklich genannt:
1. Simple Ores Metall-Baufamilie (cut_*, *_tiles, *_pillar, *_pillar_top; 6 Metalle). METALS14 ist noch nicht
   beantwortet, aber N13 d nennt die Metalle ausdruecklich -> ueberarbeiten, in der Vorschau als "ersetzt METALS14-R6"
   kennzeichnen.
2. End-Paletten im Kern (polished_*, *_pillar, *_pillar_top, *_block von Astralit/Nihilith/Enderquarz) und die
   Abwandlungen crystalline/veined/frosted: aeltere, abgenommene Texturen, aber eindeutig rot (70-80 % der Flaeche in
   einfarbigen Flaechen >= 6 px, Vanilla-Vorbilder polished_* 0-50 %) -> ueberarbeiten.
3. Soweit Zeit: orange derselben Familien.
Methode: Nachbearbeitung der vorhandenen PNG (Form, Palette und Motiv bleiben), Werkzeug texrate15-Runden:
Flaechen >= 6 px mit 2-4-px-Haufen eines Nachbartons der eigenen Rampe brechen (Ton-Schritt <= 40 Helligkeit, damit
hart nicht steigt), Drift oben/links heller, unten/rechts dunkler, lange Kantenlinien mit Tonwechsel alle 2-3 px.
Runden R1-R3 je Familie mit Messung + Blick (1x und 8x neben Vanilla), Bewertung hier und in der Vorschau.
.layers.json: vorhandene Ebenen bleiben, neue Ebene "A8d Flaechen-Brecher" mit den geaenderten Pixeln.
Bekannte Folge: generate_textures.py/metals14-Generator wuerden die alten Pixel wieder schreiben -> Hinweis im
Generator-Kopf; polierte End-Schachbretter (template polished_*) bleiben unveraendert.

## Verifikation
`python3 tools/textures/texrate.py --selftest`, `python3 tools/textures/layers.py check`; kein sbt (nur gleichnamige
PNG/.layers.json, Python-Werkzeuge, Doku). Commits je Mod-Paket. Push nur /root/sb.git b-texrate15.

## Risiken
- Ampel nur Zahlen: Regel an RIFT14-Entscheidung kalibriert (R6 orange, R9 gruen) und gegen Vanilla gemessen;
  Blick-Befund je Familie im Dokument.
- Unruhe statt Flaeche (Kruemel): nur Haufen 2-4 px, keine Einzelpixel-Streuung (RIFT14-R7-Lehre).
