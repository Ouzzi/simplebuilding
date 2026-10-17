window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simpleoctets"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simpleoctets",
    "name": "Simple Octets",
    "version": "1.0.0",
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
      "id": "octets",
      "sources": [
        "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
        "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
        "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
        "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
        "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java",
        "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetMaterials.java",
        "tools/textures/octets_2026_10_09.py",
        "tools/textures/octet_materials.json"
      ],
      "en": {
        "title": "Octets: half-block cubes of every material",
        "summary": "An octet is a cube of 0.5 x 0.5 x 0.5 blocks. Every wood (planks), every block with stairs and slabs and a set of building colors without stairs (terracotta, metal and mineral blocks, logs and stems, wool, concrete) has its own octet with the texture of its block; up to eight octets fill one block cell.",
        "details": [
          "Right-click places the octet a quarter block in front of the hit point: in the clicked corner of a block top or side, or right next to the clicked octet - in the same cell or the neighboring one. An occupied octet or a creature in the way refuses it.",
          "A cell holds up to 8 octets; a full cell (8/8) is a full cube and stays an octet cell. Placed into water, a cell is waterlogged until the eighth octet fills it; a full cell takes no water.",
          "Breaking a cell without sneaking takes the whole cell and drops every octet; sneaking breaks only the octet you look at. Sneak + right-click with an empty hand takes the clicked octet out into the hand. Explosions, pistons and other breaks without a player drop every octet.",
          "An octet item stacks to 99. Each cell has the hardness, sound, map color and light of its block; octets of materials that need a tool drop only with the right tool.",
          "Wood octets burn like their planks (not crimson and warped), wool octets like wool, log octets like logs; wood, Simple End log and bamboo mosaic octets are mined fastest with an axe, wool without a tool, all others with a pickaxe.",
          "The creative tab \"Simple Octets\" lists the wood octets, the melon slice and the material octets by group (stone, sandstone, bricks, deepslate and blackstone, tuff, prismarine, quartz and purpur, copper, metal and mineral blocks, logs and stems, wool, concrete, terracotta, blocks of other mods, other); with \"add items to Vanilla tabs\" each octet also sits right after the slab of its block (else after the block).",
          "The list of materials is generated (tools/textures/octets_2026_10_09.py from tools/textures/octet_materials.json)."
        ]
      },
      "de": {
        "title": "Achtel: halbe Blockwürfel aus jedem Material",
        "summary": "Ein Achtel ist ein Würfel von 0,5 x 0,5 x 0,5 Blöcken. Jedes Holz (Bretter), jeder Block mit Treppe und Stufe und eine Reihe Baufarben ohne Treppe (Terrakotta, Metall- und Mineralblöcke, Stämme und Stängel, Wolle, Beton) hat ein eigenes Achtel mit der Textur seines Blocks; bis zu acht Achtel füllen eine Blockzelle.",
        "details": [
          "Rechtsklick setzt das Achtel ein Viertel Block vor den Trefferpunkt: in die angeklickte Ecke einer Blockoberseite oder -seite oder direkt neben das angeklickte Achtel - in derselben Zelle oder der benachbarten. Ein belegtes Achtel oder ein Wesen im Weg lehnt ab.",
          "Eine Zelle fasst bis zu 8 Achtel; eine volle Zelle (8/8) ist ein voller Würfel und bleibt Achtelzelle. In Wasser gesetzt ist eine Zelle wassergefüllt, bis das achte Achtel sie füllt; eine volle Zelle nimmt kein Wasser.",
          "Abbauen ohne Schleichen nimmt die ganze Zelle und droppt jedes Achtel; schleichend baut man nur das angeschaute Achtel ab. Schleichen + Rechtsklick mit leerer Hand nimmt das angeklickte Achtel in die Hand. Explosionen, Kolben und anderes Abbauen ohne Spieler droppen jedes Achtel.",
          "Ein Achtel-Item stapelt bis 99. Jede Zelle hat Härte, Klang, Kartenfarbe und Licht ihres Blocks; Achtel aus Materialien, die ein Werkzeug brauchen, droppen nur mit dem passenden Werkzeug.",
          "Holz-Achtel brennen wie ihre Bretter (nicht Karmesin und Wirr), Woll-Achtel wie Wolle, Stamm-Achtel wie Stämme; Holz-, Simple-End-Stamm- und Bambusmosaik-Achtel baut die Axt am schnellsten ab, Wolle ohne Werkzeug, alle übrigen die Spitzhacke.",
          "Der Kreativ-Tab \"Simple Octets\" zeigt die Holz-Achtel, die Melonenscheibe und die Material-Achtel nach Gruppen (Stein, Sandstein, Ziegel, Tiefenschiefer und Schwarzstein, Tuff, Prismarin, Quarz und Purpur, Kupfer, Metall- und Mineralblöcke, Stämme und Stängel, Wolle, Beton, Terrakotta, Blöcke anderer Mods, Sonstiges); mit \"Items in Vanilla-Tabs\" steht jedes Achtel zusätzlich direkt hinter der Stufe seines Blocks (sonst hinter dem Block).",
          "Die Materialliste wird generiert (tools/textures/octets_2026_10_09.py aus tools/textures/octet_materials.json)."
        ]
      },
      "related": [
        "simpleoctets:*_octet",
        "simpleoctets:mixed_octet"
      ]
    },
    {
      "id": "recipes",
      "sources": [
        "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/oak_octet_from_oak_planks_stonecutting.json",
        "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/oak_planks_from_octets.json",
        "tools/textures/octets_2026_10_09.py"
      ],
      "en": {
        "title": "Stonecutter and back",
        "summary": "The stonecutter cuts 8 octets from the full block of their material (wood octets from the planks); 8 octets of one material craft back into the block (shapeless). Both recipes unlock in the recipe book once you hold the block or the octet.",
        "details": []
      },
      "de": {
        "title": "Steinmetz und zurück",
        "summary": "Der Steinmetz schneidet 8 Achtel aus dem Vollblock ihres Materials (Holz-Achtel aus den Brettern); 8 Achtel eines Materials ergeben formlos wieder den Block. Beide Rezepte schalten sich im Rezeptbuch frei, sobald man den Block bzw. das Achtel hält.",
        "details": []
      }
    },
    {
      "id": "mixed",
      "sources": [
        "modules/simpleoctets/shared/java/com/simpleoctets/block/MixedOctetBlock.java",
        "modules/simpleoctets/shared/java/com/simpleoctets/block/MixedOctetBlockEntity.java",
        "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
        "modules/simpleoctets/shared/java/com/simpleoctets/client/MixedOctetRenderer.java"
      ],
      "en": {
        "title": "Mixed cells",
        "summary": "Octets of different materials share one cell: the cell becomes the mixed octet cell, which keeps the material of each octet in its block entity, and turns back into a pure cell as soon as one material is left.",
        "details": [
          "A mixed cell glows with the light of its brightest material. Breaking it whole takes as long as its hardest material; sneaking, the looked-at octet sets speed, tool and drop.",
          "It has no item of its own: every octet drops as its own item. Blueprints, which store only block states, keep the most common material with every octet.",
          "(with SimpleBuilding) Its chess octets mix with these octets in the same cell."
        ]
      },
      "de": {
        "title": "Gemischte Zellen",
        "summary": "Achtel verschiedener Materialien teilen sich eine Zelle: sie wird zur gemischten Achtelzelle, die das Material jedes Achtels in ihrem Block-Entity hält, und wird wieder zur reinen Zelle, sobald nur ein Material übrig ist.",
        "details": [
          "Eine gemischte Zelle leuchtet wie ihr hellstes Material. Ganz abgebaut dauert sie so lange wie ihr härtestes Material; schleichend bestimmt das angeschaute Achtel Tempo, Werkzeug und Drop.",
          "Sie hat kein eigenes Item: jedes Achtel droppt als eigenes Item. Blaupausen, die nur Blockzustände speichern, behalten das häufigste Material mit allen Achteln.",
          "(mit SimpleBuilding) Dessen Schach-Achtel mischen sich mit diesen Achteln in derselben Zelle."
        ]
      }
    },
    {
      "id": "melon",
      "sources": [
        "modules/simpleoctets/shared/java/com/simpleoctets/MelonSlices.java",
        "modules/simpleoctets/shared/java/com/simpleoctets/mixin/MelonSliceUseMixin.java",
        "modules/simpleoctets/shared/java/com/simpleoctets/mixin/MelonSliceTooltipMixin.java",
        "tools/textures/octets_2026_10_09.py"
      ],
      "en": {
        "title": "Melon octets",
        "summary": "Sneak + right-click a block with a melon slice to place it as a melon octet (without sneaking it is eaten). The cell shows rind outside and flesh on the cut faces and drops one melon slice per octet.",
        "details": []
      },
      "de": {
        "title": "Melonen-Achtel",
        "summary": "Schleichen + Rechtsklick mit einer Melonenscheibe auf einen Block setzt sie als Melonen-Achtel (ohne Schleichen wird gegessen). Die Zelle zeigt außen Schale und an den Schnittflächen Fruchtfleisch und droppt je Achtel eine Melonenscheibe.",
        "details": []
      }
    },
    {
      "id": "partners",
      "sources": [
        "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java",
        "modules/simpleoctets/shared/java/com/simpleoctets/SimpleOctets.java",
        "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/ashwood_octet_from_ashwood_planks_stonecutting.json"
      ],
      "en": {
        "title": "Octets of other mods' blocks",
        "summary": "Octets of another mod's blocks exist only while that mod is loaded, with their recipes; without it neither the cells nor the recipes are there.",
        "details": [
          "(with SimpleBuilding) Octets of the Astralit, Nihilith and Ender Quartz palettes; its sneaking sledgehammer carves full blocks, stairs and slabs into these octets and drops the removed octet; its chess octets stay SimpleBuilding's and mix with these.",
          "(with Simple End) Ashwood and Duskwood octets (planks) and octets of their logs and stripped logs.",
          "(with Simple Blocks) Octets of Blue Nether Bricks and Hushed Deepslate Tiles.",
          "Carving tools of other mods find the cells through SimpleLib's OctetCells; Simple Octets registers every cell there."
        ]
      },
      "de": {
        "title": "Achtel aus Blöcken anderer Mods",
        "summary": "Achtel aus Blöcken einer anderen Mod gibt es nur, solange diese Mod geladen ist, samt ihren Rezepten; ohne sie fehlen Zellen und Rezepte.",
        "details": [
          "(mit SimpleBuilding) Achtel der Paletten Astralit, Nihilith und Enderquarz; sein schleichender Vorschlaghammer zerlegt Vollblöcke, Treppen und Stufen in diese Achtel und lässt das abgetragene Achtel fallen; seine Schach-Achtel bleiben bei SimpleBuilding und mischen sich mit diesen.",
          "(mit Simple End) Aschholz- und Dämmerholz-Achtel (Bretter) und Achtel ihrer Stämme und entrindeten Stämme.",
          "(mit Simple Blocks) Achtel aus Blauen Netherziegeln und Gedämpften Tiefenschieferfliesen.",
          "Schnitzwerkzeuge anderer Mods finden die Zellen über SimpleLibs OctetCells; Simple Octets meldet dort jede Zelle an."
        ]
      }
    },
    {
      "id": "migration",
      "sources": [
        "modules/simpleoctets/shared/java/com/simpleoctets/SimpleOctets.java"
      ],
      "en": {
        "title": "Worlds from SimpleBuilding",
        "summary": "The octets were SimpleBuilding's before split stage 6. Placed cells, octet items, the mixed cell and its block entity, the recipes in the recipe book and their unlocks keep working under the new simpleoctets: ids; the next save writes the new ids.",
        "details": [
          "\"Enable Simple Octets\" in SimpleBuilding's config switches the module's creative tab, its Vanilla tab entries, its guide and the melon octet placing off; placed octets and recipes stay."
        ]
      },
      "de": {
        "title": "Welten aus SimpleBuilding",
        "summary": "Die Achtel gehörten bis zur Aufteilungsstufe 6 zu SimpleBuilding. Gesetzte Zellen, Achtel-Items, die gemischte Zelle samt Block-Entity, die Rezepte im Rezeptbuch und ihre Freischaltungen funktionieren unter den neuen simpleoctets:-Ids weiter; das nächste Speichern schreibt die neuen Ids.",
        "details": [
          "\"Simple Octets aktivieren\" in der Config von SimpleBuilding schaltet Kreativ-Tab, Einträge in den Vanilla-Tabs, Handbuch und das Setzen von Melonen-Achteln des Moduls ab; gesetzte Achtel und Rezepte bleiben."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simpleoctets:acacia_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:acacia_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/acacia_log_from_octets.json",
      "ingredients": [
        "simpleoctets:acacia_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:acacia_log_octet"
        ],
        [
          "simpleoctets:acacia_log_octet"
        ],
        [
          "simpleoctets:acacia_log_octet"
        ],
        [
          "simpleoctets:acacia_log_octet"
        ],
        [
          "simpleoctets:acacia_log_octet"
        ],
        [
          "simpleoctets:acacia_log_octet"
        ],
        [
          "simpleoctets:acacia_log_octet"
        ],
        [
          "simpleoctets:acacia_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:acacia_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:acacia_log_octet_from_acacia_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:acacia_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/acacia_log_octet_from_acacia_log_stonecutting.json",
      "ingredients": [
        "minecraft:acacia_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:acacia_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:acacia_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:acacia_octet_from_acacia_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:acacia_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/acacia_octet_from_acacia_planks_stonecutting.json",
      "ingredients": [
        "minecraft:acacia_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:acacia_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:acacia_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleoctets:acacia_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:acacia_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/acacia_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:acacia_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:acacia_octet"
        ],
        [
          "simpleoctets:acacia_octet"
        ],
        [
          "simpleoctets:acacia_octet"
        ],
        [
          "simpleoctets:acacia_octet"
        ],
        [
          "simpleoctets:acacia_octet"
        ],
        [
          "simpleoctets:acacia_octet"
        ],
        [
          "simpleoctets:acacia_octet"
        ],
        [
          "simpleoctets:acacia_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:acacia_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:andesite_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:andesite",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/andesite_from_octets.json",
      "ingredients": [
        "simpleoctets:andesite_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:andesite_octet"
        ],
        [
          "simpleoctets:andesite_octet"
        ],
        [
          "simpleoctets:andesite_octet"
        ],
        [
          "simpleoctets:andesite_octet"
        ],
        [
          "simpleoctets:andesite_octet"
        ],
        [
          "simpleoctets:andesite_octet"
        ],
        [
          "simpleoctets:andesite_octet"
        ],
        [
          "simpleoctets:andesite_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:andesite_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:andesite_octet_from_andesite_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:andesite_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/andesite_octet_from_andesite_stonecutting.json",
      "ingredients": [
        "minecraft:andesite"
      ],
      "slots": {
        "ingredient": [
          "minecraft:andesite"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cobblestone",
            "count": 1
          },
          {
            "id": "minecraft:quartz",
            "count": 0.5
          }
        ]
      }
    },
    {
      "id": "simpleoctets:ashwood_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleend:ashwood_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/ashwood_log_from_octets.json",
      "ingredients": [
        "simpleoctets:ashwood_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:ashwood_log_octet"
        ],
        [
          "simpleoctets:ashwood_log_octet"
        ],
        [
          "simpleoctets:ashwood_log_octet"
        ],
        [
          "simpleoctets:ashwood_log_octet"
        ],
        [
          "simpleoctets:ashwood_log_octet"
        ],
        [
          "simpleoctets:ashwood_log_octet"
        ],
        [
          "simpleoctets:ashwood_log_octet"
        ],
        [
          "simpleoctets:ashwood_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:ashwood_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:ashwood_log_octet_from_ashwood_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:ashwood_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/ashwood_log_octet_from_ashwood_log_stonecutting.json",
      "ingredients": [
        "simpleend:ashwood_log"
      ],
      "slots": {
        "ingredient": [
          "simpleend:ashwood_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simpleend:ashwood_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:ashwood_octet_from_ashwood_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:ashwood_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/ashwood_octet_from_ashwood_planks_stonecutting.json",
      "ingredients": [
        "simpleend:ashwood_planks"
      ],
      "slots": {
        "ingredient": [
          "simpleend:ashwood_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simpleend:ashwood_planks",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:ashwood_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleend:ashwood_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/ashwood_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:ashwood_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:ashwood_octet"
        ],
        [
          "simpleoctets:ashwood_octet"
        ],
        [
          "simpleoctets:ashwood_octet"
        ],
        [
          "simpleoctets:ashwood_octet"
        ],
        [
          "simpleoctets:ashwood_octet"
        ],
        [
          "simpleoctets:ashwood_octet"
        ],
        [
          "simpleoctets:ashwood_octet"
        ],
        [
          "simpleoctets:ashwood_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:ashwood_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:astralit_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simplebuilding:astralit_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/astralit_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:astralit_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:astralit_bricks_octet"
        ],
        [
          "simpleoctets:astralit_bricks_octet"
        ],
        [
          "simpleoctets:astralit_bricks_octet"
        ],
        [
          "simpleoctets:astralit_bricks_octet"
        ],
        [
          "simpleoctets:astralit_bricks_octet"
        ],
        [
          "simpleoctets:astralit_bricks_octet"
        ],
        [
          "simpleoctets:astralit_bricks_octet"
        ],
        [
          "simpleoctets:astralit_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:astralit_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:astralit_bricks_octet_from_astralit_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:astralit_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/astralit_bricks_octet_from_astralit_bricks_stonecutting.json",
      "ingredients": [
        "simplebuilding:astralit_bricks"
      ],
      "slots": {
        "ingredient": [
          "simplebuilding:astralit_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simplebuilding:astralit_bricks",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:bamboo_mosaic_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:bamboo_mosaic",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/bamboo_mosaic_from_octets.json",
      "ingredients": [
        "simpleoctets:bamboo_mosaic_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:bamboo_mosaic_octet"
        ],
        [
          "simpleoctets:bamboo_mosaic_octet"
        ],
        [
          "simpleoctets:bamboo_mosaic_octet"
        ],
        [
          "simpleoctets:bamboo_mosaic_octet"
        ],
        [
          "simpleoctets:bamboo_mosaic_octet"
        ],
        [
          "simpleoctets:bamboo_mosaic_octet"
        ],
        [
          "simpleoctets:bamboo_mosaic_octet"
        ],
        [
          "simpleoctets:bamboo_mosaic_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:bamboo_mosaic_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:bamboo_mosaic_octet_from_bamboo_mosaic_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:bamboo_mosaic_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/bamboo_mosaic_octet_from_bamboo_mosaic_stonecutting.json",
      "ingredients": [
        "minecraft:bamboo_mosaic"
      ],
      "slots": {
        "ingredient": [
          "minecraft:bamboo_mosaic"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:bamboo",
            "count": 4.5
          }
        ]
      }
    },
    {
      "id": "simpleoctets:bamboo_octet_from_bamboo_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:bamboo_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/bamboo_octet_from_bamboo_planks_stonecutting.json",
      "ingredients": [
        "minecraft:bamboo_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:bamboo_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:bamboo",
            "count": 4.5
          }
        ]
      }
    },
    {
      "id": "simpleoctets:bamboo_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:bamboo_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/bamboo_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:bamboo_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:bamboo_octet"
        ],
        [
          "simpleoctets:bamboo_octet"
        ],
        [
          "simpleoctets:bamboo_octet"
        ],
        [
          "simpleoctets:bamboo_octet"
        ],
        [
          "simpleoctets:bamboo_octet"
        ],
        [
          "simpleoctets:bamboo_octet"
        ],
        [
          "simpleoctets:bamboo_octet"
        ],
        [
          "simpleoctets:bamboo_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:bamboo_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:birch_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:birch_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/birch_log_from_octets.json",
      "ingredients": [
        "simpleoctets:birch_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:birch_log_octet"
        ],
        [
          "simpleoctets:birch_log_octet"
        ],
        [
          "simpleoctets:birch_log_octet"
        ],
        [
          "simpleoctets:birch_log_octet"
        ],
        [
          "simpleoctets:birch_log_octet"
        ],
        [
          "simpleoctets:birch_log_octet"
        ],
        [
          "simpleoctets:birch_log_octet"
        ],
        [
          "simpleoctets:birch_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:birch_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:birch_log_octet_from_birch_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:birch_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/birch_log_octet_from_birch_log_stonecutting.json",
      "ingredients": [
        "minecraft:birch_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:birch_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:birch_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:birch_octet_from_birch_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:birch_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/birch_octet_from_birch_planks_stonecutting.json",
      "ingredients": [
        "minecraft:birch_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:birch_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:birch_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleoctets:birch_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:birch_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/birch_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:birch_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:birch_octet"
        ],
        [
          "simpleoctets:birch_octet"
        ],
        [
          "simpleoctets:birch_octet"
        ],
        [
          "simpleoctets:birch_octet"
        ],
        [
          "simpleoctets:birch_octet"
        ],
        [
          "simpleoctets:birch_octet"
        ],
        [
          "simpleoctets:birch_octet"
        ],
        [
          "simpleoctets:birch_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:birch_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:black_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:black_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/black_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:black_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:black_concrete_octet"
        ],
        [
          "simpleoctets:black_concrete_octet"
        ],
        [
          "simpleoctets:black_concrete_octet"
        ],
        [
          "simpleoctets:black_concrete_octet"
        ],
        [
          "simpleoctets:black_concrete_octet"
        ],
        [
          "simpleoctets:black_concrete_octet"
        ],
        [
          "simpleoctets:black_concrete_octet"
        ],
        [
          "simpleoctets:black_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:black_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:black_concrete_octet_from_black_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:black_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/black_concrete_octet_from_black_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:black_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:black_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:black_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:black_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:black_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/black_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:black_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:black_terracotta_octet"
        ],
        [
          "simpleoctets:black_terracotta_octet"
        ],
        [
          "simpleoctets:black_terracotta_octet"
        ],
        [
          "simpleoctets:black_terracotta_octet"
        ],
        [
          "simpleoctets:black_terracotta_octet"
        ],
        [
          "simpleoctets:black_terracotta_octet"
        ],
        [
          "simpleoctets:black_terracotta_octet"
        ],
        [
          "simpleoctets:black_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:black_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:black_terracotta_octet_from_black_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:black_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/black_terracotta_octet_from_black_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:black_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:black_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:ink_sac",
            "count": 0.125
          }
        ]
      }
    },
    {
      "id": "simpleoctets:black_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:black_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/black_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:black_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:black_wool_octet"
        ],
        [
          "simpleoctets:black_wool_octet"
        ],
        [
          "simpleoctets:black_wool_octet"
        ],
        [
          "simpleoctets:black_wool_octet"
        ],
        [
          "simpleoctets:black_wool_octet"
        ],
        [
          "simpleoctets:black_wool_octet"
        ],
        [
          "simpleoctets:black_wool_octet"
        ],
        [
          "simpleoctets:black_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:black_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:black_wool_octet_from_black_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:black_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/black_wool_octet_from_black_wool_stonecutting.json",
      "ingredients": [
        "minecraft:black_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:black_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:ink_sac",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:blackstone_block_octet_from_blackstone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:blackstone_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/blackstone_block_octet_from_blackstone_stonecutting.json",
      "ingredients": [
        "minecraft:blackstone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:blackstone"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:blackstone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:blackstone_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:blackstone",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/blackstone_from_octets.json",
      "ingredients": [
        "simpleoctets:blackstone_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:blackstone_block_octet"
        ],
        [
          "simpleoctets:blackstone_block_octet"
        ],
        [
          "simpleoctets:blackstone_block_octet"
        ],
        [
          "simpleoctets:blackstone_block_octet"
        ],
        [
          "simpleoctets:blackstone_block_octet"
        ],
        [
          "simpleoctets:blackstone_block_octet"
        ],
        [
          "simpleoctets:blackstone_block_octet"
        ],
        [
          "simpleoctets:blackstone_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:blackstone_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:blue_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:blue_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/blue_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:blue_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:blue_concrete_octet"
        ],
        [
          "simpleoctets:blue_concrete_octet"
        ],
        [
          "simpleoctets:blue_concrete_octet"
        ],
        [
          "simpleoctets:blue_concrete_octet"
        ],
        [
          "simpleoctets:blue_concrete_octet"
        ],
        [
          "simpleoctets:blue_concrete_octet"
        ],
        [
          "simpleoctets:blue_concrete_octet"
        ],
        [
          "simpleoctets:blue_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:blue_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:blue_concrete_octet_from_blue_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:blue_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/blue_concrete_octet_from_blue_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:blue_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:blue_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:blue_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:blue_nether_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:blue_nether_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/blue_nether_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:blue_nether_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:blue_nether_bricks_octet"
        ],
        [
          "simpleoctets:blue_nether_bricks_octet"
        ],
        [
          "simpleoctets:blue_nether_bricks_octet"
        ],
        [
          "simpleoctets:blue_nether_bricks_octet"
        ],
        [
          "simpleoctets:blue_nether_bricks_octet"
        ],
        [
          "simpleoctets:blue_nether_bricks_octet"
        ],
        [
          "simpleoctets:blue_nether_bricks_octet"
        ],
        [
          "simpleoctets:blue_nether_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:blue_nether_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:blue_nether_bricks_octet_from_blue_nether_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:blue_nether_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/blue_nether_bricks_octet_from_blue_nether_bricks_stonecutting.json",
      "ingredients": [
        "simpleblocks:blue_nether_bricks"
      ],
      "slots": {
        "ingredient": [
          "simpleblocks:blue_nether_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simpleblocks:blue_nether_bricks",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:blue_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:blue_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/blue_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:blue_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:blue_terracotta_octet"
        ],
        [
          "simpleoctets:blue_terracotta_octet"
        ],
        [
          "simpleoctets:blue_terracotta_octet"
        ],
        [
          "simpleoctets:blue_terracotta_octet"
        ],
        [
          "simpleoctets:blue_terracotta_octet"
        ],
        [
          "simpleoctets:blue_terracotta_octet"
        ],
        [
          "simpleoctets:blue_terracotta_octet"
        ],
        [
          "simpleoctets:blue_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:blue_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:blue_terracotta_octet_from_blue_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:blue_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/blue_terracotta_octet_from_blue_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:blue_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:blue_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:lapis_lazuli",
            "count": 0.125
          }
        ]
      }
    },
    {
      "id": "simpleoctets:blue_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:blue_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/blue_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:blue_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:blue_wool_octet"
        ],
        [
          "simpleoctets:blue_wool_octet"
        ],
        [
          "simpleoctets:blue_wool_octet"
        ],
        [
          "simpleoctets:blue_wool_octet"
        ],
        [
          "simpleoctets:blue_wool_octet"
        ],
        [
          "simpleoctets:blue_wool_octet"
        ],
        [
          "simpleoctets:blue_wool_octet"
        ],
        [
          "simpleoctets:blue_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:blue_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:blue_wool_octet_from_blue_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:blue_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/blue_wool_octet_from_blue_wool_stonecutting.json",
      "ingredients": [
        "minecraft:blue_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:blue_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:lapis_lazuli",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:bone_block_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:bone_block",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/bone_block_from_octets.json",
      "ingredients": [
        "simpleoctets:bone_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:bone_block_octet"
        ],
        [
          "simpleoctets:bone_block_octet"
        ],
        [
          "simpleoctets:bone_block_octet"
        ],
        [
          "simpleoctets:bone_block_octet"
        ],
        [
          "simpleoctets:bone_block_octet"
        ],
        [
          "simpleoctets:bone_block_octet"
        ],
        [
          "simpleoctets:bone_block_octet"
        ],
        [
          "simpleoctets:bone_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:bone_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:bone_block_octet_from_bone_block_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:bone_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/bone_block_octet_from_bone_block_stonecutting.json",
      "ingredients": [
        "minecraft:bone_block"
      ],
      "slots": {
        "ingredient": [
          "minecraft:bone_block"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:bone",
            "count": 3
          }
        ]
      }
    },
    {
      "id": "simpleoctets:bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:bricks_octet"
        ],
        [
          "simpleoctets:bricks_octet"
        ],
        [
          "simpleoctets:bricks_octet"
        ],
        [
          "simpleoctets:bricks_octet"
        ],
        [
          "simpleoctets:bricks_octet"
        ],
        [
          "simpleoctets:bricks_octet"
        ],
        [
          "simpleoctets:bricks_octet"
        ],
        [
          "simpleoctets:bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:bricks_octet_from_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/bricks_octet_from_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:brown_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:brown_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/brown_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:brown_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:brown_concrete_octet"
        ],
        [
          "simpleoctets:brown_concrete_octet"
        ],
        [
          "simpleoctets:brown_concrete_octet"
        ],
        [
          "simpleoctets:brown_concrete_octet"
        ],
        [
          "simpleoctets:brown_concrete_octet"
        ],
        [
          "simpleoctets:brown_concrete_octet"
        ],
        [
          "simpleoctets:brown_concrete_octet"
        ],
        [
          "simpleoctets:brown_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:brown_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:brown_concrete_octet_from_brown_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:brown_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/brown_concrete_octet_from_brown_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:brown_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:brown_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:brown_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:brown_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:brown_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/brown_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:brown_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:brown_terracotta_octet"
        ],
        [
          "simpleoctets:brown_terracotta_octet"
        ],
        [
          "simpleoctets:brown_terracotta_octet"
        ],
        [
          "simpleoctets:brown_terracotta_octet"
        ],
        [
          "simpleoctets:brown_terracotta_octet"
        ],
        [
          "simpleoctets:brown_terracotta_octet"
        ],
        [
          "simpleoctets:brown_terracotta_octet"
        ],
        [
          "simpleoctets:brown_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:brown_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:brown_terracotta_octet_from_brown_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:brown_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/brown_terracotta_octet_from_brown_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:brown_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:brown_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:cocoa_beans",
            "count": 0.125
          }
        ]
      }
    },
    {
      "id": "simpleoctets:brown_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:brown_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/brown_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:brown_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:brown_wool_octet"
        ],
        [
          "simpleoctets:brown_wool_octet"
        ],
        [
          "simpleoctets:brown_wool_octet"
        ],
        [
          "simpleoctets:brown_wool_octet"
        ],
        [
          "simpleoctets:brown_wool_octet"
        ],
        [
          "simpleoctets:brown_wool_octet"
        ],
        [
          "simpleoctets:brown_wool_octet"
        ],
        [
          "simpleoctets:brown_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:brown_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:brown_wool_octet_from_brown_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:brown_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/brown_wool_octet_from_brown_wool_stonecutting.json",
      "ingredients": [
        "minecraft:brown_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:brown_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:cocoa_beans",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:calcite_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:calcite",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/calcite_from_octets.json",
      "ingredients": [
        "simpleoctets:calcite_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:calcite_octet"
        ],
        [
          "simpleoctets:calcite_octet"
        ],
        [
          "simpleoctets:calcite_octet"
        ],
        [
          "simpleoctets:calcite_octet"
        ],
        [
          "simpleoctets:calcite_octet"
        ],
        [
          "simpleoctets:calcite_octet"
        ],
        [
          "simpleoctets:calcite_octet"
        ],
        [
          "simpleoctets:calcite_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:calcite_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:calcite_octet_from_calcite_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:calcite_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/calcite_octet_from_calcite_stonecutting.json",
      "ingredients": [
        "minecraft:calcite"
      ],
      "slots": {
        "ingredient": [
          "minecraft:calcite"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:calcite",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cherry_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:cherry_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cherry_log_from_octets.json",
      "ingredients": [
        "simpleoctets:cherry_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:cherry_log_octet"
        ],
        [
          "simpleoctets:cherry_log_octet"
        ],
        [
          "simpleoctets:cherry_log_octet"
        ],
        [
          "simpleoctets:cherry_log_octet"
        ],
        [
          "simpleoctets:cherry_log_octet"
        ],
        [
          "simpleoctets:cherry_log_octet"
        ],
        [
          "simpleoctets:cherry_log_octet"
        ],
        [
          "simpleoctets:cherry_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:cherry_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cherry_log_octet_from_cherry_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:cherry_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cherry_log_octet_from_cherry_log_stonecutting.json",
      "ingredients": [
        "minecraft:cherry_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cherry_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cherry_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cherry_octet_from_cherry_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:cherry_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cherry_octet_from_cherry_planks_stonecutting.json",
      "ingredients": [
        "minecraft:cherry_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cherry_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cherry_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cherry_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:cherry_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cherry_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:cherry_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:cherry_octet"
        ],
        [
          "simpleoctets:cherry_octet"
        ],
        [
          "simpleoctets:cherry_octet"
        ],
        [
          "simpleoctets:cherry_octet"
        ],
        [
          "simpleoctets:cherry_octet"
        ],
        [
          "simpleoctets:cherry_octet"
        ],
        [
          "simpleoctets:cherry_octet"
        ],
        [
          "simpleoctets:cherry_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:cherry_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cinnabar_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:cinnabar_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cinnabar_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:cinnabar_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:cinnabar_bricks_octet"
        ],
        [
          "simpleoctets:cinnabar_bricks_octet"
        ],
        [
          "simpleoctets:cinnabar_bricks_octet"
        ],
        [
          "simpleoctets:cinnabar_bricks_octet"
        ],
        [
          "simpleoctets:cinnabar_bricks_octet"
        ],
        [
          "simpleoctets:cinnabar_bricks_octet"
        ],
        [
          "simpleoctets:cinnabar_bricks_octet"
        ],
        [
          "simpleoctets:cinnabar_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:cinnabar_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cinnabar_bricks_octet_from_cinnabar_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:cinnabar_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cinnabar_bricks_octet_from_cinnabar_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:cinnabar_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cinnabar_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cinnabar",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cinnabar_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:cinnabar",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cinnabar_from_octets.json",
      "ingredients": [
        "simpleoctets:cinnabar_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:cinnabar_octet"
        ],
        [
          "simpleoctets:cinnabar_octet"
        ],
        [
          "simpleoctets:cinnabar_octet"
        ],
        [
          "simpleoctets:cinnabar_octet"
        ],
        [
          "simpleoctets:cinnabar_octet"
        ],
        [
          "simpleoctets:cinnabar_octet"
        ],
        [
          "simpleoctets:cinnabar_octet"
        ],
        [
          "simpleoctets:cinnabar_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:cinnabar_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cinnabar_octet_from_cinnabar_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:cinnabar_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cinnabar_octet_from_cinnabar_stonecutting.json",
      "ingredients": [
        "minecraft:cinnabar"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cinnabar"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cinnabar",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:coal_block_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:coal_block",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/coal_block_from_octets.json",
      "ingredients": [
        "simpleoctets:coal_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:coal_block_octet"
        ],
        [
          "simpleoctets:coal_block_octet"
        ],
        [
          "simpleoctets:coal_block_octet"
        ],
        [
          "simpleoctets:coal_block_octet"
        ],
        [
          "simpleoctets:coal_block_octet"
        ],
        [
          "simpleoctets:coal_block_octet"
        ],
        [
          "simpleoctets:coal_block_octet"
        ],
        [
          "simpleoctets:coal_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:coal_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:coal_block_octet_from_coal_block_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:coal_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/coal_block_octet_from_coal_block_stonecutting.json",
      "ingredients": [
        "minecraft:coal_block"
      ],
      "slots": {
        "ingredient": [
          "minecraft:coal_block"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:coal",
            "count": 9
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cobbled_deepslate_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:cobbled_deepslate",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cobbled_deepslate_from_octets.json",
      "ingredients": [
        "simpleoctets:cobbled_deepslate_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:cobbled_deepslate_octet"
        ],
        [
          "simpleoctets:cobbled_deepslate_octet"
        ],
        [
          "simpleoctets:cobbled_deepslate_octet"
        ],
        [
          "simpleoctets:cobbled_deepslate_octet"
        ],
        [
          "simpleoctets:cobbled_deepslate_octet"
        ],
        [
          "simpleoctets:cobbled_deepslate_octet"
        ],
        [
          "simpleoctets:cobbled_deepslate_octet"
        ],
        [
          "simpleoctets:cobbled_deepslate_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:cobbled_deepslate_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cobbled_deepslate_octet_from_cobbled_deepslate_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:cobbled_deepslate_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cobbled_deepslate_octet_from_cobbled_deepslate_stonecutting.json",
      "ingredients": [
        "minecraft:cobbled_deepslate"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cobbled_deepslate"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cobblestone_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:cobblestone",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cobblestone_from_octets.json",
      "ingredients": [
        "simpleoctets:cobblestone_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:cobblestone_octet"
        ],
        [
          "simpleoctets:cobblestone_octet"
        ],
        [
          "simpleoctets:cobblestone_octet"
        ],
        [
          "simpleoctets:cobblestone_octet"
        ],
        [
          "simpleoctets:cobblestone_octet"
        ],
        [
          "simpleoctets:cobblestone_octet"
        ],
        [
          "simpleoctets:cobblestone_octet"
        ],
        [
          "simpleoctets:cobblestone_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:cobblestone_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cobblestone_octet_from_cobblestone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:cobblestone_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cobblestone_octet_from_cobblestone_stonecutting.json",
      "ingredients": [
        "minecraft:cobblestone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cobblestone"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cobblestone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:copper_block_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:copper_block",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/copper_block_from_octets.json",
      "ingredients": [
        "simpleoctets:copper_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:copper_block_octet"
        ],
        [
          "simpleoctets:copper_block_octet"
        ],
        [
          "simpleoctets:copper_block_octet"
        ],
        [
          "simpleoctets:copper_block_octet"
        ],
        [
          "simpleoctets:copper_block_octet"
        ],
        [
          "simpleoctets:copper_block_octet"
        ],
        [
          "simpleoctets:copper_block_octet"
        ],
        [
          "simpleoctets:copper_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:copper_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:copper_block_octet_from_copper_block_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:copper_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/copper_block_octet_from_copper_block_stonecutting.json",
      "ingredients": [
        "minecraft:copper_block"
      ],
      "slots": {
        "ingredient": [
          "minecraft:copper_block"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:copper_ingot",
            "count": 9
          }
        ]
      }
    },
    {
      "id": "simpleoctets:crimson_octet_from_crimson_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:crimson_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/crimson_octet_from_crimson_planks_stonecutting.json",
      "ingredients": [
        "minecraft:crimson_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:crimson_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:crimson_stem",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleoctets:crimson_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:crimson_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/crimson_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:crimson_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:crimson_octet"
        ],
        [
          "simpleoctets:crimson_octet"
        ],
        [
          "simpleoctets:crimson_octet"
        ],
        [
          "simpleoctets:crimson_octet"
        ],
        [
          "simpleoctets:crimson_octet"
        ],
        [
          "simpleoctets:crimson_octet"
        ],
        [
          "simpleoctets:crimson_octet"
        ],
        [
          "simpleoctets:crimson_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:crimson_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:crimson_stem_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:crimson_stem",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/crimson_stem_from_octets.json",
      "ingredients": [
        "simpleoctets:crimson_stem_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:crimson_stem_octet"
        ],
        [
          "simpleoctets:crimson_stem_octet"
        ],
        [
          "simpleoctets:crimson_stem_octet"
        ],
        [
          "simpleoctets:crimson_stem_octet"
        ],
        [
          "simpleoctets:crimson_stem_octet"
        ],
        [
          "simpleoctets:crimson_stem_octet"
        ],
        [
          "simpleoctets:crimson_stem_octet"
        ],
        [
          "simpleoctets:crimson_stem_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:crimson_stem_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:crimson_stem_octet_from_crimson_stem_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:crimson_stem_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/crimson_stem_octet_from_crimson_stem_stonecutting.json",
      "ingredients": [
        "minecraft:crimson_stem"
      ],
      "slots": {
        "ingredient": [
          "minecraft:crimson_stem"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:crimson_stem",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cut_copper_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:cut_copper",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cut_copper_from_octets.json",
      "ingredients": [
        "simpleoctets:cut_copper_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:cut_copper_octet"
        ],
        [
          "simpleoctets:cut_copper_octet"
        ],
        [
          "simpleoctets:cut_copper_octet"
        ],
        [
          "simpleoctets:cut_copper_octet"
        ],
        [
          "simpleoctets:cut_copper_octet"
        ],
        [
          "simpleoctets:cut_copper_octet"
        ],
        [
          "simpleoctets:cut_copper_octet"
        ],
        [
          "simpleoctets:cut_copper_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:cut_copper_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cut_copper_octet_from_cut_copper_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:cut_copper_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cut_copper_octet_from_cut_copper_stonecutting.json",
      "ingredients": [
        "minecraft:cut_copper"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cut_copper"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:copper_ingot",
            "count": 9
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cyan_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:cyan_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cyan_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:cyan_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:cyan_concrete_octet"
        ],
        [
          "simpleoctets:cyan_concrete_octet"
        ],
        [
          "simpleoctets:cyan_concrete_octet"
        ],
        [
          "simpleoctets:cyan_concrete_octet"
        ],
        [
          "simpleoctets:cyan_concrete_octet"
        ],
        [
          "simpleoctets:cyan_concrete_octet"
        ],
        [
          "simpleoctets:cyan_concrete_octet"
        ],
        [
          "simpleoctets:cyan_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:cyan_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cyan_concrete_octet_from_cyan_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:cyan_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cyan_concrete_octet_from_cyan_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:cyan_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cyan_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cyan_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cyan_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:cyan_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cyan_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:cyan_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:cyan_terracotta_octet"
        ],
        [
          "simpleoctets:cyan_terracotta_octet"
        ],
        [
          "simpleoctets:cyan_terracotta_octet"
        ],
        [
          "simpleoctets:cyan_terracotta_octet"
        ],
        [
          "simpleoctets:cyan_terracotta_octet"
        ],
        [
          "simpleoctets:cyan_terracotta_octet"
        ],
        [
          "simpleoctets:cyan_terracotta_octet"
        ],
        [
          "simpleoctets:cyan_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:cyan_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cyan_terracotta_octet_from_cyan_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:cyan_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cyan_terracotta_octet_from_cyan_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:cyan_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cyan_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:cactus",
            "count": 0.062
          },
          {
            "id": "minecraft:lapis_lazuli",
            "count": 0.062
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cyan_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:cyan_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cyan_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:cyan_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:cyan_wool_octet"
        ],
        [
          "simpleoctets:cyan_wool_octet"
        ],
        [
          "simpleoctets:cyan_wool_octet"
        ],
        [
          "simpleoctets:cyan_wool_octet"
        ],
        [
          "simpleoctets:cyan_wool_octet"
        ],
        [
          "simpleoctets:cyan_wool_octet"
        ],
        [
          "simpleoctets:cyan_wool_octet"
        ],
        [
          "simpleoctets:cyan_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:cyan_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:cyan_wool_octet_from_cyan_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:cyan_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/cyan_wool_octet_from_cyan_wool_stonecutting.json",
      "ingredients": [
        "minecraft:cyan_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:cyan_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:cactus",
            "count": 0.5
          },
          {
            "id": "minecraft:lapis_lazuli",
            "count": 0.5
          }
        ]
      }
    },
    {
      "id": "simpleoctets:dark_oak_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:dark_oak_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/dark_oak_log_from_octets.json",
      "ingredients": [
        "simpleoctets:dark_oak_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:dark_oak_log_octet"
        ],
        [
          "simpleoctets:dark_oak_log_octet"
        ],
        [
          "simpleoctets:dark_oak_log_octet"
        ],
        [
          "simpleoctets:dark_oak_log_octet"
        ],
        [
          "simpleoctets:dark_oak_log_octet"
        ],
        [
          "simpleoctets:dark_oak_log_octet"
        ],
        [
          "simpleoctets:dark_oak_log_octet"
        ],
        [
          "simpleoctets:dark_oak_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:dark_oak_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:dark_oak_log_octet_from_dark_oak_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:dark_oak_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/dark_oak_log_octet_from_dark_oak_log_stonecutting.json",
      "ingredients": [
        "minecraft:dark_oak_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:dark_oak_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:dark_oak_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:dark_oak_octet_from_dark_oak_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:dark_oak_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/dark_oak_octet_from_dark_oak_planks_stonecutting.json",
      "ingredients": [
        "minecraft:dark_oak_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:dark_oak_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:dark_oak_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleoctets:dark_oak_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:dark_oak_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/dark_oak_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:dark_oak_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:dark_oak_octet"
        ],
        [
          "simpleoctets:dark_oak_octet"
        ],
        [
          "simpleoctets:dark_oak_octet"
        ],
        [
          "simpleoctets:dark_oak_octet"
        ],
        [
          "simpleoctets:dark_oak_octet"
        ],
        [
          "simpleoctets:dark_oak_octet"
        ],
        [
          "simpleoctets:dark_oak_octet"
        ],
        [
          "simpleoctets:dark_oak_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:dark_oak_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:dark_prismarine_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:dark_prismarine",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/dark_prismarine_from_octets.json",
      "ingredients": [
        "simpleoctets:dark_prismarine_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:dark_prismarine_octet"
        ],
        [
          "simpleoctets:dark_prismarine_octet"
        ],
        [
          "simpleoctets:dark_prismarine_octet"
        ],
        [
          "simpleoctets:dark_prismarine_octet"
        ],
        [
          "simpleoctets:dark_prismarine_octet"
        ],
        [
          "simpleoctets:dark_prismarine_octet"
        ],
        [
          "simpleoctets:dark_prismarine_octet"
        ],
        [
          "simpleoctets:dark_prismarine_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:dark_prismarine_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:dark_prismarine_octet_from_dark_prismarine_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:dark_prismarine_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/dark_prismarine_octet_from_dark_prismarine_stonecutting.json",
      "ingredients": [
        "minecraft:dark_prismarine"
      ],
      "slots": {
        "ingredient": [
          "minecraft:dark_prismarine"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:prismarine_shard",
            "count": 8
          },
          {
            "id": "minecraft:ink_sac",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:deepslate_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:deepslate_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/deepslate_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:deepslate_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:deepslate_bricks_octet"
        ],
        [
          "simpleoctets:deepslate_bricks_octet"
        ],
        [
          "simpleoctets:deepslate_bricks_octet"
        ],
        [
          "simpleoctets:deepslate_bricks_octet"
        ],
        [
          "simpleoctets:deepslate_bricks_octet"
        ],
        [
          "simpleoctets:deepslate_bricks_octet"
        ],
        [
          "simpleoctets:deepslate_bricks_octet"
        ],
        [
          "simpleoctets:deepslate_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:deepslate_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:deepslate_bricks_octet_from_deepslate_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:deepslate_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/deepslate_bricks_octet_from_deepslate_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:deepslate_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:deepslate_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:deepslate_tiles_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:deepslate_tiles",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/deepslate_tiles_from_octets.json",
      "ingredients": [
        "simpleoctets:deepslate_tiles_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:deepslate_tiles_octet"
        ],
        [
          "simpleoctets:deepslate_tiles_octet"
        ],
        [
          "simpleoctets:deepslate_tiles_octet"
        ],
        [
          "simpleoctets:deepslate_tiles_octet"
        ],
        [
          "simpleoctets:deepslate_tiles_octet"
        ],
        [
          "simpleoctets:deepslate_tiles_octet"
        ],
        [
          "simpleoctets:deepslate_tiles_octet"
        ],
        [
          "simpleoctets:deepslate_tiles_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:deepslate_tiles_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:deepslate_tiles_octet_from_deepslate_tiles_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:deepslate_tiles_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/deepslate_tiles_octet_from_deepslate_tiles_stonecutting.json",
      "ingredients": [
        "minecraft:deepslate_tiles"
      ],
      "slots": {
        "ingredient": [
          "minecraft:deepslate_tiles"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:diamond_block_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:diamond_block",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/diamond_block_from_octets.json",
      "ingredients": [
        "simpleoctets:diamond_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:diamond_block_octet"
        ],
        [
          "simpleoctets:diamond_block_octet"
        ],
        [
          "simpleoctets:diamond_block_octet"
        ],
        [
          "simpleoctets:diamond_block_octet"
        ],
        [
          "simpleoctets:diamond_block_octet"
        ],
        [
          "simpleoctets:diamond_block_octet"
        ],
        [
          "simpleoctets:diamond_block_octet"
        ],
        [
          "simpleoctets:diamond_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:diamond_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:diamond_block_octet_from_diamond_block_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:diamond_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/diamond_block_octet_from_diamond_block_stonecutting.json",
      "ingredients": [
        "minecraft:diamond_block"
      ],
      "slots": {
        "ingredient": [
          "minecraft:diamond_block"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 9
          }
        ]
      }
    },
    {
      "id": "simpleoctets:diorite_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:diorite",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/diorite_from_octets.json",
      "ingredients": [
        "simpleoctets:diorite_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:diorite_octet"
        ],
        [
          "simpleoctets:diorite_octet"
        ],
        [
          "simpleoctets:diorite_octet"
        ],
        [
          "simpleoctets:diorite_octet"
        ],
        [
          "simpleoctets:diorite_octet"
        ],
        [
          "simpleoctets:diorite_octet"
        ],
        [
          "simpleoctets:diorite_octet"
        ],
        [
          "simpleoctets:diorite_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:diorite_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:diorite_octet_from_diorite_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:diorite_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/diorite_octet_from_diorite_stonecutting.json",
      "ingredients": [
        "minecraft:diorite"
      ],
      "slots": {
        "ingredient": [
          "minecraft:diorite"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cobblestone",
            "count": 1
          },
          {
            "id": "minecraft:quartz",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:duskwood_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleend:duskwood_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/duskwood_log_from_octets.json",
      "ingredients": [
        "simpleoctets:duskwood_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:duskwood_log_octet"
        ],
        [
          "simpleoctets:duskwood_log_octet"
        ],
        [
          "simpleoctets:duskwood_log_octet"
        ],
        [
          "simpleoctets:duskwood_log_octet"
        ],
        [
          "simpleoctets:duskwood_log_octet"
        ],
        [
          "simpleoctets:duskwood_log_octet"
        ],
        [
          "simpleoctets:duskwood_log_octet"
        ],
        [
          "simpleoctets:duskwood_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:duskwood_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:duskwood_log_octet_from_duskwood_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:duskwood_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/duskwood_log_octet_from_duskwood_log_stonecutting.json",
      "ingredients": [
        "simpleend:duskwood_log"
      ],
      "slots": {
        "ingredient": [
          "simpleend:duskwood_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simpleend:duskwood_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:duskwood_octet_from_duskwood_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:duskwood_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/duskwood_octet_from_duskwood_planks_stonecutting.json",
      "ingredients": [
        "simpleend:duskwood_planks"
      ],
      "slots": {
        "ingredient": [
          "simpleend:duskwood_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simpleend:duskwood_planks",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:duskwood_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleend:duskwood_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/duskwood_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:duskwood_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:duskwood_octet"
        ],
        [
          "simpleoctets:duskwood_octet"
        ],
        [
          "simpleoctets:duskwood_octet"
        ],
        [
          "simpleoctets:duskwood_octet"
        ],
        [
          "simpleoctets:duskwood_octet"
        ],
        [
          "simpleoctets:duskwood_octet"
        ],
        [
          "simpleoctets:duskwood_octet"
        ],
        [
          "simpleoctets:duskwood_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:duskwood_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:emerald_block_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:emerald_block",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/emerald_block_from_octets.json",
      "ingredients": [
        "simpleoctets:emerald_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:emerald_block_octet"
        ],
        [
          "simpleoctets:emerald_block_octet"
        ],
        [
          "simpleoctets:emerald_block_octet"
        ],
        [
          "simpleoctets:emerald_block_octet"
        ],
        [
          "simpleoctets:emerald_block_octet"
        ],
        [
          "simpleoctets:emerald_block_octet"
        ],
        [
          "simpleoctets:emerald_block_octet"
        ],
        [
          "simpleoctets:emerald_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:emerald_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:emerald_block_octet_from_emerald_block_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:emerald_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/emerald_block_octet_from_emerald_block_stonecutting.json",
      "ingredients": [
        "minecraft:emerald_block"
      ],
      "slots": {
        "ingredient": [
          "minecraft:emerald_block"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:emerald",
            "count": 9
          }
        ]
      }
    },
    {
      "id": "simpleoctets:end_stone_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:end_stone_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/end_stone_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:end_stone_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:end_stone_bricks_octet"
        ],
        [
          "simpleoctets:end_stone_bricks_octet"
        ],
        [
          "simpleoctets:end_stone_bricks_octet"
        ],
        [
          "simpleoctets:end_stone_bricks_octet"
        ],
        [
          "simpleoctets:end_stone_bricks_octet"
        ],
        [
          "simpleoctets:end_stone_bricks_octet"
        ],
        [
          "simpleoctets:end_stone_bricks_octet"
        ],
        [
          "simpleoctets:end_stone_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:end_stone_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:end_stone_bricks_octet_from_end_stone_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:end_stone_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/end_stone_bricks_octet_from_end_stone_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:end_stone_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:end_stone_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:end_stone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:end_stone_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:end_stone",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/end_stone_from_octets.json",
      "ingredients": [
        "simpleoctets:end_stone_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:end_stone_octet"
        ],
        [
          "simpleoctets:end_stone_octet"
        ],
        [
          "simpleoctets:end_stone_octet"
        ],
        [
          "simpleoctets:end_stone_octet"
        ],
        [
          "simpleoctets:end_stone_octet"
        ],
        [
          "simpleoctets:end_stone_octet"
        ],
        [
          "simpleoctets:end_stone_octet"
        ],
        [
          "simpleoctets:end_stone_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:end_stone_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:end_stone_octet_from_end_stone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:end_stone_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/end_stone_octet_from_end_stone_stonecutting.json",
      "ingredients": [
        "minecraft:end_stone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:end_stone"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:end_stone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:ender_quartz_block_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simplebuilding:ender_quartz_block",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/ender_quartz_block_from_octets.json",
      "ingredients": [
        "simpleoctets:ender_quartz_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:ender_quartz_block_octet"
        ],
        [
          "simpleoctets:ender_quartz_block_octet"
        ],
        [
          "simpleoctets:ender_quartz_block_octet"
        ],
        [
          "simpleoctets:ender_quartz_block_octet"
        ],
        [
          "simpleoctets:ender_quartz_block_octet"
        ],
        [
          "simpleoctets:ender_quartz_block_octet"
        ],
        [
          "simpleoctets:ender_quartz_block_octet"
        ],
        [
          "simpleoctets:ender_quartz_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:ender_quartz_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:ender_quartz_block_octet_from_ender_quartz_block_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:ender_quartz_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/ender_quartz_block_octet_from_ender_quartz_block_stonecutting.json",
      "ingredients": [
        "simplebuilding:ender_quartz_block"
      ],
      "slots": {
        "ingredient": [
          "simplebuilding:ender_quartz_block"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simplebuilding:ender_quartz_block",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:ender_quartz_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simplebuilding:ender_quartz_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/ender_quartz_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:ender_quartz_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:ender_quartz_bricks_octet"
        ],
        [
          "simpleoctets:ender_quartz_bricks_octet"
        ],
        [
          "simpleoctets:ender_quartz_bricks_octet"
        ],
        [
          "simpleoctets:ender_quartz_bricks_octet"
        ],
        [
          "simpleoctets:ender_quartz_bricks_octet"
        ],
        [
          "simpleoctets:ender_quartz_bricks_octet"
        ],
        [
          "simpleoctets:ender_quartz_bricks_octet"
        ],
        [
          "simpleoctets:ender_quartz_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:ender_quartz_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:ender_quartz_bricks_octet_from_ender_quartz_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:ender_quartz_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/ender_quartz_bricks_octet_from_ender_quartz_bricks_stonecutting.json",
      "ingredients": [
        "simplebuilding:ender_quartz_bricks"
      ],
      "slots": {
        "ingredient": [
          "simplebuilding:ender_quartz_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simplebuilding:ender_quartz_bricks",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:exposed_copper_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:exposed_copper",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/exposed_copper_from_octets.json",
      "ingredients": [
        "simpleoctets:exposed_copper_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:exposed_copper_octet"
        ],
        [
          "simpleoctets:exposed_copper_octet"
        ],
        [
          "simpleoctets:exposed_copper_octet"
        ],
        [
          "simpleoctets:exposed_copper_octet"
        ],
        [
          "simpleoctets:exposed_copper_octet"
        ],
        [
          "simpleoctets:exposed_copper_octet"
        ],
        [
          "simpleoctets:exposed_copper_octet"
        ],
        [
          "simpleoctets:exposed_copper_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:exposed_copper_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:exposed_copper_octet_from_exposed_copper_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:exposed_copper_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/exposed_copper_octet_from_exposed_copper_stonecutting.json",
      "ingredients": [
        "minecraft:exposed_copper"
      ],
      "slots": {
        "ingredient": [
          "minecraft:exposed_copper"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:exposed_copper",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:exposed_cut_copper_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:exposed_cut_copper",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/exposed_cut_copper_from_octets.json",
      "ingredients": [
        "simpleoctets:exposed_cut_copper_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:exposed_cut_copper_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:exposed_cut_copper_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:exposed_cut_copper_octet_from_exposed_cut_copper_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:exposed_cut_copper_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/exposed_cut_copper_octet_from_exposed_cut_copper_stonecutting.json",
      "ingredients": [
        "minecraft:exposed_cut_copper"
      ],
      "slots": {
        "ingredient": [
          "minecraft:exposed_cut_copper"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:exposed_copper",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:gold_block_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:gold_block",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/gold_block_from_octets.json",
      "ingredients": [
        "simpleoctets:gold_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:gold_block_octet"
        ],
        [
          "simpleoctets:gold_block_octet"
        ],
        [
          "simpleoctets:gold_block_octet"
        ],
        [
          "simpleoctets:gold_block_octet"
        ],
        [
          "simpleoctets:gold_block_octet"
        ],
        [
          "simpleoctets:gold_block_octet"
        ],
        [
          "simpleoctets:gold_block_octet"
        ],
        [
          "simpleoctets:gold_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:gold_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:gold_block_octet_from_gold_block_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:gold_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/gold_block_octet_from_gold_block_stonecutting.json",
      "ingredients": [
        "minecraft:gold_block"
      ],
      "slots": {
        "ingredient": [
          "minecraft:gold_block"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:gold_ingot",
            "count": 9
          }
        ]
      }
    },
    {
      "id": "simpleoctets:granite_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:granite",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/granite_from_octets.json",
      "ingredients": [
        "simpleoctets:granite_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:granite_octet"
        ],
        [
          "simpleoctets:granite_octet"
        ],
        [
          "simpleoctets:granite_octet"
        ],
        [
          "simpleoctets:granite_octet"
        ],
        [
          "simpleoctets:granite_octet"
        ],
        [
          "simpleoctets:granite_octet"
        ],
        [
          "simpleoctets:granite_octet"
        ],
        [
          "simpleoctets:granite_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:granite_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:granite_octet_from_granite_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:granite_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/granite_octet_from_granite_stonecutting.json",
      "ingredients": [
        "minecraft:granite"
      ],
      "slots": {
        "ingredient": [
          "minecraft:granite"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:quartz",
            "count": 2
          },
          {
            "id": "minecraft:cobblestone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:gray_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:gray_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/gray_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:gray_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:gray_concrete_octet"
        ],
        [
          "simpleoctets:gray_concrete_octet"
        ],
        [
          "simpleoctets:gray_concrete_octet"
        ],
        [
          "simpleoctets:gray_concrete_octet"
        ],
        [
          "simpleoctets:gray_concrete_octet"
        ],
        [
          "simpleoctets:gray_concrete_octet"
        ],
        [
          "simpleoctets:gray_concrete_octet"
        ],
        [
          "simpleoctets:gray_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:gray_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:gray_concrete_octet_from_gray_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:gray_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/gray_concrete_octet_from_gray_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:gray_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:gray_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:gray_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:gray_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:gray_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/gray_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:gray_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:gray_terracotta_octet"
        ],
        [
          "simpleoctets:gray_terracotta_octet"
        ],
        [
          "simpleoctets:gray_terracotta_octet"
        ],
        [
          "simpleoctets:gray_terracotta_octet"
        ],
        [
          "simpleoctets:gray_terracotta_octet"
        ],
        [
          "simpleoctets:gray_terracotta_octet"
        ],
        [
          "simpleoctets:gray_terracotta_octet"
        ],
        [
          "simpleoctets:gray_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:gray_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:gray_terracotta_octet_from_gray_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:gray_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/gray_terracotta_octet_from_gray_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:gray_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:gray_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:ink_sac",
            "count": 0.062
          },
          {
            "id": "minecraft:bone",
            "count": 0.021
          }
        ]
      }
    },
    {
      "id": "simpleoctets:gray_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:gray_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/gray_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:gray_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:gray_wool_octet"
        ],
        [
          "simpleoctets:gray_wool_octet"
        ],
        [
          "simpleoctets:gray_wool_octet"
        ],
        [
          "simpleoctets:gray_wool_octet"
        ],
        [
          "simpleoctets:gray_wool_octet"
        ],
        [
          "simpleoctets:gray_wool_octet"
        ],
        [
          "simpleoctets:gray_wool_octet"
        ],
        [
          "simpleoctets:gray_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:gray_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:gray_wool_octet_from_gray_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:gray_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/gray_wool_octet_from_gray_wool_stonecutting.json",
      "ingredients": [
        "minecraft:gray_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:gray_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:ink_sac",
            "count": 0.5
          },
          {
            "id": "minecraft:bone",
            "count": 0.167
          }
        ]
      }
    },
    {
      "id": "simpleoctets:green_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:green_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/green_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:green_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:green_concrete_octet"
        ],
        [
          "simpleoctets:green_concrete_octet"
        ],
        [
          "simpleoctets:green_concrete_octet"
        ],
        [
          "simpleoctets:green_concrete_octet"
        ],
        [
          "simpleoctets:green_concrete_octet"
        ],
        [
          "simpleoctets:green_concrete_octet"
        ],
        [
          "simpleoctets:green_concrete_octet"
        ],
        [
          "simpleoctets:green_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:green_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:green_concrete_octet_from_green_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:green_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/green_concrete_octet_from_green_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:green_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:green_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:green_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:green_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:green_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/green_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:green_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:green_terracotta_octet"
        ],
        [
          "simpleoctets:green_terracotta_octet"
        ],
        [
          "simpleoctets:green_terracotta_octet"
        ],
        [
          "simpleoctets:green_terracotta_octet"
        ],
        [
          "simpleoctets:green_terracotta_octet"
        ],
        [
          "simpleoctets:green_terracotta_octet"
        ],
        [
          "simpleoctets:green_terracotta_octet"
        ],
        [
          "simpleoctets:green_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:green_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:green_terracotta_octet_from_green_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:green_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/green_terracotta_octet_from_green_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:green_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:green_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:cactus",
            "count": 0.125
          }
        ]
      }
    },
    {
      "id": "simpleoctets:green_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:green_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/green_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:green_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:green_wool_octet"
        ],
        [
          "simpleoctets:green_wool_octet"
        ],
        [
          "simpleoctets:green_wool_octet"
        ],
        [
          "simpleoctets:green_wool_octet"
        ],
        [
          "simpleoctets:green_wool_octet"
        ],
        [
          "simpleoctets:green_wool_octet"
        ],
        [
          "simpleoctets:green_wool_octet"
        ],
        [
          "simpleoctets:green_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:green_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:green_wool_octet_from_green_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:green_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/green_wool_octet_from_green_wool_stonecutting.json",
      "ingredients": [
        "minecraft:green_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:green_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:cactus",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpleoctets:guide_book",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:stonecutter"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:stonecutter"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:stone",
            "count": 3
          },
          {
            "id": "minecraft:sugar_cane",
            "count": 3
          },
          {
            "id": "minecraft:iron_ingot",
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
      "id": "simpleoctets:hushed_deepslate_tiles_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleblocks:hushed_deepslate_tiles",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/hushed_deepslate_tiles_from_octets.json",
      "ingredients": [
        "simpleoctets:hushed_deepslate_tiles_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:hushed_deepslate_tiles_octet"
        ],
        [
          "simpleoctets:hushed_deepslate_tiles_octet"
        ],
        [
          "simpleoctets:hushed_deepslate_tiles_octet"
        ],
        [
          "simpleoctets:hushed_deepslate_tiles_octet"
        ],
        [
          "simpleoctets:hushed_deepslate_tiles_octet"
        ],
        [
          "simpleoctets:hushed_deepslate_tiles_octet"
        ],
        [
          "simpleoctets:hushed_deepslate_tiles_octet"
        ],
        [
          "simpleoctets:hushed_deepslate_tiles_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:hushed_deepslate_tiles_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:hushed_deepslate_tiles_octet_from_hushed_deepslate_tiles_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:hushed_deepslate_tiles_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/hushed_deepslate_tiles_octet_from_hushed_deepslate_tiles_stonecutting.json",
      "ingredients": [
        "simpleblocks:hushed_deepslate_tiles"
      ],
      "slots": {
        "ingredient": [
          "simpleblocks:hushed_deepslate_tiles"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simpleblocks:hushed_deepslate_tiles",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:iron_block_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:iron_block",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/iron_block_from_octets.json",
      "ingredients": [
        "simpleoctets:iron_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:iron_block_octet"
        ],
        [
          "simpleoctets:iron_block_octet"
        ],
        [
          "simpleoctets:iron_block_octet"
        ],
        [
          "simpleoctets:iron_block_octet"
        ],
        [
          "simpleoctets:iron_block_octet"
        ],
        [
          "simpleoctets:iron_block_octet"
        ],
        [
          "simpleoctets:iron_block_octet"
        ],
        [
          "simpleoctets:iron_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:iron_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:iron_block_octet_from_iron_block_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:iron_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/iron_block_octet_from_iron_block_stonecutting.json",
      "ingredients": [
        "minecraft:iron_block"
      ],
      "slots": {
        "ingredient": [
          "minecraft:iron_block"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:iron_ingot",
            "count": 9
          }
        ]
      }
    },
    {
      "id": "simpleoctets:jungle_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:jungle_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/jungle_log_from_octets.json",
      "ingredients": [
        "simpleoctets:jungle_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:jungle_log_octet"
        ],
        [
          "simpleoctets:jungle_log_octet"
        ],
        [
          "simpleoctets:jungle_log_octet"
        ],
        [
          "simpleoctets:jungle_log_octet"
        ],
        [
          "simpleoctets:jungle_log_octet"
        ],
        [
          "simpleoctets:jungle_log_octet"
        ],
        [
          "simpleoctets:jungle_log_octet"
        ],
        [
          "simpleoctets:jungle_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:jungle_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:jungle_log_octet_from_jungle_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:jungle_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/jungle_log_octet_from_jungle_log_stonecutting.json",
      "ingredients": [
        "minecraft:jungle_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:jungle_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:jungle_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:jungle_octet_from_jungle_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:jungle_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/jungle_octet_from_jungle_planks_stonecutting.json",
      "ingredients": [
        "minecraft:jungle_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:jungle_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:jungle_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleoctets:jungle_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:jungle_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/jungle_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:jungle_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:jungle_octet"
        ],
        [
          "simpleoctets:jungle_octet"
        ],
        [
          "simpleoctets:jungle_octet"
        ],
        [
          "simpleoctets:jungle_octet"
        ],
        [
          "simpleoctets:jungle_octet"
        ],
        [
          "simpleoctets:jungle_octet"
        ],
        [
          "simpleoctets:jungle_octet"
        ],
        [
          "simpleoctets:jungle_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:jungle_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:lapis_block_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:lapis_block",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/lapis_block_from_octets.json",
      "ingredients": [
        "simpleoctets:lapis_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:lapis_block_octet"
        ],
        [
          "simpleoctets:lapis_block_octet"
        ],
        [
          "simpleoctets:lapis_block_octet"
        ],
        [
          "simpleoctets:lapis_block_octet"
        ],
        [
          "simpleoctets:lapis_block_octet"
        ],
        [
          "simpleoctets:lapis_block_octet"
        ],
        [
          "simpleoctets:lapis_block_octet"
        ],
        [
          "simpleoctets:lapis_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:lapis_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:lapis_block_octet_from_lapis_block_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:lapis_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/lapis_block_octet_from_lapis_block_stonecutting.json",
      "ingredients": [
        "minecraft:lapis_block"
      ],
      "slots": {
        "ingredient": [
          "minecraft:lapis_block"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:lapis_lazuli",
            "count": 9
          }
        ]
      }
    },
    {
      "id": "simpleoctets:light_blue_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:light_blue_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/light_blue_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:light_blue_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:light_blue_concrete_octet"
        ],
        [
          "simpleoctets:light_blue_concrete_octet"
        ],
        [
          "simpleoctets:light_blue_concrete_octet"
        ],
        [
          "simpleoctets:light_blue_concrete_octet"
        ],
        [
          "simpleoctets:light_blue_concrete_octet"
        ],
        [
          "simpleoctets:light_blue_concrete_octet"
        ],
        [
          "simpleoctets:light_blue_concrete_octet"
        ],
        [
          "simpleoctets:light_blue_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:light_blue_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:light_blue_concrete_octet_from_light_blue_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:light_blue_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/light_blue_concrete_octet_from_light_blue_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:light_blue_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:light_blue_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:light_blue_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:light_blue_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:light_blue_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/light_blue_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:light_blue_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:light_blue_terracotta_octet"
        ],
        [
          "simpleoctets:light_blue_terracotta_octet"
        ],
        [
          "simpleoctets:light_blue_terracotta_octet"
        ],
        [
          "simpleoctets:light_blue_terracotta_octet"
        ],
        [
          "simpleoctets:light_blue_terracotta_octet"
        ],
        [
          "simpleoctets:light_blue_terracotta_octet"
        ],
        [
          "simpleoctets:light_blue_terracotta_octet"
        ],
        [
          "simpleoctets:light_blue_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:light_blue_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:light_blue_terracotta_octet_from_light_blue_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:light_blue_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/light_blue_terracotta_octet_from_light_blue_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:light_blue_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:light_blue_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:blue_orchid",
            "count": 0.125
          }
        ]
      }
    },
    {
      "id": "simpleoctets:light_blue_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:light_blue_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/light_blue_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:light_blue_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:light_blue_wool_octet"
        ],
        [
          "simpleoctets:light_blue_wool_octet"
        ],
        [
          "simpleoctets:light_blue_wool_octet"
        ],
        [
          "simpleoctets:light_blue_wool_octet"
        ],
        [
          "simpleoctets:light_blue_wool_octet"
        ],
        [
          "simpleoctets:light_blue_wool_octet"
        ],
        [
          "simpleoctets:light_blue_wool_octet"
        ],
        [
          "simpleoctets:light_blue_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:light_blue_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:light_blue_wool_octet_from_light_blue_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:light_blue_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/light_blue_wool_octet_from_light_blue_wool_stonecutting.json",
      "ingredients": [
        "minecraft:light_blue_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:light_blue_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:blue_orchid",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:light_gray_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:light_gray_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/light_gray_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:light_gray_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:light_gray_concrete_octet"
        ],
        [
          "simpleoctets:light_gray_concrete_octet"
        ],
        [
          "simpleoctets:light_gray_concrete_octet"
        ],
        [
          "simpleoctets:light_gray_concrete_octet"
        ],
        [
          "simpleoctets:light_gray_concrete_octet"
        ],
        [
          "simpleoctets:light_gray_concrete_octet"
        ],
        [
          "simpleoctets:light_gray_concrete_octet"
        ],
        [
          "simpleoctets:light_gray_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:light_gray_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:light_gray_concrete_octet_from_light_gray_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:light_gray_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/light_gray_concrete_octet_from_light_gray_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:light_gray_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:light_gray_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:light_gray_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:light_gray_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:light_gray_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/light_gray_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:light_gray_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:light_gray_terracotta_octet"
        ],
        [
          "simpleoctets:light_gray_terracotta_octet"
        ],
        [
          "simpleoctets:light_gray_terracotta_octet"
        ],
        [
          "simpleoctets:light_gray_terracotta_octet"
        ],
        [
          "simpleoctets:light_gray_terracotta_octet"
        ],
        [
          "simpleoctets:light_gray_terracotta_octet"
        ],
        [
          "simpleoctets:light_gray_terracotta_octet"
        ],
        [
          "simpleoctets:light_gray_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:light_gray_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:light_gray_terracotta_octet_from_light_gray_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:light_gray_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/light_gray_terracotta_octet_from_light_gray_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:light_gray_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:light_gray_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:azure_bluet",
            "count": 0.125
          }
        ]
      }
    },
    {
      "id": "simpleoctets:light_gray_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:light_gray_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/light_gray_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:light_gray_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:light_gray_wool_octet"
        ],
        [
          "simpleoctets:light_gray_wool_octet"
        ],
        [
          "simpleoctets:light_gray_wool_octet"
        ],
        [
          "simpleoctets:light_gray_wool_octet"
        ],
        [
          "simpleoctets:light_gray_wool_octet"
        ],
        [
          "simpleoctets:light_gray_wool_octet"
        ],
        [
          "simpleoctets:light_gray_wool_octet"
        ],
        [
          "simpleoctets:light_gray_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:light_gray_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:light_gray_wool_octet_from_light_gray_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:light_gray_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/light_gray_wool_octet_from_light_gray_wool_stonecutting.json",
      "ingredients": [
        "minecraft:light_gray_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:light_gray_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:azure_bluet",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:lime_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:lime_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/lime_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:lime_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:lime_concrete_octet"
        ],
        [
          "simpleoctets:lime_concrete_octet"
        ],
        [
          "simpleoctets:lime_concrete_octet"
        ],
        [
          "simpleoctets:lime_concrete_octet"
        ],
        [
          "simpleoctets:lime_concrete_octet"
        ],
        [
          "simpleoctets:lime_concrete_octet"
        ],
        [
          "simpleoctets:lime_concrete_octet"
        ],
        [
          "simpleoctets:lime_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:lime_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:lime_concrete_octet_from_lime_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:lime_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/lime_concrete_octet_from_lime_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:lime_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:lime_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:lime_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:lime_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:lime_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/lime_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:lime_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:lime_terracotta_octet"
        ],
        [
          "simpleoctets:lime_terracotta_octet"
        ],
        [
          "simpleoctets:lime_terracotta_octet"
        ],
        [
          "simpleoctets:lime_terracotta_octet"
        ],
        [
          "simpleoctets:lime_terracotta_octet"
        ],
        [
          "simpleoctets:lime_terracotta_octet"
        ],
        [
          "simpleoctets:lime_terracotta_octet"
        ],
        [
          "simpleoctets:lime_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:lime_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:lime_terracotta_octet_from_lime_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:lime_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/lime_terracotta_octet_from_lime_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:lime_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:lime_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:cactus",
            "count": 0.062
          },
          {
            "id": "minecraft:bone",
            "count": 0.021
          }
        ]
      }
    },
    {
      "id": "simpleoctets:lime_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:lime_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/lime_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:lime_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:lime_wool_octet"
        ],
        [
          "simpleoctets:lime_wool_octet"
        ],
        [
          "simpleoctets:lime_wool_octet"
        ],
        [
          "simpleoctets:lime_wool_octet"
        ],
        [
          "simpleoctets:lime_wool_octet"
        ],
        [
          "simpleoctets:lime_wool_octet"
        ],
        [
          "simpleoctets:lime_wool_octet"
        ],
        [
          "simpleoctets:lime_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:lime_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:lime_wool_octet_from_lime_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:lime_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/lime_wool_octet_from_lime_wool_stonecutting.json",
      "ingredients": [
        "minecraft:lime_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:lime_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:cactus",
            "count": 0.5
          },
          {
            "id": "minecraft:bone",
            "count": 0.167
          }
        ]
      }
    },
    {
      "id": "simpleoctets:magenta_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:magenta_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/magenta_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:magenta_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:magenta_concrete_octet"
        ],
        [
          "simpleoctets:magenta_concrete_octet"
        ],
        [
          "simpleoctets:magenta_concrete_octet"
        ],
        [
          "simpleoctets:magenta_concrete_octet"
        ],
        [
          "simpleoctets:magenta_concrete_octet"
        ],
        [
          "simpleoctets:magenta_concrete_octet"
        ],
        [
          "simpleoctets:magenta_concrete_octet"
        ],
        [
          "simpleoctets:magenta_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:magenta_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:magenta_concrete_octet_from_magenta_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:magenta_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/magenta_concrete_octet_from_magenta_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:magenta_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:magenta_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:magenta_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:magenta_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:magenta_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/magenta_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:magenta_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:magenta_terracotta_octet"
        ],
        [
          "simpleoctets:magenta_terracotta_octet"
        ],
        [
          "simpleoctets:magenta_terracotta_octet"
        ],
        [
          "simpleoctets:magenta_terracotta_octet"
        ],
        [
          "simpleoctets:magenta_terracotta_octet"
        ],
        [
          "simpleoctets:magenta_terracotta_octet"
        ],
        [
          "simpleoctets:magenta_terracotta_octet"
        ],
        [
          "simpleoctets:magenta_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:magenta_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:magenta_terracotta_octet_from_magenta_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:magenta_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/magenta_terracotta_octet_from_magenta_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:magenta_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:magenta_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:allium",
            "count": 0.125
          }
        ]
      }
    },
    {
      "id": "simpleoctets:magenta_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:magenta_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/magenta_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:magenta_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:magenta_wool_octet"
        ],
        [
          "simpleoctets:magenta_wool_octet"
        ],
        [
          "simpleoctets:magenta_wool_octet"
        ],
        [
          "simpleoctets:magenta_wool_octet"
        ],
        [
          "simpleoctets:magenta_wool_octet"
        ],
        [
          "simpleoctets:magenta_wool_octet"
        ],
        [
          "simpleoctets:magenta_wool_octet"
        ],
        [
          "simpleoctets:magenta_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:magenta_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:magenta_wool_octet_from_magenta_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:magenta_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/magenta_wool_octet_from_magenta_wool_stonecutting.json",
      "ingredients": [
        "minecraft:magenta_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:magenta_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:allium",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:mangrove_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:mangrove_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/mangrove_log_from_octets.json",
      "ingredients": [
        "simpleoctets:mangrove_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:mangrove_log_octet"
        ],
        [
          "simpleoctets:mangrove_log_octet"
        ],
        [
          "simpleoctets:mangrove_log_octet"
        ],
        [
          "simpleoctets:mangrove_log_octet"
        ],
        [
          "simpleoctets:mangrove_log_octet"
        ],
        [
          "simpleoctets:mangrove_log_octet"
        ],
        [
          "simpleoctets:mangrove_log_octet"
        ],
        [
          "simpleoctets:mangrove_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:mangrove_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:mangrove_log_octet_from_mangrove_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:mangrove_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/mangrove_log_octet_from_mangrove_log_stonecutting.json",
      "ingredients": [
        "minecraft:mangrove_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:mangrove_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:mangrove_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:mangrove_octet_from_mangrove_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:mangrove_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/mangrove_octet_from_mangrove_planks_stonecutting.json",
      "ingredients": [
        "minecraft:mangrove_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:mangrove_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:mangrove_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleoctets:mangrove_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:mangrove_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/mangrove_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:mangrove_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:mangrove_octet"
        ],
        [
          "simpleoctets:mangrove_octet"
        ],
        [
          "simpleoctets:mangrove_octet"
        ],
        [
          "simpleoctets:mangrove_octet"
        ],
        [
          "simpleoctets:mangrove_octet"
        ],
        [
          "simpleoctets:mangrove_octet"
        ],
        [
          "simpleoctets:mangrove_octet"
        ],
        [
          "simpleoctets:mangrove_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:mangrove_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:mossy_cobblestone_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:mossy_cobblestone",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/mossy_cobblestone_from_octets.json",
      "ingredients": [
        "simpleoctets:mossy_cobblestone_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:mossy_cobblestone_octet"
        ],
        [
          "simpleoctets:mossy_cobblestone_octet"
        ],
        [
          "simpleoctets:mossy_cobblestone_octet"
        ],
        [
          "simpleoctets:mossy_cobblestone_octet"
        ],
        [
          "simpleoctets:mossy_cobblestone_octet"
        ],
        [
          "simpleoctets:mossy_cobblestone_octet"
        ],
        [
          "simpleoctets:mossy_cobblestone_octet"
        ],
        [
          "simpleoctets:mossy_cobblestone_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:mossy_cobblestone_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:mossy_cobblestone_octet_from_mossy_cobblestone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:mossy_cobblestone_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/mossy_cobblestone_octet_from_mossy_cobblestone_stonecutting.json",
      "ingredients": [
        "minecraft:mossy_cobblestone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:mossy_cobblestone"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cobblestone",
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
      "id": "simpleoctets:mossy_stone_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:mossy_stone_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/mossy_stone_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:mossy_stone_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:mossy_stone_bricks_octet"
        ],
        [
          "simpleoctets:mossy_stone_bricks_octet"
        ],
        [
          "simpleoctets:mossy_stone_bricks_octet"
        ],
        [
          "simpleoctets:mossy_stone_bricks_octet"
        ],
        [
          "simpleoctets:mossy_stone_bricks_octet"
        ],
        [
          "simpleoctets:mossy_stone_bricks_octet"
        ],
        [
          "simpleoctets:mossy_stone_bricks_octet"
        ],
        [
          "simpleoctets:mossy_stone_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:mossy_stone_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:mossy_stone_bricks_octet_from_mossy_stone_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:mossy_stone_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/mossy_stone_bricks_octet_from_mossy_stone_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:mossy_stone_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:mossy_stone_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:moss_block",
            "count": 1
          },
          {
            "id": "minecraft:stone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:mud_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:mud_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/mud_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:mud_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:mud_bricks_octet"
        ],
        [
          "simpleoctets:mud_bricks_octet"
        ],
        [
          "simpleoctets:mud_bricks_octet"
        ],
        [
          "simpleoctets:mud_bricks_octet"
        ],
        [
          "simpleoctets:mud_bricks_octet"
        ],
        [
          "simpleoctets:mud_bricks_octet"
        ],
        [
          "simpleoctets:mud_bricks_octet"
        ],
        [
          "simpleoctets:mud_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:mud_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:mud_bricks_octet_from_mud_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:mud_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/mud_bricks_octet_from_mud_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:mud_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:mud_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
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
      "id": "simpleoctets:nether_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:nether_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/nether_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:nether_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:nether_bricks_octet"
        ],
        [
          "simpleoctets:nether_bricks_octet"
        ],
        [
          "simpleoctets:nether_bricks_octet"
        ],
        [
          "simpleoctets:nether_bricks_octet"
        ],
        [
          "simpleoctets:nether_bricks_octet"
        ],
        [
          "simpleoctets:nether_bricks_octet"
        ],
        [
          "simpleoctets:nether_bricks_octet"
        ],
        [
          "simpleoctets:nether_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:nether_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:nether_bricks_octet_from_nether_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:nether_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/nether_bricks_octet_from_nether_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:nether_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:nether_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:netherite_block_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:netherite_block",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/netherite_block_from_octets.json",
      "ingredients": [
        "simpleoctets:netherite_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:netherite_block_octet"
        ],
        [
          "simpleoctets:netherite_block_octet"
        ],
        [
          "simpleoctets:netherite_block_octet"
        ],
        [
          "simpleoctets:netherite_block_octet"
        ],
        [
          "simpleoctets:netherite_block_octet"
        ],
        [
          "simpleoctets:netherite_block_octet"
        ],
        [
          "simpleoctets:netherite_block_octet"
        ],
        [
          "simpleoctets:netherite_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:netherite_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:netherite_block_octet_from_netherite_block_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:netherite_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/netherite_block_octet_from_netherite_block_stonecutting.json",
      "ingredients": [
        "minecraft:netherite_block"
      ],
      "slots": {
        "ingredient": [
          "minecraft:netherite_block"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:netherite_ingot",
            "count": 9
          }
        ]
      }
    },
    {
      "id": "simpleoctets:netherrack_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:netherrack",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/netherrack_from_octets.json",
      "ingredients": [
        "simpleoctets:netherrack_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:netherrack_octet"
        ],
        [
          "simpleoctets:netherrack_octet"
        ],
        [
          "simpleoctets:netherrack_octet"
        ],
        [
          "simpleoctets:netherrack_octet"
        ],
        [
          "simpleoctets:netherrack_octet"
        ],
        [
          "simpleoctets:netherrack_octet"
        ],
        [
          "simpleoctets:netherrack_octet"
        ],
        [
          "simpleoctets:netherrack_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:netherrack_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:netherrack_octet_from_netherrack_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:netherrack_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/netherrack_octet_from_netherrack_stonecutting.json",
      "ingredients": [
        "minecraft:netherrack"
      ],
      "slots": {
        "ingredient": [
          "minecraft:netherrack"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:netherrack",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:nihilith_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simplebuilding:nihilith_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/nihilith_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:nihilith_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:nihilith_bricks_octet"
        ],
        [
          "simpleoctets:nihilith_bricks_octet"
        ],
        [
          "simpleoctets:nihilith_bricks_octet"
        ],
        [
          "simpleoctets:nihilith_bricks_octet"
        ],
        [
          "simpleoctets:nihilith_bricks_octet"
        ],
        [
          "simpleoctets:nihilith_bricks_octet"
        ],
        [
          "simpleoctets:nihilith_bricks_octet"
        ],
        [
          "simpleoctets:nihilith_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:nihilith_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:nihilith_bricks_octet_from_nihilith_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:nihilith_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/nihilith_bricks_octet_from_nihilith_bricks_stonecutting.json",
      "ingredients": [
        "simplebuilding:nihilith_bricks"
      ],
      "slots": {
        "ingredient": [
          "simplebuilding:nihilith_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simplebuilding:nihilith_bricks",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:oak_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:oak_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/oak_log_from_octets.json",
      "ingredients": [
        "simpleoctets:oak_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:oak_log_octet"
        ],
        [
          "simpleoctets:oak_log_octet"
        ],
        [
          "simpleoctets:oak_log_octet"
        ],
        [
          "simpleoctets:oak_log_octet"
        ],
        [
          "simpleoctets:oak_log_octet"
        ],
        [
          "simpleoctets:oak_log_octet"
        ],
        [
          "simpleoctets:oak_log_octet"
        ],
        [
          "simpleoctets:oak_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:oak_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:oak_log_octet_from_oak_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:oak_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/oak_log_octet_from_oak_log_stonecutting.json",
      "ingredients": [
        "minecraft:oak_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:oak_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:oak_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:oak_octet_from_oak_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:oak_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/oak_octet_from_oak_planks_stonecutting.json",
      "ingredients": [
        "minecraft:oak_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:oak_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:oak_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleoctets:oak_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:oak_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/oak_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:oak_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:oak_octet"
        ],
        [
          "simpleoctets:oak_octet"
        ],
        [
          "simpleoctets:oak_octet"
        ],
        [
          "simpleoctets:oak_octet"
        ],
        [
          "simpleoctets:oak_octet"
        ],
        [
          "simpleoctets:oak_octet"
        ],
        [
          "simpleoctets:oak_octet"
        ],
        [
          "simpleoctets:oak_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:oak_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:obsidian_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:obsidian",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/obsidian_from_octets.json",
      "ingredients": [
        "simpleoctets:obsidian_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:obsidian_octet"
        ],
        [
          "simpleoctets:obsidian_octet"
        ],
        [
          "simpleoctets:obsidian_octet"
        ],
        [
          "simpleoctets:obsidian_octet"
        ],
        [
          "simpleoctets:obsidian_octet"
        ],
        [
          "simpleoctets:obsidian_octet"
        ],
        [
          "simpleoctets:obsidian_octet"
        ],
        [
          "simpleoctets:obsidian_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:obsidian_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:obsidian_octet_from_obsidian_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:obsidian_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/obsidian_octet_from_obsidian_stonecutting.json",
      "ingredients": [
        "minecraft:obsidian"
      ],
      "slots": {
        "ingredient": [
          "minecraft:obsidian"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:obsidian",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:orange_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:orange_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/orange_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:orange_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:orange_concrete_octet"
        ],
        [
          "simpleoctets:orange_concrete_octet"
        ],
        [
          "simpleoctets:orange_concrete_octet"
        ],
        [
          "simpleoctets:orange_concrete_octet"
        ],
        [
          "simpleoctets:orange_concrete_octet"
        ],
        [
          "simpleoctets:orange_concrete_octet"
        ],
        [
          "simpleoctets:orange_concrete_octet"
        ],
        [
          "simpleoctets:orange_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:orange_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:orange_concrete_octet_from_orange_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:orange_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/orange_concrete_octet_from_orange_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:orange_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:orange_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:orange_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:orange_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:orange_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/orange_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:orange_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:orange_terracotta_octet"
        ],
        [
          "simpleoctets:orange_terracotta_octet"
        ],
        [
          "simpleoctets:orange_terracotta_octet"
        ],
        [
          "simpleoctets:orange_terracotta_octet"
        ],
        [
          "simpleoctets:orange_terracotta_octet"
        ],
        [
          "simpleoctets:orange_terracotta_octet"
        ],
        [
          "simpleoctets:orange_terracotta_octet"
        ],
        [
          "simpleoctets:orange_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:orange_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:orange_terracotta_octet_from_orange_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:orange_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/orange_terracotta_octet_from_orange_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:orange_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:orange_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:open_eyeblossom",
            "count": 0.125
          }
        ]
      }
    },
    {
      "id": "simpleoctets:orange_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:orange_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/orange_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:orange_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:orange_wool_octet"
        ],
        [
          "simpleoctets:orange_wool_octet"
        ],
        [
          "simpleoctets:orange_wool_octet"
        ],
        [
          "simpleoctets:orange_wool_octet"
        ],
        [
          "simpleoctets:orange_wool_octet"
        ],
        [
          "simpleoctets:orange_wool_octet"
        ],
        [
          "simpleoctets:orange_wool_octet"
        ],
        [
          "simpleoctets:orange_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:orange_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:orange_wool_octet_from_orange_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:orange_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/orange_wool_octet_from_orange_wool_stonecutting.json",
      "ingredients": [
        "minecraft:orange_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:orange_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:open_eyeblossom",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:oxidized_copper_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:oxidized_copper",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/oxidized_copper_from_octets.json",
      "ingredients": [
        "simpleoctets:oxidized_copper_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:oxidized_copper_octet"
        ],
        [
          "simpleoctets:oxidized_copper_octet"
        ],
        [
          "simpleoctets:oxidized_copper_octet"
        ],
        [
          "simpleoctets:oxidized_copper_octet"
        ],
        [
          "simpleoctets:oxidized_copper_octet"
        ],
        [
          "simpleoctets:oxidized_copper_octet"
        ],
        [
          "simpleoctets:oxidized_copper_octet"
        ],
        [
          "simpleoctets:oxidized_copper_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:oxidized_copper_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:oxidized_copper_octet_from_oxidized_copper_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:oxidized_copper_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/oxidized_copper_octet_from_oxidized_copper_stonecutting.json",
      "ingredients": [
        "minecraft:oxidized_copper"
      ],
      "slots": {
        "ingredient": [
          "minecraft:oxidized_copper"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:oxidized_copper",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:oxidized_cut_copper_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:oxidized_cut_copper",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/oxidized_cut_copper_from_octets.json",
      "ingredients": [
        "simpleoctets:oxidized_cut_copper_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:oxidized_cut_copper_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:oxidized_cut_copper_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:oxidized_cut_copper_octet_from_oxidized_cut_copper_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:oxidized_cut_copper_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/oxidized_cut_copper_octet_from_oxidized_cut_copper_stonecutting.json",
      "ingredients": [
        "minecraft:oxidized_cut_copper"
      ],
      "slots": {
        "ingredient": [
          "minecraft:oxidized_cut_copper"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:oxidized_copper",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:pale_oak_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:pale_oak_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/pale_oak_log_from_octets.json",
      "ingredients": [
        "simpleoctets:pale_oak_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:pale_oak_log_octet"
        ],
        [
          "simpleoctets:pale_oak_log_octet"
        ],
        [
          "simpleoctets:pale_oak_log_octet"
        ],
        [
          "simpleoctets:pale_oak_log_octet"
        ],
        [
          "simpleoctets:pale_oak_log_octet"
        ],
        [
          "simpleoctets:pale_oak_log_octet"
        ],
        [
          "simpleoctets:pale_oak_log_octet"
        ],
        [
          "simpleoctets:pale_oak_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:pale_oak_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:pale_oak_log_octet_from_pale_oak_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:pale_oak_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/pale_oak_log_octet_from_pale_oak_log_stonecutting.json",
      "ingredients": [
        "minecraft:pale_oak_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:pale_oak_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:pale_oak_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:pale_oak_octet_from_pale_oak_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:pale_oak_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/pale_oak_octet_from_pale_oak_planks_stonecutting.json",
      "ingredients": [
        "minecraft:pale_oak_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:pale_oak_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:pale_oak_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleoctets:pale_oak_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:pale_oak_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/pale_oak_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:pale_oak_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:pale_oak_octet"
        ],
        [
          "simpleoctets:pale_oak_octet"
        ],
        [
          "simpleoctets:pale_oak_octet"
        ],
        [
          "simpleoctets:pale_oak_octet"
        ],
        [
          "simpleoctets:pale_oak_octet"
        ],
        [
          "simpleoctets:pale_oak_octet"
        ],
        [
          "simpleoctets:pale_oak_octet"
        ],
        [
          "simpleoctets:pale_oak_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:pale_oak_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:pink_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:pink_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/pink_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:pink_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:pink_concrete_octet"
        ],
        [
          "simpleoctets:pink_concrete_octet"
        ],
        [
          "simpleoctets:pink_concrete_octet"
        ],
        [
          "simpleoctets:pink_concrete_octet"
        ],
        [
          "simpleoctets:pink_concrete_octet"
        ],
        [
          "simpleoctets:pink_concrete_octet"
        ],
        [
          "simpleoctets:pink_concrete_octet"
        ],
        [
          "simpleoctets:pink_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:pink_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:pink_concrete_octet_from_pink_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:pink_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/pink_concrete_octet_from_pink_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:pink_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:pink_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:pink_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:pink_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:pink_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/pink_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:pink_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:pink_terracotta_octet"
        ],
        [
          "simpleoctets:pink_terracotta_octet"
        ],
        [
          "simpleoctets:pink_terracotta_octet"
        ],
        [
          "simpleoctets:pink_terracotta_octet"
        ],
        [
          "simpleoctets:pink_terracotta_octet"
        ],
        [
          "simpleoctets:pink_terracotta_octet"
        ],
        [
          "simpleoctets:pink_terracotta_octet"
        ],
        [
          "simpleoctets:pink_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:pink_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:pink_terracotta_octet_from_pink_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:pink_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/pink_terracotta_octet_from_pink_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:pink_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:pink_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:cactus_flower",
            "count": 0.125
          }
        ]
      }
    },
    {
      "id": "simpleoctets:pink_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:pink_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/pink_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:pink_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:pink_wool_octet"
        ],
        [
          "simpleoctets:pink_wool_octet"
        ],
        [
          "simpleoctets:pink_wool_octet"
        ],
        [
          "simpleoctets:pink_wool_octet"
        ],
        [
          "simpleoctets:pink_wool_octet"
        ],
        [
          "simpleoctets:pink_wool_octet"
        ],
        [
          "simpleoctets:pink_wool_octet"
        ],
        [
          "simpleoctets:pink_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:pink_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:pink_wool_octet_from_pink_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:pink_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/pink_wool_octet_from_pink_wool_stonecutting.json",
      "ingredients": [
        "minecraft:pink_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:pink_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:cactus_flower",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_andesite_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:polished_andesite",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_andesite_from_octets.json",
      "ingredients": [
        "simpleoctets:polished_andesite_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:polished_andesite_octet"
        ],
        [
          "simpleoctets:polished_andesite_octet"
        ],
        [
          "simpleoctets:polished_andesite_octet"
        ],
        [
          "simpleoctets:polished_andesite_octet"
        ],
        [
          "simpleoctets:polished_andesite_octet"
        ],
        [
          "simpleoctets:polished_andesite_octet"
        ],
        [
          "simpleoctets:polished_andesite_octet"
        ],
        [
          "simpleoctets:polished_andesite_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:polished_andesite_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_andesite_octet_from_polished_andesite_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:polished_andesite_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_andesite_octet_from_polished_andesite_stonecutting.json",
      "ingredients": [
        "minecraft:polished_andesite"
      ],
      "slots": {
        "ingredient": [
          "minecraft:polished_andesite"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cobblestone",
            "count": 1
          },
          {
            "id": "minecraft:quartz",
            "count": 0.5
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_astralit_block_octet_from_polished_astralit_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:polished_astralit_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_astralit_block_octet_from_polished_astralit_stonecutting.json",
      "ingredients": [
        "simplebuilding:polished_astralit"
      ],
      "slots": {
        "ingredient": [
          "simplebuilding:polished_astralit"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simplebuilding:polished_astralit",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_astralit_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simplebuilding:polished_astralit",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_astralit_from_octets.json",
      "ingredients": [
        "simpleoctets:polished_astralit_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:polished_astralit_block_octet"
        ],
        [
          "simpleoctets:polished_astralit_block_octet"
        ],
        [
          "simpleoctets:polished_astralit_block_octet"
        ],
        [
          "simpleoctets:polished_astralit_block_octet"
        ],
        [
          "simpleoctets:polished_astralit_block_octet"
        ],
        [
          "simpleoctets:polished_astralit_block_octet"
        ],
        [
          "simpleoctets:polished_astralit_block_octet"
        ],
        [
          "simpleoctets:polished_astralit_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:polished_astralit_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_blackstone_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:polished_blackstone_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_blackstone_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:polished_blackstone_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:polished_blackstone_bricks_octet"
        ],
        [
          "simpleoctets:polished_blackstone_bricks_octet"
        ],
        [
          "simpleoctets:polished_blackstone_bricks_octet"
        ],
        [
          "simpleoctets:polished_blackstone_bricks_octet"
        ],
        [
          "simpleoctets:polished_blackstone_bricks_octet"
        ],
        [
          "simpleoctets:polished_blackstone_bricks_octet"
        ],
        [
          "simpleoctets:polished_blackstone_bricks_octet"
        ],
        [
          "simpleoctets:polished_blackstone_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:polished_blackstone_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_blackstone_bricks_octet_from_polished_blackstone_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:polished_blackstone_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_blackstone_bricks_octet_from_polished_blackstone_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:polished_blackstone_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:polished_blackstone_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:blackstone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_blackstone_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:polished_blackstone",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_blackstone_from_octets.json",
      "ingredients": [
        "simpleoctets:polished_blackstone_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:polished_blackstone_octet"
        ],
        [
          "simpleoctets:polished_blackstone_octet"
        ],
        [
          "simpleoctets:polished_blackstone_octet"
        ],
        [
          "simpleoctets:polished_blackstone_octet"
        ],
        [
          "simpleoctets:polished_blackstone_octet"
        ],
        [
          "simpleoctets:polished_blackstone_octet"
        ],
        [
          "simpleoctets:polished_blackstone_octet"
        ],
        [
          "simpleoctets:polished_blackstone_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:polished_blackstone_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_blackstone_octet_from_polished_blackstone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:polished_blackstone_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_blackstone_octet_from_polished_blackstone_stonecutting.json",
      "ingredients": [
        "minecraft:polished_blackstone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:polished_blackstone"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:blackstone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_cinnabar_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:polished_cinnabar",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_cinnabar_from_octets.json",
      "ingredients": [
        "simpleoctets:polished_cinnabar_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:polished_cinnabar_octet"
        ],
        [
          "simpleoctets:polished_cinnabar_octet"
        ],
        [
          "simpleoctets:polished_cinnabar_octet"
        ],
        [
          "simpleoctets:polished_cinnabar_octet"
        ],
        [
          "simpleoctets:polished_cinnabar_octet"
        ],
        [
          "simpleoctets:polished_cinnabar_octet"
        ],
        [
          "simpleoctets:polished_cinnabar_octet"
        ],
        [
          "simpleoctets:polished_cinnabar_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:polished_cinnabar_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_cinnabar_octet_from_polished_cinnabar_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:polished_cinnabar_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_cinnabar_octet_from_polished_cinnabar_stonecutting.json",
      "ingredients": [
        "minecraft:polished_cinnabar"
      ],
      "slots": {
        "ingredient": [
          "minecraft:polished_cinnabar"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cinnabar",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_deepslate_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:polished_deepslate",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_deepslate_from_octets.json",
      "ingredients": [
        "simpleoctets:polished_deepslate_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:polished_deepslate_octet"
        ],
        [
          "simpleoctets:polished_deepslate_octet"
        ],
        [
          "simpleoctets:polished_deepslate_octet"
        ],
        [
          "simpleoctets:polished_deepslate_octet"
        ],
        [
          "simpleoctets:polished_deepslate_octet"
        ],
        [
          "simpleoctets:polished_deepslate_octet"
        ],
        [
          "simpleoctets:polished_deepslate_octet"
        ],
        [
          "simpleoctets:polished_deepslate_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:polished_deepslate_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_deepslate_octet_from_polished_deepslate_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:polished_deepslate_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_deepslate_octet_from_polished_deepslate_stonecutting.json",
      "ingredients": [
        "minecraft:polished_deepslate"
      ],
      "slots": {
        "ingredient": [
          "minecraft:polished_deepslate"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cobbled_deepslate",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_diorite_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:polished_diorite",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_diorite_from_octets.json",
      "ingredients": [
        "simpleoctets:polished_diorite_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:polished_diorite_octet"
        ],
        [
          "simpleoctets:polished_diorite_octet"
        ],
        [
          "simpleoctets:polished_diorite_octet"
        ],
        [
          "simpleoctets:polished_diorite_octet"
        ],
        [
          "simpleoctets:polished_diorite_octet"
        ],
        [
          "simpleoctets:polished_diorite_octet"
        ],
        [
          "simpleoctets:polished_diorite_octet"
        ],
        [
          "simpleoctets:polished_diorite_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:polished_diorite_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_diorite_octet_from_polished_diorite_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:polished_diorite_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_diorite_octet_from_polished_diorite_stonecutting.json",
      "ingredients": [
        "minecraft:polished_diorite"
      ],
      "slots": {
        "ingredient": [
          "minecraft:polished_diorite"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:cobblestone",
            "count": 1
          },
          {
            "id": "minecraft:quartz",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_ender_quartz_block_octet_from_polished_ender_quartz_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:polished_ender_quartz_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_ender_quartz_block_octet_from_polished_ender_quartz_stonecutting.json",
      "ingredients": [
        "simplebuilding:polished_ender_quartz"
      ],
      "slots": {
        "ingredient": [
          "simplebuilding:polished_ender_quartz"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simplebuilding:polished_ender_quartz",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_ender_quartz_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simplebuilding:polished_ender_quartz",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_ender_quartz_from_octets.json",
      "ingredients": [
        "simpleoctets:polished_ender_quartz_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:polished_ender_quartz_block_octet"
        ],
        [
          "simpleoctets:polished_ender_quartz_block_octet"
        ],
        [
          "simpleoctets:polished_ender_quartz_block_octet"
        ],
        [
          "simpleoctets:polished_ender_quartz_block_octet"
        ],
        [
          "simpleoctets:polished_ender_quartz_block_octet"
        ],
        [
          "simpleoctets:polished_ender_quartz_block_octet"
        ],
        [
          "simpleoctets:polished_ender_quartz_block_octet"
        ],
        [
          "simpleoctets:polished_ender_quartz_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:polished_ender_quartz_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_granite_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:polished_granite",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_granite_from_octets.json",
      "ingredients": [
        "simpleoctets:polished_granite_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:polished_granite_octet"
        ],
        [
          "simpleoctets:polished_granite_octet"
        ],
        [
          "simpleoctets:polished_granite_octet"
        ],
        [
          "simpleoctets:polished_granite_octet"
        ],
        [
          "simpleoctets:polished_granite_octet"
        ],
        [
          "simpleoctets:polished_granite_octet"
        ],
        [
          "simpleoctets:polished_granite_octet"
        ],
        [
          "simpleoctets:polished_granite_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:polished_granite_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_granite_octet_from_polished_granite_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:polished_granite_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_granite_octet_from_polished_granite_stonecutting.json",
      "ingredients": [
        "minecraft:polished_granite"
      ],
      "slots": {
        "ingredient": [
          "minecraft:polished_granite"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:quartz",
            "count": 2
          },
          {
            "id": "minecraft:cobblestone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_nihilith_block_octet_from_polished_nihilith_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:polished_nihilith_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_nihilith_block_octet_from_polished_nihilith_stonecutting.json",
      "ingredients": [
        "simplebuilding:polished_nihilith"
      ],
      "slots": {
        "ingredient": [
          "simplebuilding:polished_nihilith"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simplebuilding:polished_nihilith",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_nihilith_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simplebuilding:polished_nihilith",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_nihilith_from_octets.json",
      "ingredients": [
        "simpleoctets:polished_nihilith_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:polished_nihilith_block_octet"
        ],
        [
          "simpleoctets:polished_nihilith_block_octet"
        ],
        [
          "simpleoctets:polished_nihilith_block_octet"
        ],
        [
          "simpleoctets:polished_nihilith_block_octet"
        ],
        [
          "simpleoctets:polished_nihilith_block_octet"
        ],
        [
          "simpleoctets:polished_nihilith_block_octet"
        ],
        [
          "simpleoctets:polished_nihilith_block_octet"
        ],
        [
          "simpleoctets:polished_nihilith_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:polished_nihilith_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_sulfur_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:polished_sulfur",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_sulfur_from_octets.json",
      "ingredients": [
        "simpleoctets:polished_sulfur_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:polished_sulfur_octet"
        ],
        [
          "simpleoctets:polished_sulfur_octet"
        ],
        [
          "simpleoctets:polished_sulfur_octet"
        ],
        [
          "simpleoctets:polished_sulfur_octet"
        ],
        [
          "simpleoctets:polished_sulfur_octet"
        ],
        [
          "simpleoctets:polished_sulfur_octet"
        ],
        [
          "simpleoctets:polished_sulfur_octet"
        ],
        [
          "simpleoctets:polished_sulfur_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:polished_sulfur_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_sulfur_octet_from_polished_sulfur_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:polished_sulfur_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_sulfur_octet_from_polished_sulfur_stonecutting.json",
      "ingredients": [
        "minecraft:polished_sulfur"
      ],
      "slots": {
        "ingredient": [
          "minecraft:polished_sulfur"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:sulfur_spike",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_tuff_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:polished_tuff",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_tuff_from_octets.json",
      "ingredients": [
        "simpleoctets:polished_tuff_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:polished_tuff_octet"
        ],
        [
          "simpleoctets:polished_tuff_octet"
        ],
        [
          "simpleoctets:polished_tuff_octet"
        ],
        [
          "simpleoctets:polished_tuff_octet"
        ],
        [
          "simpleoctets:polished_tuff_octet"
        ],
        [
          "simpleoctets:polished_tuff_octet"
        ],
        [
          "simpleoctets:polished_tuff_octet"
        ],
        [
          "simpleoctets:polished_tuff_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:polished_tuff_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:polished_tuff_octet_from_polished_tuff_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:polished_tuff_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/polished_tuff_octet_from_polished_tuff_stonecutting.json",
      "ingredients": [
        "minecraft:polished_tuff"
      ],
      "slots": {
        "ingredient": [
          "minecraft:polished_tuff"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:tuff",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:poplar_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:poplar_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/poplar_log_from_octets.json",
      "ingredients": [
        "simpleoctets:poplar_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:poplar_log_octet"
        ],
        [
          "simpleoctets:poplar_log_octet"
        ],
        [
          "simpleoctets:poplar_log_octet"
        ],
        [
          "simpleoctets:poplar_log_octet"
        ],
        [
          "simpleoctets:poplar_log_octet"
        ],
        [
          "simpleoctets:poplar_log_octet"
        ],
        [
          "simpleoctets:poplar_log_octet"
        ],
        [
          "simpleoctets:poplar_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:poplar_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:poplar_log_octet_from_poplar_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:poplar_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/poplar_log_octet_from_poplar_log_stonecutting.json",
      "ingredients": [
        "minecraft:poplar_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:poplar_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:poplar_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:poplar_octet_from_poplar_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:poplar_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/poplar_octet_from_poplar_planks_stonecutting.json",
      "ingredients": [
        "minecraft:poplar_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:poplar_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:poplar_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleoctets:poplar_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:poplar_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/poplar_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:poplar_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:poplar_octet"
        ],
        [
          "simpleoctets:poplar_octet"
        ],
        [
          "simpleoctets:poplar_octet"
        ],
        [
          "simpleoctets:poplar_octet"
        ],
        [
          "simpleoctets:poplar_octet"
        ],
        [
          "simpleoctets:poplar_octet"
        ],
        [
          "simpleoctets:poplar_octet"
        ],
        [
          "simpleoctets:poplar_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:poplar_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:prismarine_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:prismarine_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/prismarine_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:prismarine_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:prismarine_bricks_octet"
        ],
        [
          "simpleoctets:prismarine_bricks_octet"
        ],
        [
          "simpleoctets:prismarine_bricks_octet"
        ],
        [
          "simpleoctets:prismarine_bricks_octet"
        ],
        [
          "simpleoctets:prismarine_bricks_octet"
        ],
        [
          "simpleoctets:prismarine_bricks_octet"
        ],
        [
          "simpleoctets:prismarine_bricks_octet"
        ],
        [
          "simpleoctets:prismarine_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:prismarine_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:prismarine_bricks_octet_from_prismarine_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:prismarine_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/prismarine_bricks_octet_from_prismarine_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:prismarine_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:prismarine_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:prismarine_shard",
            "count": 9
          }
        ]
      }
    },
    {
      "id": "simpleoctets:prismarine_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:prismarine",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/prismarine_from_octets.json",
      "ingredients": [
        "simpleoctets:prismarine_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:prismarine_octet"
        ],
        [
          "simpleoctets:prismarine_octet"
        ],
        [
          "simpleoctets:prismarine_octet"
        ],
        [
          "simpleoctets:prismarine_octet"
        ],
        [
          "simpleoctets:prismarine_octet"
        ],
        [
          "simpleoctets:prismarine_octet"
        ],
        [
          "simpleoctets:prismarine_octet"
        ],
        [
          "simpleoctets:prismarine_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:prismarine_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:prismarine_octet_from_prismarine_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:prismarine_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/prismarine_octet_from_prismarine_stonecutting.json",
      "ingredients": [
        "minecraft:prismarine"
      ],
      "slots": {
        "ingredient": [
          "minecraft:prismarine"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:prismarine_shard",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:purple_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:purple_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/purple_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:purple_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:purple_concrete_octet"
        ],
        [
          "simpleoctets:purple_concrete_octet"
        ],
        [
          "simpleoctets:purple_concrete_octet"
        ],
        [
          "simpleoctets:purple_concrete_octet"
        ],
        [
          "simpleoctets:purple_concrete_octet"
        ],
        [
          "simpleoctets:purple_concrete_octet"
        ],
        [
          "simpleoctets:purple_concrete_octet"
        ],
        [
          "simpleoctets:purple_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:purple_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:purple_concrete_octet_from_purple_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:purple_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/purple_concrete_octet_from_purple_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:purple_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:purple_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:purple_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:purple_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:purple_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/purple_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:purple_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:purple_terracotta_octet"
        ],
        [
          "simpleoctets:purple_terracotta_octet"
        ],
        [
          "simpleoctets:purple_terracotta_octet"
        ],
        [
          "simpleoctets:purple_terracotta_octet"
        ],
        [
          "simpleoctets:purple_terracotta_octet"
        ],
        [
          "simpleoctets:purple_terracotta_octet"
        ],
        [
          "simpleoctets:purple_terracotta_octet"
        ],
        [
          "simpleoctets:purple_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:purple_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:purple_terracotta_octet_from_purple_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:purple_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/purple_terracotta_octet_from_purple_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:purple_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:purple_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:beetroot",
            "count": 0.062
          },
          {
            "id": "minecraft:lapis_lazuli",
            "count": 0.062
          }
        ]
      }
    },
    {
      "id": "simpleoctets:purple_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:purple_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/purple_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:purple_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:purple_wool_octet"
        ],
        [
          "simpleoctets:purple_wool_octet"
        ],
        [
          "simpleoctets:purple_wool_octet"
        ],
        [
          "simpleoctets:purple_wool_octet"
        ],
        [
          "simpleoctets:purple_wool_octet"
        ],
        [
          "simpleoctets:purple_wool_octet"
        ],
        [
          "simpleoctets:purple_wool_octet"
        ],
        [
          "simpleoctets:purple_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:purple_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:purple_wool_octet_from_purple_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:purple_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/purple_wool_octet_from_purple_wool_stonecutting.json",
      "ingredients": [
        "minecraft:purple_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:purple_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:beetroot",
            "count": 0.5
          },
          {
            "id": "minecraft:lapis_lazuli",
            "count": 0.5
          }
        ]
      }
    },
    {
      "id": "simpleoctets:purpur_block_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:purpur_block",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/purpur_block_from_octets.json",
      "ingredients": [
        "simpleoctets:purpur_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:purpur_block_octet"
        ],
        [
          "simpleoctets:purpur_block_octet"
        ],
        [
          "simpleoctets:purpur_block_octet"
        ],
        [
          "simpleoctets:purpur_block_octet"
        ],
        [
          "simpleoctets:purpur_block_octet"
        ],
        [
          "simpleoctets:purpur_block_octet"
        ],
        [
          "simpleoctets:purpur_block_octet"
        ],
        [
          "simpleoctets:purpur_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:purpur_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:purpur_block_octet_from_purpur_block_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:purpur_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/purpur_block_octet_from_purpur_block_stonecutting.json",
      "ingredients": [
        "minecraft:purpur_block"
      ],
      "slots": {
        "ingredient": [
          "minecraft:purpur_block"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:chorus_fruit",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:quartz_block_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:quartz_block",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/quartz_block_from_octets.json",
      "ingredients": [
        "simpleoctets:quartz_block_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:quartz_block_octet"
        ],
        [
          "simpleoctets:quartz_block_octet"
        ],
        [
          "simpleoctets:quartz_block_octet"
        ],
        [
          "simpleoctets:quartz_block_octet"
        ],
        [
          "simpleoctets:quartz_block_octet"
        ],
        [
          "simpleoctets:quartz_block_octet"
        ],
        [
          "simpleoctets:quartz_block_octet"
        ],
        [
          "simpleoctets:quartz_block_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:quartz_block_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:quartz_block_octet_from_quartz_block_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:quartz_block_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/quartz_block_octet_from_quartz_block_stonecutting.json",
      "ingredients": [
        "minecraft:quartz_block"
      ],
      "slots": {
        "ingredient": [
          "minecraft:quartz_block"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:quartz",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:red_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:red_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/red_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:red_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:red_concrete_octet"
        ],
        [
          "simpleoctets:red_concrete_octet"
        ],
        [
          "simpleoctets:red_concrete_octet"
        ],
        [
          "simpleoctets:red_concrete_octet"
        ],
        [
          "simpleoctets:red_concrete_octet"
        ],
        [
          "simpleoctets:red_concrete_octet"
        ],
        [
          "simpleoctets:red_concrete_octet"
        ],
        [
          "simpleoctets:red_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:red_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:red_concrete_octet_from_red_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:red_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/red_concrete_octet_from_red_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:red_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:red_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:red_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:red_nether_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:red_nether_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/red_nether_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:red_nether_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:red_nether_bricks_octet"
        ],
        [
          "simpleoctets:red_nether_bricks_octet"
        ],
        [
          "simpleoctets:red_nether_bricks_octet"
        ],
        [
          "simpleoctets:red_nether_bricks_octet"
        ],
        [
          "simpleoctets:red_nether_bricks_octet"
        ],
        [
          "simpleoctets:red_nether_bricks_octet"
        ],
        [
          "simpleoctets:red_nether_bricks_octet"
        ],
        [
          "simpleoctets:red_nether_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:red_nether_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:red_nether_bricks_octet_from_red_nether_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:red_nether_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/red_nether_bricks_octet_from_red_nether_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:red_nether_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:red_nether_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
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
      "id": "simpleoctets:red_sandstone_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:red_sandstone",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/red_sandstone_from_octets.json",
      "ingredients": [
        "simpleoctets:red_sandstone_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:red_sandstone_octet"
        ],
        [
          "simpleoctets:red_sandstone_octet"
        ],
        [
          "simpleoctets:red_sandstone_octet"
        ],
        [
          "simpleoctets:red_sandstone_octet"
        ],
        [
          "simpleoctets:red_sandstone_octet"
        ],
        [
          "simpleoctets:red_sandstone_octet"
        ],
        [
          "simpleoctets:red_sandstone_octet"
        ],
        [
          "simpleoctets:red_sandstone_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:red_sandstone_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:red_sandstone_octet_from_red_sandstone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:red_sandstone_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/red_sandstone_octet_from_red_sandstone_stonecutting.json",
      "ingredients": [
        "minecraft:red_sandstone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:red_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:red_sand",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:red_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:red_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/red_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:red_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:red_terracotta_octet"
        ],
        [
          "simpleoctets:red_terracotta_octet"
        ],
        [
          "simpleoctets:red_terracotta_octet"
        ],
        [
          "simpleoctets:red_terracotta_octet"
        ],
        [
          "simpleoctets:red_terracotta_octet"
        ],
        [
          "simpleoctets:red_terracotta_octet"
        ],
        [
          "simpleoctets:red_terracotta_octet"
        ],
        [
          "simpleoctets:red_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:red_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:red_terracotta_octet_from_red_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:red_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/red_terracotta_octet_from_red_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:red_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:red_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:beetroot",
            "count": 0.125
          }
        ]
      }
    },
    {
      "id": "simpleoctets:red_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:red_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/red_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:red_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:red_wool_octet"
        ],
        [
          "simpleoctets:red_wool_octet"
        ],
        [
          "simpleoctets:red_wool_octet"
        ],
        [
          "simpleoctets:red_wool_octet"
        ],
        [
          "simpleoctets:red_wool_octet"
        ],
        [
          "simpleoctets:red_wool_octet"
        ],
        [
          "simpleoctets:red_wool_octet"
        ],
        [
          "simpleoctets:red_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:red_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:red_wool_octet_from_red_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:red_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/red_wool_octet_from_red_wool_stonecutting.json",
      "ingredients": [
        "minecraft:red_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:red_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:beetroot",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:resin_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:resin_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/resin_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:resin_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:resin_bricks_octet"
        ],
        [
          "simpleoctets:resin_bricks_octet"
        ],
        [
          "simpleoctets:resin_bricks_octet"
        ],
        [
          "simpleoctets:resin_bricks_octet"
        ],
        [
          "simpleoctets:resin_bricks_octet"
        ],
        [
          "simpleoctets:resin_bricks_octet"
        ],
        [
          "simpleoctets:resin_bricks_octet"
        ],
        [
          "simpleoctets:resin_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:resin_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:resin_bricks_octet_from_resin_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:resin_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/resin_bricks_octet_from_resin_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:resin_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:resin_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:resin_clump",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:sandstone_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:sandstone",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/sandstone_from_octets.json",
      "ingredients": [
        "simpleoctets:sandstone_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:sandstone_octet"
        ],
        [
          "simpleoctets:sandstone_octet"
        ],
        [
          "simpleoctets:sandstone_octet"
        ],
        [
          "simpleoctets:sandstone_octet"
        ],
        [
          "simpleoctets:sandstone_octet"
        ],
        [
          "simpleoctets:sandstone_octet"
        ],
        [
          "simpleoctets:sandstone_octet"
        ],
        [
          "simpleoctets:sandstone_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:sandstone_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:sandstone_octet_from_sandstone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:sandstone_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/sandstone_octet_from_sandstone_stonecutting.json",
      "ingredients": [
        "minecraft:sandstone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:sand",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:smooth_basalt_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:smooth_basalt",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/smooth_basalt_from_octets.json",
      "ingredients": [
        "simpleoctets:smooth_basalt_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:smooth_basalt_octet"
        ],
        [
          "simpleoctets:smooth_basalt_octet"
        ],
        [
          "simpleoctets:smooth_basalt_octet"
        ],
        [
          "simpleoctets:smooth_basalt_octet"
        ],
        [
          "simpleoctets:smooth_basalt_octet"
        ],
        [
          "simpleoctets:smooth_basalt_octet"
        ],
        [
          "simpleoctets:smooth_basalt_octet"
        ],
        [
          "simpleoctets:smooth_basalt_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:smooth_basalt_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:smooth_basalt_octet_from_smooth_basalt_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:smooth_basalt_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/smooth_basalt_octet_from_smooth_basalt_stonecutting.json",
      "ingredients": [
        "minecraft:smooth_basalt"
      ],
      "slots": {
        "ingredient": [
          "minecraft:smooth_basalt"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:basalt",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:smooth_quartz_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:smooth_quartz",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/smooth_quartz_from_octets.json",
      "ingredients": [
        "simpleoctets:smooth_quartz_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:smooth_quartz_octet"
        ],
        [
          "simpleoctets:smooth_quartz_octet"
        ],
        [
          "simpleoctets:smooth_quartz_octet"
        ],
        [
          "simpleoctets:smooth_quartz_octet"
        ],
        [
          "simpleoctets:smooth_quartz_octet"
        ],
        [
          "simpleoctets:smooth_quartz_octet"
        ],
        [
          "simpleoctets:smooth_quartz_octet"
        ],
        [
          "simpleoctets:smooth_quartz_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:smooth_quartz_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:smooth_quartz_octet_from_smooth_quartz_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:smooth_quartz_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/smooth_quartz_octet_from_smooth_quartz_stonecutting.json",
      "ingredients": [
        "minecraft:smooth_quartz"
      ],
      "slots": {
        "ingredient": [
          "minecraft:smooth_quartz"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:quartz",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:smooth_red_sandstone_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:smooth_red_sandstone",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/smooth_red_sandstone_from_octets.json",
      "ingredients": [
        "simpleoctets:smooth_red_sandstone_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:smooth_red_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_red_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_red_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_red_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_red_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_red_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_red_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_red_sandstone_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:smooth_red_sandstone_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:smooth_red_sandstone_octet_from_smooth_red_sandstone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:smooth_red_sandstone_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/smooth_red_sandstone_octet_from_smooth_red_sandstone_stonecutting.json",
      "ingredients": [
        "minecraft:smooth_red_sandstone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:smooth_red_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:red_sand",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:smooth_sandstone_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:smooth_sandstone",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/smooth_sandstone_from_octets.json",
      "ingredients": [
        "simpleoctets:smooth_sandstone_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:smooth_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_sandstone_octet"
        ],
        [
          "simpleoctets:smooth_sandstone_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:smooth_sandstone_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:smooth_sandstone_octet_from_smooth_sandstone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:smooth_sandstone_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/smooth_sandstone_octet_from_smooth_sandstone_stonecutting.json",
      "ingredients": [
        "minecraft:smooth_sandstone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:smooth_sandstone"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:sand",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:spruce_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:spruce_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/spruce_log_from_octets.json",
      "ingredients": [
        "simpleoctets:spruce_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:spruce_log_octet"
        ],
        [
          "simpleoctets:spruce_log_octet"
        ],
        [
          "simpleoctets:spruce_log_octet"
        ],
        [
          "simpleoctets:spruce_log_octet"
        ],
        [
          "simpleoctets:spruce_log_octet"
        ],
        [
          "simpleoctets:spruce_log_octet"
        ],
        [
          "simpleoctets:spruce_log_octet"
        ],
        [
          "simpleoctets:spruce_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:spruce_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:spruce_log_octet_from_spruce_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:spruce_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/spruce_log_octet_from_spruce_log_stonecutting.json",
      "ingredients": [
        "minecraft:spruce_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:spruce_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:spruce_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:spruce_octet_from_spruce_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:spruce_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/spruce_octet_from_spruce_planks_stonecutting.json",
      "ingredients": [
        "minecraft:spruce_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:spruce_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:spruce_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleoctets:spruce_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:spruce_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/spruce_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:spruce_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:spruce_octet"
        ],
        [
          "simpleoctets:spruce_octet"
        ],
        [
          "simpleoctets:spruce_octet"
        ],
        [
          "simpleoctets:spruce_octet"
        ],
        [
          "simpleoctets:spruce_octet"
        ],
        [
          "simpleoctets:spruce_octet"
        ],
        [
          "simpleoctets:spruce_octet"
        ],
        [
          "simpleoctets:spruce_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:spruce_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stone_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stone_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stone_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:stone_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stone_bricks_octet"
        ],
        [
          "simpleoctets:stone_bricks_octet"
        ],
        [
          "simpleoctets:stone_bricks_octet"
        ],
        [
          "simpleoctets:stone_bricks_octet"
        ],
        [
          "simpleoctets:stone_bricks_octet"
        ],
        [
          "simpleoctets:stone_bricks_octet"
        ],
        [
          "simpleoctets:stone_bricks_octet"
        ],
        [
          "simpleoctets:stone_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stone_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stone_bricks_octet_from_stone_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stone_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stone_bricks_octet_from_stone_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:stone_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stone_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stone_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stone",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stone_from_octets.json",
      "ingredients": [
        "simpleoctets:stone_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stone_octet"
        ],
        [
          "simpleoctets:stone_octet"
        ],
        [
          "simpleoctets:stone_octet"
        ],
        [
          "simpleoctets:stone_octet"
        ],
        [
          "simpleoctets:stone_octet"
        ],
        [
          "simpleoctets:stone_octet"
        ],
        [
          "simpleoctets:stone_octet"
        ],
        [
          "simpleoctets:stone_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stone_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stone_octet_from_stone_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stone_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stone_octet_from_stone_stonecutting.json",
      "ingredients": [
        "minecraft:stone"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stone"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_acacia_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stripped_acacia_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_acacia_log_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_acacia_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_acacia_log_octet"
        ],
        [
          "simpleoctets:stripped_acacia_log_octet"
        ],
        [
          "simpleoctets:stripped_acacia_log_octet"
        ],
        [
          "simpleoctets:stripped_acacia_log_octet"
        ],
        [
          "simpleoctets:stripped_acacia_log_octet"
        ],
        [
          "simpleoctets:stripped_acacia_log_octet"
        ],
        [
          "simpleoctets:stripped_acacia_log_octet"
        ],
        [
          "simpleoctets:stripped_acacia_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_acacia_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_acacia_log_octet_from_stripped_acacia_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_acacia_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_acacia_log_octet_from_stripped_acacia_log_stonecutting.json",
      "ingredients": [
        "minecraft:stripped_acacia_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stripped_acacia_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stripped_acacia_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_ashwood_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleend:stripped_ashwood_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_ashwood_log_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_ashwood_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_ashwood_log_octet"
        ],
        [
          "simpleoctets:stripped_ashwood_log_octet"
        ],
        [
          "simpleoctets:stripped_ashwood_log_octet"
        ],
        [
          "simpleoctets:stripped_ashwood_log_octet"
        ],
        [
          "simpleoctets:stripped_ashwood_log_octet"
        ],
        [
          "simpleoctets:stripped_ashwood_log_octet"
        ],
        [
          "simpleoctets:stripped_ashwood_log_octet"
        ],
        [
          "simpleoctets:stripped_ashwood_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_ashwood_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_ashwood_log_octet_from_stripped_ashwood_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_ashwood_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_ashwood_log_octet_from_stripped_ashwood_log_stonecutting.json",
      "ingredients": [
        "simpleend:stripped_ashwood_log"
      ],
      "slots": {
        "ingredient": [
          "simpleend:stripped_ashwood_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simpleend:stripped_ashwood_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_birch_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stripped_birch_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_birch_log_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_birch_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_birch_log_octet"
        ],
        [
          "simpleoctets:stripped_birch_log_octet"
        ],
        [
          "simpleoctets:stripped_birch_log_octet"
        ],
        [
          "simpleoctets:stripped_birch_log_octet"
        ],
        [
          "simpleoctets:stripped_birch_log_octet"
        ],
        [
          "simpleoctets:stripped_birch_log_octet"
        ],
        [
          "simpleoctets:stripped_birch_log_octet"
        ],
        [
          "simpleoctets:stripped_birch_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_birch_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_birch_log_octet_from_stripped_birch_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_birch_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_birch_log_octet_from_stripped_birch_log_stonecutting.json",
      "ingredients": [
        "minecraft:stripped_birch_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stripped_birch_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stripped_birch_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_cherry_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stripped_cherry_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_cherry_log_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_cherry_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_cherry_log_octet"
        ],
        [
          "simpleoctets:stripped_cherry_log_octet"
        ],
        [
          "simpleoctets:stripped_cherry_log_octet"
        ],
        [
          "simpleoctets:stripped_cherry_log_octet"
        ],
        [
          "simpleoctets:stripped_cherry_log_octet"
        ],
        [
          "simpleoctets:stripped_cherry_log_octet"
        ],
        [
          "simpleoctets:stripped_cherry_log_octet"
        ],
        [
          "simpleoctets:stripped_cherry_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_cherry_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_cherry_log_octet_from_stripped_cherry_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_cherry_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_cherry_log_octet_from_stripped_cherry_log_stonecutting.json",
      "ingredients": [
        "minecraft:stripped_cherry_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stripped_cherry_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stripped_cherry_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_crimson_stem_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stripped_crimson_stem",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_crimson_stem_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_crimson_stem_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_crimson_stem_octet"
        ],
        [
          "simpleoctets:stripped_crimson_stem_octet"
        ],
        [
          "simpleoctets:stripped_crimson_stem_octet"
        ],
        [
          "simpleoctets:stripped_crimson_stem_octet"
        ],
        [
          "simpleoctets:stripped_crimson_stem_octet"
        ],
        [
          "simpleoctets:stripped_crimson_stem_octet"
        ],
        [
          "simpleoctets:stripped_crimson_stem_octet"
        ],
        [
          "simpleoctets:stripped_crimson_stem_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_crimson_stem_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_crimson_stem_octet_from_stripped_crimson_stem_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_crimson_stem_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_crimson_stem_octet_from_stripped_crimson_stem_stonecutting.json",
      "ingredients": [
        "minecraft:stripped_crimson_stem"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stripped_crimson_stem"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stripped_crimson_stem",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_dark_oak_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stripped_dark_oak_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_dark_oak_log_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_dark_oak_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_dark_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_dark_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_dark_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_dark_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_dark_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_dark_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_dark_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_dark_oak_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_dark_oak_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_dark_oak_log_octet_from_stripped_dark_oak_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_dark_oak_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_dark_oak_log_octet_from_stripped_dark_oak_log_stonecutting.json",
      "ingredients": [
        "minecraft:stripped_dark_oak_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stripped_dark_oak_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stripped_dark_oak_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_duskwood_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "simpleend:stripped_duskwood_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_duskwood_log_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_duskwood_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_duskwood_log_octet"
        ],
        [
          "simpleoctets:stripped_duskwood_log_octet"
        ],
        [
          "simpleoctets:stripped_duskwood_log_octet"
        ],
        [
          "simpleoctets:stripped_duskwood_log_octet"
        ],
        [
          "simpleoctets:stripped_duskwood_log_octet"
        ],
        [
          "simpleoctets:stripped_duskwood_log_octet"
        ],
        [
          "simpleoctets:stripped_duskwood_log_octet"
        ],
        [
          "simpleoctets:stripped_duskwood_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_duskwood_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_duskwood_log_octet_from_stripped_duskwood_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_duskwood_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_duskwood_log_octet_from_stripped_duskwood_log_stonecutting.json",
      "ingredients": [
        "simpleend:stripped_duskwood_log"
      ],
      "slots": {
        "ingredient": [
          "simpleend:stripped_duskwood_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "simpleend:stripped_duskwood_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_jungle_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stripped_jungle_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_jungle_log_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_jungle_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_jungle_log_octet"
        ],
        [
          "simpleoctets:stripped_jungle_log_octet"
        ],
        [
          "simpleoctets:stripped_jungle_log_octet"
        ],
        [
          "simpleoctets:stripped_jungle_log_octet"
        ],
        [
          "simpleoctets:stripped_jungle_log_octet"
        ],
        [
          "simpleoctets:stripped_jungle_log_octet"
        ],
        [
          "simpleoctets:stripped_jungle_log_octet"
        ],
        [
          "simpleoctets:stripped_jungle_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_jungle_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_jungle_log_octet_from_stripped_jungle_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_jungle_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_jungle_log_octet_from_stripped_jungle_log_stonecutting.json",
      "ingredients": [
        "minecraft:stripped_jungle_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stripped_jungle_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stripped_jungle_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_mangrove_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stripped_mangrove_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_mangrove_log_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_mangrove_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_mangrove_log_octet"
        ],
        [
          "simpleoctets:stripped_mangrove_log_octet"
        ],
        [
          "simpleoctets:stripped_mangrove_log_octet"
        ],
        [
          "simpleoctets:stripped_mangrove_log_octet"
        ],
        [
          "simpleoctets:stripped_mangrove_log_octet"
        ],
        [
          "simpleoctets:stripped_mangrove_log_octet"
        ],
        [
          "simpleoctets:stripped_mangrove_log_octet"
        ],
        [
          "simpleoctets:stripped_mangrove_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_mangrove_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_mangrove_log_octet_from_stripped_mangrove_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_mangrove_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_mangrove_log_octet_from_stripped_mangrove_log_stonecutting.json",
      "ingredients": [
        "minecraft:stripped_mangrove_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stripped_mangrove_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stripped_mangrove_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_oak_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stripped_oak_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_oak_log_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_oak_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_oak_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_oak_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_oak_log_octet_from_stripped_oak_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_oak_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_oak_log_octet_from_stripped_oak_log_stonecutting.json",
      "ingredients": [
        "minecraft:stripped_oak_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stripped_oak_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stripped_oak_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_pale_oak_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stripped_pale_oak_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_pale_oak_log_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_pale_oak_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_pale_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_pale_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_pale_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_pale_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_pale_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_pale_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_pale_oak_log_octet"
        ],
        [
          "simpleoctets:stripped_pale_oak_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_pale_oak_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_pale_oak_log_octet_from_stripped_pale_oak_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_pale_oak_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_pale_oak_log_octet_from_stripped_pale_oak_log_stonecutting.json",
      "ingredients": [
        "minecraft:stripped_pale_oak_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stripped_pale_oak_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stripped_pale_oak_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_poplar_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stripped_poplar_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_poplar_log_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_poplar_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_poplar_log_octet"
        ],
        [
          "simpleoctets:stripped_poplar_log_octet"
        ],
        [
          "simpleoctets:stripped_poplar_log_octet"
        ],
        [
          "simpleoctets:stripped_poplar_log_octet"
        ],
        [
          "simpleoctets:stripped_poplar_log_octet"
        ],
        [
          "simpleoctets:stripped_poplar_log_octet"
        ],
        [
          "simpleoctets:stripped_poplar_log_octet"
        ],
        [
          "simpleoctets:stripped_poplar_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_poplar_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_poplar_log_octet_from_stripped_poplar_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_poplar_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_poplar_log_octet_from_stripped_poplar_log_stonecutting.json",
      "ingredients": [
        "minecraft:stripped_poplar_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stripped_poplar_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stripped_poplar_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_spruce_log_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stripped_spruce_log",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_spruce_log_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_spruce_log_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_spruce_log_octet"
        ],
        [
          "simpleoctets:stripped_spruce_log_octet"
        ],
        [
          "simpleoctets:stripped_spruce_log_octet"
        ],
        [
          "simpleoctets:stripped_spruce_log_octet"
        ],
        [
          "simpleoctets:stripped_spruce_log_octet"
        ],
        [
          "simpleoctets:stripped_spruce_log_octet"
        ],
        [
          "simpleoctets:stripped_spruce_log_octet"
        ],
        [
          "simpleoctets:stripped_spruce_log_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_spruce_log_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_spruce_log_octet_from_stripped_spruce_log_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_spruce_log_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_spruce_log_octet_from_stripped_spruce_log_stonecutting.json",
      "ingredients": [
        "minecraft:stripped_spruce_log"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stripped_spruce_log"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stripped_spruce_log",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_warped_stem_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:stripped_warped_stem",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_warped_stem_from_octets.json",
      "ingredients": [
        "simpleoctets:stripped_warped_stem_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:stripped_warped_stem_octet"
        ],
        [
          "simpleoctets:stripped_warped_stem_octet"
        ],
        [
          "simpleoctets:stripped_warped_stem_octet"
        ],
        [
          "simpleoctets:stripped_warped_stem_octet"
        ],
        [
          "simpleoctets:stripped_warped_stem_octet"
        ],
        [
          "simpleoctets:stripped_warped_stem_octet"
        ],
        [
          "simpleoctets:stripped_warped_stem_octet"
        ],
        [
          "simpleoctets:stripped_warped_stem_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:stripped_warped_stem_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:stripped_warped_stem_octet_from_stripped_warped_stem_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:stripped_warped_stem_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/stripped_warped_stem_octet_from_stripped_warped_stem_stonecutting.json",
      "ingredients": [
        "minecraft:stripped_warped_stem"
      ],
      "slots": {
        "ingredient": [
          "minecraft:stripped_warped_stem"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:stripped_warped_stem",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:sulfur_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:sulfur_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/sulfur_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:sulfur_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:sulfur_bricks_octet"
        ],
        [
          "simpleoctets:sulfur_bricks_octet"
        ],
        [
          "simpleoctets:sulfur_bricks_octet"
        ],
        [
          "simpleoctets:sulfur_bricks_octet"
        ],
        [
          "simpleoctets:sulfur_bricks_octet"
        ],
        [
          "simpleoctets:sulfur_bricks_octet"
        ],
        [
          "simpleoctets:sulfur_bricks_octet"
        ],
        [
          "simpleoctets:sulfur_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:sulfur_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:sulfur_bricks_octet_from_sulfur_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:sulfur_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/sulfur_bricks_octet_from_sulfur_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:sulfur_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:sulfur_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:sulfur_spike",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:sulfur_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:sulfur",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/sulfur_from_octets.json",
      "ingredients": [
        "simpleoctets:sulfur_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:sulfur_octet"
        ],
        [
          "simpleoctets:sulfur_octet"
        ],
        [
          "simpleoctets:sulfur_octet"
        ],
        [
          "simpleoctets:sulfur_octet"
        ],
        [
          "simpleoctets:sulfur_octet"
        ],
        [
          "simpleoctets:sulfur_octet"
        ],
        [
          "simpleoctets:sulfur_octet"
        ],
        [
          "simpleoctets:sulfur_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:sulfur_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:sulfur_octet_from_sulfur_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:sulfur_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/sulfur_octet_from_sulfur_stonecutting.json",
      "ingredients": [
        "minecraft:sulfur"
      ],
      "slots": {
        "ingredient": [
          "minecraft:sulfur"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:sulfur_spike",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:terracotta_octet"
        ],
        [
          "simpleoctets:terracotta_octet"
        ],
        [
          "simpleoctets:terracotta_octet"
        ],
        [
          "simpleoctets:terracotta_octet"
        ],
        [
          "simpleoctets:terracotta_octet"
        ],
        [
          "simpleoctets:terracotta_octet"
        ],
        [
          "simpleoctets:terracotta_octet"
        ],
        [
          "simpleoctets:terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:terracotta_octet_from_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/terracotta_octet_from_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:tuff_bricks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:tuff_bricks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/tuff_bricks_from_octets.json",
      "ingredients": [
        "simpleoctets:tuff_bricks_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:tuff_bricks_octet"
        ],
        [
          "simpleoctets:tuff_bricks_octet"
        ],
        [
          "simpleoctets:tuff_bricks_octet"
        ],
        [
          "simpleoctets:tuff_bricks_octet"
        ],
        [
          "simpleoctets:tuff_bricks_octet"
        ],
        [
          "simpleoctets:tuff_bricks_octet"
        ],
        [
          "simpleoctets:tuff_bricks_octet"
        ],
        [
          "simpleoctets:tuff_bricks_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:tuff_bricks_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:tuff_bricks_octet_from_tuff_bricks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:tuff_bricks_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/tuff_bricks_octet_from_tuff_bricks_stonecutting.json",
      "ingredients": [
        "minecraft:tuff_bricks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:tuff_bricks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:tuff",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:tuff_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:tuff",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/tuff_from_octets.json",
      "ingredients": [
        "simpleoctets:tuff_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:tuff_octet"
        ],
        [
          "simpleoctets:tuff_octet"
        ],
        [
          "simpleoctets:tuff_octet"
        ],
        [
          "simpleoctets:tuff_octet"
        ],
        [
          "simpleoctets:tuff_octet"
        ],
        [
          "simpleoctets:tuff_octet"
        ],
        [
          "simpleoctets:tuff_octet"
        ],
        [
          "simpleoctets:tuff_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:tuff_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:tuff_octet_from_tuff_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:tuff_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/tuff_octet_from_tuff_stonecutting.json",
      "ingredients": [
        "minecraft:tuff"
      ],
      "slots": {
        "ingredient": [
          "minecraft:tuff"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:tuff",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:warped_octet_from_warped_planks_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:warped_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/warped_octet_from_warped_planks_stonecutting.json",
      "ingredients": [
        "minecraft:warped_planks"
      ],
      "slots": {
        "ingredient": [
          "minecraft:warped_planks"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:warped_stem",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simpleoctets:warped_planks_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:warped_planks",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/warped_planks_from_octets.json",
      "ingredients": [
        "simpleoctets:warped_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:warped_octet"
        ],
        [
          "simpleoctets:warped_octet"
        ],
        [
          "simpleoctets:warped_octet"
        ],
        [
          "simpleoctets:warped_octet"
        ],
        [
          "simpleoctets:warped_octet"
        ],
        [
          "simpleoctets:warped_octet"
        ],
        [
          "simpleoctets:warped_octet"
        ],
        [
          "simpleoctets:warped_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:warped_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:warped_stem_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:warped_stem",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/warped_stem_from_octets.json",
      "ingredients": [
        "simpleoctets:warped_stem_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:warped_stem_octet"
        ],
        [
          "simpleoctets:warped_stem_octet"
        ],
        [
          "simpleoctets:warped_stem_octet"
        ],
        [
          "simpleoctets:warped_stem_octet"
        ],
        [
          "simpleoctets:warped_stem_octet"
        ],
        [
          "simpleoctets:warped_stem_octet"
        ],
        [
          "simpleoctets:warped_stem_octet"
        ],
        [
          "simpleoctets:warped_stem_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:warped_stem_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:warped_stem_octet_from_warped_stem_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:warped_stem_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/warped_stem_octet_from_warped_stem_stonecutting.json",
      "ingredients": [
        "minecraft:warped_stem"
      ],
      "slots": {
        "ingredient": [
          "minecraft:warped_stem"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:warped_stem",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:waxed_cut_copper_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:waxed_cut_copper",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/waxed_cut_copper_from_octets.json",
      "ingredients": [
        "simpleoctets:waxed_cut_copper_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:waxed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_cut_copper_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:waxed_cut_copper_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:waxed_cut_copper_octet_from_waxed_cut_copper_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:waxed_cut_copper_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/waxed_cut_copper_octet_from_waxed_cut_copper_stonecutting.json",
      "ingredients": [
        "minecraft:waxed_cut_copper"
      ],
      "slots": {
        "ingredient": [
          "minecraft:waxed_cut_copper"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:copper_ingot",
            "count": 9
          },
          {
            "id": "minecraft:honeycomb",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:waxed_exposed_cut_copper_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:waxed_exposed_cut_copper",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/waxed_exposed_cut_copper_from_octets.json",
      "ingredients": [
        "simpleoctets:waxed_exposed_cut_copper_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:waxed_exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_exposed_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_exposed_cut_copper_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:waxed_exposed_cut_copper_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:waxed_exposed_cut_copper_octet_from_waxed_exposed_cut_copper_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:waxed_exposed_cut_copper_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/waxed_exposed_cut_copper_octet_from_waxed_exposed_cut_copper_stonecutting.json",
      "ingredients": [
        "minecraft:waxed_exposed_cut_copper"
      ],
      "slots": {
        "ingredient": [
          "minecraft:waxed_exposed_cut_copper"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:exposed_copper",
            "count": 1
          },
          {
            "id": "minecraft:honeycomb",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:waxed_oxidized_cut_copper_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:waxed_oxidized_cut_copper",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/waxed_oxidized_cut_copper_from_octets.json",
      "ingredients": [
        "simpleoctets:waxed_oxidized_cut_copper_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:waxed_oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_oxidized_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_oxidized_cut_copper_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:waxed_oxidized_cut_copper_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:waxed_oxidized_cut_copper_octet_from_waxed_oxidized_cut_copper_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:waxed_oxidized_cut_copper_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/waxed_oxidized_cut_copper_octet_from_waxed_oxidized_cut_copper_stonecutting.json",
      "ingredients": [
        "minecraft:waxed_oxidized_cut_copper"
      ],
      "slots": {
        "ingredient": [
          "minecraft:waxed_oxidized_cut_copper"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:honeycomb",
            "count": 1
          },
          {
            "id": "minecraft:oxidized_copper",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:waxed_weathered_cut_copper_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:waxed_weathered_cut_copper",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/waxed_weathered_cut_copper_from_octets.json",
      "ingredients": [
        "simpleoctets:waxed_weathered_cut_copper_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:waxed_weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:waxed_weathered_cut_copper_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:waxed_weathered_cut_copper_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:waxed_weathered_cut_copper_octet_from_waxed_weathered_cut_copper_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:waxed_weathered_cut_copper_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/waxed_weathered_cut_copper_octet_from_waxed_weathered_cut_copper_stonecutting.json",
      "ingredients": [
        "minecraft:waxed_weathered_cut_copper"
      ],
      "slots": {
        "ingredient": [
          "minecraft:waxed_weathered_cut_copper"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:honeycomb",
            "count": 1
          },
          {
            "id": "minecraft:weathered_copper",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:weathered_copper_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:weathered_copper",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/weathered_copper_from_octets.json",
      "ingredients": [
        "simpleoctets:weathered_copper_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:weathered_copper_octet"
        ],
        [
          "simpleoctets:weathered_copper_octet"
        ],
        [
          "simpleoctets:weathered_copper_octet"
        ],
        [
          "simpleoctets:weathered_copper_octet"
        ],
        [
          "simpleoctets:weathered_copper_octet"
        ],
        [
          "simpleoctets:weathered_copper_octet"
        ],
        [
          "simpleoctets:weathered_copper_octet"
        ],
        [
          "simpleoctets:weathered_copper_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:weathered_copper_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:weathered_copper_octet_from_weathered_copper_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:weathered_copper_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/weathered_copper_octet_from_weathered_copper_stonecutting.json",
      "ingredients": [
        "minecraft:weathered_copper"
      ],
      "slots": {
        "ingredient": [
          "minecraft:weathered_copper"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:weathered_copper",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:weathered_cut_copper_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:weathered_cut_copper",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/weathered_cut_copper_from_octets.json",
      "ingredients": [
        "simpleoctets:weathered_cut_copper_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:weathered_cut_copper_octet"
        ],
        [
          "simpleoctets:weathered_cut_copper_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:weathered_cut_copper_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:weathered_cut_copper_octet_from_weathered_cut_copper_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:weathered_cut_copper_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/weathered_cut_copper_octet_from_weathered_cut_copper_stonecutting.json",
      "ingredients": [
        "minecraft:weathered_cut_copper"
      ],
      "slots": {
        "ingredient": [
          "minecraft:weathered_cut_copper"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:weathered_copper",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:white_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:white_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/white_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:white_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:white_concrete_octet"
        ],
        [
          "simpleoctets:white_concrete_octet"
        ],
        [
          "simpleoctets:white_concrete_octet"
        ],
        [
          "simpleoctets:white_concrete_octet"
        ],
        [
          "simpleoctets:white_concrete_octet"
        ],
        [
          "simpleoctets:white_concrete_octet"
        ],
        [
          "simpleoctets:white_concrete_octet"
        ],
        [
          "simpleoctets:white_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:white_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:white_concrete_octet_from_white_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:white_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/white_concrete_octet_from_white_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:white_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:white_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:white_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:white_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:white_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/white_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:white_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:white_terracotta_octet"
        ],
        [
          "simpleoctets:white_terracotta_octet"
        ],
        [
          "simpleoctets:white_terracotta_octet"
        ],
        [
          "simpleoctets:white_terracotta_octet"
        ],
        [
          "simpleoctets:white_terracotta_octet"
        ],
        [
          "simpleoctets:white_terracotta_octet"
        ],
        [
          "simpleoctets:white_terracotta_octet"
        ],
        [
          "simpleoctets:white_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:white_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:white_terracotta_octet_from_white_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:white_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/white_terracotta_octet_from_white_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:white_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:white_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:bone",
            "count": 0.042
          }
        ]
      }
    },
    {
      "id": "simpleoctets:white_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:white_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/white_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:white_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:white_wool_octet"
        ],
        [
          "simpleoctets:white_wool_octet"
        ],
        [
          "simpleoctets:white_wool_octet"
        ],
        [
          "simpleoctets:white_wool_octet"
        ],
        [
          "simpleoctets:white_wool_octet"
        ],
        [
          "simpleoctets:white_wool_octet"
        ],
        [
          "simpleoctets:white_wool_octet"
        ],
        [
          "simpleoctets:white_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:white_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:white_wool_octet_from_white_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:white_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/white_wool_octet_from_white_wool_stonecutting.json",
      "ingredients": [
        "minecraft:white_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:white_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          }
        ]
      }
    },
    {
      "id": "simpleoctets:yellow_concrete_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:yellow_concrete",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/yellow_concrete_from_octets.json",
      "ingredients": [
        "simpleoctets:yellow_concrete_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:yellow_concrete_octet"
        ],
        [
          "simpleoctets:yellow_concrete_octet"
        ],
        [
          "simpleoctets:yellow_concrete_octet"
        ],
        [
          "simpleoctets:yellow_concrete_octet"
        ],
        [
          "simpleoctets:yellow_concrete_octet"
        ],
        [
          "simpleoctets:yellow_concrete_octet"
        ],
        [
          "simpleoctets:yellow_concrete_octet"
        ],
        [
          "simpleoctets:yellow_concrete_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:yellow_concrete_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:yellow_concrete_octet_from_yellow_concrete_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:yellow_concrete_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/yellow_concrete_octet_from_yellow_concrete_stonecutting.json",
      "ingredients": [
        "minecraft:yellow_concrete"
      ],
      "slots": {
        "ingredient": [
          "minecraft:yellow_concrete"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:yellow_concrete",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simpleoctets:yellow_terracotta_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:yellow_terracotta",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/yellow_terracotta_from_octets.json",
      "ingredients": [
        "simpleoctets:yellow_terracotta_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:yellow_terracotta_octet"
        ],
        [
          "simpleoctets:yellow_terracotta_octet"
        ],
        [
          "simpleoctets:yellow_terracotta_octet"
        ],
        [
          "simpleoctets:yellow_terracotta_octet"
        ],
        [
          "simpleoctets:yellow_terracotta_octet"
        ],
        [
          "simpleoctets:yellow_terracotta_octet"
        ],
        [
          "simpleoctets:yellow_terracotta_octet"
        ],
        [
          "simpleoctets:yellow_terracotta_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:yellow_terracotta_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:yellow_terracotta_octet_from_yellow_terracotta_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:yellow_terracotta_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/yellow_terracotta_octet_from_yellow_terracotta_stonecutting.json",
      "ingredients": [
        "minecraft:yellow_terracotta"
      ],
      "slots": {
        "ingredient": [
          "minecraft:yellow_terracotta"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:clay_ball",
            "count": 4
          },
          {
            "id": "minecraft:dandelion",
            "count": 0.125
          }
        ]
      }
    },
    {
      "id": "simpleoctets:yellow_wool_from_octets",
      "type": "minecraft:crafting_shapeless",
      "category": "building",
      "group": null,
      "result": {
        "id": "minecraft:yellow_wool",
        "count": 1
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/yellow_wool_from_octets.json",
      "ingredients": [
        "simpleoctets:yellow_wool_octet"
      ],
      "ingredientGroups": [
        [
          "simpleoctets:yellow_wool_octet"
        ],
        [
          "simpleoctets:yellow_wool_octet"
        ],
        [
          "simpleoctets:yellow_wool_octet"
        ],
        [
          "simpleoctets:yellow_wool_octet"
        ],
        [
          "simpleoctets:yellow_wool_octet"
        ],
        [
          "simpleoctets:yellow_wool_octet"
        ],
        [
          "simpleoctets:yellow_wool_octet"
        ],
        [
          "simpleoctets:yellow_wool_octet"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simpleoctets:yellow_wool_octet",
            "count": 8
          }
        ]
      }
    },
    {
      "id": "simpleoctets:yellow_wool_octet_from_yellow_wool_stonecutting",
      "type": "minecraft:stonecutting",
      "category": null,
      "group": null,
      "result": {
        "id": "simpleoctets:yellow_wool_octet",
        "count": 8
      },
      "source": "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/yellow_wool_octet_from_yellow_wool_stonecutting.json",
      "ingredients": [
        "minecraft:yellow_wool"
      ],
      "slots": {
        "ingredient": [
          "minecraft:yellow_wool"
        ]
      },
      "baseMaterials": {
        "yield": 8,
        "materials": [
          {
            "id": "minecraft:string",
            "count": 4
          },
          {
            "id": "minecraft:dandelion",
            "count": 1
          }
        ]
      }
    }
  ],
  "lootTables": [],
  "tags": [],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "simpleoctets:guide_book",
      "name": {
        "en_us": "Simple Octets Guide",
        "de_de": "Simple-Octets-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/guide/OctetsGuide.java",
          "modules/simpleoctets/shared/resources/data/simpleoctets/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 13 pages taken from this wiki, shown in your language. Shapeless recipe: book + stonecutter. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Octets\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 13 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Steinmetz. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Octets\" eins."
        }
      },
      "texture": "assets/textures/simpleoctets/item/guide_book.png",
      "craftedBy": [
        "simpleoctets:guide_book"
      ],
      "usedIn": []
    }
  ],
  "blocks": [
    {
      "id": "simpleoctets:acacia_log_octet",
      "name": {
        "en_us": "Acacia Log Octet Cell",
        "de_de": "Akazienstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:acacia_log_octet_from_acacia_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:acacia_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:acacia_octet",
      "name": {
        "en_us": "Acacia Octet Cell",
        "de_de": "Akazien-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:acacia_octet_from_acacia_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:acacia_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:andesite_octet",
      "name": {
        "en_us": "Andesite Octet Cell",
        "de_de": "Andesit-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:andesite_octet_from_andesite_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:andesite_from_octets"
      ]
    },
    {
      "id": "simpleoctets:ashwood_log_octet",
      "name": {
        "en_us": "Ashwood Log Octet Cell",
        "de_de": "Aschstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:ashwood_log_octet_from_ashwood_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:ashwood_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:ashwood_octet",
      "name": {
        "en_us": "Ashwood Octet Cell",
        "de_de": "Aschholz-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:ashwood_octet_from_ashwood_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:ashwood_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:astralit_bricks_octet",
      "name": {
        "en_us": "Astralit Bricks Octet Cell",
        "de_de": "Astralitziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:astralit_bricks_octet_from_astralit_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:astralit_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:bamboo_mosaic_octet",
      "name": {
        "en_us": "Bamboo Mosaic Octet Cell",
        "de_de": "Bambusmosaik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:bamboo_mosaic_octet_from_bamboo_mosaic_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:bamboo_mosaic_from_octets"
      ]
    },
    {
      "id": "simpleoctets:bamboo_octet",
      "name": {
        "en_us": "Bamboo Octet Cell",
        "de_de": "Bambus-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:bamboo_octet_from_bamboo_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:bamboo_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:birch_log_octet",
      "name": {
        "en_us": "Birch Log Octet Cell",
        "de_de": "Birkenstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:birch_log_octet_from_birch_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:birch_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:birch_octet",
      "name": {
        "en_us": "Birch Octet Cell",
        "de_de": "Birken-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:birch_octet_from_birch_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:birch_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:black_concrete_octet",
      "name": {
        "en_us": "Black Concrete Octet Cell",
        "de_de": "Schwarzer Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:black_concrete_octet_from_black_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:black_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:black_terracotta_octet",
      "name": {
        "en_us": "Black Terracotta Octet Cell",
        "de_de": "Schwarze Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:black_terracotta_octet_from_black_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:black_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:black_wool_octet",
      "name": {
        "en_us": "Black Wool Octet Cell",
        "de_de": "Schwarze Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:black_wool_octet_from_black_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:black_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:blackstone_block_octet",
      "name": {
        "en_us": "Blackstone Block Octet Cell",
        "de_de": "Schwarzstein-Block-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:blackstone_block_octet_from_blackstone_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:blackstone_from_octets"
      ]
    },
    {
      "id": "simpleoctets:blue_concrete_octet",
      "name": {
        "en_us": "Blue Concrete Octet Cell",
        "de_de": "Blauer Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:blue_concrete_octet_from_blue_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:blue_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:blue_nether_bricks_octet",
      "name": {
        "en_us": "Blue Nether Bricks Octet Cell",
        "de_de": "Blaue Netherziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:blue_nether_bricks_octet_from_blue_nether_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:blue_nether_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:blue_terracotta_octet",
      "name": {
        "en_us": "Blue Terracotta Octet Cell",
        "de_de": "Blaue Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:blue_terracotta_octet_from_blue_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:blue_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:blue_wool_octet",
      "name": {
        "en_us": "Blue Wool Octet Cell",
        "de_de": "Blaue Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:blue_wool_octet_from_blue_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:blue_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:bone_block_octet",
      "name": {
        "en_us": "Bone Block Octet Cell",
        "de_de": "Knochenblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:bone_block_octet_from_bone_block_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:bone_block_from_octets"
      ]
    },
    {
      "id": "simpleoctets:bricks_octet",
      "name": {
        "en_us": "Bricks Octet Cell",
        "de_de": "Ziegelsteine-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:bricks_octet_from_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:brown_concrete_octet",
      "name": {
        "en_us": "Brown Concrete Octet Cell",
        "de_de": "Brauner Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:brown_concrete_octet_from_brown_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:brown_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:brown_terracotta_octet",
      "name": {
        "en_us": "Brown Terracotta Octet Cell",
        "de_de": "Braune Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:brown_terracotta_octet_from_brown_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:brown_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:brown_wool_octet",
      "name": {
        "en_us": "Brown Wool Octet Cell",
        "de_de": "Braune Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:brown_wool_octet_from_brown_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:brown_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:calcite_octet",
      "name": {
        "en_us": "Calcite Octet Cell",
        "de_de": "Kalzit-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:calcite_octet_from_calcite_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:calcite_from_octets"
      ]
    },
    {
      "id": "simpleoctets:cherry_log_octet",
      "name": {
        "en_us": "Cherry Log Octet Cell",
        "de_de": "Kirschstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:cherry_log_octet_from_cherry_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:cherry_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:cherry_octet",
      "name": {
        "en_us": "Cherry Octet Cell",
        "de_de": "Kirsch-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:cherry_octet_from_cherry_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:cherry_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:cinnabar_bricks_octet",
      "name": {
        "en_us": "Cinnabar Bricks Octet Cell",
        "de_de": "Zinnoberziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:cinnabar_bricks_octet_from_cinnabar_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:cinnabar_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:cinnabar_octet",
      "name": {
        "en_us": "Cinnabar Octet Cell",
        "de_de": "Zinnober-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:cinnabar_octet_from_cinnabar_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:cinnabar_from_octets"
      ]
    },
    {
      "id": "simpleoctets:coal_block_octet",
      "name": {
        "en_us": "Block of Coal Octet Cell",
        "de_de": "Kohleblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:coal_block_octet_from_coal_block_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:coal_block_from_octets"
      ]
    },
    {
      "id": "simpleoctets:cobbled_deepslate_octet",
      "name": {
        "en_us": "Cobbled Deepslate Octet Cell",
        "de_de": "Bruchtiefenschiefer-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:cobbled_deepslate_octet_from_cobbled_deepslate_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:cobbled_deepslate_from_octets"
      ]
    },
    {
      "id": "simpleoctets:cobblestone_octet",
      "name": {
        "en_us": "Cobblestone Octet Cell",
        "de_de": "Bruchstein-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:cobblestone_octet_from_cobblestone_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:cobblestone_from_octets"
      ]
    },
    {
      "id": "simpleoctets:copper_block_octet",
      "name": {
        "en_us": "Block of Copper Octet Cell",
        "de_de": "Kupferblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:copper_block_octet_from_copper_block_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:copper_block_from_octets"
      ]
    },
    {
      "id": "simpleoctets:crimson_octet",
      "name": {
        "en_us": "Crimson Octet Cell",
        "de_de": "Karmesin-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:crimson_octet_from_crimson_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:crimson_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:crimson_stem_octet",
      "name": {
        "en_us": "Crimson Stem Octet Cell",
        "de_de": "Karmesinstiel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:crimson_stem_octet_from_crimson_stem_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:crimson_stem_from_octets"
      ]
    },
    {
      "id": "simpleoctets:cut_copper_octet",
      "name": {
        "en_us": "Cut Copper Octet Cell",
        "de_de": "Geschnittener Kupferblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:cut_copper_octet_from_cut_copper_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:cut_copper_from_octets"
      ]
    },
    {
      "id": "simpleoctets:cyan_concrete_octet",
      "name": {
        "en_us": "Cyan Concrete Octet Cell",
        "de_de": "Türkiser Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:cyan_concrete_octet_from_cyan_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:cyan_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:cyan_terracotta_octet",
      "name": {
        "en_us": "Cyan Terracotta Octet Cell",
        "de_de": "Türkise Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:cyan_terracotta_octet_from_cyan_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:cyan_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:cyan_wool_octet",
      "name": {
        "en_us": "Cyan Wool Octet Cell",
        "de_de": "Türkise Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:cyan_wool_octet_from_cyan_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:cyan_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:dark_oak_log_octet",
      "name": {
        "en_us": "Dark Oak Log Octet Cell",
        "de_de": "Schwarzeichenstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:dark_oak_log_octet_from_dark_oak_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:dark_oak_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:dark_oak_octet",
      "name": {
        "en_us": "Dark Oak Octet Cell",
        "de_de": "Schwarzeichen-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:dark_oak_octet_from_dark_oak_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:dark_oak_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:dark_prismarine_octet",
      "name": {
        "en_us": "Dark Prismarine Octet Cell",
        "de_de": "Dunkler Prismarin-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:dark_prismarine_octet_from_dark_prismarine_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:dark_prismarine_from_octets"
      ]
    },
    {
      "id": "simpleoctets:deepslate_bricks_octet",
      "name": {
        "en_us": "Deepslate Bricks Octet Cell",
        "de_de": "Tiefenschieferziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:deepslate_bricks_octet_from_deepslate_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:deepslate_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:deepslate_tiles_octet",
      "name": {
        "en_us": "Deepslate Tiles Octet Cell",
        "de_de": "Tiefenschieferfliesen-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:deepslate_tiles_octet_from_deepslate_tiles_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:deepslate_tiles_from_octets"
      ]
    },
    {
      "id": "simpleoctets:diamond_block_octet",
      "name": {
        "en_us": "Block of Diamond Octet Cell",
        "de_de": "Diamantblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:diamond_block_octet_from_diamond_block_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:diamond_block_from_octets"
      ]
    },
    {
      "id": "simpleoctets:diorite_octet",
      "name": {
        "en_us": "Diorite Octet Cell",
        "de_de": "Diorit-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:diorite_octet_from_diorite_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:diorite_from_octets"
      ]
    },
    {
      "id": "simpleoctets:duskwood_log_octet",
      "name": {
        "en_us": "Duskwood Log Octet Cell",
        "de_de": "Dämmerstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:duskwood_log_octet_from_duskwood_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:duskwood_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:duskwood_octet",
      "name": {
        "en_us": "Duskwood Octet Cell",
        "de_de": "Dämmerholz-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:duskwood_octet_from_duskwood_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:duskwood_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:emerald_block_octet",
      "name": {
        "en_us": "Block of Emerald Octet Cell",
        "de_de": "Smaragdblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:emerald_block_octet_from_emerald_block_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:emerald_block_from_octets"
      ]
    },
    {
      "id": "simpleoctets:end_stone_bricks_octet",
      "name": {
        "en_us": "End Stone Bricks Octet Cell",
        "de_de": "Endsteinziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:end_stone_bricks_octet_from_end_stone_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:end_stone_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:end_stone_octet",
      "name": {
        "en_us": "End Stone Octet Cell",
        "de_de": "Endstein-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:end_stone_octet_from_end_stone_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:end_stone_from_octets"
      ]
    },
    {
      "id": "simpleoctets:ender_quartz_block_octet",
      "name": {
        "en_us": "Block of Ender Quartz Octet Cell",
        "de_de": "Enderquarzblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:ender_quartz_block_octet_from_ender_quartz_block_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:ender_quartz_block_from_octets"
      ]
    },
    {
      "id": "simpleoctets:ender_quartz_bricks_octet",
      "name": {
        "en_us": "Ender Quartz Bricks Octet Cell",
        "de_de": "Enderquarzziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:ender_quartz_bricks_octet_from_ender_quartz_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:ender_quartz_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:exposed_copper_octet",
      "name": {
        "en_us": "Exposed Copper Octet Cell",
        "de_de": "Angelaufener Kupferblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:exposed_copper_octet_from_exposed_copper_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:exposed_copper_from_octets"
      ]
    },
    {
      "id": "simpleoctets:exposed_cut_copper_octet",
      "name": {
        "en_us": "Exposed Cut Copper Octet Cell",
        "de_de": "Angelaufener geschnittener Kupferblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:exposed_cut_copper_octet_from_exposed_cut_copper_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:exposed_cut_copper_from_octets"
      ]
    },
    {
      "id": "simpleoctets:gold_block_octet",
      "name": {
        "en_us": "Block of Gold Octet Cell",
        "de_de": "Goldblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:gold_block_octet_from_gold_block_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:gold_block_from_octets"
      ]
    },
    {
      "id": "simpleoctets:granite_octet",
      "name": {
        "en_us": "Granite Octet Cell",
        "de_de": "Granit-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:granite_octet_from_granite_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:granite_from_octets"
      ]
    },
    {
      "id": "simpleoctets:gray_concrete_octet",
      "name": {
        "en_us": "Gray Concrete Octet Cell",
        "de_de": "Grauer Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:gray_concrete_octet_from_gray_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:gray_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:gray_terracotta_octet",
      "name": {
        "en_us": "Gray Terracotta Octet Cell",
        "de_de": "Graue Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:gray_terracotta_octet_from_gray_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:gray_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:gray_wool_octet",
      "name": {
        "en_us": "Gray Wool Octet Cell",
        "de_de": "Graue Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:gray_wool_octet_from_gray_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:gray_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:green_concrete_octet",
      "name": {
        "en_us": "Green Concrete Octet Cell",
        "de_de": "Grüner Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:green_concrete_octet_from_green_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:green_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:green_terracotta_octet",
      "name": {
        "en_us": "Green Terracotta Octet Cell",
        "de_de": "Grüne Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:green_terracotta_octet_from_green_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:green_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:green_wool_octet",
      "name": {
        "en_us": "Green Wool Octet Cell",
        "de_de": "Grüne Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:green_wool_octet_from_green_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:green_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:hushed_deepslate_tiles_octet",
      "name": {
        "en_us": "Hushed Deepslate Tiles Octet Cell",
        "de_de": "Gedämpfte Tiefenschieferfliesen-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:hushed_deepslate_tiles_octet_from_hushed_deepslate_tiles_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:hushed_deepslate_tiles_from_octets"
      ]
    },
    {
      "id": "simpleoctets:iron_block_octet",
      "name": {
        "en_us": "Block of Iron Octet Cell",
        "de_de": "Eisenblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:iron_block_octet_from_iron_block_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:iron_block_from_octets"
      ]
    },
    {
      "id": "simpleoctets:jungle_log_octet",
      "name": {
        "en_us": "Jungle Log Octet Cell",
        "de_de": "Tropenbaumstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:jungle_log_octet_from_jungle_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:jungle_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:jungle_octet",
      "name": {
        "en_us": "Jungle Octet Cell",
        "de_de": "Tropenholz-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:jungle_octet_from_jungle_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:jungle_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:lapis_block_octet",
      "name": {
        "en_us": "Block of Lapis Lazuli Octet Cell",
        "de_de": "Lapislazuliblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:lapis_block_octet_from_lapis_block_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:lapis_block_from_octets"
      ]
    },
    {
      "id": "simpleoctets:light_blue_concrete_octet",
      "name": {
        "en_us": "Light Blue Concrete Octet Cell",
        "de_de": "Hellblauer Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:light_blue_concrete_octet_from_light_blue_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:light_blue_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:light_blue_terracotta_octet",
      "name": {
        "en_us": "Light Blue Terracotta Octet Cell",
        "de_de": "Hellblaue Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:light_blue_terracotta_octet_from_light_blue_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:light_blue_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:light_blue_wool_octet",
      "name": {
        "en_us": "Light Blue Wool Octet Cell",
        "de_de": "Hellblaue Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:light_blue_wool_octet_from_light_blue_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:light_blue_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:light_gray_concrete_octet",
      "name": {
        "en_us": "Light Gray Concrete Octet Cell",
        "de_de": "Hellgrauer Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:light_gray_concrete_octet_from_light_gray_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:light_gray_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:light_gray_terracotta_octet",
      "name": {
        "en_us": "Light Gray Terracotta Octet Cell",
        "de_de": "Hellgraue Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:light_gray_terracotta_octet_from_light_gray_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:light_gray_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:light_gray_wool_octet",
      "name": {
        "en_us": "Light Gray Wool Octet Cell",
        "de_de": "Hellgraue Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:light_gray_wool_octet_from_light_gray_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:light_gray_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:lime_concrete_octet",
      "name": {
        "en_us": "Lime Concrete Octet Cell",
        "de_de": "Hellgrüner Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:lime_concrete_octet_from_lime_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:lime_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:lime_terracotta_octet",
      "name": {
        "en_us": "Lime Terracotta Octet Cell",
        "de_de": "Hellgrüne Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:lime_terracotta_octet_from_lime_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:lime_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:lime_wool_octet",
      "name": {
        "en_us": "Lime Wool Octet Cell",
        "de_de": "Hellgrüne Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:lime_wool_octet_from_lime_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:lime_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:magenta_concrete_octet",
      "name": {
        "en_us": "Magenta Concrete Octet Cell",
        "de_de": "Magenta Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:magenta_concrete_octet_from_magenta_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:magenta_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:magenta_terracotta_octet",
      "name": {
        "en_us": "Magenta Terracotta Octet Cell",
        "de_de": "Magenta Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:magenta_terracotta_octet_from_magenta_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:magenta_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:magenta_wool_octet",
      "name": {
        "en_us": "Magenta Wool Octet Cell",
        "de_de": "Magenta Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:magenta_wool_octet_from_magenta_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:magenta_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:mangrove_log_octet",
      "name": {
        "en_us": "Mangrove Log Octet Cell",
        "de_de": "Mangrovenstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:mangrove_log_octet_from_mangrove_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:mangrove_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:mangrove_octet",
      "name": {
        "en_us": "Mangrove Octet Cell",
        "de_de": "Mangroven-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:mangrove_octet_from_mangrove_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:mangrove_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:melon_octet",
      "name": {
        "en_us": "Melon Octet Cell",
        "de_de": "Melonen-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simpleoctets:mixed_octet",
      "name": {
        "en_us": "Mixed Octet Cell",
        "de_de": "Gemischte Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simpleoctets:mossy_cobblestone_octet",
      "name": {
        "en_us": "Mossy Cobblestone Octet Cell",
        "de_de": "Bemooster Bruchstein-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:mossy_cobblestone_octet_from_mossy_cobblestone_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:mossy_cobblestone_from_octets"
      ]
    },
    {
      "id": "simpleoctets:mossy_stone_bricks_octet",
      "name": {
        "en_us": "Mossy Stone Bricks Octet Cell",
        "de_de": "Bemooste Steinziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:mossy_stone_bricks_octet_from_mossy_stone_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:mossy_stone_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:mud_bricks_octet",
      "name": {
        "en_us": "Mud Bricks Octet Cell",
        "de_de": "Schlammziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:mud_bricks_octet_from_mud_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:mud_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:nether_bricks_octet",
      "name": {
        "en_us": "Nether Bricks Octet Cell",
        "de_de": "Netherziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:nether_bricks_octet_from_nether_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:nether_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:netherite_block_octet",
      "name": {
        "en_us": "Block of Netherite Octet Cell",
        "de_de": "Netheritblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:netherite_block_octet_from_netherite_block_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:netherite_block_from_octets"
      ]
    },
    {
      "id": "simpleoctets:netherrack_octet",
      "name": {
        "en_us": "Netherrack Octet Cell",
        "de_de": "Netherrack-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:netherrack_octet_from_netherrack_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:netherrack_from_octets"
      ]
    },
    {
      "id": "simpleoctets:nihilith_bricks_octet",
      "name": {
        "en_us": "Nihilit Bricks Octet Cell",
        "de_de": "Nihilitziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:nihilith_bricks_octet_from_nihilith_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:nihilith_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:oak_log_octet",
      "name": {
        "en_us": "Oak Log Octet Cell",
        "de_de": "Eichenstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:oak_log_octet_from_oak_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:oak_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:oak_octet",
      "name": {
        "en_us": "Oak Octet Cell",
        "de_de": "Eichen-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:oak_octet_from_oak_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:oak_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:obsidian_octet",
      "name": {
        "en_us": "Obsidian Octet Cell",
        "de_de": "Obsidian-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:obsidian_octet_from_obsidian_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:obsidian_from_octets"
      ]
    },
    {
      "id": "simpleoctets:orange_concrete_octet",
      "name": {
        "en_us": "Orange Concrete Octet Cell",
        "de_de": "Oranger Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:orange_concrete_octet_from_orange_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:orange_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:orange_terracotta_octet",
      "name": {
        "en_us": "Orange Terracotta Octet Cell",
        "de_de": "Orange Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:orange_terracotta_octet_from_orange_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:orange_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:orange_wool_octet",
      "name": {
        "en_us": "Orange Wool Octet Cell",
        "de_de": "Orange Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:orange_wool_octet_from_orange_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:orange_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:oxidized_copper_octet",
      "name": {
        "en_us": "Oxidized Copper Octet Cell",
        "de_de": "Oxidierter Kupferblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:oxidized_copper_octet_from_oxidized_copper_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:oxidized_copper_from_octets"
      ]
    },
    {
      "id": "simpleoctets:oxidized_cut_copper_octet",
      "name": {
        "en_us": "Oxidized Cut Copper Octet Cell",
        "de_de": "Oxidierter geschnittener Kupferblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:oxidized_cut_copper_octet_from_oxidized_cut_copper_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:oxidized_cut_copper_from_octets"
      ]
    },
    {
      "id": "simpleoctets:pale_oak_log_octet",
      "name": {
        "en_us": "Pale Oak Log Octet Cell",
        "de_de": "Blasseichenstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:pale_oak_log_octet_from_pale_oak_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:pale_oak_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:pale_oak_octet",
      "name": {
        "en_us": "Pale Oak Octet Cell",
        "de_de": "Blasseichen-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:pale_oak_octet_from_pale_oak_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:pale_oak_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:pink_concrete_octet",
      "name": {
        "en_us": "Pink Concrete Octet Cell",
        "de_de": "Rosa Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:pink_concrete_octet_from_pink_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:pink_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:pink_terracotta_octet",
      "name": {
        "en_us": "Pink Terracotta Octet Cell",
        "de_de": "Rosa Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:pink_terracotta_octet_from_pink_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:pink_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:pink_wool_octet",
      "name": {
        "en_us": "Pink Wool Octet Cell",
        "de_de": "Rosa Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:pink_wool_octet_from_pink_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:pink_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:polished_andesite_octet",
      "name": {
        "en_us": "Polished Andesite Octet Cell",
        "de_de": "Polierter Andesit-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:polished_andesite_octet_from_polished_andesite_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:polished_andesite_from_octets"
      ]
    },
    {
      "id": "simpleoctets:polished_astralit_block_octet",
      "name": {
        "en_us": "Polished Astralit Block Octet Cell",
        "de_de": "Polierter Astralit-Block-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:polished_astralit_block_octet_from_polished_astralit_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:polished_astralit_from_octets"
      ]
    },
    {
      "id": "simpleoctets:polished_blackstone_bricks_octet",
      "name": {
        "en_us": "Polished Blackstone Bricks Octet Cell",
        "de_de": "Polierte Schwarzsteinziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:polished_blackstone_bricks_octet_from_polished_blackstone_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:polished_blackstone_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:polished_blackstone_octet",
      "name": {
        "en_us": "Polished Blackstone Octet Cell",
        "de_de": "Polierter Schwarzstein-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:polished_blackstone_octet_from_polished_blackstone_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:polished_blackstone_from_octets"
      ]
    },
    {
      "id": "simpleoctets:polished_cinnabar_octet",
      "name": {
        "en_us": "Polished Cinnabar Octet Cell",
        "de_de": "Polierter Zinnober-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:polished_cinnabar_octet_from_polished_cinnabar_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:polished_cinnabar_from_octets"
      ]
    },
    {
      "id": "simpleoctets:polished_deepslate_octet",
      "name": {
        "en_us": "Polished Deepslate Octet Cell",
        "de_de": "Polierter Tiefenschiefer-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:polished_deepslate_octet_from_polished_deepslate_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:polished_deepslate_from_octets"
      ]
    },
    {
      "id": "simpleoctets:polished_diorite_octet",
      "name": {
        "en_us": "Polished Diorite Octet Cell",
        "de_de": "Polierter Diorit-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:polished_diorite_octet_from_polished_diorite_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:polished_diorite_from_octets"
      ]
    },
    {
      "id": "simpleoctets:polished_ender_quartz_block_octet",
      "name": {
        "en_us": "Polished Ender Quartz Block Octet Cell",
        "de_de": "Polierter Enderquarz-Block-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:polished_ender_quartz_block_octet_from_polished_ender_quartz_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:polished_ender_quartz_from_octets"
      ]
    },
    {
      "id": "simpleoctets:polished_granite_octet",
      "name": {
        "en_us": "Polished Granite Octet Cell",
        "de_de": "Polierter Granit-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:polished_granite_octet_from_polished_granite_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:polished_granite_from_octets"
      ]
    },
    {
      "id": "simpleoctets:polished_nihilith_block_octet",
      "name": {
        "en_us": "Polished Nihilit Block Octet Cell",
        "de_de": "Polierter Nihilit-Block-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:polished_nihilith_block_octet_from_polished_nihilith_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:polished_nihilith_from_octets"
      ]
    },
    {
      "id": "simpleoctets:polished_sulfur_octet",
      "name": {
        "en_us": "Polished Sulfur Octet Cell",
        "de_de": "Polierter Schwefel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:polished_sulfur_octet_from_polished_sulfur_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:polished_sulfur_from_octets"
      ]
    },
    {
      "id": "simpleoctets:polished_tuff_octet",
      "name": {
        "en_us": "Polished Tuff Octet Cell",
        "de_de": "Polierter Tuffstein-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:polished_tuff_octet_from_polished_tuff_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:polished_tuff_from_octets"
      ]
    },
    {
      "id": "simpleoctets:poplar_log_octet",
      "name": {
        "en_us": "Poplar Log Octet Cell",
        "de_de": "Pappelstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:poplar_log_octet_from_poplar_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:poplar_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:poplar_octet",
      "name": {
        "en_us": "Poplar Octet Cell",
        "de_de": "Pappel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:poplar_octet_from_poplar_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:poplar_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:prismarine_bricks_octet",
      "name": {
        "en_us": "Prismarine Bricks Octet Cell",
        "de_de": "Prismarinziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:prismarine_bricks_octet_from_prismarine_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:prismarine_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:prismarine_octet",
      "name": {
        "en_us": "Prismarine Octet Cell",
        "de_de": "Prismarin-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:prismarine_octet_from_prismarine_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:prismarine_from_octets"
      ]
    },
    {
      "id": "simpleoctets:purple_concrete_octet",
      "name": {
        "en_us": "Purple Concrete Octet Cell",
        "de_de": "Violetter Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:purple_concrete_octet_from_purple_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:purple_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:purple_terracotta_octet",
      "name": {
        "en_us": "Purple Terracotta Octet Cell",
        "de_de": "Violette Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:purple_terracotta_octet_from_purple_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:purple_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:purple_wool_octet",
      "name": {
        "en_us": "Purple Wool Octet Cell",
        "de_de": "Violette Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:purple_wool_octet_from_purple_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:purple_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:purpur_block_octet",
      "name": {
        "en_us": "Purpur Block Octet Cell",
        "de_de": "Purpurblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:purpur_block_octet_from_purpur_block_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:purpur_block_from_octets"
      ]
    },
    {
      "id": "simpleoctets:quartz_block_octet",
      "name": {
        "en_us": "Block of Quartz Octet Cell",
        "de_de": "Quarzblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:quartz_block_octet_from_quartz_block_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:quartz_block_from_octets"
      ]
    },
    {
      "id": "simpleoctets:red_concrete_octet",
      "name": {
        "en_us": "Red Concrete Octet Cell",
        "de_de": "Roter Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:red_concrete_octet_from_red_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:red_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:red_nether_bricks_octet",
      "name": {
        "en_us": "Red Nether Bricks Octet Cell",
        "de_de": "Rote Netherziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:red_nether_bricks_octet_from_red_nether_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:red_nether_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:red_sandstone_octet",
      "name": {
        "en_us": "Red Sandstone Octet Cell",
        "de_de": "Roter Sandstein-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:red_sandstone_octet_from_red_sandstone_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:red_sandstone_from_octets"
      ]
    },
    {
      "id": "simpleoctets:red_terracotta_octet",
      "name": {
        "en_us": "Red Terracotta Octet Cell",
        "de_de": "Rote Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:red_terracotta_octet_from_red_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:red_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:red_wool_octet",
      "name": {
        "en_us": "Red Wool Octet Cell",
        "de_de": "Rote Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:red_wool_octet_from_red_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:red_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:resin_bricks_octet",
      "name": {
        "en_us": "Resin Bricks Octet Cell",
        "de_de": "Harzziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:resin_bricks_octet_from_resin_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:resin_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:sandstone_octet",
      "name": {
        "en_us": "Sandstone Octet Cell",
        "de_de": "Sandstein-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:sandstone_octet_from_sandstone_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:sandstone_from_octets"
      ]
    },
    {
      "id": "simpleoctets:smooth_basalt_octet",
      "name": {
        "en_us": "Smooth Basalt Octet Cell",
        "de_de": "Glatter Basalt-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:smooth_basalt_octet_from_smooth_basalt_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:smooth_basalt_from_octets"
      ]
    },
    {
      "id": "simpleoctets:smooth_quartz_octet",
      "name": {
        "en_us": "Smooth Quartz Block Octet Cell",
        "de_de": "Glatter Quarzblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:smooth_quartz_octet_from_smooth_quartz_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:smooth_quartz_from_octets"
      ]
    },
    {
      "id": "simpleoctets:smooth_red_sandstone_octet",
      "name": {
        "en_us": "Smooth Red Sandstone Octet Cell",
        "de_de": "Glatter roter Sandstein-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:smooth_red_sandstone_octet_from_smooth_red_sandstone_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:smooth_red_sandstone_from_octets"
      ]
    },
    {
      "id": "simpleoctets:smooth_sandstone_octet",
      "name": {
        "en_us": "Smooth Sandstone Octet Cell",
        "de_de": "Glatter Sandstein-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:smooth_sandstone_octet_from_smooth_sandstone_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:smooth_sandstone_from_octets"
      ]
    },
    {
      "id": "simpleoctets:spruce_log_octet",
      "name": {
        "en_us": "Spruce Log Octet Cell",
        "de_de": "Fichtenstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:spruce_log_octet_from_spruce_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:spruce_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:spruce_octet",
      "name": {
        "en_us": "Spruce Octet Cell",
        "de_de": "Fichten-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:spruce_octet_from_spruce_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:spruce_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stone_bricks_octet",
      "name": {
        "en_us": "Stone Bricks Octet Cell",
        "de_de": "Steinziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stone_bricks_octet_from_stone_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stone_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stone_octet",
      "name": {
        "en_us": "Stone Octet Cell",
        "de_de": "Stein-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stone_octet_from_stone_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stone_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_acacia_log_octet",
      "name": {
        "en_us": "Stripped Acacia Log Octet Cell",
        "de_de": "Entrindeter Akazienstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_acacia_log_octet_from_stripped_acacia_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_acacia_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_ashwood_log_octet",
      "name": {
        "en_us": "Stripped Ashwood Log Octet Cell",
        "de_de": "Entrindeter Aschstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_ashwood_log_octet_from_stripped_ashwood_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_ashwood_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_birch_log_octet",
      "name": {
        "en_us": "Stripped Birch Log Octet Cell",
        "de_de": "Entrindeter Birkenstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_birch_log_octet_from_stripped_birch_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_birch_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_cherry_log_octet",
      "name": {
        "en_us": "Stripped Cherry Log Octet Cell",
        "de_de": "Entrindeter Kirschstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_cherry_log_octet_from_stripped_cherry_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_cherry_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_crimson_stem_octet",
      "name": {
        "en_us": "Stripped Crimson Stem Octet Cell",
        "de_de": "Geschälter Karmesinstiel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_crimson_stem_octet_from_stripped_crimson_stem_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_crimson_stem_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_dark_oak_log_octet",
      "name": {
        "en_us": "Stripped Dark Oak Log Octet Cell",
        "de_de": "Entrindeter Schwarzeichenstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_dark_oak_log_octet_from_stripped_dark_oak_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_dark_oak_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_duskwood_log_octet",
      "name": {
        "en_us": "Stripped Duskwood Log Octet Cell",
        "de_de": "Entrindeter Dämmerstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_duskwood_log_octet_from_stripped_duskwood_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_duskwood_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_jungle_log_octet",
      "name": {
        "en_us": "Stripped Jungle Log Octet Cell",
        "de_de": "Entrindeter Tropenbaumstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_jungle_log_octet_from_stripped_jungle_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_jungle_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_mangrove_log_octet",
      "name": {
        "en_us": "Stripped Mangrove Log Octet Cell",
        "de_de": "Entrindeter Mangrovenstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_mangrove_log_octet_from_stripped_mangrove_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_mangrove_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_oak_log_octet",
      "name": {
        "en_us": "Stripped Oak Log Octet Cell",
        "de_de": "Entrindeter Eichenstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_oak_log_octet_from_stripped_oak_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_oak_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_pale_oak_log_octet",
      "name": {
        "en_us": "Stripped Pale Oak Log Octet Cell",
        "de_de": "Entrindeter Blasseichenstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_pale_oak_log_octet_from_stripped_pale_oak_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_pale_oak_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_poplar_log_octet",
      "name": {
        "en_us": "Stripped Poplar Log Octet Cell",
        "de_de": "Entrindeter Pappelstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_poplar_log_octet_from_stripped_poplar_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_poplar_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_spruce_log_octet",
      "name": {
        "en_us": "Stripped Spruce Log Octet Cell",
        "de_de": "Entrindeter Fichtenstamm-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_spruce_log_octet_from_stripped_spruce_log_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_spruce_log_from_octets"
      ]
    },
    {
      "id": "simpleoctets:stripped_warped_stem_octet",
      "name": {
        "en_us": "Stripped Warped Stem Octet Cell",
        "de_de": "Geschälter Wirrstiel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:stripped_warped_stem_octet_from_stripped_warped_stem_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:stripped_warped_stem_from_octets"
      ]
    },
    {
      "id": "simpleoctets:sulfur_bricks_octet",
      "name": {
        "en_us": "Sulfur Bricks Octet Cell",
        "de_de": "Schwefelziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:sulfur_bricks_octet_from_sulfur_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:sulfur_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:sulfur_octet",
      "name": {
        "en_us": "Sulfur Octet Cell",
        "de_de": "Schwefel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:sulfur_octet_from_sulfur_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:sulfur_from_octets"
      ]
    },
    {
      "id": "simpleoctets:terracotta_octet",
      "name": {
        "en_us": "Terracotta Octet Cell",
        "de_de": "Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:terracotta_octet_from_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:tuff_bricks_octet",
      "name": {
        "en_us": "Tuff Bricks Octet Cell",
        "de_de": "Tuffsteinziegel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:tuff_bricks_octet_from_tuff_bricks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:tuff_bricks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:tuff_octet",
      "name": {
        "en_us": "Tuff Octet Cell",
        "de_de": "Tuffstein-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:tuff_octet_from_tuff_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:tuff_from_octets"
      ]
    },
    {
      "id": "simpleoctets:warped_octet",
      "name": {
        "en_us": "Warped Octet Cell",
        "de_de": "Wirr-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:warped_octet_from_warped_planks_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:warped_planks_from_octets"
      ]
    },
    {
      "id": "simpleoctets:warped_stem_octet",
      "name": {
        "en_us": "Warped Stem Octet Cell",
        "de_de": "Wirrstiel-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:warped_stem_octet_from_warped_stem_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:warped_stem_from_octets"
      ]
    },
    {
      "id": "simpleoctets:waxed_cut_copper_octet",
      "name": {
        "en_us": "Waxed Cut Copper Octet Cell",
        "de_de": "Gewachster geschnittener Kupferblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:waxed_cut_copper_octet_from_waxed_cut_copper_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:waxed_cut_copper_from_octets"
      ]
    },
    {
      "id": "simpleoctets:waxed_exposed_cut_copper_octet",
      "name": {
        "en_us": "Waxed Exposed Cut Copper Octet Cell",
        "de_de": "Gewachster angelaufener geschnittener Kupferblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:waxed_exposed_cut_copper_octet_from_waxed_exposed_cut_copper_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:waxed_exposed_cut_copper_from_octets"
      ]
    },
    {
      "id": "simpleoctets:waxed_oxidized_cut_copper_octet",
      "name": {
        "en_us": "Waxed Oxidized Cut Copper Octet Cell",
        "de_de": "Gewachster oxidierter geschnittener Kupferblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:waxed_oxidized_cut_copper_octet_from_waxed_oxidized_cut_copper_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:waxed_oxidized_cut_copper_from_octets"
      ]
    },
    {
      "id": "simpleoctets:waxed_weathered_cut_copper_octet",
      "name": {
        "en_us": "Waxed Weathered Cut Copper Octet Cell",
        "de_de": "Gewachster verwitterter geschnittener Kupferblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:waxed_weathered_cut_copper_octet_from_waxed_weathered_cut_copper_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:waxed_weathered_cut_copper_from_octets"
      ]
    },
    {
      "id": "simpleoctets:weathered_copper_octet",
      "name": {
        "en_us": "Weathered Copper Octet Cell",
        "de_de": "Verwitterter Kupferblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:weathered_copper_octet_from_weathered_copper_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:weathered_copper_from_octets"
      ]
    },
    {
      "id": "simpleoctets:weathered_cut_copper_octet",
      "name": {
        "en_us": "Weathered Cut Copper Octet Cell",
        "de_de": "Verwitterter geschnittener Kupferblock-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:weathered_cut_copper_octet_from_weathered_cut_copper_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:weathered_cut_copper_from_octets"
      ]
    },
    {
      "id": "simpleoctets:white_concrete_octet",
      "name": {
        "en_us": "White Concrete Octet Cell",
        "de_de": "Weißer Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:white_concrete_octet_from_white_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:white_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:white_terracotta_octet",
      "name": {
        "en_us": "White Terracotta Octet Cell",
        "de_de": "Weiße Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:white_terracotta_octet_from_white_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:white_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:white_wool_octet",
      "name": {
        "en_us": "White Wool Octet Cell",
        "de_de": "Weiße Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:white_wool_octet_from_white_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:white_wool_from_octets"
      ]
    },
    {
      "id": "simpleoctets:yellow_concrete_octet",
      "name": {
        "en_us": "Yellow Concrete Octet Cell",
        "de_de": "Gelber Beton-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:yellow_concrete_octet_from_yellow_concrete_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:yellow_concrete_from_octets"
      ]
    },
    {
      "id": "simpleoctets:yellow_terracotta_octet",
      "name": {
        "en_us": "Yellow Terracotta Octet Cell",
        "de_de": "Gelbe Keramik-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:yellow_terracotta_octet_from_yellow_terracotta_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:yellow_terracotta_from_octets"
      ]
    },
    {
      "id": "simpleoctets:yellow_wool_octet",
      "name": {
        "en_us": "Yellow Wool Octet Cell",
        "de_de": "Gelbe Wolle-Achtelzelle"
      },
      "note": {
        "sources": [
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCellBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/OctetCells.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/block/MaterialOctetBlock.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/item/MaterialOctetItem.java",
          "modules/simpleoctets/shared/java/com/simpleoctets/registry/OctetRegistry.java"
        ],
        "en": {
          "summary": "An octet (0.5 x 0.5 x 0.5) of its material; right-click places it into the clicked corner. The stonecutter cuts 8 from the full block, 8 craft back into it."
        },
        "de": {
          "summary": "Ein Achtel (0,5 x 0,5 x 0,5) seines Materials; Rechtsklick setzt es in die angeklickte Ecke. Der Steinmetz schneidet 8 aus dem Vollblock, 8 ergeben ihn wieder."
        }
      },
      "craftedBy": [
        "simpleoctets:yellow_wool_octet_from_yellow_wool_stonecutting"
      ],
      "usedIn": [
        "simpleoctets:yellow_wool_from_octets"
      ]
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
    "features": 6,
    "recipes": 341,
    "lootTables": 0,
    "tags": 0,
    "advancements": 0,
    "enchantments": 0,
    "items": 1,
    "blocks": 172,
    "trades": 0,
    "config": 0,
    "quests": 0,
    "recipesOtherLines": 0,
    "undocumented": 0,
    "inWorld": 0,
    "incompleteProse": 0
  }
};
