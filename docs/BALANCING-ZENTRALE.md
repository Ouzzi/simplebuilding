# Balancing-Zentrale (Stand 2026-09-29, Phase 1)

Lokaler Entwicklungs-Server fuer **alle** Balance-Werte der Mod: Beute, Handel, Mob-/Block-Drops, Erze,
Werkzeug- und Ruestungswerte, Verzauberungen, Rezepte, Code-Konstanten (Ladungen, Abklingzeiten,
Reichweiten, Tempo), Config-Standards - mit Versionen, Rueckgaengig, Rechnern fuer die Beschaffungszeit
und den Entwickler-Dokumenten. Dieses Dokument beschreibt Bedienung, Ablage und den **Plan fuer Phase 2**
(wie gespeicherte Werte in die Mod kommen).

## 1. Starten

```
python tools/devserver/serve.py
```

Oeffnet den Browser auf `http://127.0.0.1:8770/` (nur lokal erreichbar, keine Anmeldung). Optionen:
`--no-browser`, `--port 8771`, `--store <ordner>` (andere Ablage, z. B. zum Ausprobieren),
`--refresh-vanilla` (Vanilla-Handelspools und -Namen neu aus dem Client-Jar lesen). In Claude Code gibt
es den Eintrag `balancing-zentrale` in `.claude/launch.json`. Nur Python-Standardbibliothek.

Tests: `python -m unittest discover -s tools/devserver/tests` (Auslesen, Rechner, Ablage inkl.
gleichzeitiger Schreibzugriffe und Absturz-Wiederherstellung, Pruefung ungueltiger Werte, Schreiben in
Handelsdateien, HTTP-API).

## 2. Was die Zentrale liest (Quellen der Wahrheit, nur lesend)

| Bereich | Quelle | Leser |
|---|---|---|
| Beute (Truhen, Tresore, Angeln, geladener Creeper) | `common/src/shared/java/.../loot/ModLootTableModifications.java` (jedes Zahlenliteral mit Zeile und Zeichenbereich) | `sbdev/ex_loot.py` |
| Kern-Chancen | Konstanten `*_CORE_CHANCE` derselben Datei (+ Config-Faktor `worldGen.buildingCoreLootChanceMultiplier`) | `sbdev/ex_constants.py` |
| Handel | `src/main/resources/data/simplebuilding/villager_trade/**.json` + Tags `data/minecraft/tags/villager_trade/**` + Vanilla-Pools (`tools/devserver/data/vanilla-trade-pools-26.2.json`, aus dem Client-Jar) | `sbdev/ex_trades.py`, `sbdev/vanilla.py` |
| Config-Standards | `SimplebuildingConfig.java`, `TweaksConfig.java`, `ConfigOptions.java` (Grenzen aus `@BoundedDiscrete`, `Math.max(n, feld)`, `nonNegative(...)`), Namen/Tooltips aus `lang/de_de.json` | `sbdev/ex_config.py` |
| Werkzeug-/Ruestungswerte | `src/main/generated/wiki/items.json` (Export von WikiDataProvider) | `sbdev/ex_data.py` |
| Code-Konstanten | jede `static final int/float/double/long` im gemeinsamen Code, deren Name nach Balance klingt | `sbdev/ex_constants.py` |
| Rezepte, Verzauberungen, Erz-Generierung, Block-Drops | `src/main/generated/data/simplebuilding/{recipe,enchantment,worldgen,loot_table/blocks}` | `sbdev/ex_data.py` |
| Namen, Icons | Sprachdateien der Mod, Vanilla-Sprachdateien (Gradle-Cache, nicht versioniert), Icons aus `wiki/assets` | `sbdev/ex_data.py`, `sbdev/vanilla.py` |
| Doku | `docs/*.md` live gerendert (ohne `WIKI-HOSTING.md` - Wiki-Themen bleiben im Wiki) | `sbdev/docs.py` |

Was **nicht** auslesbar ist, steht auf der Seite *Auslese-Bericht* (mit Datei, Zeile und Grund), z. B.
`EnderitePistonBlock.ENDERITE_MAX_DURABILITY = ninthOf(...)` (Methodenaufruf), die 1.21.11-Linie
(Handel in Java), Wirkungen von Verzauberungen, die nur im Code stecken, und alle Spieler-Annahmen
(Kisten je Struktur, Haendlerbesuche ...). Neue, unbekannte Bausteine in der Loot-Datei werden ein
Eintrag im Bericht - nie eine stille Luecke.

## 3. Bearbeiten, Speichern, Versionen

