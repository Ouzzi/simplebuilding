# Konzept a-models11: Simple Models – Modell-Editor im Spiel (W2, 09.11.2026)

Status: **nur Konzept und Plan, kein Code.** Branch `a-models11` (Basis `claude-wave1` d7c6608de).
Besitzer-Wunsch W2 (`/root/sb-tools-a/wuensche-091109.md`): Beispiel-3D-Modelle einpflegen; Editor wie Blockbench
(Maus + Tastatur), eigene, von Blockbench inspirierte UI, saubere 3D-Bearbeitung der Blockmodelle; zusätzlich ein
Code-Editor wie beim Bauplan; fremde Repos nutzen, wenn hilfreich (Lizenz beachten).
Vorschau: `/root/sb-tools-b/previews/a-models11-preview.html`, Antwortcode **`MODELS11: layout=A|B|C + Anmerkung`**.
Queue: IG31-60.

## 1. Ist-Zustand (erhoben 09.11.)

**Simple Models** (`modules/simplemodels`, 18 Java-Dateien, Sub-Mod von Simple Aesthetics, allein spielbar mit
eingebetteter SimpleLib):
- Kein Editor, keine Geometrie. Das Modul ist ein **Katalog + Amboss-Zuweisung** (CIT-Ersatz): `ModelDefinition`
  (`id`, `base_item`, `match_name`, `model`, `tags`, `author`, streng validiert), `ModelCatalogue.load` liest
  `config/simplemodels/catalog/*.json` (begrenzter Ordner-Scan, kein Upload, kein Netz), `Models.assign` setzt im Amboss
  serverseitig `DataComponents.ITEM_MODEL` (Eingabe `@model:<id>`, `@model:reset`), `CataloguePayload` schickt nur den
  Katalog (JSON-Text, Größengrenze `MAX_SNAPSHOT_CHARS`) an den Client.
- Die Modelle selbst kommen aus einem Resourcepack des Spielers/Servers (`examples/resourcepack/assets/renamed/...`);
  Beispiel bisher nur `tomato` (Apfel → Tomate).
- Client-UI (SM21, `docs/ai/PLAN-B-MODELS21-2026-10-22.md`): `ModelBrowser` (Reiter Modelle/Servereinstellungen/
  Importhilfe, Suche, Filter/Sortierung `ModelQuery`, Mausrad), `ModelPreviewTooltip` (Modell 2× im Tooltip),
  Lesezeichen `ModelsTab` an Inventar/Amboss/Kreativ (`ModelScreenMixin`).
- Audit: `modules/simplemodels/AUDIT.md`, `docs/ai/AUDIT-SIMPLEMODELS-2026-10-10.md`.

**Bauplan-Editor** (`modules/simplebuildingtools/.../blueprint`, Vorbild für den Code-Editor):
- `BlueprintCode` (1386 Z.): Parser + Serialisierer + Syntax-Einfärbung „aus einer Hand“, liefert
  `ParseResult(model, problems, styles, …)` mit `Problem(line, start, end, key, args)` (übersetzbare Fehlertexte).
- `BlueprintScreen` (975 Z.): Code links, 3D-Vorschau rechts, Live-Neuparsen; `BlueprintView` (eigenes Mesh,
  Drehen/Zoomen/Verschieben) mit `BlueprintViewControls` (Konstanten Drag/Zoom/Pitch, testbar ohne Client);
  `BlueprintEditorLayout` (Geometrie als reine Rechnung, JUnit-getestet); `BlueprintIoScreen` (Import/Export-Modal,
  alles-oder-nichts-Prüfung).
- **SimpleLib `UiCodeArea`** (KONZ20 N1 dorthin verschoben): mehrzeiliges Codefeld mit Einfärbung, Zeilennummern,
  unterstrichenen Problemen, eigener Scrollleiste, Text-Logik von Vanilla `MultilineTextField`. Wird schon von Bauplan
  und Simple Music genutzt → **direkt wiederverwendbar**.
- Weitere SimpleLib-Bausteine: `UiBoxes` (Rahmen, Ecken, Pfeile), `UiPalette` (`DARK_LABEL`/`LIGHT_LABEL`),
  `UiContrast`, `UiContextMenu`, `UiKeyboard`, `UiSymbols`, `UiShiftTooltip`, `UiBookmarks`.
