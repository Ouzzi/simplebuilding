# Plan: Claim-Urkunde mit Rechte-Code, Claim-Tisch, Karten-Anzeige und Erweiterung (Queue N54, 13.10.)

Stand 2026-10-13, Fassung 2 (Besitzer-Änderungen vom selben Tag eingearbeitet) · Branch `claude-claimdeed-plan`
(von `claude-wave1` 4f1d5a8e2) · nur Plan, nichts umgesetzt.

Besitzer-Wunsch: Die Claim-Urkunde (Simple Tweaks) wird wie die Blaupause (SimpleBuilding) gehandhabt: eigene GUI,
Personen hinzufügen, Fähigkeiten vergeben. **Entscheidung Besitzer (Fassung 2): Code-Form**, wie der Blaupausen-Editor.
Dazu neu: Claim-Tisch (Block, alle Claims als Karte/Liste), Claims auf der Simple-Maps-Karte, Urkunde kopieren,
so Vanilla-nah wie möglich, Gebietserweiterung mit steigenden Kosten. Mockup: Artifact-Seite „Claim-Urkunde Entwurf“.

## 1. Ist-Zustand

### 1.1 Urkunde (`modules/simpletweaks/shared/java/.../LegacyDeed.java`)
- Item `simpletweaks:claim_deed`, Name „Legacy Claim Deed“, Stapel 16, **kein Rezept, kein Kreativ-Tab** (nur `/give`).
- Rechtsklick (Server, Claims aktiv) = `Claims.claim(player)`: Chunk unter dem Spieler beanspruchen, wie `/claim create`;
  Erfolg = XP-Ton + Funken, Fehlschlag = Werfer-Klick + Rauch. Die Urkunde wird **nicht** verbraucht.
- Kein Item-Datenteil wirkt als Berechtigung („only the server ledger grants authority“) – Grundsatz bleibt.

### 1.2 Claims-Datenmodell und Persistenz (`claims/ClaimStore.java`, `Claims.java`, `ClaimConfig.java`)
- Pro **Chunk** ein Eintrag `Key(dimension, chunk)` → `Claim(owner UUID, whitelist Set<UUID>)`.
- Rechte sind **binär**: Besitzer oder Vertrauter darf alles (`Claim.permits`), sonst nichts.
- Vertrauen gilt **nur für den Chunk, in dem der Besitzer steht** (`trust()` nimmt `actor.chunkPosition()`): wer zehn
  Chunks hat, muss zehnmal `/claim trust` ausführen (je 100 Ticks Abklingzeit).
- Ledger: eine globale JSON-Datei `<welt>/simpletweaks-claims.json`, `version: 1`, Zeilen
  `{dimension, chunk, Owner, Whitelist[]}`; atomar geschrieben (Temp + ATOMIC_MOVE), 32 MiB-Grenze, Symlinks abgelehnt;
  fehlerhafte Daten → `locked` (fail closed, Originale bleiben). Alte NBT-Dateien je Dimension werden nur gelesen.
- Grenzen (Config `config/simpletweaks-claims.json`, serverweit, Neustart): `maxClaimsPerPlayer` 16 (≤256),
  `maxTrustedPlayers` **8** (≤64), `globalCap` 4096 (≤10000), `cooldownTicks` 100, `opBypass` aus, `spawnBuffer` 16,
  Dimensionsliste. Seit 13.10. `enabled: true` (Plan PLAN-CLAIMS-AN).
- **Kein Sync zum Client**, keine Pakete, kein Screen, kein Client-Entrypoint im Modul. Alles serverseitig.

### 1.3 Befehle (`ClaimCommands.java`)
`/claim` (Besitzer anzeigen), `/claim create`, `/claim unclaim`, `/claim trust|untrust <Name|UUID>` (Online-Name oder
volle UUID, keine blockierende Namensauflösung), `/claim admin [unclaim|listall|list <Spieler>]` (OP4). Fremdes `/claim`
→ Warnung, Befehle entfallen, Schutz bleibt.

### 1.4 Schutz-Hooks (~45 Mixins unter `mixin/claims`) und Framework
- Spieler-Aktionen laufen über wenige Eingänge: `Claims.allowBlock` (Abbau `destroyBlock`, **jedes** `useItemOn` –
  Truhe, Tür, Werkbank und Platzieren sind dieselbe Prüfung), `Claims.allow` (`mayInteract`, Block-Item, Eimer),
  `Claims.allowEntity` (`interactOn`, `attack`, `stabAttack`, Boote), `Claims.action/damage` (Geschosse, Effekte, Aufheben).
- Umgebung/Automation (Feuer, Explosion, Fluss, Kolben, Trichter, Werfer, Kupfergolem, Mobs) prüft nur
  „gleicher Besitzer“ – **unabhängig von Vertrauten**, bleibt unverändert.
