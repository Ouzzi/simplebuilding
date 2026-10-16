---
name: loot-format-26-3
description: "26.3 liest Loot-Tabellen nur im neuen Format (condition/modifier); das alte conditions/functions wird still ignoriert - alle Tabellen umgestellt, check_loot_format.py --strict haelt es bei 0 (16.10.2026)"
metadata:
  node_type: memory
  type: project
---

Befund (oc-n58-small, bewiesen am Eisen-Tiegel-Rohling): Eine Loot-Tabelle mit den Vor-26-Listen `"conditions": [...]` und `"functions": [...]`
(`{"condition": "minecraft:x"}`, `{"function": "minecraft:set_count"}`) laedt auf 26.3 ohne Fehler, aber Bedingungen und Funktionen
fehlen ganz (alle Pools gaben ihre Items, `set_count` wirkte nicht). Das war die Ursache der "greift nicht"-Umgehung per `getDrops`.
26.3-Format wie Vanilla (candle/wheat): Pool/Eintrag haben GENAU EIN `"condition": {"type": "minecraft:match_block", "blocks": ..., "state": {...}}`
(Mehrere: `all_of`), Funktionen als `"modifier": [{"type": "minecraft:set_count", "condition": ..., "count": n}]`.

Erledigt 16.10. (b-n58rest2): alle 23 Altformat-Tabellen umgestellt, Generatoren (simplelib, simplecrucibles gen_resources.py) schreiben das neue Format; keine Baseline mehr, jede neue Altformat-Tabelle macht `check` rot.
