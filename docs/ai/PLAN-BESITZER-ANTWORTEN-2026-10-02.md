# Besitzer-Antworten A–D (2026-10-05)

## Auftrag und Grenzen

Arbeitsbranch `gpt-answers`; je Punkt ein Commit, kein Push, Merge oder Clientstart.
Verbindlich: `wave2-rules.md` des Auftrags, AGENTS.md und WORKFLOW.md.
Neue Funktionen nur 26.3; gemeinsame Quellen müssen weiter für 26.2 kompilieren.
EN/DE werden in Basis und 26.3-Overlay gepflegt. Wiki-Aussagen brauchen Codebelege.

## Plan

1. A: Resonanzstab über einen Reparaturmaterial-Tag mit Amethystscherben reparierbar machen.
   Registrierung, Datagen, Amboss-Test, Tooltip und Wiki prüfen. Rotator-Mending bleibt gesperrt.
2. B: Original der zwölf Config-Ideen aus Run D ermitteln; gegen vorhandene Optionen abgleichen.
   Fehlendes in vorhandene Server-/Client-Gruppen integrieren, numerische Werte hart begrenzen,
   ConfigOptions/ConfigOptionTests, vier Sprachdateien und Wiki-Config-Seite nachziehen.
3. C: `docs/KERNE-SELTENHEIT.md` und die bereits umgesetzte Welle 23 abgleichen.
   Eisen/Gold/Diamant/Netherit nach §5.3 beibehalten; der aktuelle ausdrückliche Auftrag
   setzt Enderit auf 0,5 %. Keine erneuten Steinmetz-Kerne: Besitzerentscheidung vom
   2026-09-28 entfernt sie bereits. Zweite Eisenquelle ist die verlassene Mine (0,5 %).
4. D: Liste G, alte Vorlagenbegriffe und Cover-Buch-Widerspruch mit historischen und aktuellen
   Belegen kurz erklären. Nur belegte veraltete Spielertexte entfernen. Erledigte Queue-Punkte abhaken.

## Risiken und Entscheidungen

- Alte Queue-Zeilen sind keine Belege für fehlende Implementierungen. Neuere Besitzerentscheidungen
  und Code haben Vorrang; bereits vorhandene Optionen werden nicht doppelt angelegt.
- Die zwölf zusätzlichen Ideen und die 58er-Liste wurden nur als Verweis gefunden. Die Recherche
  in den beauftragten Quellen ist abgeschlossen; ohne Original bleibt ihr vollständiger Abgleich offen.
- Installierte Werkzeuge und Caches sind in der Sandbox teilweise unlesbar. Betroffene Befehle
  erhalten einzeln erweiterte Rechte. Build ausschließlich in diesem isolierten Worktree.
- Vorhandenes unversioniertes `.serena/` bleibt außerhalb der Commits.

## Verifikation

- Datagen ausschließlich `:mc26_3:fabric:runDatagen :mc26_3:fabric:syncGenerated263`.
- Wiki mit vorhandener venv `--all`, anschließend `uv ... --all --check`.
- Vollständige Serverziele `fabric-263,neoforge-263`; echte Zeile `alles gruen` lesen.
- `gradlew.bat check -q` einschließlich checkBalance; erforderliche 26.2-Kompatibilität prüfen.
- Testzentralen-Neubau und vollständige Item-Abdeckung über Servertests; Besitzerwelt bleibt unangetastet.

## Ergebnisse

- A: Reparaturmaterial-Tag mit Amethystscherben, Reparaturkomponente, vorhandene Amboss-Aufladung,
  EN/DE an beiden Orten und Wiki umgesetzt. Rotator bleibt ohne Mending. Commit `fc024cb67`.
- B ist ausdrücklich nur teilweise erledigt: sechs belegte bestehende Run-D-Zahlenoptionen haben
  harte 26.3-Grenzen an Lade- und Laufzeitstellen, EN/DE-Tooltips und ConfigOptionTests. Wiki-Grenzen
  berücksichtigen den Versionsschalter. Keine zusätzlichen zwölf Optionen wurden erfunden.
  Commit `1334a536e`; Details und Werte in `docs/CONFIG.md`.
- C: Eisen/Gold/Diamant/Netherit und zweite Eisenquelle bleiben beim dokumentierten aktuellen Stand;
  Steinmetz-Kerne bleiben gemäß neuerer Besitzerentscheidung entfernt. Enderit ist auf 26.3 jetzt
  0,5 %, auf 26.2 weiterhin 0,175 %. Loot-Parser, Wiki und Balancing-Prüfer wählen den Versionszweig.
  Versionsabhängige Werte schreiben keine Altlinien-Zwillinge; der inaktive Altwert ist nur lesbar.
  Commit `6bb9d43cf`.
- D: Belege und kurze Erklärungen in `RUECKFRAGEN-ERKLAERT-2026-10-05.md`. Falsche Vorlagenbegriffe
  waren bereits korrigiert; kein erneuter Spielertext-Fix. Cover bleibt gemäß bestehender Entscheidung
  im Loot. G58 ist erklärt, aber die fehlende Originalliste konnte nicht rekonstruiert werden.
- Serverlauf `testing/runs/2026-10-05T13-31-09Z-d33e.json`: Fabric 966/966, NeoForge 966/966;
  Ausgabe **alles gruen: 1932/1932 bestanden, 0 rot**. Enthält Amboss-Reparatur, Config-Grenzen,
  Kern-Loot sowie Testzentralen-Neubau und vollständige Item-Abdeckung.
- 26.3-Datagen und `syncGenerated263` erfolgreich (`scratchpad/answers-datagen-final.log`).
  Wiki-venv `--all` und uv-Python `--all --check` erfolgreich (`answers-wiki-final-3.log`,
  `answers-wiki-check-3.log`); 55/55 Wiki-Tests grün (`answers-wiki-tests-final.log`).
- `check -q` einschließlich `checkBalance` erfolgreich (`answers-gate.log`); 223 erzeugte Stellen,
  0 Fehler. Abschlusslauf nach der Balancing-Schreibzielkorrektur ebenfalls Exit 0
  (`answers-gate-final.log`), einschließlich 55 Wiki-Tests und erneut 223 Balance-Prüfstellen.
- Pflicht-Compiles `:compileJava :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava`
  Exit 0 (`answers-compiles.log`): 26.2 Fabric/NeoForge und 26.3 Forge, ohne Datagen auf Altlinien.
- Balancing: Gesamtlauf mit 122 Tests deckte alte Enderit-Sollwerte auf. Nach deren Korrektur
  blieben eine Rückschreibprüfung und ein lokaler HTTP-Abbruch (WinError 10053), ein Test war übersprungen.
  Der HTTP-Test bestand unverändert in der Einzelprüfung (`answers-balance-recheck.log`).
  Versionsbeschränkung und bytegenaue Testsimulation korrigiert; abschließende 21/21 gezielte
  Parser-, Extraktions-, Schreib-/Rollback- und Solver-Tests grün (`answers-balance-recheck-3.log`).
  Die gesamte Zusatzsuite wurde danach nicht erneut ausgeführt.
- Kein Clientstart, Push, Merge oder Port-Run. Die Besitzerwelt wurde nicht verändert.
