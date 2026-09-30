# Offene Orchestrator-Arbeit – 2026-09-30

Branch `master`, Plan-/Briefcommit `07eb0323`, Ausgangscommit `821dd131`.
Arbeitsplan: `docs/ai/CODEX-PLAN.md`.

- Setup: Java 25.0.4.1, Python 3.12.14 und Codex CLI 0.159.2 eingerichtet;
  ChatGPT-Login vorhanden. Java 21.0.12.1 für `common` ist ebenfalls installiert.
  Erster isolierter `check` auf `07eb0323` scheiterte an fehlendem Java 21;
  Log `.ai-runs/setup-check.log`. Zweiter Lauf scheiterte im bestehenden
  Forge-26.2-Mavenizer beim Patchen eines ZIPs (`EOFException`), noch vor
  Feature-Kompilierung: `.ai-runs/setup-check-java21.log`. Frische parallele
  Cache-Erzeugung ist ein möglicher Faktor, bisher kein belegter Codefehler.
- Claims läuft auf `codex-next-claims` im Worktree `%TEMP%/cx-next-claims`:
  `docs/ai/briefs/run-next-claims.md`, Ziel `modules/simpletweaks/`;
  Quellrepo auf diesem Rechner nicht am historischen Pfad vorhanden, GitHub
  `Ouzzi/simpletweaks` verfügbar. Default aus; alle sechs Stufen noch offen.
- Unabhängige Worker danach gestartet: `codex-next-dimensions` und
  `codex-next-small`, Worktrees `%TEMP%/cx-next-dimensions` bzw. `cx-next-small`,
  Briefs `run-next-dimensions.md` / `run-next-small.md`. Keine Featurecommits
  geprüft oder gemergt. Berichte `.ai-runs/out-next-*.txt` abwarten und Diffs prüfen;
  Claims zuerst integrieren, dann Dimensions und QoL/Sounds, anschließend Forge.
- Vollständiges Gate mit Integration und exakter SHA-Push sind offen.
  Gate-Worktree `%TEMP%/sbgate` auf `07eb0323` vorhanden.
- Reviewpunkt: Neue Framework-APIs müssen in den ausgelieferten Fabric- und
  NeoForge-JARs verfügbar sein; `implementation project(':framework')` allein
  reicht nicht als Paketierungsnachweis. QoL/Sounds bearbeitet diesen Punkt.
- Besitzerwelt/Testzentrale und Clientabnahme noch nicht geprüft.
- Besitzerfragen: Echolot 3 Sekunden halten oder Ein-Klick? Morgenbericht der
  Sprach-Bridge ja oder nein?
