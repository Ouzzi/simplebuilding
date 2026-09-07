# Weg zu vollständiger Abdeckung

Stand: 2026-09-07, Commit `dd7606d`. Ziel ist, dass jedes Verhalten der Mod auf **beiden
Minecraft-Linien und beiden Modloadern** von einem Test gedeckt ist, der rot wird, wenn das
Verhalten kaputtgeht — und dass alles, was das nicht sein kann, benannt ist statt vergessen.

---

## 1. Wo wir stehen

### Serverseitig: fertig

| Ziel | Tests |
|---|---:|
| Fabric · MC 26.2 | 269 |
| NeoForge · MC 26.2 | 269 |
| Fabric · MC 1.21.11 | 266 |
| NeoForge · MC 1.21.11 | 266 |

**263 Tests tragen auf beiden Linien dieselbe Id** — ein Bericht der einen Linie lässt sich Zeile
für Zeile neben den der anderen legen. Die sechs Abweichungen stehen als `LINE_DIFFERENCES` in
`tools/testrunner/run.py`, und zwar als **Gegenstücke**, nicht als Ausnahmen: vier prüfen etwas,
das es auf 1.21.11 gar nicht gibt (datengetriebene Handelsangebote gibt es erst ab MC 26.1, die
gemeinsame `ConstructorsTouchInteraction` erst ab 26.2), zwei zeigen auf ihr Gegenüber auf der
anderen Linie, das dieselbe Aussage über einen anderen Mechanismus erreicht. Ein Eintrag sagt
damit nicht „ignorier das", sondern „das hier deckt es drüben ab" — und wird rot, sobald er nicht
mehr stimmt.

Eine siebte Abweichung war **keine** Abweichung, sondern eine echte Lücke:
`wandering_trader_can_roll_amod_trade` gab es nur auf 1.21.11, obwohl 26.2 die Angebote des
fahrenden Händlers genauso mitliefert — geprüft war dort nur, dass sie in den Pools *stehen*,
nicht, dass ein Händler sie auch *auswürfelt*. Das Paritätstor hat sie im ersten Lauf gefunden.
Der Test ist portiert und auf beiden 26.2-Loadern grün; die Gegenprobe (unsere Tag-Einträge
entfernt) macht ihn rot.

### Clientseitig: schief

| Ziel | Dateien | Prüfpunkte | Rückstand |
|---|---:|---:|---:|
| Fabric · MC 26.2 | 11 | **84** | — |
| Fabric · MC 1.21.11 | 7 | 16 | −68 |
| NeoForge · MC 26.2 | 7 | 12 | −73 |
| NeoForge · MC 1.21.11 | 7 | 12 | −73 |

Das ist die große offene Baustelle. Sie ist am 2026-09-07 entstanden, als die 56 clientseitigen
Lücken auf Fabric 26.2 geschlossen wurden; die anderen drei Ziele haben davon nichts bekommen.

---

## 2. Warum die Client-Parität nicht einfach nachzuziehen ist

Die beiden Loader benutzen **verschiedene Gerüste**, und das ist kein Schönheitsfehler:

- **Fabric** hat die `FabricClientGameTest`-API mit einer imperativen, blockierenden Oberfläche:
  `runOnClient`, `computeOnClient`, `waitTicks`, `waitFor`, `waitForScreen`, `setScreen`,
  `takeScreenshot`. Ein Test liest sich wie ein Ablauf.
- **NeoForge hat keine Client-Test-API.** In `neoforge/src/clientGameTest/` steht ein
  selbstgebauter Schrittautomat (`TestScript` mit `act` / `await` / `idle` / `step`), der über
  einen Tick-Rückruf getrieben wird. Ein Test ist dort eine Liste von Schritten, keine Abfolge
  von Anweisungen.

Ein Fabric-Test lässt sich also **nicht kopieren**, sondern muss neu ausgedrückt werden. Die
Absicht überträgt sich eins zu eins, der Code nicht.

