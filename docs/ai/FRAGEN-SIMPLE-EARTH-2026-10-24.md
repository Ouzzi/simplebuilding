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
