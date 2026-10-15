# Bericht oc-gatefix8
1. ModBucketItem.care: `playSound(null,x,y,z,SoundEvent,...)`-Überladung (wie CopperPressurePlateBlock) statt `.value()`; `./gradlew compileJava` (26.2) + 26.3 fabric/gametest/neoforge/forge grün.
2. Loot-Test: Ursache war nicht die Loot-Tabelle, sondern veraltete Erwartung. Astral-Tisch droppt seit f48f6dc01 (Besitzer 15.10.) Enderit-Barren statt Nugget; `DataIntegrityTests` erwartet jetzt Barren (Loot-Tabelle/Datagen unverändert, Test nicht aufgeweicht).
3. Guide-Seite de_de building 19 (Lichtscheiben) gekürzt, Inhalt gleich. `guide_book_pages.py` kannte `addChapter`-Kapitel nicht und nutzte Schlüsselitems statt `hint()`; beides ergänzt. Themenseiten (de): `topics.recipe` "Frei: %s herstellen" -> "Frei: %s" (sonst 14/15 Zeilen). Skript: problems: 0.
4. Wiki: stale Sandwich-Texturkopien (bottom*.png, textures.js) per `wiki/generate.py` synchronisiert (kein Art-Change), sonst checkWiki rot.
Tests: fabric-263 + neoforge-263 je "alles gruen: 2/2" für loot_table_loads und every_guide_page_fits. `./gradlew check -q --continue`: keine Fehler.
Offen/nicht getestet: Forge-GameTests, volles Gate/Standalone nicht gelaufen.
