# Plan b-blocksmove (18.10.2026): Baublöcke ohne Fachort nach `simpleblocks` + Kreativtab T2

Branch `b-blocksmove` (von `ff4fa8502`), Sitzung B. Queue 17.10. (b) + (c) T2. Grundlage: `INVENTAR-BAUBLOECKE-2026-10-17.md`,
Vorbilder Stufe 4 (Ovens), 5b (`PLAN-S5B-2026-10-16.md`), 6 (`PLAN-S6-OCTETS-2026-10-17.md`), T1 (`PLAN-B-TABT1-2026-10-18.md`),
`PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`, AGENTS.md 3.16-3.19. E-B1: Lichtscheiben + Schwefellichter bleiben SB.

## 1. Ist-Zustand (grep, Stand ff4fa8502)

- **15 Items** in SB `ModBlocks`/`ModItems`: Schwerkraft `suspended_sand|gravel` (Vanilla-`Block`), `levitating_sand|gravel`
  (`LevitatingBlock` + Entity `simplebuilding:levitating_block` = `LevitatingBlockEntity`, Renderer `FallingBlockRenderer` in drei Clients,
  DataFixer-Zweig in `ModDataFixer`) - auch auf 26.2 registriert. Naturvarianten (nur 26.3, `McVersion.NATURE_VARIANTS`):
  `dirt|grass|sand|gravel_slab` (`MergingSlabBlock`, `FallingSlabBlock`; Gras-Stufe biomgefärbt = Block-Tint in drei Clients, eigenes Modell
  + Beute im Overlay), `cracked_ice` (`CrackedIceBlock`, AGE 0-3; Mixins `BlockItemMixin` „nicht anbauen“, `HalfTransparentBlockMixin`
  Flächen wie Eis), `chiseled_packed_ice`, `chiseled_blue_ice`, `nautilus_shell_block` (Säule), `scarlet|aqua|azure_froglight` (Säule, Licht 15).
- Daten: SB-Datagen (Modelle, Blockstates, Items, Loot, Rezepte + Freischaltungen, Tags `mineable/*`, `slabs`, `ice`, `simplebuilding:froglights`),
  Overlay (Gras-Stufe Modell/Beute, Texturen Froschlichter/Nautilus/gemeißeltes Eis = Besitzer-/eigene Kunst, eingecheckt), `craftable`-Tag,
  Lang EN/DE (src/main + Overlay; Tooltips `tooltip.simplebuilding.levitating|suspended`, Entity-Name), Questionary-Einträge, Wiki `manual.json`,
  Advancement `end/defying_gravity` (Kriterium: die vier Schwerkraftblöcke), Money-Preise (4 Schwerkraftblöcke, Links).
- SB-Nutzer: Tabs (`buildingBlocksRows` Zeilen `gravity_blocks`, `natureRows`), `SearchTabPlacement` (Vanilla-Tabs), `InfoTooltips`,
  `RecipelessJeiInfo` (Eissplitter-Seite), `SledgehammerChips` (Hammer: Eis -> rissiges Eis -> Splitter), `ChiselItem` (Eis <-> rissiges Eis),
  Weltgenerierung `ModWorldGen` (rissige-Eis-Flecken/-Felder, Schalter `server.oreGeneration.crackedIceFields`, Biom-Tags, Biome-Modifier),
  `TestCentreSections`, Discover, Guide, Tests (`NatureBlockTests`, `GravityBlockTests`, `BlockBehaviourTests`, `DataIntegrityTests`, `ChiselTests`,
  `FurnaceTests`, `ImmersionTests`, `SledgehammerTests`, `WorldUpgradeTests`, `CrackedIceFieldChecks`, Client-Tests N52/N53, Simple-Blocks-`SawTests`).
- Simple Blocks: 0.1.0, `bundle: optional`, `requires: []`, eigene Daten per `tools/generate_data.py` (+ `check_data.py`), 4 Formsäge-Rezepte
  liefern `simplebuilding:*_slab` (mit `mod_loaded simplebuilding`). Tab `simpleblocks:blocks`; T1: SB hängt seine Zeilen an.

## 2. Entscheidungen (selbst getroffen)

- **E1 Umfang:** alle 15 Items + Entity-Typ `levitating_block` + die Blockklassen und das Verhalten (Fallen/Steigen, Stufen-Verschmelzen,
  Reißen, Anbausperre, Flächen-Tarnung, Gras-Tint, Tooltip der Schwerkraftblöcke) ziehen nach `simpleblocks` (Paket `com.simpleblocks.nature`).
  **In SB bleiben** (SB-Funktionen, die die Blöcke per Id nutzen): Weltgenerierung des rissigen Eises (SB-Config-Schalter, Biom-Tags),
  Hammer-/Meißel-Wege, Eissplitter-JEI, Advancement `defying_gravity`, Kolben-/Schwerkrafttests (`GravityBlockTests`, `BlockBehaviourTests`).
