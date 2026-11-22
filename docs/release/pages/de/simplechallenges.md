# Simple Challenges - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Spielmodi für eine Welt deiner Wahl: Freischalt-Zellen, Skyblock, Sky Grid, Glücksblock, Skyline und mehr, insgesamt 16 Presets.

## Release-Kanal: Beta

- Versionsnummer: `0.1.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Challenges

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Challenges bringt den Freischalt-Modus: Die Welt wird in Zellen zerlegt (von einer einzelnen Blocksäule bis zu drei Chunks), und nur freie Zellen dürfen betreten werden. Jede Zelle hat einen Preis aus der Welt, mit Deadlock-Schutz und Notfall-Freischaltung. Die Modi Skyblock, Sky One Block, Sky Grid, Glücksblock und Skyline gibt es ebenfalls als Presets (insgesamt 16). Ohne gewählte Challenge ändert die Mod nichts.

## Funktionen

- **Sicherheitssperre** - Ohne gewählte Challenge ändert Simple Challenges nichts: Es wird keine Zustandsdatei geschrieben, die Kollisions- und Block-Hooks kehren sofort zurück und es läuft keine Tick-Arbeit.
- **Freischalt-Raster** - Die Oberwelt wird in quadratische Säulen von 1, 2, 4, 8, 16 (ein Chunk), 32 oder 48 Blöcken (drei Chunks) über die ganze Höhe zerlegt.
- **Grenzlinie** - Clients mit der Mod bekommen die freien Zellen vom Server (alles beim Beitritt, eine Zelle je Freischaltung) und sehen den Rand des freien Bereichs als Linie roter Partikel in ihrer Nähe.
- **Preise** - Jede Zelle neben dem freien Bereich hat einen Preis, einmal aus Welt-Seed, Zelle und dem, was die Welt dort bietet, berechnet und dann im Weltzustand gehalten.
- **Credits durch Erfolge**
- **Deadlock-Schutz** - Beim Start einer Challenge und nach jeder Freischaltung prüft die Mod, dass mindestens eine Zelle neben dem freien Bereich mit etwas bezahlt werden kann, das der freie Bereich bietet.
- **Presets** - Eine Challenge ist eine Preset-Datei: data/<Namensraum>/simplechallenges/preset/<id>.json in einer Mod oder einem Datenpaket, oder eigene in config/simplechallenges/presets/<id>.json.
- **Befehle** - /simplechallenges (Status) zeigt Challenge, freie Zellen, Credits und den Preis der Zelle, die du ansiehst.
- **Welt-Erstellung** - Der Weltentyp-Knopf von Neue Welt erstellen bietet Simple Challenges neben Superflat an.
- **Skyblock-Familie** - Drei Weltmodi starten in der Leere und lassen sich mit dem Freischalt-Modus kombinieren.
- **Spawn, Ränder und die Fadenkreuz-Zeile**
- **Glücksblock und Zufallsdrops** - In einer Lucky-Challenge lässt ein abgebauter Block einen anderen Block fallen statt sich selbst.

## Im Vanilla-Spiel

Bietet einen optionalen Weltmodus mit freien und gesperrten Bereichen für Spieler, die einen härteren Start wollen; ohne gewählte Challenge ändert sie nichts.

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

- Wiki: https://wiki.simplemods.de/?mod=simplechallenges
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: game-mechanics, adventure
- Tags: challenge, game-mode, survival
- Umgebung: Client und Server

