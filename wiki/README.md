# SimpleBuilding Wiki

Eine leichtgewichtige, offline lauffähige Dokumentation der ganzen Mod: jedes Item, jeder
Block, jedes Rezept, jede Loot-Tabelle, jeder Handel, jede Verzauberung, jeder Tag und
jede Konfigurationsoption – dazu Fließtext, der erklärt, was die Dinge tun.

## Öffnen

Doppelklick auf `wiki/index.html`. Kein Server, kein Build, kein Internet nötig.

Wer lieber über einen Server geht (z. B. für Hash-Links in anderen Browsern):

```bash
cd wiki && python -m http.server 8080
```

und dann `http://localhost:8080/` öffnen.

## Wie die Doku aktuell bleibt

Das ist der Kern des Aufbaus, deshalb ausführlich.

**Nichts in `wiki/data/` ist von Hand geschrieben.** `generate.py` liest die Mod selbst:

| Bereich | Quelle im Repo |
|---|---|
| Items, Blöcke, Namen | `src/main/resources/assets/simplebuilding/lang/*.json` |
| Texturen | `src/main/generated/assets/simplebuilding/models/**` → `textures/**` |
| 3D-Ansicht der Blöcke | dieselben Blockmodelle: Ober-, Seiten- und Vorderseite je Block |
| Vanilla-Texturen der Zutaten | `minecraft-client.jar` im Gradle-Cache (nicht im Repo, s. u.) |
| Rezepte | `src/main/generated/data/simplebuilding/recipe/**` |
| Loot-Tabellen | `src/main/generated/data/simplebuilding/loot_table/**` |
| Handel | `src/main/resources/data/simplebuilding/villager_trade/**` |
| Verzauberungen | `src/main/generated/data/simplebuilding/enchantment/*.json` |
| Tags | `.../tags/**` (generiert und Ressourcen) |
| Konfiguration | `common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java` |
| Haltbarkeit, Stapelgröße, Verzauberbarkeit, Angriffswerte, Zauberstab-Durchmesser, Meißel-Abklingzeit | `src/main/generated/wiki/items.json` – vom Datagen-Provider `WikiDataProvider` aus der **Item-Registry** geschrieben |
| Umwandlungen in der Welt (Maschinen-Aufwertung, Umformen, Diamantblock, Meißel, Schere auf Wolle, Besatzvorlage im Rahmen, Waschen im Kessel) | `src/main/generated/wiki/inworld.json` – vom Datagen-Provider über `InWorldTransformations` aus denselben Tabellen und Konstanten geschrieben, die das Spiel benutzt |
| Umwandlungen ohne Tabelle im Code (derzeit keine) und die Prosa je Art | `wiki/manual.json` → `inWorld` |
| Vanilla-Rezepte für den Rezeptbaum, je Minecraft-Linie | `minecraft-client.jar` der Linie im Gradle-Cache → `wiki/data/vanilla-<linie>.js` (committet, nur Rezeptdaten und Item-Tags) |
| Welche Items eigenes Verhalten haben | Registrierungen in `ModItems.java` / `ModBlocks.java` gegen die Klassen in `items/custom/` und `blocks/custom/` |

Ändert sich die Mod, ändert sich beim nächsten Lauf die Doku. Angestoßen wird dieser Lauf von
`gradlew runDatagen` (hängt `generateWiki` an), optional vom Pre-commit-Hook, und GitHub Actions
prüft vor jeder Veröffentlichung, dass nichts vergessen wurde – siehe
[`docs/WIKI-HOSTING.md`](../docs/WIKI-HOSTING.md).

### Zahlen, die in Java-Konstanten stehen

Haltbarkeit und Verwandtes liegen in keiner Datendatei, sondern in Konstanten in
`ModItems.java`. Statt diese Datei zu parsen – was bei jeder Umformatierung bräche –
schreibt der Datagen-Provider
`src/main/java/com/simplebuilding/datagen/WikiDataProvider.java` beim gewohnten
`gradlew runDatagen` die **tatsächlichen** Werte aus der Item-Registry nach
`src/main/generated/wiki/items.json`. Wer eine Konstante ändert, ändert Mod und
Wiki mit einem Datagen-Lauf.

Das ist nicht dasselbe wie die Konstante: Minecraft überschreibt manche Werte.
`Item.Properties.pickaxe(...)` setzt `enchantable(...)` auf den Wert des
ToolMaterial zurück, weshalb der Stein-Vorschlaghammer trotz
`ENCHANTABILITY_WOOD_STONE = 15` mit **5** verzaubert wird. Das Wiki zeigt, was
das Spiel benutzt.

