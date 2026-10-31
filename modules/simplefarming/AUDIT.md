# AUDIT Simple Farming (`simplefarming`) – Release-Audit b-rel32-qolb, 31.10.2026

Basis: b-rel32 d9cda1e21, Branch `b-rel32-qolb`. Umfang: 1021 Zeilen Java (davon ca. 86 Tests), 3 Mixins. Nur MC 26.3 Fabric. Pruefliste C1–C9 aus `docs/ai/RELEASE-QOL-OPT-AES.md`.

## Durchgesehene Dateien (jede Datei vollstaendig gelesen)

Java (Fabric 26.3 + shared; neoforge/forge-Loader-Klassen nicht Teil des Auftrags, nur NUR-Fabric):
- `clienttest/java/com/simplebuilding/modules/simplefarming/ModuleClientSmoke.java` (9 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simplefarming/FarmingFabric.java` (29 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simplefarming/FarmingFabricClient.java` (13 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simplefarming/FarmingModMenu.java` (9 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simplefarming/ModuleGameTest.java` (20 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/FarmingCommands.java` (32 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/FarmingConfig.java` (30 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/FarmingScreen.java` (44 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/FarmingTests.java` (86 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/HoeHarvestHandler.java` (128 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/HoeHarvestHint.java` (24 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/InteractionGuard.java` (16 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/Protection.java` (7 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/SimpleFarming.java` (147 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/VegetationUtil.java` (48 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/mixin/FarmlandTrampleMixin.java` (6 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/mixin/GrassOutlineMixin.java` (68 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/mixin/SharpnessGrassCutMixin.java` (69 Zeilen)
- `shared/java/com/simplebuilding/modules/simplefarming/network/ConfigSyncPayload.java` (18 Zeilen)

Ressourcen/Daten (Zeilen):
- `fabric/src/main/resources/fabric.mod.json` (48)
- `shared/resources/assets/simplefarming/icon.layers.json` (100)
- `shared/resources/assets/simplefarming/lang/de_de.json` (19)
- `shared/resources/assets/simplefarming/lang/en_us.json` (19)
- `shared/resources/simplefarming.mixins.json` (13)
- 2 PNG-Dateien (Texturen/Icons, aus `*.layers.json` erzeugt; Pixelgroesse geprueft durch check_mod_icons / check_missing_textures)
- `fabric/build.gradle`, `wiki/manual.json`, `tools/check_data.py`


## Ergebnis C1–C9

- **C1** Alle Klassen gelesen. Hacken-Ernte, Schaerfe-Schnitt und Farmland-Schutz laufen serverseitig (`level.isClientSide()`-Wache); der Client nutzt die per `config_sync` gelieferten Werte nur fuer Vorhersage (Umriss, Hand-Hinweis). `InteractionGuard.LAST` ist eine WeakHashMap, nur Server-Haupt-Thread. Befund farming-P3-1 (gefixt), -2.
- **C2** FarmlandTrample (MixinExtras `@ModifyExpressionValue` auf `getBbHeight()` in `FarmlandBlock.fallOn`, `require=0`: faellt still aus, wenn Vanilla das Ziel aendert – der Test `farmland` prueft die Wirkung ueber `Protection`, nicht den Mixin selbst); GrassOutline (`BlockStateBase.getShape(...)` HEAD, heisser Pfad, vor der Verzauberungs-Suche auf Pflanzenzustand geprueft); SharpnessGrassCut (`Player.attack` HEAD, nur Server). Konfliktrisiko: `getShape` ist ein Allerwelts-Ziel und ein heisser Pfad (nicht gegen Formen-Caches wie Lithium getestet); der Eingriff ist auf Spieler mit Schwert/Axt und Pflanzenzustand beschraenkt.
- **C3** Defaults an; Zahlen geklemmt (`grassThroughMax >= Min`). Cloth-Screen, `/simplefarming` + `/simplefarming config`. Lang 19 Schluessel EN=DE; Tooltip „Schaerfe III+“ war veraltet (farming-P2-2, gefixt).
- **C4** Keine eigenen Daten ausser Lang/Icon. Hacken-Ernte nutzt Vanilla-Tags (`#minecraft:hoes`, Blumen), keine Rezepte.
- **C5** Allein spielbar (Standalone-Target). Hand-Hinweis ueber Framework-`TransformHints` (ohne SimpleBuilding fragt niemand). Cloth Config harte Abhaengigkeit (Q-B1).
- **C6** A12: Hacken-Ernte (Rechtsklick auf reife Pflanze) und Schaerfe-Schnitt sind spuerbare Eingriffe, beide einzeln abschaltbar; Farmland-Schutz nur mit Feder-Fall-Stiefeln. Erntet nur Vanilla-Pflanzen (Samenpflicht, auch Fremd-Netherwarzen ueber `asItem`).
- **C7** S2C `config_sync` (nie C2S). Server prueft Reichweite 4.5, Weltrand, Spectator, `mayBuild`, Schutzbereiche (Fabric-`PlayerBlockBreakEvents.BEFORE`) vor jeder Aenderung; Aktions-Abklingzeit 4 Ticks. Vanilla-Client: Wirkung serverseitig, Umriss-Vorhersage entfaellt.
- **C8** Keine fremden Assets/Pfade/Schluessel, Lizenz CC0.
- **C9** Keine TODO/FIXME. Queue: REV2-A-8 erledigt; offen laut Queue nur `InteractionGuard`-Kopie (REV24-N-5, Duplikat von `com.simplelib.api.InteractionGuard`, bewusst wegen Modul-Unabhaengigkeit).

## Befunde

| ID | Befund | Fix / Restpunkt |
|---|---|---|
| simplefarming-P2-1 | `fabric.mod.json`/`mods.toml`: kopierter Weather-Text („…rain density (client)“). | gefixt: Commit bb1592f6c. |
| simplefarming-P2-2 | Tooltip `sharpnessCutsGrass` (EN/DE) sagte „Schaerfe III+“, tatsaechlicher Standard ist ab Schaerfe I (`grassCutMinSharpness=1`). | gefixt: Commit e4a1919c3. |
| simplefarming-P3-1 | `SharpnessGrassCutMixin` feuerte pro Position erst die Schutz-Ereignisse (`mayChange`) und pruefte dann, ob dort ueberhaupt Gras steht – bis ca. 880 Ereignisse pro Angriff mit Schwung-Verzauberung III. | gefixt: Commit e4a1919c3 (Zustand zuerst). |
| simplefarming-P3-2 | Hacken-Ernte im Kreativmodus laesst die Ernte-Drops fallen (Vanilla-Abbau im Kreativmodus liefert keine). | Restpunkt/Entscheidung: bewusst oder nicht? (Test `hoeHarvest`), kein Dupe, kostet nur Platz. |

## Test (ein sbt-Lauf fuer alle sechs Mods)

Lauf 31.10.2026 auf sb-test, Branch `b-rel32-qolb` (Commit e4a1919c3; Docs/Lang-Kategorie ff9fdd1fa): `:mc26_3:fabric:compileJava` + Modul-Compile C=0; `module-simplefarming-fabric-263` **13/13 gruen**, `module-simplefarming-standalone-fabric-263` **13/13 gruen** (Standalone-Lauf zunaechst durch Gradle-Daemon-Abbruch der Test-Maschine ohne Tests beendet, Wiederholung nur dieses Targets auf ff9fdd1fa gruen); `check_client_mixins.py` M=0 (660 Mixins, 0 Probleme, alle Mods); `check_lang_keys.py` L=0 (52 Module).

## Urteil

**shipbar** (nach gruenem Testlauf). Ein P2 (Beschreibung) plus ein P2 (Tooltip) gefixt, ein P3 gefixt, ein P3 offen. Rückfragen: Q-B1.
