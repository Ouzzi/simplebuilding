# Plan A-SHOPMUSIC37 (12.11.2026): Shop-Musik – 3 Discs + Musik im Shop

Branch `a-shopmusic37` (Basis `hub/claude-wave1` 240d278d5), Auftrag `/root/sb-tools-a/agenten-1530.md` §6 (Besitzer 12.11. ~11:50).
Nur 26.3 Fabric. Antwortcode **SHOPMUSIC37**.

## Urheberrecht (verbindlich, A-Entscheidung)
Keine Noten, Melodie, Akkordfolge oder Hook eines geschützten Stücks; kein Name/Hinweis auf eine Konsole oder einen
Hersteller in Prompts, Texten, Dateinamen. Erlaubt ist nur Genre/Stimmung: entspannte Lounge-/Bossa-Nova-Musik für einen
Markt, E-Piano/Vibraphon, leichte Percussion, verspielt, loopbar, 94–108 BPM. Die Melodie-Idee je Disc ist eigene
Beschreibung (Kontur in Worten, s. u.); die ElevenLabs-Prompts enthalten nur Stil + diese eigene Kontur
(`/root/sb-tools-a/shopmusic37/prompts.tsv`, geprüft: kein Marken-/Werkname).

## Ist-Zustand (erhoben)
- **Simple Music** hat 6 ElevenLabs-Discs (`disc/MusicDiscs.java`, `DISCS`-Liste: Song-JSON mit direktem Sound-Event,
  Loot-Hook je Tabelle, Texturen `tools/gen_textures.py` `VDISCS`, `check_data.py`, Gametests `music_discs`/`music_disc_loot`,
  JEI-Info je Disc). Neue Discs = drei weitere Einträge, alles andere läuft mit.
- Wandernder Händler: 26.3 datengetrieben (`data/<ns>/villager_trade/wandering_trader/*.json` + Tag
  `data/minecraft/tags/villager_trade/wandering_trader/{common,uncommon}.json`, Vorbild SimpleBuilding).
- **Verstärker-Logik** (SimpleBuilding-Kern, `util/SpeakerBoost.java`, `client/ChainedJukeboxSound`): Astralit
  (Musik-Verstärker) = weiterer Abspielpunkt, der Client hört **einen** Klang am **nächsten** Punkt (kein Phasenchaos);
  Nihilit (Noten-Verstärker) = Faktor 1 + n × 50 % (n ≤ 3, also ≤ 2,5) auf die **Reichweite** („Lautstärken über 1 heben
  nicht den Pegel, sondern die Reichweite“). Simple Shops darf SB-Klassen nicht importieren (A1) → gleiche Regeln, eigene
  kleine reine Logik in Simple Shops (Konstanten gleich: 50 %, max. 3, Deckel 2,5).
- **Seek** (Spätstart mitten im Stück): Simple Music hat einen Mixin auf `SoundBufferLibrary#getStream` für
  `simplemusic:sounds/seek/<s>/<ns>/<path>` (Taschen-Jukebox). Simple Shops darf ihn nicht nutzen (Modul-Grenze).
- **Simple Shops**: Verkaufsblock (`SalesBlockEntity`, Inhalte in Weltdaten `EnderShops`), Besitzer-Menü
  `SalesOwnerMenu` (allein: Preisreihe x 60–150/y 18 + 27 Einnahmen; kombiniert mit Vitrine: Waren/Preis/Einnahmen),
  Bildschirm `ShopScreens.Owner` (SimpleLib-Kastenstil). Spalte x 152 (9. Spalte) ist in Zeile y 18 in beiden Layouts frei.

## A12 – Vanilla-Nutzen und Eingriff
1. Nutzen: Läden in Dörfern/auf Servern bekommen Atmosphäre; jede Vanilla-Disc (und jede Mod-Disc mit `jukebox_playable`)
   funktioniert im Shop; neue Discs sind normale Discs für die Jukebox.
2. Änderung: ein zusätzlicher Platz im Besitzer-Menü des Verkaufsblocks; Musik nur clientseitig, nur in Reichweite.
3. Eingriff gering: kein Vanilla-Mixin am Spielgeschehen, nichts tickt auf dem Server; Schalter `shopMusic` (Server, Standard an),
   Reichweite `shopMusicRange` (8–48, Standard 24); Lautstärke über den Vanilla-Regler „Plattenspieler/Notenblöcke“.

