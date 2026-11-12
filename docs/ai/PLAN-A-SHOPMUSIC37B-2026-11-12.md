# Plan A-SHOPMUSIC37B (12.11.2026): alle 9 Takes – A-/B-Seiten + vierte Laden-Platte

Branch `a-shopmusic37b` (Basis `hub/b-bundle188` 90fb9d8b6, enthält a-shopmusic37 f0e296e32), Auftrag
`/root/sb-tools-a/agenten-1530.md` §13b. Besitzer 12.11. ~19:10: „SHOPMUSIC37=einwand: alle Takes nutzen, mit B-Seite der Platte;
da es nur A und B gibt, weitere Platte erstellen und Rest kombinieren.“ Antwortcode **SHOPMUSIC37b**. Nur 26.3 Fabric.
Urheberrecht wie a-shopmusic37 (`docs/ai/PLAN-A-SHOPMUSIC37-2026-11-12.md`): keine neuen Generierungen nötig, nur die 9
vorhandenen ElevenLabs-Takes (Prompts nur Genre/Stimmung + eigene Melodie-Kontur), kein Werk-/Markenname.

## Ist-Stand (erhoben)
- **Rohtakes:** `/root/sb-tools-a/shopmusic37/raw/d{1,2,3}{a,b,c}_*.mp3` (je 72 s, 192 kbit), verarbeitet in `ogg/` (process.py:
  Stille am Ende weg, 2 s Überblendung Ende→Anfang, −17 LUFS, ≤ −1,2 dBTP, mono OGG q3). Eingebaut sind 1c, 2a, 3a.
- **A/B-Seiten-Mechanik:** in Simple Music **keine**. SimpleBuilding hat eine (`util/DiscFlips`, `util/MusicDiscs`,
  `docs/ai/PLAN-SCHALLPLATTEN-2026-10-03.md`): je Seite ein eigenes Item `music_disc_<name>_b_side` mit eigenem Song/Sound,
  Etikett invertiert; gewendet wird die **abgelegte** Platte mit dem **Vorschlaghammer** (beides SB). Simple Music muss allein
  spielbar bleiben (PRINZIPIEN-MODUL-UNABHAENGIGKEIT) → SBs Datenmodell übernehmen, das Wenden selbst eigenständig lösen.
- **Shop-Synchronität** (Simple Shops `ShopMusic.startFor`, `ShopMusicPick.Shop#sameGroup`) gruppiert nach **Song-ID**
  (Sound-Event des `jukebox_song`), nicht nach Item → eine Seite = ein Song = eine eigene Gruppe. Kein Code nötig.
- Messung der Takes (Tonart per Chroma/Krumhansl, Tempo per Onset-Autokorrelation, Pegelverlauf):
  1a/1b/1c G-Dur 100 BPM; 2a B-Dur, 2b d-Moll/F, 2c F-Dur (2a/2c ~120 bzw. Swing); 3a A-Dur, 3b C-Dur 108, 3c G-Dur 107.
  1b, 2b, 2c haben 6–8 s Ausblendung am Ende; die bisherige Schleife überblendet diese Ausblendung in den Anfang (Naht −7 dB).

## Entscheidungen
1. **Seiten = eigene Items** wie in SimpleBuilding: `simplemusic:music_disc_<id>_b_side`, Song `<id>_b_side`, Sound
   `music_disc.<id>_b_side`, OGG `records/<id>_b_side.ogg`, Titel „Simple Music - <Name> (B-Side)“ / „(B-Seite)“, gleicher
   Komparatorwert wie A. Vorteil: jede Jukebox, die Taschen-Jukebox, Simple Shops und fremde Mods spielen die B-Seite ohne
   Sonderfall (Vanilla-Komponente `jukebox_playable`), Tooltip zeigt Vanilla-typisch den Titel mit „(B-Side)“.
   B-Seiten nie im Loot und nicht beim Händler (wie SB): man bekommt sie nur durch Umdrehen.
2. **Umdrehen (Vanilla-nah, ohne Bildschirm-Text):** Rechtsklick mit der Platte in der Hand (in die Luft oder auf einen Block
   ohne eigene Aktion) dreht sie um: A → B → A. Hand schwingt, leiser Dreh-Klang (`item_frame.rotate_item`, Tonhöhe 1,2),
   0,5 s Abklingzeit (gegen Dauer-Wenden bei gehaltener Taste). Jukebox/Shop: Platte herausnehmen, umdrehen, wieder einlegen
   – wie bei einer echten Platte. Kein Mixin, kein Eingriff in Vanilla-Blöcke; nur Simple-Music-Platten mit B-Seite.
   Verworfen: Wenden in der Jukebox (Mixin am Vanilla-Block, A12 stärkerer Eingriff), Wenden per Klick im Shop-Platz
   (ändert Vanillas Slot-Bedienung), Vorschlaghammer (gehört SimpleBuilding, Simple Music allein spielbar).
   Neue Klasse `disc/SidedDiscItem` (Unterklasse von `Item`, nur `use`).