* Jedes Feld ist ein Entwurf, sobald es sich aendert: der **alte Wert steht rot durchgestrichen daneben**,
  die Leiste unten zaehlt die ungespeicherten Aenderungen. Entwuerfe liegen im Browser (localStorage) und
  ueberleben ein Neuladen.
* **Speichern** zeigt immer erst eine Zusammenfassung (alt -> neu, je Bereich, mit Datei und Wirkung) und
  speichert erst nach Bestaetigung. Ungueltiges (keine Zahl, ausserhalb der Grenzen, nur lesbare
  Werte, unbekannte Ids) blockiert mit klarer Meldung; Werte, die jemand anderes inzwischen geaendert hat,
  erscheinen als Konflikt statt still ueberschrieben zu werden.
* Jede Bestaetigung ist eine neue **Version** `vN`. Keine Version wird je geloescht oder ueberschrieben.
  **Zuruecksetzen** auf eine alte Version legt eine neue Version mit deren Inhalt an - mit derselben
  Vorschau und Bestaetigung.
* Plan verwerfen (Symbol neben einem geplanten Wert) setzt einen Wert als Entwurf auf den Mod-Wert zurueck.

### Ablage

```
balance/balance.json              aktueller Stand (atomar ersetzt)
balance/versions/v0001.json       jede Version, unveraenderlich (Aenderungsliste + kompletter Stand)
balance/versions/v0001.applied.json  welche Mod-Dateien diese Version beschrieben hat (falls welche)
balance/applied-log.jsonl         spaeteres "Jetzt anwenden" geplanter Handelswerte
```

Eintrag je Wert: `{"value": Plan, "mod": Mod-Wert beim Speichern, "origin": Mod-Wert, als der Wert zum
ersten Mal geplant wurde}`. Nur geplante Werte stehen in der Ablage; alles andere folgt der Mod.
`balance/` gehoert ins Repo (die Sperrdatei `balance/.lock` nicht).

Schutz gegen Verlust: Commit-Punkt ist die Versionsdatei (exklusiv angelegt), danach wird
`balance.json` atomar ersetzt; nach einem Absturz dazwischen stellt das naechste Laden den Stand aus der
Version her. Eine kaputte `balance.json` wird als `balance.json.corrupt-<zeit>` beiseite gelegt, nie
ueberschrieben. Eine Sperrdatei verhindert gleichzeitiges Schreiben zweier Prozesse, `baseVersion`
verhindert das Ueberschreiben fremder Speicherungen.

### Status eines Werts

| Anzeige | Bedeutung |
|---|---|
| **wirkt in Mod** | Speichern schreibt den Wert direkt in die Mod-Datei (Phase 1: Handel) |
| **Phase 2** | nur Planung; wirkt erst, wenn Phase 2 umgesetzt ist (Abschnitt 5) |
| **Rechner** | Annahme der Rechner; wirkt nie in der Mod |
| geplant | gespeicherter Plan weicht vom Mod-Wert ab |
| in Mod | gespeicherter Plan steht so in der Mod |
| Mod geaendert | der Mod-Wert hat sich seit dem Speichern bewegt (z. B. ein paralleler Lauf) |
| verwaist | den Wert gibt es in der Mod nicht mehr (Umbenennung?) - bleibt, bis er bewusst entfernt wird |

## 4. Was schon in Phase 1 wirkt: Handel

Handelswerte (Preis, zweiter Preis, Menge, Nutzungen, Haendler-Erfahrung, Rabattfaktor, Angebots-Chance,
Gewichte des Verzauberungs-Pools, zweite Verzauberung) schreibt Speichern **chirurgisch** in
`src/main/resources/data/simplebuilding/villager_trade/**.json` (`sbdev/jsonedit.py`): genau ein Token
wechselt, Formatierung und Zeilenenden bleiben, die Datei wird danach geprueft. Fehlt eine
Angebots-Chance, wird `merchant_predicate` (`minecraft:random_chance`) vor `max_uses` eingefuegt. Vorher
wird geprueft, dass in der Datei noch der eingelesene Wert steht - sonst wird nichts gespeichert
("Neu einlesen"). 26.2 und 26.3 teilen diese Dateien. **1.21.11** baut dieselben Angebote in Java
(`mc1_21_11/shared/.../trade/ModTradeDefinitions.java`) und wird nicht angepasst - das sagt auch jede
Handelszeile im Speichern-Dialog.

Im Speichern-Dialog laesst sich das Schreiben abschalten (Wert bleibt dann "geplant"); die Uebersicht
bietet spaeter "Jetzt anwenden".

