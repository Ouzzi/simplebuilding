# Plan b-trims3 (Queue N58: Simple Trims, Rest)

Ist: JEI-Extension erledigt (oc-n58-small). Platten: SB `placed_smithing_template` (nur noch Nicht-Besatz-Vorlagen auf 26.3) und
`simpletrims:placed_template` (Besatzvorlagen). Hand-Neigung = framework `TransformHints` (Hoe/Sandwiches nutzen es); SB fragt es
über `ModuleTransformHints` in `TransformTargets.handTransforms`.

1. Hammer-Neigung: `PlateHint` (simpletrims) registriert in `SimpleTrims.init`: Werkzeug (Hammer/Axt je Regel) + Material (Leuchttinte/
   Glowstone) + Ziel = aufwertbare Platte + Materialien da -> Neigung. Grenzen: nur volle Neigung (das Framework kennt keinen Teil-Hinweis);
   Off-Hand-Neigung unterdrückt SB, wenn die Haupthand klickt (wie sonst).
2. Ziel-Highlight: `PlacedTrimTemplateRenderer` zeichnet die anvisierte Platte im Werkzeug-Stand pulsierend hell (Licht 11..15).
   Kein neuer Umriss-Renderer (je Loader unterschiedlich).
3. Migration: IdMoves für Items/Komponenten/Rezepte/Advancements schon da (SimpleTrims.registerIdMoves). Fehlte: alte SB-Platten mit
   Besatzvorlage bleiben im SB-Block. Neu: `PlacedTemplates.migrateToSimpleTrims` (SB, per ID, kein Import) im Ticker alle 20 Ticks
   -> Block + Template wandern nach `simpletrims:placed_template` (Face/Facing/Wasser/Flat). Kein Block-Move per IdMoves, weil der
   SB-Block weiter andere Vorlagen hält.
Tests: Modul `plate_hint`; SB `placed_template_game_test_old_plates_with_trim_templates_move_to_simple_trims`.
Verifikation: Compile 3 Loader, Modul-Targets fabric/neoforge-263, SB-Filter placed_template*.
