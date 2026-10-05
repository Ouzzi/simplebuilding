# Plan: Client-Tests 26.3 wieder grün (Nacht-Serie master e2597bc8, 59 rot)

Branch `claude-clienttests` (Basis 6735bc9b, enthält e2597bc8). Nur committen, nicht pushen.

## Befund (Logs `testing/runs/2026-10-05T00-45-51Z-c50b-client-*-263.log`)
Alle 59 „kein frischer Screenshot“ sind Folgeschäden: das Skript brach vorher ab, alle späteren
Shots fehlen. Es sind nur 6 echte Abbrüche:

| Loader | Skript / Schritt | Ursache |
|---|---|---|
| Fabric + NeoForge | hud-and-tooltip „noise floor of the glimmer scene“ (112 px, erlaubt 60) | aafca8ae: `detector.png.mcmeta` (2 Frames, frametime 16, interpoliert) – der ruhende Detektor pulsiert jetzt im Inventar. Test-Szene nicht mehr statisch. Besitzer-Entscheidung (Puls gewollt) → Test anpassen |
| Fabric + NeoForge | item-rendering „chisel tilt is at rest at 0.0“ nach Enderit-Nugget | 5af04b2a/Q5: Hammer + falsches Material = Teil-Hinweis (Neigung 1.0, Skala 0.5). Test erwartet noch 0. Besitzer-Entscheidung (Backlog Q5) → Test anpassen, 26.2 behält alte Erwartung (`McVersion.TRANSFORM_HINTS_AND_CORNERS`) |
| NeoForge | block-highlight, building-wand-preview, hud whitelist icon, chisel tilt drew 0 px | Umgebung: Fenster 627x1112 (Hochformat, Windows-Snap) statt `--width 854 --height 480`; am 01.10. 1902x1112. 26.3-SDL-Fenster wird vom Desktop umgelegt. GUI-Scale 1 → Icon 128 statt 512 px, Hand aus dem Bild, Ghosts am Rand |

## Umsetzung
1. `neoforge/src/clientGameTest/.../mixin/WindowSizeMixin.java` (nur Test-Sourceset, 26.2+26.3):
   wie Fabrics `WindowMixin` – Fenster-/Framebuffergröße am Ende des Konstruktors auf die
   angeforderte Größe (windowedWidth/Height = 854x480) festnageln, `onResize`/`onFramebufferResize`/
   `refreshFramebufferSize` verwerfen. Eintrag in `simplebuildingclienttest.mixins.json`.
2. `HudAndTooltipClientTest.oreDetectorGlintMarksTheCalibratedSlot`: Item-Atlas-Animationen für
   die Szene anhalten (Atlas aus `TextureManager.tickableTextures` nehmen, danach zurück) – analog
   zur bereits eingefrorenen Glimmer-Uhr. Prüfungen unverändert.
3. `ItemRenderingClientTest.sledgehammerUpgradeAnimation`: Enderit-Nugget → 26.3: voller Hinweis aus,
   Teil-Hinweis an (`TransformTargets.partialTransformTarget`), Neigung 1.0 bei Skala 0.5; Netherit
   zurück → Skala 1.0. 26.2: wie bisher Neigung 0.

## Risiken
- Mixin-Namen 26.2 (GLFW) vs 26.3 (SDL): Handler nur mit CallbackInfo, Methodennamen-Liste.
- Präsentation in größeres/kleineres Fenster: OpenGL-Blit clippt, Screenshots lesen das Render-Target.

## Verifikation
`run.py --targets client-fabric-263`, `client-neoforge-263` (alles gruen), Gegenprobe
`fabric-263,neoforge-263`, `gradlew check -q -PskipWiki`, Compile 26.2 (`:compileJava :neoforge:compileJava`
inkl. clientGameTest).
