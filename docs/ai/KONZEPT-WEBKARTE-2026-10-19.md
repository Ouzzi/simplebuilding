# Konzept: Simple Maps Webkarte (2D, später 3D) – B, 19.10.2026

Status: Konzept, Umsetzung erst nach Besitzer-Freigabe. Nur Dokument.

## Ziel
Eine Karte der Server-Welt im Browser (wie JourneyMap Web Map, Dynmap, squaremap, BlueMap): Welt, Spielerpunkte, Wegpunkte. Stufe 1 2D, später 3D wie BlueMap.

## Ist-Zustand
- Simple Maps: Kachel-/Tile-System (`TileRequestPayload`, `TileCodec`, `TileCache`), Server-Dienst `LayerService` (Spieler muss die Karte halten, Fläche und Rate begrenzt, nur bereits erforschte Kacheln), `Reveal` (Erforschung), Wayfinder-Wegpunkte (`WayfinderData`, `Waypoints`).
- Der Erforschungsgedanke ist Spielprinzip: Die Karte zeigt nie mehr, als der Spieler kennt.
- Claims: Simple Tweaks (`claims/`) kennt Besitzer und Bereiche.
- Hosting: Wiki läuft auf CT sb-web hinter Cloudflare Access. Spielserver sind Besitzer-fremd (Mod ist für beliebige Server).
- autoOffWith-Muster: Simple Tweaks (`docs/ai/PLAN-B-AUTOOFF-2026-10-18.md`), Feature wird automatisch aus, wenn eine ersetzende Mod geladen ist.

## Vorbilder (öffentliche Seiten; keine Quellen kopiert)
| Vorbild | Kern | Übernehmen? |
|---|---|---|
| squaremap | leichte 2D-Kacheln, eingebauter Webserver | ja als Maß für Stufe 1 |
| Dynmap | 2D/Isometrisch, Chat, Marker, hoher CPU-/Plattenbedarf | nein (zu schwer), nur Marker-Idee |
| BlueMap | 3D im Browser (WebGL), periodische Aktualisierung, Spieler per WebSocket | später (Stufe 4), nur eigene Umsetzung oder Anbindung |
| JourneyMap Web Map | Client-Karte im Browser, lokal | Idee: Einzelspieler-Ansicht der eigenen Karte |
Lizenzen vor Nutzung prüfen (BlueMap/Dynmap/squaremap jeweils Open Source, genaue Lizenz im Repository); kein Code wird kopiert, nur Funktionsliste.

## Zielmod
**Simple Maps.** Kacheln, Wegpunkte und Erforschung sind dort. Der HTTP-Teil ist Zusatz im Server-Teil. Gemeinsames (Kachel-Export-Format, Palette) in simplelib nur, wenn ein zweites Modul es braucht. Simple Tweaks Claims werden nur bedingt angebunden (kein Muss, Modul bleibt allein spielbar): per Schnittstelle in simplelib (`MapLayerSources`, existiert).

## Zwei Auslieferungswege
A. **Statischer Export** (Empfehlung Stufe 1): Server schreibt Kacheln (PNG, Pyramide `z/x/y.png`) plus `markers.json` periodisch nach `<world>/simplemaps/web/`. Der Besitzer liefert den Ordner mit beliebigem Webserver aus (nginx, sb-web, Cloudflare Access davor). Mod öffnet keinen Port, kein Sicherheitsrisiko im Spielprozess.
B. **Eingebauter HTTP-Server** (Stufe 2, optional, Standard aus): nur auf Config-Port, nur lesend, nur Dateien aus dem Exportordner, `bindAddress` Standard 127.0.0.1, optionales Token. Vorteil Bequemlichkeit; Nachteil Port-Freigaben, Angriffsfläche, Hoster-Regeln.

