# Plan EL24-1: Wither-Schwingen (Elytra-Stufen mit Wither-Herz und Nether-Juwel)

Queue EL24-1 (Besitzer 23.10. nachts), Branch `a-elytra24` (Basis `claude-wave1`). NUR 26.3 Fabric.

## Wunsch (sinngemäß)
Eine weitere Elytra mit dem Wither-Herz, mit dem Strong Upgrade geschmiedet, stilistisch mehr „Flügel“. Stufe 1: Elytra + Strong Upgrade +
Wither-Herz; Stufe 2: Stufe 1 + Strong Upgrade + Nether-Juwel. Kein Grundtempo-Bonus: nur die **Höchstgeschwindigkeit** (z. B. mit Raketen)
ist bei Stufe 1 doppelt, bei Stufe 2 vierfach so hoch wie Vanilla (höheres Risiko an Wänden). Später folgen Anpassungen.

## Ist-Stand (erhoben)
- **„Wither-Herz“ = `simplemobs:nether_heart`** (Netherherz). Bis 21.10. hieß es `simplemobs:wither_heart`, IdMove auf `nether_heart`
  (b-wither21, `WitherLoot.idMoves`); Drop des Verdorrten/Abgrund-Withers. **„Nether Gem“ = `simplemobs:nether_gem`** („Red Nether Gem“ /
  „Rotes Nether-Juwel“, Drop des Abgrund-Withers, Tooltip „Sein Zweck folgt später“) – kein Netherstern. Beide in Simple Mobs (`WitherMobs`).
- **Strong-Upgrade-Vorlage** `simplebeacons:strong_upgrade_template` (Simple Beacons, `SmithingTemplateItem`, MYTHIC): aus 9 Splittern
  (Vanilla-Wither), mit Simple Mobs aus Diamant + Weinendem Obsidian + Netherherz bzw. Vervielfältigung; Vorbild `strong_beacon_smithing.json`
  (`smithing_transform`, Vorlage + Leuchtfeuer + Netherstern).
- **Vorhandene Elytra-Erweiterungen:** keine eigenen Elytra-Items in der Familie (Spawn-Elytra/Elytra-Pads gibt es im Code nicht; nur
  HUD/Statistik/Gravitation lesen `isFallFlying`). Kein Konflikt.
- **Vanilla 26.3 Physik:** `LivingEntity.updateFallFlyingMovement` (Gleiten, Luftwiderstand 0,99) hat keine feste Obergrenze; die effektive
  Reisegeschwindigkeit setzt der **Raketenschub** in `FireworkRocketEntity.tick`:
  `v += look·0,1 + (look·1,5 − v)·0,5` je Tick → Gleichgewicht `v* = 0,99·0,85 / (1 − 0,99·0,5) ≈ 1,67 B/Tick (≈ 33 m/s)`.
  Wandschaden `handleFallFlyingCollisions`: `(v_vorher − v_nachher)·10 − 3`, wächst also von selbst mit der Geschwindigkeit.
  Elytra: 432 Haltbarkeit, 1 Punkt je 20 Ticks Flug, Reparatur Phantomhaut, Komponenten `glider` + `equippable` (Asset `minecraft:elytra`).
- Spielerbewegung ist client-simuliert: der Raketenschub des eigenen Spielers wird **auf dem Client** gerechnet (Server rechnet parallel und
  prüft „moved too quickly“ gegen seine eigene `deltaMovement`, Schwelle 300 beim Gleiten).

## Entscheidungen
1. **Ort: Simple Mobs** (`simplemobs`). Begründung: beide Zutaten (Herz, Juwel) gehören Simple Mobs, das Juwel wartet dort auf seinen Zweck,
   das Thema ist der Wither. Simple Beacons liefert nur die Vorlage → Rezept bedingt (`fabric:all_mods_loaded simplebeacons`, + Neo/Forge-Form
   wie bei den bestehenden Rezepten). Registrierung hängt von nichts ab (A1).
2. **Vanilla-Weg ohne Simple Beacons (Regel 1/5):** dieselben Rezepte mit der **Netherit-Aufwertung** als Vorlage, Bedingung „simplebeacons
   nicht geladen“ (`fabric:not`, Vorbild `simpleblocks/*_without_simplebuilding.json`). Mit Simple Beacons ist nur der Strong-Weg aktiv.
3. **Schmiedetisch statt Amboss:** `minecraft:smithing_transform` (Vorlage + Basis + Zusatz) ist der Vanilla-Weg für Vorlagen, behält
   Verzauberungen und Abnutzung (wie Netherit), erscheint im SB-Rezeptbuch des Schmiedetischs. Der Besitzer erlaubte den Amboss nur als
   „erstmal“; ein Amboss-Rezept bräuchte eigenen Code und kennt keine Vorlagen.
4. **Items:** `simplemobs:wither_wings` („Wither Wings“ / „Wither-Schwingen“, Stufe 1) und `simplemobs:nether_wings` („Nether Wings“ /
   „Nether-Schwingen“, Stufe 2). Komponenten wie Vanilla-Elytra (`glider`, `equippable` Brust mit eigenem Equipment-Asset, Elytra-Klang,
   `damageOnHurt=false`), Reparatur **Phantomhaut** (Vanilla), Seltenheit CURSED (wie Herz/Juwel, SimpleLib, Fallback Epic).
   **Haltbarkeit 576 / 768** (432 · 4/3 je Stufe – etwa der Netherit-Sprung Diamant→Netherit, +30 %); Verbrauch je Zeit bleibt Vanilla,
   also mehr Strecke durch mehr Tempo. **Feuerfest** wie Netherit (`damage_resistant #is_fire`) – Nether-Material.