- **Modul-Unabhängigkeit:** Simple Models darf nicht von Simple Building Tools abhängen. `BlueprintViewControls` und die
  Orbit-Kamera-Logik werden deshalb (wie `UiCodeArea`) nach SimpleLib verschoben (`UiOrbitCamera`), Bauplan nutzt sie
  danach von dort.

**Vanilla 26.3 Modellformat** (geprüft in `/root/vsrc263/net/minecraft/client/resources/model/cuboid/`):
- `CuboidModelElement`: `from`/`to` je Achse **-16..32**, `faces`, `shade`, `light_emission`,
  `shade_direction_override`, `rotation` mit `origin` und **entweder** `axis` + `angle` (beliebiger Winkel,
  `SingleAxisRotation`) **oder** `x`/`y`/`z` (Euler XYZ, `EulerXYZRotation`, mehrachsig), dazu `rescale`.
- `CuboidFace`: `uv` (0..16), `texture` (`#name`), `cullface`, `rotation` (0/90/180/270), `tintindex`.
- Modell: `parent`, `textures`, `elements`, `display` (`ItemTransforms`), `ambientocclusion`, `gui_light`.
- Folge: Der Editor braucht **kein eigenes Laufzeitformat** – Vanilla-JSON ist das Speicherformat; mehrachsige
  Drehung ist seit 26.x Vanilla (früher nur ±45° in 22,5°-Schritten; bei Ports auf 1.21.1 prüfen, Vanilla-Rückport-Regel).

**Prinzipien:** A7 Kontrast ≥ 4,5:1 (Test `ui_text_contrast`), Tooltips kurz; A8 nie Vanilla-Pixel einchecken
(Beispiele referenzieren nur `minecraft:block/...`-Texturen, keine Kopien); UI-Lesbarkeit: Mehrfach-Rand mit stufigen
Ecken, Vanilla-nah; UI erst exemplarisch (diese Vorschau); UI-Tests nur mit echter Maus (`menu_probe.py`, XTest);
A12 siehe Abschnitt 9.

## 2. Ziele

1. **Beispielmodelle**: 6 sofort nutzbare Modelle im Katalog (Amboss-Zuweisung), nur mit Vanilla-Texturverweisen.
2. **Modell-Editor im Spiel** (Client-Screen), Blockbench-inspiriert, aber eigene Vanilla-nahe Optik:
   Würfel anlegen, verschieben, drehen, skalieren per Gizmo **und** per Zahlenfeld; Flächen-UV und Textur je Fläche;
   Undo/Redo; Raster/Snap; Tastenkürzel; Vorschau als Block, in der Hand, im GUI.
3. **Code-Editor** daneben, wie beim Bauplan: kleine Modell-DSL **und** Vanilla-JSON, beides live in beide Richtungen,
   Fehler mit Zeile/Spalte unterstrichen.
4. **Speichern/Laden/Export**: Arbeitsstand lokal; Export als Resourcepack-Ordner + Katalog-Eintrag (direkt im Amboss
   nutzbar); Import von Vanilla-JSON und (Stufe 7) Blockbench-`.bbmodel` (Java-Block-Modell-Projekte).
5. **Nicht-Ziele (vorerst):** Animationen, Entity-/Bone-Modelle (GeckoLib-Gebiet), Textur-Malen (dafür gibt es den
   Wiki-Pixeleditor), neue Blöcke registrieren. Der Editor gestaltet **Item-Modelle bzw. Blockmodell-JSON**, die über
   Simple Models auf Items gelegt werden.

## 3. UI-Entwurf (gemeinsam für alle Layouts)

Bausteine; die Vorschau zeigt drei Anordnungen (A Blockbench-klassisch, B Vanilla-Reiter, C Bauplan-Split).

- **Viewport (3D):** eigener Renderpfad wie `BlueprintView` (Mesh aus den Elementen, gebacken mit Vanilla
  `FaceBakery`-Logik bzw. eigenem Quad-Bau), dunkler Hintergrund mit 16×16-Bodenraster, Block-Umriss 0..16 als
  Referenz, Bereich -16..32 schwach gestrichelt. Orbit = rechte Maustaste ziehen, Verschieben = mittlere Maustaste
  bzw. Shift + rechte, Zoom = Mausrad. Ansichtswürfel oben rechts (Klick auf Seite = Ansicht von vorn/oben/…),
  Taste `Num 1/3/7` = vorn/rechts/oben, `Num 5` = orthografisch/perspektivisch.
