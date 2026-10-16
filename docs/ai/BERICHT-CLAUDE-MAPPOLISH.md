# Bericht claude-mappolish (Nachtrag 68, Wayfinder-Karte)
- WaypointArt: Icon wird aus der gerenderten Modell-Bounding-Box (ItemStackRenderState.visitExtents) skaliert/zentriert, füllt das Quadrat; 1-px-Outline per 4 schwarzen Durchgängen (flache Items: Sprite-Silhouette, 3D: konvexe Hülle); kein Hintergrund mehr. Karte, Lesezeichen, Configure-Dialog (Raster, Vorschau; Würfelnetz ohne Outline).
- Wegpunkt-Name neben dem Icon, vertikal auf Icon-Mitte (WayfinderScreen.drawMarks); Koordinaten darunter.
- Seitliche Knöpfe: neue WayfinderTabStyle (Rim/Bevel 0.82/Band 0.64 wie UiBoxes.box, gestufte Ecken via stairFrame); aus = dunkler, an = Boxfarbe + Balken, Hover = heller Rand. Kontrast Icon/Fläche >= 4,5:1 (Test sideButtonInkIsReadableInEveryState).
- Tests: Compile 3 Loader + :modules:simplemaps:fabric:test EXIT=0; Client-Smoke module-simplemaps-client-263 alles gruen 27/27.
- Vorschauen: docs/ai/previews/claude-mappolish/ (Karte mit Creeper-Kopf + Camp, Configure-Dialog) selbst angesehen.
- Nicht getestet: Forge-/NeoForge-Client, Ausreißer-Modelle (Hülle bei konkaven 3D-Modellen leicht größer), Skalierung bei GUI-Scale 1 (leichte Unschärfe möglich). Offen: Merge nach wave1.
