# Plan B-COMPAT26 (10.11.2026): Kompat-Lauf Lithium + JEI + Mod Menu (REL-AUDIT 5 / REL13)

Branch b-compat26, Basis hub/b-bundle175-gen. Stand 11.11. (2): **Kompat-Lauf GRÜN (84ae74108), beide Probes grün** – siehe „Teststand 11.11. (2)“.

## Ist-Stand
- Kein Lauf mit Lithium/JEI/Mod Menu je gelaufen (git log 0 Treffer). Vorhanden: Ziel `compat-opt-263` (nur Opt-Rivalen, Muster), `rel10-client-263`, `integration/enabled-mods.json` (alle 61 Module + JEI, Mod Menu, Cloth, Jade ...), `tools/devmods.json` (Maven-Quellen), `gradle/local-devmods.gradle`.
- Lithium stand in devmods.json nur als `local`-Pfad (mc26_3/fabric/run/mods, nicht im Repo) -> Lauf nicht reproduzierbar.

## Umsetzung (fertig)
1. `tools/devmods.json` + `gradle.properties`: Lithium 0.26.3+mc26.3 jetzt per Modrinth-Maven (`maven.modrinth:lithium:${mc263_dev_lithium_fabric}` = fWv5bnZk), wie Jade. Nichts im Repo kopiert. Lizenz: Lithium LGPL-3.0-only, JEI MIT, Mod Menu MIT - nur als Test-Laufzeit geladen, nicht weitergegeben (defaultEnabled bleibt false).
2. `integration/enabled-mods-compat26.json` = enabled-mods.json + devMod `lithium` (alle Simple-Module + JEI + Mod Menu + Cloth + Jade + Mousetweaks + AppleSkin + Lithium).
3. `integration/build.gradle`: Property `-PintegrationMods=<datei>` (Default enabled-mods.json) statt fest verdrahtet; `-PmoduleClientTest=compat26` -> Einstieg `Compat26ClientTest`.
4. `Compat26ClientTest` (integration/src/gametest/...): prueft, dass simplebuilding/lithium/jei/modmenu und >= 20 Simple-Mods geladen sind; Mod-Menu-Liste (`ModsScreen`, per Reflexion) + Config-Screens aller Simple-Mods ueber den modmenu-Entrypoint (erste 4 fotografiert); Welt, 40 Ticks; Block per Spielerklick setzen (stone, chiseled_prismarine, pipe; Server prueft); Inventar; JEI R/U ueber 3 Items (Screen muss `mezz.jei` sein). Log `[compat26] OK|FAIL|FAULT|INFO`, Schlusszeile `done: shots N, faults 0`. Screenshots `compat26-NNN-*`.
5. `tools/testrunner/run.py`: Ziel `compat26-client-263` (opt-in, nicht in TARGETS/all; Feld `compat26`), `compat26_check()`: Schlusszeile faults 0, Lithium/JEI/Mod Menu geladen, Log ohne Mixin-Konflikt-Muster (InjectionError, Critical injection failure, Mixin apply ... failed, MixinApplyError, ...). Unit `tools/testrunner/tests/test_compat26.py`.

## Befehle (NICHT gestartet - nach Entwarnung, einzeln auf sb-test, Branch vorher nach /root/sb.git pushen)
Kompat-Lauf (ein Befehl, schwere Schlange):
`/root/sbt b-compat26 "LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s '-screen 0 1920x1080x24 +extension GLX +render -noreset' python3.12 tools/testrunner/run.py --targets compat26-client-263; echo COMPAT26_RC=$?; mkdir -p /var/tmp/sb/compat26shots; cp integration/run-fabric-263/screenshots/compat26-* /var/tmp/sb/compat26shots/; ls /var/tmp/sb/compat26shots"`
Danach Ergebnis: `/root/t "ls -t /root/sb-runs 2>/dev/null | head -1"` bzw. Log `testing/runs/*-compat26-client-263.log` im Worktree t-b-compat26; Screenshots ansehen (Read auf PNG), Log-Zeilen `[compat26]`.
Wichtig: Danach ist `integration/run-fabric-263/mods` mit Lithium belegt; der naechste normale Integrationslauf synchronisiert neu (sync loescht Fremdjars). Probes darum NICHT direkt nach dem Kompat-Lauf mit Lithium mischen, wenn ohne Lithium gemessen werden soll (zweiter Lauf: `-PintegrationMods` weglassen -> Standardauswahl).

Echte-Maus-Probes (einzeln, nur auf leerem sb-test, Quelle Memory `## b-rel13` / /root/sb-tools-b/b-rel13-cmd2.txt; Probes starten selbst den Integrations-Client):
- Bau-Werkzeuge + JEI: `mkdir -p /var/tmp/sb/rel13mouse; LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s '-screen 0 1920x1080x24 +extension GLX +render -noreset' python3.12 modules/simplebuildingtools/tools/mouse_probe.py --out /var/tmp/sb/rel13mouse; echo MOUSEPROBE_RC=$?` (Ergebnis `/var/tmp/sb/rel13mouse/results.json`)
- Tastenbelegung: `mkdir -p /var/tmp/sb/rel13keys; LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s '-screen 0 1920x1080x24 +extension GLX +render -noreset' python3.12 modules/simpleinterfaces/tools/keys_probe.py --out /var/tmp/sb/rel13keys --scales 2,3; echo KEYSPROBE_RC=$?`
- Vorher pruefen, dass nichts anderes laeuft: `ps -eo args | grep -c 'heavy.sh job'` und `gradle-wrapper.jar` (cmd2-Muster: nur starten, wenn beide 0). Beide Probes nacheinander, nie parallel zum Kompat-Lauf.
Reihenfolge: Kompat-Lauf, mouse_probe, keys_probe (je eigener Befehl, nie parallel).