- **Auswahl:** Linksklick wählt den Würfel unter der Maus (Raycast Kamera-Strahl gegen gedrehte AABB, JOML
  `Intersectionf.intersectRayAab` im lokalen Raum des Elements), Shift = Mehrfachauswahl, Strg + Klick = Fläche wählen
  (für UV). Umriss gewählter Würfel hell, Flächen-Hover zart eingefärbt.
- **Gizmo** (Taste `G` Verschieben, `R` Drehen, `S` Skalieren, wie Blockbench/Blender):
  - Verschieben: drei Pfeile rot/grün/blau (X/Y/Z), Ziehen projiziert die Mausbewegung auf die Achse; Ebenen-Quadrate
    für zwei Achsen.
  - Drehen: drei Ringe; Drehpunkt (`origin`) als kleiner Diamant, mit `Alt` ziehen = Drehpunkt verschieben.
  - Skalieren: Würfelgriffe an den Achsenenden; `from`/`to` werden an der gezogenen Seite geändert (wie Blockbench
    „Resize“), Strg = symmetrisch um die Mitte.
  - Während des Ziehens Zahl am Cursor (z. B. `+2,5 px`), `Esc`/Rechtsklick bricht ab (zurück zum Ausgangswert).
- **Würfel-Liste (Outliner):** Zeilen mit Name, Augen-Symbol (ausblenden), Schloss (sperren); Ziehen ordnet um;
  Gruppen (nur im Editor, beim Export flach). Doppelklick = umbenennen. Kontextmenü `UiContextMenu`: Duplizieren,
  Spiegeln X/Z, Löschen, Gruppieren.
- **Eigenschaften-Panel:** `from`, `to` (bzw. Position + Größe), `origin`, Drehung (Modus „eine Achse“ oder „X/Y/Z“),
  `rescale`, `shade`, `light_emission`; Zahlenfelder mit Mausrad ±1 (Shift ±0,25, Strg ±4).
- **UV-Editor:** 2D-Feld 16×16 (bzw. Texturgröße) mit der Textur der gewählten Fläche; UV-Rechteck ziehen/skalieren,
  Drehung 0/90/180/270, „Auto-UV“ (aus der Würfelgröße, Standard), Spiegeln. Flächen-Leiste N/O/S/W/oben/unten mit
  Häkchen (Fläche an/aus), `cullface`-Auswahl, `tintindex`.
- **Texturwahl:** Textur-Variablen des Modells (`#side`, `#top` …) als Liste; Zuweisung per Suchfeld über den
  Block-Atlas (alle geladenen Texturen, Vanilla und Mods, mit Icon), keine Bilddateien hochladen. Eigene Texturen nur
  über einen Resourcepack (Hinweis in der Importhilfe).
- **Undo/Redo:** Befehls-Stapel (`Strg+Z`, `Strg+Y`/`Strg+Shift+Z`), je Aktion ein Eintrag (ein ganzer Gizmo-Zug = ein
  Eintrag), Grenze 200 Schritte; Code-Änderungen im Code-Editor zählen als ein Eintrag je Übernahme.
- **Raster/Snap:** Schritt wählbar 1 / 0,5 / 0,25 px (Standard 1), Drehen in 22,5° (Shift = 1°, aus = frei);
  Snap-Schalter in der Werkzeugleiste, `Strg` beim Ziehen hebt Snap kurz auf.
- **Vorschau-Modi:** Block / Hand (rechte Hand, `display.thirdperson_righthand`) / GUI / Boden; `display`-Werte als
  eigener Reiter mit denselben Gizmos.
