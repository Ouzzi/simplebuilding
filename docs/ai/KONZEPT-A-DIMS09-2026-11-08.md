# Konzept a-dims09: Verarbeitung des Dimensions-Schrotts (IG31-58), 08.11.2026

Besitzer 08.11.: Die Verarbeitung der Dimensional Scraps fehlt. **Jetzt nur Namen und Texturen**, die Mechanik kommt später.
Kette: Schrott besonders schmelzen → Zwischenmaterial → Zwischen-Block (wie Netherit-Schrottblock) → Kombination wie bei
Antikem Schrott (4 Schrott + 4 Gold = Netherit): Oberwelt mit Enderit, Nether mit dem Oberwelt-Ergebnis, End mit dem
Nether-Ergebnis → je Dimension ein schöner End-Block wie der Enderit-Block.

Vorschau: `/root/sb-tools-b/previews/a-dims09-preview.html`, Generator `tools/textures/dims09_2026_11_08.py`
(Ausgabe nur nach `build/a-dims09/`, nichts ist im Spiel eingebaut). Antwort: `DIMS09: name=…, tex=A|B + Anmerkung`.

## 1. Ist-Zustand

| Teil | Stand |
|---|---|
| Schrott-Blöcke | `simpleores:dimensional_scrap`, `nether_dimensional_scrap`, `end_dimensional_scrap` (`OresRegistry`, `DimensionalScrapBlock`): kein Rezept, abbaubar nur mit Tag `simpleores:breaks_dimensional_scrap` (Enderit-Spitzhacke/-Vorschlaghammer), 250 s mit unverzauberter Enderit-Spitzhacke; feuerfest; Config `dimensionalScrap` (Standard an). |
| Vorkommen | je Dimension Erz-Feature Größe 2, `rarity_filter` 1/3 Chunks, Luftkontakt verwirft: Oberwelt Tiefenschiefer Y −64…−32, Nether Y 8…24, End Endstein Y 10…70. |
| Loot | droppt sich selbst. JEI: Info-Seite `RecipelessJeiInfo` („dimensional_scrap“). |
| Vorbild Netherit | Antiker Schrott → Schmelzen → Netheritplatten; 4 Platten + 4 Gold → Barren; Netherit-Schrottblock (9 Platten, a-blocks07) in Simple Ores. |
| Vorbild Enderit | Rohes Enderit ×4 → geschichtetes Rohenderit → Hochofen 288 000 Ticks → Enderitplatten; Barren = 4 Platten + 4 Diamant + 1 Netheritbarren (geformt); Enderitplatten-Block (9). |
| Hitze | Simple Crucibles: Stufen MEDIUM/HIGH/EXTREME (Seelenlava). `CrucibleJob.requiredHeat`: Items im Tag `simplecrucibles:needs_extreme_heat` brauchen EXTREME (heute nur `minecraft:ancient_debris`). Der Tiegel verarbeitet die normalen Schmelz-/Hochofen-Rezepte. |
| „Blaze Furnace“ | gibt es nicht. Simple Ovens hat verstärkte, Netherit- und Enderit-Öfen/-Hochöfen (`OvenTier`). Vorschlag unten nutzt Hochofen + Tiegel; ein eigener Lohen-Ofen wäre ein neues Modul-Feature (siehe 6, optional). |

## 2. Namen (EN / DE)

Zwischenmaterial (geschmolzener Schrott), drei Sätze:

| Satz | Oberwelt | Nether | End | Block |
|---|---|---|---|---|
| **A Empfehlung** | Rift Shard / Riss-Splitter | Nether Rift Shard / Nether-Riss-Splitter | End Rift Shard / End-Riss-Splitter | Block of Rift Shards / Riss-Splitter-Block |
| B | Dimensional Shard / Dimensionssplitter | Nether Dimensional Shard / Nether-Dimensionssplitter | End … | Block of Dimensional Shards / Dimensionssplitter-Block |
| C | Phase Fragment / Phasenfragment | Nether Phase Fragment / Nether-Phasenfragment | End … | Block of Phase Fragments / Phasenfragment-Block |

Begründung A: „Riss“ erklärt, woher der Schrott kommt (Risse zwischen den Dimensionen, das Leuchten der Schrott-Textur),
kurz, kollidiert mit nichts im Repo (geprüft: kein `rift`, `infern`, `zenit`, `terran` als ID). B bleibt nah am vorhandenen
Namen „Dimensionsschrott“, ist aber lang. C ist abstrakt.

