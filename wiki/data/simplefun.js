window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplefun"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplefun",
    "name": "Simple Fun",
    "version": "1.4.0",
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
      "id": "yeet",
      "en": {
        "title": "Sneak Throw",
        "summary": "Sneak while dropping items. Server multiplier 0.1–3, velocity capped at 1.5 blocks/tick, pickup delay 20 ticks.",
        "details": [
          "Sneak while dropping items. Server multiplier 0.1–3, velocity capped at 1.5 blocks/tick, pickup delay 20 ticks."
        ]
      },
      "de": {
        "title": "Schleichwurf",
        "summary": "Beim Wegwerfen schleichen. Serverfaktor 0,1–3, Tempo höchstens 1,5 Blöcke/Tick, Aufhebesperre 20 Ticks.",
        "details": [
          "Beim Wegwerfen schleichen. Serverfaktor 0,1–3, Tempo höchstens 1,5 Blöcke/Tick, Aufhebesperre 20 Ticks."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/mixin/YeetMixin.java"
      ]
    },
    {
      "id": "bricks",
      "en": {
        "title": "Throwable Bricks",
        "summary": "Brick, nether brick and resin brick: throw with use. Server cooldown 10 ticks, lifetime 200 ticks, damage 0–4. Glass breaking defaults off.",
        "details": [
          "Brick, nether brick and resin brick: throw with use. Server cooldown 10 ticks, lifetime 200 ticks, damage 0–4. Glass breaking defaults off.",
          "Glass destruction is available only in private single-player; published LAN and dedicated servers refuse it even if enabled."
        ]
      },
      "de": {
        "title": "Wurfziegel",
        "summary": "Ziegel, Netherziegel und Harzziegel werfen. Serversperre 10 Ticks, Lebensdauer 200 Ticks, Schaden 0–4. Glasbrechen standardmäßig aus.",
        "details": [
          "Ziegel, Netherziegel und Harzziegel werfen. Serversperre 10 Ticks, Lebensdauer 200 Ticks, Schaden 0–4. Glasbrechen standardmäßig aus.",
          "Glaszerstörung nur im privaten Einzelspieler; veröffentlichte LAN-Welten und dedizierte Server verweigern sie auch bei aktiviertem Schalter."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/entity/BrickProjectileEntity.java"
      ]
    },
    {
      "id": "piggy_effect",
      "en": {
        "title": "Piggy Transformation",
        "summary": "Eating raw or cooked pork grants a cosmetic pig head for five minutes; milk removes the effect.",
        "details": [
          "Eating raw or cooked pork grants a cosmetic pig head for five minutes; milk removes the effect.",
          "The five-minute duration is fixed and capped by the server."
        ]
      },
      "de": {
        "title": "Schweinverwandlung",
        "summary": "Rohes oder gebratenes Schweinefleisch gibt fünf Minuten einen kosmetischen Schweinekopf; Milch entfernt den Effekt.",
        "details": [
          "Rohes oder gebratenes Schweinefleisch gibt fünf Minuten einen kosmetischen Schweinekopf; Milch entfernt den Effekt.",
          "Der Server begrenzt die feste Dauer auf fünf Minuten."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/mixin/PiggyFoodMixin.java",
        "modules/simplefun/shared/java/com/simplefun/client/PigHeadFeatureRenderer.java"
      ]
    },
    {
      "id": "no_damage",
      "en": {
        "title": "Damageless",
        "summary": "Feather melee and Damageless I deal zero direct melee damage, with knockback. Novice librarians offer the book for 25 emeralds, three uses. Switches control effect and trades.",
        "details": [
          "Feather melee and Damageless I deal zero direct melee damage, with knockback. Novice librarians offer the book for 25 emeralds, three uses. Switches control effect and trades."
        ]
      },
      "de": {
        "title": "Schadlos",
        "summary": "Feder und Schadlos I verursachen keinen direkten Nahkampfschaden, mit Rückstoß. Anfängerbibliothekare bieten das Buch für 25 Smaragde, dreimal. Effekt und Handel abschaltbar.",
        "details": [
          "Feder und Schadlos I verursachen keinen direkten Nahkampfschaden, mit Rückstoß. Anfängerbibliothekare bieten das Buch für 25 Smaragde, dreimal. Effekt und Handel abschaltbar."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/mixin/NoDamageMixin.java",
        "modules/simplefun/generated/resources/data/simplefun/villager_trade/librarian/1/no_damage.json"
      ]
    },
    {
      "id": "knockback",
      "en": {
        "title": "Knockback V",
        "summary": "Vanilla Knockback accepts feathers/sticks and levels up to five; this replaces vanilla enchantment data and can collide with datapacks.",
        "details": [
          "Vanilla Knockback accepts feathers/sticks and levels up to five; this replaces vanilla enchantment data and can collide with datapacks."
        ]
      },
      "de": {
        "title": "Rückstoß V",
        "summary": "Vanilla-Rückstoß erlaubt Federn/Stöcke und Stufe fünf; überschreibt Vanilla-Daten und kann mit Datenpaketen kollidieren.",
        "details": [
          "Vanilla-Rückstoß erlaubt Federn/Stöcke und Stufe fünf; überschreibt Vanilla-Daten und kann mit Datenpaketen kollidieren."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/mixin/KnockbackCapMixin.java",
        "modules/simplefun/shared/resources/data/minecraft/enchantment/knockback.json"
      ]
    },
    {
      "id": "commands",
      "en": {
        "title": "Admin Commands",
        "summary": "Permission level 4: /simplefun tweaks yeet toggle/strength; /simplefun tweaks bricks enable/breakGlass/damage/snowballDamage. Values are bounded server-side.",
        "details": [
          "Permission level 4: /simplefun tweaks yeet toggle/strength; /simplefun tweaks bricks enable/breakGlass/damage/snowballDamage. Values are bounded server-side."
        ]
      },
      "de": {
        "title": "Adminbefehle",
        "summary": "Berechtigung 4: /simplefun tweaks yeet toggle/strength; /simplefun tweaks bricks enable/breakGlass/damage/snowballDamage. Server begrenzt Werte.",
        "details": [
          "Berechtigung 4: /simplefun tweaks yeet toggle/strength; /simplefun tweaks bricks enable/breakGlass/damage/snowballDamage. Server begrenzt Werte."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "brick_snowball",
      "en": {
        "title": "Brick Snowball",
        "summary": "Four snowballs around one brick craft one. Stack 16. Separate switch; damage 0–4, throw cooldown 10 ticks.",
        "details": [
          "Four snowballs around one brick craft one. Stack 16. Separate switch; damage 0–4, throw cooldown 10 ticks."
        ]
      },
      "de": {
        "title": "Ziegelschneeball",
        "summary": "Vier Schneebälle um einen Ziegel ergeben einen. Stapel 16. Eigener Schalter; Schaden 0–4, Wurfsperre 10 Ticks.",
        "details": [
          "Vier Schneebälle um einen Ziegel ergeben einen. Stapel 16. Eigener Schalter; Schaden 0–4, Wurfsperre 10 Ticks."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/entity/BrickProjectileEntity.java"
      ]
    },
    {
      "id": "flowerSniff",
      "en": {
        "title": "Flower Sniff",
        "summary": "Cosmetic delight: hold a flower and sneak. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards.",
        "details": [
          "Cosmetic delight: hold a flower and sneak. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards."
        ]
      },
      "de": {
        "title": "Blumenschnuppern",
        "summary": "Kosmetische Freude: Blume halten und schleichen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen.",
        "details": [
          "Kosmetische Freude: Blume halten und schleichen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/FunDelights.java"
      ]
    },
    {
      "id": "cookieCrumbs",
      "en": {
        "title": "Cookie Crumbs",
        "summary": "Cosmetic delight: eat a cookie. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards.",
        "details": [
          "Cosmetic delight: eat a cookie. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards."
        ]
      },
      "de": {
        "title": "Kekskrümel",
        "summary": "Kosmetische Freude: Keks essen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen.",
        "details": [
          "Kosmetische Freude: Keks essen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/FunDelights.java"
      ]
    },
    {
      "id": "appleSparkle",
      "en": {
        "title": "Apple Sparkle",
        "summary": "Cosmetic delight: eat an apple. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards.",
        "details": [
          "Cosmetic delight: eat an apple. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards."
        ]
      },
      "de": {
        "title": "Apfelglitzer",
        "summary": "Kosmetische Freude: Apfel essen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen.",
        "details": [
          "Kosmetische Freude: Apfel essen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/FunDelights.java"
      ]
    },
    {
      "id": "carrotCrunch",
      "en": {
        "title": "Carrot Crunch",
        "summary": "Cosmetic delight: eat a carrot. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards.",
        "details": [
          "Cosmetic delight: eat a carrot. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards."
        ]
      },
      "de": {
        "title": "Karottenknuspern",
        "summary": "Kosmetische Freude: Karotte essen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen.",
        "details": [
          "Kosmetische Freude: Karotte essen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/FunDelights.java"
      ]
    },
    {
      "id": "melonSplash",
      "en": {
        "title": "Melon Splash",
        "summary": "Cosmetic delight: eat melon. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards.",
        "details": [
          "Cosmetic delight: eat melon. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards."
        ]
      },
      "de": {
        "title": "Melonenspritzer",
        "summary": "Kosmetische Freude: Melone essen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen.",
        "details": [
          "Kosmetische Freude: Melone essen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/FunDelights.java"
      ]
    },
    {
      "id": "honeyBubbles",
      "en": {
        "title": "Honey Bubbles",
        "summary": "Cosmetic delight: drink honey. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards.",
        "details": [
          "Cosmetic delight: drink honey. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards."
        ]
      },
      "de": {
        "title": "Honigblasen",
        "summary": "Kosmetische Freude: Honig trinken. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen.",
        "details": [
          "Kosmetische Freude: Honig trinken. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/FunDelights.java"
      ]
    },
    {
      "id": "breadCrumbs",
      "en": {
        "title": "Bread Crumbs",
        "summary": "Cosmetic delight: eat bread. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards.",
        "details": [
          "Cosmetic delight: eat bread. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards."
        ]
      },
      "de": {
        "title": "Brotkrümel",
        "summary": "Kosmetische Freude: Brot essen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen.",
        "details": [
          "Kosmetische Freude: Brot essen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/FunDelights.java"
      ]
    },
    {
      "id": "berryBlush",
      "en": {
        "title": "Berry Blush",
        "summary": "Cosmetic delight: eat sweet or glow berries. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards.",
        "details": [
          "Cosmetic delight: eat sweet or glow berries. Server switch; at most six particles and a quiet sound every five seconds. No gameplay rewards."
        ]
      },
      "de": {
        "title": "Beerenfreude",
        "summary": "Kosmetische Freude: Süß- oder Leuchtbeeren essen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen.",
        "details": [
          "Kosmetische Freude: Süß- oder Leuchtbeeren essen. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden. Keine Spielbelohnungen."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/FunDelights.java"
      ]
    },
    {
      "id": "config_enablePiggyEffect",
      "en": {
        "title": "Piggy Transformation",
        "summary": "Eating (cooked) porkchop turns the player into a pig for 5 minutes. Default: true. Server-owned; restart required.",
        "details": [
          "Eating (cooked) porkchop turns the player into a pig for 5 minutes. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Schweine-Verwandlung",
        "summary": "Das Essen von (gebratenem) Schweinefleisch verwandelt den Spieler 5 Minuten lang in ein Schwein. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Das Essen von (gebratenem) Schweinefleisch verwandelt den Spieler 5 Minuten lang in ein Schwein. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_enableNoDamage",
      "en": {
        "title": "Damageless / Feather",
        "summary": "Feathers and the Damageless enchantment make player melee attacks deal 0 damage (knockback still applies). Default: true. Server-owned; restart required.",
        "details": [
          "Feathers and the Damageless enchantment make player melee attacks deal 0 damage (knockback still applies). Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Schadlos / Feder",
        "summary": "Federn und die Verzauberung „Schadlos“ lassen Nahkampfangriffe von Spielern 0 Schaden machen (Knockback bleibt erhalten). Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Federn und die Verzauberung „Schadlos“ lassen Nahkampfangriffe von Spielern 0 Schaden machen (Knockback bleibt erhalten). Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_enableYeet",
      "en": {
        "title": "Enable Yeet (Throw)",
        "summary": "Allows throwing items far while sneaking (Sneak + Q). Default: true. Server-owned; restart required.",
        "details": [
          "Allows throwing items far while sneaking (Sneak + Q). Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Yeet (Werfen) aktivieren",
        "summary": "Erlaubt das weite Werfen von Items beim Schleichen (Schleichen + Q). Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Erlaubt das weite Werfen von Items beim Schleichen (Schleichen + Q). Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_yeetStrength",
      "en": {
        "title": "Yeet Strength",
        "summary": "Velocity multiplier when throwing items while sneaking. Default: 3.0. Server-owned; restart required.",
        "details": [
          "Velocity multiplier when throwing items while sneaking. Default: 3.0. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Yeet-Stärke",
        "summary": "Geschwindigkeits-Multiplikator beim Werfen während des Schleichens. Standard: 3.0. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Geschwindigkeits-Multiplikator beim Werfen während des Schleichens. Standard: 3.0. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_enableThrowableBricks",
      "en": {
        "title": "Enable Throwable Bricks",
        "summary": "If enabled, Bricks, Nether Bricks and Resin Bricks can be thrown. Default: true. Server-owned; restart required.",
        "details": [
          "If enabled, Bricks, Nether Bricks and Resin Bricks can be thrown. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Werfbare Ziegel",
        "summary": "Wenn aktiviert, können Ziegel, Netherziegel und Harzziegel geworfen werden. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Wenn aktiviert, können Ziegel, Netherziegel und Harzziegel geworfen werden. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_throwableBricksBreakBlocks",
      "en": {
        "title": "Throwable Bricks Break Glass",
        "summary": "Breaks ordinary/stained glass only in private single-player, with server protection and world border checks. Published LAN and dedicated servers refuse terrain destruction. Default: false.",
        "details": [
          "Breaks ordinary/stained glass only in private single-player, with server protection and world border checks. Published LAN and dedicated servers refuse terrain destruction. Default: false."
        ]
      },
      "de": {
        "title": "Ziegel brechen Glas",
        "summary": "Bricht normales/buntes Glas nur im privaten Einzelspieler, mit Serverschutz und Weltgrenze. Veröffentlichte LAN-Welten und dedizierte Server verweigern Geländezerstörung. Standard: false.",
        "details": [
          "Bricht normales/buntes Glas nur im privaten Einzelspieler, mit Serverschutz und Weltgrenze. Veröffentlichte LAN-Welten und dedizierte Server verweigern Geländezerstörung. Standard: false."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_brickDamage",
      "en": {
        "title": "Brick Damage",
        "summary": "Damage dealt by thrown bricks. Default: 2.0. Server-owned; restart required.",
        "details": [
          "Damage dealt by thrown bricks. Default: 2.0. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Ziegel-Schaden",
        "summary": "Schaden durch geworfene Ziegel. Standard: 2.0. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Schaden durch geworfene Ziegel. Standard: 2.0. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_brickSnowballDamage",
      "en": {
        "title": "Brick Snowball Damage",
        "summary": "Damage dealt by thrown Brick Snowballs. Default: 2.0. Server-owned; restart required.",
        "details": [
          "Damage dealt by thrown Brick Snowballs. Default: 2.0. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Schneeball-Ziegel-Schaden",
        "summary": "Schaden durch geworfene Ziegel-Schneebälle. Standard: 2.0. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Schaden durch geworfene Ziegel-Schneebälle. Standard: 2.0. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_enableBrickSnowball",
      "en": {
        "title": "Enable brick snowball",
        "summary": "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required.",
        "details": [
          "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Ziegelschneeball aktivieren",
        "summary": "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_enableNoDamageTrades",
      "en": {
        "title": "Enable no damage trades",
        "summary": "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required.",
        "details": [
          "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Kein-Schaden-Handel aktivieren",
        "summary": "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_flowerSniff",
      "en": {
        "title": "Flower sniff",
        "summary": "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required.",
        "details": [
          "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Blumenschnuppern",
        "summary": "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_cookieCrumbs",
      "en": {
        "title": "Cookie crumbs",
        "summary": "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required.",
        "details": [
          "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Kekskrümel",
        "summary": "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_appleSparkle",
      "en": {
        "title": "Apple sparkle",
        "summary": "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required.",
        "details": [
          "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Apfelglitzer",
        "summary": "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_carrotCrunch",
      "en": {
        "title": "Carrot crunch",
        "summary": "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required.",
        "details": [
          "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Karottenknuspern",
        "summary": "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_melonSplash",
      "en": {
        "title": "Melon splash",
        "summary": "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required.",
        "details": [
          "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Melonenspritzer",
        "summary": "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_honeyBubbles",
      "en": {
        "title": "Honey bubbles",
        "summary": "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required.",
        "details": [
          "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Honigblasen",
        "summary": "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_breadCrumbs",
      "en": {
        "title": "Bread crumbs",
        "summary": "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required.",
        "details": [
          "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Brotkrümel",
        "summary": "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_berryBlush",
      "en": {
        "title": "Berry blush",
        "summary": "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required.",
        "details": [
          "Cosmetic effect; no gameplay rewards. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Beerenfreude",
        "summary": "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Kosmetischer Effekt ohne Spielbelohnungen. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      },
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ]
    },
    {
      "id": "config_enableHigherKnockback",
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ],
      "en": {
        "title": "Enable higher knockback",
        "summary": "Server option. Default: true. Server-owned; restart required.",
        "details": [
          "Server option. Default: true. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Stärkerer Rückstoß",
        "summary": "Serveroption. Standard: true. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Serveroption. Standard: true. Server bestimmt Werte; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config_maxKnockback",
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java",
        "modules/simplefun/shared/java/com/simplefun/command/SimplefunCommands.java"
      ],
      "en": {
        "title": "Max knockback",
        "summary": "Server option. Default: 4.0. Server-owned; restart required.",
        "details": [
          "Server option. Default: 4.0. Server-owned; restart required."
        ]
      },
      "de": {
        "title": "Maximaler Rückstoß",
        "summary": "Serveroption. Standard: 4.0. Server bestimmt Werte; Neustart erforderlich.",
        "details": [
          "Serveroption. Standard: 4.0. Server bestimmt Werte; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "configuration",
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/client/FunConfigScreen.java",
        "modules/simplefun/shared/java/com/simplefun/SimplefunCommon.java"
      ],
      "en": {
        "title": "Server Configuration",
        "summary": "Two tabs cover gameplay and small delights; every option explains its default. Gameplay uses only server settings.",
        "details": [
          "The client screen edits the local simplefun.json for the next local server start. Remote clients cannot submit settings. Remote admins use the original level-4 commands or change the server file and restart.",
          "Librarian trades are selected when server data loads; changing their file switches requires restart."
        ]
      },
      "de": {
        "title": "Serverkonfiguration",
        "summary": "Zwei Reiter für Spielmechanik und kleine Freuden; jede Option erklärt ihren Standardwert. Nur Serverwerte bestimmen das Spiel.",
        "details": [
          "Der Clientbildschirm bearbeitet die lokale simplefun.json für den nächsten lokalen Serverstart. Fremde Clients können keine Einstellungen senden. Entfernte Administratoren nutzen die alten Befehle mit Stufe 4 oder ändern die Serverdatei und starten neu.",
          "Bibliothekarhandel wird beim Laden der Serverdaten ausgewählt; Dateischalter brauchen einen Neustart."
        ]
      }
    },
    {
      "id": "forge_263",
      "en": {
        "title": "Experimental Forge 26.3",
        "summary": "Opt-in Forge adapter with shared bounded mechanics, registries, conditional trades, loot, JSON configuration, and server tests.",
        "details": [
          "Enable -Pforge263=true. Forge client display and optional integrations require separate acceptance.",
          "Cloth Config screens are unavailable on Forge 26.3; edit the module JSON configuration instead."
        ]
      },
      "de": {
        "title": "Experimentelles Forge 26.3",
        "summary": "Opt-in-Forge-Adapter mit gemeinsamem Modulcode, JSON-Konfiguration und gleichem Servertestkatalog.",
        "details": [
          "Mit -Pforge263=true aktivieren. Forge-Clientdarstellung und optionale Integrationen brauchen eine eigene Abnahme.",
          "Cloth-Config-Seiten fehlen auf Forge 26.3; stattdessen die Modul-JSON-Konfiguration bearbeiten."
        ]
      },
      "sources": [
        "modules/simplefun/forge/build.gradle"
      ],
      "related": []
    },
    {
      "id": "creative_tab_switch",
      "sources": [
        "modules/simplefun/shared/java/com/simplefun/registry/FunContent.java"
      ],
      "en": {
        "title": "Creative tabs",
        "summary": "The module's items sit in its own creative tab and are also sorted into the fitting Vanilla tabs and the search tab.",
        "details": [
          "The shared switch simplefun.addItemsToVanillaTabs in config/simple-creative-tabs.properties (default true, read when a world is joined) keeps the Vanilla tabs unchanged; the module's own tab stays. Guide books always stay in Tools & Utilities."
        ]
      },
      "de": {
        "title": "Kreativ-Tabs",
        "summary": "Die Items des Moduls stehen im eigenen Kreativ-Tab und zusätzlich in den passenden Vanilla-Tabs und im Suchtab.",
        "details": [
          "Der gemeinsame Schalter simplefun.addItemsToVanillaTabs in config/simple-creative-tabs.properties (Standard true, wird beim Betreten einer Welt gelesen) lässt die Vanilla-Tabs unverändert; der eigene Tab des Moduls bleibt. Handbücher bleiben immer in Werkzeuge & Hilfsmittel."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simplefun:brick_snowball",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplefun:brick_snowball",
        "count": 1
      },
      "source": "modules/simplefun/shared/resources/data/simplefun/recipe/brick_snowball.json",
      "ingredients": [
        "minecraft:brick",
        "minecraft:snowball"
      ],
      "pattern": [
        " S ",
        "SBS",
        " S "
      ],
      "key": {
        "B": [
          "minecraft:brick"
        ],
        "S": [
          "minecraft:snowball"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:snowball",
            "count": 4
          },
          {
            "id": "minecraft:clay_ball",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simplefun:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplefun:guide_book",
        "count": 1
      },
      "source": "modules/simplefun/shared/resources/data/simplefun/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:brick"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:brick"
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
            "id": "minecraft:clay_ball",
            "count": 1
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
      "id": "simplefun:item/knockback_allowed",
      "replace": false,
      "values": [
        {
          "id": "minecraft:feather",
          "required": true
        },
        {
          "id": "minecraft:stick",
          "required": true
        },
        {
          "id": "#minecraft:swords",
          "required": true
        },
        {
          "id": "#minecraft:axes",
          "required": true
        },
        {
          "id": "#minecraft:enchantable/mace",
          "required": true
        },
        {
          "id": "#minecraft:enchantable/trident",
          "required": true
        },
        {
          "id": "#minecraft:enchantable/bow",
          "required": true
        },
        {
          "id": "#minecraft:enchantable/crossbow",
          "required": true
        },
        {
          "id": "#minecraft:enchantable/lunge",
          "required": true
        }
      ],
      "source": "modules/simplefun/shared/resources/data/simplefun/tags/item/knockback_allowed.json"
    }
  ],
  "advancements": [
    {
      "id": "simplefun:content/brick_snowball",
      "parent": null,
      "icon": "simplefun:brick_snowball",
      "frame": "task",
      "hidden": false,
      "title": {
        "en_us": "Brick Snowball"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_item",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplefun:brick_snowball"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplefun/generated/resources/data/simplefun/advancement/content/brick_snowball.json"
    }
  ],
  "enchantments": [
    {
      "id": "simplefun:no_damage",
      "name": {
        "en_us": "Damageless",
        "de_de": "Schadlos"
      },
      "description": {
        "en_us": ""
      },
      "maxLevel": 1,
      "weight": 1,
      "anvilCost": 1,
      "slots": [
        "mainhand"
      ],
      "supportedItems": "#simplefun:knockback_allowed",
      "primaryItems": null,
      "exclusiveSet": null,
      "effects": [],
      "implementedIn": "none",
      "hasEffect": false,
      "source": "modules/simplefun/shared/resources/data/simplefun/enchantment/no_damage.json",
      "note": {
        "en": {
          "title": "Damageless",
          "summary": "Feather melee and Damageless I deal zero direct melee damage, with knockback. Novice librarians offer the book for 25 emeralds, three uses. Switches control effect and trades.",
          "details": [
            "Feather melee and Damageless I deal zero direct melee damage, with knockback. Novice librarians offer the book for 25 emeralds, three uses. Switches control effect and trades."
          ]
        },
        "de": {
          "title": "Schadlos",
          "summary": "Feder und Schadlos I verursachen keinen direkten Nahkampfschaden, mit Rückstoß. Anfängerbibliothekare bieten das Buch für 25 Smaragde, dreimal. Effekt und Handel abschaltbar.",
          "details": [
            "Feder und Schadlos I verursachen keinen direkten Nahkampfschaden, mit Rückstoß. Anfängerbibliothekare bieten das Buch für 25 Smaragde, dreimal. Effekt und Handel abschaltbar."
          ]
        }
      }
    }
  ],
  "items": [
    {
      "id": "simplefun:brick_snowball",
      "name": {
        "en_us": "Brick Snowball",
        "de_de": "Ziegel-Schneeball"
      },
      "note": {
        "en": {
          "summary": "Four snowballs around one brick craft one. Stack 16. Separate switch; damage 0–4, throw cooldown 10 ticks."
        },
        "de": {
          "summary": "Vier Schneebälle um einen Ziegel ergeben einen. Stapel 16. Eigener Schalter; Schaden 0–4, Wurfsperre 10 Ticks."
        }
      },
      "texture": "assets/textures/simplefun/item/brick_snowball.png",
      "questionary": {
        "en": {
          "lore": [
            "A snowball with a brick for a heart. Everyone knows it is a bad idea, which is the joke."
          ],
          "use": [
            "Craft four snowballs around one brick. Stacks to 16. Thrown, it deals 0 to 4 damage (default 2) with a cooldown of 10 ticks; both have their own server switch."
          ],
          "secret": [
            "It is mostly a cosmetic gag: it knocks back and hurts a little, and it breaks no blocks."
          ]
        },
        "de": {
          "lore": [
            "Ein Schneeball mit einem Ziegel als Herz. Alle wissen, dass es eine schlechte Idee ist, das ist der Witz."
          ],
          "use": [
            "Vier Schneebälle um einen Ziegel herstellen. Stapelt bis 16. Geworfen richtet er 0 bis 4 Schaden an (Standard 2), Abklingzeit 10 Ticks; beides hat einen eigenen Serverschalter."
          ],
          "secret": [
            "Er ist vor allem ein kosmetischer Scherz: Er stößt zurück, tut ein wenig weh und zerstört keine Blöcke."
          ]
        }
      },
      "craftedBy": [
        "simplefun:brick_snowball"
      ],
      "usedIn": []
    },
    {
      "id": "simplefun:guide_book",
      "name": {
        "en_us": "Simple Fun Guide",
        "de_de": "Simple-Fun-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simplefun/shared/java/com/simplefun/guide/FunGuide.java",
          "modules/simplefun/shared/resources/data/simplefun/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 18 pages taken from this wiki, shown in your language. Shapeless recipe: book + brick. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Fun\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 18 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Ziegel. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Fun\" eins."
        }
      },
      "texture": "assets/textures/simplefun/item/guide_book.png",
      "questionary": {
        "en": {
          "lore": [
            "A short book that knows throwable bricks, heads and other silly joys."
          ],
          "use": [
            "The Simple Fun Guide explains throwable bricks, heads and other silly joys. Craft it shapeless from a book and a crafting table."
          ],
          "secret": [
            "Its chapters unlock as the matching recipes land in your recipe book. Nothing is consumed."
          ]
        },
        "de": {
          "lore": [
            "Ein kurzes Buch, das Wurfziegel, Köpfe und andere alberne Freuden kennt."
          ],
          "use": [
            "Das Simple Fun-Handbuch erklärt Wurfziegel, Köpfe und andere alberne Freuden. Formlos aus einem Buch und einer Werkbank herstellbar."
          ],
          "secret": [
            "Die Kapitel öffnen sich, sobald die passenden Rezepte im Rezeptbuch erscheinen. Nichts wird verbraucht."
          ]
        }
      },
      "craftedBy": [
        "simplefun:guide_book"
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
    "features": 38,
    "recipes": 2,
    "lootTables": 0,
    "tags": 1,
    "advancements": 1,
    "enchantments": 1,
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
