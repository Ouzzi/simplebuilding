# Fragen Simple Earth (WC24-4, 24.10.2026)

Branch `b-earth24`, Plan `docs/ai/PLAN-B-EARTH24-2026-10-24.md`. Alles unten ist **eingebaut und als Config änderbar**;
gewählt ist jeweils die Empfehlung aus `KONZEPT-SIMPLE-EARTH-2026-10-23.md` §7.3. Bitte je Punkt nur „passt“ oder die andere Wahl.

## E1–E12 (gewählte Standards)
| # | Frage | Gewählt (Standard) | Andere Wahl |
|---|---|---|---|
| E1 | Kantenlänge | **65 536** Blöcke (Client 8 192 … 262 144, Server-Grenzen `minEdge`/`maxEdge`) | 200 000 |
| E2 | Flugtempo im Orbit | **Orbit-Gleiten 256 b/s** (Server-Deckel `maxOrbitSpeed` 2 048) | echtes Elytra-Tempo (~33 b/s) |
| E3 | Diagonale/Tesserakt | **alle 9 Versionen wählbar, Standard V6** (Besitzer) | anderer Standard |
| E4 | Würfelmitte | **je Spieler** am Aktivierungspunkt, Kachel-Cache global | globales Raster |
| E5 | Wie kommt man hoch | **jede Flugart**, kein eigenes Item | zusätzlich Höhenrakete |
| E6 | Landung | **Wiedereintritt an der Orbit-Position** + 30 s Langsamfall (`slowFallSeconds`) | zurück zum Parkpunkt |
| E7 | Dimensionen | **nur Oberwelt** (`dimensions`) | End als Insel-Würfel später |
| E8 | Seed-Schutz | **alles zeigen**; Server-Option `exploredOnly` (nur Gebiete mit Regionsdatei) | „nur erkundet“ als Standard |
| E9 | Andere Spieler im Orbit | **als Punkte** (`showOthers`), Spieler am Boden nie | aus |
| E10 | Mondphasen | **ja**, Würfelmond von der Sonne beleuchtet, Lage nach Vanilla-Phase | fester Vollmond |
| E11 | Höhen | **1 024 / 1 536 / 6 144** (je Version eigene Werte) | andere Höhen |
| E12 | Name/ID | **Simple Earth, `simpleearth`** | anderer Name |

## Neue Fragen (aus der Umsetzung)
- **N1 Dev-Menü:** Ein Dev-Menü gibt es im Spiel nicht. Gebaut: Knopf „Erde“ oben links im Pausenmenü, nur in der
  Entwicklungsumgebung (Client-Option `devButton` auto/on/off). → a) passt · b) immer zeigen · c) eigenes Dev-Menü für alle Dev-Knöpfe bauen
- **N2 Taste:** Standard **O** („Orbit“; frei in Vanilla und der Familie). → a) passt · b) andere Taste
- **N3 Abmelden im Orbit:** Wer sich geparkt abmeldet, wird an die Orbit-Position (gleiche Höhe) gesetzt und startet dort.
  → a) passt · b) zurück zum Parkpunkt
- **N4 Versionen V7–V9** (neu, Skizze `/root/sb-tools-b/previews/wc24-4-earth-v7-v9.html`): V7 Zellen-Netz (Dalí-Kreuz),
  V8 Netz + Eckfenster + Umstülpen, V9 Faltschale (lückenlos, ohne Überblendung). → welche behalten, welche streichen?
- **N5 V9 als Standard?** V9 ist die einzige Version ganz ohne Bildüberblendung (JUnit belegt: kein Sprung über Kante und Ecke).
  → a) V6 bleibt Standard · b) V9 Standard
- **N6 Höhenschattierung:** Bei 256 Blöcken je Pixel ist die Vanilla-Kartenschattierung flach; Regler `relief` (Standard 1 = Vanilla). → a) passt · b) Standard 2 (plastischer)
- **N7 Mod-Biome:** Farbe = nächste Kartenfarbe zur Grasfarbe des Bioms; eine Datapack-Tabelle `simpleearth:biome_colors` ist noch nicht gebaut. → a) reicht · b) Tabelle bauen
- **N8 Shader (Iris) / Distant Horizons:** nicht geprüft (kein Client mit Shadern). → a) später prüfen · b) jetzt eigene Stufe
- **N9 Andere Loader:** nur Fabric 26.3 gebaut (Besitzer 22.10.). NeoForge/Forge erst auf Anforderung.
- **N10 Fleckigkeit:** Bei 256 Blöcken je Pixel sieht die Karte fleckig aus (echte Biomwechsel; schon mit 2×2-Mehrheit je Pixel
  und „flache Senken bleiben Land“). Client-Bilder: `/root/sb-tools-b/previews/wc24-4-earth-client.html`. → a) passt · b) stärker glätten (z. B. 3×3-Mehrheit, kostet Abtastzeit) · c) Standard-Abtastraster 512
