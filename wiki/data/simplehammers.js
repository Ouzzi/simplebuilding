window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplehammers"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplehammers",
    "name": "Simple Hammers",
    "version": "1.0.0",
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
      "id": "sledgehammers",
      "sources": [
        "modules/simplehammers/shared/java/com/simplehammers/item/SledgehammerItem.java",
        "modules/simplehammers/shared/java/com/simplehammers/HammerTiers.java",
        "modules/simplehammers/shared/java/com/simplehammers/registry/HammerRegistry.java",
        "modules/simplehammers/shared/java/com/simplehammers/engine/SledgehammerUsageEvent.java",
        "modules/simplehammers/shared/java/com/simplehammers/engine/SledgehammerUtils.java",
        "modules/simplehammers/shared/java/com/simplehammers/mixin/HammerMiningSpeedMixin.java",
        "modules/simplehammers/shared/java/com/simplehammers/mixin/client/MultiPlayerGameModeHammerProgressMixin.java",
        "modules/simplehammers/shared/resources/data/simplehammers/recipe/iron_sledgehammer.json",
        "modules/simplehammers/shared/resources/data/simplehammers/recipe/netherite_sledgehammer_smithing.json",
        "modules/simplehammers/shared/resources/data/simplehammers/recipe/enderite_sledgehammer_smithing.json",
        "modules/simplehammers/shared/resources/data/simplehammers/tags/item/sledgehammers.json"
      ],
      "en": {
        "title": "Sledgehammers: mining a whole area",
        "summary": "Seven sledgehammers from Stone to Enderite. Each mines like a pickaxe of its tier, but one swing takes the 3x3 area in front of you along; sneaking mines just the aimed block. Bigger areas take longer: the first block costs 1.5 times a same-tier pickaxe, blocks 2 to 9 another 0.8 each, every further block 0.7.",
        "details": [
          "Recipes: a block and two ingots (or gems) of the material in the top row, two sticks below (Stone: cobblestone and an iron ingot). Netherite: smithing table with a Netherite Upgrade and a Netherite Ingot on the Diamond Sledgehammer; Enderite: Enderite Upgrade template and an Enderite Ingot on the Netherite Sledgehammer. With SimpleBuilding a Basic Upgrade template and 22 ingots also take Copper to Iron, Iron to Gold and Gold to Diamond.",
          "The area lies flat when you look steeply up or down (more than 60 degrees), otherwise it stands upright in front of you. Without an enchantment it takes only blocks of the same type that need a pickaxe and that the hammer may harvest.",
          "Durability is four times a pickaxe of the tier (Stone 760 ... Enderite 10000); every broken block costs 2, a block of the area it is not the right tool for one more. Netherite and Enderite hammers do not burn.",
          "Mining time: the area time is computed on client and server alike; switching between sneaking (one block) and the area while mining converts the progress, so it never jumps ahead.",
          "Other mods may replace the area (SimpleBuilding: an octant with a selection in the off hand) through SimpleLib's HammerActions."
        ]
      },
      "de": {
        "title": "Vorschlaghämmer: eine ganze Fläche abbauen",
        "summary": "Sieben Vorschlaghämmer von Stein bis Enderit. Jeder baut ab wie eine Spitzhacke seiner Stufe, nimmt aber mit einem Schlag die 3x3-Fläche vor dir mit; Schleichen baut nur den angezielten Block ab. Größere Flächen dauern länger: der erste Block kostet das 1,5-fache einer Spitzhacke derselben Stufe, Block 2 bis 9 je 0,8 mehr, jeder weitere 0,7.",
        "details": [
          "Rezepte: oben ein Block und zwei Barren (oder Edelsteine) des Materials, darunter zwei Stöcke (Stein: Bruchstein und ein Eisenbarren). Netherit: Schmiedetisch mit Netherit-Aufwertung und Netheritbarren auf dem Diamant-Vorschlaghammer; Enderit: Enderit-Aufwertungsvorlage und Enderitbarren auf dem Netherit-Vorschlaghammer. Mit SimpleBuilding werten eine einfache Aufwertungsvorlage und 22 Barren auch Kupfer zu Eisen, Eisen zu Gold und Gold zu Diamant auf.",
          "Die Fläche liegt flach, wenn du steil nach oben oder unten schaust (mehr als 60 Grad), sonst steht sie aufrecht vor dir. Ohne Verzauberung nimmt sie nur Blöcke derselben Art, die eine Spitzhacke brauchen und die der Hammer ernten darf.",
          "Haltbarkeit: das Vierfache einer Spitzhacke der Stufe (Stein 760 ... Enderit 10000); jeder abgebaute Block kostet 2, ein Block der Fläche, für den er nicht das richtige Werkzeug ist, einen mehr. Netherit- und Enderit-Hämmer verbrennen nicht.",
          "Abbauzeit: Client und Server rechnen die Flächenzeit gleich; wer beim Abbauen zwischen Schleichen (ein Block) und Fläche wechselt, behält die Abbauzeit, der Fortschritt springt nie vor.",
          "Andere Mods können die Fläche ersetzen (SimpleBuilding: ein Oktant mit Auswahl in der Nebenhand), über SimpleLibs HammerActions."
        ]
      },
      "related": [
        "simplehammers:stone_sledgehammer",
        "simplehammers:copper_sledgehammer",
        "simplehammers:iron_sledgehammer",
        "simplehammers:gold_sledgehammer",
        "simplehammers:diamond_sledgehammer",
        "simplehammers:netherite_sledgehammer",
        "simplehammers:enderite_sledgehammer"
      ]
    },
    {
      "id": "reshaping",
      "sources": [
        "modules/simplehammers/shared/java/com/simplehammers/item/SledgehammerItem.java",
        "modules/simplehammers/shared/java/com/simplehammers/engine/HammerCorners.java",
        "modules/simplehammers/shared/java/com/simplehammers/engine/Orientation.java",
        "modules/simplehammers/shared/java/com/simplehammers/mixin/CarvedStairMixin.java",
        "modules/simplehammers/shared/java/com/simplehammers/engine/HammerEnchantments.java"
      ],
      "en": {
        "title": "Carving and reshaping",
        "summary": "Sneak and hold use on a block: after a short charge the hammer knocks the aimed octet (an eighth) off it. The rest becomes the slab, a stair or - with Simple Octets - the material's octets; blocks without stairs or octets stay as they are.",
        "details": [
          "The charge takes 200 / (material speed + 5 per Efficiency level) ticks, 4 to 40, two thirds of that for carving; it finishes only on the block it started on while you still aim at it.",
          "A carved stair keeps its shape when blocks next to it change (property carved).",
          "With SimpleBuilding's enchantment Constructor's Touch sneaking runs it backwards: slab to stairs, stairs to the full block.",
          "Reshaping costs 1 durability (backwards 2) and checks that you may build there (spawn protection, adventure mode, claims)."
        ]
      },
      "de": {
        "title": "Abtragen und Umformen",
        "summary": "Schleichen und Benutzen halten: nach einer kurzen Ladezeit schlägt der Hammer das angezielte Achtel vom Block. Der Rest wird zur Stufe, zur Treppe oder - mit Simple Octets - zu Achteln des Materials; Blöcke ohne Treppe und Achtel bleiben, wie sie sind.",
        "details": [
          "Die Ladung dauert 200 / (Materialtempo + 5 je Effizienzstufe) Ticks, 4 bis 40, beim Abtragen zwei Drittel davon; sie endet nur auf dem Block, auf dem sie begann, solange du ihn noch anschaust.",
          "Eine abgetragene Treppe behält ihre Form, wenn sich Nachbarblöcke ändern (Eigenschaft carved).",
          "Mit SimpleBuildings Verzauberung Berührung des Konstrukteurs läuft es beim Schleichen rückwärts: Stufe zu Treppe, Treppe zum vollen Block.",
          "Umformen kostet 1 Haltbarkeit (rückwärts 2) und prüft, ob du dort bauen darfst (Spawnschutz, Abenteuermodus, Claims)."
        ]
      }
    },
    {
      "id": "crushing",
      "sources": [
        "modules/simplehammers/shared/java/com/simplehammers/item/SledgehammerItem.java",
        "modules/simplehammers/shared/java/com/simplehammers/config/HammerConfig.java"
      ],
      "en": {
        "title": "Crushing diamond and quartz blocks",
        "summary": "Right-click a diamond block or a Cracked Diamond Block nine times with an iron-tier hammer or better: it breaks into 81 Diamond Pebbles (config diamondBlockPebbles). A quartz block breaks into 4 quartz in 4 strikes.",
        "details": [
          "Each strike shows its share of the result above the block; the last one frees everything. Strikes count per block, any player continues them; without a further strike the count runs out after 5 seconds.",
          "Stone and copper hammers only clank on a diamond block. Crushing costs 1 durability; the quartz block gives the hammer a short cooldown.",
          "The pebbles and the cracked diamond block are SimpleLib's shared materials."
        ]
      },
      "de": {
        "title": "Diamant- und Quarzblöcke zerschlagen",
        "summary": "Neun Rechtsklicks mit einem Hammer ab der Eisenstufe auf einen Diamantblock oder rissigen Diamantblock: er zerspringt in 81 Diamantkiesel (Config diamondBlockPebbles). Ein Quarzblock gibt in 4 Schlägen 4 Quarz.",
        "details": [
          "Jeder Schlag zeigt seinen Teil des Ergebnisses über dem Block, der letzte gibt alles frei. Die Schläge zählen je Block, jeder Spieler setzt sie fort; ohne weiteren Schlag verfällt die Zählung nach 5 Sekunden.",
          "Stein- und Kupferhämmer prallen am Diamantblock nur ab. Zerschlagen kostet 1 Haltbarkeit; der Quarzblock gibt dem Hammer eine kurze Abklingzeit.",
          "Die Kiesel und der rissige Diamantblock sind SimpleLibs gemeinsame Materialien."
        ]
      }
    },
    {
      "id": "in_world_upgrade",
      "sources": [
        "modules/simplehammers/shared/java/com/simplehammers/engine/SledgehammerUpgrades.java",
        "modules/simplehammers/shared/java/com/simplehammers/engine/SledgehammerProgress.java",
        "modules/simplehammers/shared/java/com/simplehammers/SimpleHammers.java",
        "modules/simplelib/shared/java/com/simplelib/api/InWorldUpgrades.java",
        "modules/simplelib/shared/java/com/simplelib/api/HammerActions.java"
      ],
      "en": {
        "title": "In-world upgrades with the hammer",
        "summary": "Hammer in the main hand, the upgrade material in the off hand, hold use on an upgradable block: one strike per second (5 by default), the last one turns it into the next tier with its contents. Which blocks go up is SimpleLib's data type in_world_upgrades - every Simple mod ships its own (crucibles, furnaces, beacons, SimpleBuilding's chests, hoppers and pistons).",
        "details": [
          "While Simple Hammers is loaded the axe ways of the other Simple mods are off: building crucibles, attaching barrels and upgrading run with the hammer only.",
          "Netherite tiers need at least a Diamond Sledgehammer, Enderite tiers a Netherite one (min_tool). Each strike costs 2 (to reinforced), 4 (Netherite) or 10 (Enderite) durability (config).",
          "Strikes are saved with the world and shown as cracks; anyone continues where it stopped. Letting go, looking away, a changed block or missing material stops it without using the material.",
          "Crucibles go up strike by strike with one material unit every few strikes (Simple Crucibles); a double chest needs the material twice (SimpleBuilding).",
          "Wrong material, a too weak hammer or too little material: a quiet clunk instead of text."
        ]
      },
      "de": {
        "title": "Aufwerten in der Welt mit dem Hammer",
        "summary": "Hammer in der Haupthand, das Aufwertungsmaterial in der Nebenhand, Benutzen auf einem aufwertbaren Block halten: ein Schlag je Sekunde (Standard 5), der letzte macht ihn samt Inhalt zur nächsten Stufe. Welche Blöcke aufsteigen, sagt SimpleLibs Datentyp in_world_upgrades - jede Simple-Mod bringt ihre eigenen mit (Tiegel, Öfen, Leuchtfeuer, SimpleBuildings Truhen, Trichter und Kolben).",
        "details": [
          "Solange Simple Hammers geladen ist, sind die Axt-Wege der anderen Simple-Mods aus: Tiegel bauen, Fässer andocken und Aufwerten gehen nur mit dem Hammer.",
          "Netherit-Stufen brauchen mindestens einen Diamant-Vorschlaghammer, Enderit-Stufen einen aus Netherit (min_tool). Jeder Schlag kostet 2 (zu verstärkt), 4 (Netherit) oder 10 (Enderit) Haltbarkeit (Config).",
          "Die Schläge werden mit der Welt gespeichert und als Risse gezeigt; jeder setzt dort fort. Loslassen, Wegschauen, ein anderer Block oder fehlendes Material bricht ab, ohne Material zu verbrauchen.",
          "Tiegel steigen Schlag für Schlag auf, alle paar Schläge eine Materialeinheit (Simple Crucibles); eine Doppeltruhe braucht das Material doppelt (SimpleBuilding).",
          "Falsches Material, zu schwacher Hammer oder zu wenig Material: ein leises Klonk statt Text."
        ]
      }
    },
    {
      "id": "hammer_enchantments",
      "sources": [
        "modules/simplehammers/shared/java/com/simplehammers/engine/HammerEnchantments.java",
        "modules/simplehammers/shared/resources/data/simplehammers/enchantment/radius.json",
        "modules/simplehammers/shared/resources/data/simplehammers/enchantment/override.json",
        "modules/simplehammers/shared/resources/data/simplehammers/enchantment/break_through.json",
        "modules/simplehammers/shared/resources/data/simplehammers/tags/item/radius_enchantable.json"
      ],
      "en": {
        "title": "Hammer enchantments",
        "summary": "Three enchantments for sledgehammers: Radius makes the area 5x5, Break Through (I-II) takes one more layer behind the face per level, Override (I-II) takes other blocks along - level I every pickaxe block, level II everything breakable, and the hammer then also harvests axe, shovel and hoe blocks at its full speed.",
        "details": [
          "Enchanting table weights: Radius 1 (very rare), Break Through and Override 2 (rare); anvil cost 8 / 4 / 4.",
          "With SimpleBuilding Radius also goes on its Ore Detector (tag simplebuilding:radius_enchantable)."
        ]
      },
      "de": {
        "title": "Hammer-Verzauberungen",
        "summary": "Drei Verzauberungen für Vorschlaghämmer: Radius macht die Fläche 5x5, Durchbruch (I-II) nimmt je Stufe eine Lage hinter der Fläche mit, Übersteuerung (I-II) nimmt andere Blöcke mit - Stufe I jeden Spitzhacken-Block, Stufe II alles Abbaubare, und der Hammer erntet dann auch Axt-, Schaufel- und Hacken-Blöcke in vollem Tempo.",
        "details": [
          "Gewicht am Zaubertisch: Radius 1 (sehr selten), Durchbruch und Übersteuerung 2 (selten); Ambosskosten 8 / 4 / 4.",
          "Mit SimpleBuilding geht Radius auch auf dessen Erzdetektor (Tag simplebuilding:radius_enchantable)."
        ]
      },
      "related": [
        "simplehammers:radius",
        "simplehammers:override",
        "simplehammers:break_through"
      ]
    },
    {
      "id": "crafting_tool",
      "sources": [
        "modules/simplehammers/shared/java/com/simplehammers/engine/SledgehammerCrafting.java",
        "modules/simplehammers/shared/java/com/simplehammers/mixin/ShapelessRecipeMixin.java"
      ],
      "en": {
        "title": "A tool in the crafting grid",
        "summary": "A shapeless recipe with a sledgehammer gives the hammer back with 1 durability less instead of using it up; a hammer that breaks doing so is gone.",
        "details": []
      },
      "de": {
        "title": "Werkzeug im Werkbankraster",
        "summary": "Ein formloses Rezept mit einem Vorschlaghammer gibt den Hammer mit 1 Haltbarkeit weniger zurück, statt ihn zu verbrauchen; bricht er dabei, ist er weg.",
        "details": []
      }
    },
    {
      "id": "config_server",
      "sources": [
        "modules/simplehammers/shared/java/com/simplehammers/config/HammerConfig.java"
      ],
      "en": {
        "title": "Server Config",
        "summary": "config/simplehammers-server.json: enabled, upgradeSeconds (1-30, default 5), reinforcedDamagePerHit (2), netheriteDamagePerHit (4), enderiteDamagePerHit (10) (0-64 each) and diamondBlockPebbles (1-81, default 81). On the first start it takes these values from SimpleBuilding's config (server.tools); missing or broken keys use the defaults.",
        "details": [
          "enabled false makes the hammers plain pickaxes of their tier (no area, no reshaping, no upgrades)."
        ]
      },
      "de": {
        "title": "Server-Config",
        "summary": "config/simplehammers-server.json: enabled, upgradeSeconds (1-30, Standard 5), reinforcedDamagePerHit (2), netheriteDamagePerHit (4), enderiteDamagePerHit (10) (je 0-64) und diamondBlockPebbles (1-81, Standard 81). Beim ersten Start übernimmt sie diese Werte aus SimpleBuildings Config (server.tools); fehlende oder kaputte Schlüssel nehmen die Standardwerte.",
        "details": [
          "enabled false macht die Hämmer zu einfachen Spitzhacken ihrer Stufe (keine Fläche, kein Umformen, keine Aufwertung)."
        ]
      }
    },
    {
      "id": "migration",
      "sources": [
        "modules/simplehammers/shared/java/com/simplehammers/SimpleHammers.java",
        "mc26_3/overlay/java/com/simplebuilding/gametest/MigrationWorldTests.java"
      ],
      "en": {
        "title": "Moved from SimpleBuilding",
        "summary": "The seven sledgehammers, the enchantments Break Through, Override and Radius, the ten hammer recipes and their recipe unlocks were SimpleBuilding's (simplebuilding:*). Old worlds load them under simplehammers:* through SimpleLib's id moves and save the new ids.",
        "details": [
          "Enchantments on items and stored progress of begun upgrades stay.",
          "SimpleBuilding needs Simple Hammers and bundles it; Simple Hammers runs on its own with SimpleLib, which it bundles. The Enderite hammer stays craftable until Simple Enderite exists."
        ]
      },
      "de": {
        "title": "Aus SimpleBuilding umgezogen",
        "summary": "Die sieben Vorschlaghämmer, die Verzauberungen Durchbruch, Übersteuerung und Radius, die zehn Hammer-Rezepte und ihre Rezeptfreischaltungen gehörten SimpleBuilding (simplebuilding:*). Alte Welten laden sie über SimpleLibs Id-Umzüge als simplehammers:* und speichern die neuen Ids.",
        "details": [
          "Verzauberungen auf Items und gespeicherter Fortschritt angefangener Aufwertungen bleiben.",
          "SimpleBuilding braucht Simple Hammers und bündelt es; Simple Hammers läuft allein mit SimpleLib, das es mitbringt. Der Enderit-Hammer bleibt herstellbar, bis es Simple Enderite gibt."
        ]
      }
    },
    {
      "id": "creative_tab_switch",
      "sources": [
        "modules/simplehammers/shared/java/com/simplehammers/registry/HammerRegistry.java"
      ],
      "en": {
        "title": "Creative tabs",
        "summary": "Own tab with the seven hammers, the guide and the three enchanted books. Without SimpleBuilding each hammer also sits behind the hoe of its tier in the Vanilla tools tab (switch addItemsToVanillaTabs).",
        "details": []
      },
      "de": {
        "title": "Kreativ-Tabs",
        "summary": "Eigener Tab mit den sieben Hämmern, dem Handbuch und den drei Verzauberungsbüchern. Ohne SimpleBuilding steht jeder Hammer zusätzlich hinter der Hacke seiner Stufe im Vanilla-Werkzeugtab (Schalter addItemsToVanillaTabs).",
        "details": []
      }
    }
  ],
  "recipes": [
    {
      "id": "simplehammers:copper_sledgehammer",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": null,
      "result": {
        "id": "simplehammers:copper_sledgehammer",
        "count": 1
      },
      "source": "modules/simplehammers/shared/resources/data/simplehammers/recipe/copper_sledgehammer.json",
      "ingredients": [
        "minecraft:copper_block",
        "minecraft:copper_ingot",
        "minecraft:stick"
      ],
      "pattern": [
        "BMM",
        " S ",
        " S "
      ],
      "key": {
        "B": [
          "minecraft:copper_block"
        ],
        "M": [
          "minecraft:copper_ingot"
        ],
        "S": [
          "minecraft:stick"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:copper_ingot",
            "count": 11
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simplehammers:diamond_sledgehammer",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": null,
      "result": {
        "id": "simplehammers:diamond_sledgehammer",
        "count": 1
      },
      "source": "modules/simplehammers/shared/resources/data/simplehammers/recipe/diamond_sledgehammer.json",
      "ingredients": [
        "minecraft:diamond",
        "minecraft:diamond_block",
        "minecraft:stick"
      ],
      "pattern": [
        "BMM",
        " S ",
        " S "
      ],
      "key": {
        "B": [
          "minecraft:diamond_block"
        ],
        "M": [
          "minecraft:diamond"
        ],
        "S": [
          "minecraft:stick"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 11
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simplehammers:enderite_sledgehammer_smithing",
      "type": "minecraft:smithing_transform",
      "category": null,
      "group": null,
      "result": {
        "id": "simplehammers:enderite_sledgehammer",
        "count": 1
      },
      "source": "modules/simplehammers/shared/resources/data/simplehammers/recipe/enderite_sledgehammer_smithing.json",
      "ingredients": [
        "simplehammers:netherite_sledgehammer",
        "simplelib:enderite_ingot",
        "simplelib:enderite_upgrade_template"
      ],
      "slots": {
        "template": [
          "simplelib:enderite_upgrade_template"
        ],
        "base": [
          "simplehammers:netherite_sledgehammer"
        ],
        "addition": [
          "simplelib:enderite_ingot"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 11
          },
          {
            "id": "minecraft:netherite_ingot",
            "count": 1
          },
          {
            "id": "minecraft:netherite_upgrade_smithing_template",
            "count": 1
          },
          {
            "id": "simplelib:enderite_ingot",
            "count": 1
          },
          {
            "id": "simplelib:enderite_upgrade_template",
            "count": 1
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simplehammers:gold_sledgehammer",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": null,
      "result": {
        "id": "simplehammers:gold_sledgehammer",
        "count": 1
      },
      "source": "modules/simplehammers/shared/resources/data/simplehammers/recipe/gold_sledgehammer.json",
      "ingredients": [
        "minecraft:gold_block",
        "minecraft:gold_ingot",
        "minecraft:stick"
      ],
      "pattern": [
        "BMM",
        " S ",
        " S "
      ],
      "key": {
        "B": [
          "minecraft:gold_block"
        ],
        "M": [
          "minecraft:gold_ingot"
        ],
        "S": [
          "minecraft:stick"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:gold_ingot",
            "count": 11
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simplehammers:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplehammers:guide_book",
        "count": 1
      },
      "source": "modules/simplehammers/shared/resources/data/simplehammers/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:iron_ingot"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:iron_ingot"
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
            "id": "minecraft:iron_ingot",
            "count": 1
          },
          {
            "id": "minecraft:leather",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simplehammers:iron_sledgehammer",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": null,
      "result": {
        "id": "simplehammers:iron_sledgehammer",
        "count": 1
      },
      "source": "modules/simplehammers/shared/resources/data/simplehammers/recipe/iron_sledgehammer.json",
      "ingredients": [
        "minecraft:iron_block",
        "minecraft:iron_ingot",
        "minecraft:stick"
      ],
      "pattern": [
        "BMM",
        " S ",
        " S "
      ],
      "key": {
        "B": [
          "minecraft:iron_block"
        ],
        "M": [
          "minecraft:iron_ingot"
        ],
        "S": [
          "minecraft:stick"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:iron_ingot",
            "count": 11
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simplehammers:netherite_sledgehammer_smithing",
      "type": "minecraft:smithing_transform",
      "category": null,
      "group": null,
      "result": {
        "id": "simplehammers:netherite_sledgehammer",
        "count": 1
      },
      "source": "modules/simplehammers/shared/resources/data/simplehammers/recipe/netherite_sledgehammer_smithing.json",
      "ingredients": [
        "minecraft:netherite_ingot",
        "minecraft:netherite_upgrade_smithing_template",
        "simplehammers:diamond_sledgehammer"
      ],
      "slots": {
        "template": [
          "minecraft:netherite_upgrade_smithing_template"
        ],
        "base": [
          "simplehammers:diamond_sledgehammer"
        ],
        "addition": [
          "minecraft:netherite_ingot"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 11
          },
          {
            "id": "minecraft:netherite_ingot",
            "count": 1
          },
          {
            "id": "minecraft:netherite_upgrade_smithing_template",
            "count": 1
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simplehammers:stone_sledgehammer",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": null,
      "result": {
        "id": "simplehammers:stone_sledgehammer",
        "count": 1
      },
      "source": "modules/simplehammers/shared/resources/data/simplehammers/recipe/stone_sledgehammer.json",
      "ingredients": [
        "minecraft:cobblestone",
        "minecraft:iron_ingot",
        "minecraft:stick"
      ],
      "pattern": [
        "BMM",
        " S ",
        " S "
      ],
      "key": {
        "B": [
          "minecraft:iron_ingot"
        ],
        "M": [
          "minecraft:cobblestone"
        ],
        "S": [
          "minecraft:stick"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:cobblestone",
            "count": 2
          },
          {
            "id": "minecraft:iron_ingot",
            "count": 1
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simplehammers:upgrade_copper_sledgehammer_to_iron_sledgehammer",
      "type": "simplebuilding:count_based_smithing",
      "category": null,
      "group": null,
      "result": {
        "id": "simplehammers:iron_sledgehammer",
        "count": 1
      },
      "source": "modules/simplehammers/shared/resources/data/simplehammers/recipe/upgrade_copper_sledgehammer_to_iron_sledgehammer.json",
      "ingredients": [
        "minecraft:iron_ingot",
        "simplehammers:copper_sledgehammer",
        "simplelib:basic_upgrade_template"
      ],
      "slots": {
        "template": [
          "simplelib:basic_upgrade_template"
        ],
        "base": [
          "simplehammers:copper_sledgehammer"
        ],
        "addition": [
          "minecraft:iron_ingot"
        ]
      },
      "addition_count": 22,
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:iron_ingot",
            "count": 22
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 11
          },
          {
            "id": "simplelib:basic_upgrade_template",
            "count": 1
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simplehammers:upgrade_gold_sledgehammer_to_diamond_sledgehammer",
      "type": "simplebuilding:count_based_smithing",
      "category": null,
      "group": null,
      "result": {
        "id": "simplehammers:diamond_sledgehammer",
        "count": 1
      },
      "source": "modules/simplehammers/shared/resources/data/simplehammers/recipe/upgrade_gold_sledgehammer_to_diamond_sledgehammer.json",
      "ingredients": [
        "minecraft:diamond",
        "simplehammers:gold_sledgehammer",
        "simplelib:basic_upgrade_template"
      ],
      "slots": {
        "template": [
          "simplelib:basic_upgrade_template"
        ],
        "base": [
          "simplehammers:gold_sledgehammer"
        ],
        "addition": [
          "minecraft:diamond"
        ]
      },
      "addition_count": 22,
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 22
          },
          {
            "id": "minecraft:gold_ingot",
            "count": 11
          },
          {
            "id": "simplelib:basic_upgrade_template",
            "count": 1
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.25
          }
        ]
      }
    },
    {
      "id": "simplehammers:upgrade_iron_sledgehammer_to_gold_sledgehammer",
      "type": "simplebuilding:count_based_smithing",
      "category": null,
      "group": null,
      "result": {
        "id": "simplehammers:gold_sledgehammer",
        "count": 1
      },
      "source": "modules/simplehammers/shared/resources/data/simplehammers/recipe/upgrade_iron_sledgehammer_to_gold_sledgehammer.json",
      "ingredients": [
        "minecraft:gold_ingot",
        "simplehammers:iron_sledgehammer",
        "simplelib:basic_upgrade_template"
      ],
      "slots": {
        "template": [
          "simplelib:basic_upgrade_template"
        ],
        "base": [
          "simplehammers:iron_sledgehammer"
        ],
        "addition": [
          "minecraft:gold_ingot"
        ]
      },
      "addition_count": 22,
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:gold_ingot",
            "count": 22
          },
          {
            "id": "minecraft:iron_ingot",
            "count": 11
          },
          {
            "id": "simplelib:basic_upgrade_template",
            "count": 1
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.25
          }
        ]
      }
    }
  ],
  "lootTables": [],
  "tags": [
    {
      "id": "simplehammers:item/radius_enchantable",
      "replace": false,
      "values": [
        {
          "id": "#simplehammers:sledgehammers",
          "required": true
        },
        {
          "id": "#simplebuilding:radius_enchantable",
          "required": false
        }
      ],
      "source": "modules/simplehammers/shared/resources/data/simplehammers/tags/item/radius_enchantable.json"
    },
    {
      "id": "simplehammers:item/sledgehammers",
      "replace": false,
      "values": [
        {
          "id": "simplehammers:stone_sledgehammer",
          "required": true
        },
        {
          "id": "simplehammers:copper_sledgehammer",
          "required": true
        },
        {
          "id": "simplehammers:iron_sledgehammer",
          "required": true
        },
        {
          "id": "simplehammers:gold_sledgehammer",
          "required": true
        },
        {
          "id": "simplehammers:diamond_sledgehammer",
          "required": true
        },
        {
          "id": "simplehammers:netherite_sledgehammer",
          "required": true
        },
        {
          "id": "simplehammers:enderite_sledgehammer",
          "required": true
        }
      ],
      "source": "modules/simplehammers/shared/resources/data/simplehammers/tags/item/sledgehammers.json"
    }
  ],
  "advancements": [],
  "enchantments": [
    {
      "id": "simplehammers:break_through",
      "name": {
        "en_us": "Break Through",
        "de_de": "Durchbruch"
      },
      "description": {
        "en_us": ""
      },
      "maxLevel": 2,
      "weight": 2,
      "anvilCost": 4,
      "slots": [
        "mainhand"
      ],
      "supportedItems": "#simplehammers:sledgehammers",
      "primaryItems": "#simplehammers:sledgehammers",
      "exclusiveSet": null,
      "effects": [],
      "implementedIn": "none",
      "hasEffect": false,
      "source": "modules/simplehammers/shared/resources/data/simplehammers/enchantment/break_through.json",
      "note": null
    },
    {
      "id": "simplehammers:override",
      "name": {
        "en_us": "Override",
        "de_de": "Übersteuerung"
      },
      "description": {
        "en_us": ""
      },
      "maxLevel": 2,
      "weight": 2,
      "anvilCost": 4,
      "slots": [
        "mainhand"
      ],
      "supportedItems": "#simplehammers:sledgehammers",
      "primaryItems": "#simplehammers:sledgehammers",
      "exclusiveSet": null,
      "effects": [],
      "implementedIn": "none",
      "hasEffect": false,
      "source": "modules/simplehammers/shared/resources/data/simplehammers/enchantment/override.json",
      "note": null
    },
    {
      "id": "simplehammers:radius",
      "name": {
        "en_us": "Radius",
        "de_de": "Radius"
      },
      "description": {
        "en_us": ""
      },
      "maxLevel": 1,
      "weight": 1,
      "anvilCost": 8,
      "slots": [
        "mainhand"
      ],
      "supportedItems": "#simplehammers:radius_enchantable",
      "primaryItems": "#simplehammers:sledgehammers",
      "exclusiveSet": null,
      "effects": [],
      "implementedIn": "none",
      "hasEffect": false,
      "source": "modules/simplehammers/shared/resources/data/simplehammers/enchantment/radius.json",
      "note": null
    }
  ],
  "items": [
    {
      "id": "simplehammers:copper_sledgehammer",
      "name": {
        "en_us": "Copper Sledgehammer",
        "de_de": "Kupfer-Vorschlaghammer"
      },
      "note": {
        "sources": [
          "modules/simplehammers/shared/java/com/simplehammers/item/SledgehammerItem.java",
          "modules/simplehammers/shared/java/com/simplehammers/registry/HammerRegistry.java"
        ],
        "en": {
          "summary": "A sledgehammer: mines the 3x3 area in front of you, sneak + hold use carves an octet, right-click crushes diamond and quartz blocks, hold use with material in the off hand upgrades blocks in the world."
        },
        "de": {
          "summary": "Ein Vorschlaghammer: baut die 3x3-Fläche vor dir ab, Schleichen + Benutzen halten trägt ein Achtel ab, Rechtsklick zerschlägt Diamant- und Quarzblöcke, Benutzen halten mit Material in der Nebenhand wertet Blöcke in der Welt auf."
        }
      },
      "texture": "assets/textures/simplehammers/item/copper_sledgehammer.png",
      "craftedBy": [
        "simplehammers:copper_sledgehammer"
      ],
      "usedIn": [
        "simplehammers:upgrade_copper_sledgehammer_to_iron_sledgehammer"
      ]
    },
    {
      "id": "simplehammers:diamond_sledgehammer",
      "name": {
        "en_us": "Diamond Sledgehammer",
        "de_de": "Diamant-Vorschlaghammer"
      },
      "note": {
        "sources": [
          "modules/simplehammers/shared/java/com/simplehammers/item/SledgehammerItem.java",
          "modules/simplehammers/shared/java/com/simplehammers/registry/HammerRegistry.java"
        ],
        "en": {
          "summary": "A sledgehammer: mines the 3x3 area in front of you, sneak + hold use carves an octet, right-click crushes diamond and quartz blocks, hold use with material in the off hand upgrades blocks in the world."
        },
        "de": {
          "summary": "Ein Vorschlaghammer: baut die 3x3-Fläche vor dir ab, Schleichen + Benutzen halten trägt ein Achtel ab, Rechtsklick zerschlägt Diamant- und Quarzblöcke, Benutzen halten mit Material in der Nebenhand wertet Blöcke in der Welt auf."
        }
      },
      "texture": "assets/textures/simplehammers/item/diamond_sledgehammer.png",
      "craftedBy": [
        "simplehammers:diamond_sledgehammer",
        "simplehammers:upgrade_gold_sledgehammer_to_diamond_sledgehammer"
      ],
      "usedIn": [
        "simplehammers:netherite_sledgehammer_smithing"
      ]
    },
    {
      "id": "simplehammers:enderite_sledgehammer",
      "name": {
        "en_us": "Enderite Sledgehammer",
        "de_de": "Enderit-Vorschlaghammer"
      },
      "note": {
        "sources": [
          "modules/simplehammers/shared/java/com/simplehammers/item/SledgehammerItem.java",
          "modules/simplehammers/shared/java/com/simplehammers/registry/HammerRegistry.java"
        ],
        "en": {
          "summary": "A sledgehammer: mines the 3x3 area in front of you, sneak + hold use carves an octet, right-click crushes diamond and quartz blocks, hold use with material in the off hand upgrades blocks in the world."
        },
        "de": {
          "summary": "Ein Vorschlaghammer: baut die 3x3-Fläche vor dir ab, Schleichen + Benutzen halten trägt ein Achtel ab, Rechtsklick zerschlägt Diamant- und Quarzblöcke, Benutzen halten mit Material in der Nebenhand wertet Blöcke in der Welt auf."
        }
      },
      "texture": "assets/textures/simplehammers/item/enderite_sledgehammer.png",
      "craftedBy": [
        "simplehammers:enderite_sledgehammer_smithing"
      ],
      "usedIn": []
    },
    {
      "id": "simplehammers:gold_sledgehammer",
      "name": {
        "en_us": "Gold Sledgehammer",
        "de_de": "Gold-Vorschlaghammer"
      },
      "note": {
        "sources": [
          "modules/simplehammers/shared/java/com/simplehammers/item/SledgehammerItem.java",
          "modules/simplehammers/shared/java/com/simplehammers/registry/HammerRegistry.java"
        ],
        "en": {
          "summary": "A sledgehammer: mines the 3x3 area in front of you, sneak + hold use carves an octet, right-click crushes diamond and quartz blocks, hold use with material in the off hand upgrades blocks in the world."
        },
        "de": {
          "summary": "Ein Vorschlaghammer: baut die 3x3-Fläche vor dir ab, Schleichen + Benutzen halten trägt ein Achtel ab, Rechtsklick zerschlägt Diamant- und Quarzblöcke, Benutzen halten mit Material in der Nebenhand wertet Blöcke in der Welt auf."
        }
      },
      "texture": "assets/textures/simplehammers/item/gold_sledgehammer.png",
      "craftedBy": [
        "simplehammers:gold_sledgehammer",
        "simplehammers:upgrade_iron_sledgehammer_to_gold_sledgehammer"
      ],
      "usedIn": [
        "simplehammers:upgrade_gold_sledgehammer_to_diamond_sledgehammer"
      ]
    },
    {
      "id": "simplehammers:guide_book",
      "name": {
        "en_us": "Simple Hammers Guide",
        "de_de": "Simple-Hammers-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simplehammers/shared/java/com/simplehammers/guide/HammersGuide.java",
          "modules/simplehammers/shared/resources/data/simplehammers/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 20 pages taken from this wiki, shown in your language. Shapeless recipe: book + iron ingot. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Hammers\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 20 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Eisenbarren. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Hammers\" eins."
        }
      },
      "texture": "assets/textures/simplehammers/item/guide_book.png",
      "craftedBy": [
        "simplehammers:guide_book"
      ],
      "usedIn": []
    },
    {
      "id": "simplehammers:iron_sledgehammer",
      "name": {
        "en_us": "Iron Sledgehammer",
        "de_de": "Eisen-Vorschlaghammer"
      },
      "note": {
        "sources": [
          "modules/simplehammers/shared/java/com/simplehammers/item/SledgehammerItem.java",
          "modules/simplehammers/shared/java/com/simplehammers/registry/HammerRegistry.java"
        ],
        "en": {
          "summary": "A sledgehammer: mines the 3x3 area in front of you, sneak + hold use carves an octet, right-click crushes diamond and quartz blocks, hold use with material in the off hand upgrades blocks in the world."
        },
        "de": {
          "summary": "Ein Vorschlaghammer: baut die 3x3-Fläche vor dir ab, Schleichen + Benutzen halten trägt ein Achtel ab, Rechtsklick zerschlägt Diamant- und Quarzblöcke, Benutzen halten mit Material in der Nebenhand wertet Blöcke in der Welt auf."
        }
      },
      "texture": "assets/textures/simplehammers/item/iron_sledgehammer.png",
      "craftedBy": [
        "simplehammers:iron_sledgehammer",
        "simplehammers:upgrade_copper_sledgehammer_to_iron_sledgehammer"
      ],
      "usedIn": [
        "simplehammers:upgrade_iron_sledgehammer_to_gold_sledgehammer"
      ]
    },
    {
      "id": "simplehammers:netherite_sledgehammer",
      "name": {
        "en_us": "Netherite Sledgehammer",
        "de_de": "Netherit-Vorschlaghammer"
      },
      "note": {
        "sources": [
          "modules/simplehammers/shared/java/com/simplehammers/item/SledgehammerItem.java",
          "modules/simplehammers/shared/java/com/simplehammers/registry/HammerRegistry.java"
        ],
        "en": {
          "summary": "A sledgehammer: mines the 3x3 area in front of you, sneak + hold use carves an octet, right-click crushes diamond and quartz blocks, hold use with material in the off hand upgrades blocks in the world."
        },
        "de": {
          "summary": "Ein Vorschlaghammer: baut die 3x3-Fläche vor dir ab, Schleichen + Benutzen halten trägt ein Achtel ab, Rechtsklick zerschlägt Diamant- und Quarzblöcke, Benutzen halten mit Material in der Nebenhand wertet Blöcke in der Welt auf."
        }
      },
      "texture": "assets/textures/simplehammers/item/netherite_sledgehammer.png",
      "craftedBy": [
        "simplehammers:netherite_sledgehammer_smithing"
      ],
      "usedIn": [
        "simplehammers:enderite_sledgehammer_smithing"
      ]
    },
    {
      "id": "simplehammers:stone_sledgehammer",
      "name": {
        "en_us": "Stone Sledgehammer",
        "de_de": "Stein-Vorschlaghammer"
      },
      "note": {
        "sources": [
          "modules/simplehammers/shared/java/com/simplehammers/item/SledgehammerItem.java",
          "modules/simplehammers/shared/java/com/simplehammers/registry/HammerRegistry.java"
        ],
        "en": {
          "summary": "A sledgehammer: mines the 3x3 area in front of you, sneak + hold use carves an octet, right-click crushes diamond and quartz blocks, hold use with material in the off hand upgrades blocks in the world."
        },
        "de": {
          "summary": "Ein Vorschlaghammer: baut die 3x3-Fläche vor dir ab, Schleichen + Benutzen halten trägt ein Achtel ab, Rechtsklick zerschlägt Diamant- und Quarzblöcke, Benutzen halten mit Material in der Nebenhand wertet Blöcke in der Welt auf."
        }
      },
      "texture": "assets/textures/simplehammers/item/stone_sledgehammer.png",
      "craftedBy": [
        "simplehammers:stone_sledgehammer"
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
  "undocumented": [
    "simplehammers:break_through",
    "simplehammers:override",
    "simplehammers:radius"
  ],
  "incompleteProse": {},
  "counts": {
    "features": 9,
    "recipes": 11,
    "lootTables": 0,
    "tags": 2,
    "advancements": 0,
    "enchantments": 3,
    "items": 8,
    "blocks": 0,
    "trades": 0,
    "config": 0,
    "quests": 0,
    "recipesOtherLines": 0,
    "undocumented": 3,
    "inWorld": 0,
    "incompleteProse": 0
  }
};
