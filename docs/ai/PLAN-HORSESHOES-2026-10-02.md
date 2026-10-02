# Plan: R1 Hufeisen (Simple Riding), 2026-10-02

Branch `claude-horseshoe` (Basis origin/master 3f62ad26). Spezifikation: `docs/ai/BACKLOG-2026-10-01.md`
R1 und Besitzer-Antworten 14-16.

## Ist-Zustand
- `modules/simpleriding` ist nur 26.3 (Fabric/NeoForge/Forge), hatte bisher **keine Items**
  (Test `launch` und `tools/check_data.py` pruefen das ausdruecklich - wird angepasst).
- Gemeinsamer Code in `shared/java/com/simpleriding`, Mixins in `simpleriding.mixins.json` (Forge nutzt
  dieselbe Datei), Daten in `generated/resources` (handgeschrieben), Lang nur `shared/resources/.../lang`
  (kein mc26_3-Overlay im Modul).
- Konfig `RidingConfig` + Katalog `RidingOptions` + `balance/simpleriding/options.json` + Lang-Tooltips
  + je Option ein Wiki-Kapitel (`check_data.py`: Kapitel = 8 + Optionen).
- Loot: `RidingLoot.apply` (alle drei Loader), Tests in `test/RidingTests.ALL`, Fabric-Katalog
  `RidingGameTest.java`, NeoForge/Forge registrieren `ALL` automatisch.
- Effekte: `RidingEffects.tick` (LivingEntity-Tick, serverseitig, transiente Attribut-Modifikatoren mit
  Gesamtdeckel `safety.maximumSpeedBonus`/`maximumJumpBonus`).

## Design (Entscheidungen)
1. **Items** `copper_horseshoe`, `iron_horseshoe`, `golden_horseshoe`, `diamond_horseshoe`,
   `netherite_horseshoe`, `enderite_horseshoe` (nur registriert, wenn SimpleBuilding geladen ist) und
   `horseshoe_smithing_template` (SmithingTemplateItem, Uncommon).
   Haltbarkeit 120/180/80/400/500/620, Verzauberbarkeit 8/9/22/10/15/15, Netherit/Enderit feuerfest,
   Reparatur mit dem Material. Tag `minecraft:enchantable/durability` -> Haltbarkeit + Reparatur
   (Unbreaking, Mending) am Amboss/Tisch.
2. **Rezepte** (Schmiedetisch, Vorlage wird wie Vanilla verbraucht): Vorlage + Material (Kupfer-/Eisen-/
   Goldbarren, Diamant) + Eisennugget (Naegel) -> Hufeisen; Vorlage + Diamant-Hufeisen + Netheritbarren ->
   Netherit; Vorlage + Netherit-Hufeisen + Enderitbarren -> Enderit (Ladebedingung simplebuilding).
   Upgrades behalten Verzauberungen/Schaden (smithing_transform). Duplizieren (Werkbank): `#S#/#C#/###`
   mit S = Vorlage, C = Eisenbarren, # = Kupferbarren -> 2 Vorlagen.
3. **Slots:** Pferd, Esel, Maultier, Skelett- und Zombiepferd (Entity-Tag
   `simpleriding:can_wear_horseshoes`) bekommen 4 Hufeisen-Slots (vorne links/rechts, hinten links/rechts)
   als Leiste links am Vanilla-Pferdeinventar. Speicherung am Pferd (`simpleriding:horseshoes`),
   Abwurf beim Tod (ausser Fluch des Verschwindens), Shift-Klick in beide Richtungen.
   Synchronisiert als eine Ganzzahl (4 x 3 Bit Stufe) fuer Modell und clientseitige Steuerung.
4. **Wertung:** Hufeisen der Stufe t (Kupfer=1 ... Enderit=6) zaehlt 2^(t-1) Punkte -> 2 Hufeisen der
   naechsten Stufe = 4 der Stufe darunter (exakt gleiche Punkte). Wirkung f = log2(1+P)/log2(129)
   (0..1; voller Enderit-Satz = 1, voller Kupfersatz = 0,33) - abnehmender Ertrag, damit Kupfer
   spuerbar bleibt.
5. **Effekte** (nur spielergeritten, serverseitig, Konfig mit harten Deckeln):
   - Gelaende: `minecraft:movement_efficiency` + `terrainBonus*f` (Seelensand/Honig bremsen weniger).
   - Handling: Seitwaerts-Anteil 0,5 -> 0,5 + `handlingBonus*f`, Rueckwaerts 0,25 -> 0,25 + `handlingBonus*f/2`
     (Eingabe wird wie Vanilla normalisiert; Vorwaerts-Hoechsttempo unveraendert).
   - Volle 4 Hufeisen: +`fullSetSpeedBonus` Tempo, +`fullSetJumpBonus` Sprung, beides innerhalb der
     bestehenden Gesamtdeckel (mit Rueckenwind/Sprungkraft zusammen); Fallschaden x(1+`fullSetFallDamageIncrease`)
     gerundet ("minimal").
   - Abnutzung: je `blocksPerDurability` geritten zurueckgelegter Bloecke am Boden 1 Haltbarkeit je
     Hufeisen (Unbreaking wirkt ueber Vanilla). Mending: XP, die der Reiter aufsammelt und Vanilla nicht
     verbraucht, repariert Mending-Hufeisen des gerittenen Pferdes (zusaetzlich zu Vanilla in der Hand).
   - Abweichung: eine eigene "Beschleunigung" gibt es nicht - Vanilla-Pferde erreichen am Boden in
     ~4 Ticks 90 % Endtempo; Beschleunigungsgewinn entsteht durch Gelaende (Lerp der Blockbremse) und
     Handling. Im Bericht benannt.