## 5. Phase 2: gespeicherte Werte wirken in der Mod

Voraussetzung: die parallelen Laeufe M (Server-Config), N (Tabellen als Datapack, Loot als Tabellen),
P2 (Id-Umbenennungen) und R sind gemergt. Danach als Erstes die Zentrale starten: Id-Umbenennungen zeigen
sich als **verwaiste** Werte (Uebersicht), neue Datenpaket-Dateien als Luecken im Auslese-Bericht.

### 5.1 Grundsatz: eine erzeugte Balance-Quelle, geprueft wie das Wiki

Nicht jede Zahl einzeln im Java-Code suchen und ersetzen, sondern:

1. **Datengetriebenes direkt schreiben** (wie heute der Handel): alles, was nach Lauf N als JSON im
   Datenpaket liegt - Loot-Tabellen/Loot-Modifier, Handel, ggf. Tags - schreibt die Zentrale selbst
   (`apply = "mod"`). Dafuer bekommt `ex_loot.py` einen JSON-Leser neben dem Java-Leser und `apply.py`
   dieselbe Pruef- und Schreiblogik wie beim Handel.
2. **Code-Werte ueber eine erzeugte Klasse**: ein in Phase 2 zu bauender Befehl `python tools/devserver/serve.py --apply-phase2`
   schreibt aus `balance/balance.json` + den Mod-Werten eine Klasse
   `common/src/shared/java/com/simplebuilding/balance/Balance.java` (und die Kopie unter `mc1_21_11/shared/`)
   mit `public static final`-Konstanten. Der Mod-Code verweist auf `Balance.X` statt auf eigene Literale.
   Kompilierzeit-Konstanten: kein Laufzeit-Aufwand, gleich auf allen Loadern und Linien.
3. **Datagen liest dieselbe Klasse**: Rezepte, Verzauberungen, Erz-Generierung und der Item-Export
   entstehen in Java-Providern; sie nehmen ihre Zahlen aus `Balance`. Danach `gradlew runDatagen`.
4. **Pruefung**: ein Gradle-Task `checkBalance` (wie `checkWiki`) ruft
   `python tools/devserver/serve.py --check` (ebenfalls Phase 2) auf und scheitert, wenn `Balance.java` oder ein erzeugtes
   Datenpaket nicht zu `balance/balance.json` passt. So kann niemand eine Zahl im Code aendern, ohne dass
   die Zentrale es merkt - und umgekehrt.

### 5.2 Je Bereich

| Bereich | heute | Phase 2 | was variabel werden muss |
|---|---|---|---|
| Beute-Pools | Java-Builder in `ModLootTableModifications` | nach Lauf N: Loot als Datenpaket-JSON -> **direkt schreiben** (wie Handel). Vorher nicht anfassen (Merge-Konflikt mit N). | Gewichte, Wuerfe, Mengen; die Ids der Zentrale (`loot:<block>:p<n>:<eintrag>:<feld>`) auf JSON-Pfade umstellen |
| Kern-Chancen | Konstanten `*_CORE_CHANCE` | JSON (`binomial` p) nach N, sonst `Balance.IRON_CORE_CHANCE` ... | 7 Konstanten; Faktor `worldGen.buildingCoreLootChanceMultiplier` bleibt Config |
| Handel | JSON (wirkt schon) | 1.21.11: `ModTradeDefinitions` liest `Balance`-Werte oder wird aus den JSON-Dateien erzeugt | Preise, Nutzungen, XP, Rabatt, Angebots-Chance in `ModTradeDefinitions` |
| Config-Standards | Feld-Initialisierer | Initialisierer verweisen auf `Balance.CONFIG_...` | 76 Felder (nur die geplanten muessen, die Klasse kann alle tragen); Grenzen in `validate()` bleiben Code |
| Code-Konstanten | `static final` in Werkzeug-/Geraeteklassen | Literal -> `Balance.<KLASSE>_<NAME>`; abgeleitete Ausdruecke (`190 * BASE_DURABILITY_MULTIPLIER`) bleiben Ausdruecke ueber Balance-Werte | u. a. `ModItems` (Haltbarkeit/Abklingzeit/Verzauberbarkeit je Stufe), `SledgehammerItem`, `BuildingWandItem`, `RotatorItem`, `LaserBeam`, `EchoCompassItem`, `OreDetectorItem`, `MagnetItem`, `TrimEffectUtil`, `FurnaceTierPerks`, `BlueprintTiers`, `SpawnTeleporterBlockEntity`, `LaunchpadBlockEntity` |
| Werkzeug-/Ruestungswerte | Export `items.json` (aus Java) | keine eigene Quelle: jeder Export-Wert wird einer Balance-Konstante zugeordnet (Zuordnungstabelle in `ex_data.py`); Aenderung = Aenderung der Konstante, dann `runDatagen` | Zuordnung Item-Wert -> Konstante (z. B. `copper_building_wand:durability` -> `ModItems.DURABILITY_...`) |
| Rezepte | Datagen (`ModRecipeProvider`) | Mengen/Garzeiten aus `Balance`; Zutaten-Aenderungen bleiben Notizen/Code | Ergebnis-Mengen, Garzeiten, XP der geplanten Rezepte |
| Verzauberungen | Datagen (`ModEnchantments`) | Gewicht, Stufen, Kosten, Wirkungswerte aus `Balance` | die geplanten Felder |
| Erz-Generierung | Datagen (`ModWorldGen`) | Adergroesse, Adern je Chunk, Hoehen aus `Balance` | 8 Werte |
| Geplante Quellen (`source:*`) | nur Planung | nach N: als Loot-Eintrag in die passende Tabelle schreiben (Struktur-Quelle = Eintrag mit Chance/Menge; Mob = Mob-Loot-Tabelle; Haendler = neue `villager_trade`-Datei + Tag) | - |
| Abgeschaltete Quellen (`sourceoff:*`) | nur Planung | Eintrag entfernen bzw. Gewicht 0 | - |
| Annahmen (`param:*`) | Rechner | wirken nie in der Mod | - |

