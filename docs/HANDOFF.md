# Übergabe (Stand 2026-09-30) – Hauptlinie 26.3

Zuerst `AGENTS.md` vollständig lesen, dann diese Datei und `docs/ai/WORKFLOW.md`.
Wünsche/offene Besitzerpunkte: `.claude/QUEUE.md`. Code gewinnt gegenüber alten Run-Berichten.
Worker arbeiten in ihrem Worktree/Branch, committen ohne Push/Merge. Andere Linien erst im
separaten Port-Run nach Besitzer-Abnahme; shared muss weiterhin für 26.2 kompilieren.
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
- Attractor: ID `magnet`. Gehalten 3 Blöcke Basis, Range +1,5 je Stufe bis 7,5 vor Config;
  abgelegt 6 vor Config. Constructor's Touch schaltet Filter frei, erhöht keinen Radius.
  Schleichklick auf Block/liegendes Item oder Rechtsklick im Inventar wählt den Filter,
  Schleichklick Luft löscht ihn. Ohne Touch gilt ein gespeicherter Filter nicht.
  Ablegen ohne Touch per Schleichklick, mit Touch per normalem Rechtsklick.
  Ruhezone für beide Formen: `server.tools.attractorMinimumDistance`, Standard 1,25,
  Cap 0,5–2; darin Bewegung dämpfen statt ziehen (`MagnetItem`, `PlacedAttractors`).
- Amethyst Resonance Rod / Amethyst-Resonanzstab: aktuelle ID `amethyst_lens`,
  `laser_pointer` Legacy-Alias. Rezept IIR/ICA/IIR: Eisenbarren, Redstone, Eisenkern,
  Amethystscherbe. 640 Ladung standardmäßig, 4 je Sekunde; 16 Scherben laden voll.
  Der alte Configschlüssel `beamCostPerSecond` wird nicht gelesen; `chargePerSecond` gilt.
- Velocity Gauge: „ NA“/„NCN“/„KN “, vier Kupfernuggets, Amethystscherbe oben rechts,
  Kompass Mitte, Kupferkern unten links. Oktant: „ NR“/„NCN“/„GNL“, vier Goldnuggets,
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
- Rezeptfilter des Strahlschalters erkennt noch laser_pointer statt amethyst_lens;
  Benutzung wird abgeschaltet, das Rezept bleibt. Separater Gameplay-Fix, hier nur dokumentiert.
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

## Simple Dimensions — Entwurf, Portierung offen (2026-09-30, Codex)
- Branch codex-port-dimensions, Entwurfscommit e7625a9d: docs/modules/simpledimensions.md. Beide Quell-READMEs, instructions+ und relevante Portal-/Config-/Loaderquellen read-only verglichen. Neuester Java-Stand hat andere Portalrezepte als README/alte Besitzeranweisung.
- Entwurf: drei Dimensionen, Quell-Configschluessel, persistenter Namensraum simpledimension, Legacy light_blue_portal, Kollisionen mit SimpleBuilding, sichere Rueckwege und Testplan. Sicherheitsbefunde: generierte wertvolle Rahmen duplizieren Ressourcen; clearIfObstructing kann Bauten loeschen; One-way-/Plattformschalter erlauben unsichere Ankunft. Nicht ungeprueft portieren.
- Implementierung NICHT fertig. newmod-Token-Geruest lokal unter ignoriertem scratchpad/dimensions-scaffold-unported archiviert; Manifest und Integrationsauswahl ohne Modul wiederhergestellt. Keine Dummy-Mod oder Dummy-Testresultate ausgeliefert.
- Offene Umsetzung: loaderneutrale Runtime, Fabric/NeoForge-26.3-Adapter, automatische Datapacks, Config-GUI/Bounds, Legacy-Weltbelege, Modul-Wiki/Balancedaten und kompletter Pflicht-Testkatalog. Forge 26.3 sowie 26.2/1.21.11/26.4 bleiben spaetere separate Ports.
- Kein Gameplay-/Launch-/Worldgen-/Modulserver-/Clientnachweis in diesem Run. Kein Neubau der Besitzer-Testzentrale, kein Push/Merge; Quellrepos und andere Minecraft-Linien unveraendert.
- Dokumentationsstand verifiziert: wiki/generate.py und --check Exit 0; abschliessendes gradlew.bat check -q --no-daemon GRADLE_EXIT=0, Ausgabe gelesen (18 Wiki-Tests gruen; Compiler-Deprecationwarnungen). Keine Serversuite ausgefuehrt; Gate beweist keinen Modul-Launch. Generierte Wiki-Dateien nur inhaltsgleiche Zeilenenden, nicht committet.
