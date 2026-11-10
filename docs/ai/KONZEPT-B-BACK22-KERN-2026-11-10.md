# Konzept B-BACK22-KERN (10.11.2026) – gemeinsamer Kern: Stapel-Dimension, Ebenen-API, Tür-Portale (SimpleLib)

NUR KONZEPT – nichts davon ist gebaut. Branch `b-back22-kern-konzept`, Basis `hub/b-back22-konzept` (0aee0cf71). Auftrag N22 +
„N22-Nachtrag“ + „N22-Nachtrag 2“ (/root/b-auftrag-20261109.md), Besitzer-Wunsch und -Antworten /root/sb-tools-a/wunsch-back22.md,
Gesamtkonzept docs/ai/KONZEPT-B-BACK22-2026-11-10.md (R1–R9, Endstand §1). Die Ebenen-Konzepte (e1–e5, e6–e10) schreiben zwei andere
Agenten parallel; dieses Dokument legt den **gemeinsamen Kern** fest, auf dem alle 10 Ebenen **parallel und konfliktfrei** gebaut werden.
Nur MC 26.3 Fabric. Skizzen (gerechnet, keine Spielaufnahmen): /root/sb-tools-b/back22/kern/ (`kern_schnitt.png`, `kern_tuer.png`,
`kern_culling.png`, Skript `scr/kern_img.py`).

## 0. Ist-Stand und geprüfte Fakten (Retrieval-First)

**Code heute** (Production 171 = b-back18, Modul `modules/simplelayers`):
- `world/gen/BackroomsLevel` (Enum, 5 Ebenen = 5 Dimensionen), `Piece` (EIN Enum für alle Blöcke aller Ebenen), `LobbyGen`,
  `WarehouseGen`, `PipesGen`, `PoolsGen` (rein, MC-frei, `at(seed,x,y,z)` je Block), `GenHash`, `EscapeCheck` (BFS über Zellgraph).
- `world/BackroomsChunkGenerator` füllt jede Chunk-Säule Block für Block über `at()` (64 hoch), `BackroomsBlocks` bildet `Piece` →
  `BlockState` ab, `BackroomsTravel` (Risse, Wartungstüren, EXIT), `BackroomEffect` + Mixins (Abbau-/Kolbenschutz), `BackroomsAmbience`.
- Zentrale Einzeldateien: `LayersRegistry`, `LayersConfig`, `tools/gen_data.py` (Blockstates/Modelle/Loot/Tags/Biome),
  `lang/en_us.json`/`de_de.json`, `wiki/manual.json`, Fabric-Adapter `ModuleGameTest.java`, Katalog `LayersTests.ALL`.
- **b-back20s** (hub, nicht gebündelt, = b-back20 + STAIRS22): `Maze` (Region-Irrgarten, Spannbaum, `loops`), `HouseTemplate`
  (Haus mit Keller), Kreide (4 Farben) + `ChalkBox` + `ChalkMarkBlock`, `BackroomsLoot` (bedingte Beute), Loot-Tabellen.
- **b-stairs22** (in b-back20s): `LobbyGen.STAIR_STEPS`, `STAIR_HEADROOM = 3` (2 Kopf + 1 Diagonale, nachgerechnet), Sockel unter jeder
  Stufe, Gametest `stairwells_walkable` mit simuliertem Spieler (echte Kollisionsformen).
- SimpleLib hat bereits API-Muster für modulübergreifende Dienste ohne Import (`com.simplelib.api.HeightHandover`: Provider-Registry),
  eigene Payloads (`com.simplelib.net`), Simple Layers bindet SimpleLib auf Fabric ein (`include` + `implementation`).

**In den 26.3-Quellen nachgelesen** (minecraft-patched-26.3.0.16-beta-sources.jar auf sb-test; nur gelesen, nichts übernommen):

| Frage | Befund | Folge |
|---|---|---|
| Trennt eine Bedrock-Lage *innerhalb* einer Sektion die Ebenen beim Rendern? | `VisGraph.resolve`: < 256 feste Blöcke → alle Seiten verbunden; sonst Flutung je Luftbereich. `SectionOcclusionGraph` (Zeile ~291) läuft nur nicht **entgegen** schon gegangener Richtungen → Weg „Ost, dann Unten“ ist erlaubt; die Nachbarsektion sieht über ihre Seitenfläche oben **und** unten | **Nein** – Leck (Bild `kern_culling.png`). Nur eine **ganze** feste Sektion (`empty == 0` → `setAll(false)`) trennt sicher |
| Sieht man Wesen anderer Ebenen? | `ChunkMap.TrackedEntity.updatePlayer`: Abstand nur **waagrecht** (`dx² + dz²`) | Spieler zweier Ebenen werden einander geschickt; Namensschilder (Sicht 64 Bl., durch Wände) wären an den Trennlagen sichtbar (17 Bl. Abstand) → Filter je Ebene nötig (§3.4) |
| Chunk-Pakete je Spieler? | `PlayerChunkSender.sendChunk(connection, level, chunk)` baut `ClientboundLevelChunkWithLightPacket` **je Verbindung** | Ein Masken-Eingriff je Spieler („Bandfenster“, §3.3) ist möglich |
| Vorlade-Tickets | `TicketType` mit Registry (`PORTAL` 300 Ticks, `ENDER_PEARL` 40) | eigener Typ `simplelib:portal` registrierbar |
| Biom-Attribute je Ebene | `EnvironmentAttributes`: `visual/fog_color`, `fog_start_distance`, `fog_end_distance`, `ambient_light_color`, `block_light_tint`, `audio/background_music`, `audio/ambient_sounds`, `gameplay/natural_mob_spawns`, `gameplay/bed_rule`, `gameplay/respawn_anchor_works` … | Nebel, Helligkeit, Musik, Klang, Spawns, Betten **je Ebene über das Biom nach Höhe**, ohne eigenen Render-Code |
| Sprachdateien | `ClientLanguage` lädt `lang/<code>.json` aus **allen** Namespaces | Sprach-Teildateien je Ebene ohne Konflikt möglich (§6) |
| Durchsicht-Zeichnen | `SubmitNodeCollector`: `submitBlockModel`, `submitMovingBlock`, `submitCustomGeometry(PoseStack, RenderType, …)`; Fabric 26.x: `LevelRenderEvents` | Tiefenfenster ohne zweiten Welt-Durchlauf machbar (§5.6), eigener RenderType/Pipeline für das Löschen der Tiefe nötig |
| Gametest-Adapter | `tools/check_gametest_adapters.py` liest **alle** `*.java` unter `modules/<mod>/fabric` (rglob) | Adapter-Klasse je Ebene erlaubt |

**Spike-Entscheidung:** Kein Java-Spike in diesem Schritt. Die drei Kernfragen, die ein *kleiner* Spike hätte klären sollen (trennt
eine Bedrock-Lage? sieht man fremde Wesen? lässt sich je Spieler maskieren?), sind durch die 26.3-Quellen entschieden. Was offen bleibt
(Speicher/FPS/Vernetzungszeit am echten Client, Verhalten mit Sodium), braucht einen **Client-Lauf** in der Schwer-Schlange (Stunden) und
ist als Stufe **S0** der Umsetzung geplant (§8, Messplan §3.2) – ein Mini-Spike jetzt würde dasselbe nur halb messen.

---

## Revisionsverlauf (je: prüfen → bewerten → verbessern)

Achsen (1–5, 5 = sehr gut / sehr geringes Risiko): **MB** Machbarkeit 26.3 Fabric · **PF** Performance/RAM · **RR** Risiko Sodium/Iris ·
**MU** Modul-Unabhängigkeit (Kern-Regeln 10/11) · **A12** Vanilla-Nutzen/Eingriff · **PAR** 10 Ebenen parallel baubar ohne Konflikte.

