# Weg zu vollständiger Abdeckung

Stand: 2026-09-10, Commit `2fa1710`. Ziel ist, dass jedes Verhalten der Mod auf **beiden
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

### Clientseitig: seit dem 2026-09-10 gerade

| Ziel | Prüfpunkte | Rückstand |
|---|---:|---:|
| Fabric · MC 26.2 | **85** | — |
| Fabric · MC 1.21.11 | **85** | — |
| NeoForge · MC 26.2 | **85** | — |
| NeoForge · MC 1.21.11 | **85** | — |

Alle vier Ziele fahren **dieselben Testkörper**: einmal als Schrittliste geschrieben, je Ziel ein
dünner Treiber. `CLIENT_PARITY_DEBT` ist leer — nicht erlassen, sondern bezahlt. Das Tor fällt ab
sofort sofort rot, wenn ein Ziel zurückfällt, statt gegen eine geduldete Zahl zu prüfen.

Die Schieflage war am 2026-09-07 entstanden, als die 56 clientseitigen Lücken auf Fabric 26.2
geschlossen wurden und die anderen drei Ziele davon nichts bekamen. Gefunden hat sie das
Paritätstor aus P5, das genau dafür gebaut wurde.

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

### P2 — Fabric 1.21.11 auf Client-Parität bringen — **erledigt am 2026-09-10**

Fiel mit P3 zusammen ab: die gemeinsame Schrittform wurde nach `mc1_21_11/shared/clientgametest/`
heruntergeportet, beide Treiber dazu. **Erster Lauf: 85 von 85 grün.**

Die Erwartung „ein Teil der Tests muss dort neu gedacht werden" hat sich **nicht** bestätigt.
Von 11 685 Zeilen waren 131 Compilerfehler zu beheben, alle mechanisch, jeder gegen die
Klassendatei der 1.21.11-Jar geprüft und im Quelltext als Linienunterschied benannt:

| Was | 26.2 | 1.21.11 |
|---|---|---|
| Bildschirm | `client.gui.screen()` | `client.screen` |
| HUD | eigenes `Hud`-Objekt mit `isHidden()`/`toggle()` | `client.options.hideGui` |
| Toasts | `client.gui.toastManager()` | `client.getToastManager()` |
| Bündelinhalt | `ItemStackTemplate` | `ItemStack` |
| Textfarben | `TextColor.AQUA` | `TextColor.fromLegacyFormat(...)` |
| Tastenbindung | `matches(InputConstants.Key)` | `matches(KeyEvent)` → hier `saveString()` |
| Blockmodelle | `BlockStateModelSet` / `BlockStateModelPart` | `BlockModelShaper` / `BlockModelPart` |
| GUI-Aufnahme | `GuiGraphicsExtractor`, `renderer.state.gui` | `GuiGraphics`, `gui.render.state` |
| Abbau-Zustände | `renderer.state.level`, Zugriffsmethoden | `renderer.state`, Felder |
| Chat stumm | `setVisibleMessageFilter` | `Options.chatVisibility` = `HIDDEN` |

Zwei Unterschiede betreffen die Treiber, nicht die Testkörper: Fabrics
`fabric-client-gametest-api-v1` **4.3.5** kennt weder `getConnection()` noch
`waitForClientboundPackets()` (`awaitPackets` antwortet dort wie auf NeoForge sofort — im
`Harness`-Javadoc benannt statt kaschiert), und das Extraktionsereignis heißt dort
`WorldRenderEvents.END_EXTRACTION` statt `LevelExtractionEvents.END_EXTRACTION`.

### P3 — NeoForge-Client — **entschieden am 2026-09-08**

> **Gewählt: (c) Schrittform für alle**, dazu **ein Mixin auf `Minecraft.disconnect`**, damit
> alle Testklassen in einem Client-Start laufen. Jeder Client-Test wird einmal als
> Schrittliste gegen eine gemeinsame Fassade geschrieben, je Ziel ein dünner Treiber.
> Danach kostet ein neuer Client-Test eine Fassung statt vier.

#### Stand der Umsetzung (2026-09-09)

**Alle neun Client-Testklassen sind in der geteilten Schrittform.** Die alten Fabric-Klassen sind
gelöscht, als Einstiegspunkt bleibt ein Treiber je Loader. Die Testkörper liegen unter
`common/src/shared/clientgametest/java/com/simplebuilding/clientgametest/`.