Zwei Dinge dazu:

* `python wiki/generate.py --check` **schlägt fehl**, wenn die Datei fehlt, und
  nennt `gradlew runDatagen`. Ein normaler Lauf erzeugt das Wiki trotzdem, warnt
  aber und lässt die Eigenschaften weg.
* `src/main/generated` ist ein Ressourcenverzeichnis, deshalb schließt
  `processResources` in `build.gradle` `wiki/**` aus – der Export ist eine
  Bauzeit-Zutat und gehört nicht ins Mod-Jar.

### Umwandlung in der Welt

Die Kategorie „In der Welt“ (`#/inworld`) und der gleichnamige Abschnitt auf jeder
Item-Seite zeigen, was sich ohne Werkbank verwandelt – mit Werkzeug, Dauer, Schlägen,
Haltbarkeit und Mindeststufe. Die Zahlen und Tabellen schreibt der Datagen-Provider aus
`InWorldTransformations` (geteilter Code, beide Linien) nach
`src/main/generated/wiki/inworld.json`; die Spieltests `InWorldExportTests` halten den
Export gegen das Spiel. Derselbe Export speist das JEI-Plugin (`InWorldRecipeCatalog`). Was
keine Tabelle im Code hat, stuende in `manual.json` unter `inWorld.entries` mit Quellen (derzeit leer), die Prosa je Art unter `inWorld.kinds` (en + de, von
`--check` verlangt).

### Rezeptbaum und Vanilla-Rezepte

Rechtsklick auf eine Item-Kachel öffnet ein kleines Menü; „Rezeptbaum“ (`#/tree/<id>`)
zeigt alle Wege zu einem Item – Rezepte der Mod und aus Vanilla, Schmelzen, Umwandlungen
in der Welt, Drops, Handel – und klappt die Zutaten rekursiv auf. Varianten (beliebige
Bretter) werden zu einem Zweig zusammengefasst. Linksklick bleibt, wie er war.

Die Vanilla-Rezepte liest `generate.py` je Linie aus dem Client-Jar im Gradle-Cache und
schreibt sie nach `wiki/data/vanilla-26.2.js` bzw. `vanilla-1.21.11.js` – nur Rezeptdaten
und die Item-Tags, keine Texturen, keine Sprachdateien. Die Dateien sind committet, weil
CI keinen Gradle-Cache hat: ohne Jar bleibt die vorhandene Datei stehen, mit Jar prüft
`--check`, dass sie aktuell ist. Die Seite lädt die Datei der gewählten Linie erst, wenn
der Baum sie braucht; `tools/wiki_site.py` veröffentlicht beide mit.

### 3D-Ansicht der Blöcke

Der Generator zieht aus jedem Blockmodell die drei sichtbaren Flächen
(`top`, `side`, `front`) und legt sie als `faces` an den Blockeintrag. Die App
stellt daraus einen isometrischen Würfel wie im Inventar (45° gedreht, 30° von
oben, orthografisch) – mit reinen 2D-Matrizen, kein 3D-Kontext, kein WebGL, kein
Build. Nur würfelartige Modelle bekommen einen (`cube_all`, `cube`,
`orientable`, `cube_bottom_top`, …); Trichter, Kolben, Stufen, Treppen und Mauern
haben eine Form, die drei Quadrate nicht abbilden, und behalten die flache Textur.

Der Würfel steht nicht nur im Kopf der Blockseite, sondern **in jedem Slot**
(Rezepte, Listen, Baum), sobald das Item das Blockmodell zeigt: `inventoryCube`
am Blockeintrag setzt der Generator nur, wenn `items/<name>.json` genau auf
`simplebuilding:block/<name>` verweist und nicht eingefärbt ist. Für Vanilla-Blöcke
schreibt er dieselben drei Flächen aus dem Client-Jar nach
`wiki/assets/textures/minecraft/cubes.js` (plus `faces/*.png`) – neben die
Vanilla-Texturen, wie sie in `.gitignore` und nicht veröffentlicht. Die Seite lädt
die Datei, wenn es sie gibt; sonst bleibt es beim flachen Bild oder der Textkachel.
Eingefärbte Modelle (Gras, Laub) und animierte Texturen (hohe Streifen) sind
ausgenommen.

