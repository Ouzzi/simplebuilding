# N77 Teil 2+3 – Besitzer-Anordnung der Metall-Linien übernehmen + „eindeutig fehlt“ (2026-11-16)

Branch `b-tz77` (Basis `b-bundle203-gen` = 9b9af8c099), Hub `/root/sb.git`. Auftrag: Abschnitt N77 in
`/root/b-auftrag-20261109.md`, Material `/root/sb-tools-a/tz-owner-1611/` (layout-scan.txt, screen-79..82.webp).
Teil 1 (Scanner mit Item-Frames) macht eine andere Session auf `b-tz77s`; Blaupausen-Code hier unangetastet.

## Phase 1 – Ist-Zustand (geprüft 16.11.)

- Scan dekodiert mit der Logik des Blaupausen-Codes (`$a = id`-Aliase, `id x,y,z`-Bereiche `a..b`): 30×3×119, 6896 Blöcke
  (Zahl stimmt). Es ist **genau der Bereich `lines/metals`** (Seite A der Linien): graue Mittellinie z=116, Steh-Schilder
  z=115/117, Reihen x=2..29 = die 28 Metall-Reihen der Config in derselben Reihenfolge (copper … amethyst), Spalte
  `c = 114 − z` = die 115 Config-Spalten (button … special_10). Boden y=0 Glatter Stein/Seelaternen, y=1 Inhalt
  (hellgraue Betonstufe = `#light`, rote = `#red`, Block = gesetzt, **Luft = Loch für einen Rahmen am Boden**), y=2
  Achtel (tops), obere Türhälften, Lichtblöcke.
- Rahmen fehlen im Scan (Teil 1). Inhalte aus den Screenshots: 79 = Netherit/Enderit-Reihen Spalten ~79–113 plus
  Eisen/Diamant 100–103 (Dino-Rüstungen); 80 = Spalten 53–75 (Behälter/Öfen der Stufen, Äpfel/Karotten, Eimer);
  81 = Ausrüstung 31–52 aller Metalle (Roh/Barren/Nugget-Spalten rechts). 82 zeigt Stein/Rest (nicht im Scan).
- Abgleich Scan ↔ Config `src/main/resources/data/simplebuilding/testcentre/layout_lines_metals.json`: 2746 Zellen gleich,
  474 anders (davon ~400 Rahmenlöcher, deren Config-Item bleibt). Echte Änderungen des Besitzers: Behälter/Öfen/Kolben/
  Kessel der Stufen (verstärkt, Netherit, Enderit) als Blöcke in Spalten 58–75 (verstärkte neu in den Linien), Kupfer-
  Golemstatue/Truhe/Kabel/Fallentruhe nach 53/60/61/63, Fackeln (Kupfer, Redstone) nach 67, Silberspiegel nach 35,
  Rohblock-Spalte Pressling-Blöcke (Rosé), Dino-Rüstungen Eisen/Diamant nach 100–103, Netherit/Enderit Ofenlore/-boote
  nach 99–102 und Farbkasten nach 86, „Splitter“ in Roh/Nugget (Netherit-Schrott → Roh, Diamant/Smaragd: rissig → Roh,
  Kiesel → Nugget, Weisheitskristall-Splitter → Nugget, matter Astralit-Staub/Nihilith-Splitter → Roh), Eimer (Vanilla)
  in Eisen 54–56; neue rote Felder (fehlt): Tier-Rüstung Kupfer/Gold/Rosé/Netherit/Enderit, Rosé Pferd/Nautilus/Hufeisen/
  Speer/Meißel/Baustab, Dino-Rüstung Gold/Rosé, Smaragd-Stange, Nuggets + verzauberter Apfel Terranit/Infernit/Zenithit,
  Golem-Statue/-Kopf Eisen/Enderit/Messing/Bronze/Edelstahl.
- Code: `LayoutConfig` liest die Config, `OrderAreas.configured` baut danach. **Befund:** `#red` wirkt nur in den festen
  Slot-Spalten (bis `boots`); in den Zusatzspalten (53+, special) baut das Spiel hellgrau, und `configProblems` prüft dort
  nicht – die Besitzer-Anordnung hat aber rote Felder in 53, 57, 65, 100–103.