## Entscheidungen
1. **Discs in Simple Music** (Musik-Mod, vorhandene Disc-Technik), **Abspielen in Simple Shops** über die Vanilla-Komponente
   `jukebox_playable` → beide Module bleiben allein spielbar; Simple Shops allein spielt Vanilla-Discs.
   | ID | EN | DE | BPM | Etikett | Komparator | Fundort |
   |---|---|---|---|---|---|---|
   | `browsing_bossa` | Browsing Bossa | Stöber-Bossa | 100 | Welle, Orange/Türkis | 2 | Savannen-Dorfhaus 5 % |
   | `coin_lounge` | Coin Lounge | Münz-Lounge | 94 | Münze, Gold auf Weinrot | 6 | Kartografen-Truhe 5 % |
   | `window_shopping` | Window Shopping | Schaufensterbummel | 108 | Markise, Rot/Creme auf Marine | 11 | Ebenen-Dorfhaus 4 % |
   Zusätzlich verkauft der **Wandernde Händler** (Tag `uncommon`) je Disc für 12 Smaragde (1 Handel). Kein Rezept (wie Vanilla).
   Eigene Melodie-Konturen (nur Beschreibung, keine Vorlage): Bossa = „fauler Sprung nach oben, dann schrittweise zurück“;
   Lounge = „kurze verspielte Zwei-Ton-Figuren mit Pausen“; Schaufensterbummel = „hüpfende Staccato-Töne, die aufwärts klettern
   und weich landen“. Je Disc 3 Takes (andere Lead-Instrumente: a Vibraphon/E-Piano/Marimba …), Besitzer wählt
   (Vorschau), eingebaut wird je Disc die Empfehlung; Wechsel = eine OGG tauschen.
2. **Nachbearbeitung** (ffmpeg + numpy, wie a-sound24): Loop-Überblendung (Ende 1,5 s in den Anfang), Hochpass 28 Hz,
   Höhen ab 6 kHz −3 dB, −17 LUFS, Spitze ≤ −1 dBTP, OGG Vorbis mono 44,1 kHz q≈3 (≤ 1 MiB je Disc). Songlänge im
   `jukebox_song` = Dateilänge (Loop-Zeitachse).
3. **SimpleLib-Baustein Seek** (`com.simplelib.api.client.SoundSeek` + Client-Mixin `SoundSeekMixin`):
   `simplelib:seek/<s>/<ns>/<path>` öffnet `<ns>:sounds/<path>.ogg` ab Sekunde s; mit `looping` nahtlose Schleife
   (Vanillas `LoopingAudioStream`, erster Durchlauf ab s). Gemeinsam für alle Mods (Simple Music kann später umziehen → TODO).
4. **Shop-Platz (Jukebox-Logik):** `SalesBlockEntity` hält `disc` + `discStart` (Spielzeit-Tick). Einlegen über den Platz im
   Besitzer-Menü (9. Spalte, Zeile 1, Geist-Disc als Hintergrund, Tooltip) oder Rechtsklick des Besitzers mit Disc in der
   Hand bei leerem Platz → spielt; Herausnehmen → Stille. Nur Items mit `jukebox_playable`, 1 Stück. Beim Abbau fällt die Disc
   heraus (wie Jukebox). Server-Shops (Op) ebenso. Die Disc gilt nur für diesen Shop.
5. **Synchron:** beim Einlegen übernimmt der Shop den Start-Tick eines geladenen Shops mit **derselben Disc** in ≤ 64 Blöcken
   (gleiche Dimension), sonst „jetzt“. Server sendet nur Disc + Start-Tick + Reichweite (Blockentity-Update), tickt nichts.
6. **Client (`ShopMusicClient`, alle 10 Ticks Suche, jeden Tick Lautstärke):** Shops mit Disc in ≤ 64 Blöcken (Block-Entities
   der geladenen Chunks). **Nächster Shop gewinnt** (Hysterese 2 Blöcke gegen Flattern); seine **Gruppe** = alle Shops mit
   gleicher Disc + gleichem Start-Tick. Ein Klang je Gruppe, Position = nächster Gruppen-Shop (wie Astralit),
   Reichweite = `shopMusicRange` × (1 + 0,5 × min(n − 1, 3)) (wie Nihilit, ≤ 2,5×). Lautstärke linear mit Abstand
   (eigene Rechnung, Dämpfung NONE, ≤ 1,0), 1 s Ein-/Ausblenden beim Wechsel. Außerhalb der Reichweite: kein Klang.
   Start ab Offset = (Spielzeit − Start) mod Länge (Seek), Schleife nahtlos. Musiknoten-Partikel über dem spielenden Shop
   (wie Jukebox, A14), clientseitig, nur in Hörweite.
7. Vanilla-Hintergrundmusik bleibt unberührt.

## Dateien
- simplelib: `api/client/SoundSeek.java`, `mixin/client/SoundSeekMixin.java`, `simplelib.mixins.json`.
- simplemusic: `disc/MusicDiscs.java` (+3), `jukebox_song/*.json`, `sounds.json`, `sounds/records/*.ogg`, `items/`, `models/item/`,
  Texturen + Ebenen (`tools/gen_textures.py` VDISCS + 3 Etikettformen), Lang EN/DE, Händler-Handel + Tag, `check_data.py`,
  Tests (6 → 9), Wiki `manual.json`.
