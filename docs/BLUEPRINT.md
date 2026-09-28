# Blaupause / Blueprint – Spezifikation

Stand 2026-09-27 (dritte Runde: Beispiele, Hilfe, Einfuege-Leiste, Layout, Autospeichern, Kopieren, nur signiert bauen; dazu Formen und Variablen in der Bausprache, 1.8). Code: `common/src/shared/java/com/simplebuilding/blueprint/` (26.2) und
`mc1_21_11/shared/java/com/simplebuilding/blueprint/` (1.21.11), Spieltests `BlueprintTests`.

Die Blaupause (`simplebuilding:blueprint`, en "Blueprint", de "Blaupause") speichert ein Bauwerk
als kurzen, lesbaren **Bau-Code**. Sie sieht aus wie ein Kartenblatt (Cyanotypie-Blau), nicht wie
ein Buch.

- **Rezept** (formlos): 1 Enderquarz + 1 Papier + 1 Tintenbeutel → 1 leere Blaupause.
- **Stapel**: leer bis 16; wird eine Blaupause aus einem Stapel beschrieben, bleibt sie im Slot
  und der Rest des Stapels wandert ins Inventar (so trifft jede weitere Autospeicherung dieselbe).
- **Neu** ist eine Blaupause leer; Beispielcode gibt es erst per Knopf im Editor (Abschnitt 2.2).
- **Textur nach Zustand** (2026-09-28): leer das normale hellblaue Blatt, bearbeitet (nicht signiert)
  mit weissem Federkiel darueber wie Vanillas Buch mit Feder, signiert ein eigenes Blatt in Nachtblau mit
  Goldrahmen, Reinzeichnung ohne Raster und rotem Wachssiegel mit Bandenden. `items/blueprint.json` fragt `minecraft:has_component`
  (`simplebuilding:blueprint`), dann `simplebuilding:blueprint_state` (`BlueprintItem#modelState`;
  wieder geleerter Code und Titel gelten als leer). Texturen aus `tools/textures/generate_textures.py`.
- **Kreativ-Tab**: Werkzeuge, hinter den Oktanten.
- **Datenkomponenten**: `simplebuilding:blueprint` = `{code, title, author, signed}`,
  `simplebuilding:blueprint_rotation` = 0..3 (Viertelumdrehungen im Baumodus).

## 1. Die Bausprache

Ein Bauwerk liegt in einem lokalen Raster von **256 × 256 × 256** Feldern, jede Koordinate liegt
in `0..255`. x zeigt nach Osten, y nach oben, z nach Süden (so, wie gescannt wird).

### 1.1 Grammatik

```
code        = { line "\n" } ;
line        = [ statement ] [ comment ] ;
comment     = "#" { any character } ;                       (bis Zeilenende)
statement   = alias-def | var-def | placement ;
alias-def   = "$" name "=" block ;                          (Leerzeichen um "=" erlaubt)
var-def     = "$" name "=" expr ;                           (expr beginnt mit Ziffer, + - ( oder $)
placement   = ( block | "$" name ) region { region } ;      (Wörter durch Leerraum getrennt)
block       = [ namespace ":" ] path [ "[" props "]" ] ;    (minecraft: darf fehlen)
props       = prop { "," prop } ;                           (Leerzeichen in [...] erlaubt)
prop        = property-name "=" value ;
region      = ( box | shape ) [ "*" num "@" num "," num "," num ] ;
box         = axis "," axis "," axis                        (Achsenform)
            | num "," num "," num ".." num "," num "," num ;(Eckenform)
axis        = num [ ".." num ] ;                            (Bereich, beide Enden inklusive)
shape       = [ "hollow_" ] shape-name "(" ( box | num { "," num } ) ")" ;
shape-name  = "box" | "sphere" | "dome" | "cylinder" | "pyramid" | "line" ;
num         = expr ohne "*" außerhalb von Klammern ;        (dort leitet "*" die Wiederholung ein)
expr        = term { ( "+" | "-" ) term } ;
term        = factor { ( "*" | "/" | "%" ) factor } ;
factor      = int | "$" name | "(" expr ")" | ( "+" | "-" ) factor ;
int         = digit { digit } ;                             (höchstens 6 Ziffern)
name        = letter { letter | digit | "_" } ;             (höchstens 24 Zeichen)
```