- **E2 Ids:** `simplebuilding:<x>` -> `simpleblocks:<x>` (BLOCK + ITEM je 15, ENTITY_TYPE `levitating_block`, RECIPE + Rezept-Advancement
  jeder bewegten Rezept-Id) per SimpleLib `IdMoves` in `SimpleBlocks.registerIdMoves()` (Loader-Konstruktor, vor den Registry-Events;
  Forge-Aliase automatisch). Simple Blocks bekommt dafür SimpleLib als gebündelte Pflicht (Regel 6a, wie Ovens/Octets).
- **E3 26.2:** `McVersion.BLOCKS_IN_SB` (26.2 true, 26.3 false) + `BLOCKS_NAMESPACE`; 26.2 behält seine Schwerkraftblöcke (SB-Klassen bleiben
  in `common/src/shared`, nur registriert wenn `BLOCKS_IN_SB`). Naturvarianten gab es auf 26.2 nie: deren SB-Klassen ziehen ganz um.
  SB erreicht alle 15 nur per Id (`util/BlockRefs`, Muster `OvenRefs`), kein Import aus `com.simpleblocks` (Prinzip 3).
- **E4 Rezepte (Prinzip 1/5):** Stufen 3 -> 6, Eis am Steinmetz, Nautilus 2x2 <-> Block, Froschlicht + Farbstoff (Tag `simpleblocks:froglights`)
  sind Vanilla-Material und bleiben unbedingt. Schwerkraftblöcke: SB-Weg (8 Sand/Kies + Nihilit-Splitter bzw. Astralit-Staub) unter
  `mod_loaded simplebuilding` mit gleicher Id; neu ein Vanilla-Weg nur ohne SB (`*_without_simplebuilding`): 8 + Phantomhaut (schwebend)
  bzw. 8 + Windrute (steigend). Formsäge-Rezepte der Naturstufen werden unbedingt `simpleblocks:*` (eine Zeile im Generator; Säge selbst
  unverändert).
- **E5 Ressourcen:** Blockstates/Modelle/Item-Definitionen/Beute/Rezepte/Freischaltungen per `git mv` aus SB-generated/Overlay ins Modul,
  Namensraum umgeschrieben; eigene Texturen per `git mv` (nichts neu gezeichnet; Modelle, die Vanilla-Texturen nennen, bleiben so). Tags und
  Lang im Modul-Generator (`generate_data.py`, Abschnitt `MOVED`), `check_data.py` prüft Dateien + Klassenliste. SB-Datagen 26.3 erzeugt
  für die 15 nichts mehr (`BLOCKS_IN_SB`), Datagen auf sb-test neu.
- **E6 Pflicht-Sub-Mod:** Simple Blocks 1.0.0, `bundle: required`, SB 26.3 `depends`/`required` auf `simpleblocks >=1.0.0` (Fabric
  `fabric.mod.json`-Filter, NeoForge/Forge-Toml-Filter), `implementation` + gebündelt, NeoForge-Läufe laden es (`gradle/module-neoforge.gradle`),
  Forge-Bündel als Pflicht. Schalter „Enable Simple Blocks“ gibt es schon (Registrierungen bleiben, aus = Tab/Vanilla-Tab-Einträge weg).
- **E7 Tabs (T2):** Die 15 stehen von Haus aus im Tab `simpleblocks:blocks` (Zeilen Schwerkraft, Naturstufen, Eis/Schale, Froschlichter hinter den
  eigenen Blöcken); SB-Baublockzeilen ohne die 15 (Falle `sculk_jaw` bleibt als eigene Zeile) und werden per T1 angehängt. Vanilla-Tabs:
  SB-Suchtab-Ketten setzen die Items weiter per Id (Reihenfolge mit Octets); das Modul setzt sie selbst nur ohne SB (wie Octets 18.10.),
  dafür `Placement` mit „hinter“-Anker. Wiki/Guide-Texte „SimpleBlocks“ -> „Simple Blocks“, wo der gemeinsame Tab gemeint ist.
- **E8 Weiteres:** Money (4 Preise -> `money/simpleblocks`, `generate_links.py`), Discover/Guide/Wiki/Questionary des Moduls, Testzentrale
  (Station des Moduls nennt die 15), `integration/enabled-mods.json`, `docs/MODS-UND-FEATURES.md`, Prinzipien-Tabelle (harte Abhängigkeit).

## 3. Umsetzung (Dateien)

