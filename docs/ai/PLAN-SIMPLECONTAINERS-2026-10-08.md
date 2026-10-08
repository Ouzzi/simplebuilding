# Plan simplecontainers + einheitlicher UI-Stil (2026-10-08)

Besitzer: Roadmap-P3 „UI-Konzepte aller UIs“ findet in **simplecontainers** statt; UI-Elemente parallel per Claude.
Referenzen: `/root/previews/refs-n12/bild3-container-stil.webp` (farbiger Block-Kasten je Container + heller
Inventar-Kasten, eingelassene gerundete Slots, Symbole statt Text) und `bild4-ofen-stil.png` (Ofen: Brennstoff-Slot
zeigt die Flammen in sich, großer Ergebnis-Slot, Pfeil, dezente Motive im Kasten). Maße/Farben: PLAN-CRUCIBLE-N12B
(FRAME 5/7, Ecken-Treppe, Slot 16+2, Palette fill/light/shade/slot/slotTop/label). Inventar: INVENTAR-UIS-2026-10-08.md.

## Entscheidungen (vom Besitzer offen gelassen, von Claude entschieden – Rückmeldung jederzeit möglich)
1. **Umfang:** alle Vanilla-Container-Screens mit Slots (Truhe/Doppeltruhe/Fass/Shulker/Endertruhe, Trichter,
   Spender/Werfer, Crafter, Werkbank, Ofen/Schmelzofen/Räucherofen, Braustand, Amboss, Schleifstein, Steinsäge,
   Webstuhl, Kartografietisch, Schmiedetisch, Verzauberungstisch, Leuchtfeuer, Handel, Reittier-Inventar,
   Spieler-Inventar). **Nicht:** Kreativinventar (eigene Tab-Logik, später), Bücher, Config-Screens (Cloth Config),
   HUD (bleibt Vanilla-Tooltip-Stil).
2. **Technik:** im Code gezeichnet (wie CrucibleScreen), keine PNG-Hintergründe; skaliert, färbt je Block, ohne
   Ressourcenpaket-Konflikte. Je Screen abschaltbar (Config), Hauptschalter.
3. **Farbe je Block:** ja, wie Bild 3 (Holzart der Truhe/Werkbank, Fass braun, Shulker in Shulkerfarbe, Endertruhe
   petrol, Ofen stein-grau, Schmiedetisch dunkel …); Inventar-Kasten immer hell (INVENTORY-Palette).
4. **Mod-UIs:** Stil-Bausteine liegen in **simplelib** (`com.simplelib.client.ui`), damit Mod-UIs (Tiegel, Mod-Truhen,
   Rucksack, Auto-Schmied, Befiederungstisch, Mod-Trichter, Hufeisen-Panel, LinkedPanel) auch **ohne**
   simplecontainers im Stil erscheinen (Modul-Unabhängigkeit). simplecontainers selbst stylt nur Vanilla-Screens
   und ist rein clientseitig (Server braucht es nicht).
5. **Guide-Buch** behält Buch-Optik; Blaupause/Oktant/Bauzauberstab/Trim-Referenz/Modell-Browser bekommen den
   Kasten-Stil in einer späteren Welle (nach Abnahme der Container).
6. **Loader:** Fabric 26.3 zuerst, NeoForge/Forge 26.3 im selben Zug (Client-Mixins sind geteilt), 26.2/1.21.11 erst
   im Port-Run.

## Architektur
- `modules/simplelib/shared/java/com/simplelib/client/ui/`: `UiPalette` (record, aus CrucibleScreen.Palette),
  `UiBoxes` (box(), slot(), bigSlot(), arrow(), progress, Symbole), `UiFlames` (= CrucibleFlames verschoben/geteilt).
  CrucibleScreen nutzt sie (Verhalten/Optik unverändert → Regression = Vorschau-Vergleich).
- `modules/simplecontainers/` (neues Modul wie simplesounds, client-only, `requires` cloth_config +
  `requires` simplelib für die Stil-Bausteine).
  - `ContainerStyles`: Zuordnung Screen-Klasse/MenuType → Stil (Palette, Kasten-Aufteilung, Sonder-Elemente).
  - Mixin auf `AbstractContainerScreen#renderBg`/extractBackground (26.3-Name prüfen): ist ein Stil registriert und
    aktiv → eigener Hintergrund: Kästen aus den Slot-Grenzen (Container-Slots vs. Spieler-Slots), Slots an jeder
    Slot-Position, Sonder-Elemente (Pfeil, Flammen, Blasen, Fortschritt) aus dem Menü-Zustand; sonst Vanilla.
  - Labels: Titel in label-Farbe der Palette; „Inventar“-Label bleibt.
  - Config: Hauptschalter + je Screen-Art an/aus (EN/DE, Tooltips mit Default).
