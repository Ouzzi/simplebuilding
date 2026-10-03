# Stiller Löwenzahn und Wollknäuel (2026-10-03)

## Plan und belegter Ist-Zustand

- Nur dieser Worktree, Branch `claude-gpt-dandelion`; committen, niemals pushen oder master ändern. Keine Minecraft-Clients.
- Die vorhandene goldene Variante ist `minecraft:golden_dandelion`, kein eigener Mod-Block. Vanilla registriert einen normalen `FlowerBlock` und `potted_golden_dandelion`; das Handbuch beschreibt Benutzung am Jungtier. Es gibt im Mod-Code keinen Wachstumsradius und keine zugehörige Config. Daher ist ein identischer Umgebungsmechanismus nicht übernehmbar.
- Gegenprobe am lokalen offiziellen 26.3-Client-Jar mit `javap`: `AgeableMob.mobInteract` prüft `canUseGoldenDandelion` und schaltet `setAgeLocked` am angeklickten Tier um; `AgeLocked` wird gespeichert. `CreativeModeTabs` sortiert die goldene Blume zwischen Rote Bete und getrockneten Seetang. Beides bestätigt die Abweichung von der ursprünglichen Aufgabenannahme.
- Entscheidung: gewünschte Bereichswirkung für den stillen Löwenzahn, kugelförmig ab Blockmitte, Standardradius 8, abschaltbar, harte Obergrenze 16. Eigener Abschnitt `server.silentDandelion` im vorhandenen ServerTuning-System, einschließlich Config-Synchronisation. Blume/Topf nach Vanilla, Rezept nach dem goldenen Vorbild.
- `Entity.isSilent()` ergänzt bei Mobs die aktuelle Bereichsprüfung. `setSilent`/Entity-Daten bleiben unangetastet. Beim Speichern wird nur die eigene Bereichsprüfung für die beiden Vanilla-Abfragen ausgesetzt (`WrapOperation`, `try/finally`), da Vanilla sonst den berechneten Wert persistiert. Die ursprüngliche Abfrage bleibt erhalten, einschließlich anderer Mods und ausdrücklich stummer Mobs. Bereichsprüfung nur in geladenen Chunk-Abschnitten mit passender Blockpalette, ohne Chunk-Loads. Beide logischen Seiten verwenden die Server-Config.
- Wollknäuel nutzt `placed_small_parts`: dieselbe Item-Textur im Inventar und abgelegt, bis vier Teile, vorhandene Drops und Platzierungsoptionen. Kein zusätzlicher Block/Renderer nötig. Vier Fäden in Rautenform ergeben ein Knäuel; eine Wolle beliebiger Farbe ergibt zwei. Die Raute vermeidet einen Rezeptkonflikt mit Vanillas 2×2-Wolle. Wolle ist die effizientere Quelle, ohne Rückrezept oder Materialduplikation.

## Dateien und Umsetzung

- `McVersion`-Zwillinge: Flag nur 26.3; ModBlocks/ModItems, ServerTuning(Config), Mixin/Registrierung, Bereichshelper.
- Datagen: drei Rezepte, Blumen-/Topf-Loot, Item- und Block-Tags; Modelle/Texturen im 26.3-Overlay. EN/DE in beiden Lang-Verzeichnissen. Mod-Tab neben goldener Variante. Vanilla registriert die goldene Variante im Nahrungstab: dort und dadurch im Suchtab folgt der stille Löwenzahn direkt danach. Garn folgt Faden im Zutatentab. Die Testzentrale zeigt Blume, Topf und abgelegtes Garn im Materialbereich.
- `tools/textures/silent_dandelion_2026_10_03.py`: deterministische Pillow-Pixelkunst, Vanilla-Referenzen, 16-fache Vorschau A–E. Ausgabe `C:/Users/o_o/code/minecraft-mods/previews/stiller-loewenzahn-vorschau.png` (explizit beauftragte Ausnahme außerhalb des Worktrees).
- GameTests mit sortiertem Katalog und Fabric-Adapter: Radius/Verlassen/Entfernen/Topf, unveränderte Speicherung und bestehende Stille, Config-Cap/Abschalten, Rezepte und Knäuel-Platzierung/Drops. Datenintegrität berücksichtigt Topf ohne eigenes Item. Wiki nur mit belegten Aussagen; Queue aktualisieren.

## Risiken und Verifikation

- Vanilla-Soundpfade anhand lokaler 26.3-Quellen prüfen: Ambient, Hurt, Death und Schritte folgen `isSilent`; keine globale Unterdrückung fremder Block-/Spielergeräusche.
- Datagen `:mc26_3:fabric:runDatagen :mc26_3:fabric:syncGenerated263`.
- Gefilterte neue Tests, dann vollständige Server-Suiten `fabric-263,neoforge-263`; tatsächliche Zeile `alles gruen`/`NICHT gruen` lesen.
- Compile 26.2 `:compileJava :neoforge:compileJava`, Forge 26.3 `-Pforge263=true :mc26_3:forge:compileJava`; vollständiges `check -q` in diesem Worktree.
- Wiki mit venv `--all`, uv-Python `--all --check`; Textur-/Buchprüfung und Testzentrale-Abdeckung. Vorschau visuell prüfen. Client-/Audioabnahme und Besitzerwelt bleiben offen.

