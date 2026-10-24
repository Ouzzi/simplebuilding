# Plan B WA24-3 / WA24-4 (24.10.)

Ist: `butter_carpet` (ButterCarpetBlock, CarpetBlock, 1 px, stepOn -> Slippery). Gefaerbte Koecher: `quiver_dyed_textures` in tools/textures/generate_textures.py, Federn (vier Grautoene) liegen ungetoent in der Beschlag-Ebene.

WA24-3: Properties `noCollision()` + `speedFactor(0.75)`; Kontakt jetzt ueber `entityInside` (stepOn greift ohne Kollision nicht). Spieler sinkt die 1 px des Teppichs ein, ist verlangsamt, Slippery wie bisher. Gametest: slipperyFromButter erweitert (entityInside, keine Kollision, Speed-Faktor).
WA24-4: `quiver_feather_outline`: dunkler Umriss (Riemenfarbe der Stufe) auf freien Nachbarpixeln der Federn, nur Overlay-Texturen der vier Stufen (+ Modul-Kopie simplestorage, mc1_21_11-Baum nur Generator-Konsistenz). Vorschau /root/sb-tools-b/previews/wa24-4-quiver.html.
Verifikation: Compile 26.3 Fabric, Tests module-simplesandwiches(-standalone)-fabric-263, module-simplestorage(-standalone), Generatoren, check.
