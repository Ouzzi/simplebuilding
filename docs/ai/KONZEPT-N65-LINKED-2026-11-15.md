# KONZEPT N65 – Simple Containers: verknüpftes Öffnen ausfeilen (2026-11-15)

Auftrag N65 (Koordinator A, **nur Vorschau**, Antwortcode N65). Branch `b-cont65` auf `b-n62`. Keine Umsetzung in diesem Schritt.
Vorschau: `/root/sb-tools-b/previews/n65.html` (+ Bilder `previews/n65/`), Generator `/root/sb-tools-b/ig57u/mock/render_n65.py`
(Bilder, Maße, gezählte Ränder → `previews/n65/n65.json`) und `html_n65.py` (Seite). Alle Bilder: Entwurf aus Code-Maßen
(Nachbau von `UiBoxes`, Slot-Plätze der 26.3-Menüs, N62-Regel), Icons/Schrift Platzhalter, keine Mojang-Pixel.

## 1 Ist-Zustand (master = b-n62, Modul `modules/simpleinterfaces`)

Server (`linked/LinkedContainers.java`):
- **Merken:** Schleich-Rechtsklick, Haupthand leer, Ziel ist `BaseContainerBlockEntity` (Doppelkiste ganz, `ChestBlock.getContainer`), höchstens
  108 Slots (`MAX_SLOTS`). Rechte: `InteractionGuard.allow/permission`, `canOpen`. **Genau eine Merkung je Spieler** (`Map<Player, Mark> MARKS`);
  gleicher Behälter erneut = Ende, anderer = umgemerkt.
- **Rückmeldung:** `AMETHYST_BLOCK_CHIME` (Tonhöhe 1,5 Merken / 0,7 Ende); alle 10 Ticks ein lila `DustParticleOptions(0xB38EF3, 0.5)` an der
  Hand, nur an den Spieler selbst (`handParticle`).
- **Ende:** jeden Tick `valid()` (Schalter, Dimension, Chunk geladen, Reichweite `linkedContainerRange` 64 / 8–128, Block-Entity), alle 20 Ticks
  Rechte, nach Besuch des gemerkten Behälters selbst (`leftMarked`), Escape (`LinkedCancelPayload`), Logout/Respawn (WeakHashMap am Spielerobjekt).
- **Anhängen:** `ServerPlayer#initMenu` (`ServerPlayerLinkedMixin`) hängt an jedes geöffnete Menü die Slots des gemerkten Behälters **hinten** an
  (`LinkedSlot`, Indizes des Menüs bleiben); ins eigene `inventoryMenu` per `syncInventory`. Nutzlast `LinkedOpenPayload(containerId, size, title, icon)`,
  `containerId = HUD` für das Banner. Shift-Klick-Regeln in `interceptClick` (gemerkt → Station → Inventar; Station-Lagerslot → gemerkt zuerst; Inventar →
  Station, sonst gemerkt). Kreativ ausgenommen.

Client:
- **HUD** (`client/LinkedHud.java`, `HudLinkedMixin`): **Banner rechts**, 10 px vom Rand, 40 px über der Bildschirmmitte, Item-Icon + Blockname
  (+ eigener Name), dunkler Bronze-Kasten 78 %. Nur ohne offenen Screen. Escape in der Welt beendet die Merkung (`KeyboardLinkedMixin`).
- **Panel** (`client/LinkedPanel.java`, `ContainerScreenLinkedMixin`): Bronze-Kasten `0xFF6F5F4B`, Titel „Vorgemerkt: %s“, 176 breit,
  `HEADER 17`, `FOOTER 7`; scrollt per Mausrad (2-px-Balken bei `WIDTH-7`).
  - **Inventar (E):** Panel **über** dem Inventar; reicht der Platz oben nicht, **daneben** (wenn dort mehr Reihen passen), sonst oben mit Scrollen.
  - **Jedes andere GUI:** *Replace-Modus* – Panel ab 14 px über der ersten Inventarreihe, **alle Spieler-Slots (auch Hotbar) auf y = −10000**,
    Inventar-Titel weg. Das zweite GUI bleibt Vanilla-mittig.
