# Trainingspuppe – alle Spieler-Interaktionen mit echten Wegen (2026-10-08)

Auftrag: (1) jede Spieler-Interaktion auf Stroh-Ruestungsstaender und Trainingspuppe
inventarisieren, (2) je Interaktion einen GameTest schreiben, der den **echten**
Vanilla-Weg benutzt (kein direktes `hurtServer(...)`-Aufrufen), (3) rote Tests im
Puppen-Code beheben – nie den Test aufweichen, (4) Ergebnis hier berichten.

Basis: `docs/ai/PLAN-TRAINING-DUMMY-2026-10-02.md` (Soll-Verhalten),
`common/src/shared/java/com/simplebuilding/dummy/TrainingDummy.java`.

## 1. Betroffener Code (Ist-Zustand)

| Datei | Rolle |
|---|---|
| `common/src/shared/java/com/simplebuilding/dummy/TrainingDummy.java` | eine Klasse, zwei Entity-Arten (`straw_armor_stand`, `training_dummy`); `hurtServer`, `interact`, `showHit`, `hurtStraw`, `breakApart`, Zahlen/Summe |
| `common/src/shared/java/com/simplebuilding/mixin/PlayerAttackDummyMixin.java` | notiert beim Schlag die Waffenstufe: `dummy.noteAttack(self, self.getAttackStrengthScale(0.5F))` |
| `common/src/shared/java/com/simplebuilding/gametest/TrainingDummyTests.java` | bestehende Tests |
| `common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java` | Registrierung ab Zeile 660 |

## 2. Vanilla-Wege (per `javap` am 26.3-deobf-Beleg nachgewiesen, keine Quellen dekompiliert)

| Weg | Beleg |
|---|---|
| Nahkampf | `ServerGamePacketListenerImpl.handlePlayerAction`/`handleAttack` -> `Player.attack(Entity)` -> `hurtOrSimulate` -> **auf `ServerLevel` immer `hurtServer`** (`Entity.hurtOrSimulate` waehlt Server/Client) |
| `Player.attack` wird nicht abgefangen | `ArmorStand.skipAttackInteraction` ist nur true, wenn `level.mayInteract(...)` false liefert; `ServerLevel.mayInteract` = `!isUnderSpawnProtection(...) && worldBorder`, und `MinecraftServer.isUnderSpawnProtection` liefert konstant `false` -> Angriffe landen immer |
| Speer-Stich | `handlePlayerAction` (Case 1) -> `stack.get(DataComponents.PIERCING_WEAPON)` -> `PiercingWeapon.attack(LivingEntity, EquipmentSlot)` -> `ProjectileUtil.getHitEntitiesAlong(user, AttackRange, canHitEntity, ClipContext.Block.COLLIDER)` -> je Treffer `LivingEntity.stabAttack(...)` -> **`target.hurtServer(...)`** |
| Speer-Hindernis | `PiercingWeapon.canHitEntity` = `!isInvulnerableToPiercingWeapon()` (=`isInvulnerable()`) && `isAlive()` && `canBeHitByProjectile()` (=`isAlive() && isPickable()`); `ArmorStand.isPickable` = `LivingEntity.isPickable && !isMarker` -> Puppe ist treffbar |
| Stich-Schaden | `PiercingWeapon.attack` nimmt `getAttributeValue(ATTACK_DAMAGE)` (Ladung zaehlt nicht), `stabAttack` rechnet `EnchantmentHelper.modifyDamage` und ruft `hurtServer` auf |
| Speer als Spielerangriff | `data/minecraft/tags/damage_type/is_player_attack.json` enthaelt `minecraft:spear` -> `TrainingDummy.hurtServer` erkennt den Brecher auch beim Stich |
| Pfeil | `Projectile.tick` -> `Projectile.canHitEntity` -> `canBeHitByProjectile` -> `entity.hurtOrSimulate` -> `hurtServer`; Quelle `minecraft:arrow` (nicht `is_player_attack`) |
| Krit | `Player.canCriticalAttack`: `fallDistance>0 && !onGround && !onClimbable && !inWater && !isMobilityRestricted && !isPassenger && Ziel ist LivingEntity && !sprinting`; `Player.isSweepAttack(fullStrength, sprinting, knockback)` verlangt `fullStrength && !sprinting && !knockback && onGround && Abstand < (Speed*2.5)^2 && Item in ItemTags.SWORDS` |
| Waffenstufe | `Player.tick` zaehlt `attackStrengthTicker` hoch, `Player.onAttack` setzt ihn zurueck; `getAttackStrengthScale(0.5F) = (ticker+0.5)/delay`. Der Mixin liest **vor** dem Reset genau den Wert, den auch `Player.attack` fuer Krit/Sweep benutzt |
| Ladung fuer den Stich | `Player.cannotAttackWithItem(stack, 5)` prueft `MINIMUM_ATTACK_CHARGE` – dieser Gate sitzt in `handlePlayerAction` (Netzwerk), nicht in `PiercingWeapon.attack` |
| Explosion | `Level.explode(...)`, Quelle `IS_EXPLOSION` |
| Commando-Tod | `DamageSources.genericKill()` traegt `BYPASSES_INVULNERABILITY`; `TrainingDummy.hurtServer` delegiert an `super.hurtServer` -> Vanilla `ArmorStand` toetet |
| Ausruesten | `ArmorStand.interact` -> `getEquipmentSlotForItem` -> `swapItem`; leerer Hand <-> Ruestung aus dem getroffenen Slot |

