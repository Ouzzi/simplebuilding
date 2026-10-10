# Audit Simple Models (2026-10-10, Queue N23)

Umfang: `modules/simplemodels` (ca. 1150 Zeilen Java, 16 gemeinsame Tests, Client-Smoke). Branch `claude-q-models`.

## Befunde und Umgang

| # | Befund | Art | Status |
|---|---|---|---|
| 1 | Models-Knopf war ein Text-Button (`Models`, 80 px) ueber dem Fenster, nicht im Stil der Guide-Lesezeichen | UX/Stil | behoben: `ModelsTab` (Lesezeichen links am Inventar/Amboss, Namensschild-Icon, Tooltip) |
| 2 | Der Knopf wurde nur in `Screen.init(II)` angehaengt. `resize()` und Rezeptbuch-Umschalten bauen nur ueber `rebuildWidgets()`/verschieben `leftPos` und liessen den Knopf verschwinden bzw. falsch stehen | Bug | behoben: Hook auf `init(II)` und `rebuildWidgets()` (ohne Duplikat), Position wird je Frame aus `leftPos/topPos` gelesen |
| 3 | Hilfetexte (Browser, Guide-Seite 6, Wiki) nannten `config/simplemodels/catalog`, der Ordner heisst `catalogue` | Doku-Bug | behoben (Lang, manual.json, Wiki regeneriert) |
| 4 | Aktiver Browser-Reiter nicht erkennbar | UX | behoben: aktiver Reiter ausgegraut |
| 5 | "Zurueck/Weiter" immer aktiv, keine Seitenanzeige | UX | behoben: an den Enden deaktiviert, Seitenzaehler `n/m` rechts im Titelbalken |
| 6 | Maustasten: Modul hat keine eigene Mausbehandlung, kein `button == 0`; neuer Tab erbt `AbstractButton` (SDL-Links) | Pruefung | ok |
| 7 | Smoke-Test klickte nur `Button`; Tab fehlte im Test | Testluecke | behoben: Tab-Anzahl (genau einer), Resize-Test, Screenshot `simplemodels-inventory-tab` |
| 8 | Modul ist allein spielbar (keine simplelib-Abhaengigkeit); Lesezeichen-Zeichnung deshalb im Modul nachgebaut statt `UiBookmarks` | Prinzip | bewusst, siehe PRINZIPIEN-MODUL-UNABHAENGIGKEIT |

## Bewusst nicht geaendert (Vorschlaege)
- Server-Einstellungen-Reiter zeigt Werte per Reflexion als `true/false`; huebscher waere ein Schalter-Stil (nur Lesen, Admins editieren die Datei).
- Mausrad-Blaettern in der Modellliste; Sortierung/Filter nach Basisitem; Vorschau im Tooltip statt nur Text.
- Creative-Inventar hat keinen Tab (anderes Screen, kein Amboss-Bezug).
- Tab-Position kollidiert bei geoeffnetem Rezeptbuch optisch mit dessen Rand (liegt darueber).