- **N62-Verstoß des Panels:** rechts 2 px (176 statt 177), unten 0 px Fläche unter der letzten Reihe (FOOTER 7 = nur Rahmen), Scrollbalken liegt im
  Rand. Muss mit der Regel A7 „Container-Margin“ ohnehin angepasst werden (Punkt N65-4).

Weitere HUD-Belegung (Kollisionsprüfung oben links): Vanilla zeichnet im Spiel oben links nichts (F3 ausgenommen; Chat unten links, Bossleiste mittig 182 px,
Toasts und Effekte oben rechts). SimpleLib `GadgetHud` (Oktant, Stab, Messgerät; `DEFAULTS = (true, 0 %, 50 %, 100 %)`, `STACK_PITCH 76`, `MARGIN 10`)
steht links mittig und stapelt sich um die Mitte – mit 3 Kästen bei 720p·3 reicht der oberste bis y ≈ 20; mit vom Spieler nach oben geschobenem Anker
liegt er bei y = 10. Simple Visuals Aufsammel-Hinweis: rechts unten (`pickupNotifierSide = RIGHT`, Offset 10/10). Respawn/Challenges: Bildmitte.

## 2 Rahmen und Höhen (Rechengrundlage)

| Fenster · Skala | GUI-Fläche | nutzbar (2 px Rand je Seite wie `LinkedPanel`) |
|---|---|---|
| 1920×1080 · 2 | 960×540 | 956×536 |
| 1920×1080 · 3 | 640×360 | 636×356 |
| 1920×1080 · 4 (**Auto**) | 480×270 | 476×266 |
| 1280×720 · 2 | 640×360 | 636×356 |
| 1280×720 · 3 (**Auto**) | 426×240 | 422×236 |

Vanilla-„Auto“: größte Skala mit ≥ 320×240 GUI-px → 1080p = 4, 720p = 3. Diese beiden Fälle sind die engen.
Kastenmaße mit N62-Regel (3 px Fläche rundum, Rahmen 5 / unten 7, Titel eigener Abstand): 9-Slot-Kasten 177 breit; Behälter `27 + 18·Reihen`
hoch (Kiste 81, Doppelkiste 135); Spielerinventar 93; Hotbar-Leiste 35; Ofen/Werkbank-Teil 80, Amboss-Teil 74, Leuchtfeuer-Teil 137 (230 breit),
Inventarbild E 169; Trenner 2. Alle Ränder der Slot-Kästen im Generator nachgezählt: überall 3 (`n65.json → margins`).

## 3 Punkt 1 – HUD-Symbol

Gespiegelt zu Vanillas Effekt-Kacheln (oben rechts, 24×24, Abstand 25, Reihe y = 1): **oben links** 24×24-Kacheln, x = 1 + 25·i, y = 1, Icon 16×16
(`graphics.item` des Block-Items) bei +4/+4, lila 2×2-Punkt unten rechts in der Partikel-Farbe. Ersetzt das Banner rechts; Klang und Partikel bleiben.

| Variante | Beschreibung | + | − |
|---|---|---|---|
| **A Kachelreihe** (Empfehlung) | je gemerktem Behälter eine Kachel nebeneinander | lesbar wie Effekte, eindeutig welcher Behälter | wächst mit Anzahl (max. 3 → 75 px) |
| B eine Kachel + Zahl | zuletzt gemerkter + Item-Zahl | immer 24 px | nicht sichtbar, welche anderen |
| C Kachel + Name 3 s | wie A, nach dem Merken kurz der Name | Name ohne Dauertext | Text auf dem Bildschirm (A7: nur kurz) |

Stil: **dunkel** wie die Gadget-HUD-Kästen (Empfehlung, passt zur Mod-Familie) oder hell (Vanilla-Effekt-artig). Optional A14: Kachel blass ab 75 % der Reichweite
(Spieler sieht, dass die Merkung gleich endet).
**A12:** Nutzen – jederzeit sichtbar, was gemerkt ist. Ändert – nur HUD in einer von Vanilla freien Ecke. Eingriff – gering, clientseitig, Client-Schalter.

