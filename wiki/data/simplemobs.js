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
        "summary": "A Deep Dark boss you summon yourself: build the figure, blow a goat horn near a sculk sensor in the dark, and it manifests within 5 seconds.",
        "details": [
          "Figure: a Sculk block at the bottom, on it a T of three Sculk Catalysts, on the middle catalyst a jaw (a Sculk Shrieker; with SimpleBuilding also its Sculk Jaw - block tag simplemobs:howler_jaw), on the jaw a skeleton skull. The arms may point either way.",
          "A sculk sensor anywhere within 5 blocks of the middle catalyst must hear the call: blow any goat horn within 16 blocks. Light at the skull (block and sky light) must be 7 or less, not on Peaceful, Overworld only by default.",
          "The manifestation takes 5 seconds. Breaking the sensor or lighting up the skull aborts it with smoke, a fading click and a message - every block stays. Only at the end are the figure's blocks used up; the sensor stays and the ground around turns to sculk.",
          "550 health, 8 armor, knockback resistance 1, speed 0.3 - a little stronger than the Warden (500 health, no armor). Bite 20 (Darkness 4 s), jaws open 0.6 s before they snap. Blind like the Warden: it hunts what it hears (sneaking and wool hide you) and its own steps are silent. Hits while its jaws open for a bite deal 1.5x damage.",
          "Special attacks, each announced by sound and particles, with a 1.5 s pause after each: ground shockwave when you stay close (1 s wind-up, a ring marks 5 blocks; 10 damage - jump or step out), echo shriek every 12 s at 6-10 blocks (1 s wind-up, 8 damage and push - cover in its line of sight blocks it), charge at 6-16 blocks (crouches 0.8 s on a locked line, 12 damage - sidestep; into a wall it staggers 1.5 s), sculk tendrils (marks under and around you pulse 1.5 s, then 4 damage and Slowness II 3 s - step off). Below 50 % it hungers: faster, longer reach and traps around you every 15 s (SimpleBuilding Sculk Jaws for 20 s, otherwise sculk snares). Below 20 % it rages: shrieks every 6 s, other attacks a third more often. It never calls reinforcements.",
          "Drops: 3-5 echo shards, one Echo Core, (with SimpleBuilding) 1-2 Sculk Jaws plus Looting, at most 4 - without SimpleBuilding one more echo shard instead. 50 XP. Never spawns naturally.",
          "Server config simplemobs-server.json: howlerEnabled, howlerMaxLight, howlerSculkTrail, howlerSculkTrailMax, howlerTransformSculk, howlerHunger, howlerDespawnMinutes, howlerAllowDimensions. mobGriefing off: no sculk trail, no sculk ground, Vanilla snares instead of jaw blocks."
        ]
      },
      "de": {
        "title": "Tiefenheuler",
        "summary": "Ein Deep-Dark-Boss, den du selbst beschwörst: Figur bauen, im Dunkeln nahe einem Sculk-Sensor ins Ziegenhorn blasen, und er manifestiert sich in 5 Sekunden.",
        "details": [
          "Figur: unten ein Sculk-Block, darauf ein T aus drei Sculk-Katalysatoren, auf dem mittleren ein Kiefer (Sculk-Kreischer; mit SimpleBuilding auch dessen Sculk-Kiefer – Block-Tag simplemobs:howler_jaw), auf dem Kiefer ein Skelettschädel. Die Arme dürfen in beide Richtungen zeigen.",
          "Ein Sculk-Sensor höchstens 5 Blöcke vom mittleren Katalysator entfernt muss den Ruf hören: ein beliebiges Ziegenhorn in bis zu 16 Blöcken Abstand blasen. Licht am Schädel (Block- und Himmelslicht) höchstens 7, nicht auf Friedlich, standardmäßig nur in der Oberwelt.",
          "Die Manifestation dauert 5 Sekunden. Wird der Sensor abgebaut oder der Schädel zu hell, bricht sie mit Rauch, verhallendem Klicken und einer Meldung ab – alle Blöcke bleiben. Erst am Ende werden die Blöcke der Figur verbraucht; der Sensor bleibt, der Boden ringsum wird zu Sculk.",
          "550 Leben, 8 Rüstung, Rückstoßresistenz 1, Tempo 0,3 – etwas stärker als der Wärter (500 Leben, keine Rüstung). Biss 20 (Dunkelheit 4 s), die Kiefer öffnen sich 0,6 s vor dem Zuschnappen. Blind wie der Wärter: Er jagt, was er hört (Schleichen und Wolle verbergen dich), seine eigenen Schritte sind lautlos. Treffer, während sich seine Kiefer zum Biss öffnen, machen 1,5-fachen Schaden.",
          "Spezialangriffe, jeweils per Geräusch und Partikeln angekündigt, danach 1,5 s Pause: Boden-Schockwelle, wenn du nah bleibst (1 s Aufladen, ein Ring markiert 5 Blöcke; 10 Schaden – springen oder rausgehen), Echo-Schrei alle 12 s auf 6–10 Blöcke (1 s Aufladen, 8 Schaden und Rückstoß – Deckung in seiner Sichtlinie blockt ihn), Ansturm auf 6–16 Blöcke (duckt sich 0,8 s auf einer festen Linie, 12 Schaden – zur Seite ausweichen; gegen eine Wand taumelt er 1,5 s), Sculk-Ranken (Markierungen unter und neben dir pulsieren 1,5 s, dann 4 Schaden und Langsamkeit II 3 s – runtergehen). Unter 50 % hungert er: schneller, mehr Reichweite und alle 15 s Fallen um dich (SimpleBuilding-Sculk-Kiefer für 20 s, sonst Sculk-Schlingen). Unter 20 % tobt er: Schrei alle 6 s, übrige Angriffe ein Drittel öfter. Er ruft nie Verstärkung.",
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
          "Its bullets home in like Shulker bullets but fly 1.5x faster and give High Gravity instead of Levitation (30 s; 1 min if you already have it): jumps, Levitation and Elytra lift keep only 10 %, gliding sinks fast. For their first 2 s they can be neither knocked away nor shot down. It fires 1.2x as often as a Shulker and almost at once after opening its shell, so hitting it nonstop does not keep it from shooting.",
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
          "Seine Geschosse suchen ihr Ziel wie Shulker-Geschosse, fliegen aber 1,5x schneller und geben Schwerkraft statt Schweben (30 s; 1 min, wenn man sie schon hat): Sprünge, Schweben und Elytra-Auftrieb behalten nur 10 %, Gleiten sinkt schnell. In den ersten 2 s lassen sie sich weder wegschlagen noch abschießen. Er schießt 1,2x so oft wie ein Shulker und fast sofort nach dem Öffnen der Schale - Dauerschlagen hält ihn nicht vom Schießen ab.",
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
        "summary": "High Gravity pulls you down: jumps, Levitation and Elytra lift keep 10 % and falls hurt 50 % more per level (Feather Falling still helps). The Shellker Helmet makes you immune.",
        "details": [
          "From Shellker bullets (30 s, 1 min if already under High Gravity) or the Potion of High Gravity: Awkward Potion + Shellker Shell = 3:00, + Redstone 8:00; Gunpowder and Dragon's Breath make splash and lingering potions.",
          "Shellker Helmet: 5 Shellker Shells in helmet shape. Armor 3, toughness 1, knockback resistance 10 %, iron durability, repaired with Shellker Shells. Its wearer is immune to High Gravity (not to the bullets' damage) and takes 20 % less fall damage. A new helmet - the Turtle Shell stays as it is. Can be trimmed and enchanted like other helmets."
        ]
      },
      "de": {
        "title": "Schwerkraft und Shellker-Helm",
        "summary": "Schwerkraft zieht nach unten: Sprünge, Schweben und Elytra-Auftrieb behalten 10 %, Stürze tun je Stufe 50 % mehr weh (Federfall hilft weiter). Der Shellker-Helm macht immun.",
        "details": [
          "Durch Shellker-Geschosse (30 s, 1 min bei bestehender Schwerkraft) oder den Trank der Schwerkraft: Seltsamer Trank + Shellker-Schale = 3:00, + Redstone 8:00; Schwarzpulver und Drachenatem ergeben Wurf- und Verweiltrank.",
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
          "Cloak of Deception (worn in the head slot: a hood, and the cloak hangs on the back like an Elytra; the chest stays free for armor or an Elytra, which is drawn under the cloak and spreads out sideways in flight; 240 durability, repaired with Deceiver Cloth): recipe Cloth-Cloth-Cloth / Phantom Membrane - . - Phantom Membrane / Cloth - . - Cloth; also dropped by the Deceiver (3 %, +2 % per Looting level).",
          "Worn, mobs notice you only within 3 blocks. With an empty hand right-click a mob to take its shape (look, hit box and eye height); mobs of that kind take you for one of theirs. Right-click another player to look like them, with their skin and the armor and gear they wore at that moment. Each disguise costs 1 durability.",
          "Any attack attempt - also a swing into the air - or breaking a block ends the disguise; the disguise shows in third person, on the inventory player model and on the first-person hands (a player shape with its skin, a humanoid mob with its own arm, a shape without arms without hands). Hurting a mob reveals you: it and every mob in the 5x5 around it know you for 60 s, all others notice you within 8 blocks for 30 s (still less than vanilla); the cloak loses 10 durability.",
          "Bosses (Wither, Ender Dragon, Elder Guardian), the Warden and the Hollow Howler see through the cloak and cannot be imitated (entity tag simplemobs:sees_through_disguise).",
          "Potion of Veiled Invisibility: Potion of Invisibility + Deceiver Cloth (also splash and lingering), Redstone for 8:00, Gunpowder / Dragon's Breath as usual. Vanilla invisibility without swirl particles.",
          "Trick Mirror: Gold Ingot - Amethyst Shard - Gold Ingot / Gold Ingot - Deceiver Cloth - Gold Ingot / - Stick -; also 8 % Deceiver drop. Hold it up for 3 s: in a cone of 20 degrees up to 12 blocks the nearest three fakes, disguised Deceivers or players glow for 2 s and lose their disguise. 60 s cooldown, 64 uses (balanced 12.10. so it eases a fight without deciding it).",
          "The Deceiver has 200 health (280 armored) and turns into one of its wave's mobs, taking its hit box only where there is room. Special attack: after a wave it may double its summons - 5 % at full health, rising with the square of the lost health to 80 % at 20 % health and below; 30 s cooldown, at most 8 copies and never above 24 summons. Copies are always fakes and vanish with it.",
          "Drops: Deceiver Cloth 2-4 with a 50 % chance (never doubled), Illusion Shard 1-2 (+Looting). An armored Deceiver (chest plate) has 280 health and 6 armor and drops twice the Illusion Shards.",
          "(With SimpleBuilding) Mirage potions from the Illusion Shard: Awkward Potion + Illusion Shard = Potion of the Mirage (\"hostile\": peaceful mobs look hostile); + Fermented Spider Eye = Potion of the Reverse Mirage (\"peaceful\": hostile mobs look peaceful); Redstone lengthens both to 8:00, Gunpowder / Dragon's Breath make splash and lingering potions. No stronger level: the effect has no strength."
        ]
      },
      "de": {
        "title": "Deceiver: Umhang, Verwandlung, Spiegel",
        "summary": "Aus Täuscherstoff: ein Umhang, mit dem du die Gestalt jedes Mobs oder Spielers annimmst, ein Unsichtbarkeitstrank ohne Partikel und ein Spiegel, der Täuschungen durchschaut. Der Deceiver selbst hat doppeltes Leben, nimmt die Hitbox des nachgeahmten Mobs an und kann seine Beschwörungen verdoppeln.",
        "details": [
          "Tarnumhang (im Kopfplatz: Kapuze, der Umhang hängt wie eine Elytra auf dem Rücken; der Brustplatz bleibt frei für Rüstung oder Elytren, die unter dem Umhang liegen und im Flug seitlich herausragen; 240 Haltbarkeit, Reparatur mit Täuscherstoff): Rezept Stoff-Stoff-Stoff / Phantomhaut - . - Phantomhaut / Stoff - . - Stoff; außerdem Deceiver-Drop (3 %, +2 % je Plünderungsstufe).",
          "Getragen bemerken dich Mobs erst im 3-Block-Radius. Mit leerer Hand Rechtsklick auf einen Mob: du nimmst seine Gestalt an (Aussehen, Hitbox, Augenhöhe); Mobs dieser Art halten dich für ihresgleichen. Rechtsklick auf einen Spieler: du siehst aus wie er, mit Skin sowie der Rüstung und Ausrüstung, die er in dem Moment trug. Jede Verwandlung kostet 1 Haltbarkeit.",
          "Jeder Angriffsversuch - auch ein Schlag in die Luft - oder Block abbauen beendet die Verwandlung; die Verkleidung sieht man in der Third Person, am Spielermodell im Inventar und an den Händen in der Ego-Ansicht (Spieler-Gestalt mit dessen Skin, humanoider Mob mit eigenem Arm, Gestalt ohne Arme ohne Hände). Verletzt du einen Mob, bist du enttarnt: er und alle Mobs im 5x5-Bereich um ihn erkennen dich 60 s lang, alle anderen bemerken dich 30 s lang im Umkreis von 8 Blöcken (weiter weniger als Vanilla); der Umhang verliert 10 Haltbarkeit.",
          "Bosse (Wither, Enderdrache, Großer Wächter), der Warden und der Hollow Howler durchschauen den Umhang und sind keine Verwandlungsziele (Entity-Tag simplemobs:sees_through_disguise).",
          "Trank der stillen Unsichtbarkeit: Trank der Unsichtbarkeit + Täuscherstoff (auch Wurf- und Verweiltrank), Redstone für 8:00, Schwarzpulver / Drachenatem wie üblich. Vanilla-Unsichtbarkeit ohne Wirbelpartikel.",
          "Trugbild-Spiegel: Goldbarren - Amethystscherbe - Goldbarren / Goldbarren - Täuscherstoff - Goldbarren / - Stock -; außerdem 8 % Deceiver-Drop. 3 s hochhalten: in einem 20-Grad-Kegel bis 12 Blöcke leuchten die drei nächsten Fälschungen, verkleideten Deceiver oder Spieler 2 s lang und verlieren ihre Verwandlung. 60 s Abklingzeit, 64 Nutzungen (am 12.10. ausbalanciert: erleichtert den Kampf, entscheidet ihn nicht).",
          "Der Deceiver hat 200 Leben (280 mit Platte) und verwandelt sich in einen Mob seiner Welle, mit dessen Hitbox nur wo Platz ist. Spezialangriff: nach einer Welle kann er seine Beschwörungen verdoppeln - 5 % bei vollem Leben, mit dem Quadrat des verlorenen Lebens steigend bis 80 % ab 20 % Leben; 30 s Abklingzeit, höchstens 8 Kopien und nie über 24 Beschwörungen. Kopien sind immer Trugbilder und verschwinden mit ihm.",
          "Drops: Täuscherstoff 2-4 mit 50 % Chance (nie doppelt), Trugsplitter 1-2 (+Plünderung). Ein Deceiver mit Brustplatte hat 280 Leben und 6 Rüstung und droppt doppelt so viele Trugsplitter.",
          "(Mit SimpleBuilding) Trugbild-Tränke aus dem Trugsplitter: Seltsamer Trank + Trugsplitter = Trank des Trugbilds (\"feindlich\": friedliche Mobs wirken feindlich); + Fermentiertes Spinnenauge = Trank des umgekehrten Trugbilds (\"friedlich\": feindliche Mobs wirken friedlich); Redstone verlängert beide auf 8:00, Schwarzpulver / Drachenatem ergeben Wurf- und Verweiltränke. Keine stärkere Stufe: der Effekt hat keine Stärke."
        ]
      }
    },
    {
      "id": "dread",
      "sources": [
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/DreadEntity.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/DreadPattern.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/FearLogic.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/FearAura.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/mixin/ItemHelmetDreadMixin.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/MobsConfig.java"
      ],
      "en": {
        "title": "Dread",
        "summary": "A fear mob you build yourself: a 2.6 block knight of rusted armor with soul flames in its empty helmet and a chain mace. Its Shivering aura makes your aim waver; bright light weakens it.",
        "details": [
          "Figure, front view: the Iron Golem's T - a Block of Iron at the bottom, three across above it, but the middle one is an anvil (any damage) - and a Skeleton Skull on top. The blocks beside the bottom block and beside the skull must be air. Then right-click the skull with an Iron Helmet: the helmet goes on and the Dread wakes (the helmet, skull and blocks are used up). On a wrong figure smoke rises and you keep the helmet. About 63 iron ingots. Not on Peaceful; server config simplemobs-server.json: dreadSummonEnabled. It never spawns naturally.",
          "It rises from the ground for 3 seconds (invulnerable), then hunts survival players. 260 health, armor 14, toughness 4, no knockback, slow. 60 XP.",
          "Mace swing, announced by the raised mace and a chain rattle for 0.7 s: 18 damage (Hard 27) and a heavy push. A shield that blocks it is disabled for 10 s (an axe: 5 s).",
          "Ground stomp, announced for 1.2 s by both arms lifting the mace, a roar and a ring of soul particles (radius 6): everyone on the ground in the ring takes 10 damage and is thrown out. Jump to dodge it. Cooldown 8 s.",
          "Weakness: light (block and sky) of 12 or more where it stands halves the damage of all its attacks and caps its aura at Shivering I; its soul flames burn low and smoke.",
          "Drops 2-4 Dread Links when a player lands the last hit (Looting +1)."
        ]
      },
      "de": {
        "title": "Schreckensritter",
        "summary": "Ein Furcht-Mob, den du selbst baust: ein 2,6 Blöcke großer Ritter in verrosteter Rüstung, Seelenflammen im leeren Helm und eine Kettenkeule. Seine Zitter-Aura lässt das Zielen wackeln; helles Licht schwächt ihn.",
        "details": [
          "Figur von vorn: das T des Eisengolems – unten ein Eisenblock, darüber drei quer, aber der mittlere ist ein Amboss (beliebig beschädigt) – und obendrauf ein Skelettschädel. Neben dem unteren Block und neben dem Schädel muss Luft sein. Dann mit einem Eisenhelm auf den Schädel rechtsklicken: Der Helm wird aufgesetzt und der Schreckensritter erwacht (Helm, Schädel und Blöcke werden verbraucht). Bei falscher Figur steigt Rauch auf und der Helm bleibt in der Hand. Etwa 63 Eisenbarren. Nicht auf Friedlich; Server-Config simplemobs-server.json: dreadSummonEnabled. Er erscheint nie von selbst.",
          "Er steigt 3 Sekunden lang aus dem Boden (unverwundbar) und jagt dann Spieler im Überlebensmodus. 260 Leben, Rüstung 14, Härte 4, kein Rückstoß, langsam. 60 EP.",
          "Keulenschlag, 0,7 s angekündigt durch die erhobene Keule und Kettenrasseln: 18 Schaden (Schwer 27) und starker Stoß. Ein Schild, der ihn blockt, ist 10 s gesperrt (Axt: 5 s).",
          "Bodenstampfer, 1,2 s angekündigt durch beide erhobenen Arme, ein Brüllen und einen Ring aus Seelenpartikeln (Radius 6): Wer im Ring am Boden steht, nimmt 10 Schaden und wird weggeschleudert. Springen weicht aus. Abklingzeit 8 s.",
          "Schwäche: Licht (Block und Himmel) ab 12 an seinem Standort halbiert den Schaden aller Angriffe und begrenzt die Aura auf Zittern I; seine Seelenflammen werden schwach und rauchen.",
          "Lässt 2–4 Schreckens-Kettenglieder fallen, wenn ein Spieler den letzten Treffer setzt (Plünderung +1)."
        ]
      }
    },
    {
      "id": "hollow_watcher",
      "sources": [
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/HollowWatcherEntity.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/FearLogic.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/FearServer.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/FearAura.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/MobsConfig.java"
      ],
      "en": {
        "title": "Hollow Watcher",
        "summary": "A 4 block shadow with many eyes on the outer End islands. It moves only while nobody looks at it; a spyglass look freezes it. Unwatched it teleports behind you and grips you.",
        "details": [
          "Spawning: rarely, only in the End farther than 1000 blocks from the origin, on End stone with room for its height, at most one within 128 blocks, not on Peaceful. Server config: hollowWatcherSpawns, hollowWatcherChance (per 30 s roll per player, default 0.04).",
          "200 health, armor 8. 50 XP. While any player looks at it (in view, line of sight, not blinded) it stands still and starts no attack; a grip or scream already running goes on.",
          "Looking at it through a spyglass freezes it for 3 seconds: no movement, no attack, a running grip lets go.",
          "Teleport: unwatched and more than 8 blocks away, portal particles mark a spot 2.5 blocks behind its target for 0.6 s, then it appears there. If someone looks at it meanwhile, the jump is cancelled. Cooldown 6 s.",
          "Grip, announced by reaching arms for 0.5 s: 6 damage and held in place for 2 s, then 4 damage and thrown away. Cooldown 10 s. Claw: 10 damage after 0.3 s.",
          "Scream, announced for 1 s (head thrown back, low Enderman scream): Shivering II for 10 s and Blindness for 3 s to everyone within 12 blocks. It screams when its target stares at it for 5 s in a row, or unwatched every 20 s when its target is within 12 blocks in sight.",
          "Hit and run: 1 s after it takes a hit it blinks 12-20 blocks away (portal particles and a low stare sound announce it; even a freeze does not stop it).",
          "Drops 1 Watcher Eye when a player lands the last hit (Looting +1)."
        ]
      },
      "de": {
        "title": "Hohler Wächter",
        "summary": "Ein 4 Blöcke großer Schatten mit vielen Augen auf den äußeren End-Inseln. Er bewegt sich nur, solange ihn niemand ansieht; ein Blick durchs Fernrohr friert ihn ein. Unbeobachtet springt er hinter dich und packt zu.",
        "details": [
          "Erscheinen: selten, nur im End weiter als 1000 Blöcke vom Ursprung, auf Endstein mit Platz für seine Höhe, höchstens einer im Umkreis von 128 Blöcken, nicht auf Friedlich. Server-Config: hollowWatcherSpawns, hollowWatcherChance (je 30-s-Wurf und Spieler, Standard 0,04).",
          "200 Leben, Rüstung 8. 50 EP. Solange ein Spieler ihn ansieht (im Blickfeld, Sichtlinie, nicht blind), steht er still und beginnt keinen Angriff; ein laufender Griff oder Schrei geht weiter.",
          "Ein Blick durchs Fernrohr friert ihn 3 Sekunden ein: keine Bewegung, kein Angriff, ein laufender Griff löst sich.",
          "Teleport: unbeobachtet und weiter als 8 Blöcke entfernt markieren Portalpartikel 0,6 s lang eine Stelle 2,5 Blöcke hinter seinem Ziel, dann erscheint er dort. Sieht ihn währenddessen jemand an, bricht der Sprung ab. Abklingzeit 6 s.",
          "Griff, 0,5 s angekündigt durch ausgestreckte Arme: 6 Schaden und 2 s festgehalten, dann 4 Schaden und weggeschleudert. Abklingzeit 10 s. Kralle: 10 Schaden nach 0,3 s.",
          "Schrei, 1 s angekündigt (Kopf zurück, tiefer Enderman-Schrei): Zittern II für 10 s und Blindheit für 3 s für alle im Umkreis von 12 Blöcken. Er schreit, wenn sein Ziel ihn 5 s am Stück anstarrt, oder unbeobachtet alle 20 s, wenn sein Ziel in Sichtweite innerhalb von 12 Blöcken ist.",
          "Zuschlagen und weg: 1 s nach einem Treffer springt er 12-20 Blöcke weit weg (Portalpartikel und ein tiefer Blick-Laut kündigen es an; auch Einfrieren hält ihn nicht auf).",
          "Lässt 1 Wächterauge fallen, wenn ein Spieler den letzten Treffer setzt (Plünderung +1)."
        ]
      }
    },
    {
      "id": "shivering_aura",
      "sources": [
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/FearAura.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/FearMobs.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/FearLogic.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/FearServer.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/client/AimSway.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/mixin/AimSwayMixin.java"
      ],
      "en": {
        "title": "Shivering aura, Calm Amulet and Focus",
        "summary": "Near a fear mob you shiver: your aim wavers. Shivering I when it sees you within 16 blocks, II closer than 6. The Calm Amulet or the Potion of Focus makes you immune.",
        "details": [
          "Every second the Dread and the Hollow Watcher give Shivering for 3 s to survival players around them: level I within 16 blocks if they can see you, level II closer than 6 blocks. Milk helps only until the next pulse.",
          "Shivering makes the view waver around your aim (about 0.6 degrees at I, 1.8 at II), so bows and crossbows lose precision. With SimpleBuilding it is SimpleBuilding's Shivering effect (which also shakes the crosshair); without it Simple Mobs brings its own.",
          "Calm Amulet: iron chain on top, a Dread Link left, right and below, an amethyst shard in the middle. Anywhere in the inventory it makes you immune to Shivering and removes it.",
          "Potion of Focus: Awkward Potion + Watcher Eye = 3:00 immunity to Shivering; + Redstone 8:00; Gunpowder and Dragon's Breath make splash and lingering potions."
        ]
      },
      "de": {
        "title": "Zitter-Aura, Ruhe-Amulett und Fokus",
        "summary": "In der Nähe eines Furcht-Mobs zitterst du: dein Zielen wackelt. Zittern I, wenn er dich im Umkreis von 16 Blöcken sieht, II näher als 6. Das Ruhe-Amulett oder der Trank des Fokus machen immun.",
        "details": [
          "Jede Sekunde geben Schreckensritter und Hohler Wächter Spielern im Überlebensmodus um sich herum 3 s Zittern: Stufe I im Umkreis von 16 Blöcken, wenn sie dich sehen, Stufe II näher als 6 Blöcke. Milch hilft nur bis zum nächsten Puls.",
          "Zittern lässt den Blick um dein Ziel pendeln (etwa 0,6 Grad bei I, 1,8 bei II), Bogen und Armbrust werden ungenau. Mit SimpleBuilding ist es dessen Effekt Zittern (der zusätzlich das Fadenkreuz wackeln lässt); ohne bringt Simple Mobs einen eigenen mit.",
          "Ruhe-Amulett: Eisenkette oben, je ein Schreckens-Kettenglied links, rechts und unten, Amethystscherbe in der Mitte. Irgendwo im Inventar macht es immun gegen Zittern und entfernt es.",
          "Trank des Fokus: Seltsamer Trank + Wächterauge = 3:00 Immunität gegen Zittern; + Redstone 8:00; Schwarzpulver und Drachenatem ergeben Wurf- und Verweiltrank."
        ]
      }
    },
    {
      "id": "blighted_wither",
      "sources": [
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/BlightedWither.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/WitherLogic.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/BlightedPattern.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/WitherMobs.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/mixin/WitherSkullBlightedMixin.java"
      ],
      "en": {
        "title": "Blighted Wither and Wither Heart",
        "summary": "The Wither, only stronger: build the Wither figure on a Block of Netherite and the Blighted Wither rises - 600 health, armor 8, three phases, and every special attack is announced before it hits. It drops a Wither Heart.",
        "details": [
          "Summoning: the Vanilla T of soul sand or soul soil with three wither skeleton skulls, plus a Block of Netherite under the foot of the T. The last skull summons the Blighted Wither and uses up the Netherite block; without it the Vanilla Wither comes. Not on Peaceful; server config witherSummonEnabled.",
          "Health 600 (Vanilla 300), armor 8 (Vanilla 4), 100 experience. It wakes like the Wither (11 s, then the explosion) and heals to full while waking.",
          "Phase 1 (above half health): the Vanilla skulls, plus a twin volley every 5 s - both side heads burn with soul fire for 1 s, then fire at the target at once.",
          "Phase 2 (half health and below): the Vanilla shield (arrows and wind charges bounce off), and every 20 s three Wither Skeletons: soul flames mark their spots for 2 s before they rise. Never more than six near it; they drop nothing.",
          "Phase 3 \"Blight\" (below 15 %): a nova every 12 s - a ring of soul smoke 8 blocks wide warns for 2 s, whoever is still inside gets Wither II for 6 s and is pushed away. Every 30 s the wilt (ash falls for 2 s first): short grass, ferns and flowers within 12 blocks turn into dead bushes, natural leaves fall, one in five grass blocks becomes coarse dirt. Placed leaves, crops, saplings and all other blocks stay. Needs the mobGriefing game rule; server config witherWilt.",
          "Drops exactly one Wither Heart besides the Nether Star (Looting does not change it). Only the Blighted Wither drops it. The Wither Heart is the core of the Biome Brush."
        ]
      },
      "de": {
        "title": "Verdorrter Wither und Witherherz",
        "summary": "Der Wither, nur stärker: Baue die Wither-Figur auf einen Netheritblock, dann erscheint der Verdorrte Wither - 600 Leben, Rüstung 8, drei Phasen, und jeder Spezialangriff wird angekündigt, bevor er trifft. Er lässt ein Witherherz fallen.",
        "details": [
          "Beschwörung: das Vanilla-T aus Seelensand oder Seelenerde mit drei Witherskelettschädeln, dazu ein Netheritblock unter dem Fuß des T. Der letzte Schädel ruft den Verdorrten Wither und verbraucht den Netheritblock; ohne ihn kommt der Vanilla-Wither. Nicht auf Friedlich; Server-Config witherSummonEnabled.",
          "Leben 600 (Vanilla 300), Rüstung 8 (Vanilla 4), 100 Erfahrung. Er erwacht wie der Wither (11 s, dann die Explosion) und heilt sich dabei voll.",
          "Phase 1 (über halbem Leben): die Vanilla-Schädel, dazu alle 5 s eine Doppelsalve - beide Seitenköpfe brennen 1 s mit Seelenfeuer, dann schießen sie gleichzeitig auf das Ziel.",
          "Phase 2 (ab halbem Leben): der Vanilla-Schild (Pfeile und Windkugeln prallen ab), und alle 20 s drei Witherskelette: Seelenflammen markieren 2 s vorher ihre Plätze. Nie mehr als sechs in seiner Nähe; sie lassen nichts fallen.",
          "Phase 3 „Welken“ (unter 15 %): alle 12 s eine Nova - ein Ring aus Seelenrauch mit 8 Blöcken Radius warnt 2 s, wer dann noch drinsteht, bekommt 6 s Ausdörrung II und wird weggestoßen. Alle 30 s das Welken (vorher fällt 2 s Asche): Gras, Farne und Blumen im Umkreis von 12 Blöcken werden zu toten Büschen, natürliches Laub fällt ab, jeder fünfte Grasblock wird zu grober Erde. Gesetztes Laub, Feldfrüchte, Setzlinge und alle anderen Blöcke bleiben. Braucht die Spielregel mobGriefing; Server-Config witherWilt.",
          "Lässt neben dem Netherstern genau ein Witherherz fallen (Plünderung ändert das nicht). Nur der Verdorrte Wither lässt es fallen. Das Witherherz ist der Kern des Biom-Pinsels."
        ]
      }
    },
    {
      "id": "biome_brush",
      "sources": [
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/BiomeBrushItem.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/BiomeBrush.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/WitherMobs.java",
        "modules/simplemobs/shared/resources/data/simplemobs/recipe/biome_brush.json"
      ],
      "en": {
        "title": "Biome Brush",
        "summary": "Brush in the main hand, a biome material in the off hand, right-click a block: the biome around it becomes the material's biome. One material and one durability point per stroke.",
        "details": [
          "Recipe (shaped, one column): Wither Heart, Netherite Ingot, Brush. Durability 512, repaired with Netherite Ingots, takes Unbreaking and Mending.",
          "A stroke sets the 4x4x4 biome cells (Vanilla stores biomes in such cells) around the clicked block: 3x3x3 cells, 12 blocks wide (server config biomeBrushRadius, 0 to 2 cells). The chunks are sent again at once, so grass, leaf and water colors, weather, mob spawns and plant growth follow the new biome. Holding the button keeps painting, one stroke per 5 ticks. Creative uses no material and no durability.",
          "Materials: Grass Block = Plains, Sand = Desert, Snow Block = Snowy Plains, Mycelium = Mushroom Fields, Cherry Leaves = Cherry Grove, Jungle Log = Jungle, Red Sand = Badlands, Podzol = Taiga, Mangrove Roots = Mangrove Swamp, Pale Moss Block = Pale Garden, Oak Log = Forest, Birch Log = Birch Forest, Dark Oak Log = Dark Forest, Acacia Log = Savanna, Lily Pad = Swamp, Packed Ice = Ice Spikes; Nether: Soul Sand or Soul Soil = Soul Sand Valley, Crimson Nylium = Crimson Forest, Warped Nylium = Warped Forest, Basalt = Basalt Deltas, Netherrack = Nether Wastes; End: Chorus Flower = End Highlands, End Stone = End Barrens. Data packs add more in data/<namespace>/biome_brush/*.json.",
          "Limits: biomes only in their own dimension (Overworld biomes in the Overworld ...). Cave biomes (Deep Dark, Dripstone and Lush Caves) and structures are never painted over.",
          "Multiplayer-safe: every cell is checked like a block edit - claims (Simple Tweaks or any mod using the shared protection check), spawn protection and adventure mode. Refused cells keep their biome; a refused clicked cell refuses the stroke and uses nothing (a puff of smoke). Server config: biomeBrushEnabled, biomeBrushOpsOnly."
        ]
      },
      "de": {
        "title": "Biom-Pinsel",
        "summary": "Pinsel in der Haupthand, ein Biom-Material in der Nebenhand, Rechtsklick auf einen Block: Das Biom ringsum wird zum Biom des Materials. Ein Material und ein Haltbarkeitspunkt je Strich.",
        "details": [
          "Rezept (geformt, eine Spalte): Witherherz, Netheritbarren, Pinsel. Haltbarkeit 512, Reparatur mit Netheritbarren, nimmt Haltbarkeit und Reparatur an.",
          "Ein Strich setzt die 4×4×4-Biomzellen (so speichert Vanilla Biome) um den geklickten Block: 3×3×3 Zellen, 12 Blöcke breit (Server-Config biomeBrushRadius, 0 bis 2 Zellen). Die Chunks werden sofort neu gesendet, Gras-, Laub- und Wasserfarbe, Wetter, Mob-Spawns und Pflanzenwachstum folgen dem neuen Biom. Gedrückt halten malt weiter, ein Strich je 5 Ticks. Kreativ verbraucht weder Material noch Haltbarkeit.",
          "Materialien: Grasblock = Ebene, Sand = Wüste, Schneeblock = Verschneite Ebene, Myzel = Pilzinsel, Kirschlaub = Kirschhain, Tropenbaumstamm = Dschungel, Roter Sand = Tafelberge, Podsol = Taiga, Mangrovenwurzeln = Mangrovensumpf, Blassmoosblock = Blasser Garten, Eichenstamm = Wald, Birkenstamm = Birkenwald, Schwarzeichenstamm = Dichter Wald, Akazienstamm = Savanne, Seerosenblatt = Sumpf, Packeis = Eiszapfentundra; Nether: Seelensand oder Seelenerde = Seelensandtal, Karmesin-Nyzel = Karmesinwald, Wirr-Nyzel = Wirrwald, Basalt = Basaltdeltas, Netherrack = Netherödland; End: Chorusblüte = End-Hochland, Endstein = Kargland des Ends. Datenpakete ergänzen weitere unter data/<namespace>/biome_brush/*.json.",
          "Grenzen: Biome nur in ihrer eigenen Dimension (Oberwelt-Biome in der Oberwelt …). Höhlenbiome (Tiefes Dunkel, Tropfsteinhöhlen, Üppige Höhlen) und Bauwerke werden nie übermalt.",
          "Mehrspielersicher: Jede Zelle wird wie ein Blockeingriff geprüft - Claims (Simple Tweaks oder jede Mod mit der gemeinsamen Schutzprüfung), Spawnschutz und Abenteuermodus. Verweigerte Zellen behalten ihr Biom; ist die geklickte Zelle verweigert, wird nichts gemalt und nichts verbraucht (ein Rauchwölkchen). Server-Config: biomeBrushEnabled, biomeBrushOpsOnly."
        ]
      }
    },
    {
      "id": "first_pickup_tips",
      "sources": [
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/discover/MobsDiscover.java",
        "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/mixin/DiscoverTipMixin.java",
        "framework/src/main/java/com/simplebuilding/framework/api/FirstUseTips.java",
        "tools/discover/discover.py"
      ],
      "en": {
        "title": "First pickup tips",
        "summary": "The first time you pick up a machine or tool whose use is not obvious, one short tip shows in the action bar - once per player and world.",
        "details": [
          "Tips exist for the Trick Mirror, the Deceiver Cloak and the Shellker Shell.",
          "A tip counts as seen through an entity tag on the player (simplemobs.tip.<group>); removing the tag with /tag shows it again.",
          "Switch: config/simple-tips.properties, key simplemobs.firstPickupTips=false turns the tips of this mod off. The file is read at every tip, so no restart is needed; every Simple mod with tips has its own key there.",
          "Every craftable item of the mod is in the recipe book: picking up one of its ingredients unlocks the recipe."
        ]
      },
      "de": {
        "title": "Tipps beim ersten Aufheben",
        "summary": "Hebst du zum ersten Mal eine Maschine oder ein Werkzeug auf, dessen Bedienung nicht offensichtlich ist, zeigt die Aktionsleiste einen kurzen Tipp - einmal je Spieler und Welt.",
        "details": [
          "Tipps gibt es für den Trickspiegel, den Täuschermantel und die Shellker-Schale.",
          "Ein Tipp gilt über ein Entity-Tag am Spieler (simplemobs.tip.<gruppe>) als gesehen; entfernt man das Tag mit /tag, erscheint er wieder.",
          "Schalter: config/simple-tips.properties, Schlüssel simplemobs.firstPickupTips=false schaltet die Tipps dieser Mod ab. Die Datei wird bei jedem Tipp gelesen, ein Neustart ist nicht nötig; jede Simple-Mod mit Tipps hat dort ihren eigenen Schlüssel.",
          "Jedes herstellbare Item der Mod steht im Rezeptbuch: Das Aufheben einer seiner Zutaten schaltet das Rezept frei."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simplemobs:biome_brush",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": null,
      "result": {
        "id": "simplemobs:biome_brush",
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/biome_brush.json",
      "ingredients": [
        "minecraft:brush",
        "minecraft:netherite_ingot",
        "simplemobs:wither_heart"
      ],
      "pattern": [
        " H ",
        " N ",
        " B "
      ],
      "key": {
        "H": [
          "simplemobs:wither_heart"
        ],
        "N": [
          "minecraft:netherite_ingot"
        ],
        "B": [
          "minecraft:brush"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:copper_ingot",
            "count": 1
          },
          {
            "id": "minecraft:feather",
            "count": 1
          },
          {
            "id": "minecraft:netherite_ingot",
            "count": 1
          },
          {
            "id": "simplemobs:wither_heart",
            "count": 1
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.125
          }
        ]
      }
    },
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
      "id": "simplemobs:brewing/lingering_potion_awkward_watcher_eye",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/lingering_potion_awkward_watcher_eye.json",
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
      "id": "simplemobs:brewing/lingering_potion_focus_redstone",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/lingering_potion_focus_redstone.json",
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
      "id": "simplemobs:brewing/potion_awkward_watcher_eye",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_awkward_watcher_eye.json",
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
      "id": "simplemobs:brewing/potion_focus_gunpowder",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_focus_gunpowder.json",
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
      "id": "simplemobs:brewing/potion_focus_redstone",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_focus_redstone.json",
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
      "id": "simplemobs:brewing/potion_long_focus_gunpowder",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/potion_long_focus_gunpowder.json",
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
      "id": "simplemobs:brewing/splash_potion_awkward_watcher_eye",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_awkward_watcher_eye.json",
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
      "id": "simplemobs:brewing/splash_potion_focus_dragon_breath",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_focus_dragon_breath.json",
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
      "id": "simplemobs:brewing/splash_potion_focus_redstone",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_focus_redstone.json",
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
      "id": "simplemobs:brewing/splash_potion_long_focus_dragon_breath",
      "type": "minecraft:brewing",
      "category": null,
      "group": null,
      "result": {
        "id": null,
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/brewing/splash_potion_long_focus_dragon_breath.json",
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
      "id": "simplemobs:calm_amulet",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": null,
      "result": {
        "id": "simplemobs:calm_amulet",
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/calm_amulet.json",
      "ingredients": [
        "minecraft:amethyst_shard",
        "minecraft:iron_chain",
        "simplemobs:dread_link"
      ],
      "pattern": [
        " C ",
        "LAL",
        " L "
      ],
      "key": {
        "C": [
          "minecraft:iron_chain"
        ],
        "L": [
          "simplemobs:dread_link"
        ],
        "A": [
          "minecraft:amethyst_shard"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "simplemobs:dread_link",
            "count": 3
          },
          {
            "id": "minecraft:iron_ingot",
            "count": 1.222
          },
          {
            "id": "minecraft:amethyst_shard",
            "count": 1
          }
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
      "id": "simplemobs:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplemobs:guide_book",
        "count": 1
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/guide_book.json",
      "ingredients": [
        "minecraft:bone",
        "minecraft:book"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:bone"
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
            "id": "minecraft:bone",
            "count": 1
          },
          {
            "id": "minecraft:leather",
            "count": 1
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
      "id": "simplemobs:soul_fire_charge",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplemobs:soul_fire_charge",
        "count": 3
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/soul_fire_charge.json",
      "ingredients": [
        "#minecraft:coals",
        "minecraft:gunpowder",
        "simplemobs:soulfire_powder"
      ],
      "ingredientGroups": [
        [
          "minecraft:gunpowder"
        ],
        [
          "simplemobs:soulfire_powder"
        ],
        [
          "#minecraft:coals"
        ]
      ],
      "baseMaterials": {
        "yield": 3,
        "materials": [
          {
            "id": "minecraft:coal",
            "count": 1
          },
          {
            "id": "minecraft:gunpowder",
            "count": 1
          },
          {
            "id": "simplemobs:soulfire_rod",
            "count": 0.5
          }
        ],
        "tagExamples": [
          "#minecraft:coals"
        ]
      }
    },
    {
      "id": "simplemobs:soulfire_powder",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplemobs:soulfire_powder",
        "count": 2
      },
      "source": "modules/simplemobs/shared/resources/data/simplemobs/recipe/soulfire_powder.json",
      "ingredients": [
        "simplemobs:soulfire_rod"
      ],
      "ingredientGroups": [
        [
          "simplemobs:soulfire_rod"
        ]
      ],
      "baseMaterials": {
        "yield": 2,
        "materials": [
          {
            "id": "simplemobs:soulfire_rod",
            "count": 1
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
      "id": "simplemobs:entities/dread",
      "kind": "entities",
      "type": "minecraft:entity",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobs:dread_link"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/loot_table/entities/dread.json"
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
      "id": "simplemobs:entities/hollow_watcher",
      "kind": "entities",
      "type": "minecraft:entity",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobs:watcher_eye"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/loot_table/entities/hollow_watcher.json"
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
    },
    {
      "id": "simplemobs:worldgen/biome/biome_brush_locked",
      "replace": false,
      "values": [
        {
          "id": "minecraft:deep_dark",
          "required": true
        },
        {
          "id": "minecraft:dripstone_caves",
          "required": true
        },
        {
          "id": "minecraft:lush_caves",
          "required": true
        },
        {
          "id": "minecraft:sulfur_caves",
          "required": false
        }
      ],
      "source": "modules/simplemobs/shared/resources/data/simplemobs/tags/worldgen/biome/biome_brush_locked.json"
    }
  ],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "simplemobs:biome_brush",
      "name": {
        "en_us": "Biome Brush",
        "de_de": "Biom-Pinsel"
      },
      "note": {
        "en": {
          "summary": "Main hand: brush, off hand: biome material. Right-click paints the biome around the block (3x3x3 cells), one material and one durability point per stroke."
        },
        "de": {
          "summary": "Haupthand: Pinsel, Nebenhand: Biom-Material. Rechtsklick malt das Biom um den Block (3×3×3 Zellen), ein Material und ein Haltbarkeitspunkt je Strich."
        }
      },
      "texture": "assets/textures/simplemobs/item/biome_brush.png",
      "questionary": {
        "en": {
          "lore": [
            "Paint the land itself. The heart in the ferrule remembers every place it has seen."
          ],
          "use": [
            "Hold it in the main hand and a biome material in the off hand, then right-click a block: sand paints desert, snow paints snowy plains, soul sand paints a soul sand valley. One material and one durability point per stroke."
          ],
          "secret": [
            "Biomes stay in their dimension, and claimed land or caves cannot be painted. Unbreaking and Mending work on it."
          ]
        },
        "de": {
          "lore": [
            "Male das Land selbst an. Das Herz in der Zwinge erinnert sich an jeden Ort, den es gesehen hat."
          ],
          "use": [
            "Halte ihn in der Haupthand und ein Biom-Material in der Nebenhand, dann Rechtsklick auf einen Block: Sand malt Wüste, Schnee verschneite Ebene, Seelensand ein Seelensandtal. Ein Material und ein Haltbarkeitspunkt je Strich."
          ],
          "secret": [
            "Biome bleiben in ihrer Dimension, geschütztes Land und Höhlen lassen sich nicht übermalen. Haltbarkeit und Reparatur wirken auf ihn."
          ]
        }
      },
      "craftedBy": [
        "simplemobs:biome_brush"
      ],
      "usedIn": []
    },
    {
      "id": "simplemobs:calm_amulet",
      "name": {
        "en_us": "Calm Amulet",
        "de_de": "Ruhe-Amulett"
      },
      "note": {
        "en": {
          "summary": "Anywhere in the inventory: immune to Shivering. Crafted from three Dread Links, an iron chain and an amethyst shard."
        },
        "de": {
          "summary": "Irgendwo im Inventar: immun gegen Zittern. Aus drei Schreckens-Kettengliedern, Eisenkette und Amethystscherbe."
        }
      },
      "texture": "assets/textures/simplemobs/item/calm_amulet.png",
      "questionary": {
        "en": {
          "lore": [
            "A small weight of iron and amethyst that keeps the hands steady."
          ],
          "use": [
            "Carry it anywhere in your inventory: you are immune to Shivering, from the Dread's and the Hollow Watcher's aura, the Watcher's scream and potions alike. Craft: iron chain on top, Dread Links left, right and below, an amethyst shard in the middle."
          ],
          "secret": [
            "It also removes Shivering you already have the moment it enters your inventory."
          ]
        },
        "de": {
          "lore": [
            "Ein kleines Gewicht aus Eisen und Amethyst, das die Hände ruhig hält."
          ],
          "use": [
            "Irgendwo im Inventar getragen macht es immun gegen Zittern, ob von der Aura des Schreckensritters oder des Hohlen Wächters, vom Schrei des Wächters oder aus Tränken. Rezept: Eisenkette oben, Schreckens-Kettenglieder links, rechts und unten, Amethystscherbe in der Mitte."
          ],
          "secret": [
            "Vorhandenes Zittern verschwindet, sobald es ins Inventar kommt."
          ]
        }
      },
      "craftedBy": [
        "simplemobs:calm_amulet"
      ],
      "usedIn": []
    },
    {
      "id": "simplemobs:deceiver_cloak",
      "name": {
        "en_us": "Cloak of Deception",
        "de_de": "Tarnumhang"
      },
      "note": {
        "en": {
          "summary": "Worn in the head slot (hood, cloak on the back over any Elytra): mobs notice you only within 3 blocks; empty-handed right-click a mob or player to take its shape. Any swing (also into the air) or breaking a block ends it."
        },
        "de": {
          "summary": "Im Kopfplatz (Kapuze, Umhang auf dem Rücken über Elytren): Mobs bemerken dich erst im 3-Block-Radius; mit leerer Hand Rechtsklick auf Mob oder Spieler = Verwandlung. Jeder Schlag (auch in die Luft) oder Abbauen beendet sie."
        }
      },
      "texture": "assets/textures/simplemobs/item/deceiver_cloak.png",
      "questionary": {
        "en": {
          "lore": [
            "A cloak that remembers every shape it ever saw."
          ],
          "use": [
            "Craft deceiver cloth and phantom membrane in an elytra-like pattern. Worn in the head slot: a hood, with the cloak on your back over any Elytra. 240 durability. Right-click a mob or player with an empty hand to take its shape."
          ],
          "secret": [
            "Any swing, even into the air, or breaking a block ends the disguise. Bosses, the Warden and the Howler see through it."
          ]
        },
        "de": {
          "lore": [
            "Ein Umhang, der sich jede Gestalt merkt, die er je sah."
          ],
          "use": [
            "Betrügertuch und Phantomhaut in einem Elytra-ähnlichen Muster herstellen. Im Kopfplatz getragen: Kapuze, der Umhang liegt auf dem Rücken über Elytren. 240 Haltbarkeit. Rechtsklick mit leerer Hand auf Mob oder Spieler nimmt dessen Gestalt an."
          ],
          "secret": [
            "Jeder Schlag, auch in die Luft, oder Blockabbau beendet die Verkleidung. Bosse, der Warden und der Heuler durchschauen sie."
          ]
        }
      },
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
      "questionary": {
        "en": {
          "lore": [
            "A cloth woven from a trick that never quite stays the same colour."
          ],
          "use": [
            "Dropped by the Deceiver, 2 to 4 at 50%. It is the base of the Cloak of Deception and the Trick Mirror, repairs the cloak, and with an Invisibility Potion brews a Veiled Invisibility."
          ],
          "secret": [
            "That invisibility has no swirl particles. It is the quietest vanishing in the game."
          ]
        },
        "de": {
          "lore": [
            "Ein Tuch aus einem Trick, der nie ganz dieselbe Farbe behält."
          ],
          "use": [
            "Vom Betrüger fallen gelassen, 2 bis 4 mit 50 %. Grundstoff des Täuschungsumhangs und des Trickspiegels, repariert den Umhang und braut mit einem Unsichtbarkeitstrank verschleierte Unsichtbarkeit."
          ],
          "secret": [
            "Diese Unsichtbarkeit hat keine Wirbelpartikel. Es ist das leiseste Verschwinden im Spiel."
          ]
        }
      },
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
      "questionary": {
        "en": {
          "lore": [
            "An egg whose shell looks like different things from different sides."
          ],
          "use": [
            "Spawns a Deceiver, a mob that turns into one of its own wave's mobs and may double its summons below half health. 200 health, 280 with armor."
          ],
          "secret": [
            "Its copies are always fakes and vanish with it."
          ]
        },
        "de": {
          "lore": [
            "Ein Ei, dessen Schale von jeder Seite anders aussieht."
          ],
          "use": [
            "Beschwört einen Betrüger, der sich in eines der Mobs seiner Welle verwandelt und unterhalb der halben Gesundheit seine Beschwörungen verdoppeln kann. 200 Leben, 280 mit Rüstung."
          ],
          "secret": [
            "Seine Kopien sind immer Fälschungen und verschwinden mit ihm."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:dread_link",
      "name": {
        "en_us": "Dread Link",
        "de_de": "Schreckens-Kettenglied"
      },
      "note": {
        "en": {
          "summary": "Dropped by the Dread (2-4, Looting +1). Three make the Calm Amulet with an iron chain and an amethyst shard."
        },
        "de": {
          "summary": "Lässt der Schreckensritter fallen (2–4, Plünderung +1). Drei ergeben mit Eisenkette und Amethystscherbe das Ruhe-Amulett."
        }
      },
      "texture": "assets/textures/simplemobs/item/dread_link.png",
      "questionary": {
        "en": {
          "lore": [
            "A link of a chain that dragged a mace through a hundred battles. It is cold and never stops trembling."
          ],
          "use": [
            "Dropped by the Dread, 2 to 4 (more with Looting), only when a player lands the last hit. Three links, an iron chain and an amethyst shard make the Calm Amulet."
          ],
          "secret": [
            "The Dread never spawns on its own: whoever wants links has to build one first."
          ]
        },
        "de": {
          "lore": [
            "Ein Glied der Kette, an der eine Keule durch hundert Schlachten schleifte. Es ist kalt und hört nie auf zu zittern."
          ],
          "use": [
            "Lässt der Schreckensritter fallen, 2 bis 4 (mehr mit Plünderung), nur wenn ein Spieler den letzten Treffer setzt. Drei Glieder, eine Eisenkette und eine Amethystscherbe ergeben das Ruhe-Amulett."
          ],
          "secret": [
            "Der Schreckensritter erscheint nie von selbst: Wer Glieder will, muss ihn erst bauen."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": [
        "simplemobs:calm_amulet"
      ]
    },
    {
      "id": "simplemobs:dread_spawn_egg",
      "name": {
        "en_us": "Dread Spawn Egg",
        "de_de": "Schreckensritter-Spawn-Ei"
      },
      "note": {
        "en": {
          "summary": "Creative only. In survival the Dread is built: an Iron Golem T with an anvil in the middle, a skeleton skull on top, then an Iron Helmet put on the skull."
        },
        "de": {
          "summary": "Nur kreativ. Im Überleben wird der Schreckensritter gebaut: Eisengolem-T mit Amboss in der Mitte, Skelettschädel obendrauf, dann einen Eisenhelm auf den Schädel setzen."
        }
      },
      "texture": "assets/textures/simplemobs/item/dread_spawn_egg.png",
      "questionary": {
        "en": {
          "lore": [
            "An egg as heavy as an anvil."
          ],
          "use": [
            "Spawns a Dread in creative. In survival you build it like an Iron Golem, with an anvil in the middle of the crossbar and a skeleton skull as the head - then put an Iron Helmet on the skull with a right-click."
          ],
          "secret": [
            "Bright light weakens it: at light 12 and more its blows deal half damage and its aura only gives Shivering I."
          ]
        },
        "de": {
          "lore": [
            "Ein Ei, schwer wie ein Amboss."
          ],
          "use": [
            "Erzeugt im Kreativmodus einen Schreckensritter. Im Überleben baust du ihn wie einen Eisengolem, mit einem Amboss in der Mitte der Querreihe und einem Skelettschädel als Kopf – dann setzt du dem Schädel per Rechtsklick einen Eisenhelm auf."
          ],
          "secret": [
            "Helles Licht schwächt ihn: ab Licht 12 machen seine Schläge nur halben Schaden und seine Aura gibt nur Zittern I."
          ]
        }
      },
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
      "questionary": {
        "en": {
          "lore": [
            "The heart of a sound that would not stop. It still echoes if you hold it to your ear."
          ],
          "use": [
            "Dropped by the Hollow Howler. Awkward Potion plus Echo Core brews a Potion of Silence, 3:00 (8:00 with redstone), splash and lingering as usual."
          ],
          "secret": [
            "Silence hides you like wool: sculk sensors, Wardens and Howlers do not hear you."
          ]
        },
        "de": {
          "lore": [
            "Das Herz eines Klangs, der nicht aufhören wollte. Am Ohr hört man ihn noch."
          ],
          "use": [
            "Wird vom Tiefenheuler fallen gelassen. Seltsamer Trank plus Echokern braut einen Trank der Stille, 3:00 (8:00 mit Redstone), als Wurf- und Verweiltrank wie üblich."
          ],
          "secret": [
            "Stille verbirgt dich wie Wolle: Sculk-Sensoren, Wardens und Heuler hören dich nicht."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:guide_book",
      "name": {
        "en_us": "Simple Mobs Guide",
        "de_de": "Simple-Mobs-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simplemobs/shared/java/com/simplebuilding/modules/simplemobs/guide/MobsGuide.java",
          "modules/simplemobs/shared/resources/data/simplemobs/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 16 pages taken from this wiki, shown in your language. Shapeless recipe: book + bone. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Mobs\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 16 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Knochen. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Mobs\" eins."
        }
      },
      "craftedBy": [
        "simplemobs:guide_book"
      ],
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
      "questionary": {
        "en": {
          "lore": [
            "An egg that hums at a pitch only the Deep Dark understands."
          ],
          "use": [
            "Spawns a Hollow Howler, the Deep Dark mid-boss, in creative. In survival you summon it yourself with a figure of sculk, a skull and a goat horn."
          ],
          "secret": [
            "The Howler never calls reinforcements, but it hunts everything it hears."
          ]
        },
        "de": {
          "lore": [
            "Ein Ei, das in einer Tonhöhe summt, die nur das Tiefe Dunkel versteht."
          ],
          "use": [
            "Beschwört im Kreativmodus einen Tiefenheuler, den Zwischenboss des Tiefen Dunkels. Im Überleben ruft man ihn selbst mit einer Figur aus Sculk, einem Schädel und einem Ziegenhorn."
          ],
          "secret": [
            "Der Heuler ruft nie Verstärkung, jagt aber alles, was er hört."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:hollow_watcher_spawn_egg",
      "name": {
        "en_us": "Hollow Watcher Spawn Egg",
        "de_de": "Hohler-Wächter-Spawn-Ei"
      },
      "note": {
        "en": {
          "summary": "Creative only. In survival the Hollow Watcher appears rarely on the outer End islands."
        },
        "de": {
          "summary": "Nur kreativ. Im Überleben erscheint der Hohle Wächter selten auf den äußeren End-Inseln."
        }
      },
      "texture": "assets/textures/simplemobs/item/hollow_watcher_spawn_egg.png",
      "questionary": {
        "en": {
          "lore": [
            "An egg that is never where you last saw it."
          ],
          "use": [
            "Spawns a Hollow Watcher in creative. In survival it appears rarely on the outer End islands, far from the dragon's island."
          ],
          "secret": [
            "It only moves while nobody looks at it. A look through a spyglass freezes it for 3 seconds - but stare too long and it screams."
          ]
        },
        "de": {
          "lore": [
            "Ein Ei, das nie dort ist, wo du es zuletzt gesehen hast."
          ],
          "use": [
            "Erzeugt im Kreativmodus einen Hohlen Wächter. Im Überleben erscheint er selten auf den äußeren End-Inseln, weit weg von der Dracheninsel."
          ],
          "secret": [
            "Er bewegt sich nur, solange ihn niemand ansieht. Ein Blick durchs Fernrohr friert ihn 3 Sekunden ein – aber wer ihn zu lange anstarrt, bringt ihn zum Schreien."
          ]
        }
      },
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
      "questionary": {
        "en": {
          "lore": [
            "A sliver of something that was never quite there."
          ],
          "use": [
            "Dropped by the Deceiver, 1 to 2 (more with Looting; an armored one drops twice as many). With an Awkward Potion it brews the Potion of the Mirage if SimpleBuilding is present."
          ],
          "secret": [
            "An armored Deceiver in a chest plate is the best source."
          ]
        },
        "de": {
          "lore": [
            "Ein Splitter von etwas, das nie ganz da war."
          ],
          "use": [
            "Vom Betrüger fallen gelassen, 1 bis 2 (mehr mit Plünderung; ein gepanzerter lässt doppelt so viele fallen). Mit einem Seltsamen Trank entsteht mit SimpleBuilding der Trank der Fata Morgana."
          ],
          "secret": [
            "Ein Betrüger mit Brustplatte ist die beste Quelle."
          ]
        }
      },
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
      "questionary": {
        "en": {
          "lore": [
            "A helmet made of the shells of those who guard the gateways."
          ],
          "use": [
            "Craft five Shellker Shells in helmet shape. Armor 3, toughness 1, 10% knockback resistance, iron durability, repaired with shells. It can be trimmed and enchanted."
          ],
          "secret": [
            "Its wearer is immune to High Gravity and takes 20% less fall damage. It does not stop the bullets' damage."
          ]
        },
        "de": {
          "lore": [
            "Ein Helm aus den Schalen derer, die die Tore bewachen."
          ],
          "use": [
            "Fünf Shellker-Schalen in Helmform herstellen. Rüstung 3, Zähigkeit 1, 10 % Rückstoßresistenz, Eisenhaltbarkeit, mit Schalen reparierbar. Kann verziert und verzaubert werden."
          ],
          "secret": [
            "Der Träger ist immun gegen Hohe Schwerkraft und erleidet 20 % weniger Fallschaden. Den Schaden der Geschosse hält er nicht ab."
          ]
        }
      },
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
          "summary": "Dropped by Shellkers (2-3); looks like a Shulker Shell of bedrock with the Shellker's violet inside. Right-click a Shulker with it to turn the Shulker into a Shellker. Brews the Potion of High Gravity; five make the Shellker Helmet."
        },
        "de": {
          "summary": "Lassen Shellker fallen (2-3); sieht aus wie eine Shulker-Schale aus Grundgestein mit dem Violett des Shellkers innen. Damit einen Shulker rechtsklicken, um ihn zum Shellker zu machen. Braut den Trank der Schwerkraft; fünf ergeben den Shellker-Helm."
        }
      },
      "texture": "assets/textures/simplemobs/item/shellker_shell.png",
      "questionary": {
        "en": {
          "lore": [
            "A shell that has stood against bullets and levitation alike. It is heavier than it looks."
          ],
          "use": [
            "Dropped by the Shellker, 2 to 3 pieces. Five make a Shellker Helmet; with an Awkward Potion it brews High Gravity. Right-click a Shulker with a shell to turn it into a Shellker."
          ],
          "secret": [
            "The shell is used up when it turns a Shulker. That is the only way to farm Shellkers, since they never spawn anywhere else."
          ]
        },
        "de": {
          "lore": [
            "Eine Schale, die Geschossen und Schwebe gleichermaßen widerstanden hat. Sie ist schwerer, als sie aussieht."
          ],
          "use": [
            "Vom Shellker fallen gelassen, 2 bis 3 Stück. Fünf ergeben einen Shellker-Helm; mit einem Seltsamen Trank braut sie Hohe Schwerkraft. Rechtsklick auf einen Shulker verwandelt ihn in einen Shellker."
          ],
          "secret": [
            "Die Schale wird beim Verwandeln verbraucht. Nur so lassen sich Shellker züchten, denn sie erscheinen sonst nirgends."
          ]
        }
      },
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
      "questionary": {
        "en": {
          "lore": [
            "An egg wrapped in shell that rattles quietly like a closing slit."
          ],
          "use": [
            "Spawns a Shellker, a 120-health Shulker that stays one block wide and guards End gateways. In survival they come only with the gateway or from a Shulker and a shell."
          ],
          "secret": [
            "Closed, a Shellker takes no damage at all; only pistons can push it away."
          ]
        },
        "de": {
          "lore": [
            "Ein Ei in Schalenpanzer, das leise klappert wie ein sich schließender Schlitz."
          ],
          "use": [
            "Beschwört einen Shellker, einen Shulker mit 120 Leben, der einen Block groß bleibt und End-Portale bewacht. Im Überleben gibt es ihn nur an den Toren oder aus Shulker und Schale."
          ],
          "secret": [
            "Geschlossen nimmt ein Shellker keinerlei Schaden; nur Kolben können ihn wegschieben."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:soul_fire_charge",
      "name": {
        "en_us": "Soulfire Charge",
        "de_de": "Seelenfeuerkugel"
      },
      "note": {
        "en": {
          "summary": "Gunpowder, Soulfire Powder and coal or charcoal make 3. Works like a fire charge (lights fire, dispensers shoot it) but flies as the Soulfire Blaze's soul fireball: burns and gives Soul Burn when Simple Building is loaded."
        },
        "de": {
          "summary": "Schießpulver, Seelenfeuer-Lohenstaub und Kohle oder Holzkohle ergeben 3. Wirkt wie eine Feuerkugel (entzündet Feuer, Dispenser verschießen sie), fliegt aber als Seelenfeuerball der Seelenfeuer-Lohe: brennt und gibt Seelenbrand, wenn Simple Building geladen ist."
        }
      },
      "texture": "assets/textures/simplemobs/item/soul_fire_charge.png",
      "questionary": {
        "en": {
          "lore": [
            "A fire charge with a blue heart."
          ],
          "use": [
            "Gunpowder, Soulfire Powder and coal or charcoal make 3. Use it on a block to light fire; a dispenser shoots it like the Soulfire Blaze does. A hit burns and, with Simple Building, adds Soul Burn."
          ]
        },
        "de": {
          "lore": [
            "Eine Feuerkugel mit blauem Herzen."
          ],
          "use": [
            "Schießpulver, Seelenfeuer-Lohenstaub und Kohle oder Holzkohle ergeben 3. Auf einen Block benutzt entzündet sie Feuer; ein Dispenser verschießt sie wie die Seelenfeuer-Lohe. Ein Treffer brennt und gibt mit Simple Building Seelenbrand."
          ]
        }
      },
      "craftedBy": [
        "simplemobs:soul_fire_charge"
      ],
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
      "questionary": {
        "en": {
          "lore": [
            "An egg that sits on soul sand as if it had always been there."
          ],
          "use": [
            "Spawns a Soulfire Blaze. In the wild it appears only in Soul Sand Valleys, over soul sand or soul soil, in packs of one or two."
          ],
          "secret": [
            "It shoots three soul fireballs of 6 damage each, but water hurts it and snowballs deal 3 damage."
          ]
        },
        "de": {
          "lore": [
            "Ein Ei, das auf Seelensand liegt, als wäre es immer schon dort gewesen."
          ],
          "use": [
            "Beschwört eine Seelenfeuer-Lohe. In freier Wildbahn erscheint sie nur in Seelensandtälern über Seelensand oder Seelenerde, in Gruppen von ein bis zwei."
          ],
          "secret": [
            "Sie feuert drei Seelenfeuerbälle zu je 6 Schaden, doch Wasser verletzt sie und Schneebälle richten 3 Schaden an."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:soulfire_powder",
      "name": {
        "en_us": "Soulfire Powder",
        "de_de": "Seelenfeuer-Lohenstaub"
      },
      "note": {
        "en": {
          "summary": "Crafted from a Soulfire Rod (2 powder). Ingredient of the Soulfire Charge."
        },
        "de": {
          "summary": "Wird aus einer Seelenfeuer-Rute gecraftet (2 Staub). Zutat der Seelenfeuerkugel."
        }
      },
      "texture": "assets/textures/simplemobs/item/soulfire_powder.png",
      "questionary": {
        "en": {
          "lore": [
            "Ground soulfire rod, blue and cold to the eye."
          ],
          "use": [
            "Craft one Soulfire Rod into 2 powder. Combine with gunpowder and coal or charcoal for Soulfire Charges."
          ]
        },
        "de": {
          "lore": [
            "Zermahlene Seelenfeuer-Rute, blau und kalt anzusehen."
          ],
          "use": [
            "Eine Seelenfeuer-Rute ergibt 2 Staub. Mit Schießpulver und Kohle oder Holzkohle entstehen Seelenfeuerkugeln."
          ]
        }
      },
      "craftedBy": [
        "simplemobs:soulfire_powder"
      ],
      "usedIn": [
        "simplemobs:soul_fire_charge"
      ]
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
      "questionary": {
        "en": {
          "lore": [
            "A rod with a blue tongue of fire that gives no heat but still burns."
          ],
          "use": [
            "Dropped by the Soulfire Blaze, 0 to 1, more with Looting. Awkward Potion plus rod brews a long Fire Resistance (8:00) directly."
          ],
          "secret": [
            "The fire is soul fire: on soul sand or soul soil the blaze's flames set soul fire."
          ]
        },
        "de": {
          "lore": [
            "Ein Stab mit blauer Feuerzunge, der keine Wärme gibt und doch brennt."
          ],
          "use": [
            "Vom Seelenfeuer-Lohe fallen gelassen, 0 bis 1, mehr mit Plünderung. Seltsamer Trank plus Stab braut direkt langes Feuerresistenz (8:00)."
          ],
          "secret": [
            "Das Feuer ist Seelenfeuer: Auf Seelensand oder Seelenerde entzünden die Flammen der Lohe Seelenfeuer."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": [
        "simplemobs:soulfire_powder"
      ]
    },
    {
      "id": "simplemobs:trick_mirror",
      "name": {
        "en_us": "Trick Mirror",
        "de_de": "Trugbild-Spiegel"
      },
      "note": {
        "en": {
          "summary": "Hold up for 3 s: the nearest three fakes and disguised Deceivers or players in a narrow cone (12 blocks) glow briefly and lose their disguise. 60 s cooldown."
        },
        "de": {
          "summary": "3 s hochhalten: die drei nächsten Fälschungen und verkleideten Deceiver oder Spieler in einem schmalen Kegel (12 Blöcke) leuchten kurz auf und verlieren ihre Tarnung. 60 s Abklingzeit."
        }
      },
      "texture": "assets/textures/simplemobs/item/trick_mirror.png",
      "questionary": {
        "en": {
          "lore": [
            "A mirror that shows what is real, which is rarely flattering."
          ],
          "use": [
            "Craft gold, amethyst shard, deceiver cloth and a stick. Hold it up for 3 seconds: in a 20 degree cone up to 12 blocks, the nearest three fakes or disguised foes glow briefly and their disguise falls. 64 uses, 60 s cooldown."
          ],
          "secret": [
            "It also drops from the Deceiver at 8%. Copies of the Deceiver glow along with it."
          ]
        },
        "de": {
          "lore": [
            "Ein Spiegel, der zeigt, was echt ist, und das ist selten schmeichelhaft."
          ],
          "use": [
            "Gold, Amethystscherbe, Betrügertuch und Stock herstellen. 3 Sekunden hochhalten: In einem 20-Grad-Kegel bis 12 Blöcke leuchten die drei nächsten Fälschungen oder Verkleideten kurz auf, ihre Verkleidung fällt. 64 Nutzungen, 60 s Abklingzeit."
          ],
          "secret": [
            "Er fällt auch mit 8 % vom Betrüger. Kopien des Betrügers leuchten gleich mit."
          ]
        }
      },
      "craftedBy": [
        "simplemobs:trick_mirror"
      ],
      "usedIn": []
    },
    {
      "id": "simplemobs:watcher_eye",
      "name": {
        "en_us": "Watcher Eye",
        "de_de": "Wächterauge"
      },
      "note": {
        "en": {
          "summary": "Dropped by the Hollow Watcher. Awkward Potion + Watcher Eye brews the Potion of Focus (3:00 immune to Shivering, redstone 8:00)."
        },
        "de": {
          "summary": "Lässt der Hohle Wächter fallen. Seltsamer Trank + Wächterauge ergibt den Trank des Fokus (3:00 immun gegen Zittern, Redstone 8:00)."
        }
      },
      "texture": "assets/textures/simplemobs/item/watcher_eye.png",
      "questionary": {
        "en": {
          "lore": [
            "An eye that keeps looking at whoever holds it. Do not look back for too long."
          ],
          "use": [
            "Dropped by the Hollow Watcher on the outer End islands. Awkward Potion plus Watcher Eye brews the Potion of Focus: 3:00 immune to Shivering (8:00 with redstone), splash and lingering as usual."
          ],
          "secret": [
            "Focus keeps the aim steady even in a Watcher's scream, but not the eyes: the scream still blinds."
          ]
        },
        "de": {
          "lore": [
            "Ein Auge, das jeden ansieht, der es hält. Schau nicht zu lange zurück."
          ],
          "use": [
            "Lässt der Hohle Wächter auf den äußeren End-Inseln fallen. Seltsamer Trank plus Wächterauge ergibt den Trank des Fokus: 3:00 immun gegen Zittern (8:00 mit Redstone), Wurf- und Verweiltrank wie üblich."
          ],
          "secret": [
            "Fokus hält das Zielen ruhig, auch beim Schrei eines Wächters – blind macht der Schrei trotzdem."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobs:wither_heart",
      "name": {
        "en_us": "Wither Heart",
        "de_de": "Witherherz"
      },
      "note": {
        "en": {
          "summary": "Dropped by the Blighted Wither, exactly one per kill. Core of the Biome Brush."
        },
        "de": {
          "summary": "Fällt vom Verdorrten Wither, genau eins pro Kampf. Kern des Biom-Pinsels."
        }
      },
      "texture": "assets/textures/simplemobs/item/wither_heart.png",
      "questionary": {
        "en": {
          "lore": [
            "It still beats, slowly, long after the Wither has stopped. Ash falls from it if you shake it."
          ],
          "use": [
            "Dropped by the Blighted Wither, exactly one per kill. With a Netherite Ingot and a Brush it becomes the Biome Brush."
          ],
          "secret": [
            "Only the Blighted Wither has one. Summon it by building the Wither figure on a Block of Netherite."
          ]
        },
        "de": {
          "lore": [
            "Es schlägt noch, langsam, lange nachdem der Wither verstummt ist. Schüttelt man es, rieselt Asche heraus."
          ],
          "use": [
            "Fällt vom Verdorrten Wither, genau eins pro Kampf. Mit einem Netheritbarren und einem Pinsel wird daraus der Biom-Pinsel."
          ],
          "secret": [
            "Nur der Verdorrte Wither hat eins. Du rufst ihn, indem du die Wither-Figur auf einen Netheritblock baust."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": [
        "simplemobs:biome_brush"
      ]
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
    "features": 12,
    "recipes": 55,
    "lootTables": 8,
    "tags": 6,
    "advancements": 0,
    "enchantments": 0,
    "items": 22,
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
