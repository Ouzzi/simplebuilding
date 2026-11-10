# Konzept B-BACK22 (10.11.2026) – Backrooms neu: eine Dimension, gestapelte Schichten, immersive Türportale

NUR KONZEPT – nichts davon ist gebaut. Branch `b-back22-konzept`, Basis `hub/b-bundle172-gen` (9433d935b). Auftrag N22
(/root/b-auftrag-20261109.md), Besitzer-Wunsch vollständig in /root/sb-tools-a/wunsch-back22.md. Vorschau mit offenen Fragen:
/root/sb-tools-b/previews/b-back22-preview.html, Antwortcode **BACK22** (R1–R6), nach den Besitzer-Antworten **BACK22b** (R7–R9). Nur MC 26.3 Fabric. Erst nach der Besitzer-Antwort wird
gebaut, und nach dem Bau gibt es wieder mindestens 5 Runden.

## 0. Ist-Stand (Retrieval-First)

| Stand | Was es gibt | Was davon bleibt |
|---|---|---|
| Production (Bündel 171, b-back18) | 5 eigene Dimensionen je 64 hoch (`backrooms`, `_warehouse`, `_pipes`, `_pools`, `_end`), reine MC-freie Generatoren `world/gen/*` (Block-Codes → `BackroomsBlocks`), Fluchtbaum, Risse/Wartungstüren/Luke als Übergänge (Dimensionswechsel mit Ladebildschirm), Effekt `simplelayers:backroom` + Mixins (Abbau-/Kolbenschutz), Schacht-Eingang + fehlerhaftes Grundgestein, `BackroomsAmbience`, `BackroomsEntries` | Generator-Prinzip (rein, testbar, Bilder aus demselben Code), Effekt + Schutz-Mixins, Ambience, Blöcke (Tapeten, Teppich, Decke, Neon, EXIT, Türen, Warnband/-schild), Mandelwasser, Schlüssel |
| b-back20 (Zwischenstand c905d030c…9de13b41e, nicht gebündelt) | Region-Irrgärten (`Maze`, Spannbaum = Erreichbarkeit beweisbar), Höhen in Zellen (abgehängte Decken, Podeste, Lichthöfe, Zwischengeschoss, Kesselräume, Galerie-Hallen, Terrassenbecken), Räume mit Absicht (Pausenraum, Sitzecke, Archiv-Flügel), **Kreide (4 Farben) + Kreidebox**, **Haus mit zubetoniertem Keller** als Eingang, bedingte Beute (Gadgets/Lights) | Kreide + Kreidebox (unverändert übernehmen), Haus-Keller-Eingang, `Maze` als Baustein, Raum-Ideen (Pausenraum, Archiv), Kesselraum/Galerie/Terrassenbecken als Raumtypen |
| b-under21 (Zwischenstand, eigene Dimension „Grundgewölbe“) | Dimension 128 hoch, Region-Komplex 3×3 Felder auf 2 Ebenen (y 20–55), Bedrock-Hülle per Konstruktion („Verkleidung + Kern“), 3 Rätsel → Altar → Aufstiegsstrom, Tor schließt nach 30 s, Effekt `vault_bound` | Hülle-Regel (fest neben offen = Verkleidung, sonst Grundgestein) für ALLE neuen Schichten; Komplex passt in eine 96er-Schicht (Frage f7) |
| b-layers13 | Bedrock-Hülle bis Bauhöhe, Effekt statt Dimensionsprüfung | bleibt die Schutzregel |
| Simple Layers allgemein | „Schichtbiom“ = je Schicht eigene Dimension mit festem Biom (`fixed`) | wird hier ersetzt: Biom **nach Höhe** in einer Dimension |

**Befund Treppen-Bug (Live):** `LobbyGen.stairPiece` öffnet die Decke über dem Treppenlauf erst ab `lx ≥ 4` (y 7–9). Die Stufe bei
`lx = 4` liegt auf y 5, der Spieler (1,8 hoch, 0,6 breit) steht beim Hochsteigen noch halb über `lx = 3` und stößt mit dem Kopf an die
Deckenplatte y 7 der Spalte `lx = 3`. Ursache ist also nicht die Stufe, sondern die fehlende „Übergangsspalte“. Kleiner Live-Fix
(Öffnung ab `lx ≥ 3`) gehört zum Punkt „Sofort“ (b-fix172 o. eigener Branch), nicht in dieses Konzept; die allgemeine Regel steht in §R3.

**Recherche (nur Ideen, kein Code übernommen):**
- *Immersive Portals* (qouteall, Apache-2.0): Durchsicht per **Stencil-Buffer**, Portale rekursiv von außen nach innen; Ausweichmodus
  rendert das Ziel in einen eigenen Framebuffer (ohne Portal-im-Portal). Sichtbarkeit per Occlusion-Query mit Vorhersage,
  Vorderschnitt per `gl_ClipDistance`. Die Mod **ersetzt die festen Chunk-Arrays des Clients durch Maps**, damit ferne Chunks geladen
  sein können, und lädt Chunks hinter sichtbaren Portalen schrittweise nah→fern. Sie hebt die Regel „ein Client = eine Welt“ auf
  (umgeleitete Pakete mit Dimensions-Info). Teleport passiert **clientseitig vor jedem Frame**, sobald die Kamera die Portalebene
  kreuzt; der Server prüft nach. Sodium/Iris nur über „ad-hoc hacking which is fragile“, Versionen fest verriegelt; NeoForge-Ports
  melden Iris als „nicht voll funktionsfähig“. Lehre: der teure Teil ist **Mehrwelt + ferne Chunks**, nicht der Teleport.
- *Liminal Industries* (Appocryptha, Forge 1.20.1): endlos wechselnde Backrooms aus handgebauten Räumen (eigener Raum-Baukasten „The
  Room Forge“), Möbel/Haushalt als Rohstoff, Fabrik + Tech-Baum in Kapiteln zum Entkommen, Zug-Netz, starke Sculk-Gegner, Ebenen wie
  Poolrooms und endlose Bibliothek; Fork „Rescripted“ ergänzt Manilla-Räume, Wegsteine, Spinde. Übernommen als Idee: Räume mit
  Absicht, Haushalt = Rohstoff, schwerere Ebenen = bessere Funde, Bibliothek als Ebene. Nicht: Fabrikzwang, Gegner, Zug-Netz (A12).
- *Fabric 26.x:* `WorldRenderEvents` heißt seit 26.1 **`LevelRenderEvents`** (Paket `rendering/v1/level`, `END_EXTRACTION` u. a.,
  Kontext mit `poseStack`, `submitNodeCollector`). Es gibt **keine** Fabric-API für Portal-/Zweitkamera-Rendering. Vanilla rendert seit
  1.21.x über Frame-Graph + Extraktion/Submit; ein zweiter Level-Durchlauf braucht tiefe Mixins in `LevelRenderer`. Sodium ersetzt das
  Terrain-Rendering vollständig.
