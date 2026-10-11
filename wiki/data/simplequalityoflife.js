window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplequalityoflife"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplequalityoflife",
    "name": "Simple Quality of Life",
    "version": "1.0.6",
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
      "id": "weather",
      "sources": [
        "modules/simpleweather/shared/java/com/simplebuilding/modules/simpleweather/mixin/ServerWeatherMixin.java"
      ],
      "en": {
        "title": "Weather controls (Simple Weather)",
        "summary": "Moved into the bundled sub-mod Simple Weather (simpleweather): disableWeather (default false) clears rain and thunder; clientRainParticleDensity (default 20, 0–100) is a local visual preference. Old settings are taken over once; \"Enable Simple Weather\" switches it off."
      },
      "de": {
        "title": "Wettersteuerung (Simple Weather)",
        "summary": "In die gebuendelte Sub-Mod Simple Weather (simpleweather) umgezogen: disableWeather (Standard false) loescht Regen und Gewitter; clientRainParticleDensity (Standard 20, 0–100) ist eine lokale Darstellungseinstellung. Alte Einstellungen werden einmalig uebernommen; \"Simple Weather aktivieren\" schaltet sie ab."
      }
    },
    {
      "id": "config_enableSimpleWeather",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java"
      ],
      "en": {
        "title": "enableSimpleWeather",
        "summary": "Super-mod switch for the bundled sub-mod Simple Weather. Default: true."
      },
      "de": {
        "title": "enableSimpleWeather",
        "summary": "Super-Mod-Schalter fuer die gebuendelte Sub-Mod Simple Weather. Standard: true."
      }
    },
    {
      "id": "config_enableSimpleInterfaces",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java"
      ],
      "en": {
        "title": "enableSimpleInterfaces",
        "summary": "Super-mod switch for the bundled sub-mod Simple Interfaces (Simple style for Vanilla container screens). Default: true."
      },
      "de": {
        "title": "enableSimpleInterfaces",
        "summary": "Super-Mod-Schalter fuer die gebuendelte Sub-Mod Simple Interfaces (Simple-Stil fuer Vanilla-Behaelterfenster). Standard: true."
      }
    },
    {
      "id": "config_enableSimpleMovement",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java"
      ],
      "en": {
        "title": "enableSimpleMovement",
        "summary": "Super-mod switch for the bundled sub-mod Simple Movement. Default: true."
      },
      "de": {
        "title": "enableSimpleMovement",
        "summary": "Super-Mod-Schalter fuer die gebuendelte Sub-Mod Simple Movement. Standard: true."
      }
    },
    {
      "id": "config_enableSimpleTools",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java"
      ],
      "en": {
        "title": "enableSimpleTools",
        "summary": "Super-mod switch for the bundled sub-mod Simple Tools. Default: true."
      },
      "de": {
        "title": "enableSimpleTools",
        "summary": "Super-Mod-Schalter fuer die gebuendelte Sub-Mod Simple Tools. Standard: true."
      }
    },
    {
      "id": "config_enableSimpleFarming",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java"
      ],
      "en": {
        "title": "enableSimpleFarming",
        "summary": "Super-mod switch for the bundled sub-mod Simple Farming. Default: true."
      },
      "de": {
        "title": "enableSimpleFarming",
        "summary": "Super-Mod-Schalter fuer die gebuendelte Sub-Mod Simple Farming. Standard: true."
      }
    },
    {
      "id": "config_enableSimpleCreatures",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java"
      ],
      "en": {
        "title": "enableSimpleCreatures",
        "summary": "Super-mod switch for the bundled sub-mod Simple Creatures. Default: true."
      },
      "de": {
        "title": "enableSimpleCreatures",
        "summary": "Super-Mod-Schalter fuer die gebuendelte Sub-Mod Simple Creatures. Standard: true."
      }
    },
    {
      "id": "config_enableSimpleLoot",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java"
      ],
      "en": {
        "title": "enableSimpleLoot",
        "summary": "Super-mod switch for the bundled sub-mod Simple Loot. Default: true."
      },
      "de": {
        "title": "enableSimpleLoot",
        "summary": "Super-Mod-Schalter fuer die gebuendelte Sub-Mod Simple Loot. Standard: true."
      }
    },
    {
      "id": "config_qOL_enableLinkedContainers",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java"
      ],
      "en": {
        "title": "qOL.enableLinkedContainers",
        "summary": "Server switch for linked containers. Default: true."
      },
      "de": {
        "title": "qOL.enableLinkedContainers",
        "summary": "Serverschalter fuer verknuepfte Behaelter. Standard: true."
      }
    },
    {
      "id": "config_qOL_linkedContainerRange",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java"
      ],
      "en": {
        "title": "qOL.linkedContainerRange",
        "summary": "Server setting, range 8–128 blocks between player and marked container. Default: 64."
      },
      "de": {
        "title": "qOL.linkedContainerRange",
        "summary": "Servereinstellung, 8–128 Bloecke zwischen Spieler und vorgemerktem Behaelter. Standard: 64."
      }
    },
    {
      "id": "config_qOL_enableEasyShulkers",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java"
      ],
      "en": {
        "title": "qOL.enableEasyShulkers",
        "summary": "Server switch for opening shulker boxes from the inventory. Default: true."
      },
      "de": {
        "title": "qOL.enableEasyShulkers",
        "summary": "Serverschalter fuer Shulkerkisten aus dem Inventar. Standard: true."
      }
    },
    {
      "id": "config_qOL_enableEasyEnderChests",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java"
      ],
      "en": {
        "title": "qOL.enableEasyEnderChests",
        "summary": "Server switch for opening ender chests from the inventory. Default: true."
      },
      "de": {
        "title": "qOL.enableEasyEnderChests",
        "summary": "Serverschalter fuer Endertruhen aus dem Inventar. Standard: true."
      }
    },
    {
      "id": "commands",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/command/ModCommands.java"
      ],
      "en": {
        "title": "Commands",
        "summary": "Owner-level /simplequalityoflife submod <weather|interfaces|loot|creatures|farming|tools|movement> <true|false> switches a bundled sub-mod; the former tweak commands moved with their features into the sub-mods (/simpleloot, /simplecreatures, /simplefarming, /simpletools, /simplemovement). Changes are saved to config/simplequalityoflife.json."
      },
      "de": {
        "title": "Befehle",
        "summary": "Besitzerberechtigung: /simplequalityoflife submod <weather|interfaces|loot|creatures|farming|tools|movement> <true|false> schaltet eine gebuendelte Sub-Mod; die frueheren tweak-Befehle sind mit ihren Funktionen in die Sub-Mods gewandert (/simpleloot, /simplecreatures, /simplefarming, /simpletools, /simplemovement). Speichern in config/simplequalityoflife.json."
      }
    },
    {
      "id": "forge_support",
      "sources": [
        "modules/simplequalityoflife/forge/build.gradle",
        "modules/simplequalityoflife/forge/src/main/java/com/simplebuilding/modules/simplequalityoflife/forge/QolForge.java"
      ],
      "en": {
        "title": "Experimental Forge 26.3",
        "summary": "Opt-in Forge adapter for the same server-controlled utilities.",
        "details": [
          "Enable -Pforge263=true. Clientbound config and crawl channels, key bindings, break veto and farmland hooks are Forge-owned. JSON configuration retains canonical fields and bounds. Cloth GUI is unavailable. Forge clients and real-player networking remain unverified.",
          "The experimental Forge transport currently requires the module on both server and client."
        ]
      },
      "de": {
        "title": "Experimentelles Forge 26.3",
        "summary": "Opt-in-Forge-Adapter für dieselben servergesteuerten Hilfen.",
        "details": [
          "Mit -Pforge263=true aktivieren. Config-/Kriechpakete nur zum Client, Tasten, Abbau-Veto und Ackerschutz nutzen Forge-Hooks. JSON-Konfiguration behält Felder und Grenzen. Cloth-GUI fehlt. Forge-Clients und Netzwerk mit echten Spielern bleiben ungeprüft.",
          "Der experimentelle Forge-Transport benötigt das Modul derzeit auf Server und Client."
        ]
      }
    },
    {
      "id": "linked_containers",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/container/LinkedContainers.java",
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/client/LinkedPanel.java"
      ],
      "en": {
        "title": "Linked containers",
        "summary": "Sneak + right-click a container with an empty main hand to mark it (a double chest counts as one); dust particles at your hand show the mark. The next chest or GUI you open (crafting table, furnace, anvil …) shows the marked container above it, sharing your inventory. Shift-click moves marked → second GUI, else inventory; inventory → second GUI, else the marked container. The mark ends beyond qOL.linkedContainerRange (default 64, range 8–128), in another dimension, in an unloaded chunk, when the block is gone, on logout or death, or with another sneak + right-click on it. Claims and locks are checked. The recipe book still only counts your inventory."
      },
      "de": {
        "title": "Verknuepfte Behaelter",
        "summary": "Schleichen + Rechtsklick mit leerer Haupthand merkt einen Behaelter vor (Doppeltruhe zaehlt als einer); Staubpartikel an der Hand zeigen die Vormerkung. Die naechste geoeffnete Truhe oder GUI (Werkbank, Ofen, Amboss …) zeigt den Behaelter darueber, mit gemeinsamem Inventar. Shift-Klick: vorgemerkt → zweites GUI, sonst Inventar; Inventar → zweites GUI, sonst vorgemerkter Behaelter. Die Vormerkung endet jenseits von qOL.linkedContainerRange (Standard 64, Bereich 8–128), in anderer Dimension, in ungeladenem Chunk, wenn der Block weg ist, bei Logout oder Tod oder mit erneutem Schleich-Rechtsklick darauf. Claims und Schloesser werden geprueft. Das Rezeptbuch zaehlt weiter nur das Inventar."
      }
    },
    {
      "id": "easy_shulkers",
      "sources": [
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/container/PortableContainers.java",
        "modules/simplequalityoflife/shared/java/com/simplequalityoflife/container/PortableMenu.java"
      ],
      "en": {
        "title": "Easy shulker boxes and ender chests",
        "summary": "Right-click a shulker box (any color, also SimpleBuilding tier boxes) or an ender chest in the air, or right-click it on its own inventory slot, to open it without placing it. The item stays locked in its slot while open; every change is written into the box at once. Shulker boxes do not fit inside. Not from the creative inventory tab. Switches: qOL.enableEasyShulkers and qOL.enableEasyEnderChests (default true)."
      },
      "de": {
        "title": "Einfache Shulkerkisten und Endertruhen",
        "summary": "Rechtsklick mit einer Shulkerkiste (jede Farbe, auch SimpleBuilding-Stufenkisten) oder Endertruhe in die Luft oder auf ihren eigenen Inventarplatz oeffnet sie, ohne sie zu platzieren. Das Item bleibt waehrenddessen in seinem Platz gesperrt; jede Aenderung wird sofort in die Kiste geschrieben. Shulkerkisten passen nicht hinein. Nicht aus dem Kreativ-Inventarreiter. Schalter: qOL.enableEasyShulkers und qOL.enableEasyEnderChests (Standard true)."
      }
    }
  ],
  "recipes": [
    {
      "id": "simplequalityoflife:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplequalityoflife:guide_book",
        "count": 1
      },
      "source": "modules/simplequalityoflife/shared/resources/data/simplequalityoflife/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:chest"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:chest"
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
            "id": "minecraft:oak_log",
            "count": 2
          },
          {
            "id": "minecraft:leather",
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
      "id": "simplequalityoflife:guide_book",
      "name": {
        "en_us": "Simple Quality of Life Guide",
        "de_de": "Simple-Quality-of-Life-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simplequalityoflife/shared/java/com/simplequalityoflife/guide/QolGuide.java",
          "modules/simplequalityoflife/shared/resources/data/simplequalityoflife/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 48 pages taken from this wiki, shown in your language. Shapeless recipe: book + chest. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Quality of Life\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 48 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Truhe. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Quality of Life\" eins."
        }
      },
      "texture": "assets/textures/simplequalityoflife/item/guide_book.png",
      "craftedBy": [
        "simplequalityoflife:guide_book"
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
    "features": 16,
    "recipes": 1,
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
