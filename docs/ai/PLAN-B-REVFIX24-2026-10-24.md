# Plan b-revfix24 (24.10.2026): Review-Funde kritisch/hoch

Quelle: /root/sb-tools-b/review24/teil1-kern.md (F-01..F-06), teil2-module.md (H-1..H-4). Nur 26.3 Fabric.

| Fund | Ist-Zustand bestaetigt | Aenderung | Test |
|---|---|---|---|
| F-01 Amboss-Kompass | ja, `findNearestMapStructure(...,300)` bei jedem createResult | `StructureCompassCache`: Ergebnis je Spieler/Dimension/Tag/512-Block-Zelle 5 min (auch "nicht gefunden"), je Spieler 1 neue Suche pro 5 s, sonst leeres Ergebnisfeld | AnvilCrushingTests.compassSearchIsCachedAndRateLimited |
| F-02 pixels vor Budget | ja | Budget (`takeSignBudget`) vor `refusal` fuer Signieren UND pixels-Code | BlueprintTests.pixelCodeEditsPayTheParseBudgetFirst |
| F-03 Parse-Cache | ja | LRU nach Eintraegen (24) und Gewicht (8 Mio. Stellen) | BlueprintTests.parseCacheIsBoundedByWeight |
| F-04 Radiance-Licht | ja; auf 26.3 live in `simpletrims/light/TrimLight` (common `DynamicLightHandler` dort tot) | stehender `minecraft:light` gilt nur als eigen, wenn der Traeger ihn selbst dort gesetzt hat; beide Kopien | TrimsTests foreign_light_untouched |
| F-05 Rucksack | ja (Javadoc sagte "dropped") | unlesbare Eintraege als `Dynamic` in `BackpackContents.unreadable()`, beim Speichern wieder angehaengt; Container reicht sie durch; beide Kopien | StorageTests backpack_keeps_unreadable_entries |
| F-06 Statischer Zustand | ja | `onDisconnect`/`clearAll` in BlueprintBuilder, RotatorItem, BuildingCoreItem, Haken in Simplebuilding.java (Fabric) | BlueprintTests.buildJobsAreDroppedOnDisconnectAndStop |

Nicht angefasst: `Protection.PROVIDERS` (Provider werden pro Server explizit abgemeldet), NeoForge/Forge-Haken (Besitzer: nur 26.3 Fabric).
