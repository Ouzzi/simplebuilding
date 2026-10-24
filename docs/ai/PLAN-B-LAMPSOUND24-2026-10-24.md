# Plan B-LAMPSOUND24 (2026-10-24): Lampen-Klaenge Astral/Nihil neu (Queue SP22-5b)

Branch `b-lampsound24`. Besitzer 24.10. abends: "Astral- und Nihil-Lampen-Sounds ueberarbeiten." Hintergrund AUD-1: eigene
Synth-Klaenge "zu grell, synthetisch, zu hell, ohne Tiefe".

## Ist-Zustand
- `LineSounds.ASTRAL_LAMP` / `NIHIL_LAMP` (SoundType: break/place = `dig1-4`, step/hit/fall = `step1-4`) und
  `lampToggle` (`toggle_on` / `toggle_off`, ruft `EndSignalLampBlock`). Dateien: 26.3-Overlay
  `sounds/block/{astralit_lamp,nihilith_lamp}/*.ogg`, Ereignisse in der Overlay-`sounds.json` (aus
  `tools/sounds/make_redstone_line_sounds.py`, Synth: Kristall mit f=1400 Hz*Tint, Astral 1,18 / Nihil 0,82).
- 10 Dateien je Lampe, mono 44,1 kHz Vorbis. Vanilla-Pegel (aktiver RMS): Amethyst-Abbau -26 dB, Kupferbirne -29 dB.

## Umsetzung
1. Je Lampe vier Klang-Gruppen: setzen/abbauen (dig, 4 Takes), Schritt (step, 4 Takes), an, aus.
2. Je Gruppe 3 Kandidaten: A und B per ElevenLabs Sound-Effects-API (kurz, tief, warm; keine Kuenstlernamen; Key nur aus
   `sb-keys.env`, nie ausgegeben; Budget <= 3000 Credits), C = verbesserter Generator (tiefer: Grundton ~800 Hz statt
   1400, weicher Anschlag, kuerzeres Ausklingen, Tiefpass 4,5 kHz Astral / 2,2 kHz Nihil, Nihil mit mehr Koerper).
3. Alles per ffmpeg: Hochpass, Tiefpass, Pegel auf Vanilla-Aktiv-RMS (dig -26, step -33, toggle -29 dB), kurz geschnitten
   (Fade-out), mono OGG Vorbis. Rohdateien `/root/sb-tools-b/lampsound24/` (nicht im Repo).
4. Empfohlener Kandidat ersetzt die alten OGGs (gleiche Dateinamen, `sounds.json` unveraendert). Generator bekommt
   `--lamp keep` (Lampen-Dateien nicht anfassen) und die verbesserte Synth-Variante `lamp()` bleibt reproduzierbar.
5. Vorschau `/root/sb-tools-b/previews/lampsound24-preview.html` (data:-URI, alt + 3 Kandidaten je Klang, Vanilla nur als
   Beschreibung).

## Lizenz
ElevenLabs Creator-Abo (Output gehoert dem Nutzer, kommerziell ok, Befund `/root/sb-tools-a/eleven23/LISTE.md`); Prompts ohne
Kuenstlernamen. Prompts stehen in `/root/sb-tools-b/lampsound24/credits.tsv`. Synth-Kandidat C: eigener Code, keine Samples.

## Risiken / Pruefung
Hoerprobe nur durch den Besitzer. Pegel/Spektrum per ffmpeg gemessen. Tests: `simplebuilding:redstone_lines*`,
`end_systems*` (fabric-263), `tools/check_missing_textures.py`, `./gradlew check -q`.
