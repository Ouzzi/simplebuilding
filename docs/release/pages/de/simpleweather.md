# Simple Weather - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Wetter-Schalter für den Server und Regendichte als Client-Option; eine Sub-Mod von Simple Quality of Life.

## Release-Kanal: Beta

- Versionsnummer: `1.0.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Weather

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Mit Simple Weather schaltet der Server das Wetter, und jeder Spieler stellt auf dem Client die Regendichte ein.

## Funktionen

- **Wetter abschalten** - Hält das Wetter an, damit es sich nicht mehr von selbst ändert: Regen und Gewitter enden und die Vanilla-Spielregel advance_weather wird auf false gesetzt (Server-Einstellung disableWeather, Standard aus).
- **Regen- und Gewitterhäufigkeit** - Lässt Regen und Gewitter öfter oder seltener kommen.
- **Regendichte** - Dünnt den Regen auf dem eigenen Bildschirm aus: weniger Regen in der Luft, weniger Bodenpartikel und weniger Regengeräusche.
- **Sub-Mod von Simple QoL** - Allein spielbar (config/simpleweather.json).
- **Befehle** - /simpleweather config zeigt und ändert jede Option von config/simpleweather.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte).

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** Simple Quality of Life, Mod Menu

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Sub-Mod** von Simple Quality of Life: im Download der Super-Mod enthalten, und sie läuft auch allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simpleweather
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: utility
- Tags: weather, rain, quality-of-life
- Umgebung: Client und Server

