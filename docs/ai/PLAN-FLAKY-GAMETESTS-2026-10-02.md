# Wackelige GameTests stabilisieren

## Auftrag und Regeln

Nur Branch `claude-gpt-flaky`, kein Push/Merge. Gemeinsame Tests bleiben auf 26.2 kompilierbar;
keine Ports oder Gameplay-Aenderungen. Bestehende unversionierte `.serena/` bleibt unberuehrt.

## Befund und Plan

1. `TestCentreTests`: feste Tickfristen fuer entfernte Forced Chunks und Befehlsausfuehrung.
   Vor dem Bau auf wirksame Forced Tickets warten. Jeden Knopf genau einmal druecken,
   Ausfuehrung abwarten, alle Zaehler exakt pruefen und erst danach fortfahren. Marker
   durch eigene Scoreboard-Zaehler ersetzen: Entity-Sichtbarkeit darf kein Messinstrument sein.
2. `Stage4ClaimTests.attractor`: synchroner Entity-Query in frisch geladenen Nachbarchunks.
   Beide Chunks erzwingen/laden und vor allen drei Schutzmodi mit Item-Probes auf tatsaechliche
   Query-Sichtbarkeit warten; echte Item-Nutzung und BlockEntity-Ticker sowie positive/negative
   Assertions beibehalten. Fabric erhaelt dasselbe 100-Tick-Ladebudget wie NeoForge (vorher 20).
3. `RareStructureFindsTests`: zwoelf zufaellige Versuche je Endermite koennen trotz freier
   Plaetze erschoepft werden. Seed und freie Spawnflaeche festlegen, exakt vier, einmalig,
   sicher und nicht persistent weiter pruefen.
4. `ToolBehaviourTests` und `MagnetTests`: Pickup-Tests pumpen nur die Spielerverbindung
   in `succeedWhen`, waehrend Itemphysik separat von Chunk-Ticking und anderen Tests abhaengt.
   Spieler und Items gemeinsam maximal 200 Schritte synchron ausfuehren; echte Verbindung,
   Inventar-/Equipment-Ticks, Itemphysik und Vanilla-Pickup beibehalten.
5. `BuildingEnchantmentTests`: positionsabhaengiger Hash auf neun Zellen garantiert keine
   zwei Farben. Vorhandener Fix prueft Zuordnung, aber nur eine Teilmenge der Materialien.
   Deterministische Vorschaupositionen mit beiden Hashwerten waehlen und Vielfalt exakt pruefen.

## Risiken und Verifikation

- Wiederholtes Polling darf Aktionen nicht wiederholen und keine falschen Zaehler verbergen.
- Chunk-Wartebedingungen muessen auf beiden Loadern funktionieren; Cleanup auch bei Timeout.
- Seeds isoliert an Test-Entities setzen, niemals am gemeinsamen Level-Zufall.
- Jeden betroffenen Filter mindestens dreimal auf Fabric und NeoForge 26.3 ausfuehren.
  Anschliessend beide vollstaendigen Basissuiten und SimpleTweaks-Modulsuiten.
- `check -q`, 26.2 `:compileJava :neoforge:compileJava`, Forge-26.3-Compile,
  Wiki generieren/pruefen, Testzentralenbau und Item-Abdeckung im GameTest-Gate.
- Rohlogs und aktuelle Ergebniszeilen lesen; keine alten Gate-Zahlen als Nachweis.

## Umgebung

RTK und rg fehlen im PATH; die dokumentierten lokalen RTK-Verzeichnisse enthalten kein Binary.
Serena/Context7 sind in dieser Sitzung nicht als MCP-Werkzeuge verfuegbar. Gezielte native
Quellreads und vorhandene Vanilla-Quellen ersetzen diese Abfragen.

## Ergebnisse

- Gegenpruefung am Vanilla-Bytecode: `GameTestHelper.spawnItem` setzt die Geschwindigkeit
  bereits auf null. Die anfaengliche Wurfimpuls-Hypothese ist widerlegt; keine redundanten
  Geschwindigkeits-Resets im Fix.
