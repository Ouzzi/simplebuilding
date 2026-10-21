window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simpletools"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simpletools",
    "name": "Simple Tools",
    "version": "0.1.0",
    "minecraftLines": [
      "26.3"
    ],
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ]
  },
  "features": [
    {
      "id": "furnace_lava",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/FurnaceLavaFillHandler.java"
      ],
      "en": {
        "title": "Furnace lava fill",
        "summary": "Either hand inserts one lava bucket into a furnace, blast furnace, or smoker with an empty fuel slot or one leftover empty bucket. Locked furnaces and sneaking are excluded. Survival consumes the lava bucket; an existing empty bucket is returned exactly once. SimpleBuilding furnace subclasses are supported through the Vanilla interface."
      },
      "de": {
        "title": "Ofen mit Lava füllen",
        "summary": "Beide Hände können einen Lavaeimer in Ofen, Hochofen oder Räucherofen mit leerem Brennstoffplatz oder einem leeren Eimer einsetzen. Verschlossene Oefen und Schleichen sind ausgeschlossen. Überleben verbraucht Lava; ein vorhandener Eimer wird genau einmal zurückgegeben. SimpleBuilding-Ofenunterklassen nutzen die Vanilla-Schnittstelle."
      }
    },
    {
      "id": "item_frames",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ItemFrames.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/mixin/ItemFrameInteractMixin.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/mixin/ItemFrameFixedAccessor.java",
        "common/src/shared/java/com/simplebuilding/mixin/ItemFrameEntityMixin.java"
      ],
      "en": {
        "title": "Better item frames",
        "summary": "Item frames and glow item frames, only while sneaking and only with an item in the frame: shears hide or show the frame (Vanilla Invisible flag, 1 durability, Unbreaking counts). If the item is knocked out of a hidden frame, the frame shows again. Honeycomb or a glass pane locks the frame (Vanilla Fixed flag, one honeycomb or pane): the item cannot be turned, taken out or swapped and the frame cannot be broken in survival or fall off. Sneak + axe unlocks it (1 durability), like waxed copper; anyone with build rights may do it, claims and adventure mode are respected. Any other sneaking right-click turns the item backwards. Without sneaking everything stays Vanilla, so a Simple Maps wayfinder in a frame still opens on a plain right-click. Server side only, sounds and wax particles are Vanilla's. With SimpleBuilding installed there is still only this one behaviour: its older frame actions (own glass-pane lock state, sneaking shows a hidden frame) step aside when Simple Tools is loaded, and frames locked by the old state (SimpleBuildingLocked) become Fixed when they load. The old pane drop on unlocking and the sneak-shows rule are gone (shears toggle visibility)."
      },
      "de": {
        "title": "Bessere Rahmen",
        "summary": "Rahmen und Leuchtrahmen, nur schleichend und nur mit Item im Rahmen: Die Schere macht den Rahmen unsichtbar oder wieder sichtbar (Vanilla-Flag Invisible, 1 Haltbarkeit, Haltbarkeit-Verzauberung zählt). Wird das Item aus einem unsichtbaren Rahmen geschlagen, wird der Rahmen wieder sichtbar. Eine Honigwabe oder eine Glasscheibe sperrt den Rahmen (Vanilla-Flag Fixed, eine Wabe bzw. Scheibe): Das Item lässt sich nicht drehen, entnehmen oder tauschen, der Rahmen bricht im Überleben nicht und fällt nicht ab. Schleichen + Axt entsperrt ihn (1 Haltbarkeit), wie gewachstes Kupfer; das darf jeder mit Baurecht, Claims und Abenteuermodus gelten. Jeder andere schleichende Rechtsklick dreht das Item rückwärts. Ohne Schleichen bleibt alles Vanilla, eine Wegfinder-Karte von Simple Maps im Rahmen öffnet sich weiter mit normalem Rechtsklick. Nur serverseitig, Töne und Wachspartikel sind Vanilla. Auch mit SimpleBuilding gibt es nur dieses eine Verhalten: dessen ältere Rahmen-Aktionen (eigener Glasscheiben-Sperrzustand, Schleichen zeigt einen versteckten Rahmen) treten zurück, sobald Simple Tools geladen ist; mit dem alten Zustand (SimpleBuildingLocked) gesperrte Rahmen werden beim Laden zu Fixed. Die zurückgegebene Glasscheibe beim Entsperren und die Regel Schleichen-zeigt entfallen (die Schere schaltet um)."
      }
    },
    {
      "id": "durability",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/mixin/FullDurabilityDamageMixin.java"
      ],
      "en": {
        "title": "Durability bonus",
        "summary": "Damageable held Vanilla and mod tools above the durability threshold can gain attack damage and mining speed. The default multiplier is 1.0 (no bonus), with a server-enforced range of 1–1.5. The threshold defaults to 80% (range 80–100%). Item durability itself is unchanged."
      },
      "de": {
        "title": "Haltbarkeitsbonus",
        "summary": "Gehaltene beschädigbare Vanilla- und Mod-Werkzeuge über der Haltbarkeitsschwelle können mehr Angriffsschaden und Abbaugeschwindigkeit erhalten. Standardfaktor 1.0 (kein Bonus), serverseitiger Bereich 1–1.5. Schwelle standardmäßig 80% (Bereich 80–100%). Die Haltbarkeit selbst bleibt unverändert."
      }
    },
    {
      "id": "config_enableFurnaceLavaFill",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "enableFurnaceLavaFill",
        "summary": "Server setting. Default: true."
      },
      "de": {
        "title": "enableFurnaceLavaFill",
        "summary": "Servereinstellung. Standard: true."
      }
    },
    {
      "id": "config_enableFullDurabilityBonus",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "enableFullDurabilityBonus",
        "summary": "Server setting. Default: true."
      },
      "de": {
        "title": "enableFullDurabilityBonus",
        "summary": "Servereinstellung. Standard: true."
      }
    },
    {
      "id": "config_fullDurabilityThreshold",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "fullDurabilityThreshold",
        "summary": "Server setting. Default: 0.8."
      },
      "de": {
        "title": "fullDurabilityThreshold",
        "summary": "Servereinstellung. Standard: 0.8."
      }
    },
    {
      "id": "config_fullDurabilityBonusMultiplier",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "fullDurabilityBonusMultiplier",
        "summary": "Server setting. Default: 1.0 (no bonus). Range: 1.0–1.5. Saved 1.5 remains 1.5; absent keys use 1.0. Nonfinite values fall back to 1.0; finite out-of-range values are clamped."
      },
      "de": {
        "title": "fullDurabilityBonusMultiplier",
        "summary": "Servereinstellung. Standard: 1.0 (kein Bonus). Bereich: 1.0–1.5. Gespeicherte 1.5 bleiben 1.5; fehlende Schlüssel nutzen 1.0. Nicht endliche Werte fallen auf 1.0 zurück; endliche Werte ausserhalb des Bereichs werden begrenzt."
      }
    },
    {
      "id": "config_anvilRepairKeepsCost",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/mixin/AnvilRepairCostMixin.java"
      ],
      "en": {
        "title": "anvilRepairKeepsCost",
        "summary": "Server setting. Default: true. Repairing with material, combining two items without new enchantments or renaming keeps the higher anvil cost of the inputs instead of raising it; only a change of enchantments raises it like vanilla."
      },
      "de": {
        "title": "anvilRepairKeepsCost",
        "summary": "Servereinstellung. Standard: true. Reparieren mit Material, Zusammenlegen zweier Gegenstände ohne neue Verzauberung oder Umbenennen behält die höhere Ambosskosten der Zutaten, statt sie zu erhöhen; nur eine Änderung der Verzauberungen erhöht sie wie in Vanilla."
      }
    },
    {
      "id": "config_enableItemFrameInvisibility",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "enableItemFrameInvisibility",
        "summary": "Sneak + shears hide or show a filled item frame. Server setting. Default: true."
      },
      "de": {
        "title": "enableItemFrameInvisibility",
        "summary": "Schleichen + Schere versteckt einen vollen Rahmen oder zeigt ihn. Servereinstellung. Standard: true."
      }
    },
    {
      "id": "config_enableItemFrameLock",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "enableItemFrameLock",
        "summary": "Sneak + honeycomb locks a filled item frame, sneak + axe unlocks it. Server setting. Default: true."
      },
      "de": {
        "title": "enableItemFrameLock",
        "summary": "Schleichen + Honigwabe sperrt einen vollen Rahmen, Schleichen + Axt entsperrt ihn. Servereinstellung. Standard: true."
      }
    },
    {
      "id": "config_enableItemFrameReverseRotate",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "enableItemFrameReverseRotate",
        "summary": "Sneak + right-click turns a framed item backwards. Server setting. Default: true."
      },
      "de": {
        "title": "enableItemFrameReverseRotate",
        "summary": "Schleichen + Rechtsklick dreht ein Rahmen-Item rückwärts. Servereinstellung. Standard: true."
      }
    },
    {
      "id": "biome_brush",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/brush/BiomeBrushItem.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/brush/BiomeBrush.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsRegistry.java",
        "modules/simpletools/shared/resources/data/simpletools/recipe/biome_brush.json",
        "modules/simpletools/shared/resources/data/simpletools/recipe/biome_brush_nether_heart.json"
      ],
      "en": {
        "title": "Biome Brush",
        "summary": "Brush in the main hand, a biome material in the off hand, right-click a block: the biome around it becomes the material's biome. One material and one durability point per stroke.",
        "details": [
          "Recipe (shaped): three Nether Stars in the top row, a Netherite Nugget in the middle, a Brush below. With Simple Mobs instead (one column): Nether Heart, Netherite Nugget, Brush. Durability 512, repaired with Netherite Ingots, takes Unbreaking and Mending. Moved from Simple Mobs (21.10.); old brushes keep working.",
          "A stroke sets the 4x4x4 biome cells (Vanilla stores biomes in such cells) around the clicked block: 3x3x3 cells, 12 blocks wide (server config biomeBrushRadius, 0 to 2 cells). The chunks are sent again at once, so grass, leaf and water colors, weather, mob spawns and plant growth follow the new biome. Holding the button keeps painting, one stroke per 5 ticks. Creative uses no material and no durability.",
          "Sneaking paints only the clicked cell (one 4x4x4 cell, the smallest spot a biome can have) - for borders and details.",
          "Materials: Grass Block = Plains, Sand = Desert, Snow Block = Snowy Plains, Mycelium = Mushroom Fields, Cherry Leaves = Cherry Grove, Jungle Log = Jungle, Red Sand = Badlands, Podzol = Taiga, Mangrove Roots = Mangrove Swamp, Pale Moss Block = Pale Garden, Oak Log = Forest, Birch Log = Birch Forest, Dark Oak Log = Dark Forest, Acacia Log = Savanna, Lily Pad = Swamp, Packed Ice = Ice Spikes; Nether: Soul Sand or Soul Soil = Soul Sand Valley, Crimson Nylium = Crimson Forest, Warped Nylium = Warped Forest, Basalt = Basalt Deltas, Netherrack = Nether Wastes; End: Chorus Flower = End Highlands, End Stone = End Barrens. Data packs add more in data/<namespace>/biome_brush/*.json.",
          "Limits: biomes only in their own dimension (Overworld biomes in the Overworld ...). Cave biomes (Deep Dark, Dripstone and Lush Caves) and structures are never painted over.",
          "Multiplayer-safe: every cell is checked like a block edit - claims (Simple Tweaks or any mod using the shared protection check), spawn protection and adventure mode. Refused cells keep their biome; a refused clicked cell refuses the stroke and uses nothing (a puff of smoke). Server config: biomeBrushEnabled, biomeBrushOpsOnly, biomeBrushRadius."
        ]
      },
      "de": {
        "title": "Biom-Pinsel",
        "summary": "Pinsel in der Haupthand, ein Biom-Material in der Nebenhand, Rechtsklick auf einen Block: Das Biom ringsum wird zum Biom des Materials. Ein Material und ein Haltbarkeitspunkt je Strich.",
        "details": [
          "Rezept (geformt): drei Nethersterne in der oberen Reihe, ein Netheritnugget in der Mitte, ein Pinsel darunter. Mit Simple Mobs stattdessen (eine Spalte): Netherherz, Netheritnugget, Pinsel. Haltbarkeit 512, Reparatur mit Netheritbarren, nimmt Haltbarkeit und Reparatur an. Aus Simple Mobs umgezogen (21.10.); alte Pinsel bleiben gültig.",
          "Ein Strich setzt die 4×4×4-Biomzellen (so speichert Vanilla Biome) um den geklickten Block: 3×3×3 Zellen, 12 Blöcke breit (Server-Config biomeBrushRadius, 0 bis 2 Zellen). Die Chunks werden sofort neu gesendet, Gras-, Laub- und Wasserfarbe, Wetter, Mob-Spawns und Pflanzenwachstum folgen dem neuen Biom. Gedrückt halten malt weiter, ein Strich je 5 Ticks. Kreativ verbraucht weder Material noch Haltbarkeit.",
          "Schleichen malt nur die geklickte Zelle (eine 4×4×4-Zelle, die kleinste Stelle, die ein Biom haben kann) – für Ränder und Details.",
          "Materialien: Grasblock = Ebene, Sand = Wüste, Schneeblock = Verschneite Ebene, Myzel = Pilzinsel, Kirschlaub = Kirschhain, Tropenbaumstamm = Dschungel, Roter Sand = Tafelberge, Podsol = Taiga, Mangrovenwurzeln = Mangrovensumpf, Blassmoosblock = Blasser Garten, Eichenstamm = Wald, Birkenstamm = Birkenwald, Schwarzeichenstamm = Dichter Wald, Akazienstamm = Savanne, Seerosenblatt = Sumpf, Packeis = Eiszapfentundra; Nether: Seelensand oder Seelenerde = Seelensandtal, Karmesin-Nyzel = Karmesinwald, Wirr-Nyzel = Wirrwald, Basalt = Basaltdeltas, Netherrack = Netherödland; End: Chorusblüte = End-Hochland, Endstein = Kargland des Ends. Datenpakete ergänzen weitere unter data/<namespace>/biome_brush/*.json.",
          "Grenzen: Biome nur in ihrer eigenen Dimension (Oberwelt-Biome in der Oberwelt …). Höhlenbiome (Tiefes Dunkel, Tropfsteinhöhlen, Üppige Höhlen) und Bauwerke werden nie übermalt.",
          "Mehrspielersicher: Jede Zelle wird wie ein Blockeingriff geprüft - Claims (Simple Tweaks oder jede Mod mit der gemeinsamen Schutzprüfung), Spawnschutz und Abenteuermodus. Verweigerte Zellen behalten ihr Biom; ist die geklickte Zelle verweigert, wird nichts gemalt und nichts verbraucht (ein Rauchwölkchen). Server-Config: biomeBrushEnabled, biomeBrushOpsOnly, biomeBrushRadius."
        ]
      }
    },
    {
      "id": "config_biomeBrushEnabled",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "biomeBrushEnabled",
        "summary": "Server setting. Default: true. Off: every Biome Brush stroke is refused."
      },
      "de": {
        "title": "biomeBrushEnabled",
        "summary": "Servereinstellung. Standard: true. Aus: jeder Strich des Biom-Pinsels wird abgelehnt."
      }
    },
    {
      "id": "config_biomeBrushOpsOnly",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "biomeBrushOpsOnly",
        "summary": "Server setting. Default: false. Only operators may paint biomes."
      },
      "de": {
        "title": "biomeBrushOpsOnly",
        "summary": "Servereinstellung. Standard: false. Nur Operatoren dürfen Biome malen."
      }
    },
    {
      "id": "config_biomeBrushRadius",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "biomeBrushRadius",
        "summary": "Server setting. Default: 1 (0-2). Biome cells (4 blocks each) painted around the clicked cell."
      },
      "de": {
        "title": "biomeBrushRadius",
        "summary": "Servereinstellung. Standard: 1 (0-2). Biomzellen (je 4 Blöcke) um die geklickte Zelle."
      }
    },
    {
      "id": "thrift",
      "sources": [
        "modules/simpletools/shared/resources/data/simplequalityoflife/enchantment/thrift.json",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/mixin/ThriftDurabilityMixin.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/Thrift.java"
      ],
      "en": {
        "title": "Thrift",
        "summary": "Enchantment for every item with durability, levels I-III: +1/3 maximum durability per level, level III doubles it. Not combinable with Mending; Unbreaking still works. Anvil repair with material restores a quarter of the (larger) maximum per item, so the same number of items as without the enchantment repairs it fully. Enchanting table, trades and loot like other non-treasure enchantments. The id stays simplequalityoflife:thrift, so enchanted items from older worlds keep it; it also works with the sub-mod switched off."
      },
      "de": {
        "title": "Sparsamkeit",
        "summary": "Verzauberung für alles mit Haltbarkeit, Stufen I-III: +1/3 maximale Haltbarkeit je Stufe, Stufe III verdoppelt sie. Nicht mit Reparatur kombinierbar; Haltbarkeit (Unbreaking) wirkt weiter. Die Amboss-Reparatur mit Material gibt je Stück ein Viertel des (größeren) Maximums zurück, also repariert dieselbe Anzahl wie ohne Verzauberung vollständig. Verzauberungstisch, Handel und Beute wie andere Nicht-Schatz-Verzauberungen. Die ID bleibt simplequalityoflife:thrift, damit verzauberte Gegenstände aus älteren Welten sie behalten; sie wirkt auch bei abgeschalteter Sub-Mod."
      }
    },
    {
      "id": "anvil_repair_cost",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/mixin/AnvilRepairCostMixin.java"
      ],
      "en": {
        "title": "Repairing keeps the anvil cost",
        "summary": "Repairing with material, combining two items without new enchantments or renaming keeps the higher anvil cost of the inputs instead of raising it. Only a change of enchantments raises it like vanilla. Server option anvilRepairKeepsCost (default true)."
      },
      "de": {
        "title": "Reparieren hält die Ambosskosten",
        "summary": "Reparieren mit Material, Zusammenlegen zweier Gegenstände ohne neue Verzauberung oder Umbenennen behält die höhere Ambosskosten der Zutaten, statt sie zu erhöhen. Nur eine Änderung der Verzauberungen erhöht sie wie in Vanilla. Server-Option anvilRepairKeepsCost (Standard true)."
      }
    },
    {
      "id": "commands",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsCommands.java"
      ],
      "en": {
        "title": "Commands",
        "summary": "Owner-level /simpletools fullDurabilityBonus, anvilRepairKeepsCost, furnaceLava, itemFrameInvisible, itemFrameLock and itemFrameReverseRotate <true|false>; changes are saved to config/simpletools.json."
      },
      "de": {
        "title": "Befehle",
        "summary": "Besitzerberechtigung: /simpletools fullDurabilityBonus, anvilRepairKeepsCost, furnaceLava, itemFrameInvisible, itemFrameLock und itemFrameReverseRotate <true|false>; Speichern in config/simpletools.json."
      }
    },
    {
      "id": "submod",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/SimpleTools.java",
        "framework/src/main/java/com/simplebuilding/framework/api/SubMods.java"
      ],
      "en": {
        "title": "Sub-mod of Simple QoL",
        "summary": "Playable alone (config/simpletools.json). Bundled in Simple QoL, which can switch it off with \"Enable Simple Tools\". On the first start the old Simple QoL settings of this sub-mod are taken over once."
      },
      "de": {
        "title": "Sub-Mod von Simple QoL",
        "summary": "Allein spielbar (config/simpletools.json). In Simple QoL gebuendelt, dort mit \"Simple Tools aktivieren\" abschaltbar. Beim ersten Start werden die alten Simple-QoL-Einstellungen dieser Sub-Mod einmalig übernommen."
      }
    },
    {
      "id": "soulbound",
      "sources": [
        "modules/simpletools/shared/resources/data/simpletools/enchantment/soulbound.json",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/soulbound/Soulbound.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/mixin/InventoryDropAllMixin.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/mixin/ServerPlayerRestoreMixin.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/mixin/AnvilSoulboundMixin.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/mixin/WanderingTraderSoulboundMixin.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsLoot.java"
      ],
      "en": {
        "title": "Soulbound",
        "summary": "Treasure enchantment, one level, for everything that can carry the Curse of Vanishing (the two exclude each other). An item with Soulbound never drops on death - also in PvP, but not in Hardcore, where it has no effect (like a bed or respawn anchor; items can still be enchanted and say so in the tooltip) - and is back in the same slot after the respawn, with at most 50 durability left (min(current, 50)). Book only: never from the enchanting table, librarians or random loot; a wandering trader sells one book for 24-32 diamonds now and then (5 % per trader), and End City treasure chests hold one very rarely (2 %). Rarer than Mending. Applying it at the anvil always costs the full 35 levels; the grindstone removes it."
      },
      "de": {
        "title": "Seelengebunden",
        "summary": "Schatz-Verzauberung, eine Stufe, für alles, was den Fluch des Verschwindens tragen kann (beide schließen sich aus). Ein Gegenstand mit Seelengebunden fällt beim Tod nie - auch im PvP, nicht aber im Hardcore-Modus, wo es wirkungslos ist (wie Bett und Respawn-Anker; Gegenstände bleiben verzauberbar und sagen es im Tooltip) - und liegt nach dem Wiedereinstieg im selben Platz, mit höchstens 50 Haltbarkeit (min(aktuell, 50)). Nur als Buch: nie am Verzauberungstisch, beim Bibliothekar oder als Zufallsbeute; ein fahrender Händler verkauft ab und zu ein Buch für 24-32 Diamanten (5 % je Händler), Endsiedlungs-Schatztruhen enthalten sehr selten eines (2 %). Seltener als Reparatur. Am Amboss kostet das Auftragen immer volle 35 Level; der Schleifstein entfernt es."
      }
    },
    {
      "id": "death_vault",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/vault/DeathVault.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/vault/DeathLedger.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/vault/DeathVaultMenu.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/vault/RemovalContext.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/vault/Fate.java",
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/client/DeathVaultScreen.java",
        "modules/simpletools/shared/resources/data/simpletools/recipe/death_vault.json",
        "modules/simpletools/shared/resources/data/simpletools/tags/item/death_vault_offerings.json"
      ],
      "en": {
        "title": "Death Vault",
        "summary": "Crafted block (obsidian, eye of ender, soul sand or soul soil, chest). It shows what became of each item dropped at your last death, color coded with a legend: still lying (green), picked up by you (blue) or by another player (red, the name shows on hover), burned in lava, fire or by a cactus (orange), despawned (gray), fallen into the void (purple), gone another way (yellow). A lying item has a bar like a durability bar: how much of its five minutes is left. If its chunk is not loaded the timer stands and the field shows a pause mark. Buying lost items back with an offering is a future feature: the code stays, option deathVaultRedeem is off by default."
      },
      "de": {
        "title": "Todesgewölbe",
        "summary": "Herstellbarer Block (Obsidian, Enderauge, Seelensand oder Seelenerde, Truhe). Er zeigt, was aus jedem Gegenstand deines letzten Todes wurde, farbcodiert mit Legende: liegt noch (grün), von dir (blau) oder einem anderen Spieler (rot, der Name steht im Tooltip) aufgehoben, in Lava, Feuer oder durch einen Kaktus verbrannt (orange), verschwunden (grau), ins Leere gefallen (lila), anders weg (gelb). Ein liegender Gegenstand hat einen Balken wie eine Haltbarkeitsleiste: wie viel von seinen fünf Minuten übrig ist. Ist sein Chunk nicht geladen, steht der Timer und das Feld zeigt ein Pausen-Zeichen. Rückkauf verlorener Gegenstände gegen ein Opfer ist eine zukünftige Funktion: der Code bleibt, die Option deathVaultRedeem ist standardmäßig aus."
      }
    },
    {
      "id": "death_vault_money",
      "sources": [
        "modules/simpletools/shared/resources/data/simpletools/tags/item/death_vault_offerings.json"
      ],
      "en": {
        "title": "(with Simple Money) Money bills as offering",
        "summary": "When Simple Money is installed, its money bills count like diamonds in the Death Vault offering slot (optional tag entry, nothing changes without the mod)."
      },
      "de": {
        "title": "(mit Simple Money) Geldscheine als Opfer",
        "summary": "Ist Simple Money installiert, zählen seine Geldscheine im Opferplatz des Todesgewölbes wie Diamanten (optionaler Tag-Eintrag, ohne die Mod ändert sich nichts)."
      }
    },
    {
      "id": "config_enableSoulbound",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "enableSoulbound",
        "summary": "Server setting. Default: true. Soulbound keeps items on death; off: they drop like any item."
      },
      "de": {
        "title": "enableSoulbound",
        "summary": "Servereinstellung. Standard: true. Seelengebunden behält Gegenstände beim Tod; aus: sie fallen wie alle anderen."
      }
    },
    {
      "id": "config_enableDeathVault",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "enableDeathVault",
        "summary": "Server setting. Default: true. Records the drops of each death and lets the Death Vault show what became of them; off: nothing is recorded and the vault does not open."
      },
      "de": {
        "title": "enableDeathVault",
        "summary": "Servereinstellung. Standard: true. Merkt sich die Drops jedes Todes und lässt das Todesgewölbe zeigen, was daraus wurde; aus: nichts wird gemerkt und das Gewölbe öffnet nicht."
      }
    },
    {
      "id": "config_deathDurabilityCap",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "deathDurabilityCap",
        "summary": "Server setting. Default: 50 (1-2031). Soulbound items after a death and Death Vault returns keep at most this much durability."
      },
      "de": {
        "title": "deathDurabilityCap",
        "summary": "Servereinstellung. Standard: 50 (1-2031). Seelengebundene Gegenstände nach dem Tod und Rückgaben des Todesgewölbes behalten höchstens so viel Haltbarkeit."
      }
    },
    {
      "id": "config_soulboundAnvilCost",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "soulboundAnvilCost",
        "summary": "Server setting. Default: 35 (1-39). Levels for every anvil combination that applies Soulbound."
      },
      "de": {
        "title": "soulboundAnvilCost",
        "summary": "Servereinstellung. Standard: 35 (1-39). Level für jede Amboss-Kombination, die Seelengebunden aufträgt."
      }
    },
    {
      "id": "config_soulboundTraderChance",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "soulboundTraderChance",
        "summary": "Server setting. Default: 0.05 (0-1). Chance that a wandering trader also sells one Soulbound book for 24-32 diamonds."
      },
      "de": {
        "title": "soulboundTraderChance",
        "summary": "Servereinstellung. Standard: 0.05 (0-1). Chance, dass ein fahrender Händler zusätzlich ein Seelengebunden-Buch für 24-32 Diamanten verkauft."
      }
    },
    {
      "id": "config_soulboundEndCityChance",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "soulboundEndCityChance",
        "summary": "Server setting. Default: 0.02 (0-1). Chance of a Soulbound book in an End City treasure chest (applied when the data reloads)."
      },
      "de": {
        "title": "soulboundEndCityChance",
        "summary": "Servereinstellung. Standard: 0.02 (0-1). Chance auf ein Seelengebunden-Buch in einer Endsiedlungs-Schatztruhe (wirkt nach dem Neuladen der Daten)."
      }
    },
    {
      "id": "config_deathVaultCostPerItem",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "deathVaultCostPerItem",
        "summary": "Server setting. Default: 4 (1-64). Future buy-back (deathVaultRedeem): offering points (diamonds, money bills) per returned item."
      },
      "de": {
        "title": "deathVaultCostPerItem",
        "summary": "Servereinstellung. Standard: 4 (1-64). Zukünftiger Rückkauf (deathVaultRedeem): Opferpunkte (Diamanten, Geldscheine) je zurückgegebenem Gegenstand."
      }
    },
    {
      "id": "config_deathVaultMaxPerOffering",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "deathVaultMaxPerOffering",
        "summary": "Server setting. Default: 3 (1-27). Future buy-back (deathVaultRedeem): items one offering returns at most."
      },
      "de": {
        "title": "deathVaultMaxPerOffering",
        "summary": "Servereinstellung. Standard: 3 (1-27). Zukünftiger Rückkauf (deathVaultRedeem): so viele Gegenstände gibt ein Opfer höchstens zurück."
      }
    },
    {
      "id": "config_deathVaultRedeem",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "deathVaultRedeem",
        "summary": "Server setting. Default: false (off). Future feature: buy lost death drops back with an offering at the Death Vault. The code stays, only the switch is off; also /simpletools deathVaultRedeem <true|false>."
      },
      "de": {
        "title": "deathVaultRedeem",
        "summary": "Servereinstellung. Standard: false (aus). Zukünftige Funktion: verlorene Todes-Drops mit einem Opfer am Todesgewölbe zurückkaufen. Der Code bleibt, nur der Schalter ist aus; auch /simpletools deathVaultRedeem <true|false>."
      }
    },
    {
      "id": "config_deathVaultMaxDeaths",
      "sources": [
        "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/ToolsConfig.java"
      ],
      "en": {
        "title": "deathVaultMaxDeaths",
        "summary": "Server setting. Default: 20 (1-100). Future buy-back (deathVaultRedeem): deaths remembered per player, older ones are forgotten; without it only the last death is kept."
      },
      "de": {
        "title": "deathVaultMaxDeaths",
        "summary": "Servereinstellung. Standard: 20 (1-100). Zukünftiger Rückkauf (deathVaultRedeem): gemerkte Tode je Spieler, ältere werden vergessen; ohne ihn wird nur der letzte Tod gemerkt."
      }
    }
  ],
  "recipes": [
    {
      "id": "simpletools:biome_brush",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": null,
      "result": {
        "id": "simpletools:biome_brush",
        "count": 1
      },
      "source": "modules/simpletools/shared/resources/data/simpletools/recipe/biome_brush.json",
      "ingredients": [
        "#c:nuggets/netherite",
        "minecraft:brush",
        "minecraft:nether_star"
      ],
      "pattern": [
        "SSS",
        " N ",
        " B "
      ],
      "key": {
        "S": [
          "minecraft:nether_star"
        ],
        "N": [
          "#c:nuggets/netherite"
        ],
        "B": [
          "minecraft:brush"
        ]
      }
    },
    {
      "id": "simpletools:biome_brush_nether_heart",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": null,
      "result": {
        "id": "simpletools:biome_brush",
        "count": 1
      },
      "source": "modules/simpletools/shared/resources/data/simpletools/recipe/biome_brush_nether_heart.json",
      "ingredients": [
        "#c:nuggets/netherite",
        "minecraft:brush",
        "simplemobs:nether_heart"
      ],
      "pattern": [
        " H ",
        " N ",
        " B "
      ],
      "key": {
        "H": [
          "simplemobs:nether_heart"
        ],
        "N": [
          "#c:nuggets/netherite"
        ],
        "B": [
          "minecraft:brush"
        ]
      }
    },
    {
      "id": "simpletools:death_vault",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpletools:death_vault",
        "count": 1
      },
      "source": "modules/simpletools/shared/resources/data/simpletools/recipe/death_vault.json",
      "ingredients": [
        "#minecraft:soul_fire_base_blocks",
        "minecraft:chest",
        "minecraft:ender_eye",
        "minecraft:obsidian"
      ],
      "pattern": [
        "OEO",
        "SCS",
        "OOO"
      ],
      "key": {
        "O": [
          "minecraft:obsidian"
        ],
        "E": [
          "minecraft:ender_eye"
        ],
        "S": [
          "#minecraft:soul_fire_base_blocks"
        ],
        "C": [
          "minecraft:chest"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:obsidian",
            "count": 5
          },
          {
            "id": "minecraft:oak_log",
            "count": 2
          },
          {
            "id": "minecraft:soul_sand",
            "count": 2
          },
          {
            "id": "minecraft:ender_pearl",
            "count": 1
          },
          {
            "id": "minecraft:blaze_rod",
            "count": 0.5
          }
        ],
        "tagExamples": [
          "#minecraft:soul_fire_base_blocks"
        ]
      }
    },
    {
      "id": "simpletools:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpletools:guide_book",
        "count": 1
      },
      "source": "modules/simpletools/shared/resources/data/simpletools/recipe/guide_book.json",
      "ingredients": [
        "minecraft:anvil",
        "minecraft:book"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:anvil"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:iron_ingot",
            "count": 31
          },
          {
            "id": "minecraft:sugar_cane",
            "count": 3
          },
          {
            "id": "minecraft:leather",
            "count": 1
          }
        ]
      }
    }
  ],
  "lootTables": [
    {
      "id": "simpletools:blocks/death_vault",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpletools:death_vault"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpletools/shared/resources/data/simpletools/loot_table/blocks/death_vault.json"
    }
  ],
  "tags": [
    {
      "id": "simpletools:enchantment/exclusive_set/soulbound",
      "replace": false,
      "values": [
        {
          "id": "minecraft:vanishing_curse",
          "required": true
        }
      ],
      "source": "modules/simpletools/shared/resources/data/simpletools/tags/enchantment/exclusive_set/soulbound.json"
    },
    {
      "id": "simpletools:item/death_vault_offerings",
      "replace": false,
      "values": [
        {
          "id": "minecraft:diamond",
          "required": true
        },
        {
          "id": "simplemoney:money_bill",
          "required": false
        }
      ],
      "source": "modules/simpletools/shared/resources/data/simpletools/tags/item/death_vault_offerings.json"
    },
    {
      "id": "simpletools:worldgen/biome/biome_brush_locked",
      "replace": false,
      "values": [
        {
          "id": "minecraft:deep_dark",
          "required": true
        },
        {
          "id": "minecraft:dripstone_caves",
          "required": true
        },
        {
          "id": "minecraft:lush_caves",
          "required": true
        },
        {
          "id": "minecraft:sulfur_caves",
          "required": false
        }
      ],
      "source": "modules/simpletools/shared/resources/data/simpletools/tags/worldgen/biome/biome_brush_locked.json"
    }
  ],
  "advancements": [],
  "enchantments": [
    {
      "id": "simpletools:soulbound",
      "name": {
        "en_us": "Soulbound",
        "de_de": "Seelengebunden"
      },
      "description": {
        "en_us": ""
      },
      "maxLevel": 1,
      "weight": 1,
      "anvilCost": 8,
      "slots": [
        "any"
      ],
      "supportedItems": "#minecraft:enchantable/vanishing",
      "primaryItems": null,
      "exclusiveSet": "#simpletools:exclusive_set/soulbound",
      "effects": [],
      "implementedIn": "none",
      "hasEffect": false,
      "source": "modules/simpletools/shared/resources/data/simpletools/enchantment/soulbound.json",
      "note": {
        "en": {
          "title": "Soulbound",
          "summary": "Treasure enchantment, one level, for everything that can carry the Curse of Vanishing (the two exclude each other). An item with Soulbound never drops on death - also in PvP, but not in Hardcore, where it has no effect (like a bed or respawn anchor; items can still be enchanted and say so in the tooltip) - and is back in the same slot after the respawn, with at most 50 durability left (min(current, 50)). Book only: never from the enchanting table, librarians or random loot; a wandering trader sells one book for 24-32 diamonds now and then (5 % per trader), and End City treasure chests hold one very rarely (2 %). Rarer than Mending. Applying it at the anvil always costs the full 35 levels; the grindstone removes it."
        },
        "de": {
          "title": "Seelengebunden",
          "summary": "Schatz-Verzauberung, eine Stufe, für alles, was den Fluch des Verschwindens tragen kann (beide schließen sich aus). Ein Gegenstand mit Seelengebunden fällt beim Tod nie - auch im PvP, nicht aber im Hardcore-Modus, wo es wirkungslos ist (wie Bett und Respawn-Anker; Gegenstände bleiben verzauberbar und sagen es im Tooltip) - und liegt nach dem Wiedereinstieg im selben Platz, mit höchstens 50 Haltbarkeit (min(aktuell, 50)). Nur als Buch: nie am Verzauberungstisch, beim Bibliothekar oder als Zufallsbeute; ein fahrender Händler verkauft ab und zu ein Buch für 24-32 Diamanten (5 % je Händler), Endsiedlungs-Schatztruhen enthalten sehr selten eines (2 %). Seltener als Reparatur. Am Amboss kostet das Auftragen immer volle 35 Level; der Schleifstein entfernt es."
        }
      }
    }
  ],
  "items": [
    {
      "id": "simpletools:biome_brush",
      "name": {
        "en_us": "Biome Brush",
        "de_de": "Biom-Pinsel"
      },
      "note": {
        "en": {
          "summary": "Main hand: brush, off hand: biome material. Right-click paints the biome around the block (3x3x3 cells), one material and one durability point per stroke."
        },
        "de": {
          "summary": "Haupthand: Pinsel, Nebenhand: Biom-Material. Rechtsklick malt das Biom um den Block (3×3×3 Zellen), ein Material und ein Haltbarkeitspunkt je Strich."
        }
      },
      "texture": "assets/textures/simpletools/item/biome_brush.png",
      "questionary": {
        "en": {
          "lore": [
            "Paint the land itself. The heart in the ferrule remembers every place it has seen."
          ],
          "use": [
            "Hold it in the main hand and a biome material in the off hand, then right-click a block: sand paints desert, snow paints snowy plains, soul sand paints a soul sand valley. One material and one durability point per stroke."
          ],
          "secret": [
            "Biomes stay in their dimension, and claimed land or caves cannot be painted. Unbreaking and Mending work on it."
          ]
        },
        "de": {
          "lore": [
            "Male das Land selbst an. Das Herz in der Zwinge erinnert sich an jeden Ort, den es gesehen hat."
          ],
          "use": [
            "Halte ihn in der Haupthand und ein Biom-Material in der Nebenhand, dann Rechtsklick auf einen Block: Sand malt Wüste, Schnee verschneite Ebene, Seelensand ein Seelensandtal. Ein Material und ein Haltbarkeitspunkt je Strich."
          ],
          "secret": [
            "Biome bleiben in ihrer Dimension, geschütztes Land und Höhlen lassen sich nicht übermalen. Haltbarkeit und Reparatur wirken auf ihn."
          ]
        }
      },
      "craftedBy": [
        "simpletools:biome_brush",
        "simpletools:biome_brush_nether_heart"
      ],
      "usedIn": []
    },
    {
      "id": "simpletools:guide_book",
      "name": {
        "en_us": "Simple Tools Guide",
        "de_de": "Simple-Tools-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simpletools/shared/java/com/simplebuilding/modules/simpletools/guide/ToolsGuide.java",
          "modules/simpletools/shared/resources/data/simpletools/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 34 pages taken from this wiki, shown in your language. Shapeless recipe: book + anvil. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Tools\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 34 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Amboss. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Tools\" eins."
        }
      },
      "texture": "assets/textures/simpletools/item/guide_book.png",
      "questionary": {
        "en": {
          "lore": [
            "A tradesman's manual for gear that should outlast its owner."
          ],
          "use": [
            "The Simple Tools Guide explains the lava-filled furnace, durability bonuses, Soulbound and the Death Vault. Craft it shapeless from a book and an anvil."
          ],
          "secret": [
            "Its pages open in your language and nothing is consumed."
          ]
        },
        "de": {
          "lore": [
            "Ein Handwerkerbuch für Ausrüstung, die ihren Besitzer überleben soll."
          ],
          "use": [
            "Das Simple-Tools-Handbuch erklärt den lavagefüllten Ofen, Haltbarkeitsboni, Seelenbindung und den Todesvorrat. Herstellung formlos aus einem Buch und einem Amboss."
          ],
          "secret": [
            "Seine Seiten öffnen sich in deiner Sprache, und nichts wird verbraucht."
          ]
        }
      },
      "craftedBy": [
        "simpletools:guide_book"
      ],
      "usedIn": []
    }
  ],
  "blocks": [
    {
      "id": "simpletools:death_vault",
      "name": {
        "en_us": "Death Vault",
        "de_de": "Todesgewölbe"
      },
      "note": {
        "en": {
          "title": "Death Vault",
          "summary": "Crafted block (obsidian, eye of ender, soul sand or soul soil, chest). It shows what became of each item dropped at your last death, color coded with a legend: still lying (green), picked up by you (blue) or by another player (red, the name shows on hover), burned in lava, fire or by a cactus (orange), despawned (gray), fallen into the void (purple), gone another way (yellow). A lying item has a bar like a durability bar: how much of its five minutes is left. If its chunk is not loaded the timer stands and the field shows a pause mark. Buying lost items back with an offering is a future feature: the code stays, option deathVaultRedeem is off by default."
        },
        "de": {
          "title": "Todesgewölbe",
          "summary": "Herstellbarer Block (Obsidian, Enderauge, Seelensand oder Seelenerde, Truhe). Er zeigt, was aus jedem Gegenstand deines letzten Todes wurde, farbcodiert mit Legende: liegt noch (grün), von dir (blau) oder einem anderen Spieler (rot, der Name steht im Tooltip) aufgehoben, in Lava, Feuer oder durch einen Kaktus verbrannt (orange), verschwunden (grau), ins Leere gefallen (lila), anders weg (gelb). Ein liegender Gegenstand hat einen Balken wie eine Haltbarkeitsleiste: wie viel von seinen fünf Minuten übrig ist. Ist sein Chunk nicht geladen, steht der Timer und das Feld zeigt ein Pausen-Zeichen. Rückkauf verlorener Gegenstände gegen ein Opfer ist eine zukünftige Funktion: der Code bleibt, die Option deathVaultRedeem ist standardmäßig aus."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "Some say the dead keep a ledger. This shrine is where it is kept, and its soul flame never forgets a name."
          ],
          "use": [
            "Open it to see where the items of your last death went: still lying (with a despawn bar), picked up by you or someone else, burned, despawned, fallen into the void or gone another way - each in its own color, with a legend beside the list."
          ],
          "secret": [
            "A paused bar means the chunk is not loaded and the timer stands. Soulbound items never appear: they never left you."
          ]
        },
        "de": {
          "lore": [
            "Manche sagen, die Toten führen Buch. Dieser Schrein ist der Ort, an dem es liegt, und seine Seelenflamme vergisst keinen Namen."
          ],
          "use": [
            "Öffne ihn, um zu sehen, wohin die Gegenstände deines letzten Todes gekommen sind: liegt noch (mit Despawn-Balken), von dir oder jemand anderem aufgehoben, verbrannt, verschwunden, ins Leere gefallen oder anders weg - jeweils in eigener Farbe, mit Legende neben der Liste."
          ],
          "secret": [
            "Ein Pausen-Zeichen heißt: Der Chunk ist nicht geladen und der Timer steht. Seelengebundene Gegenstände stehen nie darin: Sie haben dich nie verlassen."
          ]
        }
      },
      "craftedBy": [
        "simpletools:death_vault"
      ],
      "usedIn": []
    }
  ],
  "trades": [],
  "config": [],
  "quests": [],
  "recipesOtherLines": [],
  "inWorld": {
    "entries": [],
    "kinds": []
  },
  "obtain": {
    "sources": []
  },
  "undocumented": [],
  "incompleteProse": {},
  "counts": {
    "features": 32,
    "recipes": 4,
    "lootTables": 1,
    "tags": 3,
    "advancements": 0,
    "enchantments": 1,
    "items": 2,
    "blocks": 1,
    "trades": 0,
    "config": 0,
    "quests": 0,
    "recipesOtherLines": 0,
    "undocumented": 0,
    "inWorld": 0,
    "incompleteProse": 0
  }
};
