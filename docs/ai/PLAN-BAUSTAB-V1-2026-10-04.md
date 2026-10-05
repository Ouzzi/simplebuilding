# Baustab V1: Flächenbau durch den Blaupausen-Planer

## Auftrag und Grenzen

Arbeitsbranch: `claude-gpt-wandv1`. Reihenfolge: Plan, Umsetzung, Test. Kleine lokale
Commits, kein Push und kein Merge. Nur 26.3 umstellen; 26.2 muss weiterhin kompilieren.
Keine neue Benutzeroberfläche, Meldung, Warnklick-Regel oder Änderung der Baugeschwindigkeit.
Vorhandene Tests bleiben unverändert, außer bei einer nachgewiesenen und bewusst behobenen Schutzlücke.

## Ist-Pfade und Belege

- `BuildingWandItem.useOn/use` speichert den laufenden Bau im Item. `Plan.step` liefert
  Ringe, Abdeckungsflächen, Linien- oder Brückenabschnitte. `inventoryTick` steuert
  Handwechsel, Materialwahl, Timer, Platzierung, Verbrauch, Haltbarkeit, Hunger, Statistik und Undo.
- Der direkte Bau prüft bereits `mayBuildAt` (Weltgrenzen, geladene Chunks,
  `mayInteract`, `mayUseItemAt`, `WorldPermissions.mayChange`), Ersetzbarkeit,
  `WandPlacement.stateFor`, Blacklist und `BuildPermissions.mayPlace` pro Position.
  D2 aus dem Fragebogen vom 24. September ist deshalb kein unverändert vorhandener Defekt.
- `BlueprintBuilder.Planner.run` besucht ein `Layout`, prüft Grenzen/Baurechte,
  Ersetzbarkeit und Schutzereignisse, berechnet Blaupausen-Kosten, ruft den `Placer`
  und zählt Ergebnisse. Blaupausen und `ShapeFill` verwenden diesen Pfad bereits.
- `BuildPermissions` ruft Spawn-/Weltgrenzenschutz, die Claims-Brücke und den
  loaderabhängigen `BuildGuard` auf. `WorldPermissions` berücksichtigt zusätzliche
  Bett-/Truhenpositionen. `WandPlacement` prüft beide Teile von Betten, Türen und
  Doppelpflanzen vor der Mutation. `WAND-FOLLOWUP-PLAN.md` dokumentiert die vorherige Reparatur.
- Direkte Itemplatzierung unterscheidet sich absichtlich von Blaupausen: ein originales
  Item mit Komponenten je erfolgreichem Callback, Live-Palette und Materialsuche pro
  Position, sofortiges Ende bei fehlendem Material. Blaupausen normalisieren Zustände,
  berechnen Mengen aus Blockzuständen und platzieren gespeicherte Gegenstücke separat.

Gelesene Grundlagen: `AGENTS.md`, externe `wave2-rules.md`, `HANDOFF.md`,
`BAUWERKZEUGE-INTERAKTIONEN.md`, `FRAGEBOGEN-BAUWERKZEUGE.md`,
`docs/ai/{WORKFLOW,LAPTOP-SETUP,VOICE-HANDS-FREE,CODEX-PLAN,WAND-FOLLOWUP-PLAN}.md`.
Historische Vorschläge sind keine Erlaubnis für zusätzliche Verhaltensänderungen.

## Zielarchitektur und kleine Schritte

1. Diesen Plan committen. Der Wunsch ist bereits in `.claude/QUEUE.md` eingetragen.
2. Im bestehenden Planer einen kleinen Layout-Typ für direkte Itemplatzierungen
   ergänzen. Dieselbe `run`-Schleife übernimmt Positions- und Schutzprüfungen;
   Itemzustände und Bezahlung bleiben Aufgabe des vorhandenen Baustab-Callbacks.
   Die echte Klickseite muss an `mayUseItemAt` gelangen, nicht pauschal `UP`.
3. Auf 26.3 jeden bisherigen Bauschritt durch diesen Planer schicken. Geometrie,
   Reihenfolge und NBT-Taktung bleiben bestehen. Die bereits gemeinsame direkte
   Schleife umfasst auch Abdeckung, Linie und Brücke; diese verwenden denselben Adapter.
   26.2 behält den bisherigen direkten Pfad hinter dem Versionsschalter.
