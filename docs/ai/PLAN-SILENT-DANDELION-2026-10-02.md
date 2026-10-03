# Stiller Löwenzahn und Wollknäuel (2026-10-03)

## Korrektur nach Besitzerauftrag (2026-10-03)

Die folgenden historischen Aura-Entscheidungen sind verworfen. Rechtsklick auf einen lebenden Mob schaltet jetzt Vanilla `Silent` um, gespeichert und synchronisiert durch `Entity#setSilent`. Keine Bereichsprüfung, kein Speicher-Wrapper und kein Config-Abschnitt bleiben bestehen. Blume, Topf, Wollknäuel und Rezepte bleiben erhalten.

- Vanilla-Beleg: lokales 26.3-Jar, `javap -c -p AgeableMob` und `Mob`. `canUseGoldenDandelion` verlangt goldenes Item, Jungtier, Partikeltimer 0 und keinen `CANNOT_BE_AGE_LOCKED`-Tag. `setAgeLockedData` toggelt und setzt Timer 40. Der öffentliche Helper `setAgeLocked` führt den übergebenen Setter aus, verbraucht via `consume(1, player)`, setzt beim Einschalten Persistenz und spielt USE/UNUSE mit PLAYERS, Lautstärke/Tonhöhe 1. `makeAgeLockedParticle` liefert 40 Ticks lang alle zwei Ticks PAUSE_MOB_GROWTH/RESET_MOB_GROWTH. `mobInteract` liefert SUCCESS.
- Umsetzung: 26.3-Mob-Mixin mit inertem 26.2-Zwilling; Interaktion vor mob-eigenen Menüs über `checkAndHandleImportantInteractions`, wie Namensschild. Vanilla-Helper mit Silent-Setter wiederverwenden; eigener flüchtiger 40-Tick-Timer, serverseitige Änderung. SUCCESS auf beiden Seiten, ohne Client-Mutation. Keine Altersänderung.
- Bewusste Anpassung: Jungtier-/Wachstumstag-Prüfung entfällt, da ausdrücklich Mobs statt nur Jungtiere stummgeschaltet werden sollen. Erwachsene, Jungtiere und feindliche Mobs sind erlaubt. Spieler und Rüstungsständer sind keine Mobs; Wither/Enderdrache werden zum Erhalt ihrer Kampfrückmeldung ausgeschlossen. Tote Mobs bleiben durch Vanillas interact-Prüfung unberührt. Der Timer verhindert Mehrfachverbrauch wie bei golden.
- Texte: Wiki, Handbuch, JEI und EN/DE beider Ressourcenbäume; Testzentralen-Hinweis zur Benutzung. Tests ersetzen Aura-Fälle durch echte Player-Interaktionen, beide Toggle-Richtungen, Timer, Survival/Kreativ, Ausschlüsse, NBT-Roundtrip und Dekoration ohne Aura.
- Verifikation: Datagen; venv-Wiki --all, uv-Wiki --all --check; gefilterte und vollständige Fabric-/NeoForge-26.3-Suiten; 26.2-Fabric/NeoForge- und Forge-26.3-Compile; check, Bücher und Testzentralen-Abdeckung. Keine Clients, kein Push, nur Branch claude-gpt-dandelion.

### Ergebnisse der Korrektur