- Framework `framework/src/main/java/com/simplebuilding/framework/api/Protection.java`: `Target(actor, dimension, x,y,z,
  administrator)` ohne Aktionsart. SB ruft es über `api/WorldPermissions` (`mayAct`, `mayChange`, `mayPlace`,
  `mayAffectEntity`, `mayTeleport`, `mayAutomate`) für Hammer/Vein/Strip, Baustab, Laser, Echo, Pads, Attractor.

### 1.5 Blaupause (SimpleBuilding, Vorbild)
- Item mit Komponente `simplebuilding:blueprint = {code, title, author, signed}`, Rezept formlos Enderquarz + Papier +
  Tintenbeutel. Rechtsklick öffnet `BlueprintScreen` (Kartenblatt-Stil): Material-Liste, **Code-Editor** (eigene
  Bausprache, ≤32000 Zeichen, Syntaxfarben, „Code OK“), 3D-Vorschau, Hilfe-Buch, Signieren | Fertig.
- Speichern: `BlueprintEditPayload(slot, code, sign, title, placed?)`, Server prüft Slot/Position, Item, Länge,
  Signatur, `WorldPermissions.mayChange`, Zeichenbudget (Token-Bucket). Kopieren am Kartentisch (beschrieben + leer →
  Kopie ohne Autor). Text-Export „Text kopieren“ (Zwischenablage).
- **Liegt komplett in SB** (`com.simplebuilding.blueprint.*`, `client/blueprint/*`), nichts davon in simplelib.
  Wiederverwendbar ist nur das **Muster** (Item öffnet Screen, Server validiert Payload, Kopie per Rezept, Text-Export),
  kein Code. Die Bausprache passt inhaltlich nicht (beschreibt Blöcke, nicht Rechte).
- Simple Tweaks darf SB nicht importieren (Regel 3); `modules.json`: simpletweaks `requires: []`,
  `optional: [simpledimensions, simplebuilding]`.

### 1.6 UI-Stil
- SimpleLib-Bausteine `com.simplelib.api.client.ui` (`docs/ai/UI-BAUSTEINE.md`): `UiBoxes.box/slot/insetColored`,
  `UiPalette.derived`, `UiSymbols`, `UiContextMenu`, `UiBookmarks.tab/button/confirm/greyOut`, `UiFilterButton`.
- Screens ohne Slots (N43): `UiBoxes.box` als Fläche, Titel x 8 / y 6 in `palette.label()`, Listen im hellen Feld
  (`insetColored` mit `INVENTORY.fill`, dunkle Schrift), Vanilla-Widgets 8 px vom Rand.
- Helligkeit N49: `UiBrightness` LIGHT/DAWN/DARK wirkt zentral über `UiPalette`, Labels werden bei dunkel hell.
- Simple Tweaks bündelt simplelib heute **nicht** (Vorbild für das Bündeln: `modules/simplemaps` – `include`/`jarJar`,
  `WaypointEditPayload`, `WayfinderScreen`, `MapsFabricClient`).
- Vanilla hat ein mehrzeiliges Textfeld (`MultiLineEditBox`) – Grundlage für einen schlichten, Vanilla-nahen Editor.

### 1.7 Simple Maps (Wayfinder-Karte)
- `modules/simplemaps`, `requires: [simplelib]`, keine Kenntnis von Simple Tweaks. Karte aus Kacheln
  (`TileDataPayload`, 128×128 Farben + Höhen), Zustand `MapStatePayload(mapId, dimension, tiles, marks)`.
- `WayfinderScreen` zeichnet Ebenen nacheinander: Kacheln → Raster → **Strukturen** (`drawStructures`, Rauten in
  Farbe aus der ID, Werkzeugknopf an/aus) → Wegpunkte → Fußzeile. Eine Claims-Ebene passt genau in dieses Muster.
- Kopplung zwischen Mods läuft bisher über `com.simplelib.api.SimpleLibApi` (Register-Muster, z. B.
  `registerHeatSource`/`heatOf`, `registerToolUse`): ein Mod meldet einen Dienst an, ein anderer fragt ihn ab, keiner
  importiert den anderen.

## 2. Entscheidung: Code-Form (Besitzer, Fassung 2)

Gebaut wird der **Rechte-Code** in einem Textfeld, wie der Blaupausen-Editor. Kein Checkbox-Raster. Gründe, die für die
Code-Form sprechen: ein Bedienmuster mit der Blaupause, Text lässt sich kopieren, einfügen, im Chat teilen und von
Admins/KI lesen, ein Code kann Abschnitte (einzelne Chunks) ausdrücken, ohne zusätzliche Knöpfe.
Die Schwächen (Tippfehler, Buchstaben lernen) fängt der Editor ab: Kommentarkopf mit Erklärung, Prüfung beim Tippen,
Fehlerzeile mit Grund, Speichern nur ohne Fehler (alles oder nichts), Hilfe im Guide.

## 3. Rechte-Code