Leerzeichen sind innerhalb von `[...]` und `(...)` erlaubt, in Ausdrücken einer Variablen-Definition
überall.

### 1.2 Bedeutung

| Schreibweise | Bedeutung |
|---|---|
| `stone 1,2,3` | ein Stein an x=1, y=2, z=3 |
| `stone 0..4,0,0..4` | 5 × 1 × 5-Platte (je Achse ein Bereich) |
| `stone 1,1,1..1,1,5` | Eckenform: von Ecke 1,1,1 bis Ecke 1,1,5 (5 Steine) |
| `stone 0,0,0 4,0,4 2,1,2` | mehrere Regionen in einer Zeile |
| `oak_stairs[facing=east,half=top] 0,3,0` | Blockzustands-Eigenschaften (Name=Wert) |
| `oak_fence 0,1,0*5@2,0,0` | Wiederholung: 5 Kopien, jede um (2,0,0) weiter (x = 0,2,4,6,8) |
| `air 1..3,1..3,1..3` | räumt Stellen, die frühere Zeilen gesetzt haben (hohler Kasten) |
| `$dach = oak_stairs[facing=north]` | Alias; danach `$dach 0..4,4,0` |
| `$r = 5` | Variable; danach `stone 0..$r,0,$r+1` (1.8) |
| `glass sphere(8,8,8,5)` | Form statt Position (1.8) |
| `# ...` | Kommentar bis Zeilenende |

- **Reihenfolge**: spätere Anweisungen überschreiben frühere an derselben Stelle.
- **Eigenschaften**: nicht genannte Eigenschaften haben den Standardwert des Blocks.
- **Bereiche** dürfen rückwärts stehen (`4..0` = `0..4`).
- **Wiederholung**: `count` 1..256; `dx,dy,dz` dürfen negativ sein; jede Kopie muss ganz im
  Raster bleiben.
- **Leerraum**: Leerzeichen/Tabs trennen Wörter; innerhalb von `[...]` sind sie erlaubt.

### 1.3 Beispiel

```
# Huette 5x6x5
$dach = oak_stairs[facing=north]
cobblestone 0..4,0,0..4
oak_planks 0..4,1..3,0..4
air 1..3,1..3,1..3          # innen hohl
glass_pane 2,2,0            # Fenster
oak_door 2,1,4 
oak_door[half=upper] 2,2,4
$dach 0..4,4,0
oak_fence 0,5,0*3@2,0,0
```

### 1.4 Grenzen

| Grenze | Wert | Wo |
|---|---|---|
| Codelänge | 32 000 Zeichen | `BlueprintCode.MAX_CODE_LENGTH` – passt in `STRING_UTF8`, Fehler `too_long` |
| Koordinaten | 0..255 | Fehler `coordinate_range` |
| Wiederholungen | 1..256 | Fehler `repeat_range` |
| ausgerollte Stellen gesamt = belegte Stellen | 4 194 304 (256 × 256 × 64) | Fehler `too_many_cells`, geprüft *vor* dem Ausrollen; beim Scan `too_many_blocks` |
| Formen: Summe der Box-Volumen | 16 777 216 (256³) | `BlueprintCode.MAX_SHAPE_VOLUME`, Fehler `shape_volume` – begrenzt die Prüfarbeit kurzer Codes mit Riesenformen |
| Zahlen und Zwischenergebnisse | ±999 999 (6 Ziffern) | Fehler `number_range`; Klammern/Vorzeichen höchstens 32 tief (`bad_expression`) |
| Item-Daten | ≤ 32 000 Zeichen Code ≈ 32 KB je Blaupause | durch die Codelänge begrenzt, nicht durch die Blockzahl |
| Titel | 1..32 Zeichen | Editor und Server |

### 1.5 Fehler