- **Tastenkürzel** (Übersicht über `F1` bzw. `?`-Knopf): `N` neuer Würfel, `Strg+D` duplizieren, `Entf` löschen,
  `G/R/S` Gizmo, `X/Y/Z` beim Ziehen Achse festlegen, `H` ausblenden, `Strg+S` speichern, `Strg+Z/Y`, `Tab`
  zwischen Viewport und Code, `F` auf Auswahl zentrieren, `Num 1/3/7/5` Ansichten. Kürzel nur bei Fokus im Viewport
  (im Code-Feld gelten die Texteingabe-Tasten), alle in `UiKeyboard` registriert, Texte EN+DE.
- **Optik:** Vanilla-Inventar-Grau (`#C6C6C6`, heller Innenrand, `#555555` Außenrand, stufige Ecken aus `UiBoxes`),
  Viewport dunkel; Text `#404040` auf `#C6C6C6` (≈ 6,6:1), heller Text `#F2EEE8` auf Viewport `#1E1F23` (≈ 15:1);
  Achsenfarben gedeckt, nicht grell (Rot `#D04848`, Grün `#5DAA3C`, Blau `#4A78D0`, je mit dunklem 1-px-Rand).
  Blockbench dient nur als Ideen-Quelle für Abläufe, nicht als Optik.
- **Zugang:** neuer Reiter „Editor“ im `ModelBrowser` + Knopf „Im Editor öffnen“ an jedem Katalogeintrag; Client-Befehl
  `/simplemodels editor [id]`. Kein Item nötig (reines Werkzeug, Client-only).

## 4. Code-Editor (wie Bauplan)

- Widget: SimpleLib `UiCodeArea` (Einfärbung, Zeilennummern, Problem-Unterstreichung, Scrollleiste) – kein Neubau.
- **Zwei Ansichten** im selben Feld, umschaltbar: **DSL** (Standard, kurz) und **JSON** (Vanilla, genau).
- Modell-DSL `ModelCode` (Parser + Serialisierer + Einfärbung aus einer Hand, wie `BlueprintCode`, ohne Client-Importe,
  JUnit-/Gametest-fähig):

```
# Hocker
textures side=minecraft:block/oak_planks top=minecraft:block/stripped_oak_log
cube sitz   2,10,2  14,12,14  tex #top
cube bein   3,0,3   5,10,5    tex #side
mirror x bein                       # erzeugt bein_x bei 11..13
cube schräg 7,12,7 9,16,9 rot y=22.5 @8,12,8
face sitz up uv 0,0,16,16 rot 90
display gui rot 30,225,0 scale 0.625
```

  - Zeilenweise Anweisungen: `textures`, `cube <name> <from> <to> [tex #v] [rot axis=angle | rot x,y,z] [@origin]`,
    `face <cube> <dir> [uv u1,v1,u2,v2] [rot n] [cull dir] [tint n] [off]`, `mirror`, `repeat`, `$var = ...` (wie
    Bauplan), `display <slot> rot/pos/scale`.
  - Grenzen wie Vanilla (-16..32, uv 0..16) + Editor-Grenzen (max. 256 Würfel, 32 000 Zeichen).
- **Live:** Neuparsen bei jeder Änderung (entprellt 150 ms); gültiger Stand erscheint sofort im Viewport; bei Fehlern
  bleibt der letzte gültige Stand sichtbar, Fehlerzeilen rot unterstrichen, Fehlerliste unter dem Feld mit
  übersetztem Text (`Problem(line, start, end, key, args)` wie Bauplan), Klick springt in die Zeile.
- **Rundweg:** Viewport-Änderung → Code wird neu geschrieben (Kommentare bleiben, solange die Zeile zuordenbar ist;
  sonst Hinweis „Code neu formatiert“). JSON-Ansicht ist immer 1:1 der Export.
- Import/Export-Modal wie `BlueprintIoScreen`: Einfügen aus Zwischenablage (Vanilla-JSON, DSL, ab Stufe 7 `.bbmodel`),
  Kopieren, alles-oder-nichts-Prüfung.

## 5. Speichern, Laden, Export, Multiplayer

- **Arbeitsstand:** `config/simplemodels/workspace/<name>.json` = Vanilla-Modell-JSON + Editor-Block `"simplemodels"`
  (Gruppen, Namen, Sperren; Vanilla ignoriert unbekannte Felder nicht überall → Editor-Daten in eigener Datei
  `<name>.editor.json` daneben). Autospeichern alle 60 s + beim Schließen; Laden über Liste im Editor-Reiter.
