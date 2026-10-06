# Übergabe (Stand 2026-09-30) – Hauptlinie 26.3

Zuerst `AGENTS.md` vollständig lesen, dann diese Datei und `docs/ai/WORKFLOW.md`.
Wünsche/offene Besitzerpunkte: `.claude/QUEUE.md`. Code gewinnt gegenüber alten Run-Berichten.
Worker arbeiten in ihrem Worktree/Branch, committen ohne Push/Merge. Andere Linien erst im
separaten Port-Run nach Besitzer-Abnahme; shared muss weiterhin für 26.2 kompilieren.

## Worker-Nachtrag 2026-10-05: 26.2-Nachtserie Folgefix

Branch `gpt-line262b`: Die Testzentralen-Ausnahme fuer Soul Lava folgt jetzt
`McVersion.CRUCIBLE`. Maschinenkapitel 9/10 waren bereits korrekt ausgeblendet;
der Handbuchtest prueft sie nun explizit als deaktivierte Kapitel. Alle aktiven
Sprachschluessel und Registry-Abdeckungen bleiben streng geprueft, zusaetzlicher
Test fuer die flagabhaengige Soul-Lava-Ausnahme. Kein Feature-Port/Lang-Diff.

26.2 Fabric/NeoForge/Forge **2932/2932, alles gruen**; 26.3 Fabric/NeoForge
**1954/1954, alles gruen**, jeweils inklusive Testzentralen-Neubau und Abdeckung.
`check -q -PskipWiki`, 26.2-Compiles und Forge-26.3-Compile: **GRADLE_EXIT=0**.
Wiki --all/--all --check gruen, kein Inhaltsdiff. Kein Datagen/Client/Push;
Besitzerwelt-Neubau bleibt offen. Belege: `docs/ai/PLAN-LINE262B-2026-10-02.md`.

## Worker-Nachtrag 2026-10-05: Cover Option B und Configs

Branch `gpt-coverconf`: 26.3 entfernt Cover aus Außenposten/Waldanwesen, Gewichte
gehen an Leer. Keine verbleibende Survival-Quelle; Kreativmodus/Befehle bleiben.
26.2 bleibt bis zum Port unverändert. Vier neue begrenzte Serveroptionen für
Auto-Schmied-Verzögerung, Diamantkiesel-Ausbeute, Pfeil-Rückgewinnung und Scan-Intervall;
Defaults unverändert, EN/DE an beiden Orten. Zwölf Bewertungen mit Gründen:
`docs/ai/CONFIG-IDEEN-2026-10-05.md`. Fabric/NeoForge **1954/1954, alles gruen**
(je 977), einschließlich je acht Testzentralen-Prüfungen mit Neubau und Item-/Block-Abdeckung.
`check -q` einschließlich checkBalance sowie 26.2-Fabric/NeoForge- und Forge-26.3-Compiles:
**GRADLE_EXIT=0**. Datagen nur 26.3, Wiki venv --all und uv --all --check grün;
56 Wiki-Unit-Tests, 13 Balance-Auslesetests, keine doppelten EN/DE-Schlüssel.
Keine Module geändert, kein Client/Push; Besitzerwelt-Neubau bleibt offen.
Plan: `docs/ai/PLAN-COVER-CONFIG-2026-10-02.md`.

## Worker-Nachtrag 2026-10-05: Hängematten-Testcenter

Branch `gpt-hammocktc`: 22 beschriftete, getrennte Beispiele rechts der Maschinen:
gerade X/Z und 45 Grad jeweils 2/3/4 frei, sechs schräge Winkel, Zaun/Holz,
fünf Doppelstabarten und Netheritstab auf Holz, alle 16 Farben. Kein Kupferstab,
damit die Blitzstation funktioniert. Aufbau prüft intakte BE-Verknüpfungen;
Szenariotest prüft Varianten, Anker, Farben, Schilder und Überschneidungen.
Fabric/NeoForge **1932/1932, alles gruen**, je acht Testcenter-Tests grün.
`check -q -PskipWiki` samt 26.2-/Forge-26.3-Compiles: **GRADLE_EXIT=0**;
Wiki --all/--all --check grün, kein Inhaltsdiff. Kein Client/Push;
Neubau in der Besitzerwelt und Sichtabnahme offen. Belege:
`docs/ai/PLAN-HAENGEMATTEN-TESTCENTER-2026-10-02.md`.

## Worker-Nachtrag 2026-10-05: NeoForge-Modul-Gate

Branch `gpt-neorun`: `loadedMods.add(simplelib)` verwarf die Gradle-Konvention;
Money/Dimensions luden nur SimpleLib und fanden keine eigenen Tests (Exit -1).
Die 21 erfolgreichen Tests im Sammellog gehoerten zu SimpleLib, die Claims-Fehler
zu absichtlichen Negativtests. Die gemeinsame Konvention erhaelt jetzt die
effektive Mod-Liste. Modul-Spielordner liegen unter
`integration/run-neoforge-263/<id>`; Standalone bleibt getrennt. Leere alte
`mods/`-/`defaultconfigs/`-Ordner brauchen keine Migration. Bestehende Reihenfolge
der Integrationsserver bleibt erhalten; keine Dateikollision als Ursache behauptet.

Money/Dimensions zweimal **61/61**, alle 21 Modul-/Standalone-Ziele **617/617**,
Kern-NeoForge **962/962**, jeweils `alles gruen`, inklusive Testzentralen-Neubau
und Item-/Block-Abdeckung. `check -q -PskipWiki` und Pflicht-Compiles:
**GRADLE_EXIT=0**. Zusaetzliche Gradle-Konfigurationspruefung kontrolliert 21
effektive Mod-Listen und eindeutige Ordner. Kein Client/Push/Port;
Belege: `docs/ai/PLAN-NEOFORGE-MODULE-RUNS-2026-10-02.md`.

## Worker-Nachtrag 2026-10-05: Hub-Testwelt

Branch `gpt-hubworld`: `client_fresh` archiviert die gesamte alte Welt und erzeugt
per QuickPlay eine neue. Explizites Rebuild behaelt den Ursprung. Der bisher
ungeladene Ursprungschunk lieferte y=-64 (Boden unbaubar bei -65); Chunk-Laden,
Reparatur alter Urspruenge, sichere Eingangsplattform, Teleport und Weltspawn mit
Radius 0 beheben den Void-Start. Experimental-Lifecycle-Bestaetigungen entfallen
nur im expliziten Hub-Dev-Client auf 26.3; normale Installationen bleiben unveraendert.

Fabric-Filter **8/8**, volle Fabric-/NeoForge-Suiten **1922/1922, alles gruen**,
einschliesslich Neubau und Item-/Block-Abdeckung. `check -q -PskipWiki`, 26.2-
Compiles und Forge-26.3-Compile: **GRADLE_EXIT=0**. Hub-Client-Property auf allen
drei Loadern ohne Clientstart geprueft; Server erhalten es nicht. Wiki --all und
--all --check gruen, kein Inhaltsdiff. Neue Hub-Tests **5/5**, Gesamtsuite **86/87**:
unveraenderter Bestandsfehler `simplelib needs tests.standalone (principle 8)`.
Keine Testabschwaechung, kein Client/Push/Port. Besitzer prueft nach Uebernahme
zwei frische Starts, Eingang/Respawn und Dialogfreiheit. Belege:
`docs/ai/PLAN-HUB-TESTWORLD-2026-10-02.md`.

## Worker-Nachtrag 2026-10-05: Auto Smither im Crafter-Stil

Branch `gpt-checkui`: Crafter-Hintergrund, drei Eingaben links (26/44/62,35),
großer Ergebnisrahmen rechts (Item bei 134,35), dynamisch zentrierter Titel.
Geisterbilder und bedingter Fehlerpfeil erhalten; Generator prüft alle 40 Slots.
Bestehende Ausgabe-/Trichterregeln einschließlich Forge-SidedInvWrapper unverändert.
Alle vier Auto-Smither-Tests auf jedem Loader bestanden; volle Serversuiten
**2881/2881, alles gruen**, Testzentralen-Neubau und Item-/Blockabdeckung grün.
`check -q`: **GATE_EXIT=0**, 26.2-Compiles und Wiki --all/--all --check grün.
Kein Client/Push; Ingame-Sichtabnahme beim Besitzer offen.
Vorschau: `C:/Users/o_o/code/minecraft-mods/previews/auto-smither-crafter-ui-vorschau.png`.
Belege: `docs/ai/PLAN-SMITHER-CRAFTER-UI-2026-10-02.md`.

## Worker-Nachtrag 2026-10-05: Netherziegel-Checker

Branch `gpt-checkui`: Netherziegel- und rote-Netherziegel-Quarz-Schachbretter
vollständig in die bestehende 26.2/26.3-Familie integriert, ohne neues Flag.
Datagen nur 26.3; eigene Texturen und 16x-Vorschau, EN/DE an beiden Orten,
Handbuch/Wiki, Kreativ-/Suchtab, Spitzhacke/Drops, Rezepte und Money-Steinmetzangebote.
Checker-Filter **3/3**, volle Fabric-/NeoForge-/Forge-26.3-Suiten **2881/2881,
alles gruen**, einschließlich Testzentralen-Neubau und Item-/Blockabdeckung.
`check -q`: **GATE_EXIT=0**, 26.2-Compiles **COMPILE262_EXIT=0**,
Wiki --all/--all --check grün; 55 Wiki-Unit-Tests bestanden.
Keine Clients, kein Push, keine Änderung an 1.21.11/26.4. Zwei bestehende deutsche
Handbuch-Themenlisten bleiben zu lang; Checker-Seiten passen EN/DE.
Vorschau: `C:/Users/o_o/code/minecraft-mods/previews/netherziegel-checker-vorschau.png`.
Belege: `docs/ai/PLAN-CHECKER-2026-10-02.md`.

## Worker-Nachtrag 2026-10-05: Forge Crucible

Branch `gpt-forgecrucible`: SimpleLib war als Jar-in-Jar geladen, aber Forge
verwarf den Datenpack wegen fehlender `pack.mcmeta`. Dadurch fehlte
`simplelib:crucible_walls` und der erste Vorschlaghammer-Bauschlag wurde ignoriert.
Forge-Pack-Metadaten ergaenzt; Bautest prueft beide Material-Tags, den echten
Item-Aufruf und den Haltbarkeitsverbrauch jedes Schlags.
Forge-Crucible **14/14**, volle Fabric-/NeoForge-/Forge-26.3-Suiten
**2881/2881, alles gruen**, inklusive Testzentralen-Neubau und Item-/Block-Abdeckung.
`check -q -PskipWiki` und Pflicht-Compiles: **GRADLE_EXIT=0**.
Wiki --all/--all --check gruen, kein inhaltlicher Wiki-Diff.
Kein Client/Push/Port; Belege: `docs/ai/PLAN-FORGE-CRUCIBLE-2026-10-02.md`.

## Worker-Nachtrag 2026-10-05: Forge Auto Smither

Branch `gpt-forgesmither`: Forge erbte einen unsided `InvWrapper`, der zuerst
Eingaben entnahm und die Ausgabe nach 20 Ticks noch liegen liess. Ein enger
Forge-Mixin verwendet jetzt `SidedInvWrapper` mit den vorhandenen Seitenregeln;
Capability-Lebenszyklus bleibt bei Vanilla/Forge. Der reale Trichtertest behaelt
seine 20-Tick-Frist und prueft zusaetzlich den Basis-Slot.
Forge-Filter **6/6**, volle Fabric-/NeoForge-/Forge-26.3-Suiten **2839/2839,
alles gruen**, inklusive Testzentralen-Neubau und Item-/Block-Abdeckung.
`check -q -PskipWiki` und Pflicht-Compiles: **GRADLE_EXIT=0** nach Wiederholung
eines Manifest-Downloadfehlers. Kein Client/Push/Port; Belege:
`docs/ai/PLAN-FORGE-AUTO-SMITHER-2026-10-02.md`.

## Worker-Nachtrag 2026-10-05: Befunde 11/12

Branch `gpt-befunde`: Visuals/Sounds melden Fehler über GameTestHelper.assertTrue/fail.
Riding-Handling prüft alle fünf Vanilla-Stufen auch standalone, Enderit zusätzlich nur mit SB.
Zwölf Modulziele Fabric/NeoForge 26.3 **296/296, alles gruen**; Kernserver **1890/1890, alles gruen**
einschließlich Testzentralen-Neubau und Item-/Block-Abdeckung. `check -q -PskipWiki` sowie
26.2 Fabric/NeoForge- und Forge-26.3-Compile **GRADLE_EXIT=0**. Befundliste/externe Memory aktualisiert.
Kein Client, Push oder Port; Wiki wird zentral regeneriert. Belege und initiale Dateisperre:
`docs/ai/PLAN-BEFUNDE-11-12-2026-10-02.md`.