Jeder Fehler trägt Zeile, Zeichenbereich und Schlüssel (`simplebuilding.blueprint.error.<key>`):
`too_long`, `no_region`, `bad_alias_name`, `alias_without_block`, `alias_extra`, `alias_of_alias`,
`unknown_alias`, `bad_block_id`, `unknown_block`, `unclosed_properties`, `empty_property`,
`bad_property`, `unknown_property`, `bad_value`, `bad_repeat`, `repeat_range`, `bad_region`,
`coordinate_range`, `too_many_cells`, `repeat_outside`, dazu für Formen und Variablen (1.8)
`unknown_shape`, `bad_shape_args`, `shape_size`, `shape_volume`, `bad_expression`,
`unknown_variable`, `alias_as_number`, `variable_as_block`, `number_range`, `division_by_zero`.
Der Zeichenbereich zeigt auf das schuldige Wort: bei `unknown_variable` genau auf `$name`, bei
`unknown_shape` auf den Formnamen, bei `shape_size` auf die zu kleine Angabe, bei
`division_by_zero` auf den Operator. Eine fehlerhafte Zeile setzt nichts; die
übrigen Zeilen gelten weiter (die Vorschau zeigt, was gültig ist). Bauen und Signieren verlangen
fehlerfreien Code.

### 1.6 Serialisierer (Scan → Code)

`BlueprintCode.serialize(model)` schreibt die kürzeste lesbare Form, die der Parser genau so
zurückliest (`parse(serialize(m)).model() == m`, Spieltest):

1. Kopfkommentar `# BxHxT, N blocks`.
2. Je Blockzustand eine Anweisung, häufigster zuerst; Id ohne `minecraft:`, nur Eigenschaften
   abseits des Standards.
3. Stellen gierig zu Quadern zusammengefasst: Reihe entlang x, dann so weit wie möglich in z,
   dann Schichten in y. Ausgabe in Achsenform (`0..4,0,0..4`).
4. Zeilen brechen bei 100 Zeichen um (derselbe Block beginnt die nächste Zeile); ein Zustand mit
   mehr als 12 Zeichen, der mehr als eine Zeile braucht, bekommt einen Alias (`$a`, `$b`, …).

### 1.7 Syntax-Einfärbung

Eine Klasse je Zeichen (`BlueprintCode.STYLE_*`): Kommentar, Block, Namensraum, Eigenschaft
(auch Formnamen), Wert, Alias (auch Variablen), Zahl, Operator (`, .. * @ = [ ] ( ) + / %`),
Fehler, `air`. Farben im Editor: siehe `BlueprintCodeArea.STYLE_COLORS` (dunkle Tinte auf Papier).

### 1.8 Formen und Variablen

Beides ist rückwärtsverträglich: jeder Code, der vorher galt, ergibt Stelle für Stelle dasselbe
Modell (Spieltest `code_extensions_keep_existing_codes_identical` über alle Beispielvorlagen);
alte Fehler behalten ihren Schlüssel. Formwörter beginnen mit einem Buchstaben und enthalten `(`,
kollidieren also nie mit einer Position; `$name = <Zahl oder Rechnung>` war vorher ein Fehler.

**Formen** stehen überall, wo eine Position steht (mehrere je Zeile, mit Wiederholung
`*n@dx,dy,dz`, mit `air` zum Aushöhlen):

| Form | Zahlenform | Bedeutung |
|---|---|---|
| `box` | `box(x,y,z,w,h,d)` | Quader ab Ecke x,y,z, `w × h × d` groß (≥ 1) |
| `sphere` | `sphere(cx,cy,cz,r)` | Kugel um die Mitte, Radius r ≥ 0 (Box `c−r..c+r`) |
| `dome` | `dome(cx,cy,cz,r)` | obere Hälfte (ab `cy`) von `sphere(cx,cy,cz,r)` |
| `cylinder` | `cylinder(cx,y,cz,r,h)` | stehender Zylinder, Grundfläche auf Höhe y, h ≥ 1 hoch |
| `pyramid` | `pyramid(cx,y,cz,r,h)` | Grundfläche `(2r+1)²` auf Höhe y, Spitze oben, h hoch |
| `line` | `line(x1,y1,z1..x2,y2,z2)` | gerade Linie von Ecke zu Ecke (nur Eckenform) |

