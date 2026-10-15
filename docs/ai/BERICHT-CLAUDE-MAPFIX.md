# Bericht claude-mapfix (Wayfinder-Karte, N65)
Branch `claude-mapfix` (nach claude-wave1 gemergt eingearbeitet). Merge nach wave1 offen.
1. Artefakte: Ursache in `Reveal.step`: Pixel hinter einer ungeladenen Nachbar-Chunk-Zeile bekamen geratene (flache) Schattierung und ueberschrieben bekannte Pixel (Linien an Chunk-Raendern); ausserdem blieben Spalten ausserhalb des 1/16-Spaltenrasters leer (senkrechte Streifen am Rand). Fix: geratene Pixel ueberschreiben nichts und werden nachgeholt; bis zu 24 unbekannte Spalten pro Tick zusaetzlich. Nicht am Client reproduziert.
2. Zoom 2:1, 4:1, 8:1 (reine Client-Vergroesserung, `mag()`), Mausrad/Knopf haben 10 Stufen; Fusszeile zeigt "8:1".
3. Wegpunkt-Name statt Nummer auf der Karte.
4. Knopf "Koordinaten anzeigen" (links, gespeichert in config/simplemaps-client.json): Koordinaten an Wegpunkten und Spieler.
5. Ebenen-Knopf (links) mit Schaltern: geladene Chunks, Chunk-Loader, Shops (gespeichert). Server-Autoritaet `LayerService` (nur Halter der Karte, 10-Tick-Limit, max 128x128 Chunks, nur erkundete Kacheln, Config `layers`). Modul-Vertrag `framework MapLayerSources` (reines Java): SB registriert `chunk_loaders`, Simple Shops `shops` (nur Angebot-Namen + Preis + Besitzer, keine Vorraete/Einnahmen); simplemaps kennt beide nicht.
6. Kartenrahmen auf Bronze (#6F5F4B).
Ideen weitere Ebenen: Spieler/Teammitglieder-Positionen; Claims/Schutzgebiete; Todes-/Heimat-/Respawn-Punkte; Mob-Spawner/Beacon-Reichweiten.
Tests: Compile 3 Loader + Module gruen (ab8c7560b); Simple Maps fabric integration 19/19 gruen inkl. neuem GameTest `layers`.
