# Plan: Gadget-Animationen (Resonanzstab, Rotator, Attractor) – 2026-10-03

Quelle: `.claude/QUEUE.md`, Besitzer 2026-10-02 (Nachtrag 3). Nur 26.3 (`McVersion.GADGET_REWORK`), Branch `claude-anims`.

## Ist-Zustand
- Resonanzstab = Item `amethyst_lens` (`TweaksItems.LASER_POINTER`, `LaserPointerItem`): echte Benutzung
  (`startUsingItem`, 72000 Ticks) -> Vanilla-Bedingung `minecraft:using_item` greift. Texturen nur im 26.3-Overlay
  (`resonance_rod_2026_10_02.py`), Item-Definition aus `TweaksModelGen#…` (range_dispatch `damage`, leer bei 1,0).
- Rotator (`RotatorItem`): sofortige `useOn`-Aktion, kein Halten, kein Cooldown -> `using_item` ist nie wahr.
  Textur `src/main/.../rotator.png` (aus `generate_textures.py#rotator_texture`), 26.3 hat keine eigene.
  Der 26.3-Hand-Hinweis (`HeldItemRendererMixin`) fragt `TransformTargets.canTransformTarget` (= "Rechtsklick würde
  den anvisierten Block drehen").
- Attractor = Item `magnet`, Overlay-Textur (round5 A: Redstone-rot / Lapis-blau, Eisenspitzen), flaches Modell.
- Präzedenz für Item-Animationen: `detector.png.mcmeta` (2 Bilder, interpolate), Echolot (frametime 2).

## Entscheidungen / Annahmen
1. **Rotator-"Benutzung"** = Rotator zielt auf einen Block, den ein Rechtsklick drehen würde (gleiche Bedingung wie der
   Hand-Hinweis). Neue Client-Eigenschaft `simplebuilding:transform_hint` (ConditionalItemModelProperty,
   `client/property/TransformHintModelProperty`): wahr, wenn der Stapel in einer Hand des lokalen Spielers liegt und
   `TransformTargets.canTransformTarget(...)` für diese Hand gilt. Registriert wie die vorhandenen Eigenschaften:
   Fabric `ID_MAPPER.put`, NeoForge `RegisterConditionalItemModelPropertyEvent`, Forge Mixin auf
   `ConditionalItemModelProperties#bootstrap`. Kein Spielverhalten geändert.
2. **Leer-Varianten** (`amethyst_lens_empty`, `rotator_empty`) bleiben **statisch**: "erloschen" soll auf einen Blick
   lesbar sein; die Damage-Abfrage steht vor der Benutzungsbedingung.
3. Animation **nur über Farbe/Alpha**: Silhouette jedes Bildes identisch (Resonanzwellen/Feldlinien nur auf vorher
   transparenten Pixeln) -> kein Jitter. Transparente Effekt-Pixel tragen schon im Ruhebild die Effektfarbe mit
   Alpha 0, damit Vanillas Interpolation (mischt ARGB) nicht grau einblendet.
4. Ruhepausen über `frames`-Liste mit doppeltem Ruhebild (sonst würde `interpolate` während der ganzen Pause überblenden).

## Animationen (Frames / frametime, Begründung)
| Textur | Bilder | Ablauf | Begründung |
|---|---|---|---|
| `amethyst_lens` (Ruhe) | 4 (Ruhe, Funkeln Spitze/oben/unten) | interpoliert; je Funkeln 4 Ticks an, 6 aus, dazwischen 24–30 Ticks Ruhe, Zyklus 108 Ticks (5,4 s) | ruhiges Amethyst-Funkeln wie Vanilla-Glanz, nicht hektisch |
| `amethyst_lens_active` (Benutzung) | 6 | je 2 Ticks, ohne Interpolation, 12 Ticks | Wellen laufen sichtbar von der Spitze nach außen, Kristalle heller – deutlich stärker als Ruhe |
| `rotator` (Ruhe) | 9 (Ruhe + 8 Glanzstellungen) | 60 Ticks Ruhe, Glanz läuft einmal um den Bogen (8×2 Ticks, interpoliert), 76 Ticks | leichtes Kreisen des Eisenglanzes |
| `rotator_active` (zielt auf drehbaren Block) | 8 | je 2 Ticks, ohne Interpolation, 16 Ticks/Umdrehung | sichtbare Drehung, Perle leuchtet |
| `magnet` (Ruhe) | 3 (Ruhe, Feldlinie innen, beide) | 123 Ticks Ruhe, 3+3 an, 4+4 aus (~0,7 s Blitz), 137 Ticks | kurz angedeutet, lange Pause, Item unverändert |

## Dateien
- `tools/textures/gadget_animations_2026_10_03.py` (Generator, `--check`, Vorschau-PNG + GIFs).
- Overlay-Texturen + `.mcmeta`: `amethyst_lens`, `amethyst_lens_active`, `rotator`, `rotator_active`, `magnet`.
- Datagen: `TweaksModelGen` (Linse), `ModModelProvider#generateRotator` – mit `GADGET_REWORK` Bedingungen ergänzen.
- `client/property/TransformHintModelProperty.java` + Registrierung Fabric/NeoForge/Forge.
- GameTest: Item-Definitionen von Linse/Rotator/Magnet (Bedingungen, Modelle, Animationsstreifen/mcmeta) auf 26.3.
- Hinweise in `resonance_rod_2026_10_02.py` / `round5_settled_2026_10_02.py`: danach Animationsgenerator laufen lassen.

## Risiken
- Fabric-Zugriff auf `ConditionalItemModelProperties.ID_MAPPER` (Fabric-API-AW) – Compile prüft.
- 26.2 darf keine Overlay-Texturen referenzieren -> nur hinter `GADGET_REWORK` (DataIntegrity-Test prüft je Linie).
- Client-Sicht (Animation im Spiel) nicht automatisiert prüfbar -> Vorschau-GIFs; Sichtabnahme im Client offen.

## Verifikation
`generate_textures.py --check`, Generator `--check`, Datagen 26.3 (+ 26.2 unverändert), Wiki venv `--all` + uv
`--all --check`, Tests `fabric-263`, `neoforge-263`, Compile 26.2 (`:compileJava :neoforge:compileJava`) und Forge 26.3.

## Ergebnis (2026-10-03)
- Umgesetzt wie geplant. Abweichungen: Resonanzwellen von Hand platziert (Kreisringe passten am Bildrand nicht);
  zusätzlich `wiki/index.html`: Animationsstreifen zeigen im Wiki nur das erste Bild (`object-fit: cover; object-position: top`,
  betraf schon `detector.png`).
- Gates: `generate_textures.py --check` OK, Generator `--check` OK, Datagen 26.3 (nur 4 neue Dateien), Wiki venv `--all` +
  uv `--all --check` OK, Wiki-Unittests 38 OK, fabric-263 + neoforge-263 "alles gruen: 1746/1746", Compile 26.2 +
  Forge 26.3 OK.
- Nicht getestet: Animation im laufenden Client (kein Client gestartet), Forge-26.3-Mixin zur Laufzeit, 26.2-Datagen nicht
  neu gefahren (Pfad durch `GADGET_REWORK` ausgeschlossen, 26.2-Zweig im neuen Test), `wiki/tests/ui_lists.cjs` (kein Playwright).