**Kollision:** Gadget-HUD (3 Kästen bzw. Anker oben). Lösung: SimpleLib erhält eine winzige Reservierung (`HudReserve.topLeft(int px)` o. ä., Wert 0 ohne
Merkung, sonst 26); `GadgetHud.origin` hält dann `y ≥ reserve + 2`. Ohne SimpleLib-Änderung alternativ: die Kacheln rücken unter einen Gadget-Kasten,
der oben links steht (schlechter: springt).

**Mehrfach-Merken (N65-1M, Besitzer-Entscheid):** heute 1. Vorschlag bis 3 (`linkedContainerMax`, Server, 1–5, Standard 3 oder 1).
A12: Nutzen – zwischen mehreren Lagern sortieren; Ändert – mehr Fernzugriff (Reichweite bleibt 64); Eingriff – mittel (Standard 1 wäre Vanilla-näher).

## 4 Punkt 2 – Anordnung je Kombination

Wunsch: gemerkter Behälter **unten anstelle des Spielerinventars**, oben das andere GUI. Kombinationen der Vorschau: Kiste + E, Kiste + Ofen, Shulker + Werkbank,
Doppelkiste + Kiste, Kiste + Amboss, Kiste + Leuchtfeuer (hoch).

| Variante | Aufbau | Höhe (Beispiele) | + | − | A12-Eingriff |
|---|---|---|---|---|---|
| **A** (Empfehlung) | Station · gemerkter Kasten · **Hotbar-Leiste**; Reiter links am Kasten [gemerkt…] [Inventar] | Ofen/Werkbank 200, Amboss 198, Doppelkiste+Kiste 255, Leuchtfeuer 259 | entspricht dem Wunsch, Hotbar wieder da (heute fehlt sie), passt meist ganz | 27 Inventarslots hinter Reiter | gering–mittel |
| B | Station · gemerkt · ganzes Inventar | 256–317 | nichts verborgen | bei 720p·3 nur 0–1 Reihe des Kastens | gering |
| C | Vanilla-Bild unverändert, Kasten **links daneben**, Gruppe mittig | 173–234, Breite 356–409 | passt bei allen Rahmen ganz | nicht „unten“; Fenster rückt nach rechts | am geringsten |

Sichtbare Reihen des gemerkten Kastens in A (1080p·2 / ·3 / ·4 / 720p·2 / ·3): Kiste + Ofen/Werkbank/Amboss 3/3/3/3/3; Doppelkiste + Kiste 6/6/6/6/4;
Kiste + Leuchtfeuer 3/3/3/3/1. In B: Kiste + Ofen 3/3/3/3/1; Doppelkiste + Kiste 6/6/3/6/1.

**Inventar (E):** A = Inventarbild oben ganz, Kasten darunter (Kiste 252 hoch: passt außer 720p·3, dort 2 von 3 Reihen; Doppelkiste 306: 6/6/3/6/2 Reihen).
C = daneben (356×169, passt überall). Heute: oben.

**Empfehlung („intelligente Anordnung“):** Kaskade **A → C → A mit Scrollen**: A, solange alle Reihen passen; sonst C, wenn die Gruppenbreite passt; sonst A
scrollend. Damit zeigen die häufigen Fälle (Ofen, Werkbank, Amboss, Schmiede, Steinsäge … + Kiste/Shulker/Fass) immer „unten“, nur hohe Kombinationen
(Doppelkiste, Leuchtfeuer, E + Doppelkiste bei kleinen Fenstern) weichen zur Seite aus.
**A12 Punkt 2:** Nutzen – Stationen direkt aus dem Lager bedienen. Ändert – nur clientseitige Slot-Positionen fremder GUIs, solange etwas gemerkt ist;
Server-Slots, Indizes und Shift-Klick-Regeln bleiben. Eingriff – gering bis mittel, abschaltbar (`enableLinkedContainers`).