- Vorschau-Werkzeug `tools/ui/simplecontainers_preview.py`: rendert jede UI aus denselben Zahlen (Python-Port der
  Bausteine) als PNG + Kontaktbogen → `/root/previews/simplecontainers/` (Branch refs-assets, nie master).

## Wellen
- **W0 (parallel):** A) Java-Grundlage (simplelib ui + Modul-Gerüst + generischer Renderer für Truhen-artige Screens)
  – Branch `claude-sc-base`. B) Vorschau-Werkzeug + Vorschauen aller Vanilla- und Mod-Container – Branch
  `claude-sc-preview` (+ PNGs in refs-assets).
- **W1 (parallel nach W0, je Agent ein Branch auf claude-sc-base):** G1 Lager (Truhen, Fass, Shulker, Endertruhe,
  Trichter, Spender/Werfer, Crafter, Reittier), G2 Arbeit I (Werkbank, Öfen ×3, Braustand, Leuchtfeuer,
  Verzauberungstisch), G3 Arbeit II (Amboss, Schleifstein, Steinsäge, Webstuhl, Kartografie, Schmiedetisch,
  Handel, Spieler-Inventar), G4 Mod-UIs (TieredChest, Rucksack, Auto-Schmied, Befiederung, Mod-Trichter,
  Hufeisen-Panel, LinkedPanel) über simplelib-Bausteine.
- **W2:** Nicht-Container-Screens (Blaupause, Oktant, Bauzauberstab, Trim-Referenz, Modell-Browser), Kreativinventar.

## Verifikation
- Kompilieren/Tests je Branch auf sb-test: `/root/sbt <branch> "./gradlew … -q"` bzw. run.py (Job-Sperre).
- GameTests (server-seitig prüfbar): Stil-Registry deckt jede Vanilla-MenuType mit Slots ab; Kasten-Berechnung aus
  Slot-Grenzen (reine Funktionen); Config-Schalter; Modul-Check `tools/check_data.py`; Standalone-Target.
- Client-Sicht: Client-GameTest-Screenshots (xvfb, client-fabric-263) je Screen + Vorschau-Vergleich; Abnahme
  durch Besitzer anhand `/root/previews/simplecontainers/`.
- Gate über `/root/sb-gate.sh`.

## Risiken
- 26.3-Rendering-API (GuiGraphicsExtractor statt GuiGraphics) – Muster aus CrucibleScreen übernehmen.
- Fremde Mods mit eigenen Screens auf Vanilla-Menüs: nur exakte Vanilla-Screen-Klassen stylen.
- Ressourcenpakete: Hauptschalter aus = reines Vanilla.

## W0-A Umsetzung (Branch `claude-sc-base`)
Ist-Zustand (26.3-Quellen, NeoForge-patched 26.3.0.16-beta): `AbstractContainerScreen` **überschreibt
`extractBackground` nicht**; jede Unterklasse (ContainerScreen, ShulkerBoxScreen, HopperScreen, DispenserScreen …)
ruft `super.extractBackground` (= `Screen`: Abdunkeln/Blur) und blittet danach ihr PNG. Labels zeichnet
`AbstractContainerScreen#extractLabels` (Farbe 0xFF404040). Slot-Lücke zwischen Container- und Spieler-Slots ist in
Vanilla nur 13–14 px (inkl. heller Slot-Kante): Truhe 9×N/Trichter/Spender 14, Shulker 13.

Entscheidungen (Claude):
1. **Paket `com.simplelib.api.client.ui`** statt `com.simplelib.client.ui`: Prinzip 6a erlaubt Modulen nur Importe aus
   `com.simplelib.api`. Inhalt: `UiPalette` (record + INVENTORY/BARREL + `derived`), `UiBoxes` (box, slot, bigSlot,
   arrow, progressFill, progressBar, veil, rounded, scale, FRAME 5/7, RIM), `UiFlames` (Flammenstreifen nach Zielhöhe
   und Ruhe, Kern aus CrucibleFlames). CrucibleScreen/CrucibleFlames delegieren – gleicher Code, gleiche Pixel.
