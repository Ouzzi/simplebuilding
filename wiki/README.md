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

### Adressen, Navigation, Darstellung

Jede Ansicht hat eine teilbare Adresse mit Suchparametern; Zurück/Vor im Browser
funktioniert (`history.pushState`/`popstate`):

| Ansicht | Adresse |
|---|---|
| Item / Block | `?item=simplebuilding:enderite_chisel` (reine Blöcke wie Pads landen auf `?block=`), mit `&tab=recipes\|uses\|drops\|trades\|inworld\|related\|technical` springt die Seite zum Abschnitt |
| Kategorie | `?cat=tools\|building\|storage\|machines\|gadgets\|end\|trims\|materials\|misc` |
| Listen | `?tab=items\|blocks\|recipes\|allrecipes\|inworld\|loot\|trades\|enchantments\|tags\|config\|advancements\|features\|textures` |
| Einzelseiten | `?recipe=`, `?loot=`, `?trade=`, `?ench=`, `?tag=`, `?feature=`, `?config=`, `?inworld=`, `?make=`, `?use=`, `?tree=` |
| Suche | `?q=hammer` |
| Sprache | `&lang=de` (sonst gilt die gespeicherte Wahl) |

Alte Hash-Links (`#/items/<id>`) werden beim Laden auf die neue Form umgeleitet. Intern
bauen die Seiten weiter Hash-Routen; ein `MutationObserver` schreibt sie beim Einfügen
um. Wo ein Browser `pushState` unter `file://` verweigert, bleibt die Seite beim Hash-Router.

Die **Kategorien** stehen nicht in den Daten: `CAT_RULES` in `index.html` leitet sie aus
der Id ab (erste passende Regel, „End & Enderit“ kommt zusätzlich dazu; was keine Regel
trifft, landet unter „Materialien“). Suche: `/` oder `Strg+K`, Pfeiltasten, Enter.
Farbschema: Umschalter oben rechts (System → hell → dunkel, in `localStorage`).

Die **Items-Seite** (`?tab=items`) listet Items *und* Blöcke (Art-Filter Alle/Items/Blöcke;
`?tab=blocks` ist dieselbe Seite mit Blöcken vorgewählt), als Liste oder Kacheln, wahlweise mit
den Rezepten direkt am Eintrag. Ansicht, Art und Rezept-Schalter bleiben gespeichert
(`simplebuilding-wiki-itemsview`). „Alle Rezepte“ lässt sich nach Station in einklappbare
Abschnitte gruppieren (Alle auf-/zuklappen; Zustand wie die übrigen Abschnitte).

Listen merken ihren Suchtext je Mod und Bereich. Die Rezeptansicht merkt zusätzlich
ihre Filter und Ansichtsschalter je Mod. Ohne verfügbaren Browserspeicher bleiben
Listenfilter während der aktuellen Seitensitzung erhalten. Bei leerem Filterergebnis
stellt „Filter zurücksetzen“ alle Zeilen wieder her. Lange Tabellen haben feste
Kopfzeilen in scrollbaren, per Tastatur erreichbaren Bereichen.

## Wie die Doku aktuell bleibt

Familiennotizen in `manual.json` können ein optionales `items`-Objekt enthalten:
`"items": {"simplebuilding:iron_chisel": {"en": "Iron Chisel: 256 durability.",
"de": "Eisenmeißel: 256 Haltbarkeit.", "sources": ["common/src/shared/java/com/simplebuilding/items/ModItems.java"]}}`.
Die Schlüssel sind vollständige registrierte Item-/Block-IDs, passend zur Familiennotiz.
Beide Sprachsätze müssen nichtleer sein; `sources` ist optional.
`generate.py` und `modules.py` stellen den passenden Satz vor die jeweilige
Familienzusammenfassung und übernehmen seine Quellen. Das ausgegebene `note` bleibt
im bisherigen Format: `index.html` zeigt `summary` zuerst und durchsucht sie bereits;
`tools/wiki_site.py` verpackt diese Daten unverändert. `--check` prüft auch unbekannte
IDs, unpassende Familien und fehlende Sprachsätze.

Das ist der Kern des Aufbaus, deshalb ausführlich.