## 5 Punkt 3 – zwei Doppelkisten (gemerkt + geöffnet)

Untereinander mit Inventar: 135 + 2 + 135 + 2 + 93 = **367** → passt nur bei 1080p·2.

| Variante | Maß | passt ganz | + | − | A12-Eingriff |
|---|---|---|---|---|---|
| A Scrollen (mit Hotbar-Leiste) | 182×(201+18·Reihen) | 6 Reihen bei 1080p·2/·3, 720p·2; 3 bei 1080p·4; **1** bei 720p·3 | wenig Code (heute schon) | bei Auto-Skala unbrauchbar | gering |
| B Seiten-Reiter | 177×230 | alle | ein Lagerbereich, Reiter geöffnet ↔ gemerkt; skaliert auf 3 Merkungen | nur eine Kiste sichtbar, Ziehen über Reiter | mittel |
| **C nebeneinander** (Empfehlung) | 356×230 | alle (Gruppe mittig); Vanilla-Fenster mittig lassen (C1) nur ab 539 breit | beide ganz sichtbar, Drag & Drop direkt | Fenster rückt zur Seite | gering |
| D kompakte Reihen 18×3 | 339×259 | bis 270, nicht 240 | flach | Vanilla-Raster der geöffneten Kiste umgestellt, ungewohnt | mittel |

**Empfehlung: C, Rückfall B** (GUI-Breite < 360 oder mehr als ein gemerkter Kasten neben einer Doppelkiste).
**A12 Punkt 3:** Nutzen – große Lager umräumen ohne Laufen. Ändert – Anordnung nur bei aktiver Merkung. Eingriff – C gering, B/D mittel.

## 6 Umsetzungsskizze (erst nach Besitzer-Abnahme)

Server/Common (`linked/`):
1. `LinkedContainers`: `Map<Player, List<Mark>>` (max `linkedContainerMax`), Hinzufügen/Entfernen per Schleich-Rechtsklick, Escape = alle. `Session` je Mark,
   `attach` hängt alle gemerkten Behälter nacheinander an (Summe ≤ `MAX_SLOTS`, sonst ältesten weglassen). `interceptClick`: „gemerkt“ = alle `LinkedSlot`
   des aktiven Reiters (Client meldet nichts; Server nimmt alle gemerkten in Reihenfolge).
2. `LinkedOpenPayload`: Liste `(size, title, icon)` statt Einzelwert; HUD-Nutzlast mit Liste (+ optional Entfernungsanteil für die blasse Kachel, alle 20 Ticks).
3. `LinkedConfig`: `linkedContainerMax` (1–5), `normalizeNumbers`; Lang EN/DE + Tooltip.

Client (`client/`):
4. `LinkedHud` → Kachelreihe oben links (24×24, x = 1 + 25·i), dunkler Stil aus `GadgetHud`-Farben; Client-Schalter `linkedHudIcons` in `ContainersConfig`.
   SimpleLib: `HudReserve` (oder Feld in `GadgetHud`) für die obere linke Ecke, `GadgetHud.origin` respektiert sie.
5. `LinkedPanel` → `LinkedLayout` (reine Funktion, testbar ohne Client wie `BoxLayout`): Eingaben Bildmaße, Slot-Rechtecke der Station, Inventarbereich,
   gemerkte Größen, GUI-Breite/-Höhe → Modus `BELOW` (A) / `BESIDE` (C, mit `leftPos`-Verschiebung der Gruppe) / `BELOW_SCROLL` / `TABS` (B in Punkt 3)
   + Kastenrechtecke + Slot-Positionen. Kasten nach `UiBoxes.MARGIN` (177 breit, +3 unten; mit Scrollbalken 182).
6. Hotbar-Leiste in A: die 9 Hotbar-Slots bleiben sichtbar (heute alle auf −10000), werden unter den Kasten gelegt; die 27 Hauptslots hängen am Reiter „Inventar“
   (Wechsel = nur Client-Positionen, wie heute `SlotPositionAccessor`).
