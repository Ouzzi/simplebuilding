# Plan claude-q-blocks (2026-10-09): Kreativ-Tabs N22, Naturblöcke N24/N25

Auftrag: QUEUE Nachtrag 22 (Kreativ-Tabs) und die Block-Punkte aus Nachtrag 24/25.
Branch `claude-q-blocks` (von `claude-wave1`), nur 26.3 (Fabric zuerst, NeoForge/Forge im selben Zug).

## Ist-Zustand

- `ff143698a` (claude-wave1) hat N22 für SimpleBuilding schon zur Hälfte erledigt: Spacer aus
  (`CreativeTabLayout.SPACERS_ENABLED = false`, Konstante), Schalter `addItemsToVanillaTabs` (Standard an) mit
  Hooks auf allen Loadern, Tests „kein Spacer“. Offen laut `PLAN-CREATIVE-TABS-2026-10-08.md`: Schalter je Modul.
- Eigene Tabs: SimpleBuilding (8 Kategorie-Tabs), SimpleFun, SimpleMoney, SimpleRiding, SimpleSandwiches, SimpleLib.
  Module ohne eigenen Tab (Dimensions, Models, QoL, Tweaks, Sounds, Visuals, Containers) haben als eigene Items
  nur ihr Handbuch (bzw. ein Alt-Item); ihre Inhalte liegen in SimpleBuilding-Tabs.
- Vanilla-Tab-Einsortierung der Module: Handbuch nach „Werkzeuge“ (Dimensions, Models, QoL, Tweaks, Fun), Fun
  zusätzlich Kampf/Gebrauchsblöcke, Money Zutaten, Riding Kampf/Zutaten – jeweils ungeschaltet.
- Alle Module bündeln `:framework` (reines Java, `com.simplebuilding.framework.api`); SimpleLib ist nur in
  Riding, QoL, Sandwiches, Containers eingebunden.

## Entscheidungen

1. **Spacer per Config** statt Konstante: `SimplebuildingConfig.creativeTabSpacers` (Standard aus, Reiter
   „advanced“ neben `addItemsToVanillaTabs`). `CreativeTabLayout.spacersEnabled()` liest sie; Logik bleibt.
2. **Vanilla-Tab-Schalter je Mod** als gemeinsamer Baustein in `framework` (`CreativeTabSettings`, seit 0.1.5),
   nicht in SimpleLib: jedes Modul bündelt framework bereits, SimpleLib dagegen nur vier Module – ein neuer
   SimpleLib-Zwang für fünf Module nur für einen Schalter widerspräche der Modul-Unabhängigkeit.
   Datei `config/simple-creative-tabs.properties`, je Mod ein Schlüssel `<modid>.addItemsToVanillaTabs`
   (Standard `true`, fehlende Schlüssel werden ergänzt). SimpleBuilding behält zusätzlich seine Cloth-Option.
   Alle Modul-Hooks (Fabric/NeoForge/Forge) fragen den Schalter.
3. **„Jede Super-Mod hat einen eigenen Tab“**: erfüllt für alle Mods mit eigenen Spiel-Items. Module, deren
   einziges Item das Handbuch ist, bekommen keinen Ein-Item-Tab (Handbuch bleibt in „Werkzeuge“). SimpleBuilding
   behält seine Kategorie-Tabs (Besitzer N5: „Tabs weiter aufteilen wie Vanilla“).
