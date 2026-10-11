# Mob-Konzepte (Entwurf, 2026-10-07)

Nur Konzepte, noch nichts gebaut. Texturen und 3D-Modelle folgen als Vorschau, sobald gewählt.
Stil: vanilla-nah, 64×64-Textur, Modell aus wenigen Quadern wie Vanilla-Mobs.

## Mob 1 – End-Mob (niedlich, getarnt)

**Name-Vorschläge:** Endling, Steinkauz (End-Variante), Kieselpuff, Purpling.

- **Aussehen:** kleiner, runder Körper (≈ 0,6 × 0,5 × 0,7 Blöcke), vier Stummelbeine, kurze Ohren. Rücken in Endstein-Textur mit 2–3 Pixel-Kratern, Bauch heller (helles Creme). Augen dunkelviolett mit einem Lichtpunkt; wenn er schläft, sind sie geschlossen.
- **Tarnung:** liegt flach und reglos auf Endstein, sieht dann aus wie ein Steinbuckel (Modell-Pose „liegend“ = fast ein flacher Quader). Erst bei Annäherung (≤ 4 Blöcke) hebt er den Kopf.
- **Spawn:** sehr selten, nur auf Endstein der äußeren Inseln, Rudel 3–5, nie in der Nähe der Hauptinsel.
- **Verhalten:** friedlich, flieht vor dem Spieler, rollt sich bei Schaden zusammen (Schadensreduktion). Frisst Chorusfrüchte, damit zähm- oder züchtbar.
- **Nutzen:** Gezähmt warnt er durch Fiepen vor Endermen in der Nähe, die einen ansehen könnten. Fallen lässt er „Endstaub“ (Färbemittel bzw. Zutat).
- **Textur-Varianten:** Endstein (Standard), Purpur (selten), Obsidian-dunkel (sehr selten).

## Mob 2/3 – freundliche Golems (Vorschläge zur Auswahl)

1. **Kupfergolem (Lager-Golem):** sortiert Items aus einer Quell-Truhe in Zieltruhen mit passendem Inhalt. Oxidiert mit der Zeit und wird langsamer; Wachs stoppt das, eine Axt macht ihn wieder blank. Passt zu den Kupfertruhen.
2. **Amethystgolem (Klang-Golem):** verstärkt Notenblöcke und Plattenspieler in seiner Nähe (Brücke zu den Verstärkern). Leuchtet leicht, macht Kristallklänge, repariert Amethyst-Werkzeuge langsam.
3. **Moosgolem (Gärtner):** pflanzt Setzlinge nach, düngt Felder langsam, erntet reife Pflanzen in einem kleinen Radius und legt sie in eine Truhe.
4. **Tiegel-Golem (Schmied):** steht an einem Tiegel, legt Erz nach und füllt Ergebnisse ab. Gebaut aus Eisenblöcken mit einem Tiegel als Kopf.
5. **Schneewächter (Frostgolem):** stärkerer Schneegolem, der Eis-Splitter verschießt und Mobs verlangsamt. Schmilzt nicht in warmen Biomen, wenn er einen Eiskern trägt.

## Schwere Bosse (stärker als der Wither)

1. **Der Ausgehöhlte (End-Boss, Phase 2 nach dem Drachen):** riesiger Shulker-Koloss aus Purpurplatten. Phase 1: Panzerplatten müssen einzeln abgeschossen werden. Phase 2: Levitationsfelder und Teleport-Wellen. Phase 3: zieht Inseln zusammen. Drop: Shulker-Kern für Shulkerkisten mit doppelter Größe.
2. **Aschenkönig (Nether, Bastion-Tiefe):** Feuer-Golem aus Basalt und Magma, ruft Lohen-Wellen, lässt den Boden zu Lava werden (Seelenlava-Bezug, Seelenbrand). Nur verwundbar, während er abkühlt (Eis-Splitter, Schnee). Drop: Glutherz (Brennstoff für den Tiegel, Hitzestufe „extrem“ ohne Quelle).
3. **Tiefenwächter (Deep Dark, unter der Ancient City):** schwerer als der Warden. Blind, reagiert auf Vibrationen; der Spieler muss mit Schleichen, Wolle und Ablenkung (Schneebälle, Pfeile) arbeiten. Rammattacke durchbricht Blöcke. Drop: Echo-Kern.
4. **Der Uralte Golem (Oberwelt, Trial-Chamber-Tiefe):** aktiviert durch alle Trial Keys. Wechselt die Elementform (Kupfer: Blitz, Eisen: Panzer, Gold: Tempo, Diamant: Spiegelung von Schaden). Drop: Golem-Kern für eigene Golems.
5. **Leere-Wyrm (Außenenden):** Schlange aus Endstein-Segmenten, gräbt sich durch Inseln. Jedes Segment hat eigene Lebenspunkte; der Kopf ist nur nach Abtrennen des Hinterteils verwundbar.

## Mob 4 – Shellker (End, Gateway-Wächter) – Konzept N32, 2026-10-10
Queue N23/N24 (Wortlaut dort). Vorschau: `<preview-dir>/concepts/shellker.png`, `shellker_helm.png`.

