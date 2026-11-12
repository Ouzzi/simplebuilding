# Audit SimpleBuilding-Kern (a-w1a, simplebuilding-8, 12.11.2026)

Basis `hub/b-bundle190` 43b9d09bd (Branch `a-w1a`), nur MC 26.3 Fabric. Aufbau wie `modules/simplebuildingtools/AUDIT.md`.
Modulwurzel laut `modules/modules.json` (`paths.root` "."): Kern = `common/src/shared/java/com/simplebuilding/**` (662 Dateien / 86,7k
Zeilen ohne Gametests, Testzentrale, Datagen), `src/main/java/com/simplebuilding/**` (Fabric-Einstieg, 23 Dateien / 1,7k),
`mc26_3/overlay/java/com/simplebuilding/**` (66 Dateien / 5,0k); dazu ~103k Zeilen Gametests. Viele Kern-Klassen sind auf 26.3
abgeschaltete 26.2-Kopien (`McVersion.*_IN_SB = false`: Lager, Bau-Werkzeuge, Trichter, Pads, Kolben, Öfen, Hämmer, Meißel, Gadgets);
sie wurden nur auf Registrierung geprüft, nicht auditiert (Fixes dort laufen über die Sub-Mods, `tools/dup_pairs.txt`).
Ein Vollaudit Zeile für Zeile war in einem Lauf nicht machbar; geprüft wurde **risikogeleitet** (P1-Klassen zuerst).
Frühere Audits nicht wiederholt, nur auf Regressionen geprüft: `docs/AUDIT-2026-09-26.md` (#9, #11, #12, #17, #30 … im Code vermerkt),
`docs/ai/AUDIT-SB-GAMEPLAY-2026-10-11.md` (SBG-03, SBG-04, SBG-06: alle drei behoben, siehe C6/C1).

## Vollständig gelesen
- Netzwerk: `src/main/java/.../networking/ModMessages.java` (14 C2S, 8 S2C, alle über `context.server().execute`),
  `networking/ModMessageHandlers.java` (alle Handler), `tweaks/network/TweaksNetwork.java` (`handleBoost`, `handleLaser`/`relayLaser`,
  Ratenfenster), `LaserPayload`, `GuideUnlockPayload` + `guide/GuideUnlocks#creativeUnlock`, `util/PlacedBundles#scroll`,
  `component/PaintBoxContents` (Auswahl-Klemme), Registrierung `tweaks/fabric/TweaksFabric.java`.
- Befehle: `command/SimplebuildingCommand.java` (Rechte, Config get/set/reset), `tweaks/command/TweaksCommands.java` (Rechte, Grenzen),
  `dev/testcentre/TestCentreCommand.java` (Rechte), `guide/GuideBooks#isOperator`, `mixin/OperatorBook*Mixin`.
- Config-Grenzen: `config/ServerTuningConfig#validate` (inkl. Reflexions-Klemme `TrimStrengths`), `SimplebuildingConfig#validatePostLoad`,
  Skript-Abgleich aller Zahlenfelder von `ServerTuningConfig` (79), `SimplebuildingConfig` (13), `TweaksConfig` (31) gegen Klemmen.
- Abbau/Setzen/Bewegen: `util/VeinMinerUsageEvent`, `util/StripMinerUsageEvent`, `MiningUtils#getStripMinerBlocks`,
  `doors/WideDoorBlock` (Setzen, Zusammenhalt, Abbau, Öffnen) + `WideDoors`, `items/custom/HammockItem#useOn/hang`,
  `blocks/custom/HammockTime`, `entity/LevitatingBlockEntity`, Schutzprüfungen in `ColorBrushItem#useOn`, `tweaks/item/LaserBeam`,
  `ChessPieceItem`, `mixin/ItemUseOnMixin` (Ablage-Haken).
- Zusatz-Kopfplatz (N49, neu seit dem letzten Audit): `util/ExtraHead`, `mixin/PlayerExtraHeadMixin`, `ServerPlayerExtraHeadMixin`,
  `InventoryMenuExtraHeadMixin`.
- Mixins (Server): `AnvilScreenHandlerMixin` (Ergebnis-Wege, `mayPickup`), `SmithingScreenHandlerMixin` + `CountBasedSmithingRecipe#matches`,
  `AbstractFurnaceMenuMixin`, `SlotMixin`, `EnderChestOpenerMixin`, `RangeReachMixin`, Überblick Kolben-Mixins (auf 26.3 inert, `PISTONS_IN_SB`).
- Statische Maps (C6): Skript über alle `static … Map/Set/List<…Player|UUID|Level|Entity|BlockPos…>` im Kern (ohne Client/Tests).
- Unabhängigkeit (C8): Skript über alle `com.simple*`-Bezüge im Kern-Hauptcode.
- Wackel-Test: `gametest/EndStructureTests#wreckHasLootChestsAndFrame`, Vorlagen `structure/end/wreck_*.nbt` (Rahmen, Stützblock),
  `tools/structures/generate_end_structures.py`, Vanilla 26.3: `GameTestServer#startTests`, `GameTestRunner`, `StructureGridSpawner`,
  `GameTestInfo#tick`, `StructureTemplate#addEntitiesToWorld`, `BlockAttachedEntity`, `PersistentEntitySectionManager#updateChunkStatus`,
  `ChunkHolder#scheduleFullChunkPromotion`.
- Block-Entities/Menüs (26.3 aktiv: Astral-Gewölbe als `ChestMenu`, Vanilla-Amboss/Schmiede/Kartentisch/Inventar über Mixins,
  Zusatz-Kopfplatz): `AstralVaultBlock`, `util/AstralStorage`, `mixin/AstralStorageMixin`, `AstralBlockEntityTypeMixin`;
  `PlacedSmallPartsBlock` + BE, `PlacedTemplateBlock` + BE, `util/PlacedTemplates`, `PlacedPaintBoxBlock` + BE, `GoatHornHolderBlock` + BE,
  `ChessPiecesBlock` + BE, `ChessPieceItem`, `StandingRodBlock` (`tryPlace`, Drops), `ShulkerShells`, `DiscFlips#flip`,
  `SledgehammerChips#tryCrush`; Hängematte (`HammockBlock` 26.3, BE, `HammockRopeBlock`, `HammockLayout`); `dummy/TrainingDummy`,
  `SmallArmorStand`, `ArmorStandSwap`, `StrawArmorStandItem`, `mixin/ArmorStandStandsMixin`, `EquipmentDispenseStandMixin`;
  `util/JukeboxPlaylist` + beide Overlay-Jukebox-Mixins; `ShulkerBoxBlockMixin`, `ShulkerOpen*Mixin`, `ItemEntityMixin`,
  `HopperBlockEntityMixin`, `ArrowRecovery` + Pfeil-/Bogen-/Armbrust-Mixins, `LivingEntityMixin`; `api/WorldPermissions`, framework
  `Protection`, `ModScreenHandlers`, `ModBlockEntities`, `DynamicLightHandler`; zum Abgleich die Claim-Haken in Simple Tweaks
  (`ClaimGameModeMixin`, `ClaimLevelMixin`). (Teil Block-Entities/Menüs mit Unteragent, Befunde nachgeprüft.)
- Nicht gelesen: Client/Screens/Renderer (nur geprüft, dass Handler serverseitig entscheiden), Datagen, Discover/Guide-Inhalte,
  Fahrzeug-Physik, Schach-Regeln, Musik/Speaker-Ketten im Detail, Weltgen-Features, die abgeschalteten 26.2-Kopien, Gametests außer dem Wackler.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | 14 C2S, alle auf dem Server-Thread. Doppelsprung: nur mit Verzauberung, `AirJumpGuard` (Luft, einmal je Sturz/Abklingzeit, #30). Oktant/Baustab/Blaupause (26.2-Pfade, auf 26.3 ohne registriertes Item): Ecken in 320 Blöcken, Enum-Namen, Radius/Achse geklemmt (SBG-04 behoben, `ModMessageHandlers.java:233`), Code ≤ 32 000, Signier-Budget je Spieler. Bündel-/Farbkasten-Auswahl: Slot-Bereich geprüft, Index von `PaintBoxContents` bzw. Bündel geklemmt. Meisterbauer: Item bestimmt der Server aus dem eigenen Inventar, Rucksack höchstens ein Normalstapel. Abgestelltes Bündel: Schleichen, Reichweite, `mayBuild` + `mayInteract`, Klang gedrosselt (SBG-06 behoben, `PlacedBundles.java:215`). Guide-Freischaltung: nur Kreativ + Rezept-Tab. Tweaks: Boost (Spawn-Elytra, auf 26.3 aus) mit Fenster/Obergrenze, Laser nur beim Benutzen, Rate 20/s, Reichweite, UUID vom Server. |
| C2 Server-Autorität / Reichweite | Bearbeiten abgelegter Blaupausen: geladen, Reichweite 8, echter Block, `WorldPermissions.mayChange`. Ablage-Haken laufen in `Item#useOn` (Vanilla prüft davor Spawnschutz/Abenteuermodus, Claim-Mods über `UseBlockCallback`). Mehrblock-Setzen (Breite Türen, Hängematte) prüft weitere Zellen nur auf Ersetzbarkeit (P3-3). |
| C3 Dupes / Item-Verlust | Breite Türen: unverschiebbar (`PUSH_BLOCKED`), Beute nur an der unteren Scharnier-Zelle, Kreativ-Abbau ohne Drop. Zusatz-Kopfplatz: ein Wert je Spieler hinter allen Menü-Slots (kein Doppel über zwei Menüs), Tod/`keepInventory`/End-Rückkehr wie Vanilla, Schift-Klick endet. Amboss/Schmiede: Zähl-Rezepte verbrauchen erst ab passender Menge (`matches`), Aufladen verbraucht genau den Bedarf. Astral-Gewölbe: 27 Zusatzplätze als zweiter Container hinter `CompoundContainer`, `stillValid` über die aktive Truhe, Tod/`restoreFrom` wie Endertruhe. Abgelegtes (Vorlage, Horn, Farbkasten, Schach, Hängematte): Drops genau einmal als Kopie des gespeicherten Stapels; Rüstungsständer-Tausch nach Wert. **P2-2 gefunden und gefixt** (Eier beim Teil-Abbau). |
| C4 NBT/Komponenten | `PaintBoxContents` (`intRange`, Konstruktor-Klemme), Zusatzkopf lädt nur passende Köpfe (P3-5). Kleinteile ≤ 4 Teile je Anzahl 1, Schach 4 Plätze/Anzahl 1/Drehung `&3`, Hängematten-Versatz ±8/±1, Jukebox-Zeiger `floorMod`, Astral-Index 27..53. Gespeicherte Einzelstapel (Vorlage, Horn, Farbkasten, Zusatzkopf) behalten ihre Ladeanzahl (≤ 99, nur per `/data`), dazu P3-8/P3-9. |
| C5 Tick-Kosten | Ader-/Streifenabbau über `MiningUtils`-Budget, Abbau je Block über `gameMode.destroyBlock` (Ereignisse, Spawnschutz je Block, keine unzerstörbaren Blöcke), Rekursionsschutz in `finally`. Hängematte: Uhr nur bei genug Ruhenden (`players_sleeping_percentage`), Faktor gedeckelt. Alle Zahlen der Server-Config geklemmt (Skript: keine Lücke außer Koordinaten/Farbe). |
| C6 Leaks | Statische Spieler-Maps: `WeakHashMap` (Budgets, Laser, Boost, Guide, Puls) oder beim Abmelden/Stopp geräumt (`WandUndo`, SBG-03 behoben, `WandUndo.java:104/109`), kleine UUID-Maps mit Bereinigung (`PlacedBundles`, `StructureCompassCache`, `EchoCompassItem`). Thread-Locals beim gezielten Abbau (P3-11). Keine offene Leckstelle; `Protection.clear` beim Serverstopp. |
| C7 Schutz/Rechte | `/simplebuilding …` (Config set/reset, Boote/Loren entfernen, Weltspawn): **P2-1 gefunden und gefixt** (Stufe 1 reichte). `/sbtestcentre`, `/testcenter`: Stufe 2 (Release-Abschaltung vorbereitet auf `a-w1a-tcdev`, P3-7). Admin-Buch nur Ops ab Stufe 2. Laser/Farbpinsel/Schach: `mayInteract`, `mayUseItemAt`, `WorldPermissions` je Stelle. Mixins: kein `@Overwrite` gesehen, keine Reichweite oder Rechteprüfung aufgehoben; `RangeReachMixin` nimmt nur Reichweite weg. Nihil-Gewölbe: Claim-Schutz macht der Auftraggeber (simplebuilding-7). |
| C8 Unabhängigkeit | Kern-Hauptcode bezieht nur Pflicht-Sub-Mods (`simplelib`, `simplehammers`, `simplebuildingtools`, `simplestorage`, `simplemachines`, `simpleoctets`, `simpleblocks`, `simpletools`, `simplecrucibles`, `simpleenchanting`, `simpleresonance` – alle in `requires`), optionale nur über IDs. |
| C9 Doku/Pflege | 0 TODO/FIXME im Kern-Hauptcode; Lang EN=DE (3624/3624, Overlay 3748/3748); `check_dup_impl` 253 Paare, 0 Probleme. |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplebuilding-P2-1 | P2 (Rechte) | `command/SimplebuildingCommand.java:38` (vorher): `OPERATOR_ONLY` fragte `PlayerList#isOp` – jeder Eintrag der Op-Liste, auch **Stufe 1** (Vanilla: nur Spawnschutz umgehen), durfte `/simplebuilding config set|reset` (ganze Server-Config, z. B. Obergrenzen hoch), `killboats`/`killcarts` und `tweaks worldspawn set`. SimpleLibs Config-Befehl und `GuideBooks#isOperator` verlangen Stufe 2. | **gefixt**: Spieler + Stufe 2 live aus der Operatorliste über `GuideBooks#isOperator` (`SimplebuildingCommand.java:44`, wie das Admin-Buch), Konsole/Befehlsblöcke weiter draußen; die Tests, die die Befehle als Operator fahren, setzen jetzt ausdrücklich `GAMEMASTER` (der GameTest-Server vergibt bei `op` ohne Stufe nur `ALL`). Gametest `core_audit_game_test_operator_only_needs_gamemaster_level` (Stufe 0/1 abgewiesen, Stufe 2 zugelassen, Konsole abgewiesen). |
| simplebuilding-P2-2 | P2 (Gratis-Mobs) | `blocks/custom/PlacedSmallPartsBlock.java:217` (`spawnAfterBreak`) mit `:244/:273` (`playerWillDestroy`/`destroy`): baut ein Spieler im Überleben ohne Behutsamkeit ein Teil eines Haufens ab (N37), legt `destroy` die übrigen Teile zurück; erst danach ruft Vanillas `playerDestroy` → `dropResources` → `spawnAfterBreak` an derselben Stelle, das den zurückgelegten Haufen fand und **dessen Eier schlüpfen ließ** (je Ei 1/8) – die Eier blieben liegen. Kiesel neben Ei abbauen, wieder hinlegen, wiederholen: Küken ohne Ende. | **gefixt**: `destroy` merkt sich den zurückgelegten Haufen (`RESTORED`, nur Überleben), `spawnAfterBreak` lässt genau diesen aus. Gametest `core_audit_game_test_broken_pile_part_leaves_the_other_eggs_whole` (200 Abbauten, 0 Küken; vorher ≈ 25). |
| simplebuilding-P3-1 | P3 (Test) | Wackel-Test `end_structure_game_test_wreck_has_loot_chests_and_frame` (rot im Gate 94, 03.11.; Meldung nie erhalten), Ursache und Fix siehe Abschnitt „Wackel-Test“. | **gefixt** 6e8ef705d |
| simplebuilding-P3-2 | P3 | Zusatz-Kopfplatz: `ExtraHead.HeadSlot` (`util/ExtraHead.java:188`) überschreibt `mayPickup` nicht – ein Kopf mit Fluch der Bindung lässt sich im Überleben aus dem Platz nehmen (Vanillas `ArmorSlot` verweigert das; `ExtraHead#useInAir` verweigert den Tausch schon). | Vorschlag: `mayPickup` wie `ArmorSlot` (`PREVENT_ARMOR_CHANGE`, nicht Kreativ) |
| simplebuilding-P3-3 | P3 | Mehrblock-Setzen prüft nur die geklickte Stelle auf Rechte: Breite Tür (`doors/WideDoorBlock.java:218`, bis 2x3 Zellen, beim Öffnen Schwenkzellen `:349`), Hängematte (`items/custom/HammockItem.java:110`), andere Hälfte beim Farbpinsel (`ColorBrushItem.java:206`). Zellen ersetzen nur Luft/Ersetzbares; Vanillas Tür/Bett prüfen die zweite Zelle ebenso wenig. An Claim-Grenzen kann ein Teil 1-2 Blöcke ins Nachbar-Grundstück ragen. | Vorschlag: `WorldPermissions.mayPlace` je weiterer Zelle (`api/WorldPermissions.java:86`, gibt es schon) |
| simplebuilding-P3-4 | P3 | Abgestelltes Bündel weiterschalten fragt Spawnschutz/Abenteuer, nicht den Claim (`util/PlacedBundles.java:215`, SBG-06-Entscheidung „Besitzer“ offen). | Hinweis |
| simplebuilding-P3-5 | P3 | Zusatzkopf laden filtert mit `ExtraHead::fits` (`mixin/PlayerExtraHeadMixin.java:100`): fällt ein Kopf eines anderen Mods per Datapack aus `#simplebuilding:extra_head_slot`, ist er beim nächsten Einloggen still weg statt im Inventar. | Vorschlag: ins Inventar legen bzw. fallen lassen |
| simplebuilding-P3-6 | P3 | Gleiche Rechte-Lücke wie P2-1 im Sub-Mod: `modules/simplepads/shared/java/com/simplepads/command/PadsCommands.java:24` (`OPERATOR_ONLY` mit `isOp`). | TODO simplepads (nicht Kern) |
| simplebuilding-P3-7 | P3 | `/sbtestcentre` und `/testcenter` sind im Release für Ops ab Stufe 2 aktiv (`dev/testcentre/TestCentreCommand.java:67/162`): baut große Flächen, lädt Chunks. | bekannt, vorbereitet auf `a-w1a-tcdev` (zurückgehalten bis Upload-Freigabe) |
| simplebuilding-P3-8 | P3 | Umriss-Betrachter abgelegter Blaupausen: `PlacedTemplateBlockEntity#toggleOutlineViewer` (`:112`, gespeichert `:207/:217`) hängt je umschaltendem Spieler eine UUID an, ohne Grenze und ohne `mayChange` (`PlacedTemplateBlock.java:156`); Liste geht in jedes Update-Paket. Nur ein Größenhebel bei sehr vielen Spielern. | Vorschlag: Liste begrenzen (z. B. 16) |
| simplebuilding-P3-9 | P3 | `component/PaintBoxContents.java:23`: Codec nimmt je Farbe bis `Integer.MAX_VALUE`, die Kapazität prüft nur `PaintBoxItem#insert` – ein per Befehl gebauter Kasten kann mehr halten. | Hinweis |
| simplebuilding-P3-10 | P3 | `dummy/TrainingDummy.java:151`: der Kürbis wird vor `type.create` (`:166`) verbraucht; schlägt das Erzeugen fehl (praktisch nie), ist er weg. | Hinweis |
| simplebuilding-P3-11 | P3 | Gezielter Abbau (`ChessPiecesBlock.java:156`, `PlacedSmallPartsBlock.java:244`) nimmt die übrigen Teile vor dem Entfernen aus der Block-Entity; scheitert `removeBlock`, wären sie weg, und die Thread-Locals halten die Welt bis zum nächsten Abbau. Sehr unwahrscheinlich. | Hinweis |
| simplebuilding-P3-12 | P3 | Tote 26.2-Teile auf 26.3: `ModScreenHandlers.java:34/39` registriert `BACKPACK_MENU`/`TIERED_CHEST_MENU` (öffnet nichts mehr, Simple Storage hat eigene), `PlacedAttractors`/`PlacedDetectors` ohne Magnet-Item (`MAGNET` null). | Pflege (beim Port-Run aufräumen) |

## Wackel-Test `wreck_has_loot_chests_and_frame`
**Ursache (aus Code und Vanilla-26.3-Quellen abgeleitet, eine Meldung lag nie vor):**
1. *Entity-Sichtbarkeit außerhalb des Testraums.* Der Test baut die Wracks (15x12x17 und 19x11x13 Blöcke) 48 Blöcke über dem 8x8-Raum;
   der Rahmen liegt in der Vorlage bei z 8, also schon außerhalb des Raums. Der Runner (`GameTestInfo#tick`) wartet nur, bis die Chunks des
   eigenen Raums Entities ticken; die übrigen Wrack-Chunks sind nur Nachbarn eines festgeladenen Chunks. Ein Entity, das in einen Chunk
   kommt, dessen Sichtbarkeit (`ChunkHolder#scheduleFullChunkPromotion` → `PersistentEntitySectionManager#updateChunkStatus`, ein eigener
   Hauptthread-Auftrag) noch nicht bestätigt ist, landet in einer `HIDDEN`-Sektion (`chunkVisibility.defaultReturnValue(HIDDEN)`), und
   `getEntitiesOfClass` überspringt sie – „found 0“. Unter Last (Gate: alle Tests, Hauptthread hinterher) liegt zwischen der Bestätigung des
   Testraum-Chunks und der des Nachbar-Chunks ein Tick; bei z-Lage ≥ 8 im Chunk (halbe Fälle) trifft es den Rahmen. Selten, lastabhängig –
   passt zu „einmal rot, Zweitversuch grün“.
2. *Zu weite Suchkästen.* `new AABB(base).inflate(14)` reichte 14 Blöcke unter und neben das Wrack, in die Luft von bis zu acht
   Nachbartests (Raster: 5 Blöcke Abstand, Reihen 6); Aufräumen in 24x16x28 und Rahmen-Löschen in `inflate(30)` griffen in fremde Bereiche.
3. *Kein Aufräumen bei Fehlschlag:* ein roter Lauf ließ Wrack und Rahmen stehen.
Ausgeschlossen: Rahmen ohne Stützblock (alle acht Vorlagen haben den Purpurblock dahinter, Skript über die `.nbt`), Haltbarkeit
(431 = 432-1 fest in der Vorlage, Sparsamkeit nur mit Verzauberung), Rahmen früherer Läufe (frische Welt je Lauf, `runGametest` löscht
`build/gametest/world`; Raster mit `clearOnBatch=false` vergibt jede Stelle nur einmal), `/place` (Seed 0, Integrität 1, fest).
**Fix** (`gametest/EndStructureTests.java`, 6e8ef705d): Suchen, Prüfen und Aufräumen nur in den echten Vorlagen-Grenzen (`templateBox`);
alle Chunks unter dem Wrack werden festgeladen (`setChunkForced`, nur selbst gesetzte werden wieder gelöst), der Test wartet per Sequenz bis
`areEntitiesActuallyLoadedAndTicking` (dieselbe Bedingung, die der Runner für den Raum nutzt), `maxTicks` 200 (Katalog + Fabric-Adapter);
Aufräumen im `finally`; Diagnose bleibt und ist erweitert (Rahmen-Liste mit Position, Truhen-Position + Beutetabelle, fremde Rahmen im
Kasten vor dem Setzen).

## Urteil
**shipbar nach Fix** (0 P1, 2 P2 gefixt, 12 P3, davon der Wackler gefixt). Netzwerk, Config-Grenzen und Abbau-Pfade sind durch die
früheren Audits gehärtet; die beiden P2 lagen in neueren Teilen (Rechte-Prädikat der Kern-Befehle, gezielter Teil-Abbau N37).

## Testergebnis
Commits: 4fdf066f2 (Plan), 6e8ef705d (Wackler), 1a60fb497 + 330f77ad0 (P2-1), 0276f1e88 (P2-2), dieses Dokument.
Lokal: javac der geänderten Dateien gegen 26.3 + Fabric-API + Pflicht-Sub-Mods (`/root/sb-tools-a/w1a/javac.sh`; nur bekannte Meldungen:
Access-Widener (`CreativeModeTab`, `StairBlock`, `SkullBlock`, `MenuType`, `IronBarsBlock`), fehlendes Trinkets-API, 26.2-Kopie
`ReinforcedBundleItem`, Datagen-Loot, `makeMockServerPlayerInLevel` veraltet; keine Meldung in geänderten Dateien),
`tools/check_gametest_adapters.py` 0 Lücken, `tools/check_dup_impl.py` 253 Paare/0 Probleme, `precheck.py --fast` grün. API-Gleichheit 26.2/26.3
der benutzten Methoden per `javap` geprüft (geteilter Code). Vanilla-Verhalten am 26.3-Quelltext nachgesehen (siehe Wackel-Test).
sbt (je Muster ein Aufruf): `python3.12 tools/testrunner/run.py --targets fabric-263 --filter '<muster>'` mit
`simplebuilding:end_structure_game_test_wreck_has_loot_chests_and_frame` (mehrfach), `simplebuilding:core_audit_game_test_*`,
`simplebuilding:tweaks_game_test_*`, `simplebuilding:config_option_game_test_*`, `simplebuilding:placed_template_game_test_*`,
`simplebuilding:guide_book_game_test_*`.
Nicht getestet: die Gametests liefen lokal nicht (kein Gradle, sb-test nicht erreichbar); Client,
Forge/NeoForge, echter Paket-Spam, Claim-Mods (nur Fabric-Ereignisse/`WorldPermissions`), Leistungsmessung (eigener Teil von simplebuilding-8).
