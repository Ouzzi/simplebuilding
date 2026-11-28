# Simple Resonance - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Spielerwert, Gefahrenzonen und Spawn-Besitzer: eine API und Spielebene für erfahrungsabhängiges Spawnen.

## Release-Kanal: Beta

- Versionsnummer: `0.1.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Resonance

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Resonance verfolgt, wie erfahren ein Spieler ist, wie gefährlich das Land ist und welchem Spieler ein Monster-Spawn gehört, und bringt einen Anfängerschutz. Andere Mods wie Simple Mobs nutzen sie als API.

## Funktionen

- **Spielerwert** - Eine Zahl von 0 bis 1 dafür, wie erfahren ein Spieler ist: W = 0,7 x Langzeitwert + 0,3 x Anteil seit dem letzten Tod.
- **Gefahrenzonen** - Das Land wird gefährlicher, je weiter es vom Welt-Spawn entfernt ist: Stufe 0,5 bis 512 Blöcke, dann weich steigend bis 5 bei 6000 Blöcken (Smoothstep, keine harten Kanten).
- **Spawn-Besitzer und Spawn-Stufe** - Jeder Monster-Spawn-Versuch gehört genau einem Spieler in der Nähe, gewählt mit Gewicht 1/Abstand (kein Mittelwert).
- **Anfängerschutz** - Ein Spawn liegt höchstens eine Stufe über der Stufe seines Besitzers, und in den ersten 6 gespielten Stunden bleiben Monster auf Stufe 1.
- **Schnittstelle für andere Mods**
- **Übernahme aus SimpleBuilding**
- **Resonanz-Knopf** - Ein kleiner Knopf rechts neben dem Spieler im Inventar zeigt die Besatz-Resonanz als Herz in acht Stufen, von Bruchstein bis Enderit.
- **Befehle** - /simpleresonance zeigt deinen Wert, die Stufe des Landes, auf dem du stehst, und deinen Anfängerschutz.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** SimpleBuilding, Simple Mobs

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Sub-Mod** von SimpleBuilding: im Download der Super-Mod enthalten, und sie läuft auch allein mit den oben genannten erforderlichen Mods. Simple-Mods, die sie als eigenen Download brauchen: Simple Mobs.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simpleresonance
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: game-mechanics, library
- Tags: spawning, difficulty, api
- Umgebung: Client und Server