- *Chunk-Tickets:* `ServerLevel.setChunkForced` (im Repo genutzt: `ChunkHolds`, Claims-Tests) und zeitbegrenzte Tickets (Vanilla seit
  1.21.2 z. B. für Enderperlen) halten Chunks serverseitig geladen. Sie schicken Chunks aber **nicht** an einen Client außerhalb seiner
  Sichtweite – das kann nur ein Eingriff in das Chunk-Tracking (genau das, was Immersive Portals umbauen musste).

---

## Revisionsverlauf (je: prüfen → bewerten → neu)

Bewertungsachsen: **SG** Spielgefühl · **MB** Machbarkeit 26.3 Fabric · **PF** Performance · **RR** Risiko Sodium/Iris/Render-Pipeline ·
**MU** Modul-Unabhängigkeit · **A12** Vanilla-Nutzen/Eingriff. Skala 1–5 (5 = sehr gut / sehr geringes Risiko).

### R1 – Wörtliche Umsetzung
**Entwurf:** Dimension `simplelayers:backrooms`, `min_y −208`, Höhe 512; fünf Schichten exakt nach Besitzer-Zahlen (y 300–200 Lobby,
200–100 Poolrooms, 100–0 Archive, 0–−100, −100–−200), dazwischen 3 Lagen Grundgestein. Türportale wie Immersive Portals: Stencil-
Durchsicht, Türen führen an beliebig ferne Orte; generierte Türziele zufällig. Items: je Schicht 2–3. Räume aus den Back20-Generatoren.
**Prüfung:**
- 100er-Schichten liegen quer zu den 16er-Sektionen. Die Sichtbarkeits-Flutung von Vanilla (`VisGraph`, je Sektion: welche Seiten sind
  über Luft verbunden) und Sodiums Graph kommen durch eine Sektion, die nur 3 Lagen Bedrock **innerhalb** enthält, über die Seitenflächen
  hindurch (oben↔Nord↔unten). Folge: Nachbarschichten werden teils mitgezeichnet → das Render-Budget-Ziel „nur aktuelle Schicht“ ist
  nicht gesichert.
- Ferne Türziele: der Client hat die Chunks dort nicht. Durchsicht wäre nur mit Umbau des Chunk-Trackings möglich (Immersive-Portals-
  Größe), Teleport hätte sichtbares Nachladen.
- Stencil-Durchsicht: braucht einen zweiten Level-Render-Durchlauf. Mit Sodium (eigener Terrain-Renderer) und Iris (eigene Pipelines)
  nur über versionsgebundene Eingriffe – genau das, was Immersive Portals als „fragil“ beschreibt.
- Räume/Treppen: unverändert „zerklüftet“ (Besitzer-Kritik), Kopffreiheit nicht geregelt.
**Bewertung:** SG 3 · MB 2 · PF 2 · RR 1 · MU 4 · A12 3. **Verworfen** als Ganzes; brauchbar: eine Dimension, Reihenfolge der Schichten.

### R2 – Sektionsraster + „Ziel liegt im selben Chunk-Stapel“
**Neu:**
1. **Schichtraster 112 = 96 offen + 16 Grundgestein**, an Sektionsgrenzen ausgerichtet. Die Trennlage ist **eine ganze Sektion** aus
   Grundgestein: kein Sichtweg, Vanilla-/Sodium-Kulling trennt die Schichten zuverlässig. Bezeichnung bleibt „≈ 300–200, 200–100 …“.
   Konkrete Werte (5 Schichten): Deckel 304–319, S1 208–303, Trenn 192–207, S2 96–191, Trenn 80–95, S3 −16–79, Trenn −32–−17,
   S4 −128–−33, Trenn −144–−129, S5 −240–−145, Boden −256–−241 → `min_y −256`, Höhe 576. Mit 6. Schicht: `min_y −368`, Höhe 688.
2. **Kernidee Türziele:** Ein Chunk ist eine **Säule über alle Schichten**. Liegt das Ziel einer Tür in einer anderen Schicht, aber
   waagrecht nahe (≤ 16 Blöcke Versatz, also eigener oder Nachbar-Chunk), dann hat **der Client die Zielblöcke schon** und der Server hat
   sie schon erzeugt – ohne Ticket, ohne Mehrwelt, ohne Ladebildschirm. „Andere Location“ entsteht über die Schicht (andere Höhe,
   anderes Thema), nicht über Entfernung. Teleport innerhalb einer Dimension zeigt in Vanilla ohnehin keinen Ladebildschirm.
3. Durchsicht weiter per Stencil (noch nicht gelöst).
**Prüfung:** Render-Budget jetzt gesichert (ganze Bedrock-Sektion). Erzeugung: jede Chunk-Säule erzeugt alle Schichten (5 × 0,1–0,5 ms
≈ 1–2,5 ms je Säule, gemessen an back18/20-Werten) – vertretbar; Bedrock-/Füllsektionen sind einwertige Paletten (fast kein Speicher).
Ob Vanilla Sektionen außerhalb des Sichtgraphen trotzdem vernetzt (Mesh-Bau-Kosten), ist **nicht** sicher → Messpunkt im Spike S0.
Offen: Durchsicht-Risiko unverändert; Räume/Treppen/Items noch ungeklärt; 16-Block-Versatz klingt nach „gleicher Ort“.
**Bewertung:** SG 3 · MB 4 · PF 4 · RR 2 · MU 4 · A12 3.

### R3 – Bauqualität, Höhe innen, Schicht-Biome, Durchsicht als „Tiefenfenster“
**Neu:**
1. **Biom nach Höhe:** eigene Biomquelle `simplelayers:layer_stack` (Biom je y-Band). 26.3-Biome tragen Umgebungs-Attribute
   (`minecraft:visual/fog_color` usw. – schon heute in den Backrooms-Biomen genutzt) → je Schicht eigene Nebelfarbe, Stimmung, Klang.
   Prüfpunkt S0: ob auch Nebel-Start/-Ende je Biom wirken (sonst Dimensionswert für alle, Farbe je Schicht).
2. **Bauraster statt Zellen-Zufall:** jede Schicht = Raster aus **16×16-Bauplätzen**, je Bauplatz ein Raum-Modul (Raumtyp mit
   festem Grundriss + Varianten), Geschosshöhe je Schicht fest (z. B. Lobby 8 = Boden 1 + 6 Luft + Decke 1). Module schließen an
   genormten Anschlussstellen (Türmitte, Gangbreite 2 oder 3), dadurch keine abgeschnittenen Wände/Teile. Ungenutztes Volumen ist
   **massive Füllung** (Verkleidung + Bedrock-Kern nach der Under21-Hülle-Regel), nie offene Leere.
3. **Höhe innen (96 Blöcke):** je Schicht 3–6 Geschosse **plus** Großräume über mehrere Geschosse (Atrien, Hallen, Schächte).
   Wege hoch/runter innerhalb einer Schicht: Treppenhäuser, Rampen, Leitern, Galerien.
