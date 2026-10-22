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
        "summary": "Shapeless from paper, an ink sac and a feather. Use it to open the note code editor; signing with a title makes it read-only, like a written book. A sheet holds either a composition or an uploaded track.",
        "details": [
          "A draft may still contain problems; signing needs a title and a code without problems. The server checks every save again (code at most 32000 characters, title at most 32).",
          "From a stack only one sheet is written, the rest stays blank.",
          "Upload (editor button) turns an unsigned sheet into an upload sheet: the track, its title and you as uploader, read-only."
        ]
      },
      "de": {
        "title": "Notenblatt",
        "summary": "Formlos aus Papier, Tintenbeutel und Feder. Benutzen öffnet den Notencode-Editor; Signieren mit Titel macht es schreibgeschützt, wie ein beschriebenes Buch. Ein Blatt trägt entweder eine Komposition oder einen hochgeladenen Titel.",
        "details": [
          "Ein Entwurf darf noch Fehler enthalten; Signieren braucht einen Titel und einen fehlerfreien Code. Der Server prüft jedes Speichern erneut (Code höchstens 32000 Zeichen, Titel höchstens 32).",
          "Von einem Stapel wird nur ein Blatt beschrieben, der Rest bleibt leer.",
          "Hochladen (Knopf im Editor) macht aus einem unsignierten Blatt ein Upload-Blatt: Titel, Name und du als Hochladender, schreibgeschützt."
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
        "modules/simplemusic/shared/java/com/simplemusic/client/BurnScreen.java",
        "modules/simplelib/shared/java/com/simplelib/api/client/ui/UiCodeArea.java"
      ],
      "en": {
        "title": "Editor",
        "summary": "Coloured code field with line numbers and underlined problems (the blueprint editor's field, now in SimpleLib). Listen plays the code on your client only.",
        "details": [
          "Buttons: Listen/Stop, Import, Copy (code to the clipboard), Sign, Upload, Done. Closing saves the draft.",
          "Upload opens the upload window: it lists the .ogg files in config/simplemusic/upload/; choose a file, give a title, Upload. The client checks the file first, then sends it in parts. Only upload your own or free music."
        ]
      },
      "de": {
        "title": "Editor",
        "summary": "Farbiges Codefeld mit Zeilennummern und unterstrichenen Fehlern (das Feld des Blaupausen-Editors, jetzt in SimpleLib). Probehören spielt den Code nur auf deinem Client.",
        "details": [
          "Knöpfe: Probehören/Stopp, Importieren, Kopieren (Code in die Zwischenablage), Signieren, Hochladen, Fertig. Schließen speichert den Entwurf.",
          "Hochladen öffnet das Upload-Fenster mit den .ogg-Dateien aus config/simplemusic/upload/; Datei wählen, Titel geben, Hochladen. Der Client prüft die Datei zuerst und schickt sie dann in Stücken. Nur eigene oder freie Musik hochladen."
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
      "id": "sheet_copy",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/sheet/SheetCopy.java",
        "modules/simplemusic/shared/java/com/simplemusic/mixin/CartographyTableMenuMixin.java"
      ],
      "en": {
        "title": "Copying at the cartography table",
        "summary": "Like a blueprint: a signed sheet on top, an empty sheet below gives a copy; only the empty sheet is used up.",
        "details": [
          "A composition gives an unsigned copy you can edit again (code and title, no author); an upload sheet gives an exact copy."
        ]
      },
      "de": {
        "title": "Kopieren am Kartentisch",
        "summary": "Wie beim Bauplan: oben ein signiertes Notenblatt, unten ein leeres ergibt eine Kopie; nur das leere Blatt wird verbraucht.",
        "details": [
          "Eine Komposition gibt eine unsignierte, wieder bearbeitbare Kopie (Code und Titel, ohne Autor); ein Upload-Blatt gibt eine genaue Kopie."
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
        "summary": "Two copper ingots beside a note block over a jukebox. Right-click with a sheet or a pressed disc (composition or track) to put it in, with an empty hand to take it out. It plays on a loop and never ejects.",
        "details": [
          "The server plays each step's tones like a note block (Jukebox/Note Blocks volume, 48 blocks), at most 8 tones per step; one note particle per step.",
          "Comparator: 1-15 by progress through the piece, 0 when empty."
        ]
      },
      "de": {
        "title": "Orchestrion",
        "summary": "Zwei Kupferbarren neben einem Notenblock über einer Jukebox. Rechtsklick mit einem Notenblatt oder einer gepressten Platte (Komposition oder Titel) legt es ein, mit leerer Hand nimmt man es heraus. Es spielt in Schleife und wirft nie aus.",
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
        "modules/simplemusic/shared/resources/data/simplemusic/recipe/blank_disc.json"
      ],
      "en": {
        "title": "Blank Disc",
        "summary": "Four coal or charcoal and four blackstone around an amethyst shard. The disc press presses the music of a sheet onto it.",
        "details": [
          "It no longer opens a window: uploads go through the sheet music editor, discs come out of the disc press."
        ]
      },
      "de": {
        "title": "Rohling",
        "summary": "Vier Kohle oder Holzkohle und vier Schwarzstein um einen Amethystsplitter. Die Plattenpresse presst die Musik eines Notenblatts darauf.",
        "details": [
          "Er öffnet kein Fenster mehr: Hochladen läuft über den Notenblatt-Editor, Platten kommen aus der Plattenpresse."
        ]
      }
    },
    {
      "id": "disc_press",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/press/DiscPress.java",
        "modules/simplemusic/shared/java/com/simplemusic/press/DiscPressBlockEntity.java",
        "modules/simplemusic/shared/java/com/simplemusic/press/DiscPressMenu.java",
        "modules/simplemusic/shared/resources/data/simplemusic/recipe/disc_press.json"
      ],
      "en": {
        "title": "Disc Press",
        "summary": "A piston over two iron ingots over three smooth stone. Put a sheet of music on top and blank discs below: it presses the sheet's music (composition or uploaded track) onto a blank in 2 seconds. The sheet always stays.",
        "details": [
          "Only compositions without problems are pressed. The pressed disc waits in the output slot; the next one follows when it is taken.",
          "Hoppers: from above into the sheet slot, from the sides into the blank slot, out of the bottom only the pressed disc. Comparator: how full it is. Needs a pickaxe."
        ]
      },
      "de": {
        "title": "Plattenpresse",
        "summary": "Ein Kolben über zwei Eisenbarren über drei glatten Steinen. Oben ein Notenblatt, unten Rohlinge: sie presst die Musik des Blatts (Komposition oder hochgeladener Titel) in 2 Sekunden auf einen Rohling. Das Notenblatt bleibt immer erhalten.",
        "details": [
          "Nur fehlerfreie Kompositionen werden gepresst. Die fertige Platte wartet im Ausgang; die nächste folgt, wenn sie herausgenommen ist.",
          "Trichter: von oben ins Notenblatt-Feld, von der Seite ins Rohling-Feld, unten heraus nur die fertige Platte. Komparator: Füllstand. Braucht eine Spitzhacke."
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
        "modules/simplemusic/shared/java/com/simplemusic/disc/SheetPlayback.java",
        "modules/simplemusic/shared/java/com/simplemusic/disc/JukeboxDiscs.java",
        "modules/simplemusic/shared/java/com/simplemusic/client/TrackPlayer.java",
        "modules/simplemusic/shared/java/com/simplemusic/client/TrackCache.java"
      ],
      "en": {
        "title": "Custom Disc",
        "summary": "A pressed disc carries either an uploaded track (hash, title, length, uploader - never audio; amethyst label) or a composition (paper label). Both play in the Orchestrion (loop, playlist) and in the Vanilla jukebox (once); the Pocket Jukebox takes track discs.",
        "details": [
          "Track discs: players within 64 blocks get the file from the server in parts the first time (cache in simplemusic-cache/ of the game folder, 512 MiB) and join at the current position. A deleted track leaves a silent disc.",
          "Composition discs in the jukebox: the server plays the tones like the Orchestrion, once."
        ]
      },
      "de": {
        "title": "Eigene Platte",
        "summary": "Eine gepresste Platte trägt entweder einen hochgeladenen Titel (Hash, Titel, Länge, Hochladender - nie Audiodaten; Amethyst-Etikett) oder eine Komposition (Papier-Etikett). Beide spielen im Orchestrion (Schleife, Playlist) und in der Vanilla-Jukebox (einmal); die Taschen-Jukebox nimmt Titel-Platten.",
        "details": [
          "Titel-Platten: Spieler in 64 Blöcken bekommen die Datei beim ersten Mal in Stücken vom Server (Cache in simplemusic-cache/ im Spielordner, 512 MiB) und steigen an der aktuellen Stelle ein. Ein gelöschter Titel hinterlässt eine stumme Platte.",
          "Kompositions-Platten in der Jukebox: der Server spielt die Töne wie das Orchestrion, einmal."
        ]
      }
    },
    {
      "id": "pocket_jukebox",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
        "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketPlayback.java",
        "modules/simplemusic/shared/java/com/simplemusic/client/PocketPlayer.java",
        "modules/simplemusic/shared/resources/data/simplemusic/recipe/pocket_jukebox.json",
        "modules/simplemusic/shared/resources/data/simplemusic/recipe/red_pocket_jukebox_dyeing.json"
      ],
      "en": {
        "title": "Pocket Jukebox",
        "summary": "A bundle for up to 20 discs (Vanilla discs and track discs). Use it in the main hand to play or pause; only you hear it, at full volume wherever you go. Sneak + right click on a block puts it down (see Placed Pocket Jukebox).",
        "details": [
          "Recipe: 8 copper nuggets around a bundle, a diamond on top (the colour comes from the bundle). Dye it any time with a dye in the crafting grid: the discs stay in.",
          "Discs go in and out like a bundle: left click puts one in, right click with an empty hand takes the newest out.",
          "Plays the discs in the order they went in and starts over after the last. Pausing keeps the position. Starting it stops other music and pauses another pocket jukebox.",
          "Stops when the playing disc is taken out or the jukebox leaves your inventory. The off hand only holds it.",
          "Back port of the Pocket Jukebox of the Bedrock preview 26.70.21 (Java has none yet; checked again on every port)."
        ]
      },
      "de": {
        "title": "Taschen-Jukebox",
        "summary": "Ein Bündel für bis zu 20 Platten (Vanilla- und Titel-Platten). In der Haupthand benutzen: abspielen oder pausieren; nur du hörst es, in voller Lautstärke, egal wo. Schleichen + Rechtsklick auf einen Block stellt sie ab (siehe Abgestellte Taschen-Jukebox).",
        "details": [
          "Rezept: 8 Kupfernuggets um ein Bündel, oben ein Diamant (die Farbe kommt vom Bündel). Jederzeit mit Farbstoff im Handwerksfeld umfärbbar: die Platten bleiben drin.",
          "Platten rein und raus wie beim Bündel: Linksklick legt eine hinein, Rechtsklick mit leerer Hand nimmt die neueste heraus.",
          "Spielt die Platten in der Reihenfolge, in der sie hineinkamen, nach der letzten von vorn. Pause merkt sich die Stelle. Der Start stoppt andere Musik und pausiert eine andere Taschen-Jukebox.",
          "Stoppt, wenn die laufende Platte herausgenommen wird oder die Jukebox dein Inventar verlässt. Die Nebenhand hält sie nur.",
          "Rückport der Pocket Jukebox aus der Bedrock-Vorschau 26.70.21 (Java hat noch keine; bei jedem Port neu geprüft)."
        ]
      }
    },
    {
      "id": "pocket_jukebox_placed",
      "sources": [
        "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxBlock.java",
        "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxBlockEntity.java",
        "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
        "modules/simplemusic/shared/java/com/simplemusic/disc/DiscPlayback.java",
        "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/pocket_jukebox.json"
      ],
      "en": {
        "title": "Placed Pocket Jukebox",
        "summary": "Sneak + right click with the pocket jukebox on a block puts it down, front to you. Placed, it plays its discs for everyone nearby like a jukebox.",
        "details": [
          "Right click with a disc puts it in (up to 20); if nothing played, it starts. Right click without a disc plays or pauses, sneak + right click with an empty hand throws the newest disc out on top.",
          "Same order, loop and pause as in the hand: the first disc in plays first, after the last the first again, a pause keeps the position. A jukebox that played in your hand goes on playing where you put it down.",
          "The sound comes from the block like a jukebox (64 blocks, Jukebox/Note Blocks volume); players who come near later hear it from where the song is. Track discs are heard only by those allowed to hear them (moderation).",
          "Redstone like a jukebox: signal 15 while it plays, a comparator reads the current disc's Vanilla value (a track disc 1, empty 0). Parrots dance, allays and sculk sensors hear it.",
          "Breaking it (any tool, also explosions and pistons) gives the pocket jukebox back with all its discs and the position; in creative a jukebox with discs drops like a shulker box. It also goes on after a restart."
        ]
      },
      "de": {
        "title": "Abgestellte Taschen-Jukebox",
        "summary": "Schleichen + Rechtsklick mit der Taschen-Jukebox auf einen Block stellt sie ab, die Vorderseite zu dir. Abgestellt spielt sie ihre Platten wie eine Jukebox für alle in der Nähe.",
        "details": [
          "Rechtsklick mit einer Platte legt sie hinein (bis 20); lief nichts, startet sie. Rechtsklick ohne Platte spielt oder pausiert, Schleichen + Rechtsklick mit leerer Hand wirft die neueste Platte oben heraus.",
          "Reihenfolge, Schleife und Pause wie in der Hand: die zuerst eingelegte zuerst, nach der letzten von vorn, die Pause merkt sich die Stelle. Spielte die Jukebox in der Hand, spielt sie abgestellt an derselben Stelle weiter.",
          "Der Klang kommt wie bei der Jukebox vom Block (64 Blöcke, Lautstärke Jukebox/Notenblöcke); wer später dazukommt, hört ab der aktuellen Stelle. Titel-Platten hören nur die, die sie hören dürfen (Freigabe).",
          "Redstone wie bei der Jukebox: Signal 15, solange sie spielt, ein Komparator liest den Vanilla-Wert der aktuellen Platte (Titel-Platte 1, leer 0). Papageien tanzen, Allays und Sculk-Sensoren hören sie.",
          "Abbauen (jedes Werkzeug, auch Explosionen und Kolben) gibt die Taschen-Jukebox mit allen Platten und der Stelle zurück; im Kreativmodus fällt eine Jukebox mit Platten wie eine Shulkerkiste heraus. Nach einem Neustart spielt sie weiter."
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
        "summary": "Everyone may upload up to 5 tracks; others hear a track only after an operator approves it, until then only the uploader. In single player and for the LAN host it is approved at once.",
        "details": [
          "Operators: /music list [pending], /music approve|reject|delete <hash>, /music info <hash> (hash or its first 6+ characters), /music ban|unban <player>.",
          "Tracks live in <world>/simplemusic/tracks/ with an index.json; the same file is stored once."
        ]
      },
      "de": {
        "title": "Moderation",
        "summary": "Jeder darf bis zu 5 Titel hochladen; andere hören einen Titel erst nach Freigabe durch einen Operator, bis dahin nur der Hochladende. Im Einzelspieler und für den LAN-Host ist er sofort frei.",
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
      "id": "simplemusic:black_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:black_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/black_pocket_jukebox.json",
      "ingredients": [
        "minecraft:black_bundle",
        "minecraft:copper_nugget",
        "minecraft:diamond"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:black_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:black_bundle",
            "count": 1
          },
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:black_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:black_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/black_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:black_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:black_dye"
        ]
      }
    },
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
      "id": "simplemusic:blue_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:blue_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/blue_pocket_jukebox.json",
      "ingredients": [
        "minecraft:blue_bundle",
        "minecraft:copper_nugget",
        "minecraft:diamond"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:blue_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:blue_bundle",
            "count": 1
          },
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:blue_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:blue_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/blue_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:blue_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:blue_dye"
        ]
      }
    },
    {
      "id": "simplemusic:brown_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:brown_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/brown_pocket_jukebox.json",
      "ingredients": [
        "minecraft:brown_bundle",
        "minecraft:copper_nugget",
        "minecraft:diamond"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:brown_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:brown_bundle",
            "count": 1
          },
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:brown_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:brown_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/brown_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:brown_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:brown_dye"
        ]
      }
    },
    {
      "id": "simplemusic:cyan_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:cyan_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/cyan_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:cyan_bundle",
        "minecraft:diamond"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:cyan_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:cyan_bundle",
            "count": 1
          },
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:cyan_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:cyan_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/cyan_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:cyan_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:cyan_dye"
        ]
      }
    },
    {
      "id": "simplemusic:disc_press",
      "type": "minecraft:crafting_shaped",
      "category": "misc",
      "group": null,
      "result": {
        "id": "simplemusic:disc_press",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/disc_press.json",
      "ingredients": [
        "minecraft:iron_ingot",
        "minecraft:piston",
        "minecraft:smooth_stone"
      ],
      "pattern": [
        " P ",
        "I I",
        "SSS"
      ],
      "key": {
        "P": [
          "minecraft:piston"
        ],
        "I": [
          "minecraft:iron_ingot"
        ],
        "S": [
          "minecraft:smooth_stone"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:cobblestone",
            "count": 4
          },
          {
            "id": "minecraft:iron_ingot",
            "count": 3
          },
          {
            "id": "minecraft:stone",
            "count": 3
          },
          {
            "id": "minecraft:redstone",
            "count": 1
          },
          {
            "id": "minecraft:oak_log",
            "count": 0.75
          }
        ]
      }
    },
    {
      "id": "simplemusic:gray_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:gray_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/gray_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:gray_bundle"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:gray_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:gray_bundle",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:gray_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:gray_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/gray_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:gray_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:gray_dye"
        ]
      }
    },
    {
      "id": "simplemusic:green_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:green_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/green_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:green_bundle"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:green_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:green_bundle",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:green_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:green_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/green_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:green_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:green_dye"
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
      "id": "simplemusic:light_blue_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:light_blue_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/light_blue_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:light_blue_bundle"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:light_blue_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:light_blue_bundle",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:light_blue_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:light_blue_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/light_blue_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:light_blue_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:light_blue_dye"
        ]
      }
    },
    {
      "id": "simplemusic:light_gray_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:light_gray_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/light_gray_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:light_gray_bundle"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:light_gray_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:light_gray_bundle",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:light_gray_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:light_gray_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/light_gray_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:light_gray_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:light_gray_dye"
        ]
      }
    },
    {
      "id": "simplemusic:lime_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:lime_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/lime_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:lime_bundle"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:lime_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:lime_bundle",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:lime_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:lime_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/lime_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:lime_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:lime_dye"
        ]
      }
    },
    {
      "id": "simplemusic:magenta_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:magenta_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/magenta_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:magenta_bundle"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:magenta_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:magenta_bundle",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:magenta_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:magenta_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/magenta_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:magenta_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:magenta_dye"
        ]
      }
    },
    {
      "id": "simplemusic:orange_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:orange_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/orange_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:orange_bundle"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:orange_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:orange_bundle",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:orange_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:orange_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/orange_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:orange_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:orange_dye"
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
      "id": "simplemusic:pink_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:pink_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/pink_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:pink_bundle"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:pink_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:pink_bundle",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:pink_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:pink_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/pink_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:pink_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:pink_dye"
        ]
      }
    },
    {
      "id": "simplemusic:pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/pocket_jukebox.json",
      "ingredients": [
        "minecraft:bundle",
        "minecraft:copper_nugget",
        "minecraft:diamond"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:leather",
            "count": 1
          },
          {
            "id": "minecraft:string",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:purple_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:purple_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/purple_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:purple_bundle"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:purple_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:purple_bundle",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:purple_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:purple_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/purple_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:purple_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:purple_dye"
        ]
      }
    },
    {
      "id": "simplemusic:red_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:red_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/red_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:red_bundle"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:red_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:red_bundle",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:red_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:red_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/red_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:red_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:red_dye"
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
    },
    {
      "id": "simplemusic:white_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:white_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/white_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:white_bundle"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:white_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:white_bundle",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:white_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:white_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/white_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:white_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:white_dye"
        ]
      }
    },
    {
      "id": "simplemusic:yellow_pocket_jukebox",
      "type": "minecraft:crafting_shaped",
      "category": "equipment",
      "group": "pocket_jukebox",
      "result": {
        "id": "simplemusic:yellow_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/yellow_pocket_jukebox.json",
      "ingredients": [
        "minecraft:copper_nugget",
        "minecraft:diamond",
        "minecraft:yellow_bundle"
      ],
      "pattern": [
        "NDN",
        "NBN",
        "NNN"
      ],
      "key": {
        "N": [
          "minecraft:copper_nugget"
        ],
        "D": [
          "minecraft:diamond"
        ],
        "B": [
          "minecraft:yellow_bundle"
        ]
      },
      "baseMaterials": {
        "yield": 1,
        "materials": [
          {
            "id": "minecraft:diamond",
            "count": 1
          },
          {
            "id": "minecraft:yellow_bundle",
            "count": 1
          },
          {
            "id": "minecraft:copper_ingot",
            "count": 0.778
          }
        ]
      }
    },
    {
      "id": "simplemusic:yellow_pocket_jukebox_dyeing",
      "type": "minecraft:crafting_transmute",
      "category": "equipment",
      "group": "pocket_jukebox_dye",
      "result": {
        "id": "simplemusic:yellow_pocket_jukebox",
        "count": 1
      },
      "source": "modules/simplemusic/shared/resources/data/simplemusic/recipe/yellow_pocket_jukebox_dyeing.json",
      "ingredients": [
        "#simplemusic:pocket_jukeboxes",
        "minecraft:yellow_dye"
      ],
      "slots": {
        "input": [
          "#simplemusic:pocket_jukeboxes"
        ],
        "material": [
          "minecraft:yellow_dye"
        ]
      }
    }
  ],
  "lootTables": [
    {
      "id": "simplemusic:blocks/black_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:black_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/black_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/blue_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:blue_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/blue_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/brown_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:brown_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/brown_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/cyan_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:cyan_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/cyan_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/disc_press",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:disc_press"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/disc_press.json"
    },
    {
      "id": "simplemusic:blocks/gray_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:gray_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/gray_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/green_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:green_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/green_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/light_blue_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:light_blue_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/light_blue_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/light_gray_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:light_gray_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/light_gray_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/lime_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:lime_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/lime_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/magenta_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:magenta_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/magenta_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/orange_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:orange_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/orange_pocket_jukebox.json"
    },
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
    },
    {
      "id": "simplemusic:blocks/pink_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:pink_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/pink_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/purple_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:purple_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/purple_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/red_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:red_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/red_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/white_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:white_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/white_pocket_jukebox.json"
    },
    {
      "id": "simplemusic:blocks/yellow_pocket_jukebox",
      "kind": "blocks",
      "type": "minecraft:block",
      "pools": [
        {
          "rolls": 1,
          "items": [
            "simplemusic:yellow_pocket_jukebox"
          ],
          "conditions": [],
          "functions": []
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/loot_table/blocks/yellow_pocket_jukebox.json"
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
    },
    {
      "id": "simplemusic:item/pocket_jukeboxes",
      "replace": false,
      "values": [
        {
          "id": "simplemusic:pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:white_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:orange_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:magenta_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:light_blue_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:yellow_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:lime_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:pink_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:gray_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:light_gray_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:cyan_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:purple_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:blue_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:brown_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:green_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:red_pocket_jukebox",
          "required": true
        },
        {
          "id": "simplemusic:black_pocket_jukebox",
          "required": true
        }
      ],
      "source": "modules/simplemusic/shared/resources/data/simplemusic/tags/item/pocket_jukeboxes.json"
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
          "summary": "Four coal or charcoal and four blackstone around an amethyst shard. The disc press presses the music of a sheet onto it.",
          "details": [
            "It no longer opens a window: uploads go through the sheet music editor, discs come out of the disc press."
          ]
        },
        "de": {
          "title": "Rohling",
          "summary": "Vier Kohle oder Holzkohle und vier Schwarzstein um einen Amethystsplitter. Die Plattenpresse presst die Musik eines Notenblatts darauf.",
          "details": [
            "Er öffnet kein Fenster mehr: Hochladen läuft über den Notenblatt-Editor, Platten kommen aus der Plattenpresse."
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
          "summary": "A pressed disc carries either an uploaded track (hash, title, length, uploader - never audio; amethyst label) or a composition (paper label). Both play in the Orchestrion (loop, playlist) and in the Vanilla jukebox (once); the Pocket Jukebox takes track discs.",
          "details": [
            "Track discs: players within 64 blocks get the file from the server in parts the first time (cache in simplemusic-cache/ of the game folder, 512 MiB) and join at the current position. A deleted track leaves a silent disc.",
            "Composition discs in the jukebox: the server plays the tones like the Orchestrion, once."
          ]
        },
        "de": {
          "title": "Eigene Platte",
          "summary": "Eine gepresste Platte trägt entweder einen hochgeladenen Titel (Hash, Titel, Länge, Hochladender - nie Audiodaten; Amethyst-Etikett) oder eine Komposition (Papier-Etikett). Beide spielen im Orchestrion (Schleife, Playlist) und in der Vanilla-Jukebox (einmal); die Taschen-Jukebox nimmt Titel-Platten.",
          "details": [
            "Titel-Platten: Spieler in 64 Blöcken bekommen die Datei beim ersten Mal in Stücken vom Server (Cache in simplemusic-cache/ im Spielordner, 512 MiB) und steigen an der aktuellen Stelle ein. Ein gelöschter Titel hinterlässt eine stumme Platte.",
            "Kompositions-Platten in der Jukebox: der Server spielt die Töne wie das Orchestrion, einmal."
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
          "summary": "Guide to this mod: 23 pages taken from this wiki, shown in your language. Shapeless recipe: book + note block. Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to Simple Music\" gives one for free."
        },
        "de": {
          "summary": "Handbuch zu dieser Mod: 23 Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + Notenblock. Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei Simple Music\" eins."
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
          "summary": "Shapeless from paper, an ink sac and a feather. Use it to open the note code editor; signing with a title makes it read-only, like a written book. A sheet holds either a composition or an uploaded track.",
          "details": [
            "A draft may still contain problems; signing needs a title and a code without problems. The server checks every save again (code at most 32000 characters, title at most 32).",
            "From a stack only one sheet is written, the rest stays blank.",
            "Upload (editor button) turns an unsigned sheet into an upload sheet: the track, its title and you as uploader, read-only."
          ]
        },
        "de": {
          "title": "Notenblatt",
          "summary": "Formlos aus Papier, Tintenbeutel und Feder. Benutzen öffnet den Notencode-Editor; Signieren mit Titel macht es schreibgeschützt, wie ein beschriebenes Buch. Ein Blatt trägt entweder eine Komposition oder einen hochgeladenen Titel.",
          "details": [
            "Ein Entwurf darf noch Fehler enthalten; Signieren braucht einen Titel und einen fehlerfreien Code. Der Server prüft jedes Speichern erneut (Code höchstens 32000 Zeichen, Titel höchstens 32).",
            "Von einem Stapel wird nur ein Blatt beschrieben, der Rest bleibt leer.",
            "Hochladen (Knopf im Editor) macht aus einem unsignierten Blatt ein Upload-Blatt: Titel, Name und du als Hochladender, schreibgeschützt."
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
      "id": "simplemusic:black_pocket_jukebox",
      "name": {
        "en_us": "Black Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:black_pocket_jukebox",
        "simplemusic:black_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:blue_pocket_jukebox",
      "name": {
        "en_us": "Blue Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:blue_pocket_jukebox",
        "simplemusic:blue_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:brown_pocket_jukebox",
      "name": {
        "en_us": "Brown Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:brown_pocket_jukebox",
        "simplemusic:brown_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:cyan_pocket_jukebox",
      "name": {
        "en_us": "Cyan Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:cyan_pocket_jukebox",
        "simplemusic:cyan_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:disc_press",
      "name": {
        "en_us": "Disc Press",
        "de_de": "Plattenpresse"
      },
      "note": {
        "en": {
          "title": "Disc Press",
          "summary": "A piston over two iron ingots over three smooth stone. Put a sheet of music on top and blank discs below: it presses the sheet's music (composition or uploaded track) onto a blank in 2 seconds. The sheet always stays.",
          "details": [
            "Only compositions without problems are pressed. The pressed disc waits in the output slot; the next one follows when it is taken.",
            "Hoppers: from above into the sheet slot, from the sides into the blank slot, out of the bottom only the pressed disc. Comparator: how full it is. Needs a pickaxe."
          ]
        },
        "de": {
          "title": "Plattenpresse",
          "summary": "Ein Kolben über zwei Eisenbarren über drei glatten Steinen. Oben ein Notenblatt, unten Rohlinge: sie presst die Musik des Blatts (Komposition oder hochgeladener Titel) in 2 Sekunden auf einen Rohling. Das Notenblatt bleibt immer erhalten.",
          "details": [
            "Nur fehlerfreie Kompositionen werden gepresst. Die fertige Platte wartet im Ausgang; die nächste folgt, wenn sie herausgenommen ist.",
            "Trichter: von oben ins Notenblatt-Feld, von der Seite ins Rohling-Feld, unten heraus nur die fertige Platte. Komparator: Füllstand. Braucht eine Spitzhacke."
          ]
        }
      },
      "craftedBy": [
        "simplemusic:disc_press"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:gray_pocket_jukebox",
      "name": {
        "en_us": "Gray Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:gray_pocket_jukebox",
        "simplemusic:gray_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:green_pocket_jukebox",
      "name": {
        "en_us": "Green Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:green_pocket_jukebox",
        "simplemusic:green_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:light_blue_pocket_jukebox",
      "name": {
        "en_us": "Light Blue Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:light_blue_pocket_jukebox",
        "simplemusic:light_blue_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:light_gray_pocket_jukebox",
      "name": {
        "en_us": "Light Gray Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:light_gray_pocket_jukebox",
        "simplemusic:light_gray_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:lime_pocket_jukebox",
      "name": {
        "en_us": "Lime Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:lime_pocket_jukebox",
        "simplemusic:lime_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:magenta_pocket_jukebox",
      "name": {
        "en_us": "Magenta Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:magenta_pocket_jukebox",
        "simplemusic:magenta_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:orange_pocket_jukebox",
      "name": {
        "en_us": "Orange Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:orange_pocket_jukebox",
        "simplemusic:orange_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:orchestrion",
      "name": {
        "en_us": "Orchestrion",
        "de_de": "Orchestrion"
      },
      "note": {
        "en": {
          "title": "Orchestrion",
          "summary": "Two copper ingots beside a note block over a jukebox. Right-click with a sheet or a pressed disc (composition or track) to put it in, with an empty hand to take it out. It plays on a loop and never ejects.",
          "details": [
            "The server plays each step's tones like a note block (Jukebox/Note Blocks volume, 48 blocks), at most 8 tones per step; one note particle per step.",
            "Comparator: 1-15 by progress through the piece, 0 when empty."
          ]
        },
        "de": {
          "title": "Orchestrion",
          "summary": "Zwei Kupferbarren neben einem Notenblock über einer Jukebox. Rechtsklick mit einem Notenblatt oder einer gepressten Platte (Komposition oder Titel) legt es ein, mit leerer Hand nimmt man es heraus. Es spielt in Schleife und wirft nie aus.",
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
    },
    {
      "id": "simplemusic:pink_pocket_jukebox",
      "name": {
        "en_us": "Pink Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:pink_pocket_jukebox",
        "simplemusic:pink_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:pocket_jukebox",
      "name": {
        "en_us": "Pocket Jukebox"
      },
      "note": {
        "en": {
          "title": "Pocket Jukebox",
          "summary": "A bundle for up to 20 discs (Vanilla discs and track discs). Use it in the main hand to play or pause; only you hear it, at full volume wherever you go. Sneak + right click on a block puts it down (see Placed Pocket Jukebox).",
          "details": [
            "Recipe: 8 copper nuggets around a bundle, a diamond on top (the colour comes from the bundle). Dye it any time with a dye in the crafting grid: the discs stay in.",
            "Discs go in and out like a bundle: left click puts one in, right click with an empty hand takes the newest out.",
            "Plays the discs in the order they went in and starts over after the last. Pausing keeps the position. Starting it stops other music and pauses another pocket jukebox.",
            "Stops when the playing disc is taken out or the jukebox leaves your inventory. The off hand only holds it.",
            "Back port of the Pocket Jukebox of the Bedrock preview 26.70.21 (Java has none yet; checked again on every port)."
          ]
        },
        "de": {
          "title": "Taschen-Jukebox",
          "summary": "Ein Bündel für bis zu 20 Platten (Vanilla- und Titel-Platten). In der Haupthand benutzen: abspielen oder pausieren; nur du hörst es, in voller Lautstärke, egal wo. Schleichen + Rechtsklick auf einen Block stellt sie ab (siehe Abgestellte Taschen-Jukebox).",
          "details": [
            "Rezept: 8 Kupfernuggets um ein Bündel, oben ein Diamant (die Farbe kommt vom Bündel). Jederzeit mit Farbstoff im Handwerksfeld umfärbbar: die Platten bleiben drin.",
            "Platten rein und raus wie beim Bündel: Linksklick legt eine hinein, Rechtsklick mit leerer Hand nimmt die neueste heraus.",
            "Spielt die Platten in der Reihenfolge, in der sie hineinkamen, nach der letzten von vorn. Pause merkt sich die Stelle. Der Start stoppt andere Musik und pausiert eine andere Taschen-Jukebox.",
            "Stoppt, wenn die laufende Platte herausgenommen wird oder die Jukebox dein Inventar verlässt. Die Nebenhand hält sie nur.",
            "Rückport der Pocket Jukebox aus der Bedrock-Vorschau 26.70.21 (Java hat noch keine; bei jedem Port neu geprüft)."
          ]
        }
      },
      "craftedBy": [
        "simplemusic:pocket_jukebox"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:purple_pocket_jukebox",
      "name": {
        "en_us": "Purple Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:purple_pocket_jukebox",
        "simplemusic:purple_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:red_pocket_jukebox",
      "name": {
        "en_us": "Red Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:red_pocket_jukebox",
        "simplemusic:red_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:white_pocket_jukebox",
      "name": {
        "en_us": "White Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:white_pocket_jukebox",
        "simplemusic:white_pocket_jukebox_dyeing"
      ],
      "usedIn": []
    },
    {
      "id": "simplemusic:yellow_pocket_jukebox",
      "name": {
        "en_us": "Yellow Pocket Jukebox"
      },
      "note": {
        "sources": [
          "modules/simplemusic/shared/java/com/simplemusic/pocket/PocketJukeboxItem.java",
          "modules/simplemusic/shared/java/com/simplemusic/registry/MusicRegistry.java"
        ],
        "en": {
          "summary": "A dyed Pocket Jukebox: same as the undyed one (see Pocket Jukebox). Crafted from the bundle of that colour or dyed in the crafting grid; the discs stay in."
        },
        "de": {
          "summary": "Eine gefärbte Taschen-Jukebox: wie die ungefärbte (siehe Taschen-Jukebox). Aus dem Bündel dieser Farbe gefertigt oder im Handwerksfeld gefärbt; die Platten bleiben drin."
        }
      },
      "craftedBy": [
        "simplemusic:yellow_pocket_jukebox",
        "simplemusic:yellow_pocket_jukebox_dyeing"
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
    "features": 17,
    "recipes": 38,
    "lootTables": 19,
    "tags": 2,
    "advancements": 0,
    "enchantments": 0,
    "items": 4,
    "blocks": 19,
    "trades": 0,
    "config": 0,
    "quests": 0,
    "recipesOtherLines": 0,
    "undocumented": 0,
    "inWorld": 0,
    "incompleteProse": 0
  }
};
