# Balancing-Zentrale (Stand 2026-09-29, Phase 2)

Lokaler Entwicklungs-Server fuer **alle** Balance-Werte der Mod: Beute, Handel, Mob-/Block-Drops, Erze,
Werkzeug- und Ruestungswerte, Verzauberungen, Rezepte, Code-Konstanten (Ladungen, Abklingzeiten,
Reichweiten, Tempo), Config-Standards, Trank-Pad-Regeln - mit Versionen, Rueckgaengig, Rechnern fuer die
Beschaffungszeit und den Entwickler-Dokumenten. **Seit Phase 2 wirken gespeicherte Werte in der Mod**:
Speichern schreibt die Zahl an ihre Stelle im Java- oder JSON-Quelltext - in allen Minecraft-Linien -,
"Datagen starten" baut die erzeugten Dateien neu, und `gradlew checkBalance` (im `check`) haelt Ablage,
Code und erzeugte Dateien zusammen.

## 1. Starten und der Ablauf in einem Satz

```
python tools/devserver/serve.py            # Zentrale auf http://127.0.0.1:8770/
python tools/devserver/serve.py --check    # checkBalance (Exit-Code 1 bei Abweichung); gradlew checkBalance ruft das auf
```

Optionen: `--no-browser`, `--port 8771`, `--store <ordner>` (andere Ablage, z. B. zum Ausprobieren),
`--repo <ordner>` (anderer Checkout, z. B. ein Worktree), `--refresh-vanilla` (Vanilla-Handelspools und
-Namen neu aus dem Client-Jar lesen), `--check --json`. In Claude Code gibt es den Eintrag
`balancing-zentrale` in `.claude/launch.json`. Nur Python-Standardbibliothek.

**Ablauf:** Wert aendern (Entwurf, alter Wert rot durchgestrichen) -> *Speichern* zeigt je Wert alt/neu,
die Stellen je Linie (Datei:Zeile), betroffene Items, Spieltests, die den Wert festhalten -> *Bestaetigen*
legt Version `vN` an und schreibt in die Mod -> auf der *Uebersicht* "Datagen starten" (baut die
erzeugten Dateien aller Linien neu, zeigt die geaenderten Dateien und das Ergebnis von checkBalance) ->
committen. *Versionen -> auf diesen Stand* setzt alles zurueck, mit den Original-Literalen: danach ist
`git diff` leer.

Laufen Dev-Clients/-Server des Besitzers im selben Checkout, verweigert "Datagen starten" (sonst stuerzt
das Spiel mit `NoClassDefFoundError` ab) - "trotzdem starten" nur bewusst.

Tests: `python -m unittest discover -s tools/devserver/tests` (Auslesen, Rechner, Ablage inkl.
gleichzeitiger Schreibzugriffe und Absturz-Wiederherstellung, Pruefung ungueltiger Werte, Schreiben in
JSON und Java auf Repo-Kopien inkl. Rueckgaengig bis aufs Byte, checkBalance, Datagen-Auftrag, HTTP-API).
Die Tests auf dem echten Repo laufen mit einem schreibgeschuetzten Service (`read_only`).

## 2. Was die Zentrale liest und wohin Speichern schreibt

Gelesen wird die Linie 26.2. Jeder schreibbare Wert kennt seine **Stelle** (Java: Datei + Zeichenbereich
des Literals; JSON: Datei + Pfad) und die **Zwillinge** in den anderen Linien (`sbdev/sites.py`):

| Datei der Linie 26.2 | gilt fuer | Zwillinge |
|---|---|---|
| `common/src/shared/java/X` | 26.2, 26.3, 26.4 | 1.21.11: `mc1_21_11/shared/java/X` |
| `common/src/mc26_2/java/X` | 26.2 | 26.3: `mc26_3/overlay/java/X` (26.4 erbt ihn), 26.4: `mc26_4/overlay/java/X`, 1.21.11 |
| `src/main/java/X` (Datagen) | 26.2, 26.3 | 1.21.11: `mc1_21_11/fabric/src/main/java/X` |
| `src/main/resources/data/...` (Handel) | 26.2, 26.3 | 1.21.11: `ModTradeDefinitions.java`, wo eindeutig |

Ein Zwilling wird mitgeschrieben, wenn dort dieselbe Id (derselbe Leser, andere Datei) mit **demselben
Wert** steht. Weicht er ab oder fehlt er, steht das am Wert (Tooltip der Linien, Speichern-Dialog) und die
Linie bleibt, wie sie ist - nie eine stille Luecke.

