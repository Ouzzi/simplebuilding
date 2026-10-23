# Konzept Simple Earth (Arbeitsname) – 23.10.2026

Status: **Konzept + Vorschau, kein Code.** Warteliste WL-1, Umsetzung erst auf Besitzer-Anforderung.
Wunsch: `docs/ai/WUNSCH-SIMPLE-EARTH-2026-10-23.md`. Vorschau (interaktiv): `/root/sb-tools-a/previews/earth-konzept.html`
(Pfeiltasten/Ziehen = fliegen, Mausrad = Höhe, Knöpfe für die drei Tesserakt-Varianten).
Regeln: `docs/ai/PRINZIPIEN.md` (A1, A2, A7, A8, A9, A11), `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`.

---

## 0. Recherche (Kurzfassung)

| Quelle | Was sie macht | Was wir übernehmen / anders machen |
|---|---|---|
| **Cosmic Horizons** (Forge 1.20.1, Autor dea___th, Alpha 0.0.7.x, „nur Kreativ spielbar“) | Weltraum-Mod als „Galacticraft-Nachfolger“: eigene **Weltraum-Dimension** („cosmos“), Planeten/Monde als **Kugeln aus Datapacks** (Sonnensystem-Datei mit `object_name`, `atmospheric_data`), Planeten sind **eigene Dimensionen**, eigenes Render-System, das Planeten **unabhängig von der Sichtweite** zeichnet, eigene Schwerkraft je Planet, Reise per „Space Nodule“/Schiff, läuft mit Oculus/Shadern. Addons: *Cosmos Uncharted* (Weltraum durch Hochfliegen ab einer Übergangshöhe, serverseitig, jede Flugart), *Dynamic Horizons* (Umlaufbahnen, `anchor_name`, `orbit_pitch/yaw/roll`). | Übernehmen: Zeichnen unabhängig von der Sichtweite, Übergang durch reines Hochfliegen, Shader-Verträglichkeit als Pflicht. **Anders:** keine neue Dimension und keine Planeten-Sammlung – wir zeigen **die eigene, echte Overworld** als Würfel, deren Terrain aus dem echten Seed stammt. Das gibt es dort nicht. |
| **Distant Horizons** | LOD-Rendering: ferne Chunks als vereinfachte Modelle; Standard „Quick Generation“ schätzt Terrain aus Seed/Einstellungen ohne echte Chunk-Generierung. | Prinzip „aus dem Seed schätzen statt generieren“ und **Detail-Stufen (LOD)**. DH rendert im Orbit sonst weiter → optional abschalten (§6.6). |
| **Chunkbase Biome Finder** | Biom-Karte aus Seed + Version, beliebig weit zoombar, ohne Welt; nutzt das Multi-Noise-System (seit 1.18: Temperatur, Feuchte, Kontinentalität, Erosion, Tiefe, Verwitterung → Biom). | Genau unser Bildverfahren, nur **serverseitig im Spiel** mit dem echten `BiomeSource` (auch Mod-Biome). |
| **Vanilla-Karten** | Pixel = Blockspalte(n), Farbe aus `MapColor` mit 3–4 Helligkeitsstufen (Höhenschattierung). | Stil der Würfelflächen: Kartenpalette + Stufen-Schattierung, keine Fotorealistik. |
| **Vanilla-Himmel** | Sonne und Mond sind flache Quadrate mit 8 Mondphasen; Sterne als Punkt-Quads. | Sonne bleibt flach (Wunsch), Mond wird abgerundeter Würfel, Sterne bleiben (im Orbit auch tagsüber). |
| **Celestial / Planetarium** (Fabric) | Himmel per Ressourcenpaket, Planeten am Nachthimmel. | Bestätigt: Himmel-Eingriff per eigenem Renderer ist üblich; Terrain-als-Körper macht keiner. |
| **Fabric API 26.x** (geprüft: `fabric-rendering-v1` 25.3.2 und 27.0.14 im Gradle-Cache sb-test) | Öffentlich: `LevelRenderEvents` (START_MAIN, AFTER_OPAQUE_TERRAIN, BEFORE/AFTER_TRANSLUCENT_TERRAIN, AFTER_SOLID/TRANSLUCENT_FEATURES, COLLECT_SUBMITS, BEFORE_GIZMOS, END_MAIN), `LevelExtractionEvents`. **Kein öffentlicher Sky-Hook mehr** (`DimensionRenderingRegistry` entfallen; nur internes `SkyRenderStateMixin`). | Würfel über `COLLECT_SUBMITS`/`END_MAIN`, Himmel per eigenem Mixin in `SkyRenderer` (wie SB es bei `LevelRenderer` schon tut). |