3. **Take-Verteilung (alle 9):**
   | Platte | A-Seite | B-Seite |
   |---|---|---|
   | Stöber-Bossa (Browsing Bossa) | 1c Flöte+Vibraphon (unverändert) | 1b E-Piano + Vibraphon-Antworten |
   | Münz-Lounge (Coin Lounge) | 2a E-Piano (unverändert) | 2b Vibraphon und E-Piano im Wechsel |
   | Schaufensterbummel (Window Shopping) | 3a Marimba+Vibraphon (unverändert) | 3b E-Piano + Xylophon |
   | **neu: Wühltisch (Bargain Bin)** | 2c Jazzgitarre + E-Piano (ruhig, warm) | **Medley** 1a Vibraphon-Bossa → 3c Flöte+Marimba |
   Begründung: B-Seiten = der klanglich andere Take derselben Platte (anderes Leitinstrument als A); die drei Reste auf eine
   vierte Platte. Das Medley verbindet die beiden Reste in **derselben Tonart (G-Dur)** mit fast gleichem Tempo (100/107 BPM →
   beide per Rubberband auf ~103,4 BPM, ±3,4 %, Tonhöhe bleibt) – so passt die Überblendung harmonisch; 2c (F-Dur) bleibt allein.
   Name „Wühltisch“: Restposten der Laden-Musik, passt zum Shop-Thema. Etikett „Preisschild“.
4. **Schleifen der neuen Seiten** (Abweichung, bewusst: „nahtlos wie bisher“, aber besserer Schnittpunkt): Start auf dem ersten
   Taktanfang, Ende = ganze Zahl Takte **vor** der Ausblendung, Feinabgleich ±60 ms per Onset-Korrelation mit dem Anfang,
   1,5 s Überblendung (gleiche Leistung). Danach unverändert: Hochpass 28 Hz, Höhen −3 dB ab 6 kHz, −17 LUFS, ≤ −1,2 dBTP,
   mono OGG 44,1 kHz q3 (Medley ggf. kleineres q, damit ≤ 1 MiB). Die drei A-Seiten bleiben bitgleich (Besitzer hat sie gehört).
   Medley: Takt-Phasen beider Takes ausgerichtet, Pegel angeglichen, Übergang P→Q und Q→P (Schleife) je 1 Takt Überblendung.
5. **Vierte Platte** wie a-shopmusic37: Truhe Wüsten-Dorfhaus 4 %, fahrender Händler 12 Smaragde (Tag `uncommon`), Komparator 7,
   kein Rezept; Textur im Generator (`gen_textures.py`, Etikettform `tag`), Ebenen, texrate-Runden.
6. **B-Texturen:** gleiche Platte, Etikett-Töne Haupt ↔ Dunkel getauscht („invertiert“ wie SBs B-Seiten), Licht bleibt;
   Vinyl/Körper gleich. Bewertung je Runde mit `tools/textures/texrate.py` (Ziel grün, Härte ≤ ~12 % wie die A-Seiten).
7. **Simple Shops:** kein Logikwechsel (Gruppe = Song); Gametest `shop_music_per_side` (gleiches Item mit anderem Song → eigener
   Start; anderes Item mit gleichem Song → im Takt), Doku/Wiki „je Seite“.

## Dateien
- simplemusic: `disc/MusicDiscs.java` (Disc + `bSeconds`, B-Items, `otherSide`), neu `disc/SidedDiscItem.java`,
  `registry/MusicRegistry.java` (Register, Kreativ A+B), `press/MusicViewer.java` (Info je B-Seite, Umdrehen als
  „Besondere Wege“), `jukebox_song/*` (+5), `sounds.json` (+5), `sounds/records/*.ogg` (+5), `items/` + `models/item/` (+5),
  Texturen + Ebenen (+5), Händler-Handel + Tag, Lang EN/DE, `tools/check_data.py`, `tools/gen_textures.py`,
  Tests `MusicTests` (music_discs, music_disc_loot, neu music_disc_sides), `MusicJeiTests`, Wiki `manual.json`, Handbuch neu.
- simpleshops: Test `shop_music_per_side`, Javadoc `ShopMusic`, Wiki `shop_music`.
- `CREDITS.md`, `docs/MODS-UND-FEATURES.md`.
- Audio-Werkzeug außerhalb des Repos: `/root/sb-tools-a/shopmusic37b/` (`loops.py`, Vorschau-Bau).

## Verifikation
- Audio: je Seite Länge, LUFS, Spitze, Naht (Pegel letzte/erste Sekunde), Takt-Kontinuität über die Naht (Onset-
  Autokorrelation an der Naht vs. Stückmitte), Dateigröße ≤ 1 MiB, mono.
