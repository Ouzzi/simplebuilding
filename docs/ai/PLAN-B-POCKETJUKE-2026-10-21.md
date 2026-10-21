# Plan b-pocketjuke (21.10.2026): Pocket Jukebox in Simple Music

Auftrag: Queue-Zeile „(B, b-pocketjuke, 21.10.)“, Besitzer 21.10.: „soll färbbar sein und statt rohling im recipie ein
diamant um konsistent zu bleiben“, Ort Simple Music. Vorbild und Vorschlag: `docs/ai/RECHERCHE-POCKET-JUKEBOX-2026-10-21.md`.

## Ist-Zustand (erhoben)
- `modules/simplemusic`: Rohling (`blank_disc`), eigene Platte (`custom_disc`, Komponente `simplemusic:track`),
  `DiscPlayback` (Server: wer hört welchen Upload, Download-Sperre `playsFor`), Client `TrackPlayer`/`CustomDiscSound`
  (Stream aus dem Cache, Spätstart per Sekunden-Sprung im `SoundBufferLibraryMixin`), Paket `TrackPlayPayload`.
- Vanilla 26.3 hat 17 Bündel (`bundle`, `<farbe>_bundle`), Färben per `crafting_transmute` auf `#minecraft:bundles`,
  Bündel-Optik über `BUNDLE_CONTENTS` (Item-Modell `bundle/has_selected_item` + `bundle/selected_item`, Tooltip
  `ClientBundleTooltip`). Kupfernugget gibt es in 26.3.
- Simple Storage hat eigene Bündel (`ReinforcedBundleItem`, Tooltip-Mixin) – nicht nutzbar (Modul-Unabhängigkeit),
  nur als Vorbild. Simple Music braucht nur Vanilla-Bündel, also kein Fallback nötig.

## Entscheidungen (selbst getroffen, begründet)
1. **17 Items** `simplemusic:pocket_jukebox` + `simplemusic:<farbe>_pocket_jukebox` wie Vanillas Bündel (eigene
   Texturen je Farbe, keine Tönung; Färben ohne Komponenten-Verlust per `crafting_transmute`).
2. **Rezept** je Farbe: `NDN / NBN / NNN` (N Kupfernugget, D Diamant oben Mitte, B Bündel der Farbe). Färben:
   `crafting_transmute` von `#simplemusic:pocket_jukeboxes` + Farbstoff (16 Rezepte, Gruppe `pocket_jukebox_dye`).
   NICHT in `#minecraft:bundles` (sonst würden Vanillas Bündel-Färberezepte aus der Jukebox ein Bündel machen); dadurch
   kein Mausrad-Auswählen wie beim Bündel (das hängt in Vanilla am Tag) – Entnehmen nimmt die zuletzt eingelegte Platte.
3. **Inhalt** im Vanilla-`BUNDLE_CONTENTS` (Tooltip-Raster und Bündel-Modell funktionieren damit), eigene Einlege-Logik:
   nur Platten (`jukebox_playable` oder eigene Platte mit Track), je 1 Stück, höchstens 20. Klicks wie Bündel
   (Linksklick einlegen, Rechtsklick auf leeren Platz / mit leerer Hand eine herausnehmen), Bündel-Sounds
   (insert, insert_fail, remove_one). Der Füllbalken im Tooltip zeigt n/20 (eigenes Tooltip-Objekt + Client-Mixin
   `ClientBundleTooltipMixin`, das nur für dieses Objekt den Füllstand durch 20 teilt).
4. **Abspielen** (Server-Autorität, `pocket/PocketPlayback`): Benutzen in der Haupthand = Start/Pause; Zweithand: PASS.
   Reihenfolge: zuerst eingelegte zuerst, nach der letzten von vorn. Zustand am Stack (`simplemusic:pocket`:
   id, Platz ab der ältesten, Ticks) wird nur bei Start/Pause/Wechsel geschrieben, laufende Zeit im Speicher.
   Je Spieler eine Sitzung: Start eines zweiten pausiert den ersten. Stopp, wenn die laufende Platte fehlt/ausgetauscht
   ist oder der Stack nicht mehr im Inventar (inkl. Cursor) liegt; Neustart/Abmelden = Sitzung weg.
   „Offen“-Sprite: während des Spielens ist die laufende Platte im `BUNDLE_CONTENTS` ausgewählt → Vanilla-Modell zeigt
   den offenen Beutel mit der Platte, Tooltip hebt sie hervor. Ein Stack, der beim Spielen das Inventar verließ, wird
   beim nächsten Inventar-Tick wieder geschlossen.
