# Audit simplehammers (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplestorage/AUDIT.md`.
Umfang: 33 Java-Dateien / 4,4k Zeilen (ohne Tests), 8 Mixins (4 gemeinsam, 4 Client), 1 Menue (nur der Amboss: `HammerAnvilMixin` auf Vanillas `AnvilMenu`, die Aufwertung laeuft in der Welt),
Payloads: keine (weder C2S noch S2C). Risikogeleitet gelesen: Flaechenabbau und Welt-Eingriffe zuerst.

## Vollstaendig gelesen
- Flaechenabbau: `engine/SledgehammerUsageEvent.java`, `engine/SledgehammerUtils.java` (shouldBreak, Divisor), `engine/HammerProtection.java`,
  `item/SledgehammerItem.java` (Abbau, Umformen, Zerschlagen, `getBlocksToBeDestroyed`), `mixin/HammerMiningSpeedMixin.java`.
- Welt-Eingriffe: `engine/SledgehammerUpgrades.java` (Auftraege, `finish`, Schutz, Karte pro Spieler), `engine/SledgehammerProgress.java` (SavedData),
  `engine/HammerCorners.java` (Achtel abtragen), `engine/HammerCrush.java`, `engine/Orientation.java`, `mixin/CarvedStairMixin.java`.
- Items/Rezepte: `engine/HammerAnvil.java` + `mixin/HammerAnvilMixin.java`, `engine/SledgehammerCrafting.java` + `mixin/ShapelessRecipeMixin.java`,
  `registry/HammerRegistry.java`, `HammerTiers.java`, `SimpleHammers.java`, `config/HammerConfig.java`, `HammersFabric.java`, `engine/UpgradeHints.java`,
  `engine/HammerEnchantments.java` (Kopf), Datenpaket (3 Verzauberungen, Rezepte).
