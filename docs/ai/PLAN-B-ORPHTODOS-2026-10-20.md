# Plan B-orphtodos (20.10.2026): verwaiste TODOs finden

Auftrag `/root/b-auftrag-todos.md` (Besitzer über Sitzung A). Branch `b-orphtodos`, Basis claude-wave1 `e39578d3b`.
Reine Such- und Doku-Arbeit, kein Spielcode, kein Gradle.

## Plan
1. Quellen sammeln:
   - Code: `git grep` nach `TODO|FIXME|XXX|HACK` sowie „später“, „not yet“, „for now“ und „Platzhalter“ in
     Java, Python, Gradle und JSON. Nicht durchsucht: generierte Dateien, `wiki/data`, `testing/runs`, `build`,
     `graphify-out`.
   - Doku (`docs/**`, `modules/**/*.md`): Abschnitte mit Überschriften wie „Offen“, „TODO“, „Folgearbeit“,
     „später“ oder „Nicht getestet“ (Skript), dazu `- [ ]` und Inline-„Offen:“-Zeilen in Plänen, Berichten und
     Audits ab dem 14.10.
   - Memory: `uebergabe-*`, `*-offen.md`, `*-todo.md` und `docs/ai/memory/`.
2. Jeden Fund prüfen:
   - (a) Steht er in `.claude/QUEUE.md` oder `.claude/QUEUE-ARCHIV.md`? Gesucht auch mit anderen Worten und mit
     den Haken `[x]` und `[~]`.
   - (b) Ist er erledigt? Belegt per Code-Grep, Git-Historie, `tools/audit_modules_checks.py` oder einem kleinen
     Python-Test. „Merge offen“ wird per `git merge-base --is-ancestor` gegen wave1 geprüft.
   - Client- und Besitzer-Abnahmen zählen als „in der Queue“, weil sie in Abschnitt (D) stehen.
3. Ausgabe:
   - `/tmp/claude-0/ash/todos.html` im vorgegebenen Format.
   - Queue-Block „Verwaiste TODOs 20.10.“ und Abhaken der eigenen Queue-Zeile.
   - Memory: nur nachweislich erledigte Punkte korrigieren und nichts löschen.
4. Verifikation:
   - Jeder Eintrag hat eine Fundstelle (Datei:Zeile).
   - Die Zahlen stimmen mit den Tabellen überein.
   - Die HTML-Seite hält die Format-Vorgaben ein (keine doctype/html/head/body-Tags, Tokens mit Dark-Mode,
     Tabellen scrollen waagerecht).

## Ergebnis
69 geprüfte Funde: 19 stehen schon in der Queue oder im Archiv, 24 sind erledigt, 26 sind verwaist. Die
verwaisten Punkte stehen als `- [ ]` im neuen Queue-Block. Belege und Vorschläge stehen in
`/tmp/claude-0/ash/todos.html`.

Memory:
- 28 Zeilen in `MEMORY.md` nannten „Merge offen“ für 43 Branches, die alle schon in wave1 sind. Die Zeilen
  sind korrigiert.
- In 13 Themen-Dateien steht jetzt eine Zeile „Stand 20.10.“ (nur angehängt, nichts gelöscht).

Nicht geprüft:
- `tools/guide_book_pages.py` (braucht Pillow und das Client-Jar, beides nur auf sb-test). Der Punkt steht mit
  dem Hinweis „erst prüfen“ in der Queue.
- Die Fragebogen-Punkte D4–D12 und D17 nur stichprobenartig. Sie gelten als überholt durch die Audit-Runden
  vom 26. und 27.09.