## Worker-Nachtrag 2026-10-05: 26.2-Linie ohne Feature-Port repariert

Branch `gpt-line262`, Basis `6735bc9b`: Tests nach Versionsflags getrennt,
26.2-Handbuchbeispiele berichtigt, historische Upgrade-Fixture erhalten und
aktuelle Writer-Fixture ergaenzt. Forge registriert nun die bestehenden
Suchreiter-, Shulkerkisten- und Breeze-Farmland-Hooks.

Volle 26.2-Suiten Fabric/NeoForge/Forge: **2836/2836, alles gruen**;
26.3-Gegenprobe Fabric/NeoForge: **1890/1890, alles gruen**. Testzentralen-Neubau
und Item-/Block-Abdeckung bestanden. Forge-26.3-Compile und
`check -q -PskipWiki`: **GRADLE_EXIT=0**. Wiki --all/--all --check gruen,
ohne inhaltlichen Wiki-Diff.
Die geaenderten 26.2-Handbuchseiten passen in EN/DE (je 12 Zeilen).
Zwei bekannte deutsche Themenlisten-Ueberlaengen der 26.3-Pruefung bleiben offen.
Kein Client, Datagen, Push oder Merge. Ursachen und Belege:
`docs/ai/PLAN-LINE262-2026-10-02.md`.

## Worker-Nachtrag 2026-10-04: Auto Smither

Branch `gpt-smither`: gespeicherte, nur entnehmbare Ausgabe statt gesperrter
Rezeptvorschau. Fronttransfer bleibt; Rest bleibt im Ausgang. Volle/inkompatible
Ausgabe stoppt ohne Verbrauch. Spieler-Klickwege und Trichterseiten abgesichert.
Vanilla-Slot-/Labelpositionen und Geisterbilder bestaetigt; Hammer-Dekoration und
Crafter-Redstone-Symbol entfernt, Fehlerpfeil nur bei ungueltigem vollstaendigem Rezept.
Generator und 16-fache A/B-Vorschau: `tools/textures/auto_smither_gui.py`,
`C:/Users/o_o/code/minecraft-mods/previews/auto-smither-gui-vorschau.png`.

Filter 8/8, volle Fabric-/NeoForge-Suiten **1872/1872, alles gruen**; Testzentrale
gebaut und alle Items/Bloecke abgedeckt. 26.2 Fabric/NeoForge-Compile,
Forge-26.3-Compile und `check -q`: **GRADLE_EXIT=0**, einschliesslich 54 Wiki-Tests.
Wiki/GUI/allgemeine Texturpruefung gruen. Der erste Gateversuch scheiterte an
373 reinen CRLF-Abweichungen des Sandwich-Bytechecks; lokal normalisiert, kein
inhaltlicher Modul-Diff. Belege/Abweichungen: `docs/ai/PLAN-AUTO-SMITHER-2026-10-02.md`.
Kein Clientstart, Port, Merge oder Push. Sichtabnahme/Besitzerwelt bleiben offen.
Der gemeldete Einlegefehler war im Ausgangscode bereits gesperrt und dort nicht
reproduzierbar; der neue entnehmbare Ausgabe-Vertrag ist jetzt explizit getestet.


## Worker-Nachtrag 2026-10-04: small9

Branch `gpt-small9`: Basic-/Enderite-Aufwertungen nutzen echte Vanilla-Schmiedevorlagen
mit EN/DE-Tooltips. Verstärker spielen innerhalb bestehender Hörweite ohne Pegelverlust;
Noten nutzen ein S2C-Payload für alle drei Loader, Musik eine explizite Hörweitengrenze.
Der Ofen-Ausbeutebonus entfällt; alte Bonuszähler werden ignoriert, doppelte XP bleibt.
JEI, Handbuch, beide Sprachorte und belegte Wiki-Prosa aktualisiert.
Server Fabric/NeoForge 26.3: **1870/1870, alles gruen**; Testzentrale und Itemabdeckung grün.
26.3-Datagen, 26.2-Compiles, Forge-26.3-Compile, `check -q` und checkBalance grün;
Wiki --all/--all --check und 55 Wiki-Tests grün. Je Aufgabe ein Commit, kein Push/Merge.
Sicht-/Hörabnahme und Besitzerwelt offen; zwei bekannte deutsche Handbuch-Themenlisten
laufen weiterhin über. Details: `docs/ai/PLAN-SMALL9-2026-10-02.md`.


## Worker-Nachtrag 2026-10-05: Besitzer-Abgleich (`gpt-gaps`)

- Küchenmesser: ein Stock und vier Eisennuggets (`"  N"/" NN"/"SN "`),
  Rezeptgenerator, EN/DE-Tooltip, Wiki, Plan F8 und neuer Rezept-GameTest abgeglichen.
- Schmiedebuch: drei Upgrade-Dummies erzeugen auf 26.3 keine Displays mehr;
  Slot-Rezepte bleiben erhalten. JEI nutzt dieselbe ID-Liste. Handbuch-Reiter
  werden auch bei gelernten Rezepten ohne Display weiterhin freigeschaltet.
- Money: Nihil-Gewölbe wie Astralgewölbe, Standardpreis 37 Scheine, Bestand 1,
  Angebotschance 10 %, bedingt durch Mod und Handelskonfiguration.
- Serverlauf `2026-10-04T23-16-05Z-953c`: **1954/1954, alles gruen**.
  Hauptmod je 941/941, Sandwiches und Money je Loader 18/18. Testzentralen-Neubau
  und vollständige Item-/Blockabdeckung auf beiden Hauptloadern bestanden.
- Datagen nur 26.3 sowie Wiki venv `--all` und uv `--all --check` grün.
  `check -q`, 26.2 Fabric/NeoForge-Compile und Forge-26.3-Compile: `GATE_EXIT=0`.
  `checkBalance`: 223 erzeugte Stellen, 0 Fehler; 54 Wiki-Unittests: `OK`.
- Kein Push/Merge, keine Client-Tests, keine Besitzerwelt verändert. Sichtabnahme,
  Testzentrale in der Besitzerwelt und Forge-Laufzeit bleiben offen.
  Details und Fehlersuche: `docs/ai/PLAN-BESITZER-LUECKEN-2026-10-02.md`.

## Worker-Nachtrag 2026-10-04: wackelige GameTests

Branch `claude-gpt-flaky`: Knopf-Ausfuehrung bedingt abwarten, eigene Scoreboard-Zaehler und
entfernten Testbereich samt Tickwarteschlangen bereinigen; Claims-Attractor erst bei sichtbaren
Grenz-Probes pruefen; Shulker-Seed/freie Spawnzellen, gemeinsame Magnet-Taktung und gemischte
Palette-Testflaechen. Keine Gameplay-Aenderungen oder abgeschwaechten Assertions.
Alle fuenf Gruppen dreimal je Fabric/NeoForge gruen; volle Basis 1814/1814 und SimpleTweaks
104/104 gruen. `check -q`, 26.2 Fabric/NeoForge-Compile und Forge-26.3-Compile gruen.
Testzentralen-Neubau/Item-Abdeckung bestanden. Kein Clientstart, Port, Merge oder Push.
Plan, Ursachen, Diagnoselaeufe und Beleg-IDs: `docs/ai/PLAN-FLAKY-GAMETESTS-2026-10-02.md`.
Separater Handbuchcheck meldet zwei bestehende deutsche Themenlisten-Ueberlaengen; unveraendert.

## Vorlagenkosten 26.3 (2026-10-04, Worker claude-gpt-templates)

- `EXPENSIVE_TEMPLATES`: 26.3 true, 26.2 false. Glowing/Emitting kosten zusätzlich
  vier Diamanten und zwei thematische Materialien; Nebenhand-Katalysator je zweimal.
  Pulsating: zwei Echoscherben, zwei Sculk, vier Diamanten und ein zurückbleibender Hammer.
- Kopien der drei Effektvorlagen: sieben Diamanten, Basisblock, Vorlage ergeben zwei.
  Basic-Kopie jetzt sieben Diamanten statt Gold; Enderite und Rüstungsanwendung unverändert.
- Datagen nur 26.3, EN/DE, JEI/REI-Export, Wiki und Rezept-/Verbrauchstests aktualisiert.
  Vollständige Fabric-/NeoForge-Suiten: **1834/1834, alles gruen**; Testzentrale aufgebaut,
  alle Mod-Items/-Blöcke abgedeckt. Gesamt-Gate inklusive checkBalance, 26.2-Compile und
  Forge-26.3-Compile: **GRADLE_EXIT=0**. Wiki --all/--all --check und 50 Wiki-Tests grün.
- Details, Balance, Testläufe und Grenzen: `docs/ai/PLAN-VORLAGEN-TEURER-2026-10-04.md`.
  Besitzer-/Sichtabnahme und Port-Run offen; zwei bestehende deutsche Handbuchüberläufe.
  Nur Worker-Branch, kein Push.

## Stand Abend 2026-09-30 (Orchestrator)

Aktueller Nachtrag 2026-10-01: Folgewelle auf `09567ce3` mit GREEN gepusht;
nach Ende der Besitzer-Clients folgten echte serielle UI-Tests und gezielte
Testkorrekturen. Ergebnisse, offene Sicht-/Audiogrenzen und das neue exakte Gate:
`docs/ai/CLIENT-ACCEPTANCE-2026-10-01.md`. Hub-Neustart bereits erledigt.

- Auf master: SimpleBuilding plus neun Module unter `modules/` (siehe `modules/modules.json`):
  simplemoney, simpleriding, simplemodels, simplefun, simplevisuals, simplesounds,
  simplequalityoflife, simpletweaks, simpledimensions. Fabric + NeoForge 26.3 immer, Forge 26.3
  experimentell und opt-in (`-Pforge263=true`) fuer alle Module ausser simpledimensions.
  Dazu: Money-Links (243 Kaufangebote), SimpleBuilding-Haertung (`docs/SB-HARDEN.md`),
  Faktenpass, Sprach-Bridge (`tools/voicebridge`, `docs/ai/VOICE-HANDS-FREE.md`).
- Gate auf 7c540a06: 1600/1600 Server (fabric-263 + neoforge-263), 393/393 Integration + alle Modulsuiten, alles gruen. Client-Smokes und Abnahmen im Spiel stehen aus (Liste in `.claude/QUEUE.md`).
- Weiterarbeiten (auch am Laptop): `docs/ai/LAPTOP-SETUP.md`, `docs/ai/WORKFLOW.md`,
  `python tools/ai/aitool.py status|codex|merge-module|gate`. Neue Module: `python tools/newmod.py`
  (Vertrag `docs/MULTIMOD.md`, Forge-Rezept dort). Briefs liegen in `docs/ai/briefs/`, Memory in `docs/ai/memory/`.
- Besitzer-Entscheidungen 2026-09-30 Abend (Details und Reihenfolge: `.claude/QUEUE.md`, Briefs `docs/ai/briefs/next-*.md`):
  1. Claims portieren, zunaechst als ausgeschaltetes Feature; 2. alte Echo-Kompasse nicht migrieren;
  3. Dimensions: Bogen bleibt Standard, vorinstallierte Dimensionen einzeln in den Einstellungen schaltbar,
  freie Portalformen spaeter nach gemeinsamem Durchgehen; 4. QoL-Haltbarkeitsbonus auch fuer Mod-Werkzeuge,
  Standard 1, erhoehbar; 5. Sounds an die Visuals-Stufe koppeln; 6. Forge danach.
- Keine Laeufe mehr offen; alle `codex-*` Branches sind gemergt. Andere Linien (26.2, 1.21.11, 26.4)
  warten auf die Release-Ankuendigung des Besitzers (Port-Run).
- Bekannte Grenzen: NeoForge-Clients, echte Altwelten und Besitzerwelt ungeprueft; Forge ohne Cloth-Dialog;
  Claims bleiben ausgeschaltet; Dimensions sperrt erkannte Claim-Mods ohne Adapter.