### Alle Rezepte und die JEI-Ansicht (`#/allrecipes`, `#/make/<id>`, `#/use/<id>`)

Eine Karte je Rezept: Werkbank (geformt/formlos, Umfärben, Sonderrezepte), Ofen,
Schmelzofen, Räucherofen, Lagerfeuer, Schmiedetisch (auch mengenbasiert),
Steinsäge und die Umwandlungen in der Welt mit Zeit, Schlägen und Haltbarkeit.
Filter nach Station, Herkunft (Mod/Vanilla), Minecraft-Linie und Text.

* **Linien:** jedes Mod-Rezept und jede Umwandlung trägt `lines` – in welchen
  Linien es dieselbe Id gibt; was nur die andere Linie hat, steht unter
  `recipesOtherLines`. Vanilla-Rezepte kommen aus `data/vanilla-<linie>.js`;
  dasselbe Rezept in beiden Linien wird eine Karte (1.21.11 schreibt die
  Standard-Garzeit aus, 26.2 nicht – für den Vergleich gleichgezogen).
* **Varianten:** Karten, die sich nur in einer Farbe oder Holzart unterscheiden
  (als ganzes Glied einer Id), werden eine Karte, die reihum wechselt; ein Klick
  auf eine Variante hält sie fest. Tag-Zutaten zeigen reihum ihre Mitglieder.
