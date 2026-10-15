# Vorschlag (Schritt 1, nur Text): 3D-Darstellung platzierter Items nach „Items Displayed“

Queue: `.claude/QUEUE.md` Nachtrag 62. Stand 2026-10-15, Branch `oc-display-proposal`.
Ablauf verbindlich: (1) dieser Text → Freigabe, (2) Vorher/Nachher-Bilder → Freigabe, (3) erst dann Einbau.
Dieser Schritt ändert **keinen Code, keine Modelle, keine Bilder**. Alle abgenommenen Texturen bleiben unangetastet.

## 0. Kurzfassung

- Items Displayed baut jedes Teil als **kleines Blockmodell aus Quadern** (Silhouette in Stufen), mit **eigener handgemalter
  Textur** je Teil. Dicke: Scherbe 2 px, Schallplatte 1 px, Vorlage 4 px (3 + 1 erhabenes Motiv), Barren 3 px je Lage.
- Wir zeichnen Vorlagen heute als **ausgestanzte Item-Textur, ca. 1,4 px dick** (`item/generated`, 14/16 Kantenlänge);
  Scherben sind **gar nicht platzierbar**; Schallplatten sind nur die **15x10-Ellipse** des Item-Bildes, 1,4 px dick.
- Empfehlung: **eine gemeinsame Technik** („Silhouetten-Extrusion als Blockmodell“): die vorhandene Item-Textur wird direkt als
  Textur des Modells benutzt (nichts kopiert, nichts geändert), Quader folgen der Silhouette, Kanten bekommen Blockschattierung.
  Nur die Schallplatte braucht zusätzlich eine **neue runde Draufsicht-Textur** (Build-erzeugt, nicht eingecheckt).
- Reihenfolge nach Nutzen/Risiko: **Scherben (neu)** und **Schallplatten** zuerst, dann **Vorlagen**, dann „weitere“.

## 1. Recherche „Items Displayed“

Quellen: Modrinth-/CurseForge-Seite, GitHub `FREEDINN3R/Items-Displayed-Fabric` (Wiki „Placeable items“, Quelltext und
Modell-JSON, Zweig 1.21.1; die Seite nennt 26.3 als unterstützt, Maße dort können leicht abweichen).
**Nicht im Spiel gesehen, keine Bilder angeschaut** – die Angaben unten kommen aus Modell-JSON und Block-Klassen.
Lizenz **GPL-3.0**: Modelle, Texturen und Code dürfen **nicht** übernommen werden; wir nehmen nur die Maße als Vorbild und
bauen alles selbst aus unseren eigenen Quellen.

| Aspekt | Items Displayed (belegt) |
|---|---|
| Aufbau | Je Item ein Blockzustand mit statischem Quader-Modell (Blockbench), 4 Himmelsrichtungen (`facing`), wasserbefüllbar, ohne Werkzeug sofort abbaubar, braucht tragenden Boden. Kein Block-Entity-Renderer. |
| Trims/Vorlagen | 17-18 Modelle, je 9-12 Quader. Grundfläche ca. 8 x 12 px (x 4-12, z 2-14), **3 px Sockel + 1 px erhabene Motivlage = 4 px**. Trefferform Quader 8x4x12. Eigene 32x32-Textur je Vorlage. |
| Pottery Sherds | 23 Modelle, je ca. 9 Quader, **2 px dick**, Silhouette der Scherbe in Stufen, Grundfläche bis 14 x 14 px. Trefferform 1-15 x 2 hoch x 1-15. |
| Music Discs | ca. 21 Modelle, **7 Streifen von je 1 px Breite** ergeben eine **runde Scheibe**, **1 px dick**, Durchmesser 12 px (Trefferform 2-14 x 1 hoch). Eigene Draufsicht-Textur je Platte (Rille, Etikett). |
| Barren/Edelsteine | Barren 5 x 10 x 3 px, bis 3 Stück (2 unten, 1 quer darauf); Edelsteine/Lapis bis 4 Stück. Kristalle (Amethyst, Echo) bis 4, Scheiben-Fragment bis 8 Varianten. |
| Ausrichtung | Nur die 4 Himmelsrichtungen; Teile liegen flach auf dem Boden (Wand/Decke gibt es nicht). Zusätzlich ein **Item-Display** (Rüstungsständer-artig, frei drehbar) für Scherben/Vorlagen. |
| Licht | Normales Blocklicht des Blocks; Blockmodell-Schattierung je Seite (oben hell, Seiten dunkler, unten am dunkelsten); nichts leuchtet von selbst. |