2. **Mixin je Screen-Klasse statt auf AbstractContainerScreen** (dort gibt es die Methode nicht): HEAD-Inject in
   `extractBackground` der Ziel-Klassen, bei aktivem Stil `super.extractBackground` (nur Screen-Hintergrund) + eigener
   Hintergrund + cancel. Labels: HEAD-Inject in `AbstractContainerScreen#extractLabels`. Weitere Gruppen legen eine
   eigene Mixin-Klasse für ihre Screen-Klassen an (eine Zeile in `simplecontainers.mixins.json`).
3. **Kasten-Grenzen aus Slots** (`BoxLayout`, rein, serverseitig testbar): waagerecht über alle Slots ±8 (Truhe: 0..176),
   Container-Kasten oben bis Bildrand (Titel liegt im Kasten), Rahmen 5/7. Fuge nach der W0-B-Regel („Kasten-Fuge“):
   freie Zeilen ≥ 12 → zwei Kästen, Fuge min(2, frei−12), Rest polstert den Container-Kasten; < 12 → vorerst Vanilla
   (schattenloser Kasten 10–11 und Ein-Kasten-Variante < 10 kommen mit den Screens, die sie brauchen, W1). Das
   „Inventar“-Label entfällt im Stil (kein Platz, Bild 3 zeigt auch keins); der Titel steht in der Label-Farbe.
4. **Registry** (`ContainerStyles`, serverseitig ladbar): Schlüssel = `MenuType` + exakter Vanilla-Screen-Klassenname
   (fremde Unterklassen bleiben Vanilla). Je Gruppe eine Klasse (`StorageStyles` = W0), zentrale Liste
   `ContainerStyles.GROUPS`. Palette aus `StyleContext` (Menü, Titel-Schlüssel, angeschauter Block): Titel bestimmt die
   Familie (Truhe/Fass/Endertruhe/Shulker/Trichter/Spender), der Block verfeinert (Kupfertruhen-Stufen,
   Shulkerfarbe); sonst Standard je Menü (Eiche-Truhe). Füllfarben = W0-B-Palettentabelle, Rest über
   `UiPalette.derived` (gleiche Regel wie `derive` im Vorschau-Skript; Test prüft Eiche/Trichter gegen die Tabelle).
5. **Config** `config/simplecontainers.json` (nur Client): Hauptschalter `enabled` + `screens.<stil-id>` (Standard an);
   Cloth-Screen aus der Registry erzeugt (Forge: native-config wie simplesounds).
6. **SimpleLib gebündelt** (Prinzip 6a, wie Sandwiches: Fabric `include`+`implementation`, NeoForge `jarJar`, Forge
   `forgeBundleSimplelib`); `requires` nennt `cloth_config` + `simplelib`. Folge: SimpleLib bringt Blöcke mit – ein
   reiner Client ohne SimpleLib auf dem Server ist damit nicht garantiert (Registry-Abgleich). Offener Punkt für
   den Besitzer: UI-Bausteine später ggf. in eine inhaltsfreie Client-Bibliothek.
7. Tests: GameTests (Fabric @GameTest + NeoForge-Katalog) für BoxLayout (alle W0-Geometrien, zu enge Lücke → null),
   Registry-Abdeckung (alle W0-Menüs, exakte Klassen, eindeutige Ids), Palettenwahl, Config-Default; Standalone-Target
   Fabric/NeoForge; Client-Smoke mit Truhen-Screenshot.

### W0-A Verifikation (2026-10-08, sb-test)
- Compile: `:modules:simplecontainers:{fabric,neoforge}:compileJava`, `:modules:simplelib:{fabric,neoforge}:compileJava`,
  `:integration:compileGametestJava`, Forge (`-Pforge263=true`) simplecontainers + simplelib: grün.
- `run.py --targets module-simplecontainers-{fabric,neoforge,standalone-fabric,standalone-neoforge}-263`:
  „alles gruen: 20/20 bestanden, 0 rot“. `module-simplelib-fabric-263` (Tiegel-Regression): 25/25 grün.
