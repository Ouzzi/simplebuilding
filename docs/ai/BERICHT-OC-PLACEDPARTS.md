# Bericht oc-placedparts (Nachtrag 65: Platzierte Teile ueberlappen)

**Commits:** `b6fda045b` fix(placed-parts) (Layout + Tests), danach Vorschau-Schema + Bericht.
**Ursache:** feste Plaetze (`SLOTS`) mit ~6-7 px Abstand, aber 8-px-Platten, 8x4-Barren und 6-px-Edelsteine; dazu Drehung und nur 0,2 px `LIFT`
bei 0,8 px Plattendicke -> ab 3-4 Teilen (gemischt oder als Platten) steckten sie ineinander.
**Loesung (`PlacedSmallParts`):** `SLOTS`/`LIFT` ersetzt durch Zellen je Anzahl (`CELLS`: 1 = Block, 2 = zwei Haelften, 3 = zwei Viertel + Haelfte,
4 = Viertel). `footprint(kind,count)` + `slot(count,index,kind)`: Wunschdrehung wird in Vierteln verkleinert, bis die gedrehte Grundflaeche in die
Zelle passt, Mitte wird in die Zelle geschoben -> jedes Teil passt in jede Zelle, also nie Ueberlappung in beliebiger Mischung (Platten ab 2 Teilen
ungedreht im Raster, Eier/Kerzen/Gurken/Edelsteine weiter leicht gedreht). Barren-/Ziegelstapel (`INGOT_STACK`) war schon frei und blieb.
`center()` nimmt jetzt `Kind` (Kerzenflamme/Partikel), kein Hoehenversatz mehr noetig.
**Tests (neu, `PlacedPartsOverlapTests`, Fabric-Wrapper in `PlacingGameTest`, Katalog additiv):**
`no_placed_parts_overlap` (alle Mischungen 1-4 Teile x 9 Vertreter x 4 Richtungen x 3D/flach + alle Tag-Teile, echte Trefferformen paarweise
ohne gemeinsames Volumen) und `placed_part_slots_fit_their_cells` (ungeklemmte gedrehte Rechtecke in Zelle/Block, Zellen disjunkt).
**Gegenprobe:** Zellen von Anzahl 4 testweise auf den ganzen Block gesetzt -> beide neuen Tests ROT; zurueckgesetzt -> gruen.
**Testzeilen:** `placing_game_test_*` fabric+neoforge 26.3: `alles gruen: 22/22 bestanden, 0 rot`; `world_polish_*` 12/12, `placed_template_*` 52/52,
`*egg*` 10/10, `*sledgehammer*` 132/132, `*standing_rod*` 12/12, `*flat*` 2/2, `*candle*` 12/12 je `alles gruen`. Compile 3 Loader gruen.
`wiki/generate.py --all --check --strict`, `module_guides --check`, `discover --check` gruen.
**Nicht von mir:** `*disc*` zeigt 2 rot: `discover_game_test_behaviour_items_have_tooltip_or_wiki_entry` (sulfur_torch, sulfur_campfire ohne Tooltip/Wiki) -
Stand der Basis, unabhaengig von dieser Aenderung.
**Abweichungen/Entscheidungen:** Platten bleiben in Haeufchen 8 px gross, aber ungedreht (Rotation braucht Platz, den 4 Platten im Block nicht haben);
Zellen nutzen die volle Plattengroesse als Grundflaeche (konservativ, echte Pixelmasken sind kleiner). Kein Texturfile angefasst.
**Offen/nicht getestet:** kein Client-Screenshot (kein Display im Job); stattdessen Schema `docs/ai/previews/oc-placedparts/layout-cells.png`
(Generator `tools/placed_parts_layout_preview.py`, spiegelt die Java-Mathematik). Sichtpruefung im Spiel durch den Besitzer offen.
`tools/textures/placed_display_preview.py` (nur "Vorher"-Vorschau, `SLOTS`/`LIFT`) bewusst unveraendert. Port-Run (26.2/1.21.11): gemeinsamer Code, keine Kopie noetig fuer 26.2.
