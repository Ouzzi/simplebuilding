# PLAN B-METALS25 (10.11.2026) – N25 Simple Ores Metalle: Nuggets, stapelbare Platten, Sounds, Spiegel am Boden, Gitter/Rost

Auftrag: /root/b-auftrag-20261109.md „N25“ (Besitzer 10.11. ~15:20). Branch `b-metals25`, Basis hub/b-bundle174-gen (7bfe77740).
Vorschau /root/sb-tools-b/previews/b-metals25-preview.html, Antwortcode **METALS25**.

Besitzer-Wortlaut (/root/sb-tools-a/abnahme-a/antworten.md, 10.11. ~15:20): „Nuggets fehlen für Bronze, Edelstahl, Messing. Bleiplatten nicht
stapelbar wie gewünscht (aufeinander stapeln → dicker), Silberplatte ebenso; je Metall eigene Sounds. Spiegel auch auf dem Boden platzierbar. Für die
Metalle Gitter (bars) und Roste (grates), jedes mit eigenem Aussehen, Vanilla-like, aber anderes Muster.“

Ursprüngliche Platten-Anforderung (ORES31-BEXTRA, QUEUE.md Z. 383 / PLAN-B-LEAD02): „ähnlich zu teppich aber vertikal oder horizontal platzierbar,
octets sollen volle dämmung eines bleiblockes besitzen, ähnlich zur wolle logik, und platten 0,5 oder so“. Silber (a-silverlead05, b-auftrag-20261107
Z. 42): „Silberblech als 1-px-Bodenschicht analog zur Bleiplatte; Silberspiegel = Glas + Silberblech“.

## 1. Ist-Stand (erhoben 10.11.)
- Nuggets: silver, lead, zinc, nickel, tin, rose_bronze, rose_gold, enderite. **Fehlen:** brass, stainless_steel, bronze (OresRegistry.MIXES nur Barren + Block).
  Vorhandene Nuggets sind Vanilla-abgeleitet (iron/gold_nugget umgefärbt, beim Build erzeugt).
- `lead_plate` (LeadPlateBlock, FACING 6 Richtungen, 2 px, 3 Barren → 6, soundproofing_half 0,5) und `silver_sheet` (SilverSheetBlock extends LeadPlateBlock, 1 px,
  3 Barren → 12). **Keine Lagen.** Keine weiteren Metallplatten.
- `silver_mirror` (SilverMirrorBlock, nur 4 Wand-Richtungen, Leiter-Logik). Kein Renderer (nur Modell).
- Sounds: Blei `LEAD_SOUND` (Netherit/Amboss, laut + tief, Test `lead_*` prüft Lautstärke > 1, Tonhöhe < 1), alle anderen Metallblöcke = Eisenblock-Kopie (SoundType.IRON).
- Gitter/Roste: keine.

## 2. Entscheidungen (Autonomie, begründet)
1. **Silberplatte = `silver_sheet`** (kein neuer Block): das Silberblech wurde 05.11. ausdrücklich „analog zur Bleiplatte“ gebaut; der Spiegel ist Glas + Blech
   (Deko, kein Platten-Teil). Ein zweiter Silber-Plattenblock wäre eine Dublette.
2. **Lagen 1–8 wie Schnee** (`BlockStateProperties.LAYERS`): Rechtsklick mit derselben Platte auf die freie Seite (die Seite = FACING) → +1 Lage. Je Lage
   Blei 2 px (8 Lagen = voller Block, Schnee-Maß), Silber 1 px (8 Lagen = halber Block). Begründung Silber: 3 Barren ergeben 12 Bleche bzw. 6 Bleiplatten –
   gleiche Metallmenge je Pixel, das Blech bleibt das dünne Blech. Kollision = Hitbox = Form der Lagen (Metall trägt, anders als Schnee kein „einsinken“).
   Drops: Anzahl = Lagen (Loot `match_block` je Lage, wie Schnee; Explosion: survives_explosion). **Migration:** Default `layers=1` – alte Welten haben
   keinen `layers`-Wert und laden mit dem Default, also unverändert (Gametest dekodiert einen alten Zustand ohne `layers`).
3. **Octet-/Stufen-Logik:** Platten sind keine Treppe/Stufe (keine Octet-Pflicht nach PRINZIPIEN). Kein Umwandeln von 4/8 Lagen in Stufe/Block
   (8 Bleiplatten = 4 Barren ≠ Bleiblock 9 Barren; Stufe würde Material vernichten/erzeugen). Volle 8 Lagen Blei bleiben Platte (Drops 8).
4. **Weitere Metallplatten** (Zink, Nickel, Zinn, Messing, Bronze, Edelstahl) werden **nicht neu angelegt**: der Besitzer nannte Blei + Silber; „gilt für alle
   Platten, die es gibt“ – das sind diese zwei. Die Lagen-Logik steckt in der gemeinsamen Klasse (`LeadPlateBlock`), neue Platten wären eine Zeile. → Rückfrage R1.