## 3. Inventar der Spieler-Interaktionen

| # | Spieler-Interaktion | Echter Weg | Soll-Verhalten | Sichtbare Reaktion | Test (neu, echter Weg) |
|---|---|---|---|---|---|
| 1 | Nahkampf-Schlag (Hand/Axt) auf die Puppe | `Player.attack` -> `hurtServer` | nie Schaden, nie Tod; Zahl, Wackeln, Zahl-Entity | Zahl > 0, Lebenspunkte unveraendert | `meleeHitsCountThroughTheRealPath` |
| 2 | Fallender voll geladener Schlag | `Player.attack` mit `canCriticalAttack` | Zahl rot/fett als Krit | `lastCrit()` | `aFallingChargedMeleeHitShowsACrit` |
| 3 | Voll geladener Schlag mit Schwert neben zweiter Puppe | `Player.attack` + `doSweepAttack` | Sweeping trifft die Nachbarpuppe mit | Nachbar hat `sessionHits()==1` | `aSweptMeleeHitReachesTheNeighbourDummy` |
| 4 | Schleich-Schlag (auch mit Speer) | `Player.attack`/`PiercingWeapon.attack` -> `hurtServer` -> `breakApart` | Puppe abgebaut, wirft Puppe + Kopf ab | `isRemoved`, Drops | `sneakingMeleeHitsPickTheDummyUpThroughTheRealPath` |
| 5 | Speer-Stich | `PiercingWeapon.attack` -> `stabAttack` -> `hurtServer` | Zahl wie Nahkampf | Zahl > 0, steht | `spearThrustsCountAndBreakOnlyWhileSneaking` |
| 6 | Pfeil (echtes Projektil, tickt) | `AbstractArrow.tick` -> `hurtServer` | Zahl; Krit-Pfeil rot | `sessionHits()==1`, `lastCrit()` beim Krit-Pfeil | `arrowsTickIntoTheDummyAndShowTheirNumber` |
| 7 | Explosion | `Level.explode` -> `hurtServer` | Puppe bleibt (Zahl), Stroh-Staender zerfaellt mit Drops | Stroh entfernt + Drop, Puppe steht | `anExplosionBreaksTheStrawStandButOnlyNumbersTheDummy` |
| 8 | Rechtsklick mit Ruestung / leerer Hand | `ArmorStand.interact` -> `swapItem` | anziehen und abnehmen | Slot/Hand vertauscht | `rightClicksDressAndUndressTheDummy` |
| 9 | Zwei schnelle Schlaege auf den Stroh-Staender | `Player.attack` -> `hurtStraw` (5-Tick-Fenster) | zweiter Schlag baut ab, mit Drop | entfernt + Drop | `twoFastPlayerHitsBreakTheStrawStand` |
| 10 | Kreativ-Schlag | `Player.attack` -> `hurtStraw`/`breakApart` | sofort, ohne Drops | entfernt, kein Drop | `creativePlayerHitsBreakBothStandsWithoutDrops` |
| 11 | Commando-Tod / Void | `DamageSources.genericKill` -> `super.hurtServer` | beide verschwinden | `isRemoved` | `genericKillRemovesBothStands` |
| 12 | Kuerbis auf den Stroh-Staender, Schere zurueck | `TrainingDummy.interact` | wie bisher | Puppe <-> Stroh | bestehend: `pumpkinTurnsTheStrawStandIntoTrainingDummy` |
| 13 | Kopf -> Mob-Art, Verzauberungen | `proxy()`, `DummyTargets` | wie bisher | Schadenszahl | bestehend: `headsMakeEnchantmentsSeeTheirMob` |
| 14 | Ruestung/Natuerliche Ruestung/Feuer/Projektil-Immunitaet | `showHit` | wie bisher | Zahl/Ablehnung | bestehend: `armourAndImmunitiesShapeTheNumber` |
| 15 | Trefferpause, Krit-Erkennung, Summe | `showHit`, `tick` | wie bisher | Zahlen | bestehend: `everyHitShowsItsNumberAfterTheCooldown`, `numbersCooldownCritsAndTheSummary` |
| 16 | Stroh-Staender als Vogelscheuche (Ackerland) | `Scarecrow` | wie bisher | kein Umfallen | bestehend: `scarecrowKeepsMobsFromTramplingFarmland` |
| 17 | Windladung, Dreizack, Stab-/Brecher-Angriffe | `hurtServer` | Zahl, kein Rueckstoss (KNOCKBACK_RESISTANCE 1.0) | Zahl | **nicht getestet**, siehe Abschnitt 7 |

