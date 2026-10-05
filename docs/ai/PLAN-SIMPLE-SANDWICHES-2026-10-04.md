# Plan Modul „Simple Sandwiches“ (Besitzer-Queue Nachtrag 8, 2026-10-04)

Status: **nur Konzept + Fragebogen**, kein Code. Quelle der Wünsche: `.claude/QUEUE.md` → „Besitzer 2026-10-04
(Nachtrag 8)“. Stellen, an denen der Besitzer entscheiden soll, sind mit **[Fx]** markiert (Fragebogen am Ende). Bis zur
Antwort gilt jeweils die markierte Empfehlung als Arbeitsannahme.

## 1. Ist-Zustand (Recherche)

**Modul-Anlage im Repo**
- `python tools/newmod.py simplesandwiches "Simple Sandwiches"` kopiert `tools/templates/module` (shared/, fabric/,
  neoforge/, forge/, clienttest/, tools/, wiki/), trägt den Eintrag in `modules/modules.json` (inkl. `tests`-Objekt,
  Pfade, Projekte `:modules:<id>:<loader>`) ein und schaltet das Modul in `integration/enabled-mods.json` frei.
  Ein neues Modul berührt nur `modules/<id>/` + Manifest (`docs/MULTIMOD.md`, „Plugin-style test registration“).
- Pflicht-Nacharbeit laut Generator: `modules/simplemoney/shared/resources/data/simplemoney/money/simplesandwiches/prices.json`
  (leere Preise erlaubt). Wiki-Prosa in `modules/<id>/wiki/manual.json` (EN/DE, jede Item-/Block-ID braucht eine Note),
  Balance-Ablage `balance/simplesandwiches/`, Datagen-Ausgabe `modules/<id>/generated/resources`.
- Vorlage für Struktur: `modules/simplefun` (Registry-Klassen in `shared/java/com/simplefun/registry`, Plattform-Services
  über `META-INF/services`, Loader-Einstiege `SimplefunFabric`/`SimplefunNeoForge`/`FunForge`, Mixin-Configs je Loader,
  GameTest-Adapter `ModuleGameTest` (Fabric), `ModuleNeoTests`, `ModuleForgeTests`, Client-Smoke).
- Module sind 26.3-only (Konvention `gradle/module-*.gradle` pinnt 26.3/Java 25) → **kein** 26.2-Zwilling und kein
  `McVersion`-Flag nötig (die Overlay-Regel gilt für den SimpleBuilding-Kern).
- Module dürfen keine Implementierungsklassen anderer Module importieren; Kopplung nur über öffentliche Registry-IDs,
  Vanilla-Schnittstellen oder `framework/` (z. B. `TransformHints` für die Hand-Neigung beim Messer).

**SimpleBuilding-Bezug**
- SB registriert `netherite_apple`, `enchanted_netherite_apple`, `enderite_apple`, `enchanted_enderite_apple`,
  `netherite_carrot`, `enderite_carrot` mit normalen Vanilla-Komponenten `FOOD` + `CONSUMABLE`
  (`common/src/shared/java/com/simplebuilding/items/ModItems.java`, Z. ~775–910). Ein generischer Leser dieser
  Komponenten deckt sie also ohne jede Code-Abhängigkeit ab.
- SB hat ein Bündel-Muster mit eigener Kapazität: `ReinforcedBundleItem extends BundleItem` (+ `QuiverItem`,
  `ReinforcedBundleTooltips`, `BundleTooltipComponentMixin`). Darf nicht importiert, aber als Muster nachgebaut werden.
- Kein vorhandenes Messer/Käse/Butter/Schneidebrett im Repo (git ls-files geprüft).

**Vanilla 26.3 (Client-Jar per `javap` geprüft)**
- `FoodProperties(int nutrition, float saturation, boolean canAlwaysEat)` – Sättigung ist ein **absoluter** Wert;
  `FoodData` deckelt Hunger auf 20 und Sättigung auf den aktuellen Hungerwert.
- `Consumable(float consumeSeconds, ItemUseAnimation, Holder<SoundEvent>, boolean particles, List<ConsumeEffect>)`;
  Effekte als `ApplyStatusEffectsConsumeEffect(List<MobEffectInstance>, float probability)` – Wahrscheinlichkeit gilt
  pro Eintrag (rohes Huhn: Hunger 30 %, verrottetes Fleisch: Hunger 80 %, Spinnenauge: Gift 100 %).
- `ItemStack`-Stapelbarkeit = gleiches Item **und** gleiche Komponenten → „nur gleiche Sandwiches stapelbar“ ergibt
  sich automatisch, wenn der Inhalt deterministisch als Komponente gespeichert wird.
- `BundleContents(List<ItemStackTemplate>)` (26.3: `ItemStackTemplate` statt ItemStack), Gewichtslogik `weight()`,
  `getSelectedItemIndex()`; `BundleItem` (überschreibbar: `overrideStackedOnOther`, `overrideOtherStackedOnMe`, `use`,
  `getBarWidth`, `getTooltipImage`, `onDestroyed`); Auswahl per `ServerboundSelectBundleItemPacket`;
  Item-Modell `minecraft:bundle/selected_item` + Bedingung `minecraft:bundle/has_selected_item`; Tooltip
  `ClientBundleTooltip` (Füllbalken nach Gewicht → bei 5 Stacks falsch, braucht eigene Anzeige, siehe §8).
- Kessel: `CauldronInteractions.EMPTY/WATER/LAVA/POWDER_SNOW` sind `CauldronInteraction.Dispatcher`; `Dispatcher` hat
  einen öffentlichen Konstruktor, aber `put(Item|TagKey, …)` ist **package-private** → Invoker-Mixin nötig (alle Loader).
