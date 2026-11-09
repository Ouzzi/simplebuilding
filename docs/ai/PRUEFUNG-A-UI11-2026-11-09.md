# Pruefung A-UI11 (09.11.2026): Simple Interfaces (W6), Simple Trims (W7), Riss-Splitter-Block (W13)

Branch a-ui11 (Basis claude-wave1), NUR statisch geprueft und gebaut, UNGETESTET (sb-test seit 09.11. 10:00 UTC nicht erreichbar).
Quellen: PRINZIPIEN A7/A8b/A11, docs/ai/UI-BAUSTEINE.md, SimpleLib-UI-Klassen (UiBoxes, UiPalette, UiContrast, UiBrightness, UiFilterButton), tools/check_ui_contrast.py (lief: ok, 33 Dateien).

## W6 Simple Interfaces: Screens gegen das Schema

Schema = Kasten mit stufigen Ecken (UiBoxes), Palette (UiPalette), Text >= 4,5:1 (UiContrast, auch in der Helligkeit dunkel), gleiche Knoepfe/Filter (UiFilterButton), Vanilla-nahe Optik.
Erfasst: 55 Screen-Klassen (ohne Mixins/Tests, Kern und Module) plus 1 Gruppe der Vanilla-Screens, die Simple Interfaces umzeichnet. Ampel: 33 gruen, 10 gelb, 0 rot.

