# Plan A30: Simple Dinosaurs Stufe 1 (DINO28-1, Agent a-dino30)

Stand 30.10.2026. Nur MC 26.3 Fabric. Konzept (abgenommen ABN29b-4): `docs/ai/KONZEPT-A-WP18-2026-10-29.md` §3. Neues Sub-Mod `simpledinos` (Muster `simpleanimals` auf `a-animals29`, Wiring wie dort: Modul in `modules.json`, SB-Schalter „Enable Simple Dinosaurs“, Bündel). Vorschau: `/root/sb-tools-a/previews/a-dino30-preview.html`.

## 1. Ist-Zustand
- Vorbild Aufbau/Tests/Generatoren: `simpleanimals` (nur auf `a-animals29`, nicht in wave1): Registry + Loader-Adapter, `tools/gen_models.py` (Würfel-Spezifikation → Textur + `*Layers.java`), `tools/preview.py` (Turntable-Vorschau), Guide/Discover/Lang per Generator.
- Es gibt KEINE vorhandene Sitzmechanik (Sitzkissen existiert nicht), Hängematte ist ein Block mit eigener Logik. Entscheidung: Sitz = unsichtbarer Rüstungsständer als Fahrzeug (Vanilla-Entität, kein eigener Renderer), vom Ei-Block gesteuert.
- Vanilla `BrushItem` ruft nur `BrushableBlockEntity.brush`; ein eigener Block kann das nicht erben. Entscheidung: Der Fossilienblock plant sich per Block-Tick (jeden Tick, solange gepinselt wird) und fragt `player.isUsingItem() && Pinsel && Blick trifft den Block` ab. Loader-neutral, kein Mixin.
- Reittier-Basis `AbstractHorse` (wie Zebra): Sattel-/Rüstungs-Slot, Inventar, Steuerung, Sprungbalken geschenkt.

## 2. A12 (Vanilla-Nutzen / Änderung / Eingriff)
| Teil | Nutzen im Vanilla-Geschehen | Änderung | Eingriff |
|---|---|---|---|
| Fossilienblock | Archäologie mit dem Vanilla-Pinsel | seltener Fund im Untergrund von Wüste/Badlands/Sumpf | gering |
| Ei, Brüten | Langzeitprojekt (Pinseln 60 s, Brüten 5 min, Schlüpfen 10 min, Wachsen 40 min) | Spieler sitzt auf dem Ei | gering |
| T-Rex | Kampf-Reittier, verteidigt den Besitzer | nur durch Schlüpfen, nie wild | gering |
Vanilla-Fossilien/Mobs/Blöcke bleiben unverändert; kein Blockschaden; Dinos greifen nie Spieler an.