Quellen: modpackindex.com/mod/62013/cosmic-horizons, 9minecraft.net (?p=833745), discord.com/servers/cosmic-horizons-1278760883992137798,
modrinth.com/mod/dynamic-horizons, curseforge.com/minecraft/mc-mods/cosmos-uncharted, blog.curseforge.com/distant-horizons-frequently-asked-questions,
curseforge.com/minecraft/mc-mods/celestial, modrinth.com/mod/planetarium. Technik von Cosmic Horizons ist nicht offen dokumentiert (keine Quellen öffentlich gefunden) –
Angaben oben stammen aus den Mod-Beschreibungen und Addon-Seiten.

---

## 1. Der Wunsch in eigenen Worten

Wer hoch genug fliegt, verlässt die Welt „nach oben“: Ab einer **Aktivierungshöhe** merkt sich das Spiel den Punkt, an dem man war – er wird die Mitte
der Erde. Noch etwas höher (**Render-Umschalthöhe**) verschwindet die normale Blockwelt und unter einem schwebt die Minecraft-Erde als **leicht
abgerundeter Würfel**, dessen Oberseite das echte Terrain als pixelige Biom-Karte zeigt. Fliegt man weiter, dreht sich der Würfel unter einem:
geradeaus kommt eine **Kante**, schräg eine **Ecke** unter den Spieler. Hinter der Kante wartet die nächste Fläche – und weil die Welt unendlich ist,
ist jede Fläche **neues** Terrain: einmal „rundherum“ landet man nicht am Start, sondern hunderttausende Blöcke weiter („Tesserakt“). Diagonal treffen
sich in der echten Welt **vier** Gebiete an einem Punkt, am Würfel aber nur **drei** Flächen – dafür braucht es eine eigene, unverwechselbare Darstellung.
Darüber: ein **näherer, leicht würfeliger Mond** und eine **ferne, flache Sonne**. Einfacher Stil, großer Effekt, poliert.

---

## 2. Erlebnis und Ablauf

### 2.1 Phasen (Overworld)

| Phase | Höhe (Vorschlag) | Was passiert |
|---|---|---|
| Normal | < 1 024 | Vanilla. |
| **Aktivierung** | **y = 1 024** | Orbit-Status an: Punkt A = (x, z) wird Würfelmitte. Himmel beginnt nachzudunkeln, Sterne werden sichtbar, leiser Wind wird zu Stille. Keine Chunk-Änderung. |
| Aufstieg | 1 024 – 1 536 | Echte Welt noch sichtbar (Sichtweite), Nebel steigt, Horizont krümmt sich leicht (Shader-frei: Nebelfarbe + abfallender Horizont-Ring). |
| **Render-Umschaltung** | **y = 1 536** | Würfel blendet ein; echte Chunks blenden aus (Übergangsband bis 1 792, siehe 2.3). Server parkt die Spielfigur (§6.2). |
| Orbit | 1 792 – 6 144 | Nur Würfel, Mond, Sonne, Sterne. Höhe = Zoom. Über 6 144 weiche Decke (kein weiterer Zoom, sanftes Zurückdrücken). |
| Wiedereintritt | Abstieg unter 1 024 | Teleport an die **echte** Orbit-Position, Nebel/Glühen, Langsamfall bis Chunks da sind, dann normal. |

Alle Höhen sind serverseitige Config mit Grenzen (A9): Aktivierung ≥ Bauhöhe + 64, Umschaltung ≥ Aktivierung + 128, Decke ≤ 30 000.
Fliegt man senkrecht hoch, ändert sich nur y – der Würfel steht still, wie gewünscht.

### 2.2 Bewegung über dem Würfel

- Kachel-Mathematik (alle Varianten): Kantenlänge **L**, halbe Kante **H = L/2**, Aktivierungspunkt **A**.
  Kachel-Index `tx = floor((x − Ax + H) / L)`, Kachelmitte `Cx = Ax + tx·L`, Flächenkoordinate `u = (x − Cx) / H ∈ [−1, 1)`; dasselbe für z → `v`.