- Client-Smoke `module-simplecontainers-client-263` (unter `xvfb-run`, ohne Display stürzt der Client bei SDL ab):
  „alles gruen: 7/7 bestanden, 0 rot“; Screenshots `/root/previews/simplecontainers/w0a/` (Truhe, Doppeltruhe, Fass,
  Endertruhe, Shulker, Trichter, Werfer).
- Nicht getestet: NeoForge/Forge-Client-Sicht, Tiegel-Bildschirm im Client (Code nur verschoben), reiner Client gegen
  Vanilla-Server, echte Blöcke angeschaut (Smoke öffnet Screens clientseitig ohne Block).
- Abweichungen: Paket `com.simplelib.api.client.ui` (statt `com.simplelib.client.ui`), Mixin je Screen-Klasse statt
  auf AbstractContainerScreen (Methode existiert dort nicht), Fugenregel/Paletten aus W0-B übernommen.
## Paletten (Vorschau W0-B)
Quelle: `tools/ui/simplecontainers_preview.py` (P / derive), Vorschauen + Entscheidungen (Kasten-Fuge, bigSlot, Brennstoff-Slot, Symbole, Motive): `/root/previews/simplecontainers/README.md` (refs-assets). Reihenfolge = `UiPalette(fill, light, shade, slot, slotTop, label)`.

| Schlüssel | Block | fill | light | shade | slot | slotTop | label | Motiv |
|---|---|---|---|---|---|---|---|---|
| `chest_oak` | Truhe (Eiche) | #CE9148 | #FFB85B | #A8763B | #A07138 | #835C2E | #2E3034 | wood |
| `barrel` | Fass | #B9774F | #D08F68 | #955839 | #94573A | #74412B | #404040 | wood |
| `ender_chest` | Endertruhe | #597880 | #7198A2 | #486268 | #455D63 | #384C51 | #F2EEE8 | ender |
| `shulker_purple` | Shulkerkiste lila | #876C99 | #AB89C2 | #6E587D | #695477 | #564561 | #F2EEE8 | shulker |
| `shulker_light_blue` | Shulkerkiste hellblau | #5C97B8 | #74BFE9 | #4B7B96 | #47758F | #3A6075 | #2E3034 | shulker |
| `hopper` | Trichter | #5A5C63 | #84868B | #494B51 | #46474D | #393A3F | #F2EEE8 | metal |
| `dispenser` | Spender/Werfer | #878787 | #ABABAB | #6E6E6E | #696969 | #565656 | #2E3034 | stone |
| `crafter` | Crafter | #7A736A | #9A9286 | #645E56 | #5F5952 | #4E4943 | #F2EEE8 | redstone |
| `horse` | Reittier (Sattelleder) | #8B5E3C | #A9876E | #714D31 | #6C492E | #583C26 | #F2EEE8 | leather |
| `crafting` | Werkbank | #B7935B | #E8BA73 | #96784A | #8E7246 | #755E3A | #2E3034 | wood |
| `furnace` | Ofen | #929699 | #B9BEC2 | #777A7D | #717577 | #5D6061 | #2E3034 | stone |
| `blast_furnace` | Schmelzofen | #6E7179 | #8B8F99 | #5A5C63 | #55585E | #46484D | #F2EEE8 | metal |
| `smoker` | Raeucherofen | #7D6B57 | #9E876E | #665747 | #615343 | #504437 | #F2EEE8 | smoke |
| `brewing` | Braustand | #847D7D | #A79E9E | #6C6666 | #666161 | #545050 | #2E3034 | stone |
| `beacon` | Leuchtfeuer | #6FB4B1 | #8CE4E0 | #5B9391 | #568C8A | #477371 | #2E3034 | glass |
| `enchanting` | Zaubertisch | #A1282B | #B95F62 | #842023 | #7D1F21 | #67191B | #F2EEE8 | rune |
| `anvil` | Amboss | #666666 | #8D8D8D | #535353 | #4F4F4F | #414141 | #F2EEE8 | metal |
| `grindstone` | Schleifstein | #9E9A92 | #C8C3B9 | #817E77 | #7B7871 | #65625D | #2E3034 | stone |
| `stonecutter` | Steinsaege | #857A72 | #A89A90 | #6D645D | #675F58 | #554E48 | #F2EEE8 | stone |
| `loom` | Webstuhl | #9C8262 | #C6A57C | #7F6A50 | #79654C | #63533E | #2E3034 | wool |
| `cartography` | Kartentisch | #6B5A45 | #918475 | #574938 | #534635 | #44392C | #F2EEE8 | paper |
| `smithing` | Schmiedetisch | #4B1E19 | #795854 | #3D1814 | #3A1713 | #301310 | #F2EEE8 | metal |
| `merchant` | Handel | #3F8A55 | #70A881 | #337145 | #316B42 | #285836 | #F2EEE8 | leather |
| `player` | Inventar | #E3E6E9 | #F8F9FA | #C5CACE | #B4BABF | #979DA3 | #404040 | none |
| `chest_reinforced` | Verstaerkt | #6F9095 | #9DBCC1 | #587378 | #55737A | #425C61 | #F0F6F6 | metal |
| `chest_netherite` | Netherit | #5F524C | #867870 | #4A3F3A | #473C37 | #352C28 | #EFE4DA | nether |
| `chest_enderite` | Enderit | #8E6CB0 | #B99AD6 | #735693 | #70538E | #594073 | #F7F0FF | ender |
| `backpack` | Rucksack (Leder) | #8E6440 | #AB8C71 | #745234 | #6E4E31 | #5A4028 | #F2EEE8 | leather |
| `backpack_enderite` | Enderit | #8E6CB0 | #B99AD6 | #735693 | #70538E | #594073 | #F7F0FF | ender |
| `auto_smither` | Auto-Schmied | #4F5560 | #7C8189 | #40454E | #3D424A | #32363D | #F2EEE8 | redstone |
| `fletching` | Befiederungstisch | #C5B485 | #FAE4A8 | #A1936D | #998C67 | #7E7355 | #2E3034 | wood |
| `hopper_netherite` | Netherit | #5F524C | #867870 | #4A3F3A | #473C37 | #352C28 | #EFE4DA | nether |
| `horseshoe` | Eisen | #9A9DA2 | #C4C7CB | #7E8186 | #7B7E83 | #64676C | #2E3034 | metal |
| `crucible_iron` | Eisen | #9A9DA2 | #C4C7CB | #7E8186 | #7B7E83 | #64676C | #2E3034 | metal |

