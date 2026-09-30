# Übergabe (Stand 2026-09-29) – weiterarbeiten mit jedem Assistenten

## Zwischenstand auf codex-kk (Codex, 2026-09-30)
- Spawn-Teleporter: sofortiger Reset bei jeder Redstone-Signalstaerkeaenderung, inklusive Impulsen zwischen Ticks; Zielwahl unveraendert. Gemeinsamer Code, Regressionstest in Fabric-Adapter und NeoForge-Katalog, Wiki DE/EN aktualisiert.
- Globaler Loom-Cache bleibt gesperrt. Umgangen mit separatem `GRADLE_USER_HOME=$PWD/scratchpad/gradle-home` (Loom-Cache kopiert, modules-2 und Wrapper verknuepft; keine Besitzer-Prozesse beendet). Beide 26.3-Loader kompilieren erfolgreich.
- Gefilterte Spawn-Tests **14/14, alles gruen**, Run `2026-09-30T00-34-20Z-fc28`. Regression prueft Ein-/Ausschalten, 15 -> 7 -> 15, kurze Impulse zwischen Ticks, unveraenderte Nachbarbenachrichtigung und die volle neue Wartezeit. Filter braucht den Namensraum: `simplebuilding:*spawn_teleporter*`; ohne Namensraum werden keine Tests ausgewaehlt.
- Testzentrale in beiden 26.3-GameTest-Welten neu gebaut, Abdeckung und Stationspruefungen **8/8, alles gruen**, Run `2026-09-30T00-35-31Z-2e08`. Besitzerwelt nicht angefasst. Wiki generiert und `--check` gruen, Texturen **470 + 9 mcmeta aktuell**, Buecher **0 Probleme**. Abschliessendes `gradlew.bat check -q` im Worktree **gruen, Exit 0** (auch 26.2-Kompilierbarkeit). Keine Client-Pruefung und kein vollstaendiger Server-Testlauf; kein Push/Merge.
- Enderit-Redstone und die 54-Slot-Endertruhe sind noch NICHT umgesetzt. Vorschlaege: zwei isolierte Kanaele, Reichweite hoechstens 15; Truhenname Astral Vault / Astralgewoelbe. Keine Besitzer-Abnahme dieser Vorschlaege behaupten. Kein Port, mc1_21_11 unveraendert.

Zuerst `AGENTS.md` lesen (alle Regeln). Diese Datei sagt, was fertig ist und was als Nächstes kommt.
Der Besitzer schreibt Deutsch, will kurze Antworten, Fragen als Liste, und möchte **erst 26.3 fertig**, dann Port-Run.

## Erledigt und auf master (Welle 23 + 24 Teile)
Welle 23 komplett (Config serverseitig, Modpack-Hooks, Erfolge, Beute/Handel, Items, Zusammenspiel, Immersion, Optik, Balancing-Zentrale).
Welle 24, gemergt in master (lokal/gepusht siehe Git-Log):
- **BB** Attractor-Arme, Ore Detector → **Detector** (ID `detector`, Alias `ore_detector`), Detector platzierbar, neue Rezepte
  (Gauge, Oktant, Blaupause mit Leuchttinte), Enderit-Namen, klebriger verstärkter Kolben → Netherit, Diamantblock ab Eisenhammer.
- **CC** Handbücher komplett neu (11 Mod-Bücher, 9 Vanilla-Bücher, Enchantments-Buch, Admin-Buch nur für OPs, pausiert nicht),
  Resonanz-Anzeige (Wert + Tooltip, Maximalwerte, Cap).
- **DD** platzierte Bündel (Sneak+Scroll, Rechtsklick raus, Sneak+Rechtsklick rein, Item zum Spieler gedreht),
  platzierter Oktant (Umriss pro Spieler). Neu gebaut: nur ein **gesperrter** Oktant wird mit Sneak+Rechtsklick abgestellt
  (`PlacedTemplates.isPlaceableOctant`) – Besitzer hat die Geste noch nicht bestätigt.
- **EE** Aktiv-Texturen und Leerlauf-Partikel für alle Pads, Spawn-Teleporter (eigener Spawn / Redstone → Weltspawn, Sounds),
  Hammer ohne GUI-Text (Nugget neigt sich – nur 26.3), Echo Sounder (Klick einmal, Sperre 1–5 s, Config `echoSounderAttemptLockTicks`).
- **FF** Kerne verwandeln selten Wirtsblöcke in Erz, neue Kern-Animation, Warden-Gesicht gerade.
- **GG** Tabs: SimplePads-Tab, Bauplanung in SimpleTools, Zeilen-Layout überall, Mod-Items im Suchtab neben Vanilla-Vorbildern.
- Client-Tests repariert (732/732 auf allen sechs Zielen, Stand vor den letzten Merges).

## HH / II / JJ und Pulsating integriert (2026-09-29, Codex)
- HH gemergt: Materialkerne/Pad-Rezepte, zusätzliche Mobköpfe und Fähigkeiten; Creative-Tab-Layout aus GG erhalten.
  Cave-Spider-Test trennt Bodenschaden von Luftangriffen. Gefiltert **30/30 grün** (Run `2026-09-29T19-54-57Z-8b89`).
- II gemergt: gestufte Shulkerkisten, Tab-Zeile und Export-Katalog ergänzt. Gefiltert **16/16 grün**
  (Run `2026-09-29T19-56-30Z-3ece`).
- JJ gemergt: Amethyst Resonance Rod, Attractor/Range/Touch und Gauge. Veraltete 4-Block-Erwartung auf 3 korrigiert;
  Filter-Test beendet Schleichen, bevor Ansaugen geprüft wird. Gefiltert **22/22 grün**
  (Run `2026-09-29T20-00-47Z-fc9d`).
- Besitzerwahl Pulsating: **Raute mit stärkerem Augen-/Mundkontrast** umgesetzt und 16-fach gezeigt.
  `work` integriert, 1.21.11-Textur noch unverändert. `MAIN_TREE_ONLY` und `MAIN_TREE_PREFIXES` bis zum Port behalten.
- Bücher-Faktenpass: Namen, Pad-Rezepte, Spawn-Ziele, Attractor-Reichweite/Filter, Rod-Reparatur, Gauge und
  Shulkerkisten aktualisiert; passende Rezeptkarten. Bücherprüfung **0 Probleme**, keine doppelten Lang-Schlüssel.
