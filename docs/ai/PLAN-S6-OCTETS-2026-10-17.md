# Plan Stufe 6: Octets nach `simpleoctets` (2026-10-17)

Branch `claude-s6-octets` (von `claude-wave1` 369bcb4d1), Sitzung A. Grundlage: `PLAN-MODULAUFTEILUNG-2026-10-11.md`
(Stufe 6, §2.2, §3), `PLAN-MODULFAMILIE-2026-10-16.md` (§5 F7: Id `simpleoctets`), Vorbild Stufen 4/5/5b
(`PLAN-S5B-2026-10-16.md`), `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`, AGENTS.md 15-17.

## 1. Ist-Zustand (grep, Stand 369bcb4d1)

- **Zahlen (die 106/117 der Queue sind veraltet):** 15 Holz-Achtel (13 Vanilla + Ash-/Duskwood nur mit Simple End),
  153 Material-Achtel (`OctetMaterials`, 142 Vanilla, 7 SB-Paletten Astralit/Nihilith/Ender-Quarz, 4 Simple-End-Stämme),
  Melonen-Achtel, gemischte Zelle `mixed_octet` **mit Block-Entity** (N48, nicht „0 BE“ wie im Hauptplan).
  Rezepte: je Material/Holz `…_octet_from_<basis>_stonecutting` + `<basis>_from_octets` (336 + Advancements),
  12 davon von Hand im Overlay (Simple End, mit `mod_loaded`-Bedingung).
- **Klassen (SB `common/src/shared`):** `blocks/OctetMaterials` (generiert), `custom/OctetCellBlock` (Bits, Form, Wasser,
  Drops, Herausnehmen), `custom/OctetCells` (Teile lesen/schreiben, Mischen), `MaterialOctetBlock`, `MixedOctetBlock`,
  `entity/custom/MixedOctetBlockEntity`, `client/render/MixedOctetRenderer`, `items/custom/MaterialOctetItem`,
  `util/MaterialOctets` (Melonenscheibe setzen, Zellen an SimpleLib `OctetCells`), 26.3/26.2-Zwillinge
  `OctetParticleBlock`, `OctetBreakCracks`. Registrierung `ModBlocks` 496-640, `ModItems` 212-236, BE in den drei
  `ModBlockEntities`/Loader-Registries, Renderer in den drei Client-Einstiegen.
- **Bleibt SB (F3):** Schach-Achtel `checker_octet` (`CheckerOctetBlock extends OctetCellBlock`), 13 Farb-Items
  (`CheckerOctetItem` setzt über `MaterialOctetItem.place`), Schach-Rezepte/-Modelle (`block/chess/octet_corner`).
  Schach-Achtel mischen sich mit Material-Achteln in `mixed_octet` → die Zellen-Basisklasse muss beiden gehören.
- **SB-Nutzer:** Vorschlaghammer `HammerCorners` (Geometrie-Statics, `MaterialOctets.cellFor` = SimpleLib `OctetCells`),
  `BlueprintScanner` (`OctetCells.portable`), `ItemUseOnMixin` (Melonenscheibe), `InfoTooltips`, Tabs
  (`ModItemGroupsContent`, `SearchTabPlacement`), Testzentrale (`FeatureStations`, `TestCentreLayout`), Discover-Muster,
  Datagen (`ModRecipeProvider` 1306-1314, `ModBlockTagProvider` 236-262), Wiki-Notiz `*_octet`, Tests
  (`OctetMixTests`, `SledgehammerOctetTests`, `ChessTests`, `DataIntegrityTests`, `MigrationWorldTests`, Client-Test
  `OctetMixClientTest`). Generator `tools/textures/octets_2026_10_09.py` schreibt Modelle/Blockstates/Items/Lang ins
  26.3-Overlay und `OctetMaterials.java`. 26.2: `McVersion.CHESS=false` (keine Achtel registriert, Code muss nur kompilieren).
- Config: keine Octet-Schlüssel in SB (nichts zu übernehmen). Money: keine Octet-Preise. Texturen: nur
  `melon_flesh.png` eigene Kunst; alle Modelle zeigen Vanilla-/Mod-Texturen per Verweis (nichts Vanilla-Abgeleitetes).

## 2. Entscheidungen (selbst getroffen)

- **E1 Umfang:** alle Material-, Holz- und Melonen-Achtel + `mixed_octet` (+BE, Renderer) + die Zellen-Logik ziehen nach
  `simpleoctets`; Schach-Achtel/-Figuren bleiben SB (F3). Glitzernde Melonenscheibe als Nahrung (`GlisteringMelonFoodMixin`)
  bleibt SB (kein Achtel-Feature, nur die Konstante wandert in den Mixin).