4. **Treppen-Regel (Kopffreiheit, verbindlich für jeden Generator):** Treppen sind immer **durchgehende Läufe** aus Treppenblöcken
   gleicher Ausrichtung (Steigung 1:1), Podest spätestens alle 8 Stufen, offene Seite mit Geländer, Unterseite gefüllt (Stufen oder
   Füllblock, kein Loch). Lichte Höhe: über jeder Stufe **und über der vorigen Spalte** mindestens 3 Blöcke Luft (Stufe y → Luft y+1…y+3
   in Spalte i und i−1, beim Abwärtslauf i+1). Deckenöffnungen beginnen eine Spalte vor der ersten kritischen Stufe. Prüfung im
   Gametest per Kollisionsbox 0,6 × 1,8 entlang jedes Laufs (rein, aus dem Generator).
5. **Bauregel-Prüfer** (reiner Test über viele Bauplätze): keine schwebenden Teile, jeder Raum erreichbar, keine Treppe ohne
   Anschluss, jede Tür mit vollständigem Rahmen, keine sichtbare Grundgestein-Fläche in Räumen.
6. **Große Bauwerke:** je Schicht-Region 256×256 genau ein Großbauwerk (nimmt mehrere Bauplätze ein): Poolrooms „Großes Becken“
   (z. B. 96×64, 40 hoch, Sprungtürme, Tiefbecken), Archive „Rotunde“ (Ø 48, 6 Galerien, 60 hoch), Lobby „Säulensaal“ (64×64, 24 hoch),
   Kesselwerk „Kesselhalle“, Flure „Großes Treppenhaus“ über alle 96 Blöcke.
7. **Durchsicht neu: „Tiefenfenster“ statt Stencil/Zweitkamera.** Weil das Ziel im selben Chunk-Stapel liegt, kann der Client die
   Zielblöcke direkt lesen. Ein Tür-Renderer baut aus dem Zielbereich (z. B. 9 breit × 6 hoch × 16 tief) ein kleines Block-Mesh
   (zwischengespeichert, neu bei Blockänderung) und zeichnet es **versetzt hinter die Türebene**. Ablauf je Frame (eigener Schritt über
   `LevelRenderEvents` nach dem festen Terrain): (a) Türöffnung zeichnen mit Nebelfarbe und Tiefe = „ganz fern“ (löscht die Tiefe nur in der
   Öffnung), (b) Ziel-Mesh mit normalem Tiefentest zeichnen. Außerhalb der Öffnung verdeckt die echte Wand das Mesh (Tiefentest), in der
   Öffnung sieht man das Ziel. Kein Stencil, kein zweiter Level-Durchlauf, keine zweite Welt; die Tiefe des Fensters endet im Nebel, der
   in den Backrooms ohnehin dicht ist (passt zur Stimmung). Grenzen: keine Wesen auf der Gegenseite (später optional), transparente
   Blöcke/Wasser im Ziel brauchen eigene Reihenfolge, Licht = gespeichertes Ziel-Licht.
**Prüfung:** Bauraster + Treppen-Regel treffen die Kritik „zerklüftet/Kopf stoßen“ direkt und sind testbar. Tiefenfenster: kein
Sodium-Konflikt beim Terrain (eigenes Mesh, eigene Zeichnung), aber eigene Render-Pipeline mit Tiefen-Trick → mit Iris-Shaderpacks
(verzögerte Beleuchtung, Schattenpass) wahrscheinlich falsche Optik → braucht Abschaltung bei aktivem Shaderpack (Iris-Erkennung per
Reflection). Offen: Türnetz (welche Tür wohin, zurück, Symbole), Items, Oberwelt, Migration.
**Bewertung:** SG 4 · MB 3,5 · PF 4 · RR 3 · MU 4 · A12 3,5.

### R4 – Türnetz, Symbole, Items, Oberwelt-Türen
**Neu:**
1. **Portal-Tür (Regeln):** Tür 1 breit × 2 hoch, **Rahmen links, rechts und oben aus festen Vollblöcken** (oben = 1 Sturz über der
   Tür). Fehlt ein Rahmenblock, ist sie eine normale, tote Tür: Symbol erlischt, Durchgehen teleportiert nicht, kurzer Klang + Hinweis in
   der Aktionsleiste (A14). Rahmen wird bei Blockänderung neu geprüft. **Doppeltür** = 2 breit, Rahmen links/rechts außen + 2 Sturzblöcke,
   **ein** Portal über beide Flügel, Ziel ist ebenfalls eine Doppeltür.
2. **Zurück durch dieselbe Tür:** generierte Türen entstehen **paarweise** aus Seed + Bauplatz (reine Funktion: Paar-Id, Ziel-Schicht,
   Versatz ≤ 16, Drehung in 90°-Schritten). Beide Enden werden im selben Chunk-Stapel erzeugt → kein Speicher nötig, immer stabil.
   Geht eine Tür kaputt (Rahmen fehlt oder Tür abgebaut, wo erlaubt), ist das Paar in beide Richtungen tot, bis der Rahmen wieder
   vollständig ist; Zustand im Blockzustand + kleiner SavedData-Liste „zerstörte Paare“ (nur für Abbau außerhalb des Schutzes).
3. **Symbole je Ziel:** Ziel-Schicht steht als Symbol im oberen Türblatt + Farbe des Rahmenbands: Lobby = gelbes Quadrat, Poolrooms =
   Welle (türkis), Archive = Buch/Karteireiter (rotbraun), Kesselwerk = Ventilrad (kupfer), Flure = Schlüssel/Zimmernummer (weinrot),
   Grundgewölbe = Siegel (blassblau), Ausgang = Pfeil nach oben (grün). Türen innerhalb derselben Schicht (Abkürzung) tragen das eigene
   Symbol + einen Punkt. Rückseite einer Tür zeigt das Symbol ihres eigenen Ziels → wer sich die Symbole merkt, kennt den Rückweg.
4. **Wege zwischen den Schichten:** Portal-Türen (Hauptweg, je Bauplatz ~3–5 % eine Tür), seltene **Großer-Schacht**-Bauwerke
   (echter senkrechter Schacht durch die Trennlage mit Treppe, nur alle ~1024 Blöcke, Wahrzeichen; öffnet lokal den Sichtweg) und die
   alten Risse (Noclip, Einweg nach unten, sehr selten). Ausgänge zur Oberwelt: EXIT-Türen in Lobby und Poolrooms (Dimensionswechsel,
   Ladebildschirm akzeptiert – nur raus), Altar im Grundgewölbe (falls Schicht 6).
5. **Items je Schicht** (Liste §5): oben Grundlegendes, tief Einzigartiges; jede Schicht liefert eine Zutat der Oberwelt-Tür.
6. **Oberwelt-Türen (S3):** Schwellentür-Paar aus Zutaten aller Schichten + **Schwellenkern** (nur im tiefsten Großbauwerk, 1 je
   Bauwerk). Ein Kern = **ein** verknüpftes Paar.
