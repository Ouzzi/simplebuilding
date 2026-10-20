# Plan b-music20: Simple Music (Notenblatt, Orchestrion, eigene Platten) – 2026-10-20

Branch `b-music20` (Basis claude-wave1 200c482c5). Konzepte: `KONZEPT-NOTENBLATT-2026-10-19.md` (KONZ20 N1 m, N2 e1, N3 r1,
N4 a, N5 o) und `KONZEPT-MUSIKPLATTE-UPLOAD-2026-10-19.md` (U1 b, U2 b, U3 4 MiB/5 min/5, U4 Mono, U5 Ordner).

## Ist-Zustand (erhoben)
- Code-Feld `BlueprintCodeArea` liegt in `common/src/shared` (gilt für 26.2 **und** 26.3). SimpleLib gibt es nur auf 26.3.
- Vorlage junges Modul: `modules/simplebeacons` (simplelib + framework gebündelt, Forge-Test-Mixin, Guide, Discover,
  check_data.py). Unbündeltes Modul ohne Elternteil: `simplestats` (JUnit in `fabric/src/test`).
- Kiste-über-Jukebox: SB `JukeboxPlaylist` (Rückport 26.4, aus bei `#minecraft:jukebox_containers`).
- Netzwerk-Muster: `simplemaps` (Payload-Records, `toServer`/`toClient`-Hooks je Loader).
- Client-Klangstrom: `SoundBufferLibrary#getStream(Identifier, looping)` öffnet über den ResourceProvider → Mixin-Hook
  für `simplemusic:sounds/upload/<sha>.ogg` auf die Cache-Datei.

## Entscheidungen (selbst getroffen, begründet)
- **Modul ohne Elternteil** (wie simplestats): `requires simplelib`, `optional simplebuilding`. Kein Einbau in die
  SB-Super-Mod-Listen (ConfigOptions/Bundling) in diesem Zug → kein Eingriff in SB-Tests; Bündeln als Sub-Mod ist ein
  Folgeschritt der Hauptsitzung (Memory).
- **Code-Feld nach SimpleLib**: neues `com.simplelib.api.client.ui.UiCodeArea` (generisch: Farbpalette, Stil-Bytes,
  Fehlerbereiche, eigener Accessor-Mixin). SB: `BlueprintCodeArea` wandert für 26.2 nach `common/src/mc26_2/java`
  (unverändert), auf 26.3 liegt in `mc26_3/overlay/java` eine dünne Unterklasse von `UiCodeArea` (gleiche API) –
  `checkOverlays` bleibt erfüllt (keine Klasse doppelt).
- Notencode, NBS-Leser, Ogg-Prüfer sind reine Java-Klassen (ohne Minecraft) → JUnit in `modules/simplemusic/fabric/src/test`.
- Abspielen P1 (Server-Töne je Tick). Klang-Sperrliste N4 a serverseitig (Config `sheetSounds`, `blockedSounds`).

## Stufen (je eigene Commits + Tests)
1. **A1** Modul-Gerüst (Fabric/NeoForge/Forge, Standalone-Target, Wiki, Guide-Eintrag), Code-Feld nach SimpleLib,
   Notenblatt-Item (Komponente `simplemusic:sheet`, Signieren, Kopieren), Notencode-Parser (JUnit), Editor E1 mit
   Probehören (nur Client), Speichern per Payload, Server prüft erneut (Länge, Parse, Sperrliste).
2. **A2** Orchestrion: Einlegen/Herausnehmen, Server-Sequenzer, Redstone R1 (an = spielt + loopt, aus = stoppt am
   Schrittende, steigende Flanke = von vorn, ohne Anschluss spielt es), Komparator = Fortschritt, Kiste darüber = nächstes
   Blatt (Tausch, eines allein loopt), ≤ 8 Töne/Tick. GameTests.
3. **A3** Import `.txt` / `.nbs` aus `config/simplemusic/sheets/` (Client, erzeugt nur Text), Export in die Zwischenablage.
   NBS-Leser JUnit mit eigener kleiner Testdatei (im Test erzeugt).
4. **B1** Rohling `simplemusic:blank_disc`, Ordnerliste `config/simplemusic/upload/`, Ogg-Leser + Prüfung (CRC, ein
   Strom, Vorbis-Köpfe, Mono, Rate, Dauer, Größe) – JUnit mit eigenen synthetischen Test-OGGs.
5. **B2** Upload in Stücken (≤ 32 KiB, 1 je Spieler, Rate-Limit, Ankündigung gegen Quote), Speicher
   `<welt>/simplemusic/tracks/<sha>.ogg` + `index.json`, Dedup, Quote 5, Befehle `/music list|approve|reject|delete|ban|info`,
   Moderation U-B (Einzelspieler sofort frei). GameTests über simulierte Payloads.
6. **B3** Download in Stücken nur bei Bedarf, Client-Cache, Wiedergabe über eigene SoundInstance; Orchestrion spielt eigene
   Platten (Loop/Kiste nach `seconds`); Vanilla-Jukebox per Mixin (U1 b) hinter Schalter `jukeboxCustomDiscs` und
   Versions-Wächter (aus, sobald Vanilla `#minecraft:jukebox_containers` hat bzw. auf 26.4 – Kollision mit SB-Rückport
   dokumentiert).
7. Texturen (eigene Pixelkunst, 2–3 Varianten je Motiv), Wiki/Guide/Discover erzeugen (sb-test), Queue, Memory.

## Risiken
- 26.3-API-Abweichungen (Quellen nur aus 26.3.0.16-beta) → früh kompilieren (alle drei Loader).
- Client-Wiedergabe (B3) nur per Compile + Server-Tests prüfbar; Client-Läufe sind verboten → als offen melden.
- Parallele Agenten: gemeinsame Dateien (modules.json, enabled-mods.json, QUEUE.md) nur eigene Zeilen.

## Verifikation
- JUnit (`:modules:simplemusic:fabric:test`), Compile aller Loader inkl. SB 26.2/26.3 (Code-Feld-Umzug),
  `module-simplemusic-fabric-263`, `module-simplemusic-standalone-fabric-263`, `module-simplemusic-neoforge-263`,
  Blueprint-Tests SB gefiltert, `./gradlew check -q`, Guides/Wiki/Discover erzeugt.
