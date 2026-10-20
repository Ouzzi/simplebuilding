# Plan B: Speedlines an Geschwindigkeit koppeln (20.10.)
- Ist: VisualsHud.tick schiebt `animation` pro Tick konstant um speedLinesSpeed*4; Linienzahl konstant 30*amount (max 60). Schneller Flug wirkt wie Zeitlupe.
- Aenderung: reine Funktion `SpeedLinesMath` (tempo = speed/0.5 geklemmt 0.4..8; density = sqrt, 0.6..1.6). Tempo skaliert die Animation, Dichte die Linienzahl (Cap 90). Geglaettete Geschwindigkeit. Config (speedLinesSpeed/Amount, Schwelle) bleibt Basis.
- Test: GameTest `module_game_test_speed_lines_math` (kein JUnit im Modul) in Fabric/NeoForge/Forge-Adaptern.
- Verifikation: Compile 3 Loader, module-simplevisuals-fabric-263 + neoforge-263. Visuell (Client) offen.