- `CakeBlock.BITES` (0..6, `MAX_BITES`), `eat(...)` ist `protected static` – ein Bissen = 2 Hunger / 0,4 Sättigung.
- Item-Modelle: `minecraft:composite`, `minecraft:select` mit Property `minecraft:custom_model_data` (Index i →
  `strings[i]`), `minecraft:condition` (z. B. `custom_model_data`-Flag). `CustomModelData(floats, flags, strings, colors)`.
  → Mehrschicht-Sandwich-Textur ist **rein datengetrieben** möglich, ohne Client-Code pro Loader.
- `Weapon(int itemDamagePerAttack)`, `Tool`-Komponente (Regeln pro Block-Tag mit Abbaugeschwindigkeit) → Messer-
  Abbauvorteile (Spinnweben, Bambus) rein über Daten.

## 2. Modul-Rahmen

- ID **`simplesandwiches`**, Name „Simple Sandwiches“, Paket `com.simplesandwiches` **[F1]**.
- Loader: Fabric zuerst, dann NeoForge, Forge (`-Pforge263=true`). `requires: []`, `optional: ["simplebuilding", "jei"]`.
- Server-Config `simplesandwiches-server.json` mit harten Grenzen (Tabelle §11). Kein Bildschirmtext für Abläufe;
  Rückmeldung nur über Sound/Partikel/Block-Darstellung.
- Kreativ-Tab `simplesandwiches:kitchen`; Messer zusätzlich hinter der Schere (Werkzeuge), Lebensmittel hinter Brot.

## 3. Items und Blöcke (Übersicht)

| ID | Art | Kurz |
|---|---|---|
| `knife` | Item (Werkzeug) | Eisenstufe, Rezept siehe §6, 250 Haltbarkeit |
| `cutting_board` | Block + Block-Entity | Zubereitungsfläche, Zustandsmaschine §5 |
| `sandwich` | Item | Inhalt als Komponente, stapelbar nur bei gleichem Inhalt (max. 16) |
| `cheese_block` / `butter_block` | Block (+ Block-Item) | 16 Scheiben, schneidbar von oben/seitlich §7 |
| `cheese_slice` | Item (essbar) | Zutat, 2 Hunger / 1,2 Sättigung, 64 |
| `butter_slice` | Item (nicht essbar) | zum Bestreichen, 64 |
| `cake_slice` | Item (essbar) | aus Kuchen geschnitten **[F11]** |
| `milk_cauldron` | Block | Milch-Kessel mit Stufen für Butter/Käse §9 |
| `food_basket` | Item | Picknickkorb, 5 Stacks Essen wie Bündel §8 |

Optional später (nicht v1): Toast/Baguette **[F12]**, Holzarten-Varianten des Bretts **[F9]**, weitere Messerstufen **[F8]**.

## 4. Sandwich: Datenmodell, Werte, Effekte

### 4.1 Komponente
```
simplesandwiches:sandwich_contents = SandwichContents(
    List<Holder<Item>> ingredients,   // 0..5, Reihenfolge = Schichtfolge von unten
    boolean buttered)
```
- Gespeichert werden **nur Item-IDs** (keine Komponenten der Zutat). Werte/Effekte stammen aus den Standard-
  Komponenten des Zutat-Items (`item.components()`), nicht aus dem konkreten Stack → editierte/umbenannte Stacks
  bringen nichts (Exploit-Schutz), und gleiche Zutaten ergeben immer gleiche Sandwiches.
- Beim Zuklappen auf dem Brett berechnet der Server **einmalig** und schreibt deterministisch:
  `FOOD`, `CONSUMABLE`, `CUSTOM_MODEL_DATA` (Textur §10), `RARITY` (höchste Zutat), `ENCHANTMENT_GLINT_OVERRIDE`
  (wenn eine Zutat glitzert, z. B. verzauberter goldener Apfel), `USE_COOLDOWN` (größter Zutat-Cooldown).
  Vanilla übernimmt damit Essen, Effekte, Sound und Stapeln ohne Mixin.
- Gleiche Zutaten in anderer Reihenfolge = anderes Sandwich (andere Textur) → stapeln **nicht** miteinander.
  Annahme, weil die Textur die Schichtfolge zeigt; Alternative „sortieren“ wäre ein Einzeiler.
- Name: „Sandwich mit Käse, Steak, …“ (dynamisch über `getName`, Zutatennamen gekürzt ab 3: „… und 2 weitere“);
  Tooltip listet Zutaten, „Gebuttert“, Hunger/Sättigung und Effekte mit Wahrscheinlichkeit.
- Ein Sandwich ohne Zutat (nur Brot, ggf. gebuttert) ist erlaubt („Butterbrot“).

### 4.2 Zutaten
- Zulässig: Item-Tag `simplesandwiches:sandwich_ingredients` **[F2]**. Empfehlung: Tag wird per Datagen aus allen
  Vanilla-Items mit `FOOD` gefüllt, **ohne** Items mit `USE_REMAINDER`/Schüssel/Flasche (Eintöpfe, Honig, Milch),
  ohne Kuchen-Block, Chorusfrucht (Teleport) und Kugelfisch-Sonderfälle bleiben drin (Effekte werden übernommen).
  Enthalten u. a.: Kabeljau, Lachs (roh/gebraten), Kaninchen, Huhn, Hammel, Schwein, Steak/Rind, Kartoffel (roh/
  gebacken/giftig), Karotte, goldene Karotte, Apfel, goldener + verzauberter goldener Apfel, Melonenscheibe,
  Spinnenauge, verrottetes Fleisch, Rote Bete, Beeren, getrockneter Seetang, Keks + **Käsescheibe**.
- SimpleBuilding: optionale Tag-Einträge `{"id": "simplebuilding:netherite_apple", "required": false}` usw. (alle sechs
  Äpfel/Karotten). Ohne SB werden sie stillschweigend ignoriert – keine Code-Abhängigkeit, kein Registry-Problem.
