# Plan: Amboss-Kiesel (2026-10-02)

## Bestand und Entscheidung

- `ModItems.DIAMOND_PEBBLE` existiert bereits. `ModRecipeProvider` verarbeitet neun Kiesel zu einem Rohdiamanten (`cracked_diamond`); dessen Hochofenrezept liefert einen Diamanten. Ein Diamantblock entspricht damit 81 Kieseln.
- Der Vorschlaghammer liefert bereits 81 Kiesel. Der neue automatische Weg liefert fest **72 Kiesel**, entsprechend acht Diamanten: Verlust von einem Diamanten (1/9). Keine neue Config: Die feste serverseitige Menge ist zugleich die harte Obergrenze.
- `TransformTargets`/`ModuleTransformHints` beschreiben Rechtsklicks. Ein fallender Amboss ist kein Rechtsklick; daher kein irrefuehrender Handhinweis. JEI und REI verwenden gemeinsam `InWorldRecipeCatalog` aus `InWorldTransformations`.
- Es gibt bisher keine Amboss-Falltransformation. Der kleinste passende Hook ist Vanillas `AnvilBlock.onLand(..., FallingBlockEntity)` nach erfolgreicher Landung. Bestehendes Mixin-/Transformationsmuster und Block-Drops wiederverwenden.

## Umsetzung

1. `McVersion.ANVIL_DIAMOND_CRUSH`: 26.3 true, 26.2 false.
2. Gemeinsamer Server-Helper und Amboss-Mixin: nur echte fallende Ambosse nach mindestens einem Block Hoehenverlust, nur Diamantblock direkt unter der Landestelle. Automationsberechtigung pruefen; Block ohne Diamantblock-Drop erfolgreich verbrauchen, dann 72 Kiesel in legalen Stapeln ausgeben. Vanilla-Landung und Verschleiss bleiben erhalten.
3. Kreativ-Ambosse erhalten keinen Sonderbonus. Auch kostenlose/automatisch wiederverwendete Ambosse muessen einen echten Diamantblock verbrauchen. Ein weiterer Fall auf die leere Stelle liefert nichts. Dispenser-/Kolbenschleifen koennen dadurch keine Diamanten vermehren. Kreativ erzeugte Diamantbloecke bleiben bewusst normale Kreativ-Ressourcen; keine Herkunftsdatenbank.
4. Bestehenden Diamant-Crush-Export um versionierte Ambossdaten erweitern; zweite JEI-/REI-Zeile derselben Kategorie und Wiki-Zeile daraus erzeugen. EN/DE-Schluessel in Basis und Overlay; Wiki-Notiz bei den Kieseln.
5. GameTests mit echter fallender Entity fuer Ausbeute/Verbrauch, Wiederholung und ungueltige Ziele/Fallbloecke; Katalog-/Rezeptgrenze pruefen. Sortierte `GameTestSpec.named`-Eintraege und Fabric-Adapter registrieren.

## Risiken und Verifikation

- Hook muss auf Fabric und NeoForge tatsaechlich laufen; kein bloss direkter Helper-Test. Hoehenpruefung schuetzt gegen aufgesetzte Ambosse. Claim-Bruecke darf nicht umgangen werden.
- Datagen 26.3 und `syncGenerated263`, Wiki `--all` und `--all --check`; Sprach-JSON auf doppelte Schluessel pruefen.
- Gefilterte Server-Tests, anschliessend vollstaendig `python tools/testrunner/run.py --targets "fabric-263,neoforge-263"`; Ergebniszeile lesen.
- `:compileJava :neoforge:compileJava` (26.2), `-Pforge263=true :mc26_3:forge:compileJava`, vollstaendiges `check -q` hier im Worktree. Testzentrum-/Abdeckungstests sind Teil der Serversuite.
- Keine Minecraft-Clients, keine Besitzerwelt, kein anderer Worktree, kein Push/Merge. Commit ausschliesslich auf `claude-gpt-anvil` mit vorgeschriebenem Co-Author.

## Ergebnis

Umgesetzt: Server-Helper mit Vanilla-Landungs-Mixin, Versionsflag, gemeinsamer
JEI-/REI-Export, Wiki-Zeile und EN/DE-Notizen, drei registrierte GameTests.
Die Probe nutzt normale geplante Block-Ticks fuer intakte/angeschlagene Ambosse.
Nur der beschaedigte Amboss wird als echte FallingBlockEntity ohne zufaelligen
Bruch erzeugt, damit die Ausbeuteprobe deterministisch bleibt. Zerbricht ein
Amboss vor erfolgreicher Landung, greift `onLand` nicht; der Diamantblock bleibt.

- Sprach-JSON: keine doppelten Keys; zwei neue Keys in EN/DE an beiden Orten.
- `wiki/generate.py`: Python-Syntax geprueft.
- Wiki-Unit-Tests: 37/38 bestanden. Bestehender Fehler:
  `test_all_target_items_have_bilingual_unique_sourced_sentences` vermisst
  `simplebuilding:polished_ender_quartz_checker`. Dieselbe Luecke wurde direkt
  gegen `HEAD:wiki/manual.json` und `HEAD:wiki/data/simplebuilding.json` bestaetigt.