- Wiki-Fakten für Gadgets und Pads nach Code korrigiert; Quest-Hinweise der Hauptlinie korrigiert.
- Texturprüfung **470 Texturen + 9 mcmeta aktuell**. Shulker-/Gauge-Vorschau ebenfalls gezeigt.
- Der erste `check` stoppte an `checkQuests` (Reihenfolge der generierten Lang-Schlüssel); Generator erneut ausgeführt.
  Vollständiges Server-Gate: **1548/1548, alles gruen** auf `fabric-263,neoforge-263`,
  Run `2026-09-29T20-20-52Z-1579`, Code-Commit `2579653b`.
  Nach Gesamt-Gates korrigiert: Suchtab (keine doppelten Köpfe, Shulkerkisten beim Vanilla-Vorbild), neue Kern-Zuordnung,
  deutsche Enderitkern-Schreibweise, Range-Tag-Erwartungen, Touch-Filter-Fixture, gespeicherter alter Linse-Schlüssel
  vs. sichtbare Config. Druckplatten-Zeittest nutzt gewachste Platte, damit zufällige Oxidation die Messung nicht ändert.
  Testzentrale in beiden GameTest-Welten vollständig gebaut; alle Items/Blöcke abgedeckt.
  Abschließender `gradlew.bat check -q` im selben Worktree **grün (Exit 0)**. Server-Ausgabe und Build-Ergebnis gelesen.
  Push-fähiger Stand; tatsächlichen Remote-Stand mit `git log origin/master` prüfen.

## Launch- und Testzentrale (Branch `hub`, noch nicht gemerged)
Neuer Dev-Server `tools/launchhub` (Port 8771, `docs/LAUNCHHUB.md`) ersetzt die einzelnen Start-Einträge in `.claude/launch.json`.
KI-Befehlsvorlagen (claude/codex) und das Starten echter Clients wurden beim Bau nicht ausprobiert (Trockenlauf `SB_HUB_DRY_RUN`).

## Nächste Schritte in Reihenfolge
1. Push-/Remote-Stand prüfen. Server-Gate und `check` sind grün (siehe oben), Wiki `--check`, Textur- und Bücherprüfung ebenfalls.
2. Client-Gate für beide 26.3-Ziele seriell, erst wenn der Besitzer-Client geschlossen ist. Besitzer wurde gefragt.
3. Die Besitzer-Testwelt mit dem neuen Build öffnen und `/sbtestcentre build` ausführen. Der automatische Neubau
   und Abdeckungstest in separaten GameTest-Welten sind grün; die laufende Besitzerwelt wurde nicht angefasst.
4. Besitzer-Abnahme von 26.3 einholen; insbesondere neue Grafik/Bedienung im Client noch visuell testen.
5. Nach Abnahme von 26.3 durch den Besitzer: eigener **Port-Run** auf 26.2 Fabric/NeoForge/Forge, 1.21.11 und 26.4.
   Mitzunehmen: HH/II/JJ, neue Bücher/Texturen/Texte, Pulsating-Kontrast, Quest-Fakten, drei Mixins des Admin-Buchs
   (`OperatorBook*Mixin`), Nugget-Neigung im 26.2-`HeldItemRenderer`, Textur-Scope-Schalter in `tools/textures`,
   REI-Ausblendung des Admin-Buchs, Forge-Paket für `PlacedBundleScrollPayload`.
6. Später (Queue): Vorlagen teurer machen, Punkte 64–69, Wiki-UX-Ideen aus der Zentrale, Baustab über den Planer, Kerne als Module.

Die unversionierten Detector-Texturen unter `mc1_21_11/fabric/.../textures/item` lagen schon vor diesem Run
im Haupt-Repo und wurden nicht angefasst. Redundante Wiki-Sicherungen liegen als beschriftete Stashes vor.

## Fakten, die man leicht vergisst
- Mason verkauft keine Kerne; fahrender Händler: Kupfer/Eisen/Gold/Diamant-Kern selten und teuer.
- Chunk-Loader nur bei Online-Besitzer; Admin-Befehl zum Auflisten.
- Luftsprung 20 s / 10 s, Balken am XP-Balken (Priorität XP-Änderung > Luftsprung > Locator), Server erzwingt (≤ 1 s Lag).
- Linear baut eine Linie, Bridge von einem Ende mit doppelter Geschwindigkeit.
- Glowing hat eine Stufe (volle Helligkeit, Name „Glowing“); Pulsating allein pulsiert Sättigung, Pulsating+Glowing die Helligkeit 1–15.
- Hammer auf 26.3: Gesamtzeit 1,5x + 0,8x je Block 2-9 + 0,7x ab Block 10; Oktant 2x je Block gleicher Stufe. Haltbarkeit unveraendert. 26.2 behaelt die alte Formel bis zum Port-Run.
- Kerne nicht stapelbar; Enderman-/Lohenkopf nutzen die echten Vanilla-Texturen.
- Placed-Bundle/Oktant/Detector nutzen den Block `placed_smithing_template`.
- Zuletzt gepushter grüner Stand: siehe `git log origin/master`; danach nur, was im Commit „Handoff“ steht.

## Branch codex-nn (2026-09-30), Gadget-Korrekturen

Echolot `NNN/NRN/ENN`, Amethyst Resonance Rod (ID `amethyst_lens`) `IIR/ICA/IIR`; Datagen, Rezepttests, Wiki, JEI-Texte und dynamische Buch-Rezeptkarten stimmen damit überein. Attraktor-Ruhezone `server.tools.attractorMinimumDistance` (1,25 Blöcke, begrenzt auf 0,5–2) für gehaltene und abgelegte Attraktoren; 100 simulierte Zug-/Bewegungsschritte ohne Oszillation geprüft. 26.3-Bündel/Köcher zeigen die echte tier- und verzauberungsabhängige Kapazität und die Textfarbe des gemeinsamen Oktant-/Entfernungsmesser-Helfers. Rucksäcke verwenden eine eigene Slot-Anzeige und haben den `/64`-Fehler nicht.

Pulsating+Glowing moduliert im Trim-Submit Licht und Vertex-Helligkeit; Pulsating allein behält die Sättigungsanimation. Der Vanilla-Shader multipliziert Textur, Vertexfarbe und Lightmap: zusätzliche Farbmodulation verhindert einen durch die Lightmap abgeschwächten Helligkeitspuls. Beide 26.3-Loader verwenden denselben Mixin. Ohne Clientreproduktion bleibt die Ursache des ursprünglich gemeldeten Fehlers unbestätigt; Besitzerprüfung in `docs/TRIM-BALANCE.md`.

