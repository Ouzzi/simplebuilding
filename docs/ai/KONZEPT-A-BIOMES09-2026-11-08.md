# Konzept A-BIOMES09 (08.11.2026) – Wolken-Biom (y ≈ 700) und Backrooms unter dem Grundgestein

Queue: **IG31-56** „Wolken-Biom + Backrooms (Konzept)“. Branch `a-konz09` (Basis `claude-wave1` 8dd095555). Nur Konzept, kein Code.
Vorschau: `/root/sb-tools-b/previews/a-konz09-preview.html`. Antwortcode: **`KONZ09: wolke=A|B|C, backroom=A|B|C + Anmerkung`**.

Besitzer-Wunsch 08.11. (sinngemäß): „Da es in Minecraft Layered Biomes gibt, soll es zwei verschiedene Zusatz-Biome geben. Bitte beide Konzepte mit
Schwierigkeiten auflisten.“ (1) riesiges Wolken-Biom erst ab etwa y 700, Endgame-Ziel; Prototyp-Blöcke Wolkenblock, Nebelblock, Wolkenstein, alles
instabreak; Inseln außen Wolke, Kern Wolkenstein, Nebel-Fallgruben; Wolke −30 % Fallschaden, Wolkenstein −10 %. (2) Backroom-Bereich unter dem
Grundgestein, aufgebaut ungefähr wie Trial Chambers, einige einzigartige Blöcke, leer, riesig, verlassen, mit eigenem Konzept.

**Einordnung Phase:** beide sind neue Welt-Inhalte (Worldgen, Dimension/Weltgrenze) und gehören nach `PLAN-B-PHASE01-2026-11-03.md` **nach den
Release** (Phase 4, `IDEEN-PHASE4.md`). Vor dem Release höchstens die drei Wolken-Prototyp-Blöcke als Vorschau (Texturen nach A8/A8b), ohne Welt-Generierung.

---

## 0. Ist-Zustand (Retrieval-First)

| Was | Befund | Beleg |
|---|---|---|
| Vorhandene Wolken-/Backroom-Ideen | keine. `grep wolke|cloud|backroom|sky` trifft nur Simple Earth (Wolken im Orbit ausblenden) und Fremdtexte. | `docs/ai/KONZEPT-SIMPLE-EARTH-2026-10-23.md`, `simpleearth/.../mixin/client/EarthCloudMixin.java` |
| **Simple Earth** (in Production) | Orbit-Aktivierung **y 1 024**, Render-Umschaltung y 1 536, Config-Regel „Aktivierung ≥ Bauhöhe + 64“. Ein Wolken-Bereich bei y 700 liegt **unter** der Aktivierung – beide müssen sich vertragen. | `simpleearth/.../orbit/OrbitTracker.java:31` (`activationY = 1024, switchY = 1536`), Konzept Earth §2.1 |
| **Simple Dimensions** | erzeugt Dimensionen per generiertem Datapack (`GeneratedPack` + `DimensionPackMixin` auf `BuiltInPackSource.loadPacks`), Presets `skyblock`, `mining`, `travel`; prüft schon `min_y`/`height` (Vielfache von 16, `min_y + height ≤ 2032`, Höhe ≤ 4064). Portale/Teleport-Cooldowns/Rückweg vorhanden. | `simpledimensions/shared/java/dev/simpledimension/common/config/DimensionConfigStore.java:142–204` |
| **Simple End** | eigene Biome per Mixin auf `TheEndBiomeSource` (inkl. `collectPossibleBiomes` und Holder aus der Server-Registry – sonst Biom-ID −1 im Chunk-Paket). Muster für ein „Höhen-Biom“ wiederverwendbar. | `simpleend/.../mixin/TheEndBiomeSourceMixin.java` |
| Simple Weather | nur Wetterschalter/Regendichte, kein Bezug. | PRINZIPIEN Teil 2 |
| Sounds | ElevenLabs-Weg existiert (`tools/sounds/eleven_howler_takes.py`, Key `ELEVENLABS_API_KEY`), Herkunft in `CREDITS.md`. | Memory `elevenlabs-key.md` |
| Prinzipien | A8/A8b Texturen (Vanilla-nah, Ebenen, ~7 % hart), A9 Server-Autorität mit Grenzen, **A12** Vanilla-Nutzen/Eingriff, **A14** Wirkungen telegrafieren, Modul allein spielbar. | `docs/ai/PRINZIPIEN.md`, `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md` |