### 3.1 Rechte-Buchstaben (englisch, Bitmaske `int`)
| Bit | Buchstabe | Name (EN / DE) | Deckt ab |
|---|---|---|---|
| 0 | **B** | Build / Bauen | Blöcke platzieren, Eimer ausgießen, SB-Baustab/Pads platzieren |
| 1 | **D** | Destroy / Abbauen | Blöcke abbauen, Eimer füllen, SB Hammer/Vein/Strip/Laser, Feldtrampeln durch Spieler |
| 2 | **U** | Use / Benutzen | Türen, Falltüren, Tore, Knöpfe, Hebel, Druckplatten, Glocke, Werkbänke/Amboss/Zaubertisch, Betten |
| 3 | **C** | Container / Container | Truhen, Fässer, Öfen, Trichter-GUI, Shulker, Lesepult, Bücherregal, Topf, Rahmen, Rüstungsständer, Aufheben, Attractor |
| 4 | **R** | Redstone / Redstone | Verstärker, Komparator, Notenblock, Tageslichtsensor, Crafter einstellen |
| 5 | **A** | Animals & vehicles / Tiere & Fahrzeuge | füttern, scheren, anleinen, handeln, verletzen; Boote/Loren setzen, fahren, abbauen |
| 6 | **M** | Manage / Verwalten | Code der Urkunde ändern (Grenzen §3.5), Urkunden-Kopie benutzen |

Kollisionsprüfung: sieben verschiedene Buchstaben, keiner ist zugleich Rollen-Präfix (`@`) oder Abschnittszeichen
(`[`). Groß/klein egal (`bdu` = `BDU`), doppelte Buchstaben sind ein Hinweis, kein Fehler. Kein Buchstabe = Person
ohne Rechte (Zeile wird beim Speichern entfernt, Hinweis). Unbekannter Buchstabe (z. B. `P`) = Fehler mit Vorschlag.
`ALL = BDUCRAM`, `LEGACY = BDUCRA` (= bisher „vertraut“).

### 3.2 Syntax
```
# Claim-Urkunde von Ouzzi
# Rechte: Buchstaben einfach aneinanderreihen hinter Name oder UUID, z. B.  Steve: BDU
#   B Build  D Destroy  U Use  C Container  R Redstone  A Animals/vehicles  M Manage
# Rollen statt Buchstaben: @guest (U)  @helper (UCA)  @member (BDUCRA)  @manager (alles)
# Jeder Spieler: *: U      Nur ein Chunk: [x,z] darunter Zeilen      # = Kommentar
*: U
Steve: BDU
Alex: @member
069a79f4-44e9-4726-a5be-fca90e38aaf5: UC
[12,-4]
Steve: BDUC
```
- Zeile = `<Spieler>: <Buchstaben|@rolle>`; Spieler = Online-Name, Name aus dem Server-Namens-Cache oder volle UUID
  (nie Netzwerk-Abfrage, gleiche Regel wie `/claim trust`). `*` = alle Spieler.
- Oberer Teil gilt für **alle Claims des Besitzers**; `[x,z]` (Chunk-Koordinaten, Dimension = aktuelle, optional
  `[nether 3,7]`) beginnt einen Abschnitt, der für diesen Chunk die Zeilen **ersetzt** (je Person: Abschnitt vor oberem Teil).
- Der Kommentarkopf wird bei jeder neuen Urkunde vorbelegt und bleibt stehen (wie Vanilla-Buch-Vorlage).
- Speichern normalisiert: Namen bleiben lesbar, der Server merkt sich UUIDs; Anzeige mit aktuellem Namen.

### 3.3 Grenzen und Parser (`ClaimCode`, shared, ohne Client-Klassen)
- ≤ 4096 Zeichen, ≤ 200 Zeilen, Zeile ≤ 96 Zeichen, ≤ `maxTrustedPlayers` (8, ≤64) Personen oben, ≤ 32 Abschnitte,
  je Abschnitt ≤ `maxTrustedPlayers` Personen, nur Chunks des Besitzers.
- Der Client parst live für die Statuszeile („Code OK“ / „Zeile 7: unbekannter Name ‚Tomm‘“). **Der Server parst neu und
  entscheidet**; Namen löst nur der Server auf. Antwort `ClaimCodeResultPayload(ok, List<(line, key, arg)>)` ≤ 16 Fehler.
- Fehlerarten: Syntax, unbekannter Buchstabe, unbekannter Name/UUID, Null-UUID, Besitzer selbst, zu viele Personen,
  fremder/ungültiger Chunk im Abschnitt, Abschnitt doppelt, M ohne Besitzerrecht, Rechte über eigene (Verwalter).

### 3.4 Datenmodell (Ledger v2)
- Neu je Besitzer ein **Landrecht**: `Land(owner, Map<UUID,Integer> members, int everyone, Map<Key, Section> sections,
  String code, int budget)`; `code` = gespeicherter Quelltext (Kommentare bleiben erhalten), Rechte daraus abgeleitet.
