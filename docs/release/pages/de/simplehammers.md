# Simple Hammers - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Sieben Vorschlaghämmer von Stein bis Enderit (mit Simple Ores) für 3x3-Abbau, Umformen von Blöcken und Aufwerten in der Welt.

## Release-Kanal: Beta

- Versionsnummer: `0.9.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Hammers

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Hammers bringt sieben Vorschlaghämmer (Enderit mit Simple Ores), die eine 3x3-Fläche abbauen, Blöcke zu Treppen und Stufen umformen, Achtel schnitzen (mit Simple Octets), Diamant- und Quarzblöcke zerschlagen und Blöcke in der Welt aufwerten. Verzauberungen: Durchbruch, Übersteuerung und Radius.

## Funktionen

- **Vorschlaghämmer: eine ganze Fläche abbauen** - Sieben Vorschlaghämmer von Stein bis Enderit.
- **Abtragen und Umformen** - Schleichen und Benutzen halten: nach einer kurzen Ladezeit schlägt der Hammer das angezielte Achtel vom Block.
- **Diamant- und Quarzblöcke zerschlagen** - Neun Rechtsklicks mit einem Hammer ab der Eisenstufe auf einen Diamantblock oder rissigen Diamantblock: er zerspringt in 81 Diamantkiesel (Config diamondBlockPebbles).
- **Stein zerkleinern: Stein, Bruchstein, Kies, Sand** - Fünf Rechtsklicks (ohne Schleichen) auf Stein, Bruchstein oder Kies mit jedem Vorschlaghammer: Stein wird Bruchstein, Bruchstein wird Kies, Kies wird Sand.
- **Schlagtempo je Hammerstufe** - Jede Umwandlung mit dem Hammer schlägt im Takt der Stufe: Netherit alle 20 Ticks, Enderit 19, Diamant 21, Gold 22, Eisen 23, Kupfer 24, Stein 25.
- **Aufwerten in der Welt mit dem Hammer**
- **Hammer-Verzauberungen**
- **Werkzeug im Werkbankraster** - Ein formloses Rezept mit einem Vorschlaghammer gibt den Hammer mit 1 Haltbarkeit weniger zurück, statt ihn zu verbrauchen; bricht er dabei, ist er weg.
- **Vorschlaghämmer am Amboss** - Zwei Vorschlaghämmer verbinden sich für 1 Stufe: beide Haltbarkeiten plus 12 % des Höchstwerts, Verzauberungen wie in Vanilla vereint (unverträgliche des rechten Hammers fallen weg).
- **Ältere Welten** - Vorschlaghämmer aus Welten früherer SimpleBuilding-Versionen funktionieren weiter als Hämmer von Simple Hammers, mit Verzauberungen und angefangenen Aufwertungen.
- **Kreativ-Tabs** - Die Items des Moduls stehen nur im eigenen Kreativ-Tab (keine Vanilla-Tabs).
- **Befehle** - /simplehammers config zeigt und ändert jede Option von config/simplehammers-server.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte).

## Im Vanilla-Spiel

Bringt Vorschlaghämmer, die 3x3 abbauen und Blöcke umformen und aufwerten; es sind Werkzeuge und Verzauberungen zusätzlich zu Vanilla, mit Eingriffen in Abbautempo, Amboss und einige Rezepte.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** SimpleBuilding, Simple Octets, Simple Crucibles, Simple Ores, JEI

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Sub-Mod** von SimpleBuilding: im Download der Super-Mod enthalten, und sie läuft auch allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplehammers
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: equipment, utility
- Tags: hammer, mining, tools
- Umgebung: Client und Server

