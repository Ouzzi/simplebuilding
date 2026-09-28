# Quests & Advancements

Stand 2026-09-28. Gilt für alle Linien (26.2 Fabric/NeoForge/Forge, 26.3, 26.4-Snapshot, 1.21.11
Fabric/NeoForge).

## Fortschritte (Advancements)

Vier eigene Tabs wie bei Vanilla (Story / Nether / End / Abenteuer), Quelle
`src/main/java/com/simplebuilding/datagen/ModAdvancementProvider.java` (1.21.11: Kopie unter
`mc1_21_11/fabric/src/main/java/...`), Texte `advancements.simplebuilding.*` in beiden Sprachdateien.

| Tab (Wurzel) | Hintergrund | öffnet mit | Inhalt |
|---|---|---|---|
| SimpleBuilding (`root`) | Stein | Werkbank | Vorschlaghammer, Werkzeugstufen Kupfer→Netherit, Maschinen, Lager |
| Bauen & Blaupausen (`building/root`) | Ziegel | irgendein Meißel/Baustab/Oktant/Rotator/Baulicht | Meißel, Kerne & Baustab, Oktant, Blaupausen, Deko-Blöcke |
| Gadgets & Kniffe (`tweaks/root`) | Kupferblock | Kupfer-/Diamant-Druckplatte, Magnet, Detektor, Messer, Linse, Netherit-Essen, Lohenkopf | Gadgets, alle Pads, Netherit-Apfel |
| Jenseits des Endes (`end/root`) | End | Endstein | End-Minerale & -Blöcke, Enderit und alles daraus |

Dazu die zwei Teaser in Vanilla-Tabs (`story/hammer_time`, `nether/nugget_of_wisdom`) und die
versteckte Easter-Kette (`easter/*`, unverändert). Entscheidungen:

- **Keine Belohnungen** (wie Vanilla). Die Themen-Handbücher sind billig (Einsteiger-Handbuch + ein
  Gegenstand) und ihre Rezepte werden schon über die Rezept-Fortschritte freigeschaltet.
- **Ids sind Spielstand**: ein Fortschritt darf in einen anderen Zweig/Tab umziehen (Elternteil ändern),
  aber nie umbenannt werden. Bestehende Ids blieben alle erhalten.
- Kriterien: `inventory_changed` für "besitzt X" (zählt auch Funde), `simplebuilding:feature_used`
  für Aktionen (`ModTriggers`).

Test: `AdvancementTreeTests` (Filter `simplebuilding:advancement_*`) - genau vier Wurzeln mit
Hintergrund, jeder Tab mindestens fünf Einträge, alles übersetzt, keine Zyklen.

## FTB Quests (optional)

Ist [FTB Quests](https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge) installiert, legt die
Mod beim ersten Start eine Kapitelgruppe **SimpleBuilding** im Questbuch an. Ohne FTB Quests passiert
nichts; die Mod hat keine Abhängigkeit und keine FTB-Klasse.

### Kapitel (Tabs)

