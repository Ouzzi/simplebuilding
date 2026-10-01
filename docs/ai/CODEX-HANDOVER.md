# Offene Orchestrator-Arbeit – 2026-10-01

Branch `master`, Ausgang `821dd131`, Plan `docs/ai/CODEX-PLAN.md`.
Gemergt: Claims 1–4 (`bbfc39c6`, Worker `71753fe2`), Zugriffe/Portale 5–6
(`dc147181`, Worker `2a878f7a`), Dimensions-Einstellungen (`5bb8c442`) und
kompilierter UI-Test (`295ad36a`) sowie QoL/Sounds (`6bb478cd`). Einzel-Worker-Gates grün;
das ist noch kein Gesamtgate.

- Gesamtgate auf `354b1ae3` ROT: check OK, Hauptlinie 1600/1600 grün
  (`2026-10-01T00-53-59Z-55d1`), Integration `2026-10-01T00-59-50Z-bcf2`
  mit 38 Portalfehlern. Ursache bewiesen per Minecraft-Bytecode:
  `runBeforeTestEnd` plant nur bei Timeout minus einem Tick. Neue erfolgreiche
  Tests räumten nicht auf; beide Gate-Dateien `config/simpledimension/server.json`
  hatten danach alle drei Dimensionen deaktiviert. Fix in
  `DimensionSettingsTests.java` und `ClaimPortalTests.java`: explizites Cleanup
  bei Erfolg/Fehler, Timeout-Fallback bleibt. Zwei aufeinanderfolgende vollständige
  Modulprüfungen mit unveränderten Config-Bytes und neues Gesamtgate noch offen.
  Noch kein Push. Forge-Worker muss diese shared Testkorrektur ebenfalls erhalten.
- Weiterer Paketbefund: Beide normalen 26.3-JARs enthielten keinen der vier
  `com.simplebuilding.common`-Bootstrap-Typen. `mc26_3/fabric/build.gradle` und
  `mc26_3/neoforge/build.gradle` nehmen nun den bestehenden Common-Output auf.
  Neue JARs und isolierter Bootstrap-Aufruf daraus müssen noch geprüft werden.

- Claims bleiben AUS und nicht freigabereif. Offene Automation: Kupfergolem,
  Crafter-Ersatzauswurf, Blitzentzündung/Kupferreinigung, unbekannte Container,
  entfernte Mod-Storage-Ziele und weitere Sekundäreffekte. Grenzen/Dateien:
  `modules/simpletweaks/CLAIMS-STAGE4.md`; Zugriffs-/Portalnachweise:
  `modules/simpletweaks/CLAIMS-ACCESS-VERIFICATION.md`. Vor Aktivierung diese Pfade
  schützen und mit eingeschaltetem, ausgeschaltetem und fehlendem Anbieter prüfen.
- Gesamtverifikation offen. Beim Merge nullable Automation-Actor dokumentiert und
  Dimensions-Testanbieter korrigiert; beide Testkataloge vereinigt. Dimensions-
  Schalter prüfen vor dem Laden verknüpfter Zielchunks; Claim-Fußabdrücke und
  sichere Rückwege bleiben erhalten. Vereinte Modulregression auf `5f294df9`:
  176/176 grün (`2026-10-01T00-27-20Z-0efa`), beide vollständigen Tweaks- und
  Dimensions-Kataloge auf Fabric/NeoForge; echte Ergebniszeile gelesen. Abschließend
  `python tools/ai/aitool.py gate --integration`; nur dessen exakte grüne SHA pushen.
- Bestehender NeoForge-Test `each_button_runs_exactly_its_own_command_block` im
  Centre-Lauf `2026-09-30T23-53-58Z-9ce6` rot (9/10); unverändert einzeln 1/1 grün
  (`23-56-25Z-2d76`). Ursache offen; nichts entfernt/ausgenommen. Gemeinsames Gate
  muss einschließlich dieses Tests vollständig grün sein.
- Forge läuft auf `codex-next-forge`, Worktree `%TEMP%/cx-next-forge`, Basis
  `5f294df9`, mit `docs/ai/briefs/run-next-forge.md`: Dimensions-Adapter,
  persistenter Konfigurationsdialog, Claims-Katalog und Framework-Verpackung.
  Standardaktivierung nur bei belegter Reife, sonst weiter experimentell/opt-in.
- Erledigte Merge-, Review- und Paketnachweise sind in
  `docs/ai/CODEX-VERIFICATION-2026-10-01.md` dokumentiert; vollständiges Gate offen.
- Tatsächliche Client-/GUI-Prüfung offen. Besitzer-Client PID 22244 läuft seit
  2026-10-01 01:15 MESZ; vor serieller Prüfung erneut kontrollieren, niemals stoppen.
  Kommando und sieben Screenshot-Pfade: `modules/simpledimensions/DIMENSIONS-UI-PLAN.md`.
  Test nur kompiliert. Sounds/Visuals noch nicht im Client gehört/gesehen.
- Testzentralen in Wegwerf-Serverwelten mehrfach neu gebaut, vollständige
  SimpleBuilding-Item-/Blockabdeckung grün. Besitzerwelt unberührt.
- Separater Bestandsfehler: `WandPlacement.stateFor` in
  `common/src/shared/java/com/simplebuilding/util/WandPlacement.java` verwirft einen
  neuen Bettfuß vor dem Setzen des Kopfes durch `afterPlace`, auch ohne Claims.
  Vor späterem Fix echte Bettplatzierung/mehrteilige Blöcke prüfen. Kein Fix hier.
- Besitzerfragen: Echolot 3 Sekunden halten oder Ein-Klick? Morgenbericht der
  Sprach-Bridge ja oder nein?
