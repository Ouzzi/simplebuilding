# Seltenheiten, Feuerfestigkeit und Namensmuster

Stand 2026-09-28. Gilt auf allen Linien (26.2, 1.21.11, 26.3/26.4 ueber die Overlays) und allen Loadern;
festgenagelt von `DataIntegrityTests#modItemRaritiesFollowTheFamilyScheme` und
`#modItemNamesFollowTheFamilyPatterns` (Katalog `data_integrity_game_test_mod_item_*`).

## 1. Vanilla als Anker (26.2, aus `Items.java`)

| Vanilla | Seltenheit |
|---|---|
| Netheritschwert, -ruestung, -barren, -platten, -block | COMMON (feuerfest) |
| Goldener Apfel | COMMON |
| Verzauberter goldener Apfel | RARE (nicht mehr EPIC) |
| Netherit-Aufwertung, die meisten Besatz-Vorlagen | UNCOMMON |
| Besatz-Vorlagen Ward/Eye/Vex/Spire | RARE, Silence EPIC |
| Mob-Koepfe | UNCOMMON (Witherskelett RARE, Drache EPIC) |
| Bergungskompass, Echoscherbe | UNCOMMON |
| Elytra, Streitkolben, Schwerer Kern, Drachenei | EPIC |
| Barriere, Licht, Strukturleere, Befehlsbloecke | EPIC |

Vanilla haelt seine Werkzeug- und Ruestungsleiter (Holz bis Netherit) durchgehend COMMON; die
Verzauberung hebt die angezeigte Seltenheit (COMMON/UNCOMMON -> RARE, RARE -> EPIC).

## 2. Das Schema der Mod

| Familie | Regel | Beispiele |
|---|---|---|
| **Werkstoffe und Bauklötze** | COMMON auf jeder Stufe, wie Netheritbarren | Enderitbarren, -platten, -klumpen, Block aus Enderit, Rohenderit, Netheritklumpen, Rissiger Diamant, Astralit-/Nihilit-/Enderquarz-Paletten, Baustellenlicht |
| **Ausrüstung** (Werkzeuge, Waffen, Rüstung, Meißel, Spachtel, Vorschlaghämmer, Baustäbe) | COMMON auf jeder Stufe, wie Vanillas Netheritschwert | Enderitschwert, Enderit-Vorschlaghammer, Netheritmeißel |
| **Stufenfamilien** (Bündel, Köcher, Rucksäcke, Trichter, Öfen, Räucheröfen, Schmelzöfen, Kolben, Kerne, Druckplatten, Pads, Stufen-Nahrung) | Grund-/Verstärkt-/Kupfer-/Eisen-/Gold-/Diamant-Stufe COMMON, **Netherit UNCOMMON** (wie die Netherit-Aufwertung), **Enderit EPIC** | Netherittrichter UNCOMMON, Enderitofen EPIC, Netheritkern UNCOMMON, Enderitapfel EPIC |
| Pads | nach dem Material, mit dem die Stufe gebaut wird: Netherit-Druckplatte -> UNCOMMON, Enderit-Platte/-Kern -> EPIC | Spawn-Teleporter II-IV und Trank-Pad I UNCOMMON; Flugpads I-III, Feines Elytra-Pad V, Trank-Pad II/III, Spawn-Teleporter V EPIC |
| **Verzauberte Nahrung** | RARE wie der verzauberte goldene Apfel, Enderit EPIC | Verzauberter Netheritapfel RARE, Verzauberter Enderitapfel EPIC |
| **Vorlagen** | Aufwertungen UNCOMMON (wie die Netherit-Aufwertung), Besatz-Aufwertungen RARE (eine Stufe über den Vanilla-Besatzvorlagen, aus denen sie entstehen) | Basis- und Enderit-Schmiedevorlage UNCOMMON; Leuchtende/Strahlende Schmiedevorlage RARE |
| **Geräte** | nach der wertvollsten Zutat: Grundzutaten und Eisen-/Kupfer-/Goldkerne COMMON, Vanilla-UNCOMMON-Zutat (Echoscherbe) UNCOMMON, Enderit EPIC | Geschwindigkeitsmesser, Rotator, Oktanten, Magnet, Amethystlinse, Blaupause COMMON; Erzdetektor UNCOMMON; Echolot EPIC |
| **Köpfe** | UNCOMMON wie Vanillas Mob-Köpfe | Lohenkopf |
| **Bücher** | COMMON wie das beschriebene Buch | alle Handbücher |
| **Easter** | EPIC wie das Drachenei | Lustiger Stock |
| **Technik** | EPIC wie Barriere/Licht | Kreativ-Platzhalter |
| Spawn-Elytra | COMMON: Leihgabe des Elytra-Pads, ihr Name ist eigens aqua/kursiv gefärbt | - |
| Altlasten | wie ihre Familie | alte Spachtel COMMON (Ausrüstung), altes Netherit-/Enderit-Flugpad UNCOMMON/EPIC |
| Abklingende Trank-Pads | wie ihre Stufe: dasselbe Item mit der Komponente `potion_pad_cooldown` (Restzeit), nur Modell und Tooltip anders; feuerfest wie alle Trank-Pads, Stapelgröße 1 wie jedes Pad | abklingendes Trank-Pad I UNCOMMON, abklingendes Verstärktes/Durchtränktes Trank-Pad II/III EPIC |