4. Schutzregressionen ergänzen und unveränderte Bestandsfälle prüfen. Code und
   zusätzliche Tests als eigenen Commit liefern; Abschlussbelege separat ergänzen.

## Risiken und bewusste Entscheidungen

- Kein `startJob`: dessen Zwei-Klick-Regel, Fortschrittstexte, Tickbudgets und
  Persistenz würden das geforderte Verhalten verändern.
- Kein vorab materialisierter Zustandsplan: Palette, Nachbarformen, Inventar und
  Schutzentscheidungen müssen beim Besuch der jeweiligen Position aktuell sein.
- Kein `realSupply` für direkte Platzierung: dessen Rückgabe eines nackten Items
  könnte Komponenten verlieren. Verbrauch erst nach erfolgreichem Setzen beibehalten.
- `survivalState` und Blaupausen-Mengenkosten gelten nicht für direkte Items.
  Wasserzustände, Namen, Block-Entity-Daten und Multipart-Callbacks erhalten.
- Vorschau bleibt unverändert; kein Clientstart erlaubt. Serverprüfungen haben Vorrang.
- Historische Probleme wie Dimensionswechsel, manipulierte NBT und Weiterbau im
  Bruch-Ring nicht nebenbei ändern. Nur konkret belegte Schutzlücken gehören in diesen Run.
- Keine neuen Abhängigkeiten oder Änderungen an 1.21.11/26.4.

## Teststrategie

- Bestehende `BuildingWandTests`, `WandModeTests`, `WandEnchantmentTests`,
  `BuildingEnchantmentTests`, `BlueprintTests` sowie Material-/Verbrauchs-/Undo-Tests
  unverändert ausführen. Sie pinnen Geometrie, Taktung, Hände, Palette, Quellen,
  Haltbarkeit, Hunger, Orientierung und Multipart-Verhalten.
- Neue zielgerichtete GameTests: verweigerte einzelne Positionen mit erlaubten
  Nachbarn, echte Klickseite, Änderung der Rechte/Ersetzbarkeit zwischen Bauschritten,
  keine Kosten für verweigerte Positionen; beide Loader registrieren.
- Gefilterter Fabric-26.3-Lauf, danach zwingend
  `python tools/testrunner/run.py --targets "fabric-263,neoforge-263"`;
  Ergebniszeile `alles gruen` und aktuelle Datensätze lesen.
- `:compileJava :neoforge:compileJava` (26.2),
  `-Pforge263=true :mc26_3:forge:compileJava`, `gradlew.bat check -q`.
- Wiki generieren und prüfen; Bücher prüfen. Testzentralen-Neubau und vollständige
  Itemabdeckung durch die Server-Suite belegen. Keine Besitzerwelt verändern.
- Abschließend Diff, Plan-Abgleich und Commit-Trailer prüfen. Einschränkungen und
  tatsächlich beobachtete Verhaltensänderungen hier ergänzen.

## Ergebnisse

### Umsetzung und Plan-Abgleich

- Plan: `246cf357`; Umsetzung und drei Regressionstests: `66e68200`.
- `BlueprintBuilder.ItemLayout` beschreibt einen bisherigen Bauschritt mit der echten
  Klickseite. `Planner.run` übernimmt Besuchsreihenfolge, Grenzen, Baurechte,
  Ersetzbarkeit, Blacklist und Loader-Schutzprüfung. Der Adapter löst Material und
  `WandPlacement.stateFor` beim jeweiligen Besuch auf. Kosten und Callbacks bleiben
  in `placeWandBlock`, das auch der unveränderte 26.2-Algorithmus nutzt.
- Umschaltung ausschließlich über `McVersion.GADGET_REWORK`. Die verbleibende
  direkte Schleife ist der notwendige 26.2-Legacy-Pfad bis zum gesonderten Port-Run.
- Blueprint-Layouts behalten ihre bisherige Klassifikation `already/occupied`,
  Normalisierung, Kosten, Erstattung und Vorschau. Direkte Items erhalten weder
  Blueprint-Normalisierung noch Blueprint-Materialsuche.
- Kein Bestands-Testkörper verändert. Neue Tests prüfen den echten Loader-Schutz
  beim Wechsel zwischen Ringen, nachträglich belegte Zellen, erlaubte Nachbarn,
  exakte vier Pausenticks, echte Klickseite, entzogene Baurechte, Verbrauch,
  Wasserzustand und Name/Inhalt einer platzierten Shulkerkiste.