### 0.1 Technische Fakten 26.3 (aus dem dekompilierten Spiel `/root/mc263src`)

| Fakt | Wert | Stelle |
|---|---|---|
| Y-Bits | `PACKED_Y_LENGTH = 64 − 2·26 = 12` | `core/BlockPos.java:48` |
| Höhe gesamt | `Y_SIZE = 4096 − 32 = 4064` | `world/level/dimension/DimensionType.java:48` |
| Grenzen | `MAX_Y = 2031`, `MIN_Y = −2032`, `min_y + height ≤ 2032`, `height`/`min_y` Vielfache von 16, `height ≥ 16` | `DimensionType.java:46–77, 90–92` |
| Overworld Vanilla | `min_y −64`, `height 384` (Oberkante 320), Rausch-Gitter `NoiseSettings.create(−64, 384)` | `data/worldgen/DimensionTypes.java:54`, `levelgen/NoiseSettings.java:19` |
| Weltgenerierung bei größerer Dimension | `WorldGenerationContext.minY = max(Dimension, Generator)`, `height = min(...)` → Terrain bleibt im Rausch-Bereich, darüber/darunter Luft. Bedrock-Boden folgt dem **Rausch**-Boden, nicht der Dimension. | `levelgen/WorldGenerationContext.java:19–21` |
| Schwebende Inseln (Vanilla) | Preset `minecraft:floating_islands` (`NoiseSettings.create(0, 256)`, eigener Noise-Router) – per Datapack nutzbar. | `levelgen/NoiseGeneratorSettings.java:68, 190`, `NoiseSettings.java:23` |
| Wolkenhöhe Vanilla | 192,33 (Umgebungs-Attribut `visual/cloud_height`, je Biom/Dimension setzbar) | `dimension/DimensionDefaults.java:12`, `world/attribute/EnvironmentAttributes.java:71` |
| Atmosphäre datengetrieben | `visual/fog_color`, `fog_start/end_distance`, `visual/ambient_particles`, `audio/ambient_sounds`, `audio/background_music` – alles Umgebungs-Attribute (Biom/Dimension, Datapack) | `EnvironmentAttributes.java:19–124` |
| Trial Chambers | `JigsawStructure`, Start-Pool `trial_chambers/...`, `size 20` (= `MAX_DEPTH`), Höhe `uniform(−40, −20)`, `MaxDistance(116)` (Obergrenze `MAX_TOTAL_STRUCTURE_RANGE = 128`), `TerrainAdjustment.ENCAPSULATE`, alle Mob-Spawns überschrieben (leer) | `data/worldgen/Structures.java:363–386`, `levelgen/structure/structures/JigsawStructure.java:33–41` |
| Fallschaden je Block | `Block.fallOn` → `causeFallDamage(fallDistance·(1 − fallDistanceReduction), 1.0)`; Heuballen überschreibt `fallOn` mit **Schadens**-Faktor 0,2; Property `fallDistanceReduction(f)` kürzt die **Strecke** (Bett 0,5). | `level/block/Block.java:486–501`, `HayBlock.java:18`, `Blocks.java:788` |
| instabreak | `BlockBehaviour.Properties.instabreak()` | `state/BlockBehaviour.java:1160` |
| Kompass/Uhr | drehen sich in Dimensionen mit `natural = false` (Vanilla-Verhalten) – nützlich für „Verirren“. | `DimensionType` Feld `natural` |

**Wichtig für die Fallschaden-Wünsche:** −30 % *Schaden* ist nicht dasselbe wie −30 % *Fallstrecke* (die ersten 3 Blöcke sind frei).
Vorschlag: wie Heuballen `fallOn` mit Schadens-Faktor **0,7** (Wolke) bzw. **0,9** (Wolkenstein) – genau, was der Besitzer gesagt hat.

**Zum Begriff „Layered Biomes“:** Vanilla-Biome sind seit 1.18 dreidimensional (Biom je 4×4×4-Zelle, Höhe über den Klima-Parameter `depth`; Beispiel
Tiefes Dunkel, Tropfsteinhöhlen, üppige Höhlen). Ein Biom „nur ab y 700“ ist damit grundsätzlich möglich; die Overworld-Parameterliste kann man
aber nur als Ganzes per Datapack ersetzen (bricht Biom-Mods wie Terralith/TerraBlender), daher besser per Mixin wie Simple End (§0).

---

## 1. Wolken-Biom „Wolkenmeer“

### 1.1 Gemeinsamer Kern (gilt für alle Varianten)

**Blöcke (Prototyp):**