**Nichts in `wiki/data/` ist von Hand geschrieben.** `generate.py` liest die Mod selbst:

| Bereich | Quelle im Repo |
|---|---|
| Items, Blöcke, Namen | `src/main/resources/assets/simplebuilding/lang/*.json` |
| Texturen | `src/main/generated/assets/simplebuilding/models/**` → `textures/**` |
| 3D-Bilder der Blöcke und Köpfe | Item-Definitionen, Block- und Item-Modelle samt Elternkette (Vanilla-Eltern aus dem Client-Jar), gezeichnet von `wiki/model_render.py` |
| Beute ohne Rezept (Truhen, Tresore, Angeln, Mob-Drops) | `loot/ModLootTableModifications.java` der Linie (geparst) + `wiki/data/vanilla-drops-<linie>.json` aus dem Client-Jar |
| Vanilla-Texturen der Zutaten | `minecraft-client.jar` im Gradle-Cache (nicht im Repo, s. u.) |
| Rezepte | `src/main/generated/data/simplebuilding/recipe/**` |
| Loot-Tabellen | `src/main/generated/data/simplebuilding/loot_table/**` |
| Handel | `src/main/resources/data/simplebuilding/villager_trade/**` |
| Verzauberungen | `src/main/generated/data/simplebuilding/enchantment/*.json` |
| Fortschritte (Advancement-Baum mit Titeln, Hinweisen, Symbolen, Eltern) | `src/main/generated/data/simplebuilding/advancement/**` ohne `recipes/` und die geheime Kette `easter/` – vom Datagen-Provider `ModAdvancementProvider` geschrieben |
| Tags | `.../tags/**` (generiert und Ressourcen) |
| Konfiguration | `common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java` + `tweaks/TweaksConfig.java` (alle Optionen als Punkt-Pfad, Reiter, Namen/Tooltips en+de aus den Sprachdateien) |
| Haltbarkeit, Stapelgröße, Verzauberbarkeit, Angriffswerte, Zauberstab-Durchmesser, Meißel-Abklingzeit | `mc26_3/generated/wiki/items.json` (26.3; ältere Linien separat) – vom Datagen-Provider `WikiDataProvider` aus der **Item-Registry** geschrieben |
| Umwandlungen in der Welt (Maschinen-Aufwertung, Umformen, Diamantblock, Meißel, Schere auf Wolle, abgelegte Besatzvorlage, Waschen im Kessel) | `mc26_3/generated/wiki/inworld.json` (26.3; ältere Linien separat) – vom Datagen-Provider über `InWorldTransformations` aus denselben Tabellen und Konstanten geschrieben, die das Spiel benutzt |
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
`mc26_3/generated/wiki/items.json` (26.3; ältere Linien separat). Wer eine Konstante ändert, ändert Mod und
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
`mc26_3/generated/wiki/inworld.json` (26.3; ältere Linien separat); die Spieltests `InWorldExportTests` halten den
Export gegen das Spiel. Derselbe Export speist das JEI-Plugin (`InWorldRecipeCatalog`). Was
keine Tabelle im Code hat, stuende in `manual.json` unter `inWorld.entries` mit Quellen (derzeit leer), die Prosa je Art unter `inWorld.kinds` (en + de, von
`--check` verlangt).

### Rezeptbaum und Vanilla-Rezepte

Rechtsklick auf eine Item-Kachel öffnet ein kleines Menü; „Rezeptbaum“ (`#/tree/<id>`)
zeigt alle Wege zu einem Item – Rezepte der Mod und aus Vanilla, Schmelzen, Umwandlungen
in der Welt, Drops, Handel – und klappt die Zutaten rekursiv auf. Varianten (beliebige
Bretter) werden zu einem Zweig zusammengefasst. Linksklick bleibt, wie er war.

Die Vanilla-Rezepte liest `generate.py` je Linie aus dem Client-Jar im Gradle-Cache und
schreibt sie nach `wiki/data/vanilla-26.2.js`, `vanilla-26.3.js` bzw. `vanilla-1.21.11.js` – nur Rezeptdaten
und die Item-Tags, keine Texturen, keine Sprachdateien. Die Dateien sind committet, weil
CI keinen Gradle-Cache hat: ohne Jar bleibt die vorhandene Datei stehen, mit Jar prüft
`--check`, dass sie aktuell ist. Die Seite lädt die Datei der gewählten Linie erst, wenn
der Baum sie braucht; `tools/wiki_site.py` veröffentlicht alle mit.

