# Plan b-texfix20 (19.10.): graue Schachmuster statt Texturen im Client des Besitzers

Queue: „Besitzer 19.10. abends – Nachtrag“, Punkt 1 (Bilder sb-agent `/root/b-bilder-1910/12–14.png`).

## Ist-Zustand / Befund

- Das Schachmuster in Bild 12–14 ist **grau ohne Tönung** (zwei Töne 0x8A8A8A/0x454545, 8×8 Felder): das ist der
  Groovy-Platzhalter aus `mc26_3/resources.gradle` (`sbVanillaDerivedPlaceholders`), nicht der getönte Python-Platzhalter
  (`vanilla_derived.placeholder_png`) und nicht Minecrafts Magenta-„missing“. Betroffen sind genau Einträge aus
  `vanilla_derived.OUTPUTS` (Nihil-/Astral-Gewölbe, Kolben, Schienen, Kisten-/Trichterloren, Kistenboot-Overlays,
  Lohenbuch, Lautsprecher, Color Brush, Silent Dandelion, Musikplatten).
- **Gleichzeitig echt** im selben Bild: Ofenloren (Modul simpleovens) und Kupfer-/Enderit-Eimer (simplecrucibles) – auch
  sie sind vanilla-derived, kommen aber aus `gradle/module-vanilla-derived.gradle`. Python, Pillow, numpy und das
  26.3-Client-Jar waren beim Besitzer also da; nur der SimpleBuilding-Lauf fiel auf Platzhalter zurück.
- Unterschied der beiden Wege: SB prüft Python mit `python -c <Probe>`, und die Probe (`sbVanillaDerivedProbe`, seit
  33bdf7912 am 12.10.) enthält **doppelte Anführungszeichen** (`("PIL", "numpy")`, `print("SBPY", …)`). Java baut unter
  Windows die Kommandozeile selbst (`ProcessImpl.createCommandLine`): ein Argument mit Leerzeichen wird in `"…"`
  gesetzt, innere `"` werden **nicht** maskiert; die C-Laufzeit von python.exe/py.exe wirft sie beim Zerlegen weg →
  Python bekommt `for m in (PIL, numpy)` / `print(SBPY, …)` → NameError/SyntaxError → „does not run“ für jeden
  Kandidaten → Fallback mit grauen Platzhaltern, nur eine WARNING im Gradle-Log. Die Modul-Probe `import PIL, numpy`
  hat keine `"` und geht. Unter Linux (sb-test, Gate, dist) tritt das nie auf → Gate-/dist-Jars sind echt
  (`/root/dist/files/*` geprüft: 0 Platzhalter in 89 Jars).
- Weitere Windows-Schwächen: Starter setzt `SB_PYTHON` auf irgendein `python` im PATH (ungeprüft, ggf. der
  Microsoft-Store-Alias oder ein Python ohne Pillow), und ein gesetztes `SB_PYTHON` schaltet die automatische Suche ganz
  ab. Die Warnung geht im Gradle-Log unter; nichts sagt dem Besitzer im Spiel oder am Build-Ende Bescheid.
- Nebenbefund Windows: seit 17.10. ist `C:/Users/o_o/code/minecraft-mods/previews/modul-buecher-vorschau.png` im Repo
  (Standard-Vorschaupfad von `module_guide_books_2026_10_05.py`, unter Linux relativ angelegt und mitcommittet). Ein
  Pfad mit `C:` lässt sich unter Windows nicht auschecken (`invalid path`) → der Starter-Checkout von wave bricht ab.
- Echte fehlende Texturen/Modelle (nicht vanilla-derived): `tools/check_missing_textures.py --jar <26.3-Client>`
  → 1407 Item-Definitionen, 888 Blockstates, 5392 Modelle, **0 Fehler**. Musikplatten sind SB-Items (OUTPUTS), keine
  Sub-Mod-Items.

## Änderungen

1. **Probe ohne Inline-Code** (Ursache): `vanilla_derived.py --probe` gibt `SBPY <ver> <fehlende Module>` aus; beide
   Gradle-Wege (`mc26_3/resources.gradle`, `gradle/module-vanilla-derived.gradle`) rufen das Skript statt `-c` auf.
   Ein gesetztes `-PsbPython`/`SB_PYTHON` wird zuerst probiert, danach die automatische Suche (statt exklusiv).
