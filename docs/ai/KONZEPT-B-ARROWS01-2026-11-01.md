# Konzept Simple Arrows (b-arrows01, B, 01.11.2026)

Nur Konzept und Vorschau (`abnahme-b/b-arrows01-preview.html`, Antwortcode `ARROWS01|…`). Kein Spielcode, kein Gradle. NUR MC 26.3 Fabric.

## 1. Ist-Stand (aus Code und Queue, Basis hub/claude-wave1)
- **Befiederungstisch** liegt heute im SB-Kern: `common/src/shared/java/com/simplebuilding/fletching/` (`ArrowParts` 9 Spitzen / 5 Schäfte / 2 Befiederungen, `CraftedArrow` extends `AbstractArrow`, `CraftedArrowItem`, `FletchingMenu` 3 Teil-Slots + Ergebnis 4 Pfeile, `FletchingRecipe` mit `Kind` TIP/SHAFT/FLETCHING als Rezeptbuch, `ArrowRecovery`), Rezepttyp `simplebuilding:fletching` (nur `McVersion.FLETCHING`), Menü in `ModScreenHandlers`, Lang `container.simplebuilding.fletching.*`, Test `gametest/FletchingGameTest`. Layout WD25-12 (Reihe mittig, Pfeil-Logo, Leer-Slot-Icons) ist abgenommen. Spitzen nutzen SB-Kernitems (`DIAMOND_PEBBLE`, `NETHERITE_NUGGET`, `DIAMOND_ROD`) und Enderit (seit IG31-13 in Simple Ores).
- **Silberpfeil** ist in Simple Ores gebaut (a-ores31): `OresRegistry` (Entity `simpleores:silver_arrow`, Item, Dispenser), `SilverArrow` (+`silverArrowUndeadBonus` gegen `#minecraft:undead`, Standard 3, 0 = Vanilla), `SilverArrowItem`, Rezept N/S/F → 4 (Silberklumpen), Tags `minecraft:arrows`, Wiki-Eintrag, Tests in `OresTests`. Kein Modul `simplearrows` existiert.
- Weitere Pfeil-Stellen (bleiben, wo sie sind): `AbstractArrowPickupMixin` + Köcher (Simple Storage), `AbstractArrowRecoveryMixin` (REV24-F-13), bremsende Pfeile (TR31-R Stufe 3, Simple Mobs/Trials), Trainingspuppe wertet die Spitzenart.
- Queue offen: **WE25-9** (Trank-Slot/Trank-Pfeile, komplett offen), **VIS24-9** (Fletching-Freiraum oben → Wahl Besitzer: `FLETCH=gravur`), **ORES31-PFEIL** (Besitzer: Pfeil = Ores, „aber in Simple Arrows als Erweiterung der Possibilities“).

## 2. Bereits entschieden (nicht erneut fragen)
- Silberpfeil gehört in **Simple Arrows**, gebaut wird er nur mit Simple Ores (Silber). Silberspiegel bleibt Deko in Ores.
- Befiederungstisch-Freiraum oben: **Gravur** (Pfeil-/Feder-Gravur wie bei anderen Stationen).
- Alle Zahlen des Tischs sind fest (B14 „keine Config, nichts ausnutzbar“) – gilt weiter für neue Teile.

## 3. Einordnung: Sub-Mod von Simple Building (Pflicht-Sub-Mod), allein spielbar
- `modules/simplearrows`, `parent: simplebuilding` (wie Ovens/Ores/Gadgets), `requires: [simplelib]`, `optional: [simpleores, simplestorage, simplemobs, jei]`. Super-Mod requires es, Config-Punkt „Enable Simple Arrows“ oben.
- **Allein spielbar** (PRINZIPIEN-MODUL-UNABHAENGIGKEIT): Tisch + Vanilla-Spitzen (Feuerstein, Kupfer, Eisen, Gold, Amethyst, Prismarin) + Schäfte (Stock, Endstab, Lohenrute, Windrute) + Befiederung (Feder, Phantomhaut) laufen ohne jedes andere Mod. Spitzen/Schäfte aus Fremd-Mods (Diamantkiesel, Netherit-/Enderit-Splitter, Diamantstab) erscheinen nur bei geladenem Besitzer-Mod (Rezeptbedingung `fabric:all_mods_loaded`, Slots nehmen Tag-Zutaten `c:`-Tags). Silberpfeil/-spitze nur mit Simple Ores (Tag `c:nuggets/silver`).
- **Warum nicht Ores / QoL / Tools?** Ores = Materialien, kein Spielsystem; ein Tisch mit 9 Spitzen passt nicht dahin. QoL ist für Komfort ohne neue Items. Simple Arrows hat Items, Entitäten, Menü → eigenes Modul unter SB, wie Ovens (SB-Küchen) bzw. Gadgets.
- **Warum nicht in SB lassen?** SB-Kern soll nur Übergreifendes enthalten (Supermod-Konzept); der Tisch ist abgrenzbar (eigenes Paket, eigener Rezepttyp).

