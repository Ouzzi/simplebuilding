window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplemaps"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplemaps",
    "name": "Simple Maps",
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
      "id": "wayfinder_maps",
      "sources": [
        "modules/simplemaps/shared/java/com/simplemaps/WayfinderMapItem.java",
        "modules/simplemaps/shared/java/com/simplemaps/Reveal.java",
        "modules/simplemaps/shared/java/com/simplemaps/WayfinderData.java",
        "modules/simplemaps/shared/java/com/simplemaps/MapsConfig.java",
        "modules/simplemaps/shared/java/com/simplemaps/MapsLoot.java"
      ],
      "en": {
        "title": "Wayfinder maps",
        "summary": "Endless maps, one per dimension: the plain map for the Overworld and other dimensions, a Nether map and an End map.",
        "details": [
          "While held in the main or off hand the map explores like a Vanilla map at 1:1 (default radius 96 blocks, server bounds 16–128), one 16th of the columns per tick, only in loaded chunks. Under a ceiling (Nether) the floor below the holder is mapped.",
          "A map binds to the dimension it is first used in. Which dimensions a map accepts is set by the dimension type tags simplemaps:nether_wayfinder and simplemaps:end_wayfinder; the plain map takes every dimension in neither tag.",
          "Only explored 128 x 128-block tiles are stored (colour and height per block); at most maxTilesPerMap tiles per map (default 4096, server bounds 16–16384).",
          "Recipes (shapeless): map + compass + 4 gold ingots + 1 echo shard; the Nether map needs 2 echo shards and a netherite scrap instead, the End map 2 echo shards and a shulker shell.",
          "Found in chests: cartographer and shipwreck map chests 5 % (plain map), bastions and Nether fortresses 2 % (Nether map), end city treasure 2 % (End map)."
        ]
      },
      "de": {
        "title": "Wegfinder-Karten",
        "summary": "Endlose Karten, eine je Dimension: die einfache Karte für Oberwelt und weitere Dimensionen, eine Nether- und eine End-Karte.",
        "details": [
          "In Haupt- oder Nebenhand deckt die Karte wie eine Vanilla-Karte im Maßstab 1:1 auf (Standard-Radius 96 Blöcke, Servergrenzen 16–128), je Tick ein Sechzehntel der Spalten, nur in geladenen Chunks. Unter einer Decke (Nether) wird der Boden unter dem Halter kartiert.",
          "Eine Karte bindet sich an die Dimension, in der sie zuerst benutzt wird. Welche Dimensionen eine Karte annimmt, legen die Dimensionstyp-Tags simplemaps:nether_wayfinder und simplemaps:end_wayfinder fest; die einfache Karte nimmt jede Dimension, die in keinem der beiden steht.",
          "Gespeichert werden nur erkundete Kacheln zu 128 × 128 Blöcken (Farbe und Höhe je Block); höchstens maxTilesPerMap Kacheln je Karte (Standard 4096, Servergrenzen 16–16384).",
          "Rezepte (formlos): Karte + Kompass + 4 Goldbarren + 1 Echo-Splitter; die Nether-Karte braucht stattdessen 2 Echo-Splitter und einen Netheritplatten, die End-Karte 2 Echo-Splitter und eine Shulker-Schale.",
          "Fundorte: Kartografen- und Schiffswrack-Kartentruhen 5 % (einfache Karte), Bastionen und Netherfestungen 2 % (Nether-Karte), Endstadt-Schatz 2 % (End-Karte)."
        ]
      }
    },
    {
      "id": "map_screen",
      "sources": [
        "modules/simplemaps/shared/java/com/simplemaps/client/WayfinderScreen.java",
        "modules/simplemaps/shared/java/com/simplemaps/client/Contours.java",
        "modules/simplemaps/shared/java/com/simplemaps/client/MapsClient.java"
      ],
      "en": {
        "title": "Map screen",
        "summary": "Use the map to open it: drag to move, mouse wheel or +/− to zoom (1, 4, 8, 16 and 64 blocks per pixel).",
        "details": [
          "The map area is 4:3 with a 16-pixel grid that always fits (12 x 9 cells). Left bookmarks: snap to grid, grid on/off, zoom, height lines. Right bookmarks: player and waypoints 1–8; a click centres the view (with snap the player's grid cell).",
          "Height lines colour explored land in bands of 8 blocks with a dark line between bands.",
          "With a recovery compass in the other hand the last death point appears on the map, also over unexplored ground."
        ]
      },
      "de": {
        "title": "Kartenansicht",
        "summary": "Benutzen öffnet sie: Ziehen verschiebt, Mausrad oder +/− zoomt (1, 4, 8, 16 und 64 Blöcke je Pixel).",
        "details": [
          "Die Kartenfläche ist 4:3 mit einem 16-Pixel-Raster, das immer ganz passt (12 × 9 Zellen). Lesezeichen links: am Raster einrasten, Raster an/aus, Zoom, Höhenlinien. Lesezeichen rechts: Spieler und Wegpunkte 1–8; ein Klick zentriert die Ansicht (mit Einrasten die Rasterzelle des Spielers).",
          "Höhenlinien färben erkundetes Land in Stufen zu 8 Blöcken mit dunkler Linie zwischen den Stufen.",
          "Mit Bergungskompass in der anderen Hand erscheint der letzte Todespunkt auf der Karte, auch über unerkundetem Gebiet."
        ]
      }
    },
    {
      "id": "waypoints",
      "sources": [
        "modules/simplemaps/shared/java/com/simplemaps/Waypoint.java",
        "modules/simplemaps/shared/java/com/simplemaps/Waypoints.java",
        "modules/simplemaps/shared/java/com/simplemaps/net/MapsNetwork.java",
        "modules/simplemaps/shared/java/com/simplemaps/client/MapsClient.java"
      ],
      "en": {
        "title": "Waypoints",
        "summary": "Up to eight waypoints belong to the map itself; they show in the locator bar while the map is in the main or off hand.",
        "details": [
          "Right-click the map → Create waypoint: everything except the eight waypoint bookmarks is greyed out; a free bookmark takes it at once, an occupied one turns red first and is replaced by a second click.",
          "Right-click a waypoint bookmark → configure (name up to 32 characters, one of 16 colours or a mob head as icon from the tag simplemaps:waypoint_heads) or delete."
        ]
      },
      "de": {
        "title": "Wegpunkte",
        "summary": "Bis zu acht Wegpunkte gehören der Karte selbst; sie erscheinen in der Locator-Bar, solange die Karte in Haupt- oder Nebenhand ist.",
        "details": [
          "Rechtsklick auf die Karte → Wegpunkt erstellen: alles außer den acht Wegpunkt-Lesezeichen wird ausgegraut; ein freies Lesezeichen übernimmt ihn sofort, ein belegtes färbt sich erst rot und wird mit dem zweiten Klick ersetzt.",
          "Rechtsklick auf ein Wegpunkt-Lesezeichen → konfigurieren (Name bis 32 Zeichen, eine von 16 Farben oder ein Mob-Kopf aus dem Tag simplemaps:waypoint_heads als Symbol) oder löschen."
        ]
      }
    },
    {
      "id": "cartography",
      "sources": [
        "modules/simplemaps/shared/java/com/simplemaps/Cartography.java",
        "modules/simplemaps/shared/java/com/simplemaps/mixin/CartographyTableMenuMixin.java"
      ],
      "en": {
        "title": "Cartography table",
        "summary": "Copy, extend and combine wayfinder maps at the cartography table; each can be switched off on the server.",
        "details": [
          "Wayfinder map + empty map: two copies of the same map, waypoints included (allowCopy).",
          "Wayfinder map + filled map of the same dimension: only the filled map is used up; its area is added where the wayfinder map is still unexplored (allowExtend).",
          "Two wayfinder maps of the same kind and dimension: the second one is used up, the areas are united; the first map keeps its waypoints, free slots take the second one's (allowCombine)."
        ]
      },
      "de": {
        "title": "Kartentisch",
        "summary": "Wegfinder-Karten am Kartentisch kopieren, erweitern und kombinieren; jede Funktion ist auf dem Server abschaltbar.",
        "details": [
          "Wegfinder-Karte + leere Karte: zwei Kopien derselben Karte, mit Wegpunkten (allowCopy).",
          "Wegfinder-Karte + gefüllte Karte derselben Dimension: nur die gefüllte Karte wird verbraucht; ihr Bereich ergänzt die noch unerkundeten Stellen (allowExtend).",
          "Zwei Wegfinder-Karten gleicher Art und Dimension: die zweite wird verbraucht, die Bereiche werden vereinigt; die erste behält ihre Wegpunkte, freie Plätze bekommen die der zweiten (allowCombine)."
        ]
      }
    },
    {
      "id": "item_frames",
      "sources": [
        "modules/simplemaps/shared/java/com/simplemaps/mixin/ItemFrameMixin.java",
        "modules/simplemaps/shared/java/com/simplemaps/net/MapsNetwork.java",
        "modules/simplemaps/shared/java/com/simplemaps/client/MapsClient.java"
      ],
      "en": {
        "title": "Maps in item frames",
        "summary": "A framed wayfinder map shows a stored view; right-click opens the map screen, and the frame shows where you scrolled to.",
        "details": [
          "Sneak + right-click rotates the frame as usual. Waypoints are read-only in a frame."
        ]
      },
      "de": {
        "title": "Karten im Gegenstandsrahmen",
        "summary": "Eine gerahmte Wegfinder-Karte zeigt einen gespeicherten Ausschnitt; Rechtsklick öffnet die Kartenansicht, danach zeigt der Rahmen den Ausschnitt, zu dem man gescrollt hat.",
        "details": [
          "Schleichen + Rechtsklick dreht den Rahmen wie gewohnt. Wegpunkte sind im Rahmen nur lesbar."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simplemaps:end_wayfinder_map",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplemaps:end_wayfinder_map",
        "count": 1
      },
      "source": "modules/simplemaps/shared/resources/data/simplemaps/recipe/end_wayfinder_map.json",
      "ingredients": [
        "minecraft:compass",
        "minecraft:echo_shard",
        "minecraft:gold_ingot",
        "minecraft:map",
        "minecraft:shulker_shell"
      ],
      "ingredientGroups": [
        [
          "minecraft:map"
        ],
        [
          "minecraft:compass"
        ],
        [
          "minecraft:gold_ingot"
        ],
        [
          "minecraft:gold_ingot"
        ],
        [
          "minecraft:gold_ingot"
        ],
        [
          "minecraft:gold_ingot"
        ],
        [
          "minecraft:echo_shard"
        ],
        [
          "minecraft:echo_shard"
        ],
        [
          "minecraft:shulker_shell"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:iron_ingot",
            "count": 8
          },
          {
            "id": "minecraft:sugar_cane",
            "count": 8
          },
          {
            "id": "minecraft:gold_ingot",
            "count": 4
          },
          {
            "id": "minecraft:echo_shard",
            "count": 2
          },
          {
            "id": "minecraft:redstone",
            "count": 2
          },
          {
            "id": "minecraft:shulker_shell",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simplemaps:nether_wayfinder_map",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplemaps:nether_wayfinder_map",
        "count": 1
      },
      "source": "modules/simplemaps/shared/resources/data/simplemaps/recipe/nether_wayfinder_map.json",
      "ingredients": [
        "minecraft:compass",
        "minecraft:echo_shard",
        "minecraft:gold_ingot",
        "minecraft:map",
        "minecraft:netherite_scrap"
      ],
      "ingredientGroups": [
        [
          "minecraft:map"
        ],
        [
          "minecraft:compass"
        ],
        [
          "minecraft:gold_ingot"
        ],
        [
          "minecraft:gold_ingot"
        ],
        [
          "minecraft:gold_ingot"
        ],
        [
          "minecraft:gold_ingot"
        ],
        [
          "minecraft:echo_shard"
        ],
        [
          "minecraft:echo_shard"
        ],
        [
          "minecraft:netherite_scrap"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:iron_ingot",
            "count": 8
          },
          {
            "id": "minecraft:sugar_cane",
            "count": 8
          },
          {
            "id": "minecraft:gold_ingot",
            "count": 4
          },
          {
            "id": "minecraft:echo_shard",
            "count": 2
          },
          {
            "id": "minecraft:redstone",
            "count": 2
          },
          {
            "id": "minecraft:netherite_scrap",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simplemaps:wayfinder_map",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplemaps:wayfinder_map",
        "count": 1
      },
      "source": "modules/simplemaps/shared/resources/data/simplemaps/recipe/wayfinder_map.json",
      "ingredients": [
        "minecraft:compass",
        "minecraft:echo_shard",
        "minecraft:gold_ingot",
        "minecraft:map"
      ],
      "ingredientGroups": [
        [
          "minecraft:map"
        ],
        [
          "minecraft:compass"
        ],
        [
          "minecraft:gold_ingot"
        ],
        [
          "minecraft:gold_ingot"
        ],
        [
          "minecraft:gold_ingot"
        ],
        [
          "minecraft:gold_ingot"
        ],
        [
          "minecraft:echo_shard"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:iron_ingot",
            "count": 8
          },
          {
            "id": "minecraft:sugar_cane",
            "count": 8
          },
          {
            "id": "minecraft:gold_ingot",
            "count": 4
          },
          {
            "id": "minecraft:redstone",
            "count": 2
          },
          {
            "id": "minecraft:echo_shard",
            "count": 1
          }
        ]
      }
    }
  ],
  "lootTables": [],
  "tags": [
    {
      "id": "simplemaps:dimension_type/end_wayfinder",
      "replace": false,
      "values": [
        {
          "id": "minecraft:the_end",
          "required": true
        }
      ],
      "source": "modules/simplemaps/shared/resources/data/simplemaps/tags/dimension_type/end_wayfinder.json"
    },
    {
      "id": "simplemaps:dimension_type/nether_wayfinder",
      "replace": false,
      "values": [
        {
          "id": "minecraft:the_nether",
          "required": true
        }
      ],
      "source": "modules/simplemaps/shared/resources/data/simplemaps/tags/dimension_type/nether_wayfinder.json"
    },
    {
      "id": "simplemaps:item/wayfinder_maps",
      "replace": false,
      "values": [
        {
          "id": "simplemaps:wayfinder_map",
          "required": true
        },
        {
          "id": "simplemaps:nether_wayfinder_map",
          "required": true
        },
        {
          "id": "simplemaps:end_wayfinder_map",
          "required": true
        }
      ],
      "source": "modules/simplemaps/shared/resources/data/simplemaps/tags/item/wayfinder_maps.json"
    },
    {
      "id": "simplemaps:item/waypoint_heads",
      "replace": false,
      "values": [
        {
          "id": "minecraft:skeleton_skull",
          "required": true
        },
        {
          "id": "minecraft:wither_skeleton_skull",
          "required": true
        },
        {
          "id": "minecraft:zombie_head",
          "required": true
        },
        {
          "id": "minecraft:creeper_head",
          "required": true
        },
        {
          "id": "minecraft:piglin_head",
          "required": true
        },
        {
          "id": "minecraft:dragon_head",
          "required": true
        },
        {
          "id": "minecraft:player_head",
          "required": true
        }
      ],
      "source": "modules/simplemaps/shared/resources/data/simplemaps/tags/item/waypoint_heads.json"
    }
  ],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "simplemaps:end_wayfinder_map",
      "name": {
        "en_us": "End Wayfinder Map",
        "de_de": "End-Wegfinder-Karte"
      },
      "note": {
        "sources": [
          "modules/simplemaps/shared/java/com/simplemaps/MapsItems.java",
          "modules/simplemaps/shared/resources/data/simplemaps/recipe/end_wayfinder_map.json"
        ],
        "en": {
          "summary": "Endless map of the End. Shapeless: map, compass, 4 gold ingots, 2 echo shards, shulker shell."
        },
        "de": {
          "summary": "Endlose Karte des Ends. Formlos: Karte, Kompass, 4 Goldbarren, 2 Echo-Splitter, Shulker-Schale."
        }
      },
      "texture": "assets/textures/simplemaps/item/end_wayfinder_map.png",
      "craftedBy": [
        "simplemaps:end_wayfinder_map"
      ],
      "usedIn": []
    },
    {
      "id": "simplemaps:nether_wayfinder_map",
      "name": {
        "en_us": "Nether Wayfinder Map",
        "de_de": "Nether-Wegfinder-Karte"
      },
      "note": {
        "sources": [
          "modules/simplemaps/shared/java/com/simplemaps/MapsItems.java",
          "modules/simplemaps/shared/resources/data/simplemaps/recipe/nether_wayfinder_map.json"
        ],
        "en": {
          "summary": "Endless map of the Nether. Shapeless: map, compass, 4 gold ingots, 2 echo shards, netherite scrap."
        },
        "de": {
          "summary": "Endlose Karte des Nethers. Formlos: Karte, Kompass, 4 Goldbarren, 2 Echo-Splitter, Netheritplatten."
        }
      },
      "texture": "assets/textures/simplemaps/item/nether_wayfinder_map.png",
      "craftedBy": [
        "simplemaps:nether_wayfinder_map"
      ],
      "usedIn": []
    },
    {
      "id": "simplemaps:wayfinder_map",
      "name": {
        "en_us": "Wayfinder Map",
        "de_de": "Wegfinder-Karte"
      },
      "note": {
        "sources": [
          "modules/simplemaps/shared/java/com/simplemaps/MapsItems.java",
          "modules/simplemaps/shared/resources/data/simplemaps/recipe/wayfinder_map.json"
        ],
        "en": {
          "summary": "Endless map of the Overworld and every dimension that is neither Nether nor End. Shapeless: map, compass, 4 gold ingots, echo shard."
        },
        "de": {
          "summary": "Endlose Karte der Oberwelt und jeder Dimension, die weder Nether noch End ist. Formlos: Karte, Kompass, 4 Goldbarren, Echo-Splitter."
        }
      },
      "texture": "assets/textures/simplemaps/item/wayfinder_map.png",
      "craftedBy": [
        "simplemaps:wayfinder_map"
      ],
      "usedIn": []
    }
  ],
  "blocks": [],
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
    "recipes": 3,
    "lootTables": 0,
    "tags": 4,
    "advancements": 0,
    "enchantments": 0,
    "items": 3,
    "blocks": 0,
    "trades": 0,
    "config": 0,
    "quests": 0,
    "recipesOtherLines": 0,
    "undocumented": 0,
    "inWorld": 0,
    "incompleteProse": 0
  }
};