| Kapitel | Art | wird frei, wenn | Inhalt |
|---|---|---|---|
| Stufe 1: Erste Schritte | Stufe 1 | sofort | Werkbank, Stein, Eisen, Steinmeißel, Stein-Vorschlaghammer, Lederplatte, Rucksack, Handel → Kupferkern → Kupfer-Baustab |
| Stufe 2: Eisen & Diamanten | Stufe 2 | Stufe 1 fertig (Abschluss: Eisenspitzhacke) | Eisen-/Gold-/Diamantstufe, Diamantblock → Kiesel → Rissiger Diamant → verstärkte Maschine, Verzaubern |
| Stufe 3: Der Nether | Stufe 3 | Stufe 2 fertig (Abschluss: Netherportal) | Festung, Bastion, Netheritklumpen, Netherit & Vorlage, Netherit-Hammer, Maschine vor Ort schmieden, Druckplatten, Startrampe, Lohenkopf, Trank-Pad |
| Stufe 4: Das Ende und darüber hinaus | Stufe 4 | Stufe 3 fertig (Abschluss: Enderauge folgen) | Ende, Drache, Endsiedlung, Astralit/Nihilith, Enderquarz, Rohenderit → Platten → Barren, Enderit-Vorlage, Rüstung, Werkzeug, Hammer, Maschine; Abschluss: volle Enderit-Rüstung |
| Bauen & Blaupausen | Nebenkapitel | erster Steinmeißel | Oktant, Baulicht, Schachbrett, Besatz-Vorlagen, Diamantmeißel, Rotator, End-Paletten, Schwebeblöcke, Blaupause (nach Enderquarz), Scannen/Kopieren/Bauen |
| Lagerung | Nebenkapitel | erster Rucksack | Bündel, Köcher, verstärkte und Netherit-/Enderit-Stufen |
| Gadgets & Kniffe | Nebenkapitel | Start | Kupfer-Druckplatte, Geschwindigkeitsmesser, Magnet/Linse/Detektor (optional, seltene Kerne), Netherit-/Enderit-Essen, Chunk-Lader, Spawn-Teleporter, Elytra-/Flug-/Trank-Pad-Stufen |

109 Quests, davon die seltenen/teuren (Basis-Vorlage, Wither, Kerne ab Eisen, Echolot, verzauberte
Äpfel, höchste Pad-Stufen, Themen-Handbücher) **optional**.

**Stufen:** Jedes Stufen-Kapitel endet mit einer Zahnrad-Quest ("Stufe n geschafft"), die von jeder
nicht optionalen Quest des Kapitels abhängt; jede Quest der Stufe n+1 hängt (über ihre Abhängigkeiten)
an dieser Abschluss-Quest. Die Kapitel stehen im Modus `flexible`: Aufgaben lassen sich schon vorher
erfüllen - wer ein Item zufällig findet oder schon trägt, hat die Aufgabe erledigt, die Quest schließt,
sobald ihre Abhängigkeiten fertig sind. Aufgaben sind Item-Aufgaben (Inventar) oder
Fortschritts-Aufgaben (Vanilla-Meilensteine und die Mod-Fortschritte oben) - Quests und Fortschritte
erzählen dieselbe Geschichte.

**Texte:** FTB Quests wertet einen Übersetzungseintrag, der wie eine JSON-Textkomponente aussieht,
als Komponente aus. Jeder Titel/jede Beschreibung im Questbuch ist deshalb `{"translate": "<key>"}`;
die Texte stehen als `quests.simplebuilding.*` (Deutsch und Englisch) in den Sprachdateien der Mod
und erscheinen in der Sprache des Spielers. Quests mit Fortschritts-Aufgabe zeigen Titel und
Beschreibung dieses Fortschritts (Vanilla- oder Mod-Schlüssel).

### Warum eine Installation und kein Datenpaket

FTB Quests lädt sein ganzes Questbuch aus `config/ftbquests/quests/` (`ServerQuestFile`:
`configPath().resolve("ftbquests/quests")`) und kennt keinen Datenpaket- oder API-Weg für
Standard-Quests anderer Mods. Die Formate unterscheiden sich je Version:

| FTB Quests | Minecraft | Format | Sprachtexte |
|---|---|---|---|
| 2111.x | 1.21.11 | SNBT (`data.snbt`, `chapter_groups.snbt`, `chapters/*.snbt`) | eine Datei je Sprache: `lang/en_us.snbt` |
| 26.1.x (Branch `dev`) | 26.1+ | JSON5 (`data.json5`, `chapter_groups.json5`, `chapters/*.json5`) | Ordner je Sprache, alle Dateien werden zusammengeführt: `lang/en_us/**.json5` |

Deshalb liefert jede Linie ihr eigenes Format aus, beide aus derselben Quelle erzeugt:

