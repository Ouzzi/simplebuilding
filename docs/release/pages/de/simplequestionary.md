# Simple Questionary - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Der Fragentisch: Leg einen beliebigen Gegenstand hinein und lies in einem offenen Buch seine Überlieferung, seinen Nutzen und ein Geheimnis.

## Release-Kanal: Beta

- Versionsnummer: `0.1.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Questionary

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Questionary bringt einen Tisch, der jeden hineingelegten Gegenstand in einem offenen Buch erklärt.

## Funktionen

- **Fragentisch** - Leg einen Gegenstand ins Feld in der Mitte: das aufgeschlagene Buch dahinter erzählt seine Überlieferung, seinen Nutzen und ein Geheimnis.
- **Fragentisch-Texte** - Jede Mod beschreibt ihre eigenen Gegenstände in assets/<Namensraum>/questionary/<Sprache>.json; Ressourcenpakete können Einträge ergänzen oder ersetzen.
- **Ersatztexte** - Items ohne eigenen Text bekommen trotzdem eine Seite: einen kurzen Stimmungstext, aus acht Varianten je Item-Klasse gewählt, sodass dasselbe Item in jeder Welt gleich liest.
- **Rezept-Fakten** - Das Buch liest die Rezepte der Welt für das Item im Platz und listet auf, woraus es besteht und was man daraus macht, samt Werkbank.
- **Befehle** - /simplequestionary config zeigt und ändert jede Option von config/simplequestionary.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte).

## Im Vanilla-Spiel

Erklärt Neulingen jedes Item, das sie in einen Tisch legen; sie bringt kein eigenes Gameplay und ändert kein Vanilla-Verhalten.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** keine

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Eigenständige Mod.** Sie gehört zu keiner Super-Mod; installiere sie allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplequestionary
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: utility, decoration
- Tags: guide, lore, items, book
- Umgebung: Client und Server

