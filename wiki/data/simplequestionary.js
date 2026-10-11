window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplequestionary"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplequestionary",
    "name": "Simple Questionary",
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
      "id": "questionary_table",
      "sources": [
        "modules/simplequestionary/shared/java/com/simplequestionary/QuestionaryTableBlock.java",
        "modules/simplequestionary/shared/java/com/simplequestionary/QuestionaryMenu.java",
        "modules/simplequestionary/shared/java/com/simplequestionary/client/QuestionaryScreen.java",
        "modules/simplequestionary/shared/resources/data/simplequestionary/recipe/questionary_table.json"
      ],
      "en": {
        "title": "Questionary Table",
        "summary": "Put an item into the slot on the left: the open book on the right tells its lore, its use and a secret.",
        "details": [
          "Recipe (shaped): lapis, book, lapis / three planks / plank, gap, plank - 2 lapis lazuli, 1 book, 5 planks of any wood. Unlocked when you carry a book.",
          "The slot holds one piece; shift-click puts one in, and the item comes back when the table is closed.",
          "Three pages - 1 Lore, 2 Use, 3 Secret - turned with the bookmarks on the book's edge, the page arrows, the mouse wheel, A/D or the arrow keys. The secret is visible at once.",
          "The Use page ends with a line the server reads from the recipes: what the item is made from and how many recipes make and use it (config autoFacts in simplequestionary.json).",
          "Bundled in SimpleBuilding, the switch \"Enable Simple Questionary\" turns the table off after a restart."
        ]
      },
      "de": {
        "title": "Fragentisch",
        "summary": "Leg links einen Gegenstand ins Feld: das aufgeschlagene Buch rechts erzählt seine Überlieferung, seinen Nutzen und ein Geheimnis.",
        "details": [
          "Rezept (geformt): Lapis, Buch, Lapis / drei Bretter / Brett, Lücke, Brett - 2 Lapislazuli, 1 Buch, 5 Bretter beliebigen Holzes. Freigeschaltet, sobald du ein Buch trägst.",
          "Das Feld nimmt ein Stück; Shift-Klick legt eins hinein, beim Schließen kommt der Gegenstand zurück.",
          "Drei Seiten - 1 Überlieferung, 2 Nutzen, 3 Geheimnis - blätterst du mit den Lesezeichen am Buchrand, den Seitenpfeilen, dem Mausrad, A/D oder den Pfeiltasten. Das Geheimnis ist sofort sichtbar.",
          "Die Nutzen-Seite endet mit einer Zeile, die der Server aus den Rezepten liest: woraus der Gegenstand entsteht und wie viele Rezepte ihn herstellen und nutzen (Einstellung autoFacts in simplequestionary.json).",
          "In SimpleBuilding gebündelt schaltet „Simple Questionary aktivieren“ den Tisch nach einem Neustart ab."
        ]
      }
    },
    {
      "id": "questionary_texts",
      "sources": [
        "modules/simplequestionary/shared/java/com/simplequestionary/QuestionaryData.java",
        "modules/simplequestionary/shared/resources/assets/simplequestionary/questionary/en_us.json",
        "tools/questionary/check_data.py"
      ],
      "en": {
        "title": "Questionary texts",
        "summary": "Every mod describes its own items in assets/<namespace>/questionary/<language>.json; resource packs can add or replace entries.",
        "details": [
          "An entry has the pages lore, use and secret, each a list of paragraphs. Placeholders: {name}, {mod}, {item:namespace:id}.",
          "Families (\"family:<name>\" with items by tag \"#ns:tag\" or \"regex:<path>\") describe many items at once; an item's own entry wins.",
          "Items without a text get one of 8 fallback variants per page and item class (block, tool, armor, food, other), chosen by a stable hash of the item id - the same in every world.",
          "tools/questionary/check_data.py reports which items still lack a text; the coverage game test counts the same."
        ]
      },
      "de": {
        "title": "Fragentisch-Texte",
        "summary": "Jede Mod beschreibt ihre eigenen Gegenstände in assets/<Namensraum>/questionary/<Sprache>.json; Ressourcenpakete können Einträge ergänzen oder ersetzen.",
        "details": [
          "Ein Eintrag hat die Seiten lore, use und secret, je eine Liste von Absätzen. Platzhalter: {name}, {mod}, {item:Namensraum:ID}.",
          "Familien („family:<Name>“ mit Gegenständen per Tag „#ns:tag“ oder „regex:<Pfad>“) beschreiben viele Gegenstände auf einmal; ein eigener Eintrag gewinnt.",
          "Gegenstände ohne Text bekommen eine von 8 Ersatz-Varianten je Seite und Klasse (Block, Werkzeug, Rüstung, Essen, Sonstiges), gewählt über einen stabilen Hash der ID - in jeder Welt dieselbe.",
          "tools/questionary/check_data.py meldet, welchen Gegenständen noch Text fehlt; der Abdeckungs-Spieltest zählt dasselbe."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simplequestionary:questionary_table",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplequestionary:questionary_table",
        "count": 1
      },
      "source": "modules/simplequestionary/shared/resources/data/simplequestionary/recipe/questionary_table.json",
      "ingredients": [
        "#minecraft:planks",
        "minecraft:book",
        "minecraft:lapis_lazuli"
      ],
      "pattern": [
        "LBL",
        "PPP",
        "P P"
      ],
      "key": {
        "B": [
          "minecraft:book"
        ],
        "L": [
          "minecraft:lapis_lazuli"
        ],
        "P": [
          "#minecraft:planks"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:sugar_cane",
            "count": 3
          },
          {
            "id": "minecraft:lapis_lazuli",
            "count": 2
          },
          {
            "id": "minecraft:oak_log",
            "count": 1.25
          },
          {
            "id": "minecraft:leather",
            "count": 1
          }
        ],
        "tagExamples": [
          "#minecraft:planks"
        ]
      }
    }
  ],
  "lootTables": [
    {
      "id": "simplequestionary:blocks/questionary_table",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplequestionary:questionary_table"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplequestionary/shared/resources/data/simplequestionary/loot_table/blocks/questionary_table.json"
    }
  ],
  "tags": [],
  "advancements": [],
  "enchantments": [],
  "items": [],
  "blocks": [
    {
      "id": "simplequestionary:questionary_table",
      "name": {
        "en_us": "Questionary Table",
        "de_de": "Fragentisch"
      },
      "note": {
        "sources": [
          "modules/simplequestionary/shared/java/com/simplequestionary/QuestionaryTableBlock.java",
          "modules/simplequestionary/shared/resources/data/simplequestionary/recipe/questionary_table.json"
        ],
        "en": {
          "summary": "Low oak table with an open book and a lapis inkwell; opens the questionary book. Shaped: 2 lapis lazuli, 1 book, 5 planks."
        },
        "de": {
          "summary": "Niedriger Eichentisch mit aufgeschlagenem Buch und Lapis-Tintenfass; öffnet das Fragebuch. Geformt: 2 Lapislazuli, 1 Buch, 5 Bretter."
        }
      },
      "craftedBy": [
        "simplequestionary:questionary_table"
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
    "features": 2,
    "recipes": 1,
    "lootTables": 1,
    "tags": 0,
    "advancements": 0,
    "enchantments": 0,
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