- Amboss-GameTests Fabric/NeoForge 26.3: `alles gruen: 6/6 bestanden, 0 rot`
  (Run `2026-10-02T22-30-02Z-8b8e`). Der erste Versuch kompilierte wegen einer
  auf 26.3 entfernten EntityType-Konstante nicht; die unnoetige Typ-ID-Pruefung
  wurde entfernt. Mixin-Landung und Amboss-Tag begrenzen weiterhin den Pfad.
- Datagen + `syncGenerated263`, `:compileJava :neoforge:compileJava` (26.2)
  und `-Pforge263=true :mc26_3:forge:compileJava`: `BUILD SUCCESSFUL`, `GRADLE_EXIT=0`.
- Der 26.3-Datagen-Diff enthaelt ausschliesslich den Amboss-Abschnitt in
  `mc26_3/generated/wiki/inworld.json`.
- Wiki `--all` aktualisiert zusaetzlich veraltete SimpleMoney-Ableitungen:
  24.000 statt 10.000 Ticks fuer den Geldschein, wie bereits in den unveraenderten
  Modulquellen/Notizen hinterlegt. Nur generierte Daten und Modulhash werden
  aktualisiert; keine SimpleMoney-Spielmechanik wird geaendert.
- Wiki `--all --check`: `wiki: up to date, everything documented.` Der Generator
  holt auch bereits vorhandene seltene Struktur-Funde und Registry-Eintraege nach
  (HEAD-Registry-Export schon 260 Items, alte Wiki-Ableitung erst 256).
- Erste Vollsuite: `NICHT gruen: 1731/1732 bestanden, 1 rot`; NeoForge 866/866,
  Fabric 865/866. Alle Amboss-Tests bestanden. Abweichung vom Plan: Eine bestehende
  positionsabhaengige Testannahme in `BuildingEnchantmentTests` wird korrigiert.
  Neun Positionen duerfen alle denselben Palette-Hash haben; beide gesammelten
  Materialien und ihre Reihenfolge werden nun direkt geprueft, jede Position
  weiterhin gegen ihren Hash. Kein Color-Palette-Gameplay-Code wird geaendert.
- Das volle Gate bindet auch die Wiki-Unit-Tests ein. Zweite kleine Abweichung:
  Die bereits in HEAD fehlende EN/DE-Einzelnotiz fuer das polierte Enderquarz-
  Schachbrett wird anhand seines vorhandenen Rezepts ergaenzt (2 polierte
  Enderquarze + 2 Quarzbloecke diagonal ergeben 4). Keine Rezeptaenderung.
- Nach Regenerierung bestehen alle **38 Wiki-Unit-Tests**. Der erste Gate-Lauf
  scheiterte an genau dieser inzwischen behobenen Luecke (`:testWikiModules`).
- Die Amboss-Probe erlaubt normalen Verschleiss beim zweiten Fall in den
  verbrauchten Diamantblock: Ein bereits beschaedigter Amboss darf dabei brechen.
  Blockverbrauch und Kieselmenge werden unveraendert streng geprueft; der anfangs
  intakte Amboss muss beide kurzen Faelle ueberstehen.
- Abschliessende Vollsuite: **`alles gruen: 1732/1732 bestanden, 0 rot`**,
  Fabric 866/866 und NeoForge 866/866; Run `2026-10-02T23-04-19Z-69aa`.
- Testzentrale: alle sieben `test_centre_game_test_*` je Loader bestanden,
  einschliesslich echtem Vollaufbau durch `TestCentreBuilder.build` und
  Abdeckung aller registrierten Items/Bloecke in den separaten Testwelten.
- Abschliessendes Gate mit `check :compileJava :neoforge:compileJava
  -Pforge263=true :mc26_3:forge:compileJava -q`: **`CHECK_EXIT=0`**.
  Log: `.ai-runs/anvil-check-final.log`. Wiki, Sprachen, Balancing, Module,
  Overlays, Atlanten, Quests und vorhandene Unit-Tests bleiben im Gate enthalten.

## Offen / bewusst nicht ausgefuehrt

- Keine Minecraft-Clienttests oder Sichtabnahme von JEI/REI; keine Besitzerwelt
  veraendert. Testzentrale der Besitzerwelt bleibt deren eigener Abnahmeschritt.
- Forge 26.3 und 26.2 kompiliert, aber keine Laufzeittests dieser Ziele.
- Kein eigener Claim-Mod-Integrationstest und keine echte Dispenser-/Redstone-
  Anlage. Automationsschutz folgt der bestehenden `WorldPermissions`-Bruecke;
  echte wiederholte Ambossfaelle und die Ressourcenbilanz sind getestet.
- Kein Port-Run, Merge oder Push. Nur Commit auf dem aktuellen Worker-Branch.
