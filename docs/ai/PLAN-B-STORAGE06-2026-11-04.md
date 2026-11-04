# Plan b-storage06 (04.11.) – Interface-Ecke, Controller ohne eigenes Inventar (DW04C-1/2)

Basis 3a7907e06 (Bündel 106, enthält b-storage05). Besitzer-Wortlaut: /root/b-antworten-20261104c.md, Bild owner-img/20261104c/6.png.
Nur MC 26.3 Fabric.

## Ist-Stand
- **DW04C-1:** Interface unter (oder alt: auf) einer Truhe = `FLAT_PLATE` 14x14 + Kantenleisten E/W/U/D (je 1 px, x/y 1..15).
  Sind zwei benachbarte Leisten an (z. B. Wand östlich und südlich), bleibt die 1x1-px-Ecke dazwischen offen → durchsichtiger
  Punkt an der Kistenecke (Bild 6). Seitlich (`SIDE_PLATE`) gibt es nur E/W, keine Ecken.
- **DW04C-2:** `ControllerChestBlockEntity` ist eine `TieredChestBlockEntity` mit eigenen Plätzen. `ChestNetwork.Storage` setzt den
  Controller als ersten Container in die Liste; `insert` füllt erst gleiche Stapel, dann **leere Plätze ab dem Controller** →
  wieder eingelegte Items landen im Controller (Fenster zeigt sie zwar, aber in keiner Truhe). Trichter/Rohre (beide über
  `HopperBlockEntity.getContainerAt` + `addItem`) füllen ebenfalls die Controller-Plätze.

## Umsetzung
### DW04C-1
1. Neue Eck-Kästen `FLAT_CORNERS` (EU, ED, WU, WD; z 0..3) in `InterfaceBlock` und `gen_interface_models.py`; Form und Modell
   bekommen die Ecke genau dann, wenn beide angrenzenden Leisten an sind (Blockstate-Multipart mit zwei `edge_*: true`).
   Keine neue Eigenschaft (Zustandszahl bleibt). Ecke liegt in der eigenen Zelle neben den Leisten, keine deckungsgleiche
   Fläche mit Truhe/Nachbar → kein neues Z-Fighting.

### DW04C-2
1. `ChestNetwork.Storage`: Controller-Plätze nur noch **Quelle** (Liste + Entnehmen, Altbestand), nie Ziel. `insert` drei
   Durchgänge: (a) auf gleiche Stapel, (b) leere Plätze in Truhen, die die Sorte schon enthalten, (c) übrige leere Plätze in
   Netz-Reihenfolge. Neu `fits(stack)` (passt ganz hinein?) für die Automatik. Spieler optional (Automatik: wie Vanilla-
   Trichter ohne Schloss-Prüfung).
2. Controller-Block implementiert `WorldlyContainerHolder` (wie der Komposter): Trichter/Rohre sehen einen Einlass-Behälter
   (1 Platz, nimmt nur, wenn das Netz OK ist und der ganze Stapel passt; gibt nichts heraus) und verteilen direkt ins Netz.
   Rest (sollte nie vorkommen) fällt als Item heraus statt zu verschwinden. Die Block-Entity sagt `canPlaceItem = false`.
3. Alte Welten: `ControllerChestBlockEntity.moveOwnItemsIntoNetwork()` beim Öffnen des Fensters und im Zufallstick: Altbestand
   ins Netz; was nicht passt, **bleibt** im Controller (im Fenster sichtbar und entnehmbar, beim Abbauen fällt es heraus).
   Abweichung vom Auftrag „bzw. droppen“: kein Droppen im Zufallstick, weil unbeobachtet gedroppte Items nach 5 min
   verschwinden – so geht garantiert nichts verloren.
4. Fenster: Klick mit Item, Shift-Klick, Ziehen gehen alle über `insert` → nie in den Controller; passt nichts, bleibt das Item
   am Cursor/im Inventar (vorhandenes Verhalten). Ziehen über die Listenzellen fügt dort nichts hinzu (Zellen `mayPlace=false`),
   das Item bleibt beim Spieler.
5. Doku: Javadoc, Wiki-Manual EN/DE („Controller hat kein eigenes Inventar“, Trichter/Rohre).

### Tests (Gametests, Katalog `StorageTests.ALL` + `ModuleGameTest`)
- `network_store_prefers_the_chest_with_the_same_item`: Einlegen per Klick/Shift-Klick geht in die Truhe mit derselben Sorte
  (auch wenn deren Stapel voll ist, nächster freier Platz dort), Controller bleibt leer.
- `network_full_keeps_the_item_with_the_player`: volles Netz → Cursor/Inventar behalten das Item, Controller leer.
- `network_controller_hopper_feeds_the_network`: Trichter über dem Controller → Item in der Truhe, Controller leer; volles Netz
  → Trichter behält es.
- `network_old_controller_items_move_into_the_network`: Altbestand im Controller → nach Öffnen in Truhen, Überhang bleibt.
- `network_interface_fills_the_corner`: unter einer Truhe mit Wänden O+S → Ecke in der Form; nur O → keine Ecke.
- Bestehende Netz-Tests weiter grün (barrels…, window_lists…).

## Verifikation
precheck --fix; ein sbt-Lauf: Compile 26.3 Fabric + simplestorage, module-simplestorage-fabric-263 + standalone,
check_client_mixins, wiki --check; Nachweisbild Ecke vorher/nachher per Client-Szene (InterfaceShotsClient, xvfb) →
abnahme-b/bilder/STORAGE06.jpg.
