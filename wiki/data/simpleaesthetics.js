window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simpleaesthetics"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simpleaesthetics",
    "name": "Simple Aesthetics",
    "version": "0.9.0",
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
      "id": "supermod",
      "sources": [
        "modules/simpleaesthetics/shared/java/com/simplebuilding/modules/simpleaesthetics/SimpleAesthetics.java",
        "framework/src/main/java/com/simplebuilding/framework/api/SubMods.java"
      ],
      "en": {
        "title": "Super-mod for looks and sounds",
        "summary": "Simple Aesthetics has no content of its own: it bundles Simple Visuals, Simple Sounds and Simple Models. config/simpleaesthetics.json holds one switch each (enableSimpleVisuals, enableSimpleSounds, enableSimpleModels, default true); off makes that sub-mod inert, its own settings stay in its own config. Every sub-mod is also playable alone."
      },
      "de": {
        "title": "Super-Mod für Optik und Klang",
        "summary": "Simple Aesthetics hat keine eigenen Inhalte: Es buendelt Simple Visuals, Simple Sounds und Simple Models. config/simpleaesthetics.json hat je einen Schalter (enableSimpleVisuals, enableSimpleSounds, enableSimpleModels, Standard true); aus macht die Sub-Mod wirkungslos, ihre eigenen Einstellungen bleiben in ihrer eigenen Config. Jede Sub-Mod ist auch allein spielbar."
      }
    },
    {
      "id": "commands",
      "sources": [
        "modules/simpleaesthetics/fabric/src/main/java/com/simplebuilding/modules/simpleaesthetics/AestheticsFabric.java"
      ],
      "en": {
        "title": "Commands",
        "summary": "/simpleaesthetics config shows and changes every option of config/simpleaesthetics.json (list [group], get, set, reset, help; Tab completes options and values). The file is read once while the game starts, so a change is saved at once but takes effect after a restart (each answer says so). Permission level 2.",
        "details": [
          "Example: /simpleaesthetics config get <option> shows the value, the default and the type; /simpleaesthetics config reset <option> brings the default back; a wrong type is refused and nothing is saved."
        ]
      },
      "de": {
        "title": "Befehle",
        "summary": "/simpleaesthetics config zeigt und ändert jede Option von config/simpleaesthetics.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte). Die Datei wird einmal beim Start gelesen: Eine Änderung wird sofort gespeichert, wirkt aber erst nach einem Neustart (jede Antwort sagt das). Berechtigung 2.",
        "details": [
          "Beispiel: /simpleaesthetics config get <Option> zeigt Wert, Standard und Typ; /simpleaesthetics config reset <Option> stellt den Standard wieder her; ein falscher Typ wird abgelehnt und nichts gespeichert."
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
