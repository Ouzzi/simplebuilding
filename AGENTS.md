# SimpleBuilding – Arbeitsanleitung für jeden Assistenten (Claude, GPT, andere)

Diese Datei ist die einzige maßgebliche Regelsammlung. `CLAUDE.md` verweist nur hierher.
Aktueller Stand und nächste Schritte: `docs/HANDOFF.md`. Warteschlange: `.claude/QUEUE.md`.
**Prinzipien der ganzen Modfamilie (für alle Mods + je Mod): `docs/ai/PRINZIPIEN.md` – vor jeder Änderung lesen.**
Besitzer spricht Deutsch. Antworten kurz, Ergebnis zuerst, Fragen an den Besitzer immer als Liste.

## 1. Projekt in einem Absatz
Minecraft-Mod „SimpleBuilding“ (Baustäbe, Blaupause, Oktant, Hammer, Rucksack, Pads, Enderit, Kerne, Handbücher).
Mehrere MC-Linien und Loader in einem Repo. **26.3 (Fabric + NeoForge + Forge) ist die Hauptlinie; Hauptloader ist Fabric.**

## 2. Repo-Struktur
- `common/src/shared` = gemeinsamer Code für 26.2 und 26.3. `common/src/mc26_2` = 26.2-Overlays.
- `mc26_3/` = Overlay-Linie 26.3 (keine Kopie; `resources.gradle`, `syncGenerated263`).
- `mc26_4/` = Snapshot-Linie, nur mit `-Pmc264=true`.
- `mc1_21_11/` = vollständige Kopie für 1.21.11 (wird nur im Port-Run angefasst).
- `src/main` = Fabric + Datagen (`src/main/generated` gilt für alle Loader). `neoforge/`, `forge/` = weitere Loader.
- `wiki/` (Wiki, `wiki/manual.json` von Hand, `wiki/data/*` generiert), `tools/` (Textur-Generatoren, Testrunner, Balancing-Zentrale), `docs/` (Konzepte).

## 3. Harte Regeln des Besitzers
1. **Hauptlinie 26.3 zuerst, Fabric zuerst.** Neue Features zuerst auf 26.3 Fabric fertigstellen und bündeln, dann
   gebündelt auf NeoForge und Forge 26.3 ausrollen (`fabric-263`, `neoforge-263`, `forge-263`), bis der Besitzer
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
   **Aus Vanilla abgeleitete Texturen nur beim Build erzeugen** (Besitzer 12.10.2026): Pixel, die aus dem Client-Jar
   kommen (Umfärbung/Maske/Kopie eines Mojang-Sprites), nie einchecken – auch keine Wiki-Kopien oder Vorschau-PNGs.
   Eintragen in `tools/textures/vanilla_derived.py` (`OUTPUTS`, `images()`), Pfade in `.gitignore`; Gradle
   (`generateVanillaDerived`, `mc26_3/resources.gradle`) und `wiki/generate.py` erzeugen sie (Wiki ohne Jar: Platzhalter).
   Braucht Python 3.8+ mit Pillow + numpy (Windows: `py -3 -m pip install pillow numpy`); fehlt etwas, warnt der Build
   und liefert graue Platzhalter (`-PsbVanillaDerivedStrict` bricht stattdessen ab, `-PsbSkipVanillaDerived` ohne Python).
   Platzhalter bleiben nie still (19.10.): ERROR-Block + Build-Ende-Hinweis, Marker `assets/<ns>/vanilla_derived_placeholders.txt`
   im Jar, Fehlerzeile im Client-Log; Prüfung gebauter Jars: `tools/textures/placeholder_audit.py`. Python-Aufrufe aus
   Gradle nie als `python -c "<Code mit Anführungszeichen>"` (Java maskiert unter Windows keine inneren `"`), sondern
   Skript + Argumente (`vanilla_derived.py --probe`).
   Gleiches gilt für **aus Vanilla abgeleitete Sounds**: nur beim Build (`tools/sounds/vanilla_derived_sounds.py`,
   `gradle/module-vanilla-derived-sounds.gradle`, Howler-Variante B mit `-PsbHowlerSounds=vanilla`), nie einchecken.
   Die 26.4-Jukebox (Snapshot 4) auf 26.2/26.3/1.21.11 kopiert `gradle/jukebox-264.gradle` beim Build aus dem gepinnten
   Snapshot-Jar (piston-meta, SHA-1 geprüft, Gradle-Cache; offline ohne Cache: Vanilla-Jukebox + WARNING), nie auf mc26_4.
