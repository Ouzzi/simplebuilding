# Audit simplequestionary (a-audsb31e1, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Umfang des Mods: 14 Java-Dateien /
2,1k Zeilen (shared + fabric, ohne Tests/Forge/NeoForge), Tests 0,5k Zeilen; dazu ~4 000 Zeilen Textdaten (`questionary/*.json`).
Der Mod hat **keinen Netzwerkpfad vom Client zum Server** (nur ein S2C-Paket) und keinen BlockEntity: das Risiko liegt im Menue (ein Slot)
und in der Datenaufbereitung. Zusatzeingabe BEW39 (`/root/sb-tools-a/bewertung39/simplequestionary.json`) ist unten eingeordnet.

## Vollstaendig gelesen
- Menue/Block: `QuestionaryMenu` (Slot, `quickMoveStack`, `removed`, `stillValid`, `slotsChanged`), `QuestionaryTableBlock`, `QuestionaryRegistry`,
  `QuestionaryConfig`, `SimpleQuestionary`.
- Netzwerk: `net/FactsPayload` (einziges Paket, S2C, Listen auf 9 begrenzt), Registrierung/`toClient` in `QuestionaryFabric`, Empfaenger in `QuestionaryFabricClient`.
- Daten: `QuestionaryFacts` (Rezept-Scan + Cache), `QuestionaryData` (Laden, Familien, Fallback, Platzhalter), `QuestionarySheet`.
- Tests/Adapter: `fabric/.../ModuleGameTest`, `QuestionaryTests` (Katalog, `menuOpens`, `slotTakesItem`, `dataLoaded`).
- Daten/Metadaten: `data/**` (Rezept, Loot-Tabelle, Axt-Tag), `fabric.mod.json`, `modules.json` (requires `simplelib` stimmt), `tools/check_data.py`.
- Nicht gelesen: `client/QuestionaryScreen` (689 Z., nur per grep: kein Paket, keine Dateizugriffe, kein `Pattern.compile`), `discover/QuestionaryDiscover`
  (generiert), `clienttest`, Inhalt der Textdateien (nur Rechtschreibung der englischen Texte per grep), Forge/NeoForge.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk-Payloads | Keine C2S-Payload. S2C `FactsPayload` (Item, Zutaten ≤ 9, Station, 2 Zahlen) nur bei geoeffnetem Menue des Spielers (`slotsChanged` → `ServerPlayer`), Client setzt nur ein `volatile`-Feld. |
