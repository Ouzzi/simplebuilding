# Simple Opt Server - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Server-Optimierung: Die Sichtweite folgt der Serverlast, nie über dem Wert aus server.properties.

## Release-Kanal: Beta

- Versionsnummer: `1.0.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Opt Server

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

> Keine Leistungsversprechen: Die Wirkung wurde noch nicht auf verschiedenen Rechnern und in Modpacks gemessen, deshalb nennt diese Seite keine Werte für FPS, Speicher oder Tickzeit. Das Ergebnis hängt von deiner Hardware und deinen anderen Mods ab.

Auf dedizierten Servern passt Simple Opt Server die Sichtweite an die Tickzeit des Servers (MSPT) an, nie über den Wert aus server.properties, und tritt vor ServerCore zurück.

## Funktionen

- **Sichtweite nach Serverlast**
- **Aktivierungsbereich (Standard aus)** - Verändert das Spiel, deshalb steht er in config/simpleoptserver.json auf mode off; mit mode auto einschalten (Neustart nötig).
- **Befehle** - /simpleoptserver config zeigt und ändert jede Option von config/simpleoptserver.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte).

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** keine

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Sub-Mod** von Simple Optimization: im Download der Super-Mod enthalten, und sie läuft auch allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simpleoptserver
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: optimization
- Tags: performance, server, view-distance
- Umgebung: Client und Server