26.3 Fabric/NeoForge kompilieren. Gefilterte Serverprüfungen: **110/110, alles gruen** (Attraktor 16, platziert 2, Pulsating 6, Rezept-/Tweaks-Auswahl 68, Bündel 2, Köcher 2, Config-Grenzen 2, Config-Katalog 2, gespeicherte Defaults 2, Testzentrale 8). Testzentrale in beiden GameTest-Welten neu gebaut und vollständige Item-/Block-Abdeckung bestätigt (Run `2026-09-30T00-43-33Z-6fae`). Wiki `--check` aktuell, Bücherprüfung 0 Probleme, keine doppelten Lang-Schlüssel. Alte Gradle-Daemons aus dem Sandbox-Run hatten weiter Zugriffsschwierigkeiten; ein frischer Prozess (`--no-daemon` bzw. `GRADLE_OPTS=-Dorg.gradle.daemon=false`) funktioniert.

Abschließendes `gradlew.bat check -q` im Worktree grün (Exit 0), einschließlich gemeinsamer 26.2-Kompilierung, Balance, Atlanten, Jade-Split und Wiki. Keine Clienttests; Darstellung und Besitzerwelt-Neubau bleiben der Besitzerprüfung vorbehalten. Keine Ports, kein Push, kein Merge.
## Codex MM – 26.3-Transformationen (2026-09-30, Branch codex-mm)
- Eckenschlag: Schleichen ohne Constructor’s Touch, ein Zieleckenviertel, 1,5-faches Tempo (auf volle Ticks aufgerundet). Constructor’s Touch behält die Rückwärtsumformung. Diagonale Zwei-Viertel-Reste werden verweigert; fehlende Ecken/Sockel nicht getroffen.
- Gemeinsames `TransformTargets.canTransformTarget`: beide Hände, Hammer/Material, Meißel, Schere/Wolle, Oktantwäsche, Kerne, Rotator, Kupferplatten, Wachs/Schilder und Vanillas 26.3-BlockTransformer (Äxte, Hacken, Schaufeln; datengetrieben). Echoscherben sind derzeit Werkbank-/Schmiederezepte und erhalten keinen Blockhinweis.
- Eine gespiegelte Neige-/Wippanimation im 26.3-Handrenderer, pro Tick/Ziel zwischengespeichert. **26.2-HeldItemRenderer und Vanilla-Transformationsabfrage müssen im eigenen Port-Run nach Abnahme portiert werden.**
- Rahmenroute: für ablegbare Vorlagen auf 26.3 gesperrt; Inhalte alter Rahmen bleiben unverändert, nicht ablegbare Fremdvorlagen behalten den Legacy-Fallback.
- Gemeinsamer Code bleibt über `McVersion.TRANSFORM_HINTS_AND_CORNERS` auf 26.2 im alten Verhalten; mc1_21_11 unverändert.

- Verifikation: kompletter 26.3-Serverlauf **1552/1552, alles gruen**, Run `2026-09-30T01-01-13Z-9eda`. Testzentrale in beiden separaten GameTest-Welten vollständig neu gebaut; Item-/Blockabdeckung grün. Alte Haltbarkeitserwartung für den unverzauberten Schleichhammer an die neue Eckenumformung angepasst und zunächst **2/2** gezielt nachgetestet (`2026-09-30T01-00-10Z-290b`).
- Wiki: 26.3 erzeugt und `--check` grün; standardmäßige 26.2-Wiki-Ausgabe anschließend neu erzeugt. 26.3-InWorld-Export jetzt separat unter `mc26_3/generated/wiki/inworld.json`, damit die Legacy- und Eckenfakten je Linie stimmen. Bücherprüfung 0 Probleme, Questgenerator aktuell, keine doppelten Lang-Schlüssel.
- Ecktreppen speichern ihre Form über `simplebuilding_carved`; Nachbarupdates, Blockstate-Serialisierung und Survival-Blaupausen erhalten die Geometrie. Ladung bindet die Zielecke beim Beginn und verweigert geänderten Blockzustand oder gewechselten Modus.
- Visuelle Clienttests und Besitzerwelt nicht geprüft/angefasst: Bestätigung geschlossener Besitzerclients war noch ausstehend. Keine neue Pixelkunst. Kein Push/Merge, Port-Run weiter separat nach Abnahme.
- Abschließendes `gradlew.bat --no-daemon check -q` **grün (Exit 0)**, Ausgabe gelesen. 26.2/shared kompiliert; Ressourcen-Gate behandelt die ausdrücklich nicht verpackten Wiki-Metadaten wie der Ressourcen-Merger.

## Mega-Handbuecher (2026-09-30, Codex, Branch codex-ll, nicht gemerged/gepusht)
- Nur 26.3: zwei registrierte Basis-Items (guide_book, guide_book_vanilla_start), je Regal strikt getrennt.
  Themen werden mit den bisherigen Schluesselitems eingefuegt; kein eigener Themen-Item-Output.
  GuideUpgradeRecipe bewahrt Komponenten, verhindert doppelte Einlagen, vereinigt zwei Handbuecher
  desselben Regals und nimmt nur Kapitel von echten Handbuechern an. Admin-Einlage weiter nur OP >= 2.
- simplebuilding:guide_chapters speichert den Kapitel-Bitmaskenstand und synchronisiert ihn.
  LegacyItemIds gilt fuer Buch-Aliase nur auf 26.3; ModDataFixer migriert auch ohne MC-Versionswechsel.
  Alte explizite Buchseiten werden als zusaetzliche Absicherung erkannt. 26.2 behaelt das alte Verhalten.
- Keine Guide-Geschenke beim Beitritt, auch bei aktiviertem altem Config-Schalter. Kein Guide-Truhenloot
  im bestehenden Loot-Code gefunden; verzauberte Cover-/Texturbuecher bleiben unveraendert in der Beute.
- Tabs: 8 rechts, Mod-Regal 4 links inklusive Inhalt, Vanilla 2 links. Kein Wechsel in das andere Regal.
  Gesperrte Themen grau mit Einlage-Hinweis, Admin mit OP-Hinweis. Tooltip zeigt kleine Buchtexturen
  und eingefuegte Themen. Saubere 26.3-Pixeltexturen mit 16-fach alt/neu unter docs/previews/mega-guides*.
