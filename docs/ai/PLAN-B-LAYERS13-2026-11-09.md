# Plan B-LAYERS13 (09.11.2026) – Simple Layers: Texturen R4–R6 + Backrooms-Mechanik

Branch `b-layers13`, Basis `b-layers12` (34428619f). Nur 26.3 Fabric. Auftrag: /root/b-auftrag-20261109.md Abschnitt N5
(Besitzer LAYERS12 = „noch ein paar Runden nachbessern“ + Backrooms-Mechanik), Vorschau LAYERS13.

## Ist-Stand
- Texturen: Generator `tools/textures/layers10_2026_11_08.py` (Stand R3 aus b-layers12). Harte Ausreißer laut R3-Bewertung:
  EXIT-Schild (Schrift 150/255/160 auf fast schwarzem Grün, Rahmen ohne Fase), Riss (Leuchtlinie fast weiß auf schwarz,
  Funken wie Rauschen), Grundgestein-Glitch (5 Töne mit vollem Spreizen 30…146), Items (Kontur ok, aber Flasche ohne
  Glanzkante, Schlüssel-Bart zu dünn), Tür-Glas, Neonröhre (reines Weiß).
- Backrooms (`BackroomsGrid`): Grundgestein nur in y 0 und y 8 (je eine Lage), darüber Luft bis y 63 (Dimension 0..64).
  Wände = `BackroomBlock` (Tapete/Teppich/Decke/Neon/EXIT/Tür); Abbau-Schutz bisher per Dimension (`protectedFor`,
  Config `backrooms.unbreakableWalls`). Gewöhnliche Kolben schieben Wände bisher (Lücke). Keine Holz- oder Fadenquelle
  außer Fässern/Kisten im Lagerraum (Loot ohne Holz/Faden).
- Netherit-Kolben: gibt es in Simple Machines (`simplemachines:netherite_piston`, `NetheriteBreakerPistonBlock`): bricht
  beim Ausfahren den Block davor (Härte ≤ Signal/15·50, nicht `IMMOVEABLE`), unabhängig vom Spieler – ohne Bezug auf den
  Backroom-Effekt. Prüft dabei nicht `PistonBaseBlock.isPushable`.

## Entscheidungen (selbst, begründet)
1. **Hülle = Boden y 0 + massive Decke y 8 bis Bauhöhe (y 63).** Vorher lag über der einlagigen Decke 55 Blöcke Luft:
   wer die Decke durchbricht (bezahlter Netherit-Durchbruch von Simple Machines), läuft über das Labyrinth. Jetzt ist
   alles oberhalb Grundgestein; der Boden y 0 ist die untere Weltgrenze (darunter Leere wie in jeder Dimension). Seitlich
   ist das Labyrinth endlos – Außenrand = Vanilla-Weltgrenze; jedes Raumsegment ist oben/unten vom Grundgestein, seitlich
   von geschützten Wänden umschlossen. Hülle je Raumsegment (dicke Bedrock-Wände) verworfen: Wände sind 1 Block dick und
   müssen Tapete zeigen. Höhe der Dimension bleibt 64 (Änderung würde bestehende Welten brechen). Alte Chunks behalten
   die Luft über der Decke (nur neue Chunks; unsichtbar).
2. **Effekt `simplelayers:backroom`** (neutral, Farbe Tapetengelb, eigenes 18×18-Icon). Server-Tick alle 10 Ticks: jeder
   Nicht-Zuschauer in den Backrooms bekommt ihn (40 Ticks, ambient, ohne Partikel, mit Icon), solange
   `backrooms.unbreakableWalls` an ist; draußen läuft er in ≤2 s aus. Schutz hängt am Effekt (`BackroomBlock.protectedFor`:
   Überleben + Effekt), nicht mehr an der Dimension → sichtbar (A14, Icon + Name) und per `/effect clear` testbar.
   Zusätzlich Aktionsleisten-Hinweis beim Schlag gegen eine geschützte Wand (A14). Grundgestein ist ohnehin unzerstörbar.
3. **Kolben:** In den Backrooms (Config an) sind Wandblöcke für `PistonBaseBlock.isPushable` unbeweglich (Mixin in Simple
   Layers, nur Dimension + `BackroomBlock`) – gewöhnliche/verstärkte Kolben schieben sie nicht mehr. Der Netherit-Kolben
   bricht den Block davor ohne `isPushable` → Wände bleiben mit ihm entfernbar (Drop wie Abbau). Keine Abhängigkeit zu
   Simple Machines: kein Import, die Ausnahme ergibt sich aus dessen eigener Brech-Regel; der Gametest sucht den Kolben
   per ID und überspringt ohne Simple Machines (Standalone).
