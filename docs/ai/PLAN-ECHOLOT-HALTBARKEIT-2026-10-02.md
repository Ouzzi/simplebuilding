# Echolot-Haltbarkeit – 2026-10-02

## Ist und Historie

- Die Implementierung heißt weiterhin `EchoCompassItem`, die Registry-ID seit `7dce188d` `echo_sounder`.
- `git log -p` / `git log -S` zeigen: `8658c125` hatte 64 Haltbarkeit und verbrauchte mit `hurtAndBreak(1, ...)` einen Punkt sowie eine Enderperle pro sofortigem Sprung.
- `71eee6d5` führte 1500 Reparaturpunkte, 60 Ticks Halten, 32 Richtungen und drei Riss-Stufen ein. Ein erfolgreicher Sprung belastet einmalig 1500 Punkte über `EnchantmentHelper.processDurabilityChange`. Unbreaking III reduziert das zufällig auf ungefähr 375 Punkte. Auch teilweise beschädigte Echolote gelten als gerissen und zerspringen beim nächsten erfolgreichen Sprung (120 Ticks Halten).
- `549c887a` entfernte die Perlenkosten. `1121fdcd` machte die maximale Ladung serverseitig konfigurierbar (Standard weiterhin 1500). Spätere Änderungen ergänzten Sperren und Berechtigungen, keinen Verbrauch pro Tick.
- `onUseTick` erzeugt ausschließlich Effekte. `wear` wird erst nach erfolgreichem Teleport aufgerufen. Die Haltbarkeitsleiste ist unverändert Vanilla; die Riss-Stufen verwenden den Schadensanteil. Der Prozent-Tooltip zeigt die restliche Ladung.
- Die neuere Texturanimation stammt aus `181b1257`; ihre 32 `.mcmeta`-Dateien (frametime 2), die 32 Richtungsmodelle und die drei schadensabhängigen Riss-Stufen werden nicht geändert.

## Entscheidung / Soll

Der Besitzerwunsch „einmalig die 1500 aufbrauchen“ hat Vorrang vor dem historischen Unbreaking-Verhalten. Auf 26.3 leert jeder erfolgreiche normale Sprung die gesamte tatsächliche Stack-Kapazität (Standard 1500), unabhängig von Unbreaking. Damit stimmen Ladung, Vanilla-Leiste und Riss-Stufe wieder überein. Die serverseitige Kapazität und abweichende MAX_DAMAGE-Komponenten bleiben wirksam; kein Zurücksetzen vorhandener Items und kein Entfernen der Leiste. 26.2 behält sein bisheriges Verhalten bis zum Port-Run (`McVersion.GADGET_REWORK`).

3-Sekunden-Halten, Animation/FOV, 32 Richtungen, Riss-Stufen, Reparatur, Kreativ-Ausnahme und Zerspringen bleiben erhalten. Abbruch oder verweigerter Teleport kosten nichts. Es gibt keinen belegten historischen „1500 Punkte ohne Unbreaking“-Stand im Java-Code; dies stellt die ausdrücklich gewünschte vollständige Entladung her, nicht einen erfundenen historischen Commit.

## Umsetzung

1. Queue-Eintrag ergänzen. In `EchoCompassItem.wear` auf 26.3 den tatsächlichen Maximalschaden setzen; den alten Pfad für 26.2 erhalten. Kommentar präzisieren.
2. Vorhandenen Unbreaking-Test samt Fabric-Adapter/Katalog passend umbenennen und versionsabhängig prüfen. Den echten Haltevorgang, ausbleibenden Tick-Verbrauch und Vanilla-Leiste in bestehenden Tests absichern. Abweichende Stack-Kapazität und Kreativmodus prüfen.
3. JEI-Texte EN/DE an beiden Sprachorten sowie Wiki-Prosa aktualisieren; generierte Wiki-Daten erneuern.

## Risiken und Verifikation