7. `ContainerScreenLinkedMixin`: Reiter zeichnen/klicken, Layout-Modus anwenden; `BESIDE` verschiebt `leftPos` (wie Vanillas Rezeptbuch – bei Screens, die
   ihr Bild aus `width/height` statt `leftPos` rechnen, Fallback A scrollend; Liste der geprüften Screens in `LinkedLayout`).
8. Rezeptbuch-Knopf: bei `BESIDE` Rezeptbuch schließt den gemerkten Kasten nicht aus – Rezeptbuch offen ⇒ Modus A.

## 7 Risiken

- **Verschieben von `leftPos`** (C): manche Mod-/Vanilla-Screens zeichnen aus `width/height` (Grund der alten Entscheidung „zweites GUI bleibt mittig“).
  Gegenmittel: Whitelist der Screens mit sauberem `leftPos` (alle `simpleinterfaces`-gestylten), sonst A scrollend.
- **Verborgene Inventarslots** (A): Shift-Klick aus Stations-Ausgängen landet im (verborgenen) Inventar → Reiter „Inventar“ kurz aufleuchten (A14).
- **Mehrfach-Merken:** Slot-Summen, Dupe-Prüfungen je Session, Reihenfolge beim Shift-Klick; Chunk-Last durch mehrere geladene Behälter (Reichweite bleibt 64).
- **HUD-Reservierung** berührt SimpleLib (Mindestversion der Nutzer anheben).
- Vorschau ist aus Code-Maßen; echte Schrift/Items fehlen. Spiel-Screens siehe §8.

## 8 Spiel-Screens für Session B (je ein Client, nacheinander)

Welt flach, Kreativ aus (Überleben), GUI-Skala 2 und 3, Fenster 1280×720:
1. Kiste 3 Blöcke vor dem Spieler, Schleich-Rechtsklick mit leerer Hand → Screenshot Welt (Banner rechts, Partikel an der Hand).
2. Danach Ofen öffnen → Replace-Modus heute (keine Hotbar).
3. `E` drücken → Panel über dem Inventar.
4. Doppelkiste merken, zweite Doppelkiste öffnen, Skala 3 (Auto) → heutiges Scrollen.
5. Mit Oktant in der Hand und Gadget-HUD links: Banner rechts + Gadget-Kasten zusammen (für Punkt 1 Vergleich).

## 9 Testplan (bei Umsetzung)

- GameTests (`LinkedTests`): Mehrfach-Merken (hinzufügen, entfernen, Maximum, Escape alle), Shift-Klick-Summen ohne Dupe über zwei gemerkte Behälter,
  Ende einzelner Merkung bei Reichweite, Config-Grenzen `linkedContainerMax`.
- Reine Layout-Tests `LinkedLayout` (ohne Client, wie `BoxLayout`): je Kombination aus §4/§5 und je Rahmen aus §2 erwarteter Modus, Rechtecke, Ränder = 3,
  nichts außerhalb des Fensters.
- Client-Test/ui_probe: Szenario `simpleinterfaces/n65` (wie `n62`) – Screens für die Kombinationen bei 720p·3 und 1080p·4, Ränder automatisch nachgezählt.
- HUD: Client-Test Kacheln oben links, kein Überlapp mit Gadget-HUD (3 Kästen, Anker oben).
- Gates: Modul-Gate simpleinterfaces Fabric 26.3, `ui_text_contrast`, `checkModuleStyle`, Wiki/Handbuch (`manual.json` EN/DE, Guide-Seiten 12 ff.).

## 10 Offene Fragen an den Besitzer

1. HUD: A/B/C, Stil dunkel/hell; Banner rechts ganz weg?
2. Mehrfach-Merken: ja (Standard 3 / 1) oder bei einem bleiben?
3. Punkt 2: Kaskade A → C ok, oder strikt „immer unten“ (dann A mit Scrollen statt C)?
4. In A: Hotbar-Leiste immer + Inventar hinter Reiter ok?
5. Punkt 3: C nebeneinander mit Rückfall B ok?
6. E: unten (Wunsch) – bei 720p·3 scrollt dann schon eine einfache Kiste (2 von 3 Reihen); ok oder dort automatisch daneben?

