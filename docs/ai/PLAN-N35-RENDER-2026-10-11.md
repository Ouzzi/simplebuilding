# Nachtrag 35 – Darstellung (Branch claude-n35-render, 2026-10-11)

Teilauftrag aus `.claude/QUEUE.md` Nachtrag 35: Kisten-Inhalt, Kreativ-Baustab, Resonanz-Feld, gemeißeltes Eis,
Astral-/Nihil-Schalter. Referenzen des Besitzers: `~/previews/n35/crate_ref_apples.png`, `crate_ref_iso.png`.

| Punkt | Ist | Plan | Dateien |
|---|---|---|---|
| Kisten | flache Sprites 3,8 px, nur die zwei obersten Lagen (3x3-Raster) | jede Lage (Stapel) als 2x2-Haufen 3D-Items (5,8 px breit, 1 px dick), leicht versetzt/gedreht, bis an die Wände → durch die Lattenlücken sichtbar; Deckel 32 Items, ab 16 Blöcken nur die zwei obersten Lagen | `woodwork/client/CrateRenderer.java` |
| Kreativ-Baustab | eigene Quarz-/Stern-Form (passt nicht zur Familie) | Form des Diamant-Baustabs, nur Kopf umgefärbt (Kreativ-Magenta/Rosa) | `tools/textures/creative_items_2026_10_09.py` |
| Resonanz-Feld | Text 1:1, Feld 44 px breit | Text mit 0,75 skaliert, Feld schmaler (Breite = skalierte Textbreite) | `util/TrimStatsLayout.java`, `client/gui/TrimStatsPanel.java`, Test `TrimWiringTests` |
| Gemeißeltes Eis | Motive um Pixel 7 (statt 7,5) zentriert, Zufallsschlieren, Licht schräg → schief | Motive um die echte Mitte gespiegelt (beide Achsen), 1-px-Rillen, Licht nur von oben (links/rechts symmetrisch), keine Zufallsschlieren im Feld | `tools/textures/nature_blocks_2026_10_09.py` |
| Schalter | flache Platte, Rahmen weggeschnitten (UV 2..14), Zustand folgt `power` (verzögert), kaum erkennbar | Schiebeschalter: Platte mit Rahmen und Schlitz, Schieber (Knopf) vorn = aus, hinten = an mit leuchtendem Schlitz/Edelstein; Zustand folgt `enabled`; beide Kanäle gleich, nur Farbe | neuer Generator `tools/textures/end_switches_2026_10_11.py` (Texturen, Modelle, Blockstates), `EndSignalBlock` (Form 3 px hoch) |

Verifikation: Generatoren `--check`, Compile 3 Loader, Gametests `crate*`, `wand*`, `end_signal*`/`end_system*`, `resonance*`,
Client-Lauf fabric-263 (woodwork + hud-and-tooltip, neue Szene mit Schaltern/Eis/Stab), Vorher/Nachher-Bild.
