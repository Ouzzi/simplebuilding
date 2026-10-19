# Plan b-s8-storage (19.10.2026): Familienstufe 8 – Lager nach `simplestorage`, Teil 8a

Branch `b-s8-storage` (von `claude-wave1` a936c9c5c), Sitzung B, Queue-Zeile 93. Grundlage: `PLAN-MODULAUFTEILUNG-2026-10-11.md`
(§1.2 Storage, §1.4, §2.2 Simple Storage, Stufe 8, F3), Vorbilder Stufe 4 Ovens, 5 Crucibles (Menüs/BE/Renderer je Loader),
7 Hammers (`PLAN-S7-HAMMERS-2026-10-18.md`), b-blocksmove (`PLAN-B-BLOCKSMOVE-2026-10-18.md`), `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`.
Modul-Id `simplestorage` (Name „Simple Storage“) wie im Aufteilungsplan (§2.1/§2.2, Besitzer-Entscheidung „neue Namespaces“).

## 1. Ist-Zustand (git grep, Stand a936c9c5c)

Storage in SB = ~110 Klassen in sieben Gruppen, sehr unterschiedlich gekoppelt (Dateien außerhalb der Gruppe, die sie nennen):

| Gruppe | Klassen | Kopplung | 26.2 |
|---|---|---|---|
| Kisten (`woodwork/Crate*`, 12 + 2 End-Hölzer) | 4 | ~25 Dateien | nie (`WOODWORK=false`) |
| Lager-Werkbank (`StorageCrafting*`) | 5 | ~30 Dateien | nie (`STORAGE_CRAFTING_TABLE=false`) |
| Stufen-Truhen, Fallen-/Kupfer-Fallentruhen, Shulker-Stufen, Hüllen, seltene Funde | ~30 + 10 Mixins | ~110 Dateien (Trichter, Hammer-Tabelle, Fahrzeuge, Astral/Nihil, ContainerCounts, Tooltips) | ja |
| Kisten-Boote/-Loren (`TieredChest*`-Fahrzeuge) | 4 | hängt an Truhen-Menü + `VehicleTiers` | ja |
| Rucksäcke (Block + Item, Menü, Taste, Layer) | ~15 | ~105 Dateien | ja |
| Verstärkte Bündel, platziertes Bündel, Köcher | ~15 | ~100 Dateien; **b-bundles/b-tooltips arbeiten gerade an Bündel-/Container-Tooltips** | ja |
| Lager-Verzauberungen, Färben | Daten + Utils | quer über alles | ja |

Das Enderit-Fass ist schon in `simplecrucibles` (Stufe 5).

## 2. Entscheidungen (selbst getroffen)

- **E1 Teilung (Auftrag: lieber 8a vollständig grün als alles halb).** 8a = **Kisten + Lager-Werkbank** (15 Blöcke inkl. End-Kisten,
  2 BE, 1 Menü, 1 Screen, 2 BE-Renderer, 3 Texturen). Begründung: beide Gruppen sind 26.3-only (kein 26.2-Zwilling nötig, Klassen
  ziehen ganz um), fachlich „Lager“ (Plan §2.2), und 8a baut das ganze Modul-Gerüst (3 Loader, Pflicht-Einbindung, IdMoves, Tab,
  Guide, Wiki, Discover, Teststation, Tests), auf das 8b/8c nur noch Inhalt legen. **8b** = Truhen/Fallentruhen/Kupfer-Fallentruhen/
  Shulker (+ Hüllen, seltene Funde, Kisten-Boote/-Loren, Lager-Verzauberungen, `McVersion.STORAGE_IN_SB` + 26.2-Zwillinge wie
  Stufe 7). **8c** = Bündel/Rucksäcke/Köcher/platziertes Bündel – erst nach Merge von b-bundles/b-tooltips (sonst Konflikte in
  denselben Mixins/Tooltips).
- **E2 Ids:** `simplebuilding:<x>` → `simplestorage:<x>` per SimpleLib `IdMoves` (Loader-Konstruktor, Forge-Aliase automatisch):
  BLOCK + ITEM der 14 Kisten (`<holz>_crate`) und `storage_crafting_table`; BLOCK_ENTITY_TYPE `crate`, `storage_crafting_table`;
  RECIPE + Rezept-Advancement (`recipes/decorations/<id>`) je Kiste und Werkbank. Menütyp nur neu registriert (nie gespeichert).
- **E3 SB:** Klassen werden aus SB entfernt (26.2 hatte sie nie). `WoodBlocks.Family` ohne Kiste. SB erreicht beide per Id
  (`util/StorageRefs`, Muster `OvenRefs`; `McVersion.STORAGE_NAMESPACE` 26.2 `simplebuilding`, 26.3 `simplestorage`): SB-Tabs
  (Holzwerk-Zeilen, Funktionsblöcke), Suchtab-Platzierung, Testzentrale, Guide-Verweise. Flag `STORAGE_CRAFTING_TABLE` entfällt,
  wenn keine Nutzer bleiben. Datagen 26.3 liefert für die 15 nichts mehr; Overlay-/Generated-Dateien per `git mv` ins Modul
  (Namensraum umgeschrieben). Lang-Schlüssel in SB (src/main + Overlay) entfallen (26.2 nutzt sie nicht), Modul-Lang EN/DE.
- **E4 Modul:** `modules/simplestorage` 1.0.0, `bundle: required`, Pflicht `simplelib` (IdMoves, UI-Bausteine), optional
  `simplebuilding`, `simpleend` (End-Kisten nur mit Simple End wie heute). Pakete `com.simplestorage.{crate,table,registry,client,
  guide,discover,test,mixin}`. Kisten brennen (Feuer-Invoker-Mixin des Moduls) und sind Brennstoff (300, nicht Nether-Holz) wie
  bisher. Werkbank-Screen im Container-Stil über SimpleLib `api/client/ui` (wie SBs `ModScreenStyle`, Palette Eiche).