Die Seite **Phase 2: Uebergabe** (und `GET /api/phase2` als JSON) listet jeden geplanten, noch nicht
wirksamen Wert mit Datei, Zeile, Zeichenbereich (Java) bzw. JSON-Pfad - die Arbeitsliste fuer Phase 2
oder fuer einen Lauf, der die Werte von Hand uebernimmt.

### 5.3 Reihenfolge fuer Phase 2

1. Nach dem Merge von M/N/P2/R: Zentrale starten, verwaiste Werte umziehen (neue Id setzen, alte
   entfernen - beides im Speichern-Dialog sichtbar), Auslese-Bericht leeren (neue Dateiformate lesen).
2. Loot-JSON-Leser und -Schreiber (Beute, Kern-Chancen, geplante Quellen) - `apply = "mod"`.
3. `Balance.java`-Erzeuger + Umstellung des Codes Bereich fuer Bereich (Konstanten, Config-Standards,
   Datagen-Provider), jeweils mit Test, dass der Standardwert unveraendert bleibt.
4. `checkBalance` in das Server-Gate aufnehmen.
5. 1.21.11: `ModTradeDefinitions` und die Kopien der Loot-/Config-Klassen aus derselben Quelle bedienen.

## 6. Die Rechner

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
  Rechnern, landet als Planung in der Phase-2-Liste.
* **Annahmen** (Seite *Annahmen*): Oeffnungen je Stunde je Struktur und Behaelter (gezielt, normal
  optional), Faktor normales Spiel (0,22), Haendlerbesuche, Dorfbewohner, Toetungen, Abbau, Zeitalter.
  Standards aus KERNE-SELTENHEIT Abschnitt 2, der Rest geschaetzt und so beschrieben.

## 7. Aufbau

```
tools/devserver/serve.py        HTTP-Server (ThreadingHTTPServer, nur 127.0.0.1), Routen, Start
tools/devserver/sbdev/          extract + ex_* (Leser), values (Wertdatensatz, Pruefung), store (Ablage),
                                service (Vorschau/Speichern/Rollback/Rechner), model (Rechner), apply +
                                jsonedit (Schreiben in Handelsdateien), params (Annahmen), docs (Markdown)
tools/devserver/static/         Oberflaeche (index.html, app.css, app.js - ohne Framework)
tools/devserver/data/           Vanilla-Handelspools (aus dem Client-Jar, versioniert)
tools/devserver/tests/          unittest
```

API (JSON): `GET /api/state`, `POST /api/preview`, `/api/save`, `/api/rollback/preview`, `/api/rollback`,
`/api/calc`, `/api/reverse`, `/api/overview`, `/api/reload`, `/api/apply-planned`, `GET /api/version/<n>`,
`/api/phase2`, `/api/docs/<name>`. Schreibende Anfragen brauchen den Header `X-Balance-Client: 1`
(Schutz gegen fremde Webseiten) und JSON.
