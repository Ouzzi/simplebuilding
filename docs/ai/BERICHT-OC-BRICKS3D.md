# Bericht oc-bricks3d (Nachtrag 55: Ziegel/Diamant/Smaragd in 3D, Hammer-Umschaltung)

Ist-Zustand: Barren liegen als 3D-Teile im Kleinteil-Haeufchen (`PlacedSmallParts`, Kind INGOT, Schleichen + Rechtsklick; Modelle
`placed_<barren>` aus `tools/textures/placed_ingots_2026_10_09.py`). Daran angehaengt, keine neue Blockart.
- Commits: `Placed bricks, diamonds and emeralds in 3D; sledgehammer switches ...` + dieser Abschluss-Commit (Client-Test, Vorschau, Bericht).
- Neu: `Kind.BRICK` (brick, nether_brick, resin_brick; Quader 8x4x3, stapelt mit Barren), `Kind.GEM` (diamond, emerald; stehender
  abgestufter Stein, Stufen = Sprite-Silhouette im halben Massstab, `gemBoxes`). Modelle/Item-Defs per `tools/textures/placed_gems_bricks_2026_10_15.py`
  (nur JSON, **keine neue/abgeleitete Textur**: die Modelle nutzen `minecraft:item/*` direkt; kein Atlas-Eintrag noetig, im Client-Test belegt).
  Mod-Ziegel gibt es nicht. Keine abgenommene Textur angefasst.
- Umschalten: Blockstate `flat` an `PlacedSmallPartsBlock`; `FlatToggles` (Rechtsklick mit Vorschlaghammer, 1 Haltbarkeit, `mayTransform`, Hinweis in
  `TransformTargets`); gilt fuer alle unterstuetzten Teile des Flecks (Barren, Ziegel, Diamant, Smaragd), Eier/Kerzen/Gurken/Plattenteile bleiben. Flach = Platte
  wie bisher (Renderer/Trefferform/Partikel nutzen `kind(part, flat)`); `flat` faellt weg, wenn kein unterstuetztes Teil mehr da ist. Drops unveraendert.
- Tests (neu, `PlacedBricksGemsTests`): `placing_game_test_bricks_and_gems_lie_in3d`, `..._hammer_switches_placed_parts_between3d_and_flat`,
  `..._switched_parts_drop_as_themselves`. Lauf: `--filter 'simplebuilding:placing_game_test*'` Fabric+NeoForge+Forge 26.3: je 9/9, "alles gruen" (18/18 bzw. Forge+Fabric 18/18).
  Regression gruen: `music_disc*` 16, `sledgehammer*` 54, `placed_template*` 26 (Fabric+Forge). `check_missing_textures` 0 Fehler, wiki `--all --check --strict` ok, module_guides/discover ok.
- Client (Fabric+NeoForge 26.3, xvfb): `SIMPLEBUILDING_CLIENT_ONLY=placing-bricks3d` -> `docs/ai/previews/oc-bricks3d/placing-n55-3d.png` / `-flat.png`. Die Runner-Zeile "placing-n24 fehlt" kommt nur vom Filter (anderer Eintrag derselben Datei).
- Entscheidungen: "Schlag" = Rechtsklick des Hammers (wie Schallplatten-Wenden; Linksklick bleibt Abbau eines Teils); Schallplatten haben Vorrang vor dem Umschalten;
  Umschalten je Fleck, nicht je Teil; Harzziegel zusaetzlich zu Ziegel/Netherziegel.
- Offen/ungetestet: `data_integrity_game_test_every_mod_block_loot_table_loads` ist rot (astral_enchanting_table-Loot, nicht durch diese Aenderung; nicht auf Baseline gegengeprueft).
  Volles Gate und 26.2/1.21.11-Port nicht gelaufen (26.2 kompiliert die geteilten Quellen nicht eigens geprueft). Formen sind Erstentwurf, Optik bitte abnehmen; Wiki-Text EN+DE ergaenzt.
