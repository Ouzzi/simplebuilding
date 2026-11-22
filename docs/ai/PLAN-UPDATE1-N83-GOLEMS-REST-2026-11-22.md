# Plan Update 1 – N83-Rest: Golem-Modelle R3, Pflaster-Mechanik, Plüsch, Statue/Kopf (Sitzung B, 22.11., NUR PLAN)

> Status: **nur Plan vor dem Erst-Release** (O6-2). Nichts bauen, bis der Besitzer Update 1 freigibt.
> Übersicht und Reihenfolge: `docs/ai/PLAN-UPDATE1-UEBERSICHT-2026-11-22.md`.

Grundlage: Vorgabe `/root/sb-tools-a/n83-golems.md`, Auftrag `/root/b-auftrag-20261109.md` Abschnitte „## N83“,
„## ABNAHME 18.11. … N83 Metall-Golems“, N89 (Golem-Statue/-Kopf), alter Plan `docs/ai/PLAN-N83-GOLEMS-2026-11-17.md`
(auf b-golem83), B-Plan `/root/sb-tools-b/n83/PLAN.md`, Vorschau `/root/sb-tools-b/previews/n83.html`, Memory
`n83-golems-offen`. Branch **b-golem83** (weiter), Vorschau **N83B** mit Spielbildern.

## 1. Ziel und Besitzer-Abnahme (18.11., über A – verbindlich)
| Code | Abnahme | Folge für den Bau |
|---|---|---|
| **N83-1** | Messing **ok** – OHNE Text-Overlays (keine schwebenden Texte/Actionbar am Golem; Modus nur über Schlüssel/Optik) | Aktionsleiste raus, Modus nur über Aufziehschlüssel-Stellung + Klick/Partikel |
| **N83-2** | Bronze **EINWAND** – Vanilla-Speer statt eigenem Modell-Speer; Speer-Angriff als **Dash** (Sprint-Stoß wie Vanilla-Speer) | Speer = echtes Vanilla-Speer-Item in der Hand gerendert; Dash-Angriff |
| **N83-3** | Edelstahl ok | Modell R3 wie Vorschau |
| **N83-4** | Enderite ok | Modell R3 wie Vorschau (Kopf, gerundete Schultern, blass) |
| **N83-5** | Pfad-Blöcke ok + Mechanik **„Pflastern“**: Meißel Haupthand + Material Nebenhand, Rechtsklick auf normalen Pfad (erst Schaufel, dann pflastern); Verbrauch 1 Kies / 4 Ziegel oder 1 Ziegelblock / 1 Bretter / 1 Tuffziegel …; **Pfad-Stufe** ebenfalls pflasterbar; Abbau: oben = Werkzeug des Materials, Seite = untere Hälfte, unten = Schaufel; Material droppt | neue Pflaster-Pfade + Pfad-Stufen, Interaktion, Abbau-Regeln |
| **N83-6** | Werte ok – Hoplit läuft langsam wie ein Golem, kann aber den Dash | Werte der Vorschau-Tabelle übernehmen, Bronze-Tempo Golem-langsam |
| danach | Plüsch je Golem, Golem-Statue/-Kopf (9 rote TZ-Felder); Release: **erstes Update nach Release**; Vorschau N83B mit Spielbildern | |

Aus N77K2 (Konzept) gilt: Eisengolem-Plüsch (großer Kopf, kleiner Körper, mehrere Posen) ok; je neuem Golem ein Plüsch im
gleichen Stil.

## 2. Ist-Zustand (erhoben 22.11.)
- **b-golem83 81a047d644** (Hub `/root/sb.git`), 15 Commits vor, **392 hinter** claude-wave1 (Basis cf060f6938).
  Probe-Merge (`git merge-tree`): **Konflikte** in `OresFabric`, `OresFabricClient`, `OresForge`, `OresNeoForge`,
  `lang/de_de.json` + `en_us.json`, `tools/gen_data.py` und generierten Wiki-Daten.
- Gebaut (Modul `simpleores`, Paket `com.simpleores.golem`): `MetalGolem extends IronGolem` × 4 (Messing/Bronze/
  Edelstahl/Enderit), Messing-Modi Patrouille/Wandern/Wache mit Pfadgraph (`PathGraph`, Tag `#simplelib:paths` = nur
  `dirt_path`), Bronze mit Vanilla-Kupferspeer-Reichweite (2,25, Schlag alle 30 Ticks), Enderit-Teleport + „blass“,
  Nugget beruhigt / Barren heilt, Bau mit Kürbis, Beute; 9 Gametests `golem_*`; Wiki/Lang aus `gen_data.py`.
- **Platzhalter**: `MetalGolemRenderer` = getönter Eisengolem, Speer-Schicht liegt falsch (waagerecht); Messing zeigt den
  Modus heute in der **Aktionsleiste** (widerspricht N83-1). Werte im Code 85/70/140/180 HP vs. Vorschau 80/70/130/160.
- **Fehlt ganz**: Modelle R3, Spawn-Eier, Pflaster-Pfade (kein `*_path`-Block außer `simplevault:path_tile`), Pfad-Stufe
  (es gibt `simpleblocks:dirt_slab`, aber keine Pfad-Stufe), Plüsch (auch Eisengolem-Plüsch nur Konzept), Statuen/Köpfe.
