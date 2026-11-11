# PLAN b-server26 (10.11.) – prio26 #5, REL-CHK §4: Dedicated Server je Mod allein + Vanilla-Client-Beitritt

Basis hub/b-cloth26 (080c1eef6; Cloth optional in QoL/Dimensions/Fun/Money/Riding = Voraussetzung).

## Ist-Stand
- tools/testrunner: kein Ziel „Dedicated Server“ (grep dedicated 0). Nächste Verwandte: `module-<mod>-standalone-fabric-263`
  (integration/build.gradle, Loom-Server-Lauf im GameTest-Modus mit Modul-Jars + devMods aus modules.json – nur für Mods
  mit `tests.standalone`, Abhängigkeiten aus modules.json statt aus fabric.mod.json, Dev-Klassenpfad mit Fabric API).
- tools/bench/run_bench.py: Muster „Release-Jars per `:<proj>:jar` bauen und in einen mods/-Ordner kopieren“.
- Gradle-Cache hat alles Nötige: `~/.gradle/caches/fabric-loom/26.3/minecraft-server.jar` (Vanilla-Server),
  `fabric-api-0.161.0+26.3.jar`, Cloth unter me.shedaniel.cloth. Fabric-Server-Launcher: meta.fabricmc.net (181 KB).

## Umsetzung (fertig)
1. **tools/testrunner/server_solo.py** (neu, nur Python):
   - Abhängigkeiten je Mod aus `fabric.mod.json` `depends` (rekursiv; SB-Super-Mod: Build-Umschreibung aus modules.json
     nachgebildet = alle Pflicht-Sub-Mods + cloth-config). Eigene Mods → Release-Jar, `fabric-*` → Fabric-API-Jar,
     externe Libs nur über `tools/devmods.json` `loaderIds.fabric` (heute nur Cloth, nur bei SB hart). Unbekannte harte
     Abhängigkeit = ROT. Keine Client-Mods, kein :common/:framework, kein Dev-Klassenpfad.
   - Echter Server: Fabric-Server-Launcher + Vanilla-server.jar (aus Loom-Cache, sonst lädt der Launcher), gemeinsamer
     Ordner `testing/server-solo/server` (Bibliotheken/entpackter Bundler bleiben zwischen Mods; mods/config/world/logs
     je Mod frisch). `-Xmx1G -XX:+UseSerialGC` (Env `SIMPLEBUILDING_SERVER_SOLO_XMX`), Flachwelt, Sichtweite 3,
     offline-mode, freier Port nur auf 127.0.0.1.
   - Ablauf je Mod (Schritte = Testzeilen): `mods_folder` → `boot` („Done (“, Grenze 300 s) → `ticks` (5 s) →
     `status_ping` (Server-List-Ping wie jeder Vanilla-Client) → `vanilla_join` (minimaler Vanilla-Protokoll-Client:
     Handshake, Login offline, Kompression, Login-Query „nicht verstanden“, Known Packs nur `minecraft:*`, Keep-Alive,
     Ping/Pong, Code of Conduct, Finish → erstes Play-Paket; zweiter Zeuge: Server-Log „SoloProbe joined the game“ bzw.
     „lost connection: <Grund>“) → `stop` (stdin, Exit 0 in 90 s) → `log_clean` (kein `/ERROR]`, `Exception`,
     `crash`/Crash-Report; `fabric-crash-report-info-v1` ausgenommen).
   - Paket-IDs aus dem 26.3-Jar ermittelt (`javap` auf ConfigurationProtocols/LoginProtocols, addPacket-Reihenfolge):
     Config CB finish 0x03, keep_alive 0x04, ping 0x05, select_known_packs 0x0F, code_of_conduct 0x14;
     SB finish 0x03, keep_alive 0x04, pong 0x05, select_known_packs 0x07, accept_coc 0x09; Login wie bisher (0x00–0x05).
2. **run.py**: `SERVER_SOLO_TARGETS` (`kind="dedicated"`), je Mod `server-solo-<modul>-fabric-263` (60 Mods, ohne
   wiringexample), Gruppe `--targets server-solo` (alle nacheinander), ein Gradle-Aufruf baut vorher alle nötigen
   Jars. Nie im Standardlauf und nicht in `everything-plus-snapshot`. Parallel-Pfad nimmt nur noch `kind == "server"`.
3. Unit-Tests `tools/testrunner/tests/test_server_solo.py` (Fake-Server: Status, Login mit Kompression/Query/Known
   Packs/Keep-Alive bis Play, Abweisung; Abhängigkeits-Hülle: Cloth nur bei SB; Log-Filter). `.gitignore`:
   `testing/server-solo/`.

## Entscheidungen
- **Produktionsnah statt Loom-Dev-Lauf:** echte Release-Jars + echter Launcher prüfen genau das, was Spieler/Server-
  Admins installieren (Dev-Klassenpfad würde fehlendes Bündeln verdecken). Gradle nur für `:jar`.
- **Vanilla-Beitritt wird aufgezeichnet, nicht verlangt:** Mods mit eigenen Blöcken/Items werden einen Client ohne
  Mod (Fabric-API-Registry-Sync) erwartbar abweisen. Abweisung = Status `known` + Warnung mit Grund; ROT nur bei
  Absturz/Exception im Log oder Protokollfehler. `VANILLA_JOIN_REQUIRED` (server_solo.py) ist leer.
  **Folgepunkt (Besitzer-Rückfrage):** welche Mods müssen Vanilla-Clients reinlassen (reine Server-Mods wie
  Simple Opt Server/Mob Manners/Weather?) → nach dem ersten Lauf aus den `known`-Zeilen vorschlagen und eintragen.
- SimpleBuilding bekommt Cloth (bei ihm hart, siehe PLAN-B-CLOTH26 „Entscheidungen“); alle anderen ohne Cloth –
  Test `test_cloth_only_where_declared_hard`.
- Log-Filter streng; falsche Treffer nach dem ersten echten Lauf in `LOG_FAULT_IGNORED` mit Begründung.

## Lokal geprüft (10.11., sb-agent, Regel 16)
- `python3 -m unittest discover -s tools/testrunner/tests` 17/17 OK; launchhub test_hub 27/27 OK; py_compile 3.12 OK.
- `python3 tools/testrunner/server_solo.py --list` (Hülle je Mod, keine unbekannte Abhängigkeit),
  `run.py --list` 60 server-solo-Ziele; `execute()` mit gestubbtem Lauf: grün/known/rot richtig im Datensatz/Tabelle.
- precheck: 86/87 grün; einzig ROT `wiki/generate.py --all --check` = Pillow auf sb-agent ohne `Image.get_flattened_data` (Umgebung, nicht dieser Branch); `--fast` grün.
- NICHT geprüft: echter Serverstart (kein sb-test). Risiken: Launcher-Download (Netz auf sb-test), falsche Log-Treffer,
  Paket-ID-Annahmen (durch javap belegt, aber nicht gegen echten Server gelaufen).

## Teststand: offen – sb-test hing
Ein sbt-Lauf nach Entwarnung (erst alle Mods einzeln = Gruppe; Schätzung: Jar-Bau aller 60 Module 5–10 min, erster
Start +1–2 min Launcher/Bibliotheken, dann je Mod ~40–60 s → **gesamt ca. 50–70 min**, RAM ≤ 1,3 GB):

    /root/sbt b-server26 "python3.12 -m unittest discover -s tools/testrunner/tests; echo UNIT_RC=\$?; SIMPLEBUILDING_SERIAL_TESTS=1 python3.12 tools/testrunner/run.py --targets server-solo; echo SOLO_RC=\$?"

Auswertung: `testing/runs/<lauf>-server-solo-<mod>-fabric-263.log` (Serverlog) + Datensatz-JSON; erwartete Ergebnisse:
alle `boot/stop/log_clean` grün, `vanilla_join` meist `known` (Abweisung). Rot → einzeln nachlaufen:
`run.py --targets server-solo-<mod>-fabric-263`.

## Erster echter Lauf 11.11. (Datensatz 2026-11-11T01-34-23Z-b8c3) – ROT 300/420, Ursachen + Fix
Rot waren ausschließlich die 60 `log_clean`-Schritte; alle anderen Schritte grün bzw. `vanilla_join` = known.
1. **„[Worker-Main-N/ERROR]: No key layers in MapLike[{}]“** bei allen 60 Mods direkt nach „No existing world data,
   creating new world“ – **Harness-Fehler**: `level-type=minecraft:flat` ohne `generator-settings`; 26.3 schreibt
   `generator-settings={}` und der Flat-Codec verlangt `layers`. Fix: `generator-settings` mit den Schichten aus
   `data/minecraft/worldgen/world_preset/flat.json` (Grundgestein/Erde/Gras, Ebene). Kein Mod-Fehler.
2. **„You are not white-listed on this server!“** – **Harness-Fehler**: frische 26.3-`server.properties` hat
   `white-list=true` als Standard. Fix: `white-list=false`, `enforce-whitelist=false` explizit.
   Damit prüft `vanilla_join` erst jetzt die eigentliche Frage (lässt der Mod Vanilla rein?).
   `VANILLA_JOIN_REQUIRED` bleibt leer – Rückfrage an den Besitzer offen.
3. Unit-Test `test_properties_no_whitelist_and_flat_layers` (neue Funktion `properties_text`).
   Abweichung: Unit-Tests liegen in `tools/testrunner/tests/` (`discover -s tools/testrunner` findet 0 Tests),
   daher im sbt `-s tools/testrunner/tests`.

## Zweiter Lauf 11.11. (47ff8b97d, Datensatz 2026-11-11T04-22-09Z-af38) – GRÜN 360/420, 0 rot
UNIT_RC=0, SOLO_RC=0; die 60 nicht grünen Schritte sind alle `vanilla_join` = known. Kein ERROR mehr im Log.
**Vanilla-Join-Ergebnis:** alle 60 Mods weisen den Vanilla-Client ab, einheitlich durch Fabric API Registry Sync
(„This server requires Fabric Loader and Fabric API installed on your client!“ + Namensräume). `simplelib` steht in
jeder Liste – SimpleLib registriert selbst Blöcke/Items/Kreativ-Tab/Datenkomponenten (`Registry.register` in
simplelib/shared), daher lässt **kein** Mod Vanilla rein, auch reine Logik-Mods (simpleearth, simplefarming,
simplemovement, simplesounds, simplevisuals, simpleweather, simpleopt*, simplelivemap, simpleloot,
simpleinterfaces, simplechallenges) nicht. Soll ein Mod Vanilla-Clients reinlassen, müsste SimpleLib ohne
synchronisierte Registry-Einträge laufen (bzw. Einträge optional). Rückfrage Besitzer bleibt offen.
Nachbesserung: Abweisungsgrund wurde nicht erfasst (26.3 loggt „SoloProbe (<uuid>) lost connection“, mehrzeilig) →
`reject_reason()` + Unit-Test mit der echten Logzeile; nur Log-Auswertung, kein dritter sb-test-Lauf.
