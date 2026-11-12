# Audit simplecrucibles (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991 (Buendel 186), nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases).
Umfang: 60 Java-Dateien / 5,9k Zeilen ohne Tests (mit Tests 65 / 8,3k). Risikogeleitet geprueft (P1-Klassen zuerst),
Aufbau wie `modules/simplestorage/AUDIT.md`. Fruehere Funde sind im Code vermerkt (REV2-A-4a Ueberlauf beim Laden, REV2-A Abenteuermodus).

## Vollstaendig gelesen
- Menue: `crucible/CrucibleMenu.java` (Slots, Schnellverschieben, Geister-/Fass-Slots, `stillValid`), dazu `StackLimits.LimitedSlot` (SimpleLib).
- BlockEntity/Container: `crucible/CrucibleBlockEntity.java` (Tick, Reservierung, Ergebnis, Erfahrung, Hopper-Seiten, Abbau, Laden/Speichern, Sync),
  `crucible/CrucibleJob.java`, `crucible/Heat.java`, `crucible/CrucibleTier.java`.
- Bloecke/Wege: `crucible/CrucibleBlock.java`, `crucible/CrucibleBlankBlock.java` (+ Loot-Tabelle `crucible_blank`, Tags `crucible_walls/handles`),
  `crucible/CrucibleUpgrades.java`, `crucible/BarrelAttach.java`, `crucible/AxeWays.java`, `crucible/HammerWays.java`, `api/SimpleCruciblesApi.java`.
- Kessel/Fluessigkeit: `cauldron/ReinforcedCauldronBlock.java`, `cauldron/ReinforcedCauldrons.java`, `fluid/ReinforcedCauldronBuckets.java`,
  `fluid/SoulLava.java`, `fluid/SoulLavaBlock.java`, `fluid/SoulLavaFluid.java` (Quelle/Fliessen/Feuer), `fluid/CrucibleFluids.java`, `effect/SoulBurnEffect.java`.
- Mixins (alle 9): `AxeBuildMixin`, `CopperBucket{ItemEntity,Weathering,FrameCare}Mixin`, `SoulLava{Spring,Fortress}Mixin`, `SoulLavaFogMixin` (Client-Liste),
  Accessoren `StructurePieceAccessor`, `TemplatePoolAccessor`; `SimpleCrucibles.java`, `CruciblesFabric.java`, `village/VillageKitchen.java`.
- Loot-Tabellen aller Bloecke, Mixin-Konfiguration.
- Nicht gelesen: Client (`client/*`, `CrucibleScreen` nur auf Pakete geprueft: nur Vanilla-Klicks), JEI/Jade/Viewer (`compat/*`), Guide/Discover,
  `CrucibleConfig` nur Grenzen/Klemmung, Tests.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine eigenen Payloads. Das Menue nutzt nur Vanilla-Klicks, kein `clickMenuButton`. S2C: Block-Update mit vollem Inhalt (P3-1). |
