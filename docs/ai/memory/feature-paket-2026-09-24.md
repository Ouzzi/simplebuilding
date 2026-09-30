---
name: feature-paket-2026-09-24
description: Vom Besitzer entschiedene Details des Feature-Pakets vom 2026-09-24 (Kolben, Rucksack, Enderit-Maschinen, Rezepte), damit spaetere Aenderungen sie nicht versehentlich kippen
metadata:
  type: project
---

Umgesetzt und gepusht (f5288a9). Entscheidungen des Besitzers:
- Unzerstoerbar = Haerte -1 ohne Technik-Tag `piston_breach_immune`, plus verstaerkter Tiefenschiefer; Endportalrahmen per Config `pistonsBreachEndPortalFrames`. Kein Drop.
- Netherit-/Enderitkolben verbrauchen Redstoneblock + sich selbst; Enderit bricht 3 tief ("angemessen dem Preis"). Verstaerkter (auch klebriger) Kolben schiebt 1 Unzerstoerbares, verbraucht den Redstoneblock.
- Netherit-/Enderit-Maschinen nur per Hammer-Aufwertung in der Welt (1 Klumpen pro Block, 5 s, Schlag/Sound/Partikel je Sekunde), keine Werkbank-Rezepte mehr.
- Enderit-Schrott 72000 Ticks (1 h im Vanilla-Schmelzofen); Ofen-Boni belohnend, nicht nervig.
- Rucksack: 9/18/27+6/36+14, Netherit-Spalte rechts, Enderit-Spalte links, Spalten anders getoent (spaeter Spezialfelder); Taste B oeffnet Vanilla-Inventar + Rucksack nur wenn getragen; Constructor's Touch = Hotbar nachfuellen, dazu Funnel; verst. Rezept D-L-D / L-R-L / L-L-L.
- "Diamantklumpen" = bestehendes `diamond_pebble`. Verstaerkter Koecher ist Stufe zwischen Koecher und Netherit-Koecher.
- Offen beim Besitzer: Fragebogen docs/FRAGEBOGEN-BAUWERKZEUGE.md, Buecher-Konzept docs/BUECHER-KONZEPT.md (nur Konzept), GitHub Pages aktivieren (docs/WIKI-HOSTING.md).
