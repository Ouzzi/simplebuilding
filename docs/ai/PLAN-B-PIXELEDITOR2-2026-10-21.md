# Plan b-pixeleditor2 (21.10.2026) – Pixeleditor: UI/UX-Feinschliff und eigene Icons

Queue „(B, b-pixeleditor2, 21.10.)“. Besitzer wörtlich: „die demo bitte etwas ausarbeiten und verfeinern, die ui und ux
so optimal wie möglich halten, und auch bitte gute icons für die einzelnen buttons wählen.“
Grundlage: `docs/ai/PLAN-B-PIXELEDITOR-2026-10-21.md` (Format, Kern), `docs/ai/ANALYSE-RESPRITE-2026-10-21.md` (Vorbild).

## Ist-Zustand (b-pixeleditor, 99de8adcc)

- `wiki/assets/pixeleditor.js` (Oberfläche, 705 Zeilen) + `pixeleditor-core.js` (reine Funktionen, node-getestet).
- Knöpfe zeigen Unicode-Zeichen/Emoji (✎ ⌫ ▨ ⊙ 📂 🗑 🔒 …): je nach System-Schrift uneinheitlich, teils farbig, schlecht lesbar.
- Kopfleiste mit allen Schaltern in einer Reihe; „Gefüllt“/„Überall“ als Kästchen unter den Werkzeugen; Statuszeile über
  der Arbeitsfläche; Ebenen nur per ↑/↓ sortierbar; Umbenennen/Verwerfen/Fehler per `prompt`/`confirm`/`alert`.
- Handy (≤ 760 px): Werkzeuge als Reihe unter der Fläche, Seitenleiste darunter (lange Seite zum Scrollen).
- Kein Browser-Test des Editors; Tests: `wiki/tests/test_pixeleditor.py` (Verdrahtung, JS-Kern == Python).

## Umsetzung (nur `wiki/assets/pixeleditor*.js`, Tests, Doku)

1. **Icons**: eigenes Set `ICONS` in `pixeleditor.js`, selbst gezeichnet (keine Bibliothek kopiert): 24er-Raster,
   `viewBox="0 0 24 24"`, `stroke="currentColor"`, Strich 2, runde Enden, `fill="none"` (Füllflächen nur als
   `currentColor` mit Deckkraft). Jeder Knopf: Icon + `aria-label` + Tooltip „Name (Kürzel)“. Kein Unicode-Glyph mehr
   als Knopf-Inhalt.
2. **Aufbau Desktop** (Aseprite/Resprite-Rollen):
   - Kopfleiste: Name | Rückgängig/Wiederholen | Zoom −, Zoom-Anzeige, Zoom +, Einpassen | Raster | Öffnen, Export
     (Hauptknopf), Schließen. Gruppen mit Trennern.
   - Werkzeugleiste links, nur Icons (44 px), Gruppen Zeichnen (Stift, Radierer, Füllen, Pipette) | Formen (Linie,
     Rechteck) | Auswahl (Auswahl, Verschieben); darunter Primär-/Sekundärfarbe groß mit Tauschen.
   - Kontextleiste über der Fläche: Name des Werkzeugs + seine Optionen (Pinselgröße 1–3, Pixel Perfect, Symmetrie
     x/y, Gefüllt, Überall) als Umschalt-Knöpfe.
   - Statusleiste unter der Fläche: Koordinate, Farbe unter dem Zeiger (Feld + Hex), Größe, Zoom, aktive Ebene
     (+ Warnung „ausgeblendet“).
   - Paneele rechts: Farben (Primär/Sekundär, Farbwähler, Hex, Deckung, **zuletzt benutzt**, Palette der Textur,
     Vanilla-nah, GPL), Ebenen (Zeile: Ziehgriff, Auge, Vorschaubild, Name, Deckkraft-%, Sperre; Drag-Sortierung per
     Zeiger – geht auch mit Touch; Deckkraft-Regler der aktiven Ebene; Icon-Leiste neu/duplizieren/hoch/runter/
     zusammenführen/umbenennen/löschen), Auswahl/Ebene, Vorschau, Referenzbild, Export.
