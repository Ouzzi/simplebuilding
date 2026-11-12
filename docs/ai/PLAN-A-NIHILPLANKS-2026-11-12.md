# PLAN a-nihilplanks – Nihilit-Bretter nach Simple End (12.11.2026)

Auftrag: /root/sb-tools-a/agenten-1530.md §14c. Basis hub/b-bundle189 (6872a59f6), Ziel Bündel 190. Nur 26.3 Fabric.
Besitzer 12.11. ~21:00 (Screenshot Tab „Simple Enchanting“): „Bei Simple End fehlt folgender Block, und zwar die Ashwood- und
Duskwood-Planks mit dem Nihilit innen drin. Diese habe ich schon vorher requested und sind noch nicht da. Mit denen sollen auch die
Bookshelves gecraftet werden, genauso wie die Netherholz-Varianten.“ Vorgeschichte: a-end07 (06.11.), ASTRAL07=A.

## Ist-Stand (erhoben)
- `simplebuilding:nihilith_{ashwood,duskwood}_planks` liegen im **SB-Kern** (ModBlocks/ModItems, `McVersion.ASTRAL_ENCHANTING`),
  Rezept (Overlay, Bedingung simpleend) 4 Nihilitsplitter im Plus um 1 End-Brett = 2. Tab: SB-Zeile „blazewood“, die mit
  Simple Blocks im Tab **Simple Blocks** steht – weder im Tab Simple End noch Simple Enchanting. Darum „noch nicht da“.
- Textur: 16-Bild-Streifen, Ruhebild = echtes End-Brett, Fugen atmen in **Astralit-Rosa**, ±30 % – im Spiel kaum von normalen
  Brettern zu unterscheiden. Das End-Brett ist seit a-mojang08 eine Umfärbung von Vanillas Birkenbrett (Build-Zeit,
  `recolour08`); der eingecheckte Streifen enthält diese Pixel (≈84 % farbtreu) → Mojang-Pixel im Repo (Regel A8).
- Nether-Vorbild: `simplebuilding:{crimson,warped}_blazewood_planks` → `_bookshelf` (6 Bretter + 3 Lohenbücher) und
  `chiseled_…` (6 Bretter + 3 Nether-Stufen, WWW/SSS/WWW); Simple Enchanting registriert ohne SB Zwillinge; sein Tab zeigt sie,
  weil SBs eigene Tabs sie nicht führen (SimpleLib `CreativeTabPolicy` QW11: ein Item steht nur im Tab seines Mods, wenn der es hat).
- End-Regale gehören **Simple End** (`simpleend:{wood}_bookshelf`, `chiseled_{wood}_bookshelf`, Teil jedes End-Holzsets, auch ohne SB).
  Mit SB: Regal-Rezept `simplebuilding:{wood}_bookshelf` (6 Nihilit-Bretter + 3 Nihilit-Bücher), Simple Ends eigenes (Stufen + Bücher)
  aus. Gemeißelt: immer Simple Ends Rezept aus normalen Brettern (SSS/PPP/SSS) – nicht wie bei Lohenholz.
  Simple Enchanting zählt die End-Regale über `simpleenchanting:end_shelves`; seine `PARTNER_ITEMS` listen sie, QW11 entfernt sie dort.
- Simple End: SimpleLib nur auf Fabric (NeoForge/Forge ohne), Daten aus `modules/simpleend/tools/gen_data.py` (deterministisch,
  geprüft; nur Lang-Reihenfolge würde umsortiert → Lang von Hand).

## Entscheidungen
1. **Bretter nach Simple End**: `simpleend:nihilith_{ashwood,duskwood}_planks` (EndWood, Feld `nihilPlanks`), registriert **nur mit
   SimpleBuilding** (Besitzer des Nihilits; Prinzip Modul-Unabhängigkeit Regel 5 „höhere Stufen nur mit Partner, ohne ihn gar nicht
   registriert“, Vorbild Hufeisen). Schalter `SimpleEnd.setSimpleBuilding(..)` aus dem Loader-Einstieg (Fabric/NeoForge/Forge,
   je 1 Zeile). Eigenschaften wie End-Bretter (Holz, Axt, brennbar 5/20, Brennstoff wie Bretter).
