# Plan a-blockstex23 (B23-T1 bis T4, Simple Blocks) – 23.10.

Basis claude-wave1 ec9d9c7eb, Branch `a-blockstex23`. Nur MC 26.3 Fabric. Nur exemplarisch: Varianten A/B/C + Vorschau,
nichts ausgeliefert (keine PNG im Repo geändert). Regeln: „UI/Optik erst exemplarisch“, PRINZIPIEN A8 (Ebenen, Vanilla-nah),
AGENTS.md 7 (aus Vanilla abgeleitete Pixel nur beim Build, nie einchecken).

## Ist-Stand
- Bemooste/rissige Ziegel (13 Stück) sind **aus Vanilla abgeleitet** (`tools/textures/missing_blocks_2026_10_16.py`
  `cracked()`/`mossy()`, gebaut über `vanilla_derived.py`, Pfade in `.gitignore`). Moos = 4 feste, recht gesättigte Grüns
  (`MOSS`), gesetzt auf ~80 % aller dunklen Pixel + Flecken → wirkt grell und folgt der Ziegelschattierung nicht.
  Risse = 2 Zufallsdiagonalen mit Faktor 0,52 → harte braune Striche, auf hellen Ziegeln (Endstein, Schwefel) zu kräftig.
- Vanilla zum Vergleich: Moos 5 gedämpfte Oliv-Töne (82,93,57 … 122,143,85), große zusammenhängende Polster, die von den
  Fugen aus über/unter die Ziegel wachsen. Rissige Steinziegel ändern ~120 Pixel: Risslinien **und** verwitterte
  Ziegelflächen (gleiche Basis, Overlay).
- Eigene Texturen: `sculk_deepslate`, `hushed_deepslate_tiles` (modref_concept, 4–6 flache Töne, harte Kanten),
  `chiseled_packed_ice`, `chiseled_blue_ice` (nature_blocks_2026_10_09, 7 Töne, harte Ringe). Gemeißelte SB-Ziegel
  (Astralit/Ender-Quarz/Nihilith) gehören zu SimpleBuilding, nicht zu Simple Blocks → hier nur benannt, nicht angefasst.

## Umsetzung (exemplarisch)
Neuer Generator `modules/simpleblocks/tools/b23_textures.py` (Ebenen per `layers.snap`, eigene Pixel, Vanilla-Pixel nur
als Basis abgeleiteter Kandidaten und in der Vorschau). Schreibt nur `/root/sb-tools-a/previews/a-blockstex23-preview.html`.

- **T1 Moos** (Beispiele: bemooste Endstein-Ziegel, bemooste Tiefenschiefer-Fliesen): Fugen-Erkennung aus der Basis,
  Moospolster wachsen von den Fugen (oben hängend, unten aufsteigend), Ton je Pixel aus der Helligkeit der Basis
  (Moos übernimmt die Ziegelschattierung), weiche Ränder (Mischpixel). A = Vanilla-nahes Oliv (7 Töne), B = A, Moos zur
  Basisfarbe getönt, C = dezent (nur Fugen + kleine Polster).
- **T2 Sculk-Tiefenschiefer**: A = Vanilla-Tiefenschiefer + Vanilla-Sculk-Ader (reine Komposition, abgeleitet),
  B = Vanilla-Tiefenschiefer + eigene Sculk-Adern entlang der dunklen Spalten mit Leuchtpunkten (abgeleitet),
  C = eigener Tiefenschiefer (Schichtung, 9 Töne) + Adern wie B (eincheckbar, Ebenen).
- **T3 Gemeißelt/Fliesen** (Beispiele: gehüllte Tiefenschiefer-Fliesen, gemeißeltes Packeis; Motive bleiben):
  A = eigene Basis mit 9–10 Tönen, Fasen (Licht oben-links, Schatten unten-rechts), weiche Fugen; B = Vanilla-Basis
  (deepslate_tiles / packed_ice) + eingemeißeltes Motiv als Schatten-/Licht-Overlay (abgeleitet); C = A mit tieferem
  Relief bzw. kleinen 4×4-Fliesen wie Vanilla.
- **T4 Risse** (Beispiele: rissige Endstein- und Schlammziegel): gleiche Vanilla-Basis + Overlay-Ebenen „Verwitterung“ und
  „Risse“; Risse beginnen an Fugen, Kern = Fugenton, Schattenpixel + Lichtkante, Ausbrüche an Ziegelecken.
  A = mittel (≈ 60 % der Ziegel), B = Vanilla-Stärke (jeder Ziegel, Verästelung, Ausbrüche), C = feine Haarrisse.
  Ausnahmen bleiben: rissiger Diamantblock, rissiges Eis.

## Nach der Wahl (nicht in diesem Branch)
Gewählte Funktion in `missing_blocks_2026_10_16.py` (`cracked`/`mossy`) bzw. für eigene Texturen in den Generator
übernehmen, alle 13 gealterten Ziegel + alle Fliesen/Gemeißelten nachziehen, `layers.py check`, textures.js, Vorschau.

## Verifikation
- Generator läuft lokal (Pillow), Vorschau geprüft (16×, 3×3-Wand, alt, Vanilla). Keine Repo-PNG geändert (`git status`).
- sbt: `:mc26_3:fabric:compileJava` + `:modules:simpleblocks:fabric:compileJava`, `layers.py check`, `./gradlew check -q`.
- Keine Gametests nötig (keine Spiel-Änderung); keine Client-Läufe.

## Plan-Abgleich
- T1 umgesetzt wie geplant, Abweichung: Moos wächst nur an waagerechten Fugen (hängend, wie Vanilla); an senkrechten
  Fugen ergab es „Stängel“. B = Moos rückt halb zur Helligkeit des Ziegels (statt Farbmischung) – wirkt nicht mehr grell,
  bleibt auf dunklem Tiefenschiefer sichtbar. Empfehlung B.
- T2 umgesetzt (A Komposition, B/C eigene dünne, verzweigte Adern mit Saum + 3 Leuchtpunkten). Empfehlung B.
- T3 umgesetzt an gehüllten Fliesen + gemeißeltem Packeis (Motive bleiben; Relief statt Farbringe). Empfehlung A.
- T4 umgesetzt an Endstein- und Schlammziegeln (Overlay-Ebenen Verwitterung/Risse/Ausbrüche). Empfehlung A.
- Keine Repo-PNG geändert; Vorschau `/root/sb-tools-a/previews/a-blockstex23-preview.html`, Antwortcode
  `B23-T1=B B23-T4=A B23-T2=B B23-T3-Fliesen=A B23-T3-Eis=A`.
