# Fragebogen: Baustab, Oktant, Verstärktes Bündel

Stand 2026-09-24. Grundlage: Code-Durchsicht beider Linien plus Gegenprüfung. **Nur Fragen, noch
keine Änderung am Code.**

**So ausfüllen:** Kästchen ankreuzen (`[x]`). Pro Frage genau eine Option, außer wo
„Mehrfachauswahl" dasteht. Aufwand: **S** = Stunden, **M** = ein bis zwei Tage, **L** = mehr.
Jede Änderung gilt für beide Linien und beide aktiven Loader, samt Tests und Wiki.

**Belege:** Pfade ohne Präfix liegen unter `common/src/shared/java/com/simplebuilding/`, Zeilen
beziehen sich auf 26.2. Der 1.21.11-Spiegel (`mc1_21_11/shared/java/…`) hat dieselbe Logik und nur
umbenannte APIs – jeder Befund gilt also auf **beiden** Linien, außer wo es anders dasteht.

---

## Teil A – Echte Defekte

**D1 · Hoch – Der Stab verbraucht jedes BlockItem und setzt es roh.**
Materialtest ist nur `instanceof BlockItem` (`items/custom/BuildingWandItem.java:171,191,237,263,308,324`),
gesetzt wird der Standardzustand per `setBlock` (`:241`, `:420-422`), danach `shrink(1)` (`:53`).
Echter Item-Verlust: Inhalt von Shulkerkisten, Bannermuster, Kopf-Profile, Bienen im Bienenstock,
Topfverzierungen, eigene Namen (alles an Komponenten hängt); Betten (nur eine Hälfte); der
**Pulverschnee-Eimer** (`SolidBucketItem` ist ein BlockItem – der Eimer wird verbraucht, der leere
Eimer kommt nie zurück). Pflanzen, Fackeln, Redstone und Türen poppen dagegen als Item ab (kein
Verlust, nur Haltbarkeit), Schnur bleibt schwebend stehen.
beheben? [ ] ja [ ] nein

**D2 · Mittel – Schutzprüfungen nur an der geklickten Position.**
Spawnschutz, Weltgrenze und Abenteuer-Prüfung laufen nur für den geklickten Block; jede gesetzte
Position geht über rohes `setBlock` (`items/custom/BuildingWandItem.java:411-422`): kein
`mayInteract`, kein `mayUseItemAt`, keine Weltgrenze, kein NeoForge-Platzier-Event (Claim-Mods
greifen nicht) und an **keiner** Position eine Kollisionsprüfung – der Stab kann Spieler und Mobs
einmauern.
beheben? [ ] ja [ ] nein

**D3 · Mittel – Bündel in Bündeln ergeben unbegrenzten Platz.**
Jedes eingefügte Item wiegt `1/maxStackSize` (`items/custom/ReinforcedBundleItem.java:314`), die
Menge rechnet mit derselben Annahme (`:320`). Ein volles Bündel zählt beim Einfügen also wie ein
einzelnes Item; rekursiv verschachtelt passt beliebig viel in einen Slot. Steht bereits im Javadoc
von `gametest/ReinforcedBundleTests.java:88-96`, aber nicht im Wiki.
beheben? [ ] ja [ ] nein

**D4 · Niedrig–Mittel – Der Stab baut nach dem Zerbrechen weiter.**
Die Ringschleife prüft nach `hurtAndBreak` (`items/custom/BuildingWandItem.java:430`) nie, ob der
Stab weg ist (`:410-433`). Der Rest des Rings wird gesetzt und kostet Material, aber keine
Haltbarkeit mehr (bis 48 Blöcke beim Enderit-Stab).
beheben? [ ] ja [ ] nein

**D5 · Niedrig – Überschrift im Stab-Menü unsichtbar.**
`client/gui/BuildingWandScreen.java:149` zeichnet „Configuration" mit `0xFFFFFF` = Alpha 0; beide
Linien überspringen Text mit Alpha 0. Das Wiki beschreibt sie als sichtbar (`wiki/manual.json:2749`,
`:5974`).
beheben? [ ] ja [ ] nein

**D6 · Mittel – Oktant-Figur ohne Grenze und ohne Cache.**
`client/render/BlockHighlightRenderer.java:259-292` besucht in **jedem Frame** jedes Voxel der
Box. Riesige Auswahlen entstehen nicht nur im Manager, sondern schon per Klick (Pos1 setzen, weit
laufen, Pos2 setzen: `items/custom/OctantItem.java:85-88`). Mit Berührung des Konstrukteurs, Kugel und
Größe 500 hängt der Client. Nebenbei läuft das Volumen `dx*dy*dz` ab ~1290³ über
(`client/gui/RangefinderHudOverlay.java:103`).
beheben? [ ] ja [ ] nein