## 4. Inhalt und Stufen
**Stufe 1 – Modul + Silberpfeil (Aufwand M).** `simplearrows` anlegen (`tools/newmod.py`, modtree + MODSTRUKTUR neu, Standalone-Target Fabric 26.3). Silberpfeil aus Simple Ores verschieben: Item/Entity/Renderer/Tests/Wiki → `simplearrows:silver_arrow`; **IdMoves** `simpleores:silver_arrow` → `simplearrows:silver_arrow` (Item, Entity-Typ, Rezept, Fortschritt, Tags); Rezept lädt nur mit `simpleores` (Bedingung), ohne Ores gibt es ihn nicht. Gleiche Werte: +3 gegen `#minecraft:undead` (Config `silverArrowUndeadBonus` zieht um, 0 = Vanilla). Ores verliert Silberpfeil-Code, behält Silber.
**Stufe 2 – Befiederungstisch umziehen (Aufwand L, Risiko mittel).** Paket `fletching` aus dem Kern nach `simplearrows`; **IdMoves** `simplebuilding:fletching` (Rezepttyp, Rezeptbuch-Kategorien `fletching_tip/shaft/fletching`, Menütyp), Item-Komponenten/Stack-Daten gespeicherter Pfeile (Codec liest unbekannte Teile tolerant, `Shaft.byName` macht das schon), Lang-Schlüssel, Test `FletchingGameTest`. Kern-Items für Spitzen bleiben beim Besitzer; Spitzenliste wird datengetrieben (Rezept-JSON je Teil + Tag), nicht mehr Enum mit Kern-Verweisen – Voraussetzung für „allein spielbar“. Kein neues Verhalten, Welten behalten ihre Pfeile.
**Stufe 3 – Gravur (FLETCH=gravur, Aufwand S).** Obere Freifläche des Menüs (über der Slot-Reihe, siehe WD25-12) bekommt eine eingravierte Pfeil-/Feder-Zeichnung im Stil der Stationen (Vorschau 3 Varianten A–C, nur Textur im Hintergrund, kein neuer Code außer Blit). Kontrast ≥ 4,5:1 zu Slot-Beschriftungen (UI-Lesbarkeit); Endholz-Prozess (A8b) nach Wahl.
**Stufe 4 – Trank-Slot / Trank-Pfeile (WE25-9, Aufwand M–L, Wahl).** Siehe Ideen. **Stufe 5 – weitere Pfeile** nur die abgehakten, je Pfeil ein Beispiel zuerst.
Tests je Stufe: Rezept lädt (mit/ohne Ores), Tisch-Ergebnis 4 Pfeile, Schaden/Bonus, Alt-Pfeile aus Welt laden (IdMove-Test), Standalone-Start ohne SB. Gametests Fabric-Adapter + Katalog (Regel 7), Client-Mixin-Check nicht nötig ohne Mixin.

## 5. Ideen mit Vanilla-Nutzen / Eingriffsstärke (A12) und Aufwand
Alle neuen Pfeile außer Silberpfeil sind **Spitzen/Zusätze am Tisch** (kein neues Item pro Pfeil, nutzen `CraftedArrow`), Standard an, einzeln per Server-Schalter abschaltbar.
| Idee | Vanilla-Nutzen | Eingriff | Aufwand |
|---|---|---|---|
| **Silberspitze am Tisch** (zusätzlich zum Item): +2 gegen Untote, Silber-Nugget | Untote bekämpfen ohne Bann-Schwert; Gold-Spitze macht dasselbe → Überschneidung | gering | S |
| **Trank-Slot / Trank-Pfeile** (WE25-9): vierter Slot (optional, Config, aus ohne Wahl) nimmt Trank/Wurf-/Verweiltrank; 1 Flasche → 4 Pfeile mit Teilen + Effekt (Dauer wie Vanilla-Tipped, 1/8) | Vanilla: Tipped-Pfeile nur aus Verweiltrank + 8 Pfeilen in der Werkbank; hier kombinierbar mit Spitze/Schaft | gering–mittel (Effektpfeile gibt es schon) | M–L |
| **Lichtpfeil** (Glowstone-/Fackelspitze): setzt am Block eine Fackel bzw. kurzes Licht (15 s) am Einschlag | Höhlen aus der Ferne ausleuchten; ersetzt Fackeln nicht dauerhaft | gering | M |
| **Frostpfeil** (Schneeball/Eisspitze): Verlangsamung 3 s, Pulverschnee-Spur; teilt Effekt mit TR31-R „bremsende Pfeile“ | Kampf aus der Distanz, Kontrolle statt Schaden | gering | S–M |
| **Netzpfeil** (Spinnennetz/Faden-Spitze): setzt am Ziel Spinnweben 5 s, verschwinden von selbst | Fluchtweg/Kite gegen Creeper, Schmieden von Fallen | mittel (setzt Block, abschaltbar) | M |
| **Signalpfeil** (Feuerwerkstern-Spitze): Farbe/Partikel am Einschlag, markiert Ort, 0 Schaden extra | Markieren für Mitspieler/Höhlen, passt zu Feuerwerk | sehr gering | S |
| **Saatpfeil** (Samen-Spitze): pflanzt Samen am Einschlagfeld (Farmland) | Fernsaat auf Feldern | gering (nur Farmland, Samen verbraucht) | M |
Nicht vorgeschlagen (A12 zu stark): Sprengpfeil, Zielsuch-Pfeil, Teleportpfeil.

## 6. Risiken
- Stufe 2 IdMoves (Menü, Rezepttyp, Pfeil-Stapel-Daten) – alte Welten testen; Rezeptbuch-Kategorien sind Registry-Einträge.
- Datengetriebene Spitzen: Rezept-JSON-Schema (`FletchingRecipe`) wächst um `requires`-Bedingung.
- Silber: Tag `c:nuggets/silver` vs. `simpleores:silver_nugget` – Tag nutzen, damit andere Silber-Mods passen.
- Trank-Pfeile: `CraftedArrow` braucht `PotionContents`-Komponente + Treffer-Effekt (wie `Arrow`), Anzeige/Farbe im Item-Modell.

## 7. Verifikation später
Plan-Abgleich je Stufe, `module-simplearrows-fabric-263` + `-standalone-fabric-263`, `fabric-263 --filter 'simplearrows:*'`, Silber-Umzug-Test, Clientsicht Tisch/Gravur, ein sbt-Lauf am Ende.
