# PLAN B-MACHINES26 (11.11.): b-mach32s4b + b-jadehop26 zusammenführen

Auftrag A (P0/Info 01:45 UTC 11.11.): beide Branches kollidieren (simplemachines build.gradle, modules.json, fabric.mod.json,
ModuleGameTest.java, lang de/en). Ein Branch für Bündel 179.

## Umsetzung
- Basis hub/b-bundle177-gen 296d90b7d (kein bundle178-gen vorhanden), Merge hub/b-mach32s4b 15a2f7cd5, Merge hub/b-jadehop26 2c351e508
  (enthält den checkMultimod-Fix e389e0b98; das Jade-Plugin selbst war schon in 177).
- Konflikte: beide Seiten behalten – Jade-Entrypoint + JEI-Entrypoint, `suggests` jade + jei, modules.json optional
  `jade, simplebuilding, simpleores, jei` (jade nur einmal), Gametests Jade + Auto-Schmied/Crafter, Lang-Schlüssel beider Seiten.
  build.gradle: Modrinth-Repo (Jade) einmal + blamejared-Repo (JEI). Generierte Dateien (wiki/data, MODSTRUKTUR) = Basis, Neuerzeugung im Bündel.
- Abweichung: Test `auto_smither_stops_without_redstone` war im Ketten-Lauf von b-mach32s4b in beiden Modul-Läufen rot
  („no netherite sword on tick 60“, das ausgeworfene Schwert war schon aus dem Suchkasten). Fix: letzter Schritt pollt per thenWaitUntil.

## Prüfung
- Lokal grün: check_data simplemachines, check_tooltip_text, check_lang_keys, multimod, check_dup_impl, precheck --fast;
  javac simplemachines gegen MC-26.3 + Jade-Jar: nur bekannte Access-Widener-Meldungen.
- sbt (26dfb6727): COMPILE_RC=0, TEST_RC=0 (176/176 Modul fabric+standalone), DI_RC=0 (48/48 data_integrity). Log /root/sb-tools-b/b-machines26-sbt.log.
