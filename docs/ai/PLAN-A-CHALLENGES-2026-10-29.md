# Plan A: Simple Challenges S0-S2 (a-chal29, 29.10.2026)

Grundlage: `docs/ai/KONZEPT-A-WP24-2026-10-29.md` (abgenommen, ABN29b-6), Spezifikation `docs/ai/WUNSCH-SIMPLE-CHALLENGES-2026-10-29.md`.
Nur MC 26.3 Fabric. Rückfragen R1-R13: Empfehlung des Konzepts (R1 neue Sub-Mod, R2 nur Weltanlage + Admin-Befehl für Testwelten, R4 Items, R5 Zellgrößen 1/2/4/8/16/32/48).

## Ist-Stand
Kein Modul vorhanden. Vorlage für Aufbau: Simple Earth (Fabric-only, optionale gebündelte Sub-Mod, `modules.json` mit `parent`/`bundle`, SB-Schalter `enableSimpleEarth`, Client-Guide `/simpleearth guide`).

## Umfang (S0-S2)
- **S0** Gerüst `modules/simplechallenges` (Fabric-only, braucht `simplelib`, SB optional), Server-Config `simplechallenges-server.json`
  (`enabled`, `allowAdminCommands=false`, `defaultChallenge=none`, `exemptCreative`, `pityEnabled`) mit `/simplechallenges config`.
  **Harte Sperre:** `Gate` ist nur an, wenn der Welt-State (SavedData `simplechallenges:state`, nur Overworld) eine gewählte Challenge enthält.
  Ohne State wird nichts angelegt (kein `computeIfAbsent`), alle Mixins/Tick-Handler beginnen mit dem Gate und tun sonst nichts.
  Preset-JSON (`data/<ns>/simplechallenges/preset/<id>.json`, Weltordner-Datapack, `config/simplechallenges/presets/`), `PresetParser` validiert
  (Zellgröße-Schritte, Rechtecke, Chunk+Block nie zusammen, Preisobergrenze, bekannte Modi; nicht gebaute Modi = Warnung, nicht aktivierbar).
  Vier Unlock-Presets: Vanilla Unlock, Block by Block, Quick Steps, Trophy Land.
- **S1** `UnlockGrid` (reine Logik, dünn besetzte Menge freier Zellen je Dimension), Durchsetzung: Mixin `Entity.collide` (alle Entitäten, Server und Client,
  Teilbewegung bis zur Kante), Mixin `Level.setBlock` (Server: Blockänderungen in gesperrten Zellen abgebrochen), Tick-Rettung (Spieler jeden Tick, Mobs
  alle 20 Ticks: Teleports/Perle/Respawn landen in der nächsten freien Zelle), Sync an Clients (`UnlockSyncPayload`, voll bei Join, Delta bei Freischaltung),
  einfache Grenze als Partikellinie am Boden (Client, Befehl `/simplechallenges border`).
- **S2** `PriceEngine` (rein, deterministisch aus Seed+Dimension+Zelle; Stufen leicht/mittel/schwer; Säulenwert für Zellgröße 1), `LevelProbe`
  (Oberfläche geladener Chunks, sonst Generator-Höhe + Biom-Tabelle, kein Chunk wird erzeugt), Währungen Items/Credits/XP, Credits aus Erfolgen
  (Aufgabe 1 / Ziel 2 / Herausforderung 5, Abfrage alle 40 Ticks, Basislinie beim Start), `Solvability` (Lösbarkeitstest beim Aktivieren mit Ersatzpreis,
  Notfall-Gratisfreischaltung nach `pityMinutes`).
- Steuerung: `/simplechallenges status | presets | border | unlock [x z] | activate <preset> | deactivate | credits add <n>` (Admin nur mit `allowAdminCommands`).
  Dedizierter Server/neue Welt: `defaultChallenge` wirkt nur in einer Welt mit Spielzeit 0, die noch nie initialisiert wurde.

## Abweichungen vom Konzept (bewusst)
- Freie Zellen als sortierte Long-Menge im State (ein Eintrag je Zelle, 8 Byte) statt Bitset/RLE: einfacher, für <= 1 Mio. Zellen tragbar; RLE später.
- Credits/Preise: ein Team-Topf (kein Pro-Spieler-Topf). Geld über Simple Money, Fadenkreuz-Anzeige und Rechteck-Editor kommen mit S3.
- Spawn-Sperre für Mobs in gesperrten Zellen: nur Rettung/Entfernen aus gesperrten Zellen (kein NaturalSpawner-Hook) - offen.
- Rand-Lehnen (Sneak) nicht umgesetzt.

