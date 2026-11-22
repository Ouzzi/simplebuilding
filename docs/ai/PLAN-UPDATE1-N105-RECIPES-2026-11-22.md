# Plan Update 1 – N105 „Simple Recipes“ (Sitzung B, 22.11., NUR PLAN)

> Status: **nur Plan vor dem Erst-Release** (O6-2). Nichts bauen, bis der Besitzer Update 1 freigibt.
> Übersicht und Reihenfolge der vier Update-1-Pläne: `docs/ai/PLAN-UPDATE1-UEBERSICHT-2026-11-22.md`.

Grundlage: Konzept `/root/sb-tools-a/n105-simplerecipes.md` (§1–10), Mock `/root/sb-tools-a/previews/n105.html`
(Runde 1 = `n105-v1.html`), Auftrag `/root/b-auftrag-20261109.md` Abschnitt „## N105 Simple Recipes“, Memory
`n105-recipes-offen` / `n105-simplerecipes-offen`. Branch (später): **b-recipes105**, Vorschau **N105B** (Spielbilder).

## 1. Ziel und Besitzer-Entscheidungen (verbindlich, nicht erneut fragen)
Vanilla-nahe Rezeptanzeige statt JEI: Lesezeichen-Reiter am linken Rand aller Container-Screens, Fenster in voller Höhe,
Tabs Alle Items / Lesezeichen / Crafting, Suche, Rezept- und Verwendungsansicht über dem Inventar, Freischaltung wie
das Vanilla-Rezeptbuch.

| Code | Entscheidung (Besitzer 19.11., Runde 1 + 2) |
|---|---|
| **V2 + V3** | beide bauen, umschaltbar per Client-Config `panelStyle = custom \| translucent`; **Dev-Schalter im GUI-Fenster** (nur im Dev-Modus sichtbar) schaltet live um |
| **Z1** | ein globaler Wert „linkes Panel“ = keins \| Rezeptbuch \| Simple Recipes; letzte bewusste Wahl gilt überall |
| **A1** | Schalter „auch nicht gefundene“ nur mit Cheats bzw. im Kreativmodus |
| **F1** | Rezept-Freischaltung wie Vanilla: eine Zutat reicht; plus: direkt erhaltenes Item schaltet sein eigenes Rezept frei |
| **K1** | Linksklick = Rezept, Rechtsklick = Verwendungen, Shift-Klick = Lesezeichen |
| **J1** | JEI-Brücke, wenn JEI installiert ist (JEI-Laufzeit auslesen, im eigenen Stil zeigen) |
| Runde 2 | Rezept-Overlay deckt die **ganze** Container-UI ab (gleiche Größe/Position); Suche **ganz unten** bündig; Tabs/Schalter als **Icons mit Tooltip**; **kein Layoutshift** beim Tab-Wechsel |
| Plattform | nur 26.3 Fabric; releasePhase nach dem Erst-Release (Vorschlag A: erstes Update) |

Grenzpunkte §7 des Konzepts gelten mit den dort genannten Vorschlägen (Verwendungen ja, Verlauf ja, Info-Seiten aus
RecipeViewer ja, Mod-Name im Tooltip ja, Narration ja; Cheat-Geben, Mengenrechner, Mod-Maschinen-Transfer **später**;
Kreativmodus schaltet nichts dauerhaft frei; bestehende Welten starten mit dem Wissen des Vanilla-Rezeptbuchs).

## 2. Ist-Zustand (erhoben 22.11. auf claude-wave1 bc6b916725)
- **Kein Branch, kein Code.** `b-recipes105` existiert nicht; Modul `simplerecipes` gibt es nicht.
- `simplelib` `com.simplelib.api.RecipeViewer`: JEI-freie Liste (In-World-Upgrades, Special Ways, Stationen, Mob-Drops);
  heute nur von `compat/jei/SimpleLibJeiPlugin` + Kategorien gelesen, Gametests `RecipeViewerTests`. 19 Java-Dateien mit
  JEI-Plugins in den Modulen. → Datenquelle für unsere Mods ist schon da.
- UI-Bausteine in `simplelib/api/client/ui/`: `UiBoxes`, `UiPalette`, `UiContrast`, `UiMargin`, `UiStyleToggle`,
  `UiSymbols`, `UiSlotIcons`, `UiFilterButton`, `UiBookmarks`, `UiShiftTooltip`, `UiKeyboard`; Dev-Schalter-Muster
  `DevVariants` + `DevVariantsPanel` (nur sichtbar bei `DevVariants.enabled()`, seit b-devoff19 Standard AUS).