- simpleshops: `block/SalesBlockEntity`, `block/SalesBlock`, `menu/SalesOwnerMenu`, `client/ShopScreens`, neu `logic/ShopMusic`
  (Server), `logic/ShopMusicPick` (reine Logik), `client/ShopMusicClient`, `client/ShopMusicSound`; `config/ShopsConfig`,
  Fabric-Client-Tick, Lang EN/DE, Wiki, `check_data.py`, Gametests + JUnit.
- `CREDITS.md`, `docs/MODS-UND-FEATURES.md`.

## Tests / Verifikation
- Gametests simpleshops: `shop_music_slot` (nur `jukebox_playable`, 1 Stück, Besitzer-Rechtsklick legt ein, Update-Tag trägt
  Disc/Start/Reichweite), `shop_music_sync_start` (zweiter Shop mit gleicher Disc übernimmt Start-Tick, andere Disc nicht,
  weit weg nicht), `shop_music_drops_disc` (Abbau → Disc fällt), `shop_music_switch_off` (Config aus → Platz nimmt nichts,
  Update ohne Disc). JUnit `ShopMusicPickTest`: nächster Shop, Hysterese, Gruppe/Verstärkung (≤ 2,5), Offset, Reichweite.
- simplemusic: `music_discs`, `music_disc_loot` (jetzt 9), Händler-Handel geladen.
- Lokal: precheck, `check_data.py` aller Module, `check_dup_impl`, `check_tooltip_text`, `wiki/generate.py --all --check`,
  `check_missing_textures`, `layers.py check`, `gen_textures.py --check`, texrate der 3 Disc-Texturen.
- Ein Lauf auf sb-test: Compile Fabric (simplelib/simplemusic/simpleshops), `module-simpleshops-fabric-263`,
  `module-simplemusic-fabric-263` (+ standalone je), `module-simplelib-fabric-263`, JUnit simpleshops.
- Nicht testbar ohne Client: Hörbild, Seek/Schleife im Spiel, Lautstärkeverlauf → Vorschau + Besitzer-Abnahme.

## Umsetzung / Plan-Abgleich (12.11.)
- [x] Discs: 9 Takes generiert (ElevenLabs Music v1, je 72 s, ~1 320 Credits), Loop-Verarbeitung `/root/sb-tools-a/shopmusic37/process.py`
  (Stille am Ende weg, 2 s Überblendung Ende→Anfang, −17 LUFS, ≤ −1,9 dBTP, mono OGG q3, 418–621 KB). Eingebaut je Disc die
  Empfehlung nach Messwerten (Schleifennaht, Fülle, Höhen): 1c (Flöte+Vibraphon, 69,9 s), 2a (E-Piano, 69,95 s), 3a (Marimba+Vibraphon, 69,5 s).
- [x] Texturen: 4 Runden gegen texrate (Härte Welle 15,2 → 13,3 → 12,8 → 10,9 %, Münze 12,3 → 11,8 %, Markise 12,3 → 10,4 %), alle grün.
  Hinweis: `gen_textures.py --check` meldet schon auf der Basis 70 veraltete Dateien (Pocket-Jukebox/Presse, fremde Überarbeitung);
  der Generator wurde deshalb nur für die drei neuen Discs übernommen, alle anderen Ausgaben zurückgesetzt.
- [x] Händler-Handel (Tag `wandering_trader/uncommon`) + Test in `music_disc_loot`.
- [x] SimpleLib `SoundSeek` + `SoundSeekMixin` (Simple Music behält vorerst seine eigene Kopie → TODO Umzug).
- [x] Simple Shops: Platz, Rechtsklick, Sync-Start, Drop, Config, Client-Auswahl/-Klang/-Noten, Wiki „shop_music“, Lang, Handbuch neu erzeugt.
- Abweichung: „auch im Shop-Menü abspielbar“ umgesetzt als Plattenplatz im Shop-Menü mit Jukebox-Logik (kein eigener Play-Knopf);
  im kombinierten Menü (mit Vitrine) geht Umschalt-Klick weiter in die Waren (Platte von Hand in den Platz ziehen).
- Abweichung: kein sb-test-Lauf (sb-test nicht erreichbar); stattdessen javac der geänderten Klassen gegen 26.3 (keine Fehler in
  eigenen Dateien) und die Unit-Logik per Reflection-Läufer (4/4 + Layout grün). Gametests offen.
- `wiki/generate.py --all --check` meldet nur `wiki/data/simpledimensions.*` (Basis-Drift, nicht angefasst).