### 3D-Bilder aus den Modellen (`wiki/model_render.py`)

Blöcke erscheinen wie im Inventar: Treppen, Stufen, Mauern, Kolben (mit Plattform
oben), Druckplatten, Pads, Trichter, Rucksäcke, Köpfe, die abgelegte Schmiedevorlage.
`model_render.py` zeichnet sie in Python (Pillow + numpy) aus den echten Modellen:

- **welches Modell**: die Item-Definition `items/<id>.json` (Bedingungen nehmen den
  `false`-Zweig, `select` nach `display_context` den `gui`-Fall, sonst den Rückfall);
  Blöcke ohne eigenes Item (Kolbenkopf, abgelegte Vorlage) den ersten Blockstate;
- **Form**: die Elternkette (`minecraft:block/stairs` usw. aus dem Client-Jar) mit
  Elementen, UVs, UV-Drehung, Element-Drehung samt `rescale`, Textur-Verweisen `#x`
  (auch 26.x `{"sprite": …}`), `tintindex` mit den Tints der Item-Definition;
- **Ansicht**: `display.gui` des Modells (Treppen und Mauern haben eine eigene), sonst
  die von `block/block`; orthografisch, Z-Puffer, Rückseiten weg, GUI-Schattierung
  (oben hell, links heller als rechts), Pixel ohne Glättung – 128 × 128 px;
- **Sonderfälle**: `minecraft:special` mit `head` (Vanilla-Köpfe und der Lohenkopf aus
  der 64 × 32-Entity-Textur, Piglin mit Schnauze), Wandköpfe wie ihr Kopf-Item, die
  abgelegte Schmiedevorlage (nur Blockentity-Renderer) als flach liegende Platte wie
  `PlacedTemplateRenderer` sie zeichnet (`LYING_ITEM_BLOCKS` in `generate.py`).

Eigene Bilder landen unter `wiki/assets/textures/render/` (Blöcke mit flachem Item-Bild –
Trichter, Rucksack – zusätzlich als Blockform unter `render/block/`) und **werden
committet**: sie enthalten nur Texturen der Mod. Der Eintrag bekommt `icon`; die Seite
nimmt `icon` vor `texture` und glättet die großen Bilder beim Verkleinern (`img.r3d`).
Vanilla-Blöcke, -Treppen, -Mauern und -Köpfe zeichnet derselbe Renderer über ihre
flache Textur unter `assets/textures/minecraft/` (nicht committet, nicht veröffentlicht).

Ohne Pillow, numpy oder Client-Jar (CI) wird nichts gezeichnet und nichts gelöscht; das
JSON nennt ein `icon` nur, wenn die Datei da ist, deshalb bleibt es auf jedem Rechner
gleich. Ein Lauf, der zeichnen kann, löscht Bilder, die nichts mehr braucht.
**`--check` schlägt fehl**, wenn ein Item oder Block weder Textur noch Bild hat (Ausnahme:
Item-Definition `minecraft:empty`, der Kreativ-Platzhalter) oder der Renderer eine Textur
bzw. ein Modell nicht findet. Ebenso, wenn ein Item mit Blockmodell (voller Block, Treppe, Stufe …)
kein committetes 3D-Bild hat – sonst zeigte die Seite still die flache Seitentextur (so fehlten
die Bilder des Dimensionsschrotts, die in einem Lauf ohne Renderer dazukamen). Abhilfe: einmal
mit Pillow, numpy und Client-Jar erzeugen und das neue Bild unter `render/` committen.

### Woher etwas ohne Rezept kommt (`wiki/obtain_sources.py`)

