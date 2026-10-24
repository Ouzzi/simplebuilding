# Plan b-wa24trim (WA24-1): inverser Besatz in 3 Stufen (24.10.2026)

## Ist-Zustand
NA24-6 (b-trims24, PLAN-B-TRIMS24): Komponente `simpletrims:inverted_trim` (Boolean), Schmiedetisch Vorlage + Ruestung + Tintenbeutel einmal, Pixelmathe `TrimInversion` mit Modi HARD/RIM/GHOST (Default RIM), Renderer ueber `TrimPulseTextures`.

## Aenderung
1. Komponente `inverted_trim` = Integer 1-3 (Codec liest altes `true` als Stufe 2 = RIM, deren damaliger Look; Netz VAR_INT).
2. `TrimInversion`: Modi HARD (Stufe 1), RIM (2: 1 px Rand), SOFT (3: 2 px Rand + 1 px Mischfarbe Rand/Fuellung); GHOST entfaellt. `mode(level)`.
3. Erwerb: erneutes Umwandeln am Schmiedetisch (gleiche Vorlage + Tintenbeutel) = naechste Stufe, Stufe 3 bleibt (Ergebnis leer). `TrimGlow.get/setInvertLevel`.
4. Renderer: `TrimPulseTextures.texture(..., int inverted)` (nur Typ boolean -> int, Cache-Key enthaelt die Stufe); Tooltip "Invertiert I/II/III"; Lang/Handbuch.
5. Vorschau `/root/sb-tools-b/previews/wa24-1-inverse-trim.html` (Generator `/root/sb-tools-b/wa24-1-gen.py`). Feinkorrektur: `BORDER` und `mix` in `TrimInversion`.

## Verifikation
Gametests smithing_upgrades (1->2->3, 3 bleibt) + trim_inversion (Pixel je Stufe) auf module-simpletrims-fabric-263 + standalone; Compile 26.3 Fabric; Generatoren, `./gradlew check`. Nicht getestet: Client-Sicht.
