window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simpleblocks"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simpleblocks",
    "name": "Simple Blocks",
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
      "id": "hushed_deepslate_tiles",
      "sources": [
        "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
        "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/hushed_deepslate_tiles.json",
        "modules/simpleblocks/shared/resources/data/minecraft/tags/block/dampens_vibrations.json",
        "modules/simpleblocks/shared/resources/data/minecraft/tags/block/occludes_vibration_signals.json",
        "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
      ],
      "related": [
        "simpleblocks:hushed_deepslate_tiles",
        "simpleblocks:hushed_deepslate_tile_stairs",
        "simpleblocks:hushed_deepslate_tile_slab",
        "simpleblocks:hushed_deepslate_tile_wall"
      ],
      "en": {
        "title": "Hushed Deepslate Tiles",
        "summary": "Deepslate tiles that swallow vibrations like wool: a Sculk Sensor or Warden does not hear a vibration that passes through the block, and walking on it makes none.",
        "details": [
          "Recipe: four Deepslate Tiles and one Sculk make four Hushed Deepslate Tiles. Stairs, slab and wall come from the crafting table (6 / 6 / 6) or the stonecutter.",
          "The blocks are in the Vanilla block tags dampens_vibrations and occludes_vibration_signals, the same tags wool is in. Use them to build quiet floors and walls around Sculk Sensors and redstone that listens.",
          "They have the hardness, tool and sound of Deepslate Tiles."
        ]
      },
      "de": {
        "title": "Gedämpfte Tiefenschieferfliesen",
        "summary": "Tiefenschieferfliesen, die Vibrationen wie Wolle schlucken: Ein Sculk-Sensor oder Warden hört keine Vibration, die durch den Block geht, und Laufen darauf erzeugt keine.",
        "details": [
          "Rezept: vier Tiefenschieferfliesen und ein Sculk ergeben vier Gedämpfte Tiefenschieferfliesen. Treppe, Stufe und Mauer gibt es an der Werkbank (6 / 6 / 6) oder am Steinmetz.",
          "Die Blöcke stehen in den Vanilla-Block-Tags dampens_vibrations und occludes_vibration_signals, in denen auch Wolle steht. Damit baust du leise Böden und Wände um Sculk-Sensoren und lauschende Redstone-Schaltungen.",
          "Sie haben Härte, Werkzeug und Klang der Tiefenschieferfliesen."
        ]
      }
    },
    {
      "id": "sculk_deepslate",
      "sources": [
        "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
        "modules/simpleblocks/shared/java/com/simpleblocks/BlocksLoot.java",
        "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/sculk_deepslate.json",
        "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/sculk_deepslate.json",
        "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
      ],
      "related": [
        "simpleblocks:sculk_deepslate"
      ],
      "en": {
        "title": "Sculk Deepslate",
        "summary": "Deepslate with sculk veins. It is not generated in the world: craft it or find it in Ancient City chests.",
        "details": [
          "Recipe (shapeless): one Deepslate and one Sculk make two Sculk Deepslate.",
          "Ancient City chests can hold two to six of them (one pool with weight 8 against 92 empty).",
          "Mined without Silk Touch it drops Cobbled Deepslate and gives 1 to 2 experience; with Silk Touch it drops itself and gives none."
        ]
      },
      "de": {
        "title": "Sculk-Tiefenschiefer",
        "summary": "Tiefenschiefer mit Sculk-Adern. Er wird nicht in der Welt erzeugt: herstellen oder in Truhen der Antiken Stadt finden.",
        "details": [
          "Rezept (formlos): ein Tiefenschiefer und ein Sculk ergeben zwei Sculk-Tiefenschiefer.",
          "Truhen der Antiken Stadt können zwei bis sechs davon enthalten (ein Pool mit Gewicht 8 gegen 92 leer).",
          "Ohne Behutsamkeit abgebaut droppt er Bruchtiefenschiefer und gibt 1 bis 2 Erfahrung; mit Behutsamkeit droppt er sich selbst und gibt keine."
        ]
      }
    },
    {
      "id": "missing_shapes",
      "sources": [
        "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
        "modules/simpleblocks/tools/generate_data.py",
        "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
      ],
      "related": [
        "simpleblocks:calcite_stairs",
        "simpleblocks:calcite_slab",
        "simpleblocks:calcite_wall",
        "simpleblocks:netherrack_stairs",
        "simpleblocks:netherrack_slab",
        "simpleblocks:netherrack_wall",
        "simpleblocks:smooth_basalt_stairs",
        "simpleblocks:smooth_basalt_slab",
        "simpleblocks:smooth_basalt_wall",
        "simpleblocks:cut_sandstone_stairs",
        "simpleblocks:cut_sandstone_wall",
        "simpleblocks:cut_red_sandstone_stairs",
        "simpleblocks:cut_red_sandstone_wall",
        "simpleblocks:smooth_sandstone_wall",
        "simpleblocks:smooth_red_sandstone_wall"
      ],
      "en": {
        "title": "Missing Stairs, Slabs and Walls",
        "summary": "Stairs, slabs and walls Vanilla lacks: Calcite, Netherrack and Smooth Basalt (all three), Cut (Red) Sandstone stairs and walls, Smooth (Red) Sandstone walls.",
        "details": [
          "Crafting table: 6 stairs from 6 blocks, 6 slabs from 3, 6 walls from 6; stonecutter: one block makes one stair or wall, two slabs.",
          "The models use the Vanilla block textures, so they match resource packs. Hardness and sound are those of the full block.",
          "Only stairs, slabs and walls are added - no new brick sets."
        ]
      },
      "de": {
        "title": "Fehlende Treppen, Stufen und Mauern",
        "summary": "Treppen, Stufen und Mauern, die Vanilla fehlen: Kalzit, Netherrack und Glatter Basalt (alle drei), Treppen und Mauern aus Geschnittenem (Rotem) Sandstein, Mauern aus Glattem (Rotem) Sandstein.",
        "details": [
          "Werkbank: 6 Treppen aus 6 Blöcken, 6 Stufen aus 3, 6 Mauern aus 6; Steinmetz: ein Block ergibt eine Treppe oder Mauer, zwei Stufen.",
          "Die Modelle nutzen die Vanilla-Blocktexturen und passen daher zu Ressourcenpaketen. Härte und Klang sind die des Vollblocks.",
          "Es kommen nur Treppen, Stufen und Mauern dazu - keine neuen Ziegelsets."
        ]
      }
    },
    {
      "id": "cracked_and_mossy_bricks",
      "sources": [
        "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
        "tools/textures/missing_blocks_2026_10_16.py",
        "modules/simpleblocks/tools/generate_data.py",
        "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
      ],
      "related": [
        "simpleblocks:cracked_red_nether_bricks",
        "simpleblocks:cracked_tuff_bricks",
        "simpleblocks:mossy_tuff_bricks",
        "simpleblocks:cracked_mud_bricks",
        "simpleblocks:mossy_mud_bricks",
        "simpleblocks:cracked_end_stone_bricks",
        "simpleblocks:mossy_end_stone_bricks",
        "simpleblocks:mossy_deepslate_bricks",
        "simpleblocks:mossy_deepslate_tiles",
        "simpleblocks:cracked_cinnabar_bricks",
        "simpleblocks:mossy_cinnabar_bricks",
        "simpleblocks:cracked_sulfur_bricks",
        "simpleblocks:mossy_sulfur_bricks"
      ],
      "en": {
        "title": "Cracked and Mossy Bricks",
        "summary": "Aged variants of Vanilla bricks that have none: cracked Red Nether, Tuff, Mud, End Stone, Cinnabar and Sulfur bricks; mossy Tuff, Mud, End Stone, Cinnabar and Sulfur bricks, mossy Deepslate Bricks and Tiles.",
        "details": [
          "Cracked: smelt the brick in a furnace, like Vanilla's cracked stone bricks. Mossy: the brick and a Moss Block, shapeless.",
          "Full blocks only, with the properties of the brick they age.",
          "Cinnabar and Sulfur bricks are Vanilla blocks of Minecraft 26.3."
        ]
      },
      "de": {
        "title": "Rissige und bemooste Ziegel",
        "summary": "Gealterte Varianten von Vanilla-Ziegeln, die keine haben: rissige Rote Netherziegel sowie rissige Tuff-, Lehm-, Endstein-, Zinnober- und Schwefelziegel; bemooste Tuff-, Lehm-, Endstein-, Zinnober- und Schwefelziegel, bemooste Tiefenschieferziegel und -fliesen.",
        "details": [
          "Rissig: den Ziegel im Ofen schmelzen, wie Vanillas rissige Steinziegel. Bemoost: der Ziegel und ein Moosblock, formlos.",
          "Nur Vollblöcke, mit den Eigenschaften des Ziegels, den sie altern.",
          "Zinnober- und Schwefelziegel sind Vanilla-Blöcke von Minecraft 26.3."
        ]
      }
    },
    {
      "id": "blue_nether_bricks",
      "sources": [
        "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
        "modules/simpleblocks/tools/generate_data.py",
        "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/blue_nether_bricks.json",
        "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
      ],
      "related": [
        "simpleblocks:blue_nether_bricks",
        "simpleblocks:blue_nether_brick_slab",
        "simpleblocks:blue_nether_brick_stairs",
        "simpleblocks:blue_nether_brick_wall"
      ],
      "en": {
        "title": "Blue Nether Bricks",
        "summary": "A blue counterpart to Red Nether Bricks, with stairs, slab and wall.",
        "details": [
          "Recipe: two Nether Bricks and two Warped Warts in a checker pattern (needs Simple Lib). Without Simple Lib: one Warped Wart Block and eight Nether Bricks, shapeless, give 4.",
          "Stairs, slab and wall: crafting table or stonecutter, like the other Simple Blocks shapes."
        ]
      },
      "de": {
        "title": "Blaue Netherziegel",
        "summary": "Ein blaues Gegenstück zu den Roten Netherziegeln, mit Treppe, Stufe und Mauer.",
        "details": [
          "Rezept: zwei Netherziegel und zwei Wirrwarzen im Schachbrettmuster (braucht Simple Lib). Ohne Simple Lib: ein Wirrwarzenblock und acht Netherziegel, formlos, ergeben 4.",
          "Treppe, Stufe und Mauer: Werkbank oder Steinsäge, wie die anderen Simple-Blocks-Formen."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simpleblocks:blue_nether_brick_slab",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:blue_nether_brick_slab",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/blue_nether_brick_slab.json",
      "ingredients": [
        "simpleblocks:blue_nether_bricks"
      ],
      "pattern": [
        "###"
      ],
      "key": {
        "#": [
          "simpleblocks:blue_nether_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 6
          },
          {
            "id": "simplelib:warped_wart",
            "count": 6
          }
        ]
      }
    },
    {
      "id": "simpleblocks:blue_nether_brick_slab_from_blue_nether_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:blue_nether_brick_slab",
        "count": 2
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/blue_nether_brick_slab_from_blue_nether_bricks_stonecutting.json",
      "ingredients": [
        "simpleblocks:blue_nether_bricks"
      ],
      "slots": {
        "ingredient": [
          "simpleblocks:blue_nether_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 2,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 2
          },
          {
            "id": "simplelib:warped_wart",
            "count": 2
          }
        ]
      }
    },
    {
      "id": "simpleblocks:blue_nether_brick_stairs",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:blue_nether_brick_stairs",
        "count": 4
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/blue_nether_brick_stairs.json",
      "ingredients": [
        "simpleblocks:blue_nether_bricks"
      ],
      "pattern": [
        "#  ",
        "## ",
        "###"
      ],
      "key": {
        "#": [
          "simpleblocks:blue_nether_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 4,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 12
          },
          {
            "id": "simplelib:warped_wart",
            "count": 12
          }
        ]
      }
    },
    {
      "id": "simpleblocks:blue_nether_brick_stairs_from_blue_nether_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:blue_nether_brick_stairs",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/blue_nether_brick_stairs_from_blue_nether_bricks_stonecutting.json",
      "ingredients": [
        "simpleblocks:blue_nether_bricks"
      ],
      "slots": {
        "ingredient": [
          "simpleblocks:blue_nether_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 2
          },
          {
            "id": "simplelib:warped_wart",
            "count": 2
          }
        ]
      }
    },
    {
      "id": "simpleblocks:blue_nether_brick_wall",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpleblocks:blue_nether_brick_wall",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/blue_nether_brick_wall.json",
      "ingredients": [
        "simpleblocks:blue_nether_bricks"
      ],
      "pattern": [
        "###",
        "###"
      ],
      "key": {
        "#": [
          "simpleblocks:blue_nether_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 12
          },
          {
            "id": "simplelib:warped_wart",
            "count": 12
          }
        ]
      }
    },
    {
      "id": "simpleblocks:blue_nether_brick_wall_from_blue_nether_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:blue_nether_brick_wall",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/blue_nether_brick_wall_from_blue_nether_bricks_stonecutting.json",
      "ingredients": [
        "simpleblocks:blue_nether_bricks"
      ],
      "slots": {
        "ingredient": [
          "simpleblocks:blue_nether_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 2
          },
          {
            "id": "simplelib:warped_wart",
            "count": 2
          }
        ]
      }
    },
    {
      "id": "simpleblocks:blue_nether_bricks",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:blue_nether_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/blue_nether_bricks.json",
      "ingredients": [
        "minecraft:nether_brick",
        "simplelib:warped_wart"
      ],
      "pattern": [
        "WN",
        "NW"
      ],
      "key": {
        "W": [
          "simplelib:warped_wart"
        ],
        "N": [
          "minecraft:nether_brick"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 2
          },
          {
            "id": "simplelib:warped_wart",
            "count": 2
          }
        ]
      }
    },
    {
      "id": "simpleblocks:blue_nether_bricks_without_simplelib",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:blue_nether_bricks",
        "count": 4
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/blue_nether_bricks_without_simplelib.json",
      "ingredients": [
        "minecraft:nether_brick",
        "minecraft:warped_wart_block"
      ],
      "ingredientGroups": [
        [
          "minecraft:warped_wart_block"
        ],
        [
          "minecraft:nether_brick"
        ],
        [
          "minecraft:nether_brick"
        ],
        [
          "minecraft:nether_brick"
        ],
        [
          "minecraft:nether_brick"
        ],
        [
          "minecraft:nether_brick"
        ],
        [
          "minecraft:nether_brick"
        ],
        [
          "minecraft:nether_brick"
        ],
        [
          "minecraft:nether_brick"
        ]
      ],
      "baseMaterials": {
        "yield": 4,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 8
          },
          {
            "id": "minecraft:warped_wart_block",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:calcite_slab",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:calcite_slab",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/calcite_slab.json",
      "ingredients": [
        "minecraft:calcite"
      ],
      "pattern": [
        "###"
      ],
      "key": {
        "#": [
          "minecraft:calcite"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:calcite",
            "count": 3
          }
        ]
      }
    },
    {
      "id": "simpleblocks:calcite_slab_from_calcite_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:calcite_slab",
        "count": 2
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/calcite_slab_from_calcite_stonecutting.json",
      "ingredients": [
        "minecraft:calcite"
      ],
      "slots": {
        "ingredient": [
          "minecraft:calcite"
        ]
      },
      "baseMaterials": {
        "yield": 2,
        "materials": [
          {
            "id": "minecraft:calcite",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:calcite_stairs",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:calcite_stairs",
        "count": 4
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/calcite_stairs.json",
      "ingredients": [
        "minecraft:calcite"
      ],
      "pattern": [
        "#  ",
        "## ",
        "###"
      ],
      "key": {
        "#": [
          "minecraft:calcite"
        ]
      },
      "baseMaterials": {
        "yield": 4,
        "materials": [
          {
            "id": "minecraft:calcite",
            "count": 6
          }
        ]
      }
    },
    {
      "id": "simpleblocks:calcite_stairs_from_calcite_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:calcite_stairs",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/calcite_stairs_from_calcite_stonecutting.json",
      "ingredients": [
        "minecraft:calcite"
      ],
      "slots": {
        "ingredient": [
          "minecraft:calcite"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:calcite",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:calcite_wall",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpleblocks:calcite_wall",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/calcite_wall.json",
      "ingredients": [
        "minecraft:calcite"
      ],
      "pattern": [
        "###",
        "###"
      ],
      "key": {
        "#": [
          "minecraft:calcite"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:calcite",
            "count": 6
          }
        ]
      }
    },
    {
      "id": "simpleblocks:calcite_wall_from_calcite_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:calcite_wall",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/calcite_wall_from_calcite_stonecutting.json",
      "ingredients": [
        "minecraft:calcite"
      ],
      "slots": {
        "ingredient": [
          "minecraft:calcite"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:calcite",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cracked_cinnabar_bricks_from_smelting",
      "type": "minecraft:smelting",
      "category": "blocks",
      "group": null,
      "result": {
        "id": "simpleblocks:cracked_cinnabar_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cracked_cinnabar_bricks_from_smelting.json",
      "ingredients": [
        "minecraft:cinnabar_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cinnabar_bricks"
        ]
      },
      "cookingtime": 200,
      "experience": 0.1,
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:cinnabar",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cracked_end_stone_bricks_from_smelting",
      "type": "minecraft:smelting",
      "category": "blocks",
      "group": null,
      "result": {
        "id": "simpleblocks:cracked_end_stone_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cracked_end_stone_bricks_from_smelting.json",
      "ingredients": [
        "minecraft:end_stone_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:end_stone_bricks"
        ]
      },
      "cookingtime": 200,
      "experience": 0.1,
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:end_stone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cracked_mud_bricks_from_smelting",
      "type": "minecraft:smelting",
      "category": "blocks",
      "group": null,
      "result": {
        "id": "simpleblocks:cracked_mud_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cracked_mud_bricks_from_smelting.json",
      "ingredients": [
        "minecraft:mud_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:mud_bricks"
        ]
      },
      "cookingtime": 200,
      "experience": 0.1,
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:mud",
            "count": 1
          },
          {
            "id": "minecraft:wheat",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cracked_red_nether_bricks_from_smelting",
      "type": "minecraft:smelting",
      "category": "blocks",
      "group": null,
      "result": {
        "id": "simpleblocks:cracked_red_nether_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cracked_red_nether_bricks_from_smelting.json",
      "ingredients": [
        "minecraft:red_nether_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:red_nether_bricks"
        ]
      },
      "cookingtime": 200,
      "experience": 0.1,
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:nether_wart",
            "count": 2
          },
          {
            "id": "minecraft:netherrack",
            "count": 2
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cracked_sulfur_bricks_from_smelting",
      "type": "minecraft:smelting",
      "category": "blocks",
      "group": null,
      "result": {
        "id": "simpleblocks:cracked_sulfur_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cracked_sulfur_bricks_from_smelting.json",
      "ingredients": [
        "minecraft:sulfur_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:sulfur_bricks"
        ]
      },
      "cookingtime": 200,
      "experience": 0.1,
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:sulfur_spike",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cracked_tuff_bricks_from_smelting",
      "type": "minecraft:smelting",
      "category": "blocks",
      "group": null,
      "result": {
        "id": "simpleblocks:cracked_tuff_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cracked_tuff_bricks_from_smelting.json",
      "ingredients": [
        "minecraft:tuff_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:tuff_bricks"
        ]
      },
      "cookingtime": 200,
      "experience": 0.1,
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:tuff",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cut_red_sandstone_stairs",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:cut_red_sandstone_stairs",
        "count": 4
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cut_red_sandstone_stairs.json",
      "ingredients": [
        "minecraft:cut_red_sandstone"
      ],
      "pattern": [
        "#  ",
        "## ",
        "###"
      ],
      "key": {
        "#": [
          "minecraft:cut_red_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 4,
        "materials": [
          {
            "id": "minecraft:red_sand",
            "count": 24
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cut_red_sandstone_stairs_from_cut_red_sandstone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:cut_red_sandstone_stairs",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cut_red_sandstone_stairs_from_cut_red_sandstone_stonecutting.json",
      "ingredients": [
        "minecraft:cut_red_sandstone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cut_red_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:red_sand",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cut_red_sandstone_wall",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpleblocks:cut_red_sandstone_wall",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cut_red_sandstone_wall.json",
      "ingredients": [
        "minecraft:cut_red_sandstone"
      ],
      "pattern": [
        "###",
        "###"
      ],
      "key": {
        "#": [
          "minecraft:cut_red_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:red_sand",
            "count": 24
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cut_red_sandstone_wall_from_cut_red_sandstone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:cut_red_sandstone_wall",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cut_red_sandstone_wall_from_cut_red_sandstone_stonecutting.json",
      "ingredients": [
        "minecraft:cut_red_sandstone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cut_red_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:red_sand",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cut_sandstone_stairs",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:cut_sandstone_stairs",
        "count": 4
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cut_sandstone_stairs.json",
      "ingredients": [
        "minecraft:cut_sandstone"
      ],
      "pattern": [
        "#  ",
        "## ",
        "###"
      ],
      "key": {
        "#": [
          "minecraft:cut_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 4,
        "materials": [
          {
            "id": "minecraft:sand",
            "count": 24
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cut_sandstone_stairs_from_cut_sandstone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:cut_sandstone_stairs",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cut_sandstone_stairs_from_cut_sandstone_stonecutting.json",
      "ingredients": [
        "minecraft:cut_sandstone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cut_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:sand",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cut_sandstone_wall",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpleblocks:cut_sandstone_wall",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cut_sandstone_wall.json",
      "ingredients": [
        "minecraft:cut_sandstone"
      ],
      "pattern": [
        "###",
        "###"
      ],
      "key": {
        "#": [
          "minecraft:cut_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:sand",
            "count": 24
          }
        ]
      }
    },
    {
      "id": "simpleblocks:cut_sandstone_wall_from_cut_sandstone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:cut_sandstone_wall",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/cut_sandstone_wall_from_cut_sandstone_stonecutting.json",
      "ingredients": [
        "minecraft:cut_sandstone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cut_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:sand",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleblocks:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpleblocks:guide_book",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:clay_ball"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:clay_ball"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:sugar_cane",
            "count": 3
          },
          {
            "id": "minecraft:clay_ball",
            "count": 1
          },
          {
            "id": "minecraft:leather",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:hushed_deepslate_tile_slab",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:hushed_deepslate_tile_slab",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/hushed_deepslate_tile_slab.json",
      "ingredients": [
        "simpleblocks:hushed_deepslate_tiles"
      ],
      "pattern": [
        "###"
      ],
      "key": {
        "#": [
          "simpleblocks:hushed_deepslate_tiles"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 3
          },
          {
            "id": "minecraft:sculk",
            "count": 0.75
          }
        ]
      }
    },
    {
      "id": "simpleblocks:hushed_deepslate_tile_slab_from_hushed_deepslate_tiles_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:hushed_deepslate_tile_slab",
        "count": 2
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/hushed_deepslate_tile_slab_from_hushed_deepslate_tiles_stonecutting.json",
      "ingredients": [
        "simpleblocks:hushed_deepslate_tiles"
      ],
      "slots": {
        "ingredient": [
          "simpleblocks:hushed_deepslate_tiles"
        ]
      },
      "baseMaterials": {
        "yield": 2,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 1
          },
          {
            "id": "minecraft:sculk",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleblocks:hushed_deepslate_tile_stairs",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:hushed_deepslate_tile_stairs",
        "count": 4
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/hushed_deepslate_tile_stairs.json",
      "ingredients": [
        "simpleblocks:hushed_deepslate_tiles"
      ],
      "pattern": [
        "#  ",
        "## ",
        "###"
      ],
      "key": {
        "#": [
          "simpleblocks:hushed_deepslate_tiles"
        ]
      },
      "baseMaterials": {
        "yield": 4,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 6
          },
          {
            "id": "minecraft:sculk",
            "count": 1.5
          }
        ]
      }
    },
    {
      "id": "simpleblocks:hushed_deepslate_tile_stairs_from_hushed_deepslate_tiles_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:hushed_deepslate_tile_stairs",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/hushed_deepslate_tile_stairs_from_hushed_deepslate_tiles_stonecutting.json",
      "ingredients": [
        "simpleblocks:hushed_deepslate_tiles"
      ],
      "slots": {
        "ingredient": [
          "simpleblocks:hushed_deepslate_tiles"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 1
          },
          {
            "id": "minecraft:sculk",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleblocks:hushed_deepslate_tile_wall",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpleblocks:hushed_deepslate_tile_wall",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/hushed_deepslate_tile_wall.json",
      "ingredients": [
        "simpleblocks:hushed_deepslate_tiles"
      ],
      "pattern": [
        "###",
        "###"
      ],
      "key": {
        "#": [
          "simpleblocks:hushed_deepslate_tiles"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 6
          },
          {
            "id": "minecraft:sculk",
            "count": 1.5
          }
        ]
      }
    },
    {
      "id": "simpleblocks:hushed_deepslate_tile_wall_from_hushed_deepslate_tiles_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:hushed_deepslate_tile_wall",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/hushed_deepslate_tile_wall_from_hushed_deepslate_tiles_stonecutting.json",
      "ingredients": [
        "simpleblocks:hushed_deepslate_tiles"
      ],
      "slots": {
        "ingredient": [
          "simpleblocks:hushed_deepslate_tiles"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 1
          },
          {
            "id": "minecraft:sculk",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleblocks:hushed_deepslate_tiles",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:hushed_deepslate_tiles",
        "count": 4
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/hushed_deepslate_tiles.json",
      "ingredients": [
        "minecraft:deepslate_tiles",
        "minecraft:sculk"
      ],
      "ingredientGroups": [
        [
          "minecraft:deepslate_tiles"
        ],
        [
          "minecraft:deepslate_tiles"
        ],
        [
          "minecraft:deepslate_tiles"
        ],
        [
          "minecraft:deepslate_tiles"
        ],
        [
          "minecraft:sculk"
        ]
      ],
      "baseMaterials": {
        "yield": 4,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 4
          },
          {
            "id": "minecraft:sculk",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:mossy_cinnabar_bricks_from_moss_block",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:mossy_cinnabar_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/mossy_cinnabar_bricks_from_moss_block.json",
      "ingredients": [
        "minecraft:cinnabar_bricks",
        "minecraft:moss_block"
      ],
      "ingredientGroups": [
        [
          "minecraft:cinnabar_bricks"
        ],
        [
          "minecraft:moss_block"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:cinnabar",
            "count": 1
          },
          {
            "id": "minecraft:moss_block",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:mossy_deepslate_bricks_from_moss_block",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:mossy_deepslate_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/mossy_deepslate_bricks_from_moss_block.json",
      "ingredients": [
        "minecraft:deepslate_bricks",
        "minecraft:moss_block"
      ],
      "ingredientGroups": [
        [
          "minecraft:deepslate_bricks"
        ],
        [
          "minecraft:moss_block"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 1
          },
          {
            "id": "minecraft:moss_block",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:mossy_deepslate_tiles_from_moss_block",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:mossy_deepslate_tiles",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/mossy_deepslate_tiles_from_moss_block.json",
      "ingredients": [
        "minecraft:deepslate_tiles",
        "minecraft:moss_block"
      ],
      "ingredientGroups": [
        [
          "minecraft:deepslate_tiles"
        ],
        [
          "minecraft:moss_block"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 1
          },
          {
            "id": "minecraft:moss_block",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:mossy_end_stone_bricks_from_moss_block",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:mossy_end_stone_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/mossy_end_stone_bricks_from_moss_block.json",
      "ingredients": [
        "minecraft:end_stone_bricks",
        "minecraft:moss_block"
      ],
      "ingredientGroups": [
        [
          "minecraft:end_stone_bricks"
        ],
        [
          "minecraft:moss_block"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:end_stone",
            "count": 1
          },
          {
            "id": "minecraft:moss_block",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:mossy_mud_bricks_from_moss_block",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:mossy_mud_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/mossy_mud_bricks_from_moss_block.json",
      "ingredients": [
        "minecraft:moss_block",
        "minecraft:mud_bricks"
      ],
      "ingredientGroups": [
        [
          "minecraft:mud_bricks"
        ],
        [
          "minecraft:moss_block"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:moss_block",
            "count": 1
          },
          {
            "id": "minecraft:mud",
            "count": 1
          },
          {
            "id": "minecraft:wheat",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:mossy_sulfur_bricks_from_moss_block",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:mossy_sulfur_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/mossy_sulfur_bricks_from_moss_block.json",
      "ingredients": [
        "minecraft:moss_block",
        "minecraft:sulfur_bricks"
      ],
      "ingredientGroups": [
        [
          "minecraft:sulfur_bricks"
        ],
        [
          "minecraft:moss_block"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:sulfur_spike",
            "count": 4
          },
          {
            "id": "minecraft:moss_block",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:mossy_tuff_bricks_from_moss_block",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:mossy_tuff_bricks",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/mossy_tuff_bricks_from_moss_block.json",
      "ingredients": [
        "minecraft:moss_block",
        "minecraft:tuff_bricks"
      ],
      "ingredientGroups": [
        [
          "minecraft:tuff_bricks"
        ],
        [
          "minecraft:moss_block"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:moss_block",
            "count": 1
          },
          {
            "id": "minecraft:tuff",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:netherrack_slab",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:netherrack_slab",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/netherrack_slab.json",
      "ingredients": [
        "minecraft:netherrack"
      ],
      "pattern": [
        "###"
      ],
      "key": {
        "#": [
          "minecraft:netherrack"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 3
          }
        ]
      }
    },
    {
      "id": "simpleblocks:netherrack_slab_from_netherrack_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:netherrack_slab",
        "count": 2
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/netherrack_slab_from_netherrack_stonecutting.json",
      "ingredients": [
        "minecraft:netherrack"
      ],
      "slots": {
        "ingredient": [
          "minecraft:netherrack"
        ]
      },
      "baseMaterials": {
        "yield": 2,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:netherrack_stairs",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:netherrack_stairs",
        "count": 4
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/netherrack_stairs.json",
      "ingredients": [
        "minecraft:netherrack"
      ],
      "pattern": [
        "#  ",
        "## ",
        "###"
      ],
      "key": {
        "#": [
          "minecraft:netherrack"
        ]
      },
      "baseMaterials": {
        "yield": 4,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 6
          }
        ]
      }
    },
    {
      "id": "simpleblocks:netherrack_stairs_from_netherrack_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:netherrack_stairs",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/netherrack_stairs_from_netherrack_stonecutting.json",
      "ingredients": [
        "minecraft:netherrack"
      ],
      "slots": {
        "ingredient": [
          "minecraft:netherrack"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:netherrack_wall",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpleblocks:netherrack_wall",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/netherrack_wall.json",
      "ingredients": [
        "minecraft:netherrack"
      ],
      "pattern": [
        "###",
        "###"
      ],
      "key": {
        "#": [
          "minecraft:netherrack"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 6
          }
        ]
      }
    },
    {
      "id": "simpleblocks:netherrack_wall_from_netherrack_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:netherrack_wall",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/netherrack_wall_from_netherrack_stonecutting.json",
      "ingredients": [
        "minecraft:netherrack"
      ],
      "slots": {
        "ingredient": [
          "minecraft:netherrack"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:sculk_deepslate",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:sculk_deepslate",
        "count": 2
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/sculk_deepslate.json",
      "ingredients": [
        "minecraft:deepslate",
        "minecraft:sculk"
      ],
      "ingredientGroups": [
        [
          "minecraft:deepslate"
        ],
        [
          "minecraft:sculk"
        ]
      ],
      "baseMaterials": {
        "yield": 2,
        "materials": [
          {
            "id": "minecraft:deepslate",
            "count": 1
          },
          {
            "id": "minecraft:sculk",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:smooth_basalt_slab",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:smooth_basalt_slab",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/smooth_basalt_slab.json",
      "ingredients": [
        "minecraft:smooth_basalt"
      ],
      "pattern": [
        "###"
      ],
      "key": {
        "#": [
          "minecraft:smooth_basalt"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:basalt",
            "count": 3
          }
        ]
      }
    },
    {
      "id": "simpleblocks:smooth_basalt_slab_from_smooth_basalt_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:smooth_basalt_slab",
        "count": 2
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/smooth_basalt_slab_from_smooth_basalt_stonecutting.json",
      "ingredients": [
        "minecraft:smooth_basalt"
      ],
      "slots": {
        "ingredient": [
          "minecraft:smooth_basalt"
        ]
      },
      "baseMaterials": {
        "yield": 2,
        "materials": [
          {
            "id": "minecraft:basalt",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:smooth_basalt_stairs",
      "type": "minecraft:crafting_shaped",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:smooth_basalt_stairs",
        "count": 4
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/smooth_basalt_stairs.json",
      "ingredients": [
        "minecraft:smooth_basalt"
      ],
      "pattern": [
        "#  ",
        "## ",
        "###"
      ],
      "key": {
        "#": [
          "minecraft:smooth_basalt"
        ]
      },
      "baseMaterials": {
        "yield": 4,
        "materials": [
          {
            "id": "minecraft:basalt",
            "count": 6
          }
        ]
      }
    },
    {
      "id": "simpleblocks:smooth_basalt_stairs_from_smooth_basalt_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:smooth_basalt_stairs",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/smooth_basalt_stairs_from_smooth_basalt_stonecutting.json",
      "ingredients": [
        "minecraft:smooth_basalt"
      ],
      "slots": {
        "ingredient": [
          "minecraft:smooth_basalt"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:basalt",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:smooth_basalt_wall",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpleblocks:smooth_basalt_wall",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/smooth_basalt_wall.json",
      "ingredients": [
        "minecraft:smooth_basalt"
      ],
      "pattern": [
        "###",
        "###"
      ],
      "key": {
        "#": [
          "minecraft:smooth_basalt"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:basalt",
            "count": 6
          }
        ]
      }
    },
    {
      "id": "simpleblocks:smooth_basalt_wall_from_smooth_basalt_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:smooth_basalt_wall",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/smooth_basalt_wall_from_smooth_basalt_stonecutting.json",
      "ingredients": [
        "minecraft:smooth_basalt"
      ],
      "slots": {
        "ingredient": [
          "minecraft:smooth_basalt"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:basalt",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleblocks:smooth_red_sandstone_wall",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpleblocks:smooth_red_sandstone_wall",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/smooth_red_sandstone_wall.json",
      "ingredients": [
        "minecraft:smooth_red_sandstone"
      ],
      "pattern": [
        "###",
        "###"
      ],
      "key": {
        "#": [
          "minecraft:smooth_red_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:red_sand",
            "count": 24
          }
        ]
      }
    },
    {
      "id": "simpleblocks:smooth_red_sandstone_wall_from_smooth_red_sandstone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:smooth_red_sandstone_wall",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/smooth_red_sandstone_wall_from_smooth_red_sandstone_stonecutting.json",
      "ingredients": [
        "minecraft:smooth_red_sandstone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:smooth_red_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:red_sand",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleblocks:smooth_sandstone_wall",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpleblocks:smooth_sandstone_wall",
        "count": 6
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/smooth_sandstone_wall.json",
      "ingredients": [
        "minecraft:smooth_sandstone"
      ],
      "pattern": [
        "###",
        "###"
      ],
      "key": {
        "#": [
          "minecraft:smooth_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 6,
        "materials": [
          {
            "id": "minecraft:sand",
            "count": 24
          }
        ]
      }
    },
    {
      "id": "simpleblocks:smooth_sandstone_wall_from_smooth_sandstone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleblocks:smooth_sandstone_wall",
        "count": 1
      },
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/smooth_sandstone_wall_from_smooth_sandstone_stonecutting.json",
      "ingredients": [
        "minecraft:smooth_sandstone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:smooth_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:sand",
            "count": 4
          }
        ]
      }
    }
  ],
  "lootTables": [
    {
      "id": "simpleblocks:blocks/blue_nether_brick_slab",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:blue_nether_brick_slab"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/blue_nether_brick_slab.json"
    },
    {
      "id": "simpleblocks:blocks/blue_nether_brick_stairs",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:blue_nether_brick_stairs"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/blue_nether_brick_stairs.json"
    },
    {
      "id": "simpleblocks:blocks/blue_nether_brick_wall",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:blue_nether_brick_wall"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/blue_nether_brick_wall.json"
    },
    {
      "id": "simpleblocks:blocks/blue_nether_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:blue_nether_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/blue_nether_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/calcite_slab",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:calcite_slab"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/calcite_slab.json"
    },
    {
      "id": "simpleblocks:blocks/calcite_stairs",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:calcite_stairs"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/calcite_stairs.json"
    },
    {
      "id": "simpleblocks:blocks/calcite_wall",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:calcite_wall"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/calcite_wall.json"
    },
    {
      "id": "simpleblocks:blocks/cracked_cinnabar_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:cracked_cinnabar_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/cracked_cinnabar_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/cracked_end_stone_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:cracked_end_stone_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/cracked_end_stone_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/cracked_mud_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:cracked_mud_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/cracked_mud_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/cracked_red_nether_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:cracked_red_nether_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/cracked_red_nether_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/cracked_sulfur_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:cracked_sulfur_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/cracked_sulfur_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/cracked_tuff_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:cracked_tuff_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/cracked_tuff_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/cut_red_sandstone_stairs",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:cut_red_sandstone_stairs"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/cut_red_sandstone_stairs.json"
    },
    {
      "id": "simpleblocks:blocks/cut_red_sandstone_wall",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:cut_red_sandstone_wall"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/cut_red_sandstone_wall.json"
    },
    {
      "id": "simpleblocks:blocks/cut_sandstone_stairs",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:cut_sandstone_stairs"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/cut_sandstone_stairs.json"
    },
    {
      "id": "simpleblocks:blocks/cut_sandstone_wall",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:cut_sandstone_wall"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/cut_sandstone_wall.json"
    },
    {
      "id": "simpleblocks:blocks/hushed_deepslate_tile_slab",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:hushed_deepslate_tile_slab"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/hushed_deepslate_tile_slab.json"
    },
    {
      "id": "simpleblocks:blocks/hushed_deepslate_tile_stairs",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:hushed_deepslate_tile_stairs"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/hushed_deepslate_tile_stairs.json"
    },
    {
      "id": "simpleblocks:blocks/hushed_deepslate_tile_wall",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:hushed_deepslate_tile_wall"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/hushed_deepslate_tile_wall.json"
    },
    {
      "id": "simpleblocks:blocks/hushed_deepslate_tiles",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:hushed_deepslate_tiles"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/hushed_deepslate_tiles.json"
    },
    {
      "id": "simpleblocks:blocks/mossy_cinnabar_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:mossy_cinnabar_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/mossy_cinnabar_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/mossy_deepslate_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:mossy_deepslate_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/mossy_deepslate_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/mossy_deepslate_tiles",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:mossy_deepslate_tiles"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/mossy_deepslate_tiles.json"
    },
    {
      "id": "simpleblocks:blocks/mossy_end_stone_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:mossy_end_stone_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/mossy_end_stone_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/mossy_mud_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:mossy_mud_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/mossy_mud_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/mossy_sulfur_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:mossy_sulfur_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/mossy_sulfur_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/mossy_tuff_bricks",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:mossy_tuff_bricks"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/mossy_tuff_bricks.json"
    },
    {
      "id": "simpleblocks:blocks/netherrack_slab",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:netherrack_slab"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/netherrack_slab.json"
    },
    {
      "id": "simpleblocks:blocks/netherrack_stairs",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:netherrack_stairs"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/netherrack_stairs.json"
    },
    {
      "id": "simpleblocks:blocks/netherrack_wall",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:netherrack_wall"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/netherrack_wall.json"
    },
    {
      "id": "simpleblocks:blocks/sculk_deepslate",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:sculk_deepslate",
            "minecraft:cobbled_deepslate"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/sculk_deepslate.json"
    },
    {
      "id": "simpleblocks:blocks/smooth_basalt_slab",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:smooth_basalt_slab"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/smooth_basalt_slab.json"
    },
    {
      "id": "simpleblocks:blocks/smooth_basalt_stairs",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:smooth_basalt_stairs"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/smooth_basalt_stairs.json"
    },
    {
      "id": "simpleblocks:blocks/smooth_basalt_wall",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:smooth_basalt_wall"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/smooth_basalt_wall.json"
    },
    {
      "id": "simpleblocks:blocks/smooth_red_sandstone_wall",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:smooth_red_sandstone_wall"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/smooth_red_sandstone_wall.json"
    },
    {
      "id": "simpleblocks:blocks/smooth_sandstone_wall",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simpleblocks:smooth_sandstone_wall"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simpleblocks/shared/resources/data/simpleblocks/loot_table/blocks/smooth_sandstone_wall.json"
    }
  ],
  "tags": [],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "simpleblocks:guide_book",
      "name": {
        "en_us": "Simple Blocks Guide",
        "de_de": "Simple-Blocks-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/guide/BlocksGuide.java",
          "modules/simpleblocks/shared/resources/data/simpleblocks/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 7 pages taken from this wiki, shown in your language. Shapeless recipe: book + clay ball. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Blocks\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 7 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Tonklumpen. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Blocks\" eins."
        }
      },
      "texture": "assets/textures/simpleblocks/item/guide_book.png",
      "craftedBy": [
        "simpleblocks:guide_book"
      ],
      "usedIn": []
    }
  ],
  "blocks": [
    {
      "id": "simpleblocks:blue_nether_brick_slab",
      "name": {
        "en_us": "Blue Nether Brick Slab",
        "de_de": "Blaue Netherziegelstufe"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Slab variant of a Simple Blocks block, crafted in a row of three blocks of its material (six slabs); it also works in the stonecutter."
        },
        "de": {
          "summary": "Plattenvariante eines Simple-Blocks-Blocks, in einer Reihe aus drei Blöcken des Materials hergestellt (sechs Platten); auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:blue_nether_brick_slab",
        "simpleblocks:blue_nether_brick_slab_from_blue_nether_bricks_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:blue_nether_brick_stairs",
      "name": {
        "en_us": "Blue Nether Brick Stairs",
        "de_de": "Blaue Netherziegeltreppe"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Stair variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Treppenvariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:blue_nether_brick_stairs",
        "simpleblocks:blue_nether_brick_stairs_from_blue_nether_bricks_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:blue_nether_brick_wall",
      "name": {
        "en_us": "Blue Nether Brick Wall",
        "de_de": "Blaue Netherziegelmauer"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Wall variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Mauervariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:blue_nether_brick_wall",
        "simpleblocks:blue_nether_brick_wall_from_blue_nether_bricks_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:blue_nether_bricks",
      "name": {
        "en_us": "Blue Nether Bricks",
        "de_de": "Blaue Netherziegel"
      },
      "note": {
        "en": {
          "title": "Blue Nether Bricks",
          "summary": "A blue counterpart to Red Nether Bricks, with stairs, slab and wall.",
          "details": [
            "Recipe: two Nether Bricks and two Warped Warts in a checker pattern (needs Simple Lib). Without Simple Lib: one Warped Wart Block and eight Nether Bricks, shapeless, give 4.",
            "Stairs, slab and wall: crafting table or stonecutter, like the other Simple Blocks shapes."
          ]
        },
        "de": {
          "title": "Blaue Netherziegel",
          "summary": "Ein blaues Gegenstück zu den Roten Netherziegeln, mit Treppe, Stufe und Mauer.",
          "details": [
            "Rezept: zwei Netherziegel und zwei Wirrwarzen im Schachbrettmuster (braucht Simple Lib). Ohne Simple Lib: ein Wirrwarzenblock und acht Netherziegel, formlos, ergeben 4.",
            "Treppe, Stufe und Mauer: Werkbank oder Steinsäge, wie die anderen Simple-Blocks-Formen."
          ]
        }
      },
      "texture": "assets/textures/simpleblocks/block/blue_nether_bricks.png",
      "craftedBy": [
        "simpleblocks:blue_nether_bricks",
        "simpleblocks:blue_nether_bricks_without_simplelib"
      ],
      "usedIn": [
        "simpleblocks:blue_nether_brick_slab",
        "simpleblocks:blue_nether_brick_slab_from_blue_nether_bricks_stonecutting",
        "simpleblocks:blue_nether_brick_stairs",
        "simpleblocks:blue_nether_brick_stairs_from_blue_nether_bricks_stonecutting",
        "simpleblocks:blue_nether_brick_wall",
        "simpleblocks:blue_nether_brick_wall_from_blue_nether_bricks_stonecutting"
      ]
    },
    {
      "id": "simpleblocks:calcite_slab",
      "name": {
        "en_us": "Calcite Slab",
        "de_de": "Kalzitstufe"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Slab variant of a Simple Blocks block, crafted in a row of three blocks of its material (six slabs); it also works in the stonecutter."
        },
        "de": {
          "summary": "Plattenvariante eines Simple-Blocks-Blocks, in einer Reihe aus drei Blöcken des Materials hergestellt (sechs Platten); auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:calcite_slab",
        "simpleblocks:calcite_slab_from_calcite_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:calcite_stairs",
      "name": {
        "en_us": "Calcite Stairs",
        "de_de": "Kalzittreppe"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Stair variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Treppenvariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:calcite_stairs",
        "simpleblocks:calcite_stairs_from_calcite_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:calcite_wall",
      "name": {
        "en_us": "Calcite Wall",
        "de_de": "Kalzitmauer"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Wall variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Mauervariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:calcite_wall",
        "simpleblocks:calcite_wall_from_calcite_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:cracked_cinnabar_bricks",
      "name": {
        "en_us": "Cracked Cinnabar Bricks",
        "de_de": "Rissige Zinnoberziegel"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Cracked brick variant of a Vanilla or Simple Blocks brick block. Smelt the plain bricks in a furnace to get it. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Rissige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel im Ofen schmelzen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/cracked_cinnabar_bricks.png",
      "craftedBy": [
        "simpleblocks:cracked_cinnabar_bricks_from_smelting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:cracked_end_stone_bricks",
      "name": {
        "en_us": "Cracked End Stone Bricks",
        "de_de": "Rissige Endsteinziegel"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Cracked brick variant of a Vanilla or Simple Blocks brick block. Smelt the plain bricks in a furnace to get it. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Rissige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel im Ofen schmelzen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/cracked_end_stone_bricks.png",
      "craftedBy": [
        "simpleblocks:cracked_end_stone_bricks_from_smelting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:cracked_mud_bricks",
      "name": {
        "en_us": "Cracked Mud Bricks",
        "de_de": "Rissige Lehmziegel"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Cracked brick variant of a Vanilla or Simple Blocks brick block. Smelt the plain bricks in a furnace to get it. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Rissige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel im Ofen schmelzen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/cracked_mud_bricks.png",
      "craftedBy": [
        "simpleblocks:cracked_mud_bricks_from_smelting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:cracked_red_nether_bricks",
      "name": {
        "en_us": "Cracked Red Nether Bricks",
        "de_de": "Rissige Rote Netherziegel"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Cracked brick variant of a Vanilla or Simple Blocks brick block. Smelt the plain bricks in a furnace to get it. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Rissige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel im Ofen schmelzen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/cracked_red_nether_bricks.png",
      "craftedBy": [
        "simpleblocks:cracked_red_nether_bricks_from_smelting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:cracked_sulfur_bricks",
      "name": {
        "en_us": "Cracked Sulfur Bricks",
        "de_de": "Rissige Schwefelziegel"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Cracked brick variant of a Vanilla or Simple Blocks brick block. Smelt the plain bricks in a furnace to get it. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Rissige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel im Ofen schmelzen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/cracked_sulfur_bricks.png",
      "craftedBy": [
        "simpleblocks:cracked_sulfur_bricks_from_smelting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:cracked_tuff_bricks",
      "name": {
        "en_us": "Cracked Tuff Bricks",
        "de_de": "Rissige Tuffziegel"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Cracked brick variant of a Vanilla or Simple Blocks brick block. Smelt the plain bricks in a furnace to get it. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Rissige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel im Ofen schmelzen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/cracked_tuff_bricks.png",
      "craftedBy": [
        "simpleblocks:cracked_tuff_bricks_from_smelting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:cut_red_sandstone_stairs",
      "name": {
        "en_us": "Cut Red Sandstone Stairs",
        "de_de": "Geschnittene Rote Sandsteintreppe"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Stair variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Treppenvariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:cut_red_sandstone_stairs",
        "simpleblocks:cut_red_sandstone_stairs_from_cut_red_sandstone_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:cut_red_sandstone_wall",
      "name": {
        "en_us": "Cut Red Sandstone Wall",
        "de_de": "Geschnittene Rote Sandsteinmauer"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Wall variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Mauervariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:cut_red_sandstone_wall",
        "simpleblocks:cut_red_sandstone_wall_from_cut_red_sandstone_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:cut_sandstone_stairs",
      "name": {
        "en_us": "Cut Sandstone Stairs",
        "de_de": "Geschnittene Sandsteintreppe"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Stair variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Treppenvariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:cut_sandstone_stairs",
        "simpleblocks:cut_sandstone_stairs_from_cut_sandstone_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:cut_sandstone_wall",
      "name": {
        "en_us": "Cut Sandstone Wall",
        "de_de": "Geschnittene Sandsteinmauer"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Wall variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Mauervariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:cut_sandstone_wall",
        "simpleblocks:cut_sandstone_wall_from_cut_sandstone_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:hushed_deepslate_tile_slab",
      "name": {
        "en_us": "Hushed Deepslate Tile Slab",
        "de_de": "Gedämpfte Tiefenschieferfliesenstufe"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Slab variant of a Simple Blocks block, crafted in a row of three blocks of its material (six slabs); it also works in the stonecutter."
        },
        "de": {
          "summary": "Plattenvariante eines Simple-Blocks-Blocks, in einer Reihe aus drei Blöcken des Materials hergestellt (sechs Platten); auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:hushed_deepslate_tile_slab",
        "simpleblocks:hushed_deepslate_tile_slab_from_hushed_deepslate_tiles_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:hushed_deepslate_tile_stairs",
      "name": {
        "en_us": "Hushed Deepslate Tile Stairs",
        "de_de": "Gedämpfte Tiefenschieferfliesentreppe"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Stair variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Treppenvariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:hushed_deepslate_tile_stairs",
        "simpleblocks:hushed_deepslate_tile_stairs_from_hushed_deepslate_tiles_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:hushed_deepslate_tile_wall",
      "name": {
        "en_us": "Hushed Deepslate Tile Wall",
        "de_de": "Gedämpfte Tiefenschieferfliesenmauer"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Wall variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Mauervariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:hushed_deepslate_tile_wall",
        "simpleblocks:hushed_deepslate_tile_wall_from_hushed_deepslate_tiles_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:hushed_deepslate_tiles",
      "name": {
        "en_us": "Hushed Deepslate Tiles",
        "de_de": "Gedämpfte Tiefenschieferfliesen"
      },
      "note": {
        "en": {
          "title": "Hushed Deepslate Tiles",
          "summary": "Deepslate tiles that swallow vibrations like wool: a Sculk Sensor or Warden does not hear a vibration that passes through the block, and walking on it makes none.",
          "details": [
            "Recipe: four Deepslate Tiles and one Sculk make four Hushed Deepslate Tiles. Stairs, slab and wall come from the crafting table (6 / 6 / 6) or the stonecutter.",
            "The blocks are in the Vanilla block tags dampens_vibrations and occludes_vibration_signals, the same tags wool is in. Use them to build quiet floors and walls around Sculk Sensors and redstone that listens.",
            "They have the hardness, tool and sound of Deepslate Tiles."
          ]
        },
        "de": {
          "title": "Gedämpfte Tiefenschieferfliesen",
          "summary": "Tiefenschieferfliesen, die Vibrationen wie Wolle schlucken: Ein Sculk-Sensor oder Warden hört keine Vibration, die durch den Block geht, und Laufen darauf erzeugt keine.",
          "details": [
            "Rezept: vier Tiefenschieferfliesen und ein Sculk ergeben vier Gedämpfte Tiefenschieferfliesen. Treppe, Stufe und Mauer gibt es an der Werkbank (6 / 6 / 6) oder am Steinmetz.",
            "Die Blöcke stehen in den Vanilla-Block-Tags dampens_vibrations und occludes_vibration_signals, in denen auch Wolle steht. Damit baust du leise Böden und Wände um Sculk-Sensoren und lauschende Redstone-Schaltungen.",
            "Sie haben Härte, Werkzeug und Klang der Tiefenschieferfliesen."
          ]
        }
      },
      "texture": "assets/textures/simpleblocks/block/hushed_deepslate_tiles.png",
      "craftedBy": [
        "simpleblocks:hushed_deepslate_tiles"
      ],
      "usedIn": [
        "simpleblocks:hushed_deepslate_tile_slab",
        "simpleblocks:hushed_deepslate_tile_slab_from_hushed_deepslate_tiles_stonecutting",
        "simpleblocks:hushed_deepslate_tile_stairs",
        "simpleblocks:hushed_deepslate_tile_stairs_from_hushed_deepslate_tiles_stonecutting",
        "simpleblocks:hushed_deepslate_tile_wall",
        "simpleblocks:hushed_deepslate_tile_wall_from_hushed_deepslate_tiles_stonecutting"
      ]
    },
    {
      "id": "simpleblocks:mossy_cinnabar_bricks",
      "name": {
        "en_us": "Mossy Cinnabar Bricks",
        "de_de": "Bemooste Zinnoberziegel"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Mossy brick variant of a Vanilla or Simple Blocks brick block. Craft the plain bricks with a Moss Block. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Moosige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel mit einem Moosblock herstellen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/mossy_cinnabar_bricks.png",
      "craftedBy": [
        "simpleblocks:mossy_cinnabar_bricks_from_moss_block"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:mossy_deepslate_bricks",
      "name": {
        "en_us": "Mossy Deepslate Bricks",
        "de_de": "Bemooste Tiefenschieferziegel"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Mossy brick variant of a Vanilla or Simple Blocks brick block. Craft the plain bricks with a Moss Block. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Moosige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel mit einem Moosblock herstellen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/mossy_deepslate_bricks.png",
      "craftedBy": [
        "simpleblocks:mossy_deepslate_bricks_from_moss_block"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:mossy_deepslate_tiles",
      "name": {
        "en_us": "Mossy Deepslate Tiles",
        "de_de": "Bemooste Tiefenschieferfliesen"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Mossy brick variant of a Vanilla or Simple Blocks brick block. Craft the plain bricks with a Moss Block. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Moosige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel mit einem Moosblock herstellen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/mossy_deepslate_tiles.png",
      "craftedBy": [
        "simpleblocks:mossy_deepslate_tiles_from_moss_block"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:mossy_end_stone_bricks",
      "name": {
        "en_us": "Mossy End Stone Bricks",
        "de_de": "Bemooste Endsteinziegel"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Mossy brick variant of a Vanilla or Simple Blocks brick block. Craft the plain bricks with a Moss Block. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Moosige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel mit einem Moosblock herstellen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/mossy_end_stone_bricks.png",
      "craftedBy": [
        "simpleblocks:mossy_end_stone_bricks_from_moss_block"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:mossy_mud_bricks",
      "name": {
        "en_us": "Mossy Mud Bricks",
        "de_de": "Bemooste Lehmziegel"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Mossy brick variant of a Vanilla or Simple Blocks brick block. Craft the plain bricks with a Moss Block. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Moosige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel mit einem Moosblock herstellen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/mossy_mud_bricks.png",
      "craftedBy": [
        "simpleblocks:mossy_mud_bricks_from_moss_block"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:mossy_sulfur_bricks",
      "name": {
        "en_us": "Mossy Sulfur Bricks",
        "de_de": "Bemooste Schwefelziegel"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Mossy brick variant of a Vanilla or Simple Blocks brick block. Craft the plain bricks with a Moss Block. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Moosige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel mit einem Moosblock herstellen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/mossy_sulfur_bricks.png",
      "craftedBy": [
        "simpleblocks:mossy_sulfur_bricks_from_moss_block"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:mossy_tuff_bricks",
      "name": {
        "en_us": "Mossy Tuff Bricks",
        "de_de": "Bemooste Tuffziegel"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Mossy brick variant of a Vanilla or Simple Blocks brick block. Craft the plain bricks with a Moss Block. See the chapter on cracked and mossy bricks."
        },
        "de": {
          "summary": "Moosige Ziegelvariante eines Vanilla- oder Simple-Blocks-Ziegelblocks. Die glatten Ziegel mit einem Moosblock herstellen. Siehe das Kapitel zu rissigen und moosigen Ziegeln."
        }
      },
      "texture": "assets/textures/simpleblocks/block/mossy_tuff_bricks.png",
      "craftedBy": [
        "simpleblocks:mossy_tuff_bricks_from_moss_block"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:netherrack_slab",
      "name": {
        "en_us": "Netherrack Slab",
        "de_de": "Netherrack-Stufe"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Slab variant of a Simple Blocks block, crafted in a row of three blocks of its material (six slabs); it also works in the stonecutter."
        },
        "de": {
          "summary": "Plattenvariante eines Simple-Blocks-Blocks, in einer Reihe aus drei Blöcken des Materials hergestellt (sechs Platten); auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:netherrack_slab",
        "simpleblocks:netherrack_slab_from_netherrack_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:netherrack_stairs",
      "name": {
        "en_us": "Netherrack Stairs",
        "de_de": "Netherrack-Treppe"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Stair variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Treppenvariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:netherrack_stairs",
        "simpleblocks:netherrack_stairs_from_netherrack_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:netherrack_wall",
      "name": {
        "en_us": "Netherrack Wall",
        "de_de": "Netherrack-Mauer"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Wall variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Mauervariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:netherrack_wall",
        "simpleblocks:netherrack_wall_from_netherrack_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:sculk_deepslate",
      "name": {
        "en_us": "Sculk Deepslate",
        "de_de": "Sculk-Tiefenschiefer"
      },
      "note": {
        "en": {
          "title": "Sculk Deepslate",
          "summary": "Deepslate with sculk veins. It is not generated in the world: craft it or find it in Ancient City chests.",
          "details": [
            "Recipe (shapeless): one Deepslate and one Sculk make two Sculk Deepslate.",
            "Ancient City chests can hold two to six of them (one pool with weight 8 against 92 empty).",
            "Mined without Silk Touch it drops Cobbled Deepslate and gives 1 to 2 experience; with Silk Touch it drops itself and gives none."
          ]
        },
        "de": {
          "title": "Sculk-Tiefenschiefer",
          "summary": "Tiefenschiefer mit Sculk-Adern. Er wird nicht in der Welt erzeugt: herstellen oder in Truhen der Antiken Stadt finden.",
          "details": [
            "Rezept (formlos): ein Tiefenschiefer und ein Sculk ergeben zwei Sculk-Tiefenschiefer.",
            "Truhen der Antiken Stadt können zwei bis sechs davon enthalten (ein Pool mit Gewicht 8 gegen 92 leer).",
            "Ohne Behutsamkeit abgebaut droppt er Bruchtiefenschiefer und gibt 1 bis 2 Erfahrung; mit Behutsamkeit droppt er sich selbst und gibt keine."
          ]
        }
      },
      "texture": "assets/textures/simpleblocks/block/sculk_deepslate.png",
      "craftedBy": [
        "simpleblocks:sculk_deepslate"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:smooth_basalt_slab",
      "name": {
        "en_us": "Smooth Basalt Slab",
        "de_de": "Glatte Basaltstufe"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Slab variant of a Simple Blocks block, crafted in a row of three blocks of its material (six slabs); it also works in the stonecutter."
        },
        "de": {
          "summary": "Plattenvariante eines Simple-Blocks-Blocks, in einer Reihe aus drei Blöcken des Materials hergestellt (sechs Platten); auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:smooth_basalt_slab",
        "simpleblocks:smooth_basalt_slab_from_smooth_basalt_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:smooth_basalt_stairs",
      "name": {
        "en_us": "Smooth Basalt Stairs",
        "de_de": "Glatte Basalttreppe"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Stair variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Treppenvariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:smooth_basalt_stairs",
        "simpleblocks:smooth_basalt_stairs_from_smooth_basalt_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:smooth_basalt_wall",
      "name": {
        "en_us": "Smooth Basalt Wall",
        "de_de": "Glatte Basaltmauer"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Wall variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Mauervariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:smooth_basalt_wall",
        "simpleblocks:smooth_basalt_wall_from_smooth_basalt_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:smooth_red_sandstone_wall",
      "name": {
        "en_us": "Smooth Red Sandstone Wall",
        "de_de": "Glatte Rote Sandsteinmauer"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Wall variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Mauervariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:smooth_red_sandstone_wall",
        "simpleblocks:smooth_red_sandstone_wall_from_smooth_red_sandstone_stonecutting"
      ],
      "usedIn": []
    },
    {
      "id": "simpleblocks:smooth_sandstone_wall",
      "name": {
        "en_us": "Smooth Sandstone Wall",
        "de_de": "Glatte Sandsteinmauer"
      },
      "note": {
        "sources": [
          "modules/simpleblocks/shared/java/com/simpleblocks/BlocksRegistry.java",
          "modules/simpleblocks/shared/java/com/simpleblocks/test/BlocksTests.java"
        ],
        "en": {
          "summary": "Wall variant of a Simple Blocks block, crafted from six blocks of its material; it also works in the stonecutter."
        },
        "de": {
          "summary": "Mauervariante eines Simple-Blocks-Blocks, aus sechs Blöcken des Materials hergestellt; auch im Steinsägewerk nutzbar."
        }
      },
      "craftedBy": [
        "simpleblocks:smooth_sandstone_wall",
        "simpleblocks:smooth_sandstone_wall_from_smooth_sandstone_stonecutting"
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
    "features": 5,
    "recipes": 60,
    "lootTables": 37,
    "tags": 0,
    "advancements": 0,
    "enchantments": 0,
    "items": 1,
    "blocks": 37,
    "trades": 0,
    "config": 0,
    "quests": 0,
    "recipesOtherLines": 0,
    "undocumented": 0,
    "inWorld": 0,
    "incompleteProse": 0
  }
};