## VOICEBRIDGE (2026-09-30, Codex, Branch codex-voicebridge)
- tools/voicebridge: stdlib-Server (8772, Loopback oder explizite Tailscale-IP), private Token-/CSRF-/Host-/Origin-Pruefung, Rate-/Body-Limits, Audit, Notaus und persistente Projekt-Sitzungen/Berichte. Kein pip im Standard, kein Minecraft-Code oder Port geaendert.
- Handy/Laptop-PWA: grosse Tap-start/Tap-stop-Taste, Space, de-DE Browser-STT, Satzweise Vorlesen, Stop/Repeat, Haptik/Beep, Wake Lock, Media Session, Shell-Serviceworker und Raster-Installationsicons. Optional Whisper/Piper hinter getrennten Interfaces, standardmaessig aus.
- Claude/Codex immer lesend, Ollama-Q&A und Echo. Default Claude; native CLI fehlt hier, offizielle Flags geprueft und Laufzeit-Help-Pruefung bricht bei fehlendem restricted/tools/plan/resume sicher ab. Codex-Hilfen/Resume-Optionen/Features lokal gelesen; Shell/Code/Browser/Hook-Werkzeuge eingeschraenkt. Repository-Codex-Konfigurationen werden abgelehnt; normaler User-Config-Import deaktiviert.
- Aktionen: ausschliesslich konkrete UTF-8-Dateivorschlaege, vorher vorgelesen, zufaelliges Wort als naechste Nachricht binnen 60 s; genau einmal. Pfad-/Groessen-/Versionspruefung und unveraenderliche Backups. Kein allgemeiner Schreib-/Shellmodus. Runs/Merges nicht Teil der vorgegebenen Allowlist und gesperrt; Push/Force/Loeschen nie verfuegbar. Sprache von Logs/Dateien startet keine Aktion.
- Setup mit Tailscale Serve HTTPS, Projekt-/Modellwahl, Limits/Adapter in docs/ai/VOICE-HANDS-FREE.md. launch.json/voicebridge ist bewusst Trockenlauf. Echte PC-Konsole gab Loopback-URL aus, erkannte 100.69.194.127 und lokalen ts.net-Namen; keine Tailnet-Freigabe oder HTTPS-Konfiguration veraendert.
- Verifiziert: 40 Python-Unit-/HTTP-Tests gruen; JavaScript-Syntax und Node-UI-Harness (Fake-STT/TTS, Tap-Ablauf, Schweigen waehrend Aufnahme, authentifizierter Turn, Satzvorlesen/Repeat/Stopp) gruen. Echten Server per HTTP im Trockenlauf geprueft, kein echter Agent gestartet. Gradle nicht geaendert: Launch-Hub-Pythontests sind nicht in check verdrahtet.
- Wiki generate und --check aktuell; Generator erzeugte nur inhaltsgleiche Zeilenenden, keine Wiki-Aenderung committet. Vollstaendiges gradlew.bat check -q im Worktree **GRADLE_EXIT=0**, Ausgabe gelesen; shared/26.2 bleibt kompilierbar.
- Testzentralen in beiden separaten 26.3-GameTest-Welten neu gebaut: **10/10, alles gruen**, Run 2026-09-30T15-16-30Z-578c, Filter simplebuilding:*test_centre*, einschliesslich Item-/Blockabdeckung. Besitzerwelt unberuehrt.
- Nicht verifiziert: gerenderte Handy-/Desktop-Oberflaeche (Computer Use: browsers/apps leer; IAB nicht verfuegbar), echtes Mikrofon/STT/TTS, Headset/Sperrbildschirm/Installation, Tailscale-HTTPS-Ende-zu-Ende, echte Claude/Codex/Ollama-Turns und lokale Whisper/Piper-Adapter. Keine Minecraft-Clienttests oder vollstaendige Serversuite; nur Centre-Filter, keine neue Spiel-Pixelkunst. Besitzer sollte Handy/Headset und gewaehlten Anbieter abnehmen. Kein Push/Merge, keine mc1_21_11/mc26_4-Quellaenderung.


## Aktueller integrierter Bestand

Wellen 23/24 sowie KK2, BUGS, INFRA-W/B/F, FIX16 und Simple Money sind im Ausgangs-master
dieses Faktenpasses vorhanden. Historische Branchnamen bedeuten nicht mehr „ungemergt“.
Die detaillierten früheren Laufberichte bleiben in der Git-Historie dieser Datei erhalten.

- Detector: ID `detector`, `ore_detector` Legacy-Alias (`LegacyItemIds`). Platzierbar nur
  kalibriert; Nadel, Sounds, Partikel statt Bildschirmtext (`PlacedDetectors`).
- Attractor: ID `magnet`. Gehalten 4 Blöcke Basis, Range +2 je Stufe bis 9 vor Config;
  abgelegt 8 vor Config. Constructor's Touch schaltet Filter frei, erhöht keinen Radius.
  Schleichklick auf Block/liegendes Item oder Rechtsklick im Inventar wählt den Filter,
  Schleichklick Luft löscht ihn. Ohne Touch gilt ein gespeicherter Filter nicht.
  Ablegen ohne Touch per Schleichklick, mit Touch per normalem Rechtsklick.
  Ruhezone für beide Formen: `server.tools.attractorMinimumDistance`, Standard 1,25,
  Cap 0,5–2; darin Bewegung dämpfen statt ziehen (`MagnetItem`, `PlacedAttractors`).
- Resonance Rod / Resonanzstab: aktuelle ID `amethyst_lens`,
  `laser_pointer` Legacy-Alias. Rezept IIR/ICA/IIR: Eisenbarren, Redstone, Eisenkern,
  Amethystscherbe. 640 Ladung standardmäßig, 4 je Sekunde; 16 Scherben laden voll.
  Der alte Configschlüssel `beamCostPerSecond` wird nicht gelesen; `chargePerSecond` gilt.
- Velocity Gauge 26.3 (Nachtrag 8): „NAN“/„NCN“/„NKN“, sechs Kupfernuggets, Amethystscherbe oben mittig,
  Uhr Mitte, Kupferkern unten mittig. 26.2 behält „ NA“/„NCN“/„KN “ und Kompass. Oktant: „ NR“/„NCN“/„GNL“, vier Goldnuggets,
  Blitzableiter oben rechts, Goldkern unten links, Leine unten rechts, Kompass Mitte.
- Echo Sounder: NNN/NRN/ENN, sieben Enderitklumpen, Bergungskompass, Enderitkern.
  **Aktueller Code verlangt 3 Sekunden Halten** (`EchoCompassItem.use`, `releaseUsing`,
  `finishUsingItem`), kein selbstlaufender Sprung durch einen kurzen Klick. Frühes Loslassen
  bricht ab. Verknüpfen/Fehlversuche sperren standardmäßig 1–5 s je Entfernung;
  Sprungpause 24 s. Nicht voll geladen: 6 s Halten und Zerbrechen nach dem Sprung.
- Hammer 26.3: gleiche Spitzhackenstufe/Effizienz; erster Block 1,5x Zeit, Blöcke 2–9
  je +0,8x, ab Block 10 je +0,7x. Oktant 2x je Block, maximal 32 je Kante/4096 Plätze.
  Kein Quadratwurzelmodell und kein Wechsel zur niedrigeren Materialstufe.
  Schleichen beim Abbauen: ein Block. Schleichen beim Umformen ohne Touch: eine
  Zielecke 1,5x schneller; mit Touch rückwärts. Haltbarkeit unverändert.
  Diamantblock zerlegen erst ab Eisenhammer. Belege: `SledgehammerUtils`,
  `SledgehammerItem`, `HammerCorners`; Details `docs/SLEDGEHAMMER-BALANCE.md`.
- Mega-Handbücher: zwei Basisitems, getrennte Regale. Basisrezepte Buch + Werkbank
  bzw. Buch + Holzspitzhacke. Gesperrten Reiter anklicken und im Buch bestätigen;
  genau ein Schlüsselitem, auch in Creative. Kein Crafting-Upgrade/-Vereinigen,
  kein Erstbeitrittsgeschenk. Admin nur OP >= 2. Acht Themenreiter rechts; Rest links
  unter Inhalt und Regalreiter, innerhalb von sechs linken Reitern. Aktuelle Regale:
  elf Mod-/neun Vanilla-Themen inklusive Basis. Lesen pausiert nicht.
  Belege `GuideBooks`, `GuideUnlocks`, `GuideBookScreen`; alte Buch-IDs werden migriert.
- Besatzvorlagen auf 26.3 ablegen und dreimal mit Hammer/Material schlagen;
  Rahmen nur Legacy-Fallback für nicht ablegbare Ziele (`SledgehammerEntityInteraction`).
  Glowing hat eine Stufe. Pulsating allein verändert Sättigung, mit Glowing Helligkeit.
- Pads haben sichtbare Aktivzustände. Deren Bedeutung ist je Typ unterschiedlich:
  Spawn-Teleporter lädt, Trank-Pad bereit, Flug-/Elytra-Pad versorgt Spieler,
  Chunk-Loader lädt Chunks; keine universelle „Spieler steht darauf“-Regel.
- Spawn-Teleporter: eigener Bett-/Ankerspawn, sonst eingestelltes Spawnziel/Weltspawn;
  Redstone wählt immer das Spawnziel. Wartezeit standardmäßig 50/20/5 s.
  Bewegung und jede Signalstärkeänderung setzen sofort zurück, auch kurze Impulse.
- End-Signale: Nihilit/Astralit + Redstone ergibt vier Pulver; Pulver + Hebel = Schalter,
  Pulver + Redstone-Lampe = Lampe. Zwei isolierte horizontale Kanäle, max. 15 Segmente,
  Config-Cap 1–15, Updates alle 2 Ticks, Lampe Licht 12 und leitet nicht weiter.
  Kein Vanilla-Signaleingang/-ausgang (`EndSignalBlock`). `server.features.endSignals`.
- Astral Vault / Astralgewölbe: 54 persönliche Plätze, erste 27 direkt Vanilla-Enderinventar,
  letzte 27 am selben persönlichen Container gespeichert. Rezept Endertruhe + je zwei
  Enderitbarren/Astralitstaub. Abschalten bewahrt Inhalt (`AstralVaultBlock`, `AstralStorageMixin`).
- Forge 26.3 existiert opt-in mit `-Pforge263=true`, Testziel `forge-263`;
  Java 25 für Spiel/Compiler, zusätzlich Java 8 für Launcher. Nicht Standardgate.
  Config-Shim ohne Datei-Persistenz/Cloth-GUI; Details `docs/FORGE-26.3.md`.
- Additive Module unter `modules/<id>/`, Manifest `modules/modules.json`, Scaffold
  `tools/newmod.py`, getrennte Wiki-/Balance-Pfade. Wiki `--all`; keine internen
  Cross-Mod-Imports. Simple Money und Wiring Example vorhanden. Fabric-Integration
  `integration-263`; Simple Money hat eigene Fabric/NeoForge-Testinstanzen.
  Siehe `docs/MULTIMOD.md` und `docs/modules/simplemoney.md` für Grenzen.
- Launch Hub ist integriert (`tools/launchhub/server.py`, Port 8771), mit Mod-Auswahl,
  Starts/Tests/KI-Fix und Verlauf. Wiki 8765, Balancing 8770. Echte Bedienung separat
  abnehmen; Trockenläufe sind kein Spiel-/Sichtnachweis.

## Noch offen für den Besitzer

- Client-Gate/visuelle und akustische Abnahme von 26.3, insbesondere Pads, End-Signale,
  Astralgewölbe, Ecken/Animationen, Pulsating+Glowing, Mega-Handbücher und deutsche UI.
- `/sbtestcentre build` in der Besitzerwelt; GameTest-Welten ersetzen diese Abnahme nicht.
- Launch Hub, Wiki und Balancing auf Desktop/Handy ansehen; echte Client-/CLI-Starts prüfen.
- Forge-Client, optionale JEI/Jade/Curios/Cloth-Integrationen, Default-Einschaltung entscheiden.
- Simple Money: Besitzer-Abnahme, NeoForge-Client, alte Welt. Zukünftige Modulports und
  Forge-Integrationsruntime bleiben separat. Offene Wunschentscheidungen in der Queue.
- Ein-Klick-Echolot ist ein abweichender Wunsch, kein belegtes Verhalten auf master.
- Rezeptfilter des Strahlschalters erkennt jetzt amethyst_lens und die Legacy-ID laser_pointer;
  SB-HARDEN hat die Abschaltung samt Regressionstest korrigiert (siehe docs/SB-HARDEN.md).
- Erst nach Besitzer-Abnahme Port-Run 26.2/1.21.11/26.4; mc1_21_11 unverändert lassen.

## Prüfbelege

Frühere letzte vollständige Hauptlinien-Suite: FIX16, 1562/1562 „alles gruen“,
Run `2026-09-30T13-46-59Z-c843`. Historisch, kein Ersatz für die aktuelle Prüfung.
BUGS: gefiltert 178/178 und gezielte Mega-Guide-Clientabläufe auf beiden Loadern grün.
Forge: historisch 778/778, Run `2026-09-30T13-04-03Z-0963`.
Simple Money: finale Modsuite 20/20; Fabric-Client-Smoke, keine NeoForge-Clientabnahme.
Aktueller Faktenpass (Branch `codex-facts`, Ausgangs-master `274f42d7`):
- Texte in beiden DE/EN-Sprachorten, Guide-Inhalte, JEI/Advancements, Wiki und Entwicklerdoku
  korrigiert; keine Gameplay-Änderung, keine neue Pixelkunst, keine Ports/Push/Merge.
