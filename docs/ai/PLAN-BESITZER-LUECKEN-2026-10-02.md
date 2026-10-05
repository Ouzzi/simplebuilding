# Besitzer-Abgleich: drei Lücken (Ausführung 2026-10-05)

Branch: `gpt-gaps`. Je Aufgabe ein Commit, kein Push, keine Clients.

## 1. Küchenmesser
Generator und generiertes Rezept auf `"  N"/" NN"/"SN "` umstellen: ein Stock,
vier Eisennuggets. Wiki EN/DE, gemeinsame Modulsprachen und Sandwich-Plan §19 F8
korrigieren. Der vorhandene Werkzeugtest prüft kein Rezept; einen Rezept-GameTest
mit Spiegelung und Ablehnung des alten Zwei-Nugget-Musters ergänzen.
Simple Sandwiches hat einen gemeinsamen Sprachordner für alle Loader, keinen
zweiten 26.3-Sprachoverlay. Keine künstliche Sprachkopie anlegen.

## 2. Schmiedetisch-Rezeptbuch
Die drei `*_armor_upgrade_dummy`-Rezepte müssen für die Slot-Freischaltung erhalten
bleiben, dürfen aber keine falschen Ergebnisanzeigen mehr liefern. JEI-Filter und
Memory `workstations-open.md` beachten. Den Vanilla-Anzeigepfad untersuchen und
an der kleinsten zuverlässigen Stelle filtern; echte Aufwertungen weiter testen.

## 3. Nihil-Gewölbe
Preiszeile mit denselben Werten wie `astral_vault` ergänzen. Money-Linkgenerator
erzeugt bedingten Handel, Pool-Eintrag und Laufzeit-Preisindex. Die Analogie bedeutet
gleiche Preis-, Bestands- und Chancenwerte; keine neue Balanceformel.

## Verifikation und Risiken
Ressourcengeneratoren jeweils mit `--check`; sechs vollständige Serverziele:
Sandwiches, SimpleBuilding und Money jeweils Fabric/NeoForge 26.3.
Abschließend `check -q`, 26.2 `:compileJava :neoforge:compileJava`, Forge 26.3
`:mc26_3:forge:compileJava`, nur 26.3-Datagen und Wiki venv `--all` / uv `--all --check`.
Spiel-/Clientansicht und Testzentrale in der Besitzerwelt bleiben unberührt.
Generierte Änderungen auf unbeabsichtigte Port-/Zeilenendenänderungen prüfen.

## Befund und gewählte Umsetzung
- Messer: neuer `knife_recipe_shape`-GameTest, weil bisher nur Werkzeugwerte geprüft
  wurden. Testet Sollmuster, Spiegelung und Ablehnung des früheren Zwei-Nugget-Musters.
  Der neue Rezept-Tooltip verwendet die gemeinsamen EN/DE-Modulsprachen.
- Schmiedetisch: `RecipeManagerDisplayMixin` filtert die drei exakten IDs vor
  `RecipeManager.unpackRecipeInfo`. Dadurch bleiben Display-IDs lückenlos und
  Rezepte, Slot-PropertySets und alte Freischaltungen erhalten. JEI und Server
  verwenden dieselbe ID-Liste in `TrimUpgrades`. Das vorhandene 26.3-Flag
  `SMITHING_RECIPE_BOOK` lässt die 26.2-Linie unverändert.
- Der vollständige erste Hauptmod-Lauf zeigte eine echte Folgeabhängigkeit:
  `ServerRecipeBook` zählt hinzugefügte Displays, nicht gelernte Rezept-IDs.
  `GuideRecipeUnlockMixin` aktualisiert deshalb auch bei bekannten versteckten
  Gate-Rezepten die Handbuchfreischaltungen. Der bestehende Handbuch-GameTest
  hatte diesen Fehler auf beiden Loadern reproduziert.
- Money: Nihil entspricht Astral (Tier 4, Stundenparameter 25, Craft 0,
  Sicherheitsuntergrenze 8, fahrender Händler, Bestand 1, Chance 0,1).
  Der generierte Standardpreis beträgt 37 Scheine. `check_data.py` prüft die
  Analogie zusätzlich zu den bestehenden Bedingungen und Rückverwertungsgrenzen.
- `wiki --all` exportierte außerdem bereits im Ausgangsstand vorhandene
  Standing-Rod-Quellen/Prosa nach `wiki/data/simplebuilding.*`; diese nötige
  Aktualisierung ist kein neues Gameplay dieses Runs.

## Bisherige Verifikation
- Sandwich-Generator: `373 generated resources valid`.
- Money-Generator: `generated trades, optional additive tags and runtime price index current`.
- Money-Daten: `263 bounded buy-only prices, 11 conditional module tables`;
  `no diamond-exchange profit through single-material recipe/uncrafting chains`.
- Wiki venv `--all` und uv `--all --check`: `wiki: up to date, everything documented.`
- Erster Sandwich-Lauf: NeoForge 18/18; Fabric vor Teststart durch Python-Store-Stub
  im PATH blockiert. Mit dem vorgegebenen uv-Python im PATH behoben.
- Lauf `2026-10-04T23-05-08Z-d184`: je Modul/Loader 18/18; Hauptmod je 940/941
  wegen obiger Handbuchfolge. Dieser rote Lauf ist kein Abschlussnachweis.
- Korrigierter Gesamtlauf `2026-10-04T23-16-05Z-953c`:
  **`alles gruen: 1954/1954 bestanden, 0 rot`**. SimpleBuilding Fabric/NeoForge
  jeweils 941/941; Simple Sandwiches und Simple Money je Loader 18/18.
  Einschließlich Dummy-Display-Regression, Handbuchfreischaltung, echter
  Schmiedetisch-Platzierung und Testzentralen-Abdeckung.
- Abschluss-Datagen: ausschließlich `:mc26_3:fabric:runDatagen
  :mc26_3:fabric:syncGenerated263`, `BUILD SUCCESSFUL`. Kein neuer inhaltlicher
  Diff unter `mc26_3/generated`; keine Datagen für andere Minecraft-Linien.
- Anschließend erneut venv `wiki/generate.py --all` und uv
  `wiki/generate.py --all --check`: `wiki: up to date, everything documented.`
- Abschluss-Gate: `gradlew.bat check -q :compileJava :neoforge:compileJava
  -Pforge263=true :mc26_3:forge:compileJava`, **`GATE_EXIT=0`**.
  `checkBalance`: 223 erzeugte Stellen geprüft, 0 Fehler, 0 Hinweise.
  Wiki-Unittests: 54 Tests, `OK`. Nur bestehende API-/Compilerwarnungen.
- Offen: ausdrückliche Client-Sichtabnahme, Testzentralen-Neubau in der Besitzerwelt
  und Forge-Laufzeitprüfung. Keine Clienttests, kein Push/Merge und keine Ports.
