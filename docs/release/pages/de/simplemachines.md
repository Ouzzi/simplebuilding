# Simple Machines - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Doppelkolben, Signaldämpfer, verbesserte Kolben, schnellere Filtertrichter und Trichterloren, ein Schmied und ein Auto-Crafter.

## Release-Kanal: Beta

- Versionsnummer: `0.1.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Machines

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Machines bringt den Doppelkolben, den Signaldämpfer, den Verstärkten und den Verstärkten klebrigen Kolben, Netherit- und Enderit-Kolben, die Blöcke zerbrechen, Verstärkte, Netherit- und Enderit-Trichter (2-, 4- und 8-mal schneller, mit Item-Filter) und Trichterloren (die Enderit-Teile brauchen Simple Ores), den Schmied (Schmieden per Redstone) und den Auto-Crafter (stellt selbstständig in den Trichter darunter her).

## Funktionen

- **Verstärkter, Netherit- und Enderittrichter**
- **Trichterloren** - Eine Trichterlore je Trichterstufe: Sie nimmt 2, 4 oder 8 Items je Tick auf (das Tempo ihres Trichters; Vanillas Lore 1) und filtert wie der Trichterblock, mit demselben Menü.
- **Kolben-Upgrades**
- **Doppelkolben**
- **Signaldämpfer** - Ein flaches Redstone-Teil wie der Verstärker, das 0 bis 3 vom Signal abzieht und es einen Redstone-Tick später weitergibt.
- **Schmied (Auto-Schmied)** - Der Schmied bedient den Schmiedetisch wie der Crafter: ein Schmiedevorgang pro Redstone-Impuls aus Vorlage, Basis und Material.
- **Auto-Crafter (Autonomer Crafter)** - Der Auto-Crafter ist ein Crafter, der sein Raster alle 4 Ticks selbst craftet, solange ein Trichter unter ihm steht.
- **Kreativtabs** - Die Items des Moduls stehen nur im eigenen Kreativ-Tab (keine Vanilla-Tabs).
- **Befehle** - /simplemachines config [list | get | set | reset | help] (Operatoren).

## Im Vanilla-Spiel

Gibt Redstone-Spielern stärkere Kolben, schnellere Trichter, einen Doppelkolben, einen Signaldämpfer, einen Schmied und einen Auto-Crafter, der keinen Redstone-Takt braucht (der Vanilla-Crafter stellt ein Item je Impuls her); Vanilla-Kolben, -Trichter und der Crafter bleiben, wie sie sind. Der Verstärkte Kolben bewegt einen unzerstörbaren Block nur gegen einen Redstone-Block als Bezahlung, die Brecher-Kolben nutzen sich ab und fragen zuerst Claim-Mods; die Verbesserungen erweitern die Kolbenlogik per Mixin.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** Jade, SimpleBuilding, Simple Ores, JEI

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Sub-Mod** von SimpleBuilding: im Download der Super-Mod enthalten, und sie läuft auch allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplemachines
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: technology, utility
- Tags: redstone, pistons, hoppers, machines
- Umgebung: Client und Server

