# N64 – Container-Margin für alle übrigen Oberflächen (15.11.)

Auftrag (Koordinator A, Regel vom Besitzer abgenommen in N62): alle übrigen UI-Elemente nach `docs/ai/PRINZIPIEN.md` A7
„Container-Margin“ prüfen und verbessern. Branch `b-ui64` auf `b-n62`. Regel kurz: jeder Kasten hat zwischen heller
Innenlinie (Rahmen 5, unten 7 mit Schatten) und äußerstem Slot/Element auf allen vier Seiten genau `UiBoxes.MARGIN` = 3 px
Fläche; Titel hat eigenen Abstand; Teiler ≥ 2; fehlt Platz, rücken die Spieler-Slots nur im Client tiefer, das Bild wächst.
Schmale Inhalte (Trichter/Werfer-Fall): Kasten so breit wie das Inventar, Inhalt bleibt mittig (Besitzer N62).

## Ist-Zustand (erhoben)
- N62 umgestellt: Vanilla-Lager (Kiste/Fass/Endertruhe 9xN, Shulker, Trichter, Werfer/Spender) über
  `StyledScreens.applyMargin` + `StorageMarginMixin` + `BoxLayout.margin`; 8 eigene Screens über `UiMargin`
  (simplestorage, simplemachines, simplemusic).
- Noch alte Regel `BoxLayout.compute` (SIDE 8, AIR/NO_SHADOW/SEAM): alle Stationen (Werkbank, Öfen, Braustand,
  Zaubertisch, Leuchtfeuer, Amboss, Schleifstein, Steinsäge/Formsäge, Webstuhl, Kartentisch, Schmiedetisch,
  Spielerinventar), Crafter, Reittiere (Pferd/Esel/Lama, Nautilus). Gemessen (Code-Maße, `n64screens.py`):
  rechts 2 (Kasten an der Bildbreite 176 geklemmt), Inventar oben 0 und unter der Hotbar 0, Containerkasten unten 2
  (NO_SHADOW) bzw. Fugen-Varianten mit Inventar-Oberkante 1.
- Händler: ein Kasten mit senkrechter Fuge, Angebotsliste (Vanilla-Knöpfe x 5, Scrollbalken bis x 101) neben dem
  Inventar (x 108) – die Regel ist bei Vanilla-Positionen nicht erfüllbar (links 0, Fuge/Inventar 2..5).
- Eigene Mod-Screens mit alter „SIDE/GAP“-Rechnung: siehe Tabelle unten (Agent-Erhebung + Code).

## Plan
1. **Stationen (simpleinterfaces), ein Baustein:** alle gestylten Vanilla-Screens außer dem Händler nutzen
   `BoxLayout.margin`; die Elemente (großer Ergebnis-Slot, Felder, Pfeile, Vorschauen, Leuchtfeuer-Kraftfelder) kommen
   statisch je Stil-ID (`StationStyles.ELEMENTS`/`RESULT_SLOT`, neu `WorkStyles.ELEMENTS`/`RESULT_SLOT`, Crafter/Reittier aus
   `StorageDecors`), damit `init` (vor dem Zentrieren) und das Zeichnen dieselbe Rechnung haben.
   - `StyledScreens.applyMargin` bei jedem `init` idempotent: Spieler-Slots über `SlotPositionAccessor` auf
     Original-y + Shift (Original je Slot gemerkt – das Spielerinventar-Menü lebt dauerhaft, darf nicht doppelt
     schieben), Höhe aus der Vanilla-Höhe (beim ersten `init` gemerkt) neu.
   - `BoxLayout.margin`: Inventar-Kasten eigene Breite (wie `UiMargin`; bei Lager-Screens identisch, beim Leuchtfeuer
     schmaler als der Kraftfeld-Kasten).
   - Kartentisch: Spieler-Verschiebung `CARTO_EXTRA` entfällt (rechnet jetzt die Regel), `CartographyHeightMixin` entfällt.
   - Leuchtfeuer: kein „ganze Breite“-Kasten mehr, die Kraftfelder zählen als Elemente (links 1 px Versatz, Kasten 1..230).
   - Schmiedetisch: Vorschaufeld 1 px schmaler (47 statt 48, sonst 2 px über die Bildbreite).
   - Spielerinventar: Ergebnis-Slot 154→151, kleiner Pfeil 135→134 (sonst rechts nur 1 px; A12 Eingriff, Client-Menü,
     wie WD25-9/10 `SLOT_MOVES`).
   - Rezeptbuch-Knopf (Werkbank, Öfen, Spielerinventar): Vanilla setzt ihn relativ zu `height/2`; im höheren Bild an
     der Vanilla-Bildstelle halten (Mixin auf `getRecipeBookButtonPosition`), Werkbank-Knopf x 5→8 (3 px zur Linie).
   - Händler: bleibt (Besitzerfrage, A12: Vanilla-Knöpfe verschieben).