- Rezeptbuch-Eingriffe existieren schon in `simpleinterfaces` (`CraftingBackgroundMixin`, `FurnaceBackgroundMixin`) und
  `simplemodels` (`ModelRecipeBookAccessor`) → Kollisionsstellen mit dem „gegenseitigen Ausschluss“.
- Neue Module: `tools/newmod.py`; Muster für „nur Fabric, nach Erst-Release“: `simplelayers` (`loaders: [fabric]`,
  `releasePhase: 3`, `release: false`).
- Vanilla 26.3: Server schickt seit 1.21.2 nur Rezeptbuch-Anzeigen freigeschalteter Rezepte → Simple Recipes muss auch
  serverseitig laufen (Konzept §5).

## 3. Entscheidungen von B (im Plan begründet)
1. **Sub-Mod `simplerecipes` unter Simple Quality of Life** (`parent: simplequalityoflife`, `bundle: optional`,
   `requires: [simplelib]`, `optional: [jei]`, `loaders: [fabric]`, `releasePhase: 2`, bis zur Freigabe `release: false`).
   Optional statt required, damit QoL-Nutzer mit JEI nichts doppelt bekommen und das QoL-Paket des Erst-Release
   unverändert bleibt.
2. **Gemeinsames nach SimpleLib**: `UiSideTab` (Reiter-Knopf am Rand) und `UiSlidePanel` (Fenster in voller Höhe,
   150 ms) als UI-Bausteine; alles Rezept-Spezifische bleibt im Sub-Mod.
3. **Server-Teil im selben Jar**: „gefunden“-Bitset je Spieler (Fabric Data Attachment, persistent), Rezept-Anzeigedaten
   als eigenes Paket (S2C) für freigeschaltete Rezepte; ohne Server-Mod nur Vanilla-Rezeptbuch-Wissen (Grenzpunkt 10).
4. **Lesezeichen** clientseitig in `config/simplerecipes/<welt-id>.json`; Fenster-Zustand (Z1) + `panelStyle` in der
   Client-Config (Cloth Config wie die anderen Mods).
5. **JEI-Brücke (J1)** als eigener Paketteil `compat/jei`, geladen nur mit JEI; Kategorien mit eigener Zeichnung werden
   im ersten Schritt als JEI-Rendering in unserem Rahmen gezeigt; die JEI-Item-Liste per Config ausblendbar.
6. **„+“-Übertragen** nur Werkbank, Inventar-Gitter, Öfen über den Vanilla-Weg (Ghost-Rezept / Auto-Befüllung).

## 4. Arbeitspakete (Reihenfolge)
| # | Paket | Aufwand |
|---|---|---|
| P1 | Modul anlegen (`tools/newmod.py`), modules.json-Eintrag (nur Fabric, release:false, releasePhase 2), Config, Schalter, leere Gametest-Klasse, Wiki-Seite Gerüst | S |
| P2 | SimpleLib-Bausteine `UiSideTab` + `UiSlidePanel` (V2 custom + V3 translucent, Kontrast ≥ 4,5:1, Container-Margin, stufige Ecken), Gametest-frei, Client-Probe-Szene | M |
| P3 | Server: „gefunden“-Bitset + Sync, Freischaltung F1 (eine Zutat + direkt erhaltenes Item), `doLimitedCrafting`, Kreativ ohne Dauer-Freischaltung, Start-Wissen aus dem Vanilla-Rezeptbuch; Gametests | M |
| P4 | Index + Suche (Name, `@mod`, `#tag`; 10 000+ Items), Tabs Alle/Lesezeichen/Crafting, Schalter „nur herstellbar“, A1-Schalter, Verlauf, Lesezeichen ziehen, feste Höhen (kein Layoutshift) | L |
| P5 | Rezept-/Verwendungsansicht über der ganzen Container-UI: Kategorie-Reiter (Werkbank → Ofen → … → Mod-Kategorien), Blättern, Tag-Rotation, „Noch kein Rezept entdeckt“, Info-Seiten aus RecipeViewer, „+“-Übertragen | L |
| P6 | Z1 gegenseitiger Ausschluss mit dem Vanilla-Rezeptbuch (inkl. `simpleinterfaces`-/`simplemodels`-Mixins), Tasten A/R/U/Backspace/Strg+F (umbelegbar, `A` nur über Item), Mod-Name im Tooltip, Narration, Dev-Schalter V2/V3 im Fenster | M |
| P7 | JEI-Brücke J1 (Kategorien + Plugins fremder Mods, JEI-Liste ausblendbar), RecipeViewer-Einträge aller Simple-Mods direkt lesen | M |
| P8 | Texte EN/DE, Wiki/Guide/Fragebuch, Store-Seite (release:false-Kennzeichnung wie Layers), Vorschau N105B mit Spielbildern | S |

