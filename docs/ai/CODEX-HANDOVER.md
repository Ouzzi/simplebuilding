# Offene Orchestrator-Arbeit – 2026-10-01

Branch `master`, letzter Feature-Merge `39fd85b1` (Forge-Worker `6786b545`).
Quellfixes `a5d8151e` (Test-Cleanup) und `f23d1004` (JAR-Bootstrap).
Plan: `docs/ai/CODEX-PLAN.md`. Erledigte Merges und tatsächliche Prüfbelege:
`docs/ai/CODEX-VERIFICATION-2026-10-01.md`.

- Abschlussnachweis dieses Runs: `.ai-runs/final-full-gate.log` enthält die
  geprüfte SHA und den abschließenden VERDICT. Bei einer Unterbrechung ohne GREEN
  oder ohne bestätigten Remote-SHA das Gate beziehungsweise den exakten SHA-Push
  fortsetzen. Vor jedem späteren Push ist ein neuer Nachweis für dessen SHA nötig.
- Claims bleiben nicht freigabereif: Kupfergolem-Transfers, Crafter-Ersatzauswurf,
  Blitzentzündung/Kupferreinigung, unbekannte Container, entfernte Storage-Ziele
  und weitere Sekundäreffekte. Dateien/Grenzen: `modules/simpletweaks/CLAIMS-STAGE4.md`.
  Vor Aktivierung reale Pfade schützen und mit aktivem, ausgeschaltetem und
  fehlendem Anbieter prüfen. Keine Aktivierung in dieser Welle.
- Client-/GUI-Abnahme offen. Besitzer-Client PID 22244 läuft seit 01:15 MESZ;
  vor seriellen Tests erneut kontrollieren, niemals stoppen. Dimensions-Kommando
  und sieben Screenshots: `modules/simpledimensions/DIMENSIONS-UI-PLAN.md`.
  Sounds/Visuals und neue Forge-Dialoge noch nicht gesehen/gehört. Forge ohne
  Clientnachweis weiter experimentell. Besitzerwelt/Testzentrale dort unberührt;
  automatisierter Zentrumsneubau und SimpleBuilding-Abdeckung wurden geprüft.
  Forge-Befehle/Abnahmefälle: `docs/FORGE-26.3.md`; Server-/Paketbelege und
  Prüfsummen: `docs/FORGE-FOLLOWUP-RESULTS.md`. Separater Produktionsinstaller-
  beziehungsweise normaler Dedicated-Server-Start ebenfalls noch ungeprüft.
- Separater Bestandsfehler: `WandPlacement.stateFor` in
  `common/src/shared/java/com/simplebuilding/util/WandPlacement.java` verwirft
  einen neuen Bettfuß vor `afterPlace`, auch ohne Claims. Vor späterem Fix echte
  Bettplatzierung und andere mehrteilige Blöcke prüfen. Kein Fix in dieser Welle.
- Besitzerfragen: Echolot 3 Sekunden halten oder Ein-Klick? Morgenbericht der
  Sprach-Bridge ja oder nein?