- **Export „Als Modell nutzen“** (Einzelspieler / eigener Client):
  1. schreibt `resourcepacks/simplemodels-editor/assets/<ns>/models/item/<name>.json` + `items/<name>.json`
     (Item-Modell-Definition) + `pack.mcmeta`,
  2. legt den Katalog-Eintrag `config/simplemodels/catalog/<name>.json` an (Basis-Item wählbar),
  3. schaltet das Pack an und lädt Ressourcen neu (Vanilla-Reload; Dauer = Hinweis im Knopf-Tooltip).
  Im Einzelspieler ist der Spieler Server-Operator; danach im Amboss mit `@model:<ns>:<name>` nutzbar.
- **Export „Resourcepack (zip)“**: dieselben Dateien als Zip in `simplemodels/exports/` zum Weitergeben/Server-Pack.
- **Multiplayer (Stufe 8, optional):** Heute sieht jeder Spieler nur Modelle aus seinem eigenen/Server-Resourcepack.
  Vorschlag „Modell teilen“: Spieler mit Recht (`ModelPolicy`: `allowSharedModels`, `operatorsOnly`, Standard **aus**)
  schickt die Geometrie an den Server; Server prüft (gleicher Parser, Grenzen: 256 Würfel, 64 KB, nur Texturverweise auf
  **vorhandene** Atlas-Texturen, keine Bilder), speichert in `config/simplemodels/shared/` und verteilt sie mit dem
  Katalog (`CataloguePayload`, Größengrenze). Clients bauen daraus ein Laufzeit-Modell (ohne Pack-Reload: eigener
  Item-Modell-Pfad, der die Geometrie aus dem Sync rendert). Ohne Recht bleibt der Editor rein lokal.
- **Server-Rechte:** Server bleibt Autorität (A9): Zuweisung weiterhin nur im Amboss, nur serverseitig, nur Katalog-IDs;
  der Editor selbst ändert nichts auf dem Server.

## 6. Beispielmodelle (Vorschläge, 6 davon in Stufe 0)

Alle nur aus Würfeln mit `minecraft:block/...`-Texturverweisen (keine kopierten Pixel), Katalog-Eintrag + Item-Modell,
Basis-Item so gewählt, dass es thematisch passt:

| # | Modell | Basis-Item | Zeigt im Editor |
|---|---|---|---|
| 1 | **Hocker** (Eichenholz) | `oak_slab` | gespiegelte Beine, Grundformen |
| 2 | **Tischlaterne** (Kette, Glas) | `lantern` | `light_emission`, kleine Würfel |
| 3 | **Briefkasten** | `barrel` | Fläche/UV je Seite |
| 4 | **Vogelhaus** (Dach 45°) | `oak_fence` | Einachs-Drehung, Drehpunkt |
| 5 | **Wetterfahne** | `lightning_rod` | Euler X/Y/Z (neu in 26.x) |
| 6 | **Kristallgruppe** (Amethyst) | `amethyst_cluster` | mehrachsige Drehung, `rescale` |
| 7 | Teekanne (Terrakotta) | `decorated_pot` | viele kleine Würfel (Leistungs-Probe) |
| 8 | Mini-Amboss (Briefbeschwerer) | `iron_nugget` | `display`-Werte für Hand/GUI |

Liegen unter `modules/simplemodels/examples/` (Katalog + Pack) und werden zusätzlich als **eingebautes Pack** des Moduls
mitgeliefert (Standard an, abschaltbar), damit sie ohne manuelle Installation funktionieren. Tomate bleibt.

## 7. Stufenplan

