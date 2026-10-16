# Bericht claude-gatefix11 (Gate 33fdd1a27 rot)
- simplefun guide_book: Ursache war ein Rezeptkonflikt, Buch + Ziegel ergab auch den Simple-Blocks-Guide. Simple Blocks nutzt jetzt Tonklumpen (tools/guides/module_guides.py, Guides neu erzeugt).
- howler_jaw_spawn: planJaw wählte Stand-ins aus Nachbar-Tests im 24-Block-Radius. Neuer Test-Hook TEST_SOLO_PREFIX (Howler jagt nur das Stand-in mit gleichem Tag), Prüfaussage unverändert.
- soulfire_charge/_blaze: Dispenser und Feuerball tickten nicht (Dispenser feuerte nicht, Ball blieb stehen), also Testfläche in nicht tickendem Chunk. Test erzwingt jetzt seine Chunks (setChunkForced, Aufräumen am Testende); wie DisguiseTests.
- music_disc relaying_amplifiers: allein nie rot, gleiches Muster (Jukebox tickt nicht, Chunk-Kante); Chunks werden erzwungen. Nur im Gesamtlauf prüfbar.
- Ergebnisse: simplemobs fabric+neoforge 3x grün (202/202); music_disc* 3 Loader 48/48; fabric-263 komplett 1241/1241; Module simplefun/simplecrucibles/simpleblocks fabric+neoforge grün; data_integrity* fabric+neoforge 100/100. Nicht getestet: Forge-Module, Neoforge-Gesamtlauf; Jukebox-Flake nur per Muster-Analyse behoben.