## Ergebnisse

- Datagen: `BUILD SUCCESSFUL in 3m 38s`. Zwei frühere Compile-Versuche scheiterten an 26.3-API-Namen; korrigiert.
- Neue Tests auf Fabric/NeoForge: `alles gruen: 12/12 bestanden, 0 rot` (2026-10-03T07-43-10Z-ec96).
- Erster Gesamtlauf: `NICHT gruen: 1744/1756 bestanden, 12 rot` (2026-10-03T07-50-27Z-473f). Je Loader sechs Integrationsbefunde: Topf in Testzentrale fehlte, Materialtab-Erwartung veraltet, Item-Lang-Key fehlte, goldene Variante im falschen Vanilla-Tab gesucht, zwei Config-Schemavergleiche veraltet. Alle korrigiert.
- Zweiter Gesamtlauf: `NICHT gruen: 1752/1756 bestanden, 4 rot` (2026-10-03T07-59-45Z-fa97). Nur noch der jetzt zusätzlich im Mod-Tab gezeigte goldene Löwenzahn fehlte in der Vanilla-Vergleichsliste; außerdem verlangte die Config-Screen-Prüfung einen Tooltip am neuen Schalter. Erwartung, Annotation und EN/DE ergänzt.
- Dritter Gesamtlauf: `NICHT gruen: 1755/1756 bestanden, 1 rot` (2026-10-03T08-04-39Z-3eae): NeoForge 878/878, Fabric 877/878. Sämtliche neuen Funktionen, Config-/Tab-Erwartungen und Testzentralen-Prüfungen bestanden. Einzige Abweichung: bestehender `rareShulkerCallsFourEndermitesOnceWhenPlayersComeNear` auf Fabric erzeugte drei statt vier Endermiten. `RareShulkers.spawnEscort` würfelt zwölfmal je Endermite eine sichere Position; der Test erwartet trotzdem stets vier. In den beiden vorherigen Gesamtläufen bestanden beide Loader diesen Fall. Keine Änderungen an Shulker-Code oder -Tests.
- **Unveränderter Abschlusslauf: `alles gruen: 1756/1756 bestanden, 0 rot`**, Fabric 878/878, NeoForge 878/878 (2026-10-03T08-14-33Z-f8ab, `.ai-runs/dandelion-full-4.log`). Einschließlich aller sechs neuen Fälle je Loader, Shulker-Gegenprobe, Testzentralen-Neubau und vollständiger Item-/Blockabdeckung (sieben Testzentralen-Fälle je Loader). Die Besitzerwelt wurde nicht geöffnet.
- 26.2 Fabric/NeoForge und Forge 26.3: zunächst `BUILD SUCCESSFUL in 1m 51s`; im finalen Gesamtgate nach allen Quelländerungen erneut erfolgreich kompiliert.
- Finales Gesamtgate: `gradlew.bat check -q :compileJava :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava -PwikiPython=<uv-Python>` mit **`GRADLE_EXIT=0`**. Enthält Ressourcen-/Atlas-/Balance-/Modul-/Wiki-Prüfungen und 38 erfolgreiche Wiki-Tests; Protokoll `.ai-runs/dandelion-gate.log`. Keine Gate-Prüfung deaktiviert.
- Wiki zuletzt mit venv `--all` erzeugt und im Gesamtgate mit uv-Python `--all --check` geprüft: `wiki: up to date, everything documented.`
- Texturen: `Silent Dandelion textures/models: OK`; Bestand: `OK: 508 Texturen und 9 .mcmeta in 2 Baeumen aktuell`. 16-fache Vorschau visuell geprüft, Blume und Garn mit transparentem Rand.
- Handbuchprüfung: `problems: 2`, bestehende deutsche Hub-Seiten `guide topics 1` (14 Zeilen) und `guide topics 3` (15 Zeilen). Alle bisherigen `book.*`-Werte stimmen mit HEAD überein. Kein Zusammenhang mit neuen Gegenständen; keine fremden Buchtexte geändert.
- Soundbeleg aus lokalen 26.3-Quellen: `LivingEntity.playHurtSound`/Tod → `makeSound` → `Entity.playSound` → `isSilent`; Schritte und Ambient führen ebenfalls über die Vanilla-Soundabfrage. Die Mod setzt weder `DATA_SILENT` noch NBT und unterdrückt keine fremden Welt-/Spielergeräusche. Eine echte Audio-/Sichtabnahme bleibt ohne Client offen.

## Offen und Grenzen