- **N11 Nahsicht:** Direkt über der Umschalthöhe füllt die Fläche den Blick (echte Höhe ≈ 4,5 % der halben Kante); der Würfel-/Netzcharakter
  zeigt sich erst weiter oben (Deckenhöhe 6 144, Orbit-Abstand 3). → a) passt · b) schneller herauszoomen (Orbit-Abstand/Deckenhöhe kleiner)

## Stand 25.10. (WD25-16/17, Branch `b-wd25earth`, Plan `docs/ai/PLAN-B-WD25EARTH-2026-10-25.md`)
Besitzer-Antworten (`/root/sb-tools-b/wd25.md`): Richtung passt; N1/N2/N3/N5 abgenommen; Render-Einstellungen in die Config,
alles von E1–E12 konfigurierbar, N7/N8 Farbtabelle jetzt bauen; N4: V7–V9 bleiben, dazu V10–V12 (Würfelform betont), V6 Standard.

- **Darstellung (alle Versionen, Menü oben, `render` in `simpleearth-client.json`, `/simpleclient simpleearth`):** `detail` 1–5
  (Standard **3** = die Kachel unter dir doppelt so fein wie bisher, 2 = bisher), `detailRange` (Ringe mit voller Stufe, 0),
  `renderDistance` (gezeichnete Kachelringe 1–2, Standard 2 = alles), `paintBudget` (Kilopixel je Bild, 192), `textureCache` (64),
  `tileCache` (2048), `faceEdge` (Kachelrand 0,28), `moonPhases` (E10), `showOthers` (E9, Client-Seite).
  Leistung: keine Chunks; Bilder werden zeilenweise im Budget gemalt (Mitte zuerst), unfertige höchstens alle 250 ms neu;
  Standard lädt weniger Kacheln als vorher (Ring 0 fein, Ring 1 wie bisher, Ring 2 gröber).
- **„Block-Nutzung“** (Server `surfaceDetail`, an): Schnee nach Vanillas Höhentemperatur des Bioms, gefrorenes Wasser in kalten Biomen,
  Stein/Packeis an steilen Hängen, roter Sand und Terrakotta-Bänder in den Badlands – alles aus Höhe + Biom, ohne Chunks.
  Server `minGrid` (feinstes Raster, 32 Blöcke), `biomeVotes` (N10: 1/2/3, Standard 2 wie bisher).
- **E1–E12 als Config:** E1 `edge`, E2 `orbitSpeed`/`maxOrbitSpeed`, E3 `version`, E4 `globalGrid` (neu, aus), E5 `requireFlight`
  (neu, aus = jede Flugart), E6 `reentryAtOrbit` (neu, an; aus = Parkpunkt, gilt auch für N3) + `slowFallSeconds`, E7 `dimensions`,
  E8 `exploredOnly`, E9 `showOthers` (Server + Client), E10 `moonPhases` (neu), E11 Höhen je Version. **E12 (Name/ID) bleibt fest** –
  eine Mod-ID lässt sich nicht per Config ändern.
- **N7/N8 Farbtabelle gebaut:** Datapack-Datei `data/<namespace>/simpleearth/biome_colors.json` (alle Pakete, spätere gewinnen):
  `biomes` (exakte IDs) + `tags` (Reihenfolge). Mitgeliefert: Simple-End-Biome (Aschental hellgrau, Dämmerlande Terrakotta-hellgrau,
  Ränder Endstein-Sand) und 26 `c:`-Konventions-Tags; danach Grasfarben-Fallback. Laden bei Serverstart und `/reload`.
  Shader (Iris)/Distant Horizons weiter ungeprüft (kein Shader-Client).
- **V10–V12 (im Menü wählbar, je eigene Werte):** V10 Kippwürfel (scharf, Oberseite immer waagrecht, kippt an der Kante wie ein Würfel,
  Kantenlinien; `tipTime`, `edgeLines`, `rounding`), V11 Zauberwürfel (kippt; Flächen in n×n Teile mit Fugen über dunklem Kern;
  `cells`, `seam`), V12 Eckansicht (kippt; mit der Höhe dreht sich die Ecke deines Viertels nach oben, drei Flächen wie das Block-Symbol;
  `cornerTilt`, `tiltStart`, `edgeLines`).

### Neue Fragen (25.10.)
- **N12 Detail-Standard:** Stufe 3 (128 Blöcke/Pixel unter dir) → a) passt · b) Stufe 4 (64, lädt ~4× mehr Kacheln) · c) Stufe 2 (bisher)
- **N13 V10–V12:** welche behalten? (Bilder in der Vorschau `wd25earth-preview.html`)
- **N14 Badlands-Bänder:** feste Bandfolge (Vanilla würfelt sie je Seed) → a) passt · b) Seed-abhängig nachbauen
