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
        "title": "Claims: Complete, Off by Default",
        "summary": "All six stages are implemented: atomic claims with caps, Vanilla/tool checks at every target, explosions, fire, pistons, fluids, automation, projectiles, mob griefing, vehicles, owner trust, OP4 administration and the Dimensions adapter. Mobs still fight normally inside claims. Off until the owner releases it; disabled hooks do not read claim data. Default: false.",
        "details": [
          "All six stages are implemented: atomic claims with caps, Vanilla/tool checks at every target, explosions, fire, pistons, fluids, automation, projectiles, mob griefing, vehicles, owner trust, OP4 administration and the Dimensions adapter. Mobs still fight normally inside claims. Off until the owner releases it; disabled hooks do not read claim data. Default: false.",
          "Automation checks source and destination ownership, including known bed/chest counterparts and composters. Known Vanilla and SimpleBuilding piston/hopper hooks and placed attractors are guarded. Explosions never break claimed blocks or hurt claimed animals and decorations; players still take explosion damage.",
          "Plain dispenser ejection and droppers work within one owner; other dispenser behaviors stay blocked near claims. Copper golems remember the land their load came from and only put it down on that same land, so they work inside a claim but never carry items out or in. Crafters check destinations before consuming ingredients. Unknown containers are refused while enabled.",
          "Mob griefing: Endermen, Withers, Ravagers, the Ender Dragon, Silverfish and door-breaking Zombies never change claimed land; other mobs (villagers, sheep, boats) only act on the land they stand on. Farmland and turtle eggs need rights to trample. Mob attacks stay Vanilla. Boats need rights to be placed; a lightning bolt counts as the player who caused it.",
          "/claim trust and untrust accept online names or full offline UUIDs. Only the owner can change trust; trusted players cannot delegate. OP4 may use admin listall, list <UUID/name>, or unclaim, independently of the default-off protection bypass. Changes are saved atomically before becoming effective.",
          "Server ticks between successful claim, trust, revocation or removal operations by one player. Range 20–72000, across dimensions; resets on restart. Default: 100 ticks."
        ]
      },
      "de": {
        "title": "Claims: fertig, standardmäßig aus",
        "summary": "Alle sechs Stufen sind umgesetzt: atomare Claims mit Limits, Vanilla-/Werkzeugprüfung an jedem Ziel, Explosionen, Feuer, Kolben, Flüssigkeiten, Automation, Projektile, Mob-Griefing, Fahrzeuge, Besitzer-Vertrauen, OP4-Verwaltung und Dimensions-Adapter. Mobs kämpfen in Claims weiter normal. Aus, bis der Besitzer freigibt; ausgeschaltete Hooks lesen keine Claim-Daten. Standardwert: false.",
        "details": [
          "Alle sechs Stufen sind umgesetzt: atomare Claims mit Limits, Vanilla-/Werkzeugprüfung an jedem Ziel, Explosionen, Feuer, Kolben, Flüssigkeiten, Automation, Projektile, Mob-Griefing, Fahrzeuge, Besitzer-Vertrauen, OP4-Verwaltung und Dimensions-Adapter. Mobs kämpfen in Claims weiter normal. Aus, bis der Besitzer freigibt; ausgeschaltete Hooks lesen keine Claim-Daten. Standardwert: false.",
          "Automation prüft Besitz an Quelle und Ziel einschließlich bekannter Bett-/Truhengegenstücke und Komposter. Bekannte Vanilla- und SimpleBuilding-Kolben-/Hopper-Hooks sowie platzierte Attractors sind angebunden. Explosionen zerstören keine beanspruchten Blöcke und verletzen keine beanspruchten Tiere oder Dekorationen; Spieler erleiden weiter Explosionsschaden.",
          "Einfache Dispenser-Auswürfe und Dropper funktionieren innerhalb desselben Besitzes; andere Dispenser-Behaviors bleiben in Claim-Nähe gesperrt. Kupfergolems merken sich, von welchem Land ihre Ladung stammt, und legen sie nur dort wieder ab: innerhalb eines Claims arbeiten sie, tragen aber nichts hinaus oder hinein. Crafter prüfen Ziele vor dem Zutatenverbrauch. Unbekannte Container werden bei aktiviertem Schutz verweigert.",
          "Mob-Griefing: Endermen, Wither, Verwüster, Enderdrache, Silberfischchen und türbrechende Zombies verändern kein beanspruchtes Land; andere Mobs (Dorfbewohner, Schafe, Boote) wirken nur auf dem Land, auf dem sie stehen. Ackerboden und Schildkröteneier zu zertrampeln braucht Rechte. Mob-Angriffe bleiben Vanilla. Boote zu setzen braucht Rechte; ein Blitz zählt als der Spieler, der ihn ausgelöst hat.",
          "/claim trust und untrust nehmen Online-Namen oder vollständige Offline-UUIDs an. Nur der Besitzer ändert Vertrauen; Vertraute dürfen es nicht weitergeben. OP4 darf admin listall, list <UUID/Name> und unclaim nutzen, unabhängig von der standardmäßig ausgeschalteten Schutzumgehung. Änderungen gelten erst nach atomarem Speichern.",
          "Server-Ticks zwischen erfolgreichen Claim-, Vertrauens-, Widerrufs- oder Löschvorgängen eines Spielers. Bereich 20–72000, dimensionsübergreifend; Neustart setzt zurück. Standard: 100 Ticks."
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
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/mixin/claims/ClaimTransportMixin.java"
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
  "tags": [],
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
          "summary": "Legacy deed from existing worlds or /give. Stack size 16. Original texture and custom data are retained. No recipe. Claims are disabled by default; enabled deeds request ownership from the server."
        },
        "de": {
          "summary": "Alte Urkunde aus vorhandenen Welten oder /give. Stapelgröße 16. Originaltextur und Zusatzdaten bleiben erhalten. Kein Rezept. Claims sind standardmäßig aus; aktiviert beantragt die Urkunde Besitz beim Server."
        }
      },
      "texture": "assets/textures/simpletweaks/item/claim_deed.png",
      "questionary": {
        "en": {
          "lore": [
            "A deed from an older world, still stamped with its first owner."
          ],
          "use": [
            "A legacy item without a recipe, kept for existing worlds or /give. Stacks to 16. Claims are off by default; once enabled the deed asks the server for ownership."
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
            "Ein altes Item ohne Rezept, nur für bestehende Welten oder /give. Stapelt bis 16. Claims sind standardmäßig aus; sind sie aktiv, beantragt die Urkunde Besitz beim Server."
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
          "summary": "Guide to this mod: 6 pages taken from this wiki, shown in your language. Shapeless recipe: book + paper. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Tweaks\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 6 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Papier. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Tweaks\" eins."
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
    "tags": 0,
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