| Screen | Mod | Ampel | Befund | Aufwand |
|---|---|---|---|---|
| CrucibleScreen | simplecrucibles | gruen | UiBoxes.box + UiPalette, Referenz des N12-Stils | - |
| FletchingScreen | simplearrows | gruen | UiBoxes/UiMotifs/UiStyleToggle, PANEL #C6C6C6 | - |
| ShapecutterScreen | simpleblocks | gruen | UiBoxes + UiPalette; SawPalette.LABEL #404040 auf #C6C6C6 (7,6:1), Test saw-Kontrast vorhanden | - |
| EnchanterScreen | simpleenchanting | gruen | UiBoxes + eigene dunkle Violett-Palette (PANEL #1C1529, Text #EDE4FA >= 12:1, TEXT_OFF #A99FBC 7,0:1); Titel ohne Stil-Schalter #404040 nur im Aus-Fall | - |
| FoundryScreen | simplefoundry | gruen | UiBoxes, dunkles Feld #2B2B2B, TEXT_DIM #C8C8C8 8,5:1 | - |
| TierHopperScreen | simplemachines | gruen | UiBoxes + UiFilterButton (Filter-Prinzip) + UiMotifs | - |
| PipeScreen | simplestorage | gruen | UiBoxes + UiFilterButton; OUT_INK/OUT_FACE 8,4:1, OFF 5,8:1 | - |
| ControllerScreen | simplestorage | gruen | UiBoxes/UiSymbols; RED #FF6B5E auf dunklem Feld #2A1F1D | - |
| BackpackScreen | simplestorage | gruen | UiBoxes + UiPalette je Stufe/Farbe (Rucksack) | - |
| StorageCraftingScreen | simplestorage | gruen | UiBoxes + UiStyleToggle | - |
| TieredChestScreen | simplestorage | gruen | UiBoxes + UiMotifs je Tier | - |
| BurnScreen | simplemusic | gruen | UiPalette.derived(#6B4A35), Text 6,8:1, Fehler 4,8:1 (Abnahme 23.10.) | - |
| SheetMusicScreen | simplemusic | gruen | zweites Fenster im Jukebox-Kasten, UiCodeArea | - |
| DiscPressScreen | simplemusic | gruen | UiBoxes + UiStyleToggle | - |
| PictureMapScreen | simplemaps | gruen | UiBoxes + UiContrast | - |
| WayfinderScreen | simplemaps | gruen | UiBoxes/UiBookmarks/UiContextMenu (Referenz der Bausteine); Status-Farben auf Braun 4,7-6,2:1 | - |
| ModelBrowser / ModelsTab | simplemodels, simplevisuals | gruen | UiBoxes + UiStyleToggle | - |
| QuestionaryScreen | simplequestionary | gruen | Buch-Stil: UiBoxes/UiBookmarks/UiMotifs, Papier #F0E4C6; INK_FAINT nur Zierlinien (kein Text) | - |
| ItemStatsScreen | simplestats | gruen | UiBoxes; dunkle Liste #373737, Werte >= 8,7:1 | - |
| DownedScreen | simplerespawn | gruen | UiBoxes; Texte #C0C0C0/#80FF80 auf dunklem Grund | - |
| ShopScreens (Kauf/Verkauf) | simpleshops | gruen | UiBoxes + UiPalette | - |
| DeathVaultScreen | simpletools | gruen | UiBoxes + UiContrast.ensure (WARN/Dim); PAUSE-Balken ist Grafik, kein Text | - |
| DeedScreen / DeedCardScreen / DeedMapScreen / DeedTradeScreen | simpletweaks | gruen | UiBoxes/UiBookmarks, DeedStyle (Papier #E2D3AA, INK_OK/ERROR/SOFT aus UiContrast) | - |
| InvseeScreen | simpletweaks | gruen | UiBoxes + UiPalette (Admin-Ansicht) | - |
| AutoSmitherScreen | SimpleBuilding-Kern (common) | gruen | ModScreenStyle -> UiBoxes/UiPalette (AUTO_SMITHER), Filter ueber UiFilterButton | - |
| AutonomousCrafterScreen | SimpleBuilding-Kern | gruen | ModScreenStyle, UiFilterButton in AUTONOMOUS_CRAFTER-Palette | - |
| BackpackScreen (Kern) | SimpleBuilding-Kern | gruen | ModScreenStyle.backpack(..) -> UiPalette; eigene PNG nur Fallback | - |
| NetheriteHopperScreen | SimpleBuilding-Kern | gruen | ModScreenStyle.hopper + hopperFilterButton (= UiFilterButton) | - |
| TieredChestScreen (Kern) | SimpleBuilding-Kern | gruen | ModScreenStyle.tieredChest; eigene Fuellungen nur als Fallback bei ACTIVE=false | - |
| OctantScreen | simplebuildingtools / Kern | gruen | ModScreenStyle + UiBrightness + UiContrast.ensure (LABEL_COLOR wird gegen den Grund geprueft) | - |
| BuildingWandScreen, TrimReferenceScreen | simplebuildingtools / Kern | gruen | ModScreenStyle.panel/label (Kasten-Stil N43), Vanilla-Knoepfe | - |
| SelectChallengeScreen | simplechallenges | gelb | UiBoxes ja, aber Text als feste Literale (#404040, #4A4A4A, #8B1A1A) auf UiPalette.INVENTORY: in Helligkeit 'dunkel' (UiBrightness) ~1,9:1 -> BEHOBEN: label()/UiContrast.ensure gegen INVENTORY.fill() | behoben (S) |
| StartAreaEditorScreen | simplechallenges | gelb | wie oben (Titel/Zaehler/Hilfe feste Literale) -> BEHOBEN | behoben (S) |
| EarthScreen | simpleearth | gelb | Titel/Beschreibung #404040/#4A4A4A auf INVENTORY-Kasten, in 'dunkel' unlesbar -> BEHOBEN (Titel label(), Rest ensure); Listenzeilen auf eigenem hellen Feld #D6D6D6 bleiben dunkel (korrekt) | behoben (S) |
| GuideBookScreen | SimpleBuilding-Kern | gelb | Buch-Textur (gewollt, kein UiBoxes). Seitenzahl INK_FAINT #A08A6A auf Papier #EFE4C6 nur 2,6:1 -> BEHOBEN: INK_SOFT (4,5:1). INK_SOFT auf dunklerem Papier #E3D4AE nur 3,9:1: wo gesetzt, im Client pruefen | behoben (S); Rest (S) |
| ModpackGuideScreen | simplelib | gelb | wie GuideBookScreen (gleiche Seitenzahl-Farbe) -> BEHOBEN | behoben (S) |
| BlueprintScreen | simplebuildingtools / Kern | gelb | eigenes Papier-/Tinten-Design (PLAN-BLAUPAUSE-UI-N23), kein UiBoxes: gewollt; Texte INK 7,6:1, INK_SOFT/ERROR/OK je >= 5,3:1 auf Papier (im Code kommentiert) | - (gewollt) |
| BlueprintIoScreen, BlueprintPictureScreen | simplebuildingtools / Kern | gelb | Modal im Papier-Stil, Vanilla-Knoepfe, kein UiBoxes: gewollt, gleiche Papierfarben wie BlueprintScreen | - (gewollt) |
| StrongBeaconScreen | simplebeacons | gelb | Vanilla-Beacon-Textur + eigene Sprite-Knoepfe (Vanilla-Look, kein UiBoxes); DIM #8A8A8A auf #212121 4,7:1 (knapp, vom Lint tools/check_ui_contrast.py geprueft) | - (Vanilla-nah) |
| KeysScreen, KeyCellScreen | simpleinterfaces | gelb | Vanilla-Optionsseiten-Stil (Listen, Vanilla-Knoepfe, weisse Schrift auf dunklem Vanilla-Grund), kein UiBoxes; Kontrast >= 7:1; Tastatur-Bild ueber UiKeyboard | - (Vanilla-konform) |
| ViewOnlyInventoryScreen | simplerespawn | gruen | erbt Vanilla-Inventar, nur Ansicht; Simple Interfaces faerbt es wie das Inventar | - |
| Vanilla-Screens: Truhe, Faesser, Ofen, Werkbank, Verzauberung, Amboss, Schmiede, Stein-/Webstuhl, Kartografie, Schleifstein, Brauen, Haendler, Crafter, Trichter, Spender, Reittiere, Leuchtfeuer | simpleinterfaces (Mixins) | gruen | alle von Simple Interfaces ueber *Background/Station-Mixins im Kasten-Stil umgezeichnet | - |
| Lucke: Kreativ-Inventar (CreativeModeInventoryScreen), Rezeptbuch-Seiten, Buecher/Schilder | simpleinterfaces | gelb | nicht umgestylt (kein Mixin); Vanilla-Optik bleibt: Besitzer-Frage, ob Kreativ-Inventar mit soll | M (Besitzer) |

### Befunde, die nicht ins Schema passen
1. **Feste Textfarben auf Paletten-Kaesten (3 Screens, behoben):** UiBrightness 'dunkel' tont den Kasten auf ~#53 ab, `label()` schaltet mit, feste Literale (#404040) nicht: SelectChallenge, StartAreaEditor, Earth. Fix: `UiPalette.INVENTORY.label()` bzw. `UiContrast.ensure(farbe, INVENTORY.fill())`.
2. **Buch-Seitenzahl 2,6:1 (2 Screens, behoben):** GuideBookScreen und ModpackGuideScreen (INK_FAINT -> INK_SOFT, 4,5:1). Der Lint kennt die Papierfarbe nicht (kein FACE-Name); INK_SOFT auf dem dunkleren Papierton #E3D4AE hat nur 3,9:1 (im Client pruefen, ob dort Text steht).
3. **Eigener Stil, gewollt (Gelb, kein Fix):** Blaupause (Papier), Guide-Buch (Textur), Beacon (Vanilla-Textur), Tastenbelegung (Vanilla-Optionsseite).
4. **Lucke:** Kreativ-Inventar und andere Vanilla-Screens ohne Mixin (Besitzer-Frage, Aufwand M).
5. Kern-Duplikate: `common/` (26.2/26.3 gemeinsam) und `modules/simplebuildingtools` fuehren dieselben Screens; SB-Kern-Screens gehen alle ueber ModScreenStyle in die SimpleLib-Bausteine.
6. Filter-Prinzip: alle Filter-Screens (TierHopper, Pipe, Kern-Trichter, Autonomer Crafter) nutzen UiFilterButton; kein Abweichler.

Nicht geprueft (sb-test aus): Client-Sicht per menu_probe.py/Screenshots. Offen: nach Rueckkehr Challenges, Earth, Guide in 'dunkel' ansehen.

## W7 Simple Trims
| Bereich | Ampel | Befund |
|---|---|---|
| Funktionen | gruen | 4 Vorlagen (Leuchtend, Strahlend, Pulsierend, Invertierend), Platzieren, Hammer/Axt-Schmieden, Aufwertungen als echtes Schmiederezept (TrimUpgradeRecipe), Strahlkraft-Licht mit Aufraeumen verwaister Lichter; 22 Modul-Gametests (inkl. trim_inversion, smithing_upgrades, orphan_light_swept), Fabric-Adapter und check_data.py: vollstaendig, nicht gelaufen (sb-test aus) |
| Rezepte | gruen | je Vorlage Aufwertungs- und Duplizierungsrezept (Diamant + Material + Vorlage), Pulsierend zusaetzlich Werkzeug-Rezept; alle Zutaten existieren |
| Texturen/Modelle | gruen | 4 Item-Texturen mit Layer-Quellen + Item-Definitionen/Modelle, 27 Platziert-Modelle, Guide-Buch |
| Lang | gruen | EN/DE je 67 Schluessel, paarig, keine Java-Schluessel ohne Eintrag |
| Guide/Wiki | behoben | Seiten 1-2 und Wiki-Uebersicht nannten nur 3 Vorlagen, die Invertierende fehlte -> manual.json EN+DE ergaenzt, Guide-Lang per tools/guides/module_guides.py neu erzeugt |
| JEI | gruen | TrimsJeiPlugin zeigt Platzieren/Aufwerten (InWorldUpgrade) auch fuer Strahlend/Leuchtend/Invertierend |
| Beschaffung | gelb | Invertierende Vorlage: weder Handelslink noch Preis in Simple Money (Leuchtend/Strahlend/Pulsierend haben beides), Rezept nur Duplizieren -> ohne Kreativ nicht erhaeltlich. Besitzer-Frage: Preis/Handel (Vorschlag Ink/Obsidian-Stufe) oder Schmiede-Kombination |
| Altlast | gelb | mc26_3/overlay/.../lang/de_de.json fuehrt noch item.simplebuilding.*_trim_template-Schluessel (alte SB-Ids), von IdMoves ersetzt, tote Eintraege; nicht angefasst (Overlay-Bereinigung ist eigene Aufgabe, Risiko gering) |
| Tests | gelb | Neue Tests nicht noetig; Lauf steht aus: /root/sbt a-ui11 module-simpletrims fabric+standalone |

## W13 Riss-Splitter-Block
Das Rezept/Block-System bleibt unveraendert, nur die Optik der drei Bloecke (rift_shard_block, nether_, end_). Generator `tools/textures/rift11_2026_11_09.py` (nutzt Palette und Weichzeichner von dims09/dims10, keine Vanilla-Pixel). Alt: fuenf kleine Kristalle (Hartanteil 7 %). Neu, je ca. 5 % hart, mit Rahmen wie Terranit/Infernit/Zenithit:
- **A Kristallbett:** drei grosse diagonale Kristalle im dunklen Bett (eingebaut).
- **B Bruchstuecke:** acht Facetten gepresster Splitter, Fugen leuchten.
- **C Riss:** ein gezackter, leuchtender Riss mit Halo.
Vorschau: /root/sb-tools-b/previews/a-ui11-rift-preview.html, Antwortcode `RIFT11: tex=A|B|C|alt`. Wechsel: `python3 tools/textures/rift11_2026_11_09.py --apply B`. Rueckfrage: Das Besitzer-Wunschbild ist unbekannt (nur "anders haben"); falls es um Form/Modell geht, nicht nur Textur, bitte melden.

## Offen
- `/root/sbt a-ui11 "<compile :mc26_3:fabric + module-simpletrims/-challenges/-earth/-simplelib fabric+standalone + simplebuilding:data_integrity* + checkWiki>"` sobald `timeout 30 ssh -o ConnectTimeout=10 sb-test uptime` antwortet, EIN Lauf.
- Client-Sicht der drei behobenen Screens in Helligkeit 'dunkel'; Rift-Wahl RIFT11; Invertierend-Beschaffung (Besitzer).
- Wiki-Texturblaetter der Rift-Bloecke (wiki/assets, generiert) und Guide-Seiten der anderen Module bleiben dem Bundle-Generator ueberlassen.
