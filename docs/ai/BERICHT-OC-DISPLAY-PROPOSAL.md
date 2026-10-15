# Bericht OC-DISPLAY-PROPOSAL (Schritt 1, nur Text)
- Datei: `docs/ai/VORSCHLAG-ITEM-DISPLAY-3D-2026-10-15.md` (Tabellen heute → Vorschlag → Änderung je Art, Aufwand, Risiko, 10 Fragen an den Besitzer). Kein Code, keine Bilder, keine Textur angefasst.
- Recherche Items Displayed aus Modrinth/GitHub (Modell-JSON, Block-Klassen, Wiki); nicht im Spiel gesehen. Lizenz GPL-3.0: nichts übernommen, nur Maße als Vorbild.
- Ist-Befunde: Scherben heute nicht platzierbar; Vanilla-Platten nicht im Tag; Mod-Platten sind eine Schrägansicht (15x10-Ellipse); Vorlagen liegen auch im Modul simpletrims (eigene Kopie von Renderer/PlacedPlate).
- Silhouetten aus dem 26.3-Client-Jar gezählt: alle 23 Scherben gleich, 22 Platten fast gleich, 18 Vorlagen 3 Formen, daher ein Generator je Art genügt.
- Empfehlung: Silhouetten-Extrusion als Blockmodell mit der vorhandenen Item-Textur (keine neuen Pixel); nur die Schallplatte braucht eine neue runde Draufsicht-Textur (Build-abgeleitet, nicht eingecheckt).
- Nicht getestet/offen: Lichtwirkung (Blockschattierung vs. flach) ist mutmaßlich, Beleg erst in Schritt 2; Vanilla-Disc-/Scherben-Platzierbarkeit und Modulgrenze sind Entscheidungen für den Besitzer.
- Nächster Schritt: Besitzer-Freigabe, dann Schritt 2 (Vorher/Nachher-Bilder).
