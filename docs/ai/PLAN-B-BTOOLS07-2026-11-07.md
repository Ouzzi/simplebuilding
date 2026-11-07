# Plan b-btools07 (Sitzung B, 07.11.2026) – Simple Building Tools als eigener Pflicht-Sub-Mod (WF25-9)

Auftrag: /root/sb-tools-b/b07-auftrag.md, Wunsch /root/sb-tools-b/wd25.md Z.69 („Neuer Sub-Mod Simple Building Tools: Baustäbe,
Blaupausen, Oktanten auslagern (eigener Zuständigkeitsbereich)“), dazu Z.68 (WF25-8: Kerne im Tab eher bei Gadgets „und Simple
Building Tools, wenn dort genutzt“). Basis hub/claude-wave1 4f2722c8a (Bündel 120). NUR 26.3 Fabric. Muster b-pads01
(`PLAN-B-PADS01-2026-11-04.md`: Kopie der Klassen, 26.2 behält alles, Flag `*_IN_SB`, Refs per Id) und Stufe 7 Hämmer
(`PLAN-S7-HAMMERS-2026-10-18.md`: Enderit per Ladebedingung, Verzauberungen als Daten, HammerActions).

## 1. Ist-Stand (grep, 4f2722c8a)
- Alles im SB-Kern `common/src/shared` (26.2 + 26.3): Items `ModItems` (6 Baustäbe Kupfer–Enderit + Kreativ-Baustab, Oktant + 16
  Farb-Oktanten, Blaupause + Kreativ-Blaupause), Klassen `items/custom/{BuildingWandItem,OctantItem,BlueprintItem,CreativeReach}`,
  `blueprint/*` (18 Klassen: Scanner, Builder, Code, Bild-Import, Kartentisch, ShapeFill), `util/{WandPlacement,WandUndo,WandHunger,
  OctantShape,OctantSurface,OctantCauldronWash,OctantHammerArea}`, Client `client/blueprint/*`, `client/gui/{BuildingWandScreen,
  OctantScreen,RangefinderHudOverlay}`, `client/render/BuildingWandPreviewRenderer`, Oktant-Umriss in `BlockHighlightRenderer`
  (gemischt mit platzierten Oktanten), `client/property/BlueprintStateModelProperty`, `items/tooltip/BlueprintTooltipData`,
  Netz 5 Payloads (`building_wand_configure`, `octant_configure`, `octant_scroll`, `blueprint_edit`, `blueprint_rotate`) +
  Handler in `ModMessageHandlers`, Mausrad in `MouseMixin`, Kartentisch-Mixins, Komponenten `blueprint`, `blueprint_rotation`,
  Verzauberungen `cover`, `bridge`, `linear` (nur Baustab) – `master_builder`/`color_palette` gelten auch für Bündel/Rucksack/Pinsel.
  ≈12 400 Zeilen Code, ≈11 400 Zeilen Tests (BuildingWand-, WandMode-, WandEnchantment-, BuildingEnchantment-, Blueprint-, Octant-,
  ConsumptionAndDurability-, ProtectionAndRange-Tests). Kein Modul, keine Vorarbeit (Queue „komplett offen“ stimmt).
- Oktant-/Octet-BLÖCKE: liegen schon in Simple Octets (`simpleoctets`), bleiben dort. Meißel/Spachtel: Agent b-chisels07.

## 2. Schnitt und Entscheidungen (selbst getroffen, begründet)
1. **Modul `simplebuildingtools`**, Name „Simple Building Tools“, Paket `com.simplebuildingtools` (Konvention wie `simplepads`,
   `simplehammers`: modid = Anzeigename ohne Leerzeichen klein), per `newmod.py --parent simplebuilding --bundle required
   --config-switch true --enderite`. Pflicht nur `simplelib`; Enderit-Baustab per Ladebedingung `simpleores` (Muster Hämmer –
   der Besitzer-Satz „requires simple ores“ galt nur Simple Pads); optional `simplebuilding`, `simplestorage`, `simpleoctets`.
2. **Inhalt = die Werkzeuge**: 7 Baustäbe, 17 Oktanten, 2 Blaupausen samt Motor, Bildschirme, Vorschau, Oktant-HUD, Payloads,
   Mausrad, Kartentisch (Blaupause kopieren/scannen), Oktant-Waschen im Kessel, Oktant-Fläche für Hämmer (über SimpleLib
   `HammerActions`, wie bisher SBs `OctantHammerArea`), Verzauberungen `cover`/`bridge`/`linear` (Daten), Rezepte, Modelle,
   Texturen, Lang, Advancements `wand/*`, `octant/*`, `blueprint/*` (eigener Auslöser `simplebuildingtools:tool_used`), Tab,
   Handbuch, Wiki, Preise.
