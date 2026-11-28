# Simple Livemap - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Live-Webkarte deiner Welt, so wie Spieler sie erkundet haben, mit eingebautem Webserver, der sofort läuft, nur auf deinem eigenen Rechner.

## Release-Kanal: Beta

- Versionsnummer: `1.0.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Livemap

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Livemap zeichnet eine Webkarte aus den Chunks, die Spieler besucht haben, und den Karten, die sie in der Hand hatten, mit Relief und optionalen Spielerpositionen. Der eingebaute Webserver läuft sofort auf deinem eigenen Rechner (privat, mit Schlüssel), und eine Anleitung zeigt, wie du die Karte auf deine eigene Webseite bringst.

## Funktionen

- **Erkundete Chunks**
- **Kartenkacheln**
- **Webkarte** - Ein eingebauter Webserver zeigt die Karte im Browser (Ziehen, Zoomen, Dimensionen).
- **Privat oder öffentlich**
- **Livemap öffnen im Chat** - Im Einzelspieler und in einer LAN-Welt bekommt der Gastgeber einmal je Sitzung eine Chatzeile mit anklickbarem [Livemap öffnen].
- **Eigene Webseite** - Die Karte lässt sich auf einer eigenen Webseite zeigen: ein Reverse-Proxy (nginx oder Caddy) vor dem eingebauten Server oder eine statische Kopie der Kacheln auf beliebigem Webspace.
- **Eine Karte je Installation** - Selbst hosten liefert genau eine Live-Karte je Installation und Server: ein Webserver, eine Welt.
- **Gehostete Karte (livemap.simplemods.de)** - Ohne eigene Website: auf livemap.simplemods.de einen Einmal-Code erzeugen (gilt 15 Minuten) und als Spielleiter oder Besitzer der Einzelspielerwelt /simplelivemap connect <Code> eingeben.
- **Kartenansicht**

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** Simple Maps

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Eigenständige Mod.** Sie gehört zu keiner Super-Mod; installiere sie allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simplelivemap
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: utility, management
- Tags: map, web, server, exploration
- Umgebung: Client und Server