| Block | Kollision | Abbau | Fallschaden | Drop | Licht | Hinweis (A14) |
|---|---|---|---|---|---|---|
| **Wolkenblock** | voll | instabreak, Hand | ×0,7 (`fallOn`) | sich selbst | durchlässig, kein Schatten | Tooltip „−30 % Fallschaden“ |
| **Wolkenstein** | voll | instabreak, Hand | ×0,9 | sich selbst | normal | Tooltip „−10 % Fallschaden“ |
| **Nebelblock** | **keine** (`noCollision`), nicht ersetzbar (sonst überschreibt Bauen den Nebel), nicht erstickend, nicht sichtblockierend | instabreak | – (man fällt durch) | nichts, nur mit Behutsamkeit | durchlässig | sieht fast aus wie Wolke, **aber**: leicht durchscheinender Rand + seltene herabsinkende Dunst-Partikel → aufmerksame Spieler erkennen die Grube (A14, Config `fogHint`, Standard an) |

**Insel-Aufbau:** Hülle 2–4 Blöcke Wolkenblock, Kern Wolkenstein, Oberseite flach-wellig, Unterseite tropfenförmig. Fallgruben: Nebel-Pfropfen
(1×1 bis 3×3, 2–6 tief), darunter Hohlraum → man fällt durch die Insel oder auf eine tiefere Lage. Grube nie über dem Insel-Rand, damit man
nicht ins Bodenlose fällt (Fall von 700 auf ~64 = tödlich) – außer bei Absicht „Schwierig“ (Config).

**Biom:** eigenes Biom `wolkenmeer` mit hellem Nebel (`visual/fog_color`), Wolkenhöhe unter dem Spieler (`visual/cloud_height` z. B. 690, damit
Vanilla-Wolken als „Meer“ unter den Inseln liegen), leiser Wind (`audio/ambient_sounds`), kein Regen (Niederschlag aus).
Keine Mob-Spawns zum Start (nur Phantome nachts wie überall – A12: keine neuen Gegner nötig).

**Erreichbarkeit:** Elytra + Raketen (nach dem End) ≈ 1 Minute bis y 700 → „Endgame“ ohne neue Mechanik. Hochtürmen geht (Gerüst, Blöcke),
ist aber mühsam (≈ 640 Blöcke) – gewollt. Optionaler eigener Weg (später): „Aufwind“-Säule über einem Wolkenstein-Brocken, der auf dem Boden
selten landet (Teil von Variante B Stufe 3, nicht im Prototyp). Rückweg: Elytra, Wasser, oder Fallen auf Wolken-Lagen.

**Belohnung (Vorschlag, klein):** Wolkenblöcke als leichte Baumaterialien (Heuballen-ähnlich, aber schön), Wolkenstein als Deko-Stein,
seltene „Himmelstruhe“ mit Vanilla-Loot (Elytra-Reparatur-Material wie Phantomhaut, Raketen, Schallplatte). Kein neues Erz.

**instabreak-Risiken:** (a) Versehentliches Abbauen des Bodens unter einem beim Kämpfen/Klicken → Absturz; Gegenmittel: der Wolkenstein-Kern trägt;
eine Abbau-Sperre unter den Füßen wäre zu viel Eingriff, daher nur Hinweis im Handbuch. (b) Griefing im
Mehrspieler: Inseln sind in Sekunden zerlegt; Claims (SimpleBuilding) schützen wie überall. (c) Baumaterial-Missbrauch: unendlich, leicht, sofort
abbaubar → ideal für Gerüste; unkritisch, Vanilla hat Gerüst/Heu. Nebel als „falscher Boden“-Falle im PvP: erlaubt, Nebel droppt sich nicht ohne Behutsamkeit.

### 1.2 Varianten

#### Wolke A – Overworld höher machen + echtes Höhen-Biom (wörtlich am Wunsch)
- Datapack/Mod überschreibt `dimension_type minecraft:overworld`: `min_y −64`, `height 960` → Oberkante **896** (Vielfaches von 16). Rausch-Gitter bleibt
  `−64/384` (Terrain unverändert, kein Mehraufwand bei der Terrain-Erzeugung).
- Biom `wolkenmeer` per Mixin in die Overworld-Biomquelle ab `quartY ≥ 700/4` (Muster Simple End, inkl. `collectPossibleBiomes`).
- Inseln als **eigener Struktur-Typ** (Code, prozedurale Teile je Chunk wie Ozeanmonument), weil Features nur 3×3 Chunks erreichen und das Rausch-Gitter nicht bis 700 reicht. Größe 48–160 Blöcke, Abstand per `structure_set` (z. B. Spacing 24, Separation 8 Chunks), mehrere Lagen 700–860.