5. **Höchstgeschwindigkeit:** Mixin `FireworkRocketEntity.tick` (`@ModifyArg` auf `LivingEntity.setDeltaMovement`): trägt der angehängte,
   gleitende Träger Schwingen mit Faktor f, kommt `look·0,85·(f−1)` zum Vanilla-Ergebnis hinzu. Das entspricht genau dem Vanilla-Schub mit
   Ziel `1,5·f` statt `1,5` und Zusatz `0,1·f` → Gleichgewicht `f · 1,67 B/Tick`. Ohne Rakete ändert sich nichts (kein Grundtempo-Bonus,
   Gleiten/Sturzflug Vanilla); Wandschaden bleibt die Vanilla-Formel und steigt dadurch.
   **Config** `simplemobs-server.json`: `witherWingsSpeedFactor` = 2.0, `netherWingsSpeedFactor` = 4.0, begrenzt 1–8.
   **Server-autoritativ:** der Server stempelt den aktuellen Faktor beim Raketenschub in die getragenen Schwingen
   (`minecraft:custom_data` `simplemobs:speed_factor`, synchronisiert mit dem Item); der Client rechnet mit dem Stempel (begrenzt 1–8),
   ohne Stempel mit dem Standardwert der Stufe. So stimmen Client und Server-Prüfung auch bei geänderter Server-Config überein.
6. **Optik „mehr Flügel“:** eigenes Equipment-Asset `simplemobs:wither_wings` / `nether_wings` (Schicht `wings`, ohne Spieler-Umhang)
   mit eigener 64×32-Textur (nur Elytra-UV-Netz 22..46 × 0..22 belegt, Ausschnitte = Federn per Cutout; Vorder-/Rückseite gespiegelt,
   damit die Kanten der 2 px dicken Platte passen) + Item-Symbol (16×16, + „kaputt“-Variante wie Vanilla `elytra_broken`).
   Generator `tools/textures/elytra24_textures.py` (reines Python, Ebenen-Quellen `.layers.json`, `--check`, `--preview`);
   drei Varianten in der Vorschau `/root/sb-tools-a/previews/elytra24-preview.html`: **A Federschwingen** (empfohlen, eingebaut),
   B Knochenschwingen (Fledermaus/Drache), C Aschefedern. Vanilla-Elytra nur in der Vorschau zum Vergleich. Stufe 2 = gleiche Form,
   Glut-Adern in Juwel-Rot. Nur dieses eine Beispiel (zwei Stufen) – Feinschliff nach Abnahme („UI/Optik erst exemplarisch“).
7. **Kreativ:** eigener Tab (TAB_ITEMS nach dem Shellker-Helm), Vanilla-Tab Werkzeuge nach der Elytra (hinter `vanillaTabs()`).

## Dateien
- `modules/simplemobs/shared/java/.../WitherWings.java` (neu: Items, Faktor, Schub-Formel, Stempel)
- `modules/simplemobs/shared/java/.../mixin/FireworkRocketWingsMixin.java` (neu) + `simplemobs.mixins.json`
- `WitherMobs`/`MobsRegistry` (Registrierung, Tab), `MobsConfig` (2 Faktoren), `MobsFabric` (Vanilla-Tab)
- Ressourcen: `items/`, `models/item/`, `equipment/`, `textures/item/`, `textures/entity/equipment/wings/`, Lang EN/DE,
  Rezepte (2 Strong + 2 Vanilla-Ersatz), `questionary`, `wiki/manual.json`, `tools/check_data.py`
- Tests: `WingsTests.java` (Rezepte je nach geladenem Simple Beacons, Komponenten/Haltbarkeit/Reparatur, Schub-Formel Gleichgewicht
  1 : 2 : 4, echter Raketen-Tick mit gleitendem Mock-Spieler Vanilla vs. Stufe 1 vs. Stufe 2, Stempel/Config), Fabric-Adapter `ModuleGameTest`
- Generiert (sb-test): Discover (`discover.py`), Guides, Wiki (`--all`), `wiki/data/textures.js`; `docs/MODS-UND-FEATURES.md`
- Simple Money: Simple Mobs hat keine `prices.json` (keine Preise für Herz/Juwel) → keine Preise für die Schwingen.

## Nicht in diesem Schritt
NeoForge/Forge-Adapter (Registrierung läuft dort über `WitherMobs.items`, Mixin ist gemeinsam; nicht kompiliert/getestet, NUR-Fabric-Regel);
Client-Sicht (keine Client-Läufe erlaubt); `simplebeacons` als `optional` in `modules.json` (bisher nennt auch Simple Beacons Simple Mobs nur
über Rezeptbedingungen – gleiche Praxis, keine Loader-Metadaten auf Neo/Forge anfassen).

## Verifikation
`/root/sbt a-elytra24`: Compile `:mc26_3:fabric:compileJava :modules:simplemobs:fabric:compileJava`; Targets `module-simplemobs-fabric-263`,
`module-simplemobs-standalone-fabric-263` (Vanilla-Ersatzweg), `fabric-263 --filter 'simplebuilding:*'` (Datenintegrität/Tabs);
Generatoren `elytra24_textures.py --check`, `layers.py check`, Guides/Wiki/Discover, danach `./gradlew check -q`.