5. **Dämmung je Lage:** Bleiplatte 1 Lage 0,5 (wie bisher) bis 8 Lagen 1,0 (= Bleiblock), linear (`0,5 + 0,5·(n−1)/7`). Vibrationen bleiben tag-basiert (dünne Platte nein).
6. **Sounds:** eigene SoundEvents je Metall (`simpleores:block.<metall>.break|step|place|hit|fall`, 8 Metalle) – sounds.json verweist auf **Vanilla-Klangdateien
   tonhöhenversetzt** (keine Mojang-Datei im Repo), dazu für Zinn das „Zinngeschrei“ (Knistern beim Biegen) als 2 kurze ElevenLabs-Takes (Mono-Ogg, CREDITS.md).
   Leise: Schritte 0,6–0,8 der Vanilla-Lautstärke. Jeder Block eines Metalls (Speicherblock, Familie, Platte, Gitter, Rost) nutzt das SoundType des Metalls.
   Blei bleibt nach Konzept ORES31 etwas lauter und tiefer (Test bleibt sinngemäß), aber dumpf/weich (Heavy Core statt Amboss).
7. **Spiegel:** `SilverMirrorBlock` wird `FaceAttachedHorizontalDirectionalBlock` (face=floor/wall/ceiling wie der Knopf). Default face=wall → alte Spiegel
   (ohne `face`) bleiben Wandspiegel. Boden/Decke brauchen eine tragende Fläche wie der Knopf. Kein Renderer vorhanden → nur Blockstate/Modell.
8. **Gitter/Rost je Metall** (silver, lead, zinc, nickel, tin, brass, bronze, stainless_steel): `<m>_bars` = `IronBarsBlock` (Kopie Eisengitter), `<m>_grate` =
   `WaterloggedTransparentBlock` (Kopie Kupferrost). Rezepte wie Vanilla: 6 Barren (2×3) → 16 Gitter; Rost 4 Speicherblöcke (Raute) → 4, Steinsäge Block → 4.
   **Keine Oxidation/Wachs:** nur Kupfer oxidiert in Vanilla; Messing/Bronze würden real Grünspan ansetzen, aber eine eigene Alterungskette je Metall ist ein
   starker Eingriff (A12) und R4 (METALS14, Patina) ist noch offen → Rückfrage R2. Tags: `minecraft:bars` (Block+Item), mineable/pickaxe, Rost `needs_stone_tool`
   (wie Kupferrost), Gitter ohne Stufe (wie Eisengitter). Loot: sich selbst. Tab: hinter dem Speicherblock/der Familie des Metalls. Preise: Simple Ores verkauft
   nichts (excluded mit Begründung). Octet-Regel n/a.
9. **Edelstahl/Bronze** (METALS14 R2 offen): N25 Punkt 5 nennt sie ausdrücklich → Gitter/Rost/Nuggets/Sounds ja; die Bau-Familie (cut/tiles/…) bleibt bei R2.

## 3. Texturen (A8c/A8d, eigene Pixel, .layers.json)
Generator `tools/textures/metals25_2026_11_10.py`: Ton-Raster (0 hell … 9 dunkel) je Form, gefärbt mit der Metall-Rampe aus `ores_2026_10_30.RAMPS`.
- Nuggets (3): eigene Form je Metall (Messing: kantiger Barren-Splitter, Bronze: runder Guss-Tropfen, Edelstahl: flache gestanzte Scheibe).
- Gitter (8 × 2 Varianten A/B) und Rost (8 × 2 Varianten A/B), eigenes Muster je Metall (Blei dicke runde Stäbe, Silber feine Ranken, Messing Rauten …).
  Gebaut wird A; B liegt im Generator (Wahl = `PICK` + `--own`).
Runden siehe §6.

## 4. Dateien
`OresRegistry.java` (Nuggets, Gitter, Rost, SoundTypes), neu `OresSounds.java`, `block/LeadPlateBlock.java` (Lagen), `block/SilverMirrorBlock.java` (face),
`sound/Soundproofing.java` (Gewicht je Lage), Fabric/NeoForge/Forge-Einstieg (SoundEvent-Registrierung), `tools/gen_data.py` (Modelle, Blockstates, Loot,
Rezepte, Tags, Lang, Wiki, sounds.json), `tools/check_data.py`, `test/OresTests.java` + Fabric-Adapter, Texturgenerator + PNG/.layers.json, Zinn-Oggs,
`CREDITS.md`, simplemoney `prices.json` (excluded), `.claude/QUEUE.md`.

