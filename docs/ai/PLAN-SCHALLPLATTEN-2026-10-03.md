# Plan: Dimensions-Schallplatten, B-Seiten per Vorschlaghammer, Lautsprecher (2026-10-03)

Queue: `.claude/QUEUE.md` Nachtrag 2 („Eigene Schallplatte je Dimension“) und Nachtrag 6 (Alternativ-Track,
Lautsprecher). Branch `claude-audio`. Flag `McVersion.MUSIC_DISCS` (26.3 `true`, 26.2 `false`) für alles hier.

## Ist-Zustand (erhoben)
- 26.3-Jukebox ist datengetrieben: Registry `minecraft:jukebox_song` (`sound_event`, `description`, `length_in_seconds`,
  `comparator_output`); Items tragen `jukebox_playable` (`Item.Properties#jukeboxPlayable(ResourceKey)`, auch in 26.2).
- Vanilla spielt die Platte clientseitig: `LevelEventHandler#playJukeboxSong` → `SimpleSoundInstance.forJukeboxSong`
  (Lautstärke 4,0, linear → Hörweite 4 × 16 = 64 Blöcke). Das Start-/Stopp-Ereignis (1010/1011) schickt der Server
  nur an Spieler im Umkreis 64 (`ServerLevel#levelEvent`).
- Notenblock: `NoteBlock#triggerEvent` → `Level#playSeededSound(..., RECORDS, 3.0F, pitch, seed)` serverseitig
  (Reichweite 3 × 16 = 48). Lautstärken > 1 heben nicht den Pegel (OpenAL klemmt auf 1), sondern die Reichweite
  der linearen Abschwächung – beim Spieler in fester Entfernung ist es damit lauter und weiter hörbar.
- Abgelegte Kleinteile: `PlacedSmallParts` + Tag `simplebuilding:placeable_small`; Platten stehen bisher nicht darin.
  Vorbild für „Aktion am Häufchen“: `ShulkerShells` (`PlacedSmallPartsBlock#useItemOn`, Hinweis in
  `TransformTargets`, Export `InWorldTransformations` → `InWorldRecipeCatalog` → JEI/REI/Wiki).
- Loot: `ModLootTableModifications#apply` → Datagen schreibt Inject-Tabellen (`LootInjection`), Wiki liest die Datei
  (`wiki/obtain_sources.py`). Server-Config für Truhen/Konfig-Schalter (`enableLootTableChanges`).
- Kein ffmpeg auf dem Rechner. Ersatz: Python-Paket `soundfile` (libsndfile 1.2.2: OGG/Vorbis schreiben, MP3/WAV lesen).

## A) Vier Dimensions-Platten
| Dimension | Item | Titel (Künstler „SimpleBuilding“) | Stil (nur Anlehnung) | Komparator | Fundort |
|---|---|---|---|---|---|
| End | `music_disc_voidline` | Voidline | Nu-Metal/Alt-Rock instrumental | 14 | End-Stadt-Truhe 4 % |
| Oberwelt 1 | `music_disc_driftwood` | Driftwood | melancholischer Hip-Hop instrumental | 6 | Waldanwesen-Truhe 5 % + Creeper |
| Oberwelt 2 | `music_disc_daybreak` | Daybreak | melodic progressive house | 12 | Antike Stätte 2,5 % + Creeper |
| Nether | `music_disc_brimstone` | Brimstone | Hard Rock instrumental | 13 | Bastion (übrige Truhen) 4 % |

- Beschreibung wie Vanilla: `jukebox_song.simplebuilding.<titel>` = „SimpleBuilding - Voidline“; Item-Name „Music Disc“
  / „Schallplatte“. Titel werden (wie Vanilla) nicht übersetzt. Seltenheit RARE (wie Pigstep/Otherside), Stapel 1.
- Jukebox-Songs per Datagen aus `MusicDiscs.SONGS` (Bootstrap `MusicDiscs#bootstrap`) nach
  `mc26_3/generated/data/simplebuilding/jukebox_song/<name>.json`. Abweichung vom ersten Entwurf (Songs von Hand im
  Overlay): die Datagen braucht die Songs in ihrer Registry (`WikiDataProvider` baut die Item-Komponenten), darum
  stehen die Längen jetzt in `MusicDiscs.SONGS`; das Import-Skript schreibt sie dort und in die erzeugten JSONs. Sound-Events in `ModSounds`
  (`music_disc.<name>`, nur mit Flag), `sounds.json` im 26.3-Overlay mit `"stream": true` (das Overlay verdeckt die
  gemeinsame `sounds.json`, darum steht der Kolben-Bohrklang dort mit drin – Abweichung, bewusst: echte Musik
  soll nicht in die 26.2-Jars).
