## Changelog:
- Basic upgrade templates  (Stone-Iron-Gold-Diamond)
- - one item and material to upgrade a tool from one tier to another - smithing table recipe
- - all tool data and enchantments are kept
- - upgradeable: pickaxe, axe, shovel, hoe, sword, chisel, sledgehammer.
- - upgrade recepie reuires the base tool + material ingot + upgrade template - smithing table (some require more ingots, like pickaxe 3 iron ingots to upgrade from copper to iron,...)
- - upgrade templates must be find in loot chests - reproducable in by crafting - basic upgrade template + 7 gold + iron block

- veinmine too rare in loot chests
- magnet range enchantment - increases range of item pickup
- masterbuilder on bundle can let you by mouswheel click on block (select block from inventory) get swap into hotbar or next empty slot in hotbar

- add wrench item which rotates blocks  named rotator
- - rotates item when rightclicked on block, shift rightclick rotates backwards
- - - alway alongthe axis you clicked on exept clicking on the outer rim of the face (maybe 1px from end) - then it rotates around the axis perpendicular to the face
- - can be enchanted with durability and unbreaking


- Armor trims:
- - add leveling system to amortrims (low level small benifits, high level big benefits)
- - the higher the new experience value better the benifits (multiplyer)
- - calculation - base value * xplevel multipler * experience multipler
- - xplevel multipler: mimimum .10 of full effect at level 1 maximum 1.0 at level 100 (linear calculation)
- - experience multipler: other calculation method - since death distance traveled and mobs killed calculate a value which multiplys the effect
- - - (example: 1000 blocks traveled = 0.1 multiplier, 10000 blocks traveled = 1.0 multiplier, same for mobs killed - 100 mobs = 0.1 multiplier, 1000 mobs = 1.0 multiplier) - the higher value of both calculations is used
- - - no maximum, more like exponential negative curve - the more you do the less you get (example: first 1000 blocks = 0.1 multiplier, next 9000 blocks = 0.9 multiplier, next 90000 blocks = 0.09 multiplier, next 900000 blocks = 0.009 multiplier, ... )




- drawer enchantment allows up to 5 different item types in bundle (instead of only one type)

- funnel 2 allows only items already in bundle to be picked up (no new item types)

## BUGS:
- deep pockets too common in loot chests


### Bugs Later:
- reinforced bundle - should have same ui as normal bundle!
- masterbuilder on building wand and on reinforced bundle not working together properly
- chisel sometimes chisels when not wanted
- compass in smithing table - not working properly

 
## TODO:

- add echoshard as trim material, galaxy/void trim texture

- override higher tier lets sleadgehanmmer also be used as shovel, axe and hoe with biggwer radius (like a multi tool) 