**D7 · Niedrig – Eingabefelder im Oktant-Manager.**
(a) Die Größenzeile setzt immer `P2 = P1 + Größe - 1` (`client/gui/OctantScreen.java:318`) – liegt
P2 unter P1, klappt die Auswahl beim ersten Klick auf die andere Seite. (b) Nach Änderung an P1/P2
wird die Größe nicht aktualisiert (`:322-343`). (c) Ein halb getipptes „-" oder ein leeres Feld wird
0 (`:344`) und bei jedem Tastendruck an den Server geschickt (`:250`) – die Auswahl springt beim
Tippen auf Koordinate 0. (d) Nie gesetzte Ecken gehen als `(0,0,0)` raus (`:339`), obwohl das Paket
„leer" kann.
beheben? [ ] ja [ ] nein

**D8 · Niedrig (dokumentiert) – Falsche Messbeschriftung.**
Nur `dy == 1` gilt als Linie oder Fläche (`client/gui/RangefinderHudOverlay.java:97-105`,
`client/gui/OctantScreen.java:482-490`): eine senkrechte Linie oder Wand heißt „Volume … blocks³".
HUD schreibt „blocks²", der Manager „blocks^2", ein Block heißt „1 blocks". Das Wiki beschreibt die
Einteilung korrekt (`wiki/manual.json:2930`), aber `:596` (en) / `:613` (de) behaupten „Abstand bei
einer Linie".
beheben? [ ] ja [ ] nein

**D9 · Niedrig, nur 1.21.11 NeoForge – Umschalt-Meldungen landen im Chat.**
`mc1_21_11/neoforge/src/main/java/com/simplebuilding/neoforge/SimplebuildingNeoForgeClient.java:170,174`
rufen `displayClientMessage(…, false)` – auf 1.21.11 heißt `false` Chat. 1.21.11 Fabric
(`true`) und beide 26.2-Loader (`sendOverlayMessage`) nutzen die Actionbar. Das Wiki behauptet die
Actionbar für NeoForge (`wiki/manual.json:2977`).
beheben? [ ] ja [ ] nein

**D10 · Niedrig – Färben ist eine Gratis-Reparatur und löscht Verzauberungen.**
Das Färberezept ist formlos (`src/main/generated/data/simplebuilding/recipe/octant_red_from_dye.json`
usw.), das Ergebnis ein neues Item: ein beschädigter Oktant kommt heil heraus, Verzauberungen (auch
die der Beute-Oktanten aus Antiker Stadt und Netherfestung) und Name sind weg. Waschen baut
`new ItemStack(ModItems.OCTANT)` und kopiert nur `CUSTOM_DATA`
(`src/main/java/com/simplebuilding/Simplebuilding.java:166-169`, plus vier Kopien in den anderen
Loadern) – auch das repariert voll.
beheben? [ ] ja [ ] nein

**D11 · Niedrig, plausibel (im Spiel prüfen) – Der Stab „wippt" beim Bauen.**
`inventoryTick` schreibt jeden Tick `CUSTOM_DATA` neu (`items/custom/BuildingWandItem.java:378`,
`:441`); nur `DAMAGE` ist von der Wechsel-Animation ausgenommen. Jeder Oktant-Klick und -Scroll
wippt einmal.
beheben? [ ] ja [ ] nein

**D12 · Niedrig – Farbpalette am Bündel würfelt auf Client und Server getrennt.**
`items/custom/ReinforcedBundleItem.java:170-173` würfelt über **alle** Einträge, auch Nicht-Blöcke,
auf beiden Seiten einzeln. Trifft es einen Nicht-Block, fällt der Klick auf `use()` durch, und das
wirft den ausgewählten/ersten Stapel ab, sofern der kein Block ist (`:205-229`). Folge: Geisterblöcke, Klicks ohne Wirkung
oder ein Stapel auf dem Boden.
beheben? [ ] ja [ ] nein

