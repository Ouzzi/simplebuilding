# Konzept: Notenblatt + Orchestrion (Notenblock-Plattenspieler) (2026-10-19)

Queue „Besitzer 19.10. abends – Nachtrag“, Zeile Notenblatt (B, b-konz20). Nur Konzept, kein Code. Abgleich mit dem Upload-Konzept:
`docs/ai/KONZEPT-MUSIKPLATTE-UPLOAD-2026-10-19.md` (gleiches Modul, gleicher Abspielblock). Entscheidungen: §8 (Code `KONZ20 N…`).

## 1. Ist-Zustand (per grep erhoben)
- **Blaupause** (SimpleBuilding-Kern, `common/src/shared/.../blueprint/`): Text-Sprache `BlueprintCode` (Parser + Serialisierer + Einfärbung,
  Grenzen `MAX_CODE_LENGTH` 32000, Titel 32), Editor `BlueprintScreen` mit `BlueprintCodeArea`, Import/Export `BlueprintIoScreen` +
  `BlueprintFormats` (eigener Code oder Vanilla-Befehle, alles-oder-nichts, Import erzeugt nur Text), Bild-Import aus
  `config/simplebuilding/images/` (`BlueprintPictureScreen`, Datei verlässt den Client nie). Speichern über `BlueprintEditPayload`, Server prüft
  in `ModMessageHandlers#handleBlueprintEdit` erneut (Länge, Parse, Signatur, wie beim Buch).
- **Musik im Kern:** Dimensions-Platten `MusicDiscs` (datengetriebene `jukebox_song`), Musik- und Noten-Verstärker `SpeakerBoost`
  (Kette, Funk, Lautstärke > 1 über `AmplifiedSound`/`AmplifiedNoteSound`, `SoundEngineAmplifierMixin`), Jukebox + Kiste darüber
  `JukeboxPlaylist` (Rückport 26.4: nimmt nächste Platte, tauscht nach Ende, **eine Platte allein loopt schon**; aus, wenn Vanilla
  `#minecraft:jukebox_containers` hat).
- Vanilla-Notenblock: 16 Instrumente (Block darunter) + Köpfe darüber (`note_block_sound` des Kopfs), 25 Tonhöhen (F♯3–F♯5, Pitch
  2^((n−12)/12)), Klang serverseitig `playSeededSound(RECORDS, 3.0)`.
- `modules/simplesounds` = nur lokale Immersions-Klänge (Client), kein Musik-Inhalt. Kein Notenblatt, kein Sequenzer, kein NBS-Import.
- `modules/simplemobheads`: jeder Kopf hat `sound` (Notenblock-Klang) – nutzbar als Instrument, nur über Registry-Ids.

## 2. Zielmodul (Vorschlag): neues Modul **`simplemusic`** („Simple Music“)
- Notenblatt, Orchestrion und (Konzept Upload) eigene Platten gehören zusammen und sind kein Bau-Thema → eigenes Modul (A2), nicht SB-Kern,
  nicht simplesounds (das ist ein Client-Klangpaket ohne Blöcke).
- `requires: simplelib` (UI-Kasten, **Code-Feld**: `BlueprintCodeArea` ist SB-Kern und darf nicht importiert werden → das generische
  Text-Feld mit Einfärbung wandert nach `simplelib/client` und SB nutzt es von dort; Regel 3).
- Optional: simplebuilding (Noten-/Musik-Verstärker wirken auf das Orchestrion nur über einen Tag `simplebuilding:speaker_sources`, den SB
  liest – kein Klassenimport), simplemobheads (Köpfe als Instrument, über Registry-Ids), Note Block Studio-Dateien (nur Format).
- Allein spielbar: Rezepte aus Vanilla (Papier, Tintenbeutel, Feder, Notenblock, Jukebox).