| Bereich | Quelle / Stelle | Linien | danach |
|---|---|---|---|
| Beute (Gewichte, Wuerfe, Mengen) | Literale in `ModLootTableModifications.java` (`sbdev/ex_loot.py`) | 26.2-26.4 + 1.21.11 | Datagen (Inject-Tabellen `loot_table/inject/...`) |
| Kern-Chancen | Konstanten `*_CORE_CHANCE` derselben Datei; Faktor `worldGen.buildingCoreLootChanceMultiplier` = Config | 26.2-26.4 + 1.21.11 | Datagen |
| Handel | `villager_trade/**.json` (jsonedit) + `ModTradeDefinitions.java` (Preis, 2. Preis, Menge, Nutzungen, XP, Rabatt, Angebots-Chance, Pool-Gewichte wo der Pool nur einem Angebot gehoert) | 26.2/26.3 (+ 1.21.11) | - |
| Config-Standards (167) | Feldinitialisierer in `SimplebuildingConfig`, `TweaksConfig`, `ServerTuningConfig` (Zahl, an/aus, Text) | 26.2-26.4 + 1.21.11 | Wiki (`python wiki/generate.py`, laeuft im Datagen mit) |
| Code-Konstanten | `static final` mit Balance-Namen (`sbdev/ex_constants.py`); Ausdruecke `64*4`, `190 * BASE_...`, `-3.0f - OFFSET`: das Literal wird zurueckgerechnet ("transform"); `NAME * 2`: Verweis auf NAME | 26.2-26.4 + 1.21.11 | Datagen, wenn ein Item sie nutzt |
| Enderit-Materialien | Konstruktor-Argumente `ModToolMaterials.ENDERITE`, `ModArmorMaterials.ENDERITE` (Haltbarkeit, Tempo, Bonus, Verzauberbarkeit, Schutz je Teil, Haerte, Rueckstoss) | 26.2-26.4 + 1.21.11 | Datagen (Item-Export) |
| Werkzeug-/Ruestungswerte | Export `items.json`; jeder Wert zeigt auf seine Quelle: Konstante (`alias`, z. B. `DURABILITY_IRON`, Material, Config-Standard der Ladungen) oder Zahl im Registrierungs-Code (`enchantable(15)`, `stacksTo(16)`, Ruestungsfaktor 42 x Vanilla-Grundwert) | wie die Quelle | Datagen |
| Verzauberungen | `Enchantment.definition(...)` und `LevelBasedValue.perLevel(...)` in `ModEnchantments.java` (Gewicht, Stufen, Kosten, Amboss, Wirkungswerte) | 26.2-26.4 + 1.21.11 | Datagen |
| Rezepte | Menge `shaped/shapeless(..., n)`, Erfahrung und Garzeit `oreBlasting(...)` in `ModRecipeProvider.java` | 26.2/26.3 + 1.21.11 | Datagen |
| Erz-Generierung | `OreConfiguration`/`OreFeature` (Adergroesse), `CountPlacement`, `HeightRangePlacement` in `ModWorldGen.java` | 26.2, 26.3/26.4 (Overlay), 1.21.11 | Datagen |
| Trank-Pads | `PotionPadRules.TABLE` und `DEFAULT` (Lauf AA) - sobald die Datei existiert, Seite *Trank-Pads* | 26.2-26.4 (+ 1.21.11) | - |
| Annahmen der Rechner | `sbdev/params.py` | - | wirken nie in der Mod |

Erzeugte Werte (Verzauberungen, Rezepte, Erze, Item-Export) zeigt die Zentrale mit der **Java-Stelle**
als Quelle; die erzeugte JSON-Stelle steht als `source.generated` daneben, `generatedValue` ist, was dort
steht. Was sich nicht eindeutig einer Zahl im Code zuordnen laesst, bleibt **nur Planung** - mit Grund
(Tooltip und Auslese-Bericht), z. B. Rezeptmengen aus Vanilla-Mustern (Treppe 4, Stufe 6, Steinsaege),
Angriffswerte und Kapazitaeten (berechnet), die Verzauberbarkeit der Vorschlaghaemmer (setzt das
Werkzeugmaterial, die `ENCHANTABILITY_*`-Konstante im Aufruf ist dort wirkungslos).

