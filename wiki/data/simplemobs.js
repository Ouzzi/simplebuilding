window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplemobs"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplemobs",
    "name": "Simple Mobs",
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
  "features": [],
  "recipes": [],
  "lootTables": [
    {
      "id": "simplemobs:entities/deceiver",
      "kind": "entities",
      "type": "minecraft:entity",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobs:deceiver_cloth"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/loot_table/entities/deceiver.json"
    },
    {
      "id": "simplemobs:entities/summoned",
      "kind": "entities",
      "type": "minecraft:entity",
      "pools": [],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/loot_table/entities/summoned.json"
    }
  ],
  "tags": [],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "simplemobs:deceiver_cloth",
      "name": {
        "en_us": "Deceiver Cloth",
        "de_de": "Täuscherstoff"
      },
      "note": {
        "en": {
          "summary": "Shimmering cloth dropped by the Deceiver. Material for the cloak of deception and invisibility without particles (planned)."
        },
        "de": {
          "summary": "Schimmernder Stoff, den der Deceiver fallen lässt. Material für den Tarnumhang und Unsichtbarkeit ohne Partikel (geplant)."
        }
      },
      "texture": "assets/textures/simplemobs/item/deceiver_cloth.png",
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:deceiver_spawn_egg",
      "name": {
        "en_us": "Deceiver Spawn Egg",
        "de_de": "Deceiver-Spawn-Ei"
      },
      "note": {
        "en": {
          "summary": "Spawns a Deceiver. Creative only."
        },
        "de": {
          "summary": "Beschwört einen Deceiver. Nur im Kreativmodus."
        }
      },
      "texture": "assets/textures/simplemobs/item/deceiver_spawn_egg.png",
      "craftedBy": [],
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
    "features": 0,
    "recipes": 0,
    "lootTables": 2,
    "tags": 0,
    "advancements": 0,
    "enchantments": 0,
    "items": 2,
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