## 3. Feuerfestigkeit

Feuerfest ist genau, was `netherite_`/`enderite_` heißt (Werkstoffe, Ausrüstung, Maschinen, Lager,
Kerne, Nahrung, Druckplatten, Pads - seit 2026-09-28 auch der **Netheritklumpen**) oder mit Netherit/Enderit
gebaut wird: verzauberte Äpfel, Echolot, Spawn-Teleporter II-IV (Netherit-Druckplatten), alle Trank-Pads,
Flugpads I-III, Feines Elytra-Pad V. Ausnahmen mit eigenem Grund: Spawn-Elytra (verbrennt nicht über Lava)
und der Lustige Stock. Nicht feuerfest: Rohenderit (es steckt noch kein Netherit darin) und die
Enderit-Schmiedevorlage (Diamanten + Endstein; brennt wie Vanillas Netherit-Aufwertung).

## 4. Namensmuster

- **Englisch**: Stufenwort vorn ("Reinforced X", "Netherite X", "Enderite X"); Vanilla-Muster "Block of X",
  "X Ingot", "X Nugget", "X Pressure Plate", "X Smithing Template", "X Stairs/Slab/Wall/Bricks/Ore/Pillar".
  Pad-Stufen enden auf die römische Zahl, Altlasten auf "(Legacy)".
- **Deutsch**: Vanilla-Gegenstücke heißen wie Vanillas Netherit-Stück mit getauschtem Werkstoff
  (Enderitharnisch, Enderitbeinschutz, Enderitspeer, Enderitplatten, Enderitklumpen, Rohenderit,
  Enderitblock). Sonst: Werkstoff und einfaches Nomen zusammen (Netheritmeißel, Enderitkern, Netherittrichter,
  Enderitofen, Netheritkolben, Netheritbündel, Enderitköcher, Netheritapfel, Astralitziegel, Nihiliterz),
  Bindestrich vor zusammengesetzten oder fremden Nomen (Netherit-Vorschlaghammer, Kupfer-Baustab,
  Enderit-Schmelzofen, Netherit-Räucherofen, Diamant-Druckplatte, Netherit-Rucksack, Enderit-Schmiedevorlage,
  Enderit-Elytra-Pad, Netherit-Chunk-Lader). Innerhalb einer Familie auf allen Stufen gleich. Kein Englisch
  außer Eigennamen ("Nugget" heißt "Klumpen" wie Vanillas Eisenklumpen); Altlasten auf "(alt)".
- Ausnahme: Spawn-Teleporter V trägt kein "Enderite"/"Enderit" (Besitzer 2026-09-28).

## 5. Änderungen am 2026-09-28

Seltenheit: Magnet UNCOMMON -> COMMON, Blaupause UNCOMMON -> COMMON, Verstärkter Rucksack UNCOMMON -> COMMON,
Amethystlinse EPIC -> COMMON, Erzdetektor RARE -> UNCOMMON, Verzauberter Netheritapfel EPIC -> RARE,
Enderit-Schmiedevorlage COMMON -> UNCOMMON, Kreativ-Platzhalter COMMON -> EPIC; Netheritkern, Netherittrichter,
-kolben, -ofen, -räucherofen, -schmelzofen, Netheritapfel/-karotte, Netherit-Druckplatte, Netherit-Pads,
Spawn-Teleporter II-IV und Trank-Pad I COMMON -> UNCOMMON; Enderitkern, Enderitapfel/-karotte, Flugpad I/II
COMMON -> EPIC. Feuerfest neu: Netheritklumpen, Spawn-Teleporter II-IV, Flugpad I/II.

Namen: siehe Commit; en u. a. "Block of Enderite", "Block of Cracked Diamond", "... Smithing Template" für alle
vier Vorlagen, "Netherite/Enderite Flypad (Legacy)"; de u. a. Klumpen statt Nugget, Enderitharnisch,
Enderitbeinschutz, Enderitspeer, Enderitplatten, Rohenderit, Netherit-/Enderitbündel, -köcher, -apfel,
-karotte zusammengeschrieben, Basis-/Enderit-Schmiedevorlage, Astralit-/Nihilit-Paletten zusammengeschrieben,
"Netherit-/Enderit-Flugpad (alt)".