- new item magnet texture -> like tune fork but mor magnet looking
- - rename magnet to attractor
- - durabillity mechanic - loses durrability when picking up items (can't break) - can be fully recharged when rightcliched on lodestone block

- implement echocompass as mirror or simmilar (exactly like eco compas functionality but with these changes)
- - custom mirror enchantment: transdimensional (works in nether and end as well)
- - implement range (range enchantment can increase range from 128 to 512 to 1024 to 2048 blocks)
- - netherite mirror - (range is automatically doubled)
- - durability is 3 but can be restored with mending enchantment (requires a lot of xp to repair) (can't break)
- - can be crafted:
    "AGA",
    "GNG",
    "LGA"
    "G": "minecraft:gold_ingot",
    "A": "ancient_glass",
    "N": "minecraft:nether_star",
    "L": "Blazerod"
- - new dungeon type with custom vaults (contains: ancient glass, basic upgrade templates)



- add enderscape armortrim effect (if mod named enderscape is installed - give armor wich habe applyed enderscape armortrim an affect fitting to the mod )





- End Dimension:

- 2 end materials, both have raroty of netherite or slightly rarer
- - mystic stones and magenta ore naturally generating in end dimension
- - 1. only on the bottom of end terrain
- - 2. only on surface of end terrain
- - upgrade mechanic slighly diffrent from netherite

- - used to craft mystic scrap -> enderite scrap -> enderite ingot -> enderite tools and armor

- - add enderite material (stronger than netherite, but rarer)
- - - upgrade Mechanic for enderite tools
- - - new enderite armor and tools (only craftable via upgrade mechanic)
- - - new enderite block (craftable via 9 enderite ingots)
- - - enderite ingot craftable via smelting enderite scrap
- - - enderite scrap findable in end cities loot chests (rare)
- - new void protection mechanic - protection from void damage (like frost walker for lava)
- - - enderite armor gets void protection effect I per pice -> total IV slower void damage


- Sniffer
- - add use to ancient seeds and flowers and more diggaable flowers for sniffer
- - - catflower - scares creepers away in radius of 8 blocks
- - - dogflower - scares skeletons away in radius of 8 blocks
- - - voidflower - scares endermen away in radius of 8 blocks
- - - breezeflower - scares blazes away in radius of 8 blocks
- - - iceflower - scares witches away in radius of 8 blocks
- - - blazeflower - scares spiders away in radius of 8 blocks
- - when sniffing gives 1-3 of items insted of 1
- - iimplement ancient dye which is crafted from ancient flowers
- - - tourch and other flower get corresponding ancient dyes
- - - new flowers also get it ( cat-,dog-, ... flowers)

- display a charge/fuel/durrabillity gui component

- bundle hotkey (and inside e inventory openable per intuitive methode like clicking with controll on bundle) - opens bundle menu: all bundles in inventory (max 9 displayed in top row), doublechest like layout with inside items and switch able to other bundles to manage easier



- bundle/quiver gui improvements
- - beter tooltips 
- - plus icon like vanilla - green when same item is already in bundle, yellow when different item is in bundle, no plus icon when full, red when item can be added but not completly


- sledgehammer should be customizable in a gui - toggle radius, break trough entchantments

- building wand - all wand enchantments possible on it - mode togglable via gui  
- building wand - master-builder minimal faster placing



## TODO Later:
- reinforced observer - less delay, more range (7 blocks)
- netherite observer - no delay, more range (15 blocks)
- reinforced/netherite dispenser - can use more items (axe, Pickaxe, shovel ...)/ can place blocks
- reinforced/netherite shield - more durability/ axe disable shield protection - no knockback when blocking, no slowdown when blocking
- spear enchantment - less hunger when lungeing - stamina
- colored redstone
- ligtelevel item - visualisiert Lichtlevel in der Welt

- reinforced tnt - bigger explosion, less damage to players
- doubleinforced tnt - even bigger explosion, less damage to players

- later - Negative Beacon (evil Beacon) that gives negative effects to players in range
- later - extra horse inventory if storrage upgrade, for example chest saddle
- later - Reinforced Beacon that gives more effects or longer effects upgrade the whole beacon system
- later - multiple book textures as fix in external texture pack
- later - reinforced shulker - can be enchanted (bigger stack sizes)
- later - netherite shulker double stack size
- ( copy tool? )


- (later) - netherite copper chest - quadrouple stack size
- (later) - reinforced copper chest - double stack size (with diamonds)
-- chest icon no texture but moodel is rendering in hotbar
-- chest is acepting UP TO SPECIFIED STACK SIZE, BUT displays max 99 items in GUI
-- inventory everywhere accepts 99 items, instead of 64 or 16 ...
-- whed middleclicked in creative mode, it gives 1024 items instead of max stack size

## DONE:





## simple mods to add later:

// - simpleindustry
// –– some farming possibillities
// -- new block / transformation mechanic
// -- new material like special fuel or catalysator
// --


## WTCAH 1.21.11 Missing
// Server-Side
// - claimpoints
// - Ping Wheel
// - Open Parties and Claims
// Client-Side
// - // - Xaero's Minimap
// - // - Xaero's World Map
// - Just Enough Professions (JEP)
// - Just Enough Resources (JER)
// - Recolourful Containers Axiom Fix
// - Searchables
// - Proton Shaders
// - Better F3
// - Model Gap Fix
// - BoatView360
// - Fusion Connected Glass
// - Blind
// - Chat Calc
// - Cubes Without Borders
// - Silk
// -
// -




// - dynamic tooltips
// - Cicada
// - Enchantment Insights
// - ViaFabricPlus
// - ModernFix-mVUS
// - Controlify (Controller support)
// - better statistics screen
// - Fusion (Connected Textures)
// - Fusion Connected Blocks
// - Fusion Block Transitions
// - freecam
// - Just Enough Items (jei)
// - no telemetry
// - Better Advancements
// - Immersive Hotbar
// - do a barrel roll
// - ledger
// - controlling
// - voxy
// - itembound: Rebound
// - Fresh Animations
// - Fresh Animations: Objects
// - Fresh Animations: Creepers
// - Fresh Animations: Spiders
// - Fresh Animations: Details
// - Fresh Animations: Quivers
// - Fresh Animations: Emissive
// - Fresh Animations: Classic Horses
// - Fresh Animations: Extentions
// - axiom
// - Puzzles Lib
// - Even Better Enchants
// - Low On Fire
// - [EMF] Entity Model Features
// - [ETF] Entity Texture Features
// - Reese's Sodium Options
// - NoisiumForked
// - BadOptimizations
// - Cull Leaves
// - TCDCommons API
// - My Totem Doll
// - Boat Item View
// - Easy Shulker Boxes
// - ServerCore
// - Recolourful Containers GUI + HUD
// - Recolourful Containers GUI + HUD (DARK)




Name des Updates: "Echoes of the Void" (Echos der Leere)1. Die Materialien (Die Dualitäts-Mechanik)Anders als im Nether, wo man nur ein Material (Antiker Schrott) sucht, verlangt das End die Vereinigung von "Oben" und "Unten". Dies ist die neue Mechanik: Dual-Synthese.A. Material 1: Void-Sediment (Das "Unten")Name: Nihilith (lat. nihil = nichts).Fundort: Generiert nur an der absoluten Unterseite der End-Inseln (Y = 0 bis Y = 20).Gefahr: Um es abzubauen, muss der Spieler riskieren, in die Leere zu fallen. Man muss sich unter die Inseln bauen oder mit Elytren sehr präzise navigieren.Aussehen: Ein tiefschwarzer Stein, der Licht "schluckt" (dunkler als Deepslate), mit feinen Rissen, die die Leere zeigen.Drop: Droppt Nihilith-Splitter.B. Material 2: Astralit (Das "Oben")Name: Astralit.Fundort: Generiert nur offen an der Oberfläche der End-Inseln, aber extrem selten (ähnlich wie Smaragde in Bergen, aber seltener).Enderscape Integration: In Enderscape-Biomen (wie den Celestial Plains) könnte die Rate leicht erhöht sein, um die Erkundung der Mod-Biome zu belohnen.Aussehen: Ein purpur-leuchtendes Erz (Magenta Ore), das wie ein kleiner Sternenhimmel funkelt.Drop: Droppt Astralit-Staub.2. Die Verarbeitung (Der "Enderite"-Prozess)Hier weichen wir leicht von Netherite ab, um Komplexität zu schaffen.Synthese (Crafting):Du benötigst beide Komponenten, um die instabile Materie zu binden.Rezept: 4x Nihilith-Splitter + 4x Astralit-Staub + 1x Enderperle (als Bindemittel in der Mitte) = 1x Rohes Enderite (Raw Enderite).Warum: Das zwingt den Spieler, sowohl die Oberfläche zu erkunden als auch die riskante Unterseite abzubauen.Veredelung (Schmelzen):Rohes Enderite wird im Hochofen (Blast Furnace) geschmolzen $\rightarrow$ Enderite Schrott (Enderite Scrap).Loot-Table Integration: Enderite Schrott kann sehr selten (ca. 2-5% Chance) in End City Truhen gefunden werden.Der Barren (Ingot):Rezept: 4x Enderite Schrott + 4x Netherite Barren? NEIN. Das wäre zu teuer und würde Netherite entwerten.Besseres Rezept: 4x Enderite Schrott + 4x Diamanten (oder Nebulite aus Enderscape, falls Hard-Dependency gewünscht ist, sonst bleib bei Diamanten für Vanilla-Feel) = 1x Enderite Barren.3. Das Upgrade & Die AusrüstungDas Upgrade erfolgt über den Schmiedetisch (Smithing Table), benötigt aber eine neue Schmiedevorlage.Das Item: Enderite-Schmiedevorlage (Void Template).Fundort: Garantiert in der Schatzkiste eines Endschiffs (wo die Elytren sind), oder Drop vom Ender Dragon (wiederbelebt). Duplizierbar mit Diamanten und Endstein.Der Prozess:[Schmiedevorlage] + [Netherite-Rüstung/Werkzeug] + [Enderite Barren] = Enderite-Gegenstand.Die Werte (Balance)Enderite soll sich mächtig anfühlen, aber nicht das Spiel brechen ("Power Creep" vermeiden).Haltbarkeit: +20% gegenüber Netherite.Abbaugeschwindigkeit: Identisch zu Netherite (Netherite ist schon fast "Insta-Mine").Schaden: +1 Angriffsschaden gegenüber Netherite.Feuerresistenz: Behält die Eigenschaft von Netherite (verbrennt nicht in Lava).Despawn: Enderite-Items despawnen nicht, wenn sie in die Leere (Void) geworfen werden. Sie schweben bei Y=0 oder teleportieren sich nach oben auf den nächsten festen Block (ähnlich wie Chorusfrüchte).4. Die neue Mechanik: Void Protection (Leeren-Schutz)Das ist das Kern-Feature ("Selling Point").Funktionsweise:Normalerweise stirbt man im Void (Y < -64) extrem schnell (ca. 4 Schaden pro halbe Sekunde). Rüstung schützt dagegen nicht.Enderite-Rüstung ändert das physikalische Gesetz.Der Effekt: Wenn der Spieler Enderite-Rüstung trägt, wird der Schaden durch die Leere nicht mehr als "Void Damage" (absolut), sondern als regulärer Schaden berechnet, der durch Rüstungswert und Verzauberung (Schutz IV) reduziert werden kann.Der Set-Bonus (Skalierung):1 Teil: Schaden wird um 10% reduziert.2 Teile: Schaden wird um 25% reduziert + Leichter "Slow Falling" Effekt im Void.3 Teile: Schaden wird um 50% reduziert.4 Teile (Full Set): "Void Walker" Status.Du nimmst nur noch alle 2 Sekunden 1 Herz Schaden im Void.Dies gibt dir genug Zeit, um eine Enderperle zu werfen, Raketen mit Elytren zu nutzen oder dich hochzubauen. Es ist keine Unsterblichkeit, aber eine zweite Chance.5. Enderite Block & DekorationCrafting: 9x Enderite Barren.Eigenschaft: Ein Block, der aussieht wie ein Stück des Nachthimmels.Beacon-Basis: Kann für Beacons genutzt werden.Besondere Eigenschaft: Wenn er mit Redstone gepowert wird, verhindert er das Spawnen von Endermans in einem Radius von 64 Blöcken (Enderman-Schutzschild für Basen).Zusammenfassung für die Umsetzung (Roadmap)World Gen (Java/Json):Erstelle Config-Files für die World-Gen.Nihilith: Konfiguriere Ore-Gen als "Upside Down" (nur exposed zur Luft unten).Astralit: Konfiguriere Ore-Gen wie "Emeralds" im End-Biome (Enderscape kompatibel machen durch Tagging der Biome).Item Registrierung:Scrap, Ingot, Template, Raw Item.Mixin / Event Handler (Void Protection):Du musst in den DamageSource Code oder den PlayerEntity Tick-Code eingreifen.Prüfe: if (player.getY() < minimumY && player.hasEnderiteArmor()).Reduziere den Schaden oder negiere ihn periodisch.Loot Tables:In chests/end_city_treasure.json den Enderite Scrap und das Template hinzufügen.Dieses Konzept bewahrt das "Vanilla-Feeling" (Mining, Crafting, Upgrading), fügt aber durch die Positionierung der Erze (Oben/Unten) ein neues Gameplay-Element hinzu, das perfekt zur vertikalen Natur des Ends und Modifikationen wie Enderscape passt.


1. Neue Blöcke:

- purpur/Lapis/Blackstone/resin block with quarz/  - checker block

- polished endstone

- purpur block/ polished endstone (8) with astral_powder/nihil_shard -> new block 8 astral/nihil purpur block and astral/nihil endstone

- sand/gravel (8) + astral_powder -> reverse gravity (upwards)

- sand/gravel (8) + nihilith_shard -> no gravity



2. neue rüstungs materialien:  (nihilit shard, astralit powder, enderite ingot)

auch fähigkeiten mit implementieren und zur rezept seite des smithing tables hinzufügen



3. neue items: enderite bundel, enderite quiver, enderite apple, enderrite carrot - (like netherite variants)!