**Prüfung:** Paar-Generierung im selben Stapel ist stabil und braucht keinen Speicher – gut. Oberwelt-Türen: in der Oberwelt liegen die
Ziele weit weg (sonst nutzlos) → Client hat die Zielchunks **nicht** → Tiefenfenster geht dort nur, wenn das Ziel in Sichtweite liegt;
Teleport ohne Ladebildschirm geht (gleiche Dimension), aber mit kurzem Nachladen. A12: unbegrenzte Reichweite + Tiere/Items würde
Netherportal-Netze und Lore/Elytra-Reisen entwerten → Grenzen nötig. Items: 6 × 4–5 neue Items ist viel Textur-Arbeit (A8c-Runden).
**Bewertung:** SG 4,5 · MB 3,5 · PF 4 · RR 3 · MU 3,5 (SimpleLib-Schnitt noch nicht klar) · A12 3.

### R5 – Stufenplan, SimpleLib-Schnitt, A12-Grenzen, Migration
**Neu:**
1. **Stufen** (jede einzeln abnehmbar, Durchsicht ist NICHT Voraussetzung für das Spiel):
   - **S0 Spike (klein, ½–1 Agententag, kein Merge):** Dimension 576 hoch mit 5 Bänder-Biomen + Platzhalter-Räumen; messen:
     Kulling-Trennung (Draw-Calls/Sektionen sichtbar je Schicht, Vanilla und Sodium), Mesh-Baukosten, Erzeugungszeit je Säule, Speicher,
     Nebel je Biom, Teleport-Gefühl ohne Ladebildschirm. Ergebnis entscheidet Rasterwerte und S2-Weg.
   - **S1 Nahtloser Übergang (M, Risiko niedrig):** Portal-Tür, Rahmenprüfung, Paar-Logik, Doppeltür, Symbole; Übergang beim Kreuzen der
     Türebene: Client versetzt sich **im selben Frame** (eigenes Paket an den Server), Server prüft (Abstand zur Tür, Paar gültig) und
     bestätigt per relativem Teleport mit Drehung; Geschwindigkeit wird mitgedreht. Vorladen: Ziel liegt im selben Stapel (nichts zu tun);
     für Ziele außerhalb der Sichtweite (S3) ein zeitbegrenztes Ticket (Radius 2) ab 24 Blöcken Annäherung. Vor dem Kreuzen fordert der
     Client die Neuvernetzung der Zielsektionen an, damit nach dem Versatz kein Loch zu sehen ist. Ohne Render-Eingriff → kein
     Sodium/Iris-Risiko. Bis S2 zeigt die Öffnung einen **Schleier** (ruhig wabernde Nebelfläche in Schicht-Farbe), kein schwarzes Loch.
   - **S2 Durchsicht „Tiefenfenster“ (L, Risiko mittel):** wie R3.7. Schalter `portals.see_through` (Standard an), automatisch aus bei
     aktivem Iris-Shaderpack (→ Schleier), Reichweite 16 Blöcke, nur Türen im Blickfeld, Mesh-Cache je Tür. Sodium: getestet mit der im
     Pack üblichen Version; schlägt etwas fehl → Schleier statt Absturz (Schutz per try/catch beim Zeichnen).
   - **S3 Oberwelt-Türen (M, Risiko niedrig–mittel):** Schwellentür-Paar, nur **gleiche Dimension** (Vorschlag), Reichweite höchstens
     **1024 Blöcke**, nur Spieler (keine Tiere, keine Items, keine Loren → kein Logistik-Netz), Durchsicht nur wenn das Ziel in der
     Sichtweite des Spielers liegt, sonst Schleier + Teleport mit kurzem Nachladen. Zerstörte Tür = Paar tot; Kern geht nicht verloren
     (Abbau gibt die Tür mit Kern zurück).
   - **Inhalt** in Schicht-Stufen: I1 Lobby (inkl. Treppen-Regel, Bauraster, Eingang), I2 Poolrooms (Großes Becken), I3 Archive,
     I4 Kesselwerk, I5 Flure, I6 Grundgewölbe-Einbindung (falls gewählt). Jede Stufe mit ≥ 5 Runden nach dem Bau.
2. **SimpleLib-Schnitt (MU):** `com.simplelib.api.portal` (nur API + Logik, keine Backrooms-Inhalte): `PortalDoor` (Position, Breite 1/2,
   Ausrichtung, Ziel, Paar-Id), `PortalFrameRule` (links/rechts/oben fest; austauschbar), `PortalLinks` (Paar-Auflösung: rein per Funktion
   ODER gespeichert für gebaute Paare), `PortalTransit` (Kreuzen, Versatz + Drehung, Server-Prüfung, Paket), `PortalPreload` (Ticket-Art),
   Client `PortalView` (Schleier / Tiefenfenster). Simple Layers liefert nur seine Türen (Symbole, Paar-Funktion aus Seed). Simple Layers
   bindet SimpleLib schon heute auf Fabric mit ein (`include project(':modules:simplelib:fabric')`) → kein neuer Zwang. Kern
   (`common/src/shared`) wird nicht angefasst. Für spätere NeoForge/Forge-Ports: dort prüfen, ob SimpleLib im Modul-Klassenpfad liegt
   (Regel 11), sonst Reflection.
3. **A12:** (1) Nutzen: Abenteuer-Dimension mit Ziel, Deko-Blöcke je Schicht, Kreide, und als seltenste Belohnung ein **einziges**
   Oberwelt-Türpaar je großer Expedition. (2) Eingriff Oberwelt: nur seltene Eingänge (Haus, fehlerhaftes Grundgestein) und gebaute
   Schwellentüren. (3) Kein Entwerten: Reichweite ≤ 1024, nur Spieler, ein Paar je Kern, Kern nur tief unten → Netherportale bleiben
   die Reise-Infrastruktur, Schwellentür ist „mein Weg nach Hause von der Basis zur Mine“. Keine Gegner, kein Schaden (Liminal ohne
   Horror-Zwang).
4. **Migration:** noch kein öffentlicher Release → die fünf alten Dimensionen werden **ersetzt**. Vorschlag: neue Dimension unter
   neuer Id `simplelayers:backrooms_stack` (alte Chunk-Dateien mit 64er-Höhe würden in der neuen Höhe Nähte erzeugen); alte
   Dimension-JSONs entfallen. Spieler, die in einer alten Dimension ausgeloggt haben, setzt ein Login-Haken an ihren Respawn (Vanilla
   fällt sonst auf den Welt-Spawn zurück). Block-Ids bleiben (Tapeten, Teppich, Neon, Türen …) → keine IdMoves. Effekt, Schutz-Mixins,
   Ambience, Eingänge zeigen auf die neue Dimension. Config `backrooms.*` bleibt, neue Schlüssel `portals.*` in SimpleLib.
5. **Verhältnis Grundgewölbe (N21):** Empfehlung: **unterste Schicht (S6) im Stapel**. Der Under21-Komplex (y 20–55, 36 hoch) passt in
   eine 96er-Schicht, die Hülle-Regel ist dieselbe, Eintritt über die Bedrock-Lücke der Oberwelt landet unten, der Altar führt hinaus.
   Das Gewölbe behält sein Rätsel-Gefühl (eigenes Biom, eigener Effekt). Eine Dimension weniger (Besitzer: „weniger Dimensions-Teleport“).
   Alternative: bleibt eigene Dimension (b-under21 unverändert), dann nur eine Siegel-Tür aus dem Stapel dorthin.