| Schwierigkeit | Aufwand | Risiko |
|---|---|---|
| Bestandswelten: alte Chunks bekommen die neuen Sektionen leer dazu (oben wachsen ist harmlos), Höhenkarten haben neue Bit-Breite (9 → 10 Bit) und werden neu berechnet; Inseln nur in **neu** erzeugten Chunks → in alten Gebieten keine Wolken | M | mittel |
| Performance: 24 → 60 Sektionen je Chunk. Leere Sektionen kosten wenig (Einwert-Palette, nicht gerendert, kein Random-Tick), aber Chunk-Pakete, Lichtdaten (Himmelslicht bis 896) und Speicher wachsen messbar; Messung nötig (Simple Optimization) | M | mittel |
| Kompatibilität: jedes andere Datapack/Mod, das `minecraft:overworld` überschreibt (Tall-World-Packs, Tectonic „increased height“), kollidiert – der letzte gewinnt. Mods mit fest verdrahtetem 320 (Minimaps, Luftschiff-Mods) zeigen Fehler | – | **hoch** |
| **Simple Earth**: Regel „Aktivierung ≥ Bauhöhe + 64“ verschiebt die Orbit-Aktivierung von 1 024 auf ≥ 960 (passt noch), Wolken-Lagen bis 860 liegen darunter – vertragen sich, Earth muss die neue Bauhöhe aber lesen statt 320 annehmen | S | niedrig |
| Licht: Himmelslicht-Berechnung über 60 Sektionen; Himmelslicht fällt senkrecht: große Inseln verdunkeln den Boden 636 Blöcke tiefer (Mob-Spawns im Schatten!) → Wolkenblock lichtdurchlässig machen, Kern-Wolkenstein klein halten | S | mittel |
| Abschaltbar? Höhe einer bestehenden Welt zurückdrehen zerstört alles über 320 – nur beim Welt-Anlegen wählbar | – | hoch |

Aufwand gesamt **L**, Risiko **hoch** (Weltgrenze der Hauptwelt).

#### Wolke B – eigene Himmels-Dimension mit Übergang ab y 700 (**Empfehlung**)
- Neue Dimension `wolkenmeer` (Typ: `min_y 0`, `height 512`, Himmelslicht an, `natural true`, eigene Wolkenhöhe), Generator mit Vanilla-Preset
  **`floating_islands`**-Router, angepasst (Datapack-JSON: Inseln dünner, Lücken größer) + Oberflächenregeln Wolke/Wolkenstein + Nebel-Gruben als
  Feature. Biom-Quelle `fixed` oder `multi_noise` mit 2–3 Unterbiomen (dicht, zerrissen, Gewitterwolken später).
- **Übergang:** steigt eine Entität in der Overworld über **y 700** (Config 400–1 000, A9), blendet Nebel ein und sie landet in `wolkenmeer` auf y ≈ 40
  an derselben x/z (Maßstab 1:1). Fällt man in `wolkenmeer` unter y 0, kommt man in der Overworld bei y 680 an derselben Stelle heraus, mit
  3 s Sanftfall (A14: Hinweis „Du fällst aus den Wolken“). Teleport-Logik, Cooldowns und Rückweg-Schutz aus Simple Dimensions wiederverwenden
  (Kopie oder optional, siehe §3 – Modul muss allein laufen).
- Von unten sieht man die Inseln nicht – bei 636 Blöcken Abstand ohnehin außerhalb der Sichtweite (12 Chunks = 192 Blöcke), also kein Verlust gegenüber A.

| Schwierigkeit | Aufwand | Risiko |
|---|---|---|
| Overworld bleibt unverändert → Bestandswelten, Datapacks, andere Mods unberührt; Wolken auch in alten Welten erreichbar | – | niedrig |
| Übergang muss sauber sein: Elytra-Flug, Reiten, Boote, Projektile, Items (Entitäten über 700 nicht teleportieren außer Spieler + Reittier), Chunks am Ziel vorladen (Ticket), kein Pendeln an der Grenze (Hysterese 20 Blöcke) | M | mittel |
| **Simple Earth**: Overworld-Spieler, die zum Orbit (1 024) wollen, werden bei 700 abgefangen. Lösung: in `wolkenmeer` über der Oberkante (y > 512) wird an Simple Earth übergeben (dort Orbit-Ansicht für die Overworld-x/z) – nur wenn Earth geladen ist; sonst weiche Decke. Erfordert kleine Schnittstelle in SimpleLib (Vertrag „Höhenübergabe“), kein Import zwischen den Modulen | M | mittel |
| Performance: eigene Dimension tickt nur mit Spielern darin; 32 Sektionen, `floating_islands`-Router ist billig | S | niedrig |
| Andere Mods: Dimension-Listen (Karten-Mods, Wegpunkte) zeigen eine neue Dimension – normal | S | niedrig |

