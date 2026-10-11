# Konzept Questionary Table (2026-10-11)

Besitzer-Wunsch 11.10.: Tisch aus Buch + Lapis + Holz. Item links in einen Slot, rechts eine offene Buchseite mit
3 Seiten (Blättern): 1. Lore, 2. Nutzen, 3. Geheiminformation. Jedes Vanilla- und Mod-Item bekommt alle 3 Texte;
fremde Mods bekommen mystische Fallback-Texte. Status: nur Konzept, Umsetzung nach Besitzer-OK (QUEUE Nachtrag 36).
Mockup: `~/previews/concepts/questionary.png` (Seite „2 Nutzen“ für den Enderit-Barren).

## 1. Name
- EN: **Questionary Table** (Besitzer-Name, Empfehlung). Alternativen: Loremaster's Desk, Codex Table.
- DE: **Fragentisch** (Empfehlung). Alternativen: Wissenstisch, Orakeltisch.
- Item-/Block-ID: `simplequestionary:questionary_table`. Lang: `block.simplequestionary.questionary_table`.

## 2. Rezept (shaped, 2 Lapis, 1 Buch, 5 Bretter)
```
L B L      L = Lapislazuli, B = Buch, P = beliebige Bretter (#minecraft:planks)
P P P
P . P
```
Alternativ (billiger, wenn zu teuer): 1 Lapis in der Mitte unten. Freischaltung über Buch (Advancement/Rezeptbuch wie
beim Verzauberungstisch). Kreativ-Tab: Funktionsblöcke der Simple-Mods.

## 3. Block
- Optik: niedriger Tisch (3 Beine aus Holz, Platte 16x3 px), darauf ein **schräg stehendes aufgeschlagenes Buch**
  (Lesepult-Neigung wie Vanilla-Lectern, aber breiter), links daneben ein flacher Lapis-Inlay-Streifen in der Platte
  und eine kleine Lapis-Kugel/Tintenfass. Texturen im Vanilla-Stil, eigene Formen (Pixel-Art-Regeln), Holzart =
  Eiche; Varianten je Holzart optional später.
- Modell: reines Block-Model-JSON (Elemente: Platte, 4 Beine, Buch als 2 geneigte Flächen). Kein BER nötig.
  Optional später: BER, das das Buch beim Öffnen aufblättert (wie Vanilla-Enchanting-Buch, aber nicht nötig für V1).
- Eigenschaften: Facing (4 Richtungen), Drops sich selbst, Hardness wie Werkbank (2.5), Tag `mineable/axe`.
- Rechtsklick öffnet das Menü (kein Redstone, keine BlockEntity-Daten außer ggf. Slot-Inhalt, siehe GUI).

## 4. GUI (Vanilla-Stil, Buchseite wie Guide-Buch/Lesepult)
Layout (siehe Mockup), GUI-Größe 256 x 200 + Buchblatt; bei kleiner Skala wie beim Enderit-Tiegel prüfen (Audit C3):
- **Links:** grauer Vanilla-Container-Kasten mit Titel „Item“, EIN Slot (Mitte), darunter der Item-Name in Seltenheits-Farbe;
  unten Spielerinventar (3x9 + Hotbar, Vanilla-Abstände).
- **Rechts:** aufgeschlagenes Buch (Pergamentton wie GuideBookItem-Seite, Mittelfalz). Text fließt automatisch über
  linke und rechte Buchseite (links Anfang, rechts Fortsetzung); Titel der Seite oben links in Braun, Trennlinie.
- **Reiter-Lesezeichen** am rechten Buchrand, drei Stück übereinander: 1 Lore (rot), 2 Nutzen (blau), 3 Geheim (violett).
  Aktiver Reiter ragt weiter heraus und hat goldenen Rand. Klick wechselt direkt.
- **Seitenwechsel-Pfeile** unten links/rechts im Buch (Vanilla-Buch-Pfeile, Sprites aus Lesepult/Guide wiederverwenden):
  blättern 1→2→3 (nicht zyklisch; am Ende ausgegraut). Mausrad, A/D und Pfeiltasten ebenso. Mitte unten „2 / 3“.
