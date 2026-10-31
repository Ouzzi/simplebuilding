# AUDIT Simple Movement (`simplemovement`) – Release-Audit b-rel32-qolb, 31.10.2026

Basis: b-rel32 d9cda1e21, Branch `b-rel32-qolb`. Umfang: 1129 Zeilen Java (davon ca. 144 Tests), 5 Mixins. Nur MC 26.3 Fabric. Pruefliste C1–C9 aus `docs/ai/RELEASE-QOL-OPT-AES.md`.

## Durchgesehene Dateien (jede Datei vollstaendig gelesen)

Java (Fabric 26.3 + shared; neoforge/forge-Loader-Klassen nicht Teil des Auftrags, nur NUR-Fabric):
- `clienttest/java/com/simplebuilding/modules/simplemovement/ModuleClientSmoke.java` (9 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simplemovement/ModuleGameTest.java` (26 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simplemovement/MovementFabric.java` (38 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simplemovement/MovementFabricClient.java` (20 Zeilen)
- `fabric/src/main/java/com/simplebuilding/modules/simplemovement/MovementModMenu.java` (9 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/ClimbAudit.java` (3 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/ClimbSecurity.java` (22 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/CrawlAccessor.java` (6 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/CrawlLimiter.java` (7 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/JumpBoostStep.java` (56 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/KeyMigration.java` (29 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/MovementCommands.java` (49 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/MovementConfig.java` (44 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/MovementScreen.java` (46 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/MovementTests.java` (144 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/SimpleMovement.java` (149 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/client/MovementClient.java` (33 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/mixin/ClimbPacketMixin.java` (28 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/mixin/LadderSpeedMixin.java` (20 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/mixin/LivingEntityJumpMixin.java` (35 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/mixin/PlayerEntityCrawlMixin.java` (56 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/mixin/PowderSnowWalkMixin.java` (39 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/network/ConfigSyncPayload.java` (18 Zeilen)
- `shared/java/com/simplebuilding/modules/simplemovement/network/CrawlStatePayload.java` (12 Zeilen)

Ressourcen/Daten (Zeilen):
- `fabric/src/main/resources/fabric.mod.json` (47)
- `shared/resources/assets/simplemovement/icon.layers.json` (118)
- `shared/resources/assets/simplemovement/lang/de_de.json` (27)
- `shared/resources/assets/simplemovement/lang/en_us.json` (27)
- `shared/resources/data/minecraft/tags/enchantment/on_random_loot.json` (6)
- `shared/resources/data/minecraft/tags/enchantment/tradeable.json` (6)
- `shared/resources/data/minecraft/tags/enchantment/treasure.json` (6)
- `shared/resources/data/simplemovement/enchantment/sprint.json` (46)
- `shared/resources/data/simplemovement/enchantment/step_height.json` (34)
- `shared/resources/simplemovement.mixins.json` (15)
- 2 PNG-Dateien (Texturen/Icons, aus `*.layers.json` erzeugt; Pixelgroesse geprueft durch check_mod_icons / check_missing_textures)
- `fabric/build.gradle`, `wiki/manual.json`, `tools/check_data.py`


## Ergebnis C1–C9

- **C1** Alle Klassen gelesen (Config/Sync, `ClimbSecurity`, `CrawlLimiter`, `JumpBoostStep`, `KeyMigration`, Client, Fabric-Einstiege, 5 Mixins, 2 Payloads). Server-Autoritaet: Kriechen und Leiter-Paketbudget werden serverseitig entschieden; Client-Prediction nutzt die per `config_sync` gelieferten Serverwerte. `MovementClient` (Client-Klassen) nur ueber `MovementFabricClient`. Befund movement-P3-2.
- **C2** LadderSpeed (MixinExtras `@ModifyExpressionValue` auf Konstante 0.2 in `handleRelativeFrictionAndCalculateMovement` + `handleOnClimbable` RETURN); ClimbPacket (`ServerGamePacketListenerImpl.handleMovePlayer` HEAD, Anti-Cheat-Budget, nur Haupt-Thread, Rubberband per `teleport`); LivingEntityJump (`jumpFromGround` HEAD, `tick` TAIL fuer Jump-Boost-Stufe); PlayerEntityCrawl (`Player.updatePlayerPose` HEAD, Interface `CrawlAccessor`); PowderSnowWalk (`canEntityWalkOnPowderSnow` RETURN). `defaultRequire=1`. Konfliktrisiko: Konstanten-Ziel in LadderSpeed ist die fragilste Stelle (Mods, die dieselbe Methode umbauen); bei Verlust bricht der Start ab statt still zu versagen.
- **C3** Defaults: Kriechen an, Autowalk aus (Server erlaubt), Leiter 0.4/Rutschen 0.8 (Vanilla ca. 0.2/0.15; siehe A12), Frostlaeufer an, Jump-Boost-Stufe an. Zahlen NaN-fest geklemmt. Cloth-Screen, `/simplemovement` + `/simplemovement config`, `/crawl` fuer alle. Lang 27 Schluessel EN=DE, „Default:/Standard:“ ueberall.
- **C4** Zwei Datenpack-Verzauberungen (Step Height, Sprint) mit Treasure/Tradeable/Random-Loot-Tags (Test `enchantTreasureRules`), kein Rezept/Loot-Eigenbau, Lang vollstaendig, Icon vorhanden.
- **C5** Allein spielbar (Standalone-Target). Keine Fremdzugriffe. Framework (SubMods) und SimpleLib Jar-in-Jar. Cloth Config harte Abhaengigkeit (Q-B1).
- **C6** A12: Autowalk ist standardmaessig aus (Server entscheidet), Leiter-Tempo ueber `ladderClimbingSpeed` auf Vanilla zuruecksetzbar (0.2), Rutschen und Jump-Boost-Stufe einzeln abschaltbar. Rückfrage Q-B3 (Leiter-Default doppelt so schnell wie Vanilla).
- **C7** S2C `config_sync` (32 KB, nie C2S) und S2C `crawl_state` (UUID+bool; nie C2S). Server vertraut dem Client nicht: Kriechen laeuft ueber `/crawl` mit Server-Limiter (10 Ticks), Leiterbewegung ueber Paketbudget (<=3 Ticks, Jump Boost/Levitation/Riptide/Elytra beruecksichtigt). `canSend`-Pruefung vor jedem Senden (Vanilla-Client bleibt unbehelligt). Client ohne die Mod: kein Kriechen/Autowalk, sonst Vanilla.
- **C8** Keine fremden Assets, keine Pfade/Schluessel, Lizenz CC0.
- **C9** Keine TODO/FIXME. Offen: movement-P3-2 (Kriech-Status nach Dimensionswechsel).

## Befunde

| ID | Befund | Fix / Restpunkt |
|---|---|---|
| simplemovement-P2-1 | `fabric.mod.json`/`mods.toml`/`neoforge.mods.toml` trugen den kopierten Weather-Text („Movement switch (server) and rain density (client)“) – sichtbar in Mod Menu und auf der Download-Seite. | gefixt: Commit bb1592f6c (Text aus `modules.json`). |
| simplemovement-P3-1 | Tastenkategorie hiess „Simple Quality of Life“ (Mod ist „Simple Movement“; zwei gleich benannte Kategorien moeglich). | gefixt: Commit ddd4d457c (EN/DE „Simple Movement“). |
| simplemovement-P3-2 | Nach Dimensionswechsel bleibt der Server-Kriechstatus gesetzt, der neue Client-Spieler startet aufrecht; Pose weicht bis zum naechsten `/crawl` ab (selbstheilend). `START_TRACKING` feuert nicht fuer den eigenen Spieler. | Restpunkt: Kriechen bei `ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD` zuruecksetzen (Event im Repo bisher nirgends genutzt, deshalb nicht blind eingebaut). |

## Test (ein sbt-Lauf fuer alle sechs Mods)

Lauf 31.10.2026 auf sb-test, Branch `b-rel32-qolb` (Commit e4a1919c3; Docs/Lang-Kategorie ff9fdd1fa): `:mc26_3:fabric:compileJava` + Modul-Compile C=0; `module-simplemovement-fabric-263` **19/19 gruen**, `module-simplemovement-standalone-fabric-263` **19/19 gruen**; `check_client_mixins.py` M=0 (660 Mixins, 0 Probleme, alle Mods); `check_lang_keys.py` L=0 (52 Module).

## Urteil

**shipbar** (nach gruenem Testlauf). Ein P2 (Beschreibung) und ein P3 gefixt, ein P3 Restpunkt. Rückfragen: Q-B1, Q-B3.