**D13 · Niedrig–Mittel – Ein laufender Stab-Bau folgt dem Spieler in eine andere Dimension.**
Gemerkt werden nur `OriginX/Y/Z` und `Face` (`items/custom/BuildingWandItem.java:356-359`), gebaut
wird im Level des aktuellen Ticks (`:370`). Ein Portal, ein Teleport in eine andere Dimension oder
ein Respawn mit keepInventory mitten im Bau setzt die restlichen Ringe an denselben Koordinaten in
der neuen Dimension.
beheben? [ ] ja [ ] nein

**D14 · Niedrig, plausibel (im Spiel prüfen) – Trichter-Sog saugt eben Abgeworfenes sofort wieder ein.**
Die Hand-Schleife in `mixin/ItemEntityMixin.java:62-68` läuft **vor** der `pickupDelay`-Prüfung
(`:71`) und ignoriert den Werfer. Ein per Rechtsklick (`items/custom/ReinforcedBundleItem.java:220-223`)
oder Q abgeworfener Stapel landet mit Trichter II in der Hand gleich wieder im Bündel (Trichter I:
bei passenden Stapeln). Das Wiki nennt nur das Überspringen der Verzögerung.
beheben? [ ] ja [ ] nein

**D15 · Niedrig – Manipulierte NBT lässt den Spieler-Tick abstürzen.**
`Direction.values()[getBlockInt(nbt, "Face")]` (`items/custom/BuildingWandItem.java:392`) wirft bei
einem ungültigen Wert – jeden Tick. Nur über `/give` erreichbar.
beheben? [ ] ja [ ] nein

**D16 · Niedrig – Config-Option `buildingHighlightOpacity` falsch beschriftet.**
Das Feld ist ein Ganzzahl-Prozentwert (Standard 40, `config/SimplebuildingConfig.java:35`), der
Tooltip sagt „(0.0 - 1.0)" (`src/main/resources/assets/simplebuilding/lang/en_us.json:123`), und
auf den Stab wirkt die Option gar nicht (im Wiki dokumentiert).
beheben? [ ] ja [ ] nein

**D17 · Wiki veraltet.**
`wiki/manual.json:523` (en) / `:543` (de) sagen, der Enderit-Stab lasse sich nicht mit
Stab-Verzauberungen versehen – falsch seit der Entscheidung vom 2026-09-09 (generiertes
`building_wand_enchantable.json` führt ihn auf beiden Linien). Dazu die Stellen aus D5, D8 und D9.
beheben? [ ] ja [ ] nein

---

## Teil B – Verbesserungen

### Baustab

**1 · Setzen wie ein Spieler** – Aufwand **M**
Warum: `BlockItem.place` behebt den Großteil von D1 (Komponenten bleiben, Türen/Betten vollständig,
Überleben/Kollision/Ausrichtung geprüft) – aber nicht den Pulverschnee-Eimer.
- [ ] a) ja, über `BlockItem.place` (Position nur, wenn der Kontext wirklich diese Position ersetzt;
  das eigene Setzgeräusch bleibt, weil `place` den Bauenden stumm lässt; NeoForge-Platzier-Event
  über einen Plattform-Hook)
- [ ] b) nur Sicherheitsprüfungen, Standardzustände bleiben
- [ ] c) so lassen

**2 · Was als Baumaterial zählt** – Aufwand **S–M**
Warum: D1; eine Regel statt einer Liste von Sonderfällen.
- [ ] a) nur Stapel ohne Komponenten-Änderung + Item-Tag `simplebuilding:building_wand_blacklist`
  + `SolidBucketItem` ausdrücklich aus
- [ ] b) nur volle Würfel
- [ ] c) a und b

**3 · Schutz und Kollision an jeder Position** – Aufwand **M**
Warum: D2; ohne das greifen Spawnschutz und Claim-Mods nur am Klickpunkt.
- [ ] ja, gesperrte Position **überspringen**
- [ ] ja, bei gesperrter Position den **ganzen Bau stoppen**
- [ ] nein

**4 · Wenn der Stab bricht** – Aufwand **S**
Warum: D4.
- [ ] a) sofort stoppen
- [ ] b) stoppen und gar nicht erst starten, wenn die Haltbarkeit nicht reicht
- [ ] c) so lassen

**5 · Neuer Klick während eines laufenden Baus** – Aufwand **S**
Warum: `useOn` prüft `Active` nie; heute bricht der neue Klick den alten Bau still ab.
- [ ] a) so lassen (neu starten)
- [ ] b) ignorieren, bis der Bau fertig ist
- [ ] c) in die Warteschlange