| Stufe | Inhalt | Aufwand |
|---|---|---|
| 0 | Beispielmodelle 1–6 (JSON + Katalog + eingebautes Pack), Gametest „Katalog lädt, Amboss weist zu“, Wiki/Handbuch | **S** |
| 1 | Datenmodell `EditableModel` (Elemente, Flächen, Texturen, Display) + Vanilla-JSON lesen/schreiben + Validierung (Grenzen wie `CuboidModelElement`), Undo-Stapel als reine Logik, JUnit | **M** |
| 2 | SimpleLib: `UiOrbitCamera` (aus `BlueprintViewControls` verschoben) + Mesh-Viewport (nur Ansicht), Würfel-Liste, Auswahl per Raycast, Editor-Reiter im `ModelBrowser`, Layout als Rechnung (`ModelEditorLayout`, JUnit) | **M** |
| 3 | Gizmos Verschieben/Drehen/Skalieren, Drehpunkt, Snap/Raster, Eigenschaften-Panel, Undo/Redo, Tastenkürzel, Client-Test mit echter Maus | **L** |
| 4 | Code-Editor: `ModelCode` DSL (Parser/Serialisierer/Einfärbung) + JSON-Ansicht in `UiCodeArea`, Live-Rundweg, Fehlerliste, Import/Export-Modal | **M** |
| 5 | Flächen: UV-Editor, Flächen an/aus, cullface/tint, Texturwahl aus Atlas, Display-Reiter (Hand/GUI/Boden) | **L** |
| 6 | Speichern/Laden/Autospeichern, Export „Als Modell nutzen“ (Pack + Katalog + Reload), Zip-Export, Beispiele 7–8 | **M** |
| 7 | Import Blockbench `.bbmodel` (nur Java-Block-Modelle: `elements`, `faces`, `uv`, `rotation`, `origin`), eigener Code nach Formatbeschreibung | **S–M** |
| 8 | Multiplayer „Modell teilen“ (Server-Prüfung, Sync, Laufzeit-Modell ohne Reload), Standard aus | **L** |

Reihenfolge 0 → 1 → 2 → 4 → 3 → 5 → 6, weil der Code-Editor (Stufe 4) schon ohne Gizmos echte Arbeit erlaubt und den
Viewport früh nutzbar macht. Nach Stufe 2 Abnahme-Vorschau mit echtem Screenshot (UI exemplarisch).
Jede Stufe: eigener Branch, nur 26.3 Fabric (Regel „Nur 26.3 Fabric“), gezielte Tests (`module-simplemodels`
fabric + standalone), Client-Tests über `menu_probe.py` (echte Maus).

## 8. Risiken

- **Rendering:** Eigener Mesh-Pfad muss Vanilla-Bake genau nachbilden (Euler-Drehung, `rescale`, UV-Drehung, Shade),
  sonst weicht Editor-Bild vom Spiel ab. Gegenmittel: Vorschau-Modus „echt“ backt über Vanilla (`FaceBakery`) und
  vergleicht im Client-Test Pixel mit dem Editor-Mesh.
- **Performance:** Neuaufbau des Meshes bei jedem Zug; bei 256 Würfeln × 6 Flächen trivial, aber Neuparsen der DSL je
  Tastendruck entprellen; Raycast nur bei Mausbewegung.
- **Client-only:** Editor ist reiner Client-Code; Server ohne Client-Klassen darf nicht brechen (Klassen nur in
  `client/`, Mixins nur Client-Seite). Dedizierter Server: Gametest „Server startet ohne Editor-Klassen“.
- **Ressourcen-Reload:** Export ins Pack braucht Reload (Sekunden, große Modpacks länger) → nur auf Knopfdruck, nie
  automatisch; Laufzeit-Modelle (Stufe 8) vermeiden das, sind aber der heikelste Teil (eigener Item-Modell-Typ).
- **Server-Rechte/Missbrauch:** Geteilte Modelle sind Nutzerinhalt → Grenzen, nur Atlas-Verweise, Standard aus,
  Operator-Freigabe; keine Bild-Uploads (keine Moderationsfrage für Bilder).
- **Eingabe-Konflikte:** Viewport-Kürzel vs. Code-Feld vs. Vanilla-Tasten (`E` schließt Inventar) → eigener Screen ohne
  Container, Fokus-Regel klar, Client-Test mit echter Maus/Tastatur (Fabric-Testmaus umgeht den Eingabeweg).
- **Kleiner Bildschirm/GUI-Skala 4:** drei Spalten passen nicht → Layout klappt Seitenleisten ein (B/C robuster als A).
- **Modul-Unabhängigkeit:** Kamera-/Viewport-Logik nach SimpleLib verschieben berührt den Bauplan → im selben Branch
  Bauplan-Tests (`blueprint*`) mitlaufen lassen.