- Brot ist keine Zutat, sondern die Basis (1 Brot = 1 Sandwich).

### 4.3 Hunger- und Sättigungsformel
```
N_roh = N(Brot)=5 + Σ N(Zutat)
S_roh = S(Brot)=6,0 + Σ S(Zutat)
Butter: N = round(N_roh × 1,10), S = S_roh × 1,10        (Faktor config 0..0,25, Standard 0,10)
Deckel:  N ≤ 20, S ≤ 20  (Config-Obergrenzen, hart ≤ 20)
canAlwaysEat = irgendeine Zutat hat canAlwaysEat (goldene Äpfel)
Esszeit = 1,6 s + 0,2 s je Zutat  (max. 2,6 s)
```
- Beispiel: Steak + Käse + Karotte = 5+8+2+3 = 18 Hunger / 6+12,8+1,2+3,6 = 23,6 → 20 Sättigung.
- Exploit-Betrachtung: Mehr als 20 Hunger auf einmal ist nicht verwertbar, deshalb der Deckel (sonst würden 5 Steaks in
  einem Item „verschwendet“ oder – je nach Mod-Kombination – über den Balken hinaus angerechnet). Der Mehrwert liegt in
  Inventar-Kompression (bis 6 Lebensmittel pro Item, max. 16 pro Stack) gegen Zubereitungszeit – so gewünscht.
  **[F4]** fragt, ob zusätzlich ein Abschlag gewünscht ist.

### 4.4 Effekt-Kombination **[F3]**
Empfehlung (Variante B „zusammenführen“):
1. Alle `ApplyStatusEffectsConsumeEffect` der Zutaten einsammeln; jede Effekt-Instanz behält ihre Wahrscheinlichkeit p.
2. Gleicher Effekt mehrfach → **eine** Instanz: Stärke = Maximum, Dauer = Summe, p = 1 − Π(1 − pᵢ).
   Beispiel: 2× rohes Huhn (Hunger 30 s, 30 %) → Hunger 60 s mit 51 %.
3. Butter: Dauer × 1,10 (nur positive Effekte? → Annahme: **alle**, wie vom Besitzer „verstärkt Effekte“ formuliert).
4. Deckel je Effekt: Dauer ≤ min(2 × längste Einzeldauer, 12 000 Ticks = 10 min), Stärke ≤ höchste Einzelstärke
   (keine neuen Stufen durch Stapeln). Instant-Effekte (Heilung) nur Stärke-Max, keine Dauer.
5. Andere Konsum-Effekte: `remove_status_effects`/`clear_all_status_effects` werden übernommen (je einmal),
   `teleport_randomly` (Chorus) und `play_sound` nicht.
- Variante A (getrennt würfeln, Vanilla-`addEffect` nimmt länger/stärker) und C (Stärken addieren) siehe Fragebogen.

## 5. Schneidebrett: Block-Entity-Zustandsmaschine

Block `cutting_board`: flach (2 px hoch), horizontal ausgerichtet, wasserbeständig nein, Axt-Werkzeug, Holz-Sound.
Block-Entity hält `state`, `bread` (ItemStack, 1), `ingredients` (≤ 5 ItemStacks à 1), `buttered`, `sandwich`
(fertiger Stack im Zustand CLOSED). Sync per Update-Packet; Renderer legt die Teile flach/gestapelt aufs Brett
(Muster: SBs `PlacedBundleRenderer`, 26.3-Submit-API).

| Zustand | Aktion | Ergebnis |
|---|---|---|
| EMPTY | Rechtsklick mit Brot | 1 Brot aufs Brett → LOAF |
| EMPTY | Rechtsklick mit Sandwich | Sandwich aufs Brett → CLOSED (zum Ändern) |
| LOAF | Messer | aufschneiden → OPEN (Schneide-Sound, Krümel-Partikel, 1 Haltbarkeit) |
| LOAF | leere Hand | Brot zurück → EMPTY |
| OPEN, keine Zutat, ungebuttert | Messer + Butterscheibe in der anderen Hand | Butter (1 Scheibe) → `buttered` |
| OPEN | Rechtsklick mit Zutat aus Tag | 1 Stück obenauf, solange < 5 (voll → „Fehl“-Sound, nichts passiert) |
| OPEN | Butter nachdem schon Zutaten liegen | abgelehnt (Butter muss zuerst) |
| OPEN | Messer (ohne Butter in der anderen Hand) | oberste Zutat zurück in die Hand/ins Inventar (LIFO) |
| OPEN | leere Hand | zuklappen → CLOSED (berechnet Sandwich-Komponenten §4.1) |
| CLOSED | leere Hand | Sandwich ins Inventar → EMPTY |
| CLOSED | Messer | wieder öffnen → OPEN (Zutaten, Butter bleiben erhalten) |
| beliebig | Block abbauen | droppt Brot/Sandwich + Zutaten; aufgestrichene Butter verfällt |

- Offenes Brot ohne Zutaten + Messer: Annahme → nichts (kein Zurück zum Laib; aufgeschnitten bleibt aufgeschnitten).
- Butter ist optional, aber nur als erster Schritt möglich (Besitzer-Text „muss zuerst geschmiert werden“ so gelesen).
- Schleichen + Rechtsklick mit Zutat legt nicht ab (normales Block-Platzieren bleibt möglich).
- Hand-Hinweis: `TransformHints.register("simplesandwiches:knife", …)` mit derselben Prüfmethode wie die Server-Aktion.
- Hopper/Automation: nicht in v1 (bewusst, Zustandsmaschine ist spielergeführt).

## 6. Messer