## 2. Ist-Zustand SimpleBuilding (aus dem Code)

| Aspekt | Heute (Datei) |
|---|---|
| Einzelne Vorlage/Teil | `PlacedTemplateBlock` + `PlacedTemplateRenderer`: Item-Modell mit `ItemDisplayContext.NONE`, also die **ausgestanzte Item-Textur** (`item/generated`), Kantenlänge `PlacedPlate.SCALE` = 14/16, Dicke `THICKNESS_SCALE` 1,6 → **ca. 1,4 px**. Boden/Wand/Decke (`PlacedPlate.transform`), Oberkante zeigt in Blickrichtung beim Legen. Trefferform pixelgenau aus der Deckungsmaske (`PlacedPlate.build`). |
| Häufchen (bis 4) | `PlacedSmallPartsBlock` + `PlacedSmallPartsRenderer`: feste Plätze `SLOTS` mit kleinen Drehungen (-14 bis +16 Grad), Platten ab 2 Teilen **8/16 groß, ca. 0,8 px dick**, je Teil nur **0,2 px** höher (`LIFT`). |
| 3D schon vorhanden | Ei (`EGG_BOXES`), Barren (`INGOT_BOXES`, Stapel `INGOT_STACK`), Ziegel (`BRICK_BOXES`), Diamant/Smaragd (`GEM_ROWS`, halber Maßstab), Kerze, Seegurke. Erzeugt von `tools/textures/placed_ingots_2026_10_09.py` und `placed_gems_bricks_2026_10_15.py`; Hammer schaltet auf flach (`Kind.switchable`). Diamant/Smaragd/Ziegel verweisen **direkt auf die Item-Textur** (kein Pixel kopiert). |
| Vorlagen (Trims) | Auf 26.3 legt die Sub-Mod **Simple Trims** Besatzvorlagen ab (`modules/simpletrims/.../placed/*`, **eigene Kopie** von `PlacedPlate` und Renderer, Hammer-Aufwertung zu Glowing/Emitting). SimpleBuilding legt sie nur ab, wenn Simple Trims fehlt (`leftToSimpleTrims`); Netherite-/Enderit-/Basis-Aufwertungsvorlage liegen bei uns. Ab der zweiten Vorlage Häufchen (`isPileTemplate`). |
| Scherben | **Nicht platzierbar**: weder im Tag `simplebuilding:placeable_small` (67 Einträge) noch `SmithingTemplateItem`. |
| Schallplatten | Nur die 9 Mod-Platten im Tag (3x Voidline, 2x Driftwood, 2x Daybreak, 2x Brimstone); **Vanilla-Platten nicht** (nur `disc_fragment_5`). Liegen als flache **15x10-Ellipse** (das Item-Bild ist eine Schrägansicht, keine Draufsicht). B-Seite wendet der Hammer (`DiscFlips`). Die Item-Bilder der Platten werden **beim Build aus Vanilla abgeleitet** (`vanilla_derived.py`), liegen nicht im Repo. |
| Silhouetten (aus dem 26.3-Client-Jar gezählt) | **Alle 23 Scherben haben dieselbe Silhouette**; 18 Besatzvorlagen 3 Silhouetten (16 gleich, Coast und Wild je eine), die Netherite-Aufwertung eine vierte; 22 Platten 2 (alle gleich, nur Disc 11 hat einen Riss). Eine Geometrie je Art genügt, die Kinder unterscheiden sich nur in der Textur. |

Randbeobachtung (kein Teil dieses Auftrags): die Barren-Flächenbilder `placed_*_ingot.png` im Repo sind aus Vanilla-Farben
gemalt; ob das Regel 7 (nichts aus Vanilla einchecken) berührt, sollte der Besitzer einmal entscheiden.

## 3. Gemeinsame Technik (gilt für alle Tabellen unten)

- **Stufe A „dicker“**: nur `PlacedPlate.THICKNESS_SCALE` von 1,6 auf ca. 2,3 (= ca. 2 px bei 14/16). Gilt automatisch für jedes Item,
  auch fremder Mods, und für die Trefferform. Kanten bleiben `item/generated`-Extrusion (flaches Licht). Aufwand XS.
