# Offene Orchestrator-Arbeit – 2026-10-01

## Aktueller Stand

Die erste Welle ist auf `17c5c754` mit GREEN gepusht. Der frühere
CLI-Orchestrator PID 20744 ist beendet; der Desktop-Orchestrator hat die vom
Besitzer autorisierte Folgewelle übernommen. Plan:
`FOLLOWUP-PLAN-2026-10-01.md`; Belege:
`FOLLOWUP-VERIFICATION-2026-10-01.md`.

Integriert sind Wiki-UX (`a51ac895`), Bett-/Mehrblockplatzierung (`b13ebe3a`),
Claims-Automations-/Blitzschutz (`c800630a`) und der gemeinsame Bettkopf-Test
(`1ab309b1`). Die Launch-Zentrale erhält eine einklappbare Mod-Auswahl,
standardmäßig alle elf Projektmods, wirksam beim nächsten 26.3-Client-/Serverstart.
Die Gradle-Ladung wird auf Fabric, NeoForge und Forge mit echten isolierten
Serverstarts und Abwahl-Gegenproben geprüft; Details im Verifikationsbericht.

## Abschluss und Fortsetzung bei Unterbrechung

- Laufende Worker/Gates zuerst prüfen, keine parallele Übernahme. Der aktuelle
  Orchestrator besitzt Merge und Push. Keine Builds im Hauptcheckout.
- Vollständiges gemeinsames Gate mit Integration im separaten `%TEMP%/sbgate`.
  `.ai-runs/followup-full-gate.log` muss die geprüfte SHA und `VERDICT: GREEN`
  enthalten. Nur exakt diese SHA nach `master` pushen und Remote-SHA bestätigen.
  Falls ein Schritt fehlt, dort fortsetzen; alte grüne Zahlen sind kein Ersatz.
- Keine ungetrackten Serena-Dateien oder `CODEX-ORCHESTRATOR-PROMPT.md` löschen.

## Noch offen

- Claims bleiben AUS. Crafter, Kupfergolem-Endpunkte und die geprüften
  Blitz-Blockänderungen sind geschützt; Blitzableiter-/Entity-Sekundärfolgen,
  vertrauenswürdige mobile Besitzer, unbekannte Container und weitere entfernte
  Mod-/Storage-/Physikpfade bleiben unvollständig. Grenzen und Tests:
  `modules/simpletweaks/CLAIMS-STAGE4.md`.
- Besitzer-Client PID 22244 bleibt unangetastet. Vor serieller Clientprüfung
  erneut kontrollieren, niemals stoppen. Dimensions-UI:
  `modules/simpledimensions/DIMENSIONS-UI-PLAN.md`; GUI/Sounds/Visuals und
  Forge-Dialoge weiterhin ohne tatsächliche Clientabnahme. Forge bleibt opt-in.
- Der aktive Hub auf Port 8773 verwaltet diesen Client. Sein statisches Frontend
  aktualisiert sich beim Neuladen; Python-Backendänderungen brauchen einen
  Hub-Neustart. Diesen bei beendetem Besitzerlauf durchführen, damit dessen
  Prozessverfolgung und Log-Pipe erhalten bleiben. Insbesondere die neue
  Forge-Auswahlweitergabe benötigt das aktualisierte Backend.
- Besitzerwelt/Testzentrale dort unberührt. Automatisierte Zentren- und
  Abdeckungstests in isolierten Welten ersetzen diese Abnahme nicht.
- Weiterer Backlog: `.claude/QUEUE.md` (unter anderem Config-Spalten,
  Tabellenzeilen-Direktlinks und Beschaffungsdarstellung). Neue Portalformen,
  Kern-/Balancing-Entscheidungen und Ports erst nach Besitzerfreigabe.
- Besitzerfragen: Echolot drei Sekunden halten oder Ein-Klick?
  Morgenbericht der Sprach-Bridge ja oder nein?
