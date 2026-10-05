# Cover und neue Config-Ideen (Arbeitsstand 2026-10-05)

## Auftrag und Ist-Zustand

Branch `gpt-coverconf`; nur committen, kein Push/Merge, keine Clients. Cover steht in zwei
gewichteten Loot-Pools. Kein Cover-Eintrag in den Handelsdateien oder Vanilla-Bezugstags
gefunden. Die tatsächlichen geladenen Tags werden zusätzlich im GameTest geprüft.
Crucible-Tempo/Hitze/Warm-Dauer, Sandwich-Zutatenlimit, Linked-Reichweite,
Hängematten-Zeitfaktor und Seelenlava-Weltgen-Chancen sind bereits konfigurierbar.

## Umsetzung

1. Auf 26.3 die zwei Cover-Bucheinträge durch gleich gewichtete Leer-Einträge ersetzen:
   Annahme: andere Beute soll nicht unbeabsichtigt häufiger werden. 26.2 behält Cover
   bis zum Port-Run, gesteuert durch `McVersion.GADGET_REWORK`.
2. Zwölf zusätzliche Ideen in `CONFIG-IDEEN-2026-10-05.md` bewerten. Vier übernehmen:
   Auto-Schmied-Verzögerung (4–100 Ticks), Diamantblock-Ausbeute (1–81 Kiesel),
   Pfeil-Rückgewinnung (0–100 %), Laser-Scan-Intervall (20–100 Ticks, Effekt +10 Ticks).
   Standardwerte unverändert; ServerTuningConfig.validate und begrenzte Laufzeitzugriffe;
   bestehender ConfigOptions-/Netzwerksync. Auf älteren Linien bisherige Werte.
3. EN/DE-Namen und Tooltips in Basis und Overlay; ConfigOptionTests und Verhaltenstests;
   Wiki- und Balance-Ausleser müssen die versionsabhängigen Loot-Einträge korrekt lesen.
4. Datagen ausschließlich 26.3, Wiki --all, LOOT-BALANCE, beide HANDOFFs und Queue pflegen.

## Risiken und Verifikation

Keine neue Survival-Quelle erfinden: nach der Entfernung ist Cover voraussichtlich nur
über Kreativmodus/Befehle erreichbar. Keine höhere Diamantausbeute als der Eingangswert,
keine zusätzlichen Pfeile, keine schnellere Auto-Schmied-Verarbeitung als bisher.
Config-Tests prüfen Grenzen, Standardwerte, Metadaten und tatsächliche Wirkung.
Fabric-/NeoForge-26.3 vollständig, GameTests für Testzentralen-Neubau und Abdeckung;
`check -q` einschließlich checkBalance; 26.2 Fabric/NeoForge und Forge-26.3 kompilieren.
Wiki mit venv --all und uv-Python --all --check. Keine Module müssen geändert werden,
deren vorhandene Optionen bleiben unverändert. Besitzerwelt-Neubau bleibt ohne Client offen.

Die Sandbox blockierte zunächst RTK/rg und den installierten Python-Pfad. RTK wurde
anschließend mit erweiterten Rechten genutzt; Suchen liefen gezielt über Git.
Serena/Context7 waren nicht als MCP-Werkzeuge verfügbar. Quelltexte wurden lokal gelesen.

## Ergebnis und Verifikation (2026-10-05)

- Umsetzung wie geplant, keine Module geändert. Ein weiterer bestehender Loot-Vertrag
  in `WandEnchantmentTests` musste für Option B versionsabhängig angepasst werden.
- Erste gezielte Suite: 36/38 grün, nur der neue 40-Tick-Schmiedetest überschritt das
  bisherige 20-Tick-Testlimit. Budget in beiden Adaptern auf 80 erhöht.
- Erster Volltest: 1952/1954 grün, nur die alte Cover-Erwartung des Baustabtests rot.
  Erwartung für 26.3 korrigiert, auf 26.2 erhalten. Keine Gameplay-Nachbesserung erforderlich.
- Abschließender Volltest, `python tools/testrunner/run.py --targets "fabric-263,neoforge-263"`:
  **`alles gruen: 1954/1954 bestanden, 0 rot`**, je 977/977.
  Datensatz: `testing/runs/2026-10-05T16-44-20Z-7fd9.json`.
  Je acht `test_centre_game_test_*` grün, einschließlich vollständigem Aufbau und
  `every_mod_item_and_block_has_its_place_in_the_test_centre`.
- `.\gradlew.bat check -q -PwikiPython=<venv-Python> :compileJava :neoforge:compileJava
  -Pforge263=true :mc26_3:forge:compileJava`: **`GRADLE_EXIT=0`**, kein `skipWiki`.
  Enthält checkBalance, alle Modul-Datenprüfungen, Wiki, Atlanten und Pflicht-Compiles.
- 26.3-Datagen + syncGenerated263: **`DATAGEN_EXIT=0`**. Genau zwei inhaltlich geänderte
  Inject-Loot-Dateien; keine Datengenerierung anderer Linien.
- Wiki: vorgegebenes venv-Python `--all`, danach uv-Python `--all --check`:
  **`WIKI_GENERATE_EXIT=0`**, **`WIKI_CHECK_EXIT=0`**, `wiki: up to date, everything documented.`
- Wiki-Unit-Suite im Gate: **56 Tests, OK**. Balance-Ausleser: **13 Tests, OK**
  (`python -m unittest discover -s tools/devserver/tests -p test_extract.py`).
- EN/DE Basis und Overlay: **`LANG_DUPLICATES=0`**. Vier neue Wiki-Config-Zeilen mit
  korrekten Grenzen, `side=server`, `reload=no`. `git diff --check` ohne Befund.
- Logs im ignorierten `build/coverconf-*.log`; keine Clients, kein Push/Merge, keine
  Modul-Serverziele nötig. Forge-Serverlauf und ältere GameTest-Linien nicht angefordert
  und nicht gelaufen; deren verlangte Compiles sind grün. Besitzerwelt-Neubau/Sichtabnahme offen.

Annahme bestätigt: auf 26.3 bleibt keine mitgelieferte Survival-Quelle für Cover.
Die Leergewichte erhalten alle anderen Loot-Chancen; bestehende Verzauberungen bleiben
registriert und nutzbar. Vier neue Configs behalten ihre bisherigen Standardwerte.
