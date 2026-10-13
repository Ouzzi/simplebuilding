window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplebeacons"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplebeacons",
    "name": "Simple Beacons",
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
      "id": "strong_upgrade_template",
      "sources": [
        "modules/simplebeacons/shared/java/com/simplebeacons/BeaconsRegistry.java",
        "modules/simplebeacons/shared/resources/data/simplebeacons/recipe/strong_upgrade_template.json",
        "modules/simplebeacons/shared/resources/data/simplebeacons/recipe/strong_upgrade_template_duplication.json",
        "modules/simplebeacons/shared/resources/data/simplebeacons/recipe/strong_upgrade_template_from_basic.json"
      ],
      "en": {
        "title": "Strong Upgrade",
        "summary": "A smithing template crafted from a nether star in the middle, four diamonds on the edges and four obsidian in the corners. It turns a beacon into a Strong Beacon and a conduit into a Strong Conduit.",
        "details": [
          "Duplicate it like Vanilla templates: 7 diamonds, 1 obsidian and the template give 2 templates.",
          "With SimpleBuilding a Basic Upgrade can replace the four diamonds of the first template (obsidian in the corners, Basic Upgrade on top, nether star in the middle).",
          "It is used up by both upgrade ways."
        ]
      },
      "de": {
        "title": "Starke Aufwertungsvorlage",
        "summary": "Eine Schmiedevorlage aus einem Netherstern in der Mitte, vier Diamanten an den Kanten und vier Obsidian in den Ecken. Sie macht aus einem Leuchtfeuer ein Starkes Leuchtfeuer und aus einem Aquisator einen Starken Aquisator.",
        "details": [
          "Vervielfältigen wie Vanilla-Vorlagen: 7 Diamanten, 1 Obsidian und die Vorlage ergeben 2 Vorlagen.",
          "Mit SimpleBuilding ersetzt eine Basis-Aufwertung die vier Diamanten der ersten Vorlage (Obsidian in den Ecken, Basis-Aufwertung oben, Netherstern in der Mitte).",
          "Beide Aufwertungswege verbrauchen sie."
        ]
      }
    },
    {
      "id": "strong_beacon",
      "sources": [
        "modules/simplebeacons/shared/java/com/simplebeacons/beacon/StrongBeaconLogic.java",
        "modules/simplebeacons/shared/java/com/simplebeacons/beacon/StrongBeaconBlockEntity.java",
        "modules/simplebeacons/shared/java/com/simplebeacons/beacon/StrongBeaconMenu.java",
        "modules/simplebeacons/shared/resources/data/simplebeacons/recipe/strong_beacon_smithing.json"
      ],
      "en": {
        "title": "Strong Beacon",
        "summary": "A beacon with up to six pyramid levels: 11x11 and 13x13 below the Vanilla four (454 blocks in all). Range is 10 + 10 per level (60 blocks at level 5, 70 at level 6), the effect lasts 9 + 2 per level seconds; it affects players only, like Vanilla.",
        "details": [
          "Smithing table: beacon + Strong Upgrade + nether star. Levels 1 to 4 work exactly like a Vanilla beacon.",
          "Level 5 adds Night Vision, Water Breathing and Slow Falling as primary powers; level 6 adds Fire Resistance, Dolphin's Grace and Absorption I.",
          "The secondary power stays Vanilla's: Regeneration or level II of a Vanilla primary power. The new powers have no level II, so there is no Absorption II.",
          "Absorption fills two extra hearts once; while you stay in range it is only renewed, not refilled.",
          "Pyramid blocks: #minecraft:beacon_base_blocks, plus the tag #simplebeacons:strong_base_blocks (empty) for modpacks.",
          "Payment as in Vanilla: one item of #minecraft:beacon_payment_items. The server checks every choice; a power the pyramid or the server does not allow is refused and the payment stays."
        ]
      },
      "de": {
        "title": "Starkes Leuchtfeuer",
        "summary": "Ein Leuchtfeuer mit bis zu sechs Pyramidenebenen: 11x11 und 13x13 unter den vier Vanilla-Ebenen (zusammen 454 Blöcke). Die Reichweite ist 10 + 10 je Ebene (60 Blöcke auf Ebene 5, 70 auf Ebene 6), die Wirkung hält 9 + 2 je Ebene Sekunden; sie trifft nur Spieler, wie Vanilla.",
        "details": [
          "Schmiedetisch: Leuchtfeuer + Starke Aufwertungsvorlage + Netherstern. Die Ebenen 1 bis 4 verhalten sich genau wie ein Vanilla-Leuchtfeuer.",
          "Ebene 5 bringt Nachtsicht, Wasseratmung und Langsamen Fall als Primärkraft; Ebene 6 Feuerresistenz, Delfingunst und Absorption I.",
          "Die Sekundärkraft bleibt Vanilla: Regeneration oder Stufe II einer Vanilla-Primärkraft. Die neuen Kräfte haben keine Stufe II, also kein Absorption II.",
          "Absorption füllt zwei Extraherzen einmal; solange man in Reichweite bleibt, wird sie nur verlängert, nicht nachgefüllt.",
          "Pyramidenblöcke: #minecraft:beacon_base_blocks, dazu das Tag #simplebeacons:strong_base_blocks (leer) für Modpacks.",
          "Bezahlung wie Vanilla: ein Item aus #minecraft:beacon_payment_items. Der Server prüft jede Wahl; eine Kraft, die Pyramide oder Server nicht erlauben, wird abgelehnt und die Bezahlung bleibt liegen."
        ]
      }
    },
    {
      "id": "strong_beacon_screen",
      "sources": [
        "modules/simplebeacons/shared/java/com/simplebeacons/client/StrongBeaconScreen.java"
      ],
      "en": {
        "title": "Two-column screen",
        "summary": "The Strong Beacon opens Vanilla's beacon screen with an extra panel on the right: the pyramid counter (for example Pyramid 6/6), a row for level 5 and one for level 6 with three powers each, and the current range and duration.",
        "details": [
          "Rows the pyramid does not reach yet are greyed out; their tooltip names the level and the blocks it needs (level 5: 285, level 6: 454).",
          "The tooltip of every new power says in one line what it does. The II button is greyed out while a new power is chosen."
        ]
      },
      "de": {
        "title": "Zweispaltiger Bildschirm",
        "summary": "Das Starke Leuchtfeuer öffnet den Vanilla-Leuchtfeuer-Bildschirm mit einem Zusatzfeld rechts: dem Pyramidenzähler (zum Beispiel Pyramide 6/6), einer Zeile für Ebene 5 und einer für Ebene 6 mit je drei Kräften sowie der aktuellen Reichweite und Dauer.",
        "details": [
          "Zeilen, die die Pyramide noch nicht erreicht, sind ausgegraut; ihr Tooltip nennt die Ebene und die nötigen Blöcke (Ebene 5: 285, Ebene 6: 454).",
          "Der Tooltip jeder neuen Kraft sagt in einer Zeile, was sie tut. Der Knopf II ist ausgegraut, solange eine neue Kraft gewählt ist."
        ]
      }
    },
    {
      "id": "strong_upgrade_in_world",
      "sources": [
        "modules/simplebeacons/shared/java/com/simplebeacons/upgrade/StrongUpgrades.java",
        "modules/simplebeacons/shared/java/com/simplebeacons/mixin/ServerPlayerGameModeMixin.java"
      ],
      "en": {
        "title": "Upgrading a placed beacon or conduit",
        "summary": "Hold an axe (with a hammer mod such as SimpleBuilding: its sledgehammer) in the main hand and the Strong Upgrade in the off hand, carry a nether star and use a placed beacon or conduit 8 times.",
        "details": [
          "Every strike cracks the block, rings a rising anvil sound and lets the Strong block grow over the old one from below.",
          "The last strike swaps the block in place: chosen powers, lock and name stay. The template and one nether star are used up, the tool wears by one per strike.",
          "Without a nether star the strike is refused. The beacon does not have to be active."
        ]
      },
      "de": {
        "title": "Gesetztes Leuchtfeuer oder Aquisator aufwerten",
        "summary": "Eine Axt (mit einer Hammer-Mod wie SimpleBuilding: deren Vorschlaghammer) in der Haupthand, die Starke Aufwertungsvorlage in der Nebenhand, einen Netherstern dabei und 8-mal ein gesetztes Leuchtfeuer oder einen Aquisator benutzen.",
        "details": [
          "Jeder Schlag lässt Risse erscheinen, spielt einen steigenden Amboss-Klang und lässt den starken Block von unten über den alten wachsen.",
          "Der letzte Schlag tauscht den Block an Ort und Stelle: gewählte Kräfte, Sperre und Name bleiben. Vorlage und ein Netherstern werden verbraucht, das Werkzeug nutzt sich je Schlag um eins ab.",
          "Ohne Netherstern wird der Schlag abgelehnt. Das Leuchtfeuer muss nicht aktiv sein."
        ]
      }
    },
    {
      "id": "strong_conduit",
      "sources": [
        "modules/simplebeacons/shared/java/com/simplebeacons/conduit/StrongConduitLogic.java",
        "modules/simplebeacons/shared/java/com/simplebeacons/conduit/StrongConduitBlockEntity.java",
        "modules/simplebeacons/shared/resources/data/simplebeacons/recipe/strong_conduit_smithing.json"
      ],
      "en": {
        "title": "Strong Conduit",
        "summary": "A conduit that also accepts a 7x7x7 frame: three crossing 7x7 rings of prismarine, prismarine bricks, dark prismarine or sea lanterns (66 blocks). Range is (frame blocks / 7) x 16, up to 144 blocks with the full large frame.",
        "details": [
          "Smithing table: conduit + Strong Upgrade + nether star. With the Vanilla 5x5x5 frame (42 blocks) it works like a Vanilla conduit (up to 96 blocks).",
          "It needs water around it and at least 16 frame blocks, like Vanilla.",
          "Players in the water or rain get Conduit Power like Vanilla; players in the water also get Dolphin's Grace. Beyond 96 blocks only players really under water count, not rain.",
          "The full 7x7x7 frame attacks hostile mobs in the water within 12 blocks (the full 5x5x5 frame: 8), with Vanilla's damage.",
          "It looks like a Vanilla conduit."
        ]
      },
      "de": {
        "title": "Starker Aquisator",
        "summary": "Ein Aquisator, der zusätzlich einen 7x7x7-Rahmen annimmt: drei sich kreuzende 7x7-Ringe aus Prismarin, Prismarinziegeln, Dunklem Prismarin oder Seelaternen (66 Blöcke). Die Reichweite ist (Rahmenblöcke / 7) x 16, bis 144 Blöcke mit dem vollen großen Rahmen.",
        "details": [
          "Schmiedetisch: Aquisator + Starke Aufwertungsvorlage + Netherstern. Mit dem Vanilla-Rahmen 5x5x5 (42 Blöcke) wirkt er wie ein Vanilla-Aquisator (bis 96 Blöcke).",
          "Er braucht Wasser um sich und mindestens 16 Rahmenblöcke, wie Vanilla.",
          "Spieler im Wasser oder Regen erhalten Meereskraft wie bei Vanilla; Spieler im Wasser zusätzlich Delfingunst. Jenseits von 96 Blöcken zählen nur Spieler wirklich unter Wasser, nicht im Regen.",
          "Der volle 7x7x7-Rahmen greift feindliche Mobs im Wasser im Umkreis von 12 Blöcken an (der volle 5x5x5-Rahmen: 8), mit Vanilla-Schaden.",
          "Er sieht aus wie ein Vanilla-Aquisator."
        ]
      }
    },
    {
      "id": "config_server",
      "sources": [
        "modules/simplebeacons/shared/java/com/simplebeacons/config/BeaconsConfig.java"
      ],
      "en": {
        "title": "Server settings",
        "summary": "config/simplebeacons-server.json, read at start; every number is clamped to a safe range.",
        "details": [
          "enabled (true): off makes the module inert (no upgrades, placed Strong blocks do nothing).",
          "strongBeaconMaxLevels (6, 4 to 6): highest pyramid level that counts.",
          "strongBeaconRangeBase (10, 0 to 50) and strongBeaconRangePerLevel (10, 1 to 20): range = base + per level x levels.",
          "strongBeaconEffects: one switch per new power (night_vision, water_breathing, slow_falling, fire_resistance, dolphins_grace, absorption), all true.",
          "strongConduitMaxRange (144, 96 to 144): cap of the Strong Conduit range.",
          "upgradeStrikes (8, 1 to 32): strikes of the in-world upgrade.",
          "With SimpleBuilding the switch Enable Simple Beacons in its config turns the bundled module on or off."
        ]
      },
      "de": {
        "title": "Server-Einstellungen",
        "summary": "config/simplebeacons-server.json, beim Start gelesen; jede Zahl wird auf einen sicheren Bereich begrenzt.",
        "details": [
          "enabled (true): aus macht das Modul wirkungslos (keine Aufwertung, gesetzte starke Blöcke tun nichts).",
          "strongBeaconMaxLevels (6, 4 bis 6): höchste zählende Pyramidenebene.",
          "strongBeaconRangeBase (10, 0 bis 50) und strongBeaconRangePerLevel (10, 1 bis 20): Reichweite = Grundwert + je Ebene x Ebenen.",
          "strongBeaconEffects: ein Schalter je neuer Kraft (night_vision, water_breathing, slow_falling, fire_resistance, dolphins_grace, absorption), alle true.",
          "strongConduitMaxRange (144, 96 bis 144): Deckel der Reichweite des Starken Aquisators.",
          "upgradeStrikes (8, 1 bis 32): Schläge der Aufwertung in der Welt.",
          "Mit SimpleBuilding schaltet Simple Beacons aktivieren in dessen Konfiguration das gebündelte Modul ein oder aus."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simplebeacons:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplebeacons:guide_book",
        "count": 1
      },
      "source": "modules/simplebeacons/shared/resources/data/simplebeacons/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:prismarine_shard"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:prismarine_shard"
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
            "id": "minecraft:leather",
            "count": 1
          },
          {
            "id": "minecraft:prismarine_shard",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simplebeacons:strong_beacon_smithing",
      "type": "minecraft:smithing_transform",
      "category": null,
      "group": null,
      "result": {
        "id": "simplebeacons:strong_beacon",
        "count": 1
      },
      "source": "modules/simplebeacons/shared/resources/data/simplebeacons/recipe/strong_beacon_smithing.json",
      "ingredients": [
        "minecraft:beacon",
        "minecraft:nether_star",
        "simplebeacons:strong_upgrade_template"
      ],
      "slots": {
        "template": [
          "simplebeacons:strong_upgrade_template"
        ],
        "base": [
          "minecraft:beacon"
        ],
        "addition": [
          "minecraft:nether_star"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:obsidian",
            "count": 7
          },
          {
            "id": "minecraft:sand",
            "count": 5
          },
          {
            "id": "minecraft:diamond",
            "count": 4
          },
          {
            "id": "minecraft:nether_star",
            "count": 3
          }
        ]
      }
    },
    {
      "id": "simplebeacons:strong_conduit_smithing",
      "type": "minecraft:smithing_transform",
      "category": null,
      "group": null,
      "result": {
        "id": "simplebeacons:strong_conduit",
        "count": 1
      },
      "source": "modules/simplebeacons/shared/resources/data/simplebeacons/recipe/strong_conduit_smithing.json",
      "ingredients": [
        "minecraft:conduit",
        "minecraft:nether_star",
        "simplebeacons:strong_upgrade_template"
      ],
      "slots": {
        "template": [
          "simplebeacons:strong_upgrade_template"
        ],
        "base": [
          "minecraft:conduit"
        ],
        "addition": [
          "minecraft:nether_star"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:nautilus_shell",
            "count": 8
          },
          {
            "id": "minecraft:diamond",
            "count": 4
          },
          {
            "id": "minecraft:obsidian",
            "count": 4
          },
          {
            "id": "minecraft:nether_star",
            "count": 2
          },
          {
            "id": "minecraft:heart_of_the_sea",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simplebeacons:strong_upgrade_template",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplebeacons:strong_upgrade_template",
        "count": 1
      },
      "source": "modules/simplebeacons/shared/resources/data/simplebeacons/recipe/strong_upgrade_template.json",
      "ingredients": [
        "minecraft:diamond",
        "minecraft:nether_star",
        "minecraft:obsidian"
      ],
      "pattern": [
        "ODO",
        "DND",
        "ODO"
      ],
      "key": {
        "D": [
          "minecraft:diamond"
        ],
        "N": [
          "minecraft:nether_star"
        ],
        "O": [
          "minecraft:obsidian"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 4
          },
          {
            "id": "minecraft:obsidian",
            "count": 4
          },
          {
            "id": "minecraft:nether_star",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simplebeacons:strong_upgrade_template_duplication",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplebeacons:strong_upgrade_template",
        "count": 2
      },
      "source": "modules/simplebeacons/shared/resources/data/simplebeacons/recipe/strong_upgrade_template_duplication.json",
      "ingredients": [
        "minecraft:diamond",
        "minecraft:obsidian",
        "simplebeacons:strong_upgrade_template"
      ],
      "pattern": [
        "DTD",
        "DOD",
        "DDD"
      ],
      "key": {
        "D": [
          "minecraft:diamond"
        ],
        "O": [
          "minecraft:obsidian"
        ],
        "T": [
          "simplebeacons:strong_upgrade_template"
        ]
      }
    },
    {
      "id": "simplebeacons:strong_upgrade_template_from_basic",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplebeacons:strong_upgrade_template",
        "count": 1
      },
      "source": "modules/simplebeacons/shared/resources/data/simplebeacons/recipe/strong_upgrade_template_from_basic.json",
      "ingredients": [
        "minecraft:nether_star",
        "minecraft:obsidian",
        "simplebuilding:basic_upgrade_template"
      ],
      "pattern": [
        "OBO",
        " N ",
        "O O"
      ],
      "key": {
        "B": [
          "simplebuilding:basic_upgrade_template"
        ],
        "N": [
          "minecraft:nether_star"
        ],
        "O": [
          "minecraft:obsidian"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:obsidian",
            "count": 4
          },
          {
            "id": "minecraft:nether_star",
            "count": 1
          },
          {
            "id": "simplebuilding:basic_upgrade_template",
            "count": 1
          }
        ]
      }
    }
  ],
  "lootTables": [
    {
      "id": "simplebeacons:blocks/strong_beacon",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplebeacons:strong_beacon"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplebeacons/shared/resources/data/simplebeacons/loot_table/blocks/strong_beacon.json"
    },
    {
      "id": "simplebeacons:blocks/strong_conduit",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplebeacons:strong_conduit"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplebeacons/shared/resources/data/simplebeacons/loot_table/blocks/strong_conduit.json"
    }
  ],
  "tags": [
    {
      "id": "simplebeacons:block/strong_base_blocks",
      "replace": false,
      "values": [],
      "source": "modules/simplebeacons/shared/resources/data/simplebeacons/tags/block/strong_base_blocks.json"
    }
  ],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "simplebeacons:guide_book",
      "name": {
        "en_us": "Simple Beacons Guide",
        "de_de": "Simple-Beacons-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simplebeacons/shared/java/com/simplebeacons/guide/BeaconsGuide.java",
          "modules/simplebeacons/shared/resources/data/simplebeacons/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 13 pages taken from this wiki, shown in your language. Shapeless recipe: book + prismarine shard. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Beacons\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 13 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Prismarinscherbe. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Beacons\" eins."
        }
      },
      "texture": "assets/textures/simplebeacons/item/guide_book.png",
      "craftedBy": [
        "simplebeacons:guide_book"
      ],
      "usedIn": []
    },
    {
      "id": "simplebeacons:strong_upgrade_template",
      "name": {
        "en_us": "Strong Upgrade",
        "de_de": "Starke Aufwertungsvorlage"
      },
      "note": {
        "en": {
          "title": "Strong Upgrade",
          "summary": "A smithing template crafted from a nether star in the middle, four diamonds on the edges and four obsidian in the corners. It turns a beacon into a Strong Beacon and a conduit into a Strong Conduit.",
          "details": [
            "Duplicate it like Vanilla templates: 7 diamonds, 1 obsidian and the template give 2 templates.",
            "With SimpleBuilding a Basic Upgrade can replace the four diamonds of the first template (obsidian in the corners, Basic Upgrade on top, nether star in the middle).",
            "It is used up by both upgrade ways."
          ]
        },
        "de": {
          "title": "Starke Aufwertungsvorlage",
          "summary": "Eine Schmiedevorlage aus einem Netherstern in der Mitte, vier Diamanten an den Kanten und vier Obsidian in den Ecken. Sie macht aus einem Leuchtfeuer ein Starkes Leuchtfeuer und aus einem Aquisator einen Starken Aquisator.",
          "details": [
            "Vervielfältigen wie Vanilla-Vorlagen: 7 Diamanten, 1 Obsidian und die Vorlage ergeben 2 Vorlagen.",
            "Mit SimpleBuilding ersetzt eine Basis-Aufwertung die vier Diamanten der ersten Vorlage (Obsidian in den Ecken, Basis-Aufwertung oben, Netherstern in der Mitte).",
            "Beide Aufwertungswege verbrauchen sie."
          ]
        }
      },
      "texture": "assets/textures/simplebeacons/item/strong_upgrade_template.png",
      "questionary": {
        "en": {
          "lore": [
            "A slab of obsidian around a captured star; it hums when a beacon is near."
          ],
          "use": [
            "Hold it in the off hand and hit a placed beacon or conduit 8 times with an axe (with a hammer mod: the sledgehammer) while you carry a nether star; or smith it at the smithing table.",
            "Crafted from a nether star, 4 diamonds and 4 obsidian; 7 diamonds, an obsidian and one template make 2."
          ],
          "secret": [
            "The upgraded block keeps its chosen powers, its lock and its name."
          ]
        },
        "de": {
          "lore": [
            "Eine Obsidianplatte um einen gefangenen Stern; sie summt, wenn ein Leuchtfeuer nah ist."
          ],
          "use": [
            "In der Nebenhand halten und mit einer Axt (mit einer Hammer-Mod: dem Vorschlaghammer) 8-mal auf ein gesetztes Leuchtfeuer oder einen Aquisator schlagen, mit einem Netherstern im Gepäck; oder am Schmiedetisch schmieden.",
            "Gefertigt aus einem Netherstern, 4 Diamanten und 4 Obsidian; 7 Diamanten, ein Obsidian und eine Vorlage ergeben 2."
          ],
          "secret": [
            "Der aufgewertete Block behält seine gewählten Kräfte, seine Sperre und seinen Namen."
          ]
        }
      },
      "craftedBy": [
        "simplebeacons:strong_upgrade_template",
        "simplebeacons:strong_upgrade_template_duplication",
        "simplebeacons:strong_upgrade_template_from_basic"
      ],
      "usedIn": [
        "simplebeacons:strong_beacon_smithing",
        "simplebeacons:strong_conduit_smithing",
        "simplebeacons:strong_upgrade_template_duplication"
      ]
    }
  ],
  "blocks": [
    {
      "id": "simplebeacons:strong_beacon",
      "name": {
        "en_us": "Strong Beacon",
        "de_de": "Starkes Leuchtfeuer"
      },
      "note": {
        "en": {
          "title": "Strong Beacon",
          "summary": "A beacon with up to six pyramid levels: 11x11 and 13x13 below the Vanilla four (454 blocks in all). Range is 10 + 10 per level (60 blocks at level 5, 70 at level 6), the effect lasts 9 + 2 per level seconds; it affects players only, like Vanilla.",
          "details": [
            "Smithing table: beacon + Strong Upgrade + nether star. Levels 1 to 4 work exactly like a Vanilla beacon.",
            "Level 5 adds Night Vision, Water Breathing and Slow Falling as primary powers; level 6 adds Fire Resistance, Dolphin's Grace and Absorption I.",
            "The secondary power stays Vanilla's: Regeneration or level II of a Vanilla primary power. The new powers have no level II, so there is no Absorption II.",
            "Absorption fills two extra hearts once; while you stay in range it is only renewed, not refilled.",
            "Pyramid blocks: #minecraft:beacon_base_blocks, plus the tag #simplebeacons:strong_base_blocks (empty) for modpacks.",
            "Payment as in Vanilla: one item of #minecraft:beacon_payment_items. The server checks every choice; a power the pyramid or the server does not allow is refused and the payment stays."
          ]
        },
        "de": {
          "title": "Starkes Leuchtfeuer",
          "summary": "Ein Leuchtfeuer mit bis zu sechs Pyramidenebenen: 11x11 und 13x13 unter den vier Vanilla-Ebenen (zusammen 454 Blöcke). Die Reichweite ist 10 + 10 je Ebene (60 Blöcke auf Ebene 5, 70 auf Ebene 6), die Wirkung hält 9 + 2 je Ebene Sekunden; sie trifft nur Spieler, wie Vanilla.",
          "details": [
            "Schmiedetisch: Leuchtfeuer + Starke Aufwertungsvorlage + Netherstern. Die Ebenen 1 bis 4 verhalten sich genau wie ein Vanilla-Leuchtfeuer.",
            "Ebene 5 bringt Nachtsicht, Wasseratmung und Langsamen Fall als Primärkraft; Ebene 6 Feuerresistenz, Delfingunst und Absorption I.",
            "Die Sekundärkraft bleibt Vanilla: Regeneration oder Stufe II einer Vanilla-Primärkraft. Die neuen Kräfte haben keine Stufe II, also kein Absorption II.",
            "Absorption füllt zwei Extraherzen einmal; solange man in Reichweite bleibt, wird sie nur verlängert, nicht nachgefüllt.",
            "Pyramidenblöcke: #minecraft:beacon_base_blocks, dazu das Tag #simplebeacons:strong_base_blocks (leer) für Modpacks.",
            "Bezahlung wie Vanilla: ein Item aus #minecraft:beacon_payment_items. Der Server prüft jede Wahl; eine Kraft, die Pyramide oder Server nicht erlauben, wird abgelehnt und die Bezahlung bleibt liegen."
          ]
        }
      },
      "texture": "assets/textures/simplebeacons/block/strong_beacon.png",
      "questionary": {
        "en": {
          "lore": [
            "Six steps closer to the sky."
          ],
          "use": [
            "Build a pyramid of up to 6 levels (13x13 at the bottom, 454 blocks). Level 5 offers Night Vision, Water Breathing and Slow Falling, level 6 Fire Resistance, Dolphin's Grace and Absorption.",
            "Range 60 blocks at level 5 and 70 at level 6."
          ],
          "secret": [
            "The new powers have no level II, but Regeneration still works as the second power."
          ]
        },
        "de": {
          "lore": [
            "Sechs Stufen näher am Himmel."
          ],
          "use": [
            "Eine Pyramide mit bis zu 6 Ebenen bauen (13x13 unten, 454 Blöcke). Ebene 5 bietet Nachtsicht, Wasseratmung und Langsamen Fall, Ebene 6 Feuerresistenz, Delfingunst und Absorption.",
            "Reichweite 60 Blöcke auf Ebene 5 und 70 auf Ebene 6."
          ],
          "secret": [
            "Die neuen Kräfte haben keine Stufe II, Regeneration bleibt aber als zweite Kraft."
          ]
        }
      },
      "craftedBy": [
        "simplebeacons:strong_beacon_smithing"
      ],
      "usedIn": []
    },
    {
      "id": "simplebeacons:strong_conduit",
      "name": {
        "en_us": "Strong Conduit",
        "de_de": "Starker Aquisator"
      },
      "note": {
        "en": {
          "title": "Strong Conduit",
          "summary": "A conduit that also accepts a 7x7x7 frame: three crossing 7x7 rings of prismarine, prismarine bricks, dark prismarine or sea lanterns (66 blocks). Range is (frame blocks / 7) x 16, up to 144 blocks with the full large frame.",
          "details": [
            "Smithing table: conduit + Strong Upgrade + nether star. With the Vanilla 5x5x5 frame (42 blocks) it works like a Vanilla conduit (up to 96 blocks).",
            "It needs water around it and at least 16 frame blocks, like Vanilla.",
            "Players in the water or rain get Conduit Power like Vanilla; players in the water also get Dolphin's Grace. Beyond 96 blocks only players really under water count, not rain.",
            "The full 7x7x7 frame attacks hostile mobs in the water within 12 blocks (the full 5x5x5 frame: 8), with Vanilla's damage.",
            "It looks like a Vanilla conduit."
          ]
        },
        "de": {
          "title": "Starker Aquisator",
          "summary": "Ein Aquisator, der zusätzlich einen 7x7x7-Rahmen annimmt: drei sich kreuzende 7x7-Ringe aus Prismarin, Prismarinziegeln, Dunklem Prismarin oder Seelaternen (66 Blöcke). Die Reichweite ist (Rahmenblöcke / 7) x 16, bis 144 Blöcke mit dem vollen großen Rahmen.",
          "details": [
            "Schmiedetisch: Aquisator + Starke Aufwertungsvorlage + Netherstern. Mit dem Vanilla-Rahmen 5x5x5 (42 Blöcke) wirkt er wie ein Vanilla-Aquisator (bis 96 Blöcke).",
            "Er braucht Wasser um sich und mindestens 16 Rahmenblöcke, wie Vanilla.",
            "Spieler im Wasser oder Regen erhalten Meereskraft wie bei Vanilla; Spieler im Wasser zusätzlich Delfingunst. Jenseits von 96 Blöcken zählen nur Spieler wirklich unter Wasser, nicht im Regen.",
            "Der volle 7x7x7-Rahmen greift feindliche Mobs im Wasser im Umkreis von 12 Blöcken an (der volle 5x5x5-Rahmen: 8), mit Vanilla-Schaden.",
            "Er sieht aus wie ein Vanilla-Aquisator."
          ]
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "The sea answers a larger frame with a wider reach."
          ],
          "use": [
            "Frame it with a 7x7x7 ring of prismarine, dark prismarine, prismarine bricks or sea lanterns (66 blocks) for up to 144 blocks of Conduit Power.",
            "Players in the water also get Dolphin's Grace."
          ],
          "secret": [
            "The full large frame strikes hostile mobs in the water within 12 blocks."
          ]
        },
        "de": {
          "lore": [
            "Das Meer antwortet einem größeren Rahmen mit weiterer Reichweite."
          ],
          "use": [
            "Mit einem 7x7x7-Ring aus Prismarin, Dunklem Prismarin, Prismarinziegeln oder Seelaternen (66 Blöcke) umgeben: bis zu 144 Blöcke Meereskraft.",
            "Spieler im Wasser bekommen zusätzlich Delfingunst."
          ],
          "secret": [
            "Der volle große Rahmen trifft feindliche Mobs im Wasser im Umkreis von 12 Blöcken."
          ]
        }
      },
      "craftedBy": [
        "simplebeacons:strong_conduit_smithing"
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
    "features": 6,
    "recipes": 6,
    "lootTables": 2,
    "tags": 1,
    "advancements": 0,
    "enchantments": 0,
    "items": 2,
    "blocks": 2,
    "trades": 0,
    "config": 0,
    "quests": 0,
    "recipesOtherLines": 0,
    "undocumented": 0,
    "inWorld": 0,
    "incompleteProse": 0
  }
};
