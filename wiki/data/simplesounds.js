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
      "id": "config_globalLevel",
      "sources": [
        "modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds"
      ],
      "en": {
        "title": "Global intensity",
        "summary": "0/0.35/0.6/0.8/1 gain; no gameplay changes. Default: SUBTLE.",
        "details": [
          "0/0.35/0.6/0.8/1 gain; no gameplay changes. Default: SUBTLE."
        ]
      },
      "de": {
        "title": "Globale Intensit?t",
        "summary": "Faktor 0/0,35/0,6/0,8/1; keine Gameplay?nderung. Standard: SUBTLE.",
        "details": [
          "Faktor 0/0,35/0,6/0,8/1; keine Gameplay?nderung. Standard: SUBTLE."
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
        "summary": "Hard limit 0?0.25; Vanilla ambient slider also applies. Default: 0.25.",
        "details": [
          "Hard limit 0?0.25; Vanilla ambient slider also applies. Default: 0.25."
        ]
      },
      "de": {
        "title": "Lautst?rkegrenze",
        "summary": "Harte Grenze 0?0,25; Vanilla-Umgebungsregler gilt zus?tzlich. Standard: 0.25.",
        "details": [
          "Harte Grenze 0?0,25; Vanilla-Umgebungsregler gilt zus?tzlich. Standard: 0.25."
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
        "summary": "0?4; excess sounds are dropped. Default: 2.",
        "details": [
          "0?4; excess sounds are dropped. Default: 2."
        ]
      },
      "de": {
        "title": "Sounds je Tick",
        "summary": "0?4; ?berz?hlige Sounds entfallen. Standard: 2.",
        "details": [
          "0?4; ?berz?hlige Sounds entfallen. Standard: 2."
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
        "summary": "0?2; only your player is observed. Default: 1.",
        "details": [
          "0?2; only your player is observed. Default: 1."
        ]
      },
      "de": {
        "title": "Sounds je Spieler und Tick",
        "summary": "0?2; nur dein Spieler wird beobachtet. Standard: 1.",
        "details": [
          "0?2; nur dein Spieler wird beobachtet. Standard: 1."
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
        "summary": "20?1200 ticks per effect/player; mapping intervals also apply. Default: 40.",
        "details": [
          "20?1200 ticks per effect/player; mapping intervals also apply. Default: 40."
        ]
      },
      "de": {
        "title": "Effektpause (Ticks)",
        "summary": "20?1200 Ticks je Effekt/Spieler; Zuordnungsintervalle gelten zus?tzlich. Standard: 40.",
        "details": [
          "20?1200 Ticks je Effekt/Spieler; Zuordnungsintervalle gelten zus?tzlich. Standard: 40."
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
        "summary": "Local Vanilla sound minecraft:block.sand.step; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.sand.step; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Schrittstaub",
        "summary": "Lokaler Vanilla-Sound minecraft:block.sand.step; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.sand.step; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT."
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
        "summary": "Local Vanilla sound minecraft:entity.player.breath; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:entity.player.breath; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Kalter Atem",
        "summary": "Lokaler Vanilla-Sound minecraft:entity.player.breath; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:entity.player.breath; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT."
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
        "summary": "Local Vanilla sound minecraft:block.grass.step; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.grass.step; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Gl?hw?rmchen",
        "summary": "Lokaler Vanilla-Sound minecraft:block.grass.step; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.grass.step; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT."
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
        "summary": "Local Vanilla sound minecraft:block.azalea_leaves.step; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.azalea_leaves.step; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Pollen",
        "summary": "Lokaler Vanilla-Sound minecraft:block.azalea_leaves.step; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.azalea_leaves.step; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT."
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
        "summary": "Local Vanilla sound minecraft:block.fire.ambient; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.fire.ambient; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Feuerfunken",
        "summary": "Lokaler Vanilla-Sound minecraft:block.fire.ambient; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.fire.ambient; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT."
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
        "summary": "Local Vanilla sound minecraft:entity.generic.swim; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:entity.generic.swim; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Wasserwellen",
        "summary": "Lokaler Vanilla-Sound minecraft:entity.generic.swim; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:entity.generic.swim; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT."
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
        "summary": "Local Vanilla sound minecraft:block.pointed_dripstone.drip_water; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.pointed_dripstone.drip_water; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Wassertropfen",
        "summary": "Lokaler Vanilla-Sound minecraft:block.pointed_dripstone.drip_water; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.pointed_dripstone.drip_water; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT."
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
        "summary": "Local Vanilla sound minecraft:block.cherry_leaves.step; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.cherry_leaves.step; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Fallende Bl?tter",
        "summary": "Lokaler Vanilla-Sound minecraft:block.cherry_leaves.step; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.cherry_leaves.step; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT."
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
        "summary": "Local Vanilla sound minecraft:block.amethyst_block.chime; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.amethyst_block.chime; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Verzauberte Items",
        "summary": "Lokaler Vanilla-Sound minecraft:block.amethyst_block.chime; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.amethyst_block.chime; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT."
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
        "summary": "Local Vanilla sound minecraft:block.beacon.ambient; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.beacon.ambient; at least 40 ticks apart, base volume 0.12, pitch 0.95?1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Leuchtfeueraura",
        "summary": "Lokaler Vanilla-Sound minecraft:block.beacon.ambient; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.beacon.ambient; mindestens 40 Ticks Abstand, Basislautst?rke 0.12, Tonh?he 0,95?1,05. Standard: INHERIT."
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
        "summary": "Local Vanilla sound minecraft:entity.player.hurt; at least 40 ticks apart, base volume 0.22, pitch 0.95?1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:entity.player.hurt; at least 40 ticks apart, base volume 0.22, pitch 0.95?1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Schadensreaktion",
        "summary": "Lokaler Vanilla-Sound minecraft:entity.player.hurt; mindestens 40 Ticks Abstand, Basislautst?rke 0.22, Tonh?he 0,95?1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:entity.player.hurt; mindestens 40 Ticks Abstand, Basislautst?rke 0.22, Tonh?he 0,95?1,05. Standard: INHERIT."
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
        "summary": "Local Vanilla sound minecraft:block.amethyst_block.chime; at least 40 ticks apart, base volume 0.22, pitch 0.95?1.05. Default: INHERIT.",
        "details": [
          "Local Vanilla sound minecraft:block.amethyst_block.chime; at least 40 ticks apart, base volume 0.22, pitch 0.95?1.05. Default: INHERIT."
        ]
      },
      "de": {
        "title": "Heilungsreaktion",
        "summary": "Lokaler Vanilla-Sound minecraft:block.amethyst_block.chime; mindestens 40 Ticks Abstand, Basislautst?rke 0.22, Tonh?he 0,95?1,05. Standard: INHERIT.",
        "details": [
          "Lokaler Vanilla-Sound minecraft:block.amethyst_block.chime; mindestens 40 Ticks Abstand, Basislautst?rke 0.22, Tonh?he 0,95?1,05. Standard: INHERIT."
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
        "summary": "No packets, server gameplay, entities, inventory changes or remote player scans. At most four sounds per tick, two per player and eight budgeted players; excess requests are dropped. World changes clear all history. Off or zero limits disable playback. Vanilla sound assets and subtitles are reused; no bundled audio.",
        "details": [
          "No packets, server gameplay, entities, inventory changes or remote player scans. At most four sounds per tick, two per player and eight budgeted players; excess requests are dropped. World changes clear all history. Off or zero limits disable playback. Vanilla sound assets and subtitles are reused; no bundled audio."
        ]
      },
      "de": {
        "title": "Lokale Sicherheit",
        "summary": "Keine Pakete, Gameplay?nderungen, Entities, Inventar?nderungen oder Suche nach fremden Spielern. H?chstens vier Sounds je Tick, zwei je Spieler und acht budgetierte Spieler; ?berz?hlige Anfragen entfallen. Weltwechsel l?scht Verl?ufe. Aus oder Nullgrenzen deaktivieren Sounds. Vanilla-Sounds und Untertitel werden verwendet; keine eigenen Audiodateien.",
        "details": [
          "Keine Pakete, Gameplay?nderungen, Entities, Inventar?nderungen oder Suche nach fremden Spielern. H?chstens vier Sounds je Tick, zwei je Spieler und acht budgetierte Spieler; ?berz?hlige Anfragen entfallen. Weltwechsel l?scht Verl?ufe. Aus oder Nullgrenzen deaktivieren Sounds. Vanilla-Sounds und Untertitel werden verwendet; keine eigenen Audiodateien."
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
    "features": 19,
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
