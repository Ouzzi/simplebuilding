# Offene Orchestrator-Arbeit – 2026-09-30

Branch `master`, Plan-/Briefcommit `07eb0323`, Ausgangscommit `821dd131`.
Arbeitsplan: `docs/ai/CODEX-PLAN.md`.

- Claims läuft auf `codex-next-claims` im Worktree `%TEMP%/cx-next-claims`:
  `docs/ai/briefs/run-next-claims.md`, Ziel `modules/simpletweaks/`;
  Quellrepo auf diesem Rechner nicht am historischen Pfad vorhanden, GitHub
  `Ouzzi/simpletweaks` verfügbar. Stufe 1 auf `cdcd3065`, Default aus.
  Zunächst 36/36 Gesamtkatalog, nach weiteren Import-/Grenz-/Befehlsfällen
  18/18 gezielte Servertests grün. Stufe 2 auf `5fbd4064`, zuletzt 26/26 grün
  (`2026-09-30T21-45-15Z-77ed`); frühere rote/veraltete Reports ersetzt.
  Stufe 3 auf `7422a3ab`: Framework-Vertrag und mehrteiliger Werkzeugschutz geprüft,
  36/36 grün (`2026-09-30T22-48-57Z-7c6b`), bestehende Hammertests 70/70 grün
  (`2026-09-30T22-51-11Z-1a63`). Dieser Worker führt Stufe 4 und finales check weiter.
  `run-next-claims-auto.md` ist dabei zusätzlicher Reviewauftrag, kein eigener Run.
- Claims-Stufen 5–6 separat: `codex-next-claims-access`, Worktree
  `%TEMP%/cx-next-claims-access`, Ausgang `7422a3ab`, Brief
  `docs/ai/briefs/run-next-claims-access.md`. Vertrauen/Entzug/Offline/Admin und
  Dimensions-Adapter umsetzen, getrennt committen und prüfen. Zusammenführung
  nach Stufe 4; gemeinsame Framework-/Claims-/Katalogänderungen vereinigen.
- Dimensions ist geprüft und mergebereit: `codex-next-dimensions` auf `8657e3d2`
  (Umsetzung `f83a75c4`), Worktree `%TEMP%/cx-next-dimensions`.
  Bericht `.ai-runs/out-next-dimensions.txt`: 76/76 Modulserver, 10/10 Testzentrale,
  23 JUnit-Tests, 19 Wiki-Tests und `GRADLE_EXIT=0`; Diff vom Orchestrator gelesen,
  `git diff --check` sauber. Client/GUI und Besitzerwelt nicht geprüft.
- QoL/Sounds ist geprüft und mergebereit: `codex-next-small` auf `7faca095`
  (QoL `b606e27d`, Sounds `7f3ffbbc`), Worktree `%TEMP%/cx-next-small`.
  Bericht `.ai-runs/out-next-small.txt`: 48/48 QoL, 71/71 Sounds/Visuals/Integration,
  2/2 Anbieterprüfung, 10/10 Testzentrale und check Exit 0. Orchestrator hat Code
  und eingebettete API in allen vier Fabric-/NeoForge-JARs geprüft; Diff sauber.
  Cap 1,5 bleibt als bestehende begrenzte Obergrenze, alte gespeicherte Werte bleiben.
- Noch keine Feature-Merges. Claims zuerst integrieren, dann Dimensions und
  QoL/Sounds, anschließend Forge. Briefs `run-next-*.md`.
- Vollständiges Gate mit Integration und exakter SHA-Push sind offen.
  Gate-Worktree `%TEMP%/sbgate` auf `07eb0323` vorhanden.
- Forge später: Zusätzlich zu Dimensions/Config-GUI auch die neue Framework-API
  in Sounds/Visuals und den kommenden Claims-Pfaden paketieren und prüfen.
  Keine Standardaktivierung ohne belastbare Laufzeit-/Clientreife.
- Besitzerwelt/Testzentrale und Clientabnahme noch nicht geprüft.
- Separater Bestandsfehler aus der Claims-Gegenprobe: `WandPlacement.stateFor`
  (`common/src/shared/java/com/simplebuilding/util/WandPlacement.java`) verwirft
  einen neuen Bettfuß bei der Nachbarprüfung, bevor `afterPlace` den Kopf setzt.
  Besitzerkontrolle scheitert auch ohne Claims. Kein Fix in dieser Welle;
  vor einer späteren Änderung echte Bett-Platzierung und andere mehrteilige Blöcke prüfen.
- Besitzerfragen: Echolot 3 Sekunden halten oder Ein-Klick? Morgenbericht der
  Sprach-Bridge ja oder nein?