**Der `disconnect`-Eingriff entfiel.** Alle Tests laufen in *einer* Welt, weil `TestScene.build`
die Szene ohnehin vollständig zurücksetzt. Das war die riskanteste Unbekannte des Pakets.

**Was das Teilen aufgedeckt hat** — Dinge, die vier getrennte Suiten nicht zeigen konnten:

- *Fabrics Oktant-Nachweis war schwächer.* NeoForge verglich nur die rechte Bildhälfte, weil ein
  gehaltener Oktant oben links ein Panel zeichnet; Fabric verglich das ganze Bild, das Panel konnte
  den Nachweis also allein tragen. Jetzt gilt die strengere Form für beide.
- *Fabric verschluckte Befehlsfehler.* `Commands.performCommand` fängt die Ausnahme — dieselbe
  Sorte Schweigen, die dieser Suite acht tote Spielregeln beschert hat. Beide Treiber gehen jetzt
  direkt über den Dispatcher.
- *Der Abbau hörte nie auf* (`stopDestroyBlock` fehlte), und die Abbaufälle teilten sich eine Wand,
  die der erste einriss. Beides war auch vorher schon da.

#### Was danach noch drei Löcher waren — und was sie wirklich waren (2026-09-10)

NeoForge stand nach der Umstellung bei 68 von 85. Von den 17 roten Prüfpunkten gingen 15 auf
**zwei Fehler im Gerüst** zurück, nicht auf die Tests:

- *Mausklicks erreichten keinen Bildschirm.* Sie liefen nur über die Bindungsebene
  (`KeyMapping.set` / `click`), ein Bildschirm liest aber **Ereignisse**. Der Knopf, der den
  Besatz-Bildschirm öffnet, wurde also nie gedrückt. Jetzt derselbe Weg wie bei Fabric:
  `MouseHandler.onButton`. Wichtig dabei — `onButton` ruft die Bindungsebene **selbst** auf, wenn
  kein Bildschirm offen ist; beides zu tun ist ein Doppelklick, und der hat den Bilderrahmen erst
  gesperrt und sofort wieder entsperrt.
- *Der Zeiger landete neben dem Inventar.* `setCursorPos` rechnete die GUI-Skalierung ein zweites
  Mal ein, obwohl der Aufrufer sie bereits umgerechnet hatte. Fabrics API nimmt rohe Fensterpixel;
  jetzt tut das auch der NeoForge-Treiber.

Dazu ein Fehler, der **beide** Loader betraf und den erst ein gescheitertes Skript sichtbar
gemacht hat: die Freigabeschritte eines Skripts laufen nicht mehr, wenn ein Schritt davor
scheitert. Eine hängengebliebene Sneak-Taste hat danach `item-rendering` und `hud-and-tooltip` rot
gemacht — und schuld war jedes Mal die Mod. `Harness.releaseAllInput()` räumt jetzt nach **jedem**
Skript auf, bestanden wie gescheitert.

**Ergebnis: 83 von 85 auf NeoForge 26.2.**

**Offen und benannt:** `breaking-d-strip-miner-sneaking` erreicht auf NeoForge keine
Zerstörungsstufe; sechs Ursachen sind ausgemessen und ausgeschlossen. Der Zustand *zum Zeitpunkt
der Zeitüberschreitung* kann drei ganz verschiedene Fehlschläge nicht auseinanderhalten — Abbau
nie begonnen, Abbau unterbrochen, Abbau fertig und der Block schon wieder weg —, alle drei enden
mit `isDestroying=false, stage=-1`. Seit dem 2026-09-10 zeichnet `MiningTrace` das Fenster mit
auf; der nächste Lauf sagt, welcher der drei es ist.

Tastatureingaben *innerhalb* eines Bildschirms sind auf NeoForge weiterhin nicht erreichbar —
dafür fehlt der Accessor auf `KeyboardHandler.onKey`. Für die Maus ist genau dieses Argument
inzwischen in roten Tests bezahlt worden, also ist es beim ersten geteilten Test, der tippt, das
Erste, was zu tun ist.

**Nebenbefund des Paritätstors:** der Kontrollfall „Octant weggenommen" existierte nur im alten
handgeschriebenen NeoForge-Beweis. Ohne ihn erklärt „das Bild driftet ohnehin" jede
Octant-Messung genauso gut. Als `highlight-k-octant-removed` in die gemeinsame Form aufgenommen;
damit war der alte Beweis vollständig abgelöst und ist gelöscht.

#### Wie die Umsetzung aussieht — Vorarbeit vom 2026-09-08