- JEI nutzt die formlosen Anzeigen des Upgrade-Rezepts. Wiki erkennt Typ und eingefuegtes Kapitel.
  Kapitel-Erfolge und 26.3-FTB-Quest-Defaults sind vorhanden; angepasste bestehende FTB-Buecher werden
  vom bisherigen Installer absichtlich nicht ueberschrieben.
  Testzentrale plant pro Regal ein leeres und ein volles Buch; DataIntegrity-Layout und Guide-Tests angepasst.
- Fortsetzung ohne Sandbox: Fabric/NeoForge 26.3 kompilieren; echter 26.3-Datagen-Lauf Exit 0.
  Kapitel-Erfolge auf das 26.3-Format `recipe_crafted.conditions.recipes` korrigiert.
  Wiki-Standardlinie auf die Hauptlinie 26.3 gestellt; Generator und --check gruen.
  Buchpruefung 0 Probleme, Texturpruefung 470 Texturen + 9 mcmeta aktuell; keine doppelten
  Schluessel in den 26.3-Sprach-Overlays. Keine Dateien in mc1_21_11/mc26_4 geaendert.
- Gefilterte Server-Tests auf fabric-263/neoforge-263: Guides 22/22 (2026-09-30T00-30-02Z-759d),
  DataIntegrity-Tabs 20/20 (2026-09-30T00-31-52Z-e1f4), Testzentrale 8/8
  (2026-09-30T00-33-21Z-9761): jeweils Ausgabe alles gruen gelesen.
  Testzentrale in beiden GameTest-Welten vollstaendig neu gebaut, alle Items/Bloecke abgedeckt.
- Abschliessendes gradlew.bat check -q --no-daemon im Worktree: Exit 0, Ausgabe gelesen.
  Gemeinsamer Code bleibt auch fuer 26.2 kompilierbar.
- Der alte Sandbox-Daemon verursachte weiterhin AccessDeniedException. Verifikation mit
  GRADLE_USER_HOME=$TEMP/sb-codex-gradle und deaktivierter Daemon-Wiederverwendung erfolgreich.
- Noch offen: Besitzer-Abnahme der Grafik/Bedienung im Client und Neubau in der Besitzerwelt.
  Keine Client-Sichtpruefung und kein kompletter Server-Testlauf; nur die genannten Filter.
  Keine anderen Linien portieren, bevor der Besitzer 26.3 abnimmt. Kein Push, kein Merge.
## Multimod-Grundlage (Codex, 2026-09-30)
Additive 26.3-Modulprojekte, framework-API-Skelett, Manifest/Dev-Mod-Registry, Scaffold-Befehl
und separate Fabric-Integration mit eigenem Cross-Mod-Test angelegt. Launch Hub hat Mods-Auswahl,
Presets und Integrationsstarts; normale 26.3-Starts uebernehmen vier optionale Dev-Mod-Schalter.
Bestehende Verzeichnisse/IDs/Testkataloge unveraendert. Details/Grenzen: `docs/MULTIMOD.md`.
NeoForge-Integration und optionale Cloth/Mod-Menu-Abhaengigkeiten im normalen Lauf sind zurueckgestellt.
Dieser Branch wird nicht gepusht oder gemergt; Verifikation siehe abschliessenden Run-Eintrag.

### Multimod-Verifikation (2026-09-30)
- Abschliessender `gradlew.bat check -q`: Exit 0; beide Beispiel-Loader und Integrationsharness kompilieren.
- Bestehende 26.3-Server: **1548/1548, alles gruen**, Run `2026-09-30T10-26-33Z-4b06`.
  Testzentrale in den automatischen Welten gebaut, vorhandene Item-Abdeckung gruen; Besitzerwelt nicht angefasst.
- Fabric-Integration: **1/1, alles gruen**, Run `2026-09-30T10-29-33Z-7569`.
- Launch Hub: **34 Unit-Tests gruen**, JavaScript-Syntax gruen; HTTP-Trockenlauf fuer Client/Server/frische Welt/Tests,
  91 Registry-Zeilen (2 Repo-Module, 89 Dev-Mods; davon 83 lokale Fabric-JARs), lokale JAR-Erkennung geprueft.
- JAR-Auswahl praktisch geprueft: deaktivierte Repo-/Dev-Mods fehlen im Integrationsordner; normale Fabric-Hub-Vorbereitung
  konnte gezielt Jade + lokales Sodium bereitstellen. Bestehende run/mods-Ordner bleiben unveraendert.
- Wiki generiert und `--check` gruen; `-Pmc264=true help` konfiguriert erfolgreich, keine Snapshot-Tests.
- Nicht verifiziert: echte Clients, visuelles Hub-Rendering, beliebige Kombinationen des lokalen Modpacks,
  NeoForge-Integration (zurueckgestellt). Browser-Anbindung hatte keine Oberflaechen; automatische Freigabepruefung
  lehnte Headless-Edge-Bildpruefung mit "blocked by policy" ab. Kein Client-Testlauf, Push oder Merge.
