# Plan Update 1 – N96B Sheets-Bündel (mehrseitige Sheets, Nihilit-Sheet, N96C-Overlay, Blei 4/8) (Sitzung B, 22.11., NUR PLAN)

> Status: **nur Plan vor dem Erst-Release** (O6-2). Nichts bauen/bündeln, bis der Besitzer Update 1 freigibt.
> Übersicht und Reihenfolge: `docs/ai/PLAN-UPDATE1-UEBERSICHT-2026-11-22.md`.

Grundlage: Auftrag `/root/b-auftrag-20261109.md` Abschnitte „## N96“ (Punkte 2 + 3), „N96 Nachtrag Besitzer“ (Blei-Dicke),
„## ABNAHME 19.11. … N96B Einwand“; Bau-Plan `docs/ai/PLAN-N96-SHEETS-2026-11-19.md` (auf b-sheet96); b-fertig
„N96B Sheets … bündelbar“ (~14:50) und „N96C fertig – b-sheet96 b0a4eee99d“ (~19:45); Memory `n96-sheets-offen`.
Dieser Plan beschreibt **nur noch das Fertigmachen und Bündeln** – gebaut ist alles.

## 1. Ziel und Besitzer-Entscheidungen (verbindlich)
| Code | Vorgabe / Abnahme |
|---|---|
| **N96-2** | Sheets an mehreren Seiten eines Blocks gleichzeitig, Lagen je Seite stapelbar, Abbau droppt alle; Kollision/Form, Wasser, Licht, Drops, Gametests (Beispiel Tunnel unten 2 / oben 2 / links + rechts je 1) |
| **N96-3** | Sheet + Nihilit im Amboss (0 Erfahrung, keine Amboss-Abnutzung) → „Nihilit-Sheet“, doppelte Kapazität; **EIN** Overlay per Skript auf alle Sheet-Varianten |
| **Nachtrag** | „Blei ist 2 px hoch statt 4 px → Maximum je Block halbiert: **4 normal, 8 mit Nihilit**“; Maximum aus der Lagen-Dicke ableiten, nicht fest 8/16 |
| **N96B** | **EINWAND** 19.11.: Nihilit-Overlay viel zu stark → nur ein klein wenig dunkler und bläulicher, erkennbar, aber dezent; neue Vorschau N96C (alt/neu); b-sheet96 bleibt fürs erste Update |
| **N96C** | gebaut (gleichmäßige Dunkelblau-Tönung 24,46,86 @ 22 %), **Abnahme offen** (`/root/sb-tools-b/previews/n96c.html`) |
| N96-1/-4 | Dino-Texturen + Druckplatten-Item-Darstellung: **N96A ok**, schon in Production (Bündel 218) – nicht Teil dieses Bündels |

## 2. Ist-Zustand (erhoben 22.11.)
- **b-sheet96 b0a4eee99d** (Hub), 12 Commits vor, **294 hinter** claude-wave1 (Basis f38cbecd01), 81 Dateien.
  Probe-Merge (`git merge-tree`) gegen claude-wave1 bc6b916725: **Konflikte** in `simpleores/test/OresTests.java`,
  `simpleores/tools/check_data.py`, `testcentre/layout_lines_metals.json`, `tools/landing/discover.html` und generierten
  Wiki-Daten (`guidebook.js`, `modules.js`, `questionary.js`, `simpleores.js`); `gen_data.py` und Lang mergen automatisch.
- Gebaut (Modul `simpleores`): Dicken-Budget 8 px je Zelle, Nihilit halbes Gewicht → **Blei 4/8, Silber 8/16** (eine Regel
  für alle Platten-Sheets, im Bau-Plan §1 begründet); einseitig = bisheriger Block (+ `waterlogged`), mehrseitig =
  `simpleores:sheet_cell` + BE + `SheetCellRenderer`; Zustände 482 (vorher 96); `nihilith_lead_plate`,
  `nihilith_silver_sheet`, `NihilithSheetAnvilMixin`, Overlay `tools/nihilith_overlay.py --check`; N96C-Tönung.
  Holz-Sheets (`<holz>_sheet`, Glasscheiben-Form) bewusst ausgenommen.
- Geprüft auf dem Branch: precheck 93/93, `module-simpleores-fabric-263` 105/105 (+ standalone), `fabric-263` 1170/1170,
  `integration-263` 8/8, Spielprobe `simpleinterfaces/n96b` 1/1 CLEAN (Tunnel, Overlay, Blei-Maximum schiebt nach oben).
- Offen: N96C-Abnahme; Rebase; NeoForge/Forge nur kompiliert (Update nur Fabric); Fund `mirror_faces`-Gametest entfernt
  `Properties` statt 26.3-`properties` (prüft nichts); Explosionswiderstand der Zelle = Silber (6), Zelle nicht
  kolbenverschiebbar (BE).