- Rotation: Punkt unter dem Spieler auf der Würfeloberseite ist `p = (u, 1, v)` (Würfel mit Halbkante 1). Der Würfel wird mit der kürzesten Drehung
  so gedreht, dass `p̂` nach oben zeigt. Ergebnis: Mitte → 0°, Kante (u = 1) → **45°** (Kante zeigt genau zum Spieler), Ecke (u = v = 1) → **54,7°**
  (Ecke zeigt genau zum Spieler). Die Drehung ist stetig und „leicht“ am Anfang (atan-Verlauf).
- Kantenwechsel: bei `u ≥ 1` wird die Nachbarkachel die Unterseite; die Darstellung ist an der Kante vorher und nachher identisch (kein Sprung).
  Hysterese 1 % (u muss 1,01 bzw. −1,01 erreichen), damit es an der Kante nicht flackert.
- Die vier Seitenflächen zeigen immer die vier Nachbarkacheln (N/O/S/W); die abgewandte Oberseite ist nie sichtbar und muss nicht geladen werden.

### 2.3 Übergang weich (Render-Umschaltung)

Problem: Von y = 1 536 sieht man real ~1 000 Blöcke weit, eine Fläche ist aber 65 536 Blöcke breit. Lösung **logarithmischer Zoom** im Band 1 536 → 2 560:
Die virtuelle Kamera-Entfernung wächst exponentiell, sodass die sichtbare Breite von ~1 024 Blöcken (= echte Sicht) auf den ganzen Würfel steigt.
Am unteren Bandende liegt eine **Detail-Insel** (Clipmap: 4 096 × 4 096 Blöcke, 512 px = 8 Blöcke/px) passgenau über dem echten Terrain; die echten
Chunks blenden über 0,5 s aus, die Insel blendet in die Fläche über. Danach reicht die normale Flächen-LOD.

### 2.4 Werte-Vorschläge

| Größe | Vorschlag | Bemerkung |
|---|---|---|
| Kantenlänge L | **65 536 Blöcke** (H = 32 768) | = 4 096 Chunks je Kante. Besitzer-Beispiel L = 200 000 als Config möglich (E1). |
| Grenzen L | 8 192 … 262 144 | darunter wirkt es klein, darüber reicht die Weltgrenze (±29 999 984) nur für ~114 Flächen je Richtung. |
| Flugtempo im Orbit | bis 256 Blöcke/s (Orbit-Gleiten) | bei L = 65 536: Mitte → Kante in ~2 min 8 s; mit Elytra-Tempo (~33 b/s) wären es ~16,5 min (E2). |
| Pixel je Fläche nach Höhe | 512 / 256 / 128 / 64 px | bei L = 65 536 → **8 / 16 / 32 / 64 Chunks je Pixel** (deckt die gewünschten 10/20/30/64). |
| Höhenstufen LOD | 1 792–2 560: 512 px · 2 560–3 584: 256 · 3 584–4 864: 128 · darüber: 64 | Übergang per Überblendung, nie Aufploppen. |
| Rundung Kanten/Ecken | 3 Stufen, Treppe 3-2-1 Pixel bei 64 px Flächenhöhe (≈ 4,7 %) | exakt die Eckstufen der UI-Rahmen (`UiBoxes.CUT_OUTER = {3,2,1}`), skaliert mit der Flächenauflösung. |
| Bildschirmgröße Würfel | 140 % (Umschaltung) → 35 % (Decke) der Bildbreite | „Erde füllt den Blick“ → „Erde als Körper im All“. |

---

## 3. Kernfrage „Tesserakt“

### 3.1 Das Problem

Die echte Welt ist eine Ebene, gekachelt in Quadrate der Größe L. An jedem Gitterpunkt treffen sich **vier** Kacheln (Mitte, Nord, Ost, Nordost).
An einer Würfelecke treffen sich nur **drei** Flächen (Unterseite, Nord-Seite, Ost-Seite) – es fehlen 90° Winkel (Winkeldefizit des Würfels).
Fliegt man exakt diagonal über den Gitterpunkt, kommt man in der echten Welt direkt in die **Nordost-Kachel** – die am Würfel keinen Platz hat.
Das ist die „übersprungene Fläche“ des Besitzers.