Aufwand gesamt **M–L**, Risiko **mittel**. Abschaltbar (Übergang aus = Dimension unerreichbar, Welt bleibt heil).

#### Wolke C – Wolkeninseln in Vanilla-Höhe (Kleinlösung)
- Seltene Wolkeninseln als Struktur bei **y 250–310** in der normalen Overworld, eigenes Biom nur innerhalb der Struktur (oder kein Biom).
- Keine Weltgrenzen-Änderung, keine Dimension. Erreichbar schon im Mittelspiel (Türmen 200 Blöcke).

| Schwierigkeit | Aufwand | Risiko |
|---|---|---|
| widerspricht „erst ab 700 / Endgame“; Inseln sind vom Boden aus sichtbar (Sichtweite) – eher „Deko-Insel“ als Biom | S–M | niedrig |
| Größe begrenzt durch Bauhöhe (max. ~70 Blöcke dick), Vanilla-Wolken (192) liegen darunter | – | niedrig |

Aufwand **S–M**, Risiko **niedrig**. Gut als **Stufe 1 / Testfeld** für die Blöcke, dann B.

### 1.3 A12 Wolke
1. **Vanilla-Nutzen:** gibt Elytra/Raketen nach dem End ein neues Ziel; Fallschaden-Blöcke als Landepolster (Heu-Ersatz mit schönerem Look). 
2. **Änderung:** neuer, erreichbarer Ort; neue Bau-/Deko-Blöcke; keine neuen Gegner. 
3. **Eingriff:** A = **stark** (Hauptwelt-Höhe, nur beim Anlegen); B = **gering–mittel** (eigene Dimension, abschaltbarer Übergang); C = **gering**.

### 1.4 Empfehlung Wolke
**B**, mit **C als Stufe 1** (Blöcke + kleine Inseln testen, Texturen abnehmen), danach Dimension. A nur, wenn der Besitzer ausdrücklich die
Overworld-Höhe will (dann Option nur beim Welt-Anlegen).

---

## 2. Backrooms „Die Zwischenräume“ unter dem Grundgestein

### 2.1 Eigenes Konzept (Kern-Idee)
Die Backrooms sind der Ort, an dem man **„aus der Welt rutscht“** (Noclip). Wer durch das Grundgestein bricht, fällt nicht in die Leere,
sondern landet in endlosen, leeren, gelblichen Büroräumen mit Teppichboden, Tapete und summendem Neonlicht. Kein Himmel, kein Tag, Kompass und
Uhr drehen sich. **Ziel: einen Ausgang finden.** Unterwegs: wenige, gute Funde, Mandelwasser, und ein Gefühl von Beobachtet-Werden.

**Eigene Mechanik „Summen“ (Orientierung + Spannung):** Das Neon-Summen ist überall gleich laut – außer in Richtung eines Ausgangs, dort wird es
**einen Hauch tiefer** (Tonhöhe, nicht Lautstärke). Wer genau hinhört (oder Untertitel liest: „Summen (tiefer, links)“), findet heraus.
Das ist Orientierung durch Atmosphäre statt Kompass – passt zu A14 (Wirkung an der Quelle erkennbar) und ist barrierearm über Untertitel.

**Eingang (Durchbrechen):** selten im Bedrock-Boden der Overworld ein **„Fehlerhaftes Grundgestein“** (sieht aus wie Grundgestein, flackert alle
paar Sekunden ein einzelnes Pixel ins Gelbe – A14). Nur mit Netherit-Spitzhacke abbaubar (≈ 30 s, Endgame). Darunter Leere → Fall → Ankunft in den
Backrooms. Option (Config, Standard aus): jede Entität, die in der Overworld unter `min_y − 32` fällt, landet in den Backrooms statt im
Leere-Tod. Option (Standard aus, Lore-Gag): sehr selten beim Ersticken in einer Wand.

