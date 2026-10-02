# SimpleBuilding

Mod-ID `simplebuilding`, Version 1.3.1. Bau-, Abbau- und Lager-Werkzeuge fuer grosses Bauen im
Ueberlebensmodus, dazu Enderit als Endgame-Stufe und die Funktionen der Mod Simple Tweaks.

Diese Readme ist eine Uebersicht. Die vollstaendige Referenz - jedes Item, jeder Block, jedes Rezept,
jede Verzauberung und Config-Option - ist das Wiki im Ordner [`wiki/`](wiki/) (`wiki/index.html` im
Browser oeffnen; die Daten erzeugt `python wiki/generate.py` direkt aus der Mod).

## Minecraft-Linien und Loader

| Minecraft | Fabric | NeoForge | Java |
|---|---|---|---|
| 26.2 (Hauptlinie) | Loader 0.19.3, Fabric API 0.158.0+26.2 | 26.2.0.69 | 25 |
| 26.3 | Loader 0.19.5, Fabric API 0.161.0+26.3 | 26.3.0.16-beta | 25 |
| 1.21.11 | Fabric API 0.141.6+1.21.11 | 21.11.45 | 21 |
| 26.4-snapshot-1 (experimentell) | Loader 0.19.5, Fabric API 0.161.1+26.4 | - | 25 |

- Forge 26.2 wird mitgebaut; Forge 26.3 ist additiv mit `-Pforge263=true` verfügbar
  (Server-Testziel `forge-263`, Details und offene Clientprüfung: `docs/FORGE-26.3.md`).
- Die 26.4-Snapshot-Linie wird nur mit `-Pmc264=true` gebaut und gehoert nicht zum Release-Tor.
- Abhaengigkeiten: Cloth Config; Mod Menu (Fabric) fuer den Config-Bildschirm; JEI optional.

## Funktionen

- **Werkzeuge:** Baustaebe (Kupfer bis Enderit, Flaeche 3x3 bis 13x13, mit Modi per Verzauberung),
  Vorschlaghammer (3x3-Abbau, Radius/Durchbruch), Meissel (Bloecke in der Welt umwandeln), Oktant
  (Vermessen, Formen, Oktant-Manager), Detektor, Attraktor, Rotator, Tacho.
- **Blaupause:** ein Bauwerk als lesbarer Bau-Code - im Editor schreiben, am Kartentisch aus einer
  Oktant-Auswahl scannen und mit dem Baustab in der anderen Hand bauen.
- **Rucksaecke:** vier Stufen (9 bis 50 Plaetze) im Brust-Slot, eigene Taste (Standard B), als Block
  abstellbar, faerbbar, mit Verzauberungen wie Tiefe Taschen und Trichter.
- **Lager:** verstaerkte, Netherit- und Enderit-Buendel und -Koecher.
- **Maschinen:** Oefen, Hochoefen, Raeucheroefen und Trichter in den Stufen Verstaerkt, Netherit und
  Enderit (der Enderit-Ofen ist achtmal so schnell wie Vanilla, der Enderit-Trichter bewegt jeden Tick
  ein Item).
- **Kolben:** der verstaerkte Kolben schiebt 18 statt 12 Bloecke, der Netherit-Kolben zerstoert den
  Block vor sich, der Enderit-Kolben bohrt sich - bezahlt mit einem Redstone-Block - bis zu drei Bloecke
  tief durch Unzerstoerbares wie Grundgestein.
- **Enderit:** Erze und Bloecke im End (Astralit, Nihilit), Enderit-Werkzeuge, -Ruestung und
  -Aufwertungsvorlage; Enderit-Items gehen in der Leere nicht verloren.
- **19 Verzauberungen**, u. a. Aderabbau, Streifenabbau, Luftsprung, Kinetischer Schutz, Schublade,
  Farbpalette, Beruehrung des Konstrukteurs; Ruestungsbesaetze geben Boni.
- **Welt:** Handel bei Dorfbewohnern und fahrendem Haendler, zusaetzliche Beute (beides abschaltbar).

### Simple Tweaks (uebernommen)

Fast alles aus Simple Tweaks steckt jetzt in SimpleBuilding (Einzelheiten:
`docs/SIMPLETWEAKS-UEBERNAHME.md`): Diamant-, Netherit-, Enderit- und Kupfer-Druckplatten,
Elytra-Pads (fuenf Stufen) und Flypads (drei Stufen), Spawn-Teleporter, Launchpads, Chunk-Loader, die Spawn-Elytra
im Spawnbereich, Echolot, Resonanzstab, XP-Verklumpung, Raketen-Stapelgrenze und das Sperren
von Nether/End. Jede Pad-Familie ist in der Config abschaltbar.

## Hauptlinie und Zusatzmodule

26.3 Fabric + NeoForge ist die Hauptlinie. Zwei Mega-Handbücher schalten Kapitel im Buch
per Reiter und einem verbrauchten Schlüsselitem frei. Zwei getrennte End-Signalkanäle
nutzen eigenes Pulver, Schalter und Lampen; das Astralgewölbe hat 54 persönliche Plätze
(die ersten 27 gemeinsam mit der Vanilla-Endertruhe).

Zusatzmods liegen unter `modules/<id>/`, registriert in `modules/modules.json`.
Launch Hub: `python tools/launchhub/server.py` (8771); Balancing-Zentrale: Port 8770.
Siehe `docs/MULTIMOD.md` und `docs/LAUNCHHUB.md`.

## Befehle

Alle nur fuer Operatoren.

| Befehl | Wirkung |
|---|---|
| `/simplebuilding config setTrimMultiplier <wert>` / `getTrimMultiplier` | Multiplikator der Ruestungsbesatz-Boni |
| `/simplebuilding tweaks ...` | Tweaks-Config im Spiel aendern (Pads, Spawn, Spawn-Elytra, Weltspawn, Dimensionen, Befehle) |
| `/killboats [standard\|empty\|all]` | unbesetzte Boote im Umkreis von 100 Bloecken entfernen |
| `/killcarts [standard\|empty\|all]` | dasselbe fuer Loren (standardmaessig abgeschaltet) |
| `/sbtestcentre build\|section\|kit\|tp\|coverage` | Testzentrale fuer manuelle Abnahme aufbauen (`docs/TESTZENTRALE.md`) |

## Config

`config/simplebuilding.json` (im Spiel ueber Mod Menu bzw. die NeoForge-Modliste, Taste G oeffnet
die Werkzeug-Einstellungen). Abschnitte: `tools` (Hervorhebung, Animationen, Hungerkosten des
Baustabs), `worldGen` (Handel, Beute), `tweaks` (alles aus Simple Tweaks) und einzelne Schalter wie
Luftsprung, Ruestungsbesatz-Boni und Endportalrahmen-Durchbruch durch Kolben. Die clientrelevanten
Tweaks-Werte (Raketen-Stapel, Boosts, Laser) schickt der Server beim Einloggen an den Client.

## Bauen und Testen

- `./gradlew.bat check` baut und prueft alle Linien; `-Pmc264=true` nimmt die Snapshot-Linie dazu.
- Spieltests: `python tools/testrunner/run.py --targets <ziele> --filter "simplebuilding:<muster>"`
  (Ziele mit `--list`).
- Lizenz: siehe `LICENSE`.