**Wo die geteilten Testkörper liegen.** Genau wie serverseitig: ein gemeinsames Quellverzeichnis,
das jedes Loader-Modul einhängt. Für den Server ist das `common/src/shared/java`; für die
Client-Tests kommt `common/src/shared/clientgametest/java` dazu, eingehängt in Fabrics
`gametest`-Sourceset und NeoForges `clientGameTest`-Sourceset (analog auf der 1.21.11-Linie).
NeoForges Sourceset ist bereits sauber getrennt und hat eine eigene `neoforge.mods.toml`, taucht
also in keinem ausgelieferten Jar auf — diese Eigenschaft muss die Erweiterung behalten.

**Beide Seiten teilen schon die Szene.** `RendererProofRun` und die Fabric-Tests bauen dieselbe
Geometrie (Wand bei z=20, Boden y=−1, Spieler bei 10.5/0/16.5, Fadenkreuz auf Block 10/1/20).
Das ist die halbe Miete für gemeinsame Testkörper und war eine bewusste Entscheidung, kein Zufall.

**Der `disconnect`-Mixin ist aufwendiger als gedacht — aber immer noch nicht Variante (b).**
Das Problem ist nicht, dass `Minecraft.disconnect` verboten wäre, sondern dass es *selbst
Client-Ticks pumpt*, um seinen Fortschrittsbildschirm zu malen. Aus einem Tick heraus aufgerufen
tritt es in `runTick` wieder ein und kehrt nie zurück. Ein Mixin, der die Methode nur umleitet,
reicht deshalb nicht: der Abmeldevorgang muss **nicht-blockierend** werden, also am Kopf
abgebrochen und über mehrere Ticks selbst abgewickelt werden. Fabric macht genau das (Einschüsse
in beide `disconnect`-Überladungen plus `runTick`).

Damit stehen für den Weltwechsel drei Wege, in dieser Reihenfolge zu prüfen:
1. **Ein nicht-blockierender `disconnect`** in einer Klasse des Testmods — der ehrlichste Weg,
   Aufwand mittel, ein Eingriff statt einundzwanzig.
2. **Gar nicht abmelden**: alle Testklassen in *einer* Welt laufen lassen, mit einer Szene, die
   zwischen den Klassen zurückgesetzt wird. Kostet nichts, verlangt aber, dass keine Testklasse
   Welteigenschaften braucht, die eine andere stört.
3. **Ein Client-Start je Testklasse** — elf Starts je Linie, langsam, aber ohne jeden Eingriff.

Weg 2 ist zuerst zu versuchen, weil er ohne Vanilla-Eingriff auskommt und die Fabric-Tests ohnehin
alle dieselbe Szene aufbauen. Erst wenn eine Testklasse nachweislich eine eigene Welt braucht,
lohnt Weg 1.

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

### P4 — Die drei Restkategorien neu triagieren — **erledigt am 2026-09-10**

Alle 97 Einträge einzeln gegengelesen; Urteil und Grund je Eintrag in `audit_offen.json` unter
`p4`, Bericht in [`P4-TRIAGE-2026-09-10.md`](P4-TRIAGE-2026-09-10.md).

| Urteil | Zahl |
|---|---:|
| jetzt schreibbar (Client) — meist, weil die geteilte Schrittform Töne hört, Bildschirme ausliest und einen echten Überlebensspieler hat | 9 |
| jetzt schreibbar (Server) — Köcher tragen seit dem 2026-09-09 `EQUIPPABLE`; `makeMockPlayer` liefert einen Nicht-`ServerPlayer` | 2 |
| seit dem Audit gedeckt (Trichter-Ordinal, NeoForge-Luftsprung) | 2 |
| **bekannter Defekt, Entscheidung des Besitzers** | 27 |
| bleibt begründet offen (tote Zweige, Tautologien, von Vanilla getragen) | 47 |
| weiterhin harness-blockiert (Selbstausschluss bei `player.playSound`, zweite Dimension, Serverstart-Haken, echte Weltgenerierung) | 10 |

Die 27 Entscheidungen sind keine Testlücken, sondern Verhalten, das ein Test nur zementieren
würde: sieben davon betreffen das Forge-Modul (kein Gametest, kein HUD, eigene Kopien der Logik),
vier die Enderit-Stufe in Tags (Stab, Bündel, Köcher — nach der Entscheidung vom 2026-09-09
vermutlich nachzuziehen), vier die Öfen (Sprachschlüssel, Werkzeug-Tags, Leuchtstärke,
Glas-Eigenschaften), der Rest Einzelfälle (Trichter-Broadcast, `loadAdditional` ohne Klemme,
Container-Index, Netherit-Kolben-Signal, `ModCommands` vierfach, Cloth-Config-Aufruf, die vier
stummen Töne des Bilderrahmens, das Handbuch zum Trade-Rebalance).

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