**Ausgang:** seltener **Ausgangsraum** (Sonderraum mit grüner „EXIT“-Leuchttafel und Tür) → zurück in die Overworld an der Eintritts-x/z an die
Oberfläche (oder Weltspawn). Ein **Ausgangsschlüssel** aus einer Truhe öffnet zusätzlich eine „Hintertür“, die an einen anderen, weit entfernten Ort
der Overworld führt (Reise-Abkürzung als Belohnung).

**Einzigartige Blöcke (Vorschlag, alle Vanilla-nah nach A8):**

| Block | Eigenschaft |
|---|---|
| Feuchter Teppich (gelb-beige) | Boden; schluckt Schritte (Sculk hört dich nicht – Vanilla-Tag `dampens_vibrations`) |
| Gelbe Tapete (3 Muster) + Tapeten-Ecke | Wände, nicht abbaubar in den Backrooms (Abenteuer-Modus-artig per Dimensions-Regel), außerhalb normal (Deko) |
| Deckenplatte (Akustikplatte) | Decke, mit Wasserfleck-Variante |
| **Neonröhre** (Deckenleuchte, an/kaputt/flackernd) | Licht 15; **Flackern nur optisch** (Textur-Animation + Ton), Lichtwert bleibt fest → keine Licht-Updates (Performance) |
| Fehlerhaftes Grundgestein | Eingang, siehe oben |
| EXIT-Tafel | Licht 8, grün, markiert Ausgang |
| Mandelwasser (Item, Flasche) | heilt 2 Herzen + entfernt „Verwirrung“-Effekt; nur hier zu finden |

**Atmosphäre (alles Datapack-Umgebungs-Attribute, §0.1):** gelblicher Nebel (`fog_color`, Ende 48 Blöcke), kein Himmel, feste Zeit, Summ-Schleife
als `ambient_sounds` (ElevenLabs: Neon-Brummen 3 Takes, entferntes Bürogeräusch, Schritt auf Teppich; Herkunft in `CREDITS.md`), selten
`additions` (fernes Klopfen). Keine Musik. Staub-Partikel (`ambient_particles`).

**Gegner/„Entität“ (optional, Stufe 3):** **„Der Lauschende“** – groß, dünn, ohne Gesicht; wandert, sieht schlecht, **hört** (wie Wächter, Vibrationen).
Kündigt sich an (A14): Neonröhren in ~24 Blöcken flackern und gehen aus, Summen bricht ab. Greift nicht sofort an: erst Dunkelheit
(Vanilla-Effekt), dann 4 Herzen Schaden. Licht hilft nicht (überall 15); was hilft, ist **Stille**: wer schleicht, wird verloren.
Teppich dämpft Schritte → Schleichen auf Teppich = sicher. Kein Loot, kein Boss. Standard **aus** bis zur Abnahme (A12, Grusel).
Bestehender Grusel-Bezug: Hollow Howler (Simple Mobs) – nicht wiederverwenden, eigenes Wesen.

**Verirren als Mechanik:** Räume ändern sich nicht (deterministisch, keine Regeneration – sonst Datenverlust/Verwirrung bei Mehrspieler).
Kompass/Uhr drehen (`natural false`), Karten zeigen nur graues Raster (Kartenfarben der Blöcke absichtlich gleich). Markieren geht mit Vanilla-Mitteln
(Fackeln, Blöcke abstellen, Schilder) – Spieler lösen das selbst.

### 2.2 Varianten

#### Backroom A – Overworld nach unten erweitern, Räume unter dem Bedrock
- `dimension_type minecraft:overworld`: `min_y −320`, `height 640` (Oberkante bleibt 320). Rausch-Gitter bleibt −64/384 → Bedrock-Boden bleibt bei −64,
  darunter 256 Blöcke Luft, gefüllt mit Backroom-Struktur (Jigsaw wie Trial Chambers, Start-Höhe −200).

| Schwierigkeit | Aufwand | Risiko |
|---|---|---|
| **Bestandswelten:** alte Chunks haben unter −64 keine Sektionen → dort **Leere** (offene Löcher unter dem Bedrock, durch die man fällt). Vanilla löste das 1.18 nur mit eigener „below zero retrogen“ für genau diesen einen Sprung | L | **sehr hoch** |
| Kompatibilität: wie Wolke A, zusätzlich fest verdrahtete −64 (Erz-Mods, Höhlen-Mods, Leere-Tod-Logik, Simple Ores `y −64…−54`) | – | hoch |
| Endlos geht nicht: Jigsaw-Reichweite ≤ 128 Blöcke (`MAX_TOTAL_STRUCTURE_RANGE`), Tiefe ≤ 20 → einzelne Komplexe statt „riesig“ | M | mittel |
| Mobs/Höhlen-Spawns unter dem Bedrock, Licht (kein Himmelslicht nötig, gut) | S | niedrig |

