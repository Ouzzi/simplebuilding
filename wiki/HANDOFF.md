# Übergabe: SimpleBuilding Wiki

Stand 2026-09-02. Diese Datei ist für die Session gedacht, die das Wiki fertigstellt.
Lies sie zuerst, dann `wiki/README.md`, dann die Punkte unter „Feedback" – das ist die Arbeit.

## Was schon steht

| Baustein | Datei | Zustand |
|---|---|---|
| Generator | `wiki/generate.py` | fertig; liest Items, Blöcke, Rezepte, Loot, Handel, Verzauberungen, Tags, Config aus der Mod. `--check` und `--strict` vorhanden. |
| Build-Haken | `build.gradle` → Task `checkWiki`, hängt an `check` | fertig; schlägt fehl, wenn `wiki/data` veraltet ist oder etwas ohne Prosa ist |
| Daten | `wiki/data/simplebuilding.json` + `.js` | generiert, nie von Hand anfassen |
| Texturen | `wiki/assets/textures/**` | vom Generator aus dem Mod kopiert, damit `wiki/` in sich geschlossen ist |
| App | `wiki/index.html` | eine Datei, Vanilla-JS, kein Build. Von einem Agenten gebaut und im Browser durchgeklickt. **Oberfläche derzeit Deutsch – soll Englisch werden, s. u.** |
| Prosa | `wiki/manual.json` | **49 Notizen + 26 Kapitel (inkl. `welcome`, `getting_started`), alle auf Deutsch**, jede Behauptung von einem zweiten Agenten gegen den Code geprüft (111 unbelegte Behauptungen gestrichen). Rohergebnis inkl. gestrichener Behauptungen und App-Bericht: `wiki/manual.draft.json`. |
| Anleitung | `wiki/README.md` | Pflegeprozess, Schema von `manual.json` |

`python wiki/generate.py --check` ist **grün**: Daten aktuell, alles mit eigenem Verhalten hat Prosa.

App-Bericht des Agenten (vollständig in `manual.draft.json` → `appReport`): alle neun Bereiche,
Hash-Routing, Suche mit `/`-Kürzel, Rezeptgitter, Handel mit Pool-Prozenten, hell/dunkel, responsiv;
im Browser durchgeklickt, null Konsolenfehler. Der Nutzer hat die Seite bereits per Doppelklick
geöffnet und Feedback zum Inhalt gegeben – `file://` funktioniert also in der Praxis; die App
verarbeitet sowohl die alten `../src/…`- als auch die neuen `assets/…`-Texturpfade.

## Feedback des Nutzers – das ist die To-do-Liste

Wörtlich aus dem Chat, mit Einordnung und Umsetzungsvorschlag.

### 1. „bei den items fehlt durability falls die items welche haben"

Haltbarkeit (und ebenso Angriffsschaden, Abbaugeschwindigkeit, Verzauberbarkeit, Stapelgröße,
Zauberstab-Durchmesser) steht nirgends in den Wiki-Daten, weil sie nicht in JSON-Dateien liegt,
sondern in Java-Konstanten (`ModItems.java`: `DURABILITY_*`, `ENCHANTABILITY_*`,
`BUILDING_WAND_SQUARE_*` in `BuildingWandItem.java`).

### 2. „bitte solche variablen so einrichten dass sie zentral geändert werden können und sowohl im mod als auch in der seite automatisch angepasst werden"

**Empfohlene Lösung – Datagen-Export, nicht Java-Parsen:** Ein Datagen-Provider
(`WikiDataProvider`, neben den bestehenden in `src/main/java/com/simplebuilding/datagen/`),
der beim gewohnten `gradlew runDatagen` die *tatsächlichen* Item-Eigenschaften aus der
Registry nach `src/main/generated/wiki/items.json` schreibt: `maxDamage`, `maxStackSize`,
Angriffsschaden/-geschwindigkeit aus den Attribut-Modifiern, Verzauberbarkeit, bei
Zauberstäben `getWandSquareDiameter()`, bei Bündeln die Kapazitätsstufe. Der Generator liest
diese Datei ein und hängt sie an jedes Item.