**81 der 104 sind serverseitig, 23 clientseitig.** Die clientseitigen warten auf die
P3-Umstellung — sonst schreibe ich sie zweimal: einmal jetzt in der Fabric-Form und gleich
danach nochmal als Schrittliste.

Stand: **serverseitig fertig** (Commits `dd833db`, `c8d5226`, `4718d2c`, `540aa85`).
80 von 81 geschärft, jede einzeln durch ihre Mutation belegt — 70 Mutationen eingespielt,
70-mal rot. Die eine Ablehnung ist begründet: der Wert `-1` wird vom Vanilla-Konstruktor
`BundleContents(List)` erzwungen, die Mod-Zeile davor ist folgenlos, ein Verhaltensbruch also
nicht konstruierbar.

**Clientseitig (2026-09-10): alle 21 geschärft**, in der geteilten Schrittform, also auf allen vier
Zielen zugleich. Zwei der 23 aus der ersten Zählung waren Doppelnennungen desselben Eintrags.
Die Mutationen stehen mit Datei, Ankertext und erwarteter Meldung in
`tools/testrunner/mutations.py`; das Werkzeug spielt sie ein, fährt ein Client-Ziel, verlangt
genau die erwartete Meldung im Log und **keinen** Nebenschaden in einem Skript ohne Mutation, und
nimmt sie aus git zurück. 22 Mutationen (21 Client + 2 Server, eine davon deckt zwei Einträge),
9 Client-Runden (eine Mutation je Skript und Runde, weil ein Skript am ersten roten Schritt hält).

Was dabei über den Bildschirm hinaus nötig wurde, weil ein Pixelvergleich es nicht sagen kann:
der GUI-Renderzustand wird jetzt **ausgelesen** (`extractScreenState`, `drawnTexts`,
`filledRectangles`) — Knopfgeometrie, Glyphe und Farbe je Filtermodus, Overlay-Farbe, Geisterbild
nur im leeren Slot; die Rangefinder-Zeilen werden als Text gelesen (Volumen 80 statt 36 bei
gelöschtem `+ 1`); der Schwerpunkt der gemalten Fläche (`ScreenshotDiff.changedArea`) entscheidet,
ob die Geisterblöcke um ihre Mitte schrumpfen; ein Zähler-Mixin je Loader zählt die
`SpaceKeyPayload`s auf dem Server (nur bei Änderung, nicht je Tick).

| Behaupteter Test | Einträge | Stand |
|---|---:|---|
| `HudAndTooltipClientTest` (Trichterfilter, Geisterslots, Slotklicks, Rangefinder, Bündel-Skala) | 11 | geschärft |
| `ClientBootstrapClientTest` (Modifikatoren, Auswahltasten, Zweithand, Farben, Kreativ-Sperre, Leertaste) | 6 | geschärft |
| `BuildingWandPreviewClientTest` (kein Blocktreffer, Schwerpunkt) | 2 | geschärft |
| `BlockHighlightClientTest` / `MultiBlockBreakingClientTest` | 2 | geschärft |
| `SmokeClientTest` (alle fünf Sync-Felder) | 1 | geschärft |
| Server: Trichter-Ordinal, nur PICKUP | 2 | geschärft bzw. schon gedeckt, per Mutation zu belegen |

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
   **→ erreicht am 2026-09-10** (P2 und P3): ein Baum, 94 Prüfpunkte je Ziel, `CLIENT_PARITY_DEBT`
   leer, die 1.21.11-Kopie per Werkzeug reproduzierbar und vom Tor geprüft
3. Ein frisches Audit findet keine Lücke mehr, die mit einem Test erreichbar wäre.
   **→ Audit erhoben (P1). 201 erreichbare Lücken und 104 falsche Grün sind die Arbeit daraus:
   P7, dann die schreibbaren Lücken, dann P4.**
4. Das Release-Gate erzwingt 1 und 2, sodass die Parität nicht wieder still kippen kann.
   **→ erreicht.** Seit dem 2026-09-10 prüft es auch, ob die 1.21.11-Client-Kopie hinter dem
   gemeinsamen Baum zurückhängt (`port_client_tests_to_1_21_11.py --check`).
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