### K-R1 – Fortschreibung des Ist-Codes
**Entwurf:** `BackroomsLevel` auf 10 Werte erweitern, `Piece` um alle neuen Blöcke ergänzen, ein `BackroomsStackGenerator` ruft je y
die passende `at()`-Funktion; Raster 112 aus R8. Türen: `BackroomsDoorBlock` in Simple Layers, Ziel per Switch. Daten weiter in
`gen_data.py`, Texte in `en_us.json`, Tests in `LayersTests`/`ModuleGameTest`.
**Prüfung:** (a) **PAR:** jede Ebene ändert `Piece`, `BackroomsLevel`, `BackroomsBlocks`, `LayersRegistry`, `gen_data.py`, beide
Sprachdateien, `manual.json`, `ModuleGameTest`, `LayersTests` – zehn Branches erzeugen in denselben Zeilen Konflikte (Enum-Enden, Listen-
Enden, JSON-Ende). (b) **PF:** `at()` je Block: 10 × 16·16·96 = 245 760 Aufrufe je Säule; bei ~60–100 ns je Aufruf (Hash + Zellenlogik)
≈ 15–25 ms je Säule nur für Blöcke, dazu `ProtoChunk.setBlockState` mit Sperre je Aufruf. (c) **MU:** Tür-Logik nicht wiederverwendbar
(Besitzer: „Portal-System in SimpleLib, damit andere Mods es nutzen können“). (d) keine Antwort auf Speicher, Wesen-Sicht, Durchsicht.
**Bewertung:** MB 4 · PF 2 · RR 4 · MU 2 · A12 4 · PAR 1. **Verworfen**; brauchbar: Raster, reine Generatoren, Tests aus demselben Code.

### K-R2 – Ebene als Baustein, eigene Pakete, Portal-API in SimpleLib
**Neu:**
1. **Interface `Band`** (§4.1) statt Enum-Switch; jede Ebene ein eigenes Paket `…simplelayers.backrooms.level.eNN` mit eigenem
   `EnnPiece`-Enum (implementiert `BandPiece`) und eigener Palette `EnnPiece → BlockState`. Kein gemeinsames `Piece` mehr.
2. Der Kern legt **alle 10 Pakete als Stummel** an (Platzhalter-Ebene: Raumraster aus Kern-Modulen in Stummel-Farben) und eine feste
   Liste `Bands.ALL = {E01Band … E10Band}`. Ein Ebenen-Branch ändert danach nur Dateien in seinem Paket → keine Liste wächst.
3. **SimpleLib `com.simplelib.api.portal`** (§5): Rahmenregel, Verknüpfung, Regeln, Übergang, Vorladen – ohne Backrooms-Wissen.
4. Raster 112 (96 offen + 16 Grundgestein) mit **band-lokalen Koordinaten** `ly 0..95` für alle Generatoren; nur der Kern kennt Welt-y.
**Prüfung:** PAR für Java gelöst, aber Ressourcen bleiben Einzeldateien (Sprache, `gen_data.py`, Vanilla-Tags `mineable/*.json`,
`backrooms_structure`-Tag, `sounds.json`, Wiki, Gametest-Adapter). Leistung: unverändert Block-für-Block. Speicher: der Client hält jede
Säule ganz (71 Sektionen) – noch nicht beziffert. Wesen anderer Ebenen (waagrechte Tracking-Distanz) übersehen. Tür-Zustand (aktiviert/
kaputt) ohne Speicherort.
**Bewertung:** MB 4 · PF 2,5 · RR 4 · MU 4 · A12 4 · PAR 3.

### K-R3 – Leistung und Speicher: Schreibpfad, Budget, Messplan, Bandfenster, Wesen-Filter
**Neu:**
1. **Schreibpfad `BandWriter`** (§4.2): der Kern schreibt Trennlagen und massive Füllung als **einwertige Sektionen** (Palette mit einem
   Eintrag, keine 4096 Einzelaufrufe), sperrt jede Sektion einmal (`acquire`/`release`, ungesperrtes Setzen wie Vanilla-Rauschgenerator),
   pflegt die Höhenkarten nur für die oberste Ebene. Generatoren dürfen `fill()` überschreiben (ganze Zellen/Wände/Böden als Quader statt
   `at()` je Block); `at()` bleibt Pflicht (Tests, Bilder).
2. **Budget:** ≤ 1 ms je Ebenen-Chunk (16×96×16) im Mittel, ≤ 10 ms je Säule (10 Ebenen) inkl. Großbauwerke; reiner Leistungstest je
   Ebene meldet über Budget als Warnung, über 5× Budget als Fehler (Zeitmessungen sind auf sb-test schwankend → keine harte 1×-Grenze).
3. **Speicher beziffert** (Schätzung, wird in S0 gemessen; Rechnung §3.2): Stapel schlimmstenfalls ~2× Oberwelt je Chunk (bis 60 gemischte
   Sektionen statt ~20), typisch ~1,5×. Bei Sichtweite 12: ~200 MB statt ~90 MB Client-Chunkdaten.
4. **Ausweg M2 „Bandfenster“** (nur falls S0 es verlangt): der Server schickt je Spieler nur die Ebenen n−1, n, n+1 echt, alle anderen
   Sektionen als einwertiges Grundgestein (Maske in `PlayerChunkSender`, Licht-Bitsets mitmaskiert, Block-Updates fremder Ebenen
   gefiltert); beim Ebenenwechsel n→n+1 wird n+2 im Hintergrund nachgeschickt – das Ziel jeder Tür ist dadurch **immer schon da**.
   ~0,6–0,7× Oberwelt. Verworfen: **M3** „Ebenen spät füllen“ (erst Grundgestein erzeugen, beim Betreten nachgenerieren) – doppelte
   Erzeugung, sichtbares Nachladen, Speicherflag je Säule und Ebene, hohes Risiko.
5. **Wesen-Filter je Ebene** (immer, klein): SimpleLib-Haken `TrackingFilters` (Mixin `ChunkMap.TrackedEntity.updatePlayer`), Simple Layers
   meldet „nur gleiche Ebene“ → keine Namensschilder durch die Trennlage, weniger Netz.
6. **Ebenen-Stimmung über Biom-Attribute** (Nebel Anfang/Ende/Farbe, Umgebungslicht, Musik, Umgebungsklang, keine Spawns, Bettregel) statt
   eigener Render-Haken → Sodium/Iris-neutral.
**Prüfung:** M2 greift tief in Server-Chunkversand ein (Konflikte mit Chunk-Versand-Mods möglich) → nur bei Messbedarf, Schalter
`backrooms.bandWindow` (`auto`/`on`/`off`, auto = an ab Sichtweite > 16). Ressourcen-Konflikte und Tür-Zustand weiter offen.
**Bewertung:** MB 4 · PF 4 · RR 4 · MU 4 · A12 4 · PAR 3.

### K-R4 – Portal-System im Detail: Verknüpfung, Zustand, Übergang, Durchsicht-Stufen
**Neu:**
1. **Tür = Vanilla-artige Tür** (Unterklasse `DoorBlock`, öffnet/schließt normal) + **Block-Entity mit Link** (Ziel-Position, -Ausrichtung,
   Breite, Regel, Link-Id). Generierte Türen bekommen den Link schon bei der Erzeugung (BE-NBT im ProtoChunk); gebaute Türen (S3) über
   denselben Weg → **ein** Codepfad. Zustand (OFFEN/RUHEND/KAPUTT) zentral in `PortalStates` (SavedData je Dimension, nur Abweichungen vom
   Startzustand gespeichert) – beide Enden lesen dieselbe Zeile, auch wenn ein Ende ungeladen ist.
2. **Rahmenprüfung** bei Nachbaränderung → Blockzustand `portal = active|dormant|broken|unframed` (für Symbol-Leuchten am Client, kein
   eigenes Sync-Paket).
3. **Übergang S1** (Bild `kern_tuer.png`): Client erkennt Kreuzen der Türebene (Körpermitte, Tür offen, Zustand aktiv) → sendet
   `PortalCrossPayload(linkId)` und versetzt sich **im selben Frame** (Position, Blick, Tempo um k·90° gedreht). Server prüft (Abstand
   ≤ 1,5 zur Öffnung, Zustand, Abklingzeit 10 Ticks) und teleportiert über den **normalen Vanilla-Teleport** (gleiche Dimension → kein
   Ladebildschirm; Teleport-Quittung verhindert „moved wrongly“; Pakete sind geordnet: Payload kommt vor der nächsten Bewegung). Abgelehnt →
   Vanilla-Korrektur setzt zurück. **Nebelpuls 0,2 s** in Ebenenfarbe deckt das Vernetzen der Zielsektionen ab (sie waren unsichtbar, also
   nie vernetzt) – rendererunabhängig, auch mit Sodium.
4. **Durchsicht-Stufen** S0–S3 (§5.6) mit **Rückfallkette** Tiefenfenster → Kulisse → Schleier; Kulisse (Bild der Zielebene mit leichter
   Parallaxe) ist neu und ersetzt den bloßen Schleier als Rückfall bei Shaderpacks.
