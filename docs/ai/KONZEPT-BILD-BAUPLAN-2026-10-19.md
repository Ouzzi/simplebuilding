# Konzept: Bild-Bauplan (MC-Paint-Idee) – B, 19.10.2026

Status: Konzept, Umsetzung erst nach Besitzer-Freigabe. Nur Dokument, kein Code.

## Ziel
Ein Bild (Pixelbild, Foto, Logo) wird zu einem Bauplan: als Blaupause (Pixelbild aus Blöcken, flach an der Wand oder am Boden) oder als Karten-Item (Bild auf einer Karte). Eingabe per Code-Text (Import) oder per Bilddatei mit automatischer Umwandlung.

## Ist-Zustand
- SB-Blaupause: `common/.../blueprint/` (`BlueprintCode`, `BlueprintFormats`, `BlueprintBuilder`, `BlueprintMaterials`), Client `client/blueprint/BlueprintIoScreen` (Import/Export als Text, Format automatisch erkannt). Code-Grenzen: `MAX_CODE_LENGTH` 32000 Zeichen, `MAX_EXPANDED_CELLS` 4.194.304; Bauen läuft server-seitig mit Tick-Budget (`maxBlocksPerTick`), Material wird geprüft.
- Simple Maps (`modules/simplemaps`): Tile-/Layer-System (`TileRequestPayload`, `LayerService`, `Reveal`), Karten-Farben stehen nur in `Reveal`/`TileCache`; kein Bild-Import.
- Kein Datei-Dialog-Weg im Spiel vorhanden.

## Vorbilder (öffentliche Seiten, kein Code übernommen)
| Vorbild | Kern | Übernehmen? |
|---|---|---|
| Image2Map-/Map4Image-Art (Server-Befehl mit Bild-Link, Dither-Wahl) | Bild auf Karten | später: nur Datei/Code, kein Link-Abruf (Server-Netzzugriff = Risiko) |
| Artmap AutoPalette (Client, PNG/JPG aus Config-Ordner, Palettenlimit 8/16/32) | Datei-Ordner statt Dialog, Palettenlimit | ja (Ordner `config/simplebuilding/images/`) |
| Map Art Studio / Image to Ground (Web, Blockpalette aus Bau-Blöcken, Floyd-Steinberg für Fotos, aus für Logos) | Palette frei wählbar, Dithering schaltbar | ja |
| Litematica-Schematic-Import | großes Format | nein (Blaupause hat eigenes Format) |

## Zielmod
**Simple Building (Blaupause)** für den Block-Bauplan: dort liegen Format, Material, Bau und Vorschau. Karten-Variante später in **Simple Maps**, ruft nur die gemeinsame Bildumwandlung. Die Umwandlung (Farbraum, Quantisierung, Dithering, Größenbegrenzung) ist Gemeinsames und gehört nach **simplelib** (`simplelib.api.image`, rein rechnerisch, ohne MC-Abhängigkeit testbar). Module bleiben allein spielbar: SB nutzt sie ohne Simple Maps, Maps ohne SB (PRINZIPIEN-MODUL-UNABHAENGIGKEIT).

## Wie kommt das Bild ins Spiel
1. Code: Der Besitzer der Idee will „Code für ein Bild eingeben“. Ein Bild-Code ist ein Blaupausen-Bau-Code (vorhandener Weg, Limit 32000 Zeichen). Ein 64x64-Bild mit 16 Farben passt bequem (Palette + 4 Bit/Pixel, Lauflängen-Kodierung, ca. 2–6 KB). Größer: automatisch herunterrechnen.
2. Datei: Client liest PNG/JPG aus `config/simplebuilding/images/` (Auswahlliste im Import-Fenster; ein echter Betriebssystem-Dialog ist unter Wayland/Remote unzuverlässig, daher Ordner). Client rechnet um und sendet nur den fertigen Code (nie das Rohbild) an den Server. Server-Obergrenze bleibt der Code-Prüfer, kein neuer Upload-Kanal.
3. Kein URL-Abruf in Stufe 1 (SSRF, NSFW-Quellen, Datenschutz).