2. **Eigene Mod-Screens:** `UiMargin` (simplelib) wie die 8 N62-Screens; je Modul ein kleiner Geometrie-Helfer, wo es
   mehrere Screens gibt; Menü/Server-Positionen bleiben.
3. **Screens ohne Slots** (Bücher, Konfig, Panels): nur auflisten; analog angepasst nur bei klarem Kasten-mit-Inhalt.
4. **Tests:** Gametest `station_margins` (simpleinterfaces, alle Stationen aus denselben statischen Elementen, alle
   Seiten = 3, Teiler ≥ 2, Shift/Höhe), je Mod-Modul `screen_margins` nach Muster N62; Fabric-Adapter +
   NeoForge/Forge-Fälle; `stationLayouts`/`workLayouts` bleiben (prüfen `compute`).
5. **Vorschau** `/root/sb-tools-b/previews/n64.html` (+ `n64/`), vorher/nachher nur geänderte Screens, Maße im Bild,
   A12-Markierung bei Vanilla-Slot-Verschiebungen, Antwortcode N64, Banner „Entwurf aus Code-Maßen“.
6. **Prüfung:** sb-test einzeln: `module-simpleinterfaces-fabric-263`, danach die angefassten Module. Keine Client-/
   Probe-Läufe (macht Session B).

## Risiken
- Höheres Bild bei Stationen (bis +18 px): Rezeptbuch steht weiter mittig zum Bildschirm (nicht zum Bild); Knopf
  korrigiert. Andere Mods mit festen Offsets im Inventar-Screen (Seitenpanels liegen außerhalb des Bilds, zählen nicht).
- Spielerinventar-Menü dauerhaft: Slot-Verschiebung strikt idempotent (Original-y-Map), Kreativ-Inventar nutzt eigene
  Wrapper-Positionen.
- Kein Client-Lauf hier: Pixel-Nachzählung im Spiel durch Session B (Szenario siehe Abschnitt Prüfung).

## Maße (Code-Maße, `previews/n64/measures-vorher.json` / `measures-nachher.json`, Pixel-Gegenprobe `measures-bilder.json`)
Werkzeug: `/root/sb-tools-b/ig57u/mock/n64lay.py` (Ports `BoxLayout.compute`/`margin`, `UiMargin.of`), `n64screens.py` (alle Screens,
`python3 -I n64screens.py OUT.json`), `render_n64.py` (Bilder + Pixelzählung, stimmt mit den Code-Maßen überein), `html_n64.py`.
l/r/o/u = Fläche zwischen heller Innenlinie und äußerstem Slot/Element; T = Titel; Container links/rechts gegen das Äußerste
aus Inhalt und Inventarspalten (Besitzer N62: mittiger Inhalt, Kasten so breit wie das Inventar).

