# Plan b-colorbrush (18.10.2026)

Ziel: Color Brush (ColorBrushItem / BrushInkModelProperty) pruefen und Textur-Audit ohne Augen.

## Ist-Zustand
- Item-Definition `color_brush.json` (select `simplebuilding:brush_ink`, 16 Farbfaelle + `palette`), Modelle `color_brush`, `_tip`, `_palette`, Texturen aus `tools/textures/color_brush_2026_10_09.py` (zur Bauzeit abgeleitet, nicht im Repo).
- Property ist auf Fabric (SimplebuildingClient ID_MAPPER), NeoForge (RegisterSelectItemModelPropertyEvent) und Forge (Mixin) eingetragen.
- Statisch gibt es `tools/check_missing_textures.py` (Items, Modelle, Parent-Ketten, Texturen), zur Laufzeit `audit-models-263` (ModelAuditClientTest, braucht xvfb).

## Schritte
1. Brush per Client-Screenshot unter xvfb in allen Tinten-Zustaenden ansehen (none, Farbstoff rot/weiss/schwarz, Farbkasten mit Farbstoff).
2. ModelAuditClientTest um optionale Screenshots aller Kreativtabs + Brush-Inventar erweitern (Umgebungsvariable SB_TABSHOTS).
3. Statische Luecke schliessen: neues `tools/check_item_properties.py` (jede in Item-Definitionen genutzte eigene Model-Property muss auf Fabric, NeoForge und Forge registriert sein); beide statischen Pruefungen in `gradlew check` einhaengen; die 11 bekannten Lang-only-WARNs (Legacy-Aliase, home_teleporter) mit Begruendung freistellen.
4. Tests: `simplebuilding:*brush*` fabric-263 + neoforge-263, Compile, `audit-models-263`, `gradlew check`.