- 26.x: `src/main/resources/data/simplebuilding/ftbquests/` (JSON5, als striktes JSON geschrieben)
- 1.21.11: `mc1_21_11/fabric/src/main/resources/data/simplebuilding/ftbquests/` (SNBT, mit Kommas -
  FTB Library und Vanillas `TagParser` lesen es beide)

`com.simplebuilding.compat.FtbQuestsDefaults` (beide Linien identisch; aufgerufen aus jedem
Loader-Einstieg mit dessen Config-Ordner) arbeitet `install.txt` ab, **bevor** eine Welt startet:

- `file <pfad>` - nur kopieren, wenn die Datei fehlt. Kapiteldateien heißen `simplebuilding_*`,
  kollidieren also nie mit denen eines Modpacks. `data.*` wird nur bei einem neuen Buch geschrieben.
- `list chapter_groups.* ...` - neues Buch: ganze Datei; bestehendes Buch: die Gruppe wird vorn in die
  Liste `chapter_groups` eingefügt (nur wenn ihre Id noch fehlt).
- `map lang/en_us.snbt ...` (nur 1.21.11) - bestehende Sprachdatei: unsere Einträge werden vorn
  eingefügt. Auf 26.x liegen die Texte in eigenen Dateien `lang/en_us/chapters/simplebuilding_*.json5`,
  genau dort, wo FTB Quests sie selbst speichert.
- Danach `simplebuilding.installed` mit der Datenversion. Solange sie nicht steigt, installiert die
  Mod nichts mehr - **gelöschte Kapitel bleiben gelöscht**. Neu installieren: die Marker-Datei löschen.

Nichts Vorhandenes wird überschrieben; jeder Fehler wird geloggt und verschluckt. Ids sind stabile
Hashes (`sha256("simplebuilding/<art>/<schlüssel>")`, 16 Hex-Ziffern, positiv), damit ein neuer
Datenstand dieselben Quests wiedererkennt.

### Bearbeiten

Alles steht in `tools/quests/generate_quests.py` (Kapitel, Quests, Abhängigkeiten, Texte Englisch +
Deutsch). Danach:

```bash
python tools/quests/generate_quests.py          # schreibt beide Linien + Sprachschlüssel
python tools/quests/generate_quests.py --check  # prüft nur (läuft in gradlew check: checkQuests)
```

Das Skript prüft: bekannte Quests in allen Abhängigkeiten, keine Zyklen, genau eine Abschluss-Quest je
Stufe, jede Stufen-Quest wartet auf das Ende der vorigen Stufe und nie auf ein Nebenkapitel, jedes
Mod-Item und jeder Mod-Fortschritt existiert in der Datagen-Ausgabe beider Linien (nach neuen
Fortschritten erst Datagen laufen lassen). `DATA_VERSION` erhöhen, wenn bestehende Installationen neue
Dateien bekommen sollen.

Game-Tests (beide Linien, Filter `simplebuilding:advancement_*`):
`theFtbQuestsBookIsCompleteAndFormsStages` (alle ausgelieferten Dateien parsen, Items registriert,
Fortschritte geladen, Übersetzungen vorhanden, Stufen-Regel) und
`installingTheQuestBookAddsButNeverOverwrites` (Installation in ein leeres und in ein bestehendes Buch
in einem Temp-Ordner).

### Grenzen

- Ungetestet gegen ein laufendes FTB Quests (es gibt noch kein FTB Quests für 26.2+/26.3/26.4; das
  Format folgt dem Quellcode des `dev`-Branches, 26.1.2). Bricht FTB das Format, fällt es beim Laden
  des Buchs auf, nicht beim Start der Mod.
- Ändert jemand einen Titel im Questbuch, speichert FTB Quests ihn als festen Text - dann ist er
  nicht mehr zweisprachig.
- Aufgaben mit "eins von mehreren Items" gibt es in FTB Quests nur über Filter-Items; wo mehrere
  Stufen zählen sollen, nutzt die Quest deshalb den passenden Mod-Fortschritt (der jede Stufe zählt).