| C2 Server-Autoritaet | `stillValid` ueber `BaseContainerBlockEntity` (Abstand), Fass-Ansicht haengt am Tiegel. Kochen, Reservieren, Erfahrung nur serverseitig; Client-Klassen nur in `client/`, Nebel-Mixin in der Client-Liste. |
| C3 Dupes/Verlust | Schnellverschieben ohne Endlosschleife, Geister-Slots ohne Aufnahme/Ablage (auch Zifferntaste, Werfen, Doppelklick). Abbau: Inhalt per `preRemoveSideEffects`, Loot-Tabelle nur der Block (keine Doppelung), Aufwertungs-Einheiten kommen zurueck, Upgrade leert den alten Block vor dem Ersetzen. Rohling: Loot = Kessel + 1 Eisenbarren je Schlag (Tags nur Eisenbarren). Kessel/Eimer: Giessen/Schoepfen erhaelt die Menge, Seelenlava ohne Quellbildung. **P2-1, P2-2 gefunden und gefixt.** |
| C4 NBT | `SafeCodecs.listLenient`, Slot-Bereich und Menge geprueft, Ueberlauf faellt beim ersten Tick (P3-4). Ziele/Ergebnis-Felder bereichsgeprueft. |
| C5 Tick-Kosten | Hitze nur bei Nachbaraenderung bzw. alle `heatRecheckTicks`. **P2-3 gefunden und gefixt** (Rezeptsuche je Tick). Seelenbrand: 2 Blockabfragen je betroffenem Wesen und Tick. |
| C6 Leaks | Nur Registries/Listener und `CrucibleUpgrades.CAULDRON_PROGRESS` (ab 256 Eintraegen bereinigt, 30 s). |
| C7 Sicherheit/Rechte | Axt-Weg prueft `mayBuild` (Mixin am Kopf von `useOn`), Spawnschutz/Claims ueber Vanillas `mayInteract` im Paket-Handler. Mixins ohne `@Overwrite`; Weltgen-Mixins nur in neuen Chunks. Rahmen-Pflege prueft `mayInteract` + `mayUseItemAt`. |
| C8 Unabhaengigkeit | Allein spielbar (Standalone-Ziel + Combos mit Hammers/Storage/Tools), Partner nur ueber `com.simplelib.api`. |
| C9 Doku/Pflege | Keine TODO/FIXME, Lang EN=DE (92/92), keine Kern-Dubletten in `tools/dup_pairs.txt`. |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simplecrucibles-P2-1 | P2 (Verlust) | Erhoehte Stapel (Enderit-Tiegel, angedocktes Enderit-Fass: bis 128) verliessen den Slot ganz: Zifferntaste legte alle 128 in die Hotbar, ein Klick mit anderem Item gab sie dem Cursor. Mehr als 99 kann der Spieler nicht speichern (`ItemStack.CODEC` 1..99) - der Stapel ist nach dem naechsten Speichern weg. `LimitedSlot` begrenzt nur `tryRemove` (CrucibleMenu.java:284/316). | **gefixt**: `CrucibleMenu#clicked` ruft die neue SimpleLib-Wache `StackLimits.guardOversizedClick` (Regel wie Simple Storages Stufen-Truhe) - Zifferntaste nimmt einen normalen Stapel in einen leeren Hotbar-Platz, Klick mit anderem Item abgelehnt. Gametest `oversized_stack_never_leaves_whole` |
| simplecrucibles-P2-2 | P2 (Logik) | Ergebnisse gehen zuerst ins angedockte Fass; Entnehmen aus den Fass-Feldern zahlte die gespeicherte Erfahrung nicht aus (nur `CrucibleSlot#onTake`). Mit Fass kam die Erfahrung erst beim Abbau des Tiegels (Besitzer F20: beim Entnehmen). | **gefixt**: `BarrelSlot#onTake` zahlt aus (CrucibleMenu.java:331). Gametest `barrel_field_take_pays_experience` |
| simplecrucibles-P2-3 | P2 (Server-Last) | Slots ohne Rezept (Lager-Nutzung, Fremd-Items) suchten jeden Tick neu: 4 Rezepttypen x bis 30 Slots je Tiegel und Tick, auch ohne Hitze. | **gefixt**: Merker `noRecipe`, neue Suche nur bei neuem Item, anderer Hitze oder der regulaeren 20-Tick-Pruefung (CrucibleBlockEntity.java:203). Gametest `no_recipe_slots_search_seldom` |
| simplecrucibles-P3-1 | P3 (Sync) | `getUpdateTag` schickt den ganzen Inhalt samt Erfahrung/Einheiten bei jedem fertigen Job und alle 20 Ticks, solange warmes Essen drin liegt (`keepWarm` meldet immer "geaendert", CrucibleBlockEntity.java:191). | Empfehlung: `keepWarm` ohne Sync (nur `setChanged`) |
| simplecrucibles-P3-2 | P3 | Zifferntausch/Klick-Tausch auf einem Ergebnis-Slot: Vanilla leert den Slot vor `onTake`, das Ergebnis-Flag ist dann weg - die Erfahrung bleibt gespeichert und kommt beim naechsten Entnehmen/Abbau. | Hinweis |
| simplecrucibles-P3-3 | P3 (Balance) | Seelenlava macht beruehrende Wasserquellen zu Quarzbloecken (SoulLavaBlock.java:64, Besitzer 26/27): mit endloser Wasserquelle ein Quarz-Generator. | Hinweis (Besitzer-Entscheid bestaetigen) |
| simplecrucibles-P3-4 | P3 | Ueberlauf aus manipulierten Spielstaenden ohne Obergrenze (CrucibleBlockEntity.java:637), faellt als viele Item-Stapel. Nur per NBT-Bearbeitung erreichbar. | Hinweis |
| simplecrucibles-P3-5 | P3 (Nachbarmodul) | Dieselbe Luecke wie P2-1 hat Simple Storages `BarrelMenu` (Enderit-Fass x2, `LimitedSlot` ohne Tausch-Schutz); `StackLimits.LimitedSlot` verspricht "nothing leaves it larger" und haelt das nur fuer `tryRemove`. | **gefixt** (Hauptagent): `BarrelMenu#clicked` nutzt dieselbe SimpleLib-Wache; Gametest `barrel_oversized_stack_never_leaves_whole` (Simple Storage), siehe `modules/simplestorage/AUDIT.md` simplestorage-P2-3 |
| simplecrucibles-P3-6 | P3 | Schalter "Enable Simple Crucibles" blendet nur Tab/Handbuch/Dorfkueche aus; gebaute Tiegel arbeiten weiter (Rahmen-Semantik aller Sub-Mods). | Hinweis |

## Urteil
**shipbar nach Fix** (0 P1, 3 P2 gefixt, 6 P3). Laden/Speichern, Abbau und Upgrade sind sauber gegen Verlust/Doppelung abgesichert.

## Testergebnis
Lokal: `javac.sh simplecrucibles` fuer `CrucibleMenu`, `CrucibleBlockEntity`, `CrucibleTests`, `ModuleGameTest` - nur bekannte
Access-Widener-Meldungen (in `CrucibleTests.java:302/305` vorbestehend, nicht in den neuen Zeilen). sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`.
Nicht getestet: Client (Klick-Vorhersage nach abgelehntem Tausch), echter Logout mit 128er-Stapel, Fass-Test mit Simple Storage (der Gametest ruft `onTake` des Fass-Slots direkt), Forge/NeoForge.
