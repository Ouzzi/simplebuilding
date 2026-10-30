# Plan A29: Simple Animals (BS28-1 / ABN29b-3, Agent a-animals29)

Stand 29.10.2026. Nur MC 26.3 Fabric. Konzept: `docs/ai/KONZEPT-A-WP18-2026-10-29.md` (§0a Runde 2). Neues Sub-Mod `simpleanimals` mit den fünf abgenommenen Tieren. Vorschau (3D, drehbar): `/root/sb-tools-a/previews/a-animals29-preview.html`.

## 1. Ist-Zustand (erhoben)
- Vorbild für Aufbau und Tests: `simplewanderers` (Entitäten, Spawn-Ei, Config + `/simplewanderers config`, Handbuch, Testzentrale, Gametests mit eigener Arena) und `simpleend` (eigenes Modell `NibblemiteModel`, `tools/gen_textures.py`). Vanilla-Quellen (26.3 NeoForge-gepatcht) für Armadillo, PolarBear, Horse/Mule/Donkey/AbstractHorse, QuadrupedModel, HorseRenderer gelesen.
- 26.3: Entity-Typen heißen `EntityTypes.PIG` (nicht `EntityType.PIG`); Spawn-Platzierung ist nur über Loader-API registrierbar (privat in `SpawnPlacements`), ohne Eintrag gilt `NO_RESTRICTIONS`. Entscheidung: die Boden-/Licht-Regel steckt in `Mob.checkSpawnRules(level, reason)` der Entität (loader-neutral, gilt nur für NATURAL/CHUNK_GENERATION; Ei, Spawner, Zucht bleiben frei).
- Hauptlinie baut `check` über alle Modul-Projekte (auch NeoForge-Kompilat): der NeoForge-Eintrag ist deshalb voll verdrahtet (Registry, Attribute, Renderer, Tests, Biom-Modifier-JSON); Forge nur Registry + Attribute (Port erst auf Anforderung).

## 2. A12 je Tier (Vanilla-Nutzen / Änderung / Eingriff)
| Tier | Nutzen im Vanilla-Geschehen | Änderung am Geschehen | Eingriff |
|---|---|---|---|
| Hirsch | zweite Leder-/Rindfleisch-Quelle wie die Kuh; Herde im Wald | kaum; scheu (flieht vor Rennenden/Herankommenden mit leeren Händen) | gering |
| Wildschwein | Schweinefleisch; Jungtier-Schutz wie der Eisbär | kleine Gefahr nahe Frischlingen; ohne Junge nur bei Reiz | gering |
| Igel | frisst Silberfische (8 Blöcke) | kleiner Schutz vor Silberfisch-Schwärmen | gering |
| Krebs | Strand-Nahrung mit Kabeljau-Werten (kein neues Nahrungsniveau) | selten, nur sichtbar am Strand | gering |
| Zebra | reitbares Pferd mit eigenem Aussehen, gleiche Werte | Kreuzung Zebra x Pferd = unfruchtbares Zorse (wie Maultier, R16 Empfehlung) | gering |
Gemeinsam: keine Blockschäden, Vanilla-Goals, Spawn-Gewichte 1 bis 4 (Vanilla-Farmtiere 8 bis 12), Gruppen 1 bis 4, nur Tageslicht auf Gras (Krebs: Sand/Schlamm), Config `spawnScale` 0..1 und ein Schalter je Tier; SB-Schalter „Enable Simple Animals“.