## 3. Arbeitspakete (Reihenfolge)
| # | Paket | Aufwand |
|---|---|---|
| P1 | Warten auf **N96C**; bei Einwand Tönung im Overlay-Skript anpassen (ein Wert), Vorschau N96D, sonst weiter | S |
| P2 | b-sheet96 mit claude-wave1 zusammenführen (Konflikte aus 2 lösen: OresTests-Liste vereinigen, check_data-Prüfungen beider Seiten, TZ-Layout-Zeile Blei/Silber + Nihilit-Spalte prüfen), Generatoren neu (`gen_data.py`, `check_data.py`, wiki/generate, landing, Texturindex) | M |
| P3 | Fund `mirror_faces` beheben (Schlüssel `properties`), Gametest prüft wieder echte Alt-Zustände | S |
| P4 | Volle Prüfung nach dem Rebase: Gametests, Spielprobe n96b erneut, Dev-Klick | S |
| P5 | bprep → „BPREP GRUEN“, Meldung bündelbar fürs Update-1-Bündel; Changelog-Zeile (Blei-Stapel 4 statt 8) | S |

Summe: **≈ M (≈ 1,5–2,5 Agenten-Tage)** – kleinstes Vorhaben, Voraussetzung für N83 (gleiche Dateien in simpleores).

## 4. Risiken
- **Kompatibilität mit dem Erst-Release:** Im Erst-Release stapelt die Bleiplatte bis 8 Lagen. Nach dem Update gilt 4 (bzw. 8
  mit Nihilit). **Bestehende Bleiplatten mit 5–8 Lagen bleiben erhalten** (Zustände existieren weiter), man kann nur nicht
  mehr daraufstapeln → Changelog. Merge-Risiko: generierte Dateien nie von Hand lösen, immer neu erzeugen.
- **Welt-Migration / IdMoves:** keine Id-Änderung → kein IdMove/DataFixer. Neue Eigenschaft `waterlogged` mit Standard
  `false` → alte Paletten laden unverändert (Gametest `sheet_migration`). BE-Einträge unbekannter Blöcke fallen beim Laden
  weg, leere Zelle wird Luft/Wasser.
- **Texturen / Vorschau-Karten:** einzige offene Karte ist **N96C**; nach Rebase keine neuen Pixel. Grundtexturen bleiben
  Vanilla-abgeleitet beim Build (keine Mojang-Pixel im Repo), Overlay ist ein zweites Modell-Element.
- **Performance:** Block-Entity + Renderer nur für mehrseitige Zellen (einseitig bleibt Chunk-Modell); viele Zellen =
  BE-Renderer-Kosten wie gemischte Octets – in der Probe mit einem 3×3-Tunnel unauffällig; bei großen Flächen (≥ 1000
  Zellen) einmal FPS-Messung in der Probe (NOTE).
- Explosionswiderstand/Kolben (siehe 6) – Verhalten bewusst, aber für Spieler überraschend.

## 5. Testplan
- **Gametests** nach dem Rebase: `module-simpleores-fabric-263` normal + standalone (u. a. `sheet_sides_tunnel`,
  `sheet_budget` Blei 4/8 + Silber 8/16, `sheet_water_light`, `sheet_migration`, `nihilith_sheet_anvil` 0 XP/keine
  Abnutzung, `sheet_cell_soundproofing`, `plate_layers`, korrigiertes `mirror_faces`), `fabric-263`, `integration-263`,
  precheck, `tools/nihilith_overlay.py --check`, `check_data.py`.
- **Client-Probe** `simpleinterfaces/n96b` (vorhanden) erneut auf dem rebasten Stand; Probe-Lehren: `random_tick_speed 0`,
  Bauen per `/setblock`/Befehlen statt Klicks nach `tp … facing`, Zustände über Blockdaten prüfen. Befehl für die
  Hauptsitzung: `/root/sbt b-sheet96 "timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe96 --scale 2 --only simpleinterfaces/n96b"`.
- **Dev-Klicktest** `/root/n74dev/n96b_wiki.cjs`: Simple-Ores-Wiki (Bleiplatte, Silberblech, Nihilit-Varianten), TZ-Editor
  Zeilen Blei/Silber, keine „?“, DE+EN.
- **bprep** → „BPREP GRUEN“; Bündel + volles Gate (Java/Mixin → kein Express).

## 6. Vor dem Bau vom Besitzer zu klären
1. **Abnahme N96C** (dezentes Nihilit-Overlay) – einzige echte Blockade.
2. **Zelle:** Explosionswiderstand = Silber (6) für die ganze Zelle und nicht kolbenverschiebbar – so lassen (Vorschlag)?
3. Zur Kenntnis: die Dicken-Regel gilt für **alle** Platten-Sheets (Silber 1 px bleibt 8/16, Blei 2 px 4/8); alte Bleistapel
   über 4 bleiben stehen.