- Testzentrale `layout_lines_metals.json`, Spalten `m_golem_statue`/`m_golem_head`: **9 × `#red`** = Statue Eisen,
  Enderit, Messing, Bronze, Edelstahl + Kopf Enderit, Messing, Bronze, Edelstahl (vorhanden: Kupfergolem-Statue Vanilla,
  `simplemobheads:copper_golem_head`, `iron_golem_head`).
- NeoForge-Lauf von simpleores scheiterte früher („Registry is already frozen“); Erst-Release und Update sind nur Fabric.

## 3. Entscheidungen von B (begründet)
1. **Erst rebasen, dann bauen**: b-golem83 auf claude-wave1 **nach** dem N96B-Bündel (beide ändern simpleores-Loader-
   Klassen, `gen_data.py`, `check_data.py`, `OresTests`), damit nur einmal Konflikte gelöst werden.
2. **Werte = Vorschau-Tabelle** (vom Besitzer mit „Werte ok“ abgenommen): HP Messing 80, Bronze 70, Edelstahl 130,
   Enderit 160; Schaden wie Vorschau; Bronze Tempo 0,23 (Golem-langsam).
3. **Bronze-Dash (N83-2)**: Vanilla-Speer-Item (Kupferspeer, Bronze = Kupferlegierung) als Haupthand, über Vanilla
   `ItemInHandLayer` am Modell-Arm gerendert; Angriff = Anlauf-Stoß wie der Vanilla-Speer: bei Ziel in 3–6 Blöcken kurzer
   Sprint (Impuls nach vorn, ≈ 0,8 s), Treffer mit Reichweite des Speers und Bewegungs-Bonus, danach 3 s Erholung;
   im Nahkampf normaler Stoß. Laufen sonst Golem-langsam.
4. **Pflastern (N83-5) in `simpleblocks`** (Bauklötze, hat schon `dirt_slab`): neue Blöcke `gravel_path`, `cobblestone_path`,
   `brick_path`, `tuff_brick_path`, `<holz>_plank_path` (je Vanilla-Holz, siehe 7) + gleiche Reihe als Pfad-Stufe
   (`dirt_path_slab` per Schaufel aus `dirt_slab`, gepflastert `*_path_slab`); Meißel über Item-Tag (keine harte Abhängigkeit
   auf `simplechisels`); alle Pfade in `#simplelib:paths` (Golem-Patrouille nutzt sie sofort).
5. **Statuen in `simpleores`** (bei den Golems, Muster Vanilla-Kupfergolem-Statue mit Posen), **Köpfe in `simplemobheads`**
   (dort liegen Eisen-/Kupfergolem-Kopf; nur registriert, wenn `simpleores` geladen ist – optionale Abhängigkeit).
6. **Plüsch in `simpleores`** neben den Statuen (5 Stück: Eisen + 4 Metall-Golems, Posen per Rechtsklick wie die Statue).

## 4. Arbeitspakete (Reihenfolge)
| # | Paket | Aufwand |
|---|---|---|
| P0 | Rebase/Merge b-golem83 auf claude-wave1 (nach N96B), Generatoren neu, Gametests + precheck grün | M |
| P1 | N83-1: Aktionsleiste/Text raus, Modus nur über Schlüssel-Stellung (synchronisiert) + Klick/Partikel; Werte auf Vorschau-Tabelle; Gametests anpassen | S |
| P2 | N83-2: Bronze Vanilla-Speer in der Hand + Dash-Angriff + Erholung; Gametests (Dash trifft auf 4–5 Blöcke, Tempo, kein Dash im Nahkampf) | M |
| P3 | Modelle R3 als Entity-Modelle (4 × `LayerDefinition` + Texturen nach Vorschau N83, Schlüssel-Stellungen, Enderit blass, Speer-Arm aufrecht), Renderer ersetzen, **Spawn-Eier** (4 Ei-Texturen) | L |
| P4 | Pflaster-Pfade + Pfad-Stufen (simpleblocks): Blöcke, Texturen, Pflastern-Interaktion, Verbrauch, Abbau-Regeln je Seite, Drops, Tag `#simplelib:paths`, Rezepte/Säge; Gametests | L |
| P5 | Statuen (5) + Köpfe (4) = 9 rote TZ-Felder; Testzentrale-Config `#red` → Ids, Editor/Wiki nachziehen | M |
| P6 | Plüsch 5 × (Modelle, Posen, Texturen) | M |
| P7 | Texte EN/DE, Wiki, Fragebuch, Discover, Money-Preise, Vorschau **N83B** (Spielbilder: Modelle, Dash, Pflastern, Statuen/Köpfe/Plüsch) | S |

Summe: **≈ L+ (≈ 9–12 Agenten-Tage)**.

