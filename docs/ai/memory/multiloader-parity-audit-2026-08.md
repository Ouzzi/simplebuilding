---
name: multiloader-parity-audit-2026-08
description: "Ergebnis des Feature-Paritäts-Audits 1.21.11 vs. Multiloader-Branch (Fabric), Stand 2026-08-20 — bestätigte Regressionen und deren Fix-Status"
metadata: 
  node_type: memory
  type: project
  originSessionId: 6976607d-683e-4fd5-950f-ca0f899bd3cd
  modified: 2026-08-20T14:22:14.824Z
---

Am 2026-08-20 wurde ein vollständiges Paritäts-Audit zwischen Branch `1.21.11` (Merge-Base) und `feature/multiloader-fabric-forge-neoforge` (MC 26.1.2, Fabric-Fokus) durchgeführt (10 Domänen, adversarial verifiziert, plus runClient-Boot-Test — Boot fehlerfrei).

Bestätigte Regressionen aus dem Multiloader-Port:
1. Trim-Glow auf getragener Rüstung: `EquipmentRendererMixin` leerer Stub, nicht in client.mixins.json (Fix-Session vom User gestartet).
2. Villager-/Wandering-Trades komplett weg: `ModTradeOffers` nur Log-Stub, `data/simplebuilding/villager_trade/` existiert nicht; Config-Optionen enableVillagerTrades/enableWanderingTrades tot. — FABRIC BEHOBEN 2026-08-20 (Branch claude/ecstatic-hofstadter-2a6483): Trades sind in 26.1.2 wirklich datengetrieben, `TradeOfferHelper` existiert in Fabric API 0.150.0 nicht mehr. 20 Trade-JSONs + 11 Tag-Merges in Vanilla-Pools, gewichtete Verzauberungen über eigene Loot-Funktion `simplebuilding:weighted_enchant`, Config-Gate über eigene Fabric-Resource-Condition `simplebuilding:config`. NeoForge: Loot-Funktion registriert (Trades laden), aber Config-Gate greift dort noch nicht (fabric:load_conditions wird ignoriert) — offener Folge-Punkt.
3. Fabric-Outline-Invertierung: `SimplebuildingClient` ~Z.125 registriert `BEFORE_BLOCK_OUTLINE` ohne Negation — Vanilla-Auswahlrahmen wird exakt falsch herum unterdrückt (Fix: `!` davor; NeoForge-Seite korrekt).
4. Building-Wand-Ghost-Vorschau fehlt: `BuildingWandItem.getPreviewStates` verwaist, alter `BuildingWandOutlineRenderer` ersatzlos.
5. Multi-Block-Abbau-Risse (Sledgehammer/Strip-/Vein-Miner) fehlen: alter `WorldRendererMixin` leerer Stub, `MiningUtils.getStripMinerBlocks/getVeinMinerBlocks` ohne Aufrufer.
6. Systematisch Actionbar→Chat: alle `sendMessage(text, true)`-Overlays wurden zu `sendSystemMessage` (Chat) — Migrationsartefakt, 13 Stellen. BEHOBEN 2026-08-20 (Worktree-Branch `claude/gifted-hermann-669550`, uncommitted): alle 13 auf `sendOverlayMessage(component)` umgestellt, Build grün. Achtung: In MC 26.1.2 existiert `Player.displayClientMessage(Component, boolean)` NICHT mehr — die Actionbar-Methode heißt jetzt `sendOverlayMessage(Component)` (auf Player und ServerPlayer; ServerPlayer hat zusätzlich `sendSystemMessage(Component, boolean)`).

Für 2–6 wurden Task-Chips angelegt. Absichtliche Änderungen (keine Bugs): Spatula→Chisel mit LegacySpatulaMigration, Double-Jump-Cooldown statt Mehrfachsprung, Sledgehammer-Crush mit Aufladezeit, Rezept-Ausbeuten-Balance, Radius/BreakThrough nicht mehr exklusiv (dafür Anvil-Enchant-Merge). SurvivalTracer-NBT-Schema geändert ohne Migration alter Daten. Aufräum-Chip: `backup 1/`-Texturordner + ~15 MB alte JARs in `assets/simplebuilding/textures/item/`.

## Stand 2026-08-25: alle 6 Regressionen behoben und integriert
Zusammengefuehrt auf Branch `claude/integration-check` (Basis 38f27dd, 10 Commits, 68 Dateien): alle 6 Fixes plus ein nachgelagert gefundener. `gradlew build` gruen fuer Fabric UND NeoForge; Fabric-runClient sauber gebootet (Mixin-Anwendung von EquipmentRendererMixin im Log belegt, keine Injection-Fehler). Mod-JAR 15,3 MB auf 1,2 MB.

Beim Merge zweimal derselbe Konflikt in neoforge/build.gradle: drei Sessions hatten unabhaengig denselben Worktree-Bug im Root-Quellfilter behoben; aufgeloest auf die Path-Variante.

Nachgelagert gefunden und behoben (3f0a827): `BlockOutlineSupport` unterdrueckte die Vanilla-Outline auf dem anvisierten Block, obwohl beide Mod-Renderer ihn bewusst auslassen (Sledgehammer skippt centerPos, Octant steigt ohne pos1 ganz aus) - Zielblock hatte gar keinen Rahmen. In 1.21.11 gab der Renderer in JEDEM Pfad true zurueck. Jetzt: gibt immer false zurueck, Hook fuer beide Loader erhalten.

Offen (nur NeoForge, Fabric ist vollstaendig): (1) Trades-Config-Gate wirkt nicht, weil fabric:load_conditions dort ignoriert wird; (2) 3 Statusmeldungen weiterhin im Chat (Constructor's Touch, Highlights-Toggle, Octant-Figure-Toggle) wegen eigener NeoForge-Kopien; (3) ConfigResourceCondition liegt faelschlich im Paket datagen.

WERKZEUG-WARNUNG: Python `io.open(pfad, 'w')` truncatet die Datei SOFORT beim Oeffnen - schlaegt der Write danach fehl (etwa UnicodeEncodeError durch Emoji-Escapes im Quelltext), bleibt die Datei LEER. Immer in eine .tmp schreiben und mit os.replace verschieben.

## Abschluss 2026-08-25: in den Feature-Branch gemerged
`claude/integration-check` per Fast-Forward in `feature/multiloader-fabric-forge-neoforge` gemerged (HEAD eef13fa). Alle 7 Fix-Commits + Doku sind drin, Arbeitsbaum sauber, `gradlew build` gruen (Fabric-JAR 1,2 MB, NeoForge 1,1 MB). Alle 6 Fix-Worktrees und die zugehoerigen claude/*-Branches wurden entfernt — NICHT mehr danach suchen, die Commits liegen im Feature-Branch. Nichts gepusht.
Rest offen: die 3 NeoForge-Punkte (siehe MULTILOADER_TODO.md, Abschnitt "Offen aus dem Audit") und der manuelle In-Game-Test.
