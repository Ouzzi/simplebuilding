window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["wiringexample"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "wiringexample",
    "name": "Wiring Example",
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
      "id": "welcome",
      "sources": [
        "modules/wiringexample/shared/java/com/simplebuilding/modules/wiringexample/ExampleItems.java",
        "integration/src/main/java/com/simplebuilding/integration/CrossModGameTest.java",
        "modules/wiringexample/shared/resources/assets/wiringexample/items/token.json"
      ],
      "related": [
        "wiringexample:token",
        "simplebuilding:reinforced_hopper"
      ],
      "en": {
        "title": "Wiring Example",
        "summary": "A minimal example module registers a plain token. The integration harness checks storing and removing it in a SimpleBuilding reinforced hopper."
      },
      "de": {
        "title": "Wiring Example",
        "summary": "Ein minimales Beispielmodul registriert einen einfachen Token. Der Integrationsharness prueft das Einlagern und Entnehmen im verstaerkten SimpleBuilding-Trichter."
      }
    }
  ],
  "recipes": [],
  "lootTables": [],
  "tags": [],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "wiringexample:token",
      "name": {
        "en_us": "Integration Token",
        "de_de": "Integrationstest-Marke"
      },
      "note": {
        "en": {
          "summary": "A plain item used by the integration test. It has no custom behavior or creative tab and uses the Vanilla paper model."
        },
        "de": {
          "summary": "Ein einfaches Item fuer den Integrationstest. Es hat kein eigenes Verhalten oder Kreativtab und verwendet das Vanilla-Papiermodell."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "A token that exists to prove that two mods can talk."
          ],
          "use": [
            "An example item of the integration test module. It has no real use in survival."
          ],
          "secret": [
            "If you hold one, you are looking at the test bench."
          ]
        },
        "de": {
          "lore": [
            "Eine Marke, die nur beweist, dass zwei Mods miteinander sprechen können."
          ],
          "use": [
            "Ein Beispielitem des Integrationstestmoduls. Im Überleben hat es keinen echten Nutzen."
          ],
          "secret": [
            "Wer eines hält, sieht die Werkbank der Tests."
          ]
        }
      },
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
    "features": 1,
    "recipes": 0,
    "lootTables": 0,
    "tags": 0,
    "advancements": 0,
    "enchantments": 0,
    "items": 1,
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
