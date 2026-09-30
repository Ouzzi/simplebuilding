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
        "summary": "Stage 1 adds bounded server-owned claims, atomic storage, legacy data reading, a command skeleton and deeds. Protection is incomplete. The master switch is off; disabled mode never reads or writes claim data. Enable only for development testing.",
        "details": [
          "Stage 1 adds bounded server-owned claims, atomic storage, legacy data reading, a command skeleton and deeds. Protection is incomplete. The master switch is off; disabled mode never reads or writes claim data. Enable only for development testing."
        ]
      },
      "de": {
        "title": "Claims: standardmäßig aus",
        "summary": "Stufe 1 ergänzt begrenzte Server-Claims, atomare Speicherung, Altdatenlesen, Befehlsgrundlage und Urkunden. Schutz ist unvollständig. Der Hauptschalter ist aus; ausgeschaltet werden Claim-Daten weder gelesen noch geschrieben. Nur für Entwicklungstests aktivieren.",
        "details": [
          "Stufe 1 ergänzt begrenzte Server-Claims, atomare Speicherung, Altdatenlesen, Befehlsgrundlage und Urkunden. Schutz ist unvollständig. Der Hauptschalter ist aus; ausgeschaltet werden Claim-Daten weder gelesen noch geschrieben. Nur für Entwicklungstests aktivieren."
        ]
      },
      "sources": [
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/LegacyDeed.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/Claims.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimConfig.java",
        "modules/simpletweaks/shared/java/com/simplebuilding/modules/simpletweaks/claims/ClaimStore.java"
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
  "recipes": [],
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
      "texture": "assets/textures/simpletweaks/item/claim_deed.png"
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
    "recipes": 0,
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