Summe: **≈ L+ (≈ 8–11 Agenten-Tage)** – größtes der vier Vorhaben.

## 5. Risiken
- **Kompatibilität:** Kollision mit JEI/REI/EMI-Overlays und dem Vanilla-Rezeptbuch; Mixins in `simpleinterfaces`
  (Crafting/Furnace-Hintergrund) und `simplemodels` greifen an denselben Screens → P6 zuerst gegen diese Mixins
  prüfen, eine gemeinsame „linkes Panel“-Abfrage in SimpleLib statt doppelter Mixins. JEI-API-Änderungen brechen die
  Brücke → Brücke strikt optional, Fehler je Kategorie loggen und überspringen (wie RecipeViewer).
- **Welt-Migration / IdMoves:** keine Ids betroffen (neuer Mod, keine Blöcke/Items). Neue Spieler-Daten (Attachment)
  ohne Altbestand; Entfernen des Mods lässt nur ein unbekanntes Attachment zurück (Fabric verwirft es). Kein IdMove nötig.
- **Texturen / Vorschau-Karten:** Reiter-, Tab- und Schalter-Icons sind neue Pixel (eigene, keine Mojang-Pixel) →
  ein Abnahme-Block in **N105B** (eine Karte, Einzelpunkte auf der Detailseite: V2/V3, Icons, Overlay).
- **Performance:** Suchindex über alle Items beim Laden (Ziel < 200 ms bei 10 000 Items, inkrementell bei Tag-Reload);
  Bitset-Sync nur bei Änderung (Delta-Paket); Rendering nur sichtbarer Zeilen; Rezept-Anzeigedaten nur für freigeschaltete
  Rezepte senden (Paketgröße messen, Datapack-Server mit vielen Rezepten).
- **Mehrspieler:** fremde Server ohne Mod → eingeschränkt (bewusst, Grenzpunkt 10); Text im Fenster erklärt es.

## 6. Testplan
- **Gametests** (`module-simplerecipes-fabric-263`, normal + standalone): Freischaltung F1 (eine Zutat, direkt erhaltenes
  Item), `doLimitedCrafting`, Kreativ ohne Dauer-Freischaltung, Bitset speichern/laden, Sync-Paket nach Änderung,
  Start-Wissen bestehender Welten, RecipeViewer-Vollständigkeit (jede Simple-Mod hat Einträge), A1-Rechteprüfung.
- **Client-Probe** `modules/simpleinterfaces/tools/uiprobe_n105.py` (eine Datei, Hauptsitzung fährt sie): echte Maus nur
  innerhalb von Screens (`s.click_widget`/`s.click_slot`, keine Klicks nach `tp … facing`); Szenen: Reiter öffnet/schließt
  (Inventar, Truhe, Werkbank, Ofen), Z1-Zustand über Screen-Wechsel, Tabs ohne Layoutshift (Widget-Positionen vor/nach
  vergleichen), `A`-Lesezeichen, Ziehen in der Lesezeichen-Liste, Rezept-Overlay deckt die ganze UI, „+“ füllt das Gitter,
  V2/V3 per Dev-Schalter, GUI-Skala 4 (weniger Spalten). Probe-Lehren beachten: Welt startet Friedlich, kein Audio,
  Hauptand leeren vor `give`, `random_tick_speed 0`.
- **Dev-Klicktest** `/root/n74dev/n105_wiki.cjs`: Wiki-Seite Simple Recipes DE+EN, Store-Seite, keine „?“, keine
  Konsolenfehler (nur ein Chromium-Lauf zugleich).
- **bprep** (`bprep-a.sh b-recipes105` → „BPREP GRUEN“), danach Bündel durch das Gate (modules.json ändert sich → kein Express).

## 7. Vor dem Bau vom Besitzer zu klären
1. **Name + Einordnung bestätigen:** „Simple Recipes“ als optionaler Sub-Mod unter Simple Quality of Life (nicht
   ausdrücklich bestätigt; B-Vorschlag gilt, wenn kein Einwand).
2. **Release-Zuordnung:** erstes Update nach dem Erst-Release (Vorschlag) – oder später?
3. **Standard-Stil:** welcher der beiden Stile ist Standard, V2 (custom) oder V3 (transluzent)? Vorschlag V2.
