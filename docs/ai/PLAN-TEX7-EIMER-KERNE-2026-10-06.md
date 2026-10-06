# Plan Textur-Runde 7, Eimer und Kern-Animationen (2026-10-06)

Branch `claude-tex7` (von `47f392a63`, Queue „Nachtrag 11“). Nur committen, kein Push, kein Client.

## Ist-Zustand
- Hotbar `images/16`: `cracked_diamond_block`, `construction_light` (Besitzer-Originale, kein Generator),
  `enderite_spear` (`generate_textures.py`, Enderit-Ausrüstung B), `training_dummy` (Runde 6), `straw_armor_stand`
  (Heu-Recolor des Rüstungsständers), `backpack` (Leder), Kupfer-Eimer (`crucible_art_v2`, eckige Variante C).
- Eimer: `ModBucketItem` (Kinds COPPER/ENDERITE/IRON), Oxidation 0..3 als Komponente (nur Optik), Pfade:
  `use/scoop`, `BucketDispensing`, `CauldronBucketMixin`, `CrucibleCompat.SbCauldronBuckets`.
  Texturen `crucible_art_v2_2026_10_05.py`; Eisen-Seelen-Lava-Eimer hatte keinen Generator mehr (Remap ins
  helle `SOUL`-Ende → weiß/grell, `images/19`). Enderit-Eimer animiert den Inhalt.
- Kerne: Texturen 16x16 in der 26.3-Overlay (`cores_final_2026_10_02.py`, Hauptbaum `generate_textures.py`).
  Benutzung: serverseitige Partikel-Choreografie GLOW/ORBIT/BURST (70/20/10) in `BuildingCoreItem`,
  Erz-Osterei `CoreOreTransmutation`. Erste-Person-Haken: `HeldItemRendererMixin` (26.3 + 26.2-Zwilling).

## Umsetzung
1. **Hotbar-Texturen** `tools/textures/texture_round7_2026_10_06.py` (Varianten A/B in der Vorschau, eingebaut die
   bessere): Rissiger Diamantblock = Vanilla-Diamantblock-Facetten mit Rissen in dessen dunklen Tönen; Baulicht =
   gerahmte Leuchtscheibe (Vanilla-Glas/Laternen-Schattierung, hellblau); Strohständer = Rüstungsständer-Form in
   Heu-/Strohtönen mit Schnur-Bindungen statt Goldglanz; Rucksack = Bundle-Lederpalette, saubere Kontur, Riemen;
   Trainingspuppe = Runde-6-Motiv (Kürbis A) mit bereinigter Kontur. Speer: **nur** Glimmern (Form bleibt) –
   Kristall-Glanzpunkte + wandernder Glanz als Animation in `generate_textures.py` (Item + in_hand).
2. **Kupfer-Eimer** (Generator `crucible_art_v2`): Variante D = Vanilla-Eimer-Silhouette, Kupferpalette aus
   Vanilla-Kupferbarren/-blöcken, je Oxidationsstufe eigene Rampe (Kupfer, angelaufen, verwittert, oxidiert).
   **Logik**: Stufe 3 (voll oxidiert) schöpft nichts mehr (Welt, Kessel, Spender, Crucible); Ausgießen eines schon
   gefüllten bleibt erlaubt (kein eingesperrter Inhalt), Wachsen/Abschaben weiter möglich. Tooltip-Hinweis bei
   Stufe 3 (und „gewachst“).
3. **Keramik-Eimer** (neu, `McVersion.CRUCIBLE`, 26.3): `raw_ceramic_bucket` (3 Ton in Eimer-Form) → Ofen/
   Crucible (Schmelzrezept, Crucible nutzt Schmelzrezepte) → `ceramic_bucket`; `ceramic_water_bucket`.
   `ModBucketItem.Kind.CERAMIC`: nur Wasser (Entscheidung: keine Lava, keine Milch – Milch bräuchte Kuh-/Ziegen-
   Haken und Trinklogik, kein anderer Mod-Eimer kann Milch). Haltbarkeit 32 (`durability(32)`): Schöpfen und
   Ausgießen je 1, das 32. Mal (= 16. Ausgießen) zerbricht er. Kreativ: kein Verschleiß. Ausgegossenes Wasser
   wird eine Quelle (wie Eisen). Kein Rezept-Rest (sonst Reparatur per Crafting). Texturen Ton-Rotbraun
   (Terrakotta/Ziegel), roh in Tongrau. Lang EN+DE (beide Bäume), JEI-Info, Kreativ-Reihe, Wiki, Tests.
4. **Seelen-Lava-Eimer** (Eisen, Enderit): Vanilla-Lava-Eimer 1:1, jede Lava-Farbe auf eine Seelenfeuer-Farbe
   gleicher Helligkeitsstufe (dunkel-Petrol statt Rot, Türkis statt Orange, Hellcyan statt Gelb).
5. **Enderit-Eimer**: Inhalt statisch wie Vanilla; animiert wird ein diagonal wandernder Glanz über dem
   Enderit-Körper (8 Bilder, `frametime` 3) bei allen vier Enderit-Eimern.
6. **Kern-Texturen**: dezenter Schimmer als Animation (`.mcmeta`): ein Lichtpunkt läuft über die Fassungszacken,
   die Sternmitte glimmt kurz auf; Bild 0 = bisherige Textur (Besitzerwahl bleibt erkennbar).
7. **Kern-Animationen** (client, alle Loader, Erste Person): `CoreHandMotion` (gemeinsamer Zustand, keine
   Client-Typen) + Transformation im `HeldItemRendererMixin` (26.3) und im 26.2-Zwilling hinter
   `McVersion.CORE_MOTIONS` (26.3 an, 26.2 aus). Gewichtete Wahl beim Benutzen (Client-`use`/`useOn`):
   Pulsieren 45 %, Drehen 30 %, Aufsteigen 15 %, Bumerang 10 %. Erz-Umwandlung: Server schickt
   `CoreTransmutePayload` → eigene, längere „Schmiede“-Animation (aufsteigen, beschleunigt drehen, Schlag nach
   unten mit Nachfedern), Abklingzeit des Kerns dann so lang wie die Animation. Partikel-Choreografie bleibt.
   Client-Config `tools.enableCoreAnimations` (Standard an, zusätzlich zum Hauptschalter Werkzeug-Animationen).

## Risiken
- `durability` macht Keramik-Eimer unstapelbar (gewollt). Teilweise abgenutzt → Balken zeigt Rest.
- Transform-Mitte der Bewegungen ohne Client-Sicht nur rechnerisch geprüft (Vorschau-GIF der Kurven).
- Generatoren: Runde-6-Dummy und Kern-Overlay werden von Runde 7 übernommen (dort als „superseded“ markiert).

## Verifikation
- Generatoren `--check` (round7, crucible_art_v2, generate_textures), Vorschauen in `previews/`.
- GameTests: Kupfer Stufe 3 schöpft nicht (Welt/Kessel), Keramik nur Wasser, 32 Füllvorgänge → kaputt,
  Rezepte (3 Ton, Schmelzen), Gewichte/Dauer der Kern-Bewegungen, Kreativ-Reihe.
- Datagen 26.3, `fabric-263` + `neoforge-263` („alles gruen“), `gradlew check -q`, Wiki venv `--all` + uv `--all --check`.
- Nicht getestet: Client-Ansicht (kein Client erlaubt).
