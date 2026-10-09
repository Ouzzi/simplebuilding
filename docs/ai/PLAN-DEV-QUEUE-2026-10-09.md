# Plan Dev-Queue: Wünsche aus dem Wiki sammeln, gebündelt an die KI geben (2026-10-09)

Besitzer: Im Wiki direkt Textur-Reworks anfragen (einzeln oder mehrere mit einem Kommentar), dazu Rezept-Wünsche
und Anmerkungen zu Items. Alles landet zuerst in einer Queue (Kategorien **Texturen** und **Code**) und wird erst
auf Knopfdruck abgearbeitet. Nur für Entwickler: die Funktionen leben in der **Zentrale**, das öffentliche Wiki
bleibt unverändert.

## Aufbau
1. **Wiki-Spiegel in der Zentrale** (`/wiki/`): dasselbe Dev-Wiki, dazu ein Dev-Skript
   `tools/zentrale-web/dev-overlay.js`, das nur die Zentrale einbindet.
   - Das Wiki ruft an festen Stellen `window.sbDevHook(kind, element, info)` auf, falls vorhanden
     (`texture-tile`, `texture-zoom`, `item-page`, `item-tile`, `recipe`). Ohne Overlay passiert nichts.
   - Overlay: Auswahl-Kästchen je Textur/Item, „Alle sichtbaren auswählen“, Leiste unten
     („N ausgewählt“, optionaler Kommentar, Knopf „Rework anfragen“ / „Rezept ändern“ / „Anmerkung“).
     Ein Kommentar gilt für alle ausgewählten Einträge. Einzelanfrage auch aus der Großansicht.
2. **Queue-Dienst** `tools/zentrale-web/devqueue.py` (nur Python-Standardbibliothek, hinter dem Zugangsschutz der
   Zentrale; Anfragen ohne Login-Header werden abgelehnt). Speichert `queue.json`:
   `{id, category: textures|code, kind: texture|recipe|note, targets:[{id, label, path}], comment, created, by,
   status: open|started|done|dropped, run, result}`.
   API: `GET /api/queue`, `POST /api/queue` (mehrere Ziele, ein Kommentar), `PATCH/DELETE /api/queue/<id>`,
   `POST /api/run` (`{category: textures|code|all}` → alle offenen Einträge → `started`, Lauf-Datei).
3. **Reiter „KI-Queue“** in der Zentrale: Liste je Kategorie (Ziel, Kommentar, Zeit, Status, Lauf/Branch),
   bearbeiten/löschen, Knöpfe „Texturen starten“, „Code starten“, „Alles starten“.

## Orchestrierung (zentral über Claude)
Ein Lauf schickt **alle** gestarteten Einträge auf einmal an Claude (kein Limit pro Lauf). Claude orchestriert:
- Ein Abholer auf dem Steuer-Rechner prüft minütlich, ob ein Lauf gestartet wurde, und startet dafür eine
  Claude-Sitzung (headless) mit dem Lauf-Inhalt und diesem Plan als Auftrag.
- **Texturen macht Claude selbst** (Pixel-Art-Regeln aus der Memory/`pixelart-feedback`): Generator/Redraw je
  Ziel, Vorschau-Bogen vorher/nachher, eigener Branch `claude-q-tex-<lauf>`.
- **Code** (Rezepte, Anmerkungen) delegiert Claude an Helfer-Jobs (opencode bzw. Copilot, wenn Kontingent da),
  je Thema ein Branch `oc-q-<lauf>-<n>`; Claude prüft Diff und Tests und übernimmt. Ausnahme: der Kommentar sagt
  ausdrücklich „per Claude“ → Claude macht es selbst. Ohne solche Anweisung erledigt Claude selbst nur Kleinkram.
- Jeder Eintrag wird zusätzlich in `.claude/QUEUE.md` (Abschnitt „Dev-Queue“) vermerkt, damit alles zentral bleibt.
- Abschluss: Status je Eintrag `done` + Ergebnis (Branch, Vorschau), Vorschauen auf die Abnahme-Seite;
  nach Besitzer-Abnahme Merge nach `claude-wave1` und Gate. Nie direkt nach `master`.

## Verifikation
- `tools/zentrale-web/tests/test_devqueue.py`: anlegen (einzeln/mehrere, ein Kommentar), bearbeiten, löschen,
  Lauf starten (nur offene, Kategorie), 403 ohne Login-Header, kaputte Eingaben → 400.
- `wiki/tests`: Hook-Aufrufe vorhanden und folgenlos ohne Overlay.
- Klick-Test über die echte Zentrale-Adresse (anlegen → Queue-Reiter → Lauf starten → Abholer reagiert).