5. **Nur der Träger hört**: Server schickt dem Träger `TrackPlayPayload` mit `carried=true` (neues Feld, kein neues
   Paket → keine Registrierung in drei Loadern); Feld `sha256` trägt dann den Upload-Hash oder die Sound-Event-ID der
   Vanilla-Platte. Client `PocketPlayer`: nicht positionsgebunden (`relative`, keine Abschwächung), Lautstärke 1,
   Kategorie Jukebox; stoppt beim Start Musik und andere Platten-Klänge (MUSIC + RECORDS) und hält die
   Hintergrundmusik an, solange gespielt wird. Fortsetzen mitten im Lied: Sekunden-Sprung wie bei Uploads, für
   Vanilla-Platten über `simplemusic:sounds/seek/<s>/<ns>/<pfad>` im vorhandenen `SoundBufferLibraryMixin`.
   Upload-Platten: Download-Sperre erlaubt auch die Pocket-Sitzung (`PocketPlayback.playsFor`).
6. Tooltip-Zeile (Nachtrag 38 (a), Bedienung nicht offensichtlich): eine Zeile „Benutzen: abspielen/pausieren“.
7. Kreativ: eigener Musik-Tab (alle 17) und Vanilla-Tab Werkzeuge (nach den Bündeln, Fabric `insertAfter(PINK_BUNDLE)`).
8. Rückport-Regel: Java hat kein Gegenstück; Port-Notiz im Wiki.

## Dateien
- Java: `pocket/PocketJukeboxItem`, `pocket/PocketState`, `pocket/PocketPlayback`, `pocket/PocketContents`
  (Tooltip-Daten), `client/PocketPlayer`, `client/PocketSound`, `client/PocketTooltip`,
  `mixin/client/ClientBundleTooltipMixin`; Änderungen `MusicRegistry`, `MusicComponents`, `TrackPlayPayload`,
  `TrackPlayer`, `MusicServer` (tick/stopped/Download-Sperre), `SoundBufferLibraryMixin`, `MusicClient`, `SimpleMusic`
  (Testzentrale), Loader (Tooltip-Fabrik, Kreativ-Tab, Neo-Client bekommt den Mod-Bus).
- Daten: 17 + 16 Rezepte, 33 Rezept-Fortschritte, Tag `simplemusic:pocket_jukeboxes`, 17 Item-Definitionen,
  51 Modelle (geschlossen, offen hinten/vorn), Lang EN/DE.
- Texturen: `tools/gen_textures.py` erweitert (Grundform A/B, 17 Farben × geschlossen/offen hinten/offen vorn), auf
  sb-test erzeugt (-gen). Abnahme-Seite (eigenständige HTML, nicht veröffentlicht) `pj21.html`, Code `PJ21 F=?`.
- Doku: `wiki/manual.json` (Feature `pocket_jukebox`), Guide (generiert), Discover (generiert),
  `docs/MODS-UND-FEATURES.md`, `tools/check_data.py`.
- Tests: `MusicTests` + Fabric-Adapter: `pocket_recipe` (Rezept je Farbe + Färben behält Inhalt),
  `pocket_insert_remove` (nur Platten, max 20, Entnehmen), `pocket_play_pause` (Start, Pause/Fortsetzen, Schleife,
  zweiter pausiert ersten, Zweithand), `pocket_stops` (Platte entnommen, Item weg).

## Risiken
- Mixin-Ziel `extractBundleWithItemsTooltip` teilt Simple Storage (@ModifyArg verkettet sich, beide prüfen ihre Instanz).
- Sound-Sprung bei Vanilla-Platten und Client-Verhalten nur kompiliert, nicht im Client getestet (keine Client-Läufe).

## Verifikation
`module-simplemusic-fabric-263`, `module-simplemusic-neoforge-263`, `module-simplemusic-standalone-fabric-263`,
`data_integrity`, `integration-263`; Compile Fabric/NeoForge/Forge; `check_data.py`, `check_tooltip_text.py`,
`check_missing_textures.py`; Guides/Wiki/Discover auf sb-test; `./gradlew check -q`.