2. **IdMoves** (Fabric, SimpleLib): `simplebuilding:nihilith_W_planks` → `simpleend:nihilith_W_planks` (Block, Item). NeoForge/Forge
   haben in Simple End kein SimpleLib → TODO beim Port.
3. **Rezepte bleiben SB-Rezepte mit Ladebedingung simpleend** (Ids unverändert, Rezeptbuch/Fortschritte bleiben gültig):
   Bretter-Rezept liefert jetzt `simpleend:nihilith_W_planks`; Regal-Rezept nimmt sie. **Neu** `simplebuilding:chiseled_W_bookshelf`
   (6 Nihilit-Bretter + 3 End-Stufen, WWW/SSS/WWW wie Lohenholz) mit Fortschritt; Simple Ends gemeißeltes Rezept bekommt die
   Bedingung „ohne SimpleBuilding“ (wie das Regal). Kein Konflikt mit Vanillas Rezepten (Nihilit-Bretter nicht in `#planks`).
4. **Regale bleiben in Simple End** (nicht nach Simple Enchanting): sie sind Teil jedes End-Holzsets und auch allein spielbar; die
   Nether-Regale liegen nur in SB/Enchanting, weil Lohenholz keine Holzfamilie eines Sub-Mods ist. Keine neuen Regal-Blöcke.
5. **Tab**: Simple End, je Holz direkt vor dem Bücherregal (Regal-Kette Bretter → Regal → gemeißelt). SB-Zeile „blazewood“ ohne
   Nihilit-Bretter. Simple Enchanting unverändert (QW11).
6. **Textur** (A8/A8b/A8c/A8d): Build-Zeit-Ableitung (`tools/textures/nihilplanks12_2026_11_12.py`, `vanilla_derived`
   MODULE_OUTPUTS/GENERATORS simpleend, `.gitignore`), nur `.png.mcmeta` eingecheckt. Ruhebild = echtes End-Brett, Fugen in
   Nihilit-Blau gleicher Helligkeit, jede Fuge atmet eigen (Rhythmus Lohenholz, 16 Bilder, frametime 4). Alte SB-Dateien
   (PNG/Layer/mcmeta, Wiki-Kopien) gelöscht; `end07`-Skript ohne Bretter.
7. **A12**: kein Vanilla-Eingriff (Dekor-/Regal-Zutat nur mit SB+Simple End); Vanilla-Regale/-Rezepte unverändert.

## Dateien
- SB: ModBlocks, ModItems, ModItemGroupsContent (blazewoodRow), RecipelessJeiInfo (Info-Seite weg: ohne Simple End gibt es die
  Bretter nicht mehr), FeatureStations (per Id), ModBlockTagProvider/ModLootTableProvider + generierte Loot/Axt-Tag, Overlay-Assets
  (blockstates/items/models/Texturen) löschen, Lang EN/DE (beide Ablagen) Bretter + JEI-Info raus, Rezepte (2 geändert, 2 neu +
  Fortschritte), EnchantingBlockTests, wiki/manual.json (Einträge raus, Nihilit-Buch-Text), `tools/textures/end07_2026_11_06.py`.
- Simple End: SimpleEnd (Schalter), EndWood (Block/Item/Tab), EndRegistry (Brennbarkeit), Loader-Einstiege (Schalter; Fabric
  IdMoves), gen_data.py (Blockstate/Modell/Item/Loot mit Bedingung, Axt-/void_safe-Tag `required:false`, gemeißeltes Rezept
  bedingt, Lang), generierte Dateien, Lang EN/DE, mcmeta, EndTests, wiki/manual.json, Discover (Generator).
