# Plan: Blaupause – 3D-Vorschau drehen, Strg ziehen, Ansicht zurücksetzen

Stand: 2026-10-08 · Branch `bp-bpdrag` (HEAD `a25d4fc98`) · Hauptlinie 26.3, Fabric zuerst
Status: **Umsetzung abgeschlossen, Tests grün, Commit lokal (kein Push)** – siehe Abschlussbericht (§ 7)

## 1. Auftrag

1. Maus-Drag in der 3D-Vorschau des Blaupausen-Bildschirms reparieren (Besitzer-Bericht: „Ziehen dreht nicht").
2. Neu: **Strg + Ziehen** verschiebt die Ansicht (Pan) statt sie zu drehen.
3. Neu: Icon-Knopf **„Ansicht zurücksetzen"** mit Tooltip (EN + DE).
4. Die Rechenlogik (Drag-Delta → Winkel, Strg → Verschiebung, Reset, Zoom-Klemmen) in eine
   **reine Klasse ohne Client-Abhängigkeit** auslagern und dafür einen JUnit-Test schreiben.

Nicht Teil des Auftrags: neue Texturen, Änderungen an Spielregeln, `mc1_21_11/`-Kopie (Port-Run).

## 2. Ist-Zustand – Beweisführung auf 26.3-Bytecode-Ebene

### 2.1 Vanilla-Eingabepfad (bewiesen)

```
SDL_EVENT_MOUSE_BUTTON_DOWN (type 0x401 = 1025)
  → SDLEventHandler.handleMouseButtonEvent: action = (type == 1025) ? 1 : 0   // 1 = Press
  → Minecraft.execute(...) → MouseHandler.onButton
```

`MouseHandler.onButton` (Press):

- `activeButton = button`, `mousePressedTime = Blaze3D.getTime()`
- `sx/sy = getScaledXPos(window)/getScaledYPos(window)` → **GUI-skalierte** Cursorposition
- `screen.mouseClicked(MouseButtonEvent(sx, sy, button), doubleClick)` mit
  `doubleClick = time - lastClick < DOUBLE_CLICK_THRESHOLD_MS && gleicher Screen && gleicher Button`
- Ergebnis `true` → `lastClick/lastClickButton` setzen, **return**;
  Ergebnis `false` → `goto 456` → KeyMapping-Pfad

`MouseHandler.onMove` (Bewegung):

- Screen offen, `!mouseGrabbed`, Fenster aktiv → `accumulatedDX += x - xpos`, `accumulatedDY += y - ypos`
- `xpos`/`ypos` werden in **allen** Pfaden (inkl. `ignoreFirstMove`, `!isWindowActive`) aktualisiert

`MouseHandler.handleAccumulatedMovement` (aus `Minecraft.runTick` laufend, **ohne** Screen-Bedingung):

- `hasMovement = accumulatedDX != 0 || accumulatedDY != 0`
- → `screen.mouseMoved(sx, sy)`
- → wenn `activeButton != null && mousePressedTime > 0`:
  `screen.mouseDragged(MouseButtonEvent(sx, sy, activeButton), scaledDX, scaledDY)`
  – **Rückgabewert wird verworfen**
- `mousePressedTime` wird nie zurückgesetzt

`MouseHandler.onScroll` berechnet die Position ebenfalls über `getScaledXPos/getScaledYPos`.

**Fazit:** Klick-, Drag- und Scroll-Koordinaten liegen in **derselben GUI-Skala**.
Ein Koordinaten-Versatz als Ursache ist damit ausgeschlossen.

`MouseButtonEvent` ist ein Record mit den Feldern `x`, `y`, `buttonInfo`;
`x()`/`y()` liefern exakt die übergebenen (skalierten) Werte.

### 2.2 Dispatch in der Screen-Basis (bewiesen)

`ContainerEventHandler.mouseClicked` (Bytecode):

1. `getChildAt(x, y)` → erstes Kind mit `isMouseOver(x, y)`; leer → `return false`
2. `child.mouseClicked(...)`, ggf. `setFocused(child)`
3. wenn `event.button() == 1` → `setDragging(true)`
4. danach **immer `iconst_1; ireturn`** → liefert `true`, **auch wenn das Kind den Klick nicht konsumiert hat**

`BlueprintScreen` überschreibt `mouseClicked` und ruft `super.mouseClicked(...)` auf.
`Screen` selbst überschreibt **keine** Mouse-Methode, `super` ist also genau die oben stehende Methode.
`Screen.children()` gibt das Feld `children` zurück; `Screen` fügt selbst keine Kinder hinzu
(`narratorButton` wird nur gelesen, nie addiert).

### 2.3 Widget-Geometrie (bewiesen)

Beispiel Desktop 960×540 (guiScale 2): `codeX/codeW`, `viewX = codeX + codeW + 4`,
`viewY = bodyY`, `viewW`, `bodyH`, `footerY = bodyY + bodyH + 3`, `FOOTER = 56`.

`AbstractWidget.isMouseOver` = `isActive()` (`visible && active`) && Rechteck,
strikte Ungleichungen, `getRight() = x + width`, `getBottom() = y + height`.
Keine `isMouseOver`-Overrides in `AbstractButton`/`Button`/`EditBox`.
`GuiEventListener.mouseMoved` hat den Default `return`.

Überlappung mit dem View-Rechteck `[viewX, viewX+viewW) × [bodyY, bodyY+bodyH)`:

| Kind | Rechteck | Überlappt View? |
|---|---|---|
| `codeArea` | `[codeX, codeX+codeW) × [bodyY, bodyY+bodyH)` | nein (Lücke 4 px) |
| `insertBox`, `insertButton` | `y ≥ footerY + 12` | nein |
| `helpButton` (BookButton) | `[viewX+viewW-16, …) × [bodyY-16, bodyY)` | nein (View beginnt bei `bodyY`) |
| `exampleButton` | `[viewX+6, …) × [bodyY+bodyH-24, bodyY+bodyH-6)` | **ja, sichtbar nur bei** `!readOnly && !helpOpen && code.isBlank()` |
| `guideTab`, `blocksTab` | `[viewX+2, …) × [bodyY+2, bodyY+16)` | ja, sichtbar nur bei `helpOpen` |
| `helpSearch` | `[viewX+3, …) × [bodyY+19, bodyY+33)` | ja, sichtbar nur bei `helpOpen && helpBlocks` |
| `doneButton`, `signButton`, `titleBox`, `confirmSignButton`, `cancelSignButton` | `y ≥ footerY` | nein (`footerY > viewBottom`) |

Verkettung mit der Sichtbarkeit des Modells:

- Modell wird gezeichnet genau bei `!helpOpen && !parsed.model().isEmpty()`
- `code` und `parsed` werden gemeinsam in `reparse()` aktualisiert (Debounce `dirty &&
  now - lastEdit > 150 ms` in `tick()`); `updateButtons()` läuft danach im selben Aufruf
- `code` wechselt zu blank erst in `onCodeChanged`, aber `updateButtons()` wird dort **nicht**
  gerufen → `exampleButton` wird erst sichtbar, **nachdem** `reparse()` den leeren Code verarbeitet
  hat → **danach ist das Modell schon weg**

**⇒ Modell sichtbar ⟺ kein Kind liegt im View-Rechteck.**

### 2.4 Befund und offene Frage

Unter den geprüften Annahmen **müsste** der Drag funktionieren:

> freier View-Klick → `super.mouseClicked` liefert `false` → `draggingView = true` →
> nächster `mouseDragged` dreht die Ansicht.

Die statische Analyse liefert damit **keine bewiesene Ursache** für den Besitzer-Bericht.

Bewiesene **strukturelle Schwachstelle**: Der Drag-Start hängt davon ab, dass **kein** Kind
unter dem Cursor liegt – obwohl `ContainerEventHandler` schon bei reinem Kind-Treffer `true`
liefert. Das ist genau die Umkehrung des Original-Stands `81958898e` (`overView` **vor** `super`);
eingeführt durch `54988a992`. Zusätzlich ist `mousePressedTime` nie zurückgesetzt und der
Doppelklick-Reset in `mouseClicked` (`doubleClick` → `rotation = default; zoom = 1`) ein
möglicher „springt zurück"-Effekt.

**Die offene Frage wird durch den Reproduktionstest (Schritt 4.1) beantwortet** – nicht durch
weitere statische Analyse. Der Test meldet im Fehlerfall zusätzlich, **welches** Kind unter dem
Cursor liegt.

## 3. Geplante Änderungen

### 3.1 Neue reine Klasse `BlueprintViewControls`

`common/src/shared/java/com/simplebuilding/blueprint/BlueprintViewControls.java`
(Paket wie `BlueprintCode`/`BlueprintModel`, **ohne** `net.minecraft.client.*`, nur JOML):

- Zustand: `rotation` (`Quaternionf`), `zoom`, `panX`, `panY`
- Operationen: `drag(dx, dy)`, `pan(dx, dy)`, `scroll(scrollY)`, `reset()`, `isDefault()`
- Konstanten: `DRAG_RAD_PRO_PIXEL`, `ZOOM_FAKTOR`, `ZOOM_MIN/MAX`, Pan-Klemme, `DEFAULT_ROTATION`
- `BlueprintView.defaultRotation()` verweist künftig auf diese Klasse (kein Signalbruch,
  `BlueprintTooltip` usw. bleiben kompilierbar)

### 3.2 `BlueprintScreen`

- Felder `rotation`/`zoom`/`draggingView` durch eine `BlueprintViewControls`-Instanz ersetzen
- `mouseClicked`: View-Prüfung wieder **vor** `super` (wie `81958898e`), aber Kindertreffer
  ausnehmen – ein tatsächlich sichtbares Kind unter dem Cursor bekommt den Klick
- `mouseDragged`/`mouseScrolled` delegieren; bei gehaltener Strg Pan statt Rotation
- Neuer `ResetViewButton` (16×16, Stil wie `BookButton`, **kein** Textur-Asset):
  Vanilla-Item-Icon, Position `(viewX, bodyY - 16)` – **außerhalb** des View-Rechtecks,
  damit er den Drag nicht verschluckt; Tooltip-Key EN + DE

### 3.3 `BlueprintView`

- Neue `render`-Überladung mit Pan-Versatz: Clip-Rechteck bleibt, nur der Mittelpunkt (`cx`/`cy`)
  wandert; bestehende Signatur unverändert (weiterhin von `BlueprintTooltip` genutzt, Z. 92)

### 3.4 Sprache (EN + DE, gleiche Zeilennummer)

- `simplebuilding.blueprint.editor.view_reset_tip` (neu)
- `simplebuilding.blueprint.editor.view_hint` (Z. 859) um Strg ergänzen:
  EN `Drag: rotate, Ctrl+drag: move, wheel: zoom` /
  DE `Ziehen: drehen, Strg+Ziehen: verschieben, Mausrad: zoomen`

### 3.5 Nicht angefasst

`mc1_21_11/`-Kopie · Texturen/Generatoren · Wiki (`wiki/generate.py` nur nötig, wenn sich
Werkregeln-Texte ändern) · Server-Logik (reine Client-UI-Änderung) · `gradlew`-Wrapper-Datei

## 4. Testplan

### 4.1 Reproduktion zuerst (test-first)

Neuer Client-Test `BlueprintViewClientTest`, Eintrag in
`ClientTests.allInWorld()`, Einzug über `SIMPLEBUILDING_CLIENT_ONLY`:

1. Editor wie `BlueprintEditorClientTest` öffnen, Code einfügen (Clipboard + Ctrl+V), idle >
   150 ms, damit `reparse()` das Modell liefert
2. View-Mitte in Fensterkoordinaten berechnen (Formel aus `ModScreensClientTest` invertiert:
   `guiX * screenWidth / guiScaledWidth`), privates View-Rechteck per Reflexion lesen
3. `setCursorPos` → `holdMouse(LEFT)` → **`draggingView` per Reflexion lesen**
   – Fehlermeldung listet `overView`, `helpOpen`, `exampleButton.visible` **und alle Kinder mit
   `isMouseOver`**, damit der Test den Schuldigen benennt
4. Cursor um +60 px bewegen, `releaseMouse(LEFT)` → `rotation` hat sich gegenüber
   `defaultRotation()` geändert

### 4.2 Unit-Test der reinen Klasse

`src/test/java/com/simplebuilding/BlueprintViewControlsTest.java` (JUnit Jupiter 5.11.4,
`build.gradle` Z. 200/222, läuft mit `./gradlew check`):

- `drag` dreht um `dx * DRAG_RAD_PRO_PIXEL` um die Y-Achse, `dy` um die X-Achse
- `pan` verschiebt nur `panX/panY`, nie `rotation`
- `reset` stellt `DEFAULT_ROTATION`, `zoom = 1`, `pan = 0` her; `isDefault()` korrekt
- Zoom bleibt in `[ZOOM_MIN, ZOOM_MAX]` (Obergrenze Vanilla-sicher)

### 4.3 Läufe

- Unit: `./gradlew test --tests 'com.simplebuilding.BlueprintViewControlsTest'`
- Client (Xvfb, nur über den Lock):
  `SIMPLEBUILDING_SERIAL_TESTS=1 /root/heavy.sh job "xvfb-run -a -s '-screen 0 1920x1080x24' python3.12 tools/testrunner/run.py --targets client-fabric-263 --filter BlueprintView"`
- Vollständiges Gate in einem Worktree: `./gradlew check -q`
- **Nicht** `/root/sb-gate.sh` (arbeitet in `/root/simplebuilding` und pusht nach `master`)
- Am Ende: `/sbtestcentre build`, Abdeckungstest muss alle Items abdecken

## 5. Risiken

| Risiko | Gegenmittel |
|---|---|
| Reproduktion schlägt fehl (Drag funktioniert im Harness) | Testabbruch, ehrlicher Bericht, Ursache neu angreifen statt blind „fixen" |
| Reflexion auf private Felder wird instabil | Nur im Test, Muster wie `trimButtonProblem`/`expectTextInput` im Bestand |
| `hasControlDown` fehlt in 26.2 | Vorhandensein in beiden Lines prüfen, sonst Modifikatoren-Schnittstelle nutzen |
| Reset-Knopf verschluckt Drag | Position außerhalb des View-Rechtecks + Reproduktionstest deckt es auf |
| Client-Kaltlauf ~25 min, RAM im CT | Nur `/root/heavy.sh job`, nie parallel zu Gate/Nachtlauf, `SIMPLEBUILDING_SERIAL_TESTS=1` |

## 6. Umsetzungsreihenfolge

1. Reproduktionstest schreiben (4.1), Lauf unter Xvfb → benennt die Ursache
2. `BlueprintViewControls` + JUnit-Test (4.2)
3. `BlueprintScreen`/`BlueprintView` umbauen (3.2/3.3), Lang (3.4)
4. Client-Testlauf grün, `./gradlew check` grün
5. Commit auf `bp-bpdrag`, **Push erst nach grünem Ergebnis**; danach
   `/sbtestcentre build`
6. Offene Punkte in die Memory; Prod-Übernahme offen lassen (TODO)

## 7. Abschlussbericht (2026-10-08, ehrlich)

### 7.1 Reproduktion: **Drag funktioniert – Besitzer-Bericht nicht reproduzierbar**

`BlueprintViewClientTest` (26.3 Fabric, echter `MouseHandler.onButton`/`onMove`-Pfad über die
Harness, 64 Schritte):

- Lauf 1 (Code `stone 2`, ungültig): **rot** – `Problem[line=0, start=6, end=7, key=bad_region, args=[2]]`; das Modell war leer (Eigenschuld des ersten Entwurfs).
- Lauf 2 (Code `stone 0..3,0..3,0..3`): **grün** (`1/1 bestanden, 0 rot`, ~49 s,
  `2026-10-08T11-04-13Z-880d`, Screenshot `screen-blueprint-preview-dragged`).

Damit ist § 2.4 beantwortet: Die faire statische Kette hält; der Drag startet (`draggingView`)
und dreht. Der Besitzer-Bericht „Ziehen dreht nicht" lässt sich auf 26.3 Fabric mit dem
dreifachen Positions-/Extensions-Test nicht reproduzieren. Einziger verbliebener Kandidat für
„springt zurück": der **Doppelklick-Reset** in `mouseClicked` (`doubleClick` → Reset), der hier
bewusst nicht angefasst wurde.

### 7.2 Umgesetzt (alle Punkte aus § 3)

- Neu `common/src/shared/java/com/simplebuilding/blueprint/BlueprintViewControls.java`
  (pure JOML, keine MC-Imports): `drag`/`pan`/`zoom`/`reset`/`isDefault`, Konstanten
  `DRAG_RAD_PER_PIXEL=0.012`, `ZOOM_STEP=1.15`, `ZOOM_MIN=0.2`, `ZOOM_MAX=12.0`, `PAN_LIMIT=96`.
- `BlueprintScreen`: Zustand in `controls` (`BlueprintViewControls`), `mouseClicked` prüft den View
  **hinter** `super` (bewusst kleinere Abweichung von § 3.2 – einfacher, gleiche Wirkung), setzt
  `draggingPan` aus `event.hasControlDown()`; Doppelklick → Reset; `mouseDragged` → Pan oder Drag;
  `mouseScrolled` → Zoom; neuer `ResetViewButton` (16×16, `Items.COMPASS`, Tooltip/Message EN+DE,
  Position `viewX+viewW-32 … × bodyY-16 …` – außerhalb des View-Rechtecks).
- `BlueprintView`: `defaultRotation()` delegiert an die reine Klasse; neue `render`-Überladung mit
  Pan-Versatz (`cx`/`cy` wandern, Clip bleibt).
- Lang (`en_us.json`/`de_de.json`, beide Z. 859): `view_hint` EN `Drag: rotate, Ctrl+drag: move,
  wheel: zoom` / DE `Ziehen: drehen, Strg+Ziehen: verschieben, Mausrad: zoomen`; neu `view_reset`
  (EN `Reset view` / DE `Ansicht zurücksetzen`) und `view_reset_tip` (EN `Reset rotation, zoom and
  position` / DE `Drehung, Zoom und Position zurücksetzen`).

### 7.3 Tests

- JUnit `BlueprintViewControlsTest`: **7/7 grün** (`./gradlew :test --tests '…blueprint.BlueprintViewControlsTest'`).
  Achtung: `--tests` ohne Projektpräfix schlägt wegen `modules:simpledimensions:fabric:test` fehl
  (Filter findet dort nichts) – Präfix `:test` verwenden.
- Client `BlueprintViewClientTest` erweitert und **grün**: Drag an drei Höhen (Mitte, oberste Reihe,
  unterste Reihe), Strg-Pan, Reset-Knopf (`1/1 bestanden, 0 rot`, 51,7 s,
  `2026-10-08T11-31-53Z-7842`).
  **Einschränkung (bewusst):** Die Harness liefert für jedes Maus-Event Modifikatoren `0` und der
  Pan-Entscheid fällt in `mouseClicked` anhand des **Press**-Events – ein Strg-Druck kann über den
  echten Fensterpfad nicht erzeugt werden. Strg-Klick/-Drag werden deshalb direkt auf den
  Screen-Handlern mit `MouseButtonInfo(button, 192)` gefahren; die echte Maus-Mechanik
  (Press/Drag/Release) beweist der Rotations-Test. (Erster Ansatz – `activeButton`-Injektion vor dem
  Drag – funktionierte nicht, da `draggingPan` bereits beim Press entschieden wird; belegt durch
  roten Lauf `2026-10-08T11-26-33Z-92a2`.)
- **Gate:** `./gradlew check -x checkModuleData` → **BUILD SUCCESSFUL** (inkl. `:test`,
  `checkBalance`, `checkAtlases`, `checkWiki`, `checkMultimod`, Modul-Compiles, 56 Wiki-Tests).
  `compileGametestJava` (26.2-Gegenprobe des Client-Tests): grün.
- **Vorbestehendes Linux-Problem, NICHT von dieser Änderung:** `checkModuleData` scheitert auf
  Linux an `modules/simplequalityoflife/wiki/manual.json` (committet,
  letzter Commit `6fa4ac0f5/3200bbf95`): `features[].sources` enthält Windows-Pfade mit
  Backslashes, die `(MODULE.parents[1] / source)` auf Linux nicht auflöse; 10/11 Modul-Checks grün.
  Auf dem Windows-Dev-Rechner läuft der Check normal; hier nicht als Regression zu verbuchen, aber
  als offener Punkt festgehalten.
- `/sbtestcentre build` ist in diesem CT **nicht installiert** (Pfad existiert nicht) – konnte nicht
  ausgeführt werden (offener Punkt, betrifft nur UI ohne neue Items).

### 7.4 Commit

Lokaler Commit auf `bp-bpdrag` (kein Push). Enthält: `BlueprintViewControls.java` (neu),
`BlueprintScreen.java`, `BlueprintView.java`, beiden `lang/*.json`,
`BlueprintViewClientTest.java` (neu), `ClientTests.java`, `BlueprintViewControlsTest.java` (neu),
Plan-Datei. Nicht Teil des Commits (bleiben staged-fremd/unstaged): `gradlew`-Modusänderung
(100644→100755, vorbestehend) und `wiki/assets/textures/render/*_head.png` (Nebenprodukt eines
Wiki-Generator-Laufs während des Gate-Checks, unabhängig vom Blaupausen-Thema). Prod-Übernahme
offen → TODO/Memory.
