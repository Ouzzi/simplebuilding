# PLAN A-RARITY06 (06.11.2026): Ränge auf Items (TIP05 = A) + Drachen-Tönung Stufe 2 (WI26-6)

Besitzer-Abnahme 06.11.: TIP05 = A (Varianten A für Legendary/Corrupted/Void schon in Production). WI26-6 = A mit Ergänzung.

## Ist-Zustand (Mechanik)
- Ränge: `SimpleRarity` (SimpleLib) = CURSED, MYTHIC, EVIL, LEGENDARY, CORRUPTED, VOID, dazu Vanilla COMMON/UNCOMMON/RARE/EPIC. Gesetzt über `SimpleRarity.apply(Item.Properties)` = Item-Komponente `simplelib:rarity` (Vanilla-Rarity bleibt EPIC als Fallback).
- Vorher nur einzeln und von Hand gesetzt: Biome-Brush CURSED, Wings/Herz/Juwel (WitherWings), Beacons (EVIL/MYTHIC). Alles andere Vanilla-Rarity ohne Rang.

## Mechanik (neu)
`com.simplelib.api.RarityRules.forPath(path)` = eine Tabelle für alle Module, `RarityRules.apply(path, properties)` wird in den Registern beim Bauen der Properties aufgerufen: `ModItems.registerItem` (über `LibServices.rarity`, 26.2-Twin gibt Properties unverändert zurück), `OresRegistry` (Blöcke, Items, Template), `LibMaterials.item`, `Trials` (Vault-Items, Schlüssel, Tränke). Nicht genannte Items behalten ihre Vanilla-Rarity.

## Regeltabelle
| Rang | Gruppe (thematisch) | Pfade |
|---|---|---|
| EVIL | Böses: Wither-/Nether-Schwingen, Evil-Prüfungen, Evil-Beacons/-Conduits | `wither_wings`, `nether_wings`, `evil_*` (Vault, Schlüssel, Tränke, Beacons, Conduits, Template) |
| VOID | Leere: Void-Schwingen, Void-Prüfungen, Nihil-Technik | `void_*` (Wings, Vault, Schlüssel, Tränke), `nihil_redstone[_block/_torch]`, `nihil_piston/rail/vault`, `nihilith_shard/lamp/end_rod/book` |
| CORRUPTED | Kaputtes/Glitch | `cracked_diamond`, `cracked_diamond_block` |
| LEGENDARY | End-Endgame | `enderite_*` (Barren, Block, Werkzeug, Rüstung, Container, Maschinen, Äpfel, Kerne, Template; nicht Nugget/Scrap/Raw), `end_wings`, `ender_heart` |
| MYTHIC | seltenste Materialien/Gegenstück zu Nihil | `ender_gem`, Astral-Technik (`astral_redstone[_block/_torch]`, `astral_piston/rail/vault`, `astralit_dust/lamp/end_rod`), Strong-Beacons (schon) |
| CURSED | bewusst verflucht (explizit) | Biome-Brush, Wither-Mob-Items (unverändert) |
| Vanilla | alles übrige (Baublöcke, Netherit-Zusatzteile, Zwischenmaterialien) | unverändert (kein Lärm im Tooltip) |

Änderungen gegenüber vorher: Wither-/Nether-Schwingen CURSED -> EVIL, Void-Schwingen MYTHIC -> VOID, Ender-Herz CURSED -> LEGENDARY; Vault-Items/Schlüssel/Tränke Evil/Void bekommen EVIL/VOID.
Rückfrage (kein Blocker): Soll Astral lieber LEGENDARY statt MYTHIC sein? Aktuell MYTHIC (Opal passt zum Astral-Thema).

## WI26-6 Ergänzung: Tönung Stufe 2
- `DragonTinted`: Boolean wird `int tintLevel` (0 aus, 1 Chorus-Violett, 2 Seelen-Blau + Chorus-Violett); `tinted()`/`setTinted()` bleiben als default-Methoden.
- Gesyncte Entity-Daten BOOLEAN -> INT (`EnderDragonMixin`), Render-State trägt die Stufe.
- Config `tintJewelFromDragon` = 10 (0 = aus), neben `tintFromDragon` = 4; `FightSession.tintLevel(fight)`: >= tintJewelFromDragon -> 2, sonst >= tintFromDragon -> 1.
- Render Stufe 2: zwei additive Glow-Durchgänge (Violett 0xB070E0 und Seelen-Blau 0x5CC8FF) mit gegenläufiger Puls-Stärke (sin über ageInTicks): abwechselnd pulsierender Zweifarb-Glow. Keine Textur im Repo.
- Lang EN+DE, Wiki-Manual, `check_data.py` (30 Config-Keys).

## Tests (ein sbt-Lauf)
Compile `:mc26_3:fabric` + Gametests gefiltert: `simplelib:rarity_rules` (Stichprobe + jeder Rang mindestens einmal + Cracked-Diamond-Item), `simplemobs:wings_items` (Wings/Herz/Vaults/Schlüssel/Tränke), `simpleenddragonfight:*` (Tönungs-Flag und Stufen 4/10, Sperren bei 0).
Nicht getestet: Client-Sicht des Zweifarb-Glows (kein Client-Lauf im Gate-Zeitraum), Forge/NeoForge.
