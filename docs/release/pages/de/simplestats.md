# Simple Stats - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Jedes Werkzeug, jede Waffe und jedes Rüstungsteil führt eine eigene Statistik, mit einem Statistik-Bildschirm je Gegenstand.

## Release-Kanal: Beta

- Versionsnummer: `1.0.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Stats

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Stats zählt je Gegenstand abgebaute Blöcke, Kills nach Mob und Variante, abgefangenen Schaden, Strecken und Fänge und zeigt sie in einem Statistik-Bildschirm.

## Funktionen

- **Gegenstandsstatistik** - Werkzeuge, Waffen, Schilde, Elytren, Angeln, Bürsten, Feuerzeuge und Rüstung (auch Reittier-Rüstung) führen ihre eigene Statistik in der Komponente simplestats:item_stats.
- **Abgebaute Blöcke** - Jeder mit einem Werkzeug abgebaute Block zählt für dieses Werkzeug und steht nach Blockart in einer Liste, auch Blöcke mit Härte 0.
- **Kills und Schaden**
- **Rüstung, Schilde und Tragezeit** - Schaden, den Rüstung und ihre Verzauberungen abfangen, wird im Verhältnis der Rüstungspunkte auf die getragenen Teile verteilt (eine Näherung); Fallschaden geht an die Stiefel.
- **Strecken und Flüge** - Stiefel zählen die gegangene, gesprintete, geschlichene und geschwommene Strecke; der Gleiter zählt Flugstrecke, Flüge, Raketenschübe und den längsten Flug.
- **Besondere Werkzeuge**
- **Tooltip und Statistik-Bildschirm**
- **Größe, Vererbung und Zurücksetzen** - Listen sind so vollständig wie das Gezählte.
- **Wer zählt und wer sieht** - Jeder, der einen Gegenstand sieht, sieht seine Statistik: nichts ist verborgen und kein Besitzer wird gespeichert, wer einen Gegenstand teilt, teilt seine Zahlen.
- **Befehle** - /simplestats reset löscht die Statistik des gehaltenen Gegenstands, /simplestats show listet die Rohwerte, /simplestats reload liest die Einstellungen neu.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** SimpleBuilding

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Eigenständige Mod.** Sie gehört zu keiner Super-Mod; installiere sie allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplestats
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: utility, game-mechanics
- Tags: statistics, tools, weapons, armor
- Umgebung: Client und Server