- **E5 Schalter:** „Enable Simple Storage“ in SB (`enableSimpleStorage`, `SubMods`, wie Hämmer/Octets): aus = Guide-Buch und
  Vanilla-Tab-Einträge weg, Blöcke bleiben registriert und benutzbar (nie Datenverlust). Keine eigene Modul-Config (YAGNI).
- **E6 Tests:** die 4 Kisten-Tests aus `WoodworkTests` und die 3 Werkbank-Tests aus `BrewingTests` ziehen in `StorageTests` des
  Moduls (+ `registered`, `moved_ids_from_simplebuilding`, `recipes`, `burns_and_fuels`, `tab`, Discover), Fabric-Adapter
  `ModuleGameTest`, NeoForge/Forge-Adapter. SB-Tests (DataIntegrity-Tabzeilen, Woodwork-Familie, Migrationswelt), simpleend
  `partner_wood_features` (Kisten-Ids jetzt `simplestorage:`) folgen.
- **E7 Weiteres:** Pflicht-Einbindung Gradle (Fabric include/implementation, NeoForge jarJar + loadedMods, `module-neoforge.gradle`,
  Forge-Bündel), `depends` in fabric.mod.json/neoforge/forge-Toml-Filtern, `modules.json` (SB requires), `integration/enabled-mods.json`;
  Guide-Buch des Moduls (`tools/guides/module_guides.py`), SB-Lagerbuch ohne Kisten-Kapitel; Discover (`tools/discover/discover.py`);
  Wiki `modules/simplestorage/wiki/manual.json` (Kapitel aus SB), Questionary-Einträge, Teststation (framework `TestCentreStations`),
  Generatoren (`woodwork_2026_10_09.py`, `endwood_data_2026_10_12.py`, `brew_textures.py`) schreiben ins Modul, `docs/MODS-UND-FEATURES.md`,
  Mod-Übersicht.

## 3. Umsetzung (Commits)

1. Plan (dieses Dokument).
2. Modul-Gerüst (`tools/newmod.py`) + Inhalt (Klassen, Ressourcen per `git mv`, Lang, Tests, Adapter, Guide, Discover, Wiki).
3. SB umstellen (Refs, Registrierungen/Klassen raus, Tabs, Datagen, Lang, Tests) + Gradle/Manifest.
4. Fremdmodule (simpleend-Test), Generatoren, Doku.
5. Erzeugen auf sb-test: Datagen 26.3, `module_guides.py`, `discover.py`, `wiki/generate.py --all`.

## 4. Risiken

- Fabric-Entrypoint-Reihenfolge: SB darf die Modul-Blöcke nie in statischen Initialisierern nachschlagen (nur zur Laufzeit/Tab-Bau).
- End-Kisten: Registrierung nur mit Simple End (wie heute), Vorlage Eiche (End-Hölzer evtl. später registriert).
- Werkbank-Menü: `stillValid` prüft den eigenen Block; Rezeptbuch/Vanilla-`slotChangedCraftingGrid` bleibt gleich.
- Checks (`checkLangText`, `checkModuleData`, Wiki, Discover, Guides) kennen neue Ids erst nach dem Erzeugen.

## 5. Prüfung (sb-test)

Compile 26.3 fabric/neoforge/forge + 26.2 `:compileJava`; `module-simplestorage-{fabric,neoforge}-263`, Standalone fabric;
SB-Filter `woodwork`, `brewing`, `data_integrity`, `migration` (fabric-263, neoforge-263); simpleend; forge-263 gezielt; `./gradlew check`.
Nicht geprüft (bewusst, Auftrag): Client-Läufe (Kisten-Renderer, Werkbank-Screen, Tab-Bild).

## 6. Stand und Abweichungen

Umgesetzt wie geplant (8a). Belege (sb-test, 8bc5bd991 + Erzeugung): fabric-263 1201/1201, neoforge-263 1201/1201,
module-simplestorage fabric/neoforge/forge/standalone-fabric je 16/16, module-simpleend-fabric 36/36, forge-263 `*woodwork*` 7/7 und
`*data_integrity*` 51/51, Compile 26.3 fabric/neoforge/forge + 26.2 `:compileJava`, `./gradlew check` Exit 0, Datagen 26.3 (nur axe-Tag, wiki/items).

Abweichungen (bewusst):
- `McVersion.STORAGE_CRAFTING_TABLE` bleibt (heißt jetzt „gibt es auf dieser Linie“, 26.2 false); `StorageRefs` liefert per Id.
- Das Kisten-Kapitel im SB-Lagerbuch bleibt (per `STORAGE_NAMESPACE`-Id), das Modul hat zusätzlich ein eigenes Handbuch.
- Werkbank-/Kisten-Bilder der SB-Client-Tests: Werkbank-Teil in den Client-Smoke des Moduls verschoben, Kisten-Teil per Id/Container.
- Tag `simplebuilding:crate_storable` heißt jetzt `simplestorage:crate_storable` (Datapacks mit dem alten Tag müssen nachziehen).
- Wiki-Renders der Kisten/Werkbank entfallen in SB (Generator); im Modul-Wiki keine eigenen Renders.

Offen: 8b, 8c; Client-Sicht (Kisten-Renderer, Werkbank-Fenster, Tab), Forge-Volllauf, Merge + Gate durch die Hauptsitzung.