- Rezept (geformt, Eisennuggets N, Stock S):
  ```
  . . N
  . N N
  S N .
  ```
  (Stock unten links; Nuggets Mitte, rechts oben, unten Mitte, rechts Mitte – Besitzer-Vorgabe.)
- Werte: Angriffsschaden gesamt 2 (Eisenschwert 6 → ~1/3), Angriffstempo 2,0, `Weapon(1)`, Haltbarkeit 250,
  Reparatur mit Eisenbarren im Amboss, verzauberbar wie Schere (Effizienz, Haltbarkeit, Reparatur) **[F8]**.
- Wirkungen je Ziel (je 1 Haltbarkeit):
  - Brot auf dem Brett aufschneiden / Zutat zurücknehmen / zugeklapptes Sandwich wieder öffnen (§5).
  - Butter schmieren: Butterscheibe in der anderen Hand (§5).
  - Käse-/Butterblock: Rechtsklick schneidet 1 Scheibe ab (§7).
  - Melone (Block): Rechtsklick → Block verschwindet, **9** Melonenscheiben (= Crafting-Gegenwert, kein Dupe;
    normales Abbauen bleibt bei 3–7).
  - Kuchen (platziert): Rechtsklick → 1 Bissen weniger (`BITES`+1, letzter entfernt den Block), gibt 1 `cake_slice`.
    Kerzenkuchen: Kerze droppt, dann wie Kuchen.
  - Spinnweben: `Tool`-Regel Geschwindigkeit 15 (wie Schwert), Drop Faden (Vanilla-Loot, da keine Schere).
  - Bambus: `Tool`-Regel Geschwindigkeit wie Schwert (sofort).
  - Sonst: normales Item, schwacher Nahkampf.

## 7. Käse- und Butterblock (16 Scheiben)

- Blockstate: `slices` 1..16 (16 = voll) + `cut_from` ∈ {up, north, south, east, west}. Jede Scheibe ist 1 px:
  von oben geschnitten wird der Block 1 px niedriger, von der Seite 1 px schmaler auf dieser Seite. Die erste
  Schnittrichtung wird festgehalten, solange der Block nicht voll ist (sonst sind Formen nicht als Quader darstellbar).
  Seitlich angeschnitten → nur noch von derselben Seite schneidbar; von oben → nur von oben.
- 5 × 15 Teilmodelle + 1 Vollmodell, per Datagen erzeugt (Quader `from/to`), Schnittfläche mit eigener „Innen“-Textur.
- Zurücklegen: Rechtsklick mit passender Scheibe auf angeschnittenen Block → +1 (gleiche Richtung, bis 16).
- Abbauen: voll → Block-Item; angeschnitten → verbleibende Scheiben als Items (kein Verlust, kein Gewinn).
  Scheiben stapeln zu 64; kein Crafting 16→Block (Werkbank hat nur 9 Felder; Zurücklegen ersetzt es).
- Butterblock: Reibung **0,9** (Normal 0,6, Eis 0,98, Blaueis 0,989) **[F7]**; Sound weich (Honig-/Schleimblock-
  Klasse). Käseblock: normale Reibung, Woll-ähnlicher Sound. Beide Axt/Hand, Härte 0,5.

## 8. Essenskorb (Picknickkorb)

- Item `food_basket` als eigene `BundleItem`-Unterklasse (Muster wie SB `ReinforcedBundleItem`, nachgebaut, nicht
  importiert). Speichert in Vanilla-`BUNDLE_CONTENTS` → Auswahl-Packet, Mausrad-Wechsel, `bundle/selected_item`-
  Darstellung und Raster-Tooltip gibt es gratis.
- Kapazität: **5 Stacks** statt Gewicht 64: Einfügen füllt zuerst vorhandene gleiche Stacks bis zu deren Max-Größe,
  sonst neuer Platz, solange < 5 Plätze. Nur Items mit `FOOD` (inkl. Sandwiches, Kuchenstücke), keine Bündel/Körbe.
  **[F10]** (gemischt vs. gleichartig).
- Oberstes (= ausgewähltes) Item sichtbar im Inventar-Icon (offen) und in der Hand (Deckel-Modell + Item obenauf);
  Rechtsklick in der Hand isst das ausgewählte Item (Esszeit/Animation/Effekte dessen `CONSUMABLE`/`FOOD`), zieht 1 ab;
  Schleich-Rechtsklick in der Luft wechselt die Auswahl (zusätzlich zum Mausrad im Inventar). Ausschütten wie Bündel
  bleibt (Vanilla-`use` bei Schleichen? → Annahme: Ausschütten nur per Inventar-Rechtsklick, Hand-Rechtsklick = essen).
- Tooltip: Vanilla-Raster; Füllbalken/„voll“-Anzeige über eigene Berechnung (belegte Plätze / 5) – dafür ein kleiner
  Client-Mixin auf `ClientBundleTooltip` bzw. eigene `TooltipComponent` (Muster SB `ReinforcedBundleTooltips`).
  Item-Balken (`getBarWidth`) ebenfalls nach Plätzen.
- Zerstört (Lava, Kaktus) → Inhalt droppt (Vanilla `onDestroyed`).
- Rezept (Vorschlag, im Fragebogen [F10] mit abgefragt): `S . S / W B W / W W W` (S Stock, W Weizen, B Bündel).

## 9. Butter und Käse im Kessel

Neuer Block `milk_cauldron` (`AbstractCauldronBlock`-Unterklasse, im Tag `minecraft:cauldrons`), eigener `Dispatcher`:
- Milcheimer auf leeren Kessel → `milk_cauldron` (stage `milk`), Eimer wird leer. Registrierung über Invoker-Mixin auf
  `CauldronInteraction$Dispatcher#put` an `CauldronInteractions.EMPTY` (gemeinsame Mixin-Config, alle drei Loader).
