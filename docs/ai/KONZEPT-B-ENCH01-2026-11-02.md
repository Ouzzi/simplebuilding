# Konzept b-ench01: Dark Enchanter und End-Enchanter (02.11.2026)

Branch `b-ench01`, nur MC 26.3 Fabric, NUR Konzept + Vorschau (`abnahme-b/b-ench01-preview.html`), kein Gradle, nichts gebaut.
Queue: ZO7 (Teil), BS25-20, WI26-1. Entschieden: ZO7 = A (Astral-Tisch wird Dark Enchanter, End-Enchanter neu, 01.11.).

## Stufen
| | Vanilla | Dark Enchanter (heutiger Astral-Tisch) | End-Enchanter (neu) |
|---|---|---|---|
| Herstellung | Vanilla | Tisch + Hammer (Netherit), 8 Netherit-Nuggets | Dark + Hammer (Enderit), 8 Enderit-Nuggets |
| Stoffe | Buecherregal | Lohen-Obsidian 5x5, Crimson/Warped-Lohenholz-Regale | End-Obsidian 5x5 (Obsidian + Endstein), Aschholz/Daemmerholz-Regale (Simple End) |
| Tier/Budget | 30 | 50 (`TIER_MAX`) | 60 (neu `TIER_END`, `MAX_PRICE` 60) |
| Regler | 3 Angebote | alle bis Vanilla-Maximum | wie Dark + `endgame_levels` (Schutz V, Federfall V, Aufschlag +6, nie VI) |
| Goldapfel | - | 40 Level | 40 Level (Konstanten `APPLE_LEVELS_NETHER/ENDERITE` entfallen) |

Aenderung am Bestand: `ENDGAME_MIN_TIER` (heute 50, am Astral-Tisch) wandert auf den End-Enchanter (Stufe `END`); `levelCap` liest die Stufe des Tischs, nicht mehr nur das Tier. Vanilla-`max_level` bleibt unangetastet (PRINZIPIEN Endgame-Absatz).

## Regeln
- Umbenennung per IdMove: `simplebuilding:astral_enchanting_table` -> `dark_enchanter` (Alias bleibt, Block/BlockEntity/Menue in Welten bleiben). Lang EN/DE, Wiki-Manual, Guide, Money-Preis-Tabelle, Fragentisch-Text.
- Neue Stufe im Motor: `AstralEnchanting.EnchanterStage { DARK, END }` (Tier-Obergrenze, Regal-Tag, Boden-Tag, Endgame ja/nein); `registerEndgame` bleibt der Hook.
- Regale/Boden per Tags (`#simplebuilding:end_enchant_shelves`, `#simplebuilding:end_enchant_floor`), damit Simple End sie liefert und SB sie nur liest. Ohne Simple End gibt es keinen End-Tisch (Rezept per Mod-Bedingung), Dark bleibt voll spielbar.
- WI26-1: Abbau eines Tisches gibt 8 Nuggets der Herstellung zurueck (heute faellt ein Barren) - Datenwert in der in_world_upgrades-Rueckgabe.
- Netherit-Nuggets: vorhanden im Repo (Simple Tools/Storage); fuer Dark wuerde die Hammer-Stufe `netherite` gelten.

## Vanilla-Nutzen / Eingriff (A12)
Nutzen: lohnendes, schwer erreichbares Endspiel. Eingriff gering: Vanilla-Tisch und Amboss unveraendert, Schalter `server.features.endgameEnchantLevels`; nur der End-Tisch hebt Regler ueber das Maximum.

## Modul-Zuordnung
Motor, Blocklogik, Menue: Simple Building (bestehend). End-Regale und End-Boden (Bloecke, Texturen, Tags): Simple End. Enderit-Nuggets: Simple Ores. Jedes Modul bleibt allein spielbar (PRINZIPIEN-MODUL-UNABHAENGIGKEIT).

## Offene Rueckfragen (Vorschau, Code `ENCH01|stoffe|upg|dark|end|apfel|cap`)
1. End-Stoffe (A Aschholz-Regale + End-Obsidian, Empfehlung; B Purpur/Chorus; C keine neuen Bloecke).
2. Herstellung (A Dark Netherit-Nuggets / End Enderit, Empf.; B Dark bleibt Enderit).
3. Textur Dark (A heutige, Empf.; B Glut-Karmesin). 4. Textur End (A Purpur/Auge; B Endstein, Empf.).
5. Apfel 40/40 (Empf.) oder 50/60. 6. Schutz V nur End (Empf.) oder auch Dark.

## Stufenplan nach Antwort
E2a Stufen-Motor + Tags + Dark-IdMove (1 Tag, Gametests Tier/Cap/Apfel); E2b Block, Menue-Titel/Stufenanzeige, Rezepte/Upgrade, Rueckgabe (1 Tag); E2c Regal/Boden in Simple End + Texturen nach Wahl (1 Tag); Wiki/Lang/Preise, check_data. NeoForge/Forge erst auf Anforderung.

## Texturen (A8b)
Eigene Pixel, Generator `tools/textures/ench01_preview.py` (misst harte Uebergaenge). Vanilla-Tisch 8,3 % oben; Dark B 9,8 %; End A 16,7 %, End B 15,2 % (Nachschliff nach Wahl); Dark A = abgenommener Altstand (31,5 %, wird nicht angefasst).