### 3.2 Rechenbeispiele

**Geradeaus (Besitzer-Werte H = 100 000, L = 200 000), Start z = 0, Flug nach Norden (−z):**

| Kantenwechsel | Kante bei | neue Flächenmitte | Würfel hat sich gedreht um |
|---|---|---|---|
| 1 | 100 000 | 200 000 | 90° |
| 2 | 300 000 | 400 000 | 180° |
| 3 | 500 000 | 600 000 | 270° |
| 4 | 700 000 | 800 000 | **360° – gleiche Würfellage, aber 800 000 Blöcke weiter** |
| 5 | 900 000 | **1 000 000** | 450° (die „~1 000 000“ des Besitzers) |

Zurück kommt man nur denselben Weg (800 000 Blöcke zurück). Mit L = 65 536: eine volle Umdrehung = 262 144 Blöcke.

**Diagonal (45°, H = 100 000):** Ecke liegt bei (100 000, 100 000), Flugstrecke √2 · 100 000 ≈ **141 421 Blöcke**. Unter dem Spieler zeigt die Ecke
(Drehung 54,7°), sichtbar sind Mitte + Nord + Ost. Nächstes Ziel in Wirklichkeit: Nordost-Kachel mit Mitte (200 000, 200 000) – noch einmal 141 421 Blöcke.

### 3.3 Variante V1 – „Kachel-Würfel“ (feste Kacheln, einfacher Sprung)

Die Ecke zeigt drei Flächen. Wer über den Gitterpunkt fliegt, bekommt die Nordost-Kachel als neue Unterseite; der Würfel springt um die Ecke
(kurze 0,3-s-Drehung um die Raumdiagonale, 120°).
- + einfachste Mathematik, kein Verzerren, genau die echten Kacheln.
- − die Nordost-Kachel ist vor dem Überflug unsichtbar und erscheint schlagartig; der 120°-Sprung verwirrt, „Ecke der Welt“ hat keinen eigenen Moment.

### 3.4 Variante V2 – „Kegel-Faltung“ (stetig, verzerrt)

Um jeden Eckpunkt werden die 360° der Ebene auf die 270° der Würfelecke abgebildet (Winkel × ¾ in einem Radius von 0,35 H um die Ecke,
außerhalb unverzerrt). Die Nordost-Kachel erscheint **zusammengestaucht als Keil** in den Ecken der Nord- und Ost-Fläche.
- + alle vier Kacheln sind jederzeit sichtbar, mathematisch sauber (wie eine Würfel-Karte der Erde); nur genau am Eckpunkt bleibt ein kurzes Umklappen (siehe 3.6).
- − Terrain nahe den Ecken ist verzerrt (Flüsse knicken), Pixel nicht mehr quadratisch → bricht den Karten-Pixelstil; aufwendiger zu rendern (eigenes Mesh je Ecke).

### 3.5 Variante V3 – „Eckfenster + Umstülpen“ (Empfehlung)

Die ohnehin gewünschte Rundung der Ecken wird zum Fenster in die vierte Dimension:
1. **Eckfenster:** Nähert man sich einer Ecke (|u| > 0,5 und |v| > 0,5 in derselben Richtung), wächst aus der Rundung eine kleine **vierte, dreieckige
   Fläche** (abgeschnittene Ecke) mit dünnem violettem Pixelrahmen. Darin liegt die Nordost-Kachel, verkleinert, unverzerrt in Pixeln.
   Größe = 0 bei 0,5, bis ⅓ der Kante direkt über der Ecke.
2. **Umstülpen:** Überquert man den Gitterpunkt (beide Kanten innerhalb von 2 % der Strecke), läuft eine 0,8-s-Animation wie die bekannte
   Tesserakt-Projektion: aus dem Fenster wächst ein innerer Drahtwürfel, wird zum neuen Würfel, der alte schrumpft in die gegenüberliegende Ecke.
   Danach ist die Nordost-Kachel die Unterseite, die Ecke zeigt nach Südwest (zurück).
