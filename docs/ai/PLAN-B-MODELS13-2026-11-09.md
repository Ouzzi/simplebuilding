# Plan b-models13: Simple Models Editor, Stufe 2 + 3 (IG31-60, 09.11.2026)

Basis `b-models12` (96c91deeb, Stufe 0+1). Konzept `docs/ai/KONZEPT-A-MODELS11-2026-11-09.md` §3/§4/§7/§11.
Besitzer-Wahl MODELS11 = A (Blockbench-klassisch) + Umschaltknopf „Nur Code“. Anordnung des Nur-Code-Modus: keine
Antwort auf MODELS12 vorhanden (`grep MODELS12` in Auftrag/Queue: nur Frage) → **Standard A (Code | Vorschau gleich
breit)**. Nur 26.3 Fabric, kein Gate. Vorschau `/root/sb-tools-b/previews/b-models13-preview.html`, Antwortcode
**`MODELS13: ok | nachbessern=<punkt>`**.

## Ist-Stand (erhoben 09.11.)
- Editor-Daten fertig: `EditableModel`, `ModelJson` (lesen alles-oder-nichts, schreiben), `EditHistory` (200).
- Kein Screen, keine Geometrie, keine Kamera im Modul. Orbit-Logik liegt dreifach vor: Kern
  `common/.../blueprint/BlueprintViewControls` (+ 1.21.11-Kopie der Ansicht), Modul `simplebuildingtools/.../blueprint/
  BlueprintViewControls` (genutzt von `BlueprintScreen`, `BlueprintView`). Zeichnen: `BlueprintView` reicht ein selbst
  projiziertes Netz als `GuiElementRenderState` ein (Accessor-Mixin auf `GuiGraphicsExtractor.guiRenderState`).
- SimpleLib ist in simplemodels und simplebuildingtools auf allen gebauten Loadern im Klassenpfad (Regel 11 erfüllt;
  simplebuildingtools baut nur Fabric).
- Code-Feld: SimpleLib `UiCodeArea` (Bauplan/Simple Music).
- Vanilla-Geometrie (`FaceBakery`, `FaceInfo`, `CuboidRotation`, `Quadrant`): Eckreihenfolge je Seite, Auto-UV,
  UV-Drehung = Index-Verschiebung, Einachs-Drehung `rotation(angle, axis)`, Euler `rotationZYX(z, y, x)`, `rescale`
  = 1/max(|Achsbild|) je Achse.

## Stufe 2 (eigener Commit): Kamera nach SimpleLib, Viewport, Würfel-Liste, Auswahl, Layout
1. **SimpleLib `com.simplelib.api.client.ui.UiOrbitCamera`** (nur JOML, ohne Client-Importe → Gametest-fähig): die
   Drehteller-Logik aus `BlueprintViewControls` (gleiche Konstanten/Verhalten) + neu: feste Ansichten
   (vorn/rechts/oben), `scale(w, h, radius)`, `project(...)` Modell → Bildschirm, `ray(...)` Bildschirm → Strahl im
   Modellraum (orthografisch, Blick nach −z), `screenAxis(...)` (2D-Richtung einer Modellachse für das Gizmo).
   Bauplan-Modul nutzt sie danach direkt; `simplebuildingtools/.../BlueprintViewControls` entfällt.
   **Kern bleibt unverändert** (Regel 10: Kern darf SimpleLib nicht importieren; seine Kopie + JUnit bleiben) → kein
   Overlay, kein 26.2-Zwilling nötig.
   Gametest SimpleLib `orbit_camera` (Drehen/Klemmen/Zoomgrenzen/Reset/Ansichten, project↔ray rund).
2. **simplemodels `editor.ModelGeometry`** (JOML, ohne Client): Element-Ecken mit Drehung (Vanilla-Mathe), Flächen-
   Vierecke in Vanilla-Eckreihenfolge mit UV (Auto-UV, explizite UV, UV-Drehung), `pick` (Strahl → nächster sichtbarer
   Würfel, Strahl in den lokalen Raum des Würfels, Slab-Test), `bounds` (Mittelpunkt/Radius für die Kamera).
3. **`editor.ModelEditorLayout`** (reine Rechnung): Modus Klassisch = Werkzeugleiste oben, Würfel-Liste links,
   Viewport Mitte, Eigenschaften rechts, Statuszeile; Modus Nur Code = Werkzeugleiste, Code | Vorschau gleich breit
   (Variante A), Fehlerzeile unter dem Code. Kleine Fenster: Seitenleisten schmaler, Mindestbreite Viewport.
4. **Client `ModelEditorScreen` + `ModelViewport`**: Netz aus `EditableModel` (Texturen über `#var`-Kette auf den
   Block-Atlas, sonst Missing-Sprite), Malerverfahren wie `BlueprintView`, Vanilla-Schattierung; Bodenraster 16×16,
   Block-Umriss, Auswahl-Umriss. Linien als dünne Vierecke in eigenem `GuiElementRenderState` (Pipeline GUI). Eigener
   Accessor-Mixin `ModelGuiStateAccessor` (Client). Würfel-Liste mit Augen-Schalter (ausblenden), Klick = Auswahl,
   Linksklick im Viewport = Auswahl per Raycast. Zugang: Knopf „Editor“ im `ModelBrowser` (öffnet das gewählte Modell,
   sonst Beispiel Hocker); Laden aus dem Resource-Manager (`<ns>:models/<pfad>.json`).

