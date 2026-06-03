# SimpleBuilding

Mod ID: simplebuilding

SimpleBuilding erweitert Minecraft 1.21.11 (Fabric) um Bau-, Mining- und Utility-Systeme fuer Survival-Gameplay im grossen Stil.
Diese Readme ist die vollstaendige, aktualisierte Referenz fuer Features, Items, Enchantments, Config, Trades, Loot und Progression.

## Version und Anforderungen

- Minecraft: 1.21.11
- Loader: Fabric Loader 0.18.4
- Fabric API: 0.140.0+1.21.11
- Java: 21
- Mod Version: 1.2.52
- Empfohlen:
- Cloth Config (bereits als Abhaengigkeit eingebunden)
- Mod Menu (fuer Ingame-Config)

## Kernsysteme

- Building Wands mit mehreren Platzierungsmodi und Enchantments
- Sledgehammers fuer Flaechenabbau, Tiefe und Material-Override
- Chisel/Spatula fuer In-World-Blocktransformationen
- Octant mit Messung, Shape-Preview und GUI
- Reinforced/Netherite/Enderite Storage und Utility-Items
- Verstarkte Maschinen und spezielle Redstone-Blocks
- Endgame-Materialien: Enderite, Astralit, Nihilith
- Custom Enchantments (19 Stueck)
- Armor-Trim-Benefits mit dynamischem Multiplikator
- Weltintegration: End-Ores, Villager Trades, Wandering Trader, Loot-Injections

## Vollstaendige Enchantment-Referenz

| Enchantment ID | Name | Max Level | Hauptziel |
|---|---|---:|---|
| fast_chiseling | Fast Chiseling | 2 | Chisel/Spatula |
| constructors_touch | Constructor's Touch | 1 | Utility-Tools |
| range | Range | 3 | Chisel + Mining Tools |
| deep_pockets | Deep Pockets | 2 | Bundles/Storage |
| master_builder | Master Builder | 1 | Wand + Extra Inventory |
| funnel | Funnel | 2 | Bundle/Storage |
| radius | Radius | 1 | Sledgehammer |
| override | Override | 2 | Sledgehammer |
| strip_miner | Strip Miner | 3 | Pickaxes |
| cover | Cover | 1 | Building Wands |
| bridge | Bridge | 1 | Building Wands |
| linear | Linear | 1 | Building Wands |
| vein_miner | Vein Miner | 5 | Pickaxe/Axe |
| kinetic_protection | Kinetic Protection | 4 | Armor |
| drawer | Drawer | 8 | Bundle/Storage |
| versatility | Versatility | 2 | Mining Tools |
| color_palette | Color Palette | 1 | Wand + Extra Inventory |
| break_through | Break Through | 2 | Sledgehammer |
| double_jump | Air Jump | 2 | Boots |

Hinweise:
- Wand-Modes sind exklusiv ueber Enchantment-Exclusive-Sets abgesichert.
- Kinetic Protection ist im Armor-Exclusive-Set integriert.
- Drawer besitzt hohe Max-Level-Progression (bis 8).

## Vollstaendige Item- und Block-Referenz

### 1) Werkzeuge und Utility

- Building Wands:
- copper_building_wand
- iron_building_wand
- gold_building_wand
- diamond_building_wand
- netherite_building_wand
- enderite_building_wand
- Sledgehammers:
- stone_sledgehammer
- copper_sledgehammer
- iron_sledgehammer
- gold_sledgehammer
- diamond_sledgehammer
- netherite_sledgehammer
- enderite_sledgehammer
- Chisels:
- stone_chisel
- copper_chisel
- iron_chisel
- gold_chisel
- diamond_chisel
- netherite_chisel
- enderite_chisel
- Spatulas:
- stone_spatula
- copper_spatula
- iron_spatula
- gold_spatula
- diamond_spatula
- netherite_spatula
- Sonstige Utility:
- octant
- octant_<16 dye colors>
- magnet
- rotator
- ore_detector
- velocity-gauge

### 2) Storage

- reinforced_bundle
- netherite_bundle
- enderite_bundle
- quiver
- netherite_quiver
- enderite_quiver

### 3) Materialien, Cores, Templates, Nahrung

- Cores:
- copper_core
- iron_core
- gold_core
- diamond_core
- netherite_core
- enderite_core
- Enderite/Astralit/Nihilith:
- raw_enderite
- enderite_scrap
- enderite_ingot
- enderite_nugget
- astralit_dust
- nihilith_shard
- netherite_nugget
- Templates:
- basic_upgrade_template
- enderite_upgrade_template
- glowing_trim_template
- emitting_trim_template
- Nahrung:
- netherite_carrot
- netherite_apple
- enderite_carrot
- enderite_apple
- enchanted_netherite_apple
- enchanted_enderite_apple
- Weitere Materialien:
- diamond_pebble
- cracked_diamond

### 4) Enderite Gear

- enderite_sword
- enderite_spear
- enderite_pickaxe
- enderite_axe
- enderite_shovel
- enderite_hoe
- enderite_helmet
- enderite_chestplate
- enderite_leggings
- enderite_boots

### 5) Maschinen- und Functional-Blocks (als BlockItems registriert)

