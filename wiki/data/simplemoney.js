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
      "neoforge"
    ]
  },
  "features": [
    {
      "id": "special_paper",
      "en": {
        "title": "Special Paper",
        "summary": "Paper made from exotic material.",
        "details": [
          "Registry ID: simplemoney:special_paper",
          "Stack limit: 64",
          "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#M#\"], \"key\": {\"#\": \"minecraft:paper\", \"M\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:special_paper\", \"count\": 1}}"
        ]
      },
      "de": {
        "title": "Spezialpapier",
        "summary": "Papier aus exotischem Material.",
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
        "summary": "Metal and amethyst fiber for refining banknote blanks.",
        "details": [
          "Registry ID: simplemoney:special_fiber",
          "Stack limit: 16",
          "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"CGA\", \"DAG\", \"GDC\"], \"key\": {\"D\": \"minecraft:diamond\", \"G\": \"minecraft:gold_nugget\", \"C\": \"minecraft:copper_nugget\", \"A\": \"minecraft:amethyst_shard\"}, \"result\": {\"id\": \"simplemoney:special_fiber\", \"count\": 1}}"
        ]
      },
      "de": {
        "title": "Spezialfaser",
        "summary": "Metall- und Amethystfaser zum Veredeln von Banknotenrohlingen.",
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
        "summary": "Sticky and durable fiber, ideal for crafting.",
        "details": [
          "Registry ID: simplemoney:resin_fiber",
          "Stack limit: 16",
          "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"ERH\", \"RBR\", \"HRE\"], \"key\": {\"R\": \"minecraft:resin_clump\", \"E\": \"minecraft:iron_nugget\", \"B\": \"minecraft:bone_meal\", \"H\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:resin_fiber\", \"count\": 1}}"
        ]
      },
      "de": {
        "title": "Harzfaser",
        "summary": "Klebrige und haltbare Faser zum Herstellen.",
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
        "summary": "Unprinted, high-quality paper.",
        "details": [
          "Registry ID: simplemoney:blank_note",
          "Stack limit: 64",
          "Recipe: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:iron_ingot\", \"base\": \"simplemoney:special_paper\", \"addition\": \"simplemoney:resin_fiber\", \"result\": {\"id\": \"simplemoney:blank_note\", \"count\": 1}}"
        ]
      },
      "de": {
        "title": "Banknotenrohling",
        "summary": "Unbedrucktes, hochwertiges Papier.",
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
        "summary": "Treated for durability and quality.",
        "details": [
          "Registry ID: simplemoney:refined_blank_note",
          "Stack limit: 64",
          "Recipe: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:gold_ingot\", \"base\": \"simplemoney:blank_note\", \"addition\": \"simplemoney:special_fiber\", \"result\": {\"id\": \"simplemoney:refined_blank_note\", \"count\": 1}}"
        ]
      },
      "de": {
        "title": "Veredelter Banknotenrohling",
        "summary": "Für Haltbarkeit und Qualität behandelt.",
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
        "summary": "Still damp from the press.",
        "details": [
          "Registry ID: simplemoney:raw_bill",
          "Stack limit: 64",
          "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#I#\", \"PPP\", \"I#I\"], \"key\": {\"#\": \"minecraft:green_dye\", \"I\": \"minecraft:ink_sac\", \"P\": \"simplemoney:refined_blank_note\"}, \"result\": {\"id\": \"simplemoney:raw_bill\", \"count\": 3}}"
        ]
      },
      "de": {
        "title": "Roher Geldschein",
        "summary": "Noch feucht von der Presse.",
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
        "summary": "Highest trading value.",
        "details": [
          "Registry ID: simplemoney:money_bill",
          "Stack limit: 64",
          "Recipe: {\"type\": \"minecraft:blasting\", \"ingredient\": \"simplemoney:raw_bill\", \"result\": {\"id\": \"simplemoney:money_bill\"}, \"experience\": 20, \"cookingtime\": 10000}",
          "Epic rarity, fire resistant, permanent enchantment glint. Right-click plays a page-turn sound and sends a happy-villager particle; the bill is not consumed."
        ]
      },
      "de": {
        "title": "Geldschein",
        "summary": "Höchster Handelswert.",
        "details": [
          "Registry-ID: simplemoney:money_bill",
          "Stapelgrenze: 64",
          "Rezept: {\"type\": \"minecraft:blasting\", \"ingredient\": \"simplemoney:raw_bill\", \"result\": {\"id\": \"simplemoney:money_bill\"}, \"experience\": 20, \"cookingtime\": 10000}",
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
      "cookingtime": 10000,
      "experience": 20
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
          "summary": "Unprinted, high-quality paper.",
          "details": [
            "Registry ID: simplemoney:blank_note",
            "Stack limit: 64",
            "Recipe: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:iron_ingot\", \"base\": \"simplemoney:special_paper\", \"addition\": \"simplemoney:resin_fiber\", \"result\": {\"id\": \"simplemoney:blank_note\", \"count\": 1}}"
          ]
        },
        "de": {
          "title": "Banknotenrohling",
          "summary": "Unbedrucktes, hochwertiges Papier.",
          "details": [
            "Registry-ID: simplemoney:blank_note",
            "Stapelgrenze: 64",
            "Rezept: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:iron_ingot\", \"base\": \"simplemoney:special_paper\", \"addition\": \"simplemoney:resin_fiber\", \"result\": {\"id\": \"simplemoney:blank_note\", \"count\": 1}}"
          ]
        }
      },
      "texture": "assets/textures/simplemoney/item/blank_note.png"
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
          "summary": "Highest trading value.",
          "details": [
            "Registry ID: simplemoney:money_bill",
            "Stack limit: 64",
            "Recipe: {\"type\": \"minecraft:blasting\", \"ingredient\": \"simplemoney:raw_bill\", \"result\": {\"id\": \"simplemoney:money_bill\"}, \"experience\": 20, \"cookingtime\": 10000}",
            "Epic rarity, fire resistant, permanent enchantment glint. Right-click plays a page-turn sound and sends a happy-villager particle; the bill is not consumed."
          ]
        },
        "de": {
          "title": "Geldschein",
          "summary": "Höchster Handelswert.",
          "details": [
            "Registry-ID: simplemoney:money_bill",
            "Stapelgrenze: 64",
            "Rezept: {\"type\": \"minecraft:blasting\", \"ingredient\": \"simplemoney:raw_bill\", \"result\": {\"id\": \"simplemoney:money_bill\"}, \"experience\": 20, \"cookingtime\": 10000}",
            "Episch, feuerfest, dauerhafter Verzauberungsglanz. Rechtsklick erzeugt einen Umblätterklang und ein Dorfbewohnerpartikel; der Schein bleibt erhalten."
          ]
        }
      },
      "texture": "assets/textures/simplemoney/item/money_bill.png"
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
          "summary": "Still damp from the press.",
          "details": [
            "Registry ID: simplemoney:raw_bill",
            "Stack limit: 64",
            "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#I#\", \"PPP\", \"I#I\"], \"key\": {\"#\": \"minecraft:green_dye\", \"I\": \"minecraft:ink_sac\", \"P\": \"simplemoney:refined_blank_note\"}, \"result\": {\"id\": \"simplemoney:raw_bill\", \"count\": 3}}"
          ]
        },
        "de": {
          "title": "Roher Geldschein",
          "summary": "Noch feucht von der Presse.",
          "details": [
            "Registry-ID: simplemoney:raw_bill",
            "Stapelgrenze: 64",
            "Rezept: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#I#\", \"PPP\", \"I#I\"], \"key\": {\"#\": \"minecraft:green_dye\", \"I\": \"minecraft:ink_sac\", \"P\": \"simplemoney:refined_blank_note\"}, \"result\": {\"id\": \"simplemoney:raw_bill\", \"count\": 3}}"
          ]
        }
      },
      "texture": "assets/textures/simplemoney/item/raw_bill.png"
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
          "summary": "Treated for durability and quality.",
          "details": [
            "Registry ID: simplemoney:refined_blank_note",
            "Stack limit: 64",
            "Recipe: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:gold_ingot\", \"base\": \"simplemoney:blank_note\", \"addition\": \"simplemoney:special_fiber\", \"result\": {\"id\": \"simplemoney:refined_blank_note\", \"count\": 1}}"
          ]
        },
        "de": {
          "title": "Veredelter Banknotenrohling",
          "summary": "Für Haltbarkeit und Qualität behandelt.",
          "details": [
            "Registry-ID: simplemoney:refined_blank_note",
            "Stapelgrenze: 64",
            "Rezept: {\"type\": \"minecraft:smithing_transform\", \"template\": \"minecraft:gold_ingot\", \"base\": \"simplemoney:blank_note\", \"addition\": \"simplemoney:special_fiber\", \"result\": {\"id\": \"simplemoney:refined_blank_note\", \"count\": 1}}"
          ]
        }
      },
      "texture": "assets/textures/simplemoney/item/refined_blank_note.png"
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
          "summary": "Sticky and durable fiber, ideal for crafting.",
          "details": [
            "Registry ID: simplemoney:resin_fiber",
            "Stack limit: 16",
            "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"ERH\", \"RBR\", \"HRE\"], \"key\": {\"R\": \"minecraft:resin_clump\", \"E\": \"minecraft:iron_nugget\", \"B\": \"minecraft:bone_meal\", \"H\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:resin_fiber\", \"count\": 1}}"
          ]
        },
        "de": {
          "title": "Harzfaser",
          "summary": "Klebrige und haltbare Faser zum Herstellen.",
          "details": [
            "Registry-ID: simplemoney:resin_fiber",
            "Stapelgrenze: 16",
            "Rezept: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"ERH\", \"RBR\", \"HRE\"], \"key\": {\"R\": \"minecraft:resin_clump\", \"E\": \"minecraft:iron_nugget\", \"B\": \"minecraft:bone_meal\", \"H\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:resin_fiber\", \"count\": 1}}"
          ]
        }
      },
      "texture": "assets/textures/simplemoney/item/resin_fiber.png"
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
          "summary": "Metal and amethyst fiber for refining banknote blanks.",
          "details": [
            "Registry ID: simplemoney:special_fiber",
            "Stack limit: 16",
            "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"CGA\", \"DAG\", \"GDC\"], \"key\": {\"D\": \"minecraft:diamond\", \"G\": \"minecraft:gold_nugget\", \"C\": \"minecraft:copper_nugget\", \"A\": \"minecraft:amethyst_shard\"}, \"result\": {\"id\": \"simplemoney:special_fiber\", \"count\": 1}}"
          ]
        },
        "de": {
          "title": "Spezialfaser",
          "summary": "Metall- und Amethystfaser zum Veredeln von Banknotenrohlingen.",
          "details": [
            "Registry-ID: simplemoney:special_fiber",
            "Stapelgrenze: 16",
            "Rezept: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"CGA\", \"DAG\", \"GDC\"], \"key\": {\"D\": \"minecraft:diamond\", \"G\": \"minecraft:gold_nugget\", \"C\": \"minecraft:copper_nugget\", \"A\": \"minecraft:amethyst_shard\"}, \"result\": {\"id\": \"simplemoney:special_fiber\", \"count\": 1}}"
          ]
        }
      },
      "texture": "assets/textures/simplemoney/item/special_fiber.png"
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
          "summary": "Paper made from exotic material.",
          "details": [
            "Registry ID: simplemoney:special_paper",
            "Stack limit: 64",
            "Recipe: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#M#\"], \"key\": {\"#\": \"minecraft:paper\", \"M\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:special_paper\", \"count\": 1}}"
          ]
        },
        "de": {
          "title": "Spezialpapier",
          "summary": "Papier aus exotischem Material.",
          "details": [
            "Registry-ID: simplemoney:special_paper",
            "Stapelgrenze: 64",
            "Rezept: {\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"#M#\"], \"key\": {\"#\": \"minecraft:paper\", \"M\": \"minecraft:honeycomb\"}, \"result\": {\"id\": \"simplemoney:special_paper\", \"count\": 1}}"
          ]
        }
      },
      "texture": "assets/textures/simplemoney/item/special_paper.png"
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
    "features": 57,
    "recipes": 8,
    "lootTables": 0,
    "tags": 0,
    "advancements": 0,
    "enchantments": 0,
    "items": 7,
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
