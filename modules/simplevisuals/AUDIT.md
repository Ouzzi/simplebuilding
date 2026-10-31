# Audit simplevisuals (b-rel32-aes, 31.10.2026)

Basis `b-rel32` 1be1d2873, nur MC 26.3 Fabric. Gelesen: alle Fabric- und Shared-Java-Dateien des Mods vollstaendig, dazu fabric.mod.json, Mixin-JSON, Gradle, Daten, Tools-Skript und genutzte Framework-Klassen (`SubMods`, `ConfigFiles`, `CosmeticIntensity`). Gametest-Klassen (VisualsTests) nur ueberflogen, Forge/NeoForge-Zweige nicht Teil dieses Releases (nur Dateiliste). Nichts veroeffentlicht.

Durchgesehene Dateien (76 Dateien, 4644 Zeilen; PNG/Binaerdateien nur auf Herkunft geprueft). Forge/NeoForge-Teile sind nur gelesen, nicht Teil dieses Releases (nur 26.3 Fabric):

- `LICENSE` (121)
- `clienttest/java/com/simplebuilding/modules/simplevisuals/ModuleClientSmoke.java` (37)
- `fabric/build.gradle` (9)
- `fabric/src/main/java/com/simplebuilding/modules/simplevisuals/ModuleGameTest.java` (27)
- `fabric/src/main/java/com/simplebuilding/modules/simplevisuals/VisualsFabric.java` (5)
- `fabric/src/main/java/com/simplevisuals/fabric/VisualsFabricClient.java` (15)
- `fabric/src/main/java/com/simplevisuals/fabric/VisualsModMenu.java` (2)
- `fabric/src/main/resources/fabric.mod.json` (47)
- `forge/build.gradle` (10)
- `forge/src/main/java/com/simplebuilding/modules/simplevisuals/VisualsForge.java` (10)
- `forge/src/main/java/com/simplebuilding/modules/simplevisuals/forge/ModuleForgeTests.java` (20)
- `forge/src/main/java/com/simplebuilding/modules/simplevisuals/forge/VisualsForgeClient.java` (6)
- `forge/src/main/java/com/simplebuilding/modules/simplevisuals/forge/VisualsForgeSetup.java` (6)
- `forge/src/main/java/com/simplebuilding/modules/simplevisuals/forge/mixin/RegistryLoadTaskMixin.java` (54)
- `forge/src/main/resources/META-INF/mods.toml` (45)
- `forge/src/main/resources/pack.mcmeta` (10)
- `forge/src/main/resources/simplevisuals.forge.mixins.json` (12)
- `generated/resources/README.md` (1)
- `neoforge/build.gradle` (22)
- `neoforge/src/main/java/com/simplebuilding/modules/simplevisuals/ModuleNeoTests.java` (36)
- `neoforge/src/main/java/com/simplebuilding/modules/simplevisuals/VisualsNeoForge.java` (13)
- `neoforge/src/main/java/com/simplevisuals/neoforge/VisualsNeoClient.java` (6)
- `neoforge/src/main/resources/META-INF/neoforge.mods.toml` (43)
- `shared/java/com/simplebuilding/modules/simplevisuals/ExampleItems.java` (1)
- `shared/java/com/simplevisuals/AssetBounds.java` (16)
- `shared/java/com/simplevisuals/ChiseledFrame.java` (12)
- `shared/java/com/simplevisuals/ConfigOptions.java` (49)
- `shared/java/com/simplevisuals/FrameChisel.java` (43)
- `shared/java/com/simplevisuals/FrameTilt.java` (83)
- `shared/java/com/simplevisuals/SpeedLinesMath.java` (13)
- `shared/java/com/simplevisuals/Visuals.java` (40)
- `shared/java/com/simplevisuals/VisualsCommands.java` (15)
- `shared/java/com/simplevisuals/client/ClientAssertions.java` (67)
- `shared/java/com/simplevisuals/client/Immersion.java` (106)
- `shared/java/com/simplevisuals/client/LocalCommands.java` (50)
- `shared/java/com/simplevisuals/client/MapPreview.java` (12)
- `shared/java/com/simplevisuals/client/ModelBrowser.java` (27)
- `shared/java/com/simplevisuals/client/RenamedModels.java` (93)
- `shared/java/com/simplevisuals/client/TiltedFrameState.java` (18)
- `shared/java/com/simplevisuals/client/VisualsConfigScreen.java` (46)
- `shared/java/com/simplevisuals/client/VisualsHud.java` (154)
- `shared/java/com/simplevisuals/config/SimplevisualsConfig.java` (177)
- `shared/java/com/simplevisuals/effects/EffectRegistry.java` (21)
- `shared/java/com/simplevisuals/effects/Intensity.java` (15)
- `shared/java/com/simplevisuals/effects/ParticleBudget.java` (14)
- `shared/java/com/simplevisuals/guide/VisualsGuide.java` (59)
- `shared/java/com/simplevisuals/mixin/AnvilMenuMixin.java` (7)
- `shared/java/com/simplevisuals/mixin/AnvilScreenMixin.java` (20)
- `shared/java/com/simplevisuals/mixin/ChatMixin.java` (45)
- `shared/java/com/simplevisuals/mixin/ClientTooltipMixin.java` (7)
- `shared/java/com/simplevisuals/mixin/EffectBarsMixin.java` (11)
- `shared/java/com/simplevisuals/mixin/HudMixin.java` (11)
- `shared/java/com/simplevisuals/mixin/InventoryEffectBarsMixin.java` (9)
- `shared/java/com/simplevisuals/mixin/ItemFrameChiselMixin.java` (78)
- `shared/java/com/simplevisuals/mixin/ItemFrameRenderStateMixin.java` (30)
- `shared/java/com/simplevisuals/mixin/ItemFrameRendererMixin.java` (94)
- `shared/java/com/simplevisuals/mixin/ItemModelMixin.java` (7)
- `shared/java/com/simplevisuals/mixin/LocatorMixin.java` (20)
- `shared/java/com/simplevisuals/mixin/MinecraftMixin.java` (7)
- `shared/java/com/simplevisuals/mixin/PackRepositoryAccessor.java` (7)
- `shared/java/com/simplevisuals/mixin/PickupMixin.java` (8)
- `shared/java/com/simplevisuals/mixin/TooltipMixin.java` (12)
- `shared/java/com/simplevisuals/test/VisualsTests.java` (209)
- `shared/resources/assets/simplevisuals/cit/test_stick.json` (10)
- `shared/resources/assets/simplevisuals/cit/test_stick_2.json` (7)
- `shared/resources/assets/simplevisuals/effects.json` (74)
- `shared/resources/assets/simplevisuals/icon.layers.json` (28)
- `shared/resources/assets/simplevisuals/lang/de_de.json` (256)
- `shared/resources/assets/simplevisuals/lang/en_us.json` (256)
- `shared/resources/data/simplevisuals/structure/empty.nbt` (19)
- `shared/resources/data/simplevisuals/tags/item/frame_chisels.json` (8)
- `shared/resources/simplevisuals.mixins.json` (28)
- `tools/__pycache__/config_schema.cpython-313.pyc` (38)
- `tools/check_data.py` (38)
- `tools/config_schema.py` (27)
- `wiki/manual.json` (1585)

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Code | HUD/Immersion sind mit harten Grenzen gebaut (Partikelbudget 24/Tick, 4/Spieler, 8 Spieler; max. 8 Pickup-Zeilen, 32 Schadenszahlen, Cache 64; Regelzahl/Dateigroesse/PNG-Header vor dem Dekodieren geprueft). Client-Klassen nur in `client/` und Client-Mixins; `Visuals`, `FrameChisel`, `FrameTilt`, `AnvilMenuMixin`, `ItemFrameChiselMixin` sind serverfaehig und frei von `net.minecraft.client` (vom Check-Skript erzwungen). Kaputte Config wird jetzt quarantaeniert (P3-1 gefixt). |
| C2 Mixins (17) | Server/Common: `AnvilMenuMixin` (`validateName` HEAD, nur wenn Schalter an; Konflikt moeglich mit anderen Amboss-Namens-Mods, die dieselbe Methode umbiegen), `ItemFrameChiselMixin` (zusaetzliches synchronisiertes Byte am Item-Rahmen). Client: `HudMixin`, `MinecraftMixin` (tick, onResourceLoadFinished), `PickupMixin`, `ChatMixin` (`@ModifyVariable addMessage` + Kopf im Chat-Rand; doppelt zu Chat-Head-Mods), `AnvilScreenMixin`, `TooltipMixin` (ItemStack.getTooltipImage, Karten-Vorschau), `ItemModelMixin` (`ItemModelResolver.appendItemLayers`, wie simplemodels - beide `@ModifyVariable`, verkettbar), `LocatorMixin` (Lambda `lambda$extractRenderState$1` - bricht bei Mojang-Umbau, `require 1`), `ClientTooltipMixin`, `EffectBarsMixin`/`InventoryEffectBarsMixin` (`@WrapOperation`, kooperativ), `PackRepositoryAccessor`, `ItemFrameRendererMixin` (Injektionen mit `ordinal` 4 / 1 in `submit`: fragil gegen Mods, die Rahmen-Rendering aendern), `ItemFrameRenderStateMixin`. Sodium/Iris: keine Terrain-/Chunk-/Shader-Mixins; alles sitzt im GUI-/Entity-Zustand (`ItemFrameRenderer` ist das einzige Entity-Rendering; Iris zeichnet es ueber denselben Submit-Pfad, ungetestet mit Shader - Rueckfrage/Abnahme QOLA-VIS-9). Echter Client-Start (`module-simplevisuals-client-263`, `mixinaudit-client-263`) ist Teil der Abnahme. |
| C3 Config | 51 Optionen mit Name/Tooltip/Tab und "Default"-Text in EN/DE, alle Zahlen geklemmt (`ConfigOptions.normalize`, Test `configBoundsAndLegacyKeys`), Client-Befehle `/simplevisuals config ...`, serverseitig nur `enableAnvilFormatting` (Permission Gamemaster). |
| C4 Daten | `effects.json` (12), Tag `frame_chisels` (optional `#simplebuilding:chisel_tools`), Lang EN/DE 256/256 Keys, Wiki-Kapitel 78. `check_data.py` gruen (lokal). Zwei Test-CIT-Regeln (`cit/test_stick*.json`: Stock "JsonTest" -> Smaragd/Diamant) liegen im Jar; wirkungslos, solange `enableRenamedItemTextures=false` (Default) - P3-2. |
| C5 Unabhaengigkeit | allein spielbar; `modelsLoaded`-Weiche gegen doppelte Umbenennungs-Modelle; Rahmen-Meissel braucht SimpleBuilding fuer das Werkzeug-Tag (sonst Funktion ohne Werkzeug inaktiv). Kein `simplelib` in `depends` obwohl eingebettet - harmlos. |
| C6 A12 | HUD/Partikel rein lokal. Eingriffe in die Welt: Amboss-Farbcodes (Schalter + Server-Befehl), Meissel-Neigung am Rahmen (Option `enableTiltedFrameItems`, Standard an). |
| C7 Netzwerk | keine eigenen Pakete. Server vertraut dem Client nichts: Amboss-Name wird serverseitig validiert (Laenge 50, Steuerzeichen raus, nur gueltige Codes). Rahmen-Meissel prueft `mayBuild`, `fixed`, Tag. Vanilla-Client: das zusaetzliche synchronisierte Byte am Rahmen waere fuer einen Client ohne den Mod ein unbekannter Daten-Index (Vanilla-Client wirft dort eine Ausnahme); wirksam erst, wenn der Rahmen gemeisselt wird, und das braucht SimpleBuilding-Werkzeuge, die einen Mod-Client voraussetzen (P3-3, Hinweis). |
| C8 Lizenz/Assets | `LICENSE` (CC0) vorhanden; Icon/Layer selbst; keine Audio, keine fremden Texturen im Jar (nur Icon). Keine Pfade/Secrets. Zielverzeichnisse `config/simplevisuals/...` relativ zum Spielordner. |
| C9 Offen | kein TODO/FIXME. |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simplevisuals-P2-1 | P2 | `depends cloth-config` gilt auch auf dem Dedicated Server (Anvil-Farben/Rahmen sind Server-Funktionen), Server-Admin muss Cloth Config nachinstallieren | Rueckfrage (siehe simplesounds-P2-1) |
| simplevisuals-P3-1 | P3 | `Visuals.initialize` ueberschrieb eine kaputte `simplevisuals.json` beim naechsten Speichern ohne Sicherung | **gefixt** (`ConfigFiles.readOrQuarantine`) |
| simplevisuals-P3-2 | P3 | Testfixtures `cit/test_stick*.json` im Release-Jar | Hinweis (Client-Test braucht sie; spaeter in Testressourcen) |
| simplevisuals-P3-3 | P3 | Synchronisiertes Rahmen-Byte: Vanilla-Client ohne Mod vertraegt es nicht, sobald gemeisselt | Hinweis, Release-Notiz "Mod auch auf dem Client" |
| simplevisuals-P3-4 | P3 | `LocatorMixin` zielt auf Lambda-Namen, `ItemFrameRendererMixin` auf `ordinal`-Treffer; beide `require 1` (Spielende bei Mojang-Patch), im Gegensatz zu den Opt-Mods | Rueckfrage: wie Opt auf `required:false`/`defaultRequire:0` umstellen? |
| simplevisuals-P3-5 | P3 | Schadens-Partikel/-Zahl fuer alle Lebewesen im Umkreis 16 (bewusst), Immersion-Tick iteriert bis 128 Entitaeten | Hinweis, Kosten klein |

