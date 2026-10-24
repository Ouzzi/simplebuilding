# Review 2, Gruppe A (24.10.2026, Branch `b-rev2a24`)

Auftrag Besitzer: Sub-Mods fertig machen, jede Datei einzeln auf Vollständigkeit und Probleme prüfen, dazu
Verbesserungsvorschläge. Nur MC 26.3 + Fabric (NeoForge-/Forge-Quellsätze nicht geprüft). Plan:
`docs/ai/PLAN-B-REV2A-2026-10-24.md`. Vorhandenes nicht doppelt: REV24 (`REVIEW-CODE-2026-10-24.md`, für Gruppe A
H-2, H-4, M-4, M-11, N-2, N-5, N-7, N-8 laut Queue behoben), Modul-Audit `AUDIT-MODULE-2026-10-24.md`
(Branch b-audmods24), JEI (b-jei24), Config-Befehle (CMD24).

Methode je Modul: (1) Werkzeug-Durchlauf über jede Datei (JSON parsebar, TODO/Platzhalter, `@Redirect`/`@Overwrite`,
breite/stumme `catch`, Client-Importe außerhalb des Client-Teils, statische Sammlungen ohne Aufräumen, Importe über
Modulgrenzen, Threads, Netz-Empfänger, `getBlockState` ohne Chunk-Prüfung), dazu `tools/audit_data.py` (Lang, Rezepte,
Loot, Tags, Advancements) und neu `tools/check_lang_keys.py`; (2) Lesen der Java-Dateien (Server-/Logikpfad vollständig,
Client auf Seiten-Trennung), Mixin-Configs gegen Klassen, `fabric.mod.json` gegen Entrypoints/Abhängigkeiten, Daten
gegen Registry-Ids. Generierte Schablonen-Dateien (`*Guide.java`, `*Discover.java`, `DiscoverTipMixin`) wurden einmal an
der Vorlage (`tools/guides/ModuleGuide.java.in`, `tools/discover/*.in`) und per Diff gegen die Vorlage geprüft.

Schwere: P1 Absturz/Datenverlust/Exploit, P2 Fehlverhalten, P3 Qualität, P4 Vorschlag.
Dateizahlen: Fabric + shared + clienttest + tools + wiki (ohne `build/`, `neoforge/`, `forge/`, `generated/`).

## simpleaesthetics (15 Dateien, 5 Java)
Super-Mod ohne Inhalt: Config (`ConfigFiles.readOrQuarantine` + `writeAtomic`), `SubMods`-Schalter, 2 GameTests.
- Kein Fund. P4: Config-Labels ohne Tooltip (liegt bei b-cmdcfg24, AUDM24-5). Test `configFile` lässt sein Temp-Verzeichnis liegen (harmlos).

## simplebeacons (87 Dateien, 34 Java, 39 JSON)
Geprüft: Registry, Config (Klammern aller Zahlen), Strong Beacon (BE, Logik, Menü mit Server-Validierung, Range-Faktor),
Strong Conduit, In-World-Upgrade (Rechte `mayBuild`/`mayInteract`, Stern je Schlag), 7 Mixins + 4 Client-Mixins (alle im
richtigen Abschnitt), Bildschirm, Rezepte (Load-Conditions für `simplemobs:nether_heart`), Loot, Lang EN=DE.
- **P3 behoben** `mixin/BeaconRangeMixin.java`: HEAD schrieb das statische Prozent auch vom Client-Thread (der Client ruft
  `applyEffects` auch auf). Im Einzelspieler konnte das zwischen HEAD und `inflate` des Server-Threads die Reichweite auf 100 %
  zurücksetzen. Jetzt schreibt nur die Server-Seite.
- **P3 behoben** `upgrade/StrongUpgrades.finish`: fehlte beim letzten Schlag der Netherstern (vorher weggelegt), brach es still ab;
  jetzt derselbe Bruch-Sound wie beim ersten Schlag ohne Stern.
- P4 Reichweite bis (50 + 20 × 6) × 2 = 340 Blöcke per Config: `getEntitiesOfClass` über eine sehr große Box alle 80 Ticks; Config-Hinweis
  oder Obergrenze 2× Vanilla-Box. (REV2-A-1)

## Daten- und Text-Prüfungen (alle 14 Module)
- `tools/check_missing_textures.py`: 0 Fehler. `check_empty_tags.py`, `check_loot_format.py`, `check_load_conditions.py`: grün.
- Eigene Ids in Rezepten/Loot/Tags der 14 Module gegen Item-/Blockstate-/Tag-Dateien: 0 echte Lücken (16 Treffer waren Fluide,
  Biome, Tränke, Handels-Ids – geprüft, vorhanden).
- `tools/audit_data.py`: 2× britisches Englisch in Guide-Seiten (`simpleblocks.guide.page.7` „colours“, `simpleend.guide.page.6`) –
  Guide-Texte werden aus `wiki/manual.json` erzeugt (REV2-A-2). Rest der Meldungen (unerreichbare Eimer-Zustände, Endblöcke aus der
  Weltgenerierung, Wandschilder ohne eigene Loot-Tabelle) sind gewollt (Zustände/`dropsLike`).
- **Neu** `tools/check_lang_keys.py` (in `checkModuleData`): jeder wörtliche Lang-Schlüssel im eigenen Namensraum in Modul-Java
  braucht EN und DE. Fand den P2 bei simplemaps (unten); sonst 0 Lücken in allen 44 Modulen.

## simpleblocks (726 Dateien, 32 Java, 671 JSON)
Geprüft: Registry (Eigenschaften von Vanilla kopiert, gealterte Ziegel), IdMoves (alte `simplebuilding:`-Ids, Säge-Umbenennung),
Natur-Blöcke (schwebender/levitierender Sand mit eigener Entity, Gras-/Erd-Stufen mit Ausbreitung, fallende Stufen, rissiges Eis +
2 Mixins), Shapecutter (Menü wie Vanilla-Steinsäge, Rezeptliste Server→Client, Index-Gleichheit), Loot-Injektion, Kreativ-Tabs.
- **P3 behoben** `saw/SawUpgrade.tryUpgrade`: Steinsäge → Shapecutter ging auch im Abenteuermodus/als Zuschauer (Block tauschen ohne
  `mayBuild`); jetzt verweigert, GameTest-Fall im Pfad `saw_upgrade_axe/hammer` ergänzt. (Spawn-Schutz prüft Vanilla schon vor dem
  Use-Event.)
- P4 `saw/Sawing`: `clientEntries` überlebt einen Serverwechsel, `cachedManager` hält den alten RecipeManager bis zur nächsten Welt;
  bei Disconnect leeren. Paketgröße der Liste wächst mit jedem Steinsägen-Rezept (heute weit unter 1 MiB). (REV2-A-3)