- `python wiki/generate.py --all` und `--all --check`: Exit 0, aktuell. Quests erzeugt;
  Bücher 0 Probleme (maximal 13 Zeilen), Texturen 470 + 9 mcmeta aktuell.
- Vier Sprachdateien ohne doppelte Schlüssel; alle Schlüssel in beiden Orten vorhanden.
- 17 Wiki-Tests grün, davon zwei neue günstige Rezept-Prosa-Gegenproben (vier Sprachdateien
  und eingebettete Modulrezepte gegen tatsächliche Quelldaten).
- `gradlew.bat check -q`: explizit GRADLE_EXIT=0, inklusive shared/26.2 und aller Standardgates.
- Ungefilterter 26.3-Serverlauf: **1562/1562, alles gruen**, je Loader 781, 0 rot;
  Run `2026-09-30T15-04-20Z-0d07`. TestCentreTests baut die ganze Zentrale in beiden
  separaten GameTest-Welten und prüft Planung/Item-/Blockabdeckung; Besitzerwelt unberührt.
- Nicht geprüft: Minecraft-Clients/visuelle Übersetzungsdarstellung, Sounds, echte JEI-Bedienung,
  Browser-/Hub-Darstellung, Besitzerwelt, Forge263 und Modul-Integrations-Spieltests in diesem Run.
  Bereits frühere gezielte Clientberichte sind historische Belege, keine neue Abnahme.

Faktenkorrekturen/Regressionsproben: Commit `0333fa06`. Abschließendes Gate nach der
letzten Text-/Wiki-Korrektur ebenfalls GRADLE_EXIT=0 (Log lokal scratchpad/facts/gate-complete.log).

## Simple Riding 26.3 (Codex, 2026-09-30, Branch codex-port-riding)

- Read-only Quelle: Simple Riding 1.0.5 / Fabric 1.21.11, sauberer Commit `ff83701`.
  Vollstaendiges Inventar, Kollisionen, Config, IDs, Bezugsquellen und Grenzen: `docs/modules/simpleriding.md`.
- Additives Modul `modules/simpleriding` fuer Fabric + NeoForge 26.3: Tailwind, Leaping,
  Pferderuestungs-Verzauberungen, Kreativtab, Beute, Bibliothekarhandel und sechs serverseitige
  Config-Optionen mit Obergrenzen. Alle alten IDs/Config-Pfade und die ungenutzte coordinates-Komponente bleiben.
  Keine eigenen Items/Bloecke/Mobs/Rezepte/Befehle/Keybinds in der Quelle; keine Duplikat-Ruestungen.
  Optionales Enderit-Pferderuestungs-Tag nutzt nur eine oeffentliche SimpleBuilding-ID; Nautilus bleibt ausserhalb.
- Quellfehler korrigiert: keine Gratis-Tailwind-Stufe fuer Ghasts, keine Reflection/Debug-Spam-Suche,
  sofortiges Entfernen veralteter Fahrboni; Protection nur einmal ueber Vanillas BODY-Pipeline.
  Finale Quell-Whitelist beibehalten: kein Mending/Unbreaking/Thorns auf Pferderuestung.
  README-Versprechen fahrender Haendler/Meisterhandel gibt es im Quellcode nicht (nur Bibliothekar 2–4).
- Manifestvertrag fuer alle Eintraege vervollstaendigt; eigener zweisprachiger Wiki-Manual-Katalog,
  eigene generierte Ressourcen und `balance/simpleriding` als Produzentendaten. Keine nichtdestruktive
  Migration bestehender SimpleBuilding-Daten noetig; deren Speicherort bleibt unveraendert.
  Launch Hub zeigt das Modul und fuehrt seine Serverpruefungen nach dem separaten Wiring-Test aus.
  Fabric-Tests/Client nutzen die eigene Integration; NeoForge-Modulpruefungen `integration/run-neoforge-263`.
- Voller Serverlauf **1580/1580, alles gruen**: bestehende 26.3-Ziele **1554/1554**, Modulkatalog
  **26/26** (13 je Loader, SimpleBuilding mitgeladen), Run `2026-09-30T13-31-20Z-5d9f`.
  Echte Fahr-/Flug-/Sprungattribute, Ausruestungswechsel, alle Schutzarten, Amboss/Zaubertisch,
  geladene Beute, Handelsangebote, Config-Schalter/alte JSON-Pfade und Cross-Mod-Lagerung geprueft.
  Testzentrale in beiden automatischen Bestandstestwelten gebaut; Item-/Block-Abdeckung gruen.
- Bestehende Integration **1/1, alles gruen**, Run `2026-09-30T13-34-31Z-962c`.
  Fabric-Modulclient: Titel -> Welt, normale Bibliothekar-Pools, Enchantment-/Tab-Sync und Config-Seite;
  **3/3 Screenshot-Pruefpunkte, alles gruen**, Run `2026-09-30T13-28-14Z-ead5`. Config-Bild angesehen.
  Besitzerclient war vor dem Start nicht aktiv; keine SimpleBuilding-Clientsuite ausgefuehrt.
- Launch Hub **35 Unit-Tests gruen**. Wiki generiert und --check gruen, Modul-Datengate gruen,
  Buecher **0 Probleme**, Texturen **470 + 9 mcmeta aktuell**. Keine neue Pixelkunst; Quell-Icon erhalten.
  Abschliessendes **gradlew.bat check -q --no-daemon: GRADLE_EXIT=0**, Ausgabe gelesen,
  einschliesslich gemeinsamer 26.2-Kompilierbarkeit und Client-Harness-Kompilierung.
- Grenze: experimentelles Trade Rebalance ersetzt Vanilla-Bibliothekar-Tags und versteckt dabei
  auch Mod-Angebote. Servertests pruefen die ausgelieferten Verknuepfungen und echte Angebote;
  der normale Clientwelt-Test beweist die aufgeloesten Pools ohne dieses Experiment.
- Nicht verifiziert: NeoForge-Clientdarstellung/Config-Oeffnung, echte hochgestufte Quellmod-Welt,
  Besitzerwelt-Neubau. Forge 26.3 braucht eigene Loader-, Registry-, Bedingungs-/Loot- und Testadapter.
  26.2/1.21.11/26.4 erst im separaten Release-Port nach Besitzerfreigabe; keine Quelltexte dort geaendert.
  Kein Push, kein Merge; Quellrepo unveraendert.

## MERGE-RIDING / Plugin-Registrierung (Codex, 2026-09-30)

- Branch `codex-port-riding`: lokales `master` (`bf1012f9`) integriert; Merge-/Code-Commit
  `5f7aa7de`. Money und Riding samt allen Tests erhalten, Forge-26.3-Projekte und
  Multi-Mod-Wiki/Balance-Vertrag aus master erhalten; Queue/Handoff vereinigt.
- Manifest `tests` beschreibt Katalogdateien, Namespace, Gradle-Tasks/Reports pro Loader,
  Auswahlvoraussetzungen und Client-Entrypoints. Runner entdeckt Ziel-IDs, Kataloge und
  Ergebnisnamensraeume generisch; bestehende Ziel-IDs und historische mcLine-Gruppen bleiben.
- Integration erzeugt Fabric-Runs und Client-Metadaten im Build-Verzeichnis. Clientquellen
  und Datenhooks liegen jetzt in ihren Modulen. Fabric- und NeoForge-Modultasks werden fuer
  ihre gemeinsamen Testverzeichnisse auch unter `--parallel` serialisiert. Compile-only
  Modulabhaengigkeiten umgehen die Laufzeit-Auswahl nicht. Root `checkModuleData` fuehrt
  alle deklarierten Scripts aus. Launch Hub nutzt nur noch den Manifestvertrag.
- `newmod.py` liefert Fabric-/NeoForge-Serveradapter, Struktur, Client-Smoke und Datenhook;
  Auswahl wird automatisch aktualisiert. Vertrag: `docs/MULTIMOD.md`. Doppelte Resource-
  Wurzeln aus den zusammengefuehrten Loaderbuilds entfernt. Wiki erkennt Kreativtab-IDs
  nicht mehr als Items und nutzt exakte Featurekapitel als Registry-Prosa.
- Voller Bestand 26.3: **1562/1562, alles gruen**, Run `2026-09-30T14-28-00Z-a00d`.
  Testzentrale in beiden separaten GameTest-Welten vollstaendig gebaut; alle Items/Bloecke
  abgedeckt, Stations-/Befehlstests gruen. Besitzerwelt nicht angefasst.
- Integration + Money + Riding: **47/47, alles gruen** (1 + 10 + 10 + 13 + 13),
  Run `2026-09-30T14-29-34Z-e94b`. Beide Fabric-Modulclients seriell nach Prozesspruefung:
  **6/6 Screenshot-Pruefpunkte, alles gruen**, Run `2026-09-30T14-30-55Z-9035`.
- Neue reine Python-Registrierungstests: Launch Hub **44 Tests gruen**, inklusive
  Scaffold-Discovery, mehrerer Katalogdateien, Namespace-/Reporttrennung, Clientselector
  und unsicherer Pfade/Tasks. Wiki **13 Tests gruen**; Devserver **121 Tests gruen,
  ein bestehender Skip**. `wiki/generate.py --all` und `--all --check` gruen;
  Lang-Schluessel eindeutig. Quest-Lang-Reihenfolge fuer gemeinsame/26.3-Ressourcen aktuell.
- Echte Gegenprobe nach dem Codecommit: `newmod.py pluginprobe "Plugin Probe"` ohne
  manuelle gemeinsame Wiring-Aenderungen. **2/2 Servertests, alles gruen** auf Fabric/NeoForge,
  Run `2026-09-30T14-36-39Z-e66b`; **1/1 Client-Smoke, alles gruen**,
  Run `2026-09-30T14-37-51Z-065d`. Generischer Datenhook ebenfalls gruen; Clientresultat
  traegt `pluginprobe:pluginprobe-title`. Probe anschliessend unter dem ignorierten
  `scratchpad/plugin-probe-completed` archiviert, Manifest/Auswahl exakt wiederhergestellt,
  laufender Katalog aktualisiert und Integrations-JAR-Auswahl neu vorbereitet.
- Abschliessendes `gradlew.bat :integration:prepareIntegrationMods check -q --no-daemon`:
  **GRADLE_EXIT=0**, Ausgabe gelesen; gemeinsamer Code kompiliert auch fuer 26.2.
- Nicht geprueft: Forge-Ziel (ausdruecklich nicht gestartet), andere MC-Laufzeitlinien,
  NeoForge-Clientdarstellung, vollstaendige SimpleBuilding-Clientsuite, Besitzerwelt und
  beliebige Modpackkombinationen. `mc1_21_11` und `mc26_4` unveraendert. Keine neue Pixelkunst.
  Keine Besitzerentscheidung fuer diesen Infrastrukturvertrag erforderlich. Kein Push,
  kein Merge dieses Branches in master; Integration dort bleibt der Besitzersession.