- **E2 Gemeinsame Zellen-Basis:** das Modul besitzt `com.simpleoctets.block.OctetCellBlock` und `OctetCells`
  (öffentliche API für Partner). SB 26.3 bekommt im Overlay **dünne Unterklassen gleichen Namens**
  (`com.simplebuilding.blocks.custom.OctetCellBlock extends com.simpleoctets.block.OctetCellBlock`, ebenso `OctetCells`),
  damit SB-Code (Hammer, Schach, Blaupause, Tests) unverändert über die geerbten Statics arbeitet; `instanceof`-Prüfungen in
  SB laufen über `OctetCellBlock.isCell(block)` (Modulklasse, trifft auch Material-Achtel). 26.2 behält im
  `common/src/mc26_2`-Zwilling die alte Zellenlogik ohne gemischte Zelle. `MaterialOctetItem.place/placed` wandern als
  `OctetCells.place/placed` in die API (Schach-Items setzen darüber).
- **E3 Ids:** `simplebuilding:<x>_octet` (Block + Item), `melon_octet`, `mixed_octet` (Block + BE), Rezepte und
  Rezept-Advancements (`recipes/building_blocks/…`) → `simpleoctets:` per `IdMoves` (`SimpleOctets.registerIdMoves`, Listen
  aus `OctetMaterials` + Holzliste, also vollständig auch für nicht geladene Partner-Materialien). Forge-Aliase automatisch.
- **E4 Fremd-Materialien (optionale Integration):** Achtel aus Blöcken anderer Mods (SB-Paletten, Simple-End-Holz/-Stämme,
  künftig Simple Blocks) registriert das Modul nur, wenn der Mod geladen ist (wie bisher Simple End), mit fester Vorlage
  (End-Stein bzw. Eichenbrett/-stamm, Licht 10 für Astralit) und spät gelesener Quelle - unabhängig von der Ladereihenfolge.
  Rezepte/Advancements dazu mit `mod_loaded`-Bedingungen (Fabric/NeoForge/Forge). Neue Simple-Blocks-Achtel: Generator kann
  jeden Namensraum; Simple Blocks hat heute keine Treppe+Stufe-Familie mit Achtel → keine neuen Ids (offen notiert).
- **E5 Ressourcen:** Generator `octets_2026_10_09.py` schreibt künftig ins Modul (`modules/simpleoctets/shared/resources`):
  Blockstates, Modelle (eigene Vorlagen `simpleoctets:block/octet_corner`, `item/octet`, Kopie der Schach-Vorlage), Items,
  Lang EN/DE, `OctetMaterials.java` (Modulpaket), **neu auch Rezepte + Rezept-Advancements + Abbau-Tags**
  (bisher SB-Datagen). SB-Datagen 26.3 erzeugt keine Achtel-Rezepte/-Tags mehr (`McVersion.OCTETS_IN_SB`), datagen neu auf sb-test.
  `melon_flesh.png` per `git mv`. Lang `block.simpleoctets.mixed_octet` neu (offener Punkt). SB-Lang verliert die Schlüssel.
- **E6 Schalter + Bündel:** `simpleoctets` = Pflicht-Sub-Mod (SB kompiliert gegen die Zellen-API, wie Crucibles), trotzdem
  Schalter „Enable Simple Octets“ in der SB-Config (`enableSimpleOctets`, `SubMods`): aus = Tab ohne Guide, keine
  Vanilla-Tab-Einträge, Melonenscheibe setzt nicht; Registrierungen bleiben.
- **E7 Tabs:** eigener Tab `simpleoctets:main` (Holz, Melone, Material-Gruppen aus `OctetMaterials.GROUPS`, Guide-Buch);
  Vanilla-Tabs: jedes Achtel hinter seine Stufe bzw. seinen Vollblock (Schalter `addItemsToVanillaTabs`). SB-Tab und
  SB-Suchtab-Ketten nennen keine Material-Achtel mehr (Schach-Achtel bleiben).
- **E8 Weiteres wie 4/5:** JEI-Infoseite (Melonen-Achtel/Setzen, Jade nicht nötig), Guide-Buch (`module_guides.py`),
  Discover (`discover.py`), Testzentrale-Station (framework `TestCentreStations`), Wiki `modules/simpleoctets/wiki/manual.json`
  (Notizen `*_octet`, `melon_octet`, `mixed_octet` = jeder Block), Money `money/simpleoctets/prices.json` (keine Preise,
  Begründung), Standalone-Targets (`modules.json`), `integration/enabled-mods.json`, `docs/MODS-UND-FEATURES.md`.

## 3. Umsetzung (Dateien)