- Leerer Eimer auf `milk` → Milcheimer zurück, Kessel leer (kein Verlust).
- **Butter [F5]** (Empfehlung): 8× Rechtsklick mit Stock (Rühren, 10 Ticks Mindestabstand pro Klick, Spritz-Partikel)
  → stage `butter`; leere Hand → 1 `butter_block`, Kessel leer.
- **Käse [F6]** (Empfehlung): 1 fermentiertes Spinnenauge (Lab-Ersatz) → stage `curdling`; nach 6000 Ticks (5 min,
  geplanter Block-Tick, chunkgeladen) → stage `cheese`; leere Hand → 1 `cheese_block`.
- Blockstate `stage` ∈ {milk, churning_1..7 (intern Zähler im State), butter, curdling, cheese}; Inhalt-Texturen
  Milch/Butter/Quark/Käse-Laib. Kein Block-Entity nötig (Zähler im State, Reifung per scheduled tick).
- Ausbeute: 1 Milcheimer → 1 Block → 16 Scheiben. Werte in Config (Rührzahl 1..32, Reifezeit 200..72 000 Ticks).

## 10. Dynamische Item-Textur (Sandwich)

- Server schreibt beim Zuklappen `CUSTOM_MODEL_DATA`: `strings = [layer0..layer4]` (Visual-Schlüssel je Zutat),
  `flags = [buttered]`.
- `assets/simplesandwiches/items/sandwich.json`: `minecraft:composite` aus
  1. Unterseite Brot (`condition` Flag 0 → gebutterte Variante),
  2. fünf `minecraft:select` auf `custom_model_data` Index 0..4; `cases` je Visual-Schlüssel → Modell
     `item/sandwich/layer_<pos>_<visual>`; Fallback leer,
  3. Oberseite Brot – Position abhängig von Zutatenzahl → sechste `select` auf `strings[5]` = Anzahl („0“..„5“).
- Seitenansicht 16 × 16: Brot unten 2 px, je Zutat 2 px Schicht, Brot oben 2 px (max. 14 px hoch).
- Visual-Schlüssel statt Item-ID: Zutaten werden auf ~16 Bildgruppen abgebildet (rohes Fleisch, gebratenes Fleisch,
  Fisch roh/gebraten, Huhn, Kartoffel, Karotte, Apfel, goldener Apfel, Melone, Käse, Spinnenauge, verrottetes Fleisch,
  Grünzeug/Beeren, Netherit-Apfel, Enderit-Apfel) + `generic` für unbekannte/Mod-Zutaten. Zuordnung als Datenmap
  `data/simplesandwiches/sandwich_visuals.json` (Item → Schlüssel), SB-Einträge optional.
- Texturen pro Gruppe × 5 Positionen erzeugt der Generator (`tools/textures/sandwiches.py`) automatisch aus je einem
  Schicht-Motiv → 80 + Basis-PNGs, kein Handzeichnen pro Position.
- Vorteil: komplett Vanilla-Ressourcen, gleich auf allen Loadern, kein Client-Code; Texturpakete können es überschreiben.
- Das Brett rendert dieselben Schichten als 3D-Stapel (Block-Entity-Renderer).

## 11. Server-Config (harte Grenzen)

| Schlüssel | Standard | Grenzen |
|---|---|---|
| `maxIngredients` | 5 | 1..5 |
| `nutritionCap` / `saturationCap` | 20 / 20 | 1..20 |
| `butterBonus` | 0,10 | 0..0,25 |
| `effectDurationCapTicks` | 12 000 | 20..12 000 |
| `effectDurationMultiplierCap` | 2,0 | 1..3 |
| `sandwichStackSize` | 16 | 1..64 (nur bei Neustart, Komponente am Item) |
| `churnClicks` | 8 | 1..32 |
| `cheeseRipeningTicks` | 6000 | 200..72 000 |
| `butterFriction` | 0,9 | 0,6..0,98 (nur Neustart, Blockeigenschaft) |
| `basketStacks` | 5 | 1..5 |

Werte als benannte Konstanten + JSON, damit die Balancing-Zentrale sie lesen kann (`balance/simplesandwiches/`).

## 12. Multiloader

- Gemeinsamer Code in `modules/simplesandwiches/shared/java/com/simplesandwiches` (Registries, Komponente mit Codec +
  StreamCodec, Block-Entity, Logik, Rezepte per Datagen). Loader-Schicht nur: Registrierungszeitpunkt (Fabric direkt,
  NeoForge/Forge `RegisterEvent`), Block-Entity-Renderer-Registrierung, Kreativ-Tab-Einhängen, Config-Screen (ModMenu/
  NeoForge-Config-Screen wie simplefun).
- Mixins (gemeinsam): `Dispatcher#put`-Invoker; Client: Korb-Tooltip-Füllbalken. Alles andere über überschriebene
  Item-/Block-Methoden (`useItemOn`, `useWithoutItem`, `overrideStackedOnOther` …) – keine Loader-Events nötig.
- Optionale SB-Inhalte nur über `required:false`-Tag-Einträge und Visual-Map; keine Klasse aus SB.
- Forge: Bedingungen für optionale Rezepte wie simplefun (`FunCondition`), falls [F8] B gewählt wird.

## 13. Tests (GameTests je Loader + Client-Smoke)

Fabric `ModuleGameTest`, NeoForge `ModuleNeoTests`, Forge `ModuleForgeTests` (gleiche IDs):
- `sandwich_contents_deterministic_stacking` – gleicher Inhalt stapelt, andere Reihenfolge/Butter nicht.
- `sandwich_food_sum_and_caps`, `sandwich_butter_bonus_ten_percent`, `sandwich_can_always_eat_from_golden_apple`.
- `sandwich_effect_merge_probability_and_caps` (2× rohes Huhn → 51 %, Dauer-Deckel, keine Stärke-Erhöhung).
- `sandwich_ignores_ingredient_stack_components` (umbenannter/editierter Stack ändert nichts).
- `board_full_cycle` (Brot → Messer → Butter → 5 Zutaten → 6. abgelehnt → Messer LIFO → zu → nehmen → wieder öffnen),
  `board_butter_only_first`, `board_break_drops_contents`.
