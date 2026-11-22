# Simple Opt Render - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Client-Optimierungen: Blätter-Culling, Lichtkarten-Cache, schneller IP-Ping und Telemetrie aus, mit Rücksicht auf andere Mods.

## Release-Kanal: Beta

- Versionsnummer: `1.0.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Opt Render

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

> Keine Leistungsversprechen: Die Wirkung wurde noch nicht auf verschiedenen Rechnern und in Modpacks gemessen, deshalb nennt diese Seite keine Werte für FPS, Speicher oder Tickzeit. Das Ergebnis hängt von deiner Hardware und deinen anderen Mods ab.

Simple Opt Render bringt Client-Optimierungen. Jeder Bereich tritt automatisch zurück, wenn eine bekannte Optimierungsmod ihn schon abdeckt.

## Funktionen

- **Weicht bekannten Mods** - Jeder Bereich hat in config/simpleoptrender.json einen Modus: auto (Standard), ours, theirs oder off.
- **Blätter-Culling** - Blätter zeichnen die Flächen zu anderen Blättern nicht mehr, Wälder bauen so kleinere Chunk-Meshes, außen bleibt die feine Cutout-Optik.
- **Lichtkarten-Cache**
- **Schneller IP-Ping** - Eine als reine IP eingetragene Serveradresse (IPv4, oder IPv6 in Klammern) wird direkt benutzt: keine Rückwärts-DNS-Abfrage und keine SRV-Abfrage, die Serverliste füllt sich schneller.
- **Telemetrie aus** - Der Client startet keine Telemetrie-Sitzung: jedes Telemetrie-Ereignis geht an den abgeschalteten Vanilla-Sender, nichts wird gesendet.
- **Weiteres Culling**
- **Detail-Schalter**
- **Befehle** - /simpleclient simpleoptrender zeigt und ändert jede Option von config/simpleoptrender.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte).

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

- Wiki: https://wiki.simplemods.de/?mod=simpleoptrender
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: optimization
- Tags: performance, client, rendering
- Umgebung: Client und Server

