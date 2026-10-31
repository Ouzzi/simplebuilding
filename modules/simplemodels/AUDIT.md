# Audit simplemodels (b-rel32-aes, 31.10.2026)

Basis `b-rel32` 1be1d2873, nur MC 26.3 Fabric. Gelesen: alle Fabric- und Shared-Java-Dateien des Mods vollstaendig, dazu fabric.mod.json, Mixin-JSON, Gradle, Daten, Tools-Skript und genutzte Framework-Klassen (`SubMods`, `ConfigFiles`, `CosmeticIntensity`). Gametest-Klassen (ModelTests) nur ueberflogen, Forge/NeoForge-Zweige nicht Teil dieses Releases (nur Dateiliste). Nichts veroeffentlicht.

Durchgesehene Dateien (56 Dateien, 2704 Zeilen; PNG/Binaerdateien nur auf Herkunft geprueft). Forge/NeoForge-Teile sind nur gelesen, nicht Teil dieses Releases (nur 26.3 Fabric):

- `clienttest/java/com/simplebuilding/modules/simplemodels/ModuleClientSmoke.java` (190)
- `examples/catalogue/tomato.json` (11)
- `examples/resourcepack/assets/renamed/items/tomato.json` (6)
- `fabric/build.gradle` (6)
- `fabric/src/main/java/com/simplebuilding/modules/simplemodels/FabricModelsClient.java` (9)
- `fabric/src/main/java/com/simplebuilding/modules/simplemodels/ModelsFabric.java` (16)
- `fabric/src/main/java/com/simplebuilding/modules/simplemodels/ModuleGameTest.java` (26)
- `fabric/src/main/resources/fabric.mod.json` (45)
- `forge/build.gradle` (10)
- `forge/src/main/java/com/simplebuilding/modules/simplemodels/forge/ModelsForge.java` (20)
- `forge/src/main/java/com/simplebuilding/modules/simplemodels/forge/ModelsForgeClient.java` (7)
- `forge/src/main/java/com/simplebuilding/modules/simplemodels/forge/ModuleForgeTests.java` (20)
- `forge/src/main/java/com/simplebuilding/modules/simplemodels/forge/mixin/RegistryLoadTaskMixin.java` (54)
- `forge/src/main/resources/META-INF/mods.toml` (38)
- `forge/src/main/resources/pack.mcmeta` (10)
- `forge/src/main/resources/simplemodels.forge.mixins.json` (12)
- `neoforge/build.gradle` (22)
- `neoforge/src/main/java/com/simplebuilding/modules/simplemodels/ModelsNeoClient.java` (14)
- `neoforge/src/main/java/com/simplebuilding/modules/simplemodels/ModelsNeoForge.java` (25)
- `neoforge/src/main/java/com/simplebuilding/modules/simplemodels/ModuleNeoTests.java` (15)
- `neoforge/src/main/resources/META-INF/neoforge.mods.toml` (32)
- `shared/java/com/simplebuilding/modules/simplemodels/CataloguePayload.java` (13)
- `shared/java/com/simplebuilding/modules/simplemodels/ModelCatalogue.java` (88)
- `shared/java/com/simplebuilding/modules/simplemodels/ModelDefinition.java` (53)
- `shared/java/com/simplebuilding/modules/simplemodels/ModelPolicy.java` (21)
- `shared/java/com/simplebuilding/modules/simplemodels/ModelQuery.java` (42)
- `shared/java/com/simplebuilding/modules/simplemodels/ModelTests.java` (272)
- `shared/java/com/simplebuilding/modules/simplemodels/Models.java` (61)
- `shared/java/com/simplebuilding/modules/simplemodels/client/ModelBrowser.java` (233)
- `shared/java/com/simplebuilding/modules/simplemodels/client/ModelPreviewTooltip.java` (27)
- `shared/java/com/simplebuilding/modules/simplemodels/client/ModelsTab.java` (80)
- `shared/java/com/simplebuilding/modules/simplemodels/discover/ModelsDiscover.java` (255)
- `shared/java/com/simplebuilding/modules/simplemodels/guide/ModelsGuide.java` (137)
- `shared/java/com/simplebuilding/modules/simplemodels/mixin/ModelAnvilMixin.java` (44)
- `shared/java/com/simplebuilding/modules/simplemodels/mixin/ModelContainerAccessor.java` (9)
- `shared/java/com/simplebuilding/modules/simplemodels/mixin/ModelRecipeBookAccessor.java` (10)
- `shared/java/com/simplebuilding/modules/simplemodels/mixin/ModelRenderPolicyMixin.java` (12)
- `shared/java/com/simplebuilding/modules/simplemodels/mixin/ModelScreenMixin.java` (40)
- `shared/java/com/simplebuilding/modules/simplemodels/mixin/ModelTooltipInvoker.java` (15)
- `shared/resources/assets/simplemodels/icon.layers.json` (82)
- `shared/resources/assets/simplemodels/items/guide_book.json` (6)
- `shared/resources/assets/simplemodels/lang/de_de.json` (87)
- `shared/resources/assets/simplemodels/lang/en_us.json` (87)
- `shared/resources/assets/simplemodels/models/item/guide_book.json` (6)
- `shared/resources/assets/simplemodels/questionary/de_de.json` (13)
- `shared/resources/assets/simplemodels/questionary/en_us.json` (13)
- `shared/resources/assets/simplemodels/textures/item/guide_book.layers.json` (28)
- `shared/resources/data/simplemodels/advancement/recipes/misc/guide_book.json` (43)
- `shared/resources/data/simplemodels/ftbquests/chapters/simplemodels_start.json5` (41)
- `shared/resources/data/simplemodels/ftbquests/install.txt` (6)
- `shared/resources/data/simplemodels/ftbquests/lang/en_us/chapters/simplemodels_start.json5` (10)
- `shared/resources/data/simplemodels/recipe/guide_book.json` (12)
- `shared/resources/data/simplemodels/structure/empty.nbt` (19)
- `shared/resources/simplemodels.mixins.json` (1)
- `tools/check_data.py` (42)
- `wiki/manual.json` (208)

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Code | Server-eigener Katalog (`config/simplemodels/catalog/*.json`), strikte Pruefung jeder Definition (Laenge, Registry, `safeId` ohne `..`/`\`/Absolutpfad, JSON-Tiefe 16, max. 256 Dateien, 16 KiB, 64 Modelle, Symlinks verboten). Fehler im Katalog oeffnen nichts: Fehlerzustand = Modul aus (fail-closed). Amboss-Zuweisung kopiert nur das Eingabe-Item und setzt nur `item_model`. Client-Klassen nur in `client/` und Client-Mixins. |
| C2 Mixins (7) | Common: `ModelAnvilMixin` (`AnvilMenu.createResult` HEAD nur bei Anfrage `@model:...`, `mayPickup` HEAD prueft nach, Prioritaet 1100; andere Amboss-Mods (Reparatur, Aufladen) bleiben unberuehrt). Client: `ModelScreenMixin` (auf `Screen`: `init(II)V`, `rebuildWidgets()V`, `extractBackground` TAIL - Tab fuer Inventar/Kreativ/Amboss; Konflikte mit Inventar-UI-Mods vor allem visuell: Tab links am Rand), `ModelRenderPolicyMixin` (`ItemModelResolver.appendItemLayers`, teilt das Ziel mit simplevisuals `ItemModelMixin`, beide verkettbar), `ModelContainerAccessor`, `ModelRecipeBookAccessor`, `ModelTooltipInvoker`. Alles GUI/Item-Modell, nichts Sodium/Iris-Relevantes. |
| C3 Config | Server-Policy `config/simplemodels/config.json` (enabled, operatorsOnly=true, allowModItems=false, legacyNameMatching=false, levelCost 1..10, maxModels 1..64, maxFileBytes 256..16384) hart geklemmt, im Browser nur lesbar angezeigt, Lang EN/DE mit Defaults (`check_data.py` gruen). Defaults sind restriktiv: normale Spieler koennen **nichts** zuweisen, solange `operatorsOnly` an ist (Absicht). |
| C4 Daten | Rezept + Advancement Fuehrungsbuch, Items-/Model-JSON, FTB-Kapitel, `examples/` (Tomate), Lang EN/DE, `check_data.py` gruen. |
| C5 Unabhaengigkeit | allein spielbar (Standalone-Target vorhanden); `provides: renamed`; Rezept nur Vanilla-Zutaten. |
| C6 A12 | Vanilla-Amboss wird nur bei ausdruecklicher Anfrage umgeleitet; reiner `item_model`-Austausch, Identitaet/Name/Komponenten bleiben; Server kann komplett ausschalten. |
| C7 Netzwerk | Server -> Client einmaliger Katalogabgleich (`CataloguePayload`, nur an Clients mit Kanal, Join + `/simplemodels reload`); Client -> Server nur das Vanilla-Umbenennungspaket, Server prueft Berechtigung, Basis-Item, Katalog, Kosten, Nachpruefung beim Entnehmen. Vanilla-Client ohne Mod: bekommt nichts, Amboss normal. Befund P2-1 (Nutzlastgrenze) gefixt. |
| C8 Lizenz/Assets | CC0; Icon + Buch-Textur als Ebenen selbst gezeichnet; `examples/` nur Beispiel-JSON; keine fremden Assets. Kein `LICENSE`-Datei im Ordner (P3-1). |
| C9 Offen | kein TODO. `guide_book` ist das einzige Item. |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simplemodels-P2-1 | P2 | `CataloguePayload.current()` serialisierte den Katalog mit eingerueckter Ausgabe; die Serverbudget-Pruefung (24000 Zeichen) zaehlt die Eintraege einzeln, im Snapshot kommt je Zeile Einrueckung dazu. Bei vielen Modellen mit vielen Tags kann der Snapshot ueber das Paketlimit 28000 wachsen -> `writeUtf` wirft beim Join. | **gefixt** (kompaktes JSON; Test `launch` prueft die Grenze weiter) |
| simplemodels-P3-1 | P3 | Kein `LICENSE` im Modulordner | Hinweis |
| simplemodels-P3-2 | P3 | Zu viele Katalogdateien (>256) schalten das ganze Modul aus (fail-closed, im Log) | Hinweis (Absicht) |
| simplemodels-P3-3 | P3 | Jeder Spieler, der im Amboss exakt `@model:...` tippt, bekommt keine Amboss-Ausgabe (Anfrage wird abgefangen, auch bei abgeschaltetem Katalog) | Hinweis |
| simplemodels-P3-4 | P3 | Version 0.1.0, kein `ext.moduleVersion` | Rueckfrage Versionsschema |
| simplemodels-P3-5 | P3 | `ModelsDiscover` generierter Code mit leeren Tip-Listen (tote Teile) | Hinweis (Generator) |

## Urteil
**shipbar** (0 P1, 1 P2 gefixt, 5 P3). Hinweis fuer die Beschreibung: "Zuweisen braucht standardmaessig OP-Rechte".

## Testergebnis
Lauf 31.10. auf sb-test (Commit ea41423fb, 1 sbt-Aufruf, Rerun fuer ein Ziel):
- C=0: Compile `:mc26_3:fabric` + vier Modul-Compiles gruen.
- T: Gametests 129/129 gruen (0 rot): Models integration 21/21, standalone 21/21; Visuals 22/22, 22/22; Sounds 20/20, 20/20; Aesthetics standalone 3/3. `Aesthetics integration` brach im ersten Lauf nach 9 s mit einem Gradle-Abbruch ab, ohne Tests zu starten (kein Testfehler, vermutlich Gradle-Konflikt in der geteilten Schlange); der Einzel-Rerun `:integration:runSimpleaestheticsGameTest` lief gruen (3/3, "All 3 required tests passed", BUILD SUCCESSFUL).
- K=0: Client-Smokes unter `xvfb-run -a`: Models 10/10, Visuals 6/6, Sounds 3/3, Aesthetics 1/1 (echter Client, Welt betreten, Screens fotografiert). Hinweis des Laufs: je 1659 "Fehlende Textur/Modell"-Meldungen im Client-Log, in allen vier Logs gleich (stammt vermutlich aus dem gemeinsamen Spiel/Vanilla-Umfeld; nicht ausgewertet).
- M=0: `check_client_mixins.py`: 660 Mixins geprueft, 0 Probleme (inkl. aller 17 Visuals-, 7 Models-, 1 Sounds-Mixins).
- L=0: `check_lang_keys.py`: 52 Module, jeder Key EN und DE.
Nicht getestet: Sodium/Iris, Forge/NeoForge, ein Lauf von `mixinaudit-client-263` (Hauptsitzung nach Merge).