- `knife_cake_slices` (7 Stück, Block weg), `knife_melon_nine_slices`, `knife_cobweb_drops_string`,
  `knife_bamboo_tool_speed`, `knife_recipe_shape`.
- `cheese_block_sixteen_slices_top_and_side`, `cheese_block_partial_drop`, `butter_block_friction`.
- `milk_cauldron_bucket_roundtrip`, `milk_cauldron_churn_to_butter`, `milk_cauldron_cheese_ripening`.
- `basket_five_stacks_limit`, `basket_food_only`, `basket_eat_selected_decrements`, `basket_destroy_drops`.
- `optional_sb_tag_entries_resolve_without_sb`, `creative_tab_contents`, `models_and_lang_present`.
- Client-Smoke: Sandwich mit 0/3/5 Schichten, Brett mit Inhalt, Korb offen, Käseblock angeschnitten (Screenshots).
- Gates: `module-simplesandwiches-fabric-263`, `-neoforge-263`, Forge-Compile, `tools/multimod.py`, Wiki `--all --check`,
  `checkBalance`, `modules/simplesandwiches/tools/check_data.py`, Simple-Money-Preisdatei.

## 14. Texturen (Pixel-Art, Generator `tools/textures/sandwiches.py`, Vorschau `previews/sandwiches-vorschau.png`)

Messer (Item), Schneidebrett (oben/Seite/unten + Item), Käseblock (oben/Seite/Schnittfläche/Item), Butterblock (ebenso),
Käsescheibe, Butterscheibe, Kuchenstück, Sandwich-Basis (Brot unten/oben, gebuttert), 16 Schicht-Motive + generic
(× 5 Positionen automatisch), Milch-Kessel-Inhalte (Milch, Rührschaum, Butter, Quark, Käse), Essenskorb (zu, offen
hinten/vorn für Auswahl-Darstellung). Stil vanilla-nah; erst 2–3 Varianten als Vorschau (A/B/C), Besitzer wählt.

## 15. Phasen und Aufwand (Schätzung in Helfer-Sitzungen)

| Phase | Inhalt | Aufwand |
|---|---|---|
| P0 | `newmod.py`, Manifest, Money-Preise, Wiki-Gerüst, Config | 0,5 |
| P1 | Komponente, Formel, Effekt-Merge, Sandwich-Item, Tests (§4) | 1 |
| P2 | Messer + Schneidebrett (BE, Renderer, Zustandsmaschine), Kuchen/Melone/Spinnweben/Bambus | 1,5 |
| P3 | Käse-/Butterblock (16 Scheiben, Datagen-Modelle), Milch-Kessel | 1 |
| P4 | Essenskorb (Bündel-Unterklasse, Tooltip-Mixin) | 1 |
| P5 | Texturen-Generator, Vorschauen, Besitzerwahl, Item-Modell-Datagen | 1 |
| P6 | NeoForge/Forge-Abgleich, Client-Smoke, Wiki/Lang/Balance, alle Gates | 1 |

Summe ≈ 7 Sitzungen. GPT-geeignet (Low-Level): P0, Lang/Wiki-Noten, Datagen-Modelle der 16-Scheiben-Blöcke, Tag-Listen.

## 16. Risiken

- Gespeicherte `FOOD`/`CONSUMABLE` an alten Sandwiches ändern sich nicht bei Config-Änderung → alte und neue Stacks
  mit gleichem Inhalt stapeln dann nicht. Gegenmittel: Komponente trägt `formulaVersion`; beim Aufnehmen/Inventar-Tick
  neu berechnen, wenn abweichend (in P1 entscheiden, Test dazu).
- `ClientBundleTooltip` ist auf Gewicht 1 ausgelegt → eigener Füllbalken nötig (Mixin, Versionsbruch-Risiko).
- Invoker auf package-private `Dispatcher#put`: bei Vanilla-Änderung bricht der Mixin laut (gut erkennbar).
- 80+ Schichttexturen: nur generiert sinnvoll; Besitzer-Feedback „eigene Formen statt Recolors“ beachten (Motive je
  Gruppe eigen zeichnen, Positionen generieren).
- Stapel-16-Sandwiches mit 20 Hunger: starke Inventar-Kompression – bewusst gewünscht, über `sandwichStackSize` regelbar.

## 17. Fragen an den Besitzer

(Empfehlung jeweils mit ★)

1. **Modulname/ID?** ★A `simplesandwiches` „Simple Sandwiches“ · B `simplekitchen` „Simple Kitchen“ (Platz für
   späteres Kochen) · C `simplefood` „Simple Food“.
2. **Welche Zutaten?** A nur deine Liste (Fisch, Fleisch, Kartoffel, Karotte, Apfel, Melone, Spinnenauge, verrottetes
   Fleisch, goldene Früchte, Käse) · ★B alles Essbare ohne Schüssel/Flasche (inkl. Beeren, Rote Bete, Keks), SB-Äpfel/
   -Karotten automatisch · C wie B plus Essen anderer Mods automatisch.
3. **Gleicher Effekt mehrfach im Sandwich?** A getrennt würfeln, stärkster/längster gewinnt (wie mehrmals essen) ·
   ★B zusammenführen: Dauer addiert (max. doppelt bzw. 10 min), Stärke = Maximum, Chance kombiniert · C Stärken
   addieren (2 goldene Äpfel = Regeneration III, gedeckelt).