- Chunk-Zeilen bleiben `{dimension, chunk, Owner}`; die Rechte-Abfrage: Abschnitt des Chunks → sonst oberer Teil → sonst
  `*` → sonst nichts. Besitzer immer alles.
- Datei `simpletweaks-claims.json` `version: 2`: `claims[]` + `lands[]`. Größe bleibt unter 32 MiB
  (10000 Claims + ≤10000 Länder × 4 KiB Code ≈ 40 MiB Spitze → Code-Grenze je Land 4096, `globalCap` begrenzt Länder;
  Grenze `MAX_BYTES` auf 64 MiB anheben, offene Frage F9).

### 3.5 Verwalten (M)
Verwalter dürfen den Code ändern, aber nur Personen **ohne M** hinzufügen/ändern/entfernen und nur Rechte vergeben, die sie
selbst haben; `*`-Zeile, M-Vergabe, Abschnitte für Chunks und `unclaim` bleiben dem Besitzer. Der Server vergleicht alten
und neuen Zustand und lehnt jede Änderung außerhalb dieser Grenzen ab (Fehlerzeile „nur der Besitzer“).

### 3.6 Migration v1 → v2
- Je Besitzer: haben alle seine Chunks dieselbe `Whitelist`, wird sie oberer Teil mit `BDUCRA`; sonst bekommt jeder Chunk
  mit eigener Liste einen `[x,z]`-Abschnitt mit genau dieser Liste und der obere Teil bleibt leer.
  **Kein Rechte-Gewinn**, Verhalten bleibt gleich. Der Code wird mit Kommentarkopf erzeugt.
- Budget (§7): `max(startChunks, Anzahl vorhandener Claims)` – niemand verliert Land.
- Vorher einmalig Kopie `simpletweaks-claims.v1.json`; unbekannte Version/Bits → `locked` (fail closed).

## 4. Urkunde (Item, Vanilla-nah)
- „Claim-Urkunde“ / „Claim Deed“ (statt „Legacy Claim Deed“), Registry-ID bleibt `simpletweaks:claim_deed`, Stapel 1,
  Kreativ-Tab Simple Tweaks. Optik wie Vanilla-Papier/Buch mit rotem Wachssiegel (Textur-Abnahme F7).
- **Rezept formlos:** Papier + Tintenbeutel + Honigwabe → 1 leere Urkunde (nur Vanilla-Material).
- **Leer:** Rechtsklick → Bildschirm „Hier beanspruchen“ (= `/claim create`); danach ist die Urkunde an den Besitzer
  gebunden (Komponente `simpletweaks:deed = {owner, ownerName, generation}`; Anzeige, **keine** Berechtigung).
- **Gebunden:** Rechtsklick öffnet den Code-Editor des Landes. Wer weder Besitzer noch M hat, sieht nur
  „Urkunde von X – du darfst hier: U C“.
- Die Urkunde wird beim Beanspruchen nicht verbraucht; Land begrenzt das Budget (§7).
- **Kopieren (Vanilla wie Karten/Bücher):** Werkbank, formlos: gebundene Urkunde + leere Urkunde → beide gebunden
  (Original bleibt, wie `MapCloningRecipe`/`BookCloningRecipe`). `generation` Original → Kopie → Kopie einer Kopie;
  Kopien einer Kopie sind nicht möglich (wie Vanilla-Bücher), damit Kopien nachvollziehbar bleiben.
  Rezept kennt den Spieler nicht; deshalb gilt: kopieren darf jeder mit einer gebundenen Urkunde, **wirken** tut die
  Kopie nur bei Besitzer oder M (Server prüft beim Öffnen/Speichern). Eine weitergegebene Kopie ohne M zeigt nur Lesesicht.

## 5. Editor (Urkunde) – Vanilla-nah
```
┌ Claim-Urkunde von Ouzzi ─────────────── Land 6 / 8 Chunks ─┐
│ ┌ Code (Vanilla MultiLineEditBox, Schrift wie Buch) ──────┐ │
│ │ # Rechte: Buchstaben aneinanderreihen …  Steve: BDU      │ │
│ │ *: U                                                    │ │
│ │ Steve: BDU                                              │ │
│ │ Alex: @member                                           │ │
│ └─────────────────────────────────────────────────────────┘ │
│ ✔ Code OK · 3 Personen · 1 Abschnitt          412 / 4096    │
│ Online: [Lena +] [Tom +]        (fügt „Name: U“ ein)        │
│ [?]  [Kopieren] [Einfügen]                [Abbrechen] [Fertig] │
└─────────────────────────────────────────────────────────────┘
```
- Kasten `UiBoxes.box` (simplelib) in Papierfarbe, folgt `UiBrightness`; Textfeld = Vanilla `MultiLineEditBox`
  (keine Syntaxfarben in Phase 1, Vanilla-nah, wenig Aufwand; Farben optional Phase 3).