3. **Bleibt SB**: platzierte Oktanten/Blaupausen (`PlacedTemplates`, Block `placed_smithing_template`/`placed_blueprint` – SB-
   Integrationsfeature; erkennt Modul-Items per Id, das Modul-Item ruft dafür `super.useOn`, den SB per Mixin erweitert),
   `master_builder`/`color_palette` (geteilte SB-Verzauberungen; Modul fragt sie per Schlüssel `simplebuilding:*`, ohne SB
   gibt es sie nicht → Rucksack-/Bündel-Bezug der Baustäbe nur mit SB+Simple Storage), Stufen-Advancements `tiers/*`,
   Testzentrale (liest Modul-Items per Id). Kerne (WF25-8): Baustab-Rezepte nutzen `simplelib:*_core`; der Tab „Simple Building
   Tools“ zeigt die Kerne nicht (sie gehören SimpleLib, Tab-Regel: nur eigene Items), WF25-8 bleibt Besitzer-Abnahme.
4. **26.2 behält alles** (`McVersion.BTOOLS_IN_SB`: 26.2 `true`, 26.3 `false`, `BTOOLS_NAMESPACE`). 26.3: SB registriert keine
   Werkzeug-Items (Felder `null`, Muster Gadgets), Komponenten, Payloads, Bildschirme, Renderer, Kessel-/Kartentisch-/Mausrad-
   Teile; Zugriffe über `util/BuildingToolRefs` (Ids, `isWand`/`isOctant`/`isBlueprint`, Farbe aus der Id).
5. **SB-Helfer im Modul ersetzt**: Kopien klein und vanilla-/SimpleLib-only (`WornStorage`→`CarriedStacks`, Rechte über
   framework `Protection`/SimpleLib `InteractionGuard`, Bild-RLE über `com.simplelib.api.image`, `ToolPlacements`,
   `OctetCells` SimpleLib-API), UI-Stil schlicht (eigene Panel-Zeichnung statt SBs `ModScreenStyle`), Statistik/Zähler entfallen.
6. **Config** `config/simplebuildingtools-server.json`: `features.{wands,octants,blueprints}`, `blueprintBlocksPerTick`,
   `pictureImport`, `pictureMaxSide`, `picturePermission`, `wandHungerMultiplier`; einmalige Übernahme aus `simplebuilding.json`.
   Schalter „Enable Simple Building Tools“ in SB (`SubMods`, newmod).
7. **IdMoves** `simplebuilding:` → `simplebuildingtools:`: ITEM (26), DATA_COMPONENT_TYPE (`blueprint`, `blueprint_rotation`),
   ENCHANTMENT (`cover`, `bridge`, `linear`), RECIPE + Rezept-Advancements, ADVANCEMENT `wand/*`, `octant/*`, `blueprint/*`.
   Migrationstest mit `id` (`old_tool_ids_move`).
8. **Tab** „Simple Building Tools“ im Modul (Tab-Regeln `CreativeTabPolicy`, Reihenfolge `creative_tab_order.json`); SB-Tab
   verliert die Werkzeuge auf 26.3.
9. **Daten** per git mv/Kopie mit Namensraum ins Modul; SB-Datagen überspringt die Werkzeuge auf 26.3 (`BTOOLS_IN_SB`), Datagen
   26.3 auf sb-test neu erzeugt (`removed-on-26.3.txt`).

## 3. Tests
- Modul: Kopien der SB-Werkzeugtests (soweit ohne SB-Klassen lauffähig), dazu `old_tool_ids_move`, `config_adoption`, Smoke;
  Targets `module-simplebuildingtools-fabric-263`, `module-simplebuildingtools-standalone-fabric-263`.
- SB: Werkzeugtests überspringen auf 26.3 (`if (!BTOOLS_IN_SB) succeed`), `simplebuilding:data_integrity*` angepasst.
- Lokal: javac-Typprüfung (sb-tools-b/b-guide01-javac.sh), precheck.py. sbt einmal: Compile, Modul-Targets, Kern-Filter.

## 4. Risiken
- Größe (≈25 000 Zeilen Code+Tests): mechanische Kopie per Skript, Typprüfung lokal.
- Ladereihenfolge: SB liest Modul-Items nur per Id nach der Registrierung.
- Client-Sicht (Vorschau, Umriss, Bildschirme) nicht im Gametest → offen (Bündel-Client-Lauf).

