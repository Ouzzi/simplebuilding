# Plan B-CMDCFG24 (24.10.): Befehle aufräumen, zu jeder Config ein Befehl

Branch `b-cmdcfg24` (Basis claude-wave1 b74998032), Queue-Abschnitt „Befehle & Config 24.10. (CMD24)“.
Besitzer-Auftrag: „Die Befehle einmal überarbeiten, dass alles sauber ist. Zu den Konfigurationen auch, zu jeder
Konfiguration auch ein Befehl.“ Nur 26.3 Fabric (Besitzer 22.10.).

## 1. Ist-Zustand

### 1.1 Vorhandene Konvention
- **Kern** `/simplebuilding config list [filter] | get <option> | set <option> <wert> | reset <option>`
  (`common/src/shared/java/com/simplebuilding/command/SimplebuildingCommand.java`, Config-Umbau 28.09.): jede Option
  der `simplebuilding.json` per Pfad (`tweaks.pads.enableFlypads`), Tab-Vervollständigung für Optionen/Gruppen/Werte,
  Typprüfung, Grenzen wie beim Laden, Speichern, Hinweise „wirkt nach Neuladen/Neustart“. **Vollständig** – bleibt.
- **Module**: keine gemeinsame Config-API. Jede Config-Klasse ist handgeschrieben, aber fast alle haben eine von zwei
  Formen: (a) statische Felder + `apply(JsonObject)` (begrenzt jeden Wert) + `toJson()`; (b) Gson-POJO mit
  `normalize()` und eigenem `save()`. Befehle gibt es nur für einzelne Schalter (z. B. `/simplefarming hoeHarvest`).
- simplelib hat nichts Gemeinsames für Befehle/Config (nur `framework/api/ConfigFiles`: atomar schreiben, Quarantäne).

### 1.2 Befehle (Brigadier, 26.3 Fabric; ohne Tests/NeoForge/Forge)

| Wurzel | Modul | Rechte | Unterbefehle | Lang/Feedback | Probleme |
|---|---|---|---|---|---|
| `/simplebuilding` | Kern | Op-Spieler (`OPERATOR_ONLY`) | config list/get/set/reset, setTrimMultiplier, getTrimMultiplier, chunkloaders list/remove, tweaks … | translatable | keine (Referenz) |
| `/killboats`, `/killcarts` | Kern (Tweaks) | Op-Spieler | – | translatable | – |
| `/sbtestcentre` | Kern (Dev) | 2 | build, coverage, fill, give, help, here, kit, section, stats, tp | Dev-Werkzeug | bewusst englisch/Dev, nicht angefasst |
| `/simplefun` | simplefun | 3 | tweaks yeet/bricks/breakGlass/snowballDamage …, pvp headDrops (Alias aus mobheads) | translatable | Rechte 3 statt 2; keine Config-Befehle für alle Optionen (kein simplelib) |
| `/simplemobheads` | simplemobheads | 3 | pvp headDrops, drops creeper/wither/warden/playerKill/playerKillChance/lootingBonus, greeting, enabled, ability | translatable, Tab für ability | Rechte 3 statt 2; nur 11 von ~27 Optionen |
| `/simplefarming` | simplefarming | 4 | farmlandProtect, hoeHarvest, sharpnessCut | translatable | Rechte 4; nicht alle Optionen; kein simplelib |
| `/simpleloot vaults` | simpleloot | 4 | cooldown, repeat | translatable | Rechte 4; kein simplelib |
| `/simplemobmanners` | simplemobmanners | 4 | Listen add/remove/clear/list, Schalter | translatable, Tab | Rechte 4; kein simplelib |
| `/simplemovement`, `/crawl` | simplemovement | 4 / alle | autowalk, frostWalkerSnow, jumpBoostStep, ladderSpeed | translatable | Rechte 4; kein simplelib |
| `/simpletools` | simpletools | 4 | Schalter | translatable | Rechte 4; nicht alle Optionen |
| `/simplequalityoflife submod` | simplequalityoflife | 4 | Sub-Mod-Schalter | translatable | Rechte 4 |
| `/simplemaps web …` | simplemaps | 2 | export, share/unshare, show/hide, status, waypoints, full | translatable | keine Config-Befehle |
| `/simplemaps picture` (Client) | simplemaps | Client | picture | – | ok |
| `/music` | simplemusic | 2 | list, pending, info, ban, unban | translatable (+ Datenzeile literal) | Wurzel heißt nicht `simplemusic` (bleibt, Alias nicht nötig); keine Config-Befehle |
| `/simplemodels reload` | simplemodels | 2 | reload | **keine Rückmeldung** | Rückmeldung fehlt |
| `/simpleopt status` | simpleoptserver | 2 | status | translatable | – |
| `/simplerespawn` | simplerespawn | alle / 2 | giveup, down, revive | translatable | keine Config-Befehle |
| `/servershop` | simpleshops | 2 | create, add, remove, price, list, items, cash, clear, coords, goods, orphans | translatable (+ Datenzeilen literal) | keine Config-Befehle |
| `/simplestats` | simplestats | 2 | show, reset, reload | translatable | – |
| `/simplesounds guide` (Client) | simplesounds | Client | guide | – | ok |
| `/simplevisuals config visuals enableAnvilFormatting` | simplevisuals | 3 | eine Server-Option | translatable | Rechte 3 statt 2 |
| `/simplevisuals …` (Client) | simplevisuals | Client | jede Client-Option (eigener Baum `LocalCommands`), elytraHelper, guide | Wert als literal | schon vollständig, eigener Baum (bleibt) |
| `/claim …` | simpletweaks | alle / 4 (admin) | create, info, list, trust, untrust, unclaim, buy, credits, budget, code, admin, listall | translatable | – |
| `/tweaks help`, `/tweaks border/cleanup/event/restart/protect/deaths`, `/pvp`, `/afk`, `/coords`, `/seen`, `/playtime`, `/leaderboard`, `/vote`, `/phantoms`, `/pregen`, `/backup`, `/lag`, `/maintenance`, `/vanish`, `/freeze`, `/unfreeze`, `/invsee`, `/endersee`, `/inspect` | simpletweaks | je Config-Feld `permission` (`PlayPerms`/`AdminPerms`) | … | `PlayText.t`/translatable (literal nur für Daten) | sauber; Configs sind Records in mehreren Dateien → Config-Befehl Teilstand |