- Fundorte (feste Chancen ohne eigene Config, wie Vanilla-Platten; je ein eigener Pool,
  wie jeder Mod-Pool unter `enableLootTableChanges`, Struktur-Schaltern und Loot-Faktor - Abweichung: erst ungeschaltet
  geplant, die bestehenden Loot-Tests verlangen aber, dass jeder Mod-Pool schaltbar ist):
  - End-Stadt-Truhe (End), Bastion „übrige Truhen“ (Nether, wo auch Pigstep liegt).
  - Oberwelt 1 → Waldanwesen: verlassen, düster, weit weg – passt zur melancholischen Stimmung; Truhen-basiert
    (Trail Ruins hätte nur Archäologie, die das Wiki nicht als Quelle kennt). Oberwelt 2 → Antike Stätte (Vorbild
    Otherside: eine treibende Platte tief unten).
  - Creeper-von-Skelett: Oberwelt 1/2 kommen in den Vanilla-Tag `minecraft:creeper_drop_music_discs`
    (Vanilla-Mechanik ohne Code; es sind Oberwelt-Platten wie die Vanilla-Creeper-Platten). End/Nether nicht –
    sie bleiben Dimensionsfunde wie Pigstep/Otherside. B-Seiten nie im Loot.
- Texturen: `tools/textures/music_disc_textures.py`, Vanilla-Plattenaufbau (aus dem Client-Jar gelesen), je Platte
  eigenes Label-Motiv + Dimensionsfarbe; Vorschau `previews/schallplatten-vorschau.png`.
- Audio-Platzhalter: `tools/audio/make_placeholder_discs.py` → 3-s-Mono-OGG (leise, zwei Pieptöne, je Platte eigene
  Tonhöhe) unter `mc26_3/overlay/resources/assets/simplebuilding/sounds/records/<name>.ogg`.

### Echte Musik später (Besitzer)
Dateien hierhin legen (MP3 oder WAV, Name genau so):
```
C:\Users\o_o\code\minecraft-mods\musik\end.mp3          -> Voidline
C:\Users\o_o\code\minecraft-mods\musik\overworld1.mp3   -> Driftwood
C:\Users\o_o\code\minecraft-mods\musik\overworld2.mp3   -> Daybreak
C:\Users\o_o\code\minecraft-mods\musik\nether.mp3       -> Brimstone
C:\Users\o_o\code\minecraft-mods\musik\end_alt.mp3      -> Voidline (B-Side)    (usw. *_alt.mp3 je Platte)
```
Dann im Repo: `python tools/audio/import_discs.py` (Optionen `--source <ordner>`, `--ffmpeg <pfad>`, `--dry-run`).
Das Skript wandelt jede gefundene Datei in Mono-OGG Vorbis (~q5, 44,1 kHz), schreibt sie nach
`mc26_3/overlay/resources/assets/simplebuilding/sounds/records/<name>.ogg` und trägt die gemessene Länge
(aufgerundet auf ganze Sekunden) in `MusicDiscs.SONGS` (`common/src/shared/java/com/simplebuilding/util/MusicDiscs.java`)
und in `mc26_3/generated/data/simplebuilding/jukebox_song/<name>.json` ein. Danach einmal Datagen
(`gradlew :mc26_3:fabric:runDatagen :mc26_3:fabric:syncGenerated263`), Tests, committen. Encoder: ffmpeg
(PATH oder `--ffmpeg`), sonst Python `soundfile`; fehlt beides, sagt es, wie man eines installiert. Fehlende Dateien
werden übersprungen (Platzhalter bleibt).

## B) B-Seite per Vorschlaghammer
- Je Platte ein B-Seiten-Item `music_disc_<name>_b_side` mit eigenem Song/Sound („SimpleBuilding - Voidline (B-Side)“),
  gleiche Grundtextur, Label invertiert + Kratzer.
- Platten (A- und B-Seiten) kommen in `simplebuilding:placeable_small` (Schleichen + Rechtsklick legt ab).
- Rechtsklick mit einem Vorschlaghammer (ohne Schleichen) auf ein Häufchen mit einer Platte: die zuletzt gelegte
  wendbare Platte wird zur anderen Seite (A → B → A …), 1 Haltbarkeit, Klang (Amboss leise, hoch) + Noten-Partikel,
  Rechteprüfung wie jede Umwandlung (`TransformTargets.mayTransform`). Neue Klasse `util/DiscFlips`,
  Haken in `PlacedSmallPartsBlock#useItemOn` (wie Schalen), Hand-Hinweis in `TransformTargets`.
- Vanilla-Platten: nein – eine B-Seite bräuchte eigene Musik zu fremden Stücken (keine fremden Audioinhalte).
- In-World-Katalog: Abschnitt `discFlip` in `InWorldTransformations`, Kind `DISC_FLIP` (JEI/REI-Kategorie),
  Wiki-Kind `disc_flip` + Prosa in `wiki/manual.json`.

