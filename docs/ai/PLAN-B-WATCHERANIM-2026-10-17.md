# Plan b-watcheranim (17.10.2026): Hollow Watcher Animation (Rest von A1)

Konzept: KONZEPT-MOB-ANIMATIONEN-2026-10-15.md, Abschnitt Hollow Watcher. Nur Client-Modell/Renderstate, kein Verhalten (N64), keine Cubes/Texturen.

- 6 Einfrier-Posen (`FearLogic.WATCHER_POSES`); bei jedem Wechsel unbeobachtet -> beobachtet/eingefroren waehlt `nextWatcherPose` (deterministisch je Entity-Id + Zaehler, nie dieselbe wie zuvor).
- Stop-Motion-Gang: `stopMotion(pos, 0.9)` (~9 Frames/s).
- Nachziehbein: `legSwing` (Bein haengt beim Zurueckschwingen hinterher) + Z-Schleifen.
- Reach/Claw/Scream: Rampen im Entity-Clienttick (`FearLogic.ramp`, 5/8/10 Ticks) + `ease`; Schrei: Kopf zurueck + 30-Hz-Zittern 0,05 rad.
- Test: GameTest `fear_watcher_anim`. Verifikation: Compile 3 Loader + :compileJava, simplemobs-Targets Filter `simplemobs:fear_*`, Client-Smoke simplemobs.
