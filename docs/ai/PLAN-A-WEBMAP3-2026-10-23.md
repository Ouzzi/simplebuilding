# Plan: Simple Maps Webkarte Stufe 3 – Wegpunkte, Dirty-Kacheln, Claim-Verpixeln (A, `a-webmap3`, 23.10.2026)

Grundlage: `docs/ai/KONZEPT-WEBKARTE-2026-10-19.md` (Stufe 3 „Claim-/Datenschutz-Optionen, Wegpunkte“, Abschnitt
„Datenquelle und Last“: Dirty-Chunks), Stufe 1 `PLAN-B-WEBMAP-2026-10-19.md`, Stufe 2 `PLAN-A-WEBMAP2-2026-10-23.md`
(beide gemergt). Nur MC 26.3 Fabric (Regel 22.10.). Stufe 4 (3D) und „mehrere Dimensionen-Optionen“ bleiben offen.

## Ist-Zustand (erhoben)
- `web/WebExport` rendert bei jedem Export **alle** erkundeten 1:1-Kacheln neu (Vereinigung aller Wegfinder-Karten, geladene
  Chunks frisch gelesen), danach die Pyramide der berührten Kacheln (liest Kinder-PNGs von der Platte), dann `markers.json`.
- `webHideClaims`: Chunks, in denen ein `Protection`-Anbieter Fremden (`NOBODY`) das Bauen verbietet, bleiben leer – nur
  Ausblenden, kein Verpixeln. Claim-Änderungen haben kein Ereignis (framework `Protection` hat keinen Zähler).
- Wegpunkte hängen am **Kartenstapel** (Item-Komponente `simplemaps:waypoints`, Besitzer F2), nicht an den SavedData –
  der Server kennt sie nur von Stapeln, die er gerade sieht. `markers.json` enthält bisher keine Wegpunkte.
- `WayfinderData.Tile.version` steigt bei jeder Pixeländerung (nur Sitzung). Blockänderungen: `ServerLevel.sendBlockUpdated`
  (wie Simple Tweaks Anti-X-Ray-Mixin).

## Entscheidungen (selbst getroffen, begründet)
1. **Wegpunkte nur nach ausdrücklicher Freigabe, als Schnappschuss.** `/simplemaps web waypoints share` (jeder Spieler)
   gibt die Wegpunkte der **gehaltenen** Wegfinder-Karte frei: Kopie (Karten-Id, Dimension, Wegpunkte) in der Welt
   (SavedData `simplemaps:web_waypoints`, je Spieler + Karten-Id). Spätere Änderungen am Stapel erscheinen erst nach
   erneutem `share` – nie wird ein Inventar heimlich ausgelesen. `/simplemaps web waypoints unshare` nimmt die gehaltene
   Karte zurück, ohne Karte in der Hand alle eigenen Freigaben. Server-Schalter `webWaypoints` (Standard **aus**).
   „Eigene“ = nur wer die Karte hält, kann sie freigeben (eine Karte in der Hand ist die eigene).
2. **Datenschutz wie Stufe 1/2:** Export nur bei `webWaypoints`, nur auf erkundeten Kacheln der Dimension, mit
   `webHideClaims` nie in geschützten Chunks (auch nicht verpixelt); JSON ohne UUID und ohne Spielernamen (Name, Farbe,
   Icon-Id, x/z, Dimension). Höchstens 4096 Wegpunkte.
