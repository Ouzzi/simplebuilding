# Textur-Bewertung nach A8d (b-texrate15, 09.11.)

Auftrag N13 d (Besitzer 09.11.): alle eigenen Texturen von Simple End, den Metallen (Simple Ores) „usw.“ nach der
RIFT14-Methode (PRINZIPIEN A8d) bewerten, rote zuerst überarbeiten. Vorschau mit Thumbnails, Filter und Vorher/Nachher:
`/root/sb-tools-b/previews/b-texrate15-preview.html` (Antwortcode **TEXRATE15**). Plan: docs/ai/PLAN-B-TEXRATE15-2026-11-09.md.

Neu erzeugen: `python3 tools/textures/texrate15_2026_11_09.py --md <tabelle> --json <datei> --preview <html>`
(Messung `tools/textures/texrate.py`, Vanilla-Werte `python3 tools/textures/texrate.py --vanilla`).

## Messwerte (je Textur, erstes Animationsbild, nur deckende Pixel, Größen auf 16×16 normiert)
- **größte Fläche** – größte 4-verbundene Fläche exakt einer Farbe (px)
- **≥6 px %** – Anteil der deckenden Pixel, die in solchen Flächen ≥ 6 px liegen
- **Hauptton %** – Anteil der häufigsten Farbe
- **hart %** – A8b: Anteil benachbarter Paare mit Helligkeitssprung > 40
- **Farben** – Anzahl verschiedener Farben (≤ 3 = flach, > 32 = weicher Verlauf/Blur?)

## Ampel-Regel (Richtwerte, `texrate.RULES`)
| Art | größte Fläche | ≥6 px % | Hauptton % | hart % | Farben |
|---|---|---|---|---|---|
| Block (≥ 95 % deckend) | orange ≥ 20, rot ≥ 50 | orange ≥ 40, rot ≥ 65 | orange ≥ 40, rot ≥ 55 | orange ≥ 15, rot ≥ 30 | rot ≤ 3, orange > 32 |
| Ausschnitt (Items, Türen, Pflanzen) | orange ≥ 34, rot ≥ 60 | orange ≥ 45, rot ≥ 70 | orange ≥ 40, rot ≥ 55 | – (Kontur macht Kanten hart) | rot ≤ 3, orange > 32 |

**rot** = ein Wert erreicht seine rote Grenze oder drei Befunde sind orange; **orange** = ein oder zwei Befunde orange
(größte Fläche + ≥6 px zählen zusammen als ein Befund, sie beschreiben dieselbe Fläche); sonst **grün**.
**grau** = nicht bewertet: Tönungs-/Schnitzmasken, UV-Atlanten und Animationsteile (Redstone-Linien, `*_dyed`,
`*_overlay`, `carved/*`, `placed_*`, Nadeln), die gewollt einfarbig sind (Vanilla `redstone_dust_*` genauso).

### Kalibrierung
- **RIFT14-Entscheidung:** R6 (31 px, 48 % in Flächen) war dem Besitzer „zu flach“ → orange; R9 (6 px, 5 %) → grün.
- **Vanilla 26.3** (`--vanilla`, zur Laufzeit aus dem Client-Jar): 61 typische Baustoffe 29 grün / 29 orange / 3 rot.
  Grün: Bretter, Erze, Endstein, Tiefenschiefer, Ziegel, Schlammziegel, Tuffziegel, Obsidian, Goldblock. Orange
  (bewusst glatt oder hart): Stein, Steinziegel, Eisen-/Netherit-/Kupferblock, Purpur, poliertes Andesit, Amethyst
  (hart 17 %), Bruchstein (hart 20 %). Rot: Quarzblock (72 % in Flächen), geschnittener Sandstein (89 px), Eichenstamm.
  20 typische Items: 18 grün, Ziegel-Item orange, Eichentür-Item rot.
- Orange heißt also „ist die Glätte/Härte Absicht?“ – das entscheidet der Blick (Befund-Spalte). Neue Texturen
  sollen grün sein (A8d).

## Umfang
Getrackte Block-/Item-PNGs für 26.3 (Kern `src/main` + `mc26_3/overlay`, `modules/*/shared`), ohne
`mc1_21_11`-Kopien und Wiki-Kopien: **1986 Texturen**. Familien nach Sichtbarkeit: 1 Simple End (Modul + End-Texturen
im Kern: Astralit, Nihilith, Enderquarz, Enderit, Esche/Dämmerholz, Pads, …), 2 Simple Ores, 3 Simple Blocks +
Kern-Baublöcke, 4 weitere Module, 5 Kern sonstige.
- **Vanilla-abgeleitete** Texturen (`tools/textures/vanilla_derived.py`) sind git-ignoriert und daher nicht dabei.
- **Befund:** `veined_*`, `crystalline_*`, `layered_astralit`, `frosted_nihilith` sind pixelweise Umfärbungen von
  Vanilla-Texturen (astralit_nihilit_alternates_2026_10_03.py, eingecheckt vor der Regel vom 12.10.) → als
  „vanilla-abgeleitet, Altbestand“ markiert, nicht überarbeitet; gehören nach `vanilla_derived.py` (offener Punkt).
- **Gesperrt** (bewertet, nicht angefasst): `chiseled_*` (b-pal15), Simple Fun (b-fun14).

## Ergebnis (Stand nach der Überarbeitung)

| Prio | Familie | rot | orange | grün | grau | häufigster Rot-Grund |
|---|---|---|---|---|---|---|
| 1 | Simple End | 64 | 67 | 73 | 44 | groesste Flaeche |
| 2 | Simple Ores | 10 | 19 | 31 | 0 | groesste Flaeche |
| 3 | Simple Blocks / Baubloecke | 42 | 35 | 34 | 77 | groesste Flaeche |
| 4 | simpleanimals | 1 | 7 | 2 | 0 | in Flaechen>=6 |
| 4 | simplearrows | 7 | 11 | 0 | 0 | Farben |
| 4 | simplebeacons | 4 | 5 | 0 | 0 | groesste Flaeche |
| 4 | simplebuildingtools | 0 | 1 | 30 | 0 |  |
| 4 | simplechisels | 0 | 1 | 7 | 0 |  |
| 4 | simplecrucibles | 12 | 6 | 1 | 0 | groesste Flaeche |
| 4 | simpledimensions | 0 | 2 | 0 | 0 |  |
| 4 | simpledinos | 14 | 6 | 0 | 15 | groesste Flaeche |
| 4 | simpleenchanting | 6 | 6 | 1 | 0 | groesste Flaeche |
| 4 | simpleenddragonfight | 0 | 2 | 1 | 0 |  |
| 4 | simplefoundry | 5 | 13 | 1 | 0 | groesste Flaeche |
| 4 | simplefun | 0 | 2 | 0 | 0 |  |
| 4 | simplegadgets | 0 | 6 | 38 | 17 |  |
| 4 | simplehammers | 0 | 1 | 7 | 0 |  |
| 4 | simplelayers | 19 | 3 | 1 | 0 | groesste Flaeche |
| 4 | simplelib | 1 | 7 | 10 | 0 | hart |
| 4 | simplelights | 92 | 17 | 0 | 0 | groesste Flaeche |
| 4 | simplemachines | 0 | 11 | 9 | 2 |  |
| 4 | simplemaps | 0 | 5 | 0 | 0 |  |
| 4 | simplemobheads | 0 | 1 | 0 | 0 |  |
| 4 | simplemobmanners | 0 | 3 | 0 | 0 |  |
| 4 | simplemobs | 2 | 28 | 15 | 0 | groesste Flaeche |
| 4 | simplemodels | 0 | 1 | 0 | 0 |  |
| 4 | simplemoney | 0 | 6 | 2 | 0 |  |
| 4 | simplemusic | 73 | 20 | 9 | 0 | in Flaechen>=6 |
| 4 | simpleoctets | 1 | 1 | 0 | 0 | groesste Flaeche |
| 4 | simpleovens | 1 | 12 | 3 | 0 | hart |
| 4 | simplepads | 54 | 33 | 16 | 0 | hart |
| 4 | simplequalityoflife | 0 | 1 | 0 | 0 |  |
| 4 | simplequestionary | 3 | 2 | 0 | 0 | in Flaechen>=6 |
| 4 | simpleresonance | 0 | 1 | 0 | 0 |  |
| 4 | simplerespawn | 0 | 1 | 0 | 0 |  |
| 4 | simpleriding | 0 | 9 | 1 | 0 |  |
| 4 | simplesandwiches | 40 | 44 | 157 | 0 | in Flaechen>=6 |
| 4 | simpleshops | 54 | 8 | 1 | 0 | groesste Flaeche |
| 4 | simplestats | 0 | 1 | 0 | 0 |  |
| 4 | simplestorage | 25 | 22 | 9 | 86 | in Flaechen>=6 |
| 4 | simpletools | 5 | 13 | 10 | 0 | in Flaechen>=6 |
| 4 | simpletrims | 0 | 4 | 1 | 0 |  |
| 4 | simpletweaks | 3 | 1 | 1 | 0 | groesste Flaeche |
| 4 | simplewanderers | 0 | 2 | 0 | 0 |  |
| 5 | Kern (sonstige) | 4 | 64 | 146 | 75 | in Flaechen>=6 |