- Beim ersten parallelen Gate/Serverlauf fehlten NeoForge-Ressourcen; gezieltes `processResources --rerun-tasks`
  stellte sie wieder her. Anschliessend Gate und Server-Gates nacheinander gruen. Kein Gameplay-Fix noetig.

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
## End systems (2026-09-30, Codex, codex-kk2)
- Fortsetzung des unterbrochenen Runs; Implementierung frueh als eee5aa09 gesichert. Nur 26.3 registriert neue Bloecke/Items; END_SYSTEMS=false auf 26.2, keine Ports und keine Aenderungen an mc1_21_11/mc26_4.
- Nihilithscherbe bzw. Astralitstaub + Redstone ergibt 4 getrennte Pulver. Je Pulver + Hebel: eigener Schalter; Pulver + Redstone-Lampe: eigene Lampe (Lichtstaerke 12). Nur horizontale Nachbarn desselben Materials, kein Vanilla-Signaleingang/-ausgang. Maximal 15 Pulversegmente, server.machines.endSignalRange=15 mit Cap 1..15, Updates alle 2 Ticks; Lampen leiten nicht weiter. server.features.endSignals deaktiviert Gameplay/Rezepte (Rezepte nach Reload).
- Astral Vault / Astralgewoelbe: 54 persoenliche Plaetze, erste 27 direkt Vanilla-Enderinventar, letzte 27 gespeichert am selben Container. Rezept: Endertruhe + 2 Enderitbarren + 2 Astralitstaub. Vanilla-Blockentity fuer Deckel, Sounds, Partikel und Waterlogging; server.features.astralVault sperrt Nutzung ohne Inhaltsverlust.
- Rezepte, JEI-Infoseiten (getrennt von rezeptlosen Items), Jade-Kapazitaet, Erfolge ohne Toast/Chat, Handbuchkapitel, Wiki-Notizen und Features, Testzentralen-Leitungen; DE/EN in beiden Sprachorten, neue Schluessel identisch und ohne Duplikate.
- Deterministischer Generator tools/textures/end_system_textures.py mit --check (14 Texturen); 16x Alt/Neu unter docs/previews/end-signals-16x.png und astral-vault-16x.png. Vorhandene Enderittruhen-Textur bewahrt und um Astralzeichen ergaenzt.
- 26.3-Datagen erfolgreich. Registry-/Itemexport jetzt je Linie unter mc26_3/generated/wiki/items.json, da neue Registrierungen nicht in der 26.2-Registry stehen. Wiki generate/--check gruen, Haupttexturpruefung 470 + 9 mcmeta, Buecher 0 Probleme.
- Funktionspruefung 8/8 alles gruen (2026-09-30T11-31-00Z-a7f1); Testzentrale in beiden separaten GameTest-Welten neu gebaut, vollstaendige Item-/Blockabdeckung: 10/10 alles gruen (2026-09-30T12-37-08Z-3da9).
- Vollstaendiges gradlew.bat check -q im Worktree gruen (Exit 0), Ausgabe gelesen. Erstes Gate verlangte Quest-Lang-Neusortierung; Generator ausgefuehrt.
- Keine Clienttests oder Sicht-/Soundpruefung im Spiel, kein voller Serverlauf; Besitzerwelt unveraendert. Besitzer-Abnahme der Grafik und Bedienung sowie /sbtestcentre build in der Besitzerwelt bleiben offen. Kein Push/Merge.
- Rezept-/JEI-Integritaet nach Korrektur: 48/48 alles gruen (2026-09-30T12-45-41Z-d259). Paralleler Gate-/Testversuch zuvor mit Fabric-Ausgabekollision verworfen; serieller Wiederholungslauf gruen.
## INFRA-W: Multimod-Wiki (2026-09-30, Codex, Branch codex-infra-w)
- Manifest-Datenvertrag fuer alle vorhandenen Module ergaenzt. Generator: --module/--all,
  eigener datenorientierter Extraktor, Vollstaendigkeit EN/DE pro Modul, dynamische Registry-
  und komplexe Daten ueber dokumentierte Datagen-Exports. Wiringexample voll dokumentiert.
- Header-Modauswahl mit URL/localStorage, getrennte Uebersichten/Navigation/Suche,
  optionale Gesamtsuche mit Mod-Abzeichen, Cross-Mod-IDs und Abhaengigkeits-/Versionsdaten.
  Asynchroner Modulloader funktioniert ohne fetch, auch bei lokalen Dateien.
- SimpleBuilding-JSON/JS bytegenau wie HEAD; keine Java-Logik/Testkataloge oder anderen Linien
  geaendert. Neues Scaffold erfuellt den Datenvertrag und liefert Token-Prosa mit.
- checkWiki/CI/Hook/Hosting pruefen alle Manifest-Module; statisches Paket enthaelt alle
  Modskripte, Loader und Texturen mit Cache-Hashes. Dokumentation aktualisiert.
- Verifikation: finales gradlew.bat check -q --no-daemon Exit 0; 12 Python-Wiki-Tests,
  8 bestehende Launch-Hub-Modtests, 9 DOM-Integrationsfaelle ohne JS-/Console-Fehler
  (URL/alte Links/Cross-Mod-Suche/Storage-Ausfall, 1280/390 als simulierte Fensterbreiten).
  Das DOM hat KEIN visuelles Layout gerendert; keine echte Desktop-/Handy-Sichtpruefung.
- Testzentrale in beiden 26.3-GameTest-Welten neu gebaut; vollstaendige Item-/Block-Abdeckung
  und Stationspruefungen 8/8, alles gruen, Run 2026-09-30T13-08-12Z-9879.
  Erstversuch hatte versehentlich testcentre statt test_centre im Filter und waehlte null Tests;
  kein Gameplayfehler. Erstes Gate scheiterte an zu breiten modules/-Inputs, behoben durch
  Ausschluss der Buildverzeichnisse. Finale Gate-Ausgabe einschliesslich Exitcode gelesen.
- Browser-Vorschau blockiert: cua.getState meldet keine Browser/Apps, IAB nicht verfuegbar.
  Vorschau-Server gemaess launch.json/wiki auf 127.0.0.1:8765 gestartet. Kein echter Client,
  kein voller Serverlauf, keine Besitzerwelt-Pruefung. Kein Push, kein Merge, keine Ports.
- Besitzer: Desktop/Handy-Wiki visuell pruefen; dynamische Module muessen Registry-Exports
  aus Datagen liefern. Geplante Mods werden erst mit existierenden Projekten registriert.
## INFRA-B: Multimod-Balancing (2026-09-30, codex-infra-b)
- Manifest-Vertrag fuer alle vorhandenen Eintraege und Scaffold ergaenzt; Modul-Wiki-Skelett.
  Balancing-Zentrale: request-lokale Mod-Auswahl, getrennte Services/Ablagen, Browser-Entwuerfe,
  Quellen/Rechner/Solver/History/Rollback je Mod, Uebersicht und Metadaten. SimpleBuilding bleibt
  ohne Migration in balance/; Zusatzmods nutzen balance/<id>. Keine bestehenden Versionen verschoben.
- Leser fuer Balance-Konstanten, gemeinsame/Loader-JSON-Daten, Item-Export, Rezepte, Handel
  und gewoehnliche JSON-Beute. generated bleibt Planung ohne eindeutige Quellverknuepfung;
  unbekannte Loot-Bedingungen/Funktionen werden berichtet und nicht als belegte Zeiten modelliert.
  Grenzen/Producer-Vertrag in docs/BALANCING-ZENTRALE.md und docs/MULTIMOD.md.
- Hauptlinien-Schreibziele und Datagen nur 26.3; keine separate Port-Kopie geaendert.
  checkBalance prueft alle Manifest-Module. Voller bestehender Devserver-Lauf: 118 Tests gruen
  (107 bestehende + damals 11 Modul-Tests; ein vorhandener Skip). Danach finale 14 Modul-Tests
  sowie 12 bestehende Auslese-Tests gruen; JavaScript-Syntax gruen.