**Prüfung:** Ein-Codepfad und zentrale Zustände tragen alle drei Besitzer-Regeln und S3. Offen: (a) Doppeltür-Erkennung bei zwei
gespiegelten Türen; (b) wer stempelt die Tür in die Generatoren (Ebene oder Kern?) – sonst baut jede Ebene Türen anders; (c) Haus ↔
Lobby-Doppeltür: die Lobby-Seite muss die Haus-Position kennen, Haus-Platzierung ist heute ein Zufalls-Feature.
**Bewertung:** MB 4 · PF 4 · RR 4 (S1) / 3 (S2) · MU 5 · A12 4 · PAR 3.

### K-R5 – Konfliktfreie Ressourcen, Tür-Stempel im Kern, reine Haus-Platzierung, Migration
**Neu:**
1. **Datei-Eigentum** (§6): je Ebene eigene Daten-Unterordner, eigenes Generator-Skript `tools/backrooms/gen_eNN.py` (vom Kern per Glob
   gefunden), Sprach-Teildateien im eigenen Namespace `assets/simplelayers_eNN/lang/` (geladen wie oben belegt), Ebenen-Tags
   `simplelayers:backrooms/eNN_*` (vom Kern vorab in die Vanilla-Tags eingetragen), **vorregistrierte Klang-Ereignisse** je Ebene
   (`backrooms.eNN.ambient|hum|door|event1..3`, Ebene tauscht nur `.ogg`), Wiki-/Buch-Teilstücke je Ebene, Gametest-Katalog + Adapter
   je Ebene. Keine neuen Config-Schlüssel aus Ebenen-Branches.
2. **Tür-Stempel im Kern:** die Ebene **reserviert** die Türnische (Kern-Teil `DOOR_SLOT`, Rahmen aus eigenen Blöcken), der Kern setzt
   Türblock + BE + Wartungskasten. Türblöcke `backrooms_door_e01…e10` (Symbol = **Ziel**-Ebene) registriert der Kern; die Ebene liefert nur
   die Textur (Stummel-PNG im Kern, Ebene ersetzt die Datei). Doppeltür = zwei Türen mit gespiegelten Scharnieren und derselben Link-Id.
3. **Haus-Platzierung rein:** `Entrances.house(seed, regionX, regionZ)` (1 Haus je Region R×R Chunks mit Seltenheit aus Config) – dieselbe
   Funktion nutzt das Oberwelt-Feature und die Lobby (Ebene 1 setzt ihre Eingangs-Doppeltür in dieselbe Säule).
4. **Migration** (§7) und Übernahme b-back20s.
**Prüfung:** PAR jetzt fast vollständig. Restkonflikte: Kern-Dateien selbst ändern sich, während Ebenen bauen → Regel „API-Freeze“ +
additive Kern-Nachträge. Speicher-/Durchsicht-Risiken sauber in S0/S2 getrennt. Noch zu prüfen: alle Auftragspunkte, Besitzer-Wort „100“.
**Bewertung:** MB 4 · PF 4 · RR 4/3 · MU 5 · A12 4,5 · PAR 4,5.

### K-R6 – Gegenprüfung gegen Auftrag und Besitzer-Wunsch, Korrekturen
| Punkt (Auftrag / Besitzer) | Wo | Korrektur in K-R6 |
|---|---|---|
| eine Dimension, 10 Bänder „à 100“ | §1 | Abweichung 112 begründet (§1.2); spürbare Raumhöhe ist dieselbe wie bei „100 mit 4 Grundgestein“; Rückfrage k1 |
| Höhengrenzen, Bedrock-Trennung, nur eigene Ebene sichtbar | §1, §3 | Hülle-Regel (Verkleidung neben Luft, Kern Grundgestein) auch für die Trennsektion: Unterseite der oberen/Oberseite der unteren Ebene = Ebenen-Verkleidung, nie sichtbares Grundgestein |
| Client hält Chunk-Säule – Lösung/Messplan | §3.2, §3.3 | Messplan mit Schwellen statt Bauchgefühl; M2 automatisch nur ab Sichtweite > 16 |
| Ebenen-API mit Bandindex, Palette, Raumtypen-Pool, Großbauwerke, Labyrinth-Grad, Licht/Nebel/Ambient, Loot, Tür-Regel | §4.1 | Licht/Nebel/Ambient zweigeteilt: Biom-Daten im `gen_eNN.py`, Java nur Schleierfarbe/Kulisse |
| gemeinsame Hilfen: Treppen-Regel STAIRS22, Maze/EscapeCheck, Raum-Module | §4.3 | Treppen nur noch über `StairBuilder` (ein Lauf = eine Prüfung), Leiter-Regel und Rampen mit drin |
| Rahmen L/R/oben, Doppeltür, Verknüpfung, Regeln je Ebene, Reparatur, Wartungskasten, Symbole | §5 | Doppeltür-Erkennung festgelegt (§5.2); Reparatur nimmt das Teil der **eigenen** Ebene (R8) |
| Vorladen Gegenseite (gleiche Säule bzw. Ticket), nahtlos, auch zurück | §5.4/5.5 | Tür-Paare liegen im **selben Chunk** (nicht nur ≤ 16 Bl.) → Ziel ist garantiert geladen; Ticket nur für S3 und Keller |
| Durchsicht S0–S3 | §5.6 | ehrliche Grenzen (keine Wesen, kein Portal-im-Portal, Shaderpack → Kulisse) |
| Keller-Doppeltür (Dimensionswechsel) mit Kulisse/Nebel | §5.7 | Vorlade-Ticket in der Stapel-Dimension ab 8 Bl. Annäherung verkürzt die Wartezeit |
| Oberwelt-Türen extrem selten, API für andere Mods | §5.8 | Kern liefert nur API + Reichweiten-/Durchlass-Grenzen; Inhalt (Bauteile) kommt aus e10 |
| Migration, b-back20s-Teile | §7 | Altgeneratoren werden im Kern als **Startinhalt** in e01/e02/e04/e05 verschoben (Ebenen beginnen nicht leer) |
| Implementierungsplan, 10 Ebenen parallel | §8, §6 | API-Freeze nach K2; danach Ebenen in Wellen ≤ 4 Agenten (Regel Ressourcen) |
| Modul-Unabhängigkeit, Regeln 10/11, A12 | §9 | Kern (`common/src/shared`) unberührt; SimpleLib-Portal kennt keine Backrooms; Simple Layers ohne SimpleLib nicht vorgesehen (heute schon Pflicht) |
**Bewertung K-R6 (Endstand):** MB 4 · PF 4 (S0 bestätigt) · RR 4 (S1) / 3 (S2) · MU 5 · A12 4,5 · PAR 4,5.

### K-R7 – Risiko-Durchgang (was geht am wahrscheinlichsten schief?)
1. *Ebenen halten sich nicht an die Türnische* → Kern-Test `eNN_doors_framed` läuft **für jede Ebene automatisch** (Kern-Katalog iteriert
   `Bands.ALL`), nicht nur, wenn die Ebene ihn schreibt.
2. *Leistungsbudget wird von einer Ebene gesprengt* → Kern-Test `bands_gen_budget` misst alle Ebenen; Bündel sieht es sofort.
3. *Großbauwerk schneidet Türnischen* → Vorrang-Regel: Türen werden zuerst bestimmt (`DoorNet`), Großbauwerk-Platzierung meidet Chunks
   mit Tür oder muss die Tür als Anschluss aufnehmen (`GrandStructure.doorSlots`); Test prüft „jede Tür erreichbar“.
4. *API-Änderung nach dem Freeze* → nur additiv (neue Default-Methoden), nie Signatur ändern; Bruch nur mit Ansage an alle Ebenen-Branches.
5. *Sodium vernetzt nach dem Teleport langsamer* → Nebelpuls-Dauer folgt der Messung (S0), Obergrenze 0,4 s.
6. *Dimension 1136 hoch* → Vanilla-Grenzen ok (Vielfaches von 16, ≤ 4064, `min_y` ≥ −2032); `/locate`, Karten, Höhenkarten nur oberste Ebene
   relevant; Kartenitem in der Stapel-Dimension zeigt die oberste Ebene → Karten in den Backrooms aus (Biom-/Dimensionsattribut), Wegkarten
   der Archive sind eigene Items.
**Bewertung K-R7:** unverändert, Risiken mit Gegenmittel und Test belegt.