- Keine beabsichtigte sichtbare Verhaltensänderung und keine neu entdeckte,
  gezielt behobene Schutzlücke. Die bereits reparierte historische D2 wird hier
  vereinheitlicht, nicht erneut als neuer Fix beansprucht. D4 (Rest des Bruch-Rings)
  und D13 (Dimensionswechsel) bleiben ausdrücklich außerhalb dieser Änderung.

### Prüfbelege

- Gefilterter Fabric-26.3-Lauf `2026-10-04T15-24-49Z-0709`: **14/14 bestanden,
  0 rot**, einschließlich aller drei neuen Tests. Gradle: **`BUILD SUCCESSFUL in
  21m 2s`**. Der Runner meldet dennoch **`NICHT gruen: 14/14 bestanden, 0 rot`**:
  kalte Vorbereitung/Source-Remapping überschritten die 1200-s-Grenze. Dieser
  Lauf ist ausdrücklich kein grünes Gesamtgate. Die vollständige Suite wurde mit
  3600 s Zeitlimit wiederholt.
- Vollständiger Lauf `2026-10-04T15-47-30Z-4fa1`, Code-Commit `66e68200`:
  **`alles gruen: 1838/1838 bestanden, 0 rot`**. Fabric 26.3 und NeoForge 26.3
  jeweils **919/919**, Gradle-Exit 0. Beide aktuellen XML-Berichte bestätigen
  die drei neuen Regressionen ohne Fehler oder Skip. Alle bestehenden Tests
  liefen unverändert, einschließlich Blaupausen, Modi, Material und Schutzereignissen.
- Testzentrale: auf beiden Loadern alle sieben `test_centre_game_test_*`-Fälle
  ohne Fehler oder Skip, insbesondere `the_whole_centre_builds_and_matches_its_plan`
  und `every_mod_item_and_block_has_its_place_in_the_test_centre`.
  Das belegt Neubau und vollständige Item-/Blockabdeckung in den separaten Testwelten.
- `wiki/generate.py --all` und `--all --check`: Exit 0,
  **`wiki: up to date, everything documented.`** Keine inhaltlichen Änderungen
  an generierten Wiki-Dateien; nur Zeilenenden, keine Wiki-Daten committet.
- Buchprüfung zunächst mit ungeeignetem Python ohne Pillow fehlgeschlagen;
  Wiederholung mit dem vorhandenen Pillow-venv erfolgreich ausgeführt, aber
  **`problems: 2`**, Exit 1: `de_de guide topics 1` = 14 Zeilen,
  `de_de guide topics 3` = 15 Zeilen (Grenze 13). Die Eingaben
  `tools/guide_book_pages.py`, `GuideBooks.java`, `GuideContent.java` und beide
  Sprachverzeichnisse sind gegenüber dem Ausgangscommit unverändert. Diese
  vorbestehenden Buchlayouts werden nicht im Baustab-Refactoring geändert.

- Abschließendes Compile-/Check-Gate im eigenen Worktree: **`GATE_AND_COMPILE_EXIT=0`**.
  Ausgeführt mit Java 25, vorhandenem Java 8 für Forge und dem Pillow-venv auf `PATH`:

  ```powershell
  .\gradlew.bat --max-workers=2 :compileJava :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava check -q
  ```

  Damit sind 26.2 Fabric/NeoForge, 26.3 Forge und das vollständige `check` bestätigt.
  Gelesene Prüflogzeilen: `checkBalance: ok` (223 erzeugte Stellen, 0 Fehler,
  0 Hinweise) und `wiki: up to date, everything documented.`
  Compilerwarnungen zu veralteten APIs und `EnvType.CLIENT`, keine Buildfehler.
  Das projektweite Gate baut auch bestehende Module und die 1.21.11-Linie;
  deren Quelldateien wurden nicht geändert. Lokales Protokoll:
  `scratchpad/wand-gate.log`. Die separate Buchprüfung bleibt wie oben beschrieben rot.

### Grenzen der Verifikation

Keine Minecraft-Clients gestartet. Sicht-/Audioabnahme, Forge-GameTests,
echte Drittanbieter-Claim-Mods und `/sbtestcentre build` in der Besitzerwelt
bleiben außerhalb dieses Runs. Schutzereignisse wurden durch die echten
Loader-Hooks mit `ProtectionProbe` geprüft; die Besitzerwelt blieb unberührt.