## Simple Models (Codex, 2026-09-30, codex-port-models)
- Fortsetzung nach API-Abbruch: vorhandene Arbeit geprueft, in d2af64c1 gesichert; Ordnerlimit mit Regression in abe43d67. Additives 26.3-Modul, keine anderen MC-Linien/Quellrepo geaendert, kein Push/Merge.
- Serverfreigegebene item_model-Zuweisung/Entfernung im Vanilla-Amboss, Suchbrowser mit Tags/Autor/3D-Itemvorschau, Servereinstellungen/Importhilfe, begrenzter Ordnerkatalog, Reload/Join-Sync, EN/DE und Balance-/Wiki-Vertrag. Standard nur OP; Ressourcenverteilung ueber Vanilla-Serverpack. Kein versteckter Itemname oder Gameplaybonus.
- Bestand Fabric/NeoForge 1562/1562 plus Integration 1 und Mod 32 gruen (2026-09-30T15-30-36Z-a0ee). Finale Modulpruefung nach Ordnerhaertung: 32/32 alles gruen (2026-09-30T15-56-03Z-8478). Testzentralen und volle Item-/Blockabdeckung im isolierten Gesamtlauf gruen.
- Finaler Fabric-Client 5/5 alles gruen (2026-09-30T15-59-22Z-0133); Browser/Einstellungen/Hilfe/Ambossbilder angesehen und unter modules/simplemodels/previews gesichert. Wiki --all und --all --check sowie Datenhook gruen.
- Offen: Besitzerabnahme mit echtem Modellpack, NeoForge-Client, deutsche UI, echter Serverpack-Download und Besitzerwelt. Hinzufuegen/Entfernen ueber Admin-Dateien/Importordner, keine Remote-Uploads; platzierte Blockgeometrie nicht Teil der Item-Komponente. Forge 26.3 braucht Einstieg/Netzwerk/Client-/Testadapter; 26.2/1.21.11/26.4 erst separater Release-Port. Vollinventar/Grenzen: docs/modules/simplemodels.md.
- Abschliessendes gradlew.bat check -q --no-daemon im Worktree: GRADLE_EXIT=0, Ausgabe gelesen; shared 26.2 kompiliert. Keine gemeinsamen Wiring-Aenderungen, nur Manifest/Auswahl und generierte Wiki-Moduldaten/Index. Keine Besitzerwelt veraendert.
## Simple Fun (2026-09-30, codex-port-fun)
- Read-only source b778d9bb (1.2.0, 37 tracked Java files); inventory and limits: docs/modules/simplefun.md. Additive Fabric/NeoForge 26.3 port retains IDs/config keys; eight cosmetic delights, pig/cow/chicken/sheep charged-creeper heads, 26 server settings, bilingual wiki/balance/JEI/quiet advancement hints.
- Full 26.3 gate: 1627/1627, alles gruen (1562 existing + 1 integration + 64 module), run 2026-09-30T15-47-00Z-8618. Final private/LAN/dedicated glass safety: 2/2, alles gruen, 2026-09-30T15-56-02Z-6ca2. Test centres rebuilt in both separate existing GameTest worlds; coverage/stations green. Module item/block coverage is isolated in its own tests/data gate. Owner world untouched.
- Fabric client: 4/4 screenshot checkpoints, alles gruen, 2026-09-30T15-41-55Z-f8d3. Title/world/config plus standing/wall/ground/worn head models viewed; evidence modules/simplefun/docs/previews. Special item-model nesting and Windows decoding repaired. No new pixel art; source assets and Vanilla mob textures preserved.
- Default/all-module wiki generation/checks passed; actual booted registry export under module generated/resources/wiki. Final gradlew.bat check -q --no-daemon: GRADLE_EXIT=0, output read, includes shared 26.2 compilation, balance and module data. Source repo clean; no SimpleBuilding implementation or deferred MC line changed. No push/merge.
- Decisions: no duplicate SimpleBuilding content; preserved global minecraft:knockback datapack override is documented. Effective knockback capped, throw/damage/entity lifetime bounded, No Damage direct melee only. Glass destruction refuses published LAN and dedicated servers even when enabled; private single-player retains interaction/world-border checks. Client config screen writes a local restart draft, never sends server settings.
- Open verification: NeoForge client/UI, actual upgraded source world, owner modpack/world, German screenshots, interactive JEI/Jade, real two-client LAN and subjective audio. Forge 26.3 needs entrypoint/config/trade/loot/render/test adapters; 26.2/1.21.11/26.4 remain a separate release port after approval. No subagents used.
## Simple Visuals (Codex, codex-port-visuals, 26.3)
- Read-only Quelle 099a45af, bestehende Quell-Aenderung gradle.properties erhalten. Inventar/Config/Registry- und Effektvertrag: docs/modules/simplevisuals.md. Fabric/NeoForge additiv, keine Gameplay-Items oder Rezepte aus toten Sprachresten.
- Forge 26.3 benoetigt Loader-/Config-/Clientcommand-/Testadapter und Clientabnahme. 26.2/1.21.11/26.4 bleiben eigener Port-Run nach Freigabe; keine Quelltexte dort geaendert. Verifikationsergebnisse folgen als angehaengte Zeilen.
- Abschluss Simple Visuals: 1599/1599 voller 26.3-Serverlauf; danach 36/36 Modul-Serverfaelle, 5/5 Fabric-Clientpunkte, PNG-Sicherheitsgegenprobe 2/2. Wiki/--all/check und abschliessendes Gradle-check Exit 0. Testzentrale/Abdeckung gruen. Vorschauen und genaue Grenzen in docs/modules/simplevisuals.md; NeoForge-Client, echte Upgrade-Welt, Mehrspieleroptik/Last und Simple-Models-Zusammenspiel nicht abgenommen. Kein Push/Merge.

## Simple Sounds (Codex, 2026-09-30, codex-new-sounds)
- Neues additives Fabric/NeoForge-26.3-Modul, zwoelf datengetriebene Vanilla-Soundgegenstuecke zu Simple Visuals, fuenf globale Stufen und je Effekt Override/Off. Lokale Kosmetik ohne Pakete/Gameplay; Cap .25, vier Sounds/Tick, zwei/Spieler, begrenzte Cooldown-Historie, Welt-/Spielerwechsel setzt zurueck. Config-Tabs/Defaults/EN-DE/Wiki/Balance und Manifest-Tests; keine Items/Bloecke/Rezepte/Audio-/Pixelkunstdateien.
- Modul-Server 34/34 alles gruen (2026-09-30T16-34-09Z-c2c1), Fabric-Client 3/3 alles gruen (2026-09-30T16-35-28Z-342e); Welt/Config-Bilder angesehen, modules/simplesounds/previews. Beide isolierten Testzentralen samt Abdeckung 10/10 alles gruen (2026-09-30T16-36-44Z-b753). Finales gradlew.bat check -q --no-daemon Exit 0, Wiki-/Balance-/Moduldaten und shared 26.2-Kompilierung gruen. Keine gemeinsame Implementierung geaendert; Scaffold-Auswahl und automatisch generierte Wiki-Moduldaten/Index aktualisiert.
- Offen: subjektive Soundabnahme, alle Umgebungsbedingungen, NeoForge-/deutsche UI, Langzeitlast/Modpacks und Besitzerwelt. Keine komplette Bestands-Serversuite/Clientsuite, Forge-/andere Linien-Laufzeit. Entscheidungen: Vanilla-only, nur lokaler Spieler, eigene Intensitaet ohne automatisches Folgen der Visuals. Details docs/modules/simplesounds.md. Kein Push/Merge, keine mc1_21_11/mc26_4-Quellaenderung.
## Simple Quality of Life 26.3 (Codex, 2026-09-30, Branch codex-port-qol)
- Read-only Quelle: HEAD 9272a4ee08211c99caa52419bfde9a336991fff5 plus bereits vorhandene uncommittete Multiloader-Konversion; aktive common/fabric/neoforge-Quellen 1.0.6 inventarisiert. Quelle nicht geschrieben. Vollstaendiges Inventar, Config/IDs/Befehle/Grenzen/Kollisionen: docs/modules/simplequalityoflife.md.
- Additives Fabric-/NeoForge-Modul: Crawl/Auto-Walk, Klettern/Rutschen, Pulverschnee/Federfall, sechs Erntepflanzen, Lava-Ofenbefuellung, Schaerfe-Pflanzenschnitt, Haltbarkeitsbonus, stumme/dauerhaft junge Mobs, Piglin-Gold, Wetter und Vault-Pausen. Keine eigenen Items/Bloecke/Mobs/Verzauberungen/Rezepte; tote Quell-Lang-Namen nicht als neue Register erfunden. Keine neue Pixelkunst. Zusaetzliche Ideen nur in docs/modules/simplequalityoflife-ideas.md.
- Mod-ID, alte Config-Schluessel/JSON-Struktur, Befehle und Vault-Schluessel SimpleBuildingLootTimes erhalten. Keine internen Cross-Mod-Imports. Neue Server-Schalter fuer manuelles Kriechen und Vault-Resets; letzterer stellt Vanilla-Einmalbelohnung wieder her, ohne Daten zu loeschen.
- Server-Caps: Kletterwert 0.2-0.4, Rutschwert 0.15-0.8, Bonus 1-1.5, Haltbarkeitsschwelle 0.8-1, Vault 1-36500 Tage; begrenzte Listen/JSON. Server prueft Bewegungspakete mit gemeinsamem Tickbudget, Reichweite/Baurechte/Weltgrenze/Loader-Break-Veto und Aktionspausen. Keine Modul-C2S-Konfig-/Pose-/Transferpakete. Auto-Walk sendet nur Vanilla-Vorwaertseingabe, keine Geschwindigkeit/Reichweite.
- Quellfehler behoben: wirksamer Kletterbonus statt spaeterer Vanilla-Ueberschreibung; kein lokales Autowalk-Override der Serverfreigabe; keine ungeschuetzten Pflanzenaktionen; keine Beute vor erfolgreichem Erntemutationsschritt; Vault-Getter ist beim Laden ohne Level null und verlor gespeicherte Zeiten, jetzt direkter Datenfeldzugriff. Crawl nutzt Vanilla-Pose plus S2C-only crawl_state fuer Selbst-/Tracking-Sync, keinen zusaetzlichen Vanilla-Datentracker auf NeoForge. Deutsches Quell-Mojibake korrigiert.
- Eigener Manifest-Testvertrag/Launch-Hub-Discovery ohne gemeinsame Build-/Runner-/Hub-Sonderbloecke. Nur Scaffold-Auswahlzustand und generierte Wiki-Ausgaben ausserhalb Modul/Doku/Balance/Manifest. 38 zweisprachige Wiki-Kapitel; benannte Zahlenkonstanten und balance/simplequalityoflife als Producer-Vertrag. SimpleBuilding und andere Modulquellen unveraendert.
- Bestand 26.3 **1562/1562 gruen**, Integration **1/1 gruen** im Lauf 2026-09-30T16-13-25Z-0a65. Dieser Gesamtlauf war wegen eines neuen Modul-Klettertests noch rot; die Bestandsergebnisse sind getrennt davon gruen. Testeingang korrigiert (Sprungeingang statt von move zurueckgesetzter Kollisionsmarkierung).
- Finale vollstaendige Modulkataloge **48/48, alles gruen**, je Loader 24, Lauf 2026-09-30T16-33-57Z-ed7c. Echte C2S-Bewegungspakete, Caps/Spam, alle Funktionsfamilien/Schalter, Lang-Defaults, echte Vault-Blockentity-Speicherung und Cross-Mod-Ofen/Werkzeug/Breeze+Federfall geprueft. Cross-Mod-Erweiterung zusaetzlich **2/2 gruen**, Lauf 2026-09-30T16-27-22Z-cd77.
- Fabric-Modulclient nach Prozesspruefung seriell: **5/5 Screenshot-Pruefpunkte, alles gruen**, Lauf 2026-09-30T16-36-08Z-5e43. Titel -> Welt, beide Tastenbindungen, Config-Sync, echter /crawl-Netzwerkablauf mit bestandsfester Clientpose und Rueckkehr, EN/DE-Config. DE-Bild angesehen; Test wartet auf Ende der Ressourcen-Ladeblende.
- wiki/generate.py --all und --all --check gruen. Abschliessendes gradlew.bat check -q --no-daemon: **GRADLE_EXIT=0**, Ausgabe gelesen, inklusive gemeinsamer 26.2-Kompilierbarkeit und Modul-Client-Harness. Voller Bestand baut die Testzentrale in beiden separaten 26.3-Testwelten; Item-/Blockabdeckung gruen. Besitzerwelt unberuehrt.
- Nicht verifiziert: NeoForge-Clientdarstellung/physische Tastenbedienung, reale alte Quellmod-Spielwelt, Mehrspieler mit echten Fremd-Claim-Mods und beliebigen Dev-Mod-Kombinationen, Besitzerwelt-Neubau. Forge 26.3 braucht Loader-/Netzwerk-/Client-/Event-/Testadapter; 26.2/1.21.11/26.4 erst nach Release-Freigabe. Keine Quelltexte dort geaendert. Kein Push/Merge.
- Besitzer-Abnahme: Quell-1.5x-Bonus wirkt auch auf SimpleBuilding-Werkzeuge; optionale Dev-Mod-/Tasten-Kombinationen und Ideenliste pruefen. Kein weiterer Entscheid fuer diesen Port erforderlich.