- Absichtliche Balanceänderung: Unbreaking spart auf 26.3 keine Reparaturpunkte mehr. Mending und Echoscherben bleiben nutzbar.
- `python tools/testrunner/run.py --targets "fabric-263,neoforge-263"`; Ergebniszeilen lesen, nicht dem Exitcode vertrauen. Die Suite enthält die Testzentralen-Abdeckung.
- `gradlew.bat :compileJava :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava` und `gradlew.bat check -q` ausschließlich in diesem Worktree.
- Wiki generieren und prüfen; Diff und Registrierung prüfen. Keine Minecraft-Clients, kein Push/Merge, nur Commit auf `claude-gpt-echo`.
- Clientdarstellung und Besitzerwelt/Testzentrale bleiben mangels Clientlauf offen. Unveränderte Texturen benötigen keine neue Pixelkunst-Vorschau.

## Ergebnisse

- Umsetzung wie geplant: Entladung nur auf 26.3, genau einmal nach erfolgreichem Sprung, anhand der tatsächlichen Stack-Kapazität. Der bestehende Unbreaking-Test nutzt jetzt den vollständigen Haltevorgang. Neuer Test für 2250 Stack-Kapazität, Nebenhand und Kreativmodus; bestehende Reparaturtests prüfen zusätzlich die Vanilla-Leiste.
- Statische Prüfung: `ECHO_ASSETS_OK: 32 directions, 3 damage stages, 32 animation metadata files`.
- Sprachabgleich: `ECHO_LANG_OK: EN+DE match in base and 26.3 overlay`.
- `wiki/generate.py --all` und `--all --check`: `wiki: up to date, everything documented.`
- Planergänzung: Die bereits im Ausgangsbranch veralteten generierten Wiki-Daten (unter anderem seltene Shulker und Simple Money) werden für das verpflichtende `checkWiki` ebenfalls erneuert, getrennt vom Echolot-Fix committet. Keine zugehörige Gameplay- oder Modul-Quelländerung.
- Vollständige Server-Suite, Lauf `2026-10-02T22-07-07Z-864c`: Fabric 26.3 **864/864**, NeoForge 26.3 **864/864**, Ergebniszeile **`alles gruen: 1728/1728 bestanden, 0 rot`**. Beide neuen/angepassten Echolot-Tests sind im Datensatz als `passed` erfasst; ebenso Neubau und vollständige Item-/Blockabdeckung der Testzentrale auf beiden Loadern.
- Erster Compile-Versuch: vor der Java-Kompilierung an einer Windows-Dateisperre von `.gradle/mavenizer/repo/.../forge-26.3-66.0.8.jar` gescheitert. Danach nur den nachweislich eigenen untätigen Compile-Daemon beendet; Wiederholung von Compile und Gate seriell nach dem grünen Serverlauf.
- Die angeforderten Compiles liefen im seriellen Gate erfolgreich. `check` scheiterte anschließend in `testWikiModules` an der schon vorher fehlenden Einzelnotiz für `polished_ender_quartz_checker`, sichtbar durch die Pflicht-Regeneration. Kleine Planergänzung im getrennten Wiki-Commit: zweisprachige Einzelnotiz gemäß vorhandenem Rezept ergänzt und Familienprosa angepasst; keine Gameplay-Änderung.
- Abschließendes Gate einschließlich `:compileJava :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava check`: **`BUILD SUCCESSFUL in 6m 6s`**, **`GATE_AND_COMPILE_EXIT=0`**, 146 Tasks, davon 22 ausgeführt. `testWikiModules`: **38 Tests, OK**. Wiki, Balancing, Atlanten, Ressourcen, Overlays und reguläre Tests erfolgreich. Log: `scratchpad/echo-gate-final.log`. Nach dem grünen Serverlauf wurden nur noch Wiki-Prosa/abgeleitete Wiki-Daten und diese Dokumentation geändert; keine erneute Serversuite nötig.
- Kein Client gestartet; Sicht-/Audioabnahme, Besitzerwelt und Forge-GameTests bleiben offen.
