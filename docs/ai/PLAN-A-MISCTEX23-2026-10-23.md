# Plan a-misctex23 (B23-T10..T13, B23-A1) – 23.10.

Basis claude-wave1 ec9d9c7eb, Branch `a-misctex23`. Nur MC 26.3 Fabric. Textur-Branch: **exemplarisch, nichts eingebaut**
(„UI/Optik erst exemplarisch“, PRINZIPIEN A8). Taschen-Jukebox (a-pjuke23) nicht angefasst.

## Ist-Stand
- **Verstärker** (`simplebuilding:jukebox_amplifier`, `note_amplifier`): Vanilla-abgeleitet, beim Build aus dem 26.3-Jar
  (`music_disc_textures.derived()` → `speaker_settled_2026_10_04.py`: Notenblock, Löcher radial Astralit/Nihilit, eine
  Textur für alle 6 Seiten), nie eingecheckt (`.gitignore`). Neue Jukebox = 26.4-Snapshot-4 (b-jukebox264, Cache
  `~/.gradle/caches/simplebuilding/jukebox264/26.4-snapshot-4/`): Seite = Bretter-Rahmen rundum, Mitte vertieftes Gitter.
  Der Vanilla-Notenblock hat bisher keinen Override.
- **Simple Money** (`modules/simplemoney/.../item`): special_fiber (dünne Lila-Linie), blank_note / refined_blank_note /
  raw_bill (fotoartig, viele Farben); money_bill bleibt (Besitzer).
- **Verkaufslager** (`simpleshops:sales_storage_side/_intake/_back`, Generator `simple_shops_storage_2026_10_22.py`) und
  **Lager-Werkbank** (`simplestorage:storage_crafting_table_top/_front/_side`, ohne Generator, Unterseite oak_planks).
- **Rucksack-Items** (`simplestorage:item/*backpack*`): füllen 16×16 als flache Truhe; Bündel (Vanilla + SB) = runder Sack.
- **Animationen**: dimensional_scrap*, deepslate_sage_ore, crimson/warped_blazewood_planks + _bookshelf unbewegt;
  Vorbild blazing_obsidian (16 Bilder, interpoliert, `.png.mcmeta`).

## Umsetzung (nur Kandidaten)
Generator `tools/textures/misctex23_2026_10_23.py` (Pillow): zeichnet je Textur in benannten Ebenen (Basis/Overlay/Details,
`layers.from_layers`), schreibt PNG + `.layers.json` (+ `.png.mcmeta` bei Animationen) **außerhalb des Repos** nach
`/root/sb-tools-a/previews/misctex23/` und die Vorschau `/root/sb-tools-a/previews/a-misctex23-preview.html`
(16×, 3×3-Wand, Würfel-Kontext, alter Stand, Vanilla-Vergleich nur dort, GIFs, Antwortcode).
- **T10** Jukebox-Verstärker A (26.4-Rahmen + bisherige Verstärker-Mitte, Empfehlung) / B (26.4-Gitter, Löcher radial
  Astralit) / C (A + Astralit-Nieten). Notenblock + Noten-Verstärker: derselbe Rahmen, Relief 50 % (A, Empfehlung) /
  30 % (B) / 70 % (C), Mitte = eigenes Gitter.
- **T11** Spezialfaser A Strang / B Kordel / C Spule (Amethyst + Silber + Gold = „Metall- und Amethystfaser“);
  Rohling A schlicht / B leerer Druckrahmen / C Büttenrand; veredelt A Sicherheitsfaden / B + Glanz + Goldfolie /
  C + Wasserzeichen; Rohschein A nasse Tinte (Druckbild des Geldscheins) / B verlaufen / C Tropfen. Umriss = Geldschein.
- **T12** beide Blöcke je A Eiche+Eisen+Messing (Empfehlung) / B dunkles Fichtenholz / C Eiche+Smaragd.
- **T13** Leder-Rucksack im Bündel-Maß: A frontal (Klappe, Schnalle, Tasche, Griff) / B + Seitenriemen / C + Deckenrolle.
- **A1** je ein Beispiel (dimensional_scrap_side, deepslate_sage_ore, crimson_blazewood_planks, warped_blazewood_bookshelf):
  abgenommene Textur unverändert als Ruhe, nur Leuchtpixel (Kristalle/Erz/Glutfugen) bewegen sich: A atmen je Bereich,
  B Welle, C Funkeln. Regal: Unterkanten der Regalbretter glühen kurz auf (neu, Ruhe = alt).

## Nach Besitzer-Wahl (offen)
- T10: `speaker_settled` bzw. `music_disc_textures.outputs()` auf die gewählte Variante umstellen (braucht die 26.4-Seite
  aus dem jukebox264-Cache im `vanilla_derived`-Schritt; ohne Cache alter Stand). Notenblock: Vanilla-Override
  `minecraft:block/note_block` per Sprite-Mixin wie `JukeboxSnapshotSpritesMixin` + Schalter (visuals).
- T11/T12/T13: gewählte Variante in die Modul-Texturen schreiben (+ `.layers.json`), T13 danach verstärkt/Netherit/Enderit
  + `_dyed`/`_dyed_overlay` in derselben Form (Vorschau), Platzier-/Trage-Modell bleibt.
- A1: `.png.mcmeta` + Streifen für alle Geschwister (nether/end scrap, sage_ore, warped planks, crimson bookshelf),
  `layers.py check`, `wiki/data/textures.js`.

## Verifikation
- Lokal: Generator läuft (54 Kandidaten), `layers.py check` auf Stichproben grün.
- sb-test (`/root/sbt a-misctex23`): `:mc26_3:fabric:compileJava`, guides/wiki/discover ohne Diff, `./gradlew check -q`.
- Keine Gametests nötig (keine Spiel-Änderung); keine Client-Läufe.

## Plan-Abgleich
- T10 ✔ Vorschau (3 + 3 Varianten), Wahl offen. T11 ✔ 4 Items × 3. T12 ✔ 2 Blöcke × 3 Flächen × 3. T13 ✔ 3.
  A1 ✔ 4 Beispiele × 3 (GIF). Nichts eingebaut (Abweichung: keine – so beauftragt).
