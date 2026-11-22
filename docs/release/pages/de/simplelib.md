# SimpleLib - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Gemeinsame Basis der Simple-Familie: Modpack-Handbuch, warmes Essen, gemeinsame Materialien, Seltenheitsstufen und UI-Bausteine.

## Release-Kanal: Beta

- Versionsnummer: `0.4.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# SimpleLib

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

SimpleLib enthält, was alle Simple-Mods teilen: ein Modpack-Handbuch, warmes Essen, gemeinsame Materialien wie die Kerne und die Starke Aufwertungsvorlage, Helfer für Aufwertungen in der Welt, Seltenheitsstufen und UI-Komponenten. Sie wird automatisch als Abhängigkeit installiert.

## Funktionen

- **Simple-Modpack-Handbuch** - Ein Handbuch für alle Simple-Mods, vorhanden, sobald eine davon installiert ist: formlos aus Buch und Werkbank herstellen oder von der ersten FTB-Quests-Quest holen.
- **Warmes Essen** - Gegartes Essen kommt warm aus dem Schmelztiegel und wird warm 15 % schneller gegessen.
- **Gemeinsame Materialien**
- **Inhalte nur auf Anforderung (Vanilla-Clients)** - SimpleLib registriert seine Blöcke, Items, Item-Komponenten, den Wirren Trank und den legendären Klang nur, wenn ein installierter Mod sie anfordert.
- **Verfluchte und mythische Seltenheit**
- **Legendäre Momente** - Die schwersten Fortschritte und die schwersten Verzauberungen spielen einen tiefen Chor und eine Partikelschau am Spieler.
- **Hänger-Wächter** - Steht der Server-Tick still, schreibt Simple Lib eine Liste aller Threads in den Logs-Ordner, damit sich ein eingefrorenes Spiel untersuchen lässt.
- **Free Look** - Solange Free Look an ist, dreht die Maus nur die Kamera, nicht den Spieler; Laufen und alle anderen Eingaben bleiben normal.
- **Befehle** - /simplelib config zeigt und ändert jede Option von config/simplelib-server.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte).

## Im Vanilla-Spiel

Gemeinsame Basis der Simple-Mods mit einem Modpack-Handbuch und Wirrwarze (in Bastionstruhen zu finden); sie ordnet die Kreativ-Tabs neu und greift in Trichter, Amboss und Wither ein, aber das Zerkleinern im Amboss, der Wither-Drop, der legendäre Chor und die Free-Look-Taste schalten sich nur ein, wenn eine Simple-Mod sie anfordert.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **Optional:** JEI

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Bibliothek.** Jede Simple-Mod enthält SimpleLib in ihrem eigenen Download, du musst sie also nie von Hand installieren.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplelib
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: library
- Tags: library, api, guide, shared
- Umgebung: Client und Server

