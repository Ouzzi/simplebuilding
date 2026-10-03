# Plan Hängematte (Besitzer-Queue Nachtrag 2, 2026-10-02)

Wunsch: tagsüber in der Hängematte liegen lässt die Tageszeit schneller laufen (Gegenstück zum Bett). Nur 1×2×2
zwischen zwei festen Blöcken mit 2–3 Blöcken Abstand. Flag `McVersion.HAMMOCK` (26.3 true, 26.2 false).

## Ist-Zustand (Recherche)
- 26.3 hat `AbstractBedBlock` (Vanilla-Strohbett `StrawBedBlock` erbt davon): Schlaf-Pose, Bett-Ausrichtung beim Rendern,
  `checkBedExists`, Aufwachen per „Bett verlassen“ und `onStopSleeping` hängen alle an `instanceof AbstractBedBlock`.
  26.2 hat die Klasse nicht → `HammockBlock` liegt im 26.3-Overlay, Zwilling in `common/src/mc26_2/java` (nie registriert).
- Ob man liegen bleiben darf, fragt Vanilla jeden Tick über `getBedRule(level, pos).canSleep(level)` ab
  (`Player.tick`); die Regel ist überschreibbar → Hängematte: „darf“ genau wenn `level.isBrightOutside()` und die
  Dimension eine Standard-Uhr hat (das Komplement zur Bettregel `WHEN_DARK`). Bei Dunkelheit weckt Vanilla selbst.
- Tageszeit = `ServerClockManager` (Weltuhren, 26.x). `addTicks(clock, n)` gibt es auf 26.2 und 26.3.
- `ServerPlayer.startSleeping` setzt `TIME_SINCE_REST` (Phantom-Statistik) zurück; `SleepStatus` zählt jeden Schläfer;
  der Nacht-Sprung braucht `isSleepingLongEnough` (sleepCounter ≥ 100) bei genug Schläfern.
- Vorhandenes: `Feedback.playTo` (Sound nur für den Spieler, kein Text), `DISPENSER_FAIL` als Fehlschlag-Sound anderer
  Gadgets, `ServerTuningConfig` + `ServerTuning` (geklemmte Werte), Testzentrale mit Abdeckungstest,
  `SearchTabPlacement`, `ModItemGroupsContent.functionalRows`, Datagen für Rezepte/Beute, Modelle von Hand im Overlay.

## Entscheidungen (selbst getroffen)
- **Abstand** = Zahl der freien Blöcke zwischen den Ankern: 2 (Matte füllt die Lücke) oder 3 (Matte + 1 Seilstück
  `hammock_rope` am Kopfende). 1 und ≥ 4 schlagen fehl. Die Matte ist 2 lang.
- **Anker** = beliebiger Block mit nicht-leerer Kollisionsform, nicht ersetzbar, keine Hängematte/kein Seil (Zäune,
  Mauern, Stäbe, Glas, Laub zählen). Anker sitzen auf **Seilhöhe** (obere Lage).
- **Aufbau** (Blickrichtung FACING = Fuß → Kopf): obere Lage Fuß/Kopf (Seile, keine Kollision), untere Lage Fuß/Kopf
  (Tuch, hängt durch; Liegehöhe 0,25). 4 Blöcke, bei Abstand 3 plus Seil = 5.
- **Platzieren**: Klick auf die Seite eines Ankers → Matte spannt sich von dort weg; sonst Blickrichtung des Spielers,
  Klickposition = untere Fuß-Lage. Ohne passende Anker: kein Block, Sound `DISPENSER_FAIL` nur für den Spieler +
  Rauchpartikel, kein Text.
- **Abbau**: jeder Teil hängt an Partner/Anker (`updateShape`); fällt etwas weg, fällt die ganze Matte, gedroppt wird
  das Item genau einmal (Beutetabelle nur am unteren Kopfteil, wie Vanillas Bett). Kreativ: kein Drop.
- **Liegen**: Rechtsklick auf irgendeinen Teil → tagsüber, frei, keine Monster in 8×5 (wie Bett) → Vanilla-Schlafpose
  (`startSleeping`), Liegeplatz = unteres Kopfteil. Sonst Fehlschlag-Sound. Kein Spawnpunkt (Regel `can_set_spawn`
  never), kein Bett-Statistikpunkt.
- **Phantome**: Liegen setzt `TIME_SINCE_REST` **nicht** zurück (Wert vor `startSleeping` gemerkt und wieder
  gutgeschrieben). Begründung: sonst wäre jede Hängematte ein Phantom-Schutz ohne Nacht; während des Liegens zählt die
  Statistik wie in Vanilla nicht weiter (= Pause für die real gelegene Zeit, kein Reset, kein Vorteil).
- **Kein Tag-Sprung**: Hängematten-Schläfer bleiben unter 40 sleepCounter (Mixin auf `Player.tick`) → nie „tief“ →
  Vanillas Nachtsprung kann nie durch Hängematten ausgelöst werden; Nebeneffekt: der Bildschirm dunkelt nur leicht ab
  (Dösen statt Schwarz).
- **Zeitbeschleunigung** (Mixin am Ende von `ServerLevel.tick`): je Welt mit Standard-Uhr, wenn `advance_time`,
  `players_sleeping_percentage` ≤ 100, hell, und Hängematten-Schläfer ≥ max(1, ⌈aktive Spieler × Prozent / 100⌉)
  (aktive = keine Zuschauer, genau wie Vanillas `SleepStatus`) → `addTicks(uhr, faktor − 1)` je Tick, je Uhr nur einmal
  pro Server-Tick. Endet von selbst: bei Dunkelheit weckt Vanilla die Liegenden (Regel `canSleep` = never).
  `addTicks` statt `setRate`: nichts Dauerhaftes im Spielstand (ein Absturz während des Liegens hinterlässt keine
  falsche Uhrrate), keine Kollision mit anderen Raten.