## Simple Tweaks Audit / Kompatibilitaet (2026-09-30, codex-port-tweaks)
- Quelle bb8f976 plus vorhandene lokale Aenderungen voll abgeglichen; 56 Java-Dateien, 19 Rezepte, alle Configfelder und Echo-Library (mr_echo_compass, AGPL-Metadata) inventarisiert. claims-only Branch nur mit git show gelesen; Quellrepo/Hashes/dirty-Status unveraendert. Details: docs/modules/simpletweaks.md.
- Alle Nicht-Claim-Spielmechaniken bereits in SimpleBuilding: keine Duplikate. Additives Fabric/NeoForge26.3-Modul loest alte simpletweaks Item-/Block-/BE-/Komponenten-IDs auf vorhandene SB-Objekte auf; nur claim_deed bleibt eigenes inaktives Altitem, CustomData unveraendert, Textur original. Claims bleiben bewusst zurueckgestellt, keine Claim-Dateien gelesen/geschrieben, keine Schutzbehauptung.
- Manifest/Plugin-Tests, EN/DE-Wiki und Lang, Registryexport aus echtem GameTest, Balance-Produzentenvertrag und 16x-Alt/Neu-/Clientbilder. Keine shared Wiringblocks oder SimpleBuilding-Implementierung geaendert; nur Scaffold-Auswahl und generierter Wiki-Index zusaetzlich. Kein Push/Merge.
- Finale Serverpruefung 1587/1587 alles gruen (1562 Bestand +1 Integration +24 Modul), Run2026-09-30T16-46-21Z-da1c; Testzentralen in beiden isolierten Bestandswelten neu aufgebaut, Item-/Blockabdeckung gruen. Fabric-Modulclient3/3 alles gruen, Run2026-09-30T16-34-44Z-6351; Urkunde im Inventar und16x-Vorschau angesehen.
- Offene Besitzerentscheidungen: Claims freigeben? Separater SB-Sicherheitsrun fuer Booststaerke/absolute Velocity/Rate und XP-/Launch-/Kill-/Spawn-Radiuscaps; Bestand hat hier weiterhin keine ausreichenden Obergrenzen. Alte Library-Compasse bleiben ladbare Vanilla-Compasse mit Daten, ohne Library-Teleport; automatische Echo-Sounder-Migration separat entscheiden.
- Nicht verifiziert: NeoForge-Client, echte hochgestufte Quell-/Besitzerwelt, deutsche Screenshots, interaktives JEI/Jade, subjektive Sounds. Forge26.3 braucht Registrywrapper-/Aliasadapter, Loader-/Test-/Clientwiring; kein Forge-Release im Manifest.26.2/1.21.11/26.4 erst separater Release-Port nach Abnahme; Quellen dort unveraendert.
- Abschliessendes gradlew.bat check -q --no-daemon: GRADLE_EXIT=0, Ausgabe gelesen, shared26.2 kompiliert und alle Standardgates gruen. Keine Release-Freigabe fuer die oben dokumentierten SB-Cap-Luecken.

## Forge Modules 26.3 (Codex, codex-forge-modules, 2026-09-30)
- Five experimental opt-in adapters: simplemoney, simpleriding, simplemodels, simplefun, simplevisuals. Separate module commits; same catalogue IDs/bodies, own worlds/reports/registry and loot hooks. No push/merge; deferred MC source trees unchanged.
- Generic gradle/module-forge.gradle reads manifest test task/report/testMods, translates resource conditions, supplies Mixin manifest/launcher configuration and excludes unavailable optional GUI sources. No module-specific root/integration/runner/Hub wiring. Repeatable recipe: docs/MULTIMOD.md.
- Money 10, Riding 13, Models 16, Fun 32, Visuals 18: final combined Forge module run **89/89, alles gruen**, 2026-09-30T17-19-53Z-76ff. Full SimpleBuilding Forge suite **782/782**, 2026-09-30T17-10-04Z-6908; that combined run was NOT globally green because an intermediate module launcher-isolation attempt failed. All module targets were subsequently repaired and verified separately in the green final run; no skipped catalogue cases.
- Standard Fabric/NeoForge 26.3 + integration + all six existing module server suites (including Sounds): **1775/1775, alles gruen**, 2026-09-30T17-12-25Z-3f33. Existing test-centre worlds rebuilt and full item/block coverage passed on Fabric/NeoForge/Forge; owner world untouched.
- All five explicit Forge compile tasks without forge_runs: FORGE_COMPILE_EXIT=0. Default/all wiki generation and checks current. Riding data gate recognizes Forge/support chapter; module data checks exclude generated runtime worlds instead of parsing changing game files.
- Forge config remains module-owned JSON with original server bounds; unavailable Cloth GUI excluded, no cross-mod shim dependency. Fun config is generated from canonical source minus inert GUI annotations, Piggy uses tracked entity data, client render setup is dist-guarded. Models channel handler is client-only and marks payload handled; browser keeps inventory/anvil access, no Forge mod-list button. Visuals client commands/Models detection remain client-only.
- Not verified: Forge clients/render/audio/config interaction, real-player Models network delivery/serverpack, remote Piggy display, optional JEI/Jade/Curios/Cloth integrations, owner-world upgrade/centre, general Forge all-module integration launcher. No client tests or new pixel art. Sounds and future modules need their own adapter; other MC runtime lines deferred. Forge stays experimental/off by default pending owner acceptance.
- Final default `gradlew.bat check -q`: **DEFAULT_GATE_EXIT=0**, output read, includes shared/26.2 compilation, all module data/balance/atlas/wiki gates and 18 wiki tests. Forge projects remain absent without the property.
- Independent final Forge main-line run: **782/782, alles gruen**, `2026-09-30T17-24-22Z-5e9c`; no failed/missing catalogue cases. This supersedes the mixed intermediate run as the clean Forge base verification record.
## SB-HARDEN (2026-09-30, Codex, codex-sb-harden)
- Bestehende SimpleBuilding-Luecken aus dem Tweaks-Audit geschlossen; kein Modul-Duplikat. Alle 13 betroffenen Optionen mit benannten Obergrenzen, finite Pruefung, Warnung und reinem In-Memory-Clamping beim Laden. Defaults/Namen/Reiter unveraendert; Bereiche in EN/DE beider Ressourcenorte und generierter Wiki-Tabelle. Werte/Begruendungen: docs/SB-HARDEN.md.
- Boost: Staerke maximal 1,2; gesamte Velocity maximal 3 Bloecke/Tick; drei Boosts je 100 Server-Ticks, Budget unabhaengig von Spawn-/Pad-Refill und Ausruestung. Gleitflug/Ladung/Ausruestung weiterhin serverseitig geprueft. XP/Launch/Kill/Spawn/Zeiten/Cooldown/Laser auch an Runtime-Lesern begrenzt. Rezeptfilter erkennt amethyst_lens samt Legacy-ID.
- 19 neue Serverfaelle je Loader: jede Option mit Extremwert und NaN/Infinity-Verweigerung/Fallback, alle Defaults, echte Handler-Spam-/Refill-/Fenstergegenproben, falscher Zustand/Ausruestung/Ladung, riesige/nicht endliche Staerke, echte Configbefehle, XP/Launch-Runtime und Rezeptregression. Bestehender Default-Katalog um die neuen statischen Konstanten erweitert, keine Default-Erwartung geaendert. Balancing-Extractor liest alle Caps; 12 Extractor-Tests gruen.
- Voller Hauptlinienlauf **1600/1600, alles gruen**, Fabric/NeoForge je 800, Run `2026-09-30T17-23-20Z-45d2`; Integration **1/1, alles gruen**, Run `2026-09-30T17-25-45Z-47ff`. Testzentralen samt Item-/Blockabdeckung in beiden isolierten GameTest-Welten gruen. Fruehere rote Laeufe durch Float-Testvergleich/Namensformat und fehlende statische Katalogeintraege korrigiert.
- Abschliessendes `gradlew.bat check -q`: **GRADLE_EXIT=0**, Ausgabe gelesen (scratchpad/harden-complete-gate.log), einschliesslich shared/26.2-Kompilierung und aller Standardgates. Wiki --all/--all --check Exit 0; vier Sprachdateien ohne doppelte Schluessel, Tweaks-Texte beider Orte identisch. Keine neue Pixelkunst, keine mc1_21_11-/mc26_4-Quellaenderung, kein Push/Merge.
- Nicht verifiziert: Minecraft-Clients/gerenderte Config, subjektives Boost-/Pad-Balancing, Besitzerwelt/Testzentralenbau dort, echte Mehrspieler-/Modpack-Langzeitlast, Forge und andere MC-Laufzeitlinien. Besitzer sollte die festen Bewegungsgrenzen im Spiel abnehmen; kein weiterer Implementierungsentscheid noetig.
## RIDING-FOLLOWUP (2026-09-30, Codex, codex-riding-followup)
- Nautilus/Zombie-Nautilus: Tailwind, server-owned Leaping dash, Vanilla cooldown and optional public Enderite armor synergy. All 18 server config options have bounds, three GUI tabs and bilingual defaults/tooltips; separate producer balance metadata retained.
- Per-connection movement/packet budgets, server attribute envelopes, finite coordinates/pitch, loaded chunks/full world-border box, jump charge/player identity/cooldown/replay checks, dismount budget retention and server collision ground flags. Ghast physical speed uses square-root attribute scaling; combined steering/ascent and Nautilus momentum are bounded. No new claim, chunk-ticket, inventory or entity-spawn gameplay path.
- Full 26.3 suites: 1562/1562, alles gruen, run 2026-09-30T17-29-33Z-786a. Test centres rebuilt and item/block coverage passed in both isolated GameTest worlds; owner world untouched.
- Module Fabric/NeoForge plus integration: 53/53, alles gruen, run 2026-09-30T17-31-41Z-3c3b (26 + 26 + 1). Covers features/config/lang/security, actual forged packet dispatch, real aquatic dash, armor and public cross-mod interaction.
- Fabric client smoke: 3/3, alles gruen, run 2026-09-30T17-33-00Z-5b61. Valid/invalid Nautilus dash, Ghast movement including combined steering/ascent, 18 config rows in three tabs; config screenshot inspected.
- Wiki --all regenerated and --all --check passed. Final gradlew.bat check -q: GRADLE_EXIT=0, including final all-module wiki check and shared/26.2 compilation. Log scratchpad/riding/continuation-gate.log. No other module/root implementation changes; unrelated generator changes were only line endings. No new pixel art.
- Commits before final report: 1a9e5a21 (features/config/security), a692877b (ground claims/aquatic and steering regressions), ee7919a1 (wiki/docs). Worker branch only, no push/merge.
- Not verified: NeoForge client rendering, upgraded source-mod saves, owner-world test-centre rebuild, real terrain/latency/third-party physics acceptance, Forge/deferred runtime lines. Owner acceptance of 4x aggregate speed and movement envelopes remains open. mc1_21_11/mc26_4 source untouched.

## Forge Modules 2 (Codex, codex-forge-modules2, 2026-09-30)
- Experimental opt-in Forge 26.3 adapters: Simple Sounds (00c63301, 17 cases), Simple Quality of Life (109d7584, 24 cases), Simple Tweaks (db57b414, 12 cases). Each module passed its complete catalogue before its separate commit. No push/merge; simpledimensions and deferred MC source trees untouched.
- Existing manifest-driven Forge convention reused; no root/integration/runner/Hub wiring changes. Module-owned metadata, Mixin manifests, pack metadata, isolated worlds/reports and identical canonical catalogue IDs/bodies. Additional QoL Forge DENY/cancel probes exercise real loader break vetoes before the shared permissions case.
- Sounds keeps client-only tick/audio budgets and JSON settings; Cloth screen/harness excluded. QoL generates its canonical config/main class with only platform calls/GUI markers adapted, retains fields/defaults/bounds and module-owned bounded JSON load/save; S2C config/crawl channel, login/tracking, keys/tick/logout, commands, interaction DENY checks and farmland hooks. No cross-mod shim packages. Its experimental Forge channel currently requires the module on both client/server. Tweaks uses native ForgeRegistry aliases plus NamespacedWrapper lookups; same canonical saves, no duplicate gameplay or active claims.
- Individual module runs: Sounds 17/17 alles gruen (2026-09-30T17-38-02Z-ad0c), QoL with real Forge veto probes 24/24 alles gruen (2026-09-30T17-47-20Z-5f26), Tweaks 12/12 alles gruen (2026-09-30T17-50-25Z-49d3). Earlier starts found a concurrent Mavenizer file-copy conflict and test-probe compile typo; both corrected before these green records.
- Full combined final verification: 2828/2828, alles gruen, 0 red, 2026-09-30T17-52-13Z-77a1. Fabric/NeoForge existing 800 each, Forge existing 801, integration 1, all eight manifest module server catalogues on all three loaders 426. No skipped/missing catalogue cases. Test centres rebuilt with complete SimpleBuilding item/block coverage in all three isolated existing GameTest worlds; legacy deed covered by its module registry/codec tests. Owner world untouched.
- Explicit Forge compile for the base and every module project, without forge_runs: FORGE_COMPILE_EXIT=0 (scratchpad/forge-modules2-compile.log). Final default gradlew.bat check -q: DEFAULT_GATE_EXIT=0, output read (scratchpad/forge-modules2-default-gate.log); shared/26.2 compilation and all standard module/balance/atlas/Jade/wiki gates remain green. Wiki --all generation/check and final three module checks current; 18 wiki tests green. Final documentation corrects two PowerShell-replaced German umlauts; generated wiki data/index refreshed, no hand edits or other module lang changes.
- Not verified: Forge clients/audio/rendering/keys/GUI, real-player QoL network delivery or config interaction, old/owner-world upgrades, optional integrations, arbitrary modpacks, a general Forge all-module integration launcher, other MC runtime lines. No client tests or new pixel art. Cloth GUI remains unavailable for Sounds/QoL; Forge remains experimental/off by default pending owner acceptance. No further implementation decision needed; owner should accept client behavior before a release/default switch.