4. **Naturblöcke** (Flag `McVersion.NATURE_VARIANTS`, 26.3 an, 26.2 aus), alle in SimpleBuilding:
   - `dirt_slab`, `grass_slab`, `sand_slab`, `gravel_slab` (Stufen = Slabs). Rezepte 3 → 6 wie Vanilla;
     **keine Steinsäge** (Vanilla schneidet nur Stein/Kupfer, Erde/Sand passen nicht). Schaufel-Tag.
     Gras-Stufe: biomgefärbt wie Grasblock, Beute ohne Behutsamkeit = Erd-Stufe. Kein Ausbreiten/Absterben
     (bewusst: Stufe bleibt, wie gebaut). Sand-/Kies-Stufe fallen wie Sand (eigener `FallingSlabBlock`):
     obere Stufe fällt als untere, eine fallende untere Stufe auf gleicher unterer Stufe wird zur Doppelstufe.
     Kies-Stufe ohne Feuerstein-Chance (Bauteil, kein Abbauprodukt).
   - `chiseled_packed_ice`, `chiseled_blue_ice` (eigene Pixel-Art, Steinsäge 1:1 aus Pack-/Blaueis, Meißel nicht
     angefasst), `cracked_ice` (Modelle zeigen auf Vanillas `frosted_ice_0..3`, Steinsäge aus Eis; wer drauf steht,
     lässt es in 4 Stufen à ~1 s reißen, dann Wasser – im Nether verdampft es). Beute wie Eis: nur Behutsamkeit.
   - `nautilus_shell_block`: Säule (Rotation wie Knochenblock), 4 Nautilusschalen (2×2) ↔ 1 Block. Eigene Textur.
   - Froschlichter in **Scharlach (rot), Aqua (cyan), Azur (blau)**: schließen die Farbkreis-Lücken zwischen
     Ocker (gelb), Grün (verdant) und Perlmutt (violett). Herkunft: formlos Froschlicht (beliebig, Tag
     `simplebuilding:froglights`) + Farbstoff rot/cyan/blau. Vanilla-Farben bleiben nur über Frösche erhältlich.
     Eigene Texturen (Säule wie Vanilla-Froschlicht: Seite + Stirn), Leuchtkraft 15.
   Einsortierung: eigener Tab „Bausteine“ (Zeile „Naturvarianten“) + Vanilla „Natur“ hinter dem jeweiligen
   Grundblock, Froschlichter zusätzlich in „Gebrauchsblöcke“.

## Dateien (Auszug)

framework `CreativeTabSettings`, `FrameworkVersion`, `framework/build.gradle`, `gradle/forge-framework.gradle`;
`SimplebuildingConfig`, `CreativeTabLayout`; Modul-Hooks Dimensions/Fun/Models/Money/QoL/Riding/Tweaks;
`McVersion` (26.2 + 26.3), `ModBlocks`, `ModItems`, neue Klassen `FallingSlabBlock`, `CrackedIceBlock`;
Datagen Modelle/Beute/Rezepte/Tags; Gras-Färbung je Loader; `ModItemGroupsContent`, `SearchTabPlacement`;
Lang EN/DE (beide Bäume), `wiki/manual.json`; Texturen `tools/textures/nature_blocks_2026_10_09.py`;
Tests `NatureBlockTests` + Wrapper/Katalog, `DataIntegrityTests` (Zeilen, Tab-Inhalt).

## Verifikation

Compile 3 Loader (+26.2 Fabric), Tests `simplebuilding:nature_block*`, `*tab*`, `*creative*`, Config-Optionen,
Wiki `--all --check`, Texturvorschau `/root/previews/blocks/`.

## Testbericht (b866dd676)

- Compile `:mc26_3:fabric/neoforge/forge` + 26.2 `:compileJava :neoforge:compileJava`: grün.
- `simplebuilding:nature_block*` fabric/neoforge/forge-263: **alles gruen: 12/12**.
- `simplebuilding:data_integrity*` (Tabs, Spacer-Config, Framework-Schalter, Suchtab, Beute, Namen): **alles gruen: 147/147**.
- `simplebuilding:config_option*`: **alles gruen: 57/57**; Module simplemoney 19/19, simplefun 35/35, simpleriding 41/41 (fabric-263).
- Wiki `--all --check` grün, `nature_blocks_2026_10_09.py --check` grün, check_data der Module grün.

Abweichungen: Schalter in `framework` statt SimpleLib (Begründung oben); EN-Name „Block of Nautilus Shell“ (Namensmuster-Test);
Landen einer unteren Stufe auf gleicher unterer Stufe wird in `canBeReplaced` zur Doppelstufe (gleicher Zustand ließe
FallingBlockEntity sonst ein Item droppen). Nicht getestet: Client-Sicht (Färbung Grasstufe, Texturen), Geldpreise.