## 3. Notenblatt (Item `simplemusic:sheet_music`)
- Rezept (Vorschlag): Papier + Tintenbeutel + Feder (formlos) → 1 leeres Notenblatt. Kopieren: beschriebenes + leeres → 2 (wie Buch).
- Rechtsklick = Editor (wie Blaupause), Signieren mit Titel (danach schreibgeschützt, Autor-Name).
- Inhalt = Text-Notencode als Item-Komponente `simplemusic:sheet` (≤ 32000 Zeichen wie die Blaupause), Server prüft beim Speichern.

### 3.1 Notencode (Vorschlag, Stil wie `BlueprintCode`)
```
# Kommentar
tempo 4                        Ticks je Schritt (1-20; 4 = 5 Schritte/s)
$h = harp                      Alias (Instrument oder Klang)
$k = sound minecraft:entity.cat.ambient   beliebiger Klang als Instrument (Server-Liste, s. 6)
$h: c4 e4 g4 - c5 . . g4       Spur: Noten (c4..f#6 bzw. 0..24), - = halten, . = Pause
bass: c3 . c3 . [c3 g3] .      [..] = Akkord (bis 4 Töne)
x2 { $h: c4 d4 }               Wiederholung
@vol 0.6                       Lautstärke der folgenden Noten (0.1-1.0)
```
Spuren laufen parallel, Schritt n aller Spuren klingt gleichzeitig. Grenzen: ≤ 16 Spuren, ≤ 8 Töne je Tick gesamt (Rest wird beim
Prüfen als Fehler gemeldet), Länge ≤ 10 min. Fehler mit Zeile/Spalte wie im Blaupausen-Editor.

### 3.2 Editor-Varianten
| | Idee | + | − |
|---|---|---|---|
| **E1 Text + Probehören (Empfehlung Stufe 1)** | Code-Feld wie Blaupause, Knopf „Probe“ spielt lokal (nur Client) | wenig UI, gleiche Technik | Komponieren nur für Tüftler |
| E2 Raster (Piano-Roll) | Gitter Schritt × Tonhöhe, Instrument je Farbe, schreibt Code | intuitiv | großes Fenster, viel UI-Code |
| E3 Beides | Raster als zweiter Reiter, Code bleibt Wahrheit | beste Bedienung | Stufe 3 |

**Import** (wie Blaupausen-Import, Ordner `config/simplemusic/sheets/`): `.txt` (Notencode) und **Note Block Studio `.nbs`** (verbreitetes
Format; Lage/Instrument/Taste → Spuren). Der Import erzeugt nur Text, der Server sieht nur den Code. Export: Text in Zwischenablage.

## 4. Orchestrion (Block `simplemusic:orchestrion`, Notenblock + Plattenspieler)
- Rezept (Vorschlag): Notenblock + Jukebox + Kupferbarren ×2 (Glocken-Mechanik): `C N C / . J .` → 1.
- Nimmt **Notenblätter** und (Upload-Konzept) **eigene Platten**, auch Vanilla-Platten. Ein Rechtsklick legt ein/nimmt heraus. Kein Auswurf.
- **Redstone (Vorschlag R1):** Signal an = spielt und **loopt endlos**; Signal aus = hört am Ende des aktuellen Takts (Schritt) auf;
  steigende Flanke startet von vorn. Ohne Redstone-Anschluss: spielt beim Einlegen einmal in Schleife wie R1 mit „an“.
  Alternative R2: Puls schaltet an/aus (wie ein T-Flipflop).
- **Kiste darüber** (gleiche Regel wie `JukeboxPlaylist`): nach Ende eines Durchlaufs nimmt es das nächste Blatt/Platte aus der Kiste
  (Slot-Reihenfolge, Tausch), ohne weitere Blätter loopt das eine. Behälter-Erkennung über eigenen Tag `simplemusic:playlist_containers`
  (Truhe, Fass, Kupfer-/Stufentruhe bedingt), nicht über SB-Code.
- Komparator: 1–15 = Fortschritt im Stück (Vorschlag) – nützlich für Redstone-Shows.
- Abspielen **serverseitig** je Tick: der Block plant die Töne des Schritts und schickt sie wie ein Notenblock (`playSeededSound`, Kategorie
  RECORDS, Reichweite 48). Varianten:
  - **P1 Server-Töne je Tick (Empfehlung):** einfach, immer synchron, Verstärker-Tag wirkt, Traffic gering (≤ 8 Töne/Tick, nur Spieler in 48).
  - P2 Blatt einmal an Clients, Client spielt: weniger Pakete, aber Drift bei Lag, Code doppelt, Chunk-Grenzen.
