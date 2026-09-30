window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simpleriding"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simpleriding",
    "name": "Simple Riding",
    "version": "1.0.5",
    "minecraftLines": [
      "26.3"
    ],
    "loaders": [
      "fabric",
      "neoforge"
    ]
  },
  "features": [
    {
      "id": "tailwind",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingEffects.java",
        "modules/simpleriding/generated/resources/data/simpleriding/enchantment/tailwind.json"
      ],
      "en": {
        "title": "Tailwind",
        "summary": "Tailwind I–III enhances saddles and all 16 colored harnesses. Apply a book at an anvil.",
        "details": [
          "Tailwind I–III enhances saddles and all 16 colored harnesses. Apply a book at an anvil.",
          "Only a player-controlled mount receives the bonus. Each level adds 30% speed on horses and camels, 20% on pigs, striders and nautiluses, and 85% flight speed on Happy Ghasts by default. Enchantment levels are capped at III; the total server bonus is capped at +300%.",
          "The server updates transient attribute modifiers each tick; removing the enchantment or dismounting removes the bonus. A plain ghast harness grants no free boost.",
          "Happy Ghast flight uses flying speed twice in Vanilla. The server applies a square-root attribute bonus so the actual quadratic flight amplification stays within the total speed cap."
        ]
      },
      "de": {
        "title": "Rückenwind",
        "summary": "Rückenwind I–III verbessert Sättel und alle 16 Geschirrfarben. Ein Buch wird am Amboss angewendet.",
        "details": [
          "Rückenwind I–III verbessert Sättel und alle 16 Geschirrfarben. Ein Buch wird am Amboss angewendet.",
          "Nur spielergesteuerte Reittiere erhalten den Bonus: je Stufe standardmäßig 30% für Pferde und Kamele, 20% für Schweine, Schreiter und Nautilusse, 85% Flugtempo für Happy Ghasts. Höchstens Stufe III; der gesamte Serverbonus ist auf +300% begrenzt.",
          "Der Server aktualisiert flüchtige Attributmodifikatoren pro Tick. Verzauberung entfernen oder absteigen entfernt den Bonus. Normales Ghastgeschirr gibt keinen Gratisbonus.",
          "Vanilla nutzt das Happy-Ghast-Flugtempo zweimal. Der Server verwendet deshalb einen Quadratwurzel-Attributbonus, damit die tatsächliche quadratische Flugverstärkung die Gesamtgeschwindigkeitsgrenze einhält."
        ]
      }
    },
    {
      "id": "leaping",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingEffects.java",
        "modules/simpleriding/generated/resources/data/simpleriding/enchantment/leaping.json",
        "modules/simpleriding/generated/resources/data/simpleriding/tags/item/horse_armor_enchantable.json"
      ],
      "en": {
        "title": "Leaping",
        "summary": "Leaping I–III increases the jump strength of a player-controlled horse by 20% per level by default.",
        "details": [
          "Leaping I–III increases the jump strength of a player-controlled horse by 20% per level by default.",
          "Apply its book to horse armor at an anvil. All six vanilla horse armor materials and optional SimpleBuilding Enderite Horse Armor are supported. Removing armor or dismounting clears the modifier.",
          "Nautilus and zombie nautilus armor accepts Leaping too: +20% dash strength per level by default, using the same total speed cap as Tailwind. The server executes the dash and preserves the vanilla 40-tick cooldown.",
          "The total server dash impulse, including prior momentum, is clamped to the configured tick movement limit and at most 3.9 blocks/tick; nonfinite motion is refused."
        ]
      },
      "de": {
        "title": "Sprungkraft",
        "summary": "Sprungkraft I–III steigert die Sprungkraft eines spielergesteuerten Pferdes standardmäßig um 20% je Stufe.",
        "details": [
          "Sprungkraft I–III steigert die Sprungkraft eines spielergesteuerten Pferdes standardmäßig um 20% je Stufe.",
          "Das Buch wird am Amboss auf Pferderüstung angewendet. Alle sechs Vanilla-Materialien und optional SimpleBuildings Enderit-Pferderüstung werden unterstützt. Rüstung entfernen oder absteigen löscht den Modifikator.",
          "Nautilus- und Zombie-Nautilusrüstung erlaubt ebenfalls Sprungkraft: standardmäßig +20% Dash-Kraft je Stufe, mit derselben Gesamtgeschwindigkeitsgrenze wie Rückenwind. Der Server führt den Dash aus und behält Vanillas 40-Tick-Abklingzeit.",
          "Der gesamte Server-Dash-Impuls inklusive vorherigem Schwung wird auf die konfigurierte Tickbewegungsgrenze und höchstens 3.9 Blöcke/Tick begrenzt; nichtendliche Bewegung wird verweigert."
        ]
      }
    },
    {
      "id": "armor",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/mixin/RidingEnchantmentMixin.java",
        "modules/simpleriding/shared/java/com/simpleriding/mixin/RidingHelperMixin.java",
        "modules/simpleriding/shared/java/com/simpleriding/mixin/RidingFallMixin.java"
      ],
      "en": {
        "title": "Horse Armor Utilities",
        "summary": "Horse armor accepts Protection, Fire Protection, Blast Protection, Projectile Protection, Feather Falling, and Leaping. Other enchantments, including Mending and Unbreaking, are rejected by the source mod’s final whitelist.",
        "details": [
          "Horse armor accepts Protection, Fire Protection, Blast Protection, Projectile Protection, Feather Falling, and Leaping. Other enchantments, including Mending and Unbreaking, are rejected by the source mod’s final whitelist.",
          "Armor has an effective enchantability of 15 for vanilla utility enchantments at the enchanting table. Custom riding enchantments remain book-only.",
          "Protection uses the vanilla BODY-slot damage pipeline exactly once. Horse Feather Falling reduces computed fall damage by up to 12% per level, capped at IV. Nautilus armor accepts the same protection types and Leaping, but no Feather Falling. Optional SimpleBuilding Enderite armor uses public item tags only."
        ]
      },
      "de": {
        "title": "Pferderüstungsfunktionen",
        "summary": "Pferderüstung erlaubt Schutz, Feuerschutz, Explosionsschutz, Schusssicherheit, Federfall und Sprungkraft. Andere Verzauberungen einschließlich Reparatur und Haltbarkeit werden nach der abschließenden Whitelist der Quelle verweigert.",
        "details": [
          "Pferderüstung erlaubt Schutz, Feuerschutz, Explosionsschutz, Schusssicherheit, Federfall und Sprungkraft. Andere Verzauberungen einschließlich Reparatur und Haltbarkeit werden nach der abschließenden Whitelist der Quelle verweigert.",
          "Rüstung hat am Zaubertisch für Vanilla-Funktionen eine effektive Verzauberbarkeit von 15. Die eigenen Reitverzauberungen bleiben reine Buchverzauberungen.",
          "Schutz nutzt Vanillas BODY-Schadenspipeline genau einmal. Pferde-Federfall senkt berechneten Fallschaden um höchstens 12% je Stufe bis IV. Nautilusrüstung erlaubt dieselben Schutzarten und Sprungkraft, aber keinen Federfall. Optionale SimpleBuilding-Enderitrüstung nutzt nur öffentliche Item-Tags."
        ]
      }
    },
    {
      "id": "loot",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingLoot.java"
      ],
      "en": {
        "title": "Exploration Loot",
        "summary": "Bastion treasure and other chests: 0–2 rolls; Tailwind II/III weights 20/10, Leaping II/III 10/5, empty 30.",
        "details": [
          "Bastion treasure and other chests: 0–2 rolls; Tailwind II/III weights 20/10, Leaping II/III 10/5, empty 30.",
          "Nether Fortress chests: 0–1 roll; Tailwind II/III weights 5/10, empty 20.",
          "Trial Chamber common and rare reward tables: 0–2 rolls; Leaping II/III 10/3, Tailwind II/III 10/3, Protection IV 10, empty 60.",
          "These are additive pools, independently disabled by worldGen.enableLootTableChanges; default true. No existing SimpleBuilding pool is replaced."
        ]
      },
      "de": {
        "title": "Erkundungsbeute",
        "summary": "Bastions-Schatzkisten und andere Bastionskisten: 0–2 Würfe; Rückenwind II/III Gewichte 20/10, Sprungkraft II/III 10/5, leer 30.",
        "details": [
          "Bastions-Schatzkisten und andere Bastionskisten: 0–2 Würfe; Rückenwind II/III Gewichte 20/10, Sprungkraft II/III 10/5, leer 30.",
          "Netherfestungskisten: 0–1 Wurf; Rückenwind II/III Gewichte 5/10, leer 20.",
          "Gewöhnliche und seltene Prüfungskammer-Belohnungstabellen: 0–2 Würfe; Sprungkraft II/III 10/3, Rückenwind II/III 10/3, Schutz IV 10, leer 60.",
          "Die zusätzlichen Pools werden unabhängig durch worldGen.enableLootTableChanges abgeschaltet; Standard true. Kein SimpleBuilding-Pool wird ersetzt."
        ]
      }
    },
    {
      "id": "trades",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/WeightedEnchantFunction.java",
        "modules/simpleriding/generated/resources/data/simpleriding/villager_trade/librarian/2/riding_book.json",
        "modules/simpleriding/generated/resources/data/simpleriding/villager_trade/librarian/3/riding_book.json",
        "modules/simpleriding/generated/resources/data/simpleriding/villager_trade/librarian/4/riding_book.json"
      ],
      "en": {
        "title": "Librarian Trades",
        "summary": "Librarians at level 2 offer Tailwind I or Leaping I (weights 30/20), cost 10–29 emeralds, 2 uses, 25 XP, reputation discount 0.5.",
        "details": [
          "Librarians at level 2 offer Tailwind I or Leaping I (weights 30/20), cost 10–29 emeralds, 2 uses, 25 XP, reputation discount 0.5.",
          "Level 3: Tailwind II or Leaping II (20/30), cost 15–34 emeralds, 1 use, 50 XP, discount 1.0; 15% chance to attempt a second, different enchantment.",
          "Level 4: Tailwind III or Leaping III (20/20), cost 15–49 emeralds, 1 use, 80 XP, discount 1.0; 35% chance to attempt a second enchantment. Selection retries at most 10 times.",
          "worldGen.enableVillagerTrades defaults true and is evaluated on data loading. The source implements no wandering trader or level-5 offer.",
          "The experimental Trade Rebalance pack replaces the librarian pools and can hide riding offers; the normal, nonexperimental pools include them."
        ]
      },
      "de": {
        "title": "Bibliothekarhandel",
        "summary": "Bibliothekare auf Stufe 2 verkaufen Rückenwind I oder Sprungkraft I (Gewichte 30/20), Preis 10–29 Smaragde, 2 Nutzungen, 25 EP, Rufrabatt 0,5.",
        "details": [
          "Bibliothekare auf Stufe 2 verkaufen Rückenwind I oder Sprungkraft I (Gewichte 30/20), Preis 10–29 Smaragde, 2 Nutzungen, 25 EP, Rufrabatt 0,5.",
          "Stufe 3: Rückenwind II oder Sprungkraft II (20/30), Preis 15–34 Smaragde, 1 Nutzung, 50 EP, Rabatt 1,0; 15% Chance auf den Versuch einer zweiten, anderen Verzauberung.",
          "Stufe 4: Rückenwind III oder Sprungkraft III (20/20), Preis 15–49 Smaragde, 1 Nutzung, 80 EP, Rabatt 1,0; 35% Chance auf eine zweite Verzauberung. Die Auswahl wird höchstens zehnmal wiederholt.",
          "worldGen.enableVillagerTrades ist standardmäßig true und wird beim Laden der Daten ausgewertet. Die Quelle enthält weder fahrenden Händler noch Stufe-5-Angebot.",
          "Das experimentelle Handelsneuverteilungs-Datenpaket ersetzt die Bibliothekar-Pools und kann Reitangebote ausblenden; die normalen Pools enthalten sie."
        ]
      }
    },
    {
      "id": "registry",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/Riding.java"
      ],
      "en": {
        "title": "Registry and Sources",
        "summary": "The mod keeps simpleriding:tailwind, simpleriding:leaping, the riding_items creative tab, and the unused persistent coordinates BlockPos component for old saved stacks.",
        "details": [
          "The mod keeps simpleriding:tailwind, simpleriding:leaping, the riding_items creative tab, and the unused persistent coordinates BlockPos component for old saved stacks.",
          "There are no custom items, blocks, entities, commands, keybindings, recipes. Riding books come from loot, librarians, and the creative tab; existing vanilla equipment keeps its vanilla models, textures, names, and sources.",
          "The module uses its own namespace, config file, language keys, generated data, and balance metadata; SimpleBuilding’s existing directories and registry entries remain intact.",
          "Obtaining nautilus armor unlocks an advancement explaining saddle Tailwind and armor Leaping. There is no chat announcement and no gameplay reward."
        ]
      },
      "de": {
        "title": "Registry und Bezugsquellen",
        "summary": "Die Mod behält simpleriding:tailwind, simpleriding:leaping, den Kreativtab riding_items und die ungenutzte persistente BlockPos-Komponente coordinates für alte gespeicherte Stapel.",
        "details": [
          "Die Mod behält simpleriding:tailwind, simpleriding:leaping, den Kreativtab riding_items und die ungenutzte persistente BlockPos-Komponente coordinates für alte gespeicherte Stapel.",
          "Es gibt keine eigenen Items, Blöcke, Entitäten, Befehle, Tastenzuweisungen, Rezepte. Reitbücher stammen aus Beute, Bibliothekarhandel und Kreativtab; Vanilla-Ausrüstung behält ihre Modelle, Texturen, Namen und Bezugsquellen.",
          "Das Modul verwendet eigenen Namensraum, Configdatei, Sprachschlüssel, generierte Daten und Balancemetadaten. SimpleBuildings Verzeichnisse und Registryeinträge bleiben erhalten.",
          "Nautilusrüstung im Inventar schaltet einen Fortschritt mit Hinweisen zu Sattel-Rückenwind und Rüstungs-Sprungkraft frei. Keine Chatankündigung und keine Spielbelohnung."
        ]
      }
    },
    {
      "id": "server-security",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingSecurity.java",
        "modules/simpleriding/shared/java/com/simpleriding/mixin/RidingPacketMixin.java",
        "modules/simpleriding/shared/java/com/simpleriding/mixin/RidingCamelMixin.java"
      ],
      "en": {
        "title": "Server Riding Bounds",
        "summary": "Client movement and jump commands are checked on the server thread.",
        "details": [
          "Client movement and jump commands are checked on the server thread.",
          "Only the controlling rider may act. Nonfinite coordinates/steering, pitch outside -90 to 90 degrees, jump charge outside 1-100, false entity IDs and cooldown/replay attempts are refused.",
          "Movement has a cumulative server-tick distance cap of 4 blocks and 20 packets by default, plus an envelope from server attributes and accepted nautilus dash. Dismounting or changing mounts cannot refill that tick budget. Horse ascent needs a server-accepted grounded jump or a grounded step.",
          "Targets must remain in loaded chunks and inside the world border including the mount bounding box. Vanilla collision and teleport checks still run; no claim permission, teleport, chunk ticket or entity spawn is introduced. External claim mods retain their own enforcement.",
          "Rejected packet floods cause at most one position correction per server tick.",
          "Accepted camel dashes include the Vanilla impulse in the movement envelope and enforce Vanilla's 55-tick cooldown on the server.",
          "Landing and fall checks derive the mount's ground flag from server collision instead of accepting the client's on-ground claim."
        ]
      },
      "de": {
        "title": "Server-Reitgrenzen",
        "summary": "Clientbewegungen und Sprungbefehle werden im Serverthread geprüft.",
        "details": [
          "Clientbewegungen und Sprungbefehle werden im Serverthread geprüft.",
          "Nur der steuernde Reiter darf handeln. Nichtendliche Koordinaten/Drehungen, Blickneigung außerhalb -90 bis 90 Grad, Sprungladung außerhalb 1-100, falsche Entitäts-IDs und Abklingzeit-/Wiederholungsversuche werden verweigert.",
          "Standardmäßig höchstens insgesamt 4 Blöcke Bewegung und 20 Pakete je Servertick, zusätzlich durch Serverattribute und akzeptierten Nautilus-Dash begrenzt. Absteigen oder Reittierwechsel erneuern das Tickbudget nicht. Pferde-Aufstieg braucht einen serverseitig akzeptierten Sprung vom Boden oder einen bodengestützten Schritt.",
          "Ziele müssen in geladenen Chunks und mit dem gesamten Reittier innerhalb der Weltgrenze liegen. Vanillas Kollisions- und Teleportprüfungen laufen weiter; keine Claimberechtigung, Teleportation, Chunktickets oder Entitäten werden hinzugefügt. Externe Claimmods behalten ihre eigene Durchsetzung.",
          "Verweigerte Paketfluten erzeugen höchstens eine Positionskorrektur je Servertick.",
          "Akzeptierte Kamel-Dashes berücksichtigen den Vanilla-Impuls in der Bewegungsgrenze und erzwingen Vanillas 55-Tick-Abklingzeit auf dem Server.",
          "Landungs- und Fallprüfungen ermitteln den Bodenstatus des Reittiers aus Server-Kollisionen statt aus Clientangaben."
        ]
      }
    },
    {
      "id": "config-worldGen-enableVillagerTrades",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Enable Villager Trades",
        "summary": "worldGen.enableVillagerTrades: Librarians offer riding books on levels 2–4. Default: true. Server setting; restart required.",
        "details": [
          "worldGen.enableVillagerTrades: Librarians offer riding books on levels 2–4. Default: true. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Dorfbewohnerhandel aktivieren",
        "summary": "worldGen.enableVillagerTrades: Bibliothekare bieten Reitbücher auf Stufen 2–4 an. Standard: true. Servereinstellung; Neustart erforderlich.",
        "details": [
          "worldGen.enableVillagerTrades: Bibliothekare bieten Reitbücher auf Stufen 2–4 an. Standard: true. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-worldGen-enableLootTableChanges",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Enable Loot Table Changes",
        "summary": "worldGen.enableLootTableChanges: Add riding books to Bastions, Nether Fortresses, and Trial Chamber rewards. Default: true. Server setting; restart required.",
        "details": [
          "worldGen.enableLootTableChanges: Add riding books to Bastions, Nether Fortresses, and Trial Chamber rewards. Default: true. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Beutetabellen ändern",
        "summary": "worldGen.enableLootTableChanges: Reitbücher in Bastionen, Netherfestungen und Prüfungskammerbelohnungen ergänzen. Standard: true. Servereinstellung; Neustart erforderlich.",
        "details": [
          "worldGen.enableLootTableChanges: Reitbücher in Bastionen, Netherfestungen und Prüfungskammerbelohnungen ergänzen. Standard: true. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-enchantments-swiftRide-ghastSpeedMultiplier",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Ghast Speed Multiplier",
        "summary": "enchantments.swiftRide.ghastSpeedMultiplier: Speed bonus per Tailwind level; range 0–1. Default: 0.85. Server setting; restart required.",
        "details": [
          "enchantments.swiftRide.ghastSpeedMultiplier: Speed bonus per Tailwind level; range 0–1. Default: 0.85. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Ghast-Geschwindigkeitsfaktor",
        "summary": "enchantments.swiftRide.ghastSpeedMultiplier: Geschwindigkeitsbonus je Rückenwindstufe; Bereich 0–1. Standard: 0.85. Servereinstellung; Neustart erforderlich.",
        "details": [
          "enchantments.swiftRide.ghastSpeedMultiplier: Geschwindigkeitsbonus je Rückenwindstufe; Bereich 0–1. Standard: 0.85. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-enchantments-swiftRide-horseSpeedMultiplier",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Horse Speed Multiplier",
        "summary": "enchantments.swiftRide.horseSpeedMultiplier: Speed bonus per Tailwind level; range 0–1. Default: 0.3. Server setting; restart required.",
        "details": [
          "enchantments.swiftRide.horseSpeedMultiplier: Speed bonus per Tailwind level; range 0–1. Default: 0.3. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Pferde-Geschwindigkeitsfaktor",
        "summary": "enchantments.swiftRide.horseSpeedMultiplier: Geschwindigkeitsbonus je Rückenwindstufe; Bereich 0–1. Standard: 0.3. Servereinstellung; Neustart erforderlich.",
        "details": [
          "enchantments.swiftRide.horseSpeedMultiplier: Geschwindigkeitsbonus je Rückenwindstufe; Bereich 0–1. Standard: 0.3. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-enchantments-swiftRide-otherSpeedMultiplier",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Other Mount Speed Multiplier",
        "summary": "enchantments.swiftRide.otherSpeedMultiplier: Speed bonus per Tailwind level; range 0–1. Default: 0.2. Server setting; restart required.",
        "details": [
          "enchantments.swiftRide.otherSpeedMultiplier: Speed bonus per Tailwind level; range 0–1. Default: 0.2. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Geschwindigkeitsfaktor anderer Reittiere",
        "summary": "enchantments.swiftRide.otherSpeedMultiplier: Geschwindigkeitsbonus je Rückenwindstufe; Bereich 0–1. Standard: 0.2. Servereinstellung; Neustart erforderlich.",
        "details": [
          "enchantments.swiftRide.otherSpeedMultiplier: Geschwindigkeitsbonus je Rückenwindstufe; Bereich 0–1. Standard: 0.2. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-enchantments-horseJump-jumpStrengthMultiplier",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Jump Strength Multiplier",
        "summary": "enchantments.horseJump.jumpStrengthMultiplier: Jump strength bonus per Leaping level; range 0–0.5. Default: 0.2. Server setting; restart required.",
        "details": [
          "enchantments.horseJump.jumpStrengthMultiplier: Jump strength bonus per Leaping level; range 0–0.5. Default: 0.2. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Sprungkraftfaktor",
        "summary": "enchantments.horseJump.jumpStrengthMultiplier: Sprungkraftbonus je Sprungkraftstufe; Bereich 0–0,5. Standard: 0.2. Servereinstellung; Neustart erforderlich.",
        "details": [
          "enchantments.horseJump.jumpStrengthMultiplier: Sprungkraftbonus je Sprungkraftstufe; Bereich 0–0,5. Standard: 0.2. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-safety-enableTailwind",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Enable Tailwind",
        "summary": "safety.enableTailwind: Allow saddle and harness speed bonuses. Default: true. Server setting; restart required.",
        "details": [
          "safety.enableTailwind: Allow saddle and harness speed bonuses. Default: true. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Rückenwind aktivieren",
        "summary": "safety.enableTailwind: Geschwindigkeitsboni für Sattel und Geschirr erlauben. Standard: true. Servereinstellung; Neustart erforderlich.",
        "details": [
          "safety.enableTailwind: Geschwindigkeitsboni für Sattel und Geschirr erlauben. Standard: true. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-safety-enableLeaping",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Enable Leaping",
        "summary": "safety.enableLeaping: Allow horse jumps and nautilus dash bonuses. Default: true. Server setting; restart required.",
        "details": [
          "safety.enableLeaping: Allow horse jumps and nautilus dash bonuses. Default: true. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Sprungkraft aktivieren",
        "summary": "safety.enableLeaping: Pferdesprung- und Nautilus-Dash-Boni erlauben. Standard: true. Servereinstellung; Neustart erforderlich.",
        "details": [
          "safety.enableLeaping: Pferdesprung- und Nautilus-Dash-Boni erlauben. Standard: true. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-safety-enableArmorUtilities",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Enable Armor Utilities",
        "summary": "safety.enableArmorUtilities: Allow utility enchantments on mount armor and horse Feather Falling. Existing vanilla protection remains active. Default: true. Server setting; restart required.",
        "details": [
          "safety.enableArmorUtilities: Allow utility enchantments on mount armor and horse Feather Falling. Existing vanilla protection remains active. Default: true. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Rüstungsfunktionen aktivieren",
        "summary": "safety.enableArmorUtilities: Zusatzverzauberungen auf Reittier-Rüstung und Pferde-Federfall erlauben. Bestehender Vanilla-Schutz bleibt aktiv. Standard: true. Servereinstellung; Neustart erforderlich.",
        "details": [
          "safety.enableArmorUtilities: Zusatzverzauberungen auf Reittier-Rüstung und Pferde-Federfall erlauben. Bestehender Vanilla-Schutz bleibt aktiv. Standard: true. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-safety-enableNautilus",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Enable Nautilus Bonuses",
        "summary": "safety.enableNautilus: Allow Tailwind and Leaping on living and zombie nautiluses. Default: true. Server setting; restart required.",
        "details": [
          "safety.enableNautilus: Allow Tailwind and Leaping on living and zombie nautiluses. Default: true. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Nautilus-Boni aktivieren",
        "summary": "safety.enableNautilus: Rückenwind und Sprungkraft für lebende und Zombie-Nautilusse erlauben. Standard: true. Servereinstellung; Neustart erforderlich.",
        "details": [
          "safety.enableNautilus: Rückenwind und Sprungkraft für lebende und Zombie-Nautilusse erlauben. Standard: true. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-safety-maximumSpeedBonus",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Maximum Total Speed Bonus",
        "summary": "safety.maximumSpeedBonus: Maximum added speed, including combined nautilus Tailwind and dash; range 0-3 (at most 4x base). Default: 3.0. Server setting; restart required.",
        "details": [
          "safety.maximumSpeedBonus: Maximum added speed, including combined nautilus Tailwind and dash; range 0-3 (at most 4x base). Default: 3.0. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Maximaler Gesamtgeschwindigkeitsbonus",
        "summary": "safety.maximumSpeedBonus: Maximaler Geschwindigkeitsbonus inklusive Nautilus-Rückenwind und Dash; Bereich 0-3 (höchstens 4x Basis). Standard: 3.0. Servereinstellung; Neustart erforderlich.",
        "details": [
          "safety.maximumSpeedBonus: Maximaler Geschwindigkeitsbonus inklusive Nautilus-Rückenwind und Dash; Bereich 0-3 (höchstens 4x Basis). Standard: 3.0. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-safety-maximumJumpBonus",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Maximum Total Jump Bonus",
        "summary": "safety.maximumJumpBonus: Maximum horse jump or nautilus dash bonus; range 0-1.5. Default: 1.5. Server setting; restart required.",
        "details": [
          "safety.maximumJumpBonus: Maximum horse jump or nautilus dash bonus; range 0-1.5. Default: 1.5. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Maximaler Gesamtsprungbonus",
        "summary": "safety.maximumJumpBonus: Maximaler Pferdesprung- oder Nautilus-Dash-Bonus; Bereich 0-1.5. Standard: 1.5. Servereinstellung; Neustart erforderlich.",
        "details": [
          "safety.maximumJumpBonus: Maximaler Pferdesprung- oder Nautilus-Dash-Bonus; Bereich 0-1.5. Standard: 1.5. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-safety-movementDistancePerTick",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Riding Movement Limit",
        "summary": "safety.movementDistancePerTick: Maximum cumulative client vehicle movement in blocks per server tick; range 0.5-4. Lower limits may correct legitimate fast mounts or falls. Default: 4.0. Server setting; restart required.",
        "details": [
          "safety.movementDistancePerTick: Maximum cumulative client vehicle movement in blocks per server tick; range 0.5-4. Lower limits may correct legitimate fast mounts or falls. Default: 4.0. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Reitbewegungsgrenze",
        "summary": "safety.movementDistancePerTick: Maximale gesamte Client-Reitbewegung in Blöcken je Servertick; Bereich 0.5-4. Kleine Grenzen können schnelle Reittiere oder Fälle korrigieren. Standard: 4.0. Servereinstellung; Neustart erforderlich.",
        "details": [
          "safety.movementDistancePerTick: Maximale gesamte Client-Reitbewegung in Blöcken je Servertick; Bereich 0.5-4. Kleine Grenzen können schnelle Reittiere oder Fälle korrigieren. Standard: 4.0. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-safety-movementPacketsPerTick",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Riding Packet Limit",
        "summary": "safety.movementPacketsPerTick: Maximum vehicle movement packets per server tick; range 1-20. Excess packets are refused. Default: 20. Server setting; restart required.",
        "details": [
          "safety.movementPacketsPerTick: Maximum vehicle movement packets per server tick; range 1-20. Excess packets are refused. Default: 20. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Reitpaketgrenze",
        "summary": "safety.movementPacketsPerTick: Maximale Reitbewegungspakete je Servertick; Bereich 1-20. Überzaehlige Pakete werden verweigert. Standard: 20. Servereinstellung; Neustart erforderlich.",
        "details": [
          "safety.movementPacketsPerTick: Maximale Reitbewegungspakete je Servertick; Bereich 1-20. Überzaehlige Pakete werden verweigert. Standard: 20. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-enchantments-swiftRide-nautilusSpeedMultiplier",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Nautilus Speed Multiplier",
        "summary": "enchantments.swiftRide.nautilusSpeedMultiplier: Speed bonus per Tailwind level; range 0-1, also limited by the total speed cap. Default: 0.2. Server setting; restart required.",
        "details": [
          "enchantments.swiftRide.nautilusSpeedMultiplier: Speed bonus per Tailwind level; range 0-1, also limited by the total speed cap. Default: 0.2. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Nautilus-Geschwindigkeitsfaktor",
        "summary": "enchantments.swiftRide.nautilusSpeedMultiplier: Geschwindigkeitsbonus je Rückenwindstufe; Bereich 0-1, zusätzlich durch Gesamtgrenze begrenzt. Standard: 0.2. Servereinstellung; Neustart erforderlich.",
        "details": [
          "enchantments.swiftRide.nautilusSpeedMultiplier: Geschwindigkeitsbonus je Rückenwindstufe; Bereich 0-1, zusätzlich durch Gesamtgrenze begrenzt. Standard: 0.2. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-enchantments-horseJump-nautilusDashMultiplier",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Nautilus Dash Multiplier",
        "summary": "enchantments.horseJump.nautilusDashMultiplier: Dash bonus per Leaping level; range 0-0.5. Vanilla cooldown remains 40 ticks. Default: 0.2. Server setting; restart required.",
        "details": [
          "enchantments.horseJump.nautilusDashMultiplier: Dash bonus per Leaping level; range 0-0.5. Vanilla cooldown remains 40 ticks. Default: 0.2. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Nautilus-Dash-Faktor",
        "summary": "enchantments.horseJump.nautilusDashMultiplier: Dash-Bonus je Sprungkraftstufe; Bereich 0-0.5. Vanilla-Abklingzeit bleibt 40 Ticks. Standard: 0.2. Servereinstellung; Neustart erforderlich.",
        "details": [
          "enchantments.horseJump.nautilusDashMultiplier: Dash-Bonus je Sprungkraftstufe; Bereich 0-0.5. Vanilla-Abklingzeit bleibt 40 Ticks. Standard: 0.2. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-enchantments-horseJump-featherFallingReduction",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Horse Feather Falling Reduction",
        "summary": "enchantments.horseJump.featherFallingReduction: Fall damage reduction per level; range 0-0.12, at most level IV. Default: 0.12. Server setting; restart required.",
        "details": [
          "enchantments.horseJump.featherFallingReduction: Fall damage reduction per level; range 0-0.12, at most level IV. Default: 0.12. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Pferde-Federfall-Reduktion",
        "summary": "enchantments.horseJump.featherFallingReduction: Fallschadensreduktion je Stufe; Bereich 0-0.12, höchstens Stufe IV. Standard: 0.12. Servereinstellung; Neustart erforderlich.",
        "details": [
          "enchantments.horseJump.featherFallingReduction: Fallschadensreduktion je Stufe; Bereich 0-0.12, höchstens Stufe IV. Standard: 0.12. Servereinstellung; Neustart erforderlich."
        ]
      }
    },
    {
      "id": "config-enchantments-horseJump-armorEnchantability",
      "sources": [
        "modules/simpleriding/shared/java/com/simpleriding/RidingConfig.java",
        "modules/simpleriding/shared/java/com/simpleriding/RidingOptions.java",
        "modules/simpleriding/shared/java/com/simpleriding/client/RidingConfigScreen.java"
      ],
      "en": {
        "title": "Mount Armor Enchantability",
        "summary": "enchantments.horseJump.armorEnchantability: Enchanting table power for mount armor; range 0-15. Default: 15. Server setting; restart required.",
        "details": [
          "enchantments.horseJump.armorEnchantability: Enchanting table power for mount armor; range 0-15. Default: 15. Server setting; restart required."
        ]
      },
      "de": {
        "title": "Reittier-Rüstungsverzauberbarkeit",
        "summary": "enchantments.horseJump.armorEnchantability: Zaubertisch-Verzauberbarkeit für Reittier-Rüstung; Bereich 0-15. Standard: 15. Servereinstellung; Neustart erforderlich.",
        "details": [
          "enchantments.horseJump.armorEnchantability: Zaubertisch-Verzauberbarkeit für Reittier-Rüstung; Bereich 0-15. Standard: 15. Servereinstellung; Neustart erforderlich."
        ]
      }
    }
  ],
  "recipes": [],
  "lootTables": [],
  "tags": [
    {
      "id": "simpleriding:item/horse_armor_enchantable",
      "replace": false,
      "values": [
        {
          "id": "minecraft:leather_horse_armor",
          "required": true
        },
        {
          "id": "minecraft:copper_horse_armor",
          "required": true
        },
        {
          "id": "minecraft:iron_horse_armor",
          "required": true
        },
        {
          "id": "minecraft:golden_horse_armor",
          "required": true
        },
        {
          "id": "minecraft:diamond_horse_armor",
          "required": true
        },
        {
          "id": "minecraft:netherite_horse_armor",
          "required": true
        },
        {
          "id": "simplebuilding:enderite_horse_armor",
          "required": false
        }
      ],
      "source": "modules/simpleriding/generated/resources/data/simpleriding/tags/item/horse_armor_enchantable.json"
    },
    {
      "id": "simpleriding:item/mount_armor_enchantable",
      "replace": false,
      "values": [
        {
          "id": "#simpleriding:horse_armor_enchantable",
          "required": true
        },
        {
          "id": "#simpleriding:nautilus_armor_enchantable",
          "required": true
        }
      ],
      "source": "modules/simpleriding/generated/resources/data/simpleriding/tags/item/mount_armor_enchantable.json"
    },
    {
      "id": "simpleriding:item/nautilus_armor_enchantable",
      "replace": false,
      "values": [
        {
          "id": "minecraft:copper_nautilus_armor",
          "required": true
        },
        {
          "id": "minecraft:iron_nautilus_armor",
          "required": true
        },
        {
          "id": "minecraft:golden_nautilus_armor",
          "required": true
        },
        {
          "id": "minecraft:diamond_nautilus_armor",
          "required": true
        },
        {
          "id": "minecraft:netherite_nautilus_armor",
          "required": true
        },
        {
          "id": "simplebuilding:enderite_nautilus_armor",
          "required": false
        }
      ],
      "source": "modules/simpleriding/generated/resources/data/simpleriding/tags/item/nautilus_armor_enchantable.json"
    },
    {
      "id": "simpleriding:item/saddle_enchantable",
      "replace": false,
      "values": [
        {
          "id": "minecraft:saddle",
          "required": true
        },
        {
          "id": "minecraft:black_harness",
          "required": true
        },
        {
          "id": "minecraft:brown_harness",
          "required": true
        },
        {
          "id": "minecraft:white_harness",
          "required": true
        },
        {
          "id": "minecraft:gray_harness",
          "required": true
        },
        {
          "id": "minecraft:light_gray_harness",
          "required": true
        },
        {
          "id": "minecraft:cyan_harness",
          "required": true
        },
        {
          "id": "minecraft:pink_harness",
          "required": true
        },
        {
          "id": "minecraft:red_harness",
          "required": true
        },
        {
          "id": "minecraft:orange_harness",
          "required": true
        },
        {
          "id": "minecraft:yellow_harness",
          "required": true
        },
        {
          "id": "minecraft:lime_harness",
          "required": true
        },
        {
          "id": "minecraft:green_harness",
          "required": true
        },
        {
          "id": "minecraft:magenta_harness",
          "required": true
        },
        {
          "id": "minecraft:purple_harness",
          "required": true
        },
        {
          "id": "minecraft:blue_harness",
          "required": true
        },
        {
          "id": "minecraft:light_blue_harness",
          "required": true
        }
      ],
      "source": "modules/simpleriding/generated/resources/data/simpleriding/tags/item/saddle_enchantable.json"
    }
  ],
  "advancements": [
    {
      "id": "simpleriding:nautilus_equipment",
      "parent": "minecraft:adventure/root",
      "icon": "minecraft:diamond_nautilus_armor",
      "frame": "task",
      "hidden": false,
      "title": {
        "en_us": "Nautilus Equipment"
      },
      "description": {
        "en_us": ""
      },
      "criteria": [
        {
          "name": "armor",
          "trigger": "minecraft:inventory_changed",
          "items": [
            "#simpleriding:nautilus_armor_enchantable"
          ]
        }
      ],
      "needs": "any",
      "source": "modules/simpleriding/generated/resources/data/simpleriding/advancement/nautilus_equipment.json"
    }
  ],
  "enchantments": [
    {
      "id": "simpleriding:leaping",
      "name": {
        "en_us": "Leaping",
        "de_de": "Sprungkraft"
      },
      "description": {
        "en_us": "Increases ridden horse jump strength or nautilus dash strength. Server default: +20% per level; level III maximum.",
        "de_de": "Erhöht Pferdesprungkraft oder Nautilus-Dash beim Reiten. Serverstandard: +20% je Stufe; höchstens III."
      },
      "maxLevel": 3,
      "weight": 3,
      "anvilCost": 4,
      "slots": [
        "armor"
      ],
      "supportedItems": "#simpleriding:mount_armor_enchantable",
      "primaryItems": "#simpleriding:mount_armor_enchantable",
      "exclusiveSet": null,
      "effects": [],
      "implementedIn": "none",
      "hasEffect": false,
      "source": "modules/simpleriding/generated/resources/data/simpleriding/enchantment/leaping.json",
      "note": {
        "en": {
          "title": "Leaping",
          "summary": "Leaping I–III increases the jump strength of a player-controlled horse by 20% per level by default.",
          "details": [
            "Leaping I–III increases the jump strength of a player-controlled horse by 20% per level by default.",
            "Apply its book to horse armor at an anvil. All six vanilla horse armor materials and optional SimpleBuilding Enderite Horse Armor are supported. Removing armor or dismounting clears the modifier.",
            "Nautilus and zombie nautilus armor accepts Leaping too: +20% dash strength per level by default, using the same total speed cap as Tailwind. The server executes the dash and preserves the vanilla 40-tick cooldown.",
            "The total server dash impulse, including prior momentum, is clamped to the configured tick movement limit and at most 3.9 blocks/tick; nonfinite motion is refused."
          ]
        },
        "de": {
          "title": "Sprungkraft",
          "summary": "Sprungkraft I–III steigert die Sprungkraft eines spielergesteuerten Pferdes standardmäßig um 20% je Stufe.",
          "details": [
            "Sprungkraft I–III steigert die Sprungkraft eines spielergesteuerten Pferdes standardmäßig um 20% je Stufe.",
            "Das Buch wird am Amboss auf Pferderüstung angewendet. Alle sechs Vanilla-Materialien und optional SimpleBuildings Enderit-Pferderüstung werden unterstützt. Rüstung entfernen oder absteigen löscht den Modifikator.",
            "Nautilus- und Zombie-Nautilusrüstung erlaubt ebenfalls Sprungkraft: standardmäßig +20% Dash-Kraft je Stufe, mit derselben Gesamtgeschwindigkeitsgrenze wie Rückenwind. Der Server führt den Dash aus und behält Vanillas 40-Tick-Abklingzeit.",
            "Der gesamte Server-Dash-Impuls inklusive vorherigem Schwung wird auf die konfigurierte Tickbewegungsgrenze und höchstens 3.9 Blöcke/Tick begrenzt; nichtendliche Bewegung wird verweigert."
          ]
        }
      }
    },
    {
      "id": "simpleriding:tailwind",
      "name": {
        "en_us": "Tailwind",
        "de_de": "Rückenwind"
      },
      "description": {
        "en_us": "Increases ridden mount speed. Server defaults per level: horses 30%, other mounts and nautiluses 20%, Happy Ghasts 85%; level III maximum.",
        "de_de": "Erhöht Reitgeschwindigkeit. Serverstandard je Stufe: Pferde 30%, andere Reittiere und Nautilusse 20%, Happy Ghasts 85%; höchstens III."
      },
      "maxLevel": 3,
      "weight": 2,
      "anvilCost": 4,
      "slots": [
        "armor"
      ],
      "supportedItems": "#simpleriding:saddle_enchantable",
      "primaryItems": "#simpleriding:saddle_enchantable",
      "exclusiveSet": null,
      "effects": [],
      "implementedIn": "none",
      "hasEffect": false,
      "source": "modules/simpleriding/generated/resources/data/simpleriding/enchantment/tailwind.json",
      "note": {
        "en": {
          "title": "Tailwind",
          "summary": "Tailwind I–III enhances saddles and all 16 colored harnesses. Apply a book at an anvil.",
          "details": [
            "Tailwind I–III enhances saddles and all 16 colored harnesses. Apply a book at an anvil.",
            "Only a player-controlled mount receives the bonus. Each level adds 30% speed on horses and camels, 20% on pigs, striders and nautiluses, and 85% flight speed on Happy Ghasts by default. Enchantment levels are capped at III; the total server bonus is capped at +300%.",
            "The server updates transient attribute modifiers each tick; removing the enchantment or dismounting removes the bonus. A plain ghast harness grants no free boost.",
            "Happy Ghast flight uses flying speed twice in Vanilla. The server applies a square-root attribute bonus so the actual quadratic flight amplification stays within the total speed cap."
          ]
        },
        "de": {
          "title": "Rückenwind",
          "summary": "Rückenwind I–III verbessert Sättel und alle 16 Geschirrfarben. Ein Buch wird am Amboss angewendet.",
          "details": [
            "Rückenwind I–III verbessert Sättel und alle 16 Geschirrfarben. Ein Buch wird am Amboss angewendet.",
            "Nur spielergesteuerte Reittiere erhalten den Bonus: je Stufe standardmäßig 30% für Pferde und Kamele, 20% für Schweine, Schreiter und Nautilusse, 85% Flugtempo für Happy Ghasts. Höchstens Stufe III; der gesamte Serverbonus ist auf +300% begrenzt.",
            "Der Server aktualisiert flüchtige Attributmodifikatoren pro Tick. Verzauberung entfernen oder absteigen entfernt den Bonus. Normales Ghastgeschirr gibt keinen Gratisbonus.",
            "Vanilla nutzt das Happy-Ghast-Flugtempo zweimal. Der Server verwendet deshalb einen Quadratwurzel-Attributbonus, damit die tatsächliche quadratische Flugverstärkung die Gesamtgeschwindigkeitsgrenze einhält."
          ]
        }
      }
    }
  ],
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
    "features": 25,
    "recipes": 0,
    "lootTables": 0,
    "tags": 4,
    "advancements": 1,
    "enchantments": 2,
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
