# Simple Money - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Herstellbare Währung, gewichteter Dorfbewohnerhandel und Geld in Schatztruhen für Survival-Welten.

## Release-Kanal: Beta

- Versionsnummer: `0.9.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Money

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Money bringt eine Währung, die du selbst herstellst, gewichtete Dorfbewohner-Angebote und Geld in der Beute von Schatztruhen.

## Funktionen

- **Spezialpapier** - Aus zwei Papier und einer Honigwabe hergestellt; wird mit einem Eisenbarren und Harzfaser zu einem Banknotenrohling geschmiedet.
- **Spezialfaser** - Aus Kupfernuggets, Goldnuggets, Amethystscherben und Diamanten hergestellt; veredelt mit einem Goldbarren einen Banknotenrohling.
- **Harzfaser** - Aus Eisennuggets, Harzklumpen, Honigwaben und Knochenmehl hergestellt; dient zum Schmieden von Banknotenrohlingen.
- **Banknotenrohling** - Eine ungewöhnliche Schmiedezutat aus Spezialpapier, Harzfaser und einem Eisenbarren.
- **Veredelter Banknotenrohling** - Eine ungewöhnliche Zutat aus einem veredelten Banknotenrohling.
- **Roher Geldschein** - Ein seltenes Herstellungsergebnis.
- **Geldschein** - Währung für die zusätzlichen Dorfbewohner- und fahrenden Händlerangebote.
- **Eine Preislogik** - Alle 47 eigenen Angebote folgen einer Regel: ein Geldschein ist 3 Diamanten oder 16 Smaragde wert (ein Schein kostet zwei Diamanten plus Gold, Eisen, Harz und Schmelzofen-Zeit).
- **Geld in Truhen** - Ein unabhängiger Versuch pro Truhe.
- **Serverkonfiguration**
- **Geld-Verknüpfungen: Zusatzhandel** - Ergänzt reine Geld-Kaufangebote für geladene Mods, ohne deren alten Handel zu entfernen.
- **Befehle** - /simplemoney config zeigt und ändert jede Option von config/simplemoney.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte).

## Im Vanilla-Spiel

Bringt eine Währung, gewichteten Dorfbewohnerhandel und Geld in Schatztruhen, damit Survival-Welten eine Wirtschaft bekommen. Einen Geldschein herzustellen ist absichtlich teuer (zwei Diamanten plus je Gold, Eisen und Harz), so ist Geld ein Abfluss für Diamanten und Gold, kein Schlupfloch. Dorfbewohner und der Fahrende Händler bekommen zusätzliche Angebote (einige Vanilla-Waren gegen Geldscheine, Waren anderer Simple-Mods gegen Geldscheine, Geldscheine gegen Diamanten); Vanilla-Angebote bleiben im Pool, und keine Vanilla-Beutetabelle wird ersetzt. Angebote, die Vanilla-Knappheit aufheben (Knospender Amethyst, Diamanterz), sind standardmäßig aus.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** JEI, Mod Menu, SimpleBuilding, Simple Fun, Simple Models, Simple Quality of Life, Simple Riding, Simple Sandwiches, Simple Sounds, Simple Tweaks, Simple Visuals, Simple Ores

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Eigenständige Mod.** Sie gehört zu keiner Super-Mod; installiere sie allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplemoney
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: economy, game-mechanics
- Tags: money, trading, villagers, loot
- Umgebung: Client und Server