- **Boxform**: jede Form außer `line` nimmt statt der Zahlen auch eine Box in Achsen- oder
  Eckenform, etwa `sphere(0..9,0..5,0..9)` (Ellipsoid) oder `cylinder(0,0,0..8,3,4)`. Sie füllt
  genau die Figur, die ein Oktant mit dieser Auswahl zeigt und scannt (`OctantShape`, Ausrichtung
  oben) – Kugel, Zylinder, Pyramide und Quader des Oktanten sind also 1:1 schreibbar. Die
  Zahlenform ist nur Kurzschrift für die Box um die Mitte. Die Kuppel mit einer Box der Höhe h
  (Schichten `y..y+h−1`) ist das Ellipsoid über den Schichten `y−h+1..y+h−1`, abgeschnitten
  unterhalb von y. Radius r heißt: Mitte-zu-Mitte-Abstand ≤ r + ½.
- **Hohl**: `hollow_` vor dem Namen (`hollow_sphere`, `hollow_box`, `hollow_dome`,
  `hollow_cylinder`, `hollow_pyramid`) lässt nur die Hülle stehen – Stellen mit mindestens einem
  der sechs Nachbarn außerhalb der Figur, wie „Hohl“ am Oktanten. Deckel und Boden gehören dazu
  (Rand der Box zählt als außen), eine 2 breite Form hat kein Inneres.
- **Linie**: so viele Schritte wie die längste Achse, jede Achse auf die nächste Stelle gerundet
  (halbe nach oben) – rückwärts geschrieben dieselbe Linie.
- **Grenzen**: die Box jeder Form muss ganz im Raster liegen (`coordinate_range` nennt die
  Koordinate, z. B. −72 für `sphere(128,128,128,200)`). Gesetzte Stellen zählen gegen
  `MAX_EXPANDED_CELLS` (eine massive Kugel mit Radius 127 wird vor dem Ausrollen abgelehnt, die
  hohle passt), das Rastern selbst gegen `MAX_SHAPE_VOLUME` (Summe der Box-Volumen aller Formen
  eines Codes ≤ 256³, Fehler `shape_volume`).

**Variablen**: `$name = Ausdruck` legt eine ganze Zahl fest; gilt ab der nächsten Zeile, eine
spätere Definition überschreibt. Ausdrücke: Zahlen (bis 6 Ziffern), `$name`, `+ - * / %`
(Punkt vor Strich, `/` und `%` runden nach unten: `-7 / 2 = -4`), Klammern, Vorzeichen.
Benutzbar in jeder Zahl einer Region: Koordinaten, Bereichsenden, Formangaben,
Wiederholungszahl und Schritt.

```
$r = 5
$h = $r * 2 + 1             # 11
$n = ($h + 1) / 4           # 3
stone 0..$r,0,0..$r-1       # 6 × 1 × 5
glass sphere(20,$h,20,$r)
oak_fence 0,1,0*$n+1@2,0,0  # Wiederholungszahl 4
dirt ($r*3),0,20            # Malnehmen in einer Position nur in Klammern
```

- In einer **Region** leitet das erste `*` außerhalb von Klammern die Wiederholung ein;
  Multiplikation steht dort in Klammern (`($r*2),0,0`). In der Wiederholungszahl, in
  Formangaben (in der Klammer der Form) und in Definitionen ist `*` normal.
- Leerzeichen: in Definitionen überall, in Regionen nur innerhalb von Klammern
  (`( $r + 1 ),0,0`, `sphere(8, 8, 8, $r)`).
- Aliase und Variablen teilen sich die Namen; die jüngste Definition gilt. `$a = $b` bleibt der
  Fehler `alias_of_alias`, wenn `$b` ein Alias ist; ein Alias in einer Rechnung ist
  `alias_as_number`, eine Variable als Block `variable_as_block`.

Noch nicht umgesetzt: relative Regionen und Spiegelung.

## 2. Editor

Benutzen öffnet den Editor (nicht im Baumodus, siehe 4). Ein Kartenblatt, größer als ein Buch
(bis 560 × 340 GUI-Pixel), kein Blättern:

- **links** fest stehend die **Materialliste**: Item-Symbol, Menge, Name, größte Menge zuerst;
  Mausrad rollt, Überfahren zeigt Stapel (`3×64 + 5`). Reine Kreativ-Blöcke rot. **Darunter**, in
  ihrer Breite: die Maße (`5 × 4 × 5`) mit der Blockzahl daneben, darunter der nötige Baustab als
  Item-Symbol mit Stufenname (Kupfer, Eisen …) und `Kante / Grenze`, darunter die Leiste
  „genutzt / frei“ bezogen auf die Grenze dieser Stufe.