Endmaterial je Dimension (Barren + Block):

| Satz | Oberwelt (+ Enderit) | Nether (+ Oberwelt) | End (+ Nether) |
|---|---|---|---|
| **A Empfehlung** | Terranite / Terranit | Infernite / Infernit | Zenithite / Zenithit |
| B | Earthrift / Erdriss | Emberrift / Glutriss | Starrift / Sternriss |
| C | Gaian Alloy / Gaia-Legierung | Pyric Alloy / Pyr-Legierung | Aetheric Alloy / Äther-Legierung |

Begründung A: „-it/-ite“ wie Netherit/Enderit, die Dimension steckt im Wortstamm (Terra, Infernum, Zenit = höchster Punkt =
Ende der Kette). IDs dann `terranite_ingot`, `terranite_block`, `infernite_…`, `zenithite_…`; Zwischen: `rift_shard`,
`nether_rift_shard`, `end_rift_shard`, `rift_shard_block`, `nether_rift_shard_block`, `end_rift_shard_block`.
DE: „Terranitbarren“, „Terranitblock“ (wie „Enderitbarren“, „Enderitblock“).

## 3. Kette je Dimension (Satz A)

| | Oberwelt | Nether | End |
|---|---|---|---|
| Schrott | Dimensionsschrott | Nether-Dimensionsschrott | End-Dimensionsschrott |
| Verarbeitung | Hochofen 800 Ticks (40 s), 2 EP; im Tiegel nur bei **extremer Hitze** (Tag `needs_extreme_heat`) | Hochofen 1200 Ticks (60 s), 3 EP; Tiegel extrem | Hochofen 1600 Ticks (80 s), 4 EP; Tiegel extrem |
| Zwischenmaterial | Riss-Splitter | Nether-Riss-Splitter | End-Riss-Splitter |
| Zwischen-Block | Riss-Splitter-Block (9 ↔ 1) | Nether-Riss-Splitter-Block | End-Riss-Splitter-Block |
| Kombination | 4 Riss-Splitter + **1 Enderitbarren** | 4 Nether-Riss-Splitter + **1 Terranitbarren** | 4 End-Riss-Splitter + **1 Infernitbarren** |
| Endmaterial | Terranitbarren | Infernitbarren | Zenithitbarren |
| End-Block | Terranitblock (9 ↔ 1) | Infernitblock | Zenithitblock |

Schmelzen „auf besondere Art“: Der normale Ofen kann es nicht (nur `blasting`-Rezept), der Hochofen braucht lange, der Tiegel
nur mit Seelenlava. So bleibt Simple Ores allein spielbar (Hochofen ist Vanilla, Modul-Unabhängigkeit), und mit Simple
Crucibles ist es dieselbe Stufe wie Antiker Schrott dort. Hochöfen aus Simple Ovens (Netherit/Enderit) beschleunigen
automatisch über ihre Stufe, ohne eigenes Rezept.

## 4. Balancing

**Kombination – Empfehlung K1: 4 Splitter + 1 Partnerbarren** (formlos). Die wörtliche Netherit-Analogie „4 + 4“ (K2) würde
sich über die Kette vervielfachen: 1 Zenithit = 4 Infernit = 16 Terranit = **64 Enderitbarren** – nicht spielbar. K1 kostet für
1 Zenithitbarren: 1 Enderitbarren + 4 + 4 + 4 Splitter (je Dimension 4 Schrott-Blöcke à 250 s Abbau). K3 (4 Splitter + 4
Partner-Nuggets) bräuchte neue Nugget-Items je Material – nur falls K1 zu billig wirkt.

**Seltenheit:** bleibt wie heute (1/3 Chunks, Größe 2, Abbau nur mit Enderit). Schrott → Splitter 1 : 1 (kein Ertragsbonus,
auch nicht im Tiegel). Erwartung: ein Barren je Dimension pro längerem Ausflug; ein End-Block (9 Barren) ist ein Langzeitziel.

**Stufe: neben, nicht über Enderit.** Keine neue Rüstungs-/Werkzeugstufe mit höheren Werten (sonst Machtzuwachs über
Enderit, A12 stark). Die Materialien sind **Prestige- und Bau-Materialien** mit kleinen Vanilla-Anbindungen.

