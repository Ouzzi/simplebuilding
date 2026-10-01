# Forge-Follow-up — Prüfnachweis 2026-10-01

Branch: `codex-next-forge`, Ausgangspunkt `5f294df9`.
Plan vor Implementierung: `docs/FORGE-FOLLOWUP-PLAN.md`.
Kein Push, Merge, Clientstart oder Zugriff auf Besitzer-Spielstände.

## Verhalten und Entscheidungen

- Dimensions besitzt den tatsächlichen Forge-Adapter: Register, Block-Entities,
  Tick-/Stop-Hooks, Interaktionen, Claims-Anbindung, Client-Farben, Konfiguration
  und alle 41 Serverfälle. Sechs Glowstone-Bögen, drei Zugriffsschalter und sichere
  Rückwege bleiben erhalten. Forge verwendet für geschütztes Block-Entity-NBT
  seinen tatsächlichen `onlyOpCanSetNbt()`-Hook.
- Tweaks bindet die zusammengeführten Claims-Hooks, Befehle und alle 47 Fälle an.
  Money aktiviert die vorhandenen Money-Links-Mixins mit echter Mod-Erkennung.
  Sounds prüft den tatsächlichen geladenen Visuals-Anbieter zusätzlich zum
  gemeinsamen Testkörper. Kein Test wurde durch einen Erfolg-Platzhalter ersetzt.
- Forge bleibt experimentell und opt-in. Servernachweise ersetzen keine
  Client-Abnahme; es gibt keine neue Richtlinienfrage an den Besitzer.
- Native Forge-Dialoge verwenden vorhandene Optionsdefinitionen, EN/DE-Texte,
  Kategorien, Vorgaben und Grenzen. SimpleBuilding erschließt alle 172 Optionen;
  SimpleModels behält seinen nativen Browser. Fremde Server erhalten keinen
  Gameplay-Schreibpfad. Dimensions verwendet weiterhin seinen lokalen Serverthread.
- SimpleBuildings bisheriger Default-Shim lädt und speichert jetzt validiertes
  JSON atomar. Fehlerhafte Originaldateien bleiben beim Laden erhalten; fehlende
  Gruppen werden ergänzt, Werte begrenzt und Schreibfehler gemeldet. QoL behält
  seine eigene Konfiguration und speichert ebenfalls atomar.