## Urteil
**shipbar** (0 P1, 1 P2 als Rueckfrage, 5 P3; 1 gefixt). Offen vor Veroeffentlichung: echter Client-Lauf (Smoke + `mixinaudit-client-263`) und ein Sichtlauf mit Sodium und Iris.

## Testergebnis
Lauf 31.10. auf sb-test (Commit ea41423fb, 1 sbt-Aufruf, Rerun fuer ein Ziel):
- C=0: Compile `:mc26_3:fabric` + vier Modul-Compiles gruen.
- T: Gametests 129/129 gruen (0 rot): Models integration 21/21, standalone 21/21; Visuals 22/22, 22/22; Sounds 20/20, 20/20; Aesthetics standalone 3/3. `Aesthetics integration` brach im ersten Lauf nach 9 s mit einem Gradle-Abbruch ab, ohne Tests zu starten (kein Testfehler, vermutlich Gradle-Konflikt in der geteilten Schlange); der Einzel-Rerun `:integration:runSimpleaestheticsGameTest` lief gruen (3/3, "All 3 required tests passed", BUILD SUCCESSFUL).
- K=0: Client-Smokes unter `xvfb-run -a`: Models 10/10, Visuals 6/6, Sounds 3/3, Aesthetics 1/1 (echter Client, Welt betreten, Screens fotografiert). Hinweis des Laufs: je 1659 "Fehlende Textur/Modell"-Meldungen im Client-Log, in allen vier Logs gleich (stammt vermutlich aus dem gemeinsamen Spiel/Vanilla-Umfeld; nicht ausgewertet).
- M=0: `check_client_mixins.py`: 660 Mixins geprueft, 0 Probleme (inkl. aller 17 Visuals-, 7 Models-, 1 Sounds-Mixins).
- L=0: `check_lang_keys.py`: 52 Module, jeder Key EN und DE.
Nicht getestet: Sodium/Iris, Forge/NeoForge, ein Lauf von `mixinaudit-client-263` (Hauptsitzung nach Merge).
