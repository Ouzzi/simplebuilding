# Nachtrag 4: Rezepte und Pad-Einblenden

## Plan (vor Umsetzung, 2026-10-03)

Arbeitsbranch `claude-gpt-recipes`, ausschließlich dieser Worktree. Kein Push,
kein Merge, kein Minecraft-Client. Regeln: wave2-rules.md und AGENTS.md.

### Ist-Zustand und Entscheidung

- Resonanzstab 26.3: ` RA` / `NCR` / `IN `; R Redstone, N Eisennugget,
  A Amethystscherbe, C Eisenkern, I Eisenstab. Unten rechts ist bereits leer.
  Annahme zu „jeweils einen unten rechts entfernen“: gemeint sind die beiden
  Felder rechts und unterhalb des Kerns, die dessen untere rechte Seite bilden.
  Nach dem Tausch ` NA` / `RCN` / `IR ` werden dort Nugget und Redstone entfernt:
  **` NA` / `RC ` / `I  `**, je ein Nugget und Redstone bleiben.
- Messuhr: ` NA` / `NCN` / `KN ` wird auf 26.3 um 90 Grad gegen den Uhrzeigersinn
  zu **`AN ` / `NCN` / ` NK`** gedreht. Mitte Uhr; 26.2 bleibt Kompass/alte Anordnung.
- Elytra-Pads, Flypads und Spawn-Teleporter wechseln boolesch `active` und damit
  sofort das Modell. Trank-Pads zeigen Bereitschaft mit `active` und ihre einzige
  echte Texturanimation mit `cooling`. Launchpads zeigen Ladestand, Chunkloader
  Besitzeraktivität, Druckplatten Druck: keine betretenabhängige Pad-Animation.
- Vanilla-Lösung: `fade=0..3`, Schritte alle 2 Ticks, bestehende Texturen mit
  1/3 und 2/3 des Effekts mischen. Drei zusätzliche scheduled ticks pro Wechsel,
  keine zusätzlichen Dauerticks/Spielersuchen/Renderer. Auch die Trank-Abkling-
  animation wird eingeblendet. Bestehende Gameplay-Bedingungen bleiben erhalten.
  Neue Property nur hinter McVersion.GADGET_REWORK (26.3).

### Dateien und Risiken

ModRecipeProvider, TweaksTests, EN/DE-Lang in beiden Ressourcenbäumen,
wiki/manual.json und wiki/tests/test_facts.py. PadBlock, die vier aktiven
Pad-Familien, Flypad-/PotionPadBlockEntity, TweaksModelGen, Texturgenerator,
PadOverhaulTests samt Fabric-Adapter/Katalog. Abgeleitete Daten per Datagen/Wiki.

Risiken: Vanilla spiegelt Rezepte horizontal; Negativtests dürfen keine erlaubte
Spiegelung ablehnen. Block-Entities müssen bei Modellwechsel erhalten bleiben.
Abbruch, erneutes Betreten und alte Zustände mit fade=0 dürfen keine Endlosschleife
oder steckengebliebene Animation erzeugen. Globale Sprite-Zeit läuft weiter;
Einblenden erfolgt unabhängig von deren aktuellem Frame. Vorschau lokal im
Worktree unter previews/, weil ausdrücklich nur hier gearbeitet werden darf.

### Verifikation

GameTests für Rezeptinhalt/fehlende Zutaten, Fade-Stufen, Abbruch/Neustart,
Block-Entity-Erhalt und Ende der scheduled ticks. Generator --check und Vorschau
(16-fach, alt/neue Stufen). Datagen + syncGenerated263; Fabric/NeoForge 26.3 volle
Serversuiten, Ergebniszeile lesen. Compile :compileJava :neoforge:compileJava und
-Pforge263=true :mc26_3:forge:compileJava. Wiki venv --all, uv --all --check,
unittest discover -s wiki/tests; vollständiges Gradle check. Keine Clienttests.
Testzentralen/Itemabdeckung durch Serversuite prüfen. Abschließend Plan-Abgleich,
Queue aktualisieren, ausschließlich auf aktuellem Branch committen.

## Umsetzungsbefunde

- Die gedrehte Messuhr entspricht auch der horizontalen Spiegelung der alten
  Anordnung. Vanilla akzeptiert deshalb beide; die angezeigte Datagen-Anordnung
  und eine exakte Datenprüfung sichern die gewünschte Drehung ab.