2. **Jar-Suche robuster**: zusätzlich der offizielle Launcher (`%APPDATA%\.minecraft\versions\26.3\26.3.jar`,
   `~/.minecraft/…`).
3. **Platzhalter nie still**: Fallback schreibt zusätzlich `assets/<ns>/vanilla_derived_placeholders.txt` (Grund) ins
   Jar; Gradle loggt einen ERROR-Block im Task und noch einmal am Build-Ende (geteilter BuildService im Root-Build);
   SimpleBuilding loggt beim Client-Start einen ERROR, wenn die Marker-Datei im Jar liegt.
4. **Starter** (`tools/zentrale-web/starter/starter.ps1`): Python nur nach Prüfung (`import PIL, numpy`) als
   `SB_PYTHON` setzen; fehlen Pillow/numpy bei einem vorhandenen Python → einmal `pip install --user pillow numpy`;
   erscheint im Gradle-Output die Platzhalter-Warnung → Hinweis-Fenster (nicht blockierend). Der Starter aktualisiert
   sich beim nächsten Holen selbst aus dem Stand.
5. **Windows-Pfad**: `C:/…png` aus dem Repo, Vorschau-Standardpfade der drei Generatoren nach `build/previews/`,
   `/C:/` in `.gitignore`; Test, dass kein getrackter Pfad unter Windows ungültig ist.
6. **Audit-Werkzeug** `tools/textures/placeholder_audit.py`: liest gebaute Jars (oder Ordner) + Client-Jar, löst je
   Kreativtab-Eintrag Item-Definition → Modelle → Texturen auf, stuft jede Textur als ok / Platzhalter / fehlt ein,
   rendert Item-Icons offline (`wiki/model_render.py`) zu einem Kontaktbogen je Tab und schreibt eine HTML-Seite;
   `--fail-on-placeholder` (Exit 1) für Release-Prüfungen. Tab-Inhalte kommen aus einem Dump des
   Integrations-Gametests (`CrossModGameTest#everyModuleItemIsInSomeCreativeTab` schreibt `SB_CREATIVE_TAB_DUMP`,
   wenn gesetzt); ohne Dump gruppiert das Werkzeug nach Namensraum. Die HTML-Seite enthält Mojang-Pixel → **nicht
   ins Repo**, nur als Artifact; ins Repo kommt der Text-Bericht `docs/ai/AUDIT-PLATZHALTER-2026-10-19.md`.
7. **Tests**: `tools/textures/tests` (Detektor, `--probe`, Windows-Kommandozeilen-Rundreise der Gradle-Argumente nach
   Java-/CRT-Regeln, Repo-Pfade Windows-tauglich, Marker) als Gradle-Task `testTextureTools` in `check`.

## Nicht im Repo (Vorschläge in den Bericht)

- `/root/dist-publish.sh` (sb-test): `-PsbVanillaDerivedStrict` an den assemble-Aufruf, danach
  `python3.12 tools/textures/placeholder_audit.py --jars <gebaute Jars> --fail-on-placeholder` vor dem Veröffentlichen.
- `/setup/` auf sb-web: `starter.ps1` aus diesem Stand neu ausliefern (der installierte Starter aktualisiert sich sonst
  erst nach einem erfolgreichen Holen).

## Verifikation

- `python3.12 -m unittest discover -s tools/textures/tests`; `./gradlew check -q` auf sb-test.
- Build auf sb-test: `:mc26_3:fabric:generateVanillaDerived` echt; mit `-PsbPython=/nonexistent` → Auto-Suche greift;
  mit `-PsbSkipVanillaDerived` → Marker im Jar, ERROR-Block, Audit meldet Platzhalter (Exit 1).
- Audit gegen die gebauten 26.3-Jars (echt → 0 Platzhalter) + Tab-Dump aus `integration-263` (ein Lauf, gefiltert).
- Nicht prüfbar hier: echter Windows-Lauf (Probe/Starter) – nur per Simulation (Test) belegt; Client-Sicht beim Besitzer.