| Screen | Gruppe | vorher Container l/r/o/u | vorher Inventar | Shift/Höhe vorher | nachher Container | nachher Inventar | Shift/Höhe nachher |
|---|---|---|---|---|---|---|---|
| Crafting Table (Werkbank) | simpleinterfaces/work | 3/2/T/2 (NO_SHADOW) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 6/175 |
| Furnace | simpleinterfaces/work | 3/2/T/2 (NO_SHADOW) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 6/175 |
| Blast Furnace | simpleinterfaces/work | 3/2/T/2 (NO_SHADOW) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 6/175 |
| Smoker | simpleinterfaces/work | 3/2/T/2 (NO_SHADOW) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 6/175 |
| Brewing Stand | simpleinterfaces/work | 3/2/T/6 (SEAM) | 3/2/1/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 11/180 |
| Enchanting Table | simpleinterfaces/work | 3/2/T/2 (NO_SHADOW) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 8/177 |
| Beacon | simpleinterfaces/work | 4/3/3/5 (SEAM) | 31/28/1/0 | 0/219 | 3/3/3/3 | 3/3/3/3 | 12/234 |
| Anvil | simpleinterfaces/station | 3/2/T/2 (TWO_BOXES) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 4/173 |
| Grindstone | simpleinterfaces/station | 3/2/T/9 (TWO_BOXES) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 0/169 |
| Stonecutter (+ Shapecutter) | simpleinterfaces/station | 3/2/T/2 (NO_SHADOW) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 6/175 |
| Loom | simpleinterfaces/station | 3/2/T/3 (SEAM) | 3/2/1/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 14/183 |
| Smithing Table | simpleinterfaces/station | 3/1/T/4 (SEAM) | 3/2/1/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 13/182 |
| Cartography Table | simpleinterfaces/station | 3/2/T/2 (NO_SHADOW) | 3/2/0/0 | 0/182 | 3/3/T/3 | 3/3/3/3 | 22/191 |
| Player Inventory | simpleinterfaces/station | 3/0/3/2 (SEAM) | 3/2/1/0 | 0/166 | 3/3/3/3 | 3/3/3/3 | 15/184 |
| Villager Trading (Merchant) | simpleinterfaces/station | 0/2/T/2 (MERCHANT) | 0/-3/-2/-7 | 0/166 | unverändert (offen) | | |
| Crafter | simpleinterfaces/storage | 3/2/T/2 (NO_SHADOW) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 6/175 |
| Horse (no chest) | simpleinterfaces/storage | 3/2/T/2 (NO_SHADOW) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 7/176 |
| Donkey/Mule (chest) | simpleinterfaces/storage | 3/2/T/2 (NO_SHADOW) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 7/176 |
| Llama (strength 5) | simpleinterfaces/storage | 3/2/T/2 (NO_SHADOW) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 7/176 |
| Nautilus | simpleinterfaces/storage | 3/2/T/2 (NO_SHADOW) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 7/176 |
| Chest 9x3 (N62) | simpleinterfaces/storage | 3/3/T/3 (TWO_BOXES) | 3/3/3/3 | 6/176 | 3/3/T/3 | 3/3/3/3 | 6/176 |
| Large Chest 9x6 (N62) | simpleinterfaces/storage | 3/3/T/3 (TWO_BOXES) | 3/3/3/3 | 6/230 | 3/3/T/3 | 3/3/3/3 | 6/230 |
| Fletching Table (simplearrows) | mods | 3/2/T/15 (TWO_BOXES) | 3/2/0/0 | 0/166 | 3/3/T/3 | 3/3/3/3 | 0/169 |
| Enchanter (simpleenchanting) | mods | 2/1/T/0 (TWO_BOXES) | 3/2/0/0 | 0/202 | 3/3/T/3 | 3/3/3/3 | 6/211 |
| Foundry (simplefoundry) | mods | 3/2/T/0 (TWO_BOXES) | 3/2/11/0 | 0/208 | 3/3/T/3 | 3/3/3/3 | 0/211 |
| Death Vault (simpletools) | mods | 3/2/T/0 (TWO_BOXES) | 3/2/0/0 | 0/222 | 3/3/T/3 | 3/3/3/3 | 6/230 |
| Questionary (simplequestionary) | mods | 3/2/T/– (TWO_BOXES) | 3/2/T/1 | 0/238 | 3/3/T/– | 3/3/T/3 | 0/238 |
| Crucible IRON ohne Fass (simplecrucibles) | mods | 3/2/T/– (TWO_BOXES) | 3/2/T/1 | 0/173 | 3/3/T/– | 3/3/T/3 | 0/175 |
| Backpack BASIC (simplestorage) | mods | 3/0/T/2 (SEAM) | 3/2/19/0 | 0/184 | unverändert (offen) | | |
| Shop Storage (simpleshops) | mods | 3/2/T/12 (SEAM) | 3/2/1/0 | 0/168 | unverändert (offen) | | |
| Shop Stock (simpleshops) | mods | 3/2/T/11 (SEAM) | 3/2/1/0 | 0/133 | unverändert (offen) | | |
| Shop Buyer (simpleshops) | mods | 3/2/T/5 (SEAM) | 3/2/1/0 | 0/192 | unverändert (offen) | | |
| Invsee INVENTORY (simpletweaks) | mods | 3/2/T/6 (SEAM) | 3/2/1/1 | 0/207 | unverändert (offen) | | |
| Hufpanel (simpleriding) | mods | 2/–/1/1 (Box -43,12,48x49) | – | – | 3/–/3/3 (Box -44,10,49x53) | – | – |

