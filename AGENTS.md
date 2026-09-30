# SimpleBuilding – Arbeitsanleitung für jeden Assistenten (Claude, GPT, andere)

Diese Datei ist die einzige maßgebliche Regelsammlung. `CLAUDE.md` verweist nur hierher.
Aktueller Stand und nächste Schritte: `docs/HANDOFF.md`. Warteschlange: `.claude/QUEUE.md`.
Besitzer spricht Deutsch. Antworten kurz, Ergebnis zuerst, Fragen an den Besitzer immer als Liste.

## 1. Projekt in einem Absatz
Minecraft-Mod „SimpleBuilding“ (Baustäbe, Blaupause, Oktant, Hammer, Rucksack, Pads, Enderit, Kerne, Handbücher).
Mehrere MC-Linien und Loader in einem Repo. **26.3 (Fabric + NeoForge) ist die Hauptlinie.**

## 2. Repo-Struktur
- `common/src/shared` = gemeinsamer Code für 26.2 und 26.3. `common/src/mc26_2` = 26.2-Overlays.
- `mc26_3/` = Overlay-Linie 26.3 (keine Kopie; `resources.gradle`, `syncGenerated263`).
- `mc26_4/` = Snapshot-Linie, nur mit `-Pmc264=true`.
- `mc1_21_11/` = vollständige Kopie für 1.21.11 (wird nur im Port-Run angefasst).
- `src/main` = Fabric + Datagen (`src/main/generated` gilt für alle Loader). `neoforge/`, `forge/` = weitere Loader.
- `wiki/` (Wiki, `wiki/manual.json` von Hand, `wiki/data/*` generiert), `tools/` (Textur-Generatoren, Testrunner, Balancing-Zentrale), `docs/` (Konzepte).

## 3. Harte Regeln des Besitzers
1. **Hauptlinie 26.3 zuerst.** Neue Features nur für 26.3 (`fabric-263`, `neoforge-263`) bauen und testen, bis der Besitzer
   die Welle abgenommen hat. Erst dann ein eigener **Port-Run** für 26.2 Fabric/NeoForge/Forge, die 1.21.11-Kopie und 26.4.
   Gemeinsamer Code muss trotzdem für 26.2 kompilieren (`check` bleibt grün).
2. **Push nur nach grünem Ergebnis.** Test-/Gate-Ausgabe lesen, erst dann pushen. `tools/testrunner/run.py` endet auch bei
   roten Tests mit Exit 0 – die Zeile „alles gruen“ muss dastehen. Nie `run.py | grep; git push` verketten.
3. **Nicht im Haupt-Repo bauen, solange Besitzer-Clients laufen** (sonst `NoClassDefFoundError` im Spiel). Gates in einem
   Worktree: `git worktree add $TEMP/sbgate` (detached auf master), dort `./gradlew.bat check -q` (26.4: `-Pmc264=true`).
4. **Kein Text auf dem Bildschirm für Gadgets** (keine Aktionsleiste/Chat): stattdessen Sounds, Partikel, Blockzustände.
5. **Amerikanisches Englisch** in allen englischen Texten. Deutsche Texte parallel pflegen (`en_us.json` + `de_de.json`).
6. **Wiki-Aussagen müssen im Code beweisbar sein.** Kommentare sind kein Beweis – bei Widerspruch zum Code ist der Kommentar
   veraltet. Nach Änderungen: `python wiki/generate.py` und `python wiki/generate.py --check`.
7. **Neue Pixelkunst im Stil des Besitzers**, immer mit Vorschau (16-fach, alt neben neu). Nie an den Rand malen, keine dunklen
   Eckfüllungen in Konturen, alte Besitzer-Texturen respektieren, keine weichgezeichneten Skalierungen.
   Generatoren: `python tools/textures/generate_textures.py` und `--check`.
8. **Server-Autorität:** alle Gameplay-Stellschrauben serverseitig, mit Obergrenzen, die Vanilla nicht gefährden.
9. Commit auf `master`, Nachricht endet mit `Co-Authored-By: <Assistent> <noreply@anthropic.com>` (bei Claude), sonst passende Zeile.
10. Gegenprobe/Mutationstests **nie** mit `git checkout` zurücksetzen – vorher committen. Nie `find` über das ganze Laufwerk.
11. Vor jedem Löschen/Überschreiben das Ziel ansehen; keine destruktiven Git-Befehle ohne Rückfrage.
12. Testläufe sparsam: Server-Ziele parallel, Client-Tests seriell und nur bei Anzeigeänderungen, Agenten nur Server-Tests.
13. Am Ende eines Runs: Testzentrale neu bauen (`/sbtestcentre build`), Abdeckungstest muss alle Items abdecken.

## 4. Tests und Gate
- Vollständiges Gate: `./gradlew.bat check -q` (enthält checkBalance, JadeProviderSplitTest, checkAtlases, checkWiki).
- Server-Tests: `python tools/testrunner/run.py --targets fabric-263,neoforge-263 [--filter <muster>]`
  (`--targets all` für alle Linien). **Ein Filtermuster pro Aufruf** – Kommas nutzen still nur das erste.