- **Config**: `server.hammock.timeFactor` (Standard 8, hart 1…20; 1 = aus).
- **Keine Bildschirmtexte**: Vanillas „x/y Spieler schlafen“-Aktionsleiste wird unterdrückt, solange kein Bett-Schläfer
  dabei ist und es hell ist bzw. im Tick eines Hängematten-Aufwachens (Mixin `announceSleepStatus`).
- **Farben**: 16 wie Betten (vertretbar: Tuch nutzt die Vanilla-Wolltexturen, eine Vorlage, 16 kleine Modelle);
  Färben wie Betten: beliebige Hängematte + Farbstoff → Farbe.
- **Rezept** (vanilla-nah, ohne Wollknäuel): `/F/` über `WWW` (Stock, Faden, Stock; 3 Wolle einer Farbe) → 1.
- **Kreativ**: Vanilla-Funktionsblöcke direkt hinter dem rosa Bett; Mod-Tab SimpleMachines eigene Zeile.

## Dateien
- `mc26_3/overlay/.../McVersion.java` + `common/src/mc26_2/.../McVersion.java`: `HAMMOCK`.
- 26.3 `mc26_3/overlay/java/com/simplebuilding/blocks/custom/HammockBlock.java` (AbstractBedBlock), Zwilling 26.2.
- Shared: `blocks/custom/HammockRopeBlock`, `blocks/custom/HammockLayout` (Anker, Suche, Zustände),
  `blocks/custom/HammockTime` (Regel, Zählung, Beschleunigung), `items/custom/HammockItem`,
  Mixins `HammockPlayerMixin`, `HammockServerLevelMixin` (+ `simplebuilding.mixins.json`).
- `ModBlocks` (16 + Seil), `ModItems` (16), `ModItemGroupsContent`, `SearchTabPlacement`, `ServerTuningConfig`,
  `ServerTuning`, Datagen `ModRecipeProvider`, `ModLootTableProvider`.
- Ressourcen Overlay: Blockstates/Modelle/Item-Definitionen (Generator `tools/textures/hammock.py`), Texturen
  (Seil, 16 Item-Icons), Lang EN/DE beide Orte; Testzentrale (Maschinen-Abschnitt: 16 Matten an Zäunen).
- Tests `HammockTests` + `HammockGameTest` (Fabric) + Katalog; `DataIntegrityTests` (Layout, ohne Item/Beute),
  `ConfigOptionTests`; Wiki `manual.json` Notiz `*_hammock`.

## Risiken
- Platzieren mit Flag 26 (keine Formupdates) und erst danach Nachbarn benachrichtigen, sonst zerstören sich halbe Matten.
- Testserver: `advance_time` ist aus und die Tageszeit fest → Beschleunigung als reine Funktion + Rechnung am echten
  Level (ohne Anwenden) testen; Liegen nur prüfbar, wenn der Testserver hell ist (sonst Gegenprobe „darf nicht“).
- Mock-Spieler anderer Tests liegen in `level.players()` → Zählung im Test mit eigener Liste.

## Verifikation
GameTests: Abstand 1/2/3/4, fehlender Anker, Klick auf Ankerseite, Anker weg → Drop (einmal), Kreativ-Abbau ohne Drop,
Liegen (Pose, Ausrichtung, Phantom-Statistik bleibt, nie „tief“), Beschleunigung hell/dunkel/Gamerule/Prozent,
Mehrspieler-Anteil inkl. Zuschauer, Config-Grenzen. Gates: `fabric-263`, `neoforge-263`, 26.2-Compile, Forge-26.3-Compile,
Datagen, Wiki-Check. Nicht ohne Client prüfbar: Aussehen von Modell/Pose/Abdunkeln (Client-Abnahme offen).

## Stand nach Umsetzung (2026-10-03)
- Umgesetzt wie geplant. Abweichung: die obere Lage ist kein Teil des farbigen Blocks, sondern ein gemeinsamer,
  ungefärbter Seilblock `hammock_rope` (`kind=end` über jedem Tuchblock, `kind=span` als Seilstück bei 3 Abstand).
  Dadurch ist die Hängematte selbst ein Zwei-Block-Bett wie Vanilla und die Beute eine einfache Bett-Tabelle
  (nur `part=head`). Seile reichen 6 px in den Ankerblock (bis an Zaunpfosten/Stäbe).
- Tests: 6 GameTests `hammock_*` + DataIntegrity/ConfigOption/Testzentrale angepasst; `fabric-263` + `neoforge-263`
  1756/1756 „alles gruen“; Compile 26.2 (`:compileJava :neoforge:compileJava`) und Forge 26.3 grün; Datagen; Wiki
  `--all --check` grün. Der Testserver ist hell → der Liege-Test läuft den Tag-Zweig.
- Nicht getestet: Aussehen von Modell, Icons, Liegepose und Abdunkeln im Client; Zuschauer-Ausschluss nur per Code
  (Mock-Spieler können keine Zuschauer sein); echtes Weiterlaufen der Uhr (Testserver hat `advance_time` aus, geprüft
  wird die berechnete Tick-Zahl); Forge-26.3-GameTests nicht gelaufen (nur Compile); Mehrspieler mit echten Clients.