**Prüfung:** Stufen trennen das Risiko sauber (S1 liefert schon „kein Ladebildschirm + Rückweg + Symbole“). Schwachstellen: (a) S1-
Client-Versatz braucht Server-Bestätigung – bei hoher Latenz kurzer Rückzieher möglich (Server-Prüfung großzügig, wie Immersive
Portals); (b) Mesh-Vorbau der Zielsektionen hängt vom Vanilla-/Sodium-Verhalten ab (S0 misst); (c) 16-Block-Versatz zu eng für
„andere Location“-Gefühl in derselben Schicht → Abkürzungstüren derselben Schicht mit Versatz bis 32 (Sichtweite min. 2 Chunks + 1
Rand reicht knapp; sonst Schleier).
**Bewertung:** SG 4,5 · MB 4 · PF 4 · RR 4 (S1) / 3 (S2) · MU 4,5 · A12 4,5.

### R6 – Gegenprüfung gegen den Besitzer-Wunsch (Punkt für Punkt) + Korrekturen
| Wunsch (wunsch-back22.md) | Wo erfüllt | Korrektur in R6 |
|---|---|---|
| unpolished, zu wenige Items | Bauraster, Bauregel-Prüfer, Items §5 (27 neue + übernommene) | Items nach Tiefe gestaffelt, jede Schicht ≥ 4 eigene |
| Treppen zerklüftet, Kopf stößt | Treppen-Regel R3.4 + Kollisionstest | Regel gilt auch für Leitern (2 Luft über Ausstieg) und Rampen |
| Räume immer sauber gebaut | Module mit Anschlussstellen, Füllung statt Leere | Großbauwerke als feste Bauplan-Teile (handgebaut wirkende Vorlagen, generiert aus Code, testbar) |
| eine Dimension, 100er-Schichten, Bedrock dazwischen | R2 (Raster 112 ≈ 100, Frage f3) | Besitzer kann exakt-100 wählen (mit Kulling-Nachteil, ehrlich) |
| viel Höhe, unendlich in 4 Richtungen | 96 offen, Geschosse + Großräume, Raster endlos | – |
| ≥ 5 Schichten | 5 + optional Grundgewölbe als 6. | – |
| nicht alle Schichten rendern | ganze Bedrock-Sektion trennt (R2) | S0 misst es wirklich, bevor gebaut wird |
| große Bauwerke, riesige Pools | Großbauwerk je Region (R3.6) | – |
| Durchsicht + sofort drüben | S1 sofort drüben, S2 Durchsicht | ehrlich: S2 mit Shaderpack = Schleier |
| gleiche Dimension | ja (R2) | – |
| Gegenseite gerendert/generiert wie Chunkloader, auch zurück | gleicher Stapel = schon geladen; S3 per Ticket | Ticket auch an der Rückseite, solange der Spieler ≤ 24 Blöcke an der Zieltür steht |
| zurück durch dieselbe Tür, außer kaputt | Paar-Logik R4.2 | – |
| Rahmen links/rechts/oben | R4.1 | Rahmen-Blöcke: jeder feste Vollblock (auch Bedrock/Tapete) |
| Rückweg merken | Symbole beidseitig, stabile Paare | zusätzlich Item „Wegbuch“ (Archive) als Hilfe, nicht Pflicht |
| Doppeltür = Doppelportal | R4.1 | – |
| zwei verschiedene Türen verbinden, System in SimpleLib, erst nur Backrooms | R5.2, S1/S2 nur Backrooms-Türen, S3 später | – |
| Türen unterscheiden sich, Symbol | R4.3 + Entwürfe in der Vorschau | – |
| offen a/b/c Verknüpfung | Frage f1 | Empfehlung (a) + seltene, markierte Einwegtüren (d) |
| Custom Items je Schicht, oben einfach, tief einzigartig | §5 | – |
| Oberwelt-Türen extrem schwer, wenige, kein Easy-Travel | S3 + A12-Grenzen | – |
| ≥ 5 Revisionen, Eckpunkte, Fragen | R1–R6, Vorschau | – |
**Bewertung R6 (Endstand):** SG 4,5 · MB 4 · PF 4 · RR 4/3 · MU 4,5 · A12 4,5.

### R7 – Besitzer-Antworten 10.11. ~12:00 eingearbeitet (/root/sb-tools-a/wunsch-back22.md, verbindlich)
| # | Besitzer-Antwort | Meine Empfehlung vorher | Folge im Konzept |
|---|---|---|---|
| 1 | Ebene 1 zweiwegig; ab Ebene 2 Portal einmal **aktivieren**, dann dauerhaft; ab Ebene 3 **bricht** das Portal beim Durchgehen, jedes Mal reparieren | f1 d (zweiwegig + seltene Einwegtüren) | **Abweichung:** drei Tür-Regeln nach Ebene statt Einwegtüren. Neu: Ersatzteile je Ebene, Wartungskasten an jeder Tür (kein Festsitzen), siehe §4 |
| 2 | **10 Ebenen** aus SCP-/Backrooms-Lore, A listet, B darf begründet ändern | 5 Schichten | Abweichung: 10 Ebenen, Raster neu gerechnet (R8), Lore nur als Idee (eigene Namen/Texte/Assets) |
| 3 | gecraftete Portaltüren erst **nur in der Oberwelt**; Eingang = **Doppeltür mit immersivem Portal**, kein Riss mehr | f6 a (gleiche Dimension ≤ 1024) | S3 nur Oberwelt (beide Enden dort). Risse und fehlerhaftes Grundgestein als Eingang entfallen. Die Keller-Doppeltür führt in eine **andere Dimension**: ehrliche Lösung in R8.4 |
| 4 | Eingang im Keller des Vanilla-artigen Gebäudes: ja (mit Doppeltür) | f4 a | Haus aus b-back20 bleibt; statt Betonpfropfen + Schacht eine versiegelte Doppeltür hinter dem Absperrband |
| 5 | **Grundgewölbe = eigene, unabhängige Mod** | f7 a (Schicht 6) | **Abweichung:** Grundgewölbe ist nicht Teil des Stapels; keine Tür dorthin, keine gemeinsame Dimension. Gemeinsam nur über SimpleLib (Hülle-Regel/Portal-API falls nötig); Umzug macht N21-Nachtrag |
| 6 | bestehende Backrooms-Dimensionen ersetzen, **ohne Welt-Umzug** | f5 a (+ Login-Haken) | ersetzen, neue Id `simplelayers:backrooms_stack`; **kein** Login-Haken (Vanilla-Rückfall genügt), keine Datenmigration |
**Bewertung R7:** SG 4,5 (Ebenen-Regeln geben echten Fortschritt) · MB 4 · PF 3,5 (10 Ebenen, siehe R8) · RR 4/3 · MU 5 (Grundgewölbe raus) · A12 4,5.