## 3. Entscheidungen (selbst getroffen)
- **Ein Ei-Block je Sorte** (`trex_egg` …), Zustand `phase` 0..14 (0..11 Brüten in 12 Stufen à 500 Ticks, 12..14 = Risse 1..3 nach dem Brüten). Modelle: Basis-Textur je Sorte + gemeinsame Überlagerungen (Glut je Stufe, Risse).
- **Fossil**: Zustand `stage` 0..9, BE speichert `sort` (aus Position gewürfelt, nur unter den schlüpffähigen Sorten) und fortlaufenden `Progress`. Fortschritt ist lazy: `Progress` (rein, testbar): Zuwachs je Pinsel-Tick, nach `grace` Ticks Pause Rücklauf 2 pro Tick (1 s Pause = 2 s weg); Ei: Sitzen zählt in 10-Tick-Schritten, Aufstehen = Rücklauf 2/Tick (doppelt so schnell). Nach 100 % sperrt das Brüten (R5).
- **Schlüpfen** nach 12 000 Ticks (R1 = 10 min), nur wenn ein Spieler in 16 Blöcken ist (Besitzer = längster Sitzer, sonst nächster Spieler). Zeiten stehen in der Config (`dinos-server.json`), Standard = Konzept.
- **Nur der T-Rex hat eine Entität** (Stufe 1). Fossilien würfeln daher nur unter schlüpffähigen Sorten (`DinoKind.hatchable`); die Eier der anderen vier Sorten sind registriert, brüten, haben Textur, schlüpfen aber erst mit ihrer Entität (Rest, kein Platzhalter ohne Optik).
- **T-Rex**: `AbstractHorse`-Unterklasse `Dino`; Hitbox-Kern 3,0 x 4,0, große Culling-Box im Renderer; Wachstum 25 → 100 % über 48 000 Ticks (synchronisiert, `getAgeScale` stufenlos); leichte KI (Schweben, Panik als Jungtier, Besitzer folgen, nur Monster angreifen, die Besitzer/Dino angreifen). Futter rohes Fleisch (Tag `meat`). Baby nicht reitbar, nicht satteln.
- **Großer Sattel** (`large_saddle`, Rezept Sattel + 4 Leder + 4 Eisenbarren) und **T-Rex-Rüstung** Leder/Eisen/Diamant (Vanilla-Pferderüstungswerte, `Equippable` mit eigenen Entity-Tags): Anzeige als Texturvariante des T-Rex (kein Extra-Layer, vermeidet 26.3-Render-Risiko).
- Kein Spawn-Ei (Besitzer). Testzentrale legt Eier/Sattel/Rüstung bereit; `/summon simpledinos:trex` zum Testen.
- SB-Schalter „Enable Simple Dinosaurs“ blendet Kreativ-/Handbuch-Einträge aus; Mechanik bleibt (wie Gadgets).

## 4. Dateien
`modules/simpledinos/**` (neu), `modules/modules.json`, `integration/enabled-mods.json`, `mc26_3/{fabric,neoforge,forge}/build.gradle`, `src/main/resources/fabric.mod.json`, `neoforge/.../neoforge.mods.toml`, SB-Schalter (`SimplebuildingConfig`, `ConfigOptions`, `ConfigOptionTests`, Overlay-Lang, `TcContext.FAMILY`), `tools/{guides/module_guides.py,discover/discover.py,textures/mod_icons.py,textures/module_guide_books_2026_10_05.py,audit/module_purposes.json}`, `simplemoney/.../money/simpledinos/prices.json`, `simplemobheads/data/mobs.json`, `docs/MODS-UND-FEATURES.md`, `docs/ai/PRINZIPIEN.md`, `.claude/QUEUE.md`.

## 5. Verifikation
Billig lokal: Python-Prüfer (check_data, Texte, Loot, Texturen). Ein `sbt`-Lauf: Generatoren (Guide, Wiki, Discover, Icons) → `-gen` Commit; Compile `:mc26_3:fabric` + `:modules:simpledinos:{fabric,neoforge}`; Targets `module-simpledinos-fabric-263`, `module-simpledinos-standalone-fabric-263`; `./gradlew check -q`.
Gametests: Registrierung, Config-Klemmen, Progress-Rücklauf (Pinsel + Ei), Fossil-Abschluss → Ei, Ei-Brüten (Stufen, Sperre), Schlüpfen → Jungtier mit Besitzer, Wachstum (Skalierung), Reiten-Grundlagen (Sattel, Rüstung, Baby nicht), Handbuch.

## 6. Risiken / Rest
- Optik nur erste Fassung (Vorschau, Standard = Empfehlung); keine Client-Läufe.
- Nicht in Stufe 1: Triceratops/Stegosaurus/Utahraptor/Parasaurolophus-Entitäten, Vanilla-Fossilienstruktur-Anteil (R7 a), Brüllen/Ausdauer-Sprint des T-Rex, Dino-Sounds (ElevenLabs R14, bis dahin Vanilla-Töne), Rüstungen der übrigen Dinos, max. zahme Dinos je Spieler, Forge/NeoForge-Läufe (NeoForge nur kompiliert).
