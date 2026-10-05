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
        "title": "Claims: Disabled by Default",
        "summary": "Stages 1–3, 5 and 6 plus partial stage-4 protection are implemented: atomic claims, Vanilla/tool checks, owner trust, OP4 administration and the Dimensions adapter. Automation still has unsupported paths, including lightning secondary effects and unknown mod behaviors. Not ready to enable. Off by default; disabled hooks do not read claim data. Development tests only. Default: false.",
        "details": [
          "Stages 1–3, 5 and 6 plus partial stage-4 protection are implemented: atomic claims, Vanilla/tool checks, owner trust, OP4 administration and the Dimensions adapter. Automation still has unsupported paths, including lightning secondary effects and unknown mod behaviors. Not ready to enable. Off by default; disabled hooks do not read claim data. Development tests only. Default: false.",
          "Automation checks source and destination ownership, including known bed/chest counterparts. Known Vanilla and SimpleBuilding piston/hopper hooks and placed attractors are guarded. Natural damage remains active. Explosions are blocked on claimed targets regardless of owner.",
          "Plain dispenser ejection and droppers work within one owner. Other dispenser behaviors remain blocked near claims. Unknown containers are refused when enabled. Copper golems have no trusted owner, so their transfers touching claimed land are blocked. Crafters check destinations before consuming ingredients, preventing refused fallback ejections. Arbitrary remote mod mutations remain unsupported; Claims must remain off.",
          "Lightning ignition and copper cleaning now check each target, including random neighboring targets. Lightning rod redstone and entity transformations still need protection. Lightning entity damage is currently denied conservatively. Claims is not ready for activation.",
          "/claim trust and untrust accept online names or full offline UUIDs. Only the owner can change trust; trusted players cannot delegate. OP4 may use admin listall, list <UUID/name>, or unclaim, independently of the default-off protection bypass. Changes are saved atomically before becoming effective.",
          "Server ticks between successful claim, trust, revocation or removal operations by one player. Range 20–72000, across dimensions; resets on restart. Default: 100 ticks."
        ]
      },
      "de": {
        "title": "Claims: standardmäßig aus",
        "summary": "Stufen 1–3, 5 und 6 sowie Teile des Stage-4-Schutzes sind umgesetzt: atomare Claims, Vanilla-/Werkzeugprüfungen, Besitzer-Vertrauen, OP4-Verwaltung und Dimensions-Adapter. Automation hat weiterhin ungeschützte Pfade, darunter Blitz-Sekundaerfolgen und unbekannte Mod-Behaviors. Nicht einschaltbereit. Standard aus; ausgeschaltete Hooks lesen keine Claim-Daten. Nur Entwicklungstests. Standardwert: false.",
        "details": [
          "Stufen 1–3, 5 und 6 sowie Teile des Stage-4-Schutzes sind umgesetzt: atomare Claims, Vanilla-/Werkzeugprüfungen, Besitzer-Vertrauen, OP4-Verwaltung und Dimensions-Adapter. Automation hat weiterhin ungeschützte Pfade, darunter Blitz-Sekundaerfolgen und unbekannte Mod-Behaviors. Nicht einschaltbereit. Standard aus; ausgeschaltete Hooks lesen keine Claim-Daten. Nur Entwicklungstests. Standardwert: false.",
          "Automation prüft Besitz an Quelle und Ziel einschließlich bekannter Bett-/Truhengegenstücke. Bekannte Vanilla- und SimpleBuilding-Kolben-/Hopper-Hooks sowie platzierte Attractors sind angebunden. Naturschaden bleibt wirksam. Explosionen werden auf beanspruchten Zielen unabhängig vom Besitzer verweigert.",
          "Einfache Dispenser-Auswürfe und Dropper funktionieren innerhalb desselben Besitzes. Andere Dispenser-Behaviors bleiben in Claim-Nähe gesperrt. Unbekannte Container werden bei aktiviertem Schutz verweigert. Kupfergolems haben keinen verlässlichen Besitzer; ihre Transfers mit beanspruchtem Land sind deshalb gesperrt. Crafter prüfen Ziele vor dem Zutatenverbrauch und verhindern verweigerte Ersatzauswürfe. Beliebige entfernte Mod-Mutationen bleiben ungeschützt; Claims muss aus bleiben.",
          "Blitzentzündung und Kupferreinigung prüfen nun jedes Ziel, einschließlich zufälliger Nachbarziele. Blitzableiter-Redstone und Entity-Verwandlungen benötigen weiterhin Schutz. Blitzschaden als Entity-Quelle wird momentan konservativ verweigert. Claims ist nicht einschaltbereit.",
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
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimCommands.java"
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