- Bestehende 26.3-Server: 1554/1554, alles gruen, Run 2026-09-30T13-03-57Z-722d.
  Testzentralen in beiden GameTest-Welten gebaut; Item-Abdeckung im gruenen Gesamt-Lauf enthalten.
  Abschliessendes gradlew.bat check -q --no-daemon: Exit 0, Ausgabe gelesen (gemeinsame
  26.2-Kompilierung bleibt gruen). Wiki generiert und --check aktuell.
- Offen/unverifiziert: Desktop-/Handy-Sichtpruefung (cua: browsers=[]; In-App-Browser nicht
  verfuegbar), echte Modul-Datagen-Auftraege zukuenftiger Port-Module und deren spezielle
  Java-Builder/Loot-Adapter. Keine Minecraft-Clienttests, Besitzerwelt unveraendert.
  Keine neuen Texturen. Kein Push/Merge; Commit auf dem Arbeitsbranch.
## INFRA-F Forge 26.3 (Codex, 2026-09-30, Branch codex-infra-f)
- Offizielle Forge-Seite, Maven-Metadaten und MDK verifiziert: 26.3-66.0.8 (2026-09-28).
  MDK-SHA1 stimmt mit der Downloadseite ueberein. Java 25, ForgeGradle [7.0.17,8),
  MDK-Wrapper 9.7.1; hier Pin 7.0.36. Runs brauchen zusaetzlich Java 8 fuer Slime Launcher.
  Kein fehlender Upstream-Build als Blocker. Details/Quellen: `docs/FORGE-26.3.md`.
- Additives `mc26_3/forge`, standardmaessig aus (`-Pforge263=true`). Bestehende Forge-26.2-
  und andere Linien unveraendert. Nur Forge-Adapter-Overlays: KeyMapping, dimensionierte
  GameTest-Metadaten, Loot-Holders nach Registry-Laden/vor Validierung, Suchtab-Platzierung,
  Breezekopf-Ackerlandschutz, Shulkerkisten-Waschen/Werfer und HUD-Umschalttaste.
- Testziel `forge-263`, eigene Run-/Reportpfade, Aufzeichnungen/Tabellen und Launch Hub
  (Server, Client, Client + frische Welt, Tests). Bestehende Standard-/Release-Auswahl bleibt
  unveraendert. Forge263 kann nach Besitzer-Abnahme separat zum Default werden.
- Modulmanifest-Vertrag fuer alle vorhandenen Eintraege ergaenzt; sichere Pfadvalidierung,
  Forge-Modulvorlage/Beispielprojekt, eigene Manuals/Generated-/Balance-Pfade. Kein Verschieben
  von SimpleBuilding oder Balance-Historie. Forge-Integrationsruntime weiterhin zurueckgestellt.
- Server Forge: **778/778, alles gruen**, Run `2026-09-30T13-04-03Z-0963`:
  dieselben 777 gemeinsamen Tests wie pro Fabric/NeoForge plus ein Forge-Netzwerktest.
  **Keine Forge-Skips/known failures.** Nach erstem Voll-Lauf (774/778) fehlende Loader-Hooks
  repariert; Shulker-Gegenprobe 8/8 (`2026-09-30T13-02-07Z-6500`), dann Voll-Lauf gruen.
- Testzentrale in separaten Forge/Fabric/NeoForge-GameTest-Welten gebaut; alle Mod-Items/-Bloecke
  abgedeckt. Fabric/NeoForge-Centre-Filter **10/10, alles gruen**, Run `2026-09-30T13-06-59Z-6972`.
  Besitzerwelt nicht angefasst, keine Wiederholung der gesamten Fabric/NeoForge-Suiten.
- Echter `:mc26_3:forge:runDatagen` (delegiert Fabric + syncGenerated263) Exit 0.
  Gate fand zwei JSON-identische Overlay-Rezepte nur wegen Schluss-Zeilenumbruch; Sync-Vergleich
  normalisiert nun Rand-Leerraum. Korrigierter Sync Exit 0, keine redundanten Rezept-Overlays.
  Reine Generator-Zeilenenden-Aenderungen nach Inhaltsvergleich zurueckgesetzt.
  Wiki generiert, anschliessend --check aktuell. Keine Wiki-Inhaltsaenderung erforderlich.
- **37 Hub-/Registry-/Scaffold-Tests gruen**, inklusive Forge-Client/Server/frische-Welt-Trockenlauf;
  Testing-UI-JavaScript-Syntax gruen. Vollstaendiges `gradlew.bat -Pforge263=true check -q`
  im Worktree **gruen (Exit 0)**, Ausgabe gelesen; Forge-Ressourcen/Atlanten und Modul kompilieren.
- Offene Besitzerpunkte: echte Forge-Clientdarstellung/Bedienung abnehmen und eigene Testwelt
  neu bauen; bestehender AutoConfig-Shim hat keine Datei-Persistenz/Cloth-GUI. Optionale
  JEI/Jade/Curios/Cloth-Integrationen und Forge-Integrationsinstance nicht verifiziert.
  Kein Clienttest, kein separater normaler Dedicated-Serverstart, kein Port anderer Linien,
  keine Pixelkunst, kein Push/Merge.
- Abschliessendes normales `gradlew.bat check -q` (Forge263 standardmaessig aus) ebenfalls
  **gruen, Exit 0**, Ausgabe gelesen. Beide Gate-Konfigurationen bestaetigt; keine Dateien
  in mc1_21_11, mc26_4, forge/, common/ oder src/ geaendert.

