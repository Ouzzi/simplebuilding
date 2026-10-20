window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};
window.WIKI_MODULE_DATA["simplemusic"] = {
  "schema": 1,
  "generatedFrom": {
    "line": "26.3",
    "generator": "wiki/generate.py"
  },
  "mod": {
    "id": "simplemusic",
    "name": "Simple Music",
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
      "id": "sheet_music",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/sheet/SheetMusicItem.java",
        "modules/simplemusic/shared/java/com/simplemusic/upload/MusicServer.java",
        "modules/simplemusic/shared/resources/data/simplemusic/recipe/sheet_music.json"
      ],
      "en": {
        "title": "Sheet Music",
        "summary": "Shapeless from paper, an ink sac and a feather. Use it to open the note code editor; signing with a title makes it read-only, like a written book.",
        "details": [
          "A draft may still contain problems; signing needs a title and a code without problems. The server checks every save again (code at most 32000 characters, title at most 32).",
          "From a stack only one sheet is written, the rest stays blank."
        ]
      },
      "de": {
        "title": "Notenblatt",
        "summary": "Formlos aus Papier, Tintenbeutel und Feder. Benutzen öffnet den Notencode-Editor; Signieren mit Titel macht es schreibgeschützt, wie ein beschriebenes Buch.",
        "details": [
          "Ein Entwurf darf noch Fehler enthalten; Signieren braucht einen Titel und einen fehlerfreien Code. Der Server prüft jedes Speichern erneut (Code höchstens 32000 Zeichen, Titel höchstens 32).",
          "Von einem Stapel wird nur ein Blatt beschrieben, der Rest bleibt leer."
        ]
      }
    },
    {
      "id": "note_code",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/sheet/NoteCode.java"
      ],
      "en": {
        "title": "Note code",
        "summary": "Text lines: \"tempo 4\" (ticks per step, 1-20), \"$h = harp\" or \"$k = sound minecraft:entity.cat.ambient\" (aliases), \"$h: c4 e4 g4 - . [c4 e4]\" (tracks).",
        "details": [
          "- holds, . rests, [..] is a chord of up to 4 tones, @vol 0.5 sets the volume of the following notes, x2 { c4 d4 } repeats inside a line.",
          "A line \"x3 {\" ... \"}\" repeats a section: all tracks are first padded to the same length, so they stay in step.",
          "Lines with the same label continue the same track; different labels play at the same time. A bare instrument name (harp:, bass:, creeper:) is a track too.",
          "Notes are written at the pitch they sound: harp f#3-f#5, bass and didgeridoo f#1-f#3, guitar f#2-f#4, flute and cow bell f#4-f#6, bell, chime and xylophone f#5-f#7; 0-24 is the raw note block note. Head instruments ignore the pitch.",
          "Limits: 16 tracks, 8 tones per step, 10 minutes, 32000 characters. Problems show with line and position."
        ]
      },
      "de": {
        "title": "Notencode",
        "summary": "Textzeilen: \"tempo 4\" (Ticks je Schritt, 1-20), \"$h = harp\" oder \"$k = sound minecraft:entity.cat.ambient\" (Aliase), \"$h: c4 e4 g4 - . [c4 e4]\" (Spuren).",
        "details": [
          "- hält, . ist Pause, [..] ein Akkord bis 4 Töne, @vol 0.5 setzt die Lautstärke der folgenden Noten, x2 { c4 d4 } wiederholt innerhalb einer Zeile.",
          "Eine Zeile \"x3 {\" ... \"}\" wiederholt einen Abschnitt: vorher werden alle Spuren auf gleiche Länge aufgefüllt, damit sie im Takt bleiben.",
          "Zeilen mit gleichem Namen setzen dieselbe Spur fort; verschiedene Namen klingen gleichzeitig. Ein Instrumentname (harp:, bass:, creeper:) ist auch eine Spur.",
          "Noten stehen in der klingenden Tonhöhe: Harfe f#3-f#5, Bass und Didgeridoo f#1-f#3, Gitarre f#2-f#4, Flöte und Kuhglocke f#4-f#6, Glocke, Glockenspiel und Xylophon f#5-f#7; 0-24 ist die rohe Notenblock-Note. Kopf-Instrumente ignorieren die Tonhöhe.",
          "Grenzen: 16 Spuren, 8 Töne je Schritt, 10 Minuten, 32000 Zeichen. Fehler erscheinen mit Zeile und Stelle."
        ]
      }
    },
    {
      "id": "sheet_editor",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/client/SheetMusicScreen.java",
        "modules/simplemusic/shared/java/com/simplemusic/client/SheetPreview.java",
        "modules/simplelib/shared/java/com/simplelib/api/client/ui/UiCodeArea.java"
      ],
      "en": {
        "title": "Editor",
        "summary": "Coloured code field with line numbers and underlined problems (the blueprint editor's field, now in SimpleLib). Listen plays the code on your client only.",
        "details": [
          "Buttons: Listen/Stop, Import, Copy (code to the clipboard), Sign, Done. Closing saves the draft."
        ]
      },
      "de": {
        "title": "Editor",
        "summary": "Farbiges Codefeld mit Zeilennummern und unterstrichenen Fehlern (das Feld des Blaupausen-Editors, jetzt in SimpleLib). Probehören spielt den Code nur auf deinem Client.",
        "details": [
          "Knöpfe: Probehören/Stopp, Importieren, Kopieren (Code in die Zwischenablage), Signieren, Fertig. Schließen speichert den Entwurf."
        ]
      }
    },
    {
      "id": "sheet_import",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/client/SheetFiles.java",
        "modules/simplemusic/shared/java/com/simplemusic/sheet/NbsReader.java"
      ],
      "en": {
        "title": "Import",
        "summary": "Put .txt note code or Note Block Studio .nbs files into config/simplemusic/sheets/ and pick one in the editor; the import only creates text.",
        "details": [
          "NBS: one track per instrument, simultaneous notes become chords; keys outside the note block range are folded by octaves; custom instruments, volume and panning are dropped, as is anything beyond the limits."
        ]
      },
      "de": {
        "title": "Import",
        "summary": ".txt-Notencode oder Note-Block-Studio-.nbs-Dateien in config/simplemusic/sheets/ legen und im Editor wählen; der Import erzeugt nur Text.",
        "details": [
          "NBS: eine Spur je Instrument, gleichzeitige Noten werden Akkorde; Tasten außerhalb des Notenblock-Umfangs werden um Oktaven verschoben; eigene Instrumente, Lautstärke und Panorama fallen weg, ebenso alles über den Grenzen."
        ]
      }
    },
    {
      "id": "orchestrion",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/orchestrion/OrchestrionBlock.java",
        "modules/simplemusic/shared/java/com/simplemusic/orchestrion/OrchestrionBlockEntity.java",
        "modules/simplemusic/shared/resources/data/simplemusic/recipe/orchestrion.json"
      ],
      "en": {
        "title": "Orchestrion",
        "summary": "Two copper ingots beside a note block over a jukebox. Right-click with a signed or valid sheet or a custom disc to put it in, with an empty hand to take it out. It plays on a loop and never ejects.",
        "details": [
          "The server plays each step's tones like a note block (Jukebox/Note Blocks volume, 48 blocks), at most 8 tones per step; one note particle per step.",
          "Comparator: 1-15 by progress through the piece, 0 when empty."
        ]
      },
      "de": {
        "title": "Orchestrion",
        "summary": "Zwei Kupferbarren neben einem Notenblock über einer Jukebox. Rechtsklick mit einem gültigen Notenblatt oder einer eigenen Platte legt sie ein, mit leerer Hand nimmt man sie heraus. Es spielt in Schleife und wirft nie aus.",
        "details": [
          "Der Server spielt die Töne jedes Schritts wie ein Notenblock (Lautstärke Jukebox/Notenblöcke, 48 Blöcke), höchstens 8 Töne je Schritt; ein Notenpartikel je Schritt.",
          "Komparator: 1-15 nach Fortschritt im Stück, 0 wenn leer."
        ]
      }
    },
    {
      "id": "orchestrion_redstone",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/orchestrion/OrchestrionBlockEntity.java"
      ],
      "en": {
        "title": "Redstone",
        "summary": "Signal on: plays and loops. Signal off: stops at the end of the current step. A rising edge starts from the top. Never wired: plays and loops.",
        "details": []
      },
      "de": {
        "title": "Redstone",
        "summary": "Signal an: spielt und loopt. Signal aus: hört am Ende des Schritts auf. Steigende Flanke: von vorn. Ohne Signal angeschlossen: spielt und loopt.",
        "details": []
      }
    },
    {
      "id": "orchestrion_playlist",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/orchestrion/OrchestrionBlockEntity.java",
        "modules/simplemusic/shared/resources/data/simplemusic/tags/block/playlist_containers.json"
      ],
      "en": {
        "title": "Playlist on top",
        "summary": "A chest, trapped chest, barrel or copper chest on top is the playlist: after each run the next sheet or custom disc swaps places with the finished one (slot order). Without another one the piece loops.",
        "details": [
          "Only single chests without unopened loot count. Tag simplemusic:playlist_containers."
        ]
      },
      "de": {
        "title": "Playlist darüber",
        "summary": "Eine Truhe, Redstone-Truhe, ein Fass oder eine Kupfertruhe darüber ist die Playlist: nach jedem Durchlauf tauscht das nächste Notenblatt oder die nächste eigene Platte den Platz mit dem fertigen (Slot-Reihenfolge). Ohne weiteres loopt das Stück.",
        "details": [
          "Nur einzelne Truhen ohne ungeöffnete Beute zählen. Tag simplemusic:playlist_containers."
        ]
      }
    },
    {
      "id": "blank_disc",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/disc/BlankDiscItem.java",
        "modules/simplemusic/shared/java/com/simplemusic/client/BurnScreen.java",
        "modules/simplemusic/shared/resources/data/simplemusic/recipe/blank_disc.json"
      ],
      "en": {
        "title": "Blank Disc",
        "summary": "Four coal or charcoal and four blackstone around an amethyst shard. Use it to open the burn window: it lists the .ogg files in config/simplemusic/upload/.",
        "details": [
          "Choose a file, give a title, Burn: the client checks the file first, then sends it in parts. Only upload your own or free music."
        ]
      },
      "de": {
        "title": "Rohling",
        "summary": "Vier Kohle oder Holzkohle und vier Schwarzstein um einen Amethystsplitter. Benutzen öffnet das Brennfenster mit den .ogg-Dateien aus config/simplemusic/upload/.",
        "details": [
          "Datei wählen, Titel geben, Brennen: der Client prüft die Datei zuerst und schickt sie dann in Stücken. Nur eigene oder freie Musik hochladen."
        ]
      }
    },
    {
      "id": "upload_check",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/upload/OggCheck.java",
        "modules/simplemusic/shared/java/com/simplemusic/upload/UploadSession.java"
      ],
      "en": {
        "title": "File check",
        "summary": "The server checks every upload itself without decoding: Ogg Vorbis only, one stream, intact pages (checksums), mono only, 22.05-48 kHz, at most 4 MiB and 5 minutes (server settings). Tags are removed.",
        "details": [
          "Stereo is refused: export the file as mono (OpenAL only places mono sound in the world).",
          "Parts of at most 32 KiB, in order, one upload per player, at most 256 KiB/s; the server computes the SHA-256 itself."
        ]
      },
      "de": {
        "title": "Dateiprüfung",
        "summary": "Der Server prüft jeden Upload selbst ohne Dekodieren: nur Ogg Vorbis, ein Strom, heile Seiten (Prüfsummen), nur Mono, 22,05-48 kHz, höchstens 4 MiB und 5 Minuten (Server-Einstellungen). Tags werden entfernt.",
        "details": [
          "Stereo wird abgelehnt: die Datei als Mono exportieren (OpenAL verortet nur Mono-Klang in der Welt).",
          "Stücke von höchstens 32 KiB, in Reihenfolge, ein Upload je Spieler, höchstens 256 KiB/s; den SHA-256 berechnet der Server selbst."
        ]
      }
    },
    {
      "id": "custom_disc",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/disc/CustomDiscItem.java",
        "modules/simplemusic/shared/java/com/simplemusic/disc/DiscPlayback.java",
        "modules/simplemusic/shared/java/com/simplemusic/client/TrackPlayer.java",
        "modules/simplemusic/shared/java/com/simplemusic/client/TrackCache.java"
      ],
      "en": {
        "title": "Custom Disc",
        "summary": "A burned disc carries the track's hash, title, length and uploader, never audio. It plays in the Orchestrion (loop, playlist) and in the Vanilla jukebox (once).",
        "details": [
          "Players within 64 blocks get the file from the server in parts the first time (cache in simplemusic-cache/ of the game folder, 512 MiB) and join at the current position.",
          "A deleted track leaves a silent disc."
        ]
      },
      "de": {
        "title": "Eigene Platte",
        "summary": "Eine gebrannte Platte trägt Hash, Titel, Länge und Hochladenden des Titels, nie Audiodaten. Sie spielt im Orchestrion (Schleife, Playlist) und in der Vanilla-Jukebox (einmal).",
        "details": [
          "Spieler in 64 Blöcken bekommen die Datei beim ersten Mal in Stücken vom Server (Cache in simplemusic-cache/ im Spielordner, 512 MiB) und steigen an der aktuellen Stelle ein.",
          "Ein gelöschter Titel hinterlässt eine stumme Platte."
        ]
      }
    },
    {
      "id": "moderation",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/upload/MusicCommands.java",
        "modules/simplemusic/shared/java/com/simplemusic/upload/TrackStore.java"
      ],
      "en": {
        "title": "Moderation",
        "summary": "Everyone may burn up to 5 discs; others hear a disc only after an operator approves it, until then only the uploader. In single player and for the LAN host it is approved at once.",
        "details": [
          "Operators: /music list [pending], /music approve|reject|delete <hash>, /music info <hash> (hash or its first 6+ characters), /music ban|unban <player>.",
          "Tracks live in <world>/simplemusic/tracks/ with an index.json; the same file is stored once."
        ]
      },
      "de": {
        "title": "Moderation",
        "summary": "Jeder darf bis zu 5 Platten brennen; andere hören eine Platte erst nach Freigabe durch einen Operator, bis dahin nur der Hochladende. Im Einzelspieler und für den LAN-Host ist sie sofort frei.",
        "details": [
          "Operatoren: /music list [pending], /music approve|reject|delete <hash>, /music info <hash> (Hash oder seine ersten 6+ Zeichen), /music ban|unban <spieler>.",
          "Titel liegen in <welt>/simplemusic/tracks/ mit einer index.json; dieselbe Datei wird einmal gespeichert."
        ]
      }
    },
    {
      "id": "config_server",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/config/MusicConfig.java"
      ],
      "en": {
        "title": "Server settings",
        "summary": "config/simplemusic-server.json, read at start; every number is clamped.",
        "details": [
          "enabled (true); sheetSounds (all | heads | instruments) with blockedSounds (music., music_disc., ui., ambient.cave, world-wide sounds: prefix list).",
          "maxTonesPerTick (8, 1 to 8), maxPlayingOrchestrions (32 per dimension, 1 to 64), orchestrionRange (48, 16 to 64).",
          "uploads (approve | everyone | op | off), maxUploadMiB (4, 1 to 16), maxUploadSeconds (300, 10 to 900), quotaPerPlayer (5, 0 to 50), storeLimitMiB (256, 16 to 2048), uploadKiBPerSecond (256, 16 to 1024), downloadKiBPerSecond (512, 32 to 4096).",
          "jukeboxCustomDiscs (true): custom discs in the Vanilla jukebox; it switches itself off where Vanilla has its own jukebox playlist (block tag #minecraft:jukebox_containers)."
        ]
      },
      "de": {
        "title": "Server-Einstellungen",
        "summary": "config/simplemusic-server.json, beim Start gelesen; jede Zahl wird begrenzt.",
        "details": [
          "enabled (true); sheetSounds (all | heads | instruments) mit blockedSounds (music., music_disc., ui., ambient.cave, weltweite Klänge: Präfixliste).",
          "maxTonesPerTick (8, 1 bis 8), maxPlayingOrchestrions (32 je Dimension, 1 bis 64), orchestrionRange (48, 16 bis 64).",
          "uploads (approve | everyone | op | off), maxUploadMiB (4, 1 bis 16), maxUploadSeconds (300, 10 bis 900), quotaPerPlayer (5, 0 bis 50), storeLimitMiB (256, 16 bis 2048), uploadKiBPerSecond (256, 16 bis 1024), downloadKiBPerSecond (512, 32 bis 4096).",
          "jukeboxCustomDiscs (true): eigene Platten in der Vanilla-Jukebox; schaltet sich selbst ab, wo Vanilla eine eigene Jukebox-Playlist hat (Block-Tag #minecraft:jukebox_containers)."
        ]
      }
    },
    {
      "id": "creative_tab_switch",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
      ],
      "en": {
        "title": "Creative tabs",
        "summary": "The module's items sit in its own creative tab and next to the jukebox in Functional Blocks.",
        "details": [
          "The shared switch simplemusic.addItemsToVanillaTabs in config/simple-creative-tabs.properties (default true) keeps the Vanilla tabs unchanged; the guide book stays in Tools & Utilities."
        ]
      },
      "de": {
        "title": "Kreativ-Tabs",
        "summary": "Die Items des Moduls stehen im eigenen Kreativ-Tab und neben der Jukebox in Funktionsblöcke.",
        "details": [
          "Der gemeinsame Schalter simplemusic.addItemsToVanillaTabs in config/simple-creative-tabs.properties (Standard true) lässt die Vanilla-Tabs unverändert; das Handbuch bleibt in Werkzeuge & Hilfsmittel."
        ]
      }
    }
  ],
  "recipes": [
    {
      "id": "simplemusic:blank_disc",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplemusic:blank_disc",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/blank_disc.json",
      "ingredients": [
        "#minecraft:coals",
        "minecraft:amethyst_shard",
        "minecraft:blackstone"
      ],
      "pattern": [
        "CBC",
        "BAB",
        "CBC"
      ],
      "key": {
        "C": [
          "#minecraft:coals"
        ],
        "B": [
          "minecraft:blackstone"
        ],
        "A": [
          "minecraft:amethyst_shard"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:blackstone",
            "count": 4
          },
          {
            "id": "minecraft:coal",
            "count": 4
          },
          {
            "id": "minecraft:amethyst_shard",
            "count": 1
          }
        ],
        "tagExamples": [
          "#minecraft:coals"
        ]
      }
    },
    {
      "id": "simplemusic:guide_book",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplemusic:guide_book",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/guide_book.json",
      "ingredients": [
        "minecraft:book",
        "minecraft:note_block"
      ],
      "ingredientGroups": [
        [
          "minecraft:book"
        ],
        [
          "minecraft:note_block"
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
            "id": "minecraft:oak_log",
            "count": 2
          },
          {
            "id": "minecraft:leather",
            "count": 1
          },
          {
            "id": "minecraft:redstone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simplemusic:orchestrion",
      "type": "minecraft:crafting_shaped",
      "category": "redstone",
      "group": null,
      "result": {
        "id": "simplemusic:orchestrion",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/orchestrion.json",
      "ingredients": [
        "minecraft:copper_ingot",
        "minecraft:jukebox",
        "minecraft:note_block"
      ],
      "pattern": [
        "CNC",
        " J "
      ],
      "key": {
        "C": [
          "minecraft:copper_ingot"
        ],
        "N": [
          "minecraft:note_block"
        ],
        "J": [
          "minecraft:jukebox"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:oak_log",
            "count": 4
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 2
          },
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:redstone",
            "count": 1
          }
        ]
      }
    },
    {
      "id": "simplemusic:sheet_music",
      "type": "minecraft:crafting_shapeless",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplemusic:sheet_music",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/sheet_music.json",
      "ingredients": [
        "minecraft:feather",
        "minecraft:ink_sac",
        "minecraft:paper"
      ],
      "ingredientGroups": [
        [
          "minecraft:paper"
        ],
        [
          "minecraft:ink_sac"
        ],
        [
          "minecraft:feather"
        ]
      ],
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:feather",
            "count": 1
          },
          {
            "id": "minecraft:ink_sac",
            "count": 1
          },
          {
            "id": "minecraft:sugar_cane",
            "count": 1
          }
        ]
      }
    }
  ],
  "lootTables": [
    {
      "id": "simplemusic:blocks/orchestrion",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:orchestrion"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/orchestrion.json"
    }
  ],
  "tags": [
    {
      "id": "simplemusic:block/playlist_containers",
      "replace": false,
      "values": [
        {
          "id": "minecraft:chest",
          "required": true
        },
        {
          "id": "minecraft:trapped_chest",
          "required": true
        },
        {
          "id": "minecraft:barrel",
          "required": true
        },
        {
          "id": "minecraft:copper_chest",
          "required": false
        },
        {
          "id": "minecraft:exposed_copper_chest",
          "required": false
        },
        {
          "id": "minecraft:weathered_copper_chest",
          "required": false
        },
        {
          "id": "minecraft:oxidized_copper_chest",
          "required": false
        },
        {
          "id": "minecraft:waxed_copper_chest",
          "required": false
        },
        {
          "id": "minecraft:waxed_exposed_copper_chest",
          "required": false
        },
        {
          "id": "minecraft:waxed_weathered_copper_chest",
          "required": false
        },
        {
          "id": "minecraft:waxed_oxidized_copper_chest",
          "required": false
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/tags/block/playlist_containers.json"
    }
  ],
  "advancements": [],
  "enchantments": [],
  "items": [
    {
      "id": "simplemusic:blank_disc",
      "name": {
        "en_us": "Blank Disc",
        "de_de": "Rohling"
      },
      "note": {
        "en": {
          "title": "Blank Disc",
          "summary": "Four coal or charcoal and four blackstone around an amethyst shard. Use it to open the burn window: it lists the .ogg files in config/simplemusic/upload/.",
          "details": [
            "Choose a file, give a title, Burn: the client checks the file first, then sends it in parts. Only upload your own or free music."
          ]
        },
        "de": {
          "title": "Rohling",
          "summary": "Vier Kohle oder Holzkohle und vier Schwarzstein um einen Amethystsplitter. Benutzen öffnet das Brennfenster mit den .ogg-Dateien aus config/simplemusic/upload/.",
          "details": [
            "Datei wählen, Titel geben, Brennen: der Client prüft die Datei zuerst und schickt sie dann in Stücken. Nur eigene oder freie Musik hochladen."
          ]
        }
      },
      "texture": "assets/textures/simplemusic/item/blank_disc.png",
      "craftedBy": [
        "simplemusic:blank_disc"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:custom_disc",
      "name": {
        "en_us": "Custom Disc",
        "de_de": "Eigene Platte"
      },
      "note": {
        "en": {
          "title": "Custom Disc",
          "summary": "A burned disc carries the track's hash, title, length and uploader, never audio. It plays in the Orchestrion (loop, playlist) and in the Vanilla jukebox (once).",
          "details": [
            "Players within 64 blocks get the file from the server in parts the first time (cache in simplemusic-cache/ of the game folder, 512 MiB) and join at the current position.",
            "A deleted track leaves a silent disc."
          ]
        },
        "de": {
          "title": "Eigene Platte",
          "summary": "Eine gebrannte Platte trägt Hash, Titel, Länge und Hochladenden des Titels, nie Audiodaten. Sie spielt im Orchestrion (Schleife, Playlist) und in der Vanilla-Jukebox (einmal).",
          "details": [
            "Spieler in 64 Blöcken bekommen die Datei beim ersten Mal in Stücken vom Server (Cache in simplemusic-cache/ im Spielordner, 512 MiB) und steigen an der aktuellen Stelle ein.",
            "Ein gelöschter Titel hinterlässt eine stumme Platte."
          ]
        }
      },
      "texture": "assets/textures/simplemusic/item/custom_disc.png",
      "craftedBy": [],
      "usedIn": []
    },
    {
      "id": "simplemusic:guide_book",
      "name": {
        "en_us": "Simple Music Guide",
        "de_de": "Simple-Music-Handbuch"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/guide/MusicGuide.java",
          "modules/simplemusic/shared/resources/data/simplemusic/recipe/guide_book.json"
        ],
        "en": {
          "summary": "Guide to this mod: 15 pages taken from this wiki, shown in your language. Shapeless recipe: book + note block. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Music\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 15 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Notenblock. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Music\" eins."
        }
      },
      "texture": "assets/textures/simplemusic/item/guide_book.png",
      "craftedBy": [
        "simplemusic:guide_book"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:sheet_music",
      "name": {
        "en_us": "Sheet Music",
        "de_de": "Notenblatt"
      },
      "note": {
        "en": {
          "title": "Sheet Music",
          "summary": "Shapeless from paper, an ink sac and a feather. Use it to open the note code editor; signing with a title makes it read-only, like a written book.",
          "details": [
            "A draft may still contain problems; signing needs a title and a code without problems. The server checks every save again (code at most 32000 characters, title at most 32).",
            "From a stack only one sheet is written, the rest stays blank."
          ]
        },
        "de": {
          "title": "Notenblatt",
          "summary": "Formlos aus Papier, Tintenbeutel und Feder. Benutzen öffnet den Notencode-Editor; Signieren mit Titel macht es schreibgeschützt, wie ein beschriebenes Buch.",
          "details": [
            "Ein Entwurf darf noch Fehler enthalten; Signieren braucht einen Titel und einen fehlerfreien Code. Der Server prüft jedes Speichern erneut (Code höchstens 32000 Zeichen, Titel höchstens 32).",
            "Von einem Stapel wird nur ein Blatt beschrieben, der Rest bleibt leer."
          ]
        }
      },
      "texture": "assets/textures/simplemusic/item/sheet_music.png",
      "craftedBy": [
        "simplemusic:sheet_music"
      ],
      "usedIn": []
    }
  ],
  "blocks": [
    {
      "id": "simplemusic:orchestrion",
      "name": {
        "en_us": "Orchestrion",
        "de_de": "Orchestrion"
      },
      "note": {
        "en": {
          "title": "Orchestrion",
          "summary": "Two copper ingots beside a note block over a jukebox. Right-click with a signed or valid sheet or a custom disc to put it in, with an empty hand to take it out. It plays on a loop and never ejects.",
          "details": [
            "The server plays each step's tones like a note block (Jukebox/Note Blocks volume, 48 blocks), at most 8 tones per step; one note particle per step.",
            "Comparator: 1-15 by progress through the piece, 0 when empty."
          ]
        },
        "de": {
          "title": "Orchestrion",
          "summary": "Zwei Kupferbarren neben einem Notenblock über einer Jukebox. Rechtsklick mit einem gültigen Notenblatt oder einer eigenen Platte legt sie ein, mit leerer Hand nimmt man sie heraus. Es spielt in Schleife und wirft nie aus.",
          "details": [
            "Der Server spielt die Töne jedes Schritts wie ein Notenblock (Lautstärke Jukebox/Notenblöcke, 48 Blöcke), höchstens 8 Töne je Schritt; ein Notenpartikel je Schritt.",
            "Komparator: 1-15 nach Fortschritt im Stück, 0 wenn leer."
          ]
        }
      },
      "craftedBy": [
        "simplemusic:orchestrion"
      ],
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
    "features": 13,
    "recipes": 4,
    "lootTables": 1,
    "tags": 1,
    "advancements": 0,
    "enchantments": 0,
    "items": 4,
    "blocks": 1,
    "trades": 0,
    "config": 0,
    "quests": 0,
    "recipesOtherLines": 0,
    "undocumented": 0,
    "inWorld": 0,
    "incompleteProse": 0
  }
};