## 5. Tests
Gametests: `mix_nuggets` (9↔1, Tags), `plate_layers` (Stapeln 1–8 per Item-Benutzung, Form je Lage, Drops = Lagen, Migration ohne `layers`), `mirror_faces`
(Boden/Wand/Decke, Halt, alter Zustand = wall), `bars_and_grates` (16 registriert, Rezepte Werkbank+Steinsäge, Tags, Drops), `metal_sounds` (SoundType je Metall,
Events registriert), `registered` (Zählung), `lead_plate_soundproofing` (Gewicht je Lage). Lokal: gen_data --check, check_data simpleores/simplemoney,
check_tooltip_text, check_missing_textures, module_guides --check, precheck --fast, layers check.
sbt (ein Lauf): Compile Kern + simpleores, module-simpleores fabric+standalone, `fabric-263 --filter 'simplebuilding:data_integrity*'`, module-simplemoney, Wiki-Check.

## 6. Textur-Runden (Bewertung, A8c)
Werkzeug: `python3.12 tools/textures/metals25_2026_11_10.py --rounds DIR` (jede Runde/Variante/Metall, Härte nach A8b); Kontaktbogen 6× nearest neben
Vanilla iron_bars/copper_bars/copper_grate (26.3-Jar), dazu 3×3 in Spielgröße vor Himmel. Vanilla: Eisengitter 30 % hart, Kupfergitter 10 %, Kupferrost 3 %,
Eisen-/Goldnugget 52/45 %.

| Runde | Bewertung | Änderung danach |
|---|---|---|
| R1 | Kanten 2/6, volle Rampe: Gitter 40–60 % hart, Silber-Ranken = Kratzer, Blei-Stäbe = Bretter, Edelstahl-Rost B (Schachbrett) flimmert 45 %, Messing-Nugget = Banane, Bronze = Kakaobohne | Kanten 3/6, Rampe auf Speicherblock-Helligkeit (Spannen wie METALS14), neue Nugget-Formen |
| R2 | Gitter 12–48 %, Roste 0–1 %; Silber-Rost A Filigran (Loch mit Blüte) eigenständig; Roste zu blass; Messing-Rost A wieder Kupfer-Kreuze; Messing-Nugget Goldkugel, Bronze Zwiebel | Roste eine Stufe dunkler, Messing-Rauten, Blei-Glanzmitte, Nugget-Formen R3 |
| R3 | Roste gut lesbar, Messing-Rost A Rauten, Blei-Stäbe rund; Gitter mit 3/5 flach und blass (0 % hart) | Gitter zurück auf 3/6, Silber-S-Ranken, Messing-Nugget Sechskant |
| R4 (gebaut A) | Gitter 12–48 % (Zinn A wegen der Wellen am höchsten, Eisengitter 30 %), Roste 0,5–17 %; jedes Metall eigenes Muster, 1× und 3×3 lesbar; Nuggets: Messing Sechskant-Brocken, Bronze Guss-Tropfen, Edelstahl flacher Span | – |

Muster (A gebaut / B Alternative): Silber S-Ranken|Ringe, Filigran-Löcher|Vierblatt; Blei dicke runde Stäbe mit Kragen|mit Mittelband, 4 große Quadrate|Schlitze;
Zink Zickzack-Streben|Leitersprossen, Diagonalschlitze|Waben; Nickel Nietbänder|Doppelstäbe, 1-px-Lochung|Kreuze; Zinn gebogene Stäbe|Rautengitter, Wellenschlitze|Dreiecke;
Messing Rauten|Rautenknoten, Rautenlöcher|Rautengitter; Bronze Rundbögen|Gussringe, Bullaugen|Bogenschlitze; Edelstahl Flachschienen|Quadratnetz, Lüftungsschlitze|2-px-Netz.

## 7. Abweichungen während der Umsetzung
- `module_guides.py` lief lokal (versehentlich ohne `--check`, schreibt nur simpleores: OresGuide 1 Zeile, Lang, Modpack-Guide, manual.json); Wiki/Discover werden im sbt
  erzeugt und als `b-metals25-gen` gepusht.
- Wiki-Notizen der METALS14-Familie und der Bleiplatte waren falsch („neun Barren in einem Block“) → eigene Texte (gen_data `note_summaries`).
- Vorhandene Silber-/Blei-Speicherblöcke bekommen den Metall-Klang (Familie erbt ihn); Silberspiegel/-blech klingen wie Silber.
- Preise: Simple Ores verkauft nichts → neue Teile als `excluded` mit Begründung in simplemoney `prices.json`.

## Rückfragen (Besitzer)
- R1: Platten auch für Zink/Nickel/Zinn/Messing/Bronze/Edelstahl anlegen? (Empfehlung: nur bei Bedarf; Klasse kann es schon)
- R2: Grünspan/Oxidation für Messing/Bronze-Gitter und -Rost (Wachs wie Kupfer)? (Empfehlung: nein, wie METALS14 R4)