3. Wer **knapp neben** dem Gitterpunkt fliegt, wechselt normal über eine Kante (N oder O), das Fenster schließt sich wieder.
- + alle vier echten Kacheln sind sichtbar, keine Pixelverzerrung, die „Ecke der Welt“ wird zum Ereignis – einzigartig, passt zu „abgerundet“.
- − mehr Arbeit (Eckfenster-Mesh, Animation, Hysterese an der Ecke), Eckfenster bei 64-px-Stufe sehr klein (dann nur als Leuchten).

### 3.6 Gemeinsame Mechanik: Umklappen um die Diagonale

Beim Überflug des Eckpunkts wechseln die Rollen so: alte Nord-Kachel liegt danach auf der West-Seite, alte Ost-Kachel auf der Süd-Seite, die alte Mitte verschwindet
nach unten, die Nordost-Kachel wird Oberseite. Das ist genau eine **180°-Drehung um die waagrechte Diagonale** (Achse (1, 0, 1) im Würfel bei Nordost):
`-X → -Z`, `+Z → +X`, `+Y → -Y`. Die Anzeige startet daher bei `q_alt · R180` und dreht in 0,35 s (V1/V2) bzw. 0,9 s (V3, mit Tesserakt-Linien) auf `q_neu` –
Nord- und Ost-Kachel bleiben dabei an ihrem Bildschirmplatz, die Nordost-Kachel klappt von der verdeckten Unterseite hoch. Die Vorschau zeigt das live.

**Empfehlung:** Datenmodell immer V1 (feste Kacheln ab A, Kachel = Fläche); Darstellung **V3**. S1 baut V1 (reicht zum Prüfen von Gefühl und Werten),
V3 kommt in S3. V2 bleibt verworfen wegen Pixelverzerrung (Stil-Bruch, A8 „nah an Vanilla“).

---

## 4. Terrain-Bild

### 4.1 Daten
- **Biom** je Pixel über den echten `BiomeSource` der Dimension: `getNoiseBiome(qx, qy, qz, randomState.sampler())` (Quart-Auflösung, 4 Blöcke) – keine Chunk-Generierung,
  funktioniert auch mit Mod-Weltgen (Terralith, Tectonic …). qy = geschätzte Oberfläche (sonst landen Höhlen-Biome im Bild).
- **Höhe** grob: Kontinentalität/Erosion/Peaks-and-Valleys aus dem Klima-Sampler bzw. der Router-Wert für die vorläufige Oberflächenhöhe (in 26.3 nachprüfen) →
  nur für Kartenschattierung (3 Stufen) und Wasser-Tiefe (Ozean dunkler).
- Der Seed bleibt auf dem Server; der Client bekommt nur fertige Biom-Indizes.

### 4.2 Raster und Kosten
- Pixel = 1 Punktabfrage in der Pixelmitte; ab 32 Chunks/px zusätzlich 2 × 2 Abfragen mit Mehrheitsentscheid (sonst flimmern schmale Flüsse).
- **Fortschreitend wie Chunkbase:** zuerst 64 px (4 096 Abfragen, sofort), dann 128, 256, 512; jede Stufe nutzt die gröbere als Vorschau.
- Kostenschätzung (in S1 messen): eine Multi-Noise-Abfrage ≈ 2–10 µs → 512 × 512 = 262 144 Abfragen ≈ 0,5–2,6 s je Fläche und Thread; fünf sichtbare Flächen
  mit 2 Hintergrund-Threads ≈ 1,5–6,5 s für volle Schärfe, 64-px-Bild aller Flächen < 0,2 s.
- **Threads:** eigener Worker-Pool (2 Threads, Config, max. Kerne − 1), niedrige Priorität, nie auf dem Server-Haupt-Thread. Budget je Spieler, Warteschlange nach Nähe.

### 4.3 Cache und Speicher
- Abfragen auf einem **globalen Raster** (Schrittweite = 2^n Blöcke, unabhängig von A) speichern – so teilen sich Spieler mit verschiedenen Würfelmitten den Cache.
- Server: Regionsdateien `data/simpleearth/<dim>/<schritt>/r.<x>.<z>.bin` (64 × 64 Abfragen, 1 Byte Biom-Palette + 1 Byte Höhe, komprimiert ~2–4 KB je Region);
  eine 512-px-Fläche ≈ 512 KB roh, ≈ 60–120 KB gepackt. Grenze Gesamtgröße (Config, Standard 256 MB, älteste zuerst löschen).
