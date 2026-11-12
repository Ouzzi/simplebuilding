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
### Regale
- Build-Ausgabe aus dem Jar = altes PNG, **0 Pixel verschieden** (Esche und Dämmer); texrate unverändert orange (hart 22,1 / 20,8 %,
  Farben 59/52; Vanilla-Eichenregal orange 27,9 %, gemeißelt rot 37,9 %) – keine neue Gestaltung, darum keine A8c-Runden (Abweichung
  vom Auftragstext „A8c-Runden“: es wurde nichts neu gezeichnet).
- `vanilla_derived.py --module simpleend`: 22 Texturen aus dem Jar (vorher 20).

### Herkunfts-Scan Simple End (`shelftex12 --scan --module simpleend`)
- **Vorher** (b-bundle190, 659 PNGs; erste Fassung der Regeln): Fund = die 2 Regale (je 88 px exakt = End-Brett, 25 gleiche
  8-px-Läufe). Weitere Treffer erwiesen sich als Fehlalarm und führten zu schärferen Regeln: Dämmerholz-Schild/-Hängeschild-Item
  (24/25 gleiche Pixel – im Code mit denselben Brett-Tönen gezeichnet; mit Zufallsabzug 12/7, ≤ 1 Lauf), 81 Gemälde (Teil-Farbtausch
  gegen zweitönige Banner-Muster/Rahmen-Ecken).
- **Nachher** (dieser Branch, 657 PNGs, Lauf 1224 s): **0 Funde mit Mojang-Pixeln**, 11 „Form ansehen“ (Alpha-Umriss = Vanilla):
  - `block/{ashwood,duskwood}_sign`, `block/{ashwood,duskwood}_hanging_sign` (Entity-UV-Netz) und
    `minecraft:gui/signs|hanging_signs/simpleend_{ashwood,duskwood}`: Umriss beim Generieren aus Vanillas Sprite gelesen
    (`modules/simpleend/tools/gen_textures.py vanilla_mask`), alle Farben eigen (`fill_wood`). Besitzer-Frage **F1** (A = lassen,
    Empfehlung; B = auch diese 8 zur Build-Zeit).
  - `item/duskwood_{sign,hanging_sign}`: im Code gezeichnet (b-music03 Variante B „Vanilla-Proportionen“, `ig3114.sign_item`),
    Umriss dadurch gleich Vanilla; keine Vanilla-Pixel.
  - `block/ashwood_trapdoor`: im Code gezeichnet, Lochbild ähnlich Eisenfalltür (Form 0,98).
- **Gemälde**: alle 598 PNGs pixelgleich zur Ausgabe von `modules/simpleend/tools/gen_paintings.py` (zeichnet nur aus Code, liest
  keine Datei) → Mojang-Pixel ausgeschlossen.
- Gegenproben (Scratch, nicht im Repo): Farbtausch Tür/Apfel, Helligkeitsrang-Endstein → Fund 1,00; halb umgefärbtes Birkenbrett →
  „ansehen“ (Teil 1,00); altes Regal → Fund.

### Prüfungen
Grün: `precheck.py --fast`, alle `modules/*/tools/check_data.py`, `check_dup_impl`, `check_tooltip_text`, `wiki/generate.py --all
--check` (nach `--all`: `wiki/data/textures.js` Regale als abgeleitet, `tools/landing/discover.html` Item-Zahl 3178→3176 – die
Zählung lag seit a-nihilplanks (SB-Lang-Einträge weg) im Bündel noch alt), `check_missing_textures` (0 Fehler), `layers.py check`
(0 Probleme), `tools/textures/tests` (67 OK). Java unverändert → kein sbt-Lauf. Nicht geprüft: Client-Sicht (Optik pixelgleich).
Gemeinsame Läufe auf sb-agent mit `nice`, Scan ~330 MB Spitze (Stapel je Größe, Hash-Index).

### Offen
- Besitzer: SHELFTEX (Regale ok/Einwand, F1) – Vorschau /root/sb-tools-a/previews/a-shelftex-preview.html.
- Bei F1 = B: die 8 Schild-Texturen wie hier auf Build-Zeit umstellen (Umriss aus dem Jar, eigene Farben als Rezept).
- Andere Module ohne diesen Teil-Scan (a-mojang08 prüfte nur ganze Bilder ≥ 0,90): `shelftex12 --scan --module <m>` je Modul
  laufen lassen (z. B. SB-Kern-Generatoren, die aus abgeleiteten Texturen zusammensetzen).
