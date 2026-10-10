window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simpledimensions"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simpledimensions",
    "name": "Simple Dimensions",
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
      "id": "skyblock",
      "sources": [
        "modules/simpledimensions/shared/java/dev/simpledimension/common/portal/DimensionPortalConfig.java",
        "modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/PortalWorld.java",
        "modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/DestinationPlatform.java"
      ],
      "en": {
        "title": "Skyblock",
        "summary": "Build one of six glowstone arches and place a separate emitting block within three blocks of the interior. Glowstone, fire, soul fire, and portal blocks do not count. Ignite with flint and steel or one fire charge. The light blue void has a bedrock arrival island whose diameter is the frame width plus four, and an exact return exit. No free resources are generated.",
        "details": [
          "Build one of six glowstone arches and place a separate emitting block within three blocks of the interior. Glowstone, fire, soul fire, and portal blocks do not count. Ignite with flint and steel or one fire charge. The light blue void has a bedrock arrival island whose diameter is the frame width plus four, and an exact return exit. No free resources are generated."
        ]
      },
      "de": {
        "title": "Skyblock",
        "summary": "Einen der sechs Glowstonebögen bauen und einen zusätzlichen leuchtenden Block höchstens drei Blöcke vom Innenraum setzen. Glowstone, Feuer, Seelenfeuer und Portalblöcke zählen nicht. Mit Feuerzeug oder einer Feuerkugel zünden. Die hellblaue Leere bietet eine Bedrockinsel mit Rahmendurchmesser plus vier und exaktem Rückweg; keine kostenlosen Rohstoffe.",
        "details": [
          "Einen der sechs Glowstonebögen bauen und einen zusätzlichen leuchtenden Block höchstens drei Blöcke vom Innenraum setzen. Glowstone, Feuer, Seelenfeuer und Portalblöcke zählen nicht. Mit Feuerzeug oder einer Feuerkugel zünden. Die hellblaue Leere bietet eine Bedrockinsel mit Rahmendurchmesser plus vier und exaktem Rückweg; keine kostenlosen Rohstoffe."
        ]
      }
    },
    {
      "id": "mining",
      "sources": [
        "modules/simpledimensions/shared/java/dev/simpledimension/common/portal/DimensionPortalConfig.java",
        "modules/simpledimensions/shared/java/dev/simpledimension/common/config/DimensionConfigStore.java"
      ],
      "en": {
        "title": "Mining Dimension",
        "summary": "Portal rows: DXXD / D..D / D..D / D..D / DEED. D is deepslate bricks, X diamond ore, E emerald ore. One origin block maps to two mining blocks. The forest flat preset has bedrock at -64, deepslate through 0, stone through 188, dirt at 189–190, and grass at 191. Vanilla biome features provide ores and trees; terrain is never reset.",
        "details": [
          "Portal rows: DXXD / D..D / D..D / D..D / DEED. D is deepslate bricks, X diamond ore, E emerald ore. One origin block maps to two mining blocks. The forest flat preset has bedrock at -64, deepslate through 0, stone through 188, dirt at 189–190, and grass at 191. Vanilla biome features provide ores and trees; terrain is never reset."
        ]
      },
      "de": {
        "title": "Abbaudimension",
        "summary": "Portalzeilen: DXXD / D..D / D..D / D..D / DEED. D ist Tiefenschieferziegel, X Diamanterz, E Smaragderz. Ein Quellblock entspricht zwei Zielblöcken. Das flache Waldpreset hat Bedrock bei -64, Tiefenschiefer bis 0, Stein bis 188, Erde bei 189–190 und Gras bei 191. Vanilla-Biomfeatures liefern Erze und Bäume; kein Gebietsreset.",
        "details": [
          "Portalzeilen: DXXD / D..D / D..D / D..D / DEED. D ist Tiefenschieferziegel, X Diamanterz, E Smaragderz. Ein Quellblock entspricht zwei Zielblöcken. Das flache Waldpreset hat Bedrock bei -64, Tiefenschiefer bis 0, Stein bis 188, Erde bei 189–190 und Gras bei 191. Vanilla-Biomfeatures liefern Erze und Bäume; kein Gebietsreset."
        ]
      }
    },
    {
      "id": "travel",
      "sources": [
        "modules/simpledimensions/shared/java/dev/simpledimension/common/portal/DimensionPortalConfig.java",
        "modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/DimensionRuntime.java"
      ],
      "en": {
        "title": "Travel Dimension",
        "summary": "Portal rows: KRRK / O..O / O..O / O..O / SSSS. K is a wither skeleton skull, R resin bricks, O dark oak logs, S soul sand. Ten origin blocks map to one travel block. The flat bedrock plane grants no extra movement speed. Generated exits keep exact origins rather than selecting nearby foreign portals.",
        "details": [
          "Portal rows: KRRK / O..O / O..O / O..O / SSSS. K is a wither skeleton skull, R resin bricks, O dark oak logs, S soul sand. Ten origin blocks map to one travel block. The flat bedrock plane grants no extra movement speed. Generated exits keep exact origins rather than selecting nearby foreign portals."
        ]
      },
      "de": {
        "title": "Reisedimension",
        "summary": "Portalzeilen: KRRK / O..O / O..O / O..O / SSSS. K ist Witherskelettschädel, R Harzziegel, O dunkler Eichenstamm, S Seelensand. Zehn Quellblöcke entsprechen einem Zielblock. Die Bedrockebene gibt keinen Tempobonus. Erzeugte Rückwege behalten den exakten Ursprung statt fremder Nachbarportale.",
        "details": [
          "Portalzeilen: KRRK / O..O / O..O / O..O / SSSS. K ist Witherskelettschädel, R Harzziegel, O dunkler Eichenstamm, S Seelensand. Zehn Quellblöcke entsprechen einem Zielblock. Die Bedrockebene gibt keinen Tempobonus. Erzeugte Rückwege behalten den exakten Ursprung statt fremder Nachbarportale."
        ]
      }
    },
    {
      "id": "portal_shapes",
      "sources": [
        "modules/simpledimensions/shared/java/dev/simpledimension/common/portal/DimensionPortalConfig.java",
        "modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/DimensionRuntime.java"
      ],
      "en": {
        "title": "Portal Shapes",
        "summary": "The six default matrices are AGA/G.G/G.G; AGGA/G..G/G..G/G..G; AGGGA/G...G/G...G/G...G; AAGGAA/AG..GA/G....G/G....G/G....G; AAGGGAA/AG...GA/G.....G/G.....G/G.....G; AAGGGGAA/AG....GA/G......G/G......G/G......G. A is checked air, G glowstone, and dots become portal cells. Both horizontal axes work. Breaking the frame or removing required light dissolves outbound portal cells.",
        "details": [
          "The six default matrices are AGA/G.G/G.G; AGGA/G..G/G..G/G..G; AGGGA/G...G/G...G/G...G; AAGGAA/AG..GA/G....G/G....G/G....G; AAGGGAA/AG...GA/G.....G/G.....G/G.....G; AAGGGGAA/AG....GA/G......G/G......G/G......G. A is checked air, G glowstone, and dots become portal cells. Both horizontal axes work. Breaking the frame or removing required light dissolves outbound portal cells."
        ]
      },
      "de": {
        "title": "Portalformen",
        "summary": "Die sechs Glowstonebögen bleiben Standard und verlangen separates Licht. A ist geprüfte Luft, G Glowstone, Punkte werden Portalfläche. Beide horizontalen Achsen funktionieren. Rahmenbruch oder fehlendes Pflichtlicht löst Hinportalblöcke auf.",
        "details": [
          "Die sechs Glowstonebögen bleiben Standard und verlangen separates Licht. A ist geprüfte Luft, G Glowstone, Punkte werden Portalfläche. Beide horizontalen Achsen funktionieren. Rahmenbruch oder fehlendes Pflichtlicht löst Hinportalblöcke auf."
        ]
      }
    },
    {
      "id": "safe_return",
      "sources": [
        "modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/DimensionRuntime.java",
        "modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/ConfigLimits.java"
      ],
      "en": {
        "title": "Safe Travel and Return",
        "summary": "Only unmounted players travel. Items, mobs, mounts, and passengers are refused. Target registries, borders, build height, solid ground, headroom, claim permissions, and an exact return path are checked. Builds use only empty cells and bedrock; valuable frames are never copied. In terrain dimensions, the platform is raised above every existing block in its bounded footprint, including nonblocking plants. Vegetation and existing builds are preserved. One build per server tick, at most nine chunks per build and 1,024 recorded portals. Warmup is 0–200 ticks, cooldown 20–1,200 ticks, and players must leave the portal after arrival. Generated exits remain usable when access is disabled. Missing definitions or falling near the void floor invokes the persisted safe origin; otherwise a checked overworld spawn is used.",
        "details": [
          "Only unmounted players travel. Items, mobs, mounts, and passengers are refused. Target registries, borders, build height, solid ground, headroom, claim permissions, and an exact return path are checked. Builds use only empty cells and bedrock; valuable frames are never copied. In terrain dimensions, the platform is raised above every existing block in its bounded footprint, including nonblocking plants. Vegetation and existing builds are preserved. One build per server tick, at most nine chunks per build and 1,024 recorded portals. Warmup is 0–200 ticks, cooldown 20–1,200 ticks, and players must leave the portal after arrival. Generated exits remain usable when access is disabled. Missing definitions or falling near the void floor invokes the persisted safe origin; otherwise a checked overworld spawn is used."
        ]
      },
      "de": {
        "title": "Sichere Reise und Rückkehr",
        "summary": "Nur unberittene Spieler reisen; Items, Mobs, Reittiere und Passagiere werden verweigert. Zielregistry, Weltgrenze, Bauhöhe, Boden, Kopffreiheit, Claimfreigabe und exakter Rückweg werden geprüft. Bau nur in Luft mit Bedrock; wertvolle Rahmen werden nie kopiert. In Dimensionen mit Terrain liegt die Plattform oberhalb aller vorhandenen Bloecke innerhalb ihrer begrenzten Grundflaeche, auch oberhalb nicht kollidierender Pflanzen. Vegetation und bestehende Bauten bleiben erhalten. Ein Bau pro Servertick, höchstens neun Chunks je Bau und 1.024 gespeicherte Portale. Wartezeit 0–200, Reisesperre 20–1.200 Ticks; nach Ankunft Portal verlassen. Erzeugte Rückwege funktionieren auch bei abgeschaltetem Zugang. Fehlende Definitionen oder Nähe zum Leerboden lösen Rückkehr zum gespeicherten sicheren Ursprung aus, sonst zu geprüftem Oberweltspawn.",
        "details": [
          "Nur unberittene Spieler reisen; Items, Mobs, Reittiere und Passagiere werden verweigert. Zielregistry, Weltgrenze, Bauhöhe, Boden, Kopffreiheit, Claimfreigabe und exakter Rückweg werden geprüft. Bau nur in Luft mit Bedrock; wertvolle Rahmen werden nie kopiert. In Dimensionen mit Terrain liegt die Plattform oberhalb aller vorhandenen Bloecke innerhalb ihrer begrenzten Grundflaeche, auch oberhalb nicht kollidierender Pflanzen. Vegetation und bestehende Bauten bleiben erhalten. Ein Bau pro Servertick, höchstens neun Chunks je Bau und 1.024 gespeicherte Portale. Wartezeit 0–200, Reisesperre 20–1.200 Ticks; nach Ankunft Portal verlassen. Erzeugte Rückwege funktionieren auch bei abgeschaltetem Zugang. Fehlende Definitionen oder Nähe zum Leerboden lösen Rückkehr zum gespeicherten sicheren Ursprung aus, sonst zu geprüftem Oberweltspawn."
        ]
      }
    },
    {
      "id": "configuration",
      "sources": [
        "modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/ConfigLimits.java",
        "modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/client/DimensionConfigScreen.java",
        "modules/simpledimensions/shared/java/dev/simpledimension/common/config/DimensionConfigStore.java",
        "modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/DimensionSettings.java"
      ],
      "en": {
        "title": "Configuration and Data Packs",
        "summary": "Server JSON: config/simpledimension/server.json. Portal definitions: config/simpledimension/dimensions/*.json; existing files are preserved. The mandatory generated pack loads on both loaders before world registries. At most 16 definitions, eight recipes each, 23×23 matrices, 21×21 interiors, eight extra origins, 64 KiB per input file, and coordinate ratios 0.5–10. Generation height is 16–384 in 16-block steps, minimum Y -64–0. Destinations stay in the isolated simpledimension namespace. New dimensions require a world restart. preset and orePreset are retained legacy labels without generation effects. Access/Safety/Dimensions config tabs show defaults; multiplayer clients cannot edit server values. Nonplayer travel is reserved and permanently refused. Known claim mods require a registered permission integration; no claim bypass is offered. The Dimensions tab controls Skyblock, Mining, and Travel independently, all On by default. These server settings persist in server.json and apply immediately to the local integrated server. Dedicated servers read them at startup; remote clients cannot change them. Disabling access preserves dimension data and generated return portals. The old enabled flag in built-in dimension definitions is ignored; it remains supported for custom definitions.",
        "details": [
          "Server JSON: config/simpledimension/server.json. Portal definitions: config/simpledimension/dimensions/*.json; existing files are preserved. The mandatory generated pack loads on both loaders before world registries. At most 16 definitions, eight recipes each, 23×23 matrices, 21×21 interiors, eight extra origins, 64 KiB per input file, and coordinate ratios 0.5–10. Generation height is 16–384 in 16-block steps, minimum Y -64–0. Destinations stay in the isolated simpledimension namespace. New dimensions require a world restart. preset and orePreset are retained legacy labels without generation effects. Access/Safety/Dimensions config tabs show defaults; multiplayer clients cannot edit server values. Nonplayer travel is reserved and permanently refused. Known claim mods require a registered permission integration; no claim bypass is offered. The Dimensions tab controls Skyblock, Mining, and Travel independently, all On by default. These server settings persist in server.json and apply immediately to the local integrated server. Dedicated servers read them at startup; remote clients cannot change them. Disabling access preserves dimension data and generated return portals. The old enabled flag in built-in dimension definitions is ignored; it remains supported for custom definitions.",
          "accessEnabled – Enable Portal Access: New outbound journeys can be disabled without disabling generated exits. Default: true. Server setting.",
          "skyblockEnabled – Enable Skyblock: Allow new journeys to Skyblock. Turning this off keeps existing return paths and world data. Default: On. Server setting.",
          "miningEnabled – Enable Mining: Allow new journeys to Mining. Turning this off keeps existing return paths and world data. Default: On. Server setting.",
          "travelEnabled – Enable Travel: Allow new journeys to Travel. Turning this off keeps existing return paths and world data. Default: On. Server setting.",
          "automaticDestination – Create Safe Destinations: Build a checked bedrock island and exact return portal. Disabling this refuses first journeys. Default: true. Server setting.",
          "portalDelayTicks – Portal Warmup (Ticks): Minimum warmup for all definitions; range 0–200 ticks. Default: 0. Server setting.",
          "teleportCooldownTicks – Travel Cooldown (Ticks): Minimum cooldown; 20–1200 ticks. Leave the portal before traveling again. Default: 60. Server setting.",
          "nonPlayerTravel – Nonplayer Travel (Reserved): Locked off: items, mobs, mounts, and passengers are never transported. Default: false. Server setting.",
          "id – Definition ID: Advanced server JSON option. Default: \"skyblock\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "sourceDimensionId – Origin Dimension: Advanced server JSON option. Default: \"minecraft:overworld\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "targetDimensionId – Destination Dimension: Advanced server JSON option. Default: \"simpledimension:skyblock\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "targetDisplayName – Destination Name: Advanced server JSON option. Default: \"Skyblock\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "enabled – Enable Custom Definition (Legacy): Custom definitions only. Skyblock, Mining, and Travel use the Dimensions tab in server settings instead. Existing built-in enabled values are ignored. Default: true.",
          "requireSeparateLight – Require Separate Light: Advanced server JSON option. Default: true. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "portalColorHex – Portal Color: Advanced server JSON option. Default: \"#66D9FF\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "portalRecipes – Portal Shapes: Advanced server JSON option. Default: \"six glowstone arches\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "frameBlock – Rectangular Frame Block: Advanced server JSON option. Default: \"minecraft:crying_obsidian\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "minPortalWidth – Minimum Interior Width: Advanced server JSON option. Default: 2. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "maxPortalWidth – Maximum Interior Width: Advanced server JSON option. Default: 21. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "minPortalHeight – Minimum Interior Height: Advanced server JSON option. Default: 3. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "maxPortalHeight – Maximum Interior Height: Advanced server JSON option. Default: 21. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "travelCoordinateScale – Source Blocks per Destination Block: Advanced server JSON option. Default: 1.0. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "allowIgniteFromSource – Ignite at Origin: Advanced server JSON option. Default: true. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "allowIgniteFromTarget – Ignite at Destination: Advanced server JSON option. Default: false. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "openFromDimensions – Additional Origins: Advanced server JSON option. Default: []. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "generateReturnPortalOnArrival – Generate Return Portal: Advanced server JSON option. Default: true. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "createDestinationPlatform – Create Destination Island: Advanced server JSON option. Default: true. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.preset – Preset (legacy): Advanced server JSON option. Default: \"skyblock_void\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.generatorType – Generator type: Advanced server JSON option. Default: \"flat\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.baseBiome – Base biome: Advanced server JSON option. Default: \"minecraft:the_void\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.biomeSelection – Biome selection: Advanced server JSON option. Default: [\"minecraft:plains\"]. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.minY – Minimum Y: Advanced server JSON option. Default: -64. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.height – Height: Advanced server JSON option. Default: 384. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.logicalHeight – Logical height: Advanced server JSON option. Default: 384. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.hasSkylight – Has skylight: Advanced server JSON option. Default: true. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.hasCeiling – Has ceiling: Advanced server JSON option. Default: false. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.ultraWarm – Ultra warm: Advanced server JSON option. Default: false. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.natural – Natural: Advanced server JSON option. Default: true. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.coordinateScale – Coordinate scale: Advanced server JSON option. Default: 1.0. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.generateOres – Generate ores: Advanced server JSON option. Default: false. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.orePreset – Ore preset (legacy): Advanced server JSON option. Default: \"minecraft:overworld\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.flatLayers – Flat layers: Advanced server JSON option. Default: []. World generation requires a world restart; preset and orePreset are legacy labels only.",
          "portalRecipes.rows – Frame rows: Advanced server JSON option. Default: \"shape rows\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "portalRecipes.legend – Frame legend: Advanced server JSON option. Default: \"symbol to block ID\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "portalRecipes.interior – Frame interior: Advanced server JSON option. Default: \".\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "portalRecipes.ignore – Ignored blocks: Advanced server JSON option. Default: \" \". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.flatLayers.block – Layer block: Advanced server JSON option. Default: \"minecraft:stone\". World generation requires a world restart; preset and orePreset are legacy labels only.",
          "worldGeneration.flatLayers.height – Layer height: Advanced server JSON option. Default: 1. World generation requires a world restart; preset and orePreset are legacy labels only."
        ]
      },
      "de": {
        "title": "Konfiguration und Datenpakete",
        "summary": "Server-JSON: config/simpledimension/server.json. Portaldefinitionen: config/simpledimension/dimensions/*.json; bestehende Dateien bleiben erhalten. Das erforderliche generierte Pack lädt auf beiden Loadern vor den Weltregistries. Höchstens 16 Definitionen, acht Formen je Definition, 23×23 Matrizen, 21×21 Innenraum, acht Zusatzursprünge, 64 KiB je Eingabedatei, Verhältnis 0,5–10. Generationshöhe 16–384 in 16er-Schritten, minY -64–0. Ziele bleiben im isolierten Namensraum simpledimension. Neue Dimensionen brauchen Weltneustart. preset und orePreset bleiben wirkungslose Legacybezeichnungen. Reiter Zugang/Sicherheit zeigen Standards; Mehrspielerclients ändern keine Serverwerte. Nichtspielerreise ist reserviert und dauerhaft verweigert. Bekannte Claimmods benötigen registrierte Freigabeintegration; kein Claim-Bypass. Der eigene Reiter Dimensionen schaltet Skyblock, Abbau und Reise einzeln; Standard ist jeweils Ein. Diese Serveroptionen werden in server.json gespeichert und auf dem lokalen integrierten Server sofort wirksam. Dedizierte Server lesen sie beim Start; entfernte Clients können sie nicht ändern. Abschalten erhält Weltdaten und erzeugte Rückportale. Alte enabled-Werte in vorinstallierten Dimensionsdefinitionen werden ignoriert; eigene Definitionen behalten diesen Schalter.",
        "details": [
          "Server-JSON: config/simpledimension/server.json. Portaldefinitionen: config/simpledimension/dimensions/*.json; bestehende Dateien bleiben erhalten. Das erforderliche generierte Pack lädt auf beiden Loadern vor den Weltregistries. Höchstens 16 Definitionen, acht Formen je Definition, 23×23 Matrizen, 21×21 Innenraum, acht Zusatzursprünge, 64 KiB je Eingabedatei, Verhältnis 0,5–10. Generationshöhe 16–384 in 16er-Schritten, minY -64–0. Ziele bleiben im isolierten Namensraum simpledimension. Neue Dimensionen brauchen Weltneustart. preset und orePreset bleiben wirkungslose Legacybezeichnungen. Reiter Zugang/Sicherheit zeigen Standards; Mehrspielerclients ändern keine Serverwerte. Nichtspielerreise ist reserviert und dauerhaft verweigert. Bekannte Claimmods benötigen registrierte Freigabeintegration; kein Claim-Bypass. Der eigene Reiter Dimensionen schaltet Skyblock, Abbau und Reise einzeln; Standard ist jeweils Ein. Diese Serveroptionen werden in server.json gespeichert und auf dem lokalen integrierten Server sofort wirksam. Dedizierte Server lesen sie beim Start; entfernte Clients können sie nicht ändern. Abschalten erhält Weltdaten und erzeugte Rückportale. Alte enabled-Werte in vorinstallierten Dimensionsdefinitionen werden ignoriert; eigene Definitionen behalten diesen Schalter.",
          "accessEnabled – Portalzugang erlauben: Neue Hinreisen abschalten; erzeugte Rückwege bleiben benutzbar. Standard: true. Servereinstellung.",
          "skyblockEnabled – Skyblock erlauben: Neue Reisen zur Dimension Skyblock erlauben. Abschalten erhält bestehende Rückwege und Weltdaten. Standard: Ein. Servereinstellung.",
          "miningEnabled – Abbau erlauben: Neue Reisen zur Dimension Abbau erlauben. Abschalten erhält bestehende Rückwege und Weltdaten. Standard: Ein. Servereinstellung.",
          "travelEnabled – Reise erlauben: Neue Reisen zur Dimension Reise erlauben. Abschalten erhält bestehende Rückwege und Weltdaten. Standard: Ein. Servereinstellung.",
          "automaticDestination – Sichere Ziele erzeugen: Geprüfte Bedrockinsel und exakten Rückweg bauen. Aus verweigert erste Reisen. Standard: true. Servereinstellung.",
          "portalDelayTicks – Portalwartezeit (Ticks): Mindestwartezeit für alle Definitionen; 0–200 Ticks. Standard: 0. Servereinstellung.",
          "teleportCooldownTicks – Reisesperre (Ticks): Mindestpause; 20–1200 Ticks. Vor erneuter Reise das Portal verlassen. Standard: 60. Servereinstellung.",
          "nonPlayerTravel – Nichtspielerreise (reserviert): Fest ausgeschaltet: Items, Mobs, Reittiere und Passagiere reisen nicht. Standard: false. Servereinstellung.",
          "id – Definitions-ID: Erweiterte serverseitige JSON-Option. Standard: \"skyblock\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "sourceDimensionId – Ursprungsdimension: Erweiterte serverseitige JSON-Option. Standard: \"minecraft:overworld\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "targetDimensionId – Zieldimension: Erweiterte serverseitige JSON-Option. Standard: \"simpledimension:skyblock\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "targetDisplayName – Zielname: Erweiterte serverseitige JSON-Option. Standard: \"Skyblock\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "enabled – Eigene Definition erlauben (Altwert): Nur eigene Definitionen. Skyblock, Abbau und Reise verwenden den Reiter Dimensionen der Servereinstellungen. Alte enabled-Werte der vorinstallierten Definitionen werden ignoriert. Standard: true.",
          "requireSeparateLight – Zusatzlicht verlangen: Erweiterte serverseitige JSON-Option. Standard: true. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "portalColorHex – Portalfarbe: Erweiterte serverseitige JSON-Option. Standard: \"#66D9FF\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "portalRecipes – Portalformen: Erweiterte serverseitige JSON-Option. Standard: \"six glowstone arches\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "frameBlock – Rechteck-Rahmenblock: Erweiterte serverseitige JSON-Option. Standard: \"minecraft:crying_obsidian\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "minPortalWidth – Minimale Innenbreite: Erweiterte serverseitige JSON-Option. Standard: 2. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "maxPortalWidth – Maximale Innenbreite: Erweiterte serverseitige JSON-Option. Standard: 21. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "minPortalHeight – Minimale Innenhöhe: Erweiterte serverseitige JSON-Option. Standard: 3. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "maxPortalHeight – Maximale Innenhöhe: Erweiterte serverseitige JSON-Option. Standard: 21. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "travelCoordinateScale – Quellblöcke je Zielblock: Erweiterte serverseitige JSON-Option. Standard: 1.0. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "allowIgniteFromSource – Am Ursprung zünden: Erweiterte serverseitige JSON-Option. Standard: true. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "allowIgniteFromTarget – Am Ziel zünden: Erweiterte serverseitige JSON-Option. Standard: false. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "openFromDimensions – Weitere Ursprünge: Erweiterte serverseitige JSON-Option. Standard: []. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "generateReturnPortalOnArrival – Rückportal erzeugen: Erweiterte serverseitige JSON-Option. Standard: true. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "createDestinationPlatform – Zielinsel erzeugen: Erweiterte serverseitige JSON-Option. Standard: true. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.preset – Vorlage (veraltet): Erweiterte serverseitige JSON-Option. Standard: \"skyblock_void\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.generatorType – Generatortyp: Erweiterte serverseitige JSON-Option. Standard: \"flat\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.baseBiome – Basisbiom: Erweiterte serverseitige JSON-Option. Standard: \"minecraft:the_void\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.biomeSelection – Biomauswahl: Erweiterte serverseitige JSON-Option. Standard: [\"minecraft:plains\"]. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.minY – Minimale Y-Höhe: Erweiterte serverseitige JSON-Option. Standard: -64. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.height – Höhe: Erweiterte serverseitige JSON-Option. Standard: 384. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.logicalHeight – Logische Höhe: Erweiterte serverseitige JSON-Option. Standard: 384. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.hasSkylight – Hat Himmelslicht: Erweiterte serverseitige JSON-Option. Standard: true. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.hasCeiling – Hat Decke: Erweiterte serverseitige JSON-Option. Standard: false. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.ultraWarm – Ultraheiß: Erweiterte serverseitige JSON-Option. Standard: false. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.natural – Natürlich: Erweiterte serverseitige JSON-Option. Standard: true. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.coordinateScale – Koordinatenmaßstab: Erweiterte serverseitige JSON-Option. Standard: 1.0. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.generateOres – Erze erzeugen: Erweiterte serverseitige JSON-Option. Standard: false. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.orePreset – Erzvorlage (veraltet): Erweiterte serverseitige JSON-Option. Standard: \"minecraft:overworld\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.flatLayers – Flache Schichten: Erweiterte serverseitige JSON-Option. Standard: []. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "portalRecipes.rows – Rahmenzeilen: Erweiterte serverseitige JSON-Option. Standard: \"shape rows\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "portalRecipes.legend – Rahmenlegende: Erweiterte serverseitige JSON-Option. Standard: \"symbol to block ID\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "portalRecipes.interior – Rahmeninnenraum: Erweiterte serverseitige JSON-Option. Standard: \".\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "portalRecipes.ignore – Ignorierte Blöcke: Erweiterte serverseitige JSON-Option. Standard: \" \". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.flatLayers.block – Schichtblock: Erweiterte serverseitige JSON-Option. Standard: \"minecraft:stone\". Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen.",
          "worldGeneration.flatLayers.height – Schichthöhe: Erweiterte serverseitige JSON-Option. Standard: 1. Weltgeneration benötigt einen Weltneustart; preset und orePreset sind nur alte Bezeichnungen."
        ]
      }
    },
    {
      "id": "claims_adapter",
      "sources": [
        "framework/src/main/java/com/simplebuilding/framework/api/Protection.java",
        "modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/DimensionRuntime.java",
        "modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/PortalActivation.java"
      ],
      "en": {
        "title": "Server Claim Permissions",
        "summary": "Enabled Simple Tweaks claims are checked through the public framework API. Activation checks every frame and interior cell; travel checks the source, linked destination, landing, and every generated platform cell. Disabled or absent Simple Tweaks adds no restrictions.",
        "details": [
          "Owner and trusted-player access follows the server ledger; revocation takes effect on the next attempt. OP4 bypass follows the claim provider configuration.",
          "Generated exits allow escape from a newly denied source. A denied original return location is never entered: the server searches for an authorized safe landing, then checks the overworld spawn area. If no safe authorized location exists, travel is refused and an administrator must help. No claim blocks are overwritten.",
          "Detected unsupported foreign claim mods still require their own permission adapter. A registered framework provider does not clear that separate gate. Simple Tweaks remains disabled by default; stage 4 is a separate unmerged worker, not established coverage."
        ]
      },
      "de": {
        "title": "Serverseitige Claim-Rechte",
        "summary": "Aktivierte Simple-Tweaks-Claims werden über die öffentliche Framework-API geprüft. Zündung prüft jede Rahmen- und Innenzelle; Reisen prüfen Quelle, verbundenes Ziel, Landung und jede erzeugte Plattformzelle. Ausgeschaltetes oder fehlendes Simple Tweaks fügt keine Beschränkungen hinzu.",
        "details": [
          "Besitzer- und Vertrauensrechte stammen aus den Serverdaten; Widerruf gilt beim nächsten Versuch. OP4-Umgehung folgt der Claim-Konfiguration.",
          "Erzeugte Ausgänge erlauben das Verlassen einer nachträglich gesperrten Quelle. Ein gesperrter Ursprung wird nie betreten: Der Server sucht eine erlaubte sichere Landung und danach am Oberweltspawn. Ohne sicheren erlaubten Ort bleibt die Reise verweigert und ein Administrator muss helfen. Keine Claim-Bauten werden überschrieben.",
          "Erkannte nicht unterstützte Claim-Mods benötigen weiterhin ihren eigenen Adapter. Ein Framework-Anbieter hebt diese getrennte Sperre nicht auf. Simple Tweaks bleibt standardmäßig aus; Stufe 4 läuft separat und ungemergt, ihr Schutz ist nicht bestätigt."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simpledimension:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simpledimension:guide_book",
        "count": 1
      },
      "source": "modules/simpledimensions/shared/resources/data/simpledimension/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:flint_and_steel"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:flint_and_steel"
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
            "id": "minecraft:flint",
            "count": 1
          },
          {
            "id": "minecraft:iron_ingot",
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
  "tags": [],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "simpledimension:guide_book",
      "name": {
        "en_us": "Simple Dimensions Guide",
        "de_de": "Simple-Dimensions-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/guide/DimensionsGuide.java",
          "modules/simpledimensions/shared/resources/data/simpledimension/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 26 pages taken from this wiki, shown in your language. Shapeless recipe: book + flint and steel. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Dimensions\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 26 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Feuerzeug. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Dimensions\" eins."
        }
      },
      "texture": "assets/textures/simpledimensions/item/guide_book.png",
      "craftedBy": [
        "simpledimension:guide_book"
      ],
      "usedIn": []
    }
  ],
  "blocks": [
    {
      "id": "simpledimension:light_blue_portal",
      "kind": "block",
      "name": {
        "en_us": "Legacy Light Blue Portal",
        "de_de": "Altes hellblaues Portal"
      },
      "note": {
        "en": {
          "summary": "Legacy block, block entity, and item ID retained for world loading. No survival crafting recipe, loot source, or creative tab entry."
        },
        "de": {
          "summary": "Alte Block-, Blockentitäts- und Item-ID bleiben ladbar. Kein Survivalrezept, Beutequelle oder Kreativtab-Eintrag."
        }
      },
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simpledimension:sky_portal",
      "kind": "block",
      "name": {
        "en_us": "Sky Portal",
        "de_de": "Himmelsportal"
      },
      "note": {
        "en": {
          "summary": "An ignited in-world portal surface, not a craftable item. It has no survival drop. Tint and destination are saved in its block entity."
        },
        "de": {
          "summary": "Gezündete Portalfläche, kein herstellbares Item und kein Survivaldrop. Farbe und Ziel werden in der Blockentität gespeichert."
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
    "features": 7,
    "recipes": 1,
    "lootTables": 0,
    "tags": 0,
    "advancements": 0,
    "enchantments": 0,
    "items": 1,
    "blocks": 2,
    "trades": 0,
    "config": 0,
    "quests": 0,
    "recipesOtherLines": 0,
    "undocumented": 0,
    "inWorld": 0,
    "incompleteProse": 0
  }
};