- Statuszeile unter dem Feld (wie Blaupause „Code OK“ / Fehler mit Zeile, Klick springt zur Zeile).
- Online-Knöpfe fügen eine Zeile `Name: U` ein (Hilfe gegen Tippfehler bei Namen).
- Kopieren/Einfügen: Zwischenablage (Text-Export/Import ist der Hauptweg zum Übertragen).
- `[?]` öffnet die Guide-Seite (TweaksGuide) mit Buchstaben, Rollen, Beispielen.
- Senden nur bei „Fertig“; Server-Antwort mit Fehlern → Editor bleibt offen, Fehler markiert.

## 6. Claim-Tisch (neuer Block)
- **Zweck:** zeigt alle aktiven Claims (wer hat was), dient als Ort zum **Erweitern** (§7). Vanilla-Vorbild Kartentisch.
- **Rezept:** wie Kartentisch, oben Papier + Urkunde statt 2 Papier: `Papier, leere Urkunde / 4 Bretter` (geformt 2×3) →
  1 Claim-Tisch. Abbaubar mit Axt, Optik Holztisch mit Karte und Siegel.
- **Empfehlung Ansicht: Karte mit Liste daneben** (Reiter „Karte“ | „Erweitern“).
  - Karte: Chunk-Raster um den Tisch, je Besitzer eine Farbe (aus UUID abgeleitet, wie Strukturen in Simple Maps), eigenes
    Land hervorgehoben, Hover = Besitzer + Chunk-Koordinaten, Ziehen/Zoom 3 Stufen (8/16/32 Chunks Radius).
    **Ohne Gelände** (Simple Tweaks hat keine Kacheln); das Gelände ist Aufgabe von Simple Maps (§8).
  - Liste: Besitzer, Anzahl Chunks, Dimensionen; Klick zentriert die Karte; Filter „nur meine“.
  - Eigene Zeile: Budget „6 / 8 Chunks“, nächste Stufe.
- **Datenschutz (Server-Config `claimVisibility`):** `all` (Standard: alle Claims mit Besitzername sichtbar – Besitzer will
  „wer was geclaimt hat“), `owners_hidden` (Umrisse sichtbar, Name „Fremder Claim“), `own` (nur eigene + Claims, auf
  denen man Rechte hat), `off` (Tisch zeigt nur Erweitern). **Mitgliederlisten/Codes fremder Länder werden nie gesendet.**
  OP sieht immer `all`.
- **Sync-Grenzen:** Anfrage `ClaimAreaRequest(dimension, minChunkX, minChunkZ, w, h)` mit `w,h ≤ 64`; Antwort
  `ClaimAreaPayload` = Besitzer-Palette (≤ 255 Einträge: UUID, Name ≤16, Farbe) + Zellen als Byte-Index (≤ 4096 Bytes,
  Lauflängen-kodiert). Höchstens 4 Anfragen/s je Spieler (Token-Bucket wie Blaupause), Liste seitenweise je 50 Einträge.
  Nur während das Menü offen ist und der Spieler ≤ 8 Blöcke vom Tisch steht; Dimension = Dimension des Tisches.

## 7. Erweiterung mit steigenden Kosten
- **Budget je Spieler** (im Ledger, `Land.budget` = erlaubte Chunks). Neue Spieler: `startChunks` (Standard 4).
  Jede Stufe erhöht das Budget; je größer das Land, desto teurer. Budget gehört dem Spieler, nicht der Urkunde
  (Kopien verdoppeln nichts). `unclaim` gibt keine Kosten zurück, das Budget bleibt.
- **Bedienung (Vanilla-nah wie Kartentisch/Amboss):** Claim-Tisch, Reiter „Erweitern“: Slot Urkunde + Slot Material →
  Ergebnis = Urkunde mit Text „Land: 8 → 12 Chunks“, dazu XP-Kosten wie beim Amboss. Beim Herausnehmen
  (`ResultSlot.onTake`, Server, Spieler bekannt) wird das Material verbraucht und das Budget im Ledger erhöht
  (atomar, erst nach erfolgreichem Speichern gebucht; scheitert das Speichern, bleibt das Material). Nur der Besitzer
  kann mit seiner Urkunde erweitern (M reicht nicht – Kosten trägt der Besitzer, F11).
- **Vorschlag Kostenkurve (Server-Config `expansionTiers`):**

| Stufe | Land danach | Material | XP-Level |
|---|---|---|---|
| Start | 4 Chunks | Urkunde (Papier, Tintenbeutel, Honigwabe) | – |
| 1 | 6 | 8 Kupferbarren | 1 |
| 2 | 8 | 8 Eisenbarren | 2 |
| 3 | 12 | 8 Goldbarren | 3 |
| 4 | 16 (= bisheriges Limit) | 4 Diamanten | 5 |
| 5 | 24 | 8 Diamanten + 8 Smaragde | 8 |
| 6 | 32 | 2 Netheritbarren | 10 |
| 7 | 48 | 4 Netheritbarren | 15 |
| 8 | 64 | 1 Netherstern | 20 |
Kosten je Chunk steigen damit von ~4 Kupfer auf ~1/16 Netherstern; die Kurve ist absichtlich konvex.