- tools/textures: nihilplanks12 (neu), vanilla_derived (MODULE_OUTPUTS/GENERATORS), .gitignore.

## Prüfung
Lokal: precheck --fast, alle `modules/*/tools/check_data.py`, check_dup_impl, check_tooltip_text, `wiki/generate.py --all --check`,
check_missing_textures, `layers.py check`, discover --check, vanilla_derived --module simpleend, javac gegen das 26.3-Jar (geänderte
Klassen). Gametests (sb-test, Befehl in /root/sb-tools-a/a-nihilplanks-sbt.cmd): SB `enchanting_block`/`data_integrity`
(Tab-Zeile), Simple End `registered`/`wood_tags_and_recipes`/neu `nihilith_planks` (Registrierung nur mit SB, Rezepte, IdMove,
Tab-Position) mit SB und standalone.

## Texturrunden (A8c)
- Runde 1: Fugen = drei dunkelste Töne, Nihilit 12 Helligkeit dunkler. Bewertung: Nihilit klar sichtbar, aber Einzelpixel-Sprenkel
  auf den Brettern (dritter Ton liegt auch in Brettern), hart 10,4/13,1 % (Esche/Dämmer) → zu hart, Sprenkel weg.
- Runde 2: Fugen = zwei dunkelste Töne + dritter Ton nur in der Fugenlinie; Nihilit gleicher Helligkeit (A), tiefer (B), Welle (C).
  A: hart 7,9/6,7 % (Brett allein 6,7 %), texrate grün, Linien wechseln den Ton über die drei Fugen-Töne. B 14,6/18,8 % zu hart.
- Runde 3: D = A + Atem-Drift alle 3 px entlang der Fuge (A8d Punkt 4): lebendiger, aber hart 10,6/9,8 %. Gewählt **A**
  (ASTRAL07=A-Stil „jede Fuge atmet eigen“, A8b am nächsten); C/D als Alternativen in der Vorschau.

## Abweichungen
- Zusätzlich: Simple-End-Station der Testzentrale stellt die Nihilit-Bretter neben die End-Bretter; `docs/MODS-UND-FEATURES.md`
  (Zeile Simple End) nachgezogen; Recipe-Fortschritte der neuen Rezepte schreibt `tools/discover/discover.py` (Kriterium „hat Stufe“).
- Simple Enchanting unverändert (`PARTNER_ITEMS` bleibt; QW11 filtert die End-Regale dort ohnehin) – Besitzer-Frage F1.
- Lang von Simple End von Hand eingefügt (gen_data.py würde die Datei umsortieren); gen_data.py kennt die Schlüssel trotzdem.

## Prüfung (Stand Abschluss)
- Grün lokal: `precheck.py --fast`, alle `modules/*/tools/check_data.py`, `check_dup_impl`, `check_tooltip_text`,
  `wiki/generate.py --all --check`, `check_missing_textures` (0 Fehler), `layers.py check` (0), `discover.py --check`,
  `module_guides.py --check`, `guide_book_pages.py` (0), `vanilla_derived.py --module simpleend` (20 Texturen, Platzhalter 16×256).
- javac gegen das 26.3-Jar (Simple End + SB-Dateien): keine Fehler in den geänderten Stellen (übrige Meldungen = fehlende
  Access-Widener/Nachbarmodule im Klassenpfad, vorbestehend).
- Nicht gelaufen: Gametests (sb-test nicht erreichbar, Befehl `/root/sb-tools-a/a-nihilplanks-sbt.cmd`), Client-Sicht,
  NeoForge/Forge (nur je eine Zeile im Einstieg, nicht kompiliert).
- Vorbestehend rot, nicht angefasst: `end07 --check` (rissige Ziegel seit späteren Textur-Runden anders),
  `generate_textures.py --check` (fehlt `tools/textures/hand/enderite_carrot.png`).

