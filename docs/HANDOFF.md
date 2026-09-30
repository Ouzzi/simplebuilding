# Übergabe (Stand 2026-09-30) – Hauptlinie 26.3

Zuerst `AGENTS.md` vollständig lesen, dann diese Datei und `docs/ai/WORKFLOW.md`.
Wünsche/offene Besitzerpunkte: `.claude/QUEUE.md`. Code gewinnt gegenüber alten Run-Berichten.
Worker arbeiten in ihrem Worktree/Branch, committen ohne Push/Merge. Andere Linien erst im
separaten Port-Run nach Besitzer-Abnahme; shared muss weiterhin für 26.2 kompilieren.

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
