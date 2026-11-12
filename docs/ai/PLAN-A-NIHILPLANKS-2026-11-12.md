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

## Abweichungen / offen
- (wird beim Abschluss ergänzt)
