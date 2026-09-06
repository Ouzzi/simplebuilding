# Testläufe

Ein Aufruf statt vier, und danach eine Tabelle statt vier Logdateien.

```
python tools/testrunner/run.py
```

Das fährt alle vier Serverziele (Fabric und NeoForge, je MC 26.2 und 1.21.11),
wertet die JUnit-Berichte aus und schreibt einen Datensatz je Lauf. Rückgabewert 0
gibt es nur, wenn jedes Ziel mit Exit 0 **und** null roten Tests durchgelaufen ist.

## Client-Tests

Dazu gibt es vier Client-Ziele, die einen echten Minecraft-Client fahren und die
Renderer per Screenshot-Vergleich prüfen — Blockhervorhebung, Baustab-Vorschau,
die Menüs und den Mehrfach-Abbau.

```
python tools/testrunner/run.py --targets client            # nur die Client-Ziele
python tools/testrunner/run.py --targets everything        # Server und Client
```

Sie sind **nicht** in der Vorgabeauswahl: jeder startet einen Client und braucht
Minuten, wo der ganze Serverdurchlauf etwa zwei dauert. Beim `--release-gate`
laufen sie mit.

Client-Gametests schreiben keinen JUnit-Bericht, es gäbe also nur den Exit-Code —
und genau dem traut dieses Werkzeug nicht. Als Beweis dienen stattdessen die
Screenshots: welche der Test macht, steht als Zeichenkette in seinem Quelltext,
und nach dem Lauf muss jeder davon als **frische** Datei vorliegen. Bricht ein
Test auf halbem Weg ab, fehlen die späteren Aufnahmen, und ihre Namen sagen, wo
er stehengeblieben ist. Die Erwartung wird aus dem Quelltext gelesen und nicht
von Hand gepflegt — aus demselben Grund wie beim Katalog der Serverziele: eine
getrennt geführte Liste driftet, und eine gedriftete Erwartung ist schlimmer als
gar keine, weil sie grün wird.

## Beim Weiterentwickeln: nur das, woran man arbeitet

```
python tools/testrunner/run.py --targets neoforge-262 --filter "simplebuilding:block_behaviour_*"
```

`--list` zeigt die Ziele und je Testklasse den passenden Selektor. Die Auswahl
geht über die Gradle-Eigenschaft `-PgametestFilter`, die jede der vier
Laufkonfigurationen in das übersetzt, was ihr Loader versteht — ein
`--tests`-Argument bei NeoForge, die JVM-Eigenschaft
`fabric-api.gametest.filter` bei Fabric.

## Vor dem Veröffentlichen

```
python tools/testrunner/run.py --release-gate
```

Erst `gradlew check` und `wiki/generate.py --check`, dann alle acht Ziele
vollständig. Am Ende steht eine Go/No-Go-Zeile — daran hängt, ob gepusht und auf
Modrinth beziehungsweise CurseForge hochgeladen wird.

## Oberfläche

```
python tools/testrunner/serve.py
```

Öffnet `http://127.0.0.1:8765/`: Zustand des letzten Laufs je Ziel, der Verlauf
mit Zeitpunkt, Version und Commit, jeder Einzeltest mit seiner Historie über die
letzten Läufe, und Knöpfe zum Auslösen. Der Server bindet ausschließlich an
127.0.0.1 — er startet Gradle-Tasks und hat keine Anmeldung.

`testing/index.html` funktioniert auch direkt von der Festplatte; dann zeigt sie
den aufgezeichneten Verlauf und nennt statt der Knöpfe den passenden Befehl.

## Was hier gegen falsche grüne Läufe eingebaut ist

Drei Dinge, die alle drei schon einmal einen kaputten Lauf wie einen guten
aussehen ließen:

- **Der Exit-Code wird am Prozess abgenommen**, nicht an einer Shell-Pipeline.
  `gradlew … > log; echo $?` liefert den Rückgabewert von `echo`.
- **Der JUnit-Bericht muss von *diesem* Lauf stammen.** Ist die Datei älter als
  der Laufbeginn, gilt das Ziel als fehlerhaft — nicht als grün mit alten Zahlen.
- **Katalog und Lauf müssen sich decken.** Lief ein Test, den der Katalog nicht
  kennt, oder fehlt bei einem ungefilterten Lauf ein Test aus dem Katalog, ist
  das ein Fehler. Genau daran fiel auf, dass Fabric seine Ids aus dem
  Adapter-Methodennamen ableitet und dabei `ABlock` zu `ablock` zusammenzieht,
  während NeoForge den Katalognamen wörtlich nimmt — derselbe Test lief unter
  zwei verschiedenen Ids.

Der fremde Testfall, den Fabric aus der Umgebung `minecraft:default` mitbringt,
wird ausgewiesen, aber nicht mitgezählt.

## Ablage

`testing/runs/` und `testing/data/` entstehen bei jedem Lauf neu und sind
deshalb nicht versioniert; `testing/index.html` schon.