## C) Lautsprecher
- `astralit_speaker` (8 Bretter um Astralitstaub, wie Notenblock mit Redstone-Staub in der Mitte) verstärkt NUR
  Plattenspieler; `nihilith_speaker` (8 Bretter um Nihilitsplitter, wie Plattenspieler mit Diamant) NUR Notenblöcke.
- Wirkung: direkt angrenzende (6 Seiten) Lautsprecher der passenden Art zählen, gedeckelt durch Config.
  Faktor = 1 + Anzahl × Verstärkung. Kein Echo, keine Verzögerung: nur Lautstärke/Reichweite derselben Wiedergabe.
  - Notenblock: Server-Mixin auf die Lautstärke in `NoteBlock#triggerEvent` (3,0 × Faktor).
  - Plattenspieler: Client-Mixin `LevelEventHandler#playJukeboxSong` (4,0 × Faktor; Config kommt vom Server);
    Server-Mixin `JukeboxSongPlayer#play/stop` schickt Start/Stopp zusätzlich an Spieler zwischen 64 und der
    verstärkten Reichweite (je Start/Stopp genau ein Paket je Spieler – kein Spam).
  - Gilt beim Start eines Stücks bzw. je Notenschlag (ein später gesetzter Lautsprecher wirkt ab dem nächsten Start).
- Server-Config `server.speakers`: `maxSpeakers` 2 (0..3, 0 = aus), `boostPercent` 50 (0..50). Höchstens 2,5-fach:
  Plattenspieler ≤ 160, Notenblock ≤ 120 Blöcke.
- Texturen: Holz-Optik von Notenblock/Plattenspieler mit Astralit- bzw. Nihilit-Membran (`music_disc_textures.py`),
  Vorschau `previews/lautsprecher-vorschau.png`. Modell `cube_column` (`_side` Membran, `_top` oben/unten Holz mit Material-Einlage).

## Einordnung
- Kreativ: Platten im Vanilla-Tab Werkzeuge hinter `music_disc_bounce`, Mod-Tab Werkzeuge Zeile „music_discs“;
  Lautsprecher im Vanilla-Tab Redstone hinter dem Plattenspieler, Mod-Tab Maschinen Zeile „speakers“.
- Lang EN/DE in `src/main/resources` und `mc26_3/overlay/resources`; JEI-Info für die rezeptlosen Platten.
- Testzentrale: Abschnitt `music` (Plattenspieler + Astralit-Lautsprecher, Notenblock + Nihilit-Lautsprecher,
  Truhe mit allen Platten + Hammer, abgelegte Platte zum Wenden).

## Dateien
McVersion (2×), ModItems, ModBlocks, ModSounds, neue `util/DiscFlips`, `util/MusicDiscs`, `util/SpeakerBoost`,
`blocks/custom/SpeakerBlock`, Mixins (Overlay + leere 26.2-Zwillinge, `simplebuilding.mixins.json`,
`simplebuilding.client.mixins.json`), ServerTuningConfig/ServerTuning/ConfigOptionTests, PlacedSmallPartsBlock,
TransformTargets, InWorldTransformations, InWorldRecipeCatalog, JEI/REI-Kategorie, ModLootTableModifications,
SearchTabPlacement, ModItemGroupsContent, Datagen (Modelle, Tags, Loot, Rezepte, Songs), Lang (4), sounds.json,
OGGs (8), Texturen, `tools/audio/*`, `tools/textures/music_disc_textures.py`, Wiki (`generate.py`, `manual.json`,
`obtain_sources.py` unverändert), Testzentrale, Tests (`MusicDiscTests` + Fabric-Adapter + Katalog).

## Risiken
- Overlay-`sounds.json` verdeckt die gemeinsame – neue gemeinsame Klänge müssen künftig in beide (im Kopf des
  Skripts/Plans vermerkt).
- Client-Lautstärke ist nicht im Server-GameTest prüfbar → geprüft wird die gemeinsame Rechenfunktion
  (`SpeakerBoost`) und der Server-Teil (Notenblock-Lautstärke, erweiterte Reichweite); Hören bleibt Client-Abnahme.
- Platzhalter-Länge 3 s: Plattenspieler endet nach 3 s (+ Vanilla-Polster) – gewollt bis echte Musik kommt.

## Verifikation
GameTests `MusicDiscTests`: Registrierung/Komponente/Song/Sound, Tags, Loot in den Inject-Tabellen, Plattenspieler
spielt A- bzw. B-Song, Hammer wendet hin und zurück (Haltbarkeit, Hinweis, falsche Werkzeuge), Lautsprecher
verstärken nur ihre Quelle, Config-Grenzen, Rezepte. Gates: Datagen, `fabric-263`, `neoforge-263`, 26.2-Compile,
Forge-26.3-Compile, Texturen-/Wiki-Check.