Warum so: Die Registry *ist* die zentrale Definition. Wer eine Konstante in `ModItems.java`
ändert, ändert Mod und Wiki mit einem Datagen-Lauf – ohne Parser, der bei jeder
Umformatierung bricht, und ohne zweite Wahrheit. Beide Minecraft-Linien haben Datagen.

Zweitbeste Lösung, falls schnell nötig: `generate.py` parst die `DURABILITY_*`-Konstanten
und die `registerSledgehammer("…", DURABILITY_X * 2, …)`-Aufrufe per Regex – so, wie es
heute schon die Config-Klasse parst.

### 3. „bei den blöcken fehlt die 3d textur, also eine 3d ansicht aus einem winkel"

Vorschlag: isometrischer Würfel rein clientseitig. Der Generator liefert pro Block statt
einer Textur ein Objekt `{top, side, front}` aus dem Blockmodell (`models/block/<id>.json`:
`top`/`side`/`front`/`all`/`end`), die App zeichnet daraus per CSS-3D-Transform (drei
`<img>` mit `image-rendering: pixelated`, `transform: rotateX(-30deg) rotateY(45deg)` o. ä.)
einen Würfel. Für Blöcke mit komplexem Modell (Kolben, Trichter, Öfen mit Vorderseite)
reicht Vorderseite + Oberseite + Seite; bei Nicht-Würfeln (Kolbenkopf) Rückfall auf die
flache Textur. Kein Build, kein WebGL nötig.

### 4. „die sprache hab ich noch nicht gefunden, also main soll englisch sein und de als dropdown übersetzung"

Zwei Teile:

**a) Oberfläche der App** (`index.html`): Beschriftungen auf Englisch, oben ein Dropdown
`EN | DE`, Wahl in `localStorage` merken. Item-Namen gibt es bereits in beiden Sprachen
(`name.en_us` / `name.de_de` in den Daten).

**b) Prosa:** `manual.json` von flach auf zweisprachig umstellen:
```json
"magnet": {
  "en": { "summary": "…", "details": [], "controls": [], "tiers": [], "caveats": [] },
  "de": { "summary": "…", "details": [], "controls": [], "tiers": [], "caveats": [] },
  "sources": ["…"]
}
```
Kapitel (`features`) genauso mit `en`/`de` für `title`, `summary`, `details`.
Die deutsche Fassung ist fertig und geprüft; die englische muss erzeugt werden – am
saubersten mit demselben Muster wie die deutsche: je Familie ein Agent, der aus dem
**Code** schreibt (nicht aus dem Deutschen übersetzt), und ein Gegenprüfer. Die deutsche
Fassung dient dann als Vergleich: gleiche Behauptungen, gleiche Zahlen. Der Generator muss
`note_for()` und die `--check`-Prüfung auf „beide Sprachen vorhanden" erweitern.

### 5. „viele dinge sind noch nicht ausgefüllt, verzauberungen bspw. stehen da mit ohne wirkung und items ohne beschreibung"

Zwei Ursachen, beide inzwischen behoben bzw. erklärt:

- **„ohne Wirkung" an fast allen Verzauberungen war ein Generator-Fehler.** `hasEffect`
  wurde nur aus den JSON-`effects` abgeleitet – die meisten Verzauberungen dieser Mod wirken
  aber im Java-Code (Vein Miner, Radius, Versatility …). Behoben: der Generator prüft jetzt
  zusätzlich, ob Spielcode die Verzauberung liest, und liefert `implementedIn` =
  `data | code | both | none`. **Wirklich ohne Wirkung sind nur `cover` und `bridge`** –
  das ist ein echter Befund (nie implementiert, siehe Testsuite
  `coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown`), und das Badge soll dort bleiben.
