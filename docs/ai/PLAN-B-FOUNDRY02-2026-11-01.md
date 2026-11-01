# Plan B-FOUNDRY02 (Sitzung B, 01.11.2026) - Simple Foundry Stufen 2-4 nach den Wahlen SA2-SA10

Ist-Stand Stufe 1 (PLAN-B-FOUNDRY32): Huette (SA2 gebaut), Schmelzkammer 27 Barren (SA8), Dach Pflicht (SA9, Scan verlangt Huelle auf allen Seiten), Schichtschmelze (SA6, `Layers.at`), kein Erzbonus (SA10), Gussschale, Eisen/Gold/Kupfer. Pult-Screen und Name offen (FOUNDRY32-UI/-NAME): Bedienung bleibt Rechtsklick/Chat.

## Umgesetzt in diesem Durchgang (ohne neue Bloecke/Texturen)
- SA3 4 Stufen 3/6/9/12: `Tier` (Pult-BE, gespeichert). Upgrade per Rechtsklick: Eisenblock, Diamantblock, Netherit-Barren (Vanilla-Material, standalone). Ohne Screen werden die Plaetze zu parallelen Schmelzbahnen (1/2/3/4 Einheiten je Schmelzintervall je wartendem Metall); alle Metalle schmelzen parallel.
- SA4 Pfeil wechselt Schicht: Schleichen + leere Hand auf das Pult schaltet die gewaehlte Schicht um eine nach unten (unten -> oben), Chat nennt das Metall. Im spaeteren Screen wird daraus der Pfeil.
- SA7 Geraete einschmelzen: Eisen/Gold/Kupfer-Werkzeug und -Ruestung (Barrenkosten x 9 Einheiten, linear nach Abnutzung abgerundet, kaputt = nichts). Nachschlag per Registry-Name (Kupfer-Geraete je Version).
- SA5 = C: `CastTarget`-Schnittstelle; Gussschale implementiert sie, die Tuelle giesst in jedes CastTarget. Der verstaerkte Kessel (simplecrucibles) ist die zweite Implementierung und bleibt OFFEN (siehe unten).
- Tests: tiers, layer_select, gear_and_target (Katalog `FoundryTests.ALL` + Fabric-Adapter). Wiki-Manual EN/DE, Lang EN/DE.

## Offen (Folgearbeit)
1. Verstaerkter Kessel als Gussziel: in simplecrucibles `Content.MELT` (+Blockstate/Modell/Textur/Lang) mit BE, das `CastTarget` implementiert (Abhaengigkeit Crucibles -> Foundry nur optional per Soft-Dep, Foundry bleibt allein spielbar); erstarrt zu Barren. Braucht Textur-Vorschau (A8b).
2. Pult-Screen (FOUNDRY32-UI offen): 3/6/9/12 Plaetze als Slots, Schichtstapel mit Pfeil; dann ersetzen die Bahnen die Lane-Logik durch echte Slots.
3. Sondermaterial (Konzept Stufe 3): Amethyst/Quarz/Prismarin/Glas, Netherit (EXTREME, nur mit SimpleBuilding), Legierungen (Stufe 4) - von den Wahlen SA7 nicht verlangt.
4. Upgrade-Items sind fest (kein Config); bei Besitzerwunsch Rezept-/Config-Variante.

## A12 Vanilla-Nutzen
Schwacher Eingriff: weiterer Weg, keine Vanilla-Rezepte/Bloecke veraendert, kein Mixin, Ausbeute <= Herstellungskosten.

## Verifikation
Ein sbt-Lauf: Compile Fabric + simplefoundry/simpleovens, Modul-Gametests (+standalone), data_integrity, check_client_mixins, tooltip, loot, wiki --check.