Kasten-Fuge (für den generischen Renderer): free = erste Inventar-Slotzeile − erste freie Zeile unter dem untersten Container-Element. free ≥ 12 → zwei Kästen, Fuge min(2, free−12), Rest polstert den Container-Kasten; 10–11 → Container-Kasten ohne 2-px-Schatten; < 10 → ein Kasten, Inventarteil als helle Fläche hinter einer Naht (Braustand, Webstuhl, Spieler-Inventar, Rucksack, Leuchtfeuer, Handel). Kein „Inventar“-Label (kein Platz).

## W1 G3 (Arbeit II, Branch `claude-sc-g3`)
Screens: Amboss, Schleifstein, Steinsäge, Webstuhl, Kartografietisch, Schmiedetisch (Vanilla + SB-Ersatz), Handel,
Spieler-Inventar. Basis: claude-sc-base + G1 „narrow box layouts“ (cherry-pick, Varianten NO_SHADOW/SEAM, Elemente).
Plan:
1. `style/StationStyles.java` (Gruppe „work“, Paletten aus der W0-B-Tabelle über `UiPalette.derived`, Spieler = INVENTORY)
   → `ContainerStyles.GROUPS`. Spieler-Inventar hat keinen MenuType: Stil mit leerer Menüliste, Suche nach Klasse
   (`ContainerStyles.findMenuless`), `StyledScreens.style` nutzt sie, wenn `getType()` wirft.
2. Zeichnen in `client/StationScreens.java` (eigene Datei, nicht `drawBackground` umbauen): Layout mit Zusatz-Elementen
   (großer Ergebnis-Slot, Namensfeld, Kartenfeld, Rüstungsständer, Spielermodell) über `BoxLayout.compute(slots, elements…)`,
   Kästen über `StyledScreens.drawBoxes`, Slots, `bigSlot`, Symbole/Felder/Kacheln nach `tools/ui/simplecontainers_preview.py`.
   Symbole (Pfeil 22×15, kleiner Pfeil, Plus, Kreuz, Schleifrad, Hammer, XP, Handelspfeil) als Bitmaps in
   `client/StationDraw.java` (eingraviert: Strich slot + 1 px light darunter; Fortschritt weiß).
   Spieler-Slots = `Inventory`-Slots mit Index < 36 (Rüstung/Schild zählen zum Kasten).
