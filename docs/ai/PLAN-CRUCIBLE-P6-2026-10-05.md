# Crucible P6 (2026-10-05)

## Ist-Zustand und Vorgehen
- `SoulLava` verwendet feste Zahlen. `ServerTuningConfig` bietet einzeilige Felder, Validierung und synchronisierte Serverwerte. Die neue Gruppe folgt diesem Muster; alle bisherigen Nutzer werden umgestellt.
- Jade verwendet `BlockInfo.Topic` mit getrennten Server-/Client-Providern. Der neue Server-Topic liest ausschließlich über `CrucibleCompat` und `SimpleLibApi`.
- JEI besitzt widgetbasierte Kategorien und einen Runtime-Hook für Vanilla-Rezepte. Die Tiegelkategorie folgt diesen Mustern und wird nur bei `McVersion.CRUCIBLE` registriert.

## Dateien
Config/Fluid/Effekt/Weltgen, ConfigOptionTests und CrucibleTests samt Katalog/Adapter; CrucibleCompat (26.3 und 26.2), BlockInfo/Jade-Plugin, SimpleLibApi/CrucibleBlockEntity/CrucibleJob, JEI-Kategorie/Plugin; vier EN/DE-Sprachdateien, CONFIG.md, Queue und generierte Wiki-Daten. Balancing-Leser auf explizite Gruppen prüfen.

## Entscheidungen und Annahmen
- Der aktuelle Auftrag hat bei abweichenden Zahlen Vorrang vor dem älteren §13: Brennstofffaktor 1..20, Branddauer als Sekunden. Zündversuche/-reichweite bleiben unverändert.
- Brennstoff wird als Item-Komponente registriert: lokale Config beim Start lesen, Neustart und gleiche Modpack-Datei auf Client/Server erforderlich. Andere Werte werden zur Laufzeit gelesen; vorhandene Fluid-Ticks können noch mit alter Verzögerung geplant sein.
- Jade zählt tatsächlich belegte Slots, keine Reservierungen. Restzeit ist die kürzeste eines laufenden Slots, auf volle Sekunden aufgerundet, mit demselben diskreten Fortschrittsschritt wie das Gameplay (Stufe, Hitze, Quellenabstand). Kalte/blockierte Slots haben keine endliche Restzeit. `target` ist ein Ausgabe-Slotindex, keine Zeit.
- JEI zeigt pro Eingabe und erforderlicher Hitzestufe die schnellste Rezeptvariante. Zeit gilt für einen Eisen-Tiegel bei der angegebenen Mindesthitze ohne Abstandsabschlag. Aufwärmen nur für warmbare Nahrung ohne Kochrezept. Hitzeentscheidung wird im SimpleLib-Gameplay zentralisiert und über die API angeboten.
- 26.2-Zwilling bleibt ohne SimpleLib; keine Kategorie dort. Keine Clients, Texturen, Modelle oder gesperrten Dateien bearbeiten.

## Risiken und Verifikation
JEI-API und Rezeptzutaten anhand vorhandener Quellen/Compile prüfen; dynamische Tags und Serverwerte berücksichtigen. Testfälle für alle Config-Grenzen, Getter/Fluid, Jade-Daten und zentrale Hitzeregel hinzufügen. Gefilterte Fabric-263-Tests (`crucible*`, `config*`) jeweils auf „alles gruen“ prüfen. Angeforderte 26.2-/26.3-Compiles, Forge 26.3 und `check -q` aus diesem Worktree, Wiki `--all`/`--all --check`, Sprachschlüssel auf Duplikate prüfen. Befehle auf 600 Sekunden begrenzen. Kein Push, Commit nur auf `claude-crucibleart-gpt`.

## Ergebnis
- Implementiert: elf Config-Felder mit doppelter Begrenzung (Validierung/Getter), Neustartmarkierung für Brennstoff, Jade-Topic mit API-Brücke, JEI-Kategorie/Katalog und vier neue GameTests samt Adaptern.
- Balancing-Zentrale liest Gruppen rekursiv; keine feste Gruppenliste in `service.py` zu ergänzen. Config-Daten werden durch den Wiki-Generator übernommen.
- Jade-Plugin wird auch von der unveränderten 1.21.11-Kopie kompiliert. Deshalb Registrierung über die vorhandenen Topic-IDs statt eines dort nicht vorhandenen `CRUCIBLE`-Symbols.
- Fabric-263 `config*`: **alles gruen: 15/15 bestanden, 0 rot**, Lauf `2026-10-05T06-56-30Z-1b51`.
- Fabric-263 `crucible*`: **alles gruen: 16/16 bestanden, 0 rot**, Lauf `2026-10-05T07-00-46Z-fbcf`. Erster Lauf 14/16: zwei falsche Testannahmen korrigiert; Vanilla 26.3 verwendet für Rindfleisch 200 Räucher-Rezeptticks statt 100. Tests lesen nun die geladenen Rezeptzeiten. Jade prüft zusätzlich kürzesten Job, blockierte/kalte Slots, alle vier Stufen, Abstandsabschlag und NBT-Rundreise.
- Pflicht-Compiles Fabric/NeoForge 26.2, Fabric/NeoForge 26.3 und Forge 26.3: jeweils `BUILD SUCCESSFUL`, Exit 0.
- Vier Sprachdateien: keine doppelten Schlüssel, je 34 neue P6-Schlüssel; keine SimpleLib-Imports im Shared-/JEI-/Jade-Code oder 26.2-Zwilling. Gesperrte Dateien unverändert.
- Wiki `--all` mit der vorgegebenen venv und abschließend `--all --check` mit uv-Python grün: `wiki: up to date, everything documented.` Die vorhandene JEI-Seelenlava-Infoseite nennt Zahlen ausdrücklich als konfigurierbare Standardwerte.
- Fabric-263 `test_centre*`: **alles gruen: 7/7 bestanden, 0 rot**, Lauf `2026-10-05T07-04-36Z-c03d`. Vollständiger Testzentralen-Neubau und Item-/Block-Abdeckung bestanden.
- `checkBalance`: 223 erzeugte Stellen geprüft, 0 Fehler, 0 Hinweise. Zwei vollständige `check -q`-Versuche erreichten das vorgeschriebene 600-Sekunden-Limit ohne gemeldeten Testfehler. Der dritte vollständige Lauf (`check --console=plain --max-workers=2`) erreichte das Limit in `checkWiki`: lange Loader-Konfiguration, danach Balance-/Moduldatenprüfungen. **Kein erfolgreicher Gesamt-Gate-Abschluss.**
- Dritter Gate-Lauf: `checkAtlases`, `checkOverlays`, `checkModuleData`, `checkModuleGuides`, `checkMultimod`, `checkQuests`, Framework-/Integration-/Loader-/Modul-Checks ohne Fehler abgeschlossen. Wiki-Abgleich separat erfolgreich (siehe oben). Die noch nicht erreichten Root-JUnit-Tests und `testWikiModules` separat mit `:test :testWikiModules --console=plain --max-workers=2` ausgeführt: **BUILD SUCCESSFUL**, Exit 0; **9 JUnit-Tests, 0 Fehler**, darunter **JadeProviderSplitTest 1/1**, und **55 Wiki-Tests, OK**. Der Gesamtcheck bleibt als Timeout dokumentiert, nicht als grün.
- Kein Client: Jade-/JEI-Layout und reales Runtime-Plugin-Laden visuell nicht getestet. NeoForge-/Forge-Gameplay in diesem Run nicht gestartet. Kein Push/Port; gesperrte Artwork-/Block-/Renderer-Dateien unverändert.
