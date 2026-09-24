# In-Game-Bücher – Konzept

Stand 2026-09-24. **Nur Konzept, kein Code.** Grundlage: der Buch-Entwurf dieser Sitzung samt
Gegenprüfung. Alle Fakten wurden gegen den Code, die generierten Daten und die Vanilla-Jars beider
Linien (26.2, 1.21.11) geprüft. Wo die Gegenprüfung den Entwurf korrigiert hat, gilt hier die
korrigierte Fassung.

---

## 1. Die Idee

- Ein **Einstiegsbuch** (die *Fibel*) erklärt, wie die Bücher funktionieren, und enthält die
  Rezepte der anderen Bücher – als Rätsel.
- **Themenbücher** für Verstärkung/Redstone-Maschinen, Werkzeuge, Lagerung (inkl. kommendem
  Rucksack), Enderit & Ende, Verzauberungen und Rüstungsbesätze.
- Jedes Buch entsteht aus **Buch + Schlüsselitem** (z. B. Kolben). Das Schlüsselitem ist selbst
  schon ein Hinweis auf den Inhalt.
- Die Bücher **teasen, statt das Wiki zu wiederholen**: Rätsel, Andeutungen, Randnotizen. Wer jede
  Zahl will, findet sie im Wiki; wer spielen will, soll selbst draufkommen.

## 2. Schreibregeln (für jede Seite, beide Linien)

1. **Jeder Satz muss im Code stimmen – auf beiden MC-Linien.** Dieselbe Regel wie im Wiki. Jedes
   Kapitel führt eine Quellenliste, und zwar **je Linie** (26.2-Pfad und 1.21.11-Pfad; Beispiel:
   der Maurer-Handel liegt auf 26.2 in
   `src/main/resources/data/simplebuilding/villager_trade/mason/2/emerald_copper_core.json`, auf
   1.21.11 in `mc1_21_11/shared/java/com/simplebuilding/trade/ModTradeDefinitions.java`).
2. **Erst das Rätsel, später die Bestätigung.** Ein versiegeltes Kapitel zeigt das Rätsel (plus
   optionalen Hover-Hinweis). Ist es enthüllt, kommt eine *Randnotiz* mit **genau einem**
   zusätzlichen Geheimnis dazu – nie die ganze Mechanik.
3. **Nie ein vollständiges Rezept.**
   - Geformte Rezepte: entweder die Form weglassen, oder die Form nennen und eine Zutat
     verschweigen.
   - Formlose Rezepte: **mindestens eine Zutat oder eine Anzahl bleibt verborgen** (hier gibt es
     keine Form, die man weglassen könnte).
   - Höchstens eine Zahl pro Seite, keine Werte-Tabellen.
4. **Nichts Wirkungsloses als wirksam anpreisen.** Heute wirkungslos oder anders als ihr Name:
   Abdeckung (*Cover*) und Brücke (*Bridge*) (festgenagelt durch den Gametest
   `coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown`), Linear (beschleunigt nur die Ringe,
   keine Linie), Reichweite (*Range*) am Magneten, Kinetischer Schutz gegen anderes als
   `fly_into_wall`, die Oktant-Optionen Hohl/Schicht/Reihenfolge, Kupfer als Besatzmaterial. Ein
   **ehrlicher** Tease darüber ist erlaubt.
5. **Hartkodierte englische UI-Texte nicht zitieren**, als wären sie übersetzt (Trichterfilter,
   Erzdetektor-Modi, Oktant-HUD, Luftsprung-Balken, Rahmen-Meldungen). Die deutsche Seite
   umschreibt sie.
6. **Seitenbudget.** Das Vanilla-Buch zeichnet höchstens **14 Zeilen à 114 px** und schneidet den
   Rest **stillschweigend** ab (26.2 `BookViewScreen`: `Math.min(128 / 9, …)`, 1.21.11 dieselben
   Konstanten). Deutsch wird bis zu **40 %** länger als Englisch, nicht 15 %. Deshalb:
   - Ziel: **höchstens 11 Zeilen inklusive Titel**, in beiden Sprachen.
   - **Rätsel und Randnotiz stehen auf getrennten Seiten.** Die Notizseite gibt es in beiden
     Zuständen (versiegelt: Platzhalter), damit Seitenzahlen und Inhaltsverzeichnis-Sprünge stabil
     bleiben.
   - Maßgeblich ist später ein Client-Test, der jede Seite in `en_us` und `de_de` misst (siehe 6.5).
7. **Nur dunkle Farben** auf dem Pergament (`DARK_GREEN`, `DARK_PURPLE`, `DARK_BLUE`, `DARK_RED`,
   `DARK_GRAY`), fett nur für Titel.
8. **Verweisen statt erklären:** „Die Aufzeichnungen aus der Leere wissen mehr über diesen Sand."

## 3. Die Reihe