- **Stufe B „Silhouetten-Extrusion als Blockmodell“**: ein Generator (wie `placed_gems_bricks_2026_10_15.py`) liest die Deckungsmaske,
  fasst Zeilen gleicher Breite zu Quadern, Oberseite/Unterseite zeigen die **ganze Item-Textur** (`item/<id>` direkt), Seiten den
  Randpixel. Das Modell wird wie `item/generated` mit der Dicke in z gebaut, damit **Boden, Wand und Decke ohne Änderung der
  Transformation** weiter gehen. Eine gemeinsame Eltern-Geometrie je Art, je Item ein 3-Zeilen-Kind mit der Textur. Gewinn gegenüber A:
  Blockschattierung der Kanten (mutmaßlich; Schritt 2 belegt es im Bild), echte Stufenform, Dicke frei wählbar.
- **Stufe C „Relief“** (nur Vorlagen): zweite, 1 px dicke Lage nur über dem Motiv (heller Bereich), Gesamtdicke 3 px. Braucht je Vorlage Prüfung.
- Unverändert in allen Stufen: Daten im Block-Entity (nur Darstellung), Config (`placeVanillaItems`, `placeDisabledItems`),
  Bruch/Drop, Hammer-Wechsel, Trefferform-Logik (nur Dicke ändert sich), alle drei Loader (Renderer ist gemeinsamer Code, Modelle liegen in `mc26_3/overlay`).
  26.2/1.21.11 bleiben im Port-Run (dort `SMALL_PLACEABLES` aus).
- Neue Pixel: **keine**, außer der Draufsicht-Scheibe der Schallplatte (Abschnitt 4.3). Hammer-Umschalter auf „flach“ lässt sich auf alle neuen 3D-Arten
  ausdehnen (`Kind.switchable`), so bleibt der alte Look wählbar.

## 4. Vorschläge je Item-Art

### 4.1 Armor Trims / Schmiedevorlagen

| Aspekt | Heute | Vorschlag | Was genau sich ändert |
|---|---|---|---|
| Form | ausgestanzte Item-Textur | gleiche Silhouette als Quader-Treppe | Blockmodell statt `item/generated` (Stufe B); Textur bleibt die Item-Textur |
| Dicke | ca. 1,4 px (Häufchen 0,8 px) | **2 px** einzeln, im Häufchen ca. 1,2 px (Stufe B) bzw. 3 px mit Relief (Stufe C, optional) | Quader-Höhe im Modell; `THICKNESS_SCALE` entfällt für diese Modelle |
| Kanten | Randpixel, flach beleuchtet | Seiten bekommen Blockschattierung, Oberkante hell | Modell-Seiten; keine neuen Pixel |
| Ausrichtung | 4 Richtungen, Oberkante = Blickrichtung; Boden/Wand/Decke | **unverändert** | nichts (Transformation bleibt) |
| Licht | Blocklicht; Glowing/Emitting/Pulsating von Simple Trims | unverändert; Leuchten/Animation muss in Schritt 2 mitgeprüft werden | nichts am Licht, aber Test mit animierter `.mcmeta`-Textur |
| Häufchen | 8/16-Platten, Lift 0,2 px | Lift je Teil an Dicke anpassen (Tabelle je Platz statt konstant) | `LIFT`/`SLOTS` in `PlacedSmallParts` |
| Trefferform | Maske, Dicke 1,4 px | gleiche Maske, neue Dicke | nur Konstante |
| Wo | zwei Stellen: SimpleBuilding und Modul `simpletrims` (eigene Kopie) | beide nachziehen, **ohne** das Modul von SimpleBuilding abhängig zu machen | je ein Generator-Lauf, zwei Modellordner |

Aufwand: Stufe A XS; Stufe B M (Generator 4 Silhouetten, ca. 20 Kind-Modelle, Modul-Kopie); Stufe C zusätzlich M-L.
Risiko: **mittel** – Modulgrenze (Simple Trims läuft auch ohne uns), animierte Glow-Texturen, Hammer-Aufwertung (Treffer/Partikel an Form gekoppelt).
Empfehlung: B ohne C; C nur, wenn die Bilder in Schritt 2 zu flach wirken.

### 4.2 Pottery Sherds (Töpferscherben)

