# Simple End Dragon Fight - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Härterer Enderdrachen-Kampf: eine Lebensleiste je Spieler, ein Drache, der in Gruppen schneller wird, Eintritts-Animationen und Dragonmites.

## Release-Kanal: Beta

- Versionsnummer: `0.1.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple End Dragon Fight

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple End Dragon Fight bringt eine Lebensleiste je Spieler auf der Insel, einen Drachen, der mit jedem weiteren Spieler schneller fliegt, Eintritts-Animationen und Dragonmites ab halbem Leben.

## Funktionen

- **Eine Lebensleiste je Spieler** - Jeder Spieler auf der End-Hauptinsel gibt dem Drachen eine volle Lebensleiste mehr: 200 Leben je Spieler wie in Vanilla (200, 400, 600 ...), mit "raiseBaseHealth" an 300 je Spieler.
- **Schnellerer Drache** - Allein fliegt der Drache mit Vanilla-Tempo; jeder weitere Spieler gibt 1,5 % dazu, höchstens 35 % (die Einstellung speedMultiplier hebt die Basis an, z. B. 1,2 = 20 % schneller).
- **Schwerer mit jeder Beschwörung**
- **Eintritts-Animation** - Jeder Spieler, der einen laufenden Kampf betritt, bekommt eine 3-Sekunden-Begrüßung: Portalring, der Drache steigt auf und wittert den Neuen, Brüllen mit Schockring, dann regnen Funken herab.
- **Dragonmites** - Ab halbem Leben ruft der Drache Dragonmites nahe dem Ausgangsportal: eine erste Welle sofort, dann alle 30 Sekunden eine.
- **Einstellungen** - Server-Datei config/simpleenddragonfight.json; mit SimpleBuilding schaltet zusätzlich "Simple End Dragon Fight aktivieren" das Modul.
- **Befehle** - /simpleenddragonfight config zeigt und ändert jede Option von config/simpleenddragonfight.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte).

## Im Vanilla-Spiel

Macht den Vanilla-Drachenkampf für Gruppen härter und fairer (eine Lebensleiste je Spieler, ein schnellerer Drache, zusätzliche Diener); sie ändert den Kampf, nicht das End selbst.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** Simple Mobs

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Eigenständige Mod.** Sie gehört zu keiner Super-Mod; installiere sie allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simpleenddragonfight
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: adventure, mobs
- Tags: ender-dragon, boss, end
- Umgebung: Client und Server