- **Mitte** der **Code**: ein scrollbares Feld mit Bildlaufleiste, Zeilennummern,
  Syntax-Farben und rot unterstrichenen Fehlern. Die Schrift ist bei GUI-Maßstab ≥ 3 kleiner
  (3/4 bzw. 2/3, ganzzahlige Pixel). **Direkt darunter** nur der Status in Code-Breite: „Code in
  Ordnung“ (grün), „Ungültig: Zeile n: …“ (rot, der Fehler unter dem Cursor, sonst der erste) oder
  „Leer“. **Darunter die Einfüge-Leiste**: Suchfeld für Blöcke (angezeigter Name in der
  Spielsprache oder ID), Treffer als Symbole; ein Klick wählt einen Treffer aus, erst der Knopf
  „Einfügen“ setzt den technischen Namen (ID ohne `minecraft:`) an die Cursorposition – nie
  versehentlich mit einem Klick.
- **rechts** das **Bauwerk in 3D**: Ziehen dreht frei in jede Richtung (Trackball, auch kopfüber),
  Mausrad zoomt, Doppelklick setzt zurück. Ist der Code leer, steht dort der Hinweis „Schreib eine
  Zeile … und das Bauwerk erscheint hier“ und ganz unten der Knopf **„Beispiel einfügen“** (2.2).
  Oben rechts ein **Buch-Knopf** (wie Vanillas Rezeptbuch) öffnet statt der 3D-Ansicht die
  **Hilfe** (2.1). **Darunter** bündig „Signieren“ und „Fertig“, zusammen genau so breit wie die
  Vorschau (signiert: nur „Fertig“ über die ganze Breite).
- **Signieren** wie beim Buch: Titel (1–32 Zeichen), danach schreibgeschützt; Titel wird zum
  Namen, der Autor steht im Tooltip. Signieren geht nur mit fehlerfreiem, nicht leerem Code.
- **Autospeichern**: jede Änderung geht 1,5 s nach der letzten Eingabe per `BlueprintEditPayload`
  an den Server, zusätzlich beim Schließen (Esc, „Fertig“, Inventar-Taste, wenn kein Textfeld den
  Fokus hat) und in `Screen.removed()` – also auch, wenn ein anderer Bildschirm übernimmt, die Welt
  verlassen oder die Verbindung getrennt wird. Der Server prüft Slot, Item, Signatur, Länge (beim
  Signieren Titel und Code) und legt den Code **sofort** am Item ab. Ein harter Abbruch (Absturz,
  Kabel) kostet höchstens die letzten 1,5 s.

### 2.1 Hilfe

Zwei Reiter: **Anleitung** (Kurzfassung der Bausprache mit Beispielen, dreizehn Absätze – die letzten drei zu Formen,
Hohl und Variablen –, Mausrad
rollt) und **Blöcke** (Suchfeld für angezeigten Namen oder ID, Liste mit Symbol, Anzeigename und
technischem Namen; ein Klick wählt den Block für die Einfüge-Leiste aus). Die Suche ist
`BlueprintBlockSearch`: exakter Treffer (ID, Pfad oder Name) zuerst, dann Anfang von ID oder
Name, dann Wortanfang, dann irgendwo enthalten.

### 2.2 Beispiele

Nur bei leerem Code und unsignierter Blaupause: „Beispiel einfügen“ setzt ein Beispiel-Bauwerk
aus dem **Biom an der Position des Spielers** ein, zufällig eines aus dessen Gruppe. Alle
höchstens 16 × 16 × 16, handgeschrieben und kommentiert als Ressourcen
`data/simplebuilding/blueprint_examples/<name>.sbp` (Aliase, Bereiche, Wiederholung, `air`):

| Gruppe | Biome | Vorlagen |
|---|---|---|
| Ebene | alle übrigen (auch Nether/Ende) | Dorfhaus, Dorfbrunnen, Ruinenportal-Rest |
| Wüste | Wüste, Tafelberge | Dorfhaus, Wüstenbrunnen, Tempelruine |
| Savanne | Savanne, Hochebene, zerklüftete Savanne | Dorfhaus, Marktstand |
| Taiga | Taiga, alte Taigas, zerklüftete Hügel/Wald | Dorfhaus, Lagerfeuer-Camp |
| Schnee | verschneite Biome, Eis, Hain, Gipfel | Dorfhaus, Iglu |
| Kirsche | Kirschhain | Dorfhaus, Pavillon |
| Sumpf | Sumpf, Mangrovensumpf, Dschungel | Stelzenhaus, Sumpfhütte |