### K-R8 – Abgleich mit dem Ebenen-Konzept A (b-back22-ebenen-a c89aa6852, „Gesammelte Anforderungen an den Kern“)
| Anforderung E1–E5 | Kern-Antwort |
|---|---|
| Ebenen-Parameter (Zelle, Geschosshöhe, Nebel, Ambient, Großbauwerk, Regel, Teil, Beute) | in `Band` (§4.1) bzw. Biom-Skript; passt |
| Region-Rauschen „Geschosszahl“ | neu im Kern: `RegionNoise` (rein, je Region 256×256) |
| Treppenhaus `StairWell(H, Lauf1, Podest, Lauf2, Breite)` + Prüfer, Tabelle H 7/8/10/12 | `StairBuilder` kann gerade, L- und U-Läufe mit Podest; Wendeltreppe (E3) und Leiterschacht (E5) ebenfalls im Kern (eine Regel, ein Prüfer) |
| Türkammer (Rahmen, Symbolband, Wartungskasten) | Kern-Modul „Türraum“ + Tür-Stempel |
| **Tür-Optik je Ebene** (z. B. Rolltor E4) | **Änderung gegenüber K-R5.2:** jede Ebene registriert **ihre eigene Tür** (Skin der Quell-Ebene) mit Blockzustand `symbol = 0..10` (0 = Haus); Symbol-Overlays `textures/backrooms/symbol/eNN.png` gehören der Ziel-Ebene; `Band.doorBlock()` (Standard: Kern-Tür). Kein 10×10-Block-Kreuzprodukt, konfliktfrei |
| Bruch-Tür mit Teil der Ebene n bzw. n+1 | = R8: jede Seite nimmt das Teil der eigenen Ebene; passt |
| Kreide-Pfeile in Fluchtrichtung | Kreide ist Kern → `ChalkMarks` (Kern-Teile `CHALK_*`, Richtung aus `EscapeCheck`) |
| Wegkarten/Karten-Hook (E3) | Kern macht `DoorNet.doorsNear(seed, band, x, z, r)` öffentlich (rein); Item + Darstellung bleiben in E3 |
| Beute-Pfad `chests/layers/e<n>_*` | vereinheitlicht auf `simplelayers:backrooms/eNN/<art>` (Unterordner je Ebene, §6); garantierte Teile über `Band.loot()` |
| Rätsel-Blockentities (Katalog-Fach, Ablass, Dampfzone, Aufzug-Last) | bleiben **in der Ebene** (nur eine Ebene nutzt sie) |
| **Bewegte Plattform** (E4 + E7) | von zwei Ebenen genutzt → Kern-Baustein, eigene additive Stufe **K7** vor der Welle mit E4/E7; Technik im K7-Plan (Vanilla-nah, Spieler-Mitnahme) |
| Hülle-Verkleidung je Ebene | `Band.palette()` liefert `HULL_SKIN`; Kern-Test „kein Grundgestein neben Luft“ |
| Mob-Spawn aus, Nebel Start/Ende, Partikel | Biom-Attribute (§0-Tabelle), S0-Prüfpunkt |
| Tests rein (Treppen, Erreichbarkeit, Rahmen, Schwebendes, Teil-Abstand) | Kern-Katalog läuft sie für **alle** Ebenen; Weglängen/Landmarken bleiben Ebenen-Tests |
Regel daraus: **Was ≥ 2 Ebenen brauchen, gehört in den Kern; was eine Ebene braucht, bleibt in ihrem Paket.**
**Bewertung K-R8:** MB 4 · PF 4 · RR 4/3 · MU 5 · A12 4,5 · PAR 5 (Tür-Skins jetzt ebenfalls konfliktfrei).

### K-R9 – Abgleich mit Ebenen 6–10 und Gesamtprüfung (KONZEPT-B-BACK22-EBENEN-2026-11-10.md, G-R1–G-R3)
Fehlende bzw. falsche Bausteine, alle **vor dem API-Freeze in K2** (additiv, Regel „≥ 2 Ebenen → Kern“):
1. **Licht ohne Himmelslicht:** `BandView`/Biom-Skript bekommt `ambientLight` (Biom-Attribut, S0-Prüfpunkt) und `lightGrid` (Rückfall B′: leuchtender
   Bodenblock der Ebene im 4×4-Raster, 16 Lichtquellen/Chunk). Voll-Leuchtboden (256/Chunk) verworfen (Budget ×20–50).
2. **`StairBuilder`:** Wendeltreppen nur **eckig** (gerade Läufe ≤ 8 an der Wand, Eckpodest) – Voxel-Kreisstufen liegen über Eck und sind nicht begehbar;
   **Leiter-Austritt 3 Luft** (vorher 2); **Rampen** nur aus ½-Stufen (Platte/Vollblock, Auto-Step), Steigung ≤ 1:2; Teil `CRAWL` (1,5 hoch, Schleichen)
   ist vom Kopffreiheits-Test ausgenommen, braucht Eingangsmarke.
3. **Neue Bausteine:** `GalleryWell` (Galerie-Schacht/Atrium: Grundfläche, Geschosse, Brüstung; E1/E2/E3/E7) · `PointerBlock` (Eigenschaft `rotation`
   0–15) + `Pointers.toward(seed, x, z, ziel)` (E8/E10, Kreide) · `PuzzleController` + `PuzzleGate` (Zustand aus Seed, `solved()` liefert die Ebene,
   Test „jede Startstellung lösbar“; E3/E5/E6/E7/E8/E10) · `SoundBeacon` (Intervall, Reichweite; E8/E9) · `FacadeRoom` + `NumberedLock` + Tag
   `simplelayers:backrooms/keys` (E7/E9, Schlüsselbund) · `ExitDoor` (Ausgang ohne Link, Ziel Respawn bzw. Eingangshaus, Nebel-Übergang; E1/E10) ·
   K7-Plattform mit n Haltestellen und Eingabe-Haken der Ebene.
4. **`BuildRules` ergänzt:** Sturzschutz (Abgrund > 3 neben Weg → Brüstung oder Wasserpolster) · Decke-als-Himmel (Decke ≥ Nebelende + 8 über jedem
   begehbaren Freiluft-Augpunkt) · keine tickenden Pflanzen (Abendweizen statt Weizen, Laub persistent) · Ton-Emitter-Reichweite < Abstand zu fremden Ebenen.
5. **Tür-System:** Wartungskasten **repariert/aktiviert seine Tür direkt** (kein Item, keine Teile-Ausbeute; nur an aktivier-/brechenden Türen);
   Aktivierung 2↔3 von beiden Seiten mit dem Teil der eigenen Ebene (§5.3 präzisiert). Oberwelt-Paare (§5.8) **verankert**: Umsetzen eines Endes →
   RUHEND, Reaktivieren verbraucht Sicherung + Türschließer (Frage g15 im Gesamtdokument).
6. **Reihenfolge-unabhängig:** Pakete, Sprach-Namespaces, Tags, Beute-Pfade und Testnamen nach **Thema** (`level.lobby`, `simplelayers_pools`,
   `pools_*`) statt `eNN`; Position nur in `Bands.ORDER` → die Besitzer-Wahl g1 ändert eine Zeile. (§6-Tabelle: „eNN“ lies „<thema>“.)
7. **Beute/Fortschritt:** `BandLoot.oncePerPlayer` (SavedData-Flag; Kernkammer E10, Brunnen-Laterne E8); Fortschritte „Ebene n erreicht“ aus
   `Bands.ALL`; Wegbuch-Teile-Liste aus `Band.repairPart()`.
**Bewertung K-R9:** MB 4 · PF 4 · RR 4/3 · MU 5 · A12 4,5 · PAR 5 (K2 wird größer, Ebenen-Wellen unverändert).

---

## 1. Dimension und Bänder (Endstand)

### 1.1 Werte (Bild `kern_schnitt.png`)
- Dimension `simplelayers:backrooms_stack`, Generator-Codec `simplelayers:backrooms_stack`, Biomquelle `simplelayers:band_stack`.
- `min_y −816`, `height 1136` (71 Sektionen), `has_skylight false`, `has_ceiling true`, Umgebungslicht über Biom-Attribute je Ebene.
- Raster 112: Ebene i (1 = oben) offen `y ∈ [208 − 112(i−1), 303 − 112(i−1)]`, darunter Trennsektion `[192 − 112(i−1), 207 − 112(i−1)]`,
  Deckel 304–319. Ebene 1: 208–303 · 2: 96–191 · 3: −16–79 · 4: −128–−33 · 5: −240–−145 · 6: −352–−257 · 7: −464–−369 · 8: −576–−481 ·
  9: −688–−593 · 10: −800–−705; Boden −816…−801.