Nicht in der Tabelle, weil schon N62 (unverändert): Kiste/Fass/Endertruhe 9x1..9x6, Shulker, Trichter, Werfer/Spender, die 8 eigenen
N62-Screens (Mod-Truhen/Fässer, Storage Crafting, Controller, Rohr, Mod-Trichter, Autonomous Crafter, Auto Smither, Musikpresse).
Laden-Varianten Owner (einzeln/kombiniert) und TradeTerminal haben dieselbe Fugen-Konstruktion wie Storage/Stock/Buyer (offen);
Invsee ENDER/DEATHS wie INVENTORY; Rucksack REINFORCED/NETHERITE/ENDERITE wie BASIC (+ Zusatzspalten neben dem Inventar).
Tiegel: alle Stufen/Layouts gleich behandelt (Kasten +1 px rechts), Tabelle zeigt IRON ohne Fass.

### Screens ohne Slots (nur gelistet, A7 analog nicht erzwungen)
Bücher/Panels/Konfig mit `UiBoxes.box`: simplemaps Wayfinder/PictureMap/WaypointDialog, simplemodels ModelEditor/ModelBrowser/
ModelFile/TexturePicker (flache Vollbild-Rahmen ohne Schatten), simplevisuals ModelBrowser, simplemusic SheetMusic/Burn, simpleearth
EarthScreen, simplechallenges SelectChallenge/StartAreaEditor, simpletweaks Deed/DeedCard, simplestats ItemStats, simplerespawn
Downed, simplebuildingtools Octant/BuildingWand/Blueprint (ModScreenStyle), simplelib DevVariantsPanel, Todestresor-Legende.
Es sind Listen/Formular-/Vollbild-Layouts (Inhalt aus Widgets mit eigener Rechnung), kein „Kasten mit Slots“ – keine Änderung.
Altlast ohne UiBoxes-Stil (Vanilla-PNG): common/src TieredChest/AutoSmither/NetheriteHopper/AutonomousCrafter/Backpack-Screen (SimpleBuilding-Kern).

## Umsetzung (Stand Commit, Abweichungen)
- Wie geplant: Stationen, Fletching, Enchanter, Foundry, Todestresor, Fragebogen, Tiegel, Hufpanel.
- Abweichung: Fragebogen/Tiegel ohne `UiMargin` (eigene Spalten-/Mehrkasten-Layouts): nur Breite +1 und Inventar-Höhe 102;
  Inventar-Label bleibt dort Titel des Inventar-Kastens. Foundry: Inventar-Label entfällt (Kasten-Oberkante läge darauf).
- Offen (Besitzerfrage N64-F): Händler, Rucksack, Läden (5 Screens), Invsee, Formsäge-Ersatzstil ohne SimpleInterfaces,
  Titel-Abstand Steinsäge/Braustand.

## Prüfung
- sb-test (Fabric 26.3): simpleinterfaces 53/53 grün (7d853e9ac1); simplearrows 32, simpleenchanting 29, simplefoundry 21, simpletools 71,
  simplequestionary 15, simplecrucibles 69, simpleriding 46 – 283/283 grün. NeoForge/Forge nicht gelaufen.
- Client-Szenario: `modules/simpleinterfaces/tools/uiprobe_n64.py` (`--only simpleinterfaces/n64`).
- Client-Pixelzählung (Session B, nicht hier): Szenario `simpleinterfaces/n62` um die Stationen erweitern bzw. je Station öffnen,
  `measure_shots.py` zählen lassen; Soll = 3 überall außer Titel-Oberkante (Liste `previews/n64/measures-nachher.json`).
