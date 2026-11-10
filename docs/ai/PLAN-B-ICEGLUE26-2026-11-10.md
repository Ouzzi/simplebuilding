# PLAN B-ICEGLUE26 (10.11.2026) - WD25-2 / WD25-4 Eis-Kleber

Ist: End-Blaetter (EndLeavesBlock) haben WAXED (Honigwabe/Schleimball, Axt ab, Silk/Schere behaelt Zustand). Rissiges Eis (CrackedIceBlock, CrackedHardIceBlock in modules/simpleblocks nature) hatte nichts davon.
Umsetzung (Muster uebernommen, geteilt in CrackedIceBlock-Statics): Property WAXED an allen drei Bloecken (Rissiges Eis/Packeis/Blaueis; Blockstate-JSONs matchen `age=N` weiter). Schleimball (Klang Schleim) oder Honigwabe (Vanilla-Wachs) -> WAXED: kein stepOn-Tick, crack() tut nichts, randomTick (Lichtschmelze) aus. Axt kratzt ab (Wax-off, 1 Haltbarkeit). getDrops: bei WAXED oder AGE>0 traegt der Silk-Drop BLOCK_STATE(age, waxed); Platzieren stellt ihn her. Ohne Silk weiter kein Drop; frisches Eis stapelt wie vorher.
Abweichung: "Wachs spaeter Kleber" -> Kleber = Schleimball wie bei den End-Blaettern (kein neues Item).
Tests: NatureTests cracked_ice_glue_freezes_state, cracked_ice_glue_silk_keeps_state (+ Fabric-Adapter); bestehende Risstests decken "ohne Kleber wie Vanilla". Wiki-Notiz EN/DE in modules/simpleblocks/wiki/manual.json (wiki/data nicht regeneriert, macht das Buendel). Kein neuer Lang-/Tooltip-Text noetig (kein neues Item).
Verifikation: ein sbt: Compile + module-simpleblocks-fabric-263,module-simpleblocks-standalone-fabric-263 + data_integrity.