- Text-Overflow: pro Seite harte Grenze (~350 Zeichen, Test prüft per Schriftbreite); Rest nur über Mausrad-Scroll
  als Ausnahme, nicht als Regelfall.
- Item-Slot-Verhalten: normaler Slot, Item kommt beim Schließen zurück ins Inventar (wie Verzauberungstisch).
  **Shift-Klick** schiebt ein Item hinein. Ohne Item: Buch zeigt Willkommensseite („Lege einen Gegenstand links ab“).
- Komfort (später, nicht V1): JEI/EMI/REI-Klick auf Item füllt Slot ghost-artig; Taste Q über einem Inventar-Item
  öffnet den Tisch-Eintrag ohne Tisch (nur optional per Config).
- Item-Bild groß (2x) über dem Reiter 1 als Illustration der Lore-Seite.

## 5. Datenformat
**Empfehlung: ein Ressourcen-JSON je Namensraum und Sprache**, nicht die Lang-Dateien (die sind riesig, viele parallele
Bearbeiter, Merge-Konflikte). Pfad (Client-Ressource, Resource-Reload-Listener, Fallback `en_us`):
`assets/<ns>/questionary/<lang>.json`, z. B. `assets/simplebuilding/questionary/en_us.json` / `de_de.json`:
```json
{
  "enderite_ingot": { "lore": ["..."], "use": ["..."], "secret": ["..."] },
  "family:stairs":  { "lore": ["{name}: ..."], "use": ["..."], "secret": ["..."] }
}
```
- Schlüssel = Item-Pfad (`<ns>` folgt aus dem Dateipfad). Jede Seite = Liste von Absätzen (Zeilenumbruch/Leerzeile).
- Platzhalter: `{name}` (Itemname lokalisiert), `{mod}` (Mod-Anzeigename), `{item:ns:id}` (Itemname + Mini-Icon, auch
  klickbar zum Eintrag), `{key:ns:id}`-Lang-Verweise nicht nötig.
- **Familien/Muster** (`family:<name>`) gegen Massenpflege: Treppen, Platten, Mauern, Farbvarianten (Wolle, Beton ...),
  Octets (generiert, Nachtrag-Liste), Werkzeug-/Rüstungstiers. Eintrag nennt die Items per Tag
  (`#simplebuilding:questionary/stairs`) oder Regex; ein Einzel-Eintrag überschreibt die Familie. Ohne diese Familien
  wären es für SB allein >2000 Items (800 Item-Keys + Blöcke + Octets), mit Familien eher 400–600 Handtexte.
- **Automatik-Zusatz auf Seite 2 (Nutzen):** unter dem handgeschriebenen Text eine kurze, zur Laufzeit aus dem RecipeManager
  erzeugte Zeile „Hergestellt aus: ... / Verwendet in: n Rezepten (…)" (Items als Icons). So bleibt der Text kurz und
  die Fakten veralten nicht. Abschaltbar per Config; Handtext bleibt Pflicht.
- **Wiki-Integration:** `wiki/generate.py` liest dieselben JSONs und zeigt je Item in der Wiki-Seite einen Abschnitt
  „Questionary“ (3 Texte, EN/DE nach Wiki-Sprache). Eine Quelle, zwei Anzeigen. Die Wiki-Fakten (manual.json: Rezepte,
  Nutzungen) dienen umgekehrt dem Generator als Rohmaterial für Seite 2/3.
- **Generator** `tools/questionary/generate.py`: (1) Registry-Dump (alle `item.*`-Keys + Block-Items aus
  den Lang-Dateien/Registries), (2) pro Item Fakten sammeln: Rezepte rein/raus (`recipe/*.json`), Tags, Wiki-manual.json,
  Config-Schalter, (3) Skelett-Datei mit leeren Slots `"TODO"` je fehlendem Item + Faktenblock als Kommentarfeld
  `_facts`, (4) `--check`: meldet Lücken. Befüllen danach per Agenten (siehe 11).