Das Einfügen ändert nur den Editor-Inhalt; gespeichert wird über den normalen Edit-Payload mit
Prüfung. Signierte Blaupausen haben keinen Knopf.

Technik: Die 3D-Ansicht (Editor und Tooltip) setzt die Blockmodelle zu einem Netz zusammen
(innere Flächen zwischen vollen Blöcken fallen weg), projiziert es orthografisch auf der CPU,
sortiert von hinten nach vorn und reicht es als **ein** `GuiElementRenderState` mit dem
Block-Atlas ein – ohne eigenen Picture-in-Picture-Renderer, deshalb auf allen Loadern gleich.
Blöcke ohne Blockmodell (Truhe, Schild, Flüssigkeiten) erscheinen als Würfel mit ihrem
Partikelbild. Über 60 000 Flächen zeigt die Ansicht nur einen Teil, über 300 000 Blöcke gar kein Netz
(der Aufbau liefe sonst spürbar lange auf dem Render-Thread).

## 3. Tooltip

Über jeder gefüllten Blaupause (Inventar, Hotbar, Kreativ): Größe, Blockzahl, benötigter Baustab,
Fehlerzahl, Autor, darunter eine **langsam drehende 3D-Miniatur** (88 × 88, eine Umdrehung in 9 s).
Mit **Umschalt** die Materialliste (bis 8 Zeilen mit Symbol, dann "… und N weitere").
`BlueprintTooltipData` (Item) → `BlueprintTooltip` (Client), registriert auf Fabric
(`ClientTooltipComponentCallback` / `TooltipComponentCallback`), NeoForge und Forge
(`RegisterClientTooltipComponentFactoriesEvent`).

## 4. Bauen

**Baustab in der Haupthand + signierte Blaupause in der Nebenhand** = Baumodus. Eine
**unsignierte** Blaupause zeigt keine Vorschau und baut nicht; die Aktionsleiste sagt
„Blaupause signieren, um sie zu bauen“. (Zum Weiterbearbeiten einer signierten: Kopie am
Kartentisch, Abschnitt 5.1.)

- **Vorschau**: Geisterblöcke am anvisierten Block – genau das, was ein Klick jetzt setzen würde
  (vorhandenes Material, freie Stellen), bis 4096 Geister. Stellen, für die Material fehlt,
  erscheinen **rot**.
- **Zwei-Klick-Regel**: fehlt Material, baut der erste Rechtsklick nichts – die roten Stellen
  leuchten kurz kräftig auf, ein Hinweiston erklingt, die Aktionsleiste nennt die Zahl der
  fehlenden Blöcke. Ein zweiter Rechtsklick binnen 3 s baut alles Vorhandene. Fehlt nichts, baut
  schon der erste Klick. Die Bestätigung gilt nur für diesen einen Bau. Die Prüfung davor hat
  keine Obergrenze: 131 072 Stellen gleich beim Klick (für alle Klicks eines Spielers im selben
  Tick zusammen; ein weiterer Klick auf denselben Bau startet eine laufende Prüfung nicht neu),
  den Rest sehr großer Bauwerke mit 131 072 Stellen je Tick danach („Prüfe Material… x / y“),
  dann Warnung oder Baubeginn; ein bestätigender zweiter Klick prüft nicht erneut.
- **Ausrichtung**: das Bauwerk steht mittig (x) auf dem Zielblock (in den geklickten Block, wenn
  er ersetzbar ist, sonst davor), seine Unterkante auf dessen Höhe, seine lokale z-Achse zeigt in
  Blickrichtung. Nach Süden blickend entsteht es wie gescannt. **Strg + Mausrad** dreht um 90°
  (gespeichert auf der Blaupause); Blockzustände drehen mit (`BlockState.rotate`).