- **Config** (`config/simpletweaks-claims.json`, serverweit, Neustart, validiert wie bisher):
  `startChunks` 4 (1..256), `expansionTiers` Liste `{chunks, items:[{id, count}], levels}` (≤ 32 Stufen, `chunks` streng
  steigend, Item-IDs müssen existieren, Anzahl 1..64·9), `expansionEnabled` true. `false` = altes Verhalten: Budget =
  `maxClaimsPerPlayer`.
- **Zusammenspiel mit Limits:** `maxClaimsPerPlayer` bleibt **harte Obergrenze** (Standard von 16 auf 64 angehoben, ≤ 256);
  wirksam ist `min(budget, maxClaimsPerPlayer)`. Bestehende Server mit `16` in der Datei bleiben bei 16 (Datei gewinnt) –
  Stufen über 16 werden dann im Tisch grau mit „Server-Grenze“ angezeigt. `globalCap`, Abklingzeit, Spawnpuffer,
  Weltgrenze, Dimensionsliste unverändert. OP mit `opBypass` braucht kein Budget.
- Befehle: `/claim budget` (eigenes Budget + nächste Stufe), `/claim admin budget <Spieler> <n>` (OP4).

## 8. Simple Maps zeigt Claims
- **Kopplung nur über simplelib** (Regel 6a, keine Importe zwischen Maps und Tweaks):
  `SimpleLibApi.registerClaimView(ClaimView)` / `SimpleLibApi.claimView()` (Optional). `ClaimView` (serverseitig):
  `ownersIn(ServerLevel, minChunkX, minChunkZ, w, h, ServerPlayer viewer)` → Palette + Zellen (gleiches Format wie §6),
  bereits nach `claimVisibility` gefiltert. Simple Tweaks registriert beim Serverstart, wenn Claims aktiv sind.
- Simple Maps fragt den Dienst beim Senden des Kartenzustands ab und schickt ein eigenes `ClaimOverlayPayload`
  (Maps-Namensraum). Ohne Simple Tweaks: Dienst leer → keine Ebene, kein Knopf. Ohne Simple Maps: nichts passiert.
- Darstellung im `WayfinderScreen`: neue Ebene `drawClaims` nach den Kacheln, vor Strukturen: halbtransparente Füllung
  (25 %) + 1-px-Umriss an Besitzergrenzen, Farbe je Besitzer (gleiche Ableitung wie Claim-Tisch), eigenes Land mit
  hellerem Umriss. Tooltip beim Hover: „Claim von Steve“ (bzw. „Fremder Claim“ bei `owners_hidden`). Werkzeugknopf
  „Claims“ an/aus wie „Strukturen“ (`UiBookmarks.button`), Zustand lokal.
- Nur sichtbarer Bereich, gleiche Grenzen wie §6 (≤ 64×64 Chunks je Anfrage, ≤ 4/s), Cache im Client je Karte, verfällt
  beim Schließen. Gerahmte Karten (Item-Rahmen) zeigen keine Claims (keine Last für Zuschauer), F12.
- simplelib-Version anheben (`>=0.3.0` bei beiden Nutzern), Test im Standalone-Target beider Mods.

## 9. Netzwerk und Server-Sicherheit (Zusammenfassung)
| Paket | Richtung | Grenzen |
|---|---|---|
| `ClaimDeedOpenPayload` | S→C | Modus, Besitzername, Code ≤ 4096, Budget, Online-Namen ≤ 64 × 16 |
| `ClaimCodeEditPayload(slot, code, revision)` | C→S | `stringUtf8(4096)`, Slot gültig, Urkunde gebunden |
| `ClaimCodeResultPayload` | S→C | ≤ 16 Fehler |
| `ClaimCreatePayload(slot)` | C→S | ruft `Claims.claim` |
| `ClaimAreaRequest/ClaimAreaPayload` | C↔S | w,h ≤ 64, Palette ≤ 255, ≤ 4/s, nur am Tisch |
| `ClaimOverlayPayload` (Maps) | S→C | wie oben, nur mit offener Karte |
Server prüft immer neu: Urkunde im Slot und gebunden, Besitzer oder M (§3.5), `revision` (Hash des Landes; sonst
„Daten haben sich geändert“), vollständiges Parsen, Namensauflösung nur online/Cache, Grenzen §3.3, Abklingzeit
(`mutationReady`, ein Commit je Speichern), Hauptschalter an, Ledger nicht `locked`. Alles oder nichts.

## 10. Befehle
- `/claim trust <Spieler> [@rolle|Buchstaben]` – ohne Angabe `@member` (= bisheriges Verhalten); ändert die Zeile im Code
  (oberer Teil); `/claim untrust <Spieler>` entfernt sie.
