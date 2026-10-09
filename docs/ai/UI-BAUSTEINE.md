# UI-Bausteine der Simple-Mods (simplelib, `com.simplelib.api.client.ui`)

Grundsatz (Besitzer N18, 2026-10-07): UI-Konzepte, die mehrere Mods brauchen, liegen als Baustein in SimpleLib und
werden hier beschrieben. Stil: N12 (Bild 3/4), Maße PLAN-CRUCIBLE-N12B. Alles flache Farbe, keine Texturen.

| Baustein | Klasse | Zweck |
|---|---|---|
| Kasten | `UiBoxes.box` / `seam` | Rahmen 5 px (unten 7 px mit Schatten), Ecken 2 px gerundet; Inventar-Fuge |
| Slot / großer Slot | `UiBoxes.slot` / `bigSlot` | eingelassene 16×16-Slots, Ergebnis-Slot 24×24 |
| Pfeil, Fortschritt, Flammen | `UiBoxes.arrow`, `progressFill`, `UiFlames` | Ofen-artige Anzeigen |
| Paletten | `UiPalette` (`derived(fill)`) | Farbe je Block/Mod, Ableitung aller Töne aus einer Füllfarbe |
| Symbole, Motive | `UiSymbols`, `UiMotifs` | eingravierte Pixel-Symbole |
| Filter-Knopf | `UiFilterButton` | einheitlicher Filter-Knopf (Filter-Prinzip) |
| **Kontextmenü** (neu, Simple Maps) | `UiContextMenu` | Rechtsklick-Menü am Cursor, im Bildschirm gehalten; Klick auf Zeile führt aus und schließt, jeder andere Klick schließt und wird verschluckt; deaktivierte Zeilen grau |
| **Lesezeichen** (neu, Simple Maps) | `UiBookmarks.tab` | Lesezeichen links/rechts am Kasten, aktives in Kastenfarbe, Hover heller |
| **Zweitklick-Bestätigung** (neu) | `UiBookmarks.confirm` | erster Klick färbt rot, zweiter Klick bestätigt (z. B. belegten Wegpunkt ersetzen) |
| **Ausgrauen** (neu) | `UiBookmarks.greyOut` | Bereiche, die im aktuellen Modus nicht wählbar sind |

Nutzung: der Mod bündelt SimpleLib (Fabric `include`, NeoForge `jarJar`, Forge `forgeBundleSimplelib`) und importiert
nur `com.simplelib.api.*`. Erster Nutzer der neuen Bausteine: `modules/simplemaps/.../client/WayfinderScreen.java`.
