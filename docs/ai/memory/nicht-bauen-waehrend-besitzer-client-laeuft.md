---
name: nicht-bauen-waehrend-besitzer-client-laeuft
description: "Laufen Dev-Clients/-Server des Besitzers, nie im Haupt-Repo kompilieren - sonst NoClassDefFoundError im laufenden Spiel"
metadata:
  node_type: memory
  type: feedback
  originSessionId: c63559d3-2c6a-43ab-9c5e-92fd18904742
  modified: 2026-09-28T00:32:30.450Z
---

Laufen runClient/runServer des Besitzers (pruefen: java-Prozesse mit runClient/runServer/devlaunchinjector/forge_userdev), dann `gradlew check`, Datagen und Gates NUR in einem eigenen Worktree (`git worktree add --detach $TEMP/sbgate master`), nie im Haupt-Repo.

**Why:** Am 2026-09-28 stuerzte der Fabric-26.3-Client des Besitzers beim Hammer-Upgrade eines Ofens ab: `NoClassDefFoundError SledgehammerUpgrades$Job` - mein `check` im Haupt-Repo hatte `mc26_3/fabric/build` neu kompiliert, waehrend das Spiel lief; lazy geladene Klassen fehlten dann. Gleiches Muster schon frueher bei Forge.
**How to apply:** Nach Merges im Haupt-Repo nur mergen/committen, Kompilier-Checks im Gate-Worktree; Absturzberichte mit ClassNotFound/NoClassDefFound fuer Mod-Klassen zuerst so erklaeren (Client neu starten), nicht als Code-Bug jagen. Siehe [[testlaeufe-sparsam]].
