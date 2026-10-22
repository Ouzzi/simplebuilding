window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplemobheads"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplemobheads",
    "name": "Simple Mobheads",
    "version": "0.9.0",
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
      "id": "heads",
      "en": {
        "title": "A Head for Every Mob",
        "summary": "97 heads: every mob of 26.3 that has no Vanilla head, the Simple mods' mobs, and the heads that moved in from Simple Fun and SimpleBuilding, plus four secret heads.",
        "details": [
          "Monsters, Nether mobs, animals, water mobs and bosses; the creeper, zombie, skeleton, wither skeleton, piglin and ender dragon keep their Vanilla heads, the player head stays Vanilla.",
          "Each head shows its mob's real texture (by reference, so resource packs repaint the heads too) and the head of the mob's own model, set on the floor or hung on a wall.",
          "A test walks the whole entity registry: every mob has a head or a stated reason (Vanilla head)."
        ]
      },
      "de": {
        "title": "Ein Kopf für jeden Mob",
        "summary": "97 Köpfe: jeder Mob von 26.3 ohne Vanilla-Kopf, die Mobs der Simple-Mods und die Köpfe, die aus Simple Fun und SimpleBuilding herüberkamen, dazu vier geheime Köpfe.",
        "details": [
          "Monster, Nether-Mobs, Tiere, Wassermobs und Bosse; Creeper, Zombie, Skelett, Witherskelett, Piglin und Enderdrache behalten ihre Vanilla-Köpfe, der Spielerkopf bleibt Vanilla.",
          "Jeder Kopf zeigt die echte Textur seines Mobs (per Verweis, Ressourcenpakete färben die Köpfe mit um) und den Kopf aus dem Modell des Mobs, stehend auf dem Boden oder an der Wand.",
          "Ein Test geht die ganze Entity-Registry durch: jeder Mob hat einen Kopf oder einen genannten Grund (Vanilla-Kopf)."
        ]
      },
      "sources": [
        "modules/simplemobheads/data/mobs.json",
        "modules/simplemobheads/tools/generate_heads.py",
        "modules/simplemobheads/shared/java/com/simplemobheads/MobHeads.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/data/HeadTable.java"
      ]
    },
    {
      "id": "drops",
      "en": {
        "title": "Charged Creeper Drops",
        "summary": "Only a charged creeper's explosion drops a mob's head, like Vanilla's creeper, zombie and skeleton heads: one head per explosion. No recipe.",
        "details": [
          "The explosion must kill the mob; babies drop the normal head. Looting does not change it.",
          "The wither's head is off by default (server switch), the warden's on.",
          "Optional, off by default: a player's kill may drop the head too (base chance plus a bonus per Looting level, both bounded).",
          "The wandering trader sells most heads rarely through Simple Money; boss heads are not sold."
        ]
      },
      "de": {
        "title": "Beute geladener Creeper",
        "summary": "Nur die Explosion eines geladenen Creepers lässt den Kopf eines Mobs fallen, wie bei Vanillas Creeper-, Zombie- und Skelettköpfen: ein Kopf je Explosion. Kein Rezept.",
        "details": [
          "Die Explosion muss den Mob töten; Jungtiere lassen den normalen Kopf fallen. Plünderung ändert nichts.",
          "Der Witherkopf ist standardmäßig aus (Server-Schalter), der Wärterkopf an.",
          "Optional, standardmäßig aus: auch ein Spieler-Kill kann den Kopf fallen lassen (Grundchance plus Bonus je Plünderungsstufe, beides begrenzt).",
          "Der fahrende Händler verkauft die meisten Köpfe selten über Simple Money; Boss-Köpfe nicht."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/drop/HeadDrops.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/drop/HeadDropCondition.java"
      ]
    },
    {
      "id": "variants",
      "en": {
        "title": "Variants",
        "summary": "A head keeps its mob's variant: a Siamese cat drops a Siamese cat head, a red sheep a red sheep head.",
        "details": [
          "Variants: cat, wolf, frog, pig, cow, chicken, sheep (16 wool colors), shulker (17 colors), axolotl, fox, parrot, rabbit, horse, llama, trader llama, mooshroom, panda, copper golem (oxidation), strider, villager and zombie villager (biome), zombie nautilus and tropical fish (pattern).",
          "The item name adds the variant (\"Cat Head (Siamese)\"); the Simple Mob Heads tab lists every variant. A placed head keeps it and drops it again."
        ]
      },
      "de": {
        "title": "Varianten",
        "summary": "Ein Kopf behält die Variante seines Mobs: eine Siamkatze lässt einen Siam-Katzenkopf fallen, ein rotes Schaf einen roten Schafskopf.",
        "details": [
          "Varianten: Katze, Wolf, Frosch, Schwein, Kuh, Huhn, Schaf (16 Wollfarben), Shulker (17 Farben), Axolotl, Fuchs, Papagei, Kaninchen, Pferd, Lama, Händlerlama, Pilzkuh, Panda, Kupfergolem (Oxidation), Schreiter, Dorfbewohner und Zombiedorfbewohner (Biom), Zombie-Nautilus und Tropenfisch (Muster).",
          "Der Itemname nennt die Variante (\"Katzenkopf (Siam)\"); der Tab Simple Mob Heads zeigt jede Variante. Ein aufgestellter Kopf behält sie und lässt sie wieder fallen."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/drop/Variants.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/drop/CopyVariantFunction.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/MobHeadItem.java"
      ]
    },
    {
      "id": "wearing",
      "en": {
        "title": "Wearing, Placing, Note Blocks",
        "summary": "Worn on the head, a mob's head makes that mob see you only half as far (like Vanilla's zombie head). Placed on a note block it plays its mob's voice.",
        "details": [
          "Heads stand on the floor (16 turns) or hang on walls, break with any tool, and go in the helmet slot.",
          "Worn heads replace the wearer's own head, so small heads (silverfish, chicken) do not sit inside it.",
          "Worn heads are drawn as big as the head on the mob's own model (at most 14.25 px, larger heads are shrunk).",
          "With SimpleBuilding, its extra head slot works like the helmet slot (framework WornHeads): same size, same variants, same abilities; if both slots hold a head, the extra slot's model is drawn and the abilities of both apply."
        ]
      },
      "de": {
        "title": "Tragen, Aufstellen, Notenblock",
        "summary": "Getragen sieht der Mob des Kopfes dich nur halb so weit (wie Vanillas Zombiekopf). Auf einem Notenblock spielt er die Stimme seines Mobs.",
        "details": [
          "Köpfe stehen auf dem Boden (16 Drehungen) oder hängen an Wänden, lassen sich mit allem abbauen und passen in den Helm-Slot.",
          "Getragene Köpfe ersetzen den Kopf des Trägers, damit kleine Köpfe (Silberfischchen, Huhn) nicht darin verschwinden.",
          "Getragene Köpfe sind so groß wie der Kopf am Modell des Mobs (höchstens 14,25 px, größere Köpfe werden verkleinert).",
          "Mit SimpleBuilding wirkt dessen Extra-Kopf-Slot wie der Helm-Slot (framework WornHeads): gleiche Größe, gleiche Varianten, gleiche Fähigkeiten; tragen beide Slots einen Kopf, wird das Modell des Extra-Slots gezeichnet und die Fähigkeiten beider gelten."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/MobHeads.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/client/WornModHeadMixin.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/client/CustomHeadLayerMixin.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/ability/HeadAbilities.java"
      ]
    },
    {
      "id": "abilities",
      "en": {
        "title": "Head Abilities",
        "summary": "28 heads hide a small, harmless ability while worn, each with its own server switch.",
        "details": [
          "Blaze: magma blocks and campfires do not burn. Enderman: ender pearls cost no health. Husk: food gives no Hunger. Bogged: food does not poison. Spider: cobwebs do not slow. Cave spider: bare hands cut cobwebs fast. Stray: powder snow does not freeze. Slime: one more block of safe fall. Silverfish: half size. Breeze: no trampled farmland. Shulker: blocked shulker boxes open. Drowned: magma bubble columns do not pull down.",
          "Cow: others milk you with a bucket. Mooshroom: others fill a bowl with mushroom stew. Sheep: others shear you (1-3 wool, then it grows back). Chicken: an egg now and then. Pig: sneak on grass or dirt, the truffle nose digs something up. Sniffer: sneak five seconds on moss or dirt, it sniffs up seeds (torchflower and pitcher rarely).",
          "Bee: harvesting a hive does not anger its bees. Fox: sweet berry bushes do not prick. Frog: higher jumps from lily pads and out of water. Goat: a ram only pushes, no damage. Cat: creepers keep away. Allay: dropped items nearby float up a little. Turtle: no trampled turtle eggs. Polar bear: ice is less slippery. Wolf: wild wolves never attack. Warden: half of the wearer's steps never reach sculk."
        ]
      },
      "de": {
        "title": "Kopf-Fähigkeiten",
        "summary": "28 Köpfe verbergen beim Tragen eine kleine, harmlose Fähigkeit, jede mit eigenem Server-Schalter.",
        "details": [
          "Lohe: Magmablöcke und Lagerfeuer brennen nicht. Enderman: Enderperlen kosten keine Herzen. Wüstenzombie: Essen macht keinen Hunger. Sumpfskelett: Essen vergiftet nicht. Spinne: Netze bremsen nicht. Höhlenspinne: die Hand schneidet Netze schnell. Eiswanderer: Pulverschnee friert nicht ein. Schleim: ein Block mehr sicherer Fall. Silberfischchen: halbe Größe. Breeze: kein zertrampeltes Ackerland. Shulker: versperrte Shulkerkisten gehen auf. Ertrunkener: Magma-Blasensäulen ziehen nicht nach unten.",
          "Kuh: andere melken dich mit einem Eimer. Pilzkuh: andere füllen eine Schüssel mit Pilzsuppe. Schaf: andere scheren dich (1-3 Wolle, dann wächst sie nach). Huhn: ab und zu ein Ei. Schwein: auf Gras oder Erde schleichen, die Trüffelnase scharrt etwas aus. Schnüffler: fünf Sekunden auf Moos oder Erde schleichen, er erschnüffelt Samen (Fackellilie und Kannenpflanze selten).",
          "Biene: beim Ernten eines Stocks werden die Bienen nicht wütend. Fuchs: Süßbeerensträucher stechen nicht. Frosch: höhere Sprünge von Seerosen und aus dem Wasser. Ziege: ein Rammstoß schubst nur, ohne Schaden. Katze: Creeper halten Abstand. Hilfsgeist: liegende Items in der Nähe schweben leicht. Schildkröte: keine zertrampelten Eier. Eisbär: Eis rutscht weniger. Wolf: wilde Wölfe greifen nie an. Wärter: die Hälfte der Schritte erreicht kein Sculk."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/ability/HeadAbilities.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/HeadAbilityLivingEntityMixin.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/BeehiveBlockMixin.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/CreeperMixin.java"
      ]
    },
    {
      "id": "greeting",
      "en": {
        "title": "Greeting",
        "summary": "Sneaking with a mob head on plays its mob's voice quietly with a few notes, at most every five seconds (server switch).",
        "details": [
          "Moved in from Simple Fun's animal heads; now every head greets. Cosmetic only."
        ]
      },
      "de": {
        "title": "Gruß",
        "summary": "Schleichen mit Mob-Kopf spielt leise die Stimme des Mobs mit ein paar Noten, höchstens alle fünf Sekunden (Server-Schalter).",
        "details": [
          "Aus Simple Funs Tierköpfen übernommen; jetzt grüßt jeder Kopf. Nur Kosmetik."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/ability/HeadAbilities.java"
      ]
    },
    {
      "id": "player_heads",
      "en": {
        "title": "Player Heads",
        "summary": "A player killed by another player drops their own head with their skin (server switch, default on; moved in from Simple Fun).",
        "details": [
          "/simplemobheads pvp headDrops true|false; the old /simplefun pvp headDrops still works and names the new command."
        ]
      },
      "de": {
        "title": "Spielerköpfe",
        "summary": "Ein von einem anderen Spieler getöteter Spieler lässt seinen Kopf mit Skin fallen (Server-Schalter, standardmäßig an; aus Simple Fun übernommen).",
        "details": [
          "/simplemobheads pvp headDrops true|false; das alte /simplefun pvp headDrops geht weiter und nennt den neuen Befehl."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/drop/HeadDrops.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/command/MobHeadsCommands.java"
      ]
    },
    {
      "id": "commands",
      "en": {
        "title": "Server Commands",
        "summary": "Permission level 4: /simplemobheads pvp headDrops, drops creeper|wither|warden|playerKill, drops playerKillChance|lootingBonus, ability <id>, greeting, enabled.",
        "details": [
          "Every value is bounded server-side and saved to config/simplemobheads.json; the config screen edits the same file."
        ]
      },
      "de": {
        "title": "Server-Befehle",
        "summary": "Berechtigung 4: /simplemobheads pvp headDrops, drops creeper|wither|warden|playerKill, drops playerKillChance|lootingBonus, ability <id>, greeting, enabled.",
        "details": [
          "Jeder Wert ist serverseitig begrenzt und steht in config/simplemobheads.json; der Einstellungsbildschirm bearbeitet dieselbe Datei."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/command/MobHeadsCommands.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/config/MobHeadsConfig.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/client/MobHeadsConfigScreen.java"
      ]
    },
    {
      "id": "migration",
      "en": {
        "title": "Moved In",
        "summary": "Simple Fun's four animal heads and SimpleBuilding's twelve mob heads are Simple Mobheads heads now; old worlds keep them.",
        "details": [
          "simplefun:pig_head, simplebuilding:blaze_head, simplebuilding:stray_skull ... become simplemobheads:* - in inventories, chests and as placed heads (SimpleLib IdMoves).",
          "Simple Fun's head settings are taken over once from config/simplefun.json; Simple Fun 1.4.0 has no heads any more.",
          "SimpleBuilding requires Simple Mobheads; its pads use the same heads."
        ]
      },
      "de": {
        "title": "Umgezogen",
        "summary": "Simple Funs vier Tierköpfe und SimpleBuildings zwölf Mob-Köpfe sind jetzt Köpfe von Simple Mobheads; alte Welten behalten sie.",
        "details": [
          "simplefun:pig_head, simplebuilding:blaze_head, simplebuilding:stray_skull ... werden zu simplemobheads:* - im Inventar, in Truhen und als aufgestellte Köpfe (SimpleLib IdMoves).",
          "Die Kopf-Einstellungen von Simple Fun werden einmal aus config/simplefun.json übernommen; Simple Fun 1.4.0 hat keine Köpfe mehr.",
          "SimpleBuilding setzt Simple Mobheads voraus; seine Pads nutzen dieselben Köpfe."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/HeadMoves.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/SimpleMobHeads.java"
      ]
    },
    {
      "id": "mod_mobs",
      "en": {
        "title": "Simple Mods' Mobs",
        "summary": "Heads of the Simple mods' mobs (Deceiver, Hollow Howler, Dread, Hollow Watcher, Shellker, Soulfire Blaze, Blighted and Abyssal Wither, Nibblemite, Dragonmite) are always registered; they drop only when that mod is there.",
        "details": [
          "Without the mod the head stays in the creative tab and in trades; its model falls back to a plain cube if the mod's model is missing. The withers' heads follow the wither switch."
        ]
      },
      "de": {
        "title": "Mobs der Simple-Mods",
        "summary": "Die Köpfe der Mobs der Simple-Mods (Täuscher, Hohlheuler, Schrecken, Hohlwächter, Shellker, Seelenfeuerlohe, Verderbter und Abgrund-Wither, Knabbermilbe, Drachenmilbe) sind immer registriert; sie fallen nur, wenn die Mod da ist.",
        "details": [
          "Ohne die Mod bleibt der Kopf im Kreativ-Tab und im Handel; fehlt das Modell der Mod, zeigt er einen einfachen Würfel. Die Wither-Köpfe folgen dem Wither-Schalter."
        ]
      },
      "sources": [
        "modules/simplemobheads/data/mobs.json",
        "modules/simplemobheads/shared/java/com/simplemobheads/client/MobHeadModels.java"
      ]
    },
    {
      "id": "creative_tab",
      "en": {
        "title": "Creative Tab",
        "summary": "Own tab \"Simple Mob Heads\" with every head and variant; in Functional Blocks and the search tab the heads stand right behind the dragon head.",
        "details": [
          "The shared switch in config/simple-creative-tabs.properties keeps the Vanilla tabs unchanged."
        ]
      },
      "de": {
        "title": "Kreativ-Tab",
        "summary": "Eigener Tab \"Simple Mob Heads\" mit jedem Kopf und jeder Variante; in Funktionsblöcken und im Suchtab stehen die Köpfe direkt hinter dem Drachenkopf.",
        "details": [
          "Der gemeinsame Schalter in config/simple-creative-tabs.properties lässt die Vanilla-Tabs unverändert."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/MobHeads.java"
      ]
    },
    {
      "id": "herobrine",
      "spoiler": true,
      "en": {
        "title": "He Was Never Removed",
        "summary": "A secret head that no mob drops. A rare book in mineshaft chests and the Questionary hint at how to get it.",
        "details": [
          "Found by lighting an old shrine at night. The shrine: 3x3 gold blocks, a mossy cobblestone in the middle, a redstone torch on each of the four edge gold blocks, netherrack on top; light the netherrack with flint and steel or a fire charge.",
          "Only at night. The lightning is only a show (no fire, no damage); the torches go out, the moss turns to cobblestone, gold and netherrack stay. One head per shrine and per player per in-game day (server switch and cooldown, kept across restarts).",
          "Afterwards, for five minutes and only for that player, something with white eyes watches from far away.",
          "Worn, torches nearby flicker, and in the dark hostile mobs that target the wearer hesitate for a moment (each mob at most every 30 seconds).",
          "A rare book in mineshaft chests hints at the shrine."
        ]
      },
      "de": {
        "title": "Er wurde nie entfernt",
        "summary": "Ein geheimer Kopf, den kein Mob fallen lässt. Ein seltenes Buch in Minenschacht-Truhen und der Fragentisch deuten an, wie man ihn bekommt.",
        "details": [
          "Man findet ihn, indem man nachts einen alten Schrein anzündet. Der Schrein: 3×3 Goldblöcke, in der Mitte bemooster Bruchstein, auf jedem der vier Kanten-Goldblöcke eine Redstone-Fackel, obendrauf Netherrack; das Netherrack mit Feuerzeug oder Feuerkugel anzünden.",
          "Nur nachts. Der Blitz ist nur Schau (kein Feuer, kein Schaden); die Fackeln erlöschen, das Moos wird zu Bruchstein, Gold und Netherrack bleiben. Ein Kopf je Schrein und je Spieler pro Ingame-Tag (Server-Schalter und Abklingzeit, übersteht Neustarts).",
          "Danach beobachtet fünf Minuten lang, nur für diesen Spieler, etwas mit weißen Augen aus der Ferne.",
          "Getragen flackern Fackeln in der Nähe, und im Dunkeln zögern feindliche Mobs, die den Träger angreifen wollen, kurz (jeder Mob höchstens alle 30 Sekunden).",
          "Ein seltenes Buch in Minenschacht-Truhen deutet den Schrein an."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/ritual/HerobrineRitual.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/ritual/HerobrineHaunt.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/ability/HeadAbilities.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/IgniteItemMixin.java"
      ]
    },
    {
      "id": "technoblade",
      "spoiler": true,
      "en": {
        "title": "Technoblade Never Dies",
        "summary": "A tribute head no mob drops: the Crowned Pig Head. The title screen and the Questionary hint at it.",
        "details": [
          "Name an adult pig \"Technoblade\" (exactly, with a name tag): within a second it wears a golden crown.",
          "While it wears the crown it never dies: a deadly hit leaves it at half a heart with a totem's flash. If a player struck that hit, the crown falls off as the Crowned Pig Head (lore \"Technoblade never dies\"); without a player the crown stays.",
          "The crown grows back after an in-game day, and the same player gets the next crowned head only after an in-game day (server switch and cooldown). The void and /kill still work; lightning never turns Technoblade into a zombified piglin.",
          "Worn, pigs nearby turn their heads to the wearer and the crown glints now and then. The head looks like his skin: a player-sized pig head with a thin golden crown set with blue, purple, red and green jewels, drawn anew (own pixels, no player profile is loaded). A pig named Technoblade wears the same crown."
        ]
      },
      "de": {
        "title": "Technoblade never dies",
        "summary": "Ein Tribut-Kopf, den kein Mob fallen lässt: der Gekrönte Schweinekopf. Startbildschirm und Fragentisch deuten ihn an.",
        "details": [
          "Ein erwachsenes Schwein \"Technoblade\" nennen (genau so, mit Namensschild): binnen einer Sekunde trägt es eine goldene Krone.",
          "Solange es die Krone trägt, stirbt es nie: ein tödlicher Schlag lässt ihm ein halbes Herz und den Blitz eines Totems. War es der Schlag eines Spielers, fällt die Krone als Gekrönter Schweinekopf herunter (Lore \"Technoblade never dies\"); ohne Spieler bleibt die Krone.",
          "Die Krone wächst nach einem Ingame-Tag nach, und derselbe Spieler bekommt den nächsten gekrönten Kopf erst nach einem Ingame-Tag (Server-Schalter und Abklingzeit). Leere und /kill wirken weiter; ein Blitz macht aus Technoblade nie einen zombifizierten Piglin.",
          "Getragen drehen Schweine in der Nähe den Kopf zum Träger, und die Krone glänzt ab und zu. Der Kopf sieht aus wie sein Skin: ein Schweinekopf in Spielergröße mit einer schmalen goldenen Krone mit blauen, lila, roten und grünen Steinen, neu gezeichnet (eigene Pixel, es wird kein Spielerprofil geladen). Ein Schwein namens Technoblade trägt dieselbe Krone."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/easter/Technoblade.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/EasterDeathMixin.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/AnimalAiMixin.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/PigThunderMixin.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/client/TechnobladeCrownLayer.java",
        "tools/textures/easter_heads_2026_10_22.py"
      ]
    },
    {
      "id": "killer_bunny",
      "spoiler": true,
      "en": {
        "title": "Killer Bunny",
        "summary": "A very rare rabbit is a killer bunny; its head is the rabbit head's Killer Bunny variant.",
        "details": [
          "In flower forests and meadows one naturally spawned rabbit in a thousand is a killer bunny (biome tag simplemobheads:killer_bunny_spawns, server chance and switch).",
          "It always attacks players and wolves and never flees, hops one and a half times as fast and bites lightly (one heart). Killer bunnies from commands stay Vanilla.",
          "A charged creeper's explosion drops its head, like every head."
        ]
      },
      "de": {
        "title": "Killerkaninchen",
        "summary": "Ein sehr seltenes Kaninchen ist ein Killerkaninchen; sein Kopf ist die Variante Killerkaninchen des Kaninchenkopfs.",
        "details": [
          "In Blumenwäldern und auf Wiesen ist eines von tausend natürlich gespawnten Kaninchen ein Killerkaninchen (Biom-Tag simplemobheads:killer_bunny_spawns, Server-Chance und Schalter).",
          "Es greift Spieler und Wölfe immer an und flieht nie, hoppelt anderthalbmal so schnell und beißt leicht (ein Herz). Killerkaninchen aus Befehlen bleiben Vanilla.",
          "Die Explosion eines geladenen Creepers lässt seinen Kopf fallen, wie bei jedem Kopf."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/easter/KillerBunny.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/RabbitSpawnMixin.java"
      ]
    },
    {
      "id": "giants",
      "spoiler": true,
      "en": {
        "title": "Giants",
        "summary": "Two giant heads, one and a half blocks big: the Giant Head and the Giant Cow Head. Only a charged creeper brings them down.",
        "details": [
          "Giant: natural lightning that strikes an adult zombie of any kind (zombie, husk, drowned, zombie villager, zombified piglin) on a full-moon night makes it three times as big, with 100 health, a harder hit and more step height. It keeps its kind and its look. It needs room to grow: under trees or a roof the bolt changes nothing.",
          "Giant cow: every hay bale an adult cow eats from your hand is one try in 5000 to make it a giant cow (three times as big, 30 health, the same drops and milk as any cow); a cow eats at most 64 bales, then it is full. A lucky cow in a barn too low keeps its luck and grows as soon as it stands in the open.",
          "Only a charged creeper's explosion drops a giant's head, with its kind or variant, instead of the normal head. A player's kill gives none. Vanilla's giant (only by command) drops the Giant Head too.",
          "Worn, the wearer grows to one and a half times the size and steps up whole blocks; the Giant Head also walks a little faster, hides the wearer from every zombie kind (half the sight) and works like the head of its kind (husk: food gives no Hunger, drowned: magma bubbles do not drag down). The Giant Cow Head works like the cow head (others can milk the wearer).",
          "Placed, a giant head is one and a half blocks big on a full block; in the inventory and worn it has the normal head size."
        ]
      },
      "de": {
        "title": "Riesen",
        "summary": "Zwei Riesenköpfe, anderthalb Blöcke groß: der Riesenkopf und der Riesenkuhkopf. Nur ein geladener Creeper bringt sie zu Fall.",
        "details": [
          "Riese: Ein natürlicher Blitz, der in einer Vollmondnacht einen erwachsenen Zombie jeder Art trifft (Zombie, Wüstenzombie, Ertrunkener, Zombiedorfbewohner, zombifizierter Piglin), macht ihn dreimal so groß, mit 100 Lebenspunkten, härterem Schlag und mehr Stufenhöhe. Er behält Art und Aussehen. Er braucht Platz zum Wachsen: unter Bäumen oder einem Dach ändert der Blitz nichts.",
          "Riesen-Kuh: Jeder Heuballen, den eine erwachsene Kuh aus der Hand frisst, ist ein Versuch von 5000, sie zur Riesen-Kuh zu machen (dreimal so groß, 30 Lebenspunkte, Beute und Milch wie jede Kuh); eine Kuh frisst höchstens 64 Ballen, dann ist sie satt. Eine Glückskuh in einem zu niedrigen Stall behält ihr Glück und wächst, sobald sie im Freien steht.",
          "Nur die Explosion eines geladenen Creepers lässt den Kopf eines Riesen fallen, mit seiner Art oder Variante, statt des normalen Kopfs. Ein Spieler-Kill gibt keinen. Vanillas Riese (nur per Befehl) lässt auch den Riesenkopf fallen.",
          "Getragen wächst der Träger auf anderthalbfache Größe und steigt ganze Blöcke hoch; der Riesenkopf läuft zusätzlich etwas schneller, tarnt vor jeder Zombie-Art (halbe Sicht) und wirkt wie der Kopf seiner Art (Wüstenzombie: Essen macht nicht hungrig, Ertrunkener: Magma-Blasen ziehen nicht hinab). Der Riesenkuhkopf wirkt wie der Kuhkopf (andere können den Träger melken).",
          "Aufgestellt ist ein Riesenkopf anderthalb Blöcke groß auf einem vollen Block; im Inventar und getragen hat er normale Kopfgröße."
        ]
      },
      "sources": [
        "modules/simplemobheads/shared/java/com/simplemobheads/easter/Giants.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/EasterThunderMixin.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/CowFeedMixin.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/mixin/CreeperMixin.java",
        "modules/simplemobheads/shared/java/com/simplemobheads/MobHeads.java"
      ]
    }
  ],
  "recipes": [
    {
      "id": "simplemobheads:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplemobheads:guide_book",
        "count": 1
      },
      "source": "modules/simplemobheads/shared/resources/data/simplemobheads/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:skeleton_skull"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:skeleton_skull"
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
            "id": "minecraft:leather",
            "count": 1
          },
          {
            "id": "minecraft:skeleton_skull",
            "count": 1
          }
        ]
      }
    }
  ],
  "lootTables": [
    {
      "id": "simplemobheads:blocks/abyssal_wither_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:abyssal_wither_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/abyssal_wither_head.json"
    },
    {
      "id": "simplemobheads:blocks/allay_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:allay_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/allay_head.json"
    },
    {
      "id": "simplemobheads:blocks/armadillo_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:armadillo_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/armadillo_head.json"
    },
    {
      "id": "simplemobheads:blocks/axolotl_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:axolotl_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/axolotl_head.json"
    },
    {
      "id": "simplemobheads:blocks/bat_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:bat_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/bat_head.json"
    },
    {
      "id": "simplemobheads:blocks/bee_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:bee_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/bee_head.json"
    },
    {
      "id": "simplemobheads:blocks/blaze_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:blaze_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/blaze_head.json"
    },
    {
      "id": "simplemobheads:blocks/blighted_wither_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:blighted_wither_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/blighted_wither_head.json"
    },
    {
      "id": "simplemobheads:blocks/bogged_skull",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:bogged_skull"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/bogged_skull.json"
    },
    {
      "id": "simplemobheads:blocks/breeze_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:breeze_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/breeze_head.json"
    },
    {
      "id": "simplemobheads:blocks/camel_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:camel_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/camel_head.json"
    },
    {
      "id": "simplemobheads:blocks/camel_husk_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:camel_husk_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/camel_husk_head.json"
    },
    {
      "id": "simplemobheads:blocks/cat_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:cat_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/cat_head.json"
    },
    {
      "id": "simplemobheads:blocks/cave_spider_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:cave_spider_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/cave_spider_head.json"
    },
    {
      "id": "simplemobheads:blocks/chicken_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:chicken_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/chicken_head.json"
    },
    {
      "id": "simplemobheads:blocks/cod_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:cod_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/cod_head.json"
    },
    {
      "id": "simplemobheads:blocks/copper_golem_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:copper_golem_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/copper_golem_head.json"
    },
    {
      "id": "simplemobheads:blocks/cow_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:cow_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/cow_head.json"
    },
    {
      "id": "simplemobheads:blocks/creaking_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:creaking_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/creaking_head.json"
    },
    {
      "id": "simplemobheads:blocks/crowned_pig_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:crowned_pig_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/crowned_pig_head.json"
    },
    {
      "id": "simplemobheads:blocks/deceiver_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:deceiver_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/deceiver_head.json"
    },
    {
      "id": "simplemobheads:blocks/dolphin_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:dolphin_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/dolphin_head.json"
    },
    {
      "id": "simplemobheads:blocks/donkey_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:donkey_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/donkey_head.json"
    },
    {
      "id": "simplemobheads:blocks/dragonmite_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:dragonmite_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/dragonmite_head.json"
    },
    {
      "id": "simplemobheads:blocks/dread_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:dread_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/dread_head.json"
    },
    {
      "id": "simplemobheads:blocks/drowned_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:drowned_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/drowned_head.json"
    },
    {
      "id": "simplemobheads:blocks/elder_guardian_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:elder_guardian_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/elder_guardian_head.json"
    },
    {
      "id": "simplemobheads:blocks/enderman_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:enderman_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/enderman_head.json"
    },
    {
      "id": "simplemobheads:blocks/endermite_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:endermite_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/endermite_head.json"
    },
    {
      "id": "simplemobheads:blocks/evoker_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:evoker_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/evoker_head.json"
    },
    {
      "id": "simplemobheads:blocks/fox_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:fox_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/fox_head.json"
    },
    {
      "id": "simplemobheads:blocks/frog_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:frog_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/frog_head.json"
    },
    {
      "id": "simplemobheads:blocks/ghast_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:ghast_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/ghast_head.json"
    },
    {
      "id": "simplemobheads:blocks/giant_cow_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:giant_cow_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/giant_cow_head.json"
    },
    {
      "id": "simplemobheads:blocks/giant_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:giant_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/giant_head.json"
    },
    {
      "id": "simplemobheads:blocks/glow_squid_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:glow_squid_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/glow_squid_head.json"
    },
    {
      "id": "simplemobheads:blocks/goat_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:goat_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/goat_head.json"
    },
    {
      "id": "simplemobheads:blocks/guardian_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:guardian_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/guardian_head.json"
    },
    {
      "id": "simplemobheads:blocks/happy_ghast_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:happy_ghast_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/happy_ghast_head.json"
    },
    {
      "id": "simplemobheads:blocks/herobrine_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:herobrine_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/herobrine_head.json"
    },
    {
      "id": "simplemobheads:blocks/hoglin_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:hoglin_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/hoglin_head.json"
    },
    {
      "id": "simplemobheads:blocks/hollow_howler_skull",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:hollow_howler_skull"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/hollow_howler_skull.json"
    },
    {
      "id": "simplemobheads:blocks/hollow_watcher_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:hollow_watcher_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/hollow_watcher_head.json"
    },
    {
      "id": "simplemobheads:blocks/horse_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:horse_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/horse_head.json"
    },
    {
      "id": "simplemobheads:blocks/husk_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:husk_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/husk_head.json"
    },
    {
      "id": "simplemobheads:blocks/illusioner_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:illusioner_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/illusioner_head.json"
    },
    {
      "id": "simplemobheads:blocks/iron_golem_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:iron_golem_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/iron_golem_head.json"
    },
    {
      "id": "simplemobheads:blocks/llama_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:llama_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/llama_head.json"
    },
    {
      "id": "simplemobheads:blocks/magma_cube_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:magma_cube_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/magma_cube_head.json"
    },
    {
      "id": "simplemobheads:blocks/mooshroom_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:mooshroom_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/mooshroom_head.json"
    },
    {
      "id": "simplemobheads:blocks/mule_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:mule_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/mule_head.json"
    },
    {
      "id": "simplemobheads:blocks/nautilus_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:nautilus_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/nautilus_head.json"
    },
    {
      "id": "simplemobheads:blocks/nibblemite_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:nibblemite_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/nibblemite_head.json"
    },
    {
      "id": "simplemobheads:blocks/ocelot_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:ocelot_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/ocelot_head.json"
    },
    {
      "id": "simplemobheads:blocks/panda_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:panda_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/panda_head.json"
    },
    {
      "id": "simplemobheads:blocks/parched_skull",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:parched_skull"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/parched_skull.json"
    },
    {
      "id": "simplemobheads:blocks/parrot_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:parrot_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/parrot_head.json"
    },
    {
      "id": "simplemobheads:blocks/phantom_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:phantom_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/phantom_head.json"
    },
    {
      "id": "simplemobheads:blocks/pig_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:pig_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/pig_head.json"
    },
    {
      "id": "simplemobheads:blocks/piglin_brute_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:piglin_brute_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/piglin_brute_head.json"
    },
    {
      "id": "simplemobheads:blocks/pillager_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:pillager_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/pillager_head.json"
    },
    {
      "id": "simplemobheads:blocks/polar_bear_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:polar_bear_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/polar_bear_head.json"
    },
    {
      "id": "simplemobheads:blocks/pufferfish_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:pufferfish_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/pufferfish_head.json"
    },
    {
      "id": "simplemobheads:blocks/rabbit_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:rabbit_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/rabbit_head.json"
    },
    {
      "id": "simplemobheads:blocks/ravager_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:ravager_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/ravager_head.json"
    },
    {
      "id": "simplemobheads:blocks/salmon_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:salmon_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/salmon_head.json"
    },
    {
      "id": "simplemobheads:blocks/sheep_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:sheep_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/sheep_head.json"
    },
    {
      "id": "simplemobheads:blocks/shellker_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:shellker_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/shellker_head.json"
    },
    {
      "id": "simplemobheads:blocks/shulker_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:shulker_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/shulker_head.json"
    },
    {
      "id": "simplemobheads:blocks/silverfish_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:silverfish_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/silverfish_head.json"
    },
    {
      "id": "simplemobheads:blocks/skeleton_horse_skull",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:skeleton_horse_skull"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/skeleton_horse_skull.json"
    },
    {
      "id": "simplemobheads:blocks/slime_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:slime_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/slime_head.json"
    },
    {
      "id": "simplemobheads:blocks/sniffer_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:sniffer_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/sniffer_head.json"
    },
    {
      "id": "simplemobheads:blocks/snow_golem_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:snow_golem_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/snow_golem_head.json"
    },
    {
      "id": "simplemobheads:blocks/soulfire_blaze_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:soulfire_blaze_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/soulfire_blaze_head.json"
    },
    {
      "id": "simplemobheads:blocks/spider_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:spider_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/spider_head.json"
    },
    {
      "id": "simplemobheads:blocks/squid_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:squid_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/squid_head.json"
    },
    {
      "id": "simplemobheads:blocks/stray_skull",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:stray_skull"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/stray_skull.json"
    },
    {
      "id": "simplemobheads:blocks/strider_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:strider_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/strider_head.json"
    },
    {
      "id": "simplemobheads:blocks/sulfur_cube_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:sulfur_cube_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/sulfur_cube_head.json"
    },
    {
      "id": "simplemobheads:blocks/tadpole_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:tadpole_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/tadpole_head.json"
    },
    {
      "id": "simplemobheads:blocks/trader_llama_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:trader_llama_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/trader_llama_head.json"
    },
    {
      "id": "simplemobheads:blocks/tropical_fish_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:tropical_fish_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/tropical_fish_head.json"
    },
    {
      "id": "simplemobheads:blocks/turtle_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:turtle_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/turtle_head.json"
    },
    {
      "id": "simplemobheads:blocks/vex_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:vex_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/vex_head.json"
    },
    {
      "id": "simplemobheads:blocks/villager_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:villager_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/villager_head.json"
    },
    {
      "id": "simplemobheads:blocks/vindicator_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:vindicator_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/vindicator_head.json"
    },
    {
      "id": "simplemobheads:blocks/wandering_trader_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:wandering_trader_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/wandering_trader_head.json"
    },
    {
      "id": "simplemobheads:blocks/warden_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:warden_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/warden_head.json"
    },
    {
      "id": "simplemobheads:blocks/witch_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:witch_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/witch_head.json"
    },
    {
      "id": "simplemobheads:blocks/wither_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:wither_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/wither_head.json"
    },
    {
      "id": "simplemobheads:blocks/wolf_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:wolf_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/wolf_head.json"
    },
    {
      "id": "simplemobheads:blocks/zoglin_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:zoglin_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/zoglin_head.json"
    },
    {
      "id": "simplemobheads:blocks/zombie_horse_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:zombie_horse_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/zombie_horse_head.json"
    },
    {
      "id": "simplemobheads:blocks/zombie_nautilus_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:zombie_nautilus_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/zombie_nautilus_head.json"
    },
    {
      "id": "simplemobheads:blocks/zombie_villager_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:zombie_villager_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/zombie_villager_head.json"
    },
    {
      "id": "simplemobheads:blocks/zombified_piglin_head",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemobheads:zombified_piglin_head"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/loot_table/blocks/zombified_piglin_head.json"
    }
  ],
  "tags": [
    {
      "id": "simplemobheads:item/heads",
      "replace": false,
      "values": [
        {
          "id": "simplemobheads:blaze_head",
          "required": true
        },
        {
          "id": "simplemobheads:enderman_head",
          "required": true
        },
        {
          "id": "simplemobheads:husk_head",
          "required": true
        },
        {
          "id": "simplemobheads:spider_head",
          "required": true
        },
        {
          "id": "simplemobheads:cave_spider_head",
          "required": true
        },
        {
          "id": "simplemobheads:stray_skull",
          "required": true
        },
        {
          "id": "simplemobheads:bogged_skull",
          "required": true
        },
        {
          "id": "simplemobheads:slime_head",
          "required": true
        },
        {
          "id": "simplemobheads:silverfish_head",
          "required": true
        },
        {
          "id": "simplemobheads:breeze_head",
          "required": true
        },
        {
          "id": "simplemobheads:shulker_head",
          "required": true
        },
        {
          "id": "simplemobheads:drowned_head",
          "required": true
        },
        {
          "id": "simplemobheads:pig_head",
          "required": true
        },
        {
          "id": "simplemobheads:cow_head",
          "required": true
        },
        {
          "id": "simplemobheads:chicken_head",
          "required": true
        },
        {
          "id": "simplemobheads:sheep_head",
          "required": true
        },
        {
          "id": "simplemobheads:zombie_villager_head",
          "required": true
        },
        {
          "id": "simplemobheads:witch_head",
          "required": true
        },
        {
          "id": "simplemobheads:pillager_head",
          "required": true
        },
        {
          "id": "simplemobheads:vindicator_head",
          "required": true
        },
        {
          "id": "simplemobheads:evoker_head",
          "required": true
        },
        {
          "id": "simplemobheads:illusioner_head",
          "required": true
        },
        {
          "id": "simplemobheads:ravager_head",
          "required": true
        },
        {
          "id": "simplemobheads:vex_head",
          "required": true
        },
        {
          "id": "simplemobheads:phantom_head",
          "required": true
        },
        {
          "id": "simplemobheads:guardian_head",
          "required": true
        },
        {
          "id": "simplemobheads:elder_guardian_head",
          "required": true
        },
        {
          "id": "simplemobheads:endermite_head",
          "required": true
        },
        {
          "id": "simplemobheads:warden_head",
          "required": true
        },
        {
          "id": "simplemobheads:creaking_head",
          "required": true
        },
        {
          "id": "simplemobheads:parched_skull",
          "required": true
        },
        {
          "id": "simplemobheads:camel_husk_head",
          "required": true
        },
        {
          "id": "simplemobheads:zombie_nautilus_head",
          "required": true
        },
        {
          "id": "simplemobheads:sulfur_cube_head",
          "required": true
        },
        {
          "id": "simplemobheads:ghast_head",
          "required": true
        },
        {
          "id": "simplemobheads:magma_cube_head",
          "required": true
        },
        {
          "id": "simplemobheads:hoglin_head",
          "required": true
        },
        {
          "id": "simplemobheads:zoglin_head",
          "required": true
        },
        {
          "id": "simplemobheads:piglin_brute_head",
          "required": true
        },
        {
          "id": "simplemobheads:zombified_piglin_head",
          "required": true
        },
        {
          "id": "simplemobheads:strider_head",
          "required": true
        },
        {
          "id": "simplemobheads:wither_head",
          "required": true
        },
        {
          "id": "simplemobheads:allay_head",
          "required": true
        },
        {
          "id": "simplemobheads:armadillo_head",
          "required": true
        },
        {
          "id": "simplemobheads:axolotl_head",
          "required": true
        },
        {
          "id": "simplemobheads:bat_head",
          "required": true
        },
        {
          "id": "simplemobheads:bee_head",
          "required": true
        },
        {
          "id": "simplemobheads:camel_head",
          "required": true
        },
        {
          "id": "simplemobheads:cat_head",
          "required": true
        },
        {
          "id": "simplemobheads:ocelot_head",
          "required": true
        },
        {
          "id": "simplemobheads:copper_golem_head",
          "required": true
        },
        {
          "id": "simplemobheads:dolphin_head",
          "required": true
        },
        {
          "id": "simplemobheads:donkey_head",
          "required": true
        },
        {
          "id": "simplemobheads:mule_head",
          "required": true
        },
        {
          "id": "simplemobheads:horse_head",
          "required": true
        },
        {
          "id": "simplemobheads:skeleton_horse_skull",
          "required": true
        },
        {
          "id": "simplemobheads:zombie_horse_head",
          "required": true
        },
        {
          "id": "simplemobheads:fox_head",
          "required": true
        },
        {
          "id": "simplemobheads:frog_head",
          "required": true
        },
        {
          "id": "simplemobheads:goat_head",
          "required": true
        },
        {
          "id": "simplemobheads:iron_golem_head",
          "required": true
        },
        {
          "id": "simplemobheads:snow_golem_head",
          "required": true
        },
        {
          "id": "simplemobheads:llama_head",
          "required": true
        },
        {
          "id": "simplemobheads:trader_llama_head",
          "required": true
        },
        {
          "id": "simplemobheads:mooshroom_head",
          "required": true
        },
        {
          "id": "simplemobheads:nautilus_head",
          "required": true
        },
        {
          "id": "simplemobheads:panda_head",
          "required": true
        },
        {
          "id": "simplemobheads:parrot_head",
          "required": true
        },
        {
          "id": "simplemobheads:polar_bear_head",
          "required": true
        },
        {
          "id": "simplemobheads:rabbit_head",
          "required": true
        },
        {
          "id": "simplemobheads:sniffer_head",
          "required": true
        },
        {
          "id": "simplemobheads:turtle_head",
          "required": true
        },
        {
          "id": "simplemobheads:villager_head",
          "required": true
        },
        {
          "id": "simplemobheads:wandering_trader_head",
          "required": true
        },
        {
          "id": "simplemobheads:wolf_head",
          "required": true
        },
        {
          "id": "simplemobheads:happy_ghast_head",
          "required": true
        },
        {
          "id": "simplemobheads:cod_head",
          "required": true
        },
        {
          "id": "simplemobheads:salmon_head",
          "required": true
        },
        {
          "id": "simplemobheads:pufferfish_head",
          "required": true
        },
        {
          "id": "simplemobheads:tropical_fish_head",
          "required": true
        },
        {
          "id": "simplemobheads:squid_head",
          "required": true
        },
        {
          "id": "simplemobheads:glow_squid_head",
          "required": true
        },
        {
          "id": "simplemobheads:tadpole_head",
          "required": true
        },
        {
          "id": "simplemobheads:deceiver_head",
          "required": true
        },
        {
          "id": "simplemobheads:hollow_howler_skull",
          "required": true
        },
        {
          "id": "simplemobheads:dread_head",
          "required": true
        },
        {
          "id": "simplemobheads:hollow_watcher_head",
          "required": true
        },
        {
          "id": "simplemobheads:shellker_head",
          "required": true
        },
        {
          "id": "simplemobheads:soulfire_blaze_head",
          "required": true
        },
        {
          "id": "simplemobheads:blighted_wither_head",
          "required": true
        },
        {
          "id": "simplemobheads:abyssal_wither_head",
          "required": true
        },
        {
          "id": "simplemobheads:nibblemite_head",
          "required": true
        },
        {
          "id": "simplemobheads:dragonmite_head",
          "required": true
        },
        {
          "id": "simplemobheads:herobrine_head",
          "required": true
        },
        {
          "id": "simplemobheads:crowned_pig_head",
          "required": true
        },
        {
          "id": "simplemobheads:giant_head",
          "required": true
        },
        {
          "id": "simplemobheads:giant_cow_head",
          "required": true
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/tags/item/heads.json"
    },
    {
      "id": "simplemobheads:item/trial_chamber_heads",
      "replace": false,
      "values": [
        {
          "id": "minecraft:zombie_head",
          "required": true
        },
        {
          "id": "minecraft:skeleton_skull",
          "required": true
        },
        {
          "id": "simplemobheads:husk_head",
          "required": true
        },
        {
          "id": "simplemobheads:spider_head",
          "required": true
        },
        {
          "id": "simplemobheads:cave_spider_head",
          "required": true
        },
        {
          "id": "simplemobheads:stray_skull",
          "required": true
        },
        {
          "id": "simplemobheads:bogged_skull",
          "required": true
        },
        {
          "id": "simplemobheads:slime_head",
          "required": true
        },
        {
          "id": "simplemobheads:silverfish_head",
          "required": true
        },
        {
          "id": "simplemobheads:breeze_head",
          "required": true
        }
      ],
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/tags/item/trial_chamber_heads.json"
    },
    {
      "id": "simplemobheads:worldgen/biome/killer_bunny_spawns",
      "replace": false,
      "values": [
        {
          "id": "minecraft:flower_forest",
          "required": true
        },
        {
          "id": "minecraft:meadow",
          "required": true
        }
      ],
      "source": "modules/simplemobheads/shared/resources/data/simplemobheads/tags/worldgen/biome/killer_bunny_spawns.json"
    }
  ],
  "advancements": [
    {
      "id": "simplemobheads:heads/abyssal_wither",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:abyssal_wither_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Abyssal Wither"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:abyssal_wither_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/abyssal_wither.json"
    },
    {
      "id": "simplemobheads:heads/allay",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:allay_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Allay"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:allay_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/allay.json"
    },
    {
      "id": "simplemobheads:heads/armadillo",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:armadillo_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Armadillo"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:armadillo_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/armadillo.json"
    },
    {
      "id": "simplemobheads:heads/axolotl",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:axolotl_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Axolotl"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:axolotl_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/axolotl.json"
    },
    {
      "id": "simplemobheads:heads/bat",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:bat_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Bat"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:bat_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/bat.json"
    },
    {
      "id": "simplemobheads:heads/bee",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:bee_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Bee"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:bee_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/bee.json"
    },
    {
      "id": "simplemobheads:heads/blaze",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:blaze_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Blaze"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:blaze_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/blaze.json"
    },
    {
      "id": "simplemobheads:heads/blighted_wither",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:blighted_wither_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Blighted Wither"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:blighted_wither_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/blighted_wither.json"
    },
    {
      "id": "simplemobheads:heads/bogged",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:bogged_skull",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Bogged"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:bogged_skull"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/bogged.json"
    },
    {
      "id": "simplemobheads:heads/breeze",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:breeze_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Breeze"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:breeze_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/breeze.json"
    },
    {
      "id": "simplemobheads:heads/camel",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:camel_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Camel"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:camel_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/camel.json"
    },
    {
      "id": "simplemobheads:heads/camel_husk",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:camel_husk_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Camel Husk"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:camel_husk_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/camel_husk.json"
    },
    {
      "id": "simplemobheads:heads/cat",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:cat_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Cat"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:cat_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/cat.json"
    },
    {
      "id": "simplemobheads:heads/cave_spider",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:cave_spider_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Cave Spider"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:cave_spider_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/cave_spider.json"
    },
    {
      "id": "simplemobheads:heads/chicken",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:chicken_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Chicken"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:chicken_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/chicken.json"
    },
    {
      "id": "simplemobheads:heads/cod",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:cod_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Cod"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:cod_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/cod.json"
    },
    {
      "id": "simplemobheads:heads/copper_golem",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:copper_golem_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Copper Golem"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:copper_golem_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/copper_golem.json"
    },
    {
      "id": "simplemobheads:heads/cow",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:cow_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Cow"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:cow_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/cow.json"
    },
    {
      "id": "simplemobheads:heads/creaking",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:creaking_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Creaking"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:creaking_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/creaking.json"
    },
    {
      "id": "simplemobheads:heads/crowned_pig",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:crowned_pig_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Never Dies",
        "de_de": "Stirbt nie"
      },
      "description": {
        "en_us": "Get the crown of a pig that never dies",
        "de_de": "Erhalte die Krone eines Schweins, das nie stirbt"
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:crowned_pig_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/crowned_pig.json"
    },
    {
      "id": "simplemobheads:heads/deceiver",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:deceiver_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Deceiver"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:deceiver_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/deceiver.json"
    },
    {
      "id": "simplemobheads:heads/dolphin",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:dolphin_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Dolphin"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:dolphin_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/dolphin.json"
    },
    {
      "id": "simplemobheads:heads/donkey",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:donkey_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Donkey"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:donkey_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/donkey.json"
    },
    {
      "id": "simplemobheads:heads/dragonmite",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:dragonmite_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Dragonmite"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:dragonmite_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/dragonmite.json"
    },
    {
      "id": "simplemobheads:heads/dread",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:dread_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Dread"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:dread_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/dread.json"
    },
    {
      "id": "simplemobheads:heads/drowned",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:drowned_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Drowned"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:drowned_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/drowned.json"
    },
    {
      "id": "simplemobheads:heads/elder_guardian",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:elder_guardian_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Elder Guardian"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:elder_guardian_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/elder_guardian.json"
    },
    {
      "id": "simplemobheads:heads/enderman",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:enderman_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Enderman"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:enderman_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/enderman.json"
    },
    {
      "id": "simplemobheads:heads/endermite",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:endermite_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Endermite"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:endermite_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/endermite.json"
    },
    {
      "id": "simplemobheads:heads/evoker",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:evoker_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Evoker"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:evoker_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/evoker.json"
    },
    {
      "id": "simplemobheads:heads/fox",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:fox_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Fox"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:fox_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/fox.json"
    },
    {
      "id": "simplemobheads:heads/frog",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:frog_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Frog"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:frog_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/frog.json"
    },
    {
      "id": "simplemobheads:heads/ghast",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:ghast_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Ghast"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:ghast_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/ghast.json"
    },
    {
      "id": "simplemobheads:heads/giant",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:giant_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Big Trouble",
        "de_de": "Riesenärger"
      },
      "description": {
        "en_us": "Bring down a giant with a charged creeper",
        "de_de": "Bring einen Riesen mit einem geladenen Creeper zu Fall"
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:giant_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/giant.json"
    },
    {
      "id": "simplemobheads:heads/giant_cow",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:giant_cow_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Moo-ve Over",
        "de_de": "Mach Platz, Muh!"
      },
      "description": {
        "en_us": "Get the head of a giant cow",
        "de_de": "Erhalte den Kopf einer Riesen-Kuh"
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:giant_cow_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/giant_cow.json"
    },
    {
      "id": "simplemobheads:heads/glow_squid",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:glow_squid_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Glow Squid"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:glow_squid_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/glow_squid.json"
    },
    {
      "id": "simplemobheads:heads/goat",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:goat_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Goat"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:goat_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/goat.json"
    },
    {
      "id": "simplemobheads:heads/guardian",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:guardian_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Guardian"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:guardian_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/guardian.json"
    },
    {
      "id": "simplemobheads:heads/happy_ghast",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:happy_ghast_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Happy Ghast"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:happy_ghast_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/happy_ghast.json"
    },
    {
      "id": "simplemobheads:heads/herobrine",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:herobrine_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "He Was Never Removed",
        "de_de": "Er wurde nie entfernt"
      },
      "description": {
        "en_us": "Light the old shrine at night",
        "de_de": "Entzünde nachts den alten Schrein"
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:herobrine_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/herobrine.json"
    },
    {
      "id": "simplemobheads:heads/hoglin",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:hoglin_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Hoglin"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:hoglin_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/hoglin.json"
    },
    {
      "id": "simplemobheads:heads/hollow_howler",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:hollow_howler_skull",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Hollow Howler"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:hollow_howler_skull"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/hollow_howler.json"
    },
    {
      "id": "simplemobheads:heads/hollow_watcher",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:hollow_watcher_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Hollow Watcher"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:hollow_watcher_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/hollow_watcher.json"
    },
    {
      "id": "simplemobheads:heads/horse",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:horse_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Horse"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:horse_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/horse.json"
    },
    {
      "id": "simplemobheads:heads/husk",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:husk_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Husk"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:husk_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/husk.json"
    },
    {
      "id": "simplemobheads:heads/illusioner",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:illusioner_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Illusioner"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:illusioner_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/illusioner.json"
    },
    {
      "id": "simplemobheads:heads/iron_golem",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:iron_golem_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Iron Golem"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:iron_golem_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/iron_golem.json"
    },
    {
      "id": "simplemobheads:heads/killer_bunny",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:rabbit_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Run Away!",
        "de_de": "Lauft weg!"
      },
      "description": {
        "en_us": "Get the head of a killer bunny",
        "de_de": "Erhalte den Kopf eines Killerkaninchens"
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:rabbit_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/shared/resources/data/simplemobheads/advancement/heads/killer_bunny.json"
    },
    {
      "id": "simplemobheads:heads/llama",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:llama_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Llama"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:llama_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/llama.json"
    },
    {
      "id": "simplemobheads:heads/magma_cube",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:magma_cube_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Magma Cube"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:magma_cube_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/magma_cube.json"
    },
    {
      "id": "simplemobheads:heads/mooshroom",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:mooshroom_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Mooshroom"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:mooshroom_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/mooshroom.json"
    },
    {
      "id": "simplemobheads:heads/mule",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:mule_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Mule"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:mule_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/mule.json"
    },
    {
      "id": "simplemobheads:heads/nautilus",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:nautilus_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Nautilus"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:nautilus_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/nautilus.json"
    },
    {
      "id": "simplemobheads:heads/nibblemite",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:nibblemite_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Nibblemite"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:nibblemite_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/nibblemite.json"
    },
    {
      "id": "simplemobheads:heads/ocelot",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:ocelot_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Ocelot"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:ocelot_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/ocelot.json"
    },
    {
      "id": "simplemobheads:heads/panda",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:panda_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Panda"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:panda_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/panda.json"
    },
    {
      "id": "simplemobheads:heads/parched",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:parched_skull",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Parched"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:parched_skull"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/parched.json"
    },
    {
      "id": "simplemobheads:heads/parrot",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:parrot_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Parrot"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:parrot_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/parrot.json"
    },
    {
      "id": "simplemobheads:heads/phantom",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:phantom_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Phantom"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:phantom_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/phantom.json"
    },
    {
      "id": "simplemobheads:heads/pig",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:pig_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Pig"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:pig_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/pig.json"
    },
    {
      "id": "simplemobheads:heads/piglin_brute",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:piglin_brute_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Piglin Brute"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:piglin_brute_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/piglin_brute.json"
    },
    {
      "id": "simplemobheads:heads/pillager",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:pillager_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Pillager"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:pillager_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/pillager.json"
    },
    {
      "id": "simplemobheads:heads/polar_bear",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:polar_bear_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Polar Bear"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:polar_bear_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/polar_bear.json"
    },
    {
      "id": "simplemobheads:heads/pufferfish",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:pufferfish_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Pufferfish"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:pufferfish_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/pufferfish.json"
    },
    {
      "id": "simplemobheads:heads/rabbit",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:rabbit_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Rabbit"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:rabbit_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/rabbit.json"
    },
    {
      "id": "simplemobheads:heads/ravager",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:ravager_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Ravager"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:ravager_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/ravager.json"
    },
    {
      "id": "simplemobheads:heads/root",
      "parent": null,
      "icon": "simplemobheads:blaze_head",
      "frame": "task",
      "hidden": false,
      "title": {
        "en_us": "Mob Heads",
        "de_de": "Mob-Köpfe"
      },
      "description": {
        "en_us": "Let a charged creeper blow up a mob: it drops its head",
        "de_de": "Lass einen geladenen Creeper einen Mob sprengen: er lässt seinen Kopf fallen"
      },
      "criteria": [
        {
          "name": "any_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "#simplemobheads:heads"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/root.json"
    },
    {
      "id": "simplemobheads:heads/salmon",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:salmon_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Salmon"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:salmon_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/salmon.json"
    },
    {
      "id": "simplemobheads:heads/sheep",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:sheep_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Sheep"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:sheep_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/sheep.json"
    },
    {
      "id": "simplemobheads:heads/shellker",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:shellker_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Shellker"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:shellker_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/shellker.json"
    },
    {
      "id": "simplemobheads:heads/shulker",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:shulker_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Shulker"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:shulker_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/shulker.json"
    },
    {
      "id": "simplemobheads:heads/silverfish",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:silverfish_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Silverfish"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:silverfish_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/silverfish.json"
    },
    {
      "id": "simplemobheads:heads/skeleton_horse",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:skeleton_horse_skull",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Skeleton Horse"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:skeleton_horse_skull"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/skeleton_horse.json"
    },
    {
      "id": "simplemobheads:heads/slime",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:slime_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Slime"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:slime_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/slime.json"
    },
    {
      "id": "simplemobheads:heads/sniffer",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:sniffer_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Sniffer"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:sniffer_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/sniffer.json"
    },
    {
      "id": "simplemobheads:heads/snow_golem",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:snow_golem_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Snow Golem"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:snow_golem_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/snow_golem.json"
    },
    {
      "id": "simplemobheads:heads/soulfire_blaze",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:soulfire_blaze_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Soulfire Blaze"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:soulfire_blaze_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/soulfire_blaze.json"
    },
    {
      "id": "simplemobheads:heads/spider",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:spider_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Spider"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:spider_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/spider.json"
    },
    {
      "id": "simplemobheads:heads/squid",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:squid_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Squid"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:squid_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/squid.json"
    },
    {
      "id": "simplemobheads:heads/stray",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:stray_skull",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Stray"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:stray_skull"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/stray.json"
    },
    {
      "id": "simplemobheads:heads/strider",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:strider_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Strider"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:strider_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/strider.json"
    },
    {
      "id": "simplemobheads:heads/sulfur_cube",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:sulfur_cube_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Sulfur Cube"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:sulfur_cube_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/sulfur_cube.json"
    },
    {
      "id": "simplemobheads:heads/tadpole",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:tadpole_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Tadpole"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:tadpole_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/tadpole.json"
    },
    {
      "id": "simplemobheads:heads/trader_llama",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:trader_llama_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Trader Llama"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:trader_llama_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/trader_llama.json"
    },
    {
      "id": "simplemobheads:heads/tropical_fish",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:tropical_fish_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Tropical Fish"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:tropical_fish_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/tropical_fish.json"
    },
    {
      "id": "simplemobheads:heads/turtle",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:turtle_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Turtle"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:turtle_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/turtle.json"
    },
    {
      "id": "simplemobheads:heads/vex",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:vex_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Vex"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:vex_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/vex.json"
    },
    {
      "id": "simplemobheads:heads/villager",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:villager_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Villager"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:villager_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/villager.json"
    },
    {
      "id": "simplemobheads:heads/vindicator",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:vindicator_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Vindicator"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:vindicator_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/vindicator.json"
    },
    {
      "id": "simplemobheads:heads/wandering_trader",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:wandering_trader_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Wandering Trader"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:wandering_trader_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/wandering_trader.json"
    },
    {
      "id": "simplemobheads:heads/warden",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:warden_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Warden"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:warden_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/warden.json"
    },
    {
      "id": "simplemobheads:heads/witch",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:witch_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Witch"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:witch_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/witch.json"
    },
    {
      "id": "simplemobheads:heads/wither",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:wither_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Wither"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:wither_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/wither.json"
    },
    {
      "id": "simplemobheads:heads/wolf",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:wolf_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Wolf"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:wolf_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/wolf.json"
    },
    {
      "id": "simplemobheads:heads/zoglin",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:zoglin_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Zoglin"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:zoglin_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/zoglin.json"
    },
    {
      "id": "simplemobheads:heads/zombie_horse",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:zombie_horse_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Zombie Horse"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:zombie_horse_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/zombie_horse.json"
    },
    {
      "id": "simplemobheads:heads/zombie_nautilus",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:zombie_nautilus_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Zombie Nautilus"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:zombie_nautilus_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/zombie_nautilus.json"
    },
    {
      "id": "simplemobheads:heads/zombie_villager",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:zombie_villager_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Zombie Villager"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:zombie_villager_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/zombie_villager.json"
    },
    {
      "id": "simplemobheads:heads/zombified_piglin",
      "parent": "simplemobheads:heads/root",
      "icon": "simplemobheads:zombified_piglin_head",
      "frame": "task",
      "hidden": true,
      "title": {
        "en_us": "Zombified Piglin"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "has_head",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "simplemobheads:zombified_piglin_head"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simplemobheads/generated/resources/data/simplemobheads/advancement/heads/zombified_piglin.json"
    }
  ],
  "enchantments": [],
  "items": [
    {
      "id": "simplemobheads:guide_book",
      "name": {
        "en_us": "Simple Mobheads Guide",
        "de_de": "Simple-Mobheads-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simplemobheads/shared/java/com/simplemobheads/guide/MobHeadsGuide.java",
          "modules/simplemobheads/shared/resources/data/simplemobheads/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 17 pages taken from this wiki, shown in your language. Shapeless recipe: book + skeleton skull. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Mobheads\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 17 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Skelettschädel. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Mobheads\" eins."
        }
      },
      "texture": "assets/textures/simplemobheads/item/guide_book.png",
      "questionary": {
        "en": {
          "lore": [
            "A book about every mob's head."
          ],
          "use": [
            "The Simple Mobheads Guide explains drops, variants and head abilities. Craft it shapeless from a book and a skeleton skull."
          ],
          "secret": [
            "Its pages follow the wiki, so it never tells more than the heads really do."
          ]
        },
        "de": {
          "lore": [
            "Ein Buch über den Kopf jedes Mobs."
          ],
          "use": [
            "Das Simple-Mobheads-Handbuch erklärt Beute, Varianten und Kopf-Fähigkeiten. Formlos aus einem Buch und einem Skelettschädel."
          ],
          "secret": [
            "Seine Seiten folgen dem Wiki und verraten nie mehr, als die Köpfe wirklich tun."
          ]
        }
      },
      "craftedBy": [
        "simplemobheads:guide_book"
      ],
      "usedIn": []
    }
  ],
  "blocks": [
    {
      "id": "simplemobheads:abyssal_wither_head",
      "name": {
        "en_us": "Abyssal Wither Head",
        "de_de": "Abgrund-Wither-Kopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a abyssal wither."
          ],
          "use": [
            "Only with the server switch for boss heads: dropped when a charged creeper's explosion kills it. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Nur mit dem Server-Schalter für Boss-Köpfe: fällt, wenn ein geladener Creeper ihn sprengt. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:abyssal_wither_wall_head",
      "name": {
        "en_us": "Abyssal Wither Head",
        "de_de": "Abgrund-Wither-Kopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a abyssal wither."
          ],
          "use": [
            "Only with the server switch for boss heads: dropped when a charged creeper's explosion kills it. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Nur mit dem Server-Schalter für Boss-Köpfe: fällt, wenn ein geladener Creeper ihn sprengt. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:allay_head",
      "name": {
        "en_us": "Allay Head",
        "de_de": "Hilfsgeistkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a allay."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Dropped items near its wearer float up a little."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Liegende Items in der Nähe des Trägers schweben leicht."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:allay_wall_head",
      "name": {
        "en_us": "Allay Head",
        "de_de": "Hilfsgeistkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a allay."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Dropped items near its wearer float up a little."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Liegende Items in der Nähe des Trägers schweben leicht."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:armadillo_head",
      "name": {
        "en_us": "Armadillo Head",
        "de_de": "Gürteltierkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a armadillo."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:armadillo_wall_head",
      "name": {
        "en_us": "Armadillo Head",
        "de_de": "Gürteltierkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a armadillo."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:axolotl_head",
      "name": {
        "en_us": "Axolotl Head",
        "de_de": "Axolotlkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a axolotl."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:axolotl_wall_head",
      "name": {
        "en_us": "Axolotl Head",
        "de_de": "Axolotlkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a axolotl."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:bat_head",
      "name": {
        "en_us": "Bat Head",
        "de_de": "Fledermauskopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a bat."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:bat_wall_head",
      "name": {
        "en_us": "Bat Head",
        "de_de": "Fledermauskopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a bat."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:bee_head",
      "name": {
        "en_us": "Bee Head",
        "de_de": "Bienenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a bee."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Harvesting a hive or nest does not anger the bees at its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Ernten von Stock oder Nest werden die Bienen nicht wütend auf den Träger."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:bee_wall_head",
      "name": {
        "en_us": "Bee Head",
        "de_de": "Bienenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a bee."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Harvesting a hive or nest does not anger the bees at its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Ernten von Stock oder Nest werden die Bienen nicht wütend auf den Träger."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:blaze_head",
      "name": {
        "en_us": "Blaze Head",
        "de_de": "Lohenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a blaze."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Magma blocks and campfires do not burn its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Magmablöcke und Lagerfeuer verbrennen den Träger nicht."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:blaze_wall_head",
      "name": {
        "en_us": "Blaze Head",
        "de_de": "Lohenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a blaze."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Magma blocks and campfires do not burn its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Magmablöcke und Lagerfeuer verbrennen den Träger nicht."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:blighted_wither_head",
      "name": {
        "en_us": "Blighted Wither Head",
        "de_de": "Verderbter-Wither-Kopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a blighted wither."
          ],
          "use": [
            "Only with the server switch for boss heads: dropped when a charged creeper's explosion kills it. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Nur mit dem Server-Schalter für Boss-Köpfe: fällt, wenn ein geladener Creeper ihn sprengt. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:blighted_wither_wall_head",
      "name": {
        "en_us": "Blighted Wither Head",
        "de_de": "Verderbter-Wither-Kopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a blighted wither."
          ],
          "use": [
            "Only with the server switch for boss heads: dropped when a charged creeper's explosion kills it. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Nur mit dem Server-Schalter für Boss-Köpfe: fällt, wenn ein geladener Creeper ihn sprengt. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:bogged_skull",
      "name": {
        "en_us": "Bogged Skull",
        "de_de": "Sumpfskelettschädel"
      },
      "note": {
        "en": {
          "summary": "A mob skull: only a charged creeper's explosion killing the mob drops it. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Schädel: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a bogged."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Food never poisons its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Essen vergiftet den Träger nie."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:bogged_wall_skull",
      "name": {
        "en_us": "Bogged Skull",
        "de_de": "Sumpfskelettschädel"
      },
      "note": {
        "en": {
          "summary": "The wall form of the skull (placed against a wall); drops and names like the standing skull."
        },
        "de": {
          "summary": "Die Wandform des Schädels (an eine Wand gesetzt); Beute und Name wie beim stehenden Schädel."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a bogged."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Food never poisons its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Essen vergiftet den Träger nie."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:breeze_head",
      "name": {
        "en_us": "Breeze Head",
        "de_de": "Breeze-Kopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a breeze."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer, light as the wind, never tramples farmland."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Leicht wie der Wind: der Träger zertrampelt kein Ackerland."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:breeze_wall_head",
      "name": {
        "en_us": "Breeze Head",
        "de_de": "Breeze-Kopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a breeze."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer, light as the wind, never tramples farmland."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Leicht wie der Wind: der Träger zertrampelt kein Ackerland."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:camel_head",
      "name": {
        "en_us": "Camel Head",
        "de_de": "Kamelkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a camel."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:camel_husk_head",
      "name": {
        "en_us": "Camel Husk Head",
        "de_de": "Kamelhülsenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a camel husk."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:camel_husk_wall_head",
      "name": {
        "en_us": "Camel Husk Head",
        "de_de": "Kamelhülsenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a camel husk."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:camel_wall_head",
      "name": {
        "en_us": "Camel Head",
        "de_de": "Kamelkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a camel."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:cat_head",
      "name": {
        "en_us": "Cat Head",
        "de_de": "Katzenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a cat."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Creepers keep their distance from its wearer, as from a cat."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Creeper halten Abstand zum Träger, wie zu einer Katze."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:cat_wall_head",
      "name": {
        "en_us": "Cat Head",
        "de_de": "Katzenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a cat."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Creepers keep their distance from its wearer, as from a cat."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Creeper halten Abstand zum Träger, wie zu einer Katze."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:cave_spider_head",
      "name": {
        "en_us": "Cave Spider Head",
        "de_de": "Höhlenspinnenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a cave spider."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Bare hands cut cobwebs as fast as a sword."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Die bloße Hand schneidet Spinnennetze so schnell wie ein Schwert."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:cave_spider_wall_head",
      "name": {
        "en_us": "Cave Spider Head",
        "de_de": "Höhlenspinnenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a cave spider."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Bare hands cut cobwebs as fast as a sword."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Die bloße Hand schneidet Spinnennetze so schnell wie ein Schwert."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:chicken_head",
      "name": {
        "en_us": "Chicken Head",
        "de_de": "Hühnerkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a chicken."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer lays an egg now and then, like a chicken."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger legt ab und zu ein Ei, wie ein Huhn."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:chicken_wall_head",
      "name": {
        "en_us": "Chicken Head",
        "de_de": "Hühnerkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a chicken."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer lays an egg now and then, like a chicken."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger legt ab und zu ein Ei, wie ein Huhn."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:cod_head",
      "name": {
        "en_us": "Cod Head",
        "de_de": "Kabeljaukopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a cod."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:cod_wall_head",
      "name": {
        "en_us": "Cod Head",
        "de_de": "Kabeljaukopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a cod."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:copper_golem_head",
      "name": {
        "en_us": "Copper Golem Head",
        "de_de": "Kupfergolemkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a copper golem."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:copper_golem_wall_head",
      "name": {
        "en_us": "Copper Golem Head",
        "de_de": "Kupfergolemkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a copper golem."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:cow_head",
      "name": {
        "en_us": "Cow Head",
        "de_de": "Kuhkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a cow."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Another player can milk its wearer with a bucket."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Ein anderer Spieler kann den Träger mit einem Eimer melken."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:cow_wall_head",
      "name": {
        "en_us": "Cow Head",
        "de_de": "Kuhkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a cow."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Another player can milk its wearer with a bucket."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Ein anderer Spieler kann den Träger mit einem Eimer melken."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:creaking_head",
      "name": {
        "en_us": "Creaking Head",
        "de_de": "Knarzerkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a creaking."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:creaking_wall_head",
      "name": {
        "en_us": "Creaking Head",
        "de_de": "Knarzerkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a creaking."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:crowned_pig_head",
      "name": {
        "en_us": "Crowned Pig Head",
        "de_de": "Gekrönter Schweinekopf"
      },
      "note": {
        "spoiler": true,
        "en": {
          "summary": "A tribute head that no mob drops (see the guide \"Technoblade Never Dies\").",
          "details": [
            "A pig named Technoblade wears it as a crown and never dies while it does; a player's deadly hit makes the crown fall, once per pig and player per in-game day.",
            "Worn, pigs nearby turn their heads to the wearer. Place it, wear it or put it on a note block."
          ]
        },
        "de": {
          "summary": "Ein Tribut-Kopf, den kein Mob fallen lässt (siehe Anleitung \"Technoblade never dies\").",
          "details": [
            "Ein Schwein namens Technoblade trägt ihn als Krone und stirbt damit nie; der tödliche Schlag eines Spielers lässt die Krone fallen, einmal je Schwein und Spieler pro Ingame-Tag.",
            "Getragen drehen Schweine in der Nähe den Kopf zum Träger. Aufstellen, tragen oder auf einen Notenblock setzen."
          ]
        },
        "sources": [
          "modules/simplemobheads/shared/java/com/simplemobheads/easter/Technoblade.java"
        ]
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: Technoblade never dies."
          ],
          "use": [
            "No mob drops it. A pig with a certain name never dies - strike it down, and its crown falls instead; the title screen remembers him. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Pigs nearby turn their heads to its wearer, and the crown glints."
          ]
        },
        "de": {
          "lore": [
            "{name}: Technoblade never dies."
          ],
          "use": [
            "Kein Mob lässt ihn fallen. Ein Schwein mit einem bestimmten Namen stirbt nie - schlägst du es nieder, fällt stattdessen seine Krone; der Startbildschirm erinnert an ihn. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Schweine in der Nähe drehen den Kopf zum Träger, und die Krone glänzt."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:crowned_pig_wall_head",
      "name": {
        "en_us": "Crowned Pig Head",
        "de_de": "Gekrönter Schweinekopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: Technoblade never dies."
          ],
          "use": [
            "No mob drops it. A pig with a certain name never dies - strike it down, and its crown falls instead; the title screen remembers him. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Pigs nearby turn their heads to its wearer, and the crown glints."
          ]
        },
        "de": {
          "lore": [
            "{name}: Technoblade never dies."
          ],
          "use": [
            "Kein Mob lässt ihn fallen. Ein Schwein mit einem bestimmten Namen stirbt nie - schlägst du es nieder, fällt stattdessen seine Krone; der Startbildschirm erinnert an ihn. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Schweine in der Nähe drehen den Kopf zum Träger, und die Krone glänzt."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:deceiver_head",
      "name": {
        "en_us": "Deceiver Head",
        "de_de": "Täuscherkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a deceiver."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:deceiver_wall_head",
      "name": {
        "en_us": "Deceiver Head",
        "de_de": "Täuscherkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a deceiver."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:dolphin_head",
      "name": {
        "en_us": "Dolphin Head",
        "de_de": "Delfinkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a dolphin."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:dolphin_wall_head",
      "name": {
        "en_us": "Dolphin Head",
        "de_de": "Delfinkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a dolphin."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:donkey_head",
      "name": {
        "en_us": "Donkey Head",
        "de_de": "Eselkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a donkey."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:donkey_wall_head",
      "name": {
        "en_us": "Donkey Head",
        "de_de": "Eselkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a donkey."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:dragonmite_head",
      "name": {
        "en_us": "Dragonmite Head",
        "de_de": "Drachenmilbenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a dragonmite."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:dragonmite_wall_head",
      "name": {
        "en_us": "Dragonmite Head",
        "de_de": "Drachenmilbenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a dragonmite."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:dread_head",
      "name": {
        "en_us": "Dread Head",
        "de_de": "Schreckenskopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a dread."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:dread_wall_head",
      "name": {
        "en_us": "Dread Head",
        "de_de": "Schreckenskopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a dread."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:drowned_head",
      "name": {
        "en_us": "Drowned Head",
        "de_de": "Ertrunkenenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a drowned."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Magma bubble columns do not drag its wearer down."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Magma-Blasensäulen ziehen den Träger nicht nach unten."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:drowned_wall_head",
      "name": {
        "en_us": "Drowned Head",
        "de_de": "Ertrunkenenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a drowned."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Magma bubble columns do not drag its wearer down."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Magma-Blasensäulen ziehen den Träger nicht nach unten."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:elder_guardian_head",
      "name": {
        "en_us": "Elder Guardian Head",
        "de_de": "Großer-Wächter-Kopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a elder guardian."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:elder_guardian_wall_head",
      "name": {
        "en_us": "Elder Guardian Head",
        "de_de": "Großer-Wächter-Kopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a elder guardian."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:enderman_head",
      "name": {
        "en_us": "Enderman Head",
        "de_de": "Endermankopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a enderman."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Ender pearl teleports cost its wearer no health."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Enderperlen-Teleports kosten den Träger keine Herzen."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:enderman_wall_head",
      "name": {
        "en_us": "Enderman Head",
        "de_de": "Endermankopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a enderman."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Ender pearl teleports cost its wearer no health."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Enderperlen-Teleports kosten den Träger keine Herzen."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:endermite_head",
      "name": {
        "en_us": "Endermite Head",
        "de_de": "Endermitenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a endermite."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:endermite_wall_head",
      "name": {
        "en_us": "Endermite Head",
        "de_de": "Endermitenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a endermite."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:evoker_head",
      "name": {
        "en_us": "Evoker Head",
        "de_de": "Magierkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a evoker."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:evoker_wall_head",
      "name": {
        "en_us": "Evoker Head",
        "de_de": "Magierkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a evoker."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:fox_head",
      "name": {
        "en_us": "Fox Head",
        "de_de": "Fuchskopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a fox."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sweet berry bushes do not prick its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Süßbeerensträucher stechen den Träger nicht."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:fox_wall_head",
      "name": {
        "en_us": "Fox Head",
        "de_de": "Fuchskopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a fox."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sweet berry bushes do not prick its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Süßbeerensträucher stechen den Träger nicht."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:frog_head",
      "name": {
        "en_us": "Frog Head",
        "de_de": "Froschkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a frog."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer jumps higher from lily pads and out of water."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger springt von Seerosen und aus dem Wasser höher."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:frog_wall_head",
      "name": {
        "en_us": "Frog Head",
        "de_de": "Froschkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a frog."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer jumps higher from lily pads and out of water."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger springt von Seerosen und aus dem Wasser höher."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:ghast_head",
      "name": {
        "en_us": "Ghast Head",
        "de_de": "Ghastkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a ghast."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:ghast_wall_head",
      "name": {
        "en_us": "Ghast Head",
        "de_de": "Ghastkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a ghast."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:giant_cow_head",
      "name": {
        "en_us": "Giant Cow Head",
        "de_de": "Riesenkuhkopf"
      },
      "note": {
        "spoiler": true,
        "en": {
          "summary": "The head of a giant cow (see the guide \"Giants\"): only a charged creeper's explosion drops it, with the cow's variant.",
          "details": [
            "A cow fed hay bales may very rarely grow into a giant cow. Placed one and a half blocks big.",
            "Worn: 1.5x size, whole-block steps, and others can milk the wearer."
          ]
        },
        "de": {
          "summary": "Der Kopf einer Riesen-Kuh (siehe Anleitung \"Riesen\"): nur die Explosion eines geladenen Creepers lässt ihn fallen, mit der Variante der Kuh.",
          "details": [
            "Eine mit Heuballen gefütterte Kuh wird sehr selten zur Riesen-Kuh. Aufgestellt anderthalb Blöcke groß.",
            "Getragen: 1,5-fache Größe, ganze Blöcke hoch, und andere können den Träger melken."
          ]
        },
        "sources": [
          "modules/simplemobheads/shared/java/com/simplemobheads/easter/Giants.java"
        ]
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: it really did eat all that hay."
          ],
          "use": [
            "No ordinary cow drops it. A cow fed hay bales, one by one, may - very rarely - grow into a giant; only a charged creeper brings its head down. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Its wearer grows to 1.5x size and steps up whole blocks; another player can milk it like a cow."
          ]
        },
        "de": {
          "lore": [
            "{name}: sie hat das ganze Heu wirklich gefressen."
          ],
          "use": [
            "Keine gewöhnliche Kuh lässt ihn fallen. Eine Kuh, die Heuballen frisst, einen nach dem anderen, wird - sehr selten - zur Riesin; nur ein geladener Creeper holt ihren Kopf. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger wächst auf 1,5-fache Größe und steigt ganze Blöcke hoch; ein anderer Spieler kann ihn wie eine Kuh melken."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:giant_cow_wall_head",
      "name": {
        "en_us": "Giant Cow Head",
        "de_de": "Riesenkuhkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: it really did eat all that hay."
          ],
          "use": [
            "No ordinary cow drops it. A cow fed hay bales, one by one, may - very rarely - grow into a giant; only a charged creeper brings its head down. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Its wearer grows to 1.5x size and steps up whole blocks; another player can milk it like a cow."
          ]
        },
        "de": {
          "lore": [
            "{name}: sie hat das ganze Heu wirklich gefressen."
          ],
          "use": [
            "Keine gewöhnliche Kuh lässt ihn fallen. Eine Kuh, die Heuballen frisst, einen nach dem anderen, wird - sehr selten - zur Riesin; nur ein geladener Creeper holt ihren Kopf. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger wächst auf 1,5-fache Größe und steigt ganze Blöcke hoch; ein anderer Spieler kann ihn wie eine Kuh melken."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:giant_head",
      "name": {
        "en_us": "Giant Head",
        "de_de": "Riesenkopf"
      },
      "note": {
        "spoiler": true,
        "en": {
          "summary": "The head of a giant (see the guide \"Giants\"): only a charged creeper's explosion drops it, with the giant's zombie kind.",
          "details": [
            "Lightning on a full-moon night makes a zombie a giant. Placed one and a half blocks big.",
            "Worn: 1.5x size, whole-block steps, a little faster, hidden from zombies, and the head of its kind."
          ]
        },
        "de": {
          "summary": "Der Kopf eines Riesen (siehe Anleitung \"Riesen\"): nur die Explosion eines geladenen Creepers lässt ihn fallen, mit der Zombie-Art des Riesen.",
          "details": [
            "Ein Blitz in einer Vollmondnacht macht aus einem Zombie einen Riesen. Aufgestellt anderthalb Blöcke groß.",
            "Getragen: 1,5-fache Größe, ganze Blöcke hoch, etwas schneller, vor Zombies getarnt und der Kopf seiner Art."
          ]
        },
        "sources": [
          "modules/simplemobheads/shared/java/com/simplemobheads/easter/Giants.java"
        ]
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: the biggest trouble a full moon ever brought."
          ],
          "use": [
            "No ordinary zombie drops it. When lightning strikes a zombie under a full moon, something big gets up - only a charged creeper brings its head down. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Its wearer grows to 1.5x size, steps up whole blocks and walks a little faster; zombies see it only half as far, and it works like the head of its zombie kind (husk, drowned)."
          ]
        },
        "de": {
          "lore": [
            "{name}: der größte Ärger, den ein Vollmond je brachte."
          ],
          "use": [
            "Kein gewöhnlicher Zombie lässt ihn fallen. Trifft bei Vollmond ein Blitz einen Zombie, steht etwas Großes auf - nur ein geladener Creeper holt seinen Kopf. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger wächst auf 1,5-fache Größe, steigt ganze Blöcke hoch und läuft etwas schneller; Zombies sehen ihn nur halb so weit, und er wirkt wie der Kopf seiner Zombie-Art (Wüstenzombie, Ertrunkener)."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:giant_wall_head",
      "name": {
        "en_us": "Giant Head",
        "de_de": "Riesenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: the biggest trouble a full moon ever brought."
          ],
          "use": [
            "No ordinary zombie drops it. When lightning strikes a zombie under a full moon, something big gets up - only a charged creeper brings its head down. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Its wearer grows to 1.5x size, steps up whole blocks and walks a little faster; zombies see it only half as far, and it works like the head of its zombie kind (husk, drowned)."
          ]
        },
        "de": {
          "lore": [
            "{name}: der größte Ärger, den ein Vollmond je brachte."
          ],
          "use": [
            "Kein gewöhnlicher Zombie lässt ihn fallen. Trifft bei Vollmond ein Blitz einen Zombie, steht etwas Großes auf - nur ein geladener Creeper holt seinen Kopf. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger wächst auf 1,5-fache Größe, steigt ganze Blöcke hoch und läuft etwas schneller; Zombies sehen ihn nur halb so weit, und er wirkt wie der Kopf seiner Zombie-Art (Wüstenzombie, Ertrunkener)."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:glow_squid_head",
      "name": {
        "en_us": "Glow Squid Head",
        "de_de": "Leuchttintenfischkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a glow squid."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:glow_squid_wall_head",
      "name": {
        "en_us": "Glow Squid Head",
        "de_de": "Leuchttintenfischkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a glow squid."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:goat_head",
      "name": {
        "en_us": "Goat Head",
        "de_de": "Ziegenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a goat."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "A ramming goat only pushes its wearer, without damage."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Eine rammende Ziege schubst den Träger nur, ohne Schaden."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:goat_wall_head",
      "name": {
        "en_us": "Goat Head",
        "de_de": "Ziegenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a goat."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "A ramming goat only pushes its wearer, without damage."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Eine rammende Ziege schubst den Träger nur, ohne Schaden."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:guardian_head",
      "name": {
        "en_us": "Guardian Head",
        "de_de": "Wächterkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a guardian."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:guardian_wall_head",
      "name": {
        "en_us": "Guardian Head",
        "de_de": "Wächterkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a guardian."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:happy_ghast_head",
      "name": {
        "en_us": "Happy Ghast Head",
        "de_de": "Glücklicher-Ghast-Kopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a happy ghast."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:happy_ghast_wall_head",
      "name": {
        "en_us": "Happy Ghast Head",
        "de_de": "Glücklicher-Ghast-Kopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a happy ghast."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:herobrine_head",
      "name": {
        "en_us": "Herobrine Head",
        "de_de": "Herobrine-Kopf"
      },
      "note": {
        "spoiler": true,
        "en": {
          "summary": "A secret head that no mob drops (see the guide \"He Was Never Removed\").",
          "details": [
            "Lit at night, an old shrine of 3x3 gold, a mossy cobblestone, four redstone torches and netherrack gives it once per shrine and player per cooldown (an in-game day by default, kept across restarts).",
            "Worn, torches nearby flicker and hostile mobs that target the wearer in the dark hesitate. Place it, wear it or put it on a note block."
          ]
        },
        "de": {
          "summary": "Ein geheimer Kopf, den kein Mob fallen lässt (siehe Anleitung \"Er wurde nie entfernt\").",
          "details": [
            "Ein alter Schrein aus 3×3 Gold, bemoostem Bruchstein, vier Redstone-Fackeln und Netherrack, nachts angezündet, gibt ihn einmal je Schrein und Spieler pro Abklingzeit (standardmäßig ein Ingame-Tag, übersteht Neustarts).",
            "Getragen flackern Fackeln in der Nähe, und im Dunkeln zögern feindliche Mobs, die den Träger angreifen wollen. Aufstellen, tragen oder auf einen Notenblock setzen."
          ]
        },
        "sources": [
          "modules/simplemobheads/shared/java/com/simplemobheads/ritual/HerobrineRitual.java",
          "modules/simplemobheads/shared/java/com/simplemobheads/ritual/RitualCooldowns.java"
        ]
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: he was never removed."
          ],
          "use": [
            "No mob drops it. Gold carries moss, moss carries netherrack, four red lights keep watch - light it at night. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Torches near its wearer flicker; in the dark, hostile mobs hesitate before they attack."
          ]
        },
        "de": {
          "lore": [
            "{name}: er wurde nie entfernt."
          ],
          "use": [
            "Kein Mob lässt ihn fallen. Gold trägt Moos, Moos trägt Netherrack, vier rote Lichter wachen - zünde es nachts an. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Fackeln beim Träger flackern; im Dunkeln zögern feindliche Mobs vor dem Angriff."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:herobrine_wall_head",
      "name": {
        "en_us": "Herobrine Head",
        "de_de": "Herobrine-Kopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: he was never removed."
          ],
          "use": [
            "No mob drops it. Gold carries moss, moss carries netherrack, four red lights keep watch - light it at night. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Torches near its wearer flicker; in the dark, hostile mobs hesitate before they attack."
          ]
        },
        "de": {
          "lore": [
            "{name}: er wurde nie entfernt."
          ],
          "use": [
            "Kein Mob lässt ihn fallen. Gold trägt Moos, Moos trägt Netherrack, vier rote Lichter wachen - zünde es nachts an. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Fackeln beim Träger flackern; im Dunkeln zögern feindliche Mobs vor dem Angriff."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:hoglin_head",
      "name": {
        "en_us": "Hoglin Head",
        "de_de": "Hoglinkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a hoglin."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:hoglin_wall_head",
      "name": {
        "en_us": "Hoglin Head",
        "de_de": "Hoglinkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a hoglin."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:hollow_howler_skull",
      "name": {
        "en_us": "Hollow Howler Skull",
        "de_de": "Hohlheuler-Schädel"
      },
      "note": {
        "en": {
          "summary": "A mob skull: only a charged creeper's explosion killing the mob drops it. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Schädel: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a hollow howler."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:hollow_howler_wall_skull",
      "name": {
        "en_us": "Hollow Howler Skull",
        "de_de": "Hohlheuler-Schädel"
      },
      "note": {
        "en": {
          "summary": "The wall form of the skull (placed against a wall); drops and names like the standing skull."
        },
        "de": {
          "summary": "Die Wandform des Schädels (an eine Wand gesetzt); Beute und Name wie beim stehenden Schädel."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a hollow howler."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:hollow_watcher_head",
      "name": {
        "en_us": "Hollow Watcher Head",
        "de_de": "Hohlwächterkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a hollow watcher."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:hollow_watcher_wall_head",
      "name": {
        "en_us": "Hollow Watcher Head",
        "de_de": "Hohlwächterkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a hollow watcher."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:horse_head",
      "name": {
        "en_us": "Horse Head",
        "de_de": "Pferdekopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a horse."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:horse_wall_head",
      "name": {
        "en_us": "Horse Head",
        "de_de": "Pferdekopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a horse."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:husk_head",
      "name": {
        "en_us": "Husk Head",
        "de_de": "Wüstenzombiekopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a husk."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Food never gives its wearer Hunger."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Essen macht den Träger nie hungrig (kein Hunger-Effekt)."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:husk_wall_head",
      "name": {
        "en_us": "Husk Head",
        "de_de": "Wüstenzombiekopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a husk."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Food never gives its wearer Hunger."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Essen macht den Träger nie hungrig (kein Hunger-Effekt)."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:illusioner_head",
      "name": {
        "en_us": "Illusioner Head",
        "de_de": "Illusionistenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a illusioner."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:illusioner_wall_head",
      "name": {
        "en_us": "Illusioner Head",
        "de_de": "Illusionistenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a illusioner."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:iron_golem_head",
      "name": {
        "en_us": "Iron Golem Head",
        "de_de": "Eisengolemkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a iron golem."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:iron_golem_wall_head",
      "name": {
        "en_us": "Iron Golem Head",
        "de_de": "Eisengolemkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a iron golem."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:llama_head",
      "name": {
        "en_us": "Llama Head",
        "de_de": "Lamakopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a llama."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:llama_wall_head",
      "name": {
        "en_us": "Llama Head",
        "de_de": "Lamakopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a llama."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:magma_cube_head",
      "name": {
        "en_us": "Magma Cube Head",
        "de_de": "Magmawürfelkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a magma cube."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:magma_cube_wall_head",
      "name": {
        "en_us": "Magma Cube Head",
        "de_de": "Magmawürfelkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a magma cube."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:mooshroom_head",
      "name": {
        "en_us": "Mooshroom Head",
        "de_de": "Pilzkuhkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a mooshroom."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Another player can fill a bowl with mushroom stew from its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Ein anderer Spieler füllt beim Träger eine Schüssel mit Pilzsuppe."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:mooshroom_wall_head",
      "name": {
        "en_us": "Mooshroom Head",
        "de_de": "Pilzkuhkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a mooshroom."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Another player can fill a bowl with mushroom stew from its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Ein anderer Spieler füllt beim Träger eine Schüssel mit Pilzsuppe."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:mule_head",
      "name": {
        "en_us": "Mule Head",
        "de_de": "Maultierkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a mule."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:mule_wall_head",
      "name": {
        "en_us": "Mule Head",
        "de_de": "Maultierkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a mule."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:nautilus_head",
      "name": {
        "en_us": "Nautilus Head",
        "de_de": "Nautiluskopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a nautilus."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:nautilus_wall_head",
      "name": {
        "en_us": "Nautilus Head",
        "de_de": "Nautiluskopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a nautilus."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:nibblemite_head",
      "name": {
        "en_us": "Nibblemite Head",
        "de_de": "Knabbermilbenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a nibblemite."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:nibblemite_wall_head",
      "name": {
        "en_us": "Nibblemite Head",
        "de_de": "Knabbermilbenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a nibblemite."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:ocelot_head",
      "name": {
        "en_us": "Ocelot Head",
        "de_de": "Ozelotkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a ocelot."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:ocelot_wall_head",
      "name": {
        "en_us": "Ocelot Head",
        "de_de": "Ozelotkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a ocelot."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:panda_head",
      "name": {
        "en_us": "Panda Head",
        "de_de": "Pandakopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a panda."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:panda_wall_head",
      "name": {
        "en_us": "Panda Head",
        "de_de": "Pandakopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a panda."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:parched_skull",
      "name": {
        "en_us": "Parched Skull",
        "de_de": "Dörrlingsschädel"
      },
      "note": {
        "en": {
          "summary": "A mob skull: only a charged creeper's explosion killing the mob drops it. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Schädel: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a parched."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:parched_wall_skull",
      "name": {
        "en_us": "Parched Skull",
        "de_de": "Dörrlingsschädel"
      },
      "note": {
        "en": {
          "summary": "The wall form of the skull (placed against a wall); drops and names like the standing skull."
        },
        "de": {
          "summary": "Die Wandform des Schädels (an eine Wand gesetzt); Beute und Name wie beim stehenden Schädel."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a parched."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:parrot_head",
      "name": {
        "en_us": "Parrot Head",
        "de_de": "Papageienkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a parrot."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:parrot_wall_head",
      "name": {
        "en_us": "Parrot Head",
        "de_de": "Papageienkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a parrot."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:phantom_head",
      "name": {
        "en_us": "Phantom Head",
        "de_de": "Phantomkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a phantom."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:phantom_wall_head",
      "name": {
        "en_us": "Phantom Head",
        "de_de": "Phantomkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a phantom."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:pig_head",
      "name": {
        "en_us": "Pig Head",
        "de_de": "Schweinekopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a pig."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak three seconds on grass, dirt or moss: the truffle nose digs something up."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Drei Sekunden Schleichen auf Gras, Erde oder Moos: die Trüffelnase scharrt etwas aus."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:pig_wall_head",
      "name": {
        "en_us": "Pig Head",
        "de_de": "Schweinekopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a pig."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak three seconds on grass, dirt or moss: the truffle nose digs something up."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Drei Sekunden Schleichen auf Gras, Erde oder Moos: die Trüffelnase scharrt etwas aus."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:piglin_brute_head",
      "name": {
        "en_us": "Piglin Brute Head",
        "de_de": "Piglin-Barbaren-Kopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a piglin brute."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:piglin_brute_wall_head",
      "name": {
        "en_us": "Piglin Brute Head",
        "de_de": "Piglin-Barbaren-Kopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a piglin brute."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:pillager_head",
      "name": {
        "en_us": "Pillager Head",
        "de_de": "Plündererkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a pillager."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:pillager_wall_head",
      "name": {
        "en_us": "Pillager Head",
        "de_de": "Plündererkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a pillager."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:polar_bear_head",
      "name": {
        "en_us": "Polar Bear Head",
        "de_de": "Eisbärenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a polar bear."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Ice is less slippery for its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Eis ist für den Träger weniger rutschig."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:polar_bear_wall_head",
      "name": {
        "en_us": "Polar Bear Head",
        "de_de": "Eisbärenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a polar bear."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Ice is less slippery for its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Eis ist für den Träger weniger rutschig."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:pufferfish_head",
      "name": {
        "en_us": "Pufferfish Head",
        "de_de": "Kugelfischkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a pufferfish."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:pufferfish_wall_head",
      "name": {
        "en_us": "Pufferfish Head",
        "de_de": "Kugelfischkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a pufferfish."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:rabbit_head",
      "name": {
        "en_us": "Rabbit Head",
        "de_de": "Kaninchenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a rabbit."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:rabbit_wall_head",
      "name": {
        "en_us": "Rabbit Head",
        "de_de": "Kaninchenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a rabbit."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:ravager_head",
      "name": {
        "en_us": "Ravager Head",
        "de_de": "Verwüsterkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a ravager."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:ravager_wall_head",
      "name": {
        "en_us": "Ravager Head",
        "de_de": "Verwüsterkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a ravager."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:salmon_head",
      "name": {
        "en_us": "Salmon Head",
        "de_de": "Lachskopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a salmon."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:salmon_wall_head",
      "name": {
        "en_us": "Salmon Head",
        "de_de": "Lachskopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a salmon."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:sheep_head",
      "name": {
        "en_us": "Sheep Head",
        "de_de": "Schafskopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a sheep."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Another player can shear its wearer (1-3 wool, then it grows back)."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Ein anderer Spieler kann den Träger scheren (1-3 Wolle, dann wächst sie nach)."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:sheep_wall_head",
      "name": {
        "en_us": "Sheep Head",
        "de_de": "Schafskopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a sheep."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Another player can shear its wearer (1-3 wool, then it grows back)."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Ein anderer Spieler kann den Träger scheren (1-3 Wolle, dann wächst sie nach)."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:shellker_head",
      "name": {
        "en_us": "Shellker Head",
        "de_de": "Shellkerkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a shellker."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:shellker_wall_head",
      "name": {
        "en_us": "Shellker Head",
        "de_de": "Shellkerkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a shellker."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:shulker_head",
      "name": {
        "en_us": "Shulker Head",
        "de_de": "Shulkerkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a shulker."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer opens shulker boxes even when the lid is blocked."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger öffnet Shulkerkisten auch mit versperrtem Deckel."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:shulker_wall_head",
      "name": {
        "en_us": "Shulker Head",
        "de_de": "Shulkerkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a shulker."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer opens shulker boxes even when the lid is blocked."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger öffnet Shulkerkisten auch mit versperrtem Deckel."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:silverfish_head",
      "name": {
        "en_us": "Silverfish Head",
        "de_de": "Silberfischchenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a silverfish."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer shrinks to half size."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger schrumpft auf die halbe Größe."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:silverfish_wall_head",
      "name": {
        "en_us": "Silverfish Head",
        "de_de": "Silberfischchenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a silverfish."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer shrinks to half size."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger schrumpft auf die halbe Größe."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:skeleton_horse_skull",
      "name": {
        "en_us": "Skeleton Horse Skull",
        "de_de": "Skelettpferdeschädel"
      },
      "note": {
        "en": {
          "summary": "A mob skull: only a charged creeper's explosion killing the mob drops it. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Schädel: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a skeleton horse."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:skeleton_horse_wall_skull",
      "name": {
        "en_us": "Skeleton Horse Skull",
        "de_de": "Skelettpferdeschädel"
      },
      "note": {
        "en": {
          "summary": "The wall form of the skull (placed against a wall); drops and names like the standing skull."
        },
        "de": {
          "summary": "Die Wandform des Schädels (an eine Wand gesetzt); Beute und Name wie beim stehenden Schädel."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a skeleton horse."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:slime_head",
      "name": {
        "en_us": "Slime Head",
        "de_de": "Schleimkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a slime."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer falls one block further without damage."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger fällt einen Block tiefer ohne Schaden."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:slime_wall_head",
      "name": {
        "en_us": "Slime Head",
        "de_de": "Schleimkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a slime."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer falls one block further without damage."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger fällt einen Block tiefer ohne Schaden."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:sniffer_head",
      "name": {
        "en_us": "Sniffer Head",
        "de_de": "Schnüfflerkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a sniffer."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak five seconds on moss, grass or dirt: it sniffs up seeds."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Fünf Sekunden Schleichen auf Moos, Gras oder Erde: er erschnüffelt Samen."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:sniffer_wall_head",
      "name": {
        "en_us": "Sniffer Head",
        "de_de": "Schnüfflerkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a sniffer."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak five seconds on moss, grass or dirt: it sniffs up seeds."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Fünf Sekunden Schleichen auf Moos, Gras oder Erde: er erschnüffelt Samen."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:snow_golem_head",
      "name": {
        "en_us": "Snow Golem Head",
        "de_de": "Schneegolemkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a snow golem."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:snow_golem_wall_head",
      "name": {
        "en_us": "Snow Golem Head",
        "de_de": "Schneegolemkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a snow golem."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:soulfire_blaze_head",
      "name": {
        "en_us": "Soulfire Blaze Head",
        "de_de": "Seelenfeuerlohenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a soulfire blaze."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:soulfire_blaze_wall_head",
      "name": {
        "en_us": "Soulfire Blaze Head",
        "de_de": "Seelenfeuerlohenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a soulfire blaze."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:spider_head",
      "name": {
        "en_us": "Spider Head",
        "de_de": "Spinnenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a spider."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Cobwebs do not slow its wearer down."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Spinnennetze bremsen den Träger nicht."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:spider_wall_head",
      "name": {
        "en_us": "Spider Head",
        "de_de": "Spinnenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a spider."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Cobwebs do not slow its wearer down."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Spinnennetze bremsen den Träger nicht."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:squid_head",
      "name": {
        "en_us": "Squid Head",
        "de_de": "Tintenfischkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a squid."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:squid_wall_head",
      "name": {
        "en_us": "Squid Head",
        "de_de": "Tintenfischkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a squid."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:stray_skull",
      "name": {
        "en_us": "Stray Skull",
        "de_de": "Eiswandererschädel"
      },
      "note": {
        "en": {
          "summary": "A mob skull: only a charged creeper's explosion killing the mob drops it. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Schädel: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a stray."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Powder snow does not freeze its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Pulverschnee lässt den Träger nicht erfrieren."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:stray_wall_skull",
      "name": {
        "en_us": "Stray Skull",
        "de_de": "Eiswandererschädel"
      },
      "note": {
        "en": {
          "summary": "The wall form of the skull (placed against a wall); drops and names like the standing skull."
        },
        "de": {
          "summary": "Die Wandform des Schädels (an eine Wand gesetzt); Beute und Name wie beim stehenden Schädel."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a stray."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Powder snow does not freeze its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Pulverschnee lässt den Träger nicht erfrieren."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:strider_head",
      "name": {
        "en_us": "Strider Head",
        "de_de": "Schreiterkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a strider."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:strider_wall_head",
      "name": {
        "en_us": "Strider Head",
        "de_de": "Schreiterkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a strider."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:sulfur_cube_head",
      "name": {
        "en_us": "Sulfur Cube Head",
        "de_de": "Schwefelwürfelkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a sulfur cube."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:sulfur_cube_wall_head",
      "name": {
        "en_us": "Sulfur Cube Head",
        "de_de": "Schwefelwürfelkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a sulfur cube."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:tadpole_head",
      "name": {
        "en_us": "Tadpole Head",
        "de_de": "Kaulquappenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a tadpole."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:tadpole_wall_head",
      "name": {
        "en_us": "Tadpole Head",
        "de_de": "Kaulquappenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a tadpole."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:trader_llama_head",
      "name": {
        "en_us": "Trader Llama Head",
        "de_de": "Händlerlamakopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a trader llama."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:trader_llama_wall_head",
      "name": {
        "en_us": "Trader Llama Head",
        "de_de": "Händlerlamakopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a trader llama."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:tropical_fish_head",
      "name": {
        "en_us": "Tropical Fish Head",
        "de_de": "Tropenfischkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a tropical fish."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:tropical_fish_wall_head",
      "name": {
        "en_us": "Tropical Fish Head",
        "de_de": "Tropenfischkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a tropical fish."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:turtle_head",
      "name": {
        "en_us": "Turtle Head",
        "de_de": "Schildkrötenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a turtle."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer never tramples turtle eggs."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger zertrampelt keine Schildkröteneier."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:turtle_wall_head",
      "name": {
        "en_us": "Turtle Head",
        "de_de": "Schildkrötenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a turtle."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Its wearer never tramples turtle eggs."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Der Träger zertrampelt keine Schildkröteneier."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:vex_head",
      "name": {
        "en_us": "Vex Head",
        "de_de": "Plagegeistkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a vex."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:vex_wall_head",
      "name": {
        "en_us": "Vex Head",
        "de_de": "Plagegeistkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a vex."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:villager_head",
      "name": {
        "en_us": "Villager Head",
        "de_de": "Dorfbewohnerkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a villager."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:villager_wall_head",
      "name": {
        "en_us": "Villager Head",
        "de_de": "Dorfbewohnerkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a villager."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:vindicator_head",
      "name": {
        "en_us": "Vindicator Head",
        "de_de": "Diener-Kopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a vindicator."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:vindicator_wall_head",
      "name": {
        "en_us": "Vindicator Head",
        "de_de": "Diener-Kopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a vindicator."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:wandering_trader_head",
      "name": {
        "en_us": "Wandering Trader Head",
        "de_de": "Fahrender-Händler-Kopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a wandering trader."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:wandering_trader_wall_head",
      "name": {
        "en_us": "Wandering Trader Head",
        "de_de": "Fahrender-Händler-Kopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a wandering trader."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:warden_head",
      "name": {
        "en_us": "Warden Head",
        "de_de": "Wärterkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a warden."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Half of its wearer's footsteps never reach sculk."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Die Hälfte der Schritte des Trägers erreicht kein Sculk."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:warden_wall_head",
      "name": {
        "en_us": "Warden Head",
        "de_de": "Wärterkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a warden."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Half of its wearer's footsteps never reach sculk."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Die Hälfte der Schritte des Trägers erreicht kein Sculk."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:witch_head",
      "name": {
        "en_us": "Witch Head",
        "de_de": "Hexenkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a witch."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:witch_wall_head",
      "name": {
        "en_us": "Witch Head",
        "de_de": "Hexenkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a witch."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:wither_head",
      "name": {
        "en_us": "Wither Head",
        "de_de": "Witherkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a wither."
          ],
          "use": [
            "Only with the server switch for boss heads: dropped when a charged creeper's explosion kills it. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Nur mit dem Server-Schalter für Boss-Köpfe: fällt, wenn ein geladener Creeper ihn sprengt. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:wither_wall_head",
      "name": {
        "en_us": "Wither Head",
        "de_de": "Witherkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a wither."
          ],
          "use": [
            "Only with the server switch for boss heads: dropped when a charged creeper's explosion kills it. Place it, wear it or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Nur mit dem Server-Schalter für Boss-Köpfe: fällt, wenn ein geladener Creeper ihn sprengt. Aufstellen, tragen oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:wolf_head",
      "name": {
        "en_us": "Wolf Head",
        "de_de": "Wolfskopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a wolf."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Wild wolves never attack its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Wilde Wölfe greifen den Träger nie an."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:wolf_wall_head",
      "name": {
        "en_us": "Wolf Head",
        "de_de": "Wolfskopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a wolf."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Wild wolves never attack its wearer."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Wilde Wölfe greifen den Träger nie an."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:zoglin_head",
      "name": {
        "en_us": "Zoglin Head",
        "de_de": "Zoglinkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a zoglin."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:zoglin_wall_head",
      "name": {
        "en_us": "Zoglin Head",
        "de_de": "Zoglinkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a zoglin."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:zombie_horse_head",
      "name": {
        "en_us": "Zombie Horse Head",
        "de_de": "Zombiepferdekopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a zombie horse."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:zombie_horse_wall_head",
      "name": {
        "en_us": "Zombie Horse Head",
        "de_de": "Zombiepferdekopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a zombie horse."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:zombie_nautilus_head",
      "name": {
        "en_us": "Zombie Nautilus Head",
        "de_de": "Zombie-Nautiluskopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a zombie nautilus."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:zombie_nautilus_wall_head",
      "name": {
        "en_us": "Zombie Nautilus Head",
        "de_de": "Zombie-Nautiluskopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a zombie nautilus."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:zombie_villager_head",
      "name": {
        "en_us": "Zombie Villager Head",
        "de_de": "Zombiedorfbewohnerkopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a zombie villager."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:zombie_villager_wall_head",
      "name": {
        "en_us": "Zombie Villager Head",
        "de_de": "Zombiedorfbewohnerkopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a zombie villager."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:zombified_piglin_head",
      "name": {
        "en_us": "Zombified Piglin Head",
        "de_de": "Zombifizierter-Piglin-Kopf"
      },
      "note": {
        "en": {
          "summary": "A mob head: only a charged creeper's explosion killing the mob drops it, with the mob's variant. Wear it, place it or put it on a note block."
        },
        "de": {
          "summary": "Ein Mob-Kopf: nur die Explosion eines geladenen Creepers, die den Mob tötet, lässt ihn fallen, mit der Variante des Mobs. Tragen, aufstellen oder auf einen Notenblock setzen."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a zombified piglin."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemobheads:zombified_piglin_wall_head",
      "name": {
        "en_us": "Zombified Piglin Head",
        "de_de": "Zombifizierter-Piglin-Kopf"
      },
      "note": {
        "en": {
          "summary": "The wall form of the head (placed against a wall); drops and names like the standing head."
        },
        "de": {
          "summary": "Die Wandform des Kopfes (an eine Wand gesetzt); Beute und Name wie beim stehenden Kopf."
        }
      },
      "questionary": {
        "en": {
          "lore": [
            "{name}: proof that a charged creeper met a zombified piglin."
          ],
          "use": [
            "Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block."
          ],
          "secret": [
            "Sneak while wearing it: a quiet greeting in the mob's own voice."
          ]
        },
        "de": {
          "lore": [
            "{name}: der Beweis, dass ein geladener Creeper hier war."
          ],
          "use": [
            "Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen."
          ],
          "secret": [
            "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
          ]
        }
      },
      "craftedBy": [],
      "usedIn": []
    }
  ],
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
    "features": 15,
    "recipes": 1,
    "lootTables": 97,
    "tags": 3,
    "advancements": 99,
    "enchantments": 0,
    "items": 1,
    "blocks": 194,
    "trades": 0,
    "config": 0,
    "quests": 0,
    "recipesOtherLines": 0,
    "undocumented": 0,
    "inWorld": 0,
    "incompleteProse": 0
  }
};