* **Je Item** wie in JEI: `#/make/<id>` (Herstellung) und `#/use/<id>`
  (Verwendung, Tags aufgelöst), nach Station gruppiert, mit Blättern. In den
  Rezeptansichten führt ein Klick auf eine Zutat zu deren Rezepten; über jedem
  Item-Slot öffnen **R**/**U** dasselbe, das Kontextmenü auch.

Auf der Item-Seite stehen die Rezepte je Station in eigenen Abschnitten; jeder
Abschnitt lässt sich einklappen, und was zugeklappt ist, bleibt es auf allen
Seiten (nur in diesem Browser, `localStorage`).

### Kernmerkmal der aufgewerteten Maschinen

Ofen, Räucherofen, Schmelzofen und Trichter der Stufen Verstärkt/Netherit/Enderit
bekommen oben auf ihrer Seite eine Zeile „N× so schnell wie Vanilla". Die Zahl
steht im Spiel nicht in einer Konstante, sondern als Literal in der Tick-Methode
der Block-Entity (`extraTicks = …` bzw. `speed = …`, je Block ein Zweig).
`generate.py` liest genau diese Zweige (`collect_machine_speeds`), ordnet sie über
`registerBlock("name", Blocks.X, …)` in `ModBlocks.java` Block und
Vanilla-Gegenstück zu und legt sie mit Datei:Zeile als `machine` an den
Blockeintrag. Findet der Parser einen Zweig nicht mehr, meldet `--check` ein
PROBLEM. Den Vanilla-Trichtertakt schreibt `WikiDataProvider` aus
`HopperBlockEntity.MOVE_ITEM_SPEED` nach `items.json` (`vanilla.hopperMoveItemSpeed`).

Gerechnet wird nur, was der Code vorgibt: Vanillas `serverTick` zählt +1 Gartick,
die Mod danach `extraTicks` dazu, gekappt bei Gesamtzeit − 1 – ein Rezept mit
T Ticks braucht also ⌈(T − 1)/(1 + extraTicks)⌉ + 1 Spielticks. Die
Beispieldauer ist der Vanilla-Standard (`SmeltingRecipe` 200, `SmokingRecipe` und
`BlastingRecipe` 100 im `cookingMapCodec`). Trichter: nach jedem erfolgreichen
Schritt setzt die Mod die Abklingzeit auf `speed`, Vanilla auf `MOVE_ITEM_SPEED`; ein Schritt
bewegt ein Item.

### Wirkt eine Verzauberung? (`implementedIn`)

Die meisten Verzauberungen dieser Mod haben keinen datengetriebenen Effekt –
Aderabbau, Radius, Vielseitigkeit und andere liegen ganz im Java-Code. `effects: {}`
in der JSON sagt also nichts darüber aus, ob sie wirken. Der Generator sucht
deshalb zusätzlich nach `ModEnchantments.X` im Spiel-Code und setzt
`implementedIn` auf `data`, `code`, `both` oder `none`.

Zwei Vorkehrungen halten das ehrlich:

* Gescannt werden **alle** Codewurzeln der Linie, auch die Loader-Module. Eine nur
  dort implementierte Verzauberung galt vorher als nicht implementiert.
* Dateien, die jede Verzauberung nennen, ohne ihr Verhalten zu geben – Registrierung,
  Kreativ-Tab, Loot, Modellauswahl –, stehen in `CATALOGUE_FILES`. Und weil so eine
  Datei jederzeit neu dazukommen kann (Tooltip-Anbieter, JEI-Anbindung), gilt
  zusätzlich: Wer mehr als `CATALOGUE_SHARE` (60 %) aller Verzauberungen nennt, wird
  als Katalog gewertet und **gemeldet**, statt stillschweigend alle auf „wirkt" zu
  kippen. Gemessene Trennung: die bekannten Kataloge nennen 89–100 % der
  Verzauberungen, die größte echte Spiel-Code-Datei 37 %.

### Vanilla-Texturen

Zutaten wie `minecraft:stick` erschienen früher als Textkachel, weil Mojangs
Assets nicht im Repository liegen – und dort auch nicht hingehören. Der
Generator holt sich deshalb **nur die tatsächlich referenzierten** Vanilla-Ids
aus dem Client-Jar im Gradle-Cache
(`~/.gradle/caches/fabric-loom/<version>/minecraft-client.jar`) und legt sie
flach unter `wiki/assets/textures/minecraft/<name>.png` ab. Das Verzeichnis steht
in `.gitignore`.

Zwei Folgen davon:

* Ein frisch geklontes Wiki zeigt diese Zutaten erst nach einem
  `python wiki/generate.py`-Lauf; bis dahin bleibt es bei der Textkachel.
* Die Zuordnung steht **nicht** in der erzeugten JSON, sondern folgt der
  Konvention „ein Bild je Id". Sonst hinge `--check` am Vorhandensein des
  Gradle-Caches und schlüge auf einem Rechner fehl, der die Mod nie gebaut hat.

Wo eine Id weder `item/<name>.png` noch `block/<name>.png` hat (Ofen, Kolben,
Kompass), wird ihr Modell gelesen und dessen erste vorhandene Texturreferenz
genommen. Übrig bleibt derzeit genau eine Id: `minecraft:fly_into_wall` – ein
Schadenstyp aus einem Tag, der zu Recht kein Bild hat.

Auch die Bündel- und Köcher-Kapazität kommt von dort. Sie hing an
`getTierCapacityMultiplier(ItemStack)`, und im Datagen lässt sich kein `ItemStack`
bauen – dessen Konstruktor liest ab MC 26.2 die dort noch nicht gebundenen
Komponenten. `ReinforcedBundleItem` schlägt die Stufe deshalb item- statt
stackbasiert nach und die Stack-Variante delegiert dorthin, also bleibt es bei
einer Tabelle. `QuiverItem` überschreibt die Grundkapazität, weil Köcher den
1,5-Faktor der Bündel nicht obendrauf bekommen: 64/96/128/192 statt 96/192/288 (die 3/2 des
Verstärkten Köchers sind sein Stufenfaktor).

**Das Einzige, was von Hand gepflegt wird, ist `wiki/manual.json`:** Fließtext, den keine
Datei der Mod enthält – was ein Werkzeug *tut*, wie man es bedient, was sich je Stufe ändert.

### Der Zwang, nicht die Bitte

Ein Wiki, das „bitte aktuell halten" sagt, veraltet. Deshalb ist die Pflicht in den Build
eingebaut:

```bash
gradlew check          # enthält checkWiki
python wiki/generate.py --check
```

`checkWiki` schlägt fehl, wenn

1. die committete `wiki/data/simplebuilding.json` nicht mehr dem entspricht, was die Mod jetzt
   erzeugen würde (also: Mod geändert, Wiki nicht neu erzeugt), **oder**
2. etwas im Spiel existiert, das in `manual.json` keinen Fließtext hat.

Punkt 2 gilt gezielt nur für Dinge mit eigenem Verhalten – Werkzeuge, Maschinen, Verzauberungen.
Ein reiner Baublock ist durch Rezept und Drop ausreichend beschrieben und braucht keine Prosa.
Die Menge ergibt sich aus dem Code (welche Registrierung eine Klasse aus `items/custom/` oder
`blocks/custom/` benutzt), nicht aus einer Liste hier. Ein neues Werkzeug fordert seinen
Fließtext also von selbst ein.

### Ablauf bei einer Änderung an der Mod

```bash
# 1. Mod ändern, Datagen laufen lassen wie gewohnt
#    (schreibt auch src/main/generated/wiki/items.json)
gradlew runDatagen

# 2. Wiki neu erzeugen
python wiki/generate.py

# 3. Falls etwas Neues Fließtext braucht, sagt es dir der Lauf:
#      "N entries have no prose in wiki/manual.json"
#    -> Eintrag in manual.json ergänzen, Schritt 2 wiederholen

# 4. wiki/ mit committen
```

Stufenfamilien bekommen **einen** Eintrag mit Glob-Schlüssel, etwa `"*_sledgehammer"` – der
gilt für alle Stufen, und eine neue Stufe (etwa ein Enderit-Hammer) erbt ihn. Eine einzelne
Stufe kann mit ihrem exakten Schlüssel überschrieben werden.

### Aufbau von `manual.json`

Die Datei ist **zweisprachig**: jeder Eintrag trägt einen `en`- und einen
`de`-Block. `sources` und `related` stehen sprachneutral daneben, damit jede
Quellenangabe genau einmal dasteht.

```json
{
  "features": [
    {
      "id": "sledgehammer",
      "related": ["simplebuilding:diamond_sledgehammer"],
      "sources": ["common/src/shared/java/com/simplebuilding/items/custom/SledgehammerItem.java"],
      "en": { "title": "Sledgehammer", "summary": "Two or three sentences.", "details": ["one bullet, one claim"] },
      "de": { "title": "Vorschlaghammer", "summary": "Zwei bis drei Sätze.", "details": ["ein Stichpunkt, eine Behauptung"] }
    }
  ],
  "notes": {
    "*_sledgehammer": {
      "sources": ["..."],
      "en": { "summary": "...", "details": [], "controls": [], "tiers": [], "caveats": [] },
      "de": { "summary": "...", "details": [], "controls": [], "tiers": [], "caveats": [] }
    }
  }
}
```

Jede Behauptung in `details`, `controls`, `tiers` und `caveats` soll sich im Code belegen
lassen; `sources` nennt die Dateien. Das ist keine Formalie – jede Fassung wurde
Behauptung für Behauptung gegen den Code geprüft, und so soll es bleiben.

**Die englische Fassung ist keine Übersetzung.** Sie wurde aus demselben Code
geschrieben wie die deutsche; die jeweils andere Sprache dient nur als
Gegenprobe: gleiche Behauptungen, gleiche Zahlen. Wer einen Eintrag ändert,
ändert beide Sprachen – sonst schlägt der Lauf fehl:

```
N entries have prose in only one language:
   - simplebuilding:magnet  (missing: en)
```

Bis alle Einträge umgestellt waren, galt die alte flache Form (Prosafelder
direkt am Eintrag) als Deutsch. Diese Nachsicht steht noch in
`prose_languages()`; eine Datei, die versehentlich in die alte Form zurückfällt,
wird deshalb als „nur Deutsch" gemeldet statt als undokumentiert.

### Beide Minecraft-Linien

```bash
python wiki/generate.py                 # 26.2 (Standard)
python wiki/generate.py --line 1.21.11  # 1.21.11
```

Die Datengrundlage beider Linien stammt aus denselben Providern; das Wiki zeigt die gewählte
Linie im Kopf an.

## Dateien

| Datei | Zweck | von Hand? |
|---|---|---|
| `generate.py` | erzeugt alles aus der Mod | ja, aber selten |
| `manual.json` | Fließtext | **ja – die einzige Pflegedatei** |
| `index.html` | die App, eine Datei, Vanilla-JS | ja, selten |
| `data/simplebuilding.json` | die Doku als JSON | nein, generiert |
| `data/simplebuilding.js` | dasselbe als `window.WIKI_DATA`, damit `file://` funktioniert | nein, generiert |
| `data/vanilla-<linie>.js` | Vanilla-Rezepte je Minecraft-Linie für den Rezeptbaum | nein, generiert |
| `../tools/wiki_site.py` | stellt die statische Seite für das Hosting zusammen (ohne Vanilla-Texturen) | ja, selten |
| `../tools/git-hooks/pre-commit` | optionaler Hook: `--check` vor jedem Commit | ja, selten |
| `../.github/workflows/wiki.yml` | CI: prüfen, auf `master` nach GitHub Pages veröffentlichen | ja, selten |

Hosting, Hook und CI: [`docs/WIKI-HOSTING.md`](../docs/WIKI-HOSTING.md).
