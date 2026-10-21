# UI-Bausteine der Simple-Mods (simplelib, `com.simplelib.api.client.ui`)

Grundsatz (Besitzer N18, 2026-10-07): UI-Konzepte, die mehrere Mods brauchen, liegen als Baustein in SimpleLib und
werden hier beschrieben. Stil: N12 (Bild 3/4), Maße PLAN-CRUCIBLE-N12B. Alles flache Farbe, keine Texturen.

| Baustein | Klasse | Zweck |
|---|---|---|
| Kasten | `UiBoxes.box` / `seam` | Rahmen 5 px (unten 7 px mit Schatten), Ecken 2 px gerundet; Inventar-Fuge |
| Treppenrahmen (N67) | `UiBoxes.stairFrame` | n einpixelige Lagen (außen zuerst) um eine Füllung, je Lage Ecken n-i…1 (2 Lagen 2-1, 3 Lagen 3-2-1); jedes Pixel genau einmal gemalt, also auch mit Alpha (HUD-Kästen, 78 %) |
| Kontrast (N67) | `UiContrast` | WCAG-Verhältnis, `ensure(fg, bg)` und `INK_OK/ERROR/SOFT` (≥ 4,5:1 auf Inventar-Grau und Papier); `UiPalette.label()` und `ribbonLabel` erzwingen 4,5:1; **jede neue UI prüft Text ≥ 4,5:1, nie dunkler Text mit Schatten auf Grau** (Test `simplelib:module_game_test_ui_text_contrast`) |
| Slot / großer Slot | `UiBoxes.slot` / `bigSlot` | eingelassene 16×16-Slots, Ergebnis-Slot 24×24 |
| Pfeil, Fortschritt, Flammen | `UiBoxes.arrow`, `progressFill`, `UiFlames` | Ofen-artige Anzeigen |
| Paletten | `UiPalette` (`derived(fill)`) | Farbe je Block/Mod, Ableitung aller Töne aus einer Füllfarbe |
| Symbole, Motive | `UiSymbols`, `UiMotifs` | eingravierte Pixel-Symbole |
| Filter-Knopf | `UiFilterButton` | einheitlicher Filter-Knopf (Filter-Prinzip) |
| **Kontextmenü** (neu, Simple Maps) | `UiContextMenu` | Rechtsklick-Menü am Cursor, im Bildschirm gehalten; Klick auf Zeile führt aus und schließt, jeder andere Klick schließt und wird verschluckt; deaktivierte Zeilen grau |
| **Lesezeichen** (neu, Simple Maps) | `UiBookmarks.tab` | Lesezeichen links/rechts am Kasten, aktives in Kastenfarbe, Hover heller |
| **Lesezeichen im Guide-Stil** (N38, Simple Maps) | `UiBookmarks.ribbon` | Band mit abgerundetem Außenende, heller Oberkante, Schatten unten/außen (wie die Guide-Lesezeichen); vor dem Kasten zeichnen, Innenende liegt unter dem Rahmen; belegt = Kastenfarbe, leer = gedämpft, Hover heller und 2 px weiter draußen |
| **Werkzeug-Knopf** (N38, Simple Maps) | `UiBookmarks.button` | quadratischer Knopf auf dem Vanilla-Button-Sprite, Hover = highlighted, eingeschaltet = weißer Strich unter dem Symbol (wie Blueprint-Reiter) |
| **Zweitklick-Bestätigung** (neu) | `UiBookmarks.confirm` | erster Klick färbt rot, zweiter Klick bestätigt (z. B. belegten Wegpunkt ersetzen) |
| **Ausgrauen** (neu) | `UiBookmarks.greyOut` | Bereiche, die im aktuellen Modus nicht wählbar sind |
| **Umschalt für mehr** (Besitzer 21.10.) | `UiShiftTooltip` | einheitliches „Hold Shift for more“ / „Umschalt halten für mehr“: `held()` (einzige Umschalt-Abfrage), `hint()`/`hint(key)` in GRAY #AAAAAA (8,8:1 auf #100010), `collapse(lines, brief)` = erste Zeilen + Hinweis, mit Umschalt alles; eigene Hinweise heißen „Hold Shift for …“. Nutzer: Resonanz (SB), Simple Stats, Simple Interfaces, Simple Storage, SB-Rucksack/Bauplan |

Nutzung: der Mod bündelt SimpleLib (Fabric `include`, NeoForge `jarJar`, Forge `forgeBundleSimplelib`) und importiert
nur `com.simplelib.api.*`. Erster Nutzer der neuen Bausteine: `modules/simplemaps/.../client/WayfinderScreen.java`.

Screens ohne Slots (N43, PLAN-UI-KONZEPTE-2026-10-12): ein `UiBoxes.box` als Fläche, Titel bei x 8 / y 6 in
`palette.label()`, Listen/Zusammenfassungen in einem hellen Feld (`UiBoxes.insetColored` mit `INVENTORY.fill`, dunkle
Schrift), Vanilla-Widgets 8 px vom Kastenrand. SB ruft das über `ModScreenStyle.panel/field/label` (26.2-Zwilling = alt).
