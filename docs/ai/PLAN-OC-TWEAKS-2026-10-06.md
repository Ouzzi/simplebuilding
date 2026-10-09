# Plan Nachtrag 11 – acht Gameplay-Punkte (2026-10-06)

Branch `claude-oc-tweaks11`, Basis `1b50ab578`, Worktree `C:\Users\o_o\AppData\Local\Temp\cl-oc-tweaks11`.
Umfang: die acht offenen Gameplay-Zeilen aus Nachtrag 11 (`QUEUE.md` bleibt unangetastet, kein Push, kein Client).
Nicht enthalten (andere Sessions): Vorschlaghammer-/Besatz-Konsistenz (`claude-hammer12`), simplelib-Elemente in die
Mod-Tabs verteilen, Stil-Audit, Blaupause-Rückfrage.

## Interpretation (Sprachdiktat, im Bericht genannt)

- **P1 „Schaden im halben Intervall"** = auf kaltem Grund brennt der Seelenbrand doppelt so oft (Intervall halbiert),
  nicht seltener. Kaltes ist: Eis (Tag `minecraft:ice`), Schneeblock, Pulverschnee, Wasser (Block oder Fluid unter den
  Füßen/im Füßenblock). Ohne kalten Grund bleibt das Intervall unverändert.
- **P1 Filter** = leichter blau-dunkler Vollbildfilter, solange der lokale Spieler Seelenbrand hat (kein Text, keine
  Anzeigezeile – nur Farbe). Icon/Restzeit im HUD zeigt Vanilla bereits.
- **P3 „4 Fragmente statt 3"** = das Schichten-Rezept `raw_enderite_synthesis` (heute: drei `RAW_ENDERITE` als Säule).
  Neu: formlos vier `RAW_ENDERITE` → ein `LAYERED_RAW_ENDERITE`. „Raw Enderite Fragment" ist der EN-Name von
  `raw_enderite` (DataIntegrityTests:4935).
- **P6 „betretbar"** = hinein- und herauslaufen wie in einen Kessel (Stellhöhe ≤ 0,6 Block), nicht nur hineinspringen.
  „Schaden wie Magma" = Vanilla-`stepOn`-Weg: `hotFloor`, 1,0 Schaden, nicht bei Schleichen.
- **P8** = Platzhalter nur noch als einzelne Trennzelle zwischen zwei Kategorien; die Auffüllung einer Zeile bis
  Spalte 9 entfällt. Explizite `GAP`-Zellen, `besidePrevious` und `flowOn` bleiben wie bisher.

## Umsetzung (ein Commit je Punkt)

1. **Seelenbrand** (`common/src/shared/.../effect/SoulBurnEffect.java`, `fluid/SoulLava.java`,
   `config/ServerTuningConfig.java`, neuer Client-Mixin `mixin/client/SoulBurnOverlayMixin` + Registrierung in
   `src/main/resources/simplebuilding.client.mixins.json`):
   - `soulBurnSeconds` Standard 60 → 120 (Clamp 5..300 bleibt), Javadoc/Kommentar anpassen.
   - `shouldApplyEffectTickThisTick`: Intervall bei kaltem Standort halbieren (`Math.max(1, interval / 2)`), Kalte
     über Block-/Fluid-Tag am Füßenblock und darunter; nur serverseitig wirksam (Effekt tickt ohnehin serverseitig).
   - Filter: Client-Mixin in die HUD-Extraktion, halbtransparenter Vollbild-`fill` in Turkis/Dunkelblau
     (`0x3FD9E0`-Familie, Alpha ~0x28), nur wenn der lokale Spieler `SOUL_BURN` hat; kein Text.
   - Tests: `ConfigOptionTests` (`server.soulLava.soulBurnSeconds int=60` → 120), neuer GameTest in `CrucibleTests`
     (+ Registrierung `SimpleBuildingGameTests` und `CrucibleGameTest`) für „kaltes Intervall halb, warmes normal".