## FIX16 (2026-09-30, Codex, codex-fix16)
- KK2-Integritaet repariert: sieben item.*-Namensschluessel parallel zu block.* in beiden DE/EN-Sprachorten; Nihilit-Anzeigenamen gemaess bestehender Familienregel, IDs unveraendert. Signal-Buchseite auf passende Laenge gekuerzt (Rezepte bleiben als Karten im Kapitel).
- Suchtab: Pulver neben Redstone, Schalter neben Hebel, Lampen im zuerst eingesammelten Functional-Tab neben der Vanilla-Lampe, Astral Vault neben Endertruhe. Exakte Nachbarschaften im bestehenden Test gepinnt.
- Erwartungsupdates fuer beabsichtigtes KK2-Design: Endertruhe genau einmal in SimpleMachines (inklusive exakter Vanilla-Anzahl); nach Shulkerkisten die Endertruhe/Vault-Zeile und beide Signalfamilien mit Abstand, weiter neun Spalten. Drei neue Config-Felder mit Namen/Defaults true,true,15 gepinnt; Tooltip-Annotationen und korrekte DE/EN-Erklaerungen samt Default ergaenzt. Keine Tests abgeschwaecht.
- Finale volle 26.3-Server-Suite: 1562/1562, alles gruen (je Loader 781), Run 2026-09-30T13-46-59Z-c843. Testzentralen in beiden separaten GameTest-Welten gebaut, volle Item-/Blockabdeckung gruen. Gezielte Integritaet zuvor 82/82 gruen, Run 2026-09-30T13-45-14Z-4190.
- Wiki generiert und --check aktuell; neue KK2-Items jetzt im Wiki-Itemkatalog, Prosa mit Nihilit-Namen konsistent. Buecher 0 Probleme; vier Sprachdateien ohne doppelte Schluessel.
- Abschliessendes gradlew.bat check -q im Worktree gruen, nativer GRADLE_EXIT=0 ausdruecklich gelesen; gemeinsame 26.2-Kompilierung enthalten. Erster PowerShell-Aufruf meldete wegen stderr-Warnungen Exit 1 ohne Gradle-Fehler; Wiederholung mit explizitem LASTEXITCODE gruen.
- Keine Clienttests/Sicht- oder Soundpruefung, Besitzerwelt nicht angefasst. Keine neue Pixelkunst; mc1_21_11/mc26_4 unveraendert, keine Ports. Kein Push/Merge; Commit auf Arbeitsbranch.
## Vier Besitzer-Fehlerberichte (codex-bugs, 26.3, 2026-09-30)
Dieser Abschnitt ersetzt die Werkbank-Erweiterung aus dem historischen Mega-Handbuch-Eintrag oben.
- Hammer: stueckweise Gesamtzeit relativ zur gleichstufigen Spitzhacke (1,5 / 0,8 / 0,7; Grenze 9),
  Oktant bewusst linear 2 je Block. Benannte Balance-Konstanten; keine neue Haltbarkeitsaenderung.
- Basisbuecher bleiben normale formlose Rezepte. Rezeptfreischaltung mit Buch ODER Regal-Schluesselitem,
  auch beim Wiedereintritt mit vorhandenen Zutaten. JEI-Infoseite erklaert das Freischalten im Buch.
- Kapitel im Buch: gesperrten Reiter anklicken, Bedarf oder Bestaetigung im Buch; Server bindet die
  Anfrage an das geoeffnete gehaltene Buch und validiert Inventar-Menue, Regal, Kapitel und OP-Stufe.
  Genau ein Item, auch in Creative; Nebenhand-Zutaten werden akzeptiert. Ton und Komponentensync.
  Crafting-Erweiterungen und Kombination entfernt; vorhandene Masken und Legacy-Migration bleiben.
- Buendel-Rezept war in Quelle und gemeinsamem Datagen bereits DSD/DBD/DXD (6 Kiesel); neu erzeugt
  und um Kiesel als zusaetzlichen Rezept-Unlock erweitert. Koecher bleibt DSD/DQD/DXD, Rucksack
  DSD/DBD/LLL. Fester craftable-Tag mit 185 Ergebnissen verhindert still verschwundene Rezepte.
- Gefilterte Serverpruefungen auf Fabric/NeoForge 26.3: **178/178, alles gruen**:
  Guides 22 (2026-09-30T13-12-08Z-6404), Hammer 70 (2026-09-30T13-14-58Z-9198),
  Buendel 76 (2026-09-30T13-18-42Z-eff0), Rezeptabdeckung 2 (2026-09-30T13-20-46Z-5607),
  Testzentrale 8 (2026-09-30T13-34-37Z-c786). Testzentralen in beiden GameTest-Welten neu gebaut;
  alle Items/Bloecke abgedeckt, Besitzerwelt nicht angefasst.
- Neuer Clientfall mega-guide: echter Tab-Klick ohne Item, Bestaetigung nach Eintreffen des Items,
  genau ein Itemverbrauch im Creative-Modus, C2S-Paket und Komponentensync, doppelte Anfrage ohne Verbrauch.
  Beide Loader seriell: je 55 Schritte fertig und 2/2 frische Screenshots, alles gruen.
  NeoForge 2026-09-30T13-29-32Z-e3d8, Fabric 2026-09-30T13-31-29Z-d029.
  Bestaetigung und offenes Kapitel auf beiden Screenshots angesehen (englische Sprache).
- Testrunner korrigiert: SIMPLEBUILDING_CLIENT_ONLY grenzt nun auch erwartete Screenshots ein.
  Vorher funktionierte der Test, der Runner verlangte jedoch 119 nicht ausgewaehlte Bilder.
  Vollauswahl, Einzelauswahl und unbekannte Auswahl ohne Clientstart gegengeprueft.
- Echter 26.3-Datagen-Lauf Exit 0; gemeinsame Containerrezepte fuer alle Loader haben unveraendert
  die korrekten 6/4/6 Kiesel. Datagen zieht auch die bereits im Quellcode geaenderten
  Echolot-/Resonanzstab-Rezepte in das 26.3-Overlay nach (keine neue Gameplay-Aenderung). Fehlendes Buendelrezept im laufenden Modpack nicht reproduziert;
  geladene Rezepte, Freischaltung und feste Abdeckung abgesichert.
- Wiki erzeugt und --check aktuell; Buchpruefung 0 Probleme, Questgenerator aktuell,
  keine doppelten Schluessel in den vier Sprachdateien. checkBalance 0 Fehler (380 erzeugte Stellen),
  alle vier Hammer-Konstanten in der Balancing-Zentrale nachgewiesen.
- Abschliessendes gradlew.bat check -q --no-daemon im Worktree: **Exit 0**, Ausgabe gelesen;
  auch 26.2/shared kompiliert. Keine Dateien in mc1_21_11/mc26_4 geaendert.
- Nicht verifiziert: kompletter Server-/Client-Suitenlauf, deutsche UI-Darstellung, echte JEI-Bedienung
  und Rezeptbuch-Autofuellen im Besitzer-Modpack sowie Besitzerwelt. Keine Ports, kein Push/Merge.
## Simple Money (Branch codex-port-money, 26.3)
- Vollständiges Quellinventar: `docs/modules/simplemoney.md`. Original-IDs, Texturen und Configschlüssel erhalten; Fabric/NeoForge additiv unter `modules/simplemoney`.
- Eigener Integrations-Testkatalog; NeoForge-Testwelt separat unter `integration/run-neoforge-263`. Keine Besitzerwelt verändert.
- Forge 26.3 und 26.2/1.21.11/26.4 weiterhin separat nach Freigabe: Einstieg/Config/Loot/GUI/Testadapter portieren. Verifikationsergebnisse folgen unten.