**A12 – Vanilla-Nutzen / Eingriff:**
1. Nutzen: Ziel für späte Erkundung aller drei Dimensionen; End-Blöcke als Schmuckblöcke (wie Enderit-/Netheritblock);
   Leuchtfeuer-Basis (`beacon_base_blocks`) und Leuchtfeuer-Bezahlung (`beacon_payment_items`); Rüstungsbesatz-Material
   (Vanilla-Trim-Material, eigene Farbe je Dimension).
2. Änderung: neue Items/Blöcke und Rezepte, keine Weltgen-Änderung (Schrott gibt es schon), kein Kampfwert.
3. Eingriff: **gering** (nur Rezepte, Tags, Trim-Materialien). Optionale Ideen mit Besitzer-Freigabe, später: Enderit-Werkzeug
   mit Dimensionsmaterial veredeln → baut Dimensionsschrott schneller ab (Schleife ohne Kampfwert); Zenithitblock gibt
   schwaches Licht (7).

## 5. Texturen (Vorschau, nicht eingebaut)

Generator `tools/textures/dims09_2026_11_08.py` zeichnet alle Pixel selbst (Polygone + Paletten-Rampen, keine Vanilla-Datei
gelesen); Schrott-Körper und Riss-Farbe aus den eingecheckten Schrott-Texturen, End-Material-Paletten neu: Terranit
Tiefblaugrün mit **violetten** Enderit-Funken, Infernit Karmin-Glut mit **türkisen** Terranit-Funken, Zenithit Perlgold mit
**glutroten** Infernit-Funken – die Kette ist in den Akzenten sichtbar.

| Textur | A | B |
|---|---|---|
| Splitter (Item) | Splitter-Bündel, drei schräge Kristalle, leuchtender Kern | gerissene Platte, Zickzack-Riss, lose Splitter |
| Splitter-Block | Splitter in abgefaster dunkler Kachel, weicher Schein | gepresste Schichten, Fugen leuchten |
| Barren | Barren schräg von oben (3 Flächen), Ader des Partnermetalls | facettierter Edelstein-Barren, Naht des Partners |
| End-Block | Fase + zwei weiche Wirbelbänder (Enderit-Sprache, eigene Linien) | 2 × 2 Paneele mit Siegel (Oberwelt Raute, Nether Flamme, End Auge) |

Harte Übergänge (A8b, Schwelle 40): End-Blöcke A/B 12–14 % (Enderitblock 6 %), Splitter-Block A 8–17 %, B 14–27 %;
Items 28–46 % (Konturen; Enderitbarren 36 %, Enderitplatten 46 %). Nach der Wahl wird die gewählte Variante auf ~7 %
weichgezeichnet und mit Ebenen-Quelle (`layers.save`) eingecheckt.

## 6. Später umzusetzen (Aufwand)

| Schritt | Inhalt | Aufwand |
|---|---|---|
| 1 Registrierung | 3 Splitter, 3 Splitter-Blöcke, 3 Barren, 3 End-Blöcke in `OresRegistry` (feuerfest, `FIRE_RESISTANT`), Blockstates/Modelle/Items, Lang EN/DE, Kreativtab, Loot | M (½ Tag) |
| 2 Rezepte | 3 `blasting` (Schrott → Splitter), 6 × 9↔1 Splitter-Block, 3 Kombinationen, 6 × 9↔1 Barren-Block; Rezept-Fortschritte | S |
| 3 Tiegel | Schrott-Items in `simplecrucibles:needs_extreme_heat` (Eintrag mit `required: false`, nur mit Simple Crucibles) | S |
| 4 Tags | `beacon_base_blocks`, `beacon_payment_items`, `needs_diamond_tool`/`mineable/pickaxe`, Trim-Materialien (3 × `trim_material` + Item-Tag) | S–M |
| 5 Texturen | gewählte Variante final (A8b ~7 %, Ebenen), Trim-Paletten, Wiki-Kopien | M |
| 6 Doku | `RecipelessJeiInfo` „dimensional_scrap“ entfernen (hat dann Rezepte), Wiki `manual.json`, Handbuch-Seite 14, Questionary, Discover | S |
| 7 Tests | Gametests: Rezepte vorhanden, Tiegel braucht EXTREME, Standalone ohne Crucibles | S |
| optional | Lohen-Ofen (Blaze Furnace) als eigene Station in Simple Ovens | L, nur auf Wunsch |

Gesamt ohne Optionales etwa 1–1,5 Agenten-Tage, ein Fabric-Gate (Besitzer 22.10.: nur 26.3 Fabric).
