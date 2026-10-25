window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplemovement"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplemovement",
    "name": "Simple Movement",
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
      "id": "crawl",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/mixin/PlayerEntityCrawlMixin.java"
      ],
      "en": {
        "title": "Manual crawling",
        "summary": "P or /crawl sets the synchronized swimming pose. Jump exits. Commands are limited to one change per 10 ticks; spectators and passengers are refused."
      },
      "de": {
        "title": "Manuelles Kriechen",
        "summary": "P oder /crawl setzt die synchronisierte Schwimmpose. Springen beendet das Kriechen. Maximal eine Änderung je 10 Ticks; Zuschauer und Passagiere werden abgewiesen."
      }
    },
    {
      "id": "autowalk",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/client/MovementClient.java"
      ],
      "en": {
        "title": "Auto-walk",
        "summary": "R toggles ordinary forward input only when the server enables enableAutowalk (default false). Opening a screen or leaving the world stops it. A click sound signals the change. It adds no speed or reach. The autowalk and crawl key bindings of Simple QoL are taken over once."
      },
      "de": {
        "title": "Automatisch laufen",
        "summary": "R schaltet normale Vorwärtseingabe nur bei Serverfreigabe enableAutowalk (Standard false). Bildschirm öffnen oder Welt verlassen beendet sie. Ein Klickton meldet die Änderung. Kein Tempo- oder Reichweitenbonus. Die Tastenbelegungen für Autowalk und Kriechen aus Simple QoL werden einmalig übernommen."
      }
    },
    {
      "id": "climbing",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/mixin/LadderSpeedMixin.java"
      ],
      "en": {
        "title": "Climbing and sliding",
        "summary": "Climbing speed defaults to 0.4 blocks/tick (range 0.2–0.4). Slide speed defaults to 0.8 (range 0.15–0.8). CAMERA requires looking down and sneaking; ALWAYS requires looking down without sneaking. The server rejects excess vertical movement on climbable blocks with a bounded latency allowance."
      },
      "de": {
        "title": "Klettern und Rutschen",
        "summary": "Klettertempo standardmäßig 0.4 Blöcke/Tick (0.2–0.4), Rutschtempo 0.8 (0.15–0.8). CAMERA verlangt Blick nach unten und Schleichen; ALWAYS Blick nach unten ohne Schleichen. Der Server weist überhöhte Bewegung auf kletterbaren Blöcken mit begrenztem Latenzspielraum ab."
      }
    },
    {
      "id": "powder_snow",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/mixin/PowderSnowWalkMixin.java"
      ],
      "en": {
        "title": "Frost Walker on powder snow",
        "summary": "Frost Walker permits walking on powder snow. Enabled by frostWalkerWalkOnPowderSnow, default true."
      },
      "de": {
        "title": "Eisläufer auf Pulverschnee",
        "summary": "Eisläufer erlaubt das Gehen auf Pulverschnee. frostWalkerWalkOnPowderSnow ist standardmäßig true."
      }
    },
    {
      "id": "jump_boost_step",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/JumpBoostStep.java"
      ],
      "en": {
        "title": "Jump Boost steps up",
        "summary": "While Jump Boost is active (potion, beacon or command), the step_height attribute is topped up to at least 1 block, so you walk up a full block without jumping. Boots with Step Height are not added on top: the higher value wins. Switch jumpBoostStepUp, default true."
      },
      "de": {
        "title": "Sprungkraft steigt Stufen",
        "summary": "Solange Sprungkraft aktiv ist (Trank, Leuchtfeuer oder Befehl), wird das Attribut step_height auf mindestens 1 Block aufgefüllt, du läufst also ohne Sprung einen ganzen Block hoch. Stiefel mit Stufenhöhe werden nicht addiert: der höhere Wert gilt. Schalter jumpBoostStepUp, Standard true."
      }
    },
    {
      "id": "config_jumpBoostStepUp",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/MovementConfig.java"
      ],
      "en": {
        "title": "jumpBoostStepUp",
        "summary": "Server setting. Default: true."
      },
      "de": {
        "title": "jumpBoostStepUp",
        "summary": "Servereinstellung. Standard: true."
      }
    },
    {
      "id": "config_frostWalkerWalkOnPowderSnow",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/MovementConfig.java"
      ],
      "en": {
        "title": "frostWalkerWalkOnPowderSnow",
        "summary": "Server setting. Default: true."
      },
      "de": {
        "title": "frostWalkerWalkOnPowderSnow",
        "summary": "Servereinstellung. Standard: true."
      }
    },
    {
      "id": "config_enableAutowalk",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/MovementConfig.java"
      ],
      "en": {
        "title": "enableAutowalk",
        "summary": "Server setting. Default: false."
      },
      "de": {
        "title": "enableAutowalk",
        "summary": "Servereinstellung. Standard: false."
      }
    },
    {
      "id": "config_ladderClimbingSpeed",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/MovementConfig.java"
      ],
      "en": {
        "title": "ladderClimbingSpeed",
        "summary": "Server setting. Default: 0.4."
      },
      "de": {
        "title": "ladderClimbingSpeed",
        "summary": "Servereinstellung. Standard: 0.4."
      }
    },
    {
      "id": "config_enableFastLadderSlide",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/MovementConfig.java"
      ],
      "en": {
        "title": "enableFastLadderSlide",
        "summary": "Server setting. Default: true."
      },
      "de": {
        "title": "enableFastLadderSlide",
        "summary": "Servereinstellung. Standard: true."
      }
    },
    {
      "id": "config_ladderSlideSpeed",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/MovementConfig.java"
      ],
      "en": {
        "title": "ladderSlideSpeed",
        "summary": "Server setting. Default: 0.8."
      },
      "de": {
        "title": "ladderSlideSpeed",
        "summary": "Servereinstellung. Standard: 0.8."
      }
    },
    {
      "id": "config_ladderSlideActivation",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/MovementConfig.java"
      ],
      "en": {
        "title": "ladderSlideActivation",
        "summary": "Server setting. Default: \"CAMERA\"."
      },
      "de": {
        "title": "ladderSlideActivation",
        "summary": "Servereinstellung. Standard: \"CAMERA\"."
      }
    },
    {
      "id": "config_enableManualCrawl",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/MovementConfig.java"
      ],
      "en": {
        "title": "Manual Crawling",
        "summary": "Allow /crawl and the crawl key. The server validates and limits changes. Default: true."
      },
      "de": {
        "title": "Manuelles Kriechen",
        "summary": "/crawl und die Kriechtaste erlauben. Der Server prüft und begrenzt Änderungen. Standard: true."
      }
    },
    {
      "id": "commands",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/MovementCommands.java",
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/SimpleMovement.java"
      ],
      "en": {
        "title": "Commands",
        "summary": "/crawl is available to players. Permission level 2: /simplemovement autowalk and frostWalkerSnow <true|false> and ladderSpeed <0.2-0.4>; changes are saved to config/simplemovement.json. /simplemovement config shows and changes every option of config/simplemovement.json (list [group], get, set, reset, help; Tab completes options and values). Values are bounded like in the file, saved at once and take effect at once. Permission level 2.",
        "details": [
          "Example: /simplemovement config get <option> shows the value, the default and the type; /simplemovement config reset <option> brings the default back; a wrong type is refused and nothing is saved."
        ]
      },
      "de": {
        "title": "Befehle",
        "summary": "/crawl ist für Spieler. Berechtigung 2: /simplemovement autowalk und frostWalkerSnow <true|false> sowie ladderSpeed <0.2-0.4>; Speichern in config/simplemovement.json. /simplemovement config zeigt und ändert jede Option von config/simplemovement.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte). Werte werden wie in der Datei begrenzt, sofort gespeichert und wirken sofort. Berechtigung 2.",
        "details": [
          "Beispiel: /simplemovement config get <Option> zeigt Wert, Standard und Typ; /simplemovement config reset <Option> stellt den Standard wieder her; ein falscher Typ wird abgelehnt und nichts gespeichert."
        ]
      }
    },
    {
      "id": "submod",
      "sources": [
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/SimpleMovement.java",
        "framework/src/main/java/com/simplebuilding/framework/api/SubMods.java"
      ],
      "en": {
        "title": "Sub-mod of Simple QoL",
        "summary": "Playable alone (config/simplemovement.json). Bundled in Simple QoL, which can switch it off with \"Enable Simple Movement\". On the first start the old Simple QoL settings of this sub-mod are taken over once."
      },
      "de": {
        "title": "Sub-Mod von Simple QoL",
        "summary": "Allein spielbar (config/simplemovement.json). In Simple QoL gebuendelt, dort mit \"Simple Movement aktivieren\" abschaltbar. Beim ersten Start werden die alten Simple-QoL-Einstellungen dieser Sub-Mod einmalig übernommen."
      }
    },
    {
      "id": "step_height",
      "sources": [
        "modules/simplemovement/shared/resources/data/simplemovement/enchantment/step_height.json",
        "modules/simplemovement/shared/resources/data/minecraft/tags/enchantment/treasure.json",
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/MovementTests.java"
      ],
      "en": {
        "title": "Step Height",
        "summary": "Treasure enchantment for boots, levels I-II: the step_height attribute rises by 0.4 / 0.9, so you step up 1 block (I) or 1.5 blocks (II, fences) without jumping. Like Mending: not in the enchanting table (or the Astral Enchanter), found as a book in chest loot and from librarians, also at the anvil. Compatible with every other enchantment. Datapack-only: works with the sub-mod switched off."
      },
      "de": {
        "title": "Stufenhöhe",
        "summary": "Schatz-Verzauberung für Stiefel, Stufen I-II: Das Attribut step_height steigt um 0,4 / 0,9, du steigst ohne Sprung 1 Block (I) bzw. 1,5 Blöcke (II, Zäune) hoch. Wie Reparatur: nicht am Verzauberungstisch (auch nicht im Astral-Verzauberer), als Buch in Truhen-Beute und bei Bibliothekaren, auch am Amboss. Mit allen anderen Verzauberungen kombinierbar. Reine Datenpaket-Verzauberung: wirkt auch bei abgeschalteter Sub-Mod."
      }
    },
    {
      "id": "sprint",
      "sources": [
        "modules/simplemovement/shared/resources/data/simplemovement/enchantment/sprint.json",
        "modules/simplemovement/shared/resources/data/minecraft/tags/enchantment/tradeable.json",
        "modules/simplemovement/shared/java/com/simplebuilding/modules/simplemovement/MovementTests.java"
      ],
      "en": {
        "title": "Sprint",
        "summary": "Treasure enchantment for leggings, levels I-III: while sprinting, movement speed rises by 10 / 20 / 30 % of the base speed (so +10/20/30 % over normal sprint speed); walking and sneaking are unchanged. Swift Sneak does the same for sneaking and stays compatible. Like Mending: not in the enchanting table (or the Astral Enchanter), found as a book in chest loot and from librarians. The bonus is updated when you change blocks, so it starts and ends a fraction of a second late. Datapack-only: works with the sub-mod switched off."
      },
      "de": {
        "title": "Sprint",
        "summary": "Schatz-Verzauberung für Hosen, Stufen I-III: Beim Sprinten steigt die Laufgeschwindigkeit um 10 / 20 / 30 % der Grundgeschwindigkeit (also +10/20/30 % über dem normalen Sprinttempo); Gehen und Schleichen bleiben unverändert. Swift Sneak macht dasselbe beim Schleichen und bleibt kombinierbar. Wie Reparatur: nicht am Verzauberungstisch (auch nicht im Astral-Verzauberer), als Buch in Truhen-Beute und bei Bibliothekaren. Der Bonus wird beim Blockwechsel aktualisiert und setzt daher einen Bruchteil einer Sekunde verzögert ein bzw. aus. Reine Datenpaket-Verzauberung: wirkt auch bei abgeschalteter Sub-Mod."
      }
    }
  ],
  "recipes": [],
  "lootTables": [],
  "tags": [],
  "advancements": [],
  "enchantments": [
    {
      "id": "simplemovement:sprint",
      "name": {
        "en_us": "Sprint",
        "de_de": "Sprint"
      },
      "description": {
        "en_us": ""
      },
      "maxLevel": 3,
      "weight": 2,
      "minCost": {
        "base": 25,
        "per_level_above_first": 25
      },
      "maxCost": {
        "base": 75,
        "per_level_above_first": 25
      },
      "anvilCost": 4,
      "slots": [
        "legs"
      ],
      "supportedItems": "#minecraft:enchantable/leg_armor",
      "primaryItems": null,
      "exclusiveSet": null,
      "effects": [
        "minecraft:location_changed"
      ],
      "implementedIn": "data",
      "hasEffect": true,
      "source": "modules/simplemovement/shared/resources/data/simplemovement/enchantment/sprint.json",
      "note": {
        "en": {
          "title": "Sprint",
          "summary": "Treasure enchantment for leggings, levels I-III: while sprinting, movement speed rises by 10 / 20 / 30 % of the base speed (so +10/20/30 % over normal sprint speed); walking and sneaking are unchanged. Swift Sneak does the same for sneaking and stays compatible. Like Mending: not in the enchanting table (or the Astral Enchanter), found as a book in chest loot and from librarians. The bonus is updated when you change blocks, so it starts and ends a fraction of a second late. Datapack-only: works with the sub-mod switched off."
        },
        "de": {
          "title": "Sprint",
          "summary": "Schatz-Verzauberung für Hosen, Stufen I-III: Beim Sprinten steigt die Laufgeschwindigkeit um 10 / 20 / 30 % der Grundgeschwindigkeit (also +10/20/30 % über dem normalen Sprinttempo); Gehen und Schleichen bleiben unverändert. Swift Sneak macht dasselbe beim Schleichen und bleibt kombinierbar. Wie Reparatur: nicht am Verzauberungstisch (auch nicht im Astral-Verzauberer), als Buch in Truhen-Beute und bei Bibliothekaren. Der Bonus wird beim Blockwechsel aktualisiert und setzt daher einen Bruchteil einer Sekunde verzögert ein bzw. aus. Reine Datenpaket-Verzauberung: wirkt auch bei abgeschalteter Sub-Mod."
        }
      }
    },
    {
      "id": "simplemovement:step_height",
      "name": {
        "en_us": "Step Height",
        "de_de": "Stufenhöhe"
      },
      "description": {
        "en_us": ""
      },
      "maxLevel": 2,
      "weight": 2,
      "minCost": {
        "base": 25,
        "per_level_above_first": 25
      },
      "maxCost": {
        "base": 75,
        "per_level_above_first": 25
      },
      "anvilCost": 4,
      "slots": [
        "feet"
      ],
      "supportedItems": "#minecraft:enchantable/foot_armor",
      "primaryItems": null,
      "exclusiveSet": null,
      "effects": [
        "minecraft:attributes"
      ],
      "implementedIn": "data",
      "hasEffect": true,
      "source": "modules/simplemovement/shared/resources/data/simplemovement/enchantment/step_height.json",
      "note": {
        "en": {
          "title": "Step Height",
          "summary": "Treasure enchantment for boots, levels I-II: the step_height attribute rises by 0.4 / 0.9, so you step up 1 block (I) or 1.5 blocks (II, fences) without jumping. Like Mending: not in the enchanting table (or the Astral Enchanter), found as a book in chest loot and from librarians, also at the anvil. Compatible with every other enchantment. Datapack-only: works with the sub-mod switched off."
        },
        "de": {
          "title": "Stufenhöhe",
          "summary": "Schatz-Verzauberung für Stiefel, Stufen I-II: Das Attribut step_height steigt um 0,4 / 0,9, du steigst ohne Sprung 1 Block (I) bzw. 1,5 Blöcke (II, Zäune) hoch. Wie Reparatur: nicht am Verzauberungstisch (auch nicht im Astral-Verzauberer), als Buch in Truhen-Beute und bei Bibliothekaren, auch am Amboss. Mit allen anderen Verzauberungen kombinierbar. Reine Datenpaket-Verzauberung: wirkt auch bei abgeschalteter Sub-Mod."
        }
      }
    }
  ],
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
    "features": 17,
    "recipes": 0,
    "lootTables": 0,
    "tags": 0,
    "advancements": 0,
    "enchantments": 2,
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