- Generatoren sehen nur **band-lokale** `ly 0..95` (0 = unterste offene Lage). Umrechnung nur in `BandStack.worldY(i, ly)` /
  `BandStack.bandAt(y)`. Ändert sich das Raster später (z. B. gemischtes Raster R8.2), ändert sich keine Ebene.
- **Hülle:** Trennsektionen bestehen aus Grundgestein; die Ebene darf ihre unterste/oberste Lage (`ly 0`, `ly 95`) mit Verkleidung belegen
  (Under21-Regel „fest neben offen = Verkleidung“). Kern-Test: kein Grundgestein grenzt an eine passierbare Zelle.

### 1.2 Besitzer-Wort „100 Blöcke“ – Abweichung begründet
- 100 ist kein Vielfaches von 16. Eine Trennlage, die nicht eine **ganze** Sektion füllt, trennt das Rendern nachweislich nicht (§0, Bild
  `kern_culling.png`): dann würden Nachbarebenen mitvernetzt und mitgezeichnet – genau das, was der Besitzer vermeiden wollte („nicht alle
  Schichten rendern gleichzeitig“).
- Ein 100er-Raster mit 4 Lagen Grundgestein hätte **96 offene Blöcke** je Ebene – genau so viel wie das 112er-Raster. Der Spieler spürt
  keinen Unterschied; nur die unsichtbare Trennlage ist 16 statt 4 dick (kostet fast nichts: einwertige Sektion).
- Bezeichnungen bleiben „≈ 300–200, 200–100 …“ (nominal), F3 zeigt die echten Werte. Alternative mit mehr Höhe: Raster 128 (112 offen) →
  Höhe 1296 – mehr Speicher je Ebene, kein Spielvorteil erkennbar. **Rückfrage k1** (Empfehlung: 112).

## 2. Laden, Erzeugen, Sichtbarkeit
- Ein Chunk ist eine Säule über alle 10 Ebenen; erzeugt wird immer die ganze Säule (Vanilla kennt keine Teil-Chunks).
- Gezeichnet wird nur die eigene Ebene (Trennsektion), vernetzt ebenfalls nur, was die Sicht-Flutung erreicht.
- Biom je Ebene nach Höhe (einwertig je Sektion, weil Ebenengrenzen auf Sektionsgrenzen liegen) → Nebel/Licht/Klang je Ebene über
  Vanilla-Attribute; Übergang beim Teleport ist hart (gewollt: neuer Ort).

## 3. Leistung und Speicher

### 3.1 Erzeugung
- Kern-Schreibpfad `BandWriter`: einwertige Sektionen für Trennlagen/Füllung, Sektions-Sperre einmal je Sektion, Höhenkarten nur oben.
- Ebenen schreiben über `fill()` in Zellen-/Quader-Schritten; Budget ≤ 1 ms je Ebenen-Chunk, ≤ 10 ms je Säule (Test `bands_gen_budget`).
- Großbauwerke und Irrgärten sind reine Funktionen mit Region-Cache (Muster `Maze`, begrenzte Caches), keine Jigsaw-Strukturen.

### 3.2 Speicher – Rechnung und Messplan
Schätzung je Chunk-Säule am Client (Blockpalette 4–8 Bit ≈ 2–4 KB je gemischter Sektion, Blocklicht 2 KB je beleuchteter Sektion,
einwertige Sektionen ~0,2 KB Verwaltung; Oberwelt zusätzlich Himmelslicht 2 KB je Sektion):

| Fall | gemischte Sektionen | KB je Säule | Sichtweite 12 (625 Säulen) | Sichtweite 24 (2401) |
|---|---|---|---|---|
| Oberwelt (Vergleich) | ~20 von 24 | ~140 | ~90 MB | ~340 MB |
| Stapel schlimmstenfalls (jede Ebene 6/6 gemischt, beleuchtet) | 60 von 71 | ~310 | ~195 MB | ~745 MB |
| Stapel typisch (Ebenen 3–5 gemischt; Feld/Vorstadt weniger) | ~40 | ~210 | ~130 MB | ~505 MB |
| Stapel mit M2 Bandfenster (3 Ebenen echt) | ~15 | ~95 | ~60 MB | ~230 MB |

Server: gleiche Größenordnung je geladener Säule, Region-Dateien ~2× Oberwelt. Vernetzung (Mesh) und GPU-Speicher nur für die eigene Ebene.

**Messplan S0** (Client-Lauf auf sb-test, Welt mit Stapel-Dimension und **Worst-Case-Inhalt**: jede Ebene dicht gefüllt, alle 6 Sektionen
gemischt, Neonlicht; Vergleich Oberwelt gleicher Seed):
1. Heap nach GC (`jcmd GC.class_histogram`: `LevelChunkSection`, `PalettedContainer`, `DataLayer`) bei Sichtweite 8 / 12 / 24.
2. Sichtbare/vernetzte Sektionen (F3 `C:`-Zeile; mit Sodium dessen F3-Zeilen) in jeder Ebene – Ziel: nur die eigene Ebene.
3. Erzeugungszeit je Säule (Zeitstempel um `buildTerrain`, Mittel/95 %), Flug mit 30 Bl./s 60 s lang, TPS.
4. Zeit von Teleport bis „Zielsektionen vernetzt“ (Frames mit Loch) – Vanilla und Sodium → Länge des Nebelpulses.
5. Region-Dateigröße je 32×32 Chunks.
6. Nebel Anfang/Ende/Umgebungslicht je Biom wirksam (Bildvergleich zweier Ebenen).
**Schwellen:** Heap-Chunkdaten ≤ 2,5× Oberwelt bei Sichtweite 12 → ohne M2 bauen; darüber oder FPS < 80 % der Oberwelt → M2 in den Kern.
Erzeugung > 10 ms/Säule im Mittel → Ebenen mit den höchsten Kosten über `fill()` beschleunigen, bevor Ebenen gebaut werden.

### 3.3 M2 „Bandfenster“ (bedingt)
Server-Maske je Spieler: `PlayerChunkSender.sendChunk` baut das Paket mit einer Thread-lokalen „sichtbare Ebenen“-Angabe; fremde
Sektionen werden als einwertiges Grundgestein geschrieben, Licht-Bitsets gekürzt; `ChunkHolder`-Block-Updates fremder Ebenen pro Spieler
verworfen; Ebenenwechsel n→n±1 schickt die neue Nachbarebene im Hintergrund (Chunk-Batches, Vanilla-Taktung). Risiko: Chunk-Versand-Mods
(C2ME u. a.) → Schalter `backrooms.bandWindow = auto|on|off`. Nur in der Stapel-Dimension aktiv.

### 3.4 Wesen und Klänge anderer Ebenen
SimpleLib `TrackingFilters` (eine Mixin-Stelle `ChunkMap.TrackedEntity.updatePlayer`, Provider-Liste wie `HeightHandover`); Simple Layers
meldet „nur gleiche Ebene“ in der Stapel-Dimension. Klänge (3D-Abstand, max. ~16 Bl.) hört man höchstens an den Trennlagen → hingenommen.

## 4. Ebenen-API (Simple Layers, Paket `…simplelayers.backrooms.api`)

