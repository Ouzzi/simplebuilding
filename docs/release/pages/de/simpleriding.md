# Simple Riding - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Schnellere Sättel, höhere Pferdesprünge, Nautilus-Dashes, Funktionen für Reittier-Rüstung, Beute und Handel.

## Release-Kanal: Beta

- Versionsnummer: `0.9.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Riding

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Riding verbessert Reittiere: Satteltempo, Pferdesprünge, Nautilus-Dashes und Funktionen für Reittier-Rüstung, dazu passende Beute und Handel.

## Funktionen

- **Rückenwind** - Rückenwind I–III verbessert Sättel und alle 16 Geschirrfarben.
- **Sprungkraft** - Sprungkraft I–III steigert die Sprungkraft eines spielergesteuerten Pferdes standardmäßig um 20% je Stufe.
- **Pferderüstungsfunktionen** - Pferderüstung erlaubt Schutz, Feuerschutz, Explosionsschutz, Schusssicherheit, Federfall und Sprungkraft.
- **Erkundungsbeute** - Bastions-Schatzkisten und andere Bastionskisten: 0–2 Würfe; Rückenwind II/III Gewichte 20/10, Sprungkraft II/III 10/5, leer 30.
- **Bibliothekarhandel** - Bibliothekare auf Stufe 2 verkaufen Rückenwind I oder Sprungkraft I (Gewichte 30/20), Preis 10–29 Smaragde, 2 Nutzungen, 25 EP, Rufrabatt 0,5.
- **Server-Reitgrenzen** - Clientbewegungen und Sprungbefehle werden im Serverthread geprüft.
- **Hufeisen** - Pferde, Esel, Maultiere, Skelett- und Zombiepferde bekommen vier Hufplätze im Inventar.
- **Dorfbewohnerhandel aktivieren** - worldGen.enableVillagerTrades: Bibliothekare bieten Reitbücher auf Stufen 2–4 an.
- **Beutetabellen ändern** - worldGen.enableLootTableChanges: Reitbücher in Bastionen, Netherfestungen und Prüfungskammerbelohnungen ergänzen.
- **Ghast-Geschwindigkeitsfaktor** - enchantments.swiftRide.ghastSpeedMultiplier: Geschwindigkeitsbonus je Rückenwindstufe; Bereich 0–1.
- **Pferde-Geschwindigkeitsfaktor** - enchantments.swiftRide.horseSpeedMultiplier: Geschwindigkeitsbonus je Rückenwindstufe; Bereich 0–1.
- **Geschwindigkeitsfaktor anderer Reittiere** - enchantments.swiftRide.otherSpeedMultiplier: Geschwindigkeitsbonus je Rückenwindstufe; Bereich 0–1.

## Im Vanilla-Spiel

Verbessert Reittiere (schnellere Sättel, höhere Sprünge, Nautilus-Dashes, Reittier-Rüstung und Hufeisen), damit sich Reiten lohnt; die Vanilla-Reittiere bleiben und werden nur angepasst. Der Server prüft, wie schnell sich ein Reittier bewegen darf, so lassen sich schnelle Sättel nicht zum Teleportieren missbrauchen.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** SimpleBuilding, Mod Menu, Simple Ores

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Eigenständige Mod.** Sie gehört zu keiner Super-Mod; installiere sie allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simpleriding
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: game-mechanics, equipment
- Tags: mounts, horses, saddle, riding
- Umgebung: Client und Server

