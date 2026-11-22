# Simple Music - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Notenblätter in einem Text-Notencode schreiben, auf Platten pressen und hochgeladene Ogg-Platten in Plattenspielern abspielen.

## Release-Kanal: Beta

- Versionsnummer: `0.1.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Music

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Music bringt Notenblätter mit einem Text-Notencode (Editor mit Probehören, Import von .txt und .nbs), eine Plattenpresse, hochgeladene Mono-Ogg-Platten (Kontingent und Freigabe durch Operatoren) und eine Taschen-Jukebox.

## Funktionen

- **Notenblatt** - Formlos aus Papier, Tintenbeutel und Feder.
- **Notencode** - Textzeilen legen das Tempo (Ticks je Schritt, 1-20), Aliase für Instrumente und Klänge und die Noten jeder Spur fest.
- **Editor** - Farbiges Codefeld mit Zeilennummern und unterstrichenen Fehlern (das Feld des Blaupausen-Editors, jetzt in SimpleLib).
- **Import** - .txt-Notencode oder Note-Block-Studio-.nbs-Dateien in config/simplemusic/sheets/ legen und im Editor wählen; der Import erzeugt nur Text.
- **Kopieren am Kartentisch** - Wie beim Bauplan: oben ein signiertes Notenblatt, unten ein leeres ergibt eine Kopie; nur das leere Blatt wird verbraucht.
- **Rohling** - Vier Kohle oder Holzkohle und vier Schwarzstein um einen Amethystsplitter.
- **Plattenpresse** - Ein Kolben über zwei Eisenbarren über drei glatten Steinen.
- **Dateiprüfung**
- **Eigene Platte** - Eine gepresste Platte trägt entweder einen hochgeladenen Titel (Hash, Titel, Länge, Hochladender - nie Audiodaten; Amethyst-Etikett) oder eine Komposition (Papier-Etikett).
- **Schallplatten**
- **Taschen-Jukebox** - Ein Bündel für bis zu 20 Platten (Vanilla- und Titel-Platten).
- **Abgestellte Taschen-Jukebox** - Schleichen + Rechtsklick mit der Taschen-Jukebox auf einen Block stellt sie ab, die Vorderseite zu dir.

## Im Vanilla-Spiel

Lässt Spieler Notenblätter schreiben, Platten pressen und eine Taschen-Jukebox mitnehmen; Vanilla-Platten und der Plattenspieler funktionieren weiter, mit Eingriffen zum Abspielen hochgeladener Platten.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** SimpleBuilding, Simple Mobheads

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Eigenständige Mod.** Sie gehört zu keiner Super-Mod; installiere sie allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplemusic
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: decoration, utility
- Tags: music, discs, jukebox, sheet-music
- Umgebung: Client und Server

