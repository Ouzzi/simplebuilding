# Simple Visuals - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

HUD, Tooltips und Amboss-Formatierung auf dem Client plus Immersionspartikel im Vanilla-Stil, alle begrenzt und optional.

## Release-Kanal: Beta

- Versionsnummer: `1.0.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Visuals

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Visuals ändert, wie das Spiel auf dem Client aussieht: HUD, Tooltips, Ambosstext und Immersionspartikel aus Vanilla-Assets.

## Funktionen

- **Geschwindigkeitslinien** - Begrenzte radiale Linien bei hohem Tempo oder Beschleunigung; rein optisch, in der Egoansicht.
- **Aufnahmehinweise** - Server-Aufnahmeanimationen erzeugen bis zu acht kurzlebige Item- oder XP-Zeilen mit vier Anordnungen, Größe, Seite, Deckkraft und Seltenheitsfarben.
- **Elytra-Neigungshilfe** - Zwei Ziellinien beim Gleiten; Standard -40 und 40 Grad, Toleranz 10, Empfindlichkeit 4.
- **Effekt-Zeitleisten** - HUD- und Inventarleisten vergleichen die Restdauer mit der größten beobachteten Dauer des aktiven Effekts.
- **Spielerköpfe im Locator** - Verwendet nur serverseitig übermittelte Vanilla-Wegpunkte; ersetzt passende Symbole durch Spielerköpfe und bewahrt Vanilla-Winkel, Sichtbarkeit und Höhenpfeile.
- **Spielerköpfe im Chat** - Gesichter neben erkennbaren Spielernachrichten verwenden zwischengespeicherte Vanilla-Skins.
- **Lokale Todeskoordinaten** - Ausgeschaltete, angehängte oder separate lokale Koordinatenzeile für die eigene Todesnachricht.
- **Kartenvorschau im Tooltip** - Eine 128-Pixel-Kartenvorschau, Mittelpunkt und Dimensions-ID aus bereits auf dem Client vorhandenen Kartendaten.
- **Informationen zum gehaltenen Item** - Haltbarkeit und bis zu acht Verzauberungszeilen ergänzen den Vanilla-Itemnamen.
- **Schadensanzeigen** - Bis zu 32 kurzlebige Zahlen für beobachtete Lebensverluste naher sichtbarer Lebewesen, standardmäßig an.
- **Biomhinweise** - Optionaler Biomname beim Wechsel mit Anzeigedauer, Versatz und Wiederkehr-Sperre.
- **Ambossformatierung und Nutzungen** - Gültige &- oder Paragraphen-Farb-/Stilcodes werden nur unter der Serverregel akzeptiert.

## Gut zu wissen

- **Auch auf dem Client nötig:** Gemeißelte (gekippte) Rahmen senden einen zusätzlichen Wert, den ein Spiel ohne Simple Visuals nicht versteht. Die Meißel kommen aus Simple Chisels, das auf dem Client ohnehin nötig ist; das heißt also nur: Installiere Simple Visuals dort ebenfalls.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** SimpleBuilding, Simple Models, Mod Menu, Cloth Config

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Sub-Mod** von Simple Aesthetics: im Download der Super-Mod enthalten, und sie läuft auch allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplevisuals
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: decoration, utility
- Tags: client, hud, tooltips, particles
- Umgebung: Client und Server