- Client: nur Texturen im Speicher, LRU mit max. 16 Flächen à 512² RGBA = 16 MB.
- Netz: Kachel als Palette + RLE ≈ 5–15 KB je 256-px-Fläche, max. 64 KB/s je Spieler.

### 4.4 Stil
- Farben aus der **Vanilla-Kartenpalette**: Biom → typischer Oberflächenblock → `MapColor` (Ebene → GRASS, Wüste → SAND, Ozean → WATER mit Tiefenstufen, Schnee → SNOW, Badlands → TERRACOTTA …),
  3 Helligkeitsstufen nach Höhe wie Karten. Unbekannte Mod-Biome: Gras-/Laub-/Wasserfarbe des Bioms, sonst Hash-Farbe; Override per Datapack `simpleearth:biome_colors`.
- Pixel bleiben **scharf** (Nearest), keine Glättung; Wolken optional als dünne, langsam ziehende Pixelschicht über der Fläche.
- Kanten/Ecken: Treppenrundung 3-2-1 wie die UI-Rahmen; an der Kante ein 1-px-Rand in Kartenschatten-Farbe (wie Inventarrahmen, „Mehrfach-Rand“).
- Nachtseite: Fläche abgedunkelt (Lichtrichtung = Sonnenstand), optional später Dorf-Lichter (S4, aus Strukturdaten).

---

## 5. Himmel

- **Sonne:** bleibt flaches, quadratisches Pixel-Quad wie Vanilla, aber schärfer und mit weichem Pixel-Hof; sehr weit weg = keine Parallaxe beim Fliegen. Leuchtet Würfel und Mond an.
- **Mond:** abgerundeter Würfel (Rundung ≈ 15 %, deutlich würfelig, aber weicher als die Erde), eigene Pixeltextur je Fläche (A8: eigene Pixel, keine Vanilla-Kopie),
  ~3× Vanilla-Größe, zeigt der Erde immer dieselbe Seite. **Phasen echt durch Beleuchtung:** Bahnwinkel so versetzt, dass die Lichtgrenze zur Vanilla-Mondphase des Tages passt (Phase · 45°).
- **Atmosphärenrand:** abgerundete Hülle 3 % größer als der Würfel, additiv, Fresnel-Verlauf (Rand hell, Mitte klar); Tagseite Himmelsblau, Nachtseite dunkles Indigo,
  an der Licht-Grenze ein schmales Dämmerungsband (Orange). Pixeliger Verlauf (4–6 Stufen statt glatt) passend zum Stil.
- **Sterne:** Vanilla-Sterne, im Orbit auch tagsüber voll sichtbar, Himmel schwarz; leichtes Funkeln nur unterhalb der Aktivierungshöhe.

---

## 6. Technik-Skizze

### 6.1 Client-Renderer (Fabric 26.3)
- Würfel-Mesh (6 Flächen mit Treppenrundung, ≈ 2 000 Dreiecke) über `LevelRenderEvents.COLLECT_SUBMITS` bzw. `END_MAIN` mit eigener `RenderPipeline` (Texturen nearest, eigene Tiefe).
- Himmel: Mixin in `net.minecraft.client.renderer.SkyRenderer` (Sonne/Mond/Sternenhelligkeit im Orbit ersetzen); Fabric API 26.x hat keinen öffentlichen Sky-Hook.
- Im Orbit sind keine Chunks geladen (6.2) → Vanilla-/Sodium-Terrain zeichnet nichts; kein Eingriff in den Chunk-Renderer nötig.
- Kamera: echte Kamera bleibt am Parkpunkt; der Würfel wird relativ zur Kamera mit Zoom-Faktor aus der Höhe gezeichnet.

### 6.2 Server: Positions-Tracking („Orbit-Status“)
- Datenanhang am Spieler: `{aktiv, Ax, Az, orbitX, orbitZ, L}`. Ab Render-Umschaltung wird die Spielfigur am Aktivierungspunkt in der Höhe **geparkt**
  (dort sind Chunks bereits geladen); Bewegungseingaben verschieben nur `orbitX/orbitZ` serverautoritativ (Tempo-Deckel aus Config).
  Grund: würde der Spieler echt mit 256 b/s fliegen, müsste der Server laufend Chunks laden/generieren – und der Client friert in ungeladenen Chunks ein.
