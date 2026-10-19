# Konzept: Eigene Musikplatte per Datei-Upload (2026-10-19)

Queue „Besitzer 19.10. abends – Nachtrag“, Zeile Eigene Musikplatte (B, b-konz20). Nur Konzept, kein Code. Gehört zusammen mit
`docs/ai/KONZEPT-NOTENBLATT-2026-10-19.md` (Modul `simplemusic`, Abspielblock Orchestrion). Entscheidungen: §9 (Code `KONZ20 U…`).

## 1. Ist-Zustand (per grep erhoben)
- **Blaupausen-Import** liest Dateien nur clientseitig aus einem festen Ordner (`config/simplebuilding/images/`, `BlueprintPictureScreen`);
  der Server sieht nur das Ergebnis (Code) und prüft alles neu. Kein Datei-Dialog im Projekt (kein AWT/tinyfd) – Ordner-Muster ist erprobt.
- **Musik:** Platten sind datengetrieben (`jukebox_song`: Sound-Event, **feste Länge**, Komparator), Klang aus Ressourcenpaket
  (`assets/.../sounds/records/*.ogg`, `"stream": true`). Import nur für Entwickler: `tools/audio/import_discs.py` (MP3→OGG, `soundfile`).
  `JukeboxPlaylist` (Kiste über Jukebox, eine Platte loopt) und `SpeakerBoost` (Verstärker) im SB-Kern.
- Netzwerk: nur kleine Custom Payloads (Blaupausen-Code ≤ 32000 Zeichen). **Kein** Chunking, kein Datei-Cache, kein Download vom Server.
- Folgerung: Ein hochgeladener Klang kann **nicht** über `jukebox_song`/Ressourcen laufen (Registry fest nach Start, Länge fest). Er braucht
  eine eigene Wiedergabe-Instanz auf dem Client, die eine Datei aus einem Cache streamt.

## 2. Ablauf (Vorschlag)
1. **Rohling** `simplemusic:blank_disc`: Rezept (Vorschlag) 4 Kohle/Holzkohle + 4 Schwarzstein um 1 Amethystsplitter → 1.
2. Spieler legt `.ogg` in `config/simplemusic/upload/` (wie Bilder für die Blaupause). Rechtsklick mit Rohling = Fenster: Dateiliste,
   Titel (≤ 32), Prüfung lokal (Format/Länge/Größe), „Brennen“.
3. Client schickt die Datei **in Stücken** an den Server, Server prüft (§4), speichert, antwortet mit Hash. Der Rohling wird zur
   **Eigenen Platte** mit Komponente `simplemusic:track = {sha256, title, seconds, uploader}` (keine Audiodaten im Item).
4. Abspielen im **Orchestrion** (Notenblatt-Konzept §4): gleiche Regeln – Redstone, Schleife ohne Auswurf, **Kiste darüber = nächste Platte,
   eine Platte allein loopt endlos**.
5. Spieler in Reichweite bekommen „spiele Hash X ab Sekunde t“; fehlt die Datei im Client-Cache, lädt er sie in Stücken; danach spielt er
   ab der aktuellen Stelle (synchron zur Serverzeit).

## 3. Wo spielt die Platte? (Varianten)
| | Idee | + | − |
|---|---|---|---|
| **A · nur Orchestrion (Empfehlung)** | eigene Wiedergabe im eigenen Block | kein Vanilla-Eingriff, Länge je Track frei | Vanilla-Jukebox lehnt eigene Platten ab |
| B · auch Vanilla-Jukebox per Mixin | Mixin in `JukeboxSongPlayer` für Länge/Klang | gewohnter Block | Mixins je Loader, kollidiert mit `JukeboxPlaylist`/26.4-Vanilla |
| C · Ein Platzhalter-`jukebox_song` | ein Song „custom“ mit Höchstlänge | wenig Code | Länge falsch (Stille/abgeschnitten), Komparator falsch |

## 4. Format, Größe, Prüfung (Server)
- **Nur Ogg Vorbis** (Minecraft spielt nur das; kein MP3-Decoder auf dem Server). Client-Fenster weist andere Formate ab mit Hinweis
  „mit Audacity als OGG exportieren“.
- **Mono Pflicht:** OpenAL verortet nur Mono-Quellen; Stereo spielt ohne Entfernungsabfall → überall gleich laut (Grief). Server liest die
  Kanalzahl aus dem Vorbis-Kopf und lehnt Stereo ab (Vorschlag; Alternative: Client mischt vor dem Hochladen auf Mono – nicht möglich ohne
  Decoder im Client-Fenster → abgelehnt).
- Grenzen (Config, harte Obergrenze in Klammern): `maxBytes` 4 MiB (16), `maxSeconds` 300 (900), Abtastrate 22,05–48 kHz,
  `quotaPerPlayer` 5 Tracks (50), `storeLimit` 256 MiB (2 GiB) gesamt.
- Prüfung **ohne Decoder** (der Server hat kein LWJGL/stb_vorbis): eigener kleiner Ogg-Leser – Seiten-Kopf „OggS“, CRC jeder Seite,
  ein logischer Strom, Vorbis-Identifikations-/Kommentar-/Setup-Kopf vorhanden, Kanäle = 1, Rate im Bereich, Dauer = Granule der letzten
  Seite / Rate ≤ `maxSeconds`, Datei endet sauber. Kommentar-Felder werden verworfen (nicht gespeichert, nicht gesendet).