- **Rechtsklick** setzt alles, was geht:
  - Material wie beim Baustab: Nebenhand, Hotbar; mit Meisterbauer auf dem Stab das ganze
    Inventar; Meisterbauer-Bündel (Meisterbauer auf Stab *oder* Bündel); getragener
    Meisterbauer-Rucksack. Gesucht wird nach Item (Wandfackel → Fackel).
  - Kosten je Zustand: 1 Item; doppelte Stufe 2; Mengen-Eigenschaften zählen ihren Wert
    (`candles`, `pickles`, `eggs`, `layers`, `flower_amount`, `segment_amount` – vier Kerzen
    = 4 Kerzen, fünf Schneeschichten = 5); Mehrflächen-Blöcke (Leuchtflechte, Sculkader,
    Harzklumpen, Ranken) 1 je belegter Fläche; obere Tür-/Pflanzenhälfte und Bettkopf 0 (nur
    gesetzt, wenn ihr Gegenstück steht oder gerade gesetzt wurde); Zustände ohne Item nur Kreativ.
    Wassergeflutete Zustände werden im Überleben trocken gesetzt.
  - Übersprungen: Stellen mit schon dem richtigen Block, belegte Stellen (nicht ersetzbar),
    geschützte Stellen (`mayInteract`, `mayUseItemAt`, Weltgrenze, Bauhöhe, nicht geladen).
  - Fehlendes Material → Stelle bleibt frei; später erneut klicken füllt nach.
  - Je gesetztem Block 1 Haltbarkeit (wie der Baustab), Abbruch wenn der Stab bricht.
  - Kreativ setzt alles.
  - **Sichtbares Wachsen** wie beim Baustab, aber schneller: Schicht für Schicht von unten nach
    oben, in jeder Schicht von der Mitte nach außen (Bettkopfteile ans Ende ihrer Schicht). Die
    Bauzeit ist gedämpft: `ticks = 20 + 160 · ln(1 + n/64) / ln(1 + 4 194 304/64)`, also rund 1 s
    für ein kleines Haus, 3 s für 1000 Blöcke und höchstens 9 s für das größte Bauwerk; je Tick wird
    der Anteil `⌈n / ticks⌉` der Stellen bearbeitet – so gibt es auch bei Riesenbauten kein Hängen.
    Fortschritt „Baue… geprüft / gesamt, gesetzt“ in der Aktionsleiste.
  - **Abbruch** wie beim Baustab: anderes Werkzeug in der Haupthand (auch kurz), Blaupause aus der
    Nebenhand genommen, Baustab zerbrochen. Ein abgebrochener Bau wird nicht wieder aufgenommen;
    ein neuer Klick ersetzt einen laufenden Auftrag.
  - **Logout und Neustart** brechen nicht ab: der Stand (Code-Hash, Ziel, Drehung, Fortschritt in
    der Bau-Reihenfolge, gesetzte Blöcke) steht nach jeder Scheibe in `BlueprintJobs`
    (SavedData je Dimension). Hält der Spieler danach Baustab und eine signierte Blaupause mit
    demselben Code (Kopie genügt), läuft der Auftrag an derselben Stelle weiter, ohne erneute
    Zwei-Klick-Prüfung; mit anderer oder ohne Blaupause nennt die Aktionsleiste einmal je Sitzung
    Titel und Ort des wartenden Baus. Weitergebaut wird nur, wenn der Zielblock geladen ist.
- **Stufengrenze** (längste Kante der Bounding Box, Würfel):

  | Baustab | Kupfer | Eisen | Gold | Diamant | Netherit | Enderit |
  |---|---|---|---|---|---|---|
  | Kante | 16 | 32 | 48 | 64 | 128 | 256 |

  Vom Besitzer entschieden (2026-09-25).
  Ein zu kleiner Stab lehnt ab ("braucht Eisen-Baustab").

## 5. Scannen am Kartentisch

Oktant mit beiden Ecken in den **oberen** Slot, leere oder unsignierte Blaupause in den
**unteren** → rechts die gefüllte Blaupause. Nehmen verbraucht nur die Blaupause, der Oktant
bleibt. Umschalt-Klick legt beide in ihre Slots.