Befund: Lang ist fast überall sauber (`Component.literal` nur für Datenzeilen: IDs, Koordinaten, Zahlen, Trenner).
Echte Probleme: (1) **uneinheitliche Rechte** für Config-Änderungen (2/3/4), (2) **fehlende Config-Befehle** für die
meisten Optionen, (3) `/simplemodels reload` ohne Rückmeldung, (4) keine Hilfe zu den Config-Befehlen.

### 1.3 Configs (26.3, Server = S, Client = C)

| Modul | Datei | Seite | Form | simplelib gebündelt | Stand nach diesem Branch |
|---|---|---|---|---|---|
| Kern | simplebuilding.json | S+C | ConfigOptions | – | schon `/simplebuilding config` |
| simplelib | simplelib-server.json | S | static apply/toJson (+ fremde Schlüssel `kept`) | selbst | **neu** `/simplelib config` (nur eigene Schlüssel) |
| simplelib | simplelib-client.json | C | UiBrightness | selbst | **neu** `/simpleclient simplelib` |
| simplebeacons | simplebeacons-server.json | S | static | ja | **neu** |
| simplecrucibles | simplecrucibles-server.json | S | static | ja | **neu** |
| simplehammers | simplehammers-server.json | S | static | ja | **neu** |
| simplemaps | simplemaps-server.json | S | static | ja | **neu** |
| simplemobs | simplemobs-server.json / -client.json | S / C | static | ja | **neu** (beide) |
| simplemusic | simplemusic-server.json / -client.json | S / C | static | ja | **neu** (beide) |
| simpleovens | simpleovens-server.json | S | static | ja | **neu** |
| simplerespawn | simplerespawn.json | S | static | ja | **neu** |
| simplesandwiches | simplesandwiches-server.json | S | static | ja | **neu** |
| simpleshops | simpleshops-server.json | S | static | ja | **neu** |
| simplestorage | simplestorage.json | S | static | ja | **neu** |
| simpletrims | simpletrims-server.json | S | static | ja | **neu** |
| simplemobheads | simplemobheads.json | S | POJO | ja | **neu** |
| simpleriding | simpleriding.json | S | POJO | ja | **neu** |
| simpletools | simpletools.json | S | POJO + save | ja | **neu** |
| simpleinterfaces | simpleinterfaces-server.json | S | POJO + save | ja | **neu** |
| simplequestionary | simplequestionary.json | S | static (1 Feld) | ja | **neu** |
| simplevisuals | simplevisuals (Cloth) | S+C | eigener Baum | ja | schon vollständig (Client-Baum + Server-Option) |
| simpletweaks | simpletweaks-*.json (Records, mehrere Dateien) | S | Records | ja | Teilstand |
| simplemodels, simplestats | – (keine Optionen) | | | | – |
| simplefarming, simpleloot, simplemobmanners, simplemovement, simpleweather | eigene .json (POJO) | S | POJO + save | **nein** | Teilstand (Abhängigkeit) |
| simplefun, simplemoney, simpledimensions | Cloth/eigene | S/C | | nein | Teilstand (Abhängigkeit) |
| simpleend, simpleenddragonfight, simplewanderers, simpleaesthetics, simpleopt* | eigene .json | S/C | static/POJO | **nein** | Teilstand (Abhängigkeit) |