Aufwand **L**, Risiko **sehr hoch**.

#### Backroom B – eigene Dimension „Zwischenräume“ mit Zellen-Generator (**Empfehlung**)
- Dimension: `min_y 0`, `height 64`, kein Himmelslicht, Decke, `natural false`, feste Zeit, Umgebungs-Licht leicht > 0, Biom `zwischenraum`.
- **Zellen-Generator** (Code, eigener Chunk-Generator oder Feature auf leerem Generator): jeder Chunk = 2×2 Raumzellen à 8×8, Raumhöhe 5.
  Wände liegen immer auf Zellgrenzen; ob eine Grenze eine Tür hat, entscheidet ein **Hash der gemeinsamen Kante** (beide Nachbarn rechnen dasselbe)
  → nahtlos, **unendlich**, deterministisch, kein Speicher nötig. Raumtypen je Hash: leer (häufig), Säulenhalle (große offene Fläche über 4×4 Zellen),
  Flur, Sackgasse, Schräg-Raum, „falscher Raum“ (eine Wand fehlt ins Dunkle).
- **Sonderräume wie Trial Chambers:** Jigsaw-Struktur (NBT-Vorlagen, Datapack) für Ausgangsraum, Lager mit Truhen, Wasserraum, „Ebenen-Schacht“;
  selten (Struktur-Set Spacing ~40 Chunks → Ausgang ≈ 300–700 Blöcke entfernt – bewusst weit). Spawns leer wie Trial Chambers.
- Spätere Stufe: **Ebenen** (Level 0 Büro → Level 1 Parkhaus/Beton → Level 2 Rohrgänge) über Löcher im feuchten Teppich; jede Ebene eine weitere
  y-Schicht in derselben Dimension (`height` bis 256).

| Schwierigkeit | Aufwand | Risiko |
|---|---|---|
| Overworld bleibt unverändert, nur ein seltener Eingangsblock im Bedrock (neue Chunks; alte Gebiete: Eingänge nur über Option „Leere → Backrooms“) | S | niedrig |
| Zellen-Generator in Code (Fabric zuerst, NeoForge/Forge später – Generator-Codec registrieren je Loader) | M | mittel |
| Leere Räume langweilen schnell → Rhythmus aus Sonderräumen, Klang-Orientierung, Funde; braucht Spieltest | M | mittel |
| Unzerstörbarkeit der Wände (sonst gräbt man geradeaus): Dimensions-Regel „Abbau nur von Spieler-Blöcken“ (Abenteuer-Prinzip) → Eingriff, aber nur in dieser Dimension | S | mittel |
| Performance: 4 Sektionen je Chunk, sehr viele Lichtquellen (Neon) – Licht 15 überall heißt: Licht-Engine einmal bei der Generierung, danach fest (kein Flackern im Lichtwert) | S | niedrig |
| ElevenLabs-Sounds: Credits verfügbar (50k), Key-Rotation beim Besitzer offen | S | niedrig |

Aufwand **L** (Generator + Blöcke + Sounds + Struktur), Risiko **mittel**. Abschaltbar (Eingangsblock wird zu normalem Grundgestein).

#### Backroom C – Backroom-Struktur in der Overworld-Tiefe (Kleinlösung, Trial-Chambers-Klon)
- Jigsaw-Struktur „Verlassene Büros“ bei y −60…−30 (direkt über dem Bedrock, `ENCAPSULATE` wie Trial Chambers), selten; einzigartige Blöcke wie oben.
- Kein Unter-dem-Bedrock, keine Dimension; überwiegend Datapack (Pools/Vorlagen) + Blöcke.

| Schwierigkeit | Aufwand | Risiko |
|---|---|---|
| begrenzt auf ~250×250 Blöcke (Jigsaw), wirkt nicht endlos; Höhlen schneiden hinein (Atmosphäre leidet) | M | niedrig |
| Nebel/Klang nur über Biom-Attribute **in der Struktur** → eigenes Biom per Mixin nur im Strukturbereich nötig, sonst Vanilla-Höhlenklang | M | mittel |
| „Unter dem Bedrock“ nur als Gefühl (Bedrock-Wände/-Decke um die Struktur) | – | – |

