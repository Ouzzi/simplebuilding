# Recherche: Pocket Jukebox (Bedrock-Preview), 21.10.2026

Besitzer-Wunsch 21.10.: Pocket Jukebox mit implementieren (Referenzbild: 8 Kupfernuggets um Bündel, Rohling oben → Pocket Jukebox), **färbbar**, im Rezept **Diamant statt Rohling** (konsistent zu unseren Musik-Rezepten).

## Vanilla-Stand (Quelle: minecraft.wiki/w/Pocket_Jukebox, Preview 26.70.21)
- Nur Bedrock, Teil des eingebauten „Music Features Add-On“ (Preview 26.70.21, kommendes Bedrock 26.70 / vierter Drop 2026). Kein Java-Gegenstück angekündigt. Mit-Features: Disc Press (Lieder auf Rohlinge pressen → Track Discs), Jukebox-Playlist aus Truhe darüber (Preview 26.70.20).
- Rezept Vanilla: Kupfernugget(s) + Rohling + Bündel der passenden Farbe; Farbe kommt vom Bündel. Nicht stapelbar, gewöhnlich.
- Fasst bis 20 Platten beliebiger Art, Einlegen/Herausnehmen wie Bündel; Inhalt im Tooltip sichtbar.
- Benutzen: spielt die zuerst eingelegte Platte; danach nächste, nach der letzten wieder von vorn (Schleife). Erneut benutzen = Pause, Fortschritt bleibt, nächste Benutzung setzt fort. Anderen Pocket Jukebox starten pausiert den ersten. Stoppt andere Musik (auch Jukeboxen).
- Nur der Träger hört es, ortsunabhängig, volle Lautstärke.
- Stopp, wenn die laufende Platte entnommen wird oder das Item das Inventar verlässt. Zweithand: halten ja, benutzen nein.
- Jederzeit umfärbbar, 17 Farbvarianten (inkl. ungefärbt), je mit „offen“-Sprite.
- Sounds: Bündel-Sounds (drop_contents, insert, insert_fail, remove_one).
- Nicht dokumentiert: Überspringen/Zufall, Verhalten fallengelassen/in Behältern.

## Umsetzung bei uns (Vorschlag)
- Ort: modules/simplemusic (Musik-Modul, hat schon Rohling/eigene Platte/Upload-Tracks); Speicherlogik wie Bündel (Simple Storage-Bündel als Vorbild, nur per simplelib/ID, Modul bleibt allein spielbar).
- Rezept: 8 Kupfernuggets um Bündel + **Diamant** (statt Rohling), Farbe vom Bündel; Färben mit Farbstoff im Handwerksfeld (wie Bündel-Färben).
- Spielt auch eigene hochgeladene Platten (custom_disc). Client-Sound nur für Träger.
- Texturen: eigene Pixel im Vanilla-Stil (Bündel-/Kupfer-Look), 17 Farben + offen; Abnahme-Seite vorlegen.
- Vanilla-Rückport-Regel: Sobald Java es bekommt, Mod-Teil abschalten (Memory vanilla-rueckports).

Quellen: https://minecraft.wiki/w/Pocket_Jukebox, https://www.minecraft.net/en-us/article/minecraft-preview-26-70-21, https://minecraft.wiki/w/Fourth_Drop_2026