### 4.1 Band – ein Baustein je Ebene
```java
public interface Band {
    int index();                               // 1..10, fest durch das Paket eNN
    String id();                               // "lobby", "poolrooms", … (Biom backrooms_eNN, Texte)
    BandPalette palette();                     // BandPiece -> BlockState (Array nach ordinal, einmal aufgebaut)
    BandLayout layout();                       // reiner Generator (MC-frei), s. u.
    RoomPool rooms();                          // Raumtypen mit Gewicht + Anschlüssen (Kern-Module oder eigene)
    MazeSpec maze();                           // Regiongröße, loops 0..1 (Labyrinth-Grad: oben offen, tief verwinkelt)
    List<GrandStructure> grandStructures();    // je Region 256×256 höchstens eines, reine Platzierung
    DoorRule doorRule();                       // TWO_WAY | ACTIVATE_ONCE | BREAK_ON_TRANSIT – gilt für Paar n<->n+1 (obere Ebene, g3 a)
    @Nullable Supplier<Item> repairPart();     // Ersatzteil dieser Ebene (null bei TWO_WAY)
    float doorDensity();                       // Anteil Chunks mit Tür-Paar nach unten (Startwert 0,04)
    default Supplier<Block> doorBlock() { … }   // Tür-Skin dieser Ebene, Blockzustand symbol 0..10 (K-R8)
    BandLoot loot();                           // Loot-Tabellen je Behälterart (simplelayers:backrooms/eNN/*)
    BandView view();                           // Schleierfarbe, Kulissen-Textur (Durchsicht-Rückfall), Übergangsfarbe
    void registerContent(BandRegistrar r);     // eigene Blöcke/Items/BE (Ids eindeutig, Reiter-Reihenfolge nach index)
    default void clientInit(BandClientRegistrar r) {}  // Render-Layer, Farben – nur Client
}

public interface BandLayout {                  // ly 0..95, x/z Welt; KEIN Minecraft-Typ
    BandPiece at(long seed, int x, int ly, int z);
    default void fill(long seed, int chunkX, int chunkZ, BandWriter out) { /* Kern: Schleife über at() */ }
    CellGraph cells();                                             // für EscapeCheck/Erreichbarkeit
    default void stairs(long seed, int cx, int cz, List<StairRun> out) {}     // für Treppen-Prüfung
    default void containers(long seed, int cx, int cz, List<Container> out) {} // Truhen/Kästen + Loot-Schlüssel
    int[] arrival(long seed, int x, int z);                        // freier Stand für Notfälle (Befehl, Fallback)
}
```
- `BandPiece` (Interface: `passable()`, `solid()`, `doorSlot()`), je Ebene ein eigenes Enum. Kern-Teile (`AIR`, `HULL`, `DOOR_SLOT`,
  `FRAME_ANY`) liefert `CorePiece`.
- **Biom-Daten** (Nebelfarbe, Nebel Anfang/Ende, Umgebungslicht, Lichttönung, Musik, Umgebungsklang, Partikel, keine Spawns, Bettregel)
  stehen im Ebenen-Skript `tools/backrooms/gen_eNN.py` → `worldgen/biome/backrooms_eNN.json`. Java kennt davon nur `BandView`.

### 4.2 Was der Kern je Säule tut (Reihenfolge)
1. Deckel/Trennsektionen/Boden als einwertige Sektionen. 2. Je Ebene `DoorNet` befragen (Paar nach oben/unten in diesem Chunk?) →
3. `layout.fill()` (Ebene muss an der gemeldeten Stelle eine Türnische `DOOR_SLOT` freilassen) → 4. Großbauwerk-Teile (über `fill`
der Ebene) → 5. Kern stempelt Türen + BE + Wartungskasten → 6. Behälter-Loot (ein Kern-Feature für alle Ebenen, Muster
`BackroomsCacheFeature`) → 7. Höhenkarten.

### 4.3 Gemeinsame Hilfen (`…backrooms.gen`, alle MC-frei)
- `GenHash`, `Maze` (aus b-back20s), `EscapeCheck` (unverändert), `CellGraph`, `RegionNoise` (z. B. Geschosszahl je Region), `ChalkMarks`.
- **`StairRule` / `StairBuilder`** (aus STAIRS22 verallgemeinert): durchgehende Läufe gleicher Richtung, Steigung 1:1, Podest spätestens
  nach 8 Stufen, **Sockel unter jeder Stufe**, **3 freie Blöcke** über jeder Stufe (2 Kopf + 1 Diagonale), Deckenöffnung beginnt eine Spalte
  vor der ersten kritischen Stufe, Geländer an offenen Seiten; Läufe gerade, L, U (Podest), Wendeltreppe; Leitern: 2 frei am Austritt; Rampen gleich. Treppen entstehen **nur** über
  den Builder, der jeden Lauf als `StairRun` meldet.
- **`WalkCheck`**: simulierter Spieler 0,6 × 1,8 mit echten Kollisionsformen läuft jeden `StairRun` ab (aus `walkStair` in STAIRS22).
- **Raum-Module**: `RoomModule` (16×16-Bauplatz × Geschosshöhe der Ebene, Anschlüsse in Seitenmitte, Breite 2 oder 3) + Kern-Module
  mit Ebenen-Palette: Gang, Halle, Treppenhaus (StairBuilder), Türraum (mit `DOOR_SLOT`), Sackgasse, Schacht. Ebenen dürfen eigene Module
  hinzufügen (im eigenen Paket).
- **`GrandStructure`**: `bounds(seed, rx, rz)`, `at(...)`, `connectors()`, `doorSlots()`; Kern löst Vorrang (Tür vor Bauwerk).
- **`BuildRules`** (Bauregel-Prüfer für Tests): nichts Schwebendes ohne Anschluss, jede Tür mit vollständigem Rahmen und erreichbar, kein
  Grundgestein neben Luft, jede Treppe läuft (WalkCheck), Wartungskasten an jedem Türende.

## 5. Tür-Portal-System (SimpleLib `com.simplelib.api.portal`)

### 5.1 Bausteine (nur API + Logik, keine Backrooms-Inhalte)
| Klasse | Aufgabe |
|---|---|
| `PortalDoorBlock` (abstrakt, erbt Vanilla-Tür) + `PortalDoorBlockEntity` | Tür mit Link (Ziel-Dimension, Ziel-Anker, Ausrichtung, Breite 1/2, Regel-Id, Link-Id); Blockzustand `portal = active/dormant/broken/unframed` |
| `PortalShape` | Anker (untere linke Ecke von innen), Ausrichtung, Breite 1/2, Höhe 2 |
| `FrameRule` | austauschbar; Standard `SIDES_AND_LINTEL`: links + rechts je 2 hoch, Sturz über volle Breite, jeweils Block mit vollem Kollisionswürfel an der Innenfläche |
| `PortalRule` (Registry, Id) | `initialState()`, `canTransit(state)`, `afterTransit(state)`, `repair(state, stack, sideLevel)` – eingebaut: `simplelib:two_way`, `simplelib:activate_once`, `simplelib:break_on_transit` |
| `PortalStates` (SavedData je Dimension) | Link-Id → Zustand, nur Abweichungen vom Startzustand |
| `PortalLinks` (SavedData) | gespeicherte Paare für gebaute Türen (S3) und Dimensionswechsel-Türen (Keller) |
| `PortalTransit` | Server: Prüfung, Teleport (Drehung k·90°, Tempo mitgedreht), Abklingzeit, nur Spieler (Standard) |
| `PortalPreload` | Ticket-Typ `simplelib:portal` (Radius 2, 200 Ticks, erneuert solange Spieler ≤ 24 Bl. an der Tür) |
| `PortalCrossPayload` / `PortalTransitPayload` | Client→Server Kreuzen; Server→Client „Dimensionswechsel durch Portal“ (für Übergangsbild) |
| Client `PortalViews` | Registry Sichtmodus je Tür: `VEIL`, `BACKDROP(texture)`, `WINDOW` – Rückfallkette |
| `TrackingFilters` | Provider „darf Spieler X Wesen Y sehen“ (für §3.4, allgemein nutzbar) |
Andere Mods nutzen: eigene Tür von `PortalDoorBlock` ableiten, Regel wählen, Links per Erzeugung (BE-NBT) oder `PortalLinks.link(a, b)`.

### 5.2 Rahmen und Doppeltür (Bild `kern_tuer.png`)
- Rahmen geprüft bei Platzieren, Nachbaränderung, Laden; Ergebnis im Blockzustand. Fehlt ein Block: `unframed` → Symbol aus, kein
  Durchgang, Aktionsleisten-Hinweis + Klang (A14).
- **Doppeltür:** zwei `PortalDoorBlock` nebeneinander, gleiche Ausrichtung, gespiegelte Scharniere, **gleiche Link-Id** → `PortalShape`
  Breite 2, Rahmen außen + Sturz 2 breit; Ziel ist ebenfalls Doppeltür. Einzelner Flügel einer Doppeltür (anderer abgebaut) = unframed.

### 5.3 Regeln je Ebene (Besitzer 10.11., Auslegung R8 g3 a)
- Paar n↔n+1 nimmt `Band(n).doorRule()`: Ebene 1 `two_way`, Ebene 2 `activate_once`, Ebene 3–10 `break_on_transit`.
- *Aktivieren:* Startzustand RUHEND; Rechtsklick mit Ersatzteil der Ebene → OFFEN (dauerhaft, beide Seiten).
- *Bricht:* Start OFFEN; nach Durchgang KAPUTT (beide Seiten, Klang + Funken); Reparatur mit dem Ersatzteil **der Ebene, in der man steht**.
- *Wartungskasten* (Simple-Layers-Block, Kern) an jedem Türende mit genau einem Ersatzteil der eigenen Ebene, füllt nicht nach.