- `/claim code` – zeigt den Code im Chat (klickbar „Kopieren“); `/claim info` – Rechte am aktuellen Chunk.
- `/claim budget`, `/claim admin budget …` (§7); übrige Befehle unverändert.
- Befehle und Editor schreiben über dieselbe Server-Methode `Claims.applyCode(actor, newCode)`.

## 11. Hook-Zuordnung und Framework
- Spieler-Hooks fragen ein Recht ab: `destroyBlock` → D; `useItemOn`: Block mit Container/MenuProvider → C, Tags
  `simpletweaks:claim_redstone` → R, `simpletweaks:claim_use` (Türen, Schalter, Arbeitsblöcke, Betten) → U, Block-Item in
  der Hand/Platzieren → B, sonst **streng** B+D. Eimer: ausgießen B, füllen D. Entities: Tiere/Fahrzeuge → A,
  Rahmen/Rüstungsständer → C, Aufheben → C. PvP im Claim: wie bisher (jedes Recht ≠ 0 = früher „vertraut“).
- Framework `Protection`: `enum Action {BUILD, DESTROY, USE, CONTAINER, REDSTONE, ENTITY, ANY}` + Überladung
  `allows(server, target, action)`, `Check` mit Default auf `allows(target)`; alte Aufrufe = `ANY` = B+D (streng).
  SB `WorldPermissions` gibt die Aktion mit (Hammer → DESTROY, Baustab → BUILD, Attractor → CONTAINER,
  `mayAffectEntity` → ENTITY). Automation/Umgebung (gleicher Besitzer) unverändert.

## 12. Dateien
- simpletweaks: `claims/ClaimRights.java` (Buchstaben, Rollen), `claims/ClaimCode.java` (Parser, Fehler, Normalisieren),
  `ClaimStore.java` (v2, Länder, Migration, v1-Kopie), `Claims.java` (`allowed(…, right)`, `applyCode`, Budget),
  `ClaimCommands.java`, `ClaimConfig.java` (`startChunks`, `expansionTiers`, `claimVisibility`, `expansionEnabled`),
  Mixins aus §11; `LegacyDeed.java` → `deed/ClaimDeedItem.java`, `deed/DeedCloningRecipe.java`;
  `table/ClaimTableBlock.java` + `ClaimTableMenu.java` (Erweitern-Slots) + Block-Entity nicht nötig;
  `net/*` (Pakete §9), `client/ClaimDeedScreen.java`, `client/ClaimTableScreen.java`; Fabric-`client`-Entrypoint,
  NeoForge-Client-Event; Forge erst auf Anforderung.
- simplelib: `SimpleLibApi.ClaimView` + `registerClaimView/claimView`; Version 0.3.0; `UI-BAUSTEINE.md`.
- simplemaps: `net/ClaimOverlayPayload.java`, `client/WayfinderScreen#drawClaims` + Knopf, Lang.
- framework: `Protection.Action`; SB `api/WorldPermissions` Aktion durchreichen.
- Daten: Rezepte (Urkunde, Kopie-Serializer, Claim-Tisch), Tags, Loot-Table Tisch, Modelle/Texturen (Urkunde leer/
  gebunden, Tisch), Lang EN/DE, `modules.json` (`simpletweaks requires simplelib`), Mod-Metadaten, Wiki, Guide.

## 13. Tests
- JUnit (ohne MC): `ClaimCode` (jede Fehlerart mit Zeile, Rollen, `*`, Abschnitte, Groß/klein, Grenzen 4096/200/96,
  Normalisieren), Migration v1 → v2 (gleiche Listen → oberer Teil; verschiedene → Abschnitte; kein Rechte-Gewinn; Budget),
  Config-Validierung `expansionTiers`, Farbe je Besitzer stabil, Lauflängen-Kodierung Zellen.
- GameTests (Fabric + NeoForge 26.3, Filter `*claim*`):
  - je Buchstabe: mit nur diesem Recht genau diese Aktion erlaubt, ohne abgelehnt (Platzieren, Abbauen, Tür, Truhe,
    Verstärker, Schaf, Boot); Abschnitt `[x,z]` ersetzt oberen Teil; `*`-Zeile.
  - Verwalter: kein M vergeben, keine fremden Rechte, kein `*`, keine Abschnitte.
  - Edit-Paket: falscher Slot, leere/fremde Urkunde, Kopie ohne M, veraltete `revision`, 4097 Zeichen, unbekannter Name,
    Abklingzeit, `locked` → abgelehnt, Ledger unverändert; Fehlerliste mit richtiger Zeile.
  - Kopie: Werkbank gebunden + leer → zwei; Kopie der Kopie nicht möglich; Kopie öffnet bei M den Editor, sonst Lesesicht.
  - Budget: Claim über Budget abgelehnt; Erweitern verbraucht Material + XP und hebt Budget; Speichern scheitert → kein
    Verbrauch; `maxClaimsPerPlayer` deckelt; `expansionEnabled=false` = altes Verhalten.
  - Claim-Tisch: Bereichsanfrage > 64 abgelehnt, Rate-Grenze, `claimVisibility own/owners_hidden` filtert, keine Codes.
  - Bestehende 61 Claim-GameTests grün (Migration ändert nichts).
  - Maps: Overlay nur mit Tweaks; Maps-Standalone ohne Ebene; Tweaks-Standalone ohne Maps.