3. **Handy (≤ 760 px)**: Kopfleiste kompakt; Fläche füllt den Platz; unten feste Leiste: Werkzeuge (8 × 44 px) +
   zweite Reihe Farbfeld/Kontext-Optionen/Blätter „Farben“, „Ebenen“, „Mehr“; Paneele als ausklappbare Blätter von
   unten (Schließen-Knopf, Esc). Kein waagerechtes Seiten-Scrollen.
4. **Dialoge im Editor statt Browser-Dialogen**: Verwerfen bestätigen (Schließen mit Änderungen, Öffnen einer Datei
   mit Änderungen), Umbenennen inline (Eingabefeld in der Zeile), Fehler als Meldung.
   **Leerer Zustand/Fehler**: Laden mit Hinweis; Fehler mit Grund + „Datei öffnen“ + „Leere 16×16-Textur“.
5. **Komfort**: Pinselgröße 1–3 (`[`/`]`, Kontextleiste; neue Kern-Funktion `brush(pts, size)`), Umschalt+Klick =
   gerade Linie vom letzten Punkt (Stift/Radierer), Alt = Pipette (vorhanden), X tauschen (vorhanden), Raster
   umschalten (Knopf + Strg+'), Zoom-Knöpfe + Einpassen (0). Keine bestehende Funktion fällt weg.
6. **Barrierefreiheit**: alles per Tab erreichbar, sichtbarer Fokus (3 px Ring), Ebenen-Zeilen: Enter wählt, F2 benennt
   um, Alt+↑/↓ verschiebt; Kontrast ≥ 4,5:1 (A7) für Text und aktive Knöpfe, hell/dunkel über Tokens.

## Tests

- `wiki/tests/test_pixeleditor.py`: grün halten; neu: jedes verwendete Icon ist definiert, kein Knopf mit Glyph-Inhalt,
  Werkzeug-Tooltips enthalten das Kürzel.
- `wiki/tests/pixeleditor_core.cjs`: `brush` (Größe 1/2/3, Ränder).
- Neu `wiki/tests/pixeleditor_ui.cjs` (Playwright, wie `ui_lists.cjs`; auf sb-test): Desktop hell 1280 px + Handy dunkel
  390 px; jeder sichtbare Knopf hat ein Icon und `aria-label`, ist klickbar ohne Seitenfehler; Werkzeug-Tasten
  B/E/G/I/L/U/M/V, X, [ ], +/−/0, Strg+Z/Y; Strich zeichnen → Rückgängig; Umschalt-Linie; Ebene neu + Drag-Sortierung;
  Schließen mit Änderungen fragt nach; Handy: Werkzeugleiste unten, Ziele ≥ 44 px, kein waagerechtes Scrollen, Blatt
  „Ebenen“ öffnet; Kontrast-Stichprobe ≥ 4,5:1. Screenshots Desktop hell + Handy dunkel → Scratchpad.
- sb-test: `python3.12 -m unittest wiki.tests.test_pixeleditor`, Wiki erzeugen (-gen), `./gradlew check -q`.

## Demo

`scratchpad/pixeleditor-demo.html` neu: eigenständig (Kern + Oberfläche inline, keine externen Skripte), Beispiel
`simplecrucibles:icon` mit drei Ebenen (Kontur, Tiegel, Glut; eigene Besitzer-/Agenten-Textur, nicht Vanilla).
Veröffentlichung macht die Hauptsitzung.

## Risiken

- Größerer Umbau der Oberfläche: Verhalten der Werkzeuge bleibt in denselben Funktionen (beginStroke/shape/…), nur
  Aufbau/Bedienung ändern sich. Browser-Test deckt die Bedienung ab.
- Drag per Zeiger statt HTML5-DnD (geht auf Touch); Tastatur-Ersatz Alt+↑/↓ und Knöpfe.
- Gemeinsame Dateien: nur eigene Zeile in `.claude/QUEUE.md`; `dev-overlay.js` bleibt unverändert.