## Datenquelle und Last
- Nur Chunks, die schon geladen/gespeichert sind; Rendering auf eigenem Thread mit Drosselung (Chunks/Sekunde, Pause bei TPS unter Grenze), nie Chunks nachladen/erzeugen. Erstaufbau in Stunden verteilt.
- Pyramide (Zoom 0–4), Änderungs-Markierung (Dirty-Chunks aus Blockänderungen), Neurender nur dieser Kacheln im Takt (`refreshSeconds`, Standard 120).
- Ansicht: Draufsicht in Karten-Farben (wie Vanilla-Karte, spart Textur-Aufwand) mit Höhenschattierung; Texturansicht als spätere Option.

## Datenschutz und Claims
- Standardmäßig zeigt die Webkarte nur, was auch die Spielkarten zeigen: erforschte Kacheln (Reveal), nicht die ganze Welt.
- Spielerpositionen: Standard aus; Spieler schaltet selbst frei (`/map web show`), Gruppen/Vanish (Simple Tweaks) werden respektiert.
- Claims: Basen hinter Claims können verborgen werden (Option „Claim-Bereiche unscharf“: Kacheln dort nicht aktualisieren oder verpixelt). Privatwegpunkte werden nie exportiert, nur freigegebene.
- Tarnung/Anti-X-Ray: nur Oberfläche, nie Höhlen/Erz; Kachelnamen ohne Spielernamen.

## Kompatibilität
`autoOffWith`: `bluemap`, `dynmap`, `squaremap`, `pl3xmap`, `journeymap` (nur Webteil) aus, Log-Hinweis; Simple-Maps-Karten-Items bleiben. Wenn Kachelordner schon von BlueMap befüllt wird, bietet es sich an, nichts zu doppeln.

## Stufen
1. Export 2D (Kachelpyramide + markers.json) + Weberoberfläche (statische Seite, Leaflet-artig, ohne Fremd-CDN gebündelt), Schalter Standard aus.
2. Optionaler HTTP-Server (Port, Token) und Live-Spieler.
3. Claim-/Datenschutz-Optionen, Wegpunkte, Mehrere Dimensionen.
4. 3D (BlueMap-artig): entweder eigene Voxel-Chunk-Meshes für WebGL oder Anbindung an BlueMap (nur Marker-Quelle bereitstellen, wenn BlueMap geladen). Aufwand hoch, daher Entscheidungsfrage.

## Config
`maps.web.enabled` (Standard aus), `exportDir`, `refreshSeconds`, `maxChunksPerSecond`, `minTps`, `http.enabled` (aus), `http.bind`, `http.port`, `http.token`, `showPlayers` (aus), `hideClaims`, `dimensions`.

## Risiken
Plattenplatz (große Welten: GB), Last beim Erstaufbau, Port-Absicherung, Hoster-Verbot, Karten-Spoiler (Welt wird für Fremde sichtbar), Versionspalette bei neuem MC, 3D-Aufwand.

## Tests
Kachel-Pyramide deterministisch, Drosselung hält Grenzen, Nicht-erforschte Kacheln fehlen, Claim-Bereich verborgen, autoOff bei geladenem BlueMap, HTTP bindet nur an Localhost und liefert nur Dateien im Exportordner (Pfad-Traversal-Test).

## Offene Fragen an den Besitzer
1. Auslieferung? Empfehlung: statischer Export, Auslieferung extern (nginx/sb-web hinter Cloudflare Access). | Alternative: eingebauter HTTP-Server gleich in Stufe 1.
2. Umfang der Anzeige? Empfehlung: nur erforschte Kacheln (wie Spielkarte). | Alternative: ganze Welt wie Dynmap.
3. Spielerpositionen? Empfehlung: Standard aus, Spieler schaltet selbst frei. | Alternative: Standard an, Spieler kann ausblenden.
4. 3D? Empfehlung: erst 2D, danach prüfen, ob BlueMap-Anbindung statt Eigenbau reicht. | Alternative: Eigenbau 3D (WebGL) als feste Stufe.
5. Claim-Schutz? Empfehlung: Claim-Bereiche optional verpixeln/nicht aktualisieren, Schalter Standard aus. | Alternative: Standard an auf allen Servern mit Claims.