## 5. Risiken
- **Kompatibilität / Merge:** 392 Commits Rückstand, Konflikte in Loader-Klassen und Generatoren (siehe 2); gleiche Dateien
  wie N96B → strikt nacheinander. Mixin `CarvedPumpkinMetalGolemMixin` gegen andere Kürbis-Mixins (Vanilla-Golem-Bau)
  im Gesamt-Gate prüfen. `extends IronGolem`: Dorf-Logik zählt Metall-Golems als Golems (bewusst, benannt).
- **Welt-Migration / IdMoves:** alle Ids neu (Golems, Eier, Pfade, Statuen, Köpfe, Plüsch) – nichts aus dem Erst-Release
  wird umbenannt, kein IdMove. Golems waren nie veröffentlicht → NBT-Felder (`GolemMode`) frei änderbar. Pfad-Tag
  `#simplelib:paths` wird erweitert (rein additiv).
- **Texturen / Vorschau-Karten:** Modelle R3 sind als Konzept abgenommen, im Spiel aber neu gezeichnet → Spielbilder in
  **N83B** (ein Abnahme-Block, ok/Einwand je Golem). Neu und noch nie gezeigt: Pflaster-Texturen der Holz-/Ziegel-/Tuff-
  Varianten, Pfad-Stufen, Statuen, Köpfe, 4 Plüsch, Spawn-Eier → eigener Block in N83B (Memory
  `feedback-texturen-abnahme-bloecke`), keine Mojang-Pixel.
- **Performance:** Pfadgraph-BFS (≤ 256 Knoten, alle 30 s) je Messing-Golem; bei vielen Golems Neubau zeitlich staffeln.
  Pfad-Stufen + Holzvarianten erhöhen die Blockanzahl (≈ 2 × (4 + 12 Hölzer) ≈ 32 Blöcke, je wenige Zustände) – unkritisch.
- **Abbau-Regel „Seite = untere Hälfte“** ist mehrdeutig (siehe 7) – falsche Deutung kostet einen Umbau der Block-Logik.

## 6. Testplan
- **Gametests** (`module-simpleores-fabric-263` + standalone): vorhandene 9 `golem_*` grün, neu: kein Aktionsleisten-
  Text beim Moduswechsel (N83-1), Bronze-Dash (Abstand, Treffer, Erholung), Werte = Vorschau, Spawn-Ei setzt Golem,
  Statue/Plüsch Posen + Drops. `module-simpleblocks-fabric-263` (+ standalone): Schaufel macht Pfad/Pfad-Stufe, Pflastern
  mit Meißel+Material (Verbrauch je Material, Survival vs. Kreativ), Abbau oben/seitlich/unten mit Drops, Tag-Inhalt,
  Golem-Patrouille folgt gepflastertem Pfad. `module-simplemobheads-fabric-263`: Köpfe nur mit simpleores. Dazu
  `fabric-263`, `integration-263`, precheck.
- **Client-Probe** `uiprobe_n83b.py` (ersetzt `uiprobe_n83.py`): Probe-Lehren – `difficulty normal` im _prep (Zombie als
  Ziel für den Dash), `gamerule spawn_mobs false`/`advance_time false`, **keine echten Klicks nach `tp … facing`** (Messing-
  Schlüssel-Bilder zeigten in den Himmel → Modus per Befehl/`execute … run data` setzen, Schlüssel-Stellung per Blockdaten
  prüfen), Zielhöhe −1,62, Reichweite Entities ~4; Szenen: 4 Golems frontal/seitlich, Messing 3 Schlüssel-Stellungen,
  Bronze mit Speer + Dash-Serie, Enderit blass/voll, Pflaster-Reihe + Pfad-Stufen, Statuen/Köpfe/Plüsch, Testzentrale-Zeile.
- **Dev-Klicktest** `/root/n74dev/n83b_wiki.cjs`: Golem-Seite, Pfad-Blöcke, TZ-Editor Spalten Statue/Kopf ohne Rot/„?“, DE+EN.
- **bprep** `b-golem83` → „BPREP GRUEN“, Bündel + Gate.

## 7. Vor dem Bau vom Besitzer zu klären
1. **Abbau-Regel N83-5:** B-Deutung: der gepflasterte Pfad hat zwei Teile – oben die Pflasterschicht (Abbau von oben mit
   dem Werkzeug des Materials → Pflaster droppt, Erd-Pfad bleibt), seitlich trifft man die untere Erd-Hälfte (Schaufel →
   ganzer Block, Pflaster droppt mit), von unten = Schaufel. Stimmt das?
2. **Holz-Pflaster:** je Holzsorte ein eigener Pfad (12 Vanilla-Hölzer, ggf. + Mod-Hölzer) oder **ein** Holzbohlen-Pfad aus beliebigen Brettern
   (Vorschau N83 zeigte einen)? Vorschlag: je Holzsorte (Farbe passt zum Bau).
3. **Statue vs. Plüsch:** sind die TZ-Felder „Golem-Statue“ eigene Statuen (wie Kupfergolem-Statue, Golem-Proportionen)
   **und** zusätzlich Plüsch (chibi), oder soll der Plüsch die Statue sein? Vorschlag: beides getrennt (Statue = TZ-Feld,
   Plüsch = Deko-Item).
4. Abnahme **N83B** (Spielbilder) vor dem Bündeln.