## Umwandlung
- Blöcke: Palette = Vanilla-Blöcke mit eindeutiger Farbe (Wolle, Beton, Terrakotta, Glas optional), per Config/Auswahl begrenzbar auf Blöcke, die der Spieler hat (Materialprüfung der Blaupause bleibt). Farbabstand in Lab statt RGB.
- Karten: 61 Grundfarben x 4 Helligkeitsstufen (flach 61, mit Höhenstaffel 244 Farben); die Palette kommt aus dem Spiel (`MapColor`), nicht fest codiert, damit Versionen stimmen.
- Dithering: aus / Floyd-Steinberg / geordnet (Bayer); Vorgabe Floyd-Steinberg für Fotos, aus für Logos und Text. Vorschau im Editor vor dem Bauen.
- Größe: 16–128 je Seite, Seitenverhältnis erhalten, Obergrenze per Config.

## Ergebnis
Stufe 1: Blaupause (Wand/Boden, Hologramm-Vorschau wie bisher). Stufe 3: Karten-Item mit Bild (Rahmen-Wand aus mehreren Karten möglich).

## Stufen
1. simplelib-Umwandlung + Tests (Palette, Lab, Dithering, Lauflängen-Kodierung).
2. SB: Import-Reiter „Bild“ im `BlueprintIoScreen` (Ordnerliste, Palette/Dithering/Größe, Live-Vorschau), Ergebnis als Blaupause.
3. Simple Maps: Bild als Karten-Item, Karten-Palette.
4. Optional: Bild-Code teilen/Beispielbilder in der Beispiel-Liste.

## Config-Schalter
SB: `blueprint.imageImport.enabled` (Server, Standard an), `maxSide`, `maxColors`. Maps: `maps.imageMap.enabled` (Standard aus, bis freigegeben). Clientseitig: Ordner und Dithering-Vorgabe.

## Server/Client, Sicherheit, Datenschutz
- Umwandlung nur Client; Server prüft nur Code (Länge, Zellenzahl, Blockliste, Rate). Kein Netzabruf auf dem Server.
- Missbrauch/NSFW: Auf Servern ist ein Bild-Bau sichtbar wie jeder Bau. Admin-Schalter `imageImport.enabled` aus, optional Berechtigungsstufe `imageImport.permission` (Standard 0 im Einzelspiel, auf Servern per Config). Bauvorgänge gehen durch das Block-Protokoll (T10), also nachvollziehbar und rückrollbar. Kein automatischer Bilderkenner (nicht zuverlässig, Datenschutz).
- Karten-Pixel-Daten gehen als normale Kartenpakete; Größenlimit nötig (Karte hat 128x128).
- Lizenz: Bild-Rechte liegen beim Spieler; keine Beispielbilder aus fremder Quelle bündeln.

## Risiken
Farbtreue bei wenigen Blöcken (Foto sieht körnig aus); Textur-Farbmittel pro Resource-Pack unterschiedlich (Palette aus Standard-Pack, Pack-Wechsel ändert Eindruck); Bauzeit/Material bei großen Bildern; Wand-Orientierung und Höhenlimit.

## Tests
simplelib: Palette-Abstand, Dithering-Determinismus, Code-Rundlauf, Größenlimit. SB: Import eines Beispielbilds baut erwartete Blöcke; Code über 32000 wird abgelehnt; Schalter aus blockiert. Client-Test: Ordnerliste, Vorschau.

## Offene Fragen an den Besitzer
1. Zielmod zuerst? Empfehlung: Simple Building (Blaupause), Karten später. | Alternative: zuerst Simple Maps (Karten-Item).
2. Bildquelle? Empfehlung: Datei aus Ordner `config/simplebuilding/images/` plus Code, kein URL-Abruf. | Alternative: zusätzlich Bild-URL (Server/Client lädt herunter; höheres Risiko).
3. Server-Schutz? Empfehlung: Schalter an, aber Berechtigungsstufe konfigurierbar, Bau läuft im Block-Protokoll. | Alternative: auf Servern standardmäßig aus, nur Admin einschaltbar.
4. Blockpalette? Empfehlung: Wolle/Beton/Terrakotta, begrenzbar auf Inventar-Material. | Alternative: freie Auswahl beliebiger Vollblöcke (mehr Farben, mehr Schrott).
5. Dithering-Vorgabe? Empfehlung: Floyd-Steinberg, im Editor abschaltbar. | Alternative: Vorgabe aus (nur flache Farben, sauberer für Logos).
