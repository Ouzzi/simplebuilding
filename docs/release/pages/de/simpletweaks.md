# Simple Tweaks - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Server-Claims, Admin-Werkzeuge (Block-Protokoll, Backups, Anti-X-Ray) und Spieler-Tweaks, standardmäßig an; ältere Welten laden weiter.

## Release-Kanal: Beta

- Versionsnummer: `0.9.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Tweaks

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Tweaks hält ältere Welten kompatibel und gibt Servern drei Gruppen von Werkzeugen, jede abschaltbar und fast alle standardmäßig an: Claims, die Bauten schützen, Admin-Werkzeuge (Block- und Truhen-Protokoll, Inventar-Sicherungen, Welt-Backups, Vanish, Einfrieren, Neustart-Ankündigungen, Lag-Finder, Anti-X-Ray) und Spieler-Tweaks (Todes-Hinweis, Teil-Keep-Inventory, Neulingsschutz, AFK-Behandlung, Abstimmungen, Server-Events und Willkommensnachrichten, die standardmäßig aus sind).

## Funktionen

- **Kompatibilität alter Welten** - Eine exakte Liste verbindet alte Item-, Block-, Blockentitäts- und Komponentennamen mit vorhandenen SimpleBuilding-Registries.
- **Claims (standardmäßig an)**
- **Claim-Urkunde** - Stelle aus Papier, Tintenbeutel und Honigwabe eine leere Claim-Urkunde her.
- **Alter Claim-Tisch** - Den Claim-Tisch gibt es nicht mehr: Claims sieht man auf der Wayfinder-Karte (Simple Maps) und in der Urkunde.
- **Claims auf der Karte**
- **Block- und Truhen-Protokoll (standardmäßig an)** - Protokolliert abgebaute und gesetzte Blöcke, aus Containern genommene oder hineingelegte Items, Schild-Änderungen, Explosionen und Feuer (Trichter optional) – mit Verursacher, Zeit, Position und Dimension.
- **Inventar-Sicherung beim Tod (standardmäßig an)** - Bevor ein Spieler beim Tod etwas fallen lässt, werden Inventar, Rüstung, Nebenhand, XP, Zeit, Ursache und Position gesichert; je Spieler bleiben die letzten keep Tode (Standard 5, 1–50).
- **/simpletweaks invsee und /simpletweaks endersee (standardmäßig an)** - Eigene Admin-Ansicht im SimpleLib-Kasten: Rüstung, Nebenhand, Inventar und Hotbar des Ziels (oder seine Endertruhe) über deinem eigenen Inventar.
- **Vanish (standardmäßig an)**
- **Einfrieren (standardmäßig an)** - /simpletweaks freeze <Spieler> hält einen Spieler fest: jede Bewegung wird zurückgesetzt, Abbauen, Benutzen, Setzen, Angreifen und die Wegwerftaste werden verweigert.
- **Neustart-Ankündigung (standardmäßig an)**
- **Automatische Welt-Backups (standardmäßig an)**

## Im Vanilla-Spiel

Hält ältere Welten ladbar und bringt Server-Claims, die standardmäßig an sind; die Claim-Regeln ändern, wer auf einem Server wo bauen darf. Ebenfalls standardmäßig an: Teil-Keep-Inventory (Hotbar und Rüstung bleiben beim Tod), ein Todes-Hinweis, AFK-Spieler zählen beim Schlafen nicht mit, Neulingsschutz und ein Drachenei nach jedem Drachenkampf; alles einzeln abschaltbar.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** Simple Dimensions, SimpleBuilding, Simple Money, Simple Weather, Simple Maps

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Eigenständige Mod.** Sie gehört zu keiner Super-Mod; installiere sie allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simpletweaks
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: utility, management
- Tags: claims, compatibility, server, admin
- Umgebung: Client und Server