## 11 Umsetzung (Besitzer-Abnahme 15.11.: Empfehlungen A / 3 / Kaskade A → C → A-Scroll / C mit Rückfall B / A7)

### 11.1 Plan (vor der ersten Änderung, Ist-Zustand §1 im Code nachgeprüft)

Vorhandenes, auf dem aufgebaut wird: `LinkedContainers` (Merken, Sessions, Shift-Klick), `LinkedPanel` (Client-Slots, Scrollen,
`SlotPositionAccessor`), `ContainerScreenLinkedMixin` (Layout/Zeichnen/Klick), `HudLinkedMixin`, `KeyboardLinkedMixin` (Escape),
`BoxLayout` (Vorbild reine Funktion), `UiBoxes.MARGIN/FRAME`, `GadgetHud`-Farben, `StyledScreens` (Stil zeichnet mit `leftPos`).

Server/Common:
1. `LinkedConfig.linkedContainerMax` (1–5, Standard 3) + `normalizeNumbers`; Konfig-Screen-Eintrag, Lang EN/DE + Tooltip.
2. `LinkedContainers`: `Map<Player, List<Mark>>` (älteste zuerst). Schleich-Rechtsklick auf einen gemerkten Behälter (auch andere
   Doppelkisten-Hälfte) entfernt **nur diesen**; ein neuer wird angehängt; über `linkedContainerMax` oder Slot-Summe > `MAX_SLOTS`
   fällt der älteste weg. Escape / `clear` = alle. Tick prüft jede Merkung einzeln (Reichweite, Rechte, Besuch). `mark(p)` = jüngste,
   neu `marks(p)`.
   `Session` = ein verknüpftes Menü mit `List<Part>` (je Merkung Behälter, Block-Entities, Start, Größe); `LinkedSlot` prüft seinen Part.
   Shift-Klick „gemerkt“ = alle `LinkedSlot` in Merk-Reihenfolge (unverändert `linkedSlots`), keine Client-Meldung nötig.
3. `LinkedOpenPayload(containerId, List<Entry(size, title, icon)>)`; HUD-Nutzlast (`HUD = -1`) mit der Liste aller Merkungen.

SimpleLib:
4. `HudReserve` (neu, `com.simplelib.api.client`): `topLeft(owner, width, height)` / `release(owner)`, `bottom()`/`right()`.
   `GadgetHud.begin` schiebt einen Kasten, der die Reservierung schneidet, auf `y ≥ bottom + 2`. Reine Rechnung `GadgetHud.clear(...)`.

Client:
5. `LinkedHud` → Kachelreihe oben links: 24×24 bei x = 1 + 25·i, y = 1, Item bei +4/+4, 2×2-Punkt `0xB38EF3` unten rechts,
   Rahmen/Füllung aus den `GadgetHud`-Farben. Banner entfällt. Client-Schalter `ContainersConfig.linkedHudIcons` (Standard an)
   + Lang/Tooltip; reserviert die Ecke über `HudReserve` (Breite 25·n).
