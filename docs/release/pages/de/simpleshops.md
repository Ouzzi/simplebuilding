# Simple Shops - Entwurf der Store-Seite (Deutsch)

Status: **Entwurf**

## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)

Spieler-Shops mit Verkaufsblock und Vitrine, geschützt vor Fremden, Explosionen, Kolben und Trichtern.

## Release-Kanal: Beta

- Versionsnummer: `0.1.0-beta.1`
- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)

## Beschreibung (Markdown)

# Simple Shops

> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde Probleme im Issue-Tracker.

Simple Shops bringt einen Verkaufsblock mit einem Preis (bis zu fünf genaue Preis-Items) und eine Vitrine mit bis zu fünf Waren, ein Stück je Handel. Zum Kaufen braucht man keine Claim-Rechte.

## Funktionen

- **Vitrine** - Ein Glaskasten mit fünf Warenplätzen, der zeigt, was verkauft wird; auch der Deckel ist aus Glas, so sieht man die Ware von oben.
- **Verkaufsblock** - Die Kasse eines Shops: ein Holzblock unter der Vitrine.
- **Ladenmusik** - Leg eine Schallplatte in deinen Verkaufsblock, und der Shop spielt sie als Schleife, solange Spieler in der Nähe sind.
- **Verkaufslager** - Truhengroßes Zusatzlager für die Einnahmen.
- **Kaufen** - Vitrine oder Verkaufsblock anklicken: einen Warenstapel wählen, die Preis-Items in die Bezahlplätze legen (gleiche Positionen wie die Preiszeile) und Tauschen drücken.
- **Server-Shop** - Ein unbegrenzter Shop, den nur Operatoren (Berechtigungsstufe 2) mit /simpleshops servershop erstellen und einrichten.
- **Ender-Shops** - Wie bei der Endertruhe: Ware, Preise und Einnahmen jedes Shops liegen in den Weltdaten, die Blöcke sind nur Fenster darauf.
- **Handelskontor** - Fernzugriff auf Shops: Goldbarren, ein Enderauge, Bücherregale, ein Lesepult und Bretter.
- **Schutz** - Ein gesetzter Shop gehört seinem Besitzer (dem Spieler, der ihn gesetzt hat, auf dem Server per UUID gespeichert).
- **Claims und Simple Money** - Allein läuft ein Shop überall.
- **Kreativ-Tabs** - Die Items des Moduls stehen nur im eigenen Kreativ-Tab (keine Vanilla-Tabs).
- **Befehle** - /simpleshops config zeigt und ändert jede Option von config/simpleshops-server.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte).

## Im Vanilla-Spiel

Lässt Spieler auf einem Server Waren aus einem Block und einer Vitrine verkaufen; der Vanilla-Dorfbewohnerhandel bleibt unberührt.

## Abhängigkeiten

- **Erforderlich:** Fabric API (keine weiteren Mods)
- **In diesem Download enthalten:** SimpleLib
- **Optional:** SimpleBuilding, Simple Money, Simple Tweaks

## Kompatibilität

- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25
- Vorerst nur für Fabric.

## Teil der Simple-Familie

**Eigenständige Mod.** Sie gehört zu keiner Super-Mod; installiere sie allein mit den oben genannten erforderlichen Mods.

## Lizenz

All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.

## Links

- Wiki: https://wiki.simplemods.de/?mod=simpleshops
- Issues: https://github.com/Ouzzi/simplebuilding/issues
- Quellcode: https://github.com/Ouzzi/simplebuilding

## Kategorien und Tags (Vorschlag)

- Modrinth-Kategorien: economy, game-mechanics
- Tags: shops, trading, multiplayer
- Umgebung: Client und Server