3. Mixins je Screen-Klasse (eigene Dateien, eine Zeile je Klasse in `simplecontainers.mixins.json`): Stil nach dem
   `super.extractBackground`, Vanilla-PNG per `@WrapWithCondition` aus; Vanilla-Sprites, die der Stil ersetzt
   (Namensfeld, Fehlerpfeil → rotes Kreuz, Scrollbalken, Rezept-/Muster-Kacheln, XP-Balken, Handelspfeile), ebenso.
   Keine HEAD-Cancels, damit fremde Injektionen (simplevisuals Amboss-Label, SB Kartentisch-Vorschau, TrimStatsPanel)
   weiterlaufen. Amboss-Kosten: Vanilla-Feld/Text aus, eigene Zeile „XP-Symbol + Zahl“ (rot bei zu teuer/unbezahlbar).
4. Titel immer bei (8, 6) wie in der Vorschau (`StyledScreens.drawLabels`); Handel/Inventar haben eigene Labels.
5. SB-Ersatz `RecipeBookSmithingScreen` (Unterklasse von SmithingScreen, gleiche Geometrie): optionale Kopplung über
   den **Klassennamen als String** (`ContainerStyles.LAYOUT_ALIASES`), kein Import, kein `Class.forName`; ohne SB
   passiert nichts. (Alternative „SB nutzt simplelib selbst“ braucht SB→simplelib-Bündelung + 26.2 – später.)
6. Lang EN/DE + Wiki-Features je Stil-Id, `check_data.py`; GameTests: Registry deckt G3-Menüs ab, Paletten = Tabelle,
   Layout-Varianten je G3-Screen (zwei Kästen / Naht); Client-Smoke öffnet alle G3-Screens, Screenshots.
Risiken: Handel-Kacheln sind Widgets (Button-Sprites) – Mixin auf `MerchantScreen.TradeOfferButton`; Inventar-Rezeptbuch
verschiebt `leftPos` (Zeichnung folgt `leftPos`). Platzhalter-Sprites leerer Slots zeichnet Vanilla selbst (grau).
## W1 G2 (Arbeit I, Branch `claude-sc-g2`)
Screens: Werkbank (`CraftingScreen`), Ofen/Schmelzofen/Räucherofen (`FurnaceScreen`/`BlastFurnaceScreen`/`SmokerScreen`,
gemeinsam über `AbstractFurnaceScreen`), Braustand (`BrewingStandScreen`), Leuchtfeuer (`BeaconScreen` + Knöpfe),
Verzauberungstisch (`EnchantmentScreen`). Vorschau: `g2-*.png`, `kontakt-g2.png`.

Plan:
1. `style/WorkStyles.java` (Gruppe „work“, 7 Stile: crafting, furnace, blast_furnace, smoker, brewing_stand, beacon,
   enchanting; Füllfarben aus der W0-B-Tabelle über `UiPalette.derived`) → `ContainerStyles.GROUPS`.
2. simplelib (additiv): `UiSymbols` (eingravierte Symbole wie die Vorschau: Pfeil 22×15, Wärme, Rauch, Pfeil ab 9×26,
   Haken, Kreuz, Pyramide, Stern; Fortschritt füllt weiß), `UiBoxes.fuelSlot` (Zungen-Silhouetten + Füllung),
   `UiBoxes.inset`/`raised`, `ProgressColors.BLAST`/`BLAZE`.
3. `client/WorkScreens.java`: Zeichnung je Screen aus dem Menü-Zustand (Brenn-/Kochfortschritt, Lohenpulver, Brauzeit,
   Blasen wie Vanilla getaktet, Verzauberungs-Kosten). Kästen über `BoxLayout` aus Slots + Zusatz-Elementen
   (Verzauberungszeilen, Leuchtfeuer-Knöpfe als Pseudo-Slots), damit die Fuge wie in der Vorschau sitzt.
