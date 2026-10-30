# Plan B-TRIALS32 (31.10.2026, Branch b-trials32) - Rest der Prüfungen (Queue TR31-R)

Basis a-bundle82 (8ba4bcaa8), Modul `simplemobs`, nur MC 26.3 Fabric. Baut auf `a-trials31` (Plan `PLAN-A-TRIALS31-2026-10-30.md`, Abschnitt 3 "Nicht gebaut") und den Konzepten
`KONZEPT-A-WP20-2026-10-29.md` (3.2, 3.6, 9.4, R4, R7, R10). Nichts doppelt gebaut: alles hängt an den vorhandenen Klassen `trials/EvilOmen`, `Trials`, `TrialsViewer` und den
Mixins von a-trials31; neu sind `trials/TrialsExtras`, `trials/EvilRaid` und vier kleine Mixins.

## Teile, A12-Notiz (Vanilla-Nutzen / Eingriffsstärke) und Schalter
| # | Teil | Umsetzung | A12 | Config (Standard) |
|---|---|---|---|---|
| 1 | max. 3 aktive Spawner je Struktur | `TrialSpawnerDetectMixin` bricht `tryDetectPlayers` ab, wenn ein nicht aktiver Verwitterter Spawner >= N aktive Nachbarn in 22x22x14 hat; Aktiv-Marken aus `TrialSpawnerEvilMixin.tickServer` (40 Ticks gültig) | Nutzen: Serverlast + Fairness in großen Hallen (8 Spawner). Eingriff gering: nur Verwitterte, Vanilla unberührt | `trialSpawnerCap` 3 (0 = aus) |
| 2 | Ziehen ohne Zurücklegen | `TrialVaultKeysMixin`: Tabelle bis 12x neu würfeln, bis kein Gegenstand doppelt; Rest einmalig | Nutzen: abwechslungsreichere Beute. Eingriff gering: nur Böser/Leere-Tresor; Kategorienregel (min. 2 Kategorien) NICHT gebaut, Begründung: bei 9-10 Einträgen und 3-5 Würfen praktisch immer erfüllt | `trialLootNoRepeat` an |
| 3 | Wither-Treffer St. 2, bremsende Pfeile St. 3, Dunkelheit St. 5 | `CombatEffectsMixin` ruft nach gelandetem Treffer `TrialsExtras.onHit`; Mobs tragen Entity-Tag `simplemobs_evil:<art>:<stufe>` (gesetzt in `EvilOmen.buff`) | nur Mobs aus Böser/Leere-Prüfung/-Überfall, nur gegen Spieler; mittlere Stärke, Opt-in über Omen | `evilHitEffects` an |
| 4 | Leere: Dunkelheit-Aura 6 Blöcke, teleportierende Schützen | `TrialsExtras.aura` (je Sekunde, aus `EvilOmen.serverTick`), Teleport 25 % bei Treffer auf Leere-Schützen (`RangedAttackMob`/`CrossbowAttackMob`) | nur Leere-Mobs (Kette per Config aus) | `voidMobTricks` an (wirkt nur bei `voidChain`) |
| 5 | Evil an Vanilla-Spawnern | `EvilOmen.turn` nimmt `Blocks.TRIAL_SPAWNER`; Art/Stufe im Speicher (`VANILLA`, Blockstate gibt es nicht) - nach Server-Neustart vergessen (offen, siehe Rest) | stark (verändert Vanilla-Kammern) -> Standard AUS | `evilOnVanillaSpawners` aus |
| 6 | Struktur-Schalter + Verfall | `StructureWeatheredMixin` (Structure.generate -> INVALID_START), `BlockRotProcessorMixin` (nur block_rot mit Tag `simplemobs:weathered_rottable`) | nur eigene Struktur | `weatheredStructures` an, `weatheredDecayPercent` 11 |
| 7 | Evil-Raid Extra-Welle + Plünderer-Truhe | `RaidsEvilMixin` (createOrExtendRaid RETURN) + `RaidEvilMixin` (`numGroups++`, wird mit dem Raid gespeichert); Sieg -> Vanilla-Truhe mit `simplemobs:raid/<art>_<stufe>` nahe Glocke | nur Spieler mit laufendem Bösem Überfall (Opt-in) | `evilRaidExtras` an |
| 8 | JEI-Beute mit Gewichten | `assets/simplemobs/loot_info/trials.json` (vom Strukturgenerator aus denselben Zeilen wie die Tabellen), `TrialsViewer.lootPages` = 12 Info-Seiten (Void nur bei Kette an) | - | - |

## Abweichungen vom Konzept (begründet)
- Entity-Tag statt Attribut-Lesen als Erkennung der Böse-Mobs (persistiert in NBT, keine eigene Komponente).
- "Plünderer-Truhe" = Vanilla-Truhe mit Loot-Tabelle (kein eigener Block; Aufwand/Nutzen).
- JEI "Kategorie" als Info-Seiten (eine je Tabelle) statt eigener Rezept-Kategorie: Tabellen sind rein dekorativ, SimpleLib hat dafür `RecipeViewer.Info`.
- Raid-Markierung (`EVIL_RAIDS`) liegt im Speicher: nach Neustart mitten im Überfall fehlt die Truhe (Extra-Welle bleibt, weil `numGroups` gespeichert wird).

## Test
Gametests (`TrialsTests` + Adapter): `trials_spawner_cap`, `trials_loot_distinct`, `trials_hit_effects`, `trials_void_tricks`, `trials_vanilla_spawner`, `trials_raid_extras`,
`trials_structure_switch` (inkl. Mixin auf echtem `BlockRotProcessor`), `trials_loot_pages`; `trials_loot` um die Raid-Truhen-Tabellen erweitert. Ein sbt-Lauf: Compile 26.3 Fabric
+ simplemobs, `module-simplemobs-fabric-263` + `-standalone`, `check_client_mixins.py`. Nicht prüfbar: echte Weltgenerierung mit Schalter, echter Überfall bis zum Sieg, Client-Sicht der JEI-Seiten.