- Client-Tests: `python tools/testrunner/run.py --targets client` (seriell; Umgebungsvariable `SIMPLEBUILDING_CLIENT_ONLY`
  für einzelne Tests). Nur laufen lassen, wenn keine Besitzer-Clients offen sind.
- Texturen prüfen: `python tools/textures/generate_textures.py --check`. Handbücher: `python tools/guide_book_pages.py` (0 Probleme).
- Zuletzt grüner Stand: Server-Gate 5063/5063 (Commit fdcccdf4); Client-Suiten 732/732 nach der Testreparatur.

## 5. Merge-Hinweise (aus Erfahrung)
- Lang-Dateien (`src/main/resources/assets/simplebuilding/lang/*.json`): nach Merge auf doppelte Schlüssel prüfen.
- `wiki/data/*`: bei Konflikt „theirs“ nehmen, dann `python wiki/generate.py` neu laufen lassen. `wiki/manual.json` 3-Wege-Merge nach id.
- Der Generator schreibt neue Zeilenenden-Änderungen in `.mcmeta`/`mc1_21_11` – reine CRLF-Änderungen zurücksetzen.
- Test-Katalog `SimpleBuildingGameTests.java`: nach Union-Merges auf fehlende Kommas achten.

## 6. Fallen je Loader
- NeoForge: HUD-Ebenen sind getrennt (`extractExperienceLevel`); `playBidirectional` mit 3 Argumenten registriert keinen
  Client-Handler; eigene `trimmed_armor`-Itemmodelle → Mod lädt `ordering="AFTER"` neoforge und liefert auf 26.3 keine
  `textures/*/trims/color_palettes`.
- Forge: umbenannte IDs brauchen `ForgeRegistry#addAlias`; `BreakEvent` verweigern mit `Result.DENY`.
  26.3 ist separat mit `-Pforge263=true`; ForgeGradle-7-Runs brauchen zusaetzlich Java 8 fuer Slime Launcher,
  Spiel/Compiler Java 25. Klassen/Ressourcen im selben Ausgabeordner, MixinConfigs im Jar-Manifest.
  26.3: kein KEYSYM mehr (KeyMapping-Konstruktor ohne Type); GameTest-TestData braucht die Dimension
  (`McVersion.testData`). LootTableLoadEvent kommt vor verfuegbaren Loot-Holders: Injektion erst nach
  dem Registry-Laden, vor Validierung. FarmlandTrampleEvent braucht einen eigenen Breeze-Handler.
  Details und verifizierter Stand: `docs/FORGE-26.3.md`.
- 26.3 (SDL-Texteingabe): eigene Textfelder müssen `Minecraft.onTextInputFocusChange(this, focused)` aufrufen.
- Jade: ein Provider darf nicht `IServerDataProvider` und `IComponentProvider` zugleich sein (Split in Server-/Client-Provider).
- ModDataFixer läuft am DataFixer nach; umbenannte Item-IDs brauchen Alias in `LegacyItemIds`.

## 7. Balancing-Zentrale (Dev-Server)
`python tools/devserver/serve.py` (Port 8770), Code in `tools/devserver/sbdev`, Daten in `balance/` (Versionen, Rollback).
Speichern schreibt Zahlen direkt in Java/JSON. Details: `docs/BALANCING-ZENTRALE.md`.
**Launch- und Testzentrale:** `python tools/launchhub/server.py` (Port 8771): Clients/Server starten, Tests im Gate-Worktree, Verlauf, KI-Fix. Details: `docs/LAUNCHHUB.md`. Die Client-/Server-Einträge in `.claude/launch.json` gibt es nicht mehr.

## 8. Ablauf für neue Wünsche des Besitzers
1. Wunsch in `.claude/QUEUE.md` eintragen. 2. Umsetzen nur auf 26.3. 3. Server-Tests gefiltert, am Ende volles Gate im Worktree.
4. Grün lesen → committen → pushen. 5. Wiki/Bücher/Testzentrale aktualisieren. 6. Erst nach Abnahme: Port-Run.

## 9. Wichtige Entscheidungen des Besitzers (Kurzfassung, Details in `docs/HANDOFF.md`)
Config serverseitig + Obergrenzen · Chunk-Loader nur bei Online-Besitzer · Steinmetz verkauft keine Kerne, fahrender Händler
selten · Rotator: kein Mending · Vorlagen ohne „Smithing“ im Namen · Glowing hat eine Stufe · Magnet heißt Attractor ·
Luftsprung 20 s / 10 s, Server erzwingt · Hammer-Haltbarkeit unverändert bis der Besitzer testet · Reittier-Rüstungen abgenommen.

## 10. Multimod-Grundlage (26.3)
Zusatzmods liegen additiv unter `modules/<id>/`; bestehende Projekte niemals verschieben.
Anlage: `python tools/newmod.py <id> "Name"`. Manifest: `modules/modules.json`, Dev-Mods: `tools/devmods.json`.
Integration nutzt eigene Saves unter `integration/run-fabric-263` und `integration/enabled-mods.json`.
Module koppeln nur ueber framework-API oder oeffentliche Registry-IDs, nie interne Klassen.
Details und Grenzen: `docs/MULTIMOD.md`; Integrationstest: `--targets integration-263` (separat vom Bestand).
