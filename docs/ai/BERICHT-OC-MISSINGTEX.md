# Bericht oc-missingtex (2026-10-14)

**Prüfskript** `tools/check_missing_textures.py` (Python, ohne Gradle; in AGENTS.md §4 dokumentiert): baut den ausgelieferten Ressourcenbaum nach
(Overlay > gen263 > src/main > gen262 > modules/*, inkl. vanilla-derived und Atlas-Permutationen), löst Item-Definitionen und Blockstates -> Modelle (Parent-Kette, `#vars`, `{sprite}`)
-> Texturen auf, prüft auch Atlas-Zugehörigkeit (Textur außerhalb block/ item/ ohne Atlasquelle) und Shulker-Special-Texturen. `--jar` prüft `minecraft:*`. Warnungen: Lang-Key/registerItem ohne Item-Definition.
Lauf vorher: 4 Fehler; nachher: `0 errors` / `alles gruen` (1191 Item-Defs, 794 Blockstates, 5025 Modelle; mit `--jar` ebenfalls 0 Fehler).

**Fund 1 (Karomuster):** `<ns>:guide_book` der Module simplemobheads, simplemobs, simpleovens, simpletrims hatten Item/Modell, aber nie eine Textur (nie in Git; nichts wiederherstellbar).
Neu gemalt im Prachtband-Stil über `tools/textures/module_guide_books_2026_10_05.py` (4 neue BOOKS-Einträge, `--check` grün); Vorschau `docs/ai/previews/oc-missingtex/module-guide-books.png`. Wiki-Texturkopien neu erzeugt.
Alle übrigen WARN (11) sind Alt-Lang-Keys ohne registriertes Item (guide_book_* im MEGA_GUIDES-Modus, home_teleporter).

**Fund 2 (Färbung, 26.3):** (a) Getragener Rucksack (`textures/entity/backpack/*_dyed.png`) war als einziger *_dyed-Satz nicht neutral grau (Stufenton 54-79 Sättigung) -> Farben matschig und anders als Item/Block.
Neutrale Ebenen im 26.3-Overlay ergänzt (`storage_dye_palette.py --write-only`, `entity/backpack` neu in `write_neutral_layers`). Vorher/Nachher: `worn-backpack-before-after.png`.
(b) Abgestelltes farbiges Vanilla-Bündel nahm die rohe DyeColor-Diffuse (dunkler als im Inventar und als gefärbte Mod-Bündel) -> `PlacedBundles.dyeColor` nutzt auf 26.3 `StorageDyes`; Test `PlacedBundleTests` angepasst.
Geprüft ohne Befund: Tint-Index (alle *_dyed-Modelle: genau ein `minecraft:dye`-Tint, layer0 getönt, layer1 Overlay), 16 Rezepte `<farbe>_dyed_storage` (Farbstoff == dyed_color == StorageDyes), alle Item-/Block-Ebenen neutral.
Prüfskript `tools/textures/dyed_storage_preview.py [--check]` + Vorschau aller Farben x Stufen: `docs/ai/previews/oc-missingtex/dyed-storage-colors.png`.

**Tests:** Compile 3 Loader ohne Fehler; `simplebuilding:placed_bundle*` Fabric+NeoForge `alles gruen: 8/8`; `simplebuilding:dyed*` `alles gruen: 6/6`; `wiki/generate.py --all --check --strict` ok; `generate_textures.py --check` OK; `module_guides.py --check`, `discover.py --check` ok.
**Nicht getestet:** Client-/Sichtprüfung im Spiel (Vorschau ist simuliert: Textur x Tint), Forge-Spieltests. Die 4 `placed_*_dyed.png` überlappen Farb-/Overlay-Ebene absichtlich (Platzhalter-Sheets); der Neutralisierer würde sie leicht ändern, bewusst nicht angefasst.
**Abweichung/Entscheidung:** Modul-Buchfarben frei gewählt (Knochenweiß/Creeper-Grün, Dunkelrot/Seelenfeuer-Cyan, Ofenstein/Glutorange, Nachtblau/Besatzgold). Nicht nach Production gezogen: TODO „development -> Production ziehen“.
