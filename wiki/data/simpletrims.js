window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simpletrims"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simpletrims",
    "name": "Simple Trims",
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
      "id": "trims_module",
      "sources": [
        "modules/simpletrims/shared/java/com/simpletrims/SimpleTrims.java",
        "modules/simpletrims/shared/java/com/simpletrims/config/TrimsConfig.java"
      ],
      "en": {
        "title": "Simple Trims",
        "summary": "Sub-mod with the Glowing, Emitting and Pulsating armor trim templates, placeable trim templates and the trim upgrades. Playable on its own (axe); bundled by SimpleBuilding (sledgehammer).",
        "details": [
          "Config simpletrims-server.json: enabled (default on). With SimpleBuilding also its switch Enable Simple Trims. Off: no upgrades, no placing, no radiance light, empty tab entries; registrations stay so worlds keep loading."
        ]
      },
      "de": {
        "title": "Simple Trims",
        "summary": "Sub-Mod mit den Vorlagen Leuchtender, Strahlender und Pulsierender Rüstungsbesatz, ablegbaren Besatzvorlagen und den Besatz-Aufwertungen. Allein spielbar (Axt); von SimpleBuilding gebündelt (Vorschlaghammer).",
        "details": [
          "Config simpletrims-server.json: enabled (Standard an). Mit SimpleBuilding zusätzlich dessen Schalter „Simple Trims aktivieren“. Aus: keine Aufwertungen, kein Ablegen, kein Strahlkraft-Licht, keine Tab-Einträge; Registrierungen bleiben, Welten laden weiter."
        ]
      }
    },
    {
      "id": "trim_upgrades",
      "sources": [
        "modules/simpletrims/shared/java/com/simpletrims/TrimUpgrades.java",
        "modules/simpletrims/shared/java/com/simpletrims/recipe/TrimUpgradeRecipe.java",
        "modules/simpletrims/shared/java/com/simpletrims/TrimGlow.java",
        "modules/simpletrims/shared/resources/data/simpletrims/recipe/glowing_armor_upgrade.json"
      ],
      "en": {
        "title": "Trim upgrades at the smithing table",
        "summary": "Template + armor + material: Glowing (Glow Ink Sac, once), Radiance (Glowstone Dust, up to level 5), Pulsating (Echo Shard, once), Inverting (Ink Sac, three times: flat, rimmed, soft rim). The template is used up like any smithing template.",
        "details": [
          "Glowing draws the trim at full light. Pulsating alone fades the trim colors to gray and back; with Glowing its brightness swells between 1 and 15.",
          "Radiance gives 3 light per level to an invisible light block over the wearer's head (sum of all worn pieces, at most 15).",
          "Torches (also soul, copper, redstone), lanterns and lava buckets in either hand light the same block like their own light (redstone torch weak, 7); the brighter of held and radiance light counts, they do not add.",
          "A real smithing recipe (simpletrims:trim_upgrade): the Vanilla smithing table and SimpleBuilding's Auto Smither both use it."
        ]
      },
      "de": {
        "title": "Besatz-Aufwertungen am Schmiedetisch",
        "summary": "Vorlage + Rüstung + Material: Leuchtend (Leuchttintenbeutel, einmal), Strahlkraft (Glowstonestaub, bis Stufe 5), Pulsierend (Echoscherbe, einmal). Die Vorlage wird verbraucht wie jede Schmiedevorlage.",
        "details": [
          "Leuchtend zeichnet den Besatz voll hell. Pulsierend allein lässt die Besatzfarben zu Grau verblassen und zurückkehren; mit Leuchtend schwillt die Helligkeit zwischen 1 und 15 an und ab.",
          "Strahlkraft gibt 3 Licht je Stufe an einen unsichtbaren Lichtblock über dem Kopf (Summe aller getragenen Teile, höchstens 15).",
          "Fackeln (auch Seelen-, Kupfer-, Redstone-), Laternen und Lavaeimer in einer der beiden Hände leuchten an derselben Stelle wie ihr eigener Block (Redstone-Fackel schwach, 7); es zählt das hellere von Handlicht und Strahlkraft, sie addieren sich nicht.",
          "Echtes Schmiederezept (simpletrims:trim_upgrade): Vanilla-Schmiedetisch und der Auto-Schmied von SimpleBuilding nutzen es gleich."
        ]
      }
    },
    {
      "id": "placed_trim_templates",
      "sources": [
        "modules/simpletrims/shared/java/com/simpletrims/placed/PlacedTemplates.java",
        "modules/simpletrims/shared/java/com/simpletrims/placed/PlacedTrimTemplateBlock.java",
        "modules/simpletrims/shared/java/com/simpletrims/mixin/ItemUseOnMixin.java"
      ],
      "en": {
        "title": "Placed trim templates",
        "summary": "Sneak + use with any armor trim template lays it flat on the floor, against a wall or under a ceiling. Breaking it gives back exactly that template.",
        "details": [
          "Other smithing templates (Netherite Upgrade, SimpleBuilding's upgrade templates) are placed by SimpleBuilding, if installed."
        ]
      },
      "de": {
        "title": "Abgelegte Besatzvorlagen",
        "summary": "Schleichen + Benutzen mit einer Rüstungsbesatzvorlage legt sie flach auf den Boden, an eine Wand oder unter eine Decke. Abbauen gibt genau diese Vorlage zurück.",
        "details": [
          "Andere Schmiedevorlagen (Netherit-Aufwertung, Aufwertungsvorlagen von SimpleBuilding) legt SimpleBuilding ab, falls installiert."
        ]
      }
    },
    {
      "id": "template_tool_rule",
      "sources": [
        "modules/simpletrims/shared/java/com/simpletrims/TemplateTools.java",
        "modules/simpletrims/shared/java/com/simpletrims/placed/PlacedTemplates.java",
        "modules/simpletrims/shared/java/com/simpletrims/recipe/ToolShapelessRecipe.java",
        "modules/simpletrims/shared/resources/data/simpletrims/tags/item/template_hammers.json"
      ],
      "en": {
        "title": "Forging templates: axe or sledgehammer",
        "summary": "A placed Vanilla armor trim template becomes the Glowing one (Glow Ink Sac in the off hand) or the Emitting one (Glowstone Dust): 5 right clicks with the tool, then 2 of the off-hand material, 4 diamonds and 2 glowstone (Glowing) or 2 blaze powder (Emitting) from the inventory.",
        "details": [
          "The tool is any axe on its own; with SimpleBuilding (or another mod in simpletrims:template_hammers that switches the axe ways off) only the sledgehammer - the same rule as SimpleLib's crucibles. Each strike costs the tool 1 durability.",
          "Pulsating template (shapeless): the tool + 2 echo shards + 2 sculk + 4 diamonds; the tool stays in the grid with 1 durability less.",
          "Copies: template + 7 diamonds + glowstone block / magma block / sculk = 2 templates."
        ]
      },
      "de": {
        "title": "Vorlagen schmieden: Axt oder Vorschlaghammer",
        "summary": "Eine abgelegte Vanilla-Besatzvorlage wird zur Leuchtenden (Leuchttintenbeutel in der Nebenhand) oder Strahlenden (Glowstonestaub): 5 Rechtsklicks mit dem Werkzeug, dann 2 vom Nebenhand-Material, 4 Diamanten und 2 Glowstone (Leuchtend) bzw. 2 Lohenstaub (Strahlend) aus dem Inventar.",
        "details": [
          "Werkzeug ist allein jede Axt; mit SimpleBuilding (oder einer anderen Mod in simpletrims:template_hammers, die die Axt-Wege abschaltet) nur der Vorschlaghammer - dieselbe Regel wie bei den Tiegeln von SimpleLib. Jeder Schlag kostet das Werkzeug 1 Haltbarkeit.",
          "Pulsierende Vorlage (formlos): Werkzeug + 2 Echoscherben + 2 Sculk + 4 Diamanten; das Werkzeug bleibt mit 1 Haltbarkeit weniger im Raster.",
          "Kopien: Vorlage + 7 Diamanten + Glowsteinblock / Magmablock / Sculk = 2 Vorlagen."
        ]
      }
    },
    {
      "id": "trims_migration",
      "sources": [
        "modules/simpletrims/shared/java/com/simpletrims/SimpleTrims.java"
      ],
      "en": {
        "title": "Moved from SimpleBuilding",
        "summary": "The templates, their upgrade data, recipes and advancements used to be simplebuilding:*. Old worlds load them as simpletrims:* (SimpleLib id moves); radiance keeps its stored value.",
        "details": [
          "Advancements Glow Up, Bright Idea and Full Radiance now hang below the Vanilla trim advancement; earned progress carries over."
        ]
      },
      "de": {
        "title": "Aus SimpleBuilding umgezogen",
        "summary": "Die Vorlagen, ihre Aufwertungsdaten, Rezepte und Fortschritte hießen früher simplebuilding:*. Alte Welten laden sie als simpletrims:* (SimpleLib-ID-Umzug); die Strahlkraft behält ihren Wert.",
        "details": [
          "Die Fortschritte Glanzleistung, Geistesblitz und Volle Strahlkraft hängen jetzt unter dem Vanilla-Besatzfortschritt; erreichter Fortschritt bleibt."
        ]
      }
    },
    {
      "id": "creative_tab_switch",
      "sources": [
        "modules/simpletrims/shared/java/com/simpletrims/TrimsRegistry.java"
      ],
      "en": {
        "title": "Creative tabs",
        "summary": "The module's items sit in its own creative tab and are also sorted into the fitting Vanilla tabs and the search tab.",
        "details": [
          "The shared switch simpletrims.addItemsToVanillaTabs in config/simple-creative-tabs.properties (default true, read when a world is joined) keeps the Vanilla tabs unchanged; the module's own tab stays. Guide books always stay in Tools & Utilities."
        ]
      },
      "de": {
        "title": "Kreativ-Tabs",
        "summary": "Die Items des Moduls stehen im eigenen Kreativ-Tab und zusätzlich in den passenden Vanilla-Tabs und im Suchtab.",
        "details": [
          "Der gemeinsame Schalter simpletrims.addItemsToVanillaTabs in config/simple-creative-tabs.properties (Standard true, wird beim Betreten einer Welt gelesen) lässt die Vanilla-Tabs unverändert; der eigene Tab des Moduls bleibt. Handbücher bleiben immer in Werkzeuge & Hilfsmittel."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simpletrims:emitting_armor_upgrade",
      "type": "simpletrims:trim_upgrade",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simpletrims/shared/resources/data/simpletrims/recipe/emitting_armor_upgrade.json",
      "ingredients": [
        "#minecraft:trimmable_armor",
        "minecraft:glowstone_dust",
        "simpletrims:emitting_trim_template"
      ],
      "slots": {
        "template": [
          "simpletrims:emitting_trim_template"
        ],
        "base": [
          "#minecraft:trimmable_armor"
        ],
        "addition": [
          "minecraft:glowstone_dust"
        ]
      }
    },
    {
      "id": "simpletrims:emitting_trim_template_duplication",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpletrims:emitting_trim_template",
        "count": 2
      },
      "source": "modules/simpletrims/shared/resources/data/simpletrims/recipe/emitting_trim_template_duplication.json",
      "ingredients": [
        "minecraft:diamond",
        "minecraft:magma_block",
        "simpletrims:emitting_trim_template"
      ],
      "pattern": [
        "DTD",
        "DBD",
        "DDD"
      ],
      "key": {
        "B": [
          "minecraft:magma_block"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "T": [
          "simpletrims:emitting_trim_template"
        ]
      }
    },
    {
      "id": "simpletrims:glowing_armor_upgrade",
      "type": "simpletrims:trim_upgrade",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simpletrims/shared/resources/data/simpletrims/recipe/glowing_armor_upgrade.json",
      "ingredients": [
        "#minecraft:trimmable_armor",
        "minecraft:glow_ink_sac",
        "simpletrims:glowing_trim_template"
      ],
      "slots": {
        "template": [
          "simpletrims:glowing_trim_template"
        ],
        "base": [
          "#minecraft:trimmable_armor"
        ],
        "addition": [
          "minecraft:glow_ink_sac"
        ]
      }
    },
    {
      "id": "simpletrims:glowing_trim_template_duplication",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpletrims:glowing_trim_template",
        "count": 2
      },
      "source": "modules/simpletrims/shared/resources/data/simpletrims/recipe/glowing_trim_template_duplication.json",
      "ingredients": [
        "minecraft:diamond",
        "minecraft:glowstone",
        "simpletrims:glowing_trim_template"
      ],
      "pattern": [
        "DTD",
        "DBD",
        "DDD"
      ],
      "key": {
        "B": [
          "minecraft:glowstone"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "T": [
          "simpletrims:glowing_trim_template"
        ]
      }
    },
    {
      "id": "simpletrims:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpletrims:guide_book",
        "count": 1
      },
      "source": "modules/simpletrims/shared/resources/data/simpletrims/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:glow_ink_sac"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:glow_ink_sac"
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
            "id": "minecraft:glow_ink_sac",
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
      "id": "simpletrims:inverting_armor_upgrade",
      "type": "simpletrims:trim_upgrade",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simpletrims/shared/resources/data/simpletrims/recipe/inverting_armor_upgrade.json",
      "ingredients": [
        "#minecraft:trimmable_armor",
        "minecraft:ink_sac",
        "simpletrims:inverting_trim_template"
      ],
      "slots": {
        "template": [
          "simpletrims:inverting_trim_template"
        ],
        "base": [
          "#minecraft:trimmable_armor"
        ],
        "addition": [
          "minecraft:ink_sac"
        ]
      }
    },
    {
      "id": "simpletrims:inverting_trim_template_duplication",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpletrims:inverting_trim_template",
        "count": 2
      },
      "source": "modules/simpletrims/shared/resources/data/simpletrims/recipe/inverting_trim_template_duplication.json",
      "ingredients": [
        "minecraft:diamond",
        "minecraft:obsidian",
        "simpletrims:inverting_trim_template"
      ],
      "pattern": [
        "DTD",
        "DBD",
        "DDD"
      ],
      "key": {
        "B": [
          "minecraft:obsidian"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "T": [
          "simpletrims:inverting_trim_template"
        ]
      }
    },
    {
      "id": "simpletrims:pulsating_armor_upgrade",
      "type": "simpletrims:trim_upgrade",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simpletrims/shared/resources/data/simpletrims/recipe/pulsating_armor_upgrade.json",
      "ingredients": [
        "#minecraft:trimmable_armor",
        "minecraft:echo_shard",
        "simpletrims:pulsating_trim_template"
      ],
      "slots": {
        "template": [
          "simpletrims:pulsating_trim_template"
        ],
        "base": [
          "#minecraft:trimmable_armor"
        ],
        "addition": [
          "minecraft:echo_shard"
        ]
      }
    },
    {
      "id": "simpletrims:pulsating_trim_template",
      "type": "simpletrims:tool_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpletrims:pulsating_trim_template",
        "count": 1
      },
      "source": "modules/simpletrims/shared/resources/data/simpletrims/recipe/pulsating_trim_template.json",
      "ingredients": [
        "#simpletrims:template_tools",
        "minecraft:diamond",
        "minecraft:echo_shard",
        "minecraft:sculk"
      ],
      "ingredientGroups": [
        [
          "#simpletrims:template_tools"
        ],
        [
          "minecraft:echo_shard"
        ],
        [
          "minecraft:echo_shard"
        ],
        [
          "minecraft:sculk"
        ],
        [
          "minecraft:sculk"
        ],
        [
          "minecraft:diamond"
        ],
        [
          "minecraft:diamond"
        ],
        [
          "minecraft:diamond"
        ],
        [
          "minecraft:diamond"
        ]
      ]
    },
    {
      "id": "simpletrims:pulsating_trim_template_duplication",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpletrims:pulsating_trim_template",
        "count": 2
      },
      "source": "modules/simpletrims/shared/resources/data/simpletrims/recipe/pulsating_trim_template_duplication.json",
      "ingredients": [
        "minecraft:diamond",
        "minecraft:sculk",
        "simpletrims:pulsating_trim_template"
      ],
      "pattern": [
        "DTD",
        "DBD",
        "DDD"
      ],
      "key": {
        "B": [
          "minecraft:sculk"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "T": [
          "simpletrims:pulsating_trim_template"
        ]
      }
    }
  ],
  "lootTables": [],
  "tags": [
    {
      "id": "simpletrims:item/template_hammers",
      "replace": false,
      "values": [
        {
          "id": "#simplelib:hammers",
          "required": false
        }
      ],
      "source": "modules/simpletrims/shared/resources/data/simpletrims/tags/item/template_hammers.json"
    },
    {
      "id": "simpletrims:item/template_tools",
      "replace": false,
      "values": [
        {
          "id": "#minecraft:axes",
          "required": true
        },
        {
          "id": "#simpletrims:template_hammers",
          "required": true
        }
      ],
      "source": "modules/simpletrims/shared/resources/data/simpletrims/tags/item/template_tools.json"
    }
  ],
  "advancements": [
    {
      "id": "simpletrims:bright_idea",
      "parent": "simpletrims:glow_up",
      "icon": "simpletrims:emitting_trim_template",
      "frame": "task",
      "hidden": false,
      "title": {
        "en_us": "Bright Idea",
        "de_de": "Geistesblitz"
      },
      "description": {
        "en_us": "Glowstone Dust instead of Glow Ink turns a trim template into an Emitting one",
        "de_de": "Glowstonestaub statt Leuchttinte macht aus einer Besatz-Vorlage eine Strahlende"
      },
      "criteria": [
        {
          "name": "emitting_trim_template",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simpletrims:emitting_trim_template"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simpletrims/shared/resources/data/simpletrims/advancement/bright_idea.json"
    },
    {
      "id": "simpletrims:full_radiance",
      "parent": "simpletrims:bright_idea",
      "icon": "minecraft:glowstone",
      "frame": "goal",
      "hidden": false,
      "title": {
        "en_us": "Full Radiance",
        "de_de": "Volle Strahlkraft"
      },
      "description": {
        "en_us": "Add Radiance at the smithing table - Emitting template, armor, Glowstone Dust - until one armor piece shows Radiance 5",
        "de_de": "Füge am Schmiedetisch Strahlkraft hinzu - Strahlende Vorlage, Rüstung, Glowstonestaub -, bis ein Rüstungsteil Strahlkraft 5 zeigt"
      },
      "criteria": [
        {
          "name": "radiance_5",
          "trigger": "minecraft:inventory_changed"
        }
      ],
      "needs": "any",
      "source": "modules/simpletrims/shared/resources/data/simpletrims/advancement/full_radiance.json"
    },
    {
      "id": "simpletrims:glow_up",
      "parent": "minecraft:adventure/trim_with_any_armor_pattern",
      "icon": "simpletrims:glowing_trim_template",
      "frame": "task",
      "hidden": false,
      "title": {
        "en_us": "Glow Up",
        "de_de": "Glanzleistung"
      },
      "description": {
        "en_us": "Lay down an armor trim template, hold Glow Ink or Glowstone Dust in the off hand and hit it five times with an axe (with SimpleBuilding: a sledgehammer)",
        "de_de": "Lege eine Rüstungsbesatzvorlage ab, halte Leuchttinte oder Glowstonestaub in der Nebenhand und schlage sie fünfmal mit einer Axt (mit Simple Hammers: dem Vorschlaghammer)"
      },
      "criteria": [
        {
          "name": "glowing_trim_template",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simpletrims:glowing_trim_template"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simpletrims/shared/resources/data/simpletrims/advancement/glow_up.json"
    }
  ],
  "enchantments": [],
  "items": [
    {
      "id": "simpletrims:emitting_trim_template",
      "name": {
        "en_us": "Emitting Armor Trim",
        "de_de": "Strahlender Rüstungsbesatz"
      },
      "note": {
        "sources": [
          "modules/simpletrims/shared/java/com/simpletrims/TrimsRegistry.java",
          "modules/simpletrims/shared/java/com/simpletrims/placed/PlacedTemplates.java"
        ],
        "en": {
          "summary": "Smithing: armor + Glowstone Dust = +1 Radiance (up to 5). Made by forging a placed armor trim template with Glowstone Dust."
        },
        "de": {
          "summary": "Schmieden: Rüstung + Glowstonestaub = +1 Strahlkraft (bis 5). Entsteht beim Schmieden einer abgelegten Besatzvorlage mit Glowstonestaub."
        }
      },
      "texture": "assets/textures/simpletrims/item/emitting_trim_template.png",
      "questionary": {
        "en": {
          "lore": [
            "A trim that does not just shine, it lights the way for others."
          ],
          "use": [
            "Lay down an armor trim template, hold 2 Glowstone Dust in the off hand and hit it 5 times with an axe (with SimpleBuilding: a sledgehammer); it also takes 4 diamonds and 2 blaze powder. One template, 7 diamonds and a magma block make 2."
          ],
          "secret": [
            "At the smithing table, with Glowstone Dust, armor becomes a wearable light source: an invisible light block follows your head. More dust raises the radiance, up to level 5."
          ]
        },
        "de": {
          "lore": [
            "Ein Besatz, der nicht nur scheint, sondern anderen den Weg leuchtet."
          ],
          "use": [
            "Lege eine Rüstungsbesatzvorlage ab, halte 2 Glowstonestaub in der Nebenhand und schlage sie 5-mal mit einer Axt (mit Simple Hammers: dem Vorschlaghammer); dazu gehen 4 Diamanten und 2 Lohenstaub weg. Eine Vorlage, 7 Diamanten und ein Magmablock ergeben 2."
          ],
          "secret": [
            "Am Schmiedetisch wird Rüstung mit Glowstonestaub zur tragbaren Lichtquelle: Ein unsichtbarer Lichtblock folgt deinem Kopf. Mehr Staub erhöht die Strahlkraft, bis Stufe 5."
          ]
        }
      },
      "craftedBy": [
        "simpletrims:emitting_trim_template_duplication"
      ],
      "usedIn": [
        "simpletrims:emitting_armor_upgrade",
        "simpletrims:emitting_trim_template_duplication"
      ]
    },
    {
      "id": "simpletrims:glowing_trim_template",
      "name": {
        "en_us": "Glowing Armor Trim",
        "de_de": "Leuchtender Rüstungsbesatz"
      },
      "note": {
        "sources": [
          "modules/simpletrims/shared/java/com/simpletrims/TrimsRegistry.java",
          "modules/simpletrims/shared/java/com/simpletrims/placed/PlacedTemplates.java"
        ],
        "en": {
          "summary": "Smithing: armor + Glow Ink Sac = Glowing trim. Made by forging a placed armor trim template with Glow Ink Sac."
        },
        "de": {
          "summary": "Schmieden: Rüstung + Leuchttintenbeutel = leuchtender Besatz. Entsteht beim Schmieden einer abgelegten Besatzvorlage mit Leuchttintenbeutel."
        }
      },
      "texture": "assets/textures/simpletrims/item/glowing_trim_template.png",
      "questionary": {
        "en": {
          "lore": [
            "A trim that holds the light a moment longer than it should."
          ],
          "use": [
            "Lay down an armor trim template, hold 2 Glow Ink Sacs in the off hand and hit it 5 times with an axe (with SimpleBuilding: a sledgehammer); it also takes 4 diamonds and 2 glowstone. One template, 7 diamonds and a glowstone block make 2."
          ],
          "secret": [
            "At the smithing table, template, armor and a Glow Ink Sac make the trim glow steadily at full brightness. It only glows visually and gives off no real light."
          ]
        },
        "de": {
          "lore": [
            "Ein Besatz, der das Licht einen Moment länger hält, als er sollte."
          ],
          "use": [
            "Lege eine Rüstungsbesatzvorlage ab, halte 2 Leuchttintenbeutel in der Nebenhand und schlage sie 5-mal mit einer Axt (mit Simple Hammers: dem Vorschlaghammer); dazu gehen 4 Diamanten und 2 Glowstone weg. Eine Vorlage, 7 Diamanten und ein Glowstone-Block ergeben 2."
          ],
          "secret": [
            "Am Schmiedetisch lassen Vorlage, Rüstung und ein Leuchttintenbeutel den Besatz gleichmäßig mit voller Helligkeit leuchten. Er leuchtet nur optisch und gibt kein echtes Licht ab."
          ]
        }
      },
      "craftedBy": [
        "simpletrims:glowing_trim_template_duplication"
      ],
      "usedIn": [
        "simpletrims:glowing_armor_upgrade",
        "simpletrims:glowing_trim_template_duplication"
      ]
    },
    {
      "id": "simpletrims:guide_book",
      "name": {
        "en_us": "Simple Trims Guide",
        "de_de": "Simple-Trims-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simpletrims/shared/java/com/simpletrims/guide/TrimsGuide.java",
          "modules/simpletrims/shared/resources/data/simpletrims/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 10 pages taken from this wiki, shown in your language. Shapeless recipe: book + glow ink sac. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Trims\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 10 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Leuchttintenbeutel. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Trims\" eins."
        }
      },
      "texture": "assets/textures/simpletrims/item/guide_book.png",
      "questionary": {
        "en": {
          "lore": [
            "A book that keeps the shine of every trim on record."
          ],
          "use": [
            "The Simple Trims Guide explains the three trim templates, placing templates and forging them. Craft it shapeless from a book and a Glow Ink Sac."
          ],
          "secret": [
            "The pages follow your language and update with the mod, even in old copies of the book."
          ]
        },
        "de": {
          "lore": [
            "Ein Buch, das den Glanz jedes Besatzes festhält."
          ],
          "use": [
            "Das Simple-Trims-Handbuch erklärt die drei Besatzvorlagen, das Ablegen und das Schmieden. Herstellung formlos aus einem Buch und einem Leuchttintenbeutel."
          ],
          "secret": [
            "Die Seiten folgen deiner Sprache und wachsen mit der Mod mit, auch in alten Exemplaren."
          ]
        }
      },
      "craftedBy": [
        "simpletrims:guide_book"
      ],
      "usedIn": []
    },
    {
      "id": "simpletrims:inverting_trim_template",
      "name": {
        "en_us": "Inverting Armor Trim",
        "de_de": "Invertierender Rüstungsbesatz"
      },
      "note": {
        "sources": [
          "modules/simpletrims/shared/java/com/simpletrims/TrimsRegistry.java",
          "modules/simpletrims/shared/java/com/simpletrims/TrimInversion.java",
          "modules/simpletrims/shared/java/com/simpletrims/placed/PlacedTemplates.java"
        ],
        "en": {
          "summary": "Smithing: trimmed armor + Ink Sac = Inverted trim (pattern cut out, the rest of the armor in the trim color); repeat with another Ink Sac for stage 2 (1 px rim) and stage 3 (wider, soft rim). Made by forging a placed armor trim template with an Ink Sac (4 diamonds, 2 obsidian)."
        },
        "de": {
          "summary": "Schmieden: Rüstung mit Besatz + Tintenbeutel = invertierter Besatz (Muster ausgespart, Rest der Rüstung in der Besatzfarbe); erneut mit Tintenbeutel: Stufe 2 (1-px-Rand), Stufe 3 (breiter, weicher Rand). Entsteht beim Schmieden einer abgelegten Besatzvorlage mit Tintenbeutel (4 Diamanten, 2 Obsidian)."
        }
      },
      "texture": "assets/textures/simpletrims/item/inverting_trim_template.png",
      "questionary": {
        "en": {
          "lore": [
            "A trim turned inside out."
          ],
          "use": [
            "Place a trim template and strike it with an axe (with Simple Hammers: a sledgehammer) while holding an Ink Sac in the off hand; costs 4 diamonds and 2 obsidian. One template, 7 diamonds and an obsidian block make 2."
          ],
          "secret": [
            "At the smithing table, template, trimmed armor and an Ink Sac cut the trim pattern out and paint the rest of the armor in the trim color."
          ]
        },
        "de": {
          "lore": [
            "Ein Besatz von innen nach außen."
          ],
          "use": [
            "Besatzvorlage ablegen und mit einer Axt (mit Simple Hammers: Vorschlaghammer) schlagen, einen Tintenbeutel in der Nebenhand; kostet 4 Diamanten und 2 Obsidian. Eine Vorlage, 7 Diamanten und ein Obsidianblock ergeben 2."
          ],
          "secret": [
            "Am Schmiedetisch sparen Vorlage, Rüstung mit Besatz und ein Tintenbeutel das Besatzmuster aus und färben den Rest der Rüstung in der Besatzfarbe."
          ]
        }
      },
      "craftedBy": [
        "simpletrims:inverting_trim_template_duplication"
      ],
      "usedIn": [
        "simpletrims:inverting_armor_upgrade",
        "simpletrims:inverting_trim_template_duplication"
      ]
    },
    {
      "id": "simpletrims:pulsating_trim_template",
      "name": {
        "en_us": "Pulsating Armor Trim",
        "de_de": "Pulsierender Rüstungsbesatz"
      },
      "note": {
        "sources": [
          "modules/simpletrims/shared/java/com/simpletrims/TrimsRegistry.java",
          "modules/simpletrims/shared/resources/data/simpletrims/recipe/pulsating_trim_template.json"
        ],
        "en": {
          "summary": "Smithing: armor + Echo Shard = Pulsating trim. Crafted from the tool, 2 echo shards, 2 sculk and 4 diamonds."
        },
        "de": {
          "summary": "Schmieden: Rüstung + Echoscherbe = pulsierender Besatz. Hergestellt aus Werkzeug, 2 Echoscherben, 2 Sculk und 4 Diamanten."
        }
      },
      "texture": "assets/textures/simpletrims/item/pulsating_trim_template.png",
      "questionary": {
        "en": {
          "lore": [
            "A trim with a heartbeat of its own."
          ],
          "use": [
            "Craft 2 echo shards, 2 sculk, 4 diamonds and an axe (with SimpleBuilding: a sledgehammer); the tool stays in the grid and loses 1 durability. One template, 7 diamonds and a sculk block make 2."
          ],
          "secret": [
            "At the smithing table, template, armor and an Echo Shard make the trim pulse from full color to gray every 2.4 seconds. Together with Glowing, the brightness pulses too."
          ]
        },
        "de": {
          "lore": [
            "Ein Besatz mit eigenem Herzschlag."
          ],
          "use": [
            "Herstellung: 2 Echoscherben, 2 Sculk, 4 Diamanten und eine Axt (mit SimpleBuilding: ein Vorschlaghammer); das Werkzeug bleibt im Raster und verliert 1 Haltbarkeit. Eine Vorlage, 7 Diamanten und ein Sculkblock ergeben 2."
          ],
          "secret": [
            "Am Schmiedetisch lassen Vorlage, Rüstung und eine Echoscherbe den Besatz alle 2,4 Sekunden von voller Farbe zu Grau pulsieren. Zusammen mit Leuchtend pulsiert auch die Helligkeit."
          ]
        }
      },
      "craftedBy": [
        "simpletrims:pulsating_trim_template",
        "simpletrims:pulsating_trim_template_duplication"
      ],
      "usedIn": [
        "simpletrims:pulsating_armor_upgrade",
        "simpletrims:pulsating_trim_template_duplication"
      ]
    }
  ],
  "blocks": [
    {
      "id": "simpletrims:placed_template",
      "name": {
        "en_us": "Placed Trim Template",
        "de_de": "Abgelegte Besatzvorlage"
      },
      "note": {
        "sources": [
          "modules/simpletrims/shared/java/com/simpletrims/placed/PlacedTrimTemplateBlock.java",
          "modules/simpletrims/shared/java/com/simpletrims/placed/PlacedTemplates.java"
        ],
        "en": {
          "summary": "A laid-down armor trim template (sneak + use). Breaking it returns the template; tool + Glow Ink Sac or Glowstone Dust forge it."
        },
        "de": {
          "summary": "Eine abgelegte Rüstungsbesatzvorlage (Schleichen + Benutzen). Abbauen gibt die Vorlage zurück; Werkzeug + Leuchttintenbeutel oder Glowstonestaub schmieden sie."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "A template laid down like a note on the table."
          ],
          "use": [
            "Sneak and use an armor trim template on a floor, wall or ceiling. Breaking it gives back exactly that template, name included."
          ],
          "secret": [
            "With the template tool in the main hand and Glow Ink or Glowstone Dust in the off hand, right clicks forge it instead of breaking it."
          ]
        },
        "de": {
          "lore": [
            "Eine Vorlage, abgelegt wie ein Zettel auf dem Tisch."
          ],
          "use": [
            "Schleichen und eine Rüstungsbesatzvorlage auf Boden, Wand oder Decke benutzen. Abbauen gibt genau diese Vorlage zurück, samt Namen."
          ],
          "secret": [
            "Mit dem Vorlagen-Werkzeug in der Haupthand und Leuchttinte oder Glowstonestaub in der Nebenhand schmieden Rechtsklicks sie, statt sie abzubauen."
          ]
        }
      },
      "craftedBy": [],
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
    "features": 6,
    "recipes": 10,
    "lootTables": 0,
    "tags": 2,
    "advancements": 3,
    "enchantments": 0,
    "items": 5,
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
