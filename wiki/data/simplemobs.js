window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplemobs"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplemobs",
    "name": "Simple Mobs",
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
      "id": "hollow_howler",
      "sources": [
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/HowlerEntity.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/HowlerLogic.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/HowlerPattern.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/HowlerRitual.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/HowlerRitualEntity.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/MobsConfig.java"
      ],
      "en": {
        "title": "Hollow Howler",
        "summary": "A Deep Dark mid-boss you summon yourself: build the figure, blow a goat horn near a sculk sensor in the dark, and it manifests within 5 seconds.",
        "details": [
          "Figure: a Sculk block at the bottom, on it a T of three Sculk Catalysts, on the middle catalyst a jaw (a Sculk Shrieker; with SimpleBuilding also its Sculk Jaw - block tag simplemobs:howler_jaw), on the jaw a skeleton skull. The arms may point either way.",
          "A sculk sensor anywhere within 5 blocks of the middle catalyst must hear the call: blow any goat horn within 16 blocks. Light at the skull (block and sky light) must be 7 or less, not on Peaceful, Overworld only by default.",
          "The manifestation takes 5 seconds. Breaking the sensor or lighting up the skull aborts it with smoke, a fading click and a message - every block stays. Only at the end are the figure's blocks used up; the sensor stays and the ground around turns to sculk.",
          "240 health, 6 armor, bite 14 (Darkness 4 s). Blind like the Warden: it hunts what it hears (sneaking and wool hide you) and its own steps are silent. Hits while its jaws open for a bite deal 1.5x damage.",
          "Echo shriek every 12 s at targets 6-10 blocks away (8 damage, push). Below 50 % it hungers: faster, longer reach and traps around you every 15 s (SimpleBuilding Sculk Jaws for 20 s, otherwise sculk snares). Below 20 % it rages: shrieks every 6 s and ignores knockback. It never calls reinforcements.",
          "Drops: 3-5 echo shards, one Echo Core, (with SimpleBuilding) 1-2 Sculk Jaws plus Looting, at most 4 - without SimpleBuilding one more echo shard instead. 50 XP. Never spawns naturally.",
          "Server config simplemobs-server.json: howlerEnabled, howlerMaxLight, howlerSculkTrail, howlerSculkTrailMax, howlerTransformSculk, howlerHunger, howlerDespawnMinutes, howlerAllowDimensions. mobGriefing off: no sculk trail, no sculk ground, Vanilla snares instead of jaw blocks."
        ]
      },
      "de": {
        "title": "Tiefenheuler",
        "summary": "Ein Deep-Dark-Zwischenboss, den du selbst beschwörst: Figur bauen, im Dunkeln nahe einem Sculk-Sensor ins Ziegenhorn blasen, und er manifestiert sich in 5 Sekunden.",
        "details": [
          "Figur: unten ein Sculk-Block, darauf ein T aus drei Sculk-Katalysatoren, auf dem mittleren ein Kiefer (Sculk-Kreischer; mit SimpleBuilding auch dessen Sculk-Kiefer – Block-Tag simplemobs:howler_jaw), auf dem Kiefer ein Skelettschädel. Die Arme dürfen in beide Richtungen zeigen.",
          "Ein Sculk-Sensor höchstens 5 Blöcke vom mittleren Katalysator entfernt muss den Ruf hören: ein beliebiges Ziegenhorn in bis zu 16 Blöcken Abstand blasen. Licht am Schädel (Block- und Himmelslicht) höchstens 7, nicht auf Friedlich, standardmäßig nur in der Oberwelt.",
          "Die Manifestation dauert 5 Sekunden. Wird der Sensor abgebaut oder der Schädel zu hell, bricht sie mit Rauch, verhallendem Klicken und einer Meldung ab – alle Blöcke bleiben. Erst am Ende werden die Blöcke der Figur verbraucht; der Sensor bleibt, der Boden ringsum wird zu Sculk.",
          "240 Leben, 6 Rüstung, Biss 14 (Dunkelheit 4 s). Blind wie der Wärter: Er jagt, was er hört (Schleichen und Wolle verbergen dich), seine eigenen Schritte sind lautlos. Treffer, während sich seine Kiefer zum Biss öffnen, machen 1,5-fachen Schaden.",
          "Echo-Schrei alle 12 s gegen Ziele 6–10 Blöcke entfernt (8 Schaden, Rückstoß). Unter 50 % hungert er: schneller, mehr Reichweite und alle 15 s Fallen um dich (SimpleBuilding-Sculk-Kiefer für 20 s, sonst Sculk-Schlingen). Unter 20 % tobt er: Schrei alle 6 s, kein Rückstoß. Er ruft nie Verstärkung.",
          "Beute: 3–5 Echosplitter, ein Echokern, (mit SimpleBuilding) 1–2 Sculk-Kiefer plus Plünderung, höchstens 4 – ohne SimpleBuilding stattdessen ein Echosplitter mehr. 50 EP. Spawnt nie natürlich.",
          "Server-Konfiguration simplemobs-server.json: howlerEnabled, howlerMaxLight, howlerSculkTrail, howlerSculkTrailMax, howlerTransformSculk, howlerHunger, howlerDespawnMinutes, howlerAllowDimensions. mobGriefing aus: keine Sculk-Spur, kein Sculk-Boden, Vanilla-Schlingen statt Kiefer-Blöcken."
        ]
      }
    },
    {
      "id": "potion_of_silence",
      "sources": [
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/SilenceEffect.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/mixin/EntitySilenceMixin.java"
      ],
      "en": {
        "title": "Potion of Silence",
        "summary": "Awkward Potion + Echo Core: 3:00 of Silence (redstone 8:00, splash and lingering as usual). Sculk sensors, Wardens and Howlers do not hear you, like wool.",
        "details": [
          "Fighting and being seen still work; only vibrations you cause are dampened."
        ]
      },
      "de": {
        "title": "Trank der Stille",
        "summary": "Seltsamer Trank + Echokern: 3:00 Stille (Redstone 8:00, Wurf- und Verweiltrank wie üblich). Sculk-Sensoren, Wärter und Tiefenheuler hören dich nicht, wie bei Wolle.",
        "details": [
          "Kämpfen und Gesehenwerden bleiben möglich; gedämpft werden nur Vibrationen, die du auslöst."
        ]
      }
    },
    {
      "id": "shellker",
      "sources": [
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/ShellkerEntity.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/ShellkerBullet.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/ShellkerShellItem.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/HighGravityEffect.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/EndNetherLogic.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/EndNetherServer.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/EndNetherMobs.java"
      ],
      "en": {
        "title": "Shellker",
        "summary": "An End gateway guard: a Shulker in a bedrock-hard shell. Four of them sit beside every End gateway, disguised as bedrock, until you come close.",
        "details": [
          "1x1x1 and it stays that size: to shoot it only opens a thin slit and a violet eye lights up. Closed it takes no damage at all (hits clack like stone); open it takes damage normally. 120 health (4x a Shulker), 10 XP.",
          "It never teleports. Only pistons (and sticky pistons) can push it away - pushing the guards aside is a legitimate way past them.",
          "Every End gateway gets exactly four, in the open cells beside the portal block at portal height, standing on the bedrock below: the dragon-fight ring, the exit gateways on the outer islands and worldgen gateways. Server config simplemobs-server.json: shellkerGatewayGuards.",
          "Its bullets home in like Shulker bullets but fly 1.5x faster and give High Gravity (10 s) instead of Levitation: jumps, Levitation and Elytra lift keep only 10 %, gliding sinks fast.",
          "Drops 2-3 Shellker Shells (2.5 on average, Looting +0.5 per level). A Shellker bullet that hits an open Shellker can bud a new one into a free neighbour cell (like Shulkers). It never spawns anywhere else - to farm them, right-click a Shulker with a Shellker Shell: it turns into a Shellker (the shell is used up)."
        ]
      },
      "de": {
        "title": "Shellker",
        "summary": "Ein Wächter der End-Gateways: ein Shulker in einer grundgesteinharten Schale. Vier sitzen an jedem End-Gateway, getarnt als Grundgestein, bis man nahe kommt.",
        "details": [
          "1x1x1 und bleibt so groß: zum Schießen öffnet er nur einen schmalen Spalt, ein violettes Auge leuchtet auf. Geschlossen nimmt er gar keinen Schaden (Treffer klacken wie Stein), offen normal. 120 Leben (4x Shulker), 10 EP.",
          "Er teleportiert sich nie. Nur Kolben (auch klebrige) können ihn verschieben - die Wächter wegzuschieben ist ein erlaubter Weg vorbei.",
          "Jedes End-Gateway bekommt genau vier, in den freien Feldern neben dem Portalblock auf Portalhöhe, auf dem Grundgestein darunter: der Ring nach dem Drachenkampf, die Ausgangs-Gateways auf den äußeren Inseln und Gateways aus der Weltgenerierung. Server-Config simplemobs-server.json: shellkerGatewayGuards.",
          "Seine Geschosse suchen ihr Ziel wie Shulker-Geschosse, fliegen aber 1,5x schneller und geben Schwerkraft (10 s) statt Schweben: Sprünge, Schweben und Elytra-Auftrieb behalten nur 10 %, Gleiten sinkt schnell.",
          "Lässt 2-3 Shellker-Schalen fallen (im Schnitt 2,5, Plünderung +0,5 je Stufe). Trifft ein Shellker-Geschoss einen offenen Shellker, kann ein neuer in ein freies Nachbarfeld knospen (wie bei Shulkern). Woanders erscheint er nie - zum Farmen einen Shulker mit einer Shellker-Schale rechtsklicken: er wird zum Shellker (die Schale wird verbraucht)."
        ]
      }
    },
    {
      "id": "high_gravity",
      "sources": [
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/HighGravityEffect.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/EndNetherLogic.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/mixin/LivingEntityGravityMixin.java"
      ],
      "en": {
        "title": "High Gravity and the Shellker Helmet",
        "summary": "High Gravity pulls you down: jumps, Levitation and Elytra lift keep 10 %. The Shellker Helmet makes you immune.",
        "details": [
          "From Shellker bullets (10 s) or the Potion of High Gravity: Awkward Potion + Shellker Shell = 3:00, + Redstone 8:00; Gunpowder and Dragon's Breath make splash and lingering potions.",
          "Shellker Helmet: 5 Shellker Shells in helmet shape. Armor 3, toughness 1, knockback resistance 10 %, iron durability, repaired with Shellker Shells. Its wearer is immune to High Gravity (not to the bullets' damage) and takes 20 % less fall damage. A new helmet - the Turtle Shell stays as it is. Can be trimmed and enchanted like other helmets."
        ]
      },
      "de": {
        "title": "Schwerkraft und Shellker-Helm",
        "summary": "Schwerkraft zieht nach unten: Sprünge, Schweben und Elytra-Auftrieb behalten 10 %. Der Shellker-Helm macht immun.",
        "details": [
          "Durch Shellker-Geschosse (10 s) oder den Trank der Schwerkraft: Seltsamer Trank + Shellker-Schale = 3:00, + Redstone 8:00; Schwarzpulver und Drachenatem ergeben Wurf- und Verweiltrank.",
          "Shellker-Helm: 5 Shellker-Schalen in Helmform. Rüstung 3, Härte 1, Rückstoßresistenz 10 %, Haltbarkeit wie Eisen, Reparatur mit Shellker-Schalen. Der Träger ist immun gegen Schwerkraft (nicht gegen den Schaden der Geschosse) und nimmt 20 % weniger Fallschaden. Ein neuer Helm - der Schildkrötenpanzer bleibt unverändert. Lässt sich wie andere Helme verzieren und verzaubern."
        ]
      }
    },
    {
      "id": "soulfire_blaze",
      "sources": [
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/SoulfireBlaze.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/SoulFireball.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/EndNetherServer.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/EndNetherLogic.java"
      ],
      "en": {
        "title": "Soulfire Blaze",
        "summary": "A Blaze of soul fire, found rarely in Soul Sand Valleys. Shoots volleys of soul fireballs and drops the Soulfire Rod.",
        "details": [
          "Same size, flight, 20 health and weaknesses as a Blaze: water hurts it, snowballs deal 3 damage. Fire immune, 12 XP.",
          "Attack: charges for 3 s (soul flames swirl around it), then three soul fireballs. Each deals 6 damage and sets the target on fire for 6 s (Fire Resistance protects); on soul sand or soul soil the fire it lights is soul fire.",
          "Spawning: only in Soul Sand Valleys, over soul sand or soul soil, in packs of 1-2, at most 3 near a player. Server config simplemobs-server.json: soulfireBlazeSpawns, soulfireBlazeChance (per 10 s roll, default 0.2).",
          "Drops 0-1 Soulfire Rod when killed by a player (Looting +1). Awkward Potion + Soulfire Rod brews long Fire Resistance (8:00) directly."
        ]
      },
      "de": {
        "title": "Seelenfeuer-Lohe",
        "summary": "Eine Lohe aus Seelenfeuer, selten in Seelensandtälern. Schießt Salven aus Seelenfeuerkugeln und lässt die Seelenfeuer-Rute fallen.",
        "details": [
          "Gleiche Größe, gleicher Flug, 20 Leben und dieselben Schwächen wie eine Lohe: Wasser schadet ihr, Schneebälle machen 3 Schaden. Feuerimmun, 12 EP.",
          "Angriff: lädt 3 s auf (Seelenflammen wirbeln um sie), dann drei Seelenfeuerkugeln. Jede macht 6 Schaden und setzt das Ziel 6 s in Brand (Feuerresistenz schützt); auf Seelensand oder Seelenerde wird das gelegte Feuer zu Seelenfeuer.",
          "Spawn: nur in Seelensandtälern, über Seelensand oder Seelenerde, in Gruppen zu 1-2, höchstens 3 in der Nähe eines Spielers. Server-Config simplemobs-server.json: soulfireBlazeSpawns, soulfireBlazeChance (je 10-s-Wurf, Standard 0,2).",
          "Lässt 0-1 Seelenfeuer-Rute fallen, wenn ein Spieler sie tötet (Plünderung +1). Seltsamer Trank + Seelenfeuer-Rute braut direkt lange Feuerresistenz (8:00)."
        ]
      }
    },
    {
      "id": "deceiver_disguise",
      "sources": [
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/Disguise.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/CloakLogic.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/TrickMirrorItem.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/DisguiseRegistry.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/DeceiverLogic.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/DeceiverEntity.java"
      ],
      "en": {
        "title": "Deceiver: cloak, disguise, mirror",
        "summary": "From Deceiver Cloth: a cloak that lets you take any mob's or player's shape, a particle-free invisibility potion and a mirror that sees through deceptions. The Deceiver itself has double life, takes the hit box of the mob it imitates and can double its summons.",
        "details": [
          "Cloak of Deception (chest slot, like the Elytra, 240 durability, repaired with Deceiver Cloth): recipe Cloth-Cloth-Cloth / Phantom Membrane - . - Phantom Membrane / Cloth - . - Cloth; also dropped by the Deceiver (3 %, +2 % per Looting level).",
          "Worn, mobs notice you only within 3 blocks. With an empty hand right-click a mob to take its shape (look, hit box and eye height); mobs of that kind take you for one of theirs. Right-click another player to look like them, with their skin and the armor and gear they wore at that moment. Each disguise costs 1 durability.",
          "Hitting anything or breaking a block ends the disguise. Hurting a mob reveals you: it and every mob in the 5x5 around it know you for 60 s, all others notice you within 8 blocks for 30 s (still less than vanilla); the cloak loses 10 durability.",
          "Bosses (Wither, Ender Dragon, Elder Guardian), the Warden and the Hollow Howler see through the cloak and cannot be imitated (entity tag simplemobs:sees_through_disguise).",
          "Potion of Veiled Invisibility: Potion of Invisibility + Deceiver Cloth (also splash and lingering), Redstone for 8:00, Gunpowder / Dragon's Breath as usual. Vanilla invisibility without swirl particles.",
          "Trick Mirror: Gold Ingot - Amethyst Shard - Gold Ingot / Gold Ingot - Deceiver Cloth - Gold Ingot / - Stick -; also 8 % Deceiver drop. Hold it up for 2 s: in a cone of 30 degrees up to 24 blocks fakes and disguised Deceivers or players glow for 5 s and every disguise falls. 20 s cooldown, 64 uses.",
          "The Deceiver has 200 health (280 armored) and turns into one of its wave's mobs, taking its hit box only where there is room. Special attack: after a wave it may double its summons - 5 % at full health, rising with the square of the lost health to 80 % at 20 % health and below; 30 s cooldown, at most 8 copies and never above 24 summons. Copies are always fakes and vanish with it.",
          "Drops: Deceiver Cloth 2-4 with a 50 % chance (never doubled), Illusion Shard 1-2 (+Looting). An armored Deceiver (chest plate) has 280 health and 6 armor and drops twice the Illusion Shards.",
          "(With SimpleBuilding) Mirage potions from the Illusion Shard: Awkward Potion + Illusion Shard = Potion of the Mirage (\"hostile\": peaceful mobs look hostile); + Fermented Spider Eye = Potion of the Reverse Mirage (\"peaceful\": hostile mobs look peaceful); Redstone lengthens both to 8:00, Gunpowder / Dragon's Breath make splash and lingering potions. No stronger level: the effect has no strength."
        ]
      },
      "de": {
        "title": "Deceiver: Umhang, Verwandlung, Spiegel",
        "summary": "Aus Täuscherstoff: ein Umhang, mit dem du die Gestalt jedes Mobs oder Spielers annimmst, ein Unsichtbarkeitstrank ohne Partikel und ein Spiegel, der Täuschungen durchschaut. Der Deceiver selbst hat doppeltes Leben, nimmt die Hitbox des nachgeahmten Mobs an und kann seine Beschwörungen verdoppeln.",
        "details": [
          "Tarnumhang (Brustplatz wie die Elytren, 240 Haltbarkeit, Reparatur mit Täuscherstoff): Rezept Stoff-Stoff-Stoff / Phantomhaut - . - Phantomhaut / Stoff - . - Stoff; außerdem Deceiver-Drop (3 %, +2 % je Plünderungsstufe).",
          "Getragen bemerken dich Mobs erst im 3-Block-Radius. Mit leerer Hand Rechtsklick auf einen Mob: du nimmst seine Gestalt an (Aussehen, Hitbox, Augenhöhe); Mobs dieser Art halten dich für ihresgleichen. Rechtsklick auf einen Spieler: du siehst aus wie er, mit Skin sowie der Rüstung und Ausrüstung, die er in dem Moment trug. Jede Verwandlung kostet 1 Haltbarkeit.",
          "Schlagen oder Block abbauen beendet die Verwandlung. Verletzt du einen Mob, bist du enttarnt: er und alle Mobs im 5x5-Bereich um ihn erkennen dich 60 s lang, alle anderen bemerken dich 30 s lang im Umkreis von 8 Blöcken (weiter weniger als Vanilla); der Umhang verliert 10 Haltbarkeit.",
          "Bosse (Wither, Enderdrache, Großer Wächter), der Warden und der Hollow Howler durchschauen den Umhang und sind keine Verwandlungsziele (Entity-Tag simplemobs:sees_through_disguise).",
          "Trank der stillen Unsichtbarkeit: Trank der Unsichtbarkeit + Täuscherstoff (auch Wurf- und Verweiltrank), Redstone für 8:00, Schwarzpulver / Drachenatem wie üblich. Vanilla-Unsichtbarkeit ohne Wirbelpartikel.",
          "Trugbild-Spiegel: Goldbarren - Amethystscherbe - Goldbarren / Goldbarren - Täuscherstoff - Goldbarren / - Stock -; außerdem 8 % Deceiver-Drop. 2 s hochhalten: in einem 30-Grad-Kegel bis 24 Blöcke leuchten Fälschungen und verkleidete Deceiver oder Spieler 5 s lang, jede Verwandlung fällt. 20 s Abklingzeit, 64 Nutzungen.",
          "Der Deceiver hat 200 Leben (280 mit Platte) und verwandelt sich in einen Mob seiner Welle, mit dessen Hitbox nur wo Platz ist. Spezialangriff: nach einer Welle kann er seine Beschwörungen verdoppeln - 5 % bei vollem Leben, mit dem Quadrat des verlorenen Lebens steigend bis 80 % ab 20 % Leben; 30 s Abklingzeit, höchstens 8 Kopien und nie über 24 Beschwörungen. Kopien sind immer Trugbilder und verschwinden mit ihm.",
          "Drops: Täuscherstoff 2-4 mit 50 % Chance (nie doppelt), Trugsplitter 1-2 (+Plünderung). Ein Deceiver mit Brustplatte hat 280 Leben und 6 Rüstung und droppt doppelt so viele Trugsplitter.",
          "(Mit SimpleBuilding) Trugbild-Tränke aus dem Trugsplitter: Seltsamer Trank + Trugsplitter = Trank des Trugbilds (\"feindlich\": friedliche Mobs wirken feindlich); + Fermentiertes Spinnenauge = Trank des umgekehrten Trugbilds (\"friedlich\": feindliche Mobs wirken friedlich); Redstone verlängert beide auf 8:00, Schwarzpulver / Drachenatem ergeben Wurf- und Verweiltränke. Keine stärkere Stufe: der Effekt hat keine Stärke."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simplemobs:brewing/lingering_potion_awkward_echo_core",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/lingering_potion_awkward_echo_core.json",
      "ingredients": [
        "minecraft:lingering_potion"
      ],
      "slots": {
        "input": [
          "minecraft:lingering_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/lingering_potion_awkward_shellker_shell",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/lingering_potion_awkward_shellker_shell.json",
      "ingredients": [
        "minecraft:lingering_potion"
      ],
      "slots": {
        "input": [
          "minecraft:lingering_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/lingering_potion_awkward_soulfire_rod",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/lingering_potion_awkward_soulfire_rod.json",
      "ingredients": [
        "minecraft:lingering_potion"
      ],
      "slots": {
        "input": [
          "minecraft:lingering_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/lingering_potion_high_gravity_redstone",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/lingering_potion_high_gravity_redstone.json",
      "ingredients": [
        "minecraft:lingering_potion"
      ],
      "slots": {
        "input": [
          "minecraft:lingering_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/lingering_potion_invisibility_deceiver_cloth",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/lingering_potion_invisibility_deceiver_cloth.json",
      "ingredients": [
        "minecraft:lingering_potion"
      ],
      "slots": {
        "input": [
          "minecraft:lingering_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/lingering_potion_long_invisibility_deceiver_cloth",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/lingering_potion_long_invisibility_deceiver_cloth.json",
      "ingredients": [
        "minecraft:lingering_potion"
      ],
      "slots": {
        "input": [
          "minecraft:lingering_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/lingering_potion_silence_redstone",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/lingering_potion_silence_redstone.json",
      "ingredients": [
        "minecraft:lingering_potion"
      ],
      "slots": {
        "input": [
          "minecraft:lingering_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/lingering_potion_veiled_invisibility_redstone",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/lingering_potion_veiled_invisibility_redstone.json",
      "ingredients": [
        "minecraft:lingering_potion"
      ],
      "slots": {
        "input": [
          "minecraft:lingering_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_awkward_echo_core",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_awkward_echo_core.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_awkward_illusion_shard",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_awkward_illusion_shard.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_awkward_shellker_shell",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_awkward_shellker_shell.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_awkward_soulfire_rod",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_awkward_soulfire_rod.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_high_gravity_gunpowder",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_high_gravity_gunpowder.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_high_gravity_redstone",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_high_gravity_redstone.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_invisibility_deceiver_cloth",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_invisibility_deceiver_cloth.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_long_high_gravity_gunpowder",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_long_high_gravity_gunpowder.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_long_invisibility_deceiver_cloth",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_long_invisibility_deceiver_cloth.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_long_silence_gunpowder",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_long_silence_gunpowder.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_long_veiled_invisibility_gunpowder",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_long_veiled_invisibility_gunpowder.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_silence_gunpowder",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_silence_gunpowder.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_silence_redstone",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_silence_redstone.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_veiled_invisibility_gunpowder",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_veiled_invisibility_gunpowder.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/potion_veiled_invisibility_redstone",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_veiled_invisibility_redstone.json",
      "ingredients": [
        "minecraft:potion"
      ],
      "slots": {
        "input": [
          "minecraft:potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_awkward_echo_core",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_awkward_echo_core.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_awkward_shellker_shell",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_awkward_shellker_shell.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_awkward_soulfire_rod",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_awkward_soulfire_rod.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_high_gravity_dragon_breath",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_high_gravity_dragon_breath.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_high_gravity_redstone",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_high_gravity_redstone.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_invisibility_deceiver_cloth",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_invisibility_deceiver_cloth.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_long_high_gravity_dragon_breath",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_long_high_gravity_dragon_breath.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_long_invisibility_deceiver_cloth",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_long_invisibility_deceiver_cloth.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_long_silence_dragon_breath",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_long_silence_dragon_breath.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_long_veiled_invisibility_dragon_breath",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_long_veiled_invisibility_dragon_breath.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_silence_dragon_breath",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_silence_dragon_breath.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_silence_redstone",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_silence_redstone.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_veiled_invisibility_dragon_breath",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_veiled_invisibility_dragon_breath.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:brewing/splash_potion_veiled_invisibility_redstone",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_veiled_invisibility_redstone.json",
      "ingredients": [
        "minecraft:splash_potion"
      ],
      "slots": {
        "input": [
          "minecraft:splash_potion"
        ]
      }
    },
    {
      "id": "simplemobs:deceiver_cloak",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": null,
      "result": {
        "id": "simplemobs:deceiver_cloak",
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/deceiver_cloak.json",
      "ingredients": [
        "minecraft:phantom_membrane",
        "simplemobs:deceiver_cloth"
      ],
      "pattern": [
        "CCC",
        "P P",
        "C C"
      ],
      "key": {
        "C": [
          "simplemobs:deceiver_cloth"
        ],
        "P": [
          "minecraft:phantom_membrane"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simplemobs:deceiver_cloth",
            "count": 5
          },
          {
            "id": "minecraft:phantom_membrane",
            "count": 2
          }
        ]
      }
    },
    {
      "id": "simplemobs:shellker_helmet",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": null,
      "result": {
        "id": "simplemobs:shellker_helmet",
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/shellker_helmet.json",
      "ingredients": [
        "simplemobs:shellker_shell"
      ],
      "pattern": [
        "XXX",
        "X X"
      ],
      "key": {
        "X": [
          "simplemobs:shellker_shell"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simplemobs:shellker_shell",
            "count": 5
          }
        ]
      }
    },
    {
      "id": "simplemobs:trick_mirror",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": null,
      "result": {
        "id": "simplemobs:trick_mirror",
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/trick_mirror.json",
      "ingredients": [
        "minecraft:amethyst_shard",
        "minecraft:gold_ingot",
        "minecraft:stick",
        "simplemobs:deceiver_cloth"
      ],
      "pattern": [
        "GAG",
        "GCG",
        " S "
      ],
      "key": {
        "G": [
          "minecraft:gold_ingot"
        ],
        "A": [
          "minecraft:amethyst_shard"
        ],
        "C": [
          "simplemobs:deceiver_cloth"
        ],
        "S": [
          "minecraft:stick"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:gold_ingot",
            "count": 4
          },
          {
            "id": "minecraft:amethyst_shard",
            "count": 1
          },
          {
            "id": "simplemobs:deceiver_cloth",
            "count": 1
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.125
          }
        ]
      }
    }
  ],
  "lootTables": [
    {
      "id": "simplemobs:entities/deceiver",
      "kind": "entities",
      "type": "minecraft:entity",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobs:deceiver_cloth"
          ],
          "conditions": [
            "minecraft:random_chance"
          ],
          "functions": []
        },
        {
          "rolls": 1,
          "items": [
            "simplemobs:illusion_shard"
          ],
          "conditions": [],
          "functions": []
        },
        {
          "rolls": 1,
          "items": [
            "simplemobs:deceiver_cloak"
          ],
          "conditions": [
            "minecraft:killed_by_player",
            "minecraft:random_chance_with_enchanted_bonus"
          ],
          "functions": []
        },
        {
          "rolls": 1,
          "items": [
            "simplemobs:trick_mirror"
          ],
          "conditions": [
            "minecraft:killed_by_player",
            "minecraft:random_chance_with_enchanted_bonus"
          ],
          "functions": []
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/loot_table/entities/deceiver.json"
    },
    {
      "id": "simplemobs:entities/deceiver_armor_bonus",
      "kind": "entities",
      "type": "minecraft:entity",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobs:illusion_shard"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/loot_table/entities/deceiver_armor_bonus.json"
    },
    {
      "id": "simplemobs:entities/hollow_howler",
      "kind": "entities",
      "type": "minecraft:entity",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "minecraft:echo_shard"
          ],
          "conditions": [],
          "functions": []
        },
        {
          "rolls": 1,
          "items": [
            "simplemobs:echo_core"
          ],
          "conditions": [],
          "functions": []
        },
        {
          "rolls": 1,
          "items": [],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/loot_table/entities/hollow_howler.json"
    },
    {
      "id": "simplemobs:entities/shellker",
      "kind": "entities",
      "type": "minecraft:entity",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobs:shellker_shell"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/loot_table/entities/shellker.json"
    },
    {
      "id": "simplemobs:entities/soulfire_blaze",
      "kind": "entities",
      "type": "minecraft:entity",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobs:soulfire_rod"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/loot_table/entities/soulfire_blaze.json"
    },
    {
      "id": "simplemobs:entities/summoned",
      "kind": "entities",
      "type": "minecraft:entity",
      "pools": [],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/loot_table/entities/summoned.json"
    }
  ],
  "tags": [
    {
      "id": "simplemobs:block/howler_jaw",
      "replace": false,
      "values": [
        {
          "id": "minecraft:sculk_shrieker",
          "required": true
        },
        {
          "id": "simplebuilding:sculk_jaw",
          "required": false
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/tags/block/howler_jaw.json"
    },
    {
      "id": "simplemobs:block/howler_replaceable",
      "replace": false,
      "values": [
        {
          "id": "#minecraft:dirt",
          "required": true
        },
        {
          "id": "minecraft:grass_block",
          "required": true
        },
        {
          "id": "minecraft:stone",
          "required": true
        },
        {
          "id": "minecraft:deepslate",
          "required": true
        },
        {
          "id": "minecraft:tuff",
          "required": true
        },
        {
          "id": "minecraft:cobbled_deepslate",
          "required": true
        },
        {
          "id": "minecraft:sand",
          "required": true
        },
        {
          "id": "minecraft:red_sand",
          "required": true
        },
        {
          "id": "minecraft:gravel",
          "required": true
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/tags/block/howler_replaceable.json"
    },
    {
      "id": "simplemobs:entity_type/sees_through_disguise",
      "replace": false,
      "values": [
        {
          "id": "minecraft:wither",
          "required": true
        },
        {
          "id": "minecraft:ender_dragon",
          "required": true
        },
        {
          "id": "minecraft:elder_guardian",
          "required": true
        },
        {
          "id": "minecraft:warden",
          "required": true
        },
        {
          "id": "simplemobs:hollow_howler",
          "required": true
        },
        {
          "id": "#c:bosses",
          "required": false
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/tags/entity_type/sees_through_disguise.json"
    },
    {
      "id": "simplemobs:item/howler_jaw_drops",
      "replace": false,
      "values": [
        {
          "id": "simplebuilding:sculk_jaw",
          "required": false
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/tags/item/howler_jaw_drops.json"
    },
    {
      "id": "simplemobs:item/repairs_shellker_helmet",
      "replace": false,
      "values": [
        {
          "id": "simplemobs:shellker_shell",
          "required": true
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/tags/item/repairs_shellker_helmet.json"
    }
  ],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "simplemobs:deceiver_cloak",
      "name": {
        "en_us": "Cloak of Deception",
        "de_de": "Tarnumhang"
      },
      "note": {
        "en": {
          "summary": "Worn on the chest: mobs notice you only within 3 blocks; empty-handed right-click a mob or player to take its shape. Hitting or breaking a block ends it."
        },
        "de": {
          "summary": "Auf dem Brustplatz: Mobs bemerken dich erst im 3-Block-Radius; mit leerer Hand Rechtsklick auf Mob oder Spieler = Verwandlung. Schlagen oder Abbauen beendet sie."
        }
      },
      "texture": "assets/textures/simplemobs/item/deceiver_cloak.png",
      "craftedBy": [
        "simplemobs:deceiver_cloak"
      ],
      "usedIn": []
    },
    {
      "id": "simplemobs:deceiver_cloth",
      "name": {
        "en_us": "Deceiver Cloth",
        "de_de": "Täuscherstoff"
      },
      "note": {
        "en": {
          "summary": "Shimmering cloth dropped by the Deceiver. Material for the Cloak of Deception, the Trick Mirror and the Potion of Veiled Invisibility."
        },
        "de": {
          "summary": "Schimmernder Stoff, den der Deceiver fallen lässt. Material für den Tarnumhang, den Trugbild-Spiegel und den Trank der stillen Unsichtbarkeit."
        }
      },
      "texture": "assets/textures/simplemobs/item/deceiver_cloth.png",
      "craftedBy": [],
      "usedIn": [
        "simplemobs:deceiver_cloak",
        "simplemobs:trick_mirror"
      ]
    },
    {
      "id": "simplemobs:deceiver_spawn_egg",
      "name": {
        "en_us": "Deceiver Spawn Egg",
        "de_de": "Deceiver-Spawn-Ei"
      },
      "note": {
        "en": {
          "summary": "Spawns a Deceiver. Creative only."
        },
        "de": {
          "summary": "Beschwört einen Deceiver. Nur im Kreativmodus."
        }
      },
      "texture": "assets/textures/simplemobs/item/deceiver_spawn_egg.png",
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:echo_core",
      "name": {
        "en_us": "Echo Core",
        "de_de": "Echokern"
      },
      "note": {
        "en": {
          "summary": "Dropped once by every Hollow Howler. Brewed into an Awkward Potion it makes the Potion of Silence (3:00, with redstone 8:00)."
        },
        "de": {
          "summary": "Lässt jeder Tiefenheuler genau einmal fallen. In einen Seltsamen Trank gebraut ergibt er den Trank der Stille (3:00, mit Redstone 8:00)."
        }
      },
      "texture": "assets/textures/simplemobs/item/echo_core.png",
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:hollow_howler_spawn_egg",
      "name": {
        "en_us": "Hollow Howler Spawn Egg",
        "de_de": "Tiefenheuler-Spawn-Ei"
      },
      "note": {
        "en": {
          "summary": "Spawns a Hollow Howler. Creative only."
        },
        "de": {
          "summary": "Beschwört einen Tiefenheuler. Nur im Kreativmodus."
        }
      },
      "texture": "assets/textures/simplemobs/item/hollow_howler_spawn_egg.png",
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:illusion_shard",
      "name": {
        "en_us": "Illusion Shard",
        "de_de": "Trugsplitter"
      },
      "note": {
        "en": {
          "summary": "The Deceiver's second drop (1-2, armored Deceivers twice as many). (With SimpleBuilding) Awkward Potion + Illusion Shard = Potion of the Mirage."
        },
        "de": {
          "summary": "Zweiter Drop des Deceivers (1-2, mit Brustplatte doppelt so viele). (Mit SimpleBuilding) Seltsamer Trank + Trugsplitter = Trank des Trugbilds."
        }
      },
      "texture": "assets/textures/simplemobs/item/illusion_shard.png",
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:shellker_helmet",
      "name": {
        "en_us": "Shellker Helmet",
        "de_de": "Shellker-Helm"
      },
      "note": {
        "en": {
          "summary": "Helmet from 5 Shellker Shells: armor 3, toughness 1, knockback resistance 10 %, immune to High Gravity, 20 % less fall damage. Trimmable."
        },
        "de": {
          "summary": "Helm aus 5 Shellker-Schalen: Rüstung 3, Härte 1, Rückstoßresistenz 10 %, immun gegen Schwerkraft, 20 % weniger Fallschaden. Verzierbar."
        }
      },
      "texture": "assets/textures/simplemobs/item/shellker_helmet.png",
      "craftedBy": [
        "simplemobs:shellker_helmet"
      ],
      "usedIn": []
    },
    {
      "id": "simplemobs:shellker_shell",
      "name": {
        "en_us": "Shellker Shell",
        "de_de": "Shellker-Schale"
      },
      "note": {
        "en": {
          "summary": "Dropped by Shellkers (2-3). Right-click a Shulker with it to turn the Shulker into a Shellker. Brews the Potion of High Gravity; five make the Shellker Helmet."
        },
        "de": {
          "summary": "Lassen Shellker fallen (2-3). Damit einen Shulker rechtsklicken, um ihn zum Shellker zu machen. Braut den Trank der Schwerkraft; fünf ergeben den Shellker-Helm."
        }
      },
      "texture": "assets/textures/simplemobs/item/shellker_shell.png",
      "craftedBy": [],
      "usedIn": [
        "simplemobs:shellker_helmet"
      ]
    },
    {
      "id": "simplemobs:shellker_spawn_egg",
      "name": {
        "en_us": "Shellker Spawn Egg",
        "de_de": "Shellker-Spawn-Ei"
      },
      "note": {
        "en": {
          "summary": "Spawns a Shellker (creative)."
        },
        "de": {
          "summary": "Erzeugt einen Shellker (Kreativ)."
        }
      },
      "texture": "assets/textures/simplemobs/item/shellker_spawn_egg.png",
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:soulfire_blaze_spawn_egg",
      "name": {
        "en_us": "Soulfire Blaze Spawn Egg",
        "de_de": "Seelenfeuer-Lohe-Spawn-Ei"
      },
      "note": {
        "en": {
          "summary": "Spawns a Soulfire Blaze (creative)."
        },
        "de": {
          "summary": "Erzeugt eine Seelenfeuer-Lohe (Kreativ)."
        }
      },
      "texture": "assets/textures/simplemobs/item/soulfire_blaze_spawn_egg.png",
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:soulfire_rod",
      "name": {
        "en_us": "Soulfire Rod",
        "de_de": "Seelenfeuer-Rute"
      },
      "note": {
        "en": {
          "summary": "Dropped by Soulfire Blazes. Brewed into an Awkward Potion it gives long Fire Resistance (8:00)."
        },
        "de": {
          "summary": "Lassen Seelenfeuer-Lohen fallen. In einen Seltsamen Trank gebraut ergibt sie lange Feuerresistenz (8:00)."
        }
      },
      "texture": "assets/textures/simplemobs/item/soulfire_rod.png",
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:trick_mirror",
      "name": {
        "en_us": "Trick Mirror",
        "de_de": "Trugbild-Spiegel"
      },
      "note": {
        "en": {
          "summary": "Hold up for 2 s: fakes and disguised Deceivers or players in front of you glow and lose their disguise. 20 s cooldown."
        },
        "de": {
          "summary": "2 s hochhalten: Fälschungen und verkleidete Deceiver oder Spieler vor dir leuchten auf und verlieren ihre Tarnung. 20 s Abklingzeit."
        }
      },
      "texture": "assets/textures/simplemobs/item/trick_mirror.png",
      "craftedBy": [
        "simplemobs:trick_mirror"
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
    "features": 6,
    "recipes": 40,
    "lootTables": 6,
    "tags": 5,
    "advancements": 0,
    "enchantments": 0,
    "items": 12,
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