| id (`simplebuilding:`) | Titel DE / EN | Rezept (formlos) | Warum dieses Schlüsselitem |
|---|---|---|---|
| `guide_book_primer` | Fibel des Baumeisters / The Builder's Primer | Buch + Kupfernugget | Das Kupfernugget ist die Einsteiger-Zutat der Mod (Steinmeißel, Verstärktes Bündel, Köcher). Billig, früh, in der ersten Spielstunde machbar. |
| `guide_book_reinforcement` | Verstärkung: Logbuch des Maschinisten / Reinforcement: A Machinist's Log | Buch + Kolben | Das Beispiel des Besitzers. Der Kolben schaltet auch das Rezept des Verstärkten Kolbens frei; das Buch handelt davon, Vanilla-Maschinen weiterzutreiben. |
| `guide_book_tools` | Almanach des Werkzeugmachers / The Toolmaker's Almanac | Buch + Steinmeißel | Das erste Rätsel der Fibel führt zum Steinmeißel; ihn abzugeben ist das „Gesellenstück". Alternativen siehe offene Entscheidungen. |
| `guide_book_storage` | Taschen, tiefer als sie scheinen / Pockets Deeper Than They Look | Buch + Bündel | Das Vanilla-Bündel ist die Basis aller Mod-Bündel und Köcher. |
| `guide_book_void` | Aufzeichnungen aus der Leere / Notes from the Void | Buch + Enderauge | Lesbar schon **vor** dem Ende – das Buch setzt Ziele („bring eine Diamantspitzhacke mit"). Die Ende-Kapitel bleiben bis `minecraft:end/root` versiegelt. |
| `guide_book_enchanting` | Geflüster des Ambosses / Whispers of the Anvil | Buch + Lapislazuli | Lapis füttert den Zaubertisch – und die erste Lektion lautet, dass der Tisch nur eine einzige Mod-Verzauberung anbietet. |
| `guide_book_resonance` | Resonanz: Abhandlung über Rüstungsbesätze / Resonance: A Treatise on Trims | Buch + Amethystsplitter | Wortspiel „Resonanz" (Amethyst klingt). Als Besatzmaterial heilt Amethyst gelegentlich (`TrimEffectUtil.getAmethystHealChance`, aufgerufen aus `LivingEntityMixin`). |

Rezeptkonflikte: Vanilla hat kein formloses Rezept „Buch + eines dieser Items", und kein
Mod-Rezept benutzt `minecraft:book` (beide Linien geprüft). Kollisionen mit anderen Mods lassen sich
aus diesem Repo nicht prüfen.

## 4. Wie Rätsel zu Antworten werden

Drei Ebenen, alles Vanilla-Mechanik:

1. **Rezeptbuch.** Jedes Themenbuch-Rezept wird durch das Schlüsselitem freigeschaltet
   (`unlockedBy("has_<key>", …)`), die Fibel auch schon durch ein Buch. Empfehlung: die
   Themenrezepte **nicht** schon durch den Besitz der Fibel freischalten – das Rezeptbuch würde das
   Schlüsselitem zeigen und das Rätsel verraten.
2. **Die Fibel löst sich auf.** Beim Öffnen prüft der Server je Themenrezept, ob der Spieler es
   kennt. Sobald ja, steht hinter der Rätselzeile ein „✔" und der echte Itemname (dunkelgrün).
   Kreislauf: Rätsel → raten → Item aufheben → Rezeptbuch bestätigt → Fibel bestätigt.
3. **Kapitel: versiegelt → enthüllt.** Jedes Kapitel hat eine Bedingung:
   - ein **verstecktes** Mod-Advancement `simplebuilding:guide/<buch>/<kapitel>` (Auslöser
     `inventory_changed` auf das Item oder den Tag; ohne Anzeige, also kein Toast, keine
     Chatmeldung, kein Eintrag im Advancement-Bildschirm; bleibt pro Spieler im Spielstand), oder
   - ein Vanilla-Advancement (auf beiden Linien vorhanden: `end/root`, `end/find_end_city`,
     `end/enter_end_gateway`, `adventure/trim_with_any_armor_pattern`, `story/enter_the_nether`,
     `story/enchant_item`, `story/mine_diamond`, `nether/obtain_ancient_debris`,
     `adventure/minecraft_trials_edition`, `end/kill_dragon`).
   - Versiegelt: Titel und Rätsel lesbar, die Notizseite zeigt einen kurzen **verschleierten
     Platzhalter** (obfuskierter Unsinnstext, nie der echte Text).
   - Seite 1 jedes Buchs ist ein anklickbares Inhaltsverzeichnis; versiegelte Kapitel tragen ein „?".

Zusätzliche Stilmittel, die im Vanilla-Buch auf beiden Linien funktionieren:
- **Hover-Hinweise** auf unterstrichenen Wörtern (zweiter Tipp zum Rätsel).
- **Runen** in der Zaubertisch-Schrift (`minecraft:alt`) als reine Zierde. Die Schrift kennt **nur
  A–Z, a–z und Leerzeichen**, deutsche Runenzeilen also transliterieren (ae/oe/ue/ss, keine
  Satzzeichen).
- **Tastenbelegung** als Komponente (`key.simplebuilding.simple_settings`) statt eines fest
  geschriebenen „G".

Wichtig zu wissen:
- `inventory_changed` feuert nur, wenn sich ein Stapel **ändert**. Wer das Item beim Update der Mod
  schon trägt, sieht das Kapitel erst, wenn der Stapel sich bewegt – entweder so hinnehmen oder beim
  Beitritt einmal nachprüfen.
- Das Versiegeln ist **nur eine Spielerfahrung, kein Geheimnis**: alle Rätsel, Hinweise und
  Randnotizen stehen im Klartext in den Sprachdateien.
- Ein Buch auf dem Lesepult oder in fremder Hand zeigt den Stand dessen, der es zuletzt geöffnet hat.

## 5. Die Bücher

Die Beispielseiten sind Entwürfe. Jede wurde mit den Vanilla-Glyphbreiten grob umbrochen und passt
in **höchstens 11 Zeilen inklusive Titel**, auf Deutsch wie auf Englisch. Die Quelle steht jeweils
dahinter.

### 5.1 Fibel des Baumeisters (Buch + Kupfernugget)

**Ton:** ein warmer Brief eines alten Baumeisters an den Lehrling, mit trockenem Humor.
**Kapitel:** An den Lehrling · Dein erstes Werkzeug (Steinmeißel: Zutaten ohne Form) · Die sechs
Schlüssel I + II (die sechs Themenrezepte als Rätsel) · Die Sprache des Schleichens (Schleichen als
Universal-Umschalter) · Fürs Auge (Quarz-Schachbrettblöcke, Baustellenlicht) · Kleingedrucktes
(„Wer jede Zahl will, findet das Wiki; wer spielen will, liest weiter").

> **EN – To the Apprentice**
> These pages will not tell you everything - they point the way. Underlined words whisper more
> when your cursor rests on them. Blurred lines clear up once you have found what they speak of.
>
> **DE – An den Lehrling**
> Diese Seiten verraten nicht alles - sie zeigen die Richtung. Unterstrichenes flüstert mehr, wenn
> der Mauszeiger darauf ruht. Verschwommenes wird klar, sobald du es gefunden hast.

> **EN – The Six Keys (I)**
> Bind an empty book to ...
> \- what pushes but never pulls, *(Hover: "Redstone moves it. Slime would make it sticky.")*
> \- the first tool this primer taught you, *(Hover: "Stick, cobblestone, copper.")*
> \- a pouch of string and hide. *(Hover: "Vanilla's own little bag.")*
>
> **DE – Die sechs Schlüssel (I)**
> Binde ein leeres Buch an ...
> \- was schiebt, aber nie zieht, *(Hover: „Redstone bewegt ihn. Schleim machte ihn klebrig.")*
> \- das erste Werkzeug dieser Fibel, *(Hover: „Stock, Bruchstein, Kupfer.")*
> \- einen Beutel aus Schnur und Haut. *(Hover: „Das kleine Säckchen aus dem Grundspiel.")*
>
> Teil II: „an eye that knows the way to the End / ein Auge, das den Weg ins Ende kennt"; „the blue
> stone the table hungers for / den blauen Stein, nach dem der Tisch hungert"; „a crystal that rings
> when you strike it / einen Kristall, der klingt, wenn man ihn anschlägt".

### 5.2 Verstärkung: Logbuch des Maschinisten (Buch + Kolben)

**Ton:** trockenes Ingenieurs-Logbuch, nummerierte Einträge, durchgestrichene Zeilen.
**Kapitel (Freischaltung):** Der Diamant, der zerbröselte (`diamond_pebble`) · Trichter mit Manieren
(Verstärkter/Netherit-Trichter) · Feuer, das es eilig hat (ein verstärkter Ofen/Schmelzofen/Räucherofen)
· Kolben mit Kraft (`reinforced_piston`) · Ein Krümel Netherit (`netherite_nugget`) · Der dunkle
Kolben (`netherite_piston`) · Sand, der sich nicht benimmt (versiegelt bis Astralit-Staub oder
Nihilith-Splitter, verweist auf die Leere-Aufzeichnungen) · Baustellenlicht (`construction_light`:
„heller als jede Fackel, schnell wieder abgebaut, klirrt wie Glas" – Lichtstufe 15 gegen 14 der
Fackel, Härte 0,3, Glasgeräusch; **nicht** „beim ersten Antippen weg", von Hand dauert es rund
9 Ticks).

> **EN – Log, entry 1** *(Quelle: Wiki „sledgehammer", Diamant-Kiesel; `cracked_diamond.json`)*
> The hammer is not only for stone. Lean on it, long and patient, against the most precious block
> you own. It will not break - it will crumble. Nine times nine crumbs. Keep them.
>
> **DE – Logbuch, Eintrag 1**
> Der Hammer ist nicht nur für Stein. Stemm ihn geduldig gegen den wertvollsten Block, den du hast.
> Er bricht nicht - er zerbröselt. Neunmal neun Krümel. Heb sie auf.

> **EN – Log, entry 12** *(Quelle: `NetheriteBreakerPistonBlock.java:35-38`, Schwelle Signal/15 × 50)*
> The dark piston does not push what it can break. How much it breaks depends on how loud you
> shout: a whisper cracks dirt, a scream cracks obsidian. The bedrock only laughs.
>
> **DE – Logbuch, Eintrag 12**
> Der dunkle Kolben schiebt nicht, was er zerbrechen kann. Wie viel, hängt davon ab, wie laut du
> schreist: Ein Flüstern knackt Erde, ein Schrei Obsidian. Grundgestein lacht nur.

### 5.3 Almanach des Werkzeugmachers (Buch + Steinmeißel)

**Ton:** Handwerker-Almanach: Sprichwörter, kurze Verse, Rätsel.
**Kapitel:** Meißel · Vorschlaghammer · Über Kerne · Baustab · Oktant (1 schlichter + 16 gefärbte;
Tastenbelegung als Komponente) · Rotator · Magnet · Der lauschende Kompass (Erzdetektor) ·
Geschwindigkeitsmesser · Aufstieg ohne Abschied (Basis-Upgrade-Vorlage: „behält Name, Abnutzung und
Verzauberungen") · Werkstatt-Tricks (Rahmen: Glasscheibe sperrt, Schere versteckt).

> **EN – On Cores** *(Quelle: `copper/iron/gold/diamond_core_plus.json` = 4 × Material um einen
> Netherstern; Maurer Stufe 2 verkauft den Kupferkern)*
> Every wand has a heart, born of a star that falls only from a three-headed sky. Four guardians
> stand around it; what they are made of decides how strong it beats. The poor ask a mason.
>
> **DE – Über Kerne**
> Jeder Stab hat ein Herz, geboren aus einem Stern, den nur ein dreiköpfiger Himmel fallen lässt.
> Vier Wächter umstehen ihn; ihr Stoff bestimmt, wie stark es schlägt. Wer arm ist, fragt einen
> Maurer.

*(Korrigiert: der Entwurf nannte „vier Barren: Kupfer, Eisen, Gold oder Diamant" – Diamant ist kein
Barren, und das Material bleibt jetzt verborgen, damit das Rezept nicht vollständig ist.)*

> **EN – The Listening Compass** *(Quelle: Wiki „ore_detector": Tonhöhe nach Entfernung,
> Tiefenschiefer/Basalt kosten Extra-Dichte, Schleichen + Rechtsklick auf Block kalibriert)*
> It does not see ore - it hears it. High voice: close. Deep voice: far. Deepslate and basalt
> swallow the song. Crouch and touch a block to teach it a new one.
>
> **DE – Der lauschende Kompass**
> Er sieht kein Erz - er hört es. Hohe Stimme: nah. Tiefe Stimme: fern. Tiefenschiefer und Basalt
> verschlucken das Lied. Schleich und berühre einen Block, dann lernt er ein neues.

> **EN – Margin note** (Vorschlaghammer, eigene Seite)
> Hold the use key on a full block and wait. Stairs. Again: a slab. It never goes back - unless a
> certain constructor has touched the hammer.
>
> **DE – Randnotiz**
> Halte die Benutzen-Taste auf einem vollen Block und warte. Treppe. Noch einmal: Stufe. Zurück geht
> es nie - es sei denn, ein gewisser Konstrukteur hat den Hammer berührt.

### 5.4 Taschen, tiefer als sie scheinen (Buch + Bündel)

**Ton:** fröhliches Tagebuch eines Sammlers, Wortspiele, ausufernde Listen.
**Kapitel:** Rätsel des Beutels (`reinforced_bundle`) · Nether und Ende (`netherite_bundle` /
`enderite_bundle`) · Der Köcher · Taschen mit Tiefe (Tiefe Taschen/Schublade/Trichter-Sog; verweist
aufs Amboss-Buch) · Bauen aus dem Beutel (Baumeister) · **Der Rucksack (reserviert).**

**Rucksack:** Im Repo gibt es dafür noch keinen Code. Das Kapitel erscheint **erst zusammen mit dem
Rucksack** auf beiden Linien und beiden Loadern – nach Regel 1 darf keine Seite etwas beschreiben,
das es nicht gibt. Bis dahin: keine Seite, kein Platzhalter-Tease.

> **EN – Riddle of the Pouch** *(Quelle: `reinforced_bundle.json`, Kapazität × 3/2)*
> Take an old pouch. Give it a string for a crown and three hides for boots. What it wears on its
> ears, you must find yourself. Then it swallows half again as much.
>
> **DE – Rätsel des Beutels**
> Nimm einen alten Beutel. Gib ihm eine Schnur als Krone und drei Häute als Stiefel. Was er an den
> Ohren trägt, findest du selbst. Dann schluckt er die Hälfte mehr.

*(Korrigiert: der Entwurf verriet auch die Kupfernuggets und war damit das komplette Rezept.)*

> **EN – The Quiver** *(Quelle: Wiki „quiver": nur `#minecraft:arrows`; Suchreihenfolge
> Zweithand → Brustplatz → Hotbar; tragbar ohne Rüstungspunkte)*
> Arrows only - it refuses everything else. Hang it where your bow can reach: the off hand, your
> belt of nine, or where a chestplate would sit. It will not stop a single arrow.
>
> **DE – Der Köcher**
> Nur Pfeile - alles andere verweigert er. Häng ihn, wo dein Bogen hinreicht: in die zweite Hand, an
> den Neunergürtel oder wo sonst ein Brustpanzer sitzt. Aufhalten wird er keinen Pfeil.

> **EN – Margin note** (Nether und Ende) *(Quelle: beide feuerfest und explosionsfest als
> fallengelassenes Item; nur Enderit steht in `void_protected`)*
> The Nether's bundle does not burn and laughs at creepers. The End's bundle does too - and if it
> falls into the void, it floats a while. Not forever. Hurry.
>
> **DE – Randnotiz**
> Das Bündel des Nethers brennt nicht und lacht über Creeper. Das des Endes auch - und fällt es in
> die Leere, schwebt es eine Weile. Nicht ewig. Beeil dich.

*(Korrigiert: „es wartet einfach auf dich" war falsch. Der Void-Schutz hält das Item nur fest; die
normale Lebensdauer eines Items von 6000 Ticks läuft weiter, nach fünf Minuten ist es weg.)*

### 5.5 Aufzeichnungen aus der Leere (Buch + Enderauge)

**Ton:** bruchstückhafte Expeditionsnotizen, zerrissene Seiten, einzelne Runenzeilen.
**Kapitel:** Vor der Reise (immer offen) · Zwei Steine (`end/root`) · Oben und unten (Astralit hebt:
schwebender Sand, Sprungkraft-Besatz; Nihilith hält: hängender Sand, schnelles Fallen) · Rezept,
zerrissen (Randnotiz beim Halten von Roh-Enderit) · Die Vorlage (`end/find_end_city`) · Was nicht
fällt (Void-Schutz; ab 2 Rüstungsteilen sanftes Sinken bei gehaltener Sprungtaste; `enderite_ingot`)
· Früchte der Leere (`enderite_nugget`) · Baustoff der Leere (polierter Endstein, Astral-/Nihil-Blöcke;
die Astral-Blöcke leuchten).

Hinweis: Der Entwurf wollte „Verzauberbarkeit von Enderit-Stab/-Meißel/-Vorschlaghammer" aussparen,
weil sie in den Tags fehle. Das ist überholt – die generierten Tags `building_wand_enchantable`,
`chisel_tools` und `sledgehammer_tools` führen sie inzwischen auf beiden Linien. Vor dem Schreiben
des Kapitels trotzdem prüfen, welche Verzauberungen sie tatsächlich annehmen.

> **EN – Two Stones** *(Quelle: Wiki „ore_generation")*
> One sunbathes on the islands' backs and glows faintly. The other hangs beneath them over nothing,
> like a bat. Bring diamond, or bring nothing home.
>
> **DE – Zwei Steine**
> Der eine sonnt sich auf den Rücken der Inseln und schimmert schwach. Der andere hängt darunter über
> dem Nichts wie eine Fledermaus. Bring Diamant mit - oder nichts nach Hause.

> **EN – Recipe, torn** *(Quelle: `raw_enderite_synthesis.json`, nur Hochofen)*
> Four of what rises, [torn] of what floats, and the pearl of a creature you should not look in the
> eye. Only the roaring furnace melts it. Out comes scrap.
>
> **DE – Rezept, zerrissen**
> Vier von dem, was steigt, [zerrissen] von dem, was schwebt, und die Perle eines Wesens, dem man
> nicht in die Augen sieht. Nur der brüllende Ofen schmilzt es. Heraus kommt Schrott.

*(Korrigiert: der Entwurf nannte alle neun Zutaten samt Anzahl. „[zerrissen]" wird im Spiel ein
kurzer verschleierter Text.)*

### 5.6 Geflüster des Ambosses (Buch + Lapislazuli)

**Ton:** geflüstert, kurze Verse, der Amboss spricht.
**Kapitel:** Der Tisch lügt (immer offen) · Viele Gaben (Berührung des Konstrukteurs gehalten) · Wer
schleicht, gräbt tiefer (Aderabbau/Tunnelgräber/Vielseitigkeit) · Der Hammer lernt (Übersteuerung/
Radius/Durchbruch) · Baumeisterhände (Baumeister/Farbpalette/Linear als „schnellere Ringe"/Schnelles
Meißeln) · Taschen (Tiefe Taschen/Schublade/Trichter) · Der Körper (Luftsprung; Kinetischer Schutz
„für Piloten, die Wände falsch einschätzen") · Wo Bücher schlafen (Fundort-Rätsel) · Eine Warnung
(optional, s. offene Entscheidungen). Freischaltung: ein Buch mit dieser gespeicherten Verzauberung
oder ein Werkzeug, das sie trägt.

> **EN – The Table Lies** *(Quelle: `src/main/resources/data/minecraft/tags/enchantment/in_enchanting_table.json`
> enthält nur Schnelles Meißeln)*
> Feed it all the blue you like: of everything in this book it offers only one - the gift for
> impatient chisels. The rest sleeps in chests and in the pockets of clever villagers.
>
> **DE – Der Tisch lügt**
> Füttere ihn mit Blau, so viel du willst: Von allem in diesem Buch bietet er nur eines an - die
> Gabe für ungeduldige Meißel. Der Rest schläft in Truhen und in Taschen kluger Dorfbewohner.

> **EN – Many Gifts** *(Quelle: Wiki-Notiz „constructors_touch"; Loot in `ModLootTableModifications`:
> vergrabener Schatz, Iglu, Prüfungskammer-Tresore; kein Handel)*
> One blessing, a different gift in every hand. A stick changes a block's mood, a wand opens a
> window, a magnet reaches twice as far. Pirates, snow huts, trial doors - never for sale.
>
> **DE – Viele Gaben**
> Ein Segen, in jeder Hand eine andere Gabe. Ein Stock verstellt Blocklaunen, ein Stab öffnet ein
> Fenster, ein Magnet reicht doppelt so weit. Piraten, Schneehütten, Prüfungen - nie käuflich.

*(Gekürzt: die deutsche Fassung des Entwurfs hatte 15 Zeilen; die letzte, „zu kaufen.", wäre
abgeschnitten worden und hätte den Sinn umgedreht.)*

### 5.7 Resonanz: Abhandlung über Rüstungsbesätze (Buch + Amethystsplitter)

**Ton:** gelehrte Abhandlung mit Fußnoten und teils geschwärzten Formeln.
**Kapitel:** Über Resonanz (immer offen) · Das Wappen neben deinen Taschen (Inventar-Knopf;
`adventure/trim_with_any_armor_pattern`) · Muster · Materialien (Hinweis: Tooltips rechnen mit
festem Faktor 0,2, der Referenzbildschirm am Schmiedetisch sagt die Wahrheit) · Sternenstaub und
Nichts (Astralit/Nihilith/Enderit gehalten) · Licht zum Anziehen · Die Referenz. Nie erwähnen:
eine Kupfer-Besatzwirkung oder „Schallschutz" durch Amethyst (beides gibt es nicht; Amethyst heilt).

> **EN – On Resonance** *(Quelle: Wiki „armor_trim_benefits"; beim Tod setzen Distanz, Zeit, Schaden
> und Kills zurück, Vanilla wirft außerdem Erfahrung ab, sofern keepInventory aus ist)*
> A trim is a promise; you keep it. Its strength is L times S times C. The first grows as you
> learn, the second as you endure, the third as you fight. Die, and two start over.
>
> **DE – Über Resonanz**
> Ein Besatz ist ein Versprechen; einlösen musst du es. Seine Stärke: L mal S mal C. L wächst, wenn
> du lernst, S, wenn du durchhältst, C, wenn du kämpfst. Stirbst du, beginnen zwei von vorn.

> **EN – Light You Can Wear** *(Quelle: `SledgehammerItem`, Vorlage im Rahmen + Glowstone-Staub
> bzw. Leuchttintenbeutel in der Zweithand)*
> A template in a frame. A hammer in your hand. In the other, the dust that Nether ceilings shed.
> Strike. What you get teaches armor to shine.
>
> **DE – Licht zum Anziehen**
> Eine Vorlage im Rahmen. Ein Hammer in der Hand. In der anderen der Staub, den die Decken des
> Nethers verlieren. Schlag zu. Was du erhältst, lehrt Rüstung zu leuchten.

## 6. Umsetzung

### 6.1 Optionen

| Option | Wie | Pro | Contra | Urteil |
|---|---|---|---|---|
| A. Fertiges `written_book` als Rezeptergebnis | Ergebnis trägt `written_book_content` | kein neues Item | statisch (kein Fortschritt), per Vanilla kopierbar, Titel ist fester Text (max. 32 Zeichen, nicht übersetzbar) | verworfen |
| **B. Eigenes `GuideBookItem` + Vanilla-Buchbildschirm** | `use()` baut die Seiten serverseitig aus Sprachschlüsseln und Spielerstand, öffnet dann den Vanilla-Bildschirm | kein Client-Code, kein Netzwerkpaket, keine Abhängigkeit; übersetzbar; Hover, Klick, Schriften; Lesepult; läuft auf 26.2 Fabric/NeoForge/Forge und 1.21.11 | nur Text (fürs Teasen eher gut); 14-Zeilen-Seiten | **empfohlen** |
| C. Eigenes Item + eigener Bildschirm | eigener Renderer mit Bildern, Rastern | maximale Freiheit | zwei Render-APIs (26.2 `GuiGraphicsExtractor` vs. 1.21.11 `GuiGraphics`), Öffnen je Loader, Sync-Paket, mehr Client-Tests auf 4 Zielen | höchstens später, auf demselben Seitenmodell |
| D. Modonomicon | Datapack-Bücher | reich, eingebaute Freischaltung | neue Abhängigkeit auf 4 Zielen, zwei Hauptversionen je Linie (26.2: 2.x, 1.21.11: 1.12x), wohl zwei Buchformate | verworfen |
| Patchouli / Lavender / GuideME | – | – | kein brauchbarer Build für 26.2 und/oder 1.21.11 | nicht möglich |

### 6.2 Empfehlung B im Detail

Ablauf beim Rechtsklick (Server):
1. Seiten als `WrittenBookContent` bauen: Titel leer, Autor leer, Generation 0,
   **`resolved = true`** (dann greift die linienabhängige `resolveForItem` nie).
2. `stack.set(WRITTEN_BOOK_CONTENT, …)`, dann `containerMenu.broadcastChanges()` selbst aufrufen,
   dann `ServerPlayer.openItemGui(stack, hand)`. Der Client liest die Komponente von **jedem** Item
   und öffnet den Vanilla-Bildschirm.
3. Leerer Titel ⇒ `getCustomName()` fällt auf den übersetzten Itemnamen zurück.
4. Tooltip: die Buchzeile per `TooltipDisplay` ausblenden, eigener Tooltip über `appendHoverText`.

Was die Gegenprüfung zusätzlich verlangt:
- **`stacksTo(1)`** ausdrücklich setzen. Sonst schreibt ein Leser die Seiten des ganzen Stapels um,
  und Stapel verschmelzen danach nicht mehr (Vanilla-`written_book` stapelt bis 16).
- **Buch-Kopieren.** Auf **1.21.11** akzeptiert das Vanilla-Kopierrezept jedes Item mit
  `WRITTEN_BOOK_CONTENT`: ein Leitbuch + N Buch-und-Feder ergibt N Leitbücher, das Original bleibt,
  und weil `use()` die Generation auf 0 setzt, geht das endlos – das Schlüsselitem-Rezept wäre
  umgangen. Auf **26.2** nimmt das datengetriebene Rezept nur `minecraft:written_book`. Verhalten
  bewusst wählen und angleichen; Empfehlung: ein Mixin nur auf 1.21.11, das `GuideBookItem` im
  Kopierrezept ablehnt, festgenagelt durch einen Gametest auf beiden Linien.
- **Tag-Prüfung** auf beiden Linien mit `stack.is(ItemTags.LECTERN_BOOKS)` – das gibt es auf 26.2
  über `TypedInstance`; kein `typeHolder()`.
- **Reiner Seitenbauer:** `GuideBooks.build(buch, enthüllteKapitel, bekannteRezepte)` plus ein
  dünner Adapter, der die beiden Mengen aus dem `ServerPlayer` liest. Nur so kann der Client-Test
  jede Seite in jedem Zustand messen.
- **Sieben statische Felder** in `ModItems` (keine EnumMap): `DataIntegrityTests.declaredModItems()`
  (`DataIntegrityTests.java:1338`) liest nur statische Item-Felder.
- **Versteckte Advancements** kommen aus dem Fabric-Datagen nach `src/main/generated` bzw.
  `mc1_21_11/fabric/src/main/generated`. NeoForge und Forge bekommen sie nur, weil
  `neoforge/build.gradle:42`, `mc1_21_11/neoforge/build.gradle:42` und `forge/build.gradle:44` diese
  Ordner als Ressourcen einbinden.
- **Fibel beim ersten Beitritt** (optional): ein gemeinsamer Helfer, aufgerufen aus
  `Simplebuilding.java:98` (26.2) bzw. `:96` (1.21.11) und `NeoForgeGameplayEvents.java:100` bzw.
  `:101`. Merker als verstecktes Advancement (überlebt den Tod), nicht in den `SimpleBuildingData`-NBT.
- **Ausschalter** (optional): Rezepte brauchen **beide** Bedingungen, `fabric:load_conditions` und
  `neoforge:conditions`, wie die Handels-JSONs heute schon.

### 6.3 Wo die Linien sich unterscheiden

| Thema | 26.2 | 1.21.11 | Folge |
|---|---|---|---|
| `WrittenBookContent.resolveForItem` | `(ItemStack, ResolutionContext, HolderLookup.Provider)` | `(ItemStack, CommandSourceStack, Player)` | nie aufrufen, `resolved = true` |
| Buch-Kopierrezept | nur `written_book` | jedes Item mit Buchinhalt | Mixin nur auf 1.21.11 (s. o.) |
| `HoverEvent.ShowItem` | `ItemStackTemplate` | `ItemStack` | nur `ShowText` benutzen |
| Advancement-Prädikate (Datagen) | `advancements.predicates` / `.triggers` | `advancements.criterion` | nur Importe verschieden, JSON gleich |
| `FabricAdvancementProvider` | Konstruktor mit `FabricPackOutput` | mit `FabricDataOutput` | Provider je Linie |
| Actionbar-Meldung (falls gewünscht) | `sendOverlayMessage` | `displayClientMessage(c, true)` | Falle, wenn Benachrichtigungen dazukommen |

### 6.4 Dateien (Phase 2, grob)

Neu je Linie: `items/custom/GuideBookItem.java`, `guide/GuideBooks.java` (gemeinsamer Code +
1.21.11-Spiegel). Geändert je Linie: `ModItems`, Kreativ-Tab, Datagen (Rezepte, Modelle, Item-Tags
`lectern_books`/`bookshelf_books`, neuer Advancement-Provider), Sprachdateien `en_us`/`de_de`
(grob 200 Schlüssel je Sprache und Linie), 7 Cover-Texturen, `wiki/manual.json` (Notiz
`guide_book_*` + Feature `guide_books` – ohne Rätsellösungen), 1.21.11-Mixin fürs Kopierrezept.
Optional: Config-Schalter, Beitritts-Hook.

### 6.5 Tests (Phase 2, beide Linien)

- Server-Gametests: die 7 Rezepte existieren und bestehen genau aus Buch + Schlüsselitem;
  `use()` mit dem Mock-Spieler (der hat eine echte, eingebettete Verbindung) – Seiteninhalt vor und
  nach `award(…)` / `awardRecipesByKey(…)`; Lesepult-Tag; Kopierrezept lehnt ab.
- Client-Test: jede Seite, versiegelt und enthüllt, in `en_us` und `de_de`, höchstens 14 Zeilen bei
  114 px (Stil wie im Vanilla-Bildschirm zusammenführen, dann umbrechen). Die Sprache danach
  **zurücksetzen**, sonst laufen die folgenden Client-Tests auf Deutsch.
- JUnit: jeder `guide.simplebuilding.*`-Schlüssel existiert, und zwar auf beiden Linien identisch.

## 7. Risiken

- Stilles Abschneiden langer Seiten – nur der Client-Test in beiden Sprachen entscheidet.
- Wahrheitsdrift: Bücher behaupten heutiges Verhalten (wirkungslose Abdeckung/Brücke, Sand mit
  Vanilla-Texturen, …). Jedes Kapitel braucht Quellen je Linie und einen Punkt in der
  Release-Checkliste, sonst lügen die Bücher nach dem nächsten Fix.
- Rund 200 neue Sprachschlüssel je Sprache und Linie; der bestehende Test prüft nur en/de-Gleichheit
  **je** Linie, nicht die Gleichheit zwischen den Linien.
- Zu schwere Rätsel frustrieren. Gegenmittel: Hover-Hinweise und die sich auflösende Fibel – ob das
  reicht, zeigt nur Probespielen.

## 8. Offene Entscheidungen

1. Schlüsselitem fürs Werkzeugbuch: **Steinmeißel** (empfohlen), Eisenbarren oder Kompass? Und
   Werkzeuge teilen in „Werkzeuge" und „Instrumente" (Oktant, Magnet, Erzdetektor, Geschwindigkeitsmesser; Schlüssel
   Kompass) → acht Bücher?
2. Fibel beim ersten Beitritt geben? Wenn ja: Config-Standard an oder aus?
3. Rezept „Buch + Schlüsselitem" (empfohlen) oder „Fibel + Schlüsselitem" (Fibel bleibt zurück)?
4. Themenrezepte erst nach dem Schlüsselitem im Rezeptbuch (empfohlen) oder schon mit der Fibel?
5. Seite „Eine Warnung" über das wirkungslose Abdeckung/Brücke: aufnehmen, weglassen, oder erst die
   beiden Verzauberungen umsetzen?
6. Schwebender/hängender Sand und Kies sehen aus wie Vanilla: als bewusster Tease behalten oder
   eigene Texturen (dann ändert sich die Seite)?
7. Buch-Kopieren: sperren (empfohlen, Mixin auf 1.21.11) oder auf beiden Linien erlauben?
8. Lagerungsbuch jetzt veröffentlichen und das Rucksack-Kapitel später nachreichen, oder auf den
   Rucksack warten?
9. Buchtitel und Cover-Texturen: zeichnest du sie, oder erst Platzhalter?
10. Runenzeilen in Zaubertisch-Schrift: willkommene Würze oder zu obskur?
11. Bücher zusätzlich als seltene Beute (z. B. Leere-Aufzeichnungen in Festungsbibliotheken, an
    `enableLootTableChanges` gekoppelt) oder nur herstellbar?
12. Config-Schalter, der die Bücher für Modpacks mit eigenen Questbüchern ganz abschaltet?