- Historische Gate-Datensaetze `2026-10-03T22-39-45Z-3d36`, `2026-10-01T20-36-45Z-517a`
  und `2026-10-01T19-41-21Z-4bb7`: Claims scheitert erst bei der positiven Nachbarfreigabe
  auf Tick 0. Das lokale Item bewegt sich, das fremde ist noch nicht abfragbar.
- Erster Knopf-Lauf `2026-10-03T23-08-23Z-e6cd`: 0/2. Neue Vorbedingung wartet bis zum
  Timeout auf Chunk-Bereitschaft; Bedingung wird vor den Wiederholungslaeufen korrigiert.
- Vollsuite `2026-10-03T23-15-20Z-3c7b`: Fabric 907/907, NeoForge 906/907 (Knopf),
  SimpleTweaks NeoForge 51/52 (ungetickter Nachbarchunk). Fabric-Modul startet wegen fehlendem
  `python` im Gradle-PATH nicht. PATH fuer alle Folgelaeufe korrigiert.
- Knopf-Lauf `2026-10-03T23-26-12Z-4543`: Fabric sieht auf Tick 4 zahlreiche alte Marker
  und zwei statt einer Ausfuehrung des ersten Knopfs. Der Builder kann noch nicht sichtbare
  Entities beim Leeren nicht entfernen. NeoForge erreicht die Bereitschaft eines spaeteren
  Befehlsblocks nicht innerhalb von 600 Ticks. Eigene Scoreboard-Objectives isolieren jeden
  Lauf; 1200 Ticks decken pro Knopf Ausfuehrung + 3 Beobachtungsticks + Loslassen und Chunk-Ladung.
- Abweichung: Nicht alle ungenutzten Randchunks auf Entity-Ticking pruefen. Fuer den Bau
  genuegen wirksame Tickets; fuer jeden Knopf wird die tatsaechliche Ausfuehrung abgewartet.
  Fataler Fehler, Erfolg und Timeout geben die Testressourcen wieder frei.
- Der feste entfernte Bereich liegt ausserhalb der vom GameTest-Harness zurueckgesetzten
  Struktur. Der Builder entfernt Bloecke/Entities, aber keine geplanten Block-/Fluessigkeitsticks.
  Auch diese Warteschlangen werden deshalb vor dem Bau und beim Cleanup geloescht. Der neue
  Zaehler verhindert ausserdem, dass spaet sichtbar werdende alte Marker als neue Ausfuehrung gelten.
- Zusaetzliche Assertions pruefen vor jedem Druck den unveraenderten Zaehlerbefehl und
  ausgeschalteten Knopf/Befehlsblock; nach dem Druck Stromversorgung und Ausfuehrungsbedingung.
  Timeouts behalten den letzten echten Zustand (Befehl, Strom, geplanter Tick, Chunk-Bereitschaft).
- Diagnoselauf `2026-10-03T23-44-11Z-4dda`: NeoForge `alles gruen: 1/1 bestanden, 0 rot`.
- Magnet: Der gemeldete alte Timeout wurde nicht isoliert reproduziert. Die belegte
  Abhaengigkeit des Treibers von zwei getrennten Taktgebern und nebenlaeufigen Tests entfaellt;
  das 200-Schritte-Limit und alle Bewegungs-/Reichweiten-/Pickup-Aussagen bleiben erhalten.
- Endgueltige Verifikation abgeschlossen; alle angeforderten Wiederholungen und Vollsuiten gruen.
- Knopf final: `2026-10-03T23-49-56Z-4ed2`, `2026-10-03T23-59-33Z-5e92`,
  `2026-10-04T00-03-05Z-c070`: jeweils `alles gruen: 2/2 bestanden, 0 rot` (beide Loader).
- Claims `2026-10-04T00-06-58Z-75d4`: NeoForge 1/1; Fabric wartet bei Tick 22 noch auf
  Entity-Ticking. Fabric hatte nur 20 Ticks, NeoForge 100. Fabric wird auf dasselbe
  100-Tick-Budget fuer die neue Chunk-Vorbereitung angehoben; beide Chunks werden synchron
  geladen, bevor ihre Entity-Bereitschaft abgewartet wird. Keine Gameplay-Assertion veraendert.