4. **Hunger/Sättigung?** ★A Summe inkl. Brot, gedeckelt auf 20/20 · B Summe mit Abschlag (−10 % je Zutat ab der
   dritten) · C Summe ohne Deckel.
5. **Butter im Kessel?** ★A Milcheimer in Kessel, 8× mit Stock rühren · B Milch + Zucker, dann rühren · C Milch
   reift von selbst (Zeit) zu Butter.
6. **Käse im Kessel (Zutat + Reifezeit)?** ★A Milch + fermentiertes Spinnenauge, 5 min · B Milch + brauner Pilz,
   5 min · C Milch ohne Zutat, 1 Ingame-Tag (20 min) · D wie A, aber nur über Lagerfeuer/Feuer.
7. **Butter-Rutschigkeit?** A 0,8 (wie Schleim, kaum rutschig) · ★B 0,9 (deutlich, weniger als Eis) · C 0,95 (fast Eis).
8. **Messer-Ausbau?** ★A nur Eisen, Reparatur mit Eisenbarren, Effizienz/Haltbarkeit/Reparatur · B wie A plus
   Netherit-/Enderit-Messer (nur mit SimpleBuilding herstellbar, schneller/haltbarer) · C volle Reihe Holz bis Netherit.
9. **Schneidebrett-Varianten?** ★A ein Brett, Rezept aus beliebigen Holzstufen (2 Stufen + Stock) · B je Holzart ein
   eigenes Brett (12 Varianten) · C Holz + Stein-Variante.
10. **Essenskorb-Inhalt?** ★A 5 Stacks gemischt (beliebiges Essen), Rezept Bündel + Weizen + Stöcke · B 5 Stacks nur
    einer Sorte · C nur Sandwiches, dafür 9 Stacks.
11. **Kuchenstück?** ★A 2 Hunger/0,4 Sättigung (= ein Kuchenbissen, 7 pro Kuchen), auch Sandwich-Zutat · B gleich,
    aber keine Zutat · C 3/1,2 als Messer-Bonus.
12. **Brot-Varianten?** ★A v1 nur normales Brot · B zusätzlich Toast (Brot im Räucherofen, +1 Sättigung) · C zusätzlich
    Toast und Baguette (Baguette = 2 Sandwiches-Platz, 8 Zutaten).

## 18. Getroffene Arbeitsannahmen (ohne Frage)

Stapelgröße Sandwich 16; Reihenfolge der Zutaten zählt (eigene Textur); Butter optional, nur als erster Schritt;
1 Brot = 1 Sandwich; Esszeit 1,6 s + 0,2 s/Zutat; Käsescheibe essbar 2/1,2, Butterscheibe nicht essbar; Melone mit
Messer = 9 Scheiben; Spinnweben mit Messer droppen Faden; angeschnittene Blöcke droppen Restscheiben; kein Hopper am
Brett; erste Schnittrichtung am Käse-/Butterblock wird festgehalten.

## 19. Entscheidungen 2026-10-04 (Besitzer-Antworten, gehen allen Abschnitten oben vor)

| Frage | Antwort | Umsetzung |
|---|---|---|
| F1 | A | ID `simplesandwiches`, „Simple Sandwiches“, Paket `com.simplesandwiches` (Loader-Einstiege/Test-Adapter im Template-Paket `com.simplebuilding.modules.simplesandwiches`). |
| F2 | B | Tag `simplesandwiches:sandwich_ingredients` = jedes Vanilla-Item mit `FOOD` ohne `USE_REMAINDER` (Schüssel/Flasche) außer Brot (Basis), plus `cheese_slice`, `cake_slice`, SB-Äpfel/-Karotten als `required:false`. Code prüft zusätzlich `FOOD` vorhanden + kein `USE_REMAINDER` (Datapack-Fehler können nichts kaputt machen). GameTest prüft Vollständigkeit gegen die Registry. |
| F3 | B | Zusammenführen: Stärke = Maximum, Dauer = Summe (Deckel min(2 × längste Einzeldauer, 12 000 Ticks)), p = 1 − Π(1 − pᵢ). |
| F4 | C | Hunger/Sättigung = Brot + Σ Zutaten, **kein Deckel** (FoodData deckelt beim Essen ohnehin auf 20). Config-Schlüssel `nutritionCap/saturationCap` entfallen. |
| F5 | C | Butter: Milcheimer in leeren Kessel → Milch reift **von selbst** zu Butter. Standard 3000 Ticks (2,5 min), Config `butterTicks` 200..24 000. |
| F6 | (frei) | Käse: Milch + **fermentiertes Spinnenauge** (Lab-Ersatz, Plan-Empfehlung A) → reift deutlich länger (Standard 9000 Ticks = 7,5 min, Config `cheeseTicks` 600..72 000). Danach **Erntefenster** (Standard 1800 Ticks = 1,5 min, Config `cheeseHarvestWindowTicks` 200..12 000): wer den Käse nicht rechtzeitig mit leerer Hand entnimmt, findet **verdorbene Milch** (grünlich, Rauch-/Myzel-Partikel), die mit leerer Hand oder Eimer nur noch ausgeleert werden kann (gibt nichts). Butter verdirbt nicht. Begründung: „wie Butter nur länger, man muss es richtig timen“ – gleiche Mechanik, längere Zeit, Timing durch das Fenster; die Zutat beim Start trennt beide Wege eindeutig (ohne Zutat = Butter, kein Risiko, Butter niemals versehentlich verdorben). |
| F7 | B | Butterblock-Reibung 0,9 (Konstante, Block-Eigenschaft). |
| F8 | A* | Nur Eisenmesser; Reparatur im Amboss mit **Eisennuggets** (Tag `simplesandwiches:knife_repair_materials`). Rezept laut Besitzer: Stock unten links; vier Nuggets Mitte-Mitte, rechts oben, unten Mitte und rechts Mitte (`..N/.NN/SN.`). Gesamtschaden 2 (≈ 1/3 Eisenschwert 6), Tempo 2,0, Haltbarkeit 250, verzauberbar: Effizienz (Tag `enchantable/mining`), Haltbarkeit + Reparatur (`enchantable/durability`). |
| F9 | B | 13 Bretter (alle Vanilla-Holzarten in 26.3, inkl. der neuen Pappel `poplar`): `oak, spruce, birch, jungle, acacia, dark_oak, mangrove, cherry, pale_oak, poplar, bamboo, crimson, warped` → `<holz>_cutting_board`, je Rezept 2 Holzstufen + Stock (`X##` – Stock links, Stufen rechts), Textur aus den Planken abgeleitet. Ein gemeinsamer Block-Entity-Typ. |
| F10 | neu | **Kein Essenskorb.** Stattdessen Vanilla-Bündel (alle Bündelfarben, `BundleItem`) per Mixin: Ist das oberste Item (das, welches `removeOne` als nächstes liefert = Auswahl, sonst Index 0) essbar (`FOOD` + `CONSUMABLE`, **kein** `USE_REMAINDER` → Eintöpfe/Honig ausgeschlossen), dann isst Rechtsklick-Halten dieses Item: Esszeit/Animation/Sound/Partikel des Items, Hunger/Effekte über dessen `Consumable.onConsume`, 1 Stück wird aus dem Bündel entfernt. Satt → nichts passiert (wie Essen). Nicht-Essen oben → Vanilla-Verhalten (Ausschütten). Config `bundleEating` (an/aus, Standard an). |
| F11 | (frei) | Kuchenstück: 2 Hunger / 0,4 Sättigung (Vanilla `CakeBlock.eat`: `eat(2, 0.1F)` → 2 × 0,1 × 2), **sofort** gegessen (`consumeSeconds` 0, wie ein Bissen), Stapel 7. Messer auf Kuchen: 1 Stück je Klick, ein voller Kuchen = 7 (Vanilla `BITES` 0..6, `MAX_BITES` 6), angeschnittener Kuchen = Restbissen, Kerzenkuchen → Kerze droppt, dann wie voller Kuchen. Bleibt Zutat. |
| F12 | A | Nur normales Brot. Offener Punkt (nicht v1): schwerer zu farmende Weizen-/Brotvarianten. |