- reinforced_hopper
- netherite_hopper
- reinforced_piston
- netherite_piston
- reinforced_furnace
- netherite_furnace
- reinforced_smoker
- netherite_smoker
- reinforced_blast_furnace
- netherite_blast_furnace
- construction_light
- cracked_diamond_block

### 6) Deko- und End-Blocks

- polished_end_stone
- purpur_quartz_checker
- lapis_quartz_checker
- blackstone_quartz_checker
- resin_quartz_checker
- astral_purpur_block
- nihil_purpur_block
- astral_end_stone
- nihil_end_stone
- suspended_sand
- suspended_gravel
- levitating_sand
- levitating_gravel
- enderite_block
- astralit_ore
- nihilith_ore

## Spear-Status (wichtig)

- Enderite Spear ist als Spear-Item im Vanilla-Spear-Format eingebunden (kein Trident-Wurfverhalten).
- Modell/Anzeige entspricht dem 1.21.11-Spear-Standard:
- display_context select
- fallback in_hand model
- swap_animation_scale 1.95
- Eigener Spear-Tag-Eintrag vorhanden ueber minecraft:spears.

## Armor Trim System

SimpleBuilding erweitert Armor Trims um Gameplay-Boni.

### Pattern-basierte Effekte (Auszug)

- Damage-Reduktion je nach Pattern und Damage-Typ (z. B. projectile, magic, explosion, fire, fall)
- Zusatzeffekte wie Swim-Speed, Stealth, Exhaustion-Reduktion, XP-Multiplikator, Luck-Boni

### Material-basierte Effekte

- Vanilla-Materialien und Custom-Materialien werden ausgewertet
- Enderite/Astralit/Nihilith besitzen eigene Skalierung und/oder Zusatznutzen

### Dynamische Multiplikator-Logik

Gesamtformel:

globalMultiplier * xpMultiplier * survivalMultiplier * combatMultiplier

Einfuesse:
- XP-Level
- Bewegung/Survival seit letztem Tod
- Kampfaktivitaet seit letztem Tod

Config-relevant:
- enableArmorTrimBenefits
- trimBenefitBaseMultiplier
- maxMultiplierLimit

## Weltgen

- Astralit und Nihilith Ores werden in End-Biomes generiert.
- Eigene Configured/Placed Features in Registry-Datagen.
- Placement-Logik differenziert Oberflaechen/Unterseiten-Fokus.

## Trading Integration

### Villager

- Librarian: Building-/Mining-Enchant-Books (gewichtete Pools nach Level)
- Mason: Cores und Building-Wand-Angebote
- Toolsmith: Enchanted Chisels/Sledgehammers/Pickaxe-Angebote

### Wandering Trader

- Octant, Reinforced Bundle, Cores
- Spezielle Enchanted Books (z. B. Bridge/Radius Pool)

Alles ist ueber Config toggelbar (siehe Abschnitt Config).

## Loot Table Integration

Injektionen in viele Vanilla-Strukturen, unter anderem:

- Stronghold Library
- End City
- Ancient City
- Bastion
- Nether Fortress
- Pillager Outpost
- Woodland Mansion
- Buried Treasure
- Dungeons/Mineshaft
- Trial Chambers

Enthaelt je nach Struktur:
- Mod-Enchanted Books
- Mod-Items (z. B. Wand, Sledgehammer, Quiver, Bundle, Cores)
- Endgame-Drops (z. B. Enderite Scrap/Template, spezielle Aepfel)

## Config-Optionen (AutoConfig)

### Tools

- invertOctantSneak
- buildingHighlightOpacity
- enableToolAnimations
- enableChiselAnimation
- invertBundleInteractions

### World/Economy

- enableVillagerTrades
- enableWanderingTrades
- enableLootTableChanges

### Global

- enableDoubleJump
- enableArmorTrimBenefits
- trimBenefitBaseMultiplier
- maxMultiplierLimit

## Keybinds und Client-Funktionen

- Toggle Highlights
- Toggle Octant Figure
- Open Tool Settings

Client-HUD/Render:
- Rangefinder Overlay
- Speedometer Overlay
- Sledgehammer/Building Wand Highlights

## Commands

Operator Command:

- /simplebuilding config getTrimMultiplier
- /simplebuilding config setTrimMultiplier <value>

## Upgrade-Systeme

### Basic Upgrade Template

- Tier-Upgrades fuer Werkzeuge mit Daten-/NBT-Erhalt.
- Eigene Smithing-Rezeptlogik mit count-basiertem Input.

### Enderite Upgrade Template

- Upgrade von Netherite-Equipment auf Enderite.

### Trim-Upgrades

- Glowing Trim Upgrade
- Emitting Trim Upgrade

## Bekannte Hinweise

- Die Readme bildet den aktuellen Code-Zustand in diesem Repository ab.
- Falls Datagen-Runs lokal durch externe Duplikate scheitern, sind bestehende Konflikte im Workspace zu pruefen (nicht zwingend Mod-Feature-Fehler).

## Entwicklung

Build:

./gradlew build

Client starten:

./gradlew runClient

Datagen:

./gradlew runDatagen