- **Ports:** mehrachsige Drehung gibt es erst ab 26.x; ein späterer 1.21.1-Port muss die Drehung auf eine Achse ±45°
  begrenzen bzw. ablehnen.

## 9. A12 Vanilla-Nutzen und Eingriff

1. Nutzen: Spieler/Server-Bauer gestalten Deko-Modelle für Items (Namensschild-artige Optik-Varianten), ohne externes
   Programm; Server können eigene Optik anbieten.
2. Änderung am Spiel: rein optisch; Zuweisung wie bisher nur im Amboss mit Katalog-ID. Keine neuen Items, keine Werte.
3. Eingriff: **keiner** in das Vanilla-Spielgeschehen. Editor ist Client-Werkzeug; Teilen ist Standard aus.

## 10. Lizenzbewertung (Quellen, Stand 09.11.2026)

| Quelle | Lizenz | Nutzung |
|---|---|---|
| Blockbench – https://github.com/JannisX11/blockbench | **GPL-3.0** | **Kein Code, kein Klonen, keine Assets/Icons.** Nur Ideen für Abläufe (Outliner, G/R/S-Gizmo, UV-Feld). Das `.bbmodel`-Format darf nach öffentlicher Formatbeschreibung **selbst** eingelesen werden (Datenformat ist kein geschützter Code); Importer wird eigenständig geschrieben. |
| JOML – https://github.com/JOML-CI/JOML | MIT | Ist bereits Teil von Minecraft: `Matrix4f.unproject`, `Intersectionf.intersectRayAab`, Quaternionen für Gizmos. **Keine neue Abhängigkeit.** |
| imgui-java – https://github.com/SpaiR/imgui-java | MIT | Technisch möglich (Docking, Gizmo-Erweiterung ImGuizmo), **verworfen**: Native-Bibliotheken je Plattform, fremde Optik (nicht Vanilla-nah), schwer mit GUI-Skala. |
| GeckoLib – https://github.com/bernie-g/geckolib | MIT (Repo-LICENSE; ein Aggregator nennt LGPL-3.0 → vor Nutzung LICENSE prüfen) | Nicht nötig (Entity-/Bone-Animation, nicht Blockmodell). Nur als Beispiel, wie Blockbench-Exporte gelesen werden. |
| Figura – https://github.com/FiguraMC/Figura | nicht verifiziert (LICENSE.md nicht abrufbar) | Nur als Idee (Avatare aus `.bbmodel`, Sync im Multiplayer). Kein Code. |
| Axiom (Mod, proprietär) | proprietär | Nur Idee: Bau-Werkzeuge mit Gizmos direkt im Spiel. |
| MCreator | GPL-3.0 | nicht nutzen. |
| Minecraft-Modellformat | Vanilla-Quelltext `/root/vsrc263/.../model/cuboid/` | Maßgeblich für Felder und Grenzen; keine Vanilla-Pixel einchecken (A8). |

Ergebnis: **keine fremde Bibliothek nötig.** JOML (schon da) + SimpleLib (`UiCodeArea`, `UiBoxes`, `UiContextMenu`,
`UiKeyboard`) + eigener Code reichen. Keine GPL-Repos klonen.

## 11. Verifikation (je Stufe)

- JUnit: `EditableModel` Rundweg JSON → Modell → JSON (alle Vanilla-Felder, Euler/Einachs), `ModelCode` Parser
  (gültig/ungültig mit Zeile), Undo-Stapel, Layout-Rechnung, Raycast-Mathe.
- Gametest: Beispielkatalog lädt, Amboss-Zuweisung je Beispiel, Server ohne Client-Klassen.
- Client-Test (echte Maus, `menu_probe.py`): Würfel anlegen, verschieben mit Snap, Undo, Export, Zuweisung im Amboss.
- `ui_text_contrast` grün, Kontrastwerte im Bericht.
- Plan-Abgleich je Stufe; nicht Getestetes ehrlich im Bericht.

## 12. Offen beim Besitzer

- `MODELS11: layout=A|B|C + Anmerkung` (Vorschau).
- Beispiele: welche 6 von 8 (Standard: 1–6).
- Stufe 8 (Multiplayer-Teilen) überhaupt gewünscht?