`D.obtain.sources` listet jede Quelle als Karte: Truhen, Tresore, Angel-Schatz (aus den
Pools in `loot/ModLootTableModifications.java` der Linie, geparst; was der Parser nicht
versteht, wird ein PROBLEM) und Mob-Drops – die Köpfe per geladenem Creeper
(`charged_creeper/*` aus dem Jar plus der Lohenkopf-Pool der Mod) und die Schallplatten,
wenn ein Skelett einen Creeper tötet. Die Vanilla-Tabellen stehen zusätzlich in
`wiki/data/vanilla-drops-<linie>.json` (committet; ohne Jar gelesen, mit Jar von `--check`
geprüft). Die Chance ist „mindestens eins pro Truhe“: `1 − E[(1 − w/W)^Würfe]`.

Die Seite zeigt die Quellen als Karten oben auf der Item-Seite („So bekommst du es“) und
vollständig im Abschnitt „Beute & Mob-Drops“, alle zusammen auf der Seite „Loot“. Jede
Verzauberung hat oben „Wo es sie gibt“: Zaubertisch ja/nein (Tag
`minecraft:in_enchanting_table` der Mod), Truhen und Tresore mit Stufe und Chance, Angeln,
Dorfbewohner mit Gewicht, Amboss-Ziele. Die Verzauberungsliste und der Abschnitt
„Verzauberungen für dieses Item“ auf Item-Seiten tragen dieselben Kurzabzeichen. JEI
zeigt die Mob-Drops als Kategorie „Mob-Drops“ (`compat/MobDropCatalog`, geprüft vom
Spieltest `InWorldExportTests#mobDropCatalogMatchesTheGame`).

### Alle Rezepte und die JEI-Ansicht (`#/allrecipes`, `#/make/<id>`, `#/use/<id>`)

Eine Karte je Rezept: Werkbank (geformt/formlos, Umfärben, Sonderrezepte), Ofen,
Schmelzofen, Räucherofen, Lagerfeuer, Schmiedetisch (auch mengenbasiert),
Steinsäge und die Umwandlungen in der Welt mit Zeit, Schlägen und Haltbarkeit.
Filter nach Station, Herkunft (Mod/Vanilla), Minecraft-Linie und Text.

* **Linien:** jedes Mod-Rezept und jede Umwandlung trägt `lines` – in welchen
  Linien es dasselbe Rezept gibt (gleiche Id UND gleicher Inhalt; der
  Rezeptbuch-Reiter `category` zählt nicht). Hat eine andere Linie dieselbe Id
  mit anderem Inhalt, listet das Rezept sie unter `variants` (Linien, geänderte
  Felder, Quelle) – Rezeptseite und Itemseite zeigen das als "In Minecraft X
  anders" –, und die andere Fassung steht zusätzlich unter `recipesOtherLines`.
  Vanilla-Rezepte kommen aus `data/vanilla-<linie>.js`; dasselbe Rezept in
  mehreren Linien wird eine Karte (1.21.11 und 26.3 schreiben die
  Standard-Garzeit aus, 26.2 nicht – für den Vergleich gleichgezogen).
* **26.3-Garzeiten:** ab 26.3 speichern Hochofen- und Räucherofen-Rezepte die
  Ofenzeit, der Hochofen/Räucherofen halbiert sie. `generate.py` schreibt die
  echte Zeit nach `cookingtime` und den Dateiwert nach `storedCookingtime`; die
  Seite zeigt die echte Zeit und einen Hinweis auf den gespeicherten Wert.
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

### Texturen-Seite (`?tab=textures`, `wiki/textures.py`)

Zeigt **jede Textur aller Module** (Items, Blöcke, Kreaturen, GUI-/Container-Texturen,
Partikel, Effekte, Besätze, Paletten …) – nichts davon wird von Hand gepflegt.
`generate.py` ruft nach den Mod-Daten `wiki/textures.py` auf (Standardlauf und `--all`):

- Quellen: SimpleBuilding `src/main/resources` mit `mc26_3/overlay/resources` darüber (wie
  `mergeResources263`), Module `<shared>/resources`, Loader-Ressourcen und `generated`
  (erster Fund gewinnt, wie bei den Item-Texturen in `modules.py`). Alle Namensräume unter `assets/`.
- Je Textur: Art (erster Ordner unter `textures/`), Maße aus dem PNG-Kopf, Bytes, `.mcmeta`
  (Animation: Einzelbildgröße, Anzahl, Ticks, eigene Reihenfolge), zuletzt geändert (Datum des letzten
  Commits der Quelldatei, ein `git log`-Durchlauf), Name und „fertige“ Form aus den Wiki-Daten: ein
  Item/Block mit genau dieser Textur bzw. gleicher Id liefert Namen (EN/DE) und Icon bzw. 3D-Render.
