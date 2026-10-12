# Plan: Aschholz/Dämmerholz bekommen alles (claude-endwood-full, 2026-10-12)

Besitzer 12.10.: `simpleend:ashwood` und `simpleend:duskwood` bekommen alles, was die anderen Holzarten haben.
Basis `claude-wave1` cf8620847. Regeln: `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md` (Simple End allein spielbar, Fremdes nur
bedingt), `PLAN-SIMPLE-END-2026-10-12.md` Entscheidung 8 (Stufe 7 war offen).

## Inventur (Holzarten-Features im Repo, Stand cf8620847)
| Feature | Ort | Liste | End-Hölzer vorher |
|---|---|---|---|
| Vanilla-Holzset (Stamm … Boot) | simpleend `EndWood` | – | ja |
| Vanilla-Regal (`<holz>_shelf`, 26.3) | simpleend | `gen_data.py` schließt `shelf` aus | **nein** |
| Holzwerk: hohler Stamm (+entrindet), Platte (+entrindet), Holzkessel (+entrindet), Kiste, geschnitztes Holz | SB `woodwork/WoodKind` + `WoodBlocks`, Generator `woodwork_2026_10_09.py`, Datagen | Enum, nur `minecraft` | **nein** |
| Holz-Achtel (`<holz>_octet`, Bretter) | SB `ModBlocks.OCTET_WOODS`, `octets_2026_10_09.py` | Strings, `minecraft` | **nein** |
| Stamm-Achtel (`<holz>_log_octet`, `stripped_…`) | SB `OctetMaterials` aus `octet_materials.json` | mit Namensraum | **nein** |
| Gestufte Kistenboote (Holz als Komponente) | SB `BoatWoods`, `VehicleRenderers`, Datagen | Strings, `minecraft` | **nein** |
| Meißel-Kette entrindeter Stamm → hohler Stamm | SB `ChiselItem` (aus Familien) + Datei `chisel_transformations` | aus Familien | **nein** |
| Schneidebrett | simplesandwiches `ModBlocks.WOODS`, `gen_resources.py`, `sandwiches.py` | Strings, `minecraft` | **nein** |
| Schach/Checker | SB `chess` – nach Farbe, nicht nach Holz | – | entfällt |
| Bücherregal | Vanilla: eins für alle (`#planks`), SB nur Blazewood (Astral) | – | entfällt (siehe Abweichungen) |
| Fragentisch | Regex-Familien (`.*_crate`, `hollow_.*` …) | – | greift automatisch |

## Technik
1. **Registrierung ohne Reihenfolge-Annahme.** Fabric ruft Entrypoints ohne Abhängigkeitsordnung auf (Loader hat sogar
   einen Shuffle-Schalter), daher liest SB/Sandwiches beim Registrieren **keinen** Simple-End-Block: Eigenschaften kommen
   aus einer Vanilla-Vorlage (Eiche) plus fester Kartenfarbe; Stamm/Bretter werden lazy über die ID
   `simpleend:<holz>_…` gelesen. Registriert wird nur bei `isModLoaded("simpleend")` (Regel 5, Vorbild Hufeisen).
2. **SB `WoodKind`** bekommt `ASHWOOD`, `DUSKWOOD` (Namensraum `simpleend`), `WoodKind.active()` filtert. Achtel:
   `OCTET_WOODS` bekommt die End-Hölzer, `MaterialOctetBlock` eine lazy Quelle. `BoatWoods.ALL` bedingt; eigener kleiner
   Renderer (Vanilla-Eichenboot-Modell + `simpleend:textures/entity/boat/<holz>.png`).
3. **Daten** (SB-Datagen läuft ohne Simple End): neuer Generator `tools/textures/endwood_2026_10_12.py` schreibt Rezepte,
   Beute, Meißel-Datei aus den Fichten-Dateien als Formatvorlage, jede Datei mit `fabric:load_conditions` +
   `neoforge:conditions` + `forge:condition` (`simpleend` geladen). Tags (`mineable/axe`, `mineable/pickaxe`) per Datagen als
   `required:false`. Modelle/Texturen über die vorhandenen Generatoren (Holzwerk, Achtel) mit Namensraum je Holz.
4. **Simple End**: Regal je Holz (Vanilla `ShelfBlock`, Blockentity-Gültigkeit per vorhandenem Mixin), Daten über
   `gen_data.py` (Regal nicht mehr ausgeschlossen), Textur eigen aus der End-Holz-Palette.
5. **Sandwiches**: Schneidebrett je End-Holz bei geladenem Simple End, Rezept bedingt.

## Tests
- simpleend (`module-simpleend-*-263`, lädt SB): Regal vollständig; mit SB: Holzwerk-, Achtel-, Boot-Teile registriert,
  Rezepte geladen, Meißel; mit Sandwiches: Bretter. Standalone: nur Regal, keine SB-IDs.
- SB: `data_integrity*`, `octet*`, `woodwork*` (ohne Simple End unverändert grün, End-Teile fehlen dort).
- Compile 3 Loader, Generator-`--check`, `wiki/generate.py --all --check`, `check_data.py`.

## Abweichungen/Entscheidungen
- Kein eigenes End-Bücherregal: Vanilla hat eins für alle Holzarten über `#minecraft:planks` (End-Bretter sind drin);
  ein eigenes Rezept wäre mit dem Vanilla-Rezept doppelt.
