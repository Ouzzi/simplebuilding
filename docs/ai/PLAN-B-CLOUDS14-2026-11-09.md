# Plan B-CLOUDS14 (09.11.): Wolkeninsel-Unterseiten fast schwarz (N7, nur vorbereiten)

Branch `b-clouds14`, Basis `b-bundle163`. Befund A-Release-Abnahme (`docs/ai/RELEASE-ABNAHME-A-2026-11-09.md`, Zeile Wolkenmeer):
Unterseiten großer Inseln RGB 7,7,8, auch nach 200 Ticks. Auftrag: Ursache erheben, Varianten vorbereiten, Standard unverändert.

## Ursache (Ist-Stand, MC 26.3-Quellen geprüft)

1. **Kein Himmelslicht unter dem Kern.** Inseln: Hülle `cloud_block` (noOcclusion, Licht-Dämpfung 1), Kern `cloud_stone`
   (voller Block, Dämpfung 15, `default_block` in `noise_settings/cloud_sea.json`). Unter dem Kern kommt Himmelslicht nur seitlich
   vom Rand (-1 je Block) → ab ~15 Blöcken Randabstand Licht 0. Kein Blocklicht.
2. **Umgebungslicht-Farbe fehlt.** In 26.3 rechnet der Lightmap-Shader (`assets/minecraft/shaders/core/lightmap.fsh`) die Grundhelligkeit
   bei Licht 0 aus dem Umgebungs-Attribut `minecraft:visual/ambient_light_color` (nicht mehr aus `ambient_light`). Vanilla setzt es
   in jeder Dimension: Overworld `#0a0a0a`, Nether `#302821`, End `#3f473f`. `dimension_type/cloud_sea.json` setzt es **nicht** →
   Standard `#000000` = schwärzer als jede Vanilla-Höhle. Das ist der eigentliche Grund für „fast schwarz“ (Rest 7,7,8 = Nebel/AO).
3. **Unterseiten-Schattierung** `cardinal_light: default` → Unterseite × 0.5 (Nether-Typ: × 0.9). Verstärkt nur, ist nicht Ursache.
4. Geprüft und **kein** Grund: Block-Textur (`cloud_block.png` ist schon fast weiß; 15 % heller → bei Licht 0 weiter 0,0,0),
   Modell-Shade (Shade-Faktor multipliziert 0 → bleibt 0), `ambient_light` 0.0 (in 26.3 nur noch `LevelReader.getBrightness`,
   also Spiel-Logik wie Sonnenbrand-Helligkeit; für das Bild wirkungslos → **nicht** anfassen, A12).

## Varianten (alle gerechnet mit der echten Shader-Formel, Helligkeit 50 %, Mittag, ohne Nebel/AO)

| | Änderung | Mitte Unterseite (RGB) | Eingriff (A12) |
|---|---|---|---|
| Vorher | – | 0,0,0 | – |
| A | `dimension_type/cloud_sea.json` → `attributes."minecraft:visual/ambient_light_color": "#3a4252"` (bläulich, zwischen Overworld und End) | 46,53,69 | nur Optik, nur eigene Dimension, Datapack, kein Licht-/Spawn-Effekt |
| B | `cloud_stone` lässt Himmelslicht durch (wie `cloud_block`, Dämpfung 1; `noOcclusion`) | 0,0,0 bei dicken Inseln (hilft nur am Rand/dünnen Inseln) | Spiel-Logik (Lichtwerte), mehr Licht-Updates, Wolkenstein rendert Innenflächen |
| C | Decken-Regel in `material_rule/cloud_sea.json` → neuer Block „leuchtende Wolke“ (Licht 8) an der Unterseite | 55,48,32 (warm, Fackel-Ton) | neuer Block, echtes Blocklicht, Licht-Engine bei Generierung |
| D | A + `cardinal_light: "nether"` | 83,96,124 | nur Optik; alle Unterseiten in der Dimension fast so hell wie Seiten (auch gebaute) |

Bilder: Vorschau `/root/sb-tools-b/previews/b-clouds14-preview.html` (Blick von unten auf eine 46×38-Insel). Skript `/root/tmp-b-clouds14/render.py`.

## Entscheidungen (selbst, begründet)

- **Keine Config-Option, kein Code.** Abweichung vom „optional als Config“: A und D sind Dimensions-Attribute; sie werden beim Weltstart
  aus dem Datapack geladen und an den Client gesynct. Umschaltbar per Server-Config ginge nur mit Registry-Eingriff oder Client-Mixin
  in `LightmapRenderStateExtractor` – mehr Code und Risiko als die Ein-Zeilen-Umsetzung nach Wahl. Standard bleibt damit sicher unverändert.
- **Kein Client-Screenshot.** Auftrag-Regel B09 „keine Client-Läufe“ (macht das Bündel); Bilder gerechnet. Nach der Wahl im Bündel-
  Client-Lauf `rel10-client-263` (Teleport Wolkenmeer y 40) nachsehen.
- Empfehlung **A**: behebt die Ursache wie Vanilla (jede Dimension hat eine Grundfarbe), Schatten bleibt erkennbar, null Spiel-Eingriff.
  D, wenn der Besitzer Unterseiten „wolkig hell“ will. B/C nicht empfohlen.

## Umsetzung nach Wahl (für die Folge-Sitzung)

- A: eine Zeile in `modules/simplelayers/shared/resources/data/simplelayers/dimension_type/cloud_sea.json` (`attributes`).
- D: zusätzlich `"cardinal_light": "nether"` auf oberster Ebene derselben Datei.
- Test: `module-simplelayers-fabric-263,module-simplelayers-standalone-fabric-263` (Datapack lädt) + Bündel-Client `rel10-client-263`.
- Antwortcode `CLOUDS14=<keine|A|B|C|D>`.
