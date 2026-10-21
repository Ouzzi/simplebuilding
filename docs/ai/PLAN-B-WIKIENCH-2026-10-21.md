# Plan b-wikiench (21.10.2026)

Queue „(B, b-wikiench, 21.10.)“, Besitzer: „eine enchantment ansicht im wiki genau wie im spiel mit den hovers,
dass es in spiele ansicht geprüft werden kann.“

## Ist-Zustand

- Wiki = handgeschriebene `wiki/index.html`, Daten je Modul aus `wiki/generate.py` / `wiki/modules.py`
  (`collect_enchantments`: Name EN/DE, `.desc`, Max-Stufe, Gewicht, Tags – **ohne** `min_cost`/`max_cost`).
  Seiten je Modul (`D` = Daten des gewählten Moduls); Liste + Einzelseite `?tab=enchantments`/`?ench=`.
- Keine Spielansicht: kein Tooltip im Minecraft-Stil, kein Zaubertisch, keine Vanilla-Texte
  (`container.enchant.*`, Vanilla-Verzauberungsnamen) im Wiki. Kein Minecraft-Font (bleibt so: Mojang-Asset).
- Fremd-Namensräume fehlen im Modul-Export (z. B. `simplequalityoflife:thrift` liegt in simpletools).
- Am Tisch (Tag `minecraft:in_enchanting_table`) sind aus der Familie nur `fast_chiseling`, `kinetic_protection`,
  `simplequalityoflife:thrift`; alle anderen sind Schatz/Loot/Handel.
- Vanilla-Logik (26.3, `minecraft-merged.jar` per javap geprüft): `EnchantmentHelper.getEnchantmentCost`,
  `selectEnchantment`, `getAvailableEnchantmentResults`, `EnchantmentMenu.slotsChanged/getEnchantmentList`
  (Buch: bei >1 Treffer eins zufällig entfernt), Tooltip `EnchantmentScreen` (Hinweis weiß mit grauem Namen,
  Leerzeile, Lapis grau/rot, Stufen grau; zu wenig Level → „Level Requirement“ rot). Farben Runen #685E4A,
  gesperrt #342F25, Stufenzahl #80FF20 / gesperrt #407F10.
- Headless-Browser auf sb-test: `playwright-core` unter `/var/tmp/sb/pw/node_modules`, Chromium im
  ms-playwright-Cache (Muster `wiki/tests/time_ui.cjs`).

## Umsetzung

1. **Export `wiki/enchanting.py` → `wiki/data/enchanting.js`** (`window.WIKI_ENCHANTING`), aufgerufen aus
   `generate.py` (wie Vanilla-Rezepte: ohne Client-Jar bleibt die eingecheckte Datei; `--check` vergleicht).
   - Familie: alle `data/*/enchantment/*.json` und `data/*/tags/{enchantment,item}/**` der 26.3-Wurzeln
     (SimpleBuilding `build/wiki-lines/26.3` + `src/main/resources`, jedes Modul `generated`/`shared`/Loader),
     Sprachdateien `assets/*/lang/{en_us,de_de}.json` derselben Wurzeln.
   - Vanilla: Verzauberungen + Tags aus dem Client-Jar, `en_us` aus dem Jar, `de_de` aus dem Loom-Asset-Index
     (nur Texte: Verzauberungsnamen, `container.enchant*`, `enchantment.level.*`, Namen der Beispiel-Items).
     Keine Pixel.
   - Je Verzauberung: Name/Beschreibung EN/DE, Stufen, Gewicht, `minCost`/`maxCost`, `table`, `curse`,
     `treasure`, aufgelöste Ausschlussliste. Je Beispiel-Item: Name, Verzauberbarkeit (Vanilla-Konstanten aus dem
     Code bzw. `items.json`; sonst angenommen 10 und markiert), Icon-Pfad, Tisch-Kandidaten (Primär-Items, Buch =
     alle), passende Familien-Verzauberungen in Varianten ohne Ausschluss-Konflikt (für die Tooltips).
   - Beispiel-Items: Buch, Diamant-Spitzhacke/-Schwert/-Brustpanzer/-Hose/-Stiefel, Bogen + je
     Familien-Verzauberung ein Vertreter ihres Item-Tags (eigener Namensraum, „diamond“ bevorzugt).
   - `minCost`/`maxCost` zusätzlich in `collect_enchantments` (Verzauberungsseite zeigt sie).