## 4. Testentwurf (echte Wege)

* Neue Helfer in `TrainingDummyTests`: Spieler ueber `makeMockServerPlayerInLevel` +
  `runBeforeTestEnd(... remove ...)`, Zielen ueber `player.lookAt(EntityAnchorArgument.Anchor.EYES, ...)`
  (bewaehrter Muster in `MagnetTests`, `PlacedBundleTests`), Laden der Waffe ueber eine
  Schleife `player.tick()` bis `getAttackStrengthScale(0.5F) > 0.9` (nur so werden Krit und
  Sweep ueberhaupt ausgeloest – der Wert steht auch im Mixin).
* Pfeile werden als `Arrow`-Entity gespawnt und **getickt**, bis sie die Puppe treffen
  (kein `onHitEntity`-Aufruf, kein `AbstractArrowAccessor`).
* Explosion ueber `level.explode(null, x, y, z, 3.0F, Level.ExplosionInteraction.NONE)`
  (bewaehrtes Muster in `QuiverTests`, `BackpackTests`).
* Speer ueber `stack.get(DataComponents.PIERCING_WEAPON).attack(player, EquipmentSlot.MAINHAND)`
  – das ist exakt der Aufruf aus `handlePlayerAction`; die Stichweite wird ueber die
  `AttackRange` des Spielers mit dem Speer in der Mitte platziert.
* Alle Tests stehen hinter `if (!McVersion.TRAINING_DUMMY) { helper.succeed(); return; }`.

## 5. Umsetzung

Der opencode-Lauf (bp-dummy) endete an einem Rate-Limit mitten in der Speer-Diagnose; Review und Abschluss
2026-10-08 durch Claude auf `claude-wave1`:

* Diagnose-`println`s und eine nicht kompilierende `ClipContext$Block`-Probe entfernt.
* Krit-Test heisst `aFallingChargedMeleeHitShowsTheCrit` (Fabric machte aus `ShowsACrit` die Id `shows_acrit`,
  der Katalog sagte `shows_a_crit`); Kreativ-Katalog-Id an den Methodennamen angeglichen.
* Speer-Test: Ursache des Rots war der Testaufbau, nicht die Puppe. Der Spieler stand ausserhalb der 8x8-Testflaeche;
  `ProjectileUtil#getHitEntitiesAlong` kappt den Strahl am ersten Block und fand die Puppe nie. Puppe jetzt am fernen
  Rand (x=6), Spieler innerhalb, Stich voll geladen. Ergebnis: Speer-Stich zaehlt, Schleich-Stich baut ab -
  der echte Speer-Weg funktioniert an der Puppe (kein Ausschluss in `PiercingWeapon.canHitEntity`/`Player.stabAttack`).
* Echter Fehler gefunden (Forge 26.3): `ArmorStand.lastHit` startet bei 0, in den ersten 5 Ticks einer Welt brach
  schon der erste Schlag den Stroh-Staender. Fix: `TrainingDummy`-Konstruktor setzt `lastHit = -100`.

## 6. Testergebnis

* `--targets fabric-263,neoforge-263,forge-263 --filter 'simplebuilding:training_dummy_game_test_*'` ->
  `alles gruen: 57/57 bestanden, 0 rot` (Lauf 2026-10-08T17-02-46Z-160d; vor dem lastHit-Fix Forge 18/19 rot).

## 7. Nicht getestet

* Client-Sicht (Zahlen, Wackeln, Partikel) - keine Client-Tests.
* Speer-Ansturm (`KineticWeapon#damageEntities`, gehaltener Angriff mit Tempo): nur der Stich ist getestet. Der
  Ansturm filtert ebenfalls ueber `PiercingWeapon.canHitEntity` und ruft `stabAttack`; ob die Besitzer-Beschwerde
  ("Speer unsauber") den Ansturm meint, ist offen -> Besitzer im Spiel fragen/pruefen.
* Windladung, Dreizack (Wurf/Nahkampf), Streitkolben, Feuer/Lava, Schere-Interaktion ueber echte Wege (Schere/Kuerbis
  sind ueber bestehende Tests abgedeckt).
* 26.2-Linie (Port-Run).
