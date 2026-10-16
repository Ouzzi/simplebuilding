window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplecrucibles"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplecrucibles",
    "name": "Simple Crucibles",
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
      "id": "crucible",
      "en": {
        "title": "Crucibles",
        "summary": "A cooking station without fuel that cooks every slot at once, heated by the block below.",
        "details": [
          "Iron 6 slots, Reinforced 12 (2x6), Netherite 18, Enderite 30 (5x6). Speed 1x/2x/4x/8x; Netherite and Enderite count experience twice. The Enderite crucible and barrel were SimpleBuilding's; they belong to this mod now and old worlds migrate.",
          "Heat comes from the block directly below, or two blocks below at 10% less speed (air, a hopper or a non-full block in between). Medium: campfire, soul campfire, magma block, flowing lava (0.5x, campfire and smoker recipes). High: lava, lava cauldron, flowing soul lava (0.75x, also furnace and blast furnace recipes). Extreme: soul lava (1x, also ancient debris). Torches, candles and plain fire give no heat. In the Nether every source is one level hotter, at most high.",
          "Before a slot starts, it reserves the place for its result: the slot directly below first, then any other free or matching slot. A reserved slot shows the result faintly; anything else put there stops that slot until it is taken out. Finished results are never cooked again until taken out.",
          "After the heat source is gone the crucible keeps its heat for 2/4/8/16 seconds. Hoppers insert from above and the sides and take finished results from below. The crucible is walk-in like a cauldron: its walls collide only up to 9/16 of a block, so you step into it, and at high heat or more (lava, soul lava) standing in it burns like a magma block - 1 damage per hit, not while sneaking (server option crucibleBurnDamage, default 1, 0-4, 0 = off).",
          "Build an iron crucible in the world: hold an axe, put iron ingots in the off hand and use it on an empty cauldron sixteen times, one ingot per strike (a cauldron with water, lava or powder snow has to be emptied first). The first strike turns the cauldron into the base; strikes 1-8 raise the four side plates (each first half, then whole), 9-12 the two handles (half, then whole), 13-16 the four rim pieces. Every strike is visible as its own stage; breaking the half-built crucible gives back the cauldron and every ingot struck in (a blank started on an iron block in older worlds gives its iron block back and can be finished the same way).",
          "Upgrade in place without SimpleBuilding: axe in the main hand, twelve strikes on the crucible; every third strike takes one unit from the off hand and shows it as a band of the new material on one side, four units in all: Iron to Reinforced with cracked diamonds (or diamonds), Reinforced to Netherite with netherite nuggets (or netherite scrap). A crucible broken midway gives the units back. Contents, progress, experience and an attached barrel stay. With SimpleBuilding the sledgehammer replaces the axe (same 12 strikes, 4 cracked diamonds / netherite nuggets / enderite nuggets).",
          "Raised stack limits (an Enderite crucible or barrel holds 128 of a 64-stack item) hold everywhere: in the crucible menu, in a loose barrel's own chest menu, for shift-clicks and for hoppers, which top such slots up to the raised limit (also double chests). Taking from such a slot gives at most one normal stack."
        ]
      },
      "de": {
        "title": "Schmelztiegel",
        "summary": "Garstation ohne Brennstoff, die alle Plätze zugleich gart, beheizt vom Block darunter.",
        "details": [
          "Eisen 6 Plätze, Verstärkt 12 (2x6), Netherit 18, Enderit 30 (5x6). Tempo 1×/2×/4×/8×; Netherit und Enderit zählen die Erfahrung doppelt. Enderit-Tiegel und -Fass gehörten SimpleBuilding; sie gehören jetzt zu dieser Mod, alte Welten werden migriert.",
          "Hitze kommt vom Block direkt darunter oder zwei Blöcke darunter mit 10 % weniger Tempo (dazwischen Luft, ein Trichter oder ein nicht voller Block). Mittel: Lagerfeuer, Seelen-Lagerfeuer, Magmablock, fließende Lava (0,5×, Lagerfeuer- und Räucherofen-Rezepte). Hoch: Lava, Lavakessel, fließende Seelen-Lava (0,75×, auch Ofen- und Schmelzofen-Rezepte). Extrem: Seelen-Lava (1×, auch antiker Schrott). Fackeln, Kerzen und normales Feuer geben keine Hitze. Im Nether ist jede Quelle eine Stufe heißer, höchstens hoch.",
          "Bevor ein Platz anfängt, reserviert er den Platz für sein Ergebnis: zuerst den Platz direkt darunter, sonst einen anderen freien oder passenden. Ein reservierter Platz zeigt das Ergebnis blass; legt man etwas anderes hinein, stoppt dieser Platz, bis es wieder herausgenommen wird. Fertige Ergebnisse werden nie erneut gegart, bis man sie entnimmt.",
          "Nach dem Entfernen der Hitzequelle hält der Tiegel die Hitze 2/4/8/16 Sekunden. Trichter füllen von oben und von der Seite ein und nehmen fertige Ergebnisse unten heraus. Der Tiegel ist betretbar wie ein Kessel: seine Wände sind nur bis 9/16 Block fest, man steigt also hinein, und ab hoher Hitze (Lava, Seelen-Lava) brennt das Stehen darin wie ein Magmablock - 1 Schaden je Treffer, nicht beim Schleichen (Server-Option crucibleBurnDamage, Standard 1, 0-4, 0 = aus).",
          "Eisen-Tiegel in der Welt bauen: Axt in der Hand, Eisenbarren in der Nebenhand und sechzehnmal auf einen leeren Kessel anwenden, ein Barren je Schlag (ein Kessel mit Wasser, Lava oder Pulverschnee muss erst geleert werden). Der erste Schlag macht den Kessel zum Boden; Schläge 1-8 setzen die vier Seitenplatten (je erst halb, dann ganz), 9-12 die zwei Griffe (halb, dann ganz), 13-16 die vier Randstücke. Jeder Schlag ist als eigene Stufe sichtbar; wird der halbfertige Tiegel abgebaut, kommen der Kessel und alle verbauten Barren zurück (ein in älteren Welten auf einem Eisenblock begonnener Rohling gibt seinen Eisenblock zurück und lässt sich genauso fertig bauen).",
          "Aufwerten ohne SimpleBuilding: Axt in der Haupthand, zwölf Schläge auf den Tiegel; jeder dritte Schlag nimmt eine Einheit aus der Nebenhand und zeigt sie als Band des neuen Materials auf einer Seite, vier Einheiten insgesamt: Eisen zu Verstärkt mit rissigen Diamanten (oder Diamanten), Verstärkt zu Netherit mit Netheritklumpen (oder Netheritschrott). Wird der Tiegel vorher abgebaut, kommen die Einheiten zurück. Inhalt, Fortschritt, Erfahrung und ein angedocktes Fass bleiben. Mit SimpleBuilding ersetzt der Vorschlaghammer die Axt (ebenfalls 12 Schläge, 4 rissige Diamanten / Netheritklumpen / Enderitklumpen).",
          "Erhöhte Stapelgrenzen (ein Enderit-Tiegel oder -Fass fasst 128 eines 64er-Items) gelten überall: im Tiegelmenü, im eigenen Truhenmenü eines losen Fasses, beim Shift-Klick und für Trichter, die solche Plätze bis zur erhöhten Grenze füllen (auch Doppeltruhen). Herausnehmen gibt höchstens einen normalen Stapel."
        ]
      },
      "sources": [
        "modules/simplelib/shared/java/com/simplelib/api/StackLimits.java",
        "modules/simplecrucibles/shared/java/com/simplecrucibles/crucible/BarrelMenu.java",
        "modules/simplelib/shared/java/com/simplelib/mixin/HopperStackLimitsMixin.java"
      ]
    },
    {
      "id": "village_kitchen",
      "en": {
        "title": "Village field kitchens",
        "summary": "Villages of all five types can have a small field kitchen: an iron crucible on an unlit campfire with cooked food, a barrel of raw meat and a seat.",
        "details": [
          "The kitchen is added to each village houses pool at server start with weight 3 (about every third village); server config villageKitchenWeight 0–10, 0 switches it off. Only newly generated villages get it.",
          "Light the campfire to warm the food in the crucible."
        ]
      },
      "de": {
        "title": "Dorf-Feldküchen",
        "summary": "Dörfer aller fünf Arten können eine kleine Feldküche haben: ein Eisen-Schmelztiegel auf einem ausgemachten Lagerfeuer mit gegartem Essen, ein Fass mit rohem Fleisch und eine Sitzgelegenheit.",
        "details": [
          "Die Küche wird beim Serverstart jedem Häuser-Pool der Dörfer mit Gewicht 3 hinzugefügt (etwa jedes dritte Dorf); Server-Config villageKitchenWeight 0–10, 0 schaltet sie ab. Nur neu erzeugte Dörfer bekommen sie.",
          "Zündet man das Lagerfeuer an, wärmt der Tiegel das Essen auf."
        ]
      }
    },
    {
      "id": "reinforced_cauldron",
      "en": {
        "title": "Reinforced cauldron",
        "summary": "A cauldron for water, lava, powder snow and - with SimpleBuilding - soul lava; lava in it heats a crucible like a lava source.",
        "details": [
          "Without SimpleBuilding: hold an axe, put 8 diamonds in the off hand and use it on a cauldron five times. With SimpleBuilding the sledgehammer and 8 cracked diamonds replace the axe and the diamonds.",
          "It works like a Vanilla cauldron and uses the same interactions: buckets, bottles in three levels, washing dyed items and banners, rain, snow and dripstone, and what other mods add to the cauldron (milk from Simple Sandwiches turns it into a reinforced milk cauldron that becomes this cauldron again when emptied). Copper and Enderite buckets (SimpleBuilding) take only a full cauldron. As a crucible heat source it counts like its content: lava high, soul lava extreme. A normal cauldron does not take soul lava."
        ]
      },
      "de": {
        "title": "Verstärkter Kessel",
        "summary": "Ein Kessel für Wasser, Lava, Pulverschnee und - mit SimpleBuilding - Seelen-Lava; Lava darin heizt einen Schmelztiegel wie eine Lavaquelle.",
        "details": [
          "Ohne SimpleBuilding: eine Axt halten, 8 Diamanten in die Nebenhand und fünfmal auf einen Kessel anwenden. Mit SimpleBuilding ersetzen Vorschlaghammer und 8 Rissige Diamanten Axt und Diamanten.",
          "Er funktioniert wie ein Vanilla-Kessel und nutzt dieselben Interaktionen: Eimer, Flaschen in drei Stufen, Waschen gefärbter Gegenstände und Banner, Regen, Schnee und Tropfstein sowie alles, was andere Mods dem Kessel hinzufügen (Milch aus Simple Sandwiches macht ihn zum verstärkten Milchkessel, der beim Leeren wieder dieser Kessel wird). Kupfer- und Enderit-Eimer (SimpleBuilding) nehmen nur aus einem vollen Kessel. Als Hitzequelle eines Schmelztiegels zählt er wie sein Inhalt: Lava hoch, Seelen-Lava extrem. Ein normaler Kessel nimmt keine Seelen-Lava."
        ]
      }
    },
    {
      "id": "first_pickup_tips",
      "sources": [
        "modules/simplecrucibles/shared/java/com/simplecrucibles/discover/CruciblesDiscover.java",
        "modules/simplecrucibles/shared/java/com/simplecrucibles/mixin/DiscoverTipMixin.java",
        "framework/src/main/java/com/simplebuilding/framework/api/FirstUseTips.java",
        "tools/discover/discover.py"
      ],
      "en": {
        "title": "First pickup tips",
        "summary": "The first time you pick up a machine or tool whose use is not obvious, one short tip shows in the action bar - once per player and world.",
        "details": [
          "Tips exist for crucibles and crucible barrels.",
          "A tip counts as seen through an entity tag on the player (simplecrucibles.tip.<group>); removing the tag with /tag shows it again.",
          "Switch: config/simple-tips.properties, key simplecrucibles.firstPickupTips=false turns the tips of this mod off. The file is read at every tip, so no restart is needed; every Simple mod with tips has its own key there.",
          "Every craftable item of the mod is in the recipe book: picking up one of its ingredients unlocks the recipe."
        ]
      },
      "de": {
        "title": "Tipps beim ersten Aufheben",
        "summary": "Hebst du zum ersten Mal eine Maschine oder ein Werkzeug auf, dessen Bedienung nicht offensichtlich ist, zeigt die Aktionsleiste einen kurzen Tipp - einmal je Spieler und Welt.",
        "details": [
          "Tipps gibt es für Tiegel und Tiegel-Fässer.",
          "Ein Tipp gilt über ein Entity-Tag am Spieler (simplecrucibles.tip.<gruppe>) als gesehen; entfernt man das Tag mit /tag, erscheint er wieder.",
          "Schalter: config/simple-tips.properties, Schlüssel simplecrucibles.firstPickupTips=false schaltet die Tipps dieser Mod ab. Die Datei wird bei jedem Tipp gelesen, ein Neustart ist nicht nötig; jede Simple-Mod mit Tipps hat dort ihren eigenen Schlüssel.",
          "Jedes herstellbare Item der Mod steht im Rezeptbuch: Das Aufheben einer seiner Zutaten schaltet das Rezept frei."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simplecrucibles:copper_barrel",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplecrucibles:copper_barrel",
        "count": 1
      },
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/recipe/copper_barrel.json",
      "ingredients": [
        "minecraft:barrel",
        "minecraft:copper_ingot"
      ],
      "pattern": [
        " C ",
        "CBC",
        " C "
      ],
      "key": {
        "C": [
          "minecraft:copper_ingot"
        ],
        "B": [
          "minecraft:barrel"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:copper_ingot",
            "count": 4
          },
          {
            "id": "minecraft:oak_log",
            "count": 1.75
          }
        ]
      }
    },
    {
      "id": "simplecrucibles:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplecrucibles:guide_book",
        "count": 1
      },
      "source": "modules/simplecrucibles/shared/resources/data/simplecrucibles/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:cauldron"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:cauldron"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:iron_ingot",
            "count": 7
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
      "id": "simplecrucibles:blocks/copper_barrel",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplecrucibles:copper_barrel"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/loot_table/blocks/copper_barrel.json"
    },
    {
      "id": "simplecrucibles:blocks/crucible_blank",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "minecraft:cauldron"
          ],
          "conditions": [],
          "functions": []
        },
        {
          "rolls": 1,
          "items": [
            "minecraft:iron_block"
          ],
          "conditions": [],
          "functions": []
        },
        {
          "rolls": 1,
          "items": [
            "minecraft:iron_ingot"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/loot_table/blocks/crucible_blank.json"
    },
    {
      "id": "simplecrucibles:blocks/enderite_barrel",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplecrucibles:enderite_barrel"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/loot_table/blocks/enderite_barrel.json"
    },
    {
      "id": "simplecrucibles:blocks/enderite_crucible",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplecrucibles:enderite_crucible"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/loot_table/blocks/enderite_crucible.json"
    },
    {
      "id": "simplecrucibles:blocks/iron_crucible",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplecrucibles:iron_crucible"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/loot_table/blocks/iron_crucible.json"
    },
    {
      "id": "simplecrucibles:blocks/netherite_barrel",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplecrucibles:netherite_barrel"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/loot_table/blocks/netherite_barrel.json"
    },
    {
      "id": "simplecrucibles:blocks/netherite_crucible",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplecrucibles:netherite_crucible"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/loot_table/blocks/netherite_crucible.json"
    },
    {
      "id": "simplecrucibles:blocks/reinforced_barrel",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplecrucibles:reinforced_barrel"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/loot_table/blocks/reinforced_barrel.json"
    },
    {
      "id": "simplecrucibles:blocks/reinforced_cauldron",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplecrucibles:reinforced_cauldron"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/loot_table/blocks/reinforced_cauldron.json"
    },
    {
      "id": "simplecrucibles:blocks/reinforced_crucible",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplecrucibles:reinforced_crucible"
          ],
          "conditions": [
            "minecraft:survives_explosion"
          ],
          "functions": []
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/loot_table/blocks/reinforced_crucible.json"
    },
    {
      "id": "simplecrucibles:chests/village_field_kitchen",
      "kind": "chests",
      "type": "minecraft:chest",
      "pools": [
        {
          "rolls": {
            "type": "minecraft:uniform",
            "min": 2,
            "max": 4
          },
          "items": [
            "minecraft:beef",
            "minecraft:porkchop",
            "minecraft:mutton",
            "minecraft:chicken",
            "minecraft:rabbit",
            "minecraft:potato"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/loot_table/chests/village_field_kitchen.json"
    }
  ],
  "tags": [
    {
      "id": "simplecrucibles:block/heat_source/extreme",
      "replace": false,
      "values": [],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/tags/block/heat_source/extreme.json"
    },
    {
      "id": "simplecrucibles:block/heat_source/high",
      "replace": false,
      "values": [
        {
          "id": "minecraft:lava_cauldron",
          "required": true
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/tags/block/heat_source/high.json"
    },
    {
      "id": "simplecrucibles:block/heat_source/medium",
      "replace": false,
      "values": [
        {
          "id": "minecraft:campfire",
          "required": true
        },
        {
          "id": "minecraft:soul_campfire",
          "required": true
        },
        {
          "id": "minecraft:magma_block",
          "required": true
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/tags/block/heat_source/medium.json"
    },
    {
      "id": "simplecrucibles:fluid/extreme_heat",
      "replace": false,
      "values": [],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/tags/fluid/extreme_heat.json"
    },
    {
      "id": "simplecrucibles:item/crucible_handles",
      "replace": false,
      "values": [
        {
          "id": "minecraft:iron_ingot",
          "required": true
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/tags/item/crucible_handles.json"
    },
    {
      "id": "simplecrucibles:item/crucible_upgrade_netherite",
      "replace": false,
      "values": [
        {
          "id": "simplelib:netherite_nugget",
          "required": true
        },
        {
          "id": "minecraft:netherite_scrap",
          "required": true
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/tags/item/crucible_upgrade_netherite.json"
    },
    {
      "id": "simplecrucibles:item/crucible_upgrade_reinforced",
      "replace": false,
      "values": [
        {
          "id": "simplelib:cracked_diamond",
          "required": true
        },
        {
          "id": "minecraft:diamond",
          "required": true
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/tags/item/crucible_upgrade_reinforced.json"
    },
    {
      "id": "simplecrucibles:item/crucible_walls",
      "replace": false,
      "values": [
        {
          "id": "minecraft:iron_ingot",
          "required": true
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/tags/item/crucible_walls.json"
    },
    {
      "id": "simplecrucibles:item/needs_extreme_heat",
      "replace": false,
      "values": [
        {
          "id": "minecraft:ancient_debris",
          "required": true
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/tags/item/needs_extreme_heat.json"
    },
    {
      "id": "simplecrucibles:item/upgrade_netherite",
      "replace": false,
      "values": [
        {
          "id": "minecraft:netherite_ingot",
          "required": true
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/tags/item/upgrade_netherite.json"
    },
    {
      "id": "simplecrucibles:item/upgrade_reinforced",
      "replace": false,
      "values": [
        {
          "id": "minecraft:diamond",
          "required": true
        }
      ],
      "source": "modules/simplecrucibles/generated/resources/data/simplecrucibles/tags/item/upgrade_reinforced.json"
    }
  ],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "simplecrucibles:guide_book",
      "name": {
        "en_us": "Simple Crucibles Guide",
        "de_de": "Simple-Crucibles-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simplecrucibles/shared/java/com/simplecrucibles/guide/CruciblesGuide.java",
          "modules/simplecrucibles/shared/resources/data/simplecrucibles/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 5 pages taken from this wiki, shown in your language. Shapeless recipe: book + cauldron. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Crucibles\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 5 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Kessel. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Crucibles\" eins."
        }
      },
      "craftedBy": [
        "simplecrucibles:guide_book"
      ],
      "usedIn": []
    }
  ],
  "blocks": [
    {
      "id": "simplecrucibles:copper_barrel",
      "name": {
        "en_us": "Copper Barrel",
        "de_de": "Kupfer-Fass"
      },
      "note": {
        "en": {
          "summary": "A barrel of 27 slots (barrel surrounded by four copper ingots). Next to a crucible, six axe strikes (sledgehammer with SimpleBuilding) attach it: every strike shows cracks, then the barrel docks to the crucible with a flange and a chute, offers only as many slots as the crucible has (6/12/18/27; the rest stays stored, hidden, and comes back when detached) and opens the crucible screen; the crucible puts its results there first, a hopper below the barrel takes them out. Breaking the barrel drops its contents and the plain barrel; breaking the crucible makes it a normal barrel again."
        },
        "de": {
          "summary": "Ein Fass mit 27 Plätzen (Fass umringt von vier Kupferbarren). Neben einem Schmelztiegel mit sechs Axtschlägen (mit SimpleBuilding Vorschlaghammer) angebracht, zeigt jeder Schlag Risse, dann dockt das Fass mit Flansch und Rinne am Tiegel an, bietet nur so viele Plätze wie der Tiegel (6/12/18/27; der Rest bleibt verwahrt, unsichtbar, und ist nach dem Abnehmen wieder da) und öffnet das Tiegel-Fenster; der Tiegel legt seine Ergebnisse zuerst dort ab, ein Trichter unter dem Fass nimmt sie heraus. Fass abbauen gibt Inhalt und normales Fass, Tiegel abbauen macht es wieder zum normalen Fass."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "A barrel clad in copper, a little greener each year."
          ],
          "use": [
            "The copper tier of the crucible barrel: it attaches to a crucible as extra storage, and works as a normal barrel on its own."
          ],
          "secret": [
            "It is the first step of the barrel line, from copper through netherite to enderite."
          ]
        },
        "de": {
          "lore": [
            "Ein Fass mit Kupferhülle, jedes Jahr ein wenig grüner."
          ],
          "use": [
            "Die Kupferstufe des Tiegelfasses: Es lässt sich als Zusatzlager an einen Tiegel hängen und dient allein als normales Fass."
          ],
          "secret": [
            "Es ist die erste Stufe der Fassreihe, von Kupfer über Netherit zu Enderit."
          ]
        }
      },
      "craftedBy": [
        "simplecrucibles:copper_barrel"
      ],
      "usedIn": []
    },
    {
      "id": "simplecrucibles:crucible_blank",
      "name": {
        "en_us": "Crucible Blank",
        "de_de": "Schmelztiegel-Rohling"
      },
      "note": {
        "en": {
          "summary": "A half-built iron crucible; breaking it returns the cauldron (an older blank: its iron block) and every iron ingot used so far."
        },
        "de": {
          "summary": "Ein halb gebauter Eisen-Schmelztiegel; abgebaut gibt er den Kessel (ein älterer Rohling: seinen Eisenblock) und alle bisher verbauten Eisenbarren zurück."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "A block of metal that has not yet become a pot."
          ],
          "use": [
            "The crucible while it is being built: each strike on the blank adds a stage (base, four walls, handle) and cracks it further. The last strike turns it into an Iron Crucible."
          ],
          "secret": [
            "It is the block you see halfway through building a crucible in the world. It has no recipe of its own."
          ]
        },
        "de": {
          "lore": [
            "Ein Metallklotz, der noch kein Topf geworden ist."
          ],
          "use": [
            "Der Tiegel im Bau: Jeder Schlag auf den Rohling fügt eine Stufe hinzu (Boden, vier Wände, Griff) und lässt ihn weiter reißen. Der letzte Schlag macht daraus einen Eisentiegel."
          ],
          "secret": [
            "Es ist der Block, den man mitten im Bau eines Tiegels in der Welt sieht. Er hat kein eigenes Rezept."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplecrucibles:enderite_barrel",
      "name": {
        "en_us": "Enderite Barrel",
        "de_de": "Enderit-Fass"
      },
      "note": {
        "en": {
          "summary": "Barrel with 54 slots and double stacks; docks onto a crucible like the other barrels and offers as many slots as the crucible has (see Crucibles)."
        },
        "de": {
          "summary": "Fass mit 54 Plätzen und doppelten Stapeln; dockt wie die anderen Fässer an einen Tiegel an und bietet so viele Plätze wie der Tiegel (siehe Schmelztiegel)."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "A barrel that was told it could keep more, and believed it."
          ],
          "use": [
            "The Enderite tier of the crucible's barrel: it holds double stacks, 128 of a 64-stack item, in the crucible menu and its own chest menu, for shift-clicks and for hoppers."
          ],
          "secret": [
            "Taking from such a slot gives at most one normal stack. It is the last step of the line from copper through Netherite to Enderite."
          ]
        },
        "de": {
          "lore": [
            "Ein Fass, dem gesagt wurde, es dürfe mehr behalten, und das es glaubte."
          ],
          "use": [
            "Die Enderit-Stufe des Fasses der Tiegel: Es fasst doppelte Stapel, 128 eines 64er-Gegenstands, im Tiegelmenü und im eigenen Truhenmenü, bei Shift-Klicks und für Trichter."
          ],
          "secret": [
            "Aus so einem Platz bekommst du höchstens einen normalen Stapel. Es ist der letzte Schritt der Linie von Kupfer über Netherit zu Enderit."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplecrucibles:enderite_crucible",
      "name": {
        "en_us": "Enderite Crucible",
        "de_de": "Enderit-Schmelztiegel"
      },
      "note": {
        "en": {
          "summary": "Crucible with 30 slots (5x6), 8x speed and double stacks; a Netherite crucible becomes it with Enderite nuggets and a Netherite sledgehammer (see Crucibles)."
        },
        "de": {
          "summary": "Schmelztiegel mit 30 Plätzen (5x6), 8-fachem Tempo und doppelten Stapeln; ein Netherit-Tiegel wird mit Enderitklumpen und einem Netherit-Vorschlaghammer dazu (siehe Schmelztiegel)."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "A crucible that cooks like the End's own fire."
          ],
          "use": [
            "The Enderite tier of the crucible: 27 slots, double stacks, 8x speed. A Netherite crucible becomes this with Enderite Nuggets and a Netherite Sledgehammer, at twice the strikes of a furnace."
          ],
          "secret": [
            "The sledgehammer replaces the axe of the base mod here: strike the iron crucible onto an iron block, and attach a barrel with 6 strikes."
          ]
        },
        "de": {
          "lore": [
            "Ein Tiegel, der kocht wie das eigene Feuer des Endes."
          ],
          "use": [
            "Die Enderit-Stufe des Tiegels: 27 Plätze, doppelte Stapel, 8-faches Tempo. Ein Netherit-Tiegel wird mit Enderitklumpen und einem Netherit-Vorschlaghammer dazu, mit doppelt so vielen Schlägen wie ein Ofen."
          ],
          "secret": [
            "Der Vorschlaghammer ersetzt hier die Axt des Grundmods: Schlage den Eisentiegel auf einen Eisenblock und setze mit 6 Schlägen ein Fass an."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplecrucibles:iron_crucible",
      "name": {
        "en_us": "Iron Crucible",
        "de_de": "Eisen-Schmelztiegel"
      },
      "note": {
        "en": {
          "summary": "Crucible with 6 slots at furnace speed; built in the world from an empty cauldron and 16 iron ingots (see Crucibles)."
        },
        "de": {
          "summary": "Schmelztiegel mit 6 Plätzen in Ofen-Tempo; wird in der Welt aus einem leeren Kessel und 16 Eisenbarren gebaut (siehe Schmelztiegel)."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "A pot of iron that cooks without a fire of its own."
          ],
          "use": [
            "A cooking station without fuel: every slot cooks at once, heated by the block below or two blocks below. 6 slots, speed 1x."
          ],
          "secret": [
            "Build it in the world: an axe in the main hand, iron ingots in the off hand, sixteen strikes on an empty cauldron - side plates, handles, rim, one ingot each. Walk into it like a cauldron."
          ]
        },
        "de": {
          "lore": [
            "Ein Topf aus Eisen, der ohne eigenes Feuer kocht."
          ],
          "use": [
            "Eine Kochstelle ohne Brennstoff: Alle Plätze kochen gleichzeitig, beheizt vom Block darunter oder zwei Blöcke darunter. 6 Plätze, Tempo 1x."
          ],
          "secret": [
            "Im Spiel gebaut: Axt in der Haupthand, Eisenbarren in der Nebenhand, sechzehn Schläge auf einen leeren Kessel - Seitenplatten, Griffe, Rand, je ein Barren. Man kann wie in einen Kessel hineintreten."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplecrucibles:netherite_barrel",
      "name": {
        "en_us": "Netherite Barrel",
        "de_de": "Netherit-Fass"
      },
      "note": {
        "en": {
          "summary": "A reinforced barrel upgraded in place (axe, 1 netherite ingot, ten strikes; with SimpleBuilding sledgehammer and 2 netherite nuggets): 45 slots on its own, 9 when attached to a crucible; survives fire and lava as an item."
        },
        "de": {
          "summary": "Ein in der Welt aufgewertetes verstärktes Fass (Axt, 1 Netheritbarren, zehn Schläge; mit SimpleBuilding Vorschlaghammer und 2 Netheritklumpen): allein 45 Plätze, am Tiegel 9; als Gegenstand feuer- und lavafest."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "A barrel of dark metal that does not burn."
          ],
          "use": [
            "The Netherite tier of the crucible barrel: attach it to a crucible for even more storage."
          ],
          "secret": [
            "It is fire resistant, like all Netherite blocks."
          ]
        },
        "de": {
          "lore": [
            "Ein Fass aus dunklem Metall, das nicht brennt."
          ],
          "use": [
            "Die Netherit-Stufe des Tiegelfasses: An einen Tiegel gehängt bietet es noch mehr Lagerplatz."
          ],
          "secret": [
            "Es ist feuerfest wie alle Netherit-Blöcke."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplecrucibles:netherite_crucible",
      "name": {
        "en_us": "Netherite Crucible",
        "de_de": "Netherit-Schmelztiegel"
      },
      "note": {
        "en": {
          "summary": "Crucible with 18 slots at four times the speed; counts experience twice."
        },
        "de": {
          "summary": "Schmelztiegel mit 18 Plätzen in vierfachem Tempo; zählt die Erfahrung doppelt."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "A pot that cooks at the speed of a Nether fire."
          ],
          "use": [
            "The Netherite tier of the crucible: 18 slots, speed 4x, experience is counted twice. A reinforced crucible becomes this in twelve strikes, one netherite nugget (or netherite scrap) every third strike."
          ],
          "secret": [
            "Lava or soul lava underneath makes it cook furnace and blast furnace recipes, and soul lava also ancient debris."
          ]
        },
        "de": {
          "lore": [
            "Ein Topf, der mit der Geschwindigkeit eines Netherfeuers kocht."
          ],
          "use": [
            "Die Netherit-Stufe des Tiegels: 18 Plätze, Tempo 4x, Erfahrung zählt doppelt. Ein verstärkter Tiegel wird in zwölf Schlägen dazu, jeder dritte nimmt einen Netheritklumpen (oder Netheritschrott)."
          ],
          "secret": [
            "Lava oder Seelenlava darunter lässt ihn Ofen- und Hochofenrezepte kochen, Seelenlava auch Antiken Schrott."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplecrucibles:reinforced_barrel",
      "name": {
        "en_us": "Reinforced Barrel",
        "de_de": "Verstärktes Fass"
      },
      "note": {
        "en": {
          "summary": "A copper barrel upgraded in place (axe, 2 diamonds, ten strikes): 36 slots on its own, 9 when attached to a crucible."
        },
        "de": {
          "summary": "Ein in der Welt aufgewertetes Kupfer-Fass (Axt, 2 Diamanten, zehn Schläge): allein 36 Plätze, am Tiegel 9."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "A barrel that has been told to hold more."
          ],
          "use": [
            "The Reinforced tier of the crucible barrel: attach it to a crucible for more storage."
          ],
          "secret": [
            "Hoppers fill it like a chest, from above and the sides."
          ]
        },
        "de": {
          "lore": [
            "Ein Fass, dem gesagt wurde, es solle mehr halten."
          ],
          "use": [
            "Die verstärkte Stufe des Tiegelfasses: An einen Tiegel gehängt bietet es mehr Lagerplatz."
          ],
          "secret": [
            "Trichter füllen es wie eine Truhe, von oben und von der Seite."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplecrucibles:reinforced_cauldron",
      "name": {
        "en_us": "Reinforced Cauldron",
        "de_de": "Verstärkter Kessel"
      },
      "note": {
        "en": {
          "summary": "A Vanilla cauldron that also holds (with SimpleBuilding) soul lava; heats a crucible like its content (see Reinforced cauldron)."
        },
        "de": {
          "summary": "Ein Vanilla-Kessel, der (mit SimpleBuilding) auch Seelen-Lava fasst; heizt einen Schmelztiegel wie sein Inhalt (siehe Verstärkter Kessel)."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "A cauldron reinforced against its own contents."
          ],
          "use": [
            "Works like a vanilla cauldron (buckets, bottles, washing, rain, snow, dripstone) and holds lava and powder snow. With SimpleBuilding it also holds soul lava. Lava underneath a crucible heats it."
          ],
          "secret": [
            "Make it with an axe and 8 diamonds in the off hand, five uses on a cauldron. With SimpleBuilding the sledgehammer and cracked diamonds replace them."
          ]
        },
        "de": {
          "lore": [
            "Ein Kessel, verstärkt gegen seinen eigenen Inhalt."
          ],
          "use": [
            "Funktioniert wie ein Vanilla-Kessel (Eimer, Flaschen, Waschen, Regen, Schnee, Tropfstein) und fasst Lava und Pulverschnee. Mit SimpleBuilding auch Seelenlava. Lava darin heizt einen Tiegel."
          ],
          "secret": [
            "Herstellen mit einer Axt und 8 Diamanten in der Nebenhand, fünfmal auf einen Kessel. Mit SimpleBuilding ersetzen Vorschlaghammer und rissige Diamanten beides."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplecrucibles:reinforced_crucible",
      "name": {
        "en_us": "Reinforced Crucible",
        "de_de": "Verstärkter Schmelztiegel"
      },
      "note": {
        "en": {
          "summary": "Crucible with 9 slots at double speed."
        },
        "de": {
          "summary": "Schmelztiegel mit 9 Plätzen in doppeltem Tempo."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "A pot that learned to take twice the load."
          ],
          "use": [
            "The Reinforced tier of the crucible: 12 slots (2x6), speed 2x. An iron crucible becomes this in twelve axe strikes, one cracked diamond (or diamond) every third strike (the sledgehammer with SimpleBuilding)."
          ],
          "secret": [
            "Contents, progress and experience stay when you upgrade in place."
          ]
        },
        "de": {
          "lore": [
            "Ein Topf, der gelernt hat, die doppelte Last zu tragen."
          ],
          "use": [
            "Die verstärkte Stufe des Tiegels: 12 Plätze (2x6), Tempo 2x. Ein Eisentiegel wird in zwölf Axtschlägen dazu, jeder dritte nimmt einen rissigen Diamanten (oder Diamanten) aus der Nebenhand (mit SimpleBuilding der Vorschlaghammer)."
          ],
          "secret": [
            "Inhalt, Fortschritt und Erfahrung bleiben beim Aufrüsten an Ort und Stelle erhalten."
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
    "features": 4,
    "recipes": 2,
    "lootTables": 11,
    "tags": 11,
    "advancements": 0,
    "enchantments": 0,
    "items": 1,
    "blocks": 10,
    "trades": 0,
    "config": 0,
    "quests": 0,
    "recipesOtherLines": 0,
    "undocumented": 0,
    "inWorld": 0,
    "incompleteProse": 0
  }
};