### Simple Money — abschließende Ergebnisse
- Branch `codex-port-money`, keine Ports/kein Push/kein Merge. Quelle `a9c12a7` sauber und nur gelesen.
- Vollport: sieben Original-Items/Texturen, acht Rezepte, 47 Angebote mit originalen gewichteten Pools, sieben Truhenbeuten, zwei serverseitige Configschalter. Zweisprachige Module-Wiki (57 Kapitel), Balance-Daten und vollständiger Manifestvertrag für alle bisherigen Einträge. Launch-Hub-Tests starten ausgewählte Modul-Kataloge separat.
- Bestand/Integration **1555/1555 alles gruen** (`2026-09-30T13-22-27Z-8711`); finale Modsuite **20/20 alles gruen** (`2026-09-30T13-26-13Z-38db`); ausgeschaltete Handelsschalter **2/2 alles gruen** (`2026-09-30T13-16-41Z-43e8`). Testzentralen im Bestand neu gebaut und Item-/Blockabdeckung grün. Eigene Namespace-Abdeckung prüft alle Money-Items.
- Fabric-Client-Smoke mit Titel/Welt, sieben Itembildern und Configseite **Exit 0**, Screenshots unter `docs/previews/simplemoney`; 35 Hub-Tests grün. Wiki erzeugt und --check aktuell; vollständiges `gradlew.bat --no-daemon check -q` **Exit 0**, Ausgabe gelesen (inklusive gemeinsamer 26.2-Kompilierung).
- Ein früher Modlauf wurde durch eine zu früh geänderte Testconfig gestört; Originalconfigs wiederhergestellt und beide Loader stabil wiederholt. Quellenvergleich korrigierte drei Bibliothekar-Pools mit gleichnamigen lokalen Variablen; finale Modsuite prüft die richtigen Pools.
- Offen: Besitzer-Abnahme, NeoForge-Client, echte alte Spielwelt und Besitzerwelt-Testzentrale. Forge 26.3 und 26.2/1.21.11/26.4 bleiben eigener freigegebener Port-Run; Details `docs/modules/simplemoney.md`.

- Abschließender kombinierter Harness-/Modlauf nach Trennung der Fabric-Tasks: **21/21 alles gruen**, `2026-09-30T13-35-03Z-5946`. Eigene Berichte/Filter, gemeinsame Fabric-Instanz seriell. Danach vollständiges `gradlew.bat --no-daemon check -q` erneut **Exit 0**, Ausgabe gelesen.

## FIX-MONEY-WIKI (2026-09-30, codex-fix-money-wiki)
- Simple Money: notes ist jetzt ein Objekt mit sieben konkreten zweisprachigen Item-Notizen; vorhandene Kapitelzusammenfassungen nach Rezepten/Itemcode praezisiert. Keine Gameplay-Aenderungen.
- Generische Java-Inventarerkennung bindet literale IDs an ITEM-/BLOCK-Registrierungen statt alle IDs einer Datei als Items zu behandeln. money_items ist ein bereits bilingual benannter Kreativtab, kein Item. Ungueltige notes-Listen liefern einen Schemafehler statt AttributeError; echte fehlende Namen/Prosa bleiben Fehler.
- Multimod-Vertrag dokumentiert; vorhandenes newmod-Template bereits konform, durch Regressionstest abgesichert. Alle Modul-Wikis regeneriert, sieben unveraenderte Originaltexturen unter wiki/assets/textures/simplemoney kopiert.
- Verifiziert: wiki/generate.py --all und --all --check Exit 0; Wiki 15 Tests, Launch Hub 38 Tests; Devserver 121 Tests OK (1 skipped, bestehende ResourceWarnings). gradlew.bat check -q im Worktree GRADLE_EXIT=0, einschliesslich checkBalance/checkQuests/Multimod und shared-26.2-Kompilierung; Ausgabe gelesen.
- Keine Server-/Client-Spieltests, Browser-Sichtpruefung oder Testzentralen-Neubau in diesem reinen Wiki-/Extractor-Run; Besitzerwelt unangetastet. Keine Aenderungen an mc1_21_11/mc26_4, kein Port, Push oder Merge. Keine offenen Besitzerentscheidungen fuer diesen Fix.

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

## Simple Fun (2026-09-30, codex-port-fun)
- Read-only source b778d9bb (1.2.0, 37 tracked Java files); inventory and limits: docs/modules/simplefun.md. Additive Fabric/NeoForge 26.3 port retains IDs/config keys; eight cosmetic delights, pig/cow/chicken/sheep charged-creeper heads, 26 server settings, bilingual wiki/balance/JEI/quiet advancement hints.
- Full 26.3 gate: 1627/1627, alles gruen (1562 existing + 1 integration + 64 module), run 2026-09-30T15-47-00Z-8618. Final private/LAN/dedicated glass safety: 2/2, alles gruen, 2026-09-30T15-56-02Z-6ca2. Test centres rebuilt in both separate existing GameTest worlds; coverage/stations green. Module item/block coverage is isolated in its own tests/data gate. Owner world untouched.
- Fabric client: 4/4 screenshot checkpoints, alles gruen, 2026-09-30T15-41-55Z-f8d3. Title/world/config plus standing/wall/ground/worn head models viewed; evidence modules/simplefun/docs/previews. Special item-model nesting and Windows decoding repaired. No new pixel art; source assets and Vanilla mob textures preserved.
- Default/all-module wiki generation/checks passed; actual booted registry export under module generated/resources/wiki. Final gradlew.bat check -q --no-daemon: GRADLE_EXIT=0, output read, includes shared 26.2 compilation, balance and module data. Source repo clean; no SimpleBuilding implementation or deferred MC line changed. No push/merge.
- Decisions: no duplicate SimpleBuilding content; preserved global minecraft:knockback datapack override is documented. Effective knockback capped, throw/damage/entity lifetime bounded, No Damage direct melee only. Glass destruction refuses published LAN and dedicated servers even when enabled; private single-player retains interaction/world-border checks. Client config screen writes a local restart draft, never sends server settings.
- Open verification: NeoForge client/UI, actual upgraded source world, owner modpack/world, German screenshots, interactive JEI/Jade, real two-client LAN and subjective audio. Forge 26.3 needs entrypoint/config/trade/loot/render/test adapters; 26.2/1.21.11/26.4 remain a separate release port after approval. No subagents used.
