# Plan B-REDSOUND22 (2026-10-22): eigene Klänge der Redstone-Linien (Queue SP22-5)

Branch `b-redsound22`. Besitzer 22.10.: Astral-, Nihil- und Brücken-Redstone klingen nach Amethyst; Redstone soll
eher ein „Pulver-Sound“ sein, Lampen dürfen amethystartig bleiben, Kolben sollen kolbenartig klingen.

## Ist-Zustand
- `ModBlocks.signalDust()` und `endLamp()` setzen `SoundType.AMETHYST`, die beiden End-Kolben ebenso
  (`ModBlocks` Z. 48/49). Nur 26.3 (`McVersion.END_SYSTEMS`).
- Kolben-Bewegung: `EndPistonMoves.fire` spielt Vanilla `PISTON_EXTEND` (Astral) / `PISTON_CONTRACT` (Nihil),
  Tonhöhe 1,2-1,3. Lampen-Umschalten: `EndSignalLampBlock.update` spielt `COPPER_BULB_TURN_ON/OFF`.
- Linien-Redstone-Blöcke kopieren `Blocks.REDSTONE_BLOCK` (METAL), Schienen `POWERED_RAIL` (METAL): wie Vanilla,
  bleiben so (Vanilla-nah; keine Amethyst-Klänge). Schalter/Knöpfe der Linien gibt es nicht mehr (claude-redlines).
- Vorhandene Synth-Werkzeuge: `tools/sounds/howler_dsp.py` (Körner, Moden, Raum, Pegel nach aktivem RMS),
  `make_howler_sounds.py`, `make_bore_sound.py`. Eigene Klänge der 26.3-Linie: `mc26_3/overlay/resources/assets/
  simplebuilding/sounds.json` (verdeckt die gemeinsame; `tools/audio/import_discs.py` ergänzt dort nur Einträge).

## Umsetzung
1. **Generator** `tools/sounds/make_redstone_line_sounds.py` (numpy/scipy/soundfile, feste Seeds, Mono 44,1 kHz Vorbis),
   nutzt `howler_dsp`. Schreibt `sounds/block/<block>/{dig,step}<n>.ogg` (+ `toggle_on/off`, `move`) in den
   26.3-Overlay und trägt die eigenen Ereignisse in die Overlay-`sounds.json` ein (übrige Einträge bleiben).
   - Staub (Astral/Nihil/Brücke): pulvriges Rieseln aus feinen Körnern + weicher „Pff“-Hauch, Richtung Sand/Kies,
     leise. Astral heller mit feinen Glas-Ticks, Nihil dunkler mit dumpfem Tupfer, Brücke dazwischen.
   - Lampen: Kristall-/Glas-Anschlag mit unharmonischen Teiltönen (amethystartig, aber eigen); Astral hell und
     glasig, Nihil tiefer und gedämpft mit Steinkörper. Eigener Ein-/Aus-Klang statt Kupferbirne.
   - Kolben-Block: Stein-Anschlag mit mechanischem Klacken (Vanilla-Kolben = Stein), Färbung je Linie.
   - Kolben-Bewegung: Variante A Synth (Klack + pneumatisches Zischen + Anschlag), Variante B Vanilla-Kolben-
     Ereignis per `"type": "event"` mit eigener Tonhöhe (Astral 1,2 heller, Nihil 0,85 dumpfer).
   - Lampe Variante B: Vanilla-Amethyst-Ereignisse mit eigener Tonhöhe (nur Vorschau-Vergleich).
   - Eingebaut (Empfehlung): Lampe A (eigen, Wunsch „eigene Geräusche“), Kolben-Bewegung B (Wunsch „ähnlich zu den
     Pistons“). Umschalten nur in der Tabelle des Generators (`--piston-move`, `--lamp`), keine Codeänderung.
   - Pegel nach Vanilla gemessen (aktiver RMS, 26.3-Assets): Sand-/Kies-Abbau -37/-28 dB, Stein -20 dB,
     Amethyst -26 dB, Kolben -24/-21 dB, Kupferbirne -29 dB. Ziele: Staub -30, Lampe -25, Kolben -21/-23.
2. **Java** `com.simplebuilding.util.LineSounds`: unregistrierte `SoundEvent`-Objekte + `SoundType` je Block-Art
   (Staub ×3, Lampe ×2, Kolben ×2), dazu Lampen-Ein/Aus und Kolben-Bewegung je Linie. `ModSounds` registriert sie
   unter `END_SYSTEMS` (keine Registry-Zugriffe aus `ModBlocks`, Reihenfolge der Loader egal).
   `ModBlocks`: Staub/Lampen/Kolben bekommen ihre `SoundType`; `EndSignalLampBlock` und `EndPistonMoves` spielen
   die eigenen Ereignisse. Untertitel: Abbau/Setzen/Schritte nutzen die Vanilla-Schlüssel `subtitles.block.generic.*`;
   eigene Untertitel EN/DE für Lampe an/aus und Kolben-Bewegung (Overlay-Sprachdateien).
3. **Tests** (`RedstoneLinesTests`, Fabric-Adapter + Katalog): `line_blocks_have_own_sounds` prüft je Linien-Block die
   erwartete `SoundType` (nie AMETHYST), jedes Ereignis registriert, jedes in der `sounds.json` mit Untertitel,
   jede eigene Datei vorhanden (Ogg-Kopf), eigene Untertitel in EN/DE.
4. **Vorschau** `scratchpad/redsound22-preview.html`: je Block-Art alt (Amethyst) neben neu, Kolben/Lampe A/B.

## Prüfung
Gezielt `simplebuilding:redstone_lines*` und `simplebuilding:end_systems*` auf fabric-263/neoforge-263, Compile aller
26.3-Loader + `:compileJava` (26.2), `tools/check_missing_textures.py` (Sound-Dateien), danach Generatoren
(Guides, Wiki, Discover) und `./gradlew check -q`. Nicht prüfbar ohne Client: Höreindruck im Spiel (Besitzer).
