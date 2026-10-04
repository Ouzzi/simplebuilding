# Vorlagen teurer machen – 2026-10-04

## Plan (vor Umsetzung)

Branch: `claude-gpt-templates`. Nur hier committen, niemals pushen.

### Befund und Entscheidung

Glowing und Emitting werden nicht an der Werkbank hergestellt: Eine abgelegte
Vanilla-Besatzvorlage wird mit dem Hammer und einem Nebenhand-Material umgewandelt.
Pulsating hat ein formloses Rezept aus Hammer und einer Echoscherbe. Eigene
Kopierrezepte bestehen bisher nur für Basic (7 Goldbarren + Eisenblock + Vorlage)
und Enderite (7 Diamanten + Endstein + Vorlage). Die Schmiedetisch-Rezepte wenden
die verbrauchte Vorlage auf Rüstung an; sie erzeugen keine neuen Vorlagen.

26.3 erhält ein eigenes `EXPENSIVE_TEMPLATES`-Flag, der 26.2-Zwilling bleibt false.
Die bestehenden Herstellungswege bleiben erhalten:

| Vorlage | Bisher | Neu auf 26.3 |
|---|---|---|
| Glowing | Vanilla-Besatzvorlage + 1 Leuchttintenbeutel, Hammer | Vorlage + 2 Leuchttintenbeutel + 2 Glowstone-Blöcke + 4 Diamanten, Hammer |
| Emitting | Vanilla-Besatzvorlage + 1 Glowstonestaub, Hammer | Vorlage + 2 Glowstonestaub + 2 Lohenpulver + 4 Diamanten, Hammer |
| Pulsating | 1 Echoscherbe + Hammer | 2 Echoscherben + 2 Sculk + 4 Diamanten + Hammer (9 Werkbankplätze) |
| Kopie Glowing/Emitting/Pulsating | kein eigenes Rezept | Vorlage + 7 Diamanten + Glowstone/Magmablock/Sculk ergibt 2 Vorlagen |
| Kopie Basic | Vorlage + 7 Goldbarren + Eisenblock ergibt 2 | Vorlage + 7 Diamanten + Eisenblock ergibt 2 |
| Kopie Enderite | Vorlage + 7 Diamanten + Endstein ergibt 2 | unverändert |

Glowing und Pulsating sind vor allem optisch; vier Diamanten und zwei thematische
Materialarten verteuern den Einstieg, ohne einen Bosskampf vorauszusetzen.
Emitting liefert nutzbares Licht; Lohenpulver verlangt zusätzlich Nether-Material.
Ein Netherstern oder ein neu erfundener Stern-Anteil wäre dafür überzogen.
Vanilla-Vorlagen bleiben für Glowing/Emitting erforderlich. Kopien liefern netto
nur eine neue Vorlage und folgen exakt dem Vanilla-Kostenrahmen. Die teuren
Echoscherben werden nur für den ersten Pulsating-Zugang benötigt, danach Sculk.
Schmiedetisch-Anwendung und Hammerhaltbarkeit bleiben unverändert.

### Dateien und Risiken

- McVersion-Zwillinge, ModRecipeProvider: Flag, Pulsating, Kopierrezepte.
- SledgehammerEntityInteraction/PlacedTemplates: Materialprüfung vor jedem Schlag,
  vollständiger Verbrauch erst beim letzten Schlag, Creative ohne Verbrauch.
  Nebenhand bleibt der Katalysator; weitere Materialien kommen aus dem Inventar.
- InWorldTransformations/InWorldRecipeCatalog: dieselben Materialkosten in
  Datagen-Export, JEI/REI und Wiki; keine versteckten Zusatzkosten.
- EN/DE in beiden Sprachorten; Wiki-Prosa und `wiki/tests/test_facts.py`.
- GameTests: vollständige/fehlende Materialien, Verbrauch, Hammerreste,
  Kopierausgabe, Ablehnung alter Billigrezepte, Export. 26.2 behält alte Erwartungen.

Risiken: veraltete In-World-Prosa, unvollständiger Materialverbrauch, doppelte
Rezept-IDs, unerwünschte Datagen-Änderungen anderer Linien. Diffs ausdrücklich prüfen.

### Verifikation

1. Nur `:mc26_3:fabric:runDatagen :mc26_3:fabric:syncGenerated263`.
2. Wiki mit venv `--all`, mit uv-Python `--all --check`; Fakten-Tests.
3. `gradlew.bat check -q`, 26.2 `:compileJava :neoforge:compileJava`,
   Forge 26.3 `-Pforge263=true :mc26_3:forge:compileJava` im aktuellen Worktree.