Aufwand **M**, Risiko **niedrig**.

### 2.3 A12 Backrooms
1. **Vanilla-Nutzen:** neues Endgame-Abenteuer für Netherit-Spieler; Ausgangsschlüssel als Reise-Abkürzung; Deko-Blöcke (Tapete, Teppich, Neon) für Innenräume.
2. **Änderung:** ein neuer, abgeschlossener Ort; Overworld-Bedrock bekommt einen seltenen Block (B) bzw. eine Struktur in der Tiefe (C).
3. **Eingriff:** A = **sehr stark** (Weltgrenze unten, Bestandswelten); B = **gering** (eigene Dimension, Eingang selten und abschaltbar; Leere-Option Standard aus); C = **gering**. Gegner „Der Lauschende“ = mittel, Standard aus.

### 2.4 Empfehlung Backrooms
**B** (Dimension + Zellen-Generator + Jigsaw-Sonderräume), Stufen: (1) Blöcke + Sounds + Vorschau, (2) Dimension + Generator + Eingang/Ausgang,
(3) Sonderräume, Mandelwasser, Klang-Orientierung, (4) optional „Der Lauschende“, (5) Ebenen.

---

## 3. Mod-Einordnung

| Option | Bewertung |
|---|---|
| **Neuer Mod „Simple Layers“ (`simplelayers`)** mit beiden Bereichen (Name nach dem Wunsch „Layered Biomes“), Sub-Mod von SimpleBuilding, `requires simplelib` | **Empfehlung.** Ein Ort für „Welt-Schichten“ (oben Wolken, unten Backrooms), eigener Kreativtab „Simple Layers“, je Bereich Config-Schalter (`clouds.enabled`, `backrooms.enabled`). Allein spielbar mit Vanilla. |
| Zwei Mods (`simpleclouds`, `simplebackrooms`) | sauberer trennbar, aber doppelte Infrastruktur (Übergang, Dimension, Tests); erst sinnvoll, wenn einer allein veröffentlicht werden soll. |
| In Simple Dimensions | passt technisch (Datapack-Erzeugung, Portale, Grenzwerte vorhanden), aber Simple Dimensions ist „konfigurierbare Dimensionen + Portale“ ohne eigene Inhalte; Inhalte dort machen es schwer. **Wiederverwendung**: Höhen-/Teleport-Bausteine (Cooldown, Zielplattform, Rückweg) nach SimpleLib heben statt Modul-Import. |
| Bezug Simple Earth | Wolke B braucht eine kleine Übergabe „über dem Wolkenmeer → Orbit“ (SimpleLib-Vertrag, beide optional). |
| Bezug Simple End/Ores | Biom-Mixin-Muster aus Simple End übernehmen; Simple Ores bleibt bei −64…−54 (B/C ändern dort nichts). |

Nach Wahl: `tools/newmod.py`, `modules/modules.json`, `tools/modtree/planned.json`/`modtree.py` (A13), PRINZIPIEN Teil 2 Abschnitt, Handbuch.

## 4. Phase und Reihenfolge
- **Nach Release** (Phase 4). Vor Release höchstens: Texturen der drei Wolken-Blöcke und der Backroom-Blöcke als Vorschau (A8, Ebenen, ~7 % hart), Sound-Takes als Hörprobe.
- Danach Plan je gewählter Variante (`PLAN-A-BIOMES-<datum>.md`) mit Stufen wie oben; nur 26.3 Fabric zuerst (Memory „NUR 26.3 Fabric“).

## 5. Offene Fragen an den Besitzer (über KONZ09-Anmerkung)
1. Nebel-Gruben: dürfen sie (Option „Schwierig“) ins Bodenlose führen, oder immer eine tiefere Wolkenlage darunter?
2. Backrooms: Gegner „Der Lauschende“ gewünscht oder rein leer/atmosphärisch?
3. Ein Mod „Simple Layers“ für beides oder zwei Mods?

## 6. Was NICHT geprüft wurde
- Kein Code, kein Gradle, kein Spieltest. Performance-Zahlen für Wolke A sind Abschätzungen (Sektionszahl), nicht gemessen.
- Verhalten der Höhenkarten-Neuberechnung bei Höhenänderung bestehender Welten nur aus Kenntnis von Vanilla/Tall-World-Packs, nicht im 26.3-Code verfolgt.
- Context7 nicht genutzt: Quelle ist das dekompilierte 26.3 unter `/root/mc263src` (genauer als Fremddoku).
