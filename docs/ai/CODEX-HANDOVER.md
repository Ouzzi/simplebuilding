# Offene Orchestrator-Arbeit – 2026-10-01

Branch `master`, Quellfixes `a5d8151e` (Test-Cleanup) und `f23d1004` (JAR-Bootstrap).
Plan: `docs/ai/CODEX-PLAN.md`. Erledigte Merges und tatsächliche Prüfbelege:
`docs/ai/CODEX-VERIFICATION-2026-10-01.md`.

- Forge läuft auf `codex-next-forge`, Worktree `%TEMP%/cx-next-forge`, Basis
  `5f294df9`, Brief `docs/ai/briefs/run-next-forge.md`. Native Konfigurationsdialoge,
  Persistenz, vollständige Adapter/Kataloge und JAR-Verpackung sind in Arbeit.
  Alle Forge-Gates, Merge und Prüfung der finalen Artefakte stehen aus.
  Claims und Forge bleiben standardmäßig AUS beziehungsweise opt-in.
- Beim Forge-Merge die zwei gemeinsamen Testfixes aus `a5d8151e` erhalten:
  `DimensionSettingsTests.java` und `ClaimPortalTests.java`. Der Worker wurde über
  `docs/ai/ORCHESTRATOR-GATE-FAILURE.md` in seinem Worktree informiert. Er trennt
  zusätzlich Settings- und Claims-Tests auf allen drei 26.3-Loadern in eigene
  Testumgebungen (Kollision Tick 155/Settings ab Tick 130). Diese Änderungen vor
  dem finalen Gate ebenfalls integrieren; keine Testfälle streichen.
  Der Worker hat inzwischen exakt die beiden Java-Dateien aus `a5d8151e`
  übernommen; inhaltlicher Vergleich mit master ist identisch. Die zwischenzeitliche
  alternative Mixin-Lösung entfällt. Abschlussnachweise auf diesem Stand abwarten.
- Gemeinsames Abschlussgate und exakter SHA-Push offen. Das erste Gesamtgate
  auf `354b1ae3` war rot; check/1600 Hauptlinientests waren grün, der bewiesene
  Cleanupfehler ist inzwischen mit zwei aufeinanderfolgenden 176/176-Läufen und
  unveränderten Config-Bytes behoben. Neuer gesamter Nachweis erforderlich:
  `python tools/ai/aitool.py gate --integration`; nur die dabei grüne SHA pushen.
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
- Separater Bestandsfehler: `WandPlacement.stateFor` in
  `common/src/shared/java/com/simplebuilding/util/WandPlacement.java` verwirft
  einen neuen Bettfuß vor `afterPlace`, auch ohne Claims. Vor späterem Fix echte
  Bettplatzierung und andere mehrteilige Blöcke prüfen. Kein Fix in dieser Welle.
- Besitzerfragen: Echolot 3 Sekunden halten oder Ein-Klick? Morgenbericht der
  Sprach-Bridge ja oder nein?