Was **nicht** auslesbar ist, steht auf der Seite *Auslese-Bericht* (Datei, Zeile, Grund).

## 3. Bearbeiten, Speichern, Versionen

* Jedes Feld ist ein Entwurf, sobald es sich aendert: der **alte Wert steht rot durchgestrichen daneben**,
  die Leiste unten zaehlt die ungespeicherten Aenderungen. Entwuerfe liegen im Browser (localStorage).
* Ein Item-Wert, der auf eine Konstante zeigt, bearbeitet **die Konstante** (angezeigt x Faktor; ein
  Baustab mit `DURABILITY_COPPER_SLEDGEHAMMER * 2` nimmt nur gerade Zahlen). Der Speichern-Dialog nennt
  alle Items, die sich mit aendern.
* **Speichern** zeigt immer erst die Zusammenfassung: alt -> neu, je Wert die Stellen je Linie
  (`26.2/26.3/26.4: ModLootTableModifications.java:95 · 1.21.11: ...:94`), "danach Datagen",
  Zwillinge, die abweichen, betroffene Items, Spieltests, die den Wert festhalten
  (`ConfigOptionTests.EXPECTED_OPTIONS`, die Kern-Baender `CORE_CHANCES`). Ungueltiges (keine Zahl,
  ausserhalb der Grenzen, kein Vielfaches bei `64*4`, nur lesbare Werte, unbekannte Ids) blockiert.
* Vor dem Schreiben prueft die Zentrale **alle Stellen**: steht irgendwo nicht mehr der eingelesene Wert
  (paralleler Lauf, Editor), wird nichts gespeichert und nichts geschrieben ("Neu einlesen"). Scheitert
  das Schreiben einer spaeteren Datei, werden die schon geschriebenen zurueckgesetzt.
* Schreibweise bleibt: Suffix (`f`, `F`, `L`), Hex, Unterstriche, Nullen am Ende (`0.10f` -> `0.20f`),
  CRLF/LF. Das Original-Literal jeder Stelle steht in der Ablage (`originText`); Rueckgaengig schreibt
  genau dieses zurueck.
* Jede Bestaetigung ist eine neue **Version** `vN`, keine wird geloescht. **Zuruecksetzen** auf eine alte
  Version legt eine neue Version mit deren Inhalt an (gleiche Vorschau), schreibt die Mod mit zurueck und
  entfernt Plaene, die es damals nicht gab.
* Ohne den Haken "Werte direkt in die Mod schreiben" bleibt ein Wert **geplant**; die Uebersicht bietet
  spaeter "Jetzt anwenden".

### Ablage

```
balance/balance.json                 aktueller Stand (atomar ersetzt)
balance/versions/v0001.json          jede Version, unveraenderlich (Aenderungsliste + kompletter Stand)
balance/versions/v0001.applied.json  welche Stellen diese Version geschrieben hat (Datei, Linie, alt/neu)
balance/applied-log.jsonl            spaeteres "Jetzt anwenden"
```

Eintrag je Wert: `{"value": Plan, "mod": Mod-Wert beim Speichern, "origin": Mod-Wert beim ersten Planen,
"applied": true (in die Mod geschrieben), "originText": [Original-Literal je Stelle]}`. Nur geplante Werte
stehen in der Ablage. `balance/` gehoert ins Repo (die Sperrdatei `balance/.lock` nicht). Schutz gegen
Verlust wie in Phase 1: Commit-Punkt ist die Versionsdatei, Sperrdatei, `baseVersion`.

### Anzeigen

| Anzeige | Bedeutung |
|---|---|
| **wirkt in Mod** + Linien (`26.2-26.4 + 1.21.11`) | Speichern schreibt den Wert an diese Stellen |
| **wirkt in Mod** + "ueber X" | Item-Wert: geaendert wird die Konstante/der Standard X |
| **nur Planung** | die Zentrale kann ihn nicht schreiben (Grund im Tooltip) |
| **Rechner** | Annahme der Rechner; wirkt nie in der Mod |
| geplant | gespeicherter Plan weicht vom Mod-Wert ab (nicht angewendet) |
| in Mod | gespeicherter Plan steht so in der Mod |
| **Code weicht ab** | angewendet, aber im Code steht inzwischen etwas anderes - checkBalance rot |
| Mod geaendert | der Mod-Wert hat sich seit dem Speichern bewegt (z. B. paralleler Lauf) |
| Export alt | der Item-Export ist aelter als der Code - Datagen fehlt |
| verwaist | den Wert gibt es in der Mod nicht mehr (Umbenennung?) - bleibt, bis er bewusst entfernt wird |

