---
name: loot-format-26-3
description: "26.3 liest Loot-Tabellen nur im neuen Format (condition/modifier); das alte conditions/functions wird still ignoriert - 21 handgeschriebene Tabellen offen (Stand 15.10.2026, oc-n58-small)"
metadata:
  node_type: memory
  type: project
---

Befund (oc-n58-small, bewiesen am Eisen-Tiegel-Rohling): Eine Loot-Tabelle mit den Vor-26-Listen `"conditions": [...]` und `"functions": [...]`
(`{"condition": "minecraft:x"}`, `{"function": "minecraft:set_count"}`) laedt auf 26.3 ohne Fehler, aber Bedingungen und Funktionen
fehlen ganz (alle Pools gaben ihre Items, `set_count` wirkte nicht). Das war die Ursache der "greift nicht"-Umgehung per `getDrops`.
26.3-Format wie Vanilla (candle/wheat): Pool/Eintrag haben GENAU EIN `"condition": {"type": "minecraft:match_block", "blocks": ..., "state": {...}}`
(Mehrere: `all_of`), Funktionen als `"modifier": [{"type": "minecraft:set_count", "condition": ..., "count": n}]`.

Offen: 21 handgeschriebene/skriptgenerierte Tabellen noch im alten Format (`python3 tools/check_loot_format.py`, Baseline `tools/loot_format_baseline.txt`):
mc26_3/overlay (7 Bloecke), simplelib (9 Bloecke + chests/village_field_kitchen, Generator `modules/simplelib/tools/gen_resources.py`),
simplemobs (deceiver, deceiver_armor_bonus, hollow_howler), simpleenddragonfight (dragonmite). Folgen: killed_by_player/Zufall/Looting/set_count
werden ignoriert, survives_explosion fehlt. Vor der Umstellung die zugehoerigen Tests ansehen (koennen auf das falsche Verhalten geeicht sein).
Wenn erledigt: Eintrag + Baseline-Datei entfernen.
