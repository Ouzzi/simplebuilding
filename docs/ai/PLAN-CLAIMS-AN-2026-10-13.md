# Plan: Claims-Hauptschalter standardmäßig AN (13.10., Branch claude-claims-on)

Besitzer gibt den Claims-Hauptschalter frei (Entscheidung 30.09. „zuerst aus“ ist damit erfüllt).

## Ist-Zustand
- Schalter: `ClaimConfig.DEFAULT.enabled` (simpletweaks), Datei `config/simpletweaks-claims.json`
  (serverweit, Neustart). Fehlt die Datei, wird sie mit den Defaults **geschrieben**.
  Folge: Jede Instanz, die das Modul schon einmal gestartet hat, hat `"enabled": false`
  explizit in der Datei und bleibt aus – nur neue Installationen/fehlende Datei/fehlender
  Schlüssel bekommen den neuen Default. Bewusst so gelassen (Besitzervorgabe).
- Zweiter Schalter: Super-Mod „Enable Simple Tweaks“ (`SubMods`) – aus = Claims schützen nichts.
- Schutz: Vanilla-Mixins (Abbau/Benutzen/Platzieren, Eimer, Entities, Explosion, Feuer, Fluss,
  Kolben, Trichter, Werfer, Crafter, Kupfergolem, Blitz, Mobs, Boote) + Framework-`Protection`
  für SimpleBuilding-Werkzeuge (Hammer/Vein/Strip über `gameMode.destroyBlock`, Baustab,
  Laser, Echo, Pads, abgelegter Attractor, Kolben, Trichter). 61 GameTests.
- Claim anlegen: nur über die Urkunde, die kein Rezept hat → im Survival kein Weg.

## Lücken
1. Kein Survival-Weg zum Anlegen → `/claim create` (Chunk unter dem Spieler, gleiche Regeln
   wie die Urkunde: Limits, Cooldown, Weltgrenze, Spawnpuffer, Dimensionsliste).
2. Gehaltener Attractor (Magnet) zieht Items aus fremden Claims in die Wildnis, dort sind sie
   aufhebbar → `AttractorFilter.mayAttract` prüft `WorldPermissions.mayAffectEntity`.
3. Fremder `/claim`-Befehl ließ den Server mit Default an abstürzen → Warnung, Befehle
   auslassen, Schutz bleibt aktiv.
4. Texte „standardmäßig aus“ (Tooltip Urkunde, Config-Tooltip/Default, Wiki, Guides, Doku).

## Tests
Tests, die DEFAULT als „aus“ nutzten, nutzen `ClaimConfig.OFF`. Neu: `/claim create`,
Attractor (gehalten) an der Grenze. Kompatibilitätstest: `/claim` nur nutzbar, wenn aktiv.
Läufe: simpletweaks Fabric 26.3, dann NeoForge 26.3, plus SB-Filter *attractor*/*magnet*.

## Migration der alten Datei (Besitzer-Entscheidung, Branch claude-claims-migrate)
Der alte Build schrieb beim ersten Start `"enabled": false` automatisch. Das war nie eine Wahl.
`ClaimConfig.load`: Datei ohne `configVersion` (oder < 2) wird einmalig migriert: `enabled:false`
wird `true`, `configVersion: 2` wird (atomar, übrige Werte bleiben) geschrieben, eine Info-Zeile
geloggt. Mit `configVersion: 2` bleibt `false` aus (bewusster Opt-out). Fehlender Schlüssel = an.
Neue Dateien erhalten `configVersion` sofort. Tests: ClaimTests (Config-Test).
