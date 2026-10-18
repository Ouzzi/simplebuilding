# Konzept: Bundles Beyond (Ideen übernehmen)

Stand 19.10.2026, B (b-concepts19). Nur Konzept, kein Code. Freigabe durch den Besitzer offen.

## Ziel
Bündel (und SBs verstärkte Bündel) komfortabler machen: alle Inhalte sichtbar/entnehmbar, Auswahl per Tasten, optional Bündel-Varianten. Abgleich mit SB-Rucksäcken und Simple Sandwiches (Essen aus Bündeln).

## Vorbild: Bundles Beyond (Modrinth, CC-BY-NC-SA 4.0 -> kein Code/keine Assets, nur Ideen nachbauen)
Hebt die Vanilla-Grenze von 12 sichtbaren Items auf: alle Items sichtbar und entnehmbar; Scrollen mit Pfeil-/Bewegungstasten, Leertaste = Item rein/raus, Tastenwechsel horizontal/vertikal; rein Client, Fabric/NeoForge/Quilt, Konfig per Mod-Menü. (Hinweis: "Größere/farbige Bündel" im Auftrag gehört eher zu anderen Mods wie Bundle Upgrade, Best Bundles; Bundles Beyond selbst ist nur Ansicht.)

## Ist-Zustand im Repo
- SB: verstärktes Bündel (`ReinforcedBundleRecipe`, `ReinforcedBundleSelectionPayload`, Händlerhandel Wandering Trader `reinforced_bundle_emerald`) und Rucksäcke (Menü/Curios in neoforge `BackpackMenus`). Auswahl-Payload bedeutet: Server kennt bereits die ausgewählte Bündelposition.
- Simple Sandwiches: `bundleEating` (Config, Standard an) – isst aus Bündeln; GameTest `bundleEating`. Jede Änderung der Bündel-Auswahl darf das nicht brechen (Schnittstelle: Vanilla-`bundle_contents`-Komponente und "ausgewählter Index", keine Importe zwischen Mods).
- Vanilla 26.x: Bündel-Raster-Tooltip vorhanden, Scroll-Auswahl vorhanden; Vanilla-Limit der Sichtbarkeit (12) – prüfen, ob in 26.3 schon geändert (Rückport-Regel).

## Feature-Tabelle
| Feature | Übernehmen | Grund |
|---|---|---|
| Alle Inhalte sichtbar (Scroll/Seiten statt 12-Grenze) | ja | Kern, Client |
| Tasten Pfeile/Leertaste im Hover | später | Vanilla-nah halten: Mausrad reicht; Tasten optional |
| Scrollrichtung horizontal/vertikal | ja | kleiner Config-Schalter |
| Größere Bündel (mehr Platz) | bereits: verstärktes Bündel | kein Zweitprodukt; ggf. 2. Stufe prüfen |
| Farbige Bündel | ja, Stufe 3 | Vanilla hat 16 Farben (Bündel gefärbt); nur Sandwiches/Rucksack-Abgleich: Farbe darf Essen-Logik nicht ändern |
| Bündel-Komfort: Shift-Klick auf Behälter füllt Bündel | später | Konflikt mit Rucksack-Regeln |
| Essen aus Bündeln | vorhanden (Sandwiches) | nur nicht regressieren |

## Zielmod
Empfehlung: **Ansicht (Scroll/alle Items)** in **Simple Interfaces** (UI, Client), gleiche Basis wie Container-Tooltip-Konzept (Raster-Bausteine dann nach simplelib). **Verstärktes Bündel und Farben** bleiben in **SimpleBuilding** (dort liegen Rezept, Händler, Auswahl-Payload). Begründung: Modul-Unabhängigkeit – Sandwiches liest nur die Vanilla-Komponente, nicht SB-Klassen; Interfaces erkennt Bündel über `bundle_contents`, nicht über SB-IDs. Bleibt das Feature ohne SB/Sandwiches voll nutzbar (Vanilla-Bündel).

## Stufen
1. Ansicht ohne 12-Grenze (Client), Config.
2. Scrollrichtung, optionale Tasten.
3. Farbvarianten/2. Größenstufe in SB nach Besitzer-Entscheid; Rucksack-Abgleich (Rucksack nicht in Bündel stapeln; Bündel in Rucksack Raster).

## Config-Schalter
`bundles.showAll` (an), `bundles.scrollAxis` (vertical/horizontal), `bundles.keyboardNav` (aus), SB: `reinforcedBundle.capacity`, `bundles.colored` (an).

## Server/Client
Ansicht/Scrollen: nur Client (Index per vorhandenem Auswahl-Payload). Entnahme jenseits Grenze: Server muss Index >12 akzeptieren – Payload prüfen (Bounds). Farben/Kapazität: Server + Datenpack.

## Risiken
Lizenz (CC-BY-NC-SA): nichts kopieren, eigenes Layout. Index-Grenzen im Auswahl-Payload; Sandwiches-Essen mit großer Auswahl; Vanilla ändert später Limit; Tooltip-Größe auf kleinen GUI-Skalen.

## Tests
Client-Smoke: Bündel mit 20 Stacks zeigt 20; Server-Test: Auswahlindex 13+ gültig, >n abgelehnt; Sandwiches-Test `bundleEating` bleibt grün; Config aus -> Vanilla.

## Offene Fragen an den Besitzer
1. Ansicht in Simple Interfaces (Empfehlung) oder SB?
2. Sollen Farb-/Größenvarianten kommen? Empfehlung: nur Ansicht jetzt, Varianten später, da verstärktes Bündel schon "größer" ist.
3. Tastenbedienung (Pfeile/Leertaste) nötig? Empfehlung: nein, Mausrad reicht.
4. Soll Essen aus Bündeln (Sandwiches) auch aus SB-Rucksäcken gehen? Empfehlung: nein, später auf Wunsch.
5. Scrollrichtung Standard? Empfehlung: wie Vanilla (vertikal).