## Dateien
`modules/simplechallenges/**`, Registrierung wie Simple Earth (modules.json, enabled-mods.json, mc26_3/fabric/build.gradle, SB-Config-Schalter + Lang,
TcContext, fabric.mod.json suggests, simplemoney prices.json, mod_icons.py, module_guides.py, docs/MODS-UND-FEATURES.md).

## Verifikation (EIN sbt-Lauf am Ende)
Generatoren (guides, wiki, discover, icons) auf sb-test, Compile `:mc26_3:fabric:compileJava` + Modul, Tests `module-simplechallenges-fabric-263` +
`module-simplechallenges-standalone-fabric-263`, `./gradlew check -q`. Reine Logik zusätzlich lokal mit javac/Main-Tests (kein Gradle).
Nicht getestet: Client-Läufe (Partikelgrenze, Befehl `border`), Mehrspieler.

## S3 + S4 (a-chal29b, 30.10.2026)
Basis a-chal29 (S0-S2). Umsetzung:
- **S3 Welt-Erstellung:** Welt-Preset `simplechallenges:simple_challenges` (Kopie von `minecraft:normal`, im Tag `minecraft:worldgen/world_preset/normal`) erscheint im Weltentyp-Knopf neben Superflat. Mixin `WorldTabMixin` (Konstruktor der inneren Klasse `CreateWorldScreen$WorldTab`, Reihe 3) hängt `ChallengeRow` an: Zeile „gewählte Challenge", drei Knöpfe der zuletzt gespielten (`config/simplechallenges/recent.json`, Vorgabe Classic Skyblock / Sky One Block / Vanilla Unlock), „Select Challenge". Die Zeile hat Höhe 0 und ist unsichtbar bei jedem anderen Weltentyp (Vanilla-Welt-Erstellung bleibt gleich bis auf 8 px Zeilenabstand). `SelectChallengeScreen` (Vanilla-`ObjectSelectionList` mit weißem Auswahlrahmen, SB-`UiBoxes`-Panel): Liste, Bild (`PresetPreview`), eigene Presets bearbeiten (`PresetDraft` -> Datei -> normaler `PresetParser`), Duplizieren/Löschen, „Play Challenge" setzt `PendingChallenge`; `StartAreaEditorScreen` malt Rechtecke auf einer Zellkarte (`StartArea.cover`). Der Server nimmt die Wahl beim Start einer brandneuen Welt (Spielzeit 0, kein State) einmal (`PendingChallenge.take`), Verlassen der Maske (`popScreen`) oder anderer Weltentyp verwirft sie.
- **Fadenkreuz-Zeile:** `LookInfo` (Server, alle 10 Ticks, nur bei Änderung) -> `CellInfoPayload` -> `ChallengeHud` (HUD vor dem Chat).
- **Reste S0-S2:** Weltspawn (nur frische Welt) auf 0,0 ohne Streuung (`Spawn`, Gamerule `respawn_radius` 0), Mob-Spawn-Sperre (`ServerLevelSpawnMixin` auf `addFreshEntity`), Rand-Lehnen (Schleichen verkleinert die Kollisionsbox um 0,25), Credits pro Spieler (`perPlayerCredits`).
- **S4:** Weltmodi `skyblock` (Stile classic/mini/desert), `oneblock`, `skygrid` im Preset (`WorldMode`), kombinierbar mit `unlock`; `Active.mode` darf null sein (reiner Weltmodus ohne Grenzen). Leerwelt = Vanilla-Flat-Preset „The Void" mit Biom Ebenen, gesetzt vom Auswahlbildschirm; Insel/One Block werden beim Aktivieren gebaut (`SkyService.start`), One Block wächst per Tick nach, Sky Grid füllt neu erzeugte Chunks (`ServerChunkEvents.CHUNK_GENERATE`).
- Entscheidungen: Bildschirm-Aufruf über Weltentyp + Zeile statt „Anpassen"-Knopf (Spec); Seed-/Biom-Vorschau der Karte als Rest (braucht Welt-Sampler ohne Welt, der noch fehlt); Island Unlock mit Zelle 2; Preisliste der Insel aus `IslandPlan.reachable`.
- Tests: 4 neue Gametests (`sky_plans`, `preset_world_modes`, `sky_world_modes`, `lean_spawn_look`, je Katalog `ChallengeTests.ALL` + Adapter `ModuleGameTest`), Client-Smoke öffnet Welt-Erstellung, Select-Challenge und Editor.