2. **Seite `?tab=enchanting`** (`wiki/assets/enchanting.js`, UMD, Logik auch unter node testbar):
   - Zaubertisch-Fenster in CSS (eigene Farben/Rahmen, Vanilla-ähnlich): Item-Platz, Lapis-Platz, drei
     Angebotszeilen mit Runen-Andeutung (eigene Wortliste → Unicode-Runen), Kosten-Kugel + Zahl, Bücherregal-Zahl.
   - Regler: Item, Bücherregale 0–15, Erfahrungsstufe, Lapis, Seed („Neu würfeln“). Zufall: Java-LCG
     (LegacyRandomSource) + Vanilla-Formeln; Hinweis, dass Seed/Tag-Reihenfolge einer echten Welt nicht garantiert
     gleich sind.
   - Hover/Fokus/Tippen: Tooltip wie im Spiel; Klick verzaubert (Ergebnis-Tooltip mit allen gewürfelten Stufen).
   - Abschnitt „Tooltips“: je Familien-Verzauberung ein verzaubertes Buch (Max-Stufe), je Beispiel-Item die
     passenden Familien-Verzauberungen (Varianten ohne Konflikt), Namensfarben wie Vanilla (Buch gelb, Item aqua,
     Verzauberung grau, Fluch rot), Beschreibung (falls vorhanden) als markiertes Wiki-Extra.
   - Liste „nie am Tisch“ (Familie ohne `in_enchanting_table`) und Klartext-Tabelle der Angebote.
   - EN/DE über `LANG`, hell/dunkel (Seitenrahmen über Wiki-Tokens; das Spielfenster bleibt spielgetreu),
     Handy (Fenster skaliert, Tooltip unter dem Finger bzw. unter dem Fenster).
   - Einbindung in `index.html`: Route `enchanting`, Seitenleiste, Link auf Verzauberungsliste und -seite.
3. Vanilla-Texturen `book`, `enchanted_book`, `lapis_lazuli` und Beispiel-Items über den vorhandenen
   Jar-Kopierpfad (git-ignoriert) referenzieren.

## Tests

- `wiki/tests/test_enchanting.py`: Export (Kosten, Tisch-Tag inkl. Fremd-Namensraum, Ausschluss, Varianten
  konfliktfrei, Beispiel-Items decken jede unterstützte Familien-Verzauberung ab), Datei aktuell/gültig;
  `wiki/tests/enchanting_core.cjs` (node): Java-LCG gegen bekannte `java.util.Random`-Werte, Kosten-Formel,
  Angebote deterministisch, Buch-Regel, Tooltip-Zeilen.
- sb-test: Wiki erzeugen (-gen), `./gradlew check -q`; `wiki/tests/enchanting_ui.cjs` (playwright-core,
  headless): DE/EN, Desktop + 390 px, Hover → Tooltip-Texte, Klick → Ergebnis, Kontrast; Screenshots.
- Demo-HTML ohne Vanilla-Pixel im Scratchpad (nicht veröffentlichen).

## Grenzen (werden auf der Seite genannt)

- Angebote nach Vanilla-Formeln mit eigenem Seed; echte Welt-Seeds und Tag-Reihenfolge können abweichen.
- Verzauberbarkeit von Modul-Items ohne Export angenommen (10). Seltenheit der Mod-Items nicht exportiert
  (angenommen „gewöhnlich“ → verzaubert aqua). Keine Attribut-/Haltbarkeitszeilen im Tooltip.
- Spielfarben im Fenster (Stufenzahl grün auf Beige) erreichen A7 nicht – spielgetreu gewollt; dieselben Angaben
  stehen im Tooltip und in der Klartext-Tabelle mit ≥ 4,5:1.