## Offen
- sb-test-Lauf (Datei oben); Client-Sicht (Animation im Spiel, Tab); Besitzer F1–F4 (Vorschau a-nihilplanks-preview.html).
- NeoForge/Forge-Port: IdMoves fehlen dort (Simple End ohne SimpleLib) – alte NeoForge/Forge-Welten verlieren die Bretter.
- Befund außerhalb des Auftrags: `simpleend:block/{ashwood,duskwood}_bookshelf.png` (b-shelves01, eingecheckt) enthält 88/256
  Pixel, die 1:1 dem aus Vanillas Birkenbrett umgefärbten End-Brett gleichen (Rahmenzeilen) → wie hier auf Build-Zeit umstellen
  oder Rahmen neu zeichnen (Release-Blocker-Regel A8).

---

# a-nihilplanks2 – Einwand NIHILPLANKS (Besitzer 12.11. ~22:40 UTC)

Auftrag: /root/sb-tools-a/agenten-1530.md §17. Basis hub/b-bundle190 (d91f28407), Ziel Bündel 191. Wortlaut:
„Fugenfarbe ist viel zu extrem, bitte dezent halten.“ F1–F4 ohne Einwand = Empfehlungen (tab=A, tex=A-Stil „jede Fuge atmet eigen“,
chis=A, regal=A) – daran ändert sich nichts, nur die Farbe der Fugen.

## Ist-Stand
- `nihilplanks12_2026_11_12.strip()` Variante A: jeder Fugenton (zwei dunkelste Töne + dritter Ton in der Fugenlinie) wird **ganz**
  durch Nihilit gleicher Helligkeit ersetzt (Sättigung bis ~0,6 statt Holz ~0,13–0,20) und atmet ±32 % zwischen `DEEP` #19325f und
  `GLINT` #9cdddc → kräftige blau-türkise Streifen, Helligkeitssprung je Bild bis ~60. Härte 7,9/6,7 % (das war nicht das Problem).
- Ausgabe nur zur Build-Zeit (vanilla_derived, git-ignoriert); im Repo nur `.png.mcmeta` (16 Bilder, frametime 4, interpoliert).
- Wiki-/Handbuch-Texte nennen keine Farbe → keine Textänderung nötig.

## Plan
1. **Tönung statt Ersatz:** Fugenpixel = Holzton, nur zu einem Anteil `tint` Richtung Nihilit **derselben Helligkeit** gemischt
   (Farbton kippt leicht ins Blau-Grau, Helligkeitsabstand zum Holz bleibt 0, Sättigung klein).
2. **Sanfter Atem:** statt DEEP/GLINT schwankt nur der Tönungsanteil (×(1 ± 0,5)) und die Helligkeit um wenige Stufen (±4);
   Phasen je Fuge wie A (F2=A), 16 Bilder, frametime 4, interpoliert (mcmeta unverändert).
3. **2–3 Stärken** als Varianten (Runde 1: S1/S2/S3 = 0,15/0,25/0,35), A8c-Runden, eingebaut die **dezenteste plausible**
   (in Spielgröße 1×/4× noch als Nihilit zu erkennen, sonst wieder „wie normale Bretter“ – der alte Einwand vor a-nihilplanks).
4. `vorher` (Bündel 190, A) bleibt als Vergleichsvariante im Generator (nur `--preview`); B/C/D (Stil-Alternativen, durch F2=A
   entschieden) fallen weg.
5. Vorschau /root/sb-tools-a/previews/a-nihilplanks2-preview.html: je Holz End-Brett ohne Nihilit | vorher | S1 | S2 | S3, 1×/4×/12×,
   3×3-Kachel, Würfel, Animation als GIF mit der Spiel-Interpolation, ok/Einwand je Textur, Antwortcode NIHILPLANKS2.