- Lokal: `precheck.py --fast`, alle `modules/*/tools/check_data.py`, `check_dup_impl`, `check_tooltip_text`,
  `wiki/generate.py --all --check`, `check_missing_textures`, `layers.py check`, texrate der 5 neuen Texturen, javac der
  geänderten Klassen gegen 26.3.
- sb-test (wenn erreichbar, sonst `/root/sb-tools-a/a-shopmusic37b-sbt.cmd` + TODO): Compile simplemusic/simpleshops Fabric,
  `module-simplemusic-fabric-263`, `module-simplemusic-standalone-fabric-263`, `module-simpleshops-fabric-263`,
  `module-simpleshops-standalone-fabric-263`, JUnit simpleshops.
- Nicht ohne Client prüfbar: Hörbild im Spiel, Dreh-Klang, Schwung → Vorschau mit allen Seiten (Audio inline) + Abnahme.

## Umsetzung / Plan-Abgleich (12.11.)
- [x] Seiten als eigene Items (`MusicDiscs.Disc#bSeconds`, `bSide`, `otherSide`, `turnOver`, `allItems`), `SidedDiscItem`
  (Rechtsklick dreht um, 10 Ticks Abklingzeit, `item_frame.rotate_item` 0,6/1,2), Kreativ A + B, JEI: Infoseite je B-Seite +
  „Besondere Wege“ `simplemusic/turn_<id>` (A rein, B raus, kein Werkzeug).
- [x] Takes wie geplant: 1b/2b/3b = B-Seiten, Wühltisch A = 2c, B = Medley 1a → 3c. Werkzeug `/root/sb-tools-a/shopmusic37b/loops.py`.
  Längen: 52,8 / 48,0 / 53,33 / 48,0 / 120,64 s; Naht-Pegel −0,1 … +2,2 dB, Rhythmus-Treffer 0,83–0,91, Harmonie 0,92–0,99
  (Medley-Übergänge 0,56/0,70 bzw. 0,95/0,83); alle mono 44,1 kHz q3, ≤ 948 KB (das Medley passt auch mit q3 unter 1 MiB).
- [x] Wühltisch: Wüsten-Dorfhaus 4 %, Händler 12 Smaragde (`uncommon`), Komparator 7; Textur „Preisschild“ Limette auf Pappkarton.
- [x] Texturen 3 Runden (B-Regel Haupt↔Hell statt Haupt↔Dunkel; Wühltisch: „Sale“-Form 13,3 % hart verworfen, Creme ohne
  sichtbare B-Seite verworfen, Rot vs. Limette → Limette); alle 5 texrate grün (Härte 10,4–11,8 %), Ebenen über den Generator.
- [x] Simple Shops: keine Logikänderung, Gametest `shop_music_per_side`, Javadoc + Wiki „je Stück/Seite“.
- [x] Tests Simple Music: `music_discs` (10 Platten, 4 B-Seiten: Item, Song, Länge, Komparator, Sound, OGG, Paarung),
  `music_disc_loot` (Wühltisch-Fundort + Händler, nie eine B-Seite im Loot), neu `music_disc_sides` (umdrehen hin/zurück, Name
  bleibt, Abklingzeit blockt, einseitige/Vanilla-Platte bleibt, Jukebox spielt den B-Song), `jei_press_and_copy` (Infoseiten + Umdrehen).
- [x] `check_data.py` (Regex mit B-Länge, B-Titel „(B-Side)/(B-Seite)“, kein B-Handel), Lang EN/DE, Wiki (`music_discs`, Notiz
  `music_disc_*_b_side`, generische Notiz berichtigt: Laden-Platten nicht „nur im Bauwerk“), Handbücher neu, CREDITS, MODS-UND-FEATURES.
- Abweichung: `tools/dup_pairs.txt` – die Namensgleichheit SB `util/MusicDiscs` ↔ Simple Music `disc/MusicDiscs` (zwei verschiedene
  Klassen) meldet neue Methoden als „Drift“; Allow-Liste um die neuen Namen ergänzt.
- Abweichung: Simple-Shops-Handbuch 20 → 21 Seiten (längerer Absatz, Generator).
- Nicht gemacht: sb-test nicht erreichbar → `/root/sb-tools-a/a-shopmusic37b-sbt.cmd`; javac der geänderten Klassen gegen 26.3:
  keine Fehler in eigenen Dateien (nur bekanntes Access-Widener-Rauschen in `MusicRegistry`/`ShopsRegistry`/`LibMaterials`/Tests mit
  `BlockPlaceContext`, unverändert). `wiki/generate.py --all --check` meldet `wiki/data/simplebuilding.json` (Basis-Drift: lokal fehlt
  `build/wiki-lines`) und `textures.js` (Änderungsdatum je Git-Stand) – im Bündel-Generatorlauf neu erzeugen.
- Nicht ohne Client prüfbar: Dreh-Klick/Schwung, Hörbild der Seiten im Shop → Vorschau `/root/sb-tools-a/previews/a-shopmusic37b-preview.html`.