- Claims `2026-10-04T00-12-57Z-6f6f`: beide Loader warten bis Tick 102 auf den strengeren
  Entity-Ticking-Status. Der synchrone Attractor benoetigt nur Entity-Abfragbarkeit.
  Vorbedingung deshalb direkt mit zwei echten Item-Probes an der Grenze pruefen und diese
  vor den unveraenderten drei Claim-Szenarien entfernen. Kein Claim-Provider bleibt ueber
  asynchrone Schritte installiert. Sichtbarkeit der eigentlichen Szenario-Items ebenfalls geprueft.
- Wiki `--all` generiert und `--all --check`: `wiki: up to date, everything documented.`
  Keine inhaltlichen Wiki-Diffs. Handbuchcheck mit Pillow: zwei bestehende Ueberlaengen
  (`de_de guide topics 1`: 14 Zeilen; `topics 3`: 15 Zeilen, Limit 13). Sprachdateien unveraendert;
  kein sachfremder Handbuch-Fix in diesem Run.
- Claims final: `2026-10-04T00-20-03Z-51d3`, `2026-10-04T00-23-42Z-cda1`,
  `2026-10-04T00-28-00Z-bd19`: jeweils `alles gruen: 2/2 bestanden, 0 rot` (beide Modul-Loader).
- Shulker final: `2026-10-04T00-30-40Z-3a56`, `2026-10-04T00-33-30Z-95f1`,
  `2026-10-04T00-36-18Z-1bbc`: jeweils `alles gruen: 2/2 bestanden, 0 rot` (beide Loader).
- Magnet final (`simplebuilding:*magnet*`, einschliesslich Haupt-/Nebenhand-Pickup):
  `2026-10-04T00-39-08Z-350c`, `2026-10-04T00-42-42Z-d2ed`, `2026-10-04T00-45-53Z-bda9`:
  jeweils `alles gruen: 22/22 bestanden, 0 rot` (elf Tests je Loader).
- Farbpalette final: `2026-10-04T00-48-22Z-5389`, `2026-10-04T00-51-09Z-61d8`,
  `2026-10-04T00-54-05Z-5221`: jeweils `alles gruen: 2/2 bestanden, 0 rot` (beide Loader).
  Damit alle fuenf betroffenen Gruppen mindestens dreimal pro Loader gruen.
- Gradle-Gate: `./gradlew.bat check -q :compileJava :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava`
  beendet mit `GRADLE_GATE_EXIT=0`. Einschliesslich shared 26.2 Fabric/NeoForge und Forge 26.3;
  Balancing, Multimod, Atlas/Jade/Wiki-Pruefungen gruen. Wiki-Unittests: `Ran 48 tests ... OK`.
  Vollstaendiges Log lokal unter `build/flaky-gate.log`.
- Vollsuite `2026-10-04T01-12-51Z-fd89`: Fabric 907/907, NeoForge 907/907;
  `alles gruen: 1814/1814 bestanden, 0 rot`. Testzentralen-Neubau/Planvergleich,
  Abdeckung aller Items/Bloecke und jeder einzelne Knopf auf beiden Loadern bestanden.
- Vollsuite SimpleTweaks `2026-10-04T01-22-53Z-838e`: Fabric 52/52, NeoForge 52/52;
  `alles gruen: 104/104 bestanden, 0 rot`. Gesamtstatus: `FINAL_EXITS gate=0 base=0 modules=0`.
- Kein Client gestartet, keine Anzeigeaenderung, kein Gameplay-Fix, kein Port und kein Push/Merge.
  Forge-GameTests sowie GameTests fuer 26.2/1.21.11/26.4 nicht ausgefuehrt (ausserhalb dieses Auftrags).
  Generierte Wiki-Dateien nach Diff-Pruefung ohne Inhaltsaenderung; `.serena/` unangetastet.