Überarbeitet: 34 rote Texturen (Simple Ores 24, Simple End 10) → 32 grün, 2 orange (Hauptton 44–45 %).

## Blick-Befunde (8×-Kontaktbögen je Familie selbst angesehen)
- **Simple End, End-Paletten** (polished/pillar/block von Astralit, Nihilith, Enderquarz): große einfarbige
  Plattenflächen (69–78 % in Flächen, bis 58 px), Rahmen hell/dunkel richtig, aber innen glatt wie Kunststoff; Vanilla
  polished_* 0–50 % → **überarbeitet**. Ziegel/Grundblöcke: orange, glatt aber lesbar.
- **Pads** (Elytra-/Launch-/Flypads, Teleporter, Chunk-Loader; End und simplepads): Wirbel und Funken mit 30–48 %
  harten Kanten, wirken flimmernd (A8b ~7 %). Rot wegen Härte, nicht wegen Fläche → eigene Pad-Runde nötig
  (Wirbelkontrast in 2–3 Stufen statt Schwarz/Weiß), nicht im Flächen-Brecher lösbar.
- **Lampen-Mäander** (astralit/nihilith_lamp*): große einfarbige Felder + harte Kanten; Vanilla-Redstone-Lampe ist
  ebenfalls rot → orange-würdig, Felder brechen wäre die nächste Runde.
- **Esche/Dämmerholz** (simpleend): Türen, Falltüren, entrindete Stämme, Stöcke-Oberseite mit langen einfarbigen
  Brettern/Streifen; Laub-Kristall hart. Holz braucht Maserung (2–3-px-Striche), keine Haufen → eigene Runde.
- **Simple Ores**: Metall-Baufamilie (cut/tiles/pillar/pillar_top, METALS14-R6) 80–99 % in Flächen → **überarbeitet**;
  gemeißelt (b-pal15) und Dimensionsschrott (hart 31–34 %, Funken auf Schwarz) bleiben rot; Infernit/Terranit/Zenithit-
  Block und Rissscherben-Items orange wegen 36–72 Farben (feine Verläufe, A8c prüfen).
- **Simple Blocks / Baublöcke**: Froschlicht-Varianten mit weichem Verlauf + großer heller Mitte (gegen A8c);
  Schachbrett-Felder poliert flach; Rucksack-Seiten große Lederflächen; Schnitz-Masken grau.
- **Weitere Module**: simplelights (Licht-Scheiben, 16 Farben × an/aus) große Glasflächen – als Familie überarbeiten;
  simplemusic Taschen-Jukeboxen flache Kastenfläche (Item lesbar); simpleshops Vitrinen flach bzw. harte Streifen;
  simplelayers Wolken/Decken weich-flach; simpledinos Fossilien hart; simplesandwiches überwiegend grün (157).

## Überarbeitung (A8c-Runden, A8d-Methode)
Werkzeug `tools/textures/texrate15_rework_2026_11_09.py` (`--rounds DIR`, `--apply`): die vorhandene Textur bleibt
Grundlage (Form, Palette, Motiv), Flächen werden wie bei Vanilla gebrochen. Runden (jede ab R0):
- **R1** Flächen ≥ 10 px mit 2–4-px-Haufen des nächsten Tons der eigenen Rampe. Befund: Flächen weg, aber neue fast
  weiße Töne (Zinn-Fliesen) und türkise Haufen im Kristall-Nihilith (falscher Farbton).
- **R2** Grenze 7 px, Drift (oben/links heller, unten/rechts dunkler), nur Töne gleichen Farbtons, kein neuer Ton über
  dem hellsten, keine neue harte Kante. Befund: Material erkennbar, aber gleichmäßig gekörnt, unruhiger als Vanilla.
- **R3** wie R2 + Haufen entlang der Form (Säulenriefen senkrecht, Fliesen waagrecht), lange 1-px-Linien wechseln alle
  2–3 px den Ton, Stopp sobald so ruhig wie Vanilla-Baustoffe (größte Fläche ≤ 9 px, ≤ 30 % in Flächen). Befund: Form
  lesbar, Platten/Riefen/Ringe gebrochen wie cut_copper/Quarzsäule, hart unverändert. **Eingebaut.**

Eingebaut (R3, `.layers.json`: alte Ebenen + neue Ebene „A8d Flaechen-Brecher“):
- Simple Ores (24): `cut_<m>`, `<m>_tiles`, `<m>_pillar`, `<m>_pillar_top` für Messing, Blei, Nickel, Silber, Zinn, Zink.
- Simple End (10): `polished_{astralit,nihilith,ender_quartz}`, `astralit_pillar_top`, `nihilith_pillar(_top)`,
  `ender_quartz_pillar(_top)`, `nihilith_block`, `ender_quartz_block` (letzte zwei orange: Hauptton 44–45 %, ruhiger
  Grundton gewollt wie Vanilla-Stein 46 %). Abgenommene Texturen, aber eindeutig rot (Plan).

Orange wurde nicht überarbeitet: die orangen der Prio 1–3 sind ältere, abgenommene Texturen (Ziegel, Grundblöcke,
Erze) – A8d erlaubt Änderung abgenommener Texturen nur bei eindeutig Rot.

## Offen
- Pads (hart), Lampen-Mäander, Esche/Dämmerholz-Bretter, Licht-Scheiben (simplelights), Vitrinen (simpleshops),
  Froschlichter: eigene Runden je Familie (andere Methode als Flächen-Brecher: Kontrast senken, Maserung).
- `veined_*`/`crystalline_*`/`layered_astralit`/`frosted_nihilith` nach `vanilla_derived.py` umziehen.
- Generatoren `generate_textures.py` (End-Paletten) und `metals14_2026_11_09.py` würden die alten Pixel zurückschreiben:
  danach `texrate15_rework_2026_11_09.py --apply` laufen lassen (steht im Werkzeug-Kopf).
- `polished_*_checker` (Vorlage polished_*) und `mc1_21_11`-Kopien zeigen noch die alten Pixel (Ports erst auf Anforderung).

## Tabelle Prio 1–3 (rot und orange; alle Zeilen mit Thumbnail in der Vorschau)
Messwerte: größte Fläche / ≥6 px % / Hauptton % / hart % / Farben.

