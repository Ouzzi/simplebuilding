window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplesounds"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplesounds",
    "name": "Simple Sounds",
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
      "id": "config_followVisuals",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds/SoundConfig.java",
        "framework/src/main/java/com/simplebuilding/framework/api/CosmeticIntensity.java",
        "modules/simplevisuals/shared/java/com/simplevisuals/Visuals.java"
      ],
      "en": {
        "title": "Follow Simple Visuals",
        "summary": "Use the Simple Visuals intensity when available. Effect overrides take priority; otherwise your own level applies. Client only. Default: true.",
        "details": [
          "Use the Simple Visuals intensity when available. Effect overrides take priority; otherwise your own level applies. Client only. Default: true.",
          "Off/Subtle/Normal/Strong/Maximum follows the Visuals level of the same effect one-to-one - Visuals' effect override when it has one, otherwise its global level - through the optional framework API, without file polling or a hard dependency. With Visuals absent or an older version without a provider, the own level applies. A sound effect override still wins. Audio caps and cooldowns are unchanged."
        ]
      },
      "de": {
        "title": "Simple Visuals folgen",
        "summary": "Nutzt die Intensität von Simple Visuals, falls verfügbar. Effekt-Overrides haben Vorrang; sonst gilt die eigene Stufe. Nur clientseitig. Standard: true.",
        "details": [
          "Nutzt die Intensität von Simple Visuals, falls verfügbar. Effekt-Overrides haben Vorrang; sonst gilt die eigene Stufe. Nur clientseitig. Standard: true.",
          "Aus/Dezent/Normal/Stark/Maximum folgt der Visuals-Stufe desselben Effekts eins zu eins - dem Visuals-Effekt-Override, falls gesetzt, sonst der globalen Visuals-Stufe - über die optionale Framework-API, ohne Dateiabfragen oder Pflichtabhängigkeit. Ohne Visuals oder bei einer älteren Version ohne Anbieter gilt die eigene Stufe. Ein Sound-Effekt-Override hat weiter Vorrang. Audio-Obergrenzen und Pausen bleiben unverändert."
        ]
      }
    },
    {
      "id": "config_globalLevel",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Global intensity",
        "summary": "Own intensity when following is disabled or Simple Visuals is unavailable. Off/Subtle/Normal/Strong/Maximum: 0/0.35/0.6/0.8/1 gain. Default: SUBTLE.",
        "details": [
          "Own intensity when following is disabled or Simple Visuals is unavailable. Off/Subtle/Normal/Strong/Maximum: 0/0.35/0.6/0.8/1 gain. Default: SUBTLE."
        ]
      },
      "de": {
        "title": "Globale Intensität",
        "summary": "Eigene Intensität bei ausgeschalteter Kopplung oder ohne Simple Visuals. Aus/Dezent/Normal/Stark/Maximum: Faktor 0/0,35/0,6/0,8/1. Standard: SUBTLE.",
        "details": [
          "Eigene Intensität bei ausgeschalteter Kopplung oder ohne Simple Visuals. Aus/Dezent/Normal/Stark/Maximum: Faktor 0/0,35/0,6/0,8/1. Standard: SUBTLE."
        ]
      }
    },
    {
      "id": "config_volumeCap",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Volume ceiling",
        "summary": "Hard limit 0–0.25; Vanilla ambient slider also applies. Default: 0.25.",
        "details": [
          "Hard limit 0–0.25; Vanilla ambient slider also applies. Default: 0.25."
        ]
      },
      "de": {
        "title": "Lautstärkegrenze",
        "summary": "Harte Grenze 0–0,25; Vanilla-Umgebungsregler gilt zusätzlich. Standard: 0.25.",
        "details": [
          "Harte Grenze 0–0,25; Vanilla-Umgebungsregler gilt zusätzlich. Standard: 0.25."
        ]
      }
    },
    {
      "id": "config_soundsPerTick",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Sounds per tick",
        "summary": "0–4; excess sounds are dropped. Default: 2.",
        "details": [
          "0–4; excess sounds are dropped. Default: 2."
        ]
      },
      "de": {
        "title": "Sounds je Tick",
        "summary": "0–4; Überzählige Sounds entfallen. Standard: 2.",
        "details": [
          "0–4; Überzählige Sounds entfallen. Standard: 2."
        ]
      }
    },
    {
      "id": "config_soundsPerPlayer",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Sounds per player per tick",
        "summary": "0–2; only your player is observed. Default: 1.",
        "details": [
          "0–2; only your player is observed. Default: 1."
        ]
      },
      "de": {
        "title": "Sounds je Spieler und Tick",
        "summary": "0–2; nur dein Spieler wird beobachtet. Standard: 1.",
        "details": [
          "0–2; nur dein Spieler wird beobachtet. Standard: 1."
        ]
      }
    },
    {
      "id": "config_cooldownTicks",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Effect cooldown (ticks)",
        "summary": "20–1200 ticks per effect/player; mapping intervals also apply. Default: 40.",
        "details": [
          "20–1200 ticks per effect/player; mapping intervals also apply. Default: 40."
        ]
      },
      "de": {
        "title": "Effektpause (Ticks)",
        "summary": "20–1200 Ticks je Effekt/Spieler; Zuordnungsintervalle gelten zusätzlich. Standard: 40.",
        "details": [
          "20–1200 Ticks je Effekt/Spieler; Zuordnungsintervalle gelten zusätzlich. Standard: 40."
        ]
      }
    },
    {
      "id": "effect_footstep_dust",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Footstep dust",
        "summary": "Local Vanilla sound minecraft:block.powder_snow.step; at least 20 ticks apart and never sooner than the effect cooldown, base volume 0.05, pitch 1.35–1.45. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.powder_snow.step; at least 20 ticks apart and never sooner than the effect cooldown, base volume 0.05, pitch 1.35–1.45. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Schrittstaub",
        "summary": "Lokaler Vanilla-Sound minecraft:block.powder_snow.step; mindestens 20 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.05, Tonhöhe 1,35–1,45. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.powder_snow.step; mindestens 20 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.05, Tonhöhe 1,35–1,45. Standard: INHERIT."
        ]
      }
    },
    {
      "id": "effect_cold_breath",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Cold breath",
        "summary": "Local Vanilla sound minecraft:entity.player.breath; at least 160 ticks apart and never sooner than the effect cooldown, base volume 0.06, pitch 0.85–0.95. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:entity.player.breath; at least 160 ticks apart and never sooner than the effect cooldown, base volume 0.06, pitch 0.85–0.95. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Kalter Atem",
        "summary": "Lokaler Vanilla-Sound minecraft:entity.player.breath; mindestens 160 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.06, Tonhöhe 0,85–0,95. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:entity.player.breath; mindestens 160 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.06, Tonhöhe 0,85–0,95. Standard: INHERIT."
        ]
      }
    },
    {
      "id": "effect_fireflies",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Fireflies",
        "summary": "Local Vanilla sound minecraft:block.firefly_bush.idle; at least 100 ticks apart and never sooner than the effect cooldown, base volume 0.10, pitch 0.95–1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.firefly_bush.idle; at least 100 ticks apart and never sooner than the effect cooldown, base volume 0.10, pitch 0.95–1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Glühwürmchen",
        "summary": "Lokaler Vanilla-Sound minecraft:block.firefly_bush.idle; mindestens 100 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.10, Tonhöhe 0,95–1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.firefly_bush.idle; mindestens 100 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.10, Tonhöhe 0,95–1,05. Standard: INHERIT."
        ]
      }
    },
    {
      "id": "effect_pollen",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Pollen",
        "summary": "Local Vanilla sound minecraft:block.flowering_azalea.step; at least 120 ticks apart and never sooner than the effect cooldown, base volume 0.05, pitch 1.35–1.45. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.flowering_azalea.step; at least 120 ticks apart and never sooner than the effect cooldown, base volume 0.05, pitch 1.35–1.45. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Pollen",
        "summary": "Lokaler Vanilla-Sound minecraft:block.flowering_azalea.step; mindestens 120 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.05, Tonhöhe 1,35–1,45. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.flowering_azalea.step; mindestens 120 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.05, Tonhöhe 1,35–1,45. Standard: INHERIT."
        ]
      }
    },
    {
      "id": "effect_fire_sparks",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Fire sparks",
        "summary": "Local Vanilla sound minecraft:block.campfire.crackle; at least 60 ticks apart and never sooner than the effect cooldown, base volume 0.08, pitch 0.95–1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.campfire.crackle; at least 60 ticks apart and never sooner than the effect cooldown, base volume 0.08, pitch 0.95–1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Feuerfunken",
        "summary": "Lokaler Vanilla-Sound minecraft:block.campfire.crackle; mindestens 60 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.08, Tonhöhe 0,95–1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.campfire.crackle; mindestens 60 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.08, Tonhöhe 0,95–1,05. Standard: INHERIT."
        ]
      }
    },
    {
      "id": "effect_water_ripples",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Water ripples",
        "summary": "Local Vanilla sound minecraft:entity.generic.swim; at least 40 ticks apart and never sooner than the effect cooldown, base volume 0.05, pitch 1.25–1.35. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:entity.generic.swim; at least 40 ticks apart and never sooner than the effect cooldown, base volume 0.05, pitch 1.25–1.35. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Wasserwellen",
        "summary": "Lokaler Vanilla-Sound minecraft:entity.generic.swim; mindestens 40 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.05, Tonhöhe 1,25–1,35. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:entity.generic.swim; mindestens 40 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.05, Tonhöhe 1,25–1,35. Standard: INHERIT."
        ]
      }
    },
    {
      "id": "effect_water_droplets",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Water droplets",
        "summary": "Local Vanilla sound minecraft:block.pointed_dripstone.drip_water; at least 60 ticks apart and never sooner than the effect cooldown, base volume 0.08, pitch 1.05–1.15. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.pointed_dripstone.drip_water; at least 60 ticks apart and never sooner than the effect cooldown, base volume 0.08, pitch 1.05–1.15. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Wassertropfen",
        "summary": "Lokaler Vanilla-Sound minecraft:block.pointed_dripstone.drip_water; mindestens 60 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.08, Tonhöhe 1,05–1,15. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.pointed_dripstone.drip_water; mindestens 60 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.08, Tonhöhe 1,05–1,15. Standard: INHERIT."
        ]
      }
    },
    {
      "id": "effect_leaf_fall",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Leaf fall",
        "summary": "Local Vanilla sound minecraft:block.leaf_litter.step; at least 100 ticks apart and never sooner than the effect cooldown, base volume 0.06, pitch 1.15–1.25. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.leaf_litter.step; at least 100 ticks apart and never sooner than the effect cooldown, base volume 0.06, pitch 1.15–1.25. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Fallende Blätter",
        "summary": "Lokaler Vanilla-Sound minecraft:block.leaf_litter.step; mindestens 100 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.06, Tonhöhe 1,15–1,25. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.leaf_litter.step; mindestens 100 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.06, Tonhöhe 1,15–1,25. Standard: INHERIT."
        ]
      }
    },
    {
      "id": "effect_enchanted_items",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Enchanted items",
        "summary": "Local Vanilla sound minecraft:block.amethyst_block.chime; at least 200 ticks apart and never sooner than the effect cooldown, base volume 0.05, pitch 1.45–1.55. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.amethyst_block.chime; at least 200 ticks apart and never sooner than the effect cooldown, base volume 0.05, pitch 1.45–1.55. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Verzauberte Items",
        "summary": "Lokaler Vanilla-Sound minecraft:block.amethyst_block.chime; mindestens 200 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.05, Tonhöhe 1,45–1,55. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.amethyst_block.chime; mindestens 200 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.05, Tonhöhe 1,45–1,55. Standard: INHERIT."
        ]
      }
    },
    {
      "id": "effect_beacon_aura",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Beacon aura",
        "summary": "Local Vanilla sound minecraft:block.beacon.ambient; at least 200 ticks apart and never sooner than the effect cooldown, base volume 0.06, pitch 0.95–1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.beacon.ambient; at least 200 ticks apart and never sooner than the effect cooldown, base volume 0.06, pitch 0.95–1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Leuchtfeueraura",
        "summary": "Lokaler Vanilla-Sound minecraft:block.beacon.ambient; mindestens 200 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.06, Tonhöhe 0,95–1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.beacon.ambient; mindestens 200 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.06, Tonhöhe 0,95–1,05. Standard: INHERIT."
        ]
      }
    },
    {
      "id": "effect_damage_feedback",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Damage feedback",
        "summary": "Local Vanilla sound minecraft:entity.player.hurt; at least 40 ticks apart and never sooner than the effect cooldown, base volume 0.10, pitch 0.75–0.85. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:entity.player.hurt; at least 40 ticks apart and never sooner than the effect cooldown, base volume 0.10, pitch 0.75–0.85. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Schadensreaktion",
        "summary": "Lokaler Vanilla-Sound minecraft:entity.player.hurt; mindestens 40 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.10, Tonhöhe 0,75–0,85. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:entity.player.hurt; mindestens 40 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.10, Tonhöhe 0,75–0,85. Standard: INHERIT."
        ]
      }
    },
    {
      "id": "effect_healing_feedback",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Healing feedback",
        "summary": "Local Vanilla sound minecraft:block.amethyst_block.resonate; at least 40 ticks apart and never sooner than the effect cooldown, base volume 0.12, pitch 1.55–1.65. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.amethyst_block.resonate; at least 40 ticks apart and never sooner than the effect cooldown, base volume 0.12, pitch 1.55–1.65. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Heilungsreaktion",
        "summary": "Lokaler Vanilla-Sound minecraft:block.amethyst_block.resonate; mindestens 40 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.12, Tonhöhe 1,55–1,65. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.amethyst_block.resonate; mindestens 40 Ticks Abstand und nie vor Ablauf der Effektpause, Basislautstärke 0.12, Tonhöhe 1,55–1,65. Standard: INHERIT."
        ]
      }
    },
    {
      "id": "safety",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Local cosmetic safety",
        "summary": "No packets, server gameplay, entities, inventory changes or remote player scans. At most four sounds per tick, two per player and eight budgeted players; excess requests are dropped. World changes clear all history. An effective Off level or zero limits disable playback; effect overrides take priority over global levels. Vanilla sound assets and subtitles are reused; no bundled audio.",
        "details": [
          "No packets, server gameplay, entities, inventory changes or remote player scans. At most four sounds per tick, two per player and eight budgeted players; excess requests are dropped. World changes clear all history. An effective Off level or zero limits disable playback; effect overrides take priority over global levels. Vanilla sound assets and subtitles are reused; no bundled audio."
        ]
      },
      "de": {
        "title": "Lokale Sicherheit",
        "summary": "Keine Pakete, Gameplayänderungen, Entities, Inventaränderungen oder Suche nach fremden Spielern. Höchstens vier Sounds je Tick, zwei je Spieler und acht budgetierte Spieler; überzählige Anfragen entfallen. Weltwechsel löscht Verläufe. Die wirksame Stufe Aus oder Nullgrenzen deaktivieren Sounds; Effekt-Overrides haben Vorrang vor globalen Stufen. Vanilla-Sounds und Untertitel werden verwendet; keine eigenen Audiodateien.",
        "details": [
          "Keine Pakete, Gameplayänderungen, Entities, Inventaränderungen oder Suche nach fremden Spielern. Höchstens vier Sounds je Tick, zwei je Spieler und acht budgetierte Spieler; überzählige Anfragen entfallen. Weltwechsel löscht Verläufe. Die wirksame Stufe Aus oder Nullgrenzen deaktivieren Sounds; Effekt-Overrides haben Vorrang vor globalen Stufen. Vanilla-Sounds und Untertitel werden verwendet; keine eigenen Audiodateien."
        ]
      }
    },
    {
      "id": "forge_support",
      "sources": [
        "modules/simplesounds/forge/build.gradle"
      ],
      "en": {
        "title": "Experimental Forge 26.3",
        "summary": "Opt-in Forge adapter with the same bounded local sound engine.",
        "details": [
          "Enable -Pforge263=true. Cloth configuration screen is unavailable; module JSON configuration remains supported. Forge client audio has not been verified."
        ]
      },
      "de": {
        "title": "Experimentelles Forge 26.3",
        "summary": "Opt-in-Forge-Adapter mit derselben begrenzten lokalen Sound-Engine.",
        "details": [
          "Mit -Pforge263=true aktivieren. Cloth-Konfigurationsoberfläche fehlt; Modul-JSON-Konfiguration bleibt unterstützt. Forge-Clientaudio wurde nicht geprüft."
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
    "features": 20,
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
