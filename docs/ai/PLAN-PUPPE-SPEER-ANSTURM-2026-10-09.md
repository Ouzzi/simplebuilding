# Trainingspuppe – Speer-Ansturm (2026-10-09)

## Ursache

Der vorhandene GameTest deckt nur den direkten `PiercingWeapon.attack`-Stich ab.
Der aufgeladene Vanilla-Speer läuft dagegen über `LivingEntity.updateUsingItem`
und `ItemStack.onUseTick`, wo die `KineticWeapon`-Komponente
`damageEntities` aufruft. `KineticWeapon` sucht dort ausschließlich
`LivingEntity`-Ziele und verwendet als Filter
`PiercingWeapon.canHitEntity`; diese Kette muss für die Trainingspuppe
funktionieren. Die 26.3-Signaturen werden mit `javap` aus dem Gradle-Cache
belegt, nicht aus Minecraft-Quellen.

## Plan

1. Einen GameTest mit echtem `startUsingItem`-Weg ergänzen: Spieler mit
   Eisenspeer, auf die Puppe zulaufen, Ticks ausführen und Trefferzahl/Zahl
   prüfen.
2. Den Test in den Fabric-Wrapper und das gemeinsame GameTest-Katalogregister
   aufnehmen.
3. Falls der Test rot ist, die kleinste Ursache in `TrainingDummy` beheben,
   ohne den Vanilla-Ansturm nachzubauen oder den Test abzuschwächen.
4. Den gefilterten Fabric-Lauf ausführen; anschließend den geforderten
   Multi-Loader-Lauf ausführen, falls der Fabric-Lauf grün ist.

## Dateien

- `common/src/shared/java/com/simplebuilding/dummy/TrainingDummy.java`
- `common/src/shared/java/com/simplebuilding/gametest/TrainingDummyTests.java`
- `common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java`
- `src/main/java/com/simplebuilding/gametest/TrainingDummyGameTest.java`

## Testbericht

Wird nach den Läufen mit den exakten Testzeilen ergänzt. Client-Sichtprüfung
und nicht ausgeführte Loader-/Linien-Läufe werden ausdrücklich vermerkt.

## Ergebnis (Review Claude, 2026-10-09)

Der rote Copilot-Test war ein Testaufbau-Fehler, kein Puppen-Fehler:
`KineticWeapon.getMotion` liest beim Spieler `getKnownSpeed()`; beim
`ServerPlayer` setzen das die Bewegungspakete des Clients
(`setKnownMovement`). Der Schein-Spieler hat keinen Client, also Tempo 0 und
keine erfüllte Schadensbedingung. Zweitens lief der Test bis 1,2 Blöcke an die
Puppe heran, also unter den Mindestabstand der Speer-Reichweite. Der Test setzt
jetzt je Tick `setKnownMovement` und steht 3 Blöcke vor der Puppe. Der echte
Weg `Player.stabAttack` -> `hurtOrSimulate` -> `TrainingDummy.hurtServer`
zählt den Treffer ohne Änderung an `TrainingDummy`.
