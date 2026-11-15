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

## Maße vorher (Code-Maße, `previews/n64/measures-vorher.json`)
(wird nach der Erhebung ergänzt)