| C2 Server-Autoritaet / Reichweite | `stillValid` = Vanillas `stillValid(access, player, TABLE)` (Block + 8 Bloecke); Rezeptfakten rechnet der Server. Client-Klassen nur in `client/` (per grep). Abgeschalteter Mod (`enabled=false`): `useWithoutItem` gibt `PASS`, Block bleibt registriert. |
| C3 Dupes / Item-Verlust | Slot hat `getMaxStackSize() = 1`; Schnellverschieben: Item-Slot → Inventar (`moveItemStackTo` rueckwaerts, Rueckgabe EMPTY bei vollem Inventar), Inventar → Slot nur ein Stueck und nur wenn leer (`return EMPTY` sonst) → die Vanilla-Schleife der Umschalt-Klick-Wiederholung endet nach einem Durchlauf. `removed()` ruft `clearContainer` ueber den Zugriff (Block-Abbau, Tod und Trennen geben das Item zurueck bzw. werfen es ab; Vanilla-Verhalten wie Verzauberungstisch). Doppelklick-Sammeln, Zifferntasten und Ziehen laufen ueber Vanillas `Slot.getMaxStackSize`. Kein Dupe und kein Verlust ausser Server-Absturz bei offenem Menue (Vanilla). |
| C4 NBT/Komponenten | Kein BlockEntity, keine eigene Komponente. Textdaten: Gson-Parser, kaputte Dateien werden uebersprungen (Warnung), Familien-Regex bei `PatternSyntaxException` verworfen, Seiten ≤ 350 Zeichen (Test `problems`). |
| C5 Tick-Kosten | Kein Tick. Pro Slot-Aenderung ein Rezept-Scan (~1,5k Rezepte) – gecacht nach (Item, Komponenten-Patch), Cache ≤ 512 Eintraege und pro RecipeManager (P3-1). `animateTick` nur Client. |
| C6 Leaks | Statisch: `QuestionaryFacts.CACHE` (≤ 512, leert sich bei neuem RecipeManager), `FactsPayload.latest` (Client, ein Feld), `QuestionaryData.current`. Keine Spieler-/Level-Referenzen. |
| C7 Sicherheit / Rechte | Kein Befehl ausser dem SimpleLib-Config-Knoten (`/simplequestionary config`, Stufe 2, nur `autoFacts`). Keine Mixins. Block aendert nichts an der Welt. Regex aus Ressourcenpaketen (P3-2). |
| C8 Unabhaengigkeit | allein spielbar (Standalone-Ziel vorhanden); kein Import ausser SimpleLib/`framework.api` (`ConfigFiles`); Simple Building nur im Test ueber die ID `enderite_ingot` (`sbLoaded()`), Creative-Tab des Hosts nur ueber den Schluessel. |
| C9 Doku/Pflege | Kein TODO/FIXME; Lang EN/DE je 39 Schluessel, `check_data.py` gruen. 32 britische Schreibweisen in den englischen Vanilla-Texten (AGENTS §3.5) → gefixt. Keine Kern-Dubletten (`dup_pairs.txt`). |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplequestionary-P3-1 | P3 (Tick-Kosten) | `QuestionaryFacts.compute` (`QuestionaryFacts.java:47-55`) schluesselt den Cache mit `DataComponentPatch`: jedes Item mit eigenem Namen/Komponenten loest einen vollen Rezept-Scan aus (Ergebnis haengt nur vom Item ab, ausser bei Fabric-Komponenten-Zutaten). Ein Spieler mit vielen verschieden benannten Stuecken (Amboss) kann den 512er-Cache fuellen und so Scans erzwingen; Kreativ-Spieler mit > 512 Items leeren ihn bei jedem Wechsel. | Hinweis (Schluessel nur Item; Komponenten-Zutaten pruefen) |
| simplequestionary-P3-2 | P3 (Client) | `QuestionaryData.family` (`QuestionaryData.java:316`) kompiliert `regex:`-Muster aus Ressourcenpaketen; ein Server-Ressourcenpaket kann ein katastrophales Muster mitbringen, das beim Seitenaufbau (Abgleich gegen Item-Pfade) den Client haengen laesst. Nur Clients, die dem Server ein Paket erlauben. | Hinweis (Muster-Laenge begrenzen oder `Pattern` mit Zeitgrenze) |
| simplequestionary-P3-3 | P3 (Doku, AGENTS §3.5) | `assets/minecraft/questionary/en_us.json`: „colour/colours/coloured“ (28), „neighbours“ (3), „grey“ (1). | **gefixt** (color/colors/colored/neighbors/gray); `check_data.py` gruen |
| simplequestionary-P3-4 | P3 (BEW39 rot: Text-Abdeckung) | Kein Fehler im Code: `QuestionaryTests.coverage` meldet Luecken nur im Log (Simple Ores 198, Lights 75, Layers 28, Building Tools 27, Vault 14, Chisels 8, Foundry 6 Items ohne eigene Datei; Sandwiches 10/27, Fun 2/10); fehlende Seiten fallen auf die acht Fallback-Varianten je Item-Klasse zurueck (deterministischer Hash). | **nicht gebaut (BEW39 missing)** – Texte schreiben, Mindestquote je Mod im Coverage-Test: Besitzer-Antwort steht aus |
| simplequestionary-P3-5 | P3 (UI-Probe) | Keine Echt-Maus-Probe fuer den Buch-Bildschirm (Lesezeichen, Seitenpfeile, Umschalt-Klick); Klickwege nur im Smoke. Das Menue selbst ist Vanillas Container-Weg (keine eigenen Klick-Pakete). | **TODO** „Echt-Maus-Probe Buch-Bildschirm auf sb-test“ (Muster `modules/simplemaps/tools/maps_menu_probe.py`) |
| simplequestionary-P3-6 | P3 (Pflege, BEW39 orange) | Textur `questionary_table_gem` rot (texrate15), JEI-Infoseite „Item einlegen“ fehlt, Store-Seite mit Platzhalter-URLs (IG31-55). | Hinweis (nicht angefasst; Textur-Regel 15.10.) |

## Urteil
**shipbar** (0 P1, 0 P2, 6 P3 davon 1 gefixt, 1 TODO, 1 „nicht gebaut“). Sehr kleine Angriffsflaeche: kein Client-Paket, kein BlockEntity, ein Slot mit Stapelgroesse 1 und korrekter Rueckgabe beim Schliessen.

## Testergebnis
Lokal: `modules/simplequestionary/tools/check_data.py` gruen (nach der Rechtschreib-Korrektur der Textdaten, enthaelt die Struktur-Pruefung `tools/questionary/check_data.py`).
Kein Java geaendert, daher kein javac noetig. sbt: siehe `docs/ai/PLAN-A-AUDSB31E-2026-11-12.md`.
Nicht getestet: Client-Bildschirm, Forge/NeoForge, Mehrspieler-Verhalten bei gleichzeitig offenem Menue (Vanilla-Container), Lauf der bestehenden Gametests (`slotTakesItem`, `menuOpens`).