- Keine Minecraft-Clients gestartet: Sicht-/Audioabnahme, JEI/REI-Darstellung und Besitzerwelt bleiben ungeprüft.
- Forge 26.3 sowie 26.2 Fabric/NeoForge nur kompiliert; keine zusätzlichen Laufzeittests oder Ports auf 1.21.11/26.4. Die dortigen Quellen wurden nicht geändert.
- Zwei vorhandene Handbuchüberläufe und der oben dokumentierte zufallsabhängige Shulker-Test bleiben als separate Bestandsbefunde bestehen.
- Vorschau: `C:/Users/o_o/code/minecraft-mods/previews/stiller-loewenzahn-vorschau.png` (A Vanilla-Löwenzahn, B goldener Löwenzahn, C stiller Löwenzahn, D Faden, E Wollknäuel/abgelegt).
- Nur Commit auf dem aktuellen Branch, kein Push/Merge. Das vorbestehende ungetrackte `.serena/` bleibt unberührt.

## Geänderte Dateien

- .claude/QUEUE.md
- common/src/mc26_2/java/com/simplebuilding/version/McVersion.java
- common/src/shared/java/com/simplebuilding/blocks/ModBlocks.java
- common/src/shared/java/com/simplebuilding/config/ServerTuning.java
- common/src/shared/java/com/simplebuilding/config/ServerTuningConfig.java
- common/src/shared/java/com/simplebuilding/dev/testcentre/TestCentreSections.java
- common/src/shared/java/com/simplebuilding/gametest/ConfigOptionTests.java
- common/src/shared/java/com/simplebuilding/gametest/DataIntegrityTests.java
- common/src/shared/java/com/simplebuilding/gametest/SilentDandelionTests.java
- common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java
- common/src/shared/java/com/simplebuilding/items/ModItemGroupsContent.java
- common/src/shared/java/com/simplebuilding/items/ModItems.java
- common/src/shared/java/com/simplebuilding/items/SearchTabPlacement.java
- common/src/shared/java/com/simplebuilding/mixin/SilentDandelionMixin.java
- common/src/shared/java/com/simplebuilding/util/SilentDandelions.java
- docs/ai/PLAN-SILENT-DANDELION-2026-10-02.md
- mc26_3/generated/data/minecraft/tags/block/flower_pots.json
- mc26_3/generated/data/minecraft/tags/block/small_flowers.json
- mc26_3/generated/data/minecraft/tags/item/small_flowers.json
- mc26_3/generated/data/simplebuilding/advancement/recipes/decorations/silent_dandelion.json
- mc26_3/generated/data/simplebuilding/advancement/recipes/misc/yarn_ball_from_string.json
- mc26_3/generated/data/simplebuilding/advancement/recipes/misc/yarn_ball_from_wool.json
- mc26_3/generated/data/simplebuilding/loot_table/blocks/potted_silent_dandelion.json
- mc26_3/generated/data/simplebuilding/loot_table/blocks/silent_dandelion.json
- mc26_3/generated/data/simplebuilding/recipe/silent_dandelion.json
- mc26_3/generated/data/simplebuilding/recipe/yarn_ball_from_string.json
- mc26_3/generated/data/simplebuilding/recipe/yarn_ball_from_wool.json
- mc26_3/generated/data/simplebuilding/tags/item/placeable_small.json
- mc26_3/generated/wiki/items.json
- mc26_3/overlay/java/com/simplebuilding/version/McVersion.java
- mc26_3/overlay/resources/assets/simplebuilding/blockstates/potted_silent_dandelion.json
- mc26_3/overlay/resources/assets/simplebuilding/blockstates/silent_dandelion.json
- mc26_3/overlay/resources/assets/simplebuilding/items/silent_dandelion.json
- mc26_3/overlay/resources/assets/simplebuilding/items/yarn_ball.json
- mc26_3/overlay/resources/assets/simplebuilding/lang/de_de.json
- mc26_3/overlay/resources/assets/simplebuilding/lang/en_us.json
- mc26_3/overlay/resources/assets/simplebuilding/models/block/potted_silent_dandelion.json
- mc26_3/overlay/resources/assets/simplebuilding/models/block/silent_dandelion.json
- mc26_3/overlay/resources/assets/simplebuilding/models/item/silent_dandelion.json
- mc26_3/overlay/resources/assets/simplebuilding/models/item/yarn_ball.json
- mc26_3/overlay/resources/assets/simplebuilding/textures/block/silent_dandelion.png
- mc26_3/overlay/resources/assets/simplebuilding/textures/item/yarn_ball.png
- src/main/java/com/simplebuilding/datagen/ModBlockTagProvider.java
- src/main/java/com/simplebuilding/datagen/ModItemTagProvider.java
- src/main/java/com/simplebuilding/datagen/ModLootTableProvider.java
- src/main/java/com/simplebuilding/datagen/ModRecipeProvider.java
- src/main/java/com/simplebuilding/gametest/SilentDandelionGameTest.java
- src/main/resources/assets/simplebuilding/lang/de_de.json
- src/main/resources/assets/simplebuilding/lang/en_us.json
- src/main/resources/fabric.mod.json
- src/main/resources/simplebuilding.mixins.json
- tools/textures/silent_dandelion_2026_10_03.py
- wiki/assets/textures/block/silent_dandelion.png
- wiki/assets/textures/item/yarn_ball.png
- wiki/assets/textures/render/block/potted_silent_dandelion.png
- wiki/assets/textures/render/block/silent_dandelion.png
- wiki/data/simplebuilding.js
- wiki/data/simplebuilding.json
- wiki/manual.json