- Fehlende Items (Teil 3): keines existiert (`emerald_pole`, `{terranite,infernite,zenithite}_nugget`,
  `enchanted_{…}_apple`, Rosé-Pferde-/Nautilus-/Tier-/Dino-Rüstungen, Hufeisen, Speer, Meißel, Baustab, Beton-/Karo-Mauern).
  Simple Ores ist tabellengetrieben (`OresRegistry`, `EnderiteGear`, `tools/gen_data.py`, `check_data.py`); Texturen
  eigener Art als `*.layers.json` + PNG (`tools/textures/layers.py`), Äpfel nutzen die Textur des Grund-Apfels + Glanz.
  Stein-Config hat `white_concrete` wall `#red` und `blackstone_quartz_checker` wall `#red`.

## Plan

### Teil 2 – Config
1. `tmp`-Skript (nicht eingecheckt) baut die neue `layout_lines_metals.json` aus Scan + bisheriger Config:
   Block → Id (Blockzustand weg, `redstone_wire` → `redstone`), Stufen → `#light`/`#red`, Achtel y=2 → `tops`,
   Luft → Rahmen: bisheriges Item der Zelle, außer es steht jetzt als Block woanders (dann laut Screenshot oder `#light`);
   neue Rahmen laut Screenshot (Liste unten). Spalten 53+ nach neuem Inhalt umbenennen (Namen sind nur Beschriftung).
   Geschrieben mit `tzedit_save.layout_text`, geprüft mit `check_save`.
2. `OrderAreas`: `#red` auch in Zusatz-/Sonderspalten rot bauen (Platzhalter `missing`), `configProblems` prüft das in allen
   Spalten; `TestCentreDump.writeLines` schreibt dort `MISSING`.
3. Wiki-Kopien `tools/tzedit/build_editor.py`; Items-Bereich-Exporte bleiben (Spiel baut Items algorithmisch).

### Teil 3 – Umfang (selbst entschieden)
- **Bauen (Simple Ores):** `emerald_pole` (Stange wie Diamant, Smaragd-Reichweite), `terranite_nugget`, `infernite_nugget`,
  `zenithite_nugget` (9↔1 Barren), `enchanted_terranite_apple` / `_infernite_` / `_zenithite_` (wie verzauberter Rosé-Apfel:
  Grund-Apfel-Textur + Glanz, Rezept 8 Blöcke + Apfel wie die Geschwister). Neue eigene Texturen: Stange + 3 Nuggets
  (Umfärbung der eigenen Messing-Nugget-/Diamant-Stangen-Form mit der eigenen Barren-Palette) → „Abnahme offen“, Vorschau
  `/root/sb-tools-b/previews/n77.html` (Antwortcode N77).
- **Bauen (ohne neue Textur):** Beton-Mauern (16 Farben, Simple Blocks) und Quarz-Karo-Mauern (alle Karo-Farben, Simple Chess),
  Texturen = die vorhandenen Block-Texturen (Verweis, keine Pixel).
- **Zurückgestellt (Memory):** Rosé-Varianten + Tier-/Dino-/Pferde-/Nautilus-Rüstungen aller Stufen, Rosé-Hufeisen/Speer/
  Meißel/Baustab (je eigene Entity-Ebenen-Texturen, Werte/Tier-Logik in 5 Modulen – eigene Welle), Metall-Fackeln,
  Golem-Statuen/-Köpfe (N77K Konzept, erst nach Antwort).
- Was gebaut ist, ersetzt in der Config das `#red`; Mauern in der Stein-Config.

## Risiken
- Rahmen-Inhalte nur aus Screenshots (niedrige Auflösung) – unsichere als Abweichung gelistet; nach dem neuen Scan (Teil 1)
  vergleichen.
- Neue Items: Daten in drei Versionen/Loadern; Generatoren + check_data müssen grün bleiben.

## Verifikation
Plan-Abgleich; `tzedit_save.py check`; `build_editor.py --check`; Gametests `fabric-263` Filter `*test_centre*`,
`integration-263` Filter `sbintegration:cross_mod*test_centre*`; Modul-Tests simpleores/simpleblocks/simplechess inkl.
Standalone-Ziele; `check_data.py` der Module; precheck.
