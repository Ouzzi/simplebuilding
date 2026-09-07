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

### P1 — Audit neu erheben *(zuerst, klein, verhindert Fehlplanung)*

Das Audit vom 2026-09-03 ist überholt. Seitdem sind 406 Lücken geschlossen, 138 falsche
Grün-Meldungen repariert, vier Mod-Fehler behoben und 56 Client-Lücken bearbeitet. Mehrere seiner
Einstufungen stimmen nicht mehr:

- „Kreativ-Tab" stand als *lohnt nicht* — ist inzwischen von `DataIntegrityTests` gedeckt.
- Sechs der zehn *harness-blockierten* Einträge waren Töne und Partikel. Der neue Ton-Rekorder in
  `SmokeClientGameTest` beweist, dass das ging; es fehlte nur die Aufzeichnung.

Ohne frische Zahlen planen wir auf dem Stand vom Wochenanfang. Das Vorgehen von damals hat sich
bewährt: je Feature ein Prüfer, jede Behauptung „ist abgedeckt" anschließend adversarisch
gegengelesen. Der Gegenlese-Schritt ist der wichtige — er hat beim ersten Mal 138 Tests entlarvt,
die wie Schutz aussahen und keiner waren.

### P2 — Fabric 1.21.11 auf Client-Parität bringen *(68 Prüfpunkte)*

Dieselbe API, also derselbe Weg wie beim Server-Port: übersetzen, was mechanisch geht, und den
Rest vom Compiler zeigen lassen. `tools/port_tests_to_1_21_11.py` deckt die Server-Regeln ab und
muss um die Client-Fälle erweitert werden.

Erwartbare Unterschiede: dieselben API-Brüche wie serverseitig (`EntityTypes`, `typeHolder`,
`sendOverlayMessage`), dazu alles, was mit Rendering zusammenhängt — die Submit-Pipeline hat sich
mit 26.2 stark geändert, und die 1.21.11-Renderer sind anders gebaut. **Damit ist zu rechnen, dass
ein Teil der Tests dort nicht nur übersetzt, sondern neu gedacht werden muss.**

### P3 — Entscheidung: NeoForge-Client, Adapter oder Handarbeit *(73 Prüfpunkte, zweimal)*

Zwei Wege, und die Wahl gehört dem Nutzer:

**(a) Jeden Test von Hand im `TestScript`-Idiom nachbauen.** Ehrlich, aber teuer: 73 Prüfpunkte
je NeoForge-Ziel, und jeder künftige Client-Test muss danach zweimal geschrieben werden.

**(b) Einen dünnen Adapter bauen**, der über `TestScript` eine Oberfläche legt, die der
`FabricClientGameTest`-API entspricht — dann können die Testkörper geteilt werden, so wie es
serverseitig mit dem gemeinsamen Katalog längst der Fall ist. Einmal teurer, danach kostet jeder
neue Client-Test nur noch eine Fassung.

Der Adapter ist die bessere Investition, wenn noch nennenswert Client-Tests dazukommen sollen —
und danach sieht es aus. Er ist die schlechtere, wenn die Client-Abdeckung hier endet.

Zu klären ist dabei, ob NeoForges Schrittautomat alles kann, was die Fabric-Tests brauchen: der
Ton-Rekorder, das Setzen von Screens, `computeOnClient`. Das ist vor der Entscheidung zu prüfen,
nicht danach.

### P4 — Die drei Restkategorien neu triagieren *(28 Einträge)*

Aus dem alten Audit, jeweils gegen den heutigen Stand zu halten:

| Kategorie | Zahl | Einschätzung heute |
|---|---:|---|
| harness-blockiert | 10 | mindestens 6 (Töne, Partikel) sind jetzt machbar |
| strukturell blockiert | 6 | teils durch kleine Refaktorierungen erreichbar — so wie `OreDetectorItem.findTarget` |
| lohnt nicht | 12 | mindestens 2 sind inzwischen ohnehin gedeckt |

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
   **→ offen, das ist P1 und P4**
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

~~**P1** (frisches Audit)~~ → ~~**P5** (Gate)~~ → **P2** (Fabric 1.21.11) → **P3**
(Entscheidung NeoForge, dann Umsetzung) → **P4** (Restkategorien) → **P6** (Mutationstests).

P5 war früh dran, weil ein Gate, das die Schieflage bemerkt hätte, sie gar nicht erst hätte
entstehen lassen — und es hat sich sofort bezahlt gemacht: Der erste Lauf hat den fehlenden
Händlertest gefunden, den vier grüne Server-Ziele nicht zeigen konnten.

Als Nächstes ist **P3** zu entscheiden, bevor P2 anfängt: Fällt die Wahl auf den Adapter, sollten
die 1.21.11-Client-Tests gleich in der geteilten Form geschrieben werden statt zweimal.
