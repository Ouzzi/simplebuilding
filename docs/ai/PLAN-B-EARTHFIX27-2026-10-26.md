# Plan B-EARTHFIX27 (26.10.2026) – Simple Earth: Menü (WG26-3), kein Parken (WG26-1), Erde in echter Entfernung (WG26-2)

Branch `b-earthfix27` (Basis claude-wave1 2c2cfbc17), nur 26.3 Fabric. Besitzer `/root/sb-tools-b/wd25.md` Abschnitt WG26.

## Befunde
- WG26-3: Alle Bedienelemente laufen schon über das eigene `mouseClicked` (auch die Zeilen) – im Test (854×480, 1920×1177,
  GUI 2/4, Integrations-Client mit allen Modulen + JEI/Jade/Mouse Tweaks) klickt alles; beim Besitzer nur die Versionsknöpfe.
  Ursache im Code nicht auffindbar → das Menü verlässt sich nicht mehr allein auf Bildschirm-Ereignisse.
- WG26-1/2: Parken (Figur festhalten, Gleiten, Re-Entry-Teleport) und Zoom-Höhe sind laut Besitzer falsch gedacht.

## Umsetzung
1. Menü: eine gemeinsame `pressAt`-Logik (Knöpfe, Umschalter, Regler, Bildlaufleiste); zusätzlich liest das Menü jedes Bild die
   echte Maustaste und Cursorlage per GLFW (unabhängig von anderen Mods) und handelt, wenn kein Ereignis kam; Ziehen/Loslassen
   ebenso; Liste auch per Pfeil-/Bildtasten; eine Logzeile, wenn ein Klick nur über diesen Weg ankam.
2. Kein Parken: Mittelpunkt A fest, Spieler fliegt frei (Kreativ, Elytra); Lage relativ zu A = Lage über der Erde. Entfernt:
   Gleiten (OrbitMovePayload, Tastatur-Mixin), Halten, Schweben, Re-Entry-Teleport, Langsamfall, Orbit-Tempo-Optionen.
   E1-Standard 2048 (Besitzer-Beispiel), Kante ab 512.
3. Echte Entfernung (`realDistance`, Standard): Augenhöhe = Höhe über dem Meeresspiegel in echten Blöcken (Würfel in echter
   Größe); gezeichnet weiter skaliert + 32 Blöcke nach hinten geschoben (gleiches Bild wie in echter Lage, kein Far-Plane-Problem,
   immer hinter dem Spieler). Aus = alter Zoom.
4. Tests: Gametest orbit_lifecycle (frei, kein Teleport), Client: Kreativ-/Elytra-Flug bewegt Spieler und Erde gleich, Kante bei
   A + 1024 (Kachel 0 → 1, Screenshots), F5 von oben, Menüklicks (Ereignis- und Poll-Weg), Mod-Anzahl geloggt.
