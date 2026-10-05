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
- Die zwölf Ideen und die 58er-Liste sind bisher nur als Verweis gefunden; Originalrecherche läuft.
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

Noch keine Implementierung oder Prüfungen abgeschlossen.