- Datagen: `BUILD SUCCESSFUL in 3m 44s` (`.ai-runs/dandelion-correction-datagen.log`). Rezepte, Wollknäuel, Blumen-/Topf-Daten und Texturen unverändert; keine neuen generierten Rezept-Diffs.
- Erster gefilterter Lauf: `NICHT gruen: 10/14 bestanden, 4 rot` (2026-10-03T10-31-41Z-5b06). Testaufbau korrigiert: Standardtimeout 20 war kürzer als Vanillas 40-Tick-Pause; nun 100 in Katalog und Fabric-Adapter. `makeMockPlayer(CREATIVE)` initialisiert die Abilities nicht, deshalb im Test wie beim echten Spielmodus `CREATIVE.updatePlayerAbilities` aufrufen. Kein Gameplay-Fix erforderlich.
- Gefilterter Abschlusslauf: **`alles gruen: 14/14 bestanden, 0 rot`**, je Loader 7/7 (2026-10-03T10-36-30Z-35ef, `.ai-runs/dandelion-correction-filtered-2.log`). Erwachsene Kuh, Jungtier, Zombie und Villager über `Player.interactOn`; SUCCESS, Verbrauch beider Toggle-Richtungen, Timer, Kreativ-Nebenhand, Spieler/Rüstungsständer/Wither/Enderdrache/tote Mobs, Silent-NBT-Roundtrip, Dekoration ohne Aura, Topf, Rezepte und Garn.
- Vollständige Fabric-/NeoForge-26.3-Suiten: **`alles gruen: 1758/1758 bestanden, 0 rot`**, je Loader 879/879 (2026-10-03T10-38-55Z-9232, `.ai-runs/dandelion-correction-full.log`). Je 7/7 Testzentralen-Fälle inklusive vollständigem Neubau, Stationsplan und Abdeckung aller Mod-Items/-Blöcke. Besitzerwelt unberührt.
- Vollständiges `gradlew.bat check -q :compileJava :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava -PwikiPython=<uv-Python>`: **`GRADLE_EXIT=0`** (`.ai-runs/dandelion-correction-gate.log`). 26.2 Fabric/NeoForge und Forge 26.3 kompiliert, keine Gate-Prüfung deaktiviert. Einschließlich Ressourcen-/Atlas-/Balance-/Modulprüfungen und 38 erfolgreichen Wiki-Tests.
- Wiki mit venv `wiki/generate.py --all`, danach uv-Python `--all --check`: **`wiki: up to date, everything documented.`**; im Gesamtgate erneut geprüft. Die zwei Aura-Optionen sind auch aus den generierten Config-Daten entfernt.
- Handbuch: bestehender Prüfer plus explizite Einbeziehung der beiden bedingt angehängten 26.3-Kapitel (`.ai-runs/check-dandelion-books.py`). Neue EN/DE-Seiten passen. Weiterhin nur die zwei bekannten deutschen Hub-Überläufe (`guide topics 1`: 14, `guide topics 3`: 15 Zeilen), `problems: 2`. Keine neuen Überläufe.
- Offen: echte Client-Sicht-/Audioabnahme einschließlich Armschwung, Partikeldarstellung, Tooltip/JEI und Handbuch. SUCCESS und Vanilla-Helper sind serverseitig belegt; kein Spielclient gestartet. Forge 26.3 und 26.2 nur kompiliert, keine Laufzeittests dort; kein Port nach 1.21.11/26.4. Bestehende Pixelkunst/Vorschau unverändert. Kein Push/Merge; `.serena/` nicht Bestandteil des Commits.

### Geänderte Dateien dieser Korrektur (23)

- `.claude/QUEUE.md`
- `common/src/mc26_2/java/com/simplebuilding/mixin/SilentDandelionMixin.java` (inert, neu)
- `common/src/shared/java/com/simplebuilding/compat/RecipelessJeiInfo.java`
- `common/src/shared/java/com/simplebuilding/config/ServerTuning.java`
- `common/src/shared/java/com/simplebuilding/config/ServerTuningConfig.java`
- `common/src/shared/java/com/simplebuilding/dev/testcentre/TestCentreSections.java`
- `common/src/shared/java/com/simplebuilding/gametest/ConfigOptionTests.java`
- `common/src/shared/java/com/simplebuilding/gametest/SilentDandelionTests.java`
- `common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java`
- `common/src/shared/java/com/simplebuilding/guide/GuideContent.java`
- `common/src/shared/java/com/simplebuilding/items/tooltip/InfoTooltips.java`
- `common/src/shared/java/com/simplebuilding/mixin/SilentDandelionMixin.java` (Aura-Mixin gelöscht)
- `common/src/shared/java/com/simplebuilding/util/SilentDandelions.java` (Bereichsprüfung gelöscht)
- `docs/ai/PLAN-SILENT-DANDELION-2026-10-02.md`
- `mc26_3/overlay/java/com/simplebuilding/mixin/SilentDandelionMixin.java` (Benutzungs-Mixin, neu)
- `mc26_3/overlay/resources/assets/simplebuilding/lang/de_de.json`
- `mc26_3/overlay/resources/assets/simplebuilding/lang/en_us.json`
- `src/main/java/com/simplebuilding/gametest/SilentDandelionGameTest.java`
- `src/main/resources/assets/simplebuilding/lang/de_de.json`
- `src/main/resources/assets/simplebuilding/lang/en_us.json`
- `wiki/data/simplebuilding.js`
- `wiki/data/simplebuilding.json`
- `wiki/manual.json`

## Historie vor der Korrektur

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
