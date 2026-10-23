# Plan a-frames23 (Queue FR23, 23.10., Nachtrag zu PLAN-A-FRAMES22)

Besitzer 23.10.: Druckplatten und Scheiben im Item-Rahmen nicht wie gewünscht. Sie sollen flach gezeichnet
werden, aber von OBEN gesehen, als 3D-Objekt, unmissverständlich welche Platte/Scheibe es ist – nicht von der Seite.
Nur MC 26.3 Fabric.

## Ist-Zustand (a-frames22 / TZ22-7)
- `FrameTilt.flat` (Form-Regel) + `ItemFrameRendererMixin`: Item im GUI-Kontext (schräge Inventaransicht) aufgelöst,
  um 180° Y gedreht. Ergebnis: Platte von schräg/seitlich, Scheiben (Sprite-Items) unverändert als flaches Symbol.
- `ItemFrameRenderer` hat einen `BlockModelResolver` (für das Rahmenmodell) und `BLOCK_DISPLAY_CONTEXT`;
  `BlockModelRenderState` zeichnet beliebige Blockzustände (Lore nutzt das gleiche für Blöcke im Wagen),
  Transluzenz (gefärbtes Glas) eingebaut.

## Varianten (Vorschau /root/sb-tools-a/previews/frames23-preview.html, CSS-3D mit Vanilla-Texturen)
- A reine Draufsicht: Oberseite zum Betrachter, liegt bündig auf der Rückwand; Dicke nur bei schrägem Blick sichtbar.
- B leicht gekippt (15°, unterer Rand nach vorn): Kante auch frontal sichtbar. **Empfohlen und eingebaut**
  (Auftrag: empfohlene einbauen), Winkel als Konstante `FrameTilt.TILT_DEGREES` – A wäre `0`.

## Umsetzung
- `FrameTilt`: `Kind {VANILLA, FLAT, PANE}`; FLAT = bisherige Form-Regel (Druckplatten, Teppiche, Falltüren,
  Tageslichtsensor, Licht-Scheiben von SB), PANE = Block erbt von `IronBarsBlock` (Glasscheibe, gefärbt, Eisen-/Kupfergitter,
  SB-Holzscheiben `*_sheet`). `viewState`: Grundzustand, Scheiben mit east+west = true (durchgehende Scheibe).
- Mixin `extractRenderState` TAIL: Option an, keine Karte, Kind ≠ VANILLA (FLAT nur mit Block-Item-Modell, Schienen bleiben
  Sprite) → Blockmodell in eigenem `BlockModelRenderState` (Duck `TiltedFrameState`), `state.item.clear()` damit Vanilla
  sein Item nicht zeichnet.
- Mixin `submit` vor dem letzten `popPose` (Pose am Item-Ursprung, Betrachter −Z, Wand +Z): auf die Rückwand legen,
  15° kippen (Push 0,36·sin15 nach vorn, damit die Oberkante nicht in der Wand steckt), Rahmen-Drehung (8 Stufen) in der
  Ebene, Maßstab 0,5 wie Vanilla; Platte: Rx(−90) Oberseite nach vorn, Rz(180) Norden oben; Scheibe: Fläche schon vorn.
  Licht wie Vanilla (Leuchtrahmen voll hell).
- Option `visuals.enableTiltedFrameItems` bleibt (Schlüssel unverändert, Text EN/DE neu: „von oben“).

## Abweichungen
- Gitter (Eisen/Kupfer) zählen mit, weil die Regel „erbt von IronBarsBlock“ generisch Mod-Scheiben einschließt;
  sie sind genauso flach. Herausnehmen wäre eine Zeile.

## Risiken / offen
- Ohne Client-Lauf (Regel: keine client-*-Läufe) nicht sichtbar geprüft: Vorzeichen der Kippung, Abstand zur Wand,
  Licht/Schattierung des Blockmodells, Texturausrichtung. → Client-Sicht offen (Memory + Queue).

## Tests
- `VisualsTests.frameTilt` (Gametest config): FLAT für Platten/Teppiche/Falltür/Sensor, PANE für Glas-/Rotglasscheibe/
  Eisengitter mit east+west-Zustand, VANILLA für Knopf/Stufe/Stein/Stock/Schädel/Fackel/Zaun/Luft/getöntes Glas,
  Mod-Items (SB-Diamant-Druckplatte, Licht-Scheibe FLAT, Eichen-Holzscheibe PANE) falls geladen.
- Verifikation über `/root/sbt a-frames23`: Compile `:mc26_3:fabric` + `:modules:simplevisuals:fabric`, Targets
  `module-simplevisuals-fabric-263`, `module-simplevisuals-standalone-fabric-263`, Generatoren, `check`.