## 2. Schema (Entscheidung)
**Vorhandene Konvention des Kerns übernehmen**, je Modul unter seiner Mod-ID:

```
/<modid> config                          -> list (alle Optionen, geänderte gold)
/<modid> config list [filter]            -> Präfix-Filter (Gruppen per Tab)
/<modid> config get <option>             -> Wert, Standard, Typ
/<modid> config set <option> <wert>      -> Typprüfung, Grenzen der Config-Klasse, speichern, live
/<modid> config reset <option>           -> Standardwert
/<modid> config help                     -> Kurzhilfe
/simpleclient <modid> …                   -> dasselbe für Client-Configs (clientseitiger Befehl, ohne Rechte)
```
Begründung: Besitzer kennt `/simplebuilding config …`; get/set/reset als Wörter vermeidet Kollisionen zwischen
Optionsnamen und Werten. Client-Configs bekommen eine **eigene Wurzel** `/simpleclient`, weil Fabric einen Client-Befehl
zuerst ausführt und einen Befehl, dessen Wurzel der Client kennt, nicht mehr an den Server gibt (geprüft in
`ClientCommandInternals`: nur „unbekannter Befehl“ geht weiter) – eine Client-Wurzel `/<modid>` würde die
Server-Unterbefehle verdecken.

**Baustein** `com.simplelib.api.config.ConfigCommands` (simplelib, gebündelt per Regel 6a, damit standalone):
- Ein Modul meldet seine Config einmal an: `ConfigCommands.server(MOD_ID).json(X::toJson, X::apply).file(dir.resolve(X.FILE)).register()`
  (POJO: `.gson(Klasse, aktuell, ersetzen, frisch)`, eigenes Speichern: `.save(...)`, Neustart-Optionen: `.restart(...)`,
  Auswahlwerte: `.choices(...)`, Nachlauf (Sync): `.after(...)`, eigene Schlüssel: `.keys(...)`).
- Optionen = Blätter des JSON (`gruppe.feld`), automatisch aus `toJson()`; ein neues Feld ist sofort per Befehl erreichbar.
- Typ aus dem Standardwert: bool / ganze Zahl / Dezimalzahl / Text / Liste / Objekt. Falscher Typ → Fehler, nichts
  gespeichert. Wertebereich: die Config-Klasse begrenzt wie beim Laden (`apply`/`normalize`); weicht der gespeicherte
  Wert ab, meldet der Befehl „begrenzt auf X“.
- Standardwerte ohne Seiteneffekt auf die laufende Config (Schnappschuss → `apply({})` → zurück), POJO über `frisch`.
- Speichern atomar (`ConfigFiles.writeAtomic`), danach `after` (z. B. Client-Sync). Live, weil alle Module ihre
  Felder beim Gebrauch lesen; Optionen, die nur beim Start wirken, sind markiert → Hinweis „wirkt nach Neustart“.
- Rechte: Server-Configs Stufe 2 (`Commands.LEVEL_GAMEMASTERS`), Client-Configs ohne.
- Fabric: simplelib registriert alle angemeldeten Configs in einer **späten Phase** des
  `CommandRegistrationCallback` (Fabric-Event-Phasen), damit sie sich an schon vorhandene Wurzeln `/<modid>` hängen,
  ohne deren Rechteprüfung zu überschreiben. Client analog mit `ClientCommandRegistrationCallback`.
- Lang `simplelib.config.*` EN/DE.

