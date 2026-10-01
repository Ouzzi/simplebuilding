# Offene Orchestrator-Arbeit – 2026-10-01

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
  Fortsetzung mit `run-next-claims-auto.md`: ursprünglicher Worker wurde nach
  50/50 grün (`2026-09-30T23-05-57Z-8f5a`) gezielt neu gestartet, damit er nur
  Stufe 4 abschließt. Naturschaden, mehrteilige Explosionen, Kolbenrückzug und
  eigene Automation korrigiert: 64/64 grün (`2026-09-30T23-45-13Z-23b2`).
  Bestandsfälle ohne Anbieter: Kolben 56/56, Hopper 52/52, Attractors 6/6,
  Testzentralen-Neubau/Abdeckung 10/10; tatsächliche Ergebniszeilen gelesen.
  Finales check/Commit noch offen. Aktivierung bleibt blockiert (Kupfergolem,
  Crafter, Blitzfolgen, weitere Automationslücken), siehe `CLAIMS-STAGE4.md` im Modul.
  Gesichert: `.ai-runs/claims-stage4-before-resume.zip`/`.patch`, ursprüngliches
  Log `.ai-runs/log-next-claims-through-stage4.txt`. Kein Reset/Cleanup erfolgt.
- Claims-Stufen 5–6 separat: `codex-next-claims-access`, Worktree
  `%TEMP%/cx-next-claims-access`, Ausgang `7422a3ab`, Brief
  `docs/ai/briefs/run-next-claims-access.md`. Vertrauen/Entzug/Offline/Admin und
  Dimensions-Adapter getrennt committen und prüfen. Stufe 5 auf `4ac2875a`,
  42/42 grün (`2026-09-30T23-18-06Z-4f7d`); Befehle und atomare Rechteänderung
  vom Orchestrator gelesen. Stufe 6 auf `2a878f7a`, vollständiges check Exit 0 und
  beide Dimensions-Produktions-JARs mit eingebetteter API geprüft. Zusammenführung
  nach Stufe 4; gemeinsame Framework-/Claims-/Katalogänderungen vereinigen.
  Merge-Review: Stufe 4 liefert `Protection.Target.actor == null` für Automation.
  Die Dimensions-Testanbieter aus Stufe 6 müssen damit umgehen können
  (`owner.getUUID().equals(t.actor())` statt `t.actor().equals(...)`); API-Vertrag dokumentieren.
  Stufe-6-Nachweise: kompletter Tweaks-Lauf 66/66 (`23-37-40Z-b5cf`), finale
  Dimensions-Schutzfälle 10/10 (`23-51-37Z-36e1`), echter Portalablauf erneut
  2/2 (`23-57-47Z-0885`), alle am 2026-09-30 UTC. Gesamtmerge noch offen.
  Bestehender Test `each_button_runs_exactly_its_own_command_block` war auf
  NeoForge im Centre-Lauf `23-53-58Z-9ce6` rot (insgesamt 9/10); unverändert
  einzeln 1/1 grün (`23-56-25Z-2d76`). Ursache offen, kein Test entfernt/ausgenommen.
- Dimensions ist geprüft und mergebereit: `codex-next-dimensions` auf `8657e3d2`
  (Umsetzung `f83a75c4`), Worktree `%TEMP%/cx-next-dimensions`.
  Bericht `.ai-runs/out-next-dimensions.txt`: 76/76 Modulserver, 10/10 Testzentrale,
  23 JUnit-Tests, 19 Wiki-Tests und `GRADLE_EXIT=0`; Diff vom Orchestrator gelesen,
  `git diff --check` sauber. Client/GUI und Besitzerwelt nicht geprüft.
- Dimensions-UI-Testergänzung fertig: `codex-next-dimensions-ui` auf `385e4e57`,
  Worktree `%TEMP%/cx-next-dimensions-ui`, Basis `8657e3d2`.
  `:integration:compileGametestJava` Exit 0, Diff sauber; sieben Screenshot-Prüfpunkte.
  Bericht `.ai-runs/out-next-dimensions-ui.txt`, genaue Befehle/PNG-Pfade im
  `modules/simpledimensions/DIMENSIONS-UI-PLAN.md` dieses Branches.
  Tatsächlicher Clientlauf/Sichtprüfung offen: Bei Prüfung läuft ein SimpleBuilding-
  Client (seit 2026-10-01 01:15 MESZ). Vor serieller Prüfung erneut kontrollieren;
  keinen Besitzer-Client stoppen und keine Hauptcheckout-Builds ausführen.
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