- Client: `client/HammerAreaClient.java`, `client/HammerVisuals.java`, `mixin/client/*` (4), nur auf Server-Wirkung geprueft.
- Nicht gelesen: `discover/HammersDiscover.java`, `guide/HammersGuide.java` (generiert), `compat/HammersViewer.java`, `compat/jei/*`, Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk-Payloads | Keine eigenen Payloads. Der Abbau laeuft ueber Vanillas Paket + Fabric `PlayerBlockBreakEvents.BEFORE` auf dem Server-Thread; Hammer und Schleichen liest der Server aus dem Spieler. Die Mining-Zeit rechnet `HammerMiningSpeedMixin` auf Client und Server gleich (Vanilla prueft die Abbauzeit serverseitig mit `getDestroyProgress`). |
| C2 Server-Autoritaet / Reichweite | Flaeche, Haltbarkeit, Aufwertung entscheidet der Server (`SledgehammerUsageEvent`, `SledgehammerUpgrades.finish`); Aufwertung prueft jeden Tick Block, Nugget, Hammer, Material, Schutz und Blick (`stillValid`, Reichweite +1 Server-Puffer). Client-Klassen nur in `client/` und `mixin/client`. |
| C3 Dupes / Verlust | Flaechenabbau: Drops einmal je Block ueber Vanillas `destroyBlock` (Loot-Tabelle, Block-Entities lassen ihren Inhalt fallen, Shulker mit Inhalt), Haltbarkeit 2 je Block + 1 bei falschem Werkzeug, Hammer bricht -> Schleife endet, Schleichen = ein Block, Bedrock/Befehlsbloecke (`getDestroySpeed < 0`) und Luft bleiben. Schutz: Ursprung und jeder Zusatzblock `mayInteract` (Spawnschutz, Weltrand) + `HammerProtection.mayAct(DESTROY)` (Claims). Aufwertung: Material nur bei `finish` und nur wenn `stillValid`. Werkbank: der Hammer bleibt beschaedigt zurueck (kein Rezept verbraucht ihn). Amboss: nur gleiche Stufe, Kosten 1..39. **P2-1 gefunden und gefixt.** |
| C4 NBT/Komponenten | `SledgehammerProgress`: Eintraege mit `listLenient`, Treffer ohne gueltigen Block fallen beim Pruefen weg; Config mit Klammern (Sekunden 1..30, Schaden 0..64). |
| C5 Tick-Kosten | `SledgehammerProgress.tick` alle 20/100 Ticks nur ueber Eintraege geladener Positionen. Mining: `countBlocksBroken` je `getDestroyProgress` (siehe P3-2). |
| C6 Leaks | `SERVER_JOBS`, `LAST_HINT` (UUID) werden beim Abmelden bereinigt; Ladungsziele in `WeakHashMap`; `HARVESTED_BLOCKS` wird im `finally` geleert. |
| C7 Sicherheit/Rechte | Alle Weltaenderungen (Umformen, Abtragen, Zerschlagen, Aufwertung, Flaeche) fragen `HammerProtection`/`mayTransform`. Mixins: `CarvedStairMixin` (StairBlock, nur Zusatz-Eigenschaft), `HammerMiningSpeedMixin` (nur mit Hammer in der Hand), `ShapelessRecipeMixin` (nur Hammer im Raster), `HammerAnvilMixin` (nur Hammer), Client-Mixins im `client`-Array. Abgeschaltet (`SimpleHammers.on()` false) = Spitzhacke, kein Flaechenabbau. |
| C8 Unabhaengigkeit | allein spielbar (`depends` nur simplelib, Standalone-Ziel vorhanden); Enderit-Stufe nur mit Simple Ores. |
| C9 Doku/Pflege | Keine TODO/FIXME. 3 Kern-Dubletten (CarvedStairMixin, ShapelessRecipeMixin, MultiPlayerGameModeHammerProgressMixin); die 26.2-Kopien von Utils/Upgrades/UsageEvent stehen unter `common/src/mc26_2/java/com/simplebuilding/util/` (nicht in `dup_pairs.txt`). Lang EN=DE (check_data gruen). |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplehammers-P2-1 | P2 (Logikfehler, Haltbarkeit) | `SledgehammerUtils#shouldBreak` (Stufe Override II = "beliebige abbaubare Bloecke", `:205`) liess Wasser und Lava durch (Haerte 100, nicht < 0). Im Flaechenabbau (`SledgehammerUsageEvent.java:65-77`) liefert `destroyBlock` fuer eine Quelle `true`, ohne etwas zu aendern: der Block zaehlte als abgebaut (AREA_BLOCK-Ereignis), kostete 2 Haltbarkeit (`SledgehammerItem#mineBlock`) + 1 (falsches Werkzeug) und machte den Abbau durch den Zaehler (`countBlocksBroken`) langsamer. Betraf Seen, Ufer und Durchbruch-Tiefe mit Override II. | **gefixt** (`SledgehammerUtils.java:185`: `LiquidBlock` zaehlt nie; wassergefuellte Bloecke wie Stufen zaehlen weiter). Gametest `area_break_skips_fluids` (Zaehler 8 statt 9, Wasser nein, wassergefuellte Stufe ja). 26.2-Kern-Kopie `common/src/mc26_2/java/com/simplebuilding/util/SledgehammerUtils.java:218` hat denselben Fehler -> beim 26.2-Port nachziehen. |
| simplehammers-P3-1 | P3 (Server-Last) | Eine Oktant-Auswahl (Simple Building Tools, bis 32 Kanten/4096 Stellen) kann ueber `world.getBlockState` (`SledgehammerUtils.java:180`, `SledgehammerUsageEvent.java:51`) ungeladene Chunks synchron laden, wenn die Figur ueber den geladenen Bereich ragt. | Hinweis (`world.hasChunkAt(position)` vor dem Lesen) |
| simplehammers-P3-2 | P3 (Tick-Kosten) | `HammerMiningSpeedMixin` ruft bei jedem `getDestroyProgress` (Client und Server, jeder Tick des Abbaus) `miningSpeedDivisor` -> `countBlocksBroken`: bis 75 Positionen (Radius + Durchbruch) bzw. 4096 (Oktant) mit je `shouldBreak`. Nur mit Hammer in der Haupthand. | Hinweis (je Tick und Ziel cachen) |
| simplehammers-P3-3 | P3 (Balance) | Beruehrung des Konstrukteurs, rueckwaerts (`SledgehammerItem#reshapeTarget`, `finishUsingItem`): Stufe -> Treppe -> Block macht aus einer Stufe zwei Schritte spaeter einen vollen Block (Material aus dem Nichts, 1 + 2 Haltbarkeit). Vermutlich gewollt (Verzauberung des Konstrukteurs aus Simple Building). | Besitzer-Frage (nicht angefasst) |
| simplehammers-P3-4 | P3 | `finishUsingItem` prueft den Blick mit festen 5,0 Bloecken (`SledgehammerItem.java:417`) statt `blockInteractionRange()` (4,5 im Ueberleben). | Hinweis |
| simplehammers-P3-5 | P3 | 3 Kern-Dubletten + 26.2-Kopien (siehe C9): jeder Fix muss im 26.2-Kern nachgezogen werden (P2-1). | Liste |

## Urteil
**shipbar nach Fix** (0 P1, 1 P2 gefixt, 5 P3). Der Flaechenabbau ist sauber gehaertet (Schutz je Block, Haltbarkeit, Wiedereintritt, Bereinigung bei Abmeldung).

## Testergebnis
Lokal: javac der geaenderten Dateien (`SledgehammerUtils`, `HammerTests`, `ModuleGameTest`) gegen 26.3 + Fabric-API: nur die bekannten Access-Widener-Meldungen in nicht geaenderten Dateien (`HammerRegistry`, `LibMaterials`), `modules/simplehammers/tools/check_data.py` gruen, `tools/check_gametest_adapters.py` OK, `tools/check_dup_impl.py` 0 Probleme.
sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`. Nicht getestet: der neue Gametest selbst (kein Gradle), Client, Forge/NeoForge, Mehrspieler-Last.