## S5 - S8 (a-chal30, 30.10.2026)
Basis a-chal29b (S0-S4). Umsetzung (Pakete `lucky/`, `skyline/`, Presets, Tests `test/SkylineTests`):
- **S5 Lucky:** `LuckyMode` (Preset-Modi `lucky` und `randomdrops`, `alpha` 1.3, `pity` 6), `BlockCatalog` (alle Survival-Blöcke aus der Registry: Block hat ein eigenes Item gleichen Namens, nicht technisch/einmalig), `LuckyTable` (Gewicht = `commonness / (value+1)^alpha`, beides aus `block_value.json`; neuer Abschnitt `commonness` + mehr Seltenheitswerte), `LuckyService` an `PlayerBlockBreakEvents.BEFORE`: Block ohne Drops abbauen, gewürfelten Block als Item auswerfen (Tausch, nicht ersetzt). Vanilla bleibt bei Kreativ, Blöcken mit Inhalt, falschem Werkzeug. Random Drops = fester Partner je Seed und Block.
- **S6 Skyline:** `Layout` (linear / Raster / Ring, O(1) in beide Richtungen), `BlockCatalog.sequence` (kuratierter Anfang, Rest nach Seltenheitswert, Oberwelt endet mit Obsidian; Nether/End nach Namensregeln), `Support` (Stütze generisch: `canSurvive` in einer Proxy-Welt mit einem Grundgestein-Nachbarn, Fallende unten, Türen/Pflanzen zweite Hälfte, Bett Kopf), `SkylinePlans`, `SkylineService`: Blöcke beim `CHUNK_GENERATE` (nur die 8x8 Zellen des Chunks), Nachwachsen (Mixin RETURN auf `Level.setBlock` stellt in eine Warteschlange, Tick setzt sie am Tickende zurück, Flags 2|16), Ankunft (`ServerEntityLevelChangeEvents`): Spieler auf den ersten Block, End: Obsidian-Plattform weg. Leerwelt: Weltgenerierung setzt Oberwelt UND Nether auf Flat-Leer (`CreateChallenge.Terrain.SKYLINE`), End bleibt Vanilla (R6 A).
- **Siegel (R7 A):** letzter Platz der Nether-Linie = Grundgestein. Abweichung vom Konzept: kein eigenes Rezept/Item (Sicherheitssperre, Texturen), sondern Netherit-Spitzhacke in der Haupthand + Ender-Auge in der anderen Hand beim Schlagen (AttackBlockCallback); Auge wird verbraucht, Endportal 3x3 mit 12 Rahmen 4 Blöcke dahinter.
- **S7 Eggs (R8 A):** `Eggs`: Endschiff (Purpur, Elytra im Rahmen), Mini-Bastion (Truhe mit Netherit-Schmiedevorlage), Tresor (`vault` per `data merge block` auf `simplechallenges:vault/overworld` = Schwerer Kern) + Schlüssel-Rahmen; je Dimension Hauptplatz bei -X (96 Blöcke, End 190) und Backup bei gleicher Entfernung 30 Grad versetzt. Abweichung: Strukturen im Code gebaut (kein Template-Zugriff, testbar). Gebaut wird, wenn der Chunk des Ankers erzeugt wird (Warteschlange, ein Platz je Tick). Elytra/Schlüssel: alle 2 s nur für gebaute Plätze in geladenen Chunks aufgefüllt (4 Rahmen).
- **S8:** 16 Presets (Namen laut Konzept 9), `one_line` = Korridor-Startbereich (1 Chunk breit, 32 lang) mit Unlock; Wiki/Handbuch (`wiki/manual.json`, Guide-Generator), Standalone-Target war da, Client-Smoke prüft die Welt-Wahl, 7 neue Gametests (Katalog `ChallengeTests.ALL` + Adapter `ModuleGameTest`).
- **Offen/bewusst:** Dedizierter Server mit `defaultChallenge` Skyline braucht eine Leerwelt (Weltgenerator wird nur vom Erstellungsbildschirm gesetzt); R11 Audit der per Linie nicht erhältlichen Items (Blaze-Rute, Perlen) nicht umgesetzt; Nether-Portal-Bau erzeugt in der Leerwelt weiter ein Vanilla-Portal an der Zielstelle (Spieler wird danach auf den Linienanfang gesetzt); Seed-/Biom-Vorschau; Client-Sicht aller Bilder.