4. **Holz/Faden im Raster** (statt neuer Strukturen – deterministisch, im Chunk-Test mitgeprüft, per Config schaltbar):
   - „Büro“-Zelle (5 %): Schreibtisch (2 obere Eichenstufen) + Stuhl (Eichentreppe) in einer Ecke, Holzstapel (3 entrindete
     Eichenstämme = 12 Bretter) in der Gegenecke, Werkbank (50 %) in einer dritten Ecke.
   - „Spinnweben“-Zelle (4 %) und jede dunkle Zelle: Spinnweben in den Ecken unter der Decke (Faden mit Holzschwert).
   - Ecken (2×2) liegen nie in Türen (Versatz 3–5) und nie in der Ankunftsmitte.
   - Lagerraum-Loot zusätzlich Faden (2–5) und Stöcke (2–6).
   - Config `backrooms.supplies` (Standard an).
   - A12: (1) Nutzen: Überleben/Craften in einer sonst leeren Dimension (Werkzeuge, Fackeln, Bogen/Angel). (2) Änderung: nur
     Inhalt der Backrooms-Dimension, Vanilla-Blöcke. (3) Kein Eingriff ins Vanilla-Geschehen außerhalb; abschaltbar.
5. Config: `backrooms.unbreakableWalls` schaltet jetzt Effekt + Kolben-Schutz (Tooltip angepasst), neu `backrooms.supplies`.
6. **Texturen R4–R6** auf Basis R3, Schwerpunkt Ausreißer (EXIT-Schild, Riss, Grundgestein-Glitch, Items, Tür-Glas, Neon);
   Flächen (Wolke/Tapete/Teppich/Decke) nur, wenn die Bewertung etwas findet. Effekt-Icon in denselben Runden.

## Vorgehen
1. Code: `BackroomEffect` + Registrierung, Tick in `LayersFabric`, `protectedFor` auf Effekt, Mixin `PistonBaseBlockMixin`
   (+ `simplelayers.mixins.json`, fabric.mod.json), `BackroomsGrid` (Hülle bis `HULL_TOP_Y`=63, Vorräte), Generator-Schleife
   bis Hülle, Config + Lang EN/DE, Loot in `gen_data.py`, Handbuch-Seiten (LayersGuide) + wiki/manual.json.
2. Gametests (Katalog `LayersTests.ALL` + Fabric-Adapter): `backroom_effect_blocks_mining`, `netherite_piston_moves_wall`,
   `bedrock_hull`, `supplies_generated`; `backrooms_chunk_matches_grid` prüft bis y 63.
3. Texturen: je Runde Kontaktbogen 1×/8× neben Vanilla (Jar), Raumkontext, Härte/Tonanzahl, Bewertung hier + Vorschau.
4. Vorschau `/root/sb-tools-b/previews/b-layers13-preview.html`: R3→R6 mit Bewertung, Effekt-Icon, Skizzen der Hülle und
   der Holz/Faden-Zellen (gerendert aus `BackroomsGrid`-Nachbau in Python), Antwortcode LAYERS13.
5. Test: ein sbt (Compile + module-simplelayers fabric + standalone + data_integrity-Filter + Wiki/Guides erzeugen).

## Runden-Bewertung
Je Runde: Kontaktbogen 1×/10× neben Vanilla (verdant_froglight, nether_portal, bedrock, potion, trial_key, Effekt-Icons
mining_fatigue/bad_omen/weaving), Raumansicht, Härte (A8b) und Tonanzahl. Scratch /root/sb-tools-b/layers13/r3…r6.
- **R3 (Ausgang):** Flächen 0–3 % hart, Vanilla-nah – bleiben. Ausreißer: EXIT-Schild (Schrift von Rand zu Rand, flacher
  Rahmen ohne Fase, Glow-Zeile), Riss (fast weiße Linie, verrauschte Funken), Glitch-Grundgestein (Fehler nur 1 Pixel),
  Flasche ohne Glanz.
- **R4:** EXIT mit gefastem Gehäuse, schmalem I (Wort mittig), Buchstaben mit dunklerer Unterkante; Halo oben/links wirkt
  aber wie verkehrter Schatten. Riss gedämpftes Gold, 2 Funken – gut; Hintergrund mit waagrechten Streifen (Holz-Eindruck).
  Glitch: verrutschte Bildzeilen in 2 Animationsbildern. Flasche 2 Glanzpixel. Effekt-Icon A (Wand mit Tür) liest sich wie
  Hocker/Tor.
- **R5:** EXIT ohne Halo, 1 px Tiefe unter dem Rahmen – ruhig wie Redstone-Lampe. Riss-Hintergrund Einzelpixel, 2 Töne.
  Icon B (Türen ineinander = endlose Räume) in 1× klar besser als A; innerer schwarzer Rahmen zu hart.
- **R6 (eingebaut):** Icon B mit Umriss nur außen, innere Rahmen in Tapeten-Schattentönen. Härte EXIT 21 % (Redstone-Lampe
  48 %), Riss 26 %, Glitch 32 % (Grundgestein 47 %), Flasche 21 % (Trank 23 %), Icon 33 % (Vanilla-Icons 0–49 %).
  Schlüssel, Tür, Neon geprüft und unverändert (lesbar, Vanilla-nah).

## Abweichungen
- Effekt-Icon liegt im selben Generator (`mob_effect/backroom`, 18×18) statt einem eigenen Skript.
- Holz/Faden nicht als neue Jigsaw-Räume, sondern im Raster (Entscheidung 4): deterministisch, vom Chunk-Test mitgeprüft
  und per Config schaltbar; Lagertruhen-Loot zusätzlich mit Faden/Stöcken.

## Ergebnis
Vorschau /root/sb-tools-b/previews/b-layers13-preview.html, Antwortcode LAYERS13.