- Abstieg unter Aktivierungshöhe → Teleport an `(orbitX, y, orbitZ)`, Wiedereintritt mit Langsamfall bis die Chunks da sind (Zeitlimit, sonst Schweben).
- Weltgrenze: Orbit-Position wird an der Grenze gestoppt; Flächen jenseits zeigen das Barrieren-Muster.
- Bewegungsprüfung („moved too quickly“) bleibt unberührt, weil sich die echte Figur nicht schnell bewegt.

### 6.3 Netzwerk
- S2C `OrbitState` (an/aus, A, Orbit-Position, L, Höhen), S2C `EarthTile` (Dimension, Raster, Kachel, LOD, Palette + RLE), C2S `TileRequest` (vom Server begrenzt, Nähe-Priorität).
- Muster wie Simple Maps (`TileCache`-Anfrage/Antwort) – gemeinsamer Teil wandert nur nach simplelib, wenn beide Module ihn wirklich teilen (A2), keine Klassenimporte.

### 6.4 Multiplayer
- Jeder Spieler hat seinen eigenen Würfel (eigene Mitte A, Wunsch), der Cache ist global (4.3).
- Andere Spieler im Orbit erscheinen als kleine Pixel-Köpfe über dem Würfel, wenn ihre Orbit-Position auf einer sichtbaren Fläche liegt; Spieler am Boden nie (Privatsphäre wie Karten).
- Server-Option „nur erkundete Gebiete“ (Bild nur dort, wo Chunks existieren; sonst Nebel) für Server, die keine Seed-Karte verschenken wollen (E8).

### 6.5 Leistungsbudget
- Client ≤ 0,5 ms je Bild (Mesh fest, Texturen inkrementell hochgeladen), ≤ 16 MB Texturspeicher.
- Server: 0 ms auf dem Haupt-Thread je Tick außer Paketen; Worker ≤ 2 Threads; im Orbit **billiger** als Elytra-Flug (keine Chunk-Tickets).

### 6.6 Kompatibilität
- **Sodium:** unkritisch (eigener Renderer, keine Chunks im Orbit). Prüfen in S4.
- **Iris/Shader:** größtes Risiko – Shader überschreiben den Himmel und behandeln eigene Pipelines unterschiedlich. Lösung: Würfel nach der Shader-Komposition zeichnen
  („Shader-Modus“), Himmel im Orbit erzwingen; Test mit 2 gängigen Shaderpaketen.
- **Distant Horizons:** würde im Orbit LODs unter dem Parkpunkt zeigen → optionale Integration (nur wenn geladen, Adapter-Klasse) schaltet DH-Rendering ab Umschalthöhe aus.
- **Mod-Weltgen:** funktioniert über den generischen `BiomeSource`; Farben über Fallback/Datapack.

### 6.7 Modulgrenzen
- **Neuer Sub-Mod `simpleearth`** (Vorlage `tools/newmod.py`, Paket `com.simplebuilding.modules.simpleearth`, Version 0.1.0, A11), allein spielbar, nur Fabric API (+ simplelib, falls genutzt).
  In SimpleBuilding nur als `optional` mit Schalter „Enable Simple Earth“ (Super-/Sub-Mod-Konzept).
- Gehört **nicht** in Simple Maps oder Simple Dimensions (eigenes Erlebnis, Client-Renderer-lastig). Optionale spätere Brücke: Wegpunkte von Simple Maps auf dem Würfel – nur über öffentliche Daten/Registry, bedingt.
- Pflichtdateien (A11.5): Mod-Icon 16 × 16 (kleiner Würfel), `wiki/manual.json` EN/DE, Handbuch, Config-Texte EN+DE, Standalone-Target.
- Entdeckbarkeit (A6): Hinweis beim ersten Erreichen der Aktivierungshöhe (Ton + Sternenwechsel, kein Text), Handbuch-Seite, Fortschritt „Über den Wolken“.

---

## 7. Risiken, Aufwand, offene Fragen

