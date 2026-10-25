# Plan b-earthcmd25 (24.10.2026): Config-Befehle fuer Simple Earth (CMD24)

NUR 26.3 Fabric. simpleearth haengt schon von simplelib ab (fabric.mod.json), kein Build-Eingriff noetig.
1. `SimpleEarth.init`: `ConfigCommands.server(MOD_ID).json(EarthServerConfig::toJson, ::apply).file(dir, FILE)` -> `/simpleearth config`.
2. `EarthClient.init`: `ConfigCommands.client(MOD_ID)` mit EarthClientConfig -> `/simpleclient simpleearth`.
3. (entfaellt, s.u.)
4. `tools/check_config_commands.py`: simpleearth aus PENDING.
5. Gametest `config_command` (+ Fabric-Adapter): angekuendigt, set/clamp/reset/Rechte via `ConfigCommandTests.roundTrip`.
Verifikation: check_config_commands (0 problems), Compile Fabric 26.3 + :modules:simpleearth:fabric, Targets module-simpleearth-(standalone-)fabric-263, Generatoren, `./gradlew check -q`.
Abweichung: Lang-Seite 23 entfaellt (module_guides.py erzeugt die Seiten aus eigener Quelle). Offen: Client-Befehl ohne Client-Lauf nicht testbar.