Für die 1.21.11-Fabric-Seite gilt das nicht — dort ist es dieselbe API, also derselbe Fall wie
beim Server-Port: mechanische Übersetzung plus die API-Unterschiede, die der Compiler zeigt.

---

## 3. Die Arbeitspakete, nach Reihenfolge

### P1 — Audit neu erheben — **erledigt**

Erhoben am 2026-09-08 von 22 Prüfern, jede „ist abgedeckt"-Behauptung adversarisch gegengelesen,
Server- **und** Client-Tests als Deckung gezählt. Vollständiger Bericht:
[`AUDIT-2026-09-08.md`](AUDIT-2026-09-08.md), Rohdaten in `audit_offen.json` und
`audit_falsegreens.json`.

**1000 Verhaltensweisen, 702 gedeckt (70 %)** — das Audit vom 2026-09-03 kam auf 36 %. Offen sind
127 serverseitig schreibbare und 74 clientseitige Lücken; 95 stehen in den drei Restkategorien
(P4).

**Der eigentliche Fund sind 104 falsche Grün-Meldungen** (90 als „sicher" eingestuft). Eine davon
habe ich nachgestellt statt sie zu glauben: in `RotatorItem#getRimDirection` die z-Prüfungen der
Y-Fläche vor die x-Prüfungen gezogen — **269 von 269 Tests bleiben grün**, obwohl ein Klick auf
eine Ecke den Stamm danach in die falsche Achse legt. Kein Test im Repo klickt je eine Ecke. Das
ist jetzt P7.

### P2 — Fabric 1.21.11 auf Client-Parität bringen *(68 Prüfpunkte)*

Dieselbe API, also derselbe Weg wie beim Server-Port: übersetzen, was mechanisch geht, und den
Rest vom Compiler zeigen lassen. `tools/port_tests_to_1_21_11.py` deckt die Server-Regeln ab und
muss um die Client-Fälle erweitert werden.

Erwartbare Unterschiede: dieselben API-Brüche wie serverseitig (`EntityTypes`, `typeHolder`,
`sendOverlayMessage`), dazu alles, was mit Rendering zusammenhängt — die Submit-Pipeline hat sich
mit 26.2 stark geändert, und die 1.21.11-Renderer sind anders gebaut. **Damit ist zu rechnen, dass
ein Teil der Tests dort nicht nur übersetzt, sondern neu gedacht werden muss.**

### P3 — NeoForge-Client — **entschieden am 2026-09-08**

> **Gewählt: (c) Schrittform für alle**, dazu **ein Mixin auf `Minecraft.disconnect`**, damit
> alle Testklassen in einem Client-Start laufen. Jeder Client-Test wird einmal als
> Schrittliste gegen eine gemeinsame Fassade geschrieben, je Ziel ein dünner Treiber.
> Danach kostet ein neuer Client-Test eine Fassung statt vier.

#### Die Vorprüfung, die zu dieser Wahl geführt hat

Der Plan verlangte, *vor* der Entscheidung zu klären, ob NeoForges Schrittautomat alles kann, was
die Fabric-Tests brauchen. Nachgezählt an den 11 Fabric-Testklassen und nachgelesen in den Quellen
von `fabric-client-gametest-api-v1`:

**Was der Schrittautomat kann, ohne dass am Gerüst etwas zu bauen wäre.** Die Tests benutzen
167 × `waitTicks`, 93 × `computeOnClient`, 81 × `takeScreenshot`, 79 × `getInput`, 24 ×
`runOnClient`, 17 × `waitTick`, 15 × `waitForScreen`, 5 × `setScreen`, 2 × `waitFor`. Bis auf die
Eingaben bildet `TestScript` das alles direkt ab (`act` / `await` / `idle` / `step`);
`computeOnClient` wird zum Schreiben in ein Feld, weil es keinen Testthread gibt, der ein Ergebnis
entgegennähme. Die 79 Eingaben zerfallen in `pressKey`, `releaseKey`, `holdKey`, `pressMouse`,
`scroll`, `setCursorPos` und die Modifikatoren — Fabric setzt sie über einen Accessor auf
`KeyboardHandler#onKey` bzw. `MouseHandler` um, also über gewöhnliche Aufrufe auf dem
Client-Thread. Unter NeoForge kostet das einen Accessor-Mixin, keine Architektur.

**Was er heute nicht kann: eine Welt wieder verlassen.** Neun der elf Fabric-Klassen öffnen ihre
eigene Einzelspielerwelt in einem `try (TestSingleplayerContext …)` und schließen sie am Ende.
NeoForges Lauf legt eine flache Welt an und beendet danach die JVM (`Runtime.halt`), und zwar mit
Grund: `Minecraft.disconnect` pumpt selbst Client-Ticks, um seinen Fortschrittsbildschirm zu malen
— aus einem Client-Tick heraus aufgerufen kehrt es nie zurück. Das ist im Quelltext vermerkt
(`RendererProofRun`, Zeile 65 und 876) und war schon einmal die Ursache eines Deadlocks.

**Und der „dünne Adapter" aus (b) ist nicht dünn.** Fabrics blockierende API ist nicht bloß eine
Oberfläche: sie ersetzt die Tickschleife durch eine Vier-Phasen-Schranke (`Phaser` +
Semaphoren: Tick → Server-Tasks → Client-Tasks → Test) und braucht dafür 21 Mixin-Einsprungpunkte in vierzehn
Vanilla-Methoden — `Minecraft.run`, `runTick`, `runAllTasks`, `doWorldLoad`, **beide**
`disconnect`-Überladungen, `MinecraftServer.runServer` / `waitUntilNextTick` / `shouldRun`,
`BlockableEventLoop.schedule` / `doRunTask`, `Connection.channelRead0` / `sendPacket` und
`Main.main`. Genau dieses Gerüst macht `disconnect` überhaupt erst aufrufbar. Es unter NeoForge
nachzubauen heißt, ein Framework zu schreiben, keinen Adapter — zweimal, für beide MC-Linien.

#### Daraus folgt eine dritte Möglichkeit, die der Plan nicht kannte

**(c) Die Richtung umdrehen.** Der Schrittautomat ist die *schwächere* Abstraktion, und eine
schwächere lässt sich auf einer stärkeren mühelos ausdrücken — auf Fabric ist der Treiber eine
Schleife: `while (!script.tick(log)) context.waitTick();`. Umgekehrt geht es nicht, das ist
gerade der Befund oben. Also: jeden Client-Test **einmal** als Schrittliste gegen eine kleine
Fassade schreiben, und je Ziel einen dünnen Treiber — auf Fabric über die blockierende API, auf
NeoForge über den vorhandenen Tick-Treiber.

Eine Einschränkung dabei ist belegt und muss in die Fassade: Fabric besteht darauf, dass
`getInput()` **auf dem Testthread** läuft (`ThreadingImpl.checkOnGametestThread`), also gerade
nicht innerhalb von `runOnClient`. Eingabeschritte müssen deshalb eine eigene Schrittart sein und
dürfen nicht mit `act` vermischt werden.

#### Was die drei Wege kosten

| | zu schreiben | danach je neuer Client-Test | Risiko |
|---|---|---|---|
| **(a)** von Hand | 73 + 73 + 68 = **214 Prüfpunkte** | zwei Fassungen | gering, nur teuer |
| **(b)** Fabric-API auf NeoForge | 21 Vanilla-Eingriffe **× 2 Linien**, dann mechanischer Port | eine Fassung | hoch: Nachbau fremder Interna, bricht bei jedem MC-Update |
| **(c)** Schrittform für alle | 11 Klassen (8 692 Zeilen) **einmal** umschreiben + 2 Treiber | eine Fassung | mittel: grüne, funktionierende Tests werden angefasst |

Bei **(a)** und **(c)** bleibt der Weltwechsel unter NeoForge offen. Zwei ehrliche Auswege: je
Testklasse ein eigener Client-Start (elf Starts je Linie — langsam, aber ohne Vanilla-Eingriff),
oder doch ein einzelner Mixin auf `disconnect`. Der zweite ist ein kleiner, klar umrissener
Eingriff — nicht zu verwechseln mit dem ganzen Gerüst aus (b).

### P4 — Die drei Restkategorien neu triagieren *(95 Einträge)*

Zahlen aus dem Audit vom 2026-09-08. Die Einstufungen sind **Vorschläge der Prüfer, keine
Urteile** — beim letzten Mal war ein gutes Drittel davon inzwischen doch machbar:

| Kategorie | Zahl | Einschätzung |
|---|---:|---|
| strukturell blockiert | 30 | teils durch kleine Refaktorierungen erreichbar — so wie `OreDetectorItem.findTarget` |
| harness-blockiert | 24 | vor allem Töne und Partikel; der Ton-Rekorder in `SmokeClientGameTest` beweist, dass das geht |
| lohnt nicht | 41 | zu prüfen, ob nicht ein Teil inzwischen ohnehin gedeckt ist |

Der Rest — Fremdmod-Integration (`enderscape:stasis`), echte Weltgenerierung, Pakete an entfernte
Spieler — bleibt vermutlich offen. Das ist in Ordnung, solange es benannt ist.

### P5 — Das Release-Gate um die Parität erweitern — **erledigt**

`--release-gate` fuhr `gradlew check`, den Wiki-Abgleich und alle Ziele, prüfte aber **nicht**, ob
die Ziele dasselbe abdecken. Genau so ist die Client-Schieflage entstanden, ohne dass etwas rot
wurde: ein Test, den es auf einer Seite nicht gibt, ist auf der anderen grün, und Abwesenheit ist
das Einzige, was ein grüner Lauf nicht zeigen kann.

`check_parity()` in `tools/testrunner/run.py` schließt das und hängt im Gate. Es scheitert bei:

- einer Test-Id, die es nur auf einer Linie gibt und die nicht in `LINE_DIFFERENCES` steht;
- einem Eintrag in `LINE_DIFFERENCES`, der nicht mehr zutrifft (Test läuft inzwischen auf beiden
  Linien oder gar nicht mehr) — sonst wächst dort eine Liste von Ausreden zu;
- einem Client-Ziel, dessen Prüfpunkte hinter dem reichsten Ziel zurückliegen.

Der Client-Rückstand ist bekannt und steht als Zahl in `CLIENT_PARITY_DEBT`. Das Tor bleibt
deshalb rot — die Schuld ist keine Erlaubnis —, aber es unterscheidet jetzt drei Fälle: gleich
geblieben (bekannt, Verweis auf P2/P3), **gewachsen** (die laute Meldung, genau der Fall, den P5
verhindern soll) und geschrumpft (dann ist die Zahl nachzuziehen, sonst kann die Lücke unbemerkt
wieder wachsen).

Alle sechs Richtungen sind gegengeprüft, nicht nur behauptet: nicht erklärte Abweichung,
veraltete Erklärung, Rückstand gewachsen, geschrumpft, gar nicht eingetragen, Eintrag ohne
Rückstand — jeder Fall erzeugt seine eigene Meldung.

### P7 — Die 104 falschen Grün-Meldungen schärfen *(neu aus P1)*

**Das ist der wertvollste Posten der ganzen Liste**, und zwar weil er nicht Abwesenheit von Schutz
misst, sondern *vorgetäuschten* Schutz. Eine ungedeckte Stelle weiß man nicht; eine falsch grüne
glaubt man zu wissen. Genau deshalb kommt sie vor den neuen Tests.

`audit_falsegreens.json` nennt zu jedem Eintrag den Test, den behaupteten Anspruch und **eine
konkrete Änderung am Mod-Code, nach der die Suite grün bleibt**. Damit ist jeder Eintrag ohne
weitere Erhebung überprüfbar — und liefert gleich die Gegenprobe mit: Test schärfen, Mutation
einspielen, Test muss rot werden, Mutation zurück.

Verteilung: Maschinen 19, Lager 14, Werkzeuge 27, Verzauberungen 23, Rest 21.

### P6 — Mutationstests für die teuersten Tests

Beim Stapel 2 hat ein Prüfer echte Mutationstests gefahren: sieben Mutationen einzeln in
`TrimEffectUtil` eingespielt, je ein Lauf. Zwei wurden rot, **vier blieben grün** — drei davon
waren echte Lücken, die der Javadoc als gedeckt ausgab.

Das ist die schärfste Prüfung, die wir haben, und sie war ein Einzelfall. Für die Kernbereiche
(Verzauberungen, Werkzeuge, Schwerkraftblöcke) wäre sie systematisch mehr wert als weitere neue
Tests.

---

## 4. Wann „fertig" gilt

1. Alle vier Server-Ziele tragen dieselben Test-Ids, Abweichungen nur mit Begründung im Quelltext.
   **→ erreicht**, und seit P5 vom Tor erzwungen
2. Alle vier Client-Ziele tragen dieselben Prüfpunkte, Abweichungen nur mit Begründung.
   **→ offen, das ist P2 und P3**
3. Ein frisches Audit findet keine Lücke mehr, die mit einem Test erreichbar wäre.
   **→ Audit erhoben (P1). 201 erreichbare Lücken und 104 falsche Grün sind die Arbeit daraus:
   P7, dann die schreibbaren Lücken, dann P4.**
4. Das Release-Gate erzwingt 1 und 2, sodass die Parität nicht wieder still kippen kann.
   **→ erreicht.** Das Tor ist heute rot, und zwar aus genau einem Grund: dem Client-Rückstand
   aus Punkt 2. Grün wird es mit P2 und P3.
5. Jede Stelle, die kein Test erreicht, steht als „Not covered" im Quelltext, mit Grund.
   **→ weitgehend erreicht, mit dem Audit aus P1 zu bestätigen**

---

## 5. Was voraussichtlich dauerhaft offen bleibt

Ehrlich benannt, damit niemand es für eine Lücke hält:

- **Echte Weltgenerierung.** Im Testraum wird nie etwas generiert. Geprüft sind die Daten der
  Erzvorkommen und die Platzierungsfilter, nicht das Ergebnis in einer echten Welt.
- **Fremdmod-Integration.** `enderscape:stasis` lässt sich nicht positiv prüfen: kein Holder in
  der Testregistry trägt den Schlüssel, und ein selbstgebauter ist von außerhalb
  `net.minecraft.core` nicht bindbar.
- **Zufallsabhängige Pfade** ohne gesetzten Startwert — Luftersparnis, Amethyst-Heilung. Machbar,
  aber ein instabiler Test ist schlechter als gar keiner.
- **`NetheriteHopperBlockEntity`** ist toter Code. Nichts konstruiert es. Das ist kein Testthema,
  sondern eine Aufräumfrage.

---

## 6. Reihenfolge in einem Satz

~~**P1** (Audit)~~ → ~~**P5** (Gate)~~ → ~~**P3** (Entscheidung)~~ → **P7** (falsche Grün) →
**Servertests** für die 127 schreibbaren Lücken → **P3-Umsetzung** (Fassade + `disconnect`-Mixin)
→ **P2** (Client 1.21.11) und die 74 clientseitigen Lücken → **P4** (Restkategorien) → **P6**
(Mutationstests).

P5 war früh dran, weil ein Gate, das die Schieflage bemerkt hätte, sie gar nicht erst hätte
entstehen lassen — und es hat sich sofort bezahlt gemacht: Der erste Lauf hat den fehlenden
Händlertest gefunden, den vier grüne Server-Ziele nicht zeigen konnten.

**P7 vor die neuen Tests**, weil eine falsch grüne Stelle schlimmer ist als eine ungedeckte: die
eine täuscht Sicherheit vor, die andere ist wenigstens ehrlich. Und die Einträge bringen ihre
Gegenprobe schon mit.

Die Client-Arbeit kommt danach am Stück, weil Fassade, Treiber und Umschreiben zusammengehören —
in Scheiben zerlegt hätte man zwischendurch zwei halbe Gerüste.