- **Abdeckungs-Check (Pflichttest):**
  - GameTest `QuestionaryCoverageTests` (je Mod, Fabric-Wrapper-Katalog beachten): für jedes registrierte Item des eigenen
    Namensraums und `minecraft` existiert ein Eintrag (direkt oder Familie) mit **nichtleerem lore/use/secret in EN und DE**,
    keine unaufgelösten `{...}`-Platzhalter, keine `TODO`-Reste, Seitenlänge innerhalb der Grenze.
  - Python `tools/questionary/check_data.py` (leicht, lokal ausführbar) prüft dasselbe statisch gegen die Lang-/Registry-Listen,
    plus EN/DE-Schlüsselgleichheit.
  - Fremde Namensräume werden NICHT geprüft (Fallback greift), aber ein Test stellt sicher, dass der Fallback für
    jedes Item deterministisch ein Ergebnis liefert.

## 6. Freischaltung der Geheiminformation
**Empfehlung V1: sofort sichtbar.** Der Besitzer-Wunsch ist „Wissen finden“; Spoiler sind für Modpack-Wiki-Nutzer
eher Hilfe als Schaden, und Sofort-Anzeige ist einfach und testbar. Reiter 3 trägt trotzdem ein kleines Siegel-Icon,
das Geheimwissen als „besonders“ markiert.
Alternativen (per Config `questionary.secretMode`, V2):
1. `sealed_lapis`: Reiter 3 ist versiegelt; **1 Lapis** (Slot unter dem Item-Slot) entsiegelt den Eintrag dauerhaft für den
   Spieler (Player-Attachment/`Set<ResourceLocation>`, nicht Weltdaten). Nutzt das Lapis aus dem Rezept thematisch.
2. `discovered`: Geheimseite erst, wenn der Spieler das Item einmal besessen/hergestellt hat (nutzt Item-Besitz-Statistik).
3. `xp`: 1 Level pro Entsiegelung. Verworfen als Standard, weil es Neulinge bremst.
Standard bleibt `open`; Modpack-Autoren können umstellen.

## 7. Fremd-Mod-Fallback
Items aus Namensräumen ohne eigene Einträge bekommen **deterministische mystische Texte**:
- Auswahl: `Math.floorMod(hash(itemId), N)` (stabile 32-bit-Hash-Funktion, nicht `String.hashCode()` in einer JDK-abhängigen
  Weise; eigene FNV-1a) → derselbe Gegenstand zeigt in jeder Welt/jeder Sprache dieselbe Variante.
- **8 Varianten je Seite** (lore/use/secret) pro Itemklasse: Block, Werkzeug/Waffe, Rüstung, Nahrung/Trank, Sonstiges
  (Klasse aus Item-Komponenten: tool, weapon, armor, food, BlockItem). Die Texte stehen in `assets/simplequestionary/questionary/<lang>.json`
  unter `fallback.<klasse>.lore|use|secret` (Listen). Platzhalter `{name}` und `{mod}`.
- **Wahrheitsbindung:** „Nutzen“ erfindet nichts. Der mystische Text ist Stimmung; darunter steht die Automatik-Zeile aus
  Abschnitt 5 (Haltbarkeit, Nahrung, Rezeptzahl), also nur echte Fakten. „Geheim“ ist rein Stimmung und sagt das nicht als Fakt.
- Optional: Mod-Autoren können per Datapack/Resourcepack `assets/<ns>/questionary/en_us.json` ergänzen; echter Eintrag
  gewinnt vor Fallback. Das macht die Tabelle zum offenen Format.

## 8. Modul-Zuordnung
- **Neues Modul `simplequestionary`** (eigene Mod-Jar, `requires simplelib`, allein spielbar nach Prinzip-Doku). Es enthält
  Block, Menü, Screen, Reload-Listener, Fallback, Vanilla-Einträge (`minecraft` + Familien).