- **Wesen:** Shulker-Variante mit harter Schale, 1×1×1. Öffnet zum Schießen alle Seiten, wird dabei **nicht** größer (schießt aus einem 1×1-Loch, Öffnung nur ein Spalt/ein Auge). Kein Teleport; nur per (Klebe-)Kolben verschiebbar (Kolben-Schub = bewusster Weg am Wächter vorbei).
- **Schaden:** geschlossen unverwundbar (wie Gürteltier, Hit-Sound „Stein“); offen normal. Leben = 4× Shulker (120 statt 30).
- **Gateway:** je End-Gateway genau 4 Stück, auf den 4 Bedrock-Nachbarfeldern der Öffnung (Portalhöhe; genaue Positionen bei Umsetzung gegen Vanilla-Struktur prüfen). Durch die Tarnung (Textur wie Grundgestein) fallen sie dort nicht auf; Zugang erst nach Besiegen oder Wegschieben. Fake-Gateways (End-Struktur) bekommen keine.
- **Projektil:** wie Shulker-Kugel, 1,5× schneller, Effekt **Schwerkraft (High Gravity)** statt Schweben: alle Aufwärtskräfte (Sprung, Levitation, Elytra-Auftrieb) auf 10 %, Elytra gleitet 10× schlechter. Dauer wie Schweben (10 s), Stufe ohne Staffelung. Als Trank: Brauzutat **Shellker-Schale** (Seltsamer Trank → Trank der Schwerkraft 3:00 / verlängert 8:00; Wurf-/Verweiltrank wie Vanilla).
- **Vermehrung:** wie Shulker (Shulker-Kugel trifft Shulker → Klon; hier trifft die Shellker-Kugel einen Shellker).
- **Erschaffen:** Shulker + Shellker-Schale, Rechtsklick „anziehen“ → Shulker wird Shellker (Schale verbraucht). Drop Ø 2,5 Schalen (Looting +0,5/Stufe). Farmen nur über Umwandlung von Shulkern, nie natürlich außerhalb der Gateways.
- **Textur:** Schale wie Grundgestein (dunkle Grautöne, Rauschen, Risse), Auge/Spalt nur beim Öffnen heller Violett-Lichtpunkt (Tarnung bleibt bis zum ersten Schuss). Vorschau zeigt geschlossen/offen.
- **Helm („Shellker-Helm“, NEU, kein Ersatz des Schildkrötenhelms):** 5 Schalen in Helmform. Rüstung 3, Zähigkeit 1, Rückstoßresistenz 10 %, Haltbarkeit ≈ Eisen. Träger ist immun gegen den Schwerkraft-Effekt (nur gegen den Effekt, nicht gegen Schüsse) und erleidet 20 % weniger Fallschaden. Schmiedevorlagen/Besatz wie bei Vanilla-Helmen, Textur bedrockgrau mit violettem Visier-Spalt.
- **Modul:** Simple Mobs (siehe Nächste Schritte); Helm/Trank im selben Modul, Trank-Zutat nur bedingt in SimpleBuilding-Tiegel eintragen (Modul-Unabhängigkeit).

**Offene Besitzer-Fragen (Empfehlung zuerst):**
1. Helm-Werte wie oben (Empfehlung) oder reiner Effekt-Helm (Schwerkraft-Immunität + Optik, keine Rüstung)?
2. Dauer Schwerkraft 10 s ok? (Empfehlung ja; Trank 3:00.)
3. Gateway-Wächter nur im Haupt-Gateway-Ring oder auch auf den Außeninseln-Gateways? (Empfehlung: alle echten Gateways.)
4. Kolben-Verschieben: auch Beobachter/Redstone-Schub als „bewegt“? (Empfehlung: nur Kolben.)

## Mob 5 – Seelenfeuer-Lohe (Soulfire Blaze) – Konzept N32, 2026-10-10
Queue N24 nennt nur den Namen; alles Folgende ist **Vorschlag**. Vorschau: `<preview-dir>/concepts/soulfire_blaze.png`.

- **Wesen:** blaue Blaze-Variante, gleiche Größe/Flugverhalten, Stäbe und Kern in Seelenfeuer-Blau/Petrol, Kern hell-cyan. Feuerimmun.
- **Spawn:** Seelensand-Täler (selten, Rudel 1–2, über Seelensand/Seelenboden) und Seelenfeuer-Variante des Festungs-Spawners (Chance 25 %, Config). Kein Spawn in Basaltdeltas.
- **Angriff:** feuert Drei-Kugel-Salven aus Seelenfeuer: setzt Ziel in Seelenbrand (Vanilla-Seelenfeuer, Feuerresistenz hilft). Verletzlich gegen Schneebälle (wie Blaze).
- **Drop:** Seelenfeuer-Rute (0–1, Looting). Verwendung (Vorschlag): Brennstoff/Hitzequelle „hoch“ ohne Rauch im Tiegel (Queue: Hitzequelle statt Brennstoff), Brauzutat für Trank der Seelenfeuer-Resistenz, Zutat für Seelenlava-Bezüge (Seelenlava-Eimer = extrem, bleibt vom Mob unabhängig).
- **Modul:** Simple Mobs (Soulfire-Texturen in SimpleBuilding-Stilreferenz; Tiegel-Anbindung nur bedingt).

**Offene Besitzer-Fragen (Empfehlung zuerst):**
1. Spawn nur Seelensand-Täler (Empfehlung) oder auch Festungen?
2. Rute als Tiegel-Hitzequelle „hoch“ (Empfehlung) oder ein eigener Wert?
3. Soll sie Wasser meiden/Schaden nehmen wie Blaze? (Empfehlung: ja, Wassereimer/Schneeball.)

## Nächste Schritte
- Besitzer wählt Name, Variante und Golems/Bosse.
- Danach Textur- und Modell-Vorschau (Bild, mehrere Posen, vanilla-nah) für Mob 1 und die gewählten Golems/Bosse.
- Umsetzung als eigene Sub-Mod (z. B. „simple mobs“), siehe KONZEPT-SUPERMOD-SUBMOD.