- Für die Messuhr bestand noch keine JEI-Infoseite. Die vorhandene Liste
  `RecipelessJeiInfo.supplementalPages` registriert sie jetzt auf 26.3.
- Zwischenstufen liegen ausschließlich im 26.3-Overlay. Der Generator
  `tools/textures/pad_fade.py` mischt vorhandene Pixel; animierte Trank-Texturen
  behalten Framezahl und Metadaten. Vorschau: `previews/pad-fade-vorschau.png`.
- RTK/rg sind in dieser Shell nicht verfügbar; native Git-Suche und gezielte
  Dateiauszüge genutzt. Serena explizit auf diesen Worktree aktiviert; seine
  Java-Symbolübersicht blieb leer. Keine neuen Abhängigkeiten installiert.

## Wiederaufnahme nach Netzausfall (2026-10-03)

Vorhandene Änderungen und gestagte Datagen-Dateien geprüft und weiterverwendet.
Die bisherigen Server-Logs waren rot (Fade-Test: Stufe 0 statt 1). Der Test
wartet nun vor der Aktivierung auf einen block-tickenden Chunk und prüft die
Aktivierung sowie den eingetragenen scheduled tick ausdrücklich. Verifikation
erfolgt erneut; alte Logs gelten nicht als Nachweis. Lokale Logs, `.serena/`
und Build-Verzeichnisse werden nicht committet.

Ursache des roten Tests: Die vier Pad-Klassen erzeugen jeweils ihre eigene
`ACTIVE`-Property. Die Prüfung ausschließlich gegen `ElytraPadBlock.ACTIVE`
erkannte andere Familien nicht. `visualMode` verwendet jetzt die Property der
konkreten Familie; der GameTest prüft den Fade-Start für jede aktuelle Pad-Stufe.
Beide Testadapter erlauben 60 statt der voreingestellten 20 Ticks, damit die
21-Tick-Sequenz einschließlich Abkling-Fade vollständig laufen kann.

Gezielter Nachweis: Run `2026-10-03T20-28-42Z-ef46`, Fabric/NeoForge 26.3:
`alles gruen: 2/2 bestanden, 0 rot`. Wiki: 39 Unit-Tests OK; venv-Generierung
`--all` und uv-Check `--all --check` erfolgreich. Texturen: 34 Fade-PNGs und
6 Animationsmetadaten aktuell; Standardgenerator: 508 Texturen und 9 `.mcmeta`
aktuell. Vorschau visuell geprüft. Buchprüfung meldet zwei bestehende überlange
deutsche Inhaltsseiten (`guide/topics 1` und `guide/topics 3`); deren Texte
wurden in diesem Run nicht verändert.

Vollständige Serversuiten: Run `2026-10-03T20-39-33Z-97b4`, je 888 Tests auf
Fabric und NeoForge 26.3, `alles gruen: 1776/1776 bestanden, 0 rot`. Enthalten
sind Rezeptprüfungen, der Fade-Test sowie Neubau und vollständige Item-/Block-
Abdeckung der Testzentrale in den separaten GameTest-Welten.
Datagen und `syncGenerated263` erneut erfolgreich (`DATAGEN_EXIT=0`),
50 abgeleitete Rezept-/Blockstate-/Modell-Dateien wie vorgesehen vorgemerkt.

## Abschlussprüfung

- `:compileJava :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava`:
  `BUILD SUCCESSFUL`, `COMPILE_EXIT=0` (26.2 Fabric/NeoForge und 26.3 Forge).
- Vollständiges `gradlew.bat check -q` mit der vorhandenen venv als `wikiPython`:
  `GRADLE_CHECK_EXIT=0`; 39 Wiki-Tests OK. Keine Gates übersprungen.
- Nach Datagen erneut venv `wiki/generate.py --all`, uv `--all --check`:
  `wiki: up to date, everything documented.`; uv-Unittests: 39 Tests OK.
- Alle vier geänderten EN/DE-Lang-Dateien ohne doppelte Schlüssel.
- JUnit-Berichte: je acht Testzentralen-Tests grün auf Fabric und NeoForge,
  einschließlich Item-/Blockabdeckung und echtem Neubau.
- `git diff --check` sauber. Logs, `.serena/` und Build-Artefakte nicht im Commit.

Offen: visuelle Abnahme im Spiel (kein Client gestartet), die zwei bereits
bestehenden Buchlayout-Probleme und Besitzerbestätigung der dokumentierten
Rezept-Annahme. Kein Push, Merge oder Port-Run.
