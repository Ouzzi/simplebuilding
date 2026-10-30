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