8. **Server-Autorität:** alle Gameplay-Stellschrauben serverseitig, mit Obergrenzen, die Vanilla nicht gefährden.
9. Orchestrator committet auf `master`; Worker auf ihrem Arbeitsbranch (kein Push/Merge). Nachricht endet mit `Co-Authored-By: <Assistent> <noreply@anthropic.com>` (bei Claude), sonst passende Zeile.
10. Gegenprobe/Mutationstests **nie** mit `git checkout` zurücksetzen – vorher committen. Nie `find` über das ganze Laufwerk.
11. Vor jedem Löschen/Überschreiben das Ziel ansehen; keine destruktiven Git-Befehle ohne Rückfrage.
12. Testläufe sparsam: Server-Ziele parallel, Client-Tests seriell und nur bei Anzeigeänderungen, Agenten nur Server-Tests.
13. Am Ende eines Runs: Testzentrale neu bauen (`/sbtestcentre build`), Abdeckungstest muss alle Items abdecken.
14. **UI/Tooltips:** Füllstand/Haltbarkeit, die schon als Balken/Optik sichtbar ist, nicht zusätzlich im Tooltip (z. B. Keramik-Eimer, Netherite-Kolben-Item).
    **Tooltip-Regel (Nachtrag 38):** Standard ist nur der Name. Eine Beschreibung ist nur erlaubt, wenn (a) die Bedienung nicht offensichtlich ist (Schleichen + Rechtsklick, Halten, Nebenhand) oder (b) ein Zustand/eine Zahl sonst nirgends sichtbar ist (Stufe, Reichweite, Ladung). Eine Zeile, höchstens ca. 40 Zeichen, höchstens 2 Zeilen je Item; keine Rezepte, keine Herleitungen, keine Werte, die ein Balken zeigt. Erklärungen gehören ins Guide-Buch (`tools/guide_book_pages.py`, `module_guides.py`), ins Wiki oder in die JEI-Info (`jei.*.info.*`). Config-Tooltips der Einstellungsseiten sind davon ausgenommen. Neue Tooltip-Zeilen brauchen EN+DE und laufen durch `tools/check_tooltip_text.py`.,
    Eimer (Kupfer, Enderit, Eisen, Keramik) zeichnen **nie** eine Haltbarkeitslinie: Abnutzung zeigt die Textur (Keramik: heil/angeschlagen/rissig/brüchig), Füllung der Name; der Enderit-Eimer zeigt im Tooltip nur seinen Titel.

15. **Lesbarkeit (Besitzer 16.10.):** Bei jeder UI-Änderung prüfen, dass Text klar lesbar ist – Kontrast zum Hintergrund mindestens 4,5:1, nie dunkler Text mit dunklem Schatten auf Grau. Kontrastwerte im Bericht nennen; vorhandener Test `ui_text_contrast` muss grün bleiben.
16. **Baublöcke & Modulfamilie (Besitzer 16.10.):** Alle Baublöcke gehören zur SimpleBuilding-Familie – aufgeteilt in Sub-Mods unter dem Super-Mod SimpleBuilding: z. B. Simple Octets (kleine Blöcke), Simple Blocks (fehlende Block-Varianten), Simple End (End-Blöcke/-Holz) usw. Nie Baublöcke in fachfremde Mods. **Verfeinert 17.10. (B1):** Simple Blocks enthält alle Baublöcke, die keinen anderen fachlichen Ort haben (Themen-Sub-Mods behalten ihre eigenen; Lager, Maschinen und Werkzeuge bleiben, wo sie sind); Liste: `docs/ai/INVENTAR-BAUBLOECKE-2026-10-17.md`. Blöcke/Teile, die mehrere Module brauchen (z. B. polierter Endstein), liegen in simplelib. Spätere Aufteilung auch in Simple Enderite, Simple Tools, Simple Gadgets – alles bleibt untereinander kompatibel; ist ein Modul geladen (z. B. Enderite), schalten sich die passenden Varianten anderer Module frei (z. B. Enderit-Werkzeuge).
17. **Zwei Sitzungen parallel (Besitzer 16.10.):** Queue-Punkte vor Arbeitsbeginn mit `- [~] (A|B, <branch>, <Datum>)` beanspruchen und sofort pushen; fremd beanspruchte Punkte nicht anfassen; Details `docs/ai/ZWEI-SESSIONS.md`.
18. **Octet-Regel (Besitzer 17.10., O1):** Jeder Block, der Treppen- oder Stufen-Varianten (stairs/slab) hat, bekommt auch einen Octet-Block (Achtelblock) – Vanilla-Blöcke, SimpleBuilding-Blöcke und alle Blöcke, denen Simple Blocks Treppe/Stufe gibt. Das Octet liefert `simpleoctets` (bei Fremdblöcken optionale Integration, nur bei geladenem Besitzer-Mod; jeder Mod bleibt allein spielbar). Neue Treppen/Stufen brauchen im selben Zug den Octet-Eintrag; Stand und Lücken: `docs/ai/INVENTAR-OCTETS-2026-10-17.md`. **Ausnahme (Besitzer 17.10.):** die Naturstufen (Erde, Gras, Sand, Kies) bekommen keine Octets.
19. **Ein Kreativtab für Baublöcke (Besitzer 17.10.):** Simple Blocks und die Baublöcke von SimpleBuilding teilen **einen** Tab „Simple Blocks“; SimpleBuilding allein behält seinen eigenen. Plan: `docs/ai/INVENTAR-BAUBLOECKE-2026-10-17.md`.