**6 · Baustatus aus dem Item in eine Server-Jobliste** – Aufwand **M–L**
Warum: Schreiben jeden Tick (D11), `Active` bleibt nach Absturz hängen; braucht einen Tick-Hook je
Loader (Forge: Stub) und Aufräumen bei Abmelden/Tod/Dimensionswechsel (löst D13 mit).
- [ ] ja [ ] nein

**7 · Materialreihenfolge und Ursprung** – Aufwand **S**
Warum: die Materialsuche ignoriert den angeklickten Block; im Kreativmodus ohne Blöcke passiert nichts.
- [ ] a) geklickter Block → Zweithand → Hotbar
- [ ] b) Zweithand → geklickter Block → Hotbar
- [ ] c) so lassen

Gras/Schneeschicht an Ort und Stelle ersetzen? [ ] ja [ ] nein ·
Kreativ ohne Blöcke: geklickten Block nehmen? [ ] ja [ ] nein

**8 · Rückgängig (Undo)** – Aufwand **L**
Warum: haben alle vergleichbaren Mods; letzte 1–5 Bauten pro Spieler, Items zurück, wenn unverändert.
Undo? [ ] ja [ ] nein · Haltbarkeit auch zurück? [ ] ja [ ] nein

**9 · Vorschau** – Aufwand **S–M**
Warum: zeigt heute mehr Geisterblöcke, als Material/Haltbarkeit hergeben, wird jeden Frame neu
gebaut; Config-Tooltip siehe D16.
- [ ] a) materialbewusst (Rest rot) + volle Größe + Deckkraft aus der Config (Option richtig beschriften)
- [ ] b) nur materialbewusst
- [ ] c) so lassen

**10 · Tooltip und Anzeige** – Aufwand **S**
Warum: vorhandene Sprachschlüssel `tooltip.simplebuilding.building_wand.*` werden nie gezeigt.
- [ ] a) Tooltip
- [ ] b) Tooltip + Actionbar-Zeile beim Halten („7×7 · Achse Auto · Stein ×243")
- [ ] c) nichts

**11 · Feuerfeste Stäbe** – Aufwand **S**
Warum: Netherit-/Enderit-Bündel und -Köcher sind feuerfest, die Stäbe nicht (`items/ModItems.java:297,299`).
- [ ] a) Netherit + Enderit
- [ ] b) nur Enderit
- [ ] c) keiner

**12 · Server-Config für den Stab** – Aufwand **M** (Mehrfachauswahl)
Warum: heute alles Konstanten (`items/custom/BuildingWandItem.java:36-44`).
- [ ] Radius-Obergrenze [ ] Haltbarkeit pro Block [ ] Ring-Verzögerung [ ] Vorschau an/aus [ ] keine

**13 · Abdeckung (Cover)** – Aufwand **M**
Warum: heute wirkungslos (Gametest nagelt das fest), der Beschreibungstext verspricht Oberflächenbau.
- [ ] a) Oberflächenmodus: nur setzen, wo dahinter ein tragfähiger Block ist
- [ ] b) aus Beute und Kreativ-Tab nehmen, auslaufen lassen
- [ ] c) wirkungslos lassen

**14 · Brücke (Bridge)** – Aufwand **M**
Warum: wirkungslos, keine Beute- oder Handelsquelle.
- [ ] a) 1 breite Linie entlang der Flächennormalen
- [ ] b) „Engel"-Setzen in die Luft vor dem Spieler
- [ ] c) entfernen

**15 · Linear** – Aufwand **S** (umbenennen) / **M** (Linie)
Warum: verkürzt nur die Verzögerung, der Text verspricht eine Linie beim Schleichen.
- [ ] a) Schleich-Linie umsetzen
- [ ] b) umbenennen in „schnelleres Bauen"
- [ ] c) beides

**16 · Farbpalette am Stab** – Aufwand **S–M**
Warum: die Vorschau mischt, gesetzt wird immer der erste Stapel; die Amboss-Regel „Farbpalette
braucht Baumeister" gilt auch für Stäbe, obwohl der Stab-Code sie nicht braucht.
Setzen, was die Vorschau zeigt? [ ] ja [ ] nein ·
Farbpalette am Stab ohne Baumeister erlauben? [ ] ja [ ] nein

**17 · Berührung des Konstrukteurs als Sperre des Stab-Menüs** – Aufwand **S**
Warum: ohne sie baut jeder Stab immer in voller Größe.
- [ ] a) Radius für alle, Berührung des Konstrukteurs für Achse und Modi
- [ ] b) so lassen
- [ ] c) Schleichen + Mausrad ändert den Radius, für alle