### 5.4 Verknüpfung im Stapel
- `DoorNet.pair(seed, n, chunkX, chunkZ)`: reine Funktion „hat dieser Chunk ein Paar n↔n+1?“ (Dichte `Band(n).doorDensity()`).
  Ebene n setzt ihr Ende (Tür nach unten, Symbol n+1), Ebene n+1 ihr Ende (Tür nach oben, Symbol n) – **im selben Chunk**, Lage/Ausrichtung
  wählt jede Ebene selbst. Beide Enden sind damit immer gemeinsam erzeugt und beim Client geladen.
- Link-Id = Hash(n, chunkX, chunkZ); BE-NBT beider Enden kommt aus derselben Rechnung → kein Speicher nötig, stabil.
- Abkürzungen innerhalb einer Ebene (optional je Ebene): Paar in Chunks ≤ 3 auseinander, Regel der Ebene.
- **Symbol = Ziel:** die Tür (Skin der eigenen Ebene, `Band.doorBlock()`) zeigt über `symbol = NN` das Symbol der Ebene, in die sie führt; die Rückseite derselben Tür zeigt das
  Symbol ihres eigenen Ziels → wer sich Symbole merkt, kennt den Rückweg.

### 5.5 Nahtloser Übergang (S1)
Wie K-R4.3. Zusätzlich: Vorladen ist im Stapel nicht nötig (selber Chunk); für Keller und S3 `PortalPreload`. Reiten, Elytra-Flug,
Schlafen → kein Durchgang. Tiere/Items/Loren gehen nicht durch (A12, kein Logistik-Netz).

### 5.6 Durchsicht – Stufen S0–S3 und Machbarkeit
| Stufe | Spieler sieht | Technik | Aufwand | Risiko |
|---|---|---|---|---|
| S0 | (Messung) | Spike-Welt: Kulling, Speicher, Vernetzungszeit, Nebel je Biom | S | – |
| S1 | Schleier in Ebenenfarbe in der Öffnung, sofort drüben, Nebelpuls | eigenes Quad über `submitCustomGeometry`, kein Terrain-Eingriff | M | niedrig |
| S1+ | **Kulisse**: Bild der Zielebene mit leichter Parallaxe (±8 px nach Blickwinkel) + Nebelverlauf | Textur je Ebene (`textures/backrooms/eNN/backdrop.png`, aus einer Client-Test-Aufnahme), gleicher Quad-Weg | S | niedrig |
| S2 | **Tiefenfenster**: echte Zielblöcke hinter der Öffnung bis in den Nebel (ohne Wesen) | `LevelRenderEvents` nach dem festen Terrain: (a) Öffnung mit eigener Pipeline in Nebelfarbe zeichnen, Tiefe dort auf „fern“; (b) Zielblöcke (nur luftnahe, ≤ 9×6×16) per `submitBlockModel` versetzt + gedreht hinter die Türebene, normaler Tiefentest; Cache je Tür, neu bei Blockänderung; ≤ 2 Türen gleichzeitig, ≤ 16 Bl. | L | mittel |
| S3 | gebaute Oberwelt-Türpaare | S1 + Ticket; Durchsicht nur, wenn Ziel in Sichtweite, sonst Kulisse unmöglich (beliebiger Ort) → Schleier | M | niedrig–mittel |
- Rückfallkette: WINDOW → BACKDROP → VEIL. Iris-Shaderpack aktiv (Reflection `IrisApi.isShaderPackInUse`) → BACKDROP; Fehler beim Zeichnen
  → try/catch, Tür fällt dauerhaft für die Sitzung auf BACKDROP zurück, kein Absturz.
- **Nicht machbar ohne Immersive-Portals-Umbau:** Durchsicht in eine andere Dimension, auf ungeladene Ziele, Wesen im Fenster, Portal im
  Portal. Ehrlich so dem Besitzer vorlegen.

### 5.7 Keller-Doppeltür (Oberwelt ↔ Ebene 1, Dimensionswechsel)
- Haus aus b-back20s (`HouseTemplate`), Platzierung rein (`Entrances.house`), Keller mit Absperrband + Doppeltür (`backrooms_door_e01`, Regel
  `two_way`). Die Lobby setzt in derselben Säule ihre Eingangs-Doppeltür (Symbol „Haus“).
- Oberwelt-Seite: Kulisse der Lobby; Lobby-Seite: Kulisse des Kellers. Ab 8 Bl. Annäherung Ticket in der Stapel-Dimension (Ziel erzeugt).
- Durchgehen: Server wechselt die Dimension, schickt vorher `PortalTransitPayload` → Client zeigt statt des Ladebildschirms einen
  **Nebel-Übergang** (Bildschirm füllt sich mit Lobby-Gelb, Brummen) bis die Zielchunks da sind (Client-Mixin an der Stelle, die beim
  Respawn-Paket den Ladebildschirm öffnet; `tools/check_client_mixins.py`).
- Link: Haus-Feature trägt den Link in `PortalLinks` ein (Oberwelt-Höhe erst dort bekannt); die Lobby-Seite löst ihr Ziel darüber auf (Fallback
  ohne Eintrag: Oberwelt-Chunk per Ticket erzeugen, Haus-Position aus `Entrances.house`).
- Ausgänge (EXIT-Türen der Lobby, Scheune Ebene 10, Hintertür-Schlüssel) nutzen denselben Nebel-Übergang, ohne Durchsicht.

### 5.8 Oberwelt-Türen (S3, nur API im Kern)
SimpleLib erlaubt gebaute Paare mit Grenzen als Parameter des Rezept-Moduls (Simple Layers): nur gleiche Dimension, nur Oberwelt
(Besitzer 3), Reichweite (g9, Vorschlag ≤ 1024), nur Spieler, ein Kern = ein Paar, Regel `two_way`. Bauteile kommen aus Ebene 10 (Ebenen-Konzept).

## 6. Parallel bauen – Datei-Eigentum und Regeln
| Was | Kern besitzt (nur Kern ändert) | Ebene eNN besitzt (nur eNN ändert) |
|---|---|---|
| Java | `backrooms/api`, `backrooms/gen`, `backrooms/stack`, `backrooms/door`, `Bands.ALL` (alle 10 eingetragen), SimpleLib `api/portal` | `backrooms/level/eNN/**` (Band, Piece, Palette, Layout, Module, Großbauwerke, Items, Client) |
| Blöcke/Items | Kern-Tür (Standard-Skin), Wartungskasten, Kreide/Kreidebox, EXIT | eigene Ids inkl. eigener Tür-Skin (Kern-Test `backrooms_ids_unique`) |
| Daten | Dimension, Dimension-Typ, Biomquelle, Vanilla-Tags mit `#simplelayers:backrooms/eNN_*` vorab | `worldgen/biome/backrooms_eNN.json`, `loot_table/backrooms/eNN/**`, `recipe/eNN/**`, `tags/block/backrooms/eNN_*.json` |
| Assets | Kern-Tür-Modell, `sounds.json` mit vorregistrierten `backrooms.eNN.*` | `textures/**/backrooms/eNN/**`, Symbol `textures/backrooms/symbol/eNN.png`, `sounds/backrooms/eNN/*.ogg`, `assets/simplelayers_eNN/lang/{en_us,de_de}.json` |
| Daten-Skripte | `tools/gen_data.py` (ruft per Glob `tools/backrooms/gen_e*.py`) | `tools/backrooms/gen_eNN.py` |
| Wiki/Buch | Zusammenbau | `wiki/backrooms/eNN.json`, `LevelGuide` im eigenen Paket |
| Tests | `BandsTests` (läuft über alle `Bands.ALL`: Rahmen, Treppen, Erreichbarkeit, Budget, Hülle) | `EnnTests.ALL` (Katalog) + `EnnGameTest` (Fabric-Adapter, im Kern als Stummel in `fabric.mod.json` eingetragen); Namen `eNN_*` |
| Config | alle Schlüssel | keine neuen (Bedarf → Kern-Nachtrag) |
Regeln: (1) **API-Freeze** nach Kern-Stufe K2 (Commit-Tag im Plan); danach nur additive Kern-Änderungen. (2) Ebenen-Branch ändert keine
Kern-Datei; braucht er etwas, schreibt er es als Kern-Wunsch in seinen Plan. (3) Ebenen mergen Kern-Stände (nie umgekehrt außer im Bündel).
(4) Gezielte Tests je Ebene: `--targets module-simplelayers-fabric-263 --filter 'simplelayers:eNN_*'` + Kern-Filter `simplelayers:bands_*`.