- Partikel: Noten wie Notenblock (Farbe = Tonhöhe), sparsam (max. 1 Partikel je Tick).

## 5. Abgleich mit dem Upload-Konzept
Das Orchestrion ist der **eine** Abspielblock für beides (Notenblatt + eigene Platte): gleiche Redstone-/Schleifen-/Kisten-Regeln, ein
Modul, eine Config. Die Vanilla-Jukebox bleibt Vanilla (mit Kiste-Rückport aus SB); eigene Platten laufen dort nicht (ihre Länge ist nicht
im festen `jukebox_song`, s. Upload §3).

## 6. Server-Sicherheit und Balancing
- Code ≤ 32000 Zeichen, Parse auf dem Server (gleicher Parser wie Client, alles-oder-nichts), Komponente gekappt; Paket-Codec mit Längen.
- Klänge: Instrumente + Kopf-Klänge immer; **beliebige Klänge** nur aus Registry, Server-Config `sheetSounds = instruments|heads|all`
  (Standard `all`) mit Sperrliste (Standard: `music.*`, `music_disc.*`, `ui.*`, `ambient.cave`, alle Klänge mit Reichweite „global“).
- Last: ≤ 8 Töne je Tick und Block, ≤ 32 Orchestrien je Chunk-Bereich spielen gleichzeitig (Config, hart 64); Spieler hört je Ton höchstens
  einen Abspielpunkt (wie `SpeakerBoost#nearest`).
- Kein Spam: Töne nur an Spieler in Reichweite, Client respektiert Lautstärke-Regler „Jukebox/Notenblöcke“.

## 7. Stufenplan
1. Modul-Gerüst `simplemusic` (newmod), Code-Feld nach simplelib verschieben (SB nutzt es mit), Notenblatt-Item + Notencode + Editor E1
   + Probehören.
2. Orchestrion: Einlegen, Server-Sequenzer P1, Redstone R1, Loop, Komparator, Kiste darüber.
3. Import `.txt`/`.nbs` aus Ordner, Export; Raster-Editor (E2/E3) nach Besitzer-Wahl.
4. Verstärker-Tag in SB (bedingt), Kopf-Instrumente, Wiki/Guide/Discover, Client-Hörabnahme.

## 8. Testplan
- JUnit: Notencode-Parser (Rundreise, Fehlerstellen, Grenzen, Akkorde, Wiederholung), NBS-Leser mit Testdatei (eigene, kleine), Editor-Layout.
- GameTests: Orchestrion spielt Schritt n im Tick n (Tonzähler), Redstone an/aus/Flanke, Loop ohne Auswurf (Item bleibt), Kiste: nächstes
  Blatt + Tausch, ein Blatt loopt, Komparator, gesperrter Klang wird beim Speichern abgelehnt, zu langer Code abgelehnt, ≤ 8 Töne/Tick.
- Standalone `module-simplemusic-standalone-fabric-263`; Integration mit SB (Verstärker-Tag).
- Hörabnahme Besitzer (Beispielblatt „Ode an die Freude“, gemeinfrei).

## 9. Offene Besitzer-Fragen (Vorschlag fett)
- **N1 Modul:** **neues Modul Simple Music** / in SimpleBuilding-Kern / in Simple Sounds.
- **N2 Editor:** **E1 Text + Probehören zuerst, Raster später** / gleich Raster / nur Text.
- **N3 Redstone:** **R1 an = spielt + loopt, aus = stoppt am Schrittende** / R2 Puls schaltet um.
- **N4 Klänge:** nur Instrumente / Instrumente + Köpfe / **alle Klänge mit Sperrliste**.
- **N5 Name des Blocks:** **Orchestrion** / Notenspieler / Spieluhr.