| Aspekt | Heute | Vorschlag | Was genau sich ändert |
|---|---|---|---|
| Platzierbar | **nein** | ja, alle 23 Vanilla-Scherben (+ Scherben anderer Mods) über `#minecraft:decorated_pot_sherds` | Tag-Eintrag + `isPlaceableSmall`; dieselben Config-Schalter wie andere Vanilla-Teile |
| Form | – | Scherben-Silhouette (eine für alle 23), Quader-Treppe | Stufe B, ein Eltern-Modell, 23 Kinder; Textur = Item-Textur |
| Dicke / Größe | – | **2 px**, 14 px Kantenlänge einzeln; im Häufchen halb groß, ca. 1 px | wie Items Displayed (2 px), ohne deren Bilder zu nutzen |
| Kanten | – | Randpixel an den Seiten, Blockschattierung | Modell |
| Ausrichtung | – | wie Vorlagen: Oberkante in Blickrichtung; Boden, Wand, Decke | vorhandene Transformation |
| Licht | – | Blocklicht | nichts |
| Hammer | – | kann auf flache Platte umschalten | `Kind.switchable` erweitern |

Aufwand: M (Tag, neue `Kind.SHERD`, Generator, Tests, Guide/Wiki/JEI-Text EN+DE). Risiko: **niedrig-mittel** (Spielregel-Änderung: neue platzierbare
Items; Häufchen-Überlappung bei 2 px Dicke; Wiki-Aussagen müssen im Code stimmen).

### 4.3 Music Discs (Schallplatten)

| Aspekt | Heute | Vorschlag | Was genau sich ändert |
|---|---|---|---|
| Platzierbar | nur 9 Mod-Platten (+ B-Seiten), keine Vanilla-Platten | Mod-Platten bleiben; **Vanilla-Platten (22) optional** über Tag (Frage 3) | Tag-Eintrag |
| Form | 15x10-**Ellipse** des Item-Bildes, flach | **runde Scheibe**, Ø 12 px, 7-9 Streifen von 1 px, Loch in der Mitte | Blockmodell (Stufe B mit eigener Rundmaske), 1 Eltern-Modell |
| Textur | Item-Bild (Schrägansicht) | **neue Draufsicht-Textur** `block/placed_disc_<platte>`: Platten-Farbton und Etikett aus dem Item-Bild der Platte (Rille, Etikett-Motiv, B-Seite = Etikett vertauscht) | Erzeugung beim Build in `vanilla_derived.py`/`music_disc_textures.py` (Vanilla-abgeleitet, **nicht eingecheckt**); Vorschau in Schritt 2; Item-Bilder bleiben unverändert |
| Dicke | ca. 1,4 px (Häufchen 0,8 px) | **1 px** | Modell |
| Kanten | Randpixel | dunkler Rand, unterer Rand etwas dunkler | Modell/Textur |
| Ausrichtung | 4 Richtungen, kleine Drehung im Häufchen | Etikett nach oben, beliebig gedreht (Rundform): Drehung aus `SLOTS` wirkt nur im Häufchen sichtbar | nichts am Code der Lage |
| Häufchen | 8/16 groß, Lift 0,2 px | Nur Platten auf dem Fleck: **echter Stapel** (je 1 px höher, je um ±8-15 Grad versetzt, wie `INGOT_STACK`); gemischt mit anderen Teilen: Platte allein am Platz, kleiner (Maßstab 0,67) | neue Stapel-Tabelle `DISC_STACK`, `Kind.DISC` |
| B-Seiten | Hammer wendet Track (`DiscFlips`) | unverändert; das Etikett der Draufsicht wechselt mit der Seite | Item-Definition je Seite |
| Trefferform | Ellipsen-Maske | Scheibe ca. 12 x 1 px je Platte | Quader-Maske statt `PlacedPlate` für `DISC` |

Alternative ohne neue Pixel (**nicht empfohlen**): die Ellipse 1 px dick extrudieren – liest sich als schief gedrückte Platte.
Aufwand: M-L (neue Textur-Ableitung, Stapel-Lage, Trefferform, Tests, Flip-Anzeige). Risiko: **mittel** (neue Pixelkunst → braucht Vorschau und
Abnahme; Vanilla-abgeleitet, daher Regel 7 beachten; `PlacedSmallPartsRenderer` bekommt eine weitere Art).

### 4.4 Weitere passende Arten