**18 · Quellen für Baumeister** – Aufwand **M**
Warum: Kommentare versprechen Bündel und Shulker, gesucht wird nur im Verstärkten Bündel.
- [ ] a) Vanilla-Bündel [ ] b) Shulkerkisten [ ] c) beides [ ] d) keine

**19 · Stab-Menü überarbeiten** – Aufwand **S**
Warum: pausiert und verdeckt die Vorschau, zeigt Radius statt Größe, Achsen unübersetzt, E fest verdrahtet.
- [ ] a) alles (nicht pausierend, „7×7", ±-Knöpfe, übersetzte Achsen, Inventartaste, D5)
- [ ] b) nur D5 + Übersetzungen

**20 · Kern-Rezepte** – Aufwand **S**
Warum: jeder Kern braucht einen Netherstern, auch der Kupferkern (Handelsweg über den Maurer existiert).
- [ ] a) so lassen
- [ ] b) Kupfer und Eisen ohne Stern
- [ ] c) Stern nur für Diamant

### Oktant

**21 · Figur deckeln und cachen** – Aufwand **M**
Warum: die einzige echte Lösung für D6 (Klemmen der Pakete nur zusätzlich, weil große Auswahlen
auch per Klick entstehen); über der Grenze nur die Box plus HUD-Hinweis.
Grenze pro Achse: [ ] a) 128 [ ] b) 256 [ ] c) konfigurierbar

**22 · Dimension merken** – Aufwand **S**
Warum: die Auswahl speichert nur Koordinaten und erscheint im Nether an denselben Zahlen.
- [ ] ja [ ] nein

**23 · Fernauswahl** – Aufwand **S–M**
Warum: Rechtsklick in die Luft tut heute nichts; Schleichen + Luftklick ist bereits der Reset
(Gametest `air_clicks_only_reset_an_unlocked_octant_while_sneaking`).
Fernauswahl? [ ] nein [ ] ja, 64 Blöcke [ ] ja, 128 Blöcke
Pos2 aus der Ferne über: [ ] abwechselnde Ecken [ ] Reset wandert ins Menü, Schleichen setzt Pos2
[ ] Modifikatortaste

**24 · Haltbarkeit** – Aufwand **S**
Warum: jeder Klick kostet 1 von 128 – ungewöhnlich für ein Messwerkzeug.
- [ ] a) so lassen [ ] b) unzerbrechlich [ ] c) nur die Fernauswahl kostet

**25 · Messwerte** – Aufwand **S–M** (Mehrfachauswahl)
Warum: HUD rechnet immer mit der Box, nicht mit der Form; D8.
- [ ] Anzahl der Blöcke der echten Form [ ] Stapel + Shulkerkisten [ ] euklidische Distanz
[ ] nur D8 korrigieren

**26 · Füll-Optionen Hohl / Schicht / Reihenfolge** – Aufwand **L / S / S**
Warum: gespeichert, aber ungenutzt (dokumentiert).
- [ ] a) Oktant + Stab: Stab in der Haupthand füllt die Oktant-Form in der Zweithand samt Optionen
- [ ] b) nur „Hohl" in der Vorschau
- [ ] c) Seite 2 des Managers entfernen

**27 · Auswahlbox auch ohne Berührung des Konstrukteurs** – Aufwand **S**
Warum: ohne sie sieht man nur die zwei Eckwürfel; Option „Invert Octant Sneak" ist irreführend benannt.
Box immer, Formfüllung mit Berührung des Konstrukteurs, Option umbenennen? [ ] ja [ ] nein

**28 · 2D-Formen wirklich flach** – Aufwand **S**
Warum: Rechteck und Ellipse verhalten sich heute wie 3D.
- [ ] ja [ ] nein

**29 · Manager-Bedienung und Sperre** – Aufwand **S–M**
Warum: behebt D7 (Ziffernfilter, nur gültige Werte senden, Größe nachführen, leere Ecken als „leer",
breitere Felder, Umschalt-Klick ±10).
Soll die Sperre auch Änderungen im Manager blockieren (Server übernimmt bei gesperrtem Oktant nur
noch das Sperr-Feld, damit Entsperren möglich bleibt)? [ ] ja [ ] nein

**30 · Färben und Waschen** – Aufwand **S**
Warum: D10.
- [ ] a) behält Verzauberungen, Schaden, Name und Auswahl (`crafting_transmute` + `transmuteCopy`,
  ein gemeinsamer Wasch-Helfer statt fünf Kopien)