## 4. Datagen und checkBalance

**Datagen starten** (Uebersicht, `POST /api/datagen`) laeuft als Auftrag im Hintergrund, Schritt fuer
Schritt (eigene Gradle-Aufrufe, weil `syncGenerated263` die 26.3-Ausgabe mit dem frischen
`src/main/generated` vergleicht):

1. `gradlew runDatagen` (26.2, danach `generateWiki`)
2. `gradlew :mc26_3:fabric:runDatagen` (+ `syncGenerated263`)
3. `gradlew :mc1_21_11:fabric:runDatagen`
4. auf Wunsch `gradlew -Pmc264=true :mc26_4:fabric:runDatagen`
5. `python wiki/generate.py`

Danach liest die Zentrale neu ein, prueft (checkBalance) und zeigt `git diff --stat`. Ein Schritt, der
scheitert, stoppt den Auftrag (Ausgabe sichtbar); abbrechen geht jederzeit.

**checkBalance** (`sbdev/check.py`, `gradlew checkBalance`, haengt an `check`) scheitert, wenn

1. ein gespeicherter und **angewendeter** Wert nicht mehr im Code steht (jemand hat die Zahl an der
   Zentrale vorbei geaendert) - Loesung: im Code zuruecksetzen oder den neuen Wert in der Zentrale
   speichern;
2. bei einem gespeicherten Wert eine andere Linie eine andere Zahl hat;
3. erzeugte Dateien nicht zu den Java-Zahlen passen: Inject-Tabellen (26.2, 1.21.11, 26.3 - Pools,
   Wuerfe, Gewichte, Mengen, Kern-Chancen), Verzauberungen und Rezepte (26.2, 1.21.11), Erze, Item-Export.

Hinweise (kein Fehler): geplante, noch nicht angewendete Werte; verwaiste Werte. `-PskipWiki` ueberspringt
auch checkBalance (nur wo Python fehlt).

## 5. Warum keine erzeugte `Balance.java`

Der Plan aus Phase 1 sah eine erzeugte Klasse `Balance.java` vor, auf die der Mod-Code verweist. Umgesetzt
ist stattdessen das **chirurgische Schreiben der Literale an ihrer Stelle**, weil

* der Mod-Code unveraendert bleibt (kein Umbau hunderter Literale in zwei Linien, keine Merge-Konflikte mit
  parallelen Laeufen, die dieselben Dateien bearbeiten; das Verhalten bei den heutigen Werten ist
  trivial gleich);
* die Zahl im Code die eine Quelle der Wahrheit bleibt - wer den Code liest, sieht den wirksamen Wert,
  mit seinem Kommentar daneben;
* die Leser jede Stelle schon exakt kennen (Datei, Zeichenbereich) und die Vorab-Pruefung jede fremde
  Aenderung erkennt;
* checkBalance dieselbe Sicherheit gibt wie eine Pruefung von `Balance.java`: Ablage gegen Code, Linien
  gegeneinander, erzeugte Dateien gegen Code.

Datapack-Werte (Loot-Inject-Tabellen) schreibt die Zentrale nicht direkt: sie entstehen aus
`ModLootTableModifications` (die Spieltests vergleichen die geladenen Tabellen mit dieser Klasse); ein
Datapack eines Servers kann sie weiter ueberschreiben.

## 6. Was (noch) nicht in der Mod wirkt

Die Seite **Uebergabe** (`GET /api/handover`, frueher `/api/phase2`) listet jeden gespeicherten Plan, der
nicht in der Mod steht, mit Grund, Datei und Zeile:

| Was | warum |
|---|---|
| geplante Quellen (`source:*`) | eine neue Quelle ist ein neuer Pool/Eintrag im Code (Struktur, nicht Zahl) - von Hand oder per Lauf |
| abgeschaltete Quellen (`sourceoff:*`) | Planung; eine vorhandene Beute-Quelle schaltest du ab, indem du ihr Gewicht auf 0 setzt (wirkt) |
| Werte ohne eigene Zahl im Code | Rezeptmengen aus Vanilla-Mustern, berechnete Item-Werte, fehlende Felder in Handelsdateien |
| Zwillinge, die abweichen | die Linie bleibt, wie sie ist (am Wert und im Speichern-Dialog sichtbar) |
| Zutaten und Muster der Rezepte, Wirkungen ohne eigene Zahl | Struktur statt Zahl |

