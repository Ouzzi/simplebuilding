window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplemodels"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplemodels",
    "name": "Simple Models",
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
      "id": "anvil",
      "title": {
        "en": "Anvil Assignment",
        "de": "Ambosszuweisung"
      },
      "en": {
        "summary": "Put an approved base item in the left anvil slot, leave the second slot empty, choose Models, then Assign and take the result. Only item_model changes; names, enchantments, counts, durability, and other components remain intact. Removal uses the same anvil costs and permissions."
      },
      "de": {
        "summary": "Freigegebenes Basisitem links in den Amboss legen, zweiten Platz leer lassen, Modelle und Zuweisen wählen, dann Ergebnis nehmen. Nur item_model ändert sich; Namen, Verzauberungen, Anzahl, Haltbarkeit und andere Komponenten bleiben erhalten. Entfernen nutzt dieselben Kosten und Rechte."
      }
    },
    {
      "id": "browser",
      "title": {
        "en": "Model Browser",
        "de": "Modellbrowser"
      },
      "en": {
        "summary": "The inventory and anvil Models buttons open the browser. Search name, item id, model id, author, or tags. Paged rows render actual item models and show the base/result pair. Inventory browsing never modifies items."
      },
      "de": {
        "summary": "Die Modelle-Knöpfe im Inventar und Amboss öffnen den Browser. Suche nach Name, Item-ID, Modell-ID, Autor oder Tags. Seitenweise Zeilen rendern echte Itemmodelle und zeigen Basis/Ergebnis. Inventarsuche ändert keine Items."
      }
    },
    {
      "id": "import",
      "title": {
        "en": "Folder Catalog and Resource Packs",
        "de": "Ordnerkatalog und Ressourcenpakete"
      },
      "en": {
        "summary": "Add or remove JSON definitions in config/simplemodels/catalog on the server, then run /simplemodels reload. Put rendering assets in an enabled folder resource pack (assets/<namespace>/items, models, textures). Reload Assets refreshes previews. Multiplayer servers distribute assets with the standard server resource pack URL and SHA-1 settings; never share private files. Assignment requires an anvil and an empty second slot. Model changes do not change item names or stats. Server Settings is read-only; admins edit config/simplemodels/config.json. See docs/modules/simplemodels.md for the JSON format."
      },
      "de": {
        "summary": "JSON-Definitionen auf dem Server unter config/simplemodels/catalogue hinzufügen oder entfernen, dann /simplemodels reload ausführen. Grafikdateien in ein aktiviertes Ordner-Ressourcenpaket legen (assets/<Namensraum>/items, models, textures). Ressourcen laden aktualisiert die Vorschau. Mehrspielerserver verteilen Dateien über die normalen Ressourcenpaket-URL- und SHA-1-Einstellungen; keine privaten Dateien teilen. Zuweisung braucht einen Amboss mit leerem zweitem Platz. Modellwechsel ändern weder Namen noch Werte. Servereinstellungen sind schreibgeschützt; Admins bearbeiten config/simplemodels/config.json. JSON-Format: docs/modules/simplemodels.md."
      }
    },
    {
      "id": "security",
      "title": {
        "en": "Multiplayer Safety",
        "de": "Mehrspielersicherheit"
      },
      "en": {
        "summary": "The server owns the catalog and policy. It syncs definitions to clients; clients cannot upload models or select an arbitrary item_model. Default operator-only assignment. Disabling assignment refuses new changes; existing saved cosmetic components remain. No hidden item identity, automatic downloads, arbitrary file paths, or gameplay stats."
      },
      "de": {
        "summary": "Der Server verwaltet Katalog und Regeln und synchronisiert Definitionen. Clients können keine Modelle hochladen oder beliebige item_model-Werte setzen. Standard: nur Operatoren. Abschalten verweigert neue Änderungen; gespeicherte kosmetische Komponenten bleiben. Keine versteckte Itemidentität, automatischen Downloads, beliebigen Dateipfade oder Spielwerte."
      }
    },
    {
      "id": "compatibility",
      "title": {
        "en": "Legacy renamed Compatibility",
        "de": "Kompatibilität zu renamed"
      },
      "en": {
        "summary": "No item/block/entity/enchantment registries existed in renamed. Definition fields id, base_item, match_name, model, tags, author remain readable; bare ids use the renamed namespace. Optional exact legacy anvil name matching preserves custom names. The old unsafe bracket-hiding/render mixins are replaced by item_model. No new items, blocks, mobs, recipes, loot, advancements, or keybinds are added. Block-item appearances are supported; placed block geometry is not changed."
      },
      "de": {
        "summary": "renamed hatte keine Item-/Block-/Entity-/Verzauberungsregistrierungen. Die Felder id, base_item, match_name, model, tags, author bleiben lesbar; einfache IDs nutzen renamed. Optional exakte alte Namenszuweisung am Amboss; eigene Namen bleiben erhalten. Unsichere Klammer-/Render-Mixins sind durch item_model ersetzt. Keine neuen Items, Blöcke, Mobs, Rezepte, Beute, Erfolge oder Tasten. Blockitems sind unterstützt; gesetzte Blockgeometrie bleibt unverändert."
      }
    },
    {
      "id": "command_reload",
      "title": {
        "en": "/simplemodels reload",
        "de": "/simplemodels reload"
      },
      "en": {
        "summary": "Game master permission is required. Rescan the server folder and policy, then synchronize the new catalog to connected players. Add/remove files without restarting. Malformed policy disables assignment; bad definitions are skipped with server logs."
      },
      "de": {
        "summary": "Spielleiterrechte erforderlich. Serverordner und Regeln neu lesen, dann Katalog an verbundene Spieler synchronisieren. Dateien ohne Neustart hinzufügen/entfernen. Fehlerhafte Regeln sperren die Zuweisung; fehlerhafte Definitionen werden mit Serverlog übersprungen."
      }
    },
    {
      "id": "config_enabled",
      "title": {
        "en": "Enable Assignment",
        "de": "Zuweisung erlauben"
      },
      "en": {
        "summary": "Allow cosmetic model changes at anvils. Default: true."
      },
      "de": {
        "summary": "Kosmetische Modellwechsel am Amboss erlauben. Standard: true."
      }
    },
    {
      "id": "config_operatorsOnly",
      "title": {
        "en": "Operators Only",
        "de": "Nur Operatoren"
      },
      "en": {
        "summary": "Only operators with game master permission may assign or remove models. Default: true."
      },
      "de": {
        "summary": "Nur Operatoren mit Spielleiterrechten dürfen Modelle zuweisen oder entfernen. Standard: true."
      }
    },
    {
      "id": "config_allowModItems",
      "title": {
        "en": "Allow Mod Items",
        "de": "Mod-Items erlauben"
      },
      "en": {
        "summary": "Allow explicitly approved mod items; gameplay components remain unchanged. Default: false."
      },
      "de": {
        "summary": "Ausdrücklich freigegebene Mod-Items erlauben; Spielkomponenten bleiben unverändert. Standard: false."
      }
    },
    {
      "id": "config_legacyNameMatching",
      "title": {
        "en": "Legacy Names",
        "de": "Alte Namenszuweisung"
      },
      "en": {
        "summary": "Also accept exact match_name text at anvils. No suffix matching or hidden identity. Default: false."
      },
      "de": {
        "summary": "Auch exakten match_name-Text am Amboss akzeptieren. Keine Suffixsuche oder versteckte Identität. Standard: false."
      }
    },
    {
      "id": "config_levelCost",
      "title": {
        "en": "Level Cost",
        "de": "Stufenkosten"
      },
      "en": {
        "summary": "Levels charged by the vanilla anvil per model change, capped at 1–10. Default: 1."
      },
      "de": {
        "summary": "Vanilla-Ambosskosten je Modellwechsel, begrenzt auf 1–10 Stufen. Standard: 1."
      }
    },
    {
      "id": "config_maxModels",
      "title": {
        "en": "Catalog Limit",
        "de": "Kataloggrenze"
      },
      "en": {
        "summary": "Approved definitions, capped at 1–64 and 24,000 serialized characters. Default: 64."
      },
      "de": {
        "summary": "Freigegebene Definitionen, begrenzt auf 1–64 und 24.000 serialisierte Zeichen. Standard: 64."
      }
    },
    {
      "id": "config_maxFileBytes",
      "title": {
        "en": "Definition Size",
        "de": "Definitionsgröße"
      },
      "en": {
        "summary": "JSON definition bytes, capped at 256–16384. Invalid, nested, or oversized files are rejected. Default: 16384."
      },
      "de": {
        "summary": "JSON-Definitionsbytes, begrenzt auf 256–16384. Ungültige, zu tief verschachtelte oder übergroße Dateien werden verworfen. Standard: 16384."
      }
    },
    {
      "id": "forge_263",
      "en": {
        "title": "Experimental Forge 26.3",
        "summary": "Opt-in Forge adapter with server-approved anvil models, catalogue networking, JSON configuration, and server tests.",
        "details": [
          "Enable -Pforge263=true. Forge client display, catalogue networking with real players, and resource-pack delivery require separate acceptance."
        ]
      },
      "de": {
        "title": "Experimentelles Forge 26.3",
        "summary": "Opt-in-Forge-Adapter mit gemeinsamem Modulcode, JSON-Konfiguration und gleichem Servertestkatalog.",
        "details": [
          "Mit -Pforge263=true aktivieren. Forge-Clientdarstellung, Katalognetzwerk mit echten Spielern und Ressourcenpaketverteilung brauchen eine eigene Abnahme."
        ]
      },
      "sources": [
        "modules/simplemodels/forge/build.gradle"
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
    "features": 14,
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