### R8 – 10 Ebenen, Höhen-/Raster-Rechnung, Tür-Regeln, Eingangs-Doppeltür
1. **Reihenfolge (begründete Änderung an A-Vorschlag):** Poolrooms und Archive rücken auf Ebene 2 und 3, A-Reihenfolge sonst gleich:
   Lobby · Poolrooms · Archive · Lagerhalle · Rohrgänge · Elektrostation · Hotel · Lichtlos · Vorstadt bei Nacht · Weizenfeld.
   Gründe: (a) der Besitzer nannte im Wunsch selbst „300–200 Lobby, 200–100 Poolrooms, darunter Archive“; (b) das stärkste Schaubild
   (riesige Becken) kommt früh, solange Türen noch harmlos sind; (c) die Archive liefern **Akten mit Wegkarten** genau auf der ersten Ebene,
   ab der Türen brechen (Ebene 3) – Werkzeug kommt vor der Härte. A-Reihenfolge bleibt als Wahl (Frage g1).
2. **Raster für 10 Ebenen:** 112 = 96 offen + 16 Grundgestein (eine ganze Sektion). Deckel 304–319, Ebene i offen von 303−112(i−1)
   abwärts 96 Blöcke, Boden −816…−801 → `min_y −816`, Höhe **1136** (71 Sektionen). Prüfung: Vielfache von 16 ✓, Höhe ≤ 4064 ✓,
   `min_y` ≥ −2032 ✓. Render-Trennung: jede Ebene ist oben/unten von einer vollen Bedrock-Sektion eingeschlossen → Sicht-Flutung kommt nicht
   in die Nachbarebene (Vanilla `VisGraph`, Sodium-Graph gleiches Prinzip); gezeichnet wird nur die eigene Ebene.
   **Kosten ehrlich:** Der Client hält jede Chunk-Säule **ganz** im Speicher. Volle Bedrock-/Füllsektionen sind einwertig (fast nichts), aber
   etwa 4–6 gemischte Sektionen je Ebene → ~40–60 gemischte Sektionen je Säule, ungefähr **2–2,5× Oberwelt-Speicher je Chunk** (Oberwelt: 24
   Sektionen). Erzeugung ~10 × 0,1–0,5 ms ≈ 1–5 ms je Säule (+ Großbauwerke). Mesh-Bau nur für die sichtbare Ebene (zu messen). Beides misst S0;
   Ausweg falls zu teuer: **gemischtes Raster** (enge Ebenen Rohrgänge/Lichtlos/Hotel 64 = 48 + 16, weite Ebenen 112) → Höhe 944 (Frage g2).
   Exakt-100er-Raster bleibt möglich, trennt aber das Rendern nicht sicher.
3. **Tür-Regeln (Besitzer 1) – Auslegung:** Türpaare verbinden Ebene n mit n+1 (plus Abkürzungen innerhalb einer Ebene). Die Regel einer Tür
   ist die der **oberen** Ebene des Paars: 1↔2 zweiwegig, 2↔3 einmal aktivieren, ab 3↔4 bricht. Innerhalb einer Ebene gilt deren Regel.
   (Alternative „untere Ebene zählt“ → 2↔3 bräche schon: Frage g3.)
   - *Aktivieren (Ebene 2):* Tür dunkel, Symbol aus. Ein **Ersatzteil** der Ebene einsetzen (Rechtsklick) → Symbol leuchtet, Paar dauerhaft offen
     (beide Seiten). Aktionsleiste nennt das benötigte Teil (A14).
   - *Brechen (ab Ebene 3):* beim Durchgehen springt das Paar in „kaputt“ (Klang, Funken, Symbol erlischt), auf beiden Seiten. Reparatur mit
     dem Ersatzteil **der Ebene, in der man gerade steht** (beide Seiten nehmen ihr eigenes Teil an) → nie das „falsche“ Teil nötig.
   - *Kein Festsitzen:* neben jedem Türende ein **Wartungskasten** mit genau einem Ersatzteil (nicht nachfüllend) → der erste Rückweg ist immer
     möglich; danach Ersatzteile aus der Beute der Ebene (häufig). Wer sich den Weg merkt (Symbole) und Teile sammelt, kommt zurück.
   - Doppeltür = ein Portal, eine Regel, ein Teil.
4. **Eingangs-Doppeltür im Keller (Oberwelt → Lobby) – Dimensionswechsel:** Die Backrooms bleiben eine eigene Dimension; ein Wechsel lädt die
   andere Welt auf dem Client neu (Vanilla zeigt kurz einen Ladebildschirm). Echte Durchsicht über Dimensionen geht nur mit Immersive-Portals-
   Umbau (nicht machbar). Lösung: (a) **Kulissen-Durchsicht**: die Türöffnung zeigt ein gerechnetes Bild der Lobby mit Parallaxe (Bild verschiebt
   sich mit dem Blickwinkel, wirkt räumlich), da die Lobby überall gleich aussieht, glaubhaft; (b) der Ladebildschirm wird durch einen **gelben
   Nebel-Übergang** ersetzt (Bildschirm füllt sich mit Lobby-Nebel, Brummen setzt ein) → wirkt wie Hineingehen. Rückweg durch dieselbe Doppeltür
   (Ebene-1-Regel, zweiwegig), auf der Lobby-Seite gleiche Kulisse mit Kellerbild. Risiko niedrig–mittel (Client-Bildschirm-Ersatz per Mixin).
5. **Himmel in Ebene 9/10:** keine echte Himmelsbox in einer geschlossenen Ebene. Trick: Decke ~90 Blöcke über dem Boden liegt **jenseits des
   Nebelendes** und erscheint in Nebelfarbe – nachtblau (Vorstadt) bzw. Abendorange (Weizenfeld) wirkt als Himmel (Bild `lv_wheat`). Keine Sterne/
   Sonne (sie würden ebenfalls verneben); Straßenlaternen und Fensterlicht tragen die Nacht.
6. **Ausgänge:** EXIT-Türen in der Lobby bleiben (einfacher Weg raus), **Scheune in Ebene 10** = Ausgang ganz unten, Hintertür-Schlüssel bleibt.
   Beide führen in die Oberwelt (Dimensionswechsel, Nebel-Übergang wie 4b).
7. **Ebenen-Tabelle** mit Stimmung, Bauweise, Großbauwerk, Items, Tür-Regel, Gefahr/Rätsel: §2.
**Prüfung:** 10 Ebenen × Großbauwerk × Items ist ein großer Bauumfang → nur stufenweise (I1…I10) baubar; Speicher ~2–2,5× Oberwelt ist
spürbar, aber nicht kritisch (S0 misst, Ausweg gemischtes Raster). Ersatzteil-Logik verhindert Festsitzen. Die Kulissen-Durchsicht ist ehrlich
eine Täuschung – zeigt nicht die echte Stelle. Gefahr ist noch unbeantwortet (Frage g8).
**Bewertung R8:** SG 4,5 · MB 3,5 (Umfang) · PF 3,5 · RR 4 (S1) / 3 (S2) · MU 5 · A12 4.

### R9 – Gegenprüfung (Festsitzen, A12, Umfang, Unabhängigkeit)
- *Festsitzen:* Wartungskasten an jedem Türende + Ersatzteile häufig + Lobby-EXIT + Scheune + Hintertür-Schlüssel + Vanilla-Tod. Fall „Kasten schon
  geleert, kein Teil dabei“: Ersatzteile liegen in jeder Ebene in Beute (Ziel: ≤ 3 Bauplätze Abstand im Mittel, Test misst es).