3. **Dirty-Kacheln statt Vollexport.** Je Sitzung merkt sich `WebMap` den Stand des letzten fertigen Exports
   (`WebExport.State`: je Kachel eine Signatur = Kachel-Identität + `version` aller Karten + Claim-Maske, und welche Kacheln
   geschrieben sind). Der nächste Export prüft jede erkundete Kachel (1 Budget-Einheit) und rendert nur:
   neue Kacheln, geänderte Karten-Pixel, Kacheln mit Blockänderung (Mixin `ServerLevel.sendBlockUpdated` → Kachel-Schlüssel
   je Dimension, nur bei `webEnabled`, höchstens 262144 Einträge, darüber nächster Export voll) und Kacheln, deren
   Claim-Maske (64 Chunks = 64 Bit) sich geändert hat – so greift ein neuer Claim auch ohne Blockänderung.
   Danach nur deren Pyramiden-Eltern. Voll wird exportiert: erster Export der Sitzung, anderer Ordner/Claim-Modus,
   abgebrochener Export, Überlauf, oder `/simplemaps web export full`.
4. **Claim-Verpixeln:** neuer Schlüssel `webClaimStyle` = `hide` (Standard, wie bisher leer) | `pixelate` (jeder geschützte
   Chunk zeigt seine erkundeten Pixel in **einer** Farbe, der häufigsten des Chunks – Form/Farbe des Geländes grob,
   keine Bauten). Gilt nur mit `webHideClaims`. „Fremd“ = wie bisher: Bauen für Unbeteiligte verboten.
5. Ansicht: Wegpunkte als Raute in Wegpunktfarbe mit Namen, Schalter „Wegpunkte“, Hover zeigt Name + Koordinaten.

## Dateien
- `web/WebExport.java` (State, Claims-Modus, Prüfschritt, Verpixeln, `waypoints()`), `web/WebMap.java` (State, Dirty-Sets,
  Freigaben-SavedData, `start(s, full)`), `web/WebCommands.java` (`export full`, `waypoints share|unshare`),
  neu `mixin/WebDirtyMixin.java` + `simplemaps.mixins.json`, `MapsConfig.java` (`webWaypoints`, `webClaimStyle`)
- `assets/simplemaps/web/index.html` (Wegpunkte), Lang EN/DE (neue Befehlstexte)
- `test/WebTests.java` (+4 Gametests), Fabric-Adapter `ModuleGameTest`
- Doku `docs/SIMPLEMAPS-WEBKARTE.md`, `modules/simplemaps/wiki/manual.json`, Queue, Memory.

## Tests (Gametests, Fabric-Adapter)
`web_dirty_tiles` (voll 3 Kacheln → ohne Änderung 0 → Pixel in Kachel B → genau B → Dirty-Schlüssel C → genau C →
anderer Claim-Modus → voll), `web_dirty_block_change` (setBlock bei `webEnabled` → Kachel-Schlüssel im Dirty-Set),
`web_claims_pixelate` (Standard `hide`, `pixelate` = eine Farbe im geschützten Chunk, freier Chunk unverändert,
unerkundet bleibt durchsichtig; neuer Claim → inkrementeller Export rendert genau die Kachel neu und blendet aus),
`web_waypoints` (Standard aus, ohne Freigabe leer, `share` → nur erkundete, ohne UUID/Spielername, Claim verbirgt,
`unshare` leert).

## Verifikation (sb-test über `/root/sbt a-webmap3`)
Compile `:mc26_3:fabric:compileJava` + `:modules:simplemaps:fabric:compileJava`; Targets `module-simplemaps-fabric-263`,
`module-simplemaps-standalone-fabric-263` (Filter `simplemaps:*`); Claims-Kombination `module-simpletweaks-with-simplemaps-fabric-263`
nur falls vorhanden (Schnittstelle `Protection` unverändert → nicht betroffen); Guides/Wiki/Discover; `./gradlew check -q`.
Nicht getestet: Ansicht im Browser (Besitzer-Abnahme), NeoForge/Forge (Regel 22.10.).

## Risiken
Blockänderungen ohne `sendBlockUpdated` (Flag ohne Client-Update, Weltgenerierung) → erst beim nächsten Vollexport
(Neustart oder `export full`); Signatur-Kollision praktisch ausgeschlossen (64 Bit); Wegpunkt-Schnappschuss veraltet bis
erneutem `share` (gewollt).