## 3. Entscheidungen (selbst getroffen, begründet)
- **Zorse (R16): ja, unfruchtbar.** Passt zur Vanilla-Maultier-Regel; das Zebra sucht das Pferd (`Zebra.canMate`), das Vanilla-Pferd bleibt unverändert; Schalter `zorse`. Kein Ei, kein Spawn; Werte aus beiden Eltern gemischt.
- **Zebra-Zähmung/Zucht = Pferd:** Zucht braucht gezähmte Tiere (`readyToBreed` spiegelt die geschützte Vanilla-Methode `canParent` aus öffentlichen Methoden).
- **Wildschwein-Aggro:** `NearestAttackableTargetGoal` mit Auswahlbedingung `provokedBy`: (a) Spieler ≤ 5 Blöcke um ein Ferkel der Herde (Ferkel ≤ 16 Blöcke vom Adulten, Sichtlinie) wie Eisbär; (b) Schubsen (`push`, 100 Ticks); (c) zu lange zu nah (≤ 3 Blöcke, Standard 6 s, Schleichen zählt nicht); plus `HurtByTargetGoal`, ein verletztes Ferkel ruft die Erwachsenen. Rammen = `MeleeAttackGoal` (Tempo 1.5), Schaden 4/6/8 je Schwierigkeit, Rückstoß 0.8.
- **Igel:** eigene kleine Zustandsmaschine statt Armadillo-Brain: Boolean `CURLED` (synchronisiert); Einrollen in 6 Ticks (Client-Blend), Armadillo ROLLING dauert 10 + Verstecken; Ausrollen 40 Ticks nach der letzten Gefahr; eingerollt `(d-1)/2` und Stachel 1 auf Nahkämpfer. Silberfisch zählt nicht als Gefahr (Beute). Jagd: `HuntSilverfishGoal`, Scan alle 20 Ticks, Würfel ±8 Blöcke.
- **Krebs:** `PathfinderMob` (keine Zucht, wilde Ressource), geht seitwärts (Modell um 90° gedreht), Spawn nur Strand/Mangrove, Gewicht 1.
- **Modelle:** eigene Würfel nach einer Spezifikation (`modules/simpleanimals/tools/gen_models.py`): packt die Boxen in die Textur, malt weiche Texturen (~7–11 % hart, Zebra von Natur aus streifig ~37 %) und schreibt die Java-`*Layers`-Klassen; Zebra/Zorse nutzen das Vanilla-Pferdemodell mit eigener Textur auf dem Vanilla-UV. Jungtiere: Alter-Skalierung (0.5) bzw. eigene Jungtextur (Kitz gefleckt, Frischling gestreift).
- **Drops:** Vanilla-Items (Leder, Rind-/Schweinefleisch) im Vanilla-Beutetabellenformat (Feuer = gegart, Plünderung); Krebs: neues `raw_crab` (Wert roher Kabeljau) -> `cooked_crab` (Wert gegarter Kabeljau), Ofen/Räucherofen/Lagerfeuer; Igel/Zorse: Zorse Leder, Igel nichts.
- **Mobheads:** Ausnahmen in `simplemobheads/data/mobs.json` (Köpfe später), sonst scheitert `everyMobHasAHead` bei geladenem Mobheads.

## 4. Dateien
`modules/simpleanimals/**` (neu), `modules/modules.json`, `integration/enabled-mods.json`, `mc26_3/{fabric,neoforge,forge}/build.gradle` (Bündel), `src/main/resources/fabric.mod.json`, `neoforge/.../neoforge.mods.toml`, SB-Schalter (`SimplebuildingConfig`, `ConfigOptions`, `ConfigOptionTests`, Overlay-Lang EN/DE, `TcContext.FAMILY`), `tools/{guides/module_guides.py,discover/discover.py,textures/mod_icons.py,textures/module_guide_books_2026_10_05.py,audit/module_purposes.json}`, `modules/simplemoney/.../money/simpleanimals/prices.json`, `modules/simplemobheads/data/mobs.json` + generierte `HeadTable.java`, `docs/MODS-UND-FEATURES.md`, `docs/ai/PRINZIPIEN.md` (Teil 2).

## 5. Verifikation
Lokal ohne Gradle: alle Python-Prüfer von `gradlew check` (Texte, Loot-Format, fehlende Texturen, Ebenen, Icons, Modul-Stil, Lang-Schlüssel, Multimod, Discover, Handbücher, Quests, Questionary, Modul-`check_data`). Ein `sbt`-Lauf: Wiki-Generator + Handbuch/Discover (committen als `-gen`), Compile `:mc26_3:fabric` + `:modules:simpleanimals:{fabric,neoforge}`, Targets `module-simpleanimals-fabric-263` und `module-simpleanimals-standalone-fabric-263`, `./gradlew check -q`.
Gametests (20): Registrierung, Config-Klemmen, Super-Schalter, Spawn-Boden, Wildschwein (Ferkel-Aggro, kein Reiz ohne Ferkel, Schubsen/Nähe, Rammschaden), Igel (Einrollen schneller als Armadillo, reduzierter Schaden + Stachel, Silberfisch-Jagd 8 Blöcke), Zebra (Kreuzung, Zorse unfruchtbar, wilde Zebras paaren nicht, Pferd-Größe), Hirsch-Futter, Krebs-Nahrungswerte, Handbuch.

## 6. Risiken / offen (siehe Memory-Abschnitt)
- Optik nur erste Fassung, Besitzer wählt anhand der Vorschau (Varianten je Tier: Hirsch Rot/Gelb, Wildschwein dunkel/hell, Igel braun/grau, Krebs rot/sand; Zebra/Zorse). Keine Client-Läufe (Besitzer-Regel), also Renderer/Animation nur per Kompilat und Vorschau geprüft.
- Natürliches Spawnen nicht per Gametest (Zufall/Licht), nur die Regeln.
- Köpfe (Mobheads), NeoForge/Forge-Spawns und -Läufe, JEI-Seiten: später.
