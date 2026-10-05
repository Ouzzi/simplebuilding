window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplemoney"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplemoney",
    "name": "Simple Money",
    "version": "1.2.16",
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
      "id": "special_paper",
      "en": {
        "title": "Special Paper",
        "summary": "Crafted from two paper and one honeycomb; used with an iron ingot and resin fiber to smith a banknote blank.",
        "details": [
          "Registry ID: simplemoney:special_paper",
          "Stack limit: 64",
          "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#M#\"], \"key\": {\"#\": \"minecraft:paper\", \"M\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:special_paper\", \"count\": 1}}"
        ]
      },
      "de": {
        "title": "Spezialpapier",
        "summary": "Aus zwei Papier und einer Honigwabe hergestellt; wird mit einem Eisenbarren und Harzfaser zu einem Banknotenrohling geschmiedet.",
        "details": [
          "Registry-ID: simplemoney:special_paper",
          "Stapelgrenze: 64",
          "Rezept: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#M#\"], \"key\": {\"#\": \"minecraft:paper\", \"M\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:special_paper\", \"count\": 1}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/recipe/special_paper_from_crafting_table.json",
        "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
      ],
      "related": []
    },
    {
      "id": "special_fiber",
      "en": {
        "title": "Special Fiber",
        "summary": "Crafted from copper nuggets, gold nuggets, amethyst shards, and diamonds; used with a gold ingot to refine a banknote blank.",
        "details": [
          "Registry ID: simplemoney:special_fiber",
          "Stack limit: 16",
          "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"CGA\", \"DAG\", \"GDC\"], \"key\": {\"D\": \"minecraft:diamond\", \"G\": \"minecraft:gold_nugget\", \"C\": \"minecraft:copper_nugget\", \"A\": \"minecraft:amethyst_shard\"}, \"result\": {\"id\": \"simplemoney:special_fiber\", \"count\": 1}}"
        ]
      },
      "de": {
        "title": "Spezialfaser",
        "summary": "Aus Kupfernuggets, Goldnuggets, Amethystscherben und Diamanten hergestellt; veredelt mit einem Goldbarren einen Banknotenrohling.",
        "details": [
          "Registry-ID: simplemoney:special_fiber",
          "Stapelgrenze: 16",
          "Rezept: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"CGA\", \"DAG\", \"GDC\"], \"key\": {\"D\": \"minecraft:diamond\", \"G\": \"minecraft:gold_nugget\", \"C\": \"minecraft:copper_nugget\", \"A\": \"minecraft:amethyst_shard\"}, \"result\": {\"id\": \"simplemoney:special_fiber\", \"count\": 1}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/recipe/special_fiber_from_crafting_table.json",
        "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
      ],
      "related": []
    },
    {
      "id": "resin_fiber",
      "en": {
        "title": "Resin Fiber",
        "summary": "Crafted from iron nuggets, resin clumps, honeycomb, and bone meal; used to smith banknote blanks.",
        "details": [
          "Registry ID: simplemoney:resin_fiber",
          "Stack limit: 16",
          "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"ERH\", \"RBR\", \"HRE\"], \"key\": {\"R\": \"minecraft:resin_clump\", \"E\": \"minecraft:iron_nugget\", \"B\": \"minecraft:bone_meal\", \"H\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:resin_fiber\", \"count\": 1}}"
        ]
      },
      "de": {
        "title": "Harzfaser",
        "summary": "Aus Eisennuggets, Harzklumpen, Honigwaben und Knochenmehl hergestellt; dient zum Schmieden von Banknotenrohlingen.",
        "details": [
          "Registry-ID: simplemoney:resin_fiber",
          "Stapelgrenze: 16",
          "Rezept: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"ERH\", \"RBR\", \"HRE\"], \"key\": {\"R\": \"minecraft:resin_clump\", \"E\": \"minecraft:iron_nugget\", \"B\": \"minecraft:bone_meal\", \"H\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:resin_fiber\", \"count\": 1}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/recipe/resin_fiber_from_crafting_table.json",
        "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
      ],
      "related": []
    },
    {
      "id": "blank_note",
      "en": {
        "title": "Banknote Blank",
        "summary": "An uncommon smithing ingredient made from special paper, resin fiber, and an iron ingot. Refine it with special fiber and a gold ingot.",
        "details": [
          "Registry ID: simplemoney:blank_note",
          "Stack limit: 64",
          "Recipe: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:iron_ingot\", \"base\": \"simplemoney:special_paper\", \"addition\": \"simplemoney:resin_fiber\", \"result\": {\"id\": \"simplemoney:blank_note\", \"count\": 1}}"
        ]
      },
      "de": {
        "title": "Banknotenrohling",
        "summary": "Eine ungew?hnliche Schmiedezutat aus Spezialpapier, Harzfaser und einem Eisenbarren. Mit Spezialfaser und einem Goldbarren veredeln.",
        "details": [
          "Registry-ID: simplemoney:blank_note",
          "Stapelgrenze: 64",
          "Rezept: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:iron_ingot\", \"base\": \"simplemoney:special_paper\", \"addition\": \"simplemoney:resin_fiber\", \"result\": {\"id\": \"simplemoney:blank_note\", \"count\": 1}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/recipe/blank_note_smithing.json",
        "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
      ],
      "related": []
    },
    {
      "id": "refined_blank_note",
      "en": {
        "title": "Refined Banknote Blank",
        "summary": "An uncommon ingredient made by refining a banknote blank. Three blanks, green dye, and ink sacs produce three raw bills.",
        "details": [
          "Registry ID: simplemoney:refined_blank_note",
          "Stack limit: 64",
          "Recipe: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:gold_ingot\", \"base\": \"simplemoney:blank_note\", \"addition\": \"simplemoney:special_fiber\", \"result\": {\"id\": \"simplemoney:refined_blank_note\", \"count\": 1}}"
        ]
      },
      "de": {
        "title": "Veredelter Banknotenrohling",
        "summary": "Eine ungew?hnliche Zutat aus einem veredelten Banknotenrohling. Drei Rohlinge, gr?ner Farbstoff und Tintenbeutel ergeben drei rohe Geldscheine.",
        "details": [
          "Registry-ID: simplemoney:refined_blank_note",
          "Stapelgrenze: 64",
          "Rezept: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:gold_ingot\", \"base\": \"simplemoney:blank_note\", \"addition\": \"simplemoney:special_fiber\", \"result\": {\"id\": \"simplemoney:refined_blank_note\", \"count\": 1}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/recipe/refined_bank_note_blank_smithing.json",
        "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
      ],
      "related": []
    },
    {
      "id": "raw_bill",
      "en": {
        "title": "Raw Bill",
        "summary": "A rare crafting result. Blast it for 24,000 ticks (one in-game day in a vanilla blast furnace; SimpleBuilding blast furnaces are 2, 4 or 8 times faster) to produce one money bill and 20 experience points.",
        "details": [
          "Registry ID: simplemoney:raw_bill",
          "Stack limit: 64",
          "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#I#\", \"PPP\", \"I#I\"], \"key\": {\"#\": \"minecraft:green_dye\", \"I\": \"minecraft:ink_sac\", \"P\": \"simplemoney:refined_blank_note\"}, \"result\": {\"id\": \"simplemoney:raw_bill\", \"count\": 3}}"
        ]
      },
      "de": {
        "title": "Roher Geldschein",
        "summary": "Ein seltenes Herstellungsergebnis. Im Schmelzofen in 24.000 Ticks (ein Spieltag im Vanilla-Schmelzofen; SimpleBuildings Schmelzöfen sind 2-, 4- oder 8-mal so schnell) zu einem Geldschein verarbeiten; ergibt 20 Erfahrungspunkte.",
        "details": [
          "Registry-ID: simplemoney:raw_bill",
          "Stapelgrenze: 64",
          "Rezept: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#I#\", \"PPP\", \"I#I\"], \"key\": {\"#\": \"minecraft:green_dye\", \"I\": \"minecraft:ink_sac\", \"P\": \"simplemoney:refined_blank_note\"}, \"result\": {\"id\": \"simplemoney:raw_bill\", \"count\": 3}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/recipe/raw_bill_from_crafting_table.json",
        "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
      ],
      "related": []
    },
    {
      "id": "money_bill",
      "en": {
        "title": "Money Bill",
        "summary": "Currency for the added villager and wandering trader offers. Epic, fire resistant, and always glinting; using it plays a page-turn sound and emits a happy-villager particle without consuming it.",
        "details": [
          "Registry ID: simplemoney:money_bill",
          "Stack limit: 64",
          "Recipe: {\"type\": \"minecraft:blasting\", \"ingredient\": \"simplemoney:raw_bill\", \"result\": {\"id\": \"simplemoney:money_bill\"}, \"experience\": 20, \"cookingtime\": 24000}",
          "Epic rarity, fire resistant, permanent enchantment glint. Right-click plays a page-turn sound and sends a happy-villager particle; the bill is not consumed."
        ]
      },
      "de": {
        "title": "Geldschein",
        "summary": "W?hrung f?r die zus?tzlichen Dorfbewohner- und fahrenden H?ndlerangebote. Episch, feuerfest und dauerhaft gl?nzend; Benutzung erzeugt einen Umbl?tterklang und ein Dorfbewohnerpartikel ohne Verbrauch.",
        "details": [
          "Registry-ID: simplemoney:money_bill",
          "Stapelgrenze: 64",
          "Rezept: {\"type\": \"minecraft:blasting\", \"ingredient\": \"simplemoney:raw_bill\", \"result\": {\"id\": \"simplemoney:money_bill\"}, \"experience\": 20, \"cookingtime\": 24000}",
          "Episch, feuerfest, dauerhafter Verzauberungsglanz. Rechtsklick erzeugt einen Umblätterklang und ein Dorfbewohnerpartikel; der Schein bleibt erhalten."
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/recipe/money_bill_from_blasting.json",
        "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
      ],
      "related": []
    },
    {
      "id": "trade_armorer_25",
      "en": {
        "title": "Trade: armorer/25",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: armorer/level_2",
          "{\"wants\": {\"id\": \"minecraft:diamond\", \"count\": 15}, \"max_uses\": 2, \"xp\": 10, \"reputation_discount\": 0.2, \"gives\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}}"
        ]
      },
      "de": {
        "title": "Handel: armorer/25",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: armorer/level_2",
          "{\"wants\": {\"id\": \"minecraft:diamond\", \"count\": 15}, \"max_uses\": 2, \"xp\": 10, \"reputation_discount\": 0.2, \"gives\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/armorer/25.json"
      ],
      "related": []
    },
    {
      "id": "trade_armorer_26",
      "en": {
        "title": "Trade: armorer/26",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: armorer/level_3",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 2, \"xp\": 15, \"reputation_discount\": 0.2, \"gives\": {\"id\": \"minecraft:diamond_helmet\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:protection\", \"level\": 1, \"weight\": 40}, {\"enchantment\": \"minecraft:thorns\", \"level\": 1, \"weight\": 20}, {\"enchantment\": \"minecraft:aqua_affinity\", \"level\": 1, \"weight\": 20}, {\"enchantment\": \"minecraft:respiration\", \"level\": 2, \"weight\": 20}], \"second_chance\": 0.05, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: armorer/26",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: armorer/level_3",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 2, \"xp\": 15, \"reputation_discount\": 0.2, \"gives\": {\"id\": \"minecraft:diamond_helmet\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:protection\", \"level\": 1, \"weight\": 40}, {\"enchantment\": \"minecraft:thorns\", \"level\": 1, \"weight\": 20}, {\"enchantment\": \"minecraft:aqua_affinity\", \"level\": 1, \"weight\": 20}, {\"enchantment\": \"minecraft:respiration\", \"level\": 2, \"weight\": 20}], \"second_chance\": 0.05, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/armorer/26.json"
      ],
      "related": []
    },
    {
      "id": "trade_armorer_27",
      "en": {
        "title": "Trade: armorer/27",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: armorer/level_3",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 2, \"xp\": 15, \"reputation_discount\": 0.2, \"gives\": {\"id\": \"minecraft:diamond_boots\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:feather_falling\", \"level\": 1, \"weight\": 30}, {\"enchantment\": \"minecraft:depth_strider\", \"level\": 1, \"weight\": 20}, {\"enchantment\": \"minecraft:protection\", \"level\": 1, \"weight\": 30}, {\"enchantment\": \"minecraft:frost_walker\", \"level\": 1, \"weight\": 20}], \"second_chance\": 0.05, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: armorer/27",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: armorer/level_3",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 2, \"xp\": 15, \"reputation_discount\": 0.2, \"gives\": {\"id\": \"minecraft:diamond_boots\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:feather_falling\", \"level\": 1, \"weight\": 30}, {\"enchantment\": \"minecraft:depth_strider\", \"level\": 1, \"weight\": 20}, {\"enchantment\": \"minecraft:protection\", \"level\": 1, \"weight\": 30}, {\"enchantment\": \"minecraft:frost_walker\", \"level\": 1, \"weight\": 20}], \"second_chance\": 0.05, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/armorer/27.json"
      ],
      "related": []
    },
    {
      "id": "trade_armorer_28",
      "en": {
        "title": "Trade: armorer/28",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: armorer/level_4",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 5}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_chestplate\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:protection\", \"level\": 3, \"weight\": 40}, {\"enchantment\": \"minecraft:protection\", \"level\": 4, \"weight\": 40}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 2, \"weight\": 20}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 3, \"weight\": 20}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: armorer/28",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: armorer/level_4",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 5}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_chestplate\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:protection\", \"level\": 3, \"weight\": 40}, {\"enchantment\": \"minecraft:protection\", \"level\": 4, \"weight\": 40}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 2, \"weight\": 20}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 3, \"weight\": 20}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/armorer/28.json"
      ],
      "related": []
    },
    {
      "id": "trade_armorer_29",
      "en": {
        "title": "Trade: armorer/29",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: armorer/level_4",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 4}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_leggings\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:protection\", \"level\": 3, \"weight\": 40}, {\"enchantment\": \"minecraft:protection\", \"level\": 4, \"weight\": 40}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:swift_sneak\", \"level\": 1, \"weight\": 5}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: armorer/29",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: armorer/level_4",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 4}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_leggings\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:protection\", \"level\": 3, \"weight\": 40}, {\"enchantment\": \"minecraft:protection\", \"level\": 4, \"weight\": 40}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:swift_sneak\", \"level\": 1, \"weight\": 5}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/armorer/29.json"
      ],
      "related": []
    },
    {
      "id": "trade_armorer_30",
      "en": {
        "title": "Trade: armorer/30",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: armorer/level_5",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 5}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_chestplate\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:protection\", \"level\": 4, \"weight\": 50}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:thorns\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 3, \"weight\": 20}], \"second_chance\": 0.3, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: armorer/30",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: armorer/level_5",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 5}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_chestplate\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:protection\", \"level\": 4, \"weight\": 50}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:thorns\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 3, \"weight\": 20}], \"second_chance\": 0.3, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/armorer/30.json"
      ],
      "related": []
    },
    {
      "id": "trade_armorer_31",
      "en": {
        "title": "Trade: armorer/31",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: armorer/level_5",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 2, \"xp\": 15, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_boots\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:feather_falling\", \"level\": 4, \"weight\": 40}, {\"enchantment\": \"minecraft:depth_strider\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:soul_speed\", \"level\": 1, \"weight\": 10}], \"second_chance\": 0.3, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: armorer/31",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: armorer/level_5",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 2, \"xp\": 15, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_boots\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:feather_falling\", \"level\": 4, \"weight\": 40}, {\"enchantment\": \"minecraft:depth_strider\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:soul_speed\", \"level\": 1, \"weight\": 10}], \"second_chance\": 0.3, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/armorer/31.json"
      ],
      "related": []
    },
    {
      "id": "trade_armorer_32",
      "en": {
        "title": "Trade: armorer/32",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: armorer/level_5",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 5}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_helmet\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:protection\", \"level\": 4, \"weight\": 50}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:aqua_affinity\", \"level\": 1, \"weight\": 20}, {\"enchantment\": \"minecraft:respiration\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:thorns\", \"level\": 3, \"weight\": 30}], \"second_chance\": 0.3, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: armorer/32",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: armorer/level_5",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 5}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_helmet\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:protection\", \"level\": 4, \"weight\": 50}, {\"enchantment\": \"minecraft:projectile_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:fire_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:blast_protection\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:aqua_affinity\", \"level\": 1, \"weight\": 20}, {\"enchantment\": \"minecraft:respiration\", \"level\": 3, \"weight\": 30}, {\"enchantment\": \"minecraft:thorns\", \"level\": 3, \"weight\": 30}], \"second_chance\": 0.3, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/armorer/32.json"
      ],
      "related": []
    },
    {
      "id": "trade_cleric_08",
      "en": {
        "title": "Trade: cleric/08",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: cleric/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"gives\": {\"id\": \"minecraft:emerald\", \"count\": 3}, \"max_uses\": 10, \"xp\": 10, \"reputation_discount\": 0.05, \"given_item_modifier\": [{\"count\": {\"type\": \"minecraft:uniform\", \"min\": 3, \"max\": 35}, \"type\": \"minecraft:set_count\"}]}"
        ]
      },
      "de": {
        "title": "Handel: cleric/08",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: cleric/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"gives\": {\"id\": \"minecraft:emerald\", \"count\": 3}, \"max_uses\": 10, \"xp\": 10, \"reputation_discount\": 0.05, \"given_item_modifier\": [{\"count\": {\"type\": \"minecraft:uniform\", \"min\": 3, \"max\": 35}, \"type\": \"minecraft:set_count\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/cleric/08.json"
      ],
      "related": []
    },
    {
      "id": "trade_cleric_09",
      "en": {
        "title": "Trade: cleric/09",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: cleric/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 4, \"xp\": 10, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:ender_pearl\", \"count\": 8}}"
        ]
      },
      "de": {
        "title": "Handel: cleric/09",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: cleric/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 4, \"xp\": 10, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:ender_pearl\", \"count\": 8}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/cleric/09.json"
      ],
      "related": []
    },
    {
      "id": "trade_farmer_18",
      "en": {
        "title": "Trade: farmer/18",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: farmer/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"gives\": {\"id\": \"minecraft:emerald\", \"count\": 3}, \"max_uses\": 10, \"xp\": 10, \"reputation_discount\": 0.05, \"given_item_modifier\": [{\"count\": {\"type\": \"minecraft:uniform\", \"min\": 3, \"max\": 35}, \"type\": \"minecraft:set_count\"}]}"
        ]
      },
      "de": {
        "title": "Handel: farmer/18",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: farmer/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"gives\": {\"id\": \"minecraft:emerald\", \"count\": 3}, \"max_uses\": 10, \"xp\": 10, \"reputation_discount\": 0.05, \"given_item_modifier\": [{\"count\": {\"type\": \"minecraft:uniform\", \"min\": 3, \"max\": 35}, \"type\": \"minecraft:set_count\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/farmer/18.json"
      ],
      "related": []
    },
    {
      "id": "trade_farmer_19",
      "en": {
        "title": "Trade: farmer/19",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: farmer/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 16, \"xp\": 5, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:wheat_seeds\", \"count\": 52}}"
        ]
      },
      "de": {
        "title": "Handel: farmer/19",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: farmer/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 16, \"xp\": 5, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:wheat_seeds\", \"count\": 52}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/farmer/19.json"
      ],
      "related": []
    },
    {
      "id": "trade_farmer_20",
      "en": {
        "title": "Trade: farmer/20",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: farmer/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 6, \"xp\": 5, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:carrot\", \"count\": 20}}"
        ]
      },
      "de": {
        "title": "Handel: farmer/20",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: farmer/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 6, \"xp\": 5, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:carrot\", \"count\": 20}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/farmer/20.json"
      ],
      "related": []
    },
    {
      "id": "trade_farmer_21",
      "en": {
        "title": "Trade: farmer/21",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: farmer/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 6, \"xp\": 5, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:potato\", \"count\": 24}}"
        ]
      },
      "de": {
        "title": "Handel: farmer/21",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: farmer/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 6, \"xp\": 5, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:potato\", \"count\": 24}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/farmer/21.json"
      ],
      "related": []
    },
    {
      "id": "trade_farmer_22",
      "en": {
        "title": "Trade: farmer/22",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: farmer/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}, \"max_uses\": 4, \"xp\": 10, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:apple\", \"count\": 28}}"
        ]
      },
      "de": {
        "title": "Handel: farmer/22",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: farmer/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}, \"max_uses\": 4, \"xp\": 10, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:apple\", \"count\": 28}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/farmer/22.json"
      ],
      "related": []
    },
    {
      "id": "trade_farmer_23",
      "en": {
        "title": "Trade: farmer/23",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: farmer/level_2",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}, \"max_uses\": 2, \"xp\": 15, \"reputation_discount\": 0.1, \"gives\": {\"id\": \"minecraft:cake\", \"count\": 1}}"
        ]
      },
      "de": {
        "title": "Handel: farmer/23",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: farmer/level_2",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}, \"max_uses\": 2, \"xp\": 15, \"reputation_discount\": 0.1, \"gives\": {\"id\": \"minecraft:cake\", \"count\": 1}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/farmer/23.json"
      ],
      "related": []
    },
    {
      "id": "trade_farmer_24",
      "en": {
        "title": "Trade: farmer/24",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: farmer/level_2",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}, \"max_uses\": 4, \"xp\": 20, \"reputation_discount\": 0.1, \"gives\": {\"id\": \"minecraft:golden_carrot\", \"count\": 16}}"
        ]
      },
      "de": {
        "title": "Handel: farmer/24",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: farmer/level_2",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}, \"max_uses\": 4, \"xp\": 20, \"reputation_discount\": 0.1, \"gives\": {\"id\": \"minecraft:golden_carrot\", \"count\": 16}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/farmer/24.json"
      ],
      "related": []
    },
    {
      "id": "trade_fletcher_00",
      "en": {
        "title": "Trade: fletcher/00",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: fletcher/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"gives\": {\"id\": \"minecraft:emerald\", \"count\": 3}, \"max_uses\": 10, \"xp\": 10, \"reputation_discount\": 0.05, \"given_item_modifier\": [{\"count\": {\"type\": \"minecraft:uniform\", \"min\": 3, \"max\": 35}, \"type\": \"minecraft:set_count\"}]}"
        ]
      },
      "de": {
        "title": "Handel: fletcher/00",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: fletcher/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"gives\": {\"id\": \"minecraft:emerald\", \"count\": 3}, \"max_uses\": 10, \"xp\": 10, \"reputation_discount\": 0.05, \"given_item_modifier\": [{\"count\": {\"type\": \"minecraft:uniform\", \"min\": 3, \"max\": 35}, \"type\": \"minecraft:set_count\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/fletcher/00.json"
      ],
      "related": []
    },
    {
      "id": "trade_fletcher_01",
      "en": {
        "title": "Trade: fletcher/01",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: fletcher/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 4, \"xp\": 5, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:firework_rocket\", \"count\": 10}}"
        ]
      },
      "de": {
        "title": "Handel: fletcher/01",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: fletcher/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 4, \"xp\": 5, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:firework_rocket\", \"count\": 10}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/fletcher/01.json"
      ],
      "related": []
    },
    {
      "id": "trade_fletcher_02",
      "en": {
        "title": "Trade: fletcher/02",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: fletcher/level_3",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}, \"max_uses\": 6, \"xp\": 10, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:firework_rocket\", \"count\": 16}}"
        ]
      },
      "de": {
        "title": "Handel: fletcher/02",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: fletcher/level_3",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}, \"max_uses\": 6, \"xp\": 10, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:firework_rocket\", \"count\": 16}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/fletcher/02.json"
      ],
      "related": []
    },
    {
      "id": "trade_librarian_03",
      "en": {
        "title": "Trade: librarian/03",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: librarian/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 2, \"xp\": 10, \"reputation_discount\": 0.5, \"gives\": {\"id\": \"minecraft:enchanted_book\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:efficiency\", \"level\": 5, \"weight\": 20}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 3, \"weight\": 20}, {\"enchantment\": \"minecraft:fire_aspect\", \"level\": 2, \"weight\": 10}, {\"enchantment\": \"minecraft:fortune\", \"level\": 3, \"weight\": 10}, {\"enchantment\": \"minecraft:knockback\", \"level\": 2, \"weight\": 5}, {\"enchantment\": \"minecraft:feather_falling\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:mending\", \"level\": 1, \"weight\": 2}, {\"enchantment\": \"minecraft:silk_touch\", \"level\": 1, \"weight\": 10}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: librarian/03",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: librarian/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 2, \"xp\": 10, \"reputation_discount\": 0.5, \"gives\": {\"id\": \"minecraft:enchanted_book\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:efficiency\", \"level\": 5, \"weight\": 20}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 3, \"weight\": 20}, {\"enchantment\": \"minecraft:fire_aspect\", \"level\": 2, \"weight\": 10}, {\"enchantment\": \"minecraft:fortune\", \"level\": 3, \"weight\": 10}, {\"enchantment\": \"minecraft:knockback\", \"level\": 2, \"weight\": 5}, {\"enchantment\": \"minecraft:feather_falling\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:mending\", \"level\": 1, \"weight\": 2}, {\"enchantment\": \"minecraft:silk_touch\", \"level\": 1, \"weight\": 10}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/librarian/03.json"
      ],
      "related": []
    },
    {
      "id": "trade_librarian_04",
      "en": {
        "title": "Trade: librarian/04",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: librarian/level_2",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 2, \"xp\": 10, \"reputation_discount\": 0.5, \"gives\": {\"id\": \"minecraft:enchanted_book\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:sharpness\", \"level\": 5, \"weight\": 20}, {\"enchantment\": \"minecraft:protection\", \"level\": 4, \"weight\": 20}, {\"enchantment\": \"minecraft:feather_falling\", \"level\": 4, \"weight\": 10}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: librarian/04",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: librarian/level_2",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 2, \"xp\": 10, \"reputation_discount\": 0.5, \"gives\": {\"id\": \"minecraft:enchanted_book\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:sharpness\", \"level\": 5, \"weight\": 20}, {\"enchantment\": \"minecraft:protection\", \"level\": 4, \"weight\": 20}, {\"enchantment\": \"minecraft:feather_falling\", \"level\": 4, \"weight\": 10}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/librarian/04.json"
      ],
      "related": []
    },
    {
      "id": "trade_librarian_05",
      "en": {
        "title": "Trade: librarian/05",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: librarian/level_3",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 4}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:enchanted_book\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:respiration\", \"level\": 3, \"weight\": 10}, {\"enchantment\": \"minecraft:impaling\", \"level\": 5, \"weight\": 10}, {\"enchantment\": \"minecraft:power\", \"level\": 5, \"weight\": 15}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: librarian/05",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: librarian/level_3",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 4}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:enchanted_book\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:respiration\", \"level\": 3, \"weight\": 10}, {\"enchantment\": \"minecraft:impaling\", \"level\": 5, \"weight\": 10}, {\"enchantment\": \"minecraft:power\", \"level\": 5, \"weight\": 15}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/librarian/05.json"
      ],
      "related": []
    },
    {
      "id": "trade_librarian_06",
      "en": {
        "title": "Trade: librarian/06",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: librarian/level_4",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 4}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:enchanted_book\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:depth_strider\", \"level\": 3, \"weight\": 10}, {\"enchantment\": \"minecraft:frost_walker\", \"level\": 2, \"weight\": 10}, {\"enchantment\": \"minecraft:looting\", \"level\": 3, \"weight\": 15}, {\"enchantment\": \"minecraft:piercing\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:power\", \"level\": 5, \"weight\": 15}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: librarian/06",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: librarian/level_4",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 4}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:enchanted_book\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:depth_strider\", \"level\": 3, \"weight\": 10}, {\"enchantment\": \"minecraft:frost_walker\", \"level\": 2, \"weight\": 10}, {\"enchantment\": \"minecraft:looting\", \"level\": 3, \"weight\": 15}, {\"enchantment\": \"minecraft:piercing\", \"level\": 4, \"weight\": 10}, {\"enchantment\": \"minecraft:power\", \"level\": 5, \"weight\": 15}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/librarian/06.json"
      ],
      "related": []
    },
    {
      "id": "trade_librarian_07",
      "en": {
        "title": "Trade: librarian/07",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: librarian/level_5",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 5}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:enchanted_book\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:swift_sneak\", \"level\": 1, \"weight\": 2}, {\"enchantment\": \"minecraft:soul_speed\", \"level\": 1, \"weight\": 2}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: librarian/07",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: librarian/level_5",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 5}, \"max_uses\": 1, \"xp\": 50, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:enchanted_book\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:swift_sneak\", \"level\": 1, \"weight\": 2}, {\"enchantment\": \"minecraft:soul_speed\", \"level\": 1, \"weight\": 2}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/librarian/07.json"
      ],
      "related": []
    },
    {
      "id": "trade_mason_10",
      "en": {
        "title": "Trade: mason/10",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: mason/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"gives\": {\"id\": \"minecraft:emerald\", \"count\": 3}, \"max_uses\": 10, \"xp\": 10, \"reputation_discount\": 0.05, \"given_item_modifier\": [{\"count\": {\"type\": \"minecraft:uniform\", \"min\": 3, \"max\": 35}, \"type\": \"minecraft:set_count\"}]}"
        ]
      },
      "de": {
        "title": "Handel: mason/10",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: mason/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"gives\": {\"id\": \"minecraft:emerald\", \"count\": 3}, \"max_uses\": 10, \"xp\": 10, \"reputation_discount\": 0.05, \"given_item_modifier\": [{\"count\": {\"type\": \"minecraft:uniform\", \"min\": 3, \"max\": 35}, \"type\": \"minecraft:set_count\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/mason/10.json"
      ],
      "related": []
    },
    {
      "id": "trade_mason_11",
      "en": {
        "title": "Trade: mason/11",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: mason/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 4, \"xp\": 3, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:smooth_stone\", \"count\": 64}}"
        ]
      },
      "de": {
        "title": "Handel: mason/11",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: mason/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 4, \"xp\": 3, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:smooth_stone\", \"count\": 64}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/mason/11.json"
      ],
      "related": []
    },
    {
      "id": "trade_mason_12",
      "en": {
        "title": "Trade: mason/12",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: mason/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 4, \"xp\": 3, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:stone_bricks\", \"count\": 64}}"
        ]
      },
      "de": {
        "title": "Handel: mason/12",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: mason/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 4, \"xp\": 3, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:stone_bricks\", \"count\": 64}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/mason/12.json"
      ],
      "related": []
    },
    {
      "id": "trade_mason_13",
      "en": {
        "title": "Trade: mason/13",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: mason/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 4, \"xp\": 3, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:deepslate_bricks\", \"count\": 52}}"
        ]
      },
      "de": {
        "title": "Handel: mason/13",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: mason/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 4, \"xp\": 3, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:deepslate_bricks\", \"count\": 52}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/mason/13.json"
      ],
      "related": []
    },
    {
      "id": "trade_mason_14",
      "en": {
        "title": "Trade: mason/14",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: mason/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 4, \"xp\": 3, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:mossy_cobblestone\", \"count\": 32}}"
        ]
      },
      "de": {
        "title": "Handel: mason/14",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: mason/level_1",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 4, \"xp\": 3, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:mossy_cobblestone\", \"count\": 32}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/mason/14.json"
      ],
      "related": []
    },
    {
      "id": "trade_mason_15",
      "en": {
        "title": "Trade: mason/15",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: mason/level_2",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 6, \"xp\": 5, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:smooth_stone\", \"count\": 64}}"
        ]
      },
      "de": {
        "title": "Handel: mason/15",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: mason/level_2",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 6, \"xp\": 5, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:smooth_stone\", \"count\": 64}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/mason/15.json"
      ],
      "related": []
    },
    {
      "id": "trade_mason_16",
      "en": {
        "title": "Trade: mason/16",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: mason/level_2",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 6, \"xp\": 6, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:stone_bricks\", \"count\": 64}}"
        ]
      },
      "de": {
        "title": "Handel: mason/16",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: mason/level_2",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 6, \"xp\": 6, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:stone_bricks\", \"count\": 64}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/mason/16.json"
      ],
      "related": []
    },
    {
      "id": "trade_mason_17",
      "en": {
        "title": "Trade: mason/17",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: mason/level_2",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 3, \"xp\": 6, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:prismarine_bricks\", \"count\": 16}}"
        ]
      },
      "de": {
        "title": "Handel: mason/17",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: mason/level_2",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 1}, \"max_uses\": 3, \"xp\": 6, \"reputation_discount\": 0.05, \"gives\": {\"id\": \"minecraft:prismarine_bricks\", \"count\": 16}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/mason/17.json"
      ],
      "related": []
    },
    {
      "id": "trade_toolsmith_33",
      "en": {
        "title": "Trade: toolsmith/33",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: toolsmith/level_3",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}, \"max_uses\": 2, \"xp\": 15, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_hoe\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:efficiency\", \"level\": 1, \"weight\": 50}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 1, \"weight\": 30}, {\"enchantment\": \"minecraft:efficiency\", \"level\": 2, \"weight\": 10}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: toolsmith/33",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: toolsmith/level_3",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}, \"max_uses\": 2, \"xp\": 15, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_hoe\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:efficiency\", \"level\": 1, \"weight\": 50}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 1, \"weight\": 30}, {\"enchantment\": \"minecraft:efficiency\", \"level\": 2, \"weight\": 10}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/toolsmith/33.json"
      ],
      "related": []
    },
    {
      "id": "trade_toolsmith_34",
      "en": {
        "title": "Trade: toolsmith/34",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: toolsmith/level_3",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}, \"max_uses\": 2, \"xp\": 10, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_shovel\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:efficiency\", \"level\": 1, \"weight\": 50}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 1, \"weight\": 30}, {\"enchantment\": \"minecraft:efficiency\", \"level\": 2, \"weight\": 10}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: toolsmith/34",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: toolsmith/level_3",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}, \"max_uses\": 2, \"xp\": 10, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_shovel\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:efficiency\", \"level\": 1, \"weight\": 50}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 1, \"weight\": 30}, {\"enchantment\": \"minecraft:efficiency\", \"level\": 2, \"weight\": 10}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/toolsmith/34.json"
      ],
      "related": []
    },
    {
      "id": "trade_toolsmith_35",
      "en": {
        "title": "Trade: toolsmith/35",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: toolsmith/level_4",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 1, \"xp\": 15, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_pickaxe\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:efficiency\", \"level\": 2, \"weight\": 40}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 2, \"weight\": 30}, {\"enchantment\": \"minecraft:fortune\", \"level\": 1, \"weight\": 15}, {\"enchantment\": \"minecraft:silk_touch\", \"level\": 1, \"weight\": 10}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: toolsmith/35",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: toolsmith/level_4",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 1, \"xp\": 15, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_pickaxe\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:efficiency\", \"level\": 2, \"weight\": 40}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 2, \"weight\": 30}, {\"enchantment\": \"minecraft:fortune\", \"level\": 1, \"weight\": 15}, {\"enchantment\": \"minecraft:silk_touch\", \"level\": 1, \"weight\": 10}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/toolsmith/35.json"
      ],
      "related": []
    },
    {
      "id": "trade_toolsmith_36",
      "en": {
        "title": "Trade: toolsmith/36",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: toolsmith/level_4",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 1, \"xp\": 15, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_axe\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:efficiency\", \"level\": 2, \"weight\": 40}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 2, \"weight\": 30}, {\"enchantment\": \"minecraft:fortune\", \"level\": 1, \"weight\": 15}, {\"enchantment\": \"minecraft:silk_touch\", \"level\": 1, \"weight\": 10}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: toolsmith/36",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: toolsmith/level_4",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 3}, \"max_uses\": 1, \"xp\": 15, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_axe\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:efficiency\", \"level\": 2, \"weight\": 40}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 2, \"weight\": 30}, {\"enchantment\": \"minecraft:fortune\", \"level\": 1, \"weight\": 15}, {\"enchantment\": \"minecraft:silk_touch\", \"level\": 1, \"weight\": 10}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/toolsmith/36.json"
      ],
      "related": []
    },
    {
      "id": "trade_toolsmith_37",
      "en": {
        "title": "Trade: toolsmith/37",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: toolsmith/level_5",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 4}, \"max_uses\": 1, \"xp\": 30, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_pickaxe\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:efficiency\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:efficiency\", \"level\": 5, \"weight\": 10}, {\"enchantment\": \"minecraft:fortune\", \"level\": 2, \"weight\": 20}, {\"enchantment\": \"minecraft:fortune\", \"level\": 3, \"weight\": 10}, {\"enchantment\": \"minecraft:mending\", \"level\": 1, \"weight\": 5}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 3, \"weight\": 20}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: toolsmith/37",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: toolsmith/level_5",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 4}, \"max_uses\": 1, \"xp\": 30, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_pickaxe\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:efficiency\", \"level\": 4, \"weight\": 30}, {\"enchantment\": \"minecraft:efficiency\", \"level\": 5, \"weight\": 10}, {\"enchantment\": \"minecraft:fortune\", \"level\": 2, \"weight\": 20}, {\"enchantment\": \"minecraft:fortune\", \"level\": 3, \"weight\": 10}, {\"enchantment\": \"minecraft:mending\", \"level\": 1, \"weight\": 5}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 3, \"weight\": 20}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/toolsmith/37.json"
      ],
      "related": []
    },
    {
      "id": "trade_toolsmith_38",
      "en": {
        "title": "Trade: toolsmith/38",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: toolsmith/level_5",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 4}, \"max_uses\": 1, \"xp\": 30, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_sword\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:sharpness\", \"level\": 5, \"weight\": 30}, {\"enchantment\": \"minecraft:smite\", \"level\": 5, \"weight\": 20}, {\"enchantment\": \"minecraft:bane_of_arthropods\", \"level\": 5, \"weight\": 20}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 3, \"weight\": 20}, {\"enchantment\": \"minecraft:sweeping_edge\", \"level\": 3, \"weight\": 10}, {\"enchantment\": \"minecraft:mending\", \"level\": 1, \"weight\": 5}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "de": {
        "title": "Handel: toolsmith/38",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: toolsmith/level_5",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": 4}, \"max_uses\": 1, \"xp\": 30, \"reputation_discount\": 0.7, \"gives\": {\"id\": \"minecraft:diamond_sword\"}, \"given_item_modifier\": [{\"pool\": [{\"enchantment\": \"minecraft:sharpness\", \"level\": 5, \"weight\": 30}, {\"enchantment\": \"minecraft:smite\", \"level\": 5, \"weight\": 20}, {\"enchantment\": \"minecraft:bane_of_arthropods\", \"level\": 5, \"weight\": 20}, {\"enchantment\": \"minecraft:unbreaking\", \"level\": 3, \"weight\": 20}, {\"enchantment\": \"minecraft:sweeping_edge\", \"level\": 3, \"weight\": 10}, {\"enchantment\": \"minecraft:mending\", \"level\": 1, \"weight\": 5}], \"second_chance\": 0.1, \"type\": \"simplemoney:weighted_enchant\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/toolsmith/38.json"
      ],
      "related": []
    },
    {
      "id": "trade_toolsmith_39",
      "en": {
        "title": "Trade: toolsmith/39",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: toolsmith/level_5",
          "{\"wants\": {\"id\": \"minecraft:diamond\", \"count\": 7}, \"max_uses\": 5, \"xp\": 10, \"reputation_discount\": 0.1, \"gives\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}}"
        ]
      },
      "de": {
        "title": "Handel: toolsmith/39",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: toolsmith/level_5",
          "{\"wants\": {\"id\": \"minecraft:diamond\", \"count\": 7}, \"max_uses\": 5, \"xp\": 10, \"reputation_discount\": 0.1, \"gives\": {\"id\": \"simplemoney:money_bill\", \"count\": 2}}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/toolsmith/39.json"
      ],
      "related": []
    },
    {
      "id": "trade_wandering_trader_40",
      "en": {
        "title": "Trade: wandering_trader/40",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: wandering_trader/common",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 6, \"max\": 9}}, \"gives\": {\"id\": \"minecraft:chorus_fruit\", \"count\": 16}, \"max_uses\": 3, \"xp\": 10, \"reputation_discount\": 0.1, \"given_item_modifier\": [{\"count\": {\"type\": \"minecraft:uniform\", \"min\": 16, \"max\": 23}, \"type\": \"minecraft:set_count\"}]}"
        ]
      },
      "de": {
        "title": "Handel: wandering_trader/40",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: wandering_trader/common",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 6, \"max\": 9}}, \"gives\": {\"id\": \"minecraft:chorus_fruit\", \"count\": 16}, \"max_uses\": 3, \"xp\": 10, \"reputation_discount\": 0.1, \"given_item_modifier\": [{\"count\": {\"type\": \"minecraft:uniform\", \"min\": 16, \"max\": 23}, \"type\": \"minecraft:set_count\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/wandering_trader/40.json"
      ],
      "related": []
    },
    {
      "id": "trade_wandering_trader_41",
      "en": {
        "title": "Trade: wandering_trader/41",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: wandering_trader/common",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 2, \"max\": 3}}, \"gives\": {\"id\": \"minecraft:apple\", \"count\": 30}, \"max_uses\": 3, \"xp\": 10, \"reputation_discount\": 0.1, \"given_item_modifier\": [{\"count\": {\"type\": \"minecraft:uniform\", \"min\": 30, \"max\": 49}, \"type\": \"minecraft:set_count\"}]}"
        ]
      },
      "de": {
        "title": "Handel: wandering_trader/41",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: wandering_trader/common",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 2, \"max\": 3}}, \"gives\": {\"id\": \"minecraft:apple\", \"count\": 30}, \"max_uses\": 3, \"xp\": 10, \"reputation_discount\": 0.1, \"given_item_modifier\": [{\"count\": {\"type\": \"minecraft:uniform\", \"min\": 30, \"max\": 49}, \"type\": \"minecraft:set_count\"}]}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/wandering_trader/41.json"
      ],
      "related": []
    },
    {
      "id": "trade_wandering_trader_42",
      "en": {
        "title": "Trade: wandering_trader/42",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: wandering_trader/uncommon",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 30, \"max\": 44}}, \"gives\": {\"id\": \"minecraft:netherite_scrap\", \"count\": 7}, \"max_uses\": 1, \"xp\": 100, \"reputation_discount\": 0.5}"
        ]
      },
      "de": {
        "title": "Handel: wandering_trader/42",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: wandering_trader/uncommon",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 30, \"max\": 44}}, \"gives\": {\"id\": \"minecraft:netherite_scrap\", \"count\": 7}, \"max_uses\": 1, \"xp\": 100, \"reputation_discount\": 0.5}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/wandering_trader/42.json"
      ],
      "related": []
    },
    {
      "id": "trade_wandering_trader_43",
      "en": {
        "title": "Trade: wandering_trader/43",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: wandering_trader/uncommon",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 30, \"max\": 39}}, \"gives\": {\"id\": \"minecraft:shulker_shell\", \"count\": 2}, \"max_uses\": 1, \"xp\": 200, \"reputation_discount\": 0.5}"
        ]
      },
      "de": {
        "title": "Handel: wandering_trader/43",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: wandering_trader/uncommon",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 30, \"max\": 39}}, \"gives\": {\"id\": \"minecraft:shulker_shell\", \"count\": 2}, \"max_uses\": 1, \"xp\": 200, \"reputation_discount\": 0.5}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/wandering_trader/43.json"
      ],
      "related": []
    },
    {
      "id": "trade_wandering_trader_44",
      "en": {
        "title": "Trade: wandering_trader/44",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: wandering_trader/uncommon",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 5, \"max\": 6}}, \"gives\": {\"id\": \"minecraft:diamond_ore\", \"count\": 6}, \"max_uses\": 5, \"xp\": 150, \"reputation_discount\": 0.5}"
        ]
      },
      "de": {
        "title": "Handel: wandering_trader/44",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: wandering_trader/uncommon",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 5, \"max\": 6}}, \"gives\": {\"id\": \"minecraft:diamond_ore\", \"count\": 6}, \"max_uses\": 5, \"xp\": 150, \"reputation_discount\": 0.5}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/wandering_trader/44.json"
      ],
      "related": []
    },
    {
      "id": "trade_wandering_trader_45",
      "en": {
        "title": "Trade: wandering_trader/45",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: wandering_trader/uncommon",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 5, \"max\": 6}}, \"gives\": {\"id\": \"minecraft:deepslate_diamond_ore\", \"count\": 4}, \"max_uses\": 7, \"xp\": 150, \"reputation_discount\": 0.5}"
        ]
      },
      "de": {
        "title": "Handel: wandering_trader/45",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: wandering_trader/uncommon",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 5, \"max\": 6}}, \"gives\": {\"id\": \"minecraft:deepslate_diamond_ore\", \"count\": 4}, \"max_uses\": 7, \"xp\": 150, \"reputation_discount\": 0.5}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/wandering_trader/45.json"
      ],
      "related": []
    },
    {
      "id": "trade_wandering_trader_46",
      "en": {
        "title": "Trade: wandering_trader/46",
        "summary": "Additional offer; existing Vanilla and SimpleBuilding offers remain in the pool.",
        "details": [
          "Selection tag: wandering_trader/uncommon",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 14, \"max\": 18}}, \"gives\": {\"id\": \"minecraft:budding_amethyst\", \"count\": 1}, \"max_uses\": 3, \"xp\": 150, \"reputation_discount\": 0.5}"
        ]
      },
      "de": {
        "title": "Handel: wandering_trader/46",
        "summary": "Zusätzliches Angebot; Vanilla- und SimpleBuilding-Angebote bleiben im Pool.",
        "details": [
          "Auswahl-Tag: wandering_trader/uncommon",
          "{\"wants\": {\"id\": \"simplemoney:money_bill\", \"count\": {\"type\": \"minecraft:uniform\", \"min\": 14, \"max\": 18}}, \"gives\": {\"id\": \"minecraft:budding_amethyst\", \"count\": 1}, \"max_uses\": 3, \"xp\": 150, \"reputation_discount\": 0.5}"
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/wandering_trader/46.json"
      ],
      "related": []
    },
    {
      "id": "loot",
      "en": {
        "title": "Treasure currency",
        "summary": "One independent roll per chest.",
        "details": [
          "Igloo: 30%, 1 bill; dungeon: 20%, 2–4; End city: 20%, 1–6; mineshaft: 35%, 1–2; shipwreck treasure: 30%, 1–2; stronghold library: 25%, 8–16; buried treasure: 30%, 1–4."
        ]
      },
      "de": {
        "title": "Geld in Truhen",
        "summary": "Ein unabhängiger Versuch pro Truhe.",
        "details": [
          "Iglu: 30 %, 1 Schein; Verlies: 20 %, 2–4; Endsiedlung: 20 %, 1–6; Mine: 35 %, 1–2; Schiffswrack-Schatz: 30 %, 1–2; Festungsbibliothek: 25 %, 8–16; vergrabener Schatz: 30 %, 1–4."
        ]
      },
      "sources": [
        "modules/simplemoney/shared/java/com/simplemoney/MoneyLoot.java"
      ],
      "related": []
    },
    {
      "id": "configuration",
      "en": {
        "title": "Server configuration",
        "summary": "config/simplemoney.json preserves trades.enableVillagerTrades and trades.enableWanderingTrades, both true by default.",
        "details": [
          "The server config controls datapack loading. Restart or reload after changing it. Client settings cannot change a remote server. Cloth Config screen is available from Mod Menu on Fabric and the Mods screen on NeoForge. No blocks, mobs, commands, keybinds or custom enchantments are registered.",
          "Unused rocketStackSize and vaultCooldownDays language remnants had no source implementation and are omitted."
        ]
      },
      "de": {
        "title": "Serverkonfiguration",
        "summary": "config/simplemoney.json behält trades.enableVillagerTrades und trades.enableWanderingTrades; beide standardmäßig true.",
        "details": [
          "Die Serverkonfiguration steuert die Datenladung. Nach Änderungen neu starten oder neu laden. Clienteinstellungen verändern keinen entfernten Server. Cloth-Config-Seite über Mod Menu auf Fabric und die Mods-Seite auf NeoForge. Keine Blöcke, Mobs, Befehle, Tastenkürzel oder eigenen Verzauberungen.",
          "Die unbenutzten Sprachreste rocketStackSize und vaultCooldownDays hatten keine Implementierung und werden ausgelassen."
        ]
      },
      "sources": [
        "modules/simplemoney/shared/java/com/simplemoney/SimpleMoney.java",
        "modules/simplemoney/shared/java/com/simplemoney/client/MoneyConfigScreen.java"
      ],
      "related": []
    },
    {
      "id": "rocket_recipe",
      "en": {
        "title": "Paper rocket recipe",
        "summary": "Additional source recipe: seven paper and two gunpowder produce one rocket.",
        "details": [
          "Pattern: ### / #G# / #G#; # = paper, G = gunpowder."
        ]
      },
      "de": {
        "title": "Papierraketenrezept",
        "summary": "Zusätzliches Quellrezept: sieben Papier und zwei Schwarzpulver ergeben eine Rakete.",
        "details": [
          "Muster: ### / #G# / #G#; # = Papier, G = Schwarzpulver."
        ]
      },
      "sources": [
        "modules/simplemoney/generated/resources/data/simplemoney/recipe/rocket_from_paper.json"
      ],
      "related": []
    },
    {
      "id": "forge_263",
      "en": {
        "title": "Experimental Forge 26.3",
        "summary": "Opt-in Forge adapter with shared items, recipes, trades, loot, JSON configuration, and the same server test catalogue.",
        "details": [
          "Enable -Pforge263=true. Cloth Config screens are unavailable on Forge 26.3; use the server JSON file. Forge client display and optional integrations require separate acceptance."
        ]
      },
      "de": {
        "title": "Experimentelles Forge 26.3",
        "summary": "Opt-in-Forge-Adapter mit gemeinsamen Items, Rezepten, Handel, Beute, JSON-Konfiguration und gleichem Servertestkatalog.",
        "details": [
          "Mit -Pforge263=true aktivieren. Cloth-Config-Seiten fehlen auf Forge 26.3; die Server-JSON-Datei verwenden. Forge-Clientdarstellung und optionale Integrationen brauchen eine eigene Abnahme."
        ]
      },
      "sources": [
        "modules/simplemoney/forge/build.gradle",
        "modules/simplemoney/forge/src/main/java/com/simplebuilding/modules/simplemoney/forge/MoneyForge.java"
      ],
      "related": []
    },
    {
      "id": "money_links",
      "en": {
        "title": "Money Links: Add-on Trades",
        "summary": "Adds buy-only money offers for loaded mods without removing their original trades. Default: enabled.",
        "details": [
          "Price = clamp(ceil(model hours x billsPerHour + rarity tier x rarityStep + crafting slots x craftWeight), safetyFloor, 64). Defaults: 1, 3, 1. Every price has an 8-bill minimum. One item per purchase.",
          "Server-side limits: stock default 2 (cap 4), shared player allowance 8 purchases per 24000 elapsed game ticks (cap 16), cooldown 100 ticks (20-1200). The overworld SavedData budget survives logout, dimension changes, merchant changes and restart. Daylight/time commands do not refresh it.",
          "No linked sell offers, XP rewards or reputation/Hero discounts. Existing offers retain price/stock until replaced; the enabled switch and player budget are checked at every purchase. Settings are server-owned; the client screen edits a local reload/restart draft.",
          "Cores remain wandering-trader-only and get less likely by tier. No mason core offers. Creative spacer, integration token and inert legacy deed are intentionally excluded. Mods without distinct items have empty documented tables."
        ]
      },
      "de": {
        "title": "Geld-Verknuepfungen: Zusatzhandel",
        "summary": "Ergaenzt reine Geld-Kaufangebote fuer geladene Mods, ohne deren alten Handel zu entfernen. Standard: aktiviert.",
        "details": [
          "Preis = clamp(ceil(Modellstunden x billsPerHour + Seltenheitsstufe x rarityStep + Rezeptplaetze x craftWeight), safetyFloor, 64). Standards: 1, 3, 1. Jeder Preis mindestens 8 Scheine. Ein Item je Kauf.",
          "Servergrenzen: Vorrat Standard 2 (Cap 4), gemeinsames Spielerbudget 8 Kaeufe je 24000 vergangene Spielticks (Cap 16), Pause 100 Ticks (20-1200). Overworld-SavedData ueberlebt Logout, Dimensions-/Haendlerwechsel und Neustart. Tageszeitbefehle erneuern es nicht.",
          "Keine verknuepften Rueckkaeufe, XP-Belohnungen oder Ruf-/Heldenrabatte. Bestehende Angebote behalten Preis/Vorrat bis zum Austausch; Aktivierung und Spielerbudget werden bei jedem Kauf geprueft. Servereinstellungen; Clientseite speichert lokalen Reload-/Neustartentwurf.",
          "Kerne nur beim fahrenden Haendler, nach Stufe zunehmend selten. Keine Steinmetz-Kerne. Kreativ-Platzhalter, Testmarke und alte wirkungslose Urkunde ausgeschlossen. Mods ohne eigene Items haben dokumentierte leere Tabellen."
        ]
      },
      "sources": [
        "modules/simplemoney/shared/java/com/simplemoney/MoneyLinks.java",
        "modules/simplemoney/shared/java/com/simplemoney/LinkBudget.java",
        "modules/simplemoney/shared/java/com/simplemoney/SimpleMoney.java"
      ],
      "related": [
        "money_bill"
      ]
    },
    {
      "id": "money_links_simplebuilding",
      "en": {
        "title": "Linked Price Table: simplebuilding",
        "summary": "238 item prices; loaded-mod condition: simplebuilding.",
        "details": [
          "simplebuilding:amethyst_lens: tier 1, hours 6.7, craft 9; 19 bills; toolsmith 2; stock 2; chance 1.",
          "simplebuilding:astral_end_stone: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astral_purpur_block: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astral_vault: tier 4, hours 25.0, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astralit_block: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astralit_brick_slab: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astralit_brick_stairs: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astralit_brick_wall: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astralit_bricks: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astralit_dust: tier 4, hours 25.0, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astralit_lamp: tier 4, hours 25.0, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astralit_ore: tier 4, hours 25.0, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astralit_pillar: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astral_redstone: tier 4, hours 25.0, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astralit_quartz_checker: tier 4, hours 25.0, craft 4; 41 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:astralit_switch: tier 4, hours 25.0, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:backpack: tier 0, hours 0.5, craft 9; 10 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:basic_upgrade_template: tier 0, hours 0.5, craft 9; 10 bills; librarian 1; stock 2; chance 1.",
          "simplebuilding:blackstone_quartz_checker: tier 0, hours 0.5, craft 4; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:blaze_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplebuilding:blueprint: tier 4, hours 25.0, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:bogged_skull: tier 0, hours 0.5, craft 0; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:breeze_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplebuilding:cave_spider_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplebuilding:chiseled_astralit_bricks: tier 4, hours 25.0, craft 2; 39 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:chiseled_ender_quartz_bricks: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:chiseled_nihilith_bricks: tier 4, hours 25.0, craft 2; 39 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:chunk_loader: tier 1, hours 6.7, craft 3; 13 bills; mason 2; stock 2; chance 1.",
          "simplebuilding:construction_light: tier 0, hours 0.5, craft 9; 10 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:copper_building_wand: tier 0, hours 4.0, craft 3; 8 bills; toolsmith 1; stock 2; chance 1.",
          "simplebuilding:copper_chisel: tier 0, hours 4.0, craft 4; 8 bills; toolsmith 1; stock 2; chance 1.",
          "simplebuilding:copper_core: tier 0, hours 4.0, craft 5; 9 bills; wandering_trader 1; stock 2; chance 1.",
          "simplebuilding:copper_pressure_plate: tier 0, hours 4.0, craft 2; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:copper_sledgehammer: tier 0, hours 4.0, craft 5; 9 bills; toolsmith 1; stock 2; chance 1.",
          "simplebuilding:copper_spatula: tier 0, hours 4.0, craft 0; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:cracked_diamond: tier 3, hours 21.0, craft 9; 39 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:cracked_diamond_block: tier 3, hours 21.0, craft 9; 39 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:detector: tier 2, hours 12.5, craft 7; 26 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:diamond_building_wand: tier 3, hours 21.0, craft 3; 33 bills; toolsmith 4; stock 1; chance 1.",
          "simplebuilding:diamond_chisel: tier 3, hours 21.0, craft 4; 34 bills; toolsmith 4; stock 1; chance 1.",
          "simplebuilding:diamond_core: tier 3, hours 21.0, craft 5; 35 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplebuilding:diamond_pebble: tier 3, hours 21.0, craft 0; 30 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:diamond_pressure_plate: tier 3, hours 21.0, craft 2; 32 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:diamond_sledgehammer: tier 3, hours 21.0, craft 5; 35 bills; toolsmith 4; stock 1; chance 1.",
          "simplebuilding:diamond_spatula: tier 3, hours 21.0, craft 0; 30 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:drowned_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplebuilding:echo_sounder: tier 5, hours 38.1, craft 9; 63 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:elytra_pad: tier 3, hours 21.0, craft 3; 33 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:emitting_trim_template: tier 0, hours 0.5, craft 3; 8 bills; librarian 1; stock 2; chance 1.",
          "simplebuilding:enchanted_enderite_apple: tier 5, hours 38.1, craft 0; 54 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enchanted_netherite_apple: tier 4, hours 24.9, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:ender_quartz: tier 4, hours 25.0, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:ender_quartz_block: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:ender_quartz_brick_slab: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:ender_quartz_brick_stairs: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:ender_quartz_brick_wall: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:ender_quartz_bricks: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:ender_quartz_checker: tier 4, hours 25.0, craft 4; 41 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:ender_quartz_pillar: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:ender_quartz_slab: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:ender_quartz_stairs: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_apple: tier 5, hours 38.1, craft 9; 63 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_axe: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_backpack: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_blast_furnace: tier 5, hours 38.1, craft 0; 54 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_block: tier 5, hours 38.1, craft 9; 63 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_boots: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_building_wand: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_bundle: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_carrot: tier 5, hours 38.1, craft 5; 59 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_chest: tier 5, hours 38.1, craft 0; 54 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_chestplate: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_chisel: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_chunk_loader: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_core: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.01.",
          "simplebuilding:enderite_elytra_pad: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_flypad: tier 5, hours 38.1, craft 0; 54 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_furnace: tier 5, hours 38.1, craft 0; 54 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_helmet: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_hoe: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_hopper: tier 5, hours 38.1, craft 0; 54 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_horse_armor: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_ingot: tier 5, hours 38.1, craft 9; 63 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_launchpad: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_leggings: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_nautilus_armor: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_nugget: tier 5, hours 38.1, craft 1; 55 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_pickaxe: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_piston: tier 5, hours 38.1, craft 0; 54 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_pressure_plate: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_quiver: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_scrap: tier 5, hours 38.1, craft 1; 55 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_shovel: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_shulker_box: tier 5, hours 38.1, craft 0; 54 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_sledgehammer: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_smoker: tier 5, hours 38.1, craft 0; 54 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_spawn_teleporter: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_spear: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_sword: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderite_upgrade_template: tier 5, hours 38.1, craft 9; 63 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:enderman_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplebuilding:exposed_copper_pressure_plate: tier 0, hours 4.0, craft 0; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:fine_elytra_pad: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:flypad: tier 5, hours 38.1, craft 4; 58 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:funny_stick: tier 0, hours 0.5, craft 0; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:glowing_trim_template: tier 0, hours 0.5, craft 3; 8 bills; librarian 1; stock 2; chance 1.",
          "simplebuilding:gold_building_wand: tier 2, hours 12.5, craft 3; 22 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:gold_chisel: tier 2, hours 12.5, craft 4; 23 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:gold_core: tier 2, hours 12.5, craft 5; 24 bills; wandering_trader 3; stock 2; chance 0.25.",
          "simplebuilding:gold_sledgehammer: tier 2, hours 12.5, craft 5; 24 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:gold_spatula: tier 2, hours 12.5, craft 0; 19 bills; mason 3; stock 2; chance 1.",
          "simplebuilding:guide_book: tier 0, hours 0.5, craft 2; 8 bills; librarian 1; stock 2; chance 1.",
          "simplebuilding:guide_book_vanilla_start: tier 0, hours 0.5, craft 2; 8 bills; librarian 1; stock 2; chance 1.",
          "simplebuilding:husk_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplebuilding:infused_potion_pad: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:iron_building_wand: tier 1, hours 6.7, craft 3; 13 bills; toolsmith 2; stock 2; chance 1.",
          "simplebuilding:iron_chisel: tier 1, hours 6.7, craft 4; 14 bills; toolsmith 2; stock 2; chance 1.",
          "simplebuilding:iron_core: tier 1, hours 6.7, craft 5; 15 bills; wandering_trader 2; stock 2; chance 0.5.",
          "simplebuilding:iron_sledgehammer: tier 1, hours 6.7, craft 5; 15 bills; toolsmith 2; stock 2; chance 1.",
          "simplebuilding:iron_spatula: tier 1, hours 6.7, craft 0; 10 bills; mason 2; stock 2; chance 1.",
          "simplebuilding:lapis_quartz_checker: tier 0, hours 0.5, craft 4; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:launchpad: tier 1, hours 6.7, craft 3; 13 bills; mason 2; stock 2; chance 1.",
          "simplebuilding:layered_raw_enderite: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:leather_sheet: tier 0, hours 0.5, craft 9; 10 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:levitating_gravel: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:levitating_sand: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:magnet: tier 1, hours 6.7, craft 5; 15 bills; toolsmith 2; stock 2; chance 1.",
          "simplebuilding:netherite_apple: tier 4, hours 24.9, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_backpack: tier 4, hours 24.9, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_blast_furnace: tier 4, hours 24.9, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_building_wand: tier 4, hours 24.9, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_bundle: tier 4, hours 24.9, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_carrot: tier 4, hours 24.9, craft 5; 42 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_chest: tier 4, hours 24.9, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_chisel: tier 4, hours 24.9, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_chunk_loader: tier 4, hours 24.9, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_core: tier 4, hours 24.9, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.05.",
          "simplebuilding:netherite_elytra_pad: tier 4, hours 24.9, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_flypad: tier 4, hours 24.9, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_furnace: tier 4, hours 24.9, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_hopper: tier 4, hours 24.9, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_launchpad: tier 4, hours 24.9, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_nugget: tier 4, hours 24.9, craft 1; 38 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_piston: tier 4, hours 24.9, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_pressure_plate: tier 4, hours 24.9, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_quiver: tier 4, hours 24.9, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_shulker_box: tier 4, hours 24.9, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_sledgehammer: tier 4, hours 24.9, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_smoker: tier 4, hours 24.9, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:netherite_spatula: tier 4, hours 24.9, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihil_end_stone: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihil_purpur_block: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihilith_block: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihilith_brick_slab: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihilith_brick_stairs: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihilith_brick_wall: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihilith_bricks: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihilith_lamp: tier 4, hours 25.0, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihilith_ore: tier 4, hours 25.0, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihilith_pillar: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihil_redstone: tier 4, hours 25.0, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihilith_quartz_checker: tier 4, hours 25.0, craft 4; 41 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihilith_shard: tier 4, hours 25.0, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:nihilith_switch: tier 4, hours 25.0, craft 0; 37 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:octant: tier 2, hours 12.5, craft 8; 27 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_black: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_blue: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_brown: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_cyan: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_gray: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_green: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_light_blue: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_light_gray: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_lime: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_magenta: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_orange: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_pink: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_purple: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_red: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_white: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:octant_yellow: tier 2, hours 12.5, craft 2; 21 bills; toolsmith 3; stock 2; chance 1.",
          "simplebuilding:oxidized_copper_pressure_plate: tier 0, hours 4.0, craft 0; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:polished_astralit: tier 4, hours 25.0, craft 4; 41 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:polished_astralit_slab: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:polished_astralit_stairs: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:polished_astralit_wall: tier 4, hours 25.0, craft 6; 43 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:polished_end_stone: tier 0, hours 0.5, craft 4; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:polished_ender_quartz: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:polished_ender_quartz_slab: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:polished_ender_quartz_stairs: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:polished_ender_quartz_wall: tier 4, hours 25.0, craft 6; 43 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:polished_nihilith: tier 4, hours 25.0, craft 4; 41 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:polished_nihilith_slab: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:polished_nihilith_stairs: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:polished_nihilith_wall: tier 4, hours 25.0, craft 6; 43 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:potion_pad: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:pulsating_trim_template: tier 0, hours 0.5, craft 3; 8 bills; librarian 1; stock 2; chance 1.",
          "simplebuilding:purpur_quartz_checker: tier 0, hours 0.5, craft 4; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:quiver: tier 0, hours 0.5, craft 6; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:raw_enderite: tier 5, hours 38.1, craft 9; 63 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:reinforced_backpack: tier 3, hours 21.0, craft 9; 39 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:reinforced_blast_furnace: tier 3, hours 21.0, craft 9; 39 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:reinforced_bundle: tier 3, hours 21.0, craft 9; 39 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:reinforced_chest: tier 3, hours 21.0, craft 9; 39 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:reinforced_elytra_pad: tier 3, hours 21.0, craft 3; 33 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:reinforced_flypad: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:reinforced_furnace: tier 3, hours 21.0, craft 9; 39 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:reinforced_hopper: tier 3, hours 21.0, craft 9; 39 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:reinforced_piston: tier 3, hours 21.0, craft 9; 39 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:reinforced_potion_pad: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:reinforced_quiver: tier 3, hours 21.0, craft 9; 39 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:reinforced_shulker_box: tier 0, hours 0.5, craft 0; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:reinforced_smoker: tier 3, hours 21.0, craft 9; 39 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:reinforced_sticky_piston: tier 0, hours 0.5, craft 2; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:resin_quartz_checker: tier 0, hours 0.5, craft 4; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:nether_brick_quartz_checker: tier 0, hours 0.5, craft 4; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:red_nether_brick_quartz_checker: tier 0, hours 0.5, craft 4; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:rotator: tier 1, hours 6.7, craft 6; 16 bills; toolsmith 2; stock 2; chance 1.",
          "simplebuilding:shulker_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplebuilding:silverfish_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplebuilding:slime_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplebuilding:spawn_elytra: tier 5, hours 38.1, craft 0; 54 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:spawn_teleporter: tier 3, hours 15.0, craft 3; 27 bills; mason 4; stock 1; chance 1.",
          "simplebuilding:spawn_teleporter_tier_2: tier 4, hours 24.9, craft 3; 40 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:spawn_teleporter_tier_3: tier 0, hours 0.5, craft 0; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:spawn_teleporter_tier_4: tier 0, hours 0.5, craft 0; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:spider_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplebuilding:stellar_flypad: tier 5, hours 38.1, craft 3; 57 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:stone_chisel: tier 0, hours 0.5, craft 4; 8 bills; toolsmith 1; stock 2; chance 1.",
          "simplebuilding:stone_sledgehammer: tier 0, hours 0.5, craft 5; 8 bills; toolsmith 1; stock 2; chance 1.",
          "simplebuilding:stone_spatula: tier 0, hours 0.5, craft 0; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:stray_skull: tier 0, hours 0.5, craft 0; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:suspended_gravel: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:suspended_sand: tier 4, hours 25.0, craft 9; 46 bills; wandering_trader 5; stock 1; chance 0.1.",
          "simplebuilding:velocity_gauge: tier 0, hours 4.0, craft 7; 11 bills; toolsmith 1; stock 2; chance 1.",
          "simplebuilding:waxed_copper_pressure_plate: tier 0, hours 4.0, craft 2; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:waxed_exposed_copper_pressure_plate: tier 0, hours 4.0, craft 2; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:waxed_oxidized_copper_pressure_plate: tier 0, hours 4.0, craft 2; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:waxed_weathered_copper_pressure_plate: tier 0, hours 4.0, craft 2; 8 bills; mason 1; stock 2; chance 1.",
          "simplebuilding:weathered_copper_pressure_plate: tier 0, hours 4.0, craft 0; 8 bills; mason 1; stock 2; chance 1."
        ]
      },
      "de": {
        "title": "Verknuepfte Preisliste: simplebuilding",
        "summary": "238 Itempreise; Mod-Ladebedingung: simplebuilding.",
        "details": [
          "simplebuilding:amethyst_lens: Stufe 1, Stunden 6.7, Rezeptplaetze 9; 19 Scheine; toolsmith 2; Vorrat 2; Chance 1.",
          "simplebuilding:astral_end_stone: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astral_purpur_block: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astral_vault: Stufe 4, Stunden 25.0, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astralit_block: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astralit_brick_slab: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astralit_brick_stairs: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astralit_brick_wall: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astralit_bricks: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astralit_dust: Stufe 4, Stunden 25.0, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astralit_lamp: Stufe 4, Stunden 25.0, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astralit_ore: Stufe 4, Stunden 25.0, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astralit_pillar: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astral_redstone: Stufe 4, Stunden 25.0, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astralit_quartz_checker: Stufe 4, Stunden 25.0, Rezeptplaetze 4; 41 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:astralit_switch: Stufe 4, Stunden 25.0, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:backpack: Stufe 0, Stunden 0.5, Rezeptplaetze 9; 10 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:basic_upgrade_template: Stufe 0, Stunden 0.5, Rezeptplaetze 9; 10 Scheine; librarian 1; Vorrat 2; Chance 1.",
          "simplebuilding:blackstone_quartz_checker: Stufe 0, Stunden 0.5, Rezeptplaetze 4; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:blaze_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplebuilding:blueprint: Stufe 4, Stunden 25.0, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:bogged_skull: Stufe 0, Stunden 0.5, Rezeptplaetze 0; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:breeze_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplebuilding:cave_spider_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplebuilding:chiseled_astralit_bricks: Stufe 4, Stunden 25.0, Rezeptplaetze 2; 39 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:chiseled_ender_quartz_bricks: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:chiseled_nihilith_bricks: Stufe 4, Stunden 25.0, Rezeptplaetze 2; 39 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:chunk_loader: Stufe 1, Stunden 6.7, Rezeptplaetze 3; 13 Scheine; mason 2; Vorrat 2; Chance 1.",
          "simplebuilding:construction_light: Stufe 0, Stunden 0.5, Rezeptplaetze 9; 10 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:copper_building_wand: Stufe 0, Stunden 4.0, Rezeptplaetze 3; 8 Scheine; toolsmith 1; Vorrat 2; Chance 1.",
          "simplebuilding:copper_chisel: Stufe 0, Stunden 4.0, Rezeptplaetze 4; 8 Scheine; toolsmith 1; Vorrat 2; Chance 1.",
          "simplebuilding:copper_core: Stufe 0, Stunden 4.0, Rezeptplaetze 5; 9 Scheine; wandering_trader 1; Vorrat 2; Chance 1.",
          "simplebuilding:copper_pressure_plate: Stufe 0, Stunden 4.0, Rezeptplaetze 2; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:copper_sledgehammer: Stufe 0, Stunden 4.0, Rezeptplaetze 5; 9 Scheine; toolsmith 1; Vorrat 2; Chance 1.",
          "simplebuilding:copper_spatula: Stufe 0, Stunden 4.0, Rezeptplaetze 0; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:cracked_diamond: Stufe 3, Stunden 21.0, Rezeptplaetze 9; 39 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:cracked_diamond_block: Stufe 3, Stunden 21.0, Rezeptplaetze 9; 39 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:detector: Stufe 2, Stunden 12.5, Rezeptplaetze 7; 26 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:diamond_building_wand: Stufe 3, Stunden 21.0, Rezeptplaetze 3; 33 Scheine; toolsmith 4; Vorrat 1; Chance 1.",
          "simplebuilding:diamond_chisel: Stufe 3, Stunden 21.0, Rezeptplaetze 4; 34 Scheine; toolsmith 4; Vorrat 1; Chance 1.",
          "simplebuilding:diamond_core: Stufe 3, Stunden 21.0, Rezeptplaetze 5; 35 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplebuilding:diamond_pebble: Stufe 3, Stunden 21.0, Rezeptplaetze 0; 30 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:diamond_pressure_plate: Stufe 3, Stunden 21.0, Rezeptplaetze 2; 32 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:diamond_sledgehammer: Stufe 3, Stunden 21.0, Rezeptplaetze 5; 35 Scheine; toolsmith 4; Vorrat 1; Chance 1.",
          "simplebuilding:diamond_spatula: Stufe 3, Stunden 21.0, Rezeptplaetze 0; 30 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:drowned_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplebuilding:echo_sounder: Stufe 5, Stunden 38.1, Rezeptplaetze 9; 63 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:elytra_pad: Stufe 3, Stunden 21.0, Rezeptplaetze 3; 33 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:emitting_trim_template: Stufe 0, Stunden 0.5, Rezeptplaetze 3; 8 Scheine; librarian 1; Vorrat 2; Chance 1.",
          "simplebuilding:enchanted_enderite_apple: Stufe 5, Stunden 38.1, Rezeptplaetze 0; 54 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enchanted_netherite_apple: Stufe 4, Stunden 24.9, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:ender_quartz: Stufe 4, Stunden 25.0, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:ender_quartz_block: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:ender_quartz_brick_slab: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:ender_quartz_brick_stairs: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:ender_quartz_brick_wall: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:ender_quartz_bricks: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:ender_quartz_checker: Stufe 4, Stunden 25.0, Rezeptplaetze 4; 41 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:ender_quartz_pillar: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:ender_quartz_slab: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:ender_quartz_stairs: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_apple: Stufe 5, Stunden 38.1, Rezeptplaetze 9; 63 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_axe: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_backpack: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_blast_furnace: Stufe 5, Stunden 38.1, Rezeptplaetze 0; 54 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_block: Stufe 5, Stunden 38.1, Rezeptplaetze 9; 63 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_boots: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_building_wand: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_bundle: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_carrot: Stufe 5, Stunden 38.1, Rezeptplaetze 5; 59 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_chest: Stufe 5, Stunden 38.1, Rezeptplaetze 0; 54 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_chestplate: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_chisel: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_chunk_loader: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_core: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.01.",
          "simplebuilding:enderite_elytra_pad: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_flypad: Stufe 5, Stunden 38.1, Rezeptplaetze 0; 54 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_furnace: Stufe 5, Stunden 38.1, Rezeptplaetze 0; 54 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_helmet: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_hoe: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_hopper: Stufe 5, Stunden 38.1, Rezeptplaetze 0; 54 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_horse_armor: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_ingot: Stufe 5, Stunden 38.1, Rezeptplaetze 9; 63 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_launchpad: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_leggings: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_nautilus_armor: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_nugget: Stufe 5, Stunden 38.1, Rezeptplaetze 1; 55 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_pickaxe: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_piston: Stufe 5, Stunden 38.1, Rezeptplaetze 0; 54 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_pressure_plate: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_quiver: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_scrap: Stufe 5, Stunden 38.1, Rezeptplaetze 1; 55 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_shovel: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_shulker_box: Stufe 5, Stunden 38.1, Rezeptplaetze 0; 54 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_sledgehammer: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_smoker: Stufe 5, Stunden 38.1, Rezeptplaetze 0; 54 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_spawn_teleporter: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_spear: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_sword: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderite_upgrade_template: Stufe 5, Stunden 38.1, Rezeptplaetze 9; 63 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:enderman_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplebuilding:exposed_copper_pressure_plate: Stufe 0, Stunden 4.0, Rezeptplaetze 0; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:fine_elytra_pad: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:flypad: Stufe 5, Stunden 38.1, Rezeptplaetze 4; 58 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:funny_stick: Stufe 0, Stunden 0.5, Rezeptplaetze 0; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:glowing_trim_template: Stufe 0, Stunden 0.5, Rezeptplaetze 3; 8 Scheine; librarian 1; Vorrat 2; Chance 1.",
          "simplebuilding:gold_building_wand: Stufe 2, Stunden 12.5, Rezeptplaetze 3; 22 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:gold_chisel: Stufe 2, Stunden 12.5, Rezeptplaetze 4; 23 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:gold_core: Stufe 2, Stunden 12.5, Rezeptplaetze 5; 24 Scheine; wandering_trader 3; Vorrat 2; Chance 0.25.",
          "simplebuilding:gold_sledgehammer: Stufe 2, Stunden 12.5, Rezeptplaetze 5; 24 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:gold_spatula: Stufe 2, Stunden 12.5, Rezeptplaetze 0; 19 Scheine; mason 3; Vorrat 2; Chance 1.",
          "simplebuilding:guide_book: Stufe 0, Stunden 0.5, Rezeptplaetze 2; 8 Scheine; librarian 1; Vorrat 2; Chance 1.",
          "simplebuilding:guide_book_vanilla_start: Stufe 0, Stunden 0.5, Rezeptplaetze 2; 8 Scheine; librarian 1; Vorrat 2; Chance 1.",
          "simplebuilding:husk_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplebuilding:infused_potion_pad: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:iron_building_wand: Stufe 1, Stunden 6.7, Rezeptplaetze 3; 13 Scheine; toolsmith 2; Vorrat 2; Chance 1.",
          "simplebuilding:iron_chisel: Stufe 1, Stunden 6.7, Rezeptplaetze 4; 14 Scheine; toolsmith 2; Vorrat 2; Chance 1.",
          "simplebuilding:iron_core: Stufe 1, Stunden 6.7, Rezeptplaetze 5; 15 Scheine; wandering_trader 2; Vorrat 2; Chance 0.5.",
          "simplebuilding:iron_sledgehammer: Stufe 1, Stunden 6.7, Rezeptplaetze 5; 15 Scheine; toolsmith 2; Vorrat 2; Chance 1.",
          "simplebuilding:iron_spatula: Stufe 1, Stunden 6.7, Rezeptplaetze 0; 10 Scheine; mason 2; Vorrat 2; Chance 1.",
          "simplebuilding:lapis_quartz_checker: Stufe 0, Stunden 0.5, Rezeptplaetze 4; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:launchpad: Stufe 1, Stunden 6.7, Rezeptplaetze 3; 13 Scheine; mason 2; Vorrat 2; Chance 1.",
          "simplebuilding:layered_raw_enderite: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:leather_sheet: Stufe 0, Stunden 0.5, Rezeptplaetze 9; 10 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:levitating_gravel: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:levitating_sand: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:magnet: Stufe 1, Stunden 6.7, Rezeptplaetze 5; 15 Scheine; toolsmith 2; Vorrat 2; Chance 1.",
          "simplebuilding:netherite_apple: Stufe 4, Stunden 24.9, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_backpack: Stufe 4, Stunden 24.9, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_blast_furnace: Stufe 4, Stunden 24.9, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_building_wand: Stufe 4, Stunden 24.9, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_bundle: Stufe 4, Stunden 24.9, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_carrot: Stufe 4, Stunden 24.9, Rezeptplaetze 5; 42 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_chest: Stufe 4, Stunden 24.9, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_chisel: Stufe 4, Stunden 24.9, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_chunk_loader: Stufe 4, Stunden 24.9, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_core: Stufe 4, Stunden 24.9, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.05.",
          "simplebuilding:netherite_elytra_pad: Stufe 4, Stunden 24.9, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_flypad: Stufe 4, Stunden 24.9, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_furnace: Stufe 4, Stunden 24.9, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_hopper: Stufe 4, Stunden 24.9, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_launchpad: Stufe 4, Stunden 24.9, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_nugget: Stufe 4, Stunden 24.9, Rezeptplaetze 1; 38 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_piston: Stufe 4, Stunden 24.9, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_pressure_plate: Stufe 4, Stunden 24.9, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_quiver: Stufe 4, Stunden 24.9, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_shulker_box: Stufe 4, Stunden 24.9, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_sledgehammer: Stufe 4, Stunden 24.9, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_smoker: Stufe 4, Stunden 24.9, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:netherite_spatula: Stufe 4, Stunden 24.9, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihil_end_stone: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihil_purpur_block: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihilith_block: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihilith_brick_slab: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihilith_brick_stairs: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihilith_brick_wall: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihilith_bricks: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihilith_lamp: Stufe 4, Stunden 25.0, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihilith_ore: Stufe 4, Stunden 25.0, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihilith_pillar: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihil_redstone: Stufe 4, Stunden 25.0, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihilith_quartz_checker: Stufe 4, Stunden 25.0, Rezeptplaetze 4; 41 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihilith_shard: Stufe 4, Stunden 25.0, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:nihilith_switch: Stufe 4, Stunden 25.0, Rezeptplaetze 0; 37 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:octant: Stufe 2, Stunden 12.5, Rezeptplaetze 8; 27 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_black: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_blue: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_brown: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_cyan: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_gray: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_green: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_light_blue: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_light_gray: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_lime: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_magenta: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_orange: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_pink: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_purple: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_red: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_white: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:octant_yellow: Stufe 2, Stunden 12.5, Rezeptplaetze 2; 21 Scheine; toolsmith 3; Vorrat 2; Chance 1.",
          "simplebuilding:oxidized_copper_pressure_plate: Stufe 0, Stunden 4.0, Rezeptplaetze 0; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:polished_astralit: Stufe 4, Stunden 25.0, Rezeptplaetze 4; 41 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:polished_astralit_slab: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:polished_astralit_stairs: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:polished_astralit_wall: Stufe 4, Stunden 25.0, Rezeptplaetze 6; 43 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:polished_end_stone: Stufe 0, Stunden 0.5, Rezeptplaetze 4; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:polished_ender_quartz: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:polished_ender_quartz_slab: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:polished_ender_quartz_stairs: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:polished_ender_quartz_wall: Stufe 4, Stunden 25.0, Rezeptplaetze 6; 43 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:polished_nihilith: Stufe 4, Stunden 25.0, Rezeptplaetze 4; 41 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:polished_nihilith_slab: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:polished_nihilith_stairs: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:polished_nihilith_wall: Stufe 4, Stunden 25.0, Rezeptplaetze 6; 43 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:potion_pad: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:pulsating_trim_template: Stufe 0, Stunden 0.5, Rezeptplaetze 3; 8 Scheine; librarian 1; Vorrat 2; Chance 1.",
          "simplebuilding:purpur_quartz_checker: Stufe 0, Stunden 0.5, Rezeptplaetze 4; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:quiver: Stufe 0, Stunden 0.5, Rezeptplaetze 6; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:raw_enderite: Stufe 5, Stunden 38.1, Rezeptplaetze 9; 63 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:reinforced_backpack: Stufe 3, Stunden 21.0, Rezeptplaetze 9; 39 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:reinforced_blast_furnace: Stufe 3, Stunden 21.0, Rezeptplaetze 9; 39 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:reinforced_bundle: Stufe 3, Stunden 21.0, Rezeptplaetze 9; 39 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:reinforced_chest: Stufe 3, Stunden 21.0, Rezeptplaetze 9; 39 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:reinforced_elytra_pad: Stufe 3, Stunden 21.0, Rezeptplaetze 3; 33 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:reinforced_flypad: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:reinforced_furnace: Stufe 3, Stunden 21.0, Rezeptplaetze 9; 39 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:reinforced_hopper: Stufe 3, Stunden 21.0, Rezeptplaetze 9; 39 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:reinforced_piston: Stufe 3, Stunden 21.0, Rezeptplaetze 9; 39 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:reinforced_potion_pad: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:reinforced_quiver: Stufe 3, Stunden 21.0, Rezeptplaetze 9; 39 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:reinforced_shulker_box: Stufe 0, Stunden 0.5, Rezeptplaetze 0; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:reinforced_smoker: Stufe 3, Stunden 21.0, Rezeptplaetze 9; 39 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:reinforced_sticky_piston: Stufe 0, Stunden 0.5, Rezeptplaetze 2; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:resin_quartz_checker: Stufe 0, Stunden 0.5, Rezeptplaetze 4; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:nether_brick_quartz_checker: Stufe 0, Stunden 0.5, Rezeptplaetze 4; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:red_nether_brick_quartz_checker: Stufe 0, Stunden 0.5, Rezeptplaetze 4; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:rotator: Stufe 1, Stunden 6.7, Rezeptplaetze 6; 16 Scheine; toolsmith 2; Vorrat 2; Chance 1.",
          "simplebuilding:shulker_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplebuilding:silverfish_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplebuilding:slime_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplebuilding:spawn_elytra: Stufe 5, Stunden 38.1, Rezeptplaetze 0; 54 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:spawn_teleporter: Stufe 3, Stunden 15.0, Rezeptplaetze 3; 27 Scheine; mason 4; Vorrat 1; Chance 1.",
          "simplebuilding:spawn_teleporter_Stufe_2: Stufe 4, Stunden 24.9, Rezeptplaetze 3; 40 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:spawn_teleporter_Stufe_3: Stufe 0, Stunden 0.5, Rezeptplaetze 0; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:spawn_teleporter_Stufe_4: Stufe 0, Stunden 0.5, Rezeptplaetze 0; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:spider_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplebuilding:stellar_flypad: Stufe 5, Stunden 38.1, Rezeptplaetze 3; 57 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:stone_chisel: Stufe 0, Stunden 0.5, Rezeptplaetze 4; 8 Scheine; toolsmith 1; Vorrat 2; Chance 1.",
          "simplebuilding:stone_sledgehammer: Stufe 0, Stunden 0.5, Rezeptplaetze 5; 8 Scheine; toolsmith 1; Vorrat 2; Chance 1.",
          "simplebuilding:stone_spatula: Stufe 0, Stunden 0.5, Rezeptplaetze 0; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:stray_skull: Stufe 0, Stunden 0.5, Rezeptplaetze 0; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:suspended_gravel: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:suspended_sand: Stufe 4, Stunden 25.0, Rezeptplaetze 9; 46 Scheine; wandering_trader 5; Vorrat 1; Chance 0.1.",
          "simplebuilding:velocity_gauge: Stufe 0, Stunden 4.0, Rezeptplaetze 7; 11 Scheine; toolsmith 1; Vorrat 2; Chance 1.",
          "simplebuilding:waxed_copper_pressure_plate: Stufe 0, Stunden 4.0, Rezeptplaetze 2; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:waxed_exposed_copper_pressure_plate: Stufe 0, Stunden 4.0, Rezeptplaetze 2; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:waxed_oxidized_copper_pressure_plate: Stufe 0, Stunden 4.0, Rezeptplaetze 2; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:waxed_weathered_copper_pressure_plate: Stufe 0, Stunden 4.0, Rezeptplaetze 2; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplebuilding:weathered_copper_pressure_plate: Stufe 0, Stunden 4.0, Rezeptplaetze 0; 8 Scheine; mason 1; Vorrat 2; Chance 1."
        ]
      },
      "sources": [
        "modules/simplemoney/shared/resources/data/simplemoney/money/simplebuilding/prices.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/amethyst_lens.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astral_end_stone.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astral_purpur_block.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astral_vault.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astralit_block.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astralit_brick_slab.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astralit_brick_stairs.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astralit_brick_wall.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astralit_bricks.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astralit_dust.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astralit_lamp.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astralit_ore.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astralit_pillar.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astral_redstone.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astralit_quartz_checker.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/astralit_switch.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/backpack.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/basic_upgrade_template.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/blackstone_quartz_checker.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/blaze_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/blueprint.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/bogged_skull.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/breeze_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/cave_spider_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/chiseled_astralit_bricks.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/chiseled_ender_quartz_bricks.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/chiseled_nihilith_bricks.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/chunk_loader.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/construction_light.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/copper_building_wand.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/copper_chisel.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/copper_core.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/copper_pressure_plate.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/copper_sledgehammer.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/copper_spatula.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/cracked_diamond.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/cracked_diamond_block.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/detector.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/diamond_building_wand.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/diamond_chisel.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/diamond_core.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/diamond_pebble.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/diamond_pressure_plate.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/diamond_sledgehammer.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/diamond_spatula.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/drowned_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/echo_sounder.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/elytra_pad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/emitting_trim_template.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enchanted_enderite_apple.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enchanted_netherite_apple.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/ender_quartz.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/ender_quartz_block.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/ender_quartz_brick_slab.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/ender_quartz_brick_stairs.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/ender_quartz_brick_wall.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/ender_quartz_bricks.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/ender_quartz_checker.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/ender_quartz_pillar.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/ender_quartz_slab.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/ender_quartz_stairs.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_apple.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_axe.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_backpack.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_blast_furnace.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_block.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_boots.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_building_wand.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_bundle.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_carrot.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_chest.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_chestplate.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_chisel.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_chunk_loader.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_core.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_elytra_pad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_flypad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_furnace.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_helmet.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_hoe.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_hopper.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_horse_armor.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_ingot.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_launchpad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_leggings.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_nautilus_armor.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_nugget.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_pickaxe.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_piston.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_pressure_plate.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_quiver.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_scrap.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_shovel.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_shulker_box.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_sledgehammer.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_smoker.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_spawn_teleporter.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_spear.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_sword.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderite_upgrade_template.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/enderman_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/exposed_copper_pressure_plate.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/fine_elytra_pad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/flypad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/funny_stick.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/glowing_trim_template.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/gold_building_wand.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/gold_chisel.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/gold_core.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/gold_sledgehammer.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/gold_spatula.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/guide_book.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/guide_book_vanilla_start.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/husk_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/infused_potion_pad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/iron_building_wand.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/iron_chisel.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/iron_core.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/iron_sledgehammer.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/iron_spatula.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/lapis_quartz_checker.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/launchpad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/layered_raw_enderite.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/leather_sheet.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/levitating_gravel.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/levitating_sand.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/magnet.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_apple.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_backpack.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_blast_furnace.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_building_wand.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_bundle.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_carrot.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_chest.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_chisel.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_chunk_loader.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_core.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_elytra_pad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_flypad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_furnace.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_hopper.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_launchpad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_nugget.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_piston.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_pressure_plate.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_quiver.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_shulker_box.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_sledgehammer.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_smoker.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/netherite_spatula.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihil_end_stone.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihil_purpur_block.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihilith_block.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihilith_brick_slab.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihilith_brick_stairs.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihilith_brick_wall.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihilith_bricks.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihilith_lamp.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihilith_ore.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihilith_pillar.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihil_redstone.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihilith_quartz_checker.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihilith_shard.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nihilith_switch.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_black.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_blue.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_brown.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_cyan.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_gray.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_green.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_light_blue.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_light_gray.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_lime.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_magenta.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_orange.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_pink.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_purple.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_red.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_white.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/octant_yellow.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/oxidized_copper_pressure_plate.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_astralit.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_astralit_slab.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_astralit_stairs.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_astralit_wall.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_end_stone.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_ender_quartz.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_ender_quartz_slab.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_ender_quartz_stairs.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_ender_quartz_wall.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_nihilith.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_nihilith_slab.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_nihilith_stairs.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/polished_nihilith_wall.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/potion_pad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/pulsating_trim_template.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/purpur_quartz_checker.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/quiver.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/raw_enderite.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_backpack.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_blast_furnace.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_bundle.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_chest.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_elytra_pad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_flypad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_furnace.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_hopper.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_piston.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_potion_pad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_quiver.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_shulker_box.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_smoker.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/reinforced_sticky_piston.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/resin_quartz_checker.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/nether_brick_quartz_checker.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/red_nether_brick_quartz_checker.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/rotator.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/shulker_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/silverfish_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/slime_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/spawn_elytra.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/spawn_teleporter.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/spawn_teleporter_tier_2.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/spawn_teleporter_tier_3.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/spawn_teleporter_tier_4.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/spider_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/stellar_flypad.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/stone_chisel.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/stone_sledgehammer.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/stone_spatula.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/stray_skull.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/suspended_gravel.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/suspended_sand.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/velocity_gauge.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/waxed_copper_pressure_plate.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/waxed_exposed_copper_pressure_plate.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/waxed_oxidized_copper_pressure_plate.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/waxed_weathered_copper_pressure_plate.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplebuilding/weathered_copper_pressure_plate.json"
      ],
      "related": [
        "simplebuilding:amethyst_lens",
        "simplebuilding:astral_end_stone",
        "simplebuilding:astral_purpur_block",
        "simplebuilding:astral_vault",
        "simplebuilding:astralit_block",
        "simplebuilding:astralit_brick_slab",
        "simplebuilding:astralit_brick_stairs",
        "simplebuilding:astralit_brick_wall",
        "simplebuilding:astralit_bricks",
        "simplebuilding:astralit_dust",
        "simplebuilding:astralit_lamp",
        "simplebuilding:astralit_ore",
        "simplebuilding:astralit_pillar",
        "simplebuilding:astral_redstone",
        "simplebuilding:astralit_quartz_checker",
        "simplebuilding:astralit_switch",
        "simplebuilding:backpack",
        "simplebuilding:basic_upgrade_template",
        "simplebuilding:blackstone_quartz_checker",
        "simplebuilding:blaze_head",
        "simplebuilding:blueprint",
        "simplebuilding:bogged_skull",
        "simplebuilding:breeze_head",
        "simplebuilding:cave_spider_head",
        "simplebuilding:chiseled_astralit_bricks",
        "simplebuilding:chiseled_ender_quartz_bricks",
        "simplebuilding:chiseled_nihilith_bricks",
        "simplebuilding:chunk_loader",
        "simplebuilding:construction_light",
        "simplebuilding:copper_building_wand",
        "simplebuilding:copper_chisel",
        "simplebuilding:copper_core",
        "simplebuilding:copper_pressure_plate",
        "simplebuilding:copper_sledgehammer",
        "simplebuilding:copper_spatula",
        "simplebuilding:cracked_diamond",
        "simplebuilding:cracked_diamond_block",
        "simplebuilding:detector",
        "simplebuilding:diamond_building_wand",
        "simplebuilding:diamond_chisel",
        "simplebuilding:diamond_core",
        "simplebuilding:diamond_pebble",
        "simplebuilding:diamond_pressure_plate",
        "simplebuilding:diamond_sledgehammer",
        "simplebuilding:diamond_spatula",
        "simplebuilding:drowned_head",
        "simplebuilding:echo_sounder",
        "simplebuilding:elytra_pad",
        "simplebuilding:emitting_trim_template",
        "simplebuilding:enchanted_enderite_apple",
        "simplebuilding:enchanted_netherite_apple",
        "simplebuilding:ender_quartz",
        "simplebuilding:ender_quartz_block",
        "simplebuilding:ender_quartz_brick_slab",
        "simplebuilding:ender_quartz_brick_stairs",
        "simplebuilding:ender_quartz_brick_wall",
        "simplebuilding:ender_quartz_bricks",
        "simplebuilding:ender_quartz_checker",
        "simplebuilding:ender_quartz_pillar",
        "simplebuilding:ender_quartz_slab",
        "simplebuilding:ender_quartz_stairs",
        "simplebuilding:enderite_apple",
        "simplebuilding:enderite_axe",
        "simplebuilding:enderite_backpack",
        "simplebuilding:enderite_blast_furnace",
        "simplebuilding:enderite_block",
        "simplebuilding:enderite_boots",
        "simplebuilding:enderite_building_wand",
        "simplebuilding:enderite_bundle",
        "simplebuilding:enderite_carrot",
        "simplebuilding:enderite_chest",
        "simplebuilding:enderite_chestplate",
        "simplebuilding:enderite_chisel",
        "simplebuilding:enderite_chunk_loader",
        "simplebuilding:enderite_core",
        "simplebuilding:enderite_elytra_pad",
        "simplebuilding:enderite_flypad",
        "simplebuilding:enderite_furnace",
        "simplebuilding:enderite_helmet",
        "simplebuilding:enderite_hoe",
        "simplebuilding:enderite_hopper",
        "simplebuilding:enderite_horse_armor",
        "simplebuilding:enderite_ingot",
        "simplebuilding:enderite_launchpad",
        "simplebuilding:enderite_leggings",
        "simplebuilding:enderite_nautilus_armor",
        "simplebuilding:enderite_nugget",
        "simplebuilding:enderite_pickaxe",
        "simplebuilding:enderite_piston",
        "simplebuilding:enderite_pressure_plate",
        "simplebuilding:enderite_quiver",
        "simplebuilding:enderite_scrap",
        "simplebuilding:enderite_shovel",
        "simplebuilding:enderite_shulker_box",
        "simplebuilding:enderite_sledgehammer",
        "simplebuilding:enderite_smoker",
        "simplebuilding:enderite_spawn_teleporter",
        "simplebuilding:enderite_spear",
        "simplebuilding:enderite_sword",
        "simplebuilding:enderite_upgrade_template",
        "simplebuilding:enderman_head",
        "simplebuilding:exposed_copper_pressure_plate",
        "simplebuilding:fine_elytra_pad",
        "simplebuilding:flypad",
        "simplebuilding:funny_stick",
        "simplebuilding:glowing_trim_template",
        "simplebuilding:gold_building_wand",
        "simplebuilding:gold_chisel",
        "simplebuilding:gold_core",
        "simplebuilding:gold_sledgehammer",
        "simplebuilding:gold_spatula",
        "simplebuilding:guide_book",
        "simplebuilding:guide_book_vanilla_start",
        "simplebuilding:husk_head",
        "simplebuilding:infused_potion_pad",
        "simplebuilding:iron_building_wand",
        "simplebuilding:iron_chisel",
        "simplebuilding:iron_core",
        "simplebuilding:iron_sledgehammer",
        "simplebuilding:iron_spatula",
        "simplebuilding:lapis_quartz_checker",
        "simplebuilding:launchpad",
        "simplebuilding:layered_raw_enderite",
        "simplebuilding:leather_sheet",
        "simplebuilding:levitating_gravel",
        "simplebuilding:levitating_sand",
        "simplebuilding:magnet",
        "simplebuilding:netherite_apple",
        "simplebuilding:netherite_backpack",
        "simplebuilding:netherite_blast_furnace",
        "simplebuilding:netherite_building_wand",
        "simplebuilding:netherite_bundle",
        "simplebuilding:netherite_carrot",
        "simplebuilding:netherite_chest",
        "simplebuilding:netherite_chisel",
        "simplebuilding:netherite_chunk_loader",
        "simplebuilding:netherite_core",
        "simplebuilding:netherite_elytra_pad",
        "simplebuilding:netherite_flypad",
        "simplebuilding:netherite_furnace",
        "simplebuilding:netherite_hopper",
        "simplebuilding:netherite_launchpad",
        "simplebuilding:netherite_nugget",
        "simplebuilding:netherite_piston",
        "simplebuilding:netherite_pressure_plate",
        "simplebuilding:netherite_quiver",
        "simplebuilding:netherite_shulker_box",
        "simplebuilding:netherite_sledgehammer",
        "simplebuilding:netherite_smoker",
        "simplebuilding:netherite_spatula",
        "simplebuilding:nihil_end_stone",
        "simplebuilding:nihil_purpur_block",
        "simplebuilding:nihilith_block",
        "simplebuilding:nihilith_brick_slab",
        "simplebuilding:nihilith_brick_stairs",
        "simplebuilding:nihilith_brick_wall",
        "simplebuilding:nihilith_bricks",
        "simplebuilding:nihilith_lamp",
        "simplebuilding:nihilith_ore",
        "simplebuilding:nihilith_pillar",
        "simplebuilding:nihil_redstone",
        "simplebuilding:nihilith_quartz_checker",
        "simplebuilding:nihilith_shard",
        "simplebuilding:nihilith_switch",
        "simplebuilding:octant",
        "simplebuilding:octant_black",
        "simplebuilding:octant_blue",
        "simplebuilding:octant_brown",
        "simplebuilding:octant_cyan",
        "simplebuilding:octant_gray",
        "simplebuilding:octant_green",
        "simplebuilding:octant_light_blue",
        "simplebuilding:octant_light_gray",
        "simplebuilding:octant_lime",
        "simplebuilding:octant_magenta",
        "simplebuilding:octant_orange",
        "simplebuilding:octant_pink",
        "simplebuilding:octant_purple",
        "simplebuilding:octant_red",
        "simplebuilding:octant_white",
        "simplebuilding:octant_yellow",
        "simplebuilding:oxidized_copper_pressure_plate",
        "simplebuilding:polished_astralit",
        "simplebuilding:polished_astralit_slab",
        "simplebuilding:polished_astralit_stairs",
        "simplebuilding:polished_astralit_wall",
        "simplebuilding:polished_end_stone",
        "simplebuilding:polished_ender_quartz",
        "simplebuilding:polished_ender_quartz_slab",
        "simplebuilding:polished_ender_quartz_stairs",
        "simplebuilding:polished_ender_quartz_wall",
        "simplebuilding:polished_nihilith",
        "simplebuilding:polished_nihilith_slab",
        "simplebuilding:polished_nihilith_stairs",
        "simplebuilding:polished_nihilith_wall",
        "simplebuilding:potion_pad",
        "simplebuilding:pulsating_trim_template",
        "simplebuilding:purpur_quartz_checker",
        "simplebuilding:quiver",
        "simplebuilding:raw_enderite",
        "simplebuilding:reinforced_backpack",
        "simplebuilding:reinforced_blast_furnace",
        "simplebuilding:reinforced_bundle",
        "simplebuilding:reinforced_chest",
        "simplebuilding:reinforced_elytra_pad",
        "simplebuilding:reinforced_flypad",
        "simplebuilding:reinforced_furnace",
        "simplebuilding:reinforced_hopper",
        "simplebuilding:reinforced_piston",
        "simplebuilding:reinforced_potion_pad",
        "simplebuilding:reinforced_quiver",
        "simplebuilding:reinforced_shulker_box",
        "simplebuilding:reinforced_smoker",
        "simplebuilding:reinforced_sticky_piston",
        "simplebuilding:resin_quartz_checker",
        "simplebuilding:nether_brick_quartz_checker",
        "simplebuilding:red_nether_brick_quartz_checker",
        "simplebuilding:rotator",
        "simplebuilding:shulker_head",
        "simplebuilding:silverfish_head",
        "simplebuilding:slime_head",
        "simplebuilding:spawn_elytra",
        "simplebuilding:spawn_teleporter",
        "simplebuilding:spawn_teleporter_tier_2",
        "simplebuilding:spawn_teleporter_tier_3",
        "simplebuilding:spawn_teleporter_tier_4",
        "simplebuilding:spider_head",
        "simplebuilding:stellar_flypad",
        "simplebuilding:stone_chisel",
        "simplebuilding:stone_sledgehammer",
        "simplebuilding:stone_spatula",
        "simplebuilding:stray_skull",
        "simplebuilding:suspended_gravel",
        "simplebuilding:suspended_sand",
        "simplebuilding:velocity_gauge",
        "simplebuilding:waxed_copper_pressure_plate",
        "simplebuilding:waxed_exposed_copper_pressure_plate",
        "simplebuilding:waxed_oxidized_copper_pressure_plate",
        "simplebuilding:waxed_weathered_copper_pressure_plate",
        "simplebuilding:weathered_copper_pressure_plate"
      ]
    },
    {
      "id": "money_links_simplefun",
      "en": {
        "title": "Linked Price Table: simplefun",
        "summary": "5 item prices; loaded-mod condition: simplefun.",
        "details": [
          "simplefun:brick_snowball: tier 0, hours 0.5, craft 0; 8 bills; mason 1; stock 2; chance 1.",
          "simplefun:pig_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplefun:cow_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplefun:chicken_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1.",
          "simplefun:sheep_head: tier 3, hours 15.0, craft 0; 24 bills; wandering_trader 4; stock 1; chance 0.1."
        ]
      },
      "de": {
        "title": "Verknuepfte Preisliste: simplefun",
        "summary": "5 Itempreise; Mod-Ladebedingung: simplefun.",
        "details": [
          "simplefun:brick_snowball: Stufe 0, Stunden 0.5, Rezeptplaetze 0; 8 Scheine; mason 1; Vorrat 2; Chance 1.",
          "simplefun:pig_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplefun:cow_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplefun:chicken_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1.",
          "simplefun:sheep_head: Stufe 3, Stunden 15.0, Rezeptplaetze 0; 24 Scheine; wandering_trader 4; Vorrat 1; Chance 0.1."
        ]
      },
      "sources": [
        "modules/simplemoney/shared/resources/data/simplemoney/money/simplefun/prices.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplefun/brick_snowball.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplefun/pig_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplefun/cow_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplefun/chicken_head.json",
        "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/links/simplefun/sheep_head.json"
      ],
      "related": [
        "simplefun:brick_snowball",
        "simplefun:pig_head",
        "simplefun:cow_head",
        "simplefun:chicken_head",
        "simplefun:sheep_head"
      ]
    },
    {
      "id": "money_links_simplemodels",
      "en": {
        "title": "Linked Price Table: simplemodels",
        "summary": "0 item prices; loaded-mod condition: simplemodels.",
        "details": [
          "No distinct survival items to trade. {}"
        ]
      },
      "de": {
        "title": "Verknuepfte Preisliste: simplemodels",
        "summary": "0 Itempreise; Mod-Ladebedingung: simplemodels.",
        "details": [
          "Keine eigenen Survival-Items fuer den Handel. {}"
        ]
      },
      "sources": [
        "modules/simplemoney/shared/resources/data/simplemoney/money/simplemodels/prices.json"
      ],
      "related": []
    },
    {
      "id": "money_links_simplequalityoflife",
      "en": {
        "title": "Linked Price Table: simplequalityoflife",
        "summary": "0 item prices; loaded-mod condition: simplequalityoflife.",
        "details": [
          "No distinct survival items to trade. {}"
        ]
      },
      "de": {
        "title": "Verknuepfte Preisliste: simplequalityoflife",
        "summary": "0 Itempreise; Mod-Ladebedingung: simplequalityoflife.",
        "details": [
          "Keine eigenen Survival-Items fuer den Handel. {}"
        ]
      },
      "sources": [
        "modules/simplemoney/shared/resources/data/simplemoney/money/simplequalityoflife/prices.json"
      ],
      "related": []
    },
    {
      "id": "money_links_simpleriding",
      "en": {
        "title": "Linked Price Table: simpleriding",
        "summary": "0 item prices; loaded-mod condition: simpleriding.",
        "details": [
          "No distinct survival items to trade. {}"
        ]
      },
      "de": {
        "title": "Verknuepfte Preisliste: simpleriding",
        "summary": "0 Itempreise; Mod-Ladebedingung: simpleriding.",
        "details": [
          "Keine eigenen Survival-Items fuer den Handel. {}"
        ]
      },
      "sources": [
        "modules/simplemoney/shared/resources/data/simplemoney/money/simpleriding/prices.json"
      ],
      "related": []
    },
    {
      "id": "money_links_simplesounds",
      "en": {
        "title": "Linked Price Table: simplesounds",
        "summary": "0 item prices; loaded-mod condition: simplesounds.",
        "details": [
          "No distinct survival items to trade. {}"
        ]
      },
      "de": {
        "title": "Verknuepfte Preisliste: simplesounds",
        "summary": "0 Itempreise; Mod-Ladebedingung: simplesounds.",
        "details": [
          "Keine eigenen Survival-Items fuer den Handel. {}"
        ]
      },
      "sources": [
        "modules/simplemoney/shared/resources/data/simplemoney/money/simplesounds/prices.json"
      ],
      "related": []
    },
    {
      "id": "money_links_simpletweaks",
      "en": {
        "title": "Linked Price Table: simpletweaks",
        "summary": "0 item prices; loaded-mod condition: simpletweaks.",
        "details": [
          "No distinct survival items to trade. {'simpletweaks:claim_deed': 'Inert legacy/test item; no survival trade.'}"
        ]
      },
      "de": {
        "title": "Verknuepfte Preisliste: simpletweaks",
        "summary": "0 Itempreise; Mod-Ladebedingung: simpletweaks.",
        "details": [
          "Keine eigenen Survival-Items fuer den Handel. {'simpletweaks:claim_deed': 'Inert legacy/test item; no survival trade.'}"
        ]
      },
      "sources": [
        "modules/simplemoney/shared/resources/data/simplemoney/money/simpletweaks/prices.json"
      ],
      "related": []
    },
    {
      "id": "money_links_simplevisuals",
      "en": {
        "title": "Linked Price Table: simplevisuals",
        "summary": "0 item prices; loaded-mod condition: simplevisuals.",
        "details": [
          "No distinct survival items to trade. {}"
        ]
      },
      "de": {
        "title": "Verknuepfte Preisliste: simplevisuals",
        "summary": "0 Itempreise; Mod-Ladebedingung: simplevisuals.",
        "details": [
          "Keine eigenen Survival-Items fuer den Handel. {}"
        ]
      },
      "sources": [
        "modules/simplemoney/shared/resources/data/simplemoney/money/simplevisuals/prices.json"
      ],
      "related": []
    },
    {
      "id": "money_links_wiringexample",
      "en": {
        "title": "Linked Price Table: wiringexample",
        "summary": "0 item prices; loaded-mod condition: wiringexample.",
        "details": [
          "No distinct survival items to trade. {'wiringexample:token': 'Integration-only token; no survival trade.'}"
        ]
      },
      "de": {
        "title": "Verknuepfte Preisliste: wiringexample",
        "summary": "0 Itempreise; Mod-Ladebedingung: wiringexample.",
        "details": [
          "Keine eigenen Survival-Items fuer den Handel. {'wiringexample:token': 'Integration-only token; no survival trade.'}"
        ]
      },
      "sources": [
        "modules/simplemoney/shared/resources/data/simplemoney/money/wiringexample/prices.json"
      ],
      "related": []
    }
  ],
  "recipes": [
    {
      "id": "simplemoney:blank_note_smithing",
      "type": "minecraft:smithing_transform",
      "category": null,
      "group": null,
      "result": {
        "id": "simplemoney:blank_note",
        "count": 1
      },
      "source": "modules/simplemoney/generated/resources/data/simplemoney/recipe/blank_note_smithing.json",
      "ingredients": [
        "minecraft:iron_ingot",
        "simplemoney:resin_fiber",
        "simplemoney:special_paper"
      ],
      "slots": {
        "template": [
          "minecraft:iron_ingot"
        ],
        "base": [
          "simplemoney:special_paper"
        ],
        "addition": [
          "simplemoney:resin_fiber"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:resin_clump",
            "count": 4
          },
          {
            "id": "minecraft:honeycomb",
            "count": 3
          },
          {
            "id": "minecraft:sugar_cane",
            "count": 2
          },
          {
            "id": "minecraft:iron_ingot",
            "count": 1.222
          },
          {
            "id": "minecraft:bone",
            "count": 0.333
          }
        ]
      }
    },
    {
      "id": "simplemoney:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplemoney:guide_book",
        "count": 1
      },
      "source": "modules/simplemoney/shared/resources/data/simplemoney/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:gold_nugget"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:gold_nugget"
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
            "id": "minecraft:gold_ingot",
            "count": 0.111
          }
        ]
      }
    },
    {
      "id": "simplemoney:money_bill_from_blasting",
      "type": "minecraft:blasting",
      "category": null,
      "group": null,
      "result": {
        "id": "simplemoney:money_bill",
        "count": 1
      },
      "source": "modules/simplemoney/generated/resources/data/simplemoney/recipe/money_bill_from_blasting.json",
      "ingredients": [
        "simplemoney:raw_bill"
      ],
      "slots": {
        "ingredient": [
          "simplemoney:raw_bill"
        ]
      },
      "cookingtime": 24000,
      "experience": 20,
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:resin_clump",
            "count": 4
          },
          {
            "id": "minecraft:honeycomb",
            "count": 3
          },
          {
            "id": "minecraft:amethyst_shard",
            "count": 2
          },
          {
            "id": "minecraft:diamond",
            "count": 2
          },
          {
            "id": "minecraft:sugar_cane",
            "count": 2
          },
          {
            "id": "minecraft:gold_ingot",
            "count": 1.333
          },
          {
            "id": "minecraft:iron_ingot",
            "count": 1.222
          },
          {
            "id": "minecraft:cactus",
            "count": 1
          },
          {
            "id": "minecraft:ink_sac",
            "count": 1
          },
          {
            "id": "minecraft:bone",
            "count": 0.333
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.222
          }
        ]
      }
    },
    {
      "id": "simplemoney:raw_bill_from_crafting_table",
      "type": "minecraft:crafting_shaped",
      "category": null,
      "group": null,
      "result": {
        "id": "simplemoney:raw_bill",
        "count": 3
      },
      "source": "modules/simplemoney/generated/resources/data/simplemoney/recipe/raw_bill_from_crafting_table.json",
      "ingredients": [
        "minecraft:green_dye",
        "minecraft:ink_sac",
        "simplemoney:refined_blank_note"
      ],
      "pattern": [
        "#I#",
        "PPP",
        "I#I"
      ],
      "key": {
        "#": [
          "minecraft:green_dye"
        ],
        "I": [
          "minecraft:ink_sac"
        ],
        "P": [
          "simplemoney:refined_blank_note"
        ]
      },
      "baseMaterials": {
        "yield": 3,
        "materials": [
          {
            "id": "minecraft:resin_clump",
            "count": 12
          },
          {
            "id": "minecraft:honeycomb",
            "count": 9
          },
          {
            "id": "minecraft:amethyst_shard",
            "count": 6
          },
          {
            "id": "minecraft:diamond",
            "count": 6
          },
          {
            "id": "minecraft:sugar_cane",
            "count": 6
          },
          {
            "id": "minecraft:gold_ingot",
            "count": 4
          },
          {
            "id": "minecraft:iron_ingot",
            "count": 3.667
          },
          {
            "id": "minecraft:cactus",
            "count": 3
          },
          {
            "id": "minecraft:ink_sac",
            "count": 3
          },
          {
            "id": "minecraft:bone",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.667
          }
        ]
      }
    },
    {
      "id": "simplemoney:refined_bank_note_blank_smithing",
      "type": "minecraft:smithing_transform",
      "category": null,
      "group": null,
      "result": {
        "id": "simplemoney:refined_blank_note",
        "count": 1
      },
      "source": "modules/simplemoney/generated/resources/data/simplemoney/recipe/refined_bank_note_blank_smithing.json",
      "ingredients": [
        "minecraft:gold_ingot",
        "simplemoney:blank_note",
        "simplemoney:special_fiber"
      ],
      "slots": {
        "template": [
          "minecraft:gold_ingot"
        ],
        "base": [
          "simplemoney:blank_note"
        ],
        "addition": [
          "simplemoney:special_fiber"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:resin_clump",
            "count": 4
          },
          {
            "id": "minecraft:honeycomb",
            "count": 3
          },
          {
            "id": "minecraft:amethyst_shard",
            "count": 2
          },
          {
            "id": "minecraft:diamond",
            "count": 2
          },
          {
            "id": "minecraft:sugar_cane",
            "count": 2
          },
          {
            "id": "minecraft:gold_ingot",
            "count": 1.333
          },
          {
            "id": "minecraft:iron_ingot",
            "count": 1.222
          },
          {
            "id": "minecraft:bone",
            "count": 0.333
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.222
          }
        ]
      }
    },
    {
      "id": "simplemoney:resin_fiber_from_crafting_table",
      "type": "minecraft:crafting_shaped",
      "category": null,
      "group": null,
      "result": {
        "id": "simplemoney:resin_fiber",
        "count": 1
      },
      "source": "modules/simplemoney/generated/resources/data/simplemoney/recipe/resin_fiber_from_crafting_table.json",
      "ingredients": [
        "minecraft:bone_meal",
        "minecraft:honeycomb",
        "minecraft:iron_nugget",
        "minecraft:resin_clump"
      ],
      "pattern": [
        "ERH",
        "RBR",
        "HRE"
      ],
      "key": {
        "R": [
          "minecraft:resin_clump"
        ],
        "E": [
          "minecraft:iron_nugget"
        ],
        "B": [
          "minecraft:bone_meal"
        ],
        "H": [
          "minecraft:honeycomb"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:resin_clump",
            "count": 4
          },
          {
            "id": "minecraft:honeycomb",
            "count": 2
          },
          {
            "id": "minecraft:bone",
            "count": 0.333
          },
          {
            "id": "minecraft:iron_ingot",
            "count": 0.222
          }
        ]
      }
    },
    {
      "id": "simplemoney:rocket_from_paper",
      "type": "minecraft:crafting_shaped",
      "category": null,
      "group": null,
      "result": {
        "id": "minecraft:firework_rocket",
        "count": 1
      },
      "source": "modules/simplemoney/generated/resources/data/simplemoney/recipe/rocket_from_paper.json",
      "ingredients": [
        "minecraft:gunpowder",
        "minecraft:paper"
      ],
      "pattern": [
        "###",
        "#G#",
        "#G#"
      ],
      "key": {
        "#": [
          "minecraft:paper"
        ],
        "G": [
          "minecraft:gunpowder"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:sugar_cane",
            "count": 7
          },
          {
            "id": "minecraft:gunpowder",
            "count": 2
          }
        ]
      }
    },
    {
      "id": "simplemoney:special_fiber_from_crafting_table",
      "type": "minecraft:crafting_shaped",
      "category": null,
      "group": null,
      "result": {
        "id": "simplemoney:special_fiber",
        "count": 1
      },
      "source": "modules/simplemoney/generated/resources/data/simplemoney/recipe/special_fiber_from_crafting_table.json",
      "ingredients": [
        "minecraft:amethyst_shard",
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:gold_nugget"
      ],
      "pattern": [
        "CGA",
        "DAG",
        "GDC"
      ],
      "key": {
        "D": [
          "minecraft:diamond"
        ],
        "G": [
          "minecraft:gold_nugget"
        ],
        "C": [
          "minecraft:copper_nugget"
        ],
        "A": [
          "minecraft:amethyst_shard"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:amethyst_shard",
            "count": 2
          },
          {
            "id": "minecraft:diamond",
            "count": 2
          },
          {
            "id": "minecraft:gold_ingot",
            "count": 0.333
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.222
          }
        ]
      }
    },
    {
      "id": "simplemoney:special_paper_from_crafting_table",
      "type": "minecraft:crafting_shaped",
      "category": null,
      "group": null,
      "result": {
        "id": "simplemoney:special_paper",
        "count": 1
      },
      "source": "modules/simplemoney/generated/resources/data/simplemoney/recipe/special_paper_from_crafting_table.json",
      "ingredients": [
        "minecraft:honeycomb",
        "minecraft:paper"
      ],
      "pattern": [
        "#M#"
      ],
      "key": {
        "#": [
          "minecraft:paper"
        ],
        "M": [
          "minecraft:honeycomb"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:sugar_cane",
            "count": 2
          },
          {
            "id": "minecraft:honeycomb",
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
      "id": "simplemoney:blank_note",
      "name": {
        "en_us": "Banknote Blank",
        "de_de": "Banknotenrohling"
      },
      "note": {
        "en": {
          "title": "Banknote Blank",
          "summary": "An uncommon smithing ingredient made from special paper, resin fiber, and an iron ingot. Refine it with special fiber and a gold ingot.",
          "details": [
            "Registry ID: simplemoney:blank_note",
            "Stack limit: 64",
            "Recipe: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:iron_ingot\", \"base\": \"simplemoney:special_paper\", \"addition\": \"simplemoney:resin_fiber\", \"result\": {\"id\": \"simplemoney:blank_note\", \"count\": 1}}"
          ]
        },
        "de": {
          "title": "Banknotenrohling",
          "summary": "Eine ungew?hnliche Schmiedezutat aus Spezialpapier, Harzfaser und einem Eisenbarren. Mit Spezialfaser und einem Goldbarren veredeln.",
          "details": [
            "Registry-ID: simplemoney:blank_note",
            "Stapelgrenze: 64",
            "Rezept: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:iron_ingot\", \"base\": \"simplemoney:special_paper\", \"addition\": \"simplemoney:resin_fiber\", \"result\": {\"id\": \"simplemoney:blank_note\", \"count\": 1}}"
          ]
        },
        "sources": [
          "modules/simplemoney/generated/resources/data/simplemoney/recipe/blank_note_smithing.json",
          "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
        ]
      },
      "texture": "assets/textures/simplemoney/item/blank_note.png",
      "craftedBy": [
        "simplemoney:blank_note_smithing"
      ],
      "usedIn": [
        "simplemoney:refined_bank_note_blank_smithing"
      ]
    },
    {
      "id": "simplemoney:guide_book",
      "name": {
        "en_us": "Simple Money Guide",
        "de_de": "Simple-Money-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simplemoney/shared/java/com/simplemoney/guide/MoneyGuide.java",
          "modules/simplemoney/shared/resources/data/simplemoney/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 13 pages taken from this wiki, shown in your language. Shapeless recipe: book + gold nugget. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Money\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 13 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Goldklumpen. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Money\" eins."
        }
      },
      "texture": "assets/textures/simplemoney/item/guide_book.png",
      "craftedBy": [
        "simplemoney:guide_book"
      ],
      "usedIn": []
    },
    {
      "id": "simplemoney:money_bill",
      "name": {
        "en_us": "Money Bill",
        "de_de": "Geldschein"
      },
      "note": {
        "en": {
          "title": "Money Bill",
          "summary": "Currency for the added villager and wandering trader offers. Epic, fire resistant, and always glinting; using it plays a page-turn sound and emits a happy-villager particle without consuming it.",
          "details": [
            "Registry ID: simplemoney:money_bill",
            "Stack limit: 64",
            "Recipe: {\"type\": \"minecraft:blasting\", \"ingredient\": \"simplemoney:raw_bill\", \"result\": {\"id\": \"simplemoney:money_bill\"}, \"experience\": 20, \"cookingtime\": 24000}",
            "Epic rarity, fire resistant, permanent enchantment glint. Right-click plays a page-turn sound and sends a happy-villager particle; the bill is not consumed."
          ]
        },
        "de": {
          "title": "Geldschein",
          "summary": "W?hrung f?r die zus?tzlichen Dorfbewohner- und fahrenden H?ndlerangebote. Episch, feuerfest und dauerhaft gl?nzend; Benutzung erzeugt einen Umbl?tterklang und ein Dorfbewohnerpartikel ohne Verbrauch.",
          "details": [
            "Registry-ID: simplemoney:money_bill",
            "Stapelgrenze: 64",
            "Rezept: {\"type\": \"minecraft:blasting\", \"ingredient\": \"simplemoney:raw_bill\", \"result\": {\"id\": \"simplemoney:money_bill\"}, \"experience\": 20, \"cookingtime\": 24000}",
            "Episch, feuerfest, dauerhafter Verzauberungsglanz. Rechtsklick erzeugt einen Umblätterklang und ein Dorfbewohnerpartikel; der Schein bleibt erhalten."
          ]
        },
        "sources": [
          "modules/simplemoney/generated/resources/data/simplemoney/recipe/money_bill_from_blasting.json",
          "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
        ]
      },
      "texture": "assets/textures/simplemoney/item/money_bill.png",
      "craftedBy": [
        "simplemoney:money_bill_from_blasting"
      ],
      "usedIn": [],
      "value": {
        "emeralds": {
          "min": 3,
          "max": 35
        },
        "trades": 4,
        "sources": [
          "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/cleric/08.json",
          "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/farmer/18.json",
          "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/fletcher/00.json",
          "modules/simplemoney/generated/resources/data/simplemoney/villager_trade/mason/10.json"
        ]
      }
    },
    {
      "id": "simplemoney:raw_bill",
      "name": {
        "en_us": "Raw Bill",
        "de_de": "Roher Geldschein"
      },
      "note": {
        "en": {
          "title": "Raw Bill",
          "summary": "A rare crafting result. Blast it for 24,000 ticks (one in-game day in a vanilla blast furnace; SimpleBuilding blast furnaces are 2, 4 or 8 times faster) to produce one money bill and 20 experience points.",
          "details": [
            "Registry ID: simplemoney:raw_bill",
            "Stack limit: 64",
            "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#I#\", \"PPP\", \"I#I\"], \"key\": {\"#\": \"minecraft:green_dye\", \"I\": \"minecraft:ink_sac\", \"P\": \"simplemoney:refined_blank_note\"}, \"result\": {\"id\": \"simplemoney:raw_bill\", \"count\": 3}}"
          ]
        },
        "de": {
          "title": "Roher Geldschein",
          "summary": "Ein seltenes Herstellungsergebnis. Im Schmelzofen in 24.000 Ticks (ein Spieltag im Vanilla-Schmelzofen; SimpleBuildings Schmelzöfen sind 2-, 4- oder 8-mal so schnell) zu einem Geldschein verarbeiten; ergibt 20 Erfahrungspunkte.",
          "details": [
            "Registry-ID: simplemoney:raw_bill",
            "Stapelgrenze: 64",
            "Rezept: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#I#\", \"PPP\", \"I#I\"], \"key\": {\"#\": \"minecraft:green_dye\", \"I\": \"minecraft:ink_sac\", \"P\": \"simplemoney:refined_blank_note\"}, \"result\": {\"id\": \"simplemoney:raw_bill\", \"count\": 3}}"
          ]
        },
        "sources": [
          "modules/simplemoney/generated/resources/data/simplemoney/recipe/raw_bill_from_crafting_table.json",
          "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
        ]
      },
      "texture": "assets/textures/simplemoney/item/raw_bill.png",
      "craftedBy": [
        "simplemoney:raw_bill_from_crafting_table"
      ],
      "usedIn": [
        "simplemoney:money_bill_from_blasting"
      ]
    },
    {
      "id": "simplemoney:refined_blank_note",
      "name": {
        "en_us": "Refined Banknote Blank",
        "de_de": "Veredelter Banknotenrohling"
      },
      "note": {
        "en": {
          "title": "Refined Banknote Blank",
          "summary": "An uncommon ingredient made by refining a banknote blank. Three blanks, green dye, and ink sacs produce three raw bills.",
          "details": [
            "Registry ID: simplemoney:refined_blank_note",
            "Stack limit: 64",
            "Recipe: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:gold_ingot\", \"base\": \"simplemoney:blank_note\", \"addition\": \"simplemoney:special_fiber\", \"result\": {\"id\": \"simplemoney:refined_blank_note\", \"count\": 1}}"
          ]
        },
        "de": {
          "title": "Veredelter Banknotenrohling",
          "summary": "Eine ungew?hnliche Zutat aus einem veredelten Banknotenrohling. Drei Rohlinge, gr?ner Farbstoff und Tintenbeutel ergeben drei rohe Geldscheine.",
          "details": [
            "Registry-ID: simplemoney:refined_blank_note",
            "Stapelgrenze: 64",
            "Rezept: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:gold_ingot\", \"base\": \"simplemoney:blank_note\", \"addition\": \"simplemoney:special_fiber\", \"result\": {\"id\": \"simplemoney:refined_blank_note\", \"count\": 1}}"
          ]
        },
        "sources": [
          "modules/simplemoney/generated/resources/data/simplemoney/recipe/refined_bank_note_blank_smithing.json",
          "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
        ]
      },
      "texture": "assets/textures/simplemoney/item/refined_blank_note.png",
      "craftedBy": [
        "simplemoney:refined_bank_note_blank_smithing"
      ],
      "usedIn": [
        "simplemoney:raw_bill_from_crafting_table"
      ]
    },
    {
      "id": "simplemoney:resin_fiber",
      "name": {
        "en_us": "Resin Fiber",
        "de_de": "Harzfaser"
      },
      "note": {
        "en": {
          "title": "Resin Fiber",
          "summary": "Crafted from iron nuggets, resin clumps, honeycomb, and bone meal; used to smith banknote blanks.",
          "details": [
            "Registry ID: simplemoney:resin_fiber",
            "Stack limit: 16",
            "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"ERH\", \"RBR\", \"HRE\"], \"key\": {\"R\": \"minecraft:resin_clump\", \"E\": \"minecraft:iron_nugget\", \"B\": \"minecraft:bone_meal\", \"H\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:resin_fiber\", \"count\": 1}}"
          ]
        },
        "de": {
          "title": "Harzfaser",
          "summary": "Aus Eisennuggets, Harzklumpen, Honigwaben und Knochenmehl hergestellt; dient zum Schmieden von Banknotenrohlingen.",
          "details": [
            "Registry-ID: simplemoney:resin_fiber",
            "Stapelgrenze: 16",
            "Rezept: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"ERH\", \"RBR\", \"HRE\"], \"key\": {\"R\": \"minecraft:resin_clump\", \"E\": \"minecraft:iron_nugget\", \"B\": \"minecraft:bone_meal\", \"H\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:resin_fiber\", \"count\": 1}}"
          ]
        },
        "sources": [
          "modules/simplemoney/generated/resources/data/simplemoney/recipe/resin_fiber_from_crafting_table.json",
          "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
        ]
      },
      "texture": "assets/textures/simplemoney/item/resin_fiber.png",
      "craftedBy": [
        "simplemoney:resin_fiber_from_crafting_table"
      ],
      "usedIn": [
        "simplemoney:blank_note_smithing"
      ]
    },
    {
      "id": "simplemoney:special_fiber",
      "name": {
        "en_us": "Special Fiber",
        "de_de": "Spezialfaser"
      },
      "note": {
        "en": {
          "title": "Special Fiber",
          "summary": "Crafted from copper nuggets, gold nuggets, amethyst shards, and diamonds; used with a gold ingot to refine a banknote blank.",
          "details": [
            "Registry ID: simplemoney:special_fiber",
            "Stack limit: 16",
            "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"CGA\", \"DAG\", \"GDC\"], \"key\": {\"D\": \"minecraft:diamond\", \"G\": \"minecraft:gold_nugget\", \"C\": \"minecraft:copper_nugget\", \"A\": \"minecraft:amethyst_shard\"}, \"result\": {\"id\": \"simplemoney:special_fiber\", \"count\": 1}}"
          ]
        },
        "de": {
          "title": "Spezialfaser",
          "summary": "Aus Kupfernuggets, Goldnuggets, Amethystscherben und Diamanten hergestellt; veredelt mit einem Goldbarren einen Banknotenrohling.",
          "details": [
            "Registry-ID: simplemoney:special_fiber",
            "Stapelgrenze: 16",
            "Rezept: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"CGA\", \"DAG\", \"GDC\"], \"key\": {\"D\": \"minecraft:diamond\", \"G\": \"minecraft:gold_nugget\", \"C\": \"minecraft:copper_nugget\", \"A\": \"minecraft:amethyst_shard\"}, \"result\": {\"id\": \"simplemoney:special_fiber\", \"count\": 1}}"
          ]
        },
        "sources": [
          "modules/simplemoney/generated/resources/data/simplemoney/recipe/special_fiber_from_crafting_table.json",
          "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
        ]
      },
      "texture": "assets/textures/simplemoney/item/special_fiber.png",
      "craftedBy": [
        "simplemoney:special_fiber_from_crafting_table"
      ],
      "usedIn": [
        "simplemoney:refined_bank_note_blank_smithing"
      ]
    },
    {
      "id": "simplemoney:special_paper",
      "name": {
        "en_us": "Special Paper",
        "de_de": "Spezialpapier"
      },
      "note": {
        "en": {
          "title": "Special Paper",
          "summary": "Crafted from two paper and one honeycomb; used with an iron ingot and resin fiber to smith a banknote blank.",
          "details": [
            "Registry ID: simplemoney:special_paper",
            "Stack limit: 64",
            "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#M#\"], \"key\": {\"#\": \"minecraft:paper\", \"M\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:special_paper\", \"count\": 1}}"
          ]
        },
        "de": {
          "title": "Spezialpapier",
          "summary": "Aus zwei Papier und einer Honigwabe hergestellt; wird mit einem Eisenbarren und Harzfaser zu einem Banknotenrohling geschmiedet.",
          "details": [
            "Registry-ID: simplemoney:special_paper",
            "Stapelgrenze: 64",
            "Rezept: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#M#\"], \"key\": {\"#\": \"minecraft:paper\", \"M\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:special_paper\", \"count\": 1}}"
          ]
        },
        "sources": [
          "modules/simplemoney/generated/resources/data/simplemoney/recipe/special_paper_from_crafting_table.json",
          "modules/simplemoney/shared/java/com/simplemoney/MoneyItems.java"
        ]
      },
      "texture": "assets/textures/simplemoney/item/special_paper.png",
      "craftedBy": [
        "simplemoney:special_paper_from_crafting_table"
      ],
      "usedIn": [
        "simplemoney:blank_note_smithing"
      ]
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
    "features": 68,
    "recipes": 9,
    "lootTables": 0,
    "tags": 0,
    "advancements": 0,
    "enchantments": 0,
    "items": 8,
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