6. `LinkedLayout` (neu, `linked/`, reine Funktion ohne Client-Klassen): Eingaben GUI-Maße, Bildmaße, `leftPos/topPos` (Rezeptbuch
   = `leftPos` ≠ Mitte), Oberkante der Inventarslots, eigene Lagerreihen der Station, Inventar-Screen ja/nein, „sauberer Screen“
   (Whitelist), gemerkte Größen, aktiver Reiter → `Mode BELOW | BESIDE | BELOW_SCROLL | TABS`, `dx/dy` für `leftPos/topPos`,
   Kasten-Rechteck (177 bzw. 182 mit Scrollbalken, Höhe 27 + 18·Reihen), sichtbare Reihen, Hotbar-Leiste (35), Reiter-Rechtecke,
   Schnittlinie für den Hintergrund. Regeln:
   - Station mit Inventar: **A** (Station bis Inventar-Oberkante − 8, darunter Kasten, 2 px, Hotbar-Leiste; Reiter links
     [gemerkt…][Inventar]; Kastenhöhe aus der höchsten Reiterbelegung); passt nicht → **C** (Kasten links neben dem Bild, Gruppe
     mittig per `dx`, nur sauberer Screen und Rezeptbuch zu) → **A scrollend**. Rezeptbuch offen ⇒ A (Reiter rechts, kein `dx/dy`).
   - Inventar (E) oder Station ohne Inventarslots: Kasten **unter** dem Bild (Gruppe mittig per `dy`), sonst **daneben**, sonst unten scrollend.
   - Zwei Doppelkisten (eigene Lagerreihen ≥ 6 und gemerkt ≥ 6 Reihen, oder mehrere Merkungen neben einer Doppelkiste):
     **C** bei GUI-Breite ≥ 360 und genau einer Merkung, sonst **B** (Seiten-Reiter [geöffnet][gemerkt…], ein Lagerbereich).
   - Whitelist sauberer Screens: Vanilla-Screens, die selbst mit `leftPos` zeichnen (Inventar, Werkbank, Öfen, Kartentisch,
     Webstuhl, Schmiede, Steinsäge) und die Lager-/Werferscreens nur mit aktivem Simple-Stil (Vanilla zeichnet sie aus `width`).
7. `LinkedPanel` wendet das Layout an: Heimpositionen aller verschobenen Slots merken (`home`), Hotbar in die Leiste, 27 Hauptslots
   je nach Reiter in den Kasten oder auf −10000, gemerkte Slots fremder Reiter `shown = false`; B: eigene Lagerslots ↔ gemerkte.
   Ränder nach A7 (Slots bei +8/+18, rechts 3 px, unten 3 px + 7 Rahmen, Scrollbalken in eigener 5-px-Spalte → 182).
   `StyledScreens/WorkScreens/StationScreens.layout` rechnen mit den Heimpositionen (der Stil bleibt an).
8. `ContainerScreenLinkedMixin`: Layout nach dem ganzen `init` (Hook `Screen.init(II)` / `rebuildWidgets`) und bei Nutzlast;
   `dx/dy` auf `leftPos/topPos` und alle Widgets (rückgängig vor jedem neuen Layout); Rezeptbuch-Umschalten ⇒ neu aufbauen;
   Reiter zeichnen/klicken; Hintergrund unterhalb der Schnittlinie (A) bzw. Lagerbereich (B, Reiter gemerkt) per Scissor ausblenden,
   Titel/Inventar-Label dort weg.

Tests/Werkzeuge:
9. GameTests (`LinkedTests`, Fabric-Katalog): Mehrfach-Merken hinzufügen/entfernen/Maximum/Escape alle/Config-Grenzen,
   Slot-Summe ≤ `MAX_SLOTS`, Shift-Klick ohne Dupe über zwei gemerkte Behälter (Reihenfolge), Payload-Liste.
   Reine `LinkedLayout`-Tests: alle Kombinationen §4/§5 × alle Rahmen §2: erwarteter Modus, Ränder = 3, nichts außerhalb.
10. ui_probe-Szenario `simpleinterfaces/n65` (`tools/uiprobe_n65.py`, wie `uiprobe_n62.py`) – nur geschrieben, nicht gefahren.
11. Handbuch/Guide EN/DE (manual.json, Guide-Seiten 11 ff.) auf Mehrfach-Merken, Kacheln, Anordnung.

Risiken: `leftPos`-Verschiebung bei fremden Screens (→ Whitelist), Station-Screens, die Slots selbst umsetzen (StationScreens,
Heimpositionen), Scissor-Stapel, Vanilla-Textur unter dem Kasten (→ Schnitt), Client-Darstellung ungeprüft ohne Client-Lauf.
Verifikation: Kompilieren Fabric (simplelib + simpleinterfaces), GameTests `simpleinterfaces:*linked*`, ganzes Modul, Precheck.
Client-/Probe-Läufe macht die Eltern-Session.
