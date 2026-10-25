# Plan B-FLAKYHORSE (24.10.)
Ist: horse_leashed_and_capturable rot mit "a new horse after the wait". Ursache: VillageWanderer.spawnHorse nutzt WandererSpawner.findSpot (Radius 3, 12 Zufallsversuche); der Wanderer spawnt bis 8 Blöcke von der Glocke und kann am Arena-Rand (15x12x15) stehen, dort fehlt Boden, findSpot liefert null.
Fix: Test stellt den Wanderer vor dem Respawn auf die Glocke (deterministisch). Produktionscode ok (spawnHorse false + Upkeep-Retry ist gewolltes Verhalten). Assertion unverändert.
Verifikation: Test 5x je module-simplewanderers-standalone-fabric-263 und module-simplewanderers-fabric-263, ./gradlew check -q.