- Bilddateien: eine byte-gleiche Kopie, die das Wiki schon unter `assets/textures/` hat, wird
  benutzt; alles andere landet unter `assets/textures/sheets/<modul>/<namensraum>/…`. Verwaiste Kopien
  werden gelöscht.
- Ausgabe `data/textures.js` (`window.WIKI_TEXTURES`, eine Textur je Zeile). Die Seite lädt sie erst
  beim ersten Besuch. `--check` vergleicht ohne das Datum (sonst wäre die Datei nach jedem Commit
  „veraltet“).

Die Seite filtert nach Name/Id (Volltext), Modul, Art, Animation und Auflösung (Einzelbildgröße)
und sortiert nach Name, Modul, Art, Größe oder Änderungsdatum. Jede Kachel zeigt beide Formen:
„Fertig“ (Icon/3D-Render, sonst das erste Einzelbild) und das Textur-Sheet. Klick öffnet die
Lightbox: beide Formen nebeneinander, Zoom Original/8×/16× (große Dateien höchstens 2048 px),
Hintergrund hell/dunkel/Slot-Grau, Animation abspielbar, Pfeiltasten blättern, Esc schließt.

**Oberflächen (UIs):** zeigt die GUI-Texturen (ohne Einzel-Sprites unter `gui/sprites/`), solange
keine Screenshots übernommen wurden. Screenshots aus Client-Tests werden **nicht eingecheckt**:

```bash
python wiki/generate.py --all --ui-shots build/run/clientGameTest/screenshots
```

kopiert alle PNGs des Ordners (rekursiv) nach `wiki/assets/ui/` und schreibt `wiki/data/ui-shots.js`.
Das Modul ergibt sich aus dem Pfad oder Dateinamen (z. B. `module-simplefun-client-263/…`). Beides
steht in `.gitignore`; `tools/wiki_site.py` veröffentlicht es nicht.

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

### Die Minecraft-Linien

```bash
python wiki/generate.py                 # 26.2 (Standard)
python wiki/generate.py --line 1.21.11  # 1.21.11
python wiki/generate.py --line 26.3     # 26.3
```

Die Datengrundlage aller Linien stammt aus denselben Providern; das Wiki zeigt die gewählte
Linie im Kopf an. 26.3 hat keinen eigenen Datagen-Baum: `generate.py` legt ihn vor jedem Lauf
unter `build/wiki-lines/26.3/` an – `src/main/generated`, darüber `mc26_3/generated`, ohne die
Dateien aus `mc26_3/generated/removed-on-26.3.txt` (derselbe Vorrang wie `mergeResources263`);
die `source`-Felder nennen weiter die echte Datei.

## Dateien

| Datei | Zweck | von Hand? |
|---|---|---|
| `generate.py` | erzeugt alles aus der Mod | ja, aber selten |
| `manual.json` | Fließtext | **ja – die einzige Pflegedatei** |
| `index.html` | die App, eine Datei, Vanilla-JS | ja, selten |
| `data/simplebuilding.json` | die Doku als JSON | nein, generiert |
| `data/simplebuilding.js` | dasselbe als `window.WIKI_DATA`, damit `file://` funktioniert | nein, generiert |
| `textures.py`, `data/textures.js` | Texturen-Seite: Index aller Mod-Texturen | Skript ja, Daten nein |
| `data/vanilla-<linie>.js` | Vanilla-Rezepte je Minecraft-Linie für den Rezeptbaum | nein, generiert |
| `../tools/wiki_site.py` | stellt die statische Seite für das Hosting zusammen (ohne Vanilla-Texturen) | ja, selten |
| `../tools/git-hooks/pre-commit` | optionaler Hook: `--check` vor jedem Commit | ja, selten |
| `../.github/workflows/wiki.yml` | CI: prüfen, auf `master` nach GitHub Pages veröffentlichen | ja, selten |

Hosting, Hook und CI: [`docs/WIKI-HOSTING.md`](../docs/WIKI-HOSTING.md).
