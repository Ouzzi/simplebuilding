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

Noch nicht ausgeführt.
