# Orchestrator-Plan – 2026-09-30

## Ist-Zustand

Start auf `master`, Commit `821dd131`; `git pull --ff-only` meldet aktuell.
Die neun Module sind integriert. Frühere Gate-Zahlen sind historische Belege.
Ungetrackte `.serena/` und `CODEX-ORCHESTRATOR-PROMPT.md` bleiben unangetastet;
`CODEX-UEBERBRUECKUNG.md` wird wie beauftragt mitcommittet.
Git, Node, gh und RTK vorhanden. Python 3.12 und Codex CLI werden eingerichtet,
Java 25 wird installiert. Memory-Sync ist erfolgt (28 Dateien, vorhandene erhalten).
Das alte lokale Claim-Quellrepo fehlt; GitHub `Ouzzi/simpletweaks` ist verfügbar.

Fortsetzung: Der Desktop-Chat startet im leeren Ordner `minecraft-mods`; das
maßgebliche Repository liegt weiterhin unter `C:/Users/o_o/code/simplebuilding`.
Auf `3e8152e6` ist `git pull --ff-only` aktuell. Java 25 und Java 21 sind vorhanden;
die lokale `.ai-runs/env.ps1` setzt die benötigten Laufzeitpfade. Die drei vorhandenen
Worker sind noch aktiv und werden weiterverwendet. Der erste Check mit Java 21
scheiterte beim Forge-Mavenizer an einem unvollständigen Archiv (EOFException);
ein frischer Check muss die inzwischen weiterbefüllten Caches verifizieren.

## Umsetzung in Prioritätsreihenfolge

1. Setup vervollständigen; Brief aus `pre.md`, `mm-contract.md`, `next-claims.md`
   und konkreten Zusatzregeln schreiben. Claims in eigenem Worktree beginnen,
   stufenweise committen, Hauptschalter immer standardmäßig aus. Quellrepo nur lesen.
2. Dimensions-Einstellungen nach Claims beginnen; QoL/Sounds danach. Unabhängige
   Worker dürfen parallel laufen, Integration erfolgt in der Besitzerreihenfolge.
   Maximal drei Worker zugleich; keine Clienttests durch Worker.
3. Diffs und Berichte prüfen, mit `aitool merge-module` integrieren. Claims dürfen
   generische Framework-/SimpleBuilding-Berechtigungsintegration ergänzen; keine
   direkten internen Cross-Mod-Imports. Dimensions-Adapter mit Settings abstimmen.
4. Erst nach Punkten 1–5 Forge 26.3 bearbeiten: Dimensions, tatsächliche verfügbare
   Config-GUI-Anbindung und Standardaktivierung anhand der Verifikation bewerten.
   Ohne belegte Clientreife bleibt Forge experimentell/opt-in.
5. Generatoren, Sprach-/Datenprüfungen und das vollständige Gate im separaten
   Worktree ausführen. Nur nach `VERDICT: GREEN` exakt die geprüfte SHA pushen.

## Betroffene Dateien

`modules/simpletweaks/`, `modules/simpledimensions/`,
`modules/simplequalityoflife/`, `modules/simplesounds/`, gegebenenfalls öffentliche
API in `framework/` sowie generische Berechtigungsaufrufer auf 26.3;
Modulmanifest, Modul-Wiki/Balance/Dokumentation und generierte Wiki-Daten.
Orchestrierung: `docs/ai/briefs/run-next-*.md`, diese Datei,
`docs/ai/CODEX-HANDOVER.md`, `.claude/QUEUE.md`, `docs/HANDOFF.md`.
Forge-Dateien erst in Schritt 4. Keine Ports anderer MC-Linien.

## Risiken

Claims: unvollständige Schutzpfade, fremde Daten, Chunkgrenzen, indirekte Angriffe,
Automation, Berechtigungsänderungen und Rückreise; niemals als fertig deklarieren,
solange die Schutzmatrix nicht belegt ist. Abschaltung muss wirkungslos bleiben.
Frische Maschine: fehlende Java-Toolchains/Caches und Gradle-Downloads; keine Builds
im Hauptcheckout. Bestehende Worktrees vor Gate-Cleanup auf Änderungen prüfen.
Mergekonflikte in Manifest, Dokumentation und generierten Daten gezielt lösen.

## Verifikation und Abschluss

Worker: passende Modul-Serverziele, gezielte Verhaltenstests, `check`; tatsächliche
`alles gruen`-Zeilen lesen. Orchestrator: `python wiki/generate.py --all`,
`--all --check`, Quests, Texturen `--check`, Bücher; dann
`python tools/ai/aitool.py gate --integration` gegen den endgültigen Commit.
Der volle Serverlauf muss Testzentralenbau und vollständige Itemabdeckung enthalten.
Clientdarstellung und Besitzerwelt gesondert als ungeprüft ausweisen, wenn nicht
durchgeführt. Offene Arbeit mit Branch/SHA, Dateien und konkreter Fortsetzung in
`CODEX-HANDOVER.md`; erledigte Aufgaben dort entfernen.

Offene Besitzerfragen bleiben unverändert: Echolot 3 Sekunden halten oder Ein-Klick;
Sprach-Bridge-Morgenbericht ja/nein.

## Laufende Plan-Abweichungen und Befunde

- Zusätzlich Java 21 installiert, da `common` diese Toolchain benötigt. Java 25,
  Python 3.12, Codex und Memory-Sync sind geprüft. Der erste `check` scheiterte
  ausschließlich an der fehlenden Java-21-Installation.
- Zweiter Setup-`check` im Gate-Worktree erreichte den Forge-26.2-Mavenizer und
  scheiterte beim Lesen eines Patcharchivs (`EOFException`). Keine Portänderung;
  das unveränderte vollständige Gate konfiguriert diese vorhandene Linie mit.
- Kein weiterer zusätzlicher Orchestrator-Build parallel zu den Feature-Workern:
  Die frische Cache-Erzeugung beansprucht zeitweise fast den gesamten RAM.
- Die neue Sounds-/Visuals-Framework-API benötigt eine geprüfte Laufzeitpaketierung
  zusätzlich zum Entwicklungs-Classpath. Dieser Nachweis gehört zum Merge-Review.
