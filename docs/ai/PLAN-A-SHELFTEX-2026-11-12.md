# PLAN a-shelftex – Simple-End-Bücherregale ohne Vanilla-Pixel (12.11.2026)

Auftrag: /root/sb-tools-a/agenten-1530.md §16 (Fund a-nihilplanks). Basis hub/b-bundle190 (9d6e0d313), Ziel Bündel 191. Nur 26.3 Fabric.
Regeln: A8 (keine Mojang-Pixel im Repo, auch keine Wiki-Kopien), A8c/A8d, TEXTUR-REGEL §15 (Vorschau vorher/nachher je Textur,
ok/Einwand, Antwortcode SHELFTEX).

## Ist-Stand (erhoben)
- `simpleend:block/{ashwood,duskwood}_bookshelf.png` (b-shelves01, `tools/textures/shelves01_2026_11_04.py --write`, eingecheckt mit
  `.layers.json`, Wiki-Kopien `wiki/assets/textures/simpleend/block/*_bookshelf.png` und Sheet-Ebenen
  `wiki/assets/textures/sheets/simpleend/simpleend/block/*_bookshelf.layers.json`).
- `end_bookshelf()` beginnt mit `planks.copy()`: das End-Brett ist seit a-mojang08 ein Rezept (`recolour08`: Vanillas
  `block/birch_planks`, Palette getauscht, Build-Zeit). Rahmen = Zeilen 0, 7, 8, 15 und Spalten 0, 15 = **88/256 Pixel**, 1:1 das
  umgefärbte Birkenbrett (Messung unten: exakt 88 gegen die Build-Ausgabe `ashwood_planks`, 25 gleiche 8-px-Läufe). Die übrigen
  168 Pixel (Rückwand, Buchrücken, Schatten) sind eigene Pixel (Farben aus den eigenen Rezept-Zielfarben + `SPINES`).
- `shelves01 --check`: eingecheckte PNGs = Generator-Ausgabe (keine texrate16/17-Nacharbeit; texrate17: orange „hart 22,1,
  Farben 59“, Runden ohne Ampel-Gewinn mit Motiv-Schutz).
- Offene Besitzer-Frage a-nihilplanks F4: „Regal-Optik A = Regale bleiben wie sie sind (gebaut)“ – Optik soll also nicht
  ungefragt wechseln.
- Modell: `cube_column` mit dem Brett als Ende (`gen_data.bookshelves`), d. h. Ober-/Unterseite zeigen das End-Brett – wie Vanilla
  (Eichenregal: Rahmen = Eichenbrett-Zeilen, Ende = Eichenbrett).

## Entscheidung (begründet)
**Build-Zeit-Ableitung wie a-nihilplanks/a-mojang08** statt eigener Rahmenpixel:
1. Der Rahmen eines Vanilla-Regals *ist* das Brett des Holzes. Das End-Brett ist (abgenommen, a-abn29t/a-mojang08) eine Umfärbung
   von Vanillas Birkenbrett und entsteht beim Build – ein eigener Rahmen würde von den Brettern daneben und vom eigenen
   Regal-Deckel (cube_column-Ende = Brett) abweichen.
2. Pixelgleich zum Stand vorher: keine Optik-Änderung, F4 bleibt offen beantwortbar, keine neue Abnahme der Gestaltung nötig.
3. Keine neue Abhängigkeit: Simple End baut Bretter, gemeißelte Regale, Boote und Nihilit-Bretter schon aus dem Client-Jar
   (`MODULE_OUTPUTS['simpleend']`, Platzhalter-Kette, `-PsbVanillaDerivedStrict`).
Verworfen: eigene Rahmenpixel aus den „eigenen“ Brettern – die gibt es nicht (Bretter = Rezept); ein nachgezeichneter Rahmen wäre
entweder wieder das Birkenbrett-Muster oder sähe neben dem Brett-Deckel fremd aus (und bräuchte A8c-Runden + neue Abnahme).

## Umsetzung
- `shelves01_2026_11_04.py`: `end_bookshelf(wood, jar)` und `end_ramp(wood, jar)` lesen das Brett aus dem übergebenen Jar
  (`recolour08.derived_module`), `derived_end()` liefert zusätzlich `block/{wood}_bookshelf.png`; `--write/--check` der
  eingecheckten Regale entfallen (nichts Eigenes mehr eingecheckt).
- `vanilla_derived.py`: `MODULE_OUTPUTS['simpleend']` + 2 Einträge.
- Gelöscht (git rm): 2 PNG + 2 `.layers.json` im Modul, 2 Wiki-PNG-Kopien, 2 Sheet-`.layers.json`.
- `.gitignore`: Modulpfad + Wiki-Kopie + beide Sheet-Kopien je Holz (wie die gemeißelten Regale).
- Herkunfts-Prüfung als Werkzeug: `tools/textures/shelftex12_2026_11_12.py --scan --module simpleend` (nur Zählung, schreibt nichts
  ins Repo): exakt / Farbtausch jenseits Zufall (ganz, Hälften, Viertel; 8 Lagen) / Alpha-Maske / 8-px-Läufe an beliebiger Stelle
  gegen alle Jar-Texturen und die abgeleiteten Modul-Ausgaben. Gegenproben: Farbtausch Tür/Apfel/Endstein (Helligkeitsrang) = 1,00,
  halbes umgefärbtes Birkenbrett → „ansehen“ (Teil 1,00), Regal → Fund (88 exakt, 25 Läufe).

## Prüfung
Lokal: Regal aus dem Jar pixelgleich zum alten PNG (beide Hölzer), `vanilla_derived.py --module simpleend` (22 Texturen),
`layers.py check`, `check_missing_textures`, `modules/simpleend/tools/check_data.py` (+ alle Module), `wiki/generate.py --all --check`,
`precheck.py --fast`, Scan danach ohne Pixel-Fund. Java unverändert → kein sbt-Lauf nötig.

## Ergebnis
(folgt)
