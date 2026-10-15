# Bericht oc-wanderer-concept (2026-10-15)
- Nur Konzept, kein Code: `docs/ai/KONZEPT-DORF-WANDERER-2026-10-15.md` (148 Zeilen; Queue unverändert).
- Ist-Zustand aus MC-26.3-Quellen/-Daten + grep: Vanilla-Spawner (24000-Tick-Würfe, 2 Tage Despawn, 2 Lamas, 9 Angebote einmalig, kein Restock/Reroll); Projekt hat keinen eigenen Händler-Mob.
- **Fund:** die 238 Simple-Money-Preislisten für den Wandering Trader stehen im Tag `wandering_trader/rare`, den kein Vanilla-Trade-Set liest → per grep nie angeboten (im Spiel nicht geprüft; Stufe 0 im Konzept).
- Entscheidungen: neues Modul `simplewanderers`; Dorf-ID = Start-Chunk der Dorfstruktur (Bell-POI + `StructureTags.VILLAGE`), SavedData, 3-fach-Absicherung „genau einer“; Respawn 7 Tage nur bei Spieler vor Ort.
- Pferd: eigener `WandererHorse`, nicht reitbar/abnehmbar; Rotation per Spielzeit (Reroll 72000, Restock 36000 Ticks, deterministischer Seed, kein Nachhol-Burst).
- Trade-Aufteilung vollständig (Vanilla common 78 = 31 WT + 47 DW; uncommon/SB/Kerne bleiben am WT, DW bekommt Kopien/14 Köpfe mit Chance 0,1; kein Tier ≥4 am DW), um Kern-Kalibrierung und bestehende Tests nicht zu brechen.
- Textur: eigene Handpixel (kein Vanilla-Derivat), Pferd mit gefärbter Lederrüstung statt neuer Textur. 11 offene Besitzerfragen mit Empfehlung (Abschnitt 10).
- Nicht getestet: alles Spielverhalten (Leine, Strukturerkennung, Navigation) – im Konzept als per Test zu belegen markiert; kein Gate nötig (nur Doku).