- *A12:* keine Gegner, kein Schaden (Standard); Oberwelt-Türen nur Oberwelt, wenige (ein Paar je Schwellenkern aus Ebene 10), nur Spieler →
  Netherportal/Elytra/Loren bleiben die Reisewege. Neu in der Oberwelt nur das seltene Haus (abschaltbar) und gebaute Türen.
- *Umfang:* Reihenfolge der Stufen I1 Lobby + Eingang → I2 Poolrooms → I3 Archive → … I10; jede Ebene einzeln abnehmbar (≥ 5 Runden je Stufe).
- *Unabhängigkeit:* Simple Layers + SimpleLib (Portal-API). Grundgewölbe eigene Mod, nur SimpleLib gemeinsam. Kern unberührt.
- *Lore:* SCP-/Backrooms-Ideen sind frei als Motive; Texte/Bilder der Wikis (CC BY-SA) werden **nicht** übernommen, Namen eigene/allgemeine Wörter.
**Bewertung R9 (Endstand):** SG 4,5 · MB 4 (in Stufen) · PF 3,5–4 · RR 4/3 · MU 5 · A12 4,5.

---

## 1. Endstand nach R9 (Eckpunkte; ersetzt den Endstand R6 aus Commit 02b5c3835)
1. **Eine Dimension** `simplelayers:backrooms_stack`, **10 Ebenen**, Raster 112 (96 offen + 16 Bedrock-Sektion), `min_y −816`, Höhe 1136,
   Biom je Ebene nach Höhe, endlos in alle Richtungen, Bedrock-Hülle wie Under21 (Verkleidung + Bedrock-Kern).
2. **Bauraster 16×16 + Raum-Module + Großbauwerk je 256×256**, Treppen-Regel (mind. 3 Luft über Stufe und Vorgängerspalte, Kollisionstest),
   Bauregel-Prüfer.
3. **Portal-Türen** (SimpleLib-API): Rahmen links/rechts/oben, Doppeltür = ein Portal, Symbol + Farbband je Ziel-Ebene, Paare n ↔ n+1 in derselben
   Chunk-Säule (≤ 16 seitlich), **Regel je Ebene: 1 zweiwegig, 2 aktivieren, ab 3 bricht**; Ersatzteile + Wartungskasten.
4. **Eingang:** Haus mit Keller, darin **versiegelte Doppeltür** (Kulissen-Durchsicht, gelber Nebel-Übergang) → Lobby. Kein Riss, kein
   fehlerhaftes Grundgestein mehr als Eingang. **Ausgänge:** Lobby-EXIT, Scheune (Ebene 10), Hintertür-Schlüssel.
5. **Stufen:** S0 Spike → S1 nahtloser Übergang + Schleier → S2 Tiefenfenster → S3 Oberwelt-Türpaare (nur Oberwelt); Inhalte I1–I10.
6. **Grundgewölbe** = eigene Mod (N21-Nachtrag), nicht im Stapel.
7. **Migration:** alte 5 Dimensionen ersetzt, kein Welt-Umzug; Block-Ids bleiben, Riss-/Glitch-Teile entfallen (kein Release).

## 2. Die 10 Ebenen
| # | Name | y | Stimmung | Bauweise | Großbauwerk | Items (Ersatzteil fett) | Tür | Gefahr / Rätsel |
|---|---|---|---|---|---|---|---|---|
| 1 | Lobby | 208–303 | gelbe Tapete, feuchter Teppich, Neonbrummen, nahe Nebelwand | Geschosse 8 hoch (4×) + Atrien/Lichthöfe, Büros, Pausenraum, Sitzecke | Säulensaal 64×64×24 | Tapetenrolle, Teppichrolle (auswringen → Wasser), Neonröhre, Tapetenschaber, Kreide + Kreidebox | zweiwegig | keine; Kreide-Pfeile früherer Wanderer, EXIT nah |
| 2 | Poolrooms | 96–191 | weiße Fliesen, stilles Wasser, hell, Hall | Hallen 10–40 hoch, Becken, Galerien, Terrassenbecken, Rutschenröhren | Großes Becken ≥ 96×64, 40 hoch, Sprungtürme, Tiefbecken 30 | Poolfliesen, Chlortabletten, Beckenleuchte, Schwimmnudel, **Schwimmerventil** | aktivieren | Schleusen-Rätsel: Wasserstand senken, um Tiefbecken-Tür zu erreichen |
| 3 | Archive | −16–79 | verlassene Büros, Aktenschränke, warmes Lampenlicht, Papierrascheln | Bürogeschosse 7 hoch, Regalgassen, Lesesäle über 3 Galerien | Rotunde Ø 48, 6 Galerien, 60 hoch | Akte mit Wegkarte (zeigt Türen + Symbole der Umgebung), Stempel, Karteikarte/Wegbuch, Archivregal, **Schrankschloss** | bricht | Kartei-Rätsel: Katalognummer → Regalfach öffnet Rotunden-Tür |
| 4 | Lagerhalle | −128–−33 | Beton, Hochregale, Kisten, Gabelstapler-Spuren (bewohnbar, ruhig) | Hallen 12 hoch, Regalgassen-Irrgarten, Zwischendecks, Laderampen | Hochregallager 60 hoch mit Kran + Lastenaufzug | Mandelwasser (kleine Heilung), Paletten-/Kistenholz, **Kistenbeschlag** | bricht | Lastenaufzug mit Gegengewichten (richtige Last → fährt) |
| 5 | Rohrgänge | −240–−145 | warm, eng, Dampfzischen, Kupferrohre | Gänge 3 breit/5 hoch, echte Labyrinthe, Kriechgänge, Leiterschächte | Kesselhalle (Kupfertank 32 hoch, Laufstege) | Ventilrad, Dampfrohr-Block, Manometer, **Dichtung** | bricht | Dampfstöße sperren Gänge (schieben zurück, kein Schaden) → Ventile in richtiger Folge schließen |
| 6 | Elektrostation | −352–−257 | Maschinenhallen, Kabeltrassen, tiefes Summen, blaues Licht | Hallen 10 hoch, Schaltwarten, Kabelkanäle als Wege | Turbinenhalle / Umspannwerk | Schaltschrank-Teile (Deko), Kabeltrasse, **Sicherung** | bricht | Stromkreis: Sicherungen setzen → Licht/Tore; dunkle Abschnitte bis gelöst |
| 7 | Hotel | −464–−369 | endlose Flure, roter Teppich, Zimmertüren, gedämpft | Flure 3 breit, Zimmer 6×8, Treppenhäuser, Aufzüge ohne Kabine | Atrium 12 Galerien, Glasaufzug | Zimmerschlüssel (öffnet ein verschlossenes Zimmer mit Beute), Hotelglocke, Teppichläufer, **Türschließer** | bricht | Aufzug-Logik: falsche Stockwerke führen im Kreis, Zimmernummern sind der Hinweis |
| 8 | Lichtlos | −576–−481 | fast völlige Dunkelheit, schwarzer Nebel, ferne Geräusche | Gänge/Hallen ohne Lampen, Kalzit-Markierungen | „Brunnen“: einzige Lichtinsel, tiefer Schacht | Leuchtstäbe (= **Ersatzteil** + tragbares Licht), Laterne der Tiefe | bricht | Orientierung nur mit Kreide/Leuchtstab; keine Monster (Spawnlicht 0, Biom ohne Mobs) |
| 9 | Vorstadt bei Nacht | −688–−593 | Häuserreihen, Straßenlaternen, leere Gärten, Nachthimmel per Nebel | Straßenraster, Häuser 2-geschossig (betretbar, gleich möblierte Innenräume) | Stadtplatz mit Uhrturm | **Hausschlüssel**, Briefkasten-Funde (Notizen, Samen, Karten), Straßenlaterne (Deko) | bricht | „Ein Haus ist anders“: das abweichende Haus birgt die Tür nach unten |
| 10 | Weizenfeld | −800–−705 | endloses Feld, ewiger Sonnenuntergang (Nebel-Himmel), Wind | offen, Feldwege, vereinzelte Vogelscheuchen als Wegweiser | Scheune + Silo (Ausgang Oberwelt) | Bauteile Oberwelt-Portaltür: **Schwellenkern** (sehr selten, Scheune), Erntegarn, Endlos-Weizen (Deko) | bricht | Weite statt Labyrinth; Vogelscheuchen zeigen zur Scheune |

