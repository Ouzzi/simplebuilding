window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplevisuals"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplevisuals",
    "name": "Simple Visuals",
    "version": "1.0.7",
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
      "id": "speed_lines",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Speed lines",
        "summary": "Bounded radial lines at high speed or acceleration; cosmetic only, first-person view.",
        "details": [
          "Bounded radial lines at high speed or acceleration; cosmetic only, first-person view."
        ]
      },
      "de": {
        "title": "Geschwindigkeitslinien",
        "summary": "Begrenzte radiale Linien bei hohem Tempo oder Beschleunigung; rein optisch, in der Egoansicht.",
        "details": [
          "Begrenzte radiale Linien bei hohem Tempo oder Beschleunigung; rein optisch, in der Egoansicht."
        ]
      }
    },
    {
      "id": "pickup_notifier",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Pickup notifications",
        "summary": "Server pickup animations create up to eight short-lived item or XP rows, with four layouts, scale, side, opacity and rarity colors. No inventory mutation.",
        "details": [
          "Server pickup animations create up to eight short-lived item or XP rows, with four layouts, scale, side, opacity and rarity colors. No inventory mutation."
        ]
      },
      "de": {
        "title": "Aufnahmehinweise",
        "summary": "Server-Aufnahmeanimationen erzeugen bis zu acht kurzlebige Item- oder XP-Zeilen mit vier Anordnungen, Größe, Seite, Deckkraft und Seltenheitsfarben. Keine Inventaränderung.",
        "details": [
          "Server-Aufnahmeanimationen erzeugen bis zu acht kurzlebige Item- oder XP-Zeilen mit vier Anordnungen, Größe, Seite, Deckkraft und Seltenheitsfarben. Keine Inventaränderung."
        ]
      }
    },
    {
      "id": "elytra_pitch",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Elytra pitch helper",
        "summary": "Two target lines while gliding; defaults -40 and 40 degrees, tolerance 10, sensitivity 4. Does not change flight.",
        "details": [
          "Two target lines while gliding; defaults -40 and 40 degrees, tolerance 10, sensitivity 4. Does not change flight."
        ]
      },
      "de": {
        "title": "Elytra-Neigungshilfe",
        "summary": "Zwei Ziellinien beim Gleiten; Standard -40 und 40 Grad, Toleranz 10, Empfindlichkeit 4. Verändert den Flug nicht.",
        "details": [
          "Zwei Ziellinien beim Gleiten; Standard -40 und 40 Grad, Toleranz 10, Empfindlichkeit 4. Verändert den Flug nicht."
        ]
      }
    },
    {
      "id": "status_bars",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Status effect duration bars",
        "summary": "HUD and inventory bars compare the current duration with the greatest observed duration of that active effect. Infinite effects have no bar; reconnecting clears history.",
        "details": [
          "HUD and inventory bars compare the current duration with the greatest observed duration of that active effect. Infinite effects have no bar; reconnecting clears history."
        ]
      },
      "de": {
        "title": "Effekt-Zeitleisten",
        "summary": "HUD- und Inventarleisten vergleichen die Restdauer mit der größten beobachteten Dauer des aktiven Effekts. Unendliche Effekte haben keine Leiste; erneutes Verbinden löscht den Verlauf.",
        "details": [
          "HUD- und Inventarleisten vergleichen die Restdauer mit der größten beobachteten Dauer des aktiven Effekts. Unendliche Effekte haben keine Leiste; erneutes Verbinden löscht den Verlauf."
        ]
      }
    },
    {
      "id": "player_locator",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Locator player heads",
        "summary": "Uses only server-supplied Vanilla waypoints; replaces eligible icons with player heads and keeps Vanilla angles, visibility and elevation arrows. SimpleBuilding contextual bars keep their priority.",
        "details": [
          "Uses only server-supplied Vanilla waypoints; replaces eligible icons with player heads and keeps Vanilla angles, visibility and elevation arrows. SimpleBuilding contextual bars keep their priority."
        ]
      },
      "de": {
        "title": "Spielerköpfe im Locator",
        "summary": "Verwendet nur serverseitig übermittelte Vanilla-Wegpunkte; ersetzt passende Symbole durch Spielerköpfe und bewahrt Vanilla-Winkel, Sichtbarkeit und Höhenpfeile. SimpleBuilding-Leisten behalten ihre Priorität.",
        "details": [
          "Verwendet nur serverseitig übermittelte Vanilla-Wegpunkte; ersetzt passende Symbole durch Spielerköpfe und bewahrt Vanilla-Winkel, Sichtbarkeit und Höhenpfeile. SimpleBuilding-Leisten behalten ihre Priorität."
        ]
      }
    },
    {
      "id": "chat_heads",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Chat player heads",
        "summary": "Faces beside recognizable player messages use cached Vanilla skins. No skin download service or custom chat packets.",
        "details": [
          "Faces beside recognizable player messages use cached Vanilla skins. No skin download service or custom chat packets."
        ]
      },
      "de": {
        "title": "Spielerköpfe im Chat",
        "summary": "Gesichter neben erkennbaren Spielernachrichten verwenden zwischengespeicherte Vanilla-Skins. Kein eigener Skin-Dienst oder Chatpaket.",
        "details": [
          "Gesichter neben erkennbaren Spielernachrichten verwenden zwischengespeicherte Vanilla-Skins. Kein eigener Skin-Dienst oder Chatpaket."
        ]
      }
    },
    {
      "id": "death_coordinates",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Local death coordinates",
        "summary": "Disabled, appended, or separate local coordinate line for your own death message. Appended is still local, never public; other players' positions are not revealed.",
        "details": [
          "Disabled, appended, or separate local coordinate line for your own death message. Appended is still local, never public; other players' positions are not revealed."
        ]
      },
      "de": {
        "title": "Lokale Todeskoordinaten",
        "summary": "Ausgeschaltete, angehängte oder separate lokale Koordinatenzeile für die eigene Todesnachricht. Auch angehängte Angaben bleiben lokal und werden nie öffentlich; fremde Positionen werden nicht verraten.",
        "details": [
          "Ausgeschaltete, angehängte oder separate lokale Koordinatenzeile für die eigene Todesnachricht. Auch angehängte Angaben bleiben lokal und werden nie öffentlich; fremde Positionen werden nicht verraten."
        ]
      }
    },
    {
      "id": "map_tooltips",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Map preview tooltips",
        "summary": "A 128-pixel map preview, center coordinates and dimension id from map data already available to this client.",
        "details": [
          "A 128-pixel map preview, center coordinates and dimension id from map data already available to this client."
        ]
      },
      "de": {
        "title": "Kartenvorschau im Tooltip",
        "summary": "Eine 128-Pixel-Kartenvorschau, Mittelpunkt und Dimensions-ID aus bereits auf dem Client vorhandenen Kartendaten.",
        "details": [
          "Eine 128-Pixel-Kartenvorschau, Mittelpunkt und Dimensions-ID aus bereits auf dem Client vorhandenen Kartendaten."
        ]
      }
    },
    {
      "id": "held_item",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Held item information",
        "summary": "Durability and up to eight enchantment lines accompany the Vanilla held-item name. Enchantment changes refresh the notification.",
        "details": [
          "Durability and up to eight enchantment lines accompany the Vanilla held-item name. Enchantment changes refresh the notification."
        ]
      },
      "de": {
        "title": "Informationen zum gehaltenen Item",
        "summary": "Haltbarkeit und bis zu acht Verzauberungszeilen ergänzen den Vanilla-Itemnamen. Verzauberungsänderungen erneuern den Hinweis.",
        "details": [
          "Haltbarkeit und bis zu acht Verzauberungszeilen ergänzen den Vanilla-Itemnamen. Verzauberungsänderungen erneuern den Hinweis."
        ]
      }
    },
    {
      "id": "damage_indicators",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Damage indicators",
        "summary": "Up to 32 short-lived numbers for observed health decreases of nearby visible living entities. Values above eight use the special color; this does not prove a critical or Smite hit. Solid blocks hide numbers.",
        "details": [
          "Up to 32 short-lived numbers for observed health decreases of nearby visible living entities. Values above eight use the special color; this does not prove a critical or Smite hit. Solid blocks hide numbers."
        ]
      },
      "de": {
        "title": "Schadensanzeigen",
        "summary": "Bis zu 32 kurzlebige Zahlen für beobachtete Lebensverluste naher sichtbarer Lebewesen. Werte über acht nutzen die Sonderfarbe; dies beweist keinen kritischen oder Bann-Treffer. Feste Blöcke verbergen die Zahlen.",
        "details": [
          "Bis zu 32 kurzlebige Zahlen für beobachtete Lebensverluste naher sichtbarer Lebewesen. Werte über acht nutzen die Sonderfarbe; dies beweist keinen kritischen oder Bann-Treffer. Feste Blöcke verbergen die Zahlen."
        ]
      }
    },
    {
      "id": "biome_info",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Biome notifications",
        "summary": "Optional biome name on transition, with display duration, offset and re-entry cooldown. Disabled by default.",
        "details": [
          "Optional biome name on transition, with display duration, offset and re-entry cooldown. Disabled by default."
        ]
      },
      "de": {
        "title": "Biomhinweise",
        "summary": "Optionaler Biomname beim Wechsel mit Anzeigedauer, Versatz und Wiederkehr-Sperre. Standardmäßig aus.",
        "details": [
          "Optionaler Biomname beim Wechsel mit Anzeigedauer, Versatz und Wiederkehr-Sperre. Standardmäßig aus."
        ]
      }
    },
    {
      "id": "anvil_formatting",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Anvil formatting and uses",
        "summary": "Valid & or section-sign color/style codes are accepted only under server formatting policy. Vanilla anvil costs and 50-character name limit remain. Shows prior work uses from repair cost when no result is present.",
        "details": [
          "Valid & or section-sign color/style codes are accepted only under server formatting policy. Vanilla anvil costs and 50-character name limit remain. Shows prior work uses from repair cost when no result is present."
        ]
      },
      "de": {
        "title": "Ambossformatierung und Nutzungen",
        "summary": "Gültige &- oder Paragraphen-Farb-/Stilcodes werden nur unter der Serverregel akzeptiert. Vanilla-Ambosskosten und die Grenze von 50 Zeichen bleiben. Zeigt vorherige Nutzungen aus den Reparaturkosten, wenn kein Ergebnis vorliegt.",
        "details": [
          "Gültige &- oder Paragraphen-Farb-/Stilcodes werden nur unter der Serverregel akzeptiert. Vanilla-Ambosskosten und die Grenze von 50 Zeichen bleiben. Zeigt vorherige Nutzungen aus den Reparaturkosten, wenn kein Ergebnis vorliegt."
        ]
      }
    },
    {
      "id": "renamed_models",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Renamed model compatibility",
        "summary": "Opt-in CIT JSON under assets/<namespace>/cit and PNG files in config/simplevisuals/textures. Exact custom-name match, item filter, tags, deterministic weight priority, bounded search pages and anvil rename instructions. Generated Vanilla item definitions bridge legacy model paths. At most 128 rules; PNGs at most 1 MiB and 256 pixels per axis. Simple Models, when installed, owns this feature and disables this compatibility handler.",
        "details": [
          "Opt-in CIT JSON under assets/<namespace>/cit and PNG files in config/simplevisuals/textures. Exact custom-name match, item filter, tags, deterministic weight priority, bounded search pages and anvil rename instructions. Generated Vanilla item definitions bridge legacy model paths. At most 128 rules; PNGs at most 1 MiB and 256 pixels per axis. Simple Models, when installed, owns this feature and disables this compatibility handler."
        ]
      },
      "de": {
        "title": "Kompatibilität umbenannter Modelle",
        "summary": "Optionale CIT-JSONs unter assets/<namespace>/cit und PNGs in config/simplevisuals/textures. Exakter benutzerdefinierter Name, Itemfilter, Tags, deterministische Gewichtung, begrenzte Suchseiten und Amboss-Anleitung. Erzeugte Vanilla-Itemdefinitionen verbinden alte Modellpfade. Höchstens 128 Regeln; PNGs höchstens 1 MiB und 256 Pixel pro Achse. Bei installiertem Simple Models übernimmt dieses die Funktion und deaktiviert diesen Kompatibilitätshandler.",
        "details": [
          "Optionale CIT-JSONs unter assets/<namespace>/cit und PNGs in config/simplevisuals/textures. Exakter benutzerdefinierter Name, Itemfilter, Tags, deterministische Gewichtung, begrenzte Suchseiten und Amboss-Anleitung. Erzeugte Vanilla-Itemdefinitionen verbinden alte Modellpfade. Höchstens 128 Regeln; PNGs höchstens 1 MiB und 256 Pixel pro Achse. Bei installiertem Simple Models übernimmt dieses die Funktion und deaktiviert diesen Kompatibilitätshandler."
        ]
      }
    },
    {
      "id": "commands",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals"
      ],
      "en": {
        "title": "Configuration commands",
        "summary": "Server: /simplevisuals config visuals enableAnvilFormatting [true|false], administrator only. Cosmetic options use the local client screen; server settings never overwrite another player's HUD.",
        "details": [
          "Server: /simplevisuals config visuals enableAnvilFormatting [true|false], administrator only. Cosmetic options use the local client screen; server settings never overwrite another player's HUD."
        ]
      },
      "de": {
        "title": "Konfigurationsbefehle",
        "summary": "Server: /simplevisuals config visuals enableAnvilFormatting [true|false], nur Administratoren. Optische Optionen verwenden den lokalen Clientbildschirm; Serverwerte überschreiben kein fremdes HUD.",
        "details": [
          "Server: /simplevisuals config visuals enableAnvilFormatting [true|false], nur Administratoren. Optische Optionen verwenden den lokalen Clientbildschirm; Serverwerte überschreiben kein fremdes HUD."
        ]
      }
    },
    {
      "id": "config_particles_globalLevel",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Global particle level",
        "summary": "Setting particles.globalLevel. Local cosmetic setting; changes do not affect gameplay. Default: SUBTLE.",
        "details": [
          "Setting particles.globalLevel. Local cosmetic setting; changes do not affect gameplay. Default: SUBTLE."
        ]
      },
      "de": {
        "title": "Globale Partikelstufe",
        "summary": "Einstellung particles.globalLevel. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: SUBTLE.",
        "details": [
          "Einstellung particles.globalLevel. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: SUBTLE."
        ]
      }
    },
    {
      "id": "config_visuals_enablePlayerLocator",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Player Locator",
        "summary": "Setting visuals.enablePlayerLocator. Player heads for Vanilla waypoints; server visibility and bar priority remain. Default: true.",
        "details": [
          "Setting visuals.enablePlayerLocator. Player heads for Vanilla waypoints; server visibility and bar priority remain. Default: true."
        ]
      },
      "de": {
        "title": "Spieler-Ortung",
        "summary": "Einstellung visuals.enablePlayerLocator. Spielerköpfe für Vanilla-Wegpunkte; Server-Sichtbarkeit und Leistenpriorität bleiben erhalten. Standard: true.",
        "details": [
          "Einstellung visuals.enablePlayerLocator. Spielerköpfe für Vanilla-Wegpunkte; Server-Sichtbarkeit und Leistenpriorität bleiben erhalten. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_enableChatHeads",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Chat Heads",
        "summary": "Setting visuals.enableChatHeads. Displays the player's head next to their chat messages. Default: true.",
        "details": [
          "Setting visuals.enableChatHeads. Displays the player's head next to their chat messages. Default: true."
        ]
      },
      "de": {
        "title": "Chat-Köpfe",
        "summary": "Einstellung visuals.enableChatHeads. Zeigt den Kopf des Spielers im Chat. Standard: true.",
        "details": [
          "Einstellung visuals.enableChatHeads. Zeigt den Kopf des Spielers im Chat. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_enableStatusEffectBars",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Status Effect Bars",
        "summary": "Setting visuals.enableStatusEffectBars. Shows a duration bar under status effect icons. Default: true.",
        "details": [
          "Setting visuals.enableStatusEffectBars. Shows a duration bar under status effect icons. Default: true."
        ]
      },
      "de": {
        "title": "Statuseffekt-Balken",
        "summary": "Einstellung visuals.enableStatusEffectBars. Zeigt Dauerbalken unter Effekt-Icons. Standard: true.",
        "details": [
          "Einstellung visuals.enableStatusEffectBars. Zeigt Dauerbalken unter Effekt-Icons. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_enableElytraPitchHelper",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Elytra Pitch Helper",
        "summary": "Setting visuals.enableElytraPitchHelper. Shows guide lines for perfect pitch while gliding. Default: true.",
        "details": [
          "Setting visuals.enableElytraPitchHelper. Shows guide lines for perfect pitch while gliding. Default: true."
        ]
      },
      "de": {
        "title": "Elytra-Neigungshilfe",
        "summary": "Einstellung visuals.enableElytraPitchHelper. Zeigt Hilfslinien für optimales Gleiten. Standard: true.",
        "details": [
          "Einstellung visuals.enableElytraPitchHelper. Zeigt Hilfslinien für optimales Gleiten. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_elytraTargetAngleUp",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Target Angle Up",
        "summary": "Setting visuals.elytraTargetAngleUp. Optimal upward pitch angle for Elytra flight. Default: -40.0.",
        "details": [
          "Setting visuals.elytraTargetAngleUp. Optimal upward pitch angle for Elytra flight. Default: -40.0."
        ]
      },
      "de": {
        "title": "Zielwinkel Oben",
        "summary": "Einstellung visuals.elytraTargetAngleUp. Optimaler Steigwinkel. Standard: -40.0.",
        "details": [
          "Einstellung visuals.elytraTargetAngleUp. Optimaler Steigwinkel. Standard: -40.0."
        ]
      }
    },
    {
      "id": "config_visuals_elytraTargetAngleDown",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Target Angle Down",
        "summary": "Setting visuals.elytraTargetAngleDown. Optimal downward pitch angle for Elytra flight. Default: 40.0.",
        "details": [
          "Setting visuals.elytraTargetAngleDown. Optimal downward pitch angle for Elytra flight. Default: 40.0."
        ]
      },
      "de": {
        "title": "Zielwinkel Unten",
        "summary": "Einstellung visuals.elytraTargetAngleDown. Optimaler Sinkwinkel. Standard: 40.0.",
        "details": [
          "Einstellung visuals.elytraTargetAngleDown. Optimaler Sinkwinkel. Standard: 40.0."
        ]
      }
    },
    {
      "id": "config_visuals_elytraPitchTolerance",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Pitch Tolerance",
        "summary": "Setting visuals.elytraPitchTolerance. Acceptable deviation from target angles for optimal Elytra flight. Default: 10.0.",
        "details": [
          "Setting visuals.elytraPitchTolerance. Acceptable deviation from target angles for optimal Elytra flight. Default: 10.0."
        ]
      },
      "de": {
        "title": "Toleranz",
        "summary": "Einstellung visuals.elytraPitchTolerance. Akzeptable Abweichung vom Zielwinkel. Standard: 10.0.",
        "details": [
          "Einstellung visuals.elytraPitchTolerance. Akzeptable Abweichung vom Zielwinkel. Standard: 10.0."
        ]
      }
    },
    {
      "id": "config_visuals_elytraSensitivity",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Guide Sensitivity",
        "summary": "Setting visuals.elytraSensitivity. Responsiveness of the pitch guide lines. Default: 4.0.",
        "details": [
          "Setting visuals.elytraSensitivity. Responsiveness of the pitch guide lines. Default: 4.0."
        ]
      },
      "de": {
        "title": "Empfindlichkeit",
        "summary": "Einstellung visuals.elytraSensitivity. Reaktionsstärke der Hilfslinien. Standard: 4.0.",
        "details": [
          "Einstellung visuals.elytraSensitivity. Reaktionsstärke der Hilfslinien. Standard: 4.0."
        ]
      }
    },
    {
      "id": "config_visuals_enableRenamedItemTextures",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Renamed item model compatibility",
        "summary": "Setting visuals.enableRenamedItemTextures. Compatibility; automatically disabled with Simple Models. Enables custom textures for renamed items based on the files in the config folder.\n§cRequires a game restart to take effect! Default: false.",
        "details": [
          "Setting visuals.enableRenamedItemTextures. Compatibility; automatically disabled with Simple Models. Enables custom textures for renamed items based on the files in the config folder.\n§cRequires a game restart to take effect! Default: false."
        ]
      },
      "de": {
        "title": "Modellkompatibilität umbenannter Items",
        "summary": "Einstellung visuals.enableRenamedItemTextures. Kompatibilität; bei Simple Models automatisch deaktiviert. Aktiviert eigene Texturen für umbenannte Items basierend auf dem Config-Ordner.\n§cErfordert einen Neustart des Spiels, um wirksam zu werden! Standard: false.",
        "details": [
          "Einstellung visuals.enableRenamedItemTextures. Kompatibilität; bei Simple Models automatisch deaktiviert. Aktiviert eigene Texturen für umbenannte Items basierend auf dem Config-Ordner.\n§cErfordert einen Neustart des Spiels, um wirksam zu werden! Standard: false."
        ]
      }
    },
    {
      "id": "config_visuals_enableAnvilFormatting",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Enable Anvil Formatting codes",
        "summary": "Setting visuals.enableAnvilFormatting. The server policy decides; Vanilla costs remain. Allows using '&' as a formatting code in anvils (e.g., &c for red, &k for magic). Default: true.",
        "details": [
          "Setting visuals.enableAnvilFormatting. The server policy decides; Vanilla costs remain. Allows using '&' as a formatting code in anvils (e.g., &c for red, &k for magic). Default: true."
        ]
      },
      "de": {
        "title": "Amboss-Formatierungscodes (&)",
        "summary": "Einstellung visuals.enableAnvilFormatting. Serverregel entscheidet; Vanilla-Kosten bleiben erhalten. Erlaubt die Nutzung von '&' für Farben im Amboss (z.B. &c für Rot, &k für Magie). Standard: true.",
        "details": [
          "Einstellung visuals.enableAnvilFormatting. Serverregel entscheidet; Vanilla-Kosten bleiben erhalten. Erlaubt die Nutzung von '&' für Farben im Amboss (z.B. &c für Rot, &k für Magie). Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_enhanceDeathMessages",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Enhance own death messages",
        "summary": "Setting visuals.enhanceDeathMessages. Appends coordinates [x, y, z] to chat death messages if the player is in render distance. Default: true.",
        "details": [
          "Setting visuals.enhanceDeathMessages. Appends coordinates [x, y, z] to chat death messages if the player is in render distance. Default: true."
        ]
      },
      "de": {
        "title": "Eigene Todesnachrichten ergänzen",
        "summary": "Einstellung visuals.enhanceDeathMessages. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.enhanceDeathMessages. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_deathCoordsMode",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Own death coordinates",
        "summary": "Setting visuals.deathCoordsMode. Only your own death coordinates, entirely local. Default: SEPARATE.",
        "details": [
          "Setting visuals.deathCoordsMode. Only your own death coordinates, entirely local. Default: SEPARATE."
        ]
      },
      "de": {
        "title": "Eigene Todeskoordinaten",
        "summary": "Einstellung visuals.deathCoordsMode. Nur eigene Todeskoordinaten, ausschließlich lokal. Standard: SEPARATE.",
        "details": [
          "Einstellung visuals.deathCoordsMode. Nur eigene Todeskoordinaten, ausschließlich lokal. Standard: SEPARATE."
        ]
      }
    },
    {
      "id": "config_visuals_enableMapTooltips",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Map Tooltips",
        "summary": "Setting visuals.enableMapTooltips. Shows coordinates and biome info when hovering over maps in the inventory. Default: true.",
        "details": [
          "Setting visuals.enableMapTooltips. Shows coordinates and biome info when hovering over maps in the inventory. Default: true."
        ]
      },
      "de": {
        "title": "Karten-Tooltips",
        "summary": "Einstellung visuals.enableMapTooltips. Zeigt Koordinaten beim Hovern über Karten. Standard: true.",
        "details": [
          "Einstellung visuals.enableMapTooltips. Zeigt Koordinaten beim Hovern über Karten. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_damageIndicators_enable",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Enable",
        "summary": "Setting visuals.damageIndicators.enable. Local cosmetic setting; changes do not affect gameplay. Default: true.",
        "details": [
          "Setting visuals.damageIndicators.enable. Local cosmetic setting; changes do not affect gameplay. Default: true."
        ]
      },
      "de": {
        "title": "Enable",
        "summary": "Einstellung visuals.damageIndicators.enable. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.damageIndicators.enable. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_damageIndicators_scale",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Scale",
        "summary": "Setting visuals.damageIndicators.scale. Local cosmetic setting; changes do not affect gameplay. Default: 1.0.",
        "details": [
          "Setting visuals.damageIndicators.scale. Local cosmetic setting; changes do not affect gameplay. Default: 1.0."
        ]
      },
      "de": {
        "title": "Scale",
        "summary": "Einstellung visuals.damageIndicators.scale. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 1.0.",
        "details": [
          "Einstellung visuals.damageIndicators.scale. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 1.0."
        ]
      }
    },
    {
      "id": "config_visuals_damageIndicators_colorNormal",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Color normal",
        "summary": "Setting visuals.damageIndicators.colorNormal. Local cosmetic setting; changes do not affect gameplay. Default: 16777215.",
        "details": [
          "Setting visuals.damageIndicators.colorNormal. Local cosmetic setting; changes do not affect gameplay. Default: 16777215."
        ]
      },
      "de": {
        "title": "Color normal",
        "summary": "Einstellung visuals.damageIndicators.colorNormal. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 16777215.",
        "details": [
          "Einstellung visuals.damageIndicators.colorNormal. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 16777215."
        ]
      }
    },
    {
      "id": "config_visuals_damageIndicators_colorSpecial",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Color special",
        "summary": "Setting visuals.damageIndicators.colorSpecial. Local cosmetic setting; changes do not affect gameplay. Default: 16766720.",
        "details": [
          "Setting visuals.damageIndicators.colorSpecial. Local cosmetic setting; changes do not affect gameplay. Default: 16766720."
        ]
      },
      "de": {
        "title": "Color special",
        "summary": "Einstellung visuals.damageIndicators.colorSpecial. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 16766720.",
        "details": [
          "Einstellung visuals.damageIndicators.colorSpecial. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 16766720."
        ]
      }
    },
    {
      "id": "config_visuals_damageIndicators_showBorder",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Show border",
        "summary": "Setting visuals.damageIndicators.showBorder. Local cosmetic setting; changes do not affect gameplay. Default: true.",
        "details": [
          "Setting visuals.damageIndicators.showBorder. Local cosmetic setting; changes do not affect gameplay. Default: true."
        ]
      },
      "de": {
        "title": "Show border",
        "summary": "Einstellung visuals.damageIndicators.showBorder. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.damageIndicators.showBorder. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_biomeInfo_enable",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Enable",
        "summary": "Setting visuals.biomeInfo.enable. Local cosmetic setting; changes do not affect gameplay. Default: false.",
        "details": [
          "Setting visuals.biomeInfo.enable. Local cosmetic setting; changes do not affect gameplay. Default: false."
        ]
      },
      "de": {
        "title": "Enable",
        "summary": "Einstellung visuals.biomeInfo.enable. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: false.",
        "details": [
          "Einstellung visuals.biomeInfo.enable. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: false."
        ]
      }
    },
    {
      "id": "config_visuals_biomeInfo_displayDuration",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Display duration",
        "summary": "Setting visuals.biomeInfo.displayDuration. Local cosmetic setting; changes do not affect gameplay. Default: 60.",
        "details": [
          "Setting visuals.biomeInfo.displayDuration. Local cosmetic setting; changes do not affect gameplay. Default: 60."
        ]
      },
      "de": {
        "title": "Display duration",
        "summary": "Einstellung visuals.biomeInfo.displayDuration. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 60.",
        "details": [
          "Einstellung visuals.biomeInfo.displayDuration. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 60."
        ]
      }
    },
    {
      "id": "config_visuals_biomeInfo_cooldownSeconds",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Cooldown seconds",
        "summary": "Setting visuals.biomeInfo.cooldownSeconds. Local cosmetic setting; changes do not affect gameplay. Default: 60.",
        "details": [
          "Setting visuals.biomeInfo.cooldownSeconds. Local cosmetic setting; changes do not affect gameplay. Default: 60."
        ]
      },
      "de": {
        "title": "Cooldown seconds",
        "summary": "Einstellung visuals.biomeInfo.cooldownSeconds. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 60.",
        "details": [
          "Einstellung visuals.biomeInfo.cooldownSeconds. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 60."
        ]
      }
    },
    {
      "id": "config_visuals_biomeInfo_yOffset",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Y offset",
        "summary": "Setting visuals.biomeInfo.yOffset. Local cosmetic setting; changes do not affect gameplay. Default: 50.",
        "details": [
          "Setting visuals.biomeInfo.yOffset. Local cosmetic setting; changes do not affect gameplay. Default: 50."
        ]
      },
      "de": {
        "title": "Y offset",
        "summary": "Einstellung visuals.biomeInfo.yOffset. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 50.",
        "details": [
          "Einstellung visuals.biomeInfo.yOffset. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 50."
        ]
      }
    },
    {
      "id": "config_visuals_speedLines_enableSpeedLines",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Enable Speed Lines",
        "summary": "Setting visuals.speedLines.enableSpeedLines. Displays speed lines when moving fast with elytra. Default: true.",
        "details": [
          "Setting visuals.speedLines.enableSpeedLines. Displays speed lines when moving fast with elytra. Default: true."
        ]
      },
      "de": {
        "title": "Enable speed lines",
        "summary": "Einstellung visuals.speedLines.enableSpeedLines. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.speedLines.enableSpeedLines. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_speedLines_speedLinesColor",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Speed Lines Color (Hex)",
        "summary": "Setting visuals.speedLines.speedLinesColor. Color of the speed lines in hexadecimal format (e.g., #FF0000 for red). Default: 16777215.",
        "details": [
          "Setting visuals.speedLines.speedLinesColor. Color of the speed lines in hexadecimal format (e.g., #FF0000 for red). Default: 16777215."
        ]
      },
      "de": {
        "title": "Speed lines color",
        "summary": "Einstellung visuals.speedLines.speedLinesColor. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 16777215.",
        "details": [
          "Einstellung visuals.speedLines.speedLinesColor. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 16777215."
        ]
      }
    },
    {
      "id": "config_visuals_speedLines_speedLinesAlpha",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Speed Lines Transparency",
        "summary": "Setting visuals.speedLines.speedLinesAlpha. Transparency of the speed lines (0 = fully transparent, 255 = fully opaque). Default: 0.7.",
        "details": [
          "Setting visuals.speedLines.speedLinesAlpha. Transparency of the speed lines (0 = fully transparent, 255 = fully opaque). Default: 0.7."
        ]
      },
      "de": {
        "title": "Speed lines alpha",
        "summary": "Einstellung visuals.speedLines.speedLinesAlpha. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 0.7.",
        "details": [
          "Einstellung visuals.speedLines.speedLinesAlpha. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 0.7."
        ]
      }
    },
    {
      "id": "config_visuals_speedLines_speedLinesAmount",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Speed Lines Amount",
        "summary": "Setting visuals.speedLines.speedLinesAmount. Number of speed lines to display. Default: 1.0.",
        "details": [
          "Setting visuals.speedLines.speedLinesAmount. Number of speed lines to display. Default: 1.0."
        ]
      },
      "de": {
        "title": "Speed lines amount",
        "summary": "Einstellung visuals.speedLines.speedLinesAmount. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 1.0.",
        "details": [
          "Einstellung visuals.speedLines.speedLinesAmount. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 1.0."
        ]
      }
    },
    {
      "id": "config_visuals_speedLines_speedLinesRadius",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Speed Lines Tunnel Radius",
        "summary": "Setting visuals.speedLines.speedLinesRadius. Radius of the speed lines tunnel effect. Default: 0.7.",
        "details": [
          "Setting visuals.speedLines.speedLinesRadius. Radius of the speed lines tunnel effect. Default: 0.7."
        ]
      },
      "de": {
        "title": "Speed lines radius",
        "summary": "Einstellung visuals.speedLines.speedLinesRadius. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 0.7.",
        "details": [
          "Einstellung visuals.speedLines.speedLinesRadius. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 0.7."
        ]
      }
    },
    {
      "id": "config_visuals_speedLines_speedLinesWidth",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Speed Lines Width",
        "summary": "Setting visuals.speedLines.speedLinesWidth. Width of each speed line. Default: 8.0.",
        "details": [
          "Setting visuals.speedLines.speedLinesWidth. Width of each speed line. Default: 8.0."
        ]
      },
      "de": {
        "title": "Speed lines width",
        "summary": "Einstellung visuals.speedLines.speedLinesWidth. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 8.0.",
        "details": [
          "Einstellung visuals.speedLines.speedLinesWidth. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 8.0."
        ]
      }
    },
    {
      "id": "config_visuals_speedLines_speedLinesSpeed",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Speed Lines Animation Speed",
        "summary": "Setting visuals.speedLines.speedLinesSpeed. Speed of the speed lines animation. Default: 1.0.",
        "details": [
          "Setting visuals.speedLines.speedLinesSpeed. Speed of the speed lines animation. Default: 1.0."
        ]
      },
      "de": {
        "title": "Speed lines speed",
        "summary": "Einstellung visuals.speedLines.speedLinesSpeed. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 1.0.",
        "details": [
          "Einstellung visuals.speedLines.speedLinesSpeed. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 1.0."
        ]
      }
    },
    {
      "id": "config_visuals_speedLines_speedLinesScale",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Speed Lines Length",
        "summary": "Setting visuals.speedLines.speedLinesScale. Length of the speed lines. Default: 4.0.",
        "details": [
          "Setting visuals.speedLines.speedLinesScale. Length of the speed lines. Default: 4.0."
        ]
      },
      "de": {
        "title": "Speed lines scale",
        "summary": "Einstellung visuals.speedLines.speedLinesScale. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 4.0.",
        "details": [
          "Einstellung visuals.speedLines.speedLinesScale. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 4.0."
        ]
      }
    },
    {
      "id": "config_visuals_speedLines_speedThreshold",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Speed Lines Speed Threshold",
        "summary": "Setting visuals.speedLines.speedThreshold. Minimum speed required to display speed lines. Default: 0.6.",
        "details": [
          "Setting visuals.speedLines.speedThreshold. Minimum speed required to display speed lines. Default: 0.6."
        ]
      },
      "de": {
        "title": "Speed threshold",
        "summary": "Einstellung visuals.speedLines.speedThreshold. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 0.6.",
        "details": [
          "Einstellung visuals.speedLines.speedThreshold. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 0.6."
        ]
      }
    },
    {
      "id": "config_visuals_heldItemTooltips_enable",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Enable Held Item Tooltips",
        "summary": "Setting visuals.heldItemTooltips.enable. Local cosmetic setting; changes do not affect gameplay. Default: true.",
        "details": [
          "Setting visuals.heldItemTooltips.enable. Local cosmetic setting; changes do not affect gameplay. Default: true."
        ]
      },
      "de": {
        "title": "Aktivieren",
        "summary": "Einstellung visuals.heldItemTooltips.enable. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.heldItemTooltips.enable. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_heldItemTooltips_showDurability",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Show Durability",
        "summary": "Setting visuals.heldItemTooltips.showDurability. Local cosmetic setting; changes do not affect gameplay. Default: true.",
        "details": [
          "Setting visuals.heldItemTooltips.showDurability. Local cosmetic setting; changes do not affect gameplay. Default: true."
        ]
      },
      "de": {
        "title": "Show Durability",
        "summary": "Einstellung visuals.heldItemTooltips.showDurability. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.heldItemTooltips.showDurability. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_heldItemTooltips_showEnchantments",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Show Enchantments",
        "summary": "Setting visuals.heldItemTooltips.showEnchantments. Local cosmetic setting; changes do not affect gameplay. Default: true.",
        "details": [
          "Setting visuals.heldItemTooltips.showEnchantments. Local cosmetic setting; changes do not affect gameplay. Default: true."
        ]
      },
      "de": {
        "title": "Show Enchantments",
        "summary": "Einstellung visuals.heldItemTooltips.showEnchantments. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.heldItemTooltips.showEnchantments. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_heldItemTooltips_maxEnchantments",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Max Enchantments to Show",
        "summary": "Setting visuals.heldItemTooltips.maxEnchantments. Local cosmetic setting; changes do not affect gameplay. Default: 3.",
        "details": [
          "Setting visuals.heldItemTooltips.maxEnchantments. Local cosmetic setting; changes do not affect gameplay. Default: 3."
        ]
      },
      "de": {
        "title": "Max Enchantments",
        "summary": "Einstellung visuals.heldItemTooltips.maxEnchantments. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 3.",
        "details": [
          "Einstellung visuals.heldItemTooltips.maxEnchantments. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 3."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_enablePickupNotifier",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Enable Pickup Notifier",
        "summary": "Setting visuals.pickupNotifier.enablePickupNotifier. Displays a notifier on screen when picking up items or XP. Default: true.",
        "details": [
          "Setting visuals.pickupNotifier.enablePickupNotifier. Displays a notifier on screen when picking up items or XP. Default: true."
        ]
      },
      "de": {
        "title": "Enable pickup notifier",
        "summary": "Einstellung visuals.pickupNotifier.enablePickupNotifier. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.pickupNotifier.enablePickupNotifier. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupNotifierOffsetX",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Offset X",
        "summary": "Setting visuals.pickupNotifier.pickupNotifierOffsetX. Horizontal offset of the pickup notifier on the screen. Default: 10.",
        "details": [
          "Setting visuals.pickupNotifier.pickupNotifierOffsetX. Horizontal offset of the pickup notifier on the screen. Default: 10."
        ]
      },
      "de": {
        "title": "Pickup notifier offset x",
        "summary": "Einstellung visuals.pickupNotifier.pickupNotifierOffsetX. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 10.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupNotifierOffsetX. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 10."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupNotifierOffsetY",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Offset Y",
        "summary": "Setting visuals.pickupNotifier.pickupNotifierOffsetY. Vertical offset of the pickup notifier on the screen. Default: 10.",
        "details": [
          "Setting visuals.pickupNotifier.pickupNotifierOffsetY. Vertical offset of the pickup notifier on the screen. Default: 10."
        ]
      },
      "de": {
        "title": "Pickup notifier offset y",
        "summary": "Einstellung visuals.pickupNotifier.pickupNotifierOffsetY. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 10.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupNotifierOffsetY. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 10."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupNotifierScale",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Scale",
        "summary": "Setting visuals.pickupNotifier.pickupNotifierScale. Scale of the pickup notifier display. Default: 1.0.",
        "details": [
          "Setting visuals.pickupNotifier.pickupNotifierScale. Scale of the pickup notifier display. Default: 1.0."
        ]
      },
      "de": {
        "title": "Pickup notifier scale",
        "summary": "Einstellung visuals.pickupNotifier.pickupNotifierScale. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 1.0.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupNotifierScale. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 1.0."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupNotifierDuration",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Display Duration",
        "summary": "Setting visuals.pickupNotifier.pickupNotifierDuration. Duration (in seconds) the pickup notifier remains visible. Default: 120.",
        "details": [
          "Setting visuals.pickupNotifier.pickupNotifierDuration. Duration (in seconds) the pickup notifier remains visible. Default: 120."
        ]
      },
      "de": {
        "title": "Pickup notifier duration",
        "summary": "Einstellung visuals.pickupNotifier.pickupNotifierDuration. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 120.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupNotifierDuration. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 120."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupNotifierShowXp",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Show XP",
        "summary": "Setting visuals.pickupNotifier.pickupNotifierShowXp. If enabled, the pickup notifier will also display XP pickups. Default: true.",
        "details": [
          "Setting visuals.pickupNotifier.pickupNotifierShowXp. If enabled, the pickup notifier will also display XP pickups. Default: true."
        ]
      },
      "de": {
        "title": "Pickup notifier show xp",
        "summary": "Einstellung visuals.pickupNotifier.pickupNotifierShowXp. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupNotifierShowXp. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupNotifierSide",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Screen Side",
        "summary": "Setting visuals.pickupNotifier.pickupNotifierSide. Side of the screen where the pickup notifier appears (Left or Right). Default: RIGHT.",
        "details": [
          "Setting visuals.pickupNotifier.pickupNotifierSide. Side of the screen where the pickup notifier appears (Left or Right). Default: RIGHT."
        ]
      },
      "de": {
        "title": "Pickup notifier side",
        "summary": "Einstellung visuals.pickupNotifier.pickupNotifierSide. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: RIGHT.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupNotifierSide. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: RIGHT."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupNotifierLayout",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Content Layout",
        "summary": "Setting visuals.pickupNotifier.pickupNotifierLayout. Layout of the pickup notifier content (Horizontal or Vertical). Default: COUNT_ICON_NAME.",
        "details": [
          "Setting visuals.pickupNotifier.pickupNotifierLayout. Layout of the pickup notifier content (Horizontal or Vertical). Default: COUNT_ICON_NAME."
        ]
      },
      "de": {
        "title": "Pickup notifier layout",
        "summary": "Einstellung visuals.pickupNotifier.pickupNotifierLayout. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: COUNT_ICON_NAME.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupNotifierLayout. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: COUNT_ICON_NAME."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupShowItem",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Show item icon",
        "summary": "Setting visuals.pickupNotifier.pickupShowItem. Displays the item icon in the pickup notifier. Default: true.",
        "details": [
          "Setting visuals.pickupNotifier.pickupShowItem. Displays the item icon in the pickup notifier. Default: true."
        ]
      },
      "de": {
        "title": "Itemsymbol zeigen",
        "summary": "Einstellung visuals.pickupNotifier.pickupShowItem. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupShowItem. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupShowName",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Show item name",
        "summary": "Setting visuals.pickupNotifier.pickupShowName. Displays the item name in the pickup notifier. Default: true.",
        "details": [
          "Setting visuals.pickupNotifier.pickupShowName. Displays the item name in the pickup notifier. Default: true."
        ]
      },
      "de": {
        "title": "Itemnamen zeigen",
        "summary": "Einstellung visuals.pickupNotifier.pickupShowName. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupShowName. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupShowCount",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Show item count",
        "summary": "Setting visuals.pickupNotifier.pickupShowCount. Displays the item count in the pickup notifier. Default: true.",
        "details": [
          "Setting visuals.pickupNotifier.pickupShowCount. Displays the item count in the pickup notifier. Default: true."
        ]
      },
      "de": {
        "title": "Itemanzahl zeigen",
        "summary": "Einstellung visuals.pickupNotifier.pickupShowCount. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupShowCount. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupUseRarityColor",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Use rarity color",
        "summary": "Setting visuals.pickupNotifier.pickupUseRarityColor. Colors the item name based on its rarity. Default: true.",
        "details": [
          "Setting visuals.pickupNotifier.pickupUseRarityColor. Colors the item name based on its rarity. Default: true."
        ]
      },
      "de": {
        "title": "Seltenheitsfarbe verwenden",
        "summary": "Einstellung visuals.pickupNotifier.pickupUseRarityColor. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupUseRarityColor. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupVanillaStyle",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Vanilla background style",
        "summary": "Setting visuals.pickupNotifier.pickupVanillaStyle. Uses the vanilla Minecraft style for the pickup notifier. Default: true.",
        "details": [
          "Setting visuals.pickupNotifier.pickupVanillaStyle. Uses the vanilla Minecraft style for the pickup notifier. Default: true."
        ]
      },
      "de": {
        "title": "Vanilla-Hintergrundstil",
        "summary": "Einstellung visuals.pickupNotifier.pickupVanillaStyle. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupVanillaStyle. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: true."
        ]
      }
    },
    {
      "id": "config_visuals_pickupNotifier_pickupBackgroundOpacity",
      "sources": [
        "modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java"
      ],
      "en": {
        "title": "Background Opacity",
        "summary": "Setting visuals.pickupNotifier.pickupBackgroundOpacity. Opacity of the pickup notifier background (0.0 - 1.0). Default: 1.0.",
        "details": [
          "Setting visuals.pickupNotifier.pickupBackgroundOpacity. Opacity of the pickup notifier background (0.0 - 1.0). Default: 1.0."
        ]
      },
      "de": {
        "title": "Pickup background opacity",
        "summary": "Einstellung visuals.pickupNotifier.pickupBackgroundOpacity. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 1.0.",
        "details": [
          "Einstellung visuals.pickupNotifier.pickupBackgroundOpacity. Lokale optische Einstellung; Änderungen betreffen kein Gameplay. Standard: 1.0."
        ]
      }
    },
    {
      "id": "effect_footstep_dust",
      "sources": [
        "modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json",
        "modules/simplevisuals/shared/java/com/simplevisuals/client/Immersion.java"
      ],
      "en": {
        "title": "Footstep dust",
        "summary": "Moving on the ground outside water. Vanilla type minecraft:poof; interval 4 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps.",
        "details": [
          "Moving on the ground outside water. Vanilla type minecraft:poof; interval 4 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps."
        ]
      },
      "de": {
        "title": "Schrittstaub",
        "summary": "Bewegung am Boden außerhalb des Wassers. Vanilla-Typ minecraft:poof; Intervall 4 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen.",
        "details": [
          "Bewegung am Boden außerhalb des Wassers. Vanilla-Typ minecraft:poof; Intervall 4 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen."
        ]
      }
    },
    {
      "id": "effect_cold_breath",
      "sources": [
        "modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json",
        "modules/simplevisuals/shared/java/com/simplevisuals/client/Immersion.java"
      ],
      "en": {
        "title": "Cold breath",
        "summary": "Breath outside water in biomes cold enough for snow. Vanilla type minecraft:white_smoke; interval 40 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps.",
        "details": [
          "Breath outside water in biomes cold enough for snow. Vanilla type minecraft:white_smoke; interval 40 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps."
        ]
      },
      "de": {
        "title": "Kalter Atem",
        "summary": "Atem außerhalb des Wassers in ausreichend kalten Schneebiomen. Vanilla-Typ minecraft:white_smoke; Intervall 40 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen.",
        "details": [
          "Atem außerhalb des Wassers in ausreichend kalten Schneebiomen. Vanilla-Typ minecraft:white_smoke; Intervall 40 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen."
        ]
      }
    },
    {
      "id": "effect_fireflies",
      "sources": [
        "modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json",
        "modules/simplevisuals/shared/java/com/simplevisuals/client/Immersion.java"
      ],
      "en": {
        "title": "Fireflies",
        "summary": "Nighttime above blocks in the Vanilla dirt tag. Vanilla type minecraft:firefly; interval 40 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps.",
        "details": [
          "Nighttime above blocks in the Vanilla dirt tag. Vanilla type minecraft:firefly; interval 40 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps."
        ]
      },
      "de": {
        "title": "Glühwürmchen",
        "summary": "Nachts über Blöcken im Vanilla-Erde-Tag. Vanilla-Typ minecraft:firefly; Intervall 40 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen.",
        "details": [
          "Nachts über Blöcken im Vanilla-Erde-Tag. Vanilla-Typ minecraft:firefly; Intervall 40 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen."
        ]
      }
    },
    {
      "id": "effect_pollen",
      "sources": [
        "modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json",
        "modules/simplevisuals/shared/java/com/simplevisuals/client/Immersion.java"
      ],
      "en": {
        "title": "Pollen",
        "summary": "Daytime beside a flower. Vanilla type minecraft:wax_on; interval 40 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps.",
        "details": [
          "Daytime beside a flower. Vanilla type minecraft:wax_on; interval 40 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps."
        ]
      },
      "de": {
        "title": "Pollen",
        "summary": "Tagsüber neben einer Blume. Vanilla-Typ minecraft:wax_on; Intervall 40 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen.",
        "details": [
          "Tagsüber neben einer Blume. Vanilla-Typ minecraft:wax_on; Intervall 40 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen."
        ]
      }
    },
    {
      "id": "effect_fire_sparks",
      "sources": [
        "modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json",
        "modules/simplevisuals/shared/java/com/simplevisuals/client/Immersion.java"
      ],
      "en": {
        "title": "Fire sparks",
        "summary": "Adjacent fire, lava or campfire. Vanilla type minecraft:small_flame; interval 20 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps.",
        "details": [
          "Adjacent fire, lava or campfire. Vanilla type minecraft:small_flame; interval 20 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps."
        ]
      },
      "de": {
        "title": "Feuerfunken",
        "summary": "Angrenzendes Feuer, Lava oder Lagerfeuer. Vanilla-Typ minecraft:small_flame; Intervall 20 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen.",
        "details": [
          "Angrenzendes Feuer, Lava oder Lagerfeuer. Vanilla-Typ minecraft:small_flame; Intervall 20 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen."
        ]
      }
    },
    {
      "id": "effect_water_ripples",
      "sources": [
        "modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json",
        "modules/simplevisuals/shared/java/com/simplevisuals/client/Immersion.java"
      ],
      "en": {
        "title": "Water ripples",
        "summary": "Movement at the water surface. Vanilla type minecraft:splash; interval 8 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps.",
        "details": [
          "Movement at the water surface. Vanilla type minecraft:splash; interval 8 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps."
        ]
      },
      "de": {
        "title": "Wasserwellen",
        "summary": "Bewegung an der Wasseroberfläche. Vanilla-Typ minecraft:splash; Intervall 8 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen.",
        "details": [
          "Bewegung an der Wasseroberfläche. Vanilla-Typ minecraft:splash; Intervall 8 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen."
        ]
      }
    },
    {
      "id": "effect_water_droplets",
      "sources": [
        "modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json",
        "modules/simplevisuals/shared/java/com/simplevisuals/client/Immersion.java"
      ],
      "en": {
        "title": "Water droplets",
        "summary": "Movement in water. Vanilla type minecraft:dripping_water; interval 12 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps.",
        "details": [
          "Movement in water. Vanilla type minecraft:dripping_water; interval 12 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps."
        ]
      },
      "de": {
        "title": "Wassertropfen",
        "summary": "Bewegung im Wasser. Vanilla-Typ minecraft:dripping_water; Intervall 12 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen.",
        "details": [
          "Bewegung im Wasser. Vanilla-Typ minecraft:dripping_water; Intervall 12 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen."
        ]
      }
    },
    {
      "id": "effect_leaf_fall",
      "sources": [
        "modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json",
        "modules/simplevisuals/shared/java/com/simplevisuals/client/Immersion.java"
      ],
      "en": {
        "title": "Falling leaves",
        "summary": "Leaves next to a sampled position two blocks above the player. Vanilla type minecraft:cherry_leaves; interval 30 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps.",
        "details": [
          "Leaves next to a sampled position two blocks above the player. Vanilla type minecraft:cherry_leaves; interval 30 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps."
        ]
      },
      "de": {
        "title": "Fallende Blätter",
        "summary": "Blätter neben einer geprüften Position zwei Blöcke über dem Spieler. Vanilla-Typ minecraft:cherry_leaves; Intervall 30 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen.",
        "details": [
          "Blätter neben einer geprüften Position zwei Blöcke über dem Spieler. Vanilla-Typ minecraft:cherry_leaves; Intervall 30 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen."
        ]
      }
    },
    {
      "id": "effect_enchanted_items",
      "sources": [
        "modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json",
        "modules/simplevisuals/shared/java/com/simplevisuals/client/Immersion.java"
      ],
      "en": {
        "title": "Enchanted item hints",
        "summary": "Enchanted main-hand or off-hand item. Vanilla type minecraft:enchant; interval 20 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps.",
        "details": [
          "Enchanted main-hand or off-hand item. Vanilla type minecraft:enchant; interval 20 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps."
        ]
      },
      "de": {
        "title": "Verzauberungsschimmer",
        "summary": "Verzaubertes Item in Haupt- oder Nebenhand. Vanilla-Typ minecraft:enchant; Intervall 20 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen.",
        "details": [
          "Verzaubertes Item in Haupt- oder Nebenhand. Vanilla-Typ minecraft:enchant; Intervall 20 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen."
        ]
      }
    },
    {
      "id": "effect_beacon_aura",
      "sources": [
        "modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json",
        "modules/simplevisuals/shared/java/com/simplevisuals/client/Immersion.java"
      ],
      "en": {
        "title": "Beacon aura",
        "summary": "Adjacent beacon. Vanilla type minecraft:end_rod; interval 20 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps.",
        "details": [
          "Adjacent beacon. Vanilla type minecraft:end_rod; interval 20 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps."
        ]
      },
      "de": {
        "title": "Leuchtfeueraura",
        "summary": "Angrenzendes Leuchtfeuer. Vanilla-Typ minecraft:end_rod; Intervall 20 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen.",
        "details": [
          "Angrenzendes Leuchtfeuer. Vanilla-Typ minecraft:end_rod; Intervall 20 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen."
        ]
      }
    },
    {
      "id": "effect_damage_feedback",
      "sources": [
        "modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json",
        "modules/simplevisuals/shared/java/com/simplevisuals/client/Immersion.java"
      ],
      "en": {
        "title": "Damage feedback",
        "summary": "Observed decrease in nearby entity health. Vanilla type minecraft:damage_indicator; interval 1 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps.",
        "details": [
          "Observed decrease in nearby entity health. Vanilla type minecraft:damage_indicator; interval 1 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps."
        ]
      },
      "de": {
        "title": "Schadensrückmeldung",
        "summary": "Beobachteter Lebensverlust eines nahen Lebewesens. Vanilla-Typ minecraft:damage_indicator; Intervall 1 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen.",
        "details": [
          "Beobachteter Lebensverlust eines nahen Lebewesens. Vanilla-Typ minecraft:damage_indicator; Intervall 1 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen."
        ]
      }
    },
    {
      "id": "effect_healing_feedback",
      "sources": [
        "modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json",
        "modules/simplevisuals/shared/java/com/simplevisuals/client/Immersion.java"
      ],
      "en": {
        "title": "Healing feedback",
        "summary": "Observed increase in nearby entity health. Vanilla type minecraft:heart; interval 1 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps.",
        "details": [
          "Observed increase in nearby entity health. Vanilla type minecraft:heart; interval 1 ticks. Levels: Off, Subtle, Normal, Strong, Maximum; override inherits global Subtle by default. Counts per opportunity 0/1/2/3/4, subject to shared caps."
        ]
      },
      "de": {
        "title": "Heilungsrückmeldung",
        "summary": "Beobachteter Lebensgewinn eines nahen Lebewesens. Vanilla-Typ minecraft:heart; Intervall 1 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen.",
        "details": [
          "Beobachteter Lebensgewinn eines nahen Lebewesens. Vanilla-Typ minecraft:heart; Intervall 1 Ticks. Stufen: Aus, Dezent, Normal, Stark, Maximum; standardmäßig globale Stufe Dezent. Anzahl je Gelegenheit 0/1/2/3/4 unter gemeinsamen Grenzen."
        ]
      }
    },
    {
      "id": "forge_263",
      "en": {
        "title": "Experimental Forge 26.3",
        "summary": "Opt-in Forge adapter with shared cosmetic mixins, local commands, server formatting policy, JSON configuration, and server tests.",
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
        "modules/simplevisuals/forge/build.gradle"
      ],
      "related": []
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
    "features": 77,
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
