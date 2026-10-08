window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplecontainers"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplecontainers",
    "name": "Simple Containers",
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
      "id": "style_storage",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/StorageStyles.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/BoxLayout.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/client/StyledScreens.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/mixin/StorageBackgroundMixin.java"
      ],
      "en": {
        "title": "Simple container style",
        "summary": "Chest, barrel, ender chest, shulker box, hopper and dispenser/dropper screens get a box in the block's colour around the container slots and a light box around your inventory, with sunk rounded slots. Client only; the server needs nothing.",
        "details": [
          "Chest, barrel, ender chest, shulker box, hopper and dispenser/dropper screens get a box in the block's colour around the container slots and a light box around your inventory, with sunk rounded slots. Client only; the server needs nothing.",
          "The colour comes from the screen title (chest, barrel, ender chest ...) and the block you looked at when opening it: copper chests show their weathering stage, shulker boxes their dye colour. Unknown or custom-named containers use oak.",
          "The boxes are computed from the slot positions; the title is drawn in the box's label colour, the inventory label is left out (it would sit on the divider). Screens of other mods and screens without enough room stay Vanilla."
        ]
      },
      "de": {
        "title": "Simple-Container-Stil",
        "summary": "Truhen-, Fass-, Endertruhen-, Shulkerkisten-, Trichter- und Werfer/Spender-Bildschirme bekommen einen Kasten in der Farbe des Blocks um die Container-Slots und einen hellen Kasten um das Inventar, mit eingelassenen, abgerundeten Slots. Nur clientseitig; der Server braucht nichts.",
        "details": [
          "Truhen-, Fass-, Endertruhen-, Shulkerkisten-, Trichter- und Werfer/Spender-Bildschirme bekommen einen Kasten in der Farbe des Blocks um die Container-Slots und einen hellen Kasten um das Inventar, mit eingelassenen, abgerundeten Slots. Nur clientseitig; der Server braucht nichts.",
          "Die Farbe kommt aus dem Bildschirmtitel (Truhe, Fass, Endertruhe ...) und dem Block, den du beim Öffnen angeschaut hast: Kupfertruhen zeigen ihre Verwitterungsstufe, Shulkerkisten ihre Farbe. Unbekannte oder umbenannte Container nehmen Eiche.",
          "Die Kästen werden aus den Slot-Positionen berechnet; der Titel steht in der Label-Farbe des Kastens, das Inventar-Label entfällt (es läge auf der Fuge). Bildschirme anderer Mods und Bildschirme ohne genug Platz bleiben Vanilla."
        ]
      }
    },
    {
      "id": "config_enabled",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/ContainersConfig.java"
      ],
      "en": {
        "title": "Simple style",
        "summary": "Draw container screens in the Simple style: a box in the block's colour and a light inventory box. Off = every screen exactly Vanilla. Client only. Default: on.",
        "details": [
          "Draw container screens in the Simple style: a box in the block's colour and a light inventory box. Off = every screen exactly Vanilla. Client only. Default: on."
        ]
      },
      "de": {
        "title": "Simple-Stil",
        "summary": "Container-Bildschirme im Simple-Stil zeichnen: ein Kasten in der Farbe des Blocks und ein heller Inventar-Kasten. Aus = jeder Bildschirm genau wie Vanilla. Nur clientseitig. Standard: an.",
        "details": [
          "Container-Bildschirme im Simple-Stil zeichnen: ein Kasten in der Farbe des Blocks und ein heller Inventar-Kasten. Aus = jeder Bildschirm genau wie Vanilla. Nur clientseitig. Standard: an."
        ]
      }
    },
    {
      "id": "config_screen_chest",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/ContainersConfig.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/StorageStyles.java"
      ],
      "en": {
        "title": "Chests, barrels, ender chests",
        "summary": "Generic chest screens (1-6 rows: chest, double chest, barrel, ender chest, copper chests, chest boats and minecarts) in the block's colour. Default: on.",
        "details": [
          "Generic chest screens (1-6 rows: chest, double chest, barrel, ender chest, copper chests, chest boats and minecarts) in the block's colour. Default: on."
        ]
      },
      "de": {
        "title": "Truhen, Fässer, Endertruhen",
        "summary": "Allgemeine Truhen-Bildschirme (1-6 Reihen: Truhe, Doppeltruhe, Fass, Endertruhe, Kupfertruhen, Truhenboote und -loren) in der Farbe des Blocks. Standard: an.",
        "details": [
          "Allgemeine Truhen-Bildschirme (1-6 Reihen: Truhe, Doppeltruhe, Fass, Endertruhe, Kupfertruhen, Truhenboote und -loren) in der Farbe des Blocks. Standard: an."
        ]
      }
    },
    {
      "id": "config_screen_shulker_box",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/ContainersConfig.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/StorageStyles.java"
      ],
      "en": {
        "title": "Shulker boxes",
        "summary": "Shulker box screen in the box's dye colour (purple without dye). Default: on.",
        "details": [
          "Shulker box screen in the box's dye colour (purple without dye). Default: on."
        ]
      },
      "de": {
        "title": "Shulkerkisten",
        "summary": "Shulkerkisten-Bildschirm in der Farbe der Kiste (ungefärbt violett). Standard: an.",
        "details": [
          "Shulkerkisten-Bildschirm in der Farbe der Kiste (ungefärbt violett). Standard: an."
        ]
      }
    },
    {
      "id": "config_screen_hopper",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/ContainersConfig.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/StorageStyles.java"
      ],
      "en": {
        "title": "Hoppers",
        "summary": "Hopper screen in iron grey. Default: on.",
        "details": [
          "Hopper screen in iron grey. Default: on."
        ]
      },
      "de": {
        "title": "Trichter",
        "summary": "Trichter-Bildschirm in Eisengrau. Standard: an.",
        "details": [
          "Trichter-Bildschirm in Eisengrau. Standard: an."
        ]
      }
    },
    {
      "id": "config_screen_dispenser",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/ContainersConfig.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/StorageStyles.java"
      ],
      "en": {
        "title": "Dispensers and droppers",
        "summary": "Dispenser and dropper screen in cobblestone grey. Default: on.",
        "details": [
          "Dispenser and dropper screen in cobblestone grey. Default: on."
        ]
      },
      "de": {
        "title": "Werfer und Spender",
        "summary": "Werfer- und Spender-Bildschirm in Bruchsteingrau. Standard: an.",
        "details": [
          "Werfer- und Spender-Bildschirm in Bruchsteingrau. Standard: an."
        ]
      }
    },
    {
      "id": "style_work",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/WorkStyles.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/client/WorkScreens.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/mixin/FurnaceBackgroundMixin.java",
        "modules/simplelib/shared/java/com/simplelib/api/client/ui/UiSymbols.java"
      ],
      "en": {
        "title": "Simple style for workstations",
        "summary": "Crafting table, furnace, blast furnace, smoker, brewing stand, beacon and enchanting table screens get the Simple style in the block's colour. Client only; the server needs nothing.",
        "details": [
          "Crafting table, furnace, blast furnace, smoker, brewing stand, beacon and enchanting table screens get the Simple style in the block's colour. Client only; the server needs nothing.",
          "Furnaces show their state without the Vanilla flame: the fuel slot fills with flames for the burn time left (brighter in the blast furnace), heat waves (smoke curls in the smoker) glow warm while it burns, and the engraved arrow fills white with the cooking progress. Result slots are big.",
          "The brewing stand is one box: the blaze powder slot fills with the fuel left, pipes run to the bottles, bubbles rise and the arrow down fills while brewing. Empty bottle and lapis slots show their icon as an engraved silhouette.",
          "The beacon shows its power fields with a pyramid and a star instead of the labels, raised effect buttons (sunk when selected) and a green check and red cross; the enchanting table shows its three offers as sunk rows with lapis gems for the cost and keeps the animated book. The recipe book button stays where Vanilla puts it."
        ]
      },
      "de": {
        "title": "Simple-Stil für Arbeitsblöcke",
        "summary": "Werkbank, Ofen, Schmelzofen, Räucherofen, Braustand, Leuchtfeuer und Zaubertisch bekommen den Simple-Stil in der Farbe des Blocks. Nur clientseitig; der Server braucht nichts.",
        "details": [
          "Werkbank, Ofen, Schmelzofen, Räucherofen, Braustand, Leuchtfeuer und Zaubertisch bekommen den Simple-Stil in der Farbe des Blocks. Nur clientseitig; der Server braucht nichts.",
          "Öfen zeigen ihren Zustand ohne die Vanilla-Flamme: der Brennstoff-Slot füllt sich mit Flammen für die restliche Brenndauer (im Schmelzofen heller), Hitzewellen (beim Räucherofen Rauchkringel) leuchten warm, solange er brennt, und der eingravierte Pfeil füllt sich weiß mit dem Kochfortschritt. Ergebnis-Slots sind groß.",
          "Der Braustand ist ein Kasten: der Lohenstaub-Slot füllt sich mit dem restlichen Brennstoff, Rohre führen zu den Flaschen, beim Brauen steigen Blasen auf und der Pfeil nach unten füllt sich. Leere Flaschen- und Lapis-Slots zeigen ihr Symbol als eingravierte Silhouette.",
          "Das Leuchtfeuer zeigt seine Kraftfelder mit Pyramide und Stern statt der Beschriftung, erhabene Effekt-Knöpfe (eingelassen, wenn gewählt) sowie grünen Haken und rotes Kreuz; der Zaubertisch zeigt seine drei Angebote als eingelassene Zeilen mit Lapis-Steinen für die Kosten und behält das animierte Buch. Der Rezeptbuch-Knopf bleibt an der Vanilla-Stelle."
        ]
      }
    },
    {
      "id": "config_screen_crafting",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/ContainersConfig.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/WorkStyles.java"
      ],
      "en": {
        "title": "Crafting tables",
        "summary": "Crafting table screen in light wood with a big result slot and an engraved arrow. The recipe book button stays. Default: on.",
        "details": [
          "Crafting table screen in light wood with a big result slot and an engraved arrow. The recipe book button stays. Default: on."
        ]
      },
      "de": {
        "title": "Werkbänke",
        "summary": "Werkbank-Bildschirm in hellem Holz mit großem Ergebnis-Slot und eingraviertem Pfeil. Der Rezeptbuch-Knopf bleibt. Standard: an.",
        "details": [
          "Werkbank-Bildschirm in hellem Holz mit großem Ergebnis-Slot und eingraviertem Pfeil. Der Rezeptbuch-Knopf bleibt. Standard: an."
        ]
      }
    },
    {
      "id": "config_screen_furnace",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/ContainersConfig.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/WorkStyles.java"
      ],
      "en": {
        "title": "Furnaces",
        "summary": "Furnace screen in stone grey: the fuel slot shows the burn time as flames, heat waves over it, the arrow fills with the cooking progress. Default: on.",
        "details": [
          "Furnace screen in stone grey: the fuel slot shows the burn time as flames, heat waves over it, the arrow fills with the cooking progress. Default: on."
        ]
      },
      "de": {
        "title": "Öfen",
        "summary": "Ofen-Bildschirm in Steingrau: der Brennstoff-Slot zeigt die Brenndauer als Flammen, darüber Hitzewellen, der Pfeil füllt sich mit dem Kochfortschritt. Standard: an.",
        "details": [
          "Ofen-Bildschirm in Steingrau: der Brennstoff-Slot zeigt die Brenndauer als Flammen, darüber Hitzewellen, der Pfeil füllt sich mit dem Kochfortschritt. Standard: an."
        ]
      }
    },
    {
      "id": "config_screen_blast_furnace",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/ContainersConfig.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/WorkStyles.java"
      ],
      "en": {
        "title": "Blast furnaces",
        "summary": "Blast furnace screen in dark iron with brighter flames in the fuel slot. Default: on.",
        "details": [
          "Blast furnace screen in dark iron with brighter flames in the fuel slot. Default: on."
        ]
      },
      "de": {
        "title": "Schmelzöfen",
        "summary": "Schmelzofen-Bildschirm in dunklem Eisen mit helleren Flammen im Brennstoff-Slot. Standard: an.",
        "details": [
          "Schmelzofen-Bildschirm in dunklem Eisen mit helleren Flammen im Brennstoff-Slot. Standard: an."
        ]
      }
    },
    {
      "id": "config_screen_smoker",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/ContainersConfig.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/WorkStyles.java"
      ],
      "en": {
        "title": "Smokers",
        "summary": "Smoker screen in smoked brown with smoke curls over the fuel slot. Default: on.",
        "details": [
          "Smoker screen in smoked brown with smoke curls over the fuel slot. Default: on."
        ]
      },
      "de": {
        "title": "Räucheröfen",
        "summary": "Räucherofen-Bildschirm in Rauchbraun mit Rauchkringeln über dem Brennstoff-Slot. Standard: an.",
        "details": [
          "Räucherofen-Bildschirm in Rauchbraun mit Rauchkringeln über dem Brennstoff-Slot. Standard: an."
        ]
      }
    },
    {
      "id": "config_screen_brewing_stand",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/ContainersConfig.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/WorkStyles.java"
      ],
      "en": {
        "title": "Brewing stands",
        "summary": "Brewing stand screen in one grey box: the blaze powder slot shows the fuel left, pipes lead to the bottles, bubbles rise while brewing. Default: on.",
        "details": [
          "Brewing stand screen in one grey box: the blaze powder slot shows the fuel left, pipes lead to the bottles, bubbles rise while brewing. Default: on."
        ]
      },
      "de": {
        "title": "Braustände",
        "summary": "Braustand-Bildschirm in einem grauen Kasten: der Lohenstaub-Slot zeigt den restlichen Brennstoff, Rohre führen zu den Flaschen, beim Brauen steigen Blasen auf. Standard: an.",
        "details": [
          "Braustand-Bildschirm in einem grauen Kasten: der Lohenstaub-Slot zeigt den restlichen Brennstoff, Rohre führen zu den Flaschen, beim Brauen steigen Blasen auf. Standard: an."
        ]
      }
    },
    {
      "id": "config_screen_beacon",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/ContainersConfig.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/WorkStyles.java"
      ],
      "en": {
        "title": "Beacons",
        "summary": "Beacon screen in glass teal: power fields with pyramid and star symbols, raised effect buttons, check and cross. Default: on.",
        "details": [
          "Beacon screen in glass teal: power fields with pyramid and star symbols, raised effect buttons, check and cross. Default: on."
        ]
      },
      "de": {
        "title": "Leuchtfeuer",
        "summary": "Leuchtfeuer-Bildschirm in Glas-Türkis: Kraftfelder mit Pyramide und Stern, erhabene Effekt-Knöpfe, Haken und Kreuz. Standard: an.",
        "details": [
          "Leuchtfeuer-Bildschirm in Glas-Türkis: Kraftfelder mit Pyramide und Stern, erhabene Effekt-Knöpfe, Haken und Kreuz. Standard: an."
        ]
      }
    },
    {
      "id": "config_screen_enchanting",
      "sources": [
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/ContainersConfig.java",
        "modules/simplecontainers/shared/java/com/simplebuilding/modules/simplecontainers/style/WorkStyles.java"
      ],
      "en": {
        "title": "Enchanting tables",
        "summary": "Enchanting table screen in red: the three offers as sunk rows with lapis gems for their cost. Default: on.",
        "details": [
          "Enchanting table screen in red: the three offers as sunk rows with lapis gems for their cost. Default: on."
        ]
      },
      "de": {
        "title": "Zaubertische",
        "summary": "Zaubertisch-Bildschirm in Rot: die drei Angebote als eingelassene Zeilen mit Lapis-Steinen für ihre Kosten. Standard: an.",
        "details": [
          "Zaubertisch-Bildschirm in Rot: die drei Angebote als eingelassene Zeilen mit Lapis-Steinen für ihre Kosten. Standard: an."
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
    "features": 14,
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
