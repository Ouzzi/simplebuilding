# Plan A-VANILLA36 (12.11.2026) – SimpleLib registriert Inhalte nur auf Anforderung (VANILLA27 = A)

Branch `a-vanilla36` (Basis hub/claude-wave1 240d278d5). Konzept: `docs/ai/KONZEPT-B-VANILLACLIENT-2026-11-11.md` (Variante B).
Besitzer 12.11.: „SimpleLib vervollständigt sich je nach installierten Mods (registriert nur Angefordertes)“. Nur 26.3 Fabric.

## Ist-Stand (erhoben)
- `SimpleLibFabric.onInitialize` registriert immer: `LibComponents` (warm, rarity), `LibBlocks`/`LibItems` (Warzenwarze,
  Modpack-Guide), `LibMaterials` (alle Gruppen), `LibPotions` (warped), `LibSounds` (legendary), Tab „Simple Lib“,
  Kreativ-Einträge, Bastion-Loot Warzenwarze. Fabric Registry Sync weist darum jeden Vanilla-Client ab (b-server26).
- Daten von SimpleLib, die Inhalte nennen: Tags in `minecraft:`/`c:` (mineable/pickaxe, stairs, slabs, walls, needs_iron_tool,
  c:gems/nuggets/storage_blocks), Block-Loot (7), Warzenwarzen-Rezepte + Brau-Rezepte, Guide-Rezept + Advancements.
  Material-Rezepte hängen schon an `simplelib:material_requested`.
- Fabric 8.0.1: Client nimmt beim Sync (RemapMode REMOTE) zusätzliche lokale Einträge hin; nur fehlende sind Fehler.
  Ein Dedicated Server schreibt fehlende Einträge nur in `fabric/registry_entries` (kein Abbruch), der Client zeigt beim
  Öffnen einer Einzelspielerwelt den Warn-Bildschirm. Fabric-Ressourcenbedingungen gelten nicht für Tags.

## Entscheidung (Stufe 1, klein und sicher)
1. **Wer fordert an:** Jeder geladene Mod außer SimpleLib mit `custom."simplelib:content"` – nicht leere Liste = fordert an,
   leere Liste = braucht nichts. **Ohne Erklärung** gilt ein Mod als Anforderer, wenn er SimpleLib als Abhängigkeit nennt
   oder seine Id mit `simple` beginnt (alle Inhalts-Mods bleiben ohne Änderung wie heute; Fehlgriffe kosten nur
   Vanilla-Tauglichkeit, nie Inhalte).
2. **Ganz oder gar nicht:** Fordert irgendwer an, registriert SimpleLib alles wie heute. Feinere Gruppen = Stufe 2.
3. **Physischer Client registriert immer** (Einzelspieler/LAN unverändert, Client kann jedem Server beitreten).
   `auto` wirkt nur auf dem Dedicated Server.
4. **Altwelten-Schalter:** `alwaysRegisterContent` (simplelib-server.json, Standard `false`, Neustart nötig).
   Abweichung vom Konzept: Boolean statt `auto|always|never` (`never` = YAGNI).
5. **Ohne Inhalte:** IdMoves auf Vanilla (Warzenwarze → Netherwarze Block+Item, Trank `warped` → `awkward`,
   Guide → Buch), damit Altwelten nichts mit Fehlern verlieren; Logzeile mit Hinweis auf den Schalter.
6. **Laufzeit-Schutz:** `Warm`/`WarmMerge`/`SimpleLibApi.warmUntil` tun nichts ohne `WARM`; `LibMaterials.requested`
   ist ohne Inhalte falsch (Amboss-Zerkleinern, Wither-Drop, Material-Rezepte still); Tab/Kreativ-Einträge/Bastion-Loot/
   FTB-Kapitel nur mit Inhalten.
7. **Daten:** Tag-Einträge `{"id", "required": false}`; Loot/Rezepte/Advancements mit `fabric:registry_contains`
   (NeoForge/Forge ignorieren den Fabric-Schlüssel und registrieren weiter immer).
8. **Erklärung `[]`** in den 14 Logik-Mods des Konzepts (challenges, earth, farming, interfaces, livemap, loot, movement,
   optimization, optmemory, optrender, optserver, sounds, visuals, weather) – geprüft: keine SimpleLib-Inhalte in Code/Daten.
9. `tools/testrunner/server_solo.py`: `VANILLA_JOIN_REQUIRED` = diese 14 + simplelib.

## Dateien
`modules/simplelib/shared/java/com/simplelib/registry/LibContent.java` (neu, Regel + Status + Fallbacks),
`SimpleLibFabric.java`, `LibMaterials.java`, `Warm.java`, `WarmMerge.java`, `SimpleLibApi.java`, `MaterialLoot.java`,
`LibConfig.java`, `SimpleLib.java` (restart-Hinweis), `tools/gen_resources.py` + generierte Daten, Lang EN/DE,
Wiki `modules/simplelib/wiki/manual.json`, 14× `fabric.mod.json`, `server_solo.py`, Test `ContentRequestTests`.

## Risiken
- Vergessene Anforderung → Inhalt fehlt: durch Regel 1 (unerklärt = Anforderer) ausgeschlossen.
- Daten-Fehler im Log ohne Inhalte (server_solo prüft ERROR-Zeilen) → alles Inhaltliche bedingt/optional.
- Altwelten auf Dedicated Servern nur mit Logik-Mods → IdMoves + Schalter + Wiki/Release-Hinweis.

## Prüfung
Lokal: precheck --fast, check_data.py aller Module, gen_resources --check, wiki --all --check, check_missing_textures,
layers check, check_dup_impl, check_tooltip_text. sb-test (ein Lauf): `module-simplelib-fabric-263`
(Integration, Inhalte an), Standalone-Ziele betroffener Mods (Logik-Mod = Inhalte aus; Inhalts-Mod mit SimpleLib),
`server-solo-*` für simplelib + 14 Logik-Mods (vanilla_join muss passed sein) + ein Inhalts-Mod (known).

## Stufe 2 (offen, in uebergabe-A.md)
Gruppen je Mod deklarieren (Inhalts-Mods), B+ (Guide-Bücher simplemodels/-resonance/-respawn als Vanilla-Buch),
Texte mit englischem Fallback für Vanilla-Spieler, NeoForge/Forge beim Port.
