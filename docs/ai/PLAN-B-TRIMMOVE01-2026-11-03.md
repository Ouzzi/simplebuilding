# PLAN B-TRIMMOVE01 – Besatz-Fähigkeiten physisch nach Simple Mobs (ZO9 A / MOB27-3 Stufe C, 03.11.2026)

Branch `b-trimmove01`, Basis `hub/claude-wave1` 4835b4abe (= Production). Vorlauf: `docs/ai/PLAN-B-MOB32-2026-10-31.md`
(Stufe A/B: Fähigkeiten in Simple Mobs, Kern = Rückfall + Gastgeber), `docs/ai/PLAN-B-ZONES02-2026-11-01.md` (ZO9: Simple Mobs und
Simple Resonance Pflicht-Sub-Mods von SB).

## 1. Zielmodul (Ableitung)

Frage ZO9 (hub31/gen/build_abnahme.py) hatte die Optionen A „Besatz-Fähigkeiten nach Simple Mobs (Pflicht-Sub-Mod von SB)“ und
B „in Simple Resonance statt Mobs“. Besitzer-Antwort 01.11.: alle ZO = A. Ziel ist also **Simple Mobs** (Wirkung der Fähigkeiten);
Simple Resonance bleibt die Messung (Spielerwert, Zonen), MOB27-3-Wortlaut: „Fähigkeits-Anteil der Rüstungsbesätze nach Simple Mobs,
kosmetische Besätze bleiben in Simple Trims“.

## 2. Ist-Stand (erhoben)

- Simple Mobs hat die Regeln schon 1:1 (`trim/TrimAbilities`, `TrimAbilityAttributes`, Mixins `TrimAbilityLivingMixin`/`TrimAbilityPlayerMixin`,
  8 Tests `trim_ability_*`), **nur auf Fabric installiert** (Claim). Der Kern hat dieselben Regeln noch als Rückfall: `util/TrimEffectUtil`
  (463 Z.), `util/TrimAttributeHandler`, Hooks in `mixin/LivingEntityMixin`/`PlayerEntityMixin` hinter `TrimHost.handledElsewhere()`.
- Der Kern-Code liegt in `common/src/shared` = 26.2 **und** 26.3. 26.2 hat keine Module (Simple Mobs gibt es dort nicht).
- Kern-Anzeige (`TrimBonusCatalog` → Tooltip `mixin/client/ItemMixin`, `TrimReferenceScreen`) liest nur die Raten-Konstanten aus
  `TrimEffectUtil`; Messung (`TrimMultiplierLogic`, Tracker, Payloads, Panel) ist Kern.
- Tests: `TrimEffectTests` (8), `TrimBonusTests` (8) prüfen die Regeln direkt über `TrimEffectUtil`; `TrimWiringTests` (14) davon 4
  Hook-Tests (Mixins liefern Tempo/Hunger/XP, jeder Treffer läuft durch die Minderung, Luft/Sichtbarkeit, Tick-Takt), 10 Kern-Tests
  (Messung, Speicherung, Befehl, Materialfarben, Panel).
- Keine Welt-Daten: Boni sind rein berechnet, Attribut-Modifikatoren flüchtig mit gleichen IDs `simplebuilding:trim_*` (MOB32 E3).

## 3. Entscheidungen (selbst getroffen)

- **T1 Kein IdMove/keine Migration.** Es zieht kein Item/Block/Komponente/Gespeichertes um (s. o.). Die Attribut-IDs bleiben
  `simplebuilding:trim_*` (flüchtig, nie gespeichert; gleiche IDs verhindern Doppel-Modifikatoren bei alten Spielständen mit Kern-Werten).
- **T2 26.3: Kern ohne Fähigkeits-Code.** `TrimEffectUtil` und `TrimAttributeHandler` verlassen `common/src/shared` und liegen nur noch
  in `common/src/mc26_2` (26.2-Linie, eingefroren bis zum Port-Run, dort ziehen sie mit Simple Mobs um). Die Kern-Hooks wandern aus den
  gemeinsamen Mixins in eigene Mixins `LivingEntityTrimMixin`/`PlayerTrimMixin`: voll in `mc26_2`, leerer Zwilling in `mc26_3/overlay`
  (Muster `LivingEntityExtraHeadMixin`, die Mixin-Liste ist gemeinsam). Auf 26.3 wirkt damit nur noch Simple Mobs.
- **T3 Anzeige-Raten bleiben im Kern** als `util/TrimRates` (nur Konstanten, `isOwn`, `patternWeight`, Anzeige-Resonanz). Der Kern darf
  Simple Mobs nicht importieren (Regel 3), die Anzeige (Tooltip, Nachschlage-Bildschirm, Panel) ist Kern-UI; Umzug der UI braucht
  Client-Abnahme (UI erst exemplarisch) → offen. Gegen Auseinanderlaufen: beide Seiten pinnen dieselbe Tabelle aus docs/TRIM-BALANCE.md
  (Simple Mobs `trim_ability_table`, Kern neuer Test `trim_rates_game_test_display_rates_match_the_balance_table`).
  26.2-`TrimEffectUtil extends TrimRates` (Konstanten-Zugriffe bleiben gültig).