| Item-Art | Heute | Vorschlag | Aufwand | Risiko |
|---|---|---|---|---|
| Lapislazuli | flache Platte | wie Diamant/Smaragd stehender Stufen-Stein (`GEM_ROWS`-Eintrag, Generator-Lauf) | S | niedrig |
| Amethyst-, Echo-, Prismarin-Splitter, Quarz | flache Platte | Stufe B: Splitter 2 px dick liegend (Silhouette je Item); **kein** Handbau wie bei Items Displayed | M | niedrig |
| Nuggets (Eisen, Gold, Kupfer, Netherit, Enderit), Scheibenfragment 5 | flache Platte | Stufe A (dicker) reicht; oder Stufe B je Silhouette | XS / S | niedrig |
| Barren, Ziegel, Diamant, Smaragd, Ei | schon 3D | **unverändert** (Maße liegen bei 3 px bzw. bis 4 Stück, gleichauf mit Items Displayed) | – | – |
| Stäbe/Knochen/Pfeile/Feder | flache Platte | **nichts** vorschlagen: ohne neue Draufsicht-Textur sähe es schlechter aus als die Platte | – | – |
| Fremde Mod-Items | flache Platte | Stufe A (automatisch), kein eigenes Modell | XS | niedrig |

## 5. Gesamtübersicht und Reihenfolge

| Reihenfolge | Teil | Aufwand | Risiko |
|---|---|---|---|
| 1 | Stufe A (Dicke) als Grundlage und Rückfall für alles | XS | niedrig |
| 2 | Scherben (neu platzierbar, Stufe B) | M | niedrig-mittel |
| 3 | Schallplatten rund + Stapel + Draufsicht-Textur | M-L | mittel |
| 4 | Vorlagen Stufe B (SimpleBuilding + Modul `simpletrims`) | M | mittel |
| 5 | Weitere (Lapis, Splitter, Nuggets) | S-M | niedrig |
| optional | Stufe C Relief bei Vorlagen | M-L | mittel |

Gemeinsame Risiken: Häufchen-Überlappung (Lift je Plätze neu), Trefferform-Dicke ändert Bau-Optik bestehender Welten leicht (Daten bleiben gleich),
Hammer-Aufwertung der Vorlagen, Modulunabhängigkeit, Tests (neue GameTests für Lage und Form je Art wie `placing_game_test_ingots_*`),
Wiki/Guide/`docs/MODS-UND-FEATURES.md` nachziehen, sowie Prüfung im Spiel erst nach Freigabe von Schritt 2 (dann Bilder aus dem Modell-JSON offline gerendert oder aus dem Spiel).

## 6. Fragen an den Besitzer

1. Soll ich für Vorlagen die **Stufe B** (2 px, Blockschattierung) umsetzen, oder gefällt dir ein **erhabenes Motiv** (Stufe C, 3 px) wie bei Items Displayed besser?
2. Dürfen **Töpferscherben** neu platzierbar werden (alle 23 Vanilla-Scherben und die anderer Mods, über die bekannten Config-Schalter)?
3. Sollen auch die **22 Vanilla-Schallplatten** platzierbar werden, oder nur unsere 9 Mod-Platten?
4. Schallplatte: ist die **runde Draufsicht** mit neuer abgeleiteter Textur (nur beim Build erzeugt, nicht eingecheckt) in Ordnung, oder soll es bei der Ellipse des Item-Bildes bleiben?
5. Schallplatten im Häufchen: **echter Stapel** (nur Platten auf einem Fleck) wie bei Barren – oder immer nebeneinander wie heute?
6. Dicke einzeln/Häufchen: reichen **2 px / ca. 1 px**, oder sollen Teile im Häufchen gleich dick bleiben wie einzeln?
7. Soll der **Hammer-Wechsel auf „flach“** auch für Scherben, Schallplatten und Vorlagen gelten (alter Look bleibt wählbar)?
8. Weitere Arten aus 4.4 (Lapis, Splitter, Nuggets) gleich mitnehmen oder erst nach der ersten Welle?
9. Soll ein **frei drehbares Item-Display** (wie bei Items Displayed, Rüstungsständer-artig) ein eigenes Thema werden, oder ist es außerhalb des Auftrags?
10. Barren-Flächenbilder `placed_*_ingot.png` (aus Vanilla-Farben gemalt, eingecheckt): so lassen oder an Regel 7 angleichen?
