# Plan a-misctex23b (W23-4) – 23.10.

Basis: Bündel 50 + claude-wave1 (eeb2e22c5). Nur MC 26.3 Fabric. Vorgänger: `PLAN-A-MISCTEX23-2026-10-23.md`
(B23-T10..T13, A1) und `PLAN-A-TEX23-2026-10-23.md` (Butter/Piggy/Laub). Grundsatz des Besitzers: „alles wirkt harsch“ →
weichere Übergänge, keine reinen Weiß-/Schwarz-Spitzen, kleinere Tonsprünge. Ein Generator
`tools/textures/misctex23b_2026_10_23.py` (Pillow, Ebenen Basis/Overlay/Details, `--write` schreibt die eingebauten
Texturen + `.layers.json`, `--page` die Vorschau `/root/sb-tools-a/previews/a-misctex23b-preview.html` mit den Teilen
„Eingebaut“ und „Wahl offen“).

## Punkte

| Punkt | Besitzer | hier |
|---|---|---|
| Notenblock / Noten-Verstärker / Jukebox-Verstärker | 90°-drehsymmetrisch: Jukebox-Rahmen rundum, Mitte Notenblock-Muster (180° erlaubt), Noten-Verstärker + Notenblock schwächer | 3 Varianten (A 4 Ringe + 8×8-Gitter, B 3 Ringe + 10×10-Gitter, C = A + Nieten + Membran-Kern); **A eingebaut** für beide Verstärker; Notenblock nur über neue Client-Option |
| Simple Money | verworfen | nichts |
| Verkaufslager + Lager-Werkbank | B weicher (v. a. Verkaufslager) | B1 (gleiche Form, weiche Töne) / B2 (Holzbeschläge statt Eisen, Verläufe); **bessere eingebaut** |
| Rucksack | B verfeinern, im Uhrzeigersinn gedreht wie das Bündel, weicher | 3 neue Vorschläge D/E/F, nur Vorschau |
| Animationen A1 | einbauen wie empfohlen (A „atmen“) | alle Geschwister: Scraps (Ober-/Nether-/End, Seite + oben), Tiefenschiefer-Salbei-Erz, Karmesin/Wirr-Lohenholz-Bretter + -Regale |
| Butter innen | heutige bleibt, 3 neue Versuche (Vanilla-nah, cremig, weich) | X/Y/Z nur Vorschau |
| Piggy-Effekt | Kopf frontal, Vanilla-näher, Pupillen außen, lustiger | P1/P2/P3, **empfohlene eingebaut** |
| Aschholz-Laub locker | L1 (~30 %) | `gen_textures.leaves_loose` für Aschholz = L1 (Dämmerholz bleibt) |

## Notenblock-Override (Begründung)
Der Vanilla-Notenblock ist ein Vanilla-Block; ihn ungefragt umzufärben ändert das Spiel für jeden, der nur die Verstärker
will, und kollidiert mit Ressourcenpaketen. Darum: neue Client-Option `noteBlockFrameTexture` (Standard **aus**, Gruppe wie
`jukebox264Textures`). Die Textur `simplebuilding:block/note_block_framed` wird wie die Verstärker beim Build aus dem Jar
abgeleitet (Palette des Vanilla-Notenblocks, eigene Pixel, nie eingecheckt) und per `NoteBlockFrameSpritesMixin` nur bei
Option an **und** Notenblock aus dem Vanilla-Paket unter `minecraft:block/note_block` in den Block-Atlas gelegt (F3+T).

## Technik
- Verstärker: `music_disc_textures.outputs()` nimmt die Blöcke aus `misctex23b.amplifier_textures(note)` statt
  `speaker_settled` (Datei bleibt als Geschichte). Eine Textur für alle 6 Seiten bleibt (`SAME_AS` gültig), jetzt exakt
  90°-drehsymmetrisch (Rahmen) mit 180°-symmetrischem Gitter. Neuer Ausgang `block/note_block_framed.png` in
  `vanilla_derived.OUTPUTS` + `.gitignore`.
- Verkaufslager: `simple_shops_storage_2026_10_22.py` schreibt nur noch das Vitrinenglas; Lager-Werkbank bekommt ihre
  Ebenen-Quelle aus dem neuen Generator.
- A1: 16 Bilder, `interpolate`, `frametime` 4 (`.png.mcmeta`), Ruhebild = abgenommene Textur, nur Leuchtpixel atmen.
- Laub: `gen_textures.loose_cover()` (aus `tx22_textures.leaves_loose_alt` verschoben), Aschholz 30 %.

## Verifikation
- Lokal nur Vorschau (Generator ohne `--write`), Symmetrie-Assert im Generator (Rahmen 90°, Gitter 180°).
- sb-test (`/root/sbt a-misctex23b`): `--write`, `layers.py check`, `vanilla_derived.py --list`, guides, wiki `--all`,
  discover, `wiki/data/textures.js` → `-gen`; Compile `:mc26_3:fabric:compileJava` + betroffene Module; Targets
  module-{simplemusic?,simpleshops,simplestorage,simplesandwiches,simplefun,simpleend}-fabric-263 (+ standalone) und
  fabric-263 (Filter ConfigOption*), `./gradlew check -q`.
- Nicht: Client-Lauf (Animation/Option im Spiel ungesehen), andere Loader.

## Plan-Abgleich
(wird nach der Umsetzung ergänzt)