- **T4 Alle Loader.** Ohne Kern-Rückfall muss Simple Mobs auf NeoForge/Forge 26.3 ebenfalls installieren (`TrimAbilities.install()` in
  `MobsNeoForge`/`MobsForge`) und der Kern dort Gastgeber sein (`TrimHost.install()` in `SimplebuildingNeoForge`/`SimplebuildingForge`).
  Die Mixins von Simple Mobs laden dort schon (`simplemobs.mixins.json`). `TrimHost.handledElsewhere()` entfällt (keine Aufrufer).
- **T5 Sub-Mod aus = Fähigkeiten aus.** Simple Mobs ist Pflicht-Sub-Mod (ZO9); schaltet der Spieler „Enable Simple Mobs“ ab, wirken keine
  Besatz-Fähigkeiten mehr (vorher Kern-Rückfall). Folgt aus ZO9 A („Fähigkeiten gehören zu Simple Mobs“).
- **T6 Tests umziehen.** `TrimEffectTests`, `TrimBonusTests` und die 4 Hook-Tests aus `TrimWiringTests` ziehen als
  `TrimRuleTests` (Simple Mobs, 20 Tests `trim_rule_*`) um: gleiche Körper, `TrimEffectUtil`→`TrimAbilities`,
  `TrimAttributeHandler`→`TrimAbilityAttributes`, Resonanz statt über die Kern-Config über einen Test-Gastgeber (`TrimContracts.Host`,
  alter Gastgeber wird zurückgesetzt), Spieler-Schalter über den Gastgeber, SB-Materialien (astralit/enderite/nihilith) über ihren
  Registry-Schlüssel (`Holder.Reference.createStandAlone`, falls SB fehlt – die Regeln lesen nur den Schlüssel). Anzeige-Prüfungen
  (`TrimBonusCatalog`) bleiben im Kern. Kern-Katalog/Fabric-Adapter verlieren die 20 Einträge. Die 26.2-Linie verliert diese Tests bis
  zum Port-Run (dort kommen Code und Tests mit Simple Mobs; 26.2 wird derzeit nicht getestet, Regel „nur 26.3 Fabric“).
- **T7 Offen bleiben (bewusst):** Messungs-Zeiger R2 (Tracker/Payloads/Panel → Simple Resonance), eigene Stärken-Config in Simple Mobs
  (allein 1,0), Anzeige-UI nach Simple Mobs, Panel-Zeile.

## 4. Änderungen

1. `common/src/shared/.../util/TrimRates.java` neu; `TrimEffectUtil.java`, `TrimAttributeHandler.java` → `common/src/mc26_2/...`.
2. `LivingEntityMixin`/`PlayerEntityMixin` (shared): Besatz-Hooks raus; `mixin/LivingEntityTrimMixin` + `PlayerTrimMixin` (mc26_2 voll,
   mc26_3/overlay leer), `simplebuilding.mixins.json`.
3. `TrimBonusCatalog`, `mixin/client/ItemMixin`: `TrimRates`. `TrimHost` (beide Linien): `handledElsewhere` raus, Doku.
4. NeoForge/Forge: Kern `TrimHost.install()`, Simple Mobs `TrimAbilities.install()`.
5. Tests: `modules/simplemobs/.../TrimRuleTests.java` + `MobsTests`/`ModuleGameTest`; Kern: Trim-Test-Dateien entsprechend
   gekürzt/gelöscht, Katalog + Fabric-Adapter, neuer Raten-Test.
6. Doku: PRINZIPIEN-MODUL-UNABHAENGIGKEIT (Stand), docs/MODS-UND-FEATURES.md, Queue ZO9/MOB27-3, Memory.

## 5. Risiken

- Kein lokaler Compile (sb-agent): ein sbt-Lauf + ein Nachlauf. Umzug mechanisch, danach grep auf Kern-Reste.
- Testspieler im Simple-Mobs-Lauf ohne SB: SB-Materialien nur als Schlüssel; Netz-Sync der Ausrüstung im Nihilith-Test (connection.tick)
  liest den Wert nicht (nur Schlüssel). Fällt das, Nachlauf.
- Forge/NeoForge: keine Laufzeittests (nur Compile, Regel „nur 26.3 Fabric“).

## 6. Verifikation

Lokal: Prüfskripte aus dem B02-Brief. Ein sbt-Lauf: Compile aller Ziele (`:compileJava :mc26_3:fabric:compileJava
:mc26_3:neoforge:compileJava :mc26_3:forge:compileJava`, Module simplemobs fabric/neoforge/forge), `module-simplemobs-fabric-263`
+ standalone, `fabric-263 --filter 'simplebuilding:trim*'`, `fabric-263 --filter 'simplebuilding:data_integrity*'`,
`check_client_mixins.py`, `wiki/generate.py --all --check`.