- Cloth selbst ist nicht verfügbar: Die [offiziellen Forge-Metadaten](https://maven.shedaniel.me/me/shedaniel/cloth/cloth-config-forge/maven-metadata.xml)
  enden bei 17.0.144, ohne 26.x-Artefakt. Das tatsächlich untersuchte NeoForge-
  Artefakt 26.3.159 verlangt NeoForge und ist kein Forge-Ersatz. Gepinnt bleibt
  Forge 66.0.8 / Minecraft 26.3, obwohl die [offizielle Forge-Seite](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html)
  inzwischen 66.0.9 aufführt.

## Generische Infrastruktur und notwendige Abweichungen

`gradle/forge-framework.gradle` verpackt das Framework als versionierte
Jar-in-Jar-Bibliothek. `Protection` samt verschachtelten Typen sowie
`CosmeticIntensity`/`Level` sind in den Distributables vorhanden. SimpleBuildings
Forge-JAR enthält außerdem seine vier Common-Bootstrap-Klassen und den
Protection-Service-Bridge. Lose Bibliotheken verursachten vorher JPMS-Duplikate.

`gradle/module-forge.gradle` verwendet eindeutige Modulkoordinaten und die
Manifest-Namensräume/Testabhängigkeiten. Die identischen bisherigen Gradle-
Koordinaten unterdrückten Dimensions als Tweaks-Testabhängigkeit. Der generische
`forgePackaged`-Prüfmodus entfernt Entwicklungsordner und lose Framework-/Common-
Bibliotheken aus beiden ForgeGradle-Klassenpfaden. Zehn tatsächliche Startpfade
wurden kontrolliert. `forgeTestMods` ermöglicht optionale Anbieterprüfungen.

`gradle/forge-native-config.gradle` bindet modulweise die vorhandenen Dialoge an
native Widgets. `tools/testrunner/run.py` berücksichtigt `SIMPLEBUILDING_JAVA8_HOME`
auch bei alten absoluten Manifestpfaden; vorhandene Overrides bleiben ohne diese
Umgebungsvariable erhalten. Keine modulspezifischen zentralen Schalter.

Der Forge-Befehlsbaum musste vor Erweiterungen registriert werden. Die Settings-
Reisen brauchen auf allen drei Loadern einen getrennten Test-Batch. Der vom
Orchestrator auf master gelandete Cleanup-Patch `a5d8151e` wurde für seine beiden
Java-Testdateien exakt übernommen; die zwischenzeitliche Worker-Accessor-Lösung
wurde entfernt. Vier Testkonfigurationen blieben nach dem vollständigen Lauf und
dem zweiten Forge-Lauf bytegenau unverändert (SHA-256-Vergleich).

## Tatsächliche Serverläufe

Für jede Zeile wurde die Ausgabe `alles gruen` gelesen. Die vollständigen
ausgewählten Kataloge haben frische Berichte, keine fehlenden Fälle und keine
Fehler. Gefilterte Läufe sind ausdrücklich gekennzeichnet.

| Run-ID in `testing/runs/` | Ergebnis | Umfang |
| --- | ---: | --- |
| `2026-10-01T01-43-00Z-7974` | 889/889 | Forge-Basis 801; Dimensions 41, Tweaks 47 vor Übernahme des Cleanup-Patches von master |
| `2026-10-01T01-48-47Z-7d7a` | 326/326 | Forge Money 17, Riding 26, Models 16, Fun 32, Visuals 18, Sounds 17, QoL 24; normale Dimensions-/Tweaks-Regressionen 176 |
| `2026-10-01T02-03-44Z-90ee` | 264/264 | Finaler Cleanup: Dimensions 41 + Tweaks 47 auf Fabric, NeoForge und Forge |
| `2026-10-01T02-11-47Z-315a` | 88/88 | Zweiter vollständiger Forge-Dimensions-/Tweaks-Lauf; Konfigurationen weiterhin identisch |
| `2026-10-01T02-13-59Z-1192` | 17/17 | Sounds mit installiertem Visuals; vorherige Sounds-/Visuals-Läufe jeweils ohne den anderen Anbieter |
| `2026-10-01T02-15-19Z-efa1` | 4/4 | Gewöhnlicher Forge-Entwicklungsstart, Filter `simpledimension:*claims*` |
| `2026-10-01T02-16-20Z-36d6` | 10/10 | Fabric/NeoForge, Filter `simplebuilding:*test_centre*`; Neubau und vollständige SimpleBuilding-Item-/Blockabdeckung |

Damit sind die 1.039 vorhandenen Forge-Fälle der Basis und neun echten Module
auf den jeweils zuletzt betroffenen Quellen belegt. Der Basislauf enthält auch
seine Testzentralen und die zusätzlichen Persistenz-Assertions. Alle Forge-Läufe
außer der ausdrücklich genannten Entwicklungsprobe verwendeten gebaute Mod-JARs.
Es wurde jeweils nur ein Gradle-Aufruf mit einem Worker ausgeführt; die späten
Gradle-/Forge-Prozesse waren auf 2 GiB Heap begrenzt.

Frühere rote Versuche zählen nicht als Nachweis: `01-14-07Z-d2f1`,
`01-17-09Z-a1e6`, `01-18-30Z-963b`, `01-21-12Z-a08e` und
`01-25-50Z-8b0f` (jeweils 2026-10-01). Ursachen und Korrekturen stehen im Plan:
Klassenpfadduplikate, rekursive Gradle-FileCollection, kollidierende Test-Batches,
ausgelaufene Settings und Befehls-/Abhängigkeitsreihenfolge.

## Weitere Prüfungen und Grenzen

- Wiki `--all` generiert und `--all --check`: Exit 0.
- Launch-Hub-/Registry-/Java-8-Tests: 45 bestanden; Handbuchprüfung: 0 Probleme.
- `tools/forge/inspect_jars.py`: 11 Distributables einschließlich Wiringexample
  geprüft; API, Loader-Metadaten, native Dialogregistrierung und EN/DE vorhanden.
- Default-Gate `gradlew.bat check -q`: **Exit 0**; gemeinsame 26.2-Kompilierung
  erhalten. Zusätzlich `gradlew.bat -Pforge263=true check :modules:wiringexample:forge:jar -q`:
  **Exit 0**. Beide mit dem verifizierten Java-8-Pfad und einem Gradle-Worker;
  Logs: `scratchpad/forge-followup/default-check.log` und `forge-check.log`.
  Die bestehenden Balance-, Atlas-, Jade-, Wiki- und Modul-Datenprüfungen liefen mit.
- Keine Quelländerungen in `mc1_21_11`, `mc26_4`, der 26.2-Overlay-Linie oder
  `common/src`. Keine neue Pixelkunst.

Nicht geprüft: GUI-Darstellung und Bedienung, reale Client-Paketzustellung,
Portal-Farbdarstellung, Audio, fremde Modpacks, Besitzerwelten sowie ein separater
Produktionsinstaller-/normaler Dedicated-Server-Start. Exakte serielle
Clientbefehle und die Abnahmefälle stehen in `docs/FORGE-26.3.md`, Abschnitt
„Serial client verification for the orchestrator“. Der Orchestrator führt den
vollständigen zusammengeführten Normal-/`integration-263`-Lauf aus.

## Commits und exakte Dateien

- `1eb4b1b9` — docs: plan merged Forge follow-up
- `df354b5e` — test: isolate portal settings and restore asynchronous fixtures
- `e4b1320b` — fix(forge): package runtime APIs and persist native settings
- `2049fd48` — feat(forge): complete Dimensions and merged module integrations

Der Dokumentationsabschluss ist der nachfolgende Commit dieser Datei; sein SHA
wird im finalen Worker-Bericht genannt. Die Prüfungen beziehen sich auf den
finalen Quellstand, nicht auf isoliert ausgecheckte Zwischencommits.

Dateiinventar gegenüber `5f294df9`:

```text
.claude/QUEUE.md
docs/FORGE-26.3.md
docs/FORGE-FOLLOWUP-PLAN.md
docs/FORGE-FOLLOWUP-RESULTS.md
docs/HANDOFF.md
docs/MULTIMOD.md
gradle/forge-framework.gradle
gradle/forge-native-config.gradle
gradle/forge-packaged-run.gradle
gradle/module-forge.gradle
gradle/native-config/ConfigBuilder.java
gradle/native-config/resources/assets/simplemods/lang/de_de.json
gradle/native-config/resources/assets/simplemods/lang/en_us.json
mc26_3/forge/build.gradle
mc26_3/forge/src/main/java/com/simplebuilding/command/ModCommands.java
mc26_3/forge/src/main/java/com/simplebuilding/forge/SimplebuildingForge.java
mc26_3/forge/src/main/java/com/simplebuilding/forge/client/ForgeConfigScreen.java
mc26_3/forge/src/main/java/com/simplebuilding/forge/gametest/ForgeConfigChecks.java
mc26_3/forge/src/main/java/com/simplebuilding/forge/gametest/ForgeGameTests.java
mc26_3/forge/src/main/java/me/shedaniel/autoconfig/AutoConfig.java
modules/modules.json
modules/simpledimensions/fabric/src/main/java/com/simplebuilding/modules/simpledimensions/ModuleGameTest.java
modules/simpledimensions/fabric/src/main/resources/data/simpledimension/test_environment/settings.json
modules/simpledimensions/forge/build.gradle
modules/simpledimensions/forge/src/main/java/com/simplebuilding/modules/simpledimensions/ForgeClientColors.java
modules/simpledimensions/forge/src/main/java/com/simplebuilding/modules/simpledimensions/ForgeExample.java
modules/simpledimensions/forge/src/main/java/com/simplebuilding/modules/simpledimensions/ModuleForgeTests.java
modules/simpledimensions/forge/src/main/java/com/simplebuilding/modules/simpledimensions/forge/mixin/RegistryLoadTaskMixin.java
modules/simpledimensions/forge/src/main/resources/META-INF/mods.toml
modules/simpledimensions/forge/src/main/resources/data/simpledimension/test_environment/settings.json
modules/simpledimensions/forge/src/main/resources/pack.mcmeta
modules/simpledimensions/forge/src/main/resources/simpledimension.forge.mixins.json
modules/simpledimensions/neoforge/src/main/java/com/simplebuilding/modules/simpledimensions/ModuleNeoTests.java
modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/DimensionSettingsTests.java
modules/simplefun/forge/build.gradle
modules/simplemodels/forge/build.gradle
modules/simplemoney/forge/build.gradle
modules/simplemoney/forge/src/main/java/com/simplebuilding/modules/simplemoney/forge/MoneyForge.java
modules/simplequalityoflife/forge/build.gradle
modules/simplequalityoflife/forge/src/main/java/com/simplebuilding/modules/simplequalityoflife/forge/QolForgeConfig.java
modules/simpleriding/forge/build.gradle
modules/simplesounds/forge/build.gradle
modules/simplesounds/forge/src/main/java/com/simplebuilding/modules/simplesounds/forge/ModuleForgeTests.java
modules/simpletweaks/forge/build.gradle
modules/simpletweaks/forge/src/main/java/com/simplebuilding/modules/simpletweaks/forge/ModuleForgeTests.java
modules/simpletweaks/forge/src/main/java/com/simplebuilding/modules/simpletweaks/forge/TweaksForge.java
modules/simpletweaks/forge/src/main/resources/data/simpletweaks/test_environment/claims_portal.json
modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimPortalTests.java
modules/simplevisuals/forge/build.gradle
tools/forge/inspect_jars.py
tools/launchhub/tests/test_forge_java_path.py
tools/testrunner/run.py
wiki/data/modules.js
wiki/data/simpledimensions.js
wiki/data/simpledimensions.json
```

## Gebaute Distributables

Die abschließende Prüfung nach beiden Gates meldete Exit 0. SHA-256:

| JAR | SHA-256 |
| --- | --- |
| `mc26_3\forge\build\libs\simplebuilding-26.3-forge-1.3.1.jar` | `540ac96f57d9dbd063d7e8353f4da9ab3dbbd17b6056a7084c61bb49c6c73a60` |
| `modules\wiringexample\forge\build\libs\wiringexample-26.3-forge-0.1.0.jar` | `e4b02544d64d9d892f5df6c347d571fcbf351f268ef93b39cc94b3b18c6b2569` |
| `modules\simplemoney\forge\build\libs\simplemoney-26.3-forge-1.2.16.jar` | `07e18b3ebb171398e7a7a6832a97a09cbade4acc81cf17ea47cbd6797d957d85` |
| `modules\simpleriding\forge\build\libs\simpleriding-26.3-forge-1.0.5.jar` | `a374954574455e957cc0a8e24ed8f05be803a67f669df006608b6ac91f655a64` |
| `modules\simplemodels\forge\build\libs\simplemodels-26.3-forge-0.1.0.jar` | `30cedf095d1086ac78b5676078d69576cfd1d7b3528b742a2fe1fc0d732e0b1f` |
| `modules\simplefun\forge\build\libs\simplefun-26.3-forge-1.3.0.jar` | `8770ef93784ebe5e938ea5c2fb49880e4d5861b7fca481f91c4a9ff5f7fba18a` |
| `modules\simplevisuals\forge\build\libs\simplevisuals-26.3-forge-1.0.7.jar` | `ca0558955bf22d9773e07c668ad30b2638cdb2d67a24f952a96d90a239b37fda` |
| `modules\simplesounds\forge\build\libs\simplesounds-26.3-forge-0.1.0.jar` | `f070a7815fac206a7ec02f9898bf617d8d825aca82e35c2b6d497570857ccafb` |
| `modules\simplequalityoflife\forge\build\libs\simplequalityoflife-26.3-forge-1.0.6.jar` | `1b9b9c6a8471532f711a21fc4193ed533349b30ac46108f092648dbb000df3c0` |
| `modules\simpletweaks\forge\build\libs\simpletweaks-26.3-forge-1.2.12.jar` | `9b8ad4a21ae2a2975691c2e536c9286caf83cec79859c85dc1ca8a82dea9e3ba` |
| `modules\simpledimensions\forge\build\libs\simpledimensions-26.3-forge-0.1.0.jar` | `fa285bb7f681753a15a579b87bbc20390f33985ca97bc63641fcd69e72522a98` |