## 4. Tests und Gate
- Vollständiges Gate: `./gradlew.bat check -q` (enthält checkBalance, JadeProviderSplitTest, checkAtlases, checkWiki).
- Server-Tests: `python tools/testrunner/run.py --targets fabric-263,neoforge-263 [--filter <muster>]`
  (`--targets all` für alle Linien). **Ein Filtermuster pro Aufruf** – Kommas nutzen still nur das erste.
- Client-Tests: `python tools/testrunner/run.py --targets client` (seriell; Umgebungsvariable `SIMPLEBUILDING_CLIENT_ONLY`
  für einzelne Tests). Nur laufen lassen, wenn keine Besitzer-Clients offen sind.
- Fehlende Texturen/Modelle (lila-schwarzes Karomuster): `python3 tools/check_missing_textures.py` (ohne Gradle; Item-Definitionen und
  Blockstates -> Modelle -> Texturen für alle Namensräume inkl. `modules/*`; `--jar <client.jar>` prüft auch `minecraft:*`; Exit 1 bei ERROR).
  Seit N66 prüft es zusätzlich Partikel-, Equipment-, Sound-Dateien und `"textures/..."`-Pfade im Java-Code.
  **Laufzeit (N66):** `LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s '-screen 0 1920x1080x24 +extension GLX +render -noreset' python3.12 tools/testrunner/run.py --targets audit-models-263`
  (Integrations-Client mit allen Modulen: jedes Mod-Item/jeder Block-State muss ein echtes Modell/Sprite haben, Client-Log ohne
  „Missing textures/model“; Log-Scanner `tools/check_client_log.py <log>`, läuft bei jedem Client-Target als Warnung mit).
  Gefärbte Rucksäcke/Bündel/Köcher: `python3 tools/textures/dyed_storage_preview.py --check` (Vorschau ohne `--check`).
- Texturen prüfen: `python tools/textures/generate_textures.py --check`. Handbücher: `python tools/guide_book_pages.py` (0 Probleme).
- Historische Gate-Zahlen stehen in `docs/HANDOFF.md`; Ziele und Testanzahl ändern sich.
  Für jeden aktuellen Lauf die Ausgabe und „alles gruen“ lesen; alte Zahlen sind kein aktueller Beleg.

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
  26.3 wird standardmäßig mitgebaut (`forge263=true`, abschaltbar mit `-Pforge263=false`); ForgeGradle-7-Runs brauchen zusaetzlich Java 8 fuer Slime Launcher,
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
7. Neues oder geändertes Feature, neues Modul, geänderte Abhängigkeit: `docs/MODS-UND-FEATURES.md` im selben Commit nachziehen
   (Übersicht aller Mods/Sub-Mods; Grundlage für die Entscheidung, wohin ein Feature gehört).

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

Multimod-Datenvertrag: Manifest-Pfade/Abhaengigkeiten vollstaendig halten; Modul-Prosa in
`modules/<id>/wiki/manual.json` (EN/DE). Wiki: `python wiki/generate.py --all` und `--all --check`;
`checkWiki` prueft alle Module. Details und Datagen-Exports: `docs/MULTIMOD.md`.
Balancing-Multimod: Manifest-Pfade/Mod-Auswahl nutzen; Ablage `balance/<id>/`, SimpleBuilding weiter `balance/` (keine Migration). Schreibziele/Datagen nur 26.3; `checkBalance` prueft alle Module. Details/Lesergrenzen: `docs/BALANCING-ZENTRALE.md`.

## 11. AI workflow and continuity (added 2026-09-30)
Voicebridge: `python tools/voicebridge/server.py` (8772), phone setup and safety: `docs/ai/VOICE-HANDS-FREE.md`; default read-only, file proposals require one spoken confirmation, never push/delete/free shell.
Alle Prinzipien an einer Stelle (Teil 1 für alle Mods, Teil 2 je Mod): `docs/ai/PRINZIPIEN.md`.
Modul-Unabhängigkeit (jedes Modul allein spielbar, fremde Inhalte nur bedingt): `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`.
Rückports künftiger Vanilla-Features (je Version prüfen, ob Vanilla es schon hat → dann Mod-Teil aus; bei jedem Port erneut prüfen): `docs/ai/PRINZIPIEN-VANILLA-RUECKPORTS.md`.
Read `docs/ai/WORKFLOW.md` (roles, loop, traps), `docs/ai/LAPTOP-SETUP.md` (new machine) and `docs/ai/VOICE-HANDS-FREE.md` (voice plan). Helper: `python tools/ai/aitool.py` (`codex` = start a worker run in its own worktree, `status`, `gate [--integration] [--push]`, `sync-memory`, `merge-help`). Task briefs and the module-brief generator live in `docs/ai/briefs/`; the assistant's memory notes are mirrored in `docs/ai/memory/` (background facts, the code wins). Multi-mod rules: `docs/MULTIMOD.md`; modules are plugin-style (a new module touches only `modules/<id>/` and its manifest entry).
