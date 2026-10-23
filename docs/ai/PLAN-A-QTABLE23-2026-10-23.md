# Plan A-QTABLE23 – Fragentisch-UI neu angeordnet (Queue QT23, Besitzer 23.10.)

Branch `a-qtable23` (Basis claude-wave1 cd04b4804). NUR MC 26.3 Fabric. Ein Fenster, kein Massenumbau.

## Auftrag (Wortlaut Queue QT23, gekürzt)
Fragentisch + Inventar mittig, das Buch ragt links und rechts heraus (zwei Seiten). Links immer der Text mit klarer
Hierarchie (Überschrift farbig wie jetzt, Fließtext grau und luftiger, Schlagwörter fett). Rechts: Seite „Lore“ =
Textur-Hinweise (Bilder), Seite „Use“ = Rezepte/Verwendung/Fundort-Hinweis, dritte Seite = Textur-Hinweis zum Item.
Erst exemplarisch mit Vorschau.

## Ist-Zustand
- `modules/simplequestionary/shared/java/com/simplequestionary/client/QuestionaryScreen.java`: links Tischkasten (Lapis,
  `UiBoxes`/`UiMotifs`) über Inventarkasten (176 breit), rechts daneben ein 224 breites Buch; Text fließt von der
  linken auf die rechte Buchseite; „Nutzen“ endet mit der Rezeptzeile (`FactsPayload`). Lesezeichen rechts am Buch.
- `QuestionaryMenu`: Slot-Positionen (SLOT_X/INVENTORY_X) relativ zum Fensterrand, WIDTH 424, HEIGHT 208.
- Daten: `assets/<ns>/questionary/<lang>.json` (690 eigene Einträge/Familien je Sprache, Seiten lore/use/secret,
  Absätze bis 350 Zeichen, Platzhalter `{name}`, `{mod}`, `{item:ns:id}`). Kein Bild-/Fundort-Feld.
- Rezeptdaten: `QuestionaryFacts` (Server, RecipeManager): Zutaten des Rezepts mit den meisten Zutaten, Anzahl
  herstellender/nutzender Rezepte.
- Python-Port der simplelib-Bausteine: `tools/ui/simpleinterfaces_preview.py` (Kasten, Slot, Schrift).

## Entwurf (für beide Varianten gleich)
- **Mitte:** Tisch- und Inventarkasten unverändert (176 breit), aber waagrecht mittig; Slot-Positionen im Menü um
  `COLUMN_X` verschoben.
- **Buch dahinter:** offenes Buch über die ganze Breite, die Mittelspalte deckt den Rücken ab; je Seite ~110–124 px
  sichtbar.
- **Linke Seite = immer Text:** Überschrift (Seitenname) in HEADING-Braun wie heute + Trennlinie; Fließtext in Grau,
  Zeilenabstand 11 statt 10, Absatzabstand 5; Schlagwörter fett = alles aus `{name}`, `{item:…}`, `{mod}` und
  optional `**…**` im Datentext; fett in dunkler Tinte. Passt der Text nicht, scrollt das Mausrad über der linken
  Seite den Text (Rollbalken am Seitenrand); sonst blättert das Rad wie heute.
- **Rechte Seite je Lesezeichen:**
  1. Überlieferung → **Bildtafel**: Gegenstand groß (3×) in gerahmter Tafel, Name darunter, Reihe „Verwandt“ (Items
     aus `{item:…}` aller Seiten + Geschwister derselben Familie, max. 5).
  2. Nutzen → **Rezept + Fundort**: Zutaten (bis 9) als Mini-Raster → Pfeil → Ergebnis, Station (Werkbank, Ofen …)
     aus `RecipeDisplay.craftingStation()`, Zeile „N Rezepte / in M Rezepten Zutat“, Fundort-Hinweis abgeleitet:
     herstellbar → „an der Station herstellen“; nicht herstellbar + Block → „in der Welt abbauen“; sonst „in der Welt
     finden (Truhen, Mobs, Handel)“.
  3. Geheimnis → **Lupe**: Gegenstand 5× (Pixel sichtbar) in einem Goldring mit feinem Pixelraster, darunter
     „Textur-Hinweis“ + Texturname (`ns:item/pfad` aus der Item-Modell-Komponente).
- Kontrast ≥ 4,5:1 für jede Textfarbe auf Papier (Werte im Bericht, Unit-/Gametest).

## Varianten (Vorschau)
- **A „kompakt“:** Buch 404×208 hinter der Spalte, Lesezeichen rechts am Buchrand (wie heute), Fensterhöhe 208.
- **B „Buch größer als der Tisch“:** Buch 424×228, ragt auch oben/unten über die Spalte, Lesezeichen oben an der
  rechten Seite, mehr Textbreite (≈100 px) und -zeilen. Fenster 424×238 (Grenze 427×240 bei GUI-Skala 2).
Vorschau: `/root/sb-tools-a/previews/qtable23-preview.html` (Python-Port, echte Einträge: Enderitbarren, Aschholz-Stamm,
Buch, Lohenrute; leere Ablage).

## Umsetzung (nach Vorschau, empfohlene Variante)
1. `QuestionaryMenu`: neue Geometrie-Konstanten (COLUMN_X/COLUMN_Y, Slots verschoben).
2. Neu `QuestionarySheet` (shared, ohne Client-Klassen): Textspannen mit Fett (`spans`), verwandte Items
   (`related`), Fundort-Art (`origin`), Texturname (`textureOf`) – testbar auf dem Server.
3. `QuestionaryFacts`/`FactsPayload`: zusätzlich Station-Item des gewählten Rezepts.
4. `QuestionaryScreen`: neues Layout, linke Textseite mit Hierarchie + Scroll, drei rechte Seiten.
5. Lang-Schlüssel (de/en) für neue Beschriftungen.

## Tests
- Gametest `sheet_every_entry` (Daten): für jeden eigenen Eintrag/jede Familie in EN+DE: jede Seite hat Text
  (eigener/Familie/Fallback), und die rechte Seite hat Inhalt (Bildtafel: Item vorhanden; Nutzen: Rezept oder
  Fundort-Art; Lupe: Texturname). `{item:…}`-Verweise zeigen auf existierende Items (nur Warnung bei fremden Mods).
- Gametest `sheet_spans`: Fett-Spannen für Platzhalter und `**…**`.
- Gametest `sheet_contrast`: alle Textfarben der Seite ≥ 4,5:1 auf Papier.
- Gametest `facts` erweitert um Station.
- Compile `:mc26_3:fabric` + `:modules:simplequestionary:fabric`, Targets `module-simplequestionary-fabric-263`,
  `module-simplequestionary-standalone-fabric-263`, Filter `*questionary*` auf `fabric-263`; Generatoren + `check`.

## Risiken
- Fensterhöhe B 238 nahe an der Grenze 240 (GUI-Skala 2 auf 854×480) – vertretbar, sonst Variante A.
- Client-Smoke (`ModuleClientSmoke`) nutzt `tabCenterX/Y` – bleibt als API erhalten. Client-Läufe sind verboten →
  Client-Sicht bleibt offen (Besitzer-Abnahme).