## Stufe 3 (eigener Commit): Gizmo, Snap, Eigenschaften, Undo/Redo, Kürzel, Nur-Code-Modus
1. **`editor.Gizmo`** (rein): Modus Verschieben/Drehen/Skalieren, Mausweg → Betrag entlang der projizierten Achse,
   Snap (1 / 0,5 / 0,25 px; Drehen 22,5°, Shift = 1°), Klemmen auf −16..32; Drehen: ohne Drehung → Einachs um die
   Würfelmitte, gleiche Achse → Winkel addieren, sonst Euler; Skalieren: Seite an der gezogenen Achse (+ = `to`,
   − = `from`), Strg = symmetrisch, Größe ≥ 0.
2. **`editor.ModelEditorState`** (rein): Modell + `EditHistory`, Auswahl, ausgeblendete Würfel, Gizmo-Zug (Beginn/
   Zug/Ende = ein Undo-Eintrag, Esc/Rechtsklick = Abbruch), Neuer Würfel, Duplizieren, Löschen, Zahlenfeld setzen,
   Code übernehmen (gültiges JSON = ein Eintrag; ungültig → Probleme, letzter gültiger Stand bleibt).
3. Screen: Gizmo-Griffe (Pfeile/Ringe-Punkte/Würfelgriffe, Achsfarben gedeckt mit dunklem Rand), Zahl am Cursor beim
   Ziehen, Eigenschaften-Panel (Von/Bis/Drehpunkt/Winkel als Zahlenfelder, Mausrad ±1, Shift ±0,25, Strg ±4),
   Werkzeugleiste (G/R/S, Snap, Undo/Redo, Nur Code). Kürzel nur bei Fokus im Viewport: G/R/S, N, Strg+D, Entf, H,
   Strg+Z/Y/Strg+Shift+Z, F4 Modus, Num1/3/7 Ansichten, Num0/Pos1 Reset. Rechte Maustaste ziehen = drehen, mittlere/
   Shift+rechte = verschieben, Rad = Zoom (Konzept §3).
4. **Nur-Code-Modus**: `UiCodeArea` mit **Vanilla-JSON** (DSL folgt laut Konzept in Stufe 4), Live-Neuparsen
   (entprellt ~150 ms), Probleme mit Pfad + übersetztem Text unter dem Feld, Vorschau zeigt den letzten gültigen Stand.
   Rundweg: Gizmo/Liste ändern → JSON neu geschrieben.

## Tests / Verifikation
- Gametests simplemodels (Katalog `ModelTests.ALL` + Fabric-Adapter): `editor_layout`, `editor_geometry` (Ecken,
  Euler/Einachs/rescale gegen Handrechnung, Auto-UV, Pick), `editor_gizmo` (Snap/Klemmen/Drehwechsel/Skalieren),
  `editor_state` (Zug = ein Undo, Abbruch, Duplizieren/Löschen, Code-Übernahme). SimpleLib: `orbit_camera`.
- Client-Smoke simplemodels: Editor öffnen (Klassisch + Nur Code) mit Screenshots `simplemodels-editor`,
  `simplemodels-editor-code`.
- Ein sbt-Lauf: Compile `:mc26_3:fabric`, `:modules:simplemodels:fabric`, `:modules:simplelib:fabric`,
  `:modules:simplebuildingtools:fabric`; Targets `module-simplemodels-fabric-263`, `-standalone-`,
  `module-simplelib-fabric-263`, `-standalone-`, Bauplan-Filter in simplebuildingtools; dazu (laut Einzelauftrag) der
  Client-Smoke `module-simplemodels-client-263` für den echten Screenshot.
- Kontrast: Text `#404040` auf `#C6C6C6` 6,1:1; heller Text `#F2EEE8` auf Viewport `#1E1F23` ≈ 15:1; Auswahl-Zeile
  Weiß auf `#5A5A5A` 6,9:1.
- Nicht in diesem Branch: Echte-Maus-Klicktest (`menu_probe.py`, braucht Client-Fenster auf sb-test) → offen vermerkt.

## Entscheidungen (selbst getroffen)
- Kamera bleibt orthografisch (wie Bauplan, exakte UV, Malerverfahren); Num5 perspektivisch entfällt vorerst.
- JSON statt DSL im Nur-Code-Modus, bis Stufe 4 die DSL bringt (Konzept-Reihenfolge 2 → 4 → 3 hier zusammengezogen auf
  2+3 laut Auftrag; DSL bleibt Stufe 4).
- Texturen nur aus dem Block-Atlas (Item-Atlas-Verweise zeigen Missing-Sprite im Editor, Spiel-Bild bleibt korrekt).