## 7. Die Rechner

* **Zeit bis 1-6 Stueck** je Item und Quelle (Mittel, Median, 90 %), gezielt je Quelle, alle Quellen
  gezielt zusammen und normales Spiel. Modell aus `docs/KERNE-SELTENHEIT.md` Abschnitt 2, verallgemeinert:
  jede Quelle ist ein Strom von Ereignissen (Oeffnungen, Haendlerbesuche, Toetungen, Abbau) mit der exakt
  aus den Pools berechneten Stueckzahl je Ereignis; zusammen ein zusammengesetzter Poisson-Prozess, Mittel
  in geschlossener Form, Median per Bisektion - deterministisch, keine Simulation. Die Tests pruefen die
  Zahlen aus KERNE-SELTENHEIT 5.3 (z. B. Enderitkern 38,1 h Mittel, Median 26,4 / ... / 216 h).
* **Angebotschance im Handel**: Ziehen ohne Zuruecklegen mit Angebots-Chancen, exakt ueber ein Integral
  (Gauss-Legendre). Ergibt die ~10,1 / 4,9 / 2,4 / 1,0 % der Kerne beim fahrenden Haendler.
* **Dorfbewohner**: passenden Dorfbewohner finden (ausgebildete je Stunde x Angebotschance) plus
  Auffuellungen. **Rezepte**: Zeit fuer die verfolgten Zutaten (informativ, nicht "beste Quelle").
* **Rueckwaerts**: Zielzeit (oder Zeitalter x Zielanteil 0,85, Regel aus KERNE-SELTENHEIT 5.3) -> welcher
  Wert eines Stellwerts (Kern-Chance, Gewicht, Angebots-Chance, Nutzungen, geplante Quelle) sie erreicht;
  "Als Entwurf uebernehmen" setzt ihn.
* **Quellen planen/abschalten** je Item (Struktur, Mob, Haendler, Block, eigene) - wirkt sofort in den
  Rechnern, landet als Planung in der Uebergabe.
* **Annahmen** (Seite *Annahmen*): Oeffnungen je Stunde je Struktur und Behaelter (gezielt, normal
  optional), Faktor normales Spiel (0,22), Haendlerbesuche, Dorfbewohner, Toetungen, Abbau, Zeitalter.
  Standards aus KERNE-SELTENHEIT Abschnitt 2, der Rest geschaetzt und so beschrieben.

## 8. Aufbau

```
tools/devserver/serve.py        HTTP-Server (ThreadingHTTPServer, nur 127.0.0.1), Routen, Start, --check
tools/devserver/sbdev/          extract + ex_* (Leser), ex_javadata (Java-Quelle hinter erzeugten Dateien,
                                Materialien, 1.21.11-Handel, Trank-Pads), sites (Linien und Zwillinge),
                                values (Wertdatensatz, Pruefung), store (Ablage), service (Vorschau/Speichern/
                                Rollback/Rechner/Datagen), apply (alle Stellen schreiben), javaedit (Java-
                                Literale), jsonedit (JSON), check (checkBalance), jobs (Datagen-Auftrag),
                                model (Rechner), params (Annahmen), docs (Markdown)
tools/devserver/static/         Oberflaeche (index.html, app.css, app.js - ohne Framework)
tools/devserver/data/           Vanilla-Handelspools (aus dem Client-Jar, versioniert)
tools/devserver/tests/          unittest (fixtures/: PotionPadRules aus Lauf AA fuer den Leser-Test)
```

API (JSON): `GET /api/state`, `/api/check`, `/api/datagen` (Stand des Auftrags), `/api/handover`
(= `/api/phase2`), `/api/pending-apply`, `/api/version/<n>`, `/api/docs/<name>`; `POST /api/preview`,
`/api/save`, `/api/rollback/preview`, `/api/rollback`, `/api/apply-planned`, `/api/datagen` (starten,
`{"force", "include264"}`), `/api/datagen/cancel`, `/api/calc`, `/api/reverse`, `/api/overview`,
`/api/reload`. Schreibende Anfragen brauchen den Header `X-Balance-Client: 1` (Schutz gegen fremde
Webseiten) und JSON.
