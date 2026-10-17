window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simpleloot"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simpleloot",
    "name": "Simple Loot",
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
      "id": "vault",
      "sources": [
        "modules/simpleloot/shared/java/com/simplebuilding/modules/simpleloot/mixin/VaultServerDataMixin.java"
      ],
      "en": {
        "title": "Vault cooldown",
        "summary": "A player may loot a vault again after vaultCooldownDays, default 100 Minecraft days, bounded 1–36500. The original SimpleBuildingLootTimes UUID-to-time map is retained. Missing timestamps on already rewarded entries start a cooldown rather than granting free repeat loot; backwards time does not unlock rewards."
      },
      "de": {
        "title": "Tresor-Abklingzeit",
        "summary": "Ein Spieler darf nach vaultCooldownDays erneut plundern, Standard 100 Minecraft-Tage, Grenze 1–36500. Die originale SimpleBuildingLootTimes-UUID-Zeitkarte bleibt. Fehlende Zeitstempel bereits belohnter Spieler beginnen eine Pause statt Gratisbeute; rückwärts laufende Zeit entsperrt nichts."
      }
    },
    {
      "id": "config_vaultCooldownDays",
      "sources": [
        "modules/simpleloot/shared/java/com/simplebuilding/modules/simpleloot/LootConfig.java"
      ],
      "en": {
        "title": "vaultCooldownDays",
        "summary": "Server setting. Default: 100."
      },
      "de": {
        "title": "vaultCooldownDays",
        "summary": "Servereinstellung. Standard: 100."
      }
    },
    {
      "id": "config_enableVaultCooldown",
      "sources": [
        "modules/simpleloot/shared/java/com/simplebuilding/modules/simpleloot/LootConfig.java"
      ],
      "en": {
        "title": "Repeat Vault Rewards",
        "summary": "Allow repeat vault rewards after the server cooldown. Disable to retain Vanilla one-time rewards. Default: true."
      },
      "de": {
        "title": "Erneute Tresorbelohnungen",
        "summary": "Erneute Tresorbelohnungen nach der Serverpause erlauben. Abschalten behält einmalige Vanilla-Belohnungen. Standard: true."
      }
    },
    {
      "id": "commands",
      "sources": [
        "modules/simpleloot/shared/java/com/simplebuilding/modules/simpleloot/LootCommands.java"
      ],
      "en": {
        "title": "Commands",
        "summary": "Owner-level /simpleloot vaults cooldown <days> and /simpleloot vaults repeat <true|false>; changes are saved to config/simpleloot.json."
      },
      "de": {
        "title": "Befehle",
        "summary": "Besitzerberechtigung: /simpleloot vaults cooldown <Tage> und /simpleloot vaults repeat <true|false>; Speichern in config/simpleloot.json."
      }
    },
    {
      "id": "submod",
      "sources": [
        "modules/simpleloot/shared/java/com/simplebuilding/modules/simpleloot/SimpleLoot.java",
        "framework/src/main/java/com/simplebuilding/framework/api/SubMods.java"
      ],
      "en": {
        "title": "Sub-mod of Simple QoL",
        "summary": "Playable alone (config/simpleloot.json). Bundled in Simple QoL, which can switch it off with \"Enable Simple Loot\". On the first start the old Simple QoL settings of this sub-mod are taken over once."
      },
      "de": {
        "title": "Sub-Mod von Simple QoL",
        "summary": "Allein spielbar (config/simpleloot.json). In Simple QoL gebuendelt, dort mit \"Simple Loot aktivieren\" abschaltbar. Beim ersten Start werden die alten Simple-QoL-Einstellungen dieser Sub-Mod einmalig übernommen."
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
    "features": 5,
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