1. Modul `modules/simpleoctets/{shared,fabric,neoforge,forge,wiki,tools}` nach Vorbild `simpleovens`/`simplecrucibles`:
   `SimpleOctets` (init, IdMoves, SimpleLib-`OctetCells`, Teststation), `registry/OctetRegistry` (Blöcke, Items, BE, Tab),
   `block/*`, `item/MaterialOctetItem`, `client/*`, `mixin/{MelonSliceUse, FireFlammables, DiscoverTip}`, `guide`,
   `discover`, `compat/jei`, `test/OctetTests` (Registrierung, Setzen/Mischen, Melone, Steinsäge-Rückweg, Migration,
   Schalter, Fremd-Materialien nur mit Partner), Loader-Einstiege + Test-Kataloge.
2. SB: Registrierungen/Klassen entfernen, Overlay-Unterklassen + 26.2-Zwillinge, `util/OctetRefs` (Ids), `LibServices`
   meldet keine Zellen mehr, Tabs/Suchtab/Testzentrale/Tooltips/Discover/JEI/Wiki-Notiz/Datagen/Lang, Tests umziehen bzw. auf
   Ids umstellen, `MigrationWorldTests` erwartet `simpleoctets:`, Schalter `enableSimpleOctets` (+Lang, `ConfigOptionTests`).
3. Gradle: SB 26.3 Fabric/NeoForge/Forge bündelt + braucht `simpleoctets` (fabric.mod.json/toml-Filter),
   `gradle/module-neoforge.gradle` lädt es in SB-Läufen anderer Module.
4. Generatoren auf sb-test: datagen 26.3, `discover.py`, `module_guides.py`, `generate_links.py`, `wiki/generate.py --all`.

## 4. Risiken

- Verdeckte Kopplung über `instanceof OctetCellBlock` (SB-Unterklasse ≠ Modulklasse) → alle Stellen auf `isCell`.
- Registrierreihenfolge NeoForge/Forge: Zellen vor Items, BE nach Blöcken; Fremd-Quellen nur spät lesen.
- Masse der Dateien (≈ 700 Ressourcen) → nur per Generator erzeugen, nie von Hand.
- 26.2 muss kompilieren (Zwillinge ohne Modul-Import).

## 5. Prüfung (sb-test)

Compile 26.3 fabric/neoforge/forge + 26.2 `:compileJava`; Checks `checkModuleGuides checkModuleData checkWiki checkDiscover
checkMultimod`; SB-Filter `octet_mix`, `sledgehammer_octet`, `chess`, `data_integrity`, `migration` (fabric+neoforge);
`module-simpleoctets-{fabric,neoforge}-263`, Standalone fabric+neoforge, Forge-Compile. Nicht geprüft (bewusst): Client-Sicht.

## 6. Stand (17.10.) und Abweichungen

Umgesetzt wie geplant: Modul `modules/simpleoctets` (1.0.0, Pflicht-Sub-Mod, gebündelt auf allen drei Loadern), 170 Zellen
(15 Holz, 155 Material inkl. 2 neue aus Simple Blocks) + Melone + gemischte Zelle (BE `simpleoctets:mixed_octet`, Lang-Schlüssel
`block.simpleoctets.mixed_octet` neu), 340 Rezepte + Freischaltungen per Generator, IdMoves für Blöcke/Items/BE/Rezepte/
Advancements, Tab, Vanilla-Tab-Platzierung, JEI-Infoseiten, Handbuch, Discover, Testzentrale-Station, Wiki, Money-Tabelle,
Schalter „Enable Simple Octets“, Standalone-Targets, 15 Modul-Tests. SB 26.3: dünne Unterklassen `OctetCellBlock`/`OctetCells`,
26.2-Zwillinge, `util/OctetRefs`, Datagen ohne Achtel.

Abweichungen (bewusst):
- **Neue Achtel aus Simple Blocks** (`blue_nether_bricks_octet`, `hushed_deepslate_tiles_octet`, nur mit Simple Blocks): die
  Besitzerregel „jeder Block mit Treppe und Stufe hat ein Achtel“ gilt für die Familie; zeigt die optionale Integration.
- SB-Tests bleiben (auf Ids/`OctetRefs` umgestellt) statt umzuziehen; das Modul hat eigene Tests (allein lauffähig).
- SB-Suchtab-Ketten setzen die Modul-Achtel weiter per Id (Reihenfolge der End-Paletten); das Modul setzt sie zusätzlich selbst
  hinter die Stufe (beide überspringen schon vorhandene Stapel).
- Melonenscheiben-Tooltip per Mixin auf `Item#appendHoverText` (Vanilla-Item), Glitzermelone als Nahrung bleibt SB.
- Keine Config-Übernahme: SB hatte keine Achtel-Optionen. Kein Vanilla-abgeleitetes Bild (Modelle verweisen auf Texturen).
- Vanilla-Stamm-Achtel bleiben in `mineable/pickaxe` (wie in SB; vermutlich Altfehler, offen notiert).