### 7.1 Risiken
| # | Risiko | Gegenmittel |
|---|---|---|
| R1 | Client friert in ungeladenen Chunks ein / Server tickt Spieler ohne Chunks nicht | Parken am Aktivierungspunkt (6.2) |
| R2 | Abfragekosten bei Mod-Weltgen deutlich höher | in S1 messen, LOD-Fortschritt, Budget, Cache |
| R3 | Iris/Shader zerstören Himmel/Würfel | eigener Shader-Modus, früher Test (S3 statt erst S4) |
| R4 | Seed-Rückschluss aus der Karte | Server-Option „nur erkundet“ (E8) |
| R5 | Render-API ändert sich je Snapshot (26.x wechselt oft) | Rendern in einer kleinen Klasse bündeln, Ports erst auf Anforderung |
| R6 | Flackern an Kante/Ecke, Gimbal-Probleme | Quaternionen, Hysterese 1 % (Kante) / 2 % (Ecke), JUnit für Kachel-Mathematik |
| R7 | Spieler kommt nicht so hoch | E5 (jede Flugart; ggf. später Höhenrakete) |

### 7.2 Aufwand in Stufen
| Stufe | Inhalt | Aufwand (Agenten-Tage, grob) |
|---|---|---|
| **S1 Prototyp** | Einzelspieler; Orbit-Status, Parken, Würfel mit Unterseite + 4 Seiten, Rotation, Kantenwechsel (V1), 128-px-Biombild, schwarzer Himmel; Messung Abfragekosten; JUnit Kachel-Mathe | 3–4 |
| **S2 Orbit-Mechanik** | Server-Pakete, Cache (global, Disk), LOD 64–512, Wiedereintritt, Weltgrenze, Multiplayer (Köpfe), Config mit Grenzen | 4–5 |
| **S3 Tesserakt + Himmel** | Eckfenster + Umstülpen (V3), Zoom-Übergang mit Detail-Insel, Atmosphäre, Würfel-Mond mit Phasen, flache Sonne, Shader-Modus | 5–6 |
| **S4 Politur** | Klänge, Wolkenschicht, Nachtlichter, DH-/Iris-/Sodium-Prüfung, Wiki/Handbuch/Fortschritt, Standalone-Tests, Besitzer-Abnahme | 3–4 |

### 7.3 Offene Fragen an den Besitzer
- **E1 Kantenlänge:** 65 536 Blöcke (8–64 Chunks/px, Rundflug 262 144) oder dein Beispiel 200 000? → *Empfehlung 65 536, als Config bis 262 144.*
- **E2 Flugtempo im Orbit:** echtes Flugtempo (Elytra ~33 b/s, Kante nach ~16 min) oder eigenes Orbit-Gleiten bis 256 b/s? → *Empfehlung Orbit-Gleiten, Echt-Tempo als Config.*
- **E3 Diagonale/Tesserakt:** V1 Sprung, V2 Kegel-Faltung oder V3 Eckfenster + Umstülpen? → *Empfehlung V3 (siehe Vorschau).*
- **E4 Würfelmitte:** je Spieler am Aktivierungspunkt (dein Wunsch) oder ein globales Raster für alle? → *Empfehlung je Spieler, Cache global.*
- **E5 Wie kommt man hoch:** jede Flugart (Kreativ, Elytra+Raketen, Mods) oder zusätzlich ein eigenes Item? → *Empfehlung jede Flugart, kein eigenes Item in S1–S3.*
- **E6 Landung:** an der echten Orbit-Position mit Wiedereintritt (Langsamfall)? → *Empfehlung ja.*
- **E7 Andere Dimensionen:** nur Overworld; End später als schwebende Insel-Würfel? Nether nie (Decke). → *Empfehlung nur Overworld bis zur Abnahme.*
- **E8 Seed-Schutz:** alles zeigen (Wunsch) oder Server-Option „nur erkundet“? → *Empfehlung alles als Standard, Option für Server.*
- **E9 Andere Spieler im Orbit** als kleine Köpfe zeigen? → *Empfehlung ja, Spieler am Boden nie.*
- **E10 Mondphasen** echt durch Beleuchtung des Würfel-Monds, passend zur Vanilla-Phase? → *Empfehlung ja.*
- **E11 Höhen:** Aktivierung 1 024, Umschaltung 1 536, Decke 6 144? → *Empfehlung ja (Config mit Grenzen).*
- **E12 Name/ID:** „Simple Earth“, `simpleearth`? → *Empfehlung ja.*