- **Items ohne Beschreibung:** `manual.json` war zum Zeitpunkt des Anschauens noch leer,
  weil der Workflow lief. Inzwischen befüllt (49 Notizen). Reine Baublöcke haben absichtlich
  keine Prosa – Rezept und Drop beschreiben sie. Falls der Nutzer auch dafür Text will:
  `generate.py`, Funktion `behavioural_ids`, entscheidet, was Prosa *fordert*; Prosa für
  Baublöcke kann man trotzdem jederzeit in `manual.json` ergänzen.

### 6. „vanilla texturen fehlen"

Zutaten wie `minecraft:stick` erscheinen im Rezeptgitter als Textkachel, weil Vanilla-
Texturen nicht im Repo liegen. Sie stecken im Client-Jar im Gradle-Cache
(`~/.gradle/caches/fabric-loom/26.2/minecraft-client.jar`, Pfad
`assets/minecraft/textures/{item,block}/*.png`). Vorschlag: `generate.py` zieht **nur die
in Rezepten, Handel und Tags referenzierten** Vanilla-Ids aus dem Jar nach
`wiki/assets/textures/minecraft/…`. Hinweis zur Einordnung: Mojang-Assets gehören nicht ins
Repo – das Verzeichnis in `.gitignore` aufnehmen und im README erklären, dass ein
`generate.py`-Lauf sie lokal beschafft. Trade-off: ein frisch geklontes Wiki zeigt sie erst
nach einem Lauf.

## Bekannte Auffälligkeiten in der Mod (aus der Wiki-Erstellung)

Nicht Teil des Wikis, aber beim Durchleuchten des Codes aufgefallen; der Nutzer
entscheidet:

- `cover` und `bridge`: Verzauberungen ohne jede Wirkung (s. o.).
- Öfen/Räucheröfen/Schmelzöfen: Feld `speedMultiplier` ist deklariert (`@SuppressWarnings("unused")`),
  wird aber nie gelesen; die Beschleunigung kommt aus `extraTicks`. Toter Code, kein Fehler.
- Sprachdatei `en_us.json` enthält vier Item-Schlüssel ohne Textur/Modell: `nithilith_ore`
  (vermutlich Tippfehler für `nihilith_ore`), `polished_endstone` (vs. `polished_end_stone`),
  `purpur_lapis_checker`, `sledgehammer`. Entweder verwaiste Schlüssel oder fehlende Items.
- Rotator hat keine `enchantable(...)`-Einstellung, anders als die übrigen Werkzeuge.

## Reihenfolge, die ich vorschlage

1. Branch `feature/wiki` auschecken (dort liegt alles), `python wiki/generate.py --check` –
   muss grün sein.
2. Punkt 5 prüfen (Badge nur noch bei cover/bridge), Punkt 4a (englische Oberfläche +
   Dropdown) – kleine, sichtbare Fortschritte zuerst.
3. Punkt 2 (Datagen-Export) – dann fällt Punkt 1 automatisch mit ab.
4. Punkt 4b (englische Prosa per Agenten, gegengeprüft), `manual.json` zweisprachig,
   `--check` auf beide Sprachen erweitern.
5. Punkt 3 (3D-Würfel) und Punkt 6 (Vanilla-Texturen).
6. `gradlew check` grün, Merge auf `master` ohne PR (so will es der Nutzer).

## Arbeitsweise, die der Nutzer erwartet

- Jede Behauptung im Wiki muss im Code stehen. Nicht raten. Gegenprüfen.
- Keine Fehlalarme: bevor etwas als Mod-Fehler gemeldet wird, Testumgebung ausschließen
  (Mock-Spieler ist immer Kreativmodus; Fabric registriert Gametests nur über den Entrypoint).
- Bei langen Läufen nicht blind warten: Prozesse und Protokolle prüfen, hängende Java-
  Prozesse aufräumen, bevor ein Client-Test wiederholt wird.
- Merge direkt auf `master`, kein PR.
- Er testet selbst im Spiel und schildert Auffälligkeiten – die kommen als nächstes.
