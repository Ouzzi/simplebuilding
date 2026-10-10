window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simpleweather"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simpleweather",
    "name": "Simple Weather",
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
      "id": "weather",
      "sources": [
        "modules/simpleweather/shared/java/com/simplebuilding/modules/simpleweather/mixin/ServerWeatherMixin.java"
      ],
      "en": {
        "title": "Disable weather",
        "summary": "disableWeather (default false, server) clears rain and thunder and skips weather progression."
      },
      "de": {
        "title": "Wetter abschalten",
        "summary": "disableWeather (Standard false, Server) loescht Regen und Gewitter und stoppt den Wetterfortschritt."
      }
    },
    {
      "id": "rain_density",
      "sources": [
        "modules/simpleweather/shared/java/com/simplebuilding/modules/simpleweather/mixin/client/VisualRainMixin.java",
        "modules/simpleweather/shared/java/com/simplebuilding/modules/simpleweather/mixin/client/ClientWeatherMixin.java"
      ],
      "en": {
        "title": "Rain density",
        "summary": "clientRainParticleDensity is a local visual preference, default 20, bounded 0–100: thins rain in the air, ground particles and rain sounds; it never changes server weather."
      },
      "de": {
        "title": "Regendichte",
        "summary": "clientRainParticleDensity ist eine lokale Darstellungseinstellung, Standard 20, Grenze 0–100: duennt Regen in der Luft, Bodenpartikel und Regengeraeusche aus; Serverwetter bleibt davon unabhaengig."
      }
    },
    {
      "id": "submod",
      "sources": [
        "modules/simpleweather/shared/java/com/simplebuilding/modules/simpleweather/SimpleWeather.java",
        "framework/src/main/java/com/simplebuilding/framework/api/SubMods.java"
      ],
      "en": {
        "title": "Sub-mod of Simple QoL",
        "summary": "Playable alone (config/simpleweather.json). Bundled in Simple QoL, which can switch it off with \"Enable Simple Weather\". On the first start the old Simple QoL weather settings are taken over once."
      },
      "de": {
        "title": "Sub-Mod von Simple QoL",
        "summary": "Allein spielbar (config/simpleweather.json). In Simple QoL gebuendelt, dort mit \"Simple Weather aktivieren\" abschaltbar. Beim ersten Start werden die alten Simple-QoL-Wettereinstellungen einmalig uebernommen."
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
    "features": 3,
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