- [ ] b) nur die Auswahl (heute, inkl. Gratis-Reparatur)

**31 · Hervorhebungs-Tasten** – Aufwand **S**
Warum: Zustand vergisst sich beim Neustart, zwei Tasten schalten denselben Schalter.
- [ ] a) merken + Tasten trennen (alles / nur Figur)
- [ ] b) merken + doppelte Taste löschen
- [ ] c) so lassen

**32 · Oktant in der Zweithand konfigurierbar** – Aufwand **S**
Warum: HUD, Hervorhebung und Klicks gehen in der Zweithand, Manager und Mausrad nur in der Haupthand.
- [ ] ja [ ] nein

### Verstärktes Bündel

**33 · Auswahl in der Hand für Baumeister** – Aufwand **M**
Warum: die Auswahl setzt sich beim Verlassen des Slots zurück, „ausgewählter Eintrag" heißt praktisch
immer „erster". Farbpalette würde dabei nur unter Blockeinträgen und vom Server gewählt (behebt D12).
Auswahl in der Hand? [ ] ja [ ] nein · Taste: [ ] Alt [ ] Strg [ ] Schleichen

**34 · Tooltip genauer** – Aufwand **S**
Warum: Füllbalken teilt durch den ganzzahligen Faktor; vorhandene Sprachzeilen werden nie gezeigt;
Verzauberungen per Namensteil statt per Schlüssel erkannt.
- [ ] ja [ ] nein

**35 · Verschachtelte Bündel** – Aufwand **S**
Warum: D3; Zulassung (`:314`) **und** Mengenrechnung (`:320`) müssen das Vanilla-Gewicht nutzen.
- [ ] a) Vanilla-Gewichtung
- [ ] b) nicht-leere Bündel im Bündel verbieten
- [ ] c) so lassen

**36 · Upgrade-Rezept behält den Inhalt** – Aufwand **S**
Warum: der vorhandene Serializer `simplebuilding:reinforced_bundle` kopiert Inhalt und Name, wird
aber nicht benutzt; mit `#minecraft:bundles` gingen auch gefärbte Bündel.
- [ ] ja [ ] nein

**37 · Trichter-Sog steuern** – Aufwand **M**
Warum: heute nur „nie beim Schleichen"; D14.
- [ ] a) Umschalter pro Bündel [ ] b) Schleich-Regel behalten [ ] c) beides
Frisch Abgeworfenes nicht sofort wieder einsaugen (Verzögerung auch für Hand-Bündel)? [ ] ja [ ] nein

**38 · Rechtsklick-Abwurf** – Aufwand **S**
Warum: ein Rechtsklick in die Luft wirft sofort den ganzen ausgewählten Stapel.
- [ ] a) so lassen
- [ ] b) nur mit Schleichen
- [ ] c) wie Vanilla: Taste halten, erst ein Stapel, nach 10 Ticks alle 2 Ticks der nächste

**39 · Schublade und Kapazität** – Aufwand **S**
Warum: Formel ausdrücklich offen, Text sagt „ein Item", Code erlaubt 5 Sorten, keine Beutequelle;
Tiefe Taschen × Schublade bis 12× (Enderit-Bündel: 3456 Items).
Formel: [ ] a) (16+L)/8 [ ] b) (8+L)/8 · Schublade-Buch als Beute? [ ] ja [ ] nein ·
Gesamtfaktor deckeln? [ ] ja [ ] nein

### Übergreifend

**40 · Hartkodierte englische Texte übersetzen, Umschalt-Code vereinheitlichen** – Aufwand **S–M**
Warum: HUD, Manager, Stab-Menü und Umschaltmeldungen sind fest Englisch; Fabric 26.2 hat eine eigene
Kopie von `ClientToggleKeys`; behebt D9.
- [ ] ja [ ] nein

---

Hinweis zu den Folgen: Viele Entscheidungen ändern bewusst festgenagelte Tests auf beiden Linien
(z. B. Cover/Bridge wirkungslos, Linear verkürzt nur, Oktant setzt nichts, Oktant-Färben und
-Waschen, Luftklick-Reset, Client-Tests zu Vorschau, Hervorhebung, Stab-Menü und HUD). Loot-Änderungen
und Kreativ-Tab sind Java, Handel ist auf 26.2 JSON und auf 1.21.11 Java; nur Verzauberungen und Tags
kommen aus dem Datagen.
