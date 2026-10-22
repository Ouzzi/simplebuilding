# Plan A-LEGEND22 (22.10.2026): Legendäre Momente in SimpleLib (Queue SP22-2)

Branch `a-legend22` (Basis `claude-wave1` 6799f1190). Besitzer 22.10.: „Der Sound für legendäre Achievements ist schon
ziemlich cool … so etwas oder besser“ – gilt für die schwierigsten Verzauberungen (Mods + Vanilla) und die schwersten
Advancements wie „How Did We Get Here?“; Sitz in simplelib; eigener synthetischer Klang + sichtbarer Effekt am Spieler;
Liste per Tag/Config erweiterbar.

**Abweichung (Besitzer-Vorgabe 22.10. während der Umsetzung: nur 26.3 Fabric):** Keine Anpassungen an NeoForge/Forge.
Geteilter Code (Mixins, `LegendaryMoments`, Tag, Config, Tests) liegt in `shared`; Klang-Registrierung, Effekt-Tick und
Konfig-Bildschirm sind nur im Fabric-Einstieg verdrahtet (`LibConfigScreen` im Fabric-Quellbaum, damit `shared` Cloth nicht
braucht). NeoForge/Forge-Verdrahtung folgt im Port-Run (Klang `RegisterEvent` SOUND_EVENT, `ServerTickEvent`, Bildschirm).

## Ist-Zustand
- SimpleLib hat noch keine Klänge, keine Konfig-Oberfläche (nur `config/simplelib-server.json`, `LibConfig`) und keine
  Advancement-/Verzauberungs-Hooks. Server-Tick-Hook gibt es je Loader schon (`InWorldStrikes.tick`), Registrierung je
  Loader nach dem Muster von `LibPotions` (Fabric direkt, NeoForge/Forge im `RegisterEvent`).
- Vanilla spielt bei Challenge-Advancements nur den Toast-Klang `ui.toast.challenge_complete` (Client, nur der Spieler).
- Synth-Werkzeuge: `tools/sounds/howler_dsp.py` (Hüllkurven, Raum, Mastering, Ogg-Ausgabe über soundfile).
- Konfig-Schalter mit Tooltip: Muster `FarmingScreen` (Cloth Config, Forge über `gradle/forge-native-config.gradle`).

## Entscheidungen
1. **Advancements erkennen:** Mixin auf `PlayerAdvancements#award` am Aufruf `AdvancementRewards#grant` – genau die Stelle,
   an der ein Advancement in diesem Aufruf *fertig* wird (Vanilla ruft sie nur bei `!wasDone && isDone()`). Gilt für alle
   Loader gleich (geteiltes Mixin), auch für Befehle (`/advancement grant`).
   - Liste „legendär“ (Advancements sind keine Registry, daher kein Tag): fest eingebaute Vanilla-Liste der
     Sammel-Challenges, die das ganze Spiel umfassen: `nether/all_effects` (How Did We Get Here?), `nether/all_potions`,
     `adventure/adventuring_time`, `adventure/kill_all_mobs`, `husbandry/balanced_diet`, `husbandry/bred_all_animals`,
     `husbandry/complete_catalogue`, `husbandry/whole_pack`, `adventure/trim_with_all_exclusive_armor_patterns`,
     `husbandry/obtain_netherite_hoe`, `nether/explore_nether`. Andere Challenge-Rahmen (Sniper Duel, Arbalistic …)
     sind Glück/Geschick, kein „Impossible“ – Vanilla-Toast reicht dort.
   - Erweiterbar: Config-Liste `legendaryAdvancements` (IDs), API `LegendaryMoments.addAdvancement(id)` für Mods, Schalter
     `legendaryAllChallenges` (Standard aus): jedes Advancement mit Challenge-Rahmen aus jedem Mod zählt.
2. **Verzauberung erhalten:** zählt, wenn der Spieler ein Item *herstellt*, das eine legendäre Verzauberung auf
   Höchststufe (`level >= maxLevel`) trägt, die die Vorlage noch nicht auf dieser Stufe hatte:
   - Verzauberungstisch (und jeder Mod-Tisch, der wie der Astral-Verzauberer `CriteriaTriggers.ENCHANTED_ITEM` auslöst):
     Mixin auf `EnchantedItemTrigger#trigger`.
   - Amboss: Mixin auf `AnvilMenu#onTake` (Vergleich linke Vorlage ↔ Ergebnis; Bücher über `STORED_ENCHANTMENTS`).
   - Bewusst **nicht**: Aufheben aus Truhen/Loot (sonst feiert jedes Umräumen, und Loot ist kein Verdienst).
   - Liste: Enchantment-Tag `#simplelib:legendary` (Mods ergänzen per Datapack-Tag, `required:false`), Standard die
     Vanilla-Schatz-Verzauberungen, die es weder im Handel noch in Zufalls-Loot gibt (Vanilla-Tags `treasure` minus
     `tradeable`/`on_random_loot`): `swift_sneak`, `soul_speed`, `wind_burst`. Zusätzlich Config-Liste `legendaryEnchantments`.