## Money Links (Codex, 2026-09-30, codex-money-links)
- Simple Money add-on: 243 conditional buy offers (238 canonical SimpleBuilding items, five Simple Fun items), nine module-owned fallback tables, public registry ids only. Empty/no-survival-item modules explicitly covered; creative spacer, legacy aliases, inert deed and integration token excluded. Original 47 Money offers and other mods' tag contributions preserved; no shared implementation or foreign-module folders changed. Producer details: docs/modules/simplemoney.md.
- Bounded server price formula from current age/loot model and recipe material tiers, six balance-extractable defaults, stock/quantity caps, no discounts or XP on new offers. Buy-only decision avoids new reverse-money edges. Static salvage audit follows real single-material recipe/uncrafting chains against maximally discounted legacy diamond exchanges; arbitrary third-party economies are not certified.
- Persistent overworld player budget across items/merchants/dimensions: eight purchases/24000 elapsed ticks, 100-tick cooldown by default, hard caps and bounded SavedData. Normal pickup and quick-move validate before transfer; payment success records the budget. Config tab saves a local draft without mutating active server values. EN/DE wiki price tables, bill tooltip and optional JEI info; client screenshots preserved under modules/simplemoney/previews. No new pixel art.
- Final complete Money suites 34/34, alles gruen, 2026-09-30T18-07-56Z-1bed. Feature-off resource test 2/2, alles gruen, 2026-09-30T18-04-11Z-0abd; isolated configs restored byte-for-byte. Real missing Simple Fun on NeoForge and loaded Simple Fun on Fabric exercise conditional decoding. Integration 1/1, alles gruen, 2026-09-30T17-59-38Z-a32f. Both isolated test centres rebuilt with complete item/block coverage: 10/10, alles gruen, 2026-09-30T17-58-37Z-bb81; owner world untouched.
- Fabric client 4/4, alles gruen, 2026-09-30T18-05-38Z-000b; new tab opens and contains all seven settings, screenshots viewed. Default and module wiki generation/checks, all-module wiki check and data/salvage gates passed. Final gradlew.bat check -q --no-daemon: FINAL_GATE_EXIT=0, output read, including shared 26.2 compilation and 18 wiki tests. No deferred-line source edits, no push/merge.
- Owner acceptance: proposed tier prices, broad merchant-pool additions and buy-only policy. Existing offers keep their price/stock until replaced. Not verified: NeoForge client, German screenshots, interactive JEI/Jade, genuine multiplayer/owner saves, arbitrary datapacks or other economy mods, full existing SimpleBuilding server/client suite. Forge Money Links needs its own loaded-mod/mixin adapter; Forge and other Minecraft runtime lines remain deferred.
## Simple Dimensions — Entwurf, Portierung offen (2026-09-30, Codex)
- Branch codex-port-dimensions, Entwurfscommit e7625a9d: docs/modules/simpledimensions.md. Beide Quell-READMEs, instructions+ und relevante Portal-/Config-/Loaderquellen read-only verglichen. Neuester Java-Stand hat andere Portalrezepte als README/alte Besitzeranweisung.
- Entwurf: drei Dimensionen, Quell-Configschluessel, persistenter Namensraum simpledimension, Legacy light_blue_portal, Kollisionen mit SimpleBuilding, sichere Rueckwege und Testplan. Sicherheitsbefunde: generierte wertvolle Rahmen duplizieren Ressourcen; clearIfObstructing kann Bauten loeschen; One-way-/Plattformschalter erlauben unsichere Ankunft. Nicht ungeprueft portieren.
- Implementierung NICHT fertig. newmod-Token-Geruest lokal unter ignoriertem scratchpad/dimensions-scaffold-unported archiviert; Manifest und Integrationsauswahl ohne Modul wiederhergestellt. Keine Dummy-Mod oder Dummy-Testresultate ausgeliefert.
- Offene Umsetzung: loaderneutrale Runtime, Fabric/NeoForge-26.3-Adapter, automatische Datapacks, Config-GUI/Bounds, Legacy-Weltbelege, Modul-Wiki/Balancedaten und kompletter Pflicht-Testkatalog. Forge 26.3 sowie 26.2/1.21.11/26.4 bleiben spaetere separate Ports.
- Kein Gameplay-/Launch-/Worldgen-/Modulserver-/Clientnachweis in diesem Run. Kein Neubau der Besitzer-Testzentrale, kein Push/Merge; Quellrepos und andere Minecraft-Linien unveraendert.
- Dokumentationsstand verifiziert: wiki/generate.py und --check Exit 0; abschliessendes gradlew.bat check -q --no-daemon GRADLE_EXIT=0, Ausgabe gelesen (18 Wiki-Tests gruen; Compiler-Deprecationwarnungen). Keine Serversuite ausgefuehrt; Gate beweist keinen Modul-Launch. Generierte Wiki-Dateien nur inhaltsgleiche Zeilenenden, nicht committet.

Simple Dimensions auf codex-port-dimensions implementiert: drei Dimensionen, sechs Glowstoneformen, sichere persistente Rueckwege, Config/EN-DE/Wiki/Balance/Manifesttests. Fabric+NeoForge 26.3; Forge und 26.2/1.21.11/26.4 offen. Details/Belege: docs/modules/simpledimensions.md.


## DIMFIX (2026-09-30, Codex, codex-dimfix)
- Produktfehler direkt reproduziert: MOTION_BLOCKING ignoriert nicht kollidierendes Gras; die Luftpruefung verweigert dann den Plattformbau. Rote Gegenprobe 0/1 (2026-09-30T18-11-32Z-2ed8), Log: short_grass, Berechtigung true, lastBuild=-1, 81 Portale. WORLD_SURFACE hebt die unveraendert begrenzte Plattform ueber alle bestehenden Bloecke. Keine Pflanzen/Bauten entfernt; Sicherheitspruefungen und alle Reiseassertions erhalten. Fix/Wiki/Regression: 4fdfebe7.
- Mining-Test erzwingt die Vegetationsbedingung unabhaengig vom Seed und prueft Pflanzenerhalt, sichere Landung, echte Reise, 0.5-Skalierung und exakten Rueckweg. Temporaere Logs und feste Koordinaten entfernt. Keine anderen Modulimplementierungen oder mc1_21_11/mc26_4-Quellen geaendert.
- Beweisgrenze: frische Welt und read-only Kopie der fehlgeschlagenen sbgate-Welt liefen an neuen Fabric-Standorten gruen. Historischer genauer Blockzustand nicht erfasst; dessen Zuordnung zur direkt bewiesenen Gras-Kollision ist eine Schlussfolgerung. Original-/Besitzerwelten unberuehrt. Details docs/modules/simpledimensions.md.
- Ausgabe gelesen: 68/68 alles gruen (2026-09-30T18-12-58Z-3fd9); zweiter fester Fabric-Gesamtlauf 34/34 (2026-09-30T18-15-03Z-fddb); Bestand 1600/1600 (2026-09-30T18-16-18Z-7405); Integration und alle neun Modulpaare 379/379 (2026-09-30T18-18-28Z-fa6b), einschliesslich drittem Fabric-Dimensionslauf 34/34. Alle 0 rot. Beide Hauptlinien-Testzentralen gebaut und komplette Item-/Blockabdeckung gruen.
- Default/all Wiki generate/check gruen. Abschliessendes gradlew.bat check -q GRADLE_EXIT=0, 19 Wiki-Tests und alle Standardgates einschliesslich shared/26.2-Kompilierung. Kein Push/Merge.
- Nicht verifiziert: exakte historische Terrainursache, Clients, Besitzerwelt/Zentrale dort, externe Claims, Forge und andere MC-Laufzeitlinien. Kein neuer Implementierungsentscheid erforderlich; Besitzer-Abnahme realer Reisen bleibt offen.

## Claims Stage 4 continuation (2026-10-01, codex-next-claims)
- Fortsetzung ab 7422a3ab; bestehende Stage-4-Entwuerfe/Tests erhalten, keine Stage-5/6-, Forge- oder anderen Linien-Ports. Naturschaden bleibt Vanilla; zurechenbare Angriffe pruefen Besitzer. Explosion/Feuer pruefen Bett-/Truhengegenstuecke. Kolben-Rueckzug nutzt eine mutationsfreie Vorschau ohne Kopf, samt Schleim-/Honigzweigen.
- Generischer Framework-/26.3-Vertrag fuer Besitzergrenzen; eigene verstaerkte/Brecherkolben samt Brennstoff, Mod-Hopper-Ausgabe und platzierte Attractors angebunden. Maschinen-Pruefevents bleiben fuer fremde Loader-Vetos offen; eigenes Werkzeugevent verwechselt sie nicht mehr mit Handabbau. Ausgeschaltet/ohne Anbieter keine zusaetzlichen Welt-/Claimdatenabfragen.
- Claims-Pruefung 64/64, alles gruen, Run 2026-09-30T23-45-13Z-23b2 (32 je Loader). Natuerlicher Fall/Ertrinken, Spielerhunger, indirekte Projektile/Wolken/Items, echte Bett-Explosionen, verbundene Kolben, Custom-Brecher/Hopper/Attractor; neue Faelle pruefen auch OFF und keinen Anbieter. Fruehere rote Laeufe samt Ursachen im Modulplan.
- Claims bleibt AUS und nicht einschaltbereit. Kupfergolem-Containertransfers und Crafter-Ersatzauswuerfe sind noch nicht vollstaendig geschuetzt; unbekannte Container konservativ gesperrt, komplexe Dispenser in Claim-Naehe gesperrt, beliebige Remote-/Modmutationen nicht zertifiziert. Vollstaendige Grenzen/Pruefbelege: modules/simpletweaks/CLAIMS-STAGE4.md und docs/modules/simpletweaks.md. Stage 5/6 bleiben separater Worker; kein Push/Merge/Client.
- Stage-4-Bestand ohne Claim-Anbieter: Kolben 56/56 (2026-09-30T23-48-11Z-d4a1), Hopper 52/52 (23-49-34Z-8de1), Attractor 6/6 (23-51-32Z-e9b7), Testzentralen/Item-/Blockabdeckung 10/10 (23-53-07Z-ac63), jeweils alles gruen auf Fabric+NeoForge 26.3. Konkrete weitere Aktivierungsblocker: Blitzentzuendung/Kupferreinigung ausserhalb der Feuer-Tickhooks. Besitzerwelt unberuehrt.
- Abschliessendes volles Worktree-Gate am 2026-10-01: gradlew.bat check -q, GRADLE_EXIT=0; Standardpruefungen und bestehende Shared-/26.2-/1.21.11-Kompilierung gruen, zwei Worker/2-GiB-Heaps. Kein Runtime-Port. Praezisierung zur Naturschaden-Policy: Blitzschaden wird als Entity-Quelle weiterhin konservativ verweigert; deshalb keine vollstaendige Naturschaden-Freigabe. Claims bleibt AUS/nicht einschaltbereit. Exakte 44-Dateien-Liste und Pruefprotokolle im Stage-4-Plan.
## Claims access worker — Stufe 5 (2026-10-01)
- Branch codex-next-claims-access, Basis 7422a3ab. Besitzer-Trust/Widerruf mit Offline-UUIDs, sicheren Online-Namen, atomarer Veröffentlichung und gemeinsamen Caps/Cooldown; explizite OP4-Verwaltung unabhängig vom optionalen OP4-Schutz-Bypass.
- Stufen 1–3 und 5 enthalten; Stufe 4 separat, ungemergt und nicht als abgeschlossen bestätigt. Claims bleiben standardmäßig AUS; Werkzeug-/Umwelt-/Automationsmatrix weiterhin nicht vollständig nachgewiesen.

- Stufe-5-Verifikation: 2026-09-30T23-18-06Z-4f7d, Fabric 21/21,
  NeoForge 21/21, **alles gruen 42/42** (Filter simpletweaks:*claims*).
  Echte Brigadier-Befehle, Online-Join/Offline-UUID, Zugriff/Widerruf, OP3/OP4,
  beide Bypass-Konfigurationen, Limits, atomare Fehler und bestehende Hooks geprüft.
  Der alte Strahl-Test lädt jetzt seinen Zielchunk vor dem Entity-Spawn; vorher
  war die Zielerfassung abhängig von der zufälligen Testposition.
  Wiki default/module generate/check und Modul-/EN-DE-Datenprüfung grün.
  Keine Clients, Besitzerwelt oder vollständige Sicherheitsmatrix geprüft.

