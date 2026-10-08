# Plan: Schach-Assets – Checker-Treppen/-Stufen (b) und Achtel (a), 2026-10-08

Quelle: `.claude/QUEUE.md:449` – „0,125er Schachfiguren (0,5×0,5×0,5) rendern nicht bzw. falsch.
Checker-Treppen und -Stufen haben im Inventar das falsche Blockmodell (vermutlich Seiten vertauscht)."

Branch `bp-chess`. Hauptlinie 26.3, Fabric zuerst. Kein Push (nur Arbeitsbranch).

## Ist-Zustand (erhoben)
- Vollblock `*_checker` (Datagen `registerMirroredChecker`, `src/main/generated`): `north`/`south` = `*_checker_mirror`,
  `east`/`west`/`up`/`down` = `*_checker` (Vanilla-`cube`-Modell). Grund: Checker-Textur kachelt nur so stetig.
- Treppe/Stufe der Checker (Datagen `registerCheckerShapes`, `mc26_3/generated`): nutzen `ModelTemplates.STAIRS_*`/
  `SLAB_*`, alle Seiten **plain** `*_checker`. Dadurch weichen Nord/Süd vom Vollblock ab → im Inventar
  (gui-Sicht `[30,135,0]` Treppe / `[30,225,0]` Stufe) sichtbar vertauscht.
- Vanilla-Geometrien (`/var/tmp/sb/opencode/chess/vanilla/.../models/block/`): `stairs`, `inner_stairs`,
  `outer_stairs`, `slab`, `slab_top` vollständig bekannt (UV/cullface je Face).
- Achtel `checker_octet`: Blockstate-Multipart (104 Teile) referenziert `block/chess/octet_<farbe>` → `octet_corner`
  (Element [0,0,0]-[8,8,8]) mit `x`/`y`-Rotation je Ecke. Statisch geprüft: Property-Namen (`color`,`o000`..`o111`),
  serialisierte Werte (`ChessColor.getSerializedName` = klein), Texturen (13× vorhanden), Item-Defs, Modellpfade
  **alle korrekt**. Interne Rotationsreihenfolge X-dann-Y passt zu Vanilla (an `oak_stairs` top facing=south gegen
  die Wiki-Definition „facing = Richtung der vollblockigen Seite" verifiziert). **Ursache (a) statisch nicht gefunden.**

## Umsetzung (a) – offen, Kandidat Client
- Der Plan `docs/ai/PLAN-SCHACH-2026-10-06.md:64,81` hält fest: Client-Sicht nie geprüft (kein Client gestartet).
- Nächster Schritt: Client-/Render-Sicht herstellen (GameTest kann kein Blockmodell-Rendering prüfen, weil das
  Blockstate rein clientseitig aufgelöst wird). Ohne Client: robuster Ersatz der Blockstate-Rotation durch 8 fest
  platzierte Eck-Modelle (`octet_<farbe>_<oabc>`), die die Rotation ganz eliminieren – erst nach Client-Befund.

## Umsetzung (b) – umgesetzt
- 5 handgeschriebene Vorlagen in `mc26_3/overlay/resources/assets/simplebuilding/models/block/`
  (`template_checker_stairs[_inner|_outer].json`, `template_checker_slab[_top].json`): Vanilla-Elemente, aber
  `north`/`south` → `#side_mirror`, `east`/`west` → `#side`, `up` → `#top`, `down` → `#bottom`; Parent bleibt
  `minecraft:block/stairs|inner_stairs|outer_stairs|slab|slab_top` (display/particle).
- `ModModelProvider`: `SIDE_MIRROR = TextureSlot.create("side_mirror")` + 5 `ModelTemplate` (Suffixe
  `_stairs`/`_stairs_inner`/`_stairs_outer`/`_slab`/`_slab_top`); `registerCheckerShapes` nutzt sie und mappt
  `side_mirror` = `<name>_checker_mirror`.

## Verifikation
1. Datagen 26.3 Fabric → `mc26_3/generated` ändert genau 60 Kind-Modelle (`*_stairs`, `_stairs_inner`,
   `_stairs_outer`, `_slab`, `_slab_top`) um `side_mirror`-Textur + Parent.
2. `python3.12 tools/testrunner/run.py --targets fabric-263 --filter data_integrity` (ein schwerer Lauf) – Zeile
  „alles gruen" lesen. Asset-Chain-Test deckt Modell-/Textur-Existenz ab.
- Nicht abgedeckt: optische Client-Kontrolle der Inventar-/Weltdarstellung; Rotations-Reihenfolge der Achtel.

## BERICHT (2026-10-08)
- Datagen `:mc26_3:fabric:runDatagen` grün (BUILD SUCCESSFUL). `mc26_3/generated`: genau **60** Kind-Modelle geändert
  (12 Farben × 5), jedes mit `parent: simplebuilding:block/template_checker_<form>` und `side_mirror` =
  `<name>_checker_mirror` auf Nord/Süd. Keine Blockstate-/Item-Datei mehr geändert (Namen unverändert). Zwischenlauf
  mit doppeltem Suffix (`_stairs_stairs`) verworfen: Vanilla-Vorlagen nutzen den Blocknamen ohne zusätzliches Suffix
  (`_inner`/`_outer`/`_top`).
- Test `python3.12 tools/testrunner/run.py --targets fabric-263 --filter "simplebuilding:data_integrity_game_test_*"`:
  **„All 46 required tests passed :)"**, BUILD SUCCESSFUL. Enthält den Modell-/Textur-Existenztest, der die neue
  Template-Parent-Kette und die `_mirror`-Texturen auflöst.
- Achtel `checker_octet` zusätzlich per Skript validiert: 104 Multipart-Teile, **jede** der 13×8 Zustände trifft genau
  ein Teil, alle referenzierten Modelle und Texturen vorhanden. Rotationsreihenfolge X-dann-Y stimmt mit Vanilla
  (`facing` = Richtung der vollblockigen Seite; top/south `{x:180,y:90}` ergibt korrekt Süden) → Blockstate geometrisch
  korrekt, alle 8 Ecken mathematisch geprüft.
- **Offen (a):** Ursache des Achtel-Renderfehlers statisch nicht auffindbar; es bleibt eine reine Client-Sicht
  (Rendering/Blockstate-Auflösung läuft nur clientseitig). Benötigt einen Client-Lauf (Testrunner `--targets client`
  oder Handoff an den Besitzer). Kein Client gestartet (Regel: nur ohne offene Besitzer-Clients, schwere Last).
- **Nicht getestet:** optische Inventar-/Weltdarstellung der Treppen/Stufen (nur Datenebene), Client-Sicht Achtel,
  NeoForge/Forge (keine Ressourcen-/Teständerung nötig, Datagen ist Fabric-kanonisch).