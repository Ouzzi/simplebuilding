# Simple Movement - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Kriechen auf Knopfdruck, Autowalk mit Serverfreigabe, schnellere Leitern und Eisläufer auf Pulverschnee.

## Release-Kanal: Beta

- Versionsnummer: `1.0.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Movement

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Movement bringt Kriechen auf Knopfdruck, einen Autowalk, den der Server erlauben kann, schnellere Leitern und Eisläufer, der auf Pulverschnee wirkt.

## Funktionen

- **Manuelles Kriechen** - Pfeil runter (Standard) oder /simplemovement crawl setzt die synchronisierte Schwimmpose.
- **Automatisch laufen** - Die Autowalk-Taste (Standard: Pfeil hoch) schaltet normale Vorwärtseingabe nur bei Serverfreigabe enableAutowalk (Standard false).
- **Klettern und Rutschen** - Klettertempo standardmäßig 0.4 Blöcke/Tick, doppelt so schnell wie Vanilla (0.2) (Bereich 0.2–0.4; 0.2 = Vanilla), Rutschtempo 0.8 (0.15–0.8).
- **Eisläufer auf Pulverschnee** - Eisläufer erlaubt das Gehen auf Pulverschnee.
- **Sprungkraft steigt Stufen** - Solange Sprungkraft aktiv ist (Trank, Leuchtfeuer oder Befehl), wird das Attribut step_height auf mindestens 1 Block aufgefüllt, du läufst also ohne Sprung einen ganzen Block hoch.
- **Befehle** - /simplemovement crawl ist für Spieler.
- **Sub-Mod von Simple QoL** - Allein spielbar (config/simplemovement.json).
- **Stufenhöhe** - Schatz-Verzauberung für Stiefel, Stufen I-II: Das Attribut step_height steigt um 0,4 / 0,9, du steigst ohne Sprung 1 Block (I) bzw. 1,5 Blöcke (II, Zäune) hoch.
- **Sprint**

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

- Wiki: https://wiki.simplemods.de/?mod=simplemovement
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: game-mechanics, utility
- Tags: movement, crawl, ladders, quality-of-life
- Umgebung: Client und Server