- Gescannt wird genau die **Figur** des Oktanten (Quader, Rechteck, Zylinder, Ellipse, Kugel,
  Pyramide, Prisma, mit seiner Ausrichtung) – dieselben Blöcke, die seine Vorschau im Client zeigt
  (`OctantShape`, eine Quelle für beide). Die Bounding Box ist höchstens 256 je Kante groß und
  geladen; der Tisch steht höchstens 32 Blöcke von ihr entfernt.
- **Mehrere Ticks**: bis 262 144 Stellen fertig im selben Tick, größere Auswahlen je Tick 262 144
  Stellen weiter (ein voller 256er-Würfel in 64 Ticks, gut 3 s), angetrieben vom offenen Menü;
  Fortschritt "Scanne… n %" in der Aktionsleiste. Anderes in den Tisch legen bricht ab. Mehr als
  4 194 304 belegte Stellen → Abbruch (`too_many_blocks`).
- Luft (und `moving_piston`) wird übersprungen; von Block-Entities wird nur der Zustand
  übernommen, kein Inhalt. Koordinaten beginnen an der kleinsten belegten Stelle.
- Ergebnis-Code länger als 32 000 Zeichen → kein Ergebnis ("zu komplex").
- Ablehnungsgründe erscheinen in der Aktionsleiste.
- Solange der Oktant im Tisch liegt, zeichnet der Client seine Auswahl (Ecken + Figur) in der Welt.

Technik: `CartographyTableMenuMixin` ersetzt die drei Tisch-Slots durch Hüllen
(`BlueprintCartography`) und übernimmt `setupResultSlot`, sobald ein Oktant oben oder eine
Blaupause unten liegt.

### 5.1 Kopieren am Kartentisch

**Signierte** Blaupause oben, **leere** unten → rechts eine **unsignierte Kopie** mit demselben
Code und Titel, ohne Autor, also wieder bearbeitbar. Wie beim Karten-Kopieren bleibt das Original
liegen, verbraucht wird nur die leere. Der Kopier-Pfad greift nur mit einer Blaupause oben, der
Scan-Pfad nur mit einem Oktanten oben; eine unsignierte Blaupause nimmt der obere Slot nicht an,
eine beschriebene unten gibt keine Kopie. Umschalt-Klick legt signierte Blaupausen nach oben,
andere nach unten.

### 5.2 Vorschau im Kartentisch (Client)

Liegt oben eine **signierte** Blaupause, zeigt das große Kartenfeld (Innenfläche des
Pergamentblatts, 58×58 GUI-Pixel ab 71/17) dieselbe kreisende 3D-Miniatur wie der Tooltip:
getönte Fläche, Bauwerk mit 2 px Rand, 28° von oben, eine Umdrehung in 9 s. Das Pergamentblatt
bleibt als Rahmen stehen. Unsignierte Blaupausen, Karten und alles andere bleiben Vanilla; liegt
unten Papier, eine Karte oder eine Glasscheibe, zeichnet Vanilla sein eigenes Bild (Vergrößern,
Kopieren, Sperren) und es gibt keine Vorschau (`BlueprintCartography.previewCode`).

Technik: `CartographyTableScreenMixin` (Client-Mixin-Konfiguration aller Loader) hängt sich an das
Ende von `extractBackground` (26.2/26.3/26.4) bzw. `renderBg` (1.21.11), öffnet eine neue
GUI-Schicht (`nextStratum`, sonst sortiert der Renderer das Blatt über die Miniatur) und ruft
`BlueprintTooltip.extractPreview` / `renderPreview` – denselben Code wie der Tooltip. Nichts
davon läuft auf dem Server. Client-Test: `ModScreensClientTest.cartographyTableShowsTheBlueprintPreview`
(Screenshots `cartography-a..d`).

## 6. Offene Entscheidungen

- Große Codes (Millionen Stellen) parsen spürbar lange (einmal je Code, danach zwischengespeichert);
  der Serialisierer am Scan-Ende läuft in einem Tick.
- Kosten je Block ohne Rücksicht auf Sonderfälle wie Kerzen-/Seegurken-Anzahl, Schnee-Schichten.
- Konstrukteurs-Berührung spielt beim Bauen keine eigene Rolle (die Materialsuche ist die des
  Baustabs, also Meisterbauer).
- Kein Kreativ-Import/Export des Codes über die Zwischenablage außer Strg+C/Strg+V im Editor.