## 7. Migration und Übernahme
- **Ersetzen ohne Welt-Umzug** (Besitzer 6): die 5 Dimensionen `backrooms`, `_warehouse`, `_pipes`, `_pools`, `_end` und ihre Typen entfallen;
  neue Dimension unter neuer Id (alte 64er-Chunks erzeugten sonst Nähte). Spieler in einer entfallenen Dimension landen über den
  Vanilla-Rückfall am Welt-Spawn (kein Release, keine Datenmigration).
- Entfällt: `BackroomsLevel`, gemeinsames `Piece`, `BackroomsChunkGenerator` (→ `BackroomsStackGenerator`), Riss-Übergänge in
  `BackroomsTravel`, `EntranceShaftFeature`/`ShaftTemplate` (→ Haus), Riss/fehlerhaftes Grundgestein als Eingang (g11).
- Bleibt: alle Block-Ids (keine IdMoves), `BackroomEffect` + Schutz-Mixins (auf neue Dimension + Ebenen-Tags), `BackroomsAmbience` (Brummen;
  Rest über Biom-Attribute), Mandelwasser, Hintertür-Schlüssel.
- **Übernahme b-back20s:** `Maze`, `HouseTemplate`, Kreide + Kreidebox + Kreidezeichen, `BackroomsLoot`, Loot-Tabellen, STAIRS22-Regel und
  -Test → Kern. **Startinhalt:** `LobbyGen` → e01, `PoolsGen` → e02, `WarehouseGen` → e04, `PipesGen` → e05 (mechanisch auf `ly` + eigene
  Piece-Enums umgestellt, im Kern), die Ebenen-Branches bauen darauf um. Stil `END` entfällt (Weizenfeld neu).

## 8. Implementierungsplan
**Basis:** `b-back22-kern` = aktueller `claude-wave1` + Merge `hub/b-back20s` (nicht gebündelt, wird hier aufgenommen; Altgeneratoren
und Kreide kommen so mit). Jede Stufe einzeln abnehmbar; nach dem Bau ≥ 5 Runden (Besitzer).

| Stufe | Branch | Inhalt | Dateien (Auszug) | Tests |
|---|---|---|---|---|
| S0 | `b-back22-spike` (nicht bündeln) | Stapel-Dimension mit Worst-Case-Füllung, Mess-Befehl `/backrooms_measure` (Dev) | `spike/**` | Client-Lauf nach Messplan §3.2 → Ergebnis in den Plan, Entscheid M2 |
| K1 | `b-back22-kern` | Dimension, Biomquelle, `BackroomsStackGenerator`, `BandWriter`, `BandStack`, `Band`-API, 10 Stummel-Ebenen, 10 Stummel-Biome, `TrackingFilters` + Ebenen-Filter, Schutz/Effekt umgestellt | `backrooms/stack/**`, `backrooms/api/**`, `level/e01..e10/` (Stummel), `data/…/dimension*/backrooms_stack.json` | `bands_layout` (Grenzen, Trennsektionen voll, Biom je y), `bands_hull`, `bands_gen_budget`, `bands_tracking_filter` |
| K2 | dto. | Hilfen: `StairBuilder`/`StairRule`/`WalkCheck`, `Maze`, `EscapeCheck`, `CellGraph`, Raum-Module, `GrandStructure`, `BuildRules`, Bausteine K-R9.3, Datei-Eigentum §6 (Glob-Skripte, Sprach-Namespaces, Tags, Klänge, Adapter-Stummel) → **API-Freeze** | `backrooms/gen/**`, `tools/backrooms/gen_e*.py`, `fabric.mod.json` | `bands_stairs_walkable`, `bands_reachable`, `bands_build_rules` (je Ebene automatisch) |
| K3 | dto. | SimpleLib-Portal (Server): Tür-Basis + BE, `FrameRule`, Regeln, `PortalStates`, `PortalLinks`, `PortalTransit`, `PortalPreload`, Payloads | `simplelib/…/api/portal/**`, `simplelib/…/net/Portal*` | `portal_frame`, `portal_double_door`, `portal_two_way`, `portal_activate_once`, `portal_break_and_repair`, `portal_transform_rotations`, `portal_cooldown`, `portal_reject_far` |
| K4 | dto. | Client: Kreuzen + Vorhersage, Nebelpuls, Schleier, Kulisse, Übergangsbild beim Dimensionswechsel | `simplelib/…/client/portal/**`, Client-Mixin | `check_client_mixins.py`; Client-Lauf im Bündel (Übergang ohne Ladebildschirm, Kulisse sichtbar) |
| K5 | dto. | Tür-Netz im Stapel: `DoorNet` (+ `doorsNear`), Tür-Stempel, Kern-Tür mit `symbol 0..10` (Stummel-Symbole), Wartungskasten, Ersatzteil-Hook, `ChalkMarks` | `backrooms/door/**` | `bands_doors_paired` (symmetrisch, selber Chunk), `bands_doors_framed`, `bands_box_at_every_door` |
| K6 | dto. | Eingang/Ausgänge/Migration: Haus + Keller-Doppeltür, EXIT, alte Dimensionen weg, Startinhalt e01/e02/e04/e05 | `backrooms/entrance/**`, Daten | `entrance_house_link`, `cellar_door_roundtrip`, `exit_to_overworld`, `simplebuilding:data_integrity*`; Standalone-Ziel `module-simplelayers-standalone-fabric-263` |
| K7 | dto. (additiv, vor Welle mit E4/E7) | bewegte Plattform (Aufzug-Baustein) | `backrooms/gen/platform/**` | `bands_platform_carry` |
| S2 | `b-back22-window` (nach Abnahme S1) | Tiefenfenster | `simplelib/…/client/portal/window/**` | Client-Lauf Vanilla + Sodium, Iris-Rückfall |
| eNN | `b-back22-eNN` (je Ebene) | Ebenen-Inhalt nach Ebenen-Konzept | nur §6 rechte Spalte | `eNN_*` + Kern-Filter `bands_*` |
**Reihenfolge/Last:** S0 zuerst (entscheidet M2 und Nebelpuls); K1+K2 → API-Freeze → Ebenen in **Wellen ≤ 4 Agenten** (Regel
Ressourcen, nie zwei schwere Läufe gleichzeitig) parallel zu K3–K6; Vorschlag Welle 1 e01/e02/e03/e05, Welle 2 e04/e06/e07 (nach K7), Welle 3
e08/e09/e10. Bündel: Kern (K1–K6) zuerst, danach jede Ebene einzeln (nur eigene Dateien → konfliktfrei).

## 9. Modul-Unabhängigkeit und A12
- Kern-Code `common/src/shared` wird nicht angefasst (Regel 10). Simple Layers bindet SimpleLib auf Fabric schon heute ein; spätere
  NeoForge/Forge-Ports (erst auf Anforderung): Regel 11 prüfen, sonst Reflection.
- SimpleLib-Portal kennt keine Backrooms, keine Ebenen, keine Simple-Layers-Klasse; Grundgewölbe (eigene Mod, N21) könnte es nutzen, muss
  aber nicht. Fremde Beute (Gadgets/Lights) nur bedingt (Muster `BackroomsLoot`).
- A12: Vanilla-Nutzen = Abenteuer-Dimension mit Ziel, Deko-Blöcke, seltenste Belohnung Oberwelt-Türpaar; Eingriff in die Oberwelt nur Haus
  (abschaltbar) + gebaute Türen; keine Gegner, kein Schaden; Türen tragen nur Spieler → Netherportale/Elytra/Loren bleiben die Reisewege.
  Vanilla-Portale werden nicht verändert.

## 10. Rückfragen an den Besitzer (nur Kern)
- **k1** Raster: (a) 112 = 96 offen + 16 Grundgestein (Empfehlung, §1.2) · (b) 128 = 112 offen · (c) exakt 100 (Ebenen schimmern beim
  Rendern durch, mehr Last).
- **k2** Sichtweite sehr hoch (> 16): Bandfenster automatisch zuschalten, falls die Messung es verlangt? (a ja, Empfehlung).
- **k3** Durchsicht S2 (Tiefenfenster) Standard an, mit Shaderpack automatisch Kulisse? (a ja, Empfehlung).
Alle übrigen Kern-Entscheidungen sind oben begründet getroffen (Datei-Eigentum, Tür-Stempel, Link über BE + zentrale Zustände, kein Spike).