Dateien: `tools/textures/nihilplanks12_2026_11_12.py` (VARIANTS/strip/Docstring), dieser Plan. Keine PNGs ins Repo.
Risiko: zu dezent → wieder „nicht da“; darum S1 im Vergleich zu S2 in Spielgröße prüfen und Begründung in der Vorschau.
Prüfung: Generator `--preview`, `vanilla_derived.py --module simpleend`, texrate (grün), `layers.py check`, `check_missing_textures`,
`modules/simpleend/tools/check_data.py`, `precheck.py --fast`. Gametests unnötig (nur Pixel; Registrierung unverändert).

## Runden a-nihilplanks2 (A8c/A8d)
Messung je Variante über alle 16 Bilder: Sättigung (HSV) der Fugenpixel, mittleres Lab-b* der Fugen (+ warm, − blau), Atem
(Helligkeitsschwankung). End-Brett allein: Fuge b* +6 (Esche) / +9 (Dämmer), vorher (Bündel 190): Sättigung 0,63/0,66, b* −20/−26,
Atem ±25.
- R1: gleichmäßige Tönung 15/25/35 %, Atem Anteil ×(1±0,5) und ±4 Helligkeit. 15 % ruht warm (b* +2) = wie normale Bretter;
  einfarbig getönte Linie wirkt flach; Fehler gefunden: in der untersten Fuge blieb 1 Holzpixel (Dreier-Lauf des dritten Tons).
- R2: Tiefe (dunkelster Fugenton ×1,0, zweiter ×0,8, Fugenrand ×0,6 → Linie wechselt Ton, A8d 4), Lücke geschlossen
  (`gap_pixels` wächst entlang der Fugenlinie). 18/26/36 %: natürlicher, 18 % ruht noch warm (b* +1,5/+2,3).
- R3: 22/30/40 % (S1/S2/S3). **S1 eingebaut**: Fuge ruht neutral-kühl (b* 0/+1), Sättigung ≤ 0,17/0,15, Atem ±4, im 2×/4×-Vergleich
  neben dem End-Brett als kühle Fuge zu erkennen, ohne Streifen. S2 grau-blau (≤ 0,26), S3 hellblau (≤ 0,37) als Alternativen.
  Alle: hart 6,7 % (Esche) / 4,0 % (Dämmer, ±4 Helligkeit glättet zwei 41er-Sprünge), texrate grün.

## Abweichungen a-nihilplanks2
- Neu `--check` im Generator (Auftrag nennt „Generator --check“, gab es nicht): eingecheckte `.png.mcmeta` = MCMETA, mit Jar
  Streifen 16×256 und Fugen-Sättigung ≤ 0,25 (`SAT_MAX`, schützt den Einwand). Gegenprobe: S3 als Bau → Exit 1.
- Stil-Varianten B/C/D und DEEP/GLINT entfernt (F2=A entschieden); „vorher“ in der Vorschau aus dem Generator von hub/b-bundle190.
- mcmeta unverändert (frametime 4, interpoliert): „sanft“ über die kleine Schwankung (±4 statt ±25), nicht über ein langsameres Tempo.

## Prüfung a-nihilplanks2
Grün lokal: `nihilplanks12 --check`, `vanilla_derived.py --module simpleend --out <tmp>` (Streifen = S1), texrate (6× grün),
`layers.py check` (0), `check_missing_textures` (0 Fehler), `modules/simpleend/tools/check_data.py`, `wiki/generate.py --all --check`,
`precheck.py --fast`. Nicht gelaufen: Client-Sicht der Animation im Spiel (Vorschau-GIF ahmt die Interpolation nach, 50 ms je Tick).
Gametests nicht nötig (nur Build-Zeit-Pixel, Registrierung/Daten unverändert).

## Offen a-nihilplanks2
- Besitzer-Abnahme NIHILPLANKS2 (Vorschau /root/sb-tools-a/previews/a-nihilplanks2-preview.html), Client-Sicht im Spiel.
