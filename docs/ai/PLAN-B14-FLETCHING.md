# Plan B14 – Befiederungstisch und Pfeil-Palette (2026-10-01)

Besitzer-Vorgaben: Backlog B14 und Antworten 11, 12, 20 in `docs/ai/BACKLOG-2026-10-01.md`.
Hauptlinie 26.3, Fabric zuerst, dann gebündelt NeoForge/Forge (Flag `McVersion.FLETCHING`).

## Datenmodell

- **Ein Item** `simplebuilding:crafted_arrow` (erbt `ArrowItem`) mit Komponente `simplebuilding:arrow_parts`
  = {tip, shaft, fletching} (je eine ID aus festen Listen). Kein Item je Kombination.
- Aussehen: Item-Modell `minecraft:composite` aus drei `minecraft:select` auf `custom_model_data` (Strings
  `tip`, `shaft`, `fletching` werden beim Herstellen gesetzt) → 9 + 4 + 2 Teiltexturen statt 72 Modelle.
- Name: aus den Teilen zusammengesetzt (z. B. „Eisen-Pfeil mit Lohenschaft“), Tooltip nennt Teile, keine Zahlen
  außer den belegten.

## Teile und Effekte (freigegeben)

| Teil | Material | Wirkung |
|---|---|---|
| Spitze | Feuerstein | Vanilla (Schaden 2,0) |
| Spitze | Kupfer-Nugget | +2 gegen Ertrunkene |
| Spitze | Eisen-Nugget | +0,5; +2 gegen Zombies |
| Spitze | Gold-Nugget | +2 gegen Untote |
| Spitze | Diamantkiesel | +1 |
| Spitze | Netherit-Nugget | +1,5; durchbohrt 1 Ziel |
| Spitze | Enderit-Nugget | +1,5; erste Sekunde ohne Schwerkraft |
| Spitze | Amethyst-Splitter | zersplittert: 1 Schaden im Umkreis 1,5 |
| Spitze | Prismarin-Splitter | unter Wasser ungebremst |
| Schaft | Stock | Vanilla |
| Schaft | Endstab | halbe Schwerkraft, Leuchtspur |
| Schaft | Lohenrute | Ziel brennt zusätzlich 3 s (über Flamme hinaus) |
| Schaft | Böenrute | Windladungs-Stoß beim Treffer, zusätzlich zu Bogen-Effekten |
| Befiederung | Feder | Vanilla |
| Befiederung | Phantomhaut | 30 % weniger Schwerkraft |

Unendlichkeit wirkt nur auf Vanilla-Pfeile (`crafted_arrow` ist nie unendlich). Vanilla-Pfeilrezept bleibt; Spezialpfeile nur am
Befiederungstisch. Eine Herstellung = 1 Spitze + 1 Schaft + 1 Befiederung → 4 Pfeile (wie Vanilla).
Alle Zahlen als Konstanten, serverseitig; keine frei einstellbaren Werte (nichts exploitbar).

## Schritte

1. Server: Komponente, Item, Pfeil-Entity (`CraftedArrow` erbt `Arrow`), Effekte, Unendlichkeit, Tests (Schaden je Spitze
   gegen Zielart, Schwerkraft, Durchbohren, Brennen, Splitter, Wasser). Fabric.
2. Befiederungstisch-Menü (`FletchingMenu`: Spitze/Schaft/Befiederung diagonal, Ausgabe), Rechtsklick auf den Vanilla-Tisch
   öffnet es; Server-Validierung der Slots. Tests (Ergebnis, Verbrauch, falsche Teile abgewiesen, Shift-Klick).
3. Bildschirm im Stil Schmiedetisch/Tinkers (diagonale Felder), Rezeptbuch-Panel im Vanilla-Stil: Kategorien Spitze/Schaft/
   Befiederung, je Zeile Material-Textur (Knopf) + Zweck + Effekt; Klick legt das Material aus dem Inventar in seinen Slot
   (Server-Paket mit Validierung).
4. Texturen (Teilschichten, GUI, Icons) im Besitzerstil mit Vorschau; Lang EN/DE beide Orte; eigener Creative-Tab „Pfeile“
   (alle Kombinationen, sortiert Spitze → Schaft → Befiederung); Suchtab; Wiki; JEI-Kategorie optional.
5. NeoForge/Forge-Verdrahtung (Menü-, Entity-, Komponenten-Registrierung, Screen) gebündelt nach Fabric.
6. Gate, Push.

## Risiken

- Bogen/Armbrust-Interaktion: `ArrowItem#createArrow` + `isInfinite`; Multishot/Piercing des Bogens müssen weiter wirken.
- `custom_model_data`-Select muss Kombinationen ohne Lücken abdecken (Fallback = Vanilla-Pfeil).
- Rezeptbuch-Klick: nur Items aus dem eigenen Inventar, Server prüft Slot-Eignung (kein Item-Duplizieren).