## Claims access worker — Stufe 6 (2026-10-01)
- Stufe 5: 4ac2875a. Separater Stufe-6-Commit auf codex-next-claims-access: Dimensions über vorhandene öffentliche Protection-API, vollständige Portal-/Landeflächen, beide Reiseenden und sichere erlaubte Rückwege; keine interne Modulkopplung oder gemeinsamen API-/Root-Codeänderungen.
- Claims bleiben AUS; Stufe 4 ist separat/ungemergt und nicht als fertig bestätigt. Dimensions-Schalterbranch ebenfalls nicht enthalten. Vollständige gemergte Normal-/Integrationsmatrix bleibt beim Orchestrator.
- Modulvollsuiten: Dimensions 74/74 (2026-09-30T23-24-05Z-521d), Tweaks 66/66 (2026-09-30T23-37-40Z-b5cf). Claims-Nachprüfung 44/44 (2026-09-30T23-49-03Z-e365), abschließende Dimensions-Claimprüfung 10/10 (2026-09-30T23-51-37Z-36e1), jeweils beide 26.3-Loader und alles gruen gelesen. Details/Dateien/Grenzen: modules/simpletweaks/CLAIMS-ACCESS-VERIFICATION.md.
- Abschluss: gradlew.bat check -q mit max. 2 Workern GRADLE_EXIT=0; zusätzliche Dimensions-JARs gebaut und öffentliche Protection-API samt Fabric-/NeoForge-Metadaten in beiden Paketdateien nachgewiesen. Letzter echter Claims-/Portaltest 2/2, alles gruen, 2026-09-30T23-57-47Z-0885.
- Testzentrale in Testwelten aufgebaut; Abdeckung/Planvergleich auf beiden Loadern grün. Gesamtlauf 2026-09-30T23-53-58Z-9ce6: 9/10, NF-Knopftest einmal rot; unverändert einzeln 1/1 grün (2026-09-30T23-56-25Z-2d76). Sporadische Ursache offen, kein behaupteter sauberer 10/10-Lauf. Keine Besitzerwelt/Clients/anderen Laufzeitlinien geprüft. Kein Push/Merge.

## DIMENSIONS-SETTINGS (2026-09-30, codex-next-dimensions)
- Plan 37293dc4, Umsetzung f83a75c4. Drei standardmaessig eingeschaltete Serveroptionen skyblockEnabled/miningEnabled/travelEnabled im eigenen Reiter Dimensionen; atomare Speicherung, Anwendung auf dem lokalen Serverthread. Remote-Clients bleiben ohne Schreibpaket. Zündung sowie erste/verknuepfte Hinreisen pruefen dieselbe Serverfreigabe; erzeugte Rueckwege bleiben offen.
- Sechs Glowstoneboegen mit Zusatzlicht unveraendert; ausgelieferte Kupfer/Blaueis-Alternative entfernt. Alte enabled-Werte der drei vorinstallierten Definitionen werden ignoriert, neue Beispieldateien bieten sie nicht mehr an. Eigene Definitionen behalten den Legacy-Schalter; vorhandene Dateien werden nicht umgeschrieben. Keine neuen Portalformen/Aktivierungen.
- Modulserver Fabric/NeoForge: alles gruen, 76/76, 0 rot (38 je Loader), Run 2026-09-30T20-51-56Z-7330. Acht neue Faelle ueber echte useItemOn-/Servertick-Hooks und Persistenz; alle bisherigen Tests erhalten. Beide EN/DE-Ressourcen in beiden Loadern geprueft. Testzentralen samt Item-/Blockabdeckung: alles gruen, 10/10, Run 2026-09-30T21-05-46Z-cc12; Besitzerwelt unberuehrt.
- Finale ./gradlew.bat check -q: GRADLE_EXIT=0, Ausgabe gelesen; 23 JUnit-Tests (0 Fehler/Skips), 19 Wiki-Tests, Balance/Moduldaten/Atlas/Jade/Wiki und shared/26.2-Kompilierung gruen. Default-/Modul-Wiki generate/check gruen. Log scratchpad/dimensions-settings/final-check.log. Automatisch generierte Modul-Wiki-Daten/Index zusaetzlich aktualisiert; keine gemeinsame Implementierung, andere Modulsprachen oder anderen MC-Quellen geaendert.
- Nicht geprueft: Clients/gerenderter Reiter, Besitzerwelt, physischer Dedicated-Server-Neustart, externe Claims, komplettes zusammengefuehrtes Server-/Integrations-/Modulgate, Forge und andere MC-Laufzeitlinien. Einstellungen per Dateiladen/Runtime-Neuaufbau geprueft. Rest: Orchestrator integriert und startet python tools/ai/aitool.py gate --integration; Besitzer nimmt Reiter/Reisen/Mehrspieler und Zentrale in eigener Welt ab. Keine neue Implementierungsentscheidung, kein Push/Merge. Plan und Abweichungen: modules/simpledimensions/DIMENSIONS-SETTINGS-PLAN.md.

## NEXT-SMALL (2026-09-30, codex-next-small)
- Commits: 8148c989 Plan; b606e27d QoL; 7f3ffbbc Sounds/API. Kein Push/Merge. Plan und Abschlussbelege: modules/simplesounds/docs/next-small-plan.md.
- QoL: Standardfaktor 1.0, serverseitiger Cap 1.5 bleibt (+50% Schaden/Abbau bei ausreichender Haltbarkeit). Gespeicherte 1.5 bleiben erhalten; fehlender Wert/NaN/Infinity nutzt 1, endliche Ausreisser werden begrenzt. Vanilla, SimpleBuilding und komponentenbasiertes Fremdwerkzeug ueber echte Server-Player-Hooks geprueft; Haltbarkeit selbst unveraendert. EN/DE gemeinsam fuer beide Loader, Wiki/Moduldoku aktualisiert.
- Sounds: followVisuals standardmaessig an, rein clientlokal. Alle fuenf aktiven Visuals-Stufen 1:1; Effekt-Overrides gewinnen, ohne Anbieter/bei Opt-out eigene Stufe. Volume-/Spam-/Cooldown-Limits unveraendert. Notwendige Scope-Erweiterung: generische Framework-API 0.1.1 plus kleiner Visuals-Publisher/Regression und eingebettete Bibliothek in beiden beteiligten Modulen (Fabric/NeoForge). Keine internen Cross-Mod-Imports, keine Root-/Runner-/Hub-Sonderfaelle. Manifestvertrag bereits vollstaendig; nur NeoForge-Soft-Dependency hinzugefuegt.
- Ausgabe gelesen: QoL 48/48 alles gruen (2026-09-30T20-53-16Z-7aed); Sounds/Visuals/Integration 71/71 alles gruen (2026-09-30T21-22-48Z-11f3); verstaerkte echte Loaded-/Absent-Pruefung 2/2 alles gruen (2026-09-30T21-27-22Z-2913). Testzentralen samt kompletter Item-/Blockabdeckung 10/10 alles gruen (2026-09-30T21-31-02Z-670a), beide isolierten 26.3-Welten neu gebaut.
- Default-/Modul-Wiki generiert, Default-/all-Check gruen. Finales gradlew.bat check -q mit beiden NeoForge-jar-Tasks: FINAL_CHECK_EXIT=0 (scratchpad/next-small/final-check.log); gemeinsame 26.2-Kompilierung erhalten. Alle vier Fabric-/NeoForge-JARs auf enthaltene API und Loader-Metadaten geprueft. Erststart scheiterte vor Tests am fehlenden Java 8 der kalten ForgeGradle-Konfiguration; lokal checksum-geprueftes JDK 8 und JAVA_OPTS loesten das ohne Quellaenderung.
- Offen: Orchestrator prueft/mergt separat und fuehrt den kompletten zusammengefuehrten Gate-Lauf aus. Besitzer nimmt Cap/Altconfig-Verhalten, Client-Konfiguration und Audio ab und baut seine Welt-Testzentrale. Nicht geprueft: Clients, reale Drittmod-Werkzeuge/Modpacks, Besitzerwelt, Forge samt dessen Library-Verpackung und andere MC-Laufzeitlinien. Keine neue Pixelkunst, keine Forge-/mc1_21_11-/mc26_4-Quellaenderung.

## FORGE-FOLLOWUP (2026-10-01, codex-next-forge)
- Commits: 1eb4b1b9 Plan; df354b5e Testisolation und uebernommener Cleanup aus master a5d8151e (beide Java-Testdateien exakt); e4b1320b native Dialoge/persistente validierte JSON-Konfiguration, generische Framework-Verpackung/Java-8-Pfade; 2049fd48 vollstaendiger Dimensions-Adapter und nachgezogene Claims-/Money-/Sounds-Hooks. Keine anderen MC-Quellen geaendert, kein Push/Merge, kein Clientstart.
- Native Forge-Dialoge verwenden bestehende EN/DE-Optionen; SimpleBuilding 172 Optionen, serverseitige Grenzen und remote gesperrte Gameplay-Felder. Cloth-Forge-Metadaten enden bei 17.0.144; NeoForge 26.3.159 ist inkompatibel. Forge 66.0.8 bleibt experimentell/opt-in. Gemeinsame Aenderungen sind generisch: Jar-in-Jar/API, eindeutige Gradle-Koordinaten, native Screen-Adaption, manifestbasierte Testabhaengigkeiten/Namespace, portable Java-8-Overrides.
- Ausgabe gelesen: 889/889 alles gruen (2026-10-01T01-43-00Z-7974; Basis 801), weitere Forge-Module/Normalregressionen 326/326 (2026-10-01T01-48-47Z-7d7a). Finaler Cleanup auf drei Loadern 264/264 (2026-10-01T02-03-44Z-90ee), Forge-Wiederholung 88/88 (2026-10-01T02-11-47Z-315a); vier Serverkonfigurationen nach beiden Laeufen byteidentisch. Sounds mit Visuals 17/17 (2026-10-01T02-13-59Z-1192), normaler Forge-Entwicklungsstart mit Claims-Filter 4/4 (2026-10-01T02-15-19Z-efa1). Keine ausgelassenen Faelle in den vollstaendigen Katalogen. Insgesamt 1039 unterschiedliche Forge-Faelle belegt.
- Testzentralen in isolierten Fabric-/NeoForge-Serverwelten neu gebaut, volle SimpleBuilding-Item-/Blockabdeckung 10/10 alles gruen (2026-10-01T02-16-20Z-36d6); Forge-Abdeckung im vollen Basislauf. Wiki --all generiert/geprueft, 45 Tooltests gruen, Handbuecher 0 Probleme. Default-check und -Pforge263=true check jeweils Exit 0; 26.2-Kompilierung erhalten. Elf Distributables auf API/Loader/native GUI-Klassen/EN-DE untersucht; zehn echte Forge-Startpfade nur mit Mod-JARs statt Entwicklungsordnern/losen APIs.
- Pruefnachweis mit exakten Dateien/Run-IDs: docs/FORGE-FOLLOWUP-RESULTS.md. Serielle Clientbefehle: docs/FORGE-26.3.md. Offen beim Orchestrator: kompletter zusammengefuehrter Normal-/integration-263-Lauf und Clients. Nicht geprueft: GUI-Bedienung/Darstellung, echte Client-Pakete, Portal-Farben/Audio, Fremdmodpacks, Besitzerwelten, separater Produktionsinstaller-/normaler Dedicated-Server-Start. Forge nicht automatisch aktivieren.

## CHIPS2 (2026-10-05, gpt-chips2)
- Leaping C / Tailwind A über INSTALL und Riding-Wrapper, beide Wiki-PNGs; Vorschau `C:/Users/o_o/code/minecraft-mods/previews/riding-chips2-vorschau.png` (16x, alt/A neben neu/C).
- Vorschlaghammer in der Welt: Eis 4, Packeis 9 Eissplitter; Obsidian 9 Obsidiansplitter; abgelegte Feuerkugel 4 Feuerkugelsplitter. Vorhandene Ablege-Konvention statt neuer ItemEntity-/Amboss-Mechanik. Ein Schlag, 1 Haltbarkeit, 10 Ticks Pause; Schutzregeln und Haufenreste bleiben erhalten. JEI/Wiki und EN/DE in beiden Bäumen.
- Ausgabe gelesen: `alles gruen: 1986/1986 bestanden, 0 rot` (Run `2026-10-05T10-36-10Z-7dfc`: Fabric 973, NeoForge 973, Riding 40); alle sieben neuen Tests und Testzentralen-Aufbau/Abdeckung auf beiden Hauptloadern grün. Vollständiges `check -q` samt 26.2-Fabric/NeoForge- und 26.3-Forge-Compiles: `GATE_EXIT=0`. Datagen nur 26.3, Wiki --all/--all --check und Texturen grün.
- Plan/Belege: `docs/ai/PLAN-CHIPS2-2026-10-02.md`. Nur Branch-Commit, kein Push, keine Clients. Besitzerwelt unverändert; Ingame-Sichtabnahme bleibt offen.
