# Plan A-TRIALS31 (30.10.2026, Branch a-trials31) - Verwitterte Prüfungen, Böses Omen, Leere-Kette

Queue BS27-2/3/5/6 + ABN30-11 (Priorität 3), Paket WP-20, Modul `simplemobs`, nur MC 26.3 Fabric.
Konzept: `docs/ai/KONZEPT-A-WP20-2026-10-29.md` (inkl. Runde 2). Besitzer-Wahl: Grundrisse + Omen-Ablauf abgenommen (ABN29b-1), Spawner-Textur B,
Tresor-Textur B (A im Generator als `B_BROKEN = False`), Evil Potion = Bad-Omen-Flasche + Netherherz nur im Braustand mit Seelenfeuer-Lohenstaub (3 je Herz,
BS27-5), Void-Kette per Config aus (R9). Übrige Rückfragen wie im Konzept empfohlen (R1 simplemobs, R3 1 normaler Tresor, R4 Evil nicht an Vanilla-Spawnern,
R5 Schlüssel trägt die Stufe, R6 einmal je Tresor, R7/R8 wie 3.6, R10 Spawner-Obergrenze nicht gebaut).

## 1. Ist-Stand (vorhandene Lösungen genutzt, nichts neu erfunden)
- Vanilla-Trial-Spawner/-Tresor samt Logik werden **nicht kopiert**: `WeatheredTrialSpawnerBlock extends TrialSpawnerBlock`, `TrialVaultBlock extends VaultBlock`; die
  Vanilla-Block-Entity-Typen kennen die neuen Blöcke über ein Mixin (`BlockEntityTypeTrialsMixin`, gleiche Technik wie die Lohenköpfe in SB).
- Strukturen: Jigsaw-Datenpaket nach dem Muster der End-Strukturen (`tools/structures/generate_end_structures.py`): NBT-Vorlagen aus Python, Verfall per
  Vanilla-Prozessor `block_rot` (89 % bleiben) + `rule`, Mob-Zusammensetzung als Vanilla-`trial_spawner`-Konfigurationen (Datenpaket).
- Tresor-Umwandlung: `simplelib:in_world_upgrades` (Hammer oder Axt, Material in der Nebenhand, `strikes_per_material`) - keine eigene Schlag-Logik; JEI-Seite von SimpleLib.
- Effekte Flüchtig/Ablenkung/Spiegel aus `CombatEffects` (MOB27-1, in master), JEI über `RecipeViewer.special/info`, Texturen über `vanilla_derived.py` (Build-Ableitung).

## 2. Umsetzung (Dateien)
| Teil | Dateien |
|---|---|
| Blöcke/Items/Effekte | `trials/Trials.java`, `WeatheredTrialSpawnerBlock`, `TrialVaultBlock`, `TrialKind`; Sinks in `WitherMobs.blocks/items`, Effekte in `MobsFabric` |
| Struktur/Daten | `tools/structures/generate_weathered_trials.py` -> `data/simplemobs/{structure,worldgen,trial_spawner,loot_table,tags,in_world_upgrades}`, `assets/simplemobs/{blockstates,models,items}` |
| Texturen | `tools/textures/trials_2026_10_30.py` (75 Texturen, nur Regeln im Repo), `vanilla_derived.py` `MODULE_OUTPUTS['simplemobs']`, `.gitignore` |
| Braustand | `mixin/BrewingStandSoulMixin`, `trials/EvilBrewing`, `SoulBrewing` (Seelenladungen 20 je Staub, 1 je Evil/Void-Brauvorgang) |
| Evil Trial/Raid | `mixin/TrialSpawnerDetectMixin` (Omen erkennen), `TrialSpawnerEvilMixin` (Mob-Buffs, Schlüssel-Belohnung, Partikel, Reset), `trials/EvilOmen` (inkl. Raid) |
| Evil Vault | `mixin/TrialVaultKeysMixin` (Schlüssel jeder Stufe, Beute nach Stufe), `trials/TrialsHooks` (Handler der Umwandlung) |
| JEI/Doku | `trials/TrialsViewer`, `wiki/manual.json` (3 Features), Lang EN/DE, `MobsDiscover` (generiert), Handbuch/Wiki per Generator, `docs/MODS-UND-FEATURES.md` |
| Tests | `TrialsTests` (10) + Adapter in `ModuleGameTest` |

## 3. Entscheidungen / Abweichungen vom Konzept (begründet)
1. **Schlüssel**: 5 Items je Tier (`evil_trial_key_1..5`) statt einer Komponente: Vanilla vergleicht den Schlüssel mit `isSameItemSameComponents`; ohne eigene Komponente
   (Netzwerk/Codec-Risiko) sind Stufen als Items robuster. Der Tresor nimmt jede Stufe an (Mixin), die Beute folgt der Stufe. Gleiches bei den Tränken (Stufe = Item).
2. **Seelenladung** nur für Evil/Void-Rezepte; normale Rezepte nutzen weiter Vanilla-Brennstoff (Konzept 3.1 wollte auch Seelenladung als Ersatz). Weniger Eingriff.
3. **Void-Tresor nur aus Evil-Tresor** (eine `from`-Zeile je Block in `in_world_upgrades`); Konzept erlaubte auch direkt aus dem Verwitterten. Weg: Tresor -> Evil (Diamantblock) -> Void (Netheritblock).
4. **Kein "Evil Trial Omen"-Effekt**: das Evil Omen wird beim Betreten verbraucht und macht den Spawner direkt böse (Block-Zustand `kind`/`level`).
5. **Evil Raid** = Vanilla-Plünderung der Stufe (Raid Omen) im Dorf + 30 min Buffs für Plünderer in 160 Blöcken. Nicht gebaut: Extra-Welle, Plünderer-Truhe.
6. **Nicht gebaut**: Wither-Treffer Stufe 2, bremsende Pfeile Stufe 3, Dunkelheits-Aura/teleportierende Fernkämpfer (Void), Spawner-Obergrenze 3 aktive (R10),
   "ohne Zurücklegen"-Ziehung und Kategorienregel (Loot-Tabellen können das nicht; Gewichte/Würfe aus Runde 2 stehen), Evil an Vanilla-Spawnern (R4: aus, daher keine Config),
   Konfig für Verfall/Struktur-Schalter (Verfall steckt im Datenpaket; wer die Struktur nicht will, entfernt das Strukturset per Datenpaket), JEI-Tresor-Beute-Kategorie mit Gewichten (Info-Seiten + Wiki).
7. **Texturen**: Variante B (Ecke aufgebrochen); Vanilla-Raster/Helligkeit bleibt, Farbrampen Verdigris/Türkis(ominös)/Glutrot/Violett; 12 Töne je Sprite. Harte Übergänge 5-40 %
   (die Vanilla-Sprites selbst liegen bei 15-30 %). Nicht im Spiel gesehen.

## 4. Verifikation
`/root/sbt a-trials31 "…"` einmal am Ende: Generatoren (Struktur `--check`, Wiki/Handbuch/Discover), Compile `:mc26_3:fabric` + `:modules:simplemobs:fabric`, Targets
`module-simplemobs-fabric-263` + `module-simplemobs-standalone-fabric-263`, `./gradlew check -q`. Nicht getestet: Client-Sicht (Texturen, Partikel, JEI-Seiten), echte Weltgenerierung
(nur Vorlagen/Registrierung/Platzieren per `/place`), Hammer-Schläge im Spiel (die Umwandlung wird über `InWorldUpgrades.replace` und den Eintrag getestet), NeoForge/Forge.
