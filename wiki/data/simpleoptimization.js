window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simpleoptimization"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simpleoptimization",
    "name": "Simple Optimization",
    "version": "0.1.0",
    "minecraftLines": [
      "26.3"
    ],
    "loaders": [
      "fabric"
    ]
  },
  "features": [
    {
      "id": "supermod",
      "sources": [
        "modules/simpleoptimization/shared/java/com/simplebuilding/modules/simpleoptimization/SimpleOptimization.java",
        "framework/src/main/java/com/simplebuilding/framework/api/SubMods.java"
      ],
      "en": {
        "title": "Super-mod for optimizations",
        "summary": "Simple Optimization has no content of its own: it bundles Simple Opt Render, Simple Opt Memory and Simple Opt Server. config/simpleoptimization.json holds one switch each (enableSimpleOptRender, enableSimpleOptMemory, enableSimpleOptServer, default true); off makes that sub-mod inert, its own settings stay in its own config. Every sub-mod also runs alone; a server only needs Simple Opt Server."
      },
      "de": {
        "title": "Super-Mod für Optimierungen",
        "summary": "Simple Optimization hat keine eigenen Inhalte: Es bündelt Simple Opt Render, Simple Opt Memory und Simple Opt Server. config/simpleoptimization.json hat je einen Schalter (enableSimpleOptRender, enableSimpleOptMemory, enableSimpleOptServer, Standard true); aus macht die Sub-Mod wirkungslos, ihre eigenen Einstellungen bleiben in ihrer Config. Jede Sub-Mod läuft auch allein; ein Server braucht nur Simple Opt Server."
      }
    },
    {
      "id": "commands",
      "sources": [
        "modules/simpleoptimization/fabric/src/main/java/com/simplebuilding/modules/simpleoptimization/OptimizationFabric.java"
      ],
      "en": {
        "title": "Commands",
        "summary": "/simpleoptimization config shows and changes every option of config/simpleoptimization.json (list [group], get, set, reset, help; Tab completes options and values). The file is read once while the game starts, so a change is saved at once but takes effect after a restart (each answer says so). Permission level 2.",
        "details": [
          "Example: /simpleoptimization config get <option> shows the value, the default and the type; /simpleoptimization config reset <option> brings the default back; a wrong type is refused and nothing is saved."
        ]
      },
      "de": {
        "title": "Befehle",
        "summary": "/simpleoptimization config zeigt und ändert jede Option von config/simpleoptimization.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte). Die Datei wird einmal beim Start gelesen: Eine Änderung wird sofort gespeichert, wirkt aber erst nach einem Neustart (jede Antwort sagt das). Berechtigung 2.",
        "details": [
          "Beispiel: /simpleoptimization config get <Option> zeigt Wert, Standard und Typ; /simpleoptimization config reset <Option> stellt den Standard wieder her; ein falscher Typ wird abgelehnt und nichts gespeichert."
        ]
      }
    }
  ],
  "recipes": [],
  "lootTables": [],
  "tags": [],
  "advancements": [],
  "enchantments": [],
  "items": [],
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
    "features": 2,
    "recipes": 0,
    "lootTables": 0,
    "tags": 0,
    "advancements": 0,
    "enchantments": 0,
    "items": 0,
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
