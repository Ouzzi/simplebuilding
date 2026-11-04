# Plan b-storage05 (03.11.) – Interface-Form, Rohr-Stummel, Mod-Fässer (DW03C-1..4)

Basis e29a4b771 (Bündel 100). Besitzer-Wortlaut: /root/b-antworten-20261103c.md, Screenshots owner-img/20261103c/3–5.
Nur MC 26.3 Fabric.

## Ist-Stand
- Interface = `simplestorage:chest_interface` (`network/InterfaceBlock`): Platte 14x14, Modell `chest_interface_plate`
  z −1..3 (ragt 1 px in die Nachbarzelle), Kanten-Leisten `edge_e/w/u/d` (IG31-6: wächst zu festen Nachbarblöcken).
  Unter der Kiste ragt die Platte 1 px in die Kistenzelle; ihre Seitenflächen x=1/15, z=1/15 liegen dort in derselben
  Ebene wie die Kistenseiten → **Z-Fighting (DW03C-1)**.
- Montierbar (`ControllerLevel.mountable`): ChestBlock, BarrelBlock, ShulkerBoxBlock – Mod-Fässer (`simplecrucibles`
  `CrucibleBarrelBlock`, 4 Stufen) nicht.
- Vanilla-Kiste (ChestModel 26.3): Unterteil y 0..10, Deckel y 9..14 mit Scharnier hinten bei y=9, Schloss vorne
  x 7..9, y 7..11, reicht bis an die Zellkante. Fester Teil also y 0..9.
- Rohr (`pipe/ItemPipeBlock.arm`): Arm auf jeder OUT-Seite (auch ins Leere = Stummel) und auf IN-Seiten mit Container
  oder Nachbarrohr (auch wenn zwischen zwei Rohren nichts fließt). Rohre ziehen nie aus Rohren (nur push).
- Mod-Fässer: `facing` nur waagrecht (Andocken an den Tiegel), `open` existiert, wird nie gesetzt; Blockstate zeigt
  immer das geschlossene Modell. `*_barrel_top_open.png` gibt es schon (Kupfer/Verstärkt/Netherit im Modul, Enderit
  wird beim Build abgeleitet, vanilla_derived.py) – nur nie benutzt.

## Umsetzung
### DW03C-1/2 Interface (simplestorage)
1. Neue Blockstate-Eigenschaft `mount` = `full` | `chest` | `chest_front` (aus dem montierten Block berechnet):
   - `full` (Fass, Mod-Fass, Shulker): Platte 16x16 bündig (0..16), keine Kantenleisten.
   - `chest` waagrecht (hinten/seitlich): Platte Kistenbreite x 1..15, nur fester Teil y 0..9, 1 px in die Kistenzelle
     gerückt (z −1..2), Steg y 6..9 (Deckel schwingt hinten ab y=9 bis 5 px nach hinten – frei).
   - `chest_front` (vor der Kiste): wie `chest`, die Brücke in der Kistenzelle spart das Schloss aus (x 7..9, y 7..9).
   - `chest` senkrecht (unter der Kiste): Platte 14x14 in der eigenen Zelle (y 13..16), **kein Heranrücken**, keine
     Fläche in der Kistenzelle → kein Z-Fighting mehr. Oben auf der Kiste: **nicht platzierbar** (`getStateForPlacement`
     → null). Alte Interfaces oben bleiben stehen (nichts kaputt machen), gleiche Plattenform.
2. Kantenleisten nur bei `chest`/`chest_front`: (a) Doppelkiste – zur anderen Hälfte hin immer bis an die Zellkante
   (durchgehend wie die Kiste); (b) neue Config `interfaceFillsGaps` (Default **an**): zu festen Nachbarblöcken verlängern.
   Begründung Default an: IG31-6 war Besitzer-Wunsch (keine Durchblick-Lücke); ohne Nachbarblöcke bleibt die Platte
   ohnehin bündig mit der Kiste. Waagrecht nur links/rechts (oben = Deckel frei, unten schon an der Kante).
3. `randomTick` frischt alte Zustände auf (alte Welten bekommen `mount` ohne Nachbar-Update); `updateShape` auf der
   Kistenseite rechnet alles neu (Einzel→Doppelkiste, Fass getauscht).
4. Modelle/Blockstate neu (Generator `modules/simplestorage/tools/gen_interface_models.py`), Hitbox = gleiche Kästen
   ohne den Teil in der Nachbarzelle.
5. `mountable`: zusätzlich Block-Tag `c:barrels` (keine harte Abhängigkeit zu Crucibles, Modul-Unabhängigkeit);
   Mod-Fässer Rang 0 wie das Vanilla-Fass.

### DW03C-3 Rohr ohne Stummel
Arm auf Seite d nur, wenn dort etwas fließt: Seite nicht aus UND (Nachbar ist Container ≠ Rohr) ODER (Nachbar ist Rohr
und eine der beiden Seiten zeigt als Ausgang auf die andere und die andere Seite ist nicht aus). Ausgang ins Leere →
kein Arm (Richtung zeigt weiter der Kern/Pfeil, Fenster, Jade). Symmetrisch, darum genügt das vorhandene Nachbar-Update.

### DW03C-4 Mod-Fässer (simplecrucibles)
- Neue Eigenschaft `top` (alle 6 Richtungen, Default `up`) = Richtung des Deckels wie Vanilla-`facing`; `facing` bleibt
  waagrecht fürs Andocken. Warum nicht `facing` umdeuten: alte lose Fässer haben `facing=north` gespeichert und würden
  umkippen; ohne `top` lädt der Default `up` → alte Welten bleiben unverändert.
- Platzieren wie Vanilla (`getNearestLookingDirection().getOpposite()`), rotate/mirror, Upgrade behält `top`.
- `open` wie Vanilla über `ContainerOpenersCounter` (Fass-Sounds), Modell `*_barrel_open` mit `*_top_open`.
- Blockstate-Varianten (gen_resources.py): lose = Vanilla-Fass-Drehungen nach `top`, angedockt wie bisher.
- Tag `c:barrels` mit den 4 Fässern → Interface montierbar.

### Tests (Gametests)
- simplestorage: `network_interface_fits_the_chest` (Form vorne/hinten/unten, oben nicht platzierbar, Doppelkiste,
  Config aus), `network_interface_sits_flush_on_barrels` (Fass full, Mod-Fass montierbar falls geladen),
  `pipe_arms_only_where_items_flow` (kein Stummel ins Leere, kein Arm zwischen zwei parallelen Rohren, Arm bei Kiste/
  Rohr-Ausgang); bestehende Rohr-Tests an die neue Regel angepasst.
- simplecrucibles: `crucible_game_test_loose_barrel_faces_like_vanilla_and_opens` (top 6 Richtungen, open, Tag).
- Client: neue Szene `InterfaceShotsClient` (1920x1080, Vorher = Basis + Szene, Nachher = Branch) für die Vorschau.

### Doku
Wiki-Manual simplestorage + simplecrucibles EN/DE, Lang EN/DE `config.simplestorage.interfaceFillsGaps(.tooltip)`.

## Risiken
- Zustandszahl Interface ×3 (≈18k) – vertretbar, Formen werden einmal berechnet.
- Rohr-Arme hängen am Block-Entity des Nachbarrohrs; beim Platzieren ohne BE gilt der Ein-Ausgang aus `facing`.

## Verifikation
precheck --fix, ein sbt-Lauf: Compile Fabric + Module storage/crucibles, Modul- und Standalone-Gametests,
check_client_mixins, wiki --check, Client-Szene vorher/nachher.