## 3. Aufräumen bestehender Befehle
- Rechte für Config-Änderungen einheitlich Stufe 2: `/simplemobheads`, `/simplefun`, `/simpletools`,
  `/simplefarming`, `/simpleloot`, `/simplemobmanners`, `/simplemovement`, `/simplequalityoflife submod`,
  `/simplevisuals config` (bisher 3/4). Keine Funktion entfernt, keine Umbenennung.
- `/simplemodels reload`: Rückmeldung (Anzahl Modelle) mit Lang EN/DE; `/simplevisuals config visuals enableAnvilFormatting <wert>`:
  Rückmeldung beim Setzen.
- **Gefundener Fehler (beim Umsetzen):** Client-Wurzeln mit dem Namen eines Server-Befehls verdecken dessen Unterbefehle,
  weil Fabric Client-Befehle zuerst ausführt und nur „unbekannter Befehl“ an den Server weitergibt. Betroffen:
  `/simplemaps picture` (Client) verdeckt `/simplemaps web …`, die Client-Optionen `/simplevisuals …` verdecken
  `/simplevisuals config visuals enableAnvilFormatting`. Fix: unbekannte Eingaben dieser Client-Wurzeln gehen als
  Befehlspaket an den Server (`ConfigCommands.forwardToServer`, `simplelib/api/client/ServerCommand`; nicht über
  `sendCommand`, das Fabric wieder abfängt). Client-Configs deshalb unter eigener Wurzel `/simpleclient`.
- Bestehende Einzel-Schalter bleiben als Kurzformen neben `config`.

## 4. Tests / Prüfung
- simplelib-Gametests (`module-simplelib-fabric-263` + standalone): `config_command_types` (synthetische Config:
  lesen, setzen, ungültiger Wert, begrenzt, reset, Liste, Objekt, Persistenz in der Datei, Standardwerte unverändert),
  `config_command_lib` (LibConfig über den Dispatcher, Rechte: Stufe 0 abgewiesen), `config_command_every_option`
  (jede angemeldete Option jedes geladenen Moduls ist per `/<modid> config get <option>` erreichbar – im
  Integrationslauf über alle Module).
- Exemplarisch in Modulen: simplebeacons (`config_command`: Dispatcher set/get/reset, Datei), simplemobheads
  (POJO-Weg), simplecrucibles (verschachtelte Gruppe `soulLava.*`, Neustart-Hinweis).
- Prüfskript `tools/check_config_commands.py` (in `check`): jedes Modul mit eigener Config-Datei meldet sie bei
  `ConfigCommands` an oder steht mit Grund in der Ausnahmeliste (Teilstand) – neue Configs ohne Befehl fallen auf.
- Wiki: Befehls-Abschnitte (`modules/<id>/wiki/manual.json`, Feature `commands`) um den Config-Befehl ergänzt,
  dann `wiki/generate.py --all`, Guides, Discover.
- Läufe: `:modules:<mod>:fabric:compileJava`, Targets `module-<mod>-fabric-263` (+ standalone) gefiltert,
  `fabric-263` gefiltert, `./gradlew check -q`.

## 5. Risiken
- Wurzel-Zusammenführung in Brigadier: bestehende Wurzel behält ihre `requires`; Config-Knoten hat eigene Stufe 2.
  Liegt die Wurzel höher (z. B. 4), gilt die höhere Stufe → deshalb Rechte in §3 angeglichen.
- `apply()` mit Seiteneffekten beim Standardwert-Schnappschuss: nur reine Zuweisungen in den angemeldeten Klassen
  (geprüft); POJO-Configs nutzen `frisch`.
- NeoForge/Forge: Anmeldung liegt in shared-Code, nur der Fabric-Hook fehlt dort → Port-Run.

## 6. Teilstand (bewusst offen)
- Module ohne simplelib (farming, loot, mobmanners, movement, weather, fun, money, dimensions, end,
  enddragonfight, wanderers, aesthetics, opt*): brauchen simplelib als gebündelte Abhängigkeit (wie simpletools/
  simplerespawn) – eigener Schritt, weil simplelib dann standalone mitgeladen wird (eigene Inhalte).
- simpletweaks: Record-Configs in mehreren Dateien (`play`, `admin`, `protect`, `claims`) → je Datei eine Anmeldung
  mit `.gson(...)`; offen.
- NeoForge/Forge-Hook im Port-Run.
