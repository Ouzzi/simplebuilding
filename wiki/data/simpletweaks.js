window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simpletweaks"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simpletweaks",
    "name": "Simple Tweaks",
    "version": "1.2.12",
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
      "id": "legacy_compatibility",
      "en": {
        "title": "Legacy World Compatibility",
        "summary": "An exact whitelist resolves old item, block, block entity, and component names to existing SimpleBuilding registries. Saving uses the current names. No duplicate gameplay content is registered.",
        "details": [
          "An exact whitelist resolves old item, block, block entity, and component names to existing SimpleBuilding registries. Saving uses the current names. No duplicate gameplay content is registered."
        ]
      },
      "de": {
        "title": "Kompatibilitaet alter Welten",
        "summary": "Eine exakte Liste verbindet alte Item-, Block-, Blockentitaets- und Komponentennamen mit vorhandenen SimpleBuilding-Registries. Gespeichert werden aktuelle Namen; Spielinhalte werden nicht doppelt registriert.",
        "details": [
          "Eine exakte Liste verbindet alte Item-, Block-, Blockentitaets- und Komponentennamen mit vorhandenen SimpleBuilding-Registries. Gespeichert werden aktuelle Namen; Spielinhalte werden nicht doppelt registriert."
        ]
      },
      "sources": [
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/LegacyAliases.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/LegacyRegistryMixin.java"
      ]
    },
    {
      "id": "claims",
      "en": {
        "title": "Claims (On by Default)",
        "summary": "Server-authoritative claims, on by default: /claim create (or a legacy deed) claims the chunk you stand in; only you and players you trust may build, break, open containers, use vehicles or take items there. Explosions, fire, fluids, pistons, automation, projectiles, mob griefing, attractors and SimpleBuilding's area tools respect claims. Mobs still fight normally inside claims. Server admins can switch claims off in config/simpletweaks-claims.json; an existing file keeps its value. Default: true.",
        "details": [
          "Server-authoritative claims, on by default: /claim create (or a legacy deed) claims the chunk you stand in; only you and players you trust may build, break, open containers, use vehicles or take items there. Explosions, fire, fluids, pistons, automation, projectiles, mob griefing, attractors and SimpleBuilding's area tools respect claims. Mobs still fight normally inside claims. Server admins can switch claims off in config/simpletweaks-claims.json; an existing file keeps its value. Default: true.",
          "Automation checks source and destination ownership, including known bed/chest counterparts and composters. Known Vanilla and SimpleBuilding piston/hopper hooks and placed or held attractors are guarded. Explosions never break claimed blocks or hurt claimed animals and decorations; players still take explosion damage.",
          "Plain dispenser ejection and droppers work within one owner; other dispenser behaviors stay blocked near claims. Copper golems remember the land their load came from and only put it down on that same land, so they work inside a claim but never carry items out or in. Crafters check destinations before consuming ingredients. Unknown containers are refused while enabled.",
          "Mob griefing: Endermen, Withers, Ravagers, the Ender Dragon, Silverfish and door-breaking Zombies never change claimed land; other mobs (villagers, sheep, boats) only act on the land they stand on. Farmland and turtle eggs need rights to trample. Mob attacks stay Vanilla. Boats need rights to be placed; a lightning bolt counts as the player who caused it.",
          "/claim create claims the current chunk (limits, cooldown, world border, spawn buffer, dimension list); /claim unclaim releases it. /claim trust and untrust accept online names or full offline UUIDs. Only the owner can change trust; trusted players cannot delegate. OP4 may use admin listall, list <UUID/name>, or unclaim, independently of the default-off protection bypass. Changes are saved atomically before becoming effective.",
          "Server ticks between successful claim, trust, revocation or removal operations by one player. Range 20–72000, across dimensions; resets on restart. Default: 100 ticks.",
          "Rights per person (server-side): B Build, D Destroy, U Use (doors, levers, workbenches), C Container (chests, frames, pickup), R Redstone, A Animals and vehicles, M Manage. Roles: @guest = U, @helper = UCA, @member = BDUCRA (what \"trusted\" always meant), @manager = all. Unknown actions need B and D. /claim trust <player> [rights] sets them for the chunk you stand in, /claim info shows yours, /claim code shows the code of your land. The code (at most 4096 characters, 200 lines) is parsed and checked only by the server; the ledger is version 2 and keeps a copy of the old file (simpletweaks-claims.v1.json) on the first write."
        ]
      },
      "de": {
        "title": "Claims (standardmäßig an)",
        "summary": "Serverseitige Claims, standardmäßig an: /claim create (oder eine alte Urkunde) beansprucht den Chunk, in dem du stehst; nur du und vertraute Spieler dürfen dort bauen, abbauen, Container öffnen, Fahrzeuge nutzen oder Items nehmen. Explosionen, Feuer, Flüssigkeiten, Kolben, Automation, Projektile, Mob-Griefing, Attractors und die Flächenwerkzeuge von SimpleBuilding respektieren Claims. Mobs kämpfen in Claims weiter normal. Server-Admins schalten Claims in config/simpletweaks-claims.json aus; eine bestehende Datei behält ihren Wert. Standardwert: true.",
        "details": [
          "Serverseitige Claims, standardmäßig an: /claim create (oder eine alte Urkunde) beansprucht den Chunk, in dem du stehst; nur du und vertraute Spieler dürfen dort bauen, abbauen, Container öffnen, Fahrzeuge nutzen oder Items nehmen. Explosionen, Feuer, Flüssigkeiten, Kolben, Automation, Projektile, Mob-Griefing, Attractors und die Flächenwerkzeuge von SimpleBuilding respektieren Claims. Mobs kämpfen in Claims weiter normal. Server-Admins schalten Claims in config/simpletweaks-claims.json aus; eine bestehende Datei behält ihren Wert. Standardwert: true.",
          "Automation prüft Besitz an Quelle und Ziel einschließlich bekannter Bett-/Truhengegenstücke und Komposter. Bekannte Vanilla- und SimpleBuilding-Kolben-/Hopper-Hooks sowie platzierte und gehaltene Attractors sind angebunden. Explosionen zerstören keine beanspruchten Blöcke und verletzen keine beanspruchten Tiere oder Dekorationen; Spieler erleiden weiter Explosionsschaden.",
          "Einfache Dispenser-Auswürfe und Dropper funktionieren innerhalb desselben Besitzes; andere Dispenser-Behaviors bleiben in Claim-Nähe gesperrt. Kupfergolems merken sich, von welchem Land ihre Ladung stammt, und legen sie nur dort wieder ab: innerhalb eines Claims arbeiten sie, tragen aber nichts hinaus oder hinein. Crafter prüfen Ziele vor dem Zutatenverbrauch. Unbekannte Container werden bei aktiviertem Schutz verweigert.",
          "Mob-Griefing: Endermen, Wither, Verwüster, Enderdrache, Silberfischchen und türbrechende Zombies verändern kein beanspruchtes Land; andere Mobs (Dorfbewohner, Schafe, Boote) wirken nur auf dem Land, auf dem sie stehen. Ackerboden und Schildkröteneier zu zertrampeln braucht Rechte. Mob-Angriffe bleiben Vanilla. Boote zu setzen braucht Rechte; ein Blitz zählt als der Spieler, der ihn ausgelöst hat.",
          "/claim create beansprucht den aktuellen Chunk (Limits, Abklingzeit, Weltgrenze, Spawnpuffer, Dimensionsliste); /claim unclaim gibt ihn frei. /claim trust und untrust nehmen Online-Namen oder vollständige Offline-UUIDs an. Nur der Besitzer ändert Vertrauen; Vertraute dürfen es nicht weitergeben. OP4 darf admin listall, list <UUID/Name> und unclaim nutzen, unabhängig von der standardmäßig ausgeschalteten Schutzumgehung. Änderungen gelten erst nach atomarem Speichern.",
          "Server-Ticks zwischen erfolgreichen Claim-, Vertrauens-, Widerrufs- oder Löschvorgängen eines Spielers. Bereich 20–72000, dimensionsübergreifend; Neustart setzt zurück. Standard: 100 Ticks.",
          "Rechte je Person (serverseitig): B Bauen, D Abbauen, U Benutzen (Türen, Hebel, Werkbänke), C Container (Truhen, Rahmen, Aufheben), R Redstone, A Tiere und Fahrzeuge, M Verwalten. Rollen: @guest = U, @helper = UCA, @member = BDUCRA (so viel bedeutete „vertraut“ immer), @manager = alles. Unbekannte Aktionen brauchen B und D. /claim trust <Spieler> [Rechte] setzt sie für den Chunk, in dem du stehst, /claim info zeigt deine, /claim code zeigt den Code deines Landes. Der Code (höchstens 4096 Zeichen, 200 Zeilen) wird nur vom Server geparst und geprüft; das Ledger ist Version 2 und sichert beim ersten Schreiben die alte Datei (simpletweaks-claims.v1.json)."
        ]
      },
      "sources": [
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/LegacyDeed.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/Claims.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimConfig.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimStore.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimGameModeMixin.java",
        "framework/src/main/java/com/simplebuilding/framework/api/Protection.java",
        "common/src/shared/java/com/simplebuilding/api/WorldPermissions.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimAutomation.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimDamageMixin.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimExplosionMixin.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimCommands.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimGriefMixin.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimEndermanTakeMixin.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimDoorMixin.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimBoatMixin.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimTransportMixin.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimRights.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimCode.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimItemUseMixin.java"
      ]
    },
    {
      "id": "forge_support",
      "sources": [
        "modules/simpletweaks/forge/src/main/java/com/simplebuilding/modules/simpletweaks/forge/TweaksForge.java",
        "modules/simpletweaks/forge/src/main/java/com/simplebuilding/modules/simpletweaks/forge/mixin/NamespacedWrapperAliasMixin.java"
      ],
      "en": {
        "title": "Experimental Forge 26.3",
        "summary": "Opt-in Forge compatibility adapter, with SimpleBuilding required.",
        "details": [
          "Native Forge aliases and wrapper lookups resolve only the existing legacy whitelist. Canonical IDs are saved; no duplicate gameplay is registered. Claims remain inactive. Forge client and actual upgraded worlds remain unverified."
        ]
      },
      "de": {
        "title": "Experimentelles Forge 26.3",
        "summary": "Opt-in-Forge-Kompatibilitätsadapter; SimpleBuilding ist erforderlich.",
        "details": [
          "Native Forge-Aliase und Wrapper-Lookups lösen nur die bestehende Alt-ID-Allowlist auf. Gespeichert werden kanonische IDs; keine doppelten Spielmechaniken. Claims bleiben inaktiv. Forge-Client und echte hochgestufte Welten bleiben ungeprüft."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simpletweaks:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpletweaks:guide_book",
        "count": 1
      },
      "source": "modules/simpletweaks/shared/resources/data/simpletweaks/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:paper"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:paper"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:sugar_cane",
            "count": 4
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
  "tags": [
    {
      "id": "simpletweaks:block/claim_redstone",
      "replace": false,
      "values": [
        {
          "id": "minecraft:repeater",
          "required": true
        },
        {
          "id": "minecraft:comparator",
          "required": true
        },
        {
          "id": "minecraft:note_block",
          "required": true
        },
        {
          "id": "minecraft:daylight_detector",
          "required": true
        },
        {
          "id": "minecraft:redstone_wire",
          "required": true
        },
        {
          "id": "minecraft:crafter",
          "required": true
        }
      ],
      "source": "modules/simpletweaks/shared/resources/data/simpletweaks/tags/block/claim_redstone.json"
    },
    {
      "id": "simpletweaks:block/claim_use",
      "replace": false,
      "values": [
        {
          "id": "#minecraft:doors",
          "required": true
        },
        {
          "id": "#minecraft:trapdoors",
          "required": true
        },
        {
          "id": "#minecraft:fence_gates",
          "required": true
        },
        {
          "id": "#minecraft:buttons",
          "required": true
        },
        {
          "id": "#minecraft:pressure_plates",
          "required": true
        },
        {
          "id": "#minecraft:beds",
          "required": true
        },
        {
          "id": "#minecraft:anvil",
          "required": true
        },
        {
          "id": "minecraft:lever",
          "required": true
        },
        {
          "id": "minecraft:bell",
          "required": true
        },
        {
          "id": "minecraft:crafting_table",
          "required": true
        },
        {
          "id": "minecraft:enchanting_table",
          "required": true
        },
        {
          "id": "minecraft:grindstone",
          "required": true
        },
        {
          "id": "minecraft:stonecutter",
          "required": true
        },
        {
          "id": "minecraft:loom",
          "required": true
        },
        {
          "id": "minecraft:cartography_table",
          "required": true
        },
        {
          "id": "minecraft:smithing_table",
          "required": true
        }
      ],
      "source": "modules/simpletweaks/shared/resources/data/simpletweaks/tags/block/claim_use.json"
    }
  ],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "simpletweaks:claim_deed",
      "kind": "item",
      "name": {
        "en_us": "Legacy Claim Deed",
        "de_de": "Alte Claim-Urkunde"
      },
      "note": {
        "en": {
          "summary": "Legacy deed from existing worlds or /give. Stack size 16. Original texture and custom data are retained. No recipe; using it claims the current chunk like /claim create."
        },
        "de": {
          "summary": "Alte Urkunde aus vorhandenen Welten oder /give. Stapelgröße 16. Originaltextur und Zusatzdaten bleiben erhalten. Kein Rezept; Benutzen beansprucht den aktuellen Chunk wie /claim create."
        }
      },
      "texture": "assets/textures/simpletweaks/item/claim_deed.png",
      "questionary": {
        "en": {
          "lore": [
            "A deed from an older world, still stamped with its first owner."
          ],
          "use": [
            "A legacy item without a recipe, kept for existing worlds or /give. Stacks to 16. Using it claims the chunk you stand in, like /claim create."
          ],
          "secret": [
            "Its original texture and custom data are kept exactly, so an old deed still works after an update."
          ]
        },
        "de": {
          "lore": [
            "Eine Urkunde aus einer älteren Welt, noch mit dem ersten Besitzer gestempelt."
          ],
          "use": [
            "Ein altes Item ohne Rezept, nur für bestehende Welten oder /give. Stapelt bis 16. Benutzen beansprucht den Chunk, in dem du stehst, wie /claim create."
          ],
          "secret": [
            "Originaltextur und Zusatzdaten bleiben unverändert erhalten, eine alte Urkunde funktioniert also nach einem Update weiter."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simpletweaks:guide_book",
      "name": {
        "en_us": "Simple Tweaks Guide",
        "de_de": "Simple-Tweaks-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/guide/TweaksGuide.java",
          "modules/simpletweaks/shared/resources/data/simpletweaks/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 7 pages taken from this wiki, shown in your language. Shapeless recipe: book + paper. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Tweaks\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 7 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Papier. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Tweaks\" eins."
        }
      },
      "texture": "assets/textures/simpletweaks/item/guide_book.png",
      "questionary": {
        "en": {
          "lore": [
            "A short book that knows legacy names, claims and other tweaks."
          ],
          "use": [
            "The Simple Tweaks Guide explains legacy names, claims and other tweaks. Craft it shapeless from a book and a crafting table."
          ],
          "secret": [
            "Its chapters unlock as the matching recipes land in your recipe book. Nothing is consumed."
          ]
        },
        "de": {
          "lore": [
            "Ein kurzes Buch, das alte Namen, Claims und weitere Anpassungen kennt."
          ],
          "use": [
            "Das Simple Tweaks-Handbuch erklärt alte Namen, Claims und weitere Anpassungen. Formlos aus einem Buch und einer Werkbank herstellbar."
          ],
          "secret": [
            "Die Kapitel öffnen sich, sobald die passenden Rezepte im Rezeptbuch erscheinen. Nichts wird verbraucht."
          ]
        }
      },
      "craftedBy": [
        "simpletweaks:guide_book"
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
    "features": 3,
    "recipes": 1,
    "lootTables": 0,
    "tags": 2,
    "advancements": 0,
    "enchantments": 0,
    "items": 2,
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
