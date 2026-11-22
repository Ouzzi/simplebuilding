# Simple Slabs - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Halbstufen - 4 Pixel dünn - aus jedem Block mit Stufe, frei in einer Zelle gestapelt und aufrecht als seitliche Stufen.

## Release-Kanal: Beta

- Versionsnummer: `0.1.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Slabs

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Slabs bringt eine Halbstufe (eine halbe Stufe, 4 Pixel) für jeden Vanilla-Block mit Stufe, dazu die Stufen anderer Simple-Mods, solange diese installiert sind. Jede der vier Lagen einer Blockzelle lässt sich in beliebiger Kombination füllen, so baust du 4-Pixel-Simse, Lücken und dünne Dächer; die seitliche Halbstufe, ein eigenes Item, steht aufrecht an einer Wand und wird bei zwei Lagen zur seitlichen Stufe. Sand- und Kies-Halbstufen fallen, und Gras wird vom Biom eingefärbt (beides mit Simple Blocks); Kupfer altert. Geschnitten wird in der Formsäge (oder Steinsäge), zurück zur Stufe geht es per Rezept.

## Funktionen

- **Halbstufen: 4-Pixel-Lagen jedes Blocks mit Stufe** - Eine Halbstufe ist eine halbe Stufe: 4 Pixel hoch.
- **Seiten-Halbstufe und Seiten-Stufe** - Die Seiten-Halbstufe ist für jedes Material ein eigenes Item: Rechtsklick auf die Seite eines Blocks stellt sie aufrecht an diese Seite, 4 Pixel dünn.
- **Stufe und Halbstufe in einem Block** - Eine Halbstufe auf ihre eigene Stufe (unten oder oben, nicht doppelt) fügt eine Lage hinzu: Die Stufe wird zum Halbstufen-Block mit ihren zwei Lagen und der neuen, Wasser bleibt.
- **Abbauen und Wasser** - Abbauen eines Halbstufen-Blocks droppt für jede belegte Lage eine Halbstufe, eine Seitenform je Lage eine Seiten-Halbstufe.
- **Sand, Kies und Gras** - Sand-, Roter-Sand- und Kies-Halbstufen fallen wie Sand.
- **Kupfer-Halbstufen altern** - Ungewachste Kupfer-Halbstufen altern wie Vanilla-Kupfer: angelaufen, verwittert, oxidiert.
- **Formsäge und zurück** - Die Formsäge von Simple Blocks schneidet aus einer Stufe 2 Halbstufen oder 2 Seiten-Halbstufen und aus ihrem vollen Block 4; ohne Simple Blocks macht das die Vanilla-Steinsäge.
- **Halbstufen der Blöcke anderer Mods** - Halbstufen der Blöcke anderer Mods (SimpleBuilding, Simple Blocks, Simple End, Simple Ores, Simple Enchanting) gibt es nur, solange dieser Mod geladen ist, samt Rezepten.

## Im Vanilla-Spiel

Bringt dünne Bauteile für feinere Details, geschnitten aus Blöcken, die schon eine Stufe haben; sie fügt nur eigene Blöcke und Rezepte hinzu und ändert keinen Vanilla-Block und kein Vanilla-Rezept.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** SimpleBuilding, Simple Blocks, Simple End, Simple Ores, Simple Enchanting, JEI

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Sub-Mod** von SimpleBuilding: im Download der Super-Mod enthalten, und sie läuft auch allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simpleslabs
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: decoration
- Tags: building-blocks, slabs, decoration
- Umgebung: Client und Server

