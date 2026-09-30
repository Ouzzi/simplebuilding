# Übergabe (Stand 2026-09-29) – weiterarbeiten mit jedem Assistenten

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