4. Vollständige Server-Suiten `fabric-263,neoforge-263`; Ausgabe muss
   `alles gruen` enthalten. Enthält Neubau und Abdeckung der Testzentrale.
5. Keine Minecraft-Clients starten. Sichtprüfung JEI im Spiel und Besitzer-Abnahme
   bleiben offen. Keine Ports nach 26.2/1.21.11/26.4.

## Umsetzung und Ergebnis

Die Umsetzung folgt dem Plan. Datagen wurde ausschließlich für 26.3 ausgeführt.
Glowing/Emitting verwenden weiterhin abgelegte Vorlagen; der gemeinsame Export
enthält jetzt auch die zusätzlichen Inventar-Materialien. JEI/REI und Wiki lesen
die Mengen daraus. Die Anwendung auf Rüstung blieb unverändert.

Die neuen Rezepttests prüfen alle neun Zutatenplätze, die Ablehnung des alten
Pulsating-Rezepts, sämtliche fünf Kopierrezepte und genau zwei ausgegebene
Vorlagen ohne Zutatenreste. Der In-World-Test prüft fehlende Materialien,
geteilte Diamantstapel, Materialentzug vor dem letzten Schlag, exakten Verbrauch
und Creative ohne Verbrauch. Die bestehenden Hammer-Restetests bleiben erhalten.

Der erste vollständige Serverlauf hatte nur den neuen Materialmangel-Test rot:
`makeMockServerPlayerInLevel()` überschreibt `gameMode()` fest mit CREATIVE.
`setGameMode(SURVIVAL)` ändert diesen Rückgabewert nicht. Gegen die lokale
Minecraft-Klasse mit javap belegt; der Test verwendet jetzt einen regulären,
verbundenen ServerPlayer. Keine Änderung der Gameplay-Regel zur Testanpassung.
Die gezielte Gegenprüfung auf beiden Loadern ist grün: **2/2 bestanden, 0 rot**
(Run `2026-10-04T15-45-50Z-2a89`).

Bereits bestätigt: Datagen `BUILD SUCCESSFUL`, Wiki venv `--all`, uv-Python
`--all --check` mit `wiki: up to date, everything documented.`, alle fünf Tests
in `wiki.tests.test_facts` grün, geänderte JSON-Dateien ohne doppelte Schlüssel.

Handbuchprüfung: Die geänderten Rezeptseiten passen. Zwei bereits in HEAD
vorhandene deutsche Themenübersichten (`guide topics 1`: 14 Zeilen,
`guide topics 3`: 15 Zeilen) überschreiten die Grenze. Die Gegenprüfung las
HEAD nur im Speicher; keine Quelldatei wurde dafür überschrieben.

Vollständiger Server-Wiederholungslauf `2026-10-04T15-57-00Z-f93f`:
**Fabric 917/917, NeoForge 917/917; alles gruen: 1834/1834 bestanden, 0 rot.**
Enthalten: vollständiger Neubau der Testzentrale in den separaten Testwelten und
Prüfung, dass jedes Mod-Item und jeder Mod-Block abgedeckt ist.

Der erste zusätzliche Gate-Lauf wurde wegen RAM-Mangels (nur etwa 630 MB von
24 GB frei) über seinen eindeutig zugeordneten Gradle-Client abgebrochen.
Keine fremden Prozesse wurden beendet. Das Gesamt-Gate wird nach den grünen
Serversuiten mit `--max-workers=1` seriell wiederholt: **GRADLE_EXIT=0**.
Ausgeführter Befehl: `gradlew.bat check -q :compileJava :neoforge:compileJava
-Pforge263=true :mc26_3:forge:compileJava --max-workers=1` mit der venv als
`wikiPython`. `checkBalance`: 223 erzeugte Stellen geprüft, 0 Fehler, 0 Hinweise.
Die vollständige Wiki-Testsuite im Gate: **50 Tests, OK**. Die Gradle-Ausgabe
enthält vorhandene API-/Annotationswarnungen, keine fehlgeschlagene Prüfung.

Die vorgemerkten Generator-Daten umfassen nur acht neue Rezept-/Advancement-Dateien,
den Kostenexport und die beiden Wiki-Hauptdateien. Keine inhaltlichen Änderungen
in `src/main/generated`, `mc1_21_11` oder `mc26_4`. Alle Abschlusslogs liegen unter
`.ai-runs/templates-2026-10-04/` (nicht versioniert).

Offen: Besitzer-Abnahme der Balance und Sichtprüfung von JEI/REI im Spiel;
keine Minecraft-Clienttests oder Forge-Laufzeittests. Die zwei genannten alten
Handbuchüberläufe bleiben bestehen. Port-Run erst nach Abnahme. Kein Push.
