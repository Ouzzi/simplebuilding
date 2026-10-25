window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplewanderers"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplewanderers",
    "name": "Simple Wanderers",
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
      "id": "village_wanderer",
      "sources": [
        "modules/simplewanderers/shared/java/com/simplewanderers/WandererRegistry.java",
        "modules/simplewanderers/shared/java/com/simplewanderers/entity/VillageWanderer.java",
        "modules/simplewanderers/shared/java/com/simplewanderers/logic/WandererSpawner.java",
        "modules/simplewanderers/shared/java/com/simplewanderers/logic/WandererVillages.java"
      ],
      "en": {
        "title": "Village Wanderer",
        "summary": "One travel-cloaked trader per village: a Wandering Trader in an ochre cloak and a hat with a wide brim. He stays near the village bell, never turns invisible and never despawns. The first visit to a village brings him; killing him is allowed.",
        "details": [
          "The village counts when a bell that belongs to a village structure is within the village radius (default 48) of a player; a bell you built yourself does not count. Every village gets exactly one wanderer: a record per village, a count of loaded wanderers and a duplicate check on the entity.",
          "He is a Wandering Trader for everything else (sounds, fleeing from zombies and illagers, trading screen), without the night-time invisibility potion and without a despawn timer. Spawn egg in the spawn egg tab.",
          "Respawn: after he died, a new one comes after 7 days (setting respawnDays) and only while a player is at the village bell. The days are game time, so they pass while nobody is there; the new wanderer then appears on the next visit. No new wanderer during a raid at the village (setting raidBlocksSpawn)."
        ]
      },
      "de": {
        "title": "Dorf-Wanderer",
        "summary": "Ein Händler im Reisemantel je Dorf: ein Fahrender Händler in ockerfarbenem Mantel und einem Hut mit breiter Krempe. Er bleibt bei der Dorfglocke, wird nie unsichtbar und verschwindet nie. Der erste Besuch eines Dorfes bringt ihn; töten ist erlaubt.",
        "details": [
          "Als Dorf zählt eine Glocke, die zu einer Dorfstruktur gehört, im Dorfradius (Standard 48) um einen Spieler; eine selbst gebaute Glocke zählt nicht. Jedes Dorf bekommt genau einen Wanderer: ein Eintrag je Dorf, eine Zählung geladener Wanderer und eine Doppelprüfung an der Entität.",
          "Sonst ist er ein Fahrender Händler (Geräusche, Flucht vor Zombies und Illagern, Handelsbildschirm), aber ohne Unsichtbarkeitstrank bei Nacht und ohne Despawn-Zeit. Spawn-Ei im Spawn-Ei-Tab.",
          "Respawn: ist er gestorben, kommt nach 7 Tagen (Einstellung respawnDays) ein neuer, und nur solange ein Spieler an der Dorfglocke ist. Die Tage zählen in Spielzeit und laufen also auch ohne Besucher; der neue Wanderer erscheint dann beim nächsten Besuch. Kein neuer Wanderer während eines Überfalls am Dorf (Einstellung raidBlocksSpawn)."
        ]
      }
    },
    {
      "id": "wanderer_horse",
      "sources": [
        "modules/simplewanderers/shared/java/com/simplewanderers/entity/WandererHorse.java",
        "modules/simplewanderers/shared/java/com/simplewanderers/entity/VillageWanderer.java"
      ],
      "en": {
        "title": "Wanderer's horse",
        "summary": "A normal horse in an ochre leather armor hangs on the wanderer's leash. Nobody rides it while it hangs there; sneak and right-click with an empty main hand to take the leash over and keep the horse.",
        "details": [
          "Shears or a lead work as in Vanilla. A taken or killed horse is replaced after 7 days (respawnDays) while a player is near; killing the wanderer frees the horse (the leash drops a lead) and a new wanderer brings a new one.",
          "The horse follows the wanderer; if the leash gets too long it is teleported to him instead of snapping. Setting horse turns the horse off. Coat and stats are Vanilla's horse; the armor is not dropped on death."
        ]
      },
      "de": {
        "title": "Wanderer-Pferd",
        "summary": "Ein gewöhnliches Pferd in ockerfarbener Lederrüstung hängt an der Leine des Wanderers. Solange es dort hängt, reitet es niemand; Schleichen und Rechtsklick mit leerer Haupthand übernimmt die Leine, und das Pferd gehört dir.",
        "details": [
          "Schere oder Leine funktionieren wie in Vanilla. Ein genommenes oder getötetes Pferd wird nach 7 Tagen (respawnDays) ersetzt, solange ein Spieler in der Nähe ist; tötest du den Wanderer, wird das Pferd frei (die Leine lässt eine Leine fallen), und ein neuer Wanderer bringt ein neues mit.",
          "Das Pferd folgt dem Wanderer; wird die Leine zu lang, wird es zu ihm teleportiert, statt zu reißen. Die Einstellung horse schaltet das Pferd ab. Fell und Werte sind die des Vanilla-Pferds; die Rüstung fällt beim Tod nicht."
        ]
      }
    },
    {
      "id": "wanderer_stock",
      "sources": [
        "modules/simplewanderers/shared/java/com/simplewanderers/logic/WandererTrades.java",
        "modules/simplewanderers/shared/java/com/simplewanderers/entity/VillageWanderer.java",
        "modules/simplewanderers/shared/java/com/simplewanderers/config/WandererConfig.java"
      ],
      "en": {
        "title": "New stock and restock",
        "summary": "A fresh set of offers every 3 days and a restock every 1.5 days. The stock is drawn from a seed of village, period and world seed, so every player sees the same offers and a reload never rolls again.",
        "details": [
          "Time is game time (sleeping does not move it). A new period replaces all offers and counts as a restock; in between, all uses reset. While somebody trades, the change waits. After a long absence there is one new stock, never a burst of several.",
          "Up to ten offers per period: 2 buying, 3 common, 2 uncommon, 1 rare and 2 from the village biome; an item never appears twice. Prices are Vanilla's, with no reputation discount from the wanderer's side beyond what each trade file says.",
          "Settings rerollDays (3, 1 to 14) and restockDays (1.5, 0.5 up to rerollDays)."
        ]
      },
      "de": {
        "title": "Neues Angebot und Nachschub",
        "summary": "Alle 3 Tage ein frisches Angebot, alle 1,5 Tage Nachschub. Das Angebot wird aus einem Startwert aus Dorf, Periode und Weltsaat gezogen: alle Spieler sehen dasselbe, und Neuladen würfelt nie neu.",
        "details": [
          "Zeit ist Spielzeit (Schlafen verschiebt nichts). Eine neue Periode ersetzt alle Angebote und zählt zugleich als Nachschub; dazwischen werden alle Verwendungen zurückgesetzt. Solange jemand handelt, wartet der Wechsel. Nach langer Abwesenheit gibt es genau ein neues Angebot, nie mehrere auf einmal.",
          "Bis zu zehn Angebote je Periode: 2 Kauf-, 3 häufige, 2 seltenere, 1 rares und 2 aus dem Dorfbiom; ein Item erscheint nie doppelt. Die Preise sind die von Vanilla, ohne Rufnachlass des Wanderers über das hinaus, was die einzelne Handelsdatei sagt.",
          "Einstellungen rerollDays (3, 1 bis 14) und restockDays (1,5, 0,5 bis rerollDays)."
        ]
      }
    },
    {
      "id": "wanderer_trades",
      "sources": [
        "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/common.json",
        "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/uncommon.json",
        "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/buying.json",
        "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/rare.json",
        "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wandering_trader/common.json",
        "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wandering_trader/buying.json",
        "modules/simplewanderers/shared/resources/data/simplewanderers/trade_set/wanderer/common.json"
      ],
      "en": {
        "title": "Goods: wanderer and traveling trader",
        "summary": "The goods of the Wandering Trader are split. The Village Wanderer sells the everyday part (dyes, seeds, flowers, saplings, sand, logs, buckets); the traveling trader keeps the exotic part (coral, glow stone, moss, rare saplings, fish buckets). The wanderer sells nothing very rare.",
        "details": [
          "Vanilla common pool: 31 goods stay with the traveling trader, 47 go to the wanderer (nothing is lost, nothing appears at both). Buying pool: buckets, baked potato and hay bale go to the wanderer, bottle and fermented spider eye stay.",
          "Uncommon goods (logs, podzol, gunpowder) are offered by both; the building cores, the wand book and all other rarities stay only with the traveling trader. The wanderer never sells cores.",
          "With SimpleBuilding: the Octant and the Reinforced Bundle (buy and sell) and the Diamond Pebbles. With Simple Money: the 14 mob heads of the 0.1 group (blaze, enderman, husk, spider, cave spider, slime, silverfish, breeze, shulker, drowned, pig, cow, chicken, sheep) as the rare offer - only he sells them, not the wandering trader; Simple Money switch trades.enableWandererTrades - and the two money trades for chorus fruit and apples.",
          "While this module is loaded, the traveling trader draws his common and buying goods from this module's lists; goods other mods add only to the Vanilla lists are not offered by him."
        ]
      },
      "de": {
        "title": "Waren: Wanderer und Fahrender Händler",
        "summary": "Die Waren des Fahrenden Händlers sind aufgeteilt. Der Dorf-Wanderer verkauft den Alltag (Farben, Saatgut, Blumen, Setzlinge, Sand, Stämme, Eimer); der Fahrende Händler behält das Exotische (Korallen, Glowstone, Moos, seltene Setzlinge, Fischeimer). Der Wanderer verkauft nichts sehr Seltenes.",
        "details": [
          "Vanilla-Pool „häufig“: 31 Waren bleiben beim Fahrenden Händler, 47 gehen an den Wanderer (nichts geht verloren, nichts steht bei beiden). Pool „Kauf“: Eimer, Ofenkartoffel und Heuballen gehen an den Wanderer, Flasche und Gärendes Spinnenauge bleiben.",
          "Seltenere Waren (Stämme, Podsol, Schwarzpulver) bieten beide an; die Baukerne, das Stabbuch und alle anderen Seltenheiten bleiben nur beim Fahrenden Händler. Der Wanderer verkauft nie Kerne.",
          "Mit SimpleBuilding: der Oktant und das Verstärkte Bündel (Kauf und Verkauf) und die Diamantkiesel. Mit Simple Money: die 14 Mob-Köpfe der 0,1-Gruppe (Lohe, Enderman, Wüstenzombie, Spinne, Höhlenspinne, Schleim, Silberfischchen, Windböe, Shulker, Ertrunkener, Schwein, Kuh, Huhn, Schaf) als rares Angebot – nur er, nicht der fahrende Händler; Simple-Money-Schalter trades.enableWandererTrades – und die zwei Geld-Handel für Chorusfrucht und Äpfel.",
          "Solange dieses Modul geladen ist, zieht der Fahrende Händler seine häufigen und Kauf-Waren aus den Listen dieses Moduls; Waren, die andere Mods nur in die Vanilla-Listen eintragen, bietet er dann nicht an."
        ]
      }
    },
    {
      "id": "biome_assortment",
      "sources": [
        "modules/simplewanderers/shared/java/com/simplewanderers/logic/WandererTrades.java",
        "modules/simplewanderers/shared/java/com/simplewanderers/logic/WandererSpawner.java",
        "modules/simplewanderers/shared/resources/data/simplewanderers/trade_set/wanderer/biome/desert.json",
        "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/biome/desert.json"
      ],
      "en": {
        "title": "Biome assortment",
        "summary": "The wanderer's village type (the Vanilla villager type of the bell's biome) adds two offers from its own pool: desert (cactus, sand), savanna (acacia, dry grass), plains (flowers, seeds), taiga (spruce, podzol), snow (spruce, fern, packed ice), jungle (jungle wood, vines) and swamp (lily pad, orchids, mushrooms).",
        "details": [
          "Other biomes and modded villages count as plains. The offers come from the same Vanilla trade files as everywhere else, so prices and amounts are Vanilla's. Setting biomeAssortment turns the extra offers off."
        ]
      },
      "de": {
        "title": "Biom-Sortiment",
        "summary": "Das Dorfbiom (der Vanilla-Dorfbewohnertyp am Biom der Glocke) fügt zwei Angebote aus einem eigenen Pool hinzu: Wüste (Kaktus, Sand), Savanne (Akazie, Trockengras), Ebene (Blumen, Saatgut), Taiga (Fichte, Podsol), Schnee (Fichte, Farn, Packeis), Dschungel (Dschungelholz, Ranken) und Sumpf (Seerosenblatt, Orchideen, Pilze).",
        "details": [
          "Andere Biome und Dörfer aus Mods zählen als Ebene. Die Angebote stammen aus denselben Vanilla-Handelsdateien wie überall, Preise und Mengen sind also die von Vanilla. Die Einstellung biomeAssortment schaltet die Zusatzangebote ab."
        ]
      }
    },
    {
      "id": "config_server",
      "sources": [
        "modules/simplewanderers/shared/java/com/simplewanderers/config/WandererConfig.java"
      ],
      "en": {
        "title": "Server settings",
        "summary": "config/simplewanderers-server.json, read at start; every number is clamped to a safe range.",
        "details": [
          "enabled (true): off stops new wanderers, respawns and the rotation; existing wanderers stay plain traders.",
          "respawnDays (7, 0 to 60): days until a killed wanderer or a lost horse is replaced; 0 = never.",
          "rerollDays (3, 1 to 14) and restockDays (1.5, 0.5 up to rerollDays): see New stock and restock.",
          "villageRadius (48, 16 to 64): how close a player must be to a village bell to count as a visit. homeRadius (24, 8 to 48): how far the wanderer strolls from the bell.",
          "horse (true), raidBlocksSpawn (true), biomeAssortment (true): switches for the horse, the raid rule and the biome offers.",
          "With SimpleBuilding the switch Enable Simple Wanderers in its config turns the bundled module on or off."
        ]
      },
      "de": {
        "title": "Server-Einstellungen",
        "summary": "config/simplewanderers-server.json, beim Start gelesen; jede Zahl wird auf einen sicheren Bereich begrenzt.",
        "details": [
          "enabled (true): aus stoppt neue Wanderer, Respawns und die Rotation; vorhandene Wanderer bleiben gewöhnliche Händler.",
          "respawnDays (7, 0 bis 60): Tage, bis ein getöteter Wanderer oder ein verlorenes Pferd ersetzt wird; 0 = nie.",
          "rerollDays (3, 1 bis 14) und restockDays (1,5, 0,5 bis rerollDays): siehe Neues Angebot und Nachschub.",
          "villageRadius (48, 16 bis 64): wie nah ein Spieler an einer Dorfglocke sein muss, damit es als Besuch zählt. homeRadius (24, 8 bis 48): wie weit der Wanderer von der Glocke spazieren geht.",
          "horse (true), raidBlocksSpawn (true), biomeAssortment (true): Schalter für das Pferd, die Überfall-Regel und die Biom-Angebote.",
          "Mit SimpleBuilding schaltet der Schalter Simple Wanderers aktivieren in dessen Config das gebündelte Modul ein oder aus."
        ]
      }
    },
    {
      "id": "commands",
      "sources": [
        "modules/simplewanderers/shared/java/com/simplewanderers/SimpleWanderers.java"
      ],
      "en": {
        "title": "Commands",
        "summary": "/simplewanderers config shows and changes every option of config/simplewanderers-server.json (list [group], get, set, reset, help; Tab completes options and values). Values are bounded like in the file, saved at once and take effect at once. Permission level 2.",
        "details": [
          "Example: /simplewanderers config get <option> shows the value, the default and the type; /simplewanderers config reset <option> brings the default back; a wrong type is refused and nothing is saved."
        ]
      },
      "de": {
        "title": "Befehle",
        "summary": "/simplewanderers config zeigt und ändert jede Option von config/simplewanderers-server.json (list [Gruppe], get, set, reset, help; Tab ergänzt Optionen und Werte). Werte werden wie in der Datei begrenzt, sofort gespeichert und wirken sofort. Berechtigung 2.",
        "details": [
          "Beispiel: /simplewanderers config get <Option> zeigt Wert, Standard und Typ; /simplewanderers config reset <Option> stellt den Standard wieder her; ein falscher Typ wird abgelehnt und nichts gespeichert."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simplewanderers:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplewanderers:guide_book",
        "count": 1
      },
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:lead"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:lead"
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
            "id": "minecraft:string",
            "count": 2.5
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
      "id": "simplewanderers:villager_trade/wanderer/biome/desert",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wandering_trader/emerald_cactus",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_sand",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_red_sand",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_dry_tall_grass",
          "required": true
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/biome/desert.json"
    },
    {
      "id": "simplewanderers:villager_trade/wanderer/biome/jungle",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wandering_trader/emerald_jungle_log",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_jungle_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_vine",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_melon_seeds",
          "required": true
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/biome/jungle.json"
    },
    {
      "id": "simplewanderers:villager_trade/wanderer/biome/plains",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wandering_trader/emerald_dandelion",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_poppy",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_cornflower",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_oxeye_daisy",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_wheat_seeds",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_oak_sapling",
          "required": true
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/biome/plains.json"
    },
    {
      "id": "simplewanderers:villager_trade/wanderer/biome/savanna",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wandering_trader/emerald_acacia_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_acacia_log",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_dry_tall_grass",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_sugar_cane",
          "required": true
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/biome/savanna.json"
    },
    {
      "id": "simplewanderers:villager_trade/wanderer/biome/snow",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wandering_trader/emerald_spruce_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_spruce_log",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_fern",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_packed_ice",
          "required": true
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/biome/snow.json"
    },
    {
      "id": "simplewanderers:villager_trade/wanderer/biome/swamp",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wandering_trader/emerald_lily_pad",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_blue_orchid",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_red_mushroom",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_brown_mushroom",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_vine",
          "required": true
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/biome/swamp.json"
    },
    {
      "id": "simplewanderers:villager_trade/wanderer/biome/taiga",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wandering_trader/emerald_spruce_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_spruce_log",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_podzol",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_fern",
          "required": true
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/biome/taiga.json"
    },
    {
      "id": "simplewanderers:villager_trade/wanderer/buying",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wandering_trader/water_bucket_emerald",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/milk_bucket_emerald",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/baked_potato_emerald",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/hay_block_emerald",
          "required": true
        },
        {
          "id": "simplebuilding:wandering_trader/octant_emerald",
          "required": false
        },
        {
          "id": "simplebuilding:wandering_trader/reinforced_bundle_emerald",
          "required": false
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/buying.json"
    },
    {
      "id": "simplewanderers:villager_trade/wanderer/common",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wandering_trader/emerald_white_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_orange_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_magenta_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_light_blue_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_yellow_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_lime_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_pink_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_gray_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_light_gray_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_cyan_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_purple_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_blue_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_brown_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_green_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_red_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_black_dye",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_wheat_seeds",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_beetroot_seeds",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_pumpkin_seeds",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_melon_seeds",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_fern",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_sugar_cane",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_pumpkin",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_dandelion",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_poppy",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_allium",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_azure_bluet",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_red_tulip",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_orange_tulip",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_white_tulip",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_pink_tulip",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_oxeye_daisy",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_cornflower",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_lily_of_the_valley",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_wildflowers",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_brown_mushroom",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_red_mushroom",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_oak_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_birch_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_spruce_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_acacia_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_dark_oak_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_name_tag",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_sand",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_red_sand",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_cactus",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_dry_tall_grass",
          "required": true
        },
        {
          "id": "simplebuilding:wandering_trader/emerald_diamond_pebbles",
          "required": false
        },
        {
          "id": "simplemoney:wandering_trader/40",
          "required": false
        },
        {
          "id": "simplemoney:wandering_trader/41",
          "required": false
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/common.json"
    },
    {
      "id": "simplewanderers:villager_trade/wanderer/rare",
      "replace": false,
      "values": [
        {
          "id": "simplemoney:links/simplemobheads/blaze_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/enderman_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/husk_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/spider_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/cave_spider_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/slime_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/silverfish_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/breeze_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/shulker_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/drowned_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/pig_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/cow_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/chicken_head",
          "required": false
        },
        {
          "id": "simplemoney:links/simplemobheads/sheep_head",
          "required": false
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/rare.json"
    },
    {
      "id": "simplewanderers:villager_trade/wanderer/uncommon",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wandering_trader/emerald_oak_log",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_birch_log",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_spruce_log",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_acacia_log",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_dark_oak_log",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_jungle_log",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_podzol",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_gunpowder",
          "required": true
        },
        {
          "id": "simplebuilding:wandering_trader/emerald_octant",
          "required": false
        },
        {
          "id": "simplebuilding:wandering_trader/emerald_reinforced_bundle",
          "required": false
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wanderer/uncommon.json"
    },
    {
      "id": "simplewanderers:villager_trade/wandering_trader/buying",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wandering_trader/water_bottle_emerald",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/fermented_spider_eye_emerald",
          "required": true
        },
        {
          "id": "simplebuilding:wandering_trader/octant_emerald",
          "required": false
        },
        {
          "id": "simplebuilding:wandering_trader/reinforced_bundle_emerald",
          "required": false
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wandering_trader/buying.json"
    },
    {
      "id": "simplewanderers:villager_trade/wandering_trader/common",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wandering_trader/emerald_fish_bucket",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_pufferfish_bucket",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_sea_pickle",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_slime_ball",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_glowstone",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_nautilus_shell",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_kelp",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_blue_orchid",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_open_eyeblossom",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_jungle_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_cherry_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_pale_oak_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_mangrove_propagule",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_poplar_sapling",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_brain_coral_block",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_bubble_coral_block",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_fire_coral_block",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_horn_coral_block",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_tube_coral_block",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_vine",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_pale_hanging_moss",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_lily_pad",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_small_dripleaf",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_pointed_dripstone",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_sulfur_spike",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_rooted_dirt",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_moss_block",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_pale_moss_block",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_firefly_bush",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_golden_dandelion",
          "required": true
        },
        {
          "id": "minecraft:wandering_trader/emerald_shelf_mushroom",
          "required": true
        },
        {
          "id": "simplemoney:wandering_trader/40",
          "required": false
        },
        {
          "id": "simplemoney:wandering_trader/41",
          "required": false
        }
      ],
      "source": "modules/simplewanderers/shared/resources/data/simplewanderers/tags/villager_trade/wandering_trader/common.json"
    }
  ],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "simplewanderers:guide_book",
      "name": {
        "en_us": "Simple Wanderers Guide",
        "de_de": "Simple-Wanderers-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simplewanderers/shared/java/com/simplewanderers/guide/WanderersGuide.java",
          "modules/simplewanderers/shared/resources/data/simplewanderers/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 13 pages taken from this wiki, shown in your language. Shapeless recipe: book + lead. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Wanderers\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 13 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Leine. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Wanderers\" eins."
        }
      },
      "texture": "assets/textures/simplewanderers/item/guide_book.png",
      "questionary": {
        "en": {
          "lore": [
            "A traveler's notebook for the one trader every village keeps."
          ],
          "use": [
            "The Simple Wanderers Guide explains the Village Wanderer, his horse, his stock and the biome goods. Craft it shapeless from a book and a lead."
          ],
          "secret": [
            "Its pages open in your language and nothing is consumed."
          ]
        },
        "de": {
          "lore": [
            "Ein Reisenotizbuch für den einen Händler, den jedes Dorf behält."
          ],
          "use": [
            "Das Simple-Wanderers-Handbuch erklärt den Dorf-Wanderer, sein Pferd, sein Angebot und die Biomwaren. Herstellung formlos aus einem Buch und einer Leine."
          ],
          "secret": [
            "Seine Seiten öffnen sich in deiner Sprache, und nichts wird verbraucht."
          ]
        }
      },
      "craftedBy": [
        "simplewanderers:guide_book"
      ],
      "usedIn": []
    },
    {
      "id": "simplewanderers:village_wanderer_spawn_egg",
      "name": {
        "en_us": "Village Wanderer Spawn Egg",
        "de_de": "Dorf-Wanderer-Spawn-Ei"
      },
      "note": {
        "sources": [
          "modules/simplewanderers/shared/java/com/simplewanderers/WandererRegistry.java"
        ],
        "en": {
          "summary": "Spawns a Village Wanderer (not tied to a village: no record, no rotation seed from a village, never counted). Creative only; the wanderers of villages appear on their own."
        },
        "de": {
          "summary": "Erzeugt einen Dorf-Wanderer (an kein Dorf gebunden: kein Eintrag, nie mitgezählt). Nur im Kreativmodus; die Wanderer der Dörfer erscheinen von selbst."
        }
      },
      "texture": "assets/textures/simplewanderers/item/village_wanderer_spawn_egg.png",
      "questionary": {
        "en": {
          "lore": [
            "A trader in an ochre cloak, folded into an egg."
          ],
          "use": [
            "Creative only. It spawns a Village Wanderer that belongs to no village: no record, no village seed, never counted. The wanderers of real villages appear on their own."
          ],
          "secret": [
            "Use it to test offers or to place a trader by hand; the offers still follow the biome around him."
          ]
        },
        "de": {
          "lore": [
            "Ein Händler im ockerfarbenen Mantel, zu einem Ei gefaltet."
          ],
          "use": [
            "Nur im Kreativmodus. Es erschafft einen Dorf-Wanderer ohne Dorf: kein Eintrag, kein Dorf-Seed, wird nie mitgezählt. Die Wanderer echter Dörfer erscheinen von selbst."
          ],
          "secret": [
            "Nutze es, um Angebote zu testen oder einen Händler von Hand zu setzen; das Angebot folgt trotzdem dem Biom um ihn."
          ]
        }
      },
      "craftedBy": [],
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
    "features": 7,
    "recipes": 1,
    "lootTables": 0,
    "tags": 13,
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