| Prio | Familie | Mod | Textur | Ampel | Messwerte (Fl./>=6/Haupt/hart/Farben) | Grund |
|---|---|---|---|---|---|---|
| 1 | Simple End | simplebuilding | `block/astralit_lamp` | rot | 112/66/43.8/36.7/5 | groesste Flaeche 112, in Flaechen>=6 66, hart 36.7, Hauptton 43.8; Lampen-Maeander: grosse einfarbige Felder + harte Kanten; Vanilla-Redstone-Lampe ist ebenso hart, Flaeche brechen |
| 1 | Simple End | simplebuilding | `block/astralit_lamp_on` | rot | 26/41.8/31.2/36.7/6 | hart 36.7, groesste Flaeche 26, in Flaechen>=6 41.8; Lampen-Maeander: grosse einfarbige Felder + harte Kanten; Vanilla-Redstone-Lampe ist ebenso hart, Flaeche brechen |
| 1 | Simple End | simplebuilding | `block/astralit_lamp_top` | rot | 83/56.6/32.4/32.7/7 | groesste Flaeche 83, hart 32.7, in Flaechen>=6 56.6; Lampen-Maeander: grosse einfarbige Felder + harte Kanten; Vanilla-Redstone-Lampe ist ebenso hart, Flaeche brechen |
| 1 | Simple End | simplebuilding | `block/astralit_lamp_top_on` | rot | 17/42.2/24.2/41/8 | hart 41, in Flaechen>=6 42.2; Lampen-Maeander: grosse einfarbige Felder + harte Kanten; Vanilla-Redstone-Lampe ist ebenso hart, Flaeche brechen |
| 1 | Simple End | simplebuilding | `block/chess/nihilith` | rot | 5/0/21.9/36.7/7 | hart 36.7; Schachbrett-Feld: polierte Flaeche flach |
| 1 | Simple End | simplebuilding | `block/chess/polished_ender_quartz` | rot | 22/84.4/23.4/10.8/7 | in Flaechen>=6 84.4, groesste Flaeche 22; Schachbrett-Feld: polierte Flaeche flach |
| 1 | Simple End | simplebuilding | `block/chess/polished_ender_quartz_accent` | rot | 22/84.4/23.4/0/7 | in Flaechen>=6 84.4, groesste Flaeche 22; Schachbrett-Feld: polierte Flaeche flach |
| 1 | Simple End | simplebuilding | `block/chess/polished_nihilith` | rot | 14/77.3/25/10.8/8 | in Flaechen>=6 77.3; Schachbrett-Feld: polierte Flaeche flach |
| 1 | Simple End | simplebuilding | `block/chess/polished_nihilith_accent` | rot | 14/77.3/25/0/8 | in Flaechen>=6 77.3; Schachbrett-Feld: polierte Flaeche flach |
| 1 | Simple End | simplebuilding | `block/chiseled_astralit_bricks` (gesperrt b-pal15) | rot | 46/90.2/24.2/4.6/6 | in Flaechen>=6 90.2, groesste Flaeche 46; Ziegel: Ziegelflaechen teils einfarbig |
| 1 | Simple End | simplebuilding | `block/chiseled_ender_quartz_bricks` (gesperrt b-pal15) | rot | 29/80.1/25.4/6.7/7 | in Flaechen>=6 80.1, groesste Flaeche 29; Ziegel: Ziegelflaechen teils einfarbig |
| 1 | Simple End | simplebuilding | `block/chiseled_nihilith_bricks` (gesperrt b-pal15) | rot | 33/78.5/22.7/5.2/6 | in Flaechen>=6 78.5, groesste Flaeche 33; Ziegel: Ziegelflaechen teils einfarbig |
| 1 | Simple End | simplebuilding | `block/cracked_astralit_bricks` | rot | 16/20.7/22.7/30/29 | hart 30; Ziegel: Ziegelflaechen teils einfarbig |
| 1 | Simple End | simplebuilding | `block/crystalline_astralit` (gesperrt vanilla-abgeleitet, Altbestand) | rot | 52/56.6/43.8/4.6/8 | groesste Flaeche 52, in Flaechen>=6 56.6, Hauptton 43.8 |
| 1 | Simple End | simplebuilding | `block/crystalline_nihilith` (gesperrt vanilla-abgeleitet, Altbestand) | rot | 72/67.6/55.5/3.3/9 | groesste Flaeche 72, in Flaechen>=6 67.6, Hauptton 55.5 |
| 1 | Simple End | simplebuilding | `block/elytra_pad` | rot | 8/3.1/10.5/32.3/22 | hart 32.3; Pad: Wirbel/Funken mit harten Kanten 30-48 % (A8b ~7 %), wirkt unruhig; eigene Pad-Runde (Wirbelkontrast senken) |
| 1 | Simple End | simplebuilding | `block/elytra_pad_active` | rot | 35/23.4/22.7/38.1/21 | hart 38.1, groesste Flaeche 35; Pad: Wirbel/Funken mit harten Kanten 30-48 % (A8b ~7 %), wirkt unruhig; eigene Pad-Runde (Wirbelkontrast senken) |
| 1 | Simple End | simplebuilding | `block/enderite_backpack_back` | rot | 70/69.1/30.9/10.6/18 | groesste Flaeche 70, in Flaechen>=6 69.1; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 1 | Simple End | simplebuilding | `block/enderite_backpack_front` | rot | 131/66/57.8/10.2/18 | groesste Flaeche 131, in Flaechen>=6 66, Hauptton 57.8; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 1 | Simple End | simplebuilding | `block/enderite_backpack_side` | rot | 160/76.6/69.9/6/16 | groesste Flaeche 160, in Flaechen>=6 76.6, Hauptton 69.9; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 1 | Simple End | simplebuilding | `block/enderite_backpack_top` | rot | 129/83.6/51.2/6.7/14 | groesste Flaeche 129, in Flaechen>=6 83.6, Hauptton 51.2; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 1 | Simple End | simplebuilding | `block/enderite_chunk_loader_active` | rot | 14/13.3/12.5/39.6/14 | hart 39.6; Pad-Familie: Funken hart auf Grund |
| 1 | Simple End | simplebuilding | `block/enderite_elytra_pad` | rot | 6/2.3/10.5/44.2/21 | hart 44.2; Pad: Wirbel/Funken mit harten Kanten 30-48 % (A8b ~7 %), wirkt unruhig; eigene Pad-Runde (Wirbelkontrast senken) |
| 1 | Simple End | simplebuilding | `block/enderite_elytra_pad_active` | rot | 9/3.5/11.3/47.5/16 | hart 47.5; Pad: Wirbel/Funken mit harten Kanten 30-48 % (A8b ~7 %), wirkt unruhig; eigene Pad-Runde (Wirbelkontrast senken) |
| 1 | Simple End | simplebuilding | `block/enderite_flypad` | rot | 46/52.7/18/32.1/14 | hart 32.1, groesste Flaeche 46, in Flaechen>=6 52.7; Pad: Sterne/Wirbel hart, Rahmenflaeche flach |
| 1 | Simple End | simplebuilding | `block/enderite_launchpad` | rot | 6/4.7/10.9/41.2/22 | hart 41.2; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 1 | Simple End | simplebuilding | `block/enderite_launchpad_charge_1` | rot | 10/8.6/17.2/47.7/19 | hart 47.7; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 1 | Simple End | simplebuilding | `block/enderite_launchpad_charge_2` | rot | 16/14.8/17.2/44/19 | hart 44; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 1 | Simple End | simplebuilding | `block/enderite_launchpad_charge_3` | rot | 10/8.6/17.2/46/16 | hart 46; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 1 | Simple End | simplebuilding | `block/enderite_spawn_teleporter` | rot | 6/2.3/10.5/41.7/21 | hart 41.7; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 1 | Simple End | simplebuilding | `block/enderite_spawn_teleporter_active` | rot | 22/14.1/14.5/39.6/15 | hart 39.6, groesste Flaeche 22; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 1 | Simple End | simplebuilding | `block/fine_elytra_pad` | rot | 9/6.6/10.2/40.8/23 | hart 40.8; Pad: Wirbel/Funken mit harten Kanten 30-48 % (A8b ~7 %), wirkt unruhig; eigene Pad-Runde (Wirbelkontrast senken) |
| 1 | Simple End | simplebuilding | `block/fine_elytra_pad_active` | rot | 11/7.4/11.3/45.4/17 | hart 45.4; Pad: Wirbel/Funken mit harten Kanten 30-48 % (A8b ~7 %), wirkt unruhig; eigene Pad-Runde (Wirbelkontrast senken) |
| 1 | Simple End | simplebuilding | `block/frosted_nihilith` (gesperrt vanilla-abgeleitet, Altbestand) | rot | 94/74.2/53.9/2.3/9 | groesste Flaeche 94, in Flaechen>=6 74.2, Hauptton 53.9 |
| 1 | Simple End | simplebuilding | `block/netherite_elytra_pad` | rot | 6/2.3/10.5/38.3/22 | hart 38.3; Pad: Wirbel/Funken mit harten Kanten 30-48 % (A8b ~7 %), wirkt unruhig; eigene Pad-Runde (Wirbelkontrast senken) |
| 1 | Simple End | simplebuilding | `block/netherite_elytra_pad_active` | rot | 35/23.4/22.7/42.9/18 | hart 42.9, groesste Flaeche 35; Pad: Wirbel/Funken mit harten Kanten 30-48 % (A8b ~7 %), wirkt unruhig; eigene Pad-Runde (Wirbelkontrast senken) |
| 1 | Simple End | simplebuilding | `block/nihilith_lamp` | rot | 112/86.3/43.8/42.5/4 | groesste Flaeche 112, in Flaechen>=6 86.3, hart 42.5, Hauptton 43.8; Lampen-Maeander: grosse einfarbige Felder + harte Kanten; Vanilla-Redstone-Lampe ist ebenso hart, Flaeche brechen |
| 1 | Simple End | simplebuilding | `block/nihilith_lamp_on` | rot | 112/100/43.8/36.7/3 | groesste Flaeche 112, in Flaechen>=6 100, hart 36.7, Farben 3, Hauptton 43.8; Lampen-Maeander: grosse einfarbige Felder + harte Kanten; Vanilla-Redstone-Lampe ist ebenso hart, Flaeche brechen |
| 1 | Simple End | simplebuilding | `block/nihilith_lamp_top` | rot | 108/91/42.2/46.5/5 | groesste Flaeche 108, in Flaechen>=6 91, hart 46.5, Hauptton 42.2; Lampen-Maeander: grosse einfarbige Felder + harte Kanten; Vanilla-Redstone-Lampe ist ebenso hart, Flaeche brechen |
| 1 | Simple End | simplebuilding | `block/nihilith_lamp_top_on` | rot | 108/98.4/42.2/46.7/4 | groesste Flaeche 108, in Flaechen>=6 98.4, hart 46.7, Hauptton 42.2; Lampen-Maeander: grosse einfarbige Felder + harte Kanten; Vanilla-Redstone-Lampe ist ebenso hart, Flaeche brechen |
| 1 | Simple End | simplebuilding | `block/reinforced_elytra_pad` | rot | 6/7/10.5/37.7/21 | hart 37.7; Pad: Wirbel/Funken mit harten Kanten 30-48 % (A8b ~7 %), wirkt unruhig; eigene Pad-Runde (Wirbelkontrast senken) |
| 1 | Simple End | simplebuilding | `block/reinforced_elytra_pad_active` | rot | 35/23.4/22.7/44.6/18 | hart 44.6, groesste Flaeche 35; Pad: Wirbel/Funken mit harten Kanten 30-48 % (A8b ~7 %), wirkt unruhig; eigene Pad-Runde (Wirbelkontrast senken) |
| 1 | Simple End | simplebuilding | `block/stellar_flypad_ender` | rot | 8/13.7/15.2/30.6/22 | hart 30.6; Pad: Sterne/Wirbel hart, Rahmenflaeche flach |
| 1 | Simple End | simplebuilding | `block/stellar_flypad_ender_active` | rot | 10/14.1/18/42.3/19 | hart 42.3; Pad: Sterne/Wirbel hart, Rahmenflaeche flach |
| 1 | Simple End | simplebuilding | `block/veined_astralit` (gesperrt vanilla-abgeleitet, Altbestand) | rot | 33/73.4/43/4.4/8 | in Flaechen>=6 73.4, groesste Flaeche 33, Hauptton 43 |
| 1 | Simple End | simplebuilding | `block/veined_nihilith` (gesperrt vanilla-abgeleitet, Altbestand) | rot | 72/78.5/55.1/2.5/9 | groesste Flaeche 72, in Flaechen>=6 78.5, Hauptton 55.1 |
| 1 | Simple End | simpleend | `block/ashwood_door_bottom` | rot | 60/84/39.8/34/7 | groesste Flaeche 60, in Flaechen>=6 84, hart 34; Tuer: Brettflaechen ohne Maserung |
| 1 | Simple End | simpleend | `block/ashwood_door_top` | rot | 46/85.8/44/21.1/6 | in Flaechen>=6 85.8, groesste Flaeche 46, Hauptton 44; Tuer: Brettflaechen ohne Maserung |
| 1 | Simple End | simpleend | `block/ashwood_leaves_crystal` | rot | 23/67.6/67.6/46.2/5 | in Flaechen>=6 67.6, Hauptton 67.6, hart 46.2, groesste Flaeche 23 |
| 1 | Simple End | simpleend | `block/ashwood_sticks` | rot | 16/82/32.8/20/6 | in Flaechen>=6 82, hart 20 |
| 1 | Simple End | simpleend | `block/ashwood_sticks_top` | rot | 156/60.9/60.9/51.2/4 | groesste Flaeche 156, Hauptton 60.9, hart 51.2, in Flaechen>=6 60.9; Stoecke-Buendel oben: grosse einfarbige Flaeche zwischen den Stoecken |
| 1 | Simple End | simpleend | `block/ashwood_trapdoor` | rot | 60/76.8/38.8/21.9/6 | groesste Flaeche 60, in Flaechen>=6 76.8; Falltuer: Brettflaechen ohne Maserung |
| 1 | Simple End | simpleend | `block/burrowed_end_stone_top` | rot | 142/65.2/60.9/19.2/5 | groesste Flaeche 142, in Flaechen>=6 65.2, Hauptton 60.9, hart 19.2 |
| 1 | Simple End | simpleend | `block/duskwood_door_bottom` | rot | 60/83.6/34.8/24/7 | groesste Flaeche 60, in Flaechen>=6 83.6, hart 24; Tuer: Brettflaechen ohne Maserung |
| 1 | Simple End | simpleend | `block/duskwood_leaves_crystal` | rot | 23/67.6/67.6/50.4/5 | in Flaechen>=6 67.6, Hauptton 67.6, hart 50.4, groesste Flaeche 23 |
| 1 | Simple End | simpleend | `block/duskwood_sticks_top` | rot | 156/60.9/60.9/41.2/4 | groesste Flaeche 156, Hauptton 60.9, hart 41.2, in Flaechen>=6 60.9; Stoecke-Buendel oben: grosse einfarbige Flaeche zwischen den Stoecken |
| 1 | Simple End | simpleend | `block/duskwood_trapdoor` | rot | 60/88.2/35.5/21.2/5 | groesste Flaeche 60, in Flaechen>=6 88.2; Falltuer: Brettflaechen ohne Maserung |
| 1 | Simple End | simpleend | `block/stripped_ashwood_log` | rot | 31/73.4/38.3/30/9 | in Flaechen>=6 73.4, hart 30, groesste Flaeche 31; entrindeter Stamm: lange einfarbige Streifen |
| 1 | Simple End | simpleend | `block/stripped_ashwood_log_top` | rot | 67/42.6/32/11.2/11 | groesste Flaeche 67, in Flaechen>=6 42.6; entrindeter Stamm: lange einfarbige Streifen |
| 1 | Simple End | simpleend | `block/stripped_duskwood_log` | rot | 22/73/27.3/17.9/6 | in Flaechen>=6 73, groesste Flaeche 22, hart 17.9; entrindeter Stamm: lange einfarbige Streifen |
| 1 | Simple End | simpleend | `block/stripped_duskwood_log_top` | rot | 83/42.6/39.5/4.6/10 | groesste Flaeche 83, in Flaechen>=6 42.6; entrindeter Stamm: lange einfarbige Streifen |
| 1 | Simple End | simpleend | `item/ashwood_hanging_sign` | rot | 36/90.8/30.8/23.6/7 | in Flaechen>=6 90.8, groesste Flaeche 36; Schild-Item: Holzflaeche flach (Vanilla oak_hanging_sign ebenfalls orange) |
| 1 | Simple End | simpleend | `item/ashwood_sign` | rot | 40/86.9/32.8/29.5/7 | in Flaechen>=6 86.9, groesste Flaeche 40; Schild-Item: Holzflaeche flach (Vanilla oak_sign ebenfalls orange) |
| 1 | Simple End | simpleend | `item/end_painting` | rot | 52/98/22.2/17.4/10 | groesste Flaeche 52, in Flaechen>=6 98, hart 17.4 |
| 1 | Simple End | simplebuilding | `block/astral_redstone_block` | orange | 41/62.1/23.8/25/8 | groesste Flaeche 41, in Flaechen>=6 62.1, hart 25; Materialblock: grosse Grundflaeche |
| 1 | Simple End | simplebuilding | `block/astralit_block` | orange | 37/57.4/43.4/4.6/8 | groesste Flaeche 37, in Flaechen>=6 57.4, Hauptton 43.4; Materialblock: grosse Grundflaeche |
| 1 | Simple End | simplebuilding | `block/astralit_bricks` | orange | 37/51.6/29.7/11.9/9 | groesste Flaeche 37, in Flaechen>=6 51.6; Ziegel: Ziegelflaechen teils einfarbig |
| 1 | Simple End | simplebuilding | `block/astralit_pillar` | orange | 45/63.7/28.9/13.8/10 | groesste Flaeche 45, in Flaechen>=6 63.7; Saeule: Riefen ueber die volle Hoehe einfarbig (Vanilla-Quarzsaeule bricht sie) |
| 1 | Simple End | simplebuilding | `block/chess/astralit` | orange | 7/20.3/32.8/21.7/7 | hart 21.7; Schachbrett-Feld: polierte Flaeche flach |
| 1 | Simple End | simplebuilding | `block/chess/polished_astralit` | orange | 20/64.1/25/10.8/7 | groesste Flaeche 20, in Flaechen>=6 64.1; Schachbrett-Feld: polierte Flaeche flach |
| 1 | Simple End | simplebuilding | `block/chess/polished_astralit_accent` | orange | 20/64.1/25/0/7 | groesste Flaeche 20, in Flaechen>=6 64.1; Schachbrett-Feld: polierte Flaeche flach |
| 1 | Simple End | simplebuilding | `block/chess/purpur` | orange | 20/60.2/20.3/6.7/7 | groesste Flaeche 20, in Flaechen>=6 60.2; Schachbrett-Feld: polierte Flaeche flach |
| 1 | Simple End | simplebuilding | `block/chess/purpur_accent` | orange | 20/60.2/20.3/0/7 | groesste Flaeche 20, in Flaechen>=6 60.2; Schachbrett-Feld: polierte Flaeche flach |
| 1 | Simple End | simplebuilding | `block/cracked_ender_quartz_bricks` | orange | 16/30.9/22.7/18.5/31 | hart 18.5; Ziegel: Ziegelflaechen teils einfarbig |
| 1 | Simple End | simplebuilding | `block/cracked_nihilith_bricks` | orange | 11/18.8/19.5/26.9/34 | hart 26.9, Farben 34 (Verlauf?); Ziegel: Ziegelflaechen teils einfarbig |
| 1 | Simple End | simplebuilding | `block/ender_quartz_block` | orange | 9/27.3/44.5/6.9/7 | Hauptton 44.5; ueberarbeitet A8d R3 (b-texrate15), vorher rot: groesste Flaeche 57, Hauptton 56.6, in Flaechen>=6 60.5 |
| 1 | Simple End | simplebuilding | `block/ender_quartz_bricks` | orange | 21/51.2/30.5/12.5/10 | groesste Flaeche 21, in Flaechen>=6 51.2; Ziegel: Ziegelflaechen teils einfarbig |
| 1 | Simple End | simplebuilding | `block/enderite_blast_furnace_front` | orange | 20/21.9/12.5/12.9/27 | groesste Flaeche 20 |
| 1 | Simple End | simplebuilding | `block/enderite_blast_furnace_front_on` | orange | 20/21.9/12.5/9.2/30 | groesste Flaeche 20 |
| 1 | Simple End | simplebuilding | `block/enderite_block` | orange | 27/37.5/25.8/6.2/10 | groesste Flaeche 27; Materialblock: grosse Grundflaeche |
| 1 | Simple End | simplebuilding | `block/enderite_chunk_loader` | orange | 5/0/13.3/28.3/21 | hart 28.3; Pad-Familie: Funken hart auf Grund |
| 1 | Simple End | simplebuilding | `block/enderite_furnace_front` | orange | 20/33.2/14.8/16.2/19 | groesste Flaeche 20, hart 16.2 |
| 1 | Simple End | simplebuilding | `block/enderite_furnace_front_on` | orange | 18/18.4/10.5/29/24 | hart 29 |
| 1 | Simple End | simplebuilding | `block/enderite_pressure_plate` | orange | 27/37.5/25.8/6.2/10 | groesste Flaeche 27 |
| 1 | Simple End | simplebuilding | `block/enderite_shulker_box` | orange | 43/32.4/26.6/4.2/11 | groesste Flaeche 43 |
| 1 | Simple End | simplebuilding | `block/flypad_ender` | orange | 8/7.8/11.7/16.2/22 | hart 16.2; Pad: Sterne/Wirbel hart, Rahmenflaeche flach |
| 1 | Simple End | simplebuilding | `block/flypad_ender_active` | orange | 13/15.2/12.5/24.8/16 | hart 24.8; Pad: Sterne/Wirbel hart, Rahmenflaeche flach |
| 1 | Simple End | simplebuilding | `block/layered_astralit` (gesperrt vanilla-abgeleitet, Altbestand) | orange | 43/56.6/43/5.2/8 | groesste Flaeche 43, in Flaechen>=6 56.6, Hauptton 43 |
| 1 | Simple End | simplebuilding | `block/netherite_shulker_box` | orange | 43/32.4/26.6/10.8/11 | groesste Flaeche 43 |
| 1 | Simple End | simplebuilding | `block/nihil_obsidian` | orange | 14/14.1/15.2/24/19 | hart 24 |
| 1 | Simple End | simplebuilding | `block/nihil_redstone_block` | orange | 37/61.3/26.2/18.3/8 | groesste Flaeche 37, in Flaechen>=6 61.3, hart 18.3; Materialblock: grosse Grundflaeche |
| 1 | Simple End | simplebuilding | `block/nihilith_block` | orange | 9/26.6/44.9/2.5/9 | Hauptton 44.9; ueberarbeitet A8d R3 (b-texrate15), vorher rot: groesste Flaeche 63, in Flaechen>=6 69.5, Hauptton 54.7 |
| 1 | Simple End | simplebuilding | `block/nihilith_bricks` | orange | 24/48.4/30.1/10.4/11 | groesste Flaeche 24, in Flaechen>=6 48.4; Ziegel: Ziegelflaechen teils einfarbig |
| 1 | Simple End | simplebuilding | `block/nihilith_quartz_checker` | orange | 11/22.7/14.1/22.1/13 | hart 22.1 |
| 1 | Simple End | simplebuilding | `block/nihilith_quartz_checker_mirror` | orange | 11/22.7/14.1/22.1/13 | hart 22.1 |
| 1 | Simple End | simplebuilding | `block/polished_astralit_checker` | orange | 16/55.1/14.1/6.2/13 | in Flaechen>=6 55.1; poliert: Plattenflaechen einfarbig (Vanilla polished_* 0-50 %) |
| 1 | Simple End | simplebuilding | `block/polished_astralit_checker_mirror` | orange | 16/55.1/14.1/6.2/13 | in Flaechen>=6 55.1; poliert: Plattenflaechen einfarbig (Vanilla polished_* 0-50 %) |
| 1 | Simple End | simplebuilding | `block/polished_ender_quartz_checker` | orange | 13/63.3/14.1/8.8/13 | in Flaechen>=6 63.3; poliert: Plattenflaechen einfarbig (Vanilla polished_* 0-50 %) |
| 1 | Simple End | simplebuilding | `block/polished_ender_quartz_checker_mirror` | orange | 13/63.3/14.1/8.8/13 | in Flaechen>=6 63.3; poliert: Plattenflaechen einfarbig (Vanilla polished_* 0-50 %) |
| 1 | Simple End | simplebuilding | `block/polished_nihilith_checker` | orange | 18/56.2/14.1/5.8/14 | in Flaechen>=6 56.2; poliert: Plattenflaechen einfarbig (Vanilla polished_* 0-50 %) |
| 1 | Simple End | simplebuilding | `block/polished_nihilith_checker_mirror` | orange | 18/56.2/14.1/5.8/14 | in Flaechen>=6 56.2; poliert: Plattenflaechen einfarbig (Vanilla polished_* 0-50 %) |
| 1 | Simple End | simplebuilding | `block/purpur_quartz_checker` | orange | 15/51.2/14.1/7.1/13 | in Flaechen>=6 51.2 |
| 1 | Simple End | simplebuilding | `block/purpur_quartz_checker_mirror` | orange | 15/51.2/14.1/7.1/13 | in Flaechen>=6 51.2 |
| 1 | Simple End | simplebuilding | `block/reinforced_flypad_ender_active` | orange | 13/20.7/14.1/25.8/14 | hart 25.8; Pad: Sterne/Wirbel hart, Rahmenflaeche flach |
| 1 | Simple End | simplebuilding | `block/reinforced_shulker_box` | orange | 43/32.4/26.6/10.4/11 | groesste Flaeche 43 |
| 1 | Simple End | simplebuilding | `item/enderite_backpack` | orange | 28/67.3/28.6/16.4/14 | in Flaechen>=6 67.3; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 1 | Simple End | simplebuilding | `item/enderite_bundle` | orange | 5/0/11.5/19.3/55 | Farben 55 (Verlauf?) |
| 1 | Simple End | simplebuilding | `item/enderite_nugget` | orange | 4/0/41.7/50/9 | Hauptton 41.7 |
| 1 | Simple End | simplebuilding | `item/enderite_paint_box` | orange | 14/62.3/24.5/29.8/37 | in Flaechen>=6 62.3, Farben 37 (Verlauf?) |
| 1 | Simple End | simplebuilding | `item/enderite_quiver` | orange | 3/0/15/30.5/36 | Farben 36 (Verlauf?) |
| 1 | Simple End | simplebuilding | `item/layered_raw_enderite` | orange | 17/50/40.7/30.4/6 | in Flaechen>=6 50, Hauptton 40.7 |
| 1 | Simple End | simplebuilding | `item/raw_enderite` | orange | 27/47.4/35.3/22.6/6 | in Flaechen>=6 47.4 |
| 1 | Simple End | simpleend | `block/ashwood_bookshelf` | orange | 8/5.9/9/22.1/59 | hart 22.1, Farben 59 (Verlauf?) |
| 1 | Simple End | simpleend | `block/ashwood_leaves` | orange | 2/0/7.4/45.1/74 | Farben 74 (Verlauf?) |
| 1 | Simple End | simpleend | `block/ashwood_leaves_inner` | orange | 2/0/3.3/49.7/94 | Farben 94 (Verlauf?) |
| 1 | Simple End | simpleend | `block/ashwood_leaves_loose` | orange | 2/0/6.8/34.3/53 | Farben 53 (Verlauf?) |
| 1 | Simple End | simpleend | `block/ashwood_log_top` | orange | 10/14.5/17.6/20.6/15 | hart 20.6 |
| 1 | Simple End | simpleend | `block/ashwood_shelf` | orange | 9/12.6/11.8/23.5/30 | hart 23.5 |
| 1 | Simple End | simpleend | `block/duskwood_bookshelf` | orange | 8/3.1/9/20.8/52 | hart 20.8, Farben 52 (Verlauf?) |
| 1 | Simple End | simpleend | `block/duskwood_door_top` | orange | 46/57.4/30.1/19.2/6 | groesste Flaeche 46, in Flaechen>=6 57.4; Tuer: Brettflaechen ohne Maserung |
| 1 | Simple End | simpleend | `block/duskwood_leaves` | orange | 2/0/3.8/46.2/86 | Farben 86 (Verlauf?) |
| 1 | Simple End | simpleend | `block/duskwood_leaves_inner` | orange | 1/0/4/42.5/100 | Farben 100 (Verlauf?) |
| 1 | Simple End | simpleend | `block/duskwood_leaves_loose` | orange | 1/0/4.7/40.2/71 | Farben 71 (Verlauf?) |
| 1 | Simple End | simpleend | `block/duskwood_log` | orange | 4/0/7/2.9/86 | Farben 86 (Verlauf?) |
| 1 | Simple End | simpleend | `block/duskwood_log_top` | orange | 3/0/3.5/13.5/112 | Farben 112 (Verlauf?) |
| 1 | Simple End | simpleend | `block/duskwood_sign` | orange | 18.8/64.7/29.3/17.1/4 | in Flaechen>=6 64.7; Schild-Item: Holzflaeche flach (Vanilla oak_sign ebenfalls orange) |
| 1 | Simple End | simpleend | `block/duskwood_sticks` | orange | 16/53.1/28.1/21/6 | in Flaechen>=6 53.1, hart 21 |
| 1 | Simple End | simpleend | `block/end_grass` | orange | 1/0/6.1/16.9/45 | Farben 45 (Verlauf?) |
| 1 | Simple End | simpleend | `item/duskwood_hanging_sign` | orange | 26/61.5/29.1/4/10 | in Flaechen>=6 61.5; Schild-Item: Holzflaeche flach (Vanilla oak_hanging_sign ebenfalls orange) |
| 1 | Simple End | simpleend | `item/duskwood_sign` | orange | 25/65.9/34.1/0/10 | in Flaechen>=6 65.9; Schild-Item: Holzflaeche flach (Vanilla oak_sign ebenfalls orange) |
| 1 | Simple End | simpleend | `item/guide_book` | orange | 43/32.9/30.9/55.9/9 | groesste Flaeche 43 |
| 2 | Simple Ores | simpleores | `block/chiseled_brass` (gesperrt b-pal15) | rot | 63/85.5/39.8/1/4 | groesste Flaeche 63, in Flaechen>=6 85.5 |
| 2 | Simple Ores | simpleores | `block/chiseled_lead` (gesperrt b-pal15) | rot | 63/85.5/39.8/1/4 | groesste Flaeche 63, in Flaechen>=6 85.5 |
| 2 | Simple Ores | simpleores | `block/chiseled_nickel` (gesperrt b-pal15) | rot | 63/85.5/39.8/1/4 | groesste Flaeche 63, in Flaechen>=6 85.5 |
| 2 | Simple Ores | simpleores | `block/chiseled_silver` (gesperrt b-pal15) | rot | 63/85.5/39.8/0/4 | groesste Flaeche 63, in Flaechen>=6 85.5 |
| 2 | Simple Ores | simpleores | `block/chiseled_tin` (gesperrt b-pal15) | rot | 63/85.5/39.8/1/4 | groesste Flaeche 63, in Flaechen>=6 85.5 |
| 2 | Simple Ores | simpleores | `block/chiseled_zinc` (gesperrt b-pal15) | rot | 63/85.5/39.8/1/4 | groesste Flaeche 63, in Flaechen>=6 85.5 |
| 2 | Simple Ores | simpleores | `block/dimensional_scrap_side` | rot | 20/50.4/27.7/31.9/20 | hart 31.9, groesste Flaeche 20, in Flaechen>=6 50.4 |
| 2 | Simple Ores | simpleores | `block/dimensional_scrap_top` | rot | 9/10.5/29.7/34.2/19 | hart 34.2 |
| 2 | Simple Ores | simpleores | `block/end_dimensional_scrap_side` | rot | 20/50.4/27.7/31.5/21 | hart 31.5, groesste Flaeche 20, in Flaechen>=6 50.4 |
| 2 | Simple Ores | simpleores | `block/end_dimensional_scrap_top` | rot | 9/10.5/29.7/32.1/20 | hart 32.1 |
| 2 | Simple Ores | simpleores | `block/deepslate_sage_ore` | orange | 6/4.7/26.6/20.8/27 | hart 20.8 |
| 2 | Simple Ores | simpleores | `block/enderite_block` | orange | 27/37.5/25.8/6.2/10 | groesste Flaeche 27; Materialblock: grosse Grundflaeche |
| 2 | Simple Ores | simpleores | `block/infernite_block` | orange | 29/44.9/12.9/6.7/71 | groesste Flaeche 29, in Flaechen>=6 44.9, Farben 71 (Verlauf?); Materialblock: grosse Grundflaeche |
| 2 | Simple Ores | simpleores | `block/nether_dimensional_scrap_side` | orange | 20/50.4/27.7/26.9/14 | groesste Flaeche 20, in Flaechen>=6 50.4, hart 26.9 |
| 2 | Simple Ores | simpleores | `block/nether_dimensional_scrap_top` | orange | 9/10.5/29.7/29.4/21 | hart 29.4 |
| 2 | Simple Ores | simpleores | `block/sage_ore` | orange | 16/31.6/37.1/24.2/11 | hart 24.2 |
| 2 | Simple Ores | simpleores | `block/terranite_block` | orange | 27/40.6/12.5/6/72 | groesste Flaeche 27, in Flaechen>=6 40.6, Farben 72 (Verlauf?); Materialblock: grosse Grundflaeche |
| 2 | Simple Ores | simpleores | `block/zenithite_block` | orange | 27/44.9/12.1/6.5/69 | groesste Flaeche 27, in Flaechen>=6 44.9, Farben 69 (Verlauf?); Materialblock: grosse Grundflaeche |
| 2 | Simple Ores | simpleores | `item/end_rift_shard` | orange | 6/5.3/9.7/24.3/53 | Farben 53 (Verlauf?) |
| 2 | Simple Ores | simpleores | `item/enderite_nugget` | orange | 4/0/41.7/50/9 | Hauptton 41.7 |
| 2 | Simple Ores | simpleores | `item/guide_book` | orange | 43/32.9/30.9/59.6/9 | groesste Flaeche 43 |
| 2 | Simple Ores | simpleores | `item/infernite_ingot` | orange | 15/27.4/22.1/25.6/36 | Farben 36 (Verlauf?) |
| 2 | Simple Ores | simpleores | `item/layered_raw_enderite` | orange | 17/50/40.7/30.4/6 | in Flaechen>=6 50, Hauptton 40.7 |
| 2 | Simple Ores | simpleores | `item/nether_rift_shard` | orange | 6/10.6/12.4/26.5/38 | Farben 38 (Verlauf?) |
| 2 | Simple Ores | simpleores | `item/raw_enderite` | orange | 27/47.4/35.3/22.6/6 | in Flaechen>=6 47.4 |
| 2 | Simple Ores | simpleores | `item/rift_shard` | orange | 6/5.3/12.4/27/51 | Farben 51 (Verlauf?) |
| 2 | Simple Ores | simpleores | `item/sage_orb` | orange | 20/50/31.8/23.1/6 | in Flaechen>=6 50 |
| 2 | Simple Ores | simpleores | `item/terranite_ingot` | orange | 11/22.1/17.9/26.8/49 | Farben 49 (Verlauf?) |
| 2 | Simple Ores | simpleores | `item/zenithite_ingot` | orange | 11/18.9/17.9/26.8/60 | Farben 60 (Verlauf?) |
| 3 | Simple Blocks / Baubloecke | simpleblocks | `block/aqua_froglight_top` | rot | 113/71.1/44.5/0/6 | groesste Flaeche 113, in Flaechen>=6 71.1, Hauptton 44.5; weiche Verlaeufe + grosse helle Mitte (gegen A8c: kein weicher Verlauf) |
| 3 | Simple Blocks / Baubloecke | simpleblocks | `block/azure_froglight_side` | rot | 68/47.3/26.6/1.5/8 | groesste Flaeche 68, in Flaechen>=6 47.3; weiche Verlaeufe + grosse helle Mitte (gegen A8c: kein weicher Verlauf) |
| 3 | Simple Blocks / Baubloecke | simpleblocks | `block/azure_froglight_top` | rot | 97/56.2/39.1/2.9/6 | groesste Flaeche 97, in Flaechen>=6 56.2; weiche Verlaeufe + grosse helle Mitte (gegen A8c: kein weicher Verlauf) |
| 3 | Simple Blocks / Baubloecke | simpleblocks | `block/nautilus_shell_block_top` | rot | 52/75.8/27.3/23.8/7 | groesste Flaeche 52, in Flaechen>=6 75.8, hart 23.8; Materialblock: grosse Grundflaeche |
| 3 | Simple Blocks / Baubloecke | simpleblocks | `block/scarlet_froglight_side` | rot | 63/45.7/24.6/7.9/8 | groesste Flaeche 63, in Flaechen>=6 45.7; weiche Verlaeufe + grosse helle Mitte (gegen A8c: kein weicher Verlauf) |
| 3 | Simple Blocks / Baubloecke | simpleblocks | `block/scarlet_froglight_top` | rot | 104/60.2/40.6/7.5/6 | groesste Flaeche 104, in Flaechen>=6 60.2, Hauptton 40.6; weiche Verlaeufe + grosse helle Mitte (gegen A8c: kein weicher Verlauf) |
| 3 | Simple Blocks / Baubloecke | simpleblocks | `block/sculk_tiles` | rot | 86/62.9/33.6/7.7/15 | groesste Flaeche 86, in Flaechen>=6 62.9; Fliesen: Fliesenflaechen einfarbig, Fugen durchgehend |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/auto_smither_top` | rot | 52/46.1/22.3/17.1/15 | groesste Flaeche 52, in Flaechen>=6 46.1, hart 17.1 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/auto_smither_top_triggered` | rot | 52/46.1/22.3/16.2/15 | groesste Flaeche 52, in Flaechen>=6 46.1, hart 16.2 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/autonomous_crafter_bottom` | rot | 60/62.1/35.2/10.4/9 | groesste Flaeche 60, in Flaechen>=6 62.1 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/backpack_back` | rot | 70/75.8/30.9/9.8/13 | groesste Flaeche 70, in Flaechen>=6 75.8; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/backpack_front` | rot | 131/69.5/58.6/6.9/12 | groesste Flaeche 131, in Flaechen>=6 69.5, Hauptton 58.6; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/backpack_side` | rot | 160/80.9/70.7/4.4/11 | groesste Flaeche 160, in Flaechen>=6 80.9, Hauptton 70.7; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/backpack_top` | rot | 130/89.1/51.6/5/9 | groesste Flaeche 130, in Flaechen>=6 89.1, Hauptton 51.6; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/chunk_loader` | rot | 6/4.7/10.9/30.6/22 | hart 30.6; Pad-Familie: Funken hart auf Grund |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/chunk_loader_active` | rot | 9/8.6/12.1/36/17 | hart 36; Pad-Familie: Funken hart auf Grund |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/cracked_diamond_block` | rot | 7/2.7/21.1/39/10 | hart 39; Materialblock: grosse Grundflaeche |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/crimson_blazewood_planks` | rot | 24/57.4/45.3/3.8/54 | groesste Flaeche 24, in Flaechen>=6 57.4, Hauptton 45.3, Farben 54 (Verlauf?) |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/hammock_rope` | rot | 31/49.6/50/50/3 | hart 50, Farben 3, groesste Flaeche 31, in Flaechen>=6 49.6, Hauptton 50 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/infused_potion_pad` | rot | 16/10.5/25.4/36/14 | hart 36; Pad-Familie: Wirbel hart |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/infused_potion_pad_active` | rot | 16/10.5/25.4/46.5/14 | hart 46.5; Pad-Familie: Wirbel hart |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/infused_potion_pad_cooling` | rot | 16/10.5/25.4/38.1/14 | hart 38.1; Pad-Familie: Wirbel hart |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_backpack_back` | rot | 70/75.8/30.9/11.2/14 | groesste Flaeche 70, in Flaechen>=6 75.8; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_backpack_front` | rot | 131/69.5/58.6/6.9/13 | groesste Flaeche 131, in Flaechen>=6 69.5, Hauptton 58.6; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_backpack_side` | rot | 160/80.5/70.7/4.2/12 | groesste Flaeche 160, in Flaechen>=6 80.5, Hauptton 70.7; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_backpack_top` | rot | 130/88.7/51.6/1.7/10 | groesste Flaeche 130, in Flaechen>=6 88.7, Hauptton 51.6; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_chunk_loader` | rot | 9/7/15.2/41.9/21 | hart 41.9; Pad-Familie: Funken hart auf Grund |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_chunk_loader_active` | rot | 13/7.8/10.9/41.7/17 | hart 41.7; Pad-Familie: Funken hart auf Grund |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_launchpad` | rot | 12/9.4/14.8/33.1/23 | hart 33.1; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_launchpad_charge_1` | rot | 12/9.4/14.8/33.1/23 | hart 33.1; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_launchpad_charge_2` | rot | 12/9.4/14.1/33.1/22 | hart 33.1; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/potion_pad_active` | rot | 20/32.4/16/35.6/13 | hart 35.6, groesste Flaeche 20; Pad-Familie: Wirbel hart |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/reinforced_backpack_back` | rot | 70/75.8/30.9/13.8/13 | groesste Flaeche 70, in Flaechen>=6 75.8; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/reinforced_backpack_front` | rot | 131/69.5/58.6/9/12 | groesste Flaeche 131, in Flaechen>=6 69.5, Hauptton 58.6; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/reinforced_backpack_side` | rot | 160/80.5/70.7/6/12 | groesste Flaeche 160, in Flaechen>=6 80.5, Hauptton 70.7; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/reinforced_backpack_top` | rot | 130/88.7/51.6/5.2/10 | groesste Flaeche 130, in Flaechen>=6 88.7, Hauptton 51.6; Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/reinforced_furnace_front_on` | rot | 18/18.4/10.5/35.2/23 | hart 35.2 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/reinforced_potion_pad_active` | rot | 27/28.1/19.5/40.4/11 | hart 40.4, groesste Flaeche 27; Pad-Familie: Wirbel hart |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/spawn_teleporter_tier_2` | rot | 6/4.7/10.5/33.8/22 | hart 33.8; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/spawn_teleporter_tier_3` | rot | 6/2.3/10.5/43.3/20 | hart 43.3; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/spawn_teleporter_tier_4` | rot | 6/4.7/9/45.4/21 | hart 45.4; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/warped_blazewood_planks` | rot | 24/57.4/45.3/10.4/54 | groesste Flaeche 24, in Flaechen>=6 57.4, Hauptton 45.3, Farben 54 (Verlauf?) |
| 3 | Simple Blocks / Baubloecke | simpleblocks | `block/aqua_froglight_side` | orange | 48/48/20.7/0.6/8 | groesste Flaeche 48, in Flaechen>=6 48; weiche Verlaeufe + grosse helle Mitte (gegen A8c: kein weicher Verlauf) |
| 3 | Simple Blocks / Baubloecke | simpleblocks | `block/blue_nether_bricks` | orange | 4/0/18.8/25.4/16 | hart 25.4; Ziegel: Ziegelflaechen teils einfarbig |
| 3 | Simple Blocks / Baubloecke | simpleblocks | `block/nautilus_shell_block_side` | orange | 39/33.6/37.5/28.3/9 | groesste Flaeche 39, hart 28.3; Materialblock: grosse Grundflaeche |
| 3 | Simple Blocks / Baubloecke | simpleblocks | `item/guide_book` | orange | 43/32.9/30.9/61.9/9 | groesste Flaeche 43 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/blazing_obsidian` | orange | 9/5.9/15.2/24/96 | hart 24, Farben 96 (Verlauf?) |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/bridge_redstone_block` | orange | 23/53.5/23.8/29.4/17 | groesste Flaeche 23, in Flaechen>=6 53.5, hart 29.4; Materialblock: grosse Grundflaeche |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/chess/quartz` | orange | 12/45.3/28.1/0/6 | in Flaechen>=6 45.3; Schachbrett-Feld: polierte Flaeche flach |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/chess/quartz_accent` | orange | 12/45.3/28.1/0/6 | in Flaechen>=6 45.3; Schachbrett-Feld: polierte Flaeche flach |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/construction_light` | orange | 46/37.5/35.2/16/9 | groesste Flaeche 46, hart 16 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/crimson_blazewood_bookshelf` | orange | 14/16.4/10.9/23.3/63 | hart 23.3, Farben 63 (Verlauf?) |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/flypad` | orange | 9/8.2/7.4/0.6/98 | Farben 98 (Verlauf?); Pad: Sterne/Wirbel hart, Rahmenflaeche flach |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/launchpad` | orange | 8/10.2/15.6/21.5/19 | hart 21.5; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/launchpad_charge_1` | orange | 12/12.5/14.1/22.3/19 | hart 22.3; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/launchpad_charge_2` | orange | 8/7.8/13.3/16.5/19 | hart 16.5; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_blast_furnace_front` | orange | 20/21.9/16.8/1.7/25 | groesste Flaeche 20 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_blast_furnace_front_on` | orange | 20/21.9/15.6/7.9/28 | groesste Flaeche 20 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_furnace_front` | orange | 20/33.2/14.8/6.9/18 | groesste Flaeche 20 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_furnace_front_on` | orange | 18/18.4/10.5/19.4/23 | hart 19.4 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/netherite_launchpad_charge_3` | orange | 12/9.4/11.7/28.1/21 | hart 28.1; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/potion_pad` | orange | 20/32.4/16/14.4/13 | groesste Flaeche 20; Pad-Familie: Wirbel hart |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/potion_pad_cooling` | orange | 20/32.4/16/16/13 | groesste Flaeche 20, hart 16; Pad-Familie: Wirbel hart |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/reinforced_blast_furnace_front` | orange | 20/21.9/12.5/27.3/26 | groesste Flaeche 20, hart 27.3 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/reinforced_blast_furnace_front_on` | orange | 20/21.9/12.5/24/29 | groesste Flaeche 20, hart 24 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/reinforced_flypad` | orange | 9/8.2/7.4/1.7/99 | Farben 99 (Verlauf?); Pad: Sterne/Wirbel hart, Rahmenflaeche flach |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/reinforced_furnace_front` | orange | 20/33.2/14.8/24/18 | groesste Flaeche 20, hart 24 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/reinforced_potion_pad` | orange | 27/28.1/19.5/3.3/11 | groesste Flaeche 27; Pad-Familie: Wirbel hart |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/reinforced_potion_pad_cooling` | orange | 27/28.1/19.5/1.5/11 | groesste Flaeche 27; Pad-Familie: Wirbel hart |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/sculk_jaw` | orange | 34/54.7/28.5/1.9/7 | groesste Flaeche 34, in Flaechen>=6 54.7 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/spawn_teleporter` | orange | 8/5.5/11.3/24.4/21 | hart 24.4; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/spawn_teleporter_active` | orange | 13/13.7/18.8/25.4/14 | hart 25.4; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/spawn_teleporter_tier_2_active` | orange | 15/19.5/16.8/27.1/16 | hart 27.1; Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/stellar_flypad` | orange | 3/0/2.7/19.8/192 | hart 19.8, Farben 192 (Verlauf?); Pad: Sterne/Wirbel hart, Rahmenflaeche flach |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/warped_blazewood_bookshelf` | orange | 14/16.4/10.9/22.5/63 | hart 22.5, Farben 63 (Verlauf?) |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/weathered_copper_pressure_plate` | orange | 26/38.3/31.6/0.2/10 | groesste Flaeche 26 |
| 3 | Simple Blocks / Baubloecke | simplebuilding | `block/weathered_copper_pressure_plate_active` | orange | 24/26.2/30.1/1/20 | groesste Flaeche 24 |