## 3. Ein- und Ausgang
- **Eingang:** seltenes Haus (b-back20, Config `backrooms.shaftRarity` bleibt), Keller mit Absperrband, Warnschild und einer **versiegelten Doppeltür**
  (statt Betonpfropfen + Schacht). Öffnen → Kulissen-Durchsicht der Lobby; durchgehen → gelber Nebel-Übergang → Lobby (gleiche x/z). Zurück durch
  dieselbe Doppeltür (zweiwegig).
- **Entfällt:** Riss-Eingang, fehlerhaftes Grundgestein als Eingang (Feature + Blöcke, Frage g11 ob als Deko-Block bleibt).
- **Ausgänge:** Lobby-EXIT-Türen, Scheune in Ebene 10, Hintertür-Schlüssel (Beute).

## 4. Portal-Türen – technische Ehrlichkeit
| Stufe | Was der Spieler sieht | Technik | Aufwand | Risiko |
|---|---|---|---|---|
| S1 | kein Ladebildschirm zwischen Ebenen, sofort drüben; Öffnung zeigt Schleier; Regeln zweiwegig/aktivieren/bricht | Client-Versatz im Frame + Server-Prüfung, Ziel in derselben Chunk-Säule | M | niedrig |
| S1b | Keller-Doppeltür/EXIT/Scheune: Kulissen-Durchsicht + Nebel-Übergang statt Ladebildschirm | Dimensionswechsel bleibt, Bildschirm ersetzt (Client-Mixin) | S–M | niedrig–mittel |
| S2 | echte Durchsicht zwischen Ebenen (ohne Wesen), endet im Nebel | Tiefenfenster (eigenes Mesh, Tiefe in Öffnung löschen, `LevelRenderEvents`) | L | mittel (Iris-Shader → Schleier) |
| S3 | Schwellentür-Paar **nur in der Oberwelt**; Durchsicht nur in Sichtweite, sonst Schleier | S1 + Ticket an der Gegenseite | M | niedrig–mittel |
Nicht machbar ohne Immersive-Portals-Umbau: echte Durchsicht in eine andere Dimension, auf Ziele außerhalb der Sichtweite, Portal-im-Portal.

## 5. Oberwelt-Portaltür (S3)
Schwellentür-Paar = Schwellenkern (Ebene 10, sehr selten) + Sicherung (6) + Ventilrad (5) + Stempel (3) + Türschließer (7) + 2 Echo-Splitter.
Nur in der Oberwelt setzbar, beide Enden Oberwelt, Reichweite (Frage g9), nur Spieler (Frage g10). Ein Kern = ein Paar; Abbau gibt die Tür zurück.
Regel: zweiwegig (eigene Türen sollen verlässlich sein).

## 6. Risiken und Gegenmittel
| Risiko | Gegenmittel |
|---|---|
| Speicher 2–2,5× Oberwelt je Chunk bei 10 Ebenen | S0 misst; gemischtes Raster (Höhe 944) als Ausweg |
| Kulling trennt doch nicht | S0 misst vor dem Bau |
| Festsitzen hinter kaputter Tür | Wartungskasten je Türende, häufige Ersatzteile, Lobby-EXIT, Scheune, Schlüssel |
| Umfang 10 Ebenen | Stufen I1–I10, jede einzeln abnehmbar |
| Kulissen-Durchsicht wirkt falsch | Lobby ist gleichförmig; Parallaxe; Nebel-Übergang |
| Iris-Shaderpack + Tiefenfenster | Schleier statt Durchsicht |

## 7. Tests (für die spätere Umsetzung)
Rein: Treppen-Kollision, Bauregel-Prüfer, Tür-Paare symmetrisch, Ersatzteil-Abstand je Ebene, Erreichbarkeit/Gefälle. Gametests: Dimension/Biome
je Höhe (10 Bänder), Tür-Regeln (zweiwegig, Aktivierung bleibt, Bruch beim Durchgehen + Reparatur mit Teil der eigenen Ebene, Wartungskasten),
Rahmen fehlt → tot, Doppeltür, Keller-Doppeltür → Lobby → zurück, Scheune → Oberwelt, Schutz in allen Ebenen. Client (Bündel): Übergang ohne
Ladebildschirm, Nebel-Übergang, Tiefenfenster mit/ohne Sodium.

## 8. Restliche offene Fragen (Vorschau, Antwortcode BACK22b)
Empfehlung in Klammern: g1 Reihenfolge (a = B-Variante mit Poolrooms 2/Archive 3) · g2 Raster (a = 112, Höhe 1136) · g3 welche Ebene bestimmt die
Tür-Regel (a = obere) · g4 Aktivierung (a = Ersatzteil einmal) · g5 Reparatur (a = Teil der eigenen Ebene + Wartungskasten) · g6 Keller-Doppeltür
(a = Kulisse + Nebel-Übergang) · g7 Ausgänge (a = Lobby-EXIT + Scheune + Schlüssel) · g8 Gefahr (a = keine Gegner/kein Schaden) · g9 Oberwelt-Tür
Reichweite (a = ≤ 1024) · g10 Durchlass (a = nur Spieler) · g11 Riss/fehlerhaftes Grundgestein (a = ganz entfernen) · g12 Stufenplan (a = S0 + S1 +
I1/I2 zuerst, S2 nach Abnahme) · g13 Durchsicht bei Shaderpack (a = Schleier) · g14 Türkennzeichnung (a = Farbband + Symbol, 10 Entwürfe).
Bilder: /root/sb-tools-b/back22/img (Skripte /root/sb-tools-b/back22/scr/img.py, img2.py, preview.py; gerechnet, keine Spielaufnahmen).
