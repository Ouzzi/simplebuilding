# Plan b-sortprobe27 (11.11.) – Echte-Maus-Probe Sortieren im Kreativ-Inventar (prio27 #8, WE25-3 Rest)

Quelle: /root/sb-tools-b/prio27.md #8, Queue Z.92 (b-sort26: „offen echte-Maus-Lauf Kreativ“). Basis hub/b-bundle180-gen 7def212d6.

## Ist-Stand
- b-sort26 (f87be64c7, in Bündel 177): `SortClient.click` sortiert im Kreativmodus nur im Überlebensinventar-Tab (Hauptreihen 9..35,
  `ContainerSort.creativeInventoryIndex`), Item-Tabs/Hotbar/Truhen behalten Vanillas Mittelklick (CLONE = voller Stapel auf den Cursor).
- `modules/simpleinterfaces/tools/sort_probe.py` 13/13 (08.11.), Schritt (a) prüfte nur „Kreativ: kein Absturz“ (alter Stand: Kreativ tut nichts).
- `SortProbe` (Client, inert ohne Anforderungsdatei) meldet Slots/Inventar, aber nicht Tab, Tab-Position oder Cursor-Stapel.

## Umsetzung
1. `SortProbe.writeStatus`: zusätzlich `carried` (Stapel am Cursor), bei `CreativeModeInventoryScreen` `inventoryTab` (isInventoryOpen) und
   Mitte des Überlebensinventar-Tabs (`getTabX/getTabY` per Reflection, Fenster-Pixel). Nur Probe-Code, im Spiel inert.
2. `sort_probe.py` Schritt (a) neu: Kreativ, E → Item-Tab: echter Mittelklick auf ein Item klont vollen Stapel, Inventar unverändert (a1);
   echter Linksklick auf den Inventar-Tab (a2); Mittelklick auf Hauptreihe sortiert wie `ContainerSort.sorted`, Hotbar bleibt, Cursor leer (a3);
   Mittelklick auf Hotbar im Inventar-Tab klont (Fackel x64), Inventar unverändert (a4). (b)–(d) Überleben/Truhe unverändert.

## Test
Ein sb-test-Lauf nur bei leerem sb-test, NACH dem Lauf von b-mapsmenu27 (nie parallel): `xvfb-run … sort_probe.py --out /var/tmp/sb/sortprobe27`.
Rot wegen Probe-Logik → Probe fixen, höchstens ein zweiter Lauf. Rot wegen Mod → Fix in SortClient + gezielter sbt.
Ergebnis + Bilder: /root/sb-tools-b/previews/b-sortprobe27-preview.html.