- Client-Tests: Editor öffnen/tippen/Fehlerzeile, Tisch-Karte, Maps-Knopf; Screenshots hell/dunkel nach `previews/`.

## 14. Wiki, Guide, Tooltip
- Tooltip (AGENTS 14): leer „Rechtsklick: Chunk beanspruchen“; gebunden „Urkunde von Ouzzi“ (+ „Kopie“). Kein Budget/
  keine Rechte im Tooltip (stehen im Editor/Tisch).
- Wiki `modules/simpletweaks/wiki/manual.json`: Urkunde, Rechte-Code (Buchstaben-Tabelle, Rollen, Beispiele), Kopieren,
  Claim-Tisch, Erweiterung mit Tabelle, Befehle, Config; „mit Simple Maps: Claims auf der Karte“ (Regel 7).
  Simple-Maps-Wiki: „mit Simple Tweaks: Claims-Ebene“. Guide-Seiten in TweaksGuide.

## 15. Offene Fragen (mit Empfehlung)
- **F1 Syntaxfarben im Editor?** Empfehlung: Phase 3; Phase 1 Vanilla `MultiLineEditBox` + Statuszeile.
- **F2 Recht „Ernten“** (reife Pflanzen ohne D)? Empfehlung: später als Buchstabe **H** (Harvest), frei, keine Kollision.
- **F3 Startgröße/Kurve:** Empfehlung 4 Chunks Start und Tabelle §7; Besitzer bestätigt Materialien.
- **F4 Standard `claimVisibility`:** Empfehlung `all` (Wunsch „wer was geclaimt hat“), Mitgliederlisten nie.
- **F5 Kopie der Kopie** verbieten (wie Bücher)? Empfehlung: ja.
- **F6 Abschnitte `[x,z]`** in Phase 1? Empfehlung: ja (nötig für verlustfreie Migration).
- **F7 Texturen** Urkunde (leer/gebunden) und Claim-Tisch: 2 Entwürfe als Artifact-Abnahme.
- **F8 simplelib bündeln** in Simple Tweaks: ja (UI-Kasten + ClaimView-Dienst).
- **F9 Ledger-Grenze** 32 → 64 MiB: ja, oder Code je Land auf 2048 Zeichen.
- **F10 `maxClaimsPerPlayer`-Standard** 16 → 64 (nur neue Configs): ja.
- **F11 Erweitern nur Besitzer** (nicht M)? Empfehlung: ja.
- **F12 Claims auf gerahmten Karten?** Empfehlung: nein (Last, Datenschutz).
- **F13 Rückerstattung bei `unclaim`?** Empfehlung: nein, Budget bleibt.

## 16. Aufwand und Phasen
| Phase | Inhalt | Aufwand |
|---|---|---|
| 1 | Rechte-Bits, `ClaimCode`-Parser, Ledger v2 + Migration, Hook-Zuordnung, Framework-Aktion, Befehle, Tests | ~2,5 Agent-Tage |
| 2 | Urkunde (Rezept, Bindung, Kopie), Editor, Pakete, simplelib bündeln, Lang/Wiki/Guide | ~1,5 Agent-Tage |
| 3 | Claim-Tisch (Block, Karte+Liste, Sichtbarkeit) + Erweiterung (Budget, Tiers, Menü) | ~2 Agent-Tage |
| 4 | Simple-Maps-Ebene über simplelib-`ClaimView` | ~1 Agent-Tag |
| 5 | Texturen-Abnahme, Syntaxfarben, ggf. „Ernten“ | ~0,5–1 Agent-Tag |
Gesamt ~7,5–8 Agent-Tage auf 4–5 Branches nacheinander; Phase 1 zuerst allein mergen (berührt alle Claims).
Läufe nacheinander auf sb-test: simpletweaks Fabric 26.3 → NeoForge 26.3 → SB-Filter → simplemaps → Standalone-Targets.

## 17. Risiken
- Falsche Hook-Zuordnung lockert Schutz → unbekannt = streng (B+D), je Buchstabe ein Negativtest.
- Code-Parser auf dem Server: harte Grenzen vor dem Parsen (Länge, Zeilen), keine Regex mit Rückverfolgung, keine
  Namensabfragen übers Netz.
- Erweitern: Material-Verlust bei Speicherfehler → erst buchen, dann verbrauchen; Test dafür.
- Downgrade nach v2 sperrt Claims (fail closed) – v1-Kopie bleibt, Changelog.
- Kartendaten: Rate- und Größengrenzen, Sichtbarkeit serverseitig gefiltert, nie Codes/Mitglieder fremder Länder.
- Abklingzeit 100 Ticks → Editor zeigt Restzeit statt „hängt“.