- Restrisiko: Clients dekodieren fremde Dateien mit stb_vorbis (hatte in der Vergangenheit Speicherfehler-CVEs). Deshalb Moderation §6 und
  Client-Schalter „Fremde Musik laden“ (an / nur von Freigegebenen / aus).

## 5. Speicher und Netzwerk
- Server: `<welt>/simplemusic/tracks/<sha256>.ogg` + `index.json` (Hash, Titel, Sekunden, Uploader-UUID, Datum, Status). Gleicher Hash = eine
  Datei (Dedup). Upload erst in `tmp/`, nach Prüfung atomar verschieben; abgebrochene Uploads nach 5 min gelöscht.
- Upload: Payload `UploadChunk(id, index, bytes ≤ 32 KiB)`, höchstens 1 Upload je Spieler gleichzeitig, Rate ≤ 256 KiB/s je Spieler
  (Config), Gesamtgröße vorab angekündigt und gegen Quote geprüft, Hash am Ende vom Server selbst berechnet (nie dem Client geglaubt).
- Download: Client fragt `TrackRequest(hash)` nur, wenn ein Orchestrion in Reichweite (≤ 64) diesen Hash spielt; Server streamt Stücke mit
  Rate-Limit je Spieler und global (Config, Standard 512 KiB/s gesamt), nur freigegebene Tracks.
- Client-Cache: `<.minecraft>/simplemusic-cache/<server-hash>/<sha256>.ogg`, LRU 512 MiB (Client-Config), Hash nach Download geprüft.
- Wiedergabe: eigene `SoundInstance` mit Stream aus Cache-Datei (Mixin/Hook in die `SoundBufferLibrary`-Stream-Auflösung für den Namensraum
  `simplemusic:upload/<hash>`), Kategorie RECORDS, Lautstärke wie Jukebox (Reichweite 64), Verstärker-Tag bedingt.

## 6. Moderation, Rechte, Config
| Variante | Wer darf brennen | Sichtbar für andere |
|---|---|---|
| U-A · Alle mit Quote, sofort | jeder Spieler | sofort |
| **U-B · Alle mit Quote, OP gibt frei (Empfehlung)** | jeder Spieler | erst nach `/music approve <hash>`; vorher hört nur der Uploader die Platte |
| U-C · Nur OPs | Rechte-Stufe ≥ 2 | sofort |

- Einzelspieler / LAN-Host: immer erlaubt und sofort freigegeben.
- Befehle (OP): `/music list [pending]`, `approve`, `reject`, `delete <hash>` (Platten werden zu „zerkratzter Platte“, spielen nichts),
  `ban <spieler>` (keine Uploads mehr), `info <hash>`.
- Config `simplemusic-server.json`: `uploads = everyone|approve|op|off` (Standard `approve`), Grenzen aus §4/§5. Rechte über Vanilla-Stufe;
  ein Rechte-Mod kann später über das Framework anknüpfen.
- Urheberrecht: Hinweis im Fenster „Nur eigene oder freie Musik hochladen“; Verantwortung beim Server-Betreiber (Wiki).

## 7. Stufenplan
1. (setzt Notenblatt-Stufen 1–2 voraus: Modul + Orchestrion) Rohling, Ordner-Liste, Ogg-Leser + Prüfung (JUnit mit eigenen Test-OGGs).
2. Upload in Stücken, Speicher, Index, Quote, Befehle, Moderation U-B.
3. Download, Cache, Client-Wiedergabe mit Startversatz, Orchestrion spielt eigene Platten, Kiste/Loop.
4. Client-Schalter, Wiki/Guide, Hörabnahme, Last-Test (10 Spieler, 3 Orchestrien).

## 8. Testplan
- JUnit: Ogg-Leser (gültig mono, stereo abgelehnt, falsche CRC, abgeschnitten, zwei Ströme, zu lang, zu groß, falsche Rate, kein Vorbis);
  Chunk-Zusammenbau (Lücke, doppelt, falsche Reihenfolge, Übergröße); Hash-Dedup; Quote.
- GameTests: Upload über simulierte Payloads → Index-Eintrag + Item-Komponente; Abbruch löscht tmp; nicht freigegeben = kein Download für
  andere; `delete` macht Platte stumm; Orchestrion loopt eigene Platte ohne Auswurf, Kiste wechselt nach `seconds`; Rate-Limit greift.
- Client (nur auf Anforderung, sb-test-Last): Download + Wiedergabe + Cache-Treffer, Startversatz.

## 9. Offene Besitzer-Fragen (Vorschlag fett)
- **U1 Abspielort:** **A nur Orchestrion** / B auch Vanilla-Jukebox per Mixin.
- **U2 Wer darf hochladen:** U-A alle sofort / **U-B alle, OP gibt frei** / U-C nur OPs.
- **U3 Grenzen:** **4 MiB / 5 min / 5 je Spieler** / 8 MiB / 10 min / 10 je Spieler / 2 MiB / 3 min / 3.
- **U4 Stereo:** **ablehnen (Mono Pflicht)** / zulassen, aber nur im Umkreis hörbar per eigener Lautstärke-Kurve (unsauber).
- **U5 Datei-Auswahl:** **Ordner `config/simplemusic/upload/` wie bei Bildern** / Drag & Drop ins Fenster (GLFW-Drop, je Loader prüfen).
