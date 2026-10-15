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
        "title": "Ofen mit Lava fuellen",
        "summary": "Beide Haende koennen einen Lavaeimer in Ofen, Hochofen oder Raeucherofen mit leerem Brennstoffplatz oder einem leeren Eimer einsetzen. Verschlossene Oefen und Schleichen sind ausgeschlossen. Ueberleben verbraucht Lava; ein vorhandener Eimer wird genau einmal zurueckgegeben. SimpleBuilding-Ofenunterklassen nutzen die Vanilla-Schnittstelle."
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
        "summary": "Gehaltene beschaedigbare Vanilla- und Mod-Werkzeuge ueber der Haltbarkeitsschwelle koennen mehr Angriffsschaden und Abbaugeschwindigkeit erhalten. Standardfaktor 1.0 (kein Bonus), serverseitiger Bereich 1–1.5. Schwelle standardmaessig 80% (Bereich 80–100%). Die Haltbarkeit selbst bleibt unveraendert."
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
        "summary": "Servereinstellung. Standard: 1.0 (kein Bonus). Bereich: 1.0–1.5. Gespeicherte 1.5 bleiben 1.5; fehlende Schluessel nutzen 1.0. Nicht endliche Werte fallen auf 1.0 zurueck; endliche Werte ausserhalb des Bereichs werden begrenzt."
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
        "summary": "Verzauberung für alles mit Haltbarkeit, Stufen I-III: +1/3 maximale Haltbarkeit je Stufe, Stufe III verdoppelt sie. Nicht mit Reparatur kombinierbar; Haltbarkeit (Unbreaking) wirkt weiter. Die Amboss-Reparatur mit Material gibt je Stück ein Viertel des (größeren) Maximums zurück, also repariert dieselbe Anzahl wie ohne Verzauberung vollständig. Verzauberungstisch, Handel und Beute wie andere Nicht-Schatz-Verzauberungen. Die ID bleibt simplequalityoflife:thrift, damit verzauberte Gegenstaende aus aelteren Welten sie behalten; sie wirkt auch bei abgeschalteter Sub-Mod."
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
        "summary": "Owner-level /simpletools fullDurabilityBonus, anvilRepairKeepsCost and furnaceLava <true|false>; changes are saved to config/simpletools.json."
      },
      "de": {
        "title": "Befehle",
        "summary": "Besitzerberechtigung: /simpletools fullDurabilityBonus, anvilRepairKeepsCost und furnaceLava <true|false>; Speichern in config/simpletools.json."
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
        "summary": "Allein spielbar (config/simpletools.json). In Simple QoL gebuendelt, dort mit \"Simple Tools aktivieren\" abschaltbar. Beim ersten Start werden die alten Simple-QoL-Einstellungen dieser Sub-Mod einmalig uebernommen."
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
        "summary": "Crafted block (obsidian, eye of ender, soul sand or soul soil, chest). It shows what became of each item dropped at your last death, colour coded with a legend: still lying (green), picked up by you (blue) or by another player (red, the name shows on hover), burned in lava, fire or by a cactus (orange), despawned (gray), fallen into the void (purple), gone another way (yellow). A lying item has a bar like a durability bar: how much of its five minutes is left. If its chunk is not loaded the timer stands and the field shows a pause mark. Buying lost items back with an offering is a future feature: the code stays, option deathVaultRedeem is off by default."
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
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simpletools/shared/resources/data/simpletools/loot_table/blocks/death_vault.json"
    }
  ],
  "tags": [
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
      "exclusiveSet": "minecraft:vanishing_curse",
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
  "items": [],
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
          "summary": "Crafted block (obsidian, eye of ender, soul sand or soul soil, chest). It shows what became of each item dropped at your last death, colour coded with a legend: still lying (green), picked up by you (blue) or by another player (red, the name shows on hover), burned in lava, fire or by a cactus (orange), despawned (gray), fallen into the void (purple), gone another way (yellow). A lying item has a bar like a durability bar: how much of its five minutes is left. If its chunk is not loaded the timer stands and the field shows a pause mark. Buying lost items back with an offering is a future feature: the code stays, option deathVaultRedeem is off by default."
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
    "features": 24,
    "recipes": 1,
    "lootTables": 1,
    "tags": 1,
    "advancements": 0,
    "enchantments": 1,
    "items": 0,
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
