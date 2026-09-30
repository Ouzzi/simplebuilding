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
- Hammer: 1×1 = 1,2× gleiche Spitzhacke, Fläche wie eine Stufe darunter, Haltbarkeit unverändert bis der Besitzer testet.
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
