# Plan B-EARTHFIX26 (25.10.2026) – Simple Earth: Menü (WF25-2), Elytra-Orbit (WF25-1), Erde vor dem Spieler (WF25-3)

Branch `b-earthfix26` (Basis claude-wave1 d59199fa4), nur 26.3 Fabric. Besitzer hat master d59199fa4 getestet.

## Befunde
- WF25-3: Die Erde wird skaliert in den Nahbereich gezeichnet; Abstand Kamera→Oberfläche = h·maxDist/(h+5,5). Mit 20+ Chunks
  (maxDist 160) und F5 (Kamera 4 Blöcke hinter dem Spieler) ist das unter h ≈ 0,14 kleiner als 4 → bei y ≈ 2780 liegt die
  Oberfläche zwischen Kamera und Spieler (Besitzer: 2777/2778).
- WF25-1: Geparkt wurde nur die Waagrechte gehalten; ohne Kreativflug (Elytra) fiel man durch die Schwerkraft unter die
  Umschalthöhe → Orbit vorbei. Der alte Test maß nur Kreativflug.
- WF25-2: Versionsknöpfe gehen, Rest nicht: Regler-Klickfläche war nur das 12-px-Band um die Spur; Test lief nur bei 854×480/GUI 2.

## Umsetzung
1. Jeder Erdpunkt wird entlang der Sichtlinie 32 Blöcke nach hinten geschoben (gleiches Bild, nie näher als 32), Himmelskasten
   dahinter, Mond hinter die Erde; Messwert `EarthRenderer.lastNearest`.
2. Geparkt: Server schaltet Schwerkraft aus und klappt die Elytra ein (kein Flug-Kick, kein Verschleiß); ohne Kreativflug
   Sprung/Schleichen = hoch/runter (Sprint ×4). Re-Entry stellt die Schwerkraft wieder her.
3. Menü: ganze Zeilenhöhe klickbar; Client-Test zusätzlich bei 1920×1177 mit GUI-Skala auto (4) und 2, beim Laufen.
4. Tests: Elytra-Gleiten (geparkt, Strecke, Höhe stabil), F5 von oben bei y 2700 (Screenshot + Abstand), Gametest Schwerkraft.