## Lokal geprueft
- `python3.12 tools/multimod.py` (Registries valide), `python3.12 -m unittest discover -s tools/testrunner/tests` (test_compat26 3/3), `run.py --list` zeigt Ziel; Groovy-Aenderung statisch gelesen (Variable `selectionFile` vor erster Nutzung definiert; 4 Ersetzungen). Java des Tests: nicht kompiliert (kein Gradle erlaubt) - Mod Menu/JEI nur per Reflexion, Rest 1:1 aus Rel10ClientTest.

## Teststand: offen - sb-test hing
Offen: Kompat-Lauf, beide Probes, ggf. Compile-Fehler im Test, Pins (Lithium 0.26.3 fWv5bnZk geprueft am 10.11. gegen Modrinth-API; JEI 31.7.0.37 und Mod Menu 21.0.0 aus gradle.properties). Keine Rueckfragen.

## Teststand 11.11.
- Merge hub/b-bundle179-gen (089fdc551) → 361b89a8a, konfliktfrei. Abweichung: `integration/enabled-mods-compat26.json` neu aus enabled-mods.json + `lithium` erzeugt (neu: simplevault) → 8167a3593.
- Lokal: `test_compat26` 3/3 OK.
- sbt 8167a3593: `./gradlew :integration:compileGametestJava -PmoduleClientTest=compat26 -PintegrationMods=integration/enabled-mods-compat26.json -q` → **COMPILE_RC=0** (nur Deprecation-Warnungen, Compat26ClientTest kompiliert fehlerfrei; Log /root/sb-tools-b/b-compat26-sbt1.log). Einen separaten Server-/Gametest-Teil hat der Plan nicht.
- Client-Lauf `compat26-client-263`, mouse_probe, keys_probe: **nicht gestartet**. sb-test war 07:29–09:29 UTC (12 Prüfungen im 10-min-Takt, Log /root/sb-tools-b/b-compat26-wait1.log) nie leer: Gates 089fdc551/7def212d6 + B-sbt-Läufe. Client-Läufe genutzt: 0 von 3.
- Screenshots: keine (Zielpfad nach dem Lauf /var/tmp/sb/compat26shots auf sb-test). Gefundene Konflikte: keine bisher (ohne Client-Lauf nicht prüfbar).
- Nächster Schritt: bei leerem sb-test die drei Befehle oben einzeln nacheinander starten.

## Teststand 11.11. (2)
Client-Läufe genutzt: 2 von 2 (je bei leerem sb-test, Skripte /root/sb-tools-b/compat26-client/run.sh, run2.sh).
- **Lauf 1** (10:40, ee34fd12f, Datensatz testing/runs/2026-11-11T10-40-37Z-2df4): COMPAT26_RC=1, `faults 18`: `modmenu-list` und alle 17 `modmenu-config-*` mit InvocationTargetException. Alles andere OK (Mods geladen, Welt, 3 Blöcke per Klick, Inventar, JEI R/U für 3 Items → mezz.jei).
- **Ursache:** Fabric client gametest (`fabric-client-gametest-api-v1`, `threading.MinecraftMixin`) verbietet `Minecraft.getInstance()` im Test-Thread; `Screen.<init>` ruft es auf. Der Test baute ModsScreen und die Config-Screens per Reflexion im Test-Thread. **Test-Fehler, kein Mod-Fehler** (kein Mod geändert).
- **Fix 84ae74108** (Compat26ClientTest): Screens über `context.setScreen(() -> …)` auf dem Client-Thread bauen; angezeigte Screen-Klasse wird gelesen; `guard` loggt jetzt Wurzelursache + Stacktrace. run.sh: Screenshot-Kopie auf Muster `*compat26-*` (Dateien heißen `NNNN_compat26-…`, alte Kopie griff nicht).
- **Lauf 2** (10:55, 84ae74108): **COMPAT26_RC=0**, `done: shots 14, faults 0`.
  - Mods: 61 Simple-Mods + simplebuilding, lithium, jei, modmenu geladen.
  - Mod-Menu-Liste: öffnet (72 Mods + 1 Library).
  - Config-Screens: 17 Simple-Mods bieten einen modmenu-Entrypoint, alle öffnen als `ClothConfigScreen` (4 fotografiert).
  - Blöcke per Spielerklick: stone, simpleblocks:chiseled_prismarine, simplestorage:pipe – OK.
  - JEI R/U: oak_log, simpletools:strip_station, simplebuilding:reinforced_hopper – je mezz.jei-Fenster.
  - Mixin-Konflikte: keine (Muster InjectionError/Critical injection/MixinApplyError 0 Treffer; Lithium 170 Optionen, 0 Overrides). Einzige Mixin-WARN: fehlende Klasse `net/xolt/freecam/config/model/ModConfigImpl` (optionale Fremd-Mixin-Ziel, harmlos). Sonst nur Umgebungsfehler (OpenGL-Backend, Narrator/flite, Realms offline).
- **Probes** (nach Lauf 1, Lithium im mods-Ordner): mouse_probe **53/53 OK** (MOUSEPROBE_RC=0, sb-test /var/tmp/sb/rel13mouse/results.json), keys_probe **60/60 OK** (KEYSPROBE_RC=0, /var/tmp/sb/rel13keys/results.json). Nicht neu gelaufen nach dem Test-Fix (Fix betrifft nur Compat26ClientTest).
- Kopien auf sb-agent: /root/sb-tools-b/compat26-client/shots1, shots2 (Logs, Screenshots, results.json). Vorschau /root/sb-tools-b/previews/b-compat26-preview.html.
- Hinweis (nicht Teil des Auftrags): nur 17 von 61 Simple-Mods haben einen Mod-Menu-Config-Entrypoint.