4. Mixins je Screen-Klasse (`CraftingBackgroundMixin`, `FurnaceBackgroundMixin`, `BrewingBackgroundMixin`,
   `BeaconBackgroundMixin` + `BeaconButtonMixin`, `EnchantmentBackgroundMixin`): Vanilla-PNG und Vanilla-Sprites
   (Flamme, Pfeil, Lohen-Balken, Blasen, Verzauberungsfelder, Leuchtfeuer-Knöpfe) per `@WrapWithCondition` aus,
   Titel bei (8, 6) wie in der Vorschau. Rezeptbuch-Knopf unverändert (Vanilla-Position, Vanilla-Funktion).
5. Braustand/Leuchtfeuer: Ein-Kasten-Variante aus G1 (`feat(simplecontainers): narrow box layouts`, cherry-pick).
6. Lang EN/DE, Wiki-Features `style_work` + `config_screen_<id>`, Tests (Registry/Paletten/Layouts der G2-Geometrien),
   Client-Smoke um die 7 Screens erweitert, Screenshots nach `/root/previews/simplecontainers/w1-g2/`.

Entscheidungen (Claude): Verzauberungstisch behält das animierte 3D-Buch (echtes Modell wie die Tier-Vorschauen,
README-Punkt 11) statt des eingravierten Buch-Symbols; Leuchtfeuer: Texte „Primäre/Sekundäre Kraft“ entfallen
zugunsten von Pyramide/Stern (Symbole statt Text), Tooltips der Knöpfe bleiben.

### W1 G3 Umsetzung + Verifikation (2026-10-08, sb-test)
Abweichungen vom Plan: Klassen heißen `Station*` (G2 belegt `WorkStyles/WorkScreens`); Zeichnen über G2s simplelib-API
(cherry-pick b4076457a, e4d8f4d36, c72362e75 als Voraussetzung, d7c56ae7e): `UiBoxes.inset/sunkRect/raised`,
`UiSymbols` (G3-Bitmaps additiv ergänzt: ARROW_SMALL, PLUS, WHEEL, ANVIL_HAMMER, XP, TRADE_ARROW), `UiMotifs`
(Motiv/Seed je Screen wie die Vorschau), `StyledScreens.slotIconColor` (auch Webstuhl-Slot-Sprites und die
wechselnden Schmiedetisch-Icons, `CyclingIconStationMixin`). `StyledScreens`: Spieler-Slots = `Inventory`-Index < 36
(Rüstung/Schild gehören zum Kasten), Titel der G3-Screens bei (8, 6), menülose Stile über `findMenuless`.
Abweichungen von der Vorschau: Namensfeld 107 breit (EditBox reicht bis x 165, Vorschau 103); Webstuhl-Musterfeld
y 12 (Vanilla-Kacheln beginnen bei 13); Kartenfeld 66 hoch, darauf bleibt Vanillas Papier-/Karten-Sprite (zeigt
Kopieren/Vergrößern/Sperren); Rezeptbuch-Knopf (Inventar, SB-Schmiedetisch) bleibt Vanilla; Fehlerpfeile = rotes Kreuz
über dem eingravierten Pfeil; Handels-Angebote: gewählt = eingelassen, Pfeil in slotTop.
- Compile Fabric/NeoForge/Forge(-Pforge263=true) simplecontainers + simplelib Fabric: grün.
- `run.py --targets module-simplecontainers-{fabric,neoforge,standalone-fabric,standalone-neoforge}-263`:
  „alles gruen: 44/44 bestanden, 0 rot“ (je 11, inkl. G2-Tests und G3 stationRegistry/stationPalettes/stationLayouts).
- Client-Smoke `module-simplecontainers-client-263` (xvfb): „alles gruen: 22/22 bestanden, 0 rot“; G3-Screenshots
  `/root/previews/simplecontainers/w1-g3/` (+ `crop-*` 2-fach). SB-Schmiedebildschirm mit Rezeptbuch erscheint gestylt
  (Alias greift, SB im Client geladen), TrimStatsPanel und Rezeptbuch-Knopf im Inventar sichtbar.
- Nicht getestet: NeoForge/Forge-Client-Sicht, echte Blöcke/Dorfbewohner (Smoke öffnet Menüs clientseitig: keine
  Amboss-Namensübernahme, kein Schmiede-Fehlerkreuz, keine Banner-Vorschau), Klicks auf Kacheln/Scrollbalken, Rezeptbuch
  geöffnet (verschobenes `leftPos`), simplevisuals-Amboss-Label (dunkler Text auf dunklem Kasten möglich).