## 5. Stand / Abweichungen (07.11.)
Umgesetzt: Modul `modules/simplebuildingtools` (1.0.0, Pflicht-Sub-Mod, gebündelt, nur Fabric 26.3) mit Kopien der SB-Klassen
(Paket `com.simplebuildingtools.*`, gleiche Unterpakete), `SimpleBuildingTools` (Init, IdMoves, Config, `fire`), `BtItems`, `BtComponents`,
`BtEnchantments`, `BtConfig`/`BtTuning` (+ Payload `simplebuildingtools:config`), `BuildPermissions` (framework `Protection` + SimpleLib
`HammerActions.mayChange` + Fabric-Abbau-Ereignis als Probe), `OctantHighlightRenderer` (Oktant-Teil von SBs BlockHighlightRenderer),
HUD über SimpleLib `GadgetHud` (Kasten `simplebuildingtools:octant`), Mixins (Kartentisch, Mausrad, Kessel-Accessor, GUI-Accessor),
Daten (Texturen kopiert, Modelle, Rezepte + Freischaltungen, Verzauberungen, Tags), Lang EN/DE (≈980 Schlüssel, `simplebuilding.*` →
`simplebuildingtools.*`), Tab, Handbuch (module_guides, Freischalt-Item Stock), Discover (Tipps Baustab/Blaupause von SB übernommen),
Wiki-Manual (Features building_wand/octant/blueprint aus SB verschoben + submod/config_server/migration), Preise (24 Zeilen aus SBs
Tabelle), 94 Modul-Tests (84 verschoben + 10 eigene). SB: `McVersion.BTOOLS_IN_SB`, `util/BuildingToolRefs`, Felder null,
Tabs/Loot/Tooltips/Testzentrale/Datagen/Tags/Advancements per Id, Verzauberungs-Schlüssel und -Tags im Modul-Namensraum,
Listener `GadgetActions` (wand_build, octant_mark, blueprint_*, wand_block) für SBs Fortschritte/Zähler/Statistik, 26.3-Schlüssel K
und Oktant-Figur nur im Modul, SB-Tests mit Werkzeugen überspringen auf 26.3 (143 Methoden), DataIntegrityTests angepasst.

Abweichungen (bewusst):
- **Fortschritte bleiben SB** (`wand/*`, `octant/*`, `blueprint/*`), Auslöser über SimpleLib `GadgetActions` statt eigenem Modul-Auslöser
  (Muster Simple Gadgets) - keine ADVANCEMENT-IdMoves außer den Rezept-Freischaltungen.
- **Beute und Handel bleiben SB** (Truhen-Injektionen, Maurer-/Fahrender-Händler-Trades; Ids per Build-Umschreibung/Refs).
- **Abgelegte Blaupause bearbeiten** (Rechtsklick mit leerer Hand, N23) geht auf 26.3 vorerst nicht (SB-Editor kennt nur 26.2-Blaupausen,
  Modul-Handler ignoriert `placed`) - offen. Ablegen/Anzeigen/Abbauen geht (SB erkennt Modul-Items per Id; Modul ruft `super.useOn`).
- **SB-Config-Schlüssel** (`server.features.blueprint`, `server.blueprint.*`, `tools.*Wand*`, `tools.invertOctantSneak`) bleiben sichtbar,
  wirken auf 26.3 nicht mehr (wie Hämmer bis Stufe 9) - offen. Client-Optionen Deckkraft/H/Inversion steuern das Modul über
  `ClientState.configure` (SB-Overlay `LibServices`, Muster `HammerVisuals`).
- 13 SB-Testmethoden ließen sich nicht ohne SB-Klassen verschieben (Beute-/Rezept-Prüfungen, Testzentralen-Kit, Bild-Schalter) und
  laufen im Modul nicht; dafür `tool_recipes_are_there`, `enderite_wand_needs_simple_ores`.
- SimpleLib/Simple Octets: neuer Haken `OctetCells.registerPortable/portable` (gemischte Zelle beim Scan) statt SBs Overlay-Klasse.
- NeoForge/Forge: nicht angepasst (Regel), dort registriert SB weiter seine Tasten; Modul nur Fabric.

## 6. Tests (07.11.)
- Lokal: javac-Typprüfung Modul + geänderte SB-Dateien (nur Access-Widener-Artefakte), precheck 85 Prüfungen grün bis auf Wiki (Pillow auf
  sb-agent zu alt; auf sb-test `wiki/generate.py --all --check` grün).
- sbt Lauf 1: Datagen/Tests scheiterten an der Modulversion (modules.json 1.0.0, Jar 0.1.0 - zurück auf 0.1.0) und am Interface im
  Mixin-Paket (verschoben) - beides behoben.
- sbt Lauf 2 (`/root/sb-tools-b/b-btools07-sbt2.log`): Datagen 26.3 grün (Ergebnis gemergt, `mineable/pickaxe.json` nur umsortiert -> Basis
  behalten), Compile `:mc26_3:fabric:compileJava :mc26_3:fabric:compileGametestJava :modules:simplebuildingtools:fabric:compileJava` grün,
  check_client_mixins 374/0, wiki --check grün, check_missing_textures grün; Modul 93/94, standalone 91/94; SB `data_integrity*` 42/48,
  `smoke*` 0/1. Alle roten danach behoben (Schlüssel-Literale im Formen-Test, Blaupausen-Beispiele ins Modul, Adapter-Name, statische
  Testlisten per Id, Dev-Tab/Buchzählung/Stufen-Zahl/Bootstrap-Tag) - **nicht erneut gelaufen** (Auftrag: höchstens ein zweiter Lauf),
  prüft der Bündel-bprep.