3. **Effekt (Server, für alle in der Nähe sichtbar/hörbar):** Klang `simplelib:legendary` (SoundSource PLAYERS) am Spieler +
   2 s Partikel: Totem-Ausbruch zu Beginn, zwei goldene Spiralen (`end_rod`) steigen von den Füßen über den Kopf, Glyphen
   (`enchant`) ziehen zum Spieler, am Ende ein kleiner Funkenkranz. Kein Text auf dem Bildschirm (Regel 3.4).
4. **Klang:** synthetisch, `tools/sounds/make_legendary_sound.py` (nutzt `howler_dsp`): ~3,2 s Fanfare – tiefer Gong-Schlag,
   aufsteigender Dur-Akkord (Glocken-/Blechartige Additiv-Partials), Schimmer-Arpeggio, Raumhall; Lautheit wie
   Vanilla-UI-Klänge (aktive RMS ~ -18 dBFS, Spitze -1 dBFS). Zwei Takes (leichte Varianten). Ogg + `sounds.json`
   mit Untertitel `subtitles.simplelib.legendary` (EN „Legendary fanfare“, DE „Legendäre Fanfare“).
5. **Config:** `simplelib-server.json` neu `legendaryEffects` (true), `legendaryAllChallenges` (false),
   `legendaryAdvancements` ([]), `legendaryEnchantments` ([]); Listen auf 256 Einträge begrenzt, ungültige IDs verworfen.
   Konfig-Bildschirm `LibConfigScreen` (Cloth, Schalter + Listen mit Tooltip EN/DE); Cloth bleibt **optional** für
   SimpleLib (Mod Menu und Cloth nur `compileOnly`, kein `suggests` – checkMultimod verlangt gleiche optionale Abhängigkeiten wie modules.json, NeoForge registriert den Bildschirm nur bei geladenem
   `cloth_config`), damit keiner der 16 SimpleLib-Nutzer eine neue Pflicht-Abhängigkeit bekommt. Forge: native Widgets.

## Dateien
- `modules/simplelib/shared/java/com/simplelib/api/LegendaryMoments.java` (Liste, Prüfung, Effekt, Tick), `registry/LibSounds.java`
- Mixins `LegendaryAdvancementMixin`, `LegendaryEnchantTableMixin`, `LegendaryAnvilMixin` (+ `simplelib.mixins.json`)
- `config/LibConfig.java` (+ `save()`), `config/LibConfigScreen.java`, Loader-Einstiege (Klang, Tick, Bildschirm),
  `fabric.mod.json` (modmenu-Einstieg, suggests), Gradle (compileOnly Cloth/ModMenu, Forge native config)
- Ressourcen: `data/simplelib/tags/enchantment/legendary.json`, `assets/simplelib/sounds.json`, `sounds/legendary{1,2}.ogg`, Lang EN/DE
- Tests `test/LegendaryTests.java` + Fabric-Adapter `ModuleGameTest`; Wiki `modules/simplelib/wiki/manual.json` (Feature)

## Tests (Gametests, alle Loader über `LibTests.ALL`)
- `legendary_advancement_celebrates`: „How Did We Get Here?“ per `award` aller Kriterien → genau 1 Feier; normales
  Advancement (`story/mine_stone`) → keine; Schalter aus → keine; Config-Liste erweitert → `story/mine_stone` feiert;
  `legendaryAllChallenges` → Challenge-Rahmen (`adventure/sniper_duel`) feiert.
- `legendary_enchantment_tag`: Tag enthält die drei Vanilla-Verzauberungen; Amboss-Ergebnis Swift Sneak III aus Buch feiert,
  Swift Sneak II nicht, Schärfe V nicht; Config-Liste erweitert → Schärfe V feiert; schon vorhandene Stufe feiert nicht.
- Config-Grenzen (ungültige IDs verworfen) im Test `legendary_enchantments` mitgeprüft (kein eigener Test).

## Verifikation
`/root/sbt a-legend22`: Compile `:mc26_3:fabric` + `:modules:simplelib:fabric`, `module-simplelib-fabric-263`
(Filter `simplelib:*`; SimpleLib hat kein eigenes Standalone-Target), Generatoren (Guides, Wiki, Discover) + `./gradlew check`.
Keine Client-Läufe: Hörbild/Partikel nur über die HTML-Vorschau `/root/sb-tools-a/previews/legend22-preview.html`.

## Offen / Risiken
- Vanilla-Toast-Klang und unsere Fanfare überlagern sich bei Challenge-Advancements; Besitzer entscheidet beim Hören,
  ob der Toast-Klang unterdrückt werden soll (Client-Mixin, nicht Teil dieses Branches).
- Mod-eigene „schwerste“ Verzauberungen/Advancements (SimpleBuilding u. a.) tragen die Mods selbst nach (Tag bzw. API);
  hier nur der Mechanismus + Vanilla-Standard.