6. **Loot:** Dorf-Waffenschmied und Gerber (Stall-/Sattelkisten): eigener Pool (Vorlage 15 %, Kupfer-/
   Eisen-Hufeisen je 5 %, beschaedigt). Trail Ruins (Archaeologie, genau 1 Item je Block): Mixin am
   Pinsel-Entpacken ersetzt deterministisch (Seed) in `trail_ruins_rare` mit 1/13 die Beute durch die
   Vorlage (wie ein weiterer gleichgewichteter Eintrag), in `trail_ruins_common` mit 1/46 durch ein
   Kupfer-/Eisen-Hufeisen. Alles hinter `worldGen.enableLootTableChanges`.
7. **Modell:** Render-Ebene an Pferd/Esel/Maultier/Skelett-/Zombiepferd (nur erwachsen): je Bein ein
   eigenes Bein-Modell (Pose wie Vanilla) mit Stufen-Textur 64x64, die nur Hufpixel (unterste Seitenreihe,
   Kappe vorn, Sohle) deckt.
8. **Konfig** neue Sektion `horseshoes`: `enableHorseshoes` (true), `terrainBonus` 0.5 [0,1],
   `handlingBonus` 0.3 [0,0.5], `fullSetSpeedBonus` 0.05 [0,0.1], `fullSetJumpBonus` 0.05 [0,0.1],
   `fullSetFallDamageIncrease` 0.1 [0,0.25], `blocksPerDurability` 40 [8,400].

## Dateien
- neu: `shared/java/com/simpleriding/Horseshoes.java`, `HorseshoeHolder.java`, `HorseshoeSlot.java`,
  `client/HorseshoeLayer.java`, `client/HorseshoeState.java`, Mixins `HorseshoeHorseMixin`,
  `HorseshoeMenuMixin`, `HorseshoeQuickMoveMixin`, `HorseshoeXpMixin`, `HorseshoeBrushMixin`,
  `client/HorseshoeRenderStateMixin`, `client/HorseshoeRendererMixin`, `client/HorseshoeLayerMixins`,
  `client/LivingRendererAccess`, `client/HorseshoeScreenMixin`, `client/HorseshoeClickMixin`.
- geaendert: `Riding`, `RidingConfig`, `RidingOptions`, `RidingEffects`, `RidingLoot`, `RidingFallMixin`,
  Loader-Einstiege (Fabric/NeoForge/Forge), Lang en/de, `balance/simpleriding/options.json`,
  `wiki/manual.json`, `tools/check_data.py`, Tests `RidingTests` + `RidingGameTest`.
- Daten: Rezepte, Tags, Item-Modelle, Texturen (Generator `tools/textures/horseshoe_textures.py`,
  Vorschau `previews/hufeisen-vorschau.png` mit 3 Vorschlaegen je neuer Textur).

## Risiken
- Mixin-Ziele (Konstruktoren der Renderer, private `unpackLootTable`, `repairPlayerItems`) - per
  Build/Test pruefen. Slot-Indizes: neue Slots hinter dem Spielerinventar, `quickMoveStack` korrigiert.
- Client-Darstellung (Leiste, Modell) wird von Server-Tests nicht erfasst -> nur Kompilierung belegt.

## Verifikation
1. `python modules/simpleriding/tools/check_data.py`
2. `python tools/testrunner/run.py --targets module-simpleriding-fabric-263` und `...-neoforge-263`
   ("alles gruen" lesen); neue Tests: Registrierung/Rezepte, Punkte-Regel, Effekte+Deckel, Handling,
   Menue/Slots/Shift-Klick, Speichern/Abwurf, Abnutzung/Unbreaking, Mending, Fallschaden, Loot/Schalter.
3. `python wiki/generate.py --all`, danach `--all --check` sauber.
4. Textur-Generator `--check`.

## Abweichungen waehrend der Umsetzung
- Synchronisierung: NeoForge verbietet per Mixin hinzugefuegte SynchedEntityData (Absturz beim Laden).
  Der Hufcode laeuft daher ueber `Horseshoes.SYNC`: Fabric/Forge je ein loaderspezifischer Entity-Data-Mixin
  (`HorseshoeDataMixin`), NeoForge ein synchronisierter Data-Attachment (`RidingNeoAttachments`).
- Keine eigene Beschleunigung (siehe Design 5).

## Ergebnis
- `check_data.py` gruen; Fabric 37/37 und NeoForge 37/37 Modultests gruen; Forge nur kompiliert;
  Wiki `--all --check` sauber; Client (Leiste, Huf-Modell) nicht im Spiel geprueft.