Weitere Festlegungen beim Bau (eigene Entscheidungen):
- Ressourcen-„Datagen“ für das Modul als Python-Generator `modules/simplesandwiches/tools/gen_resources.py` → `generated/resources`
  (Module haben keine Fabric-Datagen-Kette; SimpleFun/SimpleRiding legen `generated/` ebenfalls fertig ab). `check_data.py`
  prüft, dass der Generator nichts ändern würde.
- Visual-Zuordnung Zutat → Schichtbild als Java-Konstante (`SandwichVisuals`), Unbekanntes → `generic` (statt Datenmap;
  spart je Loader einen Reload-Listener).
- Config als eigene JSON-Datei `config/simplesandwiches-server.json`, jeder Wert beim Laden auf harte Grenzen geklemmt
  (kein Cloth-Config-Zwang).
- Sandwich-Komponente trägt `formula` (Versionszahl); v1 = 1. Neuberechnung alter Stacks ist nicht v1 (offener Punkt).
- Sandwich-Stapelgröße 16 fest (Item-Eigenschaft).
- Bündel-Essen: beim Ende wird die oberste Position erneut geprüft; hat sich das Item geändert, passiert nichts.

## 20. Umsetzungsstand 2026-10-04 (Branch `claude-sandwiches`)

Umgesetzt (Code `modules/simplesandwiches/shared/java/com/simplesandwiches/`): Komponente + Formel + Effekt-Merge
(`sandwich/`), 13 Bretter mit Block-Entity, Zustandsmaschine und Renderer, Eisenmesser (Kuchen per Mixin
`CakeKnifeMixin`/`CandleCakeKnifeMixin`, Melone per `useOn`, Spinnweben/Bambus per `Tool`-Regeln des Schwerts),
Käse-/Butterblock (16 Scheiben), Milchkessel (Dispatcher-Invoker für Milcheimer → leerer Kessel), Bündel-Essen
(`BundleEatMixin` + `BundleEating`), Kuchenstück, Server-Config mit Klemmung, Kreativ-Tab `kitchen`, Hand-Hinweis
`simplesandwiches:knife` über `TransformHints`. Loader: Fabric, NeoForge (GameTests), Forge (nur kompiliert).
Ressourcen: `tools/gen_resources.py` (Datagen-Ersatz, `--check`), Texturen `tools/textures/sandwiches.py`
(Vorschau `previews/sandwiches-vorschau.png`).

Abweichungen gegenüber den Abschnitten oben (bewusst):
- Kein Essenskorb (F10), keine Holz-unabhängige Brettvariante (F9 = B), kein Rühren (F5 = C), kein Deckel (F4 = C).
- Pappel (`poplar`) als 13. Holzart, weil 26.3 sie als normale Holzart hat („alle Vanilla-Holzarten“).
- Korrektur 2026-10-05: Messerrezept mit vier Nuggets (`..N/.NN/SN.`); die urspruengliche Auslegung mit zwei Nuggets war falsch.
- Ressourcen-Datagen als Python statt Fabric-Datagen; Visual-Zuordnung als Java-Konstante.
- Bündel-Essen: Eimer-Items mit FOOD (26.3: Kugelfisch-Eimer) sind weder Zutat noch aus dem Bündel essbar.
- Forge-GameTests nicht verdrahtet (Manifest hat kein Forge-Testziel); nur Compile.
