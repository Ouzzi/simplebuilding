# Audit simpleenchanting (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991 (Buendel 186), nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases).
Umfang: 15 Java-Dateien / 2,6k Zeilen ohne Tests (mit Tests 17 / 3,5k). Risikogeleitet: Verzauberungs-Menue und Regeln vollstaendig,
Aufbau wie `modules/simplestorage/AUDIT.md`.

## Vollstaendig gelesen
- Menue: `menu/EnchanterMenu.java` (Slots, Schnellverschieben, Knoepfe, Kosten, Saat, Apfel, `removed`, `stillValid`), Vergleich mit Vanillas `EnchantmentMenu` (dekompiliert 26.3).
- Regeln: `EnchantingRules.java` (Stufe, Budget, Preis, Endgame-Stufen, Auswahl je Saat, Regale/Boden), `EnchanterStage.java`, `config/EnchantingConfig.java`.
- BlockEntity/Block: `block/EnchanterBlockEntity.java` (Lager, Hopper-Seiten), `block/EnchanterBlock.java`, `block/ObsidianFloorBlock.java`;
  Loot-Tabellen der Enchanter, `in_world_upgrades/enchanters.json`, `SimpleEnchanting.java`, `EnchantingRegistry.java` (Ausschnitte).
- Nicht gelesen: `client/EnchanterScreen.java` nur auf Pakete geprueft (nur `handleInventoryButtonClick` mit `row*16+level` und 100), `client/EnchanterRenderer`, Guide/Discover, Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine eigenen Payloads. Knoepfe: `row*16+level` (Zeile < 3, Angebot vorhanden, Stufe auf `maxAffordable` gekappt), 100 = verzaubern; sonst `false`. Vanilla prueft `stillValid` vor dem Klick. |
| C2 Server-Autoritaet | Angebot (Saat des Spielers), Stufe, Budget, Kosten nur serverseitig; die Schieber sind Server-`DataSlot`s. `enchant` liest Stufe und Bestand erneut und lehnt eine Endgame-Stufe ab, wenn der Tisch seine Spitzenstufe inzwischen verlor. |
| C3 Dupes/Verlust | Item-Slot gehoert dem Menue (max. 1, geht beim Schliessen zurueck); Lapis/Lohenstaub im Block (Abbau: Vanillas Container-Abwurf, Loot = Tisch + Nuggets). Kosten werden serverseitig abgezogen: Stufen per `onEnchantmentPerformed` (neue Saat, wie Vanilla), Lapis/Staub per `removeItem`. Hopper fuellen nur auf, nehmen nie. |
| C3 Stufen/Saat | Nur `in_enchanting_table`, nur unverzauberte Items (`isEnchantable`), nur vertraegliche Kombinationen; Stufe <= Vanilla-Maximum, an der Astral-Spitzenstufe <= Endgame-Stufe (hart <= 5). Gleiche Saat = gleiches Angebot, neue Saat erst nach dem Verzaubern (kein Neu-Wuerfeln durch Ein-/Auslegen). |
| C4 NBT | `ContainerHelper` (2 Slots). |
| C5 Tick-Kosten | Server-Ticker keiner; Regal-/Bodenpruefung (32 + 25 Abfragen) nur beim Item-Wechsel und beim Verzaubern. Partikel nur Client. |
| C6 Leaks | Nur die Endgame-Liste (Registrierung). |
| C7 Sicherheit/Rechte | Keine Befehle, keine Mixins. Upgrade Tisch -> Dark -> Astral ueber SimpleLib (`mayBuild` + `mayInteract`). |
| C8 Unabhaengigkeit | Allein spielbar (Standalone-Ziel); Astral nur mit Simple Ores (Loot-Bedingung, Upgrade-Material). |
| C9 Doku/Pflege | Keine TODO/FIXME, Lang EN=DE (63/63), keine Kern-Dubletten. |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simpleenchanting-P3-1 | P3 | Upgrade Dark -> Astral (gleicher BE-Typ) ersetzt das Block-Entity: Lapis/Lohenstaub fallen als Items heraus statt zu bleiben. `EnchanterBlock` hat kein `shouldChangedStateKeepBlockEntity` (anders als die Oefen, TierFurnaceBlock.java:53); SimpleLibs `InWorldUpgrades.replace` verspricht "keeps its block entity". Nichts geht verloren. | Empfehlung: `shouldChangedStateKeepBlockEntity` -> `oldState.getBlock() instanceof EnchanterBlock` |
| simpleenchanting-P3-2 | P3 (Balance) | Goldapfel -> Verzauberter Goldapfel (40 Stufen, 3 Lapis, 3 Staub) an jedem Enchanter ohne Regal-/Bodenbedingung (EnchanterMenu.java:330, BS25-22 "an beiden gleich"). | Hinweis (Besitzer-Entscheid bestaetigen) |
| simpleenchanting-P3-3 | P3 | Sub-Mod-Schalter blendet nur Tab/Handbuch aus (EnchantingRegistry.java:102/120); gebaute Enchanter arbeiten weiter. | Hinweis |

## Urteil
**shipbar** (0 P1, 0 P2, 3 P3). Das Menue ist serverseitig vollstaendig abgesichert (Budget, Stufen, Bestand, Saat).

## Testergebnis
Lokal: keine Code-Aenderung, daher kein javac-Lauf noetig. sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`.
Nicht getestet: Client (Schieber-Bedienung), Mehrspieler mit zwei Spielern am selben Tisch (Bestand wird bei jedem Klick live geprueft), Forge/NeoForge.