2. **Magnet-Reichweite** (`items/custom/MagnetItem.java`, `util/PlacedAttractors.java`):
   - `BASE_RANGE` 3,0 → 4,0 · `RANGE_PER_LEVEL` 1,5 → 2,0 · `MAX_RANGE` 7,5 → 9,0 · `HARD_MAX_RANGE` 12,0 → 15,0;
     `PlacedAttractors.RANGE` 6,0 → 8,0. Javadoc (Reichweite seit 2026-09-29) neu formulieren.
   - Tests: `MagnetTests.magnetReachIsThreeBlocksAndRangeWidensItUpToItsCap` (Namensschema, Javadoc, Tabelle
     4 / 6 / 9 / gedeckelt 9 / hart 15, alle Probestellen um die Differenz verschieben, Deckelmessung y-Werte),
     Config-/Tweaks-Tests mit Reichweiten-Strings, gegebenenfalls PlacedAttractors-Test.
3. **Raw-Enderite-Scrap-Rezept** (`src/main/java/.../datagen/ModRecipeProvider.java`, generierte Rezepte):
   - Rezept `raw_enderite_synthesis` von gesetzt (Säule aus 3) zu formlos (4 `RAW_ENDERITE` → `LAYERED_RAW_ENDERITE`);
     alle anderen Enderit-Rezepte bleiben. Danach Datagen (`:mc26_3:fabric:runDatagen` + `syncGenerated263`).
   - Tests/Texte: `SmeltingTests` (Spalte-von-3-Assertions), `tools/guide_book_pages.py`, `wiki/manual.json`
     („column"), Wiki-Generierung.
4. **Elytra-Pad drei Stufen** (`tweaks/block/PadTiers.java`, `TweaksBlocks.java`, `TweaksFamilies.java`,
   `LegacyTierBlock.java`/`LegacyFlypadBlock.java`, `block/entity/ElytraPadBlockEntity.java`,
   `tweaks/easter/EasterEggs.java`, `datagen/ModRecipeProvider.java`, Modelle/Blockstates/Lang):
   - Stufen: `ELYTRA_PAD` 5×5 / H 5, `NETHERITE_ELYTRA_PAD` 32×32 / H 32, `ENDERITE_ELYTRA_PAD` 128×128 / H 192
     (Fußabdruck bleibt 128, nur die Höhe 1,5×128). `PadTiers.MAX` 5 → 3, `ENDERITE` 4 → 3, Tabellen gekürzt.
   - Alte Blöcke weiter platzierbar über `LegacyTierBlock` + `migrate` (Analogon `FlypadBlockEntity` L79-82 und
     `LegacyTierBlockItem` L39/55): `REINFORCED_ELYTRA_PAD` → `ELYTRA_PAD`, `FINE_ELYTRA_PAD` → `ENDERITE_ELYTRA_PAD`;
     Migration auch im Server-Tick des Elytra-Pads, damit Blöcke in der Welt und im Inventar umsteigen.
   - `EasterEggs`: `ELYTRA_PAD`-Schritte/Stufen-Vorlagen auf drei Einträge kürzen, `migrateStage` und Codec (1..5)
     bleiben, damit alte Welten laden.
   - Rezepte: `reinforced_elytra_pad_smithing` und `fine_elytra_pad_smithing` entfallen,
     `netherite_elytra_pad_smithing` setzt auf `ELYTRA_PAD` auf; Kette 5 → 32 → 128.
   - Lang EN **und** DE an beiden Orten (`src/main/resources/.../lang/` und `mc26_3/overlay/resources/.../lang/`),
     `readme.md` („fünf Stufen"), `wiki/manual.json`, `wiki/data/simplemoney.json` (tier 5), `docs/`-Verweise.
   - Tests: `PadOverhaulTests` (Legacy-Test, `padsRows`-Iteration), `TweaksEasterTests:512`, `TweaksTierTests:466`,
     `DataIntegrityTests` (Itemabdeckung, PADS-Tab), `TweaksTests`, Datagen-Ausgabe.
5. **Trank-Pad buffen** (`tweaks/block/PotionPadBlock.java`, `tweaks/TweaksConfig.java`, Javadoc/Wiki):
   - `DURATION_TICKS` 30/60/120 s → 45/90/180 s, `padTuning.potionPadCooldownFactor` 2,0 → 1,5
     (Abklingzeit = Faktor × Wirkdauer), Rampen-/Prozent-Logik unverändert, Easter-Doppel weiterhin 2× (→ 360 s).
   - Tests: `ConfigOptionTests` (Faktor-Default, Options-Strings), `TweaksTests:1148`, `PotionPadTests:195/218/264`,
     `TweaksEasterTests:139/328/336/338`, `ImmersionTests`, `PerformanceTests` bei Bedarf.
6. **Crucible betretbar + Magma-Schaden** (`modules/simplelib/shared/.../crucible/CrucibleBlock.java`):
   - Eigene `COLLISION`-Form: Fuß + Boden + Bauch nur bis y 9 (9/16 = 0,5625 ≤ Stellhöhe 0,6), Innenraum
     (3..13) ausgespart, Hals y11..14 ohne Kollision – Ausformung (`getShape`) bleibt die Kessel-Silhouette.
     `getCollisionShape` gibt diese Form zurück.
   - `stepOn` wie Vanilla-`MagmaBlock`: nur serverseitig, nur `LivingEntity`, nicht bei Schleichen, `hotFloor` 1,0,
     ausgelöst wenn `CrucibleBlockEntity.heat().atLeast(HeatLevel.HIGH)` (Lava/Seelenlava). UI-Dateien unberührt.
   - Tests in `CrucibleTests` (+ Registrierung): Innenraum-/Rand-Kollision (steigbar, Innenraum leer), `stepOn` bei
     HIGH verletzt, bei MEDIUM nicht, Schleichen nicht.
7. **Resonanzstab** (`mixin/client/HeldItemRendererMixin` in `common/src/mc26_2` **und** `mc26_3/overlay`,
   `tweaks/TweaksConfig.java`, `docs/CONFIG.md`):
   - `applyRodTilt`: Neigung −30° → −60° (weiter nach vorn, Laser kommt aus der Spitze); beide Linien identisch,
     `mc1_21_11` bleibt unberührt (Port-Lauf).
   - `laserPointer.color` Standard `0xFF0000` → `0xB38EF3` (Amethyst hell, aus der Amethyst-Rampe des
     Textur-Skripts); `docs/CONFIG.md` Zeile 74/105.
   - Tests: `ConfigOptionTests:697` (`int=16711680` → `11767539`), `TweaksTests:1161` (`color=16711680` → neu).
8. **Kreativ-Abstandshalter** (`items/CreativeTabLayout.java`, `tweaks/item/TweaksItems.padsRows`):
   - `emit`: statt Auffüllung auf Spalte 9 fortlaufend nur noch **eine** Trennzelle, wenn die vorige Zeile nicht
     exakt aufhörte; `besidePrevious` behält seine Passformprüfung (nicht passend → weiter links wie bisher),
     `flowOn` bleibt, explizite `GAP`-Zellen bleiben. Letzte Zeile ohne Füller (unverändert).
   - PADS-Zeilen aus P4 räumen die reinen Ausrichtungs-Zeilen `pad_tier_4`/`pad_tier_5` auf.
   - Tests: `DataIntegrityTests` (Spacer-Regeln ~2423, `expectSlots` je Tab, Maschinen-Layout-Doku ~2799) –
     Reihenfolge der echten Items bleibt gleich, nur die Platzhalterzahl zwischen den Kategorien ändert sich.

## Reihenfolge und Verifikation

1. Pro Punkt: Code → gefilterter Serverlauf `python tools/testrunner/run.py --targets fabric-263,neoforge-263 --filter <muster>`
   (ein Muster pro Lauf) → Commit mit `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
2. Nach P3/P4 Datagen + `syncGenerated263`; nach allen Punkten `python wiki/generate.py --all` und `--all --check`.
3. Am Ende volles Gate `./gradlew.bat check -q` (in diesem Worktree), Textur-Check nur falls Texturen angefasst werden
   (hier nicht geplant), `tools/guide_book_pages.py` nach P3/P4, Testzentrale neu bauen, Abdeckungstest grün lesen.
4. Abschluss: `OC-BERICHT.md` (nicht committen) mit Vorher/Nachher und Testzeilen; QUEUE-Einträge bleiben offen, bis
   der Besitzer abnimmt. Kein Push, kein Client-Start, keine `mc1_21_11`-Änderung.

## Risiken

- **P1** neuartiger Client-Mixin (muss in die Loader-Liste der client-Mixin-Json; nur Fabric/NeoForge/Forge 26.3 –
  gleiche gemeinsame Datei). Ohne Client keine Sichtabnahme, nur Code-/Kompilierprüfung.
- **P1** Deutung von „halbes Intervall" (mehr Schaden auf Kälte) ist eine Interpretation – im Bericht nennen.
- **P2** Reichweitenänderung trifft Geometrie-Tests (Proben, Deckelmessung); Zahlen bewusst moderat gewählt
  (hart 15 Blöcke nach Config-Faktor), damit Vanilla-Sicherheit erhalten bleibt.
- **P4** größter Punkt: Registrierung, Legacy-Migration, Rezepte, Modelle, zwei Lang-Dateien, Wiki und mehrere
  Testlisten; Reihenfolge P4 vor P8, weil sich daraus die PADS-Zeilen ändern.
- **P6** neue Kollisionsform kann tests berühren, die das Kesselform prüfen (Loot/Hammer/Platzierung); Form nur als
  Kollision neu, Ausformung unverändert.
- **P8** Layout-Erwartungen in `DataIntegrityTests` können Spacer zählen – Prüfung, dann gezielt anpassen.

## Nachprüfung 2026-10-09 (Branch `claude-q-gadgets`)

Ist-Zustand: P1–P8 stecken bereits in `claude-wave1` (Commits `2e67933c2` … `45f36c970`, Fix `7a9a2ceef`); ebenso
verstärkter Kessel, Milchkessel-Jade/JEI und Netherit-Fass aus `claude-crucible4`. Offen waren nur Reste:

1. **P3 Texte:** Leitfaden-Seite `book.simplebuilding.end.3.text` und Erfolg `patience_is_a_virtue` sagten noch
   „drei übereinander“ – auf „vier, beliebig angeordnet“ (EN/DE, beide Lang-Orte).
2. **P7 Farbe:** Tooltip `laserPointer.color` nannte noch „rot (#FF0000)“; Wiki „red aiming dot“. Außerdem behält
   eine vorhandene Config-Datei den alten Standard Rot – `TweaksConfig.validate` hebt genau den alten Standard
   `0xFF0000` auf Amethyst `0xB38EF3` (Muster wie `migrateLegacyFirstJoinCount`; eigene Farben bleiben). Test in
   `TweaksTests.tweaksConfigKeepsItsNamesAndDefaults`.
3. **P6 Config + Wiki:** Brandschaden im heißen Tiegel als SimpleLib-Server-Option `crucibleBurnDamage`
   (Standard 1,0 wie Magma, 0–4, 0 = aus) in `LibConfig`; `CrucibleBlock.stepOn` liest sie; Test in `CrucibleTests`
   (Wert 0 → kein Schaden). Wiki SimpleLib `crucible` EN/DE: betretbar, Brand ab hoher Hitze.
4. **P1 Wiki:** Bildschirmfilter im Wiki (`crucible_parts`) EN/DE erwähnen.
5. Vorschauen `/root/previews/gadgets/` (Resonanzstab-Neigung + Amethyst-Punkt als Skizze aus den echten Sprites,
   Netherit-Fass aus dem Modell-Renderer des Wikis), Queue abhaken, Wiki neu erzeugen.
Verifikation: Compile Fabric/NeoForge/Forge 26.3, Filter `simplebuilding:crucible*` und `simplebuilding:tweaks*`
auf fabric-263/neoforge-263/forge-263, `wiki/generate.py --all --check`.