- **Simple-Mod-Einträge bleiben in den jeweiligen Mods** (`assets/<ns>/questionary/*.json` im Modul selbst): jede Mod beschreibt
  ihre Items, ohne `simplequestionary` zu kennen (reine Daten, ohne Tisch inert). So bleibt jede Mod alleinstehend,
  SB-Items werden nicht in einem Fremdmodul gepflegt. Super-Mod bündelt Questionary als Sub-Mod mit Config „Enable Simple Questionary".
- **simplelib-UI-Bausteine (neu/wiederverwendet):** `BookPageWidget` (zweiseitiges Pergament mit Textfluss), `BookTabBookmarks`
  (Reiter-Lesezeichen), `PageArrowButton` (Pfeile), `RichTextFlow` (Absätze + `{item:}`-Icons). Aus GuideBooks/Lesepult-Code
  extrahieren, nicht kopieren; im UI-Baustein-Doku-Verzeichnis eintragen (Konsistenz-Prinzip).
- Keine harte Abhängigkeit auf SB-Klassen; Item-Fakten kommen aus Registry/RecipeManager, nicht aus Modcode.

## 9. Lokalisierung
EN + DE Pflicht (Test). Weitere Sprachen: Fallback `en_us`. Vanilla-Itemnamen kommen aus dem Spiel, Texte nutzen `{name}`,
nie hartkodierte Itemnamen im Fließtext außer als `{item:...}`. DE-Texte nicht übersetzt, sondern parallel geschrieben (Stil:
EN „Über diesen Gegenstand schweigen die Sterne" ↔ DE entsprechend, kein Wort-für-Wort).

## 10. Umfang
- SB: 800 `item.simplebuilding.*`-Keys, 454 Block-Keys, dazu Octets (generierte Liste, Stufen/Platten je Block); andere Module
  zusammen ~50 Item-Keys. Vanilla 26.3: ca. 1400 Items.
- Mit Familien: Vanilla ~350 Einträge, SB ~500, übrige Module ~60. Grob 900 Einträge x 3 Seiten x 2 Sprachen.
- Geheimtexte nur wirklich zu belegen, wenn es Geheimnis gibt (Verknüpfungen, Nebennutzen, Config, Easter Eggs); bei Items ohne
  Geheimnis: Stimmung + Tipp („wird auch als Brennstoff ... "), nicht leer.

## 11. Befüllen per Agenten/Generator
1. Generator erstellt Skelett + `_facts` (Rezepte, Tags, Wiki-Text) je Item.
2. Agenten (sonnet, 40–60 Items je Auftrag, Familie zuerst) schreiben EN, danach DE parallel; Regeln im Auftrag: Stilvorgaben,
   Längenlimit, Fakten nur aus `_facts`, keine Wiki-Texte kopieren (eigene Formulierung, Vanilla-Wiki ist CC BY-NC-SA).
3. `check_data.py` + Coverage-Test pro Welle; Besitzer prüft Stichprobe (10 Items) vor nächster Welle.
4. Reihenfolge: Welt-/Werkstoff-Items (Enderit, Astral, Nihilith) → Werkzeuge → Blöcke/Octets → Vanilla.

## 12. Beispiel Enderite Ingot (`simplebuilding:enderite_ingot`)
Fakten (Code/Lang): Herstellung: Roh-Layered-Enderit wird zu **Enderit-Schrott** geschmolzen (Ofen/Hochofen, 10 XP);
**4 Schrott + 4 Diamant + 1 Netheritbarren** ergeben den Barren (Advancement „Beyond Netherite"); 9 Nuggets <-> 1 Barren
(`enderite_ingot_from_nugget`, `enderite_nugget_from_ingot`). Nutzung: **Enderit-Upgrade-Vorlage** (Schmiedetisch) hebt
Netherit-Ausrüstung und -Behälter (Rucksack, Bündel, Köcher, Truhe, Shulkerkiste, Trichter, Ofen, Kolben, Stäbe,
Hammer, Meißel ...) auf Enderit; Rüstung: weniger Void-Schaden, Gleiten beim Fallen; Trimm-Material „Enderit"; Hufeisen
(Riding, Tag `c:ingots/enderite`); **Enderit-Eimer** = 8 Nuggets + Eimer; **Astral-Verzauberungstisch** braucht ein Enderit-Nugget
als Einsatz und kommt beim Abbauen zurück (Konzept Astral-Verzauberung, Max. Level 50).

### EN
- **Lore:** "Forged where the End forgets its own edges. Four shards of scrap, four diamonds and the weight of Netherite
  were once pressed into one bar, and it has been humming ever since. Miners of the outer islands say it keeps a note
  only the void can hear."
- **Use:** "Smelt layered raw Enderite into Enderite Scrap. Combine 4 Scrap, 4 Diamonds and 1 Netherite Ingot to craft
  the ingot. At a Smithing Table, an Enderite Upgrade Template lifts Netherite gear and containers to Enderite tier.
  Armor softens void damage and lets you glide on a fall. Also: 9 nuggets make an ingot, a trim material, horseshoes."
- **Secret:** "One nugget is the heart of the Astral Enchanting Table: set it in when crafting it, and you may enchant up to
  level 50 with Blaze Dust. A bucket of 8 nuggets and a bucket holds the End's own dust-liquid. Break the table and
  the nugget returns to you."

### DE
- **Lore:** "Geschmiedet dort, wo das Ende seine eigenen Kanten vergisst. Vier Splitter Schrott, vier Diamanten und das
  Gewicht von Netherit wurden einst zu einem Barren gepresst, und er summt seitdem. Die Bergleute der äußeren Inseln
  sagen, er hält einen Ton, den nur die Leere hört."
- **Nutzen:** "Schmilz Layered-Roh-Enderit zu Enderit-Schrott. 4 Schrott, 4 Diamanten und 1 Netheritbarren ergeben den
  Barren. Am Schmiedetisch hebt die Enderit-Upgrade-Vorlage Netherit-Ausrüstung und -Behälter auf Enderit-Stufe. Rüstung
  dämpft Leere-Schaden und lässt dich beim Fallen gleiten. Außerdem: 9 Nuggets ergeben einen Barren, Trimm-Material, Hufeisen."
- **Geheim:** "Ein Nugget ist das Herz des Astral-Verzauberungstischs: beim Herstellen eingesetzt, verzauberst du mit
  Lohenstaub bis Stufe 50. Aus 8 Nuggets und einem Eimer wird der Enderit-Eimer. Brichst du den Tisch ab, kommt das Nugget
  zurück."
(Beispiel-Fakten im Konzept sind vor dem Befüllen gegen den Code zu prüfen; Astral-Tisch-Stand siehe Konzept-Doku und QUEUE Nachtrag 35.)

## 13. Beispiel Fremd-Mod (Fallback, Klasse Sonstiges, gewählt per Hash)
Item `othermod:void_pearl_shard`, Variante 3 von 8.
- EN Lore: "Over this object the stars keep silent. It was old before the first name was spoken, and it remembers every hand that
  ever held {name}."
- EN Use: "{name} answers only to those who bring patience. Whatever {mod} intended with it, it will not say; look to the
  recipes and the wise." (darunter Automatik: "Used in 2 recipes.")
- EN Secret: "Some say {name} is the last word of a dying lamp. Hold it under a moon of your choosing, and perhaps it will whisper."
- DE Lore: "Über diesen Gegenstand schweigen die Sterne. Er war alt, bevor der erste Name gesprochen wurde, und er erinnert sich
  an jede Hand, die {name} je hielt."
- DE Nutzen: "{name} antwortet nur dem Geduldigen. Was {mod} damit vorhatte, verrät es nicht; frag die Rezepte und die Weisen."
- DE Geheim: "Manche sagen, {name} sei das letzte Wort einer sterbenden Lampe. Halte es unter einen Mond deiner Wahl, vielleicht flüstert es."

## 14. Risiken / Prüfung
Textlänge pro Seite (Test per Schriftbreite), Hash-Stabilität, Lang-Fallback, Familien-Kollisionen (Einzel gewinnt),
Datenmenge (~900 x 6 Texte; lazy laden), Spieler-Entsiegelung (V2) serverseitig speichern, Forge/NeoForge-Menü-Registrierung wie
bei den anderen Modulen. Verifikation: Coverage-Test, `check_data.py`, Client-Screenshot (Tabs 1–3, Fallback-Item, leerer Slot).

## 15. Offene Fragen (max 4)
1. Name EN/DE bestätigen (Questionary Table / Fragentisch)? Eigenes Modul `simplequestionary` okay?
2. Geheimseite sofort sichtbar (Empfehlung) oder per Lapis-Siegel?
3. Rezeptmenge: 2 Lapis + Buch + 5 Bretter okay, oder teurer/billiger?
4. Vanilla vollständig in V1 oder zuerst nur Simple-Mods + Fallback, Vanilla in einer zweiten Welle?

## 16. Antworten Besitzer + Umsetzung V1 (Branch `claude-questionary`, 2026-10-11)
Besitzer: Name Questionary Table / Fragentisch; eigenes Modul `simplequestionary` (requires simplelib); Geheimseite sofort
sichtbar; Rezept 2 Lapis + 1 Buch + 5 Bretter (Shape aus 2.); V1 = Simple-Mods + Vanilla. Diese Runde nur die Mechanik,
Texte folgen später (einziges befülltes Beispiel: `simplebuilding:enderite_ingot`, Astral-Fakt korrigiert: 20 Schläge mit
dem Netherit-Vorschlaghammer auf den Zaubertisch, Enderit-Nugget in der Nebenhand).

Plan/Stand:
- Modul `modules/simplequestionary` nach Muster Simple Maps (fabric/neoforge/forge 26.3, Standalone-Targets, Forge-Target,
  Client-Smoke). Block `questionary_table` (Facing, Axt, 2.5), Modell aus Elementen, Texturen per
  `modules/simplequestionary/tools/gen_textures.py` (eigene Pixel-Art).
- Menü: 1 Slot (1 Stück, Shift-Klick), Inventar; Item kommt beim Schließen zurück. Bildschirm: Kasten links (simplelib
  `UiBoxes`), Buch rechts (2 Seiten, Textfluss links→rechts), 3 Lesezeichen (`UiBookmarks`), Vanilla-Seitenpfeile,
  Mausrad/A/D/Pfeiltasten, „n / 3“. Ohne Item: Willkommensseite.
- Daten: `assets/<ns>/questionary/<lang>.json` (Client-Reload-Listener, Fallback `en_us`), Einträge, `family:<name>` mit
  `items` (`#tag`, `regex:<pfad>`), Einzel-Eintrag gewinnt; `fallback.<block|tool|armor|food|misc>.<lore|use|secret>` je
  8 Varianten (FNV-1a über `id#seite`). Platzhalter `{name}`, `{mod}`, `{item:ns:id}`.
- Seite 2: Automatik-Zeile aus dem RecipeManager (Server berechnet beim Einlegen, Paket `facts` an den Client; Config
  `autoFacts` in `config/simplequestionary.json`).
- Abdeckung: `tools/questionary/check_data.py` (Bericht, rot nur bei Strukturfehlern; `--strict` für später) + GameTest
  `coverage` (zählt Lücken, nur Warnung). SB-Bündelung: Schalter „Enable Simple Questionary“ (`enableSimpleQuestionary`).
  Wiki: `questionary` je Item in den Wiki-Daten (Einzel-Einträge), Anzeige auf der Item-Seite.
- Später: Generator-Skelett (`_facts`), Texte für alle Items, BER-Aufblättern, simplelib-Buchbausteine (`BookPageWidget`).
