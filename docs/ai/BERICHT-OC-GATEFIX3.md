# Bericht oc-gatefix3 (2026-10-14)
- Basis: claude-wave1 92a2a92c5 (Gate-Rot war 4f1d5a8e2; 40 Commits liegen dazwischen).
- every_item_has_an_item_definition...: Ursache war das Modell cracked_ice_N mit Sprite `simplebuilding:block/cracked_ice_N`
  (Stand 4f1d5a8e2). Auf HEAD schon durch das Datagen-Update (da031d424: Modell auf minecraft:block/frosted_ice_N) behoben; kein weiterer Eingriff,
  Test auf HEAD gruen. Keine Mojang-Textur eingecheckt.
- mod_item_names_follow_the_family_patterns: de_de-Tooltip von enableSimpleBeacons nannte "Starke Aufwertungsvorlage" (verbotenes Altwort
  "Aufwertungsvorlage"). Text umformuliert ("die passende starke Vorlage"), src/main + mc26_3/overlay. Itemname im Modul bleibt unveraendert.
- config_option_game_test_*_persisted_name_and_default / *_name_tooltip_and_tab: neue Option server.oreGeneration.crackedIceFields (N53)
  fehlte in der gepinnten Liste EXPECTED_OPTIONS (ConfigOptionTests); bewusst ergaenzt (boolean=true, Lang EN+DE existiert).
- wiki/data/textures.js (und simplebuilding.js/.json) mit `python wiki/generate.py --all` neu erzeugt (SimpleOvens-Loren-Eintraege waren veraltet).
- Tests: data_integrity 49/49 und config_option 20/20 auf Fabric UND NeoForge 26.3 gruen (je 98/98 bzw. 40/40).
- Pruefungen: wiki generate --all --check --strict ok, module_guides --check ok, discover --check ok.
- Nicht geprueft: Forge 26.3, volles Gate. Auffaellig (nicht angefasst): tools/check_tooltip_text.py meldet
  "Long tooltip" modules/simplemobheads de_de ability.simplemobheads.allay.tooltip.