1. Modul: `nature/{NatureBlocks, LevitatingBlock, LevitatingBlockEntity, FallingSlabBlock, MergingSlabBlock, CrackedIceBlock, GravityBlockItem}`,
   Mixins `CrackedIcePlaceMixin`, `CrackedIceFacesMixin`; `SimpleBlocks.registerIdMoves`; `BlocksRegistry` (Registrierung, Entity, Tab-Zeilen,
   Placements); Loader Fabric/NeoForge/Forge (+ Clients: Renderer, Tint); `test/NatureTests` (Verhalten aus SB-`NatureBlockTests`, Migration,
   Rezepte mit/ohne SB, Tab) + Fabric-Adapter; Daten/Lang/Wiki/Guide/Discover; Gradle (SimpleLib).
2. SB: `McVersion` (beide Linien), `util/BlockRefs`, Registrierungen/Klassen/Mixins raus bzw. gated, Nutzer auf `BlockRefs`, Datagen gated,
   Lang/Overlay/Tags/Questionary/Wiki bereinigt, `NatureBlockTests` -> nur SB-Teile (Meißel, Felder), Migrationstest um Proben erweitert.
3. Gradle/Manifest wie E6.
4. Generatoren auf sb-test: datagen 26.3, `generate_data.py`, `generate_links.py`, `module_guides.py`, `wiki/generate.py --all`, `discover.py`, Texturindex.

## 4. Risiken

- Registrier-/Init-Reihenfolge: SB darf die Blöcke nie in statischen Initialisierern nachschlagen (Fabric-Entrypoint-Reihenfolge); Meißel-Kette
  Eis -> rissiges Eis deshalb als Datenpaket-Datei (wie Simple-End-Hölzer), Hammer/Weltgen zur Laufzeit/im Datagen per Id.
- Verdeckte Kopplung über `instanceof LevitatingBlock`/`CrackedIceBlock` in SB (Tooltips, Testzentrale) -> Id-Prüfung.
- Lang-/Wiki-/Discover-Checks prüfen Ids -> nach dem Umzug neu erzeugen. 26.2 muss kompilieren.

## 5. Prüfung (sb-test)

Compile 26.3 fabric/neoforge/forge + Module + 26.2 `:compileJava`; SB-Filter `nature`, `gravity`, `block_behaviour`, `data_integrity`, `migration`,
`chisel`, `sledgehammer` (fabric-263, neoforge-263) und am Ende voll fabric-263; `module-simpleblocks-{fabric,neoforge}-263`, Standalone fabric;
`integration-263`; `./gradlew check`. Nicht geprüft (bewusst): Client-Sicht (Tint, Renderer, Tab-Bild) - Besitzer-/Client-Abnahme.

## 6. Stand (19.10.) und Abweichungen

Umgesetzt wie geplant (Stand `b-blocksmove`, mit `claude-wave1` 23b9489dd gemergt). Belege (sb-test, ce7a29ae4): fabric-263 1207/1207,
neoforge-263 1207/1207, integration-263 3/3, module-simpleblocks fabric 37/37 (Vorlauf auch neoforge + standalone-fabric je 37/37),
Compile 26.3 fabric/neoforge/forge + Forge-Modul + 26.2 `:compileJava`, `./gradlew check` grün.

Abweichungen (bewusst):
- Ids der Verschiebung in `nature/NatureIds` (ohne Blöcke): `registerIdMoves` läuft im Loader-Konstruktor, NeoForge/Forge-Registries sind dort gesperrt.
- SB-Lang behält die Schlüssel der 15 (26.2 nutzt die Schwerkraftblöcke weiter; `checkLangText` verlangt Overlay ⊇ geteilte Datei) - wie bei den Öfen.
- Migrations-Fixture (`testing/fixtures/migration-before`) unverändert; die Id-Umzüge prüft der Modultest `moved_ids_from_simplebuilding`.
- Meißel Eis -> rissiges Eis nur per Datenpaket-Datei `cracked_ice_stone_chisel.json` (Modpack-Test zählt `simpleblocks` wie `simpleend` als reine Datei-Einträge).
- SB-Suchtab setzt Achtel von Simple-Blocks-eigenen Blöcken (Blaue Netherziegel, gedämpfte Fliesen) nicht mehr: auf Fabric lief die Kette vor dem
  Einfügen der Stufe (Achtel landete am Ende). Offen für Simple Octets/Simple Blocks (Sitzung A, octets2).
- Wiki-Notizen der Schwerkraftblöcke neu und kürzer (die alten beschrieben noch das schrittweise Steigen); Text „rissiges Eis wird zu Wasser“ korrigiert.

Offen: Client-Sicht (Gras-Tint, Renderer der aufsteigenden Blöcke, Tab-Bild), Forge-Laufzeittests, Merge + Gate durch die Hauptsitzung.